# Optional runner for the SauceDemo and DemoQA example suites.
#
# Usage: powershell -ExecutionPolicy Bypass -File .\run-examples.ps1                 (both suites)
#        powershell -ExecutionPolicy Bypass -File .\run-examples.ps1 demoqa          (one suite: saucedemo or demoqa)
#        powershell -ExecutionPolicy Bypass -File .\run-examples.ps1 all -Headless   (no browser window)
#
# With Maven installed, this is the same as: mvn clean test "-Dsurefire.suiteXmlFiles=src/testNgXml/examples.xml"
# Without Maven, it downloads Apache Maven 3.9.12 into tools\ once (about 9 MB, checksum verified, no admin
# rights) and reuses it on later runs. Nothing already installed is downloaded again.
# Needs JDK 17+. Chrome is used if installed; otherwise Selenium downloads it on first use.
param(
  [ValidateSet('all', 'saucedemo', 'demoqa')][string]$Suite = 'all',
  [switch]$Headless
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'   # Invoke-WebRequest is very slow with the progress bar on
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$root = $PSScriptRoot
$maven = @{
  Name   = 'Apache Maven 3.9.12'
  Url    = 'https://archive.apache.org/dist/maven/maven-3/3.9.12/binaries/apache-maven-3.9.12-bin.zip'
  Sha512 = '0ef0d074761a55e7b982fd7f2cd6ea2da028289c73156ebd488c0513efbff8a64ac5bc0c11c3b8bbced930ce15238ecef344b0e065ae54f7cdca8c86cf39e736'
  Dir    = Join-Path $root 'tools\apache-maven-3.9.12'
  Into   = Join-Path $root 'tools'
}
$suites = @{
  all       = @{ Xml = 'src/testNgXml/examples.xml';  Tests = 63; Minutes = 4; Report = 'logs\extent-report\ExamplesSuite.html' }
  saucedemo = @{ Xml = 'src/testNgXml/saucedemo.xml'; Tests = 45; Minutes = 2; Report = $null }
  demoqa    = @{ Xml = 'src/testNgXml/demoqa.xml';    Tests = 18; Minutes = 2; Report = 'logs\extent-report\DemoQASuite.html' }
}
$run = $suites[$Suite]

function Format-Duration($span) {
  if ($span.TotalMinutes -ge 1) { '{0}m {1:00}s' -f [int][math]::Floor($span.TotalMinutes), $span.Seconds } else { '{0}s' -f [int]$span.TotalSeconds }
}

Write-Host "Running the $Suite suite ($($run.Tests) tests, about $($run.Minutes) min once set up)" -ForegroundColor Cyan

# --- Java -------------------------------------------------------------------
Write-Host "`n[1/3] Checking Java and Chrome" -ForegroundColor Cyan
$java = if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))) { Join-Path $env:JAVA_HOME 'bin\java.exe' }
        else { (Get-Command java -ErrorAction SilentlyContinue).Source }
if (-not $java) {
  throw "Java was not found. Install JDK 17 or newer (for example from https://adoptium.net), then run this again."
}
# java -version writes to stderr; run it through cmd so PowerShell 5.1 doesn't treat that as an error.
$versionLine = (cmd /c "`"$java`" -version 2>&1" | Select-Object -First 1)
$major = if ($versionLine -match '"1\.(\d+)') { [int]$Matches[1] } elseif ($versionLine -match '"(\d+)') { [int]$Matches[1] } else { 0 }
if ($major -lt 17) {
  throw "Java 17 or newer is required, but found: $versionLine ($java). Install JDK 17+ or point JAVA_HOME at one."
}
Write-Host "  Java $major found ($java)"

$chromePaths = @(
  "$env:ProgramFiles\Google\Chrome\Application\chrome.exe",
  "${env:ProgramFiles(x86)}\Google\Chrome\Application\chrome.exe",
  "$env:LOCALAPPDATA\Google\Chrome\Application\chrome.exe"
)
$chrome = $chromePaths | Where-Object { $_ -and (Test-Path $_) } | Select-Object -First 1
if ($chrome) { Write-Host "  Chrome found ($chrome)" }
else { Write-Host "  Chrome not found; Selenium will download a copy on first use (about 150 MB)." -ForegroundColor Yellow }

# --- Maven ------------------------------------------------------------------
Write-Host "`n[2/3] Checking Maven" -ForegroundColor Cyan
$installed = Get-Command mvn -ErrorAction SilentlyContinue
$localMvn = Join-Path $maven.Dir 'bin\mvn.cmd'
if ($installed) {
  $mvn = $installed.Source
  Write-Host "  Maven found ($mvn); nothing to download."
} elseif (Test-Path $localMvn) {
  $mvn = $localMvn
  Write-Host "  Using $($maven.Name) from an earlier run (tools\apache-maven-3.9.12); nothing to download."
} else {
  $name = Split-Path $maven.Url -Leaf
  Write-Host "  Maven is not installed. Downloading $($maven.Name) into tools\ (about 9 MB, first run only) ..."
  New-Item -ItemType Directory -Force $maven.Into | Out-Null
  $zip = Join-Path $maven.Into $name
  $started = Get-Date
  # A dropped connection shouldn't end the run, so retry a few times.
  for ($attempt = 1; ; $attempt++) {
    try {
      Invoke-WebRequest $maven.Url -OutFile $zip -UseBasicParsing
      break
    } catch {
      Remove-Item $zip -ErrorAction SilentlyContinue
      if ($attempt -ge 3) { throw "Failed to download $name after 3 attempts: $($_.Exception.Message)" }
      Write-Host "  Download failed, retrying in 5 s (attempt $($attempt + 1) of 3): $($_.Exception.Message)" -ForegroundColor Yellow
      Start-Sleep -Seconds 5
    }
  }
  Write-Host ("  Downloaded {0:N1} MB in {1:N0} s" -f ((Get-Item $zip).Length / 1MB), ((Get-Date) - $started).TotalSeconds)
  $actual = (Get-FileHash $zip -Algorithm SHA512).Hash
  if ($actual -ne $maven.Sha512) {
    Remove-Item $zip
    throw "Checksum mismatch for $name (expected $($maven.Sha512), got $actual)"
  }
  Write-Host "  Checksum OK (SHA512)"
  tar -xf $zip -C $maven.Into
  if ($LASTEXITCODE -ne 0) { throw "Failed to unpack $name" }
  Remove-Item $zip
  $mvn = $localMvn
  Write-Host "  Installed in tools\apache-maven-3.9.12"
}

# --- Tests ------------------------------------------------------------------
Write-Host "`n[3/3] Running tests" -ForegroundColor Cyan
$m2 = Join-Path $env:USERPROFILE '.m2\repository'
if (-not (Test-Path (Join-Path $m2 'org\seleniumhq'))) {
  Write-Host "  First run: Maven downloads the project's libraries into $m2 before the tests start (a few minutes, once)."
}
if ($Headless) { Write-Host "  Headless: Chrome runs without a window." }
else { Write-Host "  Chrome windows will open and close while the tests run; leave them alone until the run finishes." }
Write-Host "  Command: mvn clean test -Dsurefire.suiteXmlFiles=$($run.Xml)$(if ($Headless) { ' -Dheadless=true' })`n"

$mvnArgs = @('clean', 'test', "-Dsurefire.suiteXmlFiles=$($run.Xml)")
if ($Headless) { $mvnArgs += '-Dheadless=true' }
$started = Get-Date
Push-Location $root
try {
  & $mvn @mvnArgs
  $exitCode = $LASTEXITCODE
} finally {
  Pop-Location
}

# --- Summary ----------------------------------------------------------------
Write-Host "`n=== Summary ===" -ForegroundColor Cyan
$resultsXml = Join-Path $root 'target\surefire-reports\testng-results.xml'
if (Test-Path $resultsXml) {
  $r = ([xml](Get-Content $resultsXml -Raw))."testng-results"
  $color = if ([int]$r.failed -eq 0 -and [int]$r.skipped -eq 0) { 'Green' } else { 'Red' }
  Write-Host "  Tests: $($r.total) run, $($r.passed) passed, $($r.failed) failed, $($r.skipped) skipped" -ForegroundColor $color
} else {
  Write-Host "  No test results were written; see the Maven output above." -ForegroundColor Red
}
Write-Host "  Time:  $(Format-Duration ((Get-Date) - $started))"
if ($run.Report) {
  Write-Host "  Extent report: $($run.Report)  (open in a browser)"
  Write-Host "  Known-defect tests pass while the defect is still present; the Extent report marks them as warnings."
} else {
  Write-Host "  Allure report: run `"$mvn`" allure:serve to open it in a browser"
}
exit $exitCode

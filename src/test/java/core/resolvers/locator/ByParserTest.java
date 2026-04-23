package core.resolvers.locator;

import org.openqa.selenium.By;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/** Unit tests for {@link ByParser} — the canonical raw-string → {@link By} parser. */
public class ByParserTest {

    @DataProvider(name = "prefixes")
    public Object[][] prefixes() {
        return new Object[][] {
            {"id=foo",                 By.id("foo").toString()},
            {"ID=foo",                 By.id("foo").toString()},                 // case-insensitive prefix
            {"name=q",                 By.name("q").toString()},
            {"class=btn",              By.className("btn").toString()},
            {"tag=input",              By.tagName("input").toString()},
            {"linkText=Click",         By.linkText("Click").toString()},
            {"partialLinkText=Read",   By.partialLinkText("Read").toString()},
            {"css=#x > a",             By.cssSelector("#x > a").toString()},
            {"xpath=//h1",             By.xpath("//h1").toString()},
            {"  xpath=//h1  ",         By.xpath("//h1").toString()},             // outer whitespace
            // Heuristic fallback
            {"//div",                  By.xpath("//div").toString()},
            {"/html/body",             By.xpath("/html/body").toString()},
            {"(//tr)[last()]",         By.xpath("(//tr)[last()]").toString()},
            {".//span",                By.xpath(".//span").toString()},
            {".btn",                   By.cssSelector(".btn").toString()},      // dot-prefixed → CSS
            {"div.container",          By.cssSelector("div.container").toString()},
        };
    }

    @Test(dataProvider = "prefixes")
    public void parse_recognisesPrefixesAndHeuristics(String raw, String expected) {
        assertEquals(ByParser.DEFAULT.parse(raw).toString(), expected);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void parse_null_throwsIllegalArgument() {
        ByParser.DEFAULT.parse(null);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void parse_blank_throwsIllegalState() {
        ByParser.DEFAULT.parse("   ");
    }

    @Test(expectedExceptions = IllegalStateException.class,
          expectedExceptionsMessageRegExp = ".*Empty value after 'id='.*")
    public void parse_emptyValueAfterPrefix_throwsIllegalState() {
        ByParser.DEFAULT.parse("id=   ");
    }
}


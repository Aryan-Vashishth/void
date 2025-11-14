#@AccountMapping @Regression @ImportRecords @VendorUser
#Feature: Verify import records via Vendor User in the Account Mapping page
#
#  Background:
#    Given User is on Login Page
#    And Enter valid login credentials
#    Then Validate Title of page is "Vartopia"
#    And User clicks on "Manage Users" from "tiles" in "admin_home" and wait for angular loader
#    Then User successfully landed on "Manage Users" Page
##    When "Vendor User" with Email "qoalix@vartopia.mailinator.com" is Logged in from Manage Users Page
#    When "Vendor User" with Email "qaminitest@vartopia.mailinator.com" is Logged in from Manage Users Page
##    When "Vendor User" with Email "acmeglobal@vartopia.mailinator.com" is Logged in from Manage Users Page
#    Then User successfully landed on "Home" Page
#    And User clicks on "Account Mapping" from "navigation_bar" in "ui" and wait for angular loader
#    Then User successfully landed on "Account Mapping" Page
#
#  @VendorUser @AccountMapping @AccountMappingUI #@skip
#  Scenario: UI validation for Account Mapping page
#    Given the following elements from "quick_search" in "ui" are visible on the page:
#      | Record Id     |
#      | Website       |
#      | Account Name  |
#      | Search Button |
#    And the following elements from "default_reports_tiles" in "account_mapping_home_page" are visible on the page:
#      | Open Accounts     |
#      | Pending Accounts  |
#      | Claimed Accounts  |
#      | Expired Accounts  |
#      | Archived Accounts |
#
#    Then User clicks on "Open Accounts" from "default_reports_tiles" in "account_mapping_home_page" and wait for angular loader
#    Then User successfully landed on "Records" Page
#
#    And the following elements from "default_reports" in "account_mapping" are visible on the page:
#      | Open Accounts     |
#      | Pending Accounts  |
#      | Claimed Accounts  |
#      | Expired Accounts  |
#      | Archived Accounts |
#    And the following dropdown options from "actions_dropdown" in "account_mapping" are visible on the page:
#      | Filter         |
#      | Import Records |
#      | Export         |
#    And the following dropdown options from "group_by_dropdown" in "account_mapping" are visible on the page:
#      | Country           |
#      | Geo Location      |
#      | HQ State/Province |
#      | Program Name      |
#      | Status            |
#    And the following dropdown options for dropdown #1 from "three_dots_menu" in "account_mapping" are visible:
#      | New Registration |
#      | Archive Record   |
#      | Edit Record      |
#
#    Given User clicks on "Pending Accounts" from "default_reports" in "account_mapping" and wait for angular loader
#    Given the following dropdown options for dropdown #1 from "three_dots_menu" in "account_mapping" are visible:
#      | View Registration |
#
##    And User clicks on "Account Mapping" from "navigation_bar" in "ui" and wait for angular loader
##    Then User successfully landed on "Account Mapping" Page
#
#  @VendorUser @AccountMapping @ImportRecords #@skip
#  Scenario: Functional import records workflow via file upload
#
#    And User selects "Insert New Records" from "import_records_dropdown" dropdown in "account_mapping_home_page"
#    And Switch to Import Records popup iframe
#    Then User should see error when uploading invalid file "DevOps_to_AI_Roadmap.pdf" in Import Records popup
#    And User should see error when uploading invalid file "New_Text_Document.txt" in Import Records popup
#    Then User should be able to upload valid file "Account_Records_list.xlsx" in Import Records popup
#    Given User clicks on "next" from "navigation_buttons" in "import_records_popup"
#    And User Insert records in table from import Records
#    And User clicks on "add" from "table_row_buttons" in "import_records_popup"
#    And User clicks on "submit" from "navigation_buttons" in "import_records_popup"
#    Then Switch to default iframe
#    And Wait for Import Records popup to disappear
#    Given User successfully landed on "Records" Page
#    And save the first row from "records" in "records_grid-account_mapping" page to "last-actual-open-records" as JSON
#    Then the expected JSON file "last-inserted-open-records" data should match the actual JSON file "last-actual-open-records" with ignore extra actual records set to true and ignoring the following expected keys: "null"
#
#    Given User clicks on "Pending Accounts" from "default_reports" in "account_mapping" and wait for angular loader
#    And save the first row from "records" in "records_grid-account_mapping" page to "last-expected-pending-records" as JSON
#    When User selects "View Registration" option from "three_dots_menu" dropdown #1 in "account_mapping"
#    And Temp 2
#    Then the expected JSON file "last-expected-pending-records" data should match the actual JSON file "last-actual-pending-records" with ignore extra actual records set to true and ignoring the following expected keys: "Vendor Status, Status"
#
##  @skip
##  Scenario: Temp
##    And Temp 1
##
##  @VendorUser #@skip
##  Scenario: New Reg AM
##    And User clicks on "New Registration" from "navigation_bar" in "ui" and wait for angular loader
##    And User searches and click on "Vartopia" using "select your partner" from "partner_information" in "new_registration" and wait for angular loader
###    And User searches and click on "Advanticom" using "select your partner" from "partner_information" in "new_registration" and wait for angular loader
##    And User clicks on "next" from "partner_information_navigation" in "new_registration"
##    Then User select following vendor programs in new registration page:
##      | Acme Global Deal Registration |
##
##    And User clicks on "next" from "program_selection_navigation" in "new_registration"
##    And User fill opportunity information in new registration page
##
##    And User clicks on "next" from "opportunity_information_navigation" in "new_registration"
##
##    And User clicks on "save" from "sales_rep_and_end_customer_information_navigation" in "new_registration"
##
##    And Test some class and methods
##    And Register a form
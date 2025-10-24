@AccountMapping @Regression @ImportRecords @PartnerUser
Feature: Verify import records via Partner User in the Account Mapping page

  Background:
    Given User is on Login Page
    And Enter valid login credentials
    Then Validate Title of page is "Vartopia"
    And User clicks on "Manage Users" from "tiles" in "admin_home" and wait for angular loader
#    Then Temp step
    Then User successfully landed on "Manage Users" Page
    When "Partner User" with Email "support@vartopia.mailinator.com" is Logged in from Manage Users Page
    And User clicks on "Account Mapping" from "navigation_bar" in "ui" and wait for angular loader
    Then User successfully landed on "Account Mapping" Page

  @PartnerUser @AccountMapping @AccountMappingUI #@skip
  Scenario: UI validation for Account Mapping page
    Given the following elements from "quick_search" in "ui" are visible on the page:
      | Record Id     |
      | Website       |
      | Account Name  |
      | Search Button |
    And the following elements from "default_reports_tiles" in "account_mapping_home_page" are visible on the page:
      | Open Accounts       |
      | Pending Accounts    |
      | Claimed Accounts    |
      | SmartMatch Accounts |

    Then User clicks on "Open Accounts" from "default_reports_tiles" in "account_mapping_home_page" and wait for angular loader
    Then User successfully landed on "Records" Page

    And the following elements from "default_reports" in "account_mapping" are visible on the page:
      | Open Accounts       |
      | Pending Accounts    |
      | Claimed Accounts    |
      | SmartMatch Accounts |

    And the following dropdown options from "actions_dropdown" in "account_mapping" are visible on the page:
      | Filter |
      | Export |
    And the following dropdown options from "group_by_dropdown" in "account_mapping" are visible on the page:
      | Country           |
      | Geo Location      |
      | HQ State/Province |
      | Program Name      |
      | Status            |
      | Vendor            |
    And the following dropdown options for dropdown #1 from "three_dots_menu" in "account_mapping" are visible:
      | New Registration |

    Given User clicks on "Pending Accounts" from "default_reports" in "account_mapping" and wait for angular loader
    Given the following dropdown options for dropdown #1 from "three_dots_menu" in "account_mapping" are visible:
      | View Registration |
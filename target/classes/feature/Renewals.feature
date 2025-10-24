Feature: Renewals Functionality for Bluejeans Vendor in Vartopia application

  Background:
    Given User is on Login Page
    When Enter valid login credentials
    Then Validate Title of page is "Vartopia"

  Scenario Outline: Validate Renewals Grid Deal count With Export CSV
    Then Enter username going to login "<User>"
    Then Move to "Renewals" module
    And Validation Page URL
    Then Move to Grid View "<Report_Type>" "<Report_Name>"
    And Check Grid Count with DB and Import CSV "<User_Type>" "<Report_Type>"
    Examples:
      | Report_Type | User_Type | User                    | Report_Name           |
      | Default     | Vendor    | BlueJeans@vartopia.com  | Created Last 365 Days |
      | Default     | Var       | scotsha@cdw.com         | Created Last 365 Days |
      | Custom      | Vendor    | BlueJeans@vartopia.com  |                       |
      | Custom      | Var       | scotsha@cdw.com         |                       |
      | Custom      | Dist      | jerry999@mailinator.com |                       |

  Scenario: Validate Extra Org Filter For Partner Var
    Then Enter username going to login "LenovoMasterAdmin"
    Then Move to "Renewals" module
    And Validation Page URL
    Then Move to Grid View "Default" "Created Last 365 Days"
    And Check Org Filter For Partner

  Scenario: Delete And Validate The Custom Report
    Then Enter username going to login "jerry999@mailinator.com"
    Then Move to "Renewals" module
    And Validation Page URL
    Then Move to Grid View "New Report" ""
    And Delete And Validate The Custom Report
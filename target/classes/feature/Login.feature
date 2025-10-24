Feature: I want to verify login functionality of vartopia page

  Background:
    Given User is on Login Page

  Scenario Outline: Login Vartopia with valid credentials
    Given Login with valid username "<username>" and password "<password>"
    When I click on login button
    Then Vartopia Logo and all tiles should be visible if credentials are correct or an error message should be displayed

    Examples:
      | username                   | password                 |
      | invalid@vartopia.com       |   RegressionQ@Vartopia   |
      | QaAutomation@vartopia.com  |   invalid@Vartopia       |
      | invalid@vartopia.com       |   invalid@Vartopia       |
      | QaAutomation@vartopia.com  |   RegressionQ@Vartopia   |

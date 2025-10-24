Feature: AutoReg Functionality

  Scenario: Create,Submit and Verify an AutoReg form for an opportunity
    Given User is on SFDC Login Page
    When Enter valid SFDC login credentials
    Then Validate Title of SFDC page is "Home Page ~ Salesforce - Developer Edition"
    Then Change to Classic Mode
    When User click on the Opportunity tab
    And  Select an opportunity from the list "AWS AutoReg Opp"
    And User click on the Map to Deal Reg button
    When User click on the Create Registration button
    Then Fill All Mandatory Auto Reg Form Fields
    And Wait Until The Status Is Not Changed To "Approved"
    Given User is on Login Page
    When Enter valid login credentials
    Then Validate Title of page is "Vartopia"
    Then Enter the Vendor Name "Acme Global" and Partner Name "13th Oct 2023 Account (62962)" for Manual Pull
    Then Enter username going to login "nadir@vartopia.mailinator.com"
    Then Move to "Registrations" module
    And Check page URL
    Then Find Opportunity Deal id in Quick search
    And Validate Deal id


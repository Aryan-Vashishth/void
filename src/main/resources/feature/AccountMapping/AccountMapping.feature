#  To verify Quick search:
#  Record Id - User is able to search with record id
#  Account Name - User is able to search with Account name
#  Website - User is able to search with website

#  Default Reports:
#  Open Accounts - User is navigated, Status, verify branding. search, filter.
#  Pending Accounts - User is navigated, Status, verify branding. search, filter.
#  Claimed Accounts - User is navigated, Status, verify branding. search, filter.
#  SmartMatch Accounts - User is navigated, Status, verify branding. search, filter.

#  Custom Reports:
#  Tooltip - correct message
#  Create New - new custom report is showing
#  Already existing - user is able to view old report.

#  Records Grid:
#  Grid Column names and order
#  Grid records and data



Feature: I want to verify functionality of Account Mapping module

  Background:
    Given User is on Login Page
    And Enter valid login credentials
    Then Validate Title of page is "Vartopia"

  Scenario Outline: Verify Account Mapping home page and records page
    Given User login with valid email "<User>"
    And Land on Account Mapping home page
    Then Verified all elements on home page are visible and functional based on user type "<type>"
    And Verified all elements on records page are visible and functional based on user type "<type>"
    And Verify quick search functionality
    When Column names and Records should be visible when I click on default reports based on user type "<type>"
    Then Verify default sorting for records based on user type "<type>"


    Examples:
      | User                               | type    |
      | qaminitest@vartopia.mailinator.com | vendor  |
      | miniso1@vartopia.mailinator.com    | partner |


  Scenario: Verify partner should be able to view the records of their connected (DRS) vendors under Open Accounts, Pending Accounts and Claimed Accounts.
    Given Partner User is Logged in
    When Get Record ID of records under Open Accounts, Pending Accounts and Claimed Accounts from Partner User
    And Then login as a connected DRS Vendor user
    Then Records should exist under Open Accounts, Pending Accounts and Claimed Accounts with respective statuses


  Scenario: Verify the status of any record changes to Pending or Claimed should be only visible to the users of the partner who create the registration not to other partner accounts.
    Given Partner User is Logged in
    When Get Record ID of records under Open Accounts, Pending Accounts and Claimed Accounts from Partner User
    And  login as another Partner user connected to same Vendor
    Then Records should exist under Open Accounts, Pending Accounts, and Claimed Accounts with respective statuses

  Scenario: Verify import records via the Account Mapping page
    Given Vendor User in Logged in
    And Land on Account Mapping home page
    Then Users should be able to upload and submit records via both upload method
    And Copy-paste method





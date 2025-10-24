Feature: Deal Coach Functionlity for Zoom Vendor in Vartopia application

  As a user of Zoom vendor
  User want to see all the resources which are related to zoom solution
  So that user can download the file or zip.

  Background:
    Given User is on Login Page
    When Enter valid login credentials
    Then Validate Title of page is "Vartopia"

  #Reseller Registration
  Scenario: Test1
    Then Enter username going to login "hubtest@vartopia.mailinator.com" move to "Home"
    And Check page URL
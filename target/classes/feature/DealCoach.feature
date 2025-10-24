Feature: Deal Coach Functionlity for Zoom Vendor in Vartopia application

  As a user of Zoom vendor
  User want to see all the resources which are related to zoom solution
  So that user can download the file or zip.

  Background:
    Given User is on Login Page
    When Enter valid login credentials
    Then Validate Title of page is "Vartopia"

  #Reseller Registration
  Scenario: Registration Page - Validate Deal Coach option and all resources based on solution selection when deal coach is ON and Send update is OFF
    Then Enter username going to login ""
    Then Move to "Registrations" module
    And Check page URL
    Then Find Opportunity "Reseller Registration" in Quick search
    And Validate option type "Deal Coach" "true"
    Then Validate Deal Coach Section "true"
    And Validate all the resources "Zoom Meetings,Zoom Rooms"
    Then Download the resources and validate
    And Check SendUpdate button "false"
    And Close the deal coach or send update popup
    Then Assert all soft assertions

  #Reseller Registration
  Scenario: Opportunity Page - Validate Deal Coach option and all resources based on solution selection when deal coach is ON and Send update is OFF
    Then Enter username going to login ""
    Then Move to "Registrations" module
    And Check page URL
    Then Find Opportunity "Reseller Registration" in Quick search
    Then Validate Deal Coach on Opportunity Page
    Then Validate option type "Deal Coach" "true"
    Then Validate Deal Coach Section "true"
    And Validate all the resources "Zoom Meetings,Zoom Rooms"
    Then Download the resources and validate
    And Check SendUpdate button "false"
    And Close the deal coach or send update popup
    Then Assert all soft assertions

#Federal Reseller Registration
  Scenario: Registration Page - Validate Send Update option and all resources based on solution selection when both deal coach and Send update is ON
    Then Enter username going to login ""
    Then Move to "Registrations" module
    And Check page URL
    Then Find Opportunity "Deal Coach 1" in Quick search
    And Validate option type "Send Update" "true"
    Then Validate Deal Coach Section "true"
    And Validate all the resources "Zoom Webinars,Zoom Phone"
    Then Download the resources and validate
    And Check SendUpdate button "true"
    And Close the deal coach or send update popup
    Then Assert all soft assertions

  #Federal Reseller Registration
  Scenario: Opportunity Page - Validate Send Update option and all resources based on solution selection when both deal coach and Send update is ON
    Then Enter username going to login ""
    Then Move to "Registrations" module
    And Check page URL
    Then Find Opportunity "Deal Coach 1" in Quick search
    Then Validate Deal Coach on Opportunity Page
    Then Validate option type "Send Update" "true"
    Then Validate Deal Coach Section "true"
    And Validate all the resources "Zoom Webinars,Zoom Phone"
    Then Download the resources and validate
    And Check SendUpdate button "true"
    And Close the deal coach or send update popup
    Then Assert all soft assertions

    #Master Agent Referral Registration
  Scenario: Registration Page - Validate Deal Coach when deal coach is OFF and Send update is ON
    Then Enter username going to login ""
    Then Move to "Registrations" module
    And Check page URL
    Then Find Opportunity "Deal coach 6" in Quick search
    And Validate option type "Send Update" "true"
    Then Validate Deal Coach Section "false"
    And Check SendUpdate button "true"
    And Close the deal coach or send update popup
    Then Assert all soft assertions

  #Master Agent Referral Registration
  Scenario: Opportunity Page - Validate Deal Coach when deal coach is OFF and Send update is ON
    Then Enter username going to login ""
    Then Move to "Registrations" module
    And Check page URL
    Then Find Opportunity "Deal coach 6" in Quick search
    Then Validate Deal Coach on Opportunity Page
    Then Validate option type "Send Update" "true"
    Then Validate Deal Coach Section "false"
    And Check SendUpdate button "true"
    And Close the deal coach or send update popup
    Then Assert all soft assertions

  #Zoom X Registration
  Scenario: Registration Page - Validate Deal Coach when deal coach is OFF and Send update is OFF
    Then Enter username going to login ""
    Then Move to "Registrations" module
    And Check page URL
    Then Find Opportunity "Deal coach 3" in Quick search
    And Validate option type "Send Update" "false"
    Then Assert all soft assertions

  #Zoom X Registration
  Scenario: Opportunity Page - Validate Deal Coach when deal coach is OFF and Send update is OFF
    Then Enter username going to login ""
    Then Move to "Registrations" module
    And Check page URL
    Then Find Opportunity "Deal coach 3" in Quick search
    Then Validate Deal Coach on Opportunity Page
    Then Validate option type "Send Update" "false"
    Then Assert all soft assertions

  #Federal Reseller Registration
  Scenario: Registration Page - Validate empty screen when no solution is selected in form and when both deal coach and Send update is ON
    Then Enter username going to login ""
    Then Move to "Registrations" module
    And Check page URL
    Then Find Opportunity "Deal Coach 5" in Quick search
    And Validate option type "Send Update" "true"
    Then Validate Deal Coach Solution Categories "false"
    And Check SendUpdate button "true"
    And Close the deal coach or send update popup
    Then Assert all soft assertions

  #Federal Reseller Registration
  Scenario: Opportunity Page - Validate empty screen when no solution is selected in form and when both deal coach and Send update is ON
    Then Enter username going to login ""
    Then Move to "Registrations" module
    And Check page URL
    Then Find Opportunity "Deal Coach 5" in Quick search
    Then Validate Deal Coach on Opportunity Page
    And Validate option type "Send Update" "true"
    Then Validate Deal Coach Solution Categories "false"
    And Check SendUpdate button "true"
    And Close the deal coach or send update popup
    Then Assert all soft assertions

  #Acme vendor
  Scenario: Registration Page - Validate Deal coach resources should not be visible for any vendor except Zoom
    Then Enter username going to login "acmeglobal@vartopia.com"
    Then Move to "Registrations" module
    And Check page URL
    Then Find Opportunity "Deal Coach 7" in Quick search
    And Validate option type "Send Update" "true"
    Then Validate Deal Coach Solution Categories "false"
    And Check SendUpdate button "true"
    And Close the deal coach or send update popup
    Then Assert all soft assertions

  #Acme vendor
  Scenario: Opportunity Page - Validate Deal coach resources should not be visible for any vendor except Zoom
    Then Enter username going to login "acmeglobal@vartopia.com"
    Then Move to "Registrations" module
    And Check page URL
    Then Find Opportunity "Deal Coach 7" in Quick search
    Then Validate Deal Coach on Opportunity Page
    And Validate option type "Send Update" "true"
    Then Validate Deal Coach Solution Categories "false"
    And Check SendUpdate button "true"
    And Close the deal coach or send update popup
    Then Assert all soft assertions
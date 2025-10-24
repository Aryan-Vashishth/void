Feature: Analytics Dashboard is to analysis the deal counts in Vartopia application

  Background:
    Given User is on Login Page
    When Enter valid login credentials
    Then Validate Title of page is "Vartopia"

#  Scenario: This scenario check all the valid steps of Dashboard
#    Then Enter username going to login "support@vartopia.com"
#    Then Move to "Analytics" module
#    And Check page URL
#    Then Set all the value of dashboard
#    Then Move to "Registrations" module
#    Then Move to Grid View "Default" "Submitted Last 365 Days"
#    And Open Advance Filter Popup
#    And Set Advance Filters Values "Status Filter" "Approved, Assigned, Closed - Lost,Closed - Other,Closed - Won,Denied,Exception,Expired,Pending,Recalled,Returned, Submitted"
#    And Click On Search Button To Load The Record
#
#    Then Validate Total Registrations Count
#
#    And Move to Another Report "Default" "Expiring Next 30 Days"
#    Then Validate Registrations Expiring In Next 30 Days
#
#    And Move to Another Report "Default" "Submitted Last 365 Days"
#    And Open Advance Filter Popup
#    And Set Advance Filters Values "Status Filter" "Pending"
#    And Click On Search Button To Load The Record
#    Then Validate Active Registration Count For "Pending"
#    Then Validate Registration Status Percentage "Pending"
#    And Move to Another Report "Default" "Submitted Last 365 Days"
#    And Open Advance Filter Popup
#    And Set Advance Filters Values "Status Filter" "Submitted"
#    And Click On Search Button To Load The Record
#    Then Validate Active Registration Count For "Submitted"
#    Then Validate Registration Status Percentage "Submitted"
#    And Move to Another Report "Default" "Submitted Last 365 Days"
#    And Open Advance Filter Popup
#    And Set Advance Filters Values "Status Filter" "Approved"
#    And Click On Search Button To Load The Record
#    Then Validate Active Registration Count For "Approved"
#    Then Validate Registration Status Percentage "Approved"
#    And Move to Another Report "Default" "Submitted Last 365 Days"
#
#    And Move to Another Report "Default" "Submitted Last 365 Days"
#    And Open Advance Filter Popup
#    And Set Advance Filters Values "Status Filter" "Expired"
#    And Click On Search Button To Load The Record
#    Then Validate Registration Status Percentage "Expired"
#    And Move to Another Report "Default" "Submitted Last 365 Days"
#    And Open Advance Filter Popup
#    And Set Advance Filters Values "Status Filter" "Exception"
#    And Click On Search Button To Load The Record
#    Then Validate Registration Status Percentage "Exception"
#    And Move to Another Report "Default" "Submitted Last 365 Days"
#    And Open Advance Filter Popup
#    And Set Advance Filters Values "Status Filter" "Closed - Won"
#    And Click On Search Button To Load The Record
#    Then Validate Registration Status Percentage "Closed - Won"
#    And Move to Another Report "Default" "Submitted Last 365 Days"
#    And Open Advance Filter Popup
#    And Set Advance Filters Values "Status Filter" "Closed - Other"
#    And Click On Search Button To Load The Record
#    Then Validate Registration Status Percentage "Closed - Other"
#
#    And Move to Another Report "Default" "Submitted Last 365 Days"
#    And Open Advance Filter Popup
#    And Set Advance Filters Values "Status Filter" "Closed - Won"
#    And Click On Search Button To Load The Record
#    Then Validate Partner Performance Closed-Won Registrations
#    Then Assert all soft assertions

  Scenario:This scenario check all the valid steps of Historical Dashboard
    Then Enter username going to login "2x@sofsol.co.nz"
    Then Move to "Analytics" module
    And Check page URL
    And Set all the value of historical dashboard
#    Then Move to "Registrations" module
#    Then Move to Grid View "Default" "Submitted Last 365 Days"
#    And Open Advance Filter Popup
#    And Set Advance Filters Values "Status Filter" "Approved, Assigned, Closed - Lost,Closed - Other,Closed - Won,Denied,Exception,Expired,Pending,Recalled,Returned, Submitted"
#    And Set Days "" Or Dates "" "" For WithIn Type "Submitted"
#    And Click On Search Button To Load The Record
#    And Download the CSV File and fetch count "Submitted On"
#    Then Validate Total Registrations Count Of Historical
#    Then Assert all soft assertions

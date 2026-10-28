package examples.pages.demoqa;

import domain.automation.web.vocabulary.capability.Checkable;
import domain.automation.web.vocabulary.capability.Clickable;
import domain.automation.web.vocabulary.capability.Dropdown;
import domain.automation.web.vocabulary.capability.ParameterizedClickable;
import domain.automation.web.vocabulary.capability.ReadOnly;
import domain.automation.web.vocabulary.capability.Typeable;
import domain.automation.web.vocabulary.capability.Uploadable;

/** Student Registration Form -- https://demoqa.com/automation-practice-form */
public interface PracticeFormPage {

    // ── PersonalDetails ── name, email, mobile, gender radios ────────────────

    interface PersonalDetails {

        enum Fields implements Typeable {
            FIRST_NAME_INPUT,
            LAST_NAME_INPUT,
            EMAIL_INPUT,
            MOBILE_INPUT;       // type="text", pattern="\d*", minlength/maxlength 10
        }

        enum Gender implements Clickable {
            MALE_RADIO,
            FEMALE_RADIO,
            OTHER_RADIO;
        }
    }

    // ── DateOfBirth ── react-datepicker: input + month/year selects + day grid

    interface DateOfBirth {

        enum Fields implements Typeable {
            DATE_OF_BIRTH_INPUT;
        }

        enum Buttons implements Clickable {
            CALENDAR_TOGGLE,    // same input; clicking opens the calendar popup
            YEAR_SELECT;        // native <select>, 1900-2100 -- selected by text, see DemoQATest
        }

        enum Months implements Dropdown {
            JANUARY("January"),
            FEBRUARY("February"),
            MARCH("March"),
            APRIL("April"),
            MAY("May"),
            JUNE("June"),
            JULY("July"),
            AUGUST("August"),
            SEPTEMBER("September"),
            OCTOBER("October"),
            NOVEMBER("November"),
            DECEMBER("December");

            private final String label;

            Months(String label) { this.label = label; }

            @Override
            public Object[] getArgs() { return new Object[]{label}; }
        }

        enum DynamicButtons implements ParameterizedClickable {
            DAY_CELL;           // XPath uses %1$s for the zero-padded day ("015")
        }
    }

    // ── Subjects ── react-select multi-select (autocomplete chips) ───────────

    interface Subjects {

        enum Fields implements Typeable {
            SUBJECTS_INPUT;
        }

        enum Labels implements ReadOnly {
            SELECTED_SUBJECT_CHIPS;     // all chip labels (for allTexts)
        }

        enum DynamicButtons implements ParameterizedClickable {
            REMOVE_SUBJECT_BUTTON;      // XPath uses %1$s for the subject name
        }
    }

    // ── Hobbies ── native checkboxes ─────────────────────────────────────────

    interface Hobbies {

        enum Options implements Checkable {
            SPORTS_CHECKBOX,
            READING_CHECKBOX,
            MUSIC_CHECKBOX;
        }
    }

    // ── Picture ── file input ────────────────────────────────────────────────

    interface Picture {

        enum Fields implements Uploadable {
            PICTURE_UPLOAD;
        }
    }

    // ── Address ── textarea + dependent State -> City react-selects ──────────

    interface Address {

        enum Fields implements Typeable {
            CURRENT_ADDRESS_INPUT,
            STATE_INPUT,        // react-select: type option text + ENTER
            CITY_INPUT;         // disabled until a state is chosen
        }

        enum Labels implements ReadOnly {
            SELECTED_STATE,
            SELECTED_CITY;
        }
    }

    // ── Form ── submit ───────────────────────────────────────────────────────

    enum Form implements Clickable {
        SUBMIT_BUTTON;
    }

    // ── ConfirmationModal ── "Thanks for submitting the form" summary table ──

    interface ConfirmationModal {

        enum Labels implements ReadOnly {
            MODAL_TITLE,
            STUDENT_NAME_VALUE,
            STUDENT_EMAIL_VALUE,
            GENDER_VALUE,
            MOBILE_VALUE,
            DATE_OF_BIRTH_VALUE,
            SUBJECTS_VALUE,
            HOBBIES_VALUE,
            PICTURE_VALUE,
            ADDRESS_VALUE,
            STATE_AND_CITY_VALUE;
        }

        enum Buttons implements Clickable {
            CLOSE_BUTTON;
        }
    }
}

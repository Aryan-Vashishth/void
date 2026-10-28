package examples.pages.demoqa;

import domain.automation.web.vocabulary.capability.Dropdown;
import domain.automation.web.vocabulary.capability.ReadOnly;

/** Select Menu -- https://demoqa.com/select-menu (native single and multi selects) */
public interface SelectMenuPage {

    // ── OldStyleSelect ── native single-value <select id="oldSelectMenu"> ────

    interface OldStyleSelect {

        enum Colors implements Dropdown {
            RED("Red"),
            BLUE("Blue"),
            GREEN("Green"),
            YELLOW("Yellow"),
            PURPLE("Purple"),
            BLACK("Black"),
            WHITE("White"),
            VIOLET("Voilet"),   // option text is misspelled on the site
            INDIGO("Indigo"),
            MAGENTA("Magenta"),
            AQUA("Aqua");

            private final String label;

            Colors(String label) { this.label = label; }

            @Override
            public Object[] getArgs() { return new Object[]{label}; }
        }

        enum Labels implements ReadOnly {
            SELECTED_COLOR;
        }
    }

    // ── CarsMultiSelect ── native <select multiple id="cars"> ────────────────

    interface CarsMultiSelect {

        enum Cars implements Dropdown {
            VOLVO("Volvo"),
            SAAB("Saab"),
            OPEL("Opel"),
            AUDI("Audi");

            private final String label;

            Cars(String label) { this.label = label; }

            @Override
            public Object[] getArgs() { return new Object[]{label}; }
        }

        enum Labels implements ReadOnly {
            SELECTED_CARS;      // all checked options (for allTexts)
        }
    }
}

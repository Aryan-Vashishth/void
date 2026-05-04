package core.elements;

import elements.api.capability.Clickable;
import elements.api.capability.Selectable;
import elements.api.capability.Typeable;

public class DemoPageElements {
    public enum Login implements Typeable {
        USERNAME("username"),
        PASSWORD("password");


        private final String label;

        Login(String label) {
            this.label = label;
        }

        @Override
        public String getInputLocator() {
            return "xpath=//input[@name='%s']";
        }

        @Override
        public String getExternalFileName() {
            return null;
        }

        @Override
        public Object[] getArgs() {
            return new Object[]{label};
        }
        public enum LoginButton implements Clickable {
            SUBMIT("submit");

            private final String label;

            LoginButton(String label) {
                this.label = label;
            }

            @Override
            public String getTriggerLocator() {
                return "xpath=//button[@id='" + label + "']";
            }

            @Override
            public String getExternalFileName() {
                return null;
            }

            @Override
            public Object[] getArgs() {
                return new Object[]{label};
            }
        }
    }
    public enum NavBar implements Selectable {
        PARTNER("Partner"),
        VENDOR("Vendor"),;

        private final String label; NavBar(String k){this.label=k;}

        @Override
        public String getTriggerLocator() {
            return "USER_TYPE_TRIGGER";
        }

        @Override
        public String getListLocator() {
            return "USER_TYPE_LIST ";
        }

        @Override
        public String getExternalFileName() {
            return "manage-users-elements.properties";
        }

        @Override
        public Object[] getArgs() {
            return new Object[]{label};
        }
    }
}

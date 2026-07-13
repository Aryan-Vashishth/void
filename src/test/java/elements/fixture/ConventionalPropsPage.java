package elements.fixture;

import elements.api.capability.Typeable;

public interface ConventionalPropsPage {

    enum Fields implements Typeable {
        EMAIL_INPUT,
        PHONE_INPUT;
    }
}

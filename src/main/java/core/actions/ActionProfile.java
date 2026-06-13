package core.actions;

import core.interactions.hooks.AfterActionHandler;
import core.interactions.hooks.BeforeActionHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Named hook bundle that can be applied to an {@link Action}.
 */
public interface ActionProfile {

    /**
     * Baseline before-hooks for this profile.
     */
    default List<BeforeActionHandler> before() {
        return List.of();
    }

    /**
     * Baseline after-hooks for this profile.
     */
    default List<AfterActionHandler> after() {
        return List.of();
    }

    /**
     * Capability-aware before-hooks (falls back to {@link #before()}).
     */
    default List<BeforeActionHandler> before(Action action) {
        return before();
    }

    /**
     * Capability-aware after-hooks (falls back to {@link #after()}).
     */
    default List<AfterActionHandler> after(Action action) {
        return after();
    }

    /**
     * Fluent builder for custom profiles.
     */
    static Builder builder() {
        return new Builder();
    }

    final class Builder {
        private final List<BeforeActionHandler> before = new ArrayList<>();
        private final List<AfterActionHandler> after = new ArrayList<>();

        public Builder before(BeforeActionHandler hook) {
            if (hook != null) {
                before.add(hook);
            }
            return this;
        }

        public Builder before(BeforeActionHandler... hooks) {
            if (hooks != null) {
                for (BeforeActionHandler hook : hooks) {
                    before(hook);
                }
            }
            return this;
        }

        public Builder after(AfterActionHandler hook) {
            if (hook != null) {
                after.add(hook);
            }
            return this;
        }

        public Builder after(AfterActionHandler... hooks) {
            if (hooks != null) {
                for (AfterActionHandler hook : hooks) {
                    after(hook);
                }
            }
            return this;
        }

        public ActionProfile build() {
            List<BeforeActionHandler> frozenBefore = before.isEmpty() ? List.of() : List.copyOf(before);
            List<AfterActionHandler> frozenAfter = after.isEmpty() ? List.of() : List.copyOf(after);
            return new ActionProfile() {
                @Override
                public List<BeforeActionHandler> before() {
                    return frozenBefore;
                }

                @Override
                public List<AfterActionHandler> after() {
                    return frozenAfter;
                }
            };
        }
    }
}


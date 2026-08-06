/**
 * VOID Framework — Runnable Demos and Examples.
 *
 * <p>This package contains self-contained demo applications that exercise
 * the full VOID pipeline (Action / Flow / FlowExecutor) against public demo sites.</p>
 *
 * <h3>Structure</h3>
 * <pre>
 *   tests.demo/
 *   ├── VoidDemo.java              ← Entry point: bootstraps VOID, runs login flow
 *   └── pages/
 *       ├── DemoLoginElements.java ← Simple example (Typeable, Clickable, ReadOnly)
 *       └── ManageUsersElements.java ← ResolvableEnum example
 * </pre>
 *
 * <h3>Running</h3>
 * <pre>
 *   // IDE: Run VoidDemo as a TestNG test
 *   // CLI: mvn test -Dtest=tests.demo.VoidDemo
 * </pre>
 *
 * @see examples.tests.VoidDemo
 * @see examples.pages.DemoLoginPage
 */
package examples;


package core.resolvers.locator.integration;

import domain.automation.web.resolve.api.LocatorResolvers;
import domain.automation.web.vocabulary.capability.Clickable;
import org.openqa.selenium.By;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * End-to-end integration test covering all supported locator strategies through
 * the full pipeline: contract -> locators.json -> LocatorResolver -> correct By type.
 *
 * <p>Strategies covered: xpath (inferred via //), xpath=, css=, id=, name=, class=, tag=</p>
 */
@Test(groups = {"integration"})
public class AllLocatorStrategiesIntegrationTest {

    interface StrategyFixturePage {
        enum Elements implements Clickable {
            XPATH_INFERRED,
            XPATH_EXPLICIT,
            CSS_ELEMENT,
            ID_ELEMENT,
            NAME_ELEMENT,
            CLASS_ELEMENT,
            TAG_ELEMENT;
        }
    }

    @Test
    public void resolve_xpathInferred_returnsXpathBy() {
        By by = LocatorResolvers.strict().resolve(StrategyFixturePage.Elements.XPATH_INFERRED);
        assertEquals(by.toString(), By.xpath("//button[@id='xpath-btn']").toString());
    }

    @Test
    public void resolve_xpathExplicit_returnsXpathBy() {
        By by = LocatorResolvers.strict().resolve(StrategyFixturePage.Elements.XPATH_EXPLICIT);
        assertEquals(by.toString(), By.xpath("//button[@id='xpath-explicit-btn']").toString());
    }

    @Test
    public void resolve_cssPrefix_returnsCssSelectorBy() {
        By by = LocatorResolvers.strict().resolve(StrategyFixturePage.Elements.CSS_ELEMENT);
        assertEquals(by.toString(), By.cssSelector("button.css-btn").toString());
    }

    @Test
    public void resolve_idPrefix_returnsIdBy() {
        By by = LocatorResolvers.strict().resolve(StrategyFixturePage.Elements.ID_ELEMENT);
        assertEquals(by.toString(), By.id("id-btn").toString());
    }

    @Test
    public void resolve_namePrefix_returnsNameBy() {
        By by = LocatorResolvers.strict().resolve(StrategyFixturePage.Elements.NAME_ELEMENT);
        assertEquals(by.toString(), By.name("name-btn").toString());
    }

    @Test
    public void resolve_classPrefix_returnsClassNameBy() {
        By by = LocatorResolvers.strict().resolve(StrategyFixturePage.Elements.CLASS_ELEMENT);
        assertEquals(by.toString(), By.className("btn").toString());
    }

    @Test
    public void resolve_tagPrefix_returnsTagNameBy() {
        By by = LocatorResolvers.strict().resolve(StrategyFixturePage.Elements.TAG_ELEMENT);
        assertEquals(by.toString(), By.tagName("button").toString());
    }
}

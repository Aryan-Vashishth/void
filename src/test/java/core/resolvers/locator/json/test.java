package core.resolvers.locator.json;

import core.elements.DemoPageElements;
import elements.AccountMappingElements;
import elements.ManageUsersElements;

import java.nio.file.Path;

import static core.resolvers.locator.json.JsonLocatorMigrator.buildResolvedJson;
import static core.logging.CustomLogger.*;

public class test {
    public static void main(String[] args) {
        Class<AccountMappingElements> root = AccountMappingElements.class;
        debug.log("[run] start root=" + root.getSimpleName());
        String json = buildResolvedJson(root);
        info.log(json);
        Path out = JsonLocatorMigrator.writeResolvedJson(root);
        debug.log("[run] output=" + out.toAbsolutePath() + " bytes=" + json.length());
    }

}

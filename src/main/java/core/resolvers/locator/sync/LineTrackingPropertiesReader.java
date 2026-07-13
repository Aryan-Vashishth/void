package core.resolvers.locator.sync;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Loads a {@code .properties} file while recording the 1-based line number of every key.
 * Standard {@link Properties#load} does not expose line numbers; this class fills the gap
 * so that validation errors can cite the exact line in the file.
 */
final class LineTrackingPropertiesReader {

    private final Map<String, Integer> lineNumbers = new LinkedHashMap<>();
    private final Properties properties = new Properties();

    void load(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        for (int i = 0; i < lines.size(); i++) {
            String trimmed = lines.get(i).trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            int eq = trimmed.indexOf('=');
            if (eq < 0) continue;
            String key   = trimmed.substring(0, eq).trim();
            String value = trimmed.substring(eq + 1).trim();
            if (key.isEmpty()) continue;
            lineNumbers.put(key, i + 1);
            properties.setProperty(key, value);
        }
    }

    boolean isEmpty()              { return lineNumbers.isEmpty(); }
    boolean contains(String key)   { return lineNumbers.containsKey(key); }
    int  getLineNumber(String key) { return lineNumbers.getOrDefault(key, -1); }
    String getValue(String key)    { return properties.getProperty(key); }
    Properties getProperties()     { return properties; }

    Map<String, Integer> allLineNumbers() {
        return Collections.unmodifiableMap(lineNumbers);
    }
}

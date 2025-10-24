package core.utils;

import Elements.*;
import Elements.Interfaces.ResolvableEnum;
import com.beust.jcommander.internal.Nullable;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import core.logging.CustomLogger;

import java.util.Arrays;
import java.util.List;

public class EnumResolver extends BaseUtils {

    private static final List<Class<?>> ENUM_CONTAINERS = Arrays.asList(
            CommonElements.class,
            AccountMappingElements.class,
            InfoElements.class,
            AdminHomeElements.class,
            ManageUsersElements.class
            // add more as needed
    );

    public EnumResolver(){
        initializer();
    }

    public static void printEnumFormat(By locator){
        List<WebElement> elements = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
        for(WebElement element: elements){
            String enumFormated = normalizeToEnumName(element.getText().trim());
            System.out.println((enumFormated + "(\"" + element.getText() + "\"),"));
        }
    }

    /**
     * Normalizes a string to standard Java enum constant format.
     * Example: "HQ State/Province" => "HQ_STATE_PROVINCE"
     */
    public static String normalizeToEnumName(String label) {
        return label.trim()
                .replaceAll("[^A-Za-z0-9]", "_") // Replace non-alphanumeric with underscore
                .replaceAll("_+", "_")           // Collapse multiple underscores
                .replaceAll("^_|_$", "")         // Remove leading/trailing underscores
                .toUpperCase();
    }

    private static <T extends Enum<T>> T resolveEnumConstant(Class<T> enumClass, String input, String normalized) {
        T[] constants = enumClass.getEnumConstants();

        if (constants == null) {
            throw new IllegalArgumentException("No constants found in enum: " +
                    enumClass.getSimpleName());
        }

        for (T constant : constants) {
            CustomLogger.debug.log("Checking: " + constant.name());
            if (constant.name().equalsIgnoreCase(normalized)) {
                CustomLogger.debug.log("Found enum constant: " + constant.name() +
                        " in " + enumClass.getSimpleName());
                return constant;
            }
        }

        String available = Arrays.toString(Arrays.stream(constants).map(Enum::name).toArray());
        throw new IllegalArgumentException("No matching constant for value: '" + input + "' "
                + "(normalized: '" + normalized + "') in enum: " + enumClass.getSimpleName()
                + ". Available values: " + available);
    }

    public static <T extends Enum<T> & ResolvableEnum> T stringToEnum(Class<T> enumClass, String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Input string is null or empty.");
        }

        if (!ResolvableEnum.class.isAssignableFrom(enumClass)) {
            warn.log("Enum class '" + enumClass.getSimpleName() +
                    "' does NOT implement ResolvableEnum (but was passed to stringToEnum).");
        }

        String normalized = normalizeToEnumName(input);
        CustomLogger.debug.log("Resolving enum constant '" + input + "' (normalized: " + normalized + ")"
                + " in enum class: " + enumClass.getName());

        // First, try by normalized enum constant name
        try {
            return resolveEnumConstant(enumClass, input, normalized);
        } catch (Exception ignored) {}

        // Fallback: try by label (case-insensitive)
        T[] constants = enumClass.getEnumConstants();
        for (T constant : constants) {
            if (input.equalsIgnoreCase(constant.getLabel())) {
                CustomLogger.debug.log("Found enum by label: " + constant.name() +
                        " in " + enumClass.getSimpleName());
                return constant;
            }
        }

        String available = Arrays.toString(Arrays.stream(constants).map(Enum::name).toArray());
        throw new IllegalArgumentException("No matching enum constant or label for value: '" + input + "' "
                + "(normalized: '" + normalized + "') in enum: " + enumClass.getSimpleName()
                + ". Available values: " + available);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Enum<T> & ResolvableEnum> T stringToEnum(String enumSimpleName, String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Input string is null or empty.");
        }

        String normalized = normalizeToEnumName(input);

        try {
            for (Class<?> outer : ENUM_CONTAINERS) {
                for (Class<?> inner : outer.getDeclaredClasses()) {
                    if (inner.isEnum()
                            && ResolvableEnum.class.isAssignableFrom(inner)
                            && inner.getSimpleName().equalsIgnoreCase(enumSimpleName)) {

                        // First, try by normalized enum constant name
                        try {
                            return resolveEnumConstant((Class<T>) inner, input, normalized);
                        } catch (Exception ignored) {}

                        // Fallback: try by label
                        T[] constants = ((Class<T>) inner).getEnumConstants();
                        for (T constant : constants) {
                            if (input.equalsIgnoreCase(constant.getLabel())) {
                                CustomLogger.debug.log("Found enum by label: " + constant.name() +
                                        " in " + inner.getSimpleName());
                                return constant;
                            }
                        }

                        String available = Arrays.toString(Arrays.stream(constants).map(Enum::name).toArray());
                        throw new IllegalArgumentException("No matching enum constant or label for value: '" + input + "' "
                                + "(normalized: '" + normalized + "') in enum: " + inner.getSimpleName()
                                + ". Available values: " + available);
                    }
                }
            }

            throw new IllegalArgumentException("Enum type not found: "
                    + enumSimpleName);

        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve enum '" + enumSimpleName +
                    "' from input '" + input + "'", e);
        }
    }


    public static String enumToString(ResolvableEnum e, @Nullable String[] prefixes, @Nullable String[] suffixes) {
        if (e == null) return null;

        try {
            String raw = getEnumName(e, prefixes, suffixes);

            // Format: lowercase, replace underscores, capitalize first letter
            String name = raw.toLowerCase().replace("_", " ");
            return Character.toUpperCase(name.charAt(0)) + name.substring(1);

        } catch (Exception ex) {
            throw new RuntimeException("Failed to convert enum to string for: " + e, ex);
        }
    }

    private static String getEnumName(ResolvableEnum e, String[] prefixes, String[] suffixes) {
        String raw = e.getName();

        if(prefixes !=null) {
            for (String prefix : prefixes) {
                if (raw.startsWith(prefix)) {
                    raw = raw.substring(prefix.length());
                    break;
                }
            }
        }else {
            debug.log("Prefix is Null");
        }
        if(suffixes !=null) {
            for (String suffix : suffixes) {
                if (raw.endsWith(suffix)) {
                    raw = raw.substring(0, raw.length() - suffix.length());
                    break;
                }
            }
        }else {
            debug.log("Suffix is Null");
        }
        return raw;
    }
}

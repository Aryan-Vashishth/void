package core.utils;

import com.beust.jcommander.internal.Nullable;
import org.testng.Assert;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DataVerifier provides reusable comparison utilities for:
 * <ul>
 *   <li>Maps (field → value) with normalization and alias/correction</li>
 *   <li>Lists of Maps (row-wise tables)</li>
 *   <li>Lists of Strings (order-sensitive diagnostics)</li>
 *   <li>Page/breadcrumb verification</li>
 *   <li>Manage Users tile field verification</li>
 * </ul>
 * Logging is performed via {@link BaseUtils}.
 */
public class DataVerifier extends BaseUtils {

    /** Initializes BaseUtils (logger, wait, etc.) using the current DriverContext. */
    public DataVerifier() { initializer(); }

    /** Static comparator helpers. */
    public static class compare {

        // =====================================================================================
        // ================================ LIST OF MAPS API ==================================
        // =====================================================================================

        /**
         * MASTER: Compare two lists of row maps (row-by-row).
         * <p>Modes (flag behavior):
         * <ul>
         *   <li>both flags false → strict (bidirectional), extras on either side fail</li>
         *   <li>onlyCompareKeysInActual=true → actual-driven (expected extras ignored)</li>
         *   <li>onlyCompareKeysInExpected=true → expected-driven (actual extras ignored)</li>
         *   <li>both flags true → intersection only (overlapping keys)</li>
         * </ul>
         *
         * @param expectedRows             list of expected row maps
         * @param actualRows               list of actual row maps
         * @param ignoreKeys               keys to ignore (optional; normalized before matching)
         * @param keyCorrectionMap         alias mapping for keys (optional; applied pre-normalization)
         * @param onlyCompareKeysInActual  compare only keys that exist in each actual row
         * @param onlyCompareKeysInExpected compare only keys that exist in each expected row
         * @return true if all compared rows match under the selected mode; false otherwise
         */
        public static boolean listOfMaps(List<Map<String, String>> expectedRows,
                                         List<Map<String, String>> actualRows,
                                         @Nullable List<String> ignoreKeys,
                                         @Nullable Map<String, String> keyCorrectionMap,
                                         boolean onlyCompareKeysInActual,
                                         boolean onlyCompareKeysInExpected) {
            try {
                info.log("Expected Rows: " + expectedRows);
                info.log("Actual Rows: " + actualRows);

                boolean allMatched = true;

                if (!expectedRows.isEmpty() && actualRows.isEmpty()) {
                    warn.log("[NO ACTUAL DATA] There are expected rows but NO actual rows present!");
                    return false;
                }

                int compareCount = Math.min(expectedRows.size(), actualRows.size());
                for (int i = 0; i < compareCount; i++) {
                    Map<String, String> expectedRow = expectedRows.get(i);
                    Map<String, String> actualRow = actualRows.get(i);

                    info.log("---- Comparing Row " + (i + 1) + " ----");
                    boolean rowMatch = maps(expectedRow, actualRow, ignoreKeys, keyCorrectionMap,
                            onlyCompareKeysInActual, onlyCompareKeysInExpected);

                    if (!rowMatch) {
                        warn.log("[ROW " + (i + 1) + " MISMATCH]");
                        allMatched = false;
                    } else {
                        info.log("[ROW " + (i + 1) + " MATCH]");
                    }
                }

                if (!allMatched) {
                    info.table(expectedRows, "Expected Rows");
                    info.table(actualRows, "Actual Rows");
                }

                return allMatched;
            } catch (Exception e) {
                error.log("Error comparing list of maps: " + e, e);
                return false;
            }
        }

        // ---------------------------- Overloads: listOfMaps ---------------------------------

        /** Strict (bidirectional) compare; no ignores, no alias map. */
        public static boolean listOfMaps(List<Map<String, String>> expectedRows,
                                         List<Map<String, String>> actualRows) {
            return listOfMaps(expectedRows, actualRows, null, null, false, false);
        }

        /** Strict compare with ignore keys. */
        public static boolean listOfMaps(List<Map<String, String>> expectedRows,
                                         List<Map<String, String>> actualRows,
                                         @Nullable List<String> ignoreKeys) {
            return listOfMaps(expectedRows, actualRows, ignoreKeys, null, false, false);
        }

        /** Strict compare with ignore keys + alias map. */
        public static boolean listOfMaps(List<Map<String, String>> expectedRows,
                                         List<Map<String, String>> actualRows,
                                         @Nullable List<String> ignoreKeys,
                                         @Nullable Map<String, String> keyCorrectionMap) {
            return listOfMaps(expectedRows, actualRows, ignoreKeys, keyCorrectionMap, false, false);
        }

        /** Compare using flags only (no ignores, no alias map). */
        public static boolean listOfMaps(List<Map<String, String>> expectedRows,
                                         List<Map<String, String>> actualRows,
                                         boolean onlyCompareKeysInActual,
                                         boolean onlyCompareKeysInExpected) {
            return listOfMaps(expectedRows, actualRows, null, null, onlyCompareKeysInActual, onlyCompareKeysInExpected);
        }

        /** Compare with ignore keys + flags. */
        public static boolean listOfMaps(List<Map<String, String>> expectedRows,
                                         List<Map<String, String>> actualRows,
                                         @Nullable List<String> ignoreKeys,
                                         boolean onlyCompareKeysInActual,
                                         boolean onlyCompareKeysInExpected) {
            return listOfMaps(expectedRows, actualRows, ignoreKeys, null, onlyCompareKeysInActual, onlyCompareKeysInExpected);
        }

        // =====================================================================================
        // =================================== MAPS API =======================================
        // =====================================================================================

        /**
         * MASTER: Compare two maps with normalization/correction.
         * Modes:
         * <ul>
         *   <li>both flags false → strict (bidirectional)</li>
         *   <li>onlyCompareKeysInActual=true → actual-driven</li>
         *   <li>onlyCompareKeysInExpected=true → expected-driven</li>
         *   <li>both flags true → intersection only</li>
         * </ul>
         *
         * @param expectedData              expected key → value map
         * @param actualData                actual key → value map
         * @param ignoreKeys                keys to ignore (nullable)
         * @param keyCorrectionMap          alias mapping (nullable)
         * @param onlyCompareKeysInActual   compare only keys present in {@code actualData}
         * @param onlyCompareKeysInExpected compare only keys present in {@code expectedData}
         * @return true if maps match under selected mode; false otherwise
         */
        public static boolean maps(Map<String, String> expectedData,
                                   Map<String, String> actualData,
                                   @Nullable List<String> ignoreKeys,
                                   @Nullable Map<String, String> keyCorrectionMap,
                                   boolean onlyCompareKeysInActual,
                                   boolean onlyCompareKeysInExpected) {
            try {
                info.table(expectedData, "Expected Map");
                info.table(actualData, "Actual Map");

                boolean isEqual = true;

                // === INTERSECTION MODE ===
                if (onlyCompareKeysInActual && onlyCompareKeysInExpected) {
                    for (String actualKey : actualData.keySet()) {
                        if (isKeyIgnored(actualKey, ignoreKeys)) continue;

                        String matchingExpectedKey = findMatchingKey(actualKey, expectedData, keyCorrectionMap);
                        if (matchingExpectedKey == null) continue; // not overlapping

                        String expVal = normalizeValue(expectedData.get(matchingExpectedKey));
                        String actVal = normalizeValue(actualData.get(actualKey));

                        if (!expVal.equalsIgnoreCase(actVal)) {
                            isEqual = false;
                            warn.log("[MISMATCH] Key: '" + actualKey + "' ∩ '" + matchingExpectedKey +
                                    "' → Expected: " + expVal + ", Actual: " + actVal);
                        } else {
                            info.success("[MATCH] " + actualKey + " = " + actVal);
                        }
                    }
                    return isEqual;
                }

                // === ACTUAL-DRIVEN MODE ===
                if (onlyCompareKeysInActual) {
                    for (String actualKey : actualData.keySet()) {
                        if (isKeyIgnored(actualKey, ignoreKeys)) continue;

                        String matchingExpectedKey = findMatchingKey(actualKey, expectedData, keyCorrectionMap);
                        String expectedValue = (matchingExpectedKey != null)
                                ? normalizeValue(expectedData.get(matchingExpectedKey))
                                : "(missing)";
                        String actualValue = normalizeValue(actualData.get(actualKey));

                        if (!expectedValue.equalsIgnoreCase(actualValue)) {
                            isEqual = false;
                            if ("(missing)".equals(expectedValue)) {
                                error.log("[MISSING IN EXPECTED] Key: '" + actualKey + "' not found in expected. Actual: " + actualValue);
                            } else {
                                warn.log("[MISMATCH] Key: '" + actualKey + "' → Expected: " + expectedValue + ", Actual: " + actualValue);
                            }
                        } else {
                            info.success("[MATCH] " + actualKey + " = " + actualValue);
                        }
                    }
                    return isEqual;
                }

                // === EXPECTED-DRIVEN MODE ===
                if (onlyCompareKeysInExpected) {
                    for (Map.Entry<String, String> expectedEntry : expectedData.entrySet()) {
                        String expectedKey = expectedEntry.getKey();
                        if (isKeyIgnored(expectedKey, ignoreKeys)) continue;

                        String matchingActualKey = findMatchingKey(expectedKey, actualData, keyCorrectionMap);
                        String expectedValue = normalizeValue(expectedEntry.getValue());
                        String actualValue = (matchingActualKey != null)
                                ? normalizeValue(actualData.get(matchingActualKey))
                                : "(missing)";

                        if (!expectedValue.equalsIgnoreCase(actualValue)) {
                            isEqual = false;
                            if ("(missing)".equals(actualValue)) {
                                error.log("[MISSING IN ACTUAL] Key: '" + expectedKey + "' not found in actual. Expected: " + expectedValue);
                            } else {
                                warn.log("[MISMATCH] Key: '" + expectedKey + "' → Expected: " + expectedValue + ", Actual: " + actualValue);
                            }
                        } else {
                            info.success("[MATCH] " + expectedKey + " = " + expectedValue);
                        }
                    }
                    return isEqual;
                }

                // === STRICT BIDIRECTIONAL MODE ===
                // 1) expected → actual
                for (Map.Entry<String, String> expectedEntry : expectedData.entrySet()) {
                    String expectedKey = expectedEntry.getKey();
                    if (isKeyIgnored(expectedKey, ignoreKeys)) continue;

                    String matchingActualKey = findMatchingKey(expectedKey, actualData, keyCorrectionMap);
                    String expectedValue = normalizeValue(expectedEntry.getValue());
                    String actualValue = (matchingActualKey != null)
                            ? normalizeValue(actualData.get(matchingActualKey))
                            : "(missing)";

                    if (!expectedValue.equalsIgnoreCase(actualValue)) {
                        isEqual = false;
                        if ("(missing)".equals(actualValue)) {
                            error.log("[MISSING] Key: '" + expectedKey + "' missing in actual. Expected: " + expectedValue);
                        } else {
                            warn.log("[MISMATCH] Key: '" + expectedKey + "' → Expected: " + expectedValue + ", Actual: " + actualValue);
                        }
                    } else {
                        info.success("[MATCH] " + expectedKey + " = " + expectedValue);
                    }
                }

                // 2) actual → expected (extras are errors)
                for (String actualKey : actualData.keySet()) {
                    if (isKeyIgnored(actualKey, ignoreKeys)) continue;

                    String matchingExpectedKey = findMatchingKey(actualKey, expectedData, keyCorrectionMap);
                    if (matchingExpectedKey == null) {
                        isEqual = false;
                        warn.log("[UNEXPECTED] Extra key in actual: '" + actualKey + "' = " + actualData.get(actualKey));
                    }
                }

                return isEqual;
            } catch (Exception e) {
                error.log("Error comparing maps: " + e);
                return false;
            }
        }

        // ------------------------------- Overloads: maps ------------------------------------

        /** Strict compare; no ignores, no alias map. */
        public static boolean maps(Map<String, String> expectedData,
                                   Map<String, String> actualData) {
            return maps(expectedData, actualData, null, null, false, false);
        }

        /** Strict compare with ignore keys. */
        public static boolean maps(Map<String, String> expectedData,
                                   Map<String, String> actualData,
                                   @Nullable List<String> ignoreKeys) {
            return maps(expectedData, actualData, ignoreKeys, null, false, false);
        }

        /** Strict compare with ignore keys + alias map. */
        public static boolean maps(Map<String, String> expectedData,
                                   Map<String, String> actualData,
                                   @Nullable List<String> ignoreKeys,
                                   @Nullable Map<String, String> keyCorrectionMap) {
            return maps(expectedData, actualData, ignoreKeys, keyCorrectionMap, false, false);
        }

        /** Compare using flags only (no ignores, no alias map). */
        public static boolean maps(Map<String, String> expectedData,
                                   Map<String, String> actualData,
                                   boolean onlyCompareKeysInActual,
                                   boolean onlyCompareKeysInExpected) {
            return maps(expectedData, actualData, null, null, onlyCompareKeysInActual, onlyCompareKeysInExpected);
        }

        /** Compare with ignore keys + flags (no alias map). */
        public static boolean maps(Map<String, String> expectedData,
                                   Map<String, String> actualData,
                                   @Nullable List<String> ignoreKeys,
                                   boolean onlyCompareKeysInActual,
                                   boolean onlyCompareKeysInExpected) {
            return maps(expectedData, actualData, ignoreKeys, null, onlyCompareKeysInActual, onlyCompareKeysInExpected);
        }

        // =====================================================================================
        // ================================= Helper methods ===================================
        // =====================================================================================

        /**
         * Attempts to find a key in {@code targetMap} corresponding to {@code sourceKey}
         * after alias correction and normalization.
         *
         * @param sourceKey        original key to match
         * @param targetMap        map to search
         * @param keyCorrectionMap alias mapping (nullable)
         * @return original matching key from targetMap, or null if not found
         */
        private static String findMatchingKey(String sourceKey,
                                              Map<String, String> targetMap,
                                              @Nullable Map<String, String> keyCorrectionMap) {
            String normSource = normalizeKey(applyKeyCorrection(sourceKey, keyCorrectionMap));
            for (String targetKey : targetMap.keySet()) {
                String normTarget = normalizeKey(applyKeyCorrection(targetKey, keyCorrectionMap));
                if (normSource.equals(normTarget)) return targetKey;
            }
            return null;
        }

        /** Apply alias/correction if available. */
        private static String applyKeyCorrection(String originalKey, @Nullable Map<String, String> keyCorrectionMap) {
            if (keyCorrectionMap == null || !keyCorrectionMap.containsKey(originalKey)) return originalKey;
            return keyCorrectionMap.get(originalKey);
        }

        /** Return true if key should be ignored (comparison uses normalized key). */
        private static boolean isKeyIgnored(String key, @Nullable List<String> ignoreKeys) {
            if (ignoreKeys == null) return false;
            String normKey = normalizeKey(key);
            return ignoreKeys.stream()
                    .map(compare::normalizeKey)
                    .filter(s -> !s.isEmpty())
                    .anyMatch(normKey::contains);
        }

        /** Normalize a key: remove non-alphanumerics and lowercase. */
        private static String normalizeKey(String key) {
            return key == null ? "" : key.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        }

        /** Normalize a value: trim, remove currency separators/commas, collapse ".00" if integer-like. */
        private static String normalizeValue(String value) {
            if (value == null) return "";
            String cleaned = value.replaceAll("[$,]", "").trim();
            if (cleaned.matches("\\d+\\.00")) {
                cleaned = cleaned.substring(0, cleaned.indexOf("."));
            }
            return cleaned;
        }

        // =====================================================================================
        // ============================= Other existing utilities ==============================
        // =====================================================================================

        /**
         * Order-sensitive comparison of two lists of strings with diagnostics.
         * @return true if lists are exactly the same in the same order; false otherwise
         */
        public static boolean lists(List<String> expectedList, List<String> actualList) {
            try {
                info.log("Expected List: " + expectedList);
                info.log("Actual List: " + actualList);

                if (expectedList.equals(actualList)) {
                    info.log("Lists are identical and in the same order.");
                    return true;
                }

                warn.log("Lists are not identical.");

                List<AbstractMap.SimpleEntry<String, String>> elementStatus = new ArrayList<>();
                int maxSize = Math.max(expectedList.size(), actualList.size());

                for (int i = 0; i < maxSize; i++) {
                    String expectedElement = (i < expectedList.size()) ? expectedList.get(i) : "N/A";
                    String actualElement = (i < actualList.size()) ? actualList.get(i) : "N/A";

                    if (expectedElement.equals(actualElement)) {
                        elementStatus.add(new AbstractMap.SimpleEntry<>(expectedElement, "Correct Order"));
                    } else if (expectedList.contains(actualElement) && actualList.contains(expectedElement)) {
                        elementStatus.add(new AbstractMap.SimpleEntry<>(expectedElement + " | " + actualElement, "Wrong Order"));
                    } else {
                        if (!actualList.contains(expectedElement) && !"N/A".equals(expectedElement)) {
                            elementStatus.add(new AbstractMap.SimpleEntry<>(expectedElement, "Missing"));
                        }
                        if (!expectedList.contains(actualElement) && !"N/A".equals(actualElement)) {
                            elementStatus.add(new AbstractMap.SimpleEntry<>(actualElement, "Unexpected"));
                        }
                    }
                }

                // duplicates
                for (int i = 0; i < expectedList.size(); i++) {
                    String element = expectedList.get(i);
                    if (expectedList.indexOf(element) != i) {
                        elementStatus.add(new AbstractMap.SimpleEntry<>(element, "Duplicate Expected"));
                    }
                }
                for (int i = 0; i < actualList.size(); i++) {
                    String element = actualList.get(i);
                    if (actualList.indexOf(element) != i) {
                        elementStatus.add(new AbstractMap.SimpleEntry<>(element, "Duplicate Actual"));
                    }
                }

                for (AbstractMap.SimpleEntry<String, String> entry : elementStatus) {
                    switch (entry.getValue()) {
                        case "Correct Order" -> info.log("Correct Order: " + entry.getKey());
                        case "Wrong Order" -> warn.log("Wrong Order: " + entry.getKey());
                        case "Missing" -> error.log("Missing element: " + entry.getKey());
                        case "Unexpected" -> error.log("Unexpected element: " + entry.getKey());
                        case "Duplicate Expected" -> warn.log("Duplicate in expected: " + entry.getKey());
                        case "Duplicate Actual" -> warn.log("Duplicate in actual: " + entry.getKey());
                    }
                }

                return false;
            } catch (Exception e) {
                error.log("Error comparing lists." + e);
                return false;
            }
        }
    }

    // =========================================================================================
    // ============================= Page / Manage Users helpers ===============================
    // =========================================================================================

    public static class VerifyCurrentPage {
        /** Verify current page via breadcrumb. */
        public static boolean nameViaBreadcrumb(String expectedPage, Boolean isContainsEnabled) {
            String actualPage = Info.getCurrentPageViaBreadcrumb();

            if (expectedPage == null || actualPage == null) {
                error.breadcrumb("Cannot validate page: expected or actual breadcrumb is null.");
                return false;
            }

            if (actualPage.contains(expectedPage) && isContainsEnabled) {
                info.breadcrumb("BreadCrumb contains '" + actualPage + "', Expected: '" + expectedPage + "', isContainsEnabled: true");
                return true;
            } else if (actualPage.contains(expectedPage)) {
                warn.breadcrumb("BreadCrumb contains '" + actualPage + "', Expected: '" + expectedPage + "', isContainsEnabled: false");
                return false;
            } else if (!actualPage.equalsIgnoreCase(expectedPage)) {
                debug.breadcrumb("Expected: '" + expectedPage + "', Found: '" + actualPage + "', isContainsEnabled: " + isContainsEnabled);
                return false;
            } else {
                info.breadcrumb("Landed on expected page: " + expectedPage + " | isContainsEnabled: " + isContainsEnabled);
                return true;
            }
        }
    }

    public static class VerifyManageUsers {
        /** Hard-assert each expected field on Manage Users tile. */
        public static void hasUserInfo(String identifier, Map<String, String> expected) {
            Map<String, String> actual = Info.ManageUsersInfo.getUserInfo(identifier);
            for (Map.Entry<String, String> entry : expected.entrySet()) {
                String field = entry.getKey();
                String expectedValue = entry.getValue();
                String actualValue = actual.getOrDefault(field, "(missing)");

                if (!expectedValue.equalsIgnoreCase(actualValue)) {
                    warn.log("[MISMATCH] " + field + " → Expected: " + expectedValue + ", Found: " + actualValue);
                } else {
                    info.success("[MATCH] " + field + ": " + expectedValue);
                }

                Assert.assertEquals(actualValue, expectedValue, "[ASSERT FAILED] Field: " + field);
            }
        }
    }
}

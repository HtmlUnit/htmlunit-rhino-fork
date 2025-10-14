package org.mozilla.javascript.regexp;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * Unicode properties handler for Java 11 Character class. Handles binary properties from ECMA-262
 * and general category values.
 */
public class UnicodeProperties {
    // Binary Property Names (from ECMA-262 table-binary-unicode-properties)
    public static final byte ALPHABETIC = 1;
    public static final byte ASCII = ALPHABETIC + 1;
    public static final byte CASE_IGNORABLE = ASCII + 1;
    public static final byte ASCII_HEX_DIGIT = CASE_IGNORABLE + 1;
    public static final byte HEX_DIGIT = ASCII_HEX_DIGIT + 1;
    public static final byte ID_CONTINUE = HEX_DIGIT + 1;
    public static final byte ID_START = ID_CONTINUE + 1;
    public static final byte LOWERCASE = ID_START + 1;
    public static final byte UPPERCASE = LOWERCASE + 1;
    public static final byte WHITE_SPACE = UPPERCASE + 1;

    // Non-binary properties
    public static final byte GENERAL_CATEGORY = WHITE_SPACE + 1;
    public static final byte SCRIPT = GENERAL_CATEGORY + 1;

    // Property Values for General Category (from PropertyValueAliases.txt)
    // OTHER
    public static final byte OTHER = 1;
    public static final byte CONTROL = OTHER + 1;
    public static final byte FORMAT = CONTROL + 1;
    public static final byte UNASSIGNED = FORMAT + 1;
    public static final byte PRIVATE_USE = UNASSIGNED + 1;
    public static final byte SURROGATE = PRIVATE_USE + 1;
    public static final byte LETTER = SURROGATE + 1;
    public static final byte LOWERCASE_LETTER = LETTER + 1;
    public static final byte MODIFIER_LETTER = LOWERCASE_LETTER + 1;
    public static final byte OTHER_LETTER = MODIFIER_LETTER + 1;
    public static final byte TITLECASE_LETTER = OTHER_LETTER + 1;
    public static final byte UPPERCASE_LETTER = TITLECASE_LETTER + 1;
    public static final byte MARK = UPPERCASE_LETTER + 1;
    public static final byte SPACING_MARK = MARK + 1;
    public static final byte ENCLOSING_MARK = SPACING_MARK + 1;
    public static final byte NONSPACING_MARK = ENCLOSING_MARK + 1;
    public static final byte NUMBER = NONSPACING_MARK + 1;
    public static final byte DECIMAL_NUMBER = NUMBER + 1;
    public static final byte LETTER_NUMBER = DECIMAL_NUMBER + 1;
    public static final byte OTHER_NUMBER = LETTER_NUMBER + 1;
    public static final byte PUNCTUATION = OTHER_NUMBER + 1;
    public static final byte CONNECTOR_PUNCTUATION = PUNCTUATION + 1;
    public static final byte DASH_PUNCTUATION = CONNECTOR_PUNCTUATION + 1;
    public static final byte CLOSE_PUNCTUATION = DASH_PUNCTUATION + 1;
    public static final byte FINAL_PUNCTUATION = CLOSE_PUNCTUATION + 1;
    public static final byte INITIAL_PUNCTUATION = FINAL_PUNCTUATION + 1;
    public static final byte OTHER_PUNCTUATION = INITIAL_PUNCTUATION + 1;
    public static final byte OPEN_PUNCTUATION = OTHER_PUNCTUATION + 1;
    public static final byte SYMBOL = OPEN_PUNCTUATION + 1;
    public static final byte CURRENCY_SYMBOL = SYMBOL + 1;
    public static final byte MODIFIER_SYMBOL = CURRENCY_SYMBOL + 1;
    public static final byte MATH_SYMBOL = MODIFIER_SYMBOL + 1;
    public static final byte OTHER_SYMBOL = MATH_SYMBOL + 1;
    public static final byte SEPARATOR = OTHER_SYMBOL + 1;
    public static final byte LINE_SEPARATOR = SEPARATOR + 1;
    public static final byte PARAGRAPH_SEPARATOR = LINE_SEPARATOR + 1;
    public static final byte SPACE_SEPARATOR = PARAGRAPH_SEPARATOR + 1;

    // Binary property values
    public static final byte TRUE = SPACE_SEPARATOR + 1;
    public static final byte FALSE = TRUE + 1;

    // Property Name Map (canonical names and aliases)
    public static Map<String, Byte> PROPERTY_NAMES;
    static {
        Map<String, Byte> map = new HashMap<>();
        map.put("Alphabetic", ALPHABETIC);
        map.put("Alpha", ALPHABETIC);
        map.put("ASCII", ASCII);
        map.put("Case_Ignorable", CASE_IGNORABLE);
        map.put("CI", CASE_IGNORABLE);
        map.put("General_Category", GENERAL_CATEGORY);
        map.put("gc", GENERAL_CATEGORY);
        map.put("Script", SCRIPT);
        map.put("sc", SCRIPT);
        map.put("ASCII_Hex_Digit", ASCII_HEX_DIGIT);
        map.put("AHex", ASCII_HEX_DIGIT);
        map.put("Hex_Digit", HEX_DIGIT);
        map.put("Hex", HEX_DIGIT);
        map.put("ID_Continue", ID_CONTINUE);
        map.put("IDC", ID_CONTINUE);
        map.put("ID_Start", ID_START);
        map.put("IDS", ID_START);
        map.put("Lowercase", LOWERCASE);
        map.put("Lower", LOWERCASE);
        map.put("Uppercase", UPPERCASE);
        map.put("Upper", UPPERCASE);
        map.put("White_Space", WHITE_SPACE);
        map.put("space", WHITE_SPACE);
        PROPERTY_NAMES = map;
    }

    // Property Value Map for General Category (canonical names and aliases)
    public static Map<String, Byte> PROPERTY_VALUES;
    static {
        Map<String, Byte> map = new HashMap<>();
        map.put("Other", OTHER);
        map.put("C", OTHER);
        map.put("Control", CONTROL);
        map.put("Cc", CONTROL);
        map.put("cntrl", CONTROL);
        map.put("Format", FORMAT);
        map.put("Cf", FORMAT);
        map.put("Unassigned", UNASSIGNED);
        map.put("Cn", UNASSIGNED);
        map.put("Private_Use", PRIVATE_USE);
        map.put("Co", PRIVATE_USE);
        map.put("Surrogate", SURROGATE);
        map.put("Cs", SURROGATE);
        map.put("Letter", LETTER);
        map.put("L", LETTER);
        map.put("Lowercase_Letter", LOWERCASE_LETTER);
        map.put("Ll", LOWERCASE_LETTER);
        map.put("Modifier_Letter", MODIFIER_LETTER);
        map.put("Lm", MODIFIER_LETTER);
        map.put("Other_Letter", OTHER_LETTER);
        map.put("Lo", OTHER_LETTER);
        map.put("Titlecase_Letter", TITLECASE_LETTER);
        map.put("Lt", TITLECASE_LETTER);
        map.put("Uppercase_Letter", UPPERCASE_LETTER);
        map.put("Lu", UPPERCASE_LETTER);
        map.put("Mark", MARK);
        map.put("M", MARK);
        map.put("Combining_Mark", MARK);
        map.put("Spacing_Mark", SPACING_MARK);
        map.put("Mc", SPACING_MARK);
        map.put("Enclosing_Mark", ENCLOSING_MARK);
        map.put("Me", ENCLOSING_MARK);
        map.put("Nonspacing_Mark", NONSPACING_MARK);
        map.put("Mn", NONSPACING_MARK);
        map.put("Number", NUMBER);
        map.put("N", NUMBER);
        map.put("Decimal_Number", DECIMAL_NUMBER);
        map.put("Nd", DECIMAL_NUMBER);
        map.put("digit", NUMBER);
        map.put("Letter_Number", LETTER_NUMBER);
        map.put("Nl", LETTER_NUMBER);
        map.put("Other_Number", OTHER_NUMBER);
        map.put("No", OTHER_NUMBER);
        map.put("Punctuation", PUNCTUATION);
        map.put("P", PUNCTUATION);
        map.put("punct", PUNCTUATION);
        map.put("Connector_Punctuation", CONNECTOR_PUNCTUATION);
        map.put("Pc", CONNECTOR_PUNCTUATION);
        map.put("Dash_Punctuation", DASH_PUNCTUATION);
        map.put("Pd", DASH_PUNCTUATION);
        map.put("Close_Punctuation", CLOSE_PUNCTUATION);
        map.put("Pe", CLOSE_PUNCTUATION);
        map.put("Final_Punctuation", FINAL_PUNCTUATION);
        map.put("Pf", FINAL_PUNCTUATION);
        map.put("Initial_Punctuation", INITIAL_PUNCTUATION);
        map.put("Pi", INITIAL_PUNCTUATION);
        map.put("Other_Punctuation", OTHER_PUNCTUATION);
        map.put("Po", OTHER_PUNCTUATION);
        map.put("Open_Punctuation", OPEN_PUNCTUATION);
        map.put("Ps", OPEN_PUNCTUATION);
        map.put("Symbol", SYMBOL);
        map.put("S", SYMBOL);
        map.put("Currency_Symbol", CURRENCY_SYMBOL);
        map.put("Sc", CURRENCY_SYMBOL);
        map.put("Modifier_Symbol", MODIFIER_SYMBOL);
        map.put("Sk", MODIFIER_SYMBOL);
        map.put("Math_Symbol", MATH_SYMBOL);
        map.put("Sm", MATH_SYMBOL);
        map.put("Other_Symbol", OTHER_SYMBOL);
        map.put("So", OTHER_SYMBOL);
        map.put("Separator", SEPARATOR);
        map.put("Z", SEPARATOR);
        map.put("Line_Separator", LINE_SEPARATOR);
        map.put("Zl", LINE_SEPARATOR);
        map.put("Paragraph_Separator", PARAGRAPH_SEPARATOR);
        map.put("Zp", PARAGRAPH_SEPARATOR);
        map.put("Space_Separator", SPACE_SEPARATOR);
        map.put("Zs", SPACE_SEPARATOR);
        PROPERTY_VALUES = map;
    }

    /**
     * Looks up a property name and optionally a value and returns an encoded int. For binary
     * properties, combines the property name with TRUE. For General_Category, combines
     * General_Category with the specified value.
     *
     * @param propertyOrValue Property name or property name=value pair
     * @return Encoded int combining property name and value
     */
    @SuppressWarnings("EnumOrdinal") // We don't persist the ordinals; hence this is safe.
    public static int lookup(String propertyOrValue) {
        if (propertyOrValue == null || propertyOrValue.isEmpty()) {
            return -1;
        }

        Matcher m =
                java.util.regex.Pattern.compile(
                                "^(?<propName>[a-zA-Z_]+)(?:=(?<propValue>[a-zA-Z_0-9]+))?$")
                        .matcher(propertyOrValue);
        m.find();
        if (!m.matches() || m.group("propName") == null) {
            return -1;
        }

        if (m.group("propValue") == null) {
            // It's a single property name (binary property)
            String property = m.group("propName");

            Byte propByte = PROPERTY_NAMES.get(property);

            if (propByte == null) {
                // Check if it's a general category value without the gc= prefix
                Byte valueByte = PROPERTY_VALUES.get(property);
                if (valueByte != null) {
                    // It's a GC value, encode it with GC property
                    return encodeProperty(GENERAL_CATEGORY, valueByte);
                }
                return -1;
            }

            if (propByte == GENERAL_CATEGORY || propByte == SCRIPT) {
                return -1;
            }

            // It's a binary property, encode with TRUE
            return encodeProperty(propByte, TRUE);
        } else {
            // It's a property=value format
            String property = m.group("propName");
            String value = m.group("propValue");

            Byte propByte = PROPERTY_NAMES.get(property);
            if (propByte == null) {
                return -1;
            }

            switch (propByte) {
                case GENERAL_CATEGORY:
                    Byte valueByte = PROPERTY_VALUES.get(value);
                    if (valueByte == null) {
                        return -1;
                    }
                    return encodeProperty(GENERAL_CATEGORY, valueByte);
                case SCRIPT:
                    try {
                        return encodeProperty(
                                SCRIPT, (byte) Character.UnicodeScript.forName(value).ordinal());
                    } catch (IllegalArgumentException e) {
                        return -1;
                    }
                default:
                    // Binary properties don't have values
                    return -1;
            }
        }
    }

    /**
     * Encodes a property name and value into a single int. The property name is in the high 16
     * bits, the value in the low 16 bits.
     *
     * @param property Property name constant
     * @param value Property value constant
     * @return Encoded int
     */
    private static int encodeProperty(byte property, byte value) {
        return ((property & 0xFF) << 8) | (value & 0xFF);
    }

    private static final Character.UnicodeScript[] UnicodeScriptValues =
            Character.UnicodeScript.values();

    /**
     * Tests if a code point has a specific Unicode property.
     *
     * @param property Encoded property (from lookup method)
     * @param codePoint Character code point to test
     * @return true if the code point has the property
     */
    public static boolean hasProperty(int property, int codePoint) {
        byte propByte = (byte) ((property >> 8) & 0xFF);
        int valueByte = (property & 0xFF);

        switch (propByte) {
            case ALPHABETIC:
                return Character.isAlphabetic(codePoint) == (valueByte == TRUE);

            case ASCII:
                return (codePoint <= 0x7F) == (valueByte == TRUE);

            case CASE_IGNORABLE:
                // Java doesn't have a direct method for this
                // This is an approximation
                return (Character.getType(codePoint) == Character.MODIFIER_SYMBOL
                                || Character.getType(codePoint) == Character.MODIFIER_LETTER
                                || Character.getType(codePoint) == Character.NON_SPACING_MARK)
                        == (valueByte == TRUE);

            case GENERAL_CATEGORY:
                int javaCategory = Character.getType(codePoint);
                return checkGeneralCategory(valueByte, javaCategory);
            case ASCII_HEX_DIGIT:
                return isHexDigit(codePoint) == (valueByte == TRUE);
            case HEX_DIGIT:
                return (Character.digit(codePoint, 16) != -1) == (valueByte == TRUE);
            case ID_CONTINUE:
                return Character.isUnicodeIdentifierPart(codePoint) == (valueByte == TRUE);

            case ID_START:
                return Character.isUnicodeIdentifierStart(codePoint) == (valueByte == TRUE);

            case LOWERCASE:
                return Character.isLowerCase(codePoint) == (valueByte == TRUE);

            case UPPERCASE:
                return Character.isUpperCase(codePoint) == (valueByte == TRUE);

            case WHITE_SPACE:
                {
                    // Note: This only a good approximation of the Unicode white space property
                    return (valueByte == TRUE)
                            == (Character.isSpaceChar(codePoint)
                                    || Character.isWhitespace(codePoint));
                }
            case SCRIPT:
                return Character.UnicodeScript.of(codePoint) == UnicodeScriptValues[valueByte];
            default:
                return false;
        }
    }

    /** Maps our property value bytes to Java's Character.getType() values. */
    private static boolean checkGeneralCategory(int propertyValueByte, int javaCategory) {
        switch (propertyValueByte) {
            case LETTER:
                return javaCategory == Character.UPPERCASE_LETTER
                        || javaCategory == Character.LOWERCASE_LETTER
                        || javaCategory == Character.TITLECASE_LETTER
                        || javaCategory == Character.MODIFIER_LETTER
                        || javaCategory == Character.OTHER_LETTER;
            case UPPERCASE_LETTER:
                return javaCategory == Character.UPPERCASE_LETTER;
            case LOWERCASE_LETTER:
                return javaCategory == Character.LOWERCASE_LETTER;
            case TITLECASE_LETTER:
                return javaCategory == Character.TITLECASE_LETTER;
            case MODIFIER_LETTER:
                return javaCategory == Character.MODIFIER_LETTER;
            case OTHER_LETTER:
                return javaCategory == Character.OTHER_LETTER;
            case MARK:
                return javaCategory == Character.NON_SPACING_MARK
                        || javaCategory == Character.ENCLOSING_MARK
                        || javaCategory == Character.COMBINING_SPACING_MARK;
            case NONSPACING_MARK:
                return javaCategory == Character.NON_SPACING_MARK;
            case ENCLOSING_MARK:
                return javaCategory == Character.ENCLOSING_MARK;
            case SPACING_MARK:
                return javaCategory == Character.COMBINING_SPACING_MARK;
            case NUMBER:
                return javaCategory == Character.DECIMAL_DIGIT_NUMBER
                        || javaCategory == Character.LETTER_NUMBER
                        || javaCategory == Character.OTHER_NUMBER;
            case DECIMAL_NUMBER:
                return javaCategory == Character.DECIMAL_DIGIT_NUMBER;
            case LETTER_NUMBER:
                return javaCategory == Character.LETTER_NUMBER;
            case OTHER_NUMBER:
                return javaCategory == Character.OTHER_NUMBER;

            case SEPARATOR:
                return javaCategory == Character.SPACE_SEPARATOR
                        || javaCategory == Character.LINE_SEPARATOR
                        || javaCategory == Character.PARAGRAPH_SEPARATOR;
            case SPACE_SEPARATOR:
                return javaCategory == Character.SPACE_SEPARATOR;
            case LINE_SEPARATOR:
                return javaCategory == Character.LINE_SEPARATOR;
            case PARAGRAPH_SEPARATOR:
                return javaCategory == Character.PARAGRAPH_SEPARATOR;

            case OTHER:
                return javaCategory == Character.OTHER_LETTER
                        || javaCategory == Character.OTHER_NUMBER
                        || javaCategory == Character.OTHER_PUNCTUATION
                        || javaCategory == Character.OTHER_SYMBOL;
            case CONTROL:
                return javaCategory == Character.CONTROL;
            case FORMAT:
                return javaCategory == Character.FORMAT;
            case SURROGATE:
                return javaCategory == Character.SURROGATE;
            case PRIVATE_USE:
                return javaCategory == Character.PRIVATE_USE;

            case PUNCTUATION:
                return javaCategory == Character.CONNECTOR_PUNCTUATION
                        || javaCategory == Character.DASH_PUNCTUATION
                        || javaCategory == Character.START_PUNCTUATION
                        || javaCategory == Character.END_PUNCTUATION
                        || javaCategory == Character.OTHER_PUNCTUATION
                        || javaCategory == Character.INITIAL_QUOTE_PUNCTUATION
                        || javaCategory == Character.FINAL_QUOTE_PUNCTUATION;
            case DASH_PUNCTUATION:
                return javaCategory == Character.DASH_PUNCTUATION;
            case OPEN_PUNCTUATION:
                return javaCategory == Character.START_PUNCTUATION;
            case CLOSE_PUNCTUATION:
                return javaCategory == Character.END_PUNCTUATION;
            case CONNECTOR_PUNCTUATION:
                return javaCategory == Character.CONNECTOR_PUNCTUATION;
            case OTHER_PUNCTUATION:
                return javaCategory == Character.OTHER_PUNCTUATION;
            case INITIAL_PUNCTUATION:
                return javaCategory == Character.INITIAL_QUOTE_PUNCTUATION;
            case FINAL_PUNCTUATION:
                return javaCategory == Character.FINAL_QUOTE_PUNCTUATION;

            case SYMBOL:
                return javaCategory == Character.MATH_SYMBOL
                        || javaCategory == Character.CURRENCY_SYMBOL
                        || javaCategory == Character.MODIFIER_SYMBOL
                        || javaCategory == Character.OTHER_SYMBOL;
            case MATH_SYMBOL:
                return javaCategory == Character.MATH_SYMBOL;
            case CURRENCY_SYMBOL:
                return javaCategory == Character.CURRENCY_SYMBOL;
            case MODIFIER_SYMBOL:
                return javaCategory == Character.MODIFIER_SYMBOL;
            case OTHER_SYMBOL:
                return javaCategory == Character.OTHER_SYMBOL;
            case UNASSIGNED:
                return javaCategory == Character.UNASSIGNED;

            default:
                return false;
        }
    }

    /** Checks if a code point is a hex digit. */
    private static boolean isHexDigit(int codePoint) {
        return (codePoint >= '0' && codePoint <= '9')
                || (codePoint >= 'a' && codePoint <= 'f')
                || (codePoint >= 'A' && codePoint <= 'F');
    }
}

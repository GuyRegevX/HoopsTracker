
package hoops.common.enums;

/**
 * Represents the different types of basketball statistics that can be recorded.
 */
public enum StatType {
    POINT("point"),
    ASSIST("assist"),
    REBOUND("rebound"),
    STEAL("steal"),
    BLOCK("block"),
    FOUL("foul"),
    TURNOVER("turnover"),
    MINUTES_PLAYED("minutes_played");

    private final String value;

    StatType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * Converts a string to its corresponding StatType enum value.
     *
     * @param text The string representation of the stat type
     * @return The corresponding StatType enum value
     * @throws IllegalArgumentException if no matching stat type is found
     */
    public static StatType fromString(String text) {
        for (StatType type : StatType.values()) {
            if (type.value.equalsIgnoreCase(text)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown stat type: " + text);
    }

    @Override
    public String toString() {
        return value;
    }
}
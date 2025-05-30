package explorableviz.authoringassistant.paragraph;

public enum ExpressionCategory {
    TREND("Trend"),
    AGGREGATION("Aggregation"),
    QUANTITATIVE("Quantitative");

    public final String label;
    ExpressionCategory(String label) {
        this.label = label;
    }

    public static ExpressionCategory of(String category) {
        for (ExpressionCategory cat : values()) {
            if (cat.name().equalsIgnoreCase(category)) {
                return cat;
            }
        }
        throw new IllegalArgumentException(STR."Invalid Expression: \{category}");
    }
}

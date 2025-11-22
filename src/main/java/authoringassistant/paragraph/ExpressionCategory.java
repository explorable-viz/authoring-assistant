package authoringassistant.paragraph;

public enum ExpressionCategory {
    COMPARISON("Comparison"),
    RANK("Rank"),
    RATIO("Ratio"),
    DATA_RETRIEVAL("Data Retrieval"),
    DIFFERENCE("Difference"),
    AVERAGE("Average"),
    SUM("Sum"),
    MIN_MAX("Min/Max"),
    GENERALISED_QUANTIFIER("Generalised Quantifier");


    public final String label;
    ExpressionCategory(String label) {
        this.label = label;
    }

    public String getName() {
        return name().toLowerCase().replace('_', '-');
    }

    public static ExpressionCategory of(String category) {
        for (ExpressionCategory cat : values()) {
            if (cat.name().equalsIgnoreCase(category)) {
                return cat;
            }
        }
        throw new IllegalArgumentException(STR."Invalid Category: \{category}");
    }
}

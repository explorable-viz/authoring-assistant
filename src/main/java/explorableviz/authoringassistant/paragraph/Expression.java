package explorableviz.authoringassistant.paragraph;

public class Expression extends TextFragment {

    private final String expr;
    private final ExpressionCategory category;
    public Expression(String expr, String value, ExpressionCategory category) {
        super(value);
        this.expr = expr;
        this.category = category;
    }

    public String getExpr() {
        return expr;
    }

    public ExpressionCategory getCategory() {
        return category;
    }
}

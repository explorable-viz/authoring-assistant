package authoringassistant.paragraph;

import java.util.Set;

public class Expression extends TextFragment {

    private final String expr;
    private final Set<ExpressionCategory> categories;
    public Expression(String expr, String value, Set<ExpressionCategory> categories) {
        super(value);
        this.expr = expr;
        this.categories = categories;
    }

    public String getExpr() {
        return expr;
    }

    public Set<ExpressionCategory> getCategories() {
        return categories;
    }
}

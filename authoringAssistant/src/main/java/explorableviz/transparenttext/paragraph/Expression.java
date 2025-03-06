package explorableviz.transparenttext.textfragment;

import explorableviz.transparenttext.variable.Variables;

import static explorableviz.transparenttext.Query.replaceVariables;

public class Expression extends TextFragment {

    private final String expr;
    public Expression(String expr, String value) {
        super(value);
        this.expr = expr;
    }

    public String getExpr() {
        return expr;
    }

    @Override
    public Expression clone() {
        return new Expression(expr, getValue());
    }

    public TextFragment replace(Variables computedVariables) {
        return new Expression(replaceVariables(expr, computedVariables), getValue());
    }
}

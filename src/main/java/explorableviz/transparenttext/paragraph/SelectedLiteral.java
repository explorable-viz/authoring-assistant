package explorableviz.transparenttext.paragraph;

import explorableviz.transparenttext.variable.Variables;

import static explorableviz.transparenttext.Program.replaceVariables;

public class SelectedLiteral extends TextFragment {
    protected SelectedLiteral(String value) {
        super(value);
    }

    @Override
    public Object clone() {
        return null;
    }

    @Override
    public TextFragment replace(Variables computedVariables) {
        return new Literal(replaceVariables(getValue(), computedVariables));
    }
}

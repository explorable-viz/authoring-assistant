package explorableviz.transparenttext.paragraph;

import explorableviz.transparenttext.variable.Variables;

import static explorableviz.transparenttext.Program.replaceVariables;

public class Literal extends TextFragment {

    public Literal(String value) {
        super(value);
    }

    @Override
    public Object clone() {
        return new Literal(getValue());
    }

    @Override
    public TextFragment replace(Variables computedVariables) {
        return new Literal(replaceVariables(getValue(), computedVariables));
    }
}

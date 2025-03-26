package explorableviz.transparenttext.paragraph;

import explorableviz.transparenttext.variable.Variables;

public abstract class TextFragment {
    private final String value;

    protected TextFragment(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public abstract Object clone();

    public abstract TextFragment replace(Variables computedVariables);
}

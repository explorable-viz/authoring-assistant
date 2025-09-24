package authoringassistant.paragraph;

public abstract class TextFragment {
    private final String value;

    protected TextFragment(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}

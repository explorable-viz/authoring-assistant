package explorableviz.transparenttext.paragraph;

import com.google.gson.JsonObject;
import explorableviz.transparenttext.variable.Variables;
import org.json.JSONObject;

public abstract class TextFragment {
    private final String value;

    public static TextFragment of(JSONObject element, String computedValue) {
        return (switch (element.getString("type")) {
            case "literal" -> new Literal(element.getString("value"));
            case "expression" ->
                    new Expression(element.getString("expression"), computedValue);
            default -> throw new RuntimeException(STR."\{element.getString("value")} type is invalid");
        });
    }
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

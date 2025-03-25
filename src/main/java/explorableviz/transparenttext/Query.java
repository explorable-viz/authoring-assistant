package explorableviz.transparenttext;

import explorableviz.transparenttext.paragraph.Expression;
import org.json.JSONObject;

public record Query(Program program, String paragraph, Expression expression) {

    public String toUserPrompt() {
        JSONObject object = new JSONObject();
        object.put("datasets", program.get_loadedDatasets());
        object.put("imports", program.get_loadedImports());
        object.put("code", program.getCode());
        object.put("paragraph", this.paragraph);
        return object.toString();
    }
}

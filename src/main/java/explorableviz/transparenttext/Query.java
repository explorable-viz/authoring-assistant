package explorableviz.transparenttext;

import org.json.JSONObject;

public record Query(Program program, String paragraph) {

    public String toUserPrompt() {
        JSONObject object = new JSONObject();
        object.put("datasets", program.get_loadedDatasets());
        object.put("imports", program.get_loadedImports());
        object.put("code", program.getCode());
        object.put("paragraph", this.paragraph);
        return object.toString();
    }
}

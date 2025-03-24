package explorableviz.transparenttext;

import org.json.JSONObject;

public record SubQuery(Query query, String paragraph) {

    public String toUserPrompt() {
        JSONObject object = new JSONObject();
        object.put("datasets", query.get_loadedDatasets());
        object.put("imports", query.get_loadedImports());
        object.put("code", query.getCode());
        object.put("paragraph", this.paragraph);
        return object.toString();
    }
}

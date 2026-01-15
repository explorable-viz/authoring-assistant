package authoringassistant.llm.prompt;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

public class PromptList extends ArrayList<Prompt> {
    public static final String SYSTEM = "system";
    public static final String USER = "user";
    public static final String ASSISTANT = "assistant";

    public PromptList() {
    }

    private void addPrompt(String type, String content) {
        if (this.size() == 0 && type.equals("system")) {
            this.add(new Prompt(type, content));
        } else {
            if (((Prompt)this.get(this.size() - 1)).getRole().equals(type)) {
                throw new AssertionError("WrongFormatException. Add [" + type + "] after [" + ((Prompt)this.get(this.size() - 1)).getRole() + "] is not allowed");
            }

            this.add(new Prompt(type, content));
        }

    }

    public void addSystemPrompt(String content) {
        this.addPrompt("system", content);
    }

    public void addUserPrompt(String content) {
        this.addPrompt("user", content);
    }

    public void addAssistantPrompt(String content) {
        this.addPrompt("assistant", content);
    }

    public void addPairPrompt(String user, String assistant) {
        this.addUserPrompt(user);
        this.addAssistantPrompt(assistant);
    }

    public void replaceTag(String tag, String value) {
        this.forEach((p) -> p.setContent(p.getContent().replace(tag, value)));
    }

    public void exportToJson(String filename) throws FileNotFoundException {
        JSONArray messages = new JSONArray();

        for(int i = 0; i < this.size(); ++i) {
            Prompt p = (Prompt)this.get(i);
            JSONObject message = new JSONObject();
            message.put("role", p.getRole());
            message.put("content", p.getContent());
            messages.put(message);
        }

        file_put_contents(filename, messages.toString(2));
    }

    public void importFromJson(String filename) throws IOException {
        String json = this.file_get_contents(filename);
        JSONArray messages = new JSONArray(json);
        this.parseJSONContent(messages);
    }

    public void parseJSONContent(JSONArray messages) {
        this.clear();

        for(int i = 0; i < messages.length(); ++i) {
            JSONObject m = messages.getJSONObject(i);
            Prompt p = new Prompt(m.getString("role"), m.getString("content"));
            this.add(p);
        }

    }

    public static void file_put_contents(String file_name, String content) throws FileNotFoundException {
        PrintWriter out = new PrintWriter(file_name);
        out.println(content);
        out.flush();
        out.close();
    }

    private String file_get_contents(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        StringBuilder builder = new StringBuilder();

        String line;
        while((line = reader.readLine()) != null) {
            builder.append(line);
        }

        reader.close();
        return builder.toString();
    }
}

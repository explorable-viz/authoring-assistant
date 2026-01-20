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
            if ((this.get(this.size() - 1)).getRole().equals(type)) {
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

    public void exportToJson(String filename) throws FileNotFoundException {
        JSONArray messages = new JSONArray();

        for(int i = 0; i < this.size(); ++i) {
            Prompt p = this.get(i);
            JSONObject message = new JSONObject();
            message.put("role", p.getRole());
            message.put("content", p.getContent());
            messages.put(message);
        }

        file_put_contents(filename, messages.toString(2));
    }

    public static void file_put_contents(String file_name, String content) throws FileNotFoundException {
        PrintWriter out = new PrintWriter(file_name);
        out.println(content);
        out.flush();
        out.close();
    }
}

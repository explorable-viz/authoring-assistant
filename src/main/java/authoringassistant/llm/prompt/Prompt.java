package authoringassistant.llm.prompt;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

public class Prompt extends ArrayList<Message> {
    public static final String SYSTEM = "system";
    public static final String USER = "user";
    public static final String ASSISTANT = "assistant";

    public Prompt() {
    }

    private void addMessage(String type, String content) {
        if (this.size() == 0 && type.equals("system")) {
            this.add(new Message(type, content));
        } else {
            if ((this.get(this.size() - 1)).getRole().equals(type)) {
                throw new AssertionError("WrongFormatException. Add [" + type + "] after [" + ((Message)this.get(this.size() - 1)).getRole() + "] is not allowed");
            }

            this.add(new Message(type, content));
        }

    }

    // Often called "system prompt", but "system message" might be more consistent
    public void addSystemPrompt(String content) {
        this.addMessage("system", content);
    }

    public void addUserMessage(String content) {
        this.addMessage("user", content);
    }

    public void addAssistantMessage(String content) {
        this.addMessage("assistant", content);
    }

    public void exportToJson(String filename) throws FileNotFoundException {
        JSONArray messages = new JSONArray();

        for(int i = 0; i < this.size(); ++i) {
            Message p = this.get(i);
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

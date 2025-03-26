package explorableviz.transparenttext;

import it.unisa.cluelab.lllm.llm.LLMEvaluatorAgent;
import it.unisa.cluelab.lllm.llm.prompt.PromptList;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.logging.Logger;

public class AuthoringAssistant {

    public final Logger logger = Logger.getLogger(AuthoringAssistant.class.getName());
    private final PromptList prompts;
    private final LLMEvaluatorAgent llm;

    public AuthoringAssistant(InContextLearning inContextLearning, String agentClassName) throws Exception {
        this.prompts = inContextLearning.toPromptList();
        llm = initialiseAgent(agentClassName);
    }

    public QueryResult execute(Query query) throws Exception {
        int limit = Settings.getLimit();
        // Add the input query to the KB that will be sent to the LLM
        PromptList sessionPrompt = (PromptList) this.prompts.clone();
        int attempts;
        long start = System.currentTimeMillis();
        sessionPrompt.addUserPrompt(query.toUserPrompt());

        Expression response = null;
        for (attempts = 0; response == null && attempts <= limit; attempts++) {
            logger.info(STR."Attempt #\{attempts}");
            // Send the query to the LLM to be processed
            Expression candidate = null;
            try {
                candidate = (Expression) llm.evaluate(sessionPrompt, "");
                sessionPrompt.addAssistantPrompt(candidate.getExpr() == null ? "NULL" : candidate.getExpr());
            } catch (Exception e) {
                e.printStackTrace();
                sessionPrompt.exportToJson(STR."logs/json/\{Path.of(query.program().getTestCaseFileName()).getFileName()}_\{System.currentTimeMillis()}.json");
                System.exit(1);
            }
            //Check each generated expressions
            logger.info(STR."Received response: \{candidate.getExpr()}");
            query.program().writeFluidFiles(candidate.getExpr());
            Optional<String> errors = query.program().validate(new FluidCLI(query.program().getDatasets(), query.program().getImports()).evaluate(query.program().getFluidFileName()), query.expression());
            if (errors.isPresent()) {
                sessionPrompt.addUserPrompt(generateLoopBackMessage(candidate.getExpr(), errors.get()));
            } else {
                response = candidate;
            }
        }
        long end = System.currentTimeMillis();
        if (response == null || response.getExpr() == null) {
            logger.warning(STR."Expression validation failed after \{limit} attempts");
        } else {
            //query.getParagraph().spliceExpression(response);
            //Add only the Q-A and not all the process.
            if (Settings.isEditorLoopEnabled()) {
                this.prompts.addUserPrompt(query.toUserPrompt());
                this.prompts.addAssistantPrompt(response.getExpr());
            }
            logger.info(query.paragraph());
        }

        return new QueryResult(response, attempts, query, end - start);
    }

    private LLMEvaluatorAgent initialiseAgent(String agentClassName) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        logger.info(STR."Initializing agent: \{agentClassName}");
        LLMEvaluatorAgent llmAgent;
        Class<?> agentClass = Class.forName(agentClassName);
        llmAgent = (LLMEvaluatorAgent) agentClass
                .getDeclaredConstructor(JSONObject.class)
                .newInstance(Settings.getSettings());

        return llmAgent;
    }

    private String generateLoopBackMessage(String response, String errorDetails) {
        String errorMessage;
        if (errorDetails.toLowerCase().contains("key") && errorDetails.toLowerCase().contains("not found")) {
            errorMessage = String.format(
                    "KeyNotFound Error. The generated expression %s is trying to access a key that does not exist. " +
                            "Check the code and regenerate the expression. Remember: reply only with the expression, without any other comment.",
                    response
            );
        } else if (errorDetails.toLowerCase().contains("parseerror")) {
            errorMessage = String.format(
                    "SyntacticError. The generated expression %s caused the following error: \n%s. " +
                            "Check the code and regenerate the expression. Remember: reply only with the expression, without any other comment.",
                    response, errorDetails
            );
        } else {
            errorMessage = String.format(
                    "ValueMismatchError. The generated expression %s produced an unexpected value. " +
                            "Check the code and regenerate the expression. Remember: reply only with the expression, without any other comment.",
                    response
            );
        }
        return errorMessage;
    }
}

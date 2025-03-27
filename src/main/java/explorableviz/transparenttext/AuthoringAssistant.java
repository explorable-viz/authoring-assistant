package explorableviz.transparenttext;

import explorableviz.transparenttext.paragraph.Expression;
import it.unisa.cluelab.lllm.llm.LLMEvaluatorAgent;
import it.unisa.cluelab.lllm.llm.prompt.PromptList;
import org.checkerframework.checker.units.qual.A;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public class AuthoringAssistant {

    public final Logger logger = Logger.getLogger(AuthoringAssistant.class.getName());
    private final PromptList prompts;
    private final LLMEvaluatorAgent llm;
    private final Program program;

    public AuthoringAssistant(InContextLearning inContextLearning, String agentClassName, Program program) throws Exception {
        this.prompts = inContextLearning.toPromptList();
        llm = initialiseAgent(agentClassName);
        this.program = program;
    }

    public List<QueryResult> executeProgram() throws Exception {
        List<QueryResult> results = new ArrayList<>();
        if (Settings.isEditorLoopEnabled()) {
            List<Expression> computed = new ArrayList<>();
            Optional<Query> query = program.nextQuery(computed);
            while (query.isPresent()) {
                results.add(execute(query.get(), (PromptList) prompts.clone()));
                computed.add(results.getLast().response());
                //this.prompts.addPairPrompt(query.get().toUserPrompt(), results.getLast().response().getExpr());
                query = program.nextQuery(computed);

            }
        } else {
            for(Query query : program.toQueries()) {
                results.add(execute(query, (PromptList) prompts.clone()));
                //this.prompts.addPairPrompt(query.toUserPrompt(), results.getLast().response().getExpr());
            }
        }
        return results;
    }
    public QueryResult execute(Query query, PromptList sessionPrompts) throws Exception {
        int limit = Settings.getLimit();
        // Add the input query to the KB that will be sent to the LLM
        int attempts;
        long start = System.currentTimeMillis();
        sessionPrompts.addUserPrompt(query.toUserPrompt());

        for (attempts = 0; attempts <= limit; attempts++) {
            logger.info(STR."Attempt #\{attempts}");
            // Send the query to the LLM to be processed
            Expression candidate = (Expression) llm.evaluate(sessionPrompts, "");
            //Check each generated expressions
            logger.info(STR."Received response: \{candidate.getExpr()}");
            query.program().writeFluidFiles(candidate.getExpr());
            Optional<String> errors = query.program().validate(new FluidCLI(query.program().getDatasets(), query.program().getImports()).evaluate(query.program().getFluidFileName()), query.expression());
            if (errors.isPresent()) {
                sessionPrompts.addAssistantPrompt(candidate.getExpr() == null ? "NULL" : candidate.getExpr());
                sessionPrompts.addUserPrompt(generateLoopBackMessage(candidate.getExpr(), errors.get()));
            } else {
                return new QueryResult(candidate, attempts, query, System.currentTimeMillis() - start);
            }
        }
        logger.warning(STR."Expression validation failed after \{limit} attempts");
        return new QueryResult(null, attempts, query, System.currentTimeMillis() - start);
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

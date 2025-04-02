package explorableviz.transparenttext;

import explorableviz.transparenttext.paragraph.Expression;
import it.unisa.cluelab.lllm.llm.LLMEvaluatorAgent;
import it.unisa.cluelab.lllm.llm.prompt.PromptList;
import kotlin.Pair;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
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

    public List<Pair<Query, QueryResult>> executeQueries() throws Exception {
        List<Pair<Query, QueryResult>> results = new ArrayList<>();
        List<Expression> computed = new ArrayList<>();
        List<Query> queries = program.getParagraph().queries(program, computed);
        for(Query query : queries) {
            results.add(execute(query));
            computed.add(results.getLast().component2().response());
            //Editor Loop
            /*if(Settings.isEditorLoopEnabled()) {
                queries.addAll(program.getParagraph().queries(program, computed));
            }*/
        }
        return results;
    }

    public Pair<Query, QueryResult> execute(Query query) throws Exception {
        int limit = Settings.getLimit();
        // Add the input query to the KB that will be sent to the LLM
        int attempts;
        long start = System.currentTimeMillis();
        PromptList sessionPrompts = (PromptList) prompts.clone();
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
                return new Pair<>(query, new QueryResult(candidate, attempts, System.currentTimeMillis() - start));
            }
        }
        logger.warning(STR."Expression validation failed after \{limit} attempts");
        return new Pair<>(query, new QueryResult(null, attempts, System.currentTimeMillis() - start));
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

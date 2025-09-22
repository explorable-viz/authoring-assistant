package explorableviz.authoringassistant;

import explorableviz.authoringassistant.llm.LLMDummyAgent;
import explorableviz.authoringassistant.paragraph.Expression;
import it.unisa.cluelab.lllm.llm.LLMEvaluatorAgent;
import it.unisa.cluelab.lllm.llm.prompt.PromptList;
import kotlin.Pair;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import explorableviz.authoringassistant.Program.QueryResult;

import static explorableviz.authoringassistant.Program.extractValue;
import static explorableviz.authoringassistant.Program.writeFluidFiles;

public class AuthoringAssistant {

    public final Logger logger = Logger.getLogger(AuthoringAssistant.class.getName());
    private final PromptList prompts;
    private final LLMEvaluatorAgent<Expression> llm;
    private Program templateProgram;
    private final SuggestionAgent suggestionAgent;
    private final int runId;
    private final String jsonLogFolder;
    public AuthoringAssistant(InContextLearning inContextLearning, String agentClassName, Program templateProgram, String suggestionAgentClassName, int runId, String jsonLogFolder) throws Exception {
        this.prompts = inContextLearning.toPromptList();
        llm = initialiseAgent(agentClassName);
        this.suggestionAgent = new SuggestionAgent(suggestionAgentClassName);
        this.templateProgram = templateProgram;
        this.runId = runId;
        this.jsonLogFolder = jsonLogFolder;
    }

    public List<Pair<Program, QueryResult>> executePrograms() throws Exception {
        List<Pair<Program, QueryResult>> results = new ArrayList<>();
        List<Pair<Program, Expression>> programEdits;
        int i = 0;
        if (Settings.isSuggestionAgentEnabled()) {
            templateProgram = suggestionAgent.generateTemplateProgram(templateProgram);
        }
        programEdits = templateProgram.asIndividualEdits(templateProgram);
        int editId = 0;
        while (!programEdits.isEmpty()) {
            try {
                Pair<Program, Expression> individualEdit = programEdits.get(i);
                //selection
                Program programEdit = individualEdit.getFirst();
                QueryResult result = execute(individualEdit, editId++);

                programEdit.replaceParagraph(programEdit.getParagraph().splice(result.correctResponse() == null ? individualEdit.getSecond() : result.correctResponse()));
                results.add(new Pair<>(programEdit, result));
                programEdits = programEdit.asIndividualEdits(templateProgram);
                programEdit.toWebsite();
            } catch (Exception e) {
                logger.severe("Error executing program edit: " + e.getMessage());
                logger.info("Returning partial results obtained so far: " + results.size() + " results");
                // Restituisce i risultati parziali invece di propagare l'eccezione
                return results;
            }
        }
        return results;
    }

    public QueryResult execute(Pair<Program, Expression> test, int editId) throws Exception {
        final int limit = llm instanceof LLMDummyAgent ? 1 : Settings.getLimit();
        // Add the input query to the KB that will be sent to the LLM
        int attempts;
        final long start = System.currentTimeMillis();
        Program subProgram = test.getFirst();
        Expression expected = test.getSecond();
        final PromptList sessionPrompts = (PromptList) prompts.clone();
        sessionPrompts.addUserPrompt(subProgram.toUserPrompt());

        for (attempts = 0; attempts <= limit; attempts++) {
            boolean errors = false;
            logger.info(STR."Attempt #\{attempts}");
            // Send the program to the LLM to be processed
            Expression candidate = llm.evaluate(sessionPrompts, "");
            //Check each generated expressions
            if(candidate == null) {
                sessionPrompts.addAssistantPrompt("NULL");
                sessionPrompts.addUserPrompt("Null Expression");
                continue;
            }
            if(candidate.getExpr().equals(expected.getValue())) {
                sessionPrompts.addAssistantPrompt(candidate.getExpr() == null ? "NULL" : candidate.getExpr());
                sessionPrompts.addUserPrompt("ExpressionError: Received a static value instead of a dynamic expression. " +
                        "Please provide a valid fluid expression that *evaluates to* the expected value, rather than the value itself.");
                continue;
            }
            for (Map<String, String> datasets : subProgram.getTest_datasets()) {

                logger.info(STR."Received response: \{candidate.getExpr()}");

                Optional<String> error = Program.validate(
                        evaluateExpression(subProgram, datasets, candidate),
                        new Expression(expected.getExpr(), extractValue(evaluateExpression(subProgram, datasets, expected)), expected.getCategories()));

                if (error.isPresent()) {
                    sessionPrompts.addAssistantPrompt(candidate.getExpr() == null ? "NULL" : candidate.getExpr());
                    sessionPrompts.addUserPrompt(generateLoopBackMessage(candidate.getExpr(), error.get()));
                    errors = true;
                    break;
                }
            }
            if (!errors) {
                sessionPrompts.exportToJson(STR."\{this.jsonLogFolder}/\{Path.of(test.getFirst().getTestCaseFileName()).getFileName()}_\{editId}.json");
                return new QueryResult(candidate, expected, attempts, System.currentTimeMillis() - start, runId);
            }

        }
        sessionPrompts.exportToJson(STR."\{this.jsonLogFolder}/\{Path.of(test.getFirst().getTestCaseFileName()).getFileName()}_\{editId}.json");
        logger.warning(STR."Expression validation failed after \{limit} attempts");
        return new QueryResult(null, expected, attempts, System.currentTimeMillis() - start, runId);
    }

    private static String evaluateExpression(Program p, Map<String, String> datasets, Expression expression) throws IOException {
        final FluidCLI fluidCLI = new FluidCLI();
        writeFluidFiles(Settings.getFluidTempFolder(), Program.fluidFileName, expression.getExpr(), p.getDatasets(), datasets, p.getImports(), p.get_loadedImports(), p.getCode());
        return fluidCLI.evaluate(p.getFluidFileName());
    }

    private LLMEvaluatorAgent<Expression> initialiseAgent(String agentClassName) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        logger.info(STR."Initializing agent: \{agentClassName}");
        LLMEvaluatorAgent<Expression> llmAgent;
        Class<?> agentClass = Class.forName(agentClassName);
        llmAgent = (LLMEvaluatorAgent<Expression>) agentClass
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
        } else if (errorDetails.toLowerCase().contains("parseerror") || errorDetails.toLowerCase().contains("error:")) {
            errorMessage = String.format(
                    "SyntacticError. The generated expression %s caused the following error: \n%s. " +
                            "Check the code, the parenthesis and regenerate the expression. Remember: reply only with the expression, without any other comment.",
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

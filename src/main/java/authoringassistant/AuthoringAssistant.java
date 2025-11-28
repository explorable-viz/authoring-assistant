package authoringassistant;

import authoringassistant.llm.LLMDummyAgent;
import authoringassistant.paragraph.Expression;
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

import authoringassistant.Program.QueryResult;

import static authoringassistant.Program.extractValue;
import static authoringassistant.Program.writeFluidFiles;

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
        List<Pair<Program, Expression>> problems;
        int i = 0;
        if (Settings.isSuggestionAgentEnabled()) {
            templateProgram = suggestionAgent.generateTemplateProgram(templateProgram);
        }
        problems = templateProgram.asIndividualProblems(templateProgram);
        int problemIndex = 0;
        while (!problems.isEmpty()) {
            try {
                Pair<Program, Expression> problem = problems.get(i);
                Program program = problem.getFirst();
                QueryResult result = execute(problem, problemIndex++);

                program.replaceParagraph(program.getParagraph().splice(result.correctResponse() == null ? problem.getSecond() : result.correctResponse()));
                results.add(new Pair<>(program, result));
                problems = program.asIndividualProblems(templateProgram);
                program.toWebsite();
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
        int parseErrors=0, counterfactualFails=0, nullExpressions=0, onlyLiteralExpressions=0;
        for (attempts = 0; attempts <= limit; attempts++) {
            boolean errors = false;
            logger.info(STR."Attempt #\{attempts}");
            // Send the program to the LLM to be processed
            Expression candidate = llm.evaluate(sessionPrompts, "");
            //Check each generated expressions
            if(candidate == null) {
                nullExpressions++;
                sessionPrompts.addAssistantPrompt("NULL");
                sessionPrompts.addUserPrompt("ExpressionError: Received a NULL expression instead of a valid expression. " +
                        "Please provide a valid fluid expression that *evaluates to* the expected value.");
                continue;
            }
            if(candidate.getExpr() != null && candidate.getExpr().equals(expected.getValue())) {
                onlyLiteralExpressions++;
                sessionPrompts.addAssistantPrompt(candidate.getExpr() == null ? "NULL" : candidate.getExpr());
                sessionPrompts.addUserPrompt("ExpressionError: Received a static value instead of a dynamic expression. " +
                        "Please provide a valid fluid expression that *evaluates to* the expected value, rather than the value itself.");
                continue;
            }
            boolean firstTest = false;
            for (Map<String, String> datasets : subProgram.getTest_datasets()) {
                logger.info(STR."Received response: \{candidate.getExpr()}");
                Optional<String> error = Program.validate(
                        evaluateExpression(subProgram, datasets, candidate),
                        new Expression(expected.getExpr(), extractValue(evaluateExpression(subProgram, datasets, expected)), expected.getCategories()));

                if (error.isPresent()) {
                    sessionPrompts.addAssistantPrompt(candidate.getExpr() == null ? "NULL" : candidate.getExpr());
                    sessionPrompts.addUserPrompt(generateLoopBackMessage(candidate.getExpr(), error.get()));
                    errors = true;
                    if(firstTest) {
                        parseErrors++;
                    } else {
                        counterfactualFails++;
                    }
                    break;
                }
                firstTest = true;
            }
            if (!errors) {
                sessionPrompts.addAssistantPrompt(candidate.getExpr());
                sessionPrompts.exportToJson(STR."\{this.jsonLogFolder}/\{Path.of(test.getFirst().getTestCaseFileName()).getFileName()}_\{editId}.json");
                return new QueryResult(candidate, expected, attempts, System.currentTimeMillis() - start, runId, parseErrors, counterfactualFails, nullExpressions, onlyLiteralExpressions);
            }

        }
        sessionPrompts.exportToJson(STR."\{this.jsonLogFolder}/\{Path.of(test.getFirst().getTestCaseFileName()).getFileName()}_\{editId}.json");
        logger.warning(STR."Expression validation failed after \{limit} attempts");
        return new QueryResult(null, expected, attempts, System.currentTimeMillis() - start, runId, parseErrors, counterfactualFails, nullExpressions, onlyLiteralExpressions);
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

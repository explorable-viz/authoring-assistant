package authoringassistant;

import authoringassistant.llm.interpretation.DummyAgent;
import authoringassistant.paragraph.Expression;
import authoringassistant.llm.LLMEvaluatorAgent;
import authoringassistant.llm.prompt.PromptList;
import kotlin.Pair;

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

    public AuthoringAssistant(SystemPrompt systemPrompt, String agentClassName, Program templateProgram, String suggestionAgentClassName, int runId, String jsonLogFolder) throws Exception {
        this.prompts = systemPrompt.toPromptList();
        llm = initialiseAgent(agentClassName);
        this.suggestionAgent = suggestionAgentClassName != null ? new SuggestionAgent(suggestionAgentClassName) : null;
        this.templateProgram = templateProgram;
        this.runId = runId;
        this.jsonLogFolder = jsonLogFolder;
    }

    public List<Pair<Program, QueryResult>> runTestProblems() throws Exception {
        List<Pair<Program, QueryResult>> results = new ArrayList<>();
        List<Pair<Program, Expression>> problems;
        int i = 0;
        if (this.suggestionAgent != null) {
            templateProgram = suggestionAgent.generateTemplateProgram(templateProgram).program();
        }
        problems = templateProgram.asIndividualProblems(templateProgram);
        int problemIndex = 0;
        Program finalProgram;
        if (problems.isEmpty()) {
            finalProgram = templateProgram;
        } else
            do {
                try {
                    Pair<Program, Expression> problem = problems.get(i);
                    finalProgram = problem.getFirst();
                    QueryResult result = runProblem(problem, problemIndex++);

                    finalProgram.replaceParagraph(finalProgram.getParagraph().splice(result.correctResponse() == null ? problem.getSecond() : result.correctResponse()));
                    results.add(new Pair<>(finalProgram, result));
                    problems = finalProgram.asIndividualProblems(templateProgram);
                } catch (Exception e) {
                    logger.severe("Error executing program edit; aborting.\n" + e.getMessage());
                    throw e;
                }
            } while (!problems.isEmpty());
        finalProgram.toWebpage();
        return results;
    }

    public QueryResult runProblem(Pair<Program, Expression> test, int problemIndex) throws Exception {
        final int attemptLimit = llm instanceof DummyAgent ? 2 : Settings.getInterpretationAgentLoopbackLimit();
        int attempt;
        final long start = System.currentTimeMillis();
        Program subProgram = test.getFirst();
        Expression expected = test.getSecond();
        final PromptList sessionPrompts = (PromptList) prompts.clone();
        sessionPrompts.addUserPrompt(subProgram.toUserPrompt());
        int parseErrors=0, counterfactualFails=0, missingResponses=0, literalResponses=0;
        final String info = STR."[Problem \{problemIndex + 1} of \{templateProgram.getParagraph().countExpressions()}]";
        for (attempt = 1; attempt <= attemptLimit; attempt++) {
            boolean errors = false;
            // Send the program to the LLM to be processed
            Expression candidate = llm.evaluate(sessionPrompts, "");
            //Check each generated expressions
            if(candidate == null || candidate.getExpr() == null) {
                missingResponses++;
                sessionPrompts.addAssistantPrompt("[No response received]");
                sessionPrompts.addUserPrompt("No response received. Please try again.");
                logger.fine(STR."\{info} Attempt #\{attempt}: retry");
            } else
            if (candidate.getExpr().equals(expected.getValue())) {
                literalResponses++;
                sessionPrompts.addAssistantPrompt(candidate.getExpr());
                sessionPrompts.addUserPrompt("ExpressionError: Received a static value instead of a dynamic expression. " +
                        "Please provide a valid fluid expression that *evaluates to* the expected value, rather than the value itself.");
                logger.fine(STR."\{info} Attempt #\{attempt}: retry");
            } else {
                boolean firstTest = false;
                for (Map<String, String> datasets : subProgram.getTest_datasets()) {
                    logger.fine(STR."\{info} Attempt #\{attempt}: received \{candidate.getExpr()}");
                    Optional<String> error = Program.validate(
                            evaluateExpression(subProgram, datasets, candidate),
                            new Expression(expected.getExpr(), extractValue(evaluateExpression(subProgram, datasets, expected)), expected.getCategories()));

                    if (error.isPresent()) {
                        sessionPrompts.addAssistantPrompt(candidate.getExpr());
                        sessionPrompts.addUserPrompt(loopBackMessage(candidate.getExpr(), error.get()));
                        errors = true;
                        if (firstTest) {
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
                    sessionPrompts.exportToJson(STR."\{this.jsonLogFolder}/\{Path.of(test.getFirst().getTestCaseFileName()).getFileName()}_\{problemIndex}.json");
                    logger.info(STR."\{info} Expression validation succeeded");
                    return new QueryResult(candidate, expected, attempt, System.currentTimeMillis() - start, runId, parseErrors, counterfactualFails, missingResponses, literalResponses);
                }
            }
        }
        sessionPrompts.exportToJson(STR."\{this.jsonLogFolder}/\{Path.of(test.getFirst().getTestCaseFileName()).getFileName()}_\{problemIndex}.json");
        logger.info(STR."\{info} Expression validation failed after \{attemptLimit} attempts");
        return new QueryResult(null, expected, attempt, System.currentTimeMillis() - start, runId, parseErrors, counterfactualFails, missingResponses, literalResponses);
    }

    private static String evaluateExpression(Program p, Map<String, String> datasets, Expression expression) throws IOException {
        final FluidCLI fluidCLI = new FluidCLI();
        writeFluidFiles(Settings.FLUID_TEMP_FOLDER, Program.fluidFileName, expression.getExpr(), p.getDatasets(), datasets, p.getImports(), p.get_loadedImports(), p.getCode());
        return fluidCLI.evaluate(p.getFluidFileName());
    }

    private LLMEvaluatorAgent<Expression> initialiseAgent(String agentClassName) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        logger.config(STR."Initializing agent: \{agentClassName}");
        LLMEvaluatorAgent<Expression> llmAgent;
        Class<?> agentClass = Class.forName(agentClassName);
        llmAgent = (LLMEvaluatorAgent<Expression>) agentClass
                .getDeclaredConstructor(Settings.class)
                .newInstance(Settings.getInstance());

        return llmAgent;
    }

    private String loopBackMessage(String response, String errorDetails) {
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

package authoringassistant;

import authoringassistant.llm.interpretation.DummyAgent;
import authoringassistant.paragraph.Expression;
import authoringassistant.llm.LLMEvaluatorAgent;
import authoringassistant.llm.prompt.PromptList;
import kotlin.Pair;

import java.io.IOException;
import java.nio.file.Files;
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

    public static final Logger logger = Logger.getLogger(AuthoringAssistant.class.getName());
    private final PromptList prompts;
    private final LLMEvaluatorAgent<Expression> interpretationAgent;
    private Program templateProgram;
    private final SuggestionAgent suggestionAgent;
    private final int runId;

    public AuthoringAssistant(SystemPrompt systemPrompt, String agentClassName, Program templateProgram, String suggestionAgentClassName, int runId) throws Exception {
        this.prompts = systemPrompt.toPromptList();
        this.interpretationAgent = LLMEvaluatorAgent.initialiseAgent(agentClassName);
        this.suggestionAgent = suggestionAgentClassName != null ? new SuggestionAgent(LLMEvaluatorAgent.initialiseAgent(suggestionAgentClassName)) : null;
        this.templateProgram = templateProgram;
        this.runId = runId;
    }

    public static boolean isTestMock(String interpretationAgent) {
        return interpretationAgent.equals(DummyAgent.class.getName());
    }

    public static ArrayList<Pair<Program, QueryResult>> runTestCases(SystemPrompt systemPrompt, String interpretationAgent, String suggestionAgent, List<Program> testCases) throws Exception {
        final ArrayList<Pair<Program, QueryResult>> allResults = new ArrayList<>();
        final int numRuns = isTestMock(interpretationAgent) ? 1 : Settings.numTestRuns();

        if (Settings.getTruncateTestsAt() != -1) {
            testCases = testCases.subList(0, Settings.getTruncateTestsAt());
        }

        for(int k = 0; k < numRuns; k++)
        {
            long nProblems = 0;
            int nCases = 0;
            for (Program testCase : testCases) {
                AuthoringAssistant authoringAssistant = new AuthoringAssistant(systemPrompt, interpretationAgent, testCase, suggestionAgent, k);
                List<Pair<Program, QueryResult>> results = authoringAssistant.runTestProblems();

                nProblems += results.size();
                long correct = results.stream()
                        .filter(r -> r.getSecond().correctResponse() != null)
                        .count();
                nCases++;
                logger.info(STR."[Run \{k + 1} of \{numRuns}][Test case \{nCases} of \{testCases.size()}] \{correct} of \{results.size()} responses correct");
                allResults.addAll(results);
            }
            logger.info(STR."Total number of test problems: \{nProblems}");
        }
        return allResults;
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
        final int attemptLimit = interpretationAgent instanceof DummyAgent ? 2 : Settings.getInterpretationAgentLoopbackLimit();
        Program subProgram = test.getFirst();
        Expression expected = test.getSecond();
        Files.createDirectories(Path.of(STR."results/\{Settings.getConfigName()}/\{test.getFirst().getTestCasePath()}/logs"));
        final PromptList sessionPrompts = (PromptList) prompts.clone();
        sessionPrompts.addUserPrompt(subProgram.toUserPrompt());
        int interpreterErrors = 0, counterfactualFails = 0, missingResponses = 0, literalResponses = 0;
        final String progress = STR."[Problem \{problemIndex + 1} of \{templateProgram.getParagraph().countExpressions()}]";
        final String logfile = STR."results/\{Settings.getConfigName()}/\{test.getFirst().getTestCasePath()}/logs/\{(test.getFirst().getTestCasePath()).getFileName()}_\{String.format("%02d", problemIndex)}.json";
        final List<String> errors = new ArrayList<>();
        Expression solution = null;
        int attempt = 1;
        while (attempt <= attemptLimit) {
            Expression candidate = interpretationAgent.evaluate(sessionPrompts, "");
            if (candidate == null || candidate.getExpr() == null) {
                missingResponses++;
                sessionPrompts.addAssistantPrompt("[No response received]");
                sessionPrompts.addUserPrompt("No response received. Please try again.");
                logger.fine(STR."\{progress} Attempt #\{attempt}: retry");
            } else
            if (candidate.getExpr().equals(expected.getValue())) {
                literalResponses++;
                sessionPrompts.addAssistantPrompt(candidate.getExpr());
                sessionPrompts.addUserPrompt("""
                    This is just the target string as a literal. Try again, but produce a Fluid expression that *computes* 
                    the target string as a query over the dataset, using the supplied library functions if necessary.
                    """);
                logger.fine(STR."\{progress} Attempt #\{attempt}: retry");
            } else {
                Map<String, String> datasets = subProgram.getDatasetsVariants().get(0);
//              Ignore for now
//              List<Map<String, String>> counterfactualDatasets = subProgram.getDatasetsVariants().subList(1, subProgram.getDatasetsVariants().size());
                logger.fine(STR."\{progress} Attempt #\{attempt}: received \{candidate.getExpr()}");
                Optional<String> error = Program.validate(
                    evaluateExpression(subProgram, datasets, candidate),
                    new Expression(expected.getExpr(), extractValue(evaluateExpression(subProgram, datasets, expected)), expected.getCategories())
                );
                sessionPrompts.addAssistantPrompt(candidate.getExpr());
                if (error.isPresent()) {
                    sessionPrompts.addUserPrompt(loopBackMessage(candidate.getExpr(), error.get()));
                    errors.add(error.get());
                    interpreterErrors++;
                } else {
                    solution = candidate;
                    break;
                }
            }
            attempt++;
        }
        if (solution == null) {
            assert attempt == attemptLimit + 1;
            logger.info(STR."\{progress} Expression validation failed after \{attemptLimit} attempts");
        } else {
            logger.info(STR."\{progress} Expression validation succeeded");
        }
        sessionPrompts.exportToJson(logfile);
        return new QueryResult(problemIndex + 1, interpretationAgent.getModel(), solution, expected, runId, interpreterErrors, counterfactualFails, missingResponses, literalResponses);
    }

    private static String evaluateExpression(Program p, Map<String, String> datasets, Expression expression) throws IOException {
        final FluidCLI fluidCLI = new FluidCLI();
        writeFluidFiles(Settings.INTERPRETER_TEMP_FOLDER, Program.INTERPRETER_TEMP_FILE, expression.getExpr(), p.getDatasetFilenames(), datasets, p.getImports(), p.get_loadedImports(), p.getCode());
        return fluidCLI.evaluate(Program.INTERPRETER_TEMP_FILE);
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

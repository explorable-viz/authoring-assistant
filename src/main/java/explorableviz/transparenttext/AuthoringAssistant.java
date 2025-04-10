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
import explorableviz.transparenttext.Program.ProgramResult;
public class AuthoringAssistant {

    public final Logger logger = Logger.getLogger(AuthoringAssistant.class.getName());
    private final PromptList prompts;
    private final LLMEvaluatorAgent llm;
    private final Program templateProgram;

    public AuthoringAssistant(InContextLearning inContextLearning, String agentClassName, Program templateProgram) throws Exception {
        this.prompts = inContextLearning.toPromptList();
        llm = initialiseAgent(agentClassName);
        this.templateProgram = templateProgram;
    }

    public List<Pair<Program, ProgramResult>> executePrograms() throws Exception {
        List<Pair<Program, ProgramResult>> results = new ArrayList<>();
        List<Pair<Program, Expression>> subTests = templateProgram.programs(templateProgram);
        int i = 0;
        while (!subTests.isEmpty()) {
            Pair<Program, Expression> subTest = subTests.get(i);
            Program subProgram = subTest.component1();
            ProgramResult result = execute(subTest);

            subProgram.replaceParagraph(subProgram.getParagraph().splice(result.response() == null ? subTest.component2() : result.response()));
            results.add(new Pair<>(subProgram, result));
            if(Settings.isEditorLoopEnabled()) {
                subTests.addAll(subProgram.programs(templateProgram));
            } else {
                subTests = subProgram.programs(templateProgram);
            }
        }
        return results;
    }

    public ProgramResult execute(Pair<Program, Expression> test) throws Exception {
        final int limit = Settings.getLimit();
        // Add the input query to the KB that will be sent to the LLM
        int attempts;
        final long start = System.currentTimeMillis();
        Program subProgram = test.component1();
        Expression expected = test.component2();
        final PromptList sessionPrompts = (PromptList) prompts.clone();
        sessionPrompts.addUserPrompt(subProgram.toUserPrompt());
        for (attempts = 0; attempts <= limit; attempts++) {
            logger.info(STR."Attempt #\{attempts}");
            // Send the program to the LLM to be processed
            Expression candidate = (Expression) llm.evaluate(sessionPrompts, "");
            //Check each generated expressions
            logger.info(STR."Received response: \{candidate.getExpr()}");
            //program.writeFluidFiles(candidate.getExpr());
            final FluidCLI fluidCLI = new FluidCLI(subProgram.getDatasets(), subProgram.getImports());
            Optional<String> error = Program.validate(fluidCLI.evaluate(subProgram.getFluidFileName()), expected);
            if (error.isPresent()) {
                sessionPrompts.addAssistantPrompt(candidate.getExpr() == null ? "NULL" : candidate.getExpr());
                sessionPrompts.addUserPrompt(generateLoopBackMessage(candidate.getExpr(), error.get()));
            } else {
                return new ProgramResult(candidate, expected, attempts, System.currentTimeMillis() - start);
            }
        }
        logger.warning(STR."Expression validation failed after \{limit} attempts");
        return new ProgramResult(null, expected, attempts, System.currentTimeMillis() - start);
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

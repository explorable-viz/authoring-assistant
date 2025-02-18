package explorableviz.transparenttext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.IntStream;

public class Main {
    public static Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String... args) {

        HashMap<String, String> arguments = parseArguments(args);
        logger.info("Arguments passed from command line");
        logger.info(arguments.toString().replace(",", "\n"));
        final String agent = arguments.get("agent");
        final String inContextLearningPath = arguments.get("inContextLearningPath");
        final String settingsPath = arguments.get("settings");
        final String testPath = arguments.get("testPath");
        final int numTestToGenerate = Integer.parseInt(arguments.get("numTestToGenerate"));
        final int numLearningCaseToGenerate = Integer.parseInt(arguments.get("numLearningCaseToGenerate"));
        final float threshold = Integer.parseInt(arguments.get("threshold"));
        final ArrayList<QueryContext> queryContexts;
        final LearningQueryContext learningQueryContext;
        final Optional<Integer> numQueryToExecute = arguments.containsKey("numQueryToExecute") ? Optional.of(Integer.parseInt(arguments.get("numQueryToExecute"))) : Optional.empty();

        try {
            Settings.getInstance().loadSettings(settingsPath);
            learningQueryContext = LearningQueryContext.importLearningCaseFromJSON(inContextLearningPath, numLearningCaseToGenerate);
            queryContexts = TestQueryContext.loadCases(testPath, numTestToGenerate);
            final int queryLimit = numQueryToExecute.orElseGet(queryContexts::size);
            final ArrayList<String> results = new ArrayList<>();

            AuthoringAssistant workflow = new AuthoringAssistant(learningQueryContext, agent);
            for (int i = 0; i < queryLimit; i++) {
                QueryContext queryContext = queryContexts.get(i);
                logger.info(STR."Analysing query id=\{i}");
                results.add(workflow.execute(queryContext));
            }
            logger.info("Printing generated expression");
            for (String result : results) {
                logger.info(result);
            }

            logger.info("Computing accuracy");
            int correct = (int) IntStream.range(0, results.size())
                    .filter(i -> queryContexts.get(i).getExpected().equals(results.get(i)))
                    .count();

            float rate = (float) correct / queryLimit;
            System.out.println(STR."Accuracy: \{rate}");
            if (rate < threshold) {
                System.out.println("FAILED: Accuracy too low");
                System.exit(1);
            } else {
                System.out.println("PASS: Accuracy ok");
                System.exit(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static HashMap<String, String> parseArguments(String[] args) {
        HashMap<String, String> arguments = new HashMap<>();

        for (String arg : args) {
            if (arg.contains("=")) {
                String[] keyValue = arg.split("=", 2); // Split into key and value
                if (keyValue.length == 2) {
                    arguments.put(keyValue[0], keyValue[1]);
                } else {
                    System.err.println(STR."Invalid argument format: \{arg}");
                }
            } else {
                System.err.println(STR."Skipping argument without '=': \{arg}");
            }
        }

        return arguments;
    }


}

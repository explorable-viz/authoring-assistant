package explorableviz.transparenttext;

import kotlin.Pair;

import explorableviz.transparenttext.Program.QueryResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Main {
    public static Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String... args) {
        Map<String, String> arguments = parseArguments(args);
        logger.info("Arguments passed from command line");
        logger.info(arguments.toString().replace(",", "\n"));
        final ArrayList<Program> programs;
        final InContextLearning inContextLearning;
        final String agent = arguments.get("agent");
        try {
            Settings.init("settings.json");
            inContextLearning = InContextLearning.loadLearningCases(Settings.getSystemPromptPath(), Settings.getNumLearningCaseToGenerate());
            programs = Program.loadPrograms(Settings.getTestCaseFolder(), Settings.getNumTestToGenerate());
            final int queryLimit = Settings.getNumQueryToExecute().orElseGet(programs::size);
            final ArrayList<Pair<Program, QueryResult>> results = execute(inContextLearning, agent, queryLimit, programs);
            float accuracy = computeExactMatch(results);
            generateLinks();
            writeLog(results, agent, inContextLearning.size());
            if (accuracy >= Settings.getThreshold()) {
                System.out.println(STR."Accuracy OK =\{accuracy}");
                System.exit(0);
            } else {
                System.out.println(STR."Accuracy KO =\{accuracy}");
                System.exit(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

    }

    private static void writeLog(ArrayList<Pair<Program, QueryResult>> results, String agent, int learningContextSize) throws IOException {
        Files.createDirectories(Paths.get(Settings.getLogFolder()));
        try (PrintWriter out = new PrintWriter(new FileOutputStream(STR."\{Settings.getLogFolder()}/log_\{System.currentTimeMillis()}.csv"))) {
            String[] headers = {
                    "test-case", "llm-agent", "temperature", "num-token", "in-context-learning-size", "query-paragraph",
                    "attempts", "result", "generated-expression", "expected-expression", "expected-value", "duration(ms)"
            };
            out.println(String.join(";", headers));
            String content = results.stream()
                    .map(result -> {
                        Program program = result.component1();
                        QueryResult queryResult = result.component2();
                        String[] values = {
                                result.component1().getTestCaseFileName(),
                                agent,
                                String.valueOf(Settings.getTemperature()),
                                String.valueOf(Settings.getNumContextToken()),
                                String.valueOf(learningContextSize),
                                program.getParagraph().toFluidSyntax(),
                                String.valueOf(queryResult.attempt()),
                                queryResult.response() != null ? "OK" : "KO",
                                String.valueOf(queryResult.response() != null ? queryResult.response().getExpr() : "NULL"),
                                queryResult.expected().getExpr(),
                                queryResult.expected().getValue(),
                                String.valueOf(queryResult.duration())
                        };
                        return String.join(";", Arrays.stream(values).map(s -> STR."\"\{s}\"").toList());
                    })
                    .collect(Collectors.joining("\n"));
            out.println(content);
        }
    }

    private static float computeExactMatch(List<Pair<Program, QueryResult>> results) {
        logger.info("Computing accuracy");
        long count = IntStream.range(0, results.size()).filter(i -> results.get(i).component2().response() != null && results.get(i).component2().expected().getExpr().equals(results.get(i).component2().response().getExpr())).count();
        return (float) count / results.size();
    }

    private static ArrayList<Pair<Program, QueryResult>> execute(InContextLearning inContextLearning, String agent, int programLimit, List<Program> programs) throws Exception {
        final ArrayList<Pair<Program, QueryResult>> results = new ArrayList<>();
        for (int i = 0; i < programLimit; i++) {
            AuthoringAssistant workflow = new AuthoringAssistant(inContextLearning, agent, programs.get(i));
            logger.info(STR."Analysing program id=\{i}");
            results.addAll(workflow.executePrograms());
        }
        logger.info("Printing generated expression");
        for (Pair<Program, QueryResult> result : results) {
            logger.info(result.component2().response() != null ? result.component2().response().getExpr() : "NULL");
        }
        return results;
    }

    public static Map<String, String> parseArguments(String[] args) {
        return Arrays.stream(args)
                .filter(arg -> arg.contains("="))
                .map(arg -> arg.split("=", 2))
                .filter(keyValue -> keyValue.length == 2)
                .collect(Collectors.toMap(
                        keyValue -> keyValue[0],
                        keyValue -> keyValue[1],
                        (_, replacement) -> replacement
                ));
    }

    public static void generateLinks() throws Exception {
        String path = "website/authoring-assistant";
        File htmlFile = new File(STR."\{path}/index.html");

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setIgnoringElementContentWhitespace(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(htmlFile);

        XPath xpath = XPathFactory.newInstance().newXPath();
        Node linksDiv = (Node) xpath.evaluate("//div[@class='links']", doc, XPathConstants.NODE);
        if (linksDiv == null) {
            throw new RuntimeException("Element <div class='links'> not found.");
        }

        while (linksDiv.hasChildNodes()) {
            linksDiv.removeChild(linksDiv.getFirstChild());
        }

        try (Stream<Path> paths = Files.list(Paths.get(path))) {
            paths
                    .filter(Files::isDirectory)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(name -> !name.equals("datasets") && !name.equals("fluid") && !name.equals("font") && !name.equals("css") && !name.equals("image") && !name.equals("shared"))
                    .sorted()
                    .forEach(name -> {
                        Element a = doc.createElement("a");
                        a.setAttribute("href", STR."\{name}/");
                        a.setTextContent(name);
                        Element br = doc.createElement("br");
                        linksDiv.appendChild(a);
                        linksDiv.appendChild(br);
                    });
        }

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(htmlFile);
        transformer.transform(source, result);
    }

}

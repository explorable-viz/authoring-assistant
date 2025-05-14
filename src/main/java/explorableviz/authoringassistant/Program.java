package explorableviz.authoringassistant;

import com.fasterxml.jackson.databind.ObjectMapper;
import explorableviz.authoringassistant.paragraph.Expression;
import explorableviz.authoringassistant.paragraph.Literal;
import explorableviz.authoringassistant.paragraph.Paragraph;
import explorableviz.authoringassistant.variable.ValueOptions;
import explorableviz.authoringassistant.variable.Variables;
import kotlin.Pair;
import org.checkerframework.checker.units.qual.A;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static explorableviz.authoringassistant.variable.Variables.Flat.expandVariables;

public class Program {

    public final static Logger logger = Logger.getLogger(Program.class.getName());
    private final Map<String, String> datasets;
    private final ArrayList<Map<String, String>> test_datasets;
    private final List<String> imports;
    private final ArrayList<String> _loadedImports;
    private final String code;
    private final Paragraph paragraph;
    private final Map<String, String> _loadedDatasets;
    private final String testCaseFileName;
    public static final String fluidFileName = "llmTest";

    public Program(Paragraph paragraph, Map<String,String> datasets, List<String> imports, String code, Map<String, String> loadedDataset, String testCaseFileName, ArrayList<Map<String, String>> test_datasets) throws IOException {
        this.datasets = datasets;
        this._loadedDatasets = loadedDataset;
        this.code = code;
        this.test_datasets = test_datasets;
        this.testCaseFileName = testCaseFileName;
        this.imports = imports;
        this._loadedImports = loadImports(imports);
        this.paragraph = paragraph;
    }

    public static HashMap<String, String> loadDatasetsFiles(Map<String,String> datasetMapping, Variables variables) throws IOException {
        HashMap<String, String> loadedDatasets = new HashMap<>();
        for (Map.Entry<String, String> dataset : datasetMapping.entrySet()) {
            loadedDatasets.put(dataset.getKey(), replaceVariables(new String(Files.readAllBytes(Paths.get(new File(STR."\{Settings.getFluidCommonFolder()}/\{dataset.getValue()}.fld").toURI()))),variables));
        }
        return loadedDatasets;
    }

    private static Map<String, String> datasetMapping(JSONArray json_dataset) {
        return IntStream.range(0, json_dataset.length())
                .boxed()
                .collect(Collectors.toMap(
                        i -> json_dataset.getJSONObject(i).getString("var"),
                        i -> json_dataset.getJSONObject(i).getString("file")
                ));
    }
    private static ArrayList<String> loadImports(List<String> imports) throws IOException {
        ArrayList<String> loadedImports = new ArrayList<>();
        for (String path : imports) {
            File importLib = new File(STR."\{Settings.getFluidCommonFolder()}/\{path}.fld");
            if (importLib.exists()) {
                loadedImports.add(new String(Files.readAllBytes(importLib.toPath())));
            } else {
                loadedImports.add(new String(Files.readAllBytes(Paths.get(STR."\{Settings.getLibrariesBasePath()}/\{path}.fld"))));
            }
        }
        return loadedImports;
    }

    public static String extractValue(String commandLineResponse) {
        String[] outputLines = commandLineResponse.split("\n");
        if (outputLines.length < 2) {
            throw new RuntimeException("Output format is invalid");
        }
        return outputLines[FluidCLI.isWindows() ? 2 : 1].replaceAll("^\"|\"$", "");
    }

    public static Optional<String> validate(String commandLineResponse, Expression expectedExpression) {
        logger.info(STR."Validating command line output: \{commandLineResponse}");
        String value = extractValue(commandLineResponse);
        //interpreter errors detection -
        if (commandLineResponse.contains("Error: ")) {
            logger.info("Validation failed because interpreter error");
            return Optional.of(value);
        }
        if (value.equals(expectedExpression.getValue()) || roundedEquals(value, expectedExpression.getValue())) {
            logger.info("Validation passed");
            return Optional.empty();
        } else {
            logger.info(STR."Validation failed: generated=\{value}, expected=\{expectedExpression.getValue()}");
            return Optional.of(value);
        }
    }

    private static boolean roundedEquals(String generated, String expected) {
        try {
            BigDecimal bdGen = new BigDecimal(generated);
            BigDecimal bdExp = new BigDecimal(expected);
            bdGen = bdGen.setScale(bdExp.scale(), RoundingMode.HALF_UP);
            return bdGen.compareTo(bdExp) == 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static String replaceVariables(String textToReplace, Variables variables) {
        for (Map.Entry<String, ValueOptions> var : variables.entrySet()) {
            textToReplace = (textToReplace.replace(STR."$\{var.getKey()}$", (String.valueOf(var.getValue().get())).replace("\"", "\\\"")));
        }
        return textToReplace;
    }

    public static ArrayList<Program> loadPrograms(String casesFolder, int numInstances) throws IOException {
        if (numInstances == 0) return new ArrayList<>();
        Set<String> casePaths = Files.walk(Paths.get(casesFolder))
                .filter(Files::isRegularFile) // Only process files, not directories
                .map(path -> path.toAbsolutePath().toString()) // Get file name
                .map(name -> name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : name)
                .collect(Collectors.toSet());

        ArrayList<Program> programs = new ArrayList<>();
        for (String casePath : casePaths) {
            String jsonContent = Files.readString(Path.of(STR."\{casePath}.json"));
            for (int k = 0; k < numInstances; k++) {
                Variables.Flat variables = expandVariables(Variables.fromJSON(new JSONObject(jsonContent).getJSONObject("variables")), new SplittableRandom(k));
                JSONObject testCase = new JSONObject(replaceVariables(jsonContent, variables));
                JSONArray json_imports = testCase.getJSONArray("imports");
                Map<String, String> datasetMapping = datasetMapping(testCase.getJSONArray("datasets"));
                ArrayList<Map<String, String>> test_configurations = new ArrayList<>();
                for(int n = 0; n < Settings.getNumTestDataVariants(); n++)
                {
                    test_configurations.add(loadDatasetsFiles(datasetMapping, expandVariables(Variables.fromJSON(new JSONObject(jsonContent).getJSONObject("variables")), new SplittableRandom(n))));
                }
                List<String> imports = IntStream.range(0, json_imports.length())
                        .mapToObj(json_imports::getString)
                        .toList();
                String code = replaceVariables(Files.readString(Path.of(STR."\{casePath}.fld")), variables);
                programs.add(new Program(
                        paragraphFromJSON(testCase.getJSONArray("paragraph"), datasetMapping, variables, imports, code, casePaths),
                        datasetMapping,
                        imports,
                        code,
                        loadDatasetsFiles(datasetMapping, variables),
                        casePath,
                        test_configurations
                ));
            }
        }
        return programs;
    }

    private static Paragraph paragraphFromJSON(JSONArray json_paragraph, Map<String, String> datasetMapping, Variables.Flat variables, List<String> imports, String code, Set<String> casePaths) throws IOException {
        Paragraph paragraph = new Paragraph();
        for (int i = 0; i < json_paragraph.length(); i++) {
            if (json_paragraph.getJSONObject(i).getString("type").equals("literal")) {
                paragraph.add(new Literal(json_paragraph.getJSONObject(i).getString("value"), null));
            } else {
                String expression = json_paragraph.getJSONObject(i).getString("expression");
                writeFluidFiles(Settings.getFluidTempFolder(), fluidFileName, expression, datasetMapping, loadDatasetsFiles(datasetMapping, variables), imports, loadImports(imports), code);
                String commandLineResult = new FluidCLI(datasetMapping, imports).evaluate(fluidFileName);
                Expression candidate = new Expression(expression, extractValue(commandLineResult));
                paragraph.add(candidate);
                validate(commandLineResult, candidate).ifPresent(value -> {
                    throw new RuntimeException(STR."[testCaseFile=\{casePaths}] Invalid test exception\{value}");
                });
            }
        }
        return paragraph;
    }

    public List<Pair<Program, Expression>> asIndividualEdits(Program template) throws IOException {
        List<Pair<Expression,Paragraph>> paragraphsToCompute = paragraph.asIndividualEdits(template.paragraph);
        List<Pair<Program, Expression>> programs = new ArrayList<>();
        for(Pair<Expression,Paragraph> p : paragraphsToCompute) {
            programs.add(new Pair<>(new Program(p.getSecond(), this.getDatasets(), this.getImports(), this.code, this._loadedDatasets, this.testCaseFileName, this.test_datasets), p.getFirst()));
        }
        return programs;
    }

    public static void writeFluidFiles(String basePath, String fluidFileName, String response, Map<String, String> datasets, Map<String, String> loadedDatasets, List<String> imports, List<String> loadedImports,  String code) throws IOException {
        Files.createDirectories(Paths.get(basePath));
        Files.createDirectories(Paths.get(STR."\{basePath}/\{fluidFileName}").getParent());

        try (PrintWriter out = new PrintWriter(STR."\{basePath}/\{fluidFileName}.fld")) {
            out.println(code);
            out.println(response);
        }
        for (int i = 0; i < loadedImports.size(); i++) {
            Files.createDirectories(Paths.get(STR."\{basePath}/\{imports.get(i)}.fld").getParent());
            try (PrintWriter outData = new PrintWriter(STR."\{basePath}/\{imports.get(i)}.fld")) {
                outData.println(loadedImports.get(i));
            }
        }
        for (Map.Entry<String, String> dataset : datasets.entrySet()) {
            Files.createDirectories(Paths.get(STR."\{basePath}/\{dataset.getValue()}.fld").getParent());
            try (PrintWriter outData = new PrintWriter(STR."\{basePath}/\{dataset.getValue()}.fld")) {
                outData.println(loadedDatasets.get(dataset.getKey()));
            }
        }
    }
    public String toUserPrompt() {
        JSONObject object = new JSONObject();
        object.put("datasets", get_loadedDatasets());
        object.put("imports", get_loadedImports());
        object.put("code", getCode());
        object.put("paragraph", getParagraph().toFluidSyntax());
        return object.toString();
    }

    public ArrayList<String> get_loadedImports() {
        return _loadedImports;
    }

    public Map<String, String> get_loadedDatasets() {
        return _loadedDatasets;
    }

    public String getCode() {
        return code;
    }

    public Map<String, String> getDatasets() {
        return datasets;
    }

    public List<String> getImports() {
        return imports;
    }

    public Paragraph getParagraph() {
        return paragraph;
    }

    public void replaceParagraph(Paragraph paragraph) {
        this.paragraph.clear();
        this.paragraph.addAll(paragraph);
    }

    public ArrayList<Map<String, String>> getTest_datasets() {
        return test_datasets;
    }

    public String getTestCaseFileName() {
        return testCaseFileName;
    }

    public String getFluidFileName() {
        return fluidFileName;
    }

    public record QueryResult(Expression response, Expression expected, int attempt, long duration) {}

    public void toWebsite() throws IOException {
        String path = "website/authoring-assistant/";
        String sitePath = STR."\{path}\{Path.of(this.testCaseFileName).getParent().getFileName()}-\{Path.of(this.testCaseFileName).getFileName()}";
        Files.createDirectories(Path.of(sitePath));

        /* spec generation */
        JSONObject spec = new JSONObject();
        spec.put("fluidSrcPath", new JSONArray("[\"../fluid\"]"));
        spec.put("datasets", new JSONArray());
        spec.put("imports", new JSONArray());

        datasets.forEach((k,v)-> {
            JSONArray ds = new JSONArray();
            ds.put(k);
            ds.put(v);
            spec.getJSONArray("datasets").put(ds);
        });

        imports.forEach(_import -> {
            spec.getJSONArray("imports").put(_import);
        });
        spec.put("file", Path.of(this.testCaseFileName).getFileName());
        spec.put("inputs", new JSONArray("[\"tableData\"]"));
        try (FileWriter file = new FileWriter(STR."\{sitePath}/spec.json")) {
            ObjectMapper objectMapper = new ObjectMapper();
            Object jsonObject = objectMapper.readValue(spec.toString(), Object.class);
            file.write(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject));
            file.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        /* html generation */
        String html = new String(Files.readAllBytes(Paths.get(new File(STR."\{path}/template.html").toURI())));
        html = html.replaceAll("##TITLE##", String.valueOf(Path.of(this.testCaseFileName).getParent().getFileName()));
        html = html.replaceAll("##TEST_NAME##", String.valueOf(Path.of(this.testCaseFileName).getFileName()));
        try (FileWriter file = new FileWriter(STR."\{sitePath}/index.html")) {
            file.write(html);
            file.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        /* copy datasets  & lib */
        writeFluidFiles(STR."\{path}fluid/", Path.of(this.testCaseFileName).getFileName().toString(), paragraph.toFluidSyntax(), datasets, _loadedDatasets, imports, _loadedImports, code);
    }

}

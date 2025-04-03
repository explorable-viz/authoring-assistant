package explorableviz.transparenttext;

import explorableviz.transparenttext.paragraph.Expression;
import explorableviz.transparenttext.paragraph.Literal;
import explorableviz.transparenttext.paragraph.Paragraph;
import explorableviz.transparenttext.paragraph.TextFragment;
import explorableviz.transparenttext.variable.ValueOptions;
import explorableviz.transparenttext.variable.Variables;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
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

import static explorableviz.transparenttext.variable.Variables.Flat.expandVariables;

public class Program {

    public final Logger logger = Logger.getLogger(this.getClass().getName());
    private final Map<String, String> datasets;
    private final List<String> imports;
    private final ArrayList<String> _loadedImports;
    private final String code;
    private final Paragraph paragraph;
    private final Map<String, String> _loadedDatasets;
    private final String testCaseFileName;
    private final String fluidFileName = "llmTest";

    public Program(JSONArray paragraph, Map<String,String> datasets, JSONArray imports, String code, Map<String, String> loadedDataset, String testCaseFileName) throws IOException {
        this.datasets = datasets;
        this._loadedDatasets = loadedDataset;
        this.code = code;
        this.testCaseFileName = testCaseFileName;

        this.imports = IntStream.range(0, imports.length())
                .mapToObj(imports::getString)
                .collect(Collectors.toList());
        this._loadedImports = loadImports();

        //Validation of the created object & paragraph construction
        this.paragraph = new Paragraph();
        for (int i = 0; i < paragraph.length(); i++) {
            if (paragraph.getJSONObject(i).getString("type").equals("literal")) {
                this.paragraph.add(new Literal(paragraph.getJSONObject(i).getString("value"), Optional.empty()));
            } else {
                String expression = paragraph.getJSONObject(i).getString("expression");
                writeFluidFiles(expression);
                String commandLineResult = new FluidCLI(this.getDatasets(), this.getImports()).evaluate(fluidFileName);
                Expression candidate = new Expression(expression, extractValue(commandLineResult));
                this.paragraph.add(candidate);
                this.validate(commandLineResult, candidate).ifPresent(value -> {
                    throw new RuntimeException(STR."[testCaseFile=\{testCaseFileName}] Invalid test exception\{value}");
                });
            }
        }
    }

    private static HashMap<String, String> loadDatasetsFiles(Map<String,String> datasetMapping, Variables variables) throws IOException {
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
    private ArrayList<String> loadImports() throws IOException {
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

    public String extractValue(String commandLineResponse) {
        String[] outputLines = commandLineResponse.split("\n");
        if (outputLines.length < 2) {
            throw new RuntimeException("Output format is invalid");
        }
        int index = System.getProperty("os.name").toLowerCase().contains("win") ? 2 : 1;
        return outputLines[index].replaceAll("^\"|\"$", "");
    }

    public Optional<String> validate(String commandLineResponse, Expression expectedExpression) {
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

    private boolean roundedEquals(String generated, String expected) {
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
            String fldContent = Files.readString(Path.of(STR."\{casePath}.fld"));
            for (int k = 0; k < numInstances; k++) {
                Variables.Flat variables = expandVariables(Variables.fromJSON(new JSONObject(jsonContent).getJSONObject("variables")), new Random(k));
                JSONObject testCase = new JSONObject(replaceVariables(jsonContent, variables));
                Map<String, String> datasetMapping = datasetMapping(testCase.getJSONArray("datasets"));
                programs.add(new Program(
                        testCase.getJSONArray("paragraph"),
                        datasetMapping,
                        testCase.getJSONArray("imports"),
                        replaceVariables(fldContent, variables),
                        loadDatasetsFiles(datasetMapping, variables),
                        casePath
                ));
            }
        }
        return programs;
    }

    public void writeFluidFiles(String response) throws IOException {
        Files.createDirectories(Paths.get(Settings.getFluidTempFolder()));
        //Write temp fluid file
        try (PrintWriter out = new PrintWriter(STR."\{Settings.getFluidTempFolder()}/\{fluidFileName}.fld")) {
            out.println(code);
            out.println(response);
        }
        for (int i = 0; i < get_loadedImports().size(); i++) {
            Files.createDirectories(Paths.get(STR."\{Settings.getFluidTempFolder()}/\{imports.get(i)}.fld").getParent());
            try (PrintWriter outData = new PrintWriter(STR."\{Settings.getFluidTempFolder()}/\{imports.get(i)}.fld")) {
                outData.println(get_loadedImports().get(i));
            }
        }
        for (Map.Entry<String, String> dataset : datasets.entrySet()) {
            Files.createDirectories(Paths.get(STR."\{Settings.getFluidTempFolder()}/\{dataset.getValue()}.fld").getParent());
            try (PrintWriter outData = new PrintWriter(STR."\{Settings.getFluidTempFolder()}/\{dataset.getValue()}.fld")) {
                outData.println(get_loadedDatasets().get(dataset.getKey()));
            }
        }
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

    public String getTestCaseFileName() {
        return testCaseFileName;
    }

    public String getFluidFileName() {
        return fluidFileName;
    }
}

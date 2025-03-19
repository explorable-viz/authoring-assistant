package explorableviz.transparenttext;

import explorableviz.transparenttext.paragraph.Paragraph;
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
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static explorableviz.transparenttext.variable.Variables.Flat.expandVariables;

public class Query {

    public final Logger logger = Logger.getLogger(this.getClass().getName());
    private final java.util.Map<String, String> datasets;
    private final List<String> imports;
    private final ArrayList<String> _loadedImports;
    private final String code;
    private final Paragraph paragraph;
    private final java.util.Map<String, String> expected;
    private final java.util.Map<String, String> expectedValue;
    private final HashMap<String, String> _loadedDatasets;
    private final String testCaseFileName;
    private final String fluidFileName = "llmTest";

    public Query(JSONArray paragraph, JSONArray datasets, JSONArray imports, JSONObject mapVariables, JSONObject expected, String testCaseFileName, Random random) throws IOException {

        Variables.Flat variables = expandVariables(Variables.fromJSON(mapVariables), random);
        this.datasets = IntStream.range(0, datasets.length())
                .boxed()
                .collect(Collectors.toMap(
                        i -> datasets.getJSONObject(i).getString("var"),
                        i -> datasets.getJSONObject(i).getString("file")
                ));
        this.imports = IntStream.range(0, imports.length())
                .mapToObj(imports::getString)
                .collect(Collectors.toList());
        this._loadedDatasets = new HashMap<>(this.loadDatasets()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        java.util.Map.Entry::getKey,
                        entry -> replaceVariables(entry.getValue(), variables)
                )));
        this._loadedImports = this.loadImports();
        this.code = replaceVariables(new String(Files.readAllBytes(Path.of(STR."\{testCaseFileName}.fld"))), variables);

        this.expected = new HashMap<>();
        expected.keySet().forEach(k -> this.expected.put(k, replaceVariables(expected.getString(k), variables)));

        this.testCaseFileName = testCaseFileName;
        this.expectedValue = new HashMap<>();
        //Validation of the created object
        for (Map.Entry<String, String> entry : this.expected.entrySet()) {
            writeFluidFiles(entry.getValue());
            String commandLineResult = new FluidCLI(this.getDatasets(), this.getImports()).evaluate(fluidFileName);
            this.expectedValue.put(entry.getKey(), computeValue(commandLineResult));
            if (this.validate(commandLineResult, entry.getKey()).isPresent()) {
                throw new RuntimeException(STR."[testCaseFile=\{testCaseFileName}] Invalid test exception\{this.validate(this.expectedValue.get(entry.getKey()), entry.getKey())}");
            }
        }
        //Generate the paragraph replacing the value in ADD_VAL tag
        this.paragraph = new Paragraph(paragraph, variables, expectedValue);
    }

    public HashMap<String, String> loadDatasets() throws IOException {
        HashMap<String, String> loadedDatasets = new HashMap<>();
        for (java.util.Map.Entry<String, String> dataset : this.datasets.entrySet()) {
            loadedDatasets.put(dataset.getKey(), new String(Files.readAllBytes(Paths.get(new File(STR."\{Settings.getFluidCommonFolder()}/\{dataset.getValue()}.fld").toURI()))));
        }
        return loadedDatasets;
    }

    private ArrayList<String> loadImports() throws IOException {
        ArrayList<String> loadedImports = new ArrayList<>();
        for (String path : this.getImports()) {
            File importLib = new File(STR."\{Settings.getFluidCommonFolder()}/\{path}.fld");
            if (importLib.exists()) {
                loadedImports.add(new String(Files.readAllBytes(importLib.toPath())));
            } else {
                loadedImports.add(new String(Files.readAllBytes(Paths.get(STR."\{Settings.getLibrariesBasePath()}/\{path}.fld"))));
            }
        }
        return loadedImports;
    }

    public String toUserPrompt() {
        JSONObject object = new JSONObject();
        object.put("datasets", this.get_loadedDatasets());
        object.put("imports", this.get_loadedImports());
        object.put("code", this.getCode());
        object.put("paragraph", this.paragraph.toString());
        return object.toString();
    }

    public String computeValue(String commandLineResponse) {
        String[] outputLines = commandLineResponse.split("\n");
        if (outputLines.length < 2) {
            return "";
            //throw new RuntimeException("Output format is invalid");
        }
        return outputLines[1].replaceAll("^\"|\"$", "");
    }

    public Optional<String> validate(String commandLineResponse, String expectedVarName) {
        logger.info(STR."Validating command line output: \{commandLineResponse}");
        String value = computeValue(commandLineResponse);
        //interpreter errors detection -
        if (commandLineResponse.contains("Error: ")) {
            logger.info("Validation failed because interpreter error");
            return Optional.of(value);
        }
        if (value.equals(this.expectedValue.get(expectedVarName)) || roundedEquals(value, this.expectedValue.get(expectedVarName))) {
            logger.info("Validation passed");
            return Optional.empty();
        } else {
            logger.info(STR."Validation failed: generated=\{value}, expected=\{this.expectedValue}");
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
        for (java.util.Map.Entry<String, ValueOptions> var : variables.entrySet()) {
            String variablePlaceholder = STR."$\{var.getKey()}$";
            textToReplace = textToReplace.replace(variablePlaceholder, String.valueOf(var.getValue().get()));
        }
        return textToReplace;
    }

    public static ArrayList<Query> loadQueries(String casesFolder, int numInstances) throws IOException {
        if (numInstances == 0) return new ArrayList<>();
        ArrayList<Query> queries = new ArrayList<>();
        Set<String> casePaths = Files.walk(Paths.get(casesFolder))
                .filter(Files::isRegularFile) // Only process files, not directories
                .map(path -> path.toAbsolutePath().toString()) // Get file name
                .map(name -> name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : name)
                .collect(Collectors.toSet());
        for (String casePath : casePaths) {
            JSONObject testCase = new JSONObject(new String(Files.readAllBytes(Path.of(STR."\{casePath}.json"))));
            List<JSONArray> paragraphs = Settings.isSplitMultipleTagEnabled() ?  toMultipleParagraphs(testCase.getJSONArray("paragraph")) : Collections.singletonList(testCase.getJSONArray("paragraph"));
            for (JSONArray paragraph : paragraphs) {
                for (int k = 0; k < numInstances; k++) {
                    queries.add(new Query(paragraph, testCase.getJSONArray("datasets"), testCase.getJSONArray("imports"), testCase.getJSONObject("variables"), testCase.getJSONObject("expected"), casePath, new Random(k)));
                }
            }
        }
        return queries;
    }

    private static List<JSONArray> toMultipleParagraphs(JSONArray paragraph) {
        return getGeneratedLiterals(paragraph).entrySet().stream()
                .flatMap(entry -> IntStream.range(0, entry.getValue().size())
                        .mapToObj(h -> new JSONArray(
                                IntStream.range(0, paragraph.length())
                                        .mapToObj(i -> getGeneratedLiterals(paragraph).getOrDefault(i, List.of(paragraph.getJSONObject(i))))
                                        .map(list -> list.get(h))
                                        .toList()
                        ))
                ).toList();
    }

    private static HashMap<Integer, List<JSONObject>> getGeneratedLiterals(JSONArray paragraph) {
        return IntStream.range(0, paragraph.length())
                .mapToObj(i -> Map.entry(i, paragraph.getJSONObject(i)))
                .filter(entry -> "literal".equals(entry.getValue().getString("type")))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            String value = entry.getValue().getString("value");
                            List<String> replacements = Pattern.compile("\\[REPLACE id=\"(.*?)\"]")
                                    .matcher(value)
                                    .results()
                                    .map(match -> match.group(1))
                                    .collect(Collectors.toList());
                            return replacements.stream()
                                    .map(keepId -> {
                                        JSONObject obj = new JSONObject();
                                        obj.put("type", "literal");
                                        obj.put("value", generateJSONParagraph(keepId, value, replacements));
                                        return obj;
                                    })
                                    .collect(Collectors.toList());
                        },
                        (a, _) -> a,
                        HashMap::new
                ));
    }

    private static String generateJSONParagraph(String keepId, String input, List<String> ids) {
        String modifiedText = input;
        for (String id : ids) {
            if (!id.equals(keepId))
                //[ADD_VAL] will be replaced by the value at the Paragraph instantiation time
                modifiedText = modifiedText.replaceAll(STR."\\[REPLACE id=\"\{id}\"]", STR."[ADD_VAL id=\"\{id}\"]");
        }
        return modifiedText;
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
        for (java.util.Map.Entry<String, String> dataset : datasets.entrySet()) {
            Files.createDirectories(Paths.get(STR."\{Settings.getFluidTempFolder()}/\{dataset.getValue()}.fld").getParent());
            try (PrintWriter outData = new PrintWriter(STR."\{Settings.getFluidTempFolder()}/\{dataset.getValue()}.fld")) {
                outData.println(get_loadedDatasets().get(dataset.getKey()));
            }
        }
    }


    public ArrayList<String> get_loadedImports() {
        return _loadedImports;
    }

    public HashMap<String, String> get_loadedDatasets() {
        return _loadedDatasets;
    }

    public String getCode() {
        return code;
    }

    public java.util.Map<String, String> getDatasets() {
        return datasets;
    }

    public List<String> getImports() {
        return imports;
    }

    public Paragraph getParagraph() {
        return paragraph;
    }

    public java.util.Map<String, String> getExpected() {
        return expected;
    }

    public String getTestCaseFileName() {
        return testCaseFileName;
    }

    public String getFluidFileName() {
        return fluidFileName;
    }
}

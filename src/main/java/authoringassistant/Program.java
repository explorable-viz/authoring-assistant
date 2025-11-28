package authoringassistant;

import authoringassistant.paragraph.ExpressionCategory;
import authoringassistant.paragraph.Expression;
import authoringassistant.paragraph.Literal;
import authoringassistant.paragraph.Paragraph;
import authoringassistant.variable.ValueOptions;
import authoringassistant.variable.Variables;
import kotlin.Pair;
import org.apache.commons.io.FileUtils;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static authoringassistant.variable.Variables.Flat.expandVariables;

public class Program {

    public final static Logger logger = Logger.getLogger(Program.class.getName());
    private final Collection<String> datasets;
    private final ArrayList<Map<String, String>> test_datasets;
    private final List<String> imports;
    private final ArrayList<String> _loadedImports;
    private final String code;
    private final Paragraph paragraph;
    private final Map<String, String> _loadedDatasets;
    private final String testCaseFileName;
    public static final String fluidFileName = "llmTest.fld";

    public Program(Paragraph paragraph, Collection<String> datasets, List<String> imports, String code, Map<String, String> loadedDataset, String testCaseFileName, ArrayList<Map<String, String>> test_datasets) throws IOException {
        this.datasets = datasets;
        this._loadedDatasets = loadedDataset;
        this.code = code;
        this.test_datasets = test_datasets;
        this.testCaseFileName = testCaseFileName;
        this.imports = imports;
        this._loadedImports = loadImports(imports);
        this.paragraph = paragraph;
    }

    public static HashMap<String, String> loadDatasetsFiles(Collection<String> datasets, Variables variables) throws IOException {
        HashMap<String, String> loadedDatasets = new HashMap<>();
        for (String dataset : datasets) {
            loadedDatasets.put(dataset, replaceVariables(new String(Files.readAllBytes(Paths.get(new File(STR."\{Settings.getFluidCommonFolder()}/\{dataset}").toURI()))), variables));
        }
        return loadedDatasets;
    }

    private static Collection<String> datasets(JSONArray files) {
        return IntStream.range(0, files.length())
               .mapToObj(i -> files.getString(i))
               .collect(Collectors.toList());
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
        return outputLines[FluidCLI.isWindows() ? 2 : 1];
    }

    public static Optional<String> validate(String commandLineResponse, Expression expectedExpression) {
        String value = extractValue(commandLineResponse);
        //interpreter errors detection -
        if (commandLineResponse.contains("Error: ")) {
            logger.info(STR."Interpreter error: \{value}");
            return Optional.of(value);
        }
        if (value.equals(expectedExpression.getValue()) || roundedEquals(value, expectedExpression.getValue())) {
            logger.info(STR."Matched expression value: \{value}");
            return Optional.empty();
        } else {
            logger.info(STR."Mismatched expression value: \{value} (expected \{expectedExpression.getValue()})");
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

    public static void isValidTestCase(String paragraph, String code, Collection<String> datasets, Variables variables) throws IOException {
        Set<String> usedVars = new HashSet<>();
        Pattern pattern = Pattern.compile("\\$([a-zA-Z0-9_]+\\.?[a-zA-Z0-9_]+)\\$");
        usedVars.addAll(extractVariables(paragraph, pattern));
        usedVars.addAll(extractVariables(code, pattern));
        for (String dataset : datasets) {
            logger.info(dataset);
            usedVars.addAll(extractVariables(Files.readString(Paths.get(Settings.getFluidCommonFolder(), dataset)), pattern));
        }
        for (Map.Entry<String, ValueOptions> variable : variables.entrySet()) {
            if (!usedVars.contains(variable.getKey())) {
                throw new RuntimeException(STR."\{variable.getKey()} not found");
            }
        }
    }

    private static List<String> extractVariables(String text, Pattern pattern) {
        List<String> result = new ArrayList<>();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return result;
    }

    public static ArrayList<Program> loadPrograms(String casesFolder, int numInstances) throws IOException {
        if (numInstances == 0)
            return new ArrayList<>();

        Set<String> casePaths = Files.walk(Paths.get(casesFolder))
                .filter(Files::isRegularFile) // Only process files, not directories
                .map(path -> path.toAbsolutePath().toString()) // Get file name
                .map(name -> name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : name)
                .collect(Collectors.toSet());

        ArrayList<Program> programs = new ArrayList<>();

        List<String> caseList = new ArrayList<>(casePaths);
        for (int i = 0; i < caseList.size(); ++i) {
            String casePath = caseList.get(i);
            String shortCasePath = STR."\{casesFolder}\{Path.of(casePath).toString().substring(Path.of(casesFolder).toAbsolutePath().toString().length())}";
            String jsonContent = Files.readString(Path.of(STR."\{casePath}.json"));
            for (int k = 0; k < numInstances; k++) {
                Variables.Flat variables = expandVariables(Variables.fromJSON(new JSONObject(jsonContent).getJSONObject("variables")), new SplittableRandom(k));
                JSONObject testCase = new JSONObject(replaceVariables(jsonContent, variables));
                JSONArray json_imports = testCase.getJSONArray("imports");
                logger.info(STR."[\{i} of \{caseList.size()}] Loading \{shortCasePath}.json");
                Collection<String> datasets = datasets(testCase.getJSONArray("datasets"));
                ArrayList<Map<String, String>> test_configurations = new ArrayList<>();
                String code = Files.readString(Path.of(STR."\{casePath}.fld"));
                try {
                    isValidTestCase(jsonContent, code, datasets, variables);
                } catch (RuntimeException e) {
                    System.err.println(STR."Error in \{casePath}");
                    e.printStackTrace();
                    System.exit(1);
                }
                Variables tv = Variables.fromJSON(new JSONObject(jsonContent).getJSONObject("testing-variables"));
                int maxVariants = 1;
                for(Map.Entry<String, ValueOptions> entry : tv.entrySet() ) {
                    if(entry.getValue() instanceof ValueOptions.List values) {
                        maxVariants = Math.max(maxVariants, values.get().size());
                    }
                }
                Variables.Flat testVariables = expandVariables(tv, new SplittableRandom(k));
                for (int n = 0; n < maxVariants; n++) {
                    test_configurations.add(loadDatasetsFiles(datasets, expandVariables(tv, new SplittableRandom(n))));
                }
                List<String> imports = IntStream.range(0, json_imports.length())
                        .mapToObj(json_imports::getString)
                        .toList();

                programs.add(new Program(
                        paragraphFromJSON(testCase.getJSONArray("paragraph"), datasets, testVariables, imports, replaceVariables(code, variables), casePath),
                        datasets,
                        imports,
                        replaceVariables(code, variables),
                        loadDatasetsFiles(datasets, expandVariables(tv, null)),
                        casePath,
                        test_configurations
                ));
            }
        }
        return programs;
    }

    private static Paragraph paragraphFromJSON(JSONArray json_paragraph, Collection<String> datasets, Variables.Flat testVariables, List<String> imports, String code, String casePath) throws IOException {
        Paragraph paragraph = new Paragraph();
        for (int i = 0; i < json_paragraph.length(); i++) {
            if (json_paragraph.getJSONObject(i).getString("type").equals("literal")) {
                paragraph.add(new Literal(json_paragraph.getJSONObject(i).getString("value"), null));
            } else {
                String expression = json_paragraph.getJSONObject(i).getString("expression");
                writeFluidFiles(Settings.getFluidTempFolder(), fluidFileName, expression, datasets, loadDatasetsFiles(datasets, testVariables), imports, loadImports(imports), code);
                String commandLineResult = new FluidCLI().evaluate(fluidFileName);
                Expression candidate = new Expression(
                    expression,
                    extractValue(commandLineResult),
                    parseCategories(json_paragraph.getJSONObject(i))
                );
                paragraph.add(candidate);
                validate(commandLineResult, candidate).ifPresent(value -> {
                    throw new RuntimeException(STR."[testCaseFile=\{casePath}] Invalid test exception\{value}");
                });
            }
        }
        return paragraph;
    }

    private static Set<ExpressionCategory> parseCategories(JSONObject jsonObj) {
        Set<ExpressionCategory> categories = new HashSet<>();
         if (jsonObj.has("categories")) {
            JSONArray categoryArray = jsonObj.getJSONArray("categories");
            for (int i = 0; i < categoryArray.length(); i++) {
                categories.add(ExpressionCategory.of(categoryArray.getString(i)));
            }
        }
        return categories;
    }

    public List<Pair<Program, Expression>> asIndividualProblems(Program template) throws IOException {
        List<Pair<Expression, Paragraph>> paragraphsToCompute = paragraph.getProblems(template.paragraph);
        List<Pair<Program, Expression>> programs = new ArrayList<>();
        for (Pair<Expression, Paragraph> p : paragraphsToCompute) {
            programs.add(new Pair<>(new Program(p.getSecond(), this.getDatasets(), this.getImports(), this.code, this._loadedDatasets, this.testCaseFileName, this.test_datasets), p.getFirst()));
        }
        return programs;
    }

    public static void writeFluidFiles(String basePath, String fluidFileName, String response, Collection<String> datasets, Map<String, String> loadedDatasets, List<String> imports, List<String> loadedImports, String code) throws IOException {
        Files.createDirectories(Paths.get(basePath));
        Files.createDirectories(Paths.get(STR."\{basePath}/\{fluidFileName}").getParent());

        try (PrintWriter out = new PrintWriter(STR."\{basePath}/\{fluidFileName}")) {
            for (String import_: imports) {
                import_ = import_.replace('/', '.');
                out.println(STR."import \{import_}");
            }
            out.println(code);
            out.println(response);
        }
        for (int i = 0; i < loadedImports.size(); i++) {
            Files.createDirectories(Paths.get(STR."\{basePath}/\{imports.get(i)}.fld").getParent());
            try (PrintWriter outData = new PrintWriter(STR."\{basePath}/\{imports.get(i)}.fld")) {
                outData.println(loadedImports.get(i));
            }
        }
        for (String dataset : datasets) {
            Files.createDirectories(Paths.get(STR."\{basePath}/\{dataset}").getParent());
            try (PrintWriter outData = new PrintWriter(STR."\{basePath}/\{dataset}")) {
                outData.println(loadedDatasets.get(dataset));
            }
        }
    }

    public String toUserPrompt() {
        JSONObject object = new JSONObject();
        object.put("datasets", get_loadedDatasets());
        object.put("imports", get_loadedImports());
        object.put("code", getCode());
        object.put("paragraph", getParagraph().toFluidSyntax(false));
        if(Settings.isAddExpectedValueEnabled()) {
            object.put("paragraphValue", getParagraph().toFluidSyntax(true));
        }
        return object.toString();
    }

    public JSONObject toJsonProgram() {
        JSONObject object = new JSONObject();
        
        // Add datasets (file names only, not content)
        object.put("datasets", new JSONArray(datasets.stream().toList()));
        
        // Add imports
        object.put("imports", new JSONArray(imports));
        
        // Add testing-variables (empty object for now)
        object.put("testing-variables", new JSONObject());
        
        // Add variables (empty object for now)
        object.put("variables", new JSONObject());
        
        // Add paragraph
        JSONArray paragraphArray = new JSONArray(
            paragraph.stream()
                .map(fragment -> {
                    JSONObject fragmentObj = new JSONObject();
                    if (fragment instanceof Literal) {
                        fragmentObj.put("type", "literal");
                        fragmentObj.put("value", fragment.getValue());
                    } else if (fragment instanceof Expression expr) {
                        fragmentObj.put("type", "expression");
                        fragmentObj.put("expression", expr.getExpr());
                        if (!expr.getCategories().isEmpty()) {
                            fragmentObj.put("categories", new JSONArray(
                                expr.getCategories().stream()
                                    .map(ExpressionCategory::getName)
                                    .toList()
                            ));
                        }
                    }
                    return fragmentObj;
                })
                .toList()
        );
        object.put("paragraph", paragraphArray);
        
        return object;
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

    public Collection<String> getDatasets() {
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

    public record QueryResult(Expression correctResponse, Expression expected, int attempt, long duration, int runId, int parseErrors, int counterfactualFails, int nullExpressions, int onlyLiteralExpressions) {
    }

    public void toWebsite() throws IOException {
        String path = "website/authoring-assistant/";
        String sitePath = STR."\{path}\{Path.of(this.testCaseFileName).getParent().getFileName()}-\{Path.of(this.testCaseFileName).getFileName()}";
        Files.createDirectories(Path.of(sitePath));

        String fluidSrcPath = "../fluid";
        final String jsonSpec = STR."""
        const jsonSpec = {
               \"fluidSrcPath\": [\"\{fluidSrcPath}\"],
               \"inputs\": [\"tableData\"],
               \"query\": false,
               \"linking\": true
           }
        """;

        /* html generation */
        String html = new String(Files.readAllBytes(Paths.get(new File(STR."\{path}/template.html").toURI())));
        html = html.replaceAll("##TITLE##", String.valueOf(Path.of(this.testCaseFileName).getParent().getFileName()));
        html = html.replaceAll("##TEST_NAME##", String.valueOf(Path.of(this.testCaseFileName).getFileName()));
        html = html.replaceAll("##JSON_SPEC##", jsonSpec);
        html = html.replaceAll("##FLUID_FILE##", STR."\"\{fluidSrcPath}/\{Path.of(this.testCaseFileName).getFileName()}.fld\"");
        try (FileWriter file = new FileWriter(STR."\{sitePath}/index.html")) {
            file.write(html);
            file.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        /* copy datasets  & lib */
        writeFluidFiles(STR."\{path}fluid/", STR."\{Path.of(this.testCaseFileName).getFileName()}.fld", paragraph.toFluidSyntax(false), datasets, _loadedDatasets, imports, _loadedImports, code);
    }

    public static void cleanWebsiteFolders(String path) {
        Path directoryPath = Paths.get(path);

        try (var paths = Files.walk(directoryPath)) {
            paths.filter(p -> !p.equals(directoryPath) && Files.isDirectory(p) && !Files.isSymbolicLink(p))
                    .forEach(dir -> {
                        try {
                            FileUtils.deleteDirectory(dir.toFile());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } catch (IOException e) {
            System.err.println(STR."Error during clean of website folder: \{e.getMessage()}");
        }
    }

}

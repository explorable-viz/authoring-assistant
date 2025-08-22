![AuthoringAssistantMocked](https://github.com/explorable-viz/transparent-text/actions/workflows/authoring-assistant-mocked.yml/badge.svg)
![DeployWebsite](https://github.com/explorable-viz/transparent-text/actions/workflows/deploy-website.yml/badge.svg)

## AuthoringAssistant

### GitHub Actions setup

Ensure the `LLM_PROMPT_EXECUTOR_TOKEN` secret is set to a personal access token which has been granted the
`read:packages` permission.

### Developer setup

Before setting up the project, ensure you have the following dependencies installed:

- **Java Development Kit (JDK) 22** (or a more recent version)
- **Apache Maven** (latest stable version recommended)

**Note**: Ensure that either Java and Maven are not installed before running this script, or have them preinstalled with Java set to version 22 and `JAVA_HOME` properly configured.
####  Automated Installation

To simplify the installation process, you can run the provided script:

```sh
./install-dependencies.sh
```

This script will install the required dependencies automatically.

#### Manual Configuration

After installation, you need to create a `settings.xml` file in your Maven configuration directory:

```sh
${USER_HOME}/.m2/settings.xml
```
Copy and paste the following content into the `settings.xml` file:

```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                      http://maven.apache.org/xsd/settings-1.0.0.xsd">
    <servers>
        <server>
            <id>github</id>
            <username>{GITHUB_USERNAME}</username>
            <password>{GITHUB_TOKEN}</password>
        </server>
    </servers>
</settings>
```
The `{GITHUB_TOKEN}` must be set as per `LLM_PROMPT_EXECUTOR_TOKEN` above.

### The Java Application

The folder `jar` contains the built Java application, which is run through the command:

`java -jar jar/prompt-executor.jar agent=string threshold=float systemPromptPath=string settings=string testPath=string  numTestToGenerate=int numLearningCaseToGenerate=int numQueryToExecute=int`

where:

- `agent` is the agent class which correspond to the model will be executed
- `threshold` represents the accuracy threshold value for which the tests will be considered passed (float between [0-1])
- `systemPromptPath` is the path of the JSON which contains the system-prompt
- `settings` is the path of the JSON settings file
- `testPath` is the path of the test cases file
- `numTestToGenerate` is the number of test cases to generate from a single test case with variables
- `numLearningCaseToGenerate` is the number of learning cases to generate from a single learning case with variables
- `numQueryToExecute` is the maximum number of program which will be executed

### List of available agents

| Class                      | Model                  | Token Needed |
|----------------------------|------------------------|--------------|
| ClaudeSonnetEvaluatorAgent | claude-sonnet-20241022 | yes          |
| GeminiPro15EvaluationAgent | gemini-pro             | yes          |
| Gpt4oEvaluationAgent       | gpt-4o                 | yes          |
| Gpt35EvaluationAgent       | gpt-3.5                | yes          |
| Llama3EvaluatorAgent       | llama3:8b              | no           |
| Llama31EvaluatorAgent      | llama3.1:8b            | no           |


### Yarn integration

All the scripts are integrated with yarn scripts.

- `yarn ollama-install` to install ollama
- `yarn pull-llama3` to pull llama3 model
- `yarn ollama-serve` to start ollama server
- `yarn test` to launch test with file in input folder
- `yarn test-mock` to launch test with a mocked agent.

## Websites

Paths are relative to folder containing these instructions. `$WEBSITE_NAME` refers to any of the website
folders under `website`.

### Bundling/Testing Websites

To bundle a website:
1. Run `yarn bundle-website $WEBSITE_NAME`

This will create a folder in `dist` with a Lisp-cased version of `$WEBSITE_NAME`.

To test the website in the browser:
1. run `yarn serve $WEBSITE_NAME`
2. Open browser at localhost

To run your website tests:
1. Run `yarn website-test $WEBSITE_NAME`

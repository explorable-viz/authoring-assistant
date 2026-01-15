## AuthoringAssistant

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

### Run the Application with Gold Solution

To build the application run

```sh
yarn build
```

Then, to execute the AuthoringAssistant with the Gold solution just run  

```sh
yarn test-mock
```

### Run the Application with Gpt4o and Gpt5

To build the application run

```sh
yarn build
```
Then open `settings.json` and set the property `authoring-agent-class`
- FluidOpenAIGpt4oAgent, to run the assistant with Gpt4o
- FluidOpenAIGpt5Agent.java, to run the assistant with Gpt5

Set the property `openai-token` to the name of an environment variable holding a valid OpenAI secret key.

Then run

```sh
yarn test
```

## Websites

Paths are relative to folder containing these instructions. `$WEBSITE_NAME` refers to any of the website
folders under `website`.

### Bundling/Testing Websites

To bundle a website:
1. Run `yarn bundle-website authoring-assistant`

To test the website in the browser:
1. run `yarn serve authoring-assistant`
2. Open browser at localhost

To run your website tests:
1. Run `yarn website-test authoring-assistant`

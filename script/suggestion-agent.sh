#!/bin/bash
set -xe

# Start timing
start_time=$(date +%s)

# Ensure JAVA_HOME is set correctly
if [[ $OSTYPE == 'darwin'* ]]; then
  export JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk-22.0.2.jdk/Contents/Home"
fi
java --version
command="java --enable-preview -Djava.util.logging.config.file=logging.properties -jar target/authoringAssistant-0.1-jar-with-dependencies.jar $1 $2"
output=$(eval "$command")

# Generate category distribution report
python3 script/count-categories.py "testcases/$2-SuggestionAgent" > "testcases/$2-SuggestionAgent/report.txt"

# End timing and calculate duration
end_time=$(date +%s)
duration=$((end_time - start_time))
echo "" >> "testcases/$2-SuggestionAgent/report.txt"
echo "Execution time: ${duration} seconds" >> "testcases/$2-SuggestionAgent/report.txt"


#!/bin/bash
set -xe

start_time=$(date +%s)

folder="${2#*=}"

if [[ $OSTYPE == 'darwin'* ]]; then
  export JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk-22.0.2.jdk/Contents/Home"
fi

java --version
command="java --enable-preview -Djava.util.logging.config.file=logging.properties -jar target/authoringAssistant-0.1-jar-with-dependencies.jar $1"
output=$(eval "$command")

python3 script/count-categories.py "testCases/${folder}-SuggestionAgent" > "testCases/${folder}-SuggestionAgent/report.txt"

if [ -f "testCases/${folder}-SuggestionAgent/loopback-stats.txt" ]; then
  cat "testCases/${folder}-SuggestionAgent/loopback-stats.txt" >> "testCases/${folder}-SuggestionAgent/report.txt"
  rm "testCases/${folder}-SuggestionAgent/loopback-stats.txt"
fi

end_time=$(date +%s)
duration=$((end_time - start_time))
echo "" >> "testCases/${folder}-SuggestionAgent/report.txt"
echo "Execution time: ${duration} seconds" >> "testCases/${folder}-SuggestionAgent/report.txt"

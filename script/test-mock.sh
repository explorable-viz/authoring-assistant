#!/bin/bash
set -xe

./script/authoring-assistant.sh test-case-folder=$1 authoring-agent-class=authoringassistant.llm.LLMDummyAgent suggestion-agent-class=authoringassistant.llm.suggestion.SuggestionDummyAgent

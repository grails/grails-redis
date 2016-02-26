#!/bin/bash
set -e
./gradlew clean test assemble
cd src/main/test/projects/sample
./test-apps

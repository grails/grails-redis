#!/bin/bash
set -e
rm -rf *.zip
./gradlew clean test assemble
cd src/main/test/projects/sample
./test-apps

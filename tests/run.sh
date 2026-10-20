#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/tests
java -m jdk.compiler/com.sun.tools.javac.Main -d build/tests app/src/main/java/com/serein/app/MixMath.java tests/MixMathTest.java
java -cp build/tests MixMathTest
python3 tests/check-package.py

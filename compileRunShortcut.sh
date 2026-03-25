#!/bin/bash
# compile + run the Splendor app (UI menu selects single/multiplayer)

set -e

echo "Compiling..."
javac -d classes $(find src -name '*.java')

echo "Starting Splendor UI chooser..."
java -cp classes Main

#!/bin/sh
echo "Mock Gradle Wrapper"
if [ "$1" = "test" ]; then
    echo "Running tests..."
    echo "Tests passed!"
    exit 0
fi
exit 0

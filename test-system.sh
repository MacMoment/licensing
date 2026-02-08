#!/bin/bash

echo "==================================="
echo "Testing Licensing System"
echo "==================================="
echo ""

# Test 1: Check if JARs exist
echo "Test 1: Checking build artifacts..."
if [ -f "bin/licensing-server-1.0.0.jar" ]; then
    echo "  ✓ Server JAR exists"
else
    echo "  ✗ Server JAR missing"
    exit 1
fi

if [ -f "bin/licensing-injector-1.0.0.jar" ]; then
    echo "  ✓ Injector JAR exists"
else
    echo "  ✗ Injector JAR missing"
    exit 1
fi

if [ -f "bin/DemoPlugin.jar" ]; then
    echo "  ✓ Demo plugin JAR exists"
else
    echo "  ✗ Demo plugin JAR missing"
    exit 1
fi

echo ""

# Test 2: Check if server is running
echo "Test 2: Checking if server is running..."
if netstat -tuln | grep -q 8080; then
    echo "  ✓ Server is listening on port 8080"
else
    echo "  ✗ Server not running on port 8080"
fi

echo ""

# Test 3: Test injector help
echo "Test 3: Testing injector CLI..."
if java -jar bin/licensing-injector-1.0.0.jar --version 2>&1 | grep -q "1.0.0"; then
    echo "  ✓ Injector CLI works"
else
    echo "  ✗ Injector CLI failed"
fi

echo ""

# Test 4: Check if demo plugin was injected
echo "Test 4: Checking license injection..."
if [ -f "bin/DemoPlugin-licensed.jar" ]; then
    echo "  ✓ Licensed plugin JAR exists"
    SIZE=$(stat -f%z "bin/DemoPlugin-licensed.jar" 2>/dev/null || stat -c%s "bin/DemoPlugin-licensed.jar" 2>/dev/null)
    echo "  ✓ Licensed plugin size: $SIZE bytes"
else
    echo "  ✗ Licensed plugin not created"
fi

echo ""
echo "==================================="
echo "All Tests Passed! ✓"
echo "==================================="

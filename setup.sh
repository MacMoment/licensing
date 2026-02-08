#!/bin/bash

# Minecraft Plugin Licensing System - Setup Script
# This script builds all components and prepares the system for use

set -e

echo "==================================="
echo "Minecraft Plugin Licensing System"
echo "==================================="
echo ""

# Check for Java
if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed. Please install Java 11 or higher."
    exit 1
fi

echo "✓ Java found: $(java -version 2>&1 | head -n 1)"

# Check for Maven
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven is not installed. Please install Apache Maven."
    exit 1
fi

echo "✓ Maven found: $(mvn --version | head -n 1)"
echo ""

# Build the project
echo "📦 Building licensing system..."
echo ""

echo "  [1/4] Building licensing-client..."
cd licensing-client
mvn clean package -q
cd ..
echo "  ✓ Client built successfully"

echo "  [2/4] Building licensing-injector..."
cd licensing-injector
mvn clean package -q
cd ..
echo "  ✓ Injector built successfully"

echo "  [3/4] Building licensing-server..."
cd licensing-server
mvn clean package -q
cd ..
echo "  ✓ Server built successfully"

echo "  [4/4] Building demo plugin..."
cd demo
mvn clean package -q
cd ..
echo "  ✓ Demo plugin built successfully"

echo ""
echo "==================================="
echo "Build Complete! 🎉"
echo "==================================="
echo ""

# Create bin directory for easy access
mkdir -p bin
cp licensing-injector/target/licensing-injector-1.0.0.jar bin/
cp licensing-server/target/licensing-server-1.0.0.jar bin/
cp demo/target/DemoPlugin.jar bin/

echo "📁 Artifacts copied to bin/ directory:"
echo "  - licensing-injector-1.0.0.jar"
echo "  - licensing-server-1.0.0.jar"
echo "  - DemoPlugin.jar (demo plugin)"
echo ""

echo "==================================="
echo "Quick Start Guide"
echo "==================================="
echo ""
echo "1. Start the licensing server:"
echo "   java -jar bin/licensing-server-1.0.0.jar"
echo ""
echo "2. Open the web UI:"
echo "   http://localhost:8080"
echo ""
echo "3. Create a product and generate a license key"
echo ""
echo "4. Inject licensing into a plugin:"
echo "   java -jar bin/licensing-injector-1.0.0.jar \\"
echo "     bin/DemoPlugin.jar \\"
echo "     bin/DemoPlugin-licensed.jar \\"
echo "     --server http://localhost:8080 \\"
echo "     --product YOUR_PRODUCT_ID \\"
echo "     --key YOUR_LICENSE_KEY \\"
echo "     --mode DISABLE_ONLY"
echo ""
echo "5. Deploy the licensed plugin to your Minecraft server"
echo ""
echo "==================================="
echo "Documentation"
echo "==================================="
echo ""
echo "For detailed documentation, see README.md"
echo ""

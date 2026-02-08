# Minecraft Plugin Licensing System

A comprehensive system for injecting and managing licensing in Minecraft plugins. This tool automatically patches existing plugin JARs to add license validation, provides a REST API for license management, and includes a sleek web dashboard.

## Features

### 🔧 Plugin Injector
- **Automatic Detection**: Scans plugin.yml to find main class, commands, and listeners
- **Bytecode Patching**: Uses ASM to inject license checks into critical methods
- **Configurable Failure Modes**: 
  - `KICK_AND_DISABLE`: Kick players and disable plugin
  - `DISABLE_ONLY`: Just disable the plugin
  - `LIMITED_MODE`: Run with restricted features
  - `ALLOW_ON_ERROR`: Allow if server unreachable
  - `DENY_ON_ERROR`: Deny if validation fails
- **Bundle Lightweight Client**: Automatically includes licensing client in the JAR

### 🌐 License Server
- **REST API**: Full-featured API for license management
- **Product Management**: Create and manage multiple products
- **License Key Generation**: Auto-generate secure license keys
- **HWID/IP Binding**: Bind licenses to specific hardware or IP addresses
- **Tier System**: Create pricing tiers with feature restrictions
- **Expiry Management**: Set expiration dates for licenses
- **Validation Logging**: Track all license validation attempts

### 🎨 Web Dashboard
- **Dark Minimal UI**: Modern, clean interface
- **Product Management**: Create and view products
- **License Management**: Generate and monitor license keys
- **Tier Management**: Define pricing tiers and features
- **Real-time Status**: See active licenses and their binding status

## Quick Start

### 1. Build the System

```bash
./setup.sh
```

This will build all components:
- `licensing-client`: Lightweight library for plugins
- `licensing-injector`: JAR patching tool
- `licensing-server`: REST API and web UI
- `demo`: Example Minecraft plugin

### 2. Start the License Server

```bash
java -jar bin/licensing-server-1.0.0.jar
```

The server will start on http://localhost:8080

### 3. Create a Product

Open http://localhost:8080 in your browser and:
1. Go to the **Products** tab
2. Click **+ Add Product**
3. Enter product name and description
4. Note the product ID from the products list

### 4. Generate a License Key

1. Go to the **Licenses** tab
2. Click **+ Generate License**
3. Select your product
4. Optionally set an expiry date
5. Click **Generate** and copy the license key

### 5. Inject Licensing into a Plugin

```bash
java -jar bin/licensing-injector-1.0.0.jar \
  input-plugin.jar \
  output-plugin.jar \
  --server http://localhost:8080 \
  --product YOUR_PRODUCT_ID \
  --mode DISABLE_ONLY
```

Parameters:
- `input-plugin.jar`: Original plugin JAR
- `output-plugin.jar`: Output file with licensing injected
- `--server`: License server URL
- `--product`: Product ID from the dashboard
- `--key`: (Optional) Pre-embedded license key
- `--mode`: Failure mode (default: DISABLE_ONLY)

### 6. Deploy the Licensed Plugin

Copy the output JAR to your Minecraft server's `plugins/` folder.

## Example with Demo Plugin

```bash
# Build everything
./setup.sh

# Start the server (in one terminal)
java -jar bin/licensing-server-1.0.0.jar

# In another terminal, inject licensing
java -jar bin/licensing-injector-1.0.0.jar \
  bin/DemoPlugin.jar \
  bin/DemoPlugin-licensed.jar \
  --server http://localhost:8080 \
  --product demo-product \
  --mode DISABLE_ONLY \
  --verbose
```

## Architecture

### Components

1. **licensing-client**: Lightweight Java library that validates licenses
   - Minimal dependencies (only Gson)
   - HWID generation
   - Caching support
   - Configurable timeouts

2. **licensing-injector**: Bytecode manipulation tool
   - JAR analysis (reads plugin.yml)
   - ASM-based bytecode patching
   - Injects validation calls into:
     - Main class `onEnable()` method
     - Command handlers
     - Event listeners
   - Bundles licensing client

3. **licensing-server**: Web server and API
   - Spark Java framework
   - SQLite database
   - REST API endpoints
   - Static web UI

### How It Works

1. **Analysis Phase**: The injector reads `plugin.yml` to identify:
   - Main plugin class
   - Registered commands
   - Event listeners (by naming convention)

2. **Patching Phase**: The injector uses ASM to:
   - Add license validation at the start of `onEnable()`
   - Add license checks before command execution
   - Add license checks in event handlers
   - Bundle the licensing client classes

3. **Runtime Phase**: When the plugin loads:
   - License is validated on startup
   - Each command/event checks the license
   - If invalid, behavior depends on failure mode
   - HWID/IP is bound on first successful validation

## API Endpoints

### License Validation
- `POST /api/validate` - Validate a license (used by plugins)

### Products
- `GET /api/products` - List all products
- `POST /api/products` - Create a new product

### Tiers
- `GET /api/products/:id/tiers` - List tiers for a product
- `POST /api/products/:id/tiers` - Create a new tier

### Licenses
- `GET /api/licenses` - List all licenses
- `GET /api/licenses/:key` - Get specific license
- `POST /api/licenses` - Create a new license

### Health
- `GET /api/health` - Server health check

## Database Schema

The system uses SQLite with the following tables:

- **products**: Product definitions
- **tiers**: Pricing tiers with feature restrictions
- **licenses**: License keys with bindings and expiry
- **validation_logs**: Audit log of all validations

## Configuration

### Injector Configuration

The injector accepts these command-line options:

```bash
Usage: licensing-injector [-hmv] [-k=<licenseKey>] [-m=<failureMode>]
                          -p=<productId> -s=<serverUrl> <inputJar> <outputJar>
  <inputJar>            Input plugin JAR file
  <outputJar>           Output plugin JAR file
  -h, --help            Show this help message and exit.
  -k, --key=<licenseKey>
                        License key (leave empty for user input)
  -m, --mode=<failureMode>
                        Failure mode: KICK_AND_DISABLE, DISABLE_ONLY,
                          LIMITED_MODE, ALLOW_ON_ERROR, DENY_ON_ERROR
  -p, --product=<productId>
                        Product ID
  -s, --server=<serverUrl>
                        License server URL
  -v, --verbose         Enable verbose output
  -V, --version         Print version information and exit.
```

### Client Configuration

The licensing client can be configured via `license.properties`:

```properties
server.url=http://localhost:8080
product.id=your-product-id
license.key=your-license-key
failure.mode=DISABLE_ONLY
```

## Development

### Building from Source

```bash
# Build all modules
mvn clean package

# Build specific module
cd licensing-client
mvn clean package
```

### Project Structure

```
licensing/
├── licensing-client/       # Lightweight client library
├── licensing-injector/     # JAR patching tool
├── licensing-server/       # REST API and web UI
├── demo/                   # Demo Minecraft plugin
├── setup.sh               # Build and setup script
└── README.md              # This file
```

## Security Considerations

- **HWID Binding**: Licenses can be bound to specific hardware
- **IP Tracking**: Monitor which IPs use licenses
- **Validation Logging**: All validation attempts are logged
- **Secure Communication**: Use HTTPS in production
- **Key Generation**: Uses secure random key generation

## Troubleshooting

### License Validation Fails
- Check server is running and accessible
- Verify product ID matches
- Check license hasn't expired
- Review validation logs in database

### Injection Fails
- Ensure input is a valid Bukkit/Spigot plugin
- Check `plugin.yml` exists and is valid
- Use `--verbose` for detailed output

### Server Won't Start
- Check port 8080 is not in use
- Verify Java 11+ is installed
- Check file permissions for database

## License

This project is provided as-is for license management purposes.

## Support

For issues and questions, please check the documentation or create an issue in the repository.
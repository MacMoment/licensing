# Licensing System Usage Examples

## Example 1: Complete Workflow

### Step 1: Start the License Server
```bash
java -jar bin/licensing-server-1.0.0.jar
```

Server will start on http://localhost:8080

### Step 2: Create a Product via API
```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"My Minecraft Plugin","description":"Premium features plugin"}'
```

Response:
```json
{"id":"550e8400-e29b-41d4-a716-446655440000","message":"Product created successfully"}
```

### Step 3: Generate a License Key
```bash
curl -X POST http://localhost:8080/api/licenses \
  -H "Content-Type: application/json" \
  -d '{"productId":"550e8400-e29b-41d4-a716-446655440000"}'
```

Response:
```json
{"key":"A1B2C3D4E5F6G7H8","message":"License created successfully"}
```

### Step 4: Inject Licensing into Plugin
```bash
java -jar bin/licensing-injector-1.0.0.jar \
  original-plugin.jar \
  licensed-plugin.jar \
  --server http://localhost:8080 \
  --product 550e8400-e29b-41d4-a716-446655440000 \
  --mode DISABLE_ONLY \
  --verbose
```

Output:
```
=== Minecraft Plugin Licensing Injector ===

Analyzing plugin: original-plugin.jar
Plugin: OriginalPlugin v1.0.0
Main: com.example.OriginalPlugin
Commands: 3
Listeners: 2
Total Classes: 15

Injecting licensing...
✓ Successfully injected licensing into: licensed-plugin.jar

Configuration:
  Server URL: http://localhost:8080
  Product ID: 550e8400-e29b-41d4-a716-446655440000
  Failure Mode: DISABLE_ONLY
```

### Step 5: Deploy and Test
Copy `licensed-plugin.jar` to your Minecraft server's `plugins/` folder.

When the plugin starts, it will:
1. Contact the license server
2. Validate the license key
3. Bind to the server's HWID and IP
4. Enable or disable based on validation result

## Example 2: Using the Web Dashboard

1. Open http://localhost:8080 in your browser
2. Click "Products" tab → "+ Add Product"
3. Fill in product details and click "Create"
4. Switch to "Licenses" tab → "+ Generate License"
5. Select your product and optionally set expiry date
6. Copy the generated license key

## Example 3: Different Failure Modes

### Kick Players and Disable
```bash
java -jar bin/licensing-injector-1.0.0.jar \
  plugin.jar output.jar \
  -s http://localhost:8080 \
  -p PRODUCT_ID \
  --mode KICK_AND_DISABLE
```

### Allow on Network Error (Graceful Fallback)
```bash
java -jar bin/licensing-injector-1.0.0.jar \
  plugin.jar output.jar \
  -s http://localhost:8080 \
  -p PRODUCT_ID \
  --mode ALLOW_ON_ERROR
```

### Limited Mode (Restricted Features)
```bash
java -jar bin/licensing-injector-1.0.0.jar \
  plugin.jar output.jar \
  -s http://localhost:8080 \
  -p PRODUCT_ID \
  --mode LIMITED_MODE
```

## Example 4: Creating Tiers

```bash
# Create a "Basic" tier
curl -X POST http://localhost:8080/api/products/PRODUCT_ID/tiers \
  -H "Content-Type: application/json" \
  -d '{"name":"Basic","features":"feature1,feature2","maxUsers":10}'

# Create a "Premium" tier
curl -X POST http://localhost:8080/api/products/PRODUCT_ID/tiers \
  -H "Content-Type: application/json" \
  -d '{"name":"Premium","features":"feature1,feature2,feature3,feature4","maxUsers":0}'
```

## Example 5: License with Expiry

```bash
# License that expires in 30 days
EXPIRY=$(($(date +%s) * 1000 + 30 * 24 * 60 * 60 * 1000))

curl -X POST http://localhost:8080/api/licenses \
  -H "Content-Type: application/json" \
  -d "{\"productId\":\"PRODUCT_ID\",\"expiryTime\":$EXPIRY}"
```

## Example 6: Checking License Status

```bash
# Get specific license details
curl http://localhost:8080/api/licenses/A1B2C3D4E5F6G7H8

# List all licenses
curl http://localhost:8080/api/licenses

# Get all products
curl http://localhost:8080/api/products
```

## Testing with Demo Plugin

The repository includes a demo plugin for testing:

```bash
# Inject licensing into demo plugin
java -jar bin/licensing-injector-1.0.0.jar \
  bin/DemoPlugin.jar \
  bin/DemoPlugin-licensed.jar \
  --server http://localhost:8080 \
  --product demo-product-123 \
  --mode DISABLE_ONLY \
  --verbose

# The output shows the analysis results:
# Plugin: DemoPlugin v1.0.0
# Main: com.example.demo.DemoPlugin
# Commands: 1
# Listeners: 0
# Total Classes: 1
```

## Production Deployment

### Security Recommendations

1. **Use HTTPS**: Configure the server with SSL/TLS certificates
2. **Authentication**: Add API authentication (JWT, API keys)
3. **Rate Limiting**: Implement rate limiting on validation endpoints
4. **Database Security**: Use PostgreSQL/MySQL instead of SQLite for production
5. **Backup**: Regular backups of license database
6. **Monitoring**: Set up monitoring for validation failures

### Server Configuration

Create a `application.properties`:
```properties
server.port=8443
server.ssl.enabled=true
server.ssl.key-store=keystore.jks
server.ssl.key-store-password=changeit
database.url=jdbc:postgresql://localhost/licenses
```

### Reverse Proxy (nginx)

```nginx
server {
    listen 443 ssl;
    server_name license.yourdomain.com;
    
    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;
    
    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

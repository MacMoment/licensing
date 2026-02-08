package com.macmoment.licensing.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import spark.Spark;

import java.util.*;

import static spark.Spark.*;

/**
 * REST API and web server for license management.
 */
public class LicenseServer {
    
    private static final Gson GSON = new Gson();
    private static DatabaseManager db;
    
    public static void main(String[] args) {
        try {
            // Initialize database
            db = new DatabaseManager("licenses.db");
            System.out.println("✓ Database initialized");
            
            // Configure server
            port(8080);
            
            // Try to serve static files from classpath
            try {
                staticFiles.location("/web");
            } catch (Exception e) {
                System.out.println("Note: Static files not available - web UI may not load");
            }
            
            // Enable CORS
            options("/*", (request, response) -> {
                String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
                if (accessControlRequestHeaders != null) {
                    response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
                }
                
                String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
                if (accessControlRequestMethod != null) {
                    response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
                }
                
                return "OK";
            });
            
            before((request, response) -> {
                response.header("Access-Control-Allow-Origin", "*");
                response.header("Access-Control-Allow-Methods", "GET,PUT,POST,DELETE,OPTIONS");
                response.header("Access-Control-Allow-Headers", "Content-Type,Authorization");
                response.type("application/json");
            });
            
            // Setup routes
            setupRoutes();
            
            System.out.println("\n=== License Server Started ===");
            System.out.println("API: http://localhost:8080");
            System.out.println("UI: http://localhost:8080/index.html");
            System.out.println("==============================\n");
            
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static void setupRoutes() {
        // License validation endpoint (for plugins)
        post("/api/validate", (req, res) -> {
            JsonObject request = GSON.fromJson(req.body(), JsonObject.class);
            
            String productId = request.get("productId").getAsString();
            String licenseKey = request.get("licenseKey").getAsString();
            String hwid = request.get("hwid").getAsString();
            String ip = request.get("ip").getAsString();
            
            Map<String, Object> license = db.getLicense(licenseKey);
            
            JsonObject response = new JsonObject();
            
            if (license == null) {
                response.addProperty("valid", false);
                response.addProperty("message", "Invalid license key");
                db.logValidation(licenseKey, hwid, ip, false);
            } else if (!license.get("product_id").equals(productId)) {
                response.addProperty("valid", false);
                response.addProperty("message", "License not valid for this product");
                db.logValidation(licenseKey, hwid, ip, false);
            } else if (!(Boolean) license.get("active")) {
                response.addProperty("valid", false);
                response.addProperty("message", "License has been deactivated");
                db.logValidation(licenseKey, hwid, ip, false);
            } else {
                // Check expiry
                Long expiryTime = (Long) license.get("expiry_time");
                if (expiryTime != null && expiryTime > 0 && System.currentTimeMillis() > expiryTime) {
                    response.addProperty("valid", false);
                    response.addProperty("message", "License has expired");
                    db.logValidation(licenseKey, hwid, ip, false);
                } else {
                    // Check HWID binding
                    String boundHwid = (String) license.get("hwid");
                    if (boundHwid != null && !boundHwid.isEmpty() && !boundHwid.equals(hwid)) {
                        response.addProperty("valid", false);
                        response.addProperty("message", "License bound to different hardware");
                        db.logValidation(licenseKey, hwid, ip, false);
                    } else {
                        // Valid license
                        response.addProperty("valid", true);
                        response.addProperty("message", "License validated successfully");
                        response.addProperty("tier", (String) license.get("tier_name"));
                        response.addProperty("expiryTime", expiryTime != null ? expiryTime : 0);
                        
                        // Add allowed features
                        String features = (String) license.get("features");
                        if (features != null && !features.isEmpty()) {
                            response.add("allowedFeatures", GSON.toJsonTree(Arrays.asList(features.split(","))));
                        }
                        
                        // Bind HWID and IP if not already bound
                        if (boundHwid == null || boundHwid.isEmpty()) {
                            db.updateLicenseBinding(licenseKey, hwid, ip);
                        }
                        
                        db.logValidation(licenseKey, hwid, ip, true);
                    }
                }
            }
            
            return GSON.toJson(response);
        });
        
        // Product endpoints
        get("/api/products", (req, res) -> GSON.toJson(db.getAllProducts()));
        
        post("/api/products", (req, res) -> {
            JsonObject request = GSON.fromJson(req.body(), JsonObject.class);
            String id = UUID.randomUUID().toString();
            String name = request.get("name").getAsString();
            String description = request.has("description") ? request.get("description").getAsString() : "";
            
            db.createProduct(id, name, description);
            
            Map<String, String> response = new HashMap<>();
            response.put("id", id);
            response.put("message", "Product created successfully");
            return GSON.toJson(response);
        });
        
        // Tier endpoints
        get("/api/products/:productId/tiers", (req, res) -> {
            String productId = req.params(":productId");
            return GSON.toJson(db.getTiersByProduct(productId));
        });
        
        post("/api/products/:productId/tiers", (req, res) -> {
            String productId = req.params(":productId");
            JsonObject request = GSON.fromJson(req.body(), JsonObject.class);
            
            String id = UUID.randomUUID().toString();
            String name = request.get("name").getAsString();
            String features = request.has("features") ? request.get("features").getAsString() : "";
            int maxUsers = request.has("maxUsers") ? request.get("maxUsers").getAsInt() : 0;
            
            db.createTier(id, productId, name, features, maxUsers);
            
            Map<String, String> response = new HashMap<>();
            response.put("id", id);
            response.put("message", "Tier created successfully");
            return GSON.toJson(response);
        });
        
        // License endpoints
        get("/api/licenses", (req, res) -> GSON.toJson(db.getAllLicenses()));
        
        get("/api/licenses/:key", (req, res) -> {
            String key = req.params(":key");
            Map<String, Object> license = db.getLicense(key);
            if (license == null) {
                res.status(404);
                return GSON.toJson(Map.of("error", "License not found"));
            }
            return GSON.toJson(license);
        });
        
        post("/api/licenses", (req, res) -> {
            JsonObject request = GSON.fromJson(req.body(), JsonObject.class);
            
            String key = UUID.randomUUID().toString().replace("-", "").toUpperCase().substring(0, 16);
            String productId = request.get("productId").getAsString();
            String tierId = request.has("tierId") && !request.get("tierId").isJsonNull() 
                    ? request.get("tierId").getAsString() : null;
            Long expiryTime = request.has("expiryTime") && !request.get("expiryTime").isJsonNull() 
                    ? request.get("expiryTime").getAsLong() : null;
            
            db.createLicense(key, productId, tierId, expiryTime);
            
            Map<String, String> response = new HashMap<>();
            response.put("key", key);
            response.put("message", "License created successfully");
            return GSON.toJson(response);
        });
        
        // Health check
        get("/api/health", (req, res) -> {
            Map<String, Object> health = new HashMap<>();
            health.put("status", "ok");
            health.put("timestamp", System.currentTimeMillis());
            return GSON.toJson(health);
        });
        
        // Serve UI at root
        get("/", (req, res) -> {
            res.redirect("/index.html");
            return null;
        });
        
        // Exception handling
        exception(Exception.class, (e, req, res) -> {
            res.status(500);
            res.body(GSON.toJson(Map.of("error", e.getMessage())));
            e.printStackTrace();
        });
    }
}

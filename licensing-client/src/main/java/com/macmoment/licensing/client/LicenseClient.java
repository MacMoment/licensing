package com.macmoment.licensing.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Enumeration;
import java.util.logging.Logger;

/**
 * Lightweight licensing client that validates licenses against a licensing server.
 * This client is injected into Minecraft plugins to enforce licensing.
 */
public class LicenseClient {
    
    private static final Logger LOGGER = Logger.getLogger("LicenseClient");
    private static final Gson GSON = new Gson();
    
    private final String serverUrl;
    private final String productId;
    private final String licenseKey;
    private final LicenseConfig config;
    
    private LicenseStatus status;
    private long lastCheckTime = 0;
    
    public LicenseClient(String serverUrl, String productId, String licenseKey, LicenseConfig config) {
        this.serverUrl = serverUrl;
        this.productId = productId;
        this.licenseKey = licenseKey;
        this.config = config;
    }
    
    /**
     * Validates the license with the server.
     * @return true if the license is valid, false otherwise
     */
    public boolean validate() {
        try {
            // Check cache if enabled
            if (config.isCacheEnabled() && isCacheValid()) {
                return status != null && status.isValid();
            }
            
            // Prepare validation request
            JsonObject request = new JsonObject();
            request.addProperty("productId", productId);
            request.addProperty("licenseKey", licenseKey);
            request.addProperty("hwid", getHardwareId());
            request.addProperty("ip", getIpAddress());
            
            // Send validation request
            String response = sendRequest("/api/validate", request.toString());
            status = GSON.fromJson(response, LicenseStatus.class);
            lastCheckTime = System.currentTimeMillis();
            
            return status != null && status.isValid();
            
        } catch (Exception e) {
            LOGGER.warning("License validation failed: " + e.getMessage());
            
            // Graceful failure handling
            if (config.getFailureMode() == FailureMode.ALLOW_ON_ERROR) {
                return true; // Allow if server is unreachable
            }
            return false;
        }
    }
    
    /**
     * Checks if a specific feature is allowed based on the license tier.
     */
    public boolean isFeatureAllowed(String feature) {
        if (status == null || !status.isValid()) {
            return false;
        }
        return status.getTier() != null && status.getAllowedFeatures().contains(feature);
    }
    
    /**
     * Gets the current license status.
     */
    public LicenseStatus getStatus() {
        return status;
    }
    
    /**
     * Generates a unique hardware ID for this machine.
     */
    private String getHardwareId() {
        try {
            StringBuilder sb = new StringBuilder();
            
            // Get MAC addresses
            Enumeration<NetworkInterface> networks = NetworkInterface.getNetworkInterfaces();
            while (networks.hasMoreElements()) {
                NetworkInterface network = networks.nextElement();
                byte[] mac = network.getHardwareAddress();
                if (mac != null) {
                    for (byte b : mac) {
                        sb.append(String.format("%02X", b));
                    }
                }
            }
            
            // Get OS and Java info
            sb.append(System.getProperty("os.name"));
            sb.append(System.getProperty("os.version"));
            sb.append(System.getProperty("user.name"));
            
            // Hash the combined string
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            
            return hexString.toString();
            
        } catch (Exception e) {
            LOGGER.warning("Failed to generate HWID: " + e.getMessage());
            return "UNKNOWN";
        }
    }
    
    /**
     * Gets the external IP address.
     */
    private String getIpAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
    
    /**
     * Sends an HTTP POST request to the licensing server.
     */
    private String sendRequest(String endpoint, String jsonBody) throws IOException {
        URL url = new URL(serverUrl + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("User-Agent", "LicenseClient/1.0");
        conn.setDoOutput(true);
        conn.setConnectTimeout(config.getConnectionTimeout());
        conn.setReadTimeout(config.getReadTimeout());
        
        // Write request body
        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }
        
        // Read response
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new IOException("Server returned error: " + responseCode);
        }
        
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }
    
    /**
     * Checks if the cached license status is still valid.
     */
    private boolean isCacheValid() {
        if (status == null) {
            return false;
        }
        long cacheAge = System.currentTimeMillis() - lastCheckTime;
        return cacheAge < config.getCacheDuration();
    }
}

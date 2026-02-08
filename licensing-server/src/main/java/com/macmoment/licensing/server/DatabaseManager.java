package com.macmoment.licensing.server;

import java.sql.*;
import java.util.*;

/**
 * Database manager for license data.
 */
public class DatabaseManager {
    
    private Connection connection;
    
    public DatabaseManager(String dbPath) throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        initializeTables();
    }
    
    /**
     * Creates database tables if they don't exist.
     */
    private void initializeTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Products table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS products (" +
                "id TEXT PRIMARY KEY," +
                "name TEXT NOT NULL," +
                "description TEXT," +
                "created_at INTEGER NOT NULL" +
                ")"
            );
            
            // Tiers table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS tiers (" +
                "id TEXT PRIMARY KEY," +
                "product_id TEXT NOT NULL," +
                "name TEXT NOT NULL," +
                "features TEXT," +
                "max_users INTEGER," +
                "FOREIGN KEY (product_id) REFERENCES products(id)" +
                ")"
            );
            
            // Licenses table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS licenses (" +
                "key TEXT PRIMARY KEY," +
                "product_id TEXT NOT NULL," +
                "tier_id TEXT," +
                "hwid TEXT," +
                "ip TEXT," +
                "expiry_time INTEGER," +
                "created_at INTEGER NOT NULL," +
                "last_validated INTEGER," +
                "active BOOLEAN DEFAULT 1," +
                "FOREIGN KEY (product_id) REFERENCES products(id)," +
                "FOREIGN KEY (tier_id) REFERENCES tiers(id)" +
                ")"
            );
            
            // Validation logs table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS validation_logs (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "license_key TEXT NOT NULL," +
                "hwid TEXT," +
                "ip TEXT," +
                "timestamp INTEGER NOT NULL," +
                "success BOOLEAN NOT NULL," +
                "FOREIGN KEY (license_key) REFERENCES licenses(key)" +
                ")"
            );
        }
    }
    
    // Product operations
    
    public void createProduct(String id, String name, String description) throws SQLException {
        String sql = "INSERT INTO products (id, name, description, created_at) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, id);
            stmt.setString(2, name);
            stmt.setString(3, description);
            stmt.setLong(4, System.currentTimeMillis());
            stmt.executeUpdate();
        }
    }
    
    public List<Map<String, Object>> getAllProducts() throws SQLException {
        String sql = "SELECT * FROM products ORDER BY created_at DESC";
        List<Map<String, Object>> products = new ArrayList<>();
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> product = new HashMap<>();
                product.put("id", rs.getString("id"));
                product.put("name", rs.getString("name"));
                product.put("description", rs.getString("description"));
                product.put("created_at", rs.getLong("created_at"));
                products.add(product);
            }
        }
        
        return products;
    }
    
    // Tier operations
    
    public void createTier(String id, String productId, String name, String features, int maxUsers) 
            throws SQLException {
        String sql = "INSERT INTO tiers (id, product_id, name, features, max_users) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, id);
            stmt.setString(2, productId);
            stmt.setString(3, name);
            stmt.setString(4, features);
            stmt.setInt(5, maxUsers);
            stmt.executeUpdate();
        }
    }
    
    public List<Map<String, Object>> getTiersByProduct(String productId) throws SQLException {
        String sql = "SELECT * FROM tiers WHERE product_id = ?";
        List<Map<String, Object>> tiers = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, productId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> tier = new HashMap<>();
                    tier.put("id", rs.getString("id"));
                    tier.put("product_id", rs.getString("product_id"));
                    tier.put("name", rs.getString("name"));
                    tier.put("features", rs.getString("features"));
                    tier.put("max_users", rs.getInt("max_users"));
                    tiers.add(tier);
                }
            }
        }
        
        return tiers;
    }
    
    // License operations
    
    public void createLicense(String key, String productId, String tierId, Long expiryTime) 
            throws SQLException {
        String sql = "INSERT INTO licenses (key, product_id, tier_id, expiry_time, created_at, active) " +
                     "VALUES (?, ?, ?, ?, ?, 1)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, key);
            stmt.setString(2, productId);
            stmt.setString(3, tierId);
            if (expiryTime != null) {
                stmt.setLong(4, expiryTime);
            } else {
                stmt.setNull(4, Types.INTEGER);
            }
            stmt.setLong(5, System.currentTimeMillis());
            stmt.executeUpdate();
        }
    }
    
    public Map<String, Object> getLicense(String key) throws SQLException {
        String sql = "SELECT l.*, t.name as tier_name, t.features, t.max_users " +
                     "FROM licenses l " +
                     "LEFT JOIN tiers t ON l.tier_id = t.id " +
                     "WHERE l.key = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, key);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> license = new HashMap<>();
                    license.put("key", rs.getString("key"));
                    license.put("product_id", rs.getString("product_id"));
                    license.put("tier_id", rs.getString("tier_id"));
                    license.put("tier_name", rs.getString("tier_name"));
                    license.put("features", rs.getString("features"));
                    license.put("max_users", rs.getInt("max_users"));
                    license.put("hwid", rs.getString("hwid"));
                    license.put("ip", rs.getString("ip"));
                    license.put("expiry_time", rs.getLong("expiry_time"));
                    license.put("active", rs.getBoolean("active"));
                    license.put("created_at", rs.getLong("created_at"));
                    return license;
                }
            }
        }
        
        return null;
    }
    
    public void updateLicenseBinding(String key, String hwid, String ip) throws SQLException {
        String sql = "UPDATE licenses SET hwid = ?, ip = ?, last_validated = ? WHERE key = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, hwid);
            stmt.setString(2, ip);
            stmt.setLong(3, System.currentTimeMillis());
            stmt.setString(4, key);
            stmt.executeUpdate();
        }
    }
    
    public List<Map<String, Object>> getAllLicenses() throws SQLException {
        String sql = "SELECT l.*, p.name as product_name, t.name as tier_name " +
                     "FROM licenses l " +
                     "JOIN products p ON l.product_id = p.id " +
                     "LEFT JOIN tiers t ON l.tier_id = t.id " +
                     "ORDER BY l.created_at DESC";
        List<Map<String, Object>> licenses = new ArrayList<>();
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> license = new HashMap<>();
                license.put("key", rs.getString("key"));
                license.put("product_id", rs.getString("product_id"));
                license.put("product_name", rs.getString("product_name"));
                license.put("tier_id", rs.getString("tier_id"));
                license.put("tier_name", rs.getString("tier_name"));
                license.put("hwid", rs.getString("hwid"));
                license.put("ip", rs.getString("ip"));
                license.put("expiry_time", rs.getLong("expiry_time"));
                license.put("active", rs.getBoolean("active"));
                license.put("created_at", rs.getLong("created_at"));
                licenses.add(license);
            }
        }
        
        return licenses;
    }
    
    public void logValidation(String key, String hwid, String ip, boolean success) 
            throws SQLException {
        String sql = "INSERT INTO validation_logs (license_key, hwid, ip, timestamp, success) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, key);
            stmt.setString(2, hwid);
            stmt.setString(3, ip);
            stmt.setLong(4, System.currentTimeMillis());
            stmt.setBoolean(5, success);
            stmt.executeUpdate();
        }
    }
    
    // Toggle license active status
    
    public void toggleLicense(String key, boolean active) throws SQLException {
        String sql = "UPDATE licenses SET active = ? WHERE key = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setBoolean(1, active);
            stmt.setString(2, key);
            stmt.executeUpdate();
        }
    }
    
    // Reset HWID binding
    
    public void resetLicenseHwid(String key) throws SQLException {
        String sql = "UPDATE licenses SET hwid = NULL, ip = NULL WHERE key = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, key);
            stmt.executeUpdate();
        }
    }
    
    // Delete operations
    
    public void deleteProduct(String id) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM products WHERE id = ?")) {
            stmt.setString(1, id);
            stmt.executeUpdate();
        }
    }
    
    public void deleteLicense(String key) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM licenses WHERE key = ?")) {
            stmt.setString(1, key);
            stmt.executeUpdate();
        }
    }
    
    public void deleteTier(String id) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM tiers WHERE id = ?")) {
            stmt.setString(1, id);
            stmt.executeUpdate();
        }
    }
    
    // Dashboard stats
    
    public Map<String, Object> getStats() throws SQLException {
        Map<String, Object> stats = new HashMap<>();
        
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM products");
            stats.put("totalProducts", rs.next() ? rs.getInt("count") : 0);
        }
        
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM licenses");
            stats.put("totalLicenses", rs.next() ? rs.getInt("count") : 0);
        }
        
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM licenses WHERE active = 1");
            stats.put("activeLicenses", rs.next() ? rs.getInt("count") : 0);
        }
        
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                "SELECT COUNT(*) as count FROM licenses WHERE expiry_time > 0 AND expiry_time < " + 
                System.currentTimeMillis());
            stats.put("expiredLicenses", rs.next() ? rs.getInt("count") : 0);
        }
        
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM tiers");
            stats.put("totalTiers", rs.next() ? rs.getInt("count") : 0);
        }
        
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                "SELECT COUNT(*) as count FROM validation_logs WHERE timestamp > " + 
                (System.currentTimeMillis() - 86400000));
            stats.put("validationsToday", rs.next() ? rs.getInt("count") : 0);
        }
        
        return stats;
    }
    
    // Validation logs
    
    public List<Map<String, Object>> getValidationLogs(int limit) throws SQLException {
        String sql = "SELECT vl.*, l.product_id, p.name as product_name " +
                     "FROM validation_logs vl " +
                     "LEFT JOIN licenses l ON vl.license_key = l.key " +
                     "LEFT JOIN products p ON l.product_id = p.id " +
                     "ORDER BY vl.timestamp DESC LIMIT ?";
        List<Map<String, Object>> logs = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> log = new HashMap<>();
                    log.put("id", rs.getInt("id"));
                    log.put("license_key", rs.getString("license_key"));
                    log.put("hwid", rs.getString("hwid"));
                    log.put("ip", rs.getString("ip"));
                    log.put("timestamp", rs.getLong("timestamp"));
                    log.put("success", rs.getBoolean("success"));
                    log.put("product_name", rs.getString("product_name"));
                    logs.add(log);
                }
            }
        }
        
        return logs;
    }
    
    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}

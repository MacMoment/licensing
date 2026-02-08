package com.macmoment.licensing.client;

import java.util.Collections;
import java.util.List;

/**
 * Represents the status of a license validation.
 */
public class LicenseStatus {
    
    private boolean valid;
    private String message;
    private String tier;
    private long expiryTime;
    private List<String> allowedFeatures;
    private int maxUsers;
    
    public LicenseStatus() {
        this.allowedFeatures = Collections.emptyList();
    }
    
    public boolean isValid() {
        return valid && (expiryTime == 0 || System.currentTimeMillis() < expiryTime);
    }
    
    public void setValid(boolean valid) {
        this.valid = valid;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getTier() {
        return tier;
    }
    
    public void setTier(String tier) {
        this.tier = tier;
    }
    
    public long getExpiryTime() {
        return expiryTime;
    }
    
    public void setExpiryTime(long expiryTime) {
        this.expiryTime = expiryTime;
    }
    
    public List<String> getAllowedFeatures() {
        return allowedFeatures != null ? allowedFeatures : Collections.emptyList();
    }
    
    public void setAllowedFeatures(List<String> allowedFeatures) {
        this.allowedFeatures = allowedFeatures;
    }
    
    public int getMaxUsers() {
        return maxUsers;
    }
    
    public void setMaxUsers(int maxUsers) {
        this.maxUsers = maxUsers;
    }
}

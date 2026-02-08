package com.macmoment.licensing.client;

/**
 * Configuration for the license client.
 */
public class LicenseConfig {
    
    private boolean cacheEnabled = true;
    private long cacheDuration = 3600000; // 1 hour in milliseconds
    private int connectionTimeout = 5000; // 5 seconds
    private int readTimeout = 10000; // 10 seconds
    private FailureMode failureMode = FailureMode.DENY_ON_ERROR;
    
    public boolean isCacheEnabled() {
        return cacheEnabled;
    }
    
    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }
    
    public long getCacheDuration() {
        return cacheDuration;
    }
    
    public void setCacheDuration(long cacheDuration) {
        this.cacheDuration = cacheDuration;
    }
    
    public int getConnectionTimeout() {
        return connectionTimeout;
    }
    
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }
    
    public int getReadTimeout() {
        return readTimeout;
    }
    
    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }
    
    public FailureMode getFailureMode() {
        return failureMode;
    }
    
    public void setFailureMode(FailureMode failureMode) {
        this.failureMode = failureMode;
    }
    
    public static LicenseConfig getDefault() {
        return new LicenseConfig();
    }
}

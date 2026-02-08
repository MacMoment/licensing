package com.macmoment.licensing.injector;

import com.macmoment.licensing.client.FailureMode;

/**
 * Configuration for the injector.
 */
public class InjectorConfig {
    
    private String serverUrl;
    private String productId;
    private String licenseKey;
    private FailureMode failureMode = FailureMode.DISABLE_ONLY;
    
    public String getServerUrl() {
        return serverUrl;
    }
    
    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }
    
    public String getProductId() {
        return productId;
    }
    
    public void setProductId(String productId) {
        this.productId = productId;
    }
    
    public String getLicenseKey() {
        return licenseKey;
    }
    
    public void setLicenseKey(String licenseKey) {
        this.licenseKey = licenseKey;
    }
    
    public FailureMode getFailureMode() {
        return failureMode;
    }
    
    public void setFailureMode(FailureMode failureMode) {
        this.failureMode = failureMode;
    }
}

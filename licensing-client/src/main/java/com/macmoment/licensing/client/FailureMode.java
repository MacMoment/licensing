package com.macmoment.licensing.client;

/**
 * Defines how the plugin should behave when license validation fails.
 */
public enum FailureMode {
    /**
     * Kick players and disable plugin if validation fails.
     */
    KICK_AND_DISABLE,
    
    /**
     * Only disable plugin if validation fails.
     */
    DISABLE_ONLY,
    
    /**
     * Run in limited mode if validation fails.
     */
    LIMITED_MODE,
    
    /**
     * Allow plugin to run if server is unreachable (but deny if explicitly invalid).
     */
    ALLOW_ON_ERROR,
    
    /**
     * Deny all operations if validation fails.
     */
    DENY_ON_ERROR
}

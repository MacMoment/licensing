package com.macmoment.licensing.injector;

import java.util.ArrayList;
import java.util.List;

/**
 * Metadata extracted from a plugin JAR.
 */
public class PluginMetadata {
    
    private String name;
    private String version;
    private String mainClass;
    private List<String> commands = new ArrayList<>();
    private List<String> listeners = new ArrayList<>();
    private List<String> allClasses = new ArrayList<>();
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public String getMainClass() {
        return mainClass;
    }
    
    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }
    
    public List<String> getCommands() {
        return commands;
    }
    
    public void setCommands(List<String> commands) {
        this.commands = commands;
    }
    
    public List<String> getListeners() {
        return listeners;
    }
    
    public void setListeners(List<String> listeners) {
        this.listeners = listeners;
    }
    
    public List<String> getAllClasses() {
        return allClasses;
    }
    
    public void setAllClasses(List<String> allClasses) {
        this.allClasses = allClasses;
    }
    
    @Override
    public String toString() {
        return String.format("Plugin: %s v%s\nMain: %s\nCommands: %d\nListeners: %d\nTotal Classes: %d",
                name, version, mainClass, commands.size(), listeners.size(), allClasses.size());
    }
}

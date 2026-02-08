package com.macmoment.licensing.injector;

import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * Analyzes a Minecraft plugin JAR file to extract information needed for license injection.
 */
public class PluginAnalyzer {
    
    private final File jarFile;
    
    public PluginAnalyzer(File jarFile) {
        this.jarFile = jarFile;
    }
    
    /**
     * Analyzes the plugin and returns metadata.
     */
    public PluginMetadata analyze() throws IOException {
        PluginMetadata metadata = new PluginMetadata();
        
        try (JarFile jar = new JarFile(jarFile)) {
            // Parse plugin.yml
            JarEntry pluginYml = jar.getJarEntry("plugin.yml");
            if (pluginYml == null) {
                throw new IOException("plugin.yml not found - not a valid Bukkit/Spigot plugin");
            }
            
            parsePluginYml(jar.getInputStream(pluginYml), metadata);
            
            // Scan for command and listener classes
            scanClasses(jar, metadata);
        }
        
        return metadata;
    }
    
    /**
     * Parses plugin.yml to extract plugin information.
     */
    private void parsePluginYml(InputStream stream, PluginMetadata metadata) {
        Yaml yaml = new Yaml();
        Map<String, Object> config = yaml.load(stream);
        
        metadata.setName((String) config.get("name"));
        metadata.setVersion((String) config.get("version"));
        metadata.setMainClass((String) config.get("main"));
        
        // Extract commands
        @SuppressWarnings("unchecked")
        Map<String, Object> commands = (Map<String, Object>) config.get("commands");
        if (commands != null) {
            metadata.setCommands(new ArrayList<>(commands.keySet()));
        }
    }
    
    /**
     * Scans JAR for classes that might be commands or listeners.
     */
    private void scanClasses(JarFile jar, PluginMetadata metadata) throws IOException {
        Enumeration<JarEntry> entries = jar.entries();
        List<String> listeners = new ArrayList<>();
        List<String> allClasses = new ArrayList<>();
        
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();
            
            if (name.endsWith(".class")) {
                String className = name.replace('/', '.').replace(".class", "");
                allClasses.add(className);
                
                // Heuristic: classes with "Listener" or "Command" in name
                if (name.contains("Listener") || name.contains("Command")) {
                    listeners.add(className);
                }
            }
        }
        
        metadata.setListeners(listeners);
        metadata.setAllClasses(allClasses);
    }
}

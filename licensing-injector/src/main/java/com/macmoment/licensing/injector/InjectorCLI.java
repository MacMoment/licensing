package com.macmoment.licensing.injector;

import com.macmoment.licensing.client.FailureMode;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.util.concurrent.Callable;

/**
 * Command-line interface for the licensing injector.
 */
@Command(name = "licensing-injector", 
         mixinStandardHelpOptions = true,
         version = "1.0.0",
         description = "Injects licensing into Minecraft plugin JARs")
public class InjectorCLI implements Callable<Integer> {
    
    @Parameters(index = "0", description = "Input plugin JAR file")
    private File inputJar;
    
    @Parameters(index = "1", description = "Output plugin JAR file")
    private File outputJar;
    
    @Option(names = {"-s", "--server"}, 
            description = "License server URL", 
            required = true)
    private String serverUrl;
    
    @Option(names = {"-p", "--product"}, 
            description = "Product ID", 
            required = true)
    private String productId;
    
    @Option(names = {"-k", "--key"}, 
            description = "License key (leave empty for user input)")
    private String licenseKey = "";
    
    @Option(names = {"-m", "--mode"}, 
            description = "Failure mode: ${COMPLETION-CANDIDATES}",
            defaultValue = "DISABLE_ONLY")
    private FailureMode failureMode;
    
    @Option(names = {"-v", "--verbose"}, 
            description = "Enable verbose output")
    private boolean verbose;
    
    @Override
    public Integer call() throws Exception {
        System.out.println("=== Minecraft Plugin Licensing Injector ===\n");
        
        // Validate input
        if (!inputJar.exists()) {
            System.err.println("Error: Input JAR does not exist: " + inputJar);
            return 1;
        }
        
        if (outputJar.exists()) {
            System.out.println("Warning: Output JAR already exists and will be overwritten");
        }
        
        // Analyze plugin
        System.out.println("Analyzing plugin: " + inputJar.getName());
        PluginAnalyzer analyzer = new PluginAnalyzer(inputJar);
        PluginMetadata metadata = analyzer.analyze();
        
        if (verbose) {
            System.out.println("\n" + metadata.toString() + "\n");
        } else {
            System.out.println("Plugin: " + metadata.getName() + " v" + metadata.getVersion());
        }
        
        // Configure injector
        InjectorConfig config = new InjectorConfig();
        config.setServerUrl(serverUrl);
        config.setProductId(productId);
        config.setLicenseKey(licenseKey);
        config.setFailureMode(failureMode);
        
        // Patch plugin
        System.out.println("\nInjecting licensing...");
        BytecodePatcher patcher = new BytecodePatcher(config);
        patcher.patchJar(inputJar, outputJar, metadata);
        
        System.out.println("✓ Successfully injected licensing into: " + outputJar.getName());
        System.out.println("\nConfiguration:");
        System.out.println("  Server URL: " + serverUrl);
        System.out.println("  Product ID: " + productId);
        System.out.println("  Failure Mode: " + failureMode);
        
        return 0;
    }
    
    public static void main(String[] args) {
        int exitCode = new CommandLine(new InjectorCLI()).execute(args);
        System.exit(exitCode);
    }
}

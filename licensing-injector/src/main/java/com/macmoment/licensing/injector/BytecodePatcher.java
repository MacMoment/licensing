package com.macmoment.licensing.injector;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.AdviceAdapter;

import java.io.*;
import java.util.Properties;
import java.util.jar.*;
import java.util.zip.*;

/**
 * Patches plugin bytecode to inject license checking.
 */
public class BytecodePatcher {
    
    private final InjectorConfig config;
    
    public BytecodePatcher(InjectorConfig config) {
        this.config = config;
    }
    
    /**
     * Patches the plugin JAR to add license checking.
     */
    public void patchJar(File inputJar, File outputJar, PluginMetadata metadata) throws IOException {
        try (JarFile input = new JarFile(inputJar);
             JarOutputStream output = new JarOutputStream(new FileOutputStream(outputJar))) {
            
            // Copy all entries, patching the main class
            java.util.Enumeration<JarEntry> entries = input.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                
                // Patch main class
                if (name.equals(metadata.getMainClass().replace('.', '/') + ".class")) {
                    output.putNextEntry(new ZipEntry(name));
                    byte[] patched = patchMainClass(input.getInputStream(entry));
                    output.write(patched);
                    output.closeEntry();
                } else if (name.endsWith(".class") && shouldPatchClass(name, metadata)) {
                    // Patch command/listener classes
                    output.putNextEntry(new ZipEntry(name));
                    byte[] patched = patchCommandOrListener(input.getInputStream(entry));
                    output.write(patched);
                    output.closeEntry();
                } else if (!name.equals("META-INF/MANIFEST.MF")) {
                    // Copy other files as-is (except manifest, we'll recreate it)
                    output.putNextEntry(new ZipEntry(name));
                    copyStream(input.getInputStream(entry), output);
                    output.closeEntry();
                }
            }
            
            // Add licensing client classes
            addLicensingClient(output);
            
            // Add license configuration
            addLicenseConfig(output);
        }
    }
    
    /**
     * Determines if a class should be patched based on metadata.
     */
    private boolean shouldPatchClass(String className, PluginMetadata metadata) {
        String normalizedName = className.replace('/', '.').replace(".class", "");
        return metadata.getListeners().contains(normalizedName);
    }
    
    /**
     * Patches the main class to add license validation in onEnable().
     */
    private byte[] patchMainClass(InputStream classStream) throws IOException {
        ClassReader reader = new ClassReader(classStream);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, 
                                            String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                
                // Inject into onEnable() method
                if (name.equals("onEnable") && descriptor.equals("()V")) {
                    return new AdviceAdapter(Opcodes.ASM9, mv, access, name, descriptor) {
                        @Override
                        protected void onMethodEnter() {
                            // Inject license check at the start of onEnable()
                            injectLicenseCheck(this);
                        }
                    };
                }
                
                return mv;
            }
        };
        
        reader.accept(visitor, ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();
    }
    
    /**
     * Patches command/listener classes to add license checks.
     */
    private byte[] patchCommandOrListener(InputStream classStream) throws IOException {
        ClassReader reader = new ClassReader(classStream);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, 
                                            String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                
                // Inject into onCommand and event handlers
                if (name.equals("onCommand") || name.startsWith("on")) {
                    return new AdviceAdapter(Opcodes.ASM9, mv, access, name, descriptor) {
                        @Override
                        protected void onMethodEnter() {
                            // Check license before executing
                            injectLicenseCheck(this);
                        }
                    };
                }
                
                return mv;
            }
        };
        
        reader.accept(visitor, ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();
    }
    
    /**
     * Injects bytecode to check the license.
     */
    private void injectLicenseCheck(MethodVisitor mv) {
        // Load LicenseManager and call validate()
        // INVOKESTATIC com/macmoment/licensing/client/LicenseManager.validate()Z
        // IFEQ <fail_label>
        // ... continue normally
        // <fail_label>:
        // ... handle license failure based on config
        
        mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                "com/macmoment/licensing/client/LicenseManager",
                "validate",
                "()Z",
                false);
        
        Label validLabel = new Label();
        mv.visitJumpInsn(Opcodes.IFNE, validLabel);
        
        // License is invalid - handle based on failure mode
        if (config.getFailureMode() == com.macmoment.licensing.client.FailureMode.KICK_AND_DISABLE) {
            // Throw exception or return early
            mv.visitInsn(Opcodes.RETURN);
        }
        
        mv.visitLabel(validLabel);
    }
    
    /**
     * Adds the licensing client classes to the JAR.
     */
    private void addLicensingClient(JarOutputStream output) throws IOException {
        // In a real implementation, we would copy the pre-built licensing-client.jar
        // For now, we'll add a placeholder LicenseManager class
        
        output.putNextEntry(new ZipEntry("com/macmoment/licensing/client/LicenseManager.class"));
        byte[] managerClass = createLicenseManagerClass();
        output.write(managerClass);
        output.closeEntry();
    }
    
    /**
     * Creates a simple LicenseManager class.
     */
    private byte[] createLicenseManagerClass() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V11, Opcodes.ACC_PUBLIC, 
                "com/macmoment/licensing/client/LicenseManager", null, 
                "java/lang/Object", null);
        
        // Static client field
        FieldVisitor fv = cw.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                "client", "Lcom/macmoment/licensing/client/LicenseClient;", null, null);
        fv.visitEnd();
        
        // Static validate method
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "validate", "()Z", null, null);
        mv.visitCode();
        mv.visitFieldInsn(Opcodes.GETSTATIC,
                "com/macmoment/licensing/client/LicenseManager",
                "client",
                "Lcom/macmoment/licensing/client/LicenseClient;");
        Label notNull = new Label();
        mv.visitJumpInsn(Opcodes.IFNONNULL, notNull);
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitInsn(Opcodes.IRETURN);
        mv.visitLabel(notNull);
        mv.visitFieldInsn(Opcodes.GETSTATIC,
                "com/macmoment/licensing/client/LicenseManager",
                "client",
                "Lcom/macmoment/licensing/client/LicenseClient;");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                "com/macmoment/licensing/client/LicenseClient",
                "validate",
                "()Z",
                false);
        mv.visitInsn(Opcodes.IRETURN);
        mv.visitMaxs(1, 0);
        mv.visitEnd();
        
        cw.visitEnd();
        return cw.toByteArray();
    }
    
    /**
     * Adds license configuration to the JAR.
     */
    private void addLicenseConfig(JarOutputStream output) throws IOException {
        output.putNextEntry(new ZipEntry("license.properties"));
        
        Properties props = new Properties();
        props.setProperty("server.url", config.getServerUrl());
        props.setProperty("product.id", config.getProductId());
        props.setProperty("license.key", config.getLicenseKey());
        props.setProperty("failure.mode", config.getFailureMode().name());
        
        props.store(output, "License Configuration");
        output.closeEntry();
    }
    
    /**
     * Copies an input stream to an output stream.
     */
    private void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[8192];
        int len;
        while ((len = in.read(buffer)) > 0) {
            out.write(buffer, 0, len);
        }
    }
}

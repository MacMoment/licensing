package com.example.demo;

/**
 * Demo Minecraft plugin for testing license injection.
 * This is a simplified version that compiles without Spigot dependencies.
 * Replace this with actual Bukkit/Spigot API calls when used in production.
 */
public class DemoPlugin {
    
    public void onEnable() {
        System.out.println("DemoPlugin has been enabled!");
    }
    
    public void onDisable() {
        System.out.println("DemoPlugin has been disabled!");
    }
    
    public boolean onCommand(String command, String[] args) {
        if (command.equalsIgnoreCase("democmd")) {
            System.out.println("[DemoPlugin] This is a demo command!");
            return true;
        }
        return false;
    }
    
    public void onPlayerJoin(String playerName) {
        System.out.println("[DemoPlugin] Welcome " + playerName + "!");
    }
}

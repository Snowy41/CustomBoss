package com.mcbzh.custombosses;

import com.mcbzh.custombosses.commands.EditorCommand;
import com.mcbzh.custombosses.listeners.BossListener;
import org.bukkit.plugin.java.JavaPlugin;

public class CustomBossesPlugin extends JavaPlugin {

    private static CustomBossesPlugin instance;

    private BossManager bossManager;
    private ConfigManager configManager;
    private com.mcbzh.custombosses.editor.EditorManager editorManager;
    private com.mcbzh.custombosses.animation.AnimationManager animationManager;

    @Override
    public void onEnable() {
        instance = this;

        // Initialize managers
        this.configManager = new ConfigManager(this);
        this.bossManager = new BossManager(this);
        this.editorManager = new com.mcbzh.custombosses.editor.EditorManager(this);
        this.animationManager = new com.mcbzh.custombosses.animation.AnimationManager(this);

        // Register commands
        EditorCommand editorCommand = new EditorCommand(this);
        getCommand("cb").setExecutor(editorCommand);
        getCommand("cb").setTabCompleter(editorCommand);

        // Register listeners
        getServer().getPluginManager().registerEvents(new BossListener(this), this);

        getLogger().info("CustomBosses has been enabled!");
        getLogger().info("Loaded " + configManager.getAllModels().size() + " models");
    }

    @Override
    public void onDisable() {
        // Clean shutdown - despawn all bosses
        if (bossManager != null) {
            bossManager.removeAllBosses();
            getLogger().info("Despawned all active bosses");
        }

        // Clean up editor sessions
        if (editorManager != null) {
            editorManager.cleanupAll();
        }

        getLogger().info("CustomBosses has been disabled!");
    }

    public static CustomBossesPlugin getInstance() {
        return instance;
    }

    public BossManager getBossManager() {
        return bossManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public com.mcbzh.custombosses.editor.EditorManager getEditorManager() {
        return editorManager;
    }

    public com.mcbzh.custombosses.animation.AnimationManager getAnimationManager() {
        return animationManager;
    }
}
package com.mcbzh.custombosses;

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

        this.configManager = new ConfigManager(this);
        this.bossManager = new BossManager(this);
        this.editorManager = new com.mcbzh.custombosses.editor.EditorManager(this);
        this.animationManager = new com.mcbzh.custombosses.animation.AnimationManager(this);

        getCommand("cb").setExecutor(new com.mcbzh.custombosses.commands.EditorCommand(this));

        getLogger().info("CustomBosses has been enabled!");
    }

    @Override
    public void onDisable() {
        if (bossManager != null) {
            bossManager.removeAllBosses();
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

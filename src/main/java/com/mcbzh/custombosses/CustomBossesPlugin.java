package com.mcbzh.custombosses;

import com.mcbzh.custombosses.commands.BossCommand;
import com.mcbzh.custombosses.editor.EditorManager;
import com.mcbzh.custombosses.listeners.BossListener;
import com.mcbzh.custombosses.listeners.EditorListener;
import com.mcbzh.custombosses.manager.BossManager;
import com.mcbzh.custombosses.storage.ModelStorage;
import org.bukkit.plugin.java.JavaPlugin;

public class CustomBossesPlugin extends JavaPlugin {

    private static CustomBossesPlugin instance;
    private BossManager bossManager;
    private EditorManager editorManager;
    private ModelStorage modelStorage;

    @Override
    public void onEnable() {
        instance = this;

        // Create data folders
        getDataFolder().mkdirs();

        // Initialize storage
        this.modelStorage = new ModelStorage(this);

        // Initialize managers
        this.bossManager = new BossManager(this);
        this.editorManager = new EditorManager(this);

        // Register commands
        BossCommand cmd = new BossCommand(this);
        getCommand("cb").setExecutor(cmd);
        getCommand("cb").setTabCompleter(cmd);

        // Register listeners
        getServer().getPluginManager().registerEvents(new BossListener(this), this);
        getServer().getPluginManager().registerEvents(new EditorListener(this), this);

        getLogger().info("CustomBosses enabled!");
        getLogger().info("Loaded " + modelStorage.getAllModels().size() + " models");
    }

    @Override
    public void onDisable() {
        if (bossManager != null) {
            bossManager.shutdown();
        }
        if (editorManager != null) {
            editorManager.shutdown();
        }
        getLogger().info("CustomBosses disabled!");
    }

    public static CustomBossesPlugin getInstance() {
        return instance;
    }

    public BossManager getBossManager() {
        return bossManager;
    }

    public EditorManager getEditorManager() {
        return editorManager;
    }

    public ModelStorage getModelStorage() {
        return modelStorage;
    }
}
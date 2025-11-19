package com.mcbzh.custombosses;

import com.mcbzh.custombosses.model.ModelData;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

    private final Map<String, ModelData> modelRegistry = new HashMap<>();
    private final com.mcbzh.custombosses.storage.ModelSerializer serializer;

    public ConfigManager(CustomBossesPlugin plugin) {
        this.serializer = new com.mcbzh.custombosses.storage.ModelSerializer(plugin.getDataFolder());
        loadAll();
    }

    public void loadAll() {
        modelRegistry.clear();
        File[] files = serializer.listModelFiles();
        if (files != null) {
            for (File file : files) {
                String id = file.getName().replace(".json", "");
                ModelData model = serializer.loadModel(id);
                if (model != null) {
                    modelRegistry.put(id, model);
                }
            }
        }

        // Create default if none exist
        if (modelRegistry.isEmpty()) {
            createGolemModel();
        }
    }

    private void createGolemModel() {
        ModelData golem = new ModelData("golem_boss");

        // Body (Root)
        com.mcbzh.custombosses.model.ModelPartData body = new com.mcbzh.custombosses.model.ModelPartData(
                "body",
                org.bukkit.Material.IRON_BLOCK,
                new org.bukkit.util.Vector(0, 1.5, 0),
                new org.bukkit.util.Vector(0, 0, 0),
                new org.bukkit.util.Vector(0.9, 1.2, 0.5));
        golem.addPart(body);

        // Head
        com.mcbzh.custombosses.model.ModelPartData head = new com.mcbzh.custombosses.model.ModelPartData(
                "head",
                org.bukkit.Material.CARVED_PUMPKIN,
                new org.bukkit.util.Vector(0, 0.9, 0), // Relative to body
                new org.bukkit.util.Vector(0, 0, 0),
                new org.bukkit.util.Vector(0.6, 0.6, 0.6));
        head.setParentId("body");
        golem.addPart(head);

        // Left Arm
        com.mcbzh.custombosses.model.ModelPartData leftArm = new com.mcbzh.custombosses.model.ModelPartData(
                "left_arm",
                org.bukkit.Material.IRON_BLOCK,
                new org.bukkit.util.Vector(-0.7, 0.2, 0),
                new org.bukkit.util.Vector(0, 0, 0),
                new org.bukkit.util.Vector(0.4, 1.1, 0.4));
        leftArm.setParentId("body");
        golem.addPart(leftArm);

        // Right Arm
        com.mcbzh.custombosses.model.ModelPartData rightArm = new com.mcbzh.custombosses.model.ModelPartData(
                "right_arm",
                org.bukkit.Material.IRON_BLOCK,
                new org.bukkit.util.Vector(0.7, 0.2, 0),
                new org.bukkit.util.Vector(0, 0, 0),
                new org.bukkit.util.Vector(0.4, 1.1, 0.4));
        rightArm.setParentId("body");
        golem.addPart(rightArm);

        // Left Leg
        com.mcbzh.custombosses.model.ModelPartData leftLeg = new com.mcbzh.custombosses.model.ModelPartData(
                "left_leg",
                org.bukkit.Material.IRON_BLOCK,
                new org.bukkit.util.Vector(-0.3, -1.0, 0),
                new org.bukkit.util.Vector(0, 0, 0),
                new org.bukkit.util.Vector(0.4, 1.0, 0.4));
        leftLeg.setParentId("body");
        golem.addPart(leftLeg);

        // Right Leg
        com.mcbzh.custombosses.model.ModelPartData rightLeg = new com.mcbzh.custombosses.model.ModelPartData(
                "right_leg",
                org.bukkit.Material.IRON_BLOCK,
                new org.bukkit.util.Vector(0.3, -1.0, 0),
                new org.bukkit.util.Vector(0, 0, 0),
                new org.bukkit.util.Vector(0.4, 1.0, 0.4));
        rightLeg.setParentId("body");
        golem.addPart(rightLeg);

        saveModel(golem);
    }

    public ModelData getModel(String id) {
        return modelRegistry.get(id);
    }

    public void saveModel(ModelData model) {
        modelRegistry.put(model.getId(), model);
        serializer.saveModel(model);
    }

    public java.util.Collection<ModelData> getAllModels() {
        return modelRegistry.values();
    }

    public void reload() {
        loadAll();
    }
}

package com.mcbzh.custombosses;

import com.mcbzh.custombosses.model.ModelData;
import com.mcbzh.custombosses.model.ModelPartData;
import org.bukkit.Material;
import org.bukkit.util.Vector;

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
                    System.out.println("[CustomBosses] Loaded model: " + id);
                }
            }
        }

        // Create default models if none exist
        if (modelRegistry.isEmpty()) {
            System.out.println("[CustomBosses] No models found, creating defaults...");
            createGolemModel();
            createSimpleModel();
        }
    }

    private void createSimpleModel() {
        ModelData simple = new ModelData("simple_cube");
        simple.setHitboxSize(new Vector(1.5, 2.0, 1.5));

        // Just a single cube for testing
        ModelPartData body = new ModelPartData(
                "body",
                Material.GOLD_BLOCK,
                new Vector(0, 1, 0),
                new Vector(0, 0, 0),
                new Vector(1, 1, 1));
        simple.addPart(body);

        saveModel(simple);
        System.out.println("[CustomBosses] Created simple test model");
    }

    private void createGolemModel() {
        ModelData golem = new ModelData("golem_boss");
        golem.setHitboxSize(new Vector(1.5, 3.0, 1.5));

        // Body (Root) - Centered at Y=1.5
        ModelPartData body = new ModelPartData(
                "body",
                Material.IRON_BLOCK,
                new Vector(0, 1.5, 0),
                new Vector(0, 0, 0),
                new Vector(0.9, 1.2, 0.5));
        golem.addPart(body);

        // Head - On top of body
        ModelPartData head = new ModelPartData(
                "head",
                Material.CARVED_PUMPKIN,
                new Vector(0, 0.8, 0), // Relative to body
                new Vector(0, 0, 0),
                new Vector(0.6, 0.6, 0.6));
        head.setParentId("body");
        golem.addPart(head);

        // Left Arm - Attached to body
        ModelPartData leftArm = new ModelPartData(
                "left_arm",
                Material.IRON_BLOCK,
                new Vector(-0.7, 0.3, 0), // Left side, slightly down
                new Vector(0, 0, 0),
                new Vector(0.3, 0.9, 0.3));
        leftArm.setParentId("body");
        golem.addPart(leftArm);

        // Right Arm - Attached to body
        ModelPartData rightArm = new ModelPartData(
                "right_arm",
                Material.IRON_BLOCK,
                new Vector(0.7, 0.3, 0), // Right side, slightly down
                new Vector(0, 0, 0),
                new Vector(0.3, 0.9, 0.3));
        rightArm.setParentId("body");
        golem.addPart(rightArm);

        // Left Leg - Below body
        ModelPartData leftLeg = new ModelPartData(
                "left_leg",
                Material.IRON_BLOCK,
                new Vector(-0.25, -0.9, 0), // Left side, down
                new Vector(0, 0, 0),
                new Vector(0.3, 0.9, 0.3));
        leftLeg.setParentId("body");
        golem.addPart(leftLeg);

        // Right Leg - Below body
        ModelPartData rightLeg = new ModelPartData(
                "right_leg",
                Material.IRON_BLOCK,
                new Vector(0.25, -0.9, 0), // Right side, down
                new Vector(0, 0, 0),
                new Vector(0.3, 0.9, 0.3));
        rightLeg.setParentId("body");
        golem.addPart(rightLeg);

        saveModel(golem);
        System.out.println("[CustomBosses] Created golem boss model");
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
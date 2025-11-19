package com.mcbzh.custombosses.storage;

import com.google.gson.*;
import com.mcbzh.custombosses.CustomBossesPlugin;
import com.mcbzh.custombosses.model.ModelData;
import org.bukkit.Material;
import org.bukkit.util.Vector;

import java.io.*;
import java.util.*;

public class ModelStorage {

    private final File modelsFolder;
    private final Gson gson;
    private final Map<String, ModelData> cache;

    public ModelStorage(CustomBossesPlugin plugin) {
        this.modelsFolder = new File(plugin.getDataFolder(), "models");
        this.modelsFolder.mkdirs();
        this.cache = new HashMap<>();

        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Vector.class, new VectorAdapter())
                .registerTypeAdapter(Material.class, new MaterialAdapter())
                .create();

        loadAll();

        // Create default models if none exist
        if (cache.isEmpty()) {
            createDefaultModels();
        }
    }

    private void loadAll() {
        File[] files = modelsFolder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return;

        for (File file : files) {
            try {
                String id = file.getName().replace(".json", "");
                ModelData model = loadFromFile(file);
                if (model != null) {
                    cache.put(id, model);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void save(ModelData model) {
        cache.put(model.getId(), model);
        File file = new File(modelsFolder, model.getId() + ".json");
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(model, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ModelData get(String id) {
        return cache.get(id);
    }

    public Collection<ModelData> getAllModels() {
        return cache.values();
    }

    public void reload() {
        cache.clear();
        loadAll();
    }

    private ModelData loadFromFile(File file) {
        try (FileReader reader = new FileReader(file)) {
            return gson.fromJson(reader, ModelData.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void createDefaultModels() {
        // Simple Cube
        ModelData cube = new ModelData("simple_cube");
        ModelData.PartData body = new ModelData.PartData("body");
        body.material = Material.GOLD_BLOCK;
        body.position = new Vector(0, 1, 0);
        cube.addPart(body);
        save(cube);

        // Golem
        ModelData golem = new ModelData("golem");
        golem.setHitboxSize(new Vector(1.5, 3.0, 1.5));

        // Body
        ModelData.PartData gBody = new ModelData.PartData("body");
        gBody.material = Material.IRON_BLOCK;
        gBody.position = new Vector(0, 1.5, 0);
        gBody.scale = new Vector(1.0, 1.5, 0.6);
        golem.addPart(gBody);

        // Head
        ModelData.PartData head = new ModelData.PartData("head");
        head.parentId = "body";
        head.material = Material.CARVED_PUMPKIN;
        head.position = new Vector(0, 1.0, 0);
        head.scale = new Vector(0.7, 0.7, 0.7);
        golem.addPart(head);

        // Arms
        ModelData.PartData leftArm = new ModelData.PartData("left_arm");
        leftArm.parentId = "body";
        leftArm.material = Material.IRON_BLOCK;
        leftArm.position = new Vector(-0.8, 0.3, 0);
        leftArm.scale = new Vector(0.3, 1.0, 0.3);
        golem.addPart(leftArm);

        ModelData.PartData rightArm = new ModelData.PartData("right_arm");
        rightArm.parentId = "body";
        rightArm.material = Material.IRON_BLOCK;
        rightArm.position = new Vector(0.8, 0.3, 0);
        rightArm.scale = new Vector(0.3, 1.0, 0.3);
        golem.addPart(rightArm);

        save(golem);
    }

    // GSON Adapters
    private static class VectorAdapter implements JsonSerializer<Vector>, JsonDeserializer<Vector> {
        @Override
        public JsonElement serialize(Vector src, java.lang.reflect.Type type, JsonSerializationContext ctx) {
            JsonObject obj = new JsonObject();
            obj.addProperty("x", src.getX());
            obj.addProperty("y", src.getY());
            obj.addProperty("z", src.getZ());
            return obj;
        }

        @Override
        public Vector deserialize(JsonElement json, java.lang.reflect.Type type, JsonDeserializationContext ctx) {
            JsonObject obj = json.getAsJsonObject();
            return new Vector(
                    obj.get("x").getAsDouble(),
                    obj.get("y").getAsDouble(),
                    obj.get("z").getAsDouble()
            );
        }
    }

    private static class MaterialAdapter implements JsonSerializer<Material>, JsonDeserializer<Material> {
        @Override
        public JsonElement serialize(Material src, java.lang.reflect.Type type, JsonSerializationContext ctx) {
            return new JsonPrimitive(src.name());
        }

        @Override
        public Material deserialize(JsonElement json, java.lang.reflect.Type type, JsonDeserializationContext ctx) {
            try {
                return Material.valueOf(json.getAsString());
            } catch (Exception e) {
                return Material.WHITE_CONCRETE;
            }
        }
    }
}
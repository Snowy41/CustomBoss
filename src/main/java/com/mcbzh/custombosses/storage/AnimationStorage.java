package com.mcbzh.custombosses.storage;

import com.google.gson.*;
import com.mcbzh.custombosses.CustomBossesPlugin;
import com.mcbzh.custombosses.animation.AnimationSystem;
import org.bukkit.util.Vector;

import java.io.*;
import java.util.*;

/**
 * Storage system for animations
 * Loads/saves animation data from JSON files
 */
public class AnimationStorage {

    private final File animationsFolder;
    private final Gson gson;
    private final Map<String, AnimationSystem.Animation> cache;

    public AnimationStorage(CustomBossesPlugin plugin) {
        this.animationsFolder = new File(plugin.getDataFolder(), "animations");
        this.animationsFolder.mkdirs();
        this.cache = new HashMap<>();

        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Vector.class, new VectorAdapter())
                .create();

        loadAll();
    }

    private void loadAll() {
        File[] files = animationsFolder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return;

        for (File file : files) {
            try {
                String id = file.getName().replace(".json", "");
                AnimationSystem.Animation animation = loadFromFile(file);
                if (animation != null) {
                    cache.put(id, animation);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void save(AnimationSystem.Animation animation) {
        cache.put(animation.id, animation);
        File file = new File(animationsFolder, animation.id + ".json");
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(animation, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public AnimationSystem.Animation get(String id) {
        return cache.get(id);
    }

    public List<AnimationSystem.Animation> getForModel(String modelId) {
        return cache.values().stream()
                .filter(a -> a.modelId.equals(modelId))
                .toList();
    }

    public Collection<AnimationSystem.Animation> getAllAnimations() {
        return cache.values();
    }

    public void reload() {
        cache.clear();
        loadAll();
    }

    private AnimationSystem.Animation loadFromFile(File file) {
        try (FileReader reader = new FileReader(file)) {
            return gson.fromJson(reader, AnimationSystem.Animation.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // GSON Adapter for Vector
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
}
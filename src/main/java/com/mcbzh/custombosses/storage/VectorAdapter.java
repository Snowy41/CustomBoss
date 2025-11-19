package com.mcbzh.custombosses.storage;

import com.google.gson.*;
import org.bukkit.util.Vector;

import java.lang.reflect.Type;

/**
 * GSON adapter for Bukkit Vector serialization
 */
public class VectorAdapter implements JsonSerializer<Vector>, JsonDeserializer<Vector> {

    @Override
    public JsonElement serialize(Vector src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();
        obj.addProperty("x", src.getX());
        obj.addProperty("y", src.getY());
        obj.addProperty("z", src.getZ());
        return obj;
    }

    @Override
    public Vector deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        double x = obj.get("x").getAsDouble();
        double y = obj.get("y").getAsDouble();
        double z = obj.get("z").getAsDouble();
        return new Vector(x, y, z);
    }
}

/**
 * GSON adapter for Material serialization
 */
class MaterialAdapter implements JsonSerializer<org.bukkit.Material>, JsonDeserializer<org.bukkit.Material> {

    @Override
    public JsonElement serialize(org.bukkit.Material src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.name());
    }

    @Override
    public org.bukkit.Material deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        try {
            return org.bukkit.Material.valueOf(json.getAsString());
        } catch (IllegalArgumentException e) {
            return org.bukkit.Material.WHITE_CONCRETE;
        }
    }
}
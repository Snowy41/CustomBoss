package com.mcbzh.custombosses.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mcbzh.custombosses.model.ModelData;
import org.bukkit.Material;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ModelSerializer {

    private final Gson gson;
    private final File modelsDir;

    public ModelSerializer(File dataFolder) {
        this.modelsDir = new File(dataFolder, "models");
        if (!modelsDir.exists()) {
            modelsDir.mkdirs();
        }

        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Vector.class, new VectorAdapter())
                .registerTypeAdapter(Material.class, new MaterialAdapter())
                .create();
    }

    public void saveModel(ModelData model) {
        File file = new File(modelsDir, model.getId() + ".json");
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(model, writer);
            System.out.println("[CustomBosses] Saved model: " + model.getId());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ModelData loadModel(String id) {
        File file = new File(modelsDir, id + ".json");
        if (!file.exists()) {
            System.out.println("[CustomBosses] Model file not found: " + id);
            return null;
        }

        try (FileReader reader = new FileReader(file)) {
            ModelData model = gson.fromJson(reader, ModelData.class);
            System.out.println("[CustomBosses] Loaded model: " + id + " with " + model.getParts().size() + " parts");
            return model;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public File[] listModelFiles() {
        return modelsDir.listFiles((dir, name) -> name.endsWith(".json"));
    }
}
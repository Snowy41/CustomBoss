package com.mcbzh.custombosses.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mcbzh.custombosses.model.ModelData;

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
                // We might need a TypeAdapter for Vector if GSON doesn't handle it well by
                // default
                // But usually it does (x, y, z fields)
                .create();
    }

    public void saveModel(ModelData model) {
        File file = new File(modelsDir, model.getId() + ".json");
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(model, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ModelData loadModel(String id) {
        File file = new File(modelsDir, id + ".json");
        if (!file.exists())
            return null;

        try (FileReader reader = new FileReader(file)) {
            return gson.fromJson(reader, ModelData.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public File[] listModelFiles() {
        return modelsDir.listFiles((dir, name) -> name.endsWith(".json"));
    }
}

package com.nexusmobs.resourcepack;

import com.nexusmobs.NexusMobsPlugin;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Material;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Generates resource pack with custom models for elite mobs
 */
public class ResourcePackGenerator {
    
    private final NexusMobsPlugin plugin;
    private final Gson gson;
    private final File resourcePackFolder;
    
    public ResourcePackGenerator(NexusMobsPlugin plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.resourcePackFolder = new File(plugin.getDataFolder(), "resourcepack");
    }
    
    /**
     * Generate the complete resource pack
     */
    public File generateResourcePack() throws IOException {
        plugin.getLogger().info("Generating resource pack...");
        
        // Create directory structure
        createDirectoryStructure();
        
        // Generate pack.mcmeta
        generatePackMcmeta();
        
        // Generate item models for each model type
        generateItemModels();
        
        // Generate model files
        generateModelFiles();
        
        // Create placeholder textures info file
        createTextureInfoFile();
        
        // Zip the resource pack
        File zipFile = zipResourcePack();
        
        plugin.getLogger().info("Resource pack generated: " + zipFile.getAbsolutePath());
        return zipFile;
    }
    
    private void createDirectoryStructure() throws IOException {
        // Main directories
        Files.createDirectories(resourcePackFolder.toPath());
        
        Path assetsPath = resourcePackFolder.toPath().resolve("assets/minecraft");
        Files.createDirectories(assetsPath.resolve("models/item"));
        Files.createDirectories(assetsPath.resolve("models/custom"));
        Files.createDirectories(assetsPath.resolve("textures/custom/mobs"));
        Files.createDirectories(assetsPath.resolve("textures/custom/items"));
    }
    
    private void generatePackMcmeta() throws IOException {
        JsonObject root = new JsonObject();
        JsonObject pack = new JsonObject();
        pack.addProperty("pack_format", 34); // Minecraft 1.21.x
        pack.addProperty("description", "NexusMobs Custom Resource Pack");
        root.add("pack", pack);
        
        writeJsonFile(new File(resourcePackFolder, "pack.mcmeta"), root);
    }
    
    private void generateItemModels() throws IOException {
        Map<Material, List<ModelOverride>> overridesByMaterial = new HashMap<>();
        
        // Collect all models grouped by their base material
        for (MobModel model : plugin.getModelManager().getMobModels().values()) {
            overridesByMaterial
                    .computeIfAbsent(model.getMaterial(), k -> new ArrayList<>())
                    .add(new ModelOverride(model.getCustomModelData(), "custom/" + model.getId()));
        }
        
        // Add custom item models
        plugin.getCustomItemManager().getCustomItems().forEach((id, item) -> {
            if (item.getCustomModelData() > 0) {
                overridesByMaterial
                        .computeIfAbsent(item.getMaterial(), k -> new ArrayList<>())
                        .add(new ModelOverride(item.getCustomModelData(), "custom/items/" + id));
            }
        });
        
        // Generate model files for each material
        for (Map.Entry<Material, List<ModelOverride>> entry : overridesByMaterial.entrySet()) {
            generateMaterialModel(entry.getKey(), entry.getValue());
        }
    }
    
    private void generateMaterialModel(Material material, List<ModelOverride> overrides) throws IOException {
        String materialName = material.name().toLowerCase();
        
        JsonObject root = new JsonObject();
        root.addProperty("parent", "minecraft:item/generated");
        
        JsonObject textures = new JsonObject();
        textures.addProperty("layer0", "minecraft:item/" + materialName);
        root.add("textures", textures);
        
        JsonArray overridesArray = new JsonArray();
        for (ModelOverride override : overrides) {
            JsonObject overrideObj = new JsonObject();
            JsonObject predicate = new JsonObject();
            predicate.addProperty("custom_model_data", override.customModelData);
            overrideObj.add("predicate", predicate);
            overrideObj.addProperty("model", override.modelPath);
            overridesArray.add(overrideObj);
        }
        root.add("overrides", overridesArray);
        
        Path modelPath = resourcePackFolder.toPath()
                .resolve("assets/minecraft/models/item/" + materialName + ".json");
        writeJsonFile(modelPath.toFile(), root);
    }
    
    private void generateModelFiles() throws IOException {
        // Generate mob model files
        for (MobModel model : plugin.getModelManager().getMobModels().values()) {
            generateMobModelFile(model);
        }
        
        // Generate item model files
        plugin.getCustomItemManager().getCustomItems().forEach((id, item) -> {
            if (item.getCustomModelData() > 0) {
                try {
                    generateItemModelFile(id, item.getMaterial());
                } catch (IOException e) {
                    plugin.getLogger().warning("Failed to generate model for item: " + id);
                }
            }
        });
    }
    
    private void generateMobModelFile(MobModel model) throws IOException {
        JsonObject root = new JsonObject();
        
        // For custom 3D models, use block/cube_all as parent or define custom geometry
        root.addProperty("parent", "minecraft:item/generated");
        
        JsonObject textures = new JsonObject();
        textures.addProperty("layer0", "minecraft:custom/mobs/" + model.getId());
        root.add("textures", textures);
        
        // Add display settings for proper scaling
        JsonObject display = new JsonObject();
        
        JsonObject head = new JsonObject();
        JsonArray rotation = new JsonArray();
        rotation.add(0); rotation.add(180); rotation.add(0);
        head.add("rotation", rotation);
        
        JsonArray translation = new JsonArray();
        translation.add(0); translation.add(0); translation.add(0);
        head.add("translation", translation);
        
        JsonArray scale = new JsonArray();
        double scaleVal = model.getScale();
        scale.add(scaleVal); scale.add(scaleVal); scale.add(scaleVal);
        head.add("scale", scale);
        
        display.add("head", head);
        root.add("display", display);
        
        Path modelPath = resourcePackFolder.toPath()
                .resolve("assets/minecraft/models/custom/" + model.getId() + ".json");
        writeJsonFile(modelPath.toFile(), root);
    }
    
    private void generateItemModelFile(String id, Material baseMaterial) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("parent", "minecraft:item/handheld");
        
        JsonObject textures = new JsonObject();
        textures.addProperty("layer0", "minecraft:custom/items/" + id);
        root.add("textures", textures);
        
        Path modelPath = resourcePackFolder.toPath()
                .resolve("assets/minecraft/models/custom/items/" + id + ".json");
        Files.createDirectories(modelPath.getParent());
        writeJsonFile(modelPath.toFile(), root);
    }
    
    private void createTextureInfoFile() throws IOException {
        StringBuilder info = new StringBuilder();
        info.append("=== NexusMobs Resource Pack Textures ===\n\n");
        info.append("This resource pack was auto-generated by NexusMobs.\n");
        info.append("You need to add your own textures to the following locations:\n\n");
        
        info.append("=== MOB TEXTURES ===\n");
        info.append("Location: assets/minecraft/textures/custom/mobs/\n");
        info.append("Required textures:\n");
        for (MobModel model : plugin.getModelManager().getMobModels().values()) {
            info.append("  - ").append(model.getId()).append(".png\n");
        }
        
        info.append("\n=== ITEM TEXTURES ===\n");
        info.append("Location: assets/minecraft/textures/custom/items/\n");
        info.append("Required textures:\n");
        plugin.getCustomItemManager().getCustomItems().forEach((id, item) -> {
            if (item.getCustomModelData() > 0) {
                info.append("  - ").append(id).append(".png\n");
            }
        });
        
        info.append("\n=== HOW TO ADD 3D MODELS ===\n");
        info.append("1. Create your model using BlockBench (https://blockbench.net/)\n");
        info.append("2. Export as 'Minecraft Java Block/Item'\n");
        info.append("3. Place the .json file in: assets/minecraft/models/custom/\n");
        info.append("4. Place textures in: assets/minecraft/textures/custom/mobs/\n");
        info.append("5. Update the model's texture references in the .json file\n\n");
        
        info.append("=== BLOCKBENCH SETUP FOR MOBS ===\n");
        info.append("1. Open BlockBench\n");
        info.append("2. Create new 'Java Block/Item' project\n");
        info.append("3. Design your mob model\n");
        info.append("4. Go to 'File > Export > Export Block/Item Model'\n");
        info.append("5. Make sure 'Display' settings are configured for 'Head' slot\n");
        info.append("6. The model should be scaled to fit on armor stand head\n\n");
        
        info.append("=== RECOMMENDED TEXTURE SIZE ===\n");
        info.append("- Mob textures: 64x64 or 128x128 pixels\n");
        info.append("- Item textures: 16x16 or 32x32 pixels\n");
        
        File infoFile = new File(resourcePackFolder, "TEXTURE_INFO.txt");
        Files.writeString(infoFile.toPath(), info.toString());
    }
    
    private File zipResourcePack() throws IOException {
        File zipFile = new File(plugin.getDataFolder(), "NexusMobs-ResourcePack.zip");
        
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            zipDirectory(resourcePackFolder, resourcePackFolder.getName(), zos);
        }
        
        return zipFile;
    }
    
    private void zipDirectory(File folder, String baseName, ZipOutputStream zos) throws IOException {
        File[] files = folder.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            if (file.isDirectory()) {
                // Don't include the base folder name in zip
                String entryName = baseName.equals(resourcePackFolder.getName()) 
                        ? file.getName() 
                        : baseName + "/" + file.getName();
                zipDirectory(file, entryName, zos);
            } else {
                String entryName = baseName.equals(resourcePackFolder.getName())
                        ? file.getName()
                        : baseName + "/" + file.getName();
                        
                // Skip the base resourcepack folder from path
                if (entryName.startsWith(resourcePackFolder.getName() + "/")) {
                    entryName = entryName.substring(resourcePackFolder.getName().length() + 1);
                }
                
                ZipEntry entry = new ZipEntry(entryName);
                zos.putNextEntry(entry);
                Files.copy(file.toPath(), zos);
                zos.closeEntry();
            }
        }
    }
    
    private void writeJsonFile(File file, JsonObject json) throws IOException {
        Files.createDirectories(file.getParentFile().toPath());
        try (Writer writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            gson.toJson(json, writer);
        }
    }
    
    /**
     * Calculate SHA-1 hash of resource pack for server resource pack
     */
    public String calculateSHA1(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        try (InputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        
        StringBuilder sb = new StringBuilder();
        for (byte b : digest.digest()) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    
    // Helper class
    private static class ModelOverride {
        int customModelData;
        String modelPath;
        
        ModelOverride(int customModelData, String modelPath) {
            this.customModelData = customModelData;
            this.modelPath = modelPath;
        }
    }
}


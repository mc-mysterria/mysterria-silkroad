package net.mysterria.silkroad.utils;

import net.mysterria.silkroad.SilkRoad;
import net.mysterria.silkroad.utils.TranslationManager;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ResourcePackGenerator {

    /**
     * Generates resource pack translation files from the .properties files.
     * This creates en_us.json and uk_ua.json in the resource pack assets folder.
     */
    public static void generateResourcePackTranslations() {
        try {
            SilkRoad.getInstance().log("Generating resource pack translations...");
            Path resourcePackDir = Paths.get(SilkRoad.getInstance().getDataFolder().getPath(), "resourcepack");
            Files.createDirectories(resourcePackDir);
            generatePackMcmeta(resourcePackDir);
            
            Properties enProperties = loadPropertiesFile("lang/lang_en.properties");
            if (enProperties != null) {
                generateJsonTranslationFile(enProperties, "en_us");
            }
            
            Properties ukProperties = loadPropertiesFile("lang/lang_uk.properties");
            if (ukProperties != null) {
                generateJsonTranslationFile(ukProperties, "uk_ua");
            }
            
            SilkRoad.getInstance().log("Resource pack translations generated successfully!");
            
        } catch (Exception e) {
            SilkRoad.getInstance().getLogger().warning("Failed to generate resource pack translations: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static Properties loadPropertiesFile(String resourcePath) {
        try (InputStream inputStream = SilkRoad.getInstance().getResource(resourcePath)) {
            if (inputStream == null) {
                SilkRoad.getInstance().getLogger().warning("Properties file not found: " + resourcePath);
                return null;
            }
            
            Properties properties = new Properties();
            properties.load(new InputStreamReader(inputStream, "UTF-8"));
            return properties;
            
        } catch (IOException e) {
            SilkRoad.getInstance().getLogger().warning("Failed to load properties file " + resourcePath + ": " + e.getMessage());
            return null;
        }
    }
    
    private static void generateJsonTranslationFile(Properties properties, String locale) throws IOException {
        String namespace = TranslationManager.getNamespace();
        Path outputDir = Paths.get(SilkRoad.getInstance().getDataFolder().getPath(),
                                   "resourcepack", "assets", namespace, "lang");
        Files.createDirectories(outputDir);
        
        Path outputFile = outputDir.resolve(locale + ".json");
        try (FileWriter writer = new FileWriter(outputFile.toFile(), java.nio.charset.StandardCharsets.UTF_8)) {
            writer.write(generateJsonFromProperties(properties, namespace));
        }

        SilkRoad.getInstance().log("Generated translation file: " + outputFile + " with " + properties.size() + " keys");
    }
    
    /**
     * Converts Properties to JSON format manually without external dependencies
     */
    private static String generateJsonFromProperties(Properties properties, String namespace) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");

        String[] keys = properties.stringPropertyNames().toArray(new String[0]);
        java.util.Arrays.sort(keys);
        
        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];
            String value = properties.getProperty(key);
            
            String namespacedKey = namespace + ":" + key;
            namespacedKey = escapeJsonString(namespacedKey);
            value = escapeJsonString(value);
            
            json.append("  \"").append(namespacedKey).append("\": \"").append(value).append("\"");
            if (i < keys.length - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        
        json.append("}");
        return json.toString();
    }
    
    /**
     * Escapes special characters for JSON strings
     */
    private static String escapeJsonString(String str) {
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    /**
     * Generates the pack.mcmeta file for the resource pack
     */
    private static void generatePackMcmeta(Path resourcePackDir) throws IOException {
        Path packMcmetaFile = resourcePackDir.resolve("pack.mcmeta");
        String packMcmetaContent = "{\n" +
                "  \"pack\": {\n" +
                "    \"pack_format\": 34,\n" +
                "    \"description\": \"SilkRoad Translations\"\n" +
                "  }\n" +
                "}";
        
        Files.writeString(packMcmetaFile, packMcmetaContent);
        SilkRoad.getInstance().log("Generated pack.mcmeta file");
    }
    
    /**
     * Generates and zips the resource pack, then returns the path to the ZIP file
     */
    public static Path generateResourcePackZip() throws IOException {
        generateResourcePackTranslations();
        
        Path resourcePackDir = Paths.get(SilkRoad.getInstance().getDataFolder().getPath(), "resourcepack");
        Path zipFile = Paths.get(SilkRoad.getInstance().getDataFolder().getPath(), "SilkRoad-ResourcePack.zip");
        
        try (ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            zipDirectory(resourcePackDir, resourcePackDir, zipOut);
        }

        SilkRoad.getInstance().log("Resource pack ZIP created: " + zipFile.getFileName());
        return zipFile;
    }
    
    /**
     * Recursively zips a directory
     */
    private static void zipDirectory(Path sourceDir, Path basePath, ZipOutputStream zipOut) throws IOException {
        Files.walk(sourceDir).forEach(path -> {
            try {
                if (!Files.isDirectory(path)) {
                    String relativePath = basePath.relativize(path).toString().replace('\\', '/');
                    ZipEntry zipEntry = new ZipEntry(relativePath);
                    zipOut.putNextEntry(zipEntry);
                    Files.copy(path, zipOut);
                    zipOut.closeEntry();
                }
            } catch (IOException e) {
                throw new RuntimeException("Error zipping file: " + path, e);
            }
        });
    }
    
    /**
     * Updates the existing resource pack files with current translations
     */
    public static void updateResourcePackTranslations() {
        generateResourcePackTranslations();
        SilkRoad.getInstance().log("Resource pack translations updated! Reload your resource pack (F3+T) to see changes.");
    }
}
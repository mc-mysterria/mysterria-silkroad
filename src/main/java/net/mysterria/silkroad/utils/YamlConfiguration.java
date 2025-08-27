package net.mysterria.silkroad.utils;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class YamlConfiguration {

    private final File file;
    private final Yaml yaml;
    private Map<String, Object> data;

    public YamlConfiguration(File file) {
        this.file = file;

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        this.yaml = new Yaml(options);

        ensureFileExists();
        load();
    }

    private void ensureFileExists() {
        try {
            File parentDir = file.getParentFile();
            if (!parentDir.exists() && parentDir.mkdirs()) {
                System.out.println("Created directory: " + parentDir.getAbsolutePath());
            }

            if (!file.exists()) {
                if (file.createNewFile()) {
                    System.out.println("Created file: " + file.getAbsolutePath());
                    data = new HashMap<>();
                    save();
                } else {
                    System.err.println("Failed to create file: " + file.getAbsolutePath());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void load() {
        try (InputStream inputStream = new FileInputStream(file)) {
            data = yaml.load(inputStream);
            if (data == null) {
                data = new HashMap<>();
            }
        } catch (IOException e) {
            e.printStackTrace();
            data = new HashMap<>();
        }
    }

    public void save() {
        try (Writer writer = new FileWriter(file)) {
            yaml.dump(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Object get(String path) {
        return data.get(path);
    }

    public void set(String path, Object value) {
        data.put(path, value);
    }

    public Map<String, Object> getSection(String path) {
        Object section = data.get(path);
        return (section instanceof Map) ? (Map<String, Object>) section : new HashMap<>();
    }
}

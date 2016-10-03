package net.xtrafrancyz.degustator.storage;

import com.google.gson.reflect.TypeToken;

import net.xtrafrancyz.degustator.Degustator;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;

/**
 * @author xtrafrancyz
 */
public class FileJsonStorage implements IStorage {
    private final Object lock = new Object();
    private final File file;
    private HashMap<String, String> data;
    
    public FileJsonStorage(File file) {
        this.file = file;
        this.data = new HashMap<>();
    }
    
    @Override
    public void load() {
        synchronized (lock) {
            if (!file.exists()) {
                data.clear();
                save();
                return;
            }
            try {
                HashMap<String, String> map = Degustator.instance().gson.fromJson(
                    Files.readAllLines(file.toPath()).stream()
                        .map(String::trim)
                        .reduce((a, b) -> a += b)
                        .orElse("{}"), new TypeToken<HashMap<String, String>>() {}.getType());
                data.clear();
                data.putAll(map);
            } catch (IOException e) {
                throw new RuntimeException("Cannot load config", e);
            }
        }
    }
    
    @Override
    public void save() {
        synchronized (lock) {
            String json = Degustator.instance().gson.toJson(data);
            file.delete();
            try {
                Files.write(file.toPath(), json.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW);
            } catch (IOException e) {
                throw new RuntimeException("Cannot save config", e);
            }
        }
    }
    
    @Override
    public String set(String key, String value) {
        return data.put(key, value);
    }
    
    @Override
    public String get(String key) {
        return data.get(key);
    }
    
    @Override
    public String remove(String key) {
        return data.remove(key);
    }
}

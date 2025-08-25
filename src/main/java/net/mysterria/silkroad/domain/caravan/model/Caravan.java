package net.mysterria.silkroad.domain.caravan.model;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
public class Caravan {
    
    private final String id;
    private String name;
    private Location location;
    private Map<Material, Integer> inventory;
    private long createdAt;
    private boolean active;
    // Territory represented as set of keys: "world:x:z"
    private Set<String> territoryChunks = new HashSet<>();
    
    public Caravan(String id, String name, Location location) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.inventory = new HashMap<>();
        this.createdAt = System.currentTimeMillis();
        this.active = true;
    }
    
    public void addResource(Material material, int amount) {
        inventory.put(material, inventory.getOrDefault(material, 0) + amount);
    }
    
    public boolean removeResource(Material material, int amount) {
        int current = inventory.getOrDefault(material, 0);
        if (current >= amount) {
            inventory.put(material, current - amount);
            if (inventory.get(material) == 0) {
                inventory.remove(material);
            }
            return true;
        }
        return false;
    }
    
    public int getResourceAmount(Material material) {
        return inventory.getOrDefault(material, 0);
    }
    
    public double distanceTo(Caravan other) {
        if (!location.getWorld().equals(other.location.getWorld())) {
            return Double.MAX_VALUE;
        }
        return location.distance(other.location);
    }
    
    public boolean containsChunk(String worldName, int x, int z) {
        return territoryChunks.contains(worldName + ":" + x + ":" + z);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Caravan caravan = (Caravan) obj;
        return Objects.equals(id, caravan.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
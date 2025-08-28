package net.mysterria.silkroad.domain.caravan.model;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
public class Caravan {
    
    public static final int MAX_INVENTORY_SLOTS = 36;
    
    private final String id;
    private String name;
    private Location location;
    private Map<Material, Integer> inventory;
    private List<ItemStack> itemInventory = new ArrayList<>();
    private long createdAt;
    private boolean active;
    // Territory represented as set of keys: "world:x:z"
    private Set<String> territoryChunks = new HashSet<>();
    // Member management - all members have equal rights
    private Set<UUID> members = new HashSet<>();
    
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
    
    // New ItemStack-based methods that preserve NBT data
    public boolean addItemStack(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR) return false;
        
        // Try to merge with existing similar items first
        for (ItemStack existing : itemInventory) {
            if (existing.isSimilar(itemStack)) {
                int newAmount = existing.getAmount() + itemStack.getAmount();
                if (newAmount <= existing.getMaxStackSize()) {
                    existing.setAmount(newAmount);
                    return true;
                }
            }
        }
        
        // If we can't merge, check if we have space for a new stack
        if (itemInventory.size() >= MAX_INVENTORY_SLOTS) {
            return false; // Inventory is full
        }
        
        // Add as new stack
        itemInventory.add(itemStack.clone());
        return true;
    }
    
    public boolean canAddItemStack(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR) return false;
        
        // Check if we can merge with existing similar items
        for (ItemStack existing : itemInventory) {
            if (existing.isSimilar(itemStack)) {
                int newAmount = existing.getAmount() + itemStack.getAmount();
                if (newAmount <= existing.getMaxStackSize()) {
                    return true; // Can merge
                }
            }
        }
        
        // Check if we have space for a new stack
        return itemInventory.size() < MAX_INVENTORY_SLOTS;
    }
    
    public int getAvailableSlots() {
        return MAX_INVENTORY_SLOTS - itemInventory.size();
    }
    
    public boolean removeItemStack(ItemStack itemToRemove, int amount) {
        if (itemToRemove == null || itemToRemove.getType() == Material.AIR) return false;
        
        int remaining = amount;
        for (int i = itemInventory.size() - 1; i >= 0; i--) {
            ItemStack existing = itemInventory.get(i);
            if (existing.isSimilar(itemToRemove)) {
                int availableAmount = existing.getAmount();
                if (availableAmount <= remaining) {
                    remaining -= availableAmount;
                    itemInventory.remove(i);
                } else {
                    existing.setAmount(availableAmount - remaining);
                    remaining = 0;
                }
                
                if (remaining == 0) {
                    return true;
                }
            }
        }
        
        return remaining == 0;
    }
    
    public int getItemStackAmount(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR) return 0;
        
        int total = 0;
        for (ItemStack existing : itemInventory) {
            if (existing.isSimilar(itemStack)) {
                total += existing.getAmount();
            }
        }
        return total;
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
    
    public void addMember(UUID playerId) {
        members.add(playerId);
    }
    
    public void removeMember(UUID playerId) {
        members.remove(playerId);
    }
    
    public boolean isMember(UUID playerId) {
        return members.contains(playerId);
    }
    
    public boolean hasAccess(UUID playerId) {
        return isMember(playerId);
    }
    
    // Deprecated methods for backwards compatibility
    @Deprecated
    public void addOwner(UUID playerId) {
        addMember(playerId);
    }
    
    @Deprecated
    public void removeOwner(UUID playerId) {
        removeMember(playerId);
    }
    
    @Deprecated
    public boolean isOwner(UUID playerId) {
        return isMember(playerId);
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
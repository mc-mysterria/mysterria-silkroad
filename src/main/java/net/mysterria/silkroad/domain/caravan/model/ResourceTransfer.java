package net.mysterria.silkroad.domain.caravan.model;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
public class ResourceTransfer {
    
    private String id;
    private String sourceCaravanId;
    private String destinationCaravanId;
    private UUID playerId;
    private String playerName;
    private Map<Material, Integer> resources; // Legacy Material-based resources for compatibility
    private List<ItemStack> itemResources = new ArrayList<>(); // New ItemStack-based resources
    private long createdAt;
    private long deliveryTime;
    private double distance;
    private int cost;
    private TransferStatus status;
    
    public enum TransferStatus {
        PENDING,
        IN_TRANSIT,
        DELIVERED,
        FAILED
    }
    
    public ResourceTransfer(String id, String sourceCaravanId, String destinationCaravanId, 
                           Player player, Map<Material, Integer> resources, 
                           double distance, int cost, long deliveryTime) {
        this.id = id;
        this.sourceCaravanId = sourceCaravanId;
        this.destinationCaravanId = destinationCaravanId;
        this.playerId = player.getUniqueId();
        this.playerName = player.getName();
        this.resources = new HashMap<>(resources);
        this.createdAt = System.currentTimeMillis();
        this.deliveryTime = deliveryTime;
        this.distance = distance;
        this.cost = cost;
        this.status = TransferStatus.PENDING;
    }
    
    // New ItemStack-based constructor
    public ResourceTransfer(String id, String sourceCaravanId, String destinationCaravanId, 
                           Player player, List<ItemStack> itemResources, 
                           double distance, int cost, long deliveryTime) {
        this.id = id;
        this.sourceCaravanId = sourceCaravanId;
        this.destinationCaravanId = destinationCaravanId;
        this.playerId = player.getUniqueId();
        this.playerName = player.getName();
        this.itemResources = new ArrayList<>();
        for (ItemStack item : itemResources) {
            if (item != null && item.getType() != Material.AIR) {
                this.itemResources.add(item.clone());
            }
        }
        this.resources = new HashMap<>(); // Keep empty for compatibility
        this.createdAt = System.currentTimeMillis();
        this.deliveryTime = deliveryTime;
        this.distance = distance;
        this.cost = cost;
        this.status = TransferStatus.PENDING;
    }
    
    // Constructor for deserialization from storage (legacy Material-based)
    public ResourceTransfer(String id, String sourceCaravanId, String destinationCaravanId,
                           UUID playerId, String playerName, Map<Material, Integer> resources,
                           long createdAt, long deliveryTime, double distance, int cost) {
        this.id = id;
        this.sourceCaravanId = sourceCaravanId;
        this.destinationCaravanId = destinationCaravanId;
        this.playerId = playerId;
        this.playerName = playerName;
        this.resources = new HashMap<>(resources);
        this.itemResources = new ArrayList<>();
        this.createdAt = createdAt;
        this.deliveryTime = deliveryTime;
        this.distance = distance;
        this.cost = cost;
        this.status = TransferStatus.PENDING;
    }
    
    // Constructor for deserialization from storage (new ItemStack-based)
    public ResourceTransfer(String id, String sourceCaravanId, String destinationCaravanId,
                           UUID playerId, String playerName, List<ItemStack> itemResources,
                           long createdAt, long deliveryTime, double distance, int cost) {
        this.id = id;
        this.sourceCaravanId = sourceCaravanId;
        this.destinationCaravanId = destinationCaravanId;
        this.playerId = playerId;
        this.playerName = playerName;
        this.resources = new HashMap<>(); // Empty for compatibility
        this.itemResources = new ArrayList<>();
        if (itemResources != null) {
            for (ItemStack item : itemResources) {
                if (item != null && item.getType() != Material.AIR) {
                    this.itemResources.add(item.clone());
                }
            }
        }
        this.createdAt = createdAt;
        this.deliveryTime = deliveryTime;
        this.distance = distance;
        this.cost = cost;
        this.status = TransferStatus.PENDING;
    }
    
    public boolean isReadyForDelivery() {
        return status == TransferStatus.IN_TRANSIT && System.currentTimeMillis() >= deliveryTime;
    }
    
    public long getRemainingTime() {
        return Math.max(0, deliveryTime - System.currentTimeMillis());
    }
    
    /**
     * Check if this transfer contains only ItemStack-based resources (modern)
     */
    public boolean isItemStackBased() {
        return !itemResources.isEmpty() && (resources == null || resources.isEmpty());
    }
    
    /**
     * Check if this transfer contains only legacy Material-based resources
     */
    public boolean isLegacyMaterialBased() {
        return !resources.isEmpty() && itemResources.isEmpty();
    }
    
    /**
     * Get all resources as ItemStacks (converts legacy Materials if needed)
     */
    public List<ItemStack> getAllAsItemStacks() {
        List<ItemStack> allItems = new ArrayList<>(itemResources);
        
        // Convert legacy Material resources to ItemStacks
        if (resources != null) {
            for (Map.Entry<Material, Integer> entry : resources.entrySet()) {
                Material material = entry.getKey();
                int amount = entry.getValue();
                
                // Split into stacks respecting max stack size
                int maxStack = material.getMaxStackSize();
                while (amount > 0) {
                    int stackAmount = Math.min(amount, maxStack);
                    allItems.add(new ItemStack(material, stackAmount));
                    amount -= stackAmount;
                }
            }
        }
        
        return allItems;
    }
    
    /**
     * Get the total number of items being transferred
     */
    public int getTotalItemCount() {
        int count = 0;
        
        // Count ItemStack items
        for (ItemStack item : itemResources) {
            count += item.getAmount();
        }
        
        // Count legacy Material items
        if (resources != null) {
            for (Integer amount : resources.values()) {
                count += amount;
            }
        }
        
        return count;
    }
    
    /**
     * Creates a new ItemStack-based transfer by converting from a Material-based one
     */
    public static ResourceTransfer convertFromMaterial(ResourceTransfer materialTransfer) {
        if (materialTransfer.isItemStackBased()) {
            return materialTransfer; // Already ItemStack-based
        }
        
        List<ItemStack> itemStacks = new ArrayList<>();
        
        // Convert Materials to ItemStacks
        for (Map.Entry<Material, Integer> entry : materialTransfer.resources.entrySet()) {
            Material material = entry.getKey();
            int amount = entry.getValue();
            
            // Create ItemStacks respecting max stack size
            int maxStack = material.getMaxStackSize();
            while (amount > 0) {
                int stackAmount = Math.min(amount, maxStack);
                itemStacks.add(new ItemStack(material, stackAmount));
                amount -= stackAmount;
            }
        }
        
        // Create new ResourceTransfer with ItemStacks
        ResourceTransfer newTransfer = new ResourceTransfer(
            materialTransfer.id,
            materialTransfer.sourceCaravanId,
            materialTransfer.destinationCaravanId,
            materialTransfer.playerId,
            materialTransfer.playerName,
            itemStacks,
            materialTransfer.createdAt,
            materialTransfer.deliveryTime,
            materialTransfer.distance,
            materialTransfer.cost
        );
        
        newTransfer.setStatus(materialTransfer.status);
        return newTransfer;
    }
}
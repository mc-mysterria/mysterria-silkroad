package net.mysterria.silkroad.utils;

import net.mysterria.silkroad.SilkRoad;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class ShardUtils {
    
    private static final NamespacedKey SHARD_KEY = new NamespacedKey(SilkRoad.getInstance(), "shard");
    
    /**
     * Gets the shard value from an item's persistent data container
     */
    public static int getShardValue(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return 0;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return 0;
        }
        
        return meta.getPersistentDataContainer().getOrDefault(SHARD_KEY, PersistentDataType.INTEGER, 0);
    }
    
    /**
     * Sets the shard value on an item's persistent data container
     */
    public static void setShardValue(ItemStack item, int shardValue) {
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        
        meta.getPersistentDataContainer().set(SHARD_KEY, PersistentDataType.INTEGER, shardValue);
        item.setItemMeta(meta);
    }
    
    /**
     * Checks if an item has shard value
     */
    public static boolean hasShardValue(ItemStack item) {
        return getShardValue(item) > 0;
    }
    
    /**
     * Gets the total shard value from a player's inventory
     */
    public static int getTotalPlayerShards(Player player) {
        int totalShards = 0;
        
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null) {
                totalShards += getShardValue(item) * item.getAmount();
            }
        }
        
        return totalShards;
    }
    
    /**
     * Consumes shards from a player's inventory
     * Returns true if successful, false if not enough shards
     */
    public static boolean consumeShards(Player player, int requiredShards) {
        if (getTotalPlayerShards(player) < requiredShards) {
            return false;
        }
        
        int remaining = requiredShards;
        ItemStack[] contents = player.getInventory().getContents();
        
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item == null) continue;
            
            int shardValue = getShardValue(item);
            if (shardValue <= 0) continue;
            
            int totalItemShards = shardValue * item.getAmount();
            
            if (totalItemShards <= remaining) {
                // Consume entire stack
                remaining -= totalItemShards;
                player.getInventory().setItem(i, null);
            } else {
                // Consume partial stack
                int itemsToRemove = (int) Math.ceil((double) remaining / shardValue);
                remaining = 0;
                
                if (itemsToRemove >= item.getAmount()) {
                    player.getInventory().setItem(i, null);
                } else {
                    item.setAmount(item.getAmount() - itemsToRemove);
                    player.getInventory().setItem(i, item);
                }
            }
        }
        
        return remaining == 0;
    }
    
    /**
     * Creates a display string for shard cost
     */
    public static String formatShardCost(int cost) {
        return cost + " Shards";
    }
}
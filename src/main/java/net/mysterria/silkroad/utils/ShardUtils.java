package net.mysterria.silkroad.utils;

import net.mysterria.silkroad.SilkRoad;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public class ShardUtils {

    private static NamespacedKey SHARD_KEY;

    public ShardUtils() {
        Plugin sacredOrderPlugin = Bukkit.getPluginManager().getPlugin("SacredOrder");
        if (sacredOrderPlugin != null) {
            SHARD_KEY = new NamespacedKey(sacredOrderPlugin, "shard");
        } else {
            SilkRoad.getInstance().log("SacredOrder plugin not found - shard value disabled");
        }
    }

    /**
     * Checks if an item has shard value
     */
    public boolean hasShardValue(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        return meta.getPersistentDataContainer().has(SHARD_KEY, PersistentDataType.BOOLEAN);
    }

    /**
     * Gets the total shard value from a player's inventory
     */
    public int getTotalPlayerShards(Player player) {
        int totalShards = 0;

        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null) {
                if (hasShardValue(item)) {
                    totalShards += item.getAmount();
                }
            }
        }

        return totalShards;
    }

    /**
     * Consumes shards from a player's inventory
     * Returns true if successful, false if not enough shards
     */
    public boolean consumeShards(Player player, int requiredShards) {
        if (getTotalPlayerShards(player) < requiredShards) {
            return false;
        }

        int remaining = requiredShards;
        ItemStack[] contents = player.getInventory().getContents();

        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item == null) continue;

            if (!hasShardValue(item)) continue;

            int totalItemShards = item.getAmount();

            if (totalItemShards <= remaining) {
                remaining -= totalItemShards;
                player.getInventory().setItem(i, null);
            } else {
                int itemsToRemove = remaining;
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
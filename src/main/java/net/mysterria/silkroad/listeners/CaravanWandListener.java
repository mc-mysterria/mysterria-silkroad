package net.mysterria.silkroad.listeners;

import net.mysterria.silkroad.domain.caravan.manager.CaravanManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CaravanWandListener implements Listener {
    
    private final CaravanManager caravanManager;
    
    public CaravanWandListener(CaravanManager caravanManager) {
        this.caravanManager = caravanManager;
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Process only main-hand interactions to avoid double-firing (off-hand)
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (!isCaravanWand(item)) {
            return;
        }
        
        if (!player.hasPermission("silkroad.caravan.admin")) {
            player.sendMessage("§cYou don't have permission to use the caravan wand!");
            event.setCancelled(true);
            return;
        }
        
        event.setCancelled(true);
        
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
            handleSelectChunk(player, true);
        } else if (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_AIR) {
            if (player.isSneaking()) {
                handleClearAllSelections(player);
            } else {
                handleSelectChunk(player, false);
            }
        }
    }
    
    private boolean isCaravanWand(ItemStack item) {
        if (item == null || item.getType() != Material.BLAZE_ROD) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        
        return meta.hasCustomModelData() && meta.getCustomModelData() == 12345 && 
               meta.hasDisplayName() && meta.getDisplayName().contains("Caravan Territory Wand");
    }
    
    private void handleSelectChunk(Player player, boolean add) {
        var chunk = player.getLocation().getChunk();
        String worldName = chunk.getWorld().getName();
        int cx = chunk.getX();
        int cz = chunk.getZ();
        boolean changed = caravanManager.toggleSelect(worldName, cx, cz, player.getUniqueId(), add);
        int count = caravanManager.getSelection(player.getUniqueId()).size();
        if (add) {
            if (changed) {
                player.sendMessage("§aSelected chunk §e" + worldName + ":" + cx + ":" + cz + "§a. Total selected: §d" + count);
            } else {
                player.sendMessage("§eChunk already selected §7(" + worldName + ":" + cx + ":" + cz + ")§e. Total: §d" + count);
            }
        } else {
            if (changed) {
                player.sendMessage("§cDeselected chunk §e" + worldName + ":" + cx + ":" + cz + "§c. Total selected: §d" + count);
            } else {
                player.sendMessage("§eChunk was not selected §7(" + worldName + ":" + cx + ":" + cz + ")§e. Total: §d" + count);
            }
        }
        player.sendActionBar("§6Selection: §e" + count + " §7chunk(s)");
    }
    
    private void handleClearAllSelections(Player player) {
        int previousCount = caravanManager.getSelection(player.getUniqueId()).size();
        
        if (previousCount == 0) {
            player.sendMessage("§7No chunks selected to clear.");
            player.sendActionBar("§6Selection: §e0 §7chunk(s)");
            return;
        }
        
        caravanManager.clearSelection(player.getUniqueId());
        player.sendMessage("§c✖ Cleared all selections! §7(" + previousCount + " chunk" + (previousCount == 1 ? "" : "s") + " deselected)");
        player.sendActionBar("§6Selection: §e0 §7chunk(s)");
    }
}
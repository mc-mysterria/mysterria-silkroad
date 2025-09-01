package net.mysterria.silkroad.listeners;

import net.mysterria.silkroad.domain.caravan.manager.CaravanManager;
import net.mysterria.silkroad.utils.TranslationUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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
            player.sendMessage(TranslationUtil.translatable("wand.permission.denied", NamedTextColor.RED));
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
                player.sendMessage(TranslationUtil.translatable("wand.chunk.selected", NamedTextColor.GREEN, worldName, cx, cz, count));
            } else {
                player.sendMessage(TranslationUtil.translatable("wand.chunk.already.selected", NamedTextColor.YELLOW, worldName, cx, cz, count));
            }
        } else {
            if (changed) {
                player.sendMessage(TranslationUtil.translatable("wand.chunk.deselected", NamedTextColor.RED, worldName, cx, cz, count));
            } else {
                player.sendMessage(TranslationUtil.translatable("wand.chunk.not.selected", NamedTextColor.YELLOW, worldName, cx, cz, count));
            }
        }
        player.sendActionBar(Component.text("Selection: " + count + " chunk(s)", NamedTextColor.GOLD));
    }
    
    private void handleClearAllSelections(Player player) {
        int previousCount = caravanManager.getSelection(player.getUniqueId()).size();
        
        if (previousCount == 0) {
            player.sendMessage(TranslationUtil.translatable("wand.no.chunks.to.clear", NamedTextColor.GRAY));
            player.sendActionBar(Component.text("Selection: 0 chunks", NamedTextColor.GOLD));
            return;
        }
        
        caravanManager.clearSelection(player.getUniqueId());
        player.sendMessage(TranslationUtil.translatable("wand.cleared.selections", NamedTextColor.RED, previousCount, (previousCount == 1 ? "" : "s")));
        player.sendActionBar(Component.text("Selection: 0 chunks", NamedTextColor.GOLD));
    }
}
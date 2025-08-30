package net.mysterria.silkroad.domain.caravan.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.mysterria.silkroad.domain.caravan.manager.CaravanManager;
import net.mysterria.silkroad.domain.caravan.model.ResourceTransfer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TransferClaimGUI {
    
    private final CaravanManager caravanManager;
    private final Player player;
    
    public TransferClaimGUI(CaravanManager caravanManager, Player player) {
        this.caravanManager = caravanManager;
        this.player = player;
    }
    
    public void open() {
        List<ResourceTransfer> deliveredTransfers = caravanManager.getDeliveredTransfersForPlayer(player.getUniqueId());
        
        int rows = Math.max(1, Math.min(6, (deliveredTransfers.size() / 9) + 1));
        
        Gui gui = Gui.gui()
                .title(Component.text("Awaiting Transfers", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false))
                .rows(rows)
                .disableAllInteractions()
                .create();
        
        if (deliveredTransfers.isEmpty()) {
            // Show empty message
            ItemStack emptyItem = new ItemStack(Material.BARRIER);
            ItemMeta emptyMeta = emptyItem.getItemMeta();
            emptyMeta.displayName(Component.text("No transfers available", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
            emptyMeta.lore(List.of(
                    Component.text("You have no delivered transfers", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("to claim at the moment.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
            ));
            emptyItem.setItemMeta(emptyMeta);
            
            gui.setItem(4, ItemBuilder.from(emptyItem).asGuiItem());
        } else {
            // Show delivered transfers
            for (int i = 0; i < Math.min(deliveredTransfers.size(), 45); i++) {
                ResourceTransfer transfer = deliveredTransfers.get(i);
                gui.setItem(i, createTransferItem(transfer));
            }
        }
        
        // Add close button
        ItemStack closeItem = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta closeMeta = closeItem.getItemMeta();
        closeMeta.displayName(Component.text("Close", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        closeItem.setItemMeta(closeMeta);
        
        gui.setItem(rows * 9 - 1, ItemBuilder.from(closeItem).asGuiItem(event -> {
            event.setCancelled(true);
            player.closeInventory();
        }));
        
        gui.open(player);
    }
    
    private GuiItem createTransferItem(ResourceTransfer transfer) {
        ItemStack item = new ItemStack(Material.CHEST_MINECART);
        ItemMeta meta = item.getItemMeta();
        
        String sourceCaravan = transfer.getSourceCaravanId();
        String destinationCaravan = transfer.getDestinationCaravanId();
        
        meta.displayName(Component.text("Transfer: " + sourceCaravan + " → " + destinationCaravan, 
                NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Status: Delivered", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Distance: " + String.format("%.1f", transfer.getDistance()) + " blocks", 
                NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Cost: " + transfer.getCost() + " shards", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        
        // Show items being transferred using unified method
        lore.add(Component.text("Items:", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        
        List<ItemStack> allItems = transfer.getAllAsItemStacks();
        if (allItems.isEmpty()) {
            lore.add(Component.text("  • No items", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        } else {
            // Group similar items together for cleaner display
            Map<String, Integer> itemCounts = new HashMap<>();
            for (ItemStack transferItem : allItems) {
                String displayName = getItemDisplayName(transferItem);
                itemCounts.merge(displayName, transferItem.getAmount(), Integer::sum);
            }
            
            itemCounts.forEach((displayName, amount) -> {
                lore.add(Component.text("  • " + displayName + " x" + amount, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
            });
        }
        
        // Add transfer type indicator
        lore.add(Component.empty());
        if (transfer.isItemStackBased()) {
            lore.add(Component.text("Transfer Type: ItemStack (NBT preserved)", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        } else if (transfer.isLegacyMaterialBased()) {
            lore.add(Component.text("Transfer Type: Legacy Material", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.text("Transfer Type: Mixed", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        }
        
        lore.add(Component.empty());
        lore.add(Component.text("Click to claim to inventory", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        
        return ItemBuilder.from(item).asGuiItem(event -> {
            event.setCancelled(true);
            
            // Claim to player inventory
            if (caravanManager.claimTransferToInventory(transfer.getId(), player)) {
                player.sendMessage("§a✓ Transfer successfully claimed to your inventory!");
                player.closeInventory();
            } else {
                player.sendMessage("§cFailed to claim transfer to inventory. Check that you have enough space.");
            }
        });
    }
    
    
    private String formatTime(long timeMillis) {
        long seconds = TimeUnit.MILLISECONDS.toSeconds(timeMillis);
        if (seconds <= 0) {
            return "Ready";
        } else if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            return (seconds / 60) + "m " + (seconds % 60) + "s";
        } else {
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            return hours + "h " + minutes + "m";
        }
    }
    
    private String getItemDisplayName(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return "Air";
        }
        
        // Try to get custom display name first
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            Component displayName = item.getItemMeta().displayName();
            if (displayName != null) {
                return displayName.toString();
            }
        }
        
        // Fall back to material name, formatted nicely
        String materialName = item.getType().name();
        String[] words = materialName.toLowerCase().replace('_', ' ').split(" ");
        StringBuilder result = new StringBuilder();
        
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1))
                      .append(" ");
            }
        }
        
        return result.toString().trim();
    }
}
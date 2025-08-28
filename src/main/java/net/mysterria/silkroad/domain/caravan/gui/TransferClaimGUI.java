package net.mysterria.silkroad.domain.caravan.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.mysterria.silkroad.SilkRoad;
import net.mysterria.silkroad.domain.caravan.manager.CaravanManager;
import net.mysterria.silkroad.domain.caravan.model.Caravan;
import net.mysterria.silkroad.domain.caravan.model.ResourceTransfer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
                .title(Component.text("Awaiting Transfers", NamedTextColor.GOLD))
                .rows(rows)
                .disableAllInteractions()
                .create();
        
        if (deliveredTransfers.isEmpty()) {
            // Show empty message
            ItemStack emptyItem = new ItemStack(Material.BARRIER);
            ItemMeta emptyMeta = emptyItem.getItemMeta();
            emptyMeta.displayName(Component.text("No transfers available", NamedTextColor.RED));
            emptyMeta.lore(List.of(
                    Component.text("You have no delivered transfers", NamedTextColor.GRAY),
                    Component.text("to claim at the moment.", NamedTextColor.GRAY)
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
        closeMeta.displayName(Component.text("Close", NamedTextColor.RED));
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
                NamedTextColor.YELLOW));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Status: Delivered", NamedTextColor.GREEN));
        lore.add(Component.text("Distance: " + String.format("%.1f", transfer.getDistance()) + " blocks", 
                NamedTextColor.GRAY));
        lore.add(Component.text("Cost: " + transfer.getCost() + " shards", NamedTextColor.GRAY));
        lore.add(Component.empty());
        
        // Show items being transferred using unified method
        lore.add(Component.text("Items:", NamedTextColor.YELLOW));
        
        List<ItemStack> allItems = transfer.getAllAsItemStacks();
        if (allItems.isEmpty()) {
            lore.add(Component.text("  • No items", NamedTextColor.GRAY));
        } else {
            // Group similar items together for cleaner display
            Map<String, Integer> itemCounts = new HashMap<>();
            for (ItemStack transferItem : allItems) {
                String displayName = getItemDisplayName(transferItem);
                itemCounts.merge(displayName, transferItem.getAmount(), Integer::sum);
            }
            
            itemCounts.forEach((displayName, amount) -> {
                lore.add(Component.text("  • " + displayName + " x" + amount, NamedTextColor.WHITE));
            });
        }
        
        // Add transfer type indicator
        lore.add(Component.empty());
        if (transfer.isItemStackBased()) {
            lore.add(Component.text("Transfer Type: ItemStack (NBT preserved)", NamedTextColor.GREEN));
        } else if (transfer.isLegacyMaterialBased()) {
            lore.add(Component.text("Transfer Type: Legacy Material", NamedTextColor.YELLOW));
        } else {
            lore.add(Component.text("Transfer Type: Mixed", NamedTextColor.AQUA));
        }
        
        lore.add(Component.empty());
        lore.add(Component.text("Left-click: Claim to inventory", NamedTextColor.GREEN));
        lore.add(Component.text("Right-click: Claim to caravan", NamedTextColor.AQUA));
        lore.add(Component.text("(Select destination caravan)", NamedTextColor.GRAY));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        
        return ItemBuilder.from(item).asGuiItem(event -> {
            event.setCancelled(true);
            
            if (event.isLeftClick()) {
                // Claim to player inventory
                if (caravanManager.claimTransferToInventory(transfer.getId(), player)) {
                    player.sendMessage("§a✓ Transfer successfully claimed to your inventory!");
                    player.closeInventory();
                } else {
                    player.sendMessage("§cFailed to claim transfer to inventory. Check that you have enough space.");
                }
            } else if (event.isRightClick()) {
                // Open caravan selection GUI for claiming to caravan
                openCaravanSelectionGUI(transfer);
            }
        });
    }
    
    private void openCaravanSelectionGUI(ResourceTransfer transfer) {
        List<Caravan> playerCaravans = caravanManager.getPlayerCaravans(player.getUniqueId());
        
        if (playerCaravans.isEmpty()) {
            player.sendMessage("§cYou don't have access to any caravans to claim this transfer.");
            return;
        }
        
        int rows = Math.max(1, Math.min(6, (playerCaravans.size() / 9) + 1));
        
        Gui gui = Gui.gui()
                .title(Component.text("Select Destination Caravan", NamedTextColor.GOLD))
                .rows(rows)
                .disableAllInteractions()
                .create();
        
        for (int i = 0; i < Math.min(playerCaravans.size(), 45); i++) {
            Caravan caravan = playerCaravans.get(i);
            gui.setItem(i, createCaravanSelectionItem(caravan, transfer));
        }
        
        // Add back button
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.displayName(Component.text("Back", NamedTextColor.YELLOW));
        backItem.setItemMeta(backMeta);
        
        gui.setItem(rows * 9 - 1, ItemBuilder.from(backItem).asGuiItem(event -> {
            event.setCancelled(true);
            open(); // Return to transfers list
        }));
        
        gui.open(player);
    }
    
    private GuiItem createCaravanSelectionItem(Caravan caravan, ResourceTransfer transfer) {
        ItemStack item = new ItemStack(Material.MINECART);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text(caravan.getName(), NamedTextColor.YELLOW));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("ID: " + caravan.getId(), NamedTextColor.GRAY));
        lore.add(Component.text("Location: " + formatLocation(caravan), NamedTextColor.GRAY));
        lore.add(Component.text("Inventory: " + caravan.getItemInventory().size() + "/" + 
                Caravan.MAX_INVENTORY_SLOTS + " slots", NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(Component.text("Click to claim transfer here", NamedTextColor.GREEN));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        
        return ItemBuilder.from(item).asGuiItem(event -> {
            event.setCancelled(true);
            
            if (caravanManager.claimTransferToCaravan(transfer.getId(), caravan.getId(), player)) {
                player.sendMessage("§a✓ Transfer successfully claimed to caravan " + caravan.getName() + "!");
                player.closeInventory();
            } else {
                player.sendMessage("§cFailed to claim transfer to caravan. Check caravan space and permissions.");
            }
        });
    }
    
    private String formatLocation(Caravan caravan) {
        return String.format("%s (%.0f, %.0f, %.0f)", 
                caravan.getLocation().getWorld().getName(),
                caravan.getLocation().getX(),
                caravan.getLocation().getY(),
                caravan.getLocation().getZ());
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
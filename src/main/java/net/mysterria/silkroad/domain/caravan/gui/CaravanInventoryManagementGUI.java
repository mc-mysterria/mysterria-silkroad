package net.mysterria.silkroad.domain.caravan.gui;

import dev.triumphteam.gui.builder.item.PaperItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.mysterria.silkroad.domain.caravan.manager.CaravanManager;
import net.mysterria.silkroad.domain.caravan.model.Caravan;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class CaravanInventoryManagementGUI {
    
    private final CaravanManager caravanManager;
    private final Player player;
    private final Caravan caravan;
    private boolean showPlayerInventory = true;
    
    public CaravanInventoryManagementGUI(CaravanManager caravanManager, Player player, Caravan caravan) {
        this.caravanManager = caravanManager;
        this.player = player;
        this.caravan = caravan;
    }
    
    public void open() {
        Gui gui = Gui.gui()
                .title(text("§6" + caravan.getName() + " - Inventory Management"))
                .rows(6)
                .create();
        
        // Only set drag protection - no default click action that interferes with GuiItems
        gui.setDragAction(event -> event.setCancelled(true));
        
        setupInventoryGUI(gui);
        gui.open(player);
    }
    
    private void setupInventoryGUI(Gui gui) {
        // Clear all items to ensure clean state
        for (int i = 0; i < 54; i++) {
            gui.removeItem(i);
        }
        
        // Set drag protection first
        gui.setDragAction(event -> event.setCancelled(true));
        
        // Set up inventory content
        if (showPlayerInventory) {
            setupPlayerInventoryView(gui);
        } else {
            setupCaravanInventoryView(gui);
        }
        
        // Always set navigation items after inventory view is set up
        setupNavigationItems(gui);
        
        // Update the GUI to refresh all items
        gui.update();
    }
    
    private void setupNavigationItems(Gui gui) {
        // Toggle button
        GuiItem toggleItem = PaperItemBuilder.from(showPlayerInventory ? Material.CHEST : Material.ENDER_CHEST)
                .name(text("§eToggle View"))
                .lore(text("§7Currently showing: " + (showPlayerInventory ? "§aPlayer Inventory" : "§dCaravan Inventory")),
                      text("§7Click to switch to " + (showPlayerInventory ? "§dCaravan Inventory" : "§aPlayer Inventory")))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    showPlayerInventory = !showPlayerInventory;
                    setupInventoryGUI(gui);
                });
        gui.setItem(4, toggleItem);
        
        // Control buttons
        GuiItem helpItem = PaperItemBuilder.from(Material.BOOK)
                .name(text("§bHow to Use"))
                .lore(text("§7Player Inventory View:"),
                      text("§7- Left-click item: Deposit 1"),
                      text("§7- Shift+Left-click: Deposit all"),
                      text("§7- Right-click: Deposit half"),
                      text(""),
                      text("§7Caravan Inventory View:"),
                      text("§7- Left-click item: Withdraw 1"),
                      text("§7- Shift+Left-click: Withdraw all"),
                      text("§7- Right-click: Withdraw half"))
                .asGuiItem(event -> event.setCancelled(true));
        gui.setItem(49, helpItem);
        
        GuiItem backItem = PaperItemBuilder.from(Material.ARROW)
                .name(text("§7← Back"))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    CaravanManagementGUI managementGUI = new CaravanManagementGUI(caravanManager, player);
                    managementGUI.open();
                });
        gui.setItem(45, backItem);
        
        GuiItem closeItem = PaperItemBuilder.from(Material.BARRIER)
                .name(text("§cClose"))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    player.closeInventory();
                });
        gui.setItem(53, closeItem);
    }
    
    private void setupPlayerInventoryView(Gui gui) {
        // Display player inventory items that can be deposited
        // Get fresh inventory state to ensure we show current items after deposits
        int itemCount = 0;
        
        // Get the current player inventory contents (fresh state)
        ItemStack[] currentContents = player.getInventory().getContents();
        
        // Available slots: rows 2-4, columns 1-7 (excluding navigation areas)
        int[] availableSlots = {
                9, 10, 11, 12, 13, 14, 15, 16, 17,  // Row 2
                18, 19, 20, 21, 22, 23, 24, 25, 26, // Row 3
                27, 28, 29, 30, 31, 32, 33, 34, 35,  // Row 4
                36, 37, 38, 39, 40, 41, 42, 43, 44 // Row 5
        };
        
        int slotIndex = 0;
        for (ItemStack item : currentContents) {
            if (item != null && item.getType() != Material.AIR) {
                if (slotIndex >= availableSlots.length) break;
                
                // Create a final reference for lambda
                final ItemStack currentItem = item;
                
                GuiItem guiItem = PaperItemBuilder.from(item)
                        .asGuiItem(event -> {
                            event.setCancelled(true);
                            handlePlayerItemStackClick(currentItem, event.getClick(), gui);
                        });
                
                gui.setItem(availableSlots[slotIndex], guiItem);
                slotIndex++;
                itemCount++;
            }
        }
        
        if (itemCount == 0) {
            GuiItem emptyItem = PaperItemBuilder.from(Material.BARRIER)
                    .name(text("§7No Items"))
                    .lore(text("§7Your inventory is empty or contains no items"))
                    .asGuiItem(event -> event.setCancelled(true));
            gui.setItem(22, emptyItem);
        }
        
        // Info header with caravan inventory status
        var optionalCaravan = caravanManager.getCaravan(caravan.getId());
        Caravan currentCaravan = optionalCaravan.orElse(caravan);
        int usedSlots = currentCaravan.getItemInventory().size();
        int maxSlots = Caravan.MAX_INVENTORY_SLOTS;
        
        GuiItem infoItem = PaperItemBuilder.from(Material.CHEST)
                .name(text("§aPlayer Inventory"))
                .lore(text("§7Click items to deposit them into the caravan"),
                      text("§7Caravan slots: §e" + usedSlots + "/" + maxSlots + (usedSlots >= maxSlots ? " §c(FULL)" : "")))
                .asGuiItem(event -> event.setCancelled(true));
        gui.setItem(0, infoItem);
    }
    
    private void setupCaravanInventoryView(Gui gui) {
        // Display caravan inventory items that can be withdrawn
        // Get the latest caravan state to ensure we show current inventory
        var optionalCaravan = caravanManager.getCaravan(caravan.getId());
        Caravan currentCaravan = optionalCaravan.orElse(caravan);
        
        
        // Show ItemStack inventory (with NBT) instead of Material-based inventory
        // Available slots: rows 2-4, columns 1-7 (excluding navigation areas)
        int[] availableSlots = {
            9, 10, 11, 12, 13, 14, 15, 16, 17,  // Row 2
            18, 19, 20, 21, 22, 23, 24, 25, 26, // Row 3  
            27, 28, 29, 30, 31, 32, 33, 34, 35,  // Row 4
            36, 37, 38, 39, 40, 41, 42, 43, 44 // Row 5
        };
        
        int slotIndex = 0;
        for (ItemStack itemStack : currentCaravan.getItemInventory()) {
            if (slotIndex >= availableSlots.length) break;
            
            GuiItem item = PaperItemBuilder.from(itemStack)
                    .asGuiItem(event -> {
                        event.setCancelled(true);
                        if (event.getClick() == org.bukkit.event.inventory.ClickType.SHIFT_LEFT && 
                            event.getClickedInventory() != null && 
                            event.getClickedInventory().equals(player.getInventory())) {
                            return; // Prevent shift-clicking items from player inventory into GUI
                        }
                        handleCaravanItemStackClick(itemStack, event.getClick(), gui);
                    });
            
            gui.setItem(availableSlots[slotIndex], item);
            slotIndex++;
        }
        
        if (currentCaravan.getItemInventory().isEmpty()) {
            GuiItem emptyItem = PaperItemBuilder.from(Material.BARRIER)
                    .name(text("§7No Items"))
                    .lore(text("§7Caravan inventory is empty"))
                    .asGuiItem(event -> event.setCancelled(true));
            gui.setItem(22, emptyItem);
        }
        
        // Info header
        GuiItem infoItem = PaperItemBuilder.from(Material.ENDER_CHEST)
                .name(text("§dCaravan Inventory"))
                .lore(text("§7Click items to withdraw them to your inventory"))
                .asGuiItem(event -> event.setCancelled(true));
        gui.setItem(0, infoItem);
    }
    
    private void handlePlayerItemStackClick(ItemStack itemStack, 
                                          org.bukkit.event.inventory.ClickType clickType, Gui gui) {
        int amount = 0;
        int available = itemStack.getAmount();
        
        switch (clickType) {
            case LEFT:
                amount = 1;
                break;
            case SHIFT_LEFT:
                amount = available;
                break;
            case RIGHT:
                amount = Math.max(1, available / 2);
                break;
            default:
                return;
        }
        
        amount = Math.min(amount, available);
        
        // Create a copy of the itemStack with the desired amount
        ItemStack toDeposit = itemStack.clone();
        toDeposit.setAmount(amount);
        
        if (caravanManager.addItemStackToCaravan(caravan.getId(), player, toDeposit)) {
            String itemName = itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName() 
                    ? itemStack.getItemMeta().getDisplayName() 
                    : itemStack.getType().name().toLowerCase().replace('_', ' ');
            player.sendMessage("§aDeposited §e" + amount + " §f" + itemName + " §ainto caravan.");
            // Refresh GUI with slight delay to allow inventory sync
            org.bukkit.Bukkit.getScheduler().runTaskLater(
                net.mysterria.silkroad.SilkRoad.getInstance(), 
                () -> setupInventoryGUI(gui), 
                1L
            );
        } else {
            // Check if it's a full inventory issue
            var optionalCaravan = caravanManager.getCaravan(caravan.getId());
            if (optionalCaravan.isPresent()) {
                Caravan currentCaravan = optionalCaravan.get();
                if (!currentCaravan.canAddItemStack(toDeposit)) {
                    player.sendMessage("§cCaravan inventory is full! (§e" + currentCaravan.getItemInventory().size() + "/" + net.mysterria.silkroad.domain.caravan.model.Caravan.MAX_INVENTORY_SLOTS + " slots used§c)");
                } else {
                    player.sendMessage("§cFailed to deposit items. Check that you have enough items in your inventory.");
                }
            } else {
                player.sendMessage("§cFailed to deposit items. Check that you have enough items in your inventory.");
            }
        }
    }
    
    private void handleCaravanItemStackClick(ItemStack itemStack, 
                                           org.bukkit.event.inventory.ClickType clickType, Gui gui) {
        int amount = 0;
        int available = itemStack.getAmount();
        
        switch (clickType) {
            case LEFT:
                amount = 1;
                break;
            case SHIFT_LEFT:
                amount = available;
                break;
            case RIGHT:
                amount = Math.max(1, available / 2);
                break;
            default:
                return;
        }
        
        amount = Math.min(amount, available);
        
        if (caravanManager.removeItemStackFromCaravan(caravan.getId(), player, itemStack, amount)) {
            String itemName = itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName() 
                    ? itemStack.getItemMeta().getDisplayName() 
                    : itemStack.getType().name().toLowerCase().replace('_', ' ');
            player.sendMessage("§aWithdrew §e" + amount + " §f" + itemName + " §afrom caravan.");
            // Refresh GUI with slight delay to allow inventory sync
            org.bukkit.Bukkit.getScheduler().runTaskLater(
                net.mysterria.silkroad.SilkRoad.getInstance(), 
                () -> setupInventoryGUI(gui), 
                1L
            );
        } else {
            player.sendMessage("§cFailed to withdraw items. Check that you have enough inventory space.");
        }
    }
    
    
    private Component text(String legacyText) {
        return LegacyComponentSerializer.legacySection().deserialize(legacyText);
    }
}
package net.mysterria.silkroad.domain.caravan.gui;

import dev.triumphteam.gui.builder.item.PaperItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.mysterria.silkroad.domain.caravan.manager.CaravanManager;
import net.mysterria.silkroad.domain.caravan.model.Caravan;
import net.mysterria.silkroad.utils.TranslationUtil;
import net.kyori.adventure.text.format.NamedTextColor;
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
    private boolean isProcessing = false;
    
    public CaravanInventoryManagementGUI(CaravanManager caravanManager, Player player, Caravan caravan) {
        this.caravanManager = caravanManager;
        this.player = player;
        this.caravan = caravan;
    }
    
    public void open() {
        Gui gui = Gui.gui()
                .title(TranslationUtil.translatable("gui.inventory.management.title", NamedTextColor.GOLD, caravan.getName()))
                .rows(6)
                .create();

        // Set drag protection
        gui.setDragAction(event -> event.setCancelled(true));

        // Prevent shift-clicking from player's actual inventory into the GUI
        gui.setDefaultClickAction(event -> {
            // Cancel all clicks that are not on GuiItems (prevents shift-clicking from player inventory)
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(player.getInventory())) {
                event.setCancelled(true);
            }
        });

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
                .name(TranslationUtil.translatable("gui.toggle.view").color(NamedTextColor.YELLOW))
                .lore(TranslationUtil.translatable("gui.currently.showing", (showPlayerInventory ? TranslationUtil.translate("gui.player.inventory") : TranslationUtil.translate("gui.caravan.inventory"))).color(NamedTextColor.GRAY),
                      TranslationUtil.translatable("gui.click.switch", (showPlayerInventory ? TranslationUtil.translate("gui.caravan.inventory") : TranslationUtil.translate("gui.player.inventory"))).color(NamedTextColor.GRAY))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    showPlayerInventory = !showPlayerInventory;
                    setupInventoryGUI(gui);
                });
        gui.setItem(4, toggleItem);
        
        // Control buttons
        GuiItem helpItem = PaperItemBuilder.from(Material.BOOK)
                .name(TranslationUtil.translatable("gui.how.to.use").color(NamedTextColor.AQUA))
                .lore(TranslationUtil.translatable("gui.player.view.instructions").color(NamedTextColor.GRAY),
                      TranslationUtil.translatable("gui.player.view.left.click").color(NamedTextColor.GRAY),
                      TranslationUtil.translatable("gui.player.view.shift.click").color(NamedTextColor.GRAY),
                      TranslationUtil.translatable("gui.player.view.right.click").color(NamedTextColor.GRAY),
                      Component.empty(),
                      TranslationUtil.translatable("gui.caravan.view.instructions").color(NamedTextColor.GRAY),
                      TranslationUtil.translatable("gui.caravan.view.left.click").color(NamedTextColor.GRAY),
                      TranslationUtil.translatable("gui.caravan.view.shift.click").color(NamedTextColor.GRAY),
                      TranslationUtil.translatable("gui.caravan.view.right.click").color(NamedTextColor.GRAY))
                .asGuiItem(event -> event.setCancelled(true));
        gui.setItem(49, helpItem);
        
        GuiItem backItem = PaperItemBuilder.from(Material.ARROW)
                .name(TranslationUtil.translatable("gui.back.arrow").color(NamedTextColor.GRAY))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    CaravanManagementGUI managementGUI = new CaravanManagementGUI(caravanManager, player);
                    managementGUI.open();
                });
        gui.setItem(45, backItem);
        
        GuiItem closeItem = PaperItemBuilder.from(Material.BARRIER)
                .name(TranslationUtil.translatable("gui.close").color(NamedTextColor.RED))
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
                    .name(TranslationUtil.translatable("gui.no.items").color(NamedTextColor.GRAY))
                    .lore(TranslationUtil.translatable("gui.inventory.empty").color(NamedTextColor.GRAY))
                    .asGuiItem(event -> event.setCancelled(true));
            gui.setItem(22, emptyItem);
        }
        
        // Info header with caravan inventory status
        var optionalCaravan = caravanManager.getCaravan(caravan.getId());
        Caravan currentCaravan = optionalCaravan.orElse(caravan);
        int usedSlots = currentCaravan.getItemInventory().size();
        int maxSlots = Caravan.MAX_INVENTORY_SLOTS;
        
        GuiItem infoItem = PaperItemBuilder.from(Material.CHEST)
                .name(TranslationUtil.translatable("gui.player.inventory.label").color(NamedTextColor.GREEN))
                .lore(TranslationUtil.translatable("gui.deposit.instruction").color(NamedTextColor.GRAY),
                      TranslationUtil.translatable("gui.slots.usage", String.valueOf(usedSlots), String.valueOf(maxSlots), (usedSlots >= maxSlots ? TranslationUtil.translate("gui.slots.full") : "")).color(NamedTextColor.GRAY))
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
                    .name(TranslationUtil.translatable("gui.no.items").color(NamedTextColor.GRAY))
                    .lore(TranslationUtil.translatable("gui.caravan.empty").color(NamedTextColor.GRAY))
                    .asGuiItem(event -> event.setCancelled(true));
            gui.setItem(22, emptyItem);
        }
        
        // Info header
        GuiItem infoItem = PaperItemBuilder.from(Material.ENDER_CHEST)
                .name(TranslationUtil.translatable("gui.caravan.inventory.label").color(NamedTextColor.LIGHT_PURPLE))
                .lore(TranslationUtil.translatable("gui.withdraw.instruction").color(NamedTextColor.GRAY))
                .asGuiItem(event -> event.setCancelled(true));
        gui.setItem(0, infoItem);
    }
    
    private void handlePlayerItemStackClick(ItemStack itemStack,
                                          org.bukkit.event.inventory.ClickType clickType, Gui gui) {
        // Prevent rapid clicks while processing
        if (isProcessing) {
            return;
        }

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

        // Set processing flag to prevent rapid clicks
        isProcessing = true;

        if (caravanManager.addItemStackToCaravan(caravan.getId(), player, toDeposit)) {
            String itemName = itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName()
                    ? itemStack.getItemMeta().getDisplayName()
                    : itemStack.getType().name().toLowerCase().replace('_', ' ');
            player.sendMessage(TranslationUtil.translate("inventory.deposited", String.valueOf(amount), itemName));
            // Refresh GUI with slight delay to allow inventory sync
            org.bukkit.Bukkit.getScheduler().runTaskLater(
                net.mysterria.silkroad.SilkRoad.getInstance(),
                () -> {
                    setupInventoryGUI(gui);
                    isProcessing = false;
                },
                1L
            );
        } else {
            // Reset processing flag on failure
            isProcessing = false;
            // Check if it's a full inventory issue
            var optionalCaravan = caravanManager.getCaravan(caravan.getId());
            if (optionalCaravan.isPresent()) {
                Caravan currentCaravan = optionalCaravan.get();
                if (!currentCaravan.canAddItemStack(toDeposit)) {
                    player.sendMessage(TranslationUtil.translate("inventory.full", String.valueOf(currentCaravan.getItemInventory().size()), String.valueOf(net.mysterria.silkroad.domain.caravan.model.Caravan.MAX_INVENTORY_SLOTS)));
                } else {
                    player.sendMessage(TranslationUtil.translate("inventory.deposit.failed"));
                }
            } else {
                player.sendMessage(TranslationUtil.translate("inventory.deposit.failed"));
            }
        }
    }
    
    private void handleCaravanItemStackClick(ItemStack itemStack,
                                           org.bukkit.event.inventory.ClickType clickType, Gui gui) {
        // Prevent rapid clicks while processing
        if (isProcessing) {
            return;
        }

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

        // Set processing flag to prevent rapid clicks
        isProcessing = true;

        if (caravanManager.removeItemStackFromCaravan(caravan.getId(), player, itemStack, amount)) {
            String itemName = itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName()
                    ? itemStack.getItemMeta().getDisplayName()
                    : itemStack.getType().name().toLowerCase().replace('_', ' ');
            player.sendMessage(TranslationUtil.translate("inventory.withdrew", String.valueOf(amount), itemName));
            // Refresh GUI with slight delay to allow inventory sync
            org.bukkit.Bukkit.getScheduler().runTaskLater(
                net.mysterria.silkroad.SilkRoad.getInstance(),
                () -> {
                    setupInventoryGUI(gui);
                    isProcessing = false;
                },
                1L
            );
        } else {
            // Reset processing flag on failure
            isProcessing = false;
            player.sendMessage(TranslationUtil.translate("inventory.withdraw.failed"));
        }
    }
    
    
    private Component text(String legacyText) {
        return LegacyComponentSerializer.legacySection().deserialize(legacyText);
    }
}
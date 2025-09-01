package net.mysterria.silkroad.domain.caravan.gui;

import dev.triumphteam.gui.builder.item.PaperItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.mysterria.silkroad.SilkRoad;
import net.mysterria.silkroad.config.SilkRoadConfig;
import net.mysterria.silkroad.domain.caravan.manager.CaravanManager;
import net.mysterria.silkroad.domain.caravan.model.Caravan;
import net.mysterria.silkroad.domain.caravan.model.ResourceTransfer;
import net.mysterria.silkroad.utils.TranslationUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResourceTransferGUI {
    
    private final CaravanManager caravanManager;
    private final Player player;
    private final Caravan sourceCaravan;
    private final Caravan destinationCaravan;
    private final List<ItemStack> selectedItems = new ArrayList<>();
    private final Map<ItemStack, Integer> selectionAmounts = new HashMap<>();
    private boolean showingConfirmation = false;
    
    public ResourceTransferGUI(CaravanManager caravanManager, Player player, 
                               Caravan sourceCaravan, Caravan destinationCaravan) {
        this.caravanManager = caravanManager;
        this.player = player;
        this.sourceCaravan = sourceCaravan;
        this.destinationCaravan = destinationCaravan;
        
        // Automatically select all inventory items
        autoSelectAllItems();
    }
    
    public void open() {
        if (sourceCaravan.getItemInventory().isEmpty()) {
            player.sendMessage(TranslationUtil.translate("resource.no.items"));
            return;
        }
        
        // Show confirmation preview with all items auto-selected
        showConfirmationPreview();
    }
    
    private void setupResourceSelection(Gui gui) {
        int slot = 0;
        
        // Use ItemStack inventory instead of Material inventory
        for (ItemStack itemStack : sourceCaravan.getItemInventory()) {
            if (slot >= 36) break;
            if (itemStack == null || itemStack.getType() == Material.AIR) continue;
            
            int available = itemStack.getAmount();
            int selected = getSelectedAmount(itemStack);
            String itemName = getItemDisplayName(itemStack);
            
            // Create display item with visual selection indicator
            ItemStack displayItem = itemStack.clone();
            displayItem.setAmount(Math.min(available, 64));
            
            List<Component> lore = new ArrayList<>();
            lore.add(TranslationUtil.translatable("gui.available.amount", String.valueOf(available)).color(NamedTextColor.GRAY));
            lore.add(TranslationUtil.translatable("gui.selected.amount", String.valueOf(selected > 0 ? selected : 0)).color(NamedTextColor.GRAY));
            
            // Show NBT preservation info
            if (itemStack.hasItemMeta()) {
                lore.add(Component.empty());
                lore.add(TranslationUtil.translatable("gui.nbt.preserved").color(NamedTextColor.GREEN));
                if (itemStack.getItemMeta().hasDisplayName()) {
                    lore.add(TranslationUtil.translatable("gui.nbt.custom.name").color(NamedTextColor.AQUA));
                }
                if (itemStack.getItemMeta().hasLore()) {
                    lore.add(TranslationUtil.translatable("gui.nbt.custom.lore").color(NamedTextColor.AQUA));
                }
                if (itemStack.getItemMeta().hasEnchants()) {
                    lore.add(TranslationUtil.translatable("gui.nbt.enchantments").color(NamedTextColor.LIGHT_PURPLE));
                }
            }
            
            lore.add(Component.empty());
            lore.add(TranslationUtil.translatable("gui.click.instructions.left").color(NamedTextColor.YELLOW));
            lore.add(TranslationUtil.translatable("gui.click.instructions.shift.left").color(NamedTextColor.YELLOW));
            lore.add(TranslationUtil.translatable("gui.click.instructions.right").color(NamedTextColor.YELLOW));
            lore.add(TranslationUtil.translatable("gui.click.instructions.shift.right").color(NamedTextColor.YELLOW));
            lore.add(TranslationUtil.translatable("gui.click.instructions.middle").color(NamedTextColor.YELLOW));
            
            GuiItem item = PaperItemBuilder.from(displayItem)
                    .name(Component.text(itemName).color(selected > 0 ? NamedTextColor.GREEN : NamedTextColor.WHITE))
                    .lore(lore.toArray(Component[]::new))
                    .asGuiItem(event -> {
                        event.setCancelled(true);
                        handleItemClick(itemStack, available, event.getClick());
                        setupResourceSelection(gui);
                    });
            
            gui.setItem(slot, item);
            slot++;
        }
        
        if (sourceCaravan.getItemInventory().isEmpty()) {
            GuiItem emptyItem = PaperItemBuilder.from(Material.BARRIER)
                    .name(TranslationUtil.translatable("gui.no.items.title").color(NamedTextColor.GRAY))
                    .lore(TranslationUtil.translatable("gui.no.items.description").color(NamedTextColor.GRAY))
                    .asGuiItem(event -> event.setCancelled(true));
            gui.setItem(22, emptyItem);
        }
        
        GuiItem infoItem = PaperItemBuilder.from(Material.PAPER)
                .name(TranslationUtil.translatable("gui.transfer.information.title").color(NamedTextColor.GREEN))
                .lore(createTransferInfo().toArray(Component[]::new))
                .asGuiItem(event -> event.setCancelled(true));
        gui.setItem(40, infoItem);
        
        GuiItem confirmItem = PaperItemBuilder.from(Material.GREEN_WOOL)
                .name(TranslationUtil.translatable("gui.transfer.confirm.title").color(NamedTextColor.GREEN))
                .lore(TranslationUtil.translatable("gui.transfer.confirm.click").color(NamedTextColor.GRAY))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    executeTransfer();
                });
        gui.setItem(42, confirmItem);
        
        GuiItem clearItem = PaperItemBuilder.from(Material.YELLOW_WOOL)
                .name(TranslationUtil.translatable("gui.clear.selection.title").color(NamedTextColor.YELLOW))
                .lore(TranslationUtil.translatable("gui.clear.selection.click").color(NamedTextColor.GRAY))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    selectedItems.clear();
                    selectionAmounts.clear();
                    setupResourceSelection(gui);
                });
        gui.setItem(44, clearItem);
        
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
        gui.setItem(49, closeItem);
    }
    
    private void handleItemClick(ItemStack itemStack, int available, org.bukkit.event.inventory.ClickType clickType) {
        int current = getSelectedAmount(itemStack);
        int change = 0;
        
        switch (clickType) {
            case LEFT:
                change = 1;
                break;
            case SHIFT_LEFT:
                change = 10;
                break;
            case RIGHT:
                change = -1;
                break;
            case SHIFT_RIGHT:
                change = -10;
                break;
            case MIDDLE:
                setSelectedAmount(itemStack, available);
                return;
            default:
                return;
        }
        
        int newAmount = Math.max(0, Math.min(available, current + change));
        
        if (newAmount == 0) {
            removeFromSelection(itemStack);
        } else {
            setSelectedAmount(itemStack, newAmount);
        }
    }
    
    private int getSelectedAmount(ItemStack itemStack) {
        ItemStack existing = findSimilarInSelection(itemStack);
        return existing != null ? selectionAmounts.get(existing) : 0;
    }
    
    private void setSelectedAmount(ItemStack itemStack, int amount) {
        ItemStack existing = findSimilarInSelection(itemStack);
        if (existing != null) {
            selectionAmounts.put(existing, amount);
        } else {
            ItemStack selectedItem = itemStack.clone();
            selectedItem.setAmount(amount);
            selectedItems.add(selectedItem);
            selectionAmounts.put(selectedItem, amount);
        }
    }
    
    private void removeFromSelection(ItemStack itemStack) {
        ItemStack existing = findSimilarInSelection(itemStack);
        if (existing != null) {
            selectedItems.remove(existing);
            selectionAmounts.remove(existing);
        }
    }
    
    private ItemStack findSimilarInSelection(ItemStack itemStack) {
        for (ItemStack selected : selectedItems) {
            if (selected.isSimilar(itemStack)) {
                return selected;
            }
        }
        return null;
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
    
    private List<Component> createTransferInfo() {
        List<Component> info = new ArrayList<>();
        info.add(TranslationUtil.translatable("gui.transfer.from.caravan", sourceCaravan.getName()).color(NamedTextColor.GRAY));
        info.add(TranslationUtil.translatable("gui.transfer.to.caravan", destinationCaravan.getName()).color(NamedTextColor.GRAY));
        
        double distance = sourceCaravan.distanceTo(destinationCaravan);
        if (distance == Double.MAX_VALUE) {
            info.add(TranslationUtil.translatable("gui.transfer.distance.different.world").color(NamedTextColor.GRAY));
        } else {
            info.add(TranslationUtil.translatable("gui.transfer.distance.blocks.format", String.format("%.1f", distance)).color(NamedTextColor.GRAY));
        }
        
        if (!selectedItems.isEmpty()) {
            int totalItems = selectionAmounts.values().stream().mapToInt(Integer::intValue).sum();
            int estimatedCost = calculateItemTransferCost(distance, selectedItems);
            long estimatedTime = calculateDeliveryTime(distance);
            
            info.add(TranslationUtil.translatable("gui.transfer.items.count", String.valueOf(totalItems)).color(NamedTextColor.GRAY));
            info.add(TranslationUtil.translatable("gui.transfer.estimated.cost", String.valueOf(estimatedCost)).color(NamedTextColor.GRAY));
            info.add(TranslationUtil.translatable("gui.transfer.estimated.time", formatTime(estimatedTime)).color(NamedTextColor.GRAY));
            info.add(TranslationUtil.translatable("gui.transfer.nbt.data.preserved").color(NamedTextColor.GREEN));
            info.add(Component.empty());
            info.add(TranslationUtil.translatable("gui.transfer.selected.items.header").color(NamedTextColor.GRAY));
            for (ItemStack item : selectedItems) {
                int amount = selectionAmounts.get(item);
                String itemName = getItemDisplayName(item);
                info.add(TranslationUtil.translatable("gui.transfer.selected.item.format", String.valueOf(amount), itemName).color(NamedTextColor.WHITE));
            }
        } else {
            info.add(TranslationUtil.translatable("gui.transfer.no.items.selected").color(NamedTextColor.GRAY));
        }
        
        return info;
    }
    
    private void executeTransfer() {
        if (selectedItems.isEmpty()) {
            player.sendMessage(TranslationUtil.translatable("gui.transfer.no.selection.error").color(NamedTextColor.RED));
            return;
        }
        
        // Create the list of items to transfer with correct amounts
        List<ItemStack> itemsToTransfer = new ArrayList<>();
        for (ItemStack selectedItem : selectedItems) {
            int amount = selectionAmounts.get(selectedItem);
            ItemStack transferItem = selectedItem.clone();
            transferItem.setAmount(amount);
            itemsToTransfer.add(transferItem);
        }
        
        ResourceTransfer transfer = caravanManager.createTransfer(
                player, 
                sourceCaravan.getId(), 
                destinationCaravan.getId(), 
                itemsToTransfer
        );
        
        if (transfer != null) {
            player.sendMessage(TranslationUtil.translatable("gui.transfer.success.message").color(NamedTextColor.GREEN));
            player.sendMessage(TranslationUtil.translatable("gui.transfer.success.id", transfer.getId().substring(0, 8)).color(NamedTextColor.GRAY));
            player.sendMessage(TranslationUtil.translatable("gui.transfer.success.items", String.valueOf(itemsToTransfer.size())).color(NamedTextColor.GRAY));
            player.sendMessage(TranslationUtil.translatable("gui.transfer.success.cost", String.valueOf(transfer.getCost())).color(NamedTextColor.GRAY));
            player.sendMessage(TranslationUtil.translatable("gui.transfer.success.delivery.time", formatTime(transfer.getRemainingTime())).color(NamedTextColor.GRAY));
            player.sendMessage(TranslationUtil.translatable("gui.transfer.success.nbt.preserved").color(NamedTextColor.GREEN));
            player.closeInventory();
        } else {
            player.sendMessage(TranslationUtil.translatable("gui.transfer.failed.message").color(NamedTextColor.RED));
        }
    }
    
    private int calculateItemTransferCost(double distance, List<ItemStack> items) {
        SilkRoadConfig config = SilkRoad.getInstance().getPluginConfig();
        
        // Distance cost from config (diamonds per block)
        double distanceCost = distance * config.getDistanceCostPerBlock();
        
        // Stack cost from config (diamonds per item stack)
        int stackCount = items.size();
        double stackCost = stackCount * config.getStackCost();
        
        return (int) Math.max(config.getMinimumCost(), distanceCost + stackCost);
    }
    
    private long calculateDeliveryTime(double distance) {
        SilkRoadConfig config = SilkRoad.getInstance().getPluginConfig();
        long baseTime = config.getBaseTimeMs();
        long distanceTime = (long) (distance * config.getTimePerBlockMs());
        return baseTime + distanceTime;
    }
    
    private String formatTime(long millis) {
        if (millis <= 0) return TranslationUtil.translate("time.ready");
        
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) {
            return TranslationUtil.translate("time.format.hours.minutes", String.valueOf(hours), String.valueOf(minutes % 60));
        } else if (minutes > 0) {
            return TranslationUtil.translate("time.format.minutes.seconds", String.valueOf(minutes), String.valueOf(seconds % 60));
        } else {
            return TranslationUtil.translate("time.format.seconds", String.valueOf(seconds));
        }
    }
    
    private Component text(String legacyText) {
        return LegacyComponentSerializer.legacySection().deserialize(legacyText);
    }
    
    private void autoSelectAllItems() {
        selectedItems.clear();
        selectionAmounts.clear();
        
        // Select all items from the source caravan inventory
        for (ItemStack itemStack : sourceCaravan.getItemInventory()) {
            if (itemStack != null && itemStack.getType() != Material.AIR) {
                ItemStack selectedItem = itemStack.clone();
                selectedItems.add(selectedItem);
                selectionAmounts.put(selectedItem, itemStack.getAmount());
            }
        }
    }
    
    private void showConfirmationPreview() {
        Gui gui = Gui.gui()
                .title(text("§6Transfer Preview - Confirm Transfer"))
                .rows(6)
                .create();
        
        // Display preview of all items being transferred
        int slot = 0;
        for (ItemStack item : selectedItems) {
            if (slot >= 36) break;
            
            int amount = selectionAmounts.get(item);
            String itemName = getItemDisplayName(item);
            
            ItemStack displayItem = item.clone();
            displayItem.setAmount(Math.min(amount, 64));
            
            List<String> lore = new ArrayList<>();
            lore.add("§a✓ Selected for Transfer");
            lore.add("§7Amount: §e" + amount);
            
            // Show NBT preservation info
            if (item.hasItemMeta()) {
                lore.add("");
                lore.add("§a✓ NBT Data Will Be Preserved:");
                if (item.getItemMeta().hasDisplayName()) {
                    lore.add("  §b• Custom Name");
                }
                if (item.getItemMeta().hasLore()) {
                    lore.add("  §b• Custom Lore");
                }
                if (item.getItemMeta().hasEnchants()) {
                    lore.add("  §d• Enchantments");
                }
            }
            
            GuiItem guiItem = PaperItemBuilder.from(displayItem)
                    .name(text("§a" + itemName))
                    .lore(lore.stream().map(this::text).toArray(Component[]::new))
                    .asGuiItem(event -> event.setCancelled(true));
            
            gui.setItem(slot, guiItem);
            slot++;
        }
        
        // Transfer information panel
        GuiItem infoItem = PaperItemBuilder.from(Material.PAPER)
                .name(text("§6Transfer Details"))
                .lore(createTransferInfo().toArray(Component[]::new))
                .asGuiItem(event -> event.setCancelled(true));
        gui.setItem(40, infoItem);
        
        // Confirm transfer button
        GuiItem confirmItem = PaperItemBuilder.from(Material.LIME_CONCRETE)
                .name(text("§a§lCONFIRM TRANSFER"))
                .lore(text("§7Click to execute the transfer"),
                      text("§7All inventory will be transferred"),
                      text("§a§lNBT data will be preserved!"))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    executeTransfer();
                });
        gui.setItem(42, confirmItem);
        
        // Edit selection button (goes back to manual selection)
        GuiItem editItem = PaperItemBuilder.from(Material.YELLOW_CONCRETE)
                .name(text("§e§lEDIT SELECTION"))
                .lore(text("§7Click to manually adjust items"),
                      text("§7Opens the detailed selection GUI"))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    showDetailedSelection();
                });
        gui.setItem(44, editItem);
        
        // Back button
        GuiItem backItem = PaperItemBuilder.from(Material.ARROW)
                .name(text("§7← Back"))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    CaravanManagementGUI managementGUI = new CaravanManagementGUI(caravanManager, player);
                    managementGUI.open();
                });
        gui.setItem(45, backItem);
        
        // Close button
        GuiItem closeItem = PaperItemBuilder.from(Material.BARRIER)
                .name(text("§cClose"))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    player.closeInventory();
                });
        gui.setItem(49, closeItem);
        
        gui.open(player);
    }
    
    private void showDetailedSelection() {
        Gui gui = Gui.gui()
                .title(text("§6Edit Transfer Selection"))
                .rows(6)
                .create();
        
        setupResourceSelection(gui);
        gui.open(player);
    }
}
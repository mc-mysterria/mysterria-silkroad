package net.mysterria.silkroad.domain.caravan.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.mysterria.silkroad.domain.caravan.manager.CaravanManager;
import net.mysterria.silkroad.domain.caravan.model.Caravan;
import net.mysterria.silkroad.domain.caravan.model.ResourceTransfer;
import net.mysterria.silkroad.utils.ShardUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResourceSelectionGUI {
    
    private final CaravanManager caravanManager;
    private final Player player;
    private final Caravan sourceCaravan;
    private final Caravan destinationCaravan;
    private final PaginatedGui gui;
    private final Map<Material, Integer> selectedResources = new HashMap<>(); // Legacy
    private final List<ItemStack> selectedItemStacks = new ArrayList<>(); // New ItemStack-based selection
    
    public ResourceSelectionGUI(CaravanManager caravanManager, Player player, Caravan sourceCaravan, Caravan destinationCaravan) {
        this.caravanManager = caravanManager;
        this.player = player;
        this.sourceCaravan = sourceCaravan;
        this.destinationCaravan = destinationCaravan;
        
        this.gui = Gui.paginated()
                .title(Component.text("Select Resources to Send"))
                .rows(6)
                .create();
        
        setupNavigationItems();
        addResources();
    }
    
    private void setupNavigationItems() {
        GuiItem prevItem = ItemBuilder.from(Material.ARROW)
                .name(Component.text("Previous Page").decoration(TextDecoration.ITALIC, false))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    gui.previous();
                });
        gui.setItem(6, 2, prevItem);
        
        GuiItem nextItem = ItemBuilder.from(Material.ARROW)
                .name(Component.text("Next Page").decoration(TextDecoration.ITALIC, false))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    gui.next();
                });
        gui.setItem(6, 8, nextItem);
        
        GuiItem backItem = ItemBuilder.from(Material.BARRIER)
                .name(Component.text("Back").decoration(TextDecoration.ITALIC, false))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    new CaravanSelectionGUI(caravanManager, player, sourceCaravan).open();
                });
        gui.setItem(6, 3, backItem);
        
        updateConfirmItem();
        updateSummaryItem();
    }
    
    private void addResources() {
        gui.clearPageItems();
        
        if (sourceCaravan.getItemInventory().isEmpty()) {
            GuiItem emptyItem = ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE)
                    .name(Component.text("No Resources Available")
                            .color(NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false))
                    .lore(Component.text("This caravan has no resources to send")
                            .decoration(TextDecoration.ITALIC, false))
                    .asGuiItem(event -> event.setCancelled(true));
            
            gui.addItem(emptyItem);
            return;
        }
        
        for (ItemStack itemStack : sourceCaravan.getItemInventory()) {
            if (itemStack == null || itemStack.getType() == Material.AIR) continue;
            
            // Find how many of this item stack are already selected
            int selected = 0;
            for (ItemStack selectedStack : selectedItemStacks) {
                if (selectedStack.isSimilar(itemStack)) {
                    selected += selectedStack.getAmount();
                }
            }
            
            String itemName = itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName() 
                    ? itemStack.getItemMeta().getDisplayName() 
                    : itemStack.getType().name().toLowerCase().replace('_', ' ');
            
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Available: " + itemStack.getAmount()).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Selected: " + selected)
                    .color(selected > 0 ? NamedTextColor.GREEN : NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text(""));
            lore.add(Component.text("Left-click: Select 1").decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Right-click: Select half").decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Shift-left: Select all").decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Shift-right: Remove from selection").decoration(TextDecoration.ITALIC, false));
            
            GuiItem item = ItemBuilder.from(itemStack.clone())
                    .name(Component.text(itemName).decoration(TextDecoration.ITALIC, false))
                    .lore(lore)
                    .asGuiItem(event -> {
                        event.setCancelled(true);
                        //handleItemStackClick(itemStack, event.isLeftClick(), event.isRightClick(), event.isShiftClick());
                    });
            
            gui.addItem(item);
        }
        
        gui.update();
    }
    
    private void handleResourceClick(Material material, int available, boolean leftClick, boolean rightClick, boolean shift) {
        int current = selectedResources.getOrDefault(material, 0);
        int change = 0;
        
        if (leftClick && !shift) {
            change = 1;
        } else if (rightClick && !shift) {
            change = 10;
        } else if (leftClick && shift) {
            change = -1;
        } else if (rightClick && shift) {
            change = -10;
        }
        
        int newAmount = Math.max(0, Math.min(available, current + change));
        
        if (newAmount > 0) {
            selectedResources.put(material, newAmount);
        } else {
            selectedResources.remove(material);
        }
        
        addResources();
        updateConfirmItem();
        updateSummaryItem();
    }
    
    private void updateConfirmItem() {
        boolean hasSelection = !selectedResources.isEmpty();
        double distance = sourceCaravan.distanceTo(destinationCaravan);
        int totalCost = calculateTotalCost(distance, selectedResources);
        long deliveryTime = calculateDeliveryTime(distance);
        
        Material material = hasSelection ? Material.EMERALD : Material.GRAY_DYE;
        Component name = hasSelection ? 
                Component.text("Confirm Transfer").color(NamedTextColor.GREEN) :
                Component.text("Select Resources First").color(NamedTextColor.GRAY);
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Destination: " + destinationCaravan.getName()).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Distance: " + String.format("%.1f blocks", distance)).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Total Cost: " + ShardUtils.formatShardCost(totalCost)).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Your Shards: " + ShardUtils.getTotalPlayerShards(player))
                .color(ShardUtils.getTotalPlayerShards(player) >= totalCost ? NamedTextColor.GREEN : NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Delivery Time: " + formatTime(deliveryTime)).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(""));
        
        if (hasSelection) {
            lore.add(Component.text("Click to confirm transfer").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.text("Select resources first").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        }
        
        GuiItem confirmItem = ItemBuilder.from(material)
                .name(name.decoration(TextDecoration.ITALIC, false))
                .lore(lore)
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    if (hasSelection) {
                        confirmTransfer();
                    }
                });
        
        gui.setItem(6, 7, confirmItem);
    }
    
    private void updateSummaryItem() {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Selected Resources:").decoration(TextDecoration.ITALIC, false));
        
        if (selectedResources.isEmpty()) {
            lore.add(Component.text("  None").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        } else {
            for (Map.Entry<Material, Integer> entry : selectedResources.entrySet()) {
                lore.add(Component.text("  " + entry.getKey().name() + ": " + entry.getValue())
                        .decoration(TextDecoration.ITALIC, false));
            }
        }
        
        GuiItem summaryItem = ItemBuilder.from(Material.PAPER)
                .name(Component.text("Transfer Summary").decoration(TextDecoration.ITALIC, false))
                .lore(lore)
                .asGuiItem(event -> event.setCancelled(true));
        
        gui.setItem(6, 5, summaryItem);
    }
    
    private void confirmTransfer() {
        if (selectedResources.isEmpty()) {
            player.sendMessage("§cNo resources selected!");
            return;
        }
        
        ResourceTransfer transfer = caravanManager.createTransfer(player, sourceCaravan.getId(), 
                destinationCaravan.getId(), new HashMap<>(selectedResources));
        
        if (transfer != null) {
            player.closeInventory();
            player.sendMessage("§aTransfer initiated! Your resources are on their way to " + destinationCaravan.getName());
            player.sendMessage("§7Estimated delivery time: " + formatTime(transfer.getRemainingTime()));
        } else {
            double distance = sourceCaravan.distanceTo(destinationCaravan);
            int totalCost = calculateTotalCost(distance, selectedResources);
            int playerShards = ShardUtils.getTotalPlayerShards(player);
            
            if (playerShards < totalCost) {
                player.sendMessage("§cNot enough shards! You need " + ShardUtils.formatShardCost(totalCost) + " but only have " + ShardUtils.formatShardCost(playerShards));
            } else {
                player.sendMessage("§cFailed to create transfer! Check if you have enough resources.");
            }
        }
    }
    
    private int calculateTotalCost(double distance, Map<Material, Integer> resources) {
        int totalItems = resources.values().stream().mapToInt(Integer::intValue).sum();
        double baseCost = distance * 0.1;
        double itemCost = totalItems * 0.5;
        return (int) Math.max(1, baseCost + itemCost);
    }
    
    private long calculateDeliveryTime(double distance) {
        double baseTime = 5 * 60 * 1000;
        double distanceTime = distance * 1000;
        return (long) (baseTime + distanceTime);
    }
    
    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }
    
    public void open() {
        gui.open(player);
    }
}
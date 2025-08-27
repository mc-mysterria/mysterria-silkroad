package net.mysterria.silkroad.domain.caravan.gui;

import dev.triumphteam.gui.builder.item.PaperItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.mysterria.silkroad.domain.caravan.manager.CaravanManager;
import net.mysterria.silkroad.domain.caravan.model.Caravan;
import net.mysterria.silkroad.domain.caravan.model.ResourceTransfer;
import net.kyori.adventure.text.Component;
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
    private final Map<Material, Integer> selectedResources = new HashMap<>();
    
    public ResourceTransferGUI(CaravanManager caravanManager, Player player, 
                               Caravan sourceCaravan, Caravan destinationCaravan) {
        this.caravanManager = caravanManager;
        this.player = player;
        this.sourceCaravan = sourceCaravan;
        this.destinationCaravan = destinationCaravan;
    }
    
    public void open() {
        Gui gui = Gui.gui()
                .title(text("§6Select Resources to Transfer"))
                .rows(6)
                .create();
        
        setupResourceSelection(gui);
        gui.open(player);
    }
    
    private void setupResourceSelection(Gui gui) {
        int slot = 0;
        for (var entry : sourceCaravan.getInventory().entrySet()) {
            if (slot >= 36) break;
            
            Material material = entry.getKey();
            int available = entry.getValue();
            int selected = selectedResources.getOrDefault(material, 0);
            
            ItemStack displayItem = new ItemStack(material, Math.min(available, 64));
            
            GuiItem item = PaperItemBuilder.from(displayItem)
                    .name(text("§f" + material.name().toLowerCase().replace('_', ' ')))
                    .lore(text("§7Available: §e" + available),
                          text("§7Selected: §a" + selected),
                          text(""),
                          text("§eLeft-click: +1"),
                          text("§eShift Left-click: +10"),
                          text("§eRight-click: -1"),
                          text("§eShift Right-click: -10"),
                          text("§eMiddle-click: Select all"))
                    .asGuiItem(event -> {
                        event.setCancelled(true);
                        handleResourceClick(material, available, event.getClick());
                        setupResourceSelection(gui);
                    });
            
            gui.setItem(slot, item);
            slot++;
        }
        
        if (sourceCaravan.getInventory().isEmpty()) {
            GuiItem emptyItem = PaperItemBuilder.from(Material.BARRIER)
                    .name(text("§7No Resources"))
                    .lore(text("§7This caravan has no resources to transfer"))
                    .asGuiItem(event -> event.setCancelled(true));
            gui.setItem(22, emptyItem);
        }
        
        GuiItem infoItem = PaperItemBuilder.from(Material.PAPER)
                .name(text("§aTransfer Information"))
                .lore(createTransferInfo().stream().map(this::text).toArray(Component[]::new))
                .asGuiItem(event -> event.setCancelled(true));
        gui.setItem(40, infoItem);
        
        GuiItem confirmItem = PaperItemBuilder.from(Material.GREEN_WOOL)
                .name(text("§aConfirm Transfer"))
                .lore(text("§7Click to execute the transfer"))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    executeTransfer();
                });
        gui.setItem(42, confirmItem);
        
        GuiItem clearItem = PaperItemBuilder.from(Material.YELLOW_WOOL)
                .name(text("§eClear Selection"))
                .lore(text("§7Click to clear all selected resources"))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    selectedResources.clear();
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
    
    private void handleResourceClick(Material material, int available, org.bukkit.event.inventory.ClickType clickType) {
        int current = selectedResources.getOrDefault(material, 0);
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
                selectedResources.put(material, available);
                return;
            default:
                return;
        }
        
        int newAmount = Math.max(0, Math.min(available, current + change));
        
        if (newAmount == 0) {
            selectedResources.remove(material);
        } else {
            selectedResources.put(material, newAmount);
        }
    }
    
    private List<String> createTransferInfo() {
        List<String> info = new ArrayList<>();
        info.add("§7From: §d" + sourceCaravan.getName());
        info.add("§7To: §d" + destinationCaravan.getName());
        
        double distance = sourceCaravan.distanceTo(destinationCaravan);
        if (distance == Double.MAX_VALUE) {
            info.add("§7Distance: §cDifferent World");
        } else {
            info.add("§7Distance: §f" + String.format("%.1f blocks", distance));
        }
        
        if (!selectedResources.isEmpty()) {
            int totalItems = selectedResources.values().stream().mapToInt(Integer::intValue).sum();
            int estimatedCost = calculateTransferCost(distance, selectedResources);
            long estimatedTime = calculateDeliveryTime(distance);
            
            info.add("§7Items: §e" + totalItems);
            info.add("§7Est. Cost: §6" + estimatedCost + " shards");
            info.add("§7Est. Time: §f" + formatTime(estimatedTime));
            info.add("");
            info.add("§7Selected Resources:");
            for (var entry : selectedResources.entrySet()) {
                info.add("  §8- §f" + entry.getValue() + "x " + 
                        entry.getKey().name().toLowerCase().replace('_', ' '));
            }
        } else {
            info.add("§7No resources selected");
        }
        
        return info;
    }
    
    private void executeTransfer() {
        if (selectedResources.isEmpty()) {
            player.sendMessage("§cNo resources selected for transfer!");
            return;
        }
        
        ResourceTransfer transfer = caravanManager.createTransfer(
                player, 
                sourceCaravan.getId(), 
                destinationCaravan.getId(), 
                selectedResources
        );
        
        if (transfer != null) {
            player.sendMessage("§aTransfer created successfully!");
            player.sendMessage("§7Transfer ID: §f" + transfer.getId().substring(0, 8));
            player.sendMessage("§7Cost: §6" + transfer.getCost() + " shards");
            player.sendMessage("§7Delivery time: §f" + formatTime(transfer.getRemainingTime()));
            player.closeInventory();
        } else {
            player.sendMessage("§cTransfer failed! Check that you have enough shards and the caravan has enough resources.");
        }
    }
    
    private int calculateTransferCost(double distance, Map<Material, Integer> resources) {
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
        if (millis <= 0) return "Ready!";
        
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
    
    private Component text(String legacyText) {
        return LegacyComponentSerializer.legacySection().deserialize(legacyText);
    }
}
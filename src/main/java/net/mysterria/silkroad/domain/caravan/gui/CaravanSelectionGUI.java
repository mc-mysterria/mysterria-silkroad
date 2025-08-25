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
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CaravanSelectionGUI {
    
    private final CaravanManager caravanManager;
    private final Player player;
    private final Caravan sourceCaravan;
    private final PaginatedGui gui;
    
    public CaravanSelectionGUI(CaravanManager caravanManager, Player player, Caravan sourceCaravan) {
        this.caravanManager = caravanManager;
        this.player = player;
        this.sourceCaravan = sourceCaravan;
        
        this.gui = Gui.paginated()
                .title(Component.text("Select Destination Caravan"))
                .rows(6)
                .create();
        
        setupNavigationItems();
        addCaravans();
    }
    
    private void setupNavigationItems() {
        GuiItem prevItem = ItemBuilder.from(Material.ARROW)
                .name(Component.text("Previous Page").decoration(TextDecoration.ITALIC, false))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    gui.previous();
                });
        gui.setItem(6, 3, prevItem);
        
        GuiItem nextItem = ItemBuilder.from(Material.ARROW)
                .name(Component.text("Next Page").decoration(TextDecoration.ITALIC, false))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    gui.next();
                });
        gui.setItem(6, 7, nextItem);
        
        GuiItem backItem = ItemBuilder.from(Material.BARRIER)
                .name(Component.text("Back").decoration(TextDecoration.ITALIC, false))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    new CaravanMainGUI(caravanManager, player, sourceCaravan).open();
                });
        gui.setItem(6, 5, backItem);
    }
    
    private void addCaravans() {
        Collection<Caravan> allCaravans = caravanManager.getAllCaravans();
        
        for (Caravan caravan : allCaravans) {
            if (caravan.getId().equals(sourceCaravan.getId())) {
                continue;
            }
            
            double distance = sourceCaravan.distanceTo(caravan);
            long estimatedDeliveryTime = calculateDeliveryTime(distance);
            int baseCost = calculateBaseCost(distance);
            
            Material iconMaterial = getCaravanIcon(caravan);
            
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Name: " + caravan.getName()).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Distance: " + String.format("%.1f blocks", distance))
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Estimated Time: " + formatTime(estimatedDeliveryTime))
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Base Cost: " + baseCost + " (+ item cost)")
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text(""));
            
            if (distance > 10000) {
                lore.add(Component.text("⚠ Very Long Distance!")
                        .color(NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, false));
            } else if (distance > 5000) {
                lore.add(Component.text("⚠ Long Distance")
                        .color(NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false));
            } else {
                lore.add(Component.text("✓ Reasonable Distance")
                        .color(NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, false));
            }
            
            lore.add(Component.text(""));
            lore.add(Component.text("Click to select resources to send")
                    .decoration(TextDecoration.ITALIC, false));
            
            GuiItem item = ItemBuilder.from(iconMaterial)
                    .name(Component.text(caravan.getName()).decoration(TextDecoration.ITALIC, false))
                    .lore(lore)
                    .asGuiItem(event -> {
                        event.setCancelled(true);
                        new ResourceSelectionGUI(caravanManager, player, sourceCaravan, caravan).open();
                    });
            
            gui.addItem(item);
        }
        
        if (allCaravans.size() <= 1) {
            GuiItem noCaravansItem = ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE)
                    .name(Component.text("No Other Caravans Available")
                            .color(NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false))
                    .lore(Component.text("There are no other caravans to send resources to")
                            .decoration(TextDecoration.ITALIC, false))
                    .asGuiItem(event -> event.setCancelled(true));
            
            gui.addItem(noCaravansItem);
        }
    }
    
    private Material getCaravanIcon(Caravan caravan) {
        int hash = caravan.getId().hashCode();
        Material[] icons = {
                Material.CHEST, Material.BARREL, Material.SHULKER_BOX,
                Material.ENDER_CHEST, Material.HOPPER, Material.DROPPER
        };
        return icons[Math.abs(hash) % icons.length];
    }
    
    private long calculateDeliveryTime(double distance) {
        double baseTime = 5 * 60 * 1000;
        double distanceTime = distance * 1000;
        return (long) (baseTime + distanceTime);
    }
    
    private int calculateBaseCost(double distance) {
        return (int) Math.max(1, distance * 0.1);
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
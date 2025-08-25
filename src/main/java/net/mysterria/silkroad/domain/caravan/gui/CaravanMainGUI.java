package net.mysterria.silkroad.domain.caravan.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.mysterria.silkroad.domain.caravan.manager.CaravanManager;
import net.mysterria.silkroad.domain.caravan.model.Caravan;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CaravanMainGUI {
    
    private final CaravanManager caravanManager;
    private final Player player;
    private final Caravan currentCaravan;
    private final Gui gui;
    
    public CaravanMainGUI(CaravanManager caravanManager, Player player, Caravan currentCaravan) {
        this.caravanManager = caravanManager;
        this.player = player;
        this.currentCaravan = currentCaravan;
        
        this.gui = Gui.gui()
                .title(Component.text("Caravan: " + currentCaravan.getName()))
                .rows(3)
                .create();
        
        setupGUI();
    }
    
    private void setupGUI() {
        GuiItem inventoryItem = ItemBuilder.from(Material.CHEST)
                .name(Component.text("View Inventory").decoration(TextDecoration.ITALIC, false))
                .lore(createInventoryLore())
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    new CaravanInventoryGUI(caravanManager, player, currentCaravan).open();
                });
        gui.setItem(1, 3, inventoryItem);
        
        GuiItem sendResourcesItem = ItemBuilder.from(Material.MINECART)
                .name(Component.text("Send Resources").decoration(TextDecoration.ITALIC, false))
                .lore(
                        Component.text("Send resources to other caravans").decoration(TextDecoration.ITALIC, false),
                        Component.text("Delivery time depends on distance").decoration(TextDecoration.ITALIC, false)
                )
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    new CaravanSelectionGUI(caravanManager, player, currentCaravan).open();
                });
        gui.setItem(1, 5, sendResourcesItem);
        
        GuiItem transfersItem = ItemBuilder.from(Material.CLOCK)
                .name(Component.text("Active Transfers").decoration(TextDecoration.ITALIC, false))
                .lore(
                        Component.text("View your ongoing deliveries").decoration(TextDecoration.ITALIC, false)
                )
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    new CaravanTransfersGUI(caravanManager, player).open();
                });
        gui.setItem(1, 7, transfersItem);
        
        GuiItem infoItem = ItemBuilder.from(Material.BOOK)
                .name(Component.text("Caravan Info").decoration(TextDecoration.ITALIC, false))
                .lore(createInfoLore())
                .asGuiItem(event -> {
                    event.setCancelled(true);
                });
        gui.setItem(2, 5, infoItem);
        
        GuiItem closeItem = ItemBuilder.from(Material.BARRIER)
                .name(Component.text("Close").decoration(TextDecoration.ITALIC, false))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    player.closeInventory();
                });
        gui.setItem(3, 5, closeItem);
    }
    
    private List<Component> createInventoryLore() {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Current Resources:").decoration(TextDecoration.ITALIC, false));
        
        if (currentCaravan.getInventory().isEmpty()) {
            lore.add(Component.text("  No resources stored").decoration(TextDecoration.ITALIC, false));
        } else {
            int count = 0;
            for (var entry : currentCaravan.getInventory().entrySet()) {
                if (count >= 5) {
                    lore.add(Component.text("  ... and " + (currentCaravan.getInventory().size() - 5) + " more")
                            .decoration(TextDecoration.ITALIC, false));
                    break;
                }
                lore.add(Component.text("  " + entry.getKey().name() + ": " + entry.getValue())
                        .decoration(TextDecoration.ITALIC, false));
                count++;
            }
        }
        
        lore.add(Component.text(""));
        lore.add(Component.text("Click to view full inventory").decoration(TextDecoration.ITALIC, false));
        
        return lore;
    }
    
    private List<Component> createInfoLore() {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Name: " + currentCaravan.getName()).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("ID: " + currentCaravan.getId()).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Location: " + formatLocation()).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Created: " + formatTime(currentCaravan.getCreatedAt())).decoration(TextDecoration.ITALIC, false));
        
        return lore;
    }
    
    private String formatLocation() {
        var loc = currentCaravan.getLocation();
        return String.format("%.0f, %.0f, %.0f", loc.getX(), loc.getY(), loc.getZ());
    }
    
    private String formatTime(long timestamp) {
        long hours = (System.currentTimeMillis() - timestamp) / (1000 * 60 * 60);
        if (hours < 24) {
            return hours + " hours ago";
        } else {
            long days = hours / 24;
            return days + " days ago";
        }
    }
    
    public void open() {
        gui.open(player);
    }
}
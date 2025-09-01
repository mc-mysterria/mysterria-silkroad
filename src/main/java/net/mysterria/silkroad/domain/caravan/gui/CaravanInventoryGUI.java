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
import net.mysterria.silkroad.utils.TranslationUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Map;

public class CaravanInventoryGUI {
    
    private final CaravanManager caravanManager;
    private final Player player;
    private final Caravan caravan;
    private final PaginatedGui gui;
    
    public CaravanInventoryGUI(CaravanManager caravanManager, Player player, Caravan caravan) {
        this.caravanManager = caravanManager;
        this.player = player;
        this.caravan = caravan;
        
        this.gui = Gui.paginated()
                .title(TranslationUtil.translatable("gui.inventory.title", NamedTextColor.WHITE, caravan.getName()))
                .rows(6)
                .create();
        
        setupNavigationItems();
        addInventoryItems();
    }
    
    private void setupNavigationItems() {
        GuiItem prevItem = ItemBuilder.from(Material.ARROW)
                .name(TranslationUtil.translatable("gui.previous.page").decoration(TextDecoration.ITALIC, false))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    gui.previous();
                });
        gui.setItem(6, 3, prevItem);
        
        GuiItem nextItem = ItemBuilder.from(Material.ARROW)
                .name(TranslationUtil.translatable("gui.next.page").decoration(TextDecoration.ITALIC, false))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    gui.next();
                });
        gui.setItem(6, 7, nextItem);
        
        GuiItem backItem = ItemBuilder.from(Material.BARRIER)
                .name(TranslationUtil.translatable("gui.back").decoration(TextDecoration.ITALIC, false))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    new CaravanMainGUI(caravanManager, player, caravan).open();
                });
        gui.setItem(6, 5, backItem);
    }
    
    private void addInventoryItems() {
        if (caravan.getItemInventory().isEmpty()) {
            GuiItem emptyItem = ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE)
                    .name(TranslationUtil.translatable("gui.empty.inventory")
                            .color(NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false))
                    .lore(TranslationUtil.translatable("gui.empty.inventory.description")
                            .decoration(TextDecoration.ITALIC, false))
                    .asGuiItem(event -> event.setCancelled(true));
            
            gui.addItem(emptyItem);
            return;
        }
        
        for (org.bukkit.inventory.ItemStack itemStack : caravan.getItemInventory()) {
            if (itemStack == null || itemStack.getType() == Material.AIR) continue;
            
            String itemName = itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName() 
                    ? itemStack.getItemMeta().getDisplayName() 
                    : itemStack.getType().name().toLowerCase().replace('_', ' ');
            
            GuiItem item = ItemBuilder.from(itemStack.clone())
                    .lore(
                            TranslationUtil.translatable("gui.amount.label", NamedTextColor.WHITE, itemStack.getAmount()).decoration(TextDecoration.ITALIC, false),
                            Component.empty(),
                            TranslationUtil.translatable("gui.resource.stored")
                                    .color(NamedTextColor.GRAY)
                                    .decoration(TextDecoration.ITALIC, false)
                    )
                    .asGuiItem(event -> event.setCancelled(true));
            
            gui.addItem(item);
        }
    }
    
    public void open() {
        gui.open(player);
    }
}
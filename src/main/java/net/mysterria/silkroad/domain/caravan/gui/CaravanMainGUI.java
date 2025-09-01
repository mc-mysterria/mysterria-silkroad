package net.mysterria.silkroad.domain.caravan.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.mysterria.silkroad.domain.caravan.manager.CaravanManager;
import net.mysterria.silkroad.domain.caravan.model.Caravan;
import net.mysterria.silkroad.utils.TranslationUtil;
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
                .title(TranslationUtil.translatable("gui.caravan.title", NamedTextColor.WHITE, currentCaravan.getName()).decoration(TextDecoration.ITALIC, false))
                .rows(3)
                .create();
        
        setupGUI();
    }
    
    private void setupGUI() {
        GuiItem sendResourcesItem = ItemBuilder.from(Material.MINECART)
                .name(TranslationUtil.translatable("gui.send.resources").decoration(TextDecoration.ITALIC, false))
                .lore(
                        TranslationUtil.translatable("gui.send.resources.description").decoration(TextDecoration.ITALIC, false),
                        TranslationUtil.translatable("gui.delivery.time.description").decoration(TextDecoration.ITALIC, false)
                )
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    new CaravanSelectionGUI(caravanManager, player, currentCaravan).open();
                });
        gui.setItem(1, 4, sendResourcesItem);
        
        GuiItem transfersItem = ItemBuilder.from(Material.CLOCK)
                .name(TranslationUtil.translatable("gui.active.transfers").decoration(TextDecoration.ITALIC, false))
                .lore(
                        TranslationUtil.translatable("gui.active.transfers.description").decoration(TextDecoration.ITALIC, false)
                )
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    new CaravanTransfersGUI(caravanManager, player).open();
                });
        gui.setItem(1, 6, transfersItem);
        
        GuiItem infoItem = ItemBuilder.from(Material.BOOK)
                .name(TranslationUtil.translatable("gui.caravan.info").decoration(TextDecoration.ITALIC, false))
                .lore(createInfoLore())
                .asGuiItem(event -> {
                    event.setCancelled(true);
                });
        gui.setItem(2, 5, infoItem);
        
        GuiItem closeItem = ItemBuilder.from(Material.BARRIER)
                .name(TranslationUtil.translatable("gui.close").decoration(TextDecoration.ITALIC, false))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    player.closeInventory();
                });
        gui.setItem(3, 5, closeItem);
    }
    
    
    private List<Component> createInfoLore() {
        List<Component> lore = new ArrayList<>();
        lore.add(TranslationUtil.translatable("gui.caravan.name", currentCaravan.getName()).decoration(TextDecoration.ITALIC, false));
        lore.add(TranslationUtil.translatable("gui.caravan.id", currentCaravan.getId()).decoration(TextDecoration.ITALIC, false));
        lore.add(TranslationUtil.translatable("gui.caravan.location", formatLocation()).decoration(TextDecoration.ITALIC, false));
        lore.add(TranslationUtil.translatable("gui.caravan.created", formatTime(currentCaravan.getCreatedAt())).decoration(TextDecoration.ITALIC, false));
        
        return lore;
    }
    
    private String formatLocation() {
        var loc = currentCaravan.getLocation();
        return String.format("%.0f, %.0f, %.0f", loc.getX(), loc.getY(), loc.getZ());
    }
    
    private String formatTime(long timestamp) {
        long hours = (System.currentTimeMillis() - timestamp) / (1000 * 60 * 60);
        if (hours < 24) {
            return TranslationUtil.translate("time.hours.ago", String.valueOf(hours));
        } else {
            long days = hours / 24;
            return TranslationUtil.translate("time.days.ago", String.valueOf(days));
        }
    }
    
    public void open() {
        gui.open(player);
    }
}
package net.mysterria.silkroad.domain.caravan.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.mysterria.silkroad.domain.caravan.manager.CaravanManager;
import net.mysterria.silkroad.domain.caravan.model.ResourceTransfer;
import net.mysterria.silkroad.utils.TranslationUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CaravanTransfersGUI {
    
    private final CaravanManager caravanManager;
    private final Player player;
    private final PaginatedGui gui;
    
    public CaravanTransfersGUI(CaravanManager caravanManager, Player player) {
        this.caravanManager = caravanManager;
        this.player = player;
        
        this.gui = Gui.paginated()
                .title(TranslationUtil.translatable("gui.transfers.title"))
                .rows(6)
                .create();
        
        setupNavigationItems();
        addTransfers();
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
        
        GuiItem closeItem = ItemBuilder.from(Material.BARRIER)
                .name(TranslationUtil.translatable("gui.close").decoration(TextDecoration.ITALIC, false))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    player.closeInventory();
                });
        gui.setItem(6, 5, closeItem);
    }
    
    private void addTransfers() {
        List<ResourceTransfer> transfers = caravanManager.getPlayerTransfers(player.getUniqueId());
        
        if (transfers.isEmpty()) {
            GuiItem emptyItem = ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE)
                    .name(TranslationUtil.translatable("gui.no.active.transfers")
                            .color(NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false))
                    .lore(TranslationUtil.translatable("gui.no.transfers.description")
                            .decoration(TextDecoration.ITALIC, false))
                    .asGuiItem(event -> event.setCancelled(true));
            
            gui.addItem(emptyItem);
            return;
        }
        
        for (ResourceTransfer transfer : transfers) {
            Material iconMaterial = getTransferIcon(transfer);
            NamedTextColor statusColor = getStatusColor(transfer.getStatus());
            String timeRemaining = formatTime(transfer.getRemainingTime());
            
            List<Component> lore = new ArrayList<>();
            lore.add(TranslationUtil.translatable("transfer.from", NamedTextColor.WHITE, transfer.getSourceCaravanId()).decoration(TextDecoration.ITALIC, false));
            lore.add(TranslationUtil.translatable("transfer.to", NamedTextColor.WHITE, transfer.getDestinationCaravanId()).decoration(TextDecoration.ITALIC, false));
            lore.add(TranslationUtil.translatable("transfer.status", statusColor, transfer.getStatus().name())
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            
            if (transfer.getStatus() == ResourceTransfer.TransferStatus.IN_TRANSIT) {
                if (transfer.getRemainingTime() > 0) {
                    lore.add(TranslationUtil.translatable("transfer.time.remaining", NamedTextColor.YELLOW, timeRemaining)
                            .decoration(TextDecoration.ITALIC, false));
                } else {
                    lore.add(TranslationUtil.translatable("transfer.ready.delivery")
                            .color(NamedTextColor.GREEN)
                            .decoration(TextDecoration.ITALIC, false));
                }
            } else if (transfer.getStatus() == ResourceTransfer.TransferStatus.DELIVERED) {
                lore.add(TranslationUtil.translatable("transfer.delivered.success")
                        .color(NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, false));
            } else if (transfer.getStatus() == ResourceTransfer.TransferStatus.FAILED) {
                lore.add(TranslationUtil.translatable("transfer.failed")
                        .color(NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, false));
            }
            
            lore.add(Component.empty());
            lore.add(TranslationUtil.translatable("transfer.resources").decoration(TextDecoration.ITALIC, false));
            
            int resourceCount = 0;
            for (var entry : transfer.getResources().entrySet()) {
                if (resourceCount >= 3) {
                    lore.add(TranslationUtil.translatable("transfer.more.resources", NamedTextColor.WHITE, (transfer.getResources().size() - 3))
                            .decoration(TextDecoration.ITALIC, false));
                    break;
                }
                lore.add(TranslationUtil.translatable("gui.resource.quantity", entry.getKey().name(), entry.getValue())
                        .decoration(TextDecoration.ITALIC, false));
                resourceCount++;
            }
            
            GuiItem item = ItemBuilder.from(iconMaterial)
                    .name(TranslationUtil.translatable("transfer.id.short", statusColor, transfer.getId().substring(0, 8))
                            .decoration(TextDecoration.ITALIC, false))
                    .lore(lore)
                    .asGuiItem(event -> event.setCancelled(true));
            
            gui.addItem(item);
        }
    }
    
    private Material getTransferIcon(ResourceTransfer transfer) {
        return switch (transfer.getStatus()) {
            case PENDING -> Material.HOPPER;
            case IN_TRANSIT -> Material.MINECART;
            case DELIVERED -> Material.EMERALD;
            case FAILED -> Material.REDSTONE;
        };
    }
    
    private NamedTextColor getStatusColor(ResourceTransfer.TransferStatus status) {
        return switch (status) {
            case PENDING -> NamedTextColor.YELLOW;
            case IN_TRANSIT -> NamedTextColor.BLUE;
            case DELIVERED -> NamedTextColor.GREEN;
            case FAILED -> NamedTextColor.RED;
        };
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
    
    public void open() {
        gui.open(player);
    }
}
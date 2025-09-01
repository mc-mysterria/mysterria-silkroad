package net.mysterria.silkroad.domain.caravan.gui;

import dev.triumphteam.gui.builder.item.PaperItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.mysterria.silkroad.domain.caravan.manager.CaravanManager;
import net.mysterria.silkroad.domain.caravan.model.Caravan;
import net.mysterria.silkroad.utils.TranslationUtil;
import net.kyori.adventure.text.format.NamedTextColor;
import net.mysterria.silkroad.domain.caravan.model.ResourceTransfer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CaravanManagementGUI {
    
    private final CaravanManager caravanManager;
    private final Player player;
    
    public CaravanManagementGUI(CaravanManager caravanManager, Player player) {
        this.caravanManager = caravanManager;
        this.player = player;
    }
    
    public void open() {
        List<Caravan> playerCaravans = caravanManager.getPlayerCaravans(player.getUniqueId());
        
        if (playerCaravans.isEmpty()) {
            player.sendMessage(TranslationUtil.translatable("caravan.no.member.caravans", NamedTextColor.RED));
            return;
        }
        
        Gui gui = Gui.gui()
                .title(TranslationUtil.translatable("gui.caravan.management").decoration(TextDecoration.ITALIC, false))
                .rows(6)
                .create();
        
        setupMainMenu(gui, playerCaravans);
        
        gui.open(player);
    }
    
    private void setupMainMenu(Gui gui, List<Caravan> playerCaravans) {
        int slot = 10;
        
        for (Caravan caravan : playerCaravans) {
            if (slot >= 17) {
                slot = slot + 9 - 7;
            }
            if (slot >= 44) break;
            
            GuiItem item = PaperItemBuilder.from(Material.EMERALD)
                    .name(Component.text(caravan.getName()).color(NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false))
                    .lore(TranslationUtil.translatable("gui.caravan.info.location.label", formatLocation(caravan.getLocation())).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                          TranslationUtil.translatable("gui.caravan.info.resources.count", String.valueOf(caravan.getItemInventory().size())).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                          Component.empty(),
                          TranslationUtil.translatable("gui.click.to.manage").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
                    .asGuiItem(event -> {
                        event.setCancelled(true);
                        openCaravanDetails(caravan);
                    });
            
            gui.setItem(slot, item);
            slot++;
        }
        
        // Transfer management section
        List<ResourceTransfer> incomingTransfers = caravanManager.getIncomingTransfers(player.getUniqueId());
        List<ResourceTransfer> deliveredTransfers = caravanManager.getDeliveredTransfersForPlayer(player.getUniqueId());
        
        if (!incomingTransfers.isEmpty()) {
            GuiItem transfersItem = PaperItemBuilder.from(Material.CHEST_MINECART)
                    .name(TranslationUtil.translatable("gui.incoming.transfers").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false))
                    .lore(TranslationUtil.translatable("gui.incoming.transfers.count", String.valueOf(incomingTransfers.size())).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                          TranslationUtil.translatable("gui.destination.action").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
                    .asGuiItem(event -> {
                        event.setCancelled(true);
                        openIncomingTransfers(incomingTransfers);
                    });
            gui.setItem(48, transfersItem);
        }
        
        if (!deliveredTransfers.isEmpty()) {
            GuiItem claimTransfersItem = PaperItemBuilder.from(Material.EMERALD)
                    .name(TranslationUtil.translatable("gui.delivered.transfers").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false))
                    .lore(TranslationUtil.translatable("gui.delivered.transfers.count", String.valueOf(deliveredTransfers.size())).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                          TranslationUtil.translatable("gui.delivered.transfers.action").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
                    .asGuiItem(event -> {
                        event.setCancelled(true);
                        TransferClaimGUI claimGUI = new TransferClaimGUI(caravanManager, player);
                        claimGUI.open();
                    });
            gui.setItem(49, claimTransfersItem);
        }
        
        // All transfers overview button
        GuiItem allTransfersItem = PaperItemBuilder.from(Material.COMPASS)
                .name(TranslationUtil.translatable("gui.all.transfers").color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false))
                .lore(TranslationUtil.translatable("gui.all.transfers.description1").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                      TranslationUtil.translatable("gui.all.transfers.description2").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    openAllTransfersGUI();
                });
        gui.setItem(50, allTransfersItem);
        
        GuiItem closeItem = PaperItemBuilder.from(Material.BARRIER)
                .name(TranslationUtil.translatable("gui.close").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    player.closeInventory();
                });
        gui.setItem(45, closeItem);
    }
    
    private void openCaravanDetails(Caravan caravan) {
        Gui gui = Gui.gui()
                .title(TranslationUtil.translatable("gui.caravan.details.title", caravan.getName()).color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false))
                .rows(6)
                .create();
        
        GuiItem infoItem = PaperItemBuilder.from(Material.BOOK)
                .name(TranslationUtil.translatable("gui.caravan.info.label").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false))
                .lore(TranslationUtil.translatable("gui.caravan.info.name.label", caravan.getName()).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                      TranslationUtil.translatable("gui.caravan.info.id.label", caravan.getId()).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                      TranslationUtil.translatable("gui.caravan.info.location.label", formatLocation(caravan.getLocation())).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                .asGuiItem(event -> event.setCancelled(true));
        gui.setItem(4, infoItem);
        
        GuiItem manageInventoryItem = PaperItemBuilder.from(Material.HOPPER)
                .name(TranslationUtil.translatable("gui.manage.inventory").color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false))
                .lore(TranslationUtil.translatable("gui.manage.inventory.description1").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                      TranslationUtil.translatable("gui.manage.inventory.description2").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    openInventoryManagement(caravan);
                });
        gui.setItem(23, manageInventoryItem);
        
        GuiItem transferItem = PaperItemBuilder.from(Material.MINECART)
                .name(TranslationUtil.translatable("gui.send.resources.label").color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false))
                .lore(TranslationUtil.translatable("gui.send.resources.description").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    openTransferGUI(caravan);
                });
        gui.setItem(21, transferItem);
        
        GuiItem backItem = PaperItemBuilder.from(Material.ARROW)
                .name(TranslationUtil.translatable("gui.back.arrow").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    open();
                });
        gui.setItem(45, backItem);
        
        GuiItem closeItem = PaperItemBuilder.from(Material.BARRIER)
                .name(TranslationUtil.translatable("gui.close").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    player.closeInventory();
                });
        gui.setItem(49, closeItem);
        
        gui.open(player);
    }
    
    private void openCaravanInventory(Caravan caravan) {
        Gui gui = Gui.gui()
                .title(TranslationUtil.translatable("gui.caravan.inventory.title", caravan.getName()).color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false))
                .rows(6)
                .create();
        
        int slot = 0;
        for (ItemStack itemStack : caravan.getItemInventory()) {
            if (slot >= 45) break;
            if (itemStack == null || itemStack.getType() == Material.AIR) continue;
            
            String itemName = itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName() 
                    ? itemStack.getItemMeta().getDisplayName() 
                    : itemStack.getType().name().toLowerCase().replace('_', ' ');
            
            GuiItem item = PaperItemBuilder.from(itemStack.clone())
                    .lore(TranslationUtil.translatable("gui.inventory.amount.label", String.valueOf(itemStack.getAmount())).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                          TranslationUtil.translatable("gui.inventory.type.label", itemName).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                    .asGuiItem(event -> event.setCancelled(true));
            
            gui.setItem(slot, item);
            slot++;
        }
        
        if (caravan.getItemInventory().isEmpty()) {
            GuiItem emptyItem = PaperItemBuilder.from(Material.BARRIER)
                    .name(TranslationUtil.translatable("gui.empty.inventory.title").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                    .lore(TranslationUtil.translatable("gui.empty.inventory.description").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                    .asGuiItem(event -> event.setCancelled(true));
            gui.setItem(22, emptyItem);
        }
        
        GuiItem backItem = PaperItemBuilder.from(Material.ARROW)
                .name(TranslationUtil.translatable("gui.back.arrow").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    openCaravanDetails(caravan);
                });
        gui.setItem(45, backItem);
        
        gui.open(player);
    }
    
    private void openTransferGUI(Caravan sourceCaravan) {
        List<Caravan> availableDestinations = caravanManager.getAllCaravans().stream()
                .filter(c -> !c.getId().equals(sourceCaravan.getId()))
                .toList();
        
        if (availableDestinations.isEmpty()) {
            player.sendMessage(TranslationUtil.translatable("caravan.no.other.caravans", NamedTextColor.RED));
            return;
        }
        
        Gui gui = Gui.gui()
                .title(TranslationUtil.translatable("gui.select.destination.title").color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false))
                .rows(6)
                .create();
        
        int slot = 10;
        for (Caravan destination : availableDestinations) {
            if (slot >= 17) {
                slot = slot + 9 - 7;
            }
            if (slot >= 44) break;
            
            double distance = sourceCaravan.distanceTo(destination);
            String distanceStr = distance == Double.MAX_VALUE ? 
                TranslationUtil.translate("gui.destination.different.world") : 
                TranslationUtil.translate("gui.destination.distance.blocks", String.format("%.1f", distance));
            
            GuiItem item = PaperItemBuilder.from(Material.ENDER_PEARL)
                    .name(Component.text(destination.getName()).color(NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false))
                    .lore(TranslationUtil.translatable("gui.destination.distance.label", distanceStr).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                          TranslationUtil.translatable("gui.caravan.info.location.label", formatLocation(destination.getLocation())).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                          Component.empty(),
                          TranslationUtil.translatable("gui.destination.action").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
                    .asGuiItem(event -> {
                        event.setCancelled(true);
                        openResourceSelectionGUI(sourceCaravan, destination);
                    });
            
            gui.setItem(slot, item);
            slot++;
        }
        
        GuiItem backItem = PaperItemBuilder.from(Material.ARROW)
                .name(TranslationUtil.translatable("gui.back.arrow").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    openCaravanDetails(sourceCaravan);
                });
        gui.setItem(45, backItem);
        
        gui.open(player);
    }
    
    private void openResourceSelectionGUI(Caravan source, Caravan destination) {
        // Use existing working transfer GUI (will be enhanced later)
        ResourceTransferGUI transferGUI = new ResourceTransferGUI(caravanManager, player, source, destination);
        transferGUI.open();
    }
    
    private void openInventoryManagement(Caravan caravan) {
        CaravanInventoryManagementGUI inventoryGUI = new CaravanInventoryManagementGUI(caravanManager, player, caravan);
        inventoryGUI.open();
    }
    
    private void openIncomingTransfers(List<ResourceTransfer> transfers) {
        Gui gui = Gui.gui()
                .title(TranslationUtil.translatable("gui.incoming.transfers.title.hardcoded").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false))
                .rows(6)
                .create();
        
        int slot = 0;
        for (ResourceTransfer transfer : transfers) {
            if (slot >= 45) break;
            
            String timeRemaining = formatTime(transfer.getRemainingTime());
            String status = transfer.getStatus().name();
            Material statusMaterial = switch (transfer.getStatus()) {
                case IN_TRANSIT -> Material.MINECART;
                case PENDING -> Material.HOPPER;
                case DELIVERED -> Material.GREEN_WOOL;
                case FAILED -> Material.RED_WOOL;
            };
            
            List<Component> lore = new ArrayList<>();
            lore.add(TranslationUtil.translatable("gui.transfer.from", transfer.getSourceCaravanId()).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(TranslationUtil.translatable("gui.transfer.to", transfer.getDestinationCaravanId()).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(TranslationUtil.translatable("gui.transfer.status", status).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(TranslationUtil.translatable("gui.transfer.time.remaining", timeRemaining).color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            lore.add(TranslationUtil.translatable("gui.transfer.distance.blocks.label", String.format("%.1f", transfer.getDistance())).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(TranslationUtil.translatable("gui.transfer.cost.shards", String.valueOf(transfer.getCost())).color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            lore.add(TranslationUtil.translatable("gui.transfer.resources.heading").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            for (var entry : transfer.getResources().entrySet()) {
                lore.add(Component.text("  - " + entry.getValue() + "x " + entry.getKey().name().toLowerCase().replace('_', ' ')).color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
            }
            
            GuiItem item = PaperItemBuilder.from(statusMaterial)
                    .name(TranslationUtil.translatable("gui.transfer.id", transfer.getId().substring(0, 8)).color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
                    .lore(lore.toArray(Component[]::new))
                    .asGuiItem(event -> event.setCancelled(true));
            
            gui.setItem(slot, item);
            slot++;
        }
        
        if (transfers.isEmpty()) {
            GuiItem emptyItem = PaperItemBuilder.from(Material.BARRIER)
                    .name(TranslationUtil.translatable("gui.transfer.no.incoming").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                    .lore(TranslationUtil.translatable("gui.transfer.no.incoming.description").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                    .asGuiItem(event -> event.setCancelled(true));
            gui.setItem(22, emptyItem);
        }
        
        GuiItem backItem = PaperItemBuilder.from(Material.ARROW)
                .name(TranslationUtil.translatable("gui.back.to.main").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    open();
                });
        gui.setItem(45, backItem);
        
        gui.open(player);
    }
    
    private void openAllTransfersGUI() {
        List<ResourceTransfer> playerTransfers = caravanManager.getPlayerTransfers(player.getUniqueId());
        List<ResourceTransfer> deliveredTransfers = caravanManager.getDeliveredTransfersForPlayer(player.getUniqueId());
        
        Gui gui = Gui.gui()
                .title(TranslationUtil.translatable("gui.all.transfers.title.hardcoded").color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false))
                .rows(6)
                .create();
        
        List<ResourceTransfer> allTransfers = new ArrayList<>();
        allTransfers.addAll(playerTransfers);
        allTransfers.addAll(deliveredTransfers);
        
        // Sort by creation time (newest first)
        allTransfers.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
        
        int slot = 0;
        for (ResourceTransfer transfer : allTransfers) {
            if (slot >= 45) break;
            
            String status = transfer.getStatus().name();
            Component timeInfo = switch (transfer.getStatus()) {
                case PENDING, IN_TRANSIT -> TranslationUtil.translatable("gui.transfer.eta.prefix", formatTime(transfer.getRemainingTime()));
                case DELIVERED -> TranslationUtil.translatable("gui.transfer.ready.claim");
                case FAILED -> TranslationUtil.translatable("gui.transfer.failed.status");
            };
            
            Material statusMaterial = switch (transfer.getStatus()) {
                case IN_TRANSIT -> Material.MINECART;
                case PENDING -> Material.HOPPER;
                case DELIVERED -> Material.EMERALD;
                case FAILED -> Material.RED_WOOL;
            };
            
            List<Component> lore = new ArrayList<>();
            lore.add(TranslationUtil.translatable("gui.transfer.from", transfer.getSourceCaravanId()).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(TranslationUtil.translatable("gui.transfer.to", transfer.getDestinationCaravanId()).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(TranslationUtil.translatable("gui.transfer.status", status).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(timeInfo.color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(TranslationUtil.translatable("gui.transfer.distance.blocks.label", String.format("%.1f", transfer.getDistance())).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(TranslationUtil.translatable("gui.transfer.cost.shards", String.valueOf(transfer.getCost())).color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
            
            if (transfer.getStatus() == ResourceTransfer.TransferStatus.DELIVERED) {
                lore.add(Component.empty());
                lore.add(TranslationUtil.translatable("gui.transfer.click.claim").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
            }
            
            GuiItem item = PaperItemBuilder.from(statusMaterial)
                    .name(TranslationUtil.translatable("gui.transfer.id", transfer.getId().substring(0, 8)).color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
                    .lore(lore.toArray(Component[]::new))
                    .asGuiItem(event -> {
                        event.setCancelled(true);
                        if (transfer.getStatus() == ResourceTransfer.TransferStatus.DELIVERED) {
                            TransferClaimGUI claimGUI = new TransferClaimGUI(caravanManager, player);
                            claimGUI.open();
                        }
                    });
            
            gui.setItem(slot, item);
            slot++;
        }
        
        if (allTransfers.isEmpty()) {
            GuiItem emptyItem = PaperItemBuilder.from(Material.BARRIER)
                    .name(TranslationUtil.translatable("gui.transfer.no.transfers.yet").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                    .lore(TranslationUtil.translatable("gui.transfer.no.transfers.yet.description").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                    .asGuiItem(event -> event.setCancelled(true));
            gui.setItem(22, emptyItem);
        }
        
        // Quick claim button if there are delivered transfers
        if (!deliveredTransfers.isEmpty()) {
            GuiItem quickClaimItem = PaperItemBuilder.from(Material.DIAMOND)
                    .name(TranslationUtil.translatable("gui.quick.claim.all.transfers").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false))
                    .lore(TranslationUtil.translatable("gui.quick.claim.delivered.count", String.valueOf(deliveredTransfers.size())).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                          TranslationUtil.translatable("gui.quick.claim.open.interface").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
                    .asGuiItem(event -> {
                        event.setCancelled(true);
                        TransferClaimGUI claimGUI = new TransferClaimGUI(caravanManager, player);
                        claimGUI.open();
                    });
            gui.setItem(53, quickClaimItem);
        }
        
        GuiItem backItem = PaperItemBuilder.from(Material.ARROW)
                .name(TranslationUtil.translatable("gui.back.to.main").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    open();
                });
        gui.setItem(45, backItem);
        
        gui.open(player);
    }
    
    private String formatLocation(org.bukkit.Location location) {
        return String.format("%.0f, %.0f, %.0f", 
                location.getX(), 
                location.getY(), 
                location.getZ());
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
        return LegacyComponentSerializer.legacySection().deserialize(legacyText).decoration(TextDecoration.ITALIC, false);
    }
}
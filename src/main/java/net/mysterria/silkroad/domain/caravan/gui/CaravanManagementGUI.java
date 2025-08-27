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
            player.sendMessage("§cYou are not a member or owner of any caravans.");
            return;
        }
        
        Gui gui = Gui.gui()
                .title(Component.text("Caravan Management"))
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
            
            boolean isOwner = caravan.isOwner(player.getUniqueId());
            Material material = isOwner ? Material.DIAMOND : Material.EMERALD;
            
            GuiItem item = PaperItemBuilder.from(material)
                    .name(text("§d" + caravan.getName()))
                    .lore(text("§7Role: " + (isOwner ? "§6Owner" : "§aMember")),
                          text("§7Location: §f" + formatLocation(caravan.getLocation())),
                          text("§7Resources: §f" + caravan.getItemInventory().size() + " items"),
                          text(""),
                          text("§eClick to manage this caravan"))
                    .asGuiItem(event -> {
                        event.setCancelled(true);
                        openCaravanDetails(caravan);
                    });
            
            gui.setItem(slot, item);
            slot++;
        }
        
        List<ResourceTransfer> incomingTransfers = caravanManager.getIncomingTransfers(player.getUniqueId());
        if (!incomingTransfers.isEmpty()) {
            GuiItem transfersItem = PaperItemBuilder.from(Material.CHEST_MINECART)
                    .name(text("§aIncoming Transfers"))
                    .lore(text("§7You have §e" + incomingTransfers.size() + " §7incoming transfers"),
                          text("§eClick to view details"))
                    .asGuiItem(event -> {
                        event.setCancelled(true);
                        openIncomingTransfers(incomingTransfers);
                    });
            gui.setItem(49, transfersItem);
        }
        
        GuiItem closeItem = PaperItemBuilder.from(Material.BARRIER)
                .name(text("§cClose"))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    player.closeInventory();
                });
        gui.setItem(45, closeItem);
    }
    
    private void openCaravanDetails(Caravan caravan) {
        boolean isOwner = caravan.isOwner(player.getUniqueId());
        
        Gui gui = Gui.gui()
                .title(text("§6" + caravan.getName() + " Management"))
                .rows(6)
                .create();
        
        GuiItem infoItem = PaperItemBuilder.from(Material.BOOK)
                .name(text("§aCaravan Information"))
                .lore(text("§7Name: §d" + caravan.getName()),
                      text("§7ID: §f" + caravan.getId()),
                      text("§7Role: " + (isOwner ? "§6Owner" : "§aMember")),
                      text("§7Location: §f" + formatLocation(caravan.getLocation())))
                .asGuiItem(event -> event.setCancelled(true));
        gui.setItem(4, infoItem);
        
        GuiItem inventoryItem = PaperItemBuilder.from(Material.CHEST)
                .name(text("§eCaravan Inventory"))
                .lore(text("§7View and manage caravan inventory"))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    openCaravanInventory(caravan);
                });
        gui.setItem(20, inventoryItem);
        
        // Add inventory management button for members/owners
        GuiItem manageInventoryItem = PaperItemBuilder.from(Material.HOPPER)
                .name(text("§6Manage Inventory"))
                .lore(text("§7Deposit and withdraw items"),
                      text("§7Interactive inventory management"))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    openInventoryManagement(caravan);
                });
        gui.setItem(24, manageInventoryItem);
        
        if (isOwner) {
            GuiItem transferItem = PaperItemBuilder.from(Material.MINECART)
                    .name(text("§6Send Resources"))
                    .lore(text("§7Transfer resources to another caravan"),
                          text("§c§lOwner Only"))
                    .asGuiItem(event -> {
                        event.setCancelled(true);
                        openTransferGUI(caravan);
                    });
            gui.setItem(22, transferItem);
        }
        
        GuiItem backItem = PaperItemBuilder.from(Material.ARROW)
                .name(text("§7← Back"))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    open();
                });
        gui.setItem(45, backItem);
        
        GuiItem closeItem = PaperItemBuilder.from(Material.BARRIER)
                .name(text("§cClose"))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    player.closeInventory();
                });
        gui.setItem(49, closeItem);
        
        gui.open(player);
    }
    
    private void openCaravanInventory(Caravan caravan) {
        Gui gui = Gui.gui()
                .title(text("§6" + caravan.getName() + " - Inventory"))
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
                    .lore(text("§7Amount: §e" + itemStack.getAmount()),
                          text("§7Type: §f" + itemName))
                    .asGuiItem(event -> event.setCancelled(true));
            
            gui.setItem(slot, item);
            slot++;
        }
        
        if (caravan.getItemInventory().isEmpty()) {
            GuiItem emptyItem = PaperItemBuilder.from(Material.BARRIER)
                    .name(text("§7Empty Inventory"))
                    .lore(text("§7This caravan has no resources"))
                    .asGuiItem(event -> event.setCancelled(true));
            gui.setItem(22, emptyItem);
        }
        
        GuiItem backItem = PaperItemBuilder.from(Material.ARROW)
                .name(text("§7← Back"))
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
            player.sendMessage("§cNo other caravans available for transfer.");
            return;
        }
        
        Gui gui = Gui.gui()
                .title(text("§6Select Destination Caravan"))
                .rows(6)
                .create();
        
        int slot = 10;
        for (Caravan destination : availableDestinations) {
            if (slot >= 17) {
                slot = slot + 9 - 7;
            }
            if (slot >= 44) break;
            
            double distance = sourceCaravan.distanceTo(destination);
            String distanceStr = distance == Double.MAX_VALUE ? "Different World" : String.format("%.1f blocks", distance);
            
            GuiItem item = PaperItemBuilder.from(Material.ENDER_PEARL)
                    .name(text("§d" + destination.getName()))
                    .lore(text("§7Distance: §f" + distanceStr),
                          text("§7Location: §f" + formatLocation(destination.getLocation())),
                          text(""),
                          text("§eClick to select destination"))
                    .asGuiItem(event -> {
                        event.setCancelled(true);
                        openResourceSelectionGUI(sourceCaravan, destination);
                    });
            
            gui.setItem(slot, item);
            slot++;
        }
        
        GuiItem backItem = PaperItemBuilder.from(Material.ARROW)
                .name(text("§7← Back"))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    openCaravanDetails(sourceCaravan);
                });
        gui.setItem(45, backItem);
        
        gui.open(player);
    }
    
    private void openResourceSelectionGUI(Caravan source, Caravan destination) {
        ResourceTransferGUI transferGUI = new ResourceTransferGUI(caravanManager, player, source, destination);
        transferGUI.open();
    }
    
    private void openInventoryManagement(Caravan caravan) {
        CaravanInventoryManagementGUI inventoryGUI = new CaravanInventoryManagementGUI(caravanManager, player, caravan);
        inventoryGUI.open();
    }
    
    private void openIncomingTransfers(List<ResourceTransfer> transfers) {
        Gui gui = Gui.gui()
                .title(text("§aIncoming Transfers"))
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
            
            List<String> lore = new ArrayList<>();
            lore.add("§7From: §d" + transfer.getSourceCaravanId());
            lore.add("§7To: §d" + transfer.getDestinationCaravanId());
            lore.add("§7Status: §f" + status);
            lore.add("§7Time Remaining: §e" + timeRemaining);
            lore.add("§7Distance: §f" + String.format("%.1f blocks", transfer.getDistance()));
            lore.add("§7Cost: §6" + transfer.getCost() + " shards");
            lore.add("");
            lore.add("§7Resources:");
            for (var entry : transfer.getResources().entrySet()) {
                lore.add("  §8- §f" + entry.getValue() + "x " + entry.getKey().name().toLowerCase().replace('_', ' '));
            }
            
            GuiItem item = PaperItemBuilder.from(statusMaterial)
                    .name(text("§eTransfer #" + transfer.getId().substring(0, 8)))
                    .lore(lore.stream().map(this::text).toArray(Component[]::new))
                    .asGuiItem(event -> event.setCancelled(true));
            
            gui.setItem(slot, item);
            slot++;
        }
        
        if (transfers.isEmpty()) {
            GuiItem emptyItem = PaperItemBuilder.from(Material.BARRIER)
                    .name(text("§7No Incoming Transfers"))
                    .lore(text("§7You have no incoming transfers"))
                    .asGuiItem(event -> event.setCancelled(true));
            gui.setItem(22, emptyItem);
        }
        
        GuiItem backItem = PaperItemBuilder.from(Material.ARROW)
                .name(text("§7← Back"))
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
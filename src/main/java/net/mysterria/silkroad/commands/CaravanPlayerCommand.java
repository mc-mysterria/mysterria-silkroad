package net.mysterria.silkroad.commands;

import dev.rollczi.litecommands.annotations.argument.Arg;
import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import net.mysterria.silkroad.SilkRoad;
import net.mysterria.silkroad.domain.caravan.gui.TransferClaimGUI;
import net.mysterria.silkroad.domain.caravan.manager.CaravanManager;
import net.mysterria.silkroad.domain.caravan.model.ResourceTransfer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

@Command(name = "caravan", aliases = "cv")
public class CaravanPlayerCommand {
    
    private final CaravanManager caravanManager;
    
    public CaravanPlayerCommand(SilkRoad plugin) {
        this.caravanManager = plugin.getCaravanManager();
    }
    
    @Execute
    public void openManagementGUI(@Context Player sender) {
        net.mysterria.silkroad.domain.caravan.gui.CaravanManagementGUI gui = 
                new net.mysterria.silkroad.domain.caravan.gui.CaravanManagementGUI(caravanManager, sender);
        gui.open();
    }
    
    @Execute(name = "manage")
    public void manageCommand(@Context Player sender) {
        sender.sendMessage("§6✨ Opening Caravan Management GUI...");
        sender.sendMessage("§7All caravan interactions are now available through the intuitive GUI interface!");
        net.mysterria.silkroad.domain.caravan.gui.CaravanManagementGUI gui = 
                new net.mysterria.silkroad.domain.caravan.gui.CaravanManagementGUI(caravanManager, sender);
        gui.open();
    }
    
    @Execute(name = "add")
    public void addItemGUIRedirect(@Context Player sender) {
        sender.sendMessage("§e➜ Item management is now done through GUIs!");
        sender.sendMessage("§7Use §f/caravan §7to open the management interface, then:");
        sender.sendMessage("§7• Click a caravan to manage");
        sender.sendMessage("§7• Select '§6Manage Inventory§7' for deposit/withdraw");
        sender.sendMessage("§7• Full NBT support with visual feedback!");
        
        net.mysterria.silkroad.domain.caravan.gui.CaravanManagementGUI gui = 
                new net.mysterria.silkroad.domain.caravan.gui.CaravanManagementGUI(caravanManager, sender);
        gui.open();
    }
    
    @Execute(name = "take")
    public void takeItemGUIRedirect(@Context Player sender) {
        sender.sendMessage("§e➜ Item management is now done through GUIs!");
        sender.sendMessage("§7Use §f/caravan §7to open the management interface, then:");
        sender.sendMessage("§7• Click a caravan to manage");
        sender.sendMessage("§7• Select '§6Manage Inventory§7' for deposit/withdraw");
        sender.sendMessage("§7• Full NBT support with visual feedback!");
        
        net.mysterria.silkroad.domain.caravan.gui.CaravanManagementGUI gui = 
                new net.mysterria.silkroad.domain.caravan.gui.CaravanManagementGUI(caravanManager, sender);
        gui.open();
    }
    
    @Execute(name = "inventory")
    public void inventoryCommand(@Context Player sender) {
        sender.sendMessage("§e➜ Opening inventory management interface...");
        net.mysterria.silkroad.domain.caravan.gui.CaravanManagementGUI gui = 
                new net.mysterria.silkroad.domain.caravan.gui.CaravanManagementGUI(caravanManager, sender);
        gui.open();
    }
    
    @Execute(name = "send")
    public void sendCommand(@Context Player sender) {
        sender.sendMessage("§e➜ Transfer creation is now done through GUIs!");
        sender.sendMessage("§7Use §f/caravan §7to open management, then:");
        sender.sendMessage("§7• Click a caravan you own");
        sender.sendMessage("§7• Select '§6Send Resources§7'");
        sender.sendMessage("§7• Choose destination and items with full NBT support!");
        
        net.mysterria.silkroad.domain.caravan.gui.CaravanManagementGUI gui = 
                new net.mysterria.silkroad.domain.caravan.gui.CaravanManagementGUI(caravanManager, sender);
        gui.open();
    }
    
    // Legacy commands with deprecation warnings
    @Execute(name = "add legacy")
    public void addItemLegacy(@Context Player sender, @Arg("caravan") String caravanId, 
                       @Arg("material") Material material, @Arg("amount") int amount) {
        
        if (amount <= 0) {
            sender.sendMessage("§cAmount must be greater than 0.");
            return;
        }
        
        if (amount > 64000) {
            sender.sendMessage("§cAmount cannot exceed 64000.");
            return;
        }
        
        sender.sendMessage("§c⚠ DEPRECATED: This command will be removed in future versions!");
        sender.sendMessage("§7Please use §f/caravan §7to access the modern GUI interface with full NBT support.");
        
        if (caravanManager.addItemToCaravan(caravanId, sender, material, amount)) {
            sender.sendMessage("§aAdded §e" + amount + " §f" + 
                    material.name().toLowerCase().replace('_', ' ') + 
                    " §ato caravan §d" + caravanId + "§a.");
        } else {
            sender.sendMessage("§cFailed to add items. Check that:");
            sender.sendMessage("§c- The caravan exists and you have access");
            sender.sendMessage("§c- You have enough items in your inventory");
        }
    }
    
    @Execute(name = "take legacy")
    public void removeItemLegacy(@Context Player sender, @Arg("caravan") String caravanId, 
                          @Arg("material") Material material, @Arg("amount") int amount) {
        
        if (amount <= 0) {
            sender.sendMessage("§cAmount must be greater than 0.");
            return;
        }
        
        sender.sendMessage("§c⚠ DEPRECATED: This command will be removed in future versions!");
        sender.sendMessage("§7Please use §f/caravan §7to access the modern GUI interface with full NBT support.");
        
        if (caravanManager.removeItemFromCaravan(caravanId, sender, material, amount)) {
            sender.sendMessage("§aTook §e" + amount + " §f" + 
                    material.name().toLowerCase().replace('_', ' ') + 
                    " §afrom caravan §d" + caravanId + "§a.");
        } else {
            sender.sendMessage("§cFailed to take items. Check that:");
            sender.sendMessage("§c- The caravan exists and you have access");
            sender.sendMessage("§c- The caravan has enough items");
            sender.sendMessage("§c- You have enough inventory space");
        }
    }
    
    @Execute(name = "transfers")
    public void openTransfersGUI(@Context Player sender) {
        TransferClaimGUI gui = new TransferClaimGUI(caravanManager, sender);
        gui.open();
    }
    
    @Execute(name = "transfers list")
    public void listTransfers(@Context Player sender) {
        List<ResourceTransfer> playerTransfers = caravanManager.getPlayerTransfers(sender.getUniqueId());
        List<ResourceTransfer> deliveredTransfers = caravanManager.getDeliveredTransfersForPlayer(sender.getUniqueId());
        
        sender.sendMessage("§6=== Your Transfers ===");
        
        if (playerTransfers.isEmpty() && deliveredTransfers.isEmpty()) {
            sender.sendMessage("§7You have no active or delivered transfers.");
            return;
        }
        
        // Show active transfers
        int activeCount = 0;
        for (ResourceTransfer transfer : playerTransfers) {
            if (transfer.getStatus() != ResourceTransfer.TransferStatus.DELIVERED) {
                if (activeCount == 0) {
                    sender.sendMessage("§e§lActive Transfers:");
                }
                activeCount++;
                
                String status = getStatusColor(transfer.getStatus()) + transfer.getStatus().name();
                String timeLeft = transfer.getRemainingTime() > 0 ? 
                        " §7(§f" + formatTime(transfer.getRemainingTime()) + " remaining§7)" : " §a(Ready)";
                
                sender.sendMessage("§7" + activeCount + ". §f" + transfer.getSourceCaravanId() + 
                        " §7→ §f" + transfer.getDestinationCaravanId() + " §7- " + status + timeLeft);
            }
        }
        
        // Show delivered transfers
        if (!deliveredTransfers.isEmpty()) {
            sender.sendMessage("§a§lDelivered Transfers §7(" + deliveredTransfers.size() + " awaiting claim):");
            for (int i = 0; i < Math.min(deliveredTransfers.size(), 5); i++) {
                ResourceTransfer transfer = deliveredTransfers.get(i);
                sender.sendMessage("§7" + (i + 1) + ". §f" + transfer.getSourceCaravanId() + 
                        " §7→ §f" + transfer.getDestinationCaravanId() + " §a(Ready to claim)");
            }
            
            if (deliveredTransfers.size() > 5) {
                sender.sendMessage("§7... and " + (deliveredTransfers.size() - 5) + " more");
            }
            
            sender.sendMessage("§eUse §f/caravan transfers §eto open the claim GUI");
        }
    }
    
    @Execute(name = "claim")
    public void claimTransfer(@Context Player sender, @Arg("transferId") String transferId) {
        if (caravanManager.claimTransferToInventory(transferId, sender)) {
            sender.sendMessage("§a✓ Transfer claimed to your inventory!");
        } else {
            sender.sendMessage("§cFailed to claim transfer. Make sure:");
            sender.sendMessage("§c- The transfer ID is correct");
            sender.sendMessage("§c- The transfer is delivered and ready to claim");
            sender.sendMessage("§c- You have enough inventory space");
        }
    }
    
    private String getStatusColor(ResourceTransfer.TransferStatus status) {
        return switch (status) {
            case PENDING -> "§e";
            case IN_TRANSIT -> "§6";
            case DELIVERED -> "§a";
            case FAILED -> "§c";
        };
    }
    
    private String formatTime(long timeMillis) {
        long seconds = timeMillis / 1000;
        if (seconds <= 0) {
            return "Ready";
        } else if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            return (seconds / 60) + "m " + (seconds % 60) + "s";
        } else {
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            return hours + "h " + minutes + "m";
        }
    }
}
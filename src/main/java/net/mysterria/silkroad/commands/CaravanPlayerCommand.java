package net.mysterria.silkroad.commands;

import dev.rollczi.litecommands.annotations.argument.Arg;
import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import net.mysterria.silkroad.SilkRoad;
import net.mysterria.silkroad.domain.caravan.gui.TransferClaimGUI;
import net.mysterria.silkroad.domain.caravan.manager.CaravanManager;
import net.mysterria.silkroad.domain.caravan.model.ResourceTransfer;
import net.mysterria.silkroad.utils.TranslationUtil;
import net.kyori.adventure.text.format.NamedTextColor;
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
        sender.sendMessage(TranslationUtil.translate("player.gui.opening"));
        sender.sendMessage(TranslationUtil.translate("player.gui.interface"));
        net.mysterria.silkroad.domain.caravan.gui.CaravanManagementGUI gui = 
                new net.mysterria.silkroad.domain.caravan.gui.CaravanManagementGUI(caravanManager, sender);
        gui.open();
    }
    
    @Execute(name = "add")
    public void addItemGUIRedirect(@Context Player sender) {
        sender.sendMessage(TranslationUtil.translate("player.gui.management"));
        sender.sendMessage(TranslationUtil.translate("player.gui.instructions.header"));
        sender.sendMessage(TranslationUtil.translate("player.gui.instructions.1"));
        sender.sendMessage(TranslationUtil.translate("player.gui.instructions.2"));
        sender.sendMessage(TranslationUtil.translate("player.gui.instructions.3"));
        
        net.mysterria.silkroad.domain.caravan.gui.CaravanManagementGUI gui = 
                new net.mysterria.silkroad.domain.caravan.gui.CaravanManagementGUI(caravanManager, sender);
        gui.open();
    }
    
    @Execute(name = "take")
    public void takeItemGUIRedirect(@Context Player sender) {
        sender.sendMessage(TranslationUtil.translate("player.gui.management"));
        sender.sendMessage(TranslationUtil.translate("player.gui.instructions.header"));
        sender.sendMessage(TranslationUtil.translate("player.gui.instructions.1"));
        sender.sendMessage(TranslationUtil.translate("player.gui.instructions.2"));
        sender.sendMessage(TranslationUtil.translate("player.gui.instructions.3"));
        
        net.mysterria.silkroad.domain.caravan.gui.CaravanManagementGUI gui = 
                new net.mysterria.silkroad.domain.caravan.gui.CaravanManagementGUI(caravanManager, sender);
        gui.open();
    }
    
    @Execute(name = "inventory")
    public void inventoryCommand(@Context Player sender) {
        sender.sendMessage(TranslationUtil.translate("player.inventory.opening"));
        net.mysterria.silkroad.domain.caravan.gui.CaravanManagementGUI gui = 
                new net.mysterria.silkroad.domain.caravan.gui.CaravanManagementGUI(caravanManager, sender);
        gui.open();
    }
    
    @Execute(name = "send")
    public void sendCommand(@Context Player sender) {
        sender.sendMessage(TranslationUtil.translate("player.transfer.gui"));
        sender.sendMessage(TranslationUtil.translate("player.transfer.instructions.header"));
        sender.sendMessage(TranslationUtil.translate("player.transfer.instructions.1"));
        sender.sendMessage(TranslationUtil.translate("player.transfer.instructions.2"));
        sender.sendMessage(TranslationUtil.translate("player.transfer.instructions.3"));
        
        net.mysterria.silkroad.domain.caravan.gui.CaravanManagementGUI gui = 
                new net.mysterria.silkroad.domain.caravan.gui.CaravanManagementGUI(caravanManager, sender);
        gui.open();
    }
    
    // Legacy commands with deprecation warnings
    @Execute(name = "add legacy")
    public void addItemLegacy(@Context Player sender, @Arg("caravan") String caravanId, 
                       @Arg("material") Material material, @Arg("amount") int amount) {
        
        if (amount <= 0) {
            sender.sendMessage(TranslationUtil.translate("player.amount.must.be.positive"));
            return;
        }
        
        if (amount > 64000) {
            sender.sendMessage(TranslationUtil.translate("player.amount.too.large"));
            return;
        }
        
        sender.sendMessage(TranslationUtil.translate("player.deprecated.warning"));
        sender.sendMessage(TranslationUtil.translate("player.deprecated.use.gui"));
        
        if (caravanManager.addItemToCaravan(caravanId, sender, material, amount)) {
            sender.sendMessage(TranslationUtil.translate("player.items.added", String.valueOf(amount), material.name().toLowerCase().replace('_', ' '), caravanId));
        } else {
            sender.sendMessage(TranslationUtil.translate("player.items.add.failed.header"));
            sender.sendMessage(TranslationUtil.translate("player.items.add.failed.1"));
            sender.sendMessage(TranslationUtil.translate("player.items.add.failed.2"));
        }
    }
    
    @Execute(name = "take legacy")
    public void removeItemLegacy(@Context Player sender, @Arg("caravan") String caravanId, 
                          @Arg("material") Material material, @Arg("amount") int amount) {
        
        if (amount <= 0) {
            sender.sendMessage(TranslationUtil.translate("player.amount.must.be.positive"));
            return;
        }
        
        sender.sendMessage(TranslationUtil.translate("player.deprecated.warning"));
        sender.sendMessage(TranslationUtil.translate("player.deprecated.use.gui"));
        
        if (caravanManager.removeItemFromCaravan(caravanId, sender, material, amount)) {
            sender.sendMessage(TranslationUtil.translate("player.items.took", String.valueOf(amount), material.name().toLowerCase().replace('_', ' '), caravanId));
        } else {
            sender.sendMessage(TranslationUtil.translate("player.items.take.failed.header"));
            sender.sendMessage(TranslationUtil.translate("player.items.take.failed.1"));
            sender.sendMessage(TranslationUtil.translate("player.items.take.failed.2"));
            sender.sendMessage(TranslationUtil.translate("player.items.take.failed.3"));
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
        
        sender.sendMessage(TranslationUtil.translate("player.transfers.header"));
        
        if (playerTransfers.isEmpty() && deliveredTransfers.isEmpty()) {
            sender.sendMessage(TranslationUtil.translate("player.transfers.none"));
            return;
        }
        
        // Show active transfers
        int activeCount = 0;
        for (ResourceTransfer transfer : playerTransfers) {
            if (transfer.getStatus() != ResourceTransfer.TransferStatus.DELIVERED) {
                if (activeCount == 0) {
                    sender.sendMessage(TranslationUtil.translate("player.transfers.active.header"));
                }
                activeCount++;
                
                String status = getStatusColor(transfer.getStatus()) + transfer.getStatus().name();
                String timeLeft = transfer.getRemainingTime() > 0 ? 
                        " §7(§f" + formatTime(transfer.getRemainingTime()) + " remaining§7)" : " §a(Ready)";
                
                sender.sendMessage(TranslationUtil.translate("player.transfers.active.format", String.valueOf(activeCount), transfer.getSourceCaravanId(), transfer.getDestinationCaravanId(), formatTime(transfer.getRemainingTime())));
            }
        }
        
        // Show delivered transfers
        if (!deliveredTransfers.isEmpty()) {
            sender.sendMessage(TranslationUtil.translate("player.transfers.delivered.header", String.valueOf(deliveredTransfers.size())));
            for (int i = 0; i < Math.min(deliveredTransfers.size(), 5); i++) {
                ResourceTransfer transfer = deliveredTransfers.get(i);
                sender.sendMessage(TranslationUtil.translate("player.transfers.delivered.format", String.valueOf(i + 1), transfer.getSourceCaravanId(), transfer.getDestinationCaravanId()));
            }
            
            if (deliveredTransfers.size() > 5) {
                sender.sendMessage(TranslationUtil.translate("player.transfers.more", String.valueOf(deliveredTransfers.size() - 5)));
            }
            
            sender.sendMessage(TranslationUtil.translate("player.transfers.gui.hint"));
        }
    }
    
    @Execute(name = "claim")
    public void claimTransfer(@Context Player sender, @Arg("transferId") String transferId) {
        if (caravanManager.claimTransferToInventory(transferId, sender)) {
            sender.sendMessage(TranslationUtil.translate("player.transfer.claimed"));
        } else {
            sender.sendMessage(TranslationUtil.translate("player.transfer.claim.failed.header"));
            sender.sendMessage(TranslationUtil.translate("player.transfer.claim.failed.1"));
            sender.sendMessage(TranslationUtil.translate("player.transfer.claim.failed.2"));
            sender.sendMessage(TranslationUtil.translate("player.transfer.claim.failed.3"));
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
            return TranslationUtil.translate("time.ready");
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
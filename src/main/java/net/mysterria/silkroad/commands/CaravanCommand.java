package net.mysterria.silkroad.commands;

import dev.rollczi.litecommands.annotations.argument.Arg;
import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import dev.rollczi.litecommands.annotations.permission.Permission;
import net.mysterria.silkroad.SilkRoad;
import net.mysterria.silkroad.domain.caravan.manager.CaravanManager;
import net.mysterria.silkroad.domain.caravan.model.Caravan;
import net.mysterria.silkroad.domain.caravan.model.ResourceTransfer;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

@Command(name = "silkroad", aliases = "sr")
@Permission("silkroad.caravan.admin")
public class CaravanCommand {
    
    private final CaravanManager caravanManager;
    
    public CaravanCommand(SilkRoad plugin) {
        this.caravanManager = plugin.getCaravanManager();
    }
    
    @Execute(name = "wand")
    public void giveWand(@Context Player sender) {
        ItemStack wand = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = wand.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6Caravan Territory Wand");
            meta.setLore(List.of(
                "§7Right-click: §aSelect current chunk",
                "§7Left-click: §cDeselect current chunk",
                "§7Use §e/sr create <name> §7to create caravan from selected chunks"
            ));
            meta.setCustomModelData(12345);
            wand.setItemMeta(meta);
        }
        
        sender.getInventory().addItem(wand);
        sender.sendMessage("§aCaravan wand given! Use it to select chunks, then /sr create <name>.");
    }
    
    @Execute(name = "create")
    public void createCaravan(@Context Player sender, @Arg("name") String name) {
        String id = name.toLowerCase().replaceAll("[^a-z0-9_-]", "");
        if (id.isEmpty()) {
            sender.sendMessage("§cInvalid caravan name!");
            return;
        }
        
        if (caravanManager.getCaravan(id).isPresent()) {
            sender.sendMessage("§cA caravan with that name already exists!");
            return;
        }
        
        var selection = caravanManager.getSelection(sender.getUniqueId());
        if (selection.isEmpty()) {
            sender.sendMessage("§cYou have no selected chunks! §7Use the wand to select chunks first.");
            return;
        }
        
        Caravan caravan = caravanManager.createCaravan(id, name, sender.getLocation(), new java.util.HashSet<>(selection));
        if (caravan != null) {
            String message = "§aCreated caravan: §d" + name + "§a with §e" + selection.size() + " §achunk(s).";
            sender.sendMessage(message);
            caravanManager.clearSelection(sender.getUniqueId());
        } else {
            sender.sendMessage("§cFailed to create caravan!");
        }
    }
    
    @Execute(name = "remove")
    public void removeCaravan(@Context CommandSender sender, @Arg("name") String id) {
        if (caravanManager.removeCaravan(id)) {
            String message = "§aRemoved caravan: §d" + id;
            sender.sendMessage(message);
        } else {
            sender.sendMessage("§cCaravan not found!");
        }
    }
    
    @Execute(name = "list")
    public void listCaravans(@Context CommandSender sender) {
        var caravans = caravanManager.getAllCaravans();
        if (caravans.isEmpty()) {
            sender.sendMessage("§7No caravans found.");
            return;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("§aCaravans (§d").append(caravans.size()).append("§a):\n");
        for (Caravan caravan : caravans) {
            sb.append("§7- §d").append(caravan.getName())
              .append(" §7(").append(caravan.getId()).append(")\n");
        }
        
        sender.sendMessage(sb.toString());
    }
    
    @Execute(name = "info")
    public void caravanInfo(@Context CommandSender sender, @Arg("id") String id) {
        var caravanOpt = caravanManager.getCaravan(id);
        if (caravanOpt.isEmpty()) {
            sender.sendMessage("§cCaravan not found!");
            return;
        }
        
        Caravan caravan = caravanOpt.get();
        StringBuilder sb = new StringBuilder();
        sb.append("§a=== Caravan Info ===\n");
        sb.append("§7Name: §d").append(caravan.getName()).append("\n");
        sb.append("§7ID: §f").append(caravan.getId()).append("\n");
        sb.append("§7Location: §f").append(String.format("%.1f, %.1f, %.1f in %s",
                caravan.getLocation().getX(),
                caravan.getLocation().getY(),
                caravan.getLocation().getZ(),
                caravan.getLocation().getWorld().getName())).append("\n");
        sb.append("§7Resources: ");
        if (caravan.getInventory().isEmpty()) {
            sb.append("§fNone");
        } else {
            sb.append("\n");
            for (var entry : caravan.getInventory().entrySet()) {
                sb.append("  §7- §d").append(entry.getKey().name())
                  .append("§7: §f").append(entry.getValue()).append("\n");
            }
        }
        sender.sendMessage(sb.toString());
    }
    
    @Execute(name = "transfers")
    public void listTransfers(@Context Player sender) {
        List<ResourceTransfer> transfers = caravanManager.getPlayerTransfers(sender.getUniqueId());
        
        if (transfers.isEmpty()) {
            sender.sendMessage("§7You have no active transfers.");
            return;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("§aYour Transfers (§d").append(transfers.size()).append("§a):\n");
        for (ResourceTransfer transfer : transfers) {
            String timeRemaining = formatTime(transfer.getRemainingTime());
            sb.append("§7- §d").append(transfer.getSourceCaravanId())
              .append(" §7→ §d").append(transfer.getDestinationCaravanId())
              .append(" §7(§f").append(timeRemaining).append("§7)")
              .append("\n");
        }
        
        sender.sendMessage(sb.toString());
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
}
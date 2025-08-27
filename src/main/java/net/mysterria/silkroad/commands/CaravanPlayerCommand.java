package net.mysterria.silkroad.commands;

import dev.rollczi.litecommands.annotations.argument.Arg;
import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import net.mysterria.silkroad.SilkRoad;
import net.mysterria.silkroad.domain.caravan.manager.CaravanManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;

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
    
    @Execute(name = "add")
    public void addItem(@Context Player sender, @Arg("caravan") String caravanId, 
                       @Arg("material") Material material, @Arg("amount") int amount) {
        
        if (amount <= 0) {
            sender.sendMessage("§cAmount must be greater than 0.");
            return;
        }
        
        if (amount > 64000) {
            sender.sendMessage("§cAmount cannot exceed 64000.");
            return;
        }
        
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
    
    @Execute(name = "take")
    public void removeItem(@Context Player sender, @Arg("caravan") String caravanId, 
                          @Arg("material") Material material, @Arg("amount") int amount) {
        
        if (amount <= 0) {
            sender.sendMessage("§cAmount must be greater than 0.");
            return;
        }
        
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
}
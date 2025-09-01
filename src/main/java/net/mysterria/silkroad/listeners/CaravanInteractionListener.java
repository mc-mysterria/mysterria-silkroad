package net.mysterria.silkroad.listeners;

import net.mysterria.silkroad.domain.caravan.gui.CaravanMainGUI;
import net.mysterria.silkroad.domain.caravan.manager.CaravanManager;
import net.mysterria.silkroad.domain.caravan.model.Caravan;
import net.mysterria.silkroad.utils.TranslationUtil;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.List;

public class CaravanInteractionListener implements Listener {
    
    private final CaravanManager caravanManager;
    
    public CaravanInteractionListener(CaravanManager caravanManager) {
        this.caravanManager = caravanManager;
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Process only main-hand interactions to avoid duplicate triggers
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        
        Player player = event.getPlayer();
        
        if (event.getClickedBlock() == null) {
            return;
        }
        
        if (!player.isSneaking()) {
            return;
        }
        
        List<Caravan> nearbyCaravans = caravanManager.getCaravansInRange(event.getClickedBlock().getLocation(), 5.0);
        
        if (nearbyCaravans.isEmpty()) {
            return;
        }
        
        event.setCancelled(true);
        
        Caravan caravan = nearbyCaravans.get(0);
        
        if (event.getAction().isRightClick()) {
            new CaravanMainGUI(caravanManager, player, caravan).open();
        } else {
            player.sendMessage(TranslationUtil.translatable("caravan.interaction.info", NamedTextColor.GRAY, caravan.getName()));
        }
    }
}
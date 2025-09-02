package net.mysterria.silkroad.listeners;

import net.mysterria.silkroad.domain.caravan.manager.CaravanManager;
import net.mysterria.silkroad.domain.caravan.model.Caravan;
import net.mysterria.silkroad.utils.HuskTownsIntegration;
import net.william278.husktowns.events.MemberJoinEvent;
import net.william278.husktowns.events.MemberLeaveEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Listener for HuskTowns town membership events to automatically update caravan member lists
 */
public class TownMembershipListener implements Listener {
    
    private final CaravanManager caravanManager;
    private final Logger logger;
    
    public TownMembershipListener(CaravanManager caravanManager) {
        this.caravanManager = caravanManager;
        this.logger = Logger.getLogger("SilkRoad");
    }
    
    @EventHandler
    public void onMemberJoin(MemberJoinEvent event) {
        if (!HuskTownsIntegration.isAvailable()) {
            return;
        }
        
        try {
            int townId = event.getTown().getId();
            UUID playerUuid = event.getUser().getUuid();
            String playerName = event.getUser().getUsername();
            String townName = event.getTown().getName();
            
            logger.info("Player " + playerName + " joined town " + townName + " (ID: " + townId + ")");
            
            // Find caravan belonging to this town
            Caravan townCaravan = findCaravanByTownId(townId);
            if (townCaravan != null) {
                // Add player to caravan members
                townCaravan.addMember(playerUuid);
                // Save the updated caravan
                caravanManager.saveCaravan(townCaravan);
                
                logger.info("Added player " + playerName + " to caravan " + townCaravan.getName() + 
                          " (town " + townName + ")");
            }
            
        } catch (Exception e) {
            logger.warning("Error handling town member join event: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @EventHandler
    public void onMemberLeave(MemberLeaveEvent event) {
        if (!HuskTownsIntegration.isAvailable()) {
            return;
        }
        
        try {
            int townId = event.getTown().getId();
            UUID playerUuid = event.getUser().getUuid();
            String playerName = event.getUser().getUsername();
            String townName = event.getTown().getName();
            
            logger.info("Player " + playerName + " left town " + townName + " (ID: " + townId + ")");
            
            // Find caravan belonging to this town
            Caravan townCaravan = findCaravanByTownId(townId);
            if (townCaravan != null) {
                // Remove player from caravan members
                townCaravan.removeMember(playerUuid);
                // Save the updated caravan
                caravanManager.saveCaravan(townCaravan);
                
                logger.info("Removed player " + playerName + " from caravan " + townCaravan.getName() + 
                          " (town " + townName + ")");
            }
            
        } catch (Exception e) {
            logger.warning("Error handling town member leave event: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Find a caravan that belongs to the specified town ID
     * @param townId The town ID to search for
     * @return The caravan belonging to the town, or null if not found
     */
    private Caravan findCaravanByTownId(int townId) {
        return caravanManager.getAllCaravans().stream()
                .filter(caravan -> caravan.getOwningTownId() == townId)
                .findFirst()
                .orElse(null);
    }
}
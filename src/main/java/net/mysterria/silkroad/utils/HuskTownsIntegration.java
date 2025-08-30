package net.mysterria.silkroad.utils;

import net.william278.husktowns.api.HuskTownsAPI;
import net.william278.husktowns.claim.Position;
import net.william278.husktowns.claim.TownClaim;
import net.william278.husktowns.town.Town;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Integration utility for HuskTowns API functionality
 * 
 * Provides town validation for caravan territories by checking if chunks are claimed
 * and ensuring all chunks belong to the same town. Includes proper error handling
 * and graceful fallback when HuskTowns is not available.
 */
public class HuskTownsIntegration {
    
    private static final Logger logger = Logger.getLogger("SilkRoad");
    private static HuskTownsAPI huskTownsAPI;
    
    static {
        try {
            if (Bukkit.getPluginManager().getPlugin("HuskTowns") != null) {
                huskTownsAPI = HuskTownsAPI.getInstance();
                logger.info("HuskTowns integration enabled");
            } else {
                logger.info("HuskTowns not found - town validation disabled");
            }
        } catch (Exception e) {
            logger.warning("Failed to initialize HuskTowns integration: " + e.getMessage());
            huskTownsAPI = null;
        }
    }
    
    /**
     * Check if HuskTowns integration is available
     */
    public static boolean isAvailable() {
        return huskTownsAPI != null;
    }
    
    /**
     * Validates that all chunks belong to the same town
     * @param territoryChunks Set of chunk keys in format "world:x:z"
     * @return ValidationResult with success status and details
     */
    public static ValidationResult validateCaravanTerritory(Set<String> territoryChunks) {
        if (!isAvailable()) {
            return ValidationResult.success("HuskTowns not available - validation skipped");
        }
        
        if (territoryChunks.isEmpty()) {
            return ValidationResult.error("No territory chunks provided");
        }
        
        String firstTownName = null;
        int townId = -1;
        
        try {
            for (String chunkKey : territoryChunks) {
                String[] parts = chunkKey.split(":");
                if (parts.length != 3) {
                    return ValidationResult.error("Invalid chunk format: " + chunkKey);
                }
                
                String worldName = parts[0];
                int x = Integer.parseInt(parts[1]);
                int z = Integer.parseInt(parts[2]);
                
                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    return ValidationResult.error("World not found: " + worldName);
                }

                Chunk chunk = world.getChunkAt(x, z);
                
                // Check if chunk is claimed using HuskTowns API
                boolean isClaimed = isChunkClaimed(chunk);
                if (!isClaimed) {
                    return ValidationResult.error("Chunk " + chunkKey + " is not claimed by any town");
                }
                
                String townName = getChunkTownName(chunk);
                int currentTownId = getChunkTownId(chunk);
                
                if (townName == null) {
                    return ValidationResult.error("Unable to determine town for chunk " + chunkKey);
                }
                
                if (firstTownName == null) {
                    firstTownName = townName;
                    townId = currentTownId;
                } else if (!firstTownName.equals(townName)) {
                    return ValidationResult.error("Chunks belong to different towns: '" + firstTownName + "' and '" + townName + "'");
                }
            }
            
            return ValidationResult.success("All chunks belong to town: " + firstTownName, townId, firstTownName);
            
        } catch (Exception e) {
            logger.warning("Error validating caravan territory with HuskTowns: " + e.getMessage());
            return ValidationResult.error("HuskTowns integration error: " + e.getMessage());
        }
    }
    
    /**
     * Check if a chunk is claimed by any town
     */
    private static boolean isChunkClaimed(Chunk chunk) {
        try {
            if (huskTownsAPI == null) return false;
            HuskTownsAPI huskTowns = HuskTownsAPI.getInstance();
            // Convert chunk center to Location for HuskTowns API
            Location chunkCenter = new Location(chunk.getWorld(),
                chunk.getX() * 16 + 8, 64, chunk.getZ() * 16 + 8);
            net.william278.husktowns.claim.World hw = huskTownsAPI.getWorld(chunkCenter.getWorld().getName());
            if (hw == null) {
                return false;
            }
            Position position = Position.at(chunkCenter.getX(), chunkCenter.getY(), chunkCenter.getZ(), hw);
            Optional<TownClaim> claim = huskTowns.getClaimAt(position);

            return claim.isPresent();
        } catch (Exception e) {
            logger.warning("Error checking if chunk is claimed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get the town name for a claimed chunk
     */
    private static String getChunkTownName(Chunk chunk) {
        try {
            if (huskTownsAPI == null) return null;
            // Convert chunk center to Location for HuskTowns API
            Location chunkCenter = new Location(chunk.getWorld(),
                chunk.getX() * 16 + 8, 64, chunk.getZ() * 16 + 8);
            net.william278.husktowns.claim.World hw = huskTownsAPI.getWorld(chunkCenter.getWorld().getName());
            if (hw == null) {
                return null;
            }
            Position position = Position.at(chunkCenter.getX(), chunkCenter.getY(), chunkCenter.getZ(), hw);
            Optional<TownClaim> claim = huskTownsAPI.getClaimAt(position);

            if (claim.isPresent()) {
                return claim.get().town().getName();
            }
            return null;
        } catch (Exception e) {
            logger.warning("Error getting chunk town name: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get the town ID for a claimed chunk
     */
    private static int getChunkTownId(Chunk chunk) {
        try {
            if (huskTownsAPI == null) return -1;

            // Convert chunk center to Location for HuskTowns API
            Location chunkCenter = new Location(chunk.getWorld(),
                chunk.getX() * 16 + 8, 64, chunk.getZ() * 16 + 8);
            net.william278.husktowns.claim.World hw = huskTownsAPI.getWorld(chunkCenter.getWorld().getName());
            if (hw == null) {
                return -1;
            }
            Position position = Position.at(chunkCenter.getX(), chunkCenter.getY(), chunkCenter.getZ(), hw);
            Optional<TownClaim> claim = huskTownsAPI.getClaimAt(position);

            if (claim.isPresent()) {
                return claim.get().town().getId();
            }
            return -1;
        } catch (Exception e) {
            logger.warning("Error getting chunk town ID: " + e.getMessage());
            return -1;
        }
    }
    
    /**
     * Check if a town already has a caravan
     * @param townId The town ID to check
     * @param existingCaravans Set of existing caravan territory chunks to check against
     * @return true if town already has a caravan
     */
    public static boolean townHasCaravan(int townId, Set<Set<String>> existingCaravans) {
        if (!isAvailable()) {
            return false;
        }
        
        for (Set<String> caravanChunks : existingCaravans) {
            ValidationResult result = validateCaravanTerritory(caravanChunks);
            if (result.isSuccess() && result.getTownId() == townId) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Gets all member UUIDs for a given town ID
     * @param townId The town ID to get members for
     * @return Set of member UUIDs, empty if no members or if HuskTowns unavailable
     */
    public static Set<UUID> getTownMemberUUIDs(int townId) {
        Set<UUID> memberUUIDs = new HashSet<>();
        
        if (!isAvailable()) {
            logger.info("HuskTowns not available - cannot get town members");
            return memberUUIDs;
        }
        
        try {
            // Get town by ID
            Optional<Town> townOptional = huskTownsAPI.getTown(townId);
            
            if (townOptional.isPresent()) {
                Town town = townOptional.get();
                
                // Get all members of the town using Town API (HuskTowns 3.1.4)
                java.util.Map<java.util.UUID, java.lang.Integer> members = town.getMembers();
                memberUUIDs.addAll(members.keySet());
                logger.info("Retrieved " + memberUUIDs.size() + " members for town " + town.getName());
            } else {
                logger.warning("Town with ID " + townId + " not found");
            }
            
        } catch (Exception e) {
            logger.warning("Error getting town members for town ID " + townId + ": " + e.getMessage());
        }
        
        return memberUUIDs;
    }
    
    /**
     * Result of territory validation
     */
    public static class ValidationResult {
        private final boolean success;
        private final String message;
        private final int townId;
        private final String townName;
        
        private ValidationResult(boolean success, String message, int townId, String townName) {
            this.success = success;
            this.message = message;
            this.townId = townId;
            this.townName = townName;
        }
        
        public static ValidationResult success(String message) {
            return new ValidationResult(true, message, -1, null);
        }
        
        public static ValidationResult success(String message, int townId, String townName) {
            return new ValidationResult(true, message, townId, townName);
        }
        
        public static ValidationResult error(String message) {
            return new ValidationResult(false, message, -1, null);
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
        
        public int getTownId() {
            return townId;
        }
        
        public String getTownName() {
            return townName;
        }
    }
}
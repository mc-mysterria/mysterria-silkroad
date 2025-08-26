package net.mysterria.silkroad.domain.caravan.model;

import net.mysterria.silkroad.SilkRoad;
import net.mysterria.silkroad.utils.YamlConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.util.*;
import java.util.UUID;

public class CaravanStorage {
    
    private final File caravanDir;
    private final File transferDir;
    
    public CaravanStorage() {
        File dataFolder = SilkRoad.getInstance().getDataFolder();
        this.caravanDir = new File(dataFolder, "caravans");
        this.transferDir = new File(dataFolder, "transfers");
        
        if (!caravanDir.exists()) {
            caravanDir.mkdirs();
        }
        if (!transferDir.exists()) {
            transferDir.mkdirs();
        }
    }
    
    public void saveCaravan(Caravan caravan) {
        File file = new File(caravanDir, caravan.getId() + ".yml");
        YamlConfiguration config = new YamlConfiguration(file);
        
        config.set("id", caravan.getId());
        config.set("name", caravan.getName());
        config.set("location.world", caravan.getLocation().getWorld().getName());
        config.set("location.x", caravan.getLocation().getX());
        config.set("location.y", caravan.getLocation().getY());
        config.set("location.z", caravan.getLocation().getZ());
        config.set("location.yaw", caravan.getLocation().getYaw());
        config.set("location.pitch", caravan.getLocation().getPitch());
        config.set("createdAt", caravan.getCreatedAt());
        config.set("active", caravan.isActive());
        
        Map<String, Object> inventoryMap = new HashMap<>();
        for (Map.Entry<Material, Integer> entry : caravan.getInventory().entrySet()) {
            inventoryMap.put(entry.getKey().name(), entry.getValue());
        }
        config.set("inventory", inventoryMap);
        // Save territory as list of strings
        config.set("territory", new ArrayList<>(caravan.getTerritoryChunks()));
        
        // Save owners and members
        List<String> ownersList = new ArrayList<>();
        for (UUID ownerId : caravan.getOwners()) {
            ownersList.add(ownerId.toString());
        }
        config.set("owners", ownersList);
        
        List<String> membersList = new ArrayList<>();
        for (UUID memberId : caravan.getMembers()) {
            membersList.add(memberId.toString());
        }
        config.set("members", membersList);
        
        try {
            config.save();
        } catch (Exception e) {
            SilkRoad.getInstance().getLogger().severe("Failed to save caravan " + caravan.getId() + ": " + e.getMessage());
        }
    }
    
    public Caravan loadCaravan(String id) {
        File file = new File(caravanDir, id + ".yml");
        if (!file.exists()) {
            return null;
        }
        
        try {
            YamlConfiguration config = new YamlConfiguration(file);
            
            String name = asString(config.get("name"));
            String worldName = asString(config.get("location.world"));
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                SilkRoad.getInstance().getLogger().warning("World not found for caravan " + id);
                return null;
            }
            
            Location location = new Location(
                world,
                asDouble(config.get("location.x")),
                asDouble(config.get("location.y")),
                asDouble(config.get("location.z")),
                (float) asDouble(config.get("location.yaw")),
                (float) asDouble(config.get("location.pitch"))
            );
            
            Caravan caravan = new Caravan(id, name, location);
            caravan.setCreatedAt(asLong(config.get("createdAt")));
            caravan.setActive(asBoolean(config.get("active"), true));
            
            Map<String, Object> inventorySection = config.getSection("inventory");
            if (inventorySection != null) {
                for (Map.Entry<String, Object> entry : inventorySection.entrySet()) {
                    String key = entry.getKey();
                    try {
                        Material material = Material.valueOf(key);
                        int amount = asInt(entry.getValue());
                        caravan.getInventory().put(material, amount);
                    } catch (IllegalArgumentException e) {
                        SilkRoad.getInstance().getLogger().warning("Invalid material in caravan inventory: " + key);
                    }
                }
            }

            // Load territory list
            Object territoryObj = config.get("territory");
            if (territoryObj instanceof java.util.List<?> territoryList) {
                for (Object o : territoryList) {
                    if (o != null) {
                        caravan.getTerritoryChunks().add(String.valueOf(o));
                    }
                }
            }
            
            // Load owners
            Object ownersObj = config.get("owners");
            if (ownersObj instanceof java.util.List<?> ownersList) {
                for (Object o : ownersList) {
                    if (o != null) {
                        try {
                            UUID ownerId = UUID.fromString(String.valueOf(o));
                            caravan.addOwner(ownerId);
                        } catch (IllegalArgumentException e) {
                            SilkRoad.getInstance().getLogger().warning("Invalid UUID in caravan owners: " + o);
                        }
                    }
                }
            }
            
            // Load members
            Object membersObj = config.get("members");
            if (membersObj instanceof java.util.List<?> membersList) {
                for (Object o : membersList) {
                    if (o != null) {
                        try {
                            UUID memberId = UUID.fromString(String.valueOf(o));
                            caravan.addMember(memberId);
                        } catch (IllegalArgumentException e) {
                            SilkRoad.getInstance().getLogger().warning("Invalid UUID in caravan members: " + o);
                        }
                    }
                }
            }
            
            return caravan;
        } catch (Exception e) {
            SilkRoad.getInstance().getLogger().severe("Failed to load caravan " + id + ": " + e.getMessage());
            return null;
        }
    }
    
    public void deleteCaravan(String id) {
        File file = new File(caravanDir, id + ".yml");
        if (file.exists()) {
            file.delete();
        }
    }
    
    public List<String> getAllCaravanIds() {
        List<String> ids = new ArrayList<>();
        File[] files = caravanDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                ids.add(name.substring(0, name.length() - 4));
            }
        }
        return ids;
    }
    
    public void saveTransfer(ResourceTransfer transfer) {
        File file = new File(transferDir, transfer.getId() + ".yml");
        YamlConfiguration config = new YamlConfiguration(file);
        
        config.set("id", transfer.getId());
        config.set("sourceCaravanId", transfer.getSourceCaravanId());
        config.set("destinationCaravanId", transfer.getDestinationCaravanId());
        config.set("playerId", transfer.getPlayerId().toString());
        config.set("playerName", transfer.getPlayerName());
        config.set("createdAt", transfer.getCreatedAt());
        config.set("deliveryTime", transfer.getDeliveryTime());
        config.set("distance", transfer.getDistance());
        config.set("cost", transfer.getCost());
        config.set("status", transfer.getStatus().name());
        
        Map<String, Object> resourcesMap = new HashMap<>();
        for (Map.Entry<Material, Integer> entry : transfer.getResources().entrySet()) {
            resourcesMap.put(entry.getKey().name(), entry.getValue());
        }
        config.set("resources", resourcesMap);
        
        try {
            config.save();
        } catch (Exception e) {
            SilkRoad.getInstance().getLogger().severe("Failed to save transfer " + transfer.getId() + ": " + e.getMessage());
        }
    }
    
    public ResourceTransfer loadTransfer(String id) {
        File file = new File(transferDir, id + ".yml");
        if (!file.exists()) {
            return null;
        }
        
        try {
            YamlConfiguration config = new YamlConfiguration(file);
            
            String sourceCaravanId = asString(config.get("sourceCaravanId"));
            String destinationCaravanId = asString(config.get("destinationCaravanId"));
            UUID playerId = UUID.fromString(asString(config.get("playerId")));
            String playerName = asString(config.get("playerName"));
            long createdAt = asLong(config.get("createdAt"));
            long deliveryTime = asLong(config.get("deliveryTime"));
            double distance = asDouble(config.get("distance"));
            int cost = asInt(config.get("cost"));
            ResourceTransfer.TransferStatus status = ResourceTransfer.TransferStatus.valueOf(asString(config.get("status")));
            
            Map<Material, Integer> resources = new HashMap<>();
            Map<String, Object> resourcesSection = config.getSection("resources");
            if (resourcesSection != null) {
                for (Map.Entry<String, Object> entry : resourcesSection.entrySet()) {
                    String key = entry.getKey();
                    try {
                        Material material = Material.valueOf(key);
                        int amount = asInt(entry.getValue());
                        resources.put(material, amount);
                    } catch (IllegalArgumentException e) {
                        SilkRoad.getInstance().getLogger().warning("Invalid material in transfer: " + key);
                    }
                }
            }
            
            ResourceTransfer transfer = new ResourceTransfer(id, sourceCaravanId, destinationCaravanId,
                    playerId, playerName, resources, createdAt, deliveryTime, distance, cost);
            transfer.setStatus(status);
            
            return transfer;
        } catch (Exception e) {
            SilkRoad.getInstance().getLogger().severe("Failed to load transfer " + id + ": " + e.getMessage());
            return null;
        }
    }
    
    public void deleteTransfer(String id) {
        File file = new File(transferDir, id + ".yml");
        if (file.exists()) {
            file.delete();
        }
    }
    
    public List<String> getAllTransferIds() {
        List<String> ids = new ArrayList<>();
        File[] files = transferDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                ids.add(name.substring(0, name.length() - 4));
            }
        }
        return ids;
    }

    // Helper conversion methods for our simple YamlConfiguration wrapper
    private static String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static double asDouble(Object o) {
        if (o instanceof Number) return ((Number) o).doubleValue();
        if (o == null) return 0.0d;
        try {
            return Double.parseDouble(o.toString());
        } catch (NumberFormatException e) {
            return 0.0d;
        }
    }

    private static long asLong(Object o) {
        if (o instanceof Number) return ((Number) o).longValue();
        if (o == null) return 0L;
        try {
            return Long.parseLong(o.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static int asInt(Object o) {
        if (o instanceof Number) return ((Number) o).intValue();
        if (o == null) return 0;
        try {
            return Integer.parseInt(o.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static boolean asBoolean(Object o, boolean def) {
        if (o instanceof Boolean) return (Boolean) o;
        if (o == null) return def;
        String s = o.toString();
        if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("y")) return true;
        if (s.equalsIgnoreCase("false") || s.equalsIgnoreCase("no") || s.equalsIgnoreCase("n")) return false;
        if (o instanceof Number) return ((Number) o).intValue() != 0;
        return def;
    }
}
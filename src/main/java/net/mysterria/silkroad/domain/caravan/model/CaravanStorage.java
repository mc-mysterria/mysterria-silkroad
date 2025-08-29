package net.mysterria.silkroad.domain.caravan.model;

import net.mysterria.silkroad.SilkRoad;
import net.mysterria.silkroad.utils.YamlConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

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
        
        // Create location map
        Map<String, Object> locationMap = new HashMap<>();
        locationMap.put("world", caravan.getLocation().getWorld().getName());
        locationMap.put("x", caravan.getLocation().getX());
        locationMap.put("y", caravan.getLocation().getY());
        locationMap.put("z", caravan.getLocation().getZ());
        locationMap.put("yaw", caravan.getLocation().getYaw());
        locationMap.put("pitch", caravan.getLocation().getPitch());
        config.set("location", locationMap);
        
        config.set("createdAt", caravan.getCreatedAt());
        config.set("active", caravan.isActive());
        
        Map<String, Object> inventoryMap = new HashMap<>();
        for (Map.Entry<Material, Integer> entry : caravan.getInventory().entrySet()) {
            inventoryMap.put(entry.getKey().name(), entry.getValue());
        }
        config.set("inventory", inventoryMap);
        
        // Save ItemStack inventory with NBT data
        List<Map<String, Object>> itemStacksList = new ArrayList<>();
        for (ItemStack itemStack : caravan.getItemInventory()) {
            if (itemStack != null && itemStack.getType() != Material.AIR) {
                itemStacksList.add(itemStack.serialize());
            }
        }
        config.set("itemInventory", itemStacksList);
        // Save territory as list of strings
        config.set("territory", new ArrayList<>(caravan.getTerritoryChunks()));
        
        // Save members (new unified system)
        List<String> membersList = new ArrayList<>();
        for (UUID memberId : caravan.getMembers()) {
            membersList.add(memberId.toString());
        }
        config.set("members", membersList);
        
        // Save empty owners list for backwards compatibility
        config.set("owners", new ArrayList<>());
        
        // Save town ownership information (HuskTowns integration)
        if (caravan.getOwningTownName() != null) {
            config.set("owningTownName", caravan.getOwningTownName());
        }
        if (caravan.getOwningTownId() != -1) {
            config.set("owningTownId", caravan.getOwningTownId());
        }
        
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
            
            // Load location - support both old dotted notation and new nested format
            String worldName;
            double x, y, z;
            float yaw, pitch;
            
            // Try new nested format first
            Map<String, Object> locationMap = config.getSection("location");
            if (locationMap != null && !locationMap.isEmpty()) {
                worldName = asString(locationMap.get("world"));
                x = asDouble(locationMap.get("x"));
                y = asDouble(locationMap.get("y"));
                z = asDouble(locationMap.get("z"));
                yaw = (float) asDouble(locationMap.get("yaw"));
                pitch = (float) asDouble(locationMap.get("pitch"));
            } else {
                // Fall back to old dotted notation
                worldName = asString(config.get("location.world"));
                x = asDouble(config.get("location.x"));
                y = asDouble(config.get("location.y"));
                z = asDouble(config.get("location.z"));
                yaw = (float) asDouble(config.get("location.yaw"));
                pitch = (float) asDouble(config.get("location.pitch"));
            }
            
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                SilkRoad.getInstance().getLogger().warning("World not found for caravan " + id + ": " + worldName);
                return null;
            }
            
            Location location = new Location(world, x, y, z, yaw, pitch);
            
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
            
            // Load ItemStack inventory with NBT data
            Object itemInventoryObj = config.get("itemInventory");
            if (itemInventoryObj instanceof java.util.List<?> itemInventoryList) {
                for (Object o : itemInventoryList) {
                    if (o instanceof Map<?, ?> itemData) {
                        try {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> itemMap = (Map<String, Object>) itemData;
                            ItemStack itemStack = ItemStack.deserialize(itemMap);
                            caravan.getItemInventory().add(itemStack);
                        } catch (Exception e) {
                            SilkRoad.getInstance().getLogger().warning("Failed to deserialize ItemStack: " + e.getMessage());
                        }
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
            
            // Load members from new unified system
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
            
            // MIGRATION: Load old owners and convert them to members (backwards compatibility)
            Object ownersObj = config.get("owners");
            if (ownersObj instanceof java.util.List<?> ownersList) {
                for (Object o : ownersList) {
                    if (o != null) {
                        try {
                            UUID ownerId = UUID.fromString(String.valueOf(o));
                            caravan.addMember(ownerId); // Add as member instead of owner
                            SilkRoad.getInstance().getLogger().info("Migrated owner to member for caravan " + id + ": " + ownerId);
                        } catch (IllegalArgumentException e) {
                            SilkRoad.getInstance().getLogger().warning("Invalid UUID in caravan owners: " + o);
                        }
                    }
                }
            }
            
            // Load town ownership information (HuskTowns integration)
            String owningTownName = asString(config.get("owningTownName"));
            if (owningTownName != null && !owningTownName.isEmpty()) {
                caravan.setOwningTownName(owningTownName);
            }
            
            int owningTownId = asInt(config.get("owningTownId"));
            if (owningTownId != 0) { // 0 is default when config value is missing
                caravan.setOwningTownId(owningTownId);
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
        
        // Save legacy Material-based resources (for backwards compatibility)
        Map<String, Object> resourcesMap = new HashMap<>();
        for (Map.Entry<Material, Integer> entry : transfer.getResources().entrySet()) {
            resourcesMap.put(entry.getKey().name(), entry.getValue());
        }
        config.set("resources", resourcesMap);
        
        // Save ItemStack-based resources (preserves NBT data)
        List<Map<String, Object>> itemResourcesList = new ArrayList<>();
        for (ItemStack item : transfer.getItemResources()) {
            Map<String, Object> itemMap = item.serialize();
            itemResourcesList.add(itemMap);
        }
        config.set("itemResources", itemResourcesList);
        
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
            
            // Load ItemStack-based resources (preserves NBT data)
            List<ItemStack> itemResources = new ArrayList<>();
            Object itemResourcesObj = config.get("itemResources");
            if (itemResourcesObj instanceof java.util.List<?> itemResourcesList) {
                for (Object o : itemResourcesList) {
                    if (o instanceof Map<?, ?> itemData) {
                        try {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> itemMap = (Map<String, Object>) itemData;
                            ItemStack itemStack = ItemStack.deserialize(itemMap);
                            itemResources.add(itemStack);
                        } catch (Exception e) {
                            SilkRoad.getInstance().getLogger().warning("Failed to deserialize ItemStack in transfer: " + e.getMessage());
                        }
                    }
                }
            }
            
            // Create transfer with appropriate constructor based on what data we have
            ResourceTransfer transfer;
            if (!itemResources.isEmpty()) {
                // Use ItemStack-based constructor (preferred for new transfers)
                transfer = new ResourceTransfer(id, sourceCaravanId, destinationCaravanId,
                        playerId, playerName, itemResources, createdAt, deliveryTime, distance, cost);
            } else {
                // Use legacy Material-based constructor (for backwards compatibility)
                transfer = new ResourceTransfer(id, sourceCaravanId, destinationCaravanId,
                        playerId, playerName, resources, createdAt, deliveryTime, distance, cost);
            }
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
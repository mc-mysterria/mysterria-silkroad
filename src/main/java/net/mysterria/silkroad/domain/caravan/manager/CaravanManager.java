package net.mysterria.silkroad.domain.caravan.manager;

import net.mysterria.silkroad.SilkRoad;
import net.mysterria.silkroad.domain.caravan.model.Caravan;
import net.mysterria.silkroad.domain.caravan.model.CaravanStorage;
import net.mysterria.silkroad.domain.caravan.model.ResourceTransfer;
import net.mysterria.silkroad.utils.ShardUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.logging.Logger;

public class CaravanManager {
    
    private final Map<String, Caravan> caravans = new HashMap<>();
    private final Map<String, ResourceTransfer> activeTransfers = new HashMap<>();
    private final CaravanStorage storage;
    private final Logger logger;
    private BukkitRunnable transferProcessor;
    // Admin temporary chunk selections: UUID -> set of keys "world:x:z"
    private final Map<UUID, Set<String>> selections = new HashMap<>();
    
    public CaravanManager() {
        this.logger = SilkRoad.getInstance().getLogger();
        this.storage = new CaravanStorage();
        loadCaravans();
        loadTransfers();
        startTransferProcessor();
    }
    
    public Caravan createCaravan(String id, String name, Location location) {
        if (caravans.containsKey(id)) {
            return null;
        }
        
        Caravan caravan = new Caravan(id, name, location);
        caravans.put(id, caravan);
        storage.saveCaravan(caravan);
        
        logger.info("Created caravan: " + id + " at " + locationToString(location));
        return caravan;
    }

    public Caravan createCaravan(String id, String name, Location location, Set<String> territoryChunks) {
        if (caravans.containsKey(id)) {
            return null;
        }
        Caravan caravan = new Caravan(id, name, location);
        if (territoryChunks != null) {
            caravan.getTerritoryChunks().addAll(territoryChunks);
        }
        caravans.put(id, caravan);
        storage.saveCaravan(caravan);
        logger.info("Created caravan: " + id + " with territory chunks (" + (territoryChunks == null ? 0 : territoryChunks.size()) + ") at " + locationToString(location));
        return caravan;
    }
    
    public boolean removeCaravan(String id) {
        Caravan caravan = caravans.remove(id);
        if (caravan != null) {
            storage.deleteCaravan(id);
            logger.info("Removed caravan: " + id);
            return true;
        }
        return false;
    }
    
    public Optional<Caravan> getCaravan(String id) {
        return Optional.ofNullable(caravans.get(id));
    }
    
    public Collection<Caravan> getAllCaravans() {
        return new ArrayList<>(caravans.values());
    }
    
    public List<Caravan> getCaravansInRange(Location location, double maxDistance) {
        return caravans.values().stream()
                .filter(c -> c.getLocation().getWorld().equals(location.getWorld()))
                .filter(c -> c.getLocation().distance(location) <= maxDistance)
                .sorted((a, b) -> Double.compare(a.getLocation().distance(location), b.getLocation().distance(location)))
                .toList();
    }

    // Selection handling
    public Set<String> getSelection(UUID playerId) {
        return selections.computeIfAbsent(playerId, id -> new HashSet<>());
    }

    public void clearSelection(UUID playerId) {
        selections.remove(playerId);
    }

    public boolean toggleSelect(String worldName, int chunkX, int chunkZ, UUID playerId, boolean add) {
        Set<String> sel = getSelection(playerId);
        String key = worldName + ":" + chunkX + ":" + chunkZ;
        if (add) {
            if (sel.add(key)) return true; // added
            return false; // already existed
        } else {
            return sel.remove(key);
        }
    }
    
    public ResourceTransfer createTransfer(Player player, String sourceCaravanId, String destinationCaravanId, 
                                         Map<Material, Integer> resources) {
        Caravan source = caravans.get(sourceCaravanId);
        Caravan destination = caravans.get(destinationCaravanId);
        
        if (source == null || destination == null) {
            return null;
        }
        
        // Check if source caravan has enough resources
        for (Map.Entry<Material, Integer> entry : resources.entrySet()) {
            if (source.getResourceAmount(entry.getKey()) < entry.getValue()) {
                return null;
            }
        }
        
        double distance = source.distanceTo(destination);
        int cost = calculateTransferCost(distance, resources);
        
        // Check if player has enough shards to pay for the transfer
        if (ShardUtils.getTotalPlayerShards(player) < cost) {
            return null; // Not enough shards
        }
        
        // Consume shards from player's inventory
        if (!ShardUtils.consumeShards(player, cost)) {
            return null; // Failed to consume shards
        }
        
        long deliveryTime = System.currentTimeMillis() + calculateDeliveryTime(distance);
        
        String transferId = UUID.randomUUID().toString();
        ResourceTransfer transfer = new ResourceTransfer(transferId, sourceCaravanId, destinationCaravanId, 
                player, resources, distance, cost, deliveryTime);
        
        // Remove resources from source caravan
        for (Map.Entry<Material, Integer> entry : resources.entrySet()) {
            source.removeResource(entry.getKey(), entry.getValue());
        }
        
        transfer.setStatus(ResourceTransfer.TransferStatus.IN_TRANSIT);
        activeTransfers.put(transferId, transfer);
        
        storage.saveCaravan(source);
        storage.saveTransfer(transfer);
        
        logger.info("Created transfer: " + transferId + " from " + sourceCaravanId + " to " + destinationCaravanId + " for " + cost + " shards");
        return transfer;
    }
    
    public Optional<ResourceTransfer> getTransfer(String id) {
        return Optional.ofNullable(activeTransfers.get(id));
    }
    
    public List<ResourceTransfer> getPlayerTransfers(UUID playerId) {
        return activeTransfers.values().stream()
                .filter(t -> t.getPlayerId().equals(playerId))
                .sorted((a, b) -> Long.compare(a.getDeliveryTime(), b.getDeliveryTime()))
                .toList();
    }
    
    private int calculateTransferCost(double distance, Map<Material, Integer> resources) {
        int totalItems = resources.values().stream().mapToInt(Integer::intValue).sum();
        double baseCost = distance * 0.1;
        double itemCost = totalItems * 0.5;
        return (int) Math.max(1, baseCost + itemCost);
    }
    
    private long calculateDeliveryTime(double distance) {
        double baseTime = 5 * 60 * 1000;
        double distanceTime = distance * 1000;
        return (long) (baseTime + distanceTime);
    }
    
    private void loadCaravans() {
        logger.info("Loading caravans...");
        caravans.clear();
        
        List<String> caravanIds = storage.getAllCaravanIds();
        for (String id : caravanIds) {
            Caravan caravan = storage.loadCaravan(id);
            if (caravan != null && caravan.isActive()) {
                caravans.put(id, caravan);
            }
        }
        
        logger.info("Loaded " + caravans.size() + " caravans.");
    }
    
    private void loadTransfers() {
        logger.info("Loading transfers...");
        activeTransfers.clear();
        
        List<String> transferIds = storage.getAllTransferIds();
        for (String id : transferIds) {
            ResourceTransfer transfer = storage.loadTransfer(id);
            if (transfer != null && 
                (transfer.getStatus() == ResourceTransfer.TransferStatus.IN_TRANSIT ||
                 transfer.getStatus() == ResourceTransfer.TransferStatus.PENDING)) {
                activeTransfers.put(id, transfer);
            }
        }
        
        logger.info("Loaded " + activeTransfers.size() + " active transfers.");
    }
    
    private void startTransferProcessor() {
        transferProcessor = new BukkitRunnable() {
            @Override
            public void run() {
                processTransfers();
            }
        };
        transferProcessor.runTaskTimer(SilkRoad.getInstance(), 20L, 20L);
    }
    
    private void processTransfers() {
        List<ResourceTransfer> completed = new ArrayList<>();
        
        for (ResourceTransfer transfer : activeTransfers.values()) {
            if (transfer.isReadyForDelivery()) {
                if (completeTransfer(transfer)) {
                    completed.add(transfer);
                }
            }
        }
        
        for (ResourceTransfer transfer : completed) {
            activeTransfers.remove(transfer.getId());
            storage.deleteTransfer(transfer.getId());
        }
    }
    
    private boolean completeTransfer(ResourceTransfer transfer) {
        Caravan destination = caravans.get(transfer.getDestinationCaravanId());
        if (destination == null) {
            transfer.setStatus(ResourceTransfer.TransferStatus.FAILED);
            storage.saveTransfer(transfer);
            return false;
        }
        
        for (Map.Entry<Material, Integer> entry : transfer.getResources().entrySet()) {
            destination.addResource(entry.getKey(), entry.getValue());
        }
        
        transfer.setStatus(ResourceTransfer.TransferStatus.DELIVERED);
        storage.saveCaravan(destination);
        storage.saveTransfer(transfer);
        
        Player player = SilkRoad.getInstance().getServer().getPlayer(transfer.getPlayerId());
        if (player != null && player.isOnline()) {
            player.sendMessage("§aYour caravan delivery has been completed!");
        }
        
        logger.info("Completed transfer: " + transfer.getId());
        return true;
    }
    
    public void saveCaravan(Caravan caravan) {
        storage.saveCaravan(caravan);
    }
    
    public void shutdown() {
        if (transferProcessor != null) {
            transferProcessor.cancel();
        }
        
        for (Caravan caravan : caravans.values()) {
            storage.saveCaravan(caravan);
        }
        
        for (ResourceTransfer transfer : activeTransfers.values()) {
            storage.saveTransfer(transfer);
        }
    }
    
    private String locationToString(Location location) {
        return String.format("%s(%.1f, %.1f, %.1f)", 
                location.getWorld().getName(), 
                location.getX(), 
                location.getY(), 
                location.getZ());
    }
}
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
import org.bukkit.Bukkit;

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
        Caravan caravan = caravans.get(id);
        if (caravan == null) {
            // Try to load from storage if not in cache
            caravan = storage.loadCaravan(id);
            if (caravan != null && caravan.isActive()) {
                caravans.put(id, caravan);
                logger.info("Loaded caravan from storage: " + id);
            }
        }
        return Optional.ofNullable(caravan);
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
    
    public boolean addOwner(String caravanId, String playerName) {
        Caravan caravan = caravans.get(caravanId);
        if (caravan == null) {
            return false;
        }
        
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            return false;
        }
        
        caravan.addOwner(player.getUniqueId());
        storage.saveCaravan(caravan);
        logger.info("Added owner " + playerName + " to caravan " + caravanId);
        return true;
    }
    
    public boolean addMember(String caravanId, String playerName) {
        Caravan caravan = caravans.get(caravanId);
        if (caravan == null) {
            return false;
        }
        
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            return false;
        }
        
        caravan.addMember(player.getUniqueId());
        storage.saveCaravan(caravan);
        logger.info("Added member " + playerName + " to caravan " + caravanId);
        return true;
    }
    
    public boolean removeOwner(String caravanId, String playerName) {
        Caravan caravan = caravans.get(caravanId);
        if (caravan == null) {
            return false;
        }
        
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            return false;
        }
        
        caravan.removeOwner(player.getUniqueId());
        storage.saveCaravan(caravan);
        logger.info("Removed owner " + playerName + " from caravan " + caravanId);
        return true;
    }
    
    public boolean removeMember(String caravanId, String playerName) {
        Caravan caravan = caravans.get(caravanId);
        if (caravan == null) {
            return false;
        }
        
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            return false;
        }
        
        caravan.removeMember(player.getUniqueId());
        storage.saveCaravan(caravan);
        logger.info("Removed member " + playerName + " from caravan " + caravanId);
        return true;
    }
    
    public List<Caravan> getPlayerCaravans(UUID playerId) {
        return caravans.values().stream()
                .filter(caravan -> caravan.hasAccess(playerId))
                .toList();
    }
    
    public List<Caravan> getPlayerOwnedCaravans(UUID playerId) {
        return caravans.values().stream()
                .filter(caravan -> caravan.isOwner(playerId))
                .toList();
    }
    
    public List<ResourceTransfer> getIncomingTransfers(UUID playerId) {
        return activeTransfers.values().stream()
                .filter(transfer -> {
                    Caravan destination = caravans.get(transfer.getDestinationCaravanId());
                    return destination != null && destination.hasAccess(playerId);
                })
                .sorted((a, b) -> Long.compare(a.getDeliveryTime(), b.getDeliveryTime()))
                .toList();
    }
    
    public boolean addItemToCaravan(String caravanId, Player player, Material material, int amount) {
        Caravan caravan = caravans.get(caravanId);
        if (caravan == null) {
            Optional<Caravan> loaded = getCaravan(caravanId);
            if (loaded.isPresent()) {
                caravan = loaded.get();
            } else {
                return false;
            }
        }
        
        // Check if player has permission to add items (owner or member)
        if (!caravan.hasAccess(player.getUniqueId())) {
            return false;
        }
        
        // Check if player has the items in their inventory
        if (!hasEnoughItems(player, material, amount)) {
            return false;
        }
        
        // Remove items from player inventory
        removeItemsFromPlayer(player, material, amount);
        
        // Add items to caravan inventory
        caravan.getInventory().merge(material, amount, Integer::sum);
        
        // Save changes
        storage.saveCaravan(caravan);
        
        logger.info("Player " + player.getName() + " added " + amount + " " + material + " to caravan " + caravanId);
        return true;
    }
    
    public boolean removeItemFromCaravan(String caravanId, Player player, Material material, int amount) {
        Caravan caravan = caravans.get(caravanId);
        if (caravan == null) {
            return false;
        }
        
        // Check if player has permission to remove items (owner or member)  
        if (!caravan.hasAccess(player.getUniqueId())) {
            return false;
        }
        
        // Check if caravan has enough items
        int currentAmount = caravan.getInventory().getOrDefault(material, 0);
        if (currentAmount < amount) {
            return false;
        }
        
        // Check if player has inventory space
        if (!hasInventorySpace(player, material, amount)) {
            return false;
        }
        
        // Remove from caravan inventory
        if (currentAmount == amount) {
            caravan.getInventory().remove(material);
        } else {
            caravan.getInventory().put(material, currentAmount - amount);
        }
        
        // Add to player inventory
        addItemsToPlayer(player, material, amount);
        
        // Save changes
        storage.saveCaravan(caravan);
        
        logger.info("Player " + player.getName() + " removed " + amount + " " + material + " from caravan " + caravanId);
        return true;
    }
    
    private boolean hasEnoughItems(Player player, Material material, int amount) {
        int playerAmount = 0;
        for (org.bukkit.inventory.ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                playerAmount += item.getAmount();
            }
        }
        return playerAmount >= amount;
    }
    
    private void removeItemsFromPlayer(Player player, Material material, int amount) {
        int remaining = amount;
        org.bukkit.inventory.ItemStack[] contents = player.getInventory().getContents();
        
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            org.bukkit.inventory.ItemStack item = contents[i];
            if (item != null && item.getType() == material) {
                int itemAmount = item.getAmount();
                if (itemAmount <= remaining) {
                    remaining -= itemAmount;
                    contents[i] = null;
                } else {
                    item.setAmount(itemAmount - remaining);
                    remaining = 0;
                }
            }
        }
        
        player.getInventory().setContents(contents);
        player.updateInventory();
    }
    
    private boolean hasInventorySpace(Player player, Material material, int amount) {
        int maxStackSize = material.getMaxStackSize();
        int slotsNeeded = (int) Math.ceil((double) amount / maxStackSize);
        int emptySlots = 0;
        int partialSlots = 0;
        
        for (org.bukkit.inventory.ItemStack item : player.getInventory().getContents()) {
            if (item == null) {
                emptySlots++;
            } else if (item.getType() == material && item.getAmount() < maxStackSize) {
                partialSlots++;
            }
        }
        
        return (emptySlots + partialSlots) >= slotsNeeded;
    }
    
    private void addItemsToPlayer(Player player, Material material, int amount) {
        int remaining = amount;
        int maxStackSize = material.getMaxStackSize();
        
        while (remaining > 0) {
            int stackSize = Math.min(remaining, maxStackSize);
            org.bukkit.inventory.ItemStack itemStack = new org.bukkit.inventory.ItemStack(material, stackSize);
            player.getInventory().addItem(itemStack);
            remaining -= stackSize;
        }
        
        player.updateInventory();
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
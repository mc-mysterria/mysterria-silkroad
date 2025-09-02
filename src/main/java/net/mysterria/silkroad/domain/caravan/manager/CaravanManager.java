package net.mysterria.silkroad.domain.caravan.manager;

import net.mysterria.silkroad.SilkRoad;
import net.mysterria.silkroad.config.SilkRoadConfig;
import net.mysterria.silkroad.domain.caravan.model.Caravan;
import net.mysterria.silkroad.domain.caravan.model.CaravanStorage;
import net.mysterria.silkroad.domain.caravan.model.ResourceTransfer;
import net.mysterria.silkroad.utils.ShardUtils;
import net.mysterria.silkroad.utils.HuskTownsIntegration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
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
        CaravanCreationResult result = createCaravanWithValidation(id, name, location, territoryChunks);
        return result.isSuccess() ? result.getCaravan() : null;
    }
    
    /**
     * Creates a caravan with detailed validation results
     * @param id Caravan ID
     * @param name Caravan name
     * @param location Caravan location
     * @param territoryChunks Set of territory chunks
     * @return CaravanCreationResult with details about success/failure
     */
    public CaravanCreationResult createCaravanWithValidation(String id, String name, Location location, Set<String> territoryChunks) {
        if (caravans.containsKey(id)) {
            return CaravanCreationResult.error("A caravan with that name already exists!");
        }
        
        // Validate territory chunks with HuskTowns if available
        HuskTownsIntegration.ValidationResult validation = null;
        if (territoryChunks != null && !territoryChunks.isEmpty()) {
            validation = HuskTownsIntegration.validateCaravanTerritory(territoryChunks);
            
            if (!validation.isSuccess()) {
                logger.warning("Failed to create caravan " + id + ": " + validation.getMessage());
                return CaravanCreationResult.error("§cHuskTowns validation failed: §7" + validation.getMessage());
            }
            
            // Check if town already has a caravan
            if (validation.getTownId() != -1) {
                Set<Set<String>> existingCaravanTerritories = caravans.values().stream()
                        .map(Caravan::getTerritoryChunks)
                        .collect(java.util.stream.Collectors.toSet());
                
                if (HuskTownsIntegration.townHasCaravan(validation.getTownId(), existingCaravanTerritories)) {
                    logger.warning("Failed to create caravan " + id + ": Town '" + validation.getTownName() + "' already has a caravan");
                    return CaravanCreationResult.error("§cTown '§d" + validation.getTownName() + "§c' already has a caravan! Each town can only have one caravan.");
                }
            }
            
            logger.info("Caravan territory validation passed: " + validation.getMessage());
        }
        
        Caravan caravan = new Caravan(id, name, location);
        if (territoryChunks != null) {
            caravan.getTerritoryChunks().addAll(territoryChunks);
        }
        
        // Set town ownership information if validation was successful
        if (validation != null && validation.isSuccess() && validation.getTownId() != -1) {
            caravan.setOwningTownName(validation.getTownName());
            caravan.setOwningTownId(validation.getTownId());
            logger.info("Caravan " + id + " assigned to town: " + validation.getTownName() + " (ID: " + validation.getTownId() + ")");
            
            // Automatically add all town members to the caravan
            Set<UUID> townMembers = HuskTownsIntegration.getTownMemberUUIDs(validation.getTownId());
            for (UUID memberUuid : townMembers) {
                caravan.addMember(memberUuid);
            }
            logger.info("Added " + townMembers.size() + " town members to caravan " + id);
        }
        caravans.put(id, caravan);
        storage.saveCaravan(caravan);
        logger.info("Created caravan: " + id + " with territory chunks (" + (territoryChunks == null ? 0 : territoryChunks.size()) + ") at " + locationToString(location));
        return CaravanCreationResult.success(caravan);
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
    
    /**
     * Creates a transfer using ItemStacks (preferred method)
     */
    public ResourceTransfer createTransfer(Player player, String sourceCaravanId, String destinationCaravanId, 
                                         List<ItemStack> itemResources) {
        Caravan source = caravans.get(sourceCaravanId);
        Caravan destination = caravans.get(destinationCaravanId);
        
        if (source == null || destination == null) {
            return null;
        }
        
        // Check if source caravan has enough item resources
        for (ItemStack item : itemResources) {
            if (source.getItemStackAmount(item) < item.getAmount()) {
                return null;
            }
        }
        
        double distance = source.distanceTo(destination);
        int cost = calculateItemTransferCost(distance, itemResources);
        
        // USING ENERGY SHARDS FOR TRANSFER COST
        SilkRoadConfig config = SilkRoad.getInstance().getPluginConfig();
        if (config.isTransferDebugEnabled()) {
            logger.info("DEBUG: Transfer cost calculated as " + cost + " shards for player " + player.getName());
        }
        
        // Check if player has enough shards to pay for the transfer
        if (!hasEnoughShards(player, cost)) {
            if (config.isTransferDebugEnabled()) {
                logger.info("DEBUG: Player " + player.getName() + " does not have enough shards. Required: " + cost + ", Has: " + countPlayerShards(player));
            }
            return null; // Not enough shards
        }
        
        if (config.isTransferDebugEnabled()) {
            logger.info("DEBUG: Player " + player.getName() + " has enough shards (" + countPlayerShards(player) + "/" + cost + ")");
        }
        
        // Consume shards from player's inventory
        if (!consumeShards(player, cost)) {
            if (config.isTransferDebugEnabled()) {
                logger.info("DEBUG: Failed to consume shards from player " + player.getName() + " inventory");
            }
            return null; // Failed to consume shards
        }
        
        if (config.isTransferDebugEnabled()) {
            logger.info("DEBUG: Successfully consumed " + cost + " shards from player " + player.getName());
        }
        
        long deliveryTime = System.currentTimeMillis() + calculateDeliveryTime(distance);
        
        String transferId = UUID.randomUUID().toString();
        ResourceTransfer transfer = new ResourceTransfer(transferId, sourceCaravanId, destinationCaravanId, 
                player, itemResources, distance, cost, deliveryTime);
        
        // Remove item resources from source caravan
        for (ItemStack item : itemResources) {
            source.removeItemStack(item, item.getAmount());
        }
        
        transfer.setStatus(ResourceTransfer.TransferStatus.IN_TRANSIT);
        activeTransfers.put(transferId, transfer);
        
        storage.saveCaravan(source);
        storage.saveTransfer(transfer);
        
        logger.info("Created ItemStack transfer: " + transferId + " from " + sourceCaravanId + " to " + destinationCaravanId + " for " + cost + " shards");
        return transfer;
    }
    
    /**
     * @deprecated Use createTransfer(Player, String, String, List<ItemStack>) instead.
     * Creates a transfer using legacy Material-based resources (for backwards compatibility)
     */
    @Deprecated
    public ResourceTransfer createTransferLegacy(Player player, String sourceCaravanId, String destinationCaravanId, 
                                         Map<Material, Integer> resources) {
        Caravan source = caravans.get(sourceCaravanId);
        Caravan destination = caravans.get(destinationCaravanId);
        
        if (source == null || destination == null) {
            return null;
        }
        
        for (Map.Entry<Material, Integer> entry : resources.entrySet()) {
            if (source.getResourceAmount(entry.getKey()) < entry.getValue()) {
                return null;
            }
        }
        
        double distance = source.distanceTo(destination);
        int cost = calculateTransferCostLegacy(distance, resources);
        
        if (SilkRoad.getInstance().getShardUtils().getTotalPlayerShards(player) < cost) {
            return null; // Not enough shards
        }
        
        if (!SilkRoad.getInstance().getShardUtils().consumeShards(player, cost)) {
            return null; // Failed to consume shards
        }
        
        long deliveryTime = System.currentTimeMillis() + calculateDeliveryTime(distance);
        
        String transferId = UUID.randomUUID().toString();
        ResourceTransfer transfer = new ResourceTransfer(transferId, sourceCaravanId, destinationCaravanId, 
                player, resources, distance, cost, deliveryTime);
        
        for (Map.Entry<Material, Integer> entry : resources.entrySet()) {
            source.removeResource(entry.getKey(), entry.getValue());
        }
        
        transfer.setStatus(ResourceTransfer.TransferStatus.IN_TRANSIT);
        activeTransfers.put(transferId, transfer);
        
        storage.saveCaravan(source);
        storage.saveTransfer(transfer);
        
        logger.info("Created legacy transfer: " + transferId + " from " + sourceCaravanId + " to " + destinationCaravanId + " for " + cost + " shards");
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
    
    private int calculateTransferCostLegacy(double distance, Map<Material, Integer> resources) {
        int totalItems = resources.values().stream().mapToInt(Integer::intValue).sum();
        double baseCost = distance * 0.1;
        double itemCost = totalItems * 0.5;
        return (int) Math.max(1, baseCost + itemCost);
    }
    
    private long calculateDeliveryTime(double distance) {
        SilkRoadConfig config = SilkRoad.getInstance().getPluginConfig();
        long baseTime = config.getBaseTimeMs();
        long distanceTime = (long) (distance * config.getTimePerBlockMs());
        return baseTime + distanceTime;
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
                 transfer.getStatus() == ResourceTransfer.TransferStatus.PENDING ||
                 transfer.getStatus() == ResourceTransfer.TransferStatus.DELIVERED)) {
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
        
        // Only remove completed transfers that were successfully processed
        // Delivered transfers stay active until claimed
        for (ResourceTransfer transfer : completed) {
            if (transfer.getStatus() != ResourceTransfer.TransferStatus.DELIVERED) {
                activeTransfers.remove(transfer.getId());
                storage.deleteTransfer(transfer.getId());
            }
        }
    }
    
    private boolean completeTransfer(ResourceTransfer transfer) {
        Caravan destination = caravans.get(transfer.getDestinationCaravanId());
        if (destination == null) {
            transfer.setStatus(ResourceTransfer.TransferStatus.FAILED);
            storage.saveTransfer(transfer);
            return false;
        }
        
        // Mark as delivered but don't add to caravan inventory yet
        // Players will need to claim the transfer
        transfer.setStatus(ResourceTransfer.TransferStatus.DELIVERED);
        storage.saveTransfer(transfer);
        
        Player player = SilkRoad.getInstance().getServer().getPlayer(transfer.getPlayerId());
        if (player != null && player.isOnline()) {
            player.sendMessage("§a✓ Your caravan delivery has arrived! Use /silkroad transfers to claim it.");
        }
        
        logger.info("Transfer ready for claiming: " + transfer.getId());
        return false; // Don't remove from active transfers yet - wait for claim
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
    
    // Deprecated methods for backwards compatibility
    @Deprecated
    public boolean addOwner(String caravanId, String playerName) {
        return addMember(caravanId, playerName);
    }
    
    @Deprecated
    public boolean removeOwner(String caravanId, String playerName) {
        return removeMember(caravanId, playerName);
    }
    
    public List<Caravan> getPlayerCaravans(UUID playerId) {
        return caravans.values().stream()
                .filter(caravan -> caravan.hasAccess(playerId))
                .toList();
    }
    
    // Renamed for clarity - returns caravans where player is a member (has full access)
    public List<Caravan> getPlayerMemberCaravans(UUID playerId) {
        return caravans.values().stream()
                .filter(caravan -> caravan.isMember(playerId))
                .toList();
    }
    
    // Deprecated method for backwards compatibility
    @Deprecated
    public List<Caravan> getPlayerOwnedCaravans(UUID playerId) {
        return getPlayerMemberCaravans(playerId);
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
    
    public List<ResourceTransfer> getDeliveredTransfersForPlayer(UUID playerId) {
        return activeTransfers.values().stream()
                .filter(transfer -> transfer.getPlayerId().equals(playerId) && 
                        transfer.getStatus() == ResourceTransfer.TransferStatus.DELIVERED)
                .sorted((a, b) -> Long.compare(a.getDeliveryTime(), b.getDeliveryTime()))
                .toList();
    }
    
    public List<ResourceTransfer> getDeliveredTransfersForCaravan(String caravanId, UUID playerId) {
        Caravan caravan = caravans.get(caravanId);
        if (caravan == null || !caravan.hasAccess(playerId)) {
            return new ArrayList<>();
        }
        
        return activeTransfers.values().stream()
                .filter(transfer -> transfer.getDestinationCaravanId().equals(caravanId) && 
                        transfer.getStatus() == ResourceTransfer.TransferStatus.DELIVERED)
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
        
        // Check if player has permission to add items (member)
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
            Optional<Caravan> loaded = getCaravan(caravanId);
            if (loaded.isPresent()) {
                caravan = loaded.get();
            } else {
                return false;
            }
        }
        
        // Check if player has permission to remove items (member)  
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
    
    // New ItemStack-based methods that preserve NBT data
    public boolean addItemStackToCaravan(String caravanId, Player player, ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR) return false;
        
        Caravan caravan = caravans.get(caravanId);
        if (caravan == null) {
            Optional<Caravan> loaded = getCaravan(caravanId);
            if (loaded.isPresent()) {
                caravan = loaded.get();
            } else {
                return false;
            }
        }
        
        // Check if player has permission to add items (member)
        if (!caravan.hasAccess(player.getUniqueId())) {
            return false;
        }
        
        // Check if caravan can accept the item stack
        if (!caravan.canAddItemStack(itemStack)) {
            return false; // Caravan inventory is full
        }
        
        // Check if player has the items in their inventory
        if (!hasEnoughItemStacks(player, itemStack)) {
            return false;
        }
        
        // Remove items from player inventory
        removeItemStackFromPlayer(player, itemStack);
        
        // Add items to caravan inventory
        boolean added = caravan.addItemStack(itemStack);
        if (!added) {
            // If somehow the add failed, return items to player
            addItemStackToPlayer(player, itemStack);
            return false;
        }
        
        // Save changes
        storage.saveCaravan(caravan);
        
        logger.info("Player " + player.getName() + " added " + itemStack.getAmount() + " " + itemStack.getType() + " to caravan " + caravanId);
        return true;
    }
    
    public boolean removeItemStackFromCaravan(String caravanId, Player player, ItemStack itemStack, int amount) {
        if (itemStack == null || itemStack.getType() == Material.AIR) return false;
        
        Caravan caravan = caravans.get(caravanId);
        if (caravan == null) {
            Optional<Caravan> loaded = getCaravan(caravanId);
            if (loaded.isPresent()) {
                caravan = loaded.get();
            } else {
                return false;
            }
        }
        
        // Check if player has permission to remove items (member)  
        if (!caravan.hasAccess(player.getUniqueId())) {
            return false;
        }
        
        // Check if caravan has enough items
        int currentAmount = caravan.getItemStackAmount(itemStack);
        if (currentAmount < amount) {
            return false;
        }
        
        // Check if player has inventory space
        if (!hasInventorySpaceForItemStack(player, itemStack, amount)) {
            return false;
        }
        
        // Remove from caravan inventory
        if (!caravan.removeItemStack(itemStack, amount)) {
            return false;
        }
        
        // Add to player inventory
        ItemStack toAdd = itemStack.clone();
        toAdd.setAmount(amount);
        addItemStackToPlayer(player, toAdd);
        
        // Save changes
        storage.saveCaravan(caravan);
        
        logger.info("Player " + player.getName() + " removed " + amount + " " + itemStack.getType() + " from caravan " + caravanId);
        return true;
    }
    
    private boolean hasEnoughItemStacks(Player player, ItemStack itemStack) {
        int playerAmount = 0;
        for (org.bukkit.inventory.ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.isSimilar(itemStack)) {
                playerAmount += item.getAmount();
            }
        }
        return playerAmount >= itemStack.getAmount();
    }
    
    private boolean hasInventorySpaceForItemStack(Player player, ItemStack itemStack, int amount) {
        // Calculate how much space is available for this item type
        int availableSpace = 0;
        int maxStackSize = itemStack.getMaxStackSize();
        
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) {
                // Empty slot can hold a full stack
                availableSpace += maxStackSize;
            } else if (item.isSimilar(itemStack) && item.getAmount() < maxStackSize) {
                // Partial stack of the same item can hold more
                availableSpace += maxStackSize - item.getAmount();
            }
        }
        
        return availableSpace >= amount;
    }
    
    private void removeItemStackFromPlayer(Player player, ItemStack itemStack) {
        int remaining = itemStack.getAmount();
        
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            org.bukkit.inventory.ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.isSimilar(itemStack)) {
                if (item.getAmount() <= remaining) {
                    remaining -= item.getAmount();
                    player.getInventory().setItem(i, null);
                } else {
                    item.setAmount(item.getAmount() - remaining);
                    remaining = 0;
                }
                
                if (remaining <= 0) {
                    break;
                }
            }
        }
        
        // Update the client inventory after changes
        player.updateInventory();
    }
    
    private void addItemStackToPlayer(Player player, ItemStack itemStack) {
        HashMap<Integer, org.bukkit.inventory.ItemStack> overflow = player.getInventory().addItem(itemStack);
        
        // If there's overflow, drop the items
        for (org.bukkit.inventory.ItemStack overflowItem : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), overflowItem);
        }
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
    
    public boolean claimTransferToInventory(String transferId, Player player) {
        ResourceTransfer transfer = activeTransfers.get(transferId);
        if (transfer == null || transfer.getStatus() != ResourceTransfer.TransferStatus.DELIVERED) {
            return false;
        }
        
        // Check if player has permission to claim this transfer
        if (!transfer.getPlayerId().equals(player.getUniqueId())) {
            Caravan destination = caravans.get(transfer.getDestinationCaravanId());
            if (destination == null || !destination.hasAccess(player.getUniqueId())) {
                return false;
            }
        }
        
        // Check inventory space for legacy Material-based resources
        for (Map.Entry<Material, Integer> entry : transfer.getResources().entrySet()) {
            if (!hasInventorySpace(player, entry.getKey(), entry.getValue())) {
                return false;
            }
        }
        
        // Check inventory space for ItemStack-based resources
        for (ItemStack item : transfer.getItemResources()) {
            if (!hasInventorySpaceForItemStack(player, item, item.getAmount())) {
                return false;
            }
        }
        
        // Add resources to player inventory
        for (Map.Entry<Material, Integer> entry : transfer.getResources().entrySet()) {
            addItemsToPlayer(player, entry.getKey(), entry.getValue());
        }
        
        for (ItemStack item : transfer.getItemResources()) {
            addItemStackToPlayer(player, item.clone());
        }
        
        // Remove transfer from active transfers and delete from storage
        activeTransfers.remove(transferId);
        storage.deleteTransfer(transferId);
        
        logger.info("Player " + player.getName() + " claimed transfer " + transferId + " to inventory");
        return true;
    }
    
    @Deprecated
    public boolean claimTransferToCaravan(String transferId, String caravanId, Player player) {
        ResourceTransfer transfer = activeTransfers.get(transferId);
        if (transfer == null || transfer.getStatus() != ResourceTransfer.TransferStatus.DELIVERED) {
            return false;
        }
        
        Caravan caravan = caravans.get(caravanId);
        if (caravan == null || !caravan.hasAccess(player.getUniqueId())) {
            return false;
        }
        
        // Check if player has permission to claim this transfer
        if (!transfer.getPlayerId().equals(player.getUniqueId())) {
            Caravan destination = caravans.get(transfer.getDestinationCaravanId());
            if (destination == null || !destination.hasAccess(player.getUniqueId())) {
                return false;
            }
        }
        
        // Check caravan inventory space for ItemStack-based resources
        for (ItemStack item : transfer.getItemResources()) {
            if (!caravan.canAddItemStack(item)) {
                return false;
            }
        }
        
        // Add resources to caravan
        for (Map.Entry<Material, Integer> entry : transfer.getResources().entrySet()) {
            caravan.addResource(entry.getKey(), entry.getValue());
        }
        
        for (ItemStack item : transfer.getItemResources()) {
            caravan.addItemStack(item.clone());
        }
        
        // Save caravan changes
        storage.saveCaravan(caravan);
        
        // Remove transfer from active transfers and delete from storage
        activeTransfers.remove(transferId);
        storage.deleteTransfer(transferId);
        
        logger.info("Player " + player.getName() + " claimed transfer " + transferId + " to caravan " + caravanId);
        return true;
    }
    
    
    private int calculateItemTransferCost(double distance, List<ItemStack> itemResources) {
        SilkRoadConfig config = SilkRoad.getInstance().getPluginConfig();
        
        // Distance cost from config (diamonds per block)
        double distanceCost = distance * config.getDistanceCostPerBlock();
        
        // Stack cost from config (diamonds per item stack)
        int stackCount = itemResources.size();
        double stackCost = stackCount * config.getStackCost();
        
        return (int) Math.max(config.getMinimumCost(), distanceCost + stackCost);
    }

    public void saveCaravan(Caravan caravan) {
        storage.saveCaravan(caravan);
    }
    
    /**
     * Refreshes a caravan's member list from its owning town (if it has one)
     * @param caravanId The caravan ID to refresh members for
     * @return true if members were refreshed, false if not (no town or HuskTowns unavailable)
     */
    public boolean refreshCaravanMembersFromTown(String caravanId) {
        Caravan caravan = caravans.get(caravanId);
        if (caravan == null || caravan.getOwningTownId() == -1) {
            return false;
        }
        
        return refreshCaravanMembersFromTown(caravan);
    }
    
    /**
     * Refreshes a caravan's member list from its owning town (if it has one)
     * @param caravan The caravan to refresh members for
     * @return true if members were refreshed, false if not (no town or HuskTowns unavailable)
     */
    public boolean refreshCaravanMembersFromTown(Caravan caravan) {
        if (!HuskTownsIntegration.isAvailable() || caravan.getOwningTownId() == -1) {
            return false;
        }
        
        try {
            Set<UUID> townMembers = HuskTownsIntegration.getTownMemberUUIDs(caravan.getOwningTownId());
            
            // Clear current members and add all town members
            caravan.getMembers().clear();
            for (UUID memberUuid : townMembers) {
                caravan.addMember(memberUuid);
            }
            
            // Save the updated caravan
            saveCaravan(caravan);
            
            logger.info("Refreshed members for caravan " + caravan.getName() + 
                      " from town " + caravan.getOwningTownName() + 
                      " (" + townMembers.size() + " members)");
            
            return true;
        } catch (Exception e) {
            logger.warning("Error refreshing caravan members from town: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Refreshes all caravans' member lists from their owning towns
     * @return the number of caravans that were refreshed
     */
    public int refreshAllCaravanMembersFromTowns() {
        if (!HuskTownsIntegration.isAvailable()) {
            logger.info("HuskTowns not available - cannot refresh caravan members");
            return 0;
        }
        
        int refreshedCount = 0;
        for (Caravan caravan : caravans.values()) {
            if (refreshCaravanMembersFromTown(caravan)) {
                refreshedCount++;
            }
        }
        
        logger.info("Refreshed member lists for " + refreshedCount + " caravans from their towns");
        return refreshedCount;
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
    
    // ENERGY SHARD-BASED COST SYSTEM METHODS
    // Uses SacredOrder plugin's energy shard system
    
    private boolean hasEnoughShards(Player player, int amount) {
        int shardCount = countPlayerShards(player);
        return shardCount >= amount;
    }
    
    private boolean consumeShards(Player player, int amount) {
        if (!hasEnoughShards(player, amount)) {
            return false;
        }
        
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item != null && isEnergyShard(item)) {
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
        return true;
    }
    
    private int countPlayerShards(Player player) {
        int shardCount = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && isEnergyShard(item)) {
                shardCount += item.getAmount();
            }
        }
        return shardCount;
    }
    
    private boolean isEnergyShard(ItemStack item) {
        org.bukkit.plugin.Plugin sacredOrderPlugin = Bukkit.getPluginManager().getPlugin("SacredOrder");
        if (sacredOrderPlugin != null) {
            org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(sacredOrderPlugin, "shard");
            return item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(key, org.bukkit.persistence.PersistentDataType.BOOLEAN);
        } else {
            return false;
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
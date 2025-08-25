package net.mysterria.silkroad.domain.caravan.model;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
public class ResourceTransfer {
    
    private String id;
    private String sourceCaravanId;
    private String destinationCaravanId;
    private UUID playerId;
    private String playerName;
    private Map<Material, Integer> resources;
    private long createdAt;
    private long deliveryTime;
    private double distance;
    private int cost;
    private TransferStatus status;
    
    public enum TransferStatus {
        PENDING,
        IN_TRANSIT,
        DELIVERED,
        FAILED
    }
    
    public ResourceTransfer(String id, String sourceCaravanId, String destinationCaravanId, 
                           Player player, Map<Material, Integer> resources, 
                           double distance, int cost, long deliveryTime) {
        this.id = id;
        this.sourceCaravanId = sourceCaravanId;
        this.destinationCaravanId = destinationCaravanId;
        this.playerId = player.getUniqueId();
        this.playerName = player.getName();
        this.resources = new HashMap<>(resources);
        this.createdAt = System.currentTimeMillis();
        this.deliveryTime = deliveryTime;
        this.distance = distance;
        this.cost = cost;
        this.status = TransferStatus.PENDING;
    }
    
    // Constructor for deserialization from storage
    public ResourceTransfer(String id, String sourceCaravanId, String destinationCaravanId,
                           UUID playerId, String playerName, Map<Material, Integer> resources,
                           long createdAt, long deliveryTime, double distance, int cost) {
        this.id = id;
        this.sourceCaravanId = sourceCaravanId;
        this.destinationCaravanId = destinationCaravanId;
        this.playerId = playerId;
        this.playerName = playerName;
        this.resources = new HashMap<>(resources);
        this.createdAt = createdAt;
        this.deliveryTime = deliveryTime;
        this.distance = distance;
        this.cost = cost;
        this.status = TransferStatus.PENDING;
    }
    
    public boolean isReadyForDelivery() {
        return status == TransferStatus.IN_TRANSIT && System.currentTimeMillis() >= deliveryTime;
    }
    
    public long getRemainingTime() {
        return Math.max(0, deliveryTime - System.currentTimeMillis());
    }
}
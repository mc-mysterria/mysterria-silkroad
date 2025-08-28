package net.mysterria.silkroad.config;

import net.mysterria.silkroad.SilkRoad;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Configuration manager for SilkRoad plugin settings
 */
public class SilkRoadConfig {
    
    private final FileConfiguration config;
    
    public SilkRoadConfig() {
        SilkRoad plugin = SilkRoad.getInstance();
        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();
    }
    
    // Transfer cost settings
    public double getDistanceCostPerBlock() {
        return config.getDouble("transfer.cost.distance_cost_per_block", 0.1);
    }
    
    public double getStackCost() {
        return config.getDouble("transfer.cost.stack_cost", 2.0);
    }
    
    public int getMinimumCost() {
        return config.getInt("transfer.cost.minimum_cost", 1);
    }
    
    // Delivery time settings
    public long getBaseTimeMs() {
        return config.getLong("delivery.base_time_ms", 300000L); // 5 minutes
    }
    
    public long getTimePerBlockMs() {
        return config.getLong("delivery.time_per_block_ms", 1000L); // 1 second per block
    }
    
    // Debug settings
    public boolean isTransferDebugEnabled() {
        return config.getBoolean("debug.transfer_debug", true);
    }
    
    /**
     * Reload configuration from disk
     */
    public void reload() {
        SilkRoad.getInstance().reloadConfig();
    }
}
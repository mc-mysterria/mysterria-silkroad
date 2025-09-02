package net.mysterria.silkroad;

import dev.rollczi.litecommands.LiteCommands;
import dev.rollczi.litecommands.bukkit.LiteBukkitFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.mysterria.silkroad.commands.CaravanCommand;
import net.mysterria.silkroad.commands.CaravanPlayerCommand;
import net.mysterria.silkroad.config.SilkRoadConfig;
import net.mysterria.silkroad.domain.caravan.manager.CaravanManager;
import net.mysterria.silkroad.listeners.CaravanInteractionListener;
import net.mysterria.silkroad.listeners.CaravanWandListener;
import net.mysterria.silkroad.listeners.TownMembershipListener;
import net.mysterria.silkroad.utils.ShardUtils;
import net.mysterria.silkroad.utils.TranslationManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

@Slf4j
@Getter
public final class SilkRoad extends JavaPlugin {

    private CaravanManager caravanManager;
    private LiteCommands<CommandSender> liteCommands;
    private SilkRoadConfig pluginConfig;

    @Getter
    private static SilkRoad instance;

    @Getter
    private ShardUtils shardUtils;

    @Override
    public void onEnable() {
        instance = this;

        createDataFolders();
        this.pluginConfig = new SilkRoadConfig();

        // Initialize translation system
        TranslationManager.initialize();

        this.caravanManager = new CaravanManager();

        Bukkit.getPluginManager().registerEvents(new CaravanInteractionListener(caravanManager), this);
        Bukkit.getPluginManager().registerEvents(new CaravanWandListener(caravanManager), this);
        Bukkit.getPluginManager().registerEvents(new TownMembershipListener(caravanManager), this);

        this.shardUtils = new ShardUtils();

        this.liteCommands = LiteBukkitFactory.builder("silkroad", this)
                .commands(new CaravanCommand(this), new CaravanPlayerCommand(this))
                .build();

        getLogger().info("SilkRoad caravan system enabled successfully!");
    }

    private void createDataFolders() {
        if (!getDataFolder().exists() && getDataFolder().mkdirs()) {
            getLogger().info("Created data folder: " + getDataFolder().getAbsolutePath());
        }

        if (new java.io.File(getDataFolder(), "caravans").mkdirs()) {
            getLogger().info("Created caravans directory");
        }

        if (new java.io.File(getDataFolder(), "transfers").mkdirs()) {
            getLogger().info("Created transfers directory");
        }
    }

    public void log(String message) {
        Bukkit.getConsoleSender().sendMessage("[SilkRoad] " + message);
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling SilkRoad");

        if (caravanManager != null) {
            getLogger().info("Shutting down caravan system...");
            caravanManager.shutdown();
            getLogger().info("Caravan system shut down.");
        }

        if (this.liteCommands != null) {
            this.liteCommands.unregister();
        }

        getLogger().info("SilkRoad disabled successfully!");
    }
}
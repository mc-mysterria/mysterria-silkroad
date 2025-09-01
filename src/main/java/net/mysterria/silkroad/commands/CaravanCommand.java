package net.mysterria.silkroad.commands;

import dev.rollczi.litecommands.annotations.argument.Arg;
import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import dev.rollczi.litecommands.annotations.permission.Permission;
import net.mysterria.silkroad.SilkRoad;
import net.mysterria.silkroad.domain.caravan.manager.CaravanManager;
import net.mysterria.silkroad.utils.TranslationUtil;
import net.mysterria.silkroad.utils.ResourcePackGenerator;
import net.mysterria.silkroad.utils.TranslationManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.mysterria.silkroad.domain.caravan.manager.CaravanCreationResult;
import net.mysterria.silkroad.domain.caravan.model.Caravan;
import net.mysterria.silkroad.domain.caravan.model.ResourceTransfer;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

@Command(name = "silkroad", aliases = "sr")
@Permission("silkroad.caravan.admin")
public class CaravanCommand {

    private final CaravanManager caravanManager;

    public CaravanCommand(SilkRoad plugin) {
        this.caravanManager = plugin.getCaravanManager();
    }

    @Execute(name = "wand")
    public void giveWand(@Context Player sender) {
        ItemStack wand = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = wand.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(TranslationUtil.translate("item.caravan.wand"));
            meta.setLore(List.of("§7Right-click: §aSelect current chunk", "§7Left-click: §cDeselect current chunk", "§7Shift + Left-click: §c✖ Clear all selections", "§7Use §e/sr create <name> §7to create caravan from selected chunks"));
            meta.setCustomModelData(12345);
            wand.setItemMeta(meta);
        }

        sender.getInventory().addItem(wand);
        sender.sendMessage(TranslationUtil.translatable("command.wand.given", NamedTextColor.GREEN));
    }

    @Execute(name = "create")
    public void createCaravan(@Context Player sender, @Arg("name") String name) {
        String id = name.toLowerCase().replaceAll("[^a-z0-9_-]", "");
        if (id.isEmpty()) {
            sender.sendMessage(TranslationUtil.translatable("command.name.invalid", NamedTextColor.RED));
            return;
        }

        var selection = caravanManager.getSelection(sender.getUniqueId());
        if (selection.isEmpty()) {
            sender.sendMessage(TranslationUtil.translatable("command.no.chunks.selected", NamedTextColor.RED));
            return;
        }

        CaravanCreationResult result = caravanManager.createCaravanWithValidation(id, name, sender.getLocation(), new java.util.HashSet<>(selection));
        
        if (result.isSuccess()) {
            sender.sendMessage(TranslationUtil.translatable("command.caravan.created", NamedTextColor.GREEN, name, selection.size()));
            caravanManager.clearSelection(sender.getUniqueId());
        } else {
            sender.sendMessage(result.getMessage());
        }
    }

    @Execute(name = "remove")
    public void removeCaravan(@Context CommandSender sender, @Arg("name") String id) {
        if (caravanManager.removeCaravan(id)) {
            String message = TranslationUtil.translate("command.caravan.removed", id);
            sender.sendMessage(message);
        } else {
            sender.sendMessage(TranslationUtil.translatable("command.caravan.not.found", NamedTextColor.RED));
        }
    }

    @Execute(name = "list")
    public void listCaravans(@Context CommandSender sender) {
        var caravans = caravanManager.getAllCaravans();
        if (caravans.isEmpty()) {
            sender.sendMessage(TranslationUtil.translatable("command.caravan.no.caravans", NamedTextColor.GRAY));
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(TranslationUtil.translate("command.caravan.list.header", String.valueOf(caravans.size()))).append("\n");
        for (Caravan caravan : caravans) {
            sb.append(TranslationUtil.translate("command.caravan.list.entry", caravan.getName(), caravan.getId())).append("\n");
        }

        sender.sendMessage(sb.toString());
    }

    @Execute(name = "info")
    public void caravanInfo(@Context CommandSender sender, @Arg("id") String id) {
        var caravanOpt = caravanManager.getCaravan(id);
        if (caravanOpt.isEmpty()) {
            sender.sendMessage(TranslationUtil.translatable("command.caravan.not.found", NamedTextColor.RED));
            return;
        }

        Caravan caravan = caravanOpt.get();
        StringBuilder sb = new StringBuilder();
        sb.append(TranslationUtil.translate("command.caravan.info.header")).append("\n");
        sb.append(TranslationUtil.translate("command.caravan.info.name", caravan.getName())).append("\n");
        sb.append(TranslationUtil.translate("command.caravan.info.id", caravan.getId())).append("\n");
        String locationStr = String.format("%.1f, %.1f, %.1f in %s", caravan.getLocation().getX(), caravan.getLocation().getY(), caravan.getLocation().getZ(), caravan.getLocation().getWorld().getName());
        sb.append(TranslationUtil.translate("command.caravan.info.location", locationStr)).append("\n");
        
        // Display town ownership information if available
        if (caravan.getOwningTownName() != null) {
            if (caravan.getOwningTownId() != -1) {
                sb.append(TranslationUtil.translate("command.caravan.info.town.id", caravan.getOwningTownName(), String.valueOf(caravan.getOwningTownId())));
            } else {
                sb.append(TranslationUtil.translate("command.caravan.info.town", caravan.getOwningTownName()));
            }
            sb.append("\n");
        } else {
            sb.append(TranslationUtil.translate("command.caravan.info.town.none")).append("\n");
        }
        
        sb.append(TranslationUtil.translate("command.caravan.info.chunks", String.valueOf(caravan.getTerritoryChunks().size()))).append("\n");
        sb.append(TranslationUtil.translate("command.caravan.info.resources")).append(" ");
        if (caravan.getInventory().isEmpty()) {
            sb.append(TranslationUtil.translate("command.caravan.info.resources.none"));
        } else {
            sb.append("\n");
            for (var entry : caravan.getInventory().entrySet()) {
                sb.append(TranslationUtil.translate("command.caravan.info.resource.entry", entry.getKey().name(), String.valueOf(entry.getValue()))).append("\n");
            }
        }
        sender.sendMessage(sb.toString());
    }

    @Execute(name = "transfers")
    public void listTransfers(@Context Player sender) {
        List<ResourceTransfer> transfers = caravanManager.getPlayerTransfers(sender.getUniqueId());

        if (transfers.isEmpty()) {
            sender.sendMessage(TranslationUtil.translatable("command.no.active.transfers", NamedTextColor.GRAY));
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(TranslationUtil.translate("command.transfers.header", String.valueOf(transfers.size()))).append("\n");
        for (ResourceTransfer transfer : transfers) {
            String timeRemaining = formatTime(transfer.getRemainingTime());
            sb.append(TranslationUtil.translate("command.transfers.entry", transfer.getSourceCaravanId(), transfer.getDestinationCaravanId(), timeRemaining)).append("\n");
        }

        sender.sendMessage(sb.toString());
    }

    @Execute(name = "addmember")
    public void addMember(@Context CommandSender sender, @Arg("caravan") String caravanId, @Arg("player") String playerName) {
        if (caravanManager.addMember(caravanId, playerName)) {
            sender.sendMessage(TranslationUtil.translatable("command.member.added", NamedTextColor.GREEN, playerName, caravanId));
        } else {
            sender.sendMessage(TranslationUtil.translatable("command.member.add.failed", NamedTextColor.RED));
        }
    }

    @Execute(name = "removemember")
    public void removeMember(@Context CommandSender sender, @Arg("caravan") String caravanId, @Arg("player") String playerName) {
        if (caravanManager.removeMember(caravanId, playerName)) {
            sender.sendMessage(TranslationUtil.translatable("command.member.removed", NamedTextColor.GREEN, playerName, caravanId));
        } else {
            sender.sendMessage(TranslationUtil.translatable("command.member.remove.failed", NamedTextColor.RED));
        }
    }

//    @Execute(name = "manage")
//    public void openManagementGUI(@Context Player sender) {
//        CaravanManagementGUI gui = new CaravanManagementGUI(caravanManager, sender);
//        gui.open();
//    }

    @Execute(name = "resourcepack")
    public void generateResourcePack(@Context CommandSender sender) {
        sender.sendMessage(TranslationUtil.translatable("command.resourcepack.generating", NamedTextColor.YELLOW));
        
        try {
            ResourcePackGenerator.generateResourcePackTranslations();
            sender.sendMessage(TranslationUtil.translatable("command.resourcepack.success", NamedTextColor.GREEN));
            sender.sendMessage(TranslationUtil.translatable("command.resourcepack.location", NamedTextColor.GRAY));
            sender.sendMessage(TranslationUtil.translatable("command.resourcepack.zip.hint", NamedTextColor.GRAY));
        } catch (Exception e) {
            sender.sendMessage(TranslationUtil.translatable("command.resourcepack.failed", NamedTextColor.RED, e.getMessage()));
            e.printStackTrace();
        }
    }

    @Execute(name = "resourcepack zip")
    public void generateResourcePackZip(@Context CommandSender sender) {
        sender.sendMessage(TranslationUtil.translatable("command.resourcepack.zip.generating", NamedTextColor.YELLOW));
        
        try {
            java.nio.file.Path zipPath = ResourcePackGenerator.generateResourcePackZip();
            sender.sendMessage(TranslationUtil.translatable("command.resourcepack.zip.success", NamedTextColor.GREEN));
            sender.sendMessage(TranslationUtil.translatable("command.resourcepack.zip.location", NamedTextColor.GRAY, zipPath.getFileName().toString()));
            sender.sendMessage(TranslationUtil.translatable("command.resourcepack.zip.info", NamedTextColor.GRAY));
        } catch (Exception e) {
            sender.sendMessage(TranslationUtil.translatable("command.resourcepack.zip.failed", NamedTextColor.RED, e.getMessage()));
            e.printStackTrace();
        }
    }

    @Execute(name = "reload")
    public void reloadTranslations(@Context CommandSender sender) {
        sender.sendMessage(TranslationUtil.translatable("command.translations.reloading", NamedTextColor.YELLOW));
        
        try {
            TranslationManager.reload();
            sender.sendMessage(TranslationUtil.translatable("command.translations.reload.success", NamedTextColor.GREEN));
        } catch (Exception e) {
            sender.sendMessage(TranslationUtil.translatable("command.translations.reload.failed", NamedTextColor.RED, e.getMessage()));
            e.printStackTrace();
        }
    }

    private String formatTime(long millis) {
        if (millis <= 0) return TranslationUtil.translate("time.ready");

        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }
}
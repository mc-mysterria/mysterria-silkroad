package net.mysterria.silkroad.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;

import java.util.List;

public class MessageUtils {

    public static final TextColor PRIMARY = TextColor.color(148, 68, 255);    // Custom purple
    public static final TextColor SECONDARY = TextColor.color(185, 142, 255); // Lighter purple
    public static final TextColor SUCCESS = NamedTextColor.GREEN;
    public static final TextColor ERROR = NamedTextColor.RED;
    public static final TextColor INFO = NamedTextColor.GRAY;
    public static final TextColor HIGHLIGHT = TextColor.color(255, 192, 255); // Light pink

    private static final Component PREFIX = Component.text()
            .append(Component.text("[", NamedTextColor.DARK_GRAY))
            .append(Component.text("SilkRoad", PRIMARY))
            .append(Component.text("] ", NamedTextColor.DARK_GRAY))
            .build();

    private static final Component SEPARATOR = Component.text("» ", NamedTextColor.DARK_GRAY);

    public static void sendSuccess(CommandSender sender, String message) {
        sender.sendMessage(PREFIX.append(Component.text(message, SUCCESS)));
    }

    public static void sendError(CommandSender sender, String message) {
        sender.sendMessage(PREFIX.append(Component.text(message, ERROR)));
    }

    public static void sendInfo(CommandSender sender, String message) {
        sender.sendMessage(PREFIX.append(Component.text(message, INFO)));
    }

    public static void sendListItems(CommandSender sender, String title, List<String> items) {
        TextComponent.Builder message = Component.text()
                .append(PREFIX)
                .append(Component.text(title + ":", INFO));

        if (items.isEmpty()) {
            message.append(Component.text(" None", HIGHLIGHT));
        } else {
            message.append(Component.newline());
            items.forEach(item ->
                    message.append(SEPARATOR)
                            .append(Component.text(item, HIGHLIGHT))
                            .append(Component.newline())
            );
        }

        sender.sendMessage(message.build());
    }

    public static void sendCountdownStatus(CommandSender sender, long remainingTime) {
        long seconds = remainingTime / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;

        String timeFormat = String.format("%02d:%02d", minutes, seconds);

        TextComponent message = Component.text()
                .append(PREFIX)
                .append(Component.text("Time Remaining: ", INFO))
                .append(Component.text(timeFormat, HIGHLIGHT))
                .build();

        sender.sendMessage(message);
    }

    private static void appendKeyValue(TextComponent.Builder builder, String key, String value) {
        builder.append(SEPARATOR)
                .append(Component.text(key + ": ", INFO))
                .append(Component.text(value, HIGHLIGHT))
                .append(Component.newline());
    }

    public static Component createClickableCommand(String text, String command) {
        return Component.text(text, HIGHLIGHT)
                .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand(command))
                .hoverEvent(Component.text("Click to execute", INFO));
    }

    public static Component createHoverable(String text, String hoverText) {
        return Component.text(text, HIGHLIGHT)
                .hoverEvent(Component.text(hoverText, INFO));
    }
}
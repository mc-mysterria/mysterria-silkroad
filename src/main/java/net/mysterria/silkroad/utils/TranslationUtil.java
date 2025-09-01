package net.mysterria.silkroad.utils;

import net.mysterria.silkroad.utils.TranslationManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.Arrays;

public class TranslationUtil {
    
    /**
     * Creates a translatable component with the raidstorm namespace
     * @param key The translation key
     * @param args Optional formatting arguments
     * @return The translatable component
     */
    public static Component translatable(String key, Object... args) {

        Component.translatable("");

        ComponentLike[] componentArgs = convertToComponentLike(args);
        return Component.translatable(TranslationManager.getNamespace() + ":" + key, componentArgs);
    }
    
    /**
     * Creates a translatable component with color
     * @param key The translation key
     * @param color The text color
     * @param args Optional formatting arguments
     * @return The translatable component
     */
    public static Component translatable(String key, NamedTextColor color, Object... args) {
        ComponentLike[] componentArgs = convertToComponentLike(args);
        return Component.translatable(TranslationManager.getNamespace() + ":" + key, componentArgs).color(color);
    }
    
    /**
     * Creates a translatable component with color and decoration
     * @param key The translation key
     * @param color The text color
     * @param decoration The text decoration
     * @param args Optional formatting arguments
     * @return The translatable component
     */
    public static Component translatable(String key, NamedTextColor color, TextDecoration decoration, Object... args) {
        ComponentLike[] componentArgs = convertToComponentLike(args);
        return Component.translatable(TranslationManager.getNamespace() + ":" + key, componentArgs)
                .color(color).decorate(decoration);
    }
    
    /**
     * Gets the translated string value for a key
     * @param key The translation key
     * @param args Optional formatting arguments
     * @return The translated string
     */
    public static String translate(String key, Object... args) {
        return TranslationManager.translate(key, args);
    }
    
    private static ComponentLike[] convertToComponentLike(Object... args) {
        return Arrays.stream(args)
                .map(obj -> {
                    if (obj instanceof ComponentLike) {
                        return (ComponentLike) obj;
                    } else {
                        return Component.text(String.valueOf(obj));
                    }
                })
                .toArray(ComponentLike[]::new);
    }
}
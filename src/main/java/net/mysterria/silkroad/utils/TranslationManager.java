package net.mysterria.silkroad.utils;

import net.mysterria.silkroad.SilkRoad;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationStore;
import net.kyori.adventure.util.UTF8ResourceBundleControl;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Function;

public class TranslationManager {

    private static final String NAMESPACE = "silkroad";
    private static final Key REGISTRY_KEY = Key.key(NAMESPACE, "translations");
    private static TranslationStore.StringBased<MessageFormat> translationStore;
    private static boolean initialized = false;

    public static void initialize() {
        if (initialized) {
            return;
        }

        try {
            translationStore = TranslationStore.messageFormat(REGISTRY_KEY);

            loadTranslations(Locale.ENGLISH);
            loadTranslations(Locale.forLanguageTag("uk")); // Ukrainian

            GlobalTranslator.translator().addSource(translationStore);

            initialized = true;
            SilkRoad.getInstance().log("Translation system initialized successfully");
        } catch (Exception e) {
            SilkRoad.getInstance().log("Failed to initialize translation system: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void loadTranslations(Locale locale) {
        try {
            String bundleName = "lang/lang";

            ResourceBundle bundle = ResourceBundle.getBundle(bundleName, locale,
                    SilkRoad.getInstance().getClass().getClassLoader(),
                    new UTF8ResourceBundleControl());

            Set<String> originalKeys = bundle.keySet();
            Set<String> namespacedKeys = new HashSet<>();
            
            for (String key : originalKeys) {
                namespacedKeys.add(NAMESPACE + ":" + key);
            }
            
            Function<String, MessageFormat> function = namespacedKey -> {
                String actualKey = namespacedKey.substring(NAMESPACE.length() + 1);
                return new MessageFormat(bundle.getString(actualKey), locale);
            };

            translationStore.registerAll(locale, namespacedKeys, function);

            SilkRoad.getInstance().log("Loaded " + namespacedKeys.size() + " translations for locale: " + locale);
        } catch (Exception e) {
            SilkRoad.getInstance().log("Failed to load translations for locale " + locale + ": " + e.getMessage());
        }
    }

    public static void reload() {
        if (translationStore != null) {
            GlobalTranslator.translator().removeSource(translationStore);
            initialized = false;
            initialize();
        }
    }

    public static String getNamespace() {
        return NAMESPACE;
    }
    
    /**
     * Gets the translated string for a key with the current default locale
     * @param key The translation key (without namespace)
     * @param args Optional formatting arguments
     * @return The translated and formatted string
     */
    public static String translate(String key, Object... args) {
        try {
            if (!initialized || translationStore == null) {
                return key; // Return the key as fallback if not initialized
            }
            
            // Get the MessageFormat from the translation store
            String namespacedKey = NAMESPACE + ":" + key;
            MessageFormat format = translationStore.translate(namespacedKey, Locale.ENGLISH);
            
            if (format == null) {
                return key; // Return the key as fallback if translation not found
            }
            
            // Format the message with the provided arguments
            return format.format(args);
        } catch (Exception e) {
            // Return the key as fallback if there's any error
            return key;
        }
    }
}
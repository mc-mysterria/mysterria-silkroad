package net.mysterria.silkroad.domain.caravan.gui;

import dev.triumphteam.gui.builder.item.PaperItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.mysterria.silkroad.domain.caravan.manager.CaravanManager;
import net.mysterria.silkroad.domain.caravan.model.Caravan;
import net.mysterria.silkroad.utils.TranslationUtil;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.DragType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CaravanInventoryManagementGUI {

    private final CaravanManager caravanManager;
    private final Player player;
    private final Caravan caravan;
    private boolean isProcessing = false;
    
    public CaravanInventoryManagementGUI(CaravanManager caravanManager, Player player, Caravan caravan) {
        this.caravanManager = caravanManager;
        this.player = player;
        this.caravan = caravan;
    }
    
    public void open() {
        Gui gui = Gui.gui()
                .title(TranslationUtil.translatable("gui.inventory.management.title", NamedTextColor.GOLD, caravan.getName()))
                .rows(6)
                .create();

        // Set custom drag handler for drag-and-drop deposits
        gui.setDragAction(event -> handleDragEvent(event, gui));

        // Set default click action to handle deposits and shift-clicks
        gui.setDefaultClickAction(event -> {
            int slot = event.getSlot();

            // Handle clicks in GUI area
            if (slot >= 9 && slot <= 44 && event.getClickedInventory() != null &&
                !event.getClickedInventory().equals(player.getInventory())) {
                // Click in GUI inventory area
                ItemStack cursor = event.getCursor();

                if (cursor != null && cursor.getType() != Material.AIR) {
                    // Player has item on cursor - deposit it
                    event.setCancelled(true);
                    handleClickDeposit(cursor, event, gui);
                }
                // If cursor is empty and slot has item, let the withdrawal handler deal with it
            }

            // Handle shift-clicking from player's inventory to deposit
            if (event.getClickedInventory() != null &&
                event.getClickedInventory().equals(player.getInventory()) &&
                event.getClick().isShiftClick()) {
                ItemStack clicked = event.getCurrentItem();
                if (clicked != null && clicked.getType() != Material.AIR) {
                    event.setCancelled(true);
                    handleShiftClickDeposit(clicked, event, gui);
                }
            }
        });

        setupInventoryGUI(gui);
        gui.open(player);
    }
    
    private void setupInventoryGUI(Gui gui) {
        // Clear all items to ensure clean state
        for (int i = 0; i < 54; i++) {
            gui.removeItem(i);
        }

        // Add glass pane fillers first
        setupGlassPaneFillers(gui);

        // Set up caravan inventory view
        setupCaravanInventoryView(gui);

        // Set navigation items
        setupNavigationItems(gui);

        // Update the GUI to refresh all items
        gui.update();
    }
    
    private void setupNavigationItems(Gui gui) {
        // Get caravan status for info header
        var optionalCaravan = caravanManager.getCaravan(caravan.getId());
        Caravan currentCaravan = optionalCaravan.orElse(caravan);
        int usedSlots = currentCaravan.getItemInventory().size();
        int maxSlots = Caravan.MAX_INVENTORY_SLOTS;

        // Info header
        GuiItem infoItem = PaperItemBuilder.from(Material.CHEST)
                .name(TranslationUtil.translatable("gui.caravan.inventory.label").color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false))
                .lore(TranslationUtil.translatable("gui.drag.drop.instruction").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                      TranslationUtil.translatable("gui.slots.usage", String.valueOf(usedSlots), String.valueOf(maxSlots), (usedSlots >= maxSlots ? TranslationUtil.translate("gui.slots.full") : "")).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                .asGuiItem(event -> event.setCancelled(true));
        gui.setItem(0, infoItem);

        // Control buttons - Help/Tutorial
        GuiItem helpItem = PaperItemBuilder.from(Material.BOOK)
                .name(Component.text("How to Use", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false))
                .lore(Component.text("━━━━━━━━━━━━━━━━━━━━", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false),
                      Component.text("Deposit Items:", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false),
                      Component.text("  • Click item, then click GUI", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                      Component.text("  • Right-click = deposit half", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                      Component.text("  • Shift-click = deposit all", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                      Component.empty(),
                      Component.text("Withdraw Items:", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false),
                      Component.text("  • Left-click = withdraw all", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                      Component.text("  • Right-click = withdraw half", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                      Component.text("━━━━━━━━━━━━━━━━━━━━", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false))
                .asGuiItem(event -> event.setCancelled(true));
        gui.setItem(49, helpItem);
        
        GuiItem backItem = PaperItemBuilder.from(Material.ARROW)
                .name(TranslationUtil.translatable("gui.back.arrow").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    CaravanManagementGUI managementGUI = new CaravanManagementGUI(caravanManager, player);
                    managementGUI.open();
                });
        gui.setItem(45, backItem);

        GuiItem closeItem = PaperItemBuilder.from(Material.BARRIER)
                .name(TranslationUtil.translatable("gui.close").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    player.closeInventory();
                });
        gui.setItem(53, closeItem);
    }

    private void setupGlassPaneFillers(Gui gui) {
        // Fill non-interactive slots with glass panes to prevent incorrect deposits
        int[] glassSlots = {1, 2, 3, 4, 5, 6, 7, 8, 46, 47, 48, 50, 51, 52};

        GuiItem glassPane = PaperItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE)
                .name(Component.text("").decoration(TextDecoration.ITALIC, false))
                .asGuiItem(event -> event.setCancelled(true));

        for (int slot : glassSlots) {
            gui.setItem(slot, glassPane);
        }
    }

    private void setupCaravanInventoryView(Gui gui) {
        // Display caravan inventory items (drag-and-drop enabled)
        // Get the latest caravan state to ensure we show current inventory
        var optionalCaravan = caravanManager.getCaravan(caravan.getId());
        Caravan currentCaravan = optionalCaravan.orElse(caravan);

        // Available slots: rows 2-5 (slots 9-44)
        int[] availableSlots = {
            9, 10, 11, 12, 13, 14, 15, 16, 17,  // Row 2
            18, 19, 20, 21, 22, 23, 24, 25, 26, // Row 3
            27, 28, 29, 30, 31, 32, 33, 34, 35,  // Row 4
            36, 37, 38, 39, 40, 41, 42, 43, 44 // Row 5
        };

        // Display caravan items in GUI slots
        // For withdrawals: player clicks item, then places it in their inventory
        int slotIndex = 0;
        for (ItemStack itemStack : currentCaravan.getItemInventory()) {
            if (slotIndex >= availableSlots.length) break;

            final ItemStack currentItem = itemStack;
            GuiItem item = PaperItemBuilder.from(itemStack)
                    .asGuiItem(event -> handleWithdrawal(currentItem, event, gui));

            gui.setItem(availableSlots[slotIndex], item);
            slotIndex++;
        }

        if (currentCaravan.getItemInventory().isEmpty()) {
            GuiItem emptyItem = PaperItemBuilder.from(Material.BARRIER)
                    .name(TranslationUtil.translatable("gui.no.items").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                    .lore(TranslationUtil.translatable("gui.caravan.empty").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                    .asGuiItem(event -> event.setCancelled(true));
            gui.setItem(22, emptyItem);
        }
    }

    private void handleDragEvent(InventoryDragEvent event, Gui gui) {
        // Prevent rapid actions during processing
        if (isProcessing) {
            event.setCancelled(true);
            return;
        }

        Set<Integer> rawSlots = event.getRawSlots();
        ItemStack cursor = event.getCursor();

        if (cursor == null || cursor.getType() == Material.AIR) {
            event.setCancelled(true);
            return;
        }

        // Define protected slots (navigation, controls, glass panes)
        Set<Integer> protectedSlots = Set.of(
                0, 1, 2, 3, 4, 5, 6, 7, 8,           // Row 1
                45, 46, 47, 48, 49, 50, 51, 52, 53  // Row 6
        );

        // Check if drag involves any protected slots
        for (int rawSlot : rawSlots) {
            if (protectedSlots.contains(rawSlot)) {
                event.setCancelled(true);
                return;
            }
        }

        // Check if drag includes any valid GUI inventory slots (9-44)
        // Note: rawSlots can include both GUI slots and player inventory slots when dragging
        boolean includesGUISlots = rawSlots.stream()
                .anyMatch(slot -> slot >= 9 && slot <= 44);

        if (!includesGUISlots) {
            // Drag doesn't involve GUI inventory slots, allow normal behavior
            event.setCancelled(true);
            return;
        }

        // Get only the GUI slots from the drag
        Set<Integer> guiSlots = new HashSet<>();
        for (int slot : rawSlots) {
            if (slot >= 9 && slot <= 44) {
                guiSlots.add(slot);
            }
        }

        // Handle deposit: dragging items to GUI slots
        handleDeposit(event, gui, cursor, guiSlots);
    }

    private void handleDeposit(InventoryDragEvent event, Gui gui, ItemStack cursor, Set<Integer> rawSlots) {
        // Get current caravan state
        var optionalCaravan = caravanManager.getCaravan(caravan.getId());
        if (optionalCaravan.isEmpty()) {
            event.setCancelled(true);
            player.sendMessage(TranslationUtil.translate("inventory.deposit.failed"));
            return;
        }

        Caravan currentCaravan = optionalCaravan.get();

        // Check if player has permission
        if (!currentCaravan.hasAccess(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        // Calculate amount to deposit based on drag type
        DragType dragType = event.getType();
        int totalAmount;
        int slotsCount = rawSlots.size();

        switch (dragType) {
            case SINGLE:
                // Distribute 1 item per slot
                totalAmount = Math.min(slotsCount, cursor.getAmount());
                break;
            case EVEN:
                // Distribute evenly
                int perSlot = cursor.getAmount() / slotsCount;
                totalAmount = perSlot * slotsCount;
                break;
            default:
                event.setCancelled(true);
                return;
        }

        if (totalAmount <= 0) {
            event.setCancelled(true);
            return;
        }

        // Create item stack to deposit
        ItemStack toDeposit = cursor.clone();
        toDeposit.setAmount(totalAmount);

        // Check if caravan can accept the item
        if (!currentCaravan.canAddItemStack(toDeposit)) {
            event.setCancelled(true);
            player.sendMessage(TranslationUtil.translate("inventory.full",
                    String.valueOf(currentCaravan.getItemInventory().size()),
                    String.valueOf(Caravan.MAX_INVENTORY_SLOTS)));
            return;
        }

        // Set processing flag and cancel event
        isProcessing = true;
        event.setCancelled(true);

        // Add directly to caravan (item is on cursor, not in inventory!)
        boolean added = currentCaravan.addItemStack(toDeposit);
        if (!added) {
            isProcessing = false;
            player.sendMessage(TranslationUtil.translate("inventory.deposit.failed"));
            return;
        }

        // Save caravan
        try {
            caravanManager.saveCaravan(currentCaravan);
        } catch (Exception e) {
            // Rollback on save failure
            currentCaravan.removeItemStack(toDeposit, toDeposit.getAmount());
            isProcessing = false;
            player.sendMessage(TranslationUtil.translate("inventory.deposit.failed"));
            return;
        }

        // Success - update cursor
        int remaining = cursor.getAmount() - totalAmount;
        if (remaining > 0) {
            cursor.setAmount(remaining);
            event.setCursor(cursor);
        } else {
            event.setCursor(null);
        }

        player.sendMessage(TranslationUtil.translate("inventory.deposited",
                String.valueOf(totalAmount), getItemDisplayName(toDeposit)));

        // Refresh GUI after 1 tick
        org.bukkit.Bukkit.getScheduler().runTaskLater(
                net.mysterria.silkroad.SilkRoad.getInstance(),
                () -> {
                    setupInventoryGUI(gui);
                    isProcessing = false;
                },
                1L
        );
    }

    private void handleShiftClickDeposit(ItemStack itemStack, InventoryClickEvent event, Gui gui) {
        // Prevent rapid actions during processing
        if (isProcessing) {
            return;
        }

        // Set processing flag
        isProcessing = true;

        // Use the manager method since the item is in player's inventory
        // (not on cursor like in handleClickDeposit)
        if (caravanManager.addItemStackToCaravan(caravan.getId(), player, itemStack)) {
            player.sendMessage(TranslationUtil.translate("inventory.deposited",
                    String.valueOf(itemStack.getAmount()), getItemDisplayName(itemStack)));

            // Refresh GUI after 1 tick
            org.bukkit.Bukkit.getScheduler().runTaskLater(
                    net.mysterria.silkroad.SilkRoad.getInstance(),
                    () -> {
                        setupInventoryGUI(gui);
                        isProcessing = false;
                    },
                    1L
            );
        } else {
            // Failed - send error message
            isProcessing = false;
            var optionalCaravan = caravanManager.getCaravan(caravan.getId());
            if (optionalCaravan.isPresent()) {
                Caravan currentCaravan = optionalCaravan.get();
                if (!currentCaravan.canAddItemStack(itemStack)) {
                    player.sendMessage(TranslationUtil.translate("inventory.full",
                            String.valueOf(currentCaravan.getItemInventory().size()),
                            String.valueOf(Caravan.MAX_INVENTORY_SLOTS)));
                } else {
                    player.sendMessage(TranslationUtil.translate("inventory.deposit.failed"));
                }
            }
        }
    }

    private void handleClickDeposit(ItemStack cursor, InventoryClickEvent event, Gui gui) {
        // Prevent rapid actions during processing
        if (isProcessing) {
            return;
        }

        // Get current caravan state
        var optionalCaravan = caravanManager.getCaravan(caravan.getId());
        if (optionalCaravan.isEmpty()) {
            isProcessing = false;
            player.sendMessage(TranslationUtil.translate("inventory.deposit.failed"));
            return;
        }

        Caravan currentCaravan = optionalCaravan.get();

        // Check if player has permission
        if (!currentCaravan.hasAccess(player.getUniqueId())) {
            isProcessing = false;
            return;
        }

        // Determine amount to deposit based on click type
        int amount;
        boolean isRightClick = event.getClick().isRightClick();

        if (isRightClick) {
            // Right-click: deposit half (rounded up)
            amount = (int) Math.ceil(cursor.getAmount() / 2.0);
        } else {
            // Left-click: deposit entire stack
            amount = cursor.getAmount();
        }

        // Create item stack to deposit
        ItemStack toDeposit = cursor.clone();
        toDeposit.setAmount(amount);

        // Check if caravan can accept the item
        if (!currentCaravan.canAddItemStack(toDeposit)) {
            player.sendMessage(TranslationUtil.translate("inventory.full",
                    String.valueOf(currentCaravan.getItemInventory().size()),
                    String.valueOf(Caravan.MAX_INVENTORY_SLOTS)));
            return;
        }

        // Set processing flag
        isProcessing = true;

        // Add directly to caravan (item is on cursor, not in inventory!)
        boolean added = currentCaravan.addItemStack(toDeposit);
        if (!added) {
            isProcessing = false;
            player.sendMessage(TranslationUtil.translate("inventory.deposit.failed"));
            return;
        }

        // Save caravan
        try {
            caravanManager.saveCaravan(currentCaravan);
        } catch (Exception e) {
            // Rollback on save failure
            currentCaravan.removeItemStack(toDeposit, toDeposit.getAmount());
            isProcessing = false;
            player.sendMessage(TranslationUtil.translate("inventory.deposit.failed"));
            return;
        }

        // Success - update cursor
        int remaining = cursor.getAmount() - amount;
        if (remaining > 0) {
            cursor.setAmount(remaining);
            event.setCursor(cursor);
        } else {
            event.setCursor(null);
        }

        player.sendMessage(TranslationUtil.translate("inventory.deposited",
                String.valueOf(amount), getItemDisplayName(toDeposit)));

        // Refresh GUI after 1 tick
        org.bukkit.Bukkit.getScheduler().runTaskLater(
                net.mysterria.silkroad.SilkRoad.getInstance(),
                () -> {
                    setupInventoryGUI(gui);
                    isProcessing = false;
                },
                1L
        );
    }

    private void handleWithdrawal(ItemStack itemStack, InventoryClickEvent event, Gui gui) {
        // Prevent rapid clicks while processing
        if (isProcessing) {
            event.setCancelled(true);
            return;
        }

        // Cancel the default event - we'll handle the withdrawal manually
        event.setCancelled(true);

        // Determine amount based on click type
        int amount = 0;
        int available = itemStack.getAmount();

        switch (event.getClick()) {
            case LEFT:
                // Withdraw entire stack
                amount = available;
                break;
            case RIGHT:
                // Withdraw half (rounded up)
                amount = (int) Math.ceil(available / 2.0);
                break;
            case SHIFT_LEFT:
            case SHIFT_RIGHT:
                // Withdraw entire stack
                amount = available;
                break;
            default:
                return;
        }

        amount = Math.min(amount, available);
        if (amount <= 0) {
            return;
        }

        // Set processing flag
        isProcessing = true;

        // Attempt withdrawal
        if (caravanManager.removeItemStackFromCaravan(caravan.getId(), player, itemStack, amount)) {
            player.sendMessage(TranslationUtil.translate("inventory.withdrew",
                    String.valueOf(amount), getItemDisplayName(itemStack)));

            // Refresh GUI after 1 tick
            org.bukkit.Bukkit.getScheduler().runTaskLater(
                    net.mysterria.silkroad.SilkRoad.getInstance(),
                    () -> {
                        setupInventoryGUI(gui);
                        isProcessing = false;
                    },
                    1L
            );
        } else {
            // Failed - send error message
            isProcessing = false;
            player.sendMessage(TranslationUtil.translate("inventory.withdraw.failed"));
        }
    }

    private String getItemDisplayName(ItemStack itemStack) {
        if (itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName()) {
            return LegacyComponentSerializer.legacySection()
                    .serialize(itemStack.getItemMeta().displayName());
        }
        return itemStack.getType().name().toLowerCase().replace('_', ' ');
    }

    private Component text(String legacyText) {
        return LegacyComponentSerializer.legacySection().deserialize(legacyText);
    }
}
package com.modularwarfare.common.container.chest;

import com.modularwarfare.ModConfig;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryEnderChest;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class ChestGuiFilterMatcher {

    public static final String ALL_CHEST = "allChest";
    public static final String SMALL_CHEST = "smallChest";
    public static final String LARGER_CHEST = "largerChest";
    public static final String END_CHEST = "endChest";

    private ChestGuiFilterMatcher() {
    }

    public static boolean shouldApply(final ModConfig.CustomChestGui config, final IInventory chestInventory) {
        if (config == null || chestInventory == null || !config.enable) {
            return false;
        }
        return evaluate(chestInventory, config.filterMode, toFilterNames(config.filters));
    }

    public static boolean shouldApply(final boolean enable, final String filterMode,
                                      final List<String> filterNames, final IInventory chestInventory) {
        if (!enable || chestInventory == null) {
            return false;
        }
        return evaluate(chestInventory, filterMode, filterNames);
    }

    private static List<String> toFilterNames(final List<ModConfig.CustomChestGui.ChestGuiFilter> filters) {
        final List<String> names = new ArrayList<>();
        if (filters == null) {
            return names;
        }
        for (final ModConfig.CustomChestGui.ChestGuiFilter filter : filters) {
            if (filter != null && filter.guiName != null && !filter.guiName.isEmpty()) {
                names.add(filter.guiName);
            }
        }
        return names;
    }

    private static boolean evaluate(final IInventory chestInventory, final String filterMode,
                                    final List<String> filterNames) {
        if (filterNames.isEmpty()) {
            return false;
        }
        boolean matched = false;
        for (final String rule : filterNames) {
            if (matchesRule(chestInventory, rule)) {
                matched = true;
                break;
            }
        }
        if ("blacklist".equalsIgnoreCase(filterMode)) {
            return !matched;
        }
        return matched;
    }

    private static boolean matchesRule(final IInventory chestInventory, final String rule) {
        if (ALL_CHEST.equalsIgnoreCase(rule)) {
            return true;
        }
        final int slotCount = chestInventory.getSizeInventory();
        if (SMALL_CHEST.equalsIgnoreCase(rule)) {
            return slotCount <= 27;
        }
        if (LARGER_CHEST.equalsIgnoreCase(rule)) {
            return slotCount > 27;
        }
        if (END_CHEST.equalsIgnoreCase(rule)) {
            return isEnderChest(chestInventory);
        }
        return matchesDisplayName(chestInventory, rule);
    }

    /**
     * 界面展示名匹配：优先按 Java 正则（{@code find}）匹配，语法非法时回退为精确相等。
     * 示例：{@code .*③.*}、{@code ^§f战利品$}、{@code 战利品}
     */
    private static boolean matchesDisplayName(final IInventory chestInventory, final String rule) {
        final String formatted = chestInventory.getDisplayName().getFormattedText();
        final String plain = chestInventory.getDisplayName().getUnformattedText();
        final String inventoryName = chestInventory.getName();
        try {
            final Pattern pattern = Pattern.compile(rule);
            return matchesAny(pattern, formatted, plain, inventoryName);
        } catch (final PatternSyntaxException ignored) {
            return rule.equals(formatted) || rule.equals(plain) || rule.equals(inventoryName);
        }
    }

    private static boolean matchesAny(final Pattern pattern, final String... texts) {
        for (final String text : texts) {
            if (text != null && pattern.matcher(text).find()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isEnderChest(final IInventory inventory) {
        if (inventory instanceof InventoryEnderChest) {
            return true;
        }
        final String className = inventory.getClass().getSimpleName().toLowerCase();
        if (className.contains("ender")) {
            return true;
        }
        final String inventoryName = inventory.getName().toLowerCase();
        return inventoryName.contains("enderchest") || inventoryName.contains("ender_chest");
    }
}

package com.modularwarfare.client.chest;

import com.modularwarfare.common.container.chest.ChestGuiFilterMatcher;
import net.minecraft.inventory.IInventory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ClientChestGuiSettings {

    private static boolean enable;
    private static String filterMode = "whitelist";
    private static final List<String> filters = new ArrayList<>();

    private ClientChestGuiSettings() {
    }

    public static void applyFromServer(final boolean enableIn, final String filterModeIn, final List<String> filtersIn) {
        enable = enableIn;
        filterMode = filterModeIn != null ? filterModeIn : "whitelist";
        filters.clear();
        if (filtersIn != null) {
            filters.addAll(filtersIn);
        }
    }

    public static void clear() {
        enable = false;
        filterMode = "whitelist";
        filters.clear();
    }

    public static boolean isEnabled() {
        return enable;
    }

    public static boolean shouldApply(final IInventory chestInventory) {
        return ChestGuiFilterMatcher.shouldApply(enable, filterMode, filters, chestInventory);
    }

    public static List<String> getFilters() {
        return Collections.unmodifiableList(filters);
    }
}

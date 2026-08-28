package com.modularwarfare.client.view;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class AimPoseClientStore {

    private static final long STALE_MS = 1500L;
    private static final ConcurrentHashMap<UUID, Entry> ENTRIES = new ConcurrentHashMap<UUID, Entry>();

    public static final class Entry {
        float prevLookYaw;
        float lookYaw;
        float prevLookPitch;
        float lookPitch;
        float prevBodyYaw;
        float bodyYaw;
        boolean active;
        long lastUpdateMs;
        boolean initialized;
    }

    private AimPoseClientStore() {}

    public static void apply(UUID playerId, float lookYaw, float lookPitch, float bodyYaw, boolean active) {
        if (playerId == null) {
            return;
        }
        if (!active) {
            ENTRIES.remove(playerId);
            return;
        }
        Entry entry = ENTRIES.computeIfAbsent(playerId, ignored -> new Entry());
        if (!entry.initialized) {
            entry.prevLookYaw = lookYaw;
            entry.prevLookPitch = lookPitch;
            entry.prevBodyYaw = bodyYaw;
            entry.initialized = true;
        } else {
            entry.prevLookYaw = entry.lookYaw;
            entry.prevLookPitch = entry.lookPitch;
            entry.prevBodyYaw = entry.bodyYaw;
        }
        entry.lookYaw = lookYaw;
        entry.lookPitch = lookPitch;
        entry.bodyYaw = bodyYaw;
        entry.active = true;
        entry.lastUpdateMs = System.currentTimeMillis();
    }

    @Nullable
    public static Entry get(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        Entry entry = ENTRIES.get(playerId);
        if (entry == null || !entry.active) {
            return null;
        }
        if (System.currentTimeMillis() - entry.lastUpdateMs > STALE_MS) {
            ENTRIES.remove(playerId);
            return null;
        }
        return entry;
    }
}

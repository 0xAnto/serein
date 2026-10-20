package com.serein.app;

import java.util.Locale;

public final class MixMath {
    private MixMath() {}
    public static int clamp(int value) { return Math.max(0, Math.min(100, value)); }
    public static float gain(int level, int master, int active, float fade, float duck) {
        return Math.max(0f, Math.min(1f, clamp(level) / 100f * clamp(master) / 100f
                / (float)Math.sqrt(Math.max(1, active)) * Math.max(0f, Math.min(1f, fade)) * duck));
    }
    public static float fade(long remaining, long fadeMillis) {
        if (remaining <= 0) return 0f;
        return fadeMillis <= 0 ? 1f : Math.min(1f, remaining / (float)fadeMillis);
    }
    public static String time(long millis) {
        long total = Math.max(0L, (millis + 999L) / 1000L);
        if (total >= 3600) return String.format(Locale.US, "%d:%02d:%02d", total / 3600, total / 60 % 60, total % 60);
        return String.format(Locale.US, "%02d:%02d", total / 60, total % 60);
    }
}

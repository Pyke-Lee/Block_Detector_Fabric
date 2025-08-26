package io.github.pyke.blockdetector.config;

import io.github.pyke.blockdetector.config.BlockDetectorConfigWrapper;

public class ConfigAccess {
    public static final BlockDetectorConfigWrapper CONFIG = BlockDetectorConfigWrapper.createAndLoad();

    private ConfigAccess() { }

    public static void reload() { CONFIG.load(); }

    public static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }

    public static void normalize() {
        clamp(CONFIG.scanStep(), DetectorLimits.SCAN_STEP_MIN, DetectorLimits.SCAN_STEP_MAX);
        clamp(CONFIG.scanDurationTicks(), DetectorLimits.SCAN_DURATION_MIN, DetectorLimits.SCAN_DURATION_MAX);

        clamp(CONFIG.detectorRange(), DetectorLimits.RANGE_MIN, DetectorLimits.RANGE_MAX);
        clamp(CONFIG.cooldownTicks(), DetectorLimits.COOLDOWN_MIN, DetectorLimits.COOLDOWN_MAX);

        clamp(CONFIG.holdHud().intervalTicks, DetectorLimits.HUD_INTERVAL_MIN, DetectorLimits.HUD_INTERVAL_MAX);
        clamp(CONFIG.holdHud().detectedTicks, DetectorLimits.HUD_DETECTED_MIN, DetectorLimits.HUD_DETECTED_MAX);
        clamp(CONFIG.holdHud().notFoundTicks, DetectorLimits.HUD_NOTFOUND_MIN, DetectorLimits.HUD_NOTFOUND_MAX);
    }
}

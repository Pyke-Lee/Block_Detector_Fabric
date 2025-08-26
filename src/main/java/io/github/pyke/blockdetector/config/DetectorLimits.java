package io.github.pyke.blockdetector.config;

public final record DetectorLimits() {
    public static final int SCAN_STEP_MIN = 1;
    public static final int SCAN_STEP_MAX = 8;

    public static final int SCAN_DURATION_MIN = 1;     // ticks
    public static final int SCAN_DURATION_MAX = 200;   // ticks (~10s at 20tps)

    // 아이템 기본값(NBT 미지정 시) 및 NBT 오버라이드의 허용 범위
    public static final int RANGE_MIN = 8;
    public static final int RANGE_MAX = 512;

    public static final int COOLDOWN_MIN = 0;          // ticks
    public static final int COOLDOWN_MAX = 72000;        // ticks (30s)

    // HUD
    public static final int HUD_INTERVAL_MIN = 1;      // ticks
    public static final int HUD_INTERVAL_MAX = 40;

    public static final int HUD_DETECTED_MIN = 0;         // ticks
    public static final int HUD_DETECTED_MAX = 40;       // ticks

    public static final int HUD_NOTFOUND_MIN = 0;         // ticks
    public static final int HUD_NOTFOUND_MAX = 40;       // ticks
}

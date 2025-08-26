package io.github.pyke.blockdetector.config;

import io.github.pyke.blockdetector.BlockDetector;
import io.wispforest.owo.config.annotation.Config;
import io.wispforest.owo.config.annotation.Modmenu;
import io.wispforest.owo.config.annotation.RangeConstraint;

@Modmenu(modId = BlockDetector.MOD_ID)
@Config(name = "blockdetector", wrapperName = "BlockDetectorConfigWrapper")
public class BlockDetectorConfig {
    // 서버 권위: 분할 스캔 성능
    @RangeConstraint(min = DetectorLimits.SCAN_STEP_MIN, max = DetectorLimits.SCAN_STEP_MAX)
    public int scanStep = 3;
    @RangeConstraint(min = DetectorLimits.SCAN_DURATION_MIN, max = DetectorLimits.SCAN_DURATION_MAX)
    public int scanDurationTicks = 60;

    // 목표 감지 성공 메세지 {DIST} 목표까지의 거리
    public String successMessageTemplate = "탐지기가 목표를 감지하였습니다. ({DIST}m)";
    
    // 손에 들고 있을 때 HUD
    public HoldHud holdHud = new HoldHud();
    public static class HoldHud {
        public boolean enabled = true;
        public String scanningTemplate = "탐색중({PROG}%)";
        public String detectedTemplate = "목표물 감지!";
        public String trackingTemplate = "{DIST}m";
        public String notFoundTemplate = "탐지 범위 내 결과가 없습니다.";
        public String nullTargetTemplate = "검색할 대상이 없습니다.";
        @RangeConstraint(min = DetectorLimits.HUD_INTERVAL_MIN, max = DetectorLimits.HUD_INTERVAL_MAX) public int intervalTicks = 5;
        @RangeConstraint(min = DetectorLimits.HUD_DETECTED_MIN, max = DetectorLimits.HUD_DETECTED_MAX) public int detectedTicks = 40;
        @RangeConstraint(min = DetectorLimits.HUD_NOTFOUND_MIN, max = DetectorLimits.HUD_NOTFOUND_MAX) public int notFoundTicks = 40;
        public int hudPosX = 0;
        public int hudPosY = 59;
    }

    // 아이템 NBT 미지정 시 기본값
    @RangeConstraint(min = DetectorLimits.RANGE_MIN, max = DetectorLimits.RANGE_MAX) public int detectorRange = 64;
    @RangeConstraint(min = DetectorLimits.COOLDOWN_MIN, max = DetectorLimits.COOLDOWN_MAX) public int cooldownTicks = 100;
}

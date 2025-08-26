package io.github.pyke.blockdetector.network;

import io.github.pyke.blockdetector.BlockDetector;
import net.minecraft.resources.ResourceLocation;

public class Network {
    private static ResourceLocation id(String path) { return new ResourceLocation(BlockDetector.MOD_ID, path); }

    // S2C: REQ_SCAN
    public static final ResourceLocation REQ_SCAN = id("req_scan");
    // C2S: RES_SCAN
    public static final ResourceLocation RES_SCAN = id("res_scan");

    private Network() { }
}

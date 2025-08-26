package io.github.pyke.blockdetector;

import io.github.pyke.blockdetector.command.ReloadCommand;
import io.github.pyke.blockdetector.config.ConfigAccess;
import io.github.pyke.blockdetector.item.DetectorCompass;
import io.github.pyke.blockdetector.network.Network;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class BlockDetector implements ModInitializer {
	public static final String MOD_ID = "block-detector";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final String SYSTEM_PREFIX = "§6[SYSTEM] §f";

	@Override
	public void onInitialize() {
        // 아이템 등록
        DetectorCompass.register();

        // 관리자 명령어 등록
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) -> ReloadCommand.register(dispatcher));

        // C2S: 클라이언트 스캔 결과 수신
        ServerPlayNetworking.registerGlobalReceiver(Network.RES_SCAN, (server, player, handler,  buf, responseSender) -> {
            boolean found = buf.readBoolean();
            if (!found) {
                player.displayClientMessage(Component.literal(ConfigAccess.CONFIG.holdHud().notFoundTemplate), true);
                return;
            }

            int x = buf.readInt(), y = buf.readInt(), z = buf.readInt();

            // 수평(XZ) 거리 계산
            Vec3 p = player.position();
            int dist = (int)Math.floor(Math.hypot((x + 0.5) - p.x, (z + 0.5) - p.z));

            // 성공 메시지 출력
            String msg = ConfigAccess.CONFIG.successMessageTemplate().replace("{DIST}", Integer.toString(dist));
            player.displayClientMessage(Component.literal(msg), true);

            // 나침반 바늘 고정(로드스톤 포맷)
            DetectorCompass.updateLodestoneForHeldDetector((ServerPlayer) player, new BlockPos(x, y, z));
        });
	}
}
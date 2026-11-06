package name.modid.client;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import name.modid.client.config.CrystalOptimizerConfig;
import name.modid.client.network.CrystalOptimizerNetwork;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

public class CrystalOptimizerClient implements ClientModInitializer {
	private static boolean serverDisabled = false;
	public static Minecraft mc;
	public static int hitCount = 0;
	public static int breakingBlockTick = 0;

	private static CrystalOptimizerConfig config;
	public static CrystalOptimizerConfig getConfig() {
		return config;
	}

	public static void setServerDisabled(boolean disabled) {
		serverDisabled = disabled;
	}

	public static boolean isServerDisabled() {
		return serverDisabled;
	}

	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
		mc = Minecraft.getInstance();

		config = new CrystalOptimizerConfig();
		config.load();

		CrystalOptimizerNetwork network = new CrystalOptimizerNetwork();
		network.register();

		ClientPlayConnectionEvents.JOIN.register(((_, _, _) -> {
			serverDisabled = false;
			ClientPlayNetworking.send(new CrystalOptimizerNetwork.JoinPayload());
		}));

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
				dispatcher.register(
						LiteralArgumentBuilder.<FabricClientCommandSource>literal("crystaloptimizer")
								.then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("toggle")
										.executes(context -> {
											config.setEnabled(!config.isEnabled());

											context.getSource().sendFeedback(
													Component.literal("crystaloptimizer: " +
															(config.isEnabled() ? "Enabled" : "Disabled"))
											);

											return 1;
										}
								)
						)
				)
		);
	}

	public static void useOwnTicks() {
		if (isServerDisabled()) return;
		if (!config.isEnabled()) return;
		if (mc == null || mc.player == null) return;

		var mainStack = mc.player.getMainHandItem();

		if (mc.options.keyAttack.isDown()) {
			breakingBlockTick++;
		} else {
			breakingBlockTick = 0;
		}

		if (breakingBlockTick > 2) return;

		if (!mc.options.keyUse.isDown()) {
			hitCount = 0;
		}

		if (hitCount == limitPackets()) return;

		if (lookingAtSaidEntity()) {
			if (mc.options.keyAttack.isDown()) {
				if (hitCount >= 1) {
					Entity entity = removeSaidEntity();
					if (entity != null) {
						entity.setRemoved(Entity.RemovalReason.DISCARDED);
					}
				}
				hitCount++;
			}
		}

		if (!mainStack.is(Items.END_CRYSTAL)) return;

		if (mc.options.keyUse.isDown()) {
			if (isLookingAt(Blocks.OBSIDIAN, generalLookPos().getBlockPos())
					|| isLookingAt(Blocks.BEDROCK, generalLookPos().getBlockPos())) {
				sendInteractBlockPacket(generalLookPos().getBlockPos(), generalLookPos().getDirection());
				if (canPlaceCrystalServer(generalLookPos().getBlockPos())) {
					SwingAnimation animation = mainStack.getInteractAnimation();
					mc.player.swing(InteractionHand.MAIN_HAND, animation, false);
				}
			}
		}
	}
	private static BlockState getBlockState(BlockPos pos) {
		assert mc.level != null;
		return mc.level.getBlockState(pos);
	}

	private static boolean isLookingAt(Block block, BlockPos pos) {
		return getBlockState(pos).getBlock() == block;
	}

	private static BlockHitResult generalLookPos() {
		assert mc.player != null;
		Vec3 eyePos = mc.player.getEyePosition();
		Vec3 lookVec = lookVec();
		assert mc.level != null;
		return mc.level.clip(new ClipContext(
				eyePos,
				eyePos.add(lookVec.scale(4.5)),
				ClipContext.Block.COLLIDER,
				ClipContext.Fluid.NONE,
				mc.player
		));
	}

	private static Entity removeSaidEntity() {
		Entity result = null;
		HitResult hitResult = mc.hitResult;
		if (hitResult instanceof EntityHitResult entityHit) {
			Entity entity = entityHit.getEntity();
			if (entity instanceof EndCrystal crystal) {
				result = crystal;
			}
		}
		return result;
	}

	private static boolean lookingAtSaidEntity() {
		HitResult hitResult = mc.hitResult;
		if (hitResult instanceof EntityHitResult entityHit) {
			return entityHit.getEntity() instanceof EndCrystal;
		}
		return false;
	}

	private static Vec3 lookVec() {
		float deg2rad = 0.017453292f;
		float pi = 3.1415927f;
		assert mc.player != null;
		float yawCos = (float) Math.cos(-mc.player.getYRot() * deg2rad - pi);
		float yawSin = (float) Math.sin(-mc.player.getYRot() * deg2rad - pi);
		float pitchCos = -(float) Math.cos(-mc.player.getXRot() * deg2rad);
		float pitchSin = (float) Math.sin(-mc.player.getXRot() * deg2rad);
		return new Vec3(
				yawSin * pitchCos,
				pitchSin,
				yawCos * pitchCos
		).normalize();
	}

	private static void sendInteractBlockPacket(BlockPos pos, Direction direction) {
		Vec3 hitVec = new Vec3(pos.getX(), pos.getY(), pos.getZ());
		setPacket(hitVec, direction);
	}

	private static void setPacket(Vec3 hitVec, Direction direction) {
		BlockPos blockPos = new BlockPos((int) hitVec.x, (int) hitVec.y, (int) hitVec.z);
		BlockHitResult blockHitResult = new BlockHitResult(hitVec, direction, blockPos, false);
		assert mc.gameMode != null;
		assert mc.player != null;
		mc.gameMode.useItemOn(
				mc.player,
				InteractionHand.MAIN_HAND,
				blockHitResult
		);
	}

	public static int limitPackets() {
		int limit = 2;
		if (getPing() > 50) {
			return limit;
		}
		if (getPing() < 50) {
			limit = 1;
		}
		return limit;
	}

	private static int getPing() {
		if (mc == null || mc.getConnection() == null) return 0;
		assert mc.player != null;
		var playerInfo = mc.getConnection().getPlayerInfo(mc.player.getUUID());
		if (playerInfo == null) return 0;
		return playerInfo.getLatency();
	}

	private static boolean canPlaceCrystalServer(BlockPos pos) {
		assert mc.level != null;
		BlockState state = mc.level.getBlockState(pos);
		if (!state.is(Blocks.OBSIDIAN) && !state.is(Blocks.BEDROCK)) {
			return false;
		}

		BlockPos above = pos.above();
		if (!mc.level.isEmptyBlock(above)) return false;

		double x = above.getX();
		double y = above.getY();
		double z = above.getZ();

		var entities = mc.level.getEntities(null, new AABB(x, y, z, x + 1, y + 2, z + 1));
		return entities.isEmpty();
	}
}
package name.modid.client;

import name.modid.client.config.CrystalOptimizerConfig;
import name.modid.client.network.CrystalOptimizerNetwork;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class CrystalOptimizerClient implements ClientModInitializer {
	public static MinecraftClient mc;
	public static boolean serverOptedOut = false;
	public static int hitCount = 0;
	public static int breakingBlockTick = 0;

	private static CrystalOptimizerConfig config;

	public static CrystalOptimizerConfig getConfig() {
		return config;
	}

    @Override
	public void onInitializeClient() {
		mc = MinecraftClient.getInstance();

		config = new CrystalOptimizerConfig();
		config.load();

		CrystalOptimizerNetwork network = new CrystalOptimizerNetwork();
		network.register();

		ClientPlayConnectionEvents.JOIN.register(((handler, sender, client) -> {
			serverOptedOut = false;
			ClientPlayNetworking.send(new CrystalOptimizerNetwork.JoinPayload());
		}));

		ClientCommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess) ->
				dispatcher.register(ClientCommandManager.literal("crystaloptimizer")
						.then(ClientCommandManager.literal("toggle")
								.executes(context -> {
									config.setEnabled(!config.isEnabled());

									context.getSource().sendFeedback(Text.literal("crystaloptimizer:" + (config.isEnabled() ? "Enabled" : "Disabled")));
									return 1;
								})))));
	}

	public static void useOwnTicks() {
		if (serverOptedOut) return;

		if (!config.isEnabled()) return;

		if (mc == null || mc.player == null) return;

		var mainStack = mc.player.getMainHandStack();

		if (mc.options.attackKey.isPressed()) {
			breakingBlockTick++;
		} else {
			breakingBlockTick = 0;
		}

		if (breakingBlockTick > 2) return;

		if (!mc.options.useKey.isPressed()) {
			hitCount = 0;
		}

		if (hitCount == limitPackets()) return;

		if (lookingAtSaidEntity()) {
			if (mc.options.attackKey.isPressed()) {
				if (hitCount >= 1) {
					Entity entity = removeSaidEntity();
					if (entity != null) {
						entity.setRemoved(Entity.RemovalReason.DISCARDED);
					}
				}
				hitCount++;
			}
		}

		if (!mainStack.isOf(Items.END_CRYSTAL)) return;

		if (mc.options.useKey.isPressed()) {
			if (isLookingAt(Blocks.OBSIDIAN, generalLookPos().getBlockPos())
					|| isLookingAt(Blocks.BEDROCK, generalLookPos().getBlockPos())) {
				sendInteractBlockPacket(generalLookPos().getBlockPos(), generalLookPos().getSide());
				if (canPlaceCrystalServer(generalLookPos().getBlockPos())) {
					mc.player.swingHand(mc.player.getActiveHand());
				}
			}
		}
	}

	private static BlockState getBlockState(BlockPos pos) {
        assert mc.world != null;
        return mc.world.getBlockState(pos);
	}

	private static boolean isLookingAt(Block block, BlockPos pos) {
		return getBlockState(pos).getBlock() == block;
	}

	private static BlockHitResult generalLookPos() {
        assert mc.player != null;
        Vec3d eyePos = mc.player.getEyePos();
		Vec3d lookVec = lookVec();
        assert mc.world != null;
        return mc.world.raycast(new net.minecraft.world.RaycastContext(
				eyePos,
				eyePos.add(lookVec.multiply(4.5)),
				net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
				net.minecraft.world.RaycastContext.FluidHandling.NONE,
				mc.player
		));
	}

	private static Entity removeSaidEntity() {
		Entity result = null;
		HitResult hitResult = mc.crosshairTarget;
		if (hitResult instanceof EntityHitResult entityHit) {
			Entity entity = entityHit.getEntity();
			if (entity instanceof EndCrystalEntity crystal) {
				result = crystal;
			}
		}
		return result;
	}

	private static boolean lookingAtSaidEntity() {
		HitResult hitResult = mc.crosshairTarget;
		if (hitResult instanceof EntityHitResult entityHit) {
			return entityHit.getEntity() instanceof EndCrystalEntity;
		}
		return false;
	}

	private static Vec3d lookVec() {
		float deg2rad = 0.017453292f;
		float pi = 3.1415927f;
        assert mc.player != null;
        float yawCos = (float) Math.cos(-mc.player.getYaw() * deg2rad - pi);
		float yawSin = (float) Math.sin(-mc.player.getYaw() * deg2rad - pi);
		float pitchCos = -(float) Math.cos(-mc.player.getPitch() * deg2rad);
		float pitchSin = (float) Math.sin(-mc.player.getPitch() * deg2rad);
		return new Vec3d(
				yawSin * pitchCos,
				pitchSin,
				yawCos * pitchCos
		).normalize();
	}

	private static void sendInteractBlockPacket(BlockPos pos, Direction direction) {
		Vec3d hitVec = new Vec3d(pos.getX(), pos.getY(), pos.getZ());
		setPacket(hitVec, direction);
	}

	private static void setPacket(Vec3d hitVec, Direction direction) {
		BlockPos blockPos = new BlockPos((int) hitVec.x, (int) hitVec.y, (int) hitVec.z);
		BlockHitResult blockHitResult = new BlockHitResult(hitVec, direction, blockPos, false);
        assert mc.interactionManager != null;
        assert mc.player != null;
		mc.interactionManager.interactBlock(
				mc.player,
				mc.player.getActiveHand(),
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
		if (mc == null || mc.getNetworkHandler() == null) return 0;
        assert mc.player != null;
        var playerInfo = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
		if (playerInfo == null) return 0;
		return playerInfo.getLatency();
	}

	private static boolean canPlaceCrystalServer(BlockPos pos) {
        assert mc.world != null;
        BlockState state = mc.world.getBlockState(pos);
		if (!state.isOf(Blocks.OBSIDIAN) && !state.isOf(Blocks.BEDROCK)) {
			return false;
		}

		BlockPos above = pos.up();
		if (!mc.world.isAir(above)) return false;

		double x = above.getX();
		double y = above.getY();
		double z = above.getZ();

		var entities = mc.world.getOtherEntities(null, new Box(x, y, z, x + 1, y + 2, z + 1));
		return entities.isEmpty();
	}
}

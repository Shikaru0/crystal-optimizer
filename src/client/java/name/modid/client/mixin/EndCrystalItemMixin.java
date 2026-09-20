package name.modid.client.mixin;

import name.modid.client.CrystalOptimizerClient;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({net.minecraft.item.EndCrystalItem.class})
public class EndCrystalItemMixin {
    @Inject(method = {"useOnBlock"}, at = {@At("HEAD")}, cancellable = true)
    private void modifyDecrementAmount(net.minecraft.item.ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        if (CrystalOptimizerClient.serverOptedOut) return;

        assert MinecraftClient.getInstance().player != null;
        var mainStack = MinecraftClient.getInstance().player.getMainHandStack();
        if (mainStack.isOf(Items.END_CRYSTAL)) {
            if (isLookingAt(Blocks.OBSIDIAN, generalLookPos().getBlockPos()) || isLookingAt(Blocks.BEDROCK, generalLookPos().getBlockPos())) {
                var hitResult = MinecraftClient.getInstance().crosshairTarget;
                if (hitResult instanceof BlockHitResult blockHit) {
                    BlockPos blockPos = blockHit.getBlockPos();
                    if (canPlaceCrystalServer(blockPos)) {
                        context.getStack().decrement(-1);
                    }
                }
            }
        }
    }

    private BlockState getBlockState(BlockPos pos) {
        assert MinecraftClient.getInstance().world != null;
        return MinecraftClient.getInstance().world.getBlockState(pos);
    }

    private boolean isLookingAt(Block block, BlockPos pos) {
        return getBlockState(pos).getBlock() == block;
    }

    private BlockHitResult generalLookPos() {
        MinecraftClient mc = MinecraftClient.getInstance();
        assert mc.player != null;
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d lookVec = lookVec();
        assert mc.world != null;
        return mc.world.raycast(new RaycastContext(
                eyePos,
                eyePos.add(lookVec.multiply(4.5)),
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));
    }

    private Vec3d lookVec() {
        MinecraftClient mc = MinecraftClient.getInstance();
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

    private boolean canPlaceCrystalServer(BlockPos pos) {
        MinecraftClient mc = MinecraftClient.getInstance();
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
        var entities = mc.world.getOtherEntities(null, new net.minecraft.util.math.Box(x, y, z, x + 1, y + 2, z + 1));
        return entities.isEmpty();
    }
}
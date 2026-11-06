package name.modid.client.mixin;

import name.modid.client.CrystalOptimizerClient;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.EndCrystalItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({EndCrystalItem.class})
public class EndCrystalItemMixin {
    @Inject(method = {"useOn"}, at = {@At("HEAD")}, cancellable = true)
    private void modifyDecrementAmount(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (CrystalOptimizerClient.isServerDisabled()) return;
        if (!CrystalOptimizerClient.getConfig().isEnabled()) return;

        assert Minecraft.getInstance().player != null;
        var mainStack = Minecraft.getInstance().player.getMainHandItem();
        if (mainStack.is(Items.END_CRYSTAL)) {
            if (isLookingAt(Blocks.OBSIDIAN, generalLookPos().getBlockPos()) || isLookingAt(Blocks.BEDROCK, generalLookPos().getBlockPos())) {
                var hitResult = Minecraft.getInstance().hitResult;
                if (hitResult instanceof BlockHitResult blockHit) {
                    BlockPos blockPos = blockHit.getBlockPos();
                    if (canPlaceCrystalServer(blockPos)) {
                        context.getItemInHand().shrink(-1);
                    }
                }
            }
        }
    }

    private BlockState getBlockState(BlockPos pos) {
        assert Minecraft.getInstance().level != null;
        return Minecraft.getInstance().level.getBlockState(pos);
    }

    private boolean isLookingAt(Block block, BlockPos pos) {
        return getBlockState(pos).getBlock() == block;
    }

    private BlockHitResult generalLookPos() {
        Minecraft mc = Minecraft.getInstance();
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

    private Vec3 lookVec() {
        Minecraft mc = Minecraft.getInstance();
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

    private boolean canPlaceCrystalServer(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        assert mc.level != null;
        BlockState state = mc.level.getBlockState(pos);
        if (!state.is(Blocks.OBSIDIAN) && !state.is(Blocks.BEDROCK)) {
            return false;
        }
        BlockPos above = pos.above();
        if (!mc.level.getBlockState(above).isAir()) return false;
        double x = above.getX();
        double y = above.getY();
        double z = above.getZ();
        var entities = mc.level.getEntities(null, new AABB(x, y, z, x+1, y + 2, z + 1));
        return entities.isEmpty();
    }
}
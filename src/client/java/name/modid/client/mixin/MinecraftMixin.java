package name.modid.client.mixin;

import name.modid.client.CrystalOptimizerClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Minecraft.class})
public abstract class MinecraftMixin {
    @Inject(method = {"startUseItem"}, at = {@At("HEAD")}, cancellable = true)
    private void onDoItemUse(CallbackInfo ci) {
        if (CrystalOptimizerClient.isServerDisabled()) return;
        if (!CrystalOptimizerClient.getConfig().isEnabled()) return;
        if (Minecraft.getInstance().player == null) return;
        ItemStack mainStack = Minecraft.getInstance().player.getMainHandItem();
        if (mainStack.is(Items.END_CRYSTAL)) {
            if (CrystalOptimizerClient.hitCount != CrystalOptimizerClient.limitPackets()) {
                ci.cancel();
            }
        }
    }
}
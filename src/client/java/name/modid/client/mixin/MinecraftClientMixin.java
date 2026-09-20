package name.modid.client.mixin;

import name.modid.client.CrystalOptimizerClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({MinecraftClient.class})
public abstract class MinecraftClientMixin {
    @Inject(method = {"doItemUse"}, at = {@At("HEAD")}, cancellable = true)
    private void onDoItemUse(CallbackInfo ci) {
        if (CrystalOptimizerClient.serverOptedOut) return;
        if (!CrystalOptimizerClient.getConfig().isEnabled()) return;
        if (MinecraftClient.getInstance().player == null) return;
        ItemStack mainStack = MinecraftClient.getInstance().player.getMainHandStack();
        if (mainStack.isOf(Items.END_CRYSTAL)) {
            if (CrystalOptimizerClient.hitCount != CrystalOptimizerClient.limitPackets()) {
                ci.cancel();
            }
        }
    }
}

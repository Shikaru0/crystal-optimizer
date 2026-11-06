package name.modid.client.mixin;

import name.modid.client.CrystalOptimizerClient;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
    @Inject(method = {"tick"}, at = {@At("HEAD")})
    private void useOwnTicks(CallbackInfo ci) {
        CrystalOptimizerClient.useOwnTicks();
    }
}
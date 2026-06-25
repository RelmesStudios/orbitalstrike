package pier4y.orbitalstrike.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerExplosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pier4y.orbitalstrike.OrbitalStrikeTntAccessor;
import pier4y.orbitalstrike.OrbitalStrikeExplosionTracker;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Mixin(ServerExplosion.class)
public class ServerExplosionMixin {
    @Shadow @Final private Entity source;

    @Inject(method = "explode", at = @At("HEAD"))
    private void onExplodeHead(CallbackInfoReturnable<Integer> cir) {
        if (this.source instanceof PrimedTnt tnt && ((OrbitalStrikeTntAccessor) tnt).orbitalstrike$isOrbitalStrike()) {
            OrbitalStrikeExplosionTracker.setOrbitalStrikeActive(true);
        }
    }

    @Inject(method = "explode", at = @At("RETURN"))
    private void onExplodeReturn(CallbackInfoReturnable<Integer> cir) {
        OrbitalStrikeExplosionTracker.setOrbitalStrikeActive(false);
    }

    @Inject(method = "addOrAppendStack", at = @At("HEAD"))
    private static void onAddOrAppendStack(List<ItemStack> list, ItemStack stack, BlockPos pos, CallbackInfo ci) {
        if (OrbitalStrikeExplosionTracker.isOrbitalStrikeActive()) {
            int originalCount = stack.getCount();
            int kept = 0;
            for (int i = 0; i < originalCount; i++) {
                if (ThreadLocalRandom.current().nextFloat() < 0.25f) {
                    kept++;
                }
            }
            stack.setCount(kept);
        }
    }
}

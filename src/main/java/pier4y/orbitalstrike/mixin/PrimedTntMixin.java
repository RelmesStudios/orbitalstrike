package pier4y.orbitalstrike.mixin;

import net.minecraft.world.entity.item.PrimedTnt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import pier4y.orbitalstrike.OrbitalStrikeTntAccessor;

@Mixin(PrimedTnt.class)
public class PrimedTntMixin implements OrbitalStrikeTntAccessor {

    @Unique
    private boolean orbitalstrike$isOrbitalStrike = false;

    @Override
    public boolean orbitalstrike$isOrbitalStrike() {
        return this.orbitalstrike$isOrbitalStrike;
    }

    @Override
    public void orbitalstrike$setOrbitalStrike(boolean value) {
        this.orbitalstrike$isOrbitalStrike = value;
    }
}

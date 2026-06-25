package pier4y.orbitalstrike;

public class OrbitalStrikeExplosionTracker {
    private static final ThreadLocal<Boolean> ORBITAL_STRIKE_ACTIVE = ThreadLocal.withInitial(() -> false);

    public static void setOrbitalStrikeActive(boolean active) {
        ORBITAL_STRIKE_ACTIVE.set(active);
    }

    public static boolean isOrbitalStrikeActive() {
        return ORBITAL_STRIKE_ACTIVE.get();
    }
}

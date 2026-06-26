package pier4y.orbitalstrike.strike;

import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import pier4y.orbitalstrike.OrbitalStrikeTntAccessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class StrikeManager {
    private static final Random RANDOM = new Random();

    // =========================================================================
    // NUKE CONFIGURATION
    // =========================================================================

    // Number of rings
    private static final int NUKE_RING_COUNT = 10;

    // Radius of each ring in blocks (index 0 = ring 1, index 9 = ring 10)
    private static final double[] RING_RADII = {
        6.0,   // Ring 1
        11.0,  // Ring 2
        16.0,  // Ring 3
        21.0,  // Ring 4
        26.0,  // Ring 5
        31.0,  // Ring 6
        36.0,  // Ring 7
        41.0,  // Ring 8
        46.0,  // Ring 9
        51.0,  // Ring 10
    };

    // TNT count per ring (must match RING_RADII length)
    private static final int[] RING_TNT_COUNTS = {
        15,    // Ring 1  — radius 5
        27,    // Ring 2  — radius 10
        38,    // Ring 3  — radius 15
        49,    // Ring 4  — radius 20
        62,    // Ring 5  — radius 25
        73,    // Ring 6  — radius 30
        83,    // Ring 7  — radius 35
        95,    // Ring 8  — radius 40
        107,   // Ring 9  — radius 45
        119,   // Ring 10 — radius 50
        // Total: 660 TNT + 1 center = 661
    };

    // Random misalignment applied to each TNT's radius (±half this value in blocks)
    private static final float NUKE_MISALIGN_RANGE = 0.5f;

    // Height above target to spawn all nuke TNT
    private static final double NUKE_SPAWN_HEIGHT = 72.0;

    // Fuse in ticks for all nuke TNT (must match drag scale factor below)
    private static final int NUKE_FUSE_TICKS = 79;

    // Drag scale factor for NUKE_FUSE_TICKS ticks at 0.98 drag per tick:
    // d = v_initial * (1 - 0.98^N) / 0.02
    private static final double SCALE_FACTOR = (1.0 - Math.pow(0.98, NUKE_FUSE_TICKS)) / 0.02;

    // =========================================================================
    // STAB CONFIGURATION
    // =========================================================================

    // TNT spawned per step (stacked vertically within the step)
    private static final int STAB_TNT_PER_STEP = 5;

    // Vertical gap between each step in blocks (column density)
    private static final int STAB_STEP_SIZE = 4;

    // Vertical offset between each TNT within a step (spread them inside the step)
    // e.g. 2 TNT with offset 0.75 → placed at y+0.5 and y-0.25
    private static final double STAB_TNT_VERTICAL_OFFSET = 0.75;

    // Fuse in ticks for stab TNT
    private static final int STAB_FUSE_TICKS = 1;

    // Blast resistance at or above which the stab column stops entirely
    // 1200.0 = obsidian-tier (obsidian, crying obsidian, ancient debris, reinforced deepslate)
    private static final float BLAST_RESISTANCE_STOP_THRESHOLD = 1200.0f;

    // =========================================================================
    // NUKE SHOT
    // =========================================================================

    public static void spawnNuke(ServerLevel world, BlockPos targetPos) {
        double spawnX = targetPos.getX() + 0.5;
        double spawnY = targetPos.getY() + NUKE_SPAWN_HEIGHT;
        double spawnZ = targetPos.getZ() + 0.5;

        // Center TNT — stationary
        PrimedTnt centerTnt = new PrimedTnt(world, spawnX, spawnY, spawnZ, null);
        ((OrbitalStrikeTntAccessor) centerTnt).orbitalstrike$setOrbitalStrike(true);
        centerTnt.setFuse(NUKE_FUSE_TICKS);
        centerTnt.setDeltaMovement(0, 0, 0);
        world.addFreshEntity(centerTnt);

        for (int i = 0; i < NUKE_RING_COUNT; i++) {
            double baseRadius = RING_RADII[i];
            int tntCount = RING_TNT_COUNTS[i];

            for (int j = 0; j < tntCount; j++) {
                double angle = (2 * Math.PI * j) / tntCount;

                // Misalignment is constrained to ±half of NUKE_MISALIGN_RANGE so TNT
                // always lands within the declared ring band.
                double halfRange = NUKE_MISALIGN_RANGE / 2.0;
                double misalignment = (RANDOM.nextFloat() - 0.5f) * NUKE_MISALIGN_RANGE;
                misalignment = Math.max(-halfRange, Math.min(halfRange, misalignment));
                double targetRadius = baseRadius + misalignment;

                double vInitial = targetRadius / SCALE_FACTOR;
                double vx = vInitial * Math.cos(angle);
                double vz = vInitial * Math.sin(angle);

                PrimedTnt ringTnt = new PrimedTnt(world, spawnX, spawnY, spawnZ, null);
                ((OrbitalStrikeTntAccessor) ringTnt).orbitalstrike$setOrbitalStrike(true);
                ringTnt.setFuse(NUKE_FUSE_TICKS);
                ringTnt.setDeltaMovement(vx, 0, vz);
                world.addFreshEntity(ringTnt);
            }
        }
    }

    // =========================================================================
    // STAB SHOT
    // =========================================================================

    public static void spawnStab(ServerLevel world, BlockPos targetPos) {
        int x = targetPos.getX();
        int z = targetPos.getZ();

        // Start 5 blocks above the targeted block
        int startY = targetPos.getY() + 5;
        int minY = world.getMinY();

        for (int y = startY; y >= minY; y -= STAB_STEP_SIZE) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = world.getBlockState(pos);

            boolean blastResistant = state.getBlock().getExplosionResistance() >= BLAST_RESISTANCE_STOP_THRESHOLD;

            // Spawn STAB_TNT_PER_STEP TNT evenly offset within the step
            for (int t = 0; t < STAB_TNT_PER_STEP; t++) {
                double tntY = y + 0.5 - (t * STAB_TNT_VERTICAL_OFFSET);
                PrimedTnt tnt = new PrimedTnt(world, x + 0.5, tntY, z + 0.5, null);
                ((OrbitalStrikeTntAccessor) tnt).orbitalstrike$setOrbitalStrike(true);
                tnt.setFuse(STAB_FUSE_TICKS);
                tnt.setNoGravity(true);
                world.addFreshEntity(tnt);
            }

            // Stop column after spawning on blast-resistant block — nothing below will break anyway
            if (blastResistant) {
                break;
            }
        }
    }

    // =========================================================================
    // PENDING STRIKE QUEUE — used for delayed TNT spawns (e.g. fishing rod cast delay)
    // =========================================================================

    private record PendingStrike(ServerLevel world, BlockPos pos, String type, long fireAtTick) {}

    private static final List<PendingStrike> pendingStrikes = new ArrayList<>();
    private static long currentTick = 0;

    /**
     * Queues a strike to fire after {@code delayTicks} server ticks.
     */
    public static void queueStrike(ServerLevel world, BlockPos pos, String type, int delayTicks) {
        pendingStrikes.add(new PendingStrike(world, pos, type, currentTick + delayTicks));
    }

    public static void tick() {
        currentTick++;
        if (pendingStrikes.isEmpty()) return;
        List<PendingStrike> fired = new ArrayList<>();
        for (PendingStrike s : pendingStrikes) {
            if (s.fireAtTick() <= currentTick) {
                if ("nuke".equalsIgnoreCase(s.type())) spawnNuke(s.world(), s.pos());
                else if ("stab".equalsIgnoreCase(s.type())) spawnStab(s.world(), s.pos());
                fired.add(s);
            }
        }
        pendingStrikes.removeAll(fired);
    }
}
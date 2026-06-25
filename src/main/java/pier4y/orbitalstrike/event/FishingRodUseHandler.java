package pier4y.orbitalstrike.event;

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import pier4y.orbitalstrike.strike.StrikeManager;

public class FishingRodUseHandler {

    // Delay from bobber cast to TNT spawn — exactly 0.5 seconds at 20 TPS
    private static final int CAST_DELAY_TICKS = 10;

    public static void register() {
        UseItemCallback.EVENT.register(FishingRodUseHandler::onUseItem);
    }

    private static InteractionResult onUseItem(Player player, Level world, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!stack.is(Items.FISHING_ROD)) return InteractionResult.PASS;

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return InteractionResult.PASS;

        CompoundTag nbt = customData.copyTag();
        boolean isStrikeRod = nbt.getBoolean("OrbitalStrikeRod").orElse(false);
        if (!isStrikeRod) return InteractionResult.PASS;

        String type = nbt.getString("StrikeType").orElse("");

        // Raycast to get target position at cast time
        HitResult hit = player.pick(120.0, 1.0f, false);
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResult.PASS;

        BlockPos targetPos = ((BlockHitResult) hit).getBlockPos();

        if (!world.isClientSide()) {
            ServerLevel serverWorld = (ServerLevel) world;

            // Break the rod immediately on cast
            serverWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
            stack.shrink(1);

            // Queue strike to fire exactly 0.5s (10 ticks) after cast
            // If the rod type is "strike", it triggers a nuke.
            String resolvedType = "strike".equalsIgnoreCase(type) ? "nuke" : type;
            StrikeManager.queueStrike(serverWorld, targetPos, resolvedType, CAST_DELAY_TICKS);
        }

        return world.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }
}

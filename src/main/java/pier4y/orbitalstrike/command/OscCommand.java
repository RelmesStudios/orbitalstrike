package pier4y.orbitalstrike.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.server.players.NameAndId;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.FontDescription;

import net.minecraft.core.BlockPos;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class OscCommand {

    // =========================================================================
    // Debug toggle — tracks which operators have verbose output ON
    // =========================================================================
    private static final Set<UUID> debugEnabled = new HashSet<>();

    private static boolean isDebug(CommandSourceStack source) {
        try {
            ServerPlayer p = source.getPlayerOrException();
            return debugEnabled.contains(p.getUUID());
        } catch (CommandSyntaxException e) {
            // Console sender — always show debug
            return true;
        }
    }

    // =========================================================================
    // Unicode small-caps mapper  (ᴘʀᴏᴄᴇѕѕɪɴɢ style)
    // =========================================================================
    private static final String NORMAL  = "abcdefghijklmnopqrstuvwxyz";
    private static final String SMALL_CAPS =
        "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀsᴛᴜᴠᴡxʏᴢ";

    private static String toSmallCaps(String input) {
        StringBuilder sb = new StringBuilder(input.length());
        for (char c : input.toLowerCase().toCharArray()) {
            int idx = NORMAL.indexOf(c);
            sb.append(idx >= 0 ? SMALL_CAPS.charAt(idx) : c);
        }
        return sb.toString();
    }

    // =========================================================================
    // Styled text helpers
    // =========================================================================
    private static MutableComponent prefix() {
        return Component.empty()
            .append(Component.literal("[").withStyle(ChatFormatting.DARK_GRAY))
            .append(Component.literal("OSC").withStyle(style -> style
                .withColor(0x8B0000)
                .withBold(true)))
            .append(Component.literal("] ").withStyle(ChatFormatting.DARK_GRAY));
    }

    /** Small-caps text with ChatFormatting colour */
    private static MutableComponent sc(String text, ChatFormatting colour) {
        return Component.literal(toSmallCaps(text)).withStyle(colour);
    }

    /** Small-caps text with hex colour */
    private static MutableComponent sc(String text, int hex) {
        return Component.literal(toSmallCaps(text)).withStyle(style -> style.withColor(hex));
    }

    private static int strikeColor(String type) {
        return "nuke".equalsIgnoreCase(type) ? 0xFF4500 : 0x00BFFF;
    }

    private static MutableComponent strikeLabel(String type) {
        return Component.literal(toSmallCaps(type + " shot")).withStyle(style -> style
            .withColor(strikeColor(type))
            .withBold(true));
    }

    // =========================================================================
    // Registration
    // =========================================================================
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
                                CommandBuildContext registryAccess,
                                Commands.CommandSelection environment) {

        dispatcher.register(Commands.literal("osc")
            .requires(source -> source.getServer() == null || source.getPlayer() == null ||
                source.getServer().getPlayerList().isOp(
                    new NameAndId(source.getPlayer().getUUID(),
                                  source.getPlayer().getGameProfile().name())))

            // ── /osc debug ────────────────────────────────────────────────────
            .then(Commands.literal("debug")
                .executes(OscCommand::runDebugToggle))

            // ── /osc give <type> <targets> [count] ────────────────────────────
            .then(Commands.literal("give")
                .then(Commands.argument("type", StringArgumentType.word())
                    .suggests((context, builder) -> {
                        builder.suggest("nuke");
                        builder.suggest("stab");
                        builder.suggest("strike");
                        return builder.buildFuture();
                    })
                    .then(Commands.argument("targets", EntityArgument.players())
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                            .executes(OscCommand::runGiveCommand))
                        .executes(ctx -> runGiveCommandWithCount(ctx, 1))
                    )
                )
            )

            // ── /osc strike <type> <x> <y> <z> ───────────────────────────────
            .then(Commands.literal("strike")
                .then(Commands.argument("type", StringArgumentType.word())
                    .suggests((context, builder) -> {
                        builder.suggest("nuke");
                        builder.suggest("stab");
                        return builder.buildFuture();
                    })
                    .then(Commands.argument("x", IntegerArgumentType.integer())
                        .then(Commands.argument("y", IntegerArgumentType.integer())
                            .then(Commands.argument("z", IntegerArgumentType.integer())
                                .executes(OscCommand::runStrikeCommand)
                            )
                        )
                    )
                )
            )
        );
    }

    // =========================================================================
    // /osc debug toggle
    // =========================================================================
    private static int runDebugToggle(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        UUID uuid = null;
        try {
            uuid = source.getPlayerOrException().getUUID();
        } catch (CommandSyntaxException e) {
            // Console — inform that console always shows debug
            source.sendSystemMessage(Component.empty()
                .append(prefix())
                .append(sc("console always receives debug output.", 0xAAAAAA)));
            return 1;
        }

        boolean nowEnabled;
        if (debugEnabled.contains(uuid)) {
            debugEnabled.remove(uuid);
            nowEnabled = false;
        } else {
            debugEnabled.add(uuid);
            nowEnabled = true;
        }

        source.sendSystemMessage(Component.empty()
            .append(prefix())
            .append(sc("debug output ", 0xAAAAAA))
            .append(nowEnabled
                ? sc("enabled", ChatFormatting.GREEN)
                : sc("disabled", ChatFormatting.RED))
            .append(sc(".", 0xAAAAAA)));

        return 1;
    }


    // =========================================================================
    // /osc give execution
    // =========================================================================
    private static int runGiveCommand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        int count = IntegerArgumentType.getInteger(context, "count");
        return runGiveCommandWithCount(context, count);
    }

    private static int runGiveCommandWithCount(CommandContext<CommandSourceStack> context, int count)
            throws CommandSyntaxException {

        CommandSourceStack source = context.getSource();
        String type  = StringArgumentType.getString(context, "type");
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        boolean debug = isDebug(source);

        // ── Validate type ──────────────────────────────────────────────────────
        if (!"nuke".equalsIgnoreCase(type) && !"stab".equalsIgnoreCase(type) && !"strike".equalsIgnoreCase(type)) {
            source.sendFailure(Component.empty()
                .append(prefix())
                .append(sc("invalid strike type ", ChatFormatting.RED))
                .append(Component.literal("'" + type + "'").withStyle(ChatFormatting.YELLOW))
                .append(sc(" — must be ", ChatFormatting.RED))
                .append(Component.literal("nuke").withStyle(style -> style.withColor(0xFF4500).withBold(true)))
                .append(sc(", ", ChatFormatting.GRAY))
                .append(Component.literal("stab").withStyle(style -> style.withColor(0x00BFFF).withBold(true)))
                .append(sc(" or ", ChatFormatting.GRAY))
                .append(Component.literal("strike").withStyle(style -> style.withColor(0xFFFF00).withBold(true))));
            return 0;
        }

        // ── Debug: processing notice ───────────────────────────────────────────
        if (debug) {
            source.sendSystemMessage(Component.empty()
                .append(prefix())
                .append(sc("processing ", 0x888888))
                .append(strikeLabel(type))
                .append(sc(" x" + count + " for " + targets.size() + " player(s)...", 0x888888)));
        }

        int totalGiven = 0;

        for (ServerPlayer player : targets) {
            int given   = 0;
            int dropped = 0;

            for (int i = 0; i < count; i++) {
                ItemStack rod = buildRod(type);
                if (player.getInventory().add(rod)) {
                    given++;
                } else {
                    player.drop(rod, false);
                    dropped++;
                }
            }

            totalGiven += given + dropped;

            // ── Per-player debug to sender ─────────────────────────────────────
            if (debug) {
                MutableComponent line = Component.empty()
                    .append(prefix())
                    .append(sc("-> ", 0x555555))
                    .append(Component.literal(player.getGameProfile().name()).withStyle(ChatFormatting.WHITE))
                    .append(sc(": ", 0x555555))
                    .append(sc("inventory +" + given, 0x55FF55));

                if (dropped > 0) {
                    line.append(sc(", ", 0x555555))
                        .append(sc("dropped +" + dropped, 0xFFAA00));
                }

                source.sendSystemMessage(line);
            }

            // ── Notification to the receiving player ───────────────────────────
            player.sendSystemMessage(Component.empty()
                .append(prefix())
                .append(sc("you have received ", 0xAAAAAA))
                .append(Component.literal("" + (given + dropped)).withStyle(ChatFormatting.WHITE))
                .append(sc("x ", 0xAAAAAA))
                .append(strikeLabel(type))
                .append(sc(" rod" + (given + dropped != 1 ? "s" : ""), 0xAAAAAA)));

            if (dropped > 0) {
                player.sendSystemMessage(Component.empty()
                    .append(prefix())
                    .append(sc("inventory full — ", 0xFFAA00))
                    .append(sc("" + dropped + " rod" + (dropped != 1 ? "s" : "") + " dropped at your feet", 0xFFDD88)));
            }
        }

        // ── Final success summary ──────────────────────────────────────────────
        final int finalTotal = totalGiven;
        source.sendSuccess(() -> Component.empty()
            .append(prefix())
            .append(sc("issued ", 0xAAAAAA))
            .append(Component.literal("" + finalTotal).withStyle(ChatFormatting.WHITE))
            .append(sc("x ", 0xAAAAAA))
            .append(strikeLabel(type))
            .append(sc(" rod" + (finalTotal != 1 ? "s" : "") + " to ", 0xAAAAAA))
            .append(Component.literal("" + targets.size()).withStyle(ChatFormatting.WHITE))
            .append(sc(" player" + (targets.size() != 1 ? "s" : ""), 0xAAAAAA)), true);

        return totalGiven;
    }

    // =========================================================================
    // /osc strike execution
    // =========================================================================
    private static int runStrikeCommand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String type = StringArgumentType.getString(context, "type");
        int x = IntegerArgumentType.getInteger(context, "x");
        int y = IntegerArgumentType.getInteger(context, "y");
        int z = IntegerArgumentType.getInteger(context, "z");

        // ── Validate type ──────────────────────────────────────────────────────
        if (!"nuke".equalsIgnoreCase(type) && !"stab".equalsIgnoreCase(type) && !"strike".equalsIgnoreCase(type)) {
            source.sendFailure(Component.empty()
                .append(prefix())
                .append(sc("invalid strike type ", ChatFormatting.RED))
                .append(Component.literal("'" + type + "'").withStyle(ChatFormatting.YELLOW))
                .append(sc(" — must be ", ChatFormatting.RED))
                .append(Component.literal("nuke").withStyle(style -> style.withColor(0xFF4500).withBold(true)))
                .append(sc(" or ", ChatFormatting.GRAY))
                .append(Component.literal("stab").withStyle(style -> style.withColor(0x00BFFF).withBold(true))));
            return 0;
        }

        net.minecraft.server.level.ServerLevel world = source.getLevel();
        BlockPos targetPos = new BlockPos(x, y, z);

        if ("nuke".equalsIgnoreCase(type) || "strike".equalsIgnoreCase(type)) {
            pier4y.orbitalstrike.strike.StrikeManager.spawnNuke(world, targetPos);
        } else if ("stab".equalsIgnoreCase(type)) {
            pier4y.orbitalstrike.strike.StrikeManager.spawnStab(world, targetPos);
        }

        source.sendSuccess(() -> Component.empty()
            .append(prefix())
            .append(sc("triggered ", 0xAAAAAA))
            .append(strikeLabel(type))
            .append(sc(" at ", 0xAAAAAA))
            .append(Component.literal(String.format("%d, %d, %d", x, y, z)).withStyle(ChatFormatting.WHITE)), true);

        return 1;
    }

    // =========================================================================
    // Rod builder
    // =========================================================================
    private static ItemStack buildRod(String type) {
        ItemStack rod = new ItemStack(Items.FISHING_ROD);

        rod.setDamageValue(rod.getMaxDamage() - 4);

        // Plain white italic name — no colour, no custom font
        MutableComponent name = Component.literal(type.toLowerCase() + " shot")
            .withStyle(style -> style
                .withItalic(true)
                .withColor(ChatFormatting.WHITE));
        rod.set(DataComponents.CUSTOM_NAME, name);

        CompoundTag nbt = new CompoundTag();
        nbt.putBoolean("OrbitalStrikeRod", true);
        nbt.putString("StrikeType", type.toLowerCase());
        rod.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));

        return rod;
    }
}
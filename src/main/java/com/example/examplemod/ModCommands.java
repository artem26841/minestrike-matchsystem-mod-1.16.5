package com.example.examplemod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.server.MinecraftServer;
import java.util.UUID;

public class ModCommands {
    private static boolean hasAdminPermission(CommandSource source) {
        try {
            if (source.hasPermission(2)) return true;
            if (source.getEntity() instanceof ServerPlayerEntity) {
                ServerPlayerEntity p = (ServerPlayerEntity) source.getEntity();
                return MatchSystem.MATCH_ADMINS.contains(p.getUniqueID());
            }
        } catch (Exception ignored) {}
        return false;
    }

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(Commands.literal("ms").then(Commands.literal("round")
            .then(Commands.literal("on").executes(ctx -> {
                if (!hasAdminPermission(ctx.getSource())) { ctx.getSource().sendErrorMessage(new StringTextComponent("§cNo permission!")); return 0; }
                MatchSystem.isModEnabled = true;
                ctx.getSource().sendFeedback(new StringTextComponent(RoundManager.getMsg("mod_on")), true);
                return 1;
            }))
            .then(Commands.literal("off").executes(ctx -> {
                if (!hasAdminPermission(ctx.getSource())) { ctx.getSource().sendErrorMessage(new StringTextComponent("§cNo permission!")); return 0; }
                MatchSystem.isModEnabled = false;
                RoundManager.forceStopMatch();
                ctx.getSource().sendFeedback(new StringTextComponent(RoundManager.getMsg("mod_off")), true);
                return 1;
            }))
            .then(Commands.literal("startmatch").executes(ctx -> {
                if (!hasAdminPermission(ctx.getSource())) { ctx.getSource().sendErrorMessage(new StringTextComponent("§cNo permission!")); return 0; }
                MatchSystem.isMatchStarted = true;
                MatchSystem.tPoints = 0; MatchSystem.ctPoints = 0;
                MatchSystem.matchStartTime = System.currentTimeMillis();
                StatsManager.resetStats();
                ctx.getSource().sendFeedback(new StringTextComponent(RoundManager.getMsg("match_ready")), true);
                return 1;
            }))
            .then(Commands.literal("stopmatch").executes(ctx -> {
                if (!hasAdminPermission(ctx.getSource())) { ctx.getSource().sendErrorMessage(new StringTextComponent("§cNo permission!")); return 0; }
                RoundManager.forceStopMatch();
                ctx.getSource().sendFeedback(new StringTextComponent(RoundManager.getMsg("match_stopped")), true);
                return 1;
            }))
            .then(Commands.literal("start").executes(ctx -> {
                if (!hasAdminPermission(ctx.getSource())) { ctx.getSource().sendErrorMessage(new StringTextComponent("§cNo permission!")); return 0; }
                RoundManager.startNewRound();
                return 1;
            }))
            .then(Commands.literal("stop").executes(ctx -> {
                if (!hasAdminPermission(ctx.getSource())) { ctx.getSource().sendErrorMessage(new StringTextComponent("§cNo permission!")); return 0; }
                MatchSystem.isRoundActive = false;
                RoundManager.resetPlayersToSpawn();
                ctx.getSource().sendFeedback(new StringTextComponent(RoundManager.getMsg("round_stopped")), true);
                return 1;
            }))
            .then(Commands.literal("startauto").executes(ctx -> {
                if (!hasAdminPermission(ctx.getSource())) { ctx.getSource().sendErrorMessage(new StringTextComponent("§cNo permission!")); return 0; }
                MatchSystem.isAutoMode = !MatchSystem.isAutoMode;
                String status = MatchSystem.isAutoMode ? "§aON" : "§cOFF";
                ctx.getSource().sendFeedback(new StringTextComponent("§eAutoMode: " + status), true);
                return 1;
            }))
            .then(Commands.literal("paused").executes(ctx -> {
                if (!hasAdminPermission(ctx.getSource())) { ctx.getSource().sendErrorMessage(new StringTextComponent("§cNo permission!")); return 0; }
                RoundManager.togglePause();
                return 1;
            }))
            .then(Commands.literal("settingsinfo").executes(ctx -> {
                ctx.getSource().sendFeedback(new StringTextComponent("§e=== SETTINGS ===\n§7Time: §f" + MatchSystem.configRoundTime), true);
                return 1;
            }))
        ).then(Commands.literal("language").then(Commands.argument("lang", StringArgumentType.string()).executes(ctx -> {
            if (!hasAdminPermission(ctx.getSource())) { ctx.getSource().sendErrorMessage(new StringTextComponent("§cNo permission!")); return 0; }
            MatchSystem.currentLanguage = StringArgumentType.getString(ctx, "lang").toLowerCase();
            ctx.getSource().sendFeedback(new StringTextComponent("§aLanguage changed!"), true);
            return 1;
        }))).then(Commands.literal("commandaddplayers")
            .then(Commands.literal("red").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                if (!hasAdminPermission(ctx.getSource())) { ctx.getSource().sendErrorMessage(new StringTextComponent("§cNo permission!")); return 0; }
                ServerPlayerEntity p = EntityArgument.getPlayer(ctx, "player"); UUID uuid = p.getUniqueID(); MatchSystem.removeFromAllTeams(uuid); MatchSystem.T_TEAM.add(uuid); return 1;
            })))
            .then(Commands.literal("blue").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                if (!hasAdminPermission(ctx.getSource())) { ctx.getSource().sendErrorMessage(new StringTextComponent("§cNo permission!")); return 0; }
                ServerPlayerEntity p = EntityArgument.getPlayer(ctx, "player"); UUID uuid = p.getUniqueID(); MatchSystem.removeFromAllTeams(uuid); MatchSystem.CT_TEAM.add(uuid); return 1;
            })))
            .then(Commands.literal("admin").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                if (!hasAdminPermission(ctx.getSource())) { ctx.getSource().sendErrorMessage(new StringTextComponent("§cNo permission!")); return 0; }
                ServerPlayerEntity p = EntityArgument.getPlayer(ctx, "player"); UUID uuid = p.getUniqueID();
                if (MatchSystem.MATCH_ADMINS.contains(uuid)) MatchSystem.MATCH_ADMINS.remove(uuid); else { MatchSystem.removeFromAllTeams(uuid); MatchSystem.MATCH_ADMINS.add(uuid); }
                return 1;
            })))
            .then(Commands.literal("info").executes(ctx -> {
                StringBuilder sb = new StringBuilder("§e=== TEAMS ===\n§6§lT:\n"); MinecraftServer server = ctx.getSource().getServer();
                MatchSystem.T_TEAM.forEach(uuid -> { ServerPlayerEntity p = server.getPlayerList().getPlayerByUUID(uuid); sb.append("§7- ").append(p != null ? p.getGameProfile().getName() : "Offline").append("\n"); });
                sb.append("§b§lCT:\n"); MatchSystem.CT_TEAM.forEach(uuid -> { ServerPlayerEntity p = server.getPlayerList().getPlayerByUUID(uuid); sb.append("§7- ").append(p != null ? p.getGameProfile().getName() : "Offline").append("\n"); });
                ctx.getSource().sendFeedback(new StringTextComponent(sb.toString()), true);
                return 1;
            }))
        ));
    }
}

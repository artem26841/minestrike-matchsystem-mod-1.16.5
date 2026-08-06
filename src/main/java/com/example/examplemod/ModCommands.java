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
            if (source.hasPermissionLevel(2)) return true;
            if (source.getEntity() instanceof ServerPlayerEntity) {
                ServerPlayerEntity player = (ServerPlayerEntity) source.getEntity();
                return MatchSystem.MATCH_ADMINS.contains(player.getGameProfile().getId());
            }
        } catch (Exception ignored) {}
        return false;
    }

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(Commands.literal("ms")
            .then(Commands.literal("round")
                .then(Commands.literal("on").executes(ctx -> {
                    if (!hasAdminPermission(ctx.getSource())) { ctx.getSource().sendFailure(new StringTextComponent("§cNo permission!")); return 0; }
                    MatchSystem.isModEnabled = true;
                    ctx.getSource().sendSuccess(new StringTextComponent(RoundManager.getMsg("mod_on")), true);
                    return 1;
                }))
                .then(Commands.literal("off").executes(ctx -> {
                    if (!hasAdminPermission(ctx.getSource())) { ctx.getSource().sendFailure(new StringTextComponent("§cNo permission!")); return 0; }
                    MatchSystem.isModEnabled = false;
                    if (MatchSystem.isMatchStarted) RoundManager.forceStopMatch();
                    ctx.getSource().sendSuccess(new StringTextComponent(RoundManager.getMsg("mod_off")), true);
                    return 1;
                }))
                .then(Commands.literal("startmatch").executes(ctx -> {
                    if (!hasAdminPermission(ctx.getSource())) { ctx.getSource().sendFailure(new StringTextComponent("§cNo permission!")); return 0; }
                    if (!MatchSystem.isModEnabled) { ctx.getSource().sendFailure(new StringTextComponent("§cError: Mod is off!")); return 0; }
                    MatchSystem.isMatchStarted = true;
                    MatchSystem.tPoints = 0;
                    MatchSystem.ctPoints = 0;
                    MatchSystem.matchStartTime = System.currentTimeMillis();
                    StatsManager.resetStats();
                    ctx.getSource().sendSuccess(new StringTextComponent(RoundManager.getMsg("match_ready")), true);
                    return 1;
                }))
                .then(Commands.literal("stopmatch").executes(ctx -> {
                    if (!hasAdminPermission(ctx.getSource())) { ctx.getSource().sendFailure(new StringTextComponent("§cNo permission!")); return 0; }
                    if (!MatchSystem.isMatchStarted) { ctx.getSource().sendFailure(new StringTextComponent("§cError: Match not started!")); return 0; }
                    RoundManager.forceStopMatch();
                    ctx.getSource().sendSuccess(new StringTextComponent(RoundManager.getMsg("match_stopped")), true);
                    return 1;
                }))
                .then(Commands.literal("start").executes(ctx -> {
                    if (!hasAdminPermission(ctx.getSource())) { ctx.getSource().sendFailure(new StringTextComponent("§cNo permission!")); return 0; }
                    if (!MatchSystem.isMatchStarted) { ctx.getSource().sendFailure(new StringTextComponent("§cError: Run startmatch first!")); return 0; }
                    if (MatchSystem.isRoundActive) { ctx.getSource().sendFailure(new StringTextComponent("§cError: Round active!")); return 0; }
                    RoundManager.startNewRound();
                    return 1;
                }))
                .then(Commands.literal("stop").executes(ctx -> {
                    if (!hasAdminPermission(ctx.getSource())) { ctx.getSource().sendFailure(new StringTextComponent("§cNo permission!")); return 0; }
                    if (!MatchSystem.isMatchStarted) { ctx.getSource().sendFailure(new StringTextComponent("§cError: Match not started!")); return 0; }
                    MatchSystem.isRoundActive = false;
                    RoundManager.resetPlayersToSpawn();
                    ctx.getSource().sendSuccess(new StringTextComponent(RoundManager.getMsg("round_stopped")), true);
                    return 1;
                }))
                .then(Commands.literal("startauto").executes(ctx -> {
                    if (!hasAdminPermission(ctx.getSource())) { ctx.getSource().sendFailure(new StringTextComponent("§cNo permission!")); return 0; }
                    if (!MatchSystem.isMatchStarted) { ctx.getSource().sendFailure(new StringTextComponent("§cError: Match not started!")); return 0; }
                    MatchSystem.isAutoMode = !MatchSystem.isAutoMode;
                    String status = MatchSystem.isAutoMode ? "§aON" : "§cOFF";
                    ctx.getSource().sendSuccess(new StringTextComponent("§e[MineStrike] AutoMode: " + status), true);
                    return 1;
                }))
                .then(Commands.literal("paused").executes(ctx -> {
                    if (!hasAdminPermission(ctx.getSource())) { ctx.getSource().sendFailure(new StringTextComponent("§cNo permission!")); return 0; }
                    if (!MatchSystem.isMatchStarted) { ctx.getSource().sendFailure(new StringTextComponent("§cError: Match not started!")); return 0; }
                    RoundManager.togglePause();
                    return 1;
                }))
                .then(Commands.literal("reset").executes(ctx -> {
                    if (!hasAdminPermission(ctx.getSource())) { ctx.getSource().sendFailure(new StringTextComponent("§cNo permission!")); return 0; }
                    if (!MatchSystem.isMatchStarted) { ctx.getSource().sendFailure(new StringTextComponent("§cError: Match not started!")); return 0; }
                    MatchSystem.tPoints = 0;
                    MatchSystem.ctPoints = 0;
                    MatchSystem.isRoundActive = false;
                    MatchSystem.matchStartTime = System.currentTimeMillis();
                    StatsManager.resetStats();
                    ctx.getSource().sendSuccess(new StringTextComponent(RoundManager.getMsg("match_reset")), true);
                    return 1;
                }))
                .then(Commands.literal("settings")
                    .then(Commands.argument("roundTime", IntegerArgumentType.integer(1))
                    .then(Commands.argument("freezeTime", IntegerArgumentType.integer(0))
                    .then(Commands.argument("maxPoints", IntegerArgumentType.integer(1))
                    .executes(ctx -> {
                        if (!hasAdminPermission(ctx.getSource())) { ctx.getSource().sendFailure(new StringTextComponent("§cNo permission!")); return 0; }
                        MatchSystem.configRoundTime = IntegerArgumentType.getInteger(ctx, "roundTime");
                        MatchSystem.configFreezeTime = IntegerArgumentType.getInteger(ctx, "freezeTime");
                        MatchSystem.configMaxPoints = IntegerArgumentType.getInteger(ctx, "maxPoints");
                        StatsManager.saveStatsToFile();
                        ctx.getSource().sendSuccess(new StringTextComponent("§a[MineStrike] Settings updated!"), true);
                        return 1;
                    })))))
                .then(Commands.literal("settingsinfo").executes(ctx -> {
                    ctx.getSource().sendSuccess(new StringTextComponent("§e=== SETTINGS ===\n§7Round Time: §f" + MatchSystem.configRoundTime + "s\n§7Freeze Time: §f" + MatchSystem.configFreezeTime + "s\n§7Max Points: §f" + MatchSystem.configMaxPoints), true);
                    return 1;
                }))
            )
            .then(Commands.literal("language")
                .then(Commands.argument("lang", StringArgumentType.string()).executes(ctx -> {
                    if (!hasAdminPermission(ctx.getSource())) { ctx.getSource().sendFailure(new StringTextComponent("§cNo permission!")); return 0; }
                    String lang = StringArgumentType.getString(ctx, "lang").toLowerCase();
                    if (lang.contains("en")) MatchSystem.currentLanguage = "en";
                    else if (lang.contains("jp") || lang.contains("日本")) MatchSystem.currentLanguage = "jp";
                    else MatchSystem.currentLanguage = "ru";
                    ctx.getSource().sendSuccess(new StringTextComponent("§a[MineStrike] Language changed to: " + MatchSystem.currentLanguage.toUpperCase()), true);
                    return 1;
                })))
            .then(Commands.literal("commandaddplayers")
                .then(Commands.literal("red").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                    if (!hasAdminPermission(ctx.getSource())) { ctx.getSource().sendFailure(new StringTextComponent("§cNo permission!")); return 0; }
                    ServerPlayerEntity p = EntityArgument.getPlayer(ctx, "player"); UUID uuid = p.getGameProfile().getId(); MatchSystem.removeFromAllTeams(uuid); MatchSystem.T_TEAM.add(uuid); ctx.getSource().sendSuccess(new StringTextComponent("§6" + p.getGameProfile().getName() + " §7added to §6T"), true); return 1;
                })))
                .then(Commands.literal("blue").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                    if (!hasAdminPermission(ctx.getSource())) { ctx.getSource().sendFailure(new StringTextComponent("§cNo permission!")); return 0; }
                    ServerPlayerEntity p = EntityArgument.getPlayer(ctx, "player"); UUID uuid = p.getGameProfile().getId(); MatchSystem.removeFromAllTeams(uuid); MatchSystem.CT_TEAM.add(uuid); ctx.getSource().sendSuccess(new StringTextComponent("§b" + p.getGameProfile().getName() + " §7added to §bCT"), true); return 1;
                })))
                .then(Commands.literal("spectator").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                    if (!hasAdminPermission(ctx.getSource())) { ctx.getSource().sendFailure(new StringTextComponent("§cNo permission!")); return 0; }
                    ServerPlayerEntity p = EntityArgument.getPlayer(ctx, "player"); UUID uuid = p.getGameProfile().getId(); MatchSystem.removeFromAllTeams(uuid); MatchSystem.SPECTATORS.add(uuid); ctx.getSource().sendSuccess(new StringTextComponent("§e" + p.getGameProfile().getName() + " §7added to §eSPEC"), true); return 1;
                })))
                .then(Commands.literal("del").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                    if (!hasAdminPermission(ctx.getSource())) { ctx.getSource().sendFailure(new StringTextComponent("§cNo permission!")); return 0; }
                    ServerPlayerEntity p = EntityArgument.getPlayer(ctx, "player"); UUID uuid = p.getGameProfile().getId(); MatchSystem.removeFromAllTeams(uuid); ctx.getSource().sendSuccess(new StringTextComponent("§7Player §f" + p.getGameProfile().getName() + " §7deleted."), true); return 1;
                })))
                .then(Commands.literal("admin").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                    if (!hasAdminPermission(ctx.getSource())) { ctx.getSource().sendFailure(new StringTextComponent("§cNo permission!")); return 0; }
                    ServerPlayerEntity p = EntityArgument.getPlayer(ctx, "player");
                    UUID uuid = p.getGameProfile().getId();
                    if (MatchSystem.MATCH_ADMINS.contains(uuid)) {
                        MatchSystem.MATCH_ADMINS.remove(uuid);
                        ctx.getSource().sendSuccess(new StringTextComponent("§c[MineStrike] Права админа матча забраны у " + p.getGameProfile().getName()), true);
                    } else {
                        MatchSystem.removeFromAllTeams(uuid);
                        MatchSystem.MATCH_ADMINS.add(uuid);
                        ctx.getSource().sendSuccess(new StringTextComponent("§a[MineStrike] " + p.getGameProfile().getName() + " назначен админом матча!"), true);
                    }
                    return 1;
                })))
                .then(Commands.literal("info").executes(ctx -> {
                    StringBuilder sb = new StringBuilder("§e=== TEAMS ===\n§6§lTERRORISTS (T):\n");
                    MinecraftServer server = ctx.getSource().getServer();
                    MatchSystem.T_TEAM.forEach(uuid -> {
                        ServerPlayerEntity p = null;
                        for (ServerPlayerEntity spe : server.getPlayerList().getPlayers()) {
                            if (spe.getGameProfile().getId().equals(uuid)) { p = spe; break; }
                        }
                        sb.append("§7- §f").append(p != null ? p.getGameProfile().getName() : "Offline").append("\n");
                    });
                    sb.append("§b§lCOUNTER-TERROWISTS (CT):\n");
                    MatchSystem.CT_TEAM.forEach(uuid -> {
                        ServerPlayerEntity p = null;
                        for (ServerPlayerEntity spe : server.getPlayerList().getPlayers()) {
                            if (spe.getGameProfile().getId().equals(uuid)) { p = spe; break; }
                        }
                        sb.append("§7- §f").append(p != null ? p.getGameProfile().getName() : "Offline").append("\n");
                    });
                    sb.append("§a§lMATCH ADMINS:\n");
                    MatchSystem.MATCH_ADMINS.forEach(uuid -> {
                        ServerPlayerEntity p = null;
                        for (ServerPlayerEntity spe : server.getPlayerList().getPlayers()) {
                            if (spe.getGameProfile().getId().equals(uuid)) { p = spe; break; }
                        }
                        sb.append("§7- §f").append(p != null ? p.getGameProfile().getName() : "Offline").append("\n");
                    });
                    ctx.getSource().sendSuccess(new StringTextComponent(sb.toString()), true);
                    return 1;
                }))
            )
        );
    }
}

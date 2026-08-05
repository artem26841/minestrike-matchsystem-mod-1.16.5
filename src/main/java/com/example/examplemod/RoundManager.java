package com.example.examplemod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.world.GameType;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.server.MinecraftServer;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class RoundManager {
    public static List<ServerPlayerEntity> getMatchPlayers() {
        List<ServerPlayerEntity> list = new ArrayList<>();
        if (ServerLifecycleHooks.getCurrentServer() == null) return list;
        for (ServerPlayerEntity p : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            UUID id = p.getUniqueID();
            if (MatchSystem.T_TEAM.contains(id) || MatchSystem.CT_TEAM.contains(id) || MatchSystem.SPECTATORS.contains(id) || MatchSystem.MATCH_ADMINS.contains(id)) {
                list.add(p);
            }
        }
        return list;
    }

    public static String getMsg(String key) {
        String lang = MatchSystem.currentLanguage;
        if (lang.equals("en")) {
            switch(key) {
                case "mod_on": return "§a[MineStrike] Mod Enabled!";
                case "mod_off": return "§c[MineStrike] Mod Disabled!";
                case "match_ready": return "§6[MineStrike] MATCH PREPARED! HUD Active.";
                case "match_stopped": return "§c[MineStrike] Match officially TERMINATED by admin.";
                case "match_reset": return "§e[MineStrike] Match reset. Waiting for round start.";
                case "round_start": return "§c§lROUND STARTED!";
                case "round_stopped": return "§c[MineStrike] Round stopped. Players reset to spawn.";
                case "hud_paused": return "PAUSED";
                case "hud_waiting": return "WAITING";
                case "hud_freeze": return "FREEZE";
                default: return "";
            }
        } else if (lang.equals("jp")) {
            switch(key) {
                case "mod_on": return "§a[MineStrike] モッドが有効化されました！";
                case "mod_off": return "§c[MineStrike] モッドが無効化されました！";
                case "match_ready": return "§6[MineStrike] マッチの準備完了！ HUD有効。";
                case "match_stopped": return "§c[MineStrike] マッチは管理権限により終了されました。";
                case "match_reset": return "§e[MineStrike] マッチがリセットされました。";
                case "round_start": return "§c§lラウンド開始！";
                case "round_stopped": return "§c[MineStrike] ラウンド停止。リスポーン地点に戻ります。";
                case "hud_paused": return "一時停止";
                case "hud_waiting": return "待機中";
                case "hud_freeze": return "フリーズ";
                default: return "";
            }
        } else {
            switch(key) {
                case "mod_on": return "§a[MineStrike] Мод включен!";
                case "mod_off": return "§c[MineStrike] Мод выключен!";
                case "match_ready": return "§6[MineStrike] МАТЧ ПОДГОТОВЛЕН К РАБОТЕ! Табло активировано.";
                case "match_stopped": return "§c[MineStrike] Матч официально ЗАВЕРШЕН админом.";
                case "match_reset": return "§e[MineStrike] Матч сброшен и ожидает старта.";
                case "round_start": return "§c§lРАУНД НАЧАЛСЯ!";
                case "round_stopped": return "§c[MineStrike] Раунд остановлен. Игроки вернулись на спавн.";
                case "hud_paused": return "ПАУЗА";
                case "hud_waiting": return "ОЖИДАНИЕ";
                case "hud_freeze": return "ЗАКУПКА";
                default: return "";
            }
        }
    }

    public static void togglePause() {
        MatchSystem.isPaused = !MatchSystem.isPaused;
        if (MatchSystem.isPaused) {
            broadcastMessage("§c[MineStrike] MATCH PAUSED!");
            for (ServerPlayerEntity player : getMatchPlayers()) {
                player.addPotionEffect(new EffectInstance(Effects.BLINDNESS, 999999, 255, false, false));
                player.addPotionEffect(new EffectInstance(Effects.SLOWNESS, 999999, 255, false, false));
            }
        } else {
            broadcastMessage("§a[MineStrike] MATCH RESUMED!");
            for (ServerPlayerEntity player : getMatchPlayers()) {
                player.removePotionEffect(Effects.BLINDNESS);
                player.removePotionEffect(Effects.SLOWNESS);
            }
        }
    }
    public static void endRound(String winningTeam, String endReason) {
        MatchSystem.isRoundActive = false;
        if (winningTeam.equalsIgnoreCase("t")) MatchSystem.tPoints++;
        else if (winningTeam.equalsIgnoreCase("ct")) MatchSystem.ctPoints++;

        StatsManager.saveStatsToFile();
        broadcastMessage("§6=================================\n§e§lROUND OVER!\n" + endReason + "\n§7Score: §6T " + MatchSystem.tPoints + " §7| §b" + MatchSystem.ctPoints + " CT\n§6=================================");

        if (MatchSystem.tPoints >= MatchSystem.configMaxPoints) {
            MatchSystem.matchEndTime = System.currentTimeMillis();
            sendBigTitle("title @a title \"§6§lTERRORISTS WIN THE MATCH\"", "title @a subtitle \"§fScore: §6" + MatchSystem.tPoints + " §7- §b" + MatchSystem.ctPoints + "\"");
            playVictoryEffects();
            printFinalStatistics("TERRORISTS (T)");
            forceStopMatch();
            return;
        } else if (MatchSystem.ctPoints >= MatchSystem.configMaxPoints) {
            MatchSystem.matchEndTime = System.currentTimeMillis();
            sendBigTitle("title @a title \"§b§lCOUNTER-TERRORISTS WIN THE MATCH\"", "title @a subtitle \"§fScore: §6" + MatchSystem.tPoints + " §7- §b" + MatchSystem.ctPoints + "\"");
            playVictoryEffects();
            printFinalStatistics("COUNTER-TERRORISTS (CT)");
            forceStopMatch();
            return;
        }

        if (MatchSystem.isAutoMode) {
            RoundTimerHandler.freezeTimeLeft = MatchSystem.configFreezeTime;
            RoundTimerHandler.isFreezePeriod = true;
            startNewRound();
        }
    }

    private static void printFinalStatistics(String winnerName) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        String startStr = sdf.format(new Date(MatchSystem.matchStartTime));
        String endStr = sdf.format(new Date(MatchSystem.matchEndTime));
        long diff = MatchSystem.matchEndTime - MatchSystem.matchStartTime;
        long mins = (diff / 1000) / 60;
        long secs = (diff / 1000) % 60;

        StringBuilder sb = new StringBuilder("\n§6=================================\n");
        sb.append("§e🏆 §lFINAL MATCH STATISTICS §e🏆\n\n");
        sb.append("§eWinner: §l").append(winnerName).append("\n");
        sb.append("§7Start Time: §f").append(startStr).append("\n");
        sb.append("§7End Time: §f").append(endStr).append("\n");
        sb.append("§7Duration: §f").append(mins).append("m ").append(secs).append("s\n\n");
        sb.append("§e§lFINAL PLAYER KILLS:\n");

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        getMatchPlayers().forEach(p -> {
            int kills = StatsManager.getKills(p.getUniqueID());
            String color = MatchSystem.T_TEAM.contains(p.getUniqueID()) ? "§6" : "§b";
            sb.append(color).append(p.getGameProfile().getName()).append("§0: §l").append(kills).append("\n");
        });
        sb.append("§6=================================");
        broadcastMessage(sb.toString());
    }

    private static void playVictoryEffects() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        for (ServerPlayerEntity player : server.getPlayerList().getPlayers()) {
            player.connection.sendPacket(new net.minecraft.network.play.server.SPlaySoundPacket("ui.toast.challenge_complete", SoundCategory.MASTER, player.getPositionVec(), 1.0F, 1.0F));
            player.connection.sendPacket(new net.minecraft.network.play.server.SPlaySoundPacket("entity.firework_rocket.blast", SoundCategory.MASTER, player.getPositionVec(), 1.0F, 1.0F));
        }
    }

    private static void sendBigTitle(String cmd1, String cmd2) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            server.getCommandManager().handleCommand(server.getCommandSource(), cmd1);
            server.getCommandManager().handleCommand(server.getCommandSource(), cmd2);
        }
    }

    public static void startNewRound() {
        DeathAndRoundHandler.DEAD_PLAYERS.clear();
        RoundTimerHandler.roundTimeLeft = MatchSystem.configRoundTime;
        RoundTimerHandler.freezeTimeLeft = MatchSystem.configFreezeTime;
        RoundTimerHandler.isFreezePeriod = true;
        MatchSystem.isRoundActive = true;
        MatchSystem.isPaused = false;
        resetPlayersToSpawn();
        broadcastMessage("§a[MineStrike] New round started!");
    }

    public static void resetPlayersToSpawn() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        for (ServerPlayerEntity player : getMatchPlayers()) {
            player.getServer().execute(() -> {
                player.setGameMode(GameType.SURVIVAL);
                player.removePotionEffect(Effects.BLINDNESS);
                player.removePotionEffect(Effects.SLOWNESS);
                
                net.minecraft.util.math.BlockPos spawnPos = player.getServerWorld().getSpawnPoint();
                player.teleport(player.getServerWorld(), spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, player.rotationYaw, player.rotationPitch);
            });
        }
    }

    public static void forceStopMatch() {
        MatchSystem.isMatchStarted = false;
        MatchSystem.isRoundActive = false;
        MatchSystem.isPaused = false;
        for (ServerPlayerEntity p : getMatchPlayers()) {
            p.removePotionEffect(Effects.BLINDNESS);
            p.removePotionEffect(Effects.SLOWNESS);
        }
    }

    public static void broadcastMessage(String text) {
        getMatchPlayers().forEach(p -> p.sendMessage(new StringTextComponent(text), p.getUniqueID()));
    }
}

class StatsManager {
    private static final Map<UUID, Integer> playerKills = new HashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void addKill(UUID uuid) { playerKills.put(uuid, playerKills.getOrDefault(uuid, 0) + 1); }
    public static int getKills(UUID uuid) { return playerKills.getOrDefault(uuid, 0); }
    public static void resetStats() { playerKills.clear(); saveStatsToFile(); }

    public static void saveStatsToFile() {
        File configFile = new File(FMLPaths.CONFIGDIR.get().toFile(), "matchsystem/session.json");
        JsonObject rootJson = new JsonObject();
        rootJson.addProperty("t_score", MatchSystem.tPoints);
        rootJson.addProperty("ct_score", MatchSystem.ctPoints);
        rootJson.addProperty("config_round_time", MatchSystem.configRoundTime);
        rootJson.addProperty("config_freeze_time", MatchSystem.configFreezeTime);
        rootJson.addProperty("config_max_points", MatchSystem.configMaxPoints);
        JsonObject playersJson = new JsonObject();
        MatchSystem.T_TEAM.forEach(uuid -> addPlayerStat(playersJson, uuid, "T"));
        MatchSystem.CT_TEAM.forEach(uuid -> addPlayerStat(playersJson, uuid, "CT"));
        rootJson.add("players_statistics", playersJson);
        try (FileWriter writer = new FileWriter(configFile)) { GSON.toJson(rootJson, writer); } catch (IOException ignored) {}
    }

    private static void addPlayerStat(JsonObject json, UUID uuid, String team) {
        JsonObject pStat = new JsonObject();
        pStat.addProperty("team", team);
        pStat.addProperty("kills", playerKills.getOrDefault(uuid, 0));
        json.add(uuid.toString(), pStat);
    }
}

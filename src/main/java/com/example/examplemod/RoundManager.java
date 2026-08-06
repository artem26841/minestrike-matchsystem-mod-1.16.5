package com.example.examplemod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.GameType;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.SoundCategory;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

@Mod.EventBusSubscriber(modid = "examplemod")
public class RoundManager {
    private static int tickCounter = 0;
    public static int roundTimeLeft = 115;
    public static int freezeTimeLeft = 15;
    public static boolean isFreezePeriod = true;
    public static final Set<UUID> DEAD_PLAYERS = new HashSet<>();

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
                case "match_ready": return "§6[MineStrike] MATCH PREPARED!";
                case "match_stopped": return "§c[MineStrike] Match TERMINATED!";
                case "match_reset": return "§e[MineStrike] Match reset.";
                case "round_start": return "§c§lROUND STARTED!";
                case "round_stopped": return "§c[MineStrike] Round stopped.";
                case "hud_paused": return "PAUSED";
                case "hud_waiting": return "WAITING";
                case "hud_freeze": return "FREEZE";
                default: return "";
            }
        } else if (lang.equals("jp")) {
            switch(key) {
                case "mod_on": return "§a[MineStrike] モッドが有効化されました！";
                case "mod_off": return "§c[MineStrike] モッドが無効化されました！";
                case "match_ready": return "§6[MineStrike] マッチの準備完了！";
                case "match_stopped": return "§c[MineStrike] マッチは終了されました。";
                case "match_reset": return "§e[MineStrike] マッチがリセットされました。";
                case "round_start": return "§c§lラウンド開始！";
                case "round_stopped": return "§c[MineStrike] ラウンド停止。";
                case "hud_paused": return "一時停止";
                case "hud_waiting": return "待機中";
                case "hud_freeze": return "フリーズ";
                default: return "";
            }
        } else {
            switch(key) {
                case "mod_on": return "§a[MineStrike] Мод включен!";
                case "mod_off": return "§c[MineStrike] Мод выключен!";
                case "match_ready": return "§6[MineStrike] МАТЧ ПОДГОТОВЛЕН!";
                case "match_stopped": return "§c[MineStrike] Матч ЗАВЕРШЕН админом.";
                case "match_reset": return "§e[MineStrike] Матч сброшен.";
                case "round_start": return "§c§lРАУНД НАЧАЛСЯ!";
                case "round_stopped": return "§c[MineStrike] Раунд остановлен.";
                case "hud_paused": return "ПАУЗА";
                case "hud_waiting": return "ОЖИДАНИЕ";
                case "hud_freeze": return "ЗАКУПКА";
                default: return "";
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (!MatchSystem.isModEnabled || !MatchSystem.isMatchStarted) return;
        if (event.phase == TickEvent.Phase.END) {
            drawMatchHUD();
            if (MatchSystem.isPaused || !MatchSystem.isRoundActive) return;
            tickCounter++;
            if (tickCounter >= 20) { tickCounter = 0; secondsTick(); }
        }
    }

    private static void secondsTick() {
        if (isFreezePeriod) {
            if (freezeTimeLeft > 0) { freezeTimeLeft--; } 
            else { isFreezePeriod = false; broadcastMessage(getMsg("round_start")); }
            return;
        }
        if (roundTimeLeft > 0) { roundTimeLeft--; } 
        else { endRound("ct", "§bTime Expired! CT Win."); }
    }

    private static void drawMatchHUD() {
        String timerStr;
        String timerColor = roundTimeLeft <= 10 && !isFreezePeriod ? "§c" : "§a";
        if (MatchSystem.isPaused) { timerStr = "§c§l" + getMsg("hud_paused"); } 
        else if (!MatchSystem.isRoundActive) { timerStr = "§7" + getMsg("hud_waiting"); } 
        else if (isFreezePeriod) { timerStr = "§e" + getMsg("hud_freeze") + ": " + freezeTimeLeft + "s"; } 
        else { timerStr = timerColor + String.format("%02d:%02d", roundTimeLeft / 60, roundTimeLeft % 60); }
        String hudText = "§6§lT §7[" + MatchSystem.tPoints + "] §7| " + timerStr + " §7| §7[" + MatchSystem.ctPoints + "] §b§lCT";
        getMatchPlayers().forEach(p -> p.sendStatusMessage(new StringTextComponent(hudText), true));
    }
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!MatchSystem.isModEnabled || !MatchSystem.isMatchStarted || !MatchSystem.isRoundActive) return;
        if (event.getEntity() instanceof ServerPlayerEntity) {
            ServerPlayerEntity victim = (ServerPlayerEntity) event.getEntity();
            UUID victimUUID = victim.getUniqueID();
            if (!MatchSystem.T_TEAM.contains(victimUUID) && !MatchSystem.CT_TEAM.contains(victimUUID)) return;
            DEAD_PLAYERS.add(victimUUID);
            if (event.getSource().getTrueSource() instanceof ServerPlayerEntity) {
                ServerPlayerEntity killer = (ServerPlayerEntity) event.getSource().getTrueSource();
                if (!killer.getUniqueID().equals(victimUUID)) { StatsManager.addKill(killer.getUniqueID()); }
            }
            broadcastMessage("§7[MineStrike] " + victim.getGameProfile().getName() + " was killed!");
            checkWinConditions();
        }
    }

    public static void checkWinConditions() {
        int aliveT = 0, aliveCT = 0;
        for (UUID uuid : MatchSystem.T_TEAM) if (!DEAD_PLAYERS.contains(uuid)) aliveT++;
        for (UUID uuid : MatchSystem.CT_TEAM) if (!DEAD_PLAYERS.contains(uuid)) aliveCT++;
        if (aliveT == 0 && !MatchSystem.T_TEAM.isEmpty()) { endRound("ct", "§bAll Terrorists eliminated! CT Win."); } 
        else if (aliveCT == 0 && !MatchSystem.CT_TEAM.isEmpty()) { endRound("t", "§6All Counter-Terrorists eliminated! T Win."); }
    }

    public static void togglePause() {
        MatchSystem.isPaused = !MatchSystem.isPaused;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        if (MatchSystem.isPaused) {
            broadcastMessage("§c[MineStrike] MATCH PAUSED!");
            for (ServerPlayerEntity p : getMatchPlayers()) {
                server.getCommandManager().handleCommand(server.getCommandSource(), "effect give " + p.getGameProfile().getName() + " minecraft:blindness 999999 255 true");
            }
        } else {
            broadcastMessage("§a[MineStrike] MATCH RESUMED!");
            for (ServerPlayerEntity p : getMatchPlayers()) {
                server.getCommandManager().handleCommand(server.getCommandSource(), "effect clear " + p.getGameProfile().getName() + " minecraft:blindness");
            }
        }
    }

    public static void endRound(String winningTeam, String endReason) {
        MatchSystem.isRoundActive = false;
        if (winningTeam.equalsIgnoreCase("t")) MatchSystem.tPoints++; else MatchSystem.ctPoints++;
        StatsManager.saveStatsToFile();
        broadcastMessage("§6=================================\n§e§lROUND OVER!\n" + endReason + "\n§7Score: §6T " + MatchSystem.tPoints + " §7| §b" + MatchSystem.ctPoints + " CT\n§6=================================");
        if (MatchSystem.tPoints >= MatchSystem.configMaxPoints) {
            MatchSystem.matchEndTime = System.currentTimeMillis();
            sendBigTitle("title @a title \"§6§lTERRORISTS WIN THE MATCH\"", "title @a subtitle \"§fScore: §6" + MatchSystem.tPoints + " - §b" + MatchSystem.ctPoints + "\"");
            playVictoryEffects(); printFinalStatistics("TERRORISTS (T)"); forceStopMatch(); return;
        } else if (MatchSystem.ctPoints >= MatchSystem.configMaxPoints) {
            MatchSystem.matchEndTime = System.currentTimeMillis();
            sendBigTitle("title @a title \"§b§lCOUNTER-TERRORISTS WIN THE MATCH\"", "title @a subtitle \"§fScore: §6" + MatchSystem.tPoints + " - §b" + MatchSystem.ctPoints + "\"");
            playVictoryEffects(); printFinalStatistics("COUNTER-TERRORISTS (CT)"); forceStopMatch(); return;
        }
    }

    private static void printFinalStatistics(String winnerName) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        String startStr = sdf.format(new Date(MatchSystem.matchStartTime));
        String endStr = sdf.format(new Date(MatchSystem.matchEndTime));
        long diff = MatchSystem.matchEndTime - MatchSystem.matchStartTime;
        StringBuilder sb = new StringBuilder("\n§6=================================\n§e🏆 §lFINAL MATCH STATISTICS §e🏆\n\n§eWinner: §l" + winnerName + "\n§7Start Time: §f" + startStr + "\n§7End Time: §f" + endStr + "\n\n§e§lFINAL PLAYER KILLS:\n");
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        getMatchPlayers().forEach(p -> { int kills = StatsManager.getKills(p.getUniqueID()); String color = MatchSystem.T_TEAM.contains(p.getUniqueID()) ? "§6" : "§b"; sb.append(color).append(p.getGameProfile().getName()).append("§0: §l").append(kills).append("\n"); });
        sb.append("§6================================="); broadcastMessage(sb.toString());
    }

    private static void playVictoryEffects() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer(); if (server == null) return;
        for (ServerPlayerEntity player : server.getPlayerList().getPlayers()) {
            server.getCommandManager().handleCommand(server.getCommandSource(), "playsound minecraft:ui.toast.challenge_complete master " + player.getGameProfile().getName());
        }
    }

    private static void sendBigTitle(String cmd1, String cmd2) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) { server.getCommandManager().handleCommand(server.getCommandSource(), cmd1); server.getCommandManager().handleCommand(server.getCommandSource(), cmd2); }
    }

    public static void startNewRound() {
        DEAD_PLAYERS.clear(); roundTimeLeft = MatchSystem.configRoundTime; freezeTimeLeft = MatchSystem.configFreezeTime;
        isFreezePeriod = true; MatchSystem.isRoundActive = true; MatchSystem.isPaused = false; resetPlayersToSpawn(); broadcastMessage("§a[MineStrike] New round started!");
    }

    public static void resetPlayersToSpawn() {
        for (ServerPlayerEntity player : getMatchPlayers()) {
            player.getServer().execute(() -> {
                player.setGameMode(GameType.SURVIVAL);
                player.setPositionAndUpdate(0.5, 64, 0.5);
            });
        }
    }

    public static void forceStopMatch() { MatchSystem.isMatchStarted = false; MatchSystem.isRoundActive = false; MatchSystem.isPaused = false; }
    public static void broadcastMessage(String text) { getMatchPlayers().forEach(p -> p.sendMessage(new StringTextComponent(text), p.getUniqueID())); }
}

class StatsManager {
    private static final Map<UUID, Integer> playerKills = new HashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static void addKill(UUID uuid) { playerKills.put(uuid, playerKills.getOrDefault(uuid, 0) + 1); }
    public static int getKills(UUID uuid) { return playerKills.getOrDefault(uuid, 0); }
    public static void resetStats() { playerKills.clear(); saveStatsToFile(); }
    public static void saveStatsToFile() {
        File configFile = new File(FMLPaths.CONFIGDIR.get().toFile(), "matchsystem/session.json"); JsonObject rootJson = new JsonObject();
        rootJson.addProperty("t_score", MatchSystem.tPoints); rootJson.addProperty("ct_score", MatchSystem.ctPoints);
        rootJson.add("players_statistics", new JsonObject()); try (FileWriter writer = new FileWriter(configFile)) { GSON.toJson(rootJson, writer); } catch (IOException ignored) {}
    }
}

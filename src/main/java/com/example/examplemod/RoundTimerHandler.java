package com.example.examplemod;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "examplemod")
public class RoundTimerHandler {
    private static int tickCounter = 0;
    public static int roundTimeLeft = 115;
    public static int freezeTimeLeft = 15;
    public static boolean isFreezePeriod = true;

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
            else { isFreezePeriod = false; RoundManager.broadcastMessage(RoundManager.getMsg("round_start")); }
            return;
        }
        if (roundTimeLeft > 0) { roundTimeLeft--; } 
        else { RoundManager.endRound("ct", "§bTime Expired! CT Win."); }
    }

    private static void drawMatchHUD() {
        String timerStr;
        String timerColor = roundTimeLeft <= 10 && !isFreezePeriod ? "§c" : "§a";
        if (MatchSystem.isPaused) { timerStr = "§c§l" + RoundManager.getMsg("hud_paused"); } 
        else if (!MatchSystem.isRoundActive) { timerStr = "§7" + RoundManager.getMsg("hud_waiting"); } 
        else if (isFreezePeriod) { timerStr = "§e" + RoundManager.getMsg("hud_freeze") + ": " + freezeTimeLeft + "s"; } 
        else { timerStr = timerColor + formatTime(roundTimeLeft); }
        
        String hudText = "§6§lT §7[" + MatchSystem.tPoints + "] §7| " + timerStr + " §7| §7[" + MatchSystem.ctPoints + "] §b§lCT";
        RoundManager.getMatchPlayers().forEach(p -> p.sendStatusMessage(new StringTextComponent(hudText), true));
    }

    @SubscribeEvent
    public static void onPlayerDisconnect(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!MatchSystem.isModEnabled || !MatchSystem.isMatchStarted || !MatchSystem.isRoundActive) return;
        UUID uuid = event.getPlayer().getUniqueID();
        if (MatchSystem.T_TEAM.contains(uuid) || MatchSystem.CT_TEAM.contains(uuid)) {
            if (!MatchSystem.isPaused) {
                RoundManager.togglePause();
                RoundManager.broadcastMessage("§c[MineStrike] Player " + event.getPlayer().getGameProfile().getName() + " disconnected. MATCH PAUSED!");
            }
        }
    }

    private static String formatTime(int secs) { return String.format("%02d:%02d", secs / 60, secs % 60); }
}

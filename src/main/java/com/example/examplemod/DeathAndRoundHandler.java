package com.example.examplemod;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.world.GameType;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid = "examplemod")
public class DeathAndRoundHandler {
    public static final Set<UUID> DEAD_PLAYERS = new HashSet<>();

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
            RoundManager.broadcastMessage("§7[MineStrike] " + victim.getGameProfile().getName() + " was killed!");
            checkWinConditions();
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!MatchSystem.isModEnabled || !MatchSystem.isMatchStarted) return;
        ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
        UUID uuid = player.getUniqueID();
        
        if (DEAD_PLAYERS.contains(uuid)) {
            player.getServer().execute(() -> {
                if (player.isAlive()) {
                    player.setGameMode(GameType.SPECTATOR);
                    player.addPotionEffect(new EffectInstance(Effects.BLINDNESS, 999999, 255, false, false));
                }
            });
        }
    }

    public static void checkWinConditions() {
        int aliveT = 0, aliveCT = 0;
        for (UUID uuid : MatchSystem.T_TEAM) if (!DEAD_PLAYERS.contains(uuid)) aliveT++;
        for (UUID uuid : MatchSystem.CT_TEAM) if (!DEAD_PLAYERS.contains(uuid)) aliveCT++;

        if (aliveT == 0 && !MatchSystem.T_TEAM.isEmpty()) {
            RoundManager.endRound("ct", "§bAll Terrorists eliminated! CT Win.");
        } else if (aliveCT == 0 && !MatchSystem.CT_TEAM.isEmpty()) {
            RoundManager.endRound("t", "§6All Counter-Terrorists eliminated! T Win.");
        }
    }
}

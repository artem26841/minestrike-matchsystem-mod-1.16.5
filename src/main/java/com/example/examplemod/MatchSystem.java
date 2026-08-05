package com.example.examplemod;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import java.io.File;
import java.util.*;

@Mod("examplemod")
public class MatchSystem {
    public static final String MODID = "examplemod";
    public static final Set<UUID> T_TEAM = new HashSet<>();
    public static final Set<UUID> CT_TEAM = new HashSet<>();
    public static final Set<UUID> SPECTATORS = new HashSet<>();
    public static final Set<UUID> MATCH_ADMINS = new HashSet<>();

    public static boolean isModEnabled = false;
    public static boolean isMatchStarted = false;
    public static boolean isRoundActive = false;
    public static boolean isPaused = false;
    public static boolean isAutoMode = false;
    public static String currentLanguage = "ru";

    public static int configRoundTime = 115;
    public static int configFreezeTime = 15;
    public static int configMaxPoints = 16;
    public static int tPoints = 0;
    public static int ctPoints = 0;
    public static long matchStartTime = 0;
    public static long matchEndTime = 0;

    public MatchSystem() {
        MinecraftForge.EVENT_BUS.register(this);
        createConfigFolder();
    }

    private void createConfigFolder() {
        File configDir = new File(FMLPaths.CONFIGDIR.get().toFile(), "matchsystem");
        if (!configDir.exists()) configDir.mkdirs();
    }

    public static void removeFromAllTeams(UUID uuid) {
        T_TEAM.remove(uuid);
        CT_TEAM.remove(uuid);
        SPECTATORS.remove(uuid);
        MATCH_ADMINS.remove(uuid);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        ModCommands.register(event.getDispatcher());
    }
}

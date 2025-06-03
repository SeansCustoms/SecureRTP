package com.clansmp.RTP;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.Objects;
import java.util.logging.Level;

import com.clansmp.RTP.managers.RTPManager;
import com.clansmp.RTP.commands.RTPCommand;

public final class RandomTeleportGP extends JavaPlugin {

    private static RandomTeleportGP instance; // Singleton instance

    private RTPManager rtpManager; 

    @Override
    public void onEnable() {
        instance = this;
        getLogger().log(Level.INFO, "SecureRTP has been enabled!");

        // 1. Save the default config if it doesn't exist.
        //    This ensures your config.yml is present.
        saveDefaultConfig();

        // 2. Initialize RTPManager BEFORE loading config values into it.
        //    The manager needs to exist to receive the settings.
        this.rtpManager = new RTPManager(this);

        // 3. Load configuration values into the RTPManager.
        //    This method will reload the config from disk and apply settings.
        loadConfigValuesToRTPManager();

        // 4. Register commands, ensuring they use the initialized rtpManager.
        Objects.requireNonNull(getCommand("rtp")).setExecutor(new RTPCommand(this, this.rtpManager));
        Objects.requireNonNull(getCommand("reloadrtp")).setExecutor(new RTPCommand(this, this.rtpManager));
    }

    public void loadConfigValuesToRTPManager() {
        // Always reload the config from disk to get the latest values
        reloadConfig();
        FileConfiguration config = getConfig(); // Get the current configuration

        // Apply general teleport settings
        rtpManager.setMinX(config.getInt("teleport-range.min-x", -10000));
        rtpManager.setMaxX(config.getInt("teleport-range.max-x", 10000));
        rtpManager.setMinZ(config.getInt("teleport-range.min-z", -10000));
        rtpManager.setMaxZ(config.getInt("teleport-range.max-z", 10000));
        rtpManager.setMaxAttempts(config.getInt("max-attempts-per-teleport", 50));

        // Apply optional plugin avoidance settings
        rtpManager.setAvoidGriefPreventionClaims(config.getBoolean("avoid-griefprevention-claims", true));
        rtpManager.setAvoidWorldGuardRegions(config.getBoolean("avoid-worldguard-regions", true));

        // Apply cooldown settings
        rtpManager.setCooldownEnabled(config.getBoolean("cooldown-enabled", true));
        rtpManager.setCooldownSeconds(config.getInt("cooldown-seconds", 10));

        // Apply action bar countdown settings
        rtpManager.setActionBarCountdownEnabled(config.getBoolean("action-bar-countdown-enabled", true));

        // --- IMPORTANT: Adjusting paths for action bar messages ---
        // Now reading from the 'messages' section
        String countdownFormat = config.getString("messages.teleport-countdown");
        if (countdownFormat == null || countdownFormat.isEmpty()) {
            countdownFormat = "<gold>Teleporting in <white>%time%</white> seconds..."; // Note: using %time% to match your config
            getLogger().warning("messages.teleport-countdown is missing or empty in config.yml. Using default.");
        }
        // MiniMessage uses %seconds% by convention for numbers, so replace %time%
        rtpManager.setActionBarCountdownFormat(countdownFormat.replace("%time%", "%seconds%"));


        String teleportingMessage = config.getString("messages.teleporting");
        if (teleportingMessage == null || teleportingMessage.isEmpty()) {
            teleportingMessage = "<green>Teleporting...";
            getLogger().warning("messages.teleporting is missing or empty in config.yml. Using default.");
        }
        // This message is also used in the action bar, so set it here
        rtpManager.setActionBarTeleportingMessage(teleportingMessage);

        // Set the total duration of the action bar countdown
        rtpManager.setActionBarTeleportDelaySeconds(config.getInt("action-bar-countdown-duration", 5));

        // Set the interval at which the action bar updates (convert seconds to ticks)
        // 1 second = 20 ticks
        rtpManager.setActionBarCountdownIntervalTicks(config.getInt("action-bar-countdown-interval", 1) * 20);

        getLogger().log(Level.INFO, "Configuration values loaded/reloaded into RTPManager.");
    }


    @Override
    public void onDisable() {
        getLogger().log(Level.INFO, "SecureRTP has been disabled!");
    }

    public static RandomTeleportGP getInstance() {
        return instance;
    }

    public void reloadPluginConfig() {
        reloadConfig();
        this.rtpManager.initializePluginChecks();
        loadConfigValuesToRTPManager();
        getLogger().log(Level.INFO, "SecureRTP configuration reloaded.");
    }
}
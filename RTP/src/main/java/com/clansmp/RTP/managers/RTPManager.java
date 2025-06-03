package com.clansmp.RTP.managers;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Sound;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;

import com.clansmp.RTP.RandomTeleportGP;

public class RTPManager {

    private final RandomTeleportGP plugin;
    private final Random random;

    // Configuration options
    private int minX;
    private int maxX;
    private int minZ;
    private int maxZ;
    private int maxAttempts;

    // Flags for optional plugins
    private boolean griefPreventionEnabled = false;
    private boolean worldGuardEnabled = false;
    private boolean avoidGriefPreventionClaims = true;
    private boolean avoidWorldGuardRegions = true;

    // Cooldown variables
    private boolean cooldownEnabled;
    private int cooldownSeconds;

    // Action Bar Countdown variables
    private boolean actionBarCountdownEnabled;
    @SuppressWarnings("unused")
	private String actionBarCountdownFormat;
    @SuppressWarnings("unused")
	private String actionBarTeleportingMessage;
    private int actionBarCountdownIntervalTicks;
    private int actionBarTeleportDelaySeconds;

    private final Map<UUID, Long> cooldowns;


    public RTPManager(RandomTeleportGP plugin) {
        this.plugin = plugin;
        this.random = new Random();
        this.cooldowns = new HashMap<>();
        this.actionBarCountdownIntervalTicks = 20;
        this.actionBarTeleportDelaySeconds = 0;
        initializePluginChecks();
    }

    public void initializePluginChecks() {
        Plugin gpPlugin = Bukkit.getPluginManager().getPlugin("GriefPrevention");
        if (gpPlugin != null && gpPlugin.isEnabled()) {
            griefPreventionEnabled = true;
            plugin.getLogger().log(Level.INFO, "GriefPrevention detected and enabled. Claims will be avoided if configured.");
        } else {
            griefPreventionEnabled = false;
            plugin.getLogger().log(Level.INFO, "GriefPrevention not detected or not enabled. Claims will not be avoided.");
        }

        Plugin wgPlugin = Bukkit.getPluginManager().getPlugin("WorldGuard");
        if (wgPlugin != null && wgPlugin.isEnabled()) {
            worldGuardEnabled = true;
            plugin.getLogger().log(Level.INFO, "WorldGuard detected and enabled. Regions will be avoided if configured.");
        } else {
            worldGuardEnabled = false;
            plugin.getLogger().log(Level.INFO, "WorldGuard not detected or not enabled. Regions will not be avoided.");
        }
    }

    public void teleportPlayerRandomly(Player player) {
        if (cooldownEnabled && isOnCooldown(player)) {
            long timeLeft = (cooldowns.get(player.getUniqueId()) - System.currentTimeMillis()) / 1000;
            // Using direct player.sendMessage with Component
            player.sendMessage(
                Component.text("You are on cooldown! Please wait ", NamedTextColor.RED)
                    .append(Component.text(timeLeft, NamedTextColor.RED).decorate(TextDecoration.BOLD))
                    .append(Component.text(" more second(s).", NamedTextColor.RED))
            );
            return;
        }

        // Using direct player.sendMessage with Component
        player.sendMessage(
            Component.text("Searching for a safe random location...", NamedTextColor.AQUA)
        );

        new BukkitRunnable() {
            int attempts = 0;

            @Override
            public void run() {
                if (attempts >= maxAttempts) {
                    // Using direct player.sendMessage with Component
                    player.sendMessage(
                        Component.text("Could not find a safe location after " + maxAttempts + " attempts. Please try again.", NamedTextColor.RED)
                    );
                    cancel();
                    return;
                }

                Location randomLoc = generateRandomLocation(player.getWorld());
                if (randomLoc == null) {
                    attempts++;
                    return;
                }

                if (isLocationSafe(randomLoc)) {
                    startTeleportCountdown(player, randomLoc);
                    cancel();
                } else {
                    attempts++;
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void startTeleportCountdown(Player player, Location targetLocation) {
        if (!actionBarCountdownEnabled || actionBarTeleportDelaySeconds <= 0) {
            performTeleport(player, targetLocation);
            return;
        }

        new BukkitRunnable() {
            int countdown = actionBarTeleportDelaySeconds;

            @Override
            public void run() {
                if (countdown <= 0) {
                    performTeleport(player, targetLocation);
                    cancel();
                    return;
                }

                // --- Direct Component Construction for Countdown Message ---
                Component message = (Component.text("Teleporting in ", NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
                        .append(Component.text(countdown, NamedTextColor.WHITE).decorate(TextDecoration.BOLD))
                        .append(Component.text(" seconds...", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));

                // Using direct player.sendActionBar
                player.sendActionBar(message);
                
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f);

                countdown--;
            }
        }.runTaskTimer(plugin, 0L, (long) actionBarCountdownIntervalTicks);
    }

    private void performTeleport(Player player, Location location) {
        player.teleport(location);
        // Using direct player.sendMessage with Component
        player.sendMessage(
            Component.text("You have been teleported to X: " + location.getBlockX() + ", Y: " + location.getBlockY() + ", Z: " + location.getBlockZ(), NamedTextColor.GREEN)
        );
        
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

        if (actionBarCountdownEnabled) {
            // --- Direct Component Construction for Teleporting Message ---
            Component message = (Component.text("Teleporting...", NamedTextColor.GREEN).decorate(TextDecoration.BOLD));

            // Using direct player.sendActionBar
            player.sendActionBar(message);
        }

        if (cooldownEnabled) {
            cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + (cooldownSeconds * 1000L));
        }
    }

    public boolean isOnCooldown(Player player) {
        return cooldowns.containsKey(player.getUniqueId()) &&
               cooldowns.get(player.getUniqueId()) > System.currentTimeMillis();
    }

    private Location generateRandomLocation(World world) {
        int x = random.nextInt(maxX - minX + 1) + minX;
        int z = random.nextInt(maxZ - minZ + 1) + minZ;

        for (int y = world.getMaxHeight() - 1; y >= world.getMinHeight(); y--) {
            Location potentialLoc = new Location(world, x, y, z);
            Block block = potentialLoc.getBlock();
            Block blockBelow = potentialLoc.clone().subtract(0, 1, 0).getBlock();

            if (block.getType().isAir() && blockBelow.getType().isSolid() &&
                new Location(world, x, y + 1, z).getBlock().getType().isAir() &&
                new Location(world, x, y + 2, z).getBlock().getType().isAir()) {

                boolean clearToSky = true;
                for (int currentY = y + 3; currentY < world.getMaxHeight(); currentY++) {
                    if (!world.getBlockAt(x, currentY, z).getType().isAir()) {
                        clearToSky = false;
                        break;
                    }
                }

                if (clearToSky) {
                    return potentialLoc.add(0.5, 0, 0.5);
                }
            }
        }
        return null;
    }

    private boolean isLocationSafe(Location location) {
        Block block = location.getBlock();
        Block blockBelow = location.clone().subtract(0, 1, 0).getBlock();

        if (block.getType() == Material.LAVA || block.getType() == Material.FIRE ||
            blockBelow.getType() == Material.LAVA || blockBelow.getType() == Material.FIRE ||
            block.getType() == Material.VOID_AIR || blockBelow.getType() == Material.VOID_AIR ||
            location.getY() < location.getWorld().getMinHeight() || location.getY() >= location.getWorld().getMaxHeight() - 1) {
            return false;
        }

        if (!new Location(location.getWorld(), location.getX(), location.getY() + 1, location.getZ()).getBlock().getType().isAir() ||
            !new Location(location.getWorld(), location.getX(), location.getY() + 2, location.getZ()).getBlock().getType().isAir()) {
            return false;
        }

        if (griefPreventionEnabled && avoidGriefPreventionClaims && isGriefPreventionClaimed(location)) {
            plugin.getLogger().log(Level.INFO, "Location " + location.toString() + " is within a GriefPrevention claim (avoiding).");
            return false;
        }

        if (worldGuardEnabled && avoidWorldGuardRegions && isWorldGuardRegion(location)) {
            plugin.getLogger().log(Level.INFO, "Location " + location.toString() + " is within a WorldGuard region (avoiding).");
            return false;
        }

        return true;
    }

    private boolean isGriefPreventionClaimed(Location location) {
        try {
            Claim claim = GriefPrevention.instance.dataStore.getClaimAt(location, true, null);
            return claim != null;
        } catch (NoClassDefFoundError | Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error accessing GriefPrevention API at runtime: " + e.getMessage());
            return false;
        }
    }

    private boolean isWorldGuardRegion(Location location) {
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regionManager = container.get(BukkitAdapter.adapt(location.getWorld()));

            if (regionManager != null) {
                BlockVector3 blockVector = BukkitAdapter.adapt(location).toVector().toBlockPoint();
                for (@SuppressWarnings("unused") ProtectedRegion region : regionManager.getApplicableRegions(blockVector)) {
                    return true;
                }
            }
        } catch (NoClassDefFoundError | Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error accessing WorldGuard API at runtime: " + e.getMessage());
            return false;
        }
        return false;
    }

    // --- Public Setter Methods for Configuration Values ---

    public void setMinX(int minX) {
        this.minX = minX;
    }

    public void setMaxX(int maxX) {
        this.maxX = maxX;
    }

    public void setMinZ(int minZ) {
        this.minZ = minZ;
    }

    public void setMaxZ(int maxZ) {
        this.maxZ = maxZ;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public void setAvoidGriefPreventionClaims(boolean avoid) {
        this.avoidGriefPreventionClaims = avoid;
        plugin.getLogger().log(Level.INFO, "Avoiding GriefPrevention claims set to: " + avoid);
    }

    public void setAvoidWorldGuardRegions(boolean avoid) {
        this.avoidWorldGuardRegions = avoid;
        plugin.getLogger().log(Level.INFO, "Avoiding WorldGuard regions set to: " + avoid);
    }

    public void setCooldownEnabled(boolean cooldownEnabled) {
        this.cooldownEnabled = cooldownEnabled;
        plugin.getLogger().log(Level.INFO, "RTP Cooldowns enabled: " + cooldownEnabled);
    }

    public void setCooldownSeconds(int cooldownSeconds) {
        this.cooldownSeconds = cooldownSeconds;
        plugin.getLogger().log(Level.INFO, "RTP Cooldown duration: " + cooldownSeconds + " seconds.");
    }

    public void setActionBarCountdownEnabled(boolean actionBarCountdownEnabled) {
        this.actionBarCountdownEnabled = actionBarCountdownEnabled;
        plugin.getLogger().log(Level.INFO, "RTP Action Bar Countdown enabled: " + actionBarCountdownEnabled);
    }

    public void setActionBarCountdownFormat(String actionBarCountdownFormat) {
        this.actionBarCountdownFormat = actionBarCountdownFormat;
        plugin.getLogger().log(Level.INFO, "RTP Action Bar Countdown format: " + actionBarCountdownFormat);
    }

    public void setActionBarTeleportingMessage(String actionBarTeleportingMessage) {
        this.actionBarTeleportingMessage = actionBarTeleportingMessage;
        plugin.getLogger().log(Level.INFO, "RTP Action Bar Teleporting message: " + actionBarTeleportingMessage);
    }

    public void setActionBarTeleportDelaySeconds(int delaySeconds) {
        this.actionBarTeleportDelaySeconds = delaySeconds;
        plugin.getLogger().log(Level.INFO, "Action Bar Teleport Delay (countdown duration): " + delaySeconds + " seconds.");
    }

    public void setActionBarCountdownIntervalTicks(int intervalTicks) {
        if (intervalTicks < 1) {
            intervalTicks = 1;
            plugin.getLogger().log(Level.WARNING, "Action Bar Countdown Interval must be at least 1 tick. Setting to 1.");
        }
        this.actionBarCountdownIntervalTicks = intervalTicks;
        plugin.getLogger().log(Level.INFO, "Action Bar Countdown Interval: " + intervalTicks + " ticks.");
    }
}

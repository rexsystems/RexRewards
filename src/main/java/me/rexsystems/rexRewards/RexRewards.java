package me.rexsystems.rexRewards;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.rexsystems.rexRewards.claim.ClaimService;
import me.rexsystems.rexRewards.command.RewardsCommand;
import me.rexsystems.rexRewards.config.Settings;
import me.rexsystems.rexRewards.feedback.Feedback;
import me.rexsystems.rexRewards.gui.MenuListener;
import me.rexsystems.rexRewards.gui.RewardMenus;
import me.rexsystems.rexRewards.menu.Menus;
import me.rexsystems.rexRewards.storage.Database;
import me.rexsystems.rexRewards.util.SchedulerUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class RexRewards extends JavaPlugin {

    private Settings settings;
    private Menus menus;
    private Database database;
    private Feedback feedback;
    private RewardMenus rewardMenus;
    private ScheduledTask refreshTask;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().severe("Could not create the plugin folder.");
        }
        this.settings = new Settings(this);
        this.menus = new Menus(this);
        this.feedback = new Feedback(this, settings);
        settings.load();
        menus.load();
        this.database = new Database(this);
        try {
            database.open(settings.storage());
        } catch (Exception failure) {
            getLogger().severe("Could not open storage: " + failure.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.rewardMenus = new RewardMenus(this, menus, database, settings);
        ClaimService claims = new ClaimService(this, database, feedback, rewardMenus);
        rewardMenus.bind(claims);
        getServer().getPluginManager().registerEvents(new MenuListener(rewardMenus), this);

        RewardsCommand command = new RewardsCommand(this);
        PluginCommand pluginCommand = getCommand("rexrewards");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }

        startRefresh();
        getLogger().info("RexRewards enabled.");
    }

    @Override
    public void onDisable() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
        if (database != null) {
            database.close();
        }
        getLogger().info("RexRewards disabled.");
    }

    public void reloadPlugin() {
        settings.load();
        menus.load();
        if (database != null) {
            database.swapIfChanged(settings.storage());
        }
        startRefresh();
        SchedulerUtils.runSync(this, () -> {
            if (rewardMenus != null) {
                rewardMenus.reopenAll();
            }
        });
    }

    private void startRefresh() {
        if (refreshTask != null) {
            refreshTask.cancel();
        }
        long period = settings.refreshTicks();
        refreshTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(this, task -> {
            if (database == null || !database.accepting() || rewardMenus == null) {
                return;
            }
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.getScheduler().run(this, scheduled -> rewardMenus.refreshIfDue(player), null);
            }
        }, period, period);
    }

    public Settings settings() {
        return settings;
    }

    public Menus menus() {
        return menus;
    }

    public Database database() {
        return database;
    }

    public Feedback feedback() {
        return feedback;
    }

    public RewardMenus rewardMenus() {
        return rewardMenus;
    }
}

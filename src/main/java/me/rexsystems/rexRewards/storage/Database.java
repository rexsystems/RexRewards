package me.rexsystems.rexRewards.storage;

import me.rexsystems.rexRewards.RexRewards;
import me.rexsystems.rexRewards.config.StorageSettings;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class Database {

    static final String CREATE = """
            CREATE TABLE IF NOT EXISTS rexrewards_claims (
                player_uuid VARCHAR(36) NOT NULL,
                menu_id VARCHAR(64) NOT NULL,
                reward_id VARCHAR(64) NOT NULL,
                period_key VARCHAR(64) NOT NULL,
                claimed_at BIGINT NOT NULL,
                PRIMARY KEY (player_uuid, menu_id, reward_id, period_key)
            )
            """;

    private final RexRewards plugin;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "RexRewards-Storage");
        thread.setDaemon(true);
        return thread;
    });
    private HikariDataSource source;
    private StorageSettings openSettings;
    private volatile boolean accepting = true;

    public Database(RexRewards plugin) {
        this.plugin = plugin;
    }

    public void open(StorageSettings settings) throws SQLException {
        HikariDataSource opened = create(settings);
        try {
            init(opened, settings);
        } catch (SQLException failure) {
            opened.close();
            throw failure;
        }
        this.source = opened;
        this.openSettings = settings;
        plugin.getLogger().info("Storage ready (" + settings.type() + ").");
    }

    public void swapIfChanged(StorageSettings settings) {
        if (settings.equals(openSettings)) {
            return;
        }
        supply(() -> {
            HikariDataSource opened = create(settings);
            try {
                init(opened, settings);
            } catch (SQLException failure) {
                opened.close();
                throw failure;
            }
            HikariDataSource previous = this.source;
            this.source = opened;
            this.openSettings = settings;
            if (previous != null) {
                previous.close();
            }
            return true;
        }).whenComplete((ok, error) -> {
            if (error != null) {
                plugin.getLogger().warning("Storage reconnect failed: " + root(error).getMessage());
            } else {
                plugin.getLogger().info("Storage reconnected (" + settings.type() + ").");
            }
        });
    }

    public boolean accepting() {
        return accepting;
    }

    public CompletableFuture<Boolean> insert(UUID player, String menuId, String rewardId, String periodKey) {
        return supply(() -> insertNow(player, menuId, rewardId, periodKey));
    }

    public CompletableFuture<Void> delete(UUID player, String menuId, String rewardId, String periodKey) {
        return supply(() -> {
            deleteNow(player, menuId, rewardId, periodKey);
            return null;
        });
    }

    public CompletableFuture<Integer> deleteMenu(UUID player, String menuId) {
        return supply(() -> deleteMenuNow(player, menuId));
    }

    public CompletableFuture<Set<String>> claimKeys(UUID player, String menuId) {
        return supply(() -> claimKeysNow(player, menuId));
    }

    public void close() {
        accepting = false;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
        if (source != null) {
            source.close();
            source = null;
        }
    }

    private <T> CompletableFuture<T> supply(SqlSupplier<T> supplier) {
        if (!accepting) {
            return CompletableFuture.failedFuture(new IllegalStateException("Storage is closed."));
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                return supplier.get();
            } catch (Exception exception) {
                throw new CompletionException(exception);
            }
        }, executor);
    }

    private boolean insertNow(UUID player, String menuId, String rewardId, String periodKey) throws SQLException {
        String sql = "INSERT INTO rexrewards_claims (player_uuid, menu_id, reward_id, period_key, claimed_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = source.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, player.toString());
            statement.setString(2, menuId);
            statement.setString(3, rewardId);
            statement.setString(4, periodKey);
            statement.setLong(5, System.currentTimeMillis());
            statement.executeUpdate();
            return true;
        } catch (SQLException exception) {
            // SQLite and MySQL use different codes for the same primary key collision.
            if (duplicate(exception)) {
                return false;
            }
            throw exception;
        }
    }

    private void deleteNow(UUID player, String menuId, String rewardId, String periodKey) throws SQLException {
        String sql = "DELETE FROM rexrewards_claims WHERE player_uuid = ? AND menu_id = ? AND reward_id = ? AND period_key = ?";
        try (Connection connection = source.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, player.toString());
            statement.setString(2, menuId);
            statement.setString(3, rewardId);
            statement.setString(4, periodKey);
            statement.executeUpdate();
        }
    }

    private int deleteMenuNow(UUID player, String menuId) throws SQLException {
        String sql = "DELETE FROM rexrewards_claims WHERE player_uuid = ? AND menu_id = ?";
        try (Connection connection = source.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, player.toString());
            statement.setString(2, menuId);
            return statement.executeUpdate();
        }
    }

    private Set<String> claimKeysNow(UUID player, String menuId) throws SQLException {
        String sql = "SELECT reward_id, period_key FROM rexrewards_claims WHERE player_uuid = ? AND menu_id = ?";
        Set<String> keys = new HashSet<>();
        try (Connection connection = source.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, player.toString());
            statement.setString(2, menuId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    keys.add(result.getString(1) + "\0" + result.getString(2));
                }
            }
        }
        return keys;
    }

    private void init(HikariDataSource opened, StorageSettings settings) throws SQLException {
        try (Connection connection = opened.getConnection();
             Statement statement = connection.createStatement()) {
            if (!settings.mysql()) {
                statement.execute("PRAGMA journal_mode=WAL");
                statement.execute("PRAGMA busy_timeout=5000");
            }
            statement.execute(CREATE);
        }
    }

    private HikariDataSource create(StorageSettings settings) {
        HikariConfig config = new HikariConfig();
        config.setPoolName("RexRewards");
        config.setConnectionTimeout(8_000L);
        config.setInitializationFailTimeout(8_000L);
        config.setConnectionTestQuery("SELECT 1");
        if (settings.mysql()) {
            // The relocated driver is invisible to DriverManager, so Hikari has to load it by name.
            config.setDriverClassName(driver(
                    "me.rexsystems.rexRewards.libs.mysql.cj.jdbc.Driver",
                    "com.mysql.cj.jdbc.Driver"));
            config.setJdbcUrl(mysqlUrl(settings));
            config.setUsername(settings.username());
            config.setPassword(settings.password());
            config.setMaximumPoolSize(settings.poolSize());
        } else {
            config.setDriverClassName("org.sqlite.JDBC");
            config.setJdbcUrl("jdbc:sqlite:" + sqliteFile(settings).getAbsolutePath());
            config.setMaximumPoolSize(1);
        }
        config.setMinimumIdle(1);
        return new HikariDataSource(config);
    }

    private File sqliteFile(StorageSettings settings) {
        String name = settings.sqliteFile() == null || settings.sqliteFile().isBlank() ? "rewards.db" : settings.sqliteFile();
        File file = new File(name);
        if (!file.isAbsolute()) {
            file = new File(plugin.getDataFolder(), name);
        }
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            plugin.getLogger().warning("Could not create " + parent.getAbsolutePath());
        }
        return file;
    }

    private static String mysqlUrl(StorageSettings settings) {
        String host = settings.host() == null ? "" : settings.host().trim();
        String database = settings.database() == null ? "" : settings.database().trim();
        if (!host.matches("[A-Za-z0-9._-]+") || !database.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("MySQL host or database contains unsupported characters.");
        }
        return "jdbc:mysql://" + host + ":" + settings.port() + "/" + database
                + "?useSSL=" + settings.useSsl()
                + "&characterEncoding=UTF-8"
                + (settings.useSsl() ? "" : "&allowPublicKeyRetrieval=true");
    }

    static boolean duplicate(Throwable failure) {
        return duplicate(failure, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    private static boolean duplicate(Throwable failure, Set<Throwable> seen) {
        Throwable current = failure;
        while (current != null && seen.add(current)) {
            if (current instanceof SQLException sql) {
                int code = sql.getErrorCode();
                if (code == 19 || code == 1062 || code == 1555 || code == 2067) {
                    return true;
                }
                String state = sql.getSQLState();
                if ("23000".equals(state) || "23505".equals(state)) {
                    return true;
                }
                String message = sql.getMessage();
                if (message != null) {
                    String lower = message.toLowerCase(Locale.ROOT);
                    if (lower.contains("unique constraint") || lower.contains("duplicate entry")) {
                        return true;
                    }
                }
                SQLException next = sql.getNextException();
                if (next != null && duplicate(next, seen)) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private static String driver(String relocated, String plain) {
        try {
            Class.forName(relocated, false, Database.class.getClassLoader());
            return relocated;
        } catch (ClassNotFoundException exception) {
            return plain;
        }
    }

    private static Throwable root(Throwable failure) {
        Throwable current = failure;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    @FunctionalInterface
    private interface SqlSupplier<T> {
        T get() throws Exception;
    }
}

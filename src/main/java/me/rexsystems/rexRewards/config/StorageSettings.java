package me.rexsystems.rexRewards.config;

public record StorageSettings(
        String type,
        String sqliteFile,
        String host,
        int port,
        String database,
        String username,
        String password,
        int poolSize,
        boolean useSsl
) {

    public boolean mysql() {
        return "mysql".equals(type);
    }
}

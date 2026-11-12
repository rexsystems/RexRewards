---
title: Storage
description: SQLite by default, MySQL when several servers share claims.
sidebar:
  order: 5
---

```yaml
storage:
  type: sqlite
  sqlite:
    file: rewards.db
  mysql:
    host: 127.0.0.1
    port: 3306
    database: rexrewards
    username: rexrewards
    password: ""
    pool-size: 4
    use-ssl: false
```

`type` is `sqlite` or `mysql`. Anything else is treated as SQLite. The MySQL driver and the connection pool are packed inside the jar under `me.rexsystems.rexRewards.libs`. The SQLite driver is packed too, and it stays on `org.sqlite` because the native library looks that package up by name.

SQLite writes `rewards.db` in the plugin folder unless `file` is an absolute path. The pool size is 1. The file uses WAL mode.

MySQL is the option for more than one server. Point every server at the same database and use the same menu ids, timezones, and `week-start` values. If two servers disagree on the timezone, they will write different period keys for the same calendar day and a player can claim twice.

The table is `rexrewards_claims`. The primary key is `player_uuid`, `menu_id`, `reward_id`, and `period_key`. That key is the same on SQLite and MySQL.

Reload does not drop the table and does not delete rows. `/rexrewards admin reset <player> <menu>` deletes every row for that player and menu, including older days and weeks.

Queries run off the region thread. If the database cannot be opened on startup, the plugin disables itself instead of giving rewards it cannot record.

The host may contain letters, digits, dots, underscores, and hyphens. The database name may contain letters, digits, and underscores. The password is passed to the pool separately and is not written into the log by RexRewards.

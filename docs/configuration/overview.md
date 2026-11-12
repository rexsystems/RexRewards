---
title: Settings
description: What config.yml controls, and what it leaves alone.
sidebar:
  order: 1
---

`config.yml` is messages, sounds, the claim title, the action bar, how often open menus check the clock, and where claims are stored.

`menus.yml` is the menus. It is not merged, backed up, or rewritten by the config updater.

```yaml
settings:
  refresh-ticks: 20
```

`refresh-ticks` is how often an open menu checks whether the day or week has rolled over. 20 ticks is one second. The menu stays open. The icons update when the period key changes.

`/rexrewards reload` reads both files again. It does not delete rows from the claim table. If you change the storage target, the plugin reconnects. Old rows stay in the previous database file or schema until you remove them yourself.

See [Storage](/rexrewards/configuration/storage/) for SQLite and MySQL, and [Messages](/rexrewards/configuration/messages/) for the text.

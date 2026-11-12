---
title: Commands
description: /rexrewards, /rrew, and /rewards.
sidebar:
  order: 1
---

The command is `/rexrewards`. Aliases are `/rrew` and `/rewards`. In messages, `{label}` is whichever of those the sender typed.

| Command | Who | What it does |
|---|---|---|
| `/rexrewards help` | everyone | Prints the command list from `messages.help` |
| `/rexrewards list` | `rexrewards.use` | Lists menus that player can open |
| `/rexrewards open <menu>` | `rexrewards.use` and the menu permission | Opens one menu |
| `/rexrewards reload` | `rexrewards.reload` | Reloads `config.yml` and re-reads `menus.yml` |
| `/rexrewards admin reset <player> <menu>` | `rexrewards.admin` | Deletes that player's claim rows for one menu |

Menu ids are matched without case sensitivity. The stored id is the one in `menus.yml`.

`open` is players only. `reset` accepts a name that has joined the server before, online or not. The menu must still be in `menus.yml`. Reset removes every period for that pair, not only the current day or week.

Reload keeps claim rows. It also reopens a menu that a player already has open, so a changed title or row count is applied.

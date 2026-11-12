---
title: Permissions
description: Defaults shipped with the plugin, and permissions named by a menu.
sidebar:
  order: 2
---

| Permission | Default | What it allows |
|---|---|---|
| `rexrewards.use` | true | `/rexrewards list` and `/rexrewards open` |
| `rexrewards.reload` | op | `/rexrewards reload` |
| `rexrewards.admin` | op | `/rexrewards admin reset` |
| `rexrewards.daily` | true | The default daily menu |
| `rexrewards.weekly` | true | The default weekly menu |
| `rexrewards.starter` | true | The default starter menu |

A menu's `permission` field is checked in addition to `rexrewards.use`. Leave it blank and only `rexrewards.use` is required.

The three menu permissions above match the menus shipped in `menus.yml`. A menu you add is not listed in `plugin.yml` until you add a node yourself. Bukkit still enforces whatever string you put in `permission`. Operators have the op defaults. `rexrewards.use` and the shipped menu nodes start granted to everyone.

Negating `rexrewards.use` blocks list and open even when the menu permission is still granted.

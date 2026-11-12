---
title: FAQ
description: Claims, timezones, full inventories, and reload.
sidebar:
  order: 1
---

## The player clicked twice and got one reward

That is the claim lock plus the database key. A second click on the same player and reward is dropped while the first click is still saving or giving items. After that, the row exists, so another click is told it was already claimed.

## The inventory was full and the reward is still available

The items did not fit, so the plugin deleted the row and did not run the commands. Make space and click again. The chat line is `messages.inventory-full`.

## Midnight passed on the clock but the daily reward did not reset

The menu uses its own `timezone`. UTC midnight is not Europe/Berlin midnight. The weekly key also waits for `week-start` in that zone. An open menu updates its icons on `settings.refresh-ticks` without a reopen. If the icons stay put, check the zone and the week start, then reload.

## A reward inside a daily menu never comes back

`one-time: true` stores the period key `once`. Remove that flag, then `/rexrewards admin reset <player> <menu>` if the row is already stored. Reset clears every reward in that menu for that player, not only one reward.

## Reload wiped my menus

Reload does not write `menus.yml`. A missing file is the case that copies the default out of the jar. If the file is present and still changed, another process edited it. `config.yml` can gain new keys, and the previous file is kept as `config.yml.bak.<timestamp>`. Claim rows are not part of either file.

## Two servers let the player claim the same day twice

MySQL only shares a claim when the period key matches. The key includes the menu timezone and, for weekly menus, `week-start`. Use the same menu ids and the same clock settings on every server that shares the database.

## A decorative slot was claimed

Decoration lives under `gui.slots`. Rewards live under `rewards`. A reward on the same slot replaces the decoration. The click only starts a claim when the slot is a reward.

## The plugin disabled itself on startup

The storage connection failed. Read the line above `Could not open storage` in the log. For SQLite, the plugin folder has to be writable. For MySQL, the host, database name, user, and password have to match a reachable server. The host and database name are restricted to simple characters so they cannot change the JDBC URL.

---
title: Messages
description: Chat, sounds, title, and action bar in config.yml.
sidebar:
  order: 2
---

Every player-facing string accepts MiniMessage, `&` codes, `&#RRGGBB`, and `#RRGGBB`. A blank string is skipped.

`{prefix}` in a message is replaced with `messages.prefix` before colors are parsed. Names that the plugin inserts are literal, so a player name cannot add formatting.

## Placeholders

| Token | Meaning |
|---|---|
| `{player}` | Account name |
| `{uuid}` | Player UUID |
| `{menu}` | Menu id |
| `{reward}` | Reward id |
| `{period}` | Period key, such as `2026-01-19` or `once` |
| `{count}` | Rows removed by an admin reset |
| `{label}` | The alias the sender typed |
| `{prefix}` | `messages.prefix` |

PlaceholderAPI tokens are resolved for online players in chat, the action bar, the title, item names, item lore, menu titles, and reward commands.

## Sounds

`sounds.claim`, `sounds.already-claimed`, `sounds.inventory-full`, and `sounds.unavailable` each have `enabled`, `sound`, `volume`, and `pitch`.

`sound` is a Bukkit name such as `ENTITY_EXPERIENCE_ORB_PICKUP`. Unknown names are logged once and then ignored.

## Title

`title.claim` plays when a claim is given. `fade-in`, `stay`, and `fade-out` are ticks, capped at 200. Set `enabled` to `false` to skip it.

## Action bar

`actionbar.claim`, `actionbar.already-claimed`, `actionbar.inventory-full`, and `actionbar.unavailable` are separate from the chat lines. Leave one blank to hide that bar.

## Menu icon lines

These are appended to the reward icon:

- `messages.gui.click-lore` when the reward can be claimed
- `messages.gui.claimed-lore` when this period is already claimed
- `messages.gui.unavailable-lore` when the chance roll missed

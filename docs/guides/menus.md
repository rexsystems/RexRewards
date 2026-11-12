---
title: Menus
description: Add a menu without losing the ones you already edited.
sidebar:
  order: 1
---

Menus live under the `menus` key. The key is the id players type in `/rexrewards open`. There is no fixed limit. Ids are 1 to 64 characters. Reward ids inside a menu follow the same limit.

Reload reads `menus.yml` from disk. It does not copy the jar over a file that is already there. Delete the file and reload, or restart, if you want the shipped default back. That also discards edits in that file. It does not delete claim rows. Old rows simply stop matching a menu that no longer exists until you reset them or add the id again.

## Add a weekend menu

```yaml
menus:
  weekend:
    type: weekly
    timezone: Europe/Berlin
    week-start: SATURDAY
    permission: rexrewards.weekend
    gui:
      rows: 1
      title: "<gold>Weekend"
      filler:
        material: ORANGE_STAINED_GLASS_PANE
        name: " "
      slots:
        "0":
          material: CLOCK
          name: "<gold>Saturday"
          lore:
            - "<gray>Resets Saturday at midnight"
    rewards:
      pearls:
        slot: 4
        chance: 100
        one-time: false
        commands: []
        items:
          - material: ENDER_PEARL
            amount: 2
            name: "<dark_aqua>Weekend pearls"
```

Give `rexrewards.weekend` to the players who should see it. The node does not have to be declared in `plugin.yml` to work. `/rexrewards list` shows the menu to players who have the permission.

The clock on slot 0 is decoration. The pearls on slot 4 are the reward. A click on the clock does nothing.

`one-time: true` on a reward inside this weekly menu keeps that reward on the period key `once`. The Saturday reset does not return it.

## Chance

`chance: 25` offers the reward on about a quarter of periods. The same player sees the same result for that reward until the period key changes. A missed roll is not stored, so you do not need to reset it. The next day or week rolls again.

## Commands and items together

Items are checked against free slots and leftover room in matching stacks. Commands are not simulated. A `give` command can still fail on its own if the inventory fills up after the items from `items` were placed. Put the inventory gifts in `items` when you want a full inventory to cancel the claim.

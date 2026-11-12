---
title: Rewards
description: Periods, chance, items, and commands.
sidebar:
  order: 4
---

A menu `type` is `daily`, `weekly`, or `once`.

`timezone` is a Java `ZoneId`, such as `UTC` or `America/New_York`. An unknown zone is logged and the menu uses UTC.

`week-start` is `MONDAY` through `SUNDAY`. Weekly menus use it. Daily and once menus still parse it, then ignore it.

`permission` blank means anyone with `rexrewards.use` can open the menu. Any other value is required as well.

## Period keys

The claim key is the player UUID, the menu id, the reward id, and a period key.

| Menu | Period key |
|---|---|
| `daily` | The local date in the menu timezone, `yyyy-MM-dd` |
| `weekly` | The date that week started, using `week-start` in that timezone |
| `once` | The word `once` |

A reward with `one-time: true` uses `once` even inside a daily or weekly menu. The date and the week do not bring that reward back.

The daily key changes at midnight in the menu timezone, including when UTC is already the next day. The weekly key changes at midnight at the start of `week-start`, not at Monday UTC unless the menu says so.

## A reward

```yaml
rewards:
  diamond:
    slot: 13
    chance: 100
    one-time: false
    commands:
      - "eco give {player} 100"
    items:
      - material: DIAMOND
        amount: 1
        name: "<aqua>Daily diamond"
        lore:
          - "<gray>One per day"
        custom-model-data: 1402
```

`slot` must sit inside the chest. Two rewards cannot share a slot.

`chance` is 0 through 100. The roll is fixed for that player, reward, and period, so reopening the menu does not reroll it. 100 is always offered. 0 is never offered. A miss does not write a claim row.

`items` are given into the survival inventory, not the armor slots. Each entry has `material`, `amount`, `name`, `lore`, and optional `custom-model-data`. Amounts above one stack are split. Tools use their own stack size of 1 when the plugin checks free space. Omit `custom-model-data` to leave the model alone. `0` is stored if you set it.

`commands` run as console after the items fit. A leading slash is optional. `{player}`, `{uuid}`, `{menu}`, `{reward}`, and `{period}` are replaced. Newlines in the finished command are turned into spaces.

## Order of a click

1. A second click on the same player and reward is ignored while the first click is still running.
2. The plugin inserts the claim row.
3. If that row already exists, the player is told they claimed it.
4. If the items do not fit, the row is deleted and the commands do not run.
5. If the items fit, they are added, then the commands run.

A crash after the insert and before the give cannot hand the reward out twice. The row is already there. A player who logs out before the give loses the row, so they can claim it later.

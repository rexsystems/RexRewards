---
title: GUI
description: Rows, title, filler, and decorative slots.
sidebar:
  order: 3
---

Each menu has a `gui` section.

```yaml
gui:
  rows: 3
  title: "<gold>Daily rewards"
  filler:
    material: GRAY_STAINED_GLASS_PANE
    name: " "
  slots:
    "4":
      material: CLOCK
      name: "<gold>Daily"
      lore:
        - "<gray>Resets at midnight"
```

`rows` is 1 through 6. The inventory size is `rows * 9`.

`title` is the chest title. PlaceholderAPI tokens work for the player who opened it.

`filler` fills every slot that is not a reward and not listed under `slots`. `name: " "` keeps the label short. A blank name hides the tooltip.

`gui.slots` are decoration. Clicks on them do nothing. They are not claimable, they have no period, and they are not stored. If a reward uses the same slot, the reward is shown and the decoration is skipped.

The plugin cancels clicks inside its own chest, including a shift-click that starts in the player inventory and would move items into the menu. Clicks that only rearrange the player inventory are left alone. Other plugins' inventories are not cancelled.

An open menu rebuilds its icons when the period key changes. The player does not have to close it. `/rexrewards reload` reopens menus that are already on screen so a new title or size is applied. Claim rows are not deleted.

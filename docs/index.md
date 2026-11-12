---
title: RexRewards
description: Daily, weekly, and one-time reward menus for Paper, Purpur, and Folia.
---

RexRewards opens chest menus of rewards. Each reward can be claimed once per day, once per week, or a single time. The period follows the menu's timezone, not the machine clock's default zone.

The plugin writes the claim row first. Items and commands run only after that insert succeeds. A full inventory deletes the row and skips the commands.

Text uses MiniMessage. Legacy `&` codes, `&#RRGGBB`, and `#RRGGBB` also work.

## Files

```
plugins/RexRewards/
  config.yml
  menus.yml
  rewards.db
```

`config.yml` holds messages, sounds, the title, the action bar, and storage. A reload that changes it writes `config.yml.bak.<timestamp>` before adding missing keys. Values you already set stay as they are.

`menus.yml` is yours. It is copied from the jar only when the file is missing. Reload never overwrites it and never deletes claim rows.

## Next

- [Installation](/rexrewards/getting-started/installation/)
- [Menus](/rexrewards/guides/menus/)
- [Commands](/rexrewards/reference/commands/)

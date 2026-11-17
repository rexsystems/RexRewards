# RexRewards

Daily, weekly, and one-time reward menus for Paper, Purpur, and Folia.

A claim is one database row: player, menu, reward, and period. The row is written before any items or commands run. If the inventory cannot hold the items, the row is deleted and the commands stay unused.

## Requirements

- Java 21
- Paper, Purpur, or Folia 1.21+
- PlaceholderAPI is optional
- MySQL is optional, for more than one server sharing claims

## Install

1. Put the jar in `plugins/`
2. Restart
3. Edit `plugins/RexRewards/config.yml` and `plugins/RexRewards/menus.yml`
4. Run `/rexrewards reload`

`menus.yml` is created only when it is missing. Reload reads it and does not write it back. Reload does not delete claims.

## Commands

| Command | Permission |
|---|---|
| `/rexrewards help` | everyone |
| `/rexrewards list` | `rexrewards.use` |
| `/rexrewards open <menu>` | `rexrewards.use` plus the menu permission |
| `/rexrewards reload` | `rexrewards.reload` |
| `/rexrewards admin reset <player> <menu>` | `rexrewards.admin` |

Aliases: `/rrew`, `/rewards`

## Documentation

The pages in [docs/](docs/index.md) drop into a Starlight site at `src/content/docs/rexrewards/`. Paste `docs/sidebar.mjs` into the site sidebar.

Full docs: [https://docs.rexsystems.me/rexrewards/](https://docs.rexsystems.me/rexrewards/)

## License

Licensed under the [GNU Affero General Public License v3.0](LICENSE).

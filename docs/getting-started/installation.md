---
title: Installation
description: Put RexRewards on a Paper, Purpur, or Folia server.
sidebar:
  order: 1
---

1. Download the jar.
2. Put it in the server `plugins/` folder.
3. Start the server once so `plugins/RexRewards/config.yml` and `menus.yml` are written.
4. Edit both files.
5. Run `/rexrewards reload`, or restart.

PlaceholderAPI is not required. If it is installed, `{player}` style tokens still work, and `%placeholder%` tokens in messages, item text, and reward commands are filled for that player.

MySQL is not required. The default database is `plugins/RexRewards/rewards.db`.

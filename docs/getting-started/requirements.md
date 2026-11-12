---
title: Requirements
description: What RexRewards needs to start.
sidebar:
  order: 2
---

- Java 21
- Paper, Purpur, or Folia 1.21 or newer
- PlaceholderAPI 2.11 or newer, only if you use its placeholders
- A MySQL 8 server, only if `storage.type` is `mysql`

The jar carries its own SQLite driver, MySQL driver, and pool. You do not install those on the server. MySQL and the pool live under `me.rexsystems.rexRewards.libs`. SQLite stays on `org.sqlite` because the native library looks that class up by name.

Folia is supported. Database work stays off the region thread. Items and reward commands run on the player who claimed them.

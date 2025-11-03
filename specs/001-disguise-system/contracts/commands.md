# Command API Contracts

**Feature**: 001-disguise-system
**Date**: 2025-10-23

## Overview

This document defines the command-line interface for the Hide and Seek plugin. All commands are registered in `plugin.yml` and handled by command executors.

---

## Player Commands

### `/hs join`

**Purpose**: Join the waiting lobby for the next game

**Permission**: `hideandseek.play` (default: true)

**Syntax**: `/hs join`

**Preconditions**:
- Player not already in a game
- No active game running (MVP: single arena)

**Success Response**:
```
§aゲームに参加しました！ (2/13人)
§7ゲーム開始まで待機中...
```

**Error Responses**:
- Already in game: `§cすでにゲームに参加しています`
- Game in progress: `§cゲームが既に進行中です。次のゲームをお待ちください。`
- Arena not configured: `§cアリーナが設定されていません。管理者に連絡してください。`

**Side Effects**:
- Player added to waiting list
- Broadcast to all waiting players: `§e{player} がゲームに参加しました (X/Y人)`

---

### `/hs leave`

**Purpose**: Leave the current game or waiting lobby

**Permission**: `hideandseek.play` (default: true)

**Syntax**: `/hs leave`

**Preconditions**:
- Player is in a game or waiting lobby

**Success Response**:
```
§aゲームから退出しました
```

**Error Responses**:
- Not in game: `§cゲームに参加していません`

**Side Effects**:
- If in waiting lobby: Removed from list
- If in active game: Backup restored, counted as eliminated if hider, removed from seeker count if seeker
- Broadcast: `§e{player} がゲームから退出しました`

---

### `/hs shop`

**Purpose**: Open the shop GUI (available only during active game for hiders)

**Permission**: `hideandseek.play` (default: true)

**Syntax**: `/hs shop`

**Preconditions**:
- Player in active game
- Player role is HIDER
- Game phase is PREPARATION or SEEKING

**Success Response**:
- Opens inventory GUI with shop categories

**Error Responses**:
- Not in game: `§cゲームに参加していません`
- Wrong role: `§cハイダーのみショップを使用できます`
- Wrong phase: `§cゲーム中のみショップを使用できます`

**Side Effects**:
- GUI opened (no persistence)

**Note**: Primary shop access is via right-clicking the shop item, not this command. Command exists as fallback.

---

## Admin Commands

### `/hs admin setpos1`

**Purpose**: Set first corner of arena boundary

**Permission**: `hideandseek.admin`

**Syntax**: `/hs admin setpos1`

**Preconditions**:
- Player has admin permission
- Player location is in a valid world

**Success Response**:
```
§aPos1 を設定しました: X=100, Y=64, Z=200
```

**Error Responses**:
- No permission: `§c権限がありません`

**Side Effects**:
- Creates or updates `ArenaSetupSession` for admin
- Stores current player location as pos1

---

### `/hs admin setpos2`

**Purpose**: Set second corner of arena boundary

**Permission**: `hideandseek.admin`

**Syntax**: `/hs admin setpos2`

**Preconditions**:
- Player has admin permission
- Player location is in a valid world

**Success Response**:
```
§aPos2 を設定しました: X=200, Y=100, Z=300
§7エリアサイズ: 141ブロック (対角線距離)
```

**Error Responses**:
- No permission: `§c権限がありません`

**Side Effects**:
- Updates `ArenaSetupSession` for admin
- Stores current player location as pos2
- Calculates and shows diagonal distance

---

### `/hs admin setspawn <seeker|hider>`

**Purpose**: Set spawn point for specified role

**Permission**: `hideandseek.admin`

**Syntax**:
- `/hs admin setspawn seeker`
- `/hs admin setspawn hider`

**Preconditions**:
- Player has admin permission
- Valid role argument (seeker or hider)

**Success Response**:
```
§aSeeker spawn を設定しました: X=50, Y=64, Z=50, Yaw=0.0
```

**Error Responses**:
- No permission: `§c権限がありません`
- Invalid role: `§c使い方: /hs admin setspawn <seeker|hider>`

**Side Effects**:
- Updates `ArenaSetupSession` with spawn location and rotation

---

### `/hs admin creategame <name>`

**Purpose**: Create arena from configured positions

**Permission**: `hideandseek.admin`

**Syntax**: `/hs admin creategame <name>`

**Arguments**:
- `name`: Arena identifier (alphanumeric, hyphens, underscores)

**Preconditions**:
- All positions set (pos1, pos2, seeker spawn, hider spawn)
- All positions in same world
- Arena name not already in use

**Success Response**:
```
§aアリーナ '{name}' を作成しました！
§7- ワールド: world
§7- サイズ: 141ブロック
§7- Seeker spawn: 50, 64, 50
§7- Hider spawn: 50, 64, 80
```

**Error Responses**:
- No permission: `§c権限がありません`
- Missing positions: `§c未設定の位置があります: [pos1, hider spawn]`
- Different worlds: `§cすべての位置は同じワールドに設定してください`
- Duplicate name: `§cアリーナ '{name}' は既に存在します。別の名前を使用してください。`
- Invalid name: `§c名前に使用できるのは英数字、ハイフン、アンダースコアのみです`

**Side Effects**:
- Arena saved to `arenas.yml`
- `ArenaSetupSession` cleared for admin
- Arena loaded into `ArenaManager`

---

### `/hs admin list`

**Purpose**: List all configured arenas

**Permission**: `hideandseek.admin`

**Syntax**: `/hs admin list`

**Preconditions**:
- Player has admin permission

**Success Response**:
```
§e===[ アリーナ一覧 ]===
§a✓ main-arena §7(world) - メインアリーナ
§a✓ small-arena §7(world) - 小さいアリーナ
§7合計: 2個
```

**Error Responses**:
- No permission: `§c権限がありません`
- No arenas: `§7アリーナが設定されていません。/hs admin creategame で作成してください。`

**Side Effects**: None

---

### `/hs admin delete <name>`

**Purpose**: Delete an arena

**Permission**: `hideandseek.admin`

**Syntax**: `/hs admin delete <name>`

**Arguments**:
- `name`: Arena name to delete

**Preconditions**:
- Arena exists
- Arena not currently in use by active game

**Success Response**:
```
§aアリーナ '{name}' を削除しました
```

**Error Responses**:
- No permission: `§c権限がありません`
- Not found: `§cアリーナ '{name}' が見つかりません`
- In use: `§cアリーナ '{name}' は使用中のため削除できません。ゲームを終了してください。`

**Side Effects**:
- Arena removed from `arenas.yml`
- Arena unloaded from `ArenaManager`

---

### `/hs start <arena-name>`

**Purpose**: Start game on specified arena

**Permission**: `hideandseek.admin`

**Syntax**: `/hs start <arena-name>`

**Arguments**:
- `arena-name`: Arena to start game on

**Preconditions**:
- At least 2 players in waiting lobby
- Arena exists
- No game currently active

**Success Response**:
```
§aゲームを開始します！ ({arena-name})
§7参加者: {count}人
```

**Error Responses**:
- No permission: `§c権限がありません`
- Too few players: `§c最小2人のプレイヤーが必要です (現在: 1人)`
- Arena not found: `§cアリーナ '{name}' が見つかりません。/hs admin list で確認してください。`
- Game in progress: `§cこのアリーナは既にゲーム実行中です`
- Arena world not loaded: `§cアリーナのワールド '{world}' が読み込まれていません`

**Side Effects**:
- Game created with WAITING phase
- Roles assigned (23% seekers, 77% hiders, min 1 seeker)
- Player backups created
- Players teleported to spawns
- Inventories cleared and shop items given to hiders
- WorldBorder backup and arena boundary applied
- Phase transition to PREPARATION
- 30-second preparation countdown starts
- Broadcast: `§e===[ ゲーム開始！]===`

---

### `/hs stop <arena-name>`

**Purpose**: Force-end active game

**Permission**: `hideandseek.admin`

**Syntax**: `/hs stop <arena-name>`

**Arguments**:
- `arena-name`: Arena to stop (optional if only one arena exists)

**Preconditions**:
- Game is active on specified arena

**Success Response**:
```
§cゲームを強制終了しました
§7管理者により中断されました
```

**Error Responses**:
- No permission: `§c権限がありません`
- No active game: `§cアクティブなゲームがありません`
- Arena not found: `§cアリーナ '{name}' が見つかりません`

**Side Effects**:
- Game phase set to ENDED
- No winner declared (GameResult.CANCELLED)
- Full cleanup executed:
  - Disguise blocks removed
  - Shop items removed
  - Effects cleared
  - Scoreboard restored
  - WorldBorder restored
  - Inventory restored
  - Players teleported to original locations
- Broadcast: `§cゲームが管理者により終了されました`

---

## Command Registration (plugin.yml)

```yaml
commands:
  hideandseek:
    description: Hide and Seek main command
    aliases: [hs]
    usage: /hs <subcommand>
    permission: hideandseek.use
    permission-message: "§c権限がありません"

permissions:
  hideandseek.use:
    description: Basic access to the plugin
    default: true

  hideandseek.play:
    description: Join and play games
    default: true

  hideandseek.admin:
    description: Admin commands (setup, start, stop)
    default: op

  hideandseek.admin.setup:
    description: Arena setup commands
    default: op

  hideandseek.admin.game:
    description: Game control commands (start, stop)
    default: op
```

---

## Tab Completion

### `/hs` Tab Completion

**Context**: Player types `/hs `

**Completions**:
- If has `hideandseek.play`: `join`, `leave`, `shop`
- If has `hideandseek.admin`: `admin`

### `/hs admin` Tab Completion

**Context**: Admin types `/hs admin `

**Completions**: `setpos1`, `setpos2`, `setspawn`, `creategame`, `list`, `delete`, `start`, `stop` (filtered by sub-permissions)

### `/hs admin setspawn` Tab Completion

**Context**: Admin types `/hs admin setspawn `

**Completions**: `seeker`, `hider`

### `/hs admin creategame` Tab Completion

**Context**: Admin types `/hs admin creategame `

**Completions**: `<arena-name>` (placeholder, no suggestions)

### `/hs start` Tab Completion

**Context**: Admin types `/hs start `

**Completions**: List of configured arena names from `ArenaManager`

### `/hs admin delete` Tab Completion

**Context**: Admin types `/hs admin delete `

**Completions**: List of configured arena names (excluding active game arena)

---

## Error Handling Patterns

All commands follow this error handling pattern:

1. **Permission check**: First validation, returns permission error message
2. **Precondition checks**: Validate state (game active, player in game, etc.)
3. **Argument validation**: Check required arguments and format
4. **Business logic**: Execute command action
5. **Success feedback**: Inform player of result
6. **Broadcast (if applicable)**: Notify other players of significant events

### Standard Error Messages

```kotlin
object Messages {
    const val NO_PERMISSION = "§c権限がありません"
    const val NOT_IN_GAME = "§cゲームに参加していません"
    const val ALREADY_IN_GAME = "§cすでにゲームに参加しています"
    const val GAME_IN_PROGRESS = "§cゲームが既に進行中です"
    const val NOT_ENOUGH_PLAYERS = "§c最小2人のプレイヤーが必要です"
    const val ARENA_NOT_FOUND = "§cアリーナが見つかりません"
    const val INVALID_USAGE = "§c使い方: {usage}"
}
```

---

## Conclusion

All commands are defined with syntax, permissions, preconditions, responses, and side effects. Command contracts align with functional requirements and provide complete API specification for implementation.

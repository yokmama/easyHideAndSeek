# EasyHideAndSeek

<div align="center">

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.x-green.svg)
![Paper](https://img.shields.io/badge/Paper-1.21.x-blue.svg)
![License](https://img.shields.io/badge/License-MIT-yellow.svg)
![Language](https://img.shields.io/badge/Language-Kotlin-purple.svg)

**A feature-rich Hide and Seek minigame plugin for Paper/Spigot servers**

[Features](#features) • [Installation](#installation) • [Commands](#commands) • [Configuration](#configuration) • [Development](#development)

</div>

---

## 📖 Overview

EasyHideAndSeek is a comprehensive Hide and Seek minigame plugin that brings the classic game to your Minecraft server. Players can disguise themselves as blocks, purchase items from the shop, and compete in thrilling rounds of hide and seek with automatic role assignment, economy integration, and multilingual support.

### 🎮 Game Flow

1. **Join Phase**: Players join the game using `/hs join`
2. **Preparation Phase** (30s): Hiders find hiding spots and disguise as blocks while Seekers wait
3. **Seeking Phase** (10min): Seekers hunt down disguised Hiders
4. **Results**: Winner determined and rewards distributed

---

## ✨ Features

### 🎭 Core Gameplay
- **Block Disguise System**: Hiders can disguise as any block in the environment
- **Automatic Role Assignment**: 23% Seekers, 77% Hiders (configurable)
- **Capture Mechanics**: Seekers capture Hiders by attacking disguised blocks
- **Multiple Win Conditions**: Time-based or elimination-based victory
- **Spectator Mode**: Watch ongoing games after being captured

### 💰 Economy & Shop
- **Vault Integration**: Full economy system support
- **Role-Specific Shops**: Different items for Hiders and Seekers
- **Purchase Limits**: Balanced gameplay with per-game item limits
- **Cooldown System**: Prevents item spam

#### Hider Items
- **Escape Pass** (500 coins): Avoid capture once with 5 seconds of invincibility
- **Decoy Block** (300 coins): Place a fake disguise block to confuse Seekers
- **Shadow Sprint** (300 coins): 30 seconds of speed boost

#### Seeker Items
- **Tracking Compass** (200 coins): Points to the nearest Hider for 30 seconds
- **Speed Boost** (300 coins): 30 seconds of increased movement speed
- **Area Scan** (500 coins): Shows Hider count within 10 blocks
- **Eagle Eye** (600 coins): Apply glowing effect to nearby Hiders for 30 seconds

### 🎯 Advanced Features
- **Point System**: Earn points for captures, wins, and participation
- **Statistics Tracking**: Games played, wins, captures, win rate
- **Seeker Strength System**: Automatic balancing when Hiders have advantage
- **Block Auto-Restoration**: Automatically restores broken blocks after game
- **Random Respawn System**: Fair spawn point distribution
- **World Border Integration**: Automatic boundary management

### 🌍 Internationalization (i18n)
- **Multi-Language Support**: Japanese (ja_JP) and English (en_US) built-in
- **Per-Player Language Settings**: Each player sees messages in their preferred language
- **Auto-Detection**: Automatically detects player's Minecraft client language
- **Persistent Preferences**: Language settings saved across server restarts
- **Easy Language Switching**: Simple `/hs lang` command to change language
- **Fully Localized**: All messages, UI, commands, and shop items translated

### 📊 UI & Feedback
- **Live Scoreboard**: Real-time game stats (time, players remaining, role, points)
- **Action Bar Updates**: Non-intrusive status information
- **Title Announcements**: Important events displayed prominently
- **Sound Effects**: Audio feedback for game events
- **Particle Effects**: Visual indicators for disguise, capture, and items

---

## 📋 Requirements

### Required
- **Minecraft**: 1.21.x
- **Server Software**: Paper 1.21.x (or compatible fork)
- **Java**: 21 or higher

### Optional
- **Vault**: For economy features (highly recommended)
- **Any Economy Plugin**: (e.g., EssentialsX, CMI) for currency management

---

## 🚀 Installation

1. **Download** the latest release from [Releases](https://github.com/hacklab/EasyHideAndSeek/releases)
2. **Place** the JAR file in your server's `plugins/` folder
3. **Install Vault** (optional but recommended) for economy features
4. **Restart** your server
5. **Configure** the plugin in `plugins/HideAndSeek/config.yml`
6. **Set up** your first arena using `/hs admin setup`

---

## 🎮 Commands

### Player Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/hs` | Show help menu | `hideandseek.use` |
| `/hs join` | Join the game | `hideandseek.play` |
| `/hs leave` | Leave the game | `hideandseek.play` |
| `/hs shop` | Open the shop (in-game only) | `hideandseek.play` |
| `/hs lang [language\|list]` | Change language or list available languages | `hideandseek.lang` |
| `/hs spectator <on\|off>` | Toggle spectator mode | `hideandseek.spectate` |

### Admin Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/hs admin setup [arena]` | Setup a new arena | `hideandseek.admin` |
| `/hs admin start [arena]` | Force start a game | `hideandseek.admin` |
| `/hs admin stop [arena]` | Force stop a game | `hideandseek.admin` |
| `/hs admin reload` | Reload configuration | `hideandseek.admin` |

### Language Commands

```bash
# Show current language
/hs lang

# List available languages
/hs lang list

# Change to Japanese
/hs lang ja_JP

# Change to English
/hs lang en_US
```

---

## ⚙️ Configuration

### Main Configuration (`config.yml`)

```yaml
# Game Settings
game-settings:
  preparation-time: 30      # Preparation phase duration (seconds)
  seek-time: 600           # Seeking phase duration (seconds)
  min-players: 2           # Minimum players to start
  max-players: 30          # Maximum players per game
  seeker-ratio: 0.23       # Percentage of players assigned as Seekers

# Economy Settings
economy:
  enabled: true            # Enable/disable economy features
  winner-reward: 1000      # Coins for winning team
  participation-reward: 100 # Coins for all participants
  per-capture-reward: 200   # Coins per Hider captured (Seekers only)

# Shop Settings
shop:
  enabled: true            # Enable/disable shop
  # Item prices and limits configured in shop.yml
```

### Language Settings (`languages.yml`)

```yaml
# System Settings
settings:
  default-language: "ja_JP"              # Default language for new players
  auto-detect-client-locale: true        # Auto-detect from Minecraft client
  available-languages:
    - "ja_JP"
    - "en_US"

# Player Preferences (auto-managed)
players: {}
```

### Shop Configuration (`shop.yml`)

Items, prices, and purchase limits are fully configurable. See the generated `shop.yml` for details.

### Arena Configuration (`arenas.yml`)

Arenas can be configured manually or through admin commands. Each arena requires:
- Spawn points (Hider and Seeker spawns)
- Boundary settings (WorldBorder or coordinate-based)
- Optional custom settings per arena

---

## 🎯 Permissions

### Player Permissions

| Permission | Description | Default |
|-----------|-------------|---------|
| `hideandseek.use` | Basic plugin access | `true` |
| `hideandseek.play` | Join and play games | `true` |
| `hideandseek.lang` | Change language settings | `true` |
| `hideandseek.spectate` | Use spectator mode | `true` |

### Admin Permissions

| Permission | Description | Default |
|-----------|-------------|---------|
| `hideandseek.admin` | Full administrative access | `op` |
| `hideandseek.arena.create` | Create arenas | `op` |
| `hideandseek.arena.edit` | Edit arenas | `op` |
| `hideandseek.bypass` | Bypass game restrictions | `op` |

---

## 🛠️ Development

### Building from Source

**Requirements:**
- JDK 21+
- Gradle 8.x

```bash
# Clone the repository
git clone https://github.com/hacklab/EasyHideAndSeek.git
cd EasyHideAndSeek

# Build the plugin
./gradlew build

# Output JAR location
# build/libs/EasyHideAndSeek-1.0-SNAPSHOT.jar
```

### Project Structure

```
EasyHideAndSeek/
├── src/main/kotlin/com/hideandseek/
│   ├── arena/           # Arena management
│   ├── commands/        # Command handlers
│   ├── config/          # Configuration management
│   ├── disguise/        # Block disguise system
│   ├── effects/         # Effect management
│   ├── game/            # Core game logic
│   ├── i18n/            # Internationalization system
│   ├── items/           # Shop item handlers
│   ├── listeners/       # Event listeners
│   ├── points/          # Point system
│   ├── scoreboard/      # Scoreboard management
│   ├── shop/            # Shop system
│   ├── spectator/       # Spectator mode
│   └── utils/           # Utility classes
├── src/main/resources/
│   ├── messages_ja_JP.properties  # Japanese translations
│   ├── messages_en_US.properties  # English translations
│   ├── languages.yml              # Language configuration
│   ├── config.yml                 # Main configuration
│   ├── shop.yml                   # Shop configuration
│   └── plugin.yml                 # Plugin manifest
└── build.gradle.kts              # Build configuration
```

### Technology Stack

- **Language**: Kotlin 2.2.21
- **Server API**: Paper API 1.21.5
- **Build Tool**: Gradle 8.3.0 with Shadow plugin
- **Dependencies**:
  - Paper API 1.21.5-R0.1-SNAPSHOT
  - Vault API 1.7
  - Kotlin stdlib-jdk8

### Key Features Implementation

#### i18n System Architecture
- **UTF-8 Support**: Custom `UTF8Control` for proper Japanese character handling
- **2-Layer Caching**: ResourceBundle + MessageFormat for <1ms message lookup
- **Per-Player Locale**: Individual language preferences with persistence
- **Adventure Components**: Modern text component system with color code support

#### Performance Optimizations
- In-memory caching for frequent operations
- Asynchronous file I/O for language preference saves
- Efficient block restoration with scheduled cleanup
- Optimized scoreboard updates

---

## 📝 Contributing

Contributions are welcome! Please follow these guidelines:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### Code Style
- Follow Kotlin coding conventions
- Add KDoc comments for public APIs
- Keep methods focused and concise
- Write meaningful commit messages

---

## 🐛 Bug Reports & Feature Requests

Please use [GitHub Issues](https://github.com/hacklab/EasyHideAndSeek/issues) for:
- Bug reports
- Feature requests
- Questions about usage

When reporting bugs, please include:
- Server version (Paper/Spigot version)
- Plugin version
- Steps to reproduce
- Error logs (if applicable)

---

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Credits

**Developed by**: HackLab Team
**Original Concept**: しもん

### Special Thanks
- Paper Team for the excellent server software
- Vault developers for the economy API
- All contributors and testers

---

## 📞 Support

- **Documentation**: [Wiki](https://github.com/hacklab/EasyHideAndSeek/wiki) (Coming Soon)
- **Discord**: [Join our server](#) (Coming Soon)
- **Issues**: [GitHub Issues](https://github.com/hacklab/EasyHideAndSeek/issues)

---

<div align="center">

**Enjoy playing Hide and Seek! 🎮**

Made with ❤️ by HackLab

[⬆ Back to Top](#easyhideandseek)

</div>

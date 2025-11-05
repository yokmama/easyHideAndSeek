# 🎮 EasyHideAndSeek

**The ultimate Hide and Seek minigame plugin for Paper servers!**

Transform your Minecraft server into an exciting Hide and Seek arena where players disguise as blocks, hunt each other down, and compete for rewards. Perfect for minigame servers, events, and community fun!

---

## ✨ Why Choose EasyHideAndSeek?

### 🎭 Immersive Gameplay
- **Block Disguise System**: Hiders blend into the environment by becoming blocks
- **Smart Role Assignment**: Automatic Seeker/Hider distribution (customizable 23/77 ratio)
- **Capture Mechanics**: Seekers find and attack disguised blocks to capture Hiders
- **Time-Based Rounds**: Thrilling 10-minute games with 30-second preparation phase

### 💰 Economy Integration
- **Full Vault Support**: Integrate with any economy plugin
- **Strategic Shopping**: Purchase game-changing items during matches
- **Balanced Pricing**: Fair costs with purchase limits and cooldowns
- **Reward System**: Earn coins for wins, captures, and participation

### 🛒 Shop Items

**Hider Arsenal:**
- 🛡️ **Escape Pass** - Avoid capture with invincibility
- 📦 **Decoy Block** - Place fake disguises to confuse Seekers
- ⚡ **Shadow Sprint** - Speed boost for quick repositioning

**Seeker Tools:**
- 🧭 **Tracking Compass** - Locate the nearest Hider
- 🏃 **Speed Boost** - Chase down fleeing Hiders
- 📡 **Area Scan** - Detect nearby Hiders
- 👁️ **Eagle Eye** - Make Hiders glow through walls

### 🌍 Multilingual Support
- 🇯🇵 **Japanese (日本語)** and 🇬🇧 **English** built-in
- Per-player language preferences
- Auto-detect client language
- Easy switching with `/hs lang`
- All UI, commands, and messages fully translated

### 🎯 Advanced Features
- **Live Scoreboard**: Real-time stats and game information
- **Statistics System**: Track wins, captures, and performance
- **Spectator Mode**: Watch games after being captured
- **Auto-Balancing**: Seeker strength system for fair gameplay
- **Block Restoration**: Automatically repairs arena after games
- **Particle & Sound Effects**: Immersive audio-visual feedback

---

## 🚀 Quick Start

1. **Install** the plugin and Vault (optional but recommended)
2. **Restart** your server
3. **Join** with `/hs join`
4. **Play!** The game starts automatically

### Essential Commands

```
/hs join          - Join the game
/hs leave         - Leave the game
/hs shop          - Open the item shop
/hs lang [lang]   - Change language (ja_JP/en_US)
/hs spectator     - Toggle spectator mode
```

### Admin Commands

```
/hs admin setup   - Create a new arena
/hs admin start   - Force start a game
/hs admin reload  - Reload configuration
```

---

## ⚙️ Configuration Highlights

**Highly Customizable:**
- Game duration and preparation time
- Seeker/Hider ratios
- Economy rewards and shop prices
- Item purchase limits and cooldowns
- Arena boundaries and spawn points
- Default language and auto-detection

All settings available in `config.yml`, `shop.yml`, and `languages.yml`

---

## 📋 Requirements

- **Minecraft**: 1.21.x
- **Server**: Paper 1.21.x or compatible
- **Java**: 21+
- **Optional**: Vault + Economy Plugin

---

## 🎨 Features Overview

### Core Systems
✅ Block disguise with movement detection
✅ Automatic role assignment and team balancing
✅ Economy integration with Vault
✅ Shop system with role-specific items
✅ Point and statistics tracking
✅ Multi-arena support

### UI & Feedback
✅ Real-time scoreboard updates
✅ Action bar status messages
✅ Title announcements for events
✅ Sound and particle effects
✅ Fully localized interface

### Quality of Life
✅ Auto-join on server connect
✅ Spectator mode for captured players
✅ Block auto-restoration
✅ Random respawn system
✅ World border integration
✅ Permission-based access control

---

## 🔧 Technical Details

**Built with Modern Technology:**
- Written in **Kotlin** for performance and safety
- Uses **Adventure Components** for rich text formatting
- Implements **2-layer caching** for <1ms message lookups
- **UTF-8 native** for proper multi-language support
- **Asynchronous I/O** for smooth server performance

**Performance Optimized:**
- In-memory caching for frequent operations
- Efficient block restoration scheduling
- Minimal TPS impact during games
- Supports up to 30 concurrent players per game

---

## 🎯 Perfect For

- **Minigame Servers**: Add a popular classic game
- **Community Events**: Fun party game for your players
- **Hub Attractions**: Keep players engaged while waiting
- **Server Networks**: Multi-language support for international players
- **Educational Servers**: Safe, non-violent gameplay

---

## 📊 Statistics & Tracking

Track player performance with comprehensive stats:
- Games played
- Wins as Seeker and Hider
- Total captures
- Win rate percentage
- Total earnings

Access with commands or integrate with leaderboard plugins!

---

## 🌟 What Makes It Special?

Unlike basic Hide and Seek plugins, EasyHideAndSeek offers:

1. **True Disguise System**: Players actually become blocks, not just invisible
2. **Strategic Depth**: Shop items create dynamic gameplay and counterplay
3. **Professional Polish**: Multilingual UI, sound effects, particles, and scoreboard
4. **Active Development**: Regular updates and bug fixes
5. **Open Source**: Inspect, modify, and contribute on GitHub

---

## 🔄 Recent Updates

### Latest Version
- ✨ Full internationalization system (Japanese/English)
- 🎯 Advanced seeker strength balancing
- 🔧 Block auto-restoration system
- 📊 Comprehensive statistics tracking
- 🎮 Enhanced spectator mode
- ⚡ Performance optimizations

---

## 🐛 Support & Feedback

**Need Help?**
- 📖 [Full Documentation](https://github.com/hacklab/EasyHideAndSeek/wiki)
- 🐞 [Report Issues](https://github.com/hacklab/EasyHideAndSeek/issues)
- 💬 Community Discord (Coming Soon)

**Found a Bug?** Please report it on GitHub with:
- Server version
- Plugin version
- Steps to reproduce
- Error logs

---

## 📜 License & Credits

**License**: MIT License - Free to use and modify
**Developer**: HackLab Team
**Original Concept**: しもん

Built with ❤️ for the Minecraft community

---

## 🎬 Get Started Today!

Download now and bring the classic game of Hide and Seek to your Minecraft server!

**Questions?** Check the README or open an issue on GitHub.

**Enjoy playing! 🎮**

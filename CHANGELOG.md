# Changelog

All notable changes to EasyHideAndSeek will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Full internationalization (i18n) system with Japanese and English support
- Per-player language preferences with persistence
- Auto-detection of client language on first join
- `/hs lang` command for language switching
- Comprehensive message localization (80+ message keys)
- UTF-8 native support for Japanese characters
- 2-layer caching architecture for <1ms message lookup performance

### Changed
- Updated PlayerJoinListener to use localized welcome messages
- Improved plugin initialization order (i18n managers first)

### Technical
- Added `LanguagePreferenceManager` for player language settings
- Added `MessageManager` for localized message delivery
- Added `UTF8Control` for proper UTF-8 properties file handling
- Implemented Adventure Components integration for rich text
- Created `languages.yml` for language configuration
- Created `messages_ja_JP.properties` for Japanese translations
- Created `messages_en_US.properties` for English translations

## [1.0.0] - 2025-01-06

### Added
- Initial release
- Block disguise system for Hiders
- Automatic Seeker/Hider role assignment (23/77 ratio)
- Capture mechanics via attacking disguised blocks
- Preparation phase (30 seconds) and seeking phase (10 minutes)
- Full Vault economy integration
- Role-specific shop system with balanced items
- Hider items: Escape Pass, Decoy Block, Shadow Sprint
- Seeker items: Tracking Compass, Speed Boost, Area Scan, Eagle Eye
- Point and statistics tracking system
- Live scoreboard with real-time game stats
- Spectator mode for captured players
- Seeker strength balancing system
- Block auto-restoration after games
- Random respawn system
- World border integration
- Particle and sound effects
- Multi-arena support
- Comprehensive permission system
- Admin commands for game and arena management

### Technical Details
- Built with Kotlin 2.2.21
- Paper API 1.21.5 support
- Java 21 requirement
- Gradle 8.3.0 with Shadow plugin
- Adventure Components for modern text formatting
- Async I/O for performance
- In-memory caching for frequent operations

[Unreleased]: https://github.com/hacklab/EasyHideAndSeek/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/hacklab/EasyHideAndSeek/releases/tag/v1.0.0

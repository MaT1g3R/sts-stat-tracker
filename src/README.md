# Slay the Spire Stats Tracker Mod

This is the mod component of the Slay the Spire Stats Tracker project. It integrates with the game to collect run statistics, display them in-game, and optionally upload them to the backend server.

## Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── StatsTracker/
│   │       ├── patches/           # Game patches for hooking into Slay the Spire
│   │       ├── stats/             # Statistics data models and processing
│   │       ├── ui/                # UI components for in-game displays
│   │       ├── Config.java        # Configuration management
│   │       ├── HTTPClient.java    # Client for communicating with backend
│   │       ├── RunHistoryManager.java # Manages run history data
│   │       ├── RunUploader.java   # Handles uploading runs to backend
│   │       ├── StatsTracker.java  # Main mod class
│   │       ├── Utils.java         # Utility functions
│   │       └── YearMonth.java     # Date handling utilities
│   └── resources/
│       └── StatsTracker/
│           └── img/               # Images and icons for the mod
```

## Technology Stack

- **Language**: Java
- **Game API**: ModTheSpire and BaseMod
- **Build System**: Gradle

## Development Setup

### Prerequisites

- Java Development Kit (JDK) 8
- Gradle 8.0 or higher
- Slay the Spire game installation
- ModTheSpire and BaseMod installed

### Setting Up the Development Environment

1. Clone the repository:
   ```
   git clone https://github.com/YourUsername/sts-stat-tracker.git
   cd sts-stat-tracker
   ```

2. Copy the example build file:
   ```
   cp build.gradle.kts.example build.gradle.kts
   ```

3. Edit the `build.gradle.kts` file to point to your Slay the Spire installation:
   ```kotlin
   var steamappsLocation: String = "path/to/steamapps"
   var stsInstallLocation: String = "path/to/slay-the-spire-install-directory"
   ```

4. Build the mod:
   ```
   ./gradlew buildJAR
   ```
   
   This will create a JAR file `build/libs/StatsTracker.jar` that can be installed as a mod.

5. Build the mod and copy it to your slay the spire mods folder:
   ```
   ./gradlew buildAndCopyJAR
   ```

## Mod Architecture

### Core Components

- **StatsTracker**: The main mod class that initializes the mod and sets up hooks
- **RunHistoryManager**: Manages the collection and storage of run history data
- **Config**: Handles mod configuration and settings
- **HTTPClient**: Provides communication with the backend server
- **RunUploader**: Manages the uploading of run data to the backend

### Game Integration

The mod integrates with Slay the Spire through several patch classes:

- **MetricsPatch**: Hooks into the game's metrics system to capture run data
- **StatsScreenPatch**: Modifies the game's statistics screen to display additional stats
- **AchievementGridPatch**: Enhances the achievement grid with additional information

### Data Models

The `stats` package contains data models for representing game statistics:

- **Run**: Represents a single game run with all its statistics
- **RunData**: Raw data for a run
- **ClassStat**: Statistics specific to a character class
- **Rate**: Utility for calculating rates (e.g., win rates)
- **BossRelic**: Data about boss relic choices and outcomes

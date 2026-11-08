# Discord Integration (Minecraft Forge 1.18.1 - v3.2.0-modifiedforac)

This is a **Minecraft Forge** server-side mod for **Minecraft 1.18.1** that links Minecraft server chat, events, and commands with Discord.  
Originally created by [GeheimagentNr1](https://www.curseforge.com/minecraft/mc-mods/discord-integration) and modified by **TheAndromedaCat**.  
This is a combined work only using and not modifying used libraries.

## New Features in This Fork (`-modifiedforac`)

- **Discord Display Names & Hover Tooltips**: Displays each Discord user's server nickname or global display name in Minecraft chat instead of legacy `#0000` discriminators, with a hover tooltip showing `@username`.
- **Weather Command (`!weather` / `/weather` / `/discord weather`)**: Added a built-in command to check whether the server's overworld weather is currently `clear`, `raining`, or `thundering`.
- **Player & Server Webhooks**:
  - Automatically creates or reuses a `MinecraftChatBridge` webhook in the linked Discord chat channel (falling back gracefully to standard bot messages if the bot lacks `MANAGE_WEBHOOKS` permission).
  - Player chat, `/me`, and `/say` messages are sent via webhook using the player's name and skin head avatar (`https://mc-heads.net/avatar/<playerName>/128.png`).
  - Server start, stop, crash, console `/say` & `/me`, and tamed mob death messages are sent via webhook using a configurable server webhook name (`server_webhook_name`, default `"Minecraft Server"`) and avatar (`server_webhook_avatar_url`, defaulting to the bot's Discord avatar).
  - Player join, leave, death, and advancement messages each support a `use_player_webhook` config toggle (default `true`) to choose between the player's webhook or the server's webhook.
- **Mirrored Discord Slash Commands (`/`)**: Automatically registers all enabled Discord commands as guild slash commands (`/online`, `/tps`, `/time`, `/weather`, etc.) alongside prefix commands (`!`).

## Used libraries

The mentions of the used libraries, their licences and their copyright holders can be found in the `lib_licences` folder.  
Direct dependent libraries are mentioned here:

### Minecraft Forge

This mod uses [Minecraft Forge](https://github.com/MinecraftForge/MinecraftForge) as its mod loader under the [GNU LGPLv2.1](https://www.gnu.org/licenses/old-licenses/lgpl-2.1.en.html).

### JDA (Java Discord API)

This mod uses the [JDA (Java Discord API)](https://github.com/DV8FromTheWorld/JDA) library with the [Apache License Version 2.0](https://github.com/DV8FromTheWorld/JDA/blob/master/LICENSE) licence.

#### Using modified version of JDA (Java Discord API)

The library is provided as part of the mod's jar-file.  
To customize the library version follow the following steps:
1. Clone this repository
2. Change the library version by changing the value of the `jda_version` property in the `gradle.properties` file
3. Build the mod jar (`./gradlew build` or `./gradlew shadowJar`)
4. Your custom mod version can be found in the `build/libs` folder. Copy it to the `mods` folder of your server.

### Simple Logging Facade for Java (SLF4J)

This mod uses the [Simple Logging Facade for Java (SLF4J)](http://www.slf4j.org/) library with the [MIT](http://www.slf4j.org/license.html) licence.

#### Using modified version of Simple Logging Facade for Java (SLF4J)

The library is provided as part of the mod's jar-file.  
To customize the library version follow the following steps:
1. Clone this repository
2. Change the library version by changing the value of the `slf4j_version` property in the `gradle.properties` file
3. Build the mod jar (`./gradlew build` or `./gradlew shadowJar`)
4. Your custom mod version can be found in the `build/libs` folder. Copy it to the `mods` folder of your server.

## Branches

| Branch | Description |
| ------------- | ------------- |
| `master` | Default branch - holds the general README and library licences |
| `develop_X.X.X` | Development branch for Minecraft version `X.X.X` |
| `master_X.X.X` | Release branch for Minecraft version `X.X.X` |
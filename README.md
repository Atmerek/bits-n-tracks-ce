# Create: Bits 'n' Tracks Community Edition

[![Available on Modrinth](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3.3.1/assets/cozy/available/modrinth_vector.svg)](https://modrinth.com/mod/bits-n-tracks-ce)
[![Available on CurseForge](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3.3.1/assets/cozy/available/curseforge_vector.svg)](https://www.curseforge.com/minecraft/mc-mods/bits-n-tracks-ce)
[![Source on GitHub](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3.3.1/assets/cozy/available/github_vector.svg)](https://github.com/Atmerek/bits-n-tracks-ce)
[![Supports NeoForge](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3.3.1/assets/cozy/supported/neoforge_vector.svg)](https://neoforged.net)
[![Requires Create](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3.3.1/assets/cozy/requires/create_vector.svg)](https://modrinth.com/mod/create)
[![Join the Discord](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3.3.1/assets/cozy/social/discord-plural_vector.svg)](https://discord.gg/CxTmpvp6gs)

![A tank on bogie suspension driving over slabs](https://raw.githubusercontent.com/Atmerek/bits-n-tracks-ce/main/docs/readme/01_hero.gif)

Build working tank tracks in Create.
Flanged cogwheels carry a belt or tread around them, and on a Sable vehicle the track grips the ground, drives it along and rides over what it crosses.

This is a community continuation of [**Bits 'n' Tracks**](https://modrinth.com/mod/create-bits-n-tracks) by *qwxon*, picking up development where the original left off.

## Requirements

* Minecraft 1.21.1 with NeoForge 21.1.228 or newer
* [Create](https://modrinth.com/mod/create) 6.0.10 or newer
* [Sable](https://modrinth.com/mod/sable) 2.0
* [Create: Bits 'n' Bobs](https://modrinth.com/mod/create-bits-n-bobs) 2.3.5 or newer, with its own dependencies
* [Create Aeronautics](https://modrinth.com/mod/create-aeronautics) 1.3.1 or newer, for assembling vehicles and for the spring in the Suspension Piece recipe

## What's inside

![Everything the mod adds](https://raw.githubusercontent.com/Atmerek/bits-n-tracks-ce/main/docs/readme/02_lineup.png)

* **Flanged cogwheels** in six sizes, plain and industrial, that carry a track
* **Three tracks**: Create's own belt, the Industrial Belt and the Tank Tread, plus grousers for the belts
* **Suspension Pieces** that mount swinging arms and bogies
* **Two tools**, the Cog Alignment Lever to shape a track and the Suspension Tool to tune its ride

## Flanged cogwheels

![The six cogwheel sizes, plain and industrial](https://raw.githubusercontent.com/Atmerek/bits-n-tracks-ce/main/docs/readme/03_cogwheels.png)

Six sizes: tiny, intermediate, small, medium, medium-large and large, each in a plain and an industrial style.
The industrial models are based on the Industrial Cogwheels from Create: Gears n' Kinetics.

A placed cogwheel drives its track and turns through its axle.
Switch physics on with the Cog Alignment Lever and it becomes a wheel of its Sable vehicle: it stands on the ground, blocks players and other entities, and can carry a Suspension Piece.

## Tracks

![Create's belt, Industrial Belt, Industrial Belt with grousers, Tank Tread](https://raw.githubusercontent.com/Atmerek/bits-n-tracks-ce/main/docs/readme/04_tracks.png)

Every track is strung between cogwheels like a Bits 'n' Bobs chain.

* **Create's belt**: a belt connector makes a track right away, no extra crafting needed.
* **Industrial Belt**: the mod's own belt, made from belt connectors and iron nuggets.
* **Tank Tread**: a plated track made in a Mechanical Crafter, which bites into soft ground better and grips a little less on hard ground.
* **Grousers**: right-click either belt with them to fit them along it. A belt with grousers grips much better on ice, snow and mud and a little worse on hard ground.

A track grips by the weight resting on it, and less on dirt, sand, gravel, clay, snow and mud than on stone.
Datapacks can change the grip of any block.
The run of track between cogwheels drapes over terrain and carries the vehicle over obstacles.
A redstone brake on any of a track's cogwheels brakes the whole track.

## Wide wheels

![Narrowed, full-width and two-wide tracks](https://raw.githubusercontent.com/Atmerek/bits-n-tracks-ce/main/docs/readme/05_widths.png)

Place a cogwheel beside another of the same size and style along its axle and they join into one wide wheel, with the track widening to match.
Sneak while placing to keep them apart.
The lever's Width mode narrows a cogwheel a quarter at a time, and a track is always as wide as its widest cogwheel.

## Routing

![Automatic routing and Route mode](https://raw.githubusercontent.com/Atmerek/bits-n-tracks-ce/main/docs/readme/06_routing.png)

The track wraps around its cogwheels the way a real track sits on its road wheels.
A cogwheel inside the loop that the track cannot reach is enclosed by it, and turns freely until the track touches it.

With the lever's Route mode, bring the far run of track around any cogwheel instead.
That cogwheel then turns the other way, like a belt crossed over a pulley.

## Suspension

![Arm, bogie and wide bogie](https://raw.githubusercontent.com/Atmerek/bits-n-tracks-ce/main/docs/readme/07_suspension.png)

A **Suspension Piece** gives a physics cogwheel a sprung mount.

* **Arm**: right-click a physics cogwheel of any size to select it, then look beside it and right-click where the green preview fits. The arm pivots on the cogwheel's top corner, which needs a block touching it along the axle. If blocks on both sides could hold it, it mounts on the side you are looking toward. Uses one piece.
* **Bogie**: select a small, intermediate or tiny physics cogwheel, then right-click the one of the same size next to it. The bogie hangs from a block right above either cogwheel, or one next to that spot along the axle, and blocks can sit on top of it. Uses two pieces.
* **Wide bogie**: the same between two two-wide small, intermediate or tiny cogwheels, each pair turning into two separate wheels with the bogie between them. Uses two pieces.

![Bogies rocking over a slab](https://raw.githubusercontent.com/Atmerek/bits-n-tracks-ce/main/docs/readme/08_bogie.gif)

Shift + right-click the cogwheel with an empty hand to take a piece or bogie back off.
A mount whose supporting block is removed drops its pieces.

## Getting started

Every part of the mod has a Ponder scene.
Hold the Ponder key (W by default) over a flanged cogwheel, a track, the Cog Alignment Lever, a Suspension Piece or the Suspension Tool.
The scenes walk through building a tracked vehicle, from placing and powering the cogwheels to tuning its suspension.

## The Cog Alignment Lever

![The Cog Alignment Lever nudging a cogwheel](https://raw.githubusercontent.com/Atmerek/bits-n-tracks-ce/main/docs/readme/09_lever.png)

The main tool of the mod.
It adjusts cogwheels after they are placed, narrows or hides them, routes and tensions the track, and switches physics on or off.
It only acts on Bits 'n' Tracks cogwheels and the tracks running on them, and breaks other blocks as normal.
While you hold it, a panel beside the hotbar shows the current mode, the values of what you are working on, and what to click or scroll.
Hold Alt (Create's tool menu key) and scroll to switch modes.

* **Offset**: right-click a cogwheel to select it, then scroll to move it one pixel at a time. Shift + scroll picks the axis: along the axle, across, or up and down.
* **Route**: right-click a face to bring the far run of track around the cogwheel, so clicking the top wraps the bottom track over it, and reverses its spin. The centre puts its routing back to automatic.
* **Tension**: right-click a track to select it, then scroll to tighten it, so it holds its line, or slacken it, so it sags and follows the ground.
* **Visibility**: right-click a cogwheel to show or hide it.
* **Physics**: right-click a cogwheel to switch its physics on or off.
* **Width**: right-click a cogwheel to narrow it a quarter at a time, and Shift + right-click to widen it. A track is as wide as its widest cogwheel.
* **Shaft**: right-click either end of a cogwheel to hide or show its shaft there. A hidden shaft connects to nothing, so a cogwheel without shafts only turns with its track.
* **Reset**: right-click a cogwheel twice to reset alignment, visibility, track routing and suspension tuning.

Ctrl + left-click switches Offset, Width, Shaft and Reset between one cogwheel and the whole track. On the whole track, Offset only moves the cogwheels that share the clicked one's physics setting.

## The Suspension Tool

![The Suspension Tool beside a cogwheel on an arm](https://raw.githubusercontent.com/Atmerek/bits-n-tracks-ce/main/docs/readme/10_tool.png)

The Suspension Tool reads and tunes cogwheels that carry a Suspension Piece.
It has four settings, stiffness, damping, travel and spring strength, each on levels 1 to 15 like a redstone signal, with 8 as the default.
It uses the same panel as the lever, with one mode per setting.

* **Look at a suspended cogwheel**: the panel and the dial show its levels.
* **Hold Alt and scroll**: switches setting.
* **Right-click a suspended cogwheel**: selects it.
* **Scroll**: raises or lowers the selected setting on the selected cogwheel by one level.
* **Ctrl + left-click**: switches between tuning one cogwheel and every suspended cogwheel on the track.

## Configuration

Physics settings are per world: suspension, cogwheel geometry, drive, friction, the belt, stress and sounds.
Open a world and use the Config button in the mod list to edit them, and drop a `bits_n_tracks-server.toml` into `defaultconfigs/` to give every new world the same values.
Client settings cover Flywheel rendering, how often tracks recompute their shape, and the distance past which they stop animating.

Shader packs just work.
[Colorwheel](https://modrinth.com/mod/colorwheel) is recommended alongside Iris, since it keeps Flywheel rendering on with shaders.

## Help and feedback

Report bugs on the [issue tracker](https://github.com/Atmerek/bits-n-tracks-ce/issues), and ask questions or share builds on [Discord](https://discord.gg/CxTmpvp6gs).
Development builds, when one is out ahead of the latest release, are listed as pre-releases on the [releases](https://github.com/Atmerek/bits-n-tracks-ce/releases) page.

## Building from source

Gradle needs JDK 21.

```bash
./gradlew build           # jar lands in build/libs/
./gradlew runClient       # launch a dev client
```

## License and credits

**GPL-3.0-only.** See [LICENSE](https://github.com/Atmerek/bits-n-tracks-ce/blob/main/LICENSE).

Original mod and all original code by **qwxon**.
Community Edition by **Atmerek** and **Alesrr**, with contributions from **Cubester**, the Russian translation by **Hanahaki** and art by **CloverKeep**.

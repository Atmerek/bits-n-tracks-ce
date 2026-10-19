# Create: Bits 'n' Tracks Community Edition

![Minecraft 1.21.1](https://img.shields.io/badge/Minecraft-1.21.1-62B47A?style=for-the-badge)
![Loader NeoForge](https://img.shields.io/badge/Loader-NeoForge-F16436?style=for-the-badge)
[![Latest release](https://img.shields.io/github/v/release/Atmerek/bits-n-tracks-ce?label=release&style=for-the-badge)](https://github.com/Atmerek/bits-n-tracks-ce/releases/latest)
[![Dev build](https://img.shields.io/github/actions/workflow/status/Atmerek/bits-n-tracks-ce/dev.yml?branch=main&label=dev%20build&style=for-the-badge)](https://github.com/Atmerek/bits-n-tracks-ce/releases/tag/dev)
[![License GPL-3.0](https://img.shields.io/github/license/Atmerek/bits-n-tracks-ce?style=for-the-badge)](LICENSE)
[![Discord](https://img.shields.io/discord/1553509655064354848?style=for-the-badge)](https://discord.gg/CxTmpvp6gs)

Build working tank tracks in Create.
Flanged cogwheels carry a belt or tread around them, and on a Sable vehicle the track grips the ground, drives it along and rides over what it crosses.

This is a community continuation of [**Bits 'n' Tracks**](https://modrinth.com/mod/create-bits-n-tracks) by *qwxon*, picking up development where the original left off.

## Requirements

* Minecraft 1.21.1 with NeoForge 21.1.228 or newer
* [Create](https://modrinth.com/mod/create) 6.0.10 or newer
* [Sable](https://modrinth.com/mod/sable) 2.0
* [Create: Bits 'n' Bobs](https://modrinth.com/mod/create-bits-n-bobs) 2.3.5 or newer, with its own dependencies
* [Create Aeronautics](https://modrinth.com/mod/create-aeronautics) 1.3.1 or newer, for assembling vehicles and for the spring in the Suspension Piece recipe

## Features

* **Flanged cogwheels** in tiny, intermediate, small, medium, medium-large and large, each in a plain and an industrial style, the industrial models based on the Industrial Cogwheels from Create: Gears n' Kinetics.
* **Two kinds of track**: the Industrial Belt and the plated Tank Tread, strung between cogwheels like a Bits 'n' Bobs chain.
* **Two-wide wheels**: place a cogwheel beside another of the same size and style along its axle and they join into one wide wheel, with the track widening to match. Sneak while placing to keep them apart.
* **Taut track routing**: the track wraps around its cogwheels the way a real track sits on its road wheels. A cogwheel inside the loop that the track cannot reach is enclosed by it, and turns freely until the track touches it.
* **Steerable routing**: bring the far run of track around any cogwheel, which then turns the other way, like a belt crossed over a pulley.
* **A track that meets the ground**: the run between cogwheels drapes over terrain, grips and drives along its length, and carries the vehicle over obstacles. Its tension can be set per track.
* **Suspension**: Suspension Pieces mount swinging arms, bogies and wide bogies on physics cogwheels, and the Suspension Tool tunes each one.
* **Configurable** physics, track and rendering settings.

## Learning the mod

Every part of the mod has a Ponder scene: hold the Ponder key (W by default) over a flanged cogwheel, a track, the Cog Alignment Lever, a Suspension Piece or the Suspension Tool.
The scenes walk through building a tracked vehicle, from placing and powering the cogwheels to tuning its suspension.

## The Cog Alignment Lever

The main tool of the mod. It adjusts cogwheels after they are placed, hides them, routes and tensions the track, and switches physics on or off.
It only acts on Bits 'n' Tracks cogwheels and the tracks running on them, and breaks other blocks as normal.
While you hold it, a panel beside the hotbar shows the current mode, the values of what you are working on, and what to click or scroll.
Hold Alt (Create's tool menu key) and scroll to switch modes.

| Mode | |
|---|---|
| Offset | Right-click a cogwheel to select it, then scroll to move it one pixel at a time. Shift + scroll picks the axis: along the axle, across, or up and down |
| Route | Right-click a face to bring the far run of track around the cogwheel, so clicking the top wraps the bottom track over it, and reverses its spin. The centre puts its routing back to automatic |
| Tension | Right-click a track to select it, then scroll to tighten it, so it holds its line, or slacken it, so it sags and follows the ground |
| Visibility | Right-click a cogwheel to show or hide it |
| Physics | Right-click a cogwheel to switch its physics on or off |
| Reset | Right-click a cogwheel twice to reset alignment, visibility, track routing and suspension tuning on the whole track |

Ctrl + left-click switches Offset between moving one cogwheel and moving every cogwheel on the track that shares its physics setting.

## Suspension

A **Suspension Piece** gives a physics cogwheel a sprung mount.

* **Arm**: right-click a physics cogwheel of any size to select it, then look beside it and right-click where the green preview fits. The arm pivots on the cogwheel's top corner, which needs a block touching it along the axle. Uses one piece.
* **Bogie**: select a small, intermediate or tiny physics cogwheel, then right-click the one of the same size next to it. The bogie hangs from a block right above either cogwheel, or one next to that spot along the axle, and blocks can sit on top of it. Uses two pieces.
* **Wide bogie**: the same between two two-wide small, intermediate or tiny cogwheels, each pair turning into two separate wheels with the bogie between them. Uses two pieces.

Shift + right-click the cogwheel with an empty hand to take a piece or bogie back off.
A mount whose supporting block is removed drops its pieces.

The **Suspension Tool** reads and tunes cogwheels that carry a Suspension Piece.
It has four settings, stiffness, damping, travel and spring strength, each on levels 1 to 11 with 4 as the default.

It uses the same panel as the lever, with one mode per setting.

| | |
|---|---|
| Look at a suspended cogwheel | The panel and the dial show its levels |
| Hold Alt and scroll | Switches setting |
| Right-click a suspended cogwheel | Selects it |
| Scroll | Raises or lowers the selected setting on the selected cogwheel by one level |
| Ctrl + left-click | Switches between tuning one cogwheel and every suspended cogwheel on the track |

## Configuration

Physics settings are per world: suspension, cogwheel geometry, drive, friction, the belt, stress and sounds.
Open a world and use the Config button in the mod list to edit them, and drop a `bits_n_tracks-server.toml` into `defaultconfigs/` to give every new world the same values.
Client settings cover Flywheel rendering, how often tracks recompute their shape, and the distance past which they stop animating.

Shader packs work through Iris, with or without [Colorwheel](https://modrinth.com/mod/colorwheel) keeping Flywheel on.

## Building

Gradle needs JDK 21.

```bash
./gradlew build           # jar lands in build/libs/
./gradlew runClient       # launch a dev client
```

## License

**GPL-3.0-only.** See [LICENSE](LICENSE).

Original mod and all original code by **qwxon**.
Community Edition by **Atmerek** and **Alesrr**, with contributions from **Cubester** and art by **CloverKeep**.

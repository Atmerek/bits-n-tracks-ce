# Create: Bits 'n' Tracks Community Edition

Build working tracks in Create, using the belt and chain system from Bits 'n' Bobs.

Flanged cogwheels come in several sizes and can be aligned, hidden and tuned in place.

This is a community continuation of [**Bits 'n' Tracks**](https://modrinth.com/mod/create-bits-n-tracks) by *qwxon*, picking up development where the original left off.

## Features

* **Track-focused cogwheels** in several sizes.
* **Industrial cogwheel variants**, with models based on the Industrial Cogwheels from Create: Gears n' Kinetics.
* **Hidden cogwheels** for cleaner builds.
* **Two-wide track wheels**, made by placing two cogwheels of the same size side by side along their axis, with the track stretching to match; sneak while placing to keep them apart.
* **Taut track routing**, which wraps the track around the cogwheels the way a real one sits on its road wheels; a cogwheel too far inside the loop for the track to reach is enclosed by it instead of pulling it out of shape, and it neither drives the track nor is driven by it until it reaches it.
* **Steerable track routing**, bringing whichever run of track you name all the way around a cogwheel; a cogwheel the track runs over instead of under turns the other way, the same as a real belt crossed over a pulley.
* **Cog Alignment Lever** for shifting, hiding, resetting, rerouting and tuning cogwheels.
* **Suspension-style physics** for tracked vehicles.
* **Configurable** physics and movement settings.

## The Cog Alignment Lever

The main tool of the mod. It adjusts cogwheel placement after the block is already down, hides cogwheels you would rather not see, resets a cogwheel to its default state, sends the track around the other side of a cogwheel, and switches suspension physics on or off per cogwheel.

| | |
|---|---|
| Right-click a flat face, off center | Nudges that cogwheel one pixel toward where you clicked |
| Right-click a flat face, in the center | Shows or hides that cogwheel |
| Right-click a side | Shifts the whole chain one pixel along the axis |
| Shift + right-click | Resets alignment, visibility and track routing for the whole chain |
| Left-click | Toggles suspension physics for that cogwheel |
| Shift + left-click a face, off center | Brings the far run of track around the cogwheel and reverses it, so clicking the top wraps the bottom track over it; the side is picked the same way the green highlight box picks it |
| Shift + left-click a face, in the center | Puts that cogwheel's track routing back to automatic |

## Configuration

Everything the physics uses can be changed from the Config button in the mod list: suspension, geometry, drive, friction, stress and sounds, plus client rendering options. Physics settings are per world, so open a world to edit them, and drop a `bits_n_tracks-server.toml` into `defaultconfigs/` to give every new world the same values.

## Building

```bash
./gradlew build           # jar lands in build/libs/
./gradlew runClient       # launch a dev client
```

## License

**GPL-3.0-only.** See [LICENSE](LICENSE).

Original mod and all original code by **qwxon**.

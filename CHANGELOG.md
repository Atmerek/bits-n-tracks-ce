# Changelog

## 3.0.0

Requires NeoForge 21.1.228 or newer, Create Aeronautics 1.3.1 or newer and Bits 'n' Bobs 2.3.5 or newer.

### Upgrading from 2.x

- Physics cogwheels no longer have suspension of their own. Without a Suspension Piece a physics cogwheel is a rigid wheel, so existing vehicles ride without suspension until pieces are mounted. A piece needs a block touching the cogwheel along its axle to hang from.
- The suspension settings listed under Removed are gone, and any values set in them are ignored. Suspension is now tuned per cogwheel with the Suspension Tool.
- The Cog Alignment Lever now only works on Bits 'n' Tracks cogwheels.
- Cogwheels with physics enabled are converted when a 2.x world loads. A Bits 'n' Bobs or Design n' Decor cogwheel with physics enabled becomes the Bits 'n' Tracks cogwheel of the same size.
- Bits 'n' Tracks cogwheels no longer turn the blocks beside them, or get turned by them. Drive them through their axle or their track.
- The Cog Alignment Lever now works in modes, switched by holding Alt and scrolling. What replaced its old click controls is listed under Changed.

### Added

- Two-wide track wheels. A cogwheel placed beside another of the same size and material along its axle joins it into one wide wheel, and the track widens to match. Sneak while placing to keep them apart.
- The track now wraps around its cogwheels the way a real track sits on its road wheels. A cogwheel inside the loop that the track cannot reach is enclosed by it instead of pulling it out of shape, and turns freely until the track touches it.
- Track routing. In the Cog Alignment Lever's Route mode, right-clicking a cogwheel face brings the far run of track around that cogwheel and reverses its spin, and right-clicking the centre puts it back to automatic.
- The run of track between cogwheels now meets the ground. It drapes over terrain, carries the vehicle over what it crosses, and grips and drives along its length.
- Track tension. In the Cog Alignment Lever's Tension mode, right-click a track to select it and scroll to tighten or slacken it.
- Tank Tread, a plated track made in a Mechanical Crafter and strung between cogwheels like the Industrial Belt.
- Suspension Piece, which mounts a swinging arm on a physics cogwheel, or a bogie across two small or tiny cogwheels, two-wide ones included. It is crafted with the spring from Create Aeronautics.
- Suspension Tool, which reads and tunes a suspended cogwheel's stiffness, damping, travel and spring strength over 11 levels, for one cogwheel or a whole track at once.
- Ctrl + left-click with the Cog Alignment Lever switches its Offset mode between moving one cogwheel and moving every cogwheel on the track that shares its physics setting.
- A panel for the Cog Alignment Lever and the Suspension Tool, shown while holding either. It shows the current mode, the values of the cogwheel or track being worked on, and what to click or scroll.
- Sounds for the Cog Alignment Lever and the Suspension Tool when switching modes, adjusting values, toggling settings and when an action is refused.
- Ponder scenes for the flanged cogwheels, tracks, the Cog Alignment Lever, the Suspension Piece and the Suspension Tool.
- Decorative heads of the mod's creators and contributors.
- Cogwheels with physics enabled now block players and other entities.
- Settings: a Belt section for how the track meets the ground and how its length and tension behave, Suspension Smoothing, Debug Logging, and client settings for Flywheel rendering, track update rate and animation distance.

### Changed

- Cogwheels and tracks are drawn through Flywheel when it is active, and tracks stay visible out to the render distance instead of 64 blocks.
- Bits 'n' Bobs chains on Bits 'n' Bobs cogwheels are left entirely to Bits 'n' Bobs, so they route, look and drive the same as without Bits 'n' Tracks.
- The Cog Alignment Lever works in six modes: Offset, Route, Tension, Visibility, Physics and Reset. In Offset mode, right-click a cogwheel to select it, scroll to move it one pixel at a time and Shift + scroll to pick the axis, instead of clicking parts of its face. Visibility and physics are switched by right-clicking in their own modes, instead of right-clicking the centre and left-clicking.
- Resetting with the Cog Alignment Lever is done by right-clicking a cogwheel twice in Reset mode, and also resets suspension tuning.
- The Cog Alignment Lever now breaks blocks other than Bits 'n' Tracks cogwheels normally.
- The Industrial Belt recipe now takes two belt connectors and three iron nuggets, and makes two belts.
- Tracks and Suspension Pieces add to a vehicle's mass: 0.25 for an Industrial Belt and 0.5 for a Tank Tread whatever their length, and 0.25 for each Suspension Piece.

### Removed

- Design n' Decor cogwheels can no longer be used in tracks.
- Settings: Cogwheel Suspension, Track Suspension, Base Strength, Spring Scale, Damping Scale, Cogwheel Spring, Track Spring, Cogwheel Damping, Track Damping, Cogwheel Impulse Limit, and the four Suspension Rest settings.

### Fixed

- The track cut through cogwheels, drove cogwheels it never reached, and could drop out of the kinetic network between sessions.
- Turning physics on or off for a cogwheel reset its alignment and visibility, and turned an industrial cogwheel wooden.
- Create schematics turned industrial cogwheels with physics enabled wooden, and dropped every cogwheel's alignment and visibility.
- Industrial cogwheels with physics enabled sounded like wooden ones.
- Breaking a physics cogwheel by anything other than hand dropped a plain wooden cogwheel instead of the one that was placed, and breaking it with an axe or pickaxe dropped both.
- Tracks looked wet and glossy with shader packs, and were lit as if they faced the sky.
- A wheel pressed against a wall lost its grip as if it were in the air.
- A vehicle on a slope could drift sideways.
- A wheel touching several blocks could take its grip from the wrong one.
- The Cog Alignment Lever could not reach a cogwheel drawn away from its block, such as one hanging on its suspension.
- Bits 'n' Bobs chains and ropes could be placed on a cogwheel with physics enabled.
- Turning off Bits 'n' Bobs' undedicated chain drive setting broke every track on Bits 'n' Tracks cogwheels.
- Removing a cogwheel from a chain could crash the game.
- The Cog Alignment Lever was drawn at the wrong size in item frames.
- The Cog Alignment Lever stacked up to 64.
- A cogwheel with physics enabled added no mass to its vehicle.

## 2.1.0

### Added

- A mod icon, shown in the mod list.
- Two settings. Suspension Speed Limit, under suspension, caps how hard a cogwheel can push off a surface, and Pivot Scrub, under friction, sets how freely the tracks let a vehicle turn.

### Changed

- Tracked vehicles now turn at the rate their two track speeds call for, so running one track forward and the other back turns the vehicle on the spot. Existing vehicles will steer more sharply than before.
- Suspension is stronger by default and vehicles sit higher on it. Anyone who raised the suspension strength setting to compensate can lower it again.

### Fixed

- Vehicles could not turn on the spot. The tracks slid sideways and the vehicle barely rotated.
- Breaking a vehicle into two parts could fling away the part that had a cogwheel on it.
- A cogwheel buried in terrain could launch the vehicle it was on.
- Enabling suspension on a cogwheel outside a vehicle dropped it by the full suspension travel.
- A suspension cogwheel's outline and hitbox stayed on the block grid while the cogwheel hung below it.
- Chain and belt selection, and the alignment lever's face selector, went by a cogwheel's grid position rather than where it was drawn.
- Tiny cogwheels used the small cogwheel's hitbox, and medium cogwheels used the large cogwheel's.
- Placing a vanilla chain or an industrial belt on a cogwheel that cannot take one showed a raw translation key instead of a message.

## 2.0.0

### Added

- A settings screen, reachable from the Config button in the mod list, covering suspension, geometry, drive, friction, stress and sound values. Previously every one of them was fixed and could not be changed.
- Suspension can be switched on or off separately for loose cogwheels and for cogwheels that are part of a track.

### Changed

- Renamed to Create: Bits 'n' Tracks Community Edition, shown in the creative menu as Bits 'n' Tracks CE. Existing worlds are unaffected, since block, item and recipe IDs are unchanged.
- A vehicle now travels at the speed its track links are moving, so its speed follows the size of its cogwheels and how fast they turn. Existing vehicles may run at a different speed than before.

### Fixed

- Tracks slid across the ground instead of rolling along it, and mixing cogwheel sizes in one track made it worse.
- Every suspension cogwheel added to a track raised the vehicle's top speed.
- Heavier vehicles had a lower top speed.
- Cogwheels hovering just above the ground still pushed the vehicle along.
- A vehicle whose tracks were stopped slid downhill instead of holding its position.
- The game crashed when moving out of range of a vehicle running on tracks.
- Cogwheels added by other mods could crash the game once suspension physics reached them.
- Vehicles were flung into the air the moment their tracks touched the ground, and kept bouncing instead of settling after a drop.
- Physics stopped being applied to some cogwheels after another one was broken.
- Cogwheel chains did not follow a contraption when it was moved or rotated.
- Cogwheels sometimes rendered with the wrong chain texture.
- Blocks from other mods could be mistaken for cogwheels and given the wrong track collision size.
- Chain integrity warnings filled the log.

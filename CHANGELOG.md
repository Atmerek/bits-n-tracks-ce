# Changelog

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

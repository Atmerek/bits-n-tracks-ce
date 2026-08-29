## Summary

<!-- What this changes, and why. If it closes an open issue, write "Fixes #123" here so the issue closes on merge. -->

## What a player will notice

<!-- The difference in game. Write "nothing" for refactors, build changes and documentation. -->

## How it was tested

<!-- The setup you ran it on: cogwheel sizes, chain layout, which cogwheels had physics enabled, single player or a dedicated server. Say plainly if the change was only compiled and never run. -->

## Checklist

- [ ] `./gradlew build` passes, with no deprecation or removal warnings that were not already there.
- [ ] I loaded the change in a real game session, not only compiled it.
- [ ] Any mixin I added is listed in `bits_n_tracks.mixins.json`, or I added none.
- [ ] Any config entry I added has a matching `bits_n_tracks.configuration.<entryName>` key in `en_us.json`, or I added none.
- [ ] The diff contains only what the summary describes, with no unrelated renames.

<!-- Please leave CHANGELOG.md and mod_version in gradle.properties alone. Release notes and version bumps are the maintainer's, contributed changes included. -->


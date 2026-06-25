# Orbital Strike

A Fabric mod for Minecraft that adds the Orbital Strike Cannon as seen on the Unstable SMP.
Developed by Relmes Studios.

License: Apache-2.0


## Overview

Orbital Strike provides two weapons delivered via custom-tagged fishing rods:

- **Nuke** — a wide-area bombardment. 661 TNT arranged in 10 concentric rings plus a
  center charge fall from above and detonate simultaneously, producing a large circular crater.
- **Stab** — a focused vertical strike. A column of TNT is spawned downward from 5 blocks
  above the targeted block, drilling through terrain until it hits a blast-resistant material.

Both weapons are triggered by right-clicking with an Orbital Strike fishing rod. The rod
breaks immediately on cast and the strike spawns exactly 0.5 seconds later.


## Requirements

- Minecraft 26.1.2
- Fabric Loader 0.19.3
- Fabric API
- Java 25


## Installation

1. Install Fabric Loader for your Minecraft version.
2. Install the Fabric API mod into your `mods/` folder.
3. Place `orbitalstrike.jar` in your server or client `mods/` folder.
4. Start the server or client. No configuration files are required.


## Commands

All commands require operator permission level 2 or higher.

    /osc give nuke [player]
        Gives the target player (or self if omitted) a Nuke rod.

    /osc give stab [player]
        Gives the target player (or self if omitted) a Stab rod.

    /osc strike nuke <x> <y> <z>
        Triggers a nuke strike at the given coordinates directly, without a rod.

    /osc strike stab <x> <y> <z>
        Triggers a stab strike at the given coordinates directly, without a rod.


## Weapon Details

### Nuke

The nuke spawns 661 TNT in a ring pattern centered on the targeted block:

- 1 center charge, stationary
- 10 rings at radii 6, 11, 16, 21, 26, 31, 36, 41, 46, and 51 blocks
- Each ring contains 15 to 119 evenly spaced TNT
- Every TNT is given an initial horizontal velocity calculated so it lands at its
  target radius when the fuse expires (accounting for drag at 0.98 per tick)
- A random misalignment of up to 1.75 blocks (half of 3.5) is applied per TNT,
  constrained so each charge stays within its declared ring band
- All charges spawn 72 blocks above the target and fall with no vertical velocity

Block drops are reduced to 25% of normal when destroyed by a nuke or stab, to reduce
server load from large item quantities.

### Stab

The stab spawns a vertical column of TNT starting 5 blocks above the targeted block and
stepping downward every 4 blocks. At each step, 5 TNT are placed with slight vertical
offsets so their blast radii overlap. The column stops when it reaches a block with
blast resistance at or above 1200 (obsidian, ancient debris, reinforced deepslate).

Each stab TNT has a fuse of 1 tick and does not experience gravity, so the entire column
detonates near-simultaneously.


## Technical Notes

### Explosion Optimization

Explosions caused by Orbital Strike TNT use an optimized raycast implementation that
replaces the vanilla block-destruction calculation:

- Ray directions are precomputed once at class load (identical to vanilla's 16x16x16
  surface cube, so crater shapes are the same)
- All ~1400 rays per explosion are traced in parallel using Java's ForkJoinPool
- A shared ConcurrentHashMap caches block state reads so each block position is read
  from the world at most once, regardless of how many rays cross it

This reduces world reads from roughly 5000-8000 per TNT explosion to 300-500, and
distributes the computation across available CPU cores.



## Authors

- Relmes Studios — https://github.com/RelmesStudios

# GPU battle view

The Java client has a libGDX/LWJGL3 board with one 3D scene, an orthographic orbit
camera, animated unit meeples, and contextual Scene2D controls. From deployment
onward, choose **View > GPU Battle View (Experimental)**. Launch this checkout with
`.\gradlew.bat :megamek:run` on Windows or `./gradlew :megamek:run` elsewhere.

The classic window hides after the first GPU frame. **View > Classic Board**, or
closing the GPU window, disposes the renderer and restores the existing client.
The game, pending orders, and phase controls remain the same. Startup failure
leaves the classic interface available; closing the game does not reopen it.

## Terrain and assets

Hexes tile edge to edge, with exposed walls where their surfaces stand above
neighbors. `BoardSurface` supplies the same triangles to rendering and picking.
Roads reaching an edge carve a corridor into the upper hex and raise a ramp from
the lower hex, meeting at a shared edge height. An exit from either dry hex is
enough: a road ending against unpaved ground still has a continuous approach.
Grounded movement follows that surface. Other terrain retains its actual level
step. There are no gaps, global inset/interpolation settings, or stretched top
textures on walls.

The GPU board always uses its own Saxarba tileset at
`mm-data/data/models/board/tileset/saxarba.tileset`, independently of the classic
board preference. Its 7,269 copied files include the recursive include tree and
referenced art outside the Saxarba directory. They are ordinary independent
files, so editing them does not change the 2D board.

Buildings use the exact image selected by that tileset, including theme, fluff,
and connected-section variants. Offline outline simplification and Blender
triangulation produce 3,015 indexed building models with original roof UVs and
pixels, straight wall edges, and retained courtyards/disconnected parts. The
largest is 499 triangles. Windowed facades repeat per story and inherit a tint
from the roof palette. Blank tileset sections stay blank. There are no generic
replacement building shapes or runtime terrain-image extrusion.

The asset directory also contains tank, factory, bridge-arm, crop-row, sixteen
tree models and six rubble rock models; all 26 are at or below 480 triangles.
Runtime only loads models
actually used by the board. Roof and wall picking uses their actual triangles.

`BoardFeatures` copies model placement and each feature's own height from the
hex on the Swing thread. Buildings, fuel tanks, industrial structures, foliage,
and bridge decks retain their respective game heights. Forest density controls
tree count: light uses three trees, heavy nine, and ultra-heavy sixteen. Light
woods spread their trees around the centre with a small variation in radius. Dense
woods and jungle use an equal-area spiral across the hex; deterministic positions
do not change between snapshots. Snow
terrain or a snow theme selects snow trees. Jungle and desert/sandy woods select
palms unless snow is present; paving does not change the desert tree selection.
Other woodland mixes broad/slender common trees, birch, willow and pine
silhouettes; jungle uses two palm shapes. Rubble scatters three rock shapes
at 0.16–0.265 of a level, with separate snow-covered models. Props sit on the
actual ground surface, including road approaches.

Bridge decks and rails use the copied Saxarba `bridges/bridge_09.png` artwork,
oriented along each bridge arm. Deck and rail tops retain the plan-view UVs;
vertical rail and fascia faces unwrap the original guardrail strip, including
its bars and supports. The 36-triangle arm has no trim crossing the roadway at
hex boundaries. A 0.16-world-unit deck clearance at default scale
separates zero-elevation bridges from the riverbank and its road decals, avoiding
coplanar depth flicker without changing the game's bridge elevation.

Exposed grassland faces use dirt; sand uses sandstone; rough/rubble and rocky
themes use rock; pavement uses concrete. Snow has rock beneath snow cover.
World-scaled UVs repeat the wall material once per 96 world units, with a stable
world-space phase instead of restarting each face. Repeating materials sample
at no more than 128 texels across, matching the original board artwork more
closely; editable source images retain their resolution. Window spacing is
eight windows per 128 world units. A ragged cover strip descends 12–18 world
units from the upper edge, clipped to the exposed face and faded at its lower
boundary. Its profile varies continuously in world space. Grass retains its
authored cover texture. Other surfaces fold a strip of the hex's actual ground
artwork down the face at the original texel density, so desert, volcanic, lunar,
Martian, concrete and snow edges inherit their top surface's colour and detail.
The fade is independent of the UVs and blends into the geology below; samples
meet at corners and follow atlas updates when the ground artwork changes.

The copied `High_Incline` south-edge (`08`) artwork adds top detail along exposed
sides at its original scale. Grass, snow, sand and rocky/earth materials use
their corresponding art; concrete uses the neutral rocky edge. Each patch is
oriented to its actual edge and clipped around road approaches. All orientations
use the same source instead of alternating baked bright `01` and dark `08`
variants. The original small-scale shading remains in the artwork, blended
over the ground; scene lighting and shadows remain dynamic.

Blender source, reproducible exporter, texture prompts, model counts, and
Quaternius CC0 attribution are recorded in the asset directory's README and
`mm-data/tools/`. Runtime loads indexed G3DJ files and requires no Blender
installation. The Gradle data-staging task includes these models and textures.

## Water

A water hex's solid surface is its riverbed. Positive depth lowers it by
`depth * LEVEL`. Depth zero makes a two-world-unit recess at default scale,
with the water one unit below the surrounding top, so grounded meeples only wet
their feet. Positive-depth water sits just below the hex's surface elevation.
Water of every depth sits one world unit below the nominal surface so changing
depth does not introduce a water-surface step. Rounded, slightly irregular
shorelines follow the water-neighbor pattern, inspired by `Structured_Water`.
Dry-facing edges have a land bank and sloping submerged shore; adjacent water
hexes share exactly matching open mouths. Different bed depths retain their
physical underwater steps. Each shore segment uses a bounded six-piece curve.
A blended sandy band fades from the land into damp sand along the waterline.
Hexes with exactly two nonadjacent water neighbors use a curved channel with
consistent width instead of a bay around each hex centre. The bed remains at
full depth beneath the hex centre, keeping grounded units on the riverbed.
Concave channels are triangulated from their outline for both rendering and
picking. Junctions, adjacent openings and isolated pools retain the bay contours.

An open mouth leading to a lower, unfrozen water hex generates one vertical
waterfall from the upper surface to the lower surface. It uses the upper hex's
animated water artwork with vertically repeating UVs scrolling downward, at
80% opacity. Water artwork extends into the GIF's transparent hex corners so
scrolling changes only the flow pattern, never the waterfall's width. Its fixed
edges match the river mouth. Equal surface levels and frozen connections do not generate falls.
These are lightweight animated surfaces, not a fluid simulation.

The renderer composes offset/transparent GIF patches into complete frames and
decodes the supplied Saxarba `anim_water_0.gif` through
`anim_water_4.gif`, respecting frame delays and sharing frames between hexes
of equal depth. Depth two uses the depth-two animation. Depths greater than four
keep their actual bed depth and use the deepest supplied artwork.

Water renders after units with 48% opacity, depth testing, and no depth writes.
The bed remains opaque, so underwater units are visible through water without
showing objects through solid terrain. Ice preserves the underlying riverbed
and draws an opaque captured ice surface at the game's surface elevation.

## Units, visibility, and animation

Only unit meeples use sprite-alpha extrusion. `GpuCutout` triangulates their
silhouette and merges cap runs; it is never used for terrain models. The sprite
textures the top and its alpha-weighted color shades the sides. Camouflage and
damage markings remain; classic generated drop shadows and smoke are omitted
from the meeple texture.

Footprint size uses `UNIT_SCALE` independently of hex scale. Thickness is the
game's occupied height multiplied by `LEVEL * UNIT_HEIGHT_SCALE`, including
stance. Sensor contacts retain anonymous artwork and a generic height.
Visibility is resolved by the existing client before the snapshot is published.

Walking/running paths, jump arcs, facing, labels, and shadows use the same
`UnitMotion` timeline in every camera view. Airborne units retain their flight
height and have a subtle independent wobble plus a tether to their hex when
stationary. Animation and skipping never change game state.

Annotations use the existing entity painter, rasterized at higher resolution
and drawn in screen space. They follow the animated unit, spread around nearby
labels, and stay clamped to the viewport edge when their unit is offscreen.
Normal game visibility still applies. Crowded views prioritize selected and
hovered labels; extreme crowding can still overlap.

A visible unit intersecting a feature's bounds fades the features in that hex
to 24% opacity. Occupancy follows animated transforms, including movement and
flight height. Faded instances do not write depth or cast an opaque shadow;
normal opacity and shadows return when the unit leaves. This uses conservative
bounding boxes, not mesh collision, and never exposes hidden game entities.

## Controls and presentation

| Control | Action |
| --- | --- |
| Top view / Isometric | Restore a camera preset, retaining focus and zoom |
| Fit board | Frame terrain, water, and feature heights |
| Mouse wheel / +/- | Zoom |
| Right or middle drag | Pan |
| Shift + right/middle drag | Orbit and tilt |
| Click / short right-click | Inspect a hex or visible unit |
| Plot movement here / movement mode | Enter the existing persistent board tool |
| Escape | Dismiss a menu or leave the board tool |
| Ctrl-click / Measure line of sight | Existing two-hex LOS tool |
| All actions / F10 | Search the current phase's commands |
| Tab, then Enter | Cycle visible units and inspect |
| Up/down, then Enter in a menu | Navigate enabled choices and activate |
| Orders / Clear orders / Done or Skip | Inspect, clear, or explicitly commit through the original handlers |
| Tuning / F9 | Adjust geometry, time of day, clouds, fog, haze, and exposure |
| Speed / Space during playback | Change playback rate / finish queued animation |

The HUD and context menus reuse the configured MegaMek skin and its original
button artwork. Context menus have readable action rows, expandable groups,
search, scrolling, disabled explanations, and viewport clamping. Keyboard
navigation skips disabled actions. Camera rotation, fitting, and menu interaction
do not issue game orders.

Tuning defaults are hex scale 1, unit scale 0.6, unit height scale 0.87, level
height 18, and grid shade 0.8. A single tuning record updates all derived
dimensions on the render thread. No geometry tuning requires a second artwork
capture or an alternate renderer.

Scene2D reads monitor DPI and window size. Font rasterization and UI scaling
keep controls usable from small windows to 4K. Menus and board input consume
complete gestures independently. Detailed dialogs and chat entry still use the
Swing client. See [contextual-ui.md](contextual-ui.md).

Time, lighting, weather presets, and their limits are documented in
[gpu-atmosphere.md](gpu-atmosphere.md). Daylight is calibrated for the tileset's
LDR artwork; its postprocessing does not add another filmic contrast curve.
Tactical markings and hex text draw after atmosphere compositing with restored
opaque depth. HEIGHT labels are raised to the building/feature height and fit
on the roof footprint. Other hex labels retain their terrain anchors.

## Ownership and rendering

- `GpuBoardSource` reads game objects, visibility, tile artwork, and existing
  commands on Swing's event thread and publishes immutable presentation frames.
- `BoardView.capturePlanarHexes` separates ground, flat decals, and tactical
  pixels in bounded chunks. It repaints the existing paths, borders and symbols
  at three times terrain resolution, retains ECM, FoV, special-hex, deployment,
  and plugin painters, and restores classic view state. Empty marking images
  are omitted. Ground has limited mip filtering; markings use exact UVs and
  independent textures. Changing a marking does not rebuild terrain geometry.
  Capture raster scale is independent of the classic zoom index, including
  image-backed ECM shading. Measurement cursors rasterize at that same scale.
- `BoardGeometry` owns dimensions and terrain picking; `BoardSurface` defines
  the physical roads, banks, beds and exposed sides.
- `GpuAssets` owns shared feature meshes, repeating textures, and water frames.
- `GpuTerrain` batches terrain and opaque features in 8 by 8 chunks. Changes to
  a tile's height/material/features rebuild its chunk and affected neighbors;
  atlas-layout, board-size, floor, and tuning changes rebuild the required
  scene. Pixel-only changes update texture slots. Camera changes reuse meshes.
- Chunk bounds cull offscreen terrain and include water and feature heights.
  Unit bounds first reject unrelated chunks before testing feature occupancy.
- The draw order is opaque terrain/features, flat decals, units, transparent
  water/faded features, tactical marks, and screen annotations/UI.
- One 2048-pixel directional shadow map includes terrain, opaque features, and
  units. It refreshes when geometry, light, occupancy, or unit transforms change;
  camera-only changes reuse it. Large boards have lower shadow resolution.
- GL resources are created and disposed on the render thread. Shared assets own
  their textures; instance material changes do not transfer ownership.
- `GpuBoardActions` adapts real phase buttons, menus, weapon lists, and ammunition
  models. Callbacks return to Swing and recheck the panel, phase, turn, actor,
  target, and current availability. There is no second rules engine.

## Verification

Run from the checkout root:

```text
.\gradlew.bat :megamek:test --tests "megamek.client.ui.clientGUI.boardview.gpu.*Test" --offline
.\gradlew.bat :megamek:gpuBoardSmoke --offline
```

Regular tests cover geometry/picking, one-ended road ramps in all six directions,
scale independence, complete animated GIF frame composition, water and ice depth,
snow selection, feature heights/exits, movement and jump playback, visibility,
sensor contacts, snapshot reuse, classic state restoration, shared tactical
markers, full-hex friendly/enemy deployment ECM and measurement outlines,
input consumption, stale commands, stacked targets, hidden-page
commands, and weapon/ammunition integration.

Native tests require desktop OpenGL. They exercise actual Scene2D input,
camera gestures, movement playback, exclusive-window switching, reopening,
resize through 3840 by 2160, and the atlas packing boundary. Rendered-pixel
checks verify unit lighting/shadows, translucent water at depths 0/1/2,
occupied-feature fading, and opacity restoration. Representative building
families and all 26 other models are loaded natively; GIF frames must advance.
`mm-data/tools/validate_board_assets.py` checks every exported model's budget,
indices, roof winding/pixel fidelity, and independent texture dependencies.

`GpuAssetCatalogSmokeTest` renders actual Saxarba ground with the authored
catalog, connected buildings at unequal heights, snow trees, bridges, ice,
water depths, road level changes, and material-specific exposed walls. Images and measured frame
times are written to `megamek/build/gpu-board-review/`.
`GpuTacticalSmokeTest` captures friendly and enemy deployment ECM and a distance
measurement outline over elevated terrain.
`GpuRiverSmokeTest` captures straight/bent channels, textured elevation drops,
mixed woodland, snow variants, low rubble and a zero-elevation textured bridge
over the riverbank in both camera views. A pixel sample on the vertical waterfall
verifies that the fall itself animates.

## Scope and limits

This is a 3D board renderer with native contextual controls. Shared Java2D
painters still supply tactical pixels, annotations, and existing HUD widgets.
Their refresh and initial image loading can still stall presentation. Tactical
capture is polled on Swing; it is not an entirely native vector HUD. Roof
illustrations retain their original resolution and baked rooftop detail.
Updating a copied roof silhouette requires rerunning the offline asset build.

Transparency uses bounding boxes and normal
alpha sorting, not volumetric water or order-independent transparency. Terrain
still occludes units behind banks; screen annotations remain available.

The automated checks do not establish an end-to-end human playthrough of every
aerospace, artillery, transport, multi-map, bridge, or special-equipment
workflow. Windows desktop OpenGL has been exercised; other platforms and
physical monitor transitions need validation. Measured frame times are from
a capped/vsync workload and establish neither an uncapped performance limit
nor a speedup over the classic renderer.

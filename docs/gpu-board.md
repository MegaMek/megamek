# GPU battle view

The Java client has a libGDX/LWJGL3 board with one 3D scene, an orthographic orbit
camera, animated unit models, and contextual Scene2D controls. From deployment
onward, choose **View > GPU Battle View (Experimental)**. Launch this checkout with
`.\gradlew.bat :megamek:run` on Windows or `./gradlew :megamek:run` elsewhere.

The classic window hides after the first GPU frame. **View > Classic Board**, or
closing the GPU window, disposes the renderer and restores the existing client.
The game, pending orders, and phase controls remain the same. Startup failure
leaves the classic interface available; closing the game does not reopen it.
The bottom-right corner shows the measured rendering FPS, refreshed once per second.

## Terrain and assets

Hexes tile edge to edge, with exposed walls where their surfaces stand above
neighbors. `BoardSurface` supplies the same triangles to rendering and picking.
Roads reaching an edge carve a corridor into the upper hex and raise a ramp from
the lower hex, meeting at a shared edge height. Across a difference of at most
two levels, an exit from either dry hex gives a road end a continuous approach
onto unpaved ground. Larger differences require connecting road exits on both
sides; otherwise the road ends flat and the cliff remains intact. A road exit
meeting a bridge arm at the road's elevation stays level with its deck, without
cutting the road or raising the ground beneath the bridge.
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
triangulation produce 3,295 indexed structure models with original roof UVs and
pixels, straight wall edges, and retained courtyards/disconnected parts. The
largest is 499 triangles. Facades follow the selected artwork's family and roof
palette: light windows, medium concrete, hard reinforced concrete, heavy armor,
sci-fi fortress walls, hangar shutters, tank metal and industrial service panels.
Hangar doors and fortress buttresses span the complete wall height; ordinary
courses retain fixed story spacing. Fuel tanks and industry use the same exact
roof pipeline. Blank tileset sections stay blank. There are no generic
replacement building shapes or runtime terrain-image extrusion.

Buildings also get simple square interior struts and floor sheets at each game
level. Floors reuse the roof triangles, preserving courtyards and disconnected
sections; struts are sparse, untextured boxes inside that footprint. Interior
models are shared by asset and story count. Fuel tanks and industrial terrain
do not receive building interiors.

The asset directory also contains bridge-arm, crop-row and sixteen authored tree
models, plus three rendering detail levels for each tree. All are at or below 480 triangles.
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
silhouettes; jungle uses two palm shapes. Props sit on the actual ground surface,
including road approaches.

Natural open terrain also gets sparse cosmetic scatter from `BoardFeatures`:
grass tufts and low leafy plants on grassland, dry grass on dirt/tundra, and two
stone profiles on grass, rock/lunar, sand, dirt and snow. Barren lunar/Mars/volcanic
themes use only stones. Sand occasionally uses
the low plant as a muted succulent. Stones share geometry with surface-specific
tints. `BoardFeatures.SCATTER_DENSITY_MULTIPLIER` scales all biome placement
chances: `0` disables scatter, `0.5` halves it, and `1` is the original baseline.
It is currently set to `3.0f`: coordinate-seeded placement occupies about 48% of
grass hexes, 54% of rock, 36% of dirt, 30% of sand and 18% of snow. Higher values
cap at every eligible hex; one in five occupied hexes gets a
second detail. Offsets, rotation, proportions and shade vary without reshuffling
on scene updates. Water, roads and their sloped approaches, structures, fields,
woods/jungle and special hazardous terrain stay clear.

`GpuScatter` bakes these untextured, opaque details into one additional mesh part
per nonempty 16-by-16 chunk: grass uses 6 triangles, leafy plants 16, and stones
9. This adds no per-object draw calls, textures, animation or picking geometry.
The details stay below 0.2 elevation levels and away from hex edges. Color,
camera depth and shadows share one visibility choice: hide a chunk's scatter
when its largest detail projects below 2 framebuffer pixels, restoring it at
3 pixels. This hysteresis avoids flicker, and zoom changes do not rebuild the
scatter mesh. There are no additional mesh LoD steps for these tiny objects.

Dry grass, flat stones and desert plants reuse the four basic shapes. Small
flowers, mushrooms and fallen branches were left out: they add little readable
variety at normal board scale, while larger versions would compete with terrain
and unit silhouettes. Short reeds in occasional shoreline patches are a useful
future candidate, but need placement against the actual water contour.

`BoardScatterTest` checks sparse, stable placement, biome choices and protected
terrain. `GpuScatterSmokeTest` measures the actual submitted geometry: its
256-hex, four-surface sample at `3.0f` has 144 details, 1,296 triangles and one additional draw
per visible pass, dropping to zero below the distance cutoff. It also checks
cutoff hysteresis, matching shadows/depth, unchanged picking and clear road
approaches, and captures overview/close views using the shipped ground artwork.

Trees select detail from a conservative projected bounding diameter in framebuffer
pixels. The board is orthographic: its fixed orbit distance is not a useful LoD
measure. `TreeLod` uses 80- and 24-pixel thresholds with 10% hysteresis. Zooming out
switches below 72 and 21.6 pixels; zooming in restores detail above 88 and 26.4.
Top and orbit views use the same selection, including display scaling and tree
height/hex-size tuning. Color, camera depth and shadows use that selection;
picking keeps the near mesh so zoom cannot move a board-space hit. Prop batches
rebuild only when a tree crosses a threshold, and dispose their replaced meshes.

The original trees contain 478–480 triangles. Near opaque trees omit only faces
strictly enclosed by another closed component, retaining every remaining vertex,
normal, UV and color: 366–480 triangles, 8.4% fewer across the equally weighted
catalog and up to 23.75% for one tree. Trees always remain opaque. The original
meshes are visual references. The two distant levels use 238–240 and 94–96 triangles, at least
50% and 80% below the originals. Their textures, snow materials, coordinate system
and placement remain shared. Only used models are loaded by `GpuAssets`.

Halving every near tree changes visible silhouettes at the minimum manual zoom
of 0.1, especially palm fronds and pine/snow outlines. It is therefore reserved for
distant views. Native GL comparison of all sixteen optimized near meshes against
the original catalog at that zoom found identical opaque pixels in 192 views:
four bearings at top-down, isometric and maximum 80-degree tilt. These are geometry
and image results, not an FPS benchmark. `GpuTreeLodSmokeTest` also checks submitted
triangle counts, repeated transitions, fading, depth, shadows and stable picking.

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
closely. Building facades and geology ship as 128 by 128 PNGs under
`textures/buildings/` and `textures/terrain/`; untouched originals live in each
folder's `full-resolution/` subdirectory. `tools/prepare_board_textures.py`
rebuilds those runtime copies. Light-building window spacing is
eight windows per 128 world units. Six 128 by 128 rim maps add material-specific
turf, soil, sand, rock, concrete and snow detail, tinted from opaque pixels just
inside the selected hex's ground artwork. This keeps desert, volcanic, lunar,
Martian and snow palettes tied to the actual tileset. A shared irregular mesh
profile descends 4–14 world units and fades over its final 1.5 units.
World-scaled UVs repeat once per 96 units in both directions, independently of
rim depth. Short and sloping walls clip the rim instead of squeezing or stretching
its texture. The profile meets at corners, and changed ground pixels refresh
the affected chunk's rim colors.
Concrete retains its rim and incline overlay, including between paved hexes.
Its rim has a straight lower edge at a constant nine-world-unit depth; all
other materials keep the irregular profile.

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
with the water one unit below the surrounding top, so grounded units only wet
their feet. Positive-depth water sits just below the hex's surface elevation.
Water of every depth sits one world unit below the nominal surface so changing
depth does not introduce a water-surface step. Rounded, slightly irregular
shorelines follow the water-neighbor pattern, inspired by `Structured_Water`.
Dry-facing edges have a land bank and sloping submerged shore; adjacent water
hexes share exactly matching open mouths. Different bed depths retain their
physical underwater steps. Each shore segment uses a bounded six-piece curve.
A separate 128 by 128 bed map supplies silt, sand and small pebble detail at
the same fixed material scale; it contains no baked water-surface reflections.
A blended sandy band fades from the land into damp sand along the waterline.
Each bank continues the adjoining dry hex's selected terrain artwork into that
fade, so sand, snow and other terrain retain their own shoreline palette.
Banks at the board boundary use the water hex's own ground artwork.
River mouths use roughly 33 of the hex edge's 42 world units at default scale;
their sandy fade starts at the edge corners. Connected channels retain this
width through bends, while isolated basins keep their rounded land banks.
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

Hull-down Meks take a knee using the shared posture timeline; quadrupeds lower onto
all four knees. Resolved movement carries hull-down observations, so units kneel
after arrival and stand before travelling. Pausing, skipping and newly revealed
units use that same pose. This is separate from voluntary prone and forced falls.

Conventional infantry and battle armor share one articulated base mesh per equipment
type (rifle, jump pack, or armored suit). Formation recipes reference `trooper` and
optional `jumpTrooper` assets instead of pose variants. Idle watch motions, kneeling,
walking and jump tucks animate those joints; the existing jump emitters follow the
posed pack. Member identities keep idle variation stable across casualties and
material changes. Each member rises for movement and settles into its watch stance
after its own arrival, including transport unloading.

Units use authored bodies assembled with equipment at runtime. `GpuUnitModel` shares placement,
rigs and annotation bounds. Extruded sprite meeples have been removed; unavailable or disabled
models retain a flat two-triangle sprite fallback. Raised sensor/terrain symbols independently
use `GpuCutout` to extrude their silhouettes. Those symbols do not reveal a hidden unit's model.

`GpuUnitInstance` hides attached equipment when its bounding diameter falls below 4 screen pixels,
with 15% hysteresis. Larger silhouette-defining weapons persist longer. Selected units and units
participating in current attacks show full equipment. Every pass shares that choice, including shadows
and outlines. The actual body, embedded infantry detail, picking bounds, damage flags and emitter
transforms are retained; changing detail never reassembles a unit or uploads a mesh.

Single-hex footprint size uses `UNIT_SCALE` independently of hex scale. Multi-hex models use
their full occupied footprint and `MULTI_HEX_UNIT_SCALE` (default 0.85), with a dedicated tuning slider.
Family unit/height scales multiply board tuning and default to 1. Articulated models retain their authored
proportions when prone; animation supplies the changed stance. Sensor contacts use a red question mark with a fixed marker size.
Visibility is resolved by the existing client before the snapshot is published.

Walking/running paths, jump arcs, facing, labels, and shadows use the same
`UnitMotion` timeline in every camera view. Airborne units retain their flight
height and have a subtle independent wobble plus a tether to their hex when
stationary. Animation and skipping never change game state.

Mek and ProtoMek gait cadence follows distance traveled, including acceleration and braking.
Their stride coefficient is 1.6 leg lengths per cycle, with 0.16 leg-length lift; longer cycles increase
airtime and reduce cadence by 25% from the former 1.2 stride. Foot support follows ground travel.
ProtoMek fallback art has separate foot joints, including quad and glider forms; authored triangle counts stay unchanged.

A Mek is published at its secondary (torso) facing, as the classic sprite is,
together with how many hexsides that is from its own facing. A flat sprite turns
as one piece. An authored mesh whose descriptor names an `upperBodyNode` keeps
its legs at the unit's own facing and turns only that node, easing to a new
twist at `UpperBodyTurn.DEGREES_PER_SECOND`. A descriptor without the key turns
as one piece. `[GpuTwist]` debug lines record each change.

The snapshot also lists a Mek's physically lost locations by the game's
abbreviation, which is the name of that location's node in every Mek mesh.
`UnitDamageDisplay` stops drawing a lost arm and gives any other lost location
a dark copy of its material, on the unit's own instance only. `[GpuDamage]`
debug lines record each change and any location the mesh has no node for.

Damage textures share one shader overlay, applied after the model's baked vertex colors so black detail
colors cannot erase destroyed artwork. Destroyed takes priority over structure, then armor. The unit ID
and part name seed each pattern's rotation, scale and offset; smooth coordinate warping breaks up the
regular tiling with one damage-texture sample per shaded pixel. Textures remain shared, and the pattern
stays attached through movement and survives model-instance rebuilds.

Cockpit glazing keeps its original appearance until destroyed. The shader recognizes the exporter's
fixed `PALETTE['glass']` vertex color before tinting, so glass can remain in the existing detail meshes
without additional draw calls. Only the destroyed overlay ignores that mask.

At the bottom of the tuning panel, an unchecked **Override visible unit damage** checkbox enables a
**Display damage** slider from 0 to 1. Non-Meks use the existing whole-body thresholds. Meks apply the
value to every location: 0–0.5 removes armor, then 0.5–1 removes structure with armor fully stripped.
Destroyed or detached locations keep their priority. Disabling the preview restores actual damage;
the game state never changes. **VSync**, beside **Normal maps**, takes effect immediately and defaults
to enabled. The window's separate 60 FPS cap still applies. **Defaults** restores both controls.

**Planetary conditions...**, beside **Daylight & atmosphere**, opens the existing planetary conditions
editor initialized from the game's conditions. Accepting it applies the shared scenario-to-weather
mapping to all lighting and weather controls; Cancel leaves the current preview alone. The editor
changes only this GPU preview, and **Defaults** still restores the scenario's original appearance.

Annotations use the existing entity painter, rasterized at higher resolution
and drawn in screen space. They follow the animated unit, spread around nearby
labels, and stay clamped to the viewport edge when their unit is offscreen.
Normal game visibility still applies. Crowded views prioritize selected and
hovered labels; extreme crowding can still overlap.

A visible unit intersecting a non-tree feature's bounds fades the non-tree features in that hex.
Buildings and other props default to 50% opacity. **Tuning > Building opacity** changes this live from
0% to 100% in 5% steps, including when the unit is already inside. Building
floors follow the same opacity; interior struts always remain opaque, write
depth and cast shadows. Buildings cast their full, solid shadows at
every opacity, including 0%. At 100%, features also retain their normal depth.
Occupancy follows animated transforms, including movement and flight height.
Faded surfaces do not write camera depth; normal opacity and depth return when
the unit leaves. This uses conservative
bounding boxes, not mesh collision, and never exposes hidden game entities.

Trees have no opacity control or fading path; the unit outline handles canopy occlusion.
Their opaque geometry stays in chunk batches, with individual CPU props retained for stable picking.
LoD checks run per chunk using its largest tree, conservatively retaining detail on smaller trees.
Each detail-level batch is cached on first use until the chunk is replaced or disposed. Repeated zoom
changes reuse buffers; building cutaways have separate caches. Caching several levels costs more retained
mesh memory. Forests skip unit occupancy/bounds checks, and unit-scale tuning does not rebuild tree geometry.
Taking an improvised tree club adds equipment without removing scenery. Only the authoritative woods/jungle
cover level determines tree count: ultra-heavy, heavy, light and clear progressively reduce the visible trees.
The renderer does not calculate terrain damage or maintain a separate tree inventory.

**Tuning > See-through** is an independent occlusion highlight, enabled at 75%
by default. A thin team-colored outline, dark outer edge and faint filled silhouette
identify the portions of a unit hidden behind higher terrain or other opaque
geometry, including opaque trees and buildings with the cutaway disabled. The slider scales
the outline and fill together from 0% (off) to 100%; normally exposed parts of
the unit keep their original appearance. The effect does not change terrain,
shadows, picking or game visibility. Colors follow the existing own/allied/enemy
preferences, or player colors when team coloring is disabled. Sensor contacts
retain anonymous question-mark geometry and a fixed red highlight; units omitted by the
client's visibility rules are never drawn.

The highlight compares the nearest unit surface against the rendered scene's
depth, using the same animated model instances in every camera view. This avoids
highlighting a unit's own rear surfaces or overlapping limbs. Superimposed units
share the nearest surface at each pixel. Surfaces already made transparent by
the opacity controls use their existing see-through rendering. The highlight
is drawn after atmosphere/weather and before tactical annotations, with an
outline sized in screen pixels. It shares camera-depth capture with fog when
available and adds a depth pass when needed on a clear board. Its unit-depth
and color buffers are reused until the viewport changes; 0% skips the highlight work.

Captured camera depth is restored after atmosphere compositing with one fullscreen draw, replacing a
second submission of every unit and terrain mesh. If no depth capture was required, the original direct
depth pass remains. Shadow resolution and caster coverage are unchanged. Shadow transforms are reused,
and changes only to light color, fog or exposure do not invalidate the cached shadow geometry.

`GpuMarkers` owns the raised symbol artwork, shared models, and one cosmetic
animation clock. Symbols spin once every eight seconds in
oblique views. At or below `FLAT_TILT_DEGREES` (30 degrees away from overhead),
they stop spinning and align their face and upright direction with the camera,
including while the camera rotates. Size, thickness, spin period and clearance
are constants in the same class. The classic 2D board retains its existing art.

`BoardView.getBoardMarkers()` snapshots existing sprite-handler output and shares
minefield, demolition-charge, and artillery presentation with the classic painters.
Special displays retain `SpecialHexDisplay.drawNow` filtering, including private
notes, phase/round limits and preferences. The GPU capture omits the corresponding
flat icons. Support height starts at the hex's ceiling and rises above the rendered
units overlapping that hex. Placement runs after unit poses, movement and hover,
using `UnitBounds`, so the lowest edge clears buildings, bridge decks and units. Every
marker writes scene depth. Each kind has its own named `*_OUTLINE` constant in
`GpuMarkers`; all default to `true` and use the existing **See-through** intensity
slider. The pass shows geometry: a fully occluded coin shows its coloured disk
silhouette, without the face's glyph or lettering.
Turning off one type's constant restores normal occlusion for its symbol without changing
other markers or units. Sensor contacts retain their anonymous labels and game
visibility checks. `SensorRangeSprite` draws range borders and does not supply
the contact icon.

The converted kinds include:

- Minefield signs and demolition-charge symbols (`BoardView.drawMinefields`,
  `drawDemolitionCharges`), preserving the client's knowledge and owner filtering.
- Artillery targets, incoming fire, and adjusted/auto-hit symbols; incoming orbital
  bombardment and nuclear warnings (`BoardView` and `SpecialHexDisplay`). Keep
  area footprints and drift lines on the board, separate from the raised symbol.
- Player notes (`SpecialHexDisplay.PLAYER_NOTE`), with their existing visibility.
- Objective/victory flags (`HexFlagSprite`), preserving owner colour, scheme and progress.
- Flares (`FlareSprite`) and cargo/pickup symbols (`GroundObjectSprite`). A physical
  flare or cargo model may eventually replace the icon, using the same placement.
- Saw clearing, repaired bridges, bridge construction, fortification construction,
  rubble clearing and digging-in status. Mixed engineering sprites retain their
  trench/bridge/sandbag/cleared-terrain artwork; only their status moves above it.

Details and progress counters use a separate cached camera-facing label layer.
It avoids the symbol bounds and unit labels. Unit labels also avoid location
symbols, even when spreading labels between units is disabled. Point-symbol picking uses the same
rendered meshes as unit picking and returns the original hex for its tooltip and
commands. A symbol's outline switch does not alter game visibility.

Several location symbols in one hex form a compact grid in angled views (up to
`GROUP_COLUMNS` across), with `GROUP_SCALE` and `GROUP_SPACING` controlling size
and spacing. Its width is capped to the hex's interior; an incomplete last row
is centred. Near overhead, symbols flank a unit on its left and right, leaving
the question mark's hook and dot clear. Each side forms a short arc controlled by
`TOP_UNIT_MARKER_SIDE_ARC_DEGREES`. Empty hexes use a ring instead. `TOP_GROUP_*`
and `TOP_UNIT_MARKER_*` control size and radius, always inside the source hex.
Dense groups reduce symbol size to fit their spacing. These offsets
are presentation only, and every symbol still clears the unit or roof in Z.

Sensor contacts occupy the unit's own position and elevation, independently of
this grid. They remain centred and larger in both camera modes, controlled by
`SENSOR_SIZE_IN_HEXES` (0.85 hex heights versus 0.5 for a single location symbol).
In top view, sharing a hex reduces the question mark to `SENSOR_CROWDED_SIZE_IN_HEXES`
(0.6), or `SENSOR_DENSE_SIZE_IN_HEXES` (0.5) with more than four location symbols.
Other markers never offset the contact or lift it onto a roof.
The same placement and see-through outline represent its occupancy when hidden
inside terrain or a building. Location symbols clear its rendered height just
as they clear a known unit.

To add a kind, extend `BoardMarker.Kind`, supply the visible descriptor from its
existing sprite or painter, add its artwork and outline constant in `GpuMarkers`,
and omit the old point-symbol painting during GPU capture. `BoardScene` owns an
immutable list of these descriptors; the GL thread reads no game objects.

Movement/range borders, flight-path arrows, deployment areas, and heat-map fills
describe board geometry and should retain that spatial layout rather than spin.
Diagnostic firing heat-map icons also remain in the board layer with their combined turn labels.

## Controls and presentation

| Control | Action |
| --- | --- |
| Top view / Isometric | Restore a camera preset, retaining focus and zoom |
| Fit board | Frame terrain, water, and feature heights |
| Mouse wheel / numpad +/- | Zoom |
| Right or middle drag | Pan |
| Shift + right/middle drag | Orbit and tilt |
| Q / E | Turn the camera one hex side (60 degrees); hold to keep turning |
| Page Up / Page Down | Tilt toward overhead / lower the viewing angle while held |
| Home / End | Reset camera and fit board / fit board at the current angle |
| T / Z | Toggle the isometric preset / toggle the overview zoom |
| Click / short right-click | Inspect a hex or visible unit |
| Plot movement here / movement mode | Enter the existing persistent board tool |
| Escape | Dismiss an open menu/tuning panel; otherwise leave the board tool and invoke Cancel |
| Ctrl-click / Measure line of sight | Existing two-hex LOS tool |
| Alt-click | Existing two-hex ruler; measurements bypass the phase's plotting tool |
| All actions / F10 | Search the current phase's commands |
| Tab / Shift+Tab | Select the next / previous unit through the current phase's controls |
| Enter / slash | Activate the existing chat box / start a chat command |
| Ctrl+K / Ctrl+P | Toggle the keyboard shortcuts / planetary conditions overlay |
| Up/down, then Enter in a menu | Navigate enabled choices and activate |
| Orders / Clear orders / Done or Skip | Inspect, clear, or explicitly commit through the original handlers |
| Tuning / F9 | Adjust geometry, overlap opacity, see-through intensity, time of day, clouds, fog, haze, and exposure |
| Speed / Space during playback | Change playback rate / finish queued animation |

Configured gameplay and menu shortcuts use the same handlers and availability checks as the classic board.
This includes the other overlays, labels and coordinates, range and movement displays, unit/minimap/force/bot
panels, reports, settings, saves, and loads. Camera bindings control the native camera. F9 and F10 open the
GPU tools only when those keys have no configured binding. Text entry and open menus take keyboard focus;
closing them restores the board shortcuts. Keypad navigation honors Num Lock, including the weapon-mode keys.

The HUD retains the configured MegaMek skin and its original action-button artwork.
Menu bars, dropdowns and context menus use separate flat controls: compact rows on
a matte surface, hover/keyboard highlights, and trailing arrows for submenus.
The bottom action bar keeps its metal buttons, with bold confirmation labels.
Search is reserved for the All Actions palette. Dropdowns open immediately below
their trigger, or above it when there is more room; context menus stay beside the
board click. Menus retain scrolling and viewport clamping; disabled explanations
appear in tooltips instead of expanding every unavailable row. Keyboard navigation skips disabled actions.
Camera rotation, fitting, and menu interaction do not issue game orders.

The Tuning panel shares the dropdown menus' flat styling and compact controls.
One narrow, scrollable column keeps the board visible while adjusting geometry,
visibility, field of view, atmosphere, and weather. Longer help text is available in tooltips.

Tuning defaults are hex scale 1, unit scale 0.7, unit height scale 0.87, level
height 18, grid shade 0.8, building opacity 50%, and see-through
intensity 75%. Opacity is local to the GPU window and changes materials without
rebuilding terrain. Defaults restores these values. A single geometry tuning record updates all derived
dimensions on the render thread. No geometry tuning requires a second artwork
capture or an alternate renderer.

Scene2D reads monitor DPI and window size. Font rasterization and UI scaling
keep controls usable from small windows to 4K. The shared overlay painters and
their text caches rasterize at the framebuffer's native pixel density while
retaining logical coordinates for layout and input. Menus and board input consume
complete gestures independently. Keyboard, planetary conditions, and turn details
panels keep their artwork separate from a shared 200 ms fade timeline. The GPU
evaluates opacity each render frame, retaining the same texture throughout a fade
and reversing from the current opacity when toggled again. Other widgets retain
their painter order. Chat uses the same 200 ms timing for its slide. Toasts carry
their entire 300 ms fade-in, configured hold, and 1 second fade-out schedule to
the renderer; stack changes use timed position transitions. Each animated widget
retains its own texture, so adding a toast does not rebuild the other panels.
The unit overview also uses individual cached cards and scroll-button images,
instead of a viewport-sized raster. A card is repainted only when its visible
portrait, name, bars, condition text, border, or display density changes. Layout
and input keep their existing logical coordinates. Unit labels cache the shared
sprite painter's derived presentation (text, colors, statuses, bars, and TMM
pips); unchanged labels reuse both their compact image and immutable pixel
snapshot. Camera movement does not invalidate them. Sensor-only labels exclude
private status, and removed or no-longer-visible units release their cached art.
Off-board targeting buttons similarly share a small icon texture at native display
density. Hidden buttons publish no HUD layer; eligibility, positions and click
actions still come from the existing targeting overlay.

Movement and flight arrows, movement envelopes, sensor and objective boundaries,
C3 links, flyover/vector routes, strafing footprints, ruler/LOS indicators,
deployment borders, map-sheet borders, embedded-board indicators, ECM/ECCM tints
and source outlines, artillery drift lines and predicted heat-map fills use native
tactical meshes. Their existing client painters supply immutable vector commands
at base board scale, without allocating sprite images. Stroke dashes, concave
shapes and holes are tessellated, then clipped to the shared terrain triangles,
including road ramps and water surfaces. Geometry is rebuilt when its shapes,
terrain or geometry tuning change, independently of camera motion. Movement costs
and heat-map text use compact cached artwork facing the camera.

The pink visual-range marker (often clipped to the map perimeter) uses the
existing sprite painter's straight segments as translucent upright walls.
`SensorRangeSprite.GPU_VISUAL_RANGE_HEIGHT` sets their height in terrain levels
(default 0.5); `GPU_VISUAL_RANGE_OPACITY` sets opacity from 0 (transparent) to 1
(opaque), defaulting to 0.5. Color comes from `SensorRangeSprite.getColor`,
respecting the existing visual-range color preference. The walls sit
at the hex's surface elevation, including over water and ice, independently of
water depth. At level changes, adjoining panels meet along a shared vertical
span from the lower surface to half a level above the higher surface, as the
firing contours do. They share the tactical mesh cache and movement-playback visibility.
The original thin white dashed stroke follows the top edge of each wall.
`GpuTactical.OUTLINE_SCROLL_SPEED` controls dash travel in unscaled board pixels
per second (default `4f`); `0f` keeps the original static pattern, and negative
values reverse direction. At or below `GpuMarkers.FLAT_TILT_DEGREES` (30 degrees
from overhead), the wall becomes the original flat band on the surface. Both
presentations share the animation clock, palette, opacity and playback visibility;
switching between them and animating the dashes reuse the cached meshes.
`SensorRangeSprite.GPU_RANGE_SHOW_MAP_BORDER` controls whether GPU visual and
sensor ranges also outline the map perimeter. Its default `false` removes only
edges facing off-map hexes; actual range limits within the battlefield remain
visible, including where they reach the map edge. Set it to `true` to restore
the perimeter. The setting applies to both GPU camera presentations and leaves
the classic painter unchanged.

Movement playback hides unit-dependent overlays: sensor rings, movement envelopes,
movement and flight arrows/costs, firing solutions, C3 and attack lines, strafing
footprints, collapse warnings, weapon ranges, and FoV shading. They resume from the
latest snapshot when the last queued move finishes, including landing, unloading,
and formation settling, without waiting for the final completion hold. Pause keeps
this presentation; skipping or instant speed applies the current overlays immediately.
Selection and footprint rings use the same position and facing as the animated
model, including turns, flight and multihex placement.

The Swing adapter captures immutable scene checkpoints in packet order, directly
at movement and resolved-attack callbacks. Checkpoints share the animation queue
and consume no animation time. Terrain, buildings, bridges, legacy bitmap artwork,
cargo, objectives, mines, artillery, engineering progress, ECM and heat-map markings
advance at each arrival or attack impact, before recovery and the completion hold.
Each queued action can therefore reveal its own captured changes. Sensor contacts
and previously visible units use the same checkpoints; a later disappearance or
contact update cannot overwrite an earlier action's appearance. Hidden paths and
sensor-only units never create movement animations.

Rulers, manual LOS cursors, player notes and game commands stay live. This history
is presentation only: game rules and visibility checks remain on Swing, and the
renderer only retains previously authorized snapshots. Board changes discard it;
skip and instant speed apply the latest state. Association uses received packet
order: updates without a corresponding movement or resolved-attack event have no
separate animation boundary, and a state sent only at phase end cannot be split
into outcomes the server did not transmit individually.

Map-sheet borders follow continuous hex edges, with a narrow contrast backing in
the native view. Embedded-board outlines and ECM/ECCM source outlines also have
contrast backing. Field colors, overlap resolution, range, ownership and visibility
still come from `BoardView.updateEcmList`; sources on other boards are excluded.
ECM transparency changes refresh both the field and source immediately. These
updates preserve ground/feature artwork; only changed ECM static needs new raster
pixels. ECCM tint, sheet borders and embedded-board indicators need no raster art.

Field of view uses a shared LOS/sensor classification captured on Swing and a
one-texel-per-hex GPU mask. The classic painter consumes the same classification.
The compositor reconstructs the scene's world positions from its existing opaque
depth, so FoV shading also reaches raised terrain and structures. Tactical lines, labels,
and controls remain readable above it. Transparent water and faded props
use the opaque surface beneath them for this mask, as with the scene depth pass.
Cliff lookups move slightly inward along the reconstructed surface normal, so
depth rounding at a shared hex edge cannot alternate between two visibility
states while orbiting. Visibility contours remain on horizontal surfaces.

The Tuning panel's Field of view controls select `GRAYSCALE` (the default), `DIMMED`,
or `FOG_OF_WAR` and adjust FoV darkness from 0% to 100% immediately in either camera.
All use the same visibility data, respect the existing FoV toggle and opacity,
distinguish sensor range, and draw a contour where visible and blocked areas meet.
`GRAYSCALE` fully desaturates blocked and sensor-only hex content, including its
shading and contour. Darkness scales the existing FoV opacity in all three modes:
0% disables darkening and 100% uses the full configured opacity. Defaults restores
`GpuFieldOfView.STYLE` and `GpuFieldOfView.DARKNESS`. These visual settings belong to
the open GPU window. `FOG_OF_WAR` uses a stronger darkening curve; it does not add
explored-map memory or change which units the game reveals. Stable masks reuse
their GPU texture, including when changing the mode or darkness.
Terrain, unit/turn, selection, phase and LOS-preference changes refresh the shared
result. FoV is excluded from the GPU's per-hex raster artwork.

The per-hex raster compatibility layer remains for engineering terrain previews,
wreck artwork, ECM static noise, diagnostic firing heat-map icons, other legacy
artwork and custom hex-drawing plugins. Converted native shapes are excluded from
that layer, so they are not rendered twice. This does not remove the tactical
layer as a concept or move any game-rule decisions into the GPU renderer.
Detailed dialogs use the Swing client; native chat input
uses its existing editor and sending actions. See [contextual-ui.md](contextual-ui.md).

Time, lighting, weather presets, and their limits are documented in
[gpu-atmosphere.md](gpu-atmosphere.md). Daylight is calibrated for the tileset's
LDR artwork; its postprocessing does not add another filmic contrast curve.
Tactical markings and hex text draw after atmosphere compositing with restored
opaque depth. HEIGHT labels are raised to the building/feature height and fit
on the roof footprint. Other hex labels retain their terrain anchors.

Declared attacks use thin arrows and optional red hex-corner bands at each target
unit's base. Bands mark only the currently selected unit's assigned targets,
including every target in split fire, and clear when no unit is selected.
`GpuFireControl.TARGET_ARROW_SIZE`
scales the line thickness and arrowhead together: `1f` keeps the current size,
`0.5f` halves it, and `2f` doubles it. Set
`GpuBattleView.SHOW_TARGET_MARKERS` to `true` to enable the bands, or `false` to
disable them. Their width and shared-clock bobbing use `TARGET_BAND_WIDTH`,
`TARGET_BOB_PERIOD_SECONDS`, `TARGET_BOB_HEIGHT_OFFSET`, and
`TARGET_BOB_HEIGHT_LEVELS`, initially matching the selection band's values.
`GpuBattleView.HIDE_TARGET_ARROWS_DURING_ATTACKS` defaults to `true`, hiding every
attacker's arrows during combat playback, regardless of selection.
`GpuBattleView.HIDE_TARGET_MARKERS_DURING_ATTACKS` defaults to `false`, keeping
enabled target bands visible for the currently selected unit during playback.
These switches operate independently. Weapon range contours are unaffected.

Firing playback frames the attacker and every target in the current volley,
including unit height and multi-hex footprints. The shared camera pans and zooms
into the board area left clear by the firing or report panel, using the panel's
actual resized width and excluding the top and bottom bars. Both panels reserve
the visible unit-list strip, with the same gap between strip and panel as between
strip and window edge. This follows HUD scaling and report resizing; hiding the
strip releases its space, and camera framing also excludes the strip. Above
`BoardCamera.ATTACK_TOP_VIEW_TILT_DEGREES` (30 degrees from overhead), it also
chooses a nearby orbit along the usable area's long axis and raises very low
viewpoints. At or below that threshold, all automatic framing leaves an already
visible unit or action completely alone. Otherwise it only pans and zooms out as
needed; it never zooms in, tilts or orbits.
`BoardCamera.CAMERA_FRAMING_SECONDS` (0.4 seconds) bounds the shared eased move in
wall-clock time; animated firing waits for it. Late volley targets
share the original deadline. Panel or window changes after the move refit
immediately, and manual camera input takes control.
The world-space orbit pivot stays on the action's support plane. The side-panel
offset is applied separately to the camera, so manual orbit and tilt stay centered
in the clear board area, panning remains on that plane, and pointer zoom stays
anchored under the cursor. Changing panel width repositions the pivot without
moving the displayed board.

`BoardCamera.ANIMATE_CAMERA_ON_SELECTION_CHANGE`,
`BoardCamera.ANIMATE_CAMERA_COMBAT_PLAYBACK`, and
`BoardCamera.ANIMATE_CAMERA_ON_MOVE` all default to `true`. Setting one to `false`
applies that context's framing immediately instead of animating it. Selection
changes preserve the viewing angles and avoid unnecessary zooming in. Movement
checks the start, whole rendered route and destination against the clear board
area before advancing its clock, including the jump arc, height and footprint.
If the route is already visible, there is no camera move or delay. Otherwise,
the camera uses the smallest pan and any required zoom out, preserving its
angles, then stays still throughout travel and its completion hold. Selection
changes received during playback take effect after the final hold. At Instant
speed the queue completes first and the camera snaps once to the final action,
or to a new selection if one arrived during playback. Intermediate actions do
not reposition it, and switching to Instant also settles an existing camera
transition immediately.

Weapon ranges use translucent contours and flat `min`, `S`, `M`, `L`, and `E`
markers at the existing firing-arc handler's positions within each range area.
The flat artwork reuses the classic marker painter at three times terrain resolution,
with antialiasing and a black outline. Each letter stays at its handler-selected hex
and always faces the camera, upright at every tilt and bearing, without spinning.
Its lowest edge clears the terrain or water surface by
`GpuFireControl.RANGE_LABEL_CLEARANCE_LEVELS` elevation levels. Artwork is cached;
camera movement changes only its transform, using the same orientation as flat
location markers. Set
`BoardView.GPU_SCROLLING_RANGE_LABELS` to `true` to replace those markers with
scrolling contour text; it defaults to `false`. The contour ridge heights use
hex surface levels, including above water; water depth does not lower them.
The shared range-color preferences default to red for minimum, green for short,
yellow for medium, orange for long, and purple for extreme. Flat letters,
classic range bands, and GPU contours all use that same palette.
`BoardFiringGeometry.RANGE_HEIGHT` sets height in elevation levels and
`RANGE_CLEARANCE` sets surface clearance in unscaled board pixels.
`GpuFireControl.RANGE_LABEL_SPACES` sets the blank spaces between labels and
`RANGE_SCROLL_SPEED` sets travel in unscaled board pixels per second (zero pauses,
negative reverses). Repeats fit each closed contour so letters cross its seam
continuously. Both camera modes share these meshes and the same animation clock;
the classic board keeps its ground labels.

## Ownership and rendering

- `GpuBoardSource` reads game objects, visibility, tile artwork, and existing
  commands on Swing's event thread and publishes immutable presentation frames.
- `BoardView.capturePlanarHexes` separates ground, flat decals, and remaining
  tactical pixels in bounded chunks. Its compatibility path retains ECM static,
  legacy artwork and plugin painters at three times terrain resolution, and
  restores classic view state. Empty marking images are omitted. Ground has
  limited mip filtering; markings use exact UVs and independent textures.
  Changing a marking does not rebuild terrain geometry. Capture raster scale
  is independent of the classic zoom index, including image-backed ECM noise.
  One reusable chunk buffer and bounded raster caches keep the working set
  independent of board area. Identical immutable images share pixel storage
  and atlas slots; diverging images split, and matching images merge again.
  Repaints, board changes and camera coverage invalidate tactical capture;
  unchanged frames reuse it, and offscreen markings are released.
- `BoardView.captureTacticalGeometry` records the converted painters as immutable
  vectors. `GpuTactical` renders these as terrain-following meshes shared by both
  cameras. Ruler lines, endpoint crosshairs and LOS hex outlines use this path;
  changing or clearing them does not repaint their artwork into raster tiles.
- `BoardGeometry` owns dimensions and terrain picking; `BoardSurface` defines
  the physical roads, banks, beds and exposed sides.
- `GpuAssets` owns shared feature meshes, repeating textures, and water frames.
- `GpuTerrain` batches terrain and opaque features in 16 by 16 chunks. Changes to
  a tile's height/material/features rebuild its chunk and affected neighbors;
  atlas-layout, board-size, floor, and tuning changes rebuild the required
  scene. Pixel-only changes update texture slots; ground color changes also
  rebuild their chunk to refresh rim tints. Camera changes reuse meshes.
  Opaque batches use tightly sized meshes and release mesh-builder scratch
  storage after each chunk. Label glyph geometry shares the font atlas, is
  cached by chunk/elevation, and refreshes when labels or geometry tuning change.
- Chunk bounds cull offscreen terrain and include water and feature heights.
  Unit bounds first reject unrelated chunks before testing feature occupancy.
  Pointer picking rejects unrelated chunks before running the shared surface
  and authored-mesh intersection code.
- The draw order is opaque terrain/features, flat decals, units, transparent
  water/faded features, tactical marks, and screen annotations/UI.
  Converted tactical geometry follows the shared terrain triangles, including
  road approaches and water surfaces. Remaining raster marks use a flat plane
  one-third of a level above each hex's surface. Both paths test opaque depth
  without writing it, so buildings and higher terrain occlude them. Border
  colors and existing translucent fills are retained.
- One 2048-pixel directional shadow map includes terrain, opaque features, and
  units. Its coverage follows the camera's visible receivers, keeping closeup
  detail independent of map size while including offscreen shadow casters.
  The light grid aligns to texels to stabilize panning. Geometry, lighting,
  occupancy, unit transforms and camera changes invalidate the cached map.
- GL resources are created and disposed on the render thread. Shared assets own
  their textures; instance material changes do not transfer ownership.
- `GpuBoardActions` adapts real phase buttons, menus, weapon lists, and ammunition
  models. Callbacks return to Swing and recheck the panel, phase, turn, actor,
  target, and current availability. There is no second rules engine.

## Verification

`GpuMarkersTest` checks camera alignment, stationary overhead views, vertical-axis
spinning, scale-independent roof clearance, and warning capture/removal without
duplicate flat glyphs. `GpuMarkersSmokeTest` renders the shipped building/bridge
assets, checks actual mesh clearance and each type's configured occlusion highlighting, and
writes `markers-isometric.png`, `markers-top.png`, and `markers-occluded.png`.
It also captures both camera modes through the complete `GpuBattleView` renderer
in `markers-board-isometric.png` and `markers-board-top.png`.
The same native test renders all kinds in `markers-gallery-top.png` and
`markers-gallery-isometric.png`. Sensor/collapse/artillery/note stacks appear in
`markers-stack-top.png` and `markers-stack-isometric.png`; the corresponding
Atlas standing on a roof appears in `markers-unit-stack-top.png` and
`markers-unit-stack-isometric.png`. These full-renderer checks verify that
location symbols clear the current unit geometry and stay inside their original
hex. Contact centring and relative size are also checked while warnings are added
and removed. Pure geometry checks cover every stack size up to twenty symbols,
both camera modes, twelve bearings and three board scales.
`GpuMarkerCaptureTest` covers handler visibility, owner colours, mine/charge privacy,
special-display filtering, engineering terrain preservation, classic sprite
restoration, and the separate orbital blast footprint.
`GpuSceneSourceTest` checks captures between batched movement/attack packets and
sensor-only updates. `GpuScenePlaybackTest` checks per-arrival and per-impact
terrain, bitmap, marker and contact changes, including pause, skip and board reset.
`GpuSelectionPlaybackTest` checks translated and rotated footprint geometry;
`GpuScenePlaybackSmokeTest` verifies the complete renderer in both camera views.

Run from the checkout root:

```text
.\gradlew.bat :megamek:test --tests "megamek.client.ui.clientGUI.boardview.gpu.*Test" --offline
.\gradlew.bat :megamek:gpuBoardSmoke --offline
.\gradlew.bat :megamek:gpuBoardLargeMapTest --offline
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
families and all 24 other models are loaded natively; GIF frames must advance.
`mm-data/tools/validate_board_assets.py` checks every exported model's budget,
indices, roof winding/pixel fidelity, and independent texture dependencies.

`gpuBoardLargeMapTest` captures and renders 40,000 hexes with a 512 MiB Java heap,
using the shipped Grassland 2 terrain repeated into a 200 by 200 board. It checks
full-resolution artwork, exact image sharing, marking eviction, board edits,
both camera views and offscreen chunk culling. It records overview/closeup
frame times and screenshots without coverage instrumentation. This constrains
the Java heap, not total process memory or GPU memory; results depend on the
board content, hardware and display settings.

`GpuAssetCatalogSmokeTest` renders actual Saxarba ground with the authored
catalog, connected buildings at unequal heights, snow trees, bridges, ice,
water depths, road level changes, and material-specific exposed walls. Images and measured frame
times are written to `megamek/build/gpu-board-review/`.
`GpuBuildingMaterialsSmokeTest` renders construction strengths, fortress and
hangar classes, circular fuel-tank roofs, and industrial structures selected by
the real tileset. It verifies facade families, shared texture ownership, repeat
wrapping, and 128 by 128 building/geology uploads, and captures facade closeups.
`GpuTacticalSmokeTest` captures friendly and enemy deployment ECM and a distance
measurement outline over elevated terrain. `GpuMeasurementTest` checks the shared
Alt/Ctrl gestures, phase-independent measurement, native endpoint updates and
clearing. `GpuMeasurementSmokeTest` checks native picking and modifier routing
in both cameras and captures `ruler-los-top.png` and `ruler-los-isometric.png`.
`GpuFieldOfViewTest` covers ridge occlusion, terrain edits, report phases, settings
and clearing the observer. `GpuFieldOfViewSmokeTest` compares both native styles,
checks mask reuse and visible/blocked pixel brightness, and captures matching
`fov-dimmed-*` and `fov-fog_of_war-*` views.
`GpuFieldOfViewCliffSmokeTest` compares cliff pixels against uniformly visible and
blocked references across a full orbit, including small angle changes, and captures
`fov-cliff-visible.png` and `fov-cliff-blocked.png`.
`GpuHexOverlayTest` covers sheet seams, embedded boards, terrain-following fills,
ECM/ECCM mode/opacity/color updates, hidden and other-board emitters, snapshot
immutability and terrain-art reuse. `GpuHexOverlaySmokeTest` renders both cameras,
checks mesh reuse and clearing, and captures `hex-overlays-*` views.
`GpuRiverSmokeTest` captures straight/bent channels, textured elevation drops,
mixed woodland, snow variants, low rubble and a zero-elevation textured bridge
over the riverbank in both camera views. A pixel sample on the vertical waterfall
verifies that the fall itself animates.
`GpuTerrainRimTest` checks fixed texel scale on sloping and clipped rims at
three board scales. `GpuTerrainMaterialsSmokeTest` renders all six 128 by 128
rim materials with the actual Saxarba themes and shallow/deep water. It also
checks that a ground-pixel change refreshes a rendered rim's color without an
atlas layout change. Its screenshots are named `terrain-*-rim.png`.

## Scope and limits

This is a 3D board renderer with native contextual controls. Shared Java2D
painters still supply tactical pixels, annotations, and existing HUD widgets.
Their refresh and initial image loading can still stall presentation. Tactical
capture is polled on Swing; it is not an entirely native vector HUD. Roof
illustrations retain their original resolution and baked rooftop detail.
Updating a copied roof silhouette requires rerunning the offline asset build.

Transparency uses bounding boxes and normal
alpha sorting, not volumetric water or order-independent transparency. Terrain
still hides the normal unit material behind banks; the separate see-through
highlight reveals the occluded silhouette, and screen annotations remain available.

The automated checks do not establish an end-to-end human playthrough of every
aerospace, artillery, transport, multi-map, bridge, or special-equipment
workflow. Windows desktop OpenGL has been exercised; other platforms and
physical monitor transitions need validation. Measured frame times are from
a capped/vsync workload and establish neither an uncapped performance limit
nor a speedup over the classic renderer.

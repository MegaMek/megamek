# GPU cloud shadows and wet terrain

Use **Tuning → Daylight & atmosphere → Cloud cover**. Broken cover around 0.55–0.7
shows moving sunlight and shade patches. The backdrop is a smooth blend of solid
sky/horizon colors. Cover adds a cloud-colored veil strongest overhead, fading
toward the horizon during day, twilight and night. Dawn and dusk have warm horizons
and cooler upper skies; airless worlds retain a dark background. There are no rendered sky
clouds, cloud-view shader, cloud-view buffer or cloud compositing texture.

The battle clock and weather stay fixed unless the user or scenario changes them;
there is no automatic weather transition, accumulation or drying simulation.
The initial clock is a random quarter-hour within the planetary lighting window
(day 08:00–17:00, DUSK_DAWN 06:00–06:45 or 17:15–18:00, night 20:00–04:00). The GPU window retains
that choice through refreshes and weather changes; Time of day overrides it.

## Controls and fixed settings

Cloud shadows always use the former **Low** budget: a 192×192 transmission atlas
with 12 samples through the layer. The optical density multiplier is fixed at
**0.25**. Quality and density choices have been removed from the tuning panel.

**Tuning → Cloud light and rays** contains:

| Control | Default | Effect |
| --- | --- | --- |
| God rays | 0.5 | Air/shaft density multiplier, 0–2; zero disables shafts |
| Cloud shadow min | 0.20 | Cloud-patch opacity limit near zero cover, 0–1 |
| Cloud shadow max | 0.85 | Cloud-patch opacity limit at full cover, minimum–1 |

`GpuClouds.MIN_SHADOW_STRENGTH` and `MAX_SHADOW_STRENGTH` define the defaults.
For nonzero cover, strength interpolates linearly between these limits; zero cover
has no cloud shadows. Transmission is `1 - strength * (1 - exp(-opticalDepth))`.
This preserves density, the footprint and clear openings while making sparse-cover
patches lighter. The atlas supplies this same transmission to surfaces and shafts.
Raising the minimum above the maximum raises the maximum too, and the panel displays
the effective range. Defaults restores both limits. There is no extra texture or pass.

Wetness always follows liquid rain in appropriate pressure/temperature. There is
no automatic/manual choice or separate wetness slider.

Cloud cover, rain, snow, fog, haze, time and wind remain in their existing sections.
Cloud cover preserves the selected wind strength, including zero. Cloud motion
linearly maps wind strength **0–1** to a speed factor of **0.3–1**, so calm clouds
still drift and every increase in wind strength increases their speed.
An unresolved scenario direction uses the existing visual direction of 0°
for this drift; no random game roll is performed.

Defaults restores rays at 0.5, shadow limits at 0.20 / 0.85 and the opening atmosphere;
wetness follows its rain.
These settings belong to the GPU window, are not persisted, and never alter
game rules, visibility, terrain, orders or planetary conditions.

**Daylight & atmosphere → Fixed sun/moon** is off by default. Enabling it keeps
the light at the same screen direction through camera movement, including tilt.
The existing geometry light, cloud transmission and rays use its resulting world
direction together. Defaults turns this option off. Full Moon is the default
night preview; explicit Moonless/Pitch Black conditions have no moon source and
skip geometry/cloud shadow passes. Weather presets preserve this distinction.

## Conditions

The immutable atmosphere snapshot includes actual pressure and temperature.
Space/vacuum disables clouds, fog, haze, precipitation, wind and liquid wetness,
including manual previews. Airless skies are dark. Trace air also disables clouds;
thin air permits lighter cloud shade but no precipitation, fog or wetness.
Lunar artwork does not override an explicitly atmospheric scenario.

The procedural layer still shapes its shadow footprint from weather and pressure.
Snow, fog and heavy cover flatten it; rain, hail and lightning deepen it. Higher
pressure increases optical depth. These are artistic mappings of supplied
conditions, not a humidity, condensation or planetary chemistry simulation.

Liquid rain sets wetness immediately above 0°C. Freezing conditions disable
liquid wetness. Snow, frozen hexes and existing water
surfaces are excluded from wet-ground shading. Concrete and rock have a stronger
sun sheen than porous sand, dirt or grass. The existing ground shader darkens
albedo and adds a view-dependent highlight, respecting normal maps, geometry
shadows and cloud transmission. There is no additional wetness rendering pass,
puddle reflection, wet-building effect or gameplay terrain change.

Live scenario fields follow through unless their visual controls were overridden.
Pressure and temperature always follow the snapshot. Cloud displacement is integrated, including
west/south motion across the periodic noise boundary; changing wind does not
teleport shadows. Cameras share the timeline and scene.

## Lighting and rendering

`GpuAtmosphere` lazily owns `GpuClouds`, its transmission shader, noise texture
and fixed atlas. The screen quad and environment's texture/projection are borrowed.
Steady frames and viewport changes reuse the atlas. Owned resources are disposed
with the view.

A repeatable 256-square R/G texture supplies continuous three-dimensional noise.
`cloud-transmission.frag` contains the density calculation and its fixed 12-step
Beer–Lambert integral. Coverage maintains openings through the layer; integer
octave noise and boundary erosion shape shadow patches and preserve the wind
wrapping period. There is no cloud-view march, self-lighting or multiple-scattering
approximation.

Terrain and units sample the atlas at their actual positions. With world-space
lighting, camera movement leaves the projection unchanged; Fixed sun/moon updates
it to follow the shared light. It attenuates direct diffuse/specular light, retaining ambient
and emissive light. Open columns receive clear-sky incident light. Cloud cover
does not also dim global surface lighting or change its saturation. Wind does not
invalidate terrain meshes or the native 2048-square geometry shadow map.

Daylight shafts use a fixed 12-step integration at quarter width/height, combining
cloud and native geometry shadows. Camera depth stops them at opaque surfaces.
Scattering is strongest when looking toward the sun; haze makes it more visible.
Fog, haze and shafts share the 25% opacity ceiling and depth-aware upsampling.
Fog/haze keep their original height range independently of the cloud layer;
raising the shaft ceiling no longer adds an extra column of haze. Capped pixels
skip shaft integration because its remaining opacity budget is zero.
An inactive sun, absent clouds, or a zero God rays setting disables sunlight shafts.
Dawn/dusk use the same low sun for geometry shadows, cloud transmission and rays;
sky brightness is not used to infer the active source. The sun's direct RGB already
contains its visibility fade, so shafts do not apply a second daylight-strength fade.

Scene color and hardware depth are captured together. The composite reuses that
depth for weather and tactical overlays, removing both the separate camera-depth
geometry pass and the depth-restoration draw. See the
[weather performance audit](gpu-weather-performance.md) for pass counts and budgets.

## Verification and limits

`GpuAtmosphereSmokeTest` checks moving light/shade patches with a steady, smooth
background; transmission on ordinary ground, normal-mapped ground and units;
light through openings even at full coverage; ray controls; GLSL angular scattering;
negative wind wrapping; camera independence with world-space lighting and shared
camera-relative projections with Fixed sun/moon; full-moon/moonless/pitch-black nights, storm and vacuum;
rain-driven wet/dry/snow surfaces; Defaults and visual overrides; the removed controls;
preserved wind controls with cloud cover; viewport changes; and fixed-atlas reuse.
Unit tests cover minimum cloud drift and wind-speed scaling, scenario ownership, cloud/time mapping,
temperature/pressure exclusions, and unchanged clear-day lighting.

The twilight/cloud-cover update passed 31 regular tests (`BoardAtmosphereTest`,
`GpuCloudsTest`, `GpuScenarioAtmosphereTest`), six native rendering/UI tests
(`GpuAtmosphereSmokeTest`, `GpuPlanetaryConditionsSmokeTest`, `GpuTerrainNormalsSmokeTest`,
`GpuFieldOfViewEffectsSmokeTest`), and main-source Checkstyle.
These verify warm ground/horizons throughout both twilight windows, continuous
sky colors, cover tint fading toward the horizon, actual atlas opacity scaling,
fixed-atlas reuse, tuning overrides/range/Defaults, and existing lighting/weather behavior.
The shipped-board dawn, dusk, night, overcast and live-panel captures were visually inspected.
No FPS improvement is claimed; the update retains the existing render passes,
cloud resolution, sample count and density.

Captures and reports are written to `megamek/build/gpu-board-review/`:
`clouds-*.png`, `terrain-wet.png`, `atmosphere-*.png`, `atmosphere-timing.txt`
and `cloud-shadow-timing.txt`. The larger fixture uses a 64×64 synthetic board
and 64 simple lit shadow casters at 1280×800, with 15 warm-up frames and 30 measured
frames per case, ending each frame with `glFinish`. It includes both cameras,
clear weather, broken cover and overcast. It measures total fixture render time,
not isolated GPU pass cost or whole-game FPS, and excludes UI, streaming and complex units.

The pipeline remains RGBA8/LDR. There is no HDR bloom, temporal reconstruction,
local sky occlusion, spherical planetary sky or full PBR material system.
The fixed atlas can lose fine detail on large boards or at shallow light angles.
Night ambient fill and cloud altitude/scale are adapted for a readable tactical
board. Wetness uses material families, not per-pixel roughness or puddle masks.

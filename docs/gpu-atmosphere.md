# GPU daylight and atmosphere

Open **Tuning** (F9) in the GPU battle view and use **Daylight & atmosphere**.
The panel scrolls when the available space is small; Defaults stays visible.
Opening a board initializes the preview from the scenario's effective planetary
conditions. **Defaults** restores that starting atmosphere, including precipitation
and wind, along with the geometry defaults. Ordinary frame updates preserve manual
preview changes; opening another board captures that board's starting conditions.

| Control | Visual effect |
| --- | --- |
| Time of day | 24-hour clock in 15-minute steps; 00:00 and 24:00 both mean midnight |
| Clear / Overcast / Mist / Fog | Set cloud cover, ground fog, fog height, and haze; disable precipitation, blowing sand, and lightning; preserve clock, exposure, and wind |
| Cloud cover | Reduce direct sunlight and shadow contrast; maximum now equals the former 0.6 setting so shadows remain visible |
| Ground fog | Change the density of height-dependent fog |
| Fog height | Set its vertical falloff in terrain levels (minimum 1.0), starting at the board's lowest hex LEVEL |
| Haze | Add more uniform atmospheric extinction |
| Exposure (EV) | Brighten or darken the scene by up to two stops; neutral daylight includes a +0.8-stop lift, fading to a 1.5-stop reduction at night |
| Rain / Snow / Hail / Blowing sand / Lightning | Buttons toggle each effect; adjacent sliders adjust intensity (zero is off). Turning an effect back on sets it to half strength |
| Wind strength / Wind direction | Set particle drift; blowing sand maintains a fast base flow even at zero wind, and stronger wind accelerates it. Direction is clockwise from north, toward where particles travel |
| Defaults | Restore geometry defaults and the scenario atmosphere captured when this board opened |

Settings are visual previews owned by the GPU window. They do not update the
game's planetary conditions, line of sight, visibility, or orders, and are not
persisted across window reopenings. Both camera presets and free orbit use the
same settings, geometry, and unit animation.

## Scenario condition coverage

Scenario lighting stores categories, not an exact time or astronomical date.
The clock values below are representative visual starting points.

| Scenario condition | Initial visualization |
| --- | --- |
| Day | 13:00 daylight |
| Dusk | 18:00 twilight |
| Full moon | 00:00 with a bright blue moonlit fill |
| Moonless / Pitch black | 00:00 with slightly reduced exposure; retain the board's readability floor |
| Glare / Solar flare | 13:00 with slightly increased exposure |
| Clear | No cloud cover or precipitation; independently configured fog/sand still apply |
| Light / Moderate / Heavy / Gusting rain / Downpour | Increasing rain intensity; heavy and gusting rain share density, with actual scenario wind determining drift |
| Light / Moderate / Flurries / Heavy snow | Increasing snowfall intensity; actual scenario wind determines drift |
| Sleet | Mixed rain and snow |
| Ice storm | Mixed rain and ice pellets |
| Light / Heavy hail | Increasing ice-pellet intensity; heavy hail uses the maximum slider value |
| Lightning storm | Rain and a visible illumination strike within half a second of enabling; repeat at seven-second intervals with a short attack and longer decay |
| None / Light / Heavy fog | None disables ground fog. Light uses 0.2 ground fog/haze; Heavy uses 1.0 for both. Both share the same height falloff and opacity cap; blowing sand can independently add haze. Light fog also lifts cloud cover to 0.15 and Heavy to 0.3, as haze implies a duller sky |
| Blowing sand | Fine, fast gusts of sand close to the ground and warm haze, only when the game's `isBlowingSandActive()` says it is effective |
| Calm / Light gale / Moderate gale / Strong gale / Storm / Tornado F1–F3 / Tornado F4 | Increasing wind drift, capped at storm-scale visual motion for tornadoes; no tornado funnel geometry |
| Six wind directions / Random | Use the game's resolved direction; unresolved Random adds no invented wind direction or random game rolls |
| Vacuum / Trace / Thin / Standard / High / Very high pressure | Vacuum disables atmospheric weather and wind. Trace and thin suppress precipitation and fog, matching the scenario editor. Fog height is 2.0 levels at Standard, 1.5 at High, and 1.0 at Very High |
| Space and high-altitude space boards | Suppress atmospheric precipitation, clouds, fog, haze, and wind across the board |
| Temperature / Gravity / Atmospheric taint / EMI | No invented screen tint or distortion; existing game rules and tactical indicators remain authoritative |
| Terrain affected / Wind-shift flags | Consume the game's resulting terrain and effective wind; the renderer does not simulate accumulation, freezing, wind rolls, or terrain changes |

The Swing adapter reads these conditions and publishes immutable visual settings
in its frame snapshot. The render thread never reads or mutates the live
`PlanetaryConditions` object. Scenario light and weather are independent: a clear
night remains clear, and a rainy night remains moonlit and readable.

Heavy Fog uses dense, low-lying mist with haze above the ground layer; Light Fog
uses a gentler version. Scenario lighting, exposure, precipitation, and wind
remain independent of fog, while fog adds a little cloud cover of its own. For
lower morning mist, set Time of day to 09:30,
Exposure to -0.6 EV, and Fog height to 1.5 in Tuning. The manual Mist/Fog
preview buttons keep their separate weather settings; Defaults restores the
scenario's fog appearance.

Pressure reduces scenario fog height by 0.5 levels per step above Standard,
clamped to `BoardAtmosphere.MIN_FOG_HEIGHT` (1.0). This is a visual tuning rule;
it does not change fog density or game rules. The same minimum applies to Tuning.

Fog, haze, rain, snow, hail, and blowing sand share the lowest hex **LEVEL** as
their atmospheric baseline. Water depth, riverbed recesses, and other terrain
depth do not lower it. A map whose lowest surface is level 4 starts its weather
at level 4; negative surface levels work the same way. The particle volume extends
above the highest terrain and features, with opaque terrain hiding particles behind it.

Fog and haze share a **40% maximum opacity**, including at maximum slider values.
Their sliders control density, not opacity: nearby or elevated surfaces can still
receive less fog at full density. At least 60% of scene color is retained before
color grading. The Tuning panel shows the shared cap. The Clear preset
sets cloud cover and both densities to zero and bypasses the depth and fog passes. Night uses a
bright full-moon fill with a blue tint; diffuse twilight fill prevents a dark dip
as directional sunlight and moonlight exchange positions.

## Rendering

`BoardAtmosphere` derives sun/moon direction, direct and ambient colors, fog and
background colors, tint, and saturation from one immutable settings record.
Morning and evening cast shadows in opposite directions, low sunlight casts
longer shadows, and night retains a bright cool ambient fill and moonlight.

`GpuTerrain` applies this lighting to terrain, authored features, and units.
Its existing directional shadow map is reused when controls change and is
refreshed when its light or geometry changes. Opaque geometry is shared between
the shadow and camera-depth passes, including the current animated unit poses.

`GpuAtmosphere` captures scene color. Only when fog or haze is enabled does it
also capture packed camera depth. Fog is evaluated at quarter resolution using
the analytic integral of exponential height density, stopping at opaque geometry.
This replaces the original 24-step noise march. Fog uses no shadow-map lookups.
The coarse Light shafts approximation and its tuning control have been removed;
fog retains its height falloff, haze, and day/night color.
Depth-weighted upsampling reduces bleeding across silhouettes. The backdrop is
identified using scene alpha, so clear weather needs no packed camera-depth pass.

The final pass applies fog transmission and scattering, exposure, color tint,
saturation, and a subtle vignette. Daylight is calibrated for the existing LDR
tileset; there is no additional filmic curve to amplify its baked contrast.
Neutral exposure lifts daylight by 0.8 stops, and night sits 1.5 stops below that neutral exposure.
Cloud cover maps the full control range to 0–60% of the original cloud response,
retaining at least 47% of the clear-sky directional light even at maximum.
The backdrop uses a day/night gradient. A color-masked opaque pass restores
depth after compositing so tactical markings, roof/hex text, and inspection
outlines draw sharply afterward with correct occlusion. Screen annotations
and Scene2D controls then draw in screen space.
Buffers resize with the board viewport and are disposed with the GPU view.

Precipitation uses a lazily allocated, shared pool of 15,360 GPU-animated quads,
drawn in at most four calls. Maximum rain and snow each draw 4,608 particles,
hail draws 3,072, and blowing sand draws all 15,360. Sand density scales linearly,
so the default half-strength setting already draws 7,680 grains.
A curved intensity response keeps light precipitation gentle and makes downpour,
heavy snow, and heavy hail reach full density. Rain uses fine streaks; snowflakes
are 55% of their original diameter and fall about 71% faster with less sideways sway.
Rain has a 64% peak opacity and hail uses faster pellets. Sand uses small grains
with an 85% peak opacity and short trails aligned with their instantaneous motion.
Dark brown centers and pale lit edges provide contrast over desert textures and shadows.
Its base speed ranges from 14 to 34 terrain levels per second with the wind slider;
individual speed variation and shared crosswind gusts break up uniform movement.
Most grains skim the lower weather layer with shallow, rapid hops, while a sparse
layer travels higher. Grains retain a minimum filtered screen footprint and opacity when zoomed out,
and particles fade at the weather volume's edges before wrapping.
They draw against the restored opaque scene depth before tactical markings,
so roofs occlude particles and UI remains sharp. Disabled effects issue no particle
draw calls and require no additional depth framebuffer. The weather volume is
bounded by the board and camera footprint; changing its extent can redistribute
particles. There are no physical splashes, surface wetness, snow accumulation,
lightning bolt meshes, or per-building weather simulation.

## Verification and limits

`BoardAtmosphereTest` covers the daily light cycle, clock wrapping, finite light
directions, every scenario light/weather category, pressure and space exclusions,
effective blowing sand, and shader input bounds. `GpuScenarioAtmosphereTest`
checks publication and immutable ownership across the Swing/render boundary,
including switching between Heavy Fog, Light Fog, and None without changing lighting
or precipitation. `BoardGeometryTest` checks the shared LEVEL-based weather baseline;
`GpuWeatherBaselineSmokeTest` compares actual rain, snow, hail, and sand frames at
levels -3, 0, and 4, verifying that adding depth-20 water does not move the particles.
`GpuAtmosphereSmokeTest`
exercises actual shaders, fog at different heights, contrast under maximum fog,
bright moonlight and twilight, shadow resource reuse, live controls, and resizing.
It also checks particle visibility, animation, immediate removal, opaque-depth
occlusion, scenario initialization, Defaults, rain/snow toggle input, and prompt,
visible lightning that stops immediately when disabled. A fixed-time sand render
checks visible fine grains at half strength and increasing coverage at 60% and
full strength without blanketing the scene, independently of haze and sky grading.
Isolated grain renders measure small footprints, fast motion at zero wind,
acceleration with stronger wind, and travel in all four cardinal directions.
Desert contrast checks use shipped HQ and beige sand hex textures in both camera
presets and at a wider zoom, counting visible grains only over the board interior.
It writes `atmosphere-*.png` review images and `atmosphere-timing.txt` to
`megamek/build/gpu-board-review/`. Timing compares clear weather, maximum fog/haze,
each maximum precipitation type, and all precipitation types together on the same
warmed synthetic board, using GL
completion rather than vsync timing. It does not establish full-game performance.

The renderer uses RGBA8 scene color and fog buffers, one board-wide 2048-pixel
shadow map, and no temporal accumulation. It is not an HDR/PBR renderer or a
physical astronomical simulation. Transparent water and faded features use the
opaque depth behind them for fog; tactical textures and hex text bypass
scene grading. Per-hex illumination and desaturation still come from the classic
tactical capture. Cloud cover changes lighting rather than drawing cloud meshes.
Large boards and shallow light angles retain the shadow map's resolution and bias limits.

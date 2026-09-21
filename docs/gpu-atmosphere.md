# GPU daylight and atmosphere

Open **Tuning** (F9) and select **Atmosphere** in the GPU battle view. Atmosphere controls are grouped into
**Atmosphere presets**, **Lighting**, **Planet properties**, **Clouds and ground air**,
**Weather effects**, and **Light and fog effects**.
The panel scrolls when the available space is small; Defaults stays visible.
Opening a board initializes the preview from the scenario's effective planetary
conditions. **Defaults** restores the game's current conditions, including precipitation
and wind, along with the geometry defaults. Changed scenario fields follow the game
unless that visual control has been overridden, including pressure, temperature,
gravity, moonlight and taint type. Selecting a complete preset or applying the
conditions dialog pins that visual selection until another selection or board
change. Opening another board captures its starting conditions.

| Control | Visual effect |
| --- | --- |
| Time of day | Starts at a random quarter-hour within the planetary lighting window; the slider overrides it. 00:00 and 24:00 both mean midnight |
| Fixed sun/moon | Off by default. Also available in the Camera menu, synchronized with this checkbox. Keep the source at the same screen direction through camera rotation, tilt, pan and zoom; terrain, units, cloud shadows and rays share that direction |
| Atmosphere presets | Complete previews of real planetary-condition combinations; replace the visual atmosphere and reset the extra atmosphere controls to their constants |
| Planetary conditions... | Opens the currently selected visual conditions; Apply always reapplies every derived value and resets the extra atmosphere controls, even for unchanged conditions. Cancel preserves all tuning |
| Moonlight at night | Enable/disable the directional night source, independently of clock and exposure; daytime sunlight remains available |
| Air pressure | Preview Vacuum / Trace / Thin / Standard / High / Very High air, including its restrictions on clouds and weather |
| Gravity (g) | Preview the gravity driving new jump animations, including their height and timing; already airborne jumps finish their current arc. Game movement paths and rules remain unchanged |
| Temperature (C) | Preview the temperature controlling rain wetness; 0 C and below suppress liquid wetness. Starts at the scenario value |
| Atmospheric taint | Preview the breathable, caustic, poisonous/radiological or flammable palette and severity; game rules stay unchanged |
| Cloud cover | Move patches of sunlight/shade and depth-limited sun shafts; increase cloud-patch opacity and add a cloud-colored sky veil that fades from top to bottom; rain, snow, fog and pressure shape the shadow field |
| Ground fog | Change the density of height-dependent fog |
| Ground layer height | Shared fog and sand vertical falloff in terrain levels (1–8), starting at the board's lowest hex LEVEL |
| Fog height variation | Maximum local deviation from that height, in terrain levels (0–4); default 1.25. Zero gives constant height |
| Fog density variation | Thin the spaces between fog banks (0–1); default 0.85. Zero gives uniform density, one permits clear gaps; the 25% opacity cap remains |
| Fog calm drift | Default from `GpuAtmosphere.FOG_CALM_DRIFT = 0.06f` hex widths/second; gentle intrinsic movement when wind is calm. Zero disables calm drift; wind still moves the fog |
| Haze | Add more uniform atmospheric extinction |
| Exposure (EV) | Brighten or darken the scene by up to two stops; neutral daylight includes a +0.8-stop lift, fading to -0.4 EV at night |
| Rain / Snow / Hail / Blowing sand / Lightning | Buttons toggle each effect; adjacent sliders adjust intensity (zero is off). Turning an effect back on sets it to half strength |
| Wind strength / Wind direction | Move fog banks, sand, cloud shadows and precipitation. Fog, clouds and sand retain gentle motion when calm. Direction is clockwise from north, toward where weather travels |
| God rays | Default 0.5; scale shaft density from 0 to 2; zero disables the shaft pass when ordinary fog/haze is also off |
| Sun glare | Default 0.35, range 0–1; bright golden glare, a warm veil and soft lens reflections when facing the sun, strongest near dawn/dusk. Zero disables it. This lens effect remains available in vacuum |
| Moon shadow contrast | Default from `BoardAtmosphere.MOONLIGHT_SHADOW_CONTRAST`, range 0–1; strengthen Full Moon shadows while preserving lit level-ground RGB. Zero restores the original moonlight balance; disabled when moonlight is off |
| Taint strength | Default 1, range 0–2; scale the selected taint's sky/horizon and existing fog/haze palette. Zero disables the tint; disabled for breathable air and vacuum |
| Cloud shadow min / max | Defaults from `GpuClouds.MIN_SHADOW_STRENGTH` / `MAX_SHADOW_STRENGTH`; interpolate the cloud-patch opacity cap from sparse cover to full overcast, without changing density or ambient light |
| Defaults | Return to the current game conditions and restore geometry and all extra visual controls to their constants |

Cloud shadows use fixed Low quality (192-square atlas, 12 samples) and a density
multiplier of 0.25. There are no quality/density choices or rendered sky clouds.
Ground wetness always follows liquid rain. It has no checkbox or manual slider;
freezing and very thin/airless conditions suppress it.

Settings are visual previews owned by the GPU window. They do not update the
game's planetary conditions, line of sight, visibility, or orders, and are not
persisted across window reopenings. Both camera presets and free orbit use the
same settings, geometry, and unit animation.
The clock and weather do not advance or blend automatically during a battle.
Cloud advection, fog banks, wind-driven sand, particles and lightning still animate.
Movement acceleration is no longer a tuning control. Playback always uses
`UnitMotion.DEFAULT_SPEED_GAIN_PER_HEX = .03`.

## Planetary-condition presets

Preset buttons build private `PlanetaryConditions` values and use the same
`BoardAtmosphere.fromScenario` mapping as the game snapshot and conditions editor.
There is no second set of preset cloud/fog/density rules. Each button replaces
all scenario-derived visual inputs, including time, exposure, air properties and
weather and gravity. It resets the extra visual-effect sliders to their constant
defaults, including shadow contrast, glare, rays, fog variations, calm drift, taint
strength and Fixed sun/moon. These sliders remain available for subsequent tuning.
Geometry, visibility and damage controls are separate; only Defaults resets them.
No action here changes the game's planetary conditions.
The window's existing time sample is reused, so clicking a preset again does not
reroll its clock. Dawn and Dusk restrict that sample to their respective windows.

| Preset | Planetary-condition combination |
| --- | --- |
| Clear day | Day, clear weather, no fog, calm wind, standard breathable air |
| Dawn / Dusk | Clear weather with `DUSK_DAWN`, restricted to the chosen half of the lighting window |
| Full moon | Clear night, directional moonlight, neutral exposure compensation |
| Moonless | Clear night, no moonlight, -0.6 EV |
| Pitch black | Clear night, no moonlight, -1.0 EV |
| Light fog / Heavy fog | Clear day with the actual Light/Heavy Fog conditions and their derived cloud cover, fog, haze and height |
| Rainstorm | Day, Lightning Storm weather and its required moderate gale; liquid rain wets the ground |
| Snowstorm | Day, Heavy Snow, storm wind and the game's weather-derived -50 C temperature |
| Sandstorm | Clear day, active Blowing Sand, storm wind, 40 C |
| Lunar day | Clear day in vacuum at 0.16 g, with no atmospheric clouds, weather or haze; lighter-gravity jump animation |

All presets use standard breathable air unless specified otherwise. Sliders,
the moonlight checkbox and the air-property selectors then allow individual
visual overrides. Moonless/Pitch Black differ through the existing **Moonlight at
night** and **Exposure (EV)** variables; no separate copy of the scenario light
classification is maintained in the tuning panel.

The conditions editor retains the latest preset or accepted conditions, including
properties such as gravity. Manual sliders are visual overrides of that selection;
they do not invent a new weather classification from arbitrary mixtures of rain,
snow or fog. Apply discards those overrides even when the conditions themselves
have not changed. Defaults also restores the editor to the game's current
conditions. A selected Dawn or Dusk keeps its respective clock window when
reapplying the same conditions.

## Scenario condition coverage

Atmospheric taint is copied into the immutable visual snapshot from the scenario.
Manual weather controls preserve it; the **Atmospheric taint** selector can
override its type. Changed scenario taint follows the game until overridden;
the local **Taint strength** multiplier remains independent.
Defaults restores the game's current atmosphere and a multiplier of 1.

Caustic air uses pale sulfur/olive (`CAUSTIC_TAINT_COLOR`), radiological/poisonous
air muted grey-violet (`POISON_TAINT_COLOR`), and flammable air copper/amber
(`FLAMMABLE_TAINT_COLOR`). These are artistic cues, not a claim about a gas's
chemical color, density, or emissions. Breathable air keeps the original palette.
`BoardAtmosphere.TAINTED_COLOR_STRENGTH = 0.10f` and `TOXIC_COLOR_STRENGTH = 0.25f`
set the baseline blends. Taint is strongest near the horizon and in existing fog;
the overhead blend is 35% as strong. Trace/thin air scales the effect to 15%/45%,
vacuum and space suppress it, and night and dawn/dusk reduce it further.

The palette calculation preserves each atmospheric color's luminance and leaves
surface lighting, shadow contrast, exposure and scene grading unchanged. Fog/haze
density and their activation still come from weather; taint alone adds no fog,
clouds, wind or precipitation. Wind-driven fog banks inherit the same color.
Colors are calculated on the CPU only when settings or palette controls change,
then supplied through existing uniforms. There are no shader changes, extra
texture samples, particles, framebuffers or render passes for taint.

Scenario lighting stores categories, not an exact time or astronomical date.
Each GPU window samples its visual time once, using 15-minute choices within the
windows below. Frame refreshes, weather changes, camera changes and board switches
reuse that choice. Lighting-category changes map the same sample to the appropriate
window; returning to a category restores its earlier choice. Modal condition previews
use the same selection without changing the scenario. Defaults restores the scenario's
time, while manual time overrides survive routine and scenario updates. Reopening
the GPU window can choose a new time. No gameplay random rolls are consumed.

| Scenario condition | Initial visualization |
| --- | --- |
| Day | Random 08:00–17:00 daylight |
| Dusk / Dawn (`DUSK_DAWN`) | Random 06:00–06:45 dawn or 17:15–18:00 sunset twilight |
| Full moon | Random 20:00–04:00 with a cool directional moonlight source and ambient fill; the default for night previews |
| Moonless / Pitch black | Random 20:00–04:00 with no directional moonlight or lunar shadows, plus another -0.6 / -1.0 EV (total -1.0 / -1.4 EV); retain ambient readability |
| Glare / Solar flare | Random 08:00–17:00 with slightly increased exposure |
| Clear | No cloud cover or precipitation; independently configured fog/sand still apply |
| Light / Moderate / Heavy / Gusting rain / Downpour | Increasing rain intensity; heavy and gusting rain share density, with actual scenario wind determining drift |
| Light / Moderate / Flurries / Heavy snow | Increasing snowfall intensity; actual scenario wind determines drift |
| Sleet | Mixed rain and snow |
| Ice storm | Mixed rain and ice pellets |
| Light / Heavy hail | Increasing ice-pellet intensity; heavy hail uses the maximum slider value |
| Lightning storm | Rain and a visible illumination strike within half a second of enabling; repeat at seven-second intervals with a short attack and longer decay |
| None / Light / Heavy fog | None disables ground fog. Light uses 0.2 ground fog/haze; Heavy uses 1.0 for both. Fog banks and haze share an opacity cap. Light fog also lifts cloud cover to 0.10 and Heavy to 0.25, as haze implies a duller sky |
| Blowing sand | Wind-aligned grain noise and a warm, height-limited veil, only when the game's `isBlowingSandActive()` says it is effective; does not automatically enable haze or its render pass |
| Calm / Light gale / Moderate gale / Strong gale / Storm / Tornado F1–F3 / Tornado F4 | Increasing wind drift, capped at storm-scale visual motion for tornadoes; no tornado funnel geometry |
| Six wind directions / Random | Use the game's resolved direction; unresolved Random uses the existing visual 0° direction for minimum cloud drift, without consuming or resolving game wind rolls |
| Vacuum / Trace / Thin / Standard / High / Very high pressure | Vacuum disables atmospheric weather, wind, clouds and shafts, and uses a dark sky. Trace disables clouds; Thin permits light cloud shade. Trace and Thin suppress precipitation and fog. Standard and denser air permit weather-shaped cloud shadows. Scenario fog starts at `STANDARD_GROUND_LAYER_HEIGHT`; High and Very High lower it by 0.75 and 1.5 levels respectively, with a minimum of 1.0 |
| Space and high-altitude space boards | Suppress atmospheric precipitation, clouds, fog, haze, and wind across the board |
| Temperature | Above 0°C, liquid rain can wet exposed terrain in standard or denser air; freezing suppresses the liquid-water preview |
| Atmospheric taint | Restrained palette for the sky/horizon and existing fog/haze, scaled by severity and pressure; no new scattering pass or particles |
| Gravity | Captured game gravity drives jumps by default. A local gravity override or complete conditions preview drives new visual jump arcs and timing without changing the recorded move, jump MP or game rules |
| EMI | Existing game rules and tactical indicators remain authoritative; no added screen distortion |
| Terrain affected / Wind-shift flags | Consume the game's resulting terrain and effective wind; the renderer does not simulate accumulation, freezing, wind rolls, or terrain changes |

The Swing adapter reads these conditions and publishes immutable visual settings
in its frame snapshot. The render thread never reads or mutates the live
`PlanetaryConditions` object. Scenario light and weather are independent: a clear
night remains clear, and rain does not choose a different moon mode. Explicit
Moonless/Pitch Black settings suppress the moon after manual clock/weather changes
or enabling Fixed sun/moon. Complete presets or the moonlight checkbox can change
that visual choice explicitly. Daytime previews still get sunlight.
Night previews without an explicit dark-night condition use Full Moon.
Pressure starts from that snapshot; while the selected pressure is vacuum,
weather/cloud controls stay disabled. A complete atmospheric preset or the pressure
selector can explicitly preview a different atmosphere without changing the game.
Lunar ground artwork does not override the scenario's pressure: airless lunar
scenarios start without clouds or shafts.

Heavy Fog uses dense, low-lying mist with haze above the ground layer; Light Fog
uses a gentler version. Scenario lighting, exposure, precipitation, and wind
remain independent of fog, while fog adds a little cloud cover of its own. For
lower morning mist, set Time of day to 09:30,
Exposure to -0.6 EV, and Ground layer height to 1.5 in Tuning. The manual fog
controls can then override the selected planetary conditions; Defaults restores
the scenario's fog appearance.

Pressure reduces scenario fog height by 0.75 levels per step above Standard,
clamped to `BoardAtmosphere.MIN_GROUND_LAYER_HEIGHT` (1.0). This is a visual tuning rule;
it does not change fog density or game rules. The same minimum applies to Tuning.

Fog, haze, rain, snow, hail, and blowing sand share the lowest hex **LEVEL** as
their atmospheric baseline. Water depth, riverbed recesses, and other terrain
depth do not lower it. A map whose lowest surface is level 4 starts its weather
at level 4; negative surface levels work the same way. The particle volume extends
above the highest terrain and features, with opaque terrain hiding particles behind it.

Fog and haze share a **25% maximum opacity**, including at maximum slider values.
Their sliders control density, not opacity: nearby or elevated surfaces can still
receive less fog at full density. At least 75% of scene color is retained before
color grading. The Tuning panel shows the shared cap. The Clear preset
sets cloud cover and both densities to zero and bypasses the depth and fog passes. Full Moon uses a
cool directional source and ambient fill; diffuse twilight fill prevents a dark dip
as directional sunlight and moonlight exchange positions.

## Rendering

`BoardAtmosphere` derives sun/moon direction, direct and ambient colors, fog and
background colors, tint, and saturation from one immutable settings record.
Morning and evening cast shadows in opposite directions, low sunlight casts
longer shadows, and Full Moon nights retain cool ambient fill and moonlight.
Twilight warmth peaks at the horizon independently of the remaining daylight.
Dawn uses peach light and dusk uses amber light, with warm diffuse illumination
on the board and a cooler violet upper sky. Diffuse warmth preserves its luminance;
midday and full-night surface lighting keep their established values. The sky has
separate horizon and upper colors instead of borrowing the fog color. It receives
exposure once, without the terrain's additional tint/desaturation. Smooth solar-height
curves join night, twilight and day when previewing different times; the combat clock
still stays fixed. Thin/trace air reduces twilight scattering and vacuum has no gradient.
Cloud cover blends cloud-colored tones most strongly overhead, fading toward the
horizon at every hour, including night. This is solid-color interpolation in the
existing composite, with no sky cloud texture, extra shader or render pass.
Moonless and Pitch Black detach the directional source, skip its geometry/cloud
shadow passes, and retain ambient fill at their respective exposure settings.
Reenabling the sun/moon reuses the existing native shadow resources.

Fixed sun/moon expresses the source direction in the final camera basis every
frame, before either shadow pass. It stays above the screen and terrain through
the overhead and maximum-tilt views. Incidence compensation preserves direct
illumination on level ground when the camera tilts. It uses the existing light
and shaders; no additional light, shader or pass is added. Disabling it restores
the normal world-space direction. The clock still determines lighting color,
strength and the sun/moon handover.

`BoardAtmosphere.MOONLIGHT_SHADOW_CONTRAST` controls the fraction of ambient fill
transferred into visible moonlight, giving Full Moon nights stronger native cast shadows.
The **Moon shadow contrast** control under **Light and fog effects** overrides it;
Defaults restores the constant. Zero restores the original softer shadows, and
higher values deepen them subject to the same directional RGB budget as daylight.
The transfer fades with the moon, leaves Moonless/Pitch Black and daylight alone,
and preserves each RGB channel on lit level ground, the sky palette and exposure.
It uses the existing light and shadow map, with no shader or render-pass changes.

`DAYLIGHT_SHADOW_CONTRAST = 0.25f` transfers up to 25% of ambient sky fill into
the visible sun, including dawn and dusk. Compensation for the sun's angle preserves each
RGB channel on sunlit level ground. Its RGB budget caps the world-space directional
components at 0.95 instead of disabling contrast near the horizon. The low-angle
direction is bounded to about 12 degrees above the terrain, producing shadows about
4.8–4.9 times the caster height throughout the scenario's dawn/dusk windows. This is
a readable LDR approximation, not an astronomical horizon with infinitely long shadows.
The remaining ambient light keeps shade readable. Cast shadows and
sun-facing/away-facing surfaces gain contrast through the existing shaders;
exposure and sunlit flat-ground brightness stay the same. See the
[lighting study](gpu-lighting-study.md) for measured values, the pipeline audit,
and the limits of this approximation.

Sun visibility reaches full strength at 06:00 and remains through 18:00. The single
directional source fades to zero before switching between sun and moon, in the
surrounding twilight. Sun and moon colors are never summed onto the same direction.
Only Full Moon enables the night source. The derived lighting record identifies the
active sun for god rays; sky brightness is no longer used to infer which source is
casting shadows. Geometry, cloud transmission and shafts use the same final light.
The existing Time of day and Fixed sun/moon controls drive this behavior together.

`GpuTerrain` applies this lighting to terrain, authored features, and units.
Its existing directional shadow map is reused when controls change and is
refreshed when its light or geometry changes. Opaque geometry is shared between
the shadow and scene passes, including the current animated unit poses.

`GpuAtmosphere` captures scene color and a sampleable 24-bit depth attachment in
one geometry pass. Fog, shafts and visibility effects reuse that depth; there is
no second camera-depth geometry submission. Fog is evaluated at quarter resolution using
the analytic integral of exponential height density, stopping at opaque geometry.
Four samples through the air column vary local height and density into broad, correlated banks.
Each uses two smooth volume-noise octaves and an analytic height integral within its interval.
Their world-space coordinates follow integrated motion, with no frame-random jitter
or clock-wrap discontinuity. Calm fog drifts at `GpuAtmosphere.FOG_CALM_DRIFT = 0.06f`
hex widths per second along a gently bending vector. Its direction varies smoothly
over 48 seconds and blends into the selected wind vector by light-gale strength;
wind strength one moves banks at 0.3 hex widths per second. This updates only the
existing noise offset on the CPU, adding no shader samples or render passes.
Defaults are `GpuAtmosphere.FOG_HEIGHT_VARIATION = 1.25f`
and `FOG_DENSITY_VARIATION = 0.85f`, also exposed in Tuning. Set both to zero for uniform fog.
Density variation thins gaps instead of increasing the opacity ceiling; local height
has a 0.25-level floor. Haze keeps the mean six-height column independently of bank
variation and cloud altitude. This is a sparse volume approximation, not a fluid simulation.
It occupies air in front of the sky as well as terrain; its horizontal limits fade softly.
Depth normals and the shared hex-footprint calculation exclude the solid outer map walls,
while horizontal submerged terrain can still receive weather above its water surface.
Daytime clouds add a
bounded 12-sample single-scattering estimate using cloud transmission and the
existing geometry shadow map. Shafts stop at camera depth and share the 25%
atmospheric opacity ceiling; night, vacuum and space disable sunlight shafts.
Depth-weighted upsampling reduces bleeding across silhouettes. The backdrop is
identified using scene alpha. Ground fog and sand fade out between 1.5 and 3 local
falloff heights; haze retains its original six-height column independently of
cloud altitude. Fog's four samples stay in this lower column when shafts extend
the overall volume. Pixels already at the
25% opacity cap skip the twelve shaft samples, whose contribution would be zero.

`GpuClouds` maintains one world-space density field for both camera presets and free
orbit. A fixed 192-square sunlight-transmission atlas is shared by terrain, props, units,
and shafts. It multiplies direct diffuse/specular light, retaining ambient and
emissive light. The existing geometry shadow map is not rerendered merely because
clouds moved. The backdrop uses a smooth blend of solid sky/horizon colors;
there is no cloud-view shader, render target or compositing texture.
See [cloud rendering](gpu-clouds.md) for the
model, ownership, limitations and validation.

The final pass applies fog transmission and scattering, exposure, color tint,
saturation, and a subtle vignette. Daylight is calibrated for the existing LDR
tileset; there is no additional filmic curve to amplify its baked contrast.
Neutral exposure lifts daylight by 0.8 stops and uses -0.4 EV at night, a 1.2-stop day/night difference.
The cloud atlas is the sole attenuation of surface sunlight/moonlight by clouds.
Open columns retain clear-weather incident lighting, ambient fill and grading;
dense cloud shade reduces direct light up to the cover-dependent opacity limit,
while retaining the readable ambient floor.
Coverage still affects the sky/fog palette, without a second global sun reduction.
The backdrop's day/night gradient shifts toward muted colors with cloud cover.
The composite writes color and the captured hardware depth together, so weather,
tactical markings, roof/hex text and inspection outlines retain correct occlusion.
There is no separate depth-restoration shader or pass. Screen annotations
and Scene2D controls then draw in screen space.

Sun glare is part of that same composite, before FoV shading and tactical/UI drawing.
It adds no draw calls, framebuffer, bloom chain or geometry submission. The active
light's camera-space bearing drives an angular projection for the orthographic
board: turning toward a low sun brings a warm glow into the upper viewport;
turning away or looking straight down suppresses it. Pan and zoom do not move
the distant source. Fixed sun/moon uses its existing camera-relative direction,
with glare energy independent of the surface-light compensation used for tilt.
The current fixed light stays on the camera's side of the board, so rotating that
mode cannot turn into the sun or create a flare.
The real sun color and fading source energy also prevent glare at night, including
Full Moon. Cloud cover, ground fog and haze reduce its intensity. As a lens effect,
it does not require air and remains adjustable on airless maps.

Five nearby samples of the existing scene depth mask the projected source.
The existing fullscreen vertex shader evaluates them once per quad vertex,
and interpolates the identical visibility value across the image. The shader
factory checks vertex texture support once at creation; GPUs without it retain the
same five-sample fragment path. Both paths share `sun-visibility.glsl`.
Opaque geometry at the source or viewport edge suppresses its glow. This is a
screen-space occlusion approximation, not a physical lens or an offscreen
visibility simulation. A small source footprint softens partial occlusion.
The shader adds a hot source, a broad golden veil and two lens reflections with
an amber crescent. Reflections follow the source-to-screen-center axis; their
soft edges are procedural and require no additional textures. The small reflection
is evaluated only inside its 0.035 radius; the large reflection and crescent only
inside 0.20, where the omitted Gaussian tail is below one display level even at
maximum glare/exposure. The broad glow and warm veil retain their original extent.
All components fade together with source visibility. It does no ray marching or blur.
Fewer passes are not claimed as an FPS gain:
the composite still performs extra fragment work while glare is visible.

`GpuSunGlareSmokeTest` checks dawn/dusk, orbiting toward/away, the overhead view,
source occlusion, clouds/fog, night modes, vacuum and fixed-sun behavior. It also
asserts unchanged draw counts and writes `sun-glare-timing.txt`. Native live-panel
checks also cover the slider, Defaults and before/after board captures.

`GpuSunGlareBenchmarkSmokeTest` compares the complete composite against a frozen
test-only copy of the original glare function. It alternates reference, vertex-only,
bounds-only, optimized and glare-disabled variants on the same RGBA8/depth24 targets,
rotating their order for 24 warmup and 120 measured rounds per case. GPU timestamps
measure the quad draw, excluding uniform setup, scene/UI, readback and `glFinish`.
One paired Intel Iris Xe run measured these median GPU times:

| Resolution | Original composite with glare | Optimized composite with glare | Original glare increment | Optimized glare increment |
| --- | --- | --- | --- | --- |
| 1280×800 | 0.3371 ms | 0.2365 ms | 0.1874 ms | 0.0871 ms |
| 1920×1080 | 0.5143 ms | 0.3557 ms | 0.2892 ms | 0.1317 ms |

The glare increments subtract each implementation's own disabled baseline.
Both changes contribute: at 1280×800, vertex-only measured 0.3065 ms and bounds-only
0.2688 ms for the composite with glare. Occluded glare fell from 0.1339 to 0.1085 ms;
disabled baselines stayed close (0.1038 / 0.1034 ms in that case).
Across two resolutions, 72 comparisons covering partial/full occlusion, centered
and offscreen sources, disabled glare and maximum glare/exposure on black were
pixel-identical in this run, including the fragment-sampling fallback.
Reports are `sun-glare-comparison.txt` and `sun-glare-image-comparison.txt` in the
configured screenshot directory. These paired results replace the earlier isolated
0.64 ms glare estimate; they are synthetic GPU-stage measurements, not a whole-game
FPS claim or a guarantee for other GPUs.

Buffers resize with the board viewport and are disposed with the GPU view.
See the [weather performance audit](gpu-weather-performance.md) for complete
pass counts, particle budgets and measurement limits.

Precipitation uses a lazily allocated, shared pool of 4,608 GPU-animated quads.
Maximum rain and snow each draw 4,608 particles, and hail draws 3,072.
A curved intensity response keeps light precipitation gentle and makes downpour,
heavy snow, and heavy hail reach full density. Rain uses fine streaks; snowflakes
are 55% of their original diameter and fall about 71% faster with less sideways sway.
Rain has a 64% peak opacity and hail uses faster pellets.
They draw against the restored opaque scene depth before tactical markings,
so roofs occlude particles and UI remains sharp. The mesh binds once for all active
precipitation types. Disabled effects issue no particle draw calls, and
precipitation requires no additional framebuffer. The weather volume is
bounded by the board and camera footprint; changing its extent can redistribute
particles. There are no physical splashes, snow accumulation,
lightning bolt meshes, or per-building weather simulation.

Sand uses the existing full-resolution atmosphere composite instead of particle draws.
The shared depth/height integral limits dust to the visible air column; higher terrain
and roofs emerge above shallow layers. Its analytical height integral also locates
the density-weighted centre of the visible air column, where two texture lookups sample
a broad gust and fine, wind-aligned 3D grains. They produce brown/gold noise and a veil
without an extra framebuffer, scene submission or sand draw. Wind maps to 0.65–2.15
hex widths per second. The field remains world-anchored through pan/orbit/zoom and
extends above the board silhouette. Outer map walls stay clear, and tactical/UI drawing
remains after compositing. Fog and sand share one lazy 256-square
noise texture and its existing cloud-noise generator, with separate resource ownership
from the cloud atlas. Resizing retains the noise; disposing the view releases it.
Sand intensity controls extinction, with a 25% opacity ceiling even at strength one
(`GpuAtmosphere.MAX_SAND_OPACITY`). Fine detail fades to its average when smaller than
a pixel, retaining the dust veil without shimmer. The packed volume-noise texture uses
linear filtering; ordinary mipmaps would introduce seams between its encoded Z slices.
This is a stylized visibility effect, not individual simulated grains.
Both effects skip pixels outside the projected air volume, including padding for
the fog reconstruction filter, instead of evaluating weather over empty screen space.

Exposed ground uses its existing lighting shader for wet albedo and a view-dependent
water-film sun highlight. Concrete/rock have a stronger sheen than porous sand,
dirt or grass. Geometry and cloud shadows occlude that highlight. Normal maps
remain optional; frozen surfaces and snow are excluded. Flat ground also receives
irregular puddles, weighted toward concrete and rock. A shared 64x64 mipmapped noise
texture supplies stable world-space coverage and soft sky variation. Puddles flatten
the terrain normal and reflect the current sky/horizon palette with an angle-dependent
blend. These are approximate sky reflections; nearby units and buildings are not reflected.
The coverage field uses broad basins. Increasing liquid rain lowers the wet-area
threshold and strengthens coverage, so puddles grow, join, and become more visible.
Rain amount controls both area and surface strength; porous ground retains less
water coverage than concrete and rock. No rainfall means no puddle surface.

Puddles and horizontal water share a staggered expanding-ring normal perturbation,
driven by the terrain animation clock. Each cycle hashes a new impact position;
heavier rain increases impact probability, frequency, radius, and normal strength.
Six independently staggered layers provide six times the previous downpour impact
density without shrinking the rings. The whole rainfall range uses this density
scale, with fewer active impacts at lower amounts.
Ground rings are restricted to the puddle mask. Each ring fades before its cell boundary,
avoiding neighbour searches, per-drop geometry, simulation and extra draw calls.
Water combines the rings with its animated normals before per-pixel lighting,
specular highlights and reflection. A small signed crest contrast makes the rings
readable at steep board-camera angles. It preserves tint, transparency and depth behavior;
waterfalls and magma do not receive rain rings. Wetness still follows liquid rain
immediately, with the existing temperature/pressure exclusions and no accumulation.

Detail follows projected hex size: ripples fade between 28 and 88 pixels (including
foreshortening), while puddles/reflections fade between 12 and 40 pixels. Below those
ranges the shader skips the corresponding work; wet ground darkening remains.
The noise texture is owned and disposed by `GpuTerrain`, with no per-frame upload.
There are no reflection captures, additional scene passes or gameplay terrain changes.
`GpuRainSurfaceSmokeTest` checks both cameras, animated rain after subtracting the
water artwork's own animation, unchanged draw counts, a stable paused clock, and
the distant-water cutoff. It also verifies increasing puddle coverage and ripple
activity at higher rain amounts. Appearance captures are saved for native visual review.

The 1280x800 native fixture writes `rain-surface-timing.txt` alongside its captures:
24 alternating synchronized terrain-frame samples after warmup in both cameras
and at overview scale. `GpuWaterShaderSmokeTest` separately compares procedural
and GIF water with identical effects, writing `water-color-timing.txt`. These
include CPU submission and are not GPU-only timings or whole-game FPS guarantees.
No timing queries or synchronization are added to gameplay rendering.

## Verification and limits

`BoardAtmosphereTest` covers the daily light cycle, clock wrapping, finite light
directions, every scenario light/weather category and its random clock window, pressure and space exclusions,
effective blowing sand, and shader input bounds. It also checks preserved sunlit
ground RGB values, camera-relative direction and brightness across orbit/tilt/pan/zoom,
explicit moon suppression, stronger clear-day shadows, unchanged light in cloud openings, twilight/night fill,
warm dawn/dusk across every sampled time, continuous sky palettes and the cloud veil's
top-to-bottom fade, and the absence of clipped light channels near the horizon. `GpuScenarioAtmosphereTest`
checks publication, stable sampled times and immutable ownership across the Swing/render boundary,
including switching between Heavy Fog, Light Fog, and None without changing lighting
or precipitation. `BoardGeometryTest` checks the shared LEVEL-based weather baseline;
`GpuWeatherBaselineSmokeTest` compares actual rain, snow, and hail frames at
levels -3, 0, and 4, verifying that adding depth-20 water does not move the particles.
`GpuGroundWeatherSmokeTest` checks sand/fog baselines separately, along with shared
height controls, wind movement, calm stability, desert contrast, first-use texture
bindings and the composite draw budget. `GpuGroundWeatherTest` checks wind direction,
speed, continuous periodic travel and bounded variation controls.
`GpuAtmosphereSmokeTest`
exercises actual shaders, fog at different heights, contrast under maximum fog,
daylight cast-shadow contrast, full-moon versus moonless/pitch-black lighting,
shared fixed-light projections, shadow resource reuse, live controls, and resizing.
At all eight dawn/dusk clock choices, native checks compare the same receiver with
and without a caster, checking visible contrast inside the long shadow and unchanged
lighting beyond its end. Both camera presets and Fixed sun/moon are exercised.
Twilight and cloud-cover checks read actual sky/ground pixels and verify that the
atlas shader scales opacity without changing its cloud shapes. The min/max sliders
are checked for effective range, override retention and Defaults restoration.
It also checks particle visibility, animation, immediate removal, opaque-depth
occlusion, scenario initialization, Defaults, rain/snow toggle input, and prompt,
visible lightning that stops immediately when disabled. A fixed-time sand render
checks visible fine grains at half strength and increasing coverage at 60% and
full strength, targeting 20% changed-pixel coverage (18–22%) at maximum in the
1280×800 fixture, independently of haze and sky grading.
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
tactical capture. The procedural cloud density supplies only surface shadows
and atmospheric shafts; the backdrop stays a smooth color gradient.
Large boards and shallow light angles retain the shadow map's resolution and bias limits.

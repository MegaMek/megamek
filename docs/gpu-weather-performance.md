# GPU weather pass and cost audit

The board now has one main scene geometry pass, writing color and sampleable
24-bit depth together. The complete frame still has auxiliary passes for
shadows, atmosphere and optional unit outlines. A pass is a rendering stage;
it is not the same as a draw call. Drawing thousands of particles in one target
does not require redrawing the board thousands of times.

## Pass counts

These counts include scene capture, atmosphere and the final composite. They
exclude the conditional geometry shadow map, unit outlines and UI described below.
The before column describes the implementation immediately before this optimization.

| Configuration | Before | Now | Remaining stages |
| --- | ---: | ---: | --- |
| Clear, including precipitation without fog/clouds | 3 | 2 | Scene color/depth; composite |
| Fog/haze without cloud shadows | 5 | 3 | Scene color/depth; quarter-resolution scattering; composite |
| Cloud shadows without fog/haze or rays | 4 | 3 | Cloud transmission; scene color/depth; composite |
| Clouds plus rays and/or fog/haze | 6 | 4 | Cloud transmission; scene color/depth; quarter-resolution scattering; composite |

The native sun/moon shadow map adds one geometry pass only when invalidated by
light/camera/caster changes. It is reused on unchanged frames. Moonless/Pitch Black
nights have no directional source and skip it. Cloud motion alone does not invalidate it.

See-through unit outlines now add two stages while enabled with visible candidates:
one unit-only capture writes team color and sampleable depth together, followed by
the outline composite. Previously they required three stages. Those are outside the
weather system and do not redraw the terrain. FoV shading is already in the final
composite and adds no render pass. Weather, tactical overlays, text and UI add
draw calls on the output target; they do not require another scene capture.

## Rendering follow-up

The outline composite uses a scissor rectangle enclosing the projected unit bounds
plus the complete two-sample halo. It keeps the existing pixel size and samples;
widely scattered candidates can still cover much of the screen. The single capture
uses libGDX's depth vertex shader and alpha-cutout behavior, so depth and color select
the same nearest surface. It removes one full-resolution color attachment and one
depth attachment, as well as the duplicate unit submissions.

The battle view owns a render-frame bounds cache. It resets after placement,
animation, aiming and equipment visibility settle. Frustum culling, cutaways,
shadow fitting/culling and outline clipping borrow the same posed world bounds.
Boxes are reused between frames, while removed units are released from the lookup.
Picking and other code outside that frame boundary still compute current bounds.

Geometry shadows retain their existing caster, articulation, terrain/detail and
light invalidation. A changed viewing camera is now fitted into a scratch shadow
camera first. If that produces exactly the cached projection, the map and its
biased lighting matrix remain untouched. This avoids redundant updates when panning
within an unchanged fitted texel grid; it does not freeze moving-unit shadows.

`GpuMixedUnitBenchmarkSmokeTest` also writes `mixed-unit-stage-timing.txt`, with CPU
submission and GPU median/p95 times for thirteen stages. GPU timestamps use
OpenGL 3.3/ARB_timer_query when available and report CPU-only results otherwise.
A small query ring polls availability; it never waits for a query result. Gameplay
does not allocate queries or read profiling clocks. The benchmark's pre-existing
whole-frame `glFinish` remains outside the measured stages. GPU intervals can
include command-stream idle time, and CPU/GPU values overlap: do not add them.

The crowded benchmark fixes time to 13:00 and suppresses automatic combat-camera
framing, so its named top/isometric scenarios actually retain their chosen view.
Earlier benchmark runs allowed playback to override the camera and cannot establish
a comparable whole-frame speedup against these corrected scenarios.

Native follow-up measurements on Intel Iris Xe (OpenGL 4.6), 1280×900, with 64
samples per view:

| Units | Outline GPU median, top | Outline GPU median, isometric |
| ---: | ---: | ---: |
| 64 | 0.777 ms | 0.976 ms |
| 256 | 2.047 ms | 2.649 ms |
| 512 | 4.542 ms | 4.853 ms |

These are current stage measurements, not an FPS guarantee or a controlled speedup
percentage. All six scenarios returned 64 samples per stage without query-ring
drops. The CPU results identify posing and geometry submission as substantial
remaining costs on crowded boards; weather is not active in this particular fixture.

Six saved before/after outline captures (off, ridge, partial occlusion, team colors,
and trees in both cameras) were pixel-identical. Additional native assertions compare
clipped and full-viewport outlines, including viewport edges, larger outline scale,
resize and fog. A transparent sprite hole stays absent from both captured attachments.
The outline uses one capture and leaves output depth unchanged. Resource auditing
includes its framebuffer/depth texture. Shadow checks preserve the exact biased
matrix when a changed view fits the same projection, and refresh after projection,
caster position or articulation changes. Frame-bound checks cover movement,
articulation and disabled parts.

## Changes without reducing density

The old pipeline redrew terrain and units into packed camera depth whenever fog,
rays, FoV or outlines needed it. It then used a separate fullscreen depth-restoration
shader. Clear weather without those effects instead redrew the geometry directly
into output depth. Both paths are removed: the color pass supplies hardware depth,
and the existing composite writes color and depth together. This also removes the
full-resolution packed-depth color target and its duplicate depth renderbuffer.

Water, cutaway surfaces, decals, exhaust and transparent attack effects do not
write opaque depth. Cosmetic tether lines also leave it unchanged. Fog, precipitation
and outlines therefore keep using the opaque surfaces behind them. Opaque missiles
now participate naturally in scene depth along with their visible geometry.

Rain, snow and hail share one static, GPU-animated mesh. It binds once for all
active types. Wind trigonometry is shared, and per-frame temporary
strength/height arrays are removed. Seeds, quads, opacity, size and animation equations
are unchanged.

| Effect at maximum | Particles | Draw calls | Extra offscreen weather target |
| --- | ---: | ---: | --- |
| Rain | 4,608 | 1 | None |
| Snow | 4,608 | 1 | None |
| Hail | 3,072 | 1 | None |
| Sand | Volume noise | 0 additional; existing composite | None |
| All four together | 12,288 plus volume noise | 3 particle draws | None |

Sand's former 43,008 grains in three batches have been replaced by wind-aligned
volume noise in the existing composite. Analytical height extinction and a sample at
the column's density-weighted centre supply broad gusts and filtered fine detail. Dust appears
above the board silhouette, fades at the volume edges, and excludes the solid map
base. Maximum opacity is 25%, including at strength one. Sand alone does not enable
the fog/scattering pass. The precipitation pool shrinks from 15,360 to 4,608 quads;
rain/snow/hail counts, seeds, sizes, opacity and animation remain unchanged.

Cloud shadows remain 192×192, twelve density samples, density multiplier 0.25,
with the same wind animation and shadow coverage. The sky remains a color gradient.
Cloud and fog elevation bounds now cache the immutable terrain snapshot and level
scale instead of scanning every tile every frame. Resizing the viewport does not
rebuild the cloud atlas.

Fog/haze/rays remain one combined quarter-width/height pass, so the expensive
scattering calculation covers one sixteenth of the full-resolution pixel count.
Four intervals now sample drifting fog banks, varying local height and density
without a new pass; a single 256-square packed noise texture is shared with sand.
Moving fog into the fullscreen composite simply to report fewer passes would lose
that resolution advantage. Lightning and automatic wetness use existing shaders
and add no passes. Wetness always follows liquid rain; its checkbox and manual
slider have been removed.

## Fog correction

Cloud shafts need a taller volume than ground fog. Previously, raising that shared
volume also lengthened the uniform haze integral, making unchanged fog/haze settings
look stronger after clouds were introduced. Haze retains its original
six-falloff-height column independently of cloud altitude. Fog banks and sand now
fade out by three local falloff heights, retaining a low layer above the board.
The combined fog/haze opacity
cap remains **0.25**. Pixels already at that cap skip the twelve shaft samples:
their remaining scattering contribution is exactly zero.

## Verification and performance limits

The original pass-consolidation validation covered 31 regular tests and 17 native
rendering/UI checks. Ground-weather follow-up validation is described below.

Native checks compare the composite's output depth with captured hardware depth
and an independent geometry-depth reference, including trees, a posed unit, both
cameras and a viewport with a bottom offset. The copy has zero measured error in
the fixture. A GL profiler asserts one composite draw with clear weather and two
postprocess draws with scattering, with no terrain/unit resubmission. Separate
draw/vertex counts assert the complete particle budgets above.

The old particle coverage target no longer applies to continuous dust. Native checks
instead verify bounded opacity, visible air above the silhouette, untouched map
walls, height response, calm stability, wind movement and desert readability.
The tests also cover
precipitation motion/occlusion, wind, terrain elevation and water depth, cloud
motion/density, atmosphere controls, automatic wet/dry ground, FoV, outlines,
markers, transparent terrain and viewport resizing. An isolated native fog check
raises the cloud-volume ceiling fivefold and verifies identical uncapped haze.

The ground-weather follow-up passed 38 regular tests, eight native rendering/UI
tests and both main/test Checkstyle tasks. Native checks include the first frame
after lazy noise allocation, 25% maximum sand contrast loss, visible volume above
the board, clean map walls, both camera presets, low-angle views, zoom, raised/negative
terrain and depth-20 water. Rain, snow and hail retain their full draw/vertex budgets.

`GpuGroundWeatherSmokeTest` writes `ground-weather-timing.txt`. On Intel Iris Xe at
1280×800, the final synthetic run measured GPU medians of 0.154 ms for clear weather,
0.453 ms for maximum sand and 0.388 ms for fog, including the existing composite.
It uses 16 warmups and 40 interleaved samples per mode; scene capture/UI and the
subsequent `glFinish` are outside the GPU intervals. Timings varied substantially
across runs, including the clear baseline, so they do not establish a controlled
speedup over the former particle sandstorm. Empty screen regions now skip weather
work using the projected air-volume bounds; this adds no draw or framebuffer.

`GpuAtmosphereSmokeTest` writes `atmosphere-timing.txt` and
`cloud-shadow-timing.txt` in `megamek/build/gpu-board-review/`. They use Intel Iris Xe,
1280×800, fifteen warm-up frames and thirty measured frames per case, with `glFinish`
and VSync disabled; profiler assertions run outside the timing windows. These
synthetic fixtures omit normal UI, complex battles and streaming. Timing varied
between runs, so they do not establish a reliable speedup percentage or a minimum
FPS on older computers. The verified savings are removed geometry submissions,
depth passes/buffers, redundant mesh binds and sand particle draws. The new volume
noise adds fragment work, so fewer draws alone do not establish a speedup.

The depth attachment uses the existing desktop GL20 calls and 24-bit format;
the window's requested context and shader language version have not changed.
libGDX's generic framebuffer builder reserves depth textures for its GL30 path,
so this desktop-only attachment is created explicitly and disposed with its owner.
See the pinned [libGDX framebuffer implementation](https://github.com/libgdx/libgdx/blob/1.14.2/gdx/src/com/badlogic/gdx/graphics/glutils/GLFrameBuffer.java).
Other GPU vendors/platforms have not been validated in this run.

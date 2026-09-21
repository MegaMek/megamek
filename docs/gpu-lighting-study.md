# GPU board lighting study

This study covers the native ambient/direct rebalance. The subsequent
[cloud feature](gpu-clouds.md) adds moving cloud shadows and spatial sunlight
transmission separately. Midday and full-night surface illumination still use the
calibration measured here. The later [twilight palette](gpu-atmosphere.md) intentionally
warms dawn/dusk illumination and gives the sky an independent horizon gradient;
the twilight measurements below describe the original rebalance. Cloud shadows replace the old global overcast
attenuation, so an opening retains clear-day incident light. This document keeps
the original clear-weather reference measurements; current cloud behavior and
validation are described in the cloud feature notes.

Full Moon shadows now also use `MOONLIGHT_SHADOW_CONTRAST = 0.15f`, adjustable in
Tuning. This transfers ambient fill into the existing moon source while preserving
lit level-ground RGB; the earlier night shadow measurements below predate this change.

The board uses a single shadowed directional light plus uniform ambient fill.
The original clear-day balance kept almost 69% of flat-ground illumination in
ambient light at the default 13:00 setting. Blocking the sun therefore left
most of the illumination intact. This explained the pale cast shadows without
implying a shadow-map or opacity defect.

The adjustment moves some ambient fill into the existing sun while preserving
the total diffuse light on sunlit, level ground. It changes lighting inputs only:
the GLSL, light count, render passes, shadow sampling and exposure are unchanged.

## Where illumination is controlled

| Location | Responsibility and current values |
| --- | --- |
| `BoardAtmosphere.lighting()` | Derives source identity, direction, direct/ambient RGB and grading from time. Base sunlight is `0.3 * sun`; moonlight peaks at `0.14` and fades out before the sun appears. Daytime ambient RGB starts at `(0.53, 0.56, 0.60)`; night uses `(0.62, 0.69, 0.82)`. |
| Cloud response | Coverage affects the sky/fog palette and interpolates cloud-patch opacity between the 0.20 / 0.85 limits. The shared density integral attenuates direct diffuse/specular light spatially, retaining ambient fill. No global cover-dependent light reduction or desaturation remains. |
| `BoardAtmosphere.Lighting.exposureScale()` | Interpolates the baseline exposure from +0.8 EV in daylight to -0.4 EV at night, then adds the user/scenario compensation. Exposure scales the whole scene and does not independently control cast shadows. |
| `BoardAtmosphere.fromScenario()` and `GpuBoardTuning` | Choose the initial clock, weather and exposure and publish visual previews. Glare/solar flare add +0.6/+1.2 EV; moonless/pitch-black categories add -0.6/-1.0 EV. These are presentation values, not changes to game visibility. |
| `GpuBattleView` → `GpuAtmosphere.configure()` → `GpuTerrain.setAtmosphere()` | Derives lighting when settings change and supplies it to the shared terrain, prop and unit environment before rendering. Both camera presets use this path. |
| `GpuTerrain.applyLight()` | Applies the RGB values to the existing `DirectionalShadowLight` and `ColorAttribute.AmbientLight`. Without an atmosphere, standalone scenes retain the older direct `(0.55, 0.53, 0.48)` / ambient `(0.55, 0.58, 0.62)` fallback, or ambient-only `0.85` when no light is supplied. The regular battle view supplies an atmosphere. |
| libGDX `DefaultShader`, `GpuUnitShader` and `terrain-normal.frag` | Direct diffuse light depends on the surface normal and shadow-map visibility. Ambient light remains in shadows. Unit paint uses the upstream lighting calculation; normal-mapped ground performs the equivalent calculation per pixel. |
| `GpuAtmosphere` and `atmosphere-composite.frag` | Capture RGBA8 scene color and apply fog, tint, exposure, lightning, saturation and vignette. Exposure is applied after converting the scene to linear light; this is an LDR pipeline with clipping, not HDR tone mapping. |
| `atmosphere-fog.frag` | Fog/haze mix scattered light into the scene with a shared 25% opacity cap, naturally reducing visible contrast. This is separate from surface lighting. |
| `GpuAtmosphere.renderWeather()` / `GpuWeatherParticles` | Precipitation is drawn after scene grading and uses ambient RGB plus the existing `0.25` visibility fill. Reducing clear-day ambient also slightly dims these particles; the native weather checks cover visibility and sand coverage. |
| Lightning and tactical overlays | Lightning is a temporary whole-scene linear-light multiplier of up to `1 + 1.1 * intensity`, shaped by its animation envelope. Tactical annotations render after atmosphere grading and retain their established readability. |

The native diffuse calculation is supported by the pinned libGDX 1.14.2
[vertex shader](https://github.com/libgdx/libgdx/blob/1.14.2/gdx/res/com/badlogic/gdx/graphics/g3d/shaders/default.vertex.glsl)
and [fragment shader](https://github.com/libgdx/libgdx/blob/1.14.2/gdx/res/com/badlogic/gdx/graphics/g3d/shaders/default.fragment.glsl).

## Current adjustment

`BoardAtmosphere.DAYLIGHT_SHADOW_CONTRAST` is `0.25f`. Zero restores the previous
ambient/direct balance. With `A` as ambient RGB, `D` as directional RGB and
`q = -direction.z` as the incidence on level ground:

```text
sunVisibility = smooth(-0.08, 0, sunAltitude)
compensation = min(0.25 * sunVisibility / q, minRGB((0.95 - D) / A))
transfer = compensation * q
A' = A * (1 - transfer)
D' = D + A * compensation
```

The existing shader then produces `A' + q * D' = A + q * D` on fully sunlit
level ground, independently for red, green and blue. In a full cast shadow the
direct contribution vanishes, leaving the lower `A'`. This also retains cooler
sky fill in shade and a warmer direct light.

The transfer reaches 25% under high sunlight and remains active at dawn/dusk,
bounded by the 0.95 directional RGB ceiling. The former altitude gate disabled
it below about 14 degrees, leaving 99.2% of level-ground illumination in shade
at 06:00 despite the warm sky and ground colors. Native shadows existed, but were
almost invisible. The minimum rendered source altitude now bounds shadow length
to about five caster heights and keeps incidence compensation finite. The warm palette and
fixed scenario clock windows are retained. The light fades to zero before
switching sun/moon direction; moon color no longer mixes into the rising sun.
God rays consume this same source identity and direct RGB.

Native framebuffer probes at all eight dawn/dusk times measured shadow/lit
display luminance ratios of 0.767–0.809 (roughly 19–23% contrast). World-space
shadow length was 4.82–4.91 caster heights. Fixed sun/moon preserves that contrast;
its camera-relative direction shortens the ground projection when tilted, as intended.
The fixture checks both top and isometric views and verifies that the shadow ends
at the expected projected position. Results are in
`megamek/build/gpu-board-review/twilight-shadow-checks.txt`; images use `twilight-shadow-*.png`.
This fix changes CPU light inputs only, without adding shaders, textures or passes.
Verification passed 37 regular tests across `BoardAtmosphereTest`, `BoardShadowTest`,
`GpuCloudsTest` and `GpuScenarioAtmosphereTest`; five native tests across
`GpuAtmosphereSmokeTest`, `GpuPlanetaryConditionsSmokeTest` and `GpuTerrainNormalsSmokeTest`;
and main/test Checkstyle. The new contrast regression failed against the former
06:00 lighting before the fix. Native checks include 32 combinations of time,
camera preset and fixed-light mode. Dawn/dusk board and isolated-shadow captures
were visually inspected. No whole-game FPS claim is made.

At 13:00 in clear weather, ambient RGB changes from `(0.5300, 0.5600, 0.6000)` to
`(0.3975, 0.4200, 0.4500)`. Direct RGB changes from
`(0.2954, 0.2836, 0.2540)` to `(0.4436, 0.4402, 0.4218)`.
The sum on level sunlit ground remains `(0.7941, 0.8136, 0.8272)`.

## Numerical review

The following original-rebalance measurements are weighted diffuse RGB values for neutral, level ground before
fog, exposure, grading or material-specific specular/emissive contributions.
They are relative renderer values, not measured lux or physical luminance.

| Time / cloud setting | Sunlit ground, before and after | Shadow / sunlit before | Shadow / sunlit after |
| --- | ---: | ---: | ---: |
| 09:00 clear | 0.7137 | 78.0% | 58.5% |
| 12:00 clear | 0.8253 | 67.4% | 50.6% |
| 13:00 clear, default | 0.8104 | 68.7% | 51.5% |
| 07:00 clear | 0.6533 | 95.3% | 95.3% |
| 18:00 clear | 0.7963 | 99.1% | 99.1% |
| 00:00 clear | 0.7643 | 89.6% | 89.6% |

During the original rebalance, a before/after sweep sampled every minute from 00:00 through 24:00 at five
cloud settings, totaling 7,205 samples. Maximum sunlit-ground RGB change was
`1.2e-7`, within float rounding. Night/horizon light RGB and exposure were
unchanged. The largest direct-light RGB component was `0.49175507`, below the
clamping limit. The calculations use the actual compiled `BoardAtmosphere`.

That original verification passed 30 regular tests across `BoardAtmosphereTest`,
`GpuScenarioAtmosphereTest` and `GpuUnitShaderTest`, plus all three native
`GpuAtmosphereSmokeTest` checks. These include rendered cast-shadow contrast,
fog, night/twilight readability, shadow resource reuse, weather visibility,
live controls and viewport resizing. Before/after captures of the synthetic
board and the shipped board artwork were visually reviewed. The study's
reference images are in `megamek/build/gpu-lighting-study/before/`; current
captures are in `megamek/build/gpu-board-review/atmosphere-*.png`.
In the noon fixture, a sunlit-ground pixel remained RGB `(158, 162, 165)` while
a cast-shadow pixel changed from `(105, 112, 120)` to `(80, 84, 90)`. A second
sunlit probe and the sky probe were also identical. The night fixture's PNG
was byte-identical before and after the adjustment.

The baseline exposed a pre-existing stale moonless/pitch-black exposure
assertion. It now matches the existing -0.6/-1.0 scenario compensation; the
runtime night values were not changed. The atmosphere documentation also
now reflects the actual -0.4 EV night baseline and current fog-height settings.

## Realism and limits

This is more defined directional lighting within the existing renderer, not a
physical daylight simulation. The original ambient/direct ratio is an artistic
choice, and darker shadows are not automatically more realistic.

Level sunlit diffuse brightness is preserved; walls, slopes, normal-map details
and curved unit surfaces respond differently to the stronger directional light.
Specular highlights can also strengthen. The whole image's average brightness
can decrease as shaded areas darken. It is not possible to keep every other
surface identical using only one uniform ambient term and one directional light.

Full-moon fill remains deliberately bright for playability. Moonless and
pitch-black scenarios suppress the directional moon and use darker exposure
presets while retaining ambient readability. Sun direction uses
a representative daily arc, without latitude, season or a separate moon orbit.
Uniform ambient fill has no local sky occlusion or bounced-light geometry, and
the tileset already contains painted shading. Cloud transmission occludes
sunlight spatially; dense overcast can remove most of the direct component.
Shadow filtering and the limits of the
existing geometry shadow map are unchanged.

The adjustment adds a few CPU arithmetic operations when atmosphere settings
change. It adds no shader instructions, lights, textures or rendering passes.
This is a pipeline comparison, not a measured frame-rate improvement.

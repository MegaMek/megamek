# Sprite-referenced 3D units

Authored 3D unit models are controlled by the code switch
`GpuUnitModels.ENABLED`, currently `true`. Set it to `false` to use the existing
sprite rendering in both GPU camera views. Asset generation and direct asset
review tests remain available while the switch is off.

The GPU board can resolve 3D models from the optional fourth field in
`mm-data/data/images/units/mekset.txt`. Exact model overrides take precedence;
sprite-only overrides still inherit a chassis model. Type-specific defaults
provide biped, quad, tripod, infantry and Battle Armor fallbacks.

The first asset library includes four authored Mek chassis and all 112 of their
catalogued variants, plus four infantry poses and four BA poses. Infantry uses
up to six figures and BA up to four, derived from surviving personnel. The
largest complete asset is 996 triangles; the 112 Mek assemblies use 310–672
triangles. Their silhouettes are authored from the existing chassis illustrations
as well as the top-view sprites. The build records both reference hashes and
provides front, side, top, and three-quarter review renders. Other Mek chassis
use generic models; 734 remain on the explicit authoring queue.

Conventional infantry selects its artwork from the unit's movement mode:
motorized uses reinforced jeeps; mechanized uses tracked, wheeled, or hover APCs;
jump infantry carries small back-mounted jump jets. With 1–4 compressed slots,
one vehicle replaces a troop; with 5–6 slots, two vehicles replace troops. Zero
survivors produce an empty formation. Foot infantry and Battle Armor keep their
existing geometry, and unsupported infantry movement types use foot poses.
These are baked visual groups, not additional entities or transport game rules.
The optional `movementFormations` map in the infantry descriptor is keyed by
`EntityMovementMode` names; old descriptors keep their numeric `formations`.

`MekModelCatalog` exports the current unit/equipment data and actual selected
sprites through MegaMek's existing loaders. Blender builds the source chassis
and equipment modules offline, then bakes each variant into G3DJ. The renderer
uses the same movement timeline, placement, shadows and camera scene as before.
It receives immutable model-selection data after visibility filtering. A sensor
contact has no model identity. Invalid assets fall back without preventing the
board from opening.

From mm-data, run `tools/build_unit_models.ps1 -Preview` to export, build and
validate. Full format, editing instructions, scope and limitations are in
`mm-data/data/models/units/README.md`. The generated Blender review scene is at
`mm-data/.work/mek-models/review/unit-models.blend`.
Run Blender with `--python tools/render_unit_variants.py -- --infantry` in mm-data
for movement and slot-count review sheets under `.work/mek-models/infantry`.

Focused checks from this checkout:

```text
./gradlew :megamek:test --tests '*MekTilesetModelsTest' --tests '*UnitModelSelectionTest' --tests '*MekModelCatalogTest'
./gradlew :megamek:gpuBoardSmoke --tests '*GpuUnitModelsSmokeTest'
```

No Blender or Python installation is required at runtime. The normal data-staging
tasks include all generated unit assets.

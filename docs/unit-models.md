# Sprite-referenced 3D units

Authored 3D unit models are currently disabled by the code switch
`GpuUnitModels.ENABLED = false`. Both GPU camera views use the existing sprite
rendering for Meks, infantry, Battle Armor, and other units. Setting the switch
to `true` enables model selection and loading again. Asset generation and direct
asset review tests remain available while the switch is off.

The GPU board can resolve 3D models from the optional fourth field in
`mm-data/data/images/units/mekset.txt`. Exact model overrides take precedence;
sprite-only overrides still inherit a chassis model. Type-specific defaults
provide biped, quad, tripod, infantry and Battle Armor fallbacks.

The first asset library includes four authored Mek chassis and all 112 of their
catalogued variants, plus four infantry poses and four BA poses. Infantry uses
up to six figures and BA up to four, derived from surviving personnel. The
largest complete asset is 948 triangles; the 112 Mek assemblies use 310–672
triangles. Their silhouettes are authored from the existing chassis illustrations
as well as the top-view sprites. The build records both reference hashes and
provides front, side, top, and three-quarter review renders. Other Mek chassis
use generic models; 734 remain on the explicit authoring queue.

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

Focused checks from this checkout:

```text
./gradlew :megamek:test --tests '*MekTilesetModelsTest' --tests '*UnitModelSelectionTest' --tests '*MekModelCatalogTest'
./gradlew :megamek:gpuBoardSmoke --tests '*GpuUnitModelsSmokeTest'
```

No Blender or Python installation is required at runtime. The normal data-staging
tasks include all generated unit assets.

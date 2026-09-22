/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Attributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelCache;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.DepthTestAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalShadowLight;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.shaders.BaseShader;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.graphics.g3d.utils.DepthShaderProvider;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import megamek.common.board.Coords;

/** Chunked solid hex columns, flat decals and authored features; owns all GL resources it creates. */
final class GpuTerrain implements Disposable {
    static final int CHUNK_SIZE = 16;
    static final int SHADOW_RESOLUTION = 2048;
    static final float DEFAULT_BUILDING_OPACITY = 0.5f;
    /** A fall curves over its lip and spreads into the water it lands in; both arcs' step count. */
    private static final int FALL_LIP_SEGMENTS = 4;
    private static final int FALL_FOOT_SEGMENTS = 4;
    /** Wall clearance of the hanging sheet, in hex-scale units; the flat fall used the same offset. */
    private static final float FALL_CLEARANCE = .06f;
    private static final long ATTRIBUTES = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal
          | VertexAttributes.Usage.TextureCoordinates | VertexAttributes.Usage.ColorPacked;
    private final GpuAssets assets;
    private final boolean liquidShaderAnimation;
    private final boolean proceduralWater;
    private final Texture rainNoise = rainNoise();
    private final GpuUnitModels unitModels;
    private Model limbModel;
    private record GroundSlot(Coords coords, boolean rims) { }
    private final GpuTextures<GroundSlot> ground = new GpuTextures<>(true);
    private final BoardRim rims = new BoardRim();
    private final GpuTextures<Coords> decals = new GpuTextures<>();
    private final GpuTextures<Coords> tactical = new GpuTextures<>();
    private final GpuTextures<Coords> foliage = new GpuTextures<>(true);
    private final ModelBatch batch = new ModelBatch(new DefaultShaderProvider(
          GpuCloudShadow.vertex(DefaultShader.getDefaultVertexShader()),
          GpuCloudShadow.fragment(DefaultShader.getDefaultFragmentShader(), false)) {
        private final DefaultShader.Config groundShader = new DefaultShader.Config(config.vertexShader,
              rainFragment(GpuCloudShadow.fragment(Gdx.files.classpath("megamek/client/ui/clientGUI/boardview/gpu/terrain-normal.frag")
                    .readString(), true)));
        private final DefaultShader.Config waterShader = new DefaultShader.Config(config.vertexShader,
              rainFragment(GpuCloudShadow.fragment(Gdx.files.classpath("megamek/client/ui/clientGUI/boardview/gpu/water-surface.frag")
                    .readString(), true)));
        private final DefaultShader.Config liquidShader = new DefaultShader.Config(config.vertexShader,
              GpuLiquidShader.fragment(config.fragmentShader));
        private final DefaultShader.Config waterLiquidShader = new DefaultShader.Config(config.vertexShader,
              GpuLiquidShader.fragment(waterShader.fragmentShader));

        @Override
        protected Shader createShader(Renderable renderable) {
            DefaultShader.Config chosen = renderable.material.has(Ground.TYPE) ? groundShader
                  : renderable.material.has(GpuLiquidShader.Frame.TYPE)
                        ? renderable.material.has(GpuWaterShader.TYPE) ? waterLiquidShader : liquidShader
                  : renderable.material.has(GpuWaterShader.TYPE) ? waterShader : config;
            DefaultShader result = new DefaultShader(renderable, chosen, GpuCloudShadow.prefix(renderable, chosen)) {
                private final int normalMapsUniform = register("u_normalMaps");
                private final int wetnessUniform = register("u_wetness");
                private final int viewDirectionUniform = register("u_viewDirection");
                private final int rainNoiseUniform = register("u_rainNoise");
                private final int rainScaleUniform = register("u_rainScale");
                private final int rainTimeUniform = register("u_rainTime");
                private final int rippleDetailUniform = register("u_rippleDetail");
                private final int rainDetailUniform = register("u_rainDetail");
                private final int skyUniform = register("u_rainSky");
                private final int horizonUniform = register("u_rainHorizon");
                private final int waterEffectsUniform = register("u_waterEffects");

                @Override
                public void begin(Camera camera, RenderContext context) {
                    super.begin(camera, context);
                    set(normalMapsUniform, normalMaps ? 1f : 0f);
                    set(wetnessUniform, wetness);
                    set(viewDirectionUniform, camera.direction);
                    set(waterEffectsUniform, waterEffects ? 1f : 0f);
                    if (chosen == groundShader || chosen == waterShader || chosen == waterLiquidShader) {
                        set(rainNoiseUniform, rainNoise);
                        set(rainScaleUniform, 1f / BoardGeometry.WIDTH);
                        set(rainTimeUniform, clock);
                        float zoom = camera instanceof OrthographicCamera ortho ? ortho.zoom : 1;
                        float hexPixels = BoardGeometry.WIDTH * Gdx.graphics.getBackBufferWidth()
                              / (camera.viewportWidth * zoom);
                        set(rippleDetailUniform, MathUtils.clamp((hexPixels * Math.abs(camera.direction.z) - 28) / 60, 0, 1));
                        set(rainDetailUniform, MathUtils.clamp((hexPixels - 12) / 28, 0, 1));
                        Color sky = atmosphere == null ? Color.GRAY : atmosphere.sky();
                        Color horizon = atmosphere == null ? Color.LIGHT_GRAY : atmosphere.horizon();
                        set(skyUniform, sky.r, sky.g, sky.b);
                        set(horizonUniform, horizon.r, horizon.g, horizon.b);
                    }
                }
            };
            GpuCloudShadow.register(result);
            GpuLiquidShader.register(result);
            GpuWaterShader.register(result);
            result.register("u_groundResponse", new BaseShader.LocalSetter() {
                @Override
                public void set(BaseShader target, int id, Renderable renderable, Attributes attributes) {
                    Ground ground = attributes.get(Ground.class, Ground.TYPE);
                    if (ground != null) { target.set(id, ground.value); }
                }
            });
            return result;
        }
    });
    private final ModelBatch depthBatch = new ModelBatch(new DepthShaderProvider(null,
          Gdx.files.classpath("megamek/client/ui/clientGUI/boardview/gpu/shadow-depth.frag").readString()),
          new GpuOpaqueSorter());
    private final Environment environment = new Environment();
    private final List<Chunk> chunks = new ArrayList<>();
    private final List<Model> shadowModels = new ArrayList<>();
    private final List<Matrix4> shadowTransforms = new ArrayList<>();
    private final com.badlogic.gdx.utils.IntArray shadowPoses = new com.badlogic.gdx.utils.IntArray();
    private final Map<Model, List<Vector3>> featureTriangles = new HashMap<>();
    private final BoundingBox shadowBounds = new BoundingBox();
    private final Matrix4 shadowView = new Matrix4();
    private final Matrix4 shadowProjection = new Matrix4();
    private final OrthographicCamera shadowFit = new OrthographicCamera();
    private final UnitBounds.Frame frameBounds;
    private boolean shadowViewPresent;
    private List<BoardScene.Tile> tiles;
    private Map<Coords, BoardFlow.Current> currents = Map.of();
    private BoardScene.Light light;
    private BoardAtmosphere.Lighting atmosphere;
    private DirectionalShadowLight shadow;
    private boolean shadowDirty;
    private BoardGeometry.Tuning tuning;
    private float clock;
    private float floor;
    private int chunkRows;
    private float buildingOpacity = DEFAULT_BUILDING_OPACITY;
    private boolean normalMaps = true;
    private boolean waterEffects = true;
    private float wetness;
    private float detailPixelsPerUnit = Float.NaN;
    private boolean hasCutaways;
    private boolean flatTrees;

    /** Tiny shared, mipmapped field: mask and sky variation; allocated once, never updated per frame. */
    private static Texture rainNoise() {
        Pixmap pixels = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        Random random = new Random(0x7261696eL);
        Random waterRandom = new Random(0x7761746572L);
        try {
            for (int y = 0; y < 64; y++) {
                for (int x = 0; x < 64; x++) {
                    int value = random.nextInt(256);
                    // Keep the existing red-channel puddle field; water borrows independent green/blue noise.
                    pixels.drawPixel(x, y, (value << 24) | (waterRandom.nextInt(256) << 16) | (waterRandom.nextInt(256) << 8) | 255);
                }
            }
            Texture texture = new Texture(pixels, true);
            texture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
            texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
            return texture;
        } finally {
            pixels.dispose();
        }
    }

    /** Ground and water use the same rain field, lighting and geometry-shadow response. */
    private static String rainFragment(String source) {
        String functions = Gdx.files.classpath("megamek/client/ui/clientGUI/boardview/gpu/rain-surface.glsl").readString();
        functions += Gdx.files.classpath("megamek/client/ui/clientGUI/boardview/gpu/surface-lighting.glsl").readString();
        return source.replace("void main() {", functions + "\nvoid main() {");
    }

    GpuTerrain() {
        this(null);
    }

    /** Lets native integration checks exercise the GIF fallback without changing the production constant. */
    GpuTerrain(boolean liquidShaderAnimation) {
        this(liquidShaderAnimation, false);
    }

    /** Native A/B checks share all effects and differ only in the source of the water color. */
    GpuTerrain(boolean liquidShaderAnimation, boolean proceduralWater) {
        this(null, null, liquidShaderAnimation, proceduralWater);
    }

    /** The battle view owns this library; terrain borrows the equipment buffers without disposing them. */
    GpuTerrain(GpuUnitModels unitModels) {
        this(unitModels, null);
    }

    /** Borrow the battle view's bounds only between its post-animation begin and the end of that render frame. */
    GpuTerrain(GpuUnitModels unitModels, UnitBounds.Frame frameBounds) {
        this(unitModels, frameBounds, GpuLiquidShader.USE_SHADER_ANIMATION, GpuWaterShader.USE_PROCEDURAL_WATER);
    }

    private GpuTerrain(GpuUnitModels unitModels, UnitBounds.Frame frameBounds, boolean liquidShaderAnimation, boolean proceduralWater) {
        this.unitModels = unitModels;
        this.frameBounds = frameBounds;
        this.liquidShaderAnimation = liquidShaderAnimation;
        this.proceduralWater = proceduralWater;
        assets = new GpuAssets();
    }

    private static final class Prop {
        private final Coords coords;
        private final BoundingBox bounds;
        private final String treeAsset;
        private final Model pickingModel;
        private final float treeDiameter;
        private final ModelInstance instance;

        Prop(Coords coords, ModelInstance instance, BoundingBox bounds, String treeAsset) {
            this.coords = coords;
            this.instance = instance;
            this.bounds = bounds;
            this.treeAsset = treeAsset;
            pickingModel = instance.model;
            treeDiameter = treeAsset == null ? 0 : bounds.getDimensions(new Vector3()).len();
        }

        Coords coords() { return coords; }
        ModelInstance instance() { return instance; }
        BoundingBox bounds() { return bounds; }
        boolean tree() { return treeAsset != null; }

    }
    private record LiquidSurface(Material material, BoardLiquid.Textures source, boolean falling, BoardFlow.Current current) { }

    /** Marks exposed ground and carries its water-film response; negative excludes snow, ice and water. */
    private static final class Ground extends FloatAttribute {
        static final long TYPE = register("boardGround");

        Ground(float response) {
            super(TYPE, response);
        }

        @Override
        public Ground copy() {
            return new Ground(value);
        }
    }

    private static final class Chunk implements Disposable {
        final List<ModelInstance> opaque = new ArrayList<>();
        final List<ModelInstance> scatter = new ArrayList<>();
        float scatterDiameter;
        boolean scatterVisible = true;
        final List<ModelInstance> overlays = new ArrayList<>();
        final List<ModelInstance> water = new ArrayList<>();
        final List<LiquidSurface> liquidMaterials = new ArrayList<>();
        final List<ModelInstance> tactical = new ArrayList<>();
        final List<ModelInstance> flatTrees = new ArrayList<>();
        final List<Prop> props = new ArrayList<>();
        final List<Prop> cutaways = new ArrayList<>();
        final List<Array<Renderable>> treeRenderables = List.of(new Array<>(), new Array<>(), new Array<>());
        float treeDiameter;
        int treeLod;
        final List<ModelInstance> struts = new ArrayList<>();
        final Array<Renderable> propRenderables = new Array<>();
        final Array<Renderable> shadowPropRenderables = new Array<>();
        Set<Prop> faded = Set.of();
        final RenderableProvider solidProps = (out, pool) -> supplyProps(
              faded.isEmpty() ? shadowPropRenderables : propRenderables, out);
        final RenderableProvider shadowProps = (out, pool) -> supplyProps(shadowPropRenderables, out);
        final RenderableProvider trees = (out, pool) -> supplyProps(treeRenderables.get(treeLod), out);
        final BoundingBox bounds = new BoundingBox().inf();

        private static void supplyProps(Array<Renderable> source, Array<Renderable> out) {
            for (Renderable renderable : source) {
                renderable.shader = null;
                renderable.environment = null;
            }
            out.addAll(source);
        }

        void cacheProps() {
            if (cutaways.isEmpty() && struts.isEmpty()) { return; }
            if (shadowPropRenderables.isEmpty()) {
                cacheProps(shadowPropRenderables, false);
                // A shadow always uses the original opaque materials, independently of live instance fading.
                for (Renderable renderable : shadowPropRenderables) {
                    renderable.material = new Material(renderable.material);
                    renderable.material.remove(BlendingAttribute.Type);
                    renderable.material.remove(DepthTestAttribute.Type);
                    renderable.material.remove(IntAttribute.CullFace);
                }
            }
            disposePropMeshes(propRenderables);
            // Share the complete cache for normal rendering; only occupied chunks need a second mesh cache.
            if (!faded.isEmpty()) {
                cacheProps(propRenderables, true);
            }
        }

        private void cacheProps(Array<Renderable> destination, boolean omitFaded) {
            cache(destination, builder -> {
                struts.forEach(builder::add);
                for (Prop prop : cutaways) {
                    if (!omitFaded || !faded.contains(prop)) {
                        builder.add(prop.instance());
                    }
                }
            });
        }

        /** Each of the three tree batches is built at most once per terrain generation, on first use. */
        void cacheTrees(GpuAssets assets) {
            Array<Renderable> destination = treeRenderables.get(treeLod);
            if (treeDiameter == 0 || !destination.isEmpty()) { return; }
            cache(destination, builder -> {
                for (Prop prop : props) {
                    if (prop.tree()) {
                        builder.add(treeLod == 0 ? prop.instance() : new ModelInstance(
                              assets.model(TreeLod.asset(prop.treeAsset, treeLod)), prop.instance().transform));
                    }
                }
            });
        }

        private static void cache(Array<Renderable> destination, Consumer<ModelCache> submit) {
            // ModelCache's default pool reserves 65,536 vertices per chunk. Its mesh builder also retains
            // scratch arrays after end(). Use tight meshes, and transfer those static meshes to this chunk
            // so the temporary builder and sorting/pooling buffers can be collected after each build.
            ModelCache.TightMeshPool meshes = new ModelCache.TightMeshPool();
            ModelCache builder = new ModelCache(new ModelCache.Sorter(), meshes);
            try {
                builder.begin();
                submit.accept(builder);
                builder.end();
                builder.getRenderables(destination, null);
            } catch (RuntimeException | Error failure) {
                meshes.dispose();
                destination.clear();
                throw failure;
            }
        }

        private static void disposePropMeshes(Array<Renderable> renderables) {
            Set<Mesh> meshes = new HashSet<>();
            for (Renderable renderable : renderables) {
                meshes.add(renderable.meshPart.mesh);
            }
            meshes.forEach(Mesh::dispose);
            renderables.clear();
        }

        @Override
        public void dispose() {
            disposePropMeshes(propRenderables);
            disposePropMeshes(shadowPropRenderables);
            treeRenderables.forEach(Chunk::disposePropMeshes);
            for (List<ModelInstance> layer : List.of(opaque, scatter, overlays, water, tactical, flatTrees)) {
                layer.forEach(instance -> instance.model.dispose());
            }
        }
    }

    /** Collect by material before opening a mesh part: ModelBuilder has only one active part at a time. */
    private static final class Layer {
        final Map<Material, List<Consumer<MeshPartBuilder>>> geometry = new LinkedHashMap<>();

        void add(Material material, Consumer<MeshPartBuilder> shape) {
            geometry.computeIfAbsent(material, key -> new ArrayList<>()).add(shape);
        }

        void finish(List<ModelInstance> destination) {
            if (geometry.isEmpty()) {
                return;
            }
            ModelBuilder builder = new ModelBuilder();
            builder.begin();
            int index = 0;
            for (var entry : geometry.entrySet()) {
                MeshPartBuilder mesh = builder.part("surface-" + index++, GL20.GL_TRIANGLES, ATTRIBUTES, entry.getKey());
                entry.getValue().forEach(shape -> shape.accept(mesh));
            }
            destination.add(new ModelInstance(builder.end()));
        }
    }

    void update(BoardScene scene) {
        BoardGeometry.Tuning nextTuning = BoardGeometry.tuning();
        boolean changedTuning = tuning == null || tuning.hexScale() != nextTuning.hexScale()
              || tuning.levelHeight() != nextTuning.levelHeight() || tuning.gridShade() != nextTuning.gridShade();
        boolean changedLimbScale = limbModel != null && tuning != null && tuning.unitScale() != nextTuning.unitScale();
        boolean changedLight = !Objects.equals(light, scene.light());
        if (tiles == scene.tiles() && !changedTuning && !changedLight && !changedLimbScale) {
            tuning = nextTuning;
            return;
        }
        Map<GroundSlot, BoardScene.Pixels> terrainPixels = new HashMap<>();
        Map<GroundSlot, BoardScene.Pixels> normalPixels = new HashMap<>();
        Map<Coords, BoardScene.Pixels> decalPixels = new HashMap<>();
        Map<Coords, BoardScene.Pixels> tacticalPixels = new HashMap<>();
        Map<Coords, BoardScene.Pixels> foliagePixels = new HashMap<>();
        float nextFloor = BoardGeometry.floor(scene);
        boolean rebuildAll = changedTuning || tiles == null || tiles.size() != scene.tiles().size() || nextFloor != floor;
        boolean changedFlow = rebuildAll;
        Set<Coords> changedChunks = new HashSet<>();
        for (int index = 0; index < scene.tiles().size(); index++) {
            BoardScene.Tile tile = scene.tiles().get(index);
            if (limbModel == null && unitModels != null
                  && tile.features().stream().anyMatch(feature -> feature.kind() == BoardScene.FeatureKind.LIMB)) {
                var limb = unitModels.equipment("Limb Club");
                limbModel = limb == null ? null : limb.model();
            }
            BoardRim.Images material = rims.material(scene, tile, nextFloor, assets);
            GroundSlot topSlot = new GroundSlot(tile.coords(), true);
            GroundSlot baseSlot = new GroundSlot(tile.coords(), false);
            terrainPixels.put(topSlot, material.color());
            // Riverbanks borrow adjacent ground art, never its cliff-top decoration.
            // The atlas shares these slots whenever the top has no rim.
            terrainPixels.put(baseSlot, tile.ground());
            if (material.normal() != null) {
                normalPixels.put(topSlot, material.normal());
            }
            if (tile.normals() != null) {
                normalPixels.put(baseSlot, tile.normals());
            }
            if (decals(tile) != null) {
                decalPixels.put(tile.coords(), decals(tile));
            }
            if (tile.tactical() != null) {
                tacticalPixels.put(tile.coords(), tile.tactical());
            }
            if (tile.foliage() != null) { foliagePixels.put(tile.coords(), tile.foliage()); }
            if (!rebuildAll) {
                if (changedLimbScale && tile.features().stream().anyMatch(feature -> feature.kind() == BoardScene.FeatureKind.LIMB)) {
                    dirtyChunk(changedChunks, tile.coords());
                }
                BoardScene.Tile before = tiles.get(index);
                changedFlow |= !before.coords().equals(tile.coords()) || before.elevation() != tile.elevation()
                      || before.frozen() != tile.frozen() || !before.liquid().equals(tile.liquid());
                // Rim vertex colors come from the selected ground artwork, not the atlas texture.
                if (!before.ground().equals(tile.ground())) {
                    dirtyChunk(changedChunks, tile.coords());
                }
                if (!before.coords().equals(tile.coords())) {
                    rebuildAll = true;
                } else if (before.elevation() != tile.elevation()
                      || before.waterDepth() != tile.waterDepth() || before.frozen() != tile.frozen()
                      || !before.liquid().equals(tile.liquid())
                      || before.roadExits() != tile.roadExits()
                      || before.surface() != tile.surface()
                      || !before.features().equals(tile.features())) {
                    dirtyChunk(changedChunks, tile.coords());
                    for (int direction = 0; direction < 6; direction++) {
                        Coords neighbor = tile.coords().translated(direction);
                        if (scene.tile(neighbor) != null) {
                            dirtyChunk(changedChunks, neighbor);
                        }
                    }
                }
            }
        }
        if (changedFlow) {
            Map<Coords, BoardFlow.Current> next = BoardFlow.calculate(scene);
            // An outlet edit can reverse a distant flat reach, beyond the edited hex's immediate neighbors.
            Set<Coords> affected = new HashSet<>(currents.keySet());
            affected.addAll(next.keySet());
            for (Coords coords : affected) {
                if (!Objects.equals(currents.get(coords), next.get(coords))) { dirtyChunk(changedChunks, coords); }
            }
            currents = next;
        }
        rims.retainUsed();
        rebuildAll |= ground.update(terrainPixels, normalPixels);
        rebuildAll |= decals.update(decalPixels);
        rebuildAll |= foliage.update(foliagePixels);
        boolean markingsChanged = tactical.update(tacticalPixels);
        tiles = scene.tiles();
        tuning = nextTuning;
        floor = nextFloor;
        if (rebuildAll) {
            chunks.forEach(Chunk::dispose);
            chunks.clear();
        }
        int chunkIndex = 0;
        chunkRows = (scene.height() + CHUNK_SIZE - 1) / CHUNK_SIZE;
        for (int x = 0; x < scene.width(); x += CHUNK_SIZE) {
            for (int y = 0; y < scene.height(); y += CHUNK_SIZE) {
                if (rebuildAll) {
                    chunks.add(build(scene, x, y, floor));
                } else if (changedChunks.contains(new Coords(x / CHUNK_SIZE, y / CHUNK_SIZE))) {
                    chunks.get(chunkIndex).dispose();
                    chunks.set(chunkIndex, build(scene, x, y, floor));
                } else if (markingsChanged) {
                    buildMarkings(scene, chunks.get(chunkIndex), x, y);
                }
                chunkIndex++;
            }
        }
        if (rebuildAll || !changedChunks.isEmpty() || changedLight) {
            updateLight(scene.light());
        }
        hasCutaways = chunks.stream().anyMatch(chunk -> !chunk.cutaways.isEmpty());
    }

    private static void dirtyChunk(Set<Coords> chunks, Coords coords) {
        chunks.add(new Coords(coords.getX() / CHUNK_SIZE, coords.getY() / CHUNK_SIZE));
    }

    private BoardScene.Pixels decals(BoardScene.Tile tile) {
        return limbModel != null && tile.decalsWithoutLimbs() != null ? tile.decalsWithoutLimbs() : tile.decals();
    }

    private Chunk build(BoardScene scene, int startX, int startY, float floor) {
        Chunk chunk = new Chunk();
        Layer solid = new Layer();
        Layer scatter = new Layer();
        Material scatterMaterial = new Material(ColorAttribute.createDiffuse(Color.WHITE));
        Layer overlay = new Layer();
        Layer trees = new Layer();
        Layer liquid = new Layer();
        Map<String, LiquidSurface> animations = new HashMap<>();
        for (int x = startX; x < Math.min(scene.width(), startX + CHUNK_SIZE); x++) {
            for (int y = startY; y < Math.min(scene.height(), startY + CHUNK_SIZE); y++) {
                BoardScene.Tile tile = scene.tile(new Coords(x, y));
                BoardSurface surface = new BoardSurface(scene, tile);
                TextureRegion top = ground.region(new GroundSlot(tile.coords(), true));
                for (BoardSurface.Face face : surface.faces) {
                    boolean artwork = !tile.liquid().molten() && (face.finish() == BoardSurface.Finish.TOP
                          || face.finish() == BoardSurface.Finish.ICE || face.finish() == BoardSurface.Finish.SHORE);
                    BoardScene.Tile land = face.landEdge() < 0 ? null
                          : scene.tile(tile.coords().translated(BoardGeometry.edgeDirection(face.landEdge())));
                    if (land != null && !land.liquid().present()) {
                        TextureRegion bankArt = ground.region(new GroundSlot(land.coords(), false));
                        solid.add(groundMaterial(bankArt.getTexture(), land),
                              mesh -> bank(mesh, tile.coords(), face, land.coords(), bankArt));
                    } else {
                        Texture texture = artwork ? top.getTexture() : assets.material(tile.liquid().molten() ? "terrain/rock"
                              : face.finish() == BoardSurface.Finish.BED ? "bed"
                              : tile.liquid().present() ? "terrain/sand" : tile.surface().wall);
                        solid.add(artwork ? groundMaterial(texture, tile) : material(texture, false),
                              mesh -> surface(mesh, tile.coords(), face, artwork ? top : null, 0));
                    }
                    chunk.bounds.ext(face.a()).ext(face.b()).ext(face.c());
                    if (face.finish() == BoardSurface.Finish.SHORE) {
                        overlay.add(material(assets.material(tile.liquid().molten() ? "terrain/rock" : "terrain/sand"), true),
                              mesh -> shore(mesh, tile, face));
                    }
                    if (decals(tile) != null && artwork) {
                        TextureRegion art = decals.region(tile.coords());
                        overlay.add(material(art.getTexture(), true), mesh -> surface(mesh, tile.coords(), face, art, 0.08f));
                    }
                }
                if (tile.foliage() != null) {
                    TextureRegion art = foliage.region(tile.coords());
                    trees.add(material(art.getTexture(), true), mesh -> foliage(mesh, tile, art));
                    float height = BoardGeometry.surfaceZ(tile) + .16f * BoardGeometry.HEX_SCALE;
                    chunk.bounds.ext(BoardGeometry.centerX(tile.coords()) - BoardGeometry.WIDTH / 2,
                          BoardGeometry.centerY(tile.coords()) - BoardGeometry.HEIGHT / 2, height);
                    chunk.bounds.ext(BoardGeometry.centerX(tile.coords()) + BoardGeometry.WIDTH / 2,
                          BoardGeometry.centerY(tile.coords()) + BoardGeometry.HEIGHT / 2, height);
                }
                if (!tile.liquid().present() && tile.roadExits() == 0 && surface.ramps == 0 && BoardGeometry.tuning().gridShade() < 1) {
                    solid.add(groundMaterial(top.getTexture(), tile),
                          mesh -> grid(mesh, tile.coords(), BoardGeometry.groundZ(tile), top));
                }
                for (BoardSurface.Side side : surface.sides(scene, floor)) {
                    Texture wall = assets.material(tile.surface().wall);
                    solid.add(material(wall, false), mesh -> wall(mesh, side));
                    chunk.bounds.ext(side.a().x, side.a().y, side.lowA()).ext(side.b().x, side.b().y, side.lowB());
                    if (!tile.liquid().present()) {
                        overlay.add(material(assets.material(tile.surface().rim), true), mesh -> cornice(mesh, tile, side));
                    }
                }
                if (!surface.water.isEmpty()) {
                    BoardLiquid.Textures source = tile.liquid().textures(tile.waterDepth(), tile.elevation());
                    BoardFlow.Current current = currents.getOrDefault(tile.coords(), BoardFlow.Current.STILL);
                    Material water = liquidMaterial(scene, surface, source, false, current);
                    water.id += ":" + current;
                    boolean animatedFrames = !proceduralWater || tile.liquid().molten();
                    if (animatedFrames) { animations.put(water.id, new LiquidSurface(water, source, false, current)); }
                    TextureRegion waterArt = new TextureRegion(water.get(TextureAttribute.class, TextureAttribute.Diffuse).textureDescription.texture);
                    // Molten material writes opaque depth; water and hazardous pools reveal their beds and units.
                    Layer destination = tile.liquid().molten() ? solid : liquid;
                    for (BoardSurface.Face face : surface.waterFaces) {
                        destination.add(water, mesh -> surface(mesh, tile.coords(), face, waterArt, 0));
                        chunk.bounds.ext(face.a()).ext(face.b()).ext(face.c());
                    }
                    GpuWaterShader waterSurface = water.get(GpuWaterShader.class, GpuWaterShader.TYPE);
                    if (waterSurface != null) {
                        for (GpuWaterShader.Impact impact : waterSurface.impacts) {
                            destination.add(water, mesh -> splash(mesh, impact));
                            chunk.bounds.ext(impact.center().x, impact.center().y, impact.center().z + impact.radius());
                        }
                    }
                    if (!surface.waterfalls.isEmpty()) {
                        Material fall = liquidMaterial(scene, surface, source, true, BoardFlow.Current.STILL);
                        if (animatedFrames) { animations.put(fall.id, new LiquidSurface(fall, source, true, BoardFlow.Current.STILL)); }
                        Material back = null;
                        if (!tile.liquid().molten()) {
                            fall.set(IntAttribute.createCullFace(GL20.GL_BACK));
                            // Keep the inward face visible through the upper surface, with half the front's opacity.
                            back = new Material(fall);
                            back.set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, 0.4f));
                            back.set(IntAttribute.createCullFace(GL20.GL_FRONT));
                        }
                        for (BoardSurface.Side drop : surface.waterfalls) {
                            destination.add(fall, mesh -> waterfall(mesh, drop));
                            if (back != null) { destination.add(back, mesh -> waterfall(mesh, drop)); }
                            // The landing water spreads into the receiving hex, so its reach belongs in the bounds.
                            Vector3 outward = fallOutward(drop);
                            float spread = .5f * BoardSurface.fallLip(drop.a().z, drop.lowA())
                                  + FALL_CLEARANCE * BoardGeometry.HEX_SCALE;
                            chunk.bounds.ext(drop.a().x + outward.x * spread, drop.a().y + outward.y * spread, drop.lowA());
                            chunk.bounds.ext(drop.b().x + outward.x * spread, drop.b().y + outward.y * spread, drop.lowB());
                        }
                    }
                }
                for (BoardScene.Feature feature : tile.features()) {
                    if (feature.kind() == BoardScene.FeatureKind.SCATTER) {
                        // Road approaches can extend into a hex that has no road terrain of its own.
                        if (!tile.liquid().present() && surface.ramps == 0) {
                            scatter.add(scatterMaterial, mesh -> GpuScatter.build(mesh, tile, surface, feature));
                            chunk.scatterDiameter = Math.max(chunk.scatterDiameter, GpuScatter.diameter(feature));
                            chunk.bounds.ext(BoardGeometry.centerX(tile.coords()), BoardGeometry.centerY(tile.coords()),
                                  (tile.elevation() + feature.height()) * BoardGeometry.LEVEL);
                        }
                        continue;
                    }
                    boolean limb = feature.kind() == BoardScene.FeatureKind.LIMB;
                    if (limb && limbModel == null) {
                        continue;
                    }
                    String asset = feature.kind() == BoardScene.FeatureKind.TREE
                          ? TreeLod.asset(feature.asset(), 0) : feature.asset();
                    ModelInstance instance = new ModelInstance(limb ? limbModel : assets.model(asset));
                    for (Material material : instance.materials) {
                        if (material.id.equals("wall")) {
                            material.get(TextureAttribute.class, TextureAttribute.Diffuse).scaleV = feature.height();
                        }
                    }
                    float px = BoardGeometry.centerX(tile.coords()) + feature.x() * BoardGeometry.HEX_SCALE;
                    float py = BoardGeometry.centerY(tile.coords()) + feature.y() * BoardGeometry.HEX_SCALE;
                    // Zero-elevation decks otherwise coincide with the dry riverbank and road decals.
                    float base = feature.asset().equals("bridge")
                          ? tile.elevation() * BoardGeometry.LEVEL + 0.16f * BoardGeometry.HEX_SCALE
                          : surface.height(px, py);
                    if (limb) {
                        // Lay the held limb on its side, then ground its actual bounds. No unit/entity is created.
                        float scale = feature.scale() * BoardGeometry.UNIT_SCALE * BoardGeometry.HEX_SCALE;
                        instance.transform.setToTranslation(px, py, base).rotate(Vector3.Z, feature.rotation())
                              .rotate(Vector3.Y, 90).scale(scale, scale, scale);
                        BoundingBox bounds = instance.calculateBoundingBox(new BoundingBox()).mul(instance.transform);
                        instance.transform.val[Matrix4.M23] += base - bounds.min.z + .12f * BoardGeometry.HEX_SCALE;
                    } else {
                        instance.transform.setToTranslation(px, py,
                              base + feature.elevation() * BoardGeometry.LEVEL)
                              .rotate(Vector3.Z, feature.rotation())
                              .scale(feature.scale() * BoardGeometry.HEX_SCALE, feature.scale() * BoardGeometry.HEX_SCALE,
                                    feature.height() * BoardGeometry.LEVEL);
                    }
                    BoundingBox bounds = instance.calculateBoundingBox(new BoundingBox()).mul(instance.transform);
                    chunk.props.add(new Prop(tile.coords(), instance, bounds,
                          feature.kind() == BoardScene.FeatureKind.TREE ? feature.asset() : null));
                    if (feature.kind() == BoardScene.FeatureKind.BUILDING) {
                        Model interior = assets.interior(feature.asset(), Math.round(feature.height()));
                        ModelInstance struts = new ModelInstance(interior, "struts");
                        struts.transform.set(instance.transform);
                        chunk.struts.add(struts);
                        ModelInstance floors = new ModelInstance(interior, "floors");
                        floors.transform.set(instance.transform);
                        chunk.props.add(new Prop(tile.coords(), floors,
                              floors.calculateBoundingBox(new BoundingBox()).mul(floors.transform), null));
                    }
                    chunk.bounds.ext(bounds);
                }
            }
        }
        solid.finish(chunk.opaque);
        scatter.finish(chunk.scatter);
        overlay.finish(chunk.overlays);
        trees.finish(chunk.flatTrees);
        liquid.finish(chunk.water);
        // The floating markings remain visible when only their raised edge enters the viewport.
        chunk.bounds.ext(chunk.bounds.max.x, chunk.bounds.max.y, chunk.bounds.max.z + BoardGeometry.LEVEL / 3);
        buildMarkings(scene, chunk, startX, startY);
        // ModelInstance copies materials; animate those owned by the rendered instances.
        for (List<ModelInstance> layer : List.of(chunk.opaque, chunk.water)) {
            for (ModelInstance instance : layer) {
                for (Material material : instance.materials) {
                    LiquidSurface animation = animations.get(material.id);
                    if (animation != null) {
                        chunk.liquidMaterials.add(new LiquidSurface(material, animation.source(), animation.falling(), animation.current()));
                    }
                }
            }
        }
        for (Prop prop : chunk.props) {
            if (prop.tree()) {
                chunk.treeDiameter = Math.max(chunk.treeDiameter, prop.treeDiameter);
            } else {
                chunk.cutaways.add(prop);
            }
        }
        chunk.cacheProps();
        detailPixelsPerUnit = Float.NaN;
        return chunk;
    }

    private void buildMarkings(BoardScene scene, Chunk chunk, int startX, int startY) {
        chunk.tactical.forEach(instance -> instance.model.dispose());
        chunk.tactical.clear();
        Layer marks = new Layer();
        for (int x = startX; x < Math.min(scene.width(), startX + CHUNK_SIZE); x++) {
            for (int y = startY; y < Math.min(scene.height(), startY + CHUNK_SIZE); y++) {
                BoardScene.Tile tile = scene.tile(new Coords(x, y));
                if (tile.tactical() == null) {
                    continue;
                }
                TextureRegion art = tactical.region(tile.coords());
                Material mark = material(art.getTexture(), true);
                // The shared blended material tests opaque depth without hiding later annotations.
                marks.add(mark, mesh -> markingHex(mesh, tile, art));
            }
        }
        marks.finish(chunk.tactical);
    }

    private Material groundMaterial(Texture texture, BoardScene.Tile tile) {
        Material material = material(texture, false);
        float response = tile.liquid().present() || tile.frozen() ? -1 : switch (tile.surface()) {
            case SNOW -> -1;
            case SAND -> 0.05f;
            case DIRT -> 0.15f;
            case GRASS -> 0.25f;
            case ROCK -> 0.7f;
            case CONCRETE -> 1;
        };
        material.set(new Ground(response));
        Texture normal = ground.normal(texture);
        if (normal != null) {
            material.set(TextureAttribute.createNormal(normal));
        }
        return material;
    }

    private static Material material(Texture texture, boolean blend) {
        Material material = new Material("surface", TextureAttribute.createDiffuse(texture),
              IntAttribute.createCullFace(GL20.GL_NONE));
        if (blend) {
            material.set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA));
            material.set(new DepthTestAttribute(GL20.GL_LEQUAL, false));
        }
        return material;
    }

    private Material liquidMaterial(BoardScene scene, BoardSurface surface, BoardLiquid.Textures source, boolean falling, BoardFlow.Current current) {
        BoardLiquid liquid = surface.tile.liquid();
        boolean procedural = proceduralWater && !liquid.molten();
        Texture texture = procedural ? rainNoise : assets.liquid(source, 0);
        Material material = material(texture, !liquid.molten());
        material.id = (falling ? "falls:" : "liquid:") + liquid.kind() + ":" + source.base() + ":" + source.foam();
        if (liquidShaderAnimation && !procedural) {
            material.set(new GpuLiquidShader.Frame(texture), new FloatAttribute(GpuLiquidShader.Frame.BLEND, 0));
        }
        if (liquid.molten()) {
            material.set(ColorAttribute.createDiffuse(0.35f, 0.35f, 0.35f, 1));
            material.set(TextureAttribute.createEmissive(texture), ColorAttribute.createEmissive(0.65f, 0.65f, 0.65f, 1));
        } else {
            material.set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA,
                  falling ? 0.8f : GpuWaterShader.SURFACE_OPACITY));
            if (falling) { material.set(ColorAttribute.createAmbient(assets.materialTint("bed"))); }
            GpuWaterShader water = new GpuWaterShader(scene, surface, procedural, falling, current);
            material.set(water);
            if (!water.impacts.isEmpty()) { material.id += ":splash:" + surface.tile.coords(); }
            if (liquid.kind() == BoardLiquid.Kind.HAZARDOUS) {
                material.set(ColorAttribute.createDiffuse(0.4f, 1, 0.12f, 1));
            }
        }
        return material;
    }

    private static MeshPartBuilder.VertexInfo vertex(Vector3 p, Vector3 normal, float u, float v, Color color) {
        return new MeshPartBuilder.VertexInfo().setPos(p).setNor(normal).setUV(u, v).setCol(color);
    }

    private static MeshPartBuilder.VertexInfo topVertex(Vector3 p, Coords coords, TextureRegion region, Color color) {
        // Sample just inside the artwork's alpha border, without moving the actual geometry.
        float u = 0.5f + (p.x - BoardGeometry.centerX(coords)) / BoardGeometry.WIDTH * BoardRim.GROUND_UV_SCALE;
        float v = 0.5f - (p.y - BoardGeometry.centerY(coords)) / BoardGeometry.HEIGHT * BoardRim.GROUND_UV_SCALE;
        return vertex(p, Vector3.Z, region.getU() + u * (region.getU2() - region.getU()),
              region.getV() + v * (region.getV2() - region.getV()), color);
    }

    /** One horizontal plane per hex; terrain and structures can occlude it, but cannot bend its outline. */
    private static void markingHex(MeshPartBuilder mesh, BoardScene.Tile tile, TextureRegion region) {
        Coords coords = tile.coords();
        Vector3 center = BoardGeometry.center(coords, 0);
        center.z = BoardGeometry.surfaceZ(tile) + BoardGeometry.LEVEL / 3;
        for (int edge = 0; edge < 6; edge++) {
            Vector3 a = BoardGeometry.corner(coords, 0, edge);
            Vector3 b = BoardGeometry.corner(coords, 0, edge + 1);
            a.z = b.z = center.z;
            mesh.triangle(markingVertex(center, coords, region), markingVertex(a, coords, region),
                  markingVertex(b, coords, region));
        }
    }

    private static MeshPartBuilder.VertexInfo markingVertex(Vector3 point, Coords coords, TextureRegion region) {
        float u = 0.5f + (point.x - BoardGeometry.centerX(coords)) / BoardGeometry.WIDTH;
        float v = 0.5f - (point.y - BoardGeometry.centerY(coords)) / BoardGeometry.HEIGHT;
        return vertex(point, Vector3.Z, region.getU() + u * (region.getU2() - region.getU()),
              region.getV() + v * (region.getV2() - region.getV()), Color.WHITE);
    }

    /** Preserve the full transparent sprite: canopies extend beyond the hex's diagonal edges. */
    private static void foliage(MeshPartBuilder mesh, BoardScene.Tile tile, TextureRegion art) {
        var center = BoardGeometry.center(tile.coords(), 0);
        center.z = BoardGeometry.surfaceZ(tile) + .16f * BoardGeometry.HEX_SCALE;
        float halfWidth = BoardGeometry.WIDTH / 2, halfHeight = BoardGeometry.HEIGHT / 2;
        mesh.rect(markingVertex(new Vector3(center).add(-halfWidth, -halfHeight, 0), tile.coords(), art),
              markingVertex(new Vector3(center).add(halfWidth, -halfHeight, 0), tile.coords(), art),
              markingVertex(new Vector3(center).add(halfWidth, halfHeight, 0), tile.coords(), art),
              markingVertex(new Vector3(center).add(-halfWidth, halfHeight, 0), tile.coords(), art));
    }

    private static void surface(MeshPartBuilder mesh, Coords coords, BoardSurface.Face face,
          TextureRegion art, float lift) {
        Vector3 normal = new Vector3(face.b()).sub(face.a()).crs(new Vector3(face.c()).sub(face.a())).nor();
        mesh.triangle(surfaceVertex(face.a(), coords, normal, art, lift),
              surfaceVertex(face.b(), coords, normal, art, lift), surfaceVertex(face.c(), coords, normal, art, lift));
    }

    private static MeshPartBuilder.VertexInfo surfaceVertex(Vector3 point, Coords coords, Vector3 normal,
          TextureRegion art, float lift) {
        Vector3 raised = new Vector3(point).add(0, 0, lift);
        if (art != null) {
            return topVertex(raised, coords, art, Color.WHITE).setNor(normal);
        }
        float repeat = 96 * BoardGeometry.HEX_SCALE;
        float u = Math.abs(normal.z) > 0.7f ? point.x / repeat
              : (Math.abs(normal.y) > Math.abs(normal.x) ? point.x : point.y) / repeat;
        float v = Math.abs(normal.z) > 0.7f ? point.y / repeat : -point.z / repeat;
        return vertex(raised, normal, u, v, Color.WHITE);
    }

    private static void wall(MeshPartBuilder mesh, BoardSurface.Side side) {
        Vector3 a = side.a(), b = side.b();
        Vector3 normal = new Vector3(b.x - a.x, b.y - a.y, 0).crs(Vector3.Z).nor();
        Vector3 lowerA = new Vector3(a.x, a.y, side.lowA());
        Vector3 lowerB = new Vector3(b.x, b.y, side.lowB());
        float repeat = 96 * BoardGeometry.HEX_SCALE;
        float length = (float) Math.hypot(a.x - b.x, a.y - b.y);
        float u = (a.x * (b.x - a.x) + a.y * (b.y - a.y)) / length / repeat;
        float endU = u + length / repeat;
        mesh.rect(vertex(a, normal, u, -a.z / repeat, Color.WHITE),
              vertex(lowerA, normal, u, -side.lowA() / repeat, Color.WHITE),
              vertex(lowerB, normal, endU, -side.lowB() / repeat, Color.WHITE),
              vertex(b, normal, endU, -b.z / repeat, Color.WHITE));
    }

    /** Cover geometry clips a fixed-scale material; changing its depth never stretches the texture. */
    static void cornice(MeshPartBuilder mesh, BoardScene.Tile tile, BoardSurface.Side side) {
        Vector3 direction = new Vector3(side.b()).sub(side.a());
        direction.z = 0;
        float length = direction.len();
        direction.scl(1 / length);
        Vector3 normal = new Vector3(direction).crs(Vector3.Z);
        float repeat = 96 * BoardGeometry.HEX_SCALE;
        float u = side.a().dot(direction) / repeat;
        float fade = 1.5f * BoardGeometry.HEX_SCALE;
        int segments = Math.max(1, (int) Math.ceil(length / (6 * BoardGeometry.HEX_SCALE)));
        for (int segment = 0; segment < segments; segment++) {
            float from = segment / (float) segments, to = (segment + 1f) / segments;
            Vector3 a = new Vector3(side.a()).lerp(side.b(), from);
            Vector3 b = new Vector3(side.a()).lerp(side.b(), to);
            float lowA = side.lowA() + (side.lowB() - side.lowA()) * from;
            float lowB = side.lowA() + (side.lowB() - side.lowA()) * to;
            float depthA = corniceDepth(tile.surface(), a), depthB = corniceDepth(tile.surface(), b);
            lowA = Math.max(lowA, a.z - depthA);
            lowB = Math.max(lowB, b.z - depthB);
            Color colorA = corniceColor(tile, a), colorB = corniceColor(tile, b);
            float startU = u + length * from / repeat, endU = u + length * to / repeat;
            for (int band = 0; band < 2; band++) {
                float startA = band == 0 ? 0 : Math.min(a.z - lowA, depthA - fade);
                float startB = band == 0 ? 0 : Math.min(b.z - lowB, depthB - fade);
                float endA = Math.min(a.z - lowA, band == 0 ? depthA - fade : depthA);
                float endB = Math.min(b.z - lowB, band == 0 ? depthB - fade : depthB);
                if (endA > startA || endB > startB) {
                    mesh.rect(corniceVertex(a, startA, depthA, normal, startU, colorA),
                          corniceVertex(a, endA, depthA, normal, startU, colorA),
                          corniceVertex(b, endB, depthB, normal, endU, colorB),
                          corniceVertex(b, startB, depthB, normal, endU, colorB));
                }
            }
        }
    }

    private static float corniceDepth(BoardScene.Surface surface, Vector3 point) {
        if (surface == BoardScene.Surface.CONCRETE) {
            return 9 * BoardGeometry.HEX_SCALE;
        }
        float x = point.x / BoardGeometry.HEX_SCALE, y = point.y / BoardGeometry.HEX_SCALE;
        return (9 + 2 * (float) Math.sin(x * 0.09f + y * 0.05f)
              + 3 * (float) Math.sin(y * 0.65f - x * 0.43f)) * BoardGeometry.HEX_SCALE;
    }

    private static MeshPartBuilder.VertexInfo corniceVertex(Vector3 edge, float depth, float fullDepth,
          Vector3 normal, float u, Color color) {
        Vector3 point = new Vector3(edge).add(0, 0, -depth).mulAdd(normal, 0.06f * BoardGeometry.HEX_SCALE);
        float alpha = Math.clamp((fullDepth - depth) / (1.5f * BoardGeometry.HEX_SCALE), 0, 1);
        return vertex(point, normal, u, -point.z / (96 * BoardGeometry.HEX_SCALE), color)
              .setCol(color.r, color.g, color.b, alpha);
    }

    /** Pale neutral rim detail takes its palette from opaque pixels just inside the selected hex's edge. */
    private static Color corniceColor(BoardScene.Tile tile, Vector3 edge) {
        BoardScene.Pixels pixels = tile.ground();
        float u = 0.5f + (edge.x - BoardGeometry.centerX(tile.coords())) / BoardGeometry.WIDTH * 0.9f;
        float v = 0.5f - (edge.y - BoardGeometry.centerY(tile.coords())) / BoardGeometry.HEIGHT * 0.9f;
        int centerX = Math.round(u * (pixels.width() - 1)), centerY = Math.round(v * (pixels.height() - 1));
        float red = 0, green = 0, blue = 0, weight = 0;
        for (int y = centerY - 1; y <= centerY + 1; y++) {
            for (int x = centerX - 1; x <= centerX + 1; x++) {
                int rgba = pixels.rgba(Math.clamp(y, 0, pixels.height() - 1) * pixels.width()
                      + Math.clamp(x, 0, pixels.width() - 1));
                float alpha = (rgba & 255) / 255f;
                red += (rgba >>> 24) * alpha;
                green += ((rgba >>> 16) & 255) * alpha;
                blue += ((rgba >>> 8) & 255) * alpha;
                weight += 255 * alpha;
            }
        }
        return weight == 0 ? new Color(Color.WHITE) : new Color(red / weight, green / weight, blue / weight, 1);
    }

    /** Outward plane of a fall, shared with its wall: away from the higher hex, into the receiving one. */
    private static Vector3 fallOutward(BoardSurface.Side drop) {
        return new Vector3(drop.b().x - drop.a().x, drop.b().y - drop.a().y, 0).crs(Vector3.Z).nor();
    }

    /** Unit normal along a fall's profile: angle zero faces along the surface, ninety degrees along the sheet. */
    private static Vector3 arcNormal(Vector3 outward, float angle) {
        return new Vector3(outward).scl(MathUtils.sin(angle)).add(0, 0, MathUtils.cos(angle));
    }

    /** Angle zero lies on the water surface, ninety degrees on the hanging sheet; the centre sits below. */
    private static Vector3 lipPoint(Vector3 mouth, Vector3 outward, float lip, float reach, float angle) {
        return new Vector3(mouth.x, mouth.y, mouth.z - reach).mulAdd(outward, -lip)
              .mulAdd(arcNormal(outward, angle), reach);
    }

    /** Angle zero meets the receiving surface, ninety degrees joins the hanging sheet; the centre sits outside. */
    private static Vector3 footPoint(Vector3 sheet, float surface, Vector3 outward, float foot, float angle) {
        return new Vector3(sheet.x, sheet.y, surface + foot).mulAdd(outward, foot)
              .mulAdd(arcNormal(outward, angle), -foot);
    }

    private static MeshPartBuilder.VertexInfo arcVertex(Vector3 point, Vector3 normal, float u, float repeat) {
        return vertex(point, normal, u, point.z / repeat, Color.WHITE);
    }

    /**
     * A fall leaves the upper surface inside its mouth and lands inside the receiving hex. The water stops short
     * of the shared edge, the sheet curves over that gap to the edge's own plane, hangs there and then spreads
     * into the water it lands in, so nothing is left hanging beyond the mouth. Only positions move: V stays
     * proportional to world height, so the animated downward scroll keeps its speed and the lip cannot break the
     * pool's palette mixture.
     */
    private static void waterfall(MeshPartBuilder mesh, BoardSurface.Side drop) {
        Vector3 outward = fallOutward(drop);
        float clearance = FALL_CLEARANCE * BoardGeometry.HEX_SCALE;
        float repeat = 48 * BoardGeometry.HEX_SCALE;
        float u = drop.a().dst(drop.b()) / (24 * BoardGeometry.HEX_SCALE);
        float lip = BoardSurface.fallLip(drop.a().z, drop.lowA());
        float foot = .5f * lip;
        // The lip reaches one radius upstream and one radius higher, ending at the sheet's own clearance.
        float reach = lip + clearance;
        Vector3 hangA = new Vector3(drop.a().x, drop.a().y, drop.a().z - reach).mulAdd(outward, clearance);
        Vector3 hangB = new Vector3(drop.b().x, drop.b().y, drop.b().z - reach).mulAdd(outward, clearance);
        Vector3 footA = new Vector3(hangA.x, hangA.y, drop.lowA() + foot);
        Vector3 footB = new Vector3(hangB.x, hangB.y, drop.lowB() + foot);
        for (int segment = 0; segment < FALL_LIP_SEGMENTS; segment++) {
            float from = MathUtils.PI / 2 * segment / FALL_LIP_SEGMENTS;
            float to = MathUtils.PI / 2 * (segment + 1) / FALL_LIP_SEGMENTS;
            mesh.rect(arcVertex(lipPoint(drop.a(), outward, lip, reach, from), arcNormal(outward, from), 0, repeat),
                  arcVertex(lipPoint(drop.a(), outward, lip, reach, to), arcNormal(outward, to), 0, repeat),
                  arcVertex(lipPoint(drop.b(), outward, lip, reach, to), arcNormal(outward, to), u, repeat),
                  arcVertex(lipPoint(drop.b(), outward, lip, reach, from), arcNormal(outward, from), u, repeat));
        }
        // The hanging sheet stays in the mouth's own plane, clear of the wall.
        mesh.rect(arcVertex(hangA, outward, 0, repeat), arcVertex(footA, outward, 0, repeat),
              arcVertex(footB, outward, u, repeat), arcVertex(hangB, outward, u, repeat));
        for (int segment = 0; segment < FALL_FOOT_SEGMENTS; segment++) {
            float from = MathUtils.PI / 2 * segment / FALL_FOOT_SEGMENTS;
            float to = MathUtils.PI / 2 * (segment + 1) / FALL_FOOT_SEGMENTS;
            mesh.rect(arcVertex(footPoint(hangA, drop.lowA(), outward, foot, to), arcNormal(outward, to), 0, repeat),
                  arcVertex(footPoint(hangA, drop.lowA(), outward, foot, from), arcNormal(outward, from), 0, repeat),
                  arcVertex(footPoint(hangB, drop.lowB(), outward, foot, from), arcNormal(outward, from), u, repeat),
                  arcVertex(footPoint(hangB, drop.lowB(), outward, foot, to), arcNormal(outward, to), u, repeat));
        }
    }

    /** A short transparent curtain carries animated droplets above each receiving pool; no particle objects. */
    private static void splash(MeshPartBuilder mesh, GpuWaterShader.Impact impact) {
        Vector3 center = new Vector3(impact.center()).mulAdd(impact.inward(), BoardGeometry.WIDTH * 0.025f);
        Vector3 tangent = new Vector3(impact.inward().y, -impact.inward().x, 0).scl(impact.halfWidth());
        Vector3 a = new Vector3(center).sub(tangent), b = new Vector3(center).add(tangent);
        mesh.rect(vertex(a, impact.inward(), 0, 0, Color.WHITE), vertex(b, impact.inward(), 1, 0, Color.WHITE),
              vertex(new Vector3(b).add(0, 0, impact.radius()), impact.inward(), 1, 1, Color.WHITE),
              vertex(new Vector3(a).add(0, 0, impact.radius()), impact.inward(), 0, 1, Color.WHITE));
    }

    /** Continue the neighboring land's selected artwork across the edge, beneath the existing sand fade. */
    private static void bank(MeshPartBuilder mesh, Coords water, BoardSurface.Face face, Coords land, TextureRegion art) {
        Vector3 a = BoardGeometry.corner(water, 0, face.landEdge());
        Vector3 outward = BoardGeometry.corner(water, 0, face.landEdge() + 1).sub(a).crs(Vector3.Z).nor();
        Vector3 normal = new Vector3(face.b()).sub(face.a()).crs(new Vector3(face.c()).sub(face.a())).nor();
        mesh.triangle(bankVertex(face.a(), land, art, normal, a, outward),
              bankVertex(face.b(), land, art, normal, a, outward), bankVertex(face.c(), land, art, normal, a, outward));
    }

    private static MeshPartBuilder.VertexInfo bankVertex(Vector3 point, Coords land, TextureRegion art,
          Vector3 normal, Vector3 edge, Vector3 outward) {
        // Mirror only the texture sample into the neighbor: geometry and the waterline stay unchanged.
        Vector3 sample = new Vector3(point).mulAdd(outward, -2 * new Vector3(point).sub(edge).dot(outward));
        return topVertex(sample, land, art, Color.WHITE).setPos(point).setNor(normal);
    }

    private static void shore(MeshPartBuilder mesh, BoardScene.Tile tile, BoardSurface.Face face) {
        Vector3 normal = new Vector3(face.b()).sub(face.a()).crs(new Vector3(face.c()).sub(face.a())).nor();
        mesh.triangle(shoreVertex(tile, face.a(), normal), shoreVertex(tile, face.b(), normal),
              shoreVertex(tile, face.c(), normal));
    }

    private static MeshPartBuilder.VertexInfo shoreVertex(BoardScene.Tile tile, Vector3 point, Vector3 normal) {
        float wet = Math.max(0, Math.min(1, (tile.elevation() * BoardGeometry.LEVEL - point.z) / BoardGeometry.HEX_SCALE));
        // Sand appears gradually across the bank and darkens slightly at the waterline.
        float shade = 1 - wet * 0.12f;
        return surfaceVertex(point, tile.coords(), normal, null, 0.035f * BoardGeometry.HEX_SCALE)
              .setCol(shade, shade, shade, wet);
    }

    private static void grid(MeshPartBuilder mesh, Coords coords, float z, TextureRegion region) {
        Vector3 center = BoardGeometry.center(coords, 0);
        center.z = z + 0.03f;
        float shade = BoardGeometry.tuning().gridShade();
        Color color = new Color(shade, shade, shade, 1);
        for (int edge = 0; edge < 6; edge++) {
            Vector3 a = BoardGeometry.corner(coords, 0, edge);
            Vector3 b = BoardGeometry.corner(coords, 0, edge + 1);
            a.z = b.z = center.z;
            Vector3 innerA = new Vector3(a).lerp(center, 0.012f);
            Vector3 innerB = new Vector3(b).lerp(center, 0.012f);
            mesh.rect(topVertex(a, coords, region, color), topVertex(b, coords, region, color),
                  topVertex(innerB, coords, region, color), topVertex(innerA, coords, region, color));
        }
    }

    private BoundingBox unitBounds(ModelInstance unit) {
        return frameBounds == null ? UnitBounds.world(unit) : frameBounds.get(unit);
    }

    /** Unit instances already contain the shared animation's current position; hidden units never enter this list. */
    void animate(float delta, List<ModelInstance> units) {
        animate(delta, units, DEFAULT_BUILDING_OPACITY);
    }

    void animate(float delta, List<ModelInstance> units, float buildingAlpha) {
        float nextBuilding = MathUtils.clamp(buildingAlpha, 0, 1);
        boolean changedOpacity = nextBuilding != buildingOpacity;
        buildingOpacity = nextBuilding;
        clock += delta;
        List<BoundingBox> occupied = hasCutaways && buildingOpacity < 1
              ? units.stream().map(this::unitBounds).toList() : List.of();
        for (Chunk chunk : chunks) {
            for (LiquidSurface liquid : chunk.liquidMaterials) {
                GpuAssets.Animation<Texture> frames = assets.liquidAnimation(liquid.source());
                int index = frames.index(clock);
                Texture frame = frames.frames().get(index);
                TextureAttribute texture = liquid.material().get(TextureAttribute.class, TextureAttribute.Diffuse);
                TextureAttribute emission = liquid.material().get(TextureAttribute.class, TextureAttribute.Emissive);
                float offsetU = (clock * liquid.current().u()) % 1;
                float offsetV = liquid.falling() ? (clock * (emission == null ? 1 : 0.25f)) % 1 : (clock * liquid.current().v()) % 1;
                texture.textureDescription.texture = frame;
                texture.offsetU = offsetU;
                texture.offsetV = offsetV;
                if (liquidShaderAnimation) {
                    liquid.material().get(TextureAttribute.class, GpuLiquidShader.Frame.TYPE).textureDescription.texture
                          = frames.frames().get((index + 1) % frames.frames().size());
                    liquid.material().get(FloatAttribute.class, GpuLiquidShader.Frame.BLEND).value = frames.blend(clock, index);
                }
                if (emission != null) {
                    emission.textureDescription.texture = frame;
                    emission.offsetU = offsetU;
                    emission.offsetV = offsetV;
                }
            }
            if (chunk.cutaways.isEmpty()) { continue; }
            Set<Coords> occupiedHexes = new HashSet<>();
            for (BoundingBox unit : occupied) {
                if (!chunk.bounds.intersects(unit)) {
                    continue;
                }
                for (Prop prop : chunk.cutaways) {
                    if (prop.bounds().intersects(unit)) {
                        occupiedHexes.add(prop.coords());
                    }
                }
            }
            Set<Prop> faded = new HashSet<>();
            if (!occupiedHexes.isEmpty()) {
                for (Prop prop : chunk.cutaways) {
                    if (occupiedHexes.contains(prop.coords())) {
                        faded.add(prop);
                    }
                }
            }
            boolean changedOccupancy = !faded.equals(chunk.faded);
            if (changedOccupancy || changedOpacity) {
                chunk.faded = Set.copyOf(faded);
                for (Prop prop : chunk.cutaways) {
                    for (Material material : prop.instance().materials) {
                        if (faded.contains(prop)) {
                            BlendingAttribute blend = material.get(BlendingAttribute.class, BlendingAttribute.Type);
                            if (blend == null) {
                                blend = new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
                                material.set(blend);
                                material.set(new DepthTestAttribute(GL20.GL_LEQUAL, false));
                                material.set(IntAttribute.createCullFace(GL20.GL_BACK));
                            }
                            blend.opacity = buildingOpacity;
                        } else {
                            material.remove(BlendingAttribute.Type);
                            material.remove(DepthTestAttribute.Type);
                            material.remove(IntAttribute.CullFace);
                        }
                    }
                }
                if (changedOccupancy) {
                    chunk.cacheProps();
                }
            }
        }
    }

    Environment environment() {
        return environment;
    }

    BoundingBox roofBounds(Coords coords) {
        BoundingBox result = null;
        int index = (coords.getX() / CHUNK_SIZE) * chunkRows + coords.getY() / CHUNK_SIZE;
        if (index < 0 || index >= chunks.size()) {
            return null;
        }
        for (Prop prop : chunks.get(index).props) {
            if (flatTrees && prop.tree()) { continue; }
            if (prop.coords().equals(coords)
                  && (result == null || prop.bounds().max.z > result.max.z)) {
                result = prop.bounds();
            }
        }
        return result;
    }

    /** Roofs, courtyard openings and walls use the authored mesh, not a bounding-box proxy. */
    Coords pick(BoardScene scene, Ray ray) {
        BoardGeometry.Hit hit = hit(scene, ray);
        return hit == null ? null : hit.coords();
    }

    BoardGeometry.Hit hit(BoardScene scene, Ray ray) {
        Coords result = null;
        float nearest = Float.POSITIVE_INFINITY;
        List<BoardScene.Tile> candidates = new ArrayList<>();
        Vector3 hit = new Vector3();
        for (int index = 0; index < chunks.size(); index++) {
            Chunk chunk = chunks.get(index);
            if (!Intersector.intersectRayBoundsFast(ray, chunk.bounds)) {
                continue;
            }
            // Reuse render bounds before invoking the shared surface picker; a pointer event must not
            // inspect six neighbors and allocate geometry bounds for every hex of a 40,000-hex board.
            int startX = (index / chunkRows) * CHUNK_SIZE;
            int startY = (index % chunkRows) * CHUNK_SIZE;
            for (int x = startX; x < Math.min(startX + CHUNK_SIZE, scene.width()); x++) {
                for (int y = startY; y < Math.min(startY + CHUNK_SIZE, scene.height()); y++) {
                    candidates.add(scene.tile(new Coords(x, y)));
                }
            }
            for (Prop prop : chunk.props) {
                if (flatTrees && prop.tree()) { continue; }
                if (!Intersector.intersectRayBoundsFast(ray, prop.bounds())) {
                    continue;
                }
                Ray local = new Ray(ray.origin, ray.direction).mul(new Matrix4(prop.instance().transform).inv());
                // Camera zoom changes visual detail, never the hex selected by the same board-space ray.
                List<Vector3> triangles = featureTriangles.computeIfAbsent(prop.pickingModel, GpuTerrain::triangles);
                for (int i = 0; i < triangles.size(); i += 3) {
                    if (Intersector.intersectRayTriangle(local, triangles.get(i), triangles.get(i + 1), triangles.get(i + 2), hit)) {
                        float distance = ray.origin.dst2(hit.mul(prop.instance().transform));
                        if (distance < nearest) {
                            nearest = distance;
                            result = prop.coords();
                        }
                    }
                }
            }
        }
        BoardGeometry.Hit groundHit = BoardGeometry.hit(scene, ray, candidates, floor);
        return groundHit != null && groundHit.distance() <= nearest ? groundHit
              : result == null ? null : new BoardGeometry.Hit(result, nearest);
    }

    static List<Vector3> triangles(Model model) {
        List<Vector3> result = new ArrayList<>();
        // Board assets are exported in model coordinates with identity nodes.
        for (var mesh : model.meshes) {
            int stride = mesh.getVertexSize() / Float.BYTES;
            int offset = mesh.getVertexAttribute(VertexAttributes.Usage.Position).offset / Float.BYTES;
            float[] vertices = new float[mesh.getNumVertices() * stride];
            short[] indices = new short[mesh.getNumIndices()];
            mesh.getVertices(vertices);
            mesh.getIndices(indices);
            for (short index : indices) {
                int at = Short.toUnsignedInt(index) * stride + offset;
                result.add(new Vector3(vertices[at], vertices[at + 1], vertices[at + 2]));
            }
        }
        return result;
    }

    void setFlatTrees(boolean enabled) {
        if (flatTrees != enabled) {
            flatTrees = enabled;
            shadowDirty = true;
            detailPixelsPerUnit = Float.NaN;
        }
    }

    /** Opaque world first. Tactical overlays are a separate final pass. */
    void render(Camera camera, boolean drawTactical) {
        if (!drawTactical) {
            updateDetail(camera);
        }
        batch.begin(camera);
        for (Chunk chunk : chunks) {
            if (!camera.frustum.boundsInFrustum(chunk.bounds)) {
                continue;
            }
            if (drawTactical) {
                if (flatTrees) { chunk.flatTrees.forEach(instance -> batch.render(instance)); }
                chunk.tactical.forEach(instance -> batch.render(instance));
            } else {
                chunk.opaque.forEach(instance -> batch.render(instance, environment));
                if (chunk.scatterVisible) {
                    chunk.scatter.forEach(instance -> batch.render(instance, environment));
                }
                batch.render(chunk.solidProps, environment);
                if (!flatTrees) { batch.render(chunk.trees, environment); }
            }
        }
        batch.end();
        if (!drawTactical) {
            batch.begin(camera);
            for (Chunk chunk : chunks) {
                if (camera.frustum.boundsInFrustum(chunk.bounds)) {
                    chunk.overlays.forEach(instance -> batch.render(instance, environment));
                }
            }
            batch.end();
        }
    }

    /** Water and faded features follow units, with depth testing but no depth writes. */
    void renderTransparent(Camera camera) {
        updateDetail(camera);
        batch.begin(camera);
        for (Chunk chunk : chunks) {
            if (camera.frustum.boundsInFrustum(chunk.bounds)) {
                chunk.water.forEach(instance -> batch.render(instance, environment));
                for (Prop prop : chunk.faded) { batch.render(prop.instance(), environment); }
            }
        }
        batch.end();
    }

    private void updateLight(BoardScene.Light next) {
        light = next;
        shadowDirty = true;
        shadowBounds.inf();
        chunks.forEach(chunk -> shadowBounds.ext(chunk.bounds));
        applyLight();
    }

    void setAtmosphere(BoardAtmosphere.Lighting next) {
        if (!Objects.equals(atmosphere, next)) {
            // Color, exposure and fog cannot change a shadow's geometry.
            shadowDirty |= atmosphere == null || next == null || !atmosphere.direction().equals(next.direction());
            atmosphere = next;
            applyLight();
        }
    }

    /** Live shading preview; changing it needs no geometry, atlas or shadow rebuild. */
    void setNormalMaps(boolean enabled) {
        normalMaps = enabled;
    }

    void setWetness(float wetness) {
        this.wetness = wetness;
    }

    /** Isolate authored frame interpolation from surface deformation in native reference comparisons. */
    void setWaterEffects(boolean enabled) { waterEffects = enabled; }

    private void applyLight() {
        if (light == null && atmosphere == null) {
            if (shadow != null) {
                environment.remove(shadow);
                shadow.dispose();
                shadow = null;
            }
            environment.shadowMap = null;
            environment.set(ColorAttribute.createAmbientLight(0.85f, 0.85f, 0.85f, 1));
            return;
        }
        if (atmosphere != null && !atmosphere.hasDirectLight()) {
            // Moonless and pitch-black nights retain ambient readability, but have no directional source.
            if (shadow != null) { environment.remove(shadow); }
            environment.shadowMap = null;
            environment.set(ColorAttribute.createAmbientLight(atmosphere.ambient()));
            return;
        }
        if (shadow == null) {
            shadow = new DirectionalShadowLight(SHADOW_RESOLUTION, SHADOW_RESOLUTION, 1, 1, 1, 2);
        }
        if (environment.shadowMap != shadow) {
            environment.add(shadow);
            environment.shadowMap = shadow;
        }
        if (atmosphere == null) {
            shadow.set(0.55f, 0.53f, 0.48f, light.x() * BoardGeometry.HEX_SCALE,
                  light.y() * BoardGeometry.HEX_SCALE, -BoardGeometry.LEVEL);
            environment.set(ColorAttribute.createAmbientLight(0.55f, 0.58f, 0.62f, 1));
        } else {
            shadow.set(atmosphere.direct(), atmosphere.direction());
            environment.set(ColorAttribute.createAmbientLight(atmosphere.ambient()));
        }
    }

    /** Camera depth retains the cutaway so atmosphere effects do not hide units behind faded surfaces. */
    void renderDepth(Camera camera, List<ModelInstance> units, ModelBatch pass) {
        updateDetail(camera);
        renderDepth(camera, units, pass, false);
    }

    private void renderDepth(Camera camera, List<ModelInstance> units, ModelBatch pass, boolean shadows) {
        pass.begin(camera);
        for (Chunk chunk : chunks) {
            if (camera.frustum.boundsInFrustum(chunk.bounds)) {
                chunk.opaque.forEach(pass::render);
                if (chunk.scatterVisible) {
                    chunk.scatter.forEach(pass::render);
                }
                pass.render(shadows ? chunk.shadowProps : chunk.solidProps);
                if (!flatTrees) { pass.render(chunk.trees); }
            }
        }
        for (ModelInstance unit : units) {
            if (camera.frustum.boundsInFrustum(unitBounds(unit))) { GpuUnitInstance.renderDepth(pass, unit); }
        }
        pass.end();
    }

    void renderShadows(List<ModelInstance> units) {
        renderShadows(null, units);
    }

    void renderShadows(OrthographicCamera view, List<ModelInstance> units) {
        if (view != null) {
            updateDetail(view);
        } else if (!flatTrees) {
            chunks.forEach(chunk -> chunk.cacheTrees(assets));
        }
        if (shadow == null || environment.shadowMap == null) {
            return;
        }
        boolean changed = shadowDirty || units.size() != shadowModels.size();
        for (int index = 0; !changed && index < units.size(); index++) {
            changed = units.get(index).model != shadowModels.get(index)
                  || !Arrays.equals(units.get(index).transform.val, shadowTransforms.get(index).val)
                  || shadowPose(units.get(index)) != shadowPoses.get(index);
        }
        boolean viewChanged = (view != null) != shadowViewPresent
              || (view != null && !Arrays.equals(view.combined.val, shadowView.val));
        if (!changed && !viewChanged) {
            return;
        }
        BoundingBox bounds = new BoundingBox(shadowBounds);
        for (ModelInstance unit : units) { bounds.ext(unitBounds(unit)); }
        fitShadowCamera(view, shadowFit, bounds, shadow.direction);
        shadowViewPresent = view != null;
        if (view != null) { shadowView.set(view.combined); }
        // Compare the fitted texel grid, not the viewing camera, before invalidating the cached shadow map.
        if (!changed && Arrays.equals(shadowFit.combined.val, shadowProjection.val)) { return; }
        Camera lightCamera = shadow.getCamera();
        lightCamera.position.set(shadowFit.position);
        lightCamera.direction.set(shadowFit.direction);
        lightCamera.up.set(shadowFit.up);
        lightCamera.viewportWidth = shadowFit.viewportWidth;
        lightCamera.viewportHeight = shadowFit.viewportHeight;
        lightCamera.near = shadowFit.near;
        lightCamera.far = shadowFit.far;
        lightCamera.update();
        shadowProjection.set(lightCamera.combined);
        shadowModels.clear();
        shadowPoses.clear();
        while (shadowTransforms.size() > units.size()) { shadowTransforms.removeLast(); }
        for (int index = 0; index < units.size(); index++) {
            ModelInstance unit = units.get(index);
            shadowModels.add(unit.model);
            if (index == shadowTransforms.size()) { shadowTransforms.add(new Matrix4()); }
            shadowTransforms.get(index).set(unit.transform);
            shadowPoses.add(shadowPose(unit));
        }
        shadow.begin();
        renderDepth(shadow.getCamera(), units, depthBatch, true);
        shadow.end();
        shadowDirty = false;
    }

    /** All passes use the viewing camera's detail selection; scatter culling never rebuilds a mesh. */
    private void updateDetail(Camera camera) {
        if (!(camera instanceof OrthographicCamera orthographic)) {
            return;
        }
        float pixelsPerUnit = BoardCamera.pixelsPerUnit(orthographic);
        if (pixelsPerUnit == detailPixelsPerUnit) {
            return;
        }
        detailPixelsPerUnit = pixelsPerUnit;
        for (Chunk chunk : chunks) {
            float scatterPixels = chunk.scatterDiameter * pixelsPerUnit;
            boolean visible = scatterPixels >= (chunk.scatterVisible ? 2 : 3);
            if (visible != chunk.scatterVisible) {
                chunk.scatterVisible = visible;
                shadowDirty = true;
            }
            // Largest tree wins: smaller neighbors may retain extra detail, never lose it early.
            if (flatTrees) { continue; }
            int next = TreeLod.level(chunk.treeDiameter * pixelsPerUnit, chunk.treeLod);
            if (next != chunk.treeLod) {
                chunk.treeLod = next;
                shadowDirty = true;
            }
            chunk.cacheTrees(assets);
        }
    }

    private static int poseHash(Iterable<Node> nodes) {
        int result = 1;
        for (Node node : nodes) {
            result = 31 * result + Arrays.hashCode(node.globalTransform.val);
            for (var part : node.parts) {
                result = 31 * result + (part.enabled ? 1 : 0);
            }
            result = 31 * result + poseHash(node.getChildren());
        }
        return result;
    }

    private static int shadowPose(ModelInstance instance) {
        return 31 * poseHash(instance.nodes) + (instance instanceof GpuUnitInstance unit ? unit.detailRevision() : 0);
    }

    /** Focus texels on visible receivers, retaining the full light depth for offscreen shadow casters. */
    static void fitShadowCamera(OrthographicCamera view, Camera target, BoundingBox bounds, Vector3 direction) {
        BoundingBox receivers = new BoundingBox(bounds);
        if (view != null) {
            receivers.inf();
            Vector3 right = new Vector3(view.direction).crs(view.up).nor();
            for (int x : new int[] { -1, 1 }) {
                for (int y : new int[] { -1, 1 }) {
                    Vector3 origin = new Vector3(view.position)
                          .mulAdd(right, x * view.viewportWidth * view.zoom / 2)
                          .mulAdd(view.up, y * view.viewportHeight * view.zoom / 2);
                    for (float z : new float[] { bounds.min.z, bounds.max.z }) {
                        receivers.ext(new Vector3(origin).mulAdd(view.direction, (z - origin.z) / view.direction.z));
                    }
                }
            }
            receivers.min.x = MathUtils.clamp(receivers.min.x, bounds.min.x, bounds.max.x);
            receivers.min.y = MathUtils.clamp(receivers.min.y, bounds.min.y, bounds.max.y);
            receivers.max.x = MathUtils.clamp(receivers.max.x, bounds.min.x, bounds.max.x);
            receivers.max.y = MathUtils.clamp(receivers.max.y, bounds.min.y, bounds.max.y);
            receivers.min.z = bounds.min.z;
            receivers.max.z = bounds.max.z;
            receivers.update();
        }
        target.direction.set(direction).nor();
        Vector3 right = new Vector3(target.direction)
              .crs(Math.abs(target.direction.z) > 0.99f ? Vector3.Y : Vector3.Z).nor();
        target.up.set(right).crs(target.direction).nor();
        BoundingBox lightSpace = new BoundingBox().inf();
        float near = Float.POSITIVE_INFINITY;
        float far = Float.NEGATIVE_INFINITY;
        for (int corner = 0; corner < 8; corner++) {
            Vector3 point = new Vector3((corner & 1) == 0 ? receivers.min.x : receivers.max.x,
                  (corner & 2) == 0 ? receivers.min.y : receivers.max.y,
                  (corner & 4) == 0 ? receivers.min.z : receivers.max.z);
            lightSpace.ext(point.dot(right), point.dot(target.up), 0);
            point.set((corner & 1) == 0 ? bounds.min.x : bounds.max.x,
                  (corner & 2) == 0 ? bounds.min.y : bounds.max.y,
                  (corner & 4) == 0 ? bounds.min.z : bounds.max.z);
            float depth = point.dot(target.direction);
            near = Math.min(near, depth);
            far = Math.max(far, depth);
        }
        // Leave a filtering border, and align the light grid to texels to avoid crawling edges while panning.
        float border = 1 + 4f / SHADOW_RESOLUTION;
        target.viewportWidth = Math.max(1, lightSpace.getWidth()) * border;
        target.viewportHeight = Math.max(1, lightSpace.getHeight()) * border;
        float texelX = target.viewportWidth / SHADOW_RESOLUTION;
        float texelY = target.viewportHeight / SHADOW_RESOLUTION;
        Vector3 center = lightSpace.getCenter(new Vector3());
        center.x = Math.round(center.x / texelX) * texelX;
        center.y = Math.round(center.y / texelY) * texelY;
        target.position.set(right).scl(center.x).mulAdd(target.up, center.y).mulAdd(target.direction, near - 1);
        target.near = 1;
        target.far = far - near + 2;
        target.update();
    }

    @Override
    public void dispose() {
        chunks.forEach(Chunk::dispose);
        chunks.clear();
        ground.dispose();
        rims.clear();
        decals.dispose();
        tactical.dispose();
        foliage.dispose();
        batch.dispose();
        depthBatch.dispose();
        if (shadow != null) {
            shadow.dispose();
        }
        assets.dispose();
        rainNoise.dispose();
        featureTriangles.clear();
    }
}

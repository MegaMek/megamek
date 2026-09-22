/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.JsonReader;
import com.fasterxml.jackson.databind.ObjectMapper;
import megamek.client.ui.tileset.EquipmentModelPolicy;
import megamek.client.ui.tileset.MekTileset;
import megamek.client.ui.tileset.UnitModelEquipment;
import megamek.common.Configuration;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.Coords;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Native schema/mesh/transform boundary. Review fixtures use the production library and attachment bindings. */
@Tag("on-demand")
class GpuModularUnitModelsSmokeTest {
    private static final String ROOT = "units/modular/";

    @Test
    void sharedRigidAssetsKeepInstanceStateAndEmittersIndependent() {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        new Lwjgl3Application(new ApplicationAdapter() {
            @Override
            public void create() {
                var library = new GpuUnitModels();
                var batch = new ModelBatch();
                try {
                    var body = library.modular(ROOT + "bodies/warhammer.json");
                    var gun = library.modular(ROOT + "equipment/ppc.json");
                    var launcher = library.modular(ROOT + "equipment/srm-6.json");
                    var lamp = library.modular(ROOT + "equipment/searchlight.json");
                    var troop = library.modular(ROOT + "troops/rifle-standing.json");
                    for (var asset : List.of(body, gun, launcher, lamp, troop)) {
                        assertNotNull(asset);
                    }
                    assertSame(body, library.modular(ROOT + "bodies/warhammer.json"));
                    assertNull(library.modular("../outside.json"));
                    assertNull(library.modular(ROOT + "missing.json"));
                    assertEquals(6, launcher.descriptor().emitters().size());
                    assertEquals("lamp", lamp.descriptor().emitters().getFirst().role());

                    var first = new ModelInstance(body.model());
                    var second = new ModelInstance(body.model());
                    var barrel = new ModelInstance(gun.model());
                    assertSame(first.model, second.model);
                    assertNotSame(first.getNode("CT"), second.getNode("CT"));
                    assertNotSame(first.getMaterial("paint"), second.getMaterial("paint"));
                    var attachment = new UnitModelAttachment(first, barrel,
                          socket(body, "LA-front"), new Matrix4());
                    first.transform.setToTranslation(13, 7, 4).rotate(Vector3.Z, 90).scale(2, 2, 2);
                    attachment.update();
                    var point = new Vector3();
                    var direction = new Vector3();
                    var emitter = gun.descriptor().emitters().getFirst();
                    float tip = emitter.position().get(1);
                    UnitModelAttachment.emitter(barrel, emitter, point, direction);
                    // The socket is read from the body rather than written in, because the Warhammer is drawn at its recipe's
                    // bodyScale and that changes whenever the class heights are tuned. At the authored size the socket sits at
                    // (-20, 9, 32); the barrel's own tip is never scaled.
                    var hardpoint = socket(body, "LA-front");
                    var rest = second.getNode(hardpoint.node()).globalTransform.getTranslation(new Vector3())
                          .add(UnitModelDescriptor.vector(hardpoint.position()));
                    float grown = -rest.x / 20;
                    assertTrue(point.epsilonEquals(new Vector3(13 - 2 * (rest.y + tip), 7 + 2 * rest.x, 4 + 2 * rest.z), .01f),
                          point.toString());
                    first.getNode("CT").rotation.set(Vector3.Z, 90);
                    first.getNode("LA-forearm").rotation.set(Vector3.X, 90);
                    first.calculateTransforms();
                    attachment.update();
                    UnitModelAttachment.emitter(barrel, emitter, point, direction);
                    assertTrue(point.epsilonEquals(new Vector3(13 + 40 * grown, 7 - 2 * grown, 4 + 88 * grown + 2 * tip), .01f),
                          point.toString());
                    assertTrue(direction.epsilonEquals(Vector3.Z, .001f), direction.toString());
                    assertTrue(second.getNode("CT").rotation.isIdentity());
                    UnitDamageDisplay.show(second, new BoardScene.LocationDamage(Set.of("LA"), Set.of("RL")));
                    assertFalse(second.getNode("LA").parts.first().enabled);
                    assertTrue(first.getNode("LA").parts.first().enabled);

                    first.transform.setToTranslation(-35, 0, 0).scale(.85f, .85f, .85f);
                    first.getNode("CT").rotation.set(Vector3.Z, -25);
                    first.getNode("LA-forearm").rotation.set(Vector3.X, 20);
                    first.calculateTransforms();
                    second.transform.setToTranslation(35, 0, 0).scale(.85f, .85f, .85f);
                    first.getMaterial("paint").set(ColorAttribute.createDiffuse(.55f, .8f, .58f, 1));
                    second.getMaterial("paint").set(ColorAttribute.createDiffuse(.85f, .5f, .35f, 1));
                    var instances = new ArrayList<>(List.of(first, second, barrel));
                    attachment.update();
                    attach(first, body, gun, "RA-front", instances);
                    attach(second, body, gun, "RA-front", instances);
                    attach(first, body, launcher, "RT-launcher", instances);
                    attach(first, body, lamp, "external-searchlight", instances);
                    for (int i = 0; i < 2; i++) {
                        var figure = new ModelInstance(troop.model());
                        figure.transform.setToTranslation(-13 + 26 * i, 36, 0);
                        if (i == 1) {
                            figure.getNode("rightArmForearm").rotation.set(Vector3.X, 20);
                            figure.calculateTransforms();
                        }
                        instances.add(figure);
                    }
                    renderReview(batch, instances);
                    renderFormations(library, batch);
                    GpuMekAssemblyReview.verify(library, batch);
                    GpuEquipmentAssemblyReview.verify(library, batch);
                    GpuFamilyAssemblyReview.verify(library, batch);
                    GpuLandingSupportReview.verify(library, batch);
                    GpuUnitAnimationReview.verify(library, batch);
                    GpuFamilyMotionReview.verify(library, batch);
                    GpuJumpJetReview.verify(library, batch);
                    GpuCamouflageReview.verify(library);
                    GpuDamageReview.verify(library);
                    assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
                } catch (Throwable error) {
                    failure.set(error);
                } finally {
                    batch.dispose();
                    library.dispose();
                    Gdx.app.exit();
                }
            }
        }, GpuBoardWindow.configuration(false));
        if (failure.get() != null) {
            throw new AssertionError("Native modular review failed", failure.get());
        }
    }

    private static UnitModelDescriptor.Hardpoint socket(GpuUnitModels.ModularAsset body, String id) {
        return body.descriptor().hardpoints().stream().filter(point -> point.id().equals(id)).findFirst().orElseThrow();
    }

    private static void attach(ModelInstance parent, GpuUnitModels.ModularAsset body,
          GpuUnitModels.ModularAsset module, String id, List<ModelInstance> instances) {
        var child = new ModelInstance(module.model());
        new UnitModelAttachment(parent, child, socket(body, id), new Matrix4()).update();
        instances.add(child);
    }

    private static void renderReview(ModelBatch batch, List<ModelInstance> instances) {
        renderReview(batch, instances, "modular-contract", 150, 18);
    }

    private static void renderFormations(GpuUnitModels library, ModelBatch batch) throws Exception {
        var tileset = new MekTileset(new File(Configuration.dataDir(), "images/units"));
        tileset.loadFromFile("mekset.txt");
        List<ModelInstance> instances = new ArrayList<>();
        List<Object> review = new ArrayList<>();
        var armor = new BattleArmor();
        armor.setId(101);
        armor.setSquadSize(6);
        for (int member = 1; member <= 6; member++) {
            armor.initializeInternal(1, member);
        }
        var selection = UnitModelSelection.capture(armor, -1, false, tileset);
        assertEquals(ROOT + "battle-armor.json", selection.asset());
        GpuUnitModel six = library.get(selection, armor.getId());
        assertNotNull(six);
        assertSame(six, library.get(selection, armor.getId()));
        assertEquals(6, six.instance.nodes.size);
        assertEquals(1284, triangles(six.instance.nodes));
        assertDeferredEquipment(library, selection, 1284);
        instances.add(formationInstance(six, armor, selection));
        for (int member = 1; member < 6; member++) {
            armor.setInternal(0, member);
        }
        var singleSelection = UnitModelSelection.capture(armor, -1, false, tileset);
        var single = library.get(singleSelection, armor.getId());
        assertNotNull(single);
        assertEquals(1, single.instance.nodes.size);
        assertEquals(214, triangles(single.instance.nodes));
        assertNotNull(single.instance.getNode("trooper-6"));
        assertEquals(six.instance.getNode("trooper-6").translation, single.instance.getNode("trooper-6").translation);
        instances.add(formationInstance(single, armor, singleSelection));
        armor.setInternal(0, 6);
        var zero = library.get(UnitModelSelection.capture(armor, -1, false, tileset), armor.getId());
        assertNotNull(zero);
        assertEquals(0, zero.instance.nodes.size);
        int id = 102;
        for (var movement : List.of(EntityMovementMode.INF_LEG, EntityMovementMode.INF_JUMP,
              EntityMovementMode.INF_MOTORIZED, EntityMovementMode.WHEELED, EntityMovementMode.TRACKED, EntityMovementMode.HOVER)) {
            var infantry = new ConvInfantry();
            infantry.setId(id++);
            infantry.setMovementMode(movement);
            infantry.initializeInternal(28, ConvInfantry.LOC_INFANTRY);
            var groupSelection = UnitModelSelection.capture(infantry, -1, false, tileset);
            assertEquals(ROOT + "infantry.json", groupSelection.asset());
            var group = library.get(groupSelection, infantry.getId());
            assertNotNull(group);
            assertEquals(6, group.instance.nodes.size);
            assertTrue(triangles(group.instance.nodes) > 0);
            assertTrue(triangles(group.instance.nodes) <= UnitModelDescriptor.TRIANGLE_LIMIT);
            assertDeferredEquipment(library, groupSelection, triangles(group.instance.nodes));
            instances.add(formationInstance(group, infantry, groupSelection));
            var descriptor = new JsonReader().parse(new FileHandle(new File(Configuration.dataDir(),
                  "models/units/modular/infantry.json")));
            review.add(java.util.Map.of("movement", movement.name(), "parts",
                  InfantryVisual.parts(groupSelection.state().structure(), descriptor)));
        }
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(new File(System.getProperty("megamek.gpu.screenshots"),
              "runtime-formation-placements.json"), review);
        for (int index = 0; index < instances.size(); index++) {
            // Same placement adaptation as the board; each composite keeps child and limb transforms independent.
            instances.get(index).transform.setToTranslation((1.5f - index % 4) * 78, (index / 4 == 0 ? -1 : 1) * 40, 0)
                  .scale(BoardGeometry.UNIT_SCALE, BoardGeometry.UNIT_SCALE,
                        BoardGeometry.LEVEL * BoardGeometry.UNIT_HEIGHT_SCALE / 54);
        }
        library.retainAssemblies(Set.of());
        // Dropping assemblies must not dispose the borrowed meshes used by other instances or future units.
        assertNotNull(library.modular(ROOT + "troops/battle-armor-standing.json"));
        renderReview(batch, instances, "runtime-infantry", 360, 4);
    }

    private static ModelInstance formationInstance(GpuUnitModel model, Entity entity, BoardScene.UnitModel selection) {
        var instance = new ModelInstance(model.instance.model);
        var animator = new UnitAnimator();
        var unit = new BoardScene.Unit(entity.getId(), -1, "Formation review", new BoardScene.Waypoint(new Coords(0, 0), 0, 0),
              null, false, null, 1, false, selection, 0);
        var original = BoardGeometry.tuning();
        try {
            for (float scale : new float[] { .7f, 1, .7f }) {
                BoardGeometry.tune(new BoardGeometry.Tuning(original.hexScale(), scale, original.unitHeightScale(),
                      original.levelHeight(), original.gridShade(), original.multiHexUnitScale()));
                animator.apply(model, instance, unit, UnitMotion.Sample.STILL, 0, 0, true, 0);
                if (model.rigs().stream().noneMatch(UnitRig::transport)) {
                    assertInHex(instance.nodes, model.horizontalScale(unit));
                }
                for (var member : instance.nodes) {
                    assertEquals(model.instance.getNode(member.id).scale, member.scale, "Layout must not shrink the artwork");
                }
            }
        } finally {
            BoardGeometry.tune(original);
        }
        animator.apply(model, instance, unit, UnitMotion.Sample.STILL, 0, 0, true, 0);
        return instance;
    }

    private static void assertDeferredEquipment(GpuUnitModels library, BoardScene.UnitModel selection, int expectedTriangles) {
        var original = selection.state();
        var body = original.structure();
        var weapon = new UnitModelEquipment.Mount(99, "ISMediumLaser", "BD", "", false, false, 0,
              EquipmentModelPolicy.WEAPON, "laser", List.of());
        var state = new UnitModelState(new UnitModelState.Structure(body.movement(), List.of(weapon), body.members(),
              body.activeTroopers(), body.externalSearchlight(), body.anatomy(), body.bodyForm()), original.appearance(), original.pose());
        var equipped = library.get(new BoardScene.UnitModel(selection.asset(), selection.fallback(), selection.variant(),
              selection.figures(), selection.twist(), selection.damage(), state), 9000);
        assertTrue(equipped.equipment().isEmpty(), "Dynamic infantry/BA equipment is deliberately deferred");
        assertEquals(expectedTriangles, triangles(equipped.instance.nodes));
    }

    static void assertInHex(Iterable<Node> nodes, float scale) {
        Coords hex = new Coords(0, 0);
        for (Node node : nodes) {
            for (var part : node.parts) {
                var mesh = part.meshPart.mesh;
                int stride = mesh.getVertexSize() / Float.BYTES;
                float[] vertices = mesh.getVertices(new float[mesh.getNumVertices() * stride]);
                short[] indices = new short[mesh.getNumIndices()];
                mesh.getIndices(indices);
                for (int index = part.meshPart.offset; index < part.meshPart.offset + part.meshPart.size; index++) {
                    int offset = Short.toUnsignedInt(indices[index]) * stride;
                    Vector3 vertex = new Vector3(vertices[offset], vertices[offset + 1], vertices[offset + 2])
                          .mul(node.globalTransform).scl(scale);
                    assertTrue(BoardGeometry.contains(hex, vertex.x + BoardGeometry.centerX(hex),
                          vertex.y + BoardGeometry.centerY(hex)), node.id + ": " + vertex);
                }
            }
            assertInHex(node.getChildren(), scale);
        }
    }

    private static int triangles(Iterable<Node> nodes) {
        int total = 0;
        for (Node node : nodes) {
            for (var part : node.parts) {
                total += part.meshPart.size / 3;
            }
            total += triangles(node.getChildren());
        }
        return total;
    }

    /** The six review angles, laid out three across: front, back, left / right, above, three-quarter. */
    private static final String[] FULL_VIEWS = { "front", "back", "left", "right", "above", "three-quarter" };

    private static void place(OrthographicCamera camera, String view, float focus) {
        switch (view) {
            case "front" -> camera.position.set(0, 300, focus);
            case "back" -> camera.position.set(0, -300, focus);
            case "left" -> camera.position.set(-300, 0, focus);
            case "right" -> camera.position.set(300, 0, focus);
            case "above" -> camera.position.set(0, 0, 300);
            default -> camera.position.set(170, 210, focus + 90);
        }
        // Looking down needs forward as up, so the nose runs up the page like the game sprite.
        camera.up.set("above".equals(view) ? Vector3.Y : Vector3.Z);
        camera.lookAt(0, 0, "above".equals(view) ? 0 : focus);
    }

    /** One sheet carrying every review angle of the assembled unit, weapons included. */
    static void renderFullReview(ModelBatch batch, List<ModelInstance> instances, String name, String unit,
          float width, float focus) {
        var environment = new Environment();
        environment.set(ColorAttribute.createAmbientLight(.7f, .7f, .7f, 1));
        environment.add(new DirectionalLight().set(.8f, .8f, .8f, -.4f, -.7f, -1));
        // A sheet shows every side at once, so the faces turned away from the key light need a fill
        // or the back and side cells read as flat grey.
        environment.add(new DirectionalLight().set(.38f, .38f, .38f, .5f, .8f, -.35f));
        File output = new File(System.getProperty("megamek.gpu.screenshots"));
        assertTrue(output.isDirectory() || output.mkdirs());
        int columns = 3;
        int rows = (FULL_VIEWS.length + columns - 1) / columns;
        int cellWidth = Gdx.graphics.getWidth() / columns;
        int cellHeight = Gdx.graphics.getHeight() / rows;
        var camera = new OrthographicCamera(width, width * cellHeight / (float) cellWidth);
        camera.near = 1;
        camera.far = 1000;
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(.24f, .16f, .13f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        for (int index = 0; index < FULL_VIEWS.length; index++) {
            // A viewport does not bound glClear, so depth is cleared whole between cells; the
            // colour buffer is left alone and each cell paints into its own rectangle.
            Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);
            Gdx.gl.glViewport((index % columns) * cellWidth,
                  (rows - 1 - index / columns) * cellHeight, cellWidth, cellHeight);
            place(camera, FULL_VIEWS[index], focus);
            camera.update();
            batch.begin(camera);
            for (var instance : instances) {
                batch.render(instance, environment);
            }
            batch.end();
        }
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        caption(unit);
        Pixmap pixels = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        try {
            PixmapIO.writePNG(new FileHandle(new File(output, name + "-full.png")), pixels, -1, true);
        } finally {
            pixels.dispose();
        }
    }

    /** Names the unit in the top right, so a sheet is identifiable once it leaves the build directory. */
    private static void caption(String text) {
        var generator = new FreeTypeFontGenerator(new FileHandle(
              new File(Configuration.fontsDir(), "Noto Sans/NotoSans-Bold.ttf")));
        var settings = new FreeTypeFontGenerator.FreeTypeFontParameter();
        settings.size = 26;
        BitmapFont font = generator.generateFont(settings);
        var batch = new SpriteBatch();
        try {
            font.setColor(1, 1, 1, .85f);
            var layout = new GlyphLayout(font, text);
            batch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
            batch.begin();
            font.draw(batch, layout, Gdx.graphics.getWidth() - layout.width - 18,
                  Gdx.graphics.getHeight() - 16);
            batch.end();
        } finally {
            batch.dispose();
            font.dispose();
            generator.dispose();
        }
    }

    static void renderReview(ModelBatch batch, List<ModelInstance> instances, String name, float width, float focus) {
        var environment = new Environment();
        environment.set(ColorAttribute.createAmbientLight(.7f, .7f, .7f, 1));
        environment.add(new DirectionalLight().set(.8f, .8f, .8f, -.4f, -.7f, -1));
        var camera = new OrthographicCamera(width, width * .625f);
        camera.near = 1;
        camera.far = 1000;
        File output = new File(System.getProperty("megamek.gpu.screenshots"));
        assertTrue(output.isDirectory() || output.mkdirs());
        for (boolean top : new boolean[] { false, true }) {
            camera.position.set(top ? new Vector3(0, 0, 300) : new Vector3(70, 200, 150));
            camera.up.set(top ? Vector3.Y : Vector3.Z);
            camera.lookAt(0, 5, top ? 0 : focus);
            camera.update();
            Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            Gdx.gl.glClearColor(.24f, .16f, .13f, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
            batch.begin(camera);
            for (var instance : instances) {
                batch.render(instance, environment);
            }
            batch.end();
            Pixmap pixels = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            try {
                PixmapIO.writePNG(new FileHandle(new File(output, name + "-" + (top ? "top" : "isometric")
                      + ".png")), pixels, -1, true);
            } finally {
                pixels.dispose();
            }
        }
    }
}

/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.swing.SwingUtilities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalShadowLight;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import megamek.client.ui.tileset.EquipmentModelPolicy;
import megamek.client.ui.tileset.UnitModelEquipment;
import megamek.client.ui.tileset.MekTileset;
import megamek.common.Configuration;
import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.game.Game;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.EquipmentType;
import megamek.common.icons.Camouflage;
import megamek.common.equipment.GunEmplacement;
import megamek.common.equipment.HandheldWeapon;
import megamek.common.battlefieldSupport.BattlefieldSupportAsset;
import megamek.common.battlefieldSupport.BFSAssetType;
import megamek.common.options.OptionsConstants;
import megamek.common.board.Coords;
import megamek.common.loaders.MekFileParser;
import megamek.common.units.Dropship;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.ProneCause;
import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.FighterSquadron;
import megamek.common.units.Tank;
import megamek.common.units.ProtoMek;
import megamek.common.units.SmallCraft;
import megamek.common.units.Jumpship;
import megamek.common.units.Warship;
import megamek.common.units.SpaceStation;
import megamek.common.units.CombatVehicleEscapePod;
import megamek.common.units.LandAirMek;
import megamek.common.units.QuadVee;
import megamek.common.units.Mek;

/** Native family, side-mounted equipment, footprint, placement and picking integration review. */
final class GpuFamilyAssemblyReview {
    private GpuFamilyAssemblyReview() { }

    static void verify(GpuUnitModels library, ModelBatch batch) throws Exception {
        var families = List.of("tracked", "wheeled", "hover", "wige", "rail", "vtol", "airship", "fighter",
              "aerodyne", "spheroid", "small-spheroid", "jumpship", "warship", "station", "naval", "hydrofoil",
              "submarine", "proto", "quad-proto", "glider-proto", "emplacement", "structure", "escape-pod", "missile");
        for (int page = 0; page < 4; page++) {
            List<ModelInstance> instances = new ArrayList<>();
            for (int cell = 0; cell < 6; cell++) {
                int index = page * 6 + cell;
                var family = families.get(index);
                var model = library.get(selection(family, List.of()), 5000 + index);
                assertNotNull(model, family);
                assertFalse(model.turnsUpperBody(), family);
                model.instance.transform.setToTranslation((cell % 3 - 1) * 90, (cell / 3 == 0 ? -1 : 1) * 50, 0);
                instances.add(model.instance);
            }
            GpuModularUnitModelsSmokeTest.renderReview(batch, instances, "runtime-family-" + (page + 1), 330, 20);
        }
        sideWeapons(library, batch);
        liveFamilies(library, batch);
        squadron(library, batch);
        largeShip(library, batch);
    }

    private static void liveFamilies(GpuUnitModels library, ModelBatch batch) throws Exception {
        var tileset = new MekTileset(Configuration.unitImagesDir());
        tileset.loadFromFile("mekset.txt");
        int id = 5300;
        for (var entity : List.of(new Tank(), new AeroSpaceFighter(), new ProtoMek(), new SmallCraft(),
              new Jumpship(), new Warship(), new SpaceStation(), new GunEmplacement(), new HandheldWeapon(),
              new CombatVehicleEscapePod())) {
            entity.setWeight(50);
            var selection = UnitModelSelection.capture(entity, -1, false, tileset);
            assertNotNull(selection, entity.getClass().getSimpleName());
            assertNotNull(library.get(selection, id++), entity.getClass().getSimpleName());
        }
        var tank = new Tank();
        tank.setWeight(60);
        tank.setHasNoTurret(true);
        for (var mode : List.of(EntityMovementMode.WHEELED, EntityMovementMode.HOVER, EntityMovementMode.WIGE,
              EntityMovementMode.AIRSHIP, EntityMovementMode.NAVAL, EntityMovementMode.HYDROFOIL, EntityMovementMode.SUBMARINE)) {
            tank.setMovementMode(mode);
            var model = library.get(UnitModelSelection.capture(tank, -1, false, tileset), id++);
            assertNotNull(model, mode.name());
            if (mode == EntityMovementMode.SUBMARINE || mode == EntityMovementMode.NAVAL) {
                assertTrue(bounds(model.instance).min.z < 0, "The keel is below the waterline");
            }
        }
        for (var type : List.of(BFSAssetType.CONV_INFANTRY, BFSAssetType.BATTLE_ARMOR, BFSAssetType.VEHICLE, BFSAssetType.EMPLACEMENT)) {
            var marker = new BattlefieldSupportAsset();
            marker.setAssetType(type);
            var model = library.get(UnitModelSelection.capture(marker, -1, false, tileset), id++);
            assertNotNull(model, type.name());
            assertTrue(bounds(model.instance).isValid(), "Support markers must not be empty");
            assertTrue(model.equipment().isEmpty());
        }
        var lam = new LandAirMek(Mek.GYRO_STANDARD, Mek.COCKPIT_STANDARD, LandAirMek.LAM_STANDARD);
        lam.setWeight(50);
        var vee = new QuadVee();
        vee.setWeight(70);
        List<ModelInstance> forms = new ArrayList<>();
        for (int mode : List.of(LandAirMek.CONV_MODE_MEK, LandAirMek.CONV_MODE_AIR_MEK, LandAirMek.CONV_MODE_FIGHTER)) {
            lam.setConversionMode(mode);
            var model = library.get(UnitModelSelection.capture(lam, -1, false, tileset), id++);
            assertNotNull(model);
            model.instance.transform.setToTranslation((mode - 1) * 70, 0, 0);
            forms.add(model.instance);
        }
        GpuModularUnitModelsSmokeTest.renderReview(batch, forms, "runtime-lam-forms", 270, 20);
        for (int mode : List.of(QuadVee.CONV_MODE_MEK, QuadVee.CONV_MODE_VEHICLE)) {
            vee.setConversionMode(mode);
            assertNotNull(library.get(UnitModelSelection.capture(vee, -1, false, tileset), id++));
        }
    }

    private static void squadron(GpuUnitModels library, ModelBatch batch) throws Exception {
        var game = new Game();
        game.setPhase(GamePhase.LOUNGE);
        game.getOptions().getOption(OptionsConstants.ADVANCED_AERO_RULES_ALLOW_LARGE_SQUADRONS).setValue(true);
        var owner = new Player(0, "Squadron review");
        game.addPlayer(owner.getId(), owner);
        var squadron = new FighterSquadron();
        squadron.setId(5400);
        squadron.setOwner(owner);
        game.addEntity(squadron, false);
        List<AeroSpaceFighter> fighters = new ArrayList<>();
        for (int index = 0; index < 10; index++) {
            var fighter = new AeroSpaceFighter();
            fighter.setId(5401 + index);
            fighter.setOwner(owner);
            fighter.setWeight(40);
            fighter.addEquipment(EquipmentType.get("ISMediumLaser"), AeroSpaceFighter.LOC_NOSE);
            game.addEntity(fighter, false);
            squadron.load(fighter, false, -1);
            fighters.add(fighter);
        }
        var tileset = new MekTileset(Configuration.unitImagesDir());
        tileset.loadFromFile("mekset.txt");
        var selection = UnitModelSelection.capture(squadron, -1, false, tileset);
        var model = library.get(selection, squadron.getId());
        assertNotNull(model);
        assertEquals(10, model.instance.nodes.size);
        assertEquals(10, model.equipment().size());
        assertEquals(10, model.equipment().stream().map(UnitEquipmentAssembly.Binding::memberId).distinct().count());
        assertTrue(library.modular("units/modular/bodies/family-flight-fighter.json").triangles() * 10 < 1000);
        for (var binding : model.equipment()) {
            var point = new Vector3();
            UnitModelAttachment.emitter(model.instance, binding.emitters().getFirst(), point, new Vector3());
            assertTrue(Float.isFinite(point.x));
        }
        GpuModularUnitModelsSmokeTest.renderReview(batch, List.of(model.instance), "runtime-squadron-ten", 185, 0);
        fighters.getFirst().setCamouflage(new Camouflage("test", "alternate.png"));
        fighters.getFirst().setFacing(2);
        fighters.getFirst().getEquipment(0).setDestroyed(true);
        var changed = UnitModelSelection.capture(squadron, -1, false, tileset);
        assertEquals(selection.state().structure(), changed.state().structure());
        assertSame(model, library.get(changed, squadron.getId()), "Member appearance must not rebuild a squadron");
        assertTrue(changed.state().appearance().fighters().get(fighters.getFirst().getId()).inoperableEquipment().contains(0));
        assertTrue(selection.state().appearance().fighters().get(fighters.getFirst().getId()).inoperableEquipment().isEmpty());
        var damaged = new ModelInstance(model.instance.model);
        model.showEquipment(damaged, changed.state().appearance());
        for (var binding : model.equipment()) {
            var parts = UnitDamageDisplay.locationParts(damaged, binding.node());
            assertFalse(parts.isEmpty());
            assertTrue(parts.stream().allMatch(part -> part.material.id.endsWith(UnitDamageDisplay.WRECKED_SUFFIX)
                  == (binding.memberId() == fighters.getFirst().getId())), "Each fighter owns equipment index zero independently");
        }
        fighters.get(4).setDestroyed(true);
        model = library.get(UnitModelSelection.capture(squadron, -1, false, tileset), squadron.getId());
        assertEquals(9, model.instance.nodes.size);
        assertEquals(9, model.equipment().size());
        assertTrue(model.equipment().stream().noneMatch(binding -> binding.memberId() == fighters.get(4).getId()));
        assertEquals(10, selection.state().structure().bodyForm().fighters().size());
    }

    private static BoardScene.UnitModel selection(String family, List<UnitModelEquipment.Mount> equipment) {
        var structure = new UnitModelState.Structure(EntityMovementMode.NONE, equipment, List.of(), 0, false, null,
              new UnitModelState.BodyForm(family, 3, 2));
        return new BoardScene.UnitModel("units/modular/families/" + family + ".json", null, family, 1, 0,
              BoardScene.LocationDamage.NONE, new UnitModelState(structure,
                    new UnitModelState.Appearance(Set.of(), false, null), new UnitModelState.Pose(ProneCause.NONE, 0, 0)));
    }

    private static void sideWeapons(GpuUnitModels library, ModelBatch batch) {
        List<UnitModelEquipment.Mount> mounts = new ArrayList<>();
        for (String location : List.of("LS", "RS", "TU", "FR", "RR", "custom-location")) {
            mounts.add(new UnitModelEquipment.Mount(mounts.size(), "ISMediumLaser", location, "", false, false, 0,
                  EquipmentModelPolicy.WEAPON, "laser", List.of()));
        }
        var tank = library.get(selection("tracked", mounts), 5100);
        assertNotNull(tank);
        assertEquals(mounts.size(), tank.equipment().size());
        for (var binding : tank.equipment()) {
            assertFalse(binding.embedded(), binding.location());
            var point = new Vector3();
            var direction = new Vector3();
            UnitModelAttachment.emitter(tank.instance, binding.emitters().getFirst(), point, direction);
            Vector3 expected = switch (binding.location()) {
                case "LS" -> new Vector3(-1, 0, 0);
                case "RS" -> Vector3.X;
                case "RR" -> new Vector3(0, -1, 0);
                default -> Vector3.Y;
            };
            assertTrue(expected.epsilonEquals(direction, .001f), binding.location() + ": " + direction);
        }
        GpuModularUnitModelsSmokeTest.renderReview(batch, List.of(tank.instance), "runtime-vehicle-loadout", 120, 15);
    }

    private static void largeShip(GpuUnitModels library, ModelBatch batch) throws Exception {
        try (var fixture = GpuBoardFixture.create()) {
            var ship = (Dropship) new MekFileParser(new File(Configuration.dataDir(), "mekfiles/unit_files.zip"),
                  "dropships/TRO3057R/IS/Union (2708) (Cargo).blk").getEntity();
            SwingUtilities.invokeAndWait(() -> {
                ship.setId(5200);
                ship.setOwner(fixture.player);
                ship.setDeployed(true);
                fixture.game.addEntity(ship, false);
                ship.land();
                ship.setPosition(new Coords(4, 4));
                ship.getOccupiedCoords().forEach(coords -> fixture.game.getBoard().setHex(coords, new Hex(0)));
                fixture.game.getBoard().setHex(ship.getPosition().translated(1), new Hex(3));
                fixture.source.refresh();
            });
            var captured = fixture.source.takeFrame().scene();
            var unit = captured.units().stream().filter(item -> item.id() == ship.getId()).findFirst().orElseThrow();
            assertEquals(7, unit.footprint().size());
            assertEquals(3, unit.location().elevation());
            var scene = new BoardScene(captured.boardId(), captured.width(), captured.height(), captured.tiles(),
                  List.of(unit), List.of(), unit.id(), "Union placement review", List.of(), new BoardScene.Light(1, -.6f));
            var model = library.get(unit.model(), unit.id());
            assertNotNull(model);
            var terrain = new GpuTerrain();
            var picker = new UnitPicking();
            var camera = new BoardCamera();
            camera.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            try {
                terrain.update(scene);
                Vector3 ground = BoardGeometry.center(unit.location().coords(), unit.location().elevation());
                for (boolean top : List.of(false, true)) {
                    camera.setIsometric(!top);
                    camera.fit(scene);
                    camera.zoom(.72f);
                    camera.center(BoardGeometry.center(unit.location().coords(), 2));
                    model.place(model.instance, camera.camera, ground, 0, unit);
                    var bounds = bounds(model.instance);
                    assertTrue(bounds.min.z >= 3 * BoardGeometry.LEVEL, "Hull clears the highest occupied hex");
                    assertTrue(bounds.getWidth() > BoardGeometry.WIDTH * 1.8f, "The ship spans multiple hexes");
                    assertTrue(bounds.getHeight() > BoardGeometry.HEIGHT * 1.8f);
                    Vector3 target = bounds.getCenter(new Vector3());
                    var ray = new Ray(new Vector3(target).mulAdd(camera.camera.direction, -1000), camera.camera.direction);
                    float unitHit = picker.distance(model.instance, ray);
                    assertTrue(Float.isFinite(unitHit), "Rendered hull is pickable in either view");
                    var terrainHit = terrain.hit(scene, ray);
                    assertTrue(terrainHit == null || unitHit < terrainHit.distance(), "Hull is in front of the board");
                    terrain.renderShadows(camera.camera, List.of(model.instance));
                    Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                    Gdx.gl.glClearColor(.12f, .16f, .2f, 1);
                    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
                    terrain.render(camera.camera, false);
                    batch.begin(camera.camera);
                    batch.render(model.instance, terrain.environment());
                    batch.end();
                    GpuBoardTestUi.capture(new File(System.getProperty("megamek.gpu.screenshots"),
                          "runtime-union-terrain-" + (top ? "top" : "isometric") + ".png"));
                }
                var base = BoardGeometry.tuning();
                var original = bounds(model.instance);
                var skin = new GpuBoardSkin();
                try {
                    var tuning = new GpuBoardTuning(skin.skin);
                    Slider normalScale = tuning.panel().findActor("Unit scale");
                    Slider largeScale = tuning.panel().findActor("Multi-hex unit scale");
                    assertNotNull(largeScale);
                    assertEquals(BoardGeometry.DEFAULT_MULTI_HEX_UNIT_SCALE, largeScale.getValue(), .001f);
                    normalScale.setValue(.3f);
                    model.place(model.instance, camera.camera, ground, 0, unit);
                    assertEquals(original.getWidth(), bounds(model.instance).getWidth(), .001f);
                    largeScale.setValue(.7f);
                    assertEquals(.3f, BoardGeometry.UNIT_SCALE, .001f);
                    model.place(model.instance, camera.camera, ground, 0, unit);
                    assertEquals(original.getWidth() * .7f / base.multiHexUnitScale(), bounds(model.instance).getWidth(), .001f);
                    TextButton defaults = tuning.panel().findActor("tuning-defaults");
                    defaults.fire(new ChangeListener.ChangeEvent());
                    assertEquals(BoardGeometry.DEFAULTS.unitScale(), normalScale.getValue(), .001f);
                    assertEquals(BoardGeometry.DEFAULT_MULTI_HEX_UNIT_SCALE, largeScale.getValue(), .001f);
                } finally {
                    skin.dispose();
                    BoardGeometry.tune(base);
                }
                // The same footprint with a different gameplay height must keep its resting proportions.
                var low = new BoardScene.Unit(unit.id(), unit.part(), unit.name(), unit.location(), unit.image(), false,
                      unit.annotations(), 1, false, unit.model(), unit.outlineRgb(), unit.footprint());
                model.place(model.instance, camera.camera, ground, 0, low);
                assertEquals(original.getDepth(), bounds(model.instance).getDepth(), .001f);
                terrain.renderShadows(camera.camera, List.of(model.instance));
                long restShadow = shadowChecksum(terrain);
                model.instance.getNode("hull").rotation.set(Vector3.Z, 45);
                model.instance.calculateTransforms();
                var exact = bounds(model.instance);
                var approximate = UnitBounds.world(model.instance);
                approximate.set(new Vector3(approximate.min).sub(.001f, .001f, .001f),
                      new Vector3(approximate.max).add(.001f, .001f, .001f));
                assertTrue(approximate.contains(exact),
                      "Posed part bounds must contain the rendered geometry");
                terrain.renderShadows(camera.camera, List.of(model.instance));
                assertTrue(restShadow != shadowChecksum(terrain), "Changing joints refreshes the shadow with the root unchanged");
            } finally {
                picker.clear();
                terrain.dispose();
            }
        }
    }

    private static BoundingBox bounds(ModelInstance instance) {
        return instance.calculateBoundingBox(new BoundingBox()).mul(instance.transform);
    }

    static long shadowChecksum(GpuTerrain terrain) {
        var buffer = ((DirectionalShadowLight) terrain.environment().shadowMap).getFrameBuffer();
        buffer.begin();
        Pixmap pixels = Pixmap.createFromFrameBuffer(0, 0, buffer.getWidth(), buffer.getHeight());
        try {
            var checksum = new java.util.zip.CRC32();
            checksum.update(pixels.getPixels());
            return checksum.getValue();
        } finally {
            pixels.dispose();
            buffer.end();
        }
    }
}

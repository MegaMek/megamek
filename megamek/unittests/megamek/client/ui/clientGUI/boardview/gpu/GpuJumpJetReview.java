/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.math.Vector3;
import megamek.client.ui.tileset.MekTileset;
import megamek.common.Configuration;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.units.BipedMek;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.EntityMovementType;
import megamek.common.units.Mek;

/** Real posed packs/modules, native VFX rendering, and lifecycle/visibility checks on the production jump timeline. */
final class GpuJumpJetReview {
    private static final BoardScene.Waypoint START = new BoardScene.Waypoint(new Coords(2, 3), 0, 0);
    private static final BoardScene.Waypoint END = new BoardScene.Waypoint(new Coords(2, 2), 0, 0);

    private GpuJumpJetReview() { }

    static void verify(GpuUnitModels library, ModelBatch batch) throws Exception {
        var tileset = new MekTileset(Configuration.unitImagesDir());
        tileset.loadFromFile("mekset.txt");
        var infantry = new ConvInfantry();
        infantry.setId(6500);
        infantry.initializeInternal(28, ConvInfantry.LOC_INFANTRY);
        infantry.setMovementMode(EntityMovementMode.INF_JUMP);
        var armor = new BattleArmor();
        armor.setId(6501);
        armor.setSquadSize(6);
        for (int member = 1; member <= 6; member++) {
            armor.initializeInternal(1, member);
        }
        var mek = new BipedMek();
        mek.setId(6502);
        mek.setWeight(50);
        for (int location = 0; location < mek.locations(); location++) {
            mek.initializeInternal(10, location);
        }
        EquipmentType.initializeTypes();
        mek.addEquipment(EquipmentType.get("Jump Jet"), Mek.LOC_LEFT_TORSO);
        mek.addEquipment(EquipmentType.get("Jump Jet"), Mek.LOC_RIGHT_TORSO);
        var camera = new OrthographicCamera(200, 200 * .625f);
        var center = BoardGeometry.center(START.coords(), 0).lerp(BoardGeometry.center(END.coords(), 0), .5f);
        camera.position.set(center).add(75, -170, 155);
        camera.up.set(Vector3.Z);
        camera.near = 1;
        camera.far = 1000;
        camera.lookAt(center.x, center.y, 30);
        camera.update();
        for (Entity entity : List.of(infantry, armor, mek)) {
            var selection = UnitModelSelection.capture(entity, -1, false, tileset);
            var model = library.get(selection, entity.getId());
            var instance = new ModelInstance(model.instance.model);
            var unit = unit(entity, selection, false);
            var animator = new UnitAnimator();
            var motion = jump();
            double step = motion.remainingSeconds() / 48;
            var jets = new GpuJumpJets();
            String family = entity instanceof Mek ? "mek" : entity instanceof BattleArmor ? "ba" : "infantry";
            int expected = entity instanceof Mek ? 2 : entity instanceof BattleArmor ? 12 : 6;
            try {
                for (int frame = 0; frame <= 48; frame++) {
                    if (frame > 0) {
                        motion.advance(step, 1);
                    }
                    animator.apply(model, instance, unit, motion.sample(), (float) (frame * step), (float) step, false, 0);
                    model.place(instance, camera, motion.position(), 0, unit);
                    if (frame == 10) {
                        assertThrustFollowsTravel(model, instance);
                    }
                    jets.beginFrame();
                    jets.update("review", model, instance, unit, motion.sample());
                    jets.endFrame();
                    assertEquals(frame == 48 ? 0 : expected, jets.emitterCount(), family + " frame " + frame);
                    assertTrue(jets.smokeCount() <= expected * GpuJumpJets.PUFFS_PER_EMITTER);
                    if (entity instanceof BattleArmor || frame == 10 || frame == 24 || frame == 40 || frame == 48) {
                        render(batch, camera, instance, jets, "runtime-jump-" + family + "-" + String.format("%02d", frame),
                              frame == 24 || frame == 40);
                    }
                }
                assertEquals(0, jets.smokeCount(), "The last frame has neither flame nor smoke");
                motion = jump();
                motion.advance(.2, 1);
                jets.beginFrame();
                jets.update("review", model, instance, unit, motion.sample());
                jets.endFrame();
                assertEquals(expected, jets.emitterCount());
                jets.beginFrame();
                jets.update("review", model, instance, unit(entity, selection, true), motion.sample());
                jets.endFrame();
                assertEquals(0, jets.emitterCount(), "Sensor contacts cannot reveal exhaust or troop counts");
                assertEquals(0, jets.smokeCount());
                assertEquals(smokeAt(model, instance, unit, 30), smokeAt(model, instance, unit, 60),
                      "The same playback time emits the same number of puffs at different frame rates");
                if (entity instanceof Mek) {
                    mek.getEquipment(0).setDestroyed(true);
                    var damaged = UnitModelSelection.capture(mek, -1, false, tileset);
                    jets.beginFrame();
                    jets.update("review", model, instance, unit(mek, damaged, false), motion.sample());
                    jets.endFrame();
                    assertEquals(1, jets.emitterCount(), "Inoperable mounted jump jets cannot emit");
                }
                jets.clear();
                jets.beginFrame();
                for (int count = 0; count < 140; count++) {
                    jets.update("capacity-" + count, model, instance, unit, motion.sample());
                }
                jets.endFrame();
                assertEquals(GpuJumpJets.MAX_EMITTERS, jets.emitterCount());
                jets.render(camera);
                jets.beginFrame();
                jets.endFrame();
                assertEquals(0, jets.emitterCount(), "Removed units release their effect state");
            } finally {
                jets.dispose();
            }
        }
        assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
    }

    private static int smokeAt(GpuUnitModel model, ModelInstance instance, BoardScene.Unit unit, int framesPerSecond) {
        var jets = new GpuJumpJets();
        var motion = jump();
        try {
            for (int frame = 0; frame <= framesPerSecond * 3 / 5; frame++) {
                if (frame > 0) {
                    motion.advance(1d / framesPerSecond, 1);
                }
                jets.beginFrame();
                jets.update("same-seed", model, instance, unit, motion.sample());
                jets.endFrame();
            }
            int count = jets.smokeCount();
            assertTrue(count > 0);
            motion.finish();
            jets.beginFrame();
            jets.update("same-seed", model, instance, unit, motion.sample());
            jets.endFrame();
            assertEquals(0, jets.smokeCount(), "Skipping clears already emitted smoke");
            return count;
        } finally {
            jets.dispose();
        }
    }

    private static void assertThrustFollowsTravel(GpuUnitModel model, ModelInstance instance) {
        var travel = BoardGeometry.center(END.coords(), 0).sub(BoardGeometry.center(START.coords(), 0)).nor();
        var exhaust = new Vector3();
        int checked = 0;
        for (var rig : model.rigs()) {
            var container = rig.container() == null ? null : instance.getNode(rig.container());
            for (var emitter : rig.emitters()) {
                if (!"exhaust".equals(emitter.role())) { continue; }
                var node = UnitAnimator.find(container == null ? instance.nodes : container.getChildren(), emitter.node());
                UnitModelAttachment.emitter(instance, node, emitter, new Vector3(), exhaust);
                assertTrue(exhaust.dot(travel) < -.05f && exhaust.z < -.5f, "The pack tilts with its jumping troop");
                checked++;
            }
        }
        for (var binding : model.equipment()) {
            for (var emitter : binding.emitters()) {
                if (!"exhaust".equals(emitter.role())) { continue; }
                UnitModelAttachment.emitter(instance, emitter, new Vector3(), exhaust);
                assertTrue(exhaust.dot(travel) < -.05f && exhaust.z < -.5f, "Mounted jets inherit the jumping body's tilt");
                checked++;
            }
        }
        assertTrue(checked > 0);
    }

    private static UnitMotion jump() {
        var motion = new UnitMotion(START);
        motion.append(List.of(START, END), EntityMovementType.MOVE_JUMP, 3);
        return motion;
    }

    private static BoardScene.Unit unit(Entity entity, BoardScene.UnitModel model, boolean sensor) {
        return new BoardScene.Unit(entity.getId(), -1, "Jump review", END,
              new BoardScene.Pixels(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)), sensor, null,
              entity.height() + 1, false, model, 0xFFFFFF);
    }

    private static void render(ModelBatch batch, OrthographicCamera camera, ModelInstance instance, GpuJumpJets jets,
          String name, boolean verifyBlue) {
        var environment = new Environment();
        environment.set(ColorAttribute.createAmbientLight(.7f, .7f, .7f, 1));
        environment.add(new DirectionalLight().set(.8f, .8f, .8f, -.4f, -.7f, -1));
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(.11f, .14f, .18f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        batch.begin(camera);
        batch.render(instance, environment);
        batch.end();
        Pixmap before = verifyBlue ? Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()) : null;
        if (before != null) {
            Gdx.gl.glClearDepthf(0);
            Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);
            try {
                jets.render(camera);
                Pixmap hidden = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                try {
                    assertEquals(before.getPixels(), hidden.getPixels(), "Exhaust must respect foreground depth");
                } finally {
                    hidden.dispose();
                }
            } finally {
                Gdx.gl.glClearDepthf(1);
                Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);
            }
            batch.begin(camera);
            batch.render(instance, environment);
            batch.end();
        }
        jets.render(camera);
        Pixmap pixels = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        try {
            if (before != null) {
                int bluePixels = 0;
                for (int y = 0; y < pixels.getHeight(); y++) {
                    for (int x = 0; x < pixels.getWidth(); x++) {
                        int old = before.getPixel(x, y), color = pixels.getPixel(x, y);
                        int blue = (color >>> 8) & 255;
                        if (blue > ((old >>> 8) & 255) + 10 && blue > (color >>> 24) + 30) {
                            bluePixels++;
                        }
                    }
                }
                assertTrue(bluePixels > 30, name + " has visible blue exhaust: " + bluePixels);
            }
            File directory = new File(System.getProperty("megamek.gpu.screenshots"));
            PixmapIO.writePNG(new FileHandle(new File(directory, name + ".png")), pixels, -1, true);
        } finally {
            pixels.dispose();
            if (before != null) {
                before.dispose();
            }
        }
    }
}

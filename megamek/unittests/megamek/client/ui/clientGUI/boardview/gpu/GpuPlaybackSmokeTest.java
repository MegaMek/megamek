/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import megamek.client.ui.tileset.MekTileset;
import megamek.common.Configuration;
import megamek.common.board.Coords;
import megamek.common.loaders.MekFileParser;
import megamek.common.units.EntityMovementType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Measure actual posed feet against ground travel, not just whether two animation clocks agree. */
@Tag("on-demand")
class GpuPlaybackSmokeTest {
    @Test
    void plantedFeetFollowTravelWithoutSliding() {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        new Lwjgl3Application(new ApplicationAdapter() {
            @Override
            public void create() {
                var library = new GpuUnitModels();
                try {
                    var tileset = new MekTileset(Configuration.unitImagesDir());
                    tileset.loadFromFile("mekset.txt");
                    var mek = new MekFileParser(new File("testresources/megamek/common/units/Atlas AS7-D.mtf")).getEntity();
                    mek.setId(9060);
                    var selection = UnitModelSelection.capture(mek, -1, false, tileset);
                    var model = library.get(selection, mek.getId());
                    var start = new BoardScene.Waypoint(new Coords(2, 9), 0, 0);
                    var finish = new BoardScene.Waypoint(new Coords(2, 1), 0, 0);
                    var unit = new BoardScene.Unit(mek.getId(), -1, "Atlas", finish, null, false, null, 3, false, selection, 0);
                    var camera = new OrthographicCamera(250, 180);
                    camera.up.set(Vector3.Z);
                    for (int facing : List.of(0, 3)) {
                        for (var type : List.of(EntityMovementType.MOVE_WALK, EntityMovementType.MOVE_RUN)) {
                            for (int mp : List.of(3, 12, 40)) {
                                var instance = new ModelInstance(model.instance.model);
                                var animator = new UnitAnimator();
                                var departure = new BoardScene.Waypoint(start.coords(), 0, facing);
                                var arrival = new BoardScene.Waypoint(finish.coords(), 0, facing);
                                var motion = new UnitMotion(departure);
                                motion.append(List.of(departure, arrival), type, 0, false, mp);
                                float dt = (float) (motion.remainingSeconds() / 480);
                                var previous = new Vector3();
                                var previousRoot = new Vector3();
                                int previousSupport = -1;
                                float drift = 0, travel = 0;
                                for (int frame = 0; frame < 480; frame++) {
                                    motion.advance(dt, 1);
                                    animator.apply(model, instance, unit, motion.sample(), frame * dt, dt, false, 0);
                                    model.place(instance, camera, motion.position(), motion.facing(), unit);
                                    var left = foot(instance, model.rigs().getFirst().joints().get("leftFoot"));
                                    var right = foot(instance, model.rigs().getFirst().joints().get("rightFoot"));
                                    int support = left.z < right.z ? 0 : 1;
                                    var planted = support == 0 ? left : right;
                                    if (frame > 0 && motion.sample().progress() > .15f && motion.sample().progress() < .85f
                                          && support == previousSupport && Math.abs(planted.z - previous.z) < .02f) {
                                        drift += (float) Math.hypot(planted.x - previous.x, planted.y - previous.y);
                                        travel += motion.position().dst(previousRoot);
                                    }
                                    previous.set(planted);
                                    previousRoot.set(motion.position());
                                    previousSupport = support;
                                }
                                System.out.println((facing == 0 ? "Forward " : "Reverse ") + type + " " + mp + " MP planted-foot drift / travel = " + drift / travel);
                                assertTrue(travel > 30 && drift / travel < .12f,
                                      type + " " + mp + " MP slides: planted feet travel " + drift + " while body travels " + travel);
                            }
                        }
                    }
                    GpuPlaybackReview.verify(library, tileset, mek);
                } catch (Throwable error) {
                    failure.set(error);
                } finally {
                    library.dispose();
                    Gdx.app.exit();
                }
            }
        }, GpuBoardWindow.configuration(false));
        if (failure.get() != null) {
            throw new AssertionError("Playback review failed", failure.get());
        }
    }

    private static Vector3 foot(ModelInstance instance, String node) {
        return instance.getNode(node).globalTransform.getTranslation(new Vector3()).mul(instance.transform);
    }
}

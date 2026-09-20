/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import megamek.client.ui.tileset.MekTileset;
import megamek.common.Configuration;
import megamek.common.ResolvedAttack;
import megamek.common.board.Coords;
import megamek.common.loaders.MekFileParser;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Live and restored wrecks share a terminal pose, without re-enabling damaged attachments or troop children. */
@Tag("on-demand")
class GpuDestructionSmokeTest {
    @Test
    void everyFamilyCollapsesAndRestoresTheSameWreckInBothViews() {
        var failure = new AtomicReference<Throwable>();
        new Lwjgl3Application(new ApplicationAdapter() {
            @Override
            public void create() {
                var library = new GpuUnitModels();
                var effects = new GpuAttackEffects();
                try (var renderer = new GpuPlaybackReview.ReviewRenderer()) {
                    var tileset = new MekTileset(Configuration.unitImagesDir());
                    tileset.loadFromFile("mekset.txt");
                    var selections = new ArrayList<BoardScene.UnitModel>();
                    for (String family : List.of("tracked", "wheeled", "hover", "wige", "rail", "vtol", "airship", "fighter",
                          "aerodyne", "spheroid", "small-spheroid", "jumpship", "warship", "station", "naval", "hydrofoil",
                          "submarine", "proto", "quad-proto", "glider-proto", "emplacement", "structure", "escape-pod", "missile")) {
                        selections.add(GpuFamilyAssemblyReview.selection(family, List.of()));
                    }
                    for (String file : List.of("Atlas AS7-D.mtf", "Barghest BGS-1T.mtf", "Triskelion TRK-4V.mtf",
                          "Elemental BA [Laser] (Sqd5).blk", "Foot Platoon (AFFS) (Laser 3067+).blk")) {
                        selections.add(UnitModelSelection.capture(new MekFileParser(new File("testresources/megamek/common/units", file))
                              .getEntity(), -1, false, tileset));
                    }
                    int id = 9900;
                    for (var movement : List.of(megamek.common.units.EntityMovementMode.INF_MOTORIZED,
                          megamek.common.units.EntityMovementMode.TRACKED, megamek.common.units.EntityMovementMode.WHEELED,
                          megamek.common.units.EntityMovementMode.HOVER)) {
                        var infantry = new MekFileParser(new File("testresources/megamek/common/units", "Foot Platoon (AFFS) (Laser 3067+).blk"))
                              .getEntity();
                        infantry.setMovementMode(movement);
                        selections.add(UnitModelSelection.capture(infantry, -1, false, tileset));
                    }
                    for (var selection : selections) {
                        var model = library.get(selection, ++id);
                        assertNotNull(model);
                        var unit = unit(id, selection);
                        var instance = new ModelInstance(model.instance.model);
                        var animator = new UnitAnimator();
                        var death = new UnitAttack(UnitPlaybackTest.attack(unit, unit, ResolvedAttack.Kind.DEATH, true));
                        var origin = BoardGeometry.center(unit.location().coords(), 0);
                        for (boolean top : List.of(false, true)) {
                            renderer.topView = top;
                            for (int frame = 0; frame <= 3; frame++) {
                                death.seconds = UnitAttack.DEATH_SECONDS * frame / 3;
                                animator.apply(model, instance, unit, UnitMotion.Sample.STILL, 0, 0, true, 0);
                                animator.attack(model, unit, death);
                                model.place(instance, renderer.camera, origin, 0, unit);
                                assertTrue(UnitBounds.world(instance).isValid(), selection.asset());
                                effects.update(death, library, Map.of(id + ":-1", instance));
                                renderer.frame(List.of(instance), origin, effects, "death-" + id + (top ? "-top" : "-iso"), frame);
                            }
                        }
                        var terminal = new ModelInstance(instance);
                        var state = selection.state();
                        var pose = state.pose();
                        var wreck = new BoardScene.UnitModel(selection.asset(), selection.fallback(), selection.variant(), selection.figures(),
                              selection.twist(), selection.damage(), new UnitModelState(state.structure(), state.appearance(),
                                    new UnitModelState.Pose(pose.proneCause(), pose.facing(), pose.secondaryFacing(), pose.form(), true)));
                        var restored = new ModelInstance(model.instance.model);
                        new UnitAnimator().apply(model, restored, unit(id, wreck), UnitMotion.Sample.STILL, 0, 0, true, 0);
                        for (var rig : model.rigs()) {
                            var before = rig.container() == null ? terminal.nodes : terminal.getNode(rig.container()).getChildren();
                            var after = rig.container() == null ? restored.nodes : restored.getNode(rig.container()).getChildren();
                            for (String joint : rig.joints().values()) {
                                var a = UnitAnimator.find(before, joint);
                                var b = UnitAnimator.find(after, joint);
                                assertEquals(a.translation, b.translation, selection.asset() + " " + joint);
                                assertEquals(a.rotation, b.rotation, selection.asset() + " " + joint);
                            }
                        }
                        effects.update((UnitAttack) null, library, Map.of());
                        assertEquals(0, effects.missileCount());
                    }
                } catch (Throwable error) { failure.set(error); }
                finally { effects.dispose(); library.dispose(); Gdx.app.exit(); }
            }
        }, GpuBoardWindow.configuration(false));
        if (failure.get() != null) { throw new AssertionError("Destruction review failed", failure.get()); }
    }

    private static BoardScene.Unit unit(int id, BoardScene.UnitModel model) {
        return new BoardScene.Unit(id, -1, "Destruction review", new BoardScene.Waypoint(new Coords(2, 3), 0, 0),
              null, false, null, 2, false, model, 0);
    }
}

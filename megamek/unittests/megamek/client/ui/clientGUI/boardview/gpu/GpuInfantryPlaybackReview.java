/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import megamek.client.ui.tileset.MekTileset;
import megamek.common.board.Coords;
import megamek.common.units.ConvInfantry;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.EntityMovementType;

/** Runs inside the native playback smoke context with production assemblies and the shared event queue. */
final class GpuInfantryPlaybackReview {
    private GpuInfantryPlaybackReview() { }

    private record Placement(Vector3 position, Quaternion rotation) { }

    static void verify(GpuUnitModels library, MekTileset tileset) {
        for (var mode : List.of(EntityMovementMode.INF_MOTORIZED, EntityMovementMode.WHEELED,
              EntityMovementMode.TRACKED, EntityMovementMode.HOVER)) {
            for (int survivors : List.of(9, 28)) {
                var infantry = new ConvInfantry();
                infantry.setId(9400);
                infantry.setMovementMode(mode);
                infantry.initializeInternal(survivors, ConvInfantry.LOC_INFANTRY);
                var origin = new BoardScene.Waypoint(new Coords(2, 5), 0, 0);
                var firstEnd = new BoardScene.Waypoint(new Coords(4, 2), 0, 0);
                var first = unit(infantry, tileset, firstEnd);
                infantry.setInternal(9, ConvInfantry.LOC_INFANTRY);
                var last = unit(infantry, tileset, origin);
                var scene = UnitPlaybackTest.scene(last);
                List<BoardScene.Animation> events = List.of(
                      new BoardScene.Movement(9400, 0, List.of(origin, new BoardScene.Waypoint(new Coords(2, 3), 0, 0), firstEnd),
                            EntityMovementType.MOVE_WALK, 0, 4, first),
                      new BoardScene.Movement(9400, 0, List.of(firstEnd, new BoardScene.Waypoint(new Coords(3, 3), 0, 0), origin),
                            EntityMovementType.MOVE_WALK, 0, 4, last));
                var normal = new Formation(library).run(scene, events, false);
                var skipped = new Formation(library).run(scene, events, true);
                assertEquals(normal.keySet(), skipped.keySet());
                for (var member : normal.entrySet()) {
                    var expected = member.getValue();
                    var actual = skipped.get(member.getKey());
                    assertTrue(expected.position().epsilonEquals(actual.position(), .001f), mode + " " + member.getKey() + " skip position");
                    assertTrue(Math.abs(expected.rotation().dot(actual.rotation())) > .99999f,
                          mode + " " + member.getKey() + " skip orientation");
                }
                assertEquals(1, normal.keySet().stream().filter(key -> key.startsWith("vehicle-")).count(), "The final casualty formation has one vehicle");
            }
        }
    }

    private static BoardScene.Unit unit(ConvInfantry infantry, MekTileset tileset, BoardScene.Waypoint location) {
        var selected = UnitModelSelection.capture(infantry, -1, false, tileset);
        return new BoardScene.Unit(infantry.getId(), -1, "Infantry", location, null, false, null, 1, false, selected, 0);
    }

    private static final class Formation {
        final GpuUnitModels library;
        final UnitAnimator animator = new UnitAnimator();
        final UnitPlayback playback;
        final Map<String, Vector3> parked = new HashMap<>();
        ModelInstance instance;
        GpuMeeple model;
        long parkedSequence = -1;

        Formation(GpuUnitModels library) {
            this.library = library;
            playback = new UnitPlayback(move -> pose(move.unit(), 0, true));
        }

        private void pose(BoardScene.Unit unit, float seconds, boolean instant) {
            model = library.get(unit.model(), unit.id());
            if (instance == null || instance.model != model.instance.model) {
                instance = new ModelInstance(model.instance.model);
            }
            var motion = playback.motions.get(unit.id());
            animator.apply(model, instance, unit, motion.sample(), 0, seconds, instant, 0);
            var boarding = motion.sample().boarding();
            if (boarding != null && !instant) {
                if (boarding.sequence() != parkedSequence) {
                    parked.clear();
                    parkedSequence = boarding.sequence();
                }
                for (var rig : model.rigs()) {
                    var node = instance.getNode(rig.container());
                    if (rig.trooper() && boarding.stage() == UnitMotion.Stage.DRIVE) {
                        assertFalse(UnitBounds.subtree(node).isValid(), "No passengers emerge before the vehicles stop");
                    }
                    if (rig.transport() && boarding.stage() == UnitMotion.Stage.UNLOAD) {
                        var previous = parked.putIfAbsent(rig.container(), new Vector3(node.translation));
                        if (previous != null) {
                            assertTrue(previous.epsilonEquals(node.translation, .001f), "A vehicle moved while unloading");
                        }
                    }
                }
            }
        }

        Map<String, Placement> run(BoardScene scene, List<BoardScene.Animation> events, boolean instant) {
            playback.accept(events, scene, unit -> library.get(unit.model(), unit.id()).rigs().stream().anyMatch(UnitRig::transport));
            if (instant) {
                playback.advance(0, UnitMotion.Speed.INSTANT);
            } else {
                for (int frame = 0; playback.busy() && frame < 10000; frame++) {
                    playback.advance(1.0 / 30, UnitMotion.Speed.HALF);
                    pose(playback.present(scene).units().getFirst(), 1f / 120, false);
                }
            }
            assertFalse(playback.busy());
            Map<String, Placement> result = new HashMap<>();
            for (var rig : model.rigs()) {
                var node = instance.getNode(rig.container());
                assertTrue(UnitBounds.subtree(node).isValid(), "Every surviving member is visible after arrival");
                result.put(rig.container(), new Placement(new Vector3(node.translation), new Quaternion(node.rotation)));
            }
            return result;
        }
    }
}

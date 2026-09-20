/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.GdxNativesLoader;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import megamek.common.board.Coords;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.EntityMovementType;
import megamek.common.units.FallSide;
import megamek.common.units.ProneCause;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

/** Observable joint/contact behavior on a synthetic articulated body; no renderer or GPU allocation. */
class UnitLegBendTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final float FLOOR = .5f;
    private static final float PATH_TOLERANCE = .025f;
    private static final float CONTACT_TOLERANCE = .001f;

    @BeforeAll
    static void loadMathNatives() { GdxNativesLoader.load(); }

    @ParameterizedTest
    @CsvSource({
          "MOVE_WALK, 1, 0", "MOVE_WALK, -1, 0", "MOVE_WALK, 0, 1", "MOVE_WALK, 0, -1",
          "MOVE_RUN, 1, 0", "MOVE_RUN, -1, 0", "MOVE_RUN, 0, 1", "MOVE_RUN, 0, -1",
          "MOVE_WALK, 1, 1", "MOVE_RUN, -1, -1"
    })
    void bendChoicePreservesTravelAndGroundContact(EntityMovementType mode, float forward, float lateral) throws Exception {
        var travel = new Vector3(lateral, forward, 0).nor();
        var bendPlane = travel.cpy().scl(forward < 0 ? -1 : 1);
        try (var conventional = new Fixture(false); var reverse = new Fixture(true)) {
            Vector3[] previous = null;
            int[] planted = new int[2];
            for (int frame = 0; frame <= 1024; frame++) {
                // Hold the movement envelope at full weight while sampling four hexes of actual travel.
                float steps = frame / 256f;
                var motion = sample(mode, steps, travel.y, travel.x, ProneCause.NONE);
                var ground = travel.cpy().scl(steps * BoardGeometry.HEIGHT);
                conventional.apply(motion, ground);
                reverse.apply(motion, ground);
                var soles = new Vector3[2];
                for (int leg = 0; leg < 2; leg++) {
                    String side = leg == 0 ? "L" : "R";
                    Vector3 expected = conventional.sole(side);
                    soles[leg] = reverse.sole(side);
                    assertTrue(expected.epsilonEquals(soles[leg], PATH_TOLERANCE),
                          mode + " " + travel + " " + side + " frame " + frame + ": changing knee bend must preserve the foot path");
                    assertTrue(soles[leg].z >= FLOOR - PATH_TOLERANCE, "The foot cannot penetrate the ground");
                    conventional.assertKneeSide(side, bendPlane, 1);
                    reverse.assertKneeSide(side, bendPlane, -1);
                    if (previous != null) {
                        assertTrue(soles[leg].dst(previous[leg]) < BoardGeometry.HEIGHT / 256f * 8 + PATH_TOLERANCE,
                              "A knee branch change cannot teleport the foot between consecutive samples");
                        // Near-floor swing frames still travel; only a contacting sole must remain planted.
                        if (Math.abs(soles[leg].z - FLOOR) < CONTACT_TOLERANCE
                              && Math.abs(previous[leg].z - FLOOR) < CONTACT_TOLERANCE) {
                            float slip = soles[leg].dst(previous[leg]);
                            assertTrue(slip < .04f, mode + " " + travel + " " + side + " supporting foot drift at frame "
                                  + frame + ": from " + previous[leg] + " to " + soles[leg] + ", delta " + slip
                                  + ", heights above ground " + (previous[leg].z - FLOOR) + "/" + (soles[leg].z - FLOOR));
                            planted[leg]++;
                        }
                    }
                }
                previous = soles;
            }
            assertTrue(planted[0] > 100 && planted[1] > 100, "Both legs must spend meaningful time supporting the body");
            var last = reverse.sole("L");
            reverse.apply(sample(mode, 4, travel.y, travel.x, ProneCause.NONE), travel.cpy().scl(4 * BoardGeometry.HEIGHT));
            assertTrue(last.epsilonEquals(reverse.sole("L"), .0001f), "A paused/repeated pose cannot accumulate joint rotation");
            reverse.apply(UnitMotion.Sample.STILL, Vector3.Zero);
            for (String id : List.of("LL", "LL-shin", "LL-foot", "RL", "RL-shin", "RL-foot")) {
                assertTrue(reverse.instance.getNode(id).rotation.isIdentity(.0001f), "Stopping restores the authored reverse rest pose");
                assertTrue(reverse.model.instance.getNode(id).rotation.isIdentity(.0001f), "Posing cannot mutate the shared rest model");
            }
        }
    }

    @ParameterizedTest
    @CsvSource({ "MOVE_JUMP, NONE", "MOVE_WALK, VOLUNTARY" })
    void jumpAndCrouchKeepTheAuthoredBendAndLevelFeet(EntityMovementType mode, ProneCause posture) throws Exception {
        for (boolean reverse : List.of(false, true)) {
            try (var fixture = new Fixture(reverse)) {
                fixture.apply(sample(mode, 0, 1, 0, posture), Vector3.Zero);
                for (String side : List.of("L", "R")) {
                    fixture.assertKneeSide(side, Vector3.Y, reverse ? -1 : 1);
                    var up = new Vector3(Vector3.Z).rot(fixture.instance.getNode(side + "L-foot").globalTransform).nor();
                    assertTrue(up.epsilonEquals(Vector3.Z, .001f), "The foot counter-rotation must follow the selected knee branch");
                }
            }
        }
    }

    @ParameterizedTest
    @EnumSource(FallSide.class)
    void forcedProneRecoveryDoesNotInvertTheKnees(FallSide side) throws Exception {
        for (boolean reverse : List.of(false, true)) {
            try (var fixture = new Fixture(reverse)) {
                var standing = fixture.unit.location().withProneCause(ProneCause.NONE);
                var fallen = standing.withProneCause(ProneCause.FORCED).withFallSide(side);
                var motion = new UnitMotion(fallen);
                motion.append(List.of(fallen, standing), EntityMovementType.MOVE_WALK, 0);
                int risingSamples = 0;
                for (int frame = 0; frame <= 128; frame++) {
                    if (frame > 0) { motion.advance(UnitMotion.POSTURE_SECONDS / 128, 1); }
                    var sample = motion.sample();
                    if (sample.posture() != null && sample.posture().rising()) { risingSamples++; }
                    fixture.apply(sample, motion.position());
                    // Ignore the fall/roll orientation and placement; inspect the articulated body's own bend plane.
                    var rootCoordinates = fixture.instance.getNode("root").globalTransform.cpy().inv();
                    for (String leg : List.of("L", "R")) {
                        fixture.assertKneeSide(leg, Vector3.Y, reverse ? -1 : 1, rootCoordinates,
                              side + " recovery frame " + frame + " reverse=" + reverse);
                    }
                }
                assertTrue(risingSamples > 100, "The test must exercise the transition through its deep supported bend");
                assertTrue(!motion.isMoving(), "The final sample must reach the standing pose");
            }
        }
    }

    @Test
    void metadataDefaultsAndNamespacingKeepSemanticLegRoles() throws Exception {
        var legacy = new UnitRig(descriptor(null));
        assertTrue(legacy.legBends().isEmpty());
        assertEquals(1, legacy.kneeDirection("leftLeg"));
        var rig = new UnitRig(descriptor(Map.of("leftLeg", "reverse", "rightLeg", "forward")));
        var member = rig.inside("trooper-2", "member-2-");
        assertEquals(rig.legBends(), member.legBends());
        assertEquals("member-2-LL", member.joints().get("leftLeg"));
        assertEquals(-1, member.kneeDirection("leftLeg"));
        assertEquals(1, member.kneeDirection("rightLeg"));
        assertThrows(UnsupportedOperationException.class, () -> member.legBends().put("leftLeg", "forward"));
    }

    @ParameterizedTest
    @CsvSource({ "leftLeg, backward", "leftLeg, sideways", "leftShin, reverse", "tail, reverse", "CL, reverse" })
    void malformedBendMetadataIsRejectedAtTheDescriptorBoundary(String role, String direction) {
        assertThrows(JsonMappingException.class, () -> descriptor(Map.of(role, direction)));
    }

    private static UnitMotion.Sample sample(EntityMovementType type, float steps, float forward, float lateral, ProneCause prone) {
        return new UnitMotion.Sample(true, type, .5f, steps, 0, forward, 0, 0, prone,
              null, null, null, null, 1, null, null, lateral);
    }

    private static UnitModelDescriptor descriptor(Map<String, String> bends) throws Exception {
        var document = (ObjectNode) JSON.readTree("""
              {"schema":2,"kind":"body","family":"mek-biped","mesh":"synthetic.g3dj",
               "bounds":{"min":[-20,-15,0],"max":[20,15,48]},"rig":"biped-v1",
               "joints":{"root":"root","hips":"pelvis","torso":"CT",
                 "leftLeg":"LL","leftShin":"LL-shin","leftFoot":"LL-foot",
                 "rightLeg":"RL","rightShin":"RL-shin","rightFoot":"RL-foot"},
               "locations":{},"hardpoints":[],"emitters":[]}
              """);
        if (bends != null) { document.set("legBends", JSON.valueToTree(bends)); }
        return JSON.treeToValue(document, UnitModelDescriptor.class);
    }

    private static final class Fixture implements AutoCloseable {
        final GpuUnitModel model;
        final ModelInstance instance;
        final UnitAnimator animator = new UnitAnimator();
        final OrthographicCamera camera = new OrthographicCamera();
        final BoardScene.Unit unit;

        Fixture(boolean reverse) throws Exception {
            Model raw = new Model();
            Node root = node("root", 0, 0, 0);
            Node hips = node("pelvis", 0, 0, 29);
            root.addChild(hips);
            Node torso = node("CT", 0, 0, 10);
            bounds(torso, new Vector3(), new Vector3(8, 6, 8));
            hips.addChild(torso);
            for (int side : new int[] { -1, 1 }) {
                String leg = side < 0 ? "LL" : "RL";
                // Unequal segments and a lateral splay, matching the important King Crab joint proportions.
                Node hip = node(leg, side * 10.5f, 0, 0);
                Node knee = node(leg + "-shin", side * 3.5f, reverse ? -8 : 8, -10.5f);
                Node ankle = node(leg + "-foot", side, reverse ? 9 : -9, -13.5f);
                bounds(ankle, new Vector3(0, 0, -2.5f), new Vector3(3, 5, 2.5f));
                knee.addChild(ankle);
                hip.addChild(knee);
                hips.addChild(hip);
            }
            raw.nodes.add(root);
            raw.calculateTransforms();
            String bend = reverse ? "reverse" : "forward";
            var rig = new UnitRig(descriptor(Map.of("leftLeg", bend, "rightLeg", bend)));
            model = new GpuUnitModel(raw, "CT", true, List.of(), 1f / 27, new Vector3(40, 30, 48), List.of(rig));
            instance = new ModelInstance(raw);
            var structure = new UnitModelState.Structure(EntityMovementMode.BIPED, List.of(), List.of(), 0, false,
                  new UnitModelState.MekAnatomy("biped", List.of(), List.of()));
            var state = new UnitModelState(structure, new UnitModelState.Appearance(Set.of(), false, null),
                  new UnitModelState.Pose(ProneCause.NONE, 0, 0));
            var selection = new BoardScene.UnitModel("synthetic", "", "", 1, 0, BoardScene.LocationDamage.NONE, state);
            unit = new BoardScene.Unit(71, -1, "Leg bend fixture", new BoardScene.Waypoint(new Coords(0, 0), 0, 0),
                  null, false, null, 2, false, selection, 0);
        }

        void apply(UnitMotion.Sample motion, Vector3 ground) {
            animator.apply(model, instance, unit, motion, motion.steps(), 0, true, 0);
            model.place(instance, camera, ground, 0, unit);
        }

        Vector3 sole(String side) {
            return new Vector3(0, 0, -5).mul(instance.getNode(side + "L-foot").globalTransform).mul(instance.transform);
        }

        private Vector3 joint(String id, Matrix4 coordinates) {
            return instance.getNode(id).globalTransform.getTranslation(new Vector3()).mul(coordinates);
        }

        void assertKneeSide(String side, Vector3 bendPlane, int direction) {
            assertKneeSide(side, bendPlane, direction, instance.transform, "");
        }

        void assertKneeSide(String side, Vector3 bendPlane, int direction, Matrix4 coordinates, String context) {
            Vector3 hip = joint(side + "L", coordinates), knee = joint(side + "L-shin", coordinates),
                  ankle = joint(side + "L-foot", coordinates);
            Vector3 line = ankle.cpy().sub(hip);
            Vector3 projection = hip.cpy().mulAdd(line, knee.cpy().sub(hip).dot(line) / line.len2());
            assertTrue(knee.sub(projection).dot(bendPlane) * direction > .05f,
                  context + ": the " + side + " knee must remain on its authored side of the hip-to-ankle line");
        }

        @Override public void close() { model.dispose(); }
    }

    /** Imported-part bounds are sufficient for the real contact/settling code; the mesh never reaches a GPU. */
    private static void bounds(Node node, Vector3 center, Vector3 halfExtents) {
        MeshPart part = new MeshPart();
        part.mesh = mock(Mesh.class);
        part.size = 3;
        part.center.set(center);
        part.halfExtents.set(halfExtents);
        part.radius = halfExtents.len();
        node.parts.add(new NodePart(part, new Material("paint")));
    }

    private static Node node(String id, float x, float y, float z) {
        Node node = new Node();
        node.id = id;
        node.translation.set(x, y, z);
        return node;
    }
}

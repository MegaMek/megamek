/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import com.badlogic.gdx.math.Vector3;
import megamek.common.ResolvedAttack;
import megamek.common.board.Coords;
import megamek.common.units.*;
import megamek.common.util.SerializationHelper;
import org.junit.jupiter.api.Test;

class UnitPolishTest {
    @Test
    void damageThresholdsFollowRemainingArmorAndStructureAndNeverPersonnel() {
        var mek = new BipedMek();
        mek.initializeArmor(100, Mek.LOC_LEFT_ARM);
        mek.initializeInternal(10, Mek.LOC_LEFT_ARM);
        mek.setArmor(51, Mek.LOC_LEFT_ARM);
        assertNull(UnitModelSelection.damage(mek).stages().get("LA"));
        mek.setArmor(50, Mek.LOC_LEFT_ARM);
        assertEquals(UnitDamageDisplay.Stage.ARMOR_WORN, UnitModelSelection.damage(mek).stages().get("LA"));
        mek.setArmor(0, Mek.LOC_LEFT_ARM);
        assertEquals(UnitDamageDisplay.Stage.ARMOR_STRIPPED, UnitModelSelection.damage(mek).stages().get("LA"));
        mek.setInternal(5, Mek.LOC_LEFT_ARM);
        assertEquals(UnitDamageDisplay.Stage.STRUCTURE_BATTERED, UnitModelSelection.damage(mek).stages().get("LA"));
        mek.setInternal(megamek.common.equipment.IArmorState.ARMOR_DESTROYED, Mek.LOC_LEFT_ARM);
        assertTrue(UnitModelSelection.damage(mek).removed().contains("LA"));
        var tank = new Tank();
        for (int location = 0; location < tank.locations(); location++) {
            tank.initializeArmor(30, location);
            tank.initializeInternal(10, location);
        }
        for (int stage = 1; stage <= 4; stage++) {
            for (int location = 0; location < tank.locations(); location++) {
                tank.setArmor(Math.max(0, 30 - stage * 10), location);
                tank.setInternal(stage == 4 ? 0 : 10, location);
            }
            assertEquals(UnitDamageDisplay.Stage.values()[stage + 2], UnitModelSelection.damage(tank).stages().get("*"));
        }
        assertTrue(UnitModelSelection.damage(new megamek.common.battleArmor.BattleArmor()).isNone());
        assertTrue(UnitModelSelection.damage(new ConvInfantry()).isNone());
    }

    @Test
    void fallSideSurvivesSaveAndSparseMovementThenClearsOnStanding() {
        for (int direction = 0; direction < 6; direction++) {
            var mek = new BipedMek();
            mek.setProne(ProneCause.FORCED);
            mek.setFallSide(FallSide.fromDirection(direction));
            String xml = SerializationHelper.getSaveGameXStream().toXML(mek);
            var restored = (Entity) SerializationHelper.getLoadSaveGameXStream().fromXML(xml);
            assertEquals(mek.getFallSide(), restored.getFallSide());
            var start = new BoardScene.Waypoint(new Coords(2, 2), 0, direction, ProneCause.NONE);
            var down = start.withProneCause(ProneCause.FORCED).withFallSide(mek.getFallSide());
            var motion = new UnitMotion(start);
            motion.append(List.of(start, down, start), EntityMovementType.MOVE_WALK, 0);
            motion.advance(UnitMotion.POSTURE_SECONDS * 1.5, 1);
            assertTrue(motion.sample().posture().rising());
            assertEquals(mek.getFallSide(), motion.sample().posture().side());
            assertEquals(BoardGeometry.center(start.coords(), 0), motion.position());
            restored.setProne(false);
            assertNull(restored.getFallSide());
        }
    }

    @Test
    void punchesFinishApproachBeforeSwingAndReturnBeforeTheHold() {
        var a = UnitPlaybackTest.unit(1, 1);
        var b = UnitPlaybackTest.unit(2, 2);
        for (var kind : List.of(ResolvedAttack.Kind.PUNCH, ResolvedAttack.Kind.KICK)) {
            var attack = new UnitAttack(UnitPlaybackTest.attack(a, b, kind, true));
            attack.seconds = attack.approachSeconds * .5f;
            assertEquals(0, attack.strike());
            assertEquals(0, attack.windup());
            attack.seconds = attack.contactSeconds;
            assertEquals(1, attack.approachWeight());
            assertEquals(1, attack.contactWeight());
            attack.seconds = attack.contactSeconds + UnitAttack.RECOVERY_SECONDS + UnitAttack.MELEE_RUN_SECONDS * .5f;
            assertTrue(attack.returning());
            assertEquals(.5f, attack.approachWeight(), .00001f);
            assertEquals(0, attack.contactWeight());
            attack.seconds = attack.duration;
            assertEquals(0, attack.approachWeight(), .00001f);
        }
    }

    @Test
    void missesAreIrregularGroundContactsAndLasersOnlyStopAtLandscape() {
        var attack = new UnitAttack(UnitPlaybackTest.attack(UnitPlaybackTest.unit(1, 1), UnitPlaybackTest.unit(2, 3), ResolvedAttack.Kind.SHOT, false));
        var tiles = new ArrayList<BoardScene.Tile>();
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                tiles.add(new BoardScene.Tile(new Coords(x, y), x > 3 ? 2 : 0, -1, false, 0,
                      BoardScene.Surface.GRASS, null, null, null, List.of(), List.of()));
            }
        }
        var scene = new BoardScene(0, 8, 8, tiles, List.of(), List.of(), -1, "", List.of());
        attack.landscape = ray -> BoardGeometry.hit(scene, ray);
        var origin = BoardGeometry.center(new Coords(0, 1), 1);
        var radii = new HashSet<Integer>();
        for (int i = 0; i < 30; i++) {
            var point = attack.endpoint(null, origin, true, i, new Vector3());
            assertEquals(0, point.z, .001f);
            radii.add(Math.round(point.dst(BoardGeometry.center(new Coords(0, 3), 0))));
            assertEquals(point, attack.endpoint(null, origin, true, i, new Vector3()), "Stable through repaint/pause");
        }
        assertTrue(radii.size() > 15, "Misses must not form rings");
        attack.landscape = ray -> null;
        var end = new Vector3();
        assertFalse(attack.beamEndpoint(null, origin, 2, end));
        assertTrue(origin.dst(end) > BoardGeometry.HEIGHT * 100);
        attack.landscape = ray -> new BoardGeometry.Hit(new Coords(4, 4), 10000);
        assertTrue(attack.beamEndpoint(null, origin, 2, end));
        assertEquals(100, origin.dst(end), .001f);
    }
}

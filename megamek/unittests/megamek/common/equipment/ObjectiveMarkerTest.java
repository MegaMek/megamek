/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.List;

import megamek.common.Player;
import megamek.common.equipment.ObjectiveScoringScheme.HoldCounting;
import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.moves.MoveStep;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementType;
import megamek.common.units.Mek;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.api.Test;

class ObjectiveMarkerTest {

    @Test
    void testDefaults() {
        ObjectiveMarker marker = new ObjectiveMarker();

        assertEquals(0, marker.getControlRadius());
        assertEquals(1, marker.getVictoryPointValue());
        // RAW: objectives are destroyed with their building unless the mission states otherwise
        assertFalse(marker.isInvulnerable());
        assertFalse(marker.canBePickedUp(true));
        assertFalse(marker.canBePickedUp(false));
        assertFalse(marker.isDestroyed());
    }

    @Test
    void testControlRadiusValidation() {
        ObjectiveMarker marker = new ObjectiveMarker();

        marker.setControlRadius(0);
        marker.setControlRadius(ObjectiveMarker.MAX_CONTROL_RADIUS);
        assertEquals(ObjectiveMarker.MAX_CONTROL_RADIUS, marker.getControlRadius());

        assertThrows(IllegalArgumentException.class, () -> marker.setControlRadius(-1));
        assertThrows(IllegalArgumentException.class,
              () -> marker.setControlRadius(ObjectiveMarker.MAX_CONTROL_RADIUS + 1));
    }

    @Test
    void testOnlyIntactMobileObjectivesCanBePickedUp() {
        ObjectiveMarker mobileMarker = new ObjectiveMarker();
        mobileMarker.setMobile(true);
        assertTrue(mobileMarker.canBePickedUp(false));
        assertTrue(mobileMarker.canBePickedUp(true));

        mobileMarker.setDestroyed(true);
        assertFalse(mobileMarker.canBePickedUp(false));

        ObjectiveMarker staticMarker = new ObjectiveMarker();
        assertFalse(staticMarker.canBePickedUp(false));
    }

    @Test
    void testOnlyMeksCanExecuteThePickup() {
        ObjectiveMarker marker = new ObjectiveMarker();
        marker.setName("MacGuffin");
        marker.setMobile(true);
        TWGameManager gameManager = mock(TWGameManager.class);
        Game game = mock(Game.class);
        when(gameManager.getGame()).thenReturn(game);
        MoveStep step = mock(MoveStep.class);
        when(step.getPosition()).thenReturn(new Coords(1, 1));

        Entity nonMek = mock(Entity.class);
        marker.processPickupStep(step, null, gameManager, nonMek, EntityMovementType.MOVE_WALK);
        verify(nonMek, never()).pickupCarryableObject(marker, null);

        Mek mek = mock(Mek.class);
        when(mek.getDisplayName()).thenReturn("Test Mek");
        marker.processPickupStep(step, null, gameManager, mek, EntityMovementType.MOVE_WALK);
        verify(mek).pickupCarryableObject(marker, null);
    }

    @Test
    void testAnyDamageDestroys() {
        ObjectiveMarker marker = new ObjectiveMarker();

        assertTrue(marker.damage(0.5));
        assertTrue(marker.isDestroyed());
    }

    @Test
    void testSerializationRoundTrip() throws Exception {
        ObjectiveMarker marker = new ObjectiveMarker();
        marker.setName("Left Counter");
        marker.setOwnerId(3);
        marker.setControlRadius(2);
        marker.setVictoryPointValue(2);
        marker.setPotential(true);
        marker.setConfirmed(true);
        marker.setFalseObjective(true);
        marker.setFragile(true);
        marker.setMobile(true);
        marker.setDestroyed(true);
        marker.setInsideBuilding(true);
        marker.setBuildingLinkInitialized(true);
        marker.setDestructionProcessed(true);
        marker.setInvulnerable(true);
        marker.setController(4, ObjectiveMarker.NO_CONTROLLER);
        marker.setLobbyPosition(new Coords(7, 9));

        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteStream)) {
            objectOutputStream.writeObject(marker);
        }
        ObjectiveMarker restoredMarker;
        try (ObjectInputStream objectInputStream =
              new ObjectInputStream(new ByteArrayInputStream(byteStream.toByteArray()))) {
            restoredMarker = (ObjectiveMarker) objectInputStream.readObject();
        }

        assertEquals("Left Counter", restoredMarker.generalName());
        assertEquals(3, restoredMarker.getOwnerId());
        assertEquals(2, restoredMarker.getControlRadius());
        assertEquals(2, restoredMarker.getVictoryPointValue());
        assertTrue(restoredMarker.isPotential());
        assertTrue(restoredMarker.isConfirmed());
        assertTrue(restoredMarker.isFalseObjective());
        assertTrue(restoredMarker.isFragile());
        assertTrue(restoredMarker.isMobile());
        assertTrue(restoredMarker.isDestroyed());
        assertTrue(restoredMarker.isInvulnerable());
        assertTrue(restoredMarker.isInsideBuilding());
        assertTrue(restoredMarker.isBuildingLinkInitialized());
        assertTrue(restoredMarker.isDestructionProcessed());
        assertEquals(4, restoredMarker.getControllingTeam());
        assertEquals(ObjectiveMarker.NO_CONTROLLER, restoredMarker.getControllingPlayerId());
        assertEquals(new Coords(7, 9), restoredMarker.getLobbyPosition());
    }

    @Test
    void testDesignationVisibility() {
        Player owner = new Player(0, "Alice");
        owner.setTeam(1);
        Player teammate = new Player(1, "Bob");
        teammate.setTeam(1);
        Player enemy = new Player(2, "Craig");
        enemy.setTeam(2);
        Player unteamed = new Player(3, "Dana");
        unteamed.setTeam(Player.TEAM_NONE);
        Player gameMaster = new Player(4, "Erin");
        gameMaster.setTeam(2);
        gameMaster.setGameMaster(true);

        // the owner and their teammates see the designation, an enemy team does not
        assertTrue(ObjectiveMarker.isDesignationVisibleTo(owner, owner));
        assertTrue(ObjectiveMarker.isDesignationVisibleTo(owner, teammate));
        assertFalse(ObjectiveMarker.isDesignationVisibleTo(owner, enemy));
        // an unteamed player is their own side: sees only their own
        assertFalse(ObjectiveMarker.isDesignationVisibleTo(owner, unteamed));
        assertTrue(ObjectiveMarker.isDesignationVisibleTo(unteamed, unteamed));
        assertFalse(ObjectiveMarker.isDesignationVisibleTo(unteamed, owner));
        // a game master sees every side
        assertTrue(ObjectiveMarker.isDesignationVisibleTo(owner, gameMaster));
        assertTrue(ObjectiveMarker.isDesignationVisibleTo(unteamed, gameMaster));
    }

    @Test
    void testClaimDesignationsForcesTheListOwner() {
        // the server claims received markers for the player whose list carried them, so a modified
        // client cannot spoof another player as the owner of a designation
        ObjectiveMarker spoofedMarker = new ObjectiveMarker();
        spoofedMarker.setOwnerId(7);
        ICarryable briefcase = mock(ICarryable.class);

        ObjectiveMarker.claimDesignations(List.of(spoofedMarker, briefcase), 2);

        assertEquals(2, spoofedMarker.getOwnerId());
        verify(briefcase, never()).setOwnerId(2);
    }

    @Test
    void testScoringSchemeSurvivesSerialization() throws Exception {
        // the scheme rides the marker over the wire and into save games - its setup AND its counter state
        // must survive a serialization round trip
        ObjectiveMarker marker = new ObjectiveMarker();
        marker.setName("Hold Point");
        ObjectiveScoringScheme scheme = ObjectiveScoringScheme.hold(5, HoldCounting.CUMULATIVE);
        scheme.setHeldTurns(2, ObjectiveScoringScheme.NO_SIDE, 3);
        scheme.setSecuredBy(ObjectiveScoringScheme.NO_SIDE, ObjectiveScoringScheme.NO_SIDE);
        marker.setScoringScheme(scheme);

        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try (ObjectOutputStream objectOutput = new ObjectOutputStream(byteStream)) {
            objectOutput.writeObject(marker);
        }
        ObjectiveMarker restored;
        try (ObjectInputStream objectInput = new ObjectInputStream(
              new ByteArrayInputStream(byteStream.toByteArray()))) {
            restored = (ObjectiveMarker) objectInput.readObject();
        }

        ObjectiveScoringScheme restoredScheme = restored.getScoringScheme();
        assertEquals(ObjectiveScoringScheme.SchemePreset.HOLD, restoredScheme.getPreset());
        assertEquals(5, restoredScheme.getThreshold());
        assertEquals(HoldCounting.CUMULATIVE, restoredScheme.getHoldCounting());
        assertEquals(3, restoredScheme.getHeldTurns(2, ObjectiveScoringScheme.NO_SIDE));
        assertFalse(restoredScheme.isDecided());
    }

    // --- the board's compact progress reading (shown under the flag) ---

    @Test
    void testStandardAndRaidHaveNoProgressToShow() {
        assertNull(ObjectiveScoringScheme.standard().progressLabel(),
              "the printed per-turn scheme has no running counter");
        assertNull(ObjectiveScoringScheme.raid().progressLabel(),
              "raid scores once at the end and counts nothing on the way");
    }

    @Test
    void testHoldShowsTurnsHeldAgainstTurnsNeeded() {
        ObjectiveScoringScheme scheme = ObjectiveScoringScheme.hold(3,
              ObjectiveScoringScheme.HoldCounting.CONSECUTIVE);
        assertEquals("0/3", scheme.progressLabel());
        scheme.setHeldTurns(1, ObjectiveScoringScheme.NO_SIDE, 2);
        assertEquals("2/3", scheme.progressLabel());
    }

    @Test
    void testDefendShowsTheGripThatIsLeft() {
        ObjectiveScoringScheme scheme = ObjectiveScoringScheme.defend(4, 1);
        assertEquals("4/4", scheme.progressLabel());
        scheme.setDefendGrip(1);
        assertEquals("1/4", scheme.progressLabel());
    }

    @Test
    void testCaptureShowsTheMeter() {
        ObjectiveScoringScheme scheme = ObjectiveScoringScheme.capture(2, 1);
        assertEquals("0/2", scheme.progressLabel());
        scheme.setCaptureProgress(1, ObjectiveScoringScheme.NO_SIDE, 1);
        assertEquals("1/2", scheme.progressLabel());
    }

    @Test
    void testADecidedPointStopsShowingProgress() {
        ObjectiveScoringScheme scheme = ObjectiveScoringScheme.hold(2,
              ObjectiveScoringScheme.HoldCounting.CONSECUTIVE);
        scheme.setHeldTurns(1, ObjectiveScoringScheme.NO_SIDE, 2);
        scheme.setSecuredBy(1, ObjectiveScoringScheme.NO_SIDE);
        assertNull(scheme.progressLabel(), "a settled point has no progress left to make");
    }

    // --- how far along a point is, for the board's tint ---

    @Test
    void testHoldFractionGrowsWithTurnsHeldAndTheHolderLeads() {
        ObjectiveScoringScheme scheme = ObjectiveScoringScheme.hold(10,
              ObjectiveScoringScheme.HoldCounting.CONSECUTIVE);
        assertEquals(0.0, scheme.progressFraction(), 0.001);
        scheme.setHeldTurns(2, ObjectiveScoringScheme.NO_SIDE, 1);
        assertEquals(0.1, scheme.progressFraction(), 0.001, "one turn of ten is a tenth of the way");
        scheme.setHeldTurns(2, ObjectiveScoringScheme.NO_SIDE, 9);
        assertEquals(0.9, scheme.progressFraction(), 0.001);
        assertEquals(new ObjectiveScoringScheme.CountedSide(2, ObjectiveScoringScheme.NO_SIDE),
              scheme.leadingSide());
    }

    @Test
    void testDefendFractionIsHowMuchGripHasDrained() {
        ObjectiveScoringScheme scheme = ObjectiveScoringScheme.defend(4, 1);
        assertEquals(0.0, scheme.progressFraction(), 0.001, "full grip, nothing drained");
        scheme.setDefendGrip(1);
        assertEquals(0.75, scheme.progressFraction(), 0.001, "three of four drained");
    }

    @Test
    void testStandardIsAlwaysFullyWhateverItIs() {
        assertEquals(1.0, ObjectiveScoringScheme.standard().progressFraction(), 0.001);
        assertNull(ObjectiveScoringScheme.standard().leadingSide());
    }

    @Test
    void testADecidedPointIsFullyDecided() {
        ObjectiveScoringScheme scheme = ObjectiveScoringScheme.hold(3,
              ObjectiveScoringScheme.HoldCounting.CONSECUTIVE);
        scheme.setSecuredBy(1, ObjectiveScoringScheme.NO_SIDE);
        assertEquals(1.0, scheme.progressFraction(), 0.001);
    }
}

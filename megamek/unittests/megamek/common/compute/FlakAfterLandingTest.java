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
package megamek.common.compute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.GameBoardTestCase;
import megamek.common.board.Coords;
import megamek.common.enums.MoveStepType;
import megamek.common.moves.MovePath;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.EntityMovementType;
import megamek.common.units.LandAirMek;
import megamek.common.units.SupportTank;
import megamek.common.units.VTOL;
import megamek.testUtilities.MMTestUtilities;
import org.junit.jupiter.api.Test;

/**
 * Issue #9105: flak lost its -2 to-hit bonus against a VTOL or WiGE that flew and then landed. TW p.114 (errata v12.0):
 * flak applies "against a unit that presently has an Altitude or Elevation, or that expended any VTOL or WiGE MP or
 * Thrust Points that turn (even if it landed at the end of that Movement Phase)".
 *
 * <p>Each landing is a real move path; the unit is then left where the server would leave it at the end of the
 * movement phase (final hex, final elevation, and {@code moved} set to the path's last step type).</p>
 */
class FlakAfterLandingTest extends GameBoardTestCase {

    private static final int ATTACKER_ID = 1;

    static {
        initializeBoard("FLAT_FOUR_HEXES", """
              size 1 4
              hex 0101 0 "" ""
              hex 0102 0 "" ""
              hex 0103 0 "" ""
              hex 0104 0 "" ""
              end""");
    }

    private Entity attacker() {
        BipedMek attackingMek = new BipedMek();
        attackingMek.setId(ATTACKER_ID);
        getGame().addEntity(attackingMek);
        attackingMek.setPosition(new Coords(0, 3));
        attackingMek.setDeployed(true);
        return attackingMek;
    }

    /** Leaves the unit where the server puts it at the end of the movement phase. */
    private Entity endMovement(MovePath path) {
        assertTrue(path.isMoveLegal(), "the test move should be legal: " + path);
        Entity movingUnit = path.getEntity();
        movingUnit.setPosition(path.getFinalCoords());
        movingUnit.setElevation(path.getFinalElevation());
        movingUnit.moved = path.getLastStepMovementType();
        movingUnit.setDeployed(true);
        return movingUnit;
    }

    private LandAirMek airMek() {
        LandAirMek lam = (LandAirMek) MMTestUtilities.getEntityForUnitTesting("Shadow Hawk LAM SHD-X2", false);
        assertNotNull(lam, "the Shadow Hawk LAM test unit should load");
        lam.setConversionMode(LandAirMek.CONV_MODE_AIR_MEK);
        return lam;
    }

    @Test
    void wiGEThatFlewAndLandedIsAFlakTarget() {
        setBoard("FLAT_FOUR_HEXES");
        Entity wige = endMovement(getMovePathFor(new SupportTank(), 1, EntityMovementMode.WIGE,
              MoveStepType.FORWARDS, MoveStepType.FORWARDS, MoveStepType.DOWN));

        assertEquals(0, wige.getElevation(), "the WiGE should have landed");
        assertFalse(wige.isAirborneVTOLorWIGE(), "a landed WiGE is not airborne");
        assertTrue(Compute.isFlakToHitTarget(attacker(), wige),
              "a WiGE that spent WiGE MP this turn keeps the flak bonus after landing");
    }

    @Test
    void artilleryFlakStillTreatsALandedWiGEAsAGroundTarget() {
        setBoard("FLAT_FOUR_HEXES");
        Entity wige = endMovement(getMovePathFor(new SupportTank(), 1, EntityMovementMode.WIGE,
              MoveStepType.FORWARDS, MoveStepType.FORWARDS, MoveStepType.DOWN));

        assertFalse(Compute.isFlakAttack(attacker(), wige),
              "artillery bursts at the target's height, so a landed WiGE stays a ground target for artillery");
    }

    @Test
    void vtolThatFlewAndLandedIsAFlakTarget() {
        setBoard("FLAT_FOUR_HEXES");
        Entity vtol = endMovement(getMovePathFor(new VTOL(), 1, EntityMovementMode.VTOL,
              MoveStepType.FORWARDS, MoveStepType.FORWARDS, MoveStepType.DOWN));

        assertEquals(0, vtol.getElevation(), "the VTOL should have landed");
        assertTrue(Compute.isFlakToHitTarget(attacker(), vtol),
              "a VTOL that spent VTOL MP this turn keeps the flak bonus after landing");
    }

    @Test
    void airMekThatFlewAndLandedIsAFlakTarget() {
        setBoard("FLAT_FOUR_HEXES");
        Entity lam = endMovement(getMovePathFor(airMek(), 1, EntityMovementMode.WIGE,
              MoveStepType.FORWARDS, MoveStepType.FORWARDS, MoveStepType.DOWN));

        assertEquals(0, lam.getElevation(), "the AirMek should have landed");
        assertTrue(Compute.isFlakToHitTarget(attacker(), lam),
              "an AirMek flies on WiGE MP, so it keeps the flak bonus after landing");
    }

    @Test
    void wiGEThatOnlyMovedOnTheGroundIsNotAFlakTarget() {
        setBoard("FLAT_FOUR_HEXES");
        Entity wige = endMovement(getMovePathFor(new SupportTank(), 0, EntityMovementMode.WIGE,
              MoveStepType.FORWARDS));

        assertEquals(EntityMovementType.MOVE_WALK, wige.moved, "a WiGE moving on the ground does not fly");
        assertFalse(Compute.isFlakToHitTarget(attacker(), wige),
              "a WiGE that never left the ground spent no WiGE flight MP");
    }

    @Test
    void landedVtolThatDidNotMoveIsNotAFlakTarget() {
        setBoard("FLAT_FOUR_HEXES");
        Entity vtol = endMovement(getMovePathFor(new VTOL(), 0, EntityMovementMode.VTOL));

        assertEquals(EntityMovementType.MOVE_NONE, vtol.moved);
        assertFalse(Compute.isFlakToHitTarget(attacker(), vtol),
              "a VTOL that stayed landed all turn is a ground target");
    }

    /** Ruling (forum, 2026-01-23): a Mek making a death from above attack is not a flak target. */
    @Test
    void mekThatJumpedIsNotAFlakTarget() {
        setBoard("FLAT_FOUR_HEXES");
        Entity mek = endMovement(getMovePathFor(new BipedMek(), 0, EntityMovementMode.BIPED));
        // The test Mek has no jump jets, so set the jump directly as the server would record it
        mek.moved = EntityMovementType.MOVE_JUMP;

        assertFalse(Compute.isFlakToHitTarget(attacker(), mek), "jump MP are not VTOL or WiGE MP");
    }

    /** Positive control: an airborne VTOL was always a flak target. */
    @Test
    void airborneVtolIsStillAFlakTarget() {
        setBoard("FLAT_FOUR_HEXES");
        Entity vtol = endMovement(getMovePathFor(new VTOL(), 1, EntityMovementMode.VTOL,
              MoveStepType.FORWARDS));

        Entity attackingMek = attacker();

        assertTrue(vtol.isAirborneVTOLorWIGE(), "the VTOL should still be flying");
        assertTrue(Compute.isFlakToHitTarget(attackingMek, vtol));
        assertTrue(Compute.isFlakAttack(attackingMek, vtol));
    }
}

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
package megamek.common.moves;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import megamek.common.GameBoardTestCase;
import megamek.common.Player;
import megamek.common.bays.MekBay;
import megamek.common.board.Coords;
import megamek.common.enums.BuildingType;
import megamek.common.enums.MoveStepType;
import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.BipedMek;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.units.MobileStructure;
import megamek.common.units.SmallCraft;
import megamek.common.units.VTOL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Clicking a friendly DropShip should plot a path that stops beside it, so the unit can mount. The Mek starts at
 * (0, 0) facing south and walks down a one-hex-wide column; the grounded DropShip is centred at (0, 5) facing north,
 * so its footprint covers (0, 4) and (0, 5) in that column.
 */
class MountPathHelperTest extends GameBoardTestCase {

    private static final Coords DROPSHIP_CENTRE = new Coords(0, 5);
    private static final Coords HEX_BESIDE_DROPSHIP = new Coords(0, 3);
    private static final Coords EMPTY_HEX = new Coords(0, 2);

    static {
        initializeBoard("COLUMN_BOARD", """
              size 1 7
              hex 0101 0 "" ""
              hex 0102 0 "" ""
              hex 0103 0 "" ""
              hex 0104 0 "" ""
              hex 0105 0 "" ""
              hex 0106 0 "" ""
              hex 0107 0 "" ""
              end""");
    }

    @Test
    @DisplayName("Clicking a friendly DropShip trims the path to the hex beside it, and the mount then survives")
    void clickingDropShipStopsBesideItAndMounts() {
        Dropship dropShip = placeDropShip(true);
        MovePath movePath = walkSouth(5);

        Entity transport = MountPathHelper.trimToMountableTransport(movePath, DROPSHIP_CENTRE, 0, getGame());

        assertSame(dropShip, transport, "The clicked DropShip should be returned as the transport to mount");
        assertEquals(3, movePath.length(), "The two steps inside the DropShip should be removed");
        assertEquals(HEX_BESIDE_DROPSHIP, movePath.getFinalCoords(), "The path should stop beside the DropShip");

        movePath.addStep(MoveStepType.MOUNT, transport);
        assertEquals(4, movePath.getLastStep().getMp(), "Boarding costs half the Mek's standard eight Walking MP");
        assertEquals(7, movePath.getMpUsed(), "The path includes both the three-hex approach and boarding");
        movePath.clipToPossible();
        assertEquals(4, movePath.length(), "The walk and the mount step should all survive clipping");
    }

    @Test
    @DisplayName("A dismounting unit keeps the facing it chose; with no valid choice it faces away from the carrier")
    void dismountFacingUsesTheChosenFacing() {
        Coords carrierHex = new Coords(0, 5);
        Coords hexNorthOfCarrier = new Coords(0, 4);
        int north = 0;
        int southWest = 4;

        assertEquals(southWest, MountPathHelper.dismountFacing(carrierHex, hexNorthOfCarrier, southWest),
              "The facing the player chose is used (TW p.91)");
        assertEquals(north, MountPathHelper.dismountFacing(carrierHex, hexNorthOfCarrier, null),
              "With no choice the unit faces away from the carrier, as before");
        assertEquals(north, MountPathHelper.dismountFacing(carrierHex, hexNorthOfCarrier, 6),
              "A facing outside 0-5 is ignored");
    }

    @Test
    @DisplayName("Clicking a DropShip with no room leaves the path unchanged")
    void clickingFullDropShipLeavesPathUnchanged() {
        placeDropShip(false);
        MovePath movePath = walkSouth(5);

        Entity transport = MountPathHelper.trimToMountableTransport(movePath, DROPSHIP_CENTRE, 0, getGame());

        assertNull(transport, "A DropShip that cannot load the Mek is not a mount target");
        assertEquals(5, movePath.length(), "The path should not be trimmed");
    }

    @Test
    @DisplayName("Clicking an empty hex leaves the path unchanged")
    void clickingEmptyHexLeavesPathUnchanged() {
        placeDropShip(true);
        MovePath movePath = walkSouth(2);

        Entity transport = MountPathHelper.trimToMountableTransport(movePath, EMPTY_HEX, 0, getGame());

        assertNull(transport, "No transport stands in an empty hex");
        assertEquals(2, movePath.length(), "The path should not be trimmed");
    }

    @Test
    @DisplayName("Mounting and dismounting cost half Walking MP, rounding the cost up (TW p.90-91)")
    void mountOrDismountCostRoundsUp() {
        assertEquals(0, MountPathHelper.mountOrDismountMpCost(0), "Walk 0 costs nothing");
        assertEquals(1, MountPathHelper.mountOrDismountMpCost(1), "Walk 1 rounds half up to 1");
        assertEquals(2, MountPathHelper.mountOrDismountMpCost(4), "Walk 4 costs exactly half");
        assertEquals(3, MountPathHelper.mountOrDismountMpCost(5), "Walk 5 rounds 2.5 up to 3, not down to 2");
        assertEquals(4, MountPathHelper.mountOrDismountMpCost(7), "Walk 7 rounds 3.5 up to 4");
    }

    @Test
    @DisplayName("A Mek may mount while its spent MP plus the mounting cost fits in its Walking MP (TW p.90)")
    void mekMountsWithinWalkingMp() {
        BipedMek mek = new BipedMek();
        Dropship carrier = new Dropship();
        mek.setOriginalWalkMP(4);

        assertEquals(MountPathHelper.MountRestriction.NONE, MountPathHelper.mountRestriction(mek, carrier, 4, 2, false),
              "Walk 4: 2 MP spent + 2 to mount = 4, which fits");
        assertEquals(MountPathHelper.MountRestriction.NOT_ENOUGH_WALKING_MP,
              MountPathHelper.mountRestriction(mek, carrier, 4, 3, false), "Walk 4: 3 MP spent + 2 to mount = 5, too many");
        mek.setOriginalWalkMP(5);
        assertEquals(MountPathHelper.MountRestriction.NONE, MountPathHelper.mountRestriction(mek, carrier, 5, 2, false),
              "Walk 5: 2 MP spent + 3 to mount = 5, which fits");
        assertEquals(MountPathHelper.MountRestriction.NOT_ENOUGH_WALKING_MP,
              MountPathHelper.mountRestriction(mek, carrier, 5, 3, false),
              "Walk 5: 3 MP spent + 3 to mount = 6; rounding the cost down would wrongly allow this");
    }

    @Test
    @DisplayName("A unit that has not moved may always mount, through the Minimum Movement rule (TW p.90, p.49)")
    void unmovedUnitMountsThroughMinimumMovement() {
        BipedMek mek = new BipedMek();
        Dropship carrier = new Dropship();
        mek.setOriginalWalkMP(5);

        assertEquals(MountPathHelper.MountRestriction.NONE, MountPathHelper.mountRestriction(mek, carrier, 1, 0, false),
              "A unit starting beside the carrier can pay three standard MP even with only one current MP");
        assertEquals(MountPathHelper.MountRestriction.NONE, MountPathHelper.mountRestriction(mek, carrier, 0, 0, false),
              "A unit with no Walking MP left may still mount from where it started");
        assertEquals(MountPathHelper.MountRestriction.NOT_ENOUGH_WALKING_MP,
              MountPathHelper.mountRestriction(mek, carrier, 1, 1, false), "Walk 1 that already moved has nothing left");
    }

    @Test
    @DisplayName("VTOLs, fighters and small craft cannot mount under their own power (TW p.90)")
    void craneOnlyUnitsCannotMountUnderOwnPower() {
        Dropship carrier = new Dropship();
        assertEquals(MountPathHelper.MountRestriction.CRANE_ONLY,
              MountPathHelper.mountRestriction(new VTOL(), carrier, 6, 0, false), "A VTOL must be loaded by crane");
        assertEquals(MountPathHelper.MountRestriction.CRANE_ONLY,
              MountPathHelper.mountRestriction(new AeroSpaceFighter(), carrier, 5, 0, false),
              "A grounded fighter must be loaded by crane");
        assertEquals(MountPathHelper.MountRestriction.CRANE_ONLY,
              MountPathHelper.mountRestriction(new SmallCraft(), carrier, 3, 0, false),
              "A grounded small craft must be loaded by crane");
        MobileStructure mobile = new MobileStructure(BuildingType.HEAVY, IBuilding.FORTRESS);
        assertEquals(MountPathHelper.MountRestriction.CRANE_ONLY,
              MountPathHelper.mountRestriction(new VTOL(), mobile, 6, 0, false),
              "A building target alone does not replace the required flight deck transfer");
    }

    @Test
    @DisplayName("No unit may jump and then mount (TW p.90)")
    void jumpingUnitCannotMount() {
        Dropship carrier = new Dropship();
        assertEquals(MountPathHelper.MountRestriction.JUMPED,
              MountPathHelper.mountRestriction(new BipedMek(), carrier, 5, 1, true), "A Mek that jumped cannot mount");
        assertEquals(MountPathHelper.MountRestriction.JUMPED,
              MountPathHelper.mountRestriction(new ConvInfantry(), carrier, 1, 0, true),
              "Jump infantry that jumped cannot mount");
    }

    @Test
    @DisplayName("Infantry must spend all their MP to mount, so they may not move first (TW p.223)")
    void infantryMountOnlyWithoutMoving() {
        ConvInfantry infantry = new ConvInfantry();
        Dropship carrier = new Dropship();

        assertEquals(MountPathHelper.MountRestriction.NONE,
              MountPathHelper.mountRestriction(infantry, carrier, 1, 0, false),
              "Infantry that has not moved may mount");
        assertEquals(MountPathHelper.MountRestriction.INFANTRY_ALREADY_MOVED,
              MountPathHelper.mountRestriction(infantry, carrier, 3, 1, false),
              "Infantry that moved even one MP may not mount, however much MP it has left");
    }

    @Test
    void buildingBaysUseTheSameInfantryBoardingRulesAsGroundedDropships() {
        MobileStructure carrier = new MobileStructure(BuildingType.HEAVY, IBuilding.FORTRESS);
        ConvInfantry infantry = new ConvInfantry();
        infantry.setOriginalWalkMP(3);

        assertEquals(MountPathHelper.MountRestriction.NONE,
              MountPathHelper.mountRestriction(infantry, carrier, 3, 0, false));
        assertEquals(3, MountPathHelper.mountMpCost(infantry, carrier), "Infantry spends all its movement boarding");
        assertEquals(MountPathHelper.MountRestriction.INFANTRY_ALREADY_MOVED,
              MountPathHelper.mountRestriction(infantry, carrier, 3, 1, false));
        assertEquals(MountPathHelper.MountRestriction.JUMPED,
              MountPathHelper.mountRestriction(infantry, carrier, 3, 1, true));
    }

    private Dropship placeDropShip(boolean hasMekBay) {
        setBoard("COLUMN_BOARD");
        Player player = new Player(0, "Player");
        getGame().addPlayer(player.getId(), player);

        Dropship dropShip = new Dropship();
        dropShip.setId(20);
        dropShip.setOwner(player);
        if (hasMekBay) {
            dropShip.addTransporter(new MekBay(2, 1, 1));
        }
        getGame().addEntity(dropShip);
        dropShip.setDeployed(true);
        dropShip.setFacing(0);
        dropShip.setAltitude(0);
        dropShip.setPosition(DROPSHIP_CENTRE);
        return dropShip;
    }

    private MovePath walkSouth(int hexes) {
        BipedMek mek = new BipedMek();
        mek.setOwner(getGame().getPlayer(0));
        mek.setDeployed(true);
        MoveStepType[] steps = new MoveStepType[hexes];
        for (int i = 0; i < hexes; i++) {
            steps[i] = MoveStepType.FORWARDS;
        }
        return getMovePathFor(mek, steps);
    }
}

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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.GameBoardTestCase;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.units.BipedMek;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.ProtoMek;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The movement display names the Go Down button "Enter Building" when going down takes the unit from a building's
 * roof into the building. Only infantry, battle armour included, and ProtoMeks may change levels in a building (TW p.
 * 169).
 */
@DisplayName("Going down from a building roof enters the building")
class BuildingRoofEntryTest extends GameBoardTestCase {

    private static final Coords BUILDING_HEX = new Coords(0, 0);
    private static final Coords OPEN_HEX = new Coords(0, 1);
    private static final int ROOF = 2;

    static {
        initializeBoard("BOARD_ROOF_ENTRY", """
              size 1 2
              hex 0101 0 "bldg_elev:2;building:2;bldg_cf:100" ""
              hex 0102 0 "" ""
              end""");
    }

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() {
        setBoard("BOARD_ROOF_ENTRY");
    }

    private <T extends Entity> T place(T entity, EntityMovementMode mode, Coords position, int elevation) {
        entity.setGame(getGame());
        entity.setMovementMode(mode);
        entity.setId(getGame().getNextEntityId());
        getGame().addEntity(entity);
        entity.setPosition(position);
        entity.setElevation(elevation);
        return entity;
    }

    private static boolean isEnteringFromRoof(Entity entity) {
        return BuildingRoofEntry.isEnteringFromRoof(entity, entity.getElevation(), entity.getPosition(), 0);
    }

    @Test
    @DisplayName("A jump platoon on the roof enters the building by going down")
    void platoonOnTheRoof() {
        ConvInfantry platoon = place(new ConvInfantry(), EntityMovementMode.INF_JUMP, BUILDING_HEX, ROOF);

        assertTrue(isEnteringFromRoof(platoon));
    }

    @Test
    @DisplayName("Battle armour on the roof enters the building by going down")
    void battleArmourOnTheRoof() {
        BattleArmor battleArmour = place(new BattleArmor(), EntityMovementMode.INF_JUMP, BUILDING_HEX, ROOF);

        assertTrue(isEnteringFromRoof(battleArmour));
    }

    @Test
    @DisplayName("A platoon already inside only goes down a level; the button keeps its usual name")
    void platoonAlreadyInside() {
        ConvInfantry platoon = place(new ConvInfantry(), EntityMovementMode.INF_JUMP, BUILDING_HEX, ROOF - 1);

        assertFalse(isEnteringFromRoof(platoon));
    }

    @Test
    @DisplayName("A Mek on the roof cannot change levels in a building (TW p. 169)")
    void mekOnTheRoof() {
        BipedMek mek = place(new BipedMek(), EntityMovementMode.BIPED, BUILDING_HEX, ROOF);

        assertFalse(isEnteringFromRoof(mek));
    }

    @Test
    @DisplayName("A platoon in the open has no building to enter")
    void platoonInTheOpen() {
        ConvInfantry platoon = place(new ConvInfantry(), EntityMovementMode.INF_JUMP, OPEN_HEX, 0);

        assertFalse(isEnteringFromRoof(platoon));
    }

    @Test
    @DisplayName("A ProtoMek on the roof enters the building by going down (TW p. 169)")
    void protoMekOnTheRoof() {
        ProtoMek protoMek = place(new ProtoMek(), EntityMovementMode.BIPED, BUILDING_HEX, ROOF);

        assertTrue(isEnteringFromRoof(protoMek));
    }
}

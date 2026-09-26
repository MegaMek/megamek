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
package megamek.common.weapons.handlers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.Hex;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.LandAirMek;
import megamek.common.units.Tank;
import megamek.common.units.VTOL;
import megamek.testUtilities.MMTestUtilities;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for issue #9047: artillery flak did no damage to a LAM in AirMek mode.
 *
 * <p>Flak hits "airborne ground units (VTOL Vehicles, WiGEs and units expending VTOL MPs such as infantry) as well as
 * airborne aerospace units" (TO:AR p.153). An AirMek flies as a WiGE, but the damage step only let VTOLs and aerospace
 * units through.</p>
 */
class FlakTargetTest {

    private static final int BOARD_WIDTH = 16;
    private static final int BOARD_HEIGHT = 17;

    private Game game;
    private int nextId = 1;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        game = new Game();
        Hex[] hexes = new Hex[BOARD_WIDTH * BOARD_HEIGHT];
        for (int index = 0; index < hexes.length; index++) {
            hexes[index] = new Hex();
        }
        game.setBoard(new Board(BOARD_WIDTH, BOARD_HEIGHT, hexes));
    }

    private <T extends Entity> T place(T entity, int elevation) {
        entity.setId(nextId++);
        game.addEntity(entity, false);
        entity.setPosition(new Coords(5, 5));
        entity.setElevation(elevation);
        entity.setDeployed(true);
        return entity;
    }

    private LandAirMek airMek(int elevation) {
        LandAirMek lam = (LandAirMek) MMTestUtilities.getEntityForUnitTesting("Shadow Hawk LAM SHD-X2", false);
        assertNotNull(lam, "the Shadow Hawk LAM test unit should load");
        lam.setConversionMode(LandAirMek.CONV_MODE_AIR_MEK);
        return place(lam, elevation);
    }

    @Test
    void airborneAirMekTakesFlak() {
        assertTrue(AreaEffectHelper.isFlakTarget(airMek(3)));
    }

    @Test
    void airborneWiGEVehicleTakesFlak() {
        Tank wige = new Tank();
        wige.setMovementMode(EntityMovementMode.WIGE);

        assertTrue(AreaEffectHelper.isFlakTarget(place(wige, 2)));
    }

    @Test
    void landedAirMekDoesNotTakeFlak() {
        assertFalse(AreaEffectHelper.isFlakTarget(airMek(0)));
    }

    @Test
    void groundMekDoesNotTakeFlak() {
        assertFalse(AreaEffectHelper.isFlakTarget(place(new BipedMek(), 0)));
    }

    /** The positive control: VTOLs always could be hit by flak. */
    @Test
    void airborneVtolStillTakesFlak() {
        VTOL vtol = new VTOL();
        vtol.setMovementMode(EntityMovementMode.VTOL);

        assertTrue(AreaEffectHelper.isFlakTarget(place(vtol, 3)));
    }
}

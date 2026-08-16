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

package megamek.client.ui.clientGUI.tooltip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import megamek.common.Player;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Tank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers the tooltip's carried-ammo section.
 *
 * <p>The per-weapon ammo lines only run for ammo that feeds a weapon on the same unit. An ammunition carriage such
 * as the one in a Mobile Long Tom battery carries nothing but ammo bins and hitches, so its tooltip used to show no
 * ammunition at all.</p>
 */
class CarriedAmmoToolTipTest {

    private static final String LONG_TOM_AMMO = "ISLongTomAmmo";
    private static final String LONG_TOM_CANNON = "ISLongTom";
    private static final double CARRIAGE_TONS = 10.0;

    private Game game;
    private Player owner;
    private int nextId = 1;

    @BeforeEach
    void setUp() {
        EquipmentType.initializeTypes();
        owner = new Player(0, "Owner");
        owner.setTeam(1);
        game = new Game();
        game.addPlayer(0, owner);
    }

    private Tank buildVehicle() {
        Tank vehicle = new Tank();
        vehicle.setId(nextId++);
        vehicle.setOwner(owner);
        vehicle.setWeight(CARRIAGE_TONS);
        vehicle.setMovementMode(EntityMovementMode.TRACKED);
        game.addEntity(vehicle);
        return vehicle;
    }

    @Test
    void anAmmoCarriageListsTheAmmoItCarries() throws Exception {
        Tank carriage = buildVehicle();
        carriage.addEquipment(EquipmentType.get(LONG_TOM_AMMO), Tank.LOC_BODY);
        carriage.addEquipment(EquipmentType.get(LONG_TOM_AMMO), Tank.LOC_BODY);

        Map<String, Integer> carried = UnitToolTip.carriedAmmoShots(carriage);

        assertEquals(1, carried.size(), "Both bins of the same ammo are reported on one line");
        int shots = carried.values().iterator().next();
        assertTrue(shots > 0, "The carriage reports the shots it is carrying");
    }

    @Test
    void ammoFeedingAWeaponOnTheSameUnitIsNotRepeated() throws Exception {
        Tank gunCarriage = buildVehicle();
        gunCarriage.addEquipment(EquipmentType.get(LONG_TOM_CANNON), Tank.LOC_FRONT);
        gunCarriage.addEquipment(EquipmentType.get(LONG_TOM_AMMO), Tank.LOC_BODY);

        Map<String, Integer> carried = UnitToolTip.carriedAmmoShots(gunCarriage);

        assertTrue(carried.isEmpty(), "Ammo for a weapon on this unit already shows under that weapon");
    }

    @Test
    void aUnitWithNoAmmoReportsNothing() {
        Tank tractor = buildVehicle();

        Map<String, Integer> carried = UnitToolTip.carriedAmmoShots(tractor);

        assertTrue(carried.isEmpty(), "A unit with no ammo bins has no carried-ammo section");
    }

    @Test
    void anEmptiedCarriageStillReportsItsAmmoType() throws Exception {
        Tank carriage = buildVehicle();
        carriage.addEquipment(EquipmentType.get(LONG_TOM_AMMO), Tank.LOC_BODY);
        carriage.getAmmo().getFirst().setShotsLeft(0);

        Map<String, Integer> carried = UnitToolTip.carriedAmmoShots(carriage);

        assertEquals(1, carried.size(), "An empty carriage still names what it was carrying");
        assertEquals(0, carried.values().iterator().next(), "and reports it as empty");
    }
}

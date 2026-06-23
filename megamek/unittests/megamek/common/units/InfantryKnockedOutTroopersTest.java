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
package megamek.common.units;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import megamek.common.equipment.EquipmentType;
import megamek.common.weapons.infantry.InfantryWeapon;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verifies the knocked-out-trooper counter used to record non-lethal (recoverable) infantry casualties
 * from Water Ammo (TO:AUE p.174), including that it survives entity serialization so post-battle
 * processing (e.g. MekHQ salvage) can read it.
 */
class InfantryKnockedOutTroopersTest {

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    private static ConvInfantry platoon() {
        ConvInfantry infantry = new ConvInfantry();
        infantry.setChassis("Test Platoon");
        infantry.setPrimaryWeapon((InfantryWeapon) EquipmentType.get("InfantryAssaultRifle"));
        infantry.autoSetInternal();
        infantry.initializeInternal(28, ConvInfantry.LOC_INFANTRY);
        return infantry;
    }

    @Test
    void accumulatesAndIgnoresNonPositiveCounts() {
        ConvInfantry infantry = platoon();
        assertEquals(0, infantry.getKnockedOutTroopers(), "A fresh platoon has no knocked-out troopers");

        infantry.addKnockedOutTroopers(3);
        infantry.addKnockedOutTroopers(2);
        assertEquals(5, infantry.getKnockedOutTroopers(), "Knocked-out troopers should accumulate");

        infantry.addKnockedOutTroopers(0);
        infantry.addKnockedOutTroopers(-4);
        assertEquals(5, infantry.getKnockedOutTroopers(), "Non-positive counts must not change the total");
    }

    @Test
    void survivesSerialization() throws Exception {
        ConvInfantry infantry = platoon();
        infantry.addKnockedOutTroopers(7);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(infantry);
        }
        ConvInfantry restored;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            restored = (ConvInfantry) in.readObject();
        }

        assertEquals(7, restored.getKnockedOutTroopers(),
              "Knocked-out trooper count must survive entity serialization");
    }
}

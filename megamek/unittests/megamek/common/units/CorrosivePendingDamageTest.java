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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verifies the queued-corrosive-damage state used to deliver the End-Phase portion of a Corrosive Ammo
 * attack (TO:AUE p.173), including that it survives entity serialization (it is queued during the Weapon
 * Phase and applied in the End Phase, which spans a server/client packet round-trip).
 */
class CorrosivePendingDamageTest {

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @Test
    void accumulatesIgnoresNonPositiveAndClears() {
        BipedMek mek = new BipedMek();
        assertEquals(0, mek.getPendingCorrosiveDamage(), "A fresh unit has no queued corrosive damage");

        mek.addPendingCorrosiveDamage(3);
        mek.addPendingCorrosiveDamage(2);
        assertEquals(5, mek.getPendingCorrosiveDamage(), "Queued corrosive damage should accumulate");

        mek.addPendingCorrosiveDamage(0);
        mek.addPendingCorrosiveDamage(-2);
        assertEquals(5, mek.getPendingCorrosiveDamage(), "Non-positive amounts must not change the total");

        mek.clearPendingCorrosiveDamage();
        assertEquals(0, mek.getPendingCorrosiveDamage(), "Clearing resets the queued corrosive damage");
    }

    @Test
    void survivesSerialization() throws Exception {
        BipedMek mek = new BipedMek();
        mek.addPendingCorrosiveDamage(4);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(mek);
        }
        BipedMek restored;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            restored = (BipedMek) in.readObject();
        }

        assertEquals(4, restored.getPendingCorrosiveDamage(),
              "Queued corrosive damage must survive entity serialization");
    }
}

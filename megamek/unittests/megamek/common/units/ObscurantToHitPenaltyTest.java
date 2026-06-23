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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import megamek.common.equipment.EquipmentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verifies the Paint/Obscurant sensor-fouling to-hit penalty (TO:AUE p.174): it rises by one per hit to
 * a maximum of +3, can be washed off, and survives entity serialization (the penalty lasts the rest of
 * the scenario, spanning save/load and server/client packets).
 */
class ObscurantToHitPenaltyTest {

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @Test
    void risesByOnePerHitAndCapsAtThree() {
        BipedMek mek = new BipedMek();
        assertEquals(0, mek.getObscurantToHitPenalty(), "A fresh unit has no obscurant penalty");

        assertTrue(mek.addObscurantToHitPenalty(), "First hit applies +1");
        assertTrue(mek.addObscurantToHitPenalty(), "Second hit applies +2");
        assertTrue(mek.addObscurantToHitPenalty(), "Third hit applies +3");
        assertEquals(Entity.MAX_OBSCURANT_PENALTY, mek.getObscurantToHitPenalty(), "Penalty caps at +3");

        assertFalse(mek.addObscurantToHitPenalty(), "A fourth hit cannot exceed the maximum");
        assertEquals(3, mek.getObscurantToHitPenalty(), "Penalty stays at +3");

        mek.clearObscurantToHitPenalty();
        assertEquals(0, mek.getObscurantToHitPenalty(), "Washing the paint off clears the penalty");
    }

    @Test
    void survivesSerialization() throws Exception {
        BipedMek mek = new BipedMek();
        mek.addObscurantToHitPenalty();
        mek.addObscurantToHitPenalty();

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(mek);
        }
        BipedMek restored;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            restored = (BipedMek) in.readObject();
        }

        assertEquals(2, restored.getObscurantToHitPenalty(),
              "The obscurant to-hit penalty must survive entity serialization");
    }
}

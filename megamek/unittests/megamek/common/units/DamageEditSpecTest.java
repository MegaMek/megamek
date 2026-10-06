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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.jupiter.api.Test;

/**
 * The damage editor's spec travels from the gamemaster's client to the server inside a packet, so every field the
 * editor fills has to survive Java serialization; a field that does not would silently drop the edit on the server.
 */
class DamageEditSpecTest {

    @Test
    void weaponAndLocationStatesSurviveSerialization() throws IOException, ClassNotFoundException {
        DamageEditSpec spec = new DamageEditSpec();
        spec.entityId = 3;
        spec.weaponJammed.put(4, true);
        spec.weaponFired.put(5, false);
        spec.directionalMountLocked.put(6, true);
        spec.locationBreached = new Boolean[] { null, true, false };
        spec.locationBlownOff = new Boolean[] { true, null, false };
        spec.autoEject = false;
        spec.conditionalEjectOnHeadshot = true;
        spec.targetModifier = -2;

        DamageEditSpec copy = roundTrip(spec);

        assertEquals(3, copy.entityId);
        assertEquals(spec.weaponJammed, copy.weaponJammed);
        assertEquals(spec.weaponFired, copy.weaponFired);
        assertEquals(spec.directionalMountLocked, copy.directionalMountLocked);
        assertArrayEquals(spec.locationBreached, copy.locationBreached);
        assertArrayEquals(spec.locationBlownOff, copy.locationBlownOff);
        assertEquals(false, copy.autoEject);
        assertEquals(true, copy.conditionalEjectOnHeadshot);
        assertEquals(-2, copy.targetModifier);
    }

    /** Writes the spec out and reads it back the way a packet does. */
    private static DamageEditSpec roundTrip(DamageEditSpec spec) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(spec);
        }
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return (DamageEditSpec) input.readObject();
        }
    }
}

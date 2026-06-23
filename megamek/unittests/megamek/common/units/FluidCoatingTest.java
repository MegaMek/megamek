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
 * Verifies the per-unit Fluid Gun / Sprayer chemical coating (TO:AUE pp.173-174): Oil Slick makes the
 * coated unit easier to ignite (-2), Flame-Retardant Foam harder (+4), and the coating travels with the
 * unit across serialization (it lasts the rest of the scenario, spanning save/load and server packets).
 */
class FluidCoatingTest {

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @Test
    void coatingIgnitionModifiersMatchRaw() {
        assertEquals(0, FluidCoating.NONE.ignitionModifier(), "No coating gives no modifier");
        assertEquals(-2, FluidCoating.OIL_SLICK.ignitionModifier(), "Oil Slick makes ignition easier (-2)");
        assertEquals(4, FluidCoating.FLAME_RETARDANT_FOAM.ignitionModifier(), "Foam makes ignition harder (+4)");
    }

    @Test
    void freshUnitHasNoCoating() {
        BipedMek mek = new BipedMek();
        assertEquals(FluidCoating.NONE, mek.getFluidCoating(), "A fresh unit is uncoated");
        assertEquals(0, mek.getFluidCoatingIgnitionModifier(), "An uncoated unit has no ignition modifier");
    }

    @Test
    void applyingACoatingReplacesTheLast() {
        BipedMek mek = new BipedMek();

        mek.setFluidCoating(FluidCoating.OIL_SLICK);
        assertEquals(-2, mek.getFluidCoatingIgnitionModifier(), "Oiling the unit makes it easier to ignite");

        // Foam over oil: the fire-retardant coating replaces the flammable one.
        mek.setFluidCoating(FluidCoating.FLAME_RETARDANT_FOAM);
        assertEquals(4, mek.getFluidCoatingIgnitionModifier(), "Foam replaces the oil coating");

        mek.setFluidCoating(FluidCoating.NONE);
        assertEquals(0, mek.getFluidCoatingIgnitionModifier(), "Clearing the coating removes the modifier");
    }

    @Test
    void survivesSerialization() throws Exception {
        BipedMek mek = new BipedMek();
        mek.setFluidCoating(FluidCoating.FLAME_RETARDANT_FOAM);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(mek);
        }
        BipedMek restored;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            restored = (BipedMek) in.readObject();
        }

        assertEquals(FluidCoating.FLAME_RETARDANT_FOAM, restored.getFluidCoating(),
              "The fluid coating must survive entity serialization");
    }
}

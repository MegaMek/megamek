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
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks
 * of InMediaRes Productions LLC. All Rights Reserved.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.common.loaders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.io.ByteArrayInputStream;

import megamek.common.bays.CrewQuartersCargoBay;
import megamek.common.bays.FirstClassQuartersCargoBay;
import megamek.common.bays.SecondClassQuartersCargoBay;
import megamek.common.equipment.EquipmentType;
import megamek.common.units.SmallCraft;
import megamek.common.util.BuildingBlock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BLKSmallCraftFileTest {

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @Test
    void loadingSmallCraftWithoutQuartersAddsRequiredAccommodations() throws Exception {
        SmallCraft smallCraft = (SmallCraft) new BLKSmallCraftFile(new BuildingBlock(
              new ByteArrayInputStream(minimalSmallCraft().getBytes()))).getEntity();

        assertEquals(3, smallCraft.getNCrew());
        assertEquals(1, smallCraft.getNOfficers());
        assertEquals(4, smallCraft.getTransportBays().size());
        assertInstanceOf(FirstClassQuartersCargoBay.class, smallCraft.getTransportBays().get(1));
        assertEquals(1, smallCraft.getTransportBays().get(1).getCapacity());
        assertInstanceOf(SecondClassQuartersCargoBay.class, smallCraft.getTransportBays().get(2));
        assertEquals(0, smallCraft.getTransportBays().get(2).getCapacity());
        assertInstanceOf(CrewQuartersCargoBay.class, smallCraft.getTransportBays().get(3));
        assertEquals(2, smallCraft.getTransportBays().get(3).getCapacity());
    }

    @Test
    void loadingSmallCraftWithQuartersDoesNotAddDuplicateAccommodations() throws Exception {
        SmallCraft smallCraft = (SmallCraft) new BLKSmallCraftFile(new BuildingBlock(
              new ByteArrayInputStream((minimalSmallCraft()
                    .replace("</transporters>", "crewquarters:7.0:0:-1::-1:0" + System.lineSeparator()
                          + "</transporters>")).getBytes()))).getEntity();

        assertEquals(2, smallCraft.getTransportBays().size());
        assertInstanceOf(CrewQuartersCargoBay.class, smallCraft.getTransportBays().get(1));
        assertEquals(1, smallCraft.getTransportBays().get(1).getCapacity());
    }

    private static String minimalSmallCraft() {
        return String.join(System.lineSeparator(),
              "<Name>", "Legacy Escape Pod", "</Name>",
              "<year>", "2647", "</year>",
              "<type>", "IS Level 2", "</type>",
              "<tonnage>", "5", "</tonnage>",
              "<crew>", "1", "</crew>",
              "<motion_type>", "Aerodyne", "</motion_type>",
              "<structural_integrity>", "1", "</structural_integrity>",
              "<heatsinks>", "0", "</heatsinks>",
              "<sink_type>", "0", "</sink_type>",
              "<fuel>", "10", "</fuel>",
              "<SafeThrust>", "4", "</SafeThrust>",
              "<armor_type>", "41", "</armor_type>",
              "<armor>", "1", "1", "1", "1", "</armor>",
              "<transporters>", "cargobay:0.48:0:1::-1:0", "</transporters>");
    }
}
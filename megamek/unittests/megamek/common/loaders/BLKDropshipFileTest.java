/*
 * Copyright (C) 2023-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common.loaders;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Vector;

import megamek.common.bays.BattleArmorBay;
import megamek.common.bays.Bay;
import megamek.common.equipment.EquipmentType;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.util.BuildingBlock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BLKDropshipFileTest {

    /**
     * Load a string of BLK-style blocks as an InputStream and create a new DropShip produces the desired mix of tech,
     * specifically with Clan tech and IS BA bays.
     */
    private Dropship loadDropshipFromString(String strOfBLK) throws Exception {

        // Create InputStream from string
        InputStream is = new ByteArrayInputStream(strOfBLK.getBytes());

        // Instantiate bb with string
        BuildingBlock bb = new BuildingBlock(is);

        // Instantiate Dropship with bb
        IMekLoader loader = new BLKDropshipFile(bb);

        // Get Entity
        Entity m_entity = loader.getEntity();

        return (Dropship) m_entity;
    }

    /**
     * Helper to troll through bays looking for a specific combination. Can be extended as needed.
     */
    private boolean confirmBayTypeInBays(Vector<Bay> bays, String type) {
        boolean found = false;
        for (Bay b : bays) {
            switch (type) {
                case "BA_IS":
                    if (b instanceof BattleArmorBay) {
                        found = !b.isClan();
                    }
                    break;
                case "BA_CLAN":
                    if (b instanceof BattleArmorBay) {
                        found = b.isClan();
                    }
                    break;
                case "BA_CS":
                    if (b instanceof BattleArmorBay) {
                        found = ((BattleArmorBay) b).isComStar();
                    }
                    break;
            }
            if (found) {
                return true;
            }
        }

        return false;
    }

    @BeforeAll
    static void initialize() {
        EquipmentType.initializeTypes();
    }

    @Test
    void testLoadNewFormatDSHasMixedBATechLevels() throws Exception {
        boolean parsed;
        boolean mixedTech;
        boolean ISBACorrect;
        boolean ClanBACorrect;
        boolean ComStarBACorrect;
        Vector<Bay> bays;

        Dropship dropship = loadDropshipFromString(newFormatDSWithMixedBA);
        parsed = true;
        mixedTech = dropship.isMixedTech() && dropship.isClan(); // confirm mixed-tech Clan design
        bays = dropship.getTransportBays();
        ISBACorrect = confirmBayTypeInBays(bays, "BA_IS");
        ClanBACorrect = confirmBayTypeInBays(bays, "BA_CLAN");
        ComStarBACorrect = confirmBayTypeInBays(bays, "BA_CS");

        assertTrue(parsed);
        assertTrue(mixedTech);
        assertTrue(ISBACorrect);
        assertTrue(ClanBACorrect);
        assertTrue(ComStarBACorrect);
    }

    @Test
    void testLoadOldFormatClanDSHasClanBATech() throws Exception {
        // We want to verify that the correct tech type is applied to non-mixed
        // Clan BA bays when loading old-format files.
        boolean parsed;
        boolean mixedTech;
        boolean clan;
        boolean ClanBACorrect;
        boolean ISBAExists;
        Vector<Bay> bays;

        Dropship dropship = loadDropshipFromString(oldFormatClanDSWithBA);
        parsed = true;
        mixedTech = dropship.isMixedTech(); // confirm not mixed tech
        clan = dropship.isClan(); // confirm clan tech base
        bays = dropship.getTransportBays();
        ClanBACorrect = confirmBayTypeInBays(bays, "BA_CLAN");
        ISBAExists = confirmBayTypeInBays(bays, "BA_IS");
        assertTrue(parsed);
        assertTrue(clan);
        assertFalse(mixedTech);
        assertTrue(ClanBACorrect);
        assertFalse(ISBAExists);
    }

    // region DS definitions
    private static final String newFormatDSWithMixedBA = String.join(
          System.lineSeparator(),
          "<BlockVersion>",
          "1",
          "</BlockVersion>",
          "<Version>",
          "MAM0",
          "</Version>",
          "<UnitType>",
          "Dropship",
          "</UnitType>",
          "<Name>",
          "New",
          "</Name>",
          "<Model>",
          "Dropship",
          "</Model>",
          "<year>",
          "3145",
          "</year>",
          "<originalBuildYear>",
          "3145",
          "</originalBuildYear>",
          "<type>",
          "Mixed (Clan Chassis)",
          "</type>",
          "<motion_type>",
          "Aerodyne",
          "</motion_type>",
          "<transporters>",
          "battlearmorbay:1.0:1:1::-1:0",
          "1stclassquarters:10.0:0:-1::-1:0",
          "crewquarters:28.0:0:-1::-1:0",
          "battlearmorbay:2.0:1:2::-1:2",
          "battlearmorbay:3.0:1:3::-1:1",
          "</transporters>",
          "<SafeThrust>",
          "2",
          "</SafeThrust>",
          "<heatsinks>",
          "1",
          "</heatsinks>",
          "<sink_type>",
          "1",
          "</sink_type>",
          "<fuel>",
          "4280",
          "</fuel>",
          "<armor_type>",
          "41",
          "</armor_type>",
          "<armor_tech>",
          "2",
          "</armor_tech>",
          "<internal_type>",
          "-1",
          "</internal_type>",
          "<armor>",
          "85",
          "70",
          "70",
          "57",
          "</armor>",
          "<Nose Equipment>",
          "</Nose Equipment>",
          "<Left Side Equipment>",
          "</Left Side Equipment>",
          "<Right Side Equipment>",
          "</Right Side Equipment>",
          "<Aft Equipment>",
          "</Aft Equipment>",
          "<Hull Equipment>",
          "</Hull Equipment>",
          "<structural_integrity>",
          "3",
          "</structural_integrity>",
          "<tonnage>",
          "200.0",
          "</tonnage>",
          "<designtype>",
          "1",
          "</designtype>",
          "<crew>",
          "41",
          "</crew>",
          "<officers>",
          "1",
          "</officers>",
          "<gunners>",
          "0",
          "</gunners>",
          "<passengers>",
          "0",
          "</passengers>",
          "<marines>",
          "0",
          "</marines>",
          "<battlearmor>",
          "0",
          "</battlearmor>",
          "<otherpassenger>",
          "0",
          "</otherpassenger>",
          "<life_boat>",
          "0",
          "</life_boat>",
          "<escape_pod>",
          "0",
          "</escape_pod>");

    private static final String oldFormatClanDSWithBA = String.join(
          System.lineSeparator(),
          "<BlockVersion>",
          "1",
          "</BlockVersion>",
          "<Version>",
          "MAM0",
          "</Version>",
          "<UnitType>",
          "Dropship",
          "</UnitType>",
          "<Name>",
          "Old",
          "</Name>",
          "<Model>",
          "Dropship",
          "</Model>",
          "<year>",
          "3145",
          "</year>",
          "<originalBuildYear>",
          "3145",
          "</originalBuildYear>",
          "<type>",
          "Clan Level 3",
          "</type>",
          "<motion_type>",
          "Aerodyne",
          "</motion_type>",
          "<transporters>",
          "battlearmorbay:5.0:1:1",
          "1stclassquarters:10.0:0:-1::-1:0",
          "crewquarters:28.0:0:-1::-1:0",
          "</transporters>",
          "<SafeThrust>",
          "2",
          "</SafeThrust>",
          "<heatsinks>",
          "1",
          "</heatsinks>",
          "<sink_type>",
          "1",
          "</sink_type>",
          "<fuel>",
          "4280",
          "</fuel>",
          "<armor_type>",
          "41",
          "</armor_type>",
          "<armor_tech>",
          "2",
          "</armor_tech>",
          "<internal_type>",
          "-1",
          "</internal_type>",
          "<armor>",
          "85",
          "70",
          "70",
          "57",
          "</armor>",
          "<Nose Equipment>",
          "</Nose Equipment>",
          "<Left Side Equipment>",
          "</Left Side Equipment>",
          "<Right Side Equipment>",
          "</Right Side Equipment>",
          "<Aft Equipment>",
          "</Aft Equipment>",
          "<Hull Equipment>",
          "</Hull Equipment>",
          "<structural_integrity>",
          "3",
          "</structural_integrity>",
          "<tonnage>",
          "200.0",
          "</tonnage>",
          "<designtype>",
          "1",
          "</designtype>",
          "<crew>",
          "41",
          "</crew>",
          "<officers>",
          "1",
          "</officers>",
          "<gunners>",
          "0",
          "</gunners>",
          "<passengers>",
          "0",
          "</passengers>",
          "<marines>",
          "0",
          "</marines>",
          "<battlearmor>",
          "0",
          "</battlearmor>",
          "<otherpassenger>",
          "0",
          "</otherpassenger>",
          "<life_boat>",
          "0",
          "</life_boat>",
          "<escape_pod>",
          "0",
          "</escape_pod>");
    // endregion
}

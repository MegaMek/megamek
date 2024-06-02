/*
 * Copyright (c) 2023, 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.loaders;

import megamek.common.*;
import megamek.common.util.BuildingBlock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.*;

public class BLKDropshipFileTest {

    /**
     *  Load a string of BLK-style blocks as an InputStream and create a new DropShip
     *  produces the desired mix of tech, specifically with Clan tech and IS BA bays.
     */
    private Dropship loadDropshipFromString(String strOfBLK) throws Exception {

        // Create InputStream from string
        InputStream is = new ByteArrayInputStream(strOfBLK.getBytes());

        // Instantiate bb with string
        BuildingBlock bb = new BuildingBlock(is);

        // Instantiate Dropship with bb
        IMechLoader loader = new BLKDropshipFile(bb);

        // Get Entity
        Entity m_entity = loader.getEntity();

        return (Dropship) m_entity;
    }

    /**
     *  Helper to troll through bays looking for a specific combination.
     *  Can be extended as needed.
     */
    private boolean confirmBayTypeinBays(Vector<Bay> bays, String type) {
        boolean found = false;
        for(Bay b: bays) {
            switch(type) {
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
    public static void initialize() {
        EquipmentType.initializeTypes();
    }

    @Test
    public void testLoadNewFormatDSHasMixedBATechLevels() {
        boolean parsed = false;
        boolean mixedTech = false;
        boolean ISBACorrect = false;
        boolean ClanBACorrect = false;
        boolean ComStarBACorrect = false;
        Vector<Bay> bays;

        try {
            Dropship ds = loadDropshipFromString(newFormatDSwithMixedBA);
            parsed = true;
            mixedTech = ds.isMixedTech() && ds.isClan(); // confirm mixed-tech Clan design
            bays = ds.getTransportBays();
            ISBACorrect = confirmBayTypeinBays(bays, "BA_IS");
            ClanBACorrect = confirmBayTypeinBays(bays, "BA_CLAN");
            ComStarBACorrect = confirmBayTypeinBays(bays, "BA_CS");
        } catch (Exception e){
            e.printStackTrace();
        }
        assertTrue(parsed);
        assertTrue(mixedTech);
        assertTrue(ISBACorrect);
        assertTrue(ClanBACorrect);
        assertTrue(ComStarBACorrect);
    }

    @Test
    public void testLoadOldFormatClanDSHasClanBATech() {
        // We want to verify that the correct tech type is applied to non-mixed
        // Clan BA bays when loading old-format files.
        boolean parsed = false;
        boolean mixedTech = false;
        boolean clan = false;
        boolean ClanBACorrect = false;
        boolean ISBAExists = false;
        Vector<Bay> bays;

        try {
            Dropship ds = loadDropshipFromString(oldFormatClanDSwithBA);
            parsed = true;
            mixedTech = ds.isMixedTech();   // confirm not mixed tech
            clan = ds.isClan();             // confirm clan tech base
            bays = ds.getTransportBays();
            ClanBACorrect = confirmBayTypeinBays(bays, "BA_CLAN");
            ISBAExists = confirmBayTypeinBays(bays, "BA_IS");
        } catch (Exception e){
            e.printStackTrace();
        }
        assertTrue(parsed);
        assertTrue(clan);
        assertFalse(mixedTech);
        assertTrue(ClanBACorrect);
        assertFalse(ISBAExists);
    }

    //region DS definitions
    private static final String newFormatDSwithMixedBA = String.join(
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
            "</escape_pod>"
    );

    private static final String oldFormatClanDSwithBA = String.join(
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
            "</escape_pod>"
    );
    //endregion
}

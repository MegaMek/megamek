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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import megamek.common.compute.Compute;
import megamek.common.enums.BuildingType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.loaders.MekFileParser;
import megamek.common.weapons.autoCannons.innerSphere.ISAC2;
import megamek.common.weapons.capitalWeapons.naval.NL35Weapon;
import megamek.common.weapons.gaussRifles.innerSphere.ISGaussRifle;
import megamek.common.weapons.infantry.rifle.InfantryRifleAutoRifleWeapon;
import megamek.common.weapons.lasers.innerSphere.medium.ISLaserMedium;
import megamek.common.weapons.mgs.innerSphere.ISMG;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The Advanced Building Minimum Crew Table (TO:AR p. 130) and the {@code crew} block of a building unit file.
 */
@DisplayName("Advanced Building crew")
class AdvancedBuildingCrewTest {

    private static final String LIGHT_LASER_EMPLACEMENT_WITH_CREW = """
          <UnitType>
          BuildingEntity
          </UnitType>
          <Name>
          Crew Test Emplacement
          </Name>
          <Model>
          (Test)
          </Model>
          <year>
          2300
          </year>
          <type>
          IS Level 2
          </type>
          <motion_type>
          None
          </motion_type>
          <cruiseMP>
          0
          </cruiseMP>
          <armor>
          20
          </armor>
          <Level 0 0.0,0.0,0.0 Equipment>
          Medium Laser(ST)
          Medium Laser(ST)
          Small Laser(ST)
          Small Laser(ST)
          Searchlight(ST)
          FUSION PowerGenerator:SIZE:4.0
          </Level 0 0.0,0.0,0.0 Equipment>
          <height>
          1
          </height>
          <building_class>
          3
          </building_class>
          <building_type>
          1
          </building_type>
          <cf>
          30
          </cf>
          <crew>
          %s
          </crew>
          <coords>
          0.0,0.0,0.0
          </coords>
          """;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    private static BuildingEntity emplacement() {
        return new BuildingEntity(BuildingType.LIGHT, IBuilding.GUN_EMPLACEMENT);
    }

    private static void mount(BuildingEntity building, WeaponType weaponType) throws Exception {
        building.addEquipment(new WeaponMounted(building, weaponType), 0, false);
    }

    private static void mount(BuildingEntity building, MiscType miscType, double size) throws Exception {
        MiscMounted mounted = new MiscMounted(building, miscType);
        mounted.setSize(size);
        building.addEquipment(mounted, 0, false);
    }

    @Test
    @DisplayName("An unarmed, unequipped building needs no crew")
    void emptyBuildingNeedsNobody() {
        assertEquals(0, emplacement().getNCrew());
    }

    @Test
    @DisplayName("A heavy weapon under five tons takes one gunner, plus one officer")
    void lightHeavyWeaponsTakeOneGunnerEach() throws Exception {
        BuildingEntity building = emplacement();
        mount(building, new ISMG());          // 0.5 tons
        mount(building, new ISLaserMedium()); // 1 ton
        assertEquals(2 + 1, building.getNCrew(), "two gunners and one officer");
    }

    @Test
    @DisplayName("A six-ton autocannon takes two gunners, a fifteen-ton Gauss rifle three")
    void heavyWeaponsTakeOneGunnerPerFiveTonsRoundedUp() throws Exception {
        BuildingEntity building = emplacement();
        mount(building, new ISAC2());        // 6 tons -> 2
        mount(building, new ISGaussRifle()); // 15 tons -> 3
        assertEquals(5 + 1, building.getNCrew(), "five gunners and one officer");
    }

    @Test
    @DisplayName("A capital weapon takes seven gunners")
    void capitalWeaponTakesSevenGunners() throws Exception {
        BuildingEntity building = emplacement();
        mount(building, new NL35Weapon());
        assertEquals(7 + 1, building.getNCrew(), "seven gunners and one officer");
    }

    @Test
    @DisplayName("A conventional infantry weapon takes one gunner whatever it weighs")
    void infantryWeaponTakesOneGunner() throws Exception {
        BuildingEntity building = emplacement();
        mount(building, new InfantryRifleAutoRifleWeapon());
        assertEquals(1 + 1, building.getNCrew(), "one gunner and one officer");
    }

    @Test
    @DisplayName("Ten or more non-officer crew need one officer per ten, rounded up")
    void largeCrewsNeedMoreOfficers() throws Exception {
        BuildingEntity building = emplacement();
        for (int weapon = 0; weapon < 4; weapon++) {
            mount(building, new ISGaussRifle()); // 3 gunners each, 12 gunners in all
        }
        assertEquals(12 + 2, building.getNCrew(), "twelve gunners and two officers");
    }

    @Test
    @DisplayName("Communications equipment takes one non-gunner per ton")
    void communicationsEquipmentTakesOneNonGunnerPerTon() throws Exception {
        BuildingEntity building = emplacement();
        mount(building, (MiscType) EquipmentType.get("Communications Equipment"), 7.0);
        assertEquals(7 + 1, building.getNCrew(), "seven non-gunners and one officer");
    }

    @Test
    @DisplayName("A field kitchen takes three non-gunners, a mobile field base five")
    void supportEquipmentTakesItsListedCrew() throws Exception {
        BuildingEntity building = emplacement();
        mount(building, (MiscType) EquipmentType.get("FieldKitchen"), 1.0);
        mount(building, (MiscType) EquipmentType.get("ISMobileFieldBase"), 1.0);
        assertEquals(3 + 5 + 1, building.getNCrew(), "eight non-gunners and one officer");
    }

    @Test
    @DisplayName("An explicit crew count from the unit file replaces the table")
    void explicitCrewCountReplacesTheTable() throws Exception {
        BuildingEntity building = emplacement();
        mount(building, new ISMG());
        assertFalse(building.hasExplicitCrewCount());
        building.setCrewCount(12);
        assertTrue(building.hasExplicitCrewCount());
        assertEquals(12, building.getNCrew());
        building.setCrewCount(AbstractBuildingEntity.CREW_FROM_MINIMUM_CREW_TABLE);
        assertEquals(2, building.getNCrew(), "back to the table: one gunner and one officer");
    }

    @Test
    @DisplayName("The full crew size of a building is its crew plus bay personnel")
    void fullCrewSizeIsCrewPlusBayPersonnel() throws Exception {
        BuildingEntity building = emplacement();
        building.setCrewCount(9);
        assertEquals(9 + building.getBayPersonnel(), Compute.getFullCrewSize(building));
    }

    @Test
    @DisplayName("A unit file's crew block sets the crew and sizes the crew object to match")
    void unitFileCrewBlockSetsCrewAndSizesCrewObject() throws Exception {
        AbstractBuildingEntity building = load(LIGHT_LASER_EMPLACEMENT_WITH_CREW.formatted("9"));
        assertTrue(building.hasExplicitCrewCount());
        assertEquals(9, building.getNCrew());
        assertEquals(9, building.getCrew().getSize(), "crew object sized to the head-count");
        assertEquals(9, building.getCrew().getCurrentSize());
    }

    @Test
    @DisplayName("Without a crew block the crew is the table minimum, and the crew object matches it")
    void unitFileWithoutCrewBlockUsesTheTable() throws Exception {
        String withoutCrewBlock = LIGHT_LASER_EMPLACEMENT_WITH_CREW.replace("<crew>\n%s\n</crew>\n", "");
        AbstractBuildingEntity building = load(withoutCrewBlock);
        assertFalse(building.hasExplicitCrewCount());
        assertEquals(4 + 1, building.getNCrew(), "four one-ton-or-less lasers and one officer");
        assertEquals(5, building.getCrew().getCurrentSize());
    }

    private static AbstractBuildingEntity load(String unitFile) throws Exception {
        InputStream stream = new ByteArrayInputStream(unitFile.getBytes(StandardCharsets.UTF_8));
        Entity entity = new MekFileParser(stream, "crew-test.blk").getEntity();
        return assertInstanceOf(AbstractBuildingEntity.class, entity);
    }
}

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
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.File;

import megamek.common.equipment.EquipmentType;
import megamek.common.loaders.MekFileParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for editing an Advanced Building's damage from the unit editor.
 *
 * <p>A building keeps its armor and Construction Factor in the {@link Building} it wraps, and
 * {@link AbstractBuildingEntity} answers reads from there. Writes that went only to the entity's own arrays were
 * therefore lost, so an edited value never appeared; and a zero arrived as an {@link
 * megamek.common.equipment.IArmorState} sentinel, which {@link Building} rejects outright. These tests drive the
 * same path the editor uses - {@link DamageEditApplier}, which is what the lobby commits through.</p>
 */
class BuildingDamageEditTest {

    /** The seven hex, three level test building; 21 locations, armor 10 and Construction Factor 15 per hex. */
    private static final String LARGE_BUILDING_FILE = "Simple Large Building Entity.blk";

    /** Locations are laid out hex by hex, one per floor, so the second hex's ground floor is location 3. */
    private static final int FIRST_HEX_GROUND_FLOOR = 0;
    private static final int FIRST_HEX_TOP_FLOOR = 2;
    private static final int SECOND_HEX_GROUND_FLOOR = 3;

    private static final int ORIGINAL_ARMOR = 10;
    private static final int ORIGINAL_CONSTRUCTION_FACTOR = 15;

    @BeforeAll
    static void initialize() {
        EquipmentType.initializeTypes();
    }

    private AbstractBuildingEntity loadLargeBuilding() throws Exception {
        File file = new File("testresources/megamek/common/units/" + LARGE_BUILDING_FILE);
        return (AbstractBuildingEntity) new MekFileParser(file).getEntity();
    }

    /** Applies an armor edit the way the unit editor commits one, by location. */
    private void editArmor(AbstractBuildingEntity building, int location, int armor) {
        DamageEditSpec spec = new DamageEditSpec();
        spec.entityId = building.getId();
        spec.armor = new Integer[building.locations()];
        spec.armor[location] = armor;
        new DamageEditApplier(building, spec).applyToEntity();
    }

    /** Applies a Construction Factor edit the way the unit editor commits one, by location. */
    private void editConstructionFactor(AbstractBuildingEntity building, int location, int constructionFactor) {
        DamageEditSpec spec = new DamageEditSpec();
        spec.entityId = building.getId();
        spec.internal = new Integer[building.locations()];
        spec.internal[location] = constructionFactor;
        new DamageEditApplier(building, spec).applyToEntity();
    }

    @Test
    @DisplayName("the test building starts with the armor and Construction Factor its file gives it")
    void buildingLoadsWithExpectedArmorAndConstructionFactor() throws Exception {
        AbstractBuildingEntity building = loadLargeBuilding();

        assertEquals(21, building.locations(), "seven hexes of three levels each");
        assertEquals(ORIGINAL_ARMOR, building.getArmor(FIRST_HEX_GROUND_FLOOR, false));
        assertEquals(ORIGINAL_CONSTRUCTION_FACTOR, building.getInternal(FIRST_HEX_GROUND_FLOOR));
    }

    @Test
    @DisplayName("armor set to a lower value is kept")
    void armorEditIsKept() throws Exception {
        AbstractBuildingEntity building = loadLargeBuilding();

        editArmor(building, FIRST_HEX_GROUND_FLOOR, 4);

        assertNotEquals(ORIGINAL_ARMOR, building.getArmor(FIRST_HEX_GROUND_FLOOR, false),
              "the edit must not be silently discarded");
        assertEquals(4, building.getArmor(FIRST_HEX_GROUND_FLOOR, false));
    }

    @Test
    @DisplayName("armor set to zero is kept as zero rather than discarded")
    void armorEditToZeroIsKept() throws Exception {
        AbstractBuildingEntity building = loadLargeBuilding();

        editArmor(building, FIRST_HEX_GROUND_FLOOR, 0);

        assertEquals(0, building.getArmor(FIRST_HEX_GROUND_FLOOR, false),
              "a building hex with no armor left stands at zero, it does not have a location blown off");
    }

    @Test
    @DisplayName("Construction Factor set to zero is kept rather than throwing")
    void constructionFactorEditToZeroIsKept() throws Exception {
        AbstractBuildingEntity building = loadLargeBuilding();

        editConstructionFactor(building, FIRST_HEX_GROUND_FLOOR, 0);

        assertEquals(0, building.getInternal(FIRST_HEX_GROUND_FLOOR));
    }

    @Test
    @DisplayName("Construction Factor set to a lower value is kept")
    void constructionFactorEditIsKept() throws Exception {
        AbstractBuildingEntity building = loadLargeBuilding();

        editConstructionFactor(building, FIRST_HEX_GROUND_FLOOR, 7);

        assertEquals(7, building.getInternal(FIRST_HEX_GROUND_FLOOR));
    }

    @Test
    @DisplayName("armor is held per hex, so editing one floor changes every floor of that hex only")
    void armorEditAppliesToTheWholeHexButNotToOtherHexes() throws Exception {
        AbstractBuildingEntity building = loadLargeBuilding();

        editArmor(building, FIRST_HEX_GROUND_FLOOR, 3);

        assertEquals(3, building.getArmor(FIRST_HEX_GROUND_FLOOR, false));
        assertEquals(3, building.getArmor(FIRST_HEX_TOP_FLOOR, false),
              "a building holds one armor value per hex, not one per floor");
        assertEquals(ORIGINAL_ARMOR, building.getArmor(SECOND_HEX_GROUND_FLOOR, false),
              "a neighbouring hex keeps its own armor");
    }

    @Test
    @DisplayName("a rear armor write does not stand in for the hex's real armor")
    void rearArmorWriteLeavesHexArmorAlone() throws Exception {
        AbstractBuildingEntity building = loadLargeBuilding();

        building.setArmor(1, FIRST_HEX_GROUND_FLOOR, true);

        assertEquals(ORIGINAL_ARMOR, building.getArmor(FIRST_HEX_GROUND_FLOOR, false),
              "a building hex has no rear facing, so a rear write must not overwrite its armor");
    }
}

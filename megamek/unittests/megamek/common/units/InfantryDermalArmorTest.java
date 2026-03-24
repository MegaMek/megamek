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

import megamek.common.equipment.EquipmentType;
import megamek.common.exceptions.LocationFullException;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for MD_DERMAL_ARMOR and MD_TSM_IMPLANT damage divisor interactions on Infantry units. Verifies fix for GitHub
 * issue #8111: TSM should only reduce divisor to 0.5 when no armor kit is equipped, not when an armor kit happens to
 * have divisor 1.0.
 *
 * <p>Per IO p.81:</p>
 * <ul>
 *   <li>Dermal Armor: adds +1 to the unit's damage divisor</li>
 *   <li>TSM alone, no armor: divisor of 0.5</li>
 *   <li>TSM + Dermal Armor, no other armor: combination becomes 1.5</li>
 *   <li>TSM with armor present: adds nothing to divisor</li>
 * </ul>
 */
class InfantryDermalArmorTest {

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    private Infantry createInfantry(boolean hasDermalArmor, boolean hasTSM) {
        Infantry infantry = new Infantry();
        infantry.setId(1);
        infantry.setMovementMode(EntityMovementMode.INF_LEG);
        infantry.setSquadSize(7);
        infantry.setSquadCount(4);
        infantry.autoSetInternal();

        Crew crew = new Crew(CrewType.INFANTRY_CREW);
        crew.setGunnery(4, crew.getCrewType().getGunnerPos());
        crew.setPiloting(5, crew.getCrewType().getPilotPos());
        crew.setName("Test Crew", 0);

        PilotOptions options = new PilotOptions();
        if (hasDermalArmor) {
            options.getOption(OptionsConstants.MD_DERMAL_ARMOR).setValue(true);
        }
        if (hasTSM) {
            options.getOption(OptionsConstants.MD_TSM_IMPLANT).setValue(true);
        }
        crew.setOptions(options);
        infantry.setCrew(crew);

        return infantry;
    }

    private void addArmorKit(Infantry infantry, String armorKitName) throws LocationFullException {
        EquipmentType armorKit = EquipmentType.get(armorKitName);
        if (armorKit != null) {
            infantry.addEquipment(armorKit, Infantry.LOC_INFANTRY);
        }
    }

    // --- No armor kit equipped ---

    @Test
    @DisplayName("Dermal Armor alone, no armor kit: divisor = 2.0 (base 1.0 + 1.0)")
    void dermalArmorAloneNoKit() {
        Infantry infantry = createInfantry(true, false);
        assertEquals(2.0, infantry.calcDamageDivisor(), 0.001);
    }

    @Test
    @DisplayName("TSM alone, no armor kit: divisor = 0.5")
    void tsmAloneNoKit() {
        Infantry infantry = createInfantry(false, true);
        assertEquals(0.5, infantry.calcDamageDivisor(), 0.001);
    }

    @Test
    @DisplayName("TSM + Dermal Armor, no armor kit: divisor = 1.5 (0.5 + 1.0)")
    void tsmAndDermalArmorNoKit() {
        Infantry infantry = createInfantry(true, true);
        assertEquals(1.5, infantry.calcDamageDivisor(), 0.001);
    }

    // --- With armor kit (divisor 1.0) - the bug scenario ---

    @Test
    @DisplayName("TSM + armor kit (div 1.0): divisor = 1.0 (TSM adds nothing when armor present)")
    void tsmWithArmorKitDivisorOne() throws LocationFullException {
        Infantry infantry = createInfantry(false, true);
        addArmorKit(infantry, "Generic Infantry Kit");
        assertEquals(1.0, infantry.calcDamageDivisor(), 0.001);
    }

    @Test
    @DisplayName("TSM + Dermal Armor + armor kit (div 1.0): divisor = 2.0 (kit 1.0 + dermal 1.0)")
    void tsmAndDermalArmorWithArmorKitDivisorOne() throws LocationFullException {
        Infantry infantry = createInfantry(true, true);
        addArmorKit(infantry, "Generic Infantry Kit");
        assertEquals(2.0, infantry.calcDamageDivisor(), 0.001);
    }

    @Test
    @DisplayName("Dermal Armor + armor kit (div 1.0): divisor = 2.0 (kit 1.0 + dermal 1.0)")
    void dermalArmorWithArmorKitDivisorOne() throws LocationFullException {
        Infantry infantry = createInfantry(true, false);
        addArmorKit(infantry, "Generic Infantry Kit");
        assertEquals(2.0, infantry.calcDamageDivisor(), 0.001);
    }

    // --- With armor kit (divisor > 1.0) - should be unaffected by fix ---

    @Test
    @DisplayName("TSM + armor kit (div 2.0): divisor = 2.0 (TSM adds nothing when armor present)")
    void tsmWithHighDivisorArmorKit() throws LocationFullException {
        Infantry infantry = createInfantry(false, true);
        addArmorKit(infantry, "Ballistic Plate, Standard");
        assertEquals(2.0, infantry.calcDamageDivisor(), 0.001);
    }

    @Test
    @DisplayName("TSM + Dermal Armor + armor kit (div 2.0): divisor = 3.0 (kit 2.0 + dermal 1.0)")
    void tsmAndDermalArmorWithHighDivisorArmorKit() throws LocationFullException {
        Infantry infantry = createInfantry(true, true);
        addArmorKit(infantry, "Ballistic Plate, Standard");
        assertEquals(3.0, infantry.calcDamageDivisor(), 0.001);
    }
}

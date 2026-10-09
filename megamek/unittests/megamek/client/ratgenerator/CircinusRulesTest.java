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
package megamek.client.ratgenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import megamek.common.units.UnitType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The Circinus Federation follows standard Star League organization (Field Manual: Periphery p.102), so it uses the
 * Inner Sphere baseline rules. It used to fall through to the Word of Blake Protectorate Militia rules, which offered
 * only Demi-Company, Lance and "Choir", and no conventional fighters (MegaMek/mekhq#10376 items 27 and 28).
 */
class CircinusRulesTest {

    private static final int ECHELON_COMPANY = 4;
    private static final int ECHELON_BATTALION = 5;
    private static final int ECHELON_REGIMENT = 6;
    private static final int ECHELON_BRIGADE = 7;

    @TempDir
    Path temporaryDirectory;

    private Field rulesetTable;
    private Object previousRulesets;

    @BeforeEach
    void rememberRulesets() throws Exception {
        rulesetTable = Ruleset.class.getDeclaredField("rulesets");
        rulesetTable.setAccessible(true);
        previousRulesets = rulesetTable.get(null);
    }

    @AfterEach
    void restoreRulesets() throws Exception {
        rulesetTable.set(null, previousRulesets);
    }

    @Test
    void circinusUsesTheStarLeagueBaselineMenus() throws Exception {
        Ruleset circinus = ShippedRulesetLoader.load("CIR.xml", "CIR", "IS",
              Files.createDirectories(temporaryDirectory.resolve("CIR")));
        Ruleset innerSphere = ShippedRulesetLoader.load("IS.xml", "IS",
              Files.createDirectories(temporaryDirectory.resolve("IS")));
        HashMap<String, Ruleset> isolated = new HashMap<>();
        isolated.put("CIR", circinus);
        isolated.put("IS", innerSphere);
        rulesetTable.set(null, isolated);

        TOCNode menus = Ruleset.findTOCNode(circinus);

        assertSame(innerSphere.getTOCNode(), menus);
        ForceDescriptor request = new ForceDescriptor();
        request.setFaction("CIR");
        request.setYear(3060);
        ValueNode unitTypes = menus.findUnitTypes(request);
        assertNotNull(unitTypes);
        assertTrue(List.of(unitTypes.getContent().split(",")).contains("Conventional Fighter"),
              "the McIntyre Wings fly conventional fighters: " + unitTypes.getContent());

        request.setUnitType(UnitType.MEK);
        assertTrue(List.of(menus.findEchelons(request).getContent().split(","))
              .contains(Integer.toString(ECHELON_BATTALION)), "Majors command battalions");
    }

    @Test
    void theHouseGuardIsItsOrderOfBattle() throws Exception {
        Ruleset houseGuard = ShippedRulesetLoader.load("CIR.MHG.xml", "CIR.MHG", "CIR",
              Files.createDirectories(temporaryDirectory.resolve("MHG")));
        HashMap<String, Ruleset> isolated = new HashMap<>();
        isolated.put("CIR.MHG", houseGuard);
        rulesetTable.set(null, isolated);

        ForceDescriptor guard = new ForceDescriptor();
        guard.setFaction("CIR.MHG");
        guard.setYear(3060);
        guard.setEchelon(ECHELON_BRIGADE);
        assertNull(houseGuard.getDefaultUnitType(guard), "a combined-arms request must stay blank");
        ForceNode rule = houseGuard.findForceNode(guard);
        assertNotNull(rule, "no House Guard rule for a combined-arms request");

        rule.apply(guard);

        List<String> units = new ArrayList<>();
        for (ForceDescriptor unit : guard.getSubForces()) {
            units.add(unit.getName());
        }
        assertEquals(List.of("McIntyre Command Company", "McIntyre \"A\" Battalion", "McIntyre \"B\" Battalion",
              "McIntyre Wings", "McIntyre Armored Cavalry", "McIntyre Militia"), units);

        ForceDescriptor wings = guard.getSubForces().get(3);
        ForceNode wingsRule = houseGuard.findForceNode(wings);
        assertNotNull(wingsRule, "no rule for the McIntyre Wings");
        wingsRule.apply(wings);
        assertEquals(UnitType.AEROSPACE_FIGHTER, wings.getSubForces().get(0).getUnitType());
        assertEquals(UnitType.CONV_FIGHTER, wings.getSubForces().get(1).getUnitType(),
              "half the Wings fly conventional fighters");

        assertNull(houseGuard.findForceNode(mekCompany("CIR.MHG")),
              "the House Guard keeps support out of its companies, so it uses the plain IS company");
    }

    @Test
    void theBlackWarriorsFightInCombinedArmsCompanies() throws Exception {
        Ruleset blackWarriors = ShippedRulesetLoader.load("CIR.BW.xml", "CIR.BW", "CIR",
              Files.createDirectories(temporaryDirectory.resolve("BW")));
        HashMap<String, Ruleset> isolated = new HashMap<>();
        isolated.put("CIR.BW", blackWarriors);
        rulesetTable.set(null, isolated);

        ForceDescriptor company = mekCompany("CIR.BW");
        blackWarriors.findForceNode(company).apply(company);

        assertEquals(4, company.getSubForces().size(), "three 'Mek lances and a Black Horses lance");
        ForceDescriptor blackHorses = company.getSubForces().getLast();
        assertEquals("Black Horses Lance", blackHorses.getName());
        assertTrue((blackHorses.getUnitType() == UnitType.TANK) || (blackHorses.getUnitType() == UnitType.VTOL),
              "the Black Horses field hovertanks and VTOLs");

        ForceDescriptor command = new ForceDescriptor();
        command.setFaction("CIR.BW");
        command.setYear(3065);
        command.setEchelon(ECHELON_REGIMENT);
        assertNull(blackWarriors.getDefaultUnitType(command), "a combined-arms request must stay blank");
        ForceNode rule = blackWarriors.findForceNode(command);
        assertNotNull(rule, "no Black Warriors rule for a combined-arms request");
        rule.apply(command);
        List<String> units = new ArrayList<>();
        for (ForceDescriptor unit : command.getSubForces()) {
            units.add(unit.getName());
        }
        assertEquals(List.of("Command Lance", "Black Death", "Black Hearts", "Black Angels", "Black Dogs",
              "Black Dogs"), units);
    }

    private static ForceDescriptor mekCompany(String faction) {
        ForceDescriptor company = new ForceDescriptor();
        company.setFaction(faction);
        company.setYear(3065);
        company.setUnitType(UnitType.MEK);
        company.setEchelon(ECHELON_COMPANY);
        return company;
    }
}

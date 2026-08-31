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
package megamek.client.ui.dialogs.unitDisplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.util.List;

import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.dialogs.unitDisplay.WeaponTabPanel.HeatGauge;
import megamek.client.ui.dialogs.unitDisplay.WeaponTabPanel.RangeRibbon;
import megamek.client.ui.dialogs.unitDisplay.WeaponTabPanel.ToHitBreakdown;
import megamek.common.Player;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.game.Game;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.BipedMek;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Mek;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests the parts of the Weapon tab that are new: the heat gauge, the range ribbon, the to-hit breakdown and the
 * declared attack lines.
 */
@DisplayName("Weapon tab")
class WeaponTabPanelTest {

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    @BeforeAll
    static void requireDisplay() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipping GUI tests - no display available");
        EquipmentType.initializeTypes();
    }

    private static BipedMek createMek(Game game, Player owner, int id) {
        BipedMek mek = new BipedMek();
        mek.setGame(game);
        mek.setId(id);
        mek.setOwner(owner);
        mek.setChassis("Test Mek");
        mek.setModel("Standard");
        mek.setWeight(50);
        mek.setCrew(new Crew(CrewType.SINGLE));
        mek.autoSetInternal();
        for (int location = 0; location < mek.locations(); location++) {
            mek.initializeArmor(16, location);
        }
        return mek;
    }

    private static WeaponMounted arm(Mek mek, String weaponName) throws Exception {
        return (WeaponMounted) mek.addEquipment(EquipmentType.get(weaponName), Mek.LOC_RIGHT_ARM);
    }

    @Test
    @DisplayName("The heat gauge is coloured by the heat itself, as on the record sheet, not by the overflow")
    void heatGaugeColoursByAbsoluteHeat() {
        HeatGauge gauge = new HeatGauge();

        // 12 heat with 20 sinks: well under capacity, but 12 heat is already into the coloured bands
        gauge.setHeat(new HeatForecast.Result(12, 20, "20", false, false, false), "12 (20)", null);

        Color expected = GUIP.getColorForHeat(12, Color.MAGENTA);
        assertEquals(expected, gauge.getFill());
        assertNotEquals(GUIP.getColorForHeat(12 - 20, Color.MAGENTA), gauge.getFill(),
              "the classic panel coloured by heat over capacity, which is never positive here");
        assertEquals(12, gauge.getBuildup());
        assertEquals(20, gauge.getCapacity());
    }

    @Test
    @DisplayName("The range ribbon lights the bracket the target sits in, and the minimum range under it")
    void rangeRibbonLightsTheBracket() throws Exception {
        Game game = new Game();
        BipedMek mek = createMek(game, new Player(0, "Tester"), 1);
        WeaponMounted lrm = arm(mek, "ISLRM5");
        RangeRibbon ribbon = new RangeRibbon();

        ribbon.setWeapon(lrm, null);
        ribbon.setRange(4);
        assertEquals(0, ribbon.getLitBracket(), "4 hexes is inside an LRM's minimum range of 6");
        ribbon.setRange(7);
        assertEquals(1, ribbon.getLitBracket(), "short");
        ribbon.setRange(14);
        assertEquals(2, ribbon.getLitBracket(), "medium");
        ribbon.setRange(21);
        assertEquals(3, ribbon.getLitBracket(), "long");
        ribbon.setRange(40);
        assertEquals(-1, ribbon.getLitBracket(), "beyond every bracket");

        ribbon.setRange(WeaponTabPanel.NO_RANGE);
        assertEquals(-1, ribbon.getLitBracket(), "no target lights nothing");
        ribbon.setWeapon(null, null);
        ribbon.setRange(5);
        assertEquals(-1, ribbon.getLitBracket(), "no weapon lights nothing");
    }

    @Test
    @DisplayName("The to-hit breakdown lists every modifier on its own line under the total")
    void toHitBreakdownListsEachModifier() {
        ToHitBreakdown breakdown = new ToHitBreakdown();
        ToHitData toHit = new ToHitData(4, "gunnery skill");
        toHit.addModifier(1, "target has partial cover");
        toHit.addModifier(-1, "target is a large unit");

        breakdown.show(toHit, false);

        assertTrue(breakdown.getTotalText().contains("4"), "4 + 1 - 1 = 4: " + breakdown.getTotalText());
        assertEquals(List.of("+4 gunnery skill", "+1 target has partial cover", "-1 target is a large unit"),
              breakdown.getLines());
    }

    @Test
    @DisplayName("An impossible shot shows why, with no modifier lines")
    void impossibleShotShowsWhy() {
        ToHitBreakdown breakdown = new ToHitBreakdown();

        breakdown.show(new ToHitData(TargetRoll.IMPOSSIBLE, "no line of sight"), false);

        assertTrue(breakdown.getTotalText().contains("no line of sight"), breakdown.getTotalText());
        assertTrue(breakdown.getLines().isEmpty());

        breakdown.show("already fired");
        assertTrue(breakdown.getTotalText().contains("already fired"));
        breakdown.clear();
        assertTrue(breakdown.getTotalText().contains("---"));
    }

    @Test
    @DisplayName("A declared attack reads as the weapon and what it is aimed at")
    void declaredAttackNamesWeaponAndTarget() throws Exception {
        Game game = new Game();
        Player owner = new Player(0, "Tester");
        game.addPlayer(0, owner);
        BipedMek attacker = createMek(game, owner, 1);
        WeaponMounted laser = arm(attacker, "ISMediumLaser");
        BipedMek victim = createMek(game, owner, 2);
        victim.setChassis("Victim");
        game.addEntity(attacker);
        game.addEntity(victim);
        WeaponAttackAction attack = new WeaponAttackAction(attacker.getId(), victim.getId(),
              attacker.getEquipmentNum(laser));

        String line = WeaponTabPanel.describe(attack, game);

        assertTrue(line.contains(laser.getName()), line);
        assertTrue(line.contains(victim.getDisplayName()), line);
    }

    @Test
    @DisplayName("The tab shows a unit without a client, and starts with nothing declared")
    void tabShowsAUnitWithoutAClient() throws Exception {
        Game game = new Game();
        BipedMek mek = createMek(game, new Player(0, "Tester"), 1);
        arm(mek, "ISMediumLaser");
        mek.heat = 7;
        WeaponTabPanel tab = new WeaponTabPanel(new UnitDisplayPanel(null, null), null);

        tab.displayMek(mek);

        assertTrue(tab.getHeatGauge().getBuildup() >= 7, "the forecast starts from the heat the unit carries");
        assertEquals(List.of(), tab.getDeclaredAttackLines());
        assertFalse(tab.getRangeRibbon().getLitBracket() >= 0, "no target yet");
        tab.setToHit("no target");
        assertTrue(tab.getToHitBreakdown().getTotalText().contains("no target"));
    }
}

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
package megamek.client.bot.princess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.StringReader;
import java.util.List;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import megamek.client.bot.princess.commands.PriorityTargetCommand;
import megamek.common.Player;
import megamek.common.enums.GamePhase;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.common.util.SerializationHelper;
import megamek.server.commands.arguments.ArgumentsParser;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

/** Priority target levels, 1 the most wanted to 5 the least (issue #9174). */
class PriorityTargetLevelsTest {

    private static final double TOLERANCE = 0.0001;

    @Test
    void aTargetAddedWithoutAPriorityHasTheDefault() {
        BehaviorSettings settings = new BehaviorSettings();
        settings.addPriorityUnit(7);

        assertEquals(BehaviorSettings.DEFAULT_TARGET_PRIORITY, settings.getPriorityUnitLevel(7));
    }

    @Test
    void aUnitThatIsNoTargetHasPriorityZero() {
        assertEquals(0, new BehaviorSettings().getPriorityUnitLevel(7));
    }

    @Test
    void addingATargetAgainWithoutAPriorityKeepsItsPriority() {
        BehaviorSettings settings = new BehaviorSettings();
        settings.addPriorityUnit(7, 1);
        settings.addPriorityUnit(7);

        assertEquals(1, settings.getPriorityUnitLevel(7));
    }

    @Test
    void aPriorityOutsideOneToFiveMovesToTheNearestEnd() {
        BehaviorSettings settings = new BehaviorSettings();
        settings.addPriorityUnit(7, 0);
        settings.addPriorityUnit(8, 9);

        assertEquals(1, settings.getPriorityUnitLevel(7));
        assertEquals(5, settings.getPriorityUnitLevel(8));
    }

    @Test
    void removingATargetForgetsItsPriority() {
        BehaviorSettings settings = new BehaviorSettings();
        settings.addPriorityUnit(7, 1);
        settings.removePriorityUnit(7);
        settings.addPriorityUnit(7);

        assertEquals(BehaviorSettings.DEFAULT_TARGET_PRIORITY, settings.getPriorityUnitLevel(7));
    }

    @Test
    void aCopyKeepsThePriorities() throws PrincessException {
        BehaviorSettings settings = new BehaviorSettings();
        settings.addPriorityUnit(7, 1);
        settings.addPriorityUnit(8, 5);

        BehaviorSettings copy = settings.getCopy();

        assertEquals(1, copy.getPriorityUnitLevel(7));
        assertEquals(5, copy.getPriorityUnitLevel(8));
        assertEquals(settings, copy);
    }

    @Test
    void settingsDifferingOnlyInPriorityAreNotEqual() {
        BehaviorSettings first = new BehaviorSettings();
        first.addPriorityUnit(7, 1);
        BehaviorSettings second = new BehaviorSettings();
        second.addPriorityUnit(7, 2);

        assertNotEquals(first, second);
    }

    @Test
    void theSettingsXmlWritesEachTargetsPriority() throws Exception {
        BehaviorSettings settings = new BehaviorSettings();
        settings.addPriorityUnit(7, 1);
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

        Element saved = settings.toXml(documentBuilder.newDocument(), true);
        Element unitElement = (Element) saved.getElementsByTagName("unit").item(0);

        assertEquals("7", unitElement.getTextContent());
        assertEquals("1", unitElement.getAttribute("priority"));
    }

    @Test
    void theSettingsXmlReadsEachTargetsPriority() throws Exception {
        String xml = "<behavior><strategicTargets><unit priority=\"1\">7</unit><unit priority=\"5\">8</unit>"
              + "</strategicTargets></behavior>";

        BehaviorSettings loaded = new BehaviorSettings();
        loaded.fromXml(parse(xml));

        assertEquals(1, loaded.getPriorityUnitLevel(7));
        assertEquals(5, loaded.getPriorityUnitLevel(8));
    }

    @Test
    void aPresetSavedBeforePrioritiesLoadsItsTargetsAtTheDefault() throws Exception {
        String legacyXml = "<behavior><strategicTargets><unit>12</unit></strategicTargets></behavior>";

        BehaviorSettings loaded = new BehaviorSettings();
        loaded.fromXml(parse(legacyXml));

        assertEquals(Set.of(12), loaded.getPriorityUnitTargets());
        assertEquals(BehaviorSettings.DEFAULT_TARGET_PRIORITY, loaded.getPriorityUnitLevel(12));
    }

    @Test
    void prioritiesSurviveAGameSaveAndLoad() {
        BehaviorSettings settings = new BehaviorSettings();
        settings.addPriorityUnit(7, 1);

        String savedXml = SerializationHelper.getSaveGameXStream().toXML(settings);
        BehaviorSettings restored = (BehaviorSettings) SerializationHelper.getLoadSaveGameXStream().fromXML(savedXml);

        assertEquals(1, restored.getPriorityUnitLevel(7));
    }

    @Test
    void aGameSavedBeforePrioritiesLoadsAndTakesNewPriorities() {
        // Loading skips field initialisers, so a save without the element restores the map as null.
        BehaviorSettings settings = new BehaviorSettings();
        settings.addPriorityUnit(7);
        String legacyXml = SerializationHelper.getSaveGameXStream().toXML(settings)
              .replaceAll("(?s)<priorityUnitLevels[^>]*/>|<priorityUnitLevels.*?</priorityUnitLevels>", "");

        BehaviorSettings restored = (BehaviorSettings) SerializationHelper.getLoadSaveGameXStream().fromXML(legacyXml);
        restored.addPriorityUnit(8, 1);

        assertEquals(BehaviorSettings.DEFAULT_TARGET_PRIORITY, restored.getPriorityUnitLevel(7));
        assertEquals(1, restored.getPriorityUnitLevel(8));
    }

    @Test
    void theDefaultPriorityKeepsTodaysBonus() {
        assertEquals(FireControl.PRIORITY_TARGET_UTILITY,
              FireControl.priorityTargetUtility(BehaviorSettings.DEFAULT_TARGET_PRIORITY), TOLERANCE);
    }

    @Test
    void eachPriorityStepChangesTheBonusByATenth() {
        assertEquals(0.45, FireControl.priorityTargetUtility(1), TOLERANCE);
        assertEquals(0.35, FireControl.priorityTargetUtility(2), TOLERANCE);
        assertEquals(0.15, FireControl.priorityTargetUtility(4), TOLERANCE);
        assertEquals(0.05, FireControl.priorityTargetUtility(5), TOLERANCE);
    }

    @Test
    void aPriorityOutsideTheRangeGetsTheDefaultBonus() {
        assertEquals(FireControl.PRIORITY_TARGET_UTILITY, FireControl.priorityTargetUtility(0), TOLERANCE);
    }

    @Test
    void anAttackOnATopPriorityTargetGetsTheTopBonus() {
        Princess princess = mock(Princess.class);
        when(princess.getPriorityUnitTargets()).thenReturn(Set.of(12, 13));
        when(princess.getPriorityUnitLevel(12)).thenReturn(1);
        when(princess.getPriorityUnitLevel(13)).thenReturn(BehaviorSettings.DEFAULT_TARGET_PRIORITY);
        FireControl fireControl = new FireControl(princess);
        Entity topTarget = mock(Entity.class);
        when(topTarget.getId()).thenReturn(12);
        Entity defaultTarget = mock(Entity.class);
        when(defaultTarget.getId()).thenReturn(13);
        Entity otherUnit = mock(Entity.class);
        when(otherUnit.getId()).thenReturn(14);

        assertEquals(0.45, fireControl.calcPriorityUnitTargetUtility(topTarget), TOLERANCE);
        assertEquals(0.25, fireControl.calcPriorityUnitTargetUtility(defaultTarget), TOLERANCE);
        assertEquals(0, fireControl.calcPriorityUnitTargetUtility(otherUnit), TOLERANCE);
    }

    @Test
    void anUnarmedPriorityTargetCountsAsTheMostDangerousEnemy() {
        Entity truck = unit(3);
        Entity escort = unit(10);
        FireControl fireControl = fireControlAgainst(Set.of(3), truck, escort);
        doReturn(1.0).when(fireControl).calcTargetPotentialDamageMultiplier(truck);
        doReturn(1.85).when(fireControl).calcTargetPotentialDamageMultiplier(escort);

        assertEquals(1.85, fireControl.calcThreatMultiplier(truck), TOLERANCE);
        assertEquals(1.85, fireControl.calcThreatMultiplier(escort), TOLERANCE);
    }

    @Test
    void aTargetThatIsNoPriorityKeepsItsOwnThreat() {
        Entity truck = unit(3);
        Entity escort = unit(10);
        FireControl fireControl = fireControlAgainst(Set.of(), truck, escort);
        doReturn(1.0).when(fireControl).calcTargetPotentialDamageMultiplier(truck);
        doReturn(1.85).when(fireControl).calcTargetPotentialDamageMultiplier(escort);

        assertEquals(1.0, fireControl.calcThreatMultiplier(truck), TOLERANCE);
    }

    @Test
    void aPriorityTargetPaysNoOverkillCostUntilItIsAlreadyDoomed() {
        Entity truck = unit(3);
        Entity otherTruck = unit(4);
        FireControl fireControl = fireControlAgainst(Set.of(3), truck, otherTruck);
        doReturn(0.7).when(fireControl).calcDamageAllocationUtility(truck, 36.0);
        doReturn(0.7).when(fireControl).calcDamageAllocationUtility(otherTruck, 36.0);
        doReturn(100.0).when(fireControl).calcDamageAllocationUtility(truck, 5.0);

        assertEquals(0, fireControl.calcOverkillFraction(truck, 36.0), TOLERANCE);
        assertEquals(0.7, fireControl.calcOverkillFraction(otherTruck, 36.0), TOLERANCE, "not a priority target");
        assertEquals(100, fireControl.calcOverkillFraction(truck, 5.0), TOLERANCE, "already expected destroyed");
    }

    @Test
    void aPriorityOneTruckOutscoresTheArmedEscortNextToIt() {
        // HammerGS's 2026-10-04 playtest, round 7: a Masakari with a truck 11 hexes off and a Phoenix Hawk 5 hexes
        // off shot the Phoenix Hawk every time, because the escort's threat outweighed the truck's priority bonus.
        Entity truck = unit(3);
        Entity escort = unit(10);
        when(escort.isMilitary()).thenReturn(true);
        FireControl fireControl = fireControlAgainst(Set.of(3), truck, escort);
        when(fireControl.owner.getPriorityUnitLevel(3)).thenReturn(1);
        doReturn(1.0).when(fireControl).calcTargetPotentialDamageMultiplier(truck);
        doReturn(1.85).when(fireControl).calcTargetPotentialDamageMultiplier(escort);
        doReturn(0.0).when(fireControl).calcCommandUtility(any());
        doReturn(0.0).when(fireControl).calcDamageAllocationUtility(any(), anyDouble());
        FiringPlan truckPlan = plan(truck, 18.0, 0.0, 0.15);
        FiringPlan escortPlan = plan(escort, 26.0, 0.4, 0.0);

        fireControl.calculateUtility(truckPlan, 99, false);
        fireControl.calculateUtility(escortPlan, 99, false);

        double truckScore = capturedUtility(truckPlan);
        double escortScore = capturedUtility(escortPlan);
        assertEquals(68.4, truckScore, 0.1);
        assertEquals(55.5, escortScore, 0.1);
    }

    private static Entity unit(int id) {
        Entity unit = mock(Entity.class);
        when(unit.getId()).thenReturn(id);
        return unit;
    }

    private static FireControl fireControlAgainst(Set<Integer> priorityTargets, Entity... enemies) {
        Princess princess = mock(Princess.class);
        Game game = mock(Game.class);
        when(game.getRoundCount()).thenReturn(7);
        when(game.getPhase()).thenReturn(GamePhase.FIRING);
        when(princess.getGame()).thenReturn(game);
        when(princess.getPriorityUnitTargets()).thenReturn(priorityTargets);
        when(princess.getEnemyEntities()).thenReturn(List.of(enemies));
        return spy(new FireControl(princess));
    }

    private static FiringPlan plan(Entity target, double expectedDamage, double expectedCriticals,
          double killProbability) {
        FiringPlan plan = mock(FiringPlan.class);
        when(plan.getTarget()).thenReturn(target);
        when(plan.getExpectedDamage()).thenReturn(expectedDamage);
        when(plan.getExpectedCriticals()).thenReturn(expectedCriticals);
        when(plan.getKillProbability()).thenReturn(killProbability);
        return plan;
    }

    private static double capturedUtility(FiringPlan plan) {
        ArgumentCaptor<Double> utility = ArgumentCaptor.forClass(Double.class);
        verify(plan).setUtility(utility.capture());
        return utility.getValue();
    }

    @Test
    void theChatCommandSetsTheGivenPriority() {
        Princess princess = princessWith(new BehaviorSettings());
        PriorityTargetCommand command = new PriorityTargetCommand();

        command.execute(princess, ArgumentsParser.parse(new String[] { "pr", "12", "1" }, command.defineArguments()));

        assertEquals(1, princess.getBehaviorSettings().getPriorityUnitLevel(12));
    }

    @Test
    void theChatCommandWithoutAPriorityUsesTheDefault() {
        Princess princess = princessWith(new BehaviorSettings());
        PriorityTargetCommand command = new PriorityTargetCommand();

        command.execute(princess, ArgumentsParser.parse(new String[] { "pr", "12" }, command.defineArguments()));

        assertEquals(BehaviorSettings.DEFAULT_TARGET_PRIORITY,
              princess.getBehaviorSettings().getPriorityUnitLevel(12));
    }

    private static Element parse(String xml) throws Exception {
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        return documentBuilder.parse(new InputSource(new StringReader(xml))).getDocumentElement();
    }

    private static Princess princessWith(BehaviorSettings settings) {
        Princess princess = mock(Princess.class);
        when(princess.getBehaviorSettings()).thenReturn(settings);
        when(princess.getLocalPlayer()).thenReturn(new Player(1, "Princess"));
        return princess;
    }
}

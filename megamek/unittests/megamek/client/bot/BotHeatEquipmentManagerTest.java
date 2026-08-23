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
package megamek.client.bot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Vector;

import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import megamek.common.game.Game;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import megamek.common.weapons.Weapon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link BotHeatEquipmentManager}, which switches a bot's heat-generating equipment off when its
 * units run hot and back on once they cool. See MegaMek/megamek#8802.
 *
 * @author The MegaMek Team
 */
class BotHeatEquipmentManagerTest {

    private static final int BOT_PLAYER_ID = 1;
    private static final int EQUIPMENT_NUMBER = 3;

    private BotClient mockBotClient;
    private Game mockGame;
    private BotHeatEquipmentManager heatEquipmentManager;

    @BeforeEach
    void beforeEach() {
        mockGame = mock(Game.class);
        mockBotClient = mock(BotClient.class);
        when(mockBotClient.getGame()).thenReturn(mockGame);
        when(mockBotClient.getLocalPlayerNumber()).thenReturn(BOT_PLAYER_ID);
        heatEquipmentManager = new BotHeatEquipmentManager(mockBotClient);
    }

    /**
     * A heat-tracking Mek owned by the bot, carrying one operable piece of equipment with the given flag,
     * currently switched on or off, with no stealth armor and no radical heat sink.
     */
    private BipedMek mekCarrying(megamek.common.equipment.enums.MiscTypeFlag equipmentFlag,
          int heat, boolean currentlyOn) {
        MiscType mockType = mock(MiscType.class);
        when(mockType.hasFlag(equipmentFlag)).thenReturn(true);
        when(mockType.getName()).thenReturn("Test Concealment System");

        MiscMounted mockEquipment = mock(MiscMounted.class);
        when(mockEquipment.getType()).thenReturn(mockType);
        when(mockEquipment.isOperable()).thenReturn(true);
        when(mockEquipment.isModeTurnedOff()).thenReturn(!currentlyOn);
        when(mockEquipment.setMode(Weapon.MODE_AMS_ON)).thenReturn(1);
        when(mockEquipment.setMode(Weapon.MODE_AMS_OFF)).thenReturn(0);

        BipedMek mockMek = mock(BipedMek.class);
        when(mockMek.getOwnerId()).thenReturn(BOT_PLAYER_ID);
        when(mockMek.getId()).thenReturn(42);
        when(mockMek.getShortName()).thenReturn("Test Mek");
        when(mockMek.tracksHeat()).thenReturn(true);
        when(mockMek.getHeat()).thenReturn(heat);
        when(mockMek.hasStealth()).thenReturn(false);
        when(mockMek.hasWorkingRadicalHS()).thenReturn(false);
        when(mockMek.getMisc()).thenReturn(List.of(mockEquipment));
        when(mockMek.getEquipmentNum(mockEquipment)).thenReturn(EQUIPMENT_NUMBER);

        when(mockGame.getEntitiesVector()).thenReturn(new Vector<>(List.of(mockMek)));
        return mockMek;
    }

    private MiscMounted equipmentOf(Entity entity) {
        return (MiscMounted) entity.getMisc().get(0);
    }

    @Test
    void voidSignatureIsShedWhenTheMekRunsHot() {
        // 14 heat is where shutdown rolls begin, so 10 points a turn is no longer worth what it buys.
        BipedMek mockMek = mekCarrying(MiscType.F_VOID_SIG, 14, true);
        when(mockMek.isVoidSigOn()).thenReturn(true);

        heatEquipmentManager.manageOwnedUnits();

        verify(equipmentOf(mockMek)).setMode(Weapon.MODE_AMS_OFF);
        verify(mockBotClient).sendModeChange(42, EQUIPMENT_NUMBER, 0);
    }

    @Test
    void voidSignatureComesBackOnOnceTheMekHasCooled() {
        BipedMek mockMek = mekCarrying(MiscType.F_VOID_SIG, 5, false);

        heatEquipmentManager.manageOwnedUnits();

        verify(equipmentOf(mockMek)).setMode(Weapon.MODE_AMS_ON);
        verify(mockBotClient).sendModeChange(42, EQUIPMENT_NUMBER, 1);
    }

    @Test
    void nothingHappensBetweenTheTwoThresholds() {
        // The gap between shedding at 14 and restoring at 5 is what stops a Mek hovering near the shed
        // point from switching the same system on and off every turn.
        BipedMek mockMek = mekCarrying(MiscType.F_VOID_SIG, 9, false);

        heatEquipmentManager.manageOwnedUnits();

        verify(equipmentOf(mockMek), never()).setMode(Weapon.MODE_AMS_ON);
        verify(equipmentOf(mockMek), never()).setMode(Weapon.MODE_AMS_OFF);
        verify(mockBotClient, never()).sendModeChange(anyInt(), anyInt(), anyInt());
    }

    @Test
    void nullSignatureIsShedWhenTheMekRunsHot() {
        BipedMek mockMek = mekCarrying(MiscType.F_NULL_SIG, 20, true);
        when(mockMek.isNullSigOn()).thenReturn(true);

        heatEquipmentManager.manageOwnedUnits();

        verify(equipmentOf(mockMek)).setMode(Weapon.MODE_AMS_OFF);
    }

    @Test
    void chameleonShieldIsShedWhenTheMekRunsHot() {
        BipedMek mockMek = mekCarrying(MiscType.F_CHAMELEON_SHIELD, 18, true);
        when(mockMek.isChameleonShieldOn()).thenReturn(true);

        heatEquipmentManager.manageOwnedUnits();

        verify(equipmentOf(mockMek)).setMode(Weapon.MODE_AMS_OFF);
    }

    @Test
    void novaCewsWaitsUntilConcealmentHasAlreadyGone() {
        // Switching a Nova off costs the unit its network link, and at 2 heat a turn it buys much less
        // relief than a concealment system, so it is the second thing to go rather than the first.
        BipedMek mockMek = mekCarrying(MiscType.F_NOVA, 20, true);
        when(mockMek.isVoidSigOn()).thenReturn(true);

        heatEquipmentManager.manageOwnedUnits();

        verify(equipmentOf(mockMek), never()).setMode(Weapon.MODE_AMS_OFF);
    }

    @Test
    void novaCewsIsShedOnceNothingCheaperIsLeft() {
        BipedMek mockMek = mekCarrying(MiscType.F_NOVA, 20, true);

        heatEquipmentManager.manageOwnedUnits();

        verify(equipmentOf(mockMek)).setMode(Weapon.MODE_AMS_OFF);
    }

    @Test
    void equipmentOnAUnitThatIgnoresHeatIsLeftSwitchedOn() {
        BipedMek mockMek = mekCarrying(MiscType.F_VOID_SIG, 0, true);
        when(mockMek.tracksHeat()).thenReturn(false);

        heatEquipmentManager.manageOwnedUnits();

        verify(equipmentOf(mockMek), never()).setMode(Weapon.MODE_AMS_OFF);
    }

    @Test
    void unitsBelongingToOtherPlayersAreLeftAlone() {
        BipedMek mockMek = mekCarrying(MiscType.F_VOID_SIG, 25, true);
        when(mockMek.getOwnerId()).thenReturn(BOT_PLAYER_ID + 1);

        heatEquipmentManager.manageOwnedUnits();

        verify(mockBotClient, never()).sendModeChange(anyInt(), anyInt(), anyInt());
    }

    @Test
    void theOddsTableStopsAtThreeConsecutiveRadicalHeatSinkUses() {
        // IO p.89: 3+, 5+, 7+, then 10+ and 11+. Better than even odds runs out after the third use, and
        // failure destroys the system permanently, so that is where the bot stops gambling.
        assertTrue(radicalTargetFor(1) <= BotHeatEquipmentManager.BEST_ODDS_TARGET_NUMBER);
        assertTrue(radicalTargetFor(2) <= BotHeatEquipmentManager.BEST_ODDS_TARGET_NUMBER);
        assertTrue(radicalTargetFor(3) <= BotHeatEquipmentManager.BEST_ODDS_TARGET_NUMBER);
        assertFalse(radicalTargetFor(4) <= BotHeatEquipmentManager.BEST_ODDS_TARGET_NUMBER);
        assertFalse(radicalTargetFor(5) <= BotHeatEquipmentManager.BEST_ODDS_TARGET_NUMBER);
        assertFalse(radicalTargetFor(6) <= BotHeatEquipmentManager.BEST_ODDS_TARGET_NUMBER);
    }

    private int radicalTargetFor(int consecutiveUses) {
        return Game.rulesManager.getRulesEquipment().radicalHeatSinkSuccessTarget(consecutiveUses);
    }

    @Test
    void theRestoreThresholdSitsBelowTheShedThreshold() {
        // If these ever met, a unit parked on the boundary would flap on and off every single turn.
        assertTrue(BotHeatEquipmentManager.RESTORE_HEAT_THRESHOLD
              < BotHeatEquipmentManager.SHED_HEAT_THRESHOLD);
        assertEquals(14, BotHeatEquipmentManager.SHED_HEAT_THRESHOLD);
    }
}

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
package megamek.common.actions.compute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.GameBoardTestCase;
import megamek.common.Player;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Coords;
import megamek.common.enums.AimingMode;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.options.GameOptions;
import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;
import megamek.common.units.BipedMek;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Fire between levels of one building uses the Mek hit location tables for attacks from above and below (TW p.
 * 175). The tables switch on the levels the two units stand on.
 */
@DisplayName("Hit tables between levels inside a building")
class HitTablesInsideBuildingTest extends GameBoardTestCase {

    /** Two adjacent hexes of one heavy building, three levels high; exits on every side so the hexes join. */
    static {
        initializeBoard("BOARD_TWO_HEX_HEAVY_BUILDING", """
              size 2 1
              hex 0101 0 "bldg_elev:3;building:3:63;bldg_cf:90" ""
              hex 0201 0 "bldg_elev:3;building:3:63;bldg_cf:90" ""
              end""");
    }

    private static final Coords HEX_A = new Coords(0, 0);
    private static final Coords HEX_B = new Coords(1, 0);

    private static WeaponType mediumLaserType;
    private static Player player1;
    private static Player player2;

    private Game game;
    private Mek attacker;
    private WeaponMounted laser;
    private Mek target;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
        mediumLaserType = (WeaponType) EquipmentType.get("ISMediumLaser");
        player1 = new Player(0, "Attacker");
        player2 = new Player(1, "Target");
    }

    @BeforeEach
    void beforeEach() throws Exception {
        setBoard("BOARD_TWO_HEX_HEAVY_BUILDING");
        game = getGame();
        GameOptions options = mock(GameOptions.class);
        when(options.booleanOption(anyString())).thenReturn(false);
        when(options.stringOption(OptionsConstants.ALLOWED_TECH_LEVEL)).thenReturn("Experimental");
        when(options.intOption(OptionsConstants.ALLOWED_YEAR)).thenReturn(3151);
        IOption rulesOption = mock(IOption.class);
        when(rulesOption.booleanValue()).thenReturn(false);
        when(rulesOption.stringValue()).thenReturn(OptionsConstants.RULES_CORE);
        when(options.getOption(anyString())).thenReturn(rulesOption);
        game.setOptions(options);
        game.addPlayer(0, player1);
        game.addPlayer(1, player2);
        game.initializeRulesManager(OptionsConstants.RULES_CORE);

        attacker = mek("Attacker", player1, 1, HEX_A);
        laser = (WeaponMounted) attacker.addEquipment(mediumLaserType, Mek.LOC_CENTER_TORSO);
        target = mek("Target", player2, 2, HEX_B);
    }

    private Mek mek(String chassis, Player owner, int id, Coords position) {
        Mek mek = new BipedMek();
        mek.setGame(game);
        mek.setChassis(chassis);
        mek.setModel("TST-1");
        mek.setCrew(new Crew(CrewType.SINGLE));
        mek.setOwnerId(owner.getId());
        mek.setId(id);
        game.addEntity(mek);
        mek.setPosition(position);
        return mek;
    }

    private ToHitData fire(int attackerLevel, int targetLevel) {
        attacker.setElevation(attackerLevel);
        target.setElevation(targetLevel);
        attacker.setFacing(HEX_A.direction(HEX_B));
        attacker.setSecondaryFacing(attacker.getFacing());
        return ComputeToHit.toHitCalc(game, attacker.getId(), target, attacker.getEquipmentNum(laser),
              Entity.LOC_NONE, AimingMode.NONE, false, false, null, null, false, false, null, false,
              WeaponAttackAction.UNASSIGNED, WeaponAttackAction.UNASSIGNED);
    }

    @Test
    @DisplayName("Firing down one level inside the building uses the Above table")
    void firingDownUsesTheAboveTable() {
        assertEquals(ToHitData.HIT_ABOVE, fire(1, 0).getHitTable());
    }

    @Test
    @DisplayName("Firing up one level inside the building uses the Below table")
    void firingUpUsesTheBelowTable() {
        assertEquals(ToHitData.HIT_BELOW, fire(0, 1).getHitTable());
    }

    @Test
    @DisplayName("Firing across the same level uses the normal table")
    void firingAcrossTheSameLevelUsesTheNormalTable() {
        assertEquals(ToHitData.HIT_NORMAL, fire(1, 1).getHitTable());
    }


}

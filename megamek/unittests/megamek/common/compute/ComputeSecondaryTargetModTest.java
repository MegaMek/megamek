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
package megamek.common.compute;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import megamek.common.CalledShot;
import megamek.common.Player;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Coords;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.exceptions.LocationFullException;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;
import megamek.common.units.BipedMek;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ComputeSecondaryTargetModTest {

    private Game testGame = new Game();
    private List<Entity> gameEntities = new ArrayList<>();
    private Crew mockCrew;
    private AmmoMounted mockAmmo;
    private WeaponType mockWeaponType;
    private WeaponMounted mockWeapon;
    private WeaponMounted mockWeaponEquipment;
    private CrewType mockCrewType;
    private PilotOptions mockPilotOptions;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        Player mockPlayer = mock(Player.class);

        Mek attacker = createMek(1, new Coords(0, 3));
        attacker.setFacing(2);
        Mek targetFront = createMek(2, new Coords(0, 5));
        Mek targetFrontTwo = createMek(3, new Coords(1, 4));
        Mek targetSecondaryArc = createMek(4, new Coords(1, 2));
        testGame.addEntities(gameEntities);
        // Mock Ammo
        mockAmmo = mock(AmmoMounted.class);
        when(mockAmmo.canFire()).thenReturn(true);
        when(mockAmmo.hasUsableShotsLeft()).thenReturn(true);
        when(mockAmmo.getUsableShotsLeft()).thenReturn(10);

        // Mock Weapon Type
        mockWeaponType = mock(WeaponType.class);
        when(mockWeaponType.getName()).thenReturn("Mock Weapon Type");
        when(mockWeaponType.getInternalName()).thenReturn("Mock Internal Weapon Type");
        when(mockWeaponType.getDamage()).thenReturn(5);
        mockWeaponType.setShortRange(3);
        mockWeaponType.setMediumRange(10);
        mockWeaponType.setLongRange(20);
        when(mockWeaponType.getMaxRange()).thenReturn(20);
        when(mockWeaponType.getMaxRange(any(), any())).thenReturn(20);
        when(mockWeaponType.getRanges(any(), any())).thenReturn(new int[] { 0, 3, 10, 20, 20 });
        when(mockWeaponType.getWRanges()).thenReturn(new int[] { 0, 3, 10, 20, 20 });
        when(mockWeaponType.getATRanges()).thenReturn(new int[] { 0, 3, 10, 20, 20 });

        // Mock Weapon
        mockWeapon = mock(WeaponMounted.class);
        mockWeaponEquipment = mockWeapon;
        when(mockWeapon.getType()).thenReturn(mockWeaponType);
        when(mockWeapon.getLinkedAmmo()).thenReturn(mockAmmo);
        when(mockWeapon.canFire()).thenReturn(true);
        when(mockWeapon.canFire(anyBoolean(), anyBoolean())).thenReturn(true);
        when(mockWeapon.getCalledShot()).thenReturn(new CalledShot());

        // Mock Crew Type
        mockCrewType = mock(CrewType.class);
        when(mockCrewType.getMaxPrimaryTargets()).thenReturn(0);

        // Mock Pilot Options
        mockPilotOptions = mock(PilotOptions.class);
        when(mockPilotOptions.stringOption(anyString())).thenReturn("mock");

        // Mock Crew
        mockCrew = mock(Crew.class);
        when(mockCrew.isActive()).thenReturn(true);
        when(mockCrew.getCrewType()).thenReturn(mockCrewType);
        when(mockCrew.getOptions()).thenReturn(mockPilotOptions);
        testGame.addAction(new WeaponAttackAction(1, 2, 1));
        testGame.addAction(new WeaponAttackAction(1, 2, 2));
        testGame.addAction(new WeaponAttackAction(1, 3, 3));
        testGame.addAction(new WeaponAttackAction(1, 4, 4));
    }

    private Mek createMek(int id, Coords position) {
        Mek mek = new BipedMek();
        mek.setGame(testGame);
        mek.setId(id);
        mek.setPosition(position);
        mek.setDeployed(true);
        gameEntities.add(mek);
        return mek;
    }

    @Test
    void secondaryTargetModifierWithCore() {
        testGame.initializeRulesManager(OptionsConstants.RULES_CORE);
        ToHitData toHit = Compute.getSecondaryTargetMod(testGame, gameEntities.get(0), gameEntities.get(2));
        assertSame(toHit.getValue(), 1);
    }

    @Test
    void secondaryTargetModifierWithTW() throws LocationFullException {
        testGame.initializeRulesManager(OptionsConstants.RULES_TW);
        ToHitData toHit = Compute.getSecondaryTargetMod(testGame, gameEntities.get(0), gameEntities.get(2));
        assertSame(toHit.getValue(), 1);
    }

    @Test
    void secondaryArcTargetModifierWithCore() throws LocationFullException {
        testGame.initializeRulesManager(OptionsConstants.RULES_CORE);
        ToHitData toHit = Compute.getSecondaryTargetMod(testGame, gameEntities.get(0), gameEntities.get(3));
        assertSame(toHit.getValue(), 1);
    }

    @Test
    void secondaryArcTargetModifierWithTW() throws LocationFullException {
        testGame.initializeRulesManager(OptionsConstants.RULES_TW);
        ToHitData toHit = Compute.getSecondaryTargetMod(testGame, gameEntities.get(0), gameEntities.get(3));
        assertSame(toHit.getValue(), 2);
    }
}

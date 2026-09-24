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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
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
    private Entity attacker;
    private Crew mockCrew;
    private CrewType mockCrewType;
    private PilotOptions mockPilotOptions;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        attacker = createMek(1, new Coords(0, 3));
        attacker.setFacing(2);
        attacker.setSecondaryFacing(2, false);
        Mek targetFront = createMek(2, new Coords(1, 3));
        Mek targetFrontTwo = createMek(3, new Coords(1, 4));
        Mek targetSecondaryArc = createMek(4, new Coords(-1, 2));
        testGame.addEntities(gameEntities);

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
        testGame.addAction(new WeaponAttackAction(1, 1, 1));
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

    private boolean inForwardArc(Entity target) {
        return ComputeArc.isInArc(attacker.getPosition(),
              attacker.getSecondaryFacing(),
              target,
              attacker.getForwardArc());
    }

    @Test
    void boardLayoutPutsTargetsInTheExpectedArcs() {
        assertEquals(2, attacker.getSecondaryFacing(), "attacker secondary facing");
        assertTrue(inForwardArc(testGame.getEntity(2)), "primary target should be in the forward arc");
        assertTrue(inForwardArc(testGame.getEntity(3)), "secondaryInArc should be in the forward arc");
        assertFalse(inForwardArc(testGame.getEntity(4)), "secondaryOutsideArc should be outside the forward arc");
    }

    @Test
    void secondaryTargetModifierWithCore() {
        testGame.initializeRulesManager(OptionsConstants.RULES_CORE);
        ToHitData toHit = Compute.getSecondaryTargetMod(testGame, gameEntities.get(0), gameEntities.get(2));
        assertNotNull(toHit, "Expected a secondary target modifier for entity " + gameEntities.get(2).getId()
              + "; a null means the method treated it as the primary target instead");
        assertEquals(1, toHit.getValue());
    }

    @Test
    void secondaryTargetModifierWithTW() throws LocationFullException {
        testGame.initializeRulesManager(OptionsConstants.RULES_TW);
        ToHitData toHit = Compute.getSecondaryTargetMod(testGame, gameEntities.get(0), gameEntities.get(2));
        assertEquals(1, toHit.getValue());
    }

    @Test
    void secondaryArcTargetModifierWithCore() throws LocationFullException {
        testGame.initializeRulesManager(OptionsConstants.RULES_CORE);
        ToHitData toHit = Compute.getSecondaryTargetMod(testGame, gameEntities.get(0), gameEntities.get(3));
        assertEquals(1, toHit.getValue());
    }

    @Test
    void secondaryArcTargetModifierWithTW() throws LocationFullException {
        testGame.initializeRulesManager(OptionsConstants.RULES_TW);
        ToHitData toHit = Compute.getSecondaryTargetMod(testGame, gameEntities.get(0), gameEntities.get(3));
        assertEquals(2, toHit.getValue());
    }
}

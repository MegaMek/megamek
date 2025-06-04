/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui;

import static megamek.client.ui.SharedUtility.predictLeapDamage;
import static megamek.client.ui.SharedUtility.predictLeapFallDamage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.client.Client;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.common.*;
import megamek.common.options.GameOptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SharedUtilityTest {
    static GameOptions mockGameOptions = mock(GameOptions.class);
    static ClientGUI cg = mock(ClientGUI.class);
    static Client client = mock(Client.class);
    static Game game = new Game();

    static Mek bipedMek;
    static Crew bipedPilot;
    static Mek quadMek;
    static Crew quadPilot;
    static EntityMovementType moveType = EntityMovementType.MOVE_RUN;

    @BeforeAll
    static void setUpAll() {
        // Need equipment initialized
        EquipmentType.initializeTypes();
        when(cg.getClient()).thenReturn(client);
        when(cg.getClient().getGame()).thenReturn(game);
        game.setOptions(mockGameOptions);

        bipedMek = new BipedMek();
        bipedMek.setGame(game);
        bipedPilot = new Crew(CrewType.SINGLE);
        quadMek = new QuadMek();
        quadMek.setGame(game);
        quadPilot = new Crew(CrewType.SINGLE);
        bipedPilot.setPiloting(5, bipedPilot.getCrewType().getPilotPos());
        bipedMek.setCrew(bipedPilot);
        bipedMek.setId(1);
        quadPilot.setPiloting(5, quadPilot.getCrewType().getPilotPos());
        quadMek.setCrew(quadPilot);
        quadMek.setId(2);
    }

    @BeforeEach
    void setUp() {
        bipedMek.setArmor(10, Mek.LOC_LLEG);
        bipedMek.setArmor(10, Mek.LOC_RLEG);
        bipedMek.setWeight(50.0);

        quadMek.setArmor(10, Mek.LOC_LLEG);
        quadMek.setArmor(10, Mek.LOC_RLEG);
        quadMek.setArmor(10, Mek.LOC_LARM);
        quadMek.setArmor(10, Mek.LOC_RARM);
        quadMek.setWeight(85.0);
    }

    TargetRoll generateLeapRoll(Entity entity, int leapDistance) {
        TargetRoll rollTarget = entity.getBasePilotingRoll(moveType);
        rollTarget.append(new PilotingRollData(entity.getId(),
              2 * leapDistance,
              Messages.getString("TacOps.leaping.leg_damage")));
        return rollTarget;
    }

    TargetRoll generateLeapFallRoll(Entity entity, int leapDistance) {
        TargetRoll rollTarget = entity.getBasePilotingRoll(moveType);
        rollTarget.append(new PilotingRollData(entity.getId(),
              leapDistance,
              Messages.getString("TacOps.leaping.fall_damage")));
        return rollTarget;
    }

    @Test
    void testPredictLeapDamageBipedLeap3AvgPilot() {
        TargetRoll data = generateLeapRoll(bipedMek, 3);

        // Rough math:
        // Chance of Pilot Skill 5 pilot to successfully Leap down 3 levels:
        // 1 / 12 = 8.3% = 0.083
        // Base biped mek damage from leap 3:
        // + 2 x 3 = 6
        // Estimate each crit chance expected damage at 100
        // + 2 x 100 = 200
        // Chance of extra crits if leg armor is greater than base damage is 0:
        // + 0 * 100 = 0
        // Times chance of this happening
        // * (1 - 0.083)
        // Approximately 188 damage expected.
        double expectedDamage = 188.0;
        double predictedDamage = predictLeapDamage(bipedMek, data);

        assertEquals(expectedDamage, predictedDamage, 1.0);
    }

    @Test
    void testPredictLeapDamageQuadLeap3AvgPilot() {
        TargetRoll data = generateLeapRoll(quadMek, 3);

        // Rough math:
        // Chance of Pilot Skill 5 pilot to successfully Leap down 3 levels:
        // 3 / 12 = 0.277
        // Base biped mek damage from leap 3:
        // + 4 x 3 = 12
        // Estimate each crit chance expected damage at 100
        // + 4 x 100 = 400
        // Chance of extra crits if leg armor is greater than base damage is 0:
        // + 0 * 100 = 0
        // Times chance of this happening
        // * (1 - 0.277)
        // Approximately 298 damage expected.
        double expectedDamage = 298.0;
        double predictedDamage = predictLeapDamage(quadMek, data);

        assertEquals(expectedDamage, predictedDamage, 1.0);
    }

    @Test
    void testPredictLeapFallDamageBipedLeap3AvgPilot() {
        TargetRoll data = generateLeapFallRoll(bipedMek, 3);

        double expectedDamage = 12.0;
        double predictedDamage = predictLeapFallDamage(bipedMek, data);

        assertEquals(expectedDamage, predictedDamage, 1.0);
    }

    @Test
    void testPredictLeapFallDamageQuadLeap3AvgPilot() {
        TargetRoll data = generateLeapFallRoll(quadMek, 3);

        double expectedDamage = 10.0;
        double predictedDamage = predictLeapFallDamage(quadMek, data);

        assertEquals(expectedDamage, predictedDamage, 1.0);
    }
}

/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 *  This file is part of MegaMek.
 *
 *  MekHQ is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MekHQ is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui;

import megamek.client.Client;
import megamek.client.ui.swing.ClientGUI;
import megamek.common.*;
import megamek.common.options.GameOptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static megamek.client.ui.SharedUtility.predictLeapDamage;
import static megamek.client.ui.SharedUtility.predictLeapFallDamage;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        bipedPilot.setPiloting(5);
        bipedMek.setCrew(bipedPilot);
        bipedMek.setId(1);
        quadPilot.setPiloting(5);
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
            2 * leapDistance, Messages.getString("TacOps.leaping.leg_damage")));
        return rollTarget;
    }

    TargetRoll generateLeapFallRoll(Entity entity, int leapDistance) {
        TargetRoll rollTarget = entity.getBasePilotingRoll(moveType);
        rollTarget.append(new PilotingRollData(entity.getId(),
            leapDistance, Messages.getString("TacOps.leaping.fall_damage")));
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

/*
  Copyright (C) 2003, 2004 Ben Mazur (bmazur@sev.org)
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

package megamek.common.interfaces;

import java.io.Serializable;
import java.util.Map;

import megamek.common.Team;
import megamek.common.game.IGame;
import megamek.common.game.InitiativeRoll;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 8/17/13 10:21 AM
 */
public interface ITurnOrdered extends Serializable {
    /**
     * Return the number of "normal" turns that this item requires. This is normally the sum of multi-unit turns and the
     * other turns. <p> Subclasses are expected to override this value in order to make the "move even" code work
     * correctly.
     *
     * @return the <code>int</code> number of "normal" turns this item should take in a phase.
     */
    int getNormalTurns(IGame game);

    int getOtherTurns();

    int getEvenTurns();

    int getMultiTurns(IGame game);

    int getSpaceStationTurns();

    int getJumpshipTurns();

    int getWarshipTurns();

    int getDropshipTurns();

    int getSmallCraftTurns();

    int getTeleMissileTurns();

    int getAeroTurns();

    void incrementOtherTurns();

    void incrementEvenTurns();

    void incrementMultiTurns(int entityClass);

    void incrementSpaceStationTurns();

    void incrementJumpshipTurns();

    void incrementWarshipTurns();

    void incrementDropshipTurns();

    void incrementSmallCraftTurns();

    void incrementTeleMissileTurns();

    void incrementAeroTurns();

    void resetOtherTurns();

    void resetEvenTurns();

    void resetMultiTurns();

    void resetSpaceStationTurns();

    void resetJumpshipTurns();

    void resetWarshipTurns();

    void resetDropshipTurns();

    void resetSmallCraftTurns();

    void resetTeleMissileTurns();

    void resetAeroTurns();

    InitiativeRoll getInitiative();

    /**
     * Clear the initiative of this object.
     */
    void clearInitiative(boolean bUseInitComp, Map<Team, Integer> initiativeAptitude);

    void setInitiative(InitiativeRoll newRoll);

    int getInitCompensationBonus();

    void setInitCompensationBonus(int newBonus);

}

/**
 * MegaMek - Copyright (C) 2003, 2004 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.common;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: Deric
 * Date: 8/17/13
 * Time: 10:21 AM
 * To change this template use File | Settings | File Templates.
 */
public interface ITurnOrdered extends Serializable {
    /**
     * Return the number of "normal" turns that this item requires. This is
     * normally the sum of multi-unit turns and the other turns. <p/> Subclasses
     * are expected to override this value in order to make the "move even" code
     * work correctly.
     *
     * @return the <code>int</code> number of "normal" turns this item should
     *         take in a phase.
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

    int getAeroTurns();

    void incrementOtherTurns();

    void incrementEvenTurns();

    void incrementMultiTurns();

    void incrementSpaceStationTurns();

    void incrementJumpshipTurns();

    void incrementWarshipTurns();

    void incrementDropshipTurns();

    void incrementSmallCraftTurns();

    void incrementAeroTurns();

    void resetOtherTurns();

    void resetEvenTurns();

    void resetMultiTurns();

    void resetSpaceStationTurns();

    void resetJumpshipTurns();

    void resetWarshipTurns();

    void resetDropshipTurns();

    void resetSmallCraftTurns();

    void resetAeroTurns();

    InitiativeRoll getInitiative();

    /**
     * Clear the initiative of this object.
     */
    void clearInitiative(boolean bUseInitComp);
}

/*
 * Copyright (C) 2003 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

import megamek.client.ui.Messages;
import megamek.common.OffBoardDirection;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net
 * @since 8/17/13 10:37 PM
 */
public enum CardinalEdge {
    NORTH(0, Messages.getString("BotConfigDialog.northEdge")),
    SOUTH(1, Messages.getString("BotConfigDialog.southEdge")),
    WEST(2, Messages.getString("BotConfigDialog.westEdge")),
    EAST(3, Messages.getString("BotConfigDialog.eastEdge")),
    // this signals that the nearest edge to the currently selected unit should be picked
    NEAREST(4, Messages.getString("BotConfigDialog.nearestEdge")),
    // no edge
    NONE(5, Messages.getString("BotConfigDialog.noEdge"));

    private int index;
    private String text;

    CardinalEdge(int index, String text) {
        this.index = index;
        this.text = text;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public String toString() {
        return text;
    }

    /**
     * Given an index, attempt to return a cardinal edge.
     */
    public static CardinalEdge getCardinalEdge(int index) {
        for (CardinalEdge he : values()) {
            if (he.getIndex() == index) {
                return he;
            }
        }
        return null;
    }

    /**
     * Given a string, attempt to figure out if it corresponds to a cardinal edge
     */
    public static CardinalEdge parseFromString(String source) {
        // attempt enum parse
        try {
            return valueOf(source);
        } catch (Exception ignored) {
        }

        return CardinalEdge.NONE;
    }

    /**
     * Convert an OffBoardDirection to a cardinal edge
     */
    public static CardinalEdge getCardinalEdge(OffBoardDirection direction) {
        switch (direction) {
            case NORTH:
                return NORTH;
            case SOUTH:
                return SOUTH;
            case EAST:
                return EAST;
            case WEST:
                return WEST;
            default:
                return NONE;
        }
    }

    /**
     * Attempt to determine the opposite edge, given another cardinal edge
     */
    public static CardinalEdge getOppositeEdge(CardinalEdge edge) {
        switch (edge) {
            case NORTH:
                return SOUTH;
            case SOUTH:
                return NORTH;
            case EAST:
                return WEST;
            case WEST:
                return EAST;
            default:
                return NONE;
        }

    }
}

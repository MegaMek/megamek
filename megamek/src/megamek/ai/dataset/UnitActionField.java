/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
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
 */
package megamek.ai.dataset;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum UnitActionField {
    PLAYER_ID("PLAYER_ID"),
    ENTITY_ID("ENTITY_ID"),
    CHASSIS("CHASSIS"),
    MODEL("MODEL"),
    FACING("FACING"),
    FROM_X("FROM_X"),
    FROM_Y("FROM_Y"),
    TO_X("TO_X"),
    TO_Y("TO_Y"),
    HEXES_MOVED("HEXES_MOVED"),
    DISTANCE("DISTANCE"),
    MP_USED("MP_USED"),
    MAX_MP("MAX_MP"),
    MP_P("MP_P"),
    HEAT_P("HEAT_P"),
    ARMOR_P("ARMOR_P"),
    INTERNAL_P("INTERNAL_P"),
    JUMPING("JUMPING"),
    PRONE("PRONE"),
    LEGAL("LEGAL"),
    STEPS("STEPS"),
    TEAM_ID("TEAM_ID"),
    CHANCE_OF_FAILURE("CHANCE_OF_FAILURE"),
    IS_BOT("IS_BOT"),
    HAS_ECM("HAS_ECM"),
    ARMOR("ARMOR"),
    INTERNAL("INTERNAL"),
    BV("BV"),
    MAX_RANGE("MAX_RANGE"),
    ARMOR_FRONT_P("ARMOR_FRONT_P"),
    ARMOR_LEFT_P("ARMOR_LEFT_P"),
    ARMOR_RIGHT_P("ARMOR_RIGHT_P"),
    ARMOR_BACK_P("ARMOR_BACK_P"),
    TOTAL_DAMAGE("TOTAL_DAMAGE"),
    ROLE("ROLE"),
    WEAPON_DMG_FACING_SHORT_MEDIUM_LONG_RANGE("WEAPON_DMG_FACING_SHORT_MEDIUM_LONG_RANGE"),
    ;

    private final String headerName;

    UnitActionField(String headerName) {
        this.headerName = headerName;
    }

    public String getHeaderName() {
        return headerName;
    }

    /**
     * Builds the TSV header line (joined by tabs) by iterating over all enum constants.
     */
    public static String getHeaderLine() {
        return Arrays.stream(values())
                     .map(UnitActionField::getHeaderName)
                     .collect(Collectors.joining("\t"));
    }

    /**
     * Builds the TSV header line (joined by tabs) by iterating over all enum constants.
     */
    public static String getPartialHeaderLine(int startsAt, int endsAt) {
        return Arrays.stream(values())
                     .skip(startsAt)
                     .limit(endsAt - startsAt)
                     .map(UnitActionField::getHeaderName)
                     .collect(Collectors.joining("\t"));
    }
}

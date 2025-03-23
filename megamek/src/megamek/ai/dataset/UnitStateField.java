/*
 * Copyright (C) 2017-2025 The MegaMek Team. All Rights Reserved.
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
 */
package megamek.ai.dataset;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum UnitStateField{
    ROUND("ROUND"),
    PHASE("PHASE"),
    PLAYER_ID("PLAYER_ID"),
    ENTITY_ID("ENTITY_ID"),
    CHASSIS("CHASSIS"),
    MODEL("MODEL"),
    TYPE("TYPE"),
    ROLE("ROLE"),
    X("X"),
    Y("Y"),
    FACING("FACING"),
    MP("MP"),
    HEAT("HEAT"),
    PRONE("PRONE"),
    AIRBORNE("AIRBORNE"),
    OFF_BOARD("OFF_BOARD"),
    CRIPPLED("CRIPPLED"),
    DESTROYED("DESTROYED"),
    ARMOR_P("ARMOR_P"),
    INTERNAL_P("INTERNAL_P"),
    DONE("DONE"),
    MAX_RANGE("MAX_RANGE"),
    TOTAL_DAMAGE("TOTAL_DAMAGE"),
    TEAM_ID("TEAM_ID"),
    ARMOR("ARMOR"),
    INTERNAL("INTERNAL"),
    BV("BV");

    private final String headerName;

    UnitStateField(String headerName) {
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
            .map(UnitStateField::getHeaderName)
            .collect(Collectors.joining("\t"));
    }
}

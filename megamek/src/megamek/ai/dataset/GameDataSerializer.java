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
package megamek.ai.dataset;

import java.util.ArrayList;
import java.util.List;

import megamek.ai.dataset.GameData.Field;
import megamek.ai.dataset.GameData.MinefieldData;

/**
 * <p>Serializer for GameData to TSV format.</p>
 * @author Luana Coppio
 */
public class GameDataSerializer extends EntityDataSerializer<GameData.Field, GameData> {

    private static final String MINEFIELD = "MINEFIELD";
    private final UnitStateSerializer unitStateSerializer = new UnitStateSerializer();

    /**
     * Creates a new GameDataSerializer with default serializers.
     */
    public GameDataSerializer() {
        super(Field.class);
    }


    @Override
    protected String getLineTypeMarker() {
        return "GD_1";
    }

    /**
     * Serializes game data to multiple lines of TSV format.
     * @param data The game data to serialize
     * @return A TSV-formatted strings with multiple lines
     */
    @Override
    public String serialize(GameData data) {
        List<String> lines = new ArrayList<>();

        // First add entity state data
        List<UnitState> unitStates = data.getUnitStates();
        if (unitStates != null && !unitStates.isEmpty()) {
            // Add header for unit states
            lines.add(unitStateSerializer.getHeaderLine());

            // Add each unit state
            for (UnitState unitState : unitStates) {
                lines.add(unitStateSerializer.serialize(unitState));
            }
        }

        // Then add minefield data if present
        List<MinefieldData> minefields = data.getMinefields();
        if (minefields != null && !minefields.isEmpty()) {
            // Add a header for minefields
            lines.add("ROUND\tPHASE\tOBJECT\tX\tY\tTYPE\tPLAYER_ID\tDAMAGE");

            // Add each minefield
            String round = String.valueOf(data.get(Field.ROUND));
            String phase = String.valueOf(data.get(Field.PHASE));

            for (MinefieldData minefield : minefields) {
                lines.add(getLineTypeMarker() + TAB_DELIMITER + String.join(TAB_DELIMITER,
                      round,
                      phase,
                      MINEFIELD,
                      String.valueOf(minefield.getX()),
                      String.valueOf(minefield.getY()),
                      String.valueOf(minefield.getType()),
                      String.valueOf(minefield.getPlayerId()),
                      String.valueOf(minefield.getDamage())
                ));
            }
        }

        return String.join("\n", lines);
    }
}

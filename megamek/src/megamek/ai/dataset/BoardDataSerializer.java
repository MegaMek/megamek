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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import megamek.ai.dataset.BoardData.Field;
import megamek.ai.dataset.BoardData.HexRow;

/**
 * <p>Serializer for BoardData to TSV format.</p>
 * @author Luana Coppio
 */
public class BoardDataSerializer extends EntityDataSerializer<Field, BoardData> {

    public BoardDataSerializer() {
        super(Field.class);
    }

    @Override
    public String serialize(BoardData data) {
        List<String> lines = new ArrayList<>();

        // First line: Main board data
        lines.add(getHeaderLine());

        String mainData = String.join("\t",
              String.valueOf(data.get(Field.BOARD_NAME)),
              String.valueOf(data.get(Field.WIDTH)),
              String.valueOf(data.get(Field.HEIGHT)));
        lines.add(mainData);

        // Get the hex data for serialization
        List<HexRow> hexRows = data.getHexRows();
        if (hexRows != null && !hexRows.isEmpty()) {
            // Add the column header row (COL_0, COL_1, etc.)
            int width = ((Integer) data.get(Field.WIDTH));
            String colHeader = IntStream.range(0, width)
                                     .mapToObj(i -> "COL_" + i)
                                     .collect(Collectors.joining("\t"));
            lines.add(colHeader);

            // Add each row of hex data
            for (HexRow row : hexRows) {
                StringBuilder sb = new StringBuilder("ROW_").append(row.getRowIndex()).append("\t");
                sb.append(row.getHexes().stream()
                                .map(hex -> hex == null ? "" : hex.toString())
                                .collect(Collectors.joining("\t")));
                lines.add(sb.toString());
            }
        }

        return String.join("\n", lines);
    }

    @Override
    public String getHeaderLine() {
        return String.join("\t", Field.BOARD_NAME.name(), Field.WIDTH.name(), Field.HEIGHT.name());
    }
}

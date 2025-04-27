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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import megamek.ai.dataset.BoardData.Field;
import megamek.ai.dataset.BoardData.HexRow;
import megamek.common.Hex;

/**
 * <p>Serializer for BoardData to TSV format.</p>
 * @author Luana Coppio
 */
public class BoardDataSerializer extends EntityDataSerializer<Field, BoardData> {

    public static final String LINE_BREAK_DELIMITER = "\n";

    private static final Function<Hex, String> GET_HEX_STRING_FUNCTION = (hex -> hex == null ? "" : hex.toString());

    public BoardDataSerializer() {
        super(Field.class);
    }

    @Override
    protected String getLineTypeMarker() {
        return "BD_1";
    }

    @Override
    public String serialize(BoardData data) {
        List<String> lines = new ArrayList<>();

        // First line: Main board data
        lines.add(getHeaderLine());

        String mainData = String.join(TAB_DELIMITER,
              getLineTypeMarker(),
              String.valueOf(data.get(Field.BOARD_NAME)),
              String.valueOf(data.get(Field.WIDTH)),
              String.valueOf(data.get(Field.HEIGHT)));
        lines.add(mainData);

        // Get the hex data for serialization
        List<HexRow> hexRows = data.getHexRows();
        if (hexRows != null && !hexRows.isEmpty()) {
            // Add the column header row (BD_1  COL_0   COL_1   ...)
            int width = ((Integer) data.get(Field.WIDTH));
            String colHeader = getLineTypeMarker() + TAB_DELIMITER + IntStream.range(0, width)
                                     .mapToObj(i -> "COL_" + i)
                                     .collect(Collectors.joining(TAB_DELIMITER));
            lines.add(colHeader);

            // Add each row of hex data
            for (HexRow row : hexRows) {
                StringBuilder sb = new StringBuilder(getLineTypeMarker()).append(TAB_DELIMITER)
                                         .append("ROW_").append(row.getRowIndex()).append(TAB_DELIMITER);
                sb.append(row.getHexes().stream()
                                .map(GET_HEX_STRING_FUNCTION)
                                .collect(Collectors.joining(TAB_DELIMITER)));
                lines.add(sb.toString());
            }
        }

        return String.join(LINE_BREAK_DELIMITER, lines);
    }

    @Override
    public String getHeaderLine() {
        return String.join(TAB_DELIMITER, getLineTypeMarker(), Field.BOARD_NAME.name(), Field.WIDTH.name(),
              Field.HEIGHT.name());
    }
}

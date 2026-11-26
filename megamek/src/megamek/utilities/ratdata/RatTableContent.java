/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.utilities.ratdata;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * One random assignment table as the RAT data export stores it: the faction's own units with their weights, the
 * salvage factions with their weights, and the chance a roll goes to salvage.
 * <p>
 * The encoding is canonical: entries are sorted by index, so two tables with the same contents always encode to the
 * same bytes. That is what lets {@link RatDataShard} store each distinct table once.
 * </p>
 * <p>
 * Encoded layout, little-endian:
 * </p>
 * <pre>
 * uint8  salvagePct
 * uint16 unitCount
 * uint16 salvageCount
 * unitCount    x (uint16 unitIndex,    uint16 weight)
 * salvageCount x (uint16 factionIndex, uint16 weight)
 * </pre>
 *
 * @param salvagePct the whole-number chance, 0 to 100, that a roll goes to a salvage faction
 * @param units      the faction's own units, each an index into the export's unit list with its table weight
 * @param salvage    the salvage factions, each an index into the export's faction list with its table weight
 */
public record RatTableContent(int salvagePct, List<WeightedEntry> units, List<WeightedEntry> salvage) {

    /** Largest index or weight that fits the encoding. */
    static final int MAX_UINT16 = 0xFFFF;

    /**
     * An index into one of the export's lists, with its weight in the table.
     *
     * @param index  the unit or faction index
     * @param weight the table weight, as {@code UnitTable} returns it
     */
    public record WeightedEntry(int index, int weight) {}

    public RatTableContent {
        if ((salvagePct < 0) || (salvagePct > 100)) {
            throw new IllegalArgumentException("Salvage percentage " + salvagePct + " is outside 0 to 100");
        }
        units = sortedCopy(units);
        salvage = sortedCopy(salvage);
    }

    private static List<WeightedEntry> sortedCopy(List<WeightedEntry> entries) {
        List<WeightedEntry> sorted = new ArrayList<>(entries);
        sorted.sort(Comparator.comparingInt(WeightedEntry::index));
        for (WeightedEntry entry : sorted) {
            checkUint16("index", entry.index());
            checkUint16("weight", entry.weight());
        }
        return List.copyOf(sorted);
    }

    private static void checkUint16(String name, int value) {
        if ((value < 0) || (value > MAX_UINT16)) {
            throw new IllegalArgumentException("Table entry " + name + " " + value + " does not fit in 16 bits");
        }
    }

    /**
     * @return the canonical byte encoding of this table
     */
    public byte[] encode() {
        checkUint16("unit count", units.size());
        checkUint16("salvage count", salvage.size());
        ByteBuffer buffer = ByteBuffer.allocate(5 + (4 * (units.size() + salvage.size())))
              .order(ByteOrder.LITTLE_ENDIAN);
        buffer.put((byte) salvagePct);
        buffer.putShort((short) units.size());
        buffer.putShort((short) salvage.size());
        for (WeightedEntry unit : units) {
            buffer.putShort((short) unit.index());
            buffer.putShort((short) unit.weight());
        }
        for (WeightedEntry salvageFaction : salvage) {
            buffer.putShort((short) salvageFaction.index());
            buffer.putShort((short) salvageFaction.weight());
        }
        return buffer.array();
    }
}

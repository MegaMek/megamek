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

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The tables for one year (general tables) or one era year (role tables), with each distinct table stored once.
 * <p>
 * Many factions share a table exactly, because a sub-command with no codes of its own inherits its parent's. Storing
 * each distinct table once and pointing every (faction, rating, unit type, role) at it more than halves the size.
 * </p>
 * <p>
 * Layout, little-endian:
 * </p>
 * <pre>
 * 4 bytes  magic "RAT1"
 * uint16   format version
 * uint16   year
 * uint8    kind: 0 = general tables, 1 = role tables
 * uint32   tableCount, then each table as {@link RatTableContent#encode()}
 * uint32   referenceCount, then each reference:
 *          uint16 factionIndex, uint8 ratingIndex, uint8 unitTypeIndex, uint8 roleIndex (255 = none),
 *          uint32 tableIndex
 * </pre>
 */
public class RatDataShard {

    /** Bumped whenever the layout changes, so the website can refuse data it does not understand. */
    public static final int FORMAT_VERSION = 2;

    /** Role index stored for a general table. */
    public static final int NO_ROLE = 255;

    static final byte[] MAGIC = "RAT1".getBytes(StandardCharsets.US_ASCII);

    /** What kind of tables a shard holds. */
    public enum Kind {
        GENERAL,
        ROLES
    }

    /**
     * Points one (faction, rating, unit type, role) at a stored table.
     *
     * @param factionIndex  index into the export's faction list
     * @param ratingIndex   index into that faction's rating list
     * @param unitTypeIndex the {@code UnitType} code
     * @param roleIndex     the {@code MissionRole} ordinal, or {@link #NO_ROLE}
     * @param tableIndex    index of the table within this shard
     */
    public record TableReference(int factionIndex, int ratingIndex, int unitTypeIndex, int roleIndex,
          int tableIndex) {}

    private final int year;
    private final Kind kind;
    private final Map<ByteBuffer, Integer> tableIndexByContent = new HashMap<>();
    private final ByteArrayOutputStream tableBytes = new ByteArrayOutputStream();
    private final List<TableReference> references = new ArrayList<>();

    /**
     * @param year the game year the tables were generated for
     * @param kind whether the shard holds general or role tables
     */
    public RatDataShard(int year, Kind kind) {
        this.year = year;
        this.kind = kind;
    }

    /**
     * Records a table for one faction, rating, unit type and role. A table identical to one already in the shard is
     * not stored again; the reference points at the existing copy.
     *
     * @param factionIndex  index into the export's faction list
     * @param ratingIndex   index into that faction's rating list
     * @param unitTypeIndex the {@code UnitType} code
     * @param roleIndex     the {@code MissionRole} ordinal, or {@link #NO_ROLE}
     * @param content       the table
     *
     * @return the index of the stored table within this shard
     */
    public int addTable(int factionIndex, int ratingIndex, int unitTypeIndex, int roleIndex,
          RatTableContent content) {
        checkRange("faction index", factionIndex, RatTableContent.MAX_UINT16);
        checkRange("rating index", ratingIndex, 0xFF);
        checkRange("unit type index", unitTypeIndex, 0xFF);
        checkRange("role index", roleIndex, 0xFF);

        byte[] encoded = content.encode();
        ByteBuffer key = ByteBuffer.wrap(encoded);
        Integer tableIndex = tableIndexByContent.get(key);
        if (tableIndex == null) {
            tableIndex = tableIndexByContent.size();
            tableIndexByContent.put(key, tableIndex);
            tableBytes.writeBytes(encoded);
        }
        references.add(new TableReference(factionIndex, ratingIndex, unitTypeIndex, roleIndex, tableIndex));
        return tableIndex;
    }

    private static void checkRange(String name, int value, int max) {
        if ((value < 0) || (value > max)) {
            throw new IllegalArgumentException(name + " " + value + " is outside 0 to " + max);
        }
    }

    /**
     * @return the number of distinct tables stored
     */
    public int getTableCount() {
        return tableIndexByContent.size();
    }

    /**
     * @return the number of (faction, rating, unit type, role) references
     */
    public int getReferenceCount() {
        return references.size();
    }

    /**
     * @return the shard in the layout described on this class
     */
    public byte[] toBytes() {
        byte[] tables = tableBytes.toByteArray();
        ByteBuffer buffer = ByteBuffer.allocate(MAGIC.length + 2 + 2 + 1 + 4 + tables.length + 4
                    + (references.size() * 9))
              .order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(MAGIC);
        buffer.putShort((short) FORMAT_VERSION);
        buffer.putShort((short) year);
        buffer.put((byte) kind.ordinal());
        buffer.putInt(getTableCount());
        buffer.put(tables);
        buffer.putInt(references.size());
        for (TableReference reference : references) {
            buffer.putShort((short) reference.factionIndex());
            buffer.put((byte) reference.ratingIndex());
            buffer.put((byte) reference.unitTypeIndex());
            buffer.put((byte) reference.roleIndex());
            buffer.putInt(reference.tableIndex());
        }
        return buffer.array();
    }
}

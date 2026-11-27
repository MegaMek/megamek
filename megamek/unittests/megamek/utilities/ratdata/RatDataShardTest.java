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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import megamek.utilities.ratdata.RatTableContent.WeightedEntry;
import org.junit.jupiter.api.Test;

/**
 * Checks the RAT data file layout: tables encode the same way whatever order their entries arrive in, identical
 * tables are stored once, and a shard reads back to exactly what was put in.
 */
class RatDataShardTest {

    private static RatTableContent sampleTable() {
        return new RatTableContent(5,
              List.of(new WeightedEntry(42, 180), new WeightedEntry(7, 90), new WeightedEntry(300, 12)),
              List.of(new WeightedEntry(3, 20)));
    }

    @Test
    void tableEncodingDoesNotDependOnEntryOrder() {
        RatTableContent reordered = new RatTableContent(5,
              List.of(new WeightedEntry(300, 12), new WeightedEntry(42, 180), new WeightedEntry(7, 90)),
              List.of(new WeightedEntry(3, 20)));

        assertArrayEquals(sampleTable().encode(), reordered.encode());
    }

    @Test
    void tableEncodingFollowsTheDocumentedLayout() {
        ByteBuffer buffer = ByteBuffer.wrap(sampleTable().encode()).order(ByteOrder.LITTLE_ENDIAN);

        assertEquals(5, Byte.toUnsignedInt(buffer.get()));
        assertEquals(3, Short.toUnsignedInt(buffer.getShort()));
        assertEquals(1, Short.toUnsignedInt(buffer.getShort()));
        // Units sorted by index: 7, 42, 300
        assertEquals(7, Short.toUnsignedInt(buffer.getShort()));
        assertEquals(90, Short.toUnsignedInt(buffer.getShort()));
        assertEquals(42, Short.toUnsignedInt(buffer.getShort()));
        assertEquals(180, Short.toUnsignedInt(buffer.getShort()));
        assertEquals(300, Short.toUnsignedInt(buffer.getShort()));
        assertEquals(12, Short.toUnsignedInt(buffer.getShort()));
        assertEquals(3, Short.toUnsignedInt(buffer.getShort()));
        assertEquals(20, Short.toUnsignedInt(buffer.getShort()));
        assertEquals(0, buffer.remaining());
    }

    @Test
    void differentWeightsGiveDifferentEncodings() {
        RatTableContent otherWeight = new RatTableContent(5,
              List.of(new WeightedEntry(42, 181), new WeightedEntry(7, 90), new WeightedEntry(300, 12)),
              List.of(new WeightedEntry(3, 20)));

        assertNotEquals(ByteBuffer.wrap(sampleTable().encode()), ByteBuffer.wrap(otherWeight.encode()));
    }

    @Test
    void valuesThatDoNotFitTheLayoutAreRefused() {
        List<WeightedEntry> noSalvage = List.of();
        List<WeightedEntry> tooLargeIndex = List.of(new WeightedEntry(70_000, 1));
        List<WeightedEntry> negativeWeight = List.of(new WeightedEntry(1, -1));

        assertThrows(IllegalArgumentException.class, () -> new RatTableContent(5, tooLargeIndex, noSalvage));
        assertThrows(IllegalArgumentException.class, () -> new RatTableContent(5, negativeWeight, noSalvage));
        assertThrows(IllegalArgumentException.class, () -> new RatTableContent(101, noSalvage, noSalvage));
    }

    @Test
    void identicalTablesAreStoredOnce() {
        RatDataShard shard = new RatDataShard(3039, RatDataShard.Kind.GENERAL);

        int first = shard.addTable(10, 2, 0, RatDataShard.NO_ROLE, sampleTable());
        int second = shard.addTable(11, 2, 0, RatDataShard.NO_ROLE, sampleTable());
        int different = shard.addTable(12, 2, 0, RatDataShard.NO_ROLE,
              new RatTableContent(0, List.of(new WeightedEntry(1, 1)), List.of()));

        assertEquals(first, second);
        assertNotEquals(first, different);
        assertEquals(2, shard.getTableCount());
        assertEquals(3, shard.getReferenceCount());
    }

    @Test
    void shardReadsBackToWhatWasAdded() {
        RatDataShard shard = new RatDataShard(3067, RatDataShard.Kind.ROLES);
        RatTableContent other = new RatTableContent(0, List.of(new WeightedEntry(9, 400)), List.of());
        shard.addTable(4, 1, 0, 8, sampleTable());
        shard.addTable(5, 0, 9, 8, other);
        shard.addTable(6, 4, 0, 8, sampleTable());

        DecodedShard decoded = DecodedShard.read(shard.toBytes());

        assertEquals(RatDataShard.FORMAT_VERSION, decoded.version());
        assertEquals(3067, decoded.year());
        assertEquals(RatDataShard.Kind.ROLES.ordinal(), decoded.kind());
        assertEquals(2, decoded.tables().size());
        assertArrayEquals(sampleTable().encode(), decoded.tables().get(0));
        assertArrayEquals(other.encode(), decoded.tables().get(1));
        assertEquals(List.of(
              new RatDataShard.TableReference(4, 1, 0, 8, 0),
              new RatDataShard.TableReference(5, 0, 9, 8, 1),
              new RatDataShard.TableReference(6, 4, 0, 8, 0)), decoded.references());
    }

    @Test
    void referenceFieldsOutsideTheirRangeAreRefused() {
        RatDataShard shard = new RatDataShard(3039, RatDataShard.Kind.GENERAL);

        assertThrows(IllegalArgumentException.class, () -> shard.addTable(70_000, 0, 0, 0, sampleTable()));
        assertThrows(IllegalArgumentException.class, () -> shard.addTable(0, 256, 0, 0, sampleTable()));
        assertThrows(IllegalArgumentException.class, () -> shard.addTable(0, 0, -1, 0, sampleTable()));
    }

    /** A reader for the shard layout, written from the documentation, as the website's reader will be. */
    private record DecodedShard(int version, int year, int kind, List<byte[]> tables,
          List<RatDataShard.TableReference> references) {

        static DecodedShard read(byte[] bytes) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
            byte[] magic = new byte[4];
            buffer.get(magic);
            assertArrayEquals(RatDataShard.MAGIC, magic);
            int version = Short.toUnsignedInt(buffer.getShort());
            int year = Short.toUnsignedInt(buffer.getShort());
            int kind = Byte.toUnsignedInt(buffer.get());

            int tableCount = buffer.getInt();
            List<byte[]> tables = new ArrayList<>();
            for (int tableIndex = 0; tableIndex < tableCount; tableIndex++) {
                int start = buffer.position();
                buffer.get();
                int unitCount = Short.toUnsignedInt(buffer.getShort());
                int salvageCount = Short.toUnsignedInt(buffer.getShort());
                buffer.position(buffer.position() + (4 * (unitCount + salvageCount)));
                byte[] table = new byte[buffer.position() - start];
                buffer.get(start, table);
                tables.add(table);
            }

            int referenceCount = buffer.getInt();
            List<RatDataShard.TableReference> references = new ArrayList<>();
            for (int referenceIndex = 0; referenceIndex < referenceCount; referenceIndex++) {
                references.add(new RatDataShard.TableReference(Short.toUnsignedInt(buffer.getShort()),
                      Byte.toUnsignedInt(buffer.get()), Byte.toUnsignedInt(buffer.get()),
                      Byte.toUnsignedInt(buffer.get()), buffer.getInt()));
            }
            assertEquals(0, buffer.remaining());
            return new DecodedShard(version, year, kind, tables, references);
        }
    }
}

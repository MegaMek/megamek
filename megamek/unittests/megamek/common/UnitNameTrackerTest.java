/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import megamek.utils.MockGenerators;
import org.junit.jupiter.api.Test;

class UnitNameTrackerTest {
    @Test
    void addEntitySetsDuplicateMarkerCorrectly() {
        String shortNameRaw = "Mek MEK-01X";
        Entity mockEntity = createEntity(shortNameRaw);
        UnitNameTracker tracker = new UnitNameTracker();
        tracker.add(mockEntity);
        verify(mockEntity, times(1)).setDuplicateMarker(eq(1));
    }

    @Test
    void addMultipleUnrelatedEntitiesSetsDuplicateMarkerCorrectly() {
        String shortNameRaw0 = "Mek MEK-01X";
        String shortNameRaw1 = "Mek MEK-02X";

        Entity mockEntity0 = createEntity(shortNameRaw0);
        Entity mockEntity1 = createEntity(shortNameRaw1);

        UnitNameTracker tracker = new UnitNameTracker();

        tracker.add(mockEntity0);
        tracker.add(mockEntity1);

        verify(mockEntity0, times(1)).setDuplicateMarker(eq(1));
        verify(mockEntity1, times(1)).setDuplicateMarker(eq(1));
    }

    @Test
    void addMultipleRelatedEntitiesSetsDuplicateMarkerCorrectly() {
        String shortNameRaw = "Mek MEK-01X";

        Entity mockEntity0 = createEntity(shortNameRaw);
        Entity mockEntity1 = createEntity(shortNameRaw);

        UnitNameTracker tracker = new UnitNameTracker();

        tracker.add(mockEntity0);
        tracker.add(mockEntity1);

        verify(mockEntity0, times(1)).setDuplicateMarker(eq(1));
        verify(mockEntity1, times(1)).setDuplicateMarker(eq(2));
    }

    @Test
    void removeEntityUpdatesDuplicateMarker() {
        String shortNameRaw = "Mek MEK-01X";

        Entity mockEntity0 = createEntity(shortNameRaw);
        Entity mockEntity1 = createEntity(shortNameRaw);
        Entity mockEntity2 = createEntity(shortNameRaw);

        UnitNameTracker tracker = new UnitNameTracker();

        tracker.add(mockEntity0);
        tracker.add(mockEntity1);
        tracker.add(mockEntity2);

        verify(mockEntity0, times(1)).setDuplicateMarker(eq(1));
        verify(mockEntity1, times(1)).setDuplicateMarker(eq(2));
        verify(mockEntity2, times(1)).setDuplicateMarker(eq(3));

        tracker.remove(mockEntity0);

        verify(mockEntity1, times(1)).updateDuplicateMarkerAfterDelete(eq(1));
        verify(mockEntity2, times(1)).updateDuplicateMarkerAfterDelete(eq(1));
    }

    @Test
    void removeEntityUpdatesDuplicateMarker2() {
        String shortNameRaw = "Mek MEK-01X";

        Entity mockEntity0 = createEntity(shortNameRaw);
        Entity mockEntity1 = createEntity(shortNameRaw);
        Entity mockEntity2 = createEntity(shortNameRaw);

        UnitNameTracker tracker = new UnitNameTracker();

        tracker.add(mockEntity0);
        tracker.add(mockEntity1);
        tracker.add(mockEntity2);

        verify(mockEntity0, times(1)).setDuplicateMarker(eq(1));
        verify(mockEntity1, times(1)).setDuplicateMarker(eq(2));
        verify(mockEntity2, times(1)).setDuplicateMarker(eq(3));

        tracker.remove(mockEntity1);

        verify(mockEntity0, times(1)).updateDuplicateMarkerAfterDelete(eq(2));
        verify(mockEntity2, times(1)).updateDuplicateMarkerAfterDelete(eq(2));
    }

    @Test
    void removeEntityUpdatesDuplicateMarker3() {
        String shortNameRaw = "Mek MEK-01X";

        Entity mockEntity0 = createEntity(shortNameRaw);
        Entity mockEntity1 = createEntity(shortNameRaw);
        Entity mockEntity2 = createEntity(shortNameRaw);

        UnitNameTracker tracker = new UnitNameTracker();

        tracker.add(mockEntity0);
        tracker.add(mockEntity1);

        verify(mockEntity0, times(1)).setDuplicateMarker(eq(1));
        verify(mockEntity1, times(1)).setDuplicateMarker(eq(2));

        tracker.remove(mockEntity0);

        verify(mockEntity1, times(1)).updateDuplicateMarkerAfterDelete(eq(1));

        tracker.add(mockEntity2);

        // This entity is now #2 as the original number 2 became number 1
        verify(mockEntity2, times(1)).setDuplicateMarker(eq(2));
    }

    @Test
    void removeEntityOnlyAffectsRelatedEntities() {
        String shortNameRaw0 = "Mek MEK-01X";
        String shortNameRaw1 = "Mek MEK-01Y";

        Entity mockEntity0 = createEntity(shortNameRaw0);
        Entity mockEntity1 = createEntity(shortNameRaw0);
        Entity mockEntityUnrelated = createEntity(shortNameRaw1);

        UnitNameTracker tracker = new UnitNameTracker();

        tracker.add(mockEntity0);
        tracker.add(mockEntity1);
        tracker.add(mockEntityUnrelated);

        verify(mockEntity0, times(1)).setDuplicateMarker(eq(1));
        verify(mockEntity1, times(1)).setDuplicateMarker(eq(2));
        verify(mockEntityUnrelated, times(1)).setDuplicateMarker(eq(1));

        tracker.remove(mockEntity0);

        verify(mockEntity1, times(1)).updateDuplicateMarkerAfterDelete(eq(1));

        // Should not update unrelated entity
        verify(mockEntityUnrelated, times(0)).updateDuplicateMarkerAfterDelete(anyInt());
    }

    @Test
    void clearEntities() {
        String shortNameRaw = "Mek MEK-01X";

        Entity mockEntity0 = createEntity(shortNameRaw);
        Entity mockEntity1 = createEntity(shortNameRaw);
        Entity mockEntity2 = createEntity(shortNameRaw);

        UnitNameTracker tracker = new UnitNameTracker();

        tracker.add(mockEntity0);
        tracker.add(mockEntity1);

        verify(mockEntity0, times(1)).setDuplicateMarker(eq(1));
        verify(mockEntity1, times(1)).setDuplicateMarker(eq(2));

        tracker.clear();

        tracker.add(mockEntity2);

        // There are no entities being tracked, so this should be #1
        verify(mockEntity2, times(1)).setDuplicateMarker(eq(1));
    }

    private Entity createEntity(String shortNameRaw) {
        Entity mockEntity = MockGenerators.generateMockBipedMek(0, 0);
        when(mockEntity.getShortNameRaw()).thenReturn(shortNameRaw);
        doAnswer(inv -> {
            int marker = inv.getArgument(0);
            when(mockEntity.getDuplicateMarker()).thenReturn(marker);
            return null;
        }).when(mockEntity).setDuplicateMarker(anyInt());
        return mockEntity;
    }
}

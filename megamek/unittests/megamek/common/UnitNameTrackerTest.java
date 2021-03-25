/*
 * Copyright (C) 2021 - The MegaMek Team
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

package megamek.common;

import static org.mockito.Mockito.*;

import org.junit.Test;

public class UnitNameTrackerTest {
    @Test
    public void addEntitySetsDuplicateMarkerCorrectly() {
        String shortNameRaw = "Mech MEK-01X";

        Entity mockEntity = createEntity(shortNameRaw);

        UnitNameTracker tracker = new UnitNameTracker();

        tracker.add(mockEntity);

        verify(mockEntity, times(1)).setDuplicateMarker(eq(1));
    }

    @Test
    public void addMultipleUnrelatedEntitiesSetsDuplicateMarkerCorrectly() {
        String shortNameRaw0 = "Mech MEK-01X";
        String shortNameRaw1 = "Mech MEK-02X";

        Entity mockEntity0 = createEntity(shortNameRaw0);
        Entity mockEntity1 = createEntity(shortNameRaw1);

        UnitNameTracker tracker = new UnitNameTracker();

        tracker.add(mockEntity0);
        tracker.add(mockEntity1);

        verify(mockEntity0, times(1)).setDuplicateMarker(eq(1));
        verify(mockEntity1, times(1)).setDuplicateMarker(eq(1));
    }

    @Test
    public void addMultipleRelatedEntitiesSetsDuplicateMarkerCorrectly() {
        String shortNameRaw = "Mech MEK-01X";

        Entity mockEntity0 = createEntity(shortNameRaw);
        Entity mockEntity1 = createEntity(shortNameRaw);

        UnitNameTracker tracker = new UnitNameTracker();

        tracker.add(mockEntity0);
        tracker.add(mockEntity1);

        verify(mockEntity0, times(1)).setDuplicateMarker(eq(1));
        verify(mockEntity1, times(1)).setDuplicateMarker(eq(2));
    }

    @Test
    public void removeEntityUpdatesDuplicateMarker() {
        String shortNameRaw = "Mech MEK-01X";

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
    public void removeEntityUpdatesDuplicateMarker2() {
        String shortNameRaw = "Mech MEK-01X";

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
    public void removeEntityUpdatesDuplicateMarker3() {
        String shortNameRaw = "Mech MEK-01X";

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
    public void removeEntityOnlyAffectsRelatedEntities() {
        String shortNameRaw0 = "Mech MEK-01X";
        String shortNameRaw1 = "Mech MEK-01Y";

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
    public void clearEntities() {
        String shortNameRaw = "Mech MEK-01X";

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
        Entity mockEntity = mock(Entity.class);
        when(mockEntity.getShortNameRaw()).thenReturn(shortNameRaw);
        doAnswer(inv -> {
            int marker = (int) inv.getArgument(0);
            when(mockEntity.getDuplicateMarker()).thenReturn(marker);
            return null;
        }).when(mockEntity).setDuplicateMarker(anyInt());
        return mockEntity;
    }
}

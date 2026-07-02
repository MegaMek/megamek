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

package megamek.common.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.jupiter.api.Test;

class ObjectiveMarkerTest {

    @Test
    void testDefaults() {
        ObjectiveMarker marker = new ObjectiveMarker();

        assertEquals(0, marker.getControlRadius());
        assertEquals(1, marker.getVictoryPointValue());
        // RAW: objectives are destroyed with their building unless the mission states otherwise
        assertFalse(marker.isInvulnerable());
        assertFalse(marker.canBePickedUp(true));
        assertFalse(marker.canBePickedUp(false));
        assertFalse(marker.isDestroyed());
    }

    @Test
    void testControlRadiusValidation() {
        ObjectiveMarker marker = new ObjectiveMarker();

        marker.setControlRadius(0);
        marker.setControlRadius(ObjectiveMarker.MAX_CONTROL_RADIUS);
        assertEquals(ObjectiveMarker.MAX_CONTROL_RADIUS, marker.getControlRadius());

        assertThrows(IllegalArgumentException.class, () -> marker.setControlRadius(-1));
        assertThrows(IllegalArgumentException.class,
              () -> marker.setControlRadius(ObjectiveMarker.MAX_CONTROL_RADIUS + 1));
    }

    @Test
    void testAnyDamageDestroys() {
        ObjectiveMarker marker = new ObjectiveMarker();

        assertTrue(marker.damage(0.5));
        assertTrue(marker.isDestroyed());
    }

    @Test
    void testSerializationRoundTrip() throws Exception {
        ObjectiveMarker marker = new ObjectiveMarker();
        marker.setName("Left Counter");
        marker.setOwnerId(3);
        marker.setControlRadius(2);
        marker.setVictoryPointValue(2);
        marker.setPotential(true);
        marker.setConfirmed(true);
        marker.setFalseObjective(true);
        marker.setFragile(true);
        marker.setMobile(true);
        marker.setDestroyed(true);
        marker.setInsideBuilding(true);
        marker.setBuildingLinkInitialized(true);
        marker.setDestructionProcessed(true);
        marker.setInvulnerable(true);

        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteStream)) {
            objectOutputStream.writeObject(marker);
        }
        ObjectiveMarker restoredMarker;
        try (ObjectInputStream objectInputStream =
              new ObjectInputStream(new ByteArrayInputStream(byteStream.toByteArray()))) {
            restoredMarker = (ObjectiveMarker) objectInputStream.readObject();
        }

        assertEquals("Left Counter", restoredMarker.generalName());
        assertEquals(3, restoredMarker.getOwnerId());
        assertEquals(2, restoredMarker.getControlRadius());
        assertEquals(2, restoredMarker.getVictoryPointValue());
        assertTrue(restoredMarker.isPotential());
        assertTrue(restoredMarker.isConfirmed());
        assertTrue(restoredMarker.isFalseObjective());
        assertTrue(restoredMarker.isFragile());
        assertTrue(restoredMarker.isMobile());
        assertTrue(restoredMarker.isDestroyed());
        assertTrue(restoredMarker.isInvulnerable());
        assertTrue(restoredMarker.isInsideBuilding());
        assertTrue(restoredMarker.isBuildingLinkInitialized());
        assertTrue(restoredMarker.isDestructionProcessed());
    }
}

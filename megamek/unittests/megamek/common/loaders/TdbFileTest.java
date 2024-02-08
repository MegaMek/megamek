/*
 * Copyright (c) 2000-2004 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.loaders;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import megamek.common.BipedMech;
import megamek.common.Engine;
import megamek.common.Entity;
import megamek.common.Mech;
import megamek.common.Mounted;
import megamek.common.QuadMech;
import megamek.common.loaders.TdbFile.CriticalSlot;
import megamek.common.loaders.TdbFile.Location;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author nderwin
 */
public class TdbFileTest {

    @Test
    public void testJenner() throws Exception {
        InputStream is = getClass().getResourceAsStream("JR7-D Jenner.xml");
        assertNotNull(is);

        TdbFile testMe = TdbFile.getInstance(is);

        assertEquals(6, testMe.mounted.items.size());

        Entity result = testMe.getEntity();

        assertTrue(result instanceof BipedMech);
        BipedMech mek = (BipedMech) result;

        assertEquals("Jenner", mek.getChassis());
        assertEquals("JR7-D", mek.getModel());
        assertEquals(35.0f, mek.getWeight(), 0.0f);

        assertEquals(Mech.COCKPIT_STANDARD, mek.getCockpitType());
        assertEquals(Mech.GYRO_STANDARD, mek.getGyroType());

        assertTrue(mek.getEngine().engineValid);
        assertTrue(mek.getEngine().isFusion());
        assertEquals(Engine.NORMAL_ENGINE, mek.getEngine().getEngineType());
        assertEquals(7, mek.getWalkMP());
        assertEquals(11, mek.getRunMP());

        assertEquals(10, mek.getActiveSinks());

        assertEquals(7, mek.getArmor(Mech.LOC_HEAD));
        assertEquals(8, mek.getArmor(Mech.LOC_RT));
        assertEquals(10, mek.getArmor(Mech.LOC_CT));
        assertEquals(8, mek.getArmor(Mech.LOC_LT));
        assertEquals(4, mek.getArmor(Mech.LOC_RT, true));
        assertEquals(3, mek.getArmor(Mech.LOC_CT, true));
        assertEquals(4, mek.getArmor(Mech.LOC_LT, true));
        assertEquals(4, mek.getArmor(Mech.LOC_RARM));
        assertEquals(4, mek.getArmor(Mech.LOC_LARM));
        assertEquals(6, mek.getArmor(Mech.LOC_RLEG));
        assertEquals(6, mek.getArmor(Mech.LOC_LLEG));

        assertFalse(mek.getFailedEquipment().hasNext());

        boolean foundSRM = false;
        boolean foundAmmo = false;
        int mlRARM = 0;
        int mlLARM = 0;
        int jjCT = 0;
        int jjRT = 0;
        int jjLT = 0;

        for (Mounted m : mek.getEquipment()) {
            switch (m.getLocation()) {
                case Mech.LOC_CT:
                    if ("Jump Jet".equals(m.getName())) {
                        jjCT++;
                    }

                    if (!foundSRM && "SRM 4".equals(m.getName())) {
                        foundSRM = true;
                    } else if (foundSRM && "SRM 4".equals(m.getName())) {
                        fail("Only 1 SRM 4 in CT");
                    }

                    assertNotEquals("Medium Laser", m.getName(), "Found a Medium Laser in CT");
                    assertNotEquals("SRM 4 Ammo", m.getName(), "Found a SRM-4 ammo in CT");
                    assertFalse(m.getName().contains("Heat Sink"), "Found a Heat Sink in CT");
                    break;
                case Mech.LOC_RT:
                    if ("Jump Jet".equals(m.getName())) {
                        jjRT++;
                    }

                    if (!foundAmmo && "SRM 4 Ammo".equals(m.getName())) {
                        foundAmmo = true;
                    } else if (foundAmmo && "SRM 4 Ammo".equals(m.getName())) {
                        fail("Only 1 SRM 4 ammo in RT");
                    }

                    assertNotEquals("SRM 4", m.getName(), "Found a SRM-4 in RT");
                    assertNotEquals("Medium Laser", m.getName(), "Found a Medium Laser in RT");
                    break;
                case Mech.LOC_LT:
                    if ("Jump Jet".equals(m.getName())) {
                        jjLT++;
                    }

                    assertNotEquals("SRM 4", m.getName(), "Found a SRM-4 in LT");
                    assertNotEquals("Medium Laser", m.getName(), "Found a Medium Laser in LT");
                    assertNotEquals("SRM 4 Ammo", m.getName(), "Found a SRM-4 ammo in LT");
                    break;
                case Mech.LOC_RARM:
                    if ("Medium Laser".equals(m.getName())) {
                        mlRARM++;
                    }

                    assertNotEquals("SRM 4", m.getName(), "Found a SRM-4 in RARM");
                    assertNotEquals("Jump Jet", m.getName(), "Found a Jump Jet in RARM");
                    assertNotEquals("SRM 4 Ammo", m.getName(), "Found a SRM-4 ammo in RARM");
                    break;
                case Mech.LOC_LARM:
                    if ("Medium Laser".equals(m.getName())) {
                        mlLARM++;
                    }

                    assertNotEquals("SRM 4", m.getName(), "Found a SRM-4 in LARM");
                    assertNotEquals("Jump Jet", m.getName(), "Found a Jump Jet in LARM");
                    assertNotEquals("SRM 4 Ammo", m.getName(), "Found a SRM-4 ammo in LARM");
                    break;
            }
        }

        assertTrue(foundSRM, "Found SRM-4");
        assertTrue(foundAmmo, "Found SRM-4 ammo");
        assertEquals(1, jjCT, "Found 1 Jump Jet in CT");
        assertEquals(2, jjRT, "Found 2 Jump Jets in RT");
        assertEquals(2, jjLT, "Found 2 Jump Jets in LT");
        assertEquals(2, mlRARM, "Found 2 Medium Lasers in RARM");
        assertEquals(2, mlLARM, "Found 2 Medium Lasers in LARM");
    }

    @Test
    public void testTarantula() throws Exception {
        InputStream is = getClass().getResourceAsStream("ZPH-1A Tarantula.xml");
        assertNotNull(is);

        TdbFile testMe = TdbFile.getInstance(is);

        assertEquals(4, testMe.mounted.items.size());

        Entity result = testMe.getEntity();

        assertTrue(result instanceof QuadMech);
        QuadMech mek = (QuadMech) result;

        assertEquals("Tarantula", mek.getChassis());
        assertEquals("ZPH-1A", mek.getModel());
        assertEquals(25.0f, mek.getWeight(), 0.0f);

        assertEquals(Mech.COCKPIT_STANDARD, mek.getCockpitType());
        assertEquals(Mech.GYRO_STANDARD, mek.getGyroType());

        assertTrue(mek.getEngine().engineValid);
        assertTrue(mek.getEngine().isFusion());
        assertEquals(Engine.XL_ENGINE, mek.getEngine().getEngineType());
        assertEquals(8, mek.getWalkMP());
        assertEquals(12, mek.getRunMP());

        assertEquals(10, mek.getActiveSinks());

        assertEquals(6, mek.getArmor(Mech.LOC_HEAD));
        assertEquals(7, mek.getArmor(Mech.LOC_RT));
        assertEquals(10, mek.getArmor(Mech.LOC_CT));
        assertEquals(7, mek.getArmor(Mech.LOC_LT));
        assertEquals(4, mek.getArmor(Mech.LOC_RT, true));
        assertEquals(6, mek.getArmor(Mech.LOC_CT, true));
        assertEquals(4, mek.getArmor(Mech.LOC_LT, true));
        assertEquals(7, mek.getArmor(Mech.LOC_RARM));
        assertEquals(7, mek.getArmor(Mech.LOC_LARM));
        assertEquals(7, mek.getArmor(Mech.LOC_RLEG));
        assertEquals(7, mek.getArmor(Mech.LOC_LLEG));

        assertFalse(mek.getFailedEquipment().hasNext());

        boolean foundSSRM = false;
        boolean foundAmmo = false;
        int dblHS = 0;
        int mlRT = 0;
        int mlLT = 0;
        int jjFRL = 0;
        int jjFLL = 0;
        int jjRRL = 0;
        int jjRLL = 0;

        for (Mounted m : mek.getEquipment()) {
            switch (m.getLocation()) {
                case Mech.LOC_CT:
                    if (!foundSSRM && "Streak SRM 2".equals(m.getName())) {
                        foundSSRM = true;
                    } else if (foundSSRM && "Streak SRM 2".equals(m.getName())) {
                        fail("Only 1 SSRM-2 in CT");
                    }

                    if (!foundAmmo && "Streak SRM 2 Ammo".equals(m.getName())) {
                        foundAmmo = true;
                    } else if (foundAmmo && "Streak SRM 2 Ammo".equals(m.getName())) {
                        fail("Only 1 SSRM-2 ammo in CT");
                    }

                    assertNotEquals("Medium Laser", m.getName(), "Found a Medium Laser in CT");
                    assertNotEquals("Jump Jet", m.getName(), "Found a Jump Jet in CT");
                    assertFalse(m.getName().contains("Heat Sink"), "Found a Heat Sink in CT");
                    break;
                case Mech.LOC_RT:
                    if ("Double Heat Sink".equals(m.getName())) {
                        dblHS++;
                    }

                    if ("Medium Laser".equals(m.getName())) {
                        mlRT++;
                    }

                    assertNotEquals("Streak SRM 2", m.getName(), "Found a SSRM-2 in RT");
                    assertNotEquals("Streak SRM 2 Ammo", m.getName(), "Found SSRM-2 ammo in RT");
                    assertNotEquals("Jump Jet", m.getName(), "Found a Jump Jet in RT");
                    break;
                case Mech.LOC_LT:
                    if ("Double Heat Sink".equals(m.getName())) {
                        dblHS++;
                    }

                    if ("Medium Laser".equals(m.getName())) {
                        mlLT++;
                    }

                    assertNotEquals("Streak SRM 2", m.getName(), "Found a SSRM-2 in LT");
                    assertNotEquals("Streak SRM 2 Ammo", m.getName(), "Found SSRM-2 ammo in LT");
                    assertNotEquals("Jump Jet", m.getName(), "Found a Jump Jet in LT");
                    break;
                case Mech.LOC_RARM:
                    if ("Jump Jet".equals(m.getName())) {
                        jjFRL++;
                    }

                    assertFalse(m.getName().contains("Heat Sink"), "Found a Heat Sink in RARM");
                    assertNotEquals("Medium Laser", m.getName(), "Found a Medium Laser in RARM");
                    assertNotEquals("Streak SRM 2", m.getName(), "Found a SSRM-2 in RARM");
                    assertNotEquals("Streak SRM 2 Ammo", m.getName(), "Found SSRM-2 ammo in RARM");
                    break;
                case Mech.LOC_LARM:
                    if ("Jump Jet".equals(m.getName())) {
                        jjFLL++;
                    }

                    assertFalse(m.getName().contains("Heat Sink"), "Found a Heat Sink in LARM");
                    assertNotEquals("Medium Laser", m.getName(), "Found a Medium Laser in LARM");
                    assertNotEquals("Streak SRM 2", m.getName(), "Found a SSRM-2 in LARM");
                    assertNotEquals("Streak SRM 2 Ammo", m.getName(), "Found SSRM-2 ammo in LARM");
                    break;
                case Mech.LOC_RLEG:
                    if ("Jump Jet".equals(m.getName())) {
                        jjRRL++;
                    }

                    assertFalse(m.getName().contains("Heat Sink"), "Found a Heat Sink in RLEG");
                    assertNotEquals("Medium Laser", m.getName(), "Found a Medium Laser in RLEG");
                    assertNotEquals("Streak SRM 2", m.getName(), "Found a SSRM-2 in RLEG");
                    assertNotEquals("Streak SRM 2 Ammo", m.getName(), "Found SSRM-2 ammo in RLEG");
                    break;
                case Mech.LOC_LLEG:
                    if ("Jump Jet".equals(m.getName())) {
                        jjRLL++;
                    }

                    assertFalse(m.getName().contains("Heat Sink"), "Found a Heat Sink in LLEG");
                    assertNotEquals("Medium Laser", m.getName(), "Found a Medium Laser in LLEG");
                    assertNotEquals("Streak SRM 2", m.getName(), "Found a SSRM-2 in LLEG");
                    assertNotEquals("Streak SRM 2 Ammo", m.getName(), "Found SSRM-2 ammo in LLEG");
                    break;
            }
        }

        assertTrue(foundSSRM, "Found SSRM-2");
        assertTrue(foundAmmo, "Found SSRM-2 ammo");
        assertEquals(2, dblHS, "Double heat sinks");
        assertEquals(1, mlRT, "Found 1 Medium Laser in RT");
        assertEquals(1, mlLT, "Found 1 Medium Laser in LT");
        assertEquals(2, jjFRL, "Found 2 Jump Jets in RARM");
        assertEquals(2, jjFLL, "Found 2 Jump Jets in LARM");
        assertEquals(2, jjRRL, "Found 2 Jump Jets in RLEG");
        assertEquals(2, jjRLL, "Found 2 Jump Jets in LLEG");
    }

    @Test
    public void testLocationSorting() {
        List<Location> locations = new ArrayList<>();
        locations.add(new Location(Mech.LOC_LLEG, 1, 0));
        locations.add(new Location(Mech.LOC_RT, 1, 1));
        locations.add(new Location(Mech.LOC_HEAD, 1, 0));
        locations.add(new Location(Mech.LOC_CLEG, 1, 0));

        Collections.sort(locations);

        assertEquals(Mech.LOC_HEAD, locations.get(0).bodyPart.intValue());
        assertEquals(Mech.LOC_RT, locations.get(1).bodyPart.intValue());
        assertEquals(Mech.LOC_LLEG, locations.get(2).bodyPart.intValue());
        assertEquals(Mech.LOC_CLEG, locations.get(3).bodyPart.intValue());
    }

    @Test
    public void testCriticalSorting() {
        Location location = new Location(Mech.LOC_RLEG, 1, 0);
        location.criticalSlots.add(new CriticalSlot(0, "Empty"));
        location.criticalSlots.add(new CriticalSlot(1, "Hip"));
        location.criticalSlots.add(new CriticalSlot(2, "Empty"));
        location.criticalSlots.add(new CriticalSlot(3, "Upper Leg"));
        location.criticalSlots.add(new CriticalSlot(4, "Lower Leg"));
        location.criticalSlots.add(new CriticalSlot(5, "Foot"));

        Collections.sort(location.criticalSlots);

        assertEquals("Hip", location.criticalSlots.get(0).content);
        assertEquals("Upper Leg", location.criticalSlots.get(1).content);
        assertEquals("Lower Leg", location.criticalSlots.get(2).content);
        assertEquals("Foot", location.criticalSlots.get(3).content);
        assertEquals("Empty", location.criticalSlots.get(4).content);
        assertEquals("Empty", location.criticalSlots.get(5).content);
    }
}

/*
 * MegaMek - Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
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
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author nderwin
 */
public class TdbFileTest {

    private int[] locations = new int[] {
        Mech.LOC_HEAD, 
        Mech.LOC_CT, 
        Mech.LOC_RT, 
        Mech.LOC_LT, 
        Mech.LOC_RARM, 
        Mech.LOC_LARM, 
        Mech.LOC_RLEG, 
        Mech.LOC_LLEG
    };
    
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
                        assertTrue("Only 1 SRM 4 in CT", false);
                    }
                    
                    assertFalse("Found a Medium Laser in CT", "Medium Laser".equals(m.getName()));
                    assertFalse("Found a SRM-4 ammo in CT", "SRM 4 Ammo".equals(m.getName()));
                    assertFalse("Found a Heat Sink in CT", m.getName().contains("Heat Sink"));
                    break;
                case Mech.LOC_RT:
                    if ("Jump Jet".equals(m.getName())) {
                        jjRT++;
                    }
                    
                    if (!foundAmmo && "SRM 4 Ammo".equals(m.getName())) {
                        foundAmmo = true;
                    } else if (foundAmmo && "SRM 4 Ammo".equals(m.getName())) {
                        assertTrue("Only 1 SRM 4 ammo in RT", false);
                    }
                    
                    assertFalse("Found a SRM-4 in RT", "SRM 4".equals(m.getName()));
                    assertFalse("Found a Medium Laser in RT", "Medium Laser".equals(m.getName()));
                    break;
                case Mech.LOC_LT:
                    if ("Jump Jet".equals(m.getName())) {
                        jjLT++;
                    }
                    
                    assertFalse("Found a SRM-4 in LT", "SRM 4".equals(m.getName()));
                    assertFalse("Found a Medium Laser in LT", "Medium Laser".equals(m.getName()));
                    assertFalse("Found a SRM-4 ammo in LT", "SRM 4 Ammo".equals(m.getName()));
                    break;
                case Mech.LOC_RARM:
                    if ("Medium Laser".equals(m.getName())) {
                        mlRARM++;
                    }
                    
                    assertFalse("Found a SRM-4 in RARM", "SRM 4".equals(m.getName()));
                    assertFalse("Found a Jump Jet in RARM", "Jump Jet".equals(m.getName()));
                    assertFalse("Found a SRM-4 ammo in RARM", "SRM 4 Ammo".equals(m.getName()));
                    break;
                case Mech.LOC_LARM:
                    if ("Medium Laser".equals(m.getName())) {
                        mlLARM++;
                    }
                    
                    assertFalse("Found a SRM-4 in LARM", "SRM 4".equals(m.getName()));
                    assertFalse("Found a Jump Jet in LARM", "Jump Jet".equals(m.getName()));
                    assertFalse("Found a SRM-4 ammo in LARM", "SRM 4 Ammo".equals(m.getName()));
                    break;
            }
        }
        
        assertTrue("Found SRM-4", foundSRM);
        assertTrue("Found SRM-4 ammo", foundAmmo);
        assertEquals("Found 1 Jump Jet in CT", 1, jjCT);
        assertEquals("Found 2 Jump Jets in RT", 2, jjRT);
        assertEquals("Found 2 Jump Jets in LT", 2, jjLT);
        assertEquals("Found 2 Medium Lasers in RARM", 2, mlRARM);
        assertEquals("Found 2 Medium Lasers in LARM", 2, mlLARM);
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
                        assertTrue("Only 1 SSRM-2 in CT", false);
                    }
                    
                    if (!foundAmmo && "Streak SRM 2 Ammo".equals(m.getName())) {
                        foundAmmo = true;
                    } else if (foundAmmo && "Streak SRM 2 Ammo".equals(m.getName())) {
                        assertTrue("Only 1 SSRM-2 ammo in CT", false);
                    }
                    
                    assertFalse("Found a Medium Laser in CT", "Medium Laser".equals(m.getName()));
                    assertFalse("Found a Jump Jet in CT", "Jump Jet".equals(m.getName()));
                    assertFalse("Found a Heat Sink in CT", m.getName().contains("Heat Sink"));
                    break;
                case Mech.LOC_RT:
                    if ("Double Heat Sink".equals(m.getName())) {
                        dblHS++;
                    }
                    
                    if ("Medium Laser".equals(m.getName())) {
                        mlRT++;
                    }
                    
                    assertFalse("Found a SSRM-2 in RT", "Streak SRM 2".equals(m.getName()));
                    assertFalse("Found SSRM-2 ammo in RT", "Streak SRM 2 Ammo".equals(m.getName()));
                    assertFalse("Found a Jump Jet in RT", "Jump Jet".equals(m.getName()));
                    break;
                case Mech.LOC_LT:
                    if ("Double Heat Sink".equals(m.getName())) {
                        dblHS++;
                    }
                    
                    if ("Medium Laser".equals(m.getName())) {
                        mlLT++;
                    }
                    
                    assertFalse("Found a SSRM-2 in LT", "Streak SRM 2".equals(m.getName()));
                    assertFalse("Found SSRM-2 ammo in LT", "Streak SRM 2 Ammo".equals(m.getName()));
                    assertFalse("Found a Jump Jet in LT", "Jump Jet".equals(m.getName()));
                    break;
                case Mech.LOC_RARM:
                    if ("Jump Jet".equals(m.getName())) {
                        jjFRL++;
                    }
                    
                    assertFalse("Found a Heat Sink in RARM", m.getName().contains("Heat Sink"));
                    assertFalse("Found a Medium Laser in RARM", "Medium Laser".equals(m.getName()));
                    assertFalse("Found a SSRM-2 in RARM", "Streak SRM 2".equals(m.getName()));
                    assertFalse("Found SSRM-2 ammo in RARM", "Streak SRM 2 Ammo".equals(m.getName()));
                    break;
                case Mech.LOC_LARM:
                    if ("Jump Jet".equals(m.getName())) {
                        jjFLL++;
                    }
                    
                    assertFalse("Found a Heat Sink in LARM", m.getName().contains("Heat Sink"));
                    assertFalse("Found a Medium Laser in LARM", "Medium Laser".equals(m.getName()));
                    assertFalse("Found a SSRM-2 in LARM", "Streak SRM 2".equals(m.getName()));
                    assertFalse("Found SSRM-2 ammo in LARM", "Streak SRM 2 Ammo".equals(m.getName()));
                    break;
                case Mech.LOC_RLEG:
                    if ("Jump Jet".equals(m.getName())) {
                        jjRRL++;
                    }
                    
                    assertFalse("Found a Heat Sink in RLEG", m.getName().contains("Heat Sink"));
                    assertFalse("Found a Medium Laser in RLEG", "Medium Laser".equals(m.getName()));
                    assertFalse("Found a SSRM-2 in RLEG", "Streak SRM 2".equals(m.getName()));
                    assertFalse("Found SSRM-2 ammo in RLEG", "Streak SRM 2 Ammo".equals(m.getName()));
                    break;
                case Mech.LOC_LLEG:
                    if ("Jump Jet".equals(m.getName())) {
                        jjRLL++;
                    }
                    
                    assertFalse("Found a Heat Sink in LLEG", m.getName().contains("Heat Sink"));
                    assertFalse("Found a Medium Laser in LLEG", "Medium Laser".equals(m.getName()));
                    assertFalse("Found a SSRM-2 in LLEG", "Streak SRM 2".equals(m.getName()));
                    assertFalse("Found SSRM-2 ammo in LLEG", "Streak SRM 2 Ammo".equals(m.getName()));
                    break;
            }
        }
        
        assertTrue("Found SSRM-2", foundSSRM);
        assertTrue("Found SSRM-2 ammo", foundAmmo);
        assertEquals("Double heat sinks", 2, dblHS);
        assertEquals("Found 1 Medium Laser in RT", 1, mlRT);
        assertEquals("Found 1 Medium Laser in LT", 1, mlLT);
        assertEquals("Found 2 Jump Jets in RARM", 2, jjFRL);
        assertEquals("Found 2 Jump Jets in LARM", 2, jjFLL);
        assertEquals("Found 2 Jump Jets in RLEG", 2, jjRRL);
        assertEquals("Found 2 Jump Jets in LLEG", 2, jjRLL);
    }
    
    @Test
    public void testLocationSorting() {
        List<TdbFile.Location> locations = new ArrayList<>();
        locations.add(new TdbFile.Location(Mech.LOC_LLEG, 1, 0));
        locations.add(new TdbFile.Location(Mech.LOC_RT, 1, 1));
        locations.add(new TdbFile.Location(Mech.LOC_HEAD, 1, 0));
        locations.add(new TdbFile.Location(Mech.LOC_CLEG, 1, 0));
        
        Collections.sort(locations);
        
        assertEquals(Mech.LOC_HEAD, locations.get(0).bodyPart.intValue());
        assertEquals(Mech.LOC_RT, locations.get(1).bodyPart.intValue());
        assertEquals(Mech.LOC_LLEG, locations.get(2).bodyPart.intValue());
        assertEquals(Mech.LOC_CLEG, locations.get(3).bodyPart.intValue());
    }
    
    @Test
    public void testCriticalSorting() {
        TdbFile.Location location = new TdbFile.Location(Mech.LOC_RLEG, 1, 0);
        location.criticalSlots.add(new TdbFile.CriticalSlot(0, "Empty"));
        location.criticalSlots.add(new TdbFile.CriticalSlot(1, "Hip"));
        location.criticalSlots.add(new TdbFile.CriticalSlot(2, "Empty"));
        location.criticalSlots.add(new TdbFile.CriticalSlot(3, "Upper Leg"));
        location.criticalSlots.add(new TdbFile.CriticalSlot(4, "Lower Leg"));
        location.criticalSlots.add(new TdbFile.CriticalSlot(5, "Foot"));
        
        Collections.sort(location.criticalSlots);
        
        assertEquals("Hip", location.criticalSlots.get(0).content);
        assertEquals("Upper Leg", location.criticalSlots.get(1).content);
        assertEquals("Lower Leg", location.criticalSlots.get(2).content);
        assertEquals("Foot", location.criticalSlots.get(3).content);
        assertEquals("Empty", location.criticalSlots.get(4).content);
        assertEquals("Empty", location.criticalSlots.get(5).content);
    }
}

/*
 * Copyright (c) 2018-2022 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common.verifier;

import megamek.common.*;
import megamek.common.verifier.TestEntity.Ceil;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestProtomechTest {
    
    private Protomech createGenericMockProto() {
        Protomech mockProto = mock(Protomech.class);
        when(mockProto.hasMainGun()).thenReturn(true);
        when(mockProto.locations()).thenReturn(Protomech.NUM_PMECH_LOCATIONS);
        when(mockProto.getArmorType(anyInt())).thenReturn(EquipmentType.T_ARMOR_STANDARD);
        when(mockProto.getArmorTechLevel(anyInt())).thenReturn(TechConstants.T_CLAN_EXPERIMENTAL);
        return mockProto;
    }
    
    private TestEntityOption option = new TestEntityOption() {

        @Override
        public Ceil getWeightCeilingEngine() {
            return Ceil.KILO;
        }

        @Override
        public Ceil getWeightCeilingStructure() {
            return Ceil.KILO;
        }

        @Override
        public Ceil getWeightCeilingArmor() {
            return Ceil.KILO;
        }

        @Override
        public Ceil getWeightCeilingControls() {
            return Ceil.KILO;
        }

        @Override
        public Ceil getWeightCeilingWeapons() {
            return Ceil.KILO;
        }

        @Override
        public Ceil getWeightCeilingTargComp() {
            return Ceil.KILO;
        }

        @Override
        public Ceil getWeightCeilingGyro() {
            return Ceil.KILO;
        }

        @Override
        public Ceil getWeightCeilingTurret() {
            return Ceil.KILO;
        }

        @Override
        public Ceil getWeightCeilingLifting() {
            return Ceil.KILO;
        }

        @Override
        public Ceil getWeightCeilingPowerAmp() {
            return Ceil.KILO;
        }

        @Override
        public double getMaxOverweight() {
            return 0.0;
        }

        @Override
        public boolean showOverweightedEntity() {
            return true;
        }

        @Override
        public boolean showUnderweightedEntity() {
            return false;
        }

        @Override
        public boolean showCorrectArmor() {
            return true;
        }

        @Override
        public boolean showCorrectCritical() {
            return true;
        }

        @Override
        public boolean showFailedEquip() {
            return true;
        }

        @Override
        public boolean showIncorrectIntroYear() {
            return false;
        }

        @Override
        public int getIntroYearMargin() {
            return 0;
        }

        @Override
        public double getMinUnderweight() {
            return 0.0;
        }

        @Override
        public boolean ignoreFailedEquip(String name) {
            return false;
        }

        @Override
        public boolean skip() {
            return false;
        }

        @Override
        public int getTargCompCrits() {
            return 0;
        }

        @Override
        public int getPrintSize() {
            return 0;
        }
    };
    
    @Test
    public void testCalcEngineRating() {
        Protomech mockProto = mock(Protomech.class);
        when(mockProto.getWeight()).thenReturn(6.0);
        // walking 6
        when(mockProto.getOriginalWalkMP()).thenReturn(4);
        
        assertEquals(TestProtomech.calcEngineRating(mockProto), 36);
    }
    
    @Test
    public void testCalcEngineRatingGliderEfficiency() {
        Protomech mockProto = mock(Protomech.class);
        when(mockProto.getWeight()).thenReturn(6.0);
        when(mockProto.isGlider()).thenReturn(true);
        // running 6, engine rating calculated as running - 2
        when(mockProto.getOriginalWalkMP()).thenReturn(4);
        
        assertEquals(TestProtomech.calcEngineRating(mockProto), 24);
    }
    
    @Test
    public void testCalcEngineRatingQuadEfficiency() {
        Protomech mockProto = mock(Protomech.class);
        when(mockProto.getWeight()).thenReturn(6.0);
        when(mockProto.isQuad()).thenReturn(true);
        // running 6, engine rating calculated as running - 2
        when(mockProto.getOriginalWalkMP()).thenReturn(4);
        
        assertEquals(TestProtomech.calcEngineRating(mockProto), 24);
    }
    
    @Test
    public void testEngineWeight() {
        Entity proto = new Protomech();
        Entity nonProto = new BipedMech();
        Engine engine45 = new Engine(45, Engine.NORMAL_ENGINE, Engine.CLAN_ENGINE);
        Engine engine42 = new Engine(42, Engine.NORMAL_ENGINE, Engine.CLAN_ENGINE);
        Engine engine40 = new Engine(40, Engine.NORMAL_ENGINE, Engine.CLAN_ENGINE);
        Engine engine35 = new Engine(35, Engine.NORMAL_ENGINE, Engine.CLAN_ENGINE);
        
        assertEquals(engine42.getWeightEngine(proto, RoundWeight.STANDARD),
                engine45.getWeightEngine(nonProto), 0.001);
        assertEquals(engine40.getWeightEngine(proto, RoundWeight.STANDARD),
                engine40.getWeightEngine(nonProto), 0.001);
        assertTrue(engine35.getWeightEngine(proto,
                RoundWeight.STANDARD) < engine35.getWeightEngine(nonProto));
    }

    @Test
    public void testMaxArmorFactor() {
        // Beginning of non-ultra range
        assertEquals(TestProtomech.maxArmorFactor(2.0, false), 15);
        assertEquals(TestProtomech.maxArmorFactor(2.0, true), 18);
        // End of non-ultra range
        assertEquals(TestProtomech.maxArmorFactor(9.0, false), 42);
        assertEquals(TestProtomech.maxArmorFactor(9.0, true), 45);
        // Beginning of ultra range
        assertEquals(TestProtomech.maxArmorFactor(10.0, false), 51);
        assertEquals(TestProtomech.maxArmorFactor(10.0, true), 57);
        // End of ultra range
        assertEquals(TestProtomech.maxArmorFactor(15.0, false), 67);
        assertEquals(TestProtomech.maxArmorFactor(15.0, true), 73);
    }
    
    @Test
    public void testMaxArmorPasses() {
        Protomech mockProto = mock(Protomech.class);
        when(mockProto.getWeight()).thenReturn(5.0);
        when(mockProto.hasMainGun()).thenReturn(false);
        int max = TestProtomech.maxArmorFactor(5.0, false);
        when(mockProto.getTotalArmor()).thenReturn(max);
        TestProtomech test = new TestProtomech(mockProto, option, null);
        
        assertTrue(test.correctArmor(new StringBuffer()));
    }
    
    @Test
    public void testExcessArmorFails() {
        Protomech mockProto = mock(Protomech.class);
        when(mockProto.getWeight()).thenReturn(5.0);
        when(mockProto.hasMainGun()).thenReturn(false);
        when(mockProto.getOArmor(anyInt())).thenAnswer(inv -> TestProtomech.maxArmorFactor(mockProto, inv.getArgument(0)) + 1);
        when(mockProto.locations()).thenReturn(Protomech.NUM_PMECH_LOCATIONS);
        TestProtomech test = new TestProtomech(mockProto, option, null);
        
        assertFalse(test.correctArmor(new StringBuffer()));
    }
    
    @Test
    public void testGliderRequires4MP() {
        Protomech mockProto = mock(Protomech.class);
        when(mockProto.getOriginalWalkMP()).thenReturn(3);
        when(mockProto.isGlider()).thenReturn(true);
        TestProtomech test = new TestProtomech(mockProto, option, null);
        
        assertFalse(test.correctMovement(new StringBuffer()));
    }
    
    @Test
    public void testQuadRequires3MP() {
        Protomech mockProto = mock(Protomech.class);
        when(mockProto.getOriginalWalkMP()).thenReturn(2);
        when(mockProto.isGlider()).thenReturn(false);
        when(mockProto.isQuad()).thenReturn(true);
        TestProtomech test = new TestProtomech(mockProto, option, null);
        
        assertFalse(test.correctMovement(new StringBuffer()));
    }
    
    @Test
    public void testExcessWeight() {
        Protomech mockProto = createGenericMockProto();
        Engine engine = new Engine(30, Engine.NORMAL_ENGINE, Engine.CLAN_ENGINE);
        when(mockProto.getEngine()).thenReturn(engine);
        double engineWeight = engine.getWeightEngine(mockProto);
        when(mockProto.getWeight()).thenReturn(engineWeight);
        TestProtomech test = new TestProtomech(mockProto, option, null);
        
        assertFalse(test.correctWeight(new StringBuffer()));
    }
    
    @Test
    public void testMaxWeight() {
        Protomech mockProto = createGenericMockProto();
        Engine engine = new Engine(30, Engine.NORMAL_ENGINE, Engine.CLAN_ENGINE);
        when(mockProto.getEngine()).thenReturn(engine);
        when(mockProto.getWeight()).thenReturn(TestProtomech.MAX_TONNAGE + 1);
        TestProtomech test = new TestProtomech(mockProto, option, null);
        
        assertFalse(test.correctWeight(new StringBuffer()));
    }
    
    @Test
    public void testQuadGliderFails() {
        Protomech mockProto = createGenericMockProto();
        when(mockProto.isGlider()).thenReturn(true);
        when(mockProto.isQuad()).thenReturn(true);
        TestProtomech test = new TestProtomech(mockProto, option, null);
        
        assertTrue(test.hasIllegalEquipmentCombinations(new StringBuffer()));
    }
    
    @Test
    public void testExcessiveSlots() {
        Protomech mockProto = createGenericMockProto();
        Mounted m = new Mounted(mockProto, EquipmentType.get("CLERSmallLaser"));
        m.setLocation(Protomech.LOC_TORSO);
        ArrayList<Mounted> eqList = new ArrayList<>();
        eqList.add(m);
        when(mockProto.getEquipment()).thenReturn(eqList);
        TestProtomech test = new TestProtomech(mockProto, option, null);
        
        assertFalse(test.hasIllegalEquipmentCombinations(new StringBuffer()));
        m.setLocation(Protomech.LOC_HEAD);
        assertTrue(test.hasIllegalEquipmentCombinations(new StringBuffer()));
    }

    @Test
    public void testNoArmMountsForQuads() {
        Protomech mockProto = createGenericMockProto();
        when(mockProto.isQuad()).thenReturn(true);
        Mounted m = new Mounted(mockProto, EquipmentType.get("CLERSmallLaser"));
        m.setLocation(Protomech.LOC_LARM);
        ArrayList<Mounted> eqList = new ArrayList<>();
        eqList.add(m);
        when(mockProto.getEquipment()).thenReturn(eqList);
        TestProtomech test = new TestProtomech(mockProto, option, null);
        
        assertTrue(test.hasIllegalEquipmentCombinations(new StringBuffer()));
    }
    
    @Test
    public void testEDPArmorTakesTorsoSlot() {
        Protomech mockProto = mock(Protomech.class);
        when(mockProto.hasMainGun()).thenReturn(true);
        when(mockProto.locations()).thenReturn(Protomech.NUM_PMECH_LOCATIONS);
        when(mockProto.getArmorType(anyInt())).thenReturn(EquipmentType.T_ARMOR_EDP);
        when(mockProto.getArmorTechLevel(anyInt())).thenReturn(TechConstants.T_CLAN_EXPERIMENTAL);
        Mounted m = new Mounted(mockProto, EquipmentType.get("CLERSmallLaser"));
        m.setLocation(Protomech.LOC_TORSO);
        ArrayList<Mounted> eqList = new ArrayList<>();
        eqList.add(m);
        when(mockProto.getEquipment()).thenReturn(eqList);
        TestProtomech test = new TestProtomech(mockProto, option, null);
        
        assertFalse(test.hasIllegalEquipmentCombinations(new StringBuffer()));
        eqList.add(m);
        assertTrue(test.hasIllegalEquipmentCombinations(new StringBuffer()));
    }
    
    @Test
    public void testRearMountTorsoOnly() {
        Protomech mockProto = createGenericMockProto();
        Mounted m = new Mounted(mockProto, EquipmentType.get("CLERSmallLaser"));
        ArrayList<Mounted> eqList = new ArrayList<>();
        eqList.add(m);
        when(mockProto.getEquipment()).thenReturn(eqList);
        TestProtomech test = new TestProtomech(mockProto, option, null);
        
        m.setLocation(Protomech.LOC_TORSO, true);
        assertFalse(test.hasIllegalEquipmentCombinations(new StringBuffer()));
        m.setLocation(Protomech.LOC_LARM, true);
        assertTrue(test.hasIllegalEquipmentCombinations(new StringBuffer()));
        m.setLocation(Protomech.LOC_RARM, true);
        assertTrue(test.hasIllegalEquipmentCombinations(new StringBuffer()));
        m.setLocation(Protomech.LOC_MAINGUN, true);
        assertTrue(test.hasIllegalEquipmentCombinations(new StringBuffer()));
    }

    @Test
    public void testIllegalArmor() {
        Protomech mockProto = mock(Protomech.class);
        when(mockProto.hasMainGun()).thenReturn(true);
        when(mockProto.locations()).thenReturn(Protomech.NUM_PMECH_LOCATIONS);
        when(mockProto.getArmorType(anyInt())).thenReturn(EquipmentType.T_ARMOR_FERRO_FIBROUS);
        when(mockProto.getArmorTechLevel(anyInt())).thenReturn(TechConstants.T_CLAN_EXPERIMENTAL);
        TestProtomech test = new TestProtomech(mockProto, option, null);
        
        assertTrue(test.hasIllegalEquipmentCombinations(new StringBuffer()));
    }
    
    @Test
    public void testHeatSinkCount() {
        Protomech mockProto = createGenericMockProto();
        Mounted laser = new Mounted(mockProto, EquipmentType.get("CLERSmallLaser"));
        laser.setLocation(Protomech.LOC_TORSO);
        ArrayList<Mounted> eqList = new ArrayList<>();
        eqList.add(laser);
        when(mockProto.getEquipment()).thenReturn(eqList);
        when(mockProto.getWeaponList()).thenReturn(eqList);
        TestProtomech test = new TestProtomech(mockProto, option, null);
        
        assertEquals(test.getCountHeatSinks(), ((WeaponType) laser.getType()).getHeat());
        eqList.add(new Mounted(mockProto, EquipmentType.get("CLUltraAC5")));
        assertEquals(test.getCountHeatSinks(), ((WeaponType) laser.getType()).getHeat());
    }
}

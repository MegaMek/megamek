/*
 * Copyright (c) 2018-2022 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.verifier;

import megamek.common.*;
import megamek.common.verifier.TestEntity.Ceil;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestProtoMekTest {
    
    private Protomech createGenericMockProtoMek() {
        Protomech mockProtoMek = mock(Protomech.class);
        when(mockProtoMek.hasMainGun()).thenReturn(true);
        when(mockProtoMek.locations()).thenReturn(Protomech.NUM_PMECH_LOCATIONS);
        when(mockProtoMek.getArmorType(anyInt())).thenReturn(EquipmentType.T_ARMOR_STANDARD);
        when(mockProtoMek.getArmorTechLevel(anyInt())).thenReturn(TechConstants.T_CLAN_EXPERIMENTAL);
        when(mockProtoMek.getMovementMode()).thenReturn(EntityMovementMode.BIPED);
        return mockProtoMek;
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
        Protomech mockProtoMek = mock(Protomech.class);
        when(mockProtoMek.getWeight()).thenReturn(6.0);
        // walking 6
        when(mockProtoMek.getOriginalWalkMP()).thenReturn(4);
        
        assertEquals(TestProtomech.calcEngineRating(mockProtoMek), 36);
    }
    
    @Test
    public void testCalcEngineRatingGliderEfficiency() {
        Protomech mockProtoMek = mock(Protomech.class);
        when(mockProtoMek.getWeight()).thenReturn(6.0);
        when(mockProtoMek.isGlider()).thenReturn(true);
        // running 6, engine rating calculated as running - 2
        when(mockProtoMek.getOriginalWalkMP()).thenReturn(4);
        
        assertEquals(TestProtomech.calcEngineRating(mockProtoMek), 24);
    }
    
    @Test
    public void testCalcEngineRatingQuadEfficiency() {
        Protomech mockProtoMek = mock(Protomech.class);
        when(mockProtoMek.getWeight()).thenReturn(6.0);
        when(mockProtoMek.isQuad()).thenReturn(true);
        // running 6, engine rating calculated as running - 2
        when(mockProtoMek.getOriginalWalkMP()).thenReturn(4);
        
        assertEquals(TestProtomech.calcEngineRating(mockProtoMek), 24);
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
        Protomech mockProtoMek = mock(Protomech.class);
        when(mockProtoMek.getWeight()).thenReturn(5.0);
        when(mockProtoMek.hasMainGun()).thenReturn(false);
        int max = TestProtomech.maxArmorFactor(5.0, false);
        when(mockProtoMek.getTotalArmor()).thenReturn(max);
        TestProtomech test = new TestProtomech(mockProtoMek, option, null);

        assertTrue(test.correctArmor(new StringBuffer()));
    }

    @Test
    public void testExcessArmorFails() {
        Protomech mockProtoMek = mock(Protomech.class);
        when(mockProtoMek.getWeight()).thenReturn(5.0);
        when(mockProtoMek.hasMainGun()).thenReturn(false);
        when(mockProtoMek.getOArmor(anyInt())).thenAnswer(inv -> TestProtomech.maxArmorFactor(mockProtoMek, inv.getArgument(0)) + 1);
        when(mockProtoMek.locations()).thenReturn(Protomech.NUM_PMECH_LOCATIONS);
        TestProtomech test = new TestProtomech(mockProtoMek, option, null);

        assertFalse(test.correctArmor(new StringBuffer()));
    }

    @Test
    public void testGliderRequires4MP() {
        Protomech mockProtoMek = mock(Protomech.class);
        when(mockProtoMek.getOriginalWalkMP()).thenReturn(3);
        when(mockProtoMek.isGlider()).thenReturn(true);
        TestProtomech test = new TestProtomech(mockProtoMek, option, null);

        assertFalse(test.correctMovement(new StringBuffer()));
    }

    @Test
    public void testQuadRequires3MP() {
        Protomech mockProtoMek = mock(Protomech.class);
        when(mockProtoMek.getOriginalWalkMP()).thenReturn(2);
        when(mockProtoMek.isGlider()).thenReturn(false);
        when(mockProtoMek.isQuad()).thenReturn(true);
        TestProtomech test = new TestProtomech(mockProtoMek, option, null);

        assertFalse(test.correctMovement(new StringBuffer()));
    }

    @Test
    public void testExcessWeight() {
        Protomech mockProtoMek = createGenericMockProtoMek();
        Engine engine = new Engine(30, Engine.NORMAL_ENGINE, Engine.CLAN_ENGINE);
        when(mockProtoMek.getEngine()).thenReturn(engine);
        double engineWeight = engine.getWeightEngine(mockProtoMek);
        when(mockProtoMek.getWeight()).thenReturn(engineWeight);
        TestProtomech test = new TestProtomech(mockProtoMek, option, null);

        assertFalse(test.correctWeight(new StringBuffer()));
    }

    @Test
    public void testMaxWeight() {
        Protomech mockProtoMek = createGenericMockProtoMek();
        Engine engine = new Engine(30, Engine.NORMAL_ENGINE, Engine.CLAN_ENGINE);
        when(mockProtoMek.getEngine()).thenReturn(engine);
        when(mockProtoMek.getWeight()).thenReturn(TestProtomech.MAX_TONNAGE + 1);
        TestProtomech test = new TestProtomech(mockProtoMek, option, null);

        assertFalse(test.correctWeight(new StringBuffer()));
    }

    @Test
    public void testQuadGliderFails() {
        Protomech mockProtoMek = createGenericMockProtoMek();
        when(mockProtoMek.isGlider()).thenReturn(true);
        when(mockProtoMek.isQuad()).thenReturn(true);
        TestProtomech test = new TestProtomech(mockProtoMek, option, null);

        assertTrue(test.hasIllegalEquipmentCombinations(new StringBuffer()));
    }

    @Test
    public void testExcessiveSlots() {
        Protomech mockProtoMek = createGenericMockProtoMek();
        Mounted m = new Mounted(mockProtoMek, EquipmentType.get("CLERSmallLaser"));
        m.setLocation(Protomech.LOC_TORSO);
        ArrayList<Mounted> eqList = new ArrayList<>();
        eqList.add(m);
        when(mockProtoMek.getEquipment()).thenReturn(eqList);
        TestProtomech test = new TestProtomech(mockProtoMek, option, null);

        assertFalse(test.hasIllegalEquipmentCombinations(new StringBuffer()));
        m.setLocation(Protomech.LOC_HEAD);
        assertTrue(test.hasIllegalEquipmentCombinations(new StringBuffer()));
    }

    @Test
    public void testNoArmMountsForQuads() {
        Protomech mockProtoMek = createGenericMockProtoMek();
        when(mockProtoMek.isQuad()).thenReturn(true);
        Mounted m = new Mounted(mockProtoMek, EquipmentType.get("CLERSmallLaser"));
        m.setLocation(Protomech.LOC_LARM);
        ArrayList<Mounted> eqList = new ArrayList<>();
        eqList.add(m);
        when(mockProtoMek.getEquipment()).thenReturn(eqList);
        TestProtomech test = new TestProtomech(mockProtoMek, option, null);

        assertTrue(test.hasIllegalEquipmentCombinations(new StringBuffer()));
    }

    @Test
    public void testEDPArmorTakesTorsoSlot() {
        Protomech mockProtoMek = mock(Protomech.class);
        when(mockProtoMek.hasMainGun()).thenReturn(true);
        when(mockProtoMek.locations()).thenReturn(Protomech.NUM_PMECH_LOCATIONS);
        when(mockProtoMek.getArmorType(anyInt())).thenReturn(EquipmentType.T_ARMOR_EDP);
        when(mockProtoMek.getArmorTechLevel(anyInt())).thenReturn(TechConstants.T_CLAN_EXPERIMENTAL);
        Mounted m = new Mounted(mockProtoMek, EquipmentType.get("CLERSmallLaser"));
        m.setLocation(Protomech.LOC_TORSO);
        ArrayList<Mounted> eqList = new ArrayList<>();
        eqList.add(m);
        when(mockProtoMek.getEquipment()).thenReturn(eqList);
        TestProtomech test = new TestProtomech(mockProtoMek, option, null);

        assertFalse(test.hasIllegalEquipmentCombinations(new StringBuffer()));
        eqList.add(m);
        assertTrue(test.hasIllegalEquipmentCombinations(new StringBuffer()));
    }

    @Test
    public void testRearMountTorsoOnly() {
        Protomech mockProtoMek = createGenericMockProtoMek();
        Mounted m = new Mounted(mockProtoMek, EquipmentType.get("CLERSmallLaser"));
        ArrayList<Mounted> eqList = new ArrayList<>();
        eqList.add(m);
        when(mockProtoMek.getEquipment()).thenReturn(eqList);
        TestProtomech test = new TestProtomech(mockProtoMek, option, null);

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
        Protomech mockProtoMek = mock(Protomech.class);
        when(mockProtoMek.hasMainGun()).thenReturn(true);
        when(mockProtoMek.locations()).thenReturn(Protomech.NUM_PMECH_LOCATIONS);
        when(mockProtoMek.getArmorType(anyInt())).thenReturn(EquipmentType.T_ARMOR_FERRO_FIBROUS);
        when(mockProtoMek.getArmorTechLevel(anyInt())).thenReturn(TechConstants.T_CLAN_EXPERIMENTAL);
        TestProtomech test = new TestProtomech(mockProtoMek, option, null);

        assertTrue(test.hasIllegalEquipmentCombinations(new StringBuffer()));
    }

    @Test
    public void testHeatSinkCount() {
        Protomech mockProtoMek = createGenericMockProtoMek();
        Mounted laser = new Mounted(mockProtoMek, EquipmentType.get("CLERSmallLaser"));
        laser.setLocation(Protomech.LOC_TORSO);
        ArrayList<Mounted> eqList = new ArrayList<>();
        eqList.add(laser);
        when(mockProtoMek.getEquipment()).thenReturn(eqList);
        when(mockProtoMek.getWeaponList()).thenReturn(eqList);
        TestProtomech test = new TestProtomech(mockProtoMek, option, null);

        assertEquals(test.getCountHeatSinks(), laser.getType().getHeat());
        eqList.add(new Mounted(mockProtoMek, EquipmentType.get("CLUltraAC5")));
        assertEquals(test.getCountHeatSinks(), laser.getType().getHeat());
    }
}

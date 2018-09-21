/*
 * Copyright (C) 2018 - The MegaMek Team
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
package megamek.common.verifier;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Test;

import megamek.common.Engine;
import megamek.common.Entity;
import megamek.common.Protomech;

public class TestProtomechTest {
    
    @Test
    public void testEngineWeight() {
        Entity mockProto = mock(Entity.class);
        when(mockProto.hasETypeFlag(anyLong()))
            .thenAnswer(inv -> (((Long) inv.getArgument(0)) & Entity.ETYPE_PROTOMECH) != 0);
        Entity mockNonProto = mock(Entity.class);
        when(mockNonProto.hasETypeFlag(anyLong())).thenReturn(false);
        Engine engine40 = new Engine(40, Engine.NORMAL_ENGINE, Engine.CLAN_ENGINE);
        Engine engine35 = new Engine(35, Engine.NORMAL_ENGINE, Engine.CLAN_ENGINE);
        
        assertEquals(engine40.getWeightEngine(mockProto), engine40.getWeightEngine(mockNonProto), 0.001);
        assertTrue(engine35.getWeightEngine(mockProto) < engine35.getWeightEngine(mockNonProto));
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

}

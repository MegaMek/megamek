/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
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
package megamek.common.options;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.*;

/**
 *
 * @author nderwin
 */
public class GameOptionsTest {
    
    private GameOptions testMe;
    
    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();
    
    @Before
    public void setUp() {
        testMe = new GameOptions();
    }
    
    @Test
    public void testSaveAndLoadOptions() throws IOException {
        File f = tmpFolder.newFile("test-game-options.xml");
        
        Vector<IBasicOption> options = new Vector<>();
        Enumeration<IOption> opts = testMe.getOptions();
        int count = 0;
        while (opts.hasMoreElements()) {
            IOption io = opts.nextElement();
            
            switch (io.getType()) {
                case IOption.STRING:
                case IOption.CHOICE:
                    io.setValue(""+count);
                    break;

                case IOption.BOOLEAN:
                    if (count%2==0) {
                        io.setValue(Boolean.TRUE);
                    } else {
                        io.setValue(Boolean.FALSE);
                    }
                    break;

                case IOption.INTEGER:
                    io.setValue(count);
                    break;

                case IOption.FLOAT:
                    io.setValue(new Float(""+count));
                    break;
            }
            
            options.add(io);
            count++;
        }
        
        GameOptions.saveOptions(options, f.getAbsolutePath());
        
        assertTrue(f.exists());
        assertTrue(f.length() > 0);
        
        testMe.loadOptions(f, true);
        opts = testMe.getOptions();
        count = 0;
        while (opts.hasMoreElements()) {
            IOption io = opts.nextElement();
            
            switch (io.getType()) {
                case IOption.STRING:
                case IOption.CHOICE:
                case IOption.INTEGER:
                    assertTrue(io.getValue().toString().equals(""+count));
                    break;
                case IOption.BOOLEAN:
                    if (count%2==0) {
                        assertTrue(io.booleanValue());
                    } else {
                        assertFalse(io.booleanValue());
                    }
                    break;
                case IOption.FLOAT:
                    assertEquals(new Float(""+count), io.floatValue(), 0.0f);
                    break;
            }
            
            count++;
        }
    }
    
}

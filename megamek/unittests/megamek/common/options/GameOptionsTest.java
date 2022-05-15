/*
 * Copyright (c) 2000-2005 - Ben Mazur (bmazur@sev.org)
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
package megamek.common.options;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author nderwin
 */
public class GameOptionsTest {
    
    private GameOptions testMe;

    @TempDir
    private Path tempDirectory;

    @BeforeEach
    public void beforeEach() {
        testMe = new GameOptions();
    }
    
    @Test
    public void testSaveAndLoadOptions() throws IOException {
        assertTrue(Files.isDirectory(tempDirectory));
        final Path createdFilePath = Files.createFile(tempDirectory.resolve("test-game-options.xml"));
        final File file = createdFilePath.toFile();

        Vector<IBasicOption> options = new Vector<>();
        Enumeration<IOption> opts = testMe.getOptions();
        int count = 0;
        while (opts.hasMoreElements()) {
            IOption io = opts.nextElement();
            
            switch (io.getType()) {
                case IOption.STRING:
                case IOption.CHOICE:
                    io.setValue("" + count);
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
                    io.setValue(Float.valueOf("" + count));
                    break;
            }
            
            options.add(io);
            count++;
        }
        
        GameOptions.saveOptions(options, file.getAbsolutePath());
        
        assertTrue(file.exists());
        assertTrue(file.length() > 0);
        
        testMe.loadOptions(file, true);
        opts = testMe.getOptions();
        count = 0;
        while (opts.hasMoreElements()) {
            IOption io = opts.nextElement();
            
            switch (io.getType()) {
                case IOption.STRING:
                case IOption.CHOICE:
                case IOption.INTEGER:
                    assertEquals(io.getValue().toString(), "" + count);
                    break;
                case IOption.BOOLEAN:
                    if ((count % 2) == 0) {
                        assertTrue(io.booleanValue());
                    } else {
                        assertFalse(io.booleanValue());
                    }
                    break;
                case IOption.FLOAT:
                    assertEquals(Float.parseFloat("" + count), io.floatValue(), 0.0f);
                    break;
            }
            
            count++;
        }
    }
}

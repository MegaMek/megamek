/*
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

import com.sun.mail.util.DecodingException;
import megamek.common.*;
import megamek.common.InfantryBay.PlatoonType;
import megamek.common.loaders.BLKFile.ParsedBayInfo;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

public class BLKDropshipFileTest {

    @Test
    public void loadNewFormatDropshipAndConfirmFields(){
        /**
         *  This test verifies that a dropship file using the new bay numbers format
         *  produces the desired mix of tech, specifically with Clan tech and IS BA bays.
         */
        String mockBLKFile = String.join(
                System.getProperty("line.separator"),
                ""
        );

        // Create InputStream from string
        // Instantiate bb with string
        // Instantiate Dropship with bb
        // Get Entity
        // Confirm Dropship tech
        // Confirm BA Bay tech
    }

}

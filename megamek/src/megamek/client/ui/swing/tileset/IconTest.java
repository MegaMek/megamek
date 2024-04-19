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
package megamek.client.ui.swing.tileset;

import java.io.IOException;

/**
 * Executes both {@link MechSetTest} and {@link GenerateGenericIconList} for a combined icon
 * problems result.
 */
public final class IconTest {

    public static void main(final String[] args) throws IOException {
        try {
            System.out.println("Executing MechSetTest");
            MechSetTest.main(new String[0]);
        } catch (final Exception e) {
            System.out.println("There was a problem in MechSetTest!");
            e.printStackTrace();
        }
        try {
            System.out.println();
            System.out.println("Executing GenerateGenericIconList");
            GenerateGenericIconList.main(new String[0]);
        } catch (final Exception e) {
            System.out.println("There was a problem in GenerateGenericIconList!");
            e.printStackTrace();
        }
    }

    private IconTest() { }
}

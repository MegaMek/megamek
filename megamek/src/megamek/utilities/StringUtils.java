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

package megamek.utilities;

public class StringUtils {
    /**
     * Wrap an input string, inserting newlines such that line length will not exceed the given length. Used primarily
     * for formatting tooltips with wrapping, because apparently Swing doesn't have a nice provision to automatically
     * wordwrap tooltips.
     *
     * @param in     Input string
     * @param length The maximum line length in characters
     *
     * @return The string, with lines wrapped to at most length
     */
    public static String wrapLines(String in, int length) {
        StringBuilder sb = new StringBuilder();

        while (length < in.length()) {
            int nextLineBreak = in.indexOf('\n');
            while (nextLineBreak != -1 && nextLineBreak < length) {
                sb.append(in, 0, nextLineBreak + 1);
                in = in.substring(nextLineBreak + 1);
                nextLineBreak = in.indexOf('\n');
            }
            if (in.length() < length) {
                break;
            }

            String chunk = in.substring(0, length);
            int lastBreak = chunk.lastIndexOf(' ');
            if (lastBreak == -1) {
                lastBreak = length;
            }
            sb.append(in, 0, lastBreak);
            sb.append('\n');

            in = in.substring(lastBreak + 1);
        }
        sb.append(in);

        return sb.toString();
    }
}

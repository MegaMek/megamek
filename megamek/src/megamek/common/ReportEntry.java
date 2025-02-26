/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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
package megamek.common;

import java.io.Serializable;

public interface ReportEntry extends Serializable {

    /**
     * Get the report in its final HTML form, with all the necessary substitutions made.
     *
     * @return a HTML String with the final report
     */
    String text();

    /**
     * Adds the result of the given roll to this ReportEntry. Returns this  ReportEntry to allow chaining calls.
     *
     * @param roll The roll to add
     * @return this ReportEntry
     */
    ReportEntry addRoll(Roll roll);
}

/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
 * Catalyst Game Labs and the Catalyst Game Labs logo are tradema oductions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. <Package Name> was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common;

import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.units.Crew;
import megamek.common.units.Mek;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class ReportTest {

    @Test
    public void testReportHtmlStructure() {
        // Create a report with a known message ID (e.g., 1000)
        // 1000=<B>Initiative Phase for Round #<data></B>
        Report report = new Report(1000);
        report.add("1");

        String text = report.text();
        // Check for wrapping span (using span instead of div to keep reports inline)
        assertTrue(text.startsWith("<span class='report-entry'>"),
              "Report should be wrapped in a span with class report-entry");
        assertTrue(text.endsWith("</span>"), "Report should end with closing span");

        // Check for Bold tag
        assertTrue(text.contains("<B>"), "Report should use bold tag");
    }

    @Test
    public void testEntitySpan() {
        // Create a dummy entity
        Mek m = Mockito.mock(Mek.class);
        Mockito.when(m.getId()).thenReturn(123);
        Mockito.when(m.getShortName()).thenReturn("TestMek");

        Crew mockCrew = Mockito.mock(Crew.class);
        Mockito.when(m.getCrew()).thenReturn(mockCrew);
        Mockito.when(mockCrew.getSize()).thenReturn(1);
        Mockito.when(mockCrew.getNickname()).thenReturn("");

        Report report = new Report(1000); // Just using 1000 as a placeholder
        report.addDesc(m);

        // We can't easily check the full output because addDesc adds to the tagData,
        // and we need a message that uses <data>.
        // Let's use message 1015=<data> rolls a <data>.
        report = new Report(1015);
        report.addDesc(m);
        report.add("6");

        String text = report.text();

        // Check for entity name span
        assertTrue(text.contains("<span class='entity-name' data-entity-id='123'>"),
                "Entity name should be wrapped in span with class entity-name");
    }
}

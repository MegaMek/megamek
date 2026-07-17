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
package megamek.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.thoughtworks.xstream.XStream;
import megamek.common.units.HeatBreakdown;
import org.junit.jupiter.api.Test;

class SerializationHelperTest {

    /**
     * XStream 1.4 can serialize records but cannot deserialize them without an explicit converter.
     * {@link HeatBreakdown.HeatContribution} is a {@code Serializable} record stored in {@code Entity}, so a
     * missing converter makes save games containing heat-breakdown data fail to load. This pins the converter
     * registered in {@link SerializationHelper#getLoadSaveGameXStream()}.
     */
    @Test
    void heatContributionRecordSurvivesSaveGameRoundTrip() {
        HeatBreakdown.HeatContribution original = new HeatBreakdown.HeatContribution(3, 30);

        XStream saveXStream = SerializationHelper.getSaveGameXStream();
        String xml = saveXStream.toXML(original);

        XStream loadXStream = SerializationHelper.getLoadSaveGameXStream();
        Object restored = loadXStream.fromXML(xml);

        assertEquals(original, restored);
    }

    /**
     * A malformed numeric value must not deserialize to a {@code null} {@link HeatBreakdown.HeatContribution}: a
     * null stored in {@link HeatBreakdown}'s buildup map would later NPE (for example in
     * {@code buildupTooltip()}). The converter keeps the field default instead and returns a non-null record.
     */
    @Test
    void malformedHeatContributionValueDeserializesToNonNullDefault() {
        XStream saveXStream = SerializationHelper.getSaveGameXStream();
        String xml = saveXStream.toXML(new HeatBreakdown.HeatContribution(3, 99));
        String corrupted = xml.replace("99", "notANumber");

        XStream loadXStream = SerializationHelper.getLoadSaveGameXStream();
        Object restored = loadXStream.fromXML(corrupted);

        assertNotNull(restored);
        assertInstanceOf(HeatBreakdown.HeatContribution.class, restored);
        HeatBreakdown.HeatContribution contribution = (HeatBreakdown.HeatContribution) restored;
        assertEquals(3, contribution.count());
        assertEquals(0, contribution.totalHeat());
    }
}

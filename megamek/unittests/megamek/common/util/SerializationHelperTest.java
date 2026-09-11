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
import static org.junit.jupiter.api.Assertions.assertNull;

import com.thoughtworks.xstream.XStream;
import megamek.common.RulesRef;
import megamek.common.SourceBookCode;
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
     * Equipment rules references are records stored in save-game object graphs (for example on an ejected
     * MekWarrior's infantry weapon). Without the matching load-side converter, saves taken after an ejection fail
     * to load and no units are reattached to players.
     */
    @Test
    void rulesRefRecordSurvivesSaveGameRoundTrip() {
        RulesRef original = new RulesRef(SourceBookCode.TM, 273);

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

    /**
     * {@link RulesRef} is the sourcebook reference on every {@code EquipmentType}. An ejected crew serializes its
     * infantry weapon inline, so without a converter every save taken after a crew ejects fails to load and the
     * game comes back with its players present but no units (issue #8924). This pins the converter.
     */
    @Test
    void rulesRefRecordSurvivesSaveGameRoundTrip() {
        RulesRef original = new RulesRef(SourceBookCode.TM, 273);

        XStream saveXStream = SerializationHelper.getSaveGameXStream();
        String xml = saveXStream.toXML(original);

        XStream loadXStream = SerializationHelper.getLoadSaveGameXStream();
        Object restored = loadXStream.fromXML(xml);

        assertEquals(original, restored);
    }

    /**
     * A page-less reference is valid, so the missing {@code page} element must come back as {@code null} rather
     * than tripping the record's own validation.
     */
    @Test
    void pagelessRulesRefSurvivesSaveGameRoundTrip() {
        RulesRef original = new RulesRef(SourceBookCode.TO_AR, null);

        XStream saveXStream = SerializationHelper.getSaveGameXStream();
        String xml = saveXStream.toXML(original);

        XStream loadXStream = SerializationHelper.getLoadSaveGameXStream();
        Object restored = loadXStream.fromXML(xml);

        assertEquals(original, restored);
    }

    /**
     * A book code this version no longer knows must not produce a {@code null} list entry: {@code
     * TechAdvancement.guessStaticTechLevel} maps {@code RulesRef::book} over the list and would NPE.
     */
    @Test
    void unknownBookCodeDeserializesToNonNullReference() {
        XStream saveXStream = SerializationHelper.getSaveGameXStream();
        String xml = saveXStream.toXML(new RulesRef(SourceBookCode.TM, 273));
        String corrupted = xml.replace("<book>TM</book>", "<book>NoSuchBook</book>");

        XStream loadXStream = SerializationHelper.getLoadSaveGameXStream();
        Object restored = loadXStream.fromXML(corrupted);

        assertNotNull(restored);
        assertInstanceOf(RulesRef.class, restored);
        assertEquals(SourceBookCode.UNOFFICIAL, ((RulesRef) restored).book());
    }

    /**
     * The record's compact constructor rejects a page below 1, so a corrupted page must be dropped rather than
     * thrown from inside the load.
     */
    @Test
    void nonPositivePageDeserializesToNoPage() {
        XStream saveXStream = SerializationHelper.getSaveGameXStream();
        String xml = saveXStream.toXML(new RulesRef(SourceBookCode.TM, 273));
        String corrupted = xml.replace("<page>273</page>", "<page>0</page>");

        XStream loadXStream = SerializationHelper.getLoadSaveGameXStream();
        Object restored = loadXStream.fromXML(corrupted);

        assertNotNull(restored);
        assertInstanceOf(RulesRef.class, restored);
        assertNull(((RulesRef) restored).page());
    }
}

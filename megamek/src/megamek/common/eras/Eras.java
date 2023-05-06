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
package megamek.common.eras;

import megamek.MMConstants;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.utilities.xml.MMXMLUtility;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/**
 * This singleton class represents the Eras of the BT Universe like the Civil War or the Succession Wars.
 * The Eras are read from the eras.xml definition file and are thus moddable.
 *
 * @author Justin "Windchild" Bowen
 * @author Simon (Juliez)
 */
public class Eras {

    /** @return The sole instance of Eras. Calling this also initialises and loads the eras. */
    public static Eras getInstance() {
        if (instance == null) {
            instance = new Eras();
        }
        return instance;
    }

    /**
     * Returns the {@link Era} of the given date. For the canon eras the day makes no difference but the eras
     * definition xml allows eras to be defined with day resolution.
     *
     * @param date The date to get the Era for
     * @return The Era of the given date, e.g. IlClan on March 25, 3153
     */
    public static Era getEra(final LocalDate date) {
        return getInstance().eras.ceilingEntry(date).getValue();
    }

    /**
     * Returns the {@link Era} of the first day of the given year (January 1st). For the canon eras the day
     * makes no difference but the eras definition xml allows eras to be defined with day resolution. Therefore
     * it is preferable to use {@link #getEra(LocalDate)} when a date is available.
     *
     * @param year The year to get the Era for
     * @return The Era on January 1st of the given year
     */
    public static Era getEra(final int year) {
        return getEra(LocalDate.ofYearDay(year, 1));
    }

    /**
     * Returns true when the given date is in the given Era.
     *
     * @param date The date to test
     * @param era The Era to check against the date
     * @return True when the date is in the Era's date range
     */
    public static boolean isThisEra(LocalDate date, Era era) {
        Era eraAtDate = getEra(date);
        return (era != null) && era.equals(eraAtDate);
    }

    //region non-public

    private static final Era ERA_PLACEHOLDER = new Era("???", "Unknown", null, null, -1, null);
    private static Eras instance;

    private final TreeMap<LocalDate, Era> eras = new TreeMap<>();

    private Eras() {
        try {
            initializeEras();
        } catch (Exception ex) {
            addEra(ERA_PLACEHOLDER);
            LogManager.getLogger().error("", ex);
        }
    }

    private void addEra(Era era) {
        eras.put(era.end(), era);
    }

    private void initializeEras() throws Exception {
        final File file = new MegaMekFile(MMConstants.ERAS_FILE_PATH).getFile();
        if ((file == null) || !file.exists()) {
            throw new IOException("The eras definition file " + MMConstants.ERAS_FILE_PATH + " does not exist.");
        }

        final Document xmlDoc;

        try (InputStream is = new FileInputStream(file)) {
            xmlDoc = MMXMLUtility.newSafeDocumentBuilder().parse(is);
        }

        final Element element = xmlDoc.getDocumentElement();
        element.normalize();
        final NodeList nl = element.getChildNodes();
        for (int x = 0; x < nl.getLength(); x++) {
            final Node wn = nl.item(x);
            if (wn.getParentNode().equals(element) && (wn.getNodeType() == Node.ELEMENT_NODE)
                    && wn.getNodeName().equalsIgnoreCase("era") && wn.hasChildNodes()) {
                addEra(generateInstanceFromXML(wn.getChildNodes()));
            }
        }
    }

    private static Era generateInstanceFromXML(final NodeList nl) {
        try {
            String code = "";
            String name = "";
            String icon = "";
            LocalDate end = LocalDate.MAX;
            List<EraFlag> flags = new ArrayList<>();
            int mulId = -1;
            for (int x = 0; x < nl.getLength(); x++) {
                final Node wn = nl.item(x);
                switch (wn.getNodeName().toLowerCase()) {
                    case "code":
                        code = MMXMLUtility.unEscape(wn.getTextContent().trim());
                        break;
                    case "name":
                        name = MMXMLUtility.unEscape(wn.getTextContent().trim());
                        break;
                    case "end":
                        end = parseDate(wn.getTextContent().trim());
                        break;
                    case "flag":
                        flags.add(EraFlag.valueOf(wn.getTextContent().trim()));
                        break;
                    case "mulid":
                        mulId = Integer.parseInt(wn.getTextContent().trim());
                        break;
                    case "icon":
                        icon = MMXMLUtility.unEscape(wn.getTextContent().trim());
                }
            }
            return new Era(code, name, end, flags, mulId, icon);
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
            return ERA_PLACEHOLDER;
        }
    }

    /**
     * Parse an end date from an XML node's content, assuming the last day of the year or month, if
     * only a year or month is given.
     *
     * @param value The date from an XML node's content.
     * @return The Date retrieved from the XML node content.
     */
    private static LocalDate parseDate(final String value) throws DateTimeParseException {
        // Accepts: yyyy-mm-dd
        // Accepts (assumes last day of month): yyyy-mm
        // Accepts (assumes last day of year): yyyy
        switch (value.length()) {
            case 4:
                LocalDate year = LocalDate.parse(value + "-01-01");
                return year.with(TemporalAdjusters.lastDayOfYear());
            case 7:
                LocalDate yearMonth = LocalDate.parse(value + "-01");
                return yearMonth.with(TemporalAdjusters.lastDayOfMonth());
            default:
                throw new DateTimeParseException("Wrong date format", value, 0);
        }
    }

    //endregion non-public
}
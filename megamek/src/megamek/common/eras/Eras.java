/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common.eras;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import megamek.MMConstants;
import megamek.common.annotations.Nullable;
import megamek.common.interfaces.ITechnology;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.logging.MMLogger;
import megamek.utilities.xml.MMXMLUtility;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This singleton class is a handler for the Eras of the BT Universe like the Civil War or the Succession Wars. The Eras
 * are read from the eras.xml definition file and are thus moddable. The class therefore has a few methods that deal
 * with validation and is supposed to be resistant to wrong data.
 *
 * @author Justin "Windchild" Bowen
 * @author Simon (Juliez)
 */
public final class Eras {
    private static final MMLogger logger = MMLogger.create(Eras.class);

    /**
     * @return The sole instance of Eras. Calling this also initializes and loads the eras.
     */
    public static Eras getInstance() {
        if (instance == null) {
            instance = new Eras();
        }
        return instance;
    }

    /**
     * Returns the {@link Era} of the given date. For the canon eras, the day makes no difference, but the era
     * definition XML allows eras to be defined with day resolution.
     *
     * @param date The date to get the Era for
     *
     * @return The Era of the given date, e.g., IlClan on March 25, 3153
     */
    public static Era getEra(final LocalDate date) {
        return getInstance().eras.ceilingEntry(date).getValue();
    }

    /**
     * Returns the {@link Era} of the first day of the given year (January 1st). For the canon eras, the day makes no
     * difference, but the eras definition xml allows eras to be defined with day resolution. Therefore, it is
     * preferable to use {@link #getEra(LocalDate)} when a date is available.
     *
     * @param year The year to get the Era for
     *
     * @return The Era on January 1st of the given year
     */
    public static Era getEra(final int year) {
        return getEra(LocalDate.ofYearDay(year, 1));
    }

    /**
     * Returns true when the given date is in the given Era.
     *
     * @param date The date to test
     * @param era  The Era to check against the date
     *
     * @return True when the date is in the Era's date range
     */
    public static boolean isThisEra(LocalDate date, Era era) {
        Era eraAtDate = getEra(date);
        return (era != null) && era.equals(eraAtDate);
    }

    /**
     * Returns a list of all Eras, ordered by their end dates, i.e. in their natural order with the oldest being first.
     *
     * @return All Eras
     */
    public static List<Era> getEras() {
        return new ArrayList<>(getInstance().eras.values());
    }

    /** @return True if the given Era is the first (earliest) of all eras. */
    public static boolean isFirstEra(@Nullable Era era) {
        return (era != null) && era.equals(getInstance().eras.firstEntry().getValue());
    }

    /**
     * @return The era directly preceding the given Era, if any, null if the given Era is the first era (or null).
     */
    public static @Nullable Era previousEra(@Nullable Era era) {
        if (era == null) {
            return null;
        } else {
            return getInstance().eras.lowerEntry(era.end()).getValue();
        }
    }

    /**
     * @return The era directly following the given Era, if any, null if the given Era is the last era (or null).
     */
    public static @Nullable Era nextEra(@Nullable Era era) {
        if (era == null) {
            return null;
        } else {
            return getInstance().eras.higherEntry(era.end()).getValue();
        }
    }

    /**
     * @return The starting date of the given Era or LocalDate.MIN if the given Era is the first era.
     */
    public static LocalDate startDate(Era era) {
        if (isFirstEra(era)) {
            return LocalDate.MIN;
        } else {
            return previousEra(era).end().with(TemporalAdjusters.ofDateAdjuster(date -> date.plusDays(1)));
        }
    }

    /**
     * Generates era text for a date range, similar to the original eraText method
     *
     * @param startYear Start year of the range
     * @param endYear   End year of the range (null for open-ended)
     *
     * @return Era text string
     */
    public static String getEraText(int startYear, Integer endYear) {
        String eraText = "";
        if (startYear != ITechnology.DATE_NONE) {
            Era startEra = Eras.getEra(startYear);
            if (endYear == null) {
                // Open-ended range
                eraText = " (" + startEra.name() + " and onwards)";
            } else {
                Era endEra = Eras.getEra(endYear);
                eraText = " (" + startEra.name();
                if (!endEra.equals(startEra)) {
                    eraText += " to " + endEra.name();
                }
                eraText += ")";
            }
        }
        return eraText;
    }

    /**
     * Generates era text for a date range, similar to the original eraText method
     *
     * @param year the year in question
     *
     * @return Era text string
     */
    public static String getEraText(int year) {
        String eraText = "";
        if (year != ITechnology.DATE_NONE) {
            eraText = " (" + Eras.getEra(year).name() + ")";
        }
        return eraText;
    }
    // region non-public

    private static final Era ERA_PLACEHOLDER = new Era("???", "Unknown", null, null, -1, null);
    private static Eras instance;

    /** This Map contains the sorted eras and is used for public methods. */
    private final TreeMap<LocalDate, Era> eras = new TreeMap<>();

    /**
     * This list contains all eras, even if they're malformed and is used for troubleshooting.
     */
    private final List<Era> eraList = new ArrayList<>();

    private Eras() {
        try {
            loadErasFromXML();
        } catch (Exception ex) {
            addEra(ERA_PLACEHOLDER);
            logger.error("", ex);
        }
    }

    private void addEra(Era era) {
        eras.put(era.end(), era);
        eraList.add(era);
    }

    private void loadErasFromXML() throws Exception {
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
        eraList.sort(Comparator.comparing(Era::end));
        if (!areAllValid(null)) {
            logger
                  .error("The eras definition file {}contains malformed eras!", MMConstants.ERAS_FILE_PATH);
        }
    }

    private Era generateInstanceFromXML(final NodeList nl) {
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
                        end = MMXMLUtility.parseDate(wn.getTextContent().trim());
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
            logger.error("", ex);
            return ERA_PLACEHOLDER;
        }
    }

    /**
     * @return True when the code, name and mulId of the era are valid. When true, the era may still be invalid.
     */
    private boolean isValid(Era era) {
        return !era.code().isBlank() && !era.name().isBlank()
              && ((era.mulId() == -1) || (era.mulId() > 0));
    }

    /**
     * Returns true when the set of eras is valid (meaning the xml file has no malformed entries.) A Set can be passed
     * in that will receive all invalid eras if there are any. Note that the Set is not emptied before being used.
     *
     * @param invalidErasReturn An optional Set to receive invalid eras.
     *
     * @return True when all eras are valid, false otherwise
     */
    boolean areAllValid(@Nullable Set<Era> invalidErasReturn) {
        if (invalidErasReturn == null) {
            invalidErasReturn = new HashSet<>();
        }
        for (Era era : eraList) {
            if (!isValid(era)) {
                invalidErasReturn.add(era);
                continue;
            }
            // No two eras may share a code
            if (eraList.stream().filter(era2 -> era2.code().equals(era.code())).count() != 1) {
                invalidErasReturn.add(era);
            }
        }

        // Exactly one era must be the last era
        if (eraList.stream().filter(Era::isLastEra).count() != 1) {
            invalidErasReturn.addAll(eraList.stream().filter(Era::isLastEra).toList());
        }

        return invalidErasReturn.isEmpty();
    }

    /**
     * Returns all Eras, possibly including invalid ones. This is only for Era editing when even invalid eras should be
     * available. Use {@link #getEras()} in all other circumstances. This method is intentionally package private.
     *
     * @return All eras, possibly also invalid ones.
     */
    static List<Era> getAllEras() {
        return getInstance().eraList;
    }

    // endregion non-public
}

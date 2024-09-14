/*
 * MegaMek -
 * Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 *
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
package megamek.common.verifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import megamek.common.Configuration;
import megamek.common.Entity;
import megamek.common.MekFileParser;
import megamek.common.MekSummary;
import megamek.common.MekSummaryCache;
import megamek.common.UnitType;
import megamek.logging.MMLogger;
import megamek.utilities.xml.MMXMLUtility;

/**
 * Performs verification of the validity of different types of
 * <code>Entity</code> subclasses. Most of the actual validation is performed
 * by <code>TestEntity</code> and its subclasses.
 *
 * @author Reinhard Vicinus
 */
@XmlRootElement(name = "entityverifier")
@XmlAccessorType(value = XmlAccessType.NONE)
public class EntityVerifier implements MekSummaryCache.Listener {

    public static final String CONFIG_FILENAME = "UnitVerifierOptions.xml";

    private static EntityVerifier instance = null;

    private static MekSummaryCache mekSummaryCache = null;

    private static final MMLogger logger = MMLogger.create(EntityVerifier.class);

    @XmlElement(name = "mek")
    public TestXMLOption mekOption = new TestXMLOption();

    @XmlElement(name = "protomek")
    public TestXMLOption protomekOption = new TestXMLOption();

    @XmlElement(name = "tank")
    public TestXMLOption tankOption = new TestXMLOption();

    @XmlElement(name = "aero")
    public TestXMLOption aeroOption = new TestXMLOption();

    @XmlElement(name = "ba")
    public TestXMLOption baOption = new TestXMLOption();

    @XmlElement(name = "infantry")
    public TestXMLOption infOption = new TestXMLOption();

    private boolean loadingVerbosity = false;
    private boolean failsOnly = false;

    /**
     * JAXB Constructor.
     */
    private EntityVerifier() {
    }

    /**
     * Creates and return a new instance of EntityVerifier.
     *
     * @param config a File that contains an XML representation of the configuration
     *               settings
     * @return an EntityVerifier with the configuration loaded from XML
     */
    public static EntityVerifier getInstance(final File config) {
        if (instance != null) {
            return instance;
        }

        instance = new EntityVerifier();

        try {
            JAXBContext jc = JAXBContext.newInstance(EntityVerifier.class);

            Unmarshaller um = jc.createUnmarshaller();
            InputStream is = new FileInputStream(config);
            instance = (EntityVerifier) um.unmarshal(MMXMLUtility.createSafeXmlSource(is));
        } catch (Exception e) {
            String message = String.format("Error loading XML for entity verifier: %s", e.getMessage());
            logger.error(e, message);
        }

        return instance;
    }

    public boolean checkEntity(Entity entity, String fileString, boolean verbose) {
        return checkEntity(entity, fileString, verbose, entity.getTechLevel());
    }

    public boolean checkEntity(Entity entity, String fileString, boolean verbose, int ammoTechLvl) {
        return checkEntity(entity, fileString, verbose, ammoTechLvl, false);
    }

    public boolean checkEntity(Entity entity, String fileString, boolean verbose, int ammoTechLvl, boolean failsOnly) {
        final NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.getDefault());
        boolean retVal = false;
        TestEntity testEntity = TestEntity.getEntityVerifier(entity);
        String message = "";

        if (testEntity == null) {
            message = String.format("Unknown Type: %s%nFound in: %s", entity.getDisplayName(), fileString);
            logger.error(message);
            return false;
        }

        if (verbose) {
            StringBuffer buff = new StringBuffer();
            boolean valid = testEntity.correctEntity(buff, ammoTechLvl);

            if (!valid || !failsOnly) {
                if (valid) {
                    logger.info("---Entity is valid---");
                } else {
                    logger.info("---Entity INVALID---");
                }
                message = String.format("%s%nBV: %s    Cost: %s", testEntity.printEntity(),
                        entity.calculateBattleValue(), numberFormat.format(entity.getCost(false)));
                logger.info(message);
            }
        } else {
            StringBuffer buff = new StringBuffer();
            if (testEntity.correctEntity(buff, ammoTechLvl)) {
                retVal = true;
            } else {
                message = String.format("""
                        %s
                        Found in: %s
                        Intro year: %d
                        BV: %d    Cost: %s
                        %s
                        """,
                        testEntity.getName(),
                        testEntity.fileString,
                        entity.getYear(),
                        entity.calculateBattleValue(),
                        numberFormat.format(entity.getCost(false)),
                        buff.toString());
                logger.info(message);
            }
        }

        return retVal;
    }

    public Entity loadEntity(File f, String entityName) {
        Entity entity = null;

        try {
            entity = new MekFileParser(f, entityName).getEntity();
        } catch (Exception ex) {
            logger.error(ex, "Unable to load entity.");
        }

        return entity;
    }

    // This is the listener method that MekSummaryCache calls when it finishes
    // loading all the meks. This should only happen if no specific files were
    // passed to main() as arguments (which implies all units that are loaded when
    // MegaMek normally runs should be checked).
    @Override
    public void doneLoading() {
        String message = "";
        MekSummary[] ms = mekSummaryCache.getAllMeks();

        message = String.format("""

                Mek Options: %s
                Protomek Options: %s
                Tank Options: %s
                Aero Options: %s
                BattleArmor Options: %s
                Infantry Options: %s
                        """,
                mekOption.printOptions(),
                protomekOption.printOptions(),
                tankOption.printOptions(),
                aeroOption.printOptions(),
                baOption.printOptions(),
                infOption.printOptions());
        logger.info(message);

        int failures = 0;
        Map<Integer, Integer> failedByType = new HashMap<>();

        for (int i = 0; i < ms.length; i++) {
            int unitType = UnitType.determineUnitTypeCode(ms[i].getUnitType());
            if (unitType != UnitType.GUN_EMPLACEMENT) {
                Entity entity = loadEntity(ms[i].getSourceFile(), ms[i].getEntryName());
                if (entity == null) {
                    continue;
                }

                if (!checkEntity(entity, ms[i].getSourceFile().toString(), loadingVerbosity, entity.getTechLevel(),
                        failsOnly)) {
                    failures++;
                    failedByType.merge(unitType, 1, Integer::sum);
                }
            }
        }

        message = String.format("""
                Total Failures: %d
                    Failed Meks: %d
                    Failed ProtoMeks: %d
                    Failed Tanks: %d
                    Failed VTOLs: %d
                    Failed Naval: %d
                    Failed ASFs: %d
                    Failed AeroSpaces: %d
                    Failed CFs: %d
                    Failed Small Craft: %d
                    Failed DropShips: %d
                    Failed JumpShips: %d
                    Failed WarShips: %d
                    Failed Space Stations: %d
                    Failed BA: %d
                    Failed Infantry: %d
                """,
                failures,
                failedByType.getOrDefault(UnitType.MEK, 0),
                failedByType.getOrDefault(UnitType.PROTOMEK, 0),
                failedByType.getOrDefault(UnitType.TANK, 0),
                failedByType.getOrDefault(UnitType.VTOL, 0),
                failedByType.getOrDefault(UnitType.NAVAL, 0),
                failedByType.getOrDefault(UnitType.AEROSPACEFIGHTER, 0),
                failedByType.getOrDefault(UnitType.AERO, 0),
                failedByType.getOrDefault(UnitType.CONV_FIGHTER, 0),
                failedByType.getOrDefault(UnitType.SMALL_CRAFT, 0),
                failedByType.getOrDefault(UnitType.DROPSHIP, 0),
                failedByType.getOrDefault(UnitType.JUMPSHIP, 0),
                failedByType.getOrDefault(UnitType.WARSHIP, 0),
                failedByType.getOrDefault(UnitType.SPACE_STATION, 0),
                failedByType.getOrDefault(UnitType.BATTLE_ARMOR, 0),
                failedByType.getOrDefault(UnitType.INFANTRY, 0));
        logger.info(message);
    }

    public static void main(String[] args) {
        File config = new File(Configuration.unitsDir(), CONFIG_FILENAME);
        File f = null;
        String entityName = null;
        boolean verbose = false;
        boolean ignoreUnofficial = true;
        boolean failsOnly = true;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-file")) {
                i++;
                if (i >= args.length) {
                    logger.error("Missing argument filename!");
                    return;
                }

                f = new File(args[i]);

                if (!f.exists()) {
                    String message = String.format("Can't find %s!", args[i]);
                    logger.info(message);
                    return;
                }

                if (args[i].endsWith(".zip")) {
                    i++;
                    if (i >= args.length) {
                        logger.info("Missing Entity Name!");
                        return;
                    }
                    entityName = args[i];
                }
            } else if (args[i].equals("-v") || args[i].equals("-verbose")) {
                verbose = true;
            } else if (args[i].equals("-valid")) {
                failsOnly = false;
            } else if (args[i].equals("-unofficial")) {
                ignoreUnofficial = false;
            } else {
                logger.error("""
                        Error: Invalid argument.
                        Usage:

                        EntityVerifier [flags]

                        Valid Flags:
                            -file <FILENAME> Specify a file to validate,
                                            else the data directory is checked
                            -v              Verbose -- print detailed report
                            -unofficial      Consider unofficial units in data dir
                            -valid          Print verbose reports for valid units
                        """);
                return;
            }
        }

        if (f != null) {
            Entity entity;

            try {
                entity = new MekFileParser(f, entityName).getEntity();
            } catch (Exception ex) {
                logger.error("", ex);
                return;
            }

            EntityVerifier.getInstance(config).checkEntity(entity, f.toString(), true);
        } else {
            // No specific file passed, so have MegaMek load all the meks it normally
            // would, then verify all of them.
            EntityVerifier ev = EntityVerifier.getInstance(config);
            ev.loadingVerbosity = verbose;
            ev.failsOnly = failsOnly;
            mekSummaryCache = MekSummaryCache.getInstance(ignoreUnofficial);
            mekSummaryCache.addListener(ev);
        }
    }
}

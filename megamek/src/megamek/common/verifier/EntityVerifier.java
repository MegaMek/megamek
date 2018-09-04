/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.common.verifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import megamek.common.Aero;
import megamek.common.BattleArmor;
import megamek.common.Configuration;
import megamek.common.Entity;
import megamek.common.GunEmplacement;
import megamek.common.Infantry;
import megamek.common.Jumpship;
import megamek.common.Mech;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.SmallCraft;
import megamek.common.Tank;
import megamek.common.UnitType;
import megamek.utils.MegaMekXmlUtil;

/**
 * Performs verification of the validity of different types of 
 * <code>Entity</code> subclasses.  Most of the actual validation is performed
 * by <code>TestEntity</code> and its subclasses. 
 * 
 * @author Reinhard Vicinus
 */
@XmlRootElement(name = "entityverifier")
@XmlAccessorType(XmlAccessType.NONE)
public class EntityVerifier implements MechSummaryCache.Listener {
    public static final String CONFIG_FILENAME = "UnitVerifierOptions.xml"; //$NON-NLS-1$

    private static MechSummaryCache mechSummaryCache = null;

    @XmlElement(name = "mech")
    public TestXMLOption mechOption = new TestXMLOption();
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
     * @param config a File that contains an XML representation of the configuration settings
     * @return an EntityVerifier with the configuration loaded from XML
     */
    public static EntityVerifier getInstance(final File config) {
        EntityVerifier ev = null;
        
        try {
            JAXBContext jc = JAXBContext.newInstance(EntityVerifier.class);
            
            Unmarshaller um = jc.createUnmarshaller();
            InputStream is = new FileInputStream(config);
            ev = (EntityVerifier) um.unmarshal(MegaMekXmlUtil.createSafeXmlSource(is));
        } catch (IOException | JAXBException | SAXException | ParserConfigurationException ex) {
            System.err.println("Error loading XML for entity verifier: " + ex.getMessage()); //$NON-NLS-1$
            ex.printStackTrace();
            
            ev = new EntityVerifier();
        }
        
        return ev;
    }

    public boolean checkEntity(Entity entity, String fileString, boolean verbose) {
        return checkEntity(entity, fileString, verbose, entity.getTechLevel());
    }

    public boolean checkEntity(Entity entity, String fileString,
            boolean verbose, int ammoTechLvl) {
        return checkEntity(entity, fileString, verbose, ammoTechLvl, false);
    }
    
    public boolean checkEntity(Entity entity, String fileString,
            boolean verbose, int ammoTechLvl, boolean failsOnly) {
        final NumberFormat FMT = NumberFormat.getNumberInstance(Locale.getDefault());
        boolean retVal = false;
        TestEntity testEntity = null;
        if (entity instanceof Mech) {
            testEntity = new TestMech((Mech) entity, mechOption, fileString);
        } else if ((entity instanceof Tank) && 
                !(entity instanceof GunEmplacement)) {
            if (entity.isSupportVehicle()) {
                testEntity = new TestSupportVehicle((Tank) entity, tankOption,
                        null);
            } else {
                testEntity = new TestTank((Tank) entity, tankOption, null);
            }
        } else if (entity.hasETypeFlag(Entity.ETYPE_SMALL_CRAFT)) {
            testEntity = new TestSmallCraft((SmallCraft) entity, aeroOption, fileString);
        } else if (entity.hasETypeFlag(Entity.ETYPE_JUMPSHIP)) {
            testEntity = new TestAdvancedAerospace((Jumpship) entity, aeroOption, fileString);
        } else if (entity.hasETypeFlag(Entity.ETYPE_AERO)
                && !entity.hasETypeFlag(Entity.ETYPE_FIGHTER_SQUADRON)) {
            testEntity = new TestAero((Aero)entity, aeroOption, fileString);
        } else if (entity instanceof BattleArmor){
            testEntity = new TestBattleArmor((BattleArmor) entity, baOption,
                    fileString);
        } else if (entity instanceof Infantry){
            testEntity = new TestInfantry((Infantry) entity, infOption,
                    fileString);
        } else {
            System.err.println("UnknownType: " + entity.getDisplayName());
            System.err.println("Found in: " + fileString);
            return false;
        }

        if (verbose) {
            StringBuffer buff = new StringBuffer();
            boolean valid = testEntity.correctEntity(buff, ammoTechLvl);
            if (!valid || !failsOnly){
                if (valid) {
                    System.out.println("---Entity is valid---");
                } else {
                    System.out.println("---Entity INVALID---");
                }
                System.out.print(testEntity.printEntity());                        
                System.out.println("BV: " + entity.calculateBattleValue()
                        + "    Cost: " + FMT.format(entity.getCost(false)));
            }
        } else {
            StringBuffer buff = new StringBuffer();
            if (testEntity.correctEntity(buff, ammoTechLvl)) {
                retVal = true;
            } else {
                System.out.println(testEntity.getName());
                System.out.println("Found in: " + testEntity.fileString);
                System.out.println("Intro year: " + entity.getYear());
                System.out.println("BV: " + entity.calculateBattleValue()
                + "    Cost: " + FMT.format(entity.getCost(false)));
                System.out.println(buff);

            }
        }

        return retVal;
    }

    public Entity loadEntity(File f, String entityName) {
        Entity entity = null;
        try {
            entity = new MechFileParser(f, entityName).getEntity();
        } catch (megamek.common.loaders.EntityLoadingException e) {
            System.out.println("Exception: " + e.toString());
        }
        return entity;
    }

    // This is the listener method that MechSummaryCache calls when it
    // finishes loading all the mechs. This should only happen if no
    // specific files were passed to main() as arguments (which implies
    // all units that are loaded when MegaMek normally runs should be
    // checked).
    @Override
    public void doneLoading() {
        MechSummary[] ms = mechSummaryCache.getAllMechs();
        System.out.println("\n");

        System.out.println("Mech Options:");
        System.out.println(mechOption.printOptions());
        System.out.println("\nTank Options:");
        System.out.println(tankOption.printOptions());
        System.out.println("\nAero Options:");
        System.out.println(aeroOption.printOptions());
        System.out.println("\nBattleArmor Options:");
        System.out.println(baOption.printOptions());
        System.out.println("\nInfantry Options:");
        System.out.println(infOption.printOptions());

        int failures = 0;
        Map<Integer,Integer> failedByType = new HashMap<>();
        for (int i = 0; i < ms.length; i++) {
            int unitType = UnitType.determineUnitTypeCode(ms[i].getUnitType());
            if ((unitType != UnitType.PROTOMEK)
                    && (unitType != UnitType.GUN_EMPLACEMENT)) {
                Entity entity = loadEntity(ms[i].getSourceFile(), ms[i]
                        .getEntryName());
                if (entity == null) {
                    continue;
                }
                if (entity.hasETypeFlag(Entity.ETYPE_FIXED_WING_SUPPORT)) {
                    continue;
                }
                if (!checkEntity(entity, ms[i].getSourceFile().toString(),
                        loadingVerbosity,entity.getTechLevel(),failsOnly)) {
                    failures++;
                    failedByType.merge(unitType, 1, Integer::sum);
                }
            }
        }
        System.out.println("Total Failures: " + failures);
        System.out.println("\t Failed Meks: " + failedByType.getOrDefault(UnitType.MEK, 0));
        System.out.println("\t Failed Tanks: " + failedByType.getOrDefault(UnitType.TANK, 0));
        System.out.println("\t Failed VTOLs: " + failedByType.getOrDefault(UnitType.VTOL, 0));
        System.out.println("\t Failed Naval: " + failedByType.getOrDefault(UnitType.NAVAL, 0));
        System.out.println("\t Failed ASFs: " + failedByType.getOrDefault(UnitType.AERO, 0));
        System.out.println("\t Failed CFs: " + failedByType.getOrDefault(UnitType.CONV_FIGHTER, 0));
        System.out.println("\t Failed Small Craft: " + failedByType.getOrDefault(UnitType.SMALL_CRAFT, 0));
        System.out.println("\t Failed Dropships: " + failedByType.getOrDefault(UnitType.DROPSHIP, 0));
        System.out.println("\t Failed Jumpships: " + failedByType.getOrDefault(UnitType.JUMPSHIP, 0));
        System.out.println("\t Failed Warships: " + failedByType.getOrDefault(UnitType.WARSHIP, 0));
        System.out.println("\t Failed Space Stations: " + failedByType.getOrDefault(UnitType.SPACE_STATION, 0));
        System.out.println("\t Failed BA: " + failedByType.getOrDefault(UnitType.BATTLE_ARMOR, 0));
        System.out.println("\t Failed Infantry: " + failedByType.getOrDefault(UnitType.INFANTRY, 0));
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
                if (args.length <= i) {
                    System.out.println("Missing argument filename!");
                    return;
                }
                i++;
                f = new File(args[i]);
                if (!f.exists()) {
                    System.out.println("Can't find: " + args[i] + "!");
                    return;
                }
                if (args[i].endsWith(".zip")) {
                    if (args.length <= i + 1) {
                        System.out.println("Missing Entity Name!");
                        return;
                    }
                    i++;
                    entityName = args[i];
                }
            } else if (args[i].equals("-v") || args[i].equals("-verbose")){
                verbose = true;
            } else if (args[i].equals("-valid")){
                failsOnly = false;
            } else if (args[i].equals("-unofficial")){
                ignoreUnofficial = false;
            } else {
                System.err.println("Error: Invalid argument.\n");
                System.err.println("Usage:\n\tEntityVerifier [flags] \n\n" +
                "Valid Flags: \n" +
                "-file <FILENAME> \t Specify a file to validate,\n"+
                "                 \t   else the data directory is checked\n" +
                "-v               \t Verbose -- print detailed report\n" +
                "-unofficial      \t Consider unofficial units in data dir\n"+ 
                "-valid           \t Print verbose reports for valid units\n");
                return;
            }
        }

        if (f != null) {
            Entity entity = null;
            try {
                entity = new MechFileParser(f, entityName).getEntity();
            } catch (megamek.common.loaders.EntityLoadingException e) {
                System.err.println("Exception: " + e.toString());
                System.err.println("Exception: " + e.getMessage());
                return;
            }
            EntityVerifier.getInstance(config).checkEntity(entity, f.toString(), true);
        } else {
            // No specific file passed, so have MegaMek load all the mechs it
            // normally would, then verify all of them.
            EntityVerifier ev = EntityVerifier.getInstance(config);
            ev.loadingVerbosity = verbose;
            ev.failsOnly = failsOnly;
            mechSummaryCache = MechSummaryCache.getInstance(ignoreUnofficial);
            mechSummaryCache.addListener(ev);
        }
    }
}

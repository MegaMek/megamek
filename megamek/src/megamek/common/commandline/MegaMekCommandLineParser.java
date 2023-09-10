/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved
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
package megamek.common.commandline;

import megamek.MMConstants;
import megamek.MegaMek;
import megamek.client.ui.Messages;
import megamek.common.*;
import megamek.common.alphaStrike.conversion.ASConverter;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.common.verifier.*;
import org.apache.logging.log4j.LogManager;

import java.io.*;
import java.util.Vector;

/**
 * This class parses the options passed into to MegaMek from the command line.
 */
public class MegaMekCommandLineParser extends AbstractCommandLineParser {
    private boolean dedicatedServer = false;
    private boolean host = false;
    private boolean client = false;
    private boolean quick = false;
    private boolean ratGenEditor = false;
    private String[] restArgs = new String[0];

    public MegaMekCommandLineParser(String... args) {
        super(args);
    }

    public boolean dedicatedServer() {
        return dedicatedServer;
    }

    public boolean host() {
        return host;
    }

    public boolean client() {
        return client;
    }

    public boolean quick() {
        return quick;
    }

    /**
     * Flag that indicates the option for the RAT Generator editor
     * @return Whether the RAT Generator editor should be invoked
     */
    public boolean ratGenEditor() {
        return ratGenEditor;
    }

    /**
     * @return the <code>array</code> of the unprocessed arguments
     */
    public String[] getRestArgs() {
        return restArgs;
    }


    @Override
    public String help() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s %s\n", Messages.getString("MegaMek.Version"), MMConstants.VERSION));
        for (MegaMekCommandLineFlag flag : MegaMekCommandLineFlag.values()) {
            sb.append(String.format("-%s %s\n", flag.toString().toLowerCase(), flag.helpText));
        }
        return sb.toString();
    }

    @Override
    protected void start() throws ParseException {
        if (getTokenType() == TOK_OPTION) {
            final String tokenVal = getTokenValue();
            nextToken();
            try {
                switch (MegaMekCommandLineFlag.parseFromString(tokenVal)) {
                    case HELP:
                        System.out.println(help());
                        System.exit(0);
                    case EQDB:
                        processEquipmentDb();
                        break;
                    case EQEDB:
                        processExtendedEquipmentDb();
                        break;
                    case DATADIR:
                        processDataDir();
                        break;
                    case VALIDATE:
                        processUnitValidator();
                        break;
                    case EXPORT:
                        processUnitExporter();
                        break;
                    case OUL:
                        processUnitExporter(true);
                        break;
                    case ASC:
                        processUnitAlphaStrikeConverter();
                        break;
                    case DEDICATED:
                        dedicatedServer = true;
                        break;
                    case HOST:
                        host = true;
                        break;
                    case CLIENT:
                        client = true;
                        break;
                    case QUICK:
                        quick = true;
                        break;
                    case EDITRATGEN:
                        ratGenEditor = true;
                        break;
                }
            } catch (ParseException ex) {
                LogManager.getLogger().error("Incorrect arguments:" + ex.getMessage() + '\n' + help());
                throw ex;
            }
        }
        processRestOfInput();
        if (getTokenType() != TOK_EOF) {
            throw new ParseException("unexpected input");
        }
    }

    private void processEquipmentDb() throws ParseException {
        String filename;
        if (getTokenType() == TOK_LITERAL) {
            filename = getTokenValue();
            nextToken();
            megamek.common.EquipmentType.writeEquipmentDatabase(new File(filename));
        } else {
            throw new ParseException("file name expected");
        }
        System.exit(0);
    }

    private void processExtendedEquipmentDb() throws ParseException {
        String filename;
        if (getTokenType() == TOK_LITERAL) {
            filename = getTokenValue();
            nextToken();
            megamek.common.EquipmentType.writeEquipmentExtendedDatabase(new File(filename));
        } else {
            throw new ParseException("file name expected");
        }
        System.exit(0);
    }

    private void processDataDir() throws ParseException {
        String dataDirName;
        if (getTokenType() == TOK_LITERAL) {
            dataDirName = getTokenValue();
            nextToken();
            Configuration.setDataDir(new File(dataDirName));
        } else {
            throw new ParseException("directory name expected");
        }
    }

    private void processUnitValidator() throws ParseException {
        String filename;
        if (getTokenType() == TOK_LITERAL) {
            filename = getTokenValue();
            nextToken();
            MechSummary ms = MechSummaryCache.getInstance().getMech(filename);
            if (ms == null) {
                MechSummary[] units = MechSummaryCache.getInstance().getAllMechs();
                for (MechSummary unit : units) {
                    if (unit.getSourceFile().getName().equalsIgnoreCase(filename)) {
                        ms = unit;
                        break;
                    }
                }
            }

            if (ms == null) {
                LogManager.getLogger().error(filename + " not found. Try using \"chassis model\" for input.",
                        new IOException());
            } else {
                try {
                    Entity entity = new MechFileParser(ms.getSourceFile(),
                            ms.getEntryName()).getEntity();
                    LogManager.getLogger().info("Validating Entity: " + entity.getShortNameRaw());
                    EntityVerifier entityVerifier = EntityVerifier.getInstance(
                            new MegaMekFile(Configuration.unitsDir(),
                                    EntityVerifier.CONFIG_FILENAME).getFile());
                    MechView mechView = new MechView(entity, false);
                    StringBuffer sb = new StringBuffer(mechView.getMechReadout());
                    if ((entity instanceof Mech) || (entity instanceof Tank)
                            || (entity instanceof Aero) || (entity instanceof BattleArmor)) {
                        TestEntity testEntity = null;
                        if (entity instanceof Mech) {
                            testEntity = new TestMech((Mech) entity, entityVerifier.mechOption,
                                    null);
                        } else if ((entity instanceof Tank) && !(entity instanceof GunEmplacement)) {
                            if (entity.isSupportVehicle()) {
                                testEntity = new TestSupportVehicle(entity,
                                        entityVerifier.tankOption, null);
                            } else {
                                testEntity = new TestTank((Tank) entity,
                                        entityVerifier.tankOption, null);
                            }
                        } else if ((entity.getEntityType() == Entity.ETYPE_AERO)
                                && (entity.getEntityType() != Entity.ETYPE_DROPSHIP)
                                && (entity.getEntityType() != Entity.ETYPE_SMALL_CRAFT)
                                && (entity.getEntityType() != Entity.ETYPE_FIGHTER_SQUADRON)
                                && (entity.getEntityType() != Entity.ETYPE_JUMPSHIP)
                                && (entity.getEntityType() != Entity.ETYPE_SPACE_STATION)) {
                            testEntity = new TestAero((Aero) entity,
                                    entityVerifier.aeroOption, null);
                        } else if (entity instanceof BattleArmor) {
                            testEntity = new TestBattleArmor((BattleArmor) entity,
                                    entityVerifier.baOption, null);
                        }

                        if (testEntity != null) {
                            testEntity.correctEntity(sb);
                        }
                    }
                    LogManager.getLogger().info(sb.toString());
                } catch (Exception ex) {
                    throw new ParseException("\"chassis model\" expected as input");
                }
            }
        } else {
            throw new ParseException("\"chassis model\" expected as input");
        }
        System.exit(0);
    }

    private void processUnitAlphaStrikeConverter() {
        String filename;
        if (getTokenType() == TOK_LITERAL) {
            filename = getTokenValue();
            nextToken();

            if (!new File("./docs").exists()) {
                if (!new File("./docs").mkdir()) {
                    LogManager.getLogger().error(
                            "Error in creating directory ./docs. We know this is annoying, and apologise. "
                                    + "Please submit a bug report at https://github.com/MegaMek/megamek/issues "
                                    + " and we will try to resolve your issue.");
                }
            }

            File file = new File("./docs/" + filename);
            try (Writer w = new FileWriter(file); BufferedWriter bw = new BufferedWriter(w)) {
                bw.write("Megamek Unit AlphaStrike Converter");
                bw.newLine();
                bw.write("This file can be regenerated with java -jar MegaMek.jar -asc filename");
                bw.newLine();
                bw.write("MUL ID\tChassis\tModel\tRole\tType\tSize\tMovement\tTMM\tArmor\tStructure\tThreshold\t");
                bw.write("S\tS*\tM\tM*\tL\tL*\tE\tE*\tOverheat\tPoint Value\tAbilities\t");
                bw.write("Front Arc\tLeft Arc\tRight Arc\tRear Arc");
                bw.newLine();

                MechSummary[] units = MechSummaryCache.getInstance().getAllMechs();
                for (MechSummary unit : units) {
                    Entity entity = new MechFileParser(unit.getSourceFile(), unit.getEntryName()).getEntity();
                    if (ASConverter.canConvert(entity)) {
                        AlphaStrikeElement element = ASConverter.convert(entity);
                        bw.write(element.getMulId() + "");
                        bw.write("\t");
                        bw.write(element.getChassis());
                        bw.write("\t");
                        bw.write(element.getModel());
                        bw.write("\t");
                        bw.write(element.getRole() + "");
                        bw.write("\t");
                        bw.write(element.getASUnitType() + "");
                        bw.write("\t");
                        bw.write(element.getSize() + "");
                        bw.write("\t");
                        bw.write(element.getMovementAsString());
                        bw.write("\t");
                        bw.write(element.getTMM() + "");
                        bw.write("\t");
                        bw.write(element.getFullArmor() + "");
                        bw.write("\t");
                        bw.write(element.getFullStructure() + "");
                        bw.write("\t");
                        bw.write(element.getThreshold() + "");
                        bw.write("\t");
                        bw.write(element.getStandardDamage().S.damage + "");
                        bw.write("\t");
                        bw.write(element.getStandardDamage().S.minimal ? "TRUE" : "FALSE");
                        bw.write("\t");
                        bw.write(element.getStandardDamage().M.damage + "");
                        bw.write("\t");
                        bw.write(element.getStandardDamage().M.minimal ? "TRUE" : "FALSE");
                        bw.write("\t");
                        bw.write(element.getStandardDamage().L.damage + "");
                        bw.write("\t");
                        bw.write(element.getStandardDamage().L.minimal ? "TRUE" : "FALSE");
                        bw.write("\t");
                        bw.write(element.getStandardDamage().E.damage + "");
                        bw.write("\t");
                        bw.write(element.getStandardDamage().E.minimal ? "TRUE" : "FALSE");
                        bw.write("\t");
                        bw.write(element.getOV() + "");
                        bw.write("\t");
                        bw.write(element.getPointValue() + "");
                        bw.write("\t");
                        bw.write(element.getSpecialsDisplayString(", ", element));
                        bw.write("\t");
                        if (element.usesArcs()) {
                            bw.write("FRONT(" + element.getFrontArc().getSpecialsExportString(", ", element) + ")");
                            bw.write("\t");
                            bw.write("LEFT(" + element.getLeftArc().getSpecialsExportString(", ", element) + ")");
                            bw.write("\t");
                            bw.write("RIGHT(" + element.getRightArc().getSpecialsExportString(", ", element) + ")");
                            bw.write("\t");
                            bw.write("REAR(" + element.getRearArc().getSpecialsExportString(", ", element) + ")");
                        } else {
                            bw.write("\t\t\t");
                        }
                        bw.newLine();
                    }
                }
            } catch (Exception ex) {
                LogManager.getLogger().error("", ex);
            }
        }
        System.exit(0);
    }

    private void processUnitExporter() {
        processUnitExporter(false);
    }

    private void processUnitExporter(boolean officialUnitList) {
        String filename;
        if ((getTokenType() == TOK_LITERAL) || officialUnitList) {
            if (officialUnitList) {
                filename = MechFileParser.FILENAME_OFFICIAL_UNITS;
            } else {
                filename = getTokenValue();
            }
            nextToken();

            if (!new File("./docs").exists()) {
                if (!new File("./docs").mkdir()) {
                    LogManager.getLogger().error(
                            "Error in creating directory ./docs. We know this is annoying, and apologise. "
                                    + "Please submit a bug report at https://github.com/MegaMek/megamek/issues "
                                    + " and we will try to resolve your issue.");
                }
            }
            File file = new File("./docs/" + filename);
            try (Writer w = new FileWriter(file); BufferedWriter bw = new BufferedWriter(w)) {
                if (officialUnitList) {
                    bw.write("MegaMek Official Unit List");
                    bw.newLine();
                    bw.write("This file can be regenerated with java -jar MegaMek.jar -oul");
                    bw.newLine();
                    bw.write("Format is: Chassis Model|");
                } else {
                    bw.write("MegaMek Unit Database");
                    bw.newLine();
                    bw.write("This file can be regenerated with java -jar MegaMek.jar -export filename");
                    bw.newLine();
                    bw.write("Type,SubType,Name,Model,BV,Cost (Loaded), Cost (Unloaded),Year,TechLevel,Tonnage,Tech,Canon,Walk,Run,Jump");
                }
                bw.newLine();

                MechSummary[] units = MechSummaryCache.getInstance(officialUnitList).getAllMechs();
                for (MechSummary unit : units) {
                    String unitType = unit.getUnitType();
                    if (unitType.equalsIgnoreCase("mek")) {
                        unitType = "'Mech";
                    }

                    if (!officialUnitList) {
                        bw.write(unitType);
                        bw.write(",");
                        bw.write(unit.getUnitSubType());
                        bw.write(",");
                        bw.write(unit.getChassis());
                        bw.write(",");
                        bw.write(unit.getModel());
                        bw.write(",");
                        bw.write(Integer.toString(unit.getBV()));
                        bw.write(",");
                        bw.write(Long.toString(unit.getCost()));
                        bw.write(",");
                        bw.write(Long.toString(unit.getDryCost()));
                        bw.write(",");
                        bw.write(Integer.toString(unit.getYear()));
                        bw.write(",");
                        bw.write(TechConstants.getLevelDisplayableName(unit.getType()));
                        bw.write(",");
                        bw.write(Double.toString(unit.getTons()));
                        bw.write(",");
                        if (unit.isClan()) {
                            bw.write("Clan,");
                        } else {
                            bw.write("IS,");
                        }
                        if (unit.isCanon()) {
                            bw.write("Canon,");
                        } else {
                            bw.write("Non-Canon,");
                        }
                        bw.write(Integer.toString(unit.getWalkMp()));
                        bw.write(",");
                        bw.write(Integer.toString(unit.getRunMp()));
                        bw.write(",");
                        bw.write(Integer.toString(unit.getJumpMp()));
                    } else {
                        bw.write(unit.getChassis()
                                + (unit.getModel().isBlank() ? "|" : " " + unit.getModel() + "|"));
                    }
                    bw.newLine();
                }
            } catch (Exception ex) {
                LogManager.getLogger().error("", ex);
            }
        }
        System.exit(0);
    }

    private void processRestOfInput() {
        Vector<String> v = new Vector<>();
        while (getArgValue() != null) {
            v.addElement(getArgValue());
            nextArg();
        }
        setToken(TOK_EOF);
        setTokenValue(null);
        restArgs = v.toArray(new String[0]);
    }
}
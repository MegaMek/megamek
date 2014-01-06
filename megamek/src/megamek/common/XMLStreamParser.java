/*
 * MegaMek - Copyright (C) 2003,2004 Ben Mazur (bmazur@sev.org)
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

package megamek.common;

import gd.xml.ParseException;
import gd.xml.XMLParser;
import gd.xml.XMLResponder;

import java.io.InputStream;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

import megamek.common.loaders.EntityLoadingException;

/**
 * This class parses an XML input stream. If the stream is well formed, no
 * <code>Exception</code> will be thrown. If the stream adheres to the format
 * described by the file, "xml-spec.txt", then this class can return entities.
 * If unexpected entities are encountered while parsing a well-formed stream, a
 * warning message will be available.
 *
 * @author Suvarov454@sourceforge.net (James A. Damour )
 * @version $Revision$
 */
public class XMLStreamParser implements XMLResponder {

    // Private attributes and helper functions.

    /**
     * The buffer containing the warning message.
     */
    private StringBuffer warning = new StringBuffer();

    /**
     * The entities parsed from the input stream.
     */
    private Vector<Entity> entities = new Vector<Entity>();

    /**
     * Keep a separate list of pilot/crews parsed becasue dismounted pilots may
     * need to be read separately
     */
    private Vector<Crew> pilots = new Vector<Crew>();

    /**
     * The parser for this object.
     */
    private XMLParser parser = new XMLParser();

    /**
     * The stream currently being parsed.
     */
    private InputStream inStream = null;

    /**
     * The current entity being parsed from the stream.
     */
    private Entity entity = null;

    /**
     * The current location in the entity being parsed.
     */
    private int loc = Entity.LOC_NONE;

    /**
     * The current c3i set in the entity being parsed.
     */
    private int c3i = Entity.NONE;

    /**
     * The current bomb set in the entity being parsed.
     */
    private int bombset = Entity.NONE;
    int[] bombChoices = new int[BombType.B_NUM];

    /**
     * Flag that indicates the current location is destroyed.
     */
    private boolean locDestroyed = false;

    /**
     * Counter for the amount of ammo already handled for the current location.
     */
    private int locAmmoCount = 0;

    /**
     * Marks all equipment in a location on an <code>Entity<code> as destroyed.
     *
     * @param en
     *            - the <code>Entity</code> whose location is destroyed.
     * @param loc
     *            - the <code>int</code> index of the destroyed location.
     */
    private void destroyLocation(Entity en, int loc) {

        // mark armor, internal as destroyed
        en.setArmor(IArmorState.ARMOR_DESTROYED, loc, false);
        en.setInternal(IArmorState.ARMOR_DESTROYED, loc);
        if (en.hasRearArmor(loc)) {
            en.setArmor(IArmorState.ARMOR_DESTROYED, loc, true);
        }
        // equipment marked missing
        for (Mounted mounted : en.getEquipment()) {
            if (mounted.getLocation() == loc) {
                mounted.setDestroyed(true);
            }
        }
        // all critical slots set as missing
        for (int i = 0; i < en.getNumberOfCriticals(loc); i++) {
            final CriticalSlot cs = en.getCritical(loc, i);
            if (cs != null) {
                cs.setDestroyed(true);
            }
        }

        // Mark dependent locations as destroyed.
        // Taharqa: I dont think we should do this here, it should be done
        // anyway
        // if the xml is coded right and we now have to allow for truly blown
        // off locations
        /*
         * if (en.getDependentLocation(loc) != Entity.LOC_NONE) {
         * destroyLocation(en, en.getDependentLocation(loc)); }
         */
    }

    private void breachLocation(Entity en, int loc) {
        // equipment marked breached
        for (Mounted mounted : entity.getEquipment()) {
            if (mounted.getLocation() == loc) {
                mounted.setBreached(true);
            }
        }
        // all critical slots set as breached
        for (int i = 0; i < entity.getNumberOfCriticals(loc); i++) {
            final CriticalSlot cs = entity.getCritical(loc, i);
            if (cs != null) {
                cs.setBreached(true);
            }
        }
        entity.setLocationStatus(loc, ILocationExposureStatus.BREACHED);
    }

    private void blowOffLocation(Entity en, int loc) {
        en.setLocationBlownOff(loc, true);
        for (Mounted mounted : entity.getEquipment()) {
            if (mounted.getLocation() == loc) {
                mounted.setMissing(true);
            }
        }
        for (int i = 0; i < entity.getNumberOfCriticals(loc); i++) {
            final CriticalSlot cs = entity.getCritical(loc, i);
            if (cs != null) {
                cs.setMissing(true);
            }
        }
    }

    // Public and Protected constants, constructors, and methods.

    /**
     * The names of the various elements recognized by this parser.
     */
    public static final String UNIT = "unit";
    public static final String TEMPLATE = "template";
    public static final String ENTITY = "entity";
    public static final String FLUFF = "fluff";
    public static final String PILOT = "pilot";
    public static final String LOCATION = "location";
    public static final String ARMOR = "armor";
    public static final String SLOT = "slot";
    public static final String MOVEMENT = "motive";
    public static final String TURRETLOCK = "turretlock";
    public static final String TURRET2LOCK = "turret2lock";
    public static final String SI = "structural";
    public static final String HEAT = "heat";
    public static final String FUEL = "fuel";
    public static final String KF = "KF";
    public static final String SAIL = "sail";
    public static final String AEROCRIT = "acriticals";
    public static final String TANKCRIT = "tcriticals";
    public static final String STABILIZER = "stabilizer";
    public static final String BREACH = "breached";
    public static final String BLOWN_OFF = "blownOff";
    public static final String C3I = "c3iset";
    public static final String C3ILINK = "c3i_link";
    public static final String LINK = "link";
    public static final String RFMG = "rfmg";

    /**
     * The names of the attributes recognized by this parser. Not every
     * attribute is valid for every element.
     */
    public static final String CHASSIS = "chassis";
    public static final String MODEL = "model";
    public static final String NAME = "name";
    public static final String SIZE = "size";
    public static final String CAMO_CATEGORY = "camoCategory";
    public static final String CAMO_FILENAME = "camoFileName";
    public static final String EXT_ID = "externalId";
    public static final String NICK = "nick";
    public static final String CAT_PORTRAIT = "portraitCat";
    public static final String FILE_PORTRAIT = "portraitFile";
    public static final String GUNNERY = "gunnery";
    public static final String GUNNERYL = "gunneryL";
    public static final String GUNNERYM = "gunneryM";
    public static final String GUNNERYB = "gunneryB";
    public static final String PILOTING = "piloting";
    public static final String ARTILLERY = "artillery";
    public static final String TOUGH = "toughness";
    public static final String INITB = "initB";
    public static final String COMMANDB = "commandB";
    public static final String HITS = "hits";
    public static final String ADVS = "advantages";
    public static final String EDGE = "edge";
    public static final String IMPLANTS = "implants";
    public static final String QUIRKS = "quirks";
    public static final String DRIVER = "driver";
    public static final String COMMANDER = "commander";
    public static final String DEPLOYMENT = "deployment";
    public static final String AUTOEJECT = "autoeject";
    public static final String CONDEJECTAMMO = "condejectammo";
    public static final String CONDEJECTENGINE = "condejectengine";
    public static final String CONDEJECTCTDEST = "condejectctdest";
    public static final String CONDEJECTHEADSHOT = "condejectheadshot";
    public static final String EJECTED = "ejected";
    public static final String INDEX = "index";
    public static final String IS_DESTROYED = "isDestroyed";
    public static final String IS_REPAIRABLE = "isRepairable";
    public static final String POINTS = "points";
    public static final String TYPE = "type";
    public static final String IS_REAR = "isRear";
    public static final String IS_TURRETED = "isTurreted";
    public static final String SHOTS = "shots";
    public static final String IS_HIT = "isHit";
    public static final String MUNITION = "munition";
    public static final String SPEED = "speed";
    public static final String DIRECTION = "direction";
    public static final String INTEGRITY = "integrity";
    public static final String SINK = "sinks";
    public static final String LEFT = "left";
    public static final String AVIONICS = "avionics";
    public static final String SENSORS = "sensors";
    public static final String ENGINE = "engine";
    public static final String FCS = "fcs";
    public static final String CIC = "cic";
    public static final String LEFT_THRUST = "leftThrust";
    public static final String RIGHT_THRUST = "rightThrust";
    public static final String LIFE_SUPPORT = "lifeSupport";
    public static final String GEAR = "gear";
    public static final String MDAMAGE = "damage";
    public static final String MPENALTY = "penalty";
    public static final String C3MASTERIS = "c3MasterIs";
    public static final String C3UUID = "c3UUID";
    public static final String BOMBS = "bombs";
    public static final String BOMB = "bomb";
    public static final String LOAD = "load";

    /**
     * Special values recognized by this parser.
     */
    public static final String DEAD = "Dead";
    public static final String NA = "N/A";
    public static final String DESTROYED = "Destroyed";
    public static final String FRONT = "Front";
    public static final String REAR = "Rear";
    public static final String INTERNAL = "Internal";
    public static final String EMPTY = "Empty";
    public static final String SYSTEM = "System";

    /**
     * No <code>Entity</code>s or warning message are available if the default
     * constructor is used.
     */
    public XMLStreamParser() { /* do nothing */
    }

    /**
     * Parse the indicated XML stream. Any warning message or
     * <code>Entity</code>s from a previously parsed stream will be discarded.
     *
     * @param input
     *            - the <code>InputStream</code> to be parsed.
     * @exception ParseException
     *                is thrown if a fatal error occurs during parsing.
     *                Typically, this only occurs when the XML is not
     *                well-formed.
     */
    public void parse(InputStream input) throws ParseException {
        // Reset the warning message.
        warning = new StringBuffer();

        // Clear the entities.
        entities.removeAllElements();
        pilots.removeAllElements();

        // Parse the input stream.
        inStream = input;
        parser.parseXML(this);
    }

    /**
     * Construct an object and parse the XML stream. Any warning message or
     * <code>Entity</code>s from a previously parsed stream will be discarded.
     *
     * @param input
     *            - the <code>InputStream</code> to be parsed.
     * @exception ParseException
     *                is thrown if a fatal warning occurs during parsing.
     *                Typically, this only occurs when the XML is not
     *                well-formed.
     */
    public XMLStreamParser(InputStream input) throws ParseException {
        parse(input);
    }

    /**
     * Determine if unexpected XML entities were encountered during parsing.
     *
     * @return <code>true</code> if a non-fatal warning occured.
     */
    public boolean hasWarningMessage() {
        return (warning.length() > 0);
    }

    /**
     * Get the warning message from the last parse.
     *
     * @return The <code>String</code> warning message from the last parse. If
     *         there is no warning message, then an <code>null</code> value is
     *         returned.
     */
    public String getWarningMessage() {
        if (warning.length() > 0) {
            return warning.toString();
        }
        return null;
    }

    /**
     * Get any <code>Entity</code>s parsed from the last input stream. Entities
     * may have been parsed out of the stream, even if errors were encountered.
     *
     * @return A <code>Vector</code> containing <code>Entity</code>s parsed from
     *         the stream. This <code>Vector</code> may be empty, but it will
     *         never be <code>null</code>.
     */
    public Vector<Entity> getEntities() {
        // ASSUMPTION : it is safe to return a modifiable reference to the
        // vector. If assumption is wrong, clone the vector.
        return entities;
    }

    public Vector<Crew> getPilots() {
        // ASSUMPTION : it is safe to return a modifiable reference to the
        // vector. If assumption is wrong, clone the vector.
        return pilots;
    }

    // Implementation of the XMLResponder interface:

    public void recordNotationDeclaration(String name, String pubID,
            String sysID) throws ParseException {
        // Do nothing.
    }

    public void recordEntityDeclaration(String name, String value,
            String pubID, String sysID, String notation) throws ParseException {
        // Do nothing.
    }

    public void recordElementDeclaration(String name, String content)
            throws ParseException {
        // Do nothing.
    }

    public void recordAttlistDeclaration(String element, String attr,
            boolean notation, String type, String defmod, String def)
            throws ParseException {
        // Do nothing.
    }

    public void recordDoctypeDeclaration(String name, String pubID, String sysID)
            throws ParseException {
        // Do nothing.
    }

    public void recordDocStart() {
        // Do nothing.
    }

    public void recordDocEnd() {
        // Do nothing.
    }

    @SuppressWarnings({ "rawtypes" })
    public void recordElementStart(String name, Hashtable attr)
            throws ParseException {

        // TODO: handle template files.

        // What kind of element have we started?
        if (name.equals(UNIT)) {

            // Are we in the middle of parsing an Entity?
            if (entity != null) {
                warning.append("Found a unit while parsing an Entity.\n");
            }

            // Are there *multiple* units?
            else if (!entities.isEmpty()) {
                warning.append("Found a second unit.  Clearing first unit.\n");

                // Restart the unit list.
                entities.removeAllElements();
                pilots.removeAllElements();
            }

        } else if (name.equals(TEMPLATE)) {
            // Do nothing.
        } else if (name.equals(ENTITY)) {

            // Are we in the middle of parsing an Entity?
            if (entity != null) {
                warning.append("Found another Entity while parsing an Entity.\n");
            }

            // Are we in the middle of parsing an Entity's location?
            else if (loc != Entity.LOC_NONE) {
                warning.append("Found another Entity while parsing a location.\n");
            }

            // Start a new entity.
            else {

                // Look for the element's attributes.
                String chassis = (String) attr.get(CHASSIS);
                String model = (String) attr.get(MODEL);

                // Did we find required attributes?
                if ((chassis == null) || (chassis.length() == 0)) {
                    warning.append("Could not find chassis for Entity.\n");
                } else {

                    // Try to find the entity.
                    MechSummary ms = null;
                    StringBuffer key = new StringBuffer(chassis);
                    ms = MechSummaryCache.getInstance().getMech(key.toString());
                    if ((model != null) && (model.length() > 0)) {
                        key.append(" ").append(model);
                        ms = MechSummaryCache.getInstance().getMech(
                                key.toString());
                        // That didn't work. Try swaping model and chassis.
                        if (ms == null) {
                            key = new StringBuffer(model);
                            key.append(" ").append(chassis);
                            ms = MechSummaryCache.getInstance().getMech(
                                    key.toString());
                        }
                    }

                    // We should have found the mech.
                    if (ms == null) {
                        warning.append("Could not find Entity with chassis: ");
                        warning.append(chassis);
                        if ((model != null) && (model.length() > 0)) {
                            warning.append(", and model: ");
                            warning.append(model);
                        }
                        warning.append(".\n");
                    } else {

                        // Try to load the new mech.
                        try {
                            entity = new MechFileParser(ms.getSourceFile(),
                                    ms.getEntryName()).getEntity();
                        } catch (EntityLoadingException excep) {
                            excep.printStackTrace(System.err);
                            warning.append("Unable to load mech: ")
                                    .append(ms.getSourceFile()).append(": ")
                                    .append(ms.getEntryName()).append(": ")
                                    .append(excep.getMessage());
                        }
                    } // End found-MechSummary

                } // End have-chassis

                if (null != entity) {

                    // commander
                    boolean commander = Boolean.parseBoolean((String) attr
                            .get(COMMANDER));
                    entity.setCommander(commander);

                    // deployment round
                    try {
                        int deployround = Integer.parseInt((String) attr
                                .get(DEPLOYMENT));
                        entity.setDeployRound(deployround);
                    } catch (Exception e) {
                        entity.setDeployRound(0);
                    }

                    // Camo
                    entity.setCamoCategory((String) attr.get(CAMO_CATEGORY));
                    entity.setCamoFileName((String) attr.get(CAMO_FILENAME));

                    // external id
                    String extId = (String) attr.get(EXT_ID);
                    if ((null == extId) || (extId.length() == 0)) {
                        extId = "-1";
                    }
                    entity.setExternalIdAsString(extId);

                    // quirks
                    String quirks = (String) attr.get(QUIRKS);
                    if ((null != quirks) && (quirks.trim().length() > 0)) {
                        StringTokenizer st = new StringTokenizer(quirks, "::");
                        while (st.hasMoreTokens()) {
                            String quirk = st.nextToken();
                            String quirkName = Crew.parseAdvantageName(quirk);
                            Object value = Crew.parseAdvantageValue(quirk);

                            try {
                                entity.getQuirks().getOption(quirkName)
                                        .setValue(value);
                            } catch (Exception e) {
                                warning.append("Error restoring quirk: ")
                                        .append(quirk).append(".\n");
                            }
                        }
                    }

                    // Setup for C3 Relinking
                    String c3masteris = (String) attr.get(C3MASTERIS);
                    if (c3masteris != null) {
                        entity.setC3MasterIsUUIDAsString(c3masteris);
                    }
                    String c3uuid = (String) attr.get(C3UUID);
                    if (c3uuid != null) {
                        entity.setC3UUIDAsString(c3uuid);
                    }
                }

            } // End ready-for-new-Entity

        } else if (name.equals(C3I)) {
            // Are we in the outside of an Entity?
            if (entity == null) {
                warning.append("Found a C3i set outside of an Entity.\n");
            }
            // Are we in the middle of parsing an Entity's location?
            else if (c3i != Entity.NONE) {
                warning.append("Found a c3i set while parsing a c3i set.\n");
            } else {
                c3i = 1;
            }
        } else if (name.equals(C3ILINK)) {

            // Are we in the outside of an Entity?
            if (entity == null) {
                warning.append("Found a c3i_link outside of an Entity.\n");
            }

            // Are we in the outside of parsing an Entity's location?
            else if (c3i == Entity.NONE) {
                warning.append("Found a c3i_link while outside of a c3i set.\n");
            }

            // Handle the location.
            else {
                String link = (String) attr.get(LINK);
                int pos = entity.getFreeC3iUUID();

                if ((link != null) && (pos != -1)) {
                    System.out.println("Loading C3i UUID " + pos + ": " + link);
                    entity.setC3iNextUUIDAsString(pos, link);
                }
            }
        } else if (name.equals(BOMBS)) {
            // Are we in the outside of an Entity?
            if (entity == null) {
                warning.append("Found a Bomb set outside of an Entity.\n");
            }
            // Are we in the middle of parsing an Entity's location?
            else if (bombset != Entity.NONE) {
                warning.append("Found a Bomb set while parsing a Bomb set.\n");
            } else {
                bombset = 1;
            }
        } else if (name.equals(BOMB)) {

            // Are we in the outside of an Entity?
            if (entity == null) {
                warning.append("Found a bomb outside of an Entity.\n");
            }

            // Is this entity not an Aero?
            else if (!(entity instanceof Aero)) {
                warning.append("Found a bomb but Entity is not a Fighter.\n");
            }

            // Are we in the outside of parsing an Entity's location?
            else if (bombset == Entity.NONE) {
                warning.append("Found a bomb while outside of a Bomb set.\n");
            }

            // Handle the location.
            else {
                bombChoices = ((Aero) entity).getBombChoices();
                String type = (String) attr.get(TYPE);
                String load = (String) attr.get(LOAD);
                bombChoices[Integer.parseInt(type)] = Integer.parseInt(load);
            }
            ((Aero) entity).setBombChoices(bombChoices);
        } else if (name.equals(FLUFF)) {
            // Do nothing.
        } else if (name.equals(PILOT)) {

            // Are we in the outside of an Entity?
            // pilots outside of entities will still be added to a pilot vector
            // now
            /*
             * if (entity == null) {
             * warning.append("Found a pilot outside of an Entity.\n"); }
             */

            // Are we in the middle of parsing an Entity's location?
            if (loc != Entity.LOC_NONE) {
                warning.append("Found a pilot while parsing a location.\n");
            }

            // Handle the pilot.
            else {

                // Look for the element's attributes.
                String pilotName = (String) attr.get(NAME);
                String pilotSize = (String) attr.get(SIZE);
                String pilotNickname = (String) attr.get(NICK);
                String gunnery = (String) attr.get(GUNNERY);
                String gunneryL = (String) attr.get(GUNNERYL);
                String gunneryM = (String) attr.get(GUNNERYM);
                String gunneryB = (String) attr.get(GUNNERYB);
                String piloting = (String) attr.get(PILOTING);
                String artillery = (String) attr.get(ARTILLERY);
                String tough = (String) attr.get(TOUGH);
                String initB = (String) attr.get(INITB);
                String commandB = (String) attr.get(COMMANDB);
                String hits = (String) attr.get(HITS);
                String advantages = (String) attr.get(ADVS);
                String edge = (String) attr.get(EDGE);
                String implants = (String) attr.get(IMPLANTS);
                String autoeject = (String) attr.get(AUTOEJECT);
                String condejectammo = (String) attr.get(CONDEJECTAMMO);
                String condejectengine = (String) attr.get(CONDEJECTENGINE);
                String condejectctdest = (String) attr.get(CONDEJECTCTDEST);
                String condejectheadshot = (String) attr.get(CONDEJECTHEADSHOT);
                String ejected = (String) attr.get(EJECTED);
                String extId = (String) attr.get(EXT_ID);
                String portraitCategory = (String) attr.get(CAT_PORTRAIT);
                String portraitFile = (String) attr.get(FILE_PORTRAIT);

                // Did we find required attributes?
                if ((gunnery == null) || (gunnery.length() == 0)) {
                    warning.append("Could not find gunnery for pilot.\n");
                } else if ((piloting == null) || (piloting.length() == 0)) {
                    warning.append("Could not find piloting for pilot.\n");
                } else {

                    // Try to get a good gunnery value.
                    int gunVal = -1;
                    try {
                        gunVal = Integer.parseInt(gunnery);
                    } catch (NumberFormatException excep) {
                        // Handled by the next if test.
                    }
                    if ((gunVal < 0) || (gunVal > 8)) {
                        warning.append("Found invalid gunnery value: ")
                                .append(gunnery).append(".\n");
                        return;
                    }

                    // Try to get a good piloting value.
                    int pilotVal = -1;
                    try {
                        pilotVal = Integer.parseInt(piloting);
                    } catch (NumberFormatException excep) {
                        // Handled by the next if test.
                    }
                    if ((pilotVal < 0) || (pilotVal > 8)) {
                        warning.append("Found invalid piloting value: ")
                                .append(piloting).append(".\n");
                        return;
                    }

                    // toughness
                    int toughVal = 0;
                    if ((null != tough) && (tough.length() > 0)) {
                        try {
                            toughVal = Integer.parseInt(tough);
                        } catch (NumberFormatException excep) {
                            // Handled by the next if test.
                        }
                    }

                    // init bonus
                    int initBVal = 0;
                    if ((null != initB) && (initB.length() > 0)) {
                        try {
                            initBVal = Integer.parseInt(initB);
                        } catch (NumberFormatException excep) {
                            // Handled by the next if test.
                        }
                    }
                    int commandBVal = 0;
                    if ((null != commandB) && (commandB.length() > 0)) {
                        try {
                            commandBVal = Integer.parseInt(commandB);
                        } catch (NumberFormatException excep) {
                            // Handled by the next if test.
                        }
                    }
                    // get RPG skills
                    int gunneryLVal = gunVal;
                    int gunneryMVal = gunVal;
                    int gunneryBVal = gunVal;
                    if ((null != gunneryL) && (gunneryL.length() > 0)) {
                        try {
                            gunneryLVal = Integer.parseInt(gunneryL);
                        } catch (NumberFormatException excep) {
                            // Handled by the next if test.
                        }
                        if ((gunneryLVal < 0) || (gunneryLVal > 7)) {
                            warning.append("Found invalid piloting value: ")
                                    .append(gunneryL).append(".\n");
                            return;
                        }
                    }
                    if ((null != gunneryM) && (gunneryM.length() > 0)) {
                        try {
                            gunneryMVal = Integer.parseInt(gunneryM);
                        } catch (NumberFormatException excep) {
                            // Handled by the next if test.
                        }
                        if ((gunneryMVal < 0) || (gunneryMVal > 7)) {
                            warning.append("Found invalid piloting value: ")
                                    .append(gunneryM).append(".\n");
                            return;
                        }
                    }
                    if ((null != gunneryB) && (gunneryB.length() > 0)) {
                        try {
                            gunneryBVal = Integer.parseInt(gunneryB);
                        } catch (NumberFormatException excep) {
                            // Handled by the next if test.
                        }
                        if ((gunneryBVal < 0) || (gunneryBVal > 7)) {
                            warning.append("Found invalid piloting value: ")
                                    .append(gunneryB).append(".\n");
                            return;
                        }
                    }

                    int artVal = gunVal;
                    if ((null != artillery) && (artillery.length() > 0)) {
                        try {
                            artVal = Integer.parseInt(artillery);
                        } catch (NumberFormatException excep) {
                            // Handled by the next if test.
                        }
                        if ((artVal < 0) || (artVal > 7)) {
                            warning.append("Found invalid artillery value: ")
                                    .append(artillery).append(".\n");
                            return;
                        }
                    }

                    if ((null == pilotName) || (pilotName.length() == 0)) {
                        pilotName = "Unnamed";
                    }

                    Crew crew = new Crew(pilotName, 1, gunneryLVal, gunneryMVal,
                            gunneryBVal, pilotVal);
                    
                    crew.setSize(Integer.parseInt(pilotSize));

                    if ((null != pilotNickname) && (pilotNickname.length() > 0)) {
                        crew.setNickname(pilotNickname);
                    }
                    if ((null != portraitCategory)
                            && (portraitCategory.length() > 0)) {
                        crew.setPortraitCategory(portraitCategory);
                    }
                    if ((null != portraitFile) && (portraitFile.length() > 0)) {
                        crew.setPortraitFileName(portraitFile);
                    }
                    crew.setArtillery(artVal);
                    crew.setToughness(toughVal);
                    crew.setInitBonus(initBVal);
                    crew.setCommandBonus(commandBVal);
                    if ((null != advantages)
                            && (advantages.trim().length() > 0)) {
                        StringTokenizer st = new StringTokenizer(advantages,
                                "::");
                        while (st.hasMoreTokens()) {
                            String adv = st.nextToken();
                            String advName = Crew.parseAdvantageName(adv);
                            Object value = Crew.parseAdvantageValue(adv);

                            try {
                                crew.getOptions().getOption(advName)
                                        .setValue(value);
                            } catch (Exception e) {
                                warning.append("Error restoring advantage: ")
                                        .append(adv).append(".\n");
                            }
                        }

                    }
                    if ((null != edge) && (edge.trim().length() > 0)) {
                        StringTokenizer st = new StringTokenizer(edge, "::");
                        while (st.hasMoreTokens()) {
                            String edg = st.nextToken();
                            String edgeName = Crew.parseAdvantageName(edg);
                            Object value = Crew.parseAdvantageValue(edg);

                            try {
                                crew.getOptions().getOption(edgeName)
                                        .setValue(value);
                            } catch (Exception e) {
                                warning.append("Error restoring edge: ")
                                        .append(edg).append(".\n");
                            }
                        }
                    }
                    if ((null != implants) && (implants.trim().length() > 0)) {
                        StringTokenizer st = new StringTokenizer(implants, "::");
                        while (st.hasMoreTokens()) {
                            String implant = st.nextToken();
                            String implantName = Crew
                                    .parseAdvantageName(implant);
                            Object value = Crew.parseAdvantageValue(implant);

                            try {
                                crew.getOptions().getOption(implantName)
                                        .setValue(value);
                            } catch (Exception e) {
                                warning.append("Error restoring implants: ")
                                        .append(implant).append(".\n");
                            }
                        }

                    }

                    // Was the crew wounded?
                    if (hits != null) {
                        // Try to get a good hits value.
                        int hitVal = -1;
                        try {
                            hitVal = Integer.parseInt(hits);
                        } catch (NumberFormatException excep) {
                            // Handled by the next if test.
                        }
                        if (hits.equals(DEAD)) {
                            crew.setDead(true);
                            warning.append("The pilot, ").append(pilotName)
                                    .append(", is dead.\n");
                        } else if ((hitVal < 0) || (hitVal > 5)) {
                            warning.append("Found invalid hits value: ")
                                    .append(hits).append(".\n");
                        } else {
                            crew.setHits(hitVal);
                        }

                    } // End have-hits

                    if (ejected != null) {
                        crew.setEjected(Boolean.parseBoolean(ejected));
                    }

                    if ((null == extId) || (extId.length() == 0)) {
                        extId = "-1";
                    }
                    crew.setExternalIdAsString(extId);

                    pilots.add(crew);
                    if (null != entity) {
                        // Set the crew for this entity.
                        entity.setCrew(crew);

                        if (autoeject != null) {
                            if (autoeject.equals("true")) {
                                ((Mech) entity).setAutoEject(true);
                            } else {
                                ((Mech) entity).setAutoEject(false);
                            }
                        }
                        if (condejectammo != null) {
                            if (condejectammo.equals("true")) {
                                ((Mech) entity).setCondEjectAmmo(true);
                            } else {
                                ((Mech) entity).setCondEjectAmmo(false);
                            }
                        }
                        if (condejectengine != null) {
                            if (condejectengine.equals("true")) {
                                ((Mech) entity).setCondEjectEngine(true);
                            } else {
                                ((Mech) entity).setCondEjectEngine(false);
                            }
                        }
                        if (condejectctdest != null) {
                            if (condejectctdest.equals("true")) {
                                ((Mech) entity).setCondEjectCTDest(true);
                            } else {
                                ((Mech) entity).setCondEjectCTDest(false);
                            }
                        }
                        if (condejectheadshot != null) {
                            if (condejectheadshot.equals("true")) {
                                ((Mech) entity).setCondEjectHeadshot(true);
                            } else {
                                ((Mech) entity).setCondEjectHeadshot(false);
                            }
                        }
                    }

                } // End have-required-fields
            } // End ready-for-pilot
        } else if (name.equals(LOCATION)) {

            // Are we in the outside of an Entity?
            if (entity == null) {
                warning.append("Found a location outside of an Entity.\n");
            }

            // Are we in the middle of parsing an Entity's location?
            else if (loc != Entity.LOC_NONE) {
                warning.append("Found a location while parsing a location.\n");
            }

            // Handle the location.
            else {

                // Look for the element's attributes.
                String index = (String) attr.get(INDEX);
                String destroyed = (String) attr.get(IS_DESTROYED);

                // Did we find required attributes?
                if ((index == null) || (index.length() == 0)) {
                    warning.append("Could not find index for location.\n");
                } else {

                    // Try to get a good index value.
                    int indexVal = -1;
                    try {
                        indexVal = Integer.parseInt(index);
                    } catch (NumberFormatException excep) {
                        // Handled by the next if test.
                    }
                    if ((indexVal < 0) || (indexVal > 7)) {
                        warning.append(
                                "Found invalid index value for location: ")
                                .append(index).append(".\n");
                        return;
                    } else if (indexVal >= entity.locations()) {
                        warning.append("The entity, ")
                                .append(entity.getShortName())
                                .append(" does not have a location at index: ")
                                .append(indexVal).append(".\n");
                        return;
                    } else {

                        // We're now parsing the indexed location.
                        loc = indexVal;

                        // Reset the ammo count.
                        locAmmoCount = 0;

                        // Is the location destroyed?
                        locDestroyed = false;
                        try {
                            if (destroyed != null) {
                                locDestroyed = destroyed.equals("true");
                            }
                        } catch (Throwable excep) {
                            warning.append("Found invalid isDestroyed value: ")
                                    .append(destroyed).append(".\n");
                        }

                    } // End have-valid-index
                } // End have-required-fields
            } // End ready-for-location
        } else if (name.equals(TURRETLOCK)) {
            // Are we in the outside of an Entity?
            if (entity == null) {
                warning.append("Found turret lock outside of an Entity.\n");
            } else if (!(entity instanceof Tank)) {
                warning.append("Turret crit record found outside a Tank.\n");
            }
            String value = (String) attr.get(DIRECTION);
            try {
                int turDir = Integer.parseInt(value);
                ((Tank) entity).setSecondaryFacing(turDir);
                ((Tank) entity).lockTurret(((Tank)entity).getLocTurret());
            } catch (Exception e) {
                System.err.println(e);
                e.printStackTrace();
                warning.append("Invalid turret lock direction value in movement tag.\n");
            }
        } else if (name.equals(TURRET2LOCK)) {
            // Are we in the outside of an Entity?
            if (entity == null) {
                warning.append("Found turret2 lock outside of an Entity.\n");
            } else if (!(entity instanceof Tank)) {
                warning.append("Turret2 crit record found outside a Tank.\n");
            }
            String value = (String) attr.get(DIRECTION);
            try {
                int turDir = Integer.parseInt(value);
                ((Tank) entity).setDualTurretOffset(turDir);
                ((Tank) entity).lockTurret(((Tank)entity).getLocTurret2());
            } catch (Exception e) {
                System.err.println(e);
                e.printStackTrace();
                warning.append("Invalid turret2 lock direction value in movement tag.\n");
            }
        } else if (name.equals(MOVEMENT)) {
            // Are we in the outside of an Entity?
            if (entity == null) {
                warning.append("Found movement crit outside of an Entity.\n");
            } else if (!(entity instanceof Tank)) {
                warning.append("Movement crit record found outside a Tank.\n");
            }
            String value = (String) attr.get(MDAMAGE);
            try {
                int motiveDamage = Integer.parseInt(value);
                ((Tank) entity).setMotiveDamage(motiveDamage);
                if (motiveDamage >= ((Tank) entity).getOriginalWalkMP()) {
                    ((Tank) entity).immobilize();
                    ((Tank) entity).applyDamage();
                }
            } catch (Exception e) {
                warning.append("Invalid motive damage value in movement tag.\n");
            }
            value = (String) attr.get(MPENALTY);
            try {
                int motivePenalty = Integer.parseInt(value);
                ((Tank) entity).setMotivePenalty(motivePenalty);
            } catch (Exception e) {
                warning.append("Invalid motive penalty value in movement tag.\n");
            }
        } else if (name.equals(ARMOR)) {

            // Are we in the outside of an Entity?
            if (entity == null) {
                warning.append("Found armor outside of an Entity.\n");
            }

            // Are we in the outside of parsing an Entity's location?
            else if (loc == Entity.LOC_NONE) {
                warning.append("Found armor while outside of a location.\n");
            }

            // Handle the location.
            else {

                // Look for the element's attributes.
                String points = (String) attr.get(POINTS);
                String type = (String) attr.get(TYPE);

                // Did we find required attributes?
                if ((points == null) || (points.length() == 0)) {
                    warning.append("Could not find points for armor.\n");
                } else {

                    // Try to get a good points value.
                    int pointsVal = -1;
                    try {
                        pointsVal = Integer.parseInt(points);
                    } catch (NumberFormatException excep) {
                        // Handled by the next if test.
                    }
                    if (points.equals(NA)) {
                        pointsVal = IArmorState.ARMOR_NA;
                    } else if (points.equals(DESTROYED)) {
                        pointsVal = IArmorState.ARMOR_DESTROYED;
                    } else if ((pointsVal < 0) || (pointsVal > 2000)) {
                        warning.append("Found invalid points value: ")
                                .append(points).append(".\n");
                        return;
                    }

                    // Assign the points to the correct location.
                    // Sanity check the armor value before setting it.
                    if ((type == null) || type.equals(FRONT)) {
                        if (entity.getOArmor(loc) < pointsVal) {
                            warning.append("The entity, ")
                                    .append(entity.getShortName())
                                    .append(" does not start with ")
                                    .append(pointsVal)
                                    .append(" points of armor for location: ")
                                    .append(loc).append(".\n");
                        } else {
                            entity.setArmor(pointsVal, loc);
                        }
                    } else if (type.equals(INTERNAL)) {
                        if (entity.getOInternal(loc) < pointsVal) {
                            warning.append("The entity, ")
                                    .append(entity.getShortName())
                                    .append(" does not start with ")
                                    .append(pointsVal)
                                    .append(" points of internal structure for location: ")
                                    .append(loc).append(".\n");
                        } else {
                            entity.setInternal(pointsVal, loc);
                        }
                    } else if (type.equals(REAR)) {
                        if (!entity.hasRearArmor(loc)) {
                            warning.append("The entity, ")
                                    .append(entity.getShortName())
                                    .append(" has no rear armor for location: ")
                                    .append(loc).append(".\n");
                        } else if (entity.getOArmor(loc, true) < pointsVal) {
                            warning.append("The entity, ")
                                    .append(entity.getShortName())
                                    .append(" does not start with ")
                                    .append(pointsVal)
                                    .append(" points of rear armor for location: ")
                                    .append(loc).append(".\n");
                        } else {
                            entity.setArmor(pointsVal, loc, true);
                        }
                    }
                } // End have-required-fields
            } // End ready-for-armor
        } else if (name.equals(BREACH)) {

            // Are we in the outside of an Entity?
            if (entity == null) {
                warning.append("Found breach outside of an Entity.\n");
            }

            // Are we in the outside of parsing an Entity's location?
            else if (loc == Entity.LOC_NONE) {
                warning.append("Found breach while outside of a location.\n");
            }

            // Handle the location.
            else {
                breachLocation(entity, loc);
            }
        } else if (name.equals(BLOWN_OFF)) {

            // Are we in the outside of an Entity?
            if (entity == null) {
                warning.append("Found blown off outside of an Entity.\n");
            }

            // Are we in the outside of parsing an Entity's location?
            else if (loc == Entity.LOC_NONE) {
                warning.append("Found blown off while outside of a location.\n");
            }

            // Handle the location.
            else {
                blowOffLocation(entity, loc);
            }
        } else if (name.equals(STABILIZER)) {

            // Are we in the outside of an Entity?
            if (entity == null) {
                warning.append("Found stabilizer outside of an Entity.\n");
            }
            // are we in a tank?
            else if (!(entity instanceof Tank)) {
                warning.append("Found stabilizer outside of an Tank.\n");
            }
            // Are we in the outside of parsing an Entity's location?
            else if (loc == Entity.LOC_NONE) {
                warning.append("Found stabilizer while outside of a location.\n");
            }
            // Handle the location.
            else {
                // Look for the element's attributes.
                String hit = (String) attr.get(IS_HIT);
                if (null != hit) {
                    ((Tank) entity).setStabiliserHit(loc);
                }
            } // End ready-for-stabilizier
        } else if (name.equals(SI)) {
            if (entity == null) {
                warning.append("Found structural integrity outside of an Entity.\n");
            } else if (!(entity instanceof Aero)) {
                warning.append("structural integrity record found outside an Aero.\n");
            }
            String value = (String) attr.get(INTEGRITY);
            try {
                int newSI = Integer.parseInt(value);
                ((Aero) entity).setSI(newSI);
            } catch (Exception e) {
                warning.append("Invalid SI value in structural integrity tag.\n");
            }
        } else if (name.equals(HEAT)) {
            if (entity == null) {
                warning.append("Found heat sink outside of an Entity.\n");
            } else if (!(entity instanceof Aero)) {
                warning.append("heat sink record found outside an Aero.\n");
            }
            String value = (String) attr.get(SINK);
            try {
                int newSinks = Integer.parseInt(value);
                ((Aero) entity).setHeatSinks(newSinks);
            } catch (Exception e) {
                warning.append("Invalid heat sink value in heat sink tag.\n");
            }
        } else if (name.equals(FUEL)) {
            if (entity == null) {
                warning.append("Found fuel outside of an Entity.\n");
            } else if (!(entity instanceof Aero)) {
                warning.append("fuel record found outside an Aero.\n");
            }
            String value = (String) attr.get(LEFT);
            try {
                int newFuel = Integer.parseInt(value);
                ((Aero) entity).setFuel(newFuel);
            } catch (Exception e) {
                warning.append("Invalid fuel value in fuel tag.\n");
            }
        } else if (name.equals(KF)) {
            if (entity == null) {
                warning.append("Found KF integrity outside of an Entity.\n");
            } else if (!(entity instanceof Jumpship)) {
                warning.append("KF integrity record found outside a Jumpship.\n");
            }
            String value = (String) attr.get(INTEGRITY);
            try {
                int newIntegrity = Integer.parseInt(value);
                ((Jumpship) entity).setKFIntegrity(newIntegrity);
            } catch (Exception e) {
                warning.append("Invalid KF integrity value in KF integrity tag.\n");
            }
        } else if (name.equals(SAIL)) {
            if (entity == null) {
                warning.append("Found sail integrity outside of an Entity.\n");
            } else if (!(entity instanceof Jumpship)) {
                warning.append("sail integrity record found outside a Jumpship.\n");
            }
            String value = (String) attr.get(INTEGRITY);
            try {
                int newIntegrity = Integer.parseInt(value);
                ((Jumpship) entity).setSailIntegrity(newIntegrity);
            } catch (Exception e) {
                warning.append("Invalid sail integrity value in sail integrity tag.\n");
            }
        } else if (name.equals(AEROCRIT)) {
            if (entity == null) {
                warning.append("Found aero crits outside of an Entity.\n");
            } else if (!(entity instanceof Aero)) {
                warning.append("Found aero crits outside of an Aero.\n");
            } else {
                String avionics = (String) attr.get(AVIONICS);
                String sensors = (String) attr.get(SENSORS);
                String engine = (String) attr.get(ENGINE);
                String fcs = (String) attr.get(FCS);
                String cic = (String) attr.get(CIC);
                String leftThrust = (String) attr.get(LEFT_THRUST);
                String rightThrust = (String) attr.get(RIGHT_THRUST);
                String lifeSupport = (String) attr.get(LIFE_SUPPORT);
                String gear = (String) attr.get(GEAR);

                Aero a = (Aero) entity;

                if (avionics != null) {
                    a.setAvionicsHits(Integer.parseInt(avionics));
                }

                if (sensors != null) {
                    a.setSensorHits(Integer.parseInt(sensors));
                }

                if (engine != null) {
                    a.setEngineHits(Integer.parseInt(engine));
                }

                if (fcs != null) {
                    a.setFCSHits(Integer.parseInt(fcs));
                }

                if (cic != null) {
                    a.setCICHits(Integer.parseInt(cic));
                }

                if (leftThrust != null) {
                    a.setLeftThrustHits(Integer.parseInt(leftThrust));
                }

                if (rightThrust != null) {
                    a.setRightThrustHits(Integer.parseInt(rightThrust));
                }

                if (lifeSupport != null) {
                    a.setLifeSupport(false);
                }

                if (gear != null) {
                    a.setGearHit(true);
                }
            }
        } else if (name.equals(TANKCRIT)) {
            if (entity == null) {
                warning.append("Found tank crits outside of an Entity.\n");
            } else if (!(entity instanceof Tank)) {
                warning.append("Found tank crits outside of an Tank.\n");
            } else {
                String sensors = (String) attr.get(SENSORS);
                String engine = (String) attr.get(ENGINE);
                String driver = (String) attr.get(DRIVER);
                String commander = (String) attr.get(COMMANDER);

                Tank t = (Tank) entity;

                if (sensors != null) {
                    t.setSensorHits(Integer.parseInt(sensors));
                }

                if ((engine != null) && engine.equalsIgnoreCase("hit")) {
                    t.engineHit();
                    t.applyDamage();
                }

                if ((driver != null) && driver.equalsIgnoreCase("hit")) {
                    t.setDriverHit(true);
                }

                if ((commander != null) && commander.equalsIgnoreCase("hit")) {
                    t.setCommanderHit(true);
                }

            }
        } else if (name.equals(SLOT)) {

            // Are we in the outside of an Entity?
            if (entity == null) {
                warning.append("Found a slot outside of an Entity.\n");
            }

            // Are we in the outside of parsing an Entity's location?
            else if (loc == Entity.LOC_NONE) {
                warning.append("Found a slot while outside of a location.\n");
            }

            // Handle the location.
            else {

                // Look for the element's attributes.
                String index = (String) attr.get(INDEX);
                String type = (String) attr.get(TYPE);
                // String rear = (String) attr.get( IS_REAR ); // is never read.
                String shots = (String) attr.get(SHOTS);
                String hit = (String) attr.get(IS_HIT);
                String destroyed = (String) attr.get(IS_DESTROYED);
                String repairable = (String) attr.get(IS_REPAIRABLE);
                String munition = (String) attr.get(MUNITION);
                String quirks = (String) attr.get(QUIRKS);
                String rfmg = (String) attr.get(RFMG);

                // Did we find required attributes?
                if ((index == null) || (index.length() == 0)) {
                    warning.append("Could not find index for slot.\n");
                } else if ((type == null) || (type.length() == 0)) {
                    warning.append("Could not find type for slot.\n");
                } else {

                    // Try to get a good index value.
                    // Remember, slot index starts at 1.
                    int indexVal = -1;
                    try {
                        indexVal = Integer.parseInt(index);
                        indexVal -= 1;
                    } catch (NumberFormatException excep) {
                        // Handled by the next if test.
                    }
                    if (index.equals(NA)) {
                        indexVal = IArmorState.ARMOR_NA;

                        // Tanks don't have slots, and Protomechs only have
                        // system slots, so we have to handle the ammo
                        // specially.
                        if ((entity instanceof Tank)
                                || (entity instanceof Protomech)) {

                            // Get the saved ammo load.
                            EquipmentType newLoad = EquipmentType.get(type);
                            if (newLoad instanceof AmmoType) {
                                int counter = -1;
                                Iterator<Mounted> ammo = entity.getAmmo()
                                        .iterator();
                                while (ammo.hasNext()
                                        && (counter < locAmmoCount)) {

                                    // Is this mounted in the current location?
                                    Mounted mounted = ammo.next();
                                    if (mounted.getLocation() == loc) {

                                        // Increment the loop counter.
                                        counter++;

                                        // Is this the one we want to handle?
                                        if (counter == locAmmoCount) {

                                            // Increment the counter of ammo
                                            // handled for this location.
                                            locAmmoCount++;

                                            // Reset transient values.
                                            mounted.restore();

                                            // Try to get a good shots value.
                                            int shotsVal = -1;
                                            try {
                                                shotsVal = Integer
                                                        .parseInt(shots);
                                            } catch (NumberFormatException excep) {
                                                // Handled by the next if test.
                                            }
                                            if (shots.equals(NA)) {
                                                shotsVal = IArmorState.ARMOR_NA;
                                                warning.append(
                                                        "Expected to find number of shots for ")
                                                        .append(type)
                                                        .append(", but found ")
                                                        .append(shots)
                                                        .append(" instead.\n");
                                            } else if ((shotsVal < 0)
                                                    || (shotsVal > 200)) {
                                                warning.append(
                                                        "Found invalid shots value for slot: ")
                                                        .append(shots)
                                                        .append(".\n");
                                            } else {

                                                // Change to the saved
                                                // ammo type and shots.
                                                mounted.changeAmmoType((AmmoType) newLoad);
                                                mounted.setShotsLeft(shotsVal);

                                            } // End have-good-shots-value

                                            // Stop looking for a match.
                                            break;

                                        } // End found-match-for-slot

                                    } // End ammo-in-this-loc

                                } // Check the next ammo.

                            } else {
                                // Bad XML equipment.
                                warning.append("XML file lists ")
                                        .append(type)
                                        .append(" equipment at location ")
                                        .append(loc)
                                        .append(".  XML parser expected ammo.\n");
                            } // End not-ammo-type

                        } // End is-tank

                        // TODO: handle slotless equipment.
                        return;
                    } else if ((indexVal < 0) || (indexVal > 12)) {
                        warning.append("Found invalid index value for slot: ")
                                .append(index).append(".\n");
                        return;
                    }

                    // Is this index valid for this entity?
                    if (indexVal > entity.getNumberOfCriticals(loc)) {
                        warning.append("The entity, ")
                                .append(entity.getShortName())
                                .append(" does not have ").append(index)
                                .append(" slots in location ").append(loc)
                                .append(".\n");
                        return;
                    }

                    // Try to get a good isHit value.
                    boolean hitFlag = false;
                    try {
                        if (hit != null) {
                            hitFlag = hit.equals("true");
                        }
                    } catch (Throwable excep) {
                        warning.append("Found invalid isHit value: ")
                                .append(hit).append(".\n");
                    }

                    // Is the location destroyed?
                    boolean destFlag = false;
                    try {
                        if (destroyed != null) {
                            destFlag = destroyed.equals("true");
                        }
                    } catch (Throwable excep) {
                        warning.append("Found invalid isDestroyed value: ")
                                .append(destroyed).append(".\n");
                    }

                    // Is the location repairable?
                    boolean repairFlag = true;
                    try {
                        if (repairable != null) {
                            repairFlag = repairable.equals("true");
                        }
                    } catch (Throwable excep) {
                        warning.append("Found invalid isRepairable value: ")
                                .append(destroyed).append(".\n");
                    }

                    // Try to get the critical slot.
                    CriticalSlot slot = entity.getCritical(loc, indexVal);

                    // Did we get it?
                    if (slot == null) {
                        if (!type.equals(EMPTY)) {
                            warning.append("Could not find the ")
                                    .append(type)
                                    .append(" equipment that was expected at index ")
                                    .append(indexVal).append(" of location ")
                                    .append(loc).append(".\n");
                        }
                        return;
                    }

                    // Is the slot for a critical system?
                    if (slot.getType() == CriticalSlot.TYPE_SYSTEM) {

                        // Does the XML file have some other kind of equipment?
                        if (!type.equals(SYSTEM)) {
                            warning.append("XML file expects to find ")
                                    .append(type)
                                    .append(" equipment at index ")
                                    .append(indexVal).append(" of location ")
                                    .append(loc)
                                    .append(", but Entity has a system.\n");
                        }

                    } else {

                        // Nope, we've got equipment. Get this slot's mounted.
                        Mounted mounted = slot.getMount();

                        // Reset transient values.
                        mounted.restore();

                        // quirks
                        if ((null != quirks) && (quirks.trim().length() > 0)) {
                            StringTokenizer st = new StringTokenizer(quirks,
                                    "::");
                            while (st.hasMoreTokens()) {
                                String quirk = st.nextToken();
                                String quirkName = Crew
                                        .parseAdvantageName(quirk);
                                Object value = Crew.parseAdvantageValue(quirk);

                                try {
                                    mounted.getQuirks().getOption(quirkName)
                                            .setValue(value);
                                } catch (Exception e) {
                                    warning.append("Error restoring quirk: ")
                                            .append(quirk).append(".\n");
                                }
                            }
                        }

                        // Hit and destroy the mounted, according to the flags.
                        mounted.setDestroyed(hitFlag || destFlag);

                        mounted.setRepairable(repairFlag);

                        mounted.setRapidfire(Boolean.parseBoolean(rfmg));

                        // Is the mounted a type of ammo?
                        if (mounted.getType() instanceof AmmoType) {

                            // Get the saved ammo load.
                            EquipmentType newLoad = EquipmentType.get(type);
                            if (newLoad instanceof AmmoType) {

                                // Try to get a good shots value.
                                int shotsVal = -1;
                                try {
                                    shotsVal = Integer.parseInt(shots);
                                } catch (NumberFormatException excep) {
                                    // Handled by the next if test.
                                }
                                if (shots.equals(NA)) {
                                    shotsVal = IArmorState.ARMOR_NA;
                                    warning.append(
                                            "Expected to find number of shots for ")
                                            .append(type)
                                            .append(", but found ")
                                            .append(shots)
                                            .append(" instead.\n");
                                } else if ((shotsVal < 0) || (shotsVal > 200)) {
                                    warning.append(
                                            "Found invalid shots value for slot: ")
                                            .append(shots).append(".\n");
                                } else {

                                    // Change to the saved ammo type and shots.
                                    mounted.changeAmmoType((AmmoType) newLoad);
                                    mounted.setShotsLeft(shotsVal);

                                } // End have-good-shots-value

                            } else {
                                // Bad XML equipment.
                                warning.append("XML file expects ")
                                        .append(type)
                                        .append(" equipment at index ")
                                        .append(indexVal)
                                        .append(" of location ")
                                        .append(loc)
                                        .append(", but Entity has ")
                                        .append(mounted.getType()
                                                .getInternalName())
                                        .append("there .\n");
                            }

                        } // End slot-for-ammo

                        // Not an ammo slot... does file agree with template?
                        else if (!mounted.getType().getInternalName()
                                .equals(type)) {
                            // Bad XML equipment.
                            warning.append("XML file expects ")
                                    .append(type)
                                    .append(" equipment at index ")
                                    .append(indexVal)
                                    .append(" of location ")
                                    .append(loc)
                                    .append(", but Entity has ")
                                    .append(mounted.getType().getInternalName())
                                    .append("there .\n");
                        }

                        // Check for munition attribute.
                        if (munition != null) {
                            // Retrieve munition by name.
                            EquipmentType munType = EquipmentType.get(munition);

                            // Make sure munition is a type of ammo.
                            if (munType instanceof AmmoType) {
                                // Change to the saved munition type.
                                mounted.getLinked().changeAmmoType(
                                        (AmmoType) munType);
                            } else {
                                // Bad XML equipment.
                                warning.append("XML file expects ")
                                        .append(" ammo for munition argument of ")
                                        .append(" slot tag.\n");
                            }
                        }

                    } // End have-equipment

                    // Hit and destroy the slot, according to the flags.
                    slot.setHit(hitFlag);
                    slot.setDestroyed(destFlag);
                    slot.setRepairable(repairFlag);

                } // End have-required-fields
            } // End ready-for-slot
        }

    } // End public void recordElementStart( String, Hashtable )

    public void recordElementEnd(String name) throws ParseException {

        // TODO: handle template files.

        // What kind of element have we started?
        if (name.equals(UNIT)) {

            // Are we in the middle of parsing an Entity?
            if (entity != null) {
                warning.append("End of unit while parsing an Entity.\n");

                // Are we in the middle of parsing an Entity's location?
                if (loc != Entity.LOC_NONE) {
                    warning.append("Found end of unit while parsing a location.\n");

                    // If the open location is marked destroyed, destroy it.
                    if (locDestroyed) {
                        destroyLocation(entity, loc);
                    }
                    loc = Entity.LOC_NONE;
                }

                // Add the entity to the vector.
                entities.addElement(entity);
                entity = null;
            }

            // Is this an empty unit?
            else if (entities.isEmpty()) {
                warning.append("Found an empty unit.\n");
            }

        } else if (name.equals(TEMPLATE)) {
            // Do nothing.
        } else if (name.equals(ENTITY)) {

            // We should be in the middle of parsing an Entity.
            if (entity == null) {
                warning.append("Found end of Entity, but not parsing an Entity.\n");
            } else {

                // Are we in the middle of parsing an Entity's location?
                if (loc != Entity.LOC_NONE) {
                    warning.append("Found end of Entity while parsing a location.\n");

                    // If the open location is marked destroyed, destroy it.
                    if (locDestroyed) {
                        destroyLocation(entity, loc);
                    }
                    loc = Entity.LOC_NONE;
                }

                // Add the entity to the vector.
                entities.addElement(entity);
                entity = null;

            } // End save-entity
        } else if (name.equals(FLUFF)) {
            // Do nothing.
        } else if (name.equals(PILOT)) {
            // Do nothing.
        } else if (name.equals(LOCATION)) {

            // We should be in the middle of parsing an Entity.
            if (entity == null) {
                warning.append("Found end of location, but not parsing an Entity.\n");
            }

            // Are we in the middle of parsing an Entity's location?
            else if (loc == Entity.LOC_NONE) {
                warning.append("Found end of location, but not parsing a location.\n");

            } else {

                // If the location is marked destroyed, destroy the location.
                if (locDestroyed) {
                    destroyLocation(entity, loc);
                }

                // Reset the location.
                loc = Entity.LOC_NONE;

            } // End finish-location

        } else if (name.equals(ARMOR)) {
            // Do nothing.
        } else if (name.equals(SLOT)) {
            // Do nothing.
        } else if (name.equals(C3I)) {
            // Are we in the outside of an Entity?
            if (entity == null) {
                warning.append("Found the end of aa C3i set outside of an Entity.\n");
            }
            // Are we in the middle of parsing an Entity's location?
            else if (c3i == Entity.NONE) {
                warning.append("Found the end of a c3i set while not parsing a c3i set.\n");
            } else {
                c3i = Entity.NONE;
            }
        }

    }

    public void recordPI(String name, String pValue) {
        // Do nothing.
    }

    public void recordCharData(String charData) {
        // Do nothing.
    }

    public void recordComment(String comment) {
        // Do nothing.
    }

    public InputStream getDocumentStream() throws ParseException {
        if (inStream == null) {
            throw new ParseException("Input document stream not defined.");
        }
        return inStream;
    }

    public InputStream resolveExternalEntity(String name, String pubID,
            String sysID) throws ParseException {
        // Return nothing.
        return null;
    }

    public InputStream resolveDTDEntity(String name, String pubID, String sysID)
            throws ParseException {
        // Return nothing.
        return null;
    }

} // End public class XMLStreamParser implements XMLResponder

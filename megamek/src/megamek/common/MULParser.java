/*
* Copyright (c) 2014-2022 - The MegaMek Team. All Rights Reserved.
*
* This program is free software; you can redistribute it and/or modify it under
* the terms of the GNU General Public License as published by the Free Software
* Foundation; either version 2 of the License, or (at your option) any later
* version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
* details.
*/
package megamek.common;

import megamek.client.generator.RandomNameGenerator;
import megamek.codeUtilities.StringUtility;
import megamek.common.annotations.Nullable;
import megamek.common.enums.Gender;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.infantry.InfantryWeapon;
import megamek.utilities.xml.MMXMLUtility;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

/**
 * Class for reading in and parsing MUL XML files. The MUL xsl is defined in
 * the docs directory.
 *
 * @author arlith
 */
public class MULParser {
    public static final String VERSION = "version";

    /**
     * The names of the various elements recognized by this parser.
     */
    public static final String ELE_RECORD = "record";
    public static final String ELE_SURVIVORS = "survivors";
    public static final String ELE_ALLIES = "allies";
    public static final String ELE_SALVAGE = "salvage";
    public static final String ELE_RETREATED = "retreated";
    public static final String ELE_DEVASTATED = "devastated";
    public static final String ELE_UNIT = "unit";
    public static final String ELE_ENTITY = "entity";
    public static final String ELE_PILOT = "pilot";
    public static final String ELE_CREW = "crew";
    public static final String ELE_CREWMEMBER = "crewMember";
    public static final String ELE_KILLS = "kills";
    public static final String ELE_KILL = "kill";
    public static final String ELE_LOCATION = "location";
    public static final String ELE_ARMOR = "armor";
    public static final String ELE_SLOT = "slot";
    public static final String ELE_MOTIVE = "motive";
    public static final String ELE_TURRETLOCK = "turretlock";
    private static final String ELE_TURRET2LOCK = "turret2lock";
    public static final String ELE_SI = "structural";
    public static final String ELE_HEAT = "heat";
    public static final String ELE_FUEL = "fuel";
    public static final String ELE_KF = "KF";
    public static final String ELE_SAIL = "sail";
    public static final String ELE_AEROCRIT = "acriticals";
    public static final String ELE_DROPCRIT = "dcriticals";
    public static final String ELE_TANKCRIT = "tcriticals";
    public static final String ELE_STABILIZER = "stabilizer";
    public static final String ELE_BREACH = "breached";
    public static final String ELE_BLOWN_OFF = "blownOff";
    public static final String ELE_C3I = "c3iset";
    public static final String ELE_C3ILINK = "c3i_link";
    public static final String ELE_NC3 = "NC3set";
    public static final String ELE_NC3LINK = "NC3_link";
    public static final String ELE_ESCCRAFT = "EscapeCraft";
    public static final String ELE_ESCCREW = "EscapedCrew";
    public static final String ELE_ESCPASS = "EscapedPassengers";
    public static final String ELE_ORIG_PODS = "ONumberOfPods";
    public static final String ELE_ORIG_MEN = "ONumberOfMen";
    public static final String ELE_CONVEYANCE = "Conveyance";
    public static final String ELE_GAME = "Game";
    public static final String ELE_FORCE = "Force";
    public static final String ELE_BAY = "transportBay";
    public static final String ELE_BAYDOORS = "doors";
    public static final String ELE_BAYDAMAGE = "damage";
    public static final String ELE_BOMBS = "bombs";
    public static final String ELE_BOMB = "bomb";
    public static final String ELE_BA_MEA = "modularEquipmentMount";
    public static final String ELE_BA_APM = "antiPersonnelMount";
    public static final String ELE_LOADED = "loaded";
    public static final String ELE_SHIP = "ship";

    /**
     * The names of attributes generally associated with Entity tags
     */
    public static final String ATTR_CHASSIS = "chassis";
    public static final String ATTR_MODEL = "model";
    public static final String ATTR_CAMO_CATEGORY = "camoCategory";
    public static final String ATTR_CAMO_FILENAME = "camoFileName";
    public static final String ATTR_CAMO_ROTATION = "camoRotation";
    public static final String ATTR_CAMO_SCALE = "camoScale";

    /**
     * The names of the attributes recognized by this parser. Not every
     * attribute is valid for every element.
     */

    public static final String ATTR_NAME = "name";
    public static final String ATTR_SIZE = "size";
    private static final String ATTR_CURRENTSIZE = "currentsize";
    public static final String ATTR_EXT_ID = "externalId";
    public static final String ATTR_PICKUP_ID = "pickUpId";
    public static final String ATTR_NICK = "nick";
    public static final String ATTR_GENDER = "gender";
    public static final String ATTR_CAT_PORTRAIT = "portraitCat";
    public static final String ATTR_FILE_PORTRAIT = "portraitFile";
    public static final String ATTR_GUNNERY = "gunnery";
    public static final String ATTR_GUNNERYL = "gunneryL";
    public static final String ATTR_GUNNERYM = "gunneryM";
    public static final String ATTR_GUNNERYB = "gunneryB";
    public static final String ATTR_PILOTING = "piloting";
    public static final String ATTR_ARTILLERY = "artillery";
    public static final String ATTR_TOUGH = "toughness";
    public static final String ATTR_INITB = "initB";
    public static final String ATTR_COMMANDB = "commandB";
    public static final String ATTR_HITS = "hits";
    public static final String ATTR_ADVS = "advantages";
    public static final String ATTR_EDGE = "edge";
    public static final String ATTR_IMPLANTS = "implants";
    public static final String ATTR_QUIRKS = "quirks";
    public static final String ATTR_TROOPER_MISS = "trooperMiss";
    public static final String ATTR_DRIVER = "driver";
    public static final String ATTR_COMMANDER = "commander";
    public static final String ATTR_OFFBOARD = "offboard";
    public static final String ATTR_OFFBOARD_DISTANCE = "offboard_distance";
    public static final String ATTR_OFFBOARD_DIRECTION = "offboard_direction";
    public static final String ATTR_HIDDEN = "hidden";
    public static final String ATTR_DEPLOYMENT = "deployment";
    public static final String ATTR_DEPLOYMENT_ZONE = "deploymentZone";
    public static final String ATTR_DEPLOYMENT_ZONE_WIDTH = "deploymentZoneWidth";
    public static final String ATTR_DEPLOYMENT_ZONE_OFFSET = "deploymentZoneOffset";
    public static final String ATTR_DEPLOYMENT_ZONE_ANY_NWX = "deploymentZoneAnyNWx";
    public static final String ATTR_DEPLOYMENT_ZONE_ANY_NWY = "deploymentZoneAnyNWy";
    public static final String ATTR_DEPLOYMENT_ZONE_ANY_SEX = "deploymentZoneAnySEx";
    public static final String ATTR_DEPLOYMENT_ZONE_ANY_SEY = "deploymentZoneAnySEy";
    public static final String ATTR_NEVER_DEPLOYED = "neverDeployed";
    public static final String ATTR_VELOCITY = "velocity";
    public static final String ATTR_ALTITUDE = "altitude";
    public static final String ATTR_ELEVATION = "elevation";
    public static final String ATTR_AUTOEJECT = "autoeject";
    public static final String ATTR_CONDEJECTAMMO = "condejectammo";
    public static final String ATTR_CONDEJECTENGINE = "condejectengine";
    public static final String ATTR_CONDEJECTCTDEST = "condejectctdest";
    public static final String ATTR_CONDEJECTHEADSHOT = "condejectheadshot";
    public static final String ATTR_EJECTED = "ejected";
    public static final String ATTR_INDEX = "index";
    public static final String ATTR_IS_DESTROYED = "isDestroyed";
    public static final String ATTR_IS_REPAIRABLE = "isRepairable";
    public static final String ATTR_POINTS = "points";
    public static final String ATTR_TYPE = "type";
    public static final String ATTR_SHOTS = "shots";
    public static final String ATTR_CAPACITY = "capacity";
    public static final String ATTR_IS_HIT = "isHit";
    public static final String ATTR_MUNITION = "munition";
    public static final String ATTR_STANDARD = "standard";
    public static final String ATTR_INFERNO = "inferno";
    public static final String ATTR_DIRECTION = "direction";
    public static final String ATTR_INTEGRITY = "integrity";
    public static final String ATTR_SINK = "sinks";
    public static final String ATTR_LEFT = "left";
    public static final String ATTR_AVIONICS = "avionics";
    public static final String ATTR_SENSORS = "sensors";
    public static final String ATTR_ENGINE = "engine";
    public static final String ATTR_FCS = "fcs";
    public static final String ATTR_CIC = "cic";
    public static final String ATTR_LEFT_THRUST = "leftThrust";
    public static final String ATTR_RIGHT_THRUST = "rightThrust";
    public static final String ATTR_LIFE_SUPPORT = "lifeSupport";
    public static final String ATTR_GEAR = "gear";
    public static final String ATTR_DOCKING_COLLAR = "dockingcollar";
    public static final String ATTR_KFBOOM = "kfboom";
    public static final String ATTR_WEAPONS_BAY_INDEX = "weaponsBayIndex";
    public static final String ATTR_MDAMAGE = "damage";
    public static final String ATTR_MPENALTY = "penalty";
    public static final String ATTR_C3MASTERIS = "c3MasterIs";
    public static final String ATTR_C3UUID = "c3UUID";
    public static final String ATTR_LOAD = "load";
    public static final String ATTR_INTERNAL = "Internal";
    public static final String ATTR_BA_APM_MOUNT_NUM = "baAPMMountNum";
    public static final String ATTR_BA_APM_TYPE_NAME = "baAPMTypeName";
    public static final String ATTR_BA_MEA_MOUNT_LOC = "baMEAMountLoc";
    public static final String ATTR_BA_MEA_TYPE_NAME = "baMEATypeName";
    public static final String ATTR_KILLED = "killed";
    public static final String ATTR_KILLER = "killer";
    private static final String EXTRA_DATA = "extraData";
    public static final String ATTR_ARMOR_DIVISOR = "armorDivisor";
    public static final String ATTR_ARMOR_ENC = "armorEncumbering";
    public static final String ATTR_DEST_ARMOR = "destArmor";
    public static final String ATTR_SPACESUIT = "spacesuit";
    public static final String ATTR_SNEAK_CAMO = "sneakCamo";
    public static final String ATTR_SNEAK_IR = "sneakIR";
    public static final String ATTR_SNEAK_ECM = "sneakECM";
    public static final String ATTR_INF_SPEC = "infantrySpecializations";
    public static final String ATTR_INF_SQUAD_NUM = "squadNum";
    public static final String ATTR_RFMG = "rfmg";
    public static final String ATTR_LINK = "link";
    public static final String ATTR_ID = "id";
    public static final String ATTR_NUMBER = "number";
    public static final String ATTR_FORCE = "force";
    public static final String ATTR_SLOT = "slot";
    public static final String ATTR_IS_REAR = "isRear";
    public static final String ATTR_IS_TURRETED = "isTurreted";
    public static final String ATTR_IS_MISSING = "isMissing";
    public static final String ATTR_GUNNERYAERO = "gunneryAero";
    public static final String ATTR_GUNNERYAEROL = "gunneryAeroL";
    public static final String ATTR_GUNNERYAEROM = "gunneryAeroM";
    public static final String ATTR_GUNNERYAEROB = "gunneryAeroB";
    public static final String ATTR_PILOTINGAERO = "pilotingAero";
    public static final String ATTR_CREWTYPE = "crewType";



    /**
     * Special values recognized by this parser.
     */
    public static final String VALUE_DEAD = "Dead";
    public static final String VALUE_NA = "N/A";
    public static final String VALUE_DESTROYED = "Destroyed";
    private static final String VALUE_FRONT = "Front";
    public static final String VALUE_REAR = "Rear";
    public static final String VALUE_INTERNAL = "Internal";
    public static final String VALUE_EMPTY = "Empty";
    public static final String VALUE_SYSTEM = "System";
    public static final String VALUE_NONE = "None";
    public static final String VALUE_HIT =  "hit";
    public static final String VALUE_CONSOLE =  "console";



    /**
     * Stores all of the  Entity's read in. This is for general use saving and loading to the chat lounge
     */
    Vector<Entity> entities;

    /**
     * Stores all of the  surviving Entity's read in.
     */
    Vector<Entity> survivors;

    /**
     * Stores all of the allied Entity's read in.
     */
    Vector<Entity> allies;

    /**
     * Stores all of the enemy retreated entities read in.
     */
    Vector<Entity> retreated;

    /**
     * Stores all the salvage entities read in
     */
    Vector<Entity> salvage;

    /**
     * Stores all the devastated entities read in
     */
    Vector<Entity> devastated;

    /**
     * Keep a separate list of pilot/crews parsed because dismounted pilots may
     * need to be read separately
     */
    private Vector<Crew> pilots;

    /**
     * A hashtable containing the names of killed units as the key and the external id
     * of the killer as the value
     */
    private Hashtable<String, String> kills;

    StringBuffer warning;

    //region Constructors
    /**
     * This initializes all the variables utilised by the MUL parser.
     */
    private MULParser() {
        warning = new StringBuffer();
        entities = new Vector<>();
        survivors = new Vector<>();
        allies = new Vector<>();
        salvage = new Vector<>();
        retreated = new Vector<>();
        devastated = new Vector<>();
        kills = new Hashtable<>();
        pilots = new Vector<>();
    }

    /**
     * This is the standard MULParser constructor for a file. It initializes the values to parse
     * with, then parses the file using the provided options. The options may be null in cases
     * when the crew is not to be loaded as part of the MUL.
     *
     * @param file the file to parse, or null if there isn't anything to parse
     * @param options the game options to parse the MUL with, which may be null (only to be used
     *                when the crew is not to be loaded, as no saved optional Crew-based values are
     *                loaded).
     * @throws Exception if there is an issue with parsing the file
     */
    public MULParser(final @Nullable File file, final @Nullable GameOptions options) throws Exception {
        this();

        if (file == null) {
            return;
        }

        try (InputStream is = new FileInputStream(file)) {
            parse(is, options);
        }
    }

    /**
     * This is provided for unit testing only, and should not be part of general parsing.
     *
     * @param is the input stream to parse from
     * @param options the game options to parse the MUL with, which may be null (only to be used
     *                when the crew is not to be loaded, as no saved optional Crew-based values are
     *                loaded).
     */
    public MULParser(final InputStream is, final @Nullable GameOptions options) throws Exception {
        this();
        parse(is, options);
    }

    /**
     * This is the standard MULParser constructor for a single element. It initializes the values to
     * parse with, then parses the element using the provided options. The options may be null in
     * cases when the crew is not to be loaded as part of the MUL.
     *
     * @param element the element to parse
     * @param options the game options to parse the MUL with, which may be null (only to be used
     *                when the crew is not to be loaded, as no saved optional Crew-based values are
     *                loaded).
     */
    public MULParser(final Element element, final @Nullable GameOptions options) {
        this();
        parse(element, options);
    }
    //endregion Constructors

    private void parse(final InputStream fin, final @Nullable GameOptions options) throws Exception {
        Document xmlDoc;

        try {
            final DocumentBuilder db = MMXMLUtility.newSafeDocumentBuilder();
            xmlDoc = db.parse(fin);
        } catch (Exception e) {
            warning.append("Error parsing MUL file!\n");
            throw e;
        }

        final Element element = xmlDoc.getDocumentElement();
        element.normalize();

        final String version = element.getAttribute(VERSION);
        if (version.isBlank()) {
            warning.append("Warning: No version specified, correct parsing ")
                    .append("not guaranteed!\n");
        }
        parse(element, options);
    }

    private void parse(final Element element, final @Nullable GameOptions options) {
        // Then parse the element
        if (element.getNodeName().equalsIgnoreCase(ELE_RECORD)) {
            parseRecord(element, options);
        } else if (element.getNodeName().equalsIgnoreCase(ELE_UNIT)) {
            parseUnit(element, options, entities);
        } else if (element.getNodeName().equalsIgnoreCase(ELE_ENTITY)) {
            parseEntity(element, options, entities);
        } else {
            warning.append("Error: root element isn't a Record, Unit, or Entity tag! Nothing to parse!\n");
        }

        // Finally, output the warning if there is any
        if (hasWarningMessage()) {
            LogManager.getLogger().warn(getWarningMessage());
        }
    }

    /**
     * Parse a Unit tag. Unit tags will contain a list of Entity tags.
     * @param unitNode the node containing the unit tag
     */
    private void parseRecord(final Element unitNode, final @Nullable GameOptions options) {
        NodeList nl = unitNode.getChildNodes();

        // Iterate through the children, looking for Entity tags
        for (int i = 0; i < nl.getLength(); i++) {
            Node currNode = nl.item(i);

            if (currNode.getParentNode() != unitNode) {
                continue;
            }
            int nodeType = currNode.getNodeType();
            if (nodeType == Node.ELEMENT_NODE) {
                String nodeName = currNode.getNodeName();
                if (nodeName.equalsIgnoreCase(ELE_UNIT)) {
                    parseUnit((Element) currNode, options, entities);
                } else if (nodeName.equalsIgnoreCase(ELE_SURVIVORS)) {
                    parseUnit((Element) currNode, options, survivors);
                } else if (nodeName.equalsIgnoreCase(ELE_ALLIES)) {
                    parseUnit((Element) currNode, options, allies);
                } else if (nodeName.equalsIgnoreCase(ELE_SALVAGE)) {
                    parseUnit((Element) currNode, options, salvage);
                } else if (nodeName.equalsIgnoreCase(ELE_RETREATED)) {
                    parseUnit((Element) currNode, options, retreated);
                } else if (nodeName.equalsIgnoreCase(ELE_DEVASTATED)) {
                    parseUnit((Element) currNode, options, devastated);
                } else if (nodeName.equalsIgnoreCase(ELE_KILLS)) {
                    parseKills((Element) currNode);
                } else if (nodeName.equalsIgnoreCase(ELE_ENTITY)) {
                    parseUnit((Element) currNode, options, entities);
                } else if (nodeName.equalsIgnoreCase(ELE_PILOT)) {
                    parsePilot((Element) currNode, options);
                } else if (nodeName.equalsIgnoreCase(ELE_CREW)) {
                    parseCrew((Element) currNode, options);
                }
            }
        }
    }

    /**
     * Parse a Unit tag. Unit tags will contain a list of Entity tags.
     * @param unitNode the node containing the unit tag
     * @param options the game options to parse using
     * @param list the list to add found entities to
     */
    private void parseUnit(final Element unitNode, final @Nullable GameOptions options,
                           final Vector<Entity> list) {
        NodeList nl = unitNode.getChildNodes();

        // Iterate through the children, looking for Entity tags
        for (int i = 0; i < nl.getLength(); i++) {
            Node currNode = nl.item(i);

            if (currNode.getParentNode() != unitNode) {
                continue;
            }
            int nodeType = currNode.getNodeType();
            if (nodeType == Node.ELEMENT_NODE) {
                String nodeName = currNode.getNodeName();
                if (nodeName.equalsIgnoreCase(ELE_ENTITY)) {
                    parseEntity((Element) currNode, options, list);
                } else if (nodeName.equalsIgnoreCase(ELE_PILOT)) {
                    parsePilot((Element) currNode, options);
                } else if (nodeName.equalsIgnoreCase(ELE_CREW)) {
                    parseCrew((Element) currNode, options);
                }
            }
        }
    }

    /**
     * Parse a kills tag.
     * @param killNode
     */
    private void parseKills(Element killNode) {
        NodeList nl = killNode.getChildNodes();

        // Iterate through the children, looking for Entity tags
        for (int i = 0; i < nl.getLength(); i++) {
            Node currNode = nl.item(i);

            if (currNode.getParentNode() != killNode) {
                continue;
            }
            int nodeType = currNode.getNodeType();
            if (nodeType == Node.ELEMENT_NODE) {
                String nodeName = currNode.getNodeName();
                if (nodeName.equalsIgnoreCase(ELE_KILL)) {
                    String killed = ((Element) currNode).getAttribute(ATTR_KILLED);
                    String killer = ((Element) currNode).getAttribute(ATTR_KILLER);
                    if (!killed.isBlank() && !killer.isBlank()) {
                        kills.put(killed, killer);
                    }
                }
            }
        }
    }

    /**
     * Parse an Entity tag. Entity tags will have a number of attributes such as model, chassis,
     * type, etc. They should also have a child Pilot tag, and they may also contain some number of
     * location tags.
     *
     * @param entityNode the node to parse the entity tag from
     * @param options the game options to parse using
     * @param list the list to add found entities to
     */
    private void parseEntity(final Element entityNode, final @Nullable GameOptions options,
                             final Vector<Entity> list) {
        // We need to get a new Entity, use the chassis and model to create one
        String chassis = entityNode.getAttribute(ATTR_CHASSIS);
        String model = entityNode.getAttribute(ATTR_MODEL);

        // Create a new entity
        Entity entity = getEntity(chassis, model);

        // Make sure we've got an Entity
        if (entity == null) {
            warning.append("Failed to load entity!");
            return;
        }

        // Set the attributes for the entity
        parseEntityAttributes(entity, entityNode);

        // Deal with any child nodes
        NodeList nl = entityNode.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node currNode = nl.item(i);
            if (currNode.getParentNode() != entityNode) {
                continue;
            }
            int nodeType = currNode.getNodeType();
            if (nodeType == Node.ELEMENT_NODE) {
                Element currEle = (Element) currNode;
                String nodeName = currNode.getNodeName();
                if (nodeName.equalsIgnoreCase(ELE_PILOT)) {
                    parsePilot(currEle, options, entity);
                } else if (nodeName.equalsIgnoreCase(ELE_CREW)) {
                    parseCrew(currEle, options, entity);
                } else if (nodeName.equalsIgnoreCase(ELE_LOCATION)) {
                    parseLocation(currEle, entity);
                } else if (nodeName.equalsIgnoreCase(ELE_MOTIVE)) {
                    parseMotive(currEle, entity);
                } else if (nodeName.equalsIgnoreCase(ELE_TURRETLOCK)) {
                    parseTurretLock(currEle, entity);
                } else if (nodeName.equalsIgnoreCase(ELE_TURRET2LOCK)) {
                    parseTurret2Lock(currEle, entity);
                } else if (nodeName.equalsIgnoreCase(ELE_SI)) {
                    parseSI(currEle, entity);
                } else if (nodeName.equalsIgnoreCase(ELE_HEAT)) {
                    parseHeat(currEle, entity);
                } else if (nodeName.equalsIgnoreCase(ELE_FUEL)) {
                    parseFuel(currEle, entity);
                } else if (nodeName.equalsIgnoreCase(ELE_KF)) {
                    parseKF(currEle, entity);
                } else if (nodeName.equalsIgnoreCase(ELE_SAIL)) {
                    parseSail(currEle, entity);
                } else if (nodeName.equalsIgnoreCase(ELE_BAY)) {
                    parseTransportBay(currEle, entity);
                } else if (nodeName.equalsIgnoreCase(ELE_AEROCRIT)) {
                    parseAeroCrit(currEle, entity);
                } else if (nodeName.equalsIgnoreCase(ELE_DROPCRIT)) {
                    parseDropCrit(currEle, entity);
                } else if (nodeName.equalsIgnoreCase(ELE_TANKCRIT)) {
                    parseTankCrit(currEle, entity);
                } else if (nodeName.equalsIgnoreCase(ELE_BOMBS)) {
                    parseBombs(currEle, entity);
                } else if (nodeName.equalsIgnoreCase(ELE_C3I)) {
                    parseC3I(currEle, entity);
                } else if (nodeName.equalsIgnoreCase(ELE_NC3)) {
                    parseNC3(currEle, entity);
                } else if (nodeName.equalsIgnoreCase(ELE_BA_MEA)) {
                    parseBAMEA(currEle, entity);
                } else if (nodeName.equalsIgnoreCase(ELE_BA_APM)) {
                    parseBAAPM(currEle, entity);
                } else if (nodeName.equalsIgnoreCase(ELE_ESCCRAFT)) {
                    parseEscapeCraft(currEle, entity);
                } else if (nodeName.equalsIgnoreCase(ELE_ESCPASS)) {
                    parseEscapedPassengers(currEle, entity);
                } else if (nodeName.equalsIgnoreCase(ELE_ESCCREW)) {
                    parseEscapedCrew(currEle, entity);
                } else if (nodeName.equalsIgnoreCase(ELE_ORIG_PODS)) {
                    parseOSI(currEle, entity);
                } else if (nodeName.equalsIgnoreCase(ELE_ORIG_MEN)) {
                    parseOMen(currEle, entity);
                } else if (nodeName.equalsIgnoreCase(ELE_CONVEYANCE)) {
                    parseConveyance(currEle, entity);
                } else if (nodeName.equalsIgnoreCase(ELE_GAME)) {
                    parseId(currEle, entity);
                } else if (nodeName.equalsIgnoreCase(ELE_FORCE)) {
                    parseForce(currEle, entity);
                }
            }
        }

        //Now we should be done setting up the Entity, add it to the list
        list.add(entity);
    }

    /**
     * Create a new <code>Entity</code> instance given a mode and chassis name.
     *
     * @param chassis
     * @param model
     * @return
     */
    private Entity getEntity(String chassis, @Nullable String model) {
        Entity newEntity = null;

        // First check for ejected MechWarriors, vee crews, escape pods and spacecraft crews
        if (chassis.equals(EjectedCrew.VEE_EJECT_NAME)
                || chassis.equals(EjectedCrew.SPACE_EJECT_NAME)) {
            return new EjectedCrew();
        } else if (chassis.equals(EjectedCrew.PILOT_EJECT_NAME)
                    || chassis.equals(EjectedCrew.MW_EJECT_NAME)) {
            return new MechWarrior();
        } else if (chassis.equals(EscapePods.POD_EJECT_NAME)) {
            return new EscapePods();
        }

        // Did we find required attributes?
        if (chassis.isBlank()) {
            warning.append("Could not find chassis for Entity.\n");
        } else {
            // Try to find the entity.
            StringBuffer key = new StringBuffer(chassis);
            MechSummary ms = MechSummaryCache.getInstance().getMech(key.toString());
            if (!StringUtility.isNullOrBlank(model)) {
                key.append(" ").append(model);
                ms = MechSummaryCache.getInstance().getMech(key.toString());
                // That didn't work. Try swapping model and chassis.
                if (ms == null) {
                    key = new StringBuffer(model);
                    key.append(" ").append(chassis);
                    ms = MechSummaryCache.getInstance().getMech(key.toString());
                }
            }
            // We should have found the mech.
            if (ms == null) {
                warning.append("Could not find Entity with chassis: ").append(chassis);
                if (!StringUtility.isNullOrBlank(model)) {
                    warning.append(", and model: ").append(model);
                }
                warning.append(".\n");
            } else {
                // Try to load the new mech.
                try {
                    newEntity = new MechFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
                } catch (Exception ex) {
                    LogManager.getLogger().error("", ex);
                    warning.append("Unable to load mech: ")
                            .append(ms.getSourceFile()).append(": ")
                            .append(ms.getEntryName()).append(": ")
                            .append(ex.getMessage());
                }
            }
        }
        return newEntity;
    }

    /**
     * An Entity tag can define numerous attributes for the <code>Entity</code>,
     * check and set all of the relevant attributes.
     *
     * @param entity    The newly created Entity that we are setting state for
     * @param entityTag The Entity tag that defines the attributes
     */
    private void parseEntityAttributes(Entity entity, Element entityTag) {
        // commander
        boolean commander =
                Boolean.parseBoolean(entityTag.getAttribute(ATTR_COMMANDER));
        entity.setCommander(commander);

        // hidden
        try {
            boolean isHidden =
                    Boolean.parseBoolean(entityTag.getAttribute(ATTR_HIDDEN));
            entity.setHidden(isHidden);
        } catch (Exception e) {
            entity.setHidden(false);
        }

        // deploy offboard
        try {
            boolean offBoard =
                    Boolean.parseBoolean(entityTag.getAttribute(ATTR_OFFBOARD));
            if (offBoard) {
                int distance = Integer.parseInt(entityTag
                        .getAttribute(ATTR_OFFBOARD_DISTANCE));
                OffBoardDirection dir = OffBoardDirection.getDirection(Integer
                        .parseInt(entityTag.getAttribute(ATTR_OFFBOARD_DIRECTION)));
                entity.setOffBoard(distance, dir);
            }
        } catch (Exception ignored) {
        }

        // deployment round
        try {
            int deployRound = Integer.parseInt(entityTag.getAttribute(ATTR_DEPLOYMENT));
            entity.setDeployRound(deployRound);
        } catch (Exception e) {
            entity.setDeployRound(0);
        }

        // deployment zone
        try {
            int deployZone = Integer.parseInt(entityTag.getAttribute(ATTR_DEPLOYMENT_ZONE));
            entity.setStartingPos(deployZone);
        } catch (Exception e) {
            entity.setStartingPos(Board.START_NONE);
        }

        // deployment zone width
        try {
            int deployZoneWidth = Integer.parseInt(entityTag.getAttribute(ATTR_DEPLOYMENT_ZONE_WIDTH));
            entity.setStartingWidth(deployZoneWidth);
        } catch (Exception e) {
            entity.setStartingWidth(3);
        }

        // deployment zone offset
        try {
            int deployZoneOffset = Integer.parseInt(entityTag.getAttribute(ATTR_DEPLOYMENT_ZONE_OFFSET));
            entity.setStartingOffset(deployZoneOffset);
        } catch (Exception e) {
            entity.setStartingOffset(0);
        }

        // deployment zone Any
        try {
            int deployZoneAnyNWx = Integer.parseInt(entityTag.getAttribute(ATTR_DEPLOYMENT_ZONE_ANY_NWX));
            entity.setStartingAnyNWx(deployZoneAnyNWx);
        } catch (Exception e) {
            entity.setStartingAnyNWx(Entity.STARTING_ANY_NONE);
        }

        try {
            int deployZoneAnyNWy = Integer.parseInt(entityTag.getAttribute(ATTR_DEPLOYMENT_ZONE_ANY_NWY));
            entity.setStartingAnyNWy(deployZoneAnyNWy);
        } catch (Exception e) {
            entity.setStartingAnyNWy(Entity.STARTING_ANY_NONE);
        }

        try {
            int deployZoneAnySEx = Integer.parseInt(entityTag.getAttribute(ATTR_DEPLOYMENT_ZONE_ANY_SEX));
            entity.setStartingAnySEx(deployZoneAnySEx);
        } catch (Exception e) {
            entity.setStartingAnySEx(Entity.STARTING_ANY_NONE);
        }

        try {
            int deployZoneAnySEy = Integer.parseInt(entityTag.getAttribute(ATTR_DEPLOYMENT_ZONE_ANY_SEY));
            entity.setStartingAnySEy(deployZoneAnySEy);
        } catch (Exception e) {
            entity.setStartingAnySEy(Entity.STARTING_ANY_NONE);
        }

        // Was never deployed
        try {
            String ndeploy = entityTag.getAttribute(ATTR_NEVER_DEPLOYED);
            boolean wasNeverDeployed = Boolean.parseBoolean(entityTag.getAttribute(ATTR_NEVER_DEPLOYED));
            if (ndeploy.isBlank()) {
                // this will default to false above, but we want it to default to true
                wasNeverDeployed = true;
            }
            entity.setNeverDeployed(wasNeverDeployed);
        } catch (Exception ignored) {
            entity.setNeverDeployed(true);
        }

        if (entity.isAero()) {
            String velString = entityTag.getAttribute(ATTR_VELOCITY);
            String altString = entityTag.getAttribute(ATTR_ALTITUDE);

            IAero a = (IAero) entity;
            if (!velString.isBlank()) {
                int velocity = 0;

                try {
                    velocity = Integer.parseInt(velString);
                } catch (NumberFormatException ex) {
                }

                a.setCurrentVelocity(velocity);
                a.setNextVelocity(velocity);
            }

            if (!altString.isBlank()) {
                int altitude = 0;

                try {
                    altitude = Integer.parseInt(altString);
                } catch (NumberFormatException ex) {
                }

                if (altitude <= 0) {
                    a.land();
                } else {
                    a.liftOff(altitude);
                }
            }
        }

        if (entity instanceof VTOL) {
            String elevString = entityTag.getAttribute(ATTR_ELEVATION);
            VTOL v = (VTOL) entity;

            if (!elevString.isBlank()) {
                int elevation = 0;

                try {
                    elevation = Integer.parseInt(elevString);
                } catch (NumberFormatException ex) {
                }

                v.setElevation(elevation);
            }
        }

        // Camo
        entity.getCamouflage().setCategory(entityTag.getAttribute(ATTR_CAMO_CATEGORY));
        entity.getCamouflage().setFilename(entityTag.getAttribute(ATTR_CAMO_FILENAME));

        try {
            String rotationString = entityTag.getAttribute(ATTR_CAMO_ROTATION);
            entity.getCamouflage().setRotationAngle(Integer.parseInt(rotationString));
            String scaleString = entityTag.getAttribute(ATTR_CAMO_SCALE);
            entity.getCamouflage().setScale(Integer.parseInt(scaleString));
        } catch (NumberFormatException ex) {
            entity.getCamouflage().setRotationAngle(0);
            entity.getCamouflage().resetScale();
        }

        // external id
        String extId = entityTag.getAttribute(ATTR_EXT_ID);
        if (extId.isBlank()) {
            extId = "-1";
        }
        entity.setExternalIdAsString(extId);

        // external id
        if (entity instanceof MechWarrior) {
            String pickUpId = entityTag.getAttribute(ATTR_PICKUP_ID);
            if (pickUpId.isBlank()) {
                pickUpId = "-1";
            }
            ((MechWarrior) entity).setPickedUpByExternalId(pickUpId);
        }

        // quirks
        String quirks = entityTag.getAttribute(ATTR_QUIRKS);
        if (!quirks.isBlank()) {
            StringTokenizer st = new StringTokenizer(quirks, "::");
            while (st.hasMoreTokens()) {
                String quirk = st.nextToken();
                String quirkName = Crew.parseAdvantageName(quirk);
                Object value = Crew.parseAdvantageValue(quirk);

                try {
                    entity.getQuirks().getOption(quirkName).setValue(value);
                } catch (Exception ignored) {
                    warning.append("Error restoring quirk: ").append(quirk).append(".\n");
                }
            }
        }

        // Setup for C3 Relinking
        String c3masteris = entityTag.getAttribute(ATTR_C3MASTERIS);
        if (!c3masteris.isBlank()) {
            entity.setC3MasterIsUUIDAsString(c3masteris);
        }
        String c3uuid = entityTag.getAttribute(ATTR_C3UUID);
        if (!c3uuid.isBlank()) {
            entity.setC3UUIDAsString(c3uuid);
        }

        // Load some values for conventional infantry
        if (entity.isConventionalInfantry()) {
            Infantry inf = (Infantry) entity;
            String armorDiv = entityTag.getAttribute(ATTR_ARMOR_DIVISOR);
            if (!armorDiv.isBlank()) {
                inf.setArmorDamageDivisor(Double.parseDouble(armorDiv));
            }

            if (!entityTag.getAttribute(ATTR_ARMOR_ENC).isBlank()) {
                inf.setArmorEncumbering(true);
            }

            if (!entityTag.getAttribute(ATTR_SPACESUIT).isBlank()) {
                inf.setSpaceSuit(true);
            }

            if (!entityTag.getAttribute(ATTR_DEST_ARMOR).isBlank()) {
                inf.setDEST(true);
            }

            if (!entityTag.getAttribute(ATTR_SNEAK_CAMO).isBlank()) {
                inf.setSneakCamo(true);
            }

            if (!entityTag.getAttribute(ATTR_SNEAK_IR).isBlank()) {
                inf.setSneakIR(true);
            }

            if (!entityTag.getAttribute(ATTR_SNEAK_ECM).isBlank()) {
                inf.setSneakECM(true);
            }

            String infSpec = entityTag.getAttribute(ATTR_INF_SPEC);
            if (!infSpec.isBlank()) {
                inf.setSpecializations(Integer.parseInt(infSpec));
            }

            String infSquadNum = entityTag.getAttribute(ATTR_INF_SQUAD_NUM);
            if (!infSquadNum.isBlank()) {
                inf.setSquadCount(Integer.parseInt(infSquadNum));
                inf.autoSetInternal();
            }
        }
    }

    /**
     * Convenience function that calls <code>parsePilot</code> with a null Entity.
     *
     * @param node The Pilot tag to create a <code>Crew</code> from
     * @param options The options to parse the crew based on
     */
    private void parsePilot(final Element node, final @Nullable GameOptions options) {
        parsePilot(node, options, null);
    }

    /**
     * Given a pilot tag, read the attributes and create a new <code>Crew</code> instance. If a
     * non-null <code>Entity</code> is passed, the new crew will be set as the crew for the given
     * <code>Entity</code>.
     *
     * @param pilotNode The Pilot tag to create a <code>Crew</code> from
     * @param options   The options to parse the crew based on
     * @param entity    If non-null, the new <code>Crew</code> will be set as
     *                  the crew of this <code>Entity</code>
     */
    private void parsePilot(final Element pilotNode, final @Nullable GameOptions options,
                            final Entity entity) {
        Map<String,String> attributes = new HashMap<>();
        for (int i = 0; i < pilotNode.getAttributes().getLength(); i++) {
            final Node node = pilotNode.getAttributes().item(i);
            attributes.put(node.getNodeName(), node.getTextContent());
        }

        Crew crew;
        if (null != entity) {
            crew = new Crew(entity.getCrew().getCrewType());
        } else {
            crew = new Crew(CrewType.SINGLE);
        }
        setCrewAttributes(options, entity, crew, attributes);
        setPilotAttributes(options, crew, 0, attributes);
        // LAMs have a second set of gunnery and piloting stats, so we create a dummy crew
        // and parse a copy of the attributes with the aero stats altered to their non-aero keys,
        // then copy the results into the aero skills of the LAMPilot.
        if (entity instanceof LandAirMech) {
            crew = LAMPilot.convertToLAMPilot((LandAirMech) entity, crew);
            Crew aeroCrew = new Crew(CrewType.SINGLE);
            Map<String,String> aeroAttributes = new HashMap<>(attributes);
            for (String key : attributes.keySet()) {
                if (key.contains("Aero")) {
                    aeroAttributes.put(key.replace("Aero", ""), attributes.get(key));
                }
            }
            setPilotAttributes(options, aeroCrew, 0, aeroAttributes);
            ((LAMPilot) crew).setGunneryAero(aeroCrew.getGunnery());
            ((LAMPilot) crew).setGunneryAeroM(aeroCrew.getGunneryM());
            ((LAMPilot) crew).setGunneryAeroB(aeroCrew.getGunneryB());
            ((LAMPilot) crew).setGunneryAeroL(aeroCrew.getGunneryL());
            ((LAMPilot) crew).setPilotingAero(aeroCrew.getPiloting());
            entity.setCrew(crew);
        }
        pilots.add(crew);
    }

    /**
     * Convenience function that calls <code>parseCrew</code> with a null Entity.
     *
     * @param node The crew tag to create a <code>Crew</code> from
     * @param options The game options to create the crew with
     */
    private void parseCrew(final Element node, final @Nullable GameOptions options) {
        parseCrew(node, options, null);
    }

    /**
     * Used for multi-crew cockpits.
     * Given a tag, read the attributes and create a new <code>Crew</code> instance. If a non-null
     * <code>Entity</code> is passed, the new crew will be set as the crew for the given
     * <code>Entity</code>.
     *
     * @param options  The <code>GameOptions</code> set when loading this crew
     * @param crewNode The crew tag to create a <code>Crew</code> from
     * @param entity   If non-null, the new <code>Crew</code> will be set as the crew of this Entity
     */
    private void parseCrew(final Element crewNode, final @Nullable GameOptions options,
                           final @Nullable Entity entity) {
        final Map<String, String> crewAttr = new HashMap<>();
        for (int i = 0; i < crewNode.getAttributes().getLength(); i++) {
            final Node node = crewNode.getAttributes().item(i);
            crewAttr.put(node.getNodeName(), node.getTextContent());
        }
        //Do not assign crew attributes until after individual crew members have been processed because
        //we cannot assign hits to ejected crew.

        Crew crew;
        CrewType crewType = null;
        if (crewAttr.containsKey(ATTR_CREWTYPE)) {
            for (CrewType ct : CrewType.values()) {
                if (ct.toString().equalsIgnoreCase(crewAttr.get(ATTR_CREWTYPE))) {
                    crewType = ct;
                    break;
                }
            }
        }
        crew = new Crew(Objects.requireNonNullElse(crewType, CrewType.SINGLE));
        pilots.add(crew);
        for (int i = 0; i < crew.getSlotCount(); i++) {
            crew.setMissing(true, i);
        }

        for (int n = 0; n < crewNode.getChildNodes().getLength(); n++) {
            final Node pilotNode = crewNode.getChildNodes().item(n);
            if (pilotNode.getNodeName().equalsIgnoreCase(ELE_CREWMEMBER)) {
                final Map<String, String> pilotAttr = new HashMap<>(crewAttr);
                for (int i = 0; i < pilotNode.getAttributes().getLength(); i++) {
                    final Node node = pilotNode.getAttributes().item(i);
                    pilotAttr.put(node.getNodeName(), node.getTextContent());
                }
                int slot = -1;
                if (pilotAttr.containsKey(ATTR_SLOT) && !pilotAttr.get(ATTR_SLOT).isBlank()) {
                    try {
                        slot = Integer.parseInt(pilotAttr.get(ATTR_SLOT));
                    } catch (NumberFormatException ex) {
                        warning.append("Illegal crew slot index: ").append(pilotAttr.get(ATTR_SLOT));
                    }
                }
                if (slot < 0 && slot >= crew.getSlotCount()) {
                    warning.append("Illegal crew slot index for ").append(crewType)
                            .append(" cockpit: ").append(slot);
                } else {
                    crew.setMissing(false, slot);
                    setPilotAttributes(options, crew, slot, pilotAttr);
                }
            }
        }
        setCrewAttributes(options, entity, crew, crewAttr);
    }

    /**
     * Helper method that sets field values for the crew as a whole, either from a <pilot> element
     * (single/collective crews) or a <crew> element (multi-crew cockpits). If an <code>Entity</code>
     * is provided, the crew will be assigned to it.
     *
     * @param options    The <code>GameOptions</code> set when loading this crew
     * @param entity     The <code>Entity</code> for this crew (or null if the crew has abandoned the unit).
     * @param crew       The crew to set fields for.
     * @param attributes Attribute values of the <code>pilot</code> or <code>crew</code>
     *                   element mapped to the attribute name.
     */
    private void setCrewAttributes(final @Nullable GameOptions options, final Entity entity,
                                   final Crew crew, final Map<String,String> attributes) {
        // init bonus
        int initBVal = 0;
        if ((attributes.containsKey(ATTR_INITB)) && !attributes.get(ATTR_INITB).isBlank()) {
            try {
                initBVal = Integer.parseInt(attributes.get(ATTR_INITB));
            } catch (NumberFormatException ignored) {

            }
        }
        int commandBVal = 0;
        if ((attributes.containsKey(ATTR_COMMANDB)) && !attributes.get(ATTR_COMMANDB).isBlank()) {
            try {
                commandBVal = Integer.parseInt(attributes.get(ATTR_COMMANDB));
            } catch (NumberFormatException ignored) {

            }
        }

        if (attributes.containsKey(ATTR_SIZE)) {
            if (!attributes.get(ATTR_SIZE).isBlank()) {
                int crewSize = 1;
                try {
                    crewSize = Integer.parseInt(attributes.get(ATTR_SIZE));
                } catch (NumberFormatException ignored) {

                }
                crew.setSize(crewSize);
            } else if (null != entity) {
                crew.setSize(Compute.getFullCrewSize(entity));
                //Reset the currentSize equal to the max size
                crew.setCurrentSize(Compute.getFullCrewSize(entity));
            }
        }

        if (attributes.containsKey(ATTR_CURRENTSIZE)) {
            if (!attributes.get(ATTR_CURRENTSIZE).isBlank()) {
                int crewCurrentSize = 1;
                try {
                    crewCurrentSize = Integer.parseInt(attributes.get(ATTR_CURRENTSIZE));
                } catch (NumberFormatException ignored) {

                }
                crew.setCurrentSize(crewCurrentSize);
            } else if (null != entity) {
                //Reset the currentSize equal to the max size
                crew.setCurrentSize(Compute.getFullCrewSize(entity));
            }
        }

        crew.setInitBonus(initBVal);
        crew.setCommandBonus(commandBVal);

        if ((options != null) && options.booleanOption(OptionsConstants.RPG_PILOT_ADVANTAGES)
                && attributes.containsKey(ATTR_ADVS) && !attributes.get(ATTR_ADVS).isBlank()) {
            StringTokenizer st = new StringTokenizer(attributes.get(ATTR_ADVS), "::");
            while (st.hasMoreTokens()) {
                String adv = st.nextToken();
                String advName = Crew.parseAdvantageName(adv);
                Object value = Crew.parseAdvantageValue(adv);

                try {
                    crew.getOptions().getOption(advName).setValue(value);
                } catch (Exception e) {
                    warning.append("Error restoring advantage: ").append(adv).append(".\n");
                }
            }

        }

        if ((options != null) && options.booleanOption(OptionsConstants.EDGE)
                && attributes.containsKey(ATTR_EDGE) && !attributes.get(ATTR_EDGE).isBlank()) {
            StringTokenizer st = new StringTokenizer(attributes.get(ATTR_EDGE), "::");
            while (st.hasMoreTokens()) {
                String edg = st.nextToken();
                String edgeName = Crew.parseAdvantageName(edg);
                Object value = Crew.parseAdvantageValue(edg);

                try {
                    crew.getOptions().getOption(edgeName).setValue(value);
                } catch (Exception e) {
                    warning.append("Error restoring edge: ").append(edg).append(".\n");
                }
            }
        }

        if ((options != null) && options.booleanOption(OptionsConstants.RPG_MANEI_DOMINI)
                && attributes.containsKey(ATTR_IMPLANTS) && !attributes.get(ATTR_IMPLANTS).isBlank()) {
            StringTokenizer st = new StringTokenizer(attributes.get(ATTR_IMPLANTS), "::");
            while (st.hasMoreTokens()) {
                String implant = st.nextToken();
                String implantName = Crew.parseAdvantageName(implant);
                Object value = Crew.parseAdvantageValue(implant);

                try {
                    crew.getOptions().getOption(implantName).setValue(value);
                } catch (Exception e) {
                    warning.append("Error restoring implants: ").append(implant).append(".\n");
                }
            }
        }

        if (attributes.containsKey(ATTR_EJECTED) && !attributes.get(ATTR_EJECTED).isBlank()) {
            crew.setEjected(Boolean.parseBoolean(attributes.get(ATTR_EJECTED)));
        }

        if (null != entity) {
            // Set the crew for this entity.
            entity.setCrew(crew);

            if (attributes.containsKey(ATTR_AUTOEJECT) && !attributes.get(ATTR_AUTOEJECT).isBlank()) {
                ((Mech) entity).setAutoEject(Boolean.parseBoolean(attributes.get(ATTR_AUTOEJECT)));
            }

            if (attributes.containsKey(ATTR_CONDEJECTAMMO) && !attributes.get(ATTR_CONDEJECTAMMO).isBlank()) {
                ((Mech) entity).setCondEjectAmmo(Boolean.parseBoolean(attributes.get(ATTR_CONDEJECTAMMO)));
            }

            if (attributes.containsKey(ATTR_CONDEJECTENGINE) && !attributes.get(ATTR_CONDEJECTENGINE).isBlank()) {
                ((Mech) entity).setCondEjectEngine(Boolean.parseBoolean(attributes.get(ATTR_CONDEJECTENGINE)));
            }

            if (attributes.containsKey(ATTR_CONDEJECTCTDEST) && !attributes.get(ATTR_CONDEJECTCTDEST).isBlank()) {
                ((Mech) entity).setCondEjectCTDest(Boolean.parseBoolean(attributes.get(ATTR_CONDEJECTCTDEST)));
            }

            if (attributes.containsKey(ATTR_CONDEJECTHEADSHOT) && !attributes.get(ATTR_CONDEJECTHEADSHOT).isBlank()) {
                ((Mech) entity).setCondEjectHeadshot(Boolean.parseBoolean(attributes.get(ATTR_CONDEJECTHEADSHOT)));
            }
        }
    }

    /**
     * Helper method that parses attributes common to both single/collective crews and individual
     * slots of a unit with a multi-crew cockpit.
     *
     * @param crew The crew object for set values for
     * @param slot The slot of the crew object that corresponds to these attributes.
     * @param attributes A map of attribute values keyed to the attribute names.
     */
    private void setPilotAttributes(final @Nullable GameOptions options, final Crew crew,
                                    final int slot, final Map<String, String> attributes) {
        final boolean hasGun = attributes.containsKey(ATTR_GUNNERY) && !attributes.get(ATTR_GUNNERY).isBlank();
        final boolean hasRpgGun = attributes.containsKey(ATTR_GUNNERYL) && !attributes.get(ATTR_GUNNERYL).isBlank()
                && attributes.containsKey(ATTR_GUNNERYM) && !attributes.get(ATTR_GUNNERYM).isBlank()
                && attributes.containsKey(ATTR_GUNNERYB) && !attributes.get(ATTR_GUNNERYB).isBlank();

        // Did we find required attributes?
        if (!hasGun && !hasRpgGun) {
            warning.append("Could not find gunnery for pilot.\n");
        } else if (!attributes.containsKey(ATTR_PILOTING) || attributes.get(ATTR_PILOTING).isBlank()) {
            warning.append("Could not find piloting for pilot.\n");
        } else {
            // Try to get a good gunnery value.
            int gunVal = -1;
            if (hasGun) {
                try {
                    gunVal = Integer.parseInt(attributes.get(ATTR_GUNNERY));
                } catch (NumberFormatException ignored) {

                }

                if ((gunVal < 0) || (gunVal > Crew.MAX_SKILL)) {
                    warning.append("Found invalid gunnery value: ")
                            .append(attributes.get(ATTR_GUNNERY)).append(".\n");
                    return;
                }
            }

            // get RPG skills
            int gunneryLVal = -1;
            int gunneryMVal = -1;
            int gunneryBVal = -1;
            if (hasRpgGun) {
                if ((attributes.containsKey(ATTR_GUNNERYL)) && !attributes.get(ATTR_GUNNERYL).isBlank()) {
                    try {
                        gunneryLVal = Integer.parseInt(attributes.get(ATTR_GUNNERYL));
                    } catch (NumberFormatException ignored) {

                    }

                    if ((gunneryLVal < 0) || (gunneryLVal > Crew.MAX_SKILL)) {
                        warning.append("Found invalid piloting value: ")
                                .append(attributes.get(ATTR_GUNNERYL)).append(".\n");
                        return;
                    }
                }

                if ((attributes.containsKey(ATTR_GUNNERYM)) && !attributes.get(ATTR_GUNNERYM).isBlank()) {
                    try {
                        gunneryMVal = Integer.parseInt(attributes.get(ATTR_GUNNERYM));
                    } catch (NumberFormatException ignored) {

                    }

                    if ((gunneryMVal < 0) || (gunneryMVal > Crew.MAX_SKILL)) {
                        warning.append("Found invalid piloting value: ")
                                .append(attributes.get(ATTR_GUNNERYM)).append(".\n");
                        return;
                    }
                }

                if ((attributes.containsKey(ATTR_GUNNERYB)) && !attributes.get(ATTR_GUNNERYB).isBlank()) {
                    try {
                        gunneryBVal = Integer.parseInt(attributes.get(ATTR_GUNNERYB));
                    } catch (NumberFormatException ignored) {

                    }

                    if ((gunneryBVal < 0) || (gunneryBVal > Crew.MAX_SKILL)) {
                        warning.append("Found invalid piloting value: ")
                                .append(attributes.get(ATTR_GUNNERYB)).append(".\n");
                        return;
                    }
                }
            }

            if (!hasGun) {
                gunVal = (int) Math.floor((gunneryLVal + gunneryMVal + gunneryBVal) / 3.0);
            } else if (!hasRpgGun) {
                gunneryLVal = gunVal;
                gunneryMVal = gunVal;
                gunneryBVal = gunVal;
            }

            // Try to get a good piloting value.
            int pilotVal = -1;
            try {
                pilotVal = Integer.parseInt(attributes.get(ATTR_PILOTING));
            } catch (NumberFormatException ignored) {

            }

            if ((pilotVal < 0) || (pilotVal > Crew.MAX_SKILL)) {
                warning.append("Found invalid piloting value: ")
                        .append(attributes.get(ATTR_PILOTING)).append(".\n");
                return;
            }

            // toughness
            int toughVal = 0;
            if ((options != null) && options.booleanOption(OptionsConstants.RPG_TOUGHNESS)
                    && (attributes.containsKey(ATTR_TOUGH)) && !attributes.get(ATTR_TOUGH).isBlank()) {
                try {
                    toughVal = Integer.parseInt(attributes.get(ATTR_TOUGH));
                } catch (NumberFormatException ignored) {

                }
            }

            int artVal = gunVal;
            if ((options != null) && options.booleanOption(OptionsConstants.RPG_ARTILLERY_SKILL)
                    && (attributes.containsKey(ATTR_ARTILLERY)) && !attributes.get(ATTR_ARTILLERY).isBlank()) {
                try {
                    artVal = Integer.parseInt(attributes.get(ATTR_ARTILLERY));
                } catch (NumberFormatException ignored) {

                }
                if ((artVal < 0) || (artVal > Crew.MAX_SKILL)) {
                    warning.append("Found invalid artillery value: ")
                            .append(attributes.get(ATTR_ARTILLERY)).append(".\n");
                    return;
                }
            }

            crew.setGunnery(gunVal, slot);
            crew.setGunneryL(gunneryLVal, slot);
            crew.setGunneryM(gunneryMVal, slot);
            crew.setGunneryB(gunneryBVal, slot);
            crew.setArtillery(artVal, slot);
            crew.setPiloting(pilotVal, slot);
            crew.setToughness(toughVal, slot);

            if ((attributes.containsKey(ATTR_NAME)) && !attributes.get(ATTR_NAME).isBlank()) {
                crew.setName(attributes.get(ATTR_NAME), slot);
            } else {
                crew.setName(RandomNameGenerator.UNNAMED_FULL_NAME, slot);
            }

            if ((attributes.containsKey(ATTR_NICK)) && !attributes.get(ATTR_NICK).isBlank()) {
                crew.setNickname(attributes.get(ATTR_NICK), slot);
            }

            if ((attributes.containsKey(ATTR_GENDER)) && !attributes.get(ATTR_GENDER).isBlank()) {
                crew.setGender(Gender.parseFromString(attributes.get(ATTR_GENDER)), slot);
            }

            if ((attributes.containsKey(ATTR_CAT_PORTRAIT)) && !attributes.get(ATTR_CAT_PORTRAIT).isBlank()) {
                crew.getPortrait(slot).setCategory(attributes.get(ATTR_CAT_PORTRAIT));
            }

            if ((attributes.containsKey(ATTR_FILE_PORTRAIT)) && !attributes.get(ATTR_FILE_PORTRAIT).isBlank()) {
                crew.getPortrait(slot).setFilename(attributes.get(ATTR_FILE_PORTRAIT));
            }

            // Was the crew wounded?
            if (attributes.containsKey(ATTR_HITS) && !attributes.get(ATTR_HITS).isBlank()) {
                // Try to get a good hits value.
                int hitVal = -1;
                try {
                    hitVal = Integer.parseInt(attributes.get(ATTR_HITS));
                } catch (NumberFormatException ignored) {

                }

                if (attributes.get(ATTR_HITS).equals(VALUE_DEAD)) {
                    crew.setDead(true, slot);
                    warning.append(crew.getNameAndRole(slot)).append(" is dead.\n");
                } else if ((hitVal < 0) || (hitVal > 5)) {
                    warning.append("Found invalid hits value: ")
                            .append(attributes.get(ATTR_HITS)).append(".\n");
                } else {
                    crew.setHits(hitVal, slot);
                }

            }

            if ((attributes.containsKey(ATTR_EXT_ID)) && !attributes.get(ATTR_EXT_ID).isBlank()) {
                crew.setExternalIdAsString(attributes.get(ATTR_EXT_ID), slot);
            }

            if (attributes.containsKey(EXTRA_DATA)) {
                try {
                    Map<String, String> extraData = new HashMap<>();
                    String[] valuePairs = attributes.get(EXTRA_DATA).split("\\|");
                    String[] values;
                    for (String valuePair : valuePairs) {
                        values = valuePair.split("=");
                        extraData.put(values[0], values[1]);
                    }
                    crew.setExtraDataForCrewMember(slot, extraData);
                } catch (Exception e) {
                    LogManager.getLogger().error("Error in loading MUL, issues with extraData elements!");
                }
            }
        }
    }

    /**
     * Parse a location tag and update the given <code>Entity</code> based on
     * the contents.
     *
     * @param locationTag
     * @param entity
     */
    private void parseLocation(Element locationTag, Entity entity) {
        // Look for the element's attributes.
        String index = locationTag.getAttribute(ATTR_INDEX);
        String destroyed = locationTag.getAttribute(ATTR_IS_DESTROYED);

        int loc;
        // Some units, like tanks and protos, keep track as Ammo slots as N/A
        // Since they don't have slot indices, they are accessed in order so
        // we keep track of the number of ammo slots processed for a loc
        int locAmmoCount = 0;
        // Did we find required attributes?
        if ((index == null) || index.isBlank()) {
            warning.append("Could not find index for location.\n");
            return;
        } else {
            // Try to get a good index value.
            loc = -1;
            try {
                loc = Integer.parseInt(index);
            } catch (NumberFormatException ignored) {

            }

            if (loc < 0) {
                warning.append(
                        "Found invalid index value for location: ")
                        .append(index).append(".\n");
                return;
            } else if (loc >= entity.locations()) {
                warning.append("The entity, ")
                        .append(entity.getShortName())
                        .append(" does not have a location at index: ")
                        .append(loc).append(".\n");
                return;
            } else {
                try {
                    if (Boolean.parseBoolean(destroyed)) {
                        destroyLocation(entity, loc);
                    }
                } catch (Exception ignored) {
                    warning.append("Found invalid isDestroyed value: ")
                            .append(destroyed).append(".\n");
                }
            } // End have-valid-index
        } // End have-required-fields

        // Handle children
        NodeList nl = locationTag.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node currNode = nl.item(i);

            if (currNode.getParentNode() != locationTag) {
                continue;
            }
            int nodeType = currNode.getNodeType();
            if (nodeType == Node.ELEMENT_NODE) {
                Element currEle = (Element) currNode;
                String nodeName = currNode.getNodeName();
                if (nodeName.equalsIgnoreCase(ELE_ARMOR)) {
                    parseArmor(currEle, entity, loc);
                } else if (nodeName.equalsIgnoreCase(ELE_BREACH)) {
                    breachLocation(entity, loc);
                } else if (nodeName.equalsIgnoreCase(ELE_BLOWN_OFF)) {
                    blowOffLocation(entity, loc);
                } else if (nodeName.equalsIgnoreCase(ELE_SLOT)) {
                    locAmmoCount = parseSlot(currEle, entity, loc, locAmmoCount);
                } else if (nodeName.equalsIgnoreCase(ELE_STABILIZER)) {
                    String hit = currEle.getAttribute(ATTR_IS_HIT);
                    if (!hit.isBlank()) {
                        ((Tank) entity).setStabiliserHit(loc);
                    }
                }
            }
        }
    }

    /**
     * Parse an armor tag for the given Entity and location.
     *
     * @param armorTag
     * @param entity
     * @param loc
     */
    private void parseArmor(Element armorTag, Entity entity, int loc) {
     // Look for the element's attributes.
        String points = armorTag.getAttribute(ATTR_POINTS);
        String type = armorTag.getAttribute(ATTR_TYPE);

        // Did we find required attributes?
        if (points.isBlank()) {
            warning.append("Could not find points for armor.\n");
        } else {

            // Try to get a good points value.
            int pointsVal = -1;
            try {
                pointsVal = Integer.parseInt(points);
            } catch (NumberFormatException ignored) {

            }

            if (points.equals(VALUE_NA)) {
                pointsVal = IArmorState.ARMOR_NA;
            } else if (points.equals(VALUE_DESTROYED)) {
                pointsVal = IArmorState.ARMOR_DESTROYED;
            } else if ((pointsVal < 0) || (pointsVal > 2000)) {
                warning.append("Found invalid points value: ")
                        .append(points).append(".\n");
                return;
            }

            // Assign the points to the correct location.
            // Sanity check the armor value before setting it.
            if (type.isBlank() || type.equals(VALUE_FRONT)) {
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
            } else if (type.equals(VALUE_INTERNAL)) {
                if (entity.getOInternal(loc) < pointsVal) {
                    warning.append("The entity, ").append(entity.getShortName()).append(" does not start with ")
                            .append(pointsVal).append(" points of internal structure for location: ")
                            .append(loc).append(".\n");
                } else {
                    entity.setInternal(pointsVal, loc);
                    if (entity instanceof Infantry) {
                        ((Infantry) entity).damageOrRestoreFieldWeapons();
                        entity.applyDamage();
                    }
                }
            } else if (type.equals(VALUE_REAR)) {
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
        }
    }

    /**
     * Parse a slot tag for the given Entity and location.
     *
     * @param slotTag
     * @param entity
     * @param loc
     */
    private int parseSlot(Element slotTag, Entity entity, int loc, int locAmmoCount) {
        // Look for the element's attributes.
        String index = slotTag.getAttribute(ATTR_INDEX);
        String type = slotTag.getAttribute(ATTR_TYPE);
        // String rear = slotTag.getAttribute( IS_REAR ); // is never read.
        String shots = slotTag.getAttribute(ATTR_SHOTS);
        String capacity = slotTag.getAttribute(ATTR_CAPACITY);
        String hit = slotTag.getAttribute(ATTR_IS_HIT);
        String destroyed = slotTag.getAttribute(ATTR_IS_DESTROYED);
        String repairable = (slotTag.getAttribute(ATTR_IS_REPAIRABLE).isBlank() ? "true" : slotTag.getAttribute(ATTR_IS_REPAIRABLE));
        String munition = slotTag.getAttribute(ATTR_MUNITION);
        String standard = slotTag.getAttribute(ATTR_STANDARD);
        String inferno = slotTag.getAttribute(ATTR_INFERNO);
        String quirks = slotTag.getAttribute(ATTR_QUIRKS);
        String trooperMiss = slotTag.getAttribute(ATTR_TROOPER_MISS);
        String rfmg = slotTag.getAttribute(ATTR_RFMG);
        String bayIndex = slotTag.getAttribute(ATTR_WEAPONS_BAY_INDEX);

        // Did we find required attributes?
        if (index.isBlank()) {
            warning.append("Could not find index for slot.\n");
            return locAmmoCount;
        } else if (type.isBlank()) {
            warning.append("Could not find type for slot.\n");
            return locAmmoCount;
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
            if (index.equals(VALUE_NA)) {
                indexVal = IArmorState.ARMOR_NA;

                // Protomechs only have system slots,
                // so we have to handle the ammo specially.
                if (entity instanceof Protomech || entity instanceof GunEmplacement) {
                    // Get the saved ammo load.
                    EquipmentType newLoad = EquipmentType.get(type);
                    if (newLoad instanceof AmmoType) {
                        int counter = -1;
                        Iterator<AmmoMounted> ammo = entity.getAmmo()
                                .iterator();
                        while (ammo.hasNext()
                                && (counter < locAmmoCount)) {

                            // Is this mounted in the current location?
                            AmmoMounted mounted = ammo.next();
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
                                    if (shots.equals(VALUE_NA)) {
                                        shotsVal = IArmorState.ARMOR_NA;
                                        warning.append(
                                                "Expected to find number of " +
                                                "shots for ")
                                                .append(type)
                                                .append(", but found ")
                                                .append(shots)
                                                .append(" instead.\n");
                                    } else if ((shotsVal < 0)
                                            || (shotsVal > 200)) {
                                        warning.append(
                                                "Found invalid shots value " +
                                                "for slot: ")
                                                .append(shots)
                                                .append(".\n");
                                    } else {

                                        // Change to the saved
                                        // ammo type and shots.
                                        mounted.changeAmmoType((AmmoType)
                                                newLoad);
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
                return locAmmoCount;
            } else if ((indexVal < 0)) {
                warning.append("Found invalid index value for slot: ")
                        .append(index).append(".\n");
                return locAmmoCount;
            }

            // Is this index valid for this entity?
            if (indexVal > entity.getNumberOfCriticals(loc)) {
                warning.append("The entity, ")
                        .append(entity.getShortName())
                        .append(" does not have ").append(index)
                        .append(" slots in location ").append(loc)
                        .append(".\n");
                return locAmmoCount;
            }

            // Try to get a good isHit value.
            boolean hitFlag = Boolean.parseBoolean(hit);

            // Is the location destroyed?
            boolean destFlag = Boolean.parseBoolean(destroyed);

            // Is the location repairable?
            boolean repairFlag = Boolean.parseBoolean(repairable);

            // Try to get the critical slot.
            CriticalSlot slot = entity.getCritical(loc, indexVal);

            // If we couldn't find a critical slot,
            // it's possible that this is "extra" ammo in a weapons bay, so we may attempt
            // to shove it in there
            if (slot == null) {
                if ((entity.usesWeaponBays() || (entity instanceof Dropship)) && !bayIndex.isBlank()) {
                    addExtraAmmoToBay(entity, loc, type, bayIndex);
                    slot = entity.getCritical(loc, indexVal);
                }
            }

            if (slot == null) {
                if (!type.equals(VALUE_EMPTY)) {
                    warning.append("Could not find the ")
                            .append(type)
                            .append(" equipment that was expected at index ")
                            .append(indexVal).append(" of location ")
                            .append(loc).append(".\n");
                }
                return locAmmoCount;
            }

            // Is the slot for a critical system?
            if (slot.getType() == CriticalSlot.TYPE_SYSTEM) {

                // Does the XML file have some other kind of equipment?
                if (!type.equals(VALUE_SYSTEM)) {
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
                if (!quirks.isBlank()) {
                    StringTokenizer st = new StringTokenizer(quirks, "::");
                    while (st.hasMoreTokens()) {
                        String quirk = st.nextToken();
                        String quirkName = Crew.parseAdvantageName(quirk);
                        Object value = Crew.parseAdvantageValue(quirk);

                        try {
                            mounted.getQuirks().getOption(quirkName).setValue(value);
                        } catch (Exception e) {
                            warning.append("Error restoring quirk: ").append(quirk).append(".\n");
                        }
                    }
                }

                // trooper missing equipment
                if (!trooperMiss.isBlank()) {
                    StringTokenizer st = new StringTokenizer(trooperMiss, "::");
                    int i = BattleArmor.LOC_TROOPER_1;
                    while (st.hasMoreTokens() && i <= BattleArmor.LOC_TROOPER_6) {
                        String tmiss = st.nextToken();
                        mounted.setMissingForTrooper(i, Boolean.parseBoolean(tmiss));
                        i++;
                    }
                }

                // Hit and destroy the mounted, according to the flags.
                mounted.setDestroyed(hitFlag || destFlag);

                mounted.setRepairable(repairFlag);

                mounted.setRapidfire(Boolean.parseBoolean(rfmg));

                // Is the mounted a type of ammo?
                if (mounted instanceof AmmoMounted) {
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
                        if (shots.equals(VALUE_NA)) {
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
                            ((AmmoMounted) mounted).changeAmmoType((AmmoType) newLoad);
                            mounted.setShotsLeft(shotsVal);

                        } // End have-good-shots-value
                        try {
                            double capVal = Double.parseDouble(capacity);
                            ((AmmoMounted) mounted).setAmmoCapacity(capVal);
                        } catch (NumberFormatException excep) {
                            // Handled by the next if test.
                        }
                        if (capacity.equals(VALUE_NA)) {
                            if (entity.hasETypeFlag(Entity.ETYPE_BATTLEARMOR)
                                    || entity.hasETypeFlag(Entity.ETYPE_PROTOMECH)) {
                                ((AmmoMounted) mounted).setAmmoCapacity(mounted.getOriginalShots()
                                         * ((AmmoType) mounted.getType()).getKgPerShot() * 1000);
                            } else {
                                ((AmmoMounted) mounted).setAmmoCapacity(mounted.getOriginalShots()
                                        * mounted.getTonnage()
                                        / ((AmmoType) mounted.getType()).getShots());
                            }
                        }


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
                if (!munition.isBlank()) {
                    // Retrieve munition by name.
                    EquipmentType munType = EquipmentType.get(munition);

                    // Make sure munition is a type of ammo.
                    if (munType instanceof AmmoType) {
                        // Change to the saved munition type.
                        ((AmmoMounted) mounted.getLinked()).changeAmmoType((AmmoType) munType);
                    } else {
                        // Bad XML equipment.
                        warning.append("XML file expects")
                                .append(" ammo for munition argument of")
                                .append(" slot tag.\n");
                    }
                }
                if (entity.isSupportVehicle() && (mounted.getType() instanceof InfantryWeapon)) {
                    for (Mounted ammo = mounted.getLinked(); ammo != null; ammo = ammo.getLinked()) {
                        if (((AmmoType) ammo.getType()).getMunitionType().contains(AmmoType.Munitions.M_INFERNO)) {
                            if (!inferno.isBlank()) {
                                String[] fields = inferno.split(":");
                                ammo.setShotsLeft(Integer.parseInt(fields[0]));
                                ammo.setOriginalShots(Integer.parseInt(fields[1]));
                            }
                        } else {
                            if (!standard.isBlank()) {
                                String[] fields = standard.split(":");
                                ammo.setShotsLeft(Integer.parseInt(fields[0]));
                                ammo.setOriginalShots(Integer.parseInt(fields[1]));
                            }
                        }
                    }
                }

            } // End have-equipment

            // Hit and destroy the slot, according to the flags.
            slot.setHit(hitFlag);
            slot.setDestroyed(destFlag);
            slot.setRepairable(repairFlag);

        } // End have-required-fields
        return locAmmoCount;
    }

    /**
     * Parse a motive tag for the given <code>Entity</code>.
     *
     * @param motiveTag
     * @param entity
     */
    private void parseMotive(Element motiveTag, Entity entity) {
        String value = motiveTag.getAttribute(ATTR_MDAMAGE);
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
        value = motiveTag.getAttribute(ATTR_MPENALTY);
        try {
            int motivePenalty = Integer.parseInt(value);
            ((Tank) entity).setMotivePenalty(motivePenalty);
        } catch (Exception e) {
            warning.append("Invalid motive penalty value in movement tag.\n");
        }
    }

    /**
     * Parse a turretlock tag for the given <code>Entity</code>.
     *
     * @param turretLockTag
     * @param entity
     */
    private void parseTurretLock(Element turretLockTag, Entity entity) {
        String value = turretLockTag.getAttribute(ATTR_DIRECTION);
        try {
            int turDir = Integer.parseInt(value);
            entity.setSecondaryFacing(turDir);
            ((Tank) entity).lockTurret(((Tank) entity).getLocTurret());
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
            warning.append("Invalid turret lock direction value in movement tag.\n");
        }
    }

    /**
     * Parse a turret2lock tag for the given <code>Entity</code>.
     *
     * @param turret2LockTag
     * @param entity
     */
    private void parseTurret2Lock(Element turret2LockTag, Entity entity) {
        String value = turret2LockTag.getAttribute(ATTR_DIRECTION);
        try {
            int turDir = Integer.parseInt(value);
            ((Tank) entity).setDualTurretOffset(turDir);
            ((Tank) entity).lockTurret(((Tank) entity).getLocTurret2());
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
            warning.append("Invalid turret2 lock direction value in movement tag.\n");
        }
    }

    /**
     * Parse a si tag for the given <code>Entity</code>.
     *
     * @param siTag
     * @param entity
     */
    private void parseSI(Element siTag, Entity entity) {
        String value = siTag.getAttribute(ATTR_INTEGRITY);
        try {
            int newSI = Integer.parseInt(value);
            ((Aero) entity).setSI(newSI);
        } catch (Exception ignored) {
            warning.append("Invalid SI value in structural integrity tag.\n");
        }
    }

    /**
     * Parse a heat tag for the given <code>Entity</code>.
     *
     * @param heatTag
     * @param entity
     */
    private void parseHeat(Element heatTag, Entity entity) {
        String value = heatTag.getAttribute(ATTR_SINK);
        try {
            int newSinks = Integer.parseInt(value);
            ((Aero) entity).setHeatSinks(newSinks);
        } catch (Exception ignored) {
            warning.append("Invalid heat sink value in heat sink tag.\n");
        }
    }

    /**
     * Parse a fuel tag for the given <code>Entity</code>.
     *
     * @param fuelTag
     * @param entity
     */
    private void parseFuel(Element fuelTag, Entity entity) {
        String value = fuelTag.getAttribute(ATTR_LEFT);
        try {
            int newFuel = Integer.parseInt(value);
            ((IAero) entity).setFuel(newFuel);
        } catch (Exception ignored) {
            warning.append("Invalid fuel value in fuel tag.\n");
        }
    }

    /**
     * Parse a kf tag for the given <code>Entity</code>.
     *
     * @param kfTag
     * @param entity
     */
    private void parseKF(Element kfTag, Entity entity) {
        String value = kfTag.getAttribute(ATTR_INTEGRITY);
        try {
            int newIntegrity = Integer.parseInt(value);
            ((Jumpship) entity).setKFIntegrity(newIntegrity);
        } catch (Exception e) {
            warning.append("Invalid KF integrity value in KF integrity tag.\n");
        }
    }

    /**
     * Parse a sail tag for the given <code>Entity</code>.
     *
     * @param sailTag
     * @param entity
     */
    private void parseSail(Element sailTag, Entity entity) {
        String value = sailTag.getAttribute(ATTR_INTEGRITY);
        try {
            int newIntegrity = Integer.parseInt(value);
            ((Jumpship) entity).setSailIntegrity(newIntegrity);
        } catch (Exception e) {
            warning.append("Invalid sail integrity value in sail integrity tag.\n");
        }
    }

    /**
     * Parse an aeroCrit tag for the given <code>Entity</code>.
     *
     * @param aeroCritTag
     * @param entity
     */
    private void parseAeroCrit(Element aeroCritTag, Entity entity) {
        String avionics = aeroCritTag.getAttribute(ATTR_AVIONICS);
        String sensors = aeroCritTag.getAttribute(ATTR_SENSORS);
        String engine = aeroCritTag.getAttribute(ATTR_ENGINE);
        String fcs = aeroCritTag.getAttribute(ATTR_FCS);
        String cic = aeroCritTag.getAttribute(ATTR_CIC);
        String leftThrust = aeroCritTag.getAttribute(ATTR_LEFT_THRUST);
        String rightThrust = aeroCritTag.getAttribute(ATTR_RIGHT_THRUST);
        String lifeSupport = aeroCritTag.getAttribute(ATTR_LIFE_SUPPORT);
        String gear = aeroCritTag.getAttribute(ATTR_GEAR);

        Aero a = (Aero) entity;

        if (!avionics.isBlank()) {
            a.setAvionicsHits(Integer.parseInt(avionics));
        }

        if (!sensors.isBlank()) {
            a.setSensorHits(Integer.parseInt(sensors));
        }

        if (!engine.isBlank()) {
            a.setEngineHits(Integer.parseInt(engine));
        }

        if (!fcs.isBlank()) {
            a.setFCSHits(Integer.parseInt(fcs));
        }

        if (!cic.isBlank()) {
            a.setCICHits(Integer.parseInt(cic));
        }

        if (!leftThrust.isBlank()) {
            a.setLeftThrustHits(Integer.parseInt(leftThrust));
        }

        if (!rightThrust.isBlank()) {
            a.setRightThrustHits(Integer.parseInt(rightThrust));
        }

        if (!lifeSupport.isBlank()) {
            a.setLifeSupport(false);
        }

        if (!gear.isBlank()) {
            a.setGearHit(true);
        }
    }

    /**
     *  Parse a dropCrit tag for the given <code>Entity</code>.
     *  @param dropCritTag
     *  @param entity
     */
    private void parseDropCrit(Element dropCritTag, Entity entity) {
        String dockingcollar = dropCritTag.getAttribute(ATTR_DOCKING_COLLAR);
        String kfboom = dropCritTag.getAttribute(ATTR_KFBOOM);

        Dropship d = (Dropship) entity;

        if (!dockingcollar.isBlank()) {
            d.setDamageDockCollar(true);
        }

        if (!kfboom.isBlank()) {
            d.setDamageKFBoom(true);
        }
    }

    /**
     *  Parse cargo bay and door the given <code>Entity</code>.
     *  Borrowed all this from the code that handles vehicle stabilizer crits by location.
     *
     *  @param entity
     */
    private void parseTransportBay (Element bayTag, Entity entity) {
        // Look for the element's attributes.
        String index = bayTag.getAttribute(ATTR_INDEX);

        int bay;
        // Did we find the required index?
        if (index.isBlank()) {
            warning.append("Could not find index for bay.\n");
            return;
        } else {
        // Try to get a good index value.
            bay = -1;
            try {
                bay = Integer.parseInt(index);
            } catch (NumberFormatException ignored) {
                // Handled by the next if test
            }

            if (bay < 0) {
                warning.append("Found invalid index value for bay: ").append(index).append(".\n");
                return;
            } else if (entity.getBayById(bay) == null) {
                warning.append("The entity, ")
                    .append(entity.getShortName())
                    .append(" does not have a bay at index: ")
                    .append(bay).append(".\n");
                return;
            }
        }

        Bay currentbay = entity.getBayById(bay);

        // Handle children for each bay.
        NodeList nl = bayTag.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node currNode = nl.item(i);

            if (currNode.getParentNode() != bayTag) {
                continue;
            }
            int nodeType = currNode.getNodeType();
            if (nodeType == Node.ELEMENT_NODE) {
                String nodeName = currNode.getNodeName();
                if (nodeName.equalsIgnoreCase(ELE_BAYDAMAGE)) {
                    currentbay.setBayDamage(Double.parseDouble(currNode.getTextContent()));
                } else if (nodeName.equalsIgnoreCase(ELE_BAYDOORS)) {
                    currentbay.setCurrentDoors(Integer.parseInt(currNode.getTextContent()));
                } else if (nodeName.equalsIgnoreCase(ELE_LOADED)) {
                    currentbay.troops.add(Integer.parseInt(currNode.getTextContent()));
                }
            }
        }
    }

    /**
     * Parse a tankCrit tag for the given <code>Entity</code>.
     *
     * @param tankCrit
     * @param entity
     */
    private void parseTankCrit(Element tankCrit, Entity entity) {
        String sensors = tankCrit.getAttribute(ATTR_SENSORS);
        String engine = tankCrit.getAttribute(ATTR_ENGINE);
        String driver = tankCrit.getAttribute(ATTR_DRIVER);
        String commander = tankCrit.getAttribute(ATTR_COMMANDER);

        Tank t = (Tank) entity;

        if (!sensors.isBlank()) {
            t.setSensorHits(Integer.parseInt(sensors));
        }

        if (engine.equalsIgnoreCase(VALUE_HIT)) {
            t.engineHit();
            t.applyDamage();
        }

        if (driver.equalsIgnoreCase(VALUE_HIT)) {
            t.setDriverHit(true);
        }

        if (commander.equalsIgnoreCase(VALUE_CONSOLE)) {
            t.setUsingConsoleCommander(true);
        } else if (commander.equalsIgnoreCase(VALUE_HIT)) {
            t.setCommanderHit(true);
        }
    }

    /**
     * Parse a bombs tag for the given <code>Entity</code>.
     *
     * @param bombsTag
     * @param entity
     */
    private void parseBombs(Element bombsTag, Entity entity) {
        if (!(entity instanceof IBomber)) {
            warning.append("Found a bomb but Entity cannot carry bombs.\n");
            return;
        }

        // Deal with any child nodes
        NodeList nl = bombsTag.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node currNode = nl.item(i);

            if (currNode.getParentNode() != bombsTag) {
                continue;
            }
            int nodeType = currNode.getNodeType();
            if (nodeType == Node.ELEMENT_NODE) {
                Element currEle = (Element) currNode;
                String nodeName = currNode.getNodeName();
                if (nodeName.equalsIgnoreCase(ELE_BOMB)) {
                    int[] intBombChoices = ((IBomber) entity).getIntBombChoices();
                    int[] extBombChoices = ((IBomber) entity).getExtBombChoices();
                    String type = currEle.getAttribute(ATTR_TYPE);
                    String load = currEle.getAttribute(ATTR_LOAD);
                    boolean internal = Boolean.parseBoolean(currEle.getAttribute(ATTR_INTERNAL));
                    if (!type.isBlank() && !load.isBlank()) {
                        int bombType = BombType.getBombTypeFromInternalName(type);
                        if ((bombType <= BombType.B_NONE) || (bombType >= BombType.B_NUM)) {
                            continue;
                        }

                        try {
                            if (internal) {
                                intBombChoices[bombType] += Integer.parseInt(load);
                                ((IBomber) entity).setIntBombChoices(intBombChoices);
                            } else {
                                extBombChoices[bombType] += Integer.parseInt(load);
                                ((IBomber) entity).setExtBombChoices(extBombChoices);
                            }
                        } catch (NumberFormatException ignore) {
                            // If something wrote bad bomb data, don't even bother with it - user
                            // can fix it in configure menu
                        }
                    }
                }
            }
        }
    }

    /**
     * Parse a c3i tag for the given <code>Entity</code>.
     *
     * @param c3iTag
     * @param entity
     */
    private void parseC3I(Element c3iTag, Entity entity) {
        // Deal with any child nodes
        NodeList nl = c3iTag.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node currNode = nl.item(i);

            if (currNode.getParentNode() != c3iTag) {
                continue;
            }
            int nodeType = currNode.getNodeType();
            if (nodeType == Node.ELEMENT_NODE) {
                Element currEle = (Element) currNode;
                String nodeName = currNode.getNodeName();
                if (nodeName.equalsIgnoreCase(ELE_C3ILINK)) {
                    String link = currEle.getAttribute(ATTR_LINK);
                    int pos = entity.getFreeC3iUUID();
                    if (!link.isBlank() && (pos != -1)) {
                        LogManager.getLogger().info("Loading C3i UUID " + pos + ": " + link);
                        entity.setC3iNextUUIDAsString(pos, link);
                    }
                }
            }
        }
    }

    /**
     * Parse an NC3 tag for the given <code>Entity</code>.
     *
     * @param nc3Tag
     * @param entity
     */
    private void parseNC3(Element nc3Tag, Entity entity) {
        // Deal with any child nodes
        NodeList nl = nc3Tag.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node currNode = nl.item(i);

            if (currNode.getParentNode() != nc3Tag) {
                continue;
            }
            int nodeType = currNode.getNodeType();
            if (nodeType == Node.ELEMENT_NODE) {
                Element currEle = (Element) currNode;
                String nodeName = currNode.getNodeName();
                if (nodeName.equalsIgnoreCase(ELE_NC3LINK)) {
                    String link = currEle.getAttribute(ATTR_LINK);
                    int pos = entity.getFreeNC3UUID();
                    if (!link.isBlank() && (pos != -1)) {
                        LogManager.getLogger().info("Loading NC3 UUID " + pos + ": " + link);
                        entity.setNC3NextUUIDAsString(pos, link);
                    }
                }
            }
        }
    }

    /**
     * Parse an EscapeCraft tag for the given <code>Entity</code>.
     *
     * @param escCraftTag
     * @param entity
     */
    private void parseEscapeCraft(Element escCraftTag, Entity entity) {
        if (!(entity instanceof SmallCraft || entity instanceof Jumpship)) {
            warning.append("Found an EscapeCraft tag but Entity is not a " +
                    "Crewed Spacecraft!\n");
            return;
        }

        try {
            String id = escCraftTag.getAttribute(ATTR_ID);
            ((Aero) entity).addEscapeCraft(id);
        } catch (Exception e) {
            warning.append("Invalid external entity id in EscapeCraft tag.\n");
        }
    }

    /**
     * Parse an EscapedPassengers tag for the given <code>Entity</code>.
     *
     * @param escPassTag
     * @param entity
     */
    private void parseEscapedPassengers(Element escPassTag, Entity entity) {
        if (!(entity instanceof EjectedCrew || entity instanceof SmallCraft)) {
            warning.append("Found an EscapedPassengers tag but Entity is not a " +
                    "Spacecraft Crew or Small Craft!\n");
            return;
        }
        // Deal with any child nodes
        NodeList nl = escPassTag.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node currNode = nl.item(i);
            int nodeType = currNode.getNodeType();
            if (nodeType == Node.ELEMENT_NODE) {
                Element currEle = (Element) currNode;
                String id = currEle.getAttribute(ATTR_ID);
                String number = currEle.getAttribute(ATTR_NUMBER);
                int value = Integer.parseInt(number);
                if (entity instanceof EjectedCrew) {
                    ((EjectedCrew) entity).addPassengers(id, value);
                } else {
                    ((SmallCraft) entity).addPassengers(id, value);
                }
            }
        }
    }

    /**
     * Parse an EscapedCrew tag for the given <code>Entity</code>.
     *
     * @param escCrewTag
     * @param entity
     */
    private void parseEscapedCrew(Element escCrewTag, Entity entity) {
        if (!(entity instanceof EjectedCrew || entity instanceof SmallCraft)) {
            warning.append("Found an EscapedCrew tag but Entity is not a " +
                    "Spacecraft Crew or Small Craft!\n");
            return;
        }
        // Deal with any child nodes
        NodeList nl = escCrewTag.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node currNode = nl.item(i);
            int nodeType = currNode.getNodeType();
            if (nodeType == Node.ELEMENT_NODE) {
                Element currEle = (Element) currNode;
                String id = currEle.getAttribute(ATTR_ID);
                String number = currEle.getAttribute(ATTR_NUMBER);
                int value = Integer.parseInt(number);
                if (entity instanceof EjectedCrew) {
                    ((EjectedCrew) entity).addNOtherCrew(id, value);
                } else {
                    ((SmallCraft) entity).addNOtherCrew(id, value);
                }
            }
        }
    }

    /**
     * Parse an original si tag for the given <code>Entity</code>. Used by Escape Pods
     *
     * @param OsiTag
     * @param entity
     */
    private void parseOSI(Element OsiTag, Entity entity) {
        String value = OsiTag.getAttribute(ATTR_NUMBER);
        try {
            int newSI = Integer.parseInt(value);
            ((Aero) entity).set0SI(newSI);
        } catch (Exception ignored) {
            warning.append("Invalid SI value in original structural integrity tag.\n");
        }
    }

    /**
     * Parse an original men tag for the given <code>Entity</code>. Used by Escaped spacecraft crew
     *
     * @param OMenTag
     * @param entity
     */
    private void parseOMen(Element OMenTag, Entity entity) {
        String value = OMenTag.getAttribute(ATTR_NUMBER);
        try {
            int newMen = Integer.parseInt(value);
            entity.initializeInternal(newMen, Infantry.LOC_INFANTRY);
        } catch (Exception ignored) {
            warning.append("Invalid internal value in original number of men tag.\n");
        }
    }

    /**
     * Parse a conveyance tag for the given <code>Entity</code>. Used to resolve crew damage to transported entities
     *
     * @param conveyanceTag
     * @param entity
     */
    private void parseConveyance(Element conveyanceTag, Entity entity) {
        String value = conveyanceTag.getAttribute(ATTR_ID);
        try {
            int id = Integer.parseInt(value);
            entity.setTransportId(id);
        } catch (Exception e) {
            warning.append("Invalid transport id in conveyance tag.\n");
        }
    }

    /**
     * Parse an id tag for the given <code>Entity</code>. Used to resolve crew damage to transported entities
     *
     * @param idTag
     * @param entity
     */
    private void parseId(Element idTag, Entity entity) {
        String value = idTag.getAttribute(ATTR_ID);
        //Safety. We don't want to mess with autoassigned game Ids
        if (entity.getGame() != null) {
            return;
        }
        try {
            int id = Integer.parseInt(value);
            entity.setId(id);
        } catch (Exception ignored) {
            warning.append("Invalid id in conveyance tag.\n");
        }
    }

    /**
     * Parse a force tag for the given <code>Entity</code>.
     */
    private void parseForce(Element forceTag, Entity entity) {
        entity.setForceString(forceTag.getAttribute(ATTR_FORCE));
    }

    /**
     * Parase a modularEquipmentMount tag for the supplied <code>Entity</code>.
     *
     * @param meaTag
     * @param entity
     */
    private void parseBAMEA(Element meaTag, Entity entity) {
        if (!(entity instanceof BattleArmor)) {
            warning.append("Found a BA MEA tag but Entity is not " +
                    "BattleArmor!\n");
            return;
        }

        String meaMountLocString = meaTag.getAttribute(ATTR_BA_MEA_MOUNT_LOC);
        String manipTypeName = meaTag.getAttribute(ATTR_BA_MEA_TYPE_NAME);

        // Make sure we got a mount number
        if (meaMountLocString.isBlank()) {
            warning.append("antiPersonnelMount tag does not specify a baMeaMountLoc!\n");
            return;
        }

        // We could have no mounted manipulator
        EquipmentType manipType = null;
        if (!manipTypeName.isBlank()) {
            manipType = EquipmentType.get(manipTypeName);
        }

        // Find the Mounted instance for the MEA
        Mounted mountedManip = null;
        int meaMountLoc = Integer.parseInt(meaMountLocString);
        boolean foundMea = false;
        for (Mounted m : entity.getEquipment()) {
            if ((m.getBaMountLoc() == meaMountLoc) && m.getType().hasFlag(MiscType.F_BA_MEA)) {
                foundMea = true;
                break;
            }
        }
        if (!foundMea) {
            warning.append("No modular equipment mount found in specified " + "location! Location: ")
                    .append(meaMountLoc).append("\n");
            return;
        }
        if (meaMountLoc == BattleArmor.MOUNT_LOC_LARM) {
            mountedManip = ((BattleArmor) entity).getLeftManipulator();
        } else if (meaMountLoc == BattleArmor.MOUNT_LOC_RARM) {
            mountedManip = ((BattleArmor) entity).getRightManipulator();
        }

        if (mountedManip != null) {
            entity.getEquipment().remove(mountedManip);
            entity.getMisc().remove(mountedManip);
        }

        // Was no manipulator selected?
        if (manipType == null) {
            return;
        }

        // Add the newly mounted manipulator
        try {
            int baMountLoc = mountedManip.getBaMountLoc();
            mountedManip = entity.addEquipment(manipType, mountedManip.getLocation());
            mountedManip.setBaMountLoc(baMountLoc);
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }
    }

    /**
     * Parse a antiPersonnelMount tag for the supplied <code>Entity</code>.
     *
     * @param apmTag
     * @param entity
     */
    private void parseBAAPM(Element apmTag, Entity entity) {
        if (!(entity instanceof BattleArmor)) {
            warning.append("Found a BA APM tag but Entity is not BattleArmor!\n");
            return;
        }

        String mountNumber = apmTag.getAttribute(ATTR_BA_APM_MOUNT_NUM);
        String apTypeName = apmTag.getAttribute(ATTR_BA_APM_TYPE_NAME);

        // Make sure we got a mount number
        if (mountNumber.isBlank()) {
            warning.append("antiPersonnelMount tag does not specify a baAPMountNum!\n");
            return;
        }

        Mounted apMount = entity.getEquipment(Integer.parseInt(mountNumber));
        // We may mount no AP weapon
        EquipmentType apType = null;
        if (!apTypeName.isBlank()) {
            apType = EquipmentType.get(apTypeName);
        }

        // Remove any currently mounted AP weapon
        if ((apMount.getLinked() != null) && (apMount.getLinked().getType() != apType)) {
            Mounted apWeapon = apMount.getLinked();
            entity.getEquipment().remove(apWeapon);
            entity.getWeaponList().remove(apWeapon);
            entity.getTotalWeaponList().remove(apWeapon);
            // We need to make sure that the weapon has been removed
            // from the criticals, otherwise it can cause issues
            for (int loc = 0; loc < entity.locations(); loc++) {
                for (int c = 0; c < entity.getNumberOfCriticals(loc); c++) {
                    CriticalSlot crit = entity.getCritical(loc, c);
                    if ((crit != null) && (crit.getMount() != null) && crit.getMount().equals(apWeapon)) {
                        entity.setCritical(loc, c, null);
                    }
                }
            }
        }

        // Did the selection not change, or no weapon was selected
        if (((apMount.getLinked() != null) && (apMount.getLinked().getType() == apType))
                || (apType == null)) {
            return;
        }

        // Add the newly mounted weapon
        try {
            Mounted newWeap = entity.addEquipment(apType, apMount.getLocation());
            apMount.setLinked(newWeap);
            newWeap.setLinked(apMount);
            newWeap.setAPMMounted(true);
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }
    }

    /**
     * Worker function that takes an entity, a location, an ammo type string and the critical index
     * of a weapons bay in the given location and attempts to add the ammo type there.
     * @param entity The entity we're working on loading
     * @param loc The location index on the entity
     * @param type The ammo type string
     * @param bayIndex The crit index of the bay where we want to load the ammo on the location where the bay is
     */
    private void addExtraAmmoToBay(Entity entity, int loc, String type, String bayIndex) {
        // here, we need to do the following:
        // 1: get the bay to which this ammo belongs, and add it to said bay
        // 2: add the ammo to the entity as a "new" piece of equipment
        // 3: add the ammo to a crit slot on the bay's location

        int bayCritIndex = Integer.parseInt(bayIndex);
        WeaponMounted bay = (WeaponMounted) entity.getCritical(loc, bayCritIndex - 1).getMount();

        Mounted<?> ammo = Mounted.createMounted(entity, AmmoType.get(type));

        try {
            entity.addEquipment(ammo, loc, bay.isRearMounted());
        } catch (LocationFullException ignored) {
            // silently swallow it, since DropShip locations have about a hundred crit slots
        }

        bay.addAmmoToBay(entity.getEquipmentNum(ammo));
    }

    /**
     * Determine if unexpected XML entities were encountered during parsing.
     *
     * @return <code>true</code> if a non-fatal warning occurred.
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
     * Returns a list of all of the  Entity's parsed from the input, should be
     * called after <code>parse</code>. This is for entities that we want to be loaded
     * into the chat lounge, so functional
     * @return
     */
    public Vector<Entity> getEntities() {
        Vector<Entity> toReturn = entities;
        for (Entity e : survivors) {
            if (e instanceof EjectedCrew) {
                continue;
            }
            toReturn.add(e);
        }
        return toReturn;
    }

    /**
     * Returns a list of all of the salvaged Entity's parsed from the input, should be
     * called after <code>parse</code>.
     * @return
     */
    public Vector<Entity> getSurvivors() {
        return survivors;
    }

    /**
     * Returns a list of all of the allied Entity's parsed from the input, should be
     * called after <code>parse</code>.
     * @return
     */
    public Vector<Entity> getAllies() {
        return allies;
    }

    /**
     * Returns a list of all of the salvaged Entity's parsed from the input, should be
     * called after <code>parse</code>.
     * @return
     */
    public Vector<Entity> getSalvage() {
        return salvage;
    }

    /**
     * Returns a list of all of the enemy retreated entities parsed from the input, should be
     * called after <code>parse</code>.
     * @return
     */
    public Vector<Entity> getRetreated() {
        return retreated;
    }

    /**
     * Returns a list of all of the devastated Entity's parsed from the input, should be
     * called after <code>parse</code>.
     * @return
     */
    public Vector<Entity> getDevastated() {
        return devastated;
    }

    /**
     * Returns a list of all of the Pilots parsed from the input, should be
     * called after <code>parse</code>.
     *
     * @return
     */
    public Vector<Crew> getPilots() {
        return pilots;
    }

    /**
     * Returns the kills hashtable
     *
     * @return
     */
    public Hashtable<String, String> getKills() {
        return kills;
    }

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
        if (en instanceof Infantry) {
            ((Infantry) en).damageOrRestoreFieldWeapons();
            en.applyDamage();
        }
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
    }

    private void breachLocation(Entity en, int loc) {
        // equipment marked breached
        for (Mounted mounted : en.getEquipment()) {
            if (mounted.getLocation() == loc) {
                mounted.setBreached(true);
            }
        }
        // all critical slots set as breached
        for (int i = 0; i < en.getNumberOfCriticals(loc); i++) {
            final CriticalSlot cs = en.getCritical(loc, i);
            if (cs != null) {
                cs.setBreached(true);
            }
        }
        en.setLocationStatus(loc, ILocationExposureStatus.BREACHED);
    }

    private void blowOffLocation(Entity en, int loc) {
        en.setLocationBlownOff(loc, true);
        for (Mounted mounted : en.getEquipment()) {
            if (mounted.getLocation() == loc) {
                mounted.setMissing(true);
            }
        }
        for (int i = 0; i < en.getNumberOfCriticals(loc); i++) {
            final CriticalSlot cs = en.getCritical(loc, i);
            if (cs != null) {
                cs.setMissing(true);
            }
        }
    }
}

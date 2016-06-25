package megamek.common;

import java.io.InputStream;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import megamek.common.loaders.EntityLoadingException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Class for reading in and parsing MUL XML files.  The MUL xsl is defined in 
 * the docs directory.
 * 
 * @author arlith
 *
 */
public class MULParser {

    public static final String VERSION = "version";
    /**
     * The names of the various elements recognized by this parser.
     */
    private static final String RECORD = "record";
    private static final String SURVIVORS = "survivors";
    private static final String SALVAGE = "salvage";
    private static final String DEVASTATED = "devastated";
    private static final String UNIT = "unit";
    private static final String ENTITY = "entity";
    private static final String PILOT = "pilot";
    private static final String KILLS = "kills";
    private static final String KILL = "kill";
    private static final String LOCATION = "location";
    private static final String ARMOR = "armor";
    private static final String SLOT = "slot";
    private static final String MOVEMENT = "motive";
    private static final String TURRETLOCK = "turretlock";
    private static final String TURRET2LOCK = "turret2lock";
    private static final String SI = "structural";
    private static final String HEAT = "heat";
    private static final String FUEL = "fuel";
    private static final String KF = "KF";
    private static final String SAIL = "sail";
    private static final String AEROCRIT = "acriticals";
    private static final String TANKCRIT = "tcriticals";
    private static final String STABILIZER = "stabilizer";
    private static final String BREACH = "breached";
    private static final String BLOWN_OFF = "blownOff";
    private static final String C3I = "c3iset";
    private static final String C3ILINK = "c3i_link";
    private static final String LINK = "link";
    private static final String RFMG = "rfmg";

    /** 
     * The names of attributes generally associated with Entity tags
     */
    private static final String CHASSIS = "chassis";
    private static final String MODEL = "model";
    private static final String CAMO_CATEGORY = "camoCategory";
    private static final String CAMO_FILENAME = "camoFileName";
    
    /**
     * The names of the attributes recognized by this parser. Not every
     * attribute is valid for every element.
     */
    
    private static final String NAME = "name";
    private static final String SIZE = "size";
    
    private static final String EXT_ID = "externalId";
    private static final String PICKUP_ID = "pickUpId";
    private static final String NICK = "nick";
    private static final String CAT_PORTRAIT = "portraitCat";
    private static final String FILE_PORTRAIT = "portraitFile";
    private static final String GUNNERY = "gunnery";
    private static final String GUNNERYL = "gunneryL";
    private static final String GUNNERYM = "gunneryM";
    private static final String GUNNERYB = "gunneryB";
    private static final String PILOTING = "piloting";
    private static final String ARTILLERY = "artillery";
    private static final String TOUGH = "toughness";
    private static final String INITB = "initB";
    private static final String COMMANDB = "commandB";
    private static final String HITS = "hits";
    private static final String ADVS = "advantages";
    private static final String EDGE = "edge";
    private static final String IMPLANTS = "implants";
    private static final String QUIRKS = "quirks";
    private static final String TROOPER_MISS = "trooperMiss";
    private static final String DRIVER = "driver";
    private static final String COMMANDER = "commander";
    private static final String OFFBOARD = "offboard";
    private static final String OFFBOARD_DISTANCE = "offboard_distance";
    private static final String OFFBOARD_DIRECTION = "offboard_direction";
    private static final String HIDDEN = "hidden";
    private static final String DEPLOYMENT = "deployment";
    private static final String DEPLOYMENT_ZONE = "deploymentZone";
    private static final String NEVER_DEPLOYED = "neverDeployed";
    private static final String VELOCITY = "velocity";
    private static final String ALTITUDE = "altitude";
    private static final String AUTOEJECT = "autoeject";
    private static final String CONDEJECTAMMO = "condejectammo";
    private static final String CONDEJECTENGINE = "condejectengine";
    private static final String CONDEJECTCTDEST = "condejectctdest";
    private static final String CONDEJECTHEADSHOT = "condejectheadshot";
    private static final String EJECTED = "ejected";
    private static final String INDEX = "index";
    private static final String IS_DESTROYED = "isDestroyed";
    private static final String IS_REPAIRABLE = "isRepairable";
    private static final String POINTS = "points";
    private static final String TYPE = "type";
    private static final String SHOTS = "shots";
    private static final String IS_HIT = "isHit";
    private static final String MUNITION = "munition";
    private static final String DIRECTION = "direction";
    private static final String INTEGRITY = "integrity";
    private static final String SINK = "sinks";
    private static final String LEFT = "left";
    private static final String AVIONICS = "avionics";
    private static final String SENSORS = "sensors";
    private static final String ENGINE = "engine";
    private static final String FCS = "fcs";
    private static final String CIC = "cic";
    private static final String LEFT_THRUST = "leftThrust";
    private static final String RIGHT_THRUST = "rightThrust";
    private static final String LIFE_SUPPORT = "lifeSupport";
    private static final String GEAR = "gear";
    private static final String MDAMAGE = "damage";
    private static final String MPENALTY = "penalty";
    private static final String C3MASTERIS = "c3MasterIs";
    private static final String C3UUID = "c3UUID";
    private static final String BOMBS = "bombs";
    private static final String BOMB = "bomb";
    private static final String LOAD = "load";
    private static final String BA_MEA = "modularEquipmentMount";
    private static final String BA_APM = "antiPersonnelMount";
    private static final String BA_APM_MOUNT_NUM = "baAPMMountNum";
    private static final String BA_APM_TYPE_NAME = "baAPMTypeName";
    private static final String BA_MEA_MOUNT_LOC = "baMEAMountLoc";
    private static final String BA_MEA_TYPE_NAME = "baMEATypeName";
    private static final String KILLED = "killed";
    private static final String KILLER = "killer";

    /**
     * Special values recognized by this parser.
     */
    private static final String DEAD = "Dead";
    private static final String NA = "N/A";
    private static final String DESTROYED = "Destroyed";
    private static final String FRONT = "Front";
    private static final String REAR = "Rear";
    private static final String INTERNAL = "Internal";
    private static final String EMPTY = "Empty";
    private static final String SYSTEM = "System";
    
    
    /**
     * Stores all of the  Entity's read in. This is for general use saving and loading to the chat lounge
     */
    Vector<Entity> entities;
    
    /**
     * Stores all of the  surviving Entity's read in. 
     */
    Vector<Entity> survivors;
    
    
    /**
     * Stores all the salvage entities read in 
     */
    Vector<Entity> salvage;
    
    /**
     * Stores all the devastated entities read in 
     */
    Vector<Entity> devastated;
    
    /**
     * Keep a separate list of pilot/crews parsed becasue dismounted pilots may
     * need to be read separately
     */
    private Vector<Crew> pilots;
    
    /**
     * A hashtable containing the names of killed units as the key and the external id
     * of the killer as the value
     */
    private Hashtable<String, String> kills;
    
    
    StringBuffer warning;
    
    public MULParser(){
        warning = new StringBuffer();
        entities = new Vector<Entity>();
        survivors = new Vector<Entity>();
        salvage = new Vector<Entity>();
        devastated = new Vector<Entity>();
        kills = new Hashtable<String, String>();
        pilots = new Vector<Crew>();
    }
    
    public MULParser(InputStream fin){
        this();
        parse(fin);
    }
    
    public void parse(InputStream fin){
        // Reset the warning message.
        warning = new StringBuffer();

        // Clear the entities.
        entities.removeAllElements();
        survivors.removeAllElements();
        salvage.removeAllElements();
        devastated.removeAllElements();
        pilots.removeAllElements();
        kills.clear();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Document xmlDoc = null;

        try {
            // Using factory get an instance of document builder
            DocumentBuilder db = dbf.newDocumentBuilder();

            // Parse using builder to get DOM representation of the XML file
            xmlDoc = db.parse(fin);
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
            warning.append("Error parsing MUL file!\n");
            return;
        }
        
        Element element = xmlDoc.getDocumentElement();

        // Get rid of empty text nodes and adjacent text nodes...
        // Stupid weird parsing of XML. At least this cleans it up.
        element.normalize();

        String version = element.getAttribute(VERSION);
        if (version.equals("")){
            warning.append("Warning: No version specified, correct parsing " +
                    "not guaranteed!\n");
        }
        
        String nodeName = element.getNodeName();
        if(nodeName.equalsIgnoreCase(RECORD)) {
            parseRecord(element);
        } else if (nodeName.equalsIgnoreCase(UNIT)){
            parseUnit(element, entities);
        } else if (nodeName.equalsIgnoreCase(ENTITY)){
            parseEntity(element, entities);
        } else {
            warning.append("Error: root element isn't a Record, Unit, or Entity tag! " +
                    "Nothing to parse!\n");
        }
    }
    
    /**
     * Parse a Unit tag.  Unit tags will contain a list of Entity tags.
     * @param unitNode
     */
    private void parseRecord(Element unitNode){
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
                if (nodeName.equalsIgnoreCase(UNIT)){
                    parseUnit((Element)currNode, entities);
                } else if (nodeName.equalsIgnoreCase(SURVIVORS)){
                    parseUnit((Element)currNode, survivors);
                } else if (nodeName.equalsIgnoreCase(SALVAGE)){
                    parseUnit((Element)currNode, salvage);
                } else if (nodeName.equalsIgnoreCase(DEVASTATED)){
                    parseUnit((Element)currNode, devastated);
                } else if (nodeName.equalsIgnoreCase(KILLS)){
                    parseKills((Element)currNode);
                } else if (nodeName.equalsIgnoreCase(ENTITY)){
                    parseUnit((Element)currNode, entities);
                } else if (nodeName.equalsIgnoreCase(PILOT)){
                    parsePilot((Element)currNode);
                } 
            } else {
                continue;
            }
        }
    }
    
    /**
     * Parse a Unit tag.  Unit tags will contain a list of Entity tags.
     * @param unitNode
     * @param Vector<Entity> list - which list to add found entities too
     */
    private void parseUnit(Element unitNode, Vector<Entity> list){
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
                if (nodeName.equalsIgnoreCase(ENTITY)){
                    parseEntity((Element)currNode, list);
                } else if (nodeName.equalsIgnoreCase(PILOT)){
                    parsePilot((Element)currNode);
                } 
            } else {
                continue;
            }
        }
    }
    
    /**
     * Parse a kills tag.  
     * @param unitNode
     */
    private void parseKills(Element killNode){
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
                if (nodeName.equalsIgnoreCase(KILL)){
                    String killed =  ((Element)currNode).getAttribute(KILLED);
                    String killer = ((Element)currNode).getAttribute(KILLER);
                    if(null != killed && null != killer && !killed.isEmpty() && !killer.isEmpty()) {
                        kills.put(killed, killer);
                    }
                } 
            } else {
                continue;
            }
        }
    }
    
    /**
     * Parse an Entity tag.  Entity tags will have a number of attributes such
     * as model, chassis, type, etc.  They should also have a child Pilot tag
     * and they may also contain some number of location tags.
     * 
     * @param entityNode
     * @param Vector<Entity> list - which list to add found entities too
     */
    private void parseEntity(Element entityNode, Vector<Entity> list) {
        Entity entity = null;
        
        // We need to get a new Entity, use the chassis and model to create one
        String chassis =  entityNode.getAttribute(CHASSIS);
        String model = entityNode.getAttribute(MODEL);

        // Create a new entity
        entity = getEntity(chassis, model);
        
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
                Element currEle = (Element)currNode;
                String nodeName = currNode.getNodeName();
                if (nodeName.equalsIgnoreCase(PILOT)){
                    parsePilot(currEle, entity);
                } else if (nodeName.equalsIgnoreCase(LOCATION)){
                    parseLocation(currEle, entity);
                } else if (nodeName.equalsIgnoreCase(MOVEMENT)){
                    parseMovement(currEle, entity);
                } else if (nodeName.equalsIgnoreCase(TURRETLOCK)){
                    parseTurretLock(currEle, entity);
                } else if (nodeName.equalsIgnoreCase(TURRET2LOCK)){
                    parseTurret2Lock(currEle, entity);
                } else if (nodeName.equalsIgnoreCase(SI)){
                    parseSI(currEle, entity);
                } else if (nodeName.equalsIgnoreCase(HEAT)){
                    parseHeat(currEle, entity);
                } else if (nodeName.equalsIgnoreCase(FUEL)){
                    parseFuel(currEle, entity);
                } else if (nodeName.equalsIgnoreCase(KF)){
                    parseKF(currEle, entity);
                } else if (nodeName.equalsIgnoreCase(SAIL)){
                    parseSail(currEle, entity);
                } else if (nodeName.equalsIgnoreCase(AEROCRIT)){
                    parseAeroCrit(currEle, entity);
                } else if (nodeName.equalsIgnoreCase(TANKCRIT)){
                    parseTankCrit(currEle, entity);
                } else if (nodeName.equalsIgnoreCase(BOMBS)){
                    parseBombs(currEle, entity);
                } else if (nodeName.equalsIgnoreCase(C3I)){
                    parseC3I(currEle, entity);
                } else if (nodeName.equalsIgnoreCase(BA_MEA)){
                    parseBAMEA(currEle, entity);
                } else if (nodeName.equalsIgnoreCase(BA_APM)){
                    parseBAAPM(currEle, entity);
                }
            } else {
                continue;
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
    private Entity getEntity(String chassis, String model){
        Entity newEntity = null;
        
        //first check for ejected mechwarriors and vee crews
        if(chassis.equals(EjectedCrew.VEE_EJECT_NAME)) {
            return new EjectedCrew();
        } else if(chassis.equals(EjectedCrew.MW_EJECT_NAME)) {
            return new MechWarrior();
        }
        
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
                    newEntity = new MechFileParser(ms.getSourceFile(),
                            ms.getEntryName()).getEntity();
                } catch (EntityLoadingException excep) {
                    excep.printStackTrace(System.err);
                    warning.append("Unable to load mech: ")
                            .append(ms.getSourceFile()).append(": ")
                            .append(ms.getEntryName()).append(": ")
                            .append(excep.getMessage());
                }
            } // End found-MechSummary
        }
        return newEntity;
    }
    
    /**
     * An Entity tag can define numerous attributes for the <code>Entity</code>,
     * check and set all of the relevent attributes.
     * 
     * @param entity    The newly created Entity that we are setting state for
     * @param entityTag The Entity tag that defines the attributes
     */
    private void parseEntityAttributes(Entity entity, Element entityTag){
        // commander
        boolean commander = 
                Boolean.parseBoolean(entityTag.getAttribute(COMMANDER));
        entity.setCommander(commander);

        // hidden
        try {
            boolean isHidden =
                    Boolean.parseBoolean(entityTag.getAttribute(HIDDEN));
            entity.setHidden(isHidden);
        } catch (Exception e) {
            entity.setHidden(false);
        }

        // deploy offboard
        try {
            boolean offBoard =
                    Boolean.parseBoolean(entityTag.getAttribute(OFFBOARD));
            if (offBoard) {
                int distance = Integer.parseInt(entityTag
                        .getAttribute(OFFBOARD_DISTANCE));
                OffBoardDirection dir = OffBoardDirection.getDirection(Integer
                        .parseInt(entityTag.getAttribute(OFFBOARD_DIRECTION)));
                entity.setOffBoard(distance, dir);
            }
        } catch (Exception e) {
        }

        // deployment round
        try {
            int deployround = 
                    Integer.parseInt(entityTag.getAttribute(DEPLOYMENT));
            entity.setDeployRound(deployround);
        } catch (Exception e) {
            entity.setDeployRound(0);
        }
        
        // deployment zone
        try {
            int deployZone = 
                    Integer.parseInt(entityTag.getAttribute(DEPLOYMENT_ZONE));
            entity.setStartingPos(deployZone);
        } catch (Exception e) {
            entity.setDeployRound(Board.START_NONE);
        }
        
        
        
        // Was never deployed
        try {
            String ndeploy = entityTag.getAttribute(NEVER_DEPLOYED);
            boolean wasNeverDeployed =
                    Boolean.parseBoolean(entityTag.getAttribute(NEVER_DEPLOYED));
            if(null == ndeploy || ndeploy.isEmpty()) {
                //this will default to false above, but we want it to default to true
                wasNeverDeployed = true;
            }            
            entity.setNeverDeployed(wasNeverDeployed);
        } catch (Exception e) {
            entity.setNeverDeployed(true);
        }
        
        if (entity instanceof Aero){
            String velString = entityTag.getAttribute(VELOCITY);
            String altString = entityTag.getAttribute(ALTITUDE);
            
            Aero a = (Aero) entity;
            if (velString.length() > 0){
                int velocity = Integer.parseInt(velString);
                a.setCurrentVelocity(velocity);
                a.setNextVelocity(velocity);
            }
            if (altString.length() > 0){
                int altitude = Integer.parseInt(altString);
                if (altitude <= 0) {
                    a.land();
                } else {
                    a.liftOff(altitude);
                }    
            }
        }

        // Camo
        // Must be a null, and not an empty string, if it isn't being used. - Dylan 2014-04-04
        entity.setCamoCategory(entityTag.getAttribute(CAMO_CATEGORY).equals("") ? null : entityTag.getAttribute(CAMO_CATEGORY));
        entity.setCamoFileName(entityTag.getAttribute(CAMO_FILENAME).equals("") ? null : entityTag.getAttribute(CAMO_FILENAME));

        // external id
        String extId = entityTag.getAttribute(EXT_ID);
        if ((null == extId) || (extId.length() == 0)) {
            extId = "-1";
        }
        entity.setExternalIdAsString(extId);

        // external id
        if(entity instanceof MechWarrior) {
            String pickUpId = entityTag.getAttribute(PICKUP_ID);
            if ((null == pickUpId) || (pickUpId.length() == 0)) {
                pickUpId = "-1";
            }
            ((MechWarrior)entity).setPickedUpByExternalId(pickUpId);
        }

        
        // quirks
        String quirks = entityTag.getAttribute(QUIRKS);
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
        String c3masteris = entityTag.getAttribute(C3MASTERIS);
        if (c3masteris.length() > 0) {
            entity.setC3MasterIsUUIDAsString(c3masteris);
        }
        String c3uuid = entityTag.getAttribute(C3UUID);
        if (c3uuid.length() > 0) {
            entity.setC3UUIDAsString(c3uuid);
        }
    }
    
    /**
     * Convenience function that calls <code>parsePilot</code> with a null 
     * Entity.
     * 
     * @param pilotNode
     */
    private void parsePilot(Element pilotNode){
        parsePilot(pilotNode, null);
    }
    
    /**
     * Given a Pilot tag, read the attributes and create a new <code>Crew</code>
     * instance.  If a non-null <code>Entity</code> is passed, the new crew will
     * be set as the crew for the given <code>Entity</code>.
     * 
     * @param pilotNode The Pilot tag to reate a <code>Crew</code> from
     * @param entity    If non-null, the new <code>Crew</code> will be set as
     *                  the crew of this <code>Entity</code> 
     */
    private void parsePilot(Element pilotNode, Entity entity){

        // Look for the element's attributes.
        String pilotName = pilotNode.getAttribute(NAME);
        String pilotSize = pilotNode.getAttribute(SIZE);
        String pilotNickname = pilotNode.getAttribute(NICK);
        String gunnery = pilotNode.getAttribute(GUNNERY);
        String gunneryL = pilotNode.getAttribute(GUNNERYL);
        String gunneryM = pilotNode.getAttribute(GUNNERYM);
        String gunneryB = pilotNode.getAttribute(GUNNERYB);
        String piloting = pilotNode.getAttribute(PILOTING);
        String artillery = pilotNode.getAttribute(ARTILLERY);
        String tough = pilotNode.getAttribute(TOUGH);
        String initB = pilotNode.getAttribute(INITB);
        String commandB = pilotNode.getAttribute(COMMANDB);
        String hits = pilotNode.getAttribute(HITS);
        String advantages = pilotNode.getAttribute(ADVS);
        String edge = pilotNode.getAttribute(EDGE);
        String implants = pilotNode.getAttribute(IMPLANTS);
        String autoeject = pilotNode.getAttribute(AUTOEJECT);
        String condejectammo = pilotNode.getAttribute(CONDEJECTAMMO);
        String condejectengine = pilotNode.getAttribute(CONDEJECTENGINE);
        String condejectctdest = pilotNode.getAttribute(CONDEJECTCTDEST);
        String condejectheadshot = pilotNode.getAttribute(CONDEJECTHEADSHOT);
        String ejected = pilotNode.getAttribute(EJECTED);
        String extId = pilotNode.getAttribute(EXT_ID);
        String portraitCategory = pilotNode.getAttribute(CAT_PORTRAIT);
        String portraitFile = pilotNode.getAttribute(FILE_PORTRAIT);

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
            if ((gunVal < 0) 
                    || (gunVal > Crew.MAX_SKILL)) {
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
            if ((pilotVal < 0) 
                    || (pilotVal > Crew.MAX_SKILL)) {
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
                if ((gunneryLVal < 0) 
                        || (gunneryLVal > Crew.MAX_SKILL)) {
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
                if ((gunneryMVal < 0) 
                        || (gunneryMVal > Crew.MAX_SKILL)) {
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
                if ((gunneryBVal < 0) 
                        || (gunneryBVal > Crew.MAX_SKILL)) {
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
                if ((artVal < 0) 
                        || (artVal > Crew.MAX_SKILL)) {
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
            
            if (pilotSize != null && pilotSize.length() > 0) {
                int crewSize = 1;
                try {
                    crewSize = Integer.parseInt(pilotSize);
                } catch (NumberFormatException e) {
                    // Do nothing, this field isn't required
                }
                crew.setSize(crewSize);
            } else if (pilotSize != null && pilotSize.equals("")) {
                crew.setSize(Compute.getFullCrewSize(entity));
            }

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
            if (hits.length() > 0) {
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

            if (ejected.length() > 0) {
                crew.setEjected(Boolean.parseBoolean(ejected));
            }

            if ((null != extId) && (extId.length() > 0)) {
                crew.setExternalIdAsString(extId);
            }           

            pilots.add(crew);
            if (null != entity) {
                // Set the crew for this entity.
                entity.setCrew(crew);

                if (autoeject.length() > 0) {
                    if (autoeject.equals("true")) {
                        ((Mech) entity).setAutoEject(true);
                    } else {
                        ((Mech) entity).setAutoEject(false);
                    }
                }
                if (condejectammo.length() > 0) {
                    if (condejectammo.equals("true")) {
                        ((Mech) entity).setCondEjectAmmo(true);
                    } else {
                        ((Mech) entity).setCondEjectAmmo(false);
                    }
                }
                if (condejectengine.length() > 0) {
                    if (condejectengine.equals("true")) {
                        ((Mech) entity).setCondEjectEngine(true);
                    } else {
                        ((Mech) entity).setCondEjectEngine(false);
                    }
                }
                if (condejectctdest.length() > 0) {
                    if (condejectctdest.equals("true")) {
                        ((Mech) entity).setCondEjectCTDest(true);
                    } else {
                        ((Mech) entity).setCondEjectCTDest(false);
                    }
                }
                if (condejectheadshot.length() > 0) {
                    if (condejectheadshot.equals("true")) {
                        ((Mech) entity).setCondEjectHeadshot(true);
                    } else {
                        ((Mech) entity).setCondEjectHeadshot(false);
                    }
                }
            }
        } // End have-required-fields      
    }
    
    /**
     * Parse a location tag and update the given <code>Entity</code> based on 
     * the contents.
     * 
     * @param locationTag
     * @param entity
     */
    private void parseLocation(Element locationTag, Entity entity){
        // Look for the element's attributes.
        String index = locationTag.getAttribute(INDEX);
        String destroyed = locationTag.getAttribute(IS_DESTROYED);

        int loc;
        // Some units, like tanks and protos, keep track as Ammo slots as N/A
        // Since they don't have slot indices, they are accessed in order so
        // we keep track of the number of ammo slots processed for a loc
        int locAmmoCount = 0;
        // Did we find required attributes?
        if ((index == null) || (index.length() == 0)) {
            warning.append("Could not find index for location.\n");
            return;
        } else {
            // Try to get a good index value.
            loc = -1;
            try {
                loc = Integer.parseInt(index);
            } catch (NumberFormatException excep) {
                // Handled by the next if test.
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
                } catch (Throwable excep) {
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
                Element currEle = (Element)currNode;
                String nodeName = currNode.getNodeName();
                if (nodeName.equalsIgnoreCase(ARMOR)){
                    parseArmor(currEle, entity, loc);
                } else if (nodeName.equalsIgnoreCase(BREACH)){
                    breachLocation(entity, loc);
                } else if (nodeName.equalsIgnoreCase(BLOWN_OFF)){
                    blowOffLocation(entity, loc);
                } else if (nodeName.equalsIgnoreCase(SLOT)){
                    locAmmoCount = parseSlot(currEle, entity, loc, locAmmoCount);
                } else if (nodeName.equalsIgnoreCase(STABILIZER)){
                    String hit = currEle.getAttribute(IS_HIT);
                    if (!hit.equals("")) {
                        ((Tank) entity).setStabiliserHit(loc);
                    }
                }                    
            } else {
                continue;
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
    private void parseArmor(Element armorTag, Entity entity, int loc){
     // Look for the element's attributes.
        String points = armorTag.getAttribute(POINTS);
        String type = armorTag.getAttribute(TYPE);

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
            if ((type.length() == 0) || type.equals(FRONT)) {
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
                            .append(" points of internal structure for " +
                                    "location: ")
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
        }
    }
    
    /**
     * Parse a slot tag for the given Entity and location.
     * 
     * @param slotTag
     * @param entity
     * @param loc
     */
    private int parseSlot(Element slotTag, Entity entity, int loc,
            int locAmmoCount) {
        // Look for the element's attributes.
        String index = slotTag.getAttribute(INDEX);
        String type = slotTag.getAttribute(TYPE);
        // String rear = slotTag.getAttribute( IS_REAR ); // is never read.
        String shots = slotTag.getAttribute(SHOTS);
        String hit = slotTag.getAttribute(IS_HIT);
        String destroyed = slotTag.getAttribute(IS_DESTROYED);
        String repairable = (slotTag.getAttribute(IS_REPAIRABLE).equals("") ? "true" : slotTag.getAttribute(IS_REPAIRABLE));
        String munition = slotTag.getAttribute(MUNITION);
        String quirks = slotTag.getAttribute(QUIRKS);
        String trooperMiss = slotTag.getAttribute(TROOPER_MISS);
        String rfmg = slotTag.getAttribute(RFMG);

        // Did we find required attributes?
        if ((index == null) || (index.length() == 0)) {
            warning.append("Could not find index for slot.\n");
            return locAmmoCount;
        } else if ((type == null) || (type.length() == 0)) {
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
            if (index.equals(NA)) {
                indexVal = IArmorState.ARMOR_NA;

                // Protomechs only have system slots, 
                // so we have to handle the ammo specially.
                if (entity instanceof Protomech) {
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

            // Did we get it?
            if (slot == null) {
                if (!type.equals(EMPTY)) {
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
                
                // trooper missing equipment
                if ((null != trooperMiss) && (trooperMiss.trim().length() > 0)) {
                    StringTokenizer st = new StringTokenizer(trooperMiss,
                            "::");
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
                if (munition.length() > 0) {
                    // Retrieve munition by name.
                    EquipmentType munType = EquipmentType.get(munition);

                    // Make sure munition is a type of ammo.
                    if (munType instanceof AmmoType) {
                        // Change to the saved munition type.
                        mounted.getLinked().changeAmmoType(
                                (AmmoType) munType);
                    } else {
                        // Bad XML equipment.
                        warning.append("XML file expects")
                                .append(" ammo for munition argument of")
                                .append(" slot tag.\n");
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
     * Parse a movement tag for the given <code>Entity</code>.
     * 
     * @param movementTag
     * @param entity
     */
    private void parseMovement(Element movementTag, Entity entity){
        String value = movementTag.getAttribute(MDAMAGE);
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
        value = movementTag.getAttribute(MPENALTY);
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
    private void parseTurretLock(Element turretLockTag, Entity entity){
        String value = turretLockTag.getAttribute(DIRECTION);
        try {
            int turDir = Integer.parseInt(value);
            ((Tank) entity).setSecondaryFacing(turDir);
            ((Tank) entity).lockTurret(((Tank)entity).getLocTurret());
        } catch (Exception e) {
            System.err.println(e);
            e.printStackTrace();
            warning.append("Invalid turret lock direction value in " +
                    "movement tag.\n");
        }
    }
    
    /**
     * Parse a turret2lock tag for the given <code>Entity</code>.
     *  
     * @param turret2LockTag
     * @param entity
     */
    private void parseTurret2Lock(Element turret2LockTag, Entity entity){
        String value = turret2LockTag.getAttribute(DIRECTION);
        try {
            int turDir = Integer.parseInt(value);
            ((Tank) entity).setDualTurretOffset(turDir);
            ((Tank) entity).lockTurret(((Tank)entity).getLocTurret2());
        } catch (Exception e) {
            System.err.println(e);
            e.printStackTrace();
            warning.append("Invalid turret2 lock direction value in " +
                    "movement tag.\n");
        }
    }
    
    /**
     * Parse a si tag for the given <code>Entity</code>.
     *  
     * @param turret2LockTag
     * @param entity
     */
    private void parseSI(Element siTag, Entity entity){
        String value = siTag.getAttribute(INTEGRITY);
        try {
            int newSI = Integer.parseInt(value);
            ((Aero) entity).setSI(newSI);
        } catch (Exception e) {
            warning.append("Invalid SI value in structural integrity tag.\n");
        }
    }

    /**
     * Parse a heat tag for the given <code>Entity</code>.
     *  
     * @param turret2LockTag
     * @param entity
     */
    private void parseHeat(Element heatTag, Entity entity){
        String value = heatTag.getAttribute(SINK);
        try {
            int newSinks = Integer.parseInt(value);
            ((Aero) entity).setHeatSinks(newSinks);
        } catch (Exception e) {
            warning.append("Invalid heat sink value in heat sink tag.\n");
        }
    }

    /**
     * Parse a fuel tag for the given <code>Entity</code>.
     *  
     * @param turret2LockTag
     * @param entity
     */
    private void parseFuel(Element fuelTag, Entity entity){
        String value = fuelTag.getAttribute(LEFT);
        try {
            int newFuel = Integer.parseInt(value);
            ((Aero) entity).setFuel(newFuel);
        } catch (Exception e) {
            warning.append("Invalid fuel value in fuel tag.\n");
        }
    }

    /**
     * Parse a kf tag for the given <code>Entity</code>.
     *  
     * @param turret2LockTag
     * @param entity
     */
    private void parseKF(Element kfTag, Entity entity){
        String value = kfTag.getAttribute(INTEGRITY);
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
     * @param turret2LockTag
     * @param entity
     */
    private void parseSail(Element sailTag, Entity entity){
        String value = sailTag.getAttribute(INTEGRITY);
        try {
            int newIntegrity = Integer.parseInt(value);
            ((Jumpship) entity).setSailIntegrity(newIntegrity);
        } catch (Exception e) {
            warning.append("Invalid sail integrity value in sail " +
                    "integrity tag.\n");
        }
    }
    
    /**
     * Parse an aeroCrit tag for the given <code>Entity</code>.
     * 
     * @param aeroCritTag
     * @param entity
     */
    private void parseAeroCrit(Element aeroCritTag, Entity entity){
        String avionics = aeroCritTag.getAttribute(AVIONICS);
        String sensors = aeroCritTag.getAttribute(SENSORS);
        String engine = aeroCritTag.getAttribute(ENGINE);
        String fcs = aeroCritTag.getAttribute(FCS);
        String cic = aeroCritTag.getAttribute(CIC);
        String leftThrust = aeroCritTag.getAttribute(LEFT_THRUST);
        String rightThrust = aeroCritTag.getAttribute(RIGHT_THRUST);
        String lifeSupport = aeroCritTag.getAttribute(LIFE_SUPPORT);
        String gear = aeroCritTag.getAttribute(GEAR);

        Aero a = (Aero) entity;

        if (avionics.length() > 0) {
            a.setAvionicsHits(Integer.parseInt(avionics));
        }

        if (sensors.length() > 0) {
            a.setSensorHits(Integer.parseInt(sensors));
        }

        if (engine.length() > 0) {
            a.setEngineHits(Integer.parseInt(engine));
        }

        if (fcs.length() > 0) {
            a.setFCSHits(Integer.parseInt(fcs));
        }

        if (cic.length() > 0) {
            a.setCICHits(Integer.parseInt(cic));
        }

        if (leftThrust.length() > 0) {
            a.setLeftThrustHits(Integer.parseInt(leftThrust));
        }

        if (rightThrust.length() > 0) {
            a.setRightThrustHits(Integer.parseInt(rightThrust));
        }

        if (lifeSupport.length() > 0) {
            a.setLifeSupport(false);
        }

        if (gear.length() > 0) {
            a.setGearHit(true);
        }
    }
    
    /**
     * Parse a tankCrit tag for the given <code>Entity</code>.
     * 
     * @param tankCrit
     * @param entity
     */
    private void parseTankCrit(Element tankCrit, Entity entity){
        String sensors = tankCrit.getAttribute(SENSORS);
        String engine = tankCrit.getAttribute(ENGINE);
        String driver = tankCrit.getAttribute(DRIVER);
        String commander = tankCrit.getAttribute(COMMANDER);

        Tank t = (Tank) entity;

        if (sensors.length() > 0) {
            t.setSensorHits(Integer.parseInt(sensors));
        }

        if (engine.equalsIgnoreCase("hit")) {
            t.engineHit();
            t.applyDamage();
        }

        if (driver.equalsIgnoreCase("hit")) {
            t.setDriverHit(true);
        }

        if (commander.equalsIgnoreCase("hit")) {
            t.setCommanderHit(true);
        }
    }
    
    /**
     * Parse a bombs tag for the given <code>Entity</code>.
     * 
     * @param bombsTag
     * @param entity
     */
    private void parseBombs(Element bombsTag, Entity entity){
        if (!(entity instanceof Aero)) {
            warning.append("Found a bomb but Entity is not a Fighter.\n");
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
                Element currEle = (Element)currNode;
                String nodeName = currNode.getNodeName();
                if (nodeName.equalsIgnoreCase(BOMB)){
                    int[] bombChoices = ((Aero) entity).getBombChoices();
                    String type = currEle.getAttribute(TYPE);
                    String load = currEle.getAttribute(LOAD);
                    if (type.length() > 0 && load.length() > 0){
                        bombChoices[BombType.getBombTypeFromInternalName(type)] 
                                += Integer.parseInt(load);
                        ((Aero) entity).setBombChoices(bombChoices);
                    }
                }
            } else {
                continue;
            }
        }
    }
    
    /**
     * Parse a c3i tag for the given <code>Entity</code>.
     * 
     * @param c3iTag
     * @param entity
     */
    private void parseC3I(Element c3iTag, Entity entity){
        // Deal with any child nodes
        NodeList nl = c3iTag.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node currNode = nl.item(i);

            if (currNode.getParentNode() != c3iTag) {
                continue;
            }
            int nodeType = currNode.getNodeType();
            if (nodeType == Node.ELEMENT_NODE) {
                Element currEle = (Element)currNode;
                String nodeName = currNode.getNodeName();
                if (nodeName.equalsIgnoreCase(C3ILINK)){
                    String link = currEle.getAttribute(LINK);
                    int pos = entity.getFreeC3iUUID();
                    if ((link.length() > 0) && (pos != -1)) {
                        System.out.println("Loading C3i UUID " + pos + 
                                ": " + link);
                        entity.setC3iNextUUIDAsString(pos, link);
                    }
                }
            } else {
                continue;
            }
        }
    }
    
    /**
     * Parase a modularEquipmentMount tag for the supplied <code>Entity</code>.
     * 
     * @param meaTag
     * @param entity
     */
    private void parseBAMEA(Element meaTag, Entity entity){
        if (!(entity instanceof BattleArmor)){
            warning.append("Found a BA MEA tag but Entity is not " +
                    "BattleArmor!\n");
            return;
        }
        
        String meaMountLocString = meaTag.getAttribute(BA_MEA_MOUNT_LOC);
        String manipTypeName = meaTag.getAttribute(BA_MEA_TYPE_NAME);
        
        // Make sure we got a mount number
        if (meaMountLocString.length() == 0){
            warning.append("antiPersonnelMount tag does not specify " +
                    "a baMeaMountLoc!\n");
            return;
        }
        
        // We could have no mounted manipulator
        EquipmentType manipType = null;
        if (manipTypeName.length() > 0){
            manipType = EquipmentType.get(manipTypeName);
        }
        
        // Find the Mounted instance for the MEA 
        Mounted mountedManip = null;
        int meaMountLoc = Integer.parseInt(meaMountLocString);
        boolean foundMea = false;
        for (Mounted m : entity.getEquipment()){
            if (m.getBaMountLoc() != meaMountLoc){
                continue;
            }
            if (m.getType().hasFlag(MiscType.F_BA_MEA)){
                foundMea = true;
                break;
            }                
        }
        if (!foundMea){
            warning.append("No modular equipment mount found in specified " +
                    "location! Location: " + meaMountLoc + "\n");
            return;
        }
        if (meaMountLoc == BattleArmor.MOUNT_LOC_LARM){
            mountedManip = ((BattleArmor)entity).getLeftManipulator();
        } else if (meaMountLoc == BattleArmor.MOUNT_LOC_RARM){
            mountedManip = ((BattleArmor)entity).getRightManipulator();
        }

        if (mountedManip != null){
            entity.getEquipment().remove(mountedManip);
            entity.getMisc().remove(mountedManip);
        }            
        
        // Was no manipulator selected?
        if (manipType == null){
            return;
        }
            
        // Add the newly mounted maniplator
        try{
            int baMountLoc = mountedManip.getBaMountLoc();
            mountedManip = entity.addEquipment(manipType, 
                    mountedManip.getLocation());
            mountedManip.setBaMountLoc(baMountLoc);
        } catch (LocationFullException ex){
            // This shouldn't happen for BA...
            ex.printStackTrace();
        }
    }
    
    /**
     * Parase a antiPersonnelMount tag for the supplied <code>Entity</code>.
     * 
     * @param meaTag
     * @param entity
     */
    private void parseBAAPM(Element apmTag, Entity entity){
        if (!(entity instanceof BattleArmor)){
            warning.append("Found a BA APM tag but Entity is not " +
                    "BattleArmor!\n");
            return;
        }
        
        String mountNumber = apmTag.getAttribute(BA_APM_MOUNT_NUM);
        String apTypeName = apmTag.getAttribute(BA_APM_TYPE_NAME);
        
        // Make sure we got a mount number
        if (mountNumber.length() == 0){
            warning.append("antiPersonnelMount tag does not specify " +
                    "a baAPMountNum!\n");
            return;
        }
        
        Mounted apMount = entity.getEquipment(Integer.parseInt(mountNumber));
        // We may mount no AP weapon
        EquipmentType apType = null;
        if (apTypeName.length() > 0){
            apType = EquipmentType.get(apTypeName);
        }
        
        // Remove any currently mounted AP weapon
        if (apMount.getLinked() != null 
                && apMount.getLinked().getType() != apType){
            Mounted apWeapon = apMount.getLinked();
            entity.getEquipment().remove(apWeapon);
            entity.getWeaponList().remove(apWeapon);
            entity.getTotalWeaponList().remove(apWeapon);
            // We need to make sure that the weapon has been removed
            //  from the criticals, otherwise it can cause issues
            for (int loc = 0; loc < entity.locations(); loc++) {
                for (int c = 0; 
                        c < entity.getNumberOfCriticals(loc); c++) {
                    CriticalSlot crit = entity.getCritical(loc, c);
                    if (crit != null && crit.getMount() != null 
                            && crit.getMount().equals(apWeapon)) {
                        entity.setCritical(loc, c, null);
                    }
                }
            }
        }
        
        // Did the selection not change, or no weapon was selected
        if ((apMount.getLinked() != null 
                && apMount.getLinked().getType() == apType)
                || (apType == null)){
            return;
        }
            
        // Add the newly mounted weapon
        try{
            Mounted newWeap =  entity.addEquipment(apType, 
                    apMount.getLocation());
            apMount.setLinked(newWeap);
            newWeap.setLinked(apMount);
            newWeap.setAPMMounted(true);
        } catch (LocationFullException ex){
            // This shouldn't happen for BA...
            ex.printStackTrace();
        }
        
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
     * Returns a list of all of the  Entity's parsed from the input, should be
     * called after <code>parse</code>. This is for entities that we want to be loaded
     * into the chat lounge, so functional
     * @return
     */
    public Vector<Entity> getEntities(){
        Vector<Entity> toReturn = entities;
        for(Entity e : survivors) {
            if(e instanceof EjectedCrew) {
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
    public Vector<Entity> getSurvivors(){
        return survivors;
    }
    
    /**
     * Returns a list of all of the salvaged Entity's parsed from the input, should be
     * called after <code>parse</code>.
     * @return
     */
    public Vector<Entity> getSalvage(){
        return salvage;
    }
    
    /**
     * Returns a list of all of the devastated Entity's parsed from the input, should be
     * called after <code>parse</code>.
     * @return
     */
    public Vector<Entity> getDevastated(){
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

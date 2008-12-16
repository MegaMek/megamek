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
     * @param   en - the <code>Entity</code> whose location is destroyed.
     * @param   loc - the <code>int</code> index of the destroyed location.
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
        if (en.getDependentLocation(loc) != Entity.LOC_NONE) {
            destroyLocation(en, en.getDependentLocation(loc));
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
    public static final String MOVEMENT = "movement";
    public static final String TURRETLOCK = "turretlock";
    public static final String  SI = "structural";
    public static final String  HEAT = "Heat";
    public static final String  FUEL = "fuel";
    public static final String  KF = "KF";
    public static final String  SAIL = "sail";
    public static final String  AEROCRIT = "acriticals";
 

    /**
     * The names of the attributes recognized by this parser. Not every
     * attribute is valid for every element.
     */
    public static final String CHASSIS = "chassis";
    public static final String MODEL = "model";
    public static final String NAME = "name";
    public static final String GUNNERY = "gunnery";
    public static final String GUNNERYL = "gunneryL";
    public static final String GUNNERYM = "gunneryM";
    public static final String GUNNERYB = "gunneryB";
    public static final String PILOTING = "piloting";
    public static final String INITB = "initB";
    public static final String COMMANDB = "commandB";
    public static final String HITS = "hits";
    public static final String ADVS = "advantages";
    public static final String IMPLANTS = "implants";
    public static final String AUTOEJECT = "autoeject";
    public static final String INDEX = "index";
    public static final String IS_DESTROYED = "isDestroyed";
    public static final String POINTS = "points";
    public static final String TYPE = "type";
    public static final String IS_REAR = "isRear";
    public static final String SHOTS = "shots";
    public static final String IS_HIT = "isHit";
    public static final String MUNITION = "munition";
    public static final String SPEED = "speed";
    public static final String DIRECTION = "direction";
    public static final String  INTEGRITY = "integrity";
    public static final String  SINK = "sinks";
    public static final String  LEFT = "left";
    public static final String  AVIONICS = "avionics";
    public static final String  SENSORS = "sensors";
    public static final String  ENGINE = "engine";
    public static final String  FCS = "fcs";
    public static final String  CIC = "cic";
    public static final String  LEFT_THRUST = "leftThrust";
    public static final String  RIGHT_THRUST = "rightThrust";
    public static final String  LIFE_SUPPORT = "lifeSupport";
    public static final String  GEAR = "gear";

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
     * No <code>Entity</code>s or warning message are available if the
     * default constructor is used.
     */
    public XMLStreamParser() { /* do nothing */
    }

    /**
     * Parse the indicated XML stream. Any warning message or
     * <code>Entity</code>s from a previously parsed stream will be
     * discarded.
     * 
     * @param input - the <code>InputStream</code> to be parsed.
     * @exception ParseException is thrown if a fatal error occurs during
     *                parsing. Typically, this only occurs when the XML is not
     *                well-formed.
     */
    public void parse(InputStream input) throws ParseException {
        // Reset the warning message.
        this.warning = new StringBuffer();

        // Clear the entities.
        this.entities.removeAllElements();

        // Parse the input stream.
        this.inStream = input;
        this.parser.parseXML(this);
    }

    /**
     * Construct an object and parse the XML stream. Any warning message or
     * <code>Entity</code>s from a previously parsed stream will be
     * discarded.
     * 
     * @param input - the <code>InputStream</code> to be parsed.
     * @exception ParseException is thrown if a fatal warning occurs during
     *                parsing. Typically, this only occurs when the XML is not
     *                well-formed.
     */
    public XMLStreamParser(InputStream input) throws ParseException {
        this.parse(input);
    }

    /**
     * Determine if unexpected XML entities were encountered during parsing.
     * 
     * @return <code>true</code> if a non-fatal warning occured.
     */
    public boolean hasWarningMessage() {
        return (this.warning.length() > 0);
    }

    /**
     * Get the warning message from the last parse.
     * 
     * @return The <code>String</code> warning message from the last parse. If
     *         there is no warning message, then an <code>null</code> value is
     *         returned.
     */
    public String getWarningMessage() {
        if (this.warning.length() > 0) {
            return this.warning.toString();
        }
        return null;
    }

    /**
     * Get any <code>Entity</code>s parsed from the last input stream.
     * Entities may have been parsed out of the stream, even if errors were
     * encountered.
     * 
     * @return A <code>Vector</code> containing <code>Entity</code>s parsed
     *         from the stream. This <code>Vector</code> may be empty, but it
     *         will never be <code>null</code>.
     */
    public Vector<Entity> getEntities() {
        // ASSUMPTION : it is safe to return a modifiable reference to the
        // vector. If assumption is wrong, clone the vector.
        return this.entities;
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

    @SuppressWarnings("unchecked")
    public void recordElementStart(String name, Hashtable attr)
            throws ParseException {

        // TODO: handle template files.

        // What kind of element have we started?
        if (name.equals(UNIT)) {

            // Are we in the middle of parsing an Entity?
            if (this.entity != null) {
                this.warning.append("Found a unit while parsing an Entity.\n");
            }

            // Are there *multiple* units?
            else if (!this.entities.isEmpty()) {
                this.warning
                        .append("Found a second unit.  Clearing first unit.\n");

                // Restart the unit list.
                this.entities.removeAllElements();
            }

        } else if (name.equals(TEMPLATE)) {
            // Do nothing.
        } else if (name.equals(ENTITY)) {

            // Are we in the middle of parsing an Entity?
            if (this.entity != null) {
                this.warning
                        .append("Found another Entity while parsing an Entity.\n");
            }

            // Are we in the middle of parsing an Entity's location?
            else if (this.loc != Entity.LOC_NONE) {
                this.warning
                        .append("Found another Entity while parsing a location.\n");
            }

            // Start a new entity.
            else {

                // Look for the element's attributes.
                String chassis = (String) attr.get(CHASSIS);
                String model = (String) attr.get(MODEL);

                // Did we find required attributes?
                if (chassis == null || chassis.length() == 0) {
                    this.warning.append("Could not find chassis for Entity.\n");
                } else {

                    // Try to find the entity.
                    MechSummary ms = null;
                    StringBuffer key = new StringBuffer(chassis);
                    ms = MechSummaryCache.getInstance().getMech(key.toString());
                    if (model != null && model.length() > 0) {
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
                        this.warning
                        .append("Could not find Entity with chassis: ");
                        this.warning.append(chassis);
                        if (model != null && model.length() > 0) {
                            this.warning.append(", and model: ");
                            this.warning.append(model);
                        }
                        this.warning.append(".\n");
                    } else {

                        // Try to load the new mech.
                        try {
                            this.entity = new MechFileParser(
                                    ms.getSourceFile(), ms.getEntryName())
                                    .getEntity();
                        } catch (EntityLoadingException excep) {
                            excep.printStackTrace(System.err);
                            this.warning.append("Unable to load mech: ")
                                    .append(ms.getSourceFile()).append(": ")
                                    .append(ms.getEntryName()).append(": ")
                                    .append(excep.getMessage());
                        }
                    } // End found-MechSummary

                } // End have-chassis

            } // End ready-for-new-Entity

        } else if (name.equals(FLUFF)) {
            // Do nothing.
        } else if (name.equals(PILOT)) {

            // Are we in the outside of an Entity?
            if (this.entity == null) {
                this.warning.append("Found a pilot outside of an Entity.\n");
            }

            // Are we in the middle of parsing an Entity's location?
            else if (this.loc != Entity.LOC_NONE) {
                this.warning
                        .append("Found a pilot while parsing a location.\n");
            }

            // Handle the pilot.
            else {

                // Look for the element's attributes.
                String pilotName = (String) attr.get(NAME);
                String gunnery = (String) attr.get(GUNNERY);
                String gunneryL = (String) attr.get(GUNNERYL);
                String gunneryM = (String) attr.get(GUNNERYM);
                String gunneryB = (String) attr.get(GUNNERYB);
                String piloting = (String) attr.get(PILOTING);
                String initB = (String) attr.get(INITB);
                String commandB = (String) attr.get(COMMANDB);
                String hits = (String) attr.get(HITS);
                String advantages = (String) attr.get(ADVS);
                String implants = (String) attr.get(IMPLANTS);
                String autoeject = (String) attr.get(AUTOEJECT);

                // Did we find required attributes?
                if (gunnery == null || gunnery.length() == 0) {
                    this.warning.append("Could not find gunnery for pilot.\n");
                } else if (piloting == null || piloting.length() == 0) {
                    this.warning.append("Could not find piloting for pilot.\n");
                } else {

                    // Try to get a good gunnery value.
                    int gunVal = -1;
                    try {
                        gunVal = Integer.parseInt(gunnery);
                    } catch (NumberFormatException excep) {
                        // Handled by the next if test.
                    }
                    if (gunVal < 0 || gunVal > 7) {
                        this.warning.append("Found invalid gunnery value: ")
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
                    if (pilotVal < 0 || pilotVal > 7) {
                        this.warning.append("Found invalid piloting value: ")
                                .append(piloting).append(".\n");
                        return;
                    }

                    //init bonus
                    int initBVal = 0;
                    if (null != initB && initB.length() > 0) {
                        try {
                            initBVal = Integer.parseInt(initB);
                        } catch (NumberFormatException excep) {
                            // Handled by the next if test.
                        }
                    }
                    int commandBVal = 0;
                    if (null != commandB && commandB.length() > 0) {
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
                    if (null != gunneryL && gunneryL.length() > 0) {
                        try {
                            gunneryLVal = Integer.parseInt(gunneryL);
                        } catch (NumberFormatException excep) {
                            // Handled by the next if test.
                        }
                        if (gunneryLVal < 0 || gunneryLVal > 7) {
                            this.warning.append(
                                    "Found invalid piloting value: ").append(
                                    gunneryL).append(".\n");
                            return;
                        }
                    }
                    if (null != gunneryM && gunneryM.length() > 0) {
                        try {
                            gunneryMVal = Integer.parseInt(gunneryM);
                        } catch (NumberFormatException excep) {
                            // Handled by the next if test.
                        }
                        if (gunneryMVal < 0 || gunneryMVal > 7) {
                            this.warning.append(
                                    "Found invalid piloting value: ").append(
                                    gunneryM).append(".\n");
                            return;
                        }
                    }
                    if (null != gunneryB && gunneryB.length() > 0) {
                        try {
                            gunneryBVal = Integer.parseInt(gunneryB);
                        } catch (NumberFormatException excep) {
                            // Handled by the next if test.
                        }
                        if (gunneryBVal < 0 || gunneryBVal > 7) {
                            this.warning.append(
                                    "Found invalid piloting value: ").append(
                                    gunneryB).append(".\n");
                            return;
                        }
                    }

                    // Update the entity's crew.
                    Pilot crew = entity.getCrew();
                    if (null == pilotName || pilotName.length() == 0) {
                        pilotName = crew.getName();
                    }

                    crew = new Pilot(pilotName, gunneryLVal, gunneryMVal,
                            gunneryBVal, pilotVal);

                    crew.setInitBonus(initBVal);
                    crew.setCommandBonus(commandBVal);
                    if ((null != advantages)
                            && (advantages.trim().length() > 0)) {
                        StringTokenizer st = new StringTokenizer(advantages,
                                "::");
                        while (st.hasMoreTokens()) {
                            String adv = st.nextToken();
                            String advName = Pilot.parseAdvantageName(adv);
                            Object value = Pilot.parseAdvantageValue(adv);

                            try {
                                crew.getOptions().getOption(advName).setValue(
                                        value);
                            } catch (Exception e) {
                                this.warning.append(
                                        "Error restoring advantage: ").append(
                                        adv).append(".\n");
                            }
                        }

                    }

                    if ((null != implants) && (implants.trim().length() > 0)) {
                        StringTokenizer st = new StringTokenizer(implants, "::");
                        while (st.hasMoreTokens()) {
                            String implant = st.nextToken();
                            String implantName = Pilot
                                    .parseAdvantageName(implant);
                            Object value = Pilot.parseAdvantageValue(implant);

                            try {
                                crew.getOptions().getOption(implantName)
                                        .setValue(value);
                            } catch (Exception e) {
                                this.warning.append(
                                        "Error restoring advantage: ").append(
                                        implant).append(".\n");
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
                            this.warning.append("The pilot, ")
                                    .append(pilotName).append(", is dead.\n");
                        } else if (hitVal < 0 || hitVal > 5) {
                            this.warning.append("Found invalid hits value: ")
                                    .append(hits).append(".\n");
                        } else {
                            crew.setHits(hitVal);
                        }

                    } // End have-hits

                    // Set the crew for this entity.
                    this.entity.setCrew(crew);

                    if (autoeject != null) {
                        if (autoeject.equals("true")) {
                            ((Mech) this.entity).setAutoEject(true);
                        } else {
                            ((Mech) this.entity).setAutoEject(false);
                        }
                    }

                } // End have-required-fields
            } // End ready-for-pilot
        } else if (name.equals(LOCATION)) {

            // Are we in the outside of an Entity?
            if (this.entity == null) {
                this.warning.append("Found a location outside of an Entity.\n");
            }

            // Are we in the middle of parsing an Entity's location?
            else if (this.loc != Entity.LOC_NONE) {
                this.warning
                        .append("Found a location while parsing a location.\n");
            }

            // Handle the location.
            else {

                // Look for the element's attributes.
                String index = (String) attr.get(INDEX);
                String destroyed = (String) attr.get(IS_DESTROYED);

                // Did we find required attributes?
                if (index == null || index.length() == 0) {
                    this.warning.append("Could not find index for location.\n");
                } else {

                    // Try to get a good index value.
                    int indexVal = -1;
                    try {
                        indexVal = Integer.parseInt(index);
                    } catch (NumberFormatException excep) {
                        // Handled by the next if test.
                    }
                    if (indexVal < 0 || indexVal > 7) {
                        this.warning.append(
                                "Found invalid index value for location: ")
                                .append(index).append(".\n");
                        return;
                    } else if (indexVal >= entity.locations()) {
                        this.warning.append("The entity, ").append(
                                entity.getShortName()).append(
                                " does not have a location at index: ").append(
                                indexVal).append(".\n");
                        return;
                    } else {

                        // We're now parsing the indexed location.
                        this.loc = indexVal;

                        // Reset the ammo count.
                        this.locAmmoCount = 0;

                        // Is the location destroyed?
                        this.locDestroyed = false;
                        try {
                            if (destroyed != null) {
                                this.locDestroyed = destroyed.equals("true");
                            }
                        } catch (Throwable excep) {
                            this.warning.append(
                                    "Found invalid isDestroyed value: ")
                                    .append(destroyed).append(".\n");
                        }

                    } // End have-valid-index
                } // End have-required-fields
            } // End ready-for-location
        } else if (name.equals(TURRETLOCK)) {
            // Are we in the outside of an Entity?
            if (this.entity == null) {
                this.warning
                        .append("Found turret lock outside of an Entity.\n");
            } else if (!(this.entity instanceof Tank)) {
                this.warning
                        .append("Turret crit record found outside a Tank.\n");
            }
            String value = (String) attr.get(DIRECTION);
            try {
                int turDir = Integer.parseInt(value);
                ((Tank) this.entity).setSecondaryFacing(turDir);
                ((Tank) this.entity).lockTurret();
            } catch (Exception e) {
                System.err.println(e);
                e.printStackTrace();
                this.warning
                        .append("Invalid turret lock direction value in movement tag.\n");
            }
        } else if (name.equals(MOVEMENT)) {
            // Are we in the outside of an Entity?
            if (this.entity == null) {
                this.warning
                        .append("Found movement crit outside of an Entity.\n");
            } else if (!(this.entity instanceof Tank)) {
                this.warning
                        .append("Movement crit record found outside a Tank.\n");
            }
            String value = (String) attr.get(SPEED);
            if (value.equals("immobile")) {
                ((Tank) (this.entity)).immobilize();
            } else {
                try {
                    int newSpeed = Integer.parseInt(value);
                    this.entity.setOriginalWalkMP(newSpeed);
                } catch (Exception e) {
                    this.warning
                            .append("Invalid speed value in movement tag.\n");
                }
            }
        } else if (name.equals(ARMOR)) {

            // Are we in the outside of an Entity?
            if (this.entity == null) {
                this.warning.append("Found armor outside of an Entity.\n");
            }

            // Are we in the outside of parsing an Entity's location?
            else if (this.loc == Entity.LOC_NONE) {
                this.warning
                        .append("Found armor while outside of a location.\n");
            }

            // Handle the location.
            else {

                // Look for the element's attributes.
                String points = (String) attr.get(POINTS);
                String type = (String) attr.get(TYPE);

                // Did we find required attributes?
                if (points == null || points.length() == 0) {
                    this.warning.append("Could not find points for armor.\n");
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
                    } else if (pointsVal < 0 || pointsVal > 2000) {
                        this.warning.append("Found invalid points value: ")
                                .append(points).append(".\n");
                        return;
                    }

                    // Assign the points to the correct location.
                    // Sanity check the armor value before setting it.
                    if (type == null || type.equals(FRONT)) {
                        if (this.entity.getOArmor(this.loc) < pointsVal) {
                            this.warning.append("The entity, ").append(
                                    this.entity.getShortName()).append(
                                    " does not start with ").append(pointsVal)
                                    .append(" points of armor for location: ")
                                    .append(this.loc).append(".\n");
                        } else {
                            this.entity.setArmor(pointsVal, this.loc);
                        }
                    } else if (type.equals(INTERNAL)) {
                        if (this.entity.getOInternal(this.loc) < pointsVal) {
                            this.warning
                                    .append("The entity, ")
                                    .append(this.entity.getShortName())
                                    .append(" does not start with ")
                                    .append(pointsVal)
                                    .append(
                                            " points of internal structure for location: ")
                                    .append(this.loc).append(".\n");
                        } else {
                            this.entity.setInternal(pointsVal, this.loc);
                        }
                    } else if (type.equals(REAR)) {
                        if (!this.entity.hasRearArmor(this.loc)) {
                            this.warning.append("The entity, ").append(
                                    this.entity.getShortName()).append(
                                    " has no rear armor for location: ")
                                    .append(this.loc).append(".\n");
                        } else if (this.entity.getOArmor(this.loc, true) < pointsVal) {
                            this.warning
                                    .append("The entity, ")
                                    .append(this.entity.getShortName())
                                    .append(" does not start with ")
                                    .append(pointsVal)
                                    .append(
                                            " points of rear armor for location: ")
                                    .append(this.loc).append(".\n");
                        } else {
                            this.entity.setArmor(pointsVal, this.loc, true);
                        }
                    }
                } // End have-required-fields
            } // End ready-for-armor
        } else if ( name.equals(SI) ) {
            if ( this.entity == null ) {
                this.warning.append
                    ( "Found structural integrity outside of an Entity.\n" );
            } else if (!(this.entity instanceof Aero)) {
                this.warning.append
                    ( "structural integrity record found outside an Aero.\n" );
            }
            String value = (String) attr.get( INTEGRITY );
            try {
                int newSI = Integer.parseInt(value);
                ((Aero)this.entity).setSI(newSI);
            } catch (Exception e) {
                this.warning.append
                    ( "Invalid SI value in structural integrity tag.\n" );
            }
        }
        else if ( name.equals(HEAT) ) {
            if ( this.entity == null ) {
                this.warning.append
                    ( "Found heat sink outside of an Entity.\n" );
            } else if (!(this.entity instanceof Aero)) {
                this.warning.append
                    ( "heat sink record found outside an Aero.\n" );
            }
            String value = (String) attr.get( SINK );
            try {
                int newSinks = Integer.parseInt(value);
                ((Aero)this.entity).setHeatSinks(newSinks);
            } catch (Exception e) {
                this.warning.append
                    ( "Invalid heat sink value in heat sink tag.\n" );
            }
        }
        else if ( name.equals(FUEL) ) {
            if ( this.entity == null ) {
                this.warning.append
                    ( "Found fuel outside of an Entity.\n" );
            } else if (!(this.entity instanceof Aero)) {
                this.warning.append
                    ( "fuel record found outside an Aero.\n" );
            }
            String value = (String) attr.get( LEFT );
            try {
                int newFuel = Integer.parseInt(value);
                ((Aero)this.entity).setFuel(newFuel);
            } catch (Exception e) {
                this.warning.append
                    ( "Invalid fuel value in fuel tag.\n" );
            }
        }
        else if ( name.equals(KF) ) {
            if ( this.entity == null ) {
                this.warning.append
                    ( "Found KF integrity outside of an Entity.\n" );
            } else if (!(this.entity instanceof Jumpship)) {
                this.warning.append
                    ( "KF integrity record found outside a Jumpship.\n" );
            }
            String value = (String) attr.get( INTEGRITY );
            try {
                int newIntegrity = Integer.parseInt(value);
                ((Jumpship)this.entity).setKFIntegrity(newIntegrity);
            } catch (Exception e) {
                this.warning.append
                    ( "Invalid KF integrity value in KF integrity tag.\n" );
            }
        }
        else if ( name.equals(SAIL) ) {
            if ( this.entity == null ) {
                this.warning.append
                    ( "Found sail integrity outside of an Entity.\n" );
            } else if (!(this.entity instanceof Jumpship)) {
                this.warning.append
                    ( "sail integrity record found outside a Jumpship.\n" );
            }
            String value = (String) attr.get( INTEGRITY );
            try {
                int newIntegrity = Integer.parseInt(value);
                ((Jumpship)this.entity).setSailIntegrity(newIntegrity);
            } catch (Exception e) {
                this.warning.append
                    ( "Invalid sail integrity value in sail integrity tag.\n" );
            }
        }
        else if ( name.equals(AEROCRIT) ) {
            if ( this.entity == null ) {
                this.warning.append
                    ( "Found aero crits outside of an Entity.\n" );
            } else if (!(this.entity instanceof Aero)) {
                this.warning.append
                ( "Found aero crits outside of an Aero.\n" );
            }
            else 
            {
                String avionics = (String) attr.get( AVIONICS );
                String sensors = (String) attr.get( SENSORS );
                String engine = (String) attr.get( ENGINE );
                String fcs = (String) attr.get( FCS );
                String cic = (String) attr.get( CIC );
                String leftThrust = (String) attr.get( LEFT_THRUST );
                String rightThrust = (String) attr.get( RIGHT_THRUST );
                String lifeSupport = (String) attr.get( LIFE_SUPPORT );
                String gear = (String) attr.get( GEAR );
                
                Aero a = (Aero)this.entity;
                
                if ( avionics != null ) {
                    a.setAvionicsHits(Integer.parseInt( avionics ));
                }
                
                if ( sensors != null ) {
                    a.setSensorHits(Integer.parseInt( sensors ));
                }
                
                if ( engine != null ) {
                    a.setEngineHits(Integer.parseInt( engine ));
                }
                
                if ( fcs != null ) {
                    a.setFCSHits(Integer.parseInt( fcs ));
                }
                
                if ( cic != null ) {
                    a.setCICHits(Integer.parseInt( cic ));
                }
                
                if ( leftThrust != null ) {
                    a.setLeftThrustHits(Integer.parseInt( leftThrust ));
                }
                
                if ( rightThrust != null ) {
                    a.setRightThrustHits(Integer.parseInt( rightThrust ));
                }
                
                if ( lifeSupport != null ) {
                    a.setLifeSupport(false);
                }
                
                if ( gear != null ) {
                    a.setGearHit(true);
                }
            }     
        } else if (name.equals(SLOT)) {

            // Are we in the outside of an Entity?
            if (this.entity == null) {
                this.warning.append("Found a slot outside of an Entity.\n");
            }

            // Are we in the outside of parsing an Entity's location?
            else if (this.loc == Entity.LOC_NONE) {
                this.warning
                        .append("Found a slot while outside of a location.\n");
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
                String munition = (String) attr.get(MUNITION);

                // Did we find required attributes?
                if (index == null || index.length() == 0) {
                    this.warning.append("Could not find index for slot.\n");
                } else if (type == null || type.length() == 0) {
                    this.warning.append("Could not find type for slot.\n");
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
                        if (entity instanceof Tank
                                || entity instanceof Protomech) {

                            // Get the saved ammo load.
                            EquipmentType newLoad = EquipmentType.get(type);
                            if (newLoad instanceof AmmoType) {
                                int counter = -1;
                                Iterator<Mounted> ammo = entity.getAmmo()
                                        .iterator();
                                while (ammo.hasNext()
                                        && counter < this.locAmmoCount) {

                                    // Is this mounted in the current location?
                                    Mounted mounted = ammo.next();
                                    if (mounted.getLocation() == loc) {

                                        // Increment the loop counter.
                                        counter++;

                                        // Is this the one we want to handle?
                                        if (counter == this.locAmmoCount) {

                                            // Increment the counter of ammo
                                            // handled for this location.
                                            this.locAmmoCount++;

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
                                                this.warning
                                                        .append(
                                                                "Expected to find number of shots for ")
                                                        .append(type).append(
                                                                ", but found ")
                                                        .append(shots).append(
                                                                " instead.\n");
                                            } else if (shotsVal < 0
                                                    || shotsVal > 200) {
                                                this.warning
                                                        .append(
                                                                "Found invalid shots value for slot: ")
                                                        .append(shots).append(
                                                                ".\n");
                                            } else {

                                                // Change to the saved
                                                // ammo type and shots.
                                                mounted
                                                        .changeAmmoType((AmmoType) newLoad);
                                                mounted.setShotsLeft(shotsVal);

                                            } // End have-good-shots-value

                                            // Stop looking for a match.
                                            break;

                                        } // End found-match-for-slot

                                    } // End ammo-in-this-loc

                                } // Check the next ammo.

                            } else {
                                // Bad XML equipment.
                                this.warning
                                        .append("XML file lists ")
                                        .append(type)
                                        .append(" equipment at location ")
                                        .append(this.loc)
                                        .append(
                                                ".  XML parser expected ammo.\n");
                            } // End not-ammo-type

                        } // End is-tank

                        // TODO: handle slotless equipment.
                        return;
                    } else if (indexVal < 0 || indexVal > 12) {
                        this.warning.append(
                                "Found invalid index value for slot: ").append(
                                index).append(".\n");
                        return;
                    }

                    // Is this index valid for this entity?
                    if (indexVal > entity.getNumberOfCriticals(this.loc)) {
                        this.warning.append("The entity, ").append(
                                this.entity.getShortName()).append(
                                " does not have ").append(index).append(
                                " slots in location ").append(this.loc).append(
                                ".\n");
                        return;
                    }

                    // Try to get a good isHit value.
                    boolean hitFlag = false;
                    try {
                        if (hit != null) {
                            hitFlag = hit.equals("true");
                        }
                    } catch (Throwable excep) {
                        this.warning.append("Found invalid isHit value: ")
                                .append(hit).append(".\n");
                    }

                    // Is the location destroyed?
                    boolean destFlag = false;
                    try {
                        if (destroyed != null) {
                            destFlag = destroyed.equals("true");
                        }
                    } catch (Throwable excep) {
                        this.warning
                                .append("Found invalid isDestroyed value: ")
                                .append(destroyed).append(".\n");
                    }

                    // Try to get the critical slot.
                    CriticalSlot slot = this.entity.getCritical(this.loc,
                            indexVal);

                    // Did we get it?
                    if (slot == null) {
                        if (!type.equals(EMPTY)) {
                            this.warning.append("Could not find the ").append(
                                    type).append(
                                    " equipment that was expected at index ")
                                    .append(indexVal).append(" of location ")
                                    .append(this.loc).append(".\n");
                        }
                        return;
                    }

                    // Is the slot for a critical system?
                    if (slot.getType() == CriticalSlot.TYPE_SYSTEM) {

                        // Does the XML file have some other kind of equipment?
                        if (!type.equals(SYSTEM)) {
                            this.warning.append("XML file expects to find ")
                                    .append(type)
                                    .append(" equipment at index ").append(
                                            indexVal).append(" of location ")
                                    .append(this.loc).append(
                                            ", but Entity has a system.\n");
                        }

                    } else {

                        // Nope, we've got equipment. Get this slot's mounted.
                        Mounted mounted = this.entity.getEquipment(slot
                                .getIndex());

                        // Reset transient values.
                        mounted.restore();

                        // Hit and destroy the mounted, according to the flags.
                        mounted.setDestroyed(hitFlag || destFlag);

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
                                    this.warning
                                            .append(
                                                    "Expected to find number of shots for ")
                                            .append(type)
                                            .append(", but found ").append(
                                                    shots)
                                            .append(" instead.\n");
                                } else if (shotsVal < 0 || shotsVal > 200) {
                                    this.warning
                                            .append(
                                                    "Found invalid shots value for slot: ")
                                            .append(shots).append(".\n");
                                } else {

                                    // Change to the saved ammo type and shots.
                                    mounted.changeAmmoType((AmmoType) newLoad);
                                    mounted.setShotsLeft(shotsVal);

                                } // End have-good-shots-value

                            } else {
                                // Bad XML equipment.
                                this.warning.append("XML file expects ")
                                        .append(type).append(
                                                " equipment at index ").append(
                                                indexVal).append(
                                                " of location ").append(
                                                this.loc).append(
                                                ", but Entity has ").append(
                                                mounted.getType()
                                                        .getInternalName())
                                        .append("there .\n");
                            }

                        } // End slot-for-ammo

                        // Not an ammo slot... does file agree with template?
                        else if (!mounted.getType().getInternalName().equals(
                                type)) {
                            // Bad XML equipment.
                            this.warning
                                    .append("XML file expects ")
                                    .append(type)
                                    .append(" equipment at index ")
                                    .append(indexVal)
                                    .append(" of location ")
                                    .append(this.loc)
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
                                this.warning
                                        .append("XML file expects ")
                                        .append(
                                                " ammo for munition argument of ")
                                        .append(" slot tag.\n");
                            }
                        }

                    } // End have-equipment

                    // Hit and destroy the slot, according to the flags.
                    slot.setHit(hitFlag);
                    slot.setDestroyed(destFlag);

                } // End have-required-fields
            } // End ready-for-slot
        }

    } // End public void recordElementStart( String, Hashtable )

    public void recordElementEnd(String name) throws ParseException {

        // TODO: handle template files.

        // What kind of element have we started?
        if (name.equals(UNIT)) {

            // Are we in the middle of parsing an Entity?
            if (this.entity != null) {
                this.warning.append("End of unit while parsing an Entity.\n");

                // Are we in the middle of parsing an Entity's location?
                if (this.loc != Entity.LOC_NONE) {
                    this.warning
                            .append("Found end of unit while parsing a location.\n");

                    // If the open location is marked destroyed, destroy it.
                    if (this.locDestroyed) {
                        this.destroyLocation(this.entity, this.loc);
                    }
                    this.loc = Entity.LOC_NONE;
                }

                // Add the entity to the vector.
                this.entities.addElement(this.entity);
                this.entity = null;
            }

            // Is this an empty unit?
            else if (this.entities.isEmpty()) {
                this.warning.append("Found an empty unit.\n");
            }

        } else if (name.equals(TEMPLATE)) {
            // Do nothing.
        } else if (name.equals(ENTITY)) {

            // We should be in the middle of parsing an Entity.
            if (this.entity == null) {
                this.warning
                        .append("Found end of Entity, but not parsing an Entity.\n");
            } else {

                // Are we in the middle of parsing an Entity's location?
                if (this.loc != Entity.LOC_NONE) {
                    this.warning
                            .append("Found end of Entity while parsing a location.\n");

                    // If the open location is marked destroyed, destroy it.
                    if (this.locDestroyed) {
                        this.destroyLocation(this.entity, this.loc);
                    }
                    this.loc = Entity.LOC_NONE;
                }

                // Add the entity to the vector.
                this.entities.addElement(this.entity);
                this.entity = null;

            } // End save-entity
        } else if (name.equals(FLUFF)) {
            // Do nothing.
        } else if (name.equals(PILOT)) {
            // Do nothing.
        } else if (name.equals(LOCATION)) {

            // We should be in the middle of parsing an Entity.
            if (this.entity == null) {
                this.warning
                        .append("Found end of location, but not parsing an Entity.\n");
            }

            // Are we in the middle of parsing an Entity's location?
            else if (this.loc == Entity.LOC_NONE) {
                this.warning
                        .append("Found end of location, but not parsing a location.\n");

            } else {

                // If the location is marked destroyed, destroy the location.
                if (this.locDestroyed) {
                    this.destroyLocation(this.entity, this.loc);
                }

                // Reset the location.
                this.loc = Entity.LOC_NONE;

            } // End finish-location

        } else if (name.equals(ARMOR)) {
            // Do nothing.
        } else if (name.equals(SLOT)) {
            // Do nothing.
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
        if (this.inStream == null) {
            throw new ParseException("Input document stream not defined.");
        }
        return this.inStream;
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

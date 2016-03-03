/*
 * MegaMek - Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
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
package megamek.common.loaders;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import megamek.common.BipedMech;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.LocationFullException;
import megamek.common.Mech;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.QuadMech;
import megamek.common.TechConstants;
import megamek.common.WeaponType;

/**
 * TdbFile.java
 *  -based on MtfFile.java, modifications by Ryan McConnell
 * Created on April 1, 2003, 2:48 PM
 */
@XmlRootElement(name = "mech")
@XmlAccessorType(XmlAccessType.NONE)
public class TdbFile implements IMechLoader {

    private static final String DOUBLE = "Double";

    private static final int TECHYEAR = 3068; // TDB doesn't have era

    @XmlElement
    private Creator creator;
    
    @XmlElement(name = "basics")
    private BasicInformation basics;
    
    @XmlElement(name = "mounteditems")
    MountedItems mounted;
    
    @XmlElement(name = "critdefs")
    private CriticalDefinitions critdefs;
    
    private Map<EquipmentType, Mounted> hSharedEquip = new HashMap<>();
    private List<Mounted> vSplitWeapons = new ArrayList<>();

    /**
     * Creates new TdbFile.
     * 
     * @param is an input stream that contains a "The Drawing Board" generated XML file.
     * @return an instance of a parsed file
     * @throws megamek.common.loaders.EntityLoadingException
     */
    public static TdbFile getInstance(final InputStream is) throws EntityLoadingException {
        try {
            JAXBContext jc = JAXBContext.newInstance(TdbFile.class);
            
            Unmarshaller um = jc.createUnmarshaller();
            TdbFile tdbFile = (TdbFile) um.unmarshal(is);
            
            return tdbFile;
        } catch (JAXBException e) {
            throw new EntityLoadingException("   Failure to parse XML ("
                    + e.getLocalizedMessage() + ")", e);
        }
    }
    
    /**
     * JAXB required constructor.
     */
    private TdbFile() {
    }

    @Override
    public Entity getEntity() throws EntityLoadingException {
        try {
            Mech mech;

            if (("Unknown".equals(creator.creatorName))
                    || !"The Drawing Board".equals(creator.creatorName)
                    || (Integer.parseInt(creator.creatorVersion) != 2)) {
                // MegaMek no longer supports older versions of The
                // Drawing Board (pre 2.0.23) due to incomplete xml
                // file information in those versions.
                throw new EntityLoadingException(
                        "This xml file is not a valid Drawing Board mech.  Make sure you are using version 2.0.23 or later of The Drawing Board.");
            }

            String gyroType = basics.structure.gyroType;
            if ("Extra-Light".equals(gyroType)) {
                gyroType = "XL";
            } else if ("Heavy-Duty".equals(gyroType)) {
                gyroType = "Heavy Duty";
            }

            String cockpitType = basics.structure.cockpitType;
            if ("Torso-Mounted".equals(cockpitType)) {
                cockpitType = "Torso Mounted";
            }
            
            if ("Quad".equals(basics.chassisConfig)) {
                mech = new QuadMech(gyroType, cockpitType);
            } else {
                mech = new BipedMech(gyroType, cockpitType);
            }

            // aarg! those stupid sub-names in parenthesis screw everything up
            // we may do something different in the future, but for now, I'm
            // going to strip them out
            int pindex = basics.name.indexOf('(');
            if (pindex == -1) {
                mech.setChassis(basics.name);
            } else {
                mech.setChassis(basics.name.substring(0, pindex - 1));
            }

            if (basics.variant != null) {
                mech.setModel(basics.variant);
            } else if (basics.model != null) {
                mech.setModel(basics.model);
            } else {
                // Do mechs need a model?
                mech.setModel("");
            }
            mech.setYear(TECHYEAR);
            mech.setOmni(basics.omni);

            switch (basics.technology.techBase) {
                case "Inner Sphere":
                    switch (basics.technology.rulesLevel) {
                        case 1:
                            mech.setTechLevel(TechConstants.T_INTRO_BOXSET);
                            break;
                        case 2:
                            mech.setTechLevel(TechConstants.T_IS_TW_NON_BOX);
                            break;
                        case 3:
                            mech.setTechLevel(TechConstants.T_IS_ADVANCED);
                            break;
                        default:
                            throw new EntityLoadingException(
                                    "Unsupported tech level: " + basics.technology.rulesLevel);
                    }
                    break;
                case "Clan":
                    switch (basics.technology.rulesLevel) {
                        case 2:
                            mech.setTechLevel(TechConstants.T_CLAN_TW);
                            break;
                        case 3:
                            mech.setTechLevel(TechConstants.T_CLAN_ADVANCED);
                            break;
                        default:
                            throw new EntityLoadingException(
                                    "Unsupported tech level: " + basics.technology.rulesLevel);
                    }
                    break;
                case "Mixed (IS Chassis)":
                case "Inner Sphere 'C'":
                    mech.setTechLevel(TechConstants.T_IS_ADVANCED);
                    mech.setMixedTech(true);
                    break;
                case "Mixed (Clan Chassis)":
                    mech.setTechLevel(TechConstants.T_CLAN_ADVANCED);
                    mech.setMixedTech(true);
                    break;
                default:
                    throw new EntityLoadingException("Unsupported tech base: "
                            + basics.technology.techBase);
            }
            
            if (basics.structure.structureType.substring(0, 3).equals("(C)")) {
                basics.structure.structureType = basics.structure.structureType.substring(4);
            }
            mech.setStructureType(basics.structure.structureType);

            if (basics.structure.armorType.substring(0, 3).equals("(C)")) {
                basics.structure.armorType = basics.structure.armorType.substring(4);
            }
            mech.setArmorType(basics.structure.armorType);

            mech.setWeight(basics.tonnage);
            if (basics.jumpMP != null) {
                mech.setOriginalJumpMP(basics.jumpMP);
            }

            int engineFlags = 0;
            if ((mech.isClan() && !mech.isMixedTech())
                    || (mech.isMixedTech() && mech.isClan() && !mech.itemOppositeTech(basics.structure.engine.engineType))) {
                engineFlags = megamek.common.Engine.CLAN_ENGINE;
            }
            mech.setEngine(new megamek.common.Engine(basics.structure.engine.engineRating, 
                    megamek.common.Engine.getEngineTypeByString(basics.structure.engine.engineType), engineFlags));

            mech.autoSetInternal();

            Collections.sort(critdefs.locations);
            
            for (Location loc : critdefs.locations) {
                mech.initializeArmor(loc.armor, loc.bodyPart);
                
                if (Mech.LOC_LT == loc.bodyPart || Mech.LOC_RT == loc.bodyPart || Mech.LOC_CT == loc.bodyPart) {
                    mech.initializeRearArmor(loc.rearArmor, loc.bodyPart);
                }
                
                // Don't sort the Head criticals, the empty slot (if there) needs to stay
                // right where it is.
                if (Mech.LOC_HEAD != loc.bodyPart) {
                    Collections.sort(loc.criticalSlots);
                }
            }

            // we do these in reverse order to get the outermost
            // locations first, which is necessary for split crits to work
            for (int i = mech.locations() - 1; i >= 0; i--) {
                parseCrits(mech, i);
            }

            if (mech.isClan()) {
                mech.addClanCase();
            }

            mech.setArmorTonnage(mech.getArmorWeight());

            // add any heat sinks not allocated
            mech.addEngineSinks(basics.structure.heatSink.count - mech.heatSinks(), 
                    basics.structure.heatSink.isDouble() ? MiscType.F_DOUBLE_HEAT_SINK : MiscType.F_HEAT_SINK);

            return mech;
        } catch (NumberFormatException ex) {
            throw new EntityLoadingException(
                    "NumberFormatException parsing file", ex);
        } catch (NullPointerException ex) {
            throw new EntityLoadingException(
                    "NullPointerException parsing file", ex);
        } catch (StringIndexOutOfBoundsException ex) {
            throw new EntityLoadingException(
                    "StringIndexOutOfBoundsException parsing file", ex);
        }
    }

    private void parseCrits(Mech mech, int loc) throws EntityLoadingException {
        // check for removed arm actuators
        if (!(mech instanceof QuadMech)) {
            if ((loc == Mech.LOC_LARM) || (loc == Mech.LOC_RARM)) {
                for (Location l : critdefs.locations) {
                    if (l.bodyPart == loc) {
                        if (!"Lower Arm Actuator".equals(l.criticalSlots.get(2).content)) {
                            mech.setCritical(loc, 2, null);
                        }
                        
                        if (!"Hand Actuator".equals(l.criticalSlots.get(3).content)) {
                            mech.setCritical(loc, 3, null);
                        }
                    }
                }
            }
        }

        Location location = findLocation(loc);
        
        // go thru file, add weapons
        for (int i = 0; i < mech.getNumberOfCriticals(loc); i++) {

            // if the slot's full already, skip it.
            if (mech.getCritical(loc, i) != null) {
                continue;
            }

            CriticalSlot crit = location.criticalSlots.get(i);
            
            // parse out and add the critical
            if (basics.structure.isClanTC() && "Targeting Computer".equals(crit.content)) {
                crit.content = "(C) " + crit.content;
            }

            boolean rearMounted = false;
            for (MountedItem mi : mounted.items) {
                if (Objects.equals(mi.location, location.bodyPart) && Objects.equals(mi.itemIndex, crit.itemIndex)) {
                    rearMounted = mi.rearMounted;
                }
            }

            if (crit.content.contains("Engine")) {
                mech.setCritical(loc, i, new megamek.common.CriticalSlot(
                        megamek.common.CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE));
                continue;
            }
            if (crit.content.contains("Gyro")) {
                mech.setCritical(loc, i, new megamek.common.CriticalSlot(
                        megamek.common.CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO));
                continue;
            }
            if (crit.content.contains("Life Support")) {
                mech.setCritical(loc, i, new megamek.common.CriticalSlot(
                        megamek.common.CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_LIFE_SUPPORT));
                continue;
            }
            if (crit.content.contains("Sensors")) {
                mech.setCritical(loc, i, new megamek.common.CriticalSlot(
                        megamek.common.CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_SENSORS));
                continue;
            }
            if (crit.content.contains("Cockpit")) {
                mech.setCritical(loc, i, new megamek.common.CriticalSlot(
                        megamek.common.CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_COCKPIT));
                continue;
            }
            if (crit.content.endsWith("[LRM]") || crit.content.endsWith("[SRM]")) {
                // This is a lame kludge for The Drawing Board, which
                // identifies which type of missle weapon an
                // Artemis IV system goes with.
                crit.content = crit.content.substring(0, 14);
            }
            if (crit.content.endsWith("- Artemis IV")) {
                // Ugh, another lame kludge to allow for loading of
                // The Drawing Board's specially marked Artemis IV
                // missle ammo. The only real game difference is
                // c-bill cost, which we don't care about anyway.
                crit.content = crit.content.substring(0, crit.content.indexOf(" - Artemis IV"));
            }
            if (crit.content.endsWith("- Narc")) {
                // Yet another lame kludge to allow for loading of
                // The Drawing Board's specially marked Narc
                // missle ammo.
                crit.content = crit.content.substring(0, crit.content.indexOf(" - Narc"));
            }
            try {
                String hashPrefix;
                if (crit.content.startsWith("(C)")) {
                    // equipment specifically marked as clan
                    hashPrefix = "Clan ";
                    crit.content = crit.content.substring(4);
                } else if (crit.content.startsWith("(IS)")) {
                    // equipment specifically marked as inner sphere
                    hashPrefix = "IS ";
                    crit.content = crit.content.substring(5);
                } else if (mech.isClan()) {
                    // assume equipment is clan because mech is clan
                    hashPrefix = "Clan ";
                } else {
                    // assume equipment is inner sphere
                    hashPrefix = "IS ";
                }
                EquipmentType etype = EquipmentType.get(hashPrefix + crit.content);
                if (etype == null) {
                    // try without prefix
                    etype = EquipmentType.get(crit.content);
                }
                if (etype != null) {
                    if (etype.isSpreadable()) {
                        // do we already have one of these? Key on Type
                        Mounted m = hSharedEquip.get(etype);
                        if (m != null) {
                            // use the existing one
                            mech.addCritical(loc, new megamek.common.CriticalSlot(m));
                            continue;
                        }
                        m = mech.addEquipment(etype, loc, rearMounted);
                        hSharedEquip.put(etype, m);
                    } else if (((etype instanceof WeaponType) && ((WeaponType)etype).isSplitable()) 
                            || ((etype instanceof MiscType) && etype.hasFlag(MiscType.F_SPLITABLE))) {
                        // do we already have this one in this or an outer
                        // location?
                        Mounted m = null;
                        boolean bFound = false;
                        for (int x = 0, n = vSplitWeapons.size(); x < n; x++) {
                            m = vSplitWeapons.get(x);
                            int nLoc = m.getLocation();
                            if (((nLoc == loc) || (loc == Mech
                                    .getInnerLocation(nLoc)))
                                    && (m.getType() == etype)) {
                                bFound = true;
                                break;
                            }
                        }
                        if (bFound) {
                            m.setFoundCrits(m.getFoundCrits() + 1);
                            if (m.getFoundCrits() >= etype.getCriticals(mech)) {
                                vSplitWeapons.remove(m);
                            }
                            // if we're in a new location, set the
                            // weapon as split
                            if (loc != m.getLocation()) {
                                m.setSplit(true);
                            }
                            // give the most restrictive location for arcs
                            int help = m.getLocation();
                            m.setLocation(Mech.mostRestrictiveLoc(loc, help));
                            if (loc != help) {
                                m.setSecondLocation(Mech.leastRestrictiveLoc(
                                        loc, help));
                            }
                        } else {
                            // make a new one
                            m = new Mounted(mech, etype);
                            m.setFoundCrits(1);
                            vSplitWeapons.add(m);
                        }
                        mech.addEquipment(m, loc, rearMounted);
                    } else {
                        mech.addEquipment(etype, loc, rearMounted);
                    }
                } else {
                    if (!crit.content.equals("Empty")) {
                        // Can't load this piece of equipment!
                        // Add it to the list so we can show the user.
                        mech.addFailedEquipment(crit.content);
                        // Make the failed equipment an empty slot
                        crit.content = "Empty";
                        // Compact criticals again
                        compactCriticals(location);
                        // Re-parse the same slot, since the compacting
                        // could have moved new equipment to this slot
                        i--;
                    }
                }
            } catch (LocationFullException ex) {
                throw new EntityLoadingException(ex.getMessage());
            }
        }
    }

    private void compactCriticals(final Location loc) {
        if (loc.bodyPart == Mech.LOC_HEAD) {
            // This location has an empty slot inbetween systems crits
            // which will mess up parsing if compacted.
            return;
        }
        
        Collections.sort(loc.criticalSlots);
    }
    
    private Location findLocation(final int loc) {
        for (Location l : critdefs.locations) {
            if (l.bodyPart == loc) {
                return l;
            }
        }
        
        return null;
    }

    /**
     * JAXB helper class for the creator tag.
     */
    private static class Creator {

        @XmlElement(name = "name")
        String creatorName = "Unknown";

        @XmlElement(name = "version")
        String creatorVersion = "Unknown";

        Creator() {
        }
    }

    /**
     * JAX helper class for the basics tag.
     */
    private static class BasicInformation {

        @XmlElement
        String name;

        @XmlElement
        String model;

        @XmlElement(name = "isomni")
        Boolean omni = Boolean.FALSE;

        @XmlElement
        String variant;

        @XmlElement
        Technology technology;

        @XmlElement
        Integer tonnage;

        @XmlElement(name = "type")
        String chassisConfig;

        @XmlElement(name = "movemechmod")
        MovementModifier moveMeKMod;

        @XmlElement(name = "jump")
        Integer jumpMP;

        @XmlElement(name = "structural")
        Structure structure;

        BasicInformation() {
        }

    }
    
    /**
     * JAXB helper class for the structural tag.
     */
    @XmlType
    private static class Structure {

        @XmlElement(name = "engine")
        Engine engine;

        @XmlElement(name = "gyro")
        String gyroType = "Standard";

        @XmlElement(name = "cockpit")
        String cockpitType = "Standard";

        @XmlElement(name = "targsys")
        String targSysStr;

        @XmlElement(name = "heatsinks")
        HeatSink heatSink;

        @XmlElement(name = "armor")
        String armorType;

        @XmlElement(name = "internal")
        String structureType;

        Structure() {
        }

        boolean isClanTC() {
            return (targSysStr.length() >= 3)
                    && (targSysStr.substring(0, 3).equals("(C)"));
        }
        
    }

    /**
     * JAXB helper class for the technology tag.
     */
    @XmlType
    private static class Technology {

        @XmlValue
        String techBase;

        @XmlAttribute(name = "level")
        Integer rulesLevel;

        Technology() {
        }

    }

    /**
     * JAXB helper class for the movemechmod tag.
     */
    @XmlType
    private static class MovementModifier {

        @XmlAttribute
        String LAMTonnage;

        MovementModifier() {
        }
    }

    /**
     * JAXB helper class for the heatsinks tag.
     */
    @XmlType
    private static class HeatSink {

        @XmlValue
        private String dblSinks;

        @XmlAttribute
        Integer count;

        HeatSink() {
        }

        public boolean isDouble() {
            return DOUBLE.equals(dblSinks);
        }

    }

    /**
     * JAXB helper for the engine tag.
     */
    @XmlType
    private static class Engine {

        @XmlValue
        String engineType;

        @XmlAttribute(name = "rating")
        Integer engineRating;

        Engine() {
        }

    }
    
    /**
     * JAXB helper class for the mounteditems tag.
     */
    @XmlType
    static class MountedItems {
    
        @XmlElement(name = "mounteditem")
        List<MountedItem> items = new ArrayList<>();

        MountedItems() {
        }
    }

    /**
     * JAXB helper class for the mounteditem tag.
     */
    @XmlType
    private static class MountedItem {

        @XmlAttribute(name = "itemindex")
        Integer itemIndex;

        @XmlAttribute(name = "rearmounted")
        @XmlJavaTypeAdapter(BooleanAdapter.class)
        Boolean rearMounted = Boolean.FALSE;

        @XmlAttribute
        @XmlJavaTypeAdapter(LocationAdapter.class)
        Integer location;

        MountedItem() {
        }
    }
    
    /**
     * JAXB helper class for the critdefs tag.
     */
    @XmlType
    private static class CriticalDefinitions {
        
        @XmlElement(name = "location")
        List<Location> locations = new ArrayList<>();

        CriticalDefinitions() {
        }
    }

    /**
     * JAXB helper class for the location tag.
     */
    @XmlType
    static class Location implements Comparable<Location> {

        @XmlAttribute
        Integer armor;

        @XmlAttribute(name = "reararmor")
        Integer rearArmor;

        @XmlAttribute(name = "name")
        @XmlJavaTypeAdapter(LocationAdapter.class)
        Integer bodyPart;
        
        @XmlElement(name = "criticalslot")
        List<CriticalSlot> criticalSlots = new ArrayList<>();

        Location(final Integer bodyPart, final Integer armor, final Integer rearArmor) {
            this.bodyPart = bodyPart;
            this.armor = armor;
            this.rearArmor = rearArmor;
        }

        Location() {
        }

        @Override
        public int compareTo(final Location o) {
            return bodyPart.compareTo(o.bodyPart);
        }
    }
    
    /**
     * JAXB helper class for the criticalslot tag.
     */
    @XmlType
    static class CriticalSlot implements Comparable<CriticalSlot> {

        private static final String EMPTY = "Empty";
        
        @XmlAttribute(name = "slotindex")
        Integer itemIndex;

        @XmlValue
        String content;

        CriticalSlot(final Integer itemIndex, final String content) {
            this.itemIndex = itemIndex;
            this.content = content;
        }

        CriticalSlot() {
        }

        @Override
        public int compareTo(final CriticalSlot o) {
            // Both empty slots, then compare the itemIndex
            if (EMPTY.equals(this.content) && (EMPTY.equals(o.content))) {
                return this.itemIndex.compareTo(o.itemIndex);
            }

            // This is empty, it's always "less" than something else
            if (EMPTY.equals(this.content)) {
                return 1;
            }
            
            // The other is empty, so this is always "more"
            if (EMPTY.equals(o.content)) {
                return -1;
            }

            // Neither is empty, maintain the original sort
            return this.itemIndex.compareTo(o.itemIndex);
        }
    }

    /**
     * JAXB translator for the TDB location constants to Mech.LOC_* constants.
     */
    private static class LocationAdapter extends XmlAdapter<String, Integer> {

        @Override
        public String marshal(final Integer v) throws Exception {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Integer unmarshal(final String v) throws Exception {
            switch (v) {
                case "H":
                    return Mech.LOC_HEAD;
                case "CT":
                    return Mech.LOC_CT;
                case "RT":
                    return Mech.LOC_RT;
                case "LT":
                    return Mech.LOC_LT;
                case "RA":
                case "FRL":
                    return Mech.LOC_RARM;
                case "LA":
                case "FLL":
                    return Mech.LOC_LARM;
                case "RL":
                case "RRL":
                    return Mech.LOC_RLEG;
                case "LL":
                case "RLL":
                    return Mech.LOC_LLEG;
                default:
                    return Mech.LOC_NONE;
            }
        }
        
    }
    
    private static class BooleanAdapter extends XmlAdapter<String, Boolean> {

        @Override
        public String marshal(final Boolean v) throws Exception {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Boolean unmarshal(final String v) throws Exception {
            return Boolean.getBoolean(v);
        }
        
    }
}

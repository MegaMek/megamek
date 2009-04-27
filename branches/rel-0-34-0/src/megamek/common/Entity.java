/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import megamek.common.actions.AbstractAttackAction;
import megamek.common.actions.ChargeAttackAction;
import megamek.common.actions.DfaAttackAction;
import megamek.common.actions.DisplacementAttackAction;
import megamek.common.actions.EntityAction;
import megamek.common.actions.PushAttackAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.event.GameEntityChangeEvent;
import megamek.common.preference.PreferenceManager;
import megamek.common.util.StringUtil;
import megamek.common.weapons.ACWeapon;
import megamek.common.weapons.BayWeapon;
import megamek.common.weapons.CapitalLaserBayWeapon;
import megamek.common.weapons.GaussWeapon;
import megamek.common.weapons.ISBombastLaser;
import megamek.common.weapons.WeaponHandler;

/**
 * Entity is a master class for basically anything on the board except terrain.
 */
public abstract class Entity extends TurnOrdered implements Transporter, Targetable, RoundUpdated {
    /**
     *
     */
    private static final long serialVersionUID = 1430806396279853295L;

    public static final int NONE = -1;

    public static final int LOC_NONE = -1;
    public static final int LOC_DESTROYED = -2;

    public static final int MAX_C3_NODES = 12;

    public static final int GRAPPLE_BOTH = 0;
    public static final int GRAPPLE_RIGHT = 1;
    public static final int GRAPPLE_LEFT = 2;

    protected transient IGame game;

    protected int id = Entity.NONE;

    /** ID settable by external sources (such as mm.net) */
    protected int externalId = Entity.NONE;

    protected float weight;
    protected boolean omni = false;
    protected String chassis;
    protected String model;
    protected String fluff = null;
    protected int year;
    protected int techLevel;
    protected Engine engine;
    protected boolean mixedTech = false;
    protected boolean designValid = true;

    protected String displayName = null;
    protected String shortName = null;
    public int duplicateMarker = 1;

    protected transient Player owner;
    protected int ownerId;

    /**
     * The pilot of the entity. Even infantry has a 'pilot'.
     */
    public Pilot crew = new Pilot();

    protected boolean shutDown = false;
    protected boolean doomed = false;
    protected boolean destroyed = false;

    protected Coords position = null;

    protected int facing = 0;
    protected int sec_facing = 0;

    protected int walkMP = 0;
    protected int jumpMP = 0;

    protected int targSys = MiscType.T_TARGSYS_STANDARD;

    protected boolean done = false;

    protected boolean prone = false;
    protected boolean hullDown = false;
    protected boolean findingClub = false;
    protected boolean armsFlipped = false;
    protected boolean unjammingRAC = false;
    protected boolean hasSpotlight = false;
    protected boolean illuminated = false;
    protected boolean spotlightIsActive = false;
    protected boolean usedSearchlight = false;
    protected boolean stuckInSwamp = false;
    protected boolean canUnstickByJumping = false;
    protected int taggedBy = -1;
    protected boolean layingMines = false;
    protected boolean _isEMId = false;

    protected DisplacementAttackAction displacementAttack = null;

    public int heat = 0;
    public int heatBuildup = 0;
    public int heatFromExternal = 0;
    public int delta_distance = 0;
    public int mpUsed = 0;
    public int moved = IEntityMovementType.MOVE_NONE;
    protected int mpUsedLastRound = 0;
    public boolean gotPavementBonus = false;
    public boolean hitThisRoundByAntiTSM = false;
    public boolean inReverse = false;

    private int[] exposure;
    private int[] armor;
    private int[] internal;
    private int[] orig_armor;
    private int[] orig_internal;
    public int damageThisPhase;
    public int damageThisRound;
    public int engineHitsThisRound;
    public boolean rolledForEngineExplosion = false; // So that we don't roll
    // twice in one round
    public boolean dodging;
    public boolean reckless;
    private boolean evading = false;

    public boolean spotting;
    private boolean clearingMinefield = false;
    protected int killerId = Entity.NONE;
    private int offBoardDistance = 0;
    private int offBoardDirection = IOffBoardDirections.NONE;
    private int retreatedDirection = IOffBoardDirections.NONE;

    protected int[] vectors = { 0, 0, 0, 0, 0, 0 };
    private int recoveryTurn = 0;
    // need to keep a list of areas that this entity has passed through on the
    // current turn
    private Vector<Coords> passedThrough = new Vector<Coords>();
    private boolean ramming;
    // to determine what arcs have fired for large craft
    private boolean[] frontArcFired;
    private boolean[] rearArcFired;

    /**
     * The object that tracks this unit's Inferno round hits.
     */
    public InfernoTracker infernos = new InfernoTracker();
    public ArtilleryTracker aTracker = new ArtilleryTracker();
    public TeleMissileTracker tmTracker = new TeleMissileTracker();

    protected String c3NetIdString = null;
    protected int c3Master = NONE;
    protected int c3CompanyMasterIndex = LOC_DESTROYED;

    protected int armorType = EquipmentType.T_ARMOR_UNKNOWN;
    protected int armorTechLevel = TechConstants.T_TECH_UNKNOWN;
    protected int structureType = EquipmentType.T_STRUCTURE_UNKNOWN;

    protected String source = "";

    /**
     * A list of all mounted equipment. (Weapons, ammo, and misc)
     */
    protected ArrayList<Mounted> equipmentList = new ArrayList<Mounted>();

    /**
     * A list of all mounted weapons. This only includes regular weapons, not
     * bay mounts or grouped weapon mounts.
     */
    protected ArrayList<Mounted> weaponList = new ArrayList<Mounted>();

    /**
     * A list of all mounted weapon bays
     */
    protected ArrayList<Mounted> weaponBayList = new ArrayList<Mounted>();

    /**
     * A list of all mounted weapon groups
     */
    protected ArrayList<Mounted> weaponGroupList = new ArrayList<Mounted>();

    /**
     * A list of every weapon mount, including bay mounts and weapon group
     * mounts
     */
    protected ArrayList<Mounted> totalWeaponList = new ArrayList<Mounted>();

    /**
     * A list of all mounted ammo.
     */
    protected ArrayList<Mounted> ammoList = new ArrayList<Mounted>();

    /**
     * A list of all mounted bombs.
     */
    protected ArrayList<Mounted> bombList = new ArrayList<Mounted>();

    /**
     * A list of all remaining equipment.
     */
    protected ArrayList<Mounted> miscList = new ArrayList<Mounted>();

    protected ArrayList<INarcPod> pendingINarcPods = new ArrayList<INarcPod>();
    protected ArrayList<INarcPod> iNarcPods = new ArrayList<INarcPod>();
    protected ArrayList<NarcPod> pendingNarcPods = new ArrayList<NarcPod>();
    protected ArrayList<NarcPod> narcPods = new ArrayList<NarcPod>();

    protected ArrayList<String> failedEquipmentList = new ArrayList<String>();

    // which teams have NARCd us? a long allows for 64 teams.
    protected long m_lNarcedBy = 0;
    protected long m_lPendingNarc = 0;

    /**
     * This matrix stores critical slots in the format [location][slot #]. What
     * locations entities have and how many slots there are in each is
     * determined by the subclasses of Entity such as Mech.
     */
    protected CriticalSlot[][] crits; // [loc][slot]

    /**
     * Stores the current movement mode.
     */
    protected int movementMode = IEntityMovementMode.NONE;

    protected boolean isHidden = false;

    protected boolean carcass = false;

    /**
     * The components of this entity that can transport other entities.
     */
    private Vector<Transporter> transports = new Vector<Transporter>();

    /**
     * The ids of the MechWarriors this entity has picked up
     */
    private Vector<Integer> pickedUpMechWarriors = new Vector<Integer>();

    /**
     * The ID of the <code>Entity</code> that has loaded this unit.
     */
    private int conveyance = Entity.NONE;

    /**
     * Set to <code>true</code> if this unit was unloaded this turn.
     */
    private boolean unloadedThisTurn = false;

    /**
     * The id of the <code>Entity</code> that is the current target of a swarm
     * attack by this unit.
     */
    private int swarmTargetId = Entity.NONE;

    /**
     * The id of the <code>Entity</code> that is attacking this unit with a
     * swarm attack.
     */
    private int swarmAttackerId = Entity.NONE;

    /**
     * Flag that indicates that the unit can still be salvaged (given enough
     * time and parts).
     */
    private boolean salvageable = true;

    /**
     * The removal condition is set when the entitiy is removed from the game.
     */
    private int removalCondition = IEntityRemovalConditions.REMOVE_UNKNOWN;

    /**
     * The round this unit will be deployed
     */
    private int deployRound = 0;

    /**
     * Marks an entity as having been deployed
     */
    private boolean deployed = false;

    /**
     * The unit number of this entity. All entities which are members of the
     * same low-level unit are expected to share the same unit number. Future
     * implementations may store multiple unit designations in the same unit
     * number (e.g. battalion, company, platoon, and lance).
     */
    private char unitNumber = (char) Entity.NONE;

    /**
     * Indicates whether this entity has been seen by the enemy during the
     * course of this game. Used in double-blind.
     */
    private boolean seenByEnemy = false;

    /**
     * Indicates whether this entity can currently be seen by the enemy. Used in
     * double-blind.
     */
    private boolean visibleToEnemy = false;

    /** Whether this entity is captured or not. */
    private boolean captured = false;

    /**
     * this is the elevation of the Entity--with respect to the surface of the
     * hex it's in. In other words, this may need to *change* as it moves from
     * hex to hex--without it going up or down. I.e.--level 0 hex, elevation
     * 5--it moves to a level 2 hex, without going up or down. elevation is now
     * 3.
     */
    protected int elevation = 0;

    /**
     * 2 vectors holding entity and weapon ids. to see who hit us this round
     * with a swarm volley from what launcher. This vector holds the Entity ids.
     *
     * @see megamek.common.Entity#hitBySwarmsWeapon
     */
    private Vector<Integer> hitBySwarmsEntity = new Vector<Integer>();

    /**
     * A vector that stores from which launcher we where hit by a swarm weapon
     * this round. This vector holds the weapon ID's.
     *
     * @see megamek.common.Entity#hitBySwarmsEntity
     */
    private Vector<Integer> hitBySwarmsWeapon = new Vector<Integer>();

    /**
     * True if and only if this is a canon (published) unit.
     */
    private boolean canon;

    private int assaultDropInProgress = 0;
    private boolean climbMode = false; // save climb mode from turn to turn for
    // convenience

    protected int lastTarget = Entity.NONE;

    /**
     * the entity id of our current spot-target
     */
    private int spotTargetId = Entity.NONE;

    private boolean isCommander = false;

    protected boolean isCarefulStanding = false;

    /**
     * a vector of currently active sensors that might be able to check range
     */
    private Vector<Sensor> sensors = new Vector<Sensor>();
    // the currently selected sensor
    private Sensor activeSensor;
    // the sensor chosen for next turn
    private Sensor nextSensor;
    // roll for sensor check
    private int sensorCheck;

    // the roll for ghost targets
    private int ghostTargetRoll;
    // the roll to override ghost targets
    private int ghostTargetOverride;

    // Tac Ops HeatSink Coolant Failure number
    protected int heatSinkCoolantFailureFactor;

    // for how many rounds should this unit stay shutdown due to tasering
    protected int taserShutdownRounds = 0;

    // is this unit shutdown by a BA taser?
    protected boolean shutdownByBATaser = false;

    // for how many more rounds does this unit suffer from taser feedback?
    protected int taserFeedBackRounds = 0;

    protected int taserInterference = 0;
    protected int taserInterferenceRounds = 0;

    // contains a HTML string describing BV calculation
    protected StringBuffer bvText = null;

    /**
     * Generates a new, blank, entity.
     */
    public Entity() {
        armor = new int[locations()];
        internal = new int[locations()];
        orig_armor = new int[locations()];
        orig_internal = new int[locations()];
        crits = new CriticalSlot[locations()][];
        exposure = new int[locations()];
        for (int i = 0; i < locations(); i++) {
            crits[i] = new CriticalSlot[getNumberOfCriticals(i)];
        }
        setC3NetId(this);
    }

    /**
     * Restores the entity after serialization
     */
    public void restore() {
        // restore all mounted equipments
        for (Mounted mounted : equipmentList) {
            mounted.restore();
        }
        // set game options, we derive some equipment's modes from this
        setGameOptions();
    }

    /**
     * Returns the ID number of this Entity.
     *
     * @return ID Number.
     */
    public int getId() {
        return id;
    }

    /**
     * Sets the ID number of this Entity, which will also set the display name
     * and short name to null.
     *
     * @param id
     *            the new ID.
     */
    public void setId(int id) {
        this.id = id;
        displayName = null;
        shortName = null;
    }

    /**
     * this returns the external ID.
     *
     * @return the ID settable by external sources (such as mm.net)
     * @see megamek.common.Entity#externalId
     */
    public int getExternalId() {
        return externalId;
    }

    /**
     * This sets the external ID.
     *
     * @param externalId
     *            the new external ID for this Entity.
     * @see megamek.common.Entity#externalId
     */
    public void setExternalId(int externalId) {
        this.externalId = externalId;
    }

    /**
     * This returns the game this Entity belongs to.
     *
     * @return the game.
     */
    public IGame getGame() {
        return game;
    }

    /**
     * This sets the game the entity belongs to. It also restores the entity and
     * checks that the game is in a consistent state. This function takes care
     * of the units transported by this entity.
     *
     * @param game
     *            the game.
     */
    public void setGame(IGame game) {
        this.game = game;
        restore();
        // Make sure the owner is set.
        if (null == owner) {
            if (Entity.NONE == ownerId) {
                throw new IllegalStateException("Entity doesn't know its owner's ID.");
            }
            Player player = game.getPlayer(ownerId);
            if (null == player) {
                System.err.println("Entity can't find player #" + ownerId);
            } else {
                setOwner(player);
            }
        }

        // Also set game for each entity "loaded" in this entity.
        Enumeration<Entity> iter = getLoadedUnits().elements();
        while (iter.hasMoreElements()) {
            iter.nextElement().setGame(game);
        }
    }

    /**
     * Returns the unit code for this entity.
     */
    public String getModel() {
        return model;
    }

    /**
     * Sets the unit code for this Entity.
     *
     * @param model
     *            The unit code.
     */
    public void setModel(String model) {
        this.model = model;
    }

    /**
     * Returns the chassis name for this entity.
     */
    public String getChassis() {
        return chassis;
    }

    /**
     * sets the chassis name for this entity.
     *
     * @param chassis
     *            The chassis name.
     */
    public void setChassis(String chassis) {
        this.chassis = chassis;
    }

    /**
     * Returns the fluff for this entity.
     */
    public String getFluff() {
        return fluff;
    }

    /**
     * sets the fluff text for this entity.
     *
     * @param fluff
     *            The fluff text.
     */
    public void setFluff(String fluff) {
        this.fluff = fluff;
    }

    /**
     * Returns the unit tech for this entity.
     */
    public int getTechLevel() {
        return techLevel;
    }

    /**
     * Sets the tech level for this Entity.
     *
     * @param techLevel
     *            The tech level, it must be one of the
     *            {@link megamek.common.TechConstants TechConstants }.
     */
    public void setTechLevel(int techLevel) {
        this.techLevel = techLevel;
    }

    public int getRecoveryTurn() {
        return recoveryTurn;
    }

    public void setRecoveryTurn(int r) {
        recoveryTurn = r;
    }

    /**
     * Checks if this is a clan unit. It is determined by tech level.
     *
     * @return true if this unit is a clan unit.
     * @see megamek.common.Entity#setTechLevel(int)
     */
    public boolean isClan() {
        return ((techLevel == TechConstants.T_CLAN_TW) || (techLevel == TechConstants.T_CLAN_ADVANCED) || (techLevel == TechConstants.T_CLAN_EXPERIMENTAL) || (techLevel == TechConstants.T_CLAN_UNOFFICIAL));
    }

    public boolean isClanArmor() {
        if (getArmorTechLevel() == TechConstants.T_TECH_UNKNOWN) {
            return isClan();
        }
        return ((getArmorTechLevel() == TechConstants.T_CLAN_TW) || (getArmorTechLevel() == TechConstants.T_CLAN_ADVANCED) || (getArmorTechLevel() == TechConstants.T_CLAN_EXPERIMENTAL) || (getArmorTechLevel() == TechConstants.T_CLAN_UNOFFICIAL));
    }

    public boolean isMixedTech() {
        return mixedTech;
    }

    public void setMixedTech(boolean mixedTech) {
        this.mixedTech = mixedTech;
    }

    public boolean isDesignValid() {
        return designValid;
    }

    public void setDesignValid(boolean designValid) {
        this.designValid = designValid;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public float getWeight() {
        return weight;
    }

    public int getWeightClass() {
        return EntityWeightClass.getWeightClass((int) getWeight());
    }

    public String getWeightClassName() {
        return EntityWeightClass.getClassName(getWeightClass());
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }

    public boolean isOmni() {
        return omni;
    }

    public void setOmni(boolean omni) {
        this.omni = omni;
    }

    /**
     * Returns the number of locations in the entity
     */
    public abstract int locations();

    /**
     * Returns the player that "owns" this entity.
     */
    public Player getOwner() {
        return owner;
    }

    public void setOwner(Player player) {
        owner = player;
        ownerId = player.getId();

        generateDisplayName();
    }

    public int getOwnerId() {
        return ownerId;
    }

    /**
     * Returns true if the other entity is an enemy of this entity. This is more
     * reliable than Player.isEnemyOf since it knows that an entity will never
     * be an enemy of itself.
     */
    public boolean isEnemyOf(Entity other) {
        if (null == owner) {
            return ((id != other.getId()) && (ownerId != other.ownerId));
        }
        return (id != other.getId()) && owner.isEnemyOf(other.getOwner());
    }

    public Pilot getCrew() {
        return crew;
    }

    public void setCrew(Pilot crew) {
        this.crew = crew;
    }

    public boolean isShutDown() {
        return shutDown;
    }

    public void setShutDown(boolean shutDown) {
        this.shutDown = shutDown;
    }

    public boolean isDoomed() {
        return doomed;
    }

    public void setDoomed(boolean doomed) {
        // Doomed entities aren't in retreat.
        if (doomed) {
            setRemovalCondition(IEntityRemovalConditions.REMOVE_SALVAGEABLE);
        }
        this.doomed = doomed;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    public void setDestroyed(boolean destroyed) {
        this.destroyed = destroyed;
    }

    // Targetable interface
    public int getTargetType() {
        return Targetable.TYPE_ENTITY;
    }

    public int getTargetId() {
        return getId();
    }

    public int getHeight() {
        return height();
    }

    // End Targetable interface

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    /**
     * This method should <strong>only</stong> be called when needed to remove a
     * dead swarmer's game turn.
     */
    public void setUnloaded(boolean unloaded) {
        unloadedThisTurn = unloaded;
    }

    /**
     * Determine if this entity participate in the current game phase.
     *
     * @return <code>true</code> if this entity is not shut down, is not
     *         destroyed, has an active crew, and was not unloaded from a
     *         transport this turn. <code>false</code> otherwise.
     */
    public boolean isActive() {
        return this.isActive(-1);
    }

    public boolean isActive(int turn) {
        boolean isActive = !shutDown && !destroyed && getCrew().isActive() && !unloadedThisTurn;

        if ((turn > -1) && isActive) {
            isActive = !deployed && shouldDeploy(turn);
        } else {
            isActive = isActive && deployed;
        }

        return isActive;
    }

    /**
     * Returns true if this entity is selectable for action. Transported
     * entities can not be selected.
     */
    public boolean isSelectableThisTurn() {
        return !done && (conveyance == Entity.NONE) && !unloadedThisTurn && !isClearingMinefield() && !isCarcass();
    }

    /**
     * Returns true if this entity could potentially be loaded (did not move
     * from starting hex)
     */
    public boolean isLoadableThisTurn() {
        return (delta_distance == 0) && (conveyance == Entity.NONE) && !unloadedThisTurn && !isClearingMinefield();
    }

    /**
     * Determine if this <code>Entity</code> was unloaded previously this turn.
     *
     * @return <code>true</code> if this entity was unloaded for any reason
     *         during this turn.
     */
    public boolean isUnloadedThisTurn() {
        return unloadedThisTurn;
    }

    /**
     * Returns true if this entity is targetable for attacks
     */
    public boolean isTargetable() {
        return !destroyed && !doomed && deployed && !isOffBoard();
    }

    public boolean isProne() {
        return prone;
    }

    public void setProne(boolean prone) {
        this.prone = prone;
        if (prone) {
            hullDown = false;
        }
    }

    public boolean isHullDown() {
        return hullDown;
    }

    public void setHullDown(boolean down) {
        hullDown = down;
        if (hullDown) {
            prone = false;
        }
    }

    /**
     * Is this entity shut down or is the crew unconscious?
     */
    public boolean isImmobile() {
        return isShutDown() || crew.isUnconscious();
    }

    public boolean isCharging() {
        return displacementAttack instanceof ChargeAttackAction;
    }

    public boolean isPushing() {
        return displacementAttack instanceof PushAttackAction;
    }

    public boolean isMakingDfa() {
        return displacementAttack instanceof DfaAttackAction;
    }

    public boolean hasDisplacementAttack() {
        return displacementAttack != null;
    }

    public DisplacementAttackAction getDisplacementAttack() {
        return displacementAttack;
    }

    public void setDisplacementAttack(DisplacementAttackAction displacementAttack) {
        this.displacementAttack = displacementAttack;
    }

    /**
     * Returns true if any other entities this entity knows of are making a
     * displacement attack on this entity.
     */
    public boolean isTargetOfDisplacementAttack() {
        return findTargetedDisplacement() != null;
    }

    /**
     * Returns any known displacement attacks (should only be one) that this
     * entity is a target of.
     */
    public DisplacementAttackAction findTargetedDisplacement() {
        for (Enumeration<Entity> i = game.getEntities(); i.hasMoreElements();) {
            Entity other = i.nextElement();
            if (other.hasDisplacementAttack() && (other.getDisplacementAttack().getTargetId() == id)) {
                return other.getDisplacementAttack();
            }
        }
        return null;
    }

    public boolean isUnjammingRAC() {
        return unjammingRAC;
    }

    public void setUnjammingRAC(boolean u) {
        unjammingRAC = u;
    }

    public boolean isFindingClub() {
        return findingClub;
    }

    public void setFindingClub(boolean findingClub) {
        this.findingClub = findingClub;
    }

    /**
     * Set whether or not the mech's arms are flipped to the rear
     */
    public void setArmsFlipped(boolean armsFlipped) {
        this.armsFlipped = armsFlipped;
        game.processGameEvent(new GameEntityChangeEvent(this, this));
    }

    /**
     * Returns true if the mech's arms are flipped to the rear
     */
    public boolean getArmsFlipped() {
        return armsFlipped;
    }

    /**
     * Returns the current position of this entity on the board. This is not
     * named getLocation(), since I want the word location to refer to hit
     * locations on a mech or vehicle.
     */
    public Coords getPosition() {
        return position;
    }

    /**
     * Sets the current position of this entity on the board.
     *
     * @param position
     *            the new position.
     */
    public void setPosition(Coords position) {
        this.position = position;
    }

    /**
     *
     * @return the coords of the second to last position on the passed through
     *         vector or the current position if too small
     */

    public Coords getPriorPosition() {
        if (passedThrough.size() < 2) {
            return getPosition();
        }
        return passedThrough.elementAt(passedThrough.size() - 2);
    }

    /**
     * Sets the current elevation of this entity above the ground.
     *
     * @param elevation
     *            an <code>int</code> representing the new position.
     */
    public void setElevation(int elevation) {
        this.elevation = elevation;
    }

    /**
     * A helper function for fiddling with elevation. Takes the current hex, a
     * hex being moved to, returns the elevation the Entity will be considered
     * to be at w/r/t it's new hex.
     */
    public int calcElevation(IHex current, IHex next, int assumedElevation, boolean climb, boolean wigeEndClimbPrevious) {
        int retVal = assumedElevation;
        if (this instanceof Aero) {
            return retVal;
        }
        if ((getMovementMode() == IEntityMovementMode.SUBMARINE) || ((getMovementMode() == IEntityMovementMode.INF_UMU) && (current.containsTerrain(Terrains.WATER) || next.containsTerrain(Terrains.WATER))) || (getMovementMode() == IEntityMovementMode.VTOL)
                // a WIGE in climb mode or that ended climb mode in the previous
                // hex stays at the same flight level, like a VTOL
                || ((getMovementMode() == IEntityMovementMode.WIGE) && (climb || wigeEndClimbPrevious) && (assumedElevation > 0)) || ((getMovementMode() == IEntityMovementMode.QUAD_SWIM) && hasUMU()) || ((getMovementMode() == IEntityMovementMode.BIPED_SWIM) && hasUMU())) {
            retVal += current.surface();
            retVal -= next.surface();
        } else {
            if ((getMovementMode() != IEntityMovementMode.HOVER) && (getMovementMode() != IEntityMovementMode.NAVAL) && (getMovementMode() != IEntityMovementMode.HYDROFOIL) && (getMovementMode() != IEntityMovementMode.WIGE)) {
                int prevWaterLevel = 0;
                if (current.containsTerrain(Terrains.WATER)) {
                    prevWaterLevel = current.terrainLevel(Terrains.WATER);
                    if (!(current.containsTerrain(Terrains.ICE)) || (assumedElevation < 0)) {
                        // count water, only if the entity isn't on ice surface
                        retVal += current.terrainLevel(Terrains.WATER);
                    }
                }
                if (next.containsTerrain(Terrains.WATER)) {
                    int waterLevel = next.terrainLevel(Terrains.WATER);
                    if (next.containsTerrain(Terrains.ICE)) {
                        // a mech can only climb out onto ice in depth 2 or
                        // shallower water
                        // mech on the surface will stay on the surface

                        if (((waterLevel == 1) && (prevWaterLevel == 1)) || ((prevWaterLevel <= 2) && climb) || (assumedElevation >= 0)) {
                            retVal += waterLevel;
                        }
                    }
                    retVal -= waterLevel;
                }
            }
            if (next.containsTerrain(Terrains.BUILDING) || current.containsTerrain(Terrains.BUILDING)) {
                int bldcur = Math.max(0, current.terrainLevel(Terrains.BLDG_ELEV));
                int bldnex = Math.max(0, next.terrainLevel(Terrains.BLDG_ELEV));

                if (((assumedElevation == bldcur) && climb && (this instanceof Mech)) || (retVal > bldnex)) {
                    retVal = bldnex;
                } else if (bldnex + next.surface() > bldcur + current.surface()) {
                    retVal += current.surface();
                    retVal -= next.surface();
                }
            }
            if ((getMovementMode() != IEntityMovementMode.NAVAL) && (getMovementMode() != IEntityMovementMode.HYDROFOIL) && (next.containsTerrain(Terrains.BRIDGE) || current.containsTerrain(Terrains.BRIDGE))) {
                int brdnex = Math.max(-(next.depth()), next.terrainLevel(Terrains.BRIDGE_ELEV));
                if (Math.abs((next.surface() + brdnex) - (current.surface() + assumedElevation)) <= getMaxElevationChange()) {
                    // bridge is reachable at least
                    if (climb || (Math.abs((next.surface() + retVal) - (current.surface() + assumedElevation)) > getMaxElevationChange()) || !isElevationValid(retVal, next)) {
                        // use bridge if you can't use the base terrain or if
                        // you prefer to by climb mode
                        retVal = brdnex;
                    }
                }
            }
        }
        return retVal;
    }

    public int calcElevation(IHex current, IHex next) {
        return calcElevation(current, next, elevation, false, false);
    }

    /**
     * Returns the elevation of this entity.
     */
    public int getElevation() {
        if (Entity.NONE != getTransportId()) {
            return game.getEntity(getTransportId()).getElevation();
        }

        if ((null == getPosition()) && (isDeployed())) {
            throw new IllegalStateException("Entity #" + getId() + " does not know its position.");
        }

        if (isOffBoard()) {
            return 0;
        }

        return elevation;
    }

    public boolean canGoDown() {
        return canGoDown(elevation, getPosition());
    }

    /**
     * is it possible to go down, or are we landed/just above the
     * water/treeline? assuming passed elevation.
     */
    public boolean canGoDown(int assumedElevation, Coords assumedPos) {
        boolean inWaterOrWoods = false;
        IHex hex = getGame().getBoard().getHex(assumedPos);
        int altitude = assumedElevation + hex.surface();
        int minAlt = hex.surface();
        if (hex.containsTerrain(Terrains.WOODS) || hex.containsTerrain(Terrains.WATER) || hex.containsTerrain(Terrains.JUNGLE)) {
            inWaterOrWoods = true;
        }
        switch (getMovementMode()) {
        case IEntityMovementMode.INF_JUMP:
        case IEntityMovementMode.INF_LEG:
        case IEntityMovementMode.INF_MOTORIZED:
            minAlt -= Math.max(0, hex.terrainLevel(Terrains.BLDG_BASEMENT));
            break;
        case IEntityMovementMode.VTOL:
        case IEntityMovementMode.WIGE:
            minAlt = hex.ceiling();
            if (inWaterOrWoods) {
                minAlt++; // can't land here
            }
            break;
        case IEntityMovementMode.AERODYNE:
        case IEntityMovementMode.SPHEROID:
            if (game.getBoard().inAtmosphere()) {
                altitude = assumedElevation;
                minAlt = hex.ceiling() + 1;
                // if sensors are damaged then, one higher
                if (((Aero) this).getSensorHits() > 0) {
                    minAlt++;
                }
            }
            break;
        case IEntityMovementMode.SUBMARINE:
        case IEntityMovementMode.INF_UMU:
        case IEntityMovementMode.BIPED_SWIM:
        case IEntityMovementMode.QUAD_SWIM:
            minAlt = hex.floor();
            break;
        default:
            return false;
        }
        return (altitude > minAlt);
    }

    /**
     * is it possible to go up, or are we at maximum altitude? assuming passed
     * elevation.
     */
    public boolean canGoUp(int assumedElevation, Coords assumedPos) {
        IHex hex = getGame().getBoard().getHex(assumedPos);
        int altitude = assumedElevation + hex.surface();
        int maxAlt = hex.surface();
        switch (getMovementMode()) {
        case IEntityMovementMode.INF_JUMP:
        case IEntityMovementMode.INF_LEG:
        case IEntityMovementMode.INF_MOTORIZED:
            maxAlt += Math.max(0, hex.terrainLevel(Terrains.BLDG_ELEV));
            break;
        case IEntityMovementMode.VTOL:
            maxAlt = hex.surface() + 50;
            break;
        case IEntityMovementMode.AERODYNE:
        case IEntityMovementMode.SPHEROID:
            if (game.getBoard().inAtmosphere()) {
                altitude = assumedElevation;
                maxAlt = 10;
            }
            break;
        case IEntityMovementMode.SUBMARINE:
        case IEntityMovementMode.INF_UMU:
        case IEntityMovementMode.BIPED_SWIM:
        case IEntityMovementMode.QUAD_SWIM:
            maxAlt = hex.surface();
            break;
        case IEntityMovementMode.WIGE:
            maxAlt = hex.surface() + 1;
            break;
        default:
            return false;
        }
        return (altitude < maxAlt);
    }

    /**
     * Check if this entity can legally occupy the requested elevation. Does not
     * check stacking, only terrain limitations
     */
    public boolean isElevationValid(int assumedElevation, IHex hex) {
        int altitude = assumedElevation + hex.surface();
        if (getMovementMode() == IEntityMovementMode.VTOL) {
            if ((this instanceof Infantry) && (hex.containsTerrain(Terrains.BUILDING) || hex.containsTerrain(Terrains.WOODS) || hex.containsTerrain(Terrains.JUNGLE))) {
                // VTOL BA (sylph) can move as ground unit as well
                return ((assumedElevation <= 50) && (altitude >= hex.floor()));
            } else if (hex.containsTerrain(Terrains.WOODS) || hex.containsTerrain(Terrains.WATER) || hex.containsTerrain(Terrains.JUNGLE)) {
                return ((assumedElevation <= 50) && (altitude > hex.ceiling()));
            }
            return ((assumedElevation <= 50) && (altitude >= hex.ceiling()));
        } else if ((getMovementMode() == IEntityMovementMode.SUBMARINE) || ((getMovementMode() == IEntityMovementMode.INF_UMU) && hex.containsTerrain(Terrains.WATER)) || ((getMovementMode() == IEntityMovementMode.QUAD_SWIM) && hasUMU()) || ((getMovementMode() == IEntityMovementMode.BIPED_SWIM) && hasUMU())) {
            return ((altitude >= hex.floor()) && (altitude <= hex.surface()));
        } else if ((getMovementMode() == IEntityMovementMode.HYDROFOIL) || (getMovementMode() == IEntityMovementMode.NAVAL)) {
            return altitude == hex.surface();
        } else if (getMovementMode() == IEntityMovementMode.WIGE) {
            // WiGEs can possibly be at any location above or on the surface
            return (altitude >= hex.floor());
        } else {
            // regular ground units
            if (hex.containsTerrain(Terrains.ICE) || ((getMovementMode() == IEntityMovementMode.HOVER) && hex.containsTerrain(Terrains.WATER))) {
                // surface of ice is OK, surface of water is OK for hovers
                if (altitude == hex.surface()) {
                    return true;
                }
            }
            // only mechs can move underwater
            if (hex.containsTerrain(Terrains.WATER) && (altitude < hex.surface()) && !(this instanceof Mech) && !(this instanceof Protomech)) {
                return false;
            }
            // can move on the ground unless its underwater
            if (altitude == hex.floor()) {
                return true;
            }
            if (hex.containsTerrain(Terrains.BRIDGE)) {
                // can move on top of a bridge
                if (assumedElevation == hex.terrainLevel(Terrains.BRIDGE_ELEV)) {
                    return true;
                }
            }
            if (hex.containsTerrain(Terrains.BUILDING)) {
                // Mechs, protos and infantry can occupy any floor in the
                // building
                if ((this instanceof Mech) || (this instanceof Protomech) || (this instanceof Infantry)) {
                    if ((altitude >= hex.floor()) && (altitude <= hex.ceiling())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Returns the height of the unit, that is, how many levels above it's
     * elevation is it for LOS purposes. Default is 0.
     */
    public int height() {
        return 0;
    }

    /**
     * Returns the absolute height of the entity
     */
    public int absHeight() {
        return getElevation() + height();
    }

    /**
     * Returns the display name for this entity.
     */
    public String getDisplayName() {
        if (displayName == null) {
            generateDisplayName();
        }
        return displayName;
    }

    /**
     * Generates the display name for this entity.
     * <p/>
     * Sub-classes are allowed to override this method. The display name is in
     * the format [Chassis] [Model] ([Player Name]).
     */
    public void generateDisplayName() {
        StringBuffer nbuf = new StringBuffer();
        nbuf.append(chassis);
        if ((model != null) && (model.length() > 0)) {
            nbuf.append(" ").append(model);
        }
        // if show unit id is on, append the id
        if (PreferenceManager.getClientPreferences().getShowUnitId()) {
            nbuf.append(" ID:").append(getId());
        } else if (duplicateMarker > 1) {
            // if not, and a player has more than one unit with the same name,
            // append "#N" after the model to differentiate.
            nbuf.append(" #" + duplicateMarker);
        }
        if (getOwner() != null) {
            nbuf.append(" (").append(getOwner().getName()).append(")");
        }
        if (PreferenceManager.getClientPreferences().getShowUnitId()) {
            nbuf.append(" ID:").append(getId());
        }

        displayName = nbuf.toString();
    }

    /**
     * A short name, suitable for displaying above a unit icon. The short name
     * is basically the same as the display name, minus the player name.
     */
    public String getShortName() {
        if (shortName == null) {
            generateShortName();
        }
        return shortName;
    }

    /**
     * Generate the short name for a unit
     * <p/>
     * Sub-classes are allowed to override this method. The display name is in
     * the format [Chassis] [Model].
     */
    public void generateShortName() {
        StringBuffer nbuf = new StringBuffer();
        nbuf.append(chassis);
        if ((model != null) && (model.length() > 0)) {
            nbuf.append(" ").append(model);
        }
        // if show unit id is on, append the id
        if (PreferenceManager.getClientPreferences().getShowUnitId()) {
            nbuf.append(" ID:").append(getId());
        } else if (duplicateMarker > 1) {
            // if not, and a player has more than one unit with the same name,
            // append "#N" after the model to differentiate.
            nbuf.append(" #" + duplicateMarker);
        }

        shortName = nbuf.toString();
    }

    public String getShortNameRaw() {
        StringBuffer nbuf = new StringBuffer();
        nbuf.append(chassis);
        if ((model != null) && (model.length() > 0)) {
            nbuf.append(" ").append(model);
        }
        return nbuf.toString();
    }

    /**
     * Returns the primary facing, or -1 if n/a
     */
    public int getFacing() {
        if (Entity.NONE != conveyance) {
            return game.getEntity(conveyance).getFacing();
        }
        return facing;
    }

    /**
     * Sets the primary facing.
     */
    public void setFacing(int facing) {
        this.facing = facing;
        if (game != null) {
            game.processGameEvent(new GameEntityChangeEvent(this, this));
        }
    }

    /**
     * Returns the secondary facing, or -1 if n/a
     */
    public int getSecondaryFacing() {
        return sec_facing;
    }

    /**
     * Sets the secondary facing.
     */
    public void setSecondaryFacing(int sec_facing) {
        this.sec_facing = sec_facing;
        if (game != null) {
            game.processGameEvent(new GameEntityChangeEvent(this, this));
        }
    }

    /**
     * Can this entity change secondary facing at all?
     */
    public abstract boolean canChangeSecondaryFacing();

    /**
     * Can this entity torso/turret twist the given direction?
     */
    public abstract boolean isValidSecondaryFacing(int dir);

    /**
     * Returns the closest valid secondary facing to the given direction.
     *
     * @return the the closest valid secondary facing.
     */
    public abstract int clipSecondaryFacing(int dir);

    /**
     * Returns true if the entity has an RAC which is jammed and not destroyed
     */
    public boolean canUnjamRAC() {
        for (Mounted mounted : getTotalWeaponList()) {
            WeaponType wtype = (WeaponType) mounted.getType();
            if ((wtype.getAmmoType() == AmmoType.T_AC_ROTARY) && mounted.isJammed() && !mounted.isDestroyed()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the entity can flip its arms
     */
    public boolean canFlipArms() {
        return false;
    }

    /**
     * Returns this entity's original walking movement points
     */
    public int getOriginalWalkMP() {
        return walkMP;
    }

    /**
     * Sets this entity's original walking movement points
     */
    public void setOriginalWalkMP(int walkMP) {
        this.walkMP = walkMP;
    }

    /**
     * Returns this entity's walking/cruising mp, factored for heat and gravity.
     */

    public int getWalkMP() {
        return getWalkMP(true, false);
    }

    /**
     * Returns this entity's walking/cruising mp, factored for heat and possibly
     * gravity.
     *
     * @param gravity
     *            Should the movement be factored for gravity
     * @param ignoreheat
     *            Should heat be ignored?
     */
    public int getWalkMP(boolean gravity, boolean ignoreheat) {
        int mp = getOriginalWalkMP();
        if (!ignoreheat) {
            mp = Math.max(0, mp - getHeatMPReduction());
        }
        mp = Math.max(mp - getCargoMpReduction(), 0);
        if (null != game) {
            int weatherMod = game.getPlanetaryConditions().getMovementMods(this);
            if (weatherMod != 0) {
                mp = Math.max(mp + weatherMod, 0);
            }
        }
        if (gravity) {
            mp = applyGravityEffectsOnMP(mp);
        }
        return mp;
    }

    /**
     * This returns how much MP is removed due to heat
     *
     * @return
     */
    public int getHeatMPReduction() {
        int minus;

        if ((game != null) && game.getOptions().booleanOption("tacops_heat")) {
            if (heat < 30) {
                minus = (heat / 5);
            } else if (heat >= 49) {
                minus = 9;
            } else if (heat >= 43) {
                minus = 8;
            } else if (heat >= 37) {
                minus = 7;
            } else if (heat >= 31) {
                minus = 6;
            } else {
                minus = 5;
            }
        } else {
            minus = heat / 5;
        }

        return minus;
    }

    /**
     * get the heat generated by this Entity when standing still
     */
    public int getStandingHeat() {
        return 0;
    }

    /**
     * get the heat generated by this Entity when walking/cruising
     */
    public int getWalkHeat() {
        return 0;
    }

    /**
     * Returns this entity's unmodified running/flank mp.
     */
    protected int getOriginalRunMP() {
        return (int) Math.ceil(getOriginalWalkMP() * 1.5);
    }

    /**
     * Returns this entity's running/flank mp modified for heat and gravity.
     */
    public int getRunMP() {
        return getRunMP(true, false);
    }

    public int getRunMP(boolean gravity, boolean ignoreheat) {
        return (int) Math.ceil(getWalkMP(gravity, ignoreheat) * 1.5);
    }

    /**
     * Returns run MP without considering MASC
     */
    public int getRunMPwithoutMASC() {
        return getRunMPwithoutMASC(true, false);
    }

    /**
     * Returns run MP without considering MASC, optionally figuring in gravity
     * and possibly ignoring heat
     */
    public abstract int getRunMPwithoutMASC(boolean gravity, boolean ignoreheat);

    /**
     * Returns this entity's running/flank mp as a string.
     */
    public String getRunMPasString() {
        return Integer.toString(getRunMP());
    }

    /**
     * get the heat generated by this Entity when running/flanking
     */
    public int getRunHeat() {
        return 0;
    }

    /**
     * Returns this entity's original jumping mp.
     */
    public int getOriginalJumpMP() {

        if (hasModularArmor()) {
            return jumpMP - 1;
        }

        return jumpMP;
    }

    /**
     * Sets this entity's original jump movement points
     */
    public void setOriginalJumpMP(int jumpMP) {
        this.jumpMP = jumpMP;
    }

    /**
     * Returns this entity's current jumping MP, not effected by terrain,
     * factored for gravity.
     */
    public int getJumpMP() {
        return getJumpMP(true);
    }

    /**
     * return this entity's current jump MP, possibly affected by gravity
     *
     * @param gravity
     * @return
     */
    public int getJumpMP(boolean gravity) {
        if (gravity) {
            return applyGravityEffectsOnMP(getOriginalJumpMP());
        }
        return getOriginalJumpMP();
    }

    public int getJumpType() {
        return 0;
    }

    /**
     * get the heat generated by this Entity when jumping for a certain amount
     * of MP
     *
     * @param movedMP
     *            the number of movement points spent
     */
    public int getJumpHeat(int movedMP) {
        return 0;
    }

    /**
     * Returns this entity's current jumping MP, effected by terrain (like
     * water.)
     */
    public int getJumpMPWithTerrain() {
        return getJumpMP();
    }

    /**
     * Returns the elevation that this entity would be on if it were placed into
     * the specified hex. Hovercraft, naval vessels, and hydrofoils move on the
     * surface of the water
     */
    public int elevationOccupied(IHex hex) {
        if (hex == null) {
            return 0;
        }
        if ((movementMode == IEntityMovementMode.VTOL) || (movementMode == IEntityMovementMode.WIGE)) {
            return hex.surface() + elevation;
        } else if (((movementMode == IEntityMovementMode.HOVER) || (movementMode == IEntityMovementMode.NAVAL) || (movementMode == IEntityMovementMode.HYDROFOIL) || hex.containsTerrain(Terrains.ICE)) && hex.containsTerrain(Terrains.WATER)) {
            return hex.surface();
        } else if (hex.containsTerrain(Terrains.BLDG_ELEV)) {
            return hex.floor() + hex.terrainLevel(Terrains.BLDG_ELEV);
        } else {
            return hex.floor();
        }
    }

    /**
     * Returns true if the specified hex contains some sort of prohibited
     * terrain.
     */
    public boolean isHexProhibited(IHex hex) {

        if (hex.containsTerrain(Terrains.IMPASSABLE)) {
            return true;
        }

        if (hex.containsTerrain(Terrains.SPACE) && doomedInSpace()) {
            return true;
        }

        return false;
    }

    /**
     * Returns true if the the given board is prohibited
     */
    public boolean isBoardProhibited(int mapType) {

        if ((mapType == Board.T_GROUND) && doomedOnGround()) {
            return true;
        }

        if ((mapType == Board.T_ATMOSPHERE) && doomedInAtmosphere()) {
            return true;
        }

        if ((mapType == Board.T_SPACE) && doomedInSpace()) {
            return true;
        }

        return false;
    }

    /**
     * Returns the name of the type of movement used.
     */
    public abstract String getMovementString(int mtype);

    /**
     * Returns the abbreviation of the name of the type of movement used.
     */
    public abstract String getMovementAbbr(int mtype);

    /**
     * Returns the name of the location specified.
     */
    public String getLocationName(HitData hit) {
        return getLocationName(hit.getLocation());
    }

    protected abstract String[] getLocationNames();

    /**
     * Returns the name of the location specified.
     */
    public String getLocationName(int loc) {
        String[] locationNames = getLocationNames();

        if ((null == locationNames) || (loc >= locationNames.length)) {
            return "";
        }

        return locationNames[loc];
    }

    protected abstract String[] getLocationAbbrs();

    /**
     * Returns the abbreviated name of the location specified.
     */
    public String getLocationAbbr(HitData hit) {
        return getLocationAbbr(hit.getLocation()) + (hit.isRear() && hasRearArmor(hit.getLocation()) ? "R" : "") + (hit.getEffect() == HitData.EFFECT_CRITICAL ? " (critical)" : "");
    }

    /**
     * Returns the abbreviated name of the location specified.
     */
    public String getLocationAbbr(int loc) {
        String[] locationAbbrs = getLocationAbbrs();

        if ((null == locationAbbrs) || (loc >= locationAbbrs.length)) {
            return "";
        }
        return locationAbbrs[loc];
    }

    /**
     * Returns the location that the specified abbreviation indicates
     */
    public int getLocationFromAbbr(String abbr) {
        for (int i = 0; i < locations(); i++) {
            if (getLocationAbbr(i).equalsIgnoreCase(abbr)) {
                return i;
            }
        }
        return Entity.LOC_NONE;
    }

    /**
     * Rolls the to-hit number
     */
    public abstract HitData rollHitLocation(int table, int side, int aimedLocation, int aimingMode);

    /**
     * Rolls up a hit location
     */
    public abstract HitData rollHitLocation(int table, int side);

    /**
     * Gets the location that excess damage transfers to. That is, one location
     * inwards.
     */
    public abstract HitData getTransferLocation(HitData hit);

    /** int version */
    public int getTransferLocation(int loc) {
        return getTransferLocation(new HitData(loc)).getLocation();
    }

    /**
     * Gets the location that is destroyed recursively. That is, one location
     * outwards.
     */
    public abstract int getDependentLocation(int loc);

    /**
     * Does this location have rear armor?
     */
    public abstract boolean hasRearArmor(int loc);

    /**
     * Returns the amount of armor in the location specified, or ARMOR_NA, or
     * ARMOR_DESTROYED. Only works on front locations.
     */
    public int getArmor(int loc) {
        return getArmor(loc, false);
    }

    /**
     * Returns the amount of armor in the location hit, or IArmorState.ARMOR_NA,
     * or IArmorState.ARMOR_DESTROYED.
     */
    public int getArmor(HitData hit) {
        return getArmor(hit.getLocation(), hit.isRear());
    }

    /**
     * Returns the amount of armor in the location specified, or
     * IArmorState.ARMOR_NA, or IArmorState.ARMOR_DESTROYED.
     */
    public int getArmor(int loc, boolean rear) {
        if (loc >= armor.length) {
            return IArmorState.ARMOR_NA;
        }
        return armor[loc];
    }

    /**
     * Returns the original amount of armor in the location specified. Only
     * works on front locations.
     */
    public int getOArmor(int loc) {
        return getOArmor(loc, false);
    }

    /**
     * Returns the original amount of armor in the location hit.
     */
    public int getOArmor(HitData hit) {
        return getOArmor(hit.getLocation(), hit.isRear());
    }

    /**
     * Returns the original amount of armor in the location specified, or
     * ARMOR_NA, or ARMOR_DESTROYED.
     *
     * @param loc
     *            the location to check.
     * @param rear
     *            if true inspect the rear armor, else check the front.
     */
    public int getOArmor(int loc, boolean rear) {
        return orig_armor[loc];
    }

    /**
     * Sets the amount of armor in the location specified.
     */
    public void setArmor(int val, HitData hit) {
        setArmor(val, hit.getLocation(), hit.isRear());
    }

    /**
     * Sets the amount of armor in the front location specified.
     */
    public void setArmor(int val, int loc) {
        setArmor(val, loc, false);
    }

    /**
     * Sets the amount of armor in the location specified.
     *
     * @param val
     *            the value of the armor (eg how many armor points)
     * @param loc
     *            the location of the armor
     * @param rear
     *            true iff the armor is rear mounted.
     */
    public void setArmor(int val, int loc, boolean rear) {
        armor[loc] = val;
    }

    public void refreshLocations() {
        armor = new int[locations()];
        internal = new int[locations()];
        orig_armor = new int[locations()];
        orig_internal = new int[locations()];
        crits = new CriticalSlot[locations()][];
        exposure = new int[locations()];
        for (int i = 0; i < locations(); i++) {
            crits[i] = new CriticalSlot[getNumberOfCriticals(i)];
        }
    }

    /**
     * Initializes the armor on the unit. Sets the original and starting point
     * of the armor to the same number.
     */
    public void initializeArmor(int val, int loc) {
        orig_armor[loc] = val;
        setArmor(val, loc);
    }

    /**
     * Returns the total amount of armor on the entity.
     */
    public int getTotalArmor() {
        int totalArmor = 0;
        for (int i = 0; i < locations(); i++) {
            if (getArmor(i) > 0) {
                totalArmor += getArmor(i);
            }
            if (hasRearArmor(i) && (getArmor(i, true) > 0)) {
                totalArmor += getArmor(i, true);
            }
        }
        return totalArmor;
    }

    /**
     * Returns the total amount of armor on the entity.
     */
    public int getTotalOArmor() {
        int totalArmor = 0;
        for (int i = 0; i < locations(); i++) {
            if (getOArmor(i) > 0) {
                totalArmor += getOArmor(i);
            }
            if (hasRearArmor(i) && (getOArmor(i, true) > 0)) {
                totalArmor += getOArmor(i, true);
            }
        }
        return totalArmor;
    }

    /**
     * Returns the percent of the armor remaining
     */
    public double getArmorRemainingPercent() {
        if (getTotalOArmor() == 0) {
            return IArmorState.ARMOR_NA;
        }
        return ((double) getTotalArmor() / (double) getTotalOArmor());
    }

    /**
     * Returns the amount of internal structure in the location hit.
     */
    public int getInternal(HitData hit) {
        return getInternal(hit.getLocation());
    }

    /**
     * Returns the amount of internal structure in the location specified, or
     * ARMOR_NA, or ARMOR_DESTROYED.
     */
    public int getInternal(int loc) {
        return internal[loc];
    }

    /**
     * Returns the original amount of internal structure in the location hit.
     */
    public int getOInternal(HitData hit) {
        return getOInternal(hit.getLocation());
    }

    /**
     * Returns the original amount of internal structure in the location
     * specified, or ARMOR_NA, or ARMOR_DESTROYED.
     */
    public int getOInternal(int loc) {
        return orig_internal[loc];
    }

    /**
     * Sets the amount of armor in the location specified.
     */
    public void setInternal(int val, HitData hit) {
        setInternal(val, hit.getLocation());
    }

    /**
     * Sets the amount of armor in the location specified.
     */
    public void setInternal(int val, int loc) {
        internal[loc] = val;
    }

    /**
     * Initializes the internal structure on the unit. Sets the original and
     * starting point of the internal structure to the same number.
     */
    public void initializeInternal(int val, int loc) {
        orig_internal[loc] = val;
        setInternal(val, loc);
    }

    /**
     * Set the internal structure to the appropriate value for the mech's weight
     * class
     */
    public abstract void autoSetInternal();

    /**
     * Returns the total amount of internal structure on the entity.
     */
    public int getTotalInternal() {
        int totalInternal = 0;
        for (int i = 0; i < locations(); i++) {
            if (getInternal(i) > 0) {
                totalInternal += getInternal(i);
            }
        }
        return totalInternal;
    }

    /**
     * Returns the total original amount of internal structure on the entity.
     */
    public int getTotalOInternal() {
        int totalInternal = 0;
        for (int i = 0; i < locations(); i++) {
            if (getOInternal(i) > 0) {
                totalInternal += getOInternal(i);
            }
        }
        return totalInternal;
    }

    /**
     * Returns the percent of the armor remaining
     */
    public double getInternalRemainingPercent() {
        return ((double) getTotalInternal() / (double) getTotalOInternal());
    }

    /**
     * Is this location destroyed or breached?
     */
    public boolean isLocationBad(int loc) {
        return getInternal(loc) == IArmorState.ARMOR_DESTROYED;
    }

    /**
     * Is this location destroyed or breached?
     */
    public boolean isLocationDoomed(int loc) {
        return getInternal(loc) == IArmorState.ARMOR_DOOMED;
    }

    /**
     * returns exposure or breached flag for location
     */
    public int getLocationStatus(int loc) {
        return exposure[loc];
    }

    /**
     * sets location exposure
     *
     * @param loc
     *            the location who's exposure is to be set
     * @param status
     *            the status to set
     */
    public void setLocationStatus(int loc, int status) {
        if (exposure[loc] > ILocationExposureStatus.BREACHED) { // can't change
            // BREACHED
            // status
            exposure[loc] = status;
        }
    }

    /**
     * Returns true is the location is a leg
     *
     * @param loc
     *            the location to check.
     */
    public boolean locationIsLeg(int loc) {
        return false;
    }

    /**
     * Returns a string representing the armor in the location
     */
    public String getArmorString(int loc) {
        return getArmorString(loc, false);
    }

    /**
     * Returns a string representing the armor in the location
     */
    public String getArmorString(int loc, boolean rear) {
        return Entity.armorStringFor(getArmor(loc, rear));
    }

    /**
     * Returns a string representing the internal structure in the location
     */
    public String getInternalString(int loc) {
        return Entity.armorStringFor(getInternal(loc));
    }

    /**
     * Parses the game's internal armor representation into a human-readable
     * string.
     */
    public static String armorStringFor(int value) {
        if (value == IArmorState.ARMOR_NA) {
            return "N/A";
        } else if ((value == IArmorState.ARMOR_DOOMED) || (value == IArmorState.ARMOR_DESTROYED)) {
            return "***";
        } else {
            return Integer.toString(value);
        }
    }

    /**
     * Returns the modifier to weapons fire due to heat.
     */
    public int getHeatFiringModifier() {
        int mod = 0;
        if (heat >= 8) {
            mod++;
        }
        if (heat >= 13) {
            mod++;
        }
        if (heat >= 17) {
            mod++;
        }
        if (heat >= 24) {
            mod++;
        }
        boolean mtHeat = game.getOptions().booleanOption("tacops_heat");
        if (mtHeat && (heat >= 33)) {
            mod++;
        }
        if (mtHeat && (heat >= 41)) {
            mod++;
        }
        if (mtHeat && (heat >= 48)) {
            mod++;
        }
        return mod;
    }

    /**
     * Creates a new mount for this equipment and adds it in.
     */
    public Mounted addEquipment(EquipmentType etype, int loc) throws LocationFullException {
        return addEquipment(etype, loc, false);
    }

    /**
     * Creates a new mount for this equipment and adds it in.
     */
    public Mounted addEquipment(EquipmentType etype, int loc, boolean rearMounted) throws LocationFullException {
        return addEquipment(etype, loc, rearMounted, false, false);
    }

    /**
     * Creates a new mount for this equipment and adds it in.
     */
    public Mounted addEquipment(EquipmentType etype, int loc, boolean rearMounted, boolean bodyMounted, boolean isArmored) throws LocationFullException {
        Mounted mounted = new Mounted(this, etype);
        mounted.setArmored(isArmored);
        mounted.setBodyMounted(bodyMounted);
        addEquipment(mounted, loc, rearMounted);
        return mounted;
    }

    /**
     * mounting weapons needs to take account of ammo
     *
     * @param etype
     * @param loc
     * @param rearMounted
     * @param nAmmo
     * @return
     * @throws LocationFullException
     */
    public Mounted addEquipment(EquipmentType etype, int loc, boolean rearMounted, int nAmmo) throws LocationFullException {
        Mounted mounted = new Mounted(this, etype);
        addEquipment(mounted, loc, rearMounted, nAmmo);
        return mounted;

    }

    /*
     * indicate whether this is a bomb mount
     */
    public Mounted addBomb(EquipmentType etype, int loc) throws LocationFullException {
        Mounted mounted = new Mounted(this, etype);
        addBomb(mounted, loc);
        return mounted;
    }

    public Mounted addWeaponGroup(EquipmentType etype, int loc) throws LocationFullException {
        Mounted mounted = new Mounted(this, etype);
        addEquipment(mounted, loc, false, true);
        return mounted;
    }

    /**
     * indicate whether this is bodymounted for BAs
     */
    public Mounted addEquipment(EquipmentType etype, int loc, boolean rearMounted, boolean bodyMounted) throws LocationFullException {
        Mounted mounted = new Mounted(this, etype);
        mounted.setBodyMounted(bodyMounted);
        addEquipment(mounted, loc, rearMounted);
        return mounted;
    }

    protected void addEquipment(Mounted mounted, int loc, boolean rearMounted, int nAmmo) throws LocationFullException {
        if ((mounted.getType() instanceof AmmoType) && (nAmmo > 1)) {
            mounted.setByShot(true);
            mounted.setShotsLeft(nAmmo);
        }

        addEquipment(mounted, loc, rearMounted);
    }

    protected void addBomb(Mounted mounted, int loc) throws LocationFullException {
        mounted.setBombMounted(true);
        addEquipment(mounted, loc, false);
    }

    protected void addEquipment(Mounted mounted, int loc, boolean rearMounted, boolean isWeaponGroup) throws LocationFullException {
        mounted.setWeaponGroup(true);

        addEquipment(mounted, loc, rearMounted);
    }

    protected void addEquipment(Mounted mounted, int loc, boolean rearMounted) throws LocationFullException {
        mounted.setLocation(loc, rearMounted);
        equipmentList.add(mounted);

        // add it to the proper sub-list
        if (mounted.getType() instanceof WeaponType) {
            totalWeaponList.add(mounted);
            if (mounted.isWeaponGroup()) {
                weaponGroupList.add(mounted);
            } else if (mounted.getType() instanceof BayWeapon) {
                weaponBayList.add(mounted);
            } else {
                weaponList.add(mounted);
            }
            if (mounted.getType().hasFlag(WeaponType.F_ARTILLERY)) {
                aTracker.addWeapon(mounted);
            }

            // one-shot launchers need their single shot of ammo added.
            if (mounted.getType().hasFlag(WeaponType.F_ONESHOT)) {
                Mounted m = new Mounted(this, AmmoType.getOneshotAmmo(mounted));
                m.setShotsLeft(1);
                mounted.setLinked(m);
                // Oneshot ammo will be identified by having a location
                // of null. Other areas in the code will rely on this.
                addEquipment(m, Entity.LOC_NONE, false);
            }
        }
        if (mounted.getType() instanceof AmmoType) {
            ammoList.add(mounted);
        }
        if (mounted.getType() instanceof BombType) {
            bombList.add(mounted);
        }
        if (mounted.getType() instanceof MiscType) {
            miscList.add(mounted);
        }
    }

    public void addFailedEquipment(String s) {
        failedEquipmentList.add(s);
    }

    /**
     * Returns the equipment number of the specified equipment, or -1 if
     * equipment is not present.
     */
    public int getEquipmentNum(Mounted mounted) {
        if (mounted != null) {
            return equipmentList.indexOf(mounted);
        }
        return -1;
    }

    /**
     * Returns an enumeration of all equipment
     */
    public ArrayList<Mounted> getEquipment() {
        return equipmentList;
    }

    /**
     * Returns the equipment, specified by number
     */
    public Mounted getEquipment(int index) {
        try {
            return equipmentList.get(index);
        } catch (IndexOutOfBoundsException ex) {
            return null;
        }
    }

    public EquipmentType getEquipmentType(CriticalSlot cs) {
        if (cs.getType() != CriticalSlot.TYPE_EQUIPMENT) {
            return null;
        }
        Mounted m = equipmentList.get(cs.getIndex());
        return m.getType();
    }

    /**
     * Returns an enumeration which contains the name of each piece of equipment
     * that failed to load.
     */
    public Iterator<String> getFailedEquipment() {
        return failedEquipmentList.iterator();
    }

    public int getTotalAmmoOfType(EquipmentType et) {
        int totalShotsLeft = 0;
        for (Mounted amounted : getAmmo()) {
            if ((amounted.getType() == et) && !amounted.isDumping()) {
                totalShotsLeft += amounted.getShotsLeft();
            }
        }
        return totalShotsLeft;
    }

    /**
     * Determine how much ammunition (of all munition types) remains which is
     * compatable with the given ammo.
     *
     * @param et
     *            - the <code>EquipmentType</code> of the ammo to be found. This
     *            value may be <code>null</code>.
     * @return the <code>int</code> count of the amount of shots of all
     *         munitions equivalent to the given ammo type.
     */
    public int getTotalMunitionsOfType(EquipmentType et) {
        int totalShotsLeft = 0;
        for (Mounted amounted : getAmmo()) {
            if (amounted.getType().equals(et) && !amounted.isDumping()) {
                totalShotsLeft += amounted.getShotsLeft();
            }
        }
        return totalShotsLeft;
    }

    /**
     * Returns the Rules.ARC that the weapon, specified by number, fires into.
     */
    public abstract int getWeaponArc(int wn);

    /**
     * Returns true if this weapon fires into the secondary facing arc. If
     * false, assume it fires into the primary.
     */
    public abstract boolean isSecondaryArcWeapon(int weaponId);

    public Iterator<Mounted> getWeapons() {
        if (usesWeaponBays()) {
            return weaponBayList.iterator();
        }
        if (isCapitalFighter()) {
            return weaponGroupList.iterator();
        }

        return weaponList.iterator();
    }

    public ArrayList<Mounted> getWeaponList() {
        if (usesWeaponBays()) {
            return weaponBayList;
        }
        if (isCapitalFighter()) {
            return weaponGroupList;
        }

        return weaponList;
    }

    public ArrayList<Mounted> getTotalWeaponList() {
        // return full weapon list even bay mounts and weapon groups
        return totalWeaponList;
    }

    public ArrayList<Mounted> getWeaponBayList() {
        return weaponBayList;
    }

    public ArrayList<Mounted> getWeaponGroupList() {
        return weaponGroupList;
    }

    /**
     * Returns the first ready weapon
     *
     * @return the index number of the first available weapon, or -1 if none are
     *         ready.
     */
    public int getFirstWeapon() {
        for (Mounted mounted : getWeaponList()) {
            if (mounted.isReady()) {
                return getEquipmentNum(mounted);
            }
        }
        return -1;
    }

    /**
     * Returns the next ready weapon, starting at the specified index
     */
    public int getNextWeapon(int start) {
        boolean past = false;
        for (Mounted mounted : getWeaponList()) {
            // FIXME
            // Logic must be inserted here to NOT always skip AMS once the
            // MaxTech rule for firing AMSes is implemented.
            if (past && (mounted != null) && (mounted.isReady()) && (!mounted.getType().hasFlag(WeaponType.F_AMS)) && ((mounted.getLinked() == null) || (mounted.getLinked().getShotsLeft() > 0))) {
                if (mounted.getType().hasFlag(WeaponType.F_TAG) && (game.getPhase() == IGame.Phase.PHASE_FIRING)) {
                    continue;
                }
                if (mounted.getType().hasFlag(WeaponType.F_MG)) {
                    if (hasLinkedMGA(mounted)) {
                        continue;
                    }
                }

                return getEquipmentNum(mounted);
            }

            if (getEquipmentNum(mounted) == start) {
                past = true;
                continue;
            }
            if (past && (getEquipmentNum(mounted) == start)) {
                return getFirstWeapon();
            }
        }
        return getFirstWeapon();
    }

    /**
     * Attempts to load all weapons with ammo
     */
    public void loadAllWeapons() {
        for (Mounted mounted : getTotalWeaponList()) {
            WeaponType wtype = (WeaponType) mounted.getType();
            if (wtype.getAmmoType() != AmmoType.T_NA) {
                loadWeapon(mounted);
            }
        }
    }

    /**
     * Tries to load the specified weapon with the first available ammo
     */
    public void loadWeapon(Mounted mounted) {
        for (Mounted mountedAmmo : getAmmo()) {
            if (loadWeapon(mounted, mountedAmmo)) {
                break;
            }
        }
    }

    /**
     * Tries to load the specified weapon with the first available ammo of the
     * same munition type as currently in use. If this fails, use first ammo.
     */
    public void loadWeaponWithSameAmmo(Mounted mounted) {
        for (Mounted mountedAmmo : getAmmo()) {
            if (loadWeaponWithSameAmmo(mounted, mountedAmmo)) {
                return;
            }
        }
        // fall back to use any ammo
        loadWeapon(mounted);
    }

    /**
     * Tries to load the specified weapon with the specified ammo. Returns true
     * if successful, false otherwise.
     */
    public boolean loadWeapon(Mounted mounted, Mounted mountedAmmo) {
        boolean success = false;
        WeaponType wtype = (WeaponType) mounted.getType();
        AmmoType atype = (AmmoType) mountedAmmo.getType();

        if (mountedAmmo.isAmmoUsable() && !wtype.hasFlag(WeaponType.F_ONESHOT) && (atype.getAmmoType() == wtype.getAmmoType()) && (atype.getRackSize() == wtype.getRackSize())) {
            mounted.setLinked(mountedAmmo);
            success = true;
        }
        return success;
    }

    /**
     * Tries to load the specified weapon with the specified ammo. Returns true
     * if successful, false otherwise.
     */
    public boolean loadWeaponWithSameAmmo(Mounted mounted, Mounted mountedAmmo) {
        AmmoType atype = (AmmoType) mountedAmmo.getType();
        Mounted oldammo = mounted.getLinked();

        if ((oldammo != null) && (((AmmoType) oldammo.getType()).getMunitionType() != atype.getMunitionType())) {
            return false;
        }

        return loadWeapon(mounted, mountedAmmo);
    }

    /**
     * Checks whether a weapon has been fired from the specified location this
     * turn
     */
    public boolean weaponFiredFrom(int loc) {
        // check critical slots for used weapons
        for (int i = 0; i < this.getNumberOfCriticals(loc); i++) {
            CriticalSlot slot = getCritical(loc, i);
            // ignore empty & system slots
            if ((slot == null) || (slot.getType() != CriticalSlot.TYPE_EQUIPMENT)) {
                continue;
            }
            Mounted mounted = getEquipment(slot.getIndex());
            if ((mounted.getType() instanceof WeaponType) && mounted.isUsedThisRound()) {
                return true;
            }
        }
        return false;
    }

    public ArrayList<Mounted> getAmmo() {
        return ammoList;
    }

    public ArrayList<Mounted> getMisc() {
        return miscList;
    }

    public ArrayList<Mounted> getBombs() {
        return bombList;
    }

    /**
     * Removes the first misc eq. whose name equals the specified string. Used
     * for removing broken tree clubs.
     */
    public void removeMisc(String toRemove) {
        for (Mounted mounted : getMisc()) {
            if (mounted.getName().equals(toRemove)) {
                miscList.remove(mounted);
                equipmentList.remove(mounted);
                break;
            }
        }
    }

    public List<Mounted> getClubs() {
        List<Mounted> rv = new ArrayList<Mounted>();
        for (Mounted m : getMisc()) {
            if (m.getType().hasFlag(MiscType.F_CLUB)) {
                rv.add(m);
            }
        }
        return rv;
    }

    /**
     * Check if the entity has an arbritrary type of misc equipment
     *
     * @param flag
     *            A MiscType.F_XXX
     * @param secondary
     *            A MiscType.S_XXX or -1 for don't care
     * @return true if at least one ready item.
     */
    public boolean hasWorkingMisc(long flag, long secondary) {
        for (Mounted m : miscList) {
            if ((m.getType() instanceof MiscType) && m.isReady()) {
                MiscType type = (MiscType) m.getType();
                if (type.hasFlag(flag) && ((secondary == -1) || type.hasSubType(secondary))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if the entity has an arbritrary type of misc equipment
     *
     * @param flag
     *            A MiscType.F_XXX
     * @param secondary
     *            A MiscType.S_XXX or -1 for don't care
     * @param location
     *            The location to check e.g. Mech.LOC_LARM
     * @return true if at least one ready item.
     */
    public boolean hasWorkingMisc(long flag, int secondary, int location) {
        for (Mounted m : miscList) {
            if ((m.getType() instanceof MiscType) && m.isReady() && (m.getLocation() == location)) {
                MiscType type = (MiscType) m.getType();
                if (type.hasFlag(flag) && ((secondary == -1) || type.hasSubType(secondary))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the amount of heat that the entity can sink each turn.
     */
    public abstract int getHeatCapacity();

    /**
     * Returns the amount of heat that the entity can sink each turn, factoring
     * in whether the entity is standing in water.
     */
    public abstract int getHeatCapacityWithWater();

    /**
     * Returns extra heat generated by engine crits
     */
    public abstract int getEngineCritHeat();

    /**
     * Returns a critical hit slot
     */
    public CriticalSlot getCritical(int loc, int slot) {
        return crits[loc][slot];
    }

    /**
     * Sets a critical hit slot
     */
    public void setCritical(int loc, int slot, CriticalSlot cs) {
        crits[loc][slot] = cs;
    }

    /**
     * Adds a critical to the first available slot in the location.
     *
     * @return true if there was room for the critical
     */
    public boolean addCritical(int loc, CriticalSlot cs) {
        for (int i = 0; i < getNumberOfCriticals(loc); i++) {
            if (getCritical(loc, i) == null) {
                crits[loc][i] = cs;
                return true;
            }
        }
        return false; // no slot available :(
    }

    /**
     * Attempts to set the given slot to the given critical. If the desired slot
     * is full, adds the critical to the first available slot.
     *
     * @return true if the crit was succesfully added to any slot
     */
    public boolean addCritical(int loc, int slot, CriticalSlot cs) {
        if (getCritical(loc, slot) == null) {
            setCritical(loc, slot, cs);
            return true;
        }
        return addCritical(loc, cs);
    }

    /**
     * Removes all matching critical slots from the location
     */
    public void removeCriticals(int loc, CriticalSlot cs) {
        for (int i = 0; i < getNumberOfCriticals(loc); i++) {
            if ((getCritical(loc, i) != null) && getCritical(loc, i).equals(cs)) {
                setCritical(loc, i, null);
            }
        }
    }

    /**
     * Returns the number of empty critical slots in a location
     */
    public int getEmptyCriticals(int loc) {
        int empty = 0;

        for (int i = 0; i < getNumberOfCriticals(loc); i++) {
            if (getCritical(loc, i) == null) {
                empty++;
            }
        }
        return empty;
    }

    /**
     * Returns the number of operational critical slots remaining in a location
     */
    public int getHittableCriticals(int loc) {
        int hittable = 0;

        for (int i = 0; i < getNumberOfCriticals(loc); i++) {
            if ((getCritical(loc, i) != null) && getCritical(loc, i).isHittable()) {
                hittable++;
            }
        }
        return hittable;
    }

    /**
     * Returns true if this location should transfer criticals to the next
     * location inwards. Checks to see that every critical in this location is
     * either already totally destroyed (not just hit) or was never hittable to
     * begin with.
     */
    public boolean canTransferCriticals(int loc) {
        for (int i = 0; i < getNumberOfCriticals(loc); i++) {
            CriticalSlot crit = getCritical(loc, i);
            if ((crit != null) && !crit.isDestroyed() && crit.isEverHittable()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Only Mechs have Gyros but this helps keep the code a bit cleaner.
     *
     * @return <code>-1</code>
     */
    public int getGyroType() {
        return -1;
    }

    /**
     * Returns the number of operational critical slots of the specified type in
     * the location
     */
    public int getGoodCriticals(CriticalSlot cs, int loc) {
        return getGoodCriticals(cs.getType(), cs.getIndex(), loc);
    }

    /**
     * Returns the number of operational critical slots of the specified type in
     * the location
     */
    public int getGoodCriticals(int type, int index, int loc) {
        int operational = 0;

        int numberOfCriticals = getNumberOfCriticals(loc);
        for (int i = 0; i < numberOfCriticals; i++) {
            CriticalSlot ccs = getCritical(loc, i);

            if ((ccs != null) && (ccs.getType() == type) && (ccs.getIndex() == index) && !ccs.isDestroyed() && !ccs.isBreached()) {
                operational++;
            }
        }
        return operational;
    }

    /**
     * The number of critical slots that are destroyed in the component.
     */
    public int getBadCriticals(int type, int index, int loc) {
        int hits = 0;

        int numberOfCriticals = getNumberOfCriticals(loc);
        for (int i = 0; i < numberOfCriticals; i++) {
            CriticalSlot ccs = getCritical(loc, i);

            if ((ccs != null) && (ccs.getType() == type) && (ccs.getIndex() == index)) {
                if (ccs.isDestroyed() || ccs.isBreached()) {
                    hits++;
                }
            }
        }
        return hits;
    }

    /**
     * Number of slots doomed, missing or destroyed
     */
    public int getHitCriticals(int type, int index, int loc) {
        int hits = 0;
        int numCrits = getNumberOfCriticals(loc);
        for (int i = 0; i < numCrits; i++) {
            CriticalSlot ccs = getCritical(loc, i);

            if ((ccs != null) && (ccs.getType() == type) && (ccs.getIndex() == index)) {
                if (ccs.isDamaged() || ccs.isBreached()) {
                    hits++;
                }
            }
        }
        return hits;
    }

    protected abstract int[] getNoOfSlots();

    /**
     * Returns the number of total critical slots in a location
     */
    public int getNumberOfCriticals(int loc) {
        int[] noOfSlots = getNoOfSlots();
        if ((null == noOfSlots) || (loc >= noOfSlots.length) || (loc == LOC_NONE)) {
            return 0;
        }
        return noOfSlots[loc];
    }

    /**
     * Returns the number of critical slots present in the section, destroyed or
     * not.
     */
    public int getNumberOfCriticals(int type, int index, int loc) {
        int num = 0;
        int numCrits = getNumberOfCriticals(loc);
        for (int i = 0; i < numCrits; i++) {
            CriticalSlot ccs = getCritical(loc, i);
            if ((ccs != null) && (ccs.getType() == type) && (ccs.getIndex() == index)) {
                num++;
            }
        }
        return num;
    }

    /**
     * Returns the number of critical slots present in the section, destroyed or
     * not.
     */
    public int getNumberOfCriticals(EquipmentType etype, int loc) {
        int num = 0;
        int numberOfCriticals = getNumberOfCriticals(loc);
        for (int i = 0; i < numberOfCriticals; i++) {
            CriticalSlot ccs = getCritical(loc, i);
            if ((ccs != null) && (getEquipmentType(ccs) != null) && getEquipmentType(ccs).equals(etype)) {
                num++;
            }
        }
        return num;
    }

    /**
     * Returns the number of critical slots present in the mech, destroyed or
     * not.
     */
    public int getNumberOfCriticals(EquipmentType etype) {
        int num = 0;
        int locations = locations();
        for (int l = 0; l < locations; l++) {
            num += getNumberOfCriticals(etype, l);
        }
        return num;
    }

    /**
     * Returns how many of the given equipment are present in the mech,
     * destroyed or not.
     */
    public int getNumberOf(EquipmentType etype) {
        int total = 0;
        for (Mounted m : equipmentList) {
            if (m.getType().equals(etype)) {
                total++;
            }
        }
        return total;
    }

    /**
     * Returns true if the entity has a hip crit. Overridden by sub-classes.
     */
    public boolean hasHipCrit() {
        return false;
    }

    /**
     * Returns true if the entity has a leg actuator crit
     */
    public boolean hasLegActuatorCrit() {
        boolean hasCrit = false;

        for (int i = 0; i < locations(); i++) {
            if (locationIsLeg(i)) {
                if ((getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_HIP, i) > 0) || (getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_UPPER_LEG, i) > 0) || (getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_LOWER_LEG, i) > 0) || (getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_FOOT, i) > 0)) {
                    hasCrit = true;
                    break;
                }
            }
        }
        return hasCrit;
    }

    /**
     * Returns true if there is at least 1 functional system of the type
     * specified in the location
     */
    public boolean hasWorkingSystem(int system, int loc) {
        for (int i = 0; i < getNumberOfCriticals(loc); i++) {
            CriticalSlot ccs = getCritical(loc, i);
            if ((ccs != null) && (ccs.getType() == CriticalSlot.TYPE_SYSTEM) && (ccs.getIndex() == system) && !ccs.isDamaged() && !ccs.isBreached()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the the location has a system of the type, whether is
     * destroyed or not
     */
    public boolean hasSystem(int system, int loc) {
        for (int i = 0; i < getNumberOfCriticals(loc); i++) {
            CriticalSlot ccs = getCritical(loc, i);
            if ((ccs != null) && (ccs.getType() == CriticalSlot.TYPE_SYSTEM) && (ccs.getIndex() == system)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks to see if this entity is wielding any vibroblades
     *
     * @return always returns <code>false</code> as Only biped mechs can wield
     *         vibroblades
     */
    public boolean hasVibroblades() {
        return false;
    }

    /**
     * Checks to see if any heat is given off by an active vibro blade
     *
     * @param location
     * @return always returns <code>0</code> as Only biped mechs can wield
     *         vibroblades
     */
    public int getActiveVibrobladeHeat(int location) {
        return 0;
    }

    /**
     * Does the mech have any shields. a mech can have up to 2 shields.
     *
     * @return <code>true</code> if <code>shieldCount</code> is greater then 0
     *         else <code>false</code>
     */
    public boolean hasShield() {
        return false;
    }

    /**
     * Check to see how many shields of a certian size a mek has. you can have
     * up to shields per mech. However they can be of different size and each
     * size has its own draw backs. So check each size and add modifers based on
     * the number shields of that size.
     */
    public int getNumberOfShields(long size) {
        return 0;
    }

    /**
     * Does the mech have an active shield This should only be called after
     * hasShield has been called.
     */
    public boolean hasActiveShield(int location, boolean rear) {
        return true;
    }

    /**
     * Does the mech have an active shield This should only be called by
     * hasActiveShield(location,rear)
     */
    public boolean hasActiveShield(int location) {
        return false;
    }

    /**
     * Does the mech have a passive shield This should only be called after
     * hasShield has been called.
     */
    public boolean hasPassiveShield(int location, boolean rear) {
        return false;
    }

    /**
     * Does the mech have a passive shield This should only be called by
     * hasPassiveShield(location,rear)
     */
    public boolean hasPassiveShield(int location) {
        return false;
    }

    /**
     * Does the mech have an shield in no defense mode
     */
    public boolean hasNoDefenseShield(int location) {
        return false;
    }

    /**
     * This method checks to see if a unit has Underwater Maneuvering Units Only
     * Battle Mechs may have UMU's
     *
     * @return <code>boolean</code> if the entity has usable UMU crits.
     */
    public boolean hasUMU() {
        if (!(this instanceof Mech)) {
            return false;
        }

        int umuCount = getActiveUMUCount();

        return umuCount > 0;
    }

    /**
     * This counts the number of UMU's a Mech has that are still viable
     *
     * @return number <code>int</code>of useable UMU's
     */
    public int getActiveUMUCount() {
        int count = 0;

        if (hasShield() && (getNumberOfShields(MiscType.S_SHIELD_LARGE) > 0)) {
            return 0;
        }

        for (Mounted m : getMisc()) {
            EquipmentType type = m.getType();
            if ((type instanceof MiscType) && type.hasFlag(MiscType.F_UMU) && !(m.isDestroyed() || m.isMissing() || m.isBreached())) {
                count++;
            }
        }

        return count;
    }

    /**
     * This returns all UMU a mech has.
     *
     * @return <code>int</code>Total number of UMUs a mech has.
     */
    public int getAllUMUCount() {
        int count = 0;

        if (!(this instanceof Mech)) {
            return 0;
        }

        if (hasShield() && (getNumberOfShields(MiscType.S_SHIELD_LARGE) > 0)) {
            return 0;
        }

        for (Mounted m : getMisc()) {
            EquipmentType type = m.getType();
            if ((type instanceof MiscType) && type.hasFlag(MiscType.F_UMU)) {
                count++;
            }
        }

        return count;
    }

    /**
     * Does the mech have a functioning ECM unit?
     */
    public boolean hasActiveECM() {
        // no ECM in space unless strat op option enabled
        if (game.getBoard().inSpace() && !game.getOptions().booleanOption("stratops_ecm")) {
            return false;
        }
        if (!isShutDown()) {
            for (Mounted m : getMisc()) {
                EquipmentType type = m.getType();
                // TacOps p. 100 Angle ECM can have 1 ECM and 1 ECCM at the same
                // time
                if ((type instanceof MiscType) && type.hasFlag(MiscType.F_ECM) && (m.curMode().equals("ECM") || m.curMode().equals("ECM & ECCM") || m.curMode().equals("ECM & Ghost Targets"))) {
                    return !(m.isInoperable());
                }
            }
        }
        return false;
    }

    /**
     * Does the mech have a functioning ECM unit?
     */
    public boolean hasActiveAngelECM() {
        // no ECM in space unless strat op option enabled
        if (game.getBoard().inSpace() && !game.getOptions().booleanOption("stratops_ecm")) {
            return false;
        }
        if (game.getOptions().booleanOption("tacops_angel_ecm") && !isShutDown()) {
            for (Mounted m : getMisc()) {
                EquipmentType type = m.getType();
                if ((type instanceof MiscType) && type.hasFlag(MiscType.F_ANGEL_ECM) && m.curMode().equals("ECM")) {
                    return !(m.isInoperable());
                }
            }
        }
        return false;
    }

    /**
     * Does the mech have a functioning ECM unit, tuned to ghost target
     * generation?
     */

    /**
     * Does the mech have a functioning ECM unit, tuned to ghost target
     * generation?
     */
    public boolean hasGhostTargets(boolean active) {
        // no Ghost Targets in space unless strat op option enabled
        if (game.getBoard().inSpace()) {
            return false;
        }

        // if you failed your ghost target PSR, then it doesn't matter
        if ((active && (getGhostTargetRollMoS() < 0)) || isShutDown()) {
            return false;
        }
        boolean hasGhost = false;
        for (Mounted m : getMisc()) {
            EquipmentType type = m.getType();
            // TacOps p. 100 Angle ECM can have ECM/ECCM and Ghost Targets at
            // the same time
            if ((type instanceof MiscType) && type.hasFlag(MiscType.F_ECM) && (m.curMode().equals("Ghost Targets") || m.curMode().equals("ECM & Ghost Targets") || m.curMode().equals("ECCM & Ghost Targets")) && !(m.isInoperable() || getCrew().isUnconscious())) {
                hasGhost = true;
            }
            if ((type instanceof MiscType) && type.hasFlag(MiscType.F_COMMUNICATIONS) && m.curMode().equals("Ghost Targets") && (getTotalCommGearTons() >= 7) && !(m.isInoperable() || getCrew().isUnconscious())) {
                hasGhost = true;
            }
        }
        return hasGhost;
    }

    /**
     * Checks to see if this entity has a functional ECM unit that is using
     * ECCM.
     *
     * @return <code>true</code> if the entity has angelecm and it is in ECCM
     *         mode <code>false</code> if the entity does not have angel ecm or
     *         it is not in eccm mode or it is damaged.
     */
    public boolean hasActiveECCM() {
        // no ECM in space unless strat op option enabled
        if (game.getBoard().inSpace() && !game.getOptions().booleanOption("stratops_ecm")) {
            return false;
        }
        if ((game.getOptions().booleanOption("tacops_eccm") || game.getOptions().booleanOption("stratops_ecm")) && !isShutDown()) {
            for (Mounted m : getMisc()) {
                EquipmentType type = m.getType();
                // TacOps p. 100 Angle ECM can have 1 ECM and 1 ECCM at the same
                // time
                if ((type instanceof MiscType) && ((type.hasFlag(MiscType.F_ECM) && (m.curMode().equals("ECCM") || m.curMode().equals("ECM & ECCM") || m.curMode().equals("ECCM & Ghost Targets"))) || (type.hasFlag(MiscType.F_COMMUNICATIONS) && m.curMode().equals("ECCM")))) {
                    return !m.isInoperable();
                }
            }
        }
        return false;
    }

    /**
     * Checks to see if this unit has a functional AngelECM unit that is using
     * ECCM.
     *
     * @return <code>true</code> if the entity has angelecm and it is in ECCM
     *         mode <code>false</code> if the entity does not have angel ecm or
     *         it is not in eccm mode or it is damaged.
     */
    public boolean hasActiveAngelECCM() {
        if (game.getOptions().booleanOption("tacops_angel_ecm") && game.getOptions().booleanOption("tacops_eccm") && !isShutDown()) {
            for (Mounted m : getMisc()) {
                EquipmentType type = m.getType();
                if ((type instanceof MiscType) && type.hasFlag(MiscType.F_ANGEL_ECM) && m.curMode().equals("ECCM")) {
                    return !(m.isDestroyed() || m.isMissing() || m.isBreached() || isShutDown());
                }
            }
        }
        return false;
    }

    /**
     * What's the range of the ECM equipment? Infantry can have ECM that just
     * covers their own hex.
     *
     * @return the <code>int</code> range of this unit's ECM. This value will be
     *         <code>Entity.NONE</code> if no ECM is active.
     */
    public int getECMRange() {
        // no ECM in space unless strat op option enabled
        if (game.getBoard().inSpace() && !game.getOptions().booleanOption("stratops_ecm")) {
            return Entity.NONE;
        }

        if (!isShutDown()) {
            for (Mounted m : getMisc()) {
                EquipmentType type = m.getType();
                if ((type instanceof MiscType) && type.hasFlag(MiscType.F_ECM) && !m.isInoperable()) {
                    if (BattleArmor.SINGLE_HEX_ECM.equals(type.getInternalName())) {
                        return 0;
                    }
                    if (game.getPlanetaryConditions().hasEMI()) {
                        return 12;
                    }
                    return 6;
                }
            }
        }
        return Entity.NONE;
    }

    /**
     * Does the mech have a functioning BAP? This is just for the basic BAP for
     * Beagle BloodHound WatchDog Clan Active or Light.
     */
    public boolean hasBAP() {
        return hasBAP(true);
    }

    public boolean hasBAP(boolean checkECM) {
        if (((game != null) && game.getPlanetaryConditions().hasEMI()) || isShutDown()) {
            return false;
        }
        for (Mounted m : getMisc()) {
            EquipmentType type = m.getType();
            if ((type instanceof MiscType) && type.hasFlag(MiscType.F_BAP)) {

                if (!m.isInoperable()) {
                    // Beagle Isn't effected by normal ECM
                    if (type.getName().equals("Beagle Active Probe")) {

                        if (checkECM && Compute.isAffectedByAngelECM(this, getPosition(), getPosition())) {
                            return false;
                        }
                        return true;
                    }
                    return !checkECM || !Compute.isAffectedByECM(this, getPosition(), getPosition());
                }
            }
        }
        // check for Manei Domini implants
        if (((crew.getOptions().booleanOption("cyber_eye_im") || crew.getOptions().booleanOption("mm_eye_im")) && (this instanceof Infantry) && !(this instanceof BattleArmor)) || (crew.getOptions().booleanOption("mm_eye_im") && (crew.getOptions().booleanOption("vdni") || crew.getOptions().booleanOption("bvdni")))) {
            return !checkECM || !Compute.isAffectedByECM(this, getPosition(), getPosition());
        }

        return false;
    }

    /**
     * What's the range of the BAP equipment?
     *
     * @return the <code>int</code> range of this unit's BAP. This value will be
     *         <code>Entity.NONE</code> if no BAP is active.
     */
    public int getBAPRange() {
        if (game.getPlanetaryConditions().hasEMI() || isShutDown()) {
            return Entity.NONE;
        }
        // check for Manei Domini implants
        int cyberBonus = 0;
        if (((crew.getOptions().booleanOption("cyber_eye_im") || crew.getOptions().booleanOption("mm_eye_im")) && (this instanceof Infantry) && !(this instanceof BattleArmor)) || (crew.getOptions().booleanOption("mm_eye_im") && (crew.getOptions().booleanOption("vdni") || crew.getOptions().booleanOption("bvdni")))) {
            cyberBonus = 1;
        }

        for (Mounted m : getMisc()) {
            EquipmentType type = m.getType();
            if ((type instanceof MiscType) && type.hasFlag(MiscType.F_BAP) && !m.isInoperable()) {
                // System.err.println("BAP type name: "+m.getName()+"
                // internalName: "+((MiscType)m.getType()).internalName);
                // in space the range of all BAPs is given by the mode
                if (game.getBoard().inSpace()) {
                    if (m.curMode().equals("Medium")) {
                        return 12 + cyberBonus;
                    }
                    return 6 + cyberBonus;
                }

                if (m.getName().equals("Bloodhound Active Probe (THB)") || m.getName().equals(Sensor.BAP)) {
                    return 8 + cyberBonus;
                }
                if ((m.getType()).getInternalName().equals(Sensor.CLAN_AP) || (m.getType()).getInternalName().equals(Sensor.WATCHDOG) || (m.getType().getInternalName().equals(Sensor.CLBALIGHT_AP))) {
                    return 5 + cyberBonus;
                }
                if ((m.getType()).getInternalName().equals(Sensor.LIGHT_AP) || m.getType().getInternalName().equals(Sensor.CLIMPROVED)) {
                    return 3 + cyberBonus;
                }
                if (m.getType().getInternalName().equals(Sensor.ISIMPROVED)) {
                    return 2 + cyberBonus;
                }
                return 4 + cyberBonus;// everthing else should be range 4
            }
        }
        if (cyberBonus > 0) {
            return 2;
        }

        return Entity.NONE;
    }

    /**
     * Returns wether or not this entity has a Targeting Computer.
     */
    public boolean hasTargComp() {
        for (Mounted m : getMisc()) {
            if ((m.getType() instanceof MiscType) && m.getType().hasFlag(MiscType.F_TARGCOMP)) {
                return !m.isInoperable();
            }
        }
        return false;
    }

    /**
     * Returns wether or not this entity has a Targeting Computer that is in
     * aimed shot mode.
     */
    public boolean hasAimModeTargComp() {
        if (hasActiveEiCockpit()) {
            if (this instanceof Mech) {
                if (((Mech) this).getCockpitStatus() == Mech.COCKPIT_AIMED_SHOT) {
                    return true;
                }
            } else {
                return true;
            }
        }
        for (Mounted m : getMisc()) {
            if ((m.getType() instanceof MiscType) && m.getType().hasFlag(MiscType.F_TARGCOMP) && m.curMode().equals("Aimed shot")) {
                return !m.isInoperable();
            }
        }
        return false;
    }

    /**
     * Returns whether this 'mech has a C3 Slave or not.
     */
    public boolean hasC3S() {
        if (isShutDown() || isOffBoard()) {
            return false;
        }
        for (Mounted m : getEquipment()) {
            if ((m.getType() instanceof MiscType) && m.getType().hasFlag(MiscType.F_C3S) && !m.isInoperable()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Only Meks can have CASE II so all other entites return false.
     *
     * @return true iff the mech has CASE II.
     */
    public boolean hasCASEII() {
        return false;
    }

    /**
     * Only Meks have CASE II so all other entites return false.
     *
     * @param location
     * @return true iff the mech has CASE II at this location.
     */
    public boolean hasCASEII(int location) {
        return false;
    }

    /**
     * Checks if the entity has a C3 Master.
     *
     * @return true if it has a working C3M computer and has a master.
     */
    public boolean hasC3M() {
        if (isShutDown() || isOffBoard()) {
            return false;
        }
        for (Mounted m : getEquipment()) {
            if ((m.getType() instanceof WeaponType) && m.getType().hasFlag(WeaponType.F_C3M) && !m.isInoperable()) {
                // If this unit is configured as a company commander,
                // and if this computer is the company master, then
                // this unit does not have a lance master computer.
                if (C3MasterIs(this) && (c3CompanyMasterIndex == getEquipmentNum(m))) {
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    public boolean hasC3MM() {
        if (isShutDown() || isOffBoard()) {
            return false;
        }

        // Have we already determined that there's no company command master?
        if (c3CompanyMasterIndex == LOC_NONE) {
            return false;
        }

        // Do we need to determine that there's no company command master?
        if (c3CompanyMasterIndex == LOC_DESTROYED) {
            Iterator<Mounted> e = getEquipment().iterator();
            while ((c3CompanyMasterIndex == LOC_DESTROYED) && e.hasNext()) {
                Mounted m = e.next();
                if ((m.getType() instanceof WeaponType) && m.getType().hasFlag(WeaponType.F_C3M) && !m.isInoperable()) {
                    // Now look for the company command master.
                    while ((c3CompanyMasterIndex == LOC_DESTROYED) && e.hasNext()) {
                        m = e.next();
                        if ((m.getType() instanceof WeaponType) && m.getType().hasFlag(WeaponType.F_C3M) && !m.isInoperable()) {
                            // Found the comany command master
                            c3CompanyMasterIndex = getEquipmentNum(m);
                        }
                    }
                }
            }
            // If we haven't found the company command master, there is none.
            if (c3CompanyMasterIndex == LOC_DESTROYED) {
                c3CompanyMasterIndex = LOC_NONE;
                return false;
            }
        }

        Mounted m = getEquipment(c3CompanyMasterIndex);
        if (!m.isDestroyed() && !m.isBreached()) {
            return true;
        }
        return false;
    }

    /**
     * Checks if it has any type of C3 computer.
     *
     * @return true iff it has a C3 computer.
     */
    public boolean hasC3() {
        return hasC3S() || hasC3M() || hasC3MM();
    }

    public boolean hasC3i() {
        if (isShutDown() || isOffBoard()) {
            return false;
        }
        for (Mounted m : getEquipment()) {
            if ((m.getType() instanceof MiscType) && m.getType().hasFlag(MiscType.F_C3I) && !m.isInoperable()) {
                return true;
            }
        }
        // check for Manei Domini implants
        if ((this instanceof Infantry) && crew.getOptions().booleanOption("mm_eye_im") && crew.getOptions().booleanOption("boost_comm_implant")) {
            return true;
        }
        return false;
    }

    public String getC3NetId() {
        if (c3NetIdString == null) {
            if (hasC3()) {
                c3NetIdString = "C3." + getId();
            } else if (hasC3i()) {
                c3NetIdString = "C3i." + getId();
            }
        }
        return c3NetIdString;
    }

    public void setC3NetId(Entity e) {
        if (isEnemyOf(e)) {
            return;
        }
        c3NetIdString = e.c3NetIdString;
    }

    /**
     * Determine the remaining number of other C3 Master computers that can
     * connect to this <code>Entity</code>.
     * <p>
     * Please note, if this <code>Entity</code> does not have two C3 Master
     * computers, then it must first be identified as a company commander;
     * otherwise the number of free nodes will be zero.
     *
     * @return a non-negative <code>int</code> value.
     */
    public int calculateFreeC3MNodes() {
        int nodes = 0;
        if (hasC3MM()) {
            nodes = 2;
            if (game != null) {
                for (java.util.Enumeration<Entity> i = game.getEntities(); i.hasMoreElements();) {
                    final Entity e = i.nextElement();
                    if (e.hasC3M() && (e != this)) {
                        final Entity m = e.getC3Master();
                        if (equals(m)) {
                            nodes--;
                        }
                        if (nodes <= 0) {
                            return 0;
                        }
                    }
                }
            }
        } else if (hasC3M() && C3MasterIs(this)) {
            nodes = 3;
            if (game != null) {
                for (java.util.Enumeration<Entity> i = game.getEntities(); i.hasMoreElements();) {
                    final Entity e = i.nextElement();
                    if (e.hasC3() && (e != this)) {
                        final Entity m = e.getC3Master();
                        if (equals(m)) {
                            nodes--;
                        }
                        if (nodes <= 0) {
                            return 0;
                        }
                    }
                }
            }
        }
        return nodes;
    }

    /**
     * Determine the remaining number of other C3 computers that can connect to
     * this <code>Entity</code>.
     * <p>
     * Please note, if this <code>Entity</code> has two C3 Master computers,
     * then this function only returns the remaining number of <b>C3 Slave</b>
     * computers that can connect.
     *
     * @return a non-negative <code>int</code> value.
     */
    public int calculateFreeC3Nodes() {
        int nodes = 0;
        if (hasC3i()) {
            nodes = 5;
            if (game != null) {
                for (Enumeration<Entity> i = game.getEntities(); i.hasMoreElements();) {
                    final Entity e = i.nextElement();
                    if (!equals(e) && onSameC3NetworkAs(e)) {
                        nodes--;
                        if (nodes <= 0) {
                            return 0;
                        }
                    }
                }
            }
        } else if (hasC3M()) {
            nodes = 3;
            if (game != null) {
                for (Enumeration<Entity> i = game.getEntities(); i.hasMoreElements();) {
                    final Entity e = i.nextElement();
                    if (e.hasC3() && !equals(e)) {
                        final Entity m = e.getC3Master();
                        if (equals(m)) {
                            // If this unit is a company commander, and has two
                            // C3 Master computers, only count C3 Slaves here.
                            if (!C3MasterIs(this) || !hasC3MM() || e.hasC3S()) {
                                nodes--;
                            }
                        }
                        if (nodes <= 0) {
                            return 0;
                        }
                    }
                }
            }
        }
        return nodes;
    }

    /**
     * @return the entity "above" this entity in our c3 network, or this entity
     *         itself, if none is above this
     */
    public Entity getC3Top() {
        Entity m = this;
        Entity master = m.getC3Master();
        while ((master != null) && !master.equals(m) && master.hasC3() && !(Compute.isAffectedByECM(m, m.getPosition(), master.getPosition())) && !(Compute.isAffectedByECM(master, master.getPosition(), master.getPosition()))) {
            m = master;
            master = m.getC3Master();
        }
        return m;
    }

    /**
     * Return the unit that is current master of this unit's C3 network. If the
     * master unit has been destroyed or had it's C3 master computer damaged,
     * then this unit is out of the C3 network for the rest of the game. If the
     * master unit has shut down, then this unit may return to the C3 network at
     * a later time.
     *
     * @return the <code>Entity</code> that is the master of this unit's C3
     *         network. This value may be <code>null</code>. If the value master
     *         unit has shut down, then the value will be non-<code>null</code>
     *         after the master unit restarts.
     */
    public Entity getC3Master() {
        if (c3Master == NONE) {
            return null;
        }
        if (hasC3S() && (c3Master > NONE)) {
            // since we can't seem to get the check working in setC3Master(),
            // I'll just do it here, every time. This sucks.
            Entity eMaster = game.getEntity(c3Master);
            // Have we lost our C3Master?
            if (eMaster == null) {
                c3Master = NONE;
            }
            // If our master is shut down, don't clear this slave's setting.
            else if (eMaster.isShutDown()) {
                return null;
            }
            // Slave computers can't connect to single-computer company masters.
            else if (eMaster.C3MasterIs(eMaster) && !eMaster.hasC3MM()) {
                c3Master = NONE;
            }
            // Has our lance master lost its computer?
            else if (!eMaster.hasC3M()) {
                c3Master = NONE;
            }
        } else if (hasC3M() && (c3Master > NONE)) {
            Entity eMaster = game.getEntity(c3Master);
            // Have we lost our C3Master?
            if (eMaster == null) {
                c3Master = NONE;
            }
            // If our master is shut down, don't clear this slave's setting.
            else if (eMaster.isShutDown()) {
                return null;
            }
            // Has our company commander lost his company command computer?
            else if (((eMaster.c3CompanyMasterIndex > LOC_NONE) && !eMaster.hasC3MM()) || ((eMaster.c3CompanyMasterIndex <= LOC_NONE) && !eMaster.hasC3M())) {
                c3Master = NONE;
            }
            // maximum depth of a c3 network is 2 levels.
            else if (eMaster != this) {
                Entity eCompanyMaster = eMaster.getC3Master();
                if ((eCompanyMaster != null) && (eCompanyMaster.getC3Master() != eCompanyMaster)) {
                    c3Master = NONE;
                }
            }
        }
        // If we aren't shut down, and if we don't have a company master
        // computer, but have a C3Master, then we must have lost our network.
        else if (!isShutDown() && !hasC3MM() && (c3Master > NONE)) {
            c3Master = NONE;
        }
        if (c3Master == NONE) {
            return null;
        }
        return game.getEntity(c3Master);
    }

    /**
     * Get the ID of the master unit in this unit's C3 network. If the master
     * unit has shut down, then the ID will still be returned. The only times
     * when the value, <code>Entity.NONE</code> is returned is when this unit is
     * permanently out of the C3 network, or when it was never in a C3 network.
     *
     * @return the <code>int</code> ID of the unit that is the master of this
     *         unit's C3 network, or <code>Entity.NONE</code>.
     */
    public int getC3MasterId() {
        // Make sure that this unit is still on a C3 network.
        // N.B. this call may set this.C3Master to NONE.
        getC3Master();
        return c3Master;
    }

    /**
     * Determines if the passed <code>Entity</code> is the C3 Master of this
     * unit.
     * <p>
     * Please note, that when an <code>Entity</code> is it's own C3 Master, then
     * it is a company commander.
     * <p>
     * Also note that when <code>null</code> is the master for this
     * <code>Entity</code>, then it is an independent master.
     *
     * @param e
     *            - the <code>Entity</code> that may be this unit's C3 Master.
     * @return a <code>boolean</code> that is <code>true</code> when the passed
     *         <code>Entity</code> is this unit's commander. If the passed unit
     *         isn't this unit's commander, this routine returns
     *         <code>false</code>.
     */
    public boolean C3MasterIs(Entity e) {
        if (e == null) {
            if (c3Master == NONE) {
                return true;
            }

            return false; // if this entity has a C3Master then null is not
            // it's master.
        }
        return (e.id == c3Master);
    }

    /**
     * Set another <code>Entity</code> as our C3 Master
     *
     * @param e
     *            - the <code>Entity</code> that should be set as our C3 Master.
     */
    public void setC3Master(Entity e) {
        if (e == null) {
            setC3Master(NONE);
        } else {
            if (isEnemyOf(e)) {
                return;
            }
            setC3Master(e.id);
        }
    }

    /**
     * @param entityId
     */
    public void setC3Master(int entityId) {
        if ((id == entityId) != (id == c3Master)) {
            // this just changed from a company-level to lance-level (or vice
            // versa); have to disconnect all slaved units to maintain
            // integrity.
            for (java.util.Enumeration<Entity> i = game.getEntities(); i.hasMoreElements();) {
                final Entity e = i.nextElement();
                if (e.C3MasterIs(this) && !equals(e)) {
                    e.setC3Master(NONE);
                }
            }
        }
        if (hasC3()) {
            c3Master = entityId;
        }
        if (hasC3() && (entityId == NONE)) {
            c3NetIdString = "C3." + id;
        } else if (hasC3i() && (entityId == NONE)) {
            c3NetIdString = "C3i." + id;
        } else if (hasC3() || hasC3i()) {
            c3NetIdString = game.getEntity(entityId).getC3NetId();
        }
        for (java.util.Enumeration<Entity> i = game.getEntities(); i.hasMoreElements();) {
            final Entity e = i.nextElement();
            if (e.C3MasterIs(this) && !equals(e)) {
                e.c3NetIdString = c3NetIdString;
            }
        }
    }

    public boolean onSameC3NetworkAs(Entity e) {
        return onSameC3NetworkAs(e, false);
    }

    /**
     * Checks if another entity is on the same c3 network as this entity
     *
     * @param e
     *            The <code>Entity</code> to check against this entity
     * @param ignoreECM
     *            a <code>boolean</code> indicating if ECM should be ignored, we
     *            need this for c3i
     * @return a <code>boolean</code> that is <code>true</code> if the given
     *         entity is on the same network, <code>false</code> if not.
     */
    public boolean onSameC3NetworkAs(Entity e, boolean ignoreECM) {
        if (isEnemyOf(e) || isShutDown() || e.isShutDown()) {
            return false;
        }

        // Active Mek Stealth prevents entity from participating in C3.
        // Turn off the stealth, and your back in the network.
        if ((this instanceof Mech) && isStealthActive()) {
            return false;
        }
        if ((e instanceof Mech) && e.isStealthActive()) {
            return false;
        }

        // C3i is easy - if they both have C3i, and their net ID's match,
        // they're on the same network!
        if (hasC3i() && e.hasC3i() && getC3NetId().equals(e.getC3NetId())) {
            if (ignoreECM) {
                return true;
            }
            return !(Compute.isAffectedByECM(e, e.getPosition(), e.getPosition())) && !(Compute.isAffectedByECM(this, getPosition(), getPosition()));
        }

        // simple sanity check - do they both have C3, and are they both on the
        // same network?
        if (!hasC3() || !e.hasC3()) {
            return false;
        }
        if ((getC3Top() == null) || (e.getC3Top() == null)) {
            return false;
        }
        // got the easy part out of the way, now we need to verify that the
        // network isn't down
        return (getC3Top().equals(e.getC3Top()));
    }

    /**
     * Returns whether there is CASE protecting the location.
     */
    public boolean locationHasCase(int loc) {
        for (Mounted mounted : getMisc()) {
            if ((mounted.getLocation() == loc) && mounted.getType().hasFlag(MiscType.F_CASE) && !mounted.isDestroyed()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Hits all criticals of the system occupying the specified critical slot.
     * Used, for example, in a gauss rifle capacitor discharge. Does not apply
     * any special effect of hitting the criticals, like ammo explosion.
     */
    public void hitAllCriticals(int loc, int slot) {
        CriticalSlot orig = getCritical(loc, slot);
        for (int i = 0; i < getNumberOfCriticals(loc); i++) {
            CriticalSlot cs = getCritical(loc, slot);
            if ((cs.getType() == orig.getType()) && (cs.getIndex() == orig.getIndex())) {
                cs.setHit(true);
            }
        }
    }

    /**
     * Start a new round
     *
     * @param roundNumber
     *            the <code>int</code> number of the new round
     */
    public void newRound(int roundNumber) {
        unloadedThisTurn = false;
        done = false;
        delta_distance = 0;
        mpUsedLastRound = mpUsed;
        mpUsed = 0;
        damageThisRound = 0;
        if (assaultDropInProgress == 2) {
            assaultDropInProgress = 0;
        }
        moved = IEntityMovementType.MOVE_NONE;
        gotPavementBonus = false;
        hitThisRoundByAntiTSM = false;
        inReverse = false;
        hitBySwarmsEntity.clear();
        hitBySwarmsWeapon.clear();
        setTaggedBy(-1);
        setLayingMines(false);
        setArmsFlipped(false);
        setDisplacementAttack(null);
        setFindingClub(false);
        setSpotting(false);
        spotTargetId = Entity.NONE;
        setClearingMinefield(false);
        setUnjammingRAC(false);
        crew.setKoThisRound(false);
        m_lNarcedBy |= m_lPendingNarc;
        if (pendingINarcPods.size() > 0) {
            iNarcPods.addAll(pendingINarcPods);
            pendingINarcPods = new ArrayList<INarcPod>();
        }
        if (pendingNarcPods.size() > 0) {
            narcPods.addAll(pendingNarcPods);
            pendingNarcPods.clear();
        }

        for (Mounted m : getEquipment()) {
            m.newRound(roundNumber);
        }

        // reset hexes passed through
        setPassedThrough(new Vector<Coords>());

        resetFiringArcs();

        // reset evasion
        setEvading(false);

        // make sensor checks
        sensorCheck = Compute.d6(2);
        // if the current sensor is BAP and BAP is critted, then switch to the
        // first
        // thing that works
        if ((null != nextSensor) && nextSensor.isBAP() && !hasBAP(false)) {
            for (Sensor sensor : getSensors()) {
                if (!sensor.isBAP()) {
                    nextSensor = sensor;
                    break;
                }
            }
        }
        // change the active sensor, if requested
        if (null != nextSensor) {
            activeSensor = nextSensor;
        }

        // ghost target roll
        ghostTargetRoll = Compute.d6(2);
        ghostTargetOverride = Compute.d6(2);

        // Update the inferno tracker.
        infernos.newRound(roundNumber);
        if (taserShutdownRounds > 0) {
            taserShutdownRounds--;
            if (taserShutdownRounds == 0) {
                shutdownByBATaser = false;
            }
        }
        if (taserInterferenceRounds > 0) {
            taserInterferenceRounds--;
            if (taserInterferenceRounds == 0) {
                taserInterference = 0;
            }
        }
        if (taserFeedBackRounds > 0) {
            taserFeedBackRounds--;
        }
    }

    /**
     * Applies any damage that the entity has suffered. When anything gets hit
     * it is simply marked as "hit" but does not stop working until this is
     * called.
     */
    public void applyDamage() {
        // mark all damaged equipment destroyed and empty
        for (Mounted mounted : getEquipment()) {
            if (mounted.isHit() || mounted.isMissing()) {
                mounted.setShotsLeft(0);
                mounted.setDestroyed(true);
            }
        }

        // destroy criticals that were hit last phase
        for (int i = 0; i < locations(); i++) {
            for (int j = 0; j < getNumberOfCriticals(i); j++) {
                final CriticalSlot cs = getCritical(i, j);
                if (cs != null) {
                    cs.setDestroyed(cs.isDamaged());
                }
            }
        }

        // destroy armor/internals if the section was removed
        for (int i = 0; i < locations(); i++) {
            if (getInternal(i) == IArmorState.ARMOR_DOOMED) {
                setArmor(IArmorState.ARMOR_DESTROYED, i);
                setArmor(IArmorState.ARMOR_DESTROYED, i, true);
                setInternal(IArmorState.ARMOR_DESTROYED, i);
                // destroy any Narc beacons
                for (Iterator<NarcPod> iter = narcPods.iterator(); iter.hasNext();) {
                    NarcPod p = iter.next();
                    if (p.getLocation() == i) {
                        iter.remove();
                    }
                }
                for (Iterator<INarcPod> iter = iNarcPods.iterator(); iter.hasNext();) {
                    INarcPod p = iter.next();
                    if (p.getLocation() == i) {
                        iter.remove();
                    }
                }
                for (Iterator<NarcPod> iter = pendingNarcPods.iterator(); iter.hasNext();) {
                    NarcPod p = iter.next();
                    if (p.getLocation() == i) {
                        iter.remove();
                    }
                }
                for (Iterator<INarcPod> iter = pendingINarcPods.iterator(); iter.hasNext();) {
                    INarcPod p = iter.next();
                    if (p.getLocation() == i) {
                        iter.remove();
                    }
                }
            }
        }
    }

    /**
     * Attempts to reload any empty weapons with the first ammo found
     */
    public void reloadEmptyWeapons() {
        // try to reload weapons
        for (Mounted mounted : getTotalWeaponList()) {
            WeaponType wtype = (WeaponType) mounted.getType();

            if (wtype.getAmmoType() != AmmoType.T_NA) {
                if ((mounted.getLinked() == null) || (mounted.getLinked().getShotsLeft() <= 0) || mounted.getLinked().isDumping()) {
                    loadWeaponWithSameAmmo(mounted);
                }
            }
        }
    }

    /**
     * Assign AMS systems to the most dangerous incoming missile attacks. This
     * should only be called once per turn, or AMS will get extra attacks
     */
    public void assignAMS(Vector<WeaponHandler> vAttacks) {

        HashSet<WeaponAttackAction> targets = new HashSet<WeaponAttackAction>();
        for (Mounted weapon : getWeaponList()) {
            if (weapon.getType().hasFlag(WeaponType.F_AMS)) {
                if (!weapon.isReady() || weapon.isMissing() || weapon.curMode().equals("Off")) {
                    continue;
                }

                // make sure ammo is loaded
                Mounted ammo = weapon.getLinked();
                if (!(weapon.getType().hasFlag(WeaponType.F_ENERGY)) && ((ammo == null) || (ammo.getShotsLeft() == 0) || ammo.isDumping())) {
                    loadWeapon(weapon);
                    ammo = weapon.getLinked();
                }

                // try again
                if (!(weapon.getType().hasFlag(WeaponType.F_ENERGY)) && ((ammo == null) || (ammo.getShotsLeft() == 0) || ammo.isDumping())) {
                    // No ammo for this AMS.
                    continue;
                }

                // make a new vector of only incoming attacks in arc
                Vector<WeaponAttackAction> vAttacksInArc = new Vector<WeaponAttackAction>(vAttacks.size());
                for (WeaponHandler wr : vAttacks) {
                    if (!targets.contains(wr.waa) && Compute.isInArc(game, getId(), getEquipmentNum(weapon), game.getEntity(wr.waa.getEntityId()))) {
                        vAttacksInArc.addElement(wr.waa);
                    }
                }
                // find the most dangerous salvo by expected damage
                WeaponAttackAction waa = Compute.getHighestExpectedDamage(game, vAttacksInArc, true);
                if (waa != null) {
                    waa.addCounterEquipment(weapon);
                    targets.add(waa);
                }
            }
        }
    }

    /**
     * has the team attached a narc pod to me?
     */
    public boolean isNarcedBy(int nTeamID) {
        for (NarcPod p : narcPods) {
            if (p.getTeam() == nTeamID) {
                return true;
            }
        }
        return false;
    }

    /**
     * add a narc pod from this team to the mech. Unremovable
     *
     * @param pod
     *            The <code>NarcPod</code> to be attached.
     */
    public void attachNarcPod(NarcPod pod) {
        pendingNarcPods.add(pod);
    }

    /**
     * attach an iNarcPod
     *
     * @param pod
     *            The <code>INarcPod</code> to be attached.
     */
    public void attachINarcPod(INarcPod pod) {
        pendingINarcPods.add(pod);
    }

    /**
     * Have we been iNarced with a homing pod from that team?
     *
     * @param nTeamID
     *            The id of the team that we are wondering about.
     * @return true if the Entity is narced by that team.
     */
    public boolean isINarcedBy(int nTeamID) {
        for (INarcPod pod : iNarcPods) {
            if ((pod.getTeam() == nTeamID) && (pod.getType() == INarcPod.HOMING)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Have we been iNarced with the named pod from any team?
     *
     * @param type
     *            the <code>int</code> type of iNarc pod.
     * @return <code>true</code> if we have.
     */
    public boolean isINarcedWith(long type) {
        for (INarcPod pod : iNarcPods) {
            if (pod.getType() == type) {
                return true;
            }
        }
        return false;
    }

    /**
     * Remove all attached iNarc Pods
     */
    public void removeAllINarcPods() {
        iNarcPods.clear();
    }

    /**
     * Do we have any iNarc Pods attached?
     *
     * @return true iff one or more iNarcPods are attached.
     */
    public boolean hasINarcPodsAttached() {
        if (iNarcPods.size() > 0) {
            return true;
        }
        return false;
    }

    /**
     * Get an <code>Enumeration</code> of <code>INarcPod</code>s that are
     * attached to this entity.
     *
     * @return an <code>Enumeration</code> of <code>INarcPod</code>s.
     */
    public Iterator<INarcPod> getINarcPodsAttached() {
        return iNarcPods.iterator();
    }

    /**
     * Remove an <code>INarcPod</code> from this entity.
     *
     * @param pod
     *            the <code>INarcPod</code> to be removed.
     * @return <code>true</code> if the pod was removed, <code>false</code> if
     *         the pod was not attached to this entity.
     */
    public boolean removeINarcPod(INarcPod pod) {
        return iNarcPods.remove(pod);
    }

    /**
     * Calculates the battle value of this entity
     */
    public abstract int calculateBattleValue();

    /**
     * Calculates the battle value of this mech. If the parameter is true, then
     * the battle value for c3 will be added whether the mech is currently part
     * of a network or not. This should be overwritten if necessary
     *
     * @param ignoreC3
     *            if the contribution of the C3 computer should be ignored when
     *            calculating BV.
     * @param ignorePilot
     *            if the extra BV due to piloting skill should be ignore, needed
     *            for c3 bv
     */
    public int calculateBattleValue(boolean ignoreC3, boolean ignorePilot) {
        return calculateBattleValue();
    }

    /**
     * Generates a vector containing reports on all useful information about
     * this entity.
     */
    public abstract Vector<Report> victoryReport();

    /**
     * Two entities are equal if their ids are equal
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if ((object == null) || (getClass() != object.getClass())) {
            return false;
        }
        Entity other = (Entity) object;
        return other.getId() == id;
    }

    /**
     * Get the movement mode of the entity
     */
    public int getMovementMode() {
        return movementMode;
    }

    /**
     * Get the movement mode of the entity as a String.
     */
    public String getMovementModeAsString() {
        switch (getMovementMode()) {
        case IEntityMovementMode.NONE:
            return "None";
        case IEntityMovementMode.BIPED:
        case IEntityMovementMode.BIPED_SWIM:
            return "Biped";
        case IEntityMovementMode.QUAD:
        case IEntityMovementMode.QUAD_SWIM:
            return "Quad";
        case IEntityMovementMode.TRACKED:
            return "Tracked";
        case IEntityMovementMode.WHEELED:
            return "Wheeled";
        case IEntityMovementMode.HOVER:
            return "Hover";
        case IEntityMovementMode.VTOL:
            return "VTOL";
        case IEntityMovementMode.NAVAL:
            return "Naval";
        case IEntityMovementMode.HYDROFOIL:
            return "Hydrofoil";
        case IEntityMovementMode.SUBMARINE:
        case IEntityMovementMode.INF_UMU:
            return "Submarine";
        case IEntityMovementMode.INF_LEG:
            return "Leg";
        case IEntityMovementMode.INF_MOTORIZED:
            return "Motorized";
        case IEntityMovementMode.INF_JUMP:
            return "Jump";
        case IEntityMovementMode.WIGE:
            return "WiGE";
        case IEntityMovementMode.AERODYNE:
            return "Aerodyne";
        case IEntityMovementMode.SPHEROID:
            return "Spheroid";
        default:
            return "ERROR";
        }
    }

    /**
     * Set the movement type of the entity
     */
    public void setMovementMode(int movementMode) {
        this.movementMode = movementMode;
    }

    /**
     * Helper function to determine if a entity is a biped
     */
    public boolean entityIsBiped() {
        return (getMovementMode() == IEntityMovementMode.BIPED);
    }

    /**
     * Helper function to determine if a entity is a quad
     */
    public boolean entityIsQuad() {
        return (getMovementMode() == IEntityMovementMode.QUAD);
    }

    /**
     * Returns true is the entity needs a roll to stand up
     */
    public boolean needsRollToStand() {
        return true;
    }

    /**
     * Returns an entity's base piloting skill roll needed Only use this version
     * if the entity is through processing movement
     */
    public PilotingRollData getBasePilotingRoll() {
        return getBasePilotingRoll(moved);
    }

    /**
     * Returns an entity's base piloting skill roll needed
     */
    public PilotingRollData getBasePilotingRoll(int moveType) {
        final int entityId = getId();

        PilotingRollData roll;

        // Pilot dead?
        if (getCrew().isDead() || getCrew().isDoomed() || (getCrew().getHits() >= 6)) {
            // Following line switched from impossible to automatic failure
            // -- bug fix for dead units taking PSRs
            return new PilotingRollData(entityId, TargetRoll.AUTOMATIC_FAIL, "Pilot dead");
        }
        // pilot awake?
        else if (!getCrew().isActive()) {
            return new PilotingRollData(entityId, TargetRoll.IMPOSSIBLE, "Pilot unconscious");
        }
        // gyro operational?
        if ((getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO, Mech.LOC_CT) > 1) && (getGyroType() != Mech.GYRO_HEAVY_DUTY)) {
            return new PilotingRollData(entityId, TargetRoll.AUTOMATIC_FAIL, 3, "Gyro destroyed");
        }

        // Takes 3+ hits to kill an HD Gyro.
        if ((getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO, Mech.LOC_CT) > 2) && (getGyroType() == Mech.GYRO_HEAVY_DUTY)) {
            return new PilotingRollData(entityId, TargetRoll.AUTOMATIC_FAIL, 3, "Gyro destroyed");
        }

        // both legs present?
        if (this instanceof BipedMech) {
            if (((BipedMech) this).countBadLegs() == 2) {
                return new PilotingRollData(entityId, TargetRoll.AUTOMATIC_FAIL, 10, "Both legs destroyed");
            }
        } else if (this instanceof QuadMech) {
            if (((QuadMech) this).countBadLegs() >= 3) {
                return new PilotingRollData(entityId, TargetRoll.AUTOMATIC_FAIL, 10, ((Mech) this).countBadLegs() + " legs destroyed");
            }
        }
        // entity shut down?
        if (isShutDown()) {
            return new PilotingRollData(entityId, TargetRoll.AUTOMATIC_FAIL, 3, "Reactor shut down");
        }

        // okay, let's figure out the stuff then
        roll = new PilotingRollData(entityId, getCrew().getPiloting(), "Base piloting skill");

        // Let's see if we have a modifier to our piloting skill roll. We'll
        // pass in the roll
        // object and adjust as necessary
        roll = addEntityBonuses(roll);

        // add planetary condition modifiers
        roll = addConditionBonuses(roll, moveType);

        if (isCarefulStand()) {
            roll.addModifier(-2, "careful stand");
        }

        if (game.getOptions().booleanOption("tacops_fatigue") && crew.isPilotingFatigued(game.getRoundCount())) {
            roll.addModifier(1, "fatigue");
        }

        if (taserInterference > 0) {
            roll.addModifier(taserInterference, "taser interference");
        }

        return roll;
    }

    /**
     * Add in any piloting skill mods
     */
    public abstract PilotingRollData addEntityBonuses(PilotingRollData roll);

    /**
     * Add in any modifiers due to global conditions like light/weather/etc.
     */
    public PilotingRollData addConditionBonuses(PilotingRollData roll, int moveType) {

        PlanetaryConditions conditions = game.getPlanetaryConditions();
        // check light conditions for "running" entities
        if ((moveType == IEntityMovementType.MOVE_RUN) || (moveType == IEntityMovementType.MOVE_VTOL_RUN) || (moveType == IEntityMovementType.MOVE_OVER_THRUST)) {
            int lightPenalty = conditions.getLightPilotPenalty();
            if (lightPenalty > 0) {
                roll.addModifier(lightPenalty, conditions.getLightCurrentName());
            }
        }

        // check weather conditions for all entities
        int weatherMod = conditions.getWeatherPilotPenalty();
        if ((weatherMod != 0) && !game.getBoard().inSpace()) {
            roll.addModifier(weatherMod, conditions.getWeatherCurrentName());
        }

        // check wind conditions for all entities
        int windMod = conditions.getWindPilotPenalty(this);
        if ((windMod != 0) && !game.getBoard().inSpace()) {
            roll.addModifier(windMod, conditions.getWindCurrentName());
        }

        // check gravity conditions for all entities
        int gravMod = conditions.getGravityPilotPenalty();
        if ((gravMod != 0) && !game.getBoard().inSpace()) {
            roll.addModifier(gravMod, "high/low gravity");
        }
        return roll;

    }

    /**
     * Checks if the entity is getting up. If so, returns the target roll for
     * the piloting skill check.
     */
    public PilotingRollData checkGetUp(MoveStep step) {

        if ((step == null) || ((step.getType() != MovePath.STEP_GET_UP) && (step.getType() != MovePath.STEP_CAREFUL_STAND))) {
            return new PilotingRollData(id, TargetRoll.CHECK_FALSE, "Check false: Entity is not attempting to get up.");
        }

        PilotingRollData roll = getBasePilotingRoll(step.getParent().getLastStepMovementType());

        if (this instanceof BipedMech) {
            if ((((Mech) this).countBadLegs() >= 1) && (isLocationBad(Mech.LOC_LARM) && isLocationBad(Mech.LOC_RARM))) {
                roll.addModifier(TargetRoll.IMPOSSIBLE, "can't get up with destroyed leg and arms");
                return roll;
            }
        }

        if (isHullDown() && (this instanceof QuadMech)) {
            roll.addModifier(TargetRoll.AUTOMATIC_SUCCESS, "getting up from hull down");
            return roll;
        }

        if (!needsRollToStand() && (getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO, Mech.LOC_CT) < 2)) {
            roll.addModifier(TargetRoll.AUTOMATIC_SUCCESS, "\n" + getDisplayName() + " does not need to make a piloting skill check to stand up because it has all four of its legs.");
            return roll;
        }

        // append the reason modifier
        roll.append(new PilotingRollData(getId(), 0, "getting up"));
        addPilotingModifierForTerrain(roll, step);
        return roll;
    }

    /**
     * Checks if the entity is attempting to run with damage that would force a
     * PSR. If so, returns the target roll for the piloting skill check.
     */
    public PilotingRollData checkRunningWithDamage(int overallMoveType) {
        PilotingRollData roll = getBasePilotingRoll(overallMoveType);

        int gyroDamage = getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO, Mech.LOC_CT);
        if (getGyroType() == Mech.GYRO_HEAVY_DUTY) {
            gyroDamage--; // HD gyro ignores 1st damage
        }
        if ((overallMoveType == IEntityMovementType.MOVE_RUN) && !isProne() && ((gyroDamage > 0) || hasHipCrit())) {
            // append the reason modifier
            roll.append(new PilotingRollData(getId(), 0, "running with damaged hip actuator or gyro"));
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE, "Check false: Entity is not attempting to run with damage");
        }
        addPilotingModifierForTerrain(roll);
        return roll;
    }

    /**
     * Checks if an entity is passing through certain terrain while not moving
     * carefully
     */
    public PilotingRollData checkRecklessMove(MoveStep step, IHex curHex, Coords lastPos, Coords curPos, int lastElev) {
        PilotingRollData roll = getBasePilotingRoll(step.getParent().getLastStepMovementType());
        // no need to go further if movement is careful
        if (step.getParent().isCareful()) {
            roll.addModifier(TargetRoll.CHECK_FALSE, "moving carefully");
            return roll;
        }

        // this only applies in fog, night conditions, or if a hex along the
        // move path has ice
        boolean isFoggy = game.getPlanetaryConditions().getFog() != PlanetaryConditions.FOG_NONE;
        boolean isDark = game.getPlanetaryConditions().getLight() > PlanetaryConditions.L_DUSK;
        boolean hasIce = false;

        Enumeration<MoveStep> steps = step.getParent().getSteps();
        while (steps.hasMoreElements()) {
            MoveStep nextStep = steps.nextElement();
            if (null == nextStep.getPosition()) {
                continue;
            }
            IHex nextHex = game.getBoard().getHex(nextStep.getPosition());
            if (null == nextHex) {
                continue;
            }
            if (nextHex.containsTerrain(Terrains.ICE) && (nextStep.getElevation() == 0)) {
                hasIce = true;
                break;
            }
        }

        if (!isFoggy && !isDark && !hasIce) {
            roll.addModifier(TargetRoll.CHECK_FALSE, "conditions are not dangerous");
            return roll;
        }

        // if we are jumping, then no worries
        if (step.getMovementType() == IEntityMovementType.MOVE_JUMP) {
            roll.addModifier(TargetRoll.CHECK_FALSE, "jumping is not reckless?");
            return roll;
        }

        // we need to make this check on the first move forward and anytime the
        // hex is not clear
        // or is a level change
        if (!lastPos.equals(curPos) && lastPos.equals(step.getParent().getEntity().getPosition())) {
            roll.append(new PilotingRollData(getId(), 0, "moving recklessly"));
        }
        // TODO: how do you tell if it is clear?
        // FIXME: no perfect solution in the current code. I will use movement
        // costs
        else if (!lastPos.equals(curPos) && ((curHex.movementCost(step.getParent().getLastStepMovementType()) > 0) || (lastElev != curHex.getElevation()))) {
            roll.append(new PilotingRollData(getId(), 0, "moving recklessly"));
            // ice conditions
        } else if (curHex.containsTerrain(Terrains.ICE)) {
            roll.append(new PilotingRollData(getId(), 0, "moving recklessly"));
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE, "not moving recklessly");
        }

        return roll;
    }

    /**
     * Checks if the entity is landing (from a jump) with damage that would
     * force a PSR. If so, returns the target roll for the piloting skill check.
     */
    public PilotingRollData checkLandingWithDamage(int overallMoveType) {
        PilotingRollData roll = getBasePilotingRoll(overallMoveType);

        if (((getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO, Mech.LOC_CT) > 0) && (getGyroType() != Mech.GYRO_HEAVY_DUTY)) || ((getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO, Mech.LOC_CT) > 1) && (getGyroType() == Mech.GYRO_HEAVY_DUTY)) || hasLegActuatorCrit()) {
            // append the reason modifier
            roll.append(new PilotingRollData(getId(), 0, "landing with damaged leg actuator or gyro"));
            addPilotingModifierForTerrain(roll);
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE, "Entity does not have gyro or leg accutator damage -- checking for purposes of determining PSR after jump.");
        }

        return roll;
    }

    /**
     * Checks if the entity is landing (from a jump) on ice-covered water.
     */
    public PilotingRollData checkLandingOnIce(int overallMoveType, IHex curHex) {
        PilotingRollData roll = getBasePilotingRoll(overallMoveType);

        if (curHex.containsTerrain(Terrains.ICE) && (curHex.terrainLevel(Terrains.WATER) > 0)) {
            roll.append(new PilotingRollData(getId(), 0, "landing on ice-covered water"));
            addPilotingModifierForTerrain(roll);
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE, "hex is not covered by ice");
        }

        return roll;
    }

    public PilotingRollData checkMovedTooFast(MoveStep step) {
        PilotingRollData roll = getBasePilotingRoll(step.getParent().getLastStepMovementType());
        addPilotingModifierForTerrain(roll, step);
        switch (step.getMovementType()) {
        case IEntityMovementType.MOVE_WALK:
        case IEntityMovementType.MOVE_RUN:
        case IEntityMovementType.MOVE_VTOL_WALK:
        case IEntityMovementType.MOVE_VTOL_RUN:
            if (step.getMpUsed() > (int) Math.ceil(getOriginalWalkMP() * 1.5)) {
                roll.append(new PilotingRollData(getId(), 0, "used more MPs than at 1G possible"));
            } else {
                roll.addModifier(TargetRoll.CHECK_FALSE, "Check false: Entity did not use more MPs walking/running than possible at 1G");
            }
            break;
        case IEntityMovementType.MOVE_JUMP:
            if (step.getMpUsed() > getJumpMP(false)) {
                roll.append(new PilotingRollData(getId(), 0, "used more MPs than at 1G possible"));
            } else {
                roll.addModifier(TargetRoll.CHECK_FALSE, "Check false: Entity did not use more MPs jumping than possible at 1G");
            }
            break;
        }
        return roll;
    }

    /**
     * Checks if the entity might skid on pavement. If so, returns the target
     * roll for the piloting skill check.
     */
    public PilotingRollData checkSkid(int moveType, IHex prevHex, int overallMoveType, MoveStep prevStep, int prevFacing, int curFacing, Coords lastPos, Coords curPos, boolean isInfantry, int distance) {
        PilotingRollData roll = getBasePilotingRoll(overallMoveType);
        addPilotingModifierForTerrain(roll, lastPos);

        // TODO: add check for elevation of pavement, road,
        // or bridge matches entity elevation.
        if ((moveType != IEntityMovementType.MOVE_JUMP) && (prevHex != null)
                /*
                 * Bug 754610: Revert fix for bug 702735. && (
                 * prevHex.contains(Terrain.PAVEMENT) || prevHex.contains(Terrain.ROAD)
                 * || prevHex.contains(Terrain.BRIDGE) )
                 */
                && ((prevStep.isPavementStep() && (overallMoveType == IEntityMovementType.MOVE_RUN) && (movementMode != IEntityMovementMode.HOVER) && (movementMode != IEntityMovementMode.WIGE)) || (prevHex.containsTerrain(Terrains.ICE) && (movementMode != IEntityMovementMode.HOVER) && (movementMode != IEntityMovementMode.WIGE)) || (((movementMode == IEntityMovementMode.HOVER) || (movementMode == IEntityMovementMode.WIGE)) && ((game.getPlanetaryConditions().getWeather() == PlanetaryConditions.WE_HEAVY_SNOW) || (game.getPlanetaryConditions().getWindStrength() >= PlanetaryConditions.WI_STORM)))) && (prevFacing != curFacing) && !lastPos.equals(curPos) && !isInfantry
                // Bug 912127, a unit that just got up and changed facing
                // on pavement in that getting up does not skid.
                && !prevStep.isHasJustStood()) {
            // append the reason modifier
            if (prevStep.isPavementStep() && !prevHex.containsTerrain(Terrains.ICE)) {
                if (this instanceof Mech) {
                    roll.append(new PilotingRollData(getId(), getMovementBeforeSkidPSRModifier(distance), "running & turning on pavement"));
                } else {
                    roll.append(new PilotingRollData(getId(), getMovementBeforeSkidPSRModifier(distance), "reckless driving on pavement"));
                }
            } else {
                roll.append(new PilotingRollData(getId(), getMovementBeforeSkidPSRModifier(distance), "turning on ice"));
            }
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE, "Check false: Entity is not apparently skidding");
        }

        return roll;
    }

    /**
     * Checks if the entity is moving into rubble. If so, returns the target
     * roll for the piloting skill check.
     */
    public PilotingRollData checkRubbleMove(MoveStep step, IHex curHex, Coords lastPos, Coords curPos) {
        PilotingRollData roll = getBasePilotingRoll(step.getParent().getLastStepMovementType());
        addPilotingModifierForTerrain(roll, curPos);

        if (!lastPos.equals(curPos) && (step.getMovementType() != IEntityMovementType.MOVE_JUMP) && (curHex.terrainLevel(Terrains.RUBBLE) > 0) && (this instanceof Mech)) {
            int mod = 0;
            if (curHex.terrainLevel(Terrains.RUBBLE) > 5) {
                mod++;
            }
            // append the reason modifier
            roll.append(new PilotingRollData(getId(), mod, "entering Rubble"));
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE, "Check false: Entity is not entering rubble");
        }

        return roll;
    }

    /**
     * Checks if the entity is moving into a hex that might cause it to bog
     * down. If so, returns the target roll for the piloting skill check.
     */
    public PilotingRollData checkBogDown(MoveStep step, IHex curHex, Coords lastPos, Coords curPos, int lastElev, boolean isPavementStep) {
        PilotingRollData roll = getBasePilotingRoll(step.getParent().getLastStepMovementType());
        int bgMod = curHex.getBogDownModifier(getMovementMode(), this instanceof LargeSupportTank);
        if ((!lastPos.equals(curPos) || (step.getElevation() != lastElev)) && (bgMod != TargetRoll.AUTOMATIC_SUCCESS) && (step.getMovementType() != IEntityMovementType.MOVE_JUMP) && (step.getElevation() == 0) && !isPavementStep) {
            roll.append(new PilotingRollData(getId(), bgMod, "avoid bogging down"));
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE, "Check false: Not entering bog-down terrain, or jumping/hovering over such terrain");
        }
        return roll;
    }

    /**
     * Checks if the entity is moving into depth 1+ water. If so, returns the
     * target roll for the piloting skill check.
     */
    public PilotingRollData checkWaterMove(MoveStep step, IHex curHex, Coords lastPos, Coords curPos, boolean isPavementStep) {
        if ((curHex.terrainLevel(Terrains.WATER) > 0) && (step.getElevation() < 0) && !lastPos.equals(curPos) && (step.getMovementType() != IEntityMovementType.MOVE_JUMP) && (getMovementMode() != IEntityMovementMode.HOVER) && (getMovementMode() != IEntityMovementMode.VTOL) && (getMovementMode() != IEntityMovementMode.NAVAL) && (getMovementMode() != IEntityMovementMode.HYDROFOIL) && (getMovementMode() != IEntityMovementMode.SUBMARINE) && (getMovementMode() != IEntityMovementMode.INF_UMU) && (getMovementMode() != IEntityMovementMode.BIPED_SWIM) && (getMovementMode() != IEntityMovementMode.QUAD_SWIM) && (getMovementMode() != IEntityMovementMode.WIGE) && !isPavementStep) {
            return checkWaterMove(curHex.terrainLevel(Terrains.WATER), step.getParent().getLastStepMovementType());
        }
        return checkWaterMove(0, step.getParent().getLastStepMovementType());
    }

    /**
     * Checks if the entity is moving into depth 1+ water. If so, returns the
     * target roll for the piloting skill check.
     */
    public PilotingRollData checkWaterMove(int waterLevel, int overallMoveType) {
        PilotingRollData roll = getBasePilotingRoll(overallMoveType);

        int mod;
        if (waterLevel == 1) {
            mod = -1;
        } else if (waterLevel == 2) {
            mod = 0;
        } else {
            mod = 1;
        }

        if ((waterLevel > 0) && !hasUMU()) {
            // append the reason modifier
            roll.append(new PilotingRollData(getId(), mod, "entering Depth " + waterLevel + " Water"));
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE, "Check false: No water here.");
        }

        return roll;
    }

    /**
     * Checks if the entity is being swarmed. If so, returns the target roll for
     * the piloting skill check to dislodge them.
     */
    public PilotingRollData checkDislodgeSwarmers(MoveStep step) {

        // If we're not being swarmed, return CHECK_FALSE
        if (Entity.NONE == getSwarmAttackerId()) {
            return new PilotingRollData(getId(), TargetRoll.CHECK_FALSE, "Check false: No swarmers attached");
        }

        // append the reason modifier
        PilotingRollData roll = getBasePilotingRoll(step.getParent().getLastStepMovementType());
        roll.append(new PilotingRollData(getId(), 0, "attempting to dislodge swarmers by dropping prone"));
        addPilotingModifierForTerrain(roll, step);

        return roll;
    }

    /**
     * Checks to see if an entity is moving through building walls. Note: this
     * method returns true/false, unlike the other checkStuff() methods above.
     *
     * @return 0, no eligable building; 1, exiting; 2, entering; 3, both; 4,
     *         stepping on roof
     */
    public int checkMovementInBuilding(MoveStep step, MoveStep prevStep, Coords curPos, Coords prevPos) {
        if (prevPos.equals(curPos)) {
            return 0;
        }
        IHex curHex = game.getBoard().getHex(curPos);
        IHex prevHex = game.getBoard().getHex(prevPos);
        // ineligable because of movement type or unit type
        if ((this instanceof Infantry) && (step.getMovementType() != IEntityMovementType.MOVE_JUMP)) {
            return 0;
        }

        if ((this instanceof Protomech) && (prevStep != null) && (prevStep.getMovementType() == IEntityMovementType.MOVE_JUMP)) {
            return 0;
        }

        int rv = 0;
        // check current hex for building
        if (step.getElevation() < curHex.terrainLevel(Terrains.BLDG_ELEV)) {
            rv += 2;
        } else if (((step.getElevation() == curHex.terrainLevel(Terrains.BLDG_ELEV)) || (step.getElevation() == curHex.terrainLevel(Terrains.BRIDGE_ELEV))) && (step.getMovementType() != IEntityMovementType.MOVE_JUMP)) {
            rv += 4;
        }
        // check previous hex for building
        if (prevHex != null) {
            int prevEl = getElevation();
            if (prevStep != null) {
                prevEl = prevStep.getElevation();
            }
            if (prevEl < prevHex.terrainLevel(Terrains.BLDG_ELEV)) {
                rv += 1;
            }
        }

        // check to see if its a wall
        if (rv > 1) {
            Building bldgEntered = null;
            bldgEntered = game.getBoard().getBuildingAt(curPos);
            if (bldgEntered.getType() == Building.WALL) {
                return 4;
            }
        }

        if ((this instanceof Infantry) || (this instanceof Protomech)) {
            if (rv != 2) {
                rv = 0;
            }
        }
        return rv;
    }

    /**
     * Calculates and returns the roll for an entity moving in buildings.
     */
    public PilotingRollData rollMovementInBuilding(Building bldg, int distance, String why, int overallMoveType) {
        PilotingRollData roll = getBasePilotingRoll(overallMoveType);

        int mod = 0;
        String desc;

        if (why == "") {
            desc = "moving through ";
        } else {
            desc = why + " ";
        }

        switch (bldg.getType()) {
        case Building.LIGHT:
            desc = "Light";
            break;
        case Building.MEDIUM:
            mod = 1;
            desc = "Medium";
            break;
        case Building.HEAVY:
            mod = 2;
            desc = "Heavy";
            break;
        case Building.HARDENED:
            mod = 5;
            desc = "Hardened";
            break;
        case Building.WALL:
            mod = 12;
            desc = "";
            break;
        }

        // append the reason modifier
        roll.append(new PilotingRollData(getId(), mod, "moving through " + desc + " " + bldg.getName()));

        // Modify the roll by the distance moved so far.
        if (distance >= 25) {
            roll.addModifier(6, "moved 25+ hexes");
        } else if (distance >= 18) {
            roll.addModifier(5, "moved 18-24 hexes");
        } else if (distance >= 10) {
            roll.addModifier(4, "moved 10+ hexes");
        } else if (distance >= 7) {
            roll.addModifier(3, "moved 7-9 hexes");
        } else if (distance >= 5) {
            roll.addModifier(2, "moved 5-6 hexes");
        } else if (distance >= 3) {
            roll.addModifier(1, "moved 3-4 hexes");
        }

        return roll;
    }

    /**
     * Calculate the piloting skill roll modifier, based upon the number of
     * hexes moved this phase. Used for skidding.
     */
    public int getMovementBeforeSkidPSRModifier(int distance) {
        int mod = -1;

        if (distance > 24) {
            mod = 6;
        } else if (distance > 17) {
            mod = 5;
        } else if (distance > 10) {
            mod = 4;
        } else if (distance > 7) {
            mod = 2;
        } else if (distance > 4) {
            mod = 1;
        } else if (distance > 2) {
            mod = 0;
        } else {
            // 0-2 hexes
            mod = -1;
        }

        if (getCrew().getOptions().booleanOption("maneuvering_ace")) {
            mod--;
        }

        return mod;
    }

    /**
     * The maximum elevation change the entity can cross
     */
    public abstract int getMaxElevationChange();

    /**
     * by default, entities can move as far down as they can move up
     */
    public int getMaxElevationDown() {
        return getMaxElevationChange();
    }

    /**
     * Get a list of the class names of the <code>Transporter</code>s for the
     * given <code>Entity</code>.
     * <p/>
     * This method should <strong>only</strong> be used when serializing the
     * <code>Entity</code>.
     *
     * @param entity
     *            - the <code>Entity</code> being serialized.
     * @return a <code>String</code> listing the <code>Transporter</code> class
     *         names of the <code>Entity</code>. This value will be empty ("")
     *         if the entity has no <code>Transporter</code>s.
     */
    public static String encodeTransporters(Entity entity) {
        StringBuffer buffer = new StringBuffer();
        Enumeration<Transporter> iter = entity.transports.elements();
        boolean isFirst = true;
        while (iter.hasMoreElements()) {

            // Every entry after the first gets a leading comma.
            if (isFirst) {
                isFirst = false;
            } else {
                buffer.append(',');
            }

            // Add the next Transporter's class name.
            Transporter transporter = iter.nextElement();
            buffer.append(transporter.getClass().getName());

            // If this is a TroopSpace transporter, get it's capacity.
            if (transporter instanceof TroopSpace) {
                buffer.append("=").append(((TroopSpace) transporter).totalSpace);
            }
        }
        return buffer.toString();
    }

    /**
     * Set the <code>Transporter</code>s for the given <code>Entity</code>.
     * <p/>
     * This method should <strong>only</strong> be used when deserializing the
     * <code>Entity</code>.
     *
     * @param entity
     *            - the <code>Entity</code> being deserialized.
     * @param transporters
     *            - the <code>String</code> listing the class names of the
     *            <code>Transporter</code>s to be set for the
     *            <code>Entity</code>, separated by commas. This value can be
     *            <code>null</code> or empty ("").
     * @throws IllegalStateException
     *             if any error occurs.
     */
    public static void decodeTransporters(Entity entity, String transporters) throws IllegalStateException {

        // Split the string on the commas, and add transporters to the Entity.
        Enumeration<String> iter = StringUtil.splitString(transporters, ",").elements();
        while (iter.hasMoreElements()) {
            try {
                String name = iter.nextElement();
                Class<?> transporter = Class.forName(name);
                Object object = null;
                if (TroopSpace.class.getName().equals(name)) {
                    // Get the tonnage of space.
                    int tonnage = Integer.parseInt(name.substring(name.lastIndexOf("=")));
                    object = new TroopSpace(tonnage);
                } else {
                    object = transporter.newInstance();
                }
                entity.addTransporter((Transporter) object);
            } catch (Exception err) {
                err.printStackTrace();
                throw new IllegalStateException(err.getMessage());
            }

        } // Handle the next transporter

    }

    /**
     * Add a transportation component to this Entity. Please note, this method
     * should only be called during this entity's construction.
     *
     * @param component
     *            - One of this new entity's <code>Transporter</code>s.
     */
    public void addTransporter(Transporter component) {
        // Add later transporters to the *head* of the list
        // to make Fa Shih's use their magnetic clamps by default.
        transports.insertElementAt(component, 0);
    }

    /**
     * Remove all transportation components from this Entity. Should probably
     * only be called during construction.
     */
    public void removeAllTransporters() {
        transports = new Vector<Transporter>();
    }

    /**
     * Determines if this object can accept the given unit. The unit may not be
     * of the appropriate type or there may be no room for the unit.
     *
     * @param unit
     *            - the <code>Entity</code> to be loaded.
     * @return <code>true</code> if the unit can be loaded, <code>false</code>
     *         otherwise.
     */
    public boolean canLoad(Entity unit) {
        // For now, if it's infantry, it can't load anything.
        // Period!
        if (this instanceof Infantry) {
            return false;
        }

        // one can only load one's own team's units!
        if (!unit.isEnemyOf(this)) {
            // Walk through this entity's transport components;
            // if one of them can load the unit, we can.
            Enumeration<Transporter> iter = transports.elements();
            while (iter.hasMoreElements()) {
                Transporter next = iter.nextElement();
                if (next.canLoad(unit) && (unit.getElevation() == getElevation())) {
                    return true;
                }
            }
        }

        // If we got here, none of our transports can carry the unit.
        return false;
    }

    /**
     * Load the given unit.
     *
     * @param unit
     *            - the <code>Entity</code> to be loaded.
     * @throws IllegalArgumentException
     *             If the unit can't be loaded
     */
    public void load(Entity unit) {
        // Walk through this entity's transport components;
        // find the one that can load the unit.
        // Stop looking after the first match.
        Enumeration<Transporter> iter = transports.elements();
        while (iter.hasMoreElements()) {
            Transporter next = iter.nextElement();
            if (next.canLoad(unit) && (unit.getElevation() == getElevation())) {
                next.load(unit);
                return;
            }
        }

        // If we got to this point, then we can't load the unit.
        throw new IllegalArgumentException(getShortName() + " can not load " + unit.getShortName());
    }

    /**
     * Recover the given unit. Only for ASF and Small Craft
     *
     * @param unit
     *            - the <code>Entity</code> to be loaded.
     * @throws IllegalArgumentException
     *             If the unit can't be loaded
     */
    public void recover(Entity unit) {
        // Walk through this entity's transport components;
        // find the one that can load the unit.
        // Stop looking after the first match.
        Enumeration<Transporter> iter = transports.elements();
        while (iter.hasMoreElements()) {
            Transporter next = iter.nextElement();
            if (next.canLoad(unit) && (unit.getElevation() == getElevation())) {
                if (next instanceof ASFBay) {
                    ((ASFBay) next).recover(unit);
                    return;
                }
                if (next instanceof SmallCraftBay) {
                    ((SmallCraftBay) next).recover(unit);
                    return;
                }
            }
        }

        // If we got to this point, then we can't load the unit.
        throw new IllegalArgumentException(getShortName() + " can not recover " + unit.getShortName());
    }

    /**
     * cycle through and update Bays
     */
    public void updateBays() {
        Enumeration<Transporter> iter = transports.elements();
        while (iter.hasMoreElements()) {
            Transporter next = iter.nextElement();
            if (next instanceof ASFBay) {
                ASFBay nextBay = (ASFBay) next;
                nextBay.updateSlots();
            }
        }
    }

    /**
     * Damages a randomly determined bay door on the entity, if one exists
     *
     */
    public String damageBayDoor() {

        String bayType = "none";

        Vector<Bay> potential;
        potential = new Vector<Bay>();

        Enumeration<Transporter> iter = transports.elements();
        while (iter.hasMoreElements()) {
            Transporter next = iter.nextElement();
            if (next instanceof Bay) {
                Bay nextBay = (Bay) next;
                if (nextBay.getDoors() > 0) {
                    potential.add(nextBay);
                }
            }
        }

        if (potential.size() > 0) {
            Bay chosenBay = potential.elementAt(Compute.randomInt(potential.size()));
            chosenBay.destroyDoor();
            chosenBay.resetDoors();
            chosenBay.setDoors(chosenBay.getDoors() - 1);
            bayType = chosenBay.getType();
        }

        return bayType;
    }

    /**
     * damage the door of the first bay that can load this unit
     */
    public void damageDoorRecovery(Entity en) {
        Enumeration<Transporter> iter = transports.elements();
        while (iter.hasMoreElements()) {
            Transporter next = iter.nextElement();
            if ((next instanceof ASFBay) && next.canLoad(en)) {
                ((ASFBay) next).destroyDoor();
                break;
            }
            if ((next instanceof SmallCraftBay) && next.canLoad(en)) {
                ((SmallCraftBay) next).destroyDoor();
                break;
            }

        }
    }

    /**
     * Damages a randomly determined docking collar on the entity, if one exists
     *
     */
    public boolean damageDockCollar() {

        boolean result = false;

        Vector<DockingCollar> potential;
        potential = new Vector<DockingCollar>();

        Enumeration<Transporter> iter = transports.elements();
        while (iter.hasMoreElements()) {
            Transporter next = iter.nextElement();
            if (next instanceof DockingCollar) {
                DockingCollar nextDC = (DockingCollar) next;
                if (!nextDC.isDamaged()) {
                    potential.add(nextDC);
                }
            }
        }

        if (potential.size() > 0) {
            DockingCollar chosenDC = potential.elementAt(Compute.randomInt(potential.size()));
            chosenDC.setDamaged(true);
            result = true;
        }

        return result;
    }

    public void pickUp(MechWarrior mw) {
        pickedUpMechWarriors.addElement(new Integer(mw.getId()));
    }

    /**
     * Get a <code>List</code> of the units currently loaded into this payload.
     *
     * @return A <code>List</code> of loaded <code>Entity</code> units. This
     *         list will never be <code>null</code>, but it may be empty. The
     *         returned <code>List</code> is independant from the under- lying
     *         data structure; modifying one does not affect the other.
     */
    public Vector<Entity> getLoadedUnits() {
        Vector<Entity> result = new Vector<Entity>();

        // Walk through this entity's transport components;
        // add all of their lists to ours.
        for (Transporter next : transports) {
            for (Entity e : next.getLoadedUnits()) {
                result.addElement(e);
            }
        }

        // Return the list.
        return result;
    }

    /**
     * @return the number of docking collars
     */
    public int getDocks() {
        int n = 0;

        for (Transporter next : transports) {
            if (next instanceof DockingCollar) {
                n++;
            }
        }

        // Return the number
        return n;

    }

    /**
     * only entities in Bays (for cargo damage to Aero units
     *
     * @return
     */
    public Vector<Entity> getBayLoadedUnits() {
        Vector<Entity> result = new Vector<Entity>();

        // Walk through this entity's transport components;
        // add all of their lists to ours.
        for (Transporter next : transports) {
            if (next instanceof Bay) {
                for (Entity e : next.getLoadedUnits()) {
                    result.addElement(e);
                }
            }
        }

        // Return the list.
        return result;
    }

    /**
     * @return only entities in ASF Bays
     */
    public Vector<Entity> getLoadedFighters() {
        Vector<Entity> result = new Vector<Entity>();

        // Walk through this entity's transport components;
        // add all of their lists to ours.

        // I should only add entities in bays that are functional
        for (Transporter next : transports) {
            if ((next instanceof ASFBay) && (((ASFBay) next).getDoors() > 0)) {
                for (Entity e : next.getLoadedUnits()) {
                    result.addElement(e);
                }
            }
        }

        // Return the list.
        return result;
    }

    /**
     * @return only entities in ASF Bays that can be launched (i.e. not in
     *         recovery)
     */
    public Vector<Entity> getLaunchableFighters() {
        Vector<Entity> result = new Vector<Entity>();

        // Walk through this entity's transport components;
        // add all of their lists to ours.

        // I should only add entities in bays that are functional
        for (Transporter next : transports) {
            if ((next instanceof ASFBay) && (((ASFBay) next).getDoors() > 0)) {
                Bay nextbay = (Bay) next;
                for (Entity e : nextbay.getLaunchableUnits()) {
                    result.addElement(e);
                }
            }
        }

        // Return the list.
        return result;
    }

    public Bay getLoadedBay(int bayID) {

        Vector<Bay> bays = getFighterBays();
        for (int nbay = 0; nbay < bays.size(); nbay++) {
            Bay currentBay = bays.elementAt(nbay);
            Vector<Entity> currentFighters = currentBay.getLoadedUnits();
            for (int nfighter = 0; nfighter < currentFighters.size(); nfighter++) {
                Entity fighter = currentFighters.elementAt(nfighter);
                if (fighter.getId() == bayID) {
                    // then we are in the right bay
                    return currentBay;
                }
            }
        }

        return null;

    }

    /**
     * @return get the bays separately
     */
    public Vector<Bay> getFighterBays() {
        Vector<Bay> result = new Vector<Bay>();

        for (Transporter next : transports) {
            if (((next instanceof ASFBay) || (next instanceof SmallCraftBay)) && (((Bay) next).getDoors() > 0)) {
                result.addElement((Bay) next);
            }
        }

        // Return the list.
        return result;

    }

    public Vector<Bay> getTransportBays() {

        Vector<Bay> result = new Vector<Bay>();

        for (Transporter next : transports) {
            if (next instanceof Bay) {
                result.addElement((Bay) next);
            }
        }

        // Return the list.
        return result;
    }

    /**
     * do any damage to bay doors
     */
    public void resetBayDoors() {

        for (Transporter next : transports) {
            if (next instanceof Bay) {
                ((Bay) next).resetDoors();
            }
        }

    }

    /**
     * @return the launch rate for fighters
     */
    public int getFighterLaunchRate() {
        int result = 0;

        // Walk through this entity's transport components;
        for (Transporter next : transports) {
            if (next instanceof ASFBay) {
                result += 2 * ((ASFBay) next).getDoors();
            }
        }
        // Return the number.
        return result;
    }

    public Vector<Entity> getLoadedSmallCraft() {
        Vector<Entity> result = new Vector<Entity>();

        // Walk through this entity's transport components;
        // add all of their lists to ours.
        for (Transporter next : transports) {
            if ((next instanceof SmallCraftBay) && (((SmallCraftBay) next).getDoors() > 0)) {
                for (Entity e : next.getLoadedUnits()) {
                    result.addElement(e);
                }
            }
        }

        // Return the list.
        return result;
    }

    public Vector<Entity> getLaunchableSmallCraft() {
        Vector<Entity> result = new Vector<Entity>();

        // Walk through this entity's transport components;
        // add all of their lists to ours.
        for (Transporter next : transports) {
            if ((next instanceof SmallCraftBay) && (((SmallCraftBay) next).getDoors() > 0)) {
                Bay nextbay = (Bay) next;
                for (Entity e : nextbay.getLaunchableUnits()) {
                    result.addElement(e);
                }
            }
        }

        // Return the list.
        return result;
    }

    /**
     * get the bays separately
     */
    public Vector<SmallCraftBay> getSmallCraftBays() {
        Vector<SmallCraftBay> result = new Vector<SmallCraftBay>();

        for (Transporter next : transports) {
            if ((next instanceof SmallCraftBay) && (((SmallCraftBay) next).getDoors() > 0)) {
                result.addElement((SmallCraftBay) next);
            }
        }

        // Return the list.
        return result;

    }

    /**
     * @return launch rate for Small Craft
     */
    public int getSmallCraftLaunchRate() {
        int result = 0;

        // Walk through this entity's transport components;
        for (Transporter next : transports) {
            if (next instanceof SmallCraftBay) {
                result += 2 * ((SmallCraftBay) next).getDoors();
            }
        }
        // Return the number.
        return result;
    }

    /**
     * Unload the given unit.
     *
     * @param unit
     *            - the <code>Entity</code> to be unloaded.
     * @return <code>true</code> if the unit was contained in this space,
     *         <code>false</code> otherwise.
     */
    public boolean unload(Entity unit) {
        // Walk through this entity's transport components;
        // try to remove the unit from each in turn.
        // Stop after the first match.
        Enumeration<Transporter> iter = transports.elements();
        while (iter.hasMoreElements()) {
            Transporter next = iter.nextElement();
            if (next.unload(unit)) {
                return true;
            }
        }

        // If we got here, none of our transports currently carry the unit.
        return false;
    }

    /**
     * Return a string that identifies the unused capacity of this transporter.
     *
     * @return A <code>String</code> meant for a human.
     */
    public String getUnusedString() {
        StringBuffer result = new StringBuffer();

        // Walk through this entity's transport components;
        // add all of their string to ours.
        Enumeration<Transporter> iter = transports.elements();
        while (iter.hasMoreElements()) {
            Transporter next = iter.nextElement();
            result.append(next.getUnusedString());
            // Add a newline character between strings.
            if (iter.hasMoreElements()) {
                result.append('\n');
            }
        }

        // Return the String.
        return result.toString();
    }

    /**
     * Determine if transported units prevent a weapon in the given location
     * from firing.
     *
     * @param loc
     *            - the <code>int</code> location attempting to fire.
     * @param isRear
     *            - a <code>boolean</code> value stating if the given location
     *            is rear facing; if <code>false</code>, the location is front
     *            facing.
     * @return <code>true</code> if a transported unit is in the way,
     *         <code>false</code> if the weapon can fire.
     */
    public boolean isWeaponBlockedAt(int loc, boolean isRear) {
        // Walk through this entity's transport components;
        // check each for blockage in turn.
        // Stop after the first match.
        for (Transporter next : transports) {
            if (next.isWeaponBlockedAt(loc, isRear)) {
                return true;
            }
        }

        // If we got here, none of our transports
        // carry a blocking unit at that location.
        return false;
    }

    /**
     * If a unit is being transported on the outside of the transporter, it can
     * suffer damage when the transporter is hit by an attack. Currently, no
     * more than one unit can be at any single location; that same unit can be
     * "spread" over multiple locations.
     *
     * @param loc
     *            - the <code>int</code> location hit by attack.
     * @param isRear
     *            - a <code>boolean</code> value stating if the given location
     *            is rear facing; if <code>false</code>, the location is front
     *            facing.
     * @return The <code>Entity</code> being transported on the outside at that
     *         location. This value will be <code>null</code> if no unit is
     *         transported on the outside at that location.
     */
    public Entity getExteriorUnitAt(int loc, boolean isRear) {
        // Walk through this entity's transport components;
        // check each for an exterior unit in turn.
        // Stop after the first match.
        for (Transporter next : transports) {
            Entity exterior = next.getExteriorUnitAt(loc, isRear);
            if (null != exterior) {
                return exterior;
            }
        }

        // If we got here, none of our transports
        // carry an exterior unit at that location.
        return null;
    }

    public ArrayList<Entity> getExternalUnits() {
        ArrayList<Entity> rv = new ArrayList<Entity>();
        for (Transporter t : transports) {
            rv.addAll(t.getExternalUnits());
        }
        return rv;
    }

    public int getCargoMpReduction() {
        int rv = 0;
        for (Transporter t : transports) {
            rv += t.getCargoMpReduction();
        }
        return rv;
    }

    public HitData getTrooperAtLocation(HitData hit, Entity transport) {
        return rollHitLocation(ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT);
    }

    /**
     * Record the ID of the <code>Entity</code> that has loaded this unit. A
     * unit that is unloaded can neither move nor attack for the rest of the
     * turn.
     *
     * @param transportId
     *            - the <code>int</code> ID of our transport. The ID is
     *            <b>not</b> validated. This value should be
     *            <code>Entity.NONE</code> if this unit has been unloaded.
     */
    public void setTransportId(int transportId) {
        conveyance = transportId;
        // If we were unloaded, set the appropriate flags.
        if (transportId == Entity.NONE) {
            unloadedThisTurn = true;
            done = true;
        }
    }

    /**
     * Get the ID <code>Entity</code> that has loaded this one.
     *
     * @return the <code>int</code> ID of our transport. The ID may be invalid.
     *         This value should be <code>Entity.NONE</code> if this unit has
     *         not been loaded.
     */
    public int getTransportId() {
        return conveyance;
    }

    /**
     * Determine if this unit has an active stealth system.
     * <p/>
     * Sub-classes are encouraged to override this method.
     *
     * @return <code>true</code> if this unit has a stealth system that is
     *         currently active, <code>false</code> if there is no stealth
     *         system or if it is inactive.
     */
    public boolean isStealthActive() {
        return false;
    }

    /**
     * Determine if this unit has an active null-signature system.
     * <p/>
     * Sub-classes are encouraged to override this method.
     *
     * @return <code>true</code> if this unit has a null signature system that
     *         is currently active, <code>false</code> if there is no stealth
     *         system or if it is inactive.
     */
    public boolean isNullSigActive() {
        return false;
    }

    /**
     * Determine if this unit has an active void signature system.
     * <p/>
     * Sub-classes are encouraged to override this method.
     *
     * @return <code>true</code> if this unit has a void signature system that
     *         is currently active, <code>false</code> if there is no stealth
     *         system or if it is inactive.
     */
    public boolean isVoidSigActive() {
        return false;
    }

    /**
     * Determine if this unit has an active chameleon light polarization field.
     * <p/>
     * Sub-classes are encouraged to override this method.
     *
     * @return <code>true</code> if this unit has a void signature system that
     *         is currently active, <code>false</code> if there is no stealth
     *         system or if it is inactive.
     */
    public boolean isChameleonShieldActive() {
        return false;
    }

    /**
     * Determine the stealth modifier for firing at this unit from the given
     * range. If the value supplied for <code>range</code> is not one of the
     * <code>Entity</code> class range constants, an
     * <code>IllegalArgumentException</code> will be thrown.
     * <p/>
     * Sub-classes are encouraged to override this method.
     *
     * @param range
     *            - an <code>int</code> value that must match one of the
     *            <code>Compute</code> class range constants.
     * @param ae
     *            - the entity making the attack, who maybe immune to certain
     *            kinds of stealth
     * @return a <code>TargetRoll</code> value that contains the stealth
     *         modifier for the given range.
     */
    public TargetRoll getStealthModifier(int range, Entity ae) {
        TargetRoll result = null;

        // Stealth must be active.
        if (!isStealthActive()) {
            result = new TargetRoll(0, "stealth not active");
        }

        // Get the range modifier.
        switch (range) {
        case RangeType.RANGE_MINIMUM:
        case RangeType.RANGE_SHORT:
        case RangeType.RANGE_MEDIUM:
        case RangeType.RANGE_LONG:
        case RangeType.RANGE_EXTREME:
            result = new TargetRoll(0, "stealth not installed");
            break;
        default:
            throw new IllegalArgumentException("Unknown range constant: " + range);
        }

        // Return the result.
        return result;
    }

    /**
     * Record the ID of the <code>Entity</code> that is the current target of a
     * swarm attack by this unit. A unit that stops swarming can neither move
     * nor attack for the rest of the turn.
     *
     * @param id
     *            - the <code>int</code> ID of the swarm attack's target. The ID
     *            is <b>not</b> validated. This value should be
     *            <code>Entity.NONE</code> if this unit has stopped swarming.
     */
    public void setSwarmTargetId(int id) {
        swarmTargetId = id;
        // This entity can neither move nor attack for the rest of this turn.
        if (id == Entity.NONE) {
            unloadedThisTurn = true;
            done = true;
        }
    }

    /**
     * Get the ID of the <code>Entity</code> that is the current target of a
     * swarm attack by this unit.
     *
     * @return the <code>int</code> ID of the swarm attack's target The ID may
     *         be invalid. This value should be <code>Entity.NONE</code> if this
     *         unit is not swarming.
     */
    public int getSwarmTargetId() {
        return swarmTargetId;
    }

    /**
     * Record the ID of the <code>Entity</code> that is attacking this unit with
     * a swarm attack.
     *
     * @param id
     *            - the <code>int</code> ID of the swarm attack's attacker. The
     *            ID is <b>not</b> validated. This value should be
     *            <code>Entity.NONE</code> if the swarm attack has ended.
     */
    public void setSwarmAttackerId(int id) {
        swarmAttackerId = id;
    }

    /**
     * Get the ID of the <code>Entity</code> that is attacking this unit with a
     * swarm attack.
     *
     * @return the <code>int</code> ID of the swarm attack's attacker The ID may
     *         be invalid. This value should be <code>Entity.NONE</code> if this
     *         unit is not being swarmed.
     */
    public int getSwarmAttackerId() {
        return swarmAttackerId;
    }

    /**
     * Scans through the ammo on the unit for any inferno rounds.
     *
     * @return <code>true</code> if the unit is still loaded with Inferno
     *         rounds. <code>false</code> if no rounds were ever loaded or if
     *         they have all been fired.
     */
    public boolean hasInfernoAmmo() {
        boolean found = false;

        // Walk through the unit's ammo, stop when we find a match.
        for (Mounted amounted : getAmmo()) {
            AmmoType atype = (AmmoType) amounted.getType();
            if (((atype.getAmmoType() == AmmoType.T_SRM) || (atype.getAmmoType() == AmmoType.T_MML)) && (atype.getMunitionType() == AmmoType.M_INFERNO) && (amounted.getShotsLeft() > 0)) {
                found = true;
            }
        }
        return found;
    }

    /**
     * Record if the unit is just combat-lossed or if it has been utterly
     * destroyed.
     *
     * @param canSalvage
     *            - a <code>boolean</code> that is <code>true</code> if the unit
     *            can be repaired (given time and parts); if this value is
     *            <code>false</code>, the unit is utterly destroyed.
     */
    public void setSalvage(boolean canSalvage) {
        // Unsalvageable entities aren't in retreat or salvageable.
        if (!canSalvage) {
            setRemovalCondition(IEntityRemovalConditions.REMOVE_DEVASTATED);
        }
        salvageable = canSalvage;
    }

    /**
     * Determine if the unit is just combat-lossed or if it has been utterly
     * destroyed.
     *
     * @return A <code>boolean</code> that is <code>true</code> if the unit has
     *         salvageable components; if this value is <code>false</code> the
     *         unit is utterly destroyed.
     * @see #isRepairable()
     */
    public boolean isSalvage() {
        return salvageable;
    }

    /**
     * Determine if the unit can be repaired, or only harvested for spares.
     *
     * @return A <code>boolean</code> that is <code>true</code> if the unit can
     *         be repaired (given enough time and parts); if this value is
     *         <code>false</code>, the unit is only a source of spares.
     * @see #isSalvage()
     */
    public boolean isRepairable() {
        return isSalvage();
    }

    /**
     * Getter for property removalCondition.
     *
     * @return Value of property removalCondition.
     */
    public int getRemovalCondition() {
        return removalCondition;
    }

    /**
     * Setter for property removalCondition.
     *
     * @param removalCondition
     *            New value of property removalCondition.
     */
    public void setRemovalCondition(int removalCondition) {
        // Don't replace a removal condition with a lesser condition.
        if (this.removalCondition < removalCondition) {
            this.removalCondition = removalCondition;
        }
    }

    /**
     * @return whether this entity is clearing a minefield.
     */
    public boolean isClearingMinefield() {
        return clearingMinefield;
    }

    /**
     * @param clearingMinefield
     */
    public void setClearingMinefield(boolean clearingMinefield) {
        this.clearingMinefield = clearingMinefield;
    }

    /**
     * @return whether this entity is spotting this round.
     */
    public boolean isSpotting() {
        return spotting;
    }

    /**
     * @param spotting
     */
    public void setSpotting(boolean spotting) {
        this.spotting = spotting;
    }

    /**
     * Um, basically everything can spot for LRM indirect fire.
     *
     * @return true, if the entity is active
     */
    public boolean canSpot() {
        if (game.getOptions().booleanOption("pilots_cannot_spot") && (this instanceof MechWarrior)) {
            return false;
        }
        return isActive() && !isOffBoard();
    }

    @Override
    public String toString() {
        return "Entity [" + getDisplayName() + ", " + getId() + "]";
    }

    /**
     * This returns a textual description of the entity for visualy impaired
     * users.
     */
    public String statusToString() {
        // should include additional information like imobile.
        String str = "Entity [" + getDisplayName() + ", " + getId() + "]: ";
        if (getPosition() != null) {
            str = str + "Location: (" + (getPosition().x + 1) + ", " + (getPosition().y + 1) + ") ";
        }
        str = str + "Owner: " + owner.getName() + " Armor: " + getTotalArmor() + "/" + getTotalOArmor() + " Internal Structure: " + getTotalInternal() + "/" + getTotalOInternal();

        if (!isActive()) {
            str += " Inactive";
        }
        if (isImmobile()) {
            str += " Immobile";
        }
        if (isProne()) {
            str += " Prone";
        }
        if (isDone()) {
            str += " Done";
        }

        return str;
    }

    /**
     * This returns a textual description of a specific location of the entity
     * for visualy impaired users.
     *
     * @param loc
     *            the location
     * @return a string descibing the status of the location.
     */
    public String statusToString(int loc) {
        if (loc == LOC_NONE) {
            return "No location given.";
        }

        return getLocationName(loc) + " (" + getLocationAbbr(loc) + "): Armor: " + getArmorString(loc) + "/" + getOArmor(loc) + " Structure: " + getInternalString(loc) + "/" + getOInternal(loc);
    }

    /**
     * @param string
     *            a string defining the location
     * @return the status of the given location.
     */
    public String statusToString(String str) {
        int loc = LOC_NONE;
        loc = getLocationFromAbbr(str);

        if (loc == LOC_NONE) {
            try {
                loc = Integer.parseInt(str);
            } catch (NumberFormatException nfe) {
                loc = LOC_NONE;
            }
        }

        return statusToString(loc);
    }

    /**
     * The round the unit will be deployed. We will deploy at the end of a
     * round. So if depoyRound is set to 5, we will deploy when round 5 is over.
     * Any value of zero or less is automatically set to 1
     *
     * @param deployRound
     *            an int
     */
    public void setDeployRound(int deployRound) {
        this.deployRound = deployRound;
    }

    /**
     * The round the unit will be deployed
     *
     * @return an int
     */
    public int getDeployRound() {
        return deployRound;
    }

    /**
     * Toggles if an entity has been deployed
     */
    public void setDeployed(boolean deployed) {
        this.deployed = deployed;
    }

    /**
     * Checks to see if an entity has been deployed
     */
    public boolean isDeployed() {
        return deployed;
    }

    /**
     * Returns true if the entity should be deployed
     */
    public boolean shouldDeploy(int round) {
        return (!deployed && (getDeployRound() <= round) && !isOffBoard());
    }

    /**
     * Set the unit number for this entity.
     *
     * @param unit
     *            the <code>char</code> number for the low-level unit that this
     *            entity belongs to. This entity can be removed from its unit by
     *            passing the value, <code>(char) Entity.NONE</code>.
     */
    public void setUnitNumber(char unit) {
        unitNumber = unit;
    }

    /**
     * Get the unit number of this entity.
     *
     * @return the <code>char</code> unit number. If the entity does not belong
     *         to a unit, <code>(char) Entity.NONE</code> will be returned.
     */
    public char getUnitNumber() {
        return unitNumber;
    }

    /**
     * Returns whether an entity can flee from its current position. Currently
     * returns true if the entity is on the edge of the board.
     */
    public boolean canFlee() {
        Coords pos = getPosition();
        return (pos != null) && (getWalkMP() > 0) && !isProne() && !isStuck() && !isShutDown() && !getCrew().isUnconscious() && ((pos.x == 0) || (pos.x == game.getBoard().getWidth() - 1) || (pos.y == 0) || (pos.y == game.getBoard().getHeight() - 1));
    }

    public void setSeenByEnemy(boolean b) {
        seenByEnemy = b;
    }

    public boolean isSeenByEnemy() {
        return seenByEnemy;
    }

    public void setVisibleToEnemy(boolean b) {
        visibleToEnemy = b;
    }

    public boolean isVisibleToEnemy() {
        return visibleToEnemy;
    }

    protected int applyGravityEffectsOnMP(int MP) {
        int result = MP;
        if (game != null) {
            float fMP = MP / game.getPlanetaryConditions().getGravity();
            fMP = (Math.abs((Math.round(fMP) - fMP)) == 0.5) ? (float) Math.floor(fMP) : Math.round(fMP); // the
            // rule
            // requires
            // rounding down on .5
            result = (int) fMP;
        }
        return result;
    }

    /** Whether this type of unit can perform charges */
    public boolean canCharge() {
        return !isImmobile() && (getWalkMP() > 0) && !isStuck() && !isProne();
    }

    /** Whether this type of unit can perform DFA attacks */
    public boolean canDFA() {
        return !isImmobile() && (getJumpMP() > 0) && !isStuck() && !isProne();
    }

    /** Whether this type of unit can perform Ramming attacks */
    public boolean canRam() {
        return false;
    }

    boolean isUsingManAce() {
        return getCrew().getOptions().booleanOption("maneuvering_ace");
    }

    public Enumeration<Entity> getKills() {
        final int killer = id;
        return game.getSelectedOutOfGameEntities(new EntitySelector() {
            public boolean accept(Entity entity) {
                if (killer == entity.killerId) {
                    return true;
                }
                return false;
            }
        });
    }

    public int getKillNumber() {
        final int killer = id;
        return game.getSelectedOutOfGameEntityCount(new EntitySelector() {
            public boolean accept(Entity entity) {
                if (killer == entity.killerId) {
                    return true;
                }
                return false;
            }
        });
    }

    public void addKill(Entity kill) {
        kill.killerId = id;
    }

    public boolean getGaveKillCredit() {
        return killerId != Entity.NONE;
    }

    /**
     * Determines if an entity is eligible for a phase.
     */
    public boolean isEligibleFor(IGame.Phase phase) {
        // only deploy in deployment phase
        if ((phase == IGame.Phase.PHASE_DEPLOYMENT) == isDeployed()) {
            return false;
        }

        // carcass can't do anything
        if (isCarcass()) {
            return false;
        }

        switch (phase) {
        case PHASE_MOVEMENT:
            return isEligibleForMovement();
        case PHASE_FIRING:
            return isEligibleForFiring();
        case PHASE_PHYSICAL:
            return isEligibleForPhysical();
        case PHASE_TARGETING:
            return isEligibleForTargetingPhase();
        case PHASE_OFFBOARD:
            return isEligibleForOffboard();
        default:
            return true;
        }
    }

    /**
     * Determines if an entity is eligible for a phase. Called only if at least
     * one entity returned true to isEligibleFor() This is for using
     * searchlights in physical&offboard phase, without forcing the phase to be
     * played when not needed. However it could be used for other things in the
     * future
     */
    public boolean canAssist(IGame.Phase phase) {
        if ((phase != IGame.Phase.PHASE_PHYSICAL) && (phase != IGame.Phase.PHASE_FIRING) && (phase != IGame.Phase.PHASE_OFFBOARD)) {
            return false;
        }
        // if you're charging or finding a club, it's already declared
        if (isUnjammingRAC() || isCharging() || isMakingDfa() || isRamming() || isFindingClub() || isOffBoard()) {
            return false;
        }
        // must be active
        if (!isActive()) {
            return false;
        }
        // If we have a searchlight, we can use it to assist
        if (isUsingSpotlight()) {
            return true;
        }
        return false;
    }

    /**
     * An entity is eligible if its to-hit number is anything but impossible.
     * This is only really an issue if friendly fire is turned off.
     */
    public boolean isEligibleForFiring() {
        // if you're charging, no shooting
        if (isUnjammingRAC() || isCharging() || isMakingDfa() || isRamming()) {
            return false;
        }

        // if you're offboard, no shooting
        if (isOffBoard() || isAssaultDropInProgress()) {
            return false;
        }

        // check game options
        if (!game.getOptions().booleanOption("skip_ineligable_firing")) {
            return true;
        }

        // must be active
        if (!isActive()) {
            return false;
        }

        // TODO: check for any weapon attacks

        return true;
    }

    /**
     * Pretty much anybody's eligible for movement. If the game option is
     * toggled on, inactive and immobile entities are not eligible. OffBoard
     * units are always ineligible
     *
     * @return whether or not the entity is allowed to move
     */
    public boolean isEligibleForMovement() {
        // check if entity is offboard
        if (isOffBoard() || isAssaultDropInProgress()) {
            return false;
        }
        // check game options
        if (!game.getOptions().booleanOption("skip_ineligable_movement")) {
            return true;
        }

        // must be active
        if (!isActive() || isImmobile()) {
            return false;
        }

        return true;
    }

    public boolean isEligibleForOffboard() {

        // if you're charging, no shooting
        if (isUnjammingRAC() || isCharging() || isMakingDfa()) {
            return false;
        }

        // if you're offboard, no shooting
        if (isOffBoard() || isAssaultDropInProgress()) {
            return false;
        }
        for (Mounted mounted : getWeaponList()) {
            WeaponType wtype = (WeaponType) mounted.getType();
            if (wtype.hasFlag(WeaponType.F_TAG) && mounted.isReady()) {
                return true;
            }
        }
        return false;// only things w/ tag are
    }

    public boolean isAttackingThisTurn() {
        Vector<EntityAction> actions = game.getActionsVector();
        for (EntityAction ea : actions) {
            if ((ea.getEntityId() == getId()) && (ea instanceof AbstractAttackAction)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the entity has any valid targets for physical attacks.
     */
    public boolean isEligibleForPhysical() {
        boolean canHit = false;
        boolean friendlyFire = game.getOptions().booleanOption("friendly_fire");

        // only mechs and protos have physical attacks (except tank charges)
        if (!((this instanceof Mech) || (this instanceof Protomech) || (this instanceof Infantry))) {
            return false;
        }

        // if you're charging or finding a club, it's already declared
        if (isUnjammingRAC() || isCharging() || isMakingDfa() || isFindingClub() || isOffBoard() || isAssaultDropInProgress()) {
            return false;
        }

        // check game options
        if (game.getOptions().booleanOption("no_clan_physical") && isClan() && !hasINarcPodsAttached() && (getSwarmAttackerId() == NONE)) {
            return false;
        }

        // Issue with Vibroblades only being turned on/off during Physical phase
        // -- Torren
        if (hasVibroblades()) {
            return true;
        }

        if (!game.getOptions().booleanOption("skip_ineligable_physical")) {
            return true;
        }

        // dead mek walking
        if (!isActive()) {
            return false;
        }

        if (getPosition() == null) {
            return false; // not on board?
        }

        // check if we have iNarc pods attached that can be brushed off
        if (hasINarcPodsAttached() && (this instanceof Mech)) {
            return true;
        }

        // Try to find a valid entity target.
        Enumeration<Entity> e = game.getEntities();
        while (!canHit && e.hasMoreElements()) {
            Entity target = e.nextElement();

            // don't shoot at friendlies unless you are into that sort of thing
            // and do not shoot yourself even then
            if (!(isEnemyOf(target) || (friendlyFire && (getId() != target.getId())))) {
                continue;
            }

            // No physical attack works at distances > 1.
            if ((target.getPosition() == null) || (getPosition().distance(target.getPosition()) > 1)) {
                continue;
            }

            canHit |= Compute.canPhysicalTarget(game, getId(), target);
            // check if we can dodge and target can attack us,
            // then we are eligible.
            canHit |= ((this instanceof Mech) && !isProne() && getCrew().getOptions().booleanOption("dodge_maneuver") && Compute.canPhysicalTarget(game, target.getId(), this));
        }

        // If there are no valid Entity targets, check for add valid buildings.
        Enumeration<Building> bldgs = game.getBoard().getBuildings();
        while (!canHit && bldgs.hasMoreElements()) {
            final Building bldg = bldgs.nextElement();

            // Walk through the hexes of the building.
            Enumeration<Coords> hexes = bldg.getCoords();
            while (!canHit && hexes.hasMoreElements()) {
                final Coords coords = hexes.nextElement();

                // No physical attack works at distances > 1.
                if (getPosition().distance(coords) > 1) {
                    continue;
                }

                // Can the entity target *this* hex of the building?
                final BuildingTarget target = new BuildingTarget(coords, game.getBoard(), false);
                canHit |= Compute.canPhysicalTarget(game, getId(), target);

            } // Check the next hex of the building

        } // Check the next building

        return canHit;
    }

    public boolean isEligibleForTargetingPhase() {
        if (isAssaultDropInProgress()) {
            return false;
        }
        for (Mounted mounted : getWeaponList()) {
            WeaponType wtype = (WeaponType) mounted.getType();
            if ((wtype != null) && (wtype.hasFlag(WeaponType.F_ARTILLERY))) {
                return true;
            }
        }
        return false;
    }

    public int getTroopCarryingSpace() {
        int space = 0;
        for (Transporter t : transports) {
            if (t instanceof TroopSpace) {
                space += ((TroopSpace) t).totalSpace;
            }
        }
        return space;
    }

    public boolean hasBattleArmorHandles() {
        for (Transporter t : transports) {
            if (t instanceof BattleArmorHandles) {
                return true;
            }
        }
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Targetable#isOffBoard()
     */
    public boolean isOffBoard() {
        return offBoardDistance > 0;
    }

    /**
     * Set the unit as an offboard deployment. If a non-zero distance is chosen,
     * the direction must <b>not</b> be <code>Entity.NONE</code>. If a direction
     * other than <code>Entity.NONE</code> is chosen, the distance must
     * <b>not</b> be zero (0).
     *
     * @param distance
     *            the <code>int</code> distance in hexes that the unit will be
     *            deployed from the board; this value must not be negative.
     * @param direction
     *            the <code>int</code> direction from the board that the unit
     *            will be deployed; a valid value must be selected from: NONE,
     *            NORTH, SOUTH, EAST, or WEST.
     * @throws IllegalArgumentException
     *             if a negative distance, an invalid direction is selected, or
     *             the distance does not match the direction.
     */
    public void setOffBoard(int distance, int direction) {
        if (distance < 0) {
            throw new IllegalArgumentException("negative number given for distance offboard");
        }
        if ((direction < IOffBoardDirections.NONE) || (direction > IOffBoardDirections.WEST)) {
            throw new IllegalArgumentException("bad direction");
        }
        if ((0 == distance) && (IOffBoardDirections.NONE != direction)) {
            throw new IllegalArgumentException("onboard unit was given an offboard direction");
        }
        if ((0 != distance) && (IOffBoardDirections.NONE == direction)) {
            throw new IllegalArgumentException("offboard unit was not given an offboard direction");
        }
        switch (direction) {
        case IOffBoardDirections.NORTH:
            setFacing(3);
            break;
        case IOffBoardDirections.SOUTH:
            setFacing(0);
            break;
        case IOffBoardDirections.WEST:
            setFacing(2);
            break;
        case IOffBoardDirections.EAST:
            setFacing(4);
            break;
        }
        offBoardDistance = distance;
        offBoardDirection = direction;
    }

    /**
     * Get the distance in hexes from the board that the unit will be deployed.
     * If the unit is to be deployed onboard, the distance will be zero (0).
     *
     * @return the <code>int</code> distance from the board the unit will be
     *         deployed (in hexes); this value will never be negative.
     */
    public int getOffBoardDistance() {
        return offBoardDistance;
    }

    /**
     * Get the direction the board that the unit will be deployed. If the unit
     * is to be deployed onboard, the distance will be
     * <code>IOffBoardDirections.NONE</code>, otherwise it will be one of the
     * values:
     * <ul>
     * <li><code>IOffBoardDirections.NORTH</code></li>
     * <li><code>IOffBoardDirections.SOUTH</code></li>
     * <li><code>IOffBoardDirections.EAST</code></li>
     * <li><code>IOffBoardDirections.WEST</code></li>
     * </ul>
     *
     * @return the <code>int</code> direction from the board the unit will be
     *         deployed. Only valid values will be returned.
     */
    public int getOffBoardDirection() {
        return offBoardDirection;
    }

    /**
     * Deploy this offboard entity at the previously specified distance and
     * direction. This should only be invoked by the <code>Server</code> after
     * the board has been selected and all the players are ready to start. The
     * side effects of this methods set the unit's position and facing as
     * appropriate (as well as deploying the unit).
     * <p/>
     * Onboard units (units with an offboard distance of zero and a direction of
     * <code>Entity.NONE</code>) will be unaffected by this method.
     */
    public void deployOffBoard() {
        if (null == game) {
            throw new IllegalStateException("game not set; possible serialization error");
        }
        // N.B. 17 / 2 = 8, but the middle of 1..17 is 9, so we
        // add a bit (because 17 % 2 == 1 and 16 % 2 == 0).
        switch (offBoardDirection) {
        case IOffBoardDirections.NONE:
            break;
        case IOffBoardDirections.NORTH:
            setPosition(new Coords(game.getBoard().getWidth() / 2 + game.getBoard().getWidth() % 2, -getOffBoardDistance()));
            setFacing(3);
            setDeployed(true);
            break;
        case IOffBoardDirections.SOUTH:
            setPosition(new Coords(game.getBoard().getWidth() / 2 + game.getBoard().getWidth() % 2, game.getBoard().getHeight() + getOffBoardDistance()));
            setFacing(0);
            setDeployed(true);
            break;
        case IOffBoardDirections.EAST:
            setPosition(new Coords(game.getBoard().getWidth() + getOffBoardDistance(), game.getBoard().getHeight() / 2 + game.getBoard().getHeight() % 2));
            setFacing(5);
            setDeployed(true);
            break;
        case IOffBoardDirections.WEST:
            setPosition(new Coords(-getOffBoardDistance(), game.getBoard().getHeight() / 2 + game.getBoard().getHeight() % 2));
            setFacing(1);
            setDeployed(true);
            break;
        }
    }

    public Vector<Integer> getPickedUpMechWarriors() {
        return pickedUpMechWarriors;
    }

    /**
     * Has this entity been captured?
     *
     * @return <code>true</code> if it has.
     */
    public boolean isCaptured() {
        return captured && !isDestroyed();
    }

    /**
     * Specify that this entity has been captured.
     *
     * @param arg
     *            the <code>boolean</code> value to assign.
     */
    public void setCaptured(boolean arg) {
        captured = arg;
    }

    public void setSpotlight(boolean arg) {
        hasSpotlight = arg;
    }

    public boolean hasSpotlight() {
        for (Mounted m : getMisc()) {
            if (m.getType().hasFlag(MiscType.F_SEARCHLIGHT) && !m.isInoperable()) {
                return true;
            }
        }
        return hasSpotlight;
    }

    public void setSpotlightState(boolean arg) {
        if (hasSpotlight) {
            spotlightIsActive = arg;
            if (arg) {
                illuminated = true;
            }
        }
    }

    public boolean isIlluminated() {
        return illuminated;
    }

    public void setIlluminated(boolean arg) {
        illuminated = arg;
    }

    public boolean isUsingSpotlight() {
        return hasSpotlight && spotlightIsActive;
    }

    public void setUsedSearchlight(boolean arg) {
        usedSearchlight = arg;
    }

    public boolean usedSearchlight() {
        return usedSearchlight;
    }

    /**
     * illuminate a hex and all units that are between us and the hex
     *
     * @param target
     *            the <code>HexTarget</code> to illuminate
     */
    public void illuminateTarget(HexTarget target) {
        if (hasSpotlight && spotlightIsActive && (target != null)) {
            illuminated = true;
            ArrayList<Coords> in = Coords.intervening(getPosition(), target.getPosition());
            for (Coords c : in) {
                for (Enumeration<Entity> e = game.getEntities(c); e.hasMoreElements();) {
                    Entity en = e.nextElement();
                    en.setIlluminated(true);
                }
            }
        }
    }

    /**
     * Is the Entity stuck in a swamp?
     */
    public boolean isStuck() {
        return stuckInSwamp;
    }

    /**
     * Set weather this Entity is stuck in a swamp or not
     *
     * @param arg
     *            the <code>boolean</code> value to assign
     */
    public void setStuck(boolean arg) {
        stuckInSwamp = arg;
    }

    /**
     * Is the Entity stuck in a swamp?
     */
    public boolean canUnstickByJumping() {
        return canUnstickByJumping;
    }

    /**
     * Set wether this Enity is stuck in a swamp or not
     *
     * @param arg
     *            the <code>boolean</code> value to assign
     */
    public void setCanUnstickByJumping(boolean arg) {
        canUnstickByJumping = arg;
    }

    /*
     * The following methods support the eventual refactoring into the Entity
     * class of a lot of the Server logic surrounding entity damage and death.
     * They are not currently called in Server anywhere, and so may as well not
     * exist.
     */

    public String destroy(String reason, boolean survivable, boolean canSalvage) {
        StringBuffer sb = new StringBuffer();

        int condition = IEntityRemovalConditions.REMOVE_SALVAGEABLE;
        if (!canSalvage) {
            setSalvage(canSalvage);
            condition = IEntityRemovalConditions.REMOVE_DEVASTATED;
        }

        if (isDoomed() || isDestroyed()) {
            return sb.toString();
        }

        // working under the assumption that entity was neither doomed or
        // destroyed before from here on out

        setDoomed(true);

        Enumeration<Integer> iter = getPickedUpMechWarriors().elements();
        while (iter.hasMoreElements()) {
            Integer mechWarriorId = iter.nextElement();
            Entity mw = game.getEntity(mechWarriorId.intValue());
            mw.setDestroyed(true);
            game.removeEntity(mw.getId(), condition);
            sb.append("\n*** ").append(mw.getDisplayName() + " died in the wreckage. ***\n");
        }
        return sb.toString();
    }

    /**
     * Add a targeting by a swarm volley from a specified entity
     *
     * @param entityId
     *            The <code>int</code> id of the shooting entity
     * @param weaponId
     *            The <code>int</code> id of the shooting lrm launcher
     */
    public void addTargetedBySwarm(int entityId, int weaponId) {
        hitBySwarmsEntity.addElement(new Integer(entityId));
        hitBySwarmsWeapon.addElement(new Integer(weaponId));
    }

    /**
     * Were we targeted by a certain swarm/swarm-i volley this turn?
     *
     * @param entityId
     *            The <code>int</code> id of the shooting entity we are checking
     * @param weaponId
     *            The <code>int</code> id of the launcher to check
     * @return a fitting <code>boolean</code> value
     */
    public boolean getTargetedBySwarm(int entityId, int weaponId) {
        for (int i = 0; i < hitBySwarmsEntity.size(); i++) {
            Integer entityIdToTest = hitBySwarmsEntity.elementAt(i);
            Integer weaponIdToTest = hitBySwarmsWeapon.elementAt(i);
            if ((entityId == entityIdToTest.intValue()) && (weaponId == weaponIdToTest.intValue())) {
                return true;
            }
        }
        return false;
    }

    public int getShortRangeModifier() {
        if (getTargSysType() == MiscType.T_TARGSYS_SHORTRANGE) {
            return -1;
        } else if (getTargSysType() == MiscType.T_TARGSYS_LONGRANGE) {
            return 1;
        }
        return 0;
    }

    public int getMediumRangeModifier() {
        return 2;
    }

    public int getLongRangeModifier() {
        if (getTargSysType() == MiscType.T_TARGSYS_SHORTRANGE) {
            return 5;
        } else if (getTargSysType() == MiscType.T_TARGSYS_LONGRANGE) {
            return 3;
        }
        return 4;
    }

    public int getExtremeRangeModifier() {
        return 6;
    }

    public int getTargSysType() {
        return targSys;
    }

    public void setTargSysType(int targSysType) {
        targSys = targSysType;
    }

    public void setArmorType(int armType) {
        armorType = armType;
    }

    public void setStructureType(int strucType) {
        structureType = strucType;
    }

    public void setArmorType(String armType) {
        setArmorType(EquipmentType.getArmorType(armType));
    }

    public void setStructureType(String strucType) {
        setStructureType(EquipmentType.getStructureType(strucType));
    }

    public int getArmorType() {
        return armorType;
    }

    public void setArmorTechLevel(int newTL) {
        armorTechLevel = newTL;
    }

    public int getArmorTechLevel() {
        return armorTechLevel;
    }

    public int getStructureType() {
        return structureType;
    }

    public void setWeaponDestroyed(Mounted which) {
        if (weaponList.contains(which)) {
            which.setDestroyed(true);
        }
    }

    public void setTaggedBy(int tagger) {
        taggedBy = tagger;
    }

    public int getTaggedBy() {
        return taggedBy;
    }

    public abstract double getCost();

    public int getWeaponsAndEquipmentCost() {
        int cost = 0;
        for (Mounted mounted : getEquipment()) {
            if (mounted.isWeaponGroup()) {
                continue;
            }
            int itemCost = (int) mounted.getType().getCost(this, mounted.isArmored());
            if (itemCost == EquipmentType.COST_VARIABLE) {
                itemCost = mounted.getType().resolveVariableCost(this, mounted.isArmored());
            }
            cost += itemCost;
        }
        return cost;
    }

    public boolean removePartialCoverHits(int location, int cover, int side) {
        if (cover > LosEffects.COVER_NONE) {
            switch (cover) {
            case LosEffects.COVER_LOWLEFT:
                if (location == Mech.LOC_LLEG) {
                    return true;
                }
                break;
            case LosEffects.COVER_LOWRIGHT:
                if (location == Mech.LOC_RLEG) {
                    return true;
                }
                break;
            case LosEffects.COVER_LEFT:
                if ((location == Mech.LOC_LLEG) || (location == Mech.LOC_LARM) || (location == Mech.LOC_LT)) {
                    return true;
                }
                break;
            case LosEffects.COVER_RIGHT:
                if ((location == Mech.LOC_RLEG) || (location == Mech.LOC_RARM) || (location == Mech.LOC_RT)) {
                    return true;
                }
                break;
            case LosEffects.COVER_HORIZONTAL:
                if ((location == Mech.LOC_LLEG) || (location == Mech.LOC_RLEG)) {
                    return true;
                }
                break;
            case LosEffects.COVER_UPPER:
                if ((location == Mech.LOC_LLEG) || (location == Mech.LOC_RLEG)) {
                    return false;
                }
                return true;
            case LosEffects.COVER_FULL:
                return true;
            case LosEffects.COVER_75LEFT:
                if ((location == Mech.LOC_RARM) || (location == Mech.LOC_RLEG)) {
                    return false;
                }
                return true;
            case LosEffects.COVER_75RIGHT:
                if ((location == Mech.LOC_LLEG) || (location == Mech.LOC_LARM)) {
                    return false;
                }
                return true;
            }
        }
        return false;

    }

    /**
     * returns true if this is a valid target for Arrow IV homing which was
     * aimed at hex (tc)
     */
    public boolean isOnSameSheet(Coords tc) {
        if (game.getOptions().booleanOption("a4homing_target_area")) {
            // unofficial rule which may be better with odd sized boards
            if (tc.distance(getPosition()) <= 8) {
                return true;
            }
            return false;
        }
        // using FASA map sheets
        if ((tc.x / 16 == getPosition().x / 16) && (tc.y / 17 == getPosition().y / 17)) {
            return true;
        }
        return false;
    }

    public abstract boolean doomedInVacuum();

    public abstract boolean doomedOnGround();

    public abstract boolean doomedInAtmosphere();

    public abstract boolean doomedInSpace();

    public double getArmorWeight() {
        // this roundabout method is actually necessary to avoid rounding
        // weirdness. Yeah, it's dumb.
        double armorPerTon = 16.0 * EquipmentType.getArmorPointMultiplier(armorType, armorTechLevel);
        if (armorType == EquipmentType.T_ARMOR_HARDENED) {
            armorPerTon = 8.0;
        }
        double points = getTotalOArmor();
        double armorWeight = points / armorPerTon;
        armorWeight = Math.ceil(armorWeight * 2.0) / 2.0;
        return armorWeight;
    }

    public boolean hasTAG() {
        for (Mounted m : getWeaponList()) {
            WeaponType equip = (WeaponType) (m.getType());
            if ((equip != null) && (equip.hasFlag(WeaponType.F_TAG))) {
                return true;
            }
        }
        return false;
    }

    public boolean isCanon() {
        return canon;
    }

    public void setCanon(boolean canon) {
        this.canon = canon;
    }

    public boolean climbMode() {
        return climbMode;
    }

    public void setClimbMode(boolean state) {
        climbMode = state;
    }

    public boolean usedTag() {
        for (Mounted weapon : getWeaponList()) {
            WeaponType wtype = (WeaponType) weapon.getType();
            if (weapon.isUsedThisRound() && wtype.hasFlag(WeaponType.F_TAG)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasEiCockpit() {
        return ((game != null) && game.getOptions().booleanOption("all_have_ei_cockpit"));
    }

    public boolean hasActiveEiCockpit() {
        return (hasEiCockpit() && getCrew().getOptions().booleanOption("ei_implant"));
    }

    public boolean isLayingMines() {
        return layingMines;
    }

    public void setLayingMines(boolean laying) {
        layingMines = laying;
    }

    public boolean canLayMine() {
        for (Object oMount : miscList) {
            Mounted mount = (Mounted) oMount;
            EquipmentType type = mount.getType();
            if (!mount.isMissing() && type.hasFlag(MiscType.F_MINE) && !isLayingMines()) {
                return true;
            }
        }
        return false;
    }

    public int sideTable(Coords src) {
        return sideTable(src, false);
    }

    public int sideTable(Coords src, boolean usePrior) {
        return sideTable(src, usePrior, facing);
    }

    public int sideTable(Coords src, boolean usePrior, int face) {
        Coords effectivePos = position;
        if (usePrior) {
            effectivePos = getPriorPosition();
        }

        if (src.equals(effectivePos)) {
            // most places handle 0 range explicitly,
            // this is a safe default (calculation gives SIDE_RIGHT)
            return ToHitData.SIDE_FRONT;
        }

        // calculate firing angle
        int fa = (effectivePos.degree(src) + (6 - face) * 60) % 360;

        int leftBetter = 2;
        // if we're right on the line, we need to special case this
        // defender would choose along which hex the LOS gets drawn, and that
        // side also determines the side we hit in
        if (fa % 30 == 0) {
            IHex srcHex = game.getBoard().getHex(src);
            IHex curHex = game.getBoard().getHex(getPosition());
            if ((srcHex != null) && (curHex != null)) {
                LosEffects.AttackInfo ai = LosEffects.buildAttackInfo(src, getPosition(), 1, getElevation(), srcHex.floor(), curHex.floor());
                ArrayList<Coords> in = Coords.intervening(ai.attackPos, ai.targetPos, true);
                leftBetter = LosEffects.dividedLeftBetter(in, game, ai, Compute.isInBuilding(game, this), new LosEffects());
            }
        }

        boolean targetIsTank = (this instanceof Tank) || (game.getOptions().booleanOption("tacops_advanced_mech_hit_locations") && (this instanceof QuadMech));
        if (targetIsTank) {
            if ((leftBetter == 1) && (fa == 150)) {
                return ToHitData.SIDE_REAR;
            } else if ((leftBetter == 1) && (fa == 30)) {
                return ToHitData.SIDE_RIGHT;
            } else if ((leftBetter == 0) && (fa == 330)) {
                return ToHitData.SIDE_LEFT;
            } else if ((leftBetter == 0) && (fa == 210)) {
                return ToHitData.SIDE_REAR;
            } else if ((fa > 30) && (fa <= 150)) {
                return ToHitData.SIDE_RIGHT;
            } else if ((fa > 150) && (fa < 210)) {
                return ToHitData.SIDE_REAR;
            } else if ((fa >= 210) && (fa < 330)) {
                return ToHitData.SIDE_LEFT;
            } else {
                return ToHitData.SIDE_FRONT;
            }
        }
        if (this instanceof Aero) {
            Aero a = (Aero) this;
            // Handle spheroids in atmosphere differently
            // TODO: awaiting a rules forum clarification
            // until then assume that the side must either be left or right
            if (a.isSpheroid() && game.getBoard().inAtmosphere()) {
                fa = effectivePos.degree(src);
                if ((fa >= 0) && (fa < 180)) {
                    return ToHitData.SIDE_RIGHT;
                }
                return ToHitData.SIDE_LEFT;
            }
            if ((leftBetter == 1) && (fa == 150)) {
                return ToHitData.SIDE_REAR;
            } else if ((leftBetter == 1) && (fa == 30)) {
                if (a.isRolled()) {
                    return ToHitData.SIDE_LEFT;
                }
                return ToHitData.SIDE_RIGHT;
            } else if ((leftBetter == 0) && (fa == 330)) {
                if (a.isRolled()) {
                    return ToHitData.SIDE_RIGHT;
                }
                return ToHitData.SIDE_LEFT;
            } else if ((leftBetter == 0) && (fa == 210)) {
                return ToHitData.SIDE_REAR;
            } else if ((fa > 30) && (fa <= 150)) {
                if (a.isRolled()) {
                    return ToHitData.SIDE_LEFT;
                }
                return ToHitData.SIDE_RIGHT;
            } else if ((fa > 150) && (fa < 210)) {
                return ToHitData.SIDE_REAR;
            } else if ((fa >= 210) && (fa < 330)) {
                if (a.isRolled()) {
                    return ToHitData.SIDE_RIGHT;
                }
                return ToHitData.SIDE_LEFT;
            } else {
                return ToHitData.SIDE_FRONT;
            }
        }
        if ((fa == 90) && (leftBetter == 1)) {
            return ToHitData.SIDE_RIGHT;
        } else if (((fa == 150) && (leftBetter == 1)) || ((leftBetter == 0) && (fa == 210))) {
            return ToHitData.SIDE_REAR;
        } else if ((leftBetter == 0) && (fa == 270)) {
            return ToHitData.SIDE_LEFT;
        } else if ((fa > 90) && (fa <= 150)) {
            return ToHitData.SIDE_RIGHT;
        } else if ((fa > 150) && (fa < 210)) {
            return ToHitData.SIDE_REAR;
        } else if ((fa >= 210) && (fa < 270)) {
            return ToHitData.SIDE_LEFT;
        } else {
            return ToHitData.SIDE_FRONT;
        }
    }

    public boolean canGoHullDown() {
        return false;
    }

    public boolean canAssaultDrop() {
        return false;
    }

    public void setAssaultDropInProgress(boolean flag) {
        assaultDropInProgress = flag ? 1 : 0;
    }

    public void setLandedAssaultDrop() {
        assaultDropInProgress = 2;
        moved = IEntityMovementType.MOVE_JUMP;
    }

    public boolean isAssaultDropInProgress() {
        return assaultDropInProgress != 0;
    }

    /**
     * Apply PSR modifier for difficult terrain at the specified coordinates
     *
     * @param roll
     *            the PSR to modify
     * @param c
     *            the coordinates where the PSR happens
     */
    public void addPilotingModifierForTerrain(PilotingRollData roll, Coords c) {
        if ((c == null) || (roll == null)) {
            return;
        }
        if (isOffBoard() || !(isDeployed())) {
            return;
        }
        IHex hex = game.getBoard().getHex(c);
        int modifier = hex.terrainPilotingModifier(getMovementMode());
        if (modifier != 0) {
            roll.addModifier(modifier, "difficult terrain");
        }
    }

    /**
     * Apply PSR modifier for difficult terrain at the move step position
     *
     * @param roll
     *            the PSR to modify
     * @param step
     *            the move step the PSR occurs at
     */
    public void addPilotingModifierForTerrain(PilotingRollData roll, MoveStep step) {
        if (step.getElevation() > 0) {
            return;
        }
        addPilotingModifierForTerrain(roll, step.getPosition());
    }

    /**
     * Apply PSR modifier for difficult terrain in the current position
     *
     * @param roll
     *            the PSR to modify
     */
    public void addPilotingModifierForTerrain(PilotingRollData roll) {
        if (getElevation() > 0) {
            return;
        }
        addPilotingModifierForTerrain(roll, getPosition());
    }

    /**
     * defensively check and correct elevation
     */
    public boolean fixElevation() {
        if (!isDeployed() || isOffBoard() || !game.getBoard().contains(getPosition())) {
            return false;
        }
        if (!isElevationValid(getElevation(), game.getBoard().getHex(getPosition()))) {
            System.err.println(getDisplayName() + " in hex " + HexTarget.coordsToId(getPosition()) + " is at invalid elevation: " + getElevation());
            setElevation(elevationOccupied(game.getBoard().getHex(getPosition())));
            System.err.println("   moved to elevation " + getElevation());
            return true;
        }
        return false;
    }

    public Engine getEngine() {
        return engine;
    }

    public boolean itemOppositeTech(String s) {
        if (isClan()) { // Clan base
            if ((s.toLowerCase().indexOf("(IS)") != -1) || (s.toLowerCase().indexOf("Inner Sphere") != -1)) {
                return true;
            }
            return false;
        }
        if ((s.toLowerCase().indexOf("(C)") != -1) || (s.toLowerCase().indexOf("Clan") != -1)) {
            return true;
        }
        return false;
    }

    /**
     * @return Returns the retreatedDirection.
     */
    public int getRetreatedDirection() {
        return retreatedDirection;
    }

    /**
     * @param retreatedDirection
     *            The retreatedDirection to set.
     */
    public void setRetreatedDirection(int retreatedDirection) {
        this.retreatedDirection = retreatedDirection;
    }

    public void setLastTarget(int id) {
        lastTarget = id;
    }

    public int getLastTarget() {
        return lastTarget;
    }

    /**
     * @returns whether or not the unit is suffering from Electromagnetic
     *          Interference
     */
    public boolean isSufferingEMI() {
        return _isEMId;
    }

    public void setEMI(boolean inVal) {
        _isEMId = inVal;
    }

    /**
     * Checks if the unit is hardened agaist nuclear strikes.
     *
     * @return true if this is a hardened unit.
     */
    public abstract boolean isNuclearHardened();

    public void setHidden(boolean inVal) {
        isHidden = inVal;
    }

    public boolean isHidden() {
        return isHidden;
    }

    /**
     * Is this unit a carcass, a carcass can take no action
     */
    public boolean isCarcass() {
        return carcass;
    }

    /**
     * Sets if this unit is a carcass.
     *
     * @param carcass
     *            true if this unit should be a carcass, false otherwise.
     * @see megamek.common.Entity#isCarcass
     */
    public void setCarcass(boolean carcass) {
        this.carcass = carcass;
    }

    /**
     * Marks all equipment in a location on this entity as destroyed.
     *
     * @param loc
     *            The location that is destroyed.
     */
    public void destroyLocation(int loc) {
        // if it's already marked as destroyed, don't bother
        if (getInternal(loc) < 0) {
            return;
        }
        // mark armor, internal as doomed
        setArmor(IArmorState.ARMOR_DOOMED, loc, false);
        setInternal(IArmorState.ARMOR_DOOMED, loc);
        if (hasRearArmor(loc)) {
            setArmor(IArmorState.ARMOR_DOOMED, loc, true);
        }
        // equipment marked missing
        for (Mounted mounted : getEquipment()) {
            if (((mounted.getLocation() == loc) && mounted.getType().isHittable()) || (mounted.isSplit() && (mounted.getSecondLocation() == loc))) {
                mounted.setMissing(true);
            }
        }
        // all critical slots set as missing
        for (int i = 0; i < getNumberOfCriticals(loc); i++) {
            final CriticalSlot cs = getCritical(loc, i);
            if (cs != null) {
                // count engine hits for maxtech engine explosions
                if ((cs.getType() == CriticalSlot.TYPE_SYSTEM) && (cs.getIndex() == Mech.SYSTEM_ENGINE) && !cs.isDamaged()) {
                    engineHitsThisRound++;
                }
                cs.setMissing(true);
            }
        }
        // dependent locations destroyed, unless they are already destroyed
        if ((getDependentLocation(loc) != Entity.LOC_NONE) && !(getInternal(getDependentLocation(loc)) < 0)) {
            destroyLocation(getDependentLocation(loc));
        }
        // remove any narc/inarc pods in this location
        for (Iterator<INarcPod> i = pendingINarcPods.iterator(); i.hasNext();) {
            if (i.next().getLocation() == loc) {
                i.remove();
            }
        }
        for (Iterator<INarcPod> i = iNarcPods.iterator(); i.hasNext();) {
            if (i.next().getLocation() == loc) {
                i.remove();
            }
        }
        for (Iterator<NarcPod> i = pendingNarcPods.iterator(); i.hasNext();) {
            if (i.next().getLocation() == loc) {
                i.remove();
            }
        }
        for (Iterator<NarcPod> i = narcPods.iterator(); i.hasNext();) {
            if (i.next().getLocation() == loc) {
                i.remove();
            }
        }
    }

    public PilotingRollData checkSideSlip(int moveType, IHex prevHex, int overallMoveType, MoveStep prevStep, int prevFacing, int curFacing, Coords lastPos, Coords curPos, int distance) {
        PilotingRollData roll = getBasePilotingRoll(overallMoveType);

        if ((moveType != IEntityMovementType.MOVE_JUMP) && (prevHex != null) && (distance > 1) && ((overallMoveType == IEntityMovementType.MOVE_RUN) || (overallMoveType == IEntityMovementType.MOVE_VTOL_RUN)) && (prevFacing != curFacing) && !lastPos.equals(curPos) && !(this instanceof Infantry)) {
            roll.append(new PilotingRollData(getId(), 0, "flanking and turning"));

        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE, "Check false: not apparently sideslipping");
        }

        return roll;
    }

    /**
     * returns true if the entity is flying.
     */
    public boolean isFlying() {
        // stuff that moves like a VTOL is flying unless at elevation 0 or on
        // top of/in a building,
        if (getMovementMode() == IEntityMovementMode.VTOL) {
            if ((game.getBoard().getHex(getPosition()).terrainLevel(Terrains.BLDG_ELEV) >= getElevation()) || (game.getBoard().getHex(getPosition()).terrainLevel(Terrains.BRIDGE_ELEV) >= getElevation())) {
                return false;
            }
            return getElevation() > 0;
        }
        return false;
    }

    public void setSpotTargetId(int targetId) {
        spotTargetId = targetId;
    }

    public int getSpotTargetId() {
        return spotTargetId;
    }

    public void setCommander(boolean arg) {
        isCommander = arg;
    }

    public boolean isCommander() {
        return isCommander;
    }

    public boolean hasLinkedMGA(Mounted mounted) {
        for (Mounted m : getWeaponList()) {
            if ((m.getLocation() == mounted.getLocation()) && m.getType().hasFlag(WeaponType.F_MGA) && !(m.isDestroyed() || m.isBreached()) && m.getType().hasModes() && m.curMode().equals("Linked") && (((WeaponType) m.getType()).getDamage() == ((WeaponType) mounted.getType()).getDamage())) {
                return true;
            }
        }
        return false;
    }

    public void setReckless(boolean b) {
        reckless = b;
    }

    public boolean isReckless() {
        return reckless;
    }

    public boolean isFighter() {
        return (this instanceof Aero) && !((this instanceof SmallCraft) || (this instanceof Jumpship) || (this instanceof FighterSquadron));
    }

    public boolean isCapitalFighter() {
        if (null == game) {
            return false;
        }
        return game.getOptions().booleanOption("stratops_capital_fighter") && isFighter();
    }

    /**
     * a function that let's us know if this entity has capital-scale armor
     *
     * @return
     */
    public boolean isCapitalScale() {

        if ((this instanceof Jumpship) || (this instanceof FighterSquadron) || isCapitalFighter()) {
            return true;
        }

        return false;

    }

    /**
     * a function that let's us know if this entity is using weapons bays
     *
     * @return
     */
    public boolean usesWeaponBays() {

        if ((this instanceof Jumpship) || (this instanceof Dropship)) {
            return true;
        }

        return false;

    }

    /**
     * return the bay of the current weapon
     *
     * @param bayID
     * @return
     */
    public Mounted whichBay(int bayID) {

        for (Mounted m : getWeaponBayList()) {
            for (int wId : m.getBayWeapons()) {
                // find the weapon and determine if it is there
                if (wId == bayID) {
                    return m;
                }
            }
        }
        return null;
    }

    /**
     * return the first bay of the right type in the right location with enough
     * damage to spare
     *
     * @param wtype
     * @param loc
     * @param rearMount
     * @return
     */
    public Mounted getFirstBay(WeaponType wtype, int loc, boolean rearMount) {

        int weapDamage = wtype.getRoundShortAV();
        if (wtype.isCapital()) {
            weapDamage *= 10;
        }

        for (Mounted m : getWeaponBayList()) {
            BayWeapon bay = (BayWeapon) m.getType();
            int damage = bay.getRoundShortAV() + weapDamage;
            if ((bay.getAtClass() == wtype.getAtClass()) && (m.getLocation() == loc) && (m.isRearMounted() == rearMount) && (damage <= 700)) {
                return m;
            }

        }
        return null;
    }

    public int getHeatInArc(int location, boolean rearMount) {

        int arcHeat = 0;

        for (Mounted mounted : getTotalWeaponList()) {
            // is the weapon usable?
            if (mounted.isDestroyed() || mounted.isJammed()) {
                continue;
            }

            if ((mounted.getLocation() == location) && (mounted.isRearMounted() == rearMount)) {
                arcHeat += mounted.getCurrentHeat();
            }
        }
        return arcHeat;
    }

    public int[] getVectors() {
        return vectors;
    }

    public void setVectors(int[] v) {
        if ((v == null) || (v.length != 6)) {
            return;
        }

        vectors = v;
    }

    public int getVector(int vectorFacing) {
        if (vectorFacing < 6) {
            return vectors[vectorFacing];
        }
        return 0;
    }

    public int getVelocity() {

        int total = 0;
        for (int dir = 0; dir < 6; dir++) {
            total += getVector(dir);
        }

        return total;
    }

    public int chooseSide(Coords attackPos, boolean usePrior) {
        // loop through directions and if we have a non-zero vector, then
        // compute
        // the targetsidetable. If we come to a higher vector, then replace. If
        // we come to an equal vector then take it if it is better
        int thrust = 0;
        int high = -1;
        int side = -1;
        for (int dir = 0; dir < 6; dir++) {
            thrust = getVector(dir);
            if (thrust == 0) {
                continue;
            }

            if (thrust > high) {
                high = thrust;
                side = sideTable(attackPos, usePrior, dir);
            }

            // what if they tie
            if (thrust == high) {
                int newside = sideTable(attackPos, usePrior, dir);
                // choose the best
                if ((newside == ToHitData.SIDE_LEFT) || (newside == ToHitData.SIDE_RIGHT)) {
                    newside = side;
                }
                // that should be the only case, because it can't shift you from
                // front
                // to aft or vice-versa
            }

        }
        return side;
    }

    /**
     * return the heading of the unit based on its active vectors if vectors are
     * tied then return two headings
     */
    public Vector<Integer> getHeading() {

        Vector<Integer> heading = new Vector<Integer>();
        int high = 0;
        int curDir = getFacing();
        for (int dir = 0; dir < 6; dir++) {
            int thrust = getVector(dir);
            if ((thrust >= high) && (thrust > 0)) {
                // if they were equal then add the last direction to the
                // vector before moving on
                if (thrust == high) {
                    heading.addElement(curDir);
                }
                high = getVector(dir);
                curDir = dir;
            }
        }
        heading.addElement(curDir);
        return heading;
    }

    public void setPassedThrough(Vector<Coords> pass) {
        passedThrough = pass;
    }

    public Vector<Coords> getPassedThrough() {
        return passedThrough;
    }

    public void addPassedThrough(Coords c) {
        passedThrough.add(c);
    }

    public void setRamming(boolean b) {
        ramming = b;
    }

    public boolean isRamming() {
        return ramming;
    }

    public void resetFiringArcs() {
        frontArcFired = new boolean[locations()];
        rearArcFired = new boolean[locations()];
        for (int i = 0; i < locations(); i++) {
            frontArcFired[i] = false;
            rearArcFired[i] = false;
        }
    }

    public boolean hasArcFired(int location, boolean rearMount) {
        if ((null == frontArcFired) || (null == rearArcFired)) {
            resetFiringArcs();
        }
        if ((location > locations()) || (location < 0)) {
            return false;
        }

        if (rearMount) {
            return rearArcFired[location];
        }
        return frontArcFired[location];
    }

    public void setArcFired(int location, boolean rearMount) {
        if ((null == frontArcFired) || (null == rearArcFired)) {
            resetFiringArcs();
        }
        if ((location > locations()) || (location < 0)) {
            return;
        }

        if (rearMount) {
            rearArcFired[location] = true;
        } else {
            frontArcFired[location] = true;
        }
    }

    /**
     * Force rapid fire mode to the highest level on RAC and UAC
     */
    public void setRapidFire() {
        for (Mounted m : getTotalWeaponList()) {
            WeaponType wtype = (WeaponType) m.getType();
            if (wtype.getAmmoType() == AmmoType.T_AC_ROTARY) {
                m.setMode("6-shot");
            } else if (wtype.getAmmoType() == AmmoType.T_AC_ULTRA) {
                m.setMode("Ultra");
            }
        }
    }

    /**
     * Set the retractable blade in the given location as extended Takes the
     * first piece of appropriate equipment
     */
    public void extendBlade(int loc) {
        for (Mounted m : getEquipment()) {
            if ((m.getLocation() == loc) && !m.isDestroyed() && !m.isBreached() && (m.getType() instanceof MiscType) && m.getType().hasFlag(MiscType.F_CLUB) && m.getType().hasSubType(MiscType.S_RETRACTABLE_BLADE)) {
                m.setMode("extended");
                return;
            }
        }
    }

    /**
     * destroys the first retractable blade critical slot found
     */
    /**
     * Checks whether a weapon has been fired from the specified location this
     * turn
     */
    public void destroyRetractableBlade(int loc) {
        // check critical slots
        for (int i = 0; i < this.getNumberOfCriticals(loc); i++) {
            CriticalSlot slot = getCritical(loc, i);
            // ignore empty & system slots
            if ((slot == null) || (slot.getType() != CriticalSlot.TYPE_EQUIPMENT)) {
                continue;
            }
            Mounted m = getEquipment(slot.getIndex());
            if ((m.getLocation() == loc) && !m.isDestroyed() && !m.isBreached() && (m.getType() instanceof MiscType) && m.getType().hasFlag(MiscType.F_CLUB) && m.getType().hasSubType(MiscType.S_RETRACTABLE_BLADE)) {
                slot.setDestroyed(true);
                m.setDestroyed(true);
                return;
            }
        }
    }

    public TeleMissileTracker getTMTracker() {
        return tmTracker;
    }

    public void setGrappled(int id, boolean attacker) {
        // TODO?
    }

    public boolean isGrappleAttacker() {
        return false;
    }

    public int getGrappled() {
        return Entity.NONE;
    }

    public void setGameOptions() {

        if (game == null) {
            return;
        }

        // if the small craft does not already have ECM, then give them a single
        // hex ECM so they can change the mode
        if ((this instanceof SmallCraft) && !hasActiveECM() && isMilitary()) {
            try {
                this.addEquipment(EquipmentType.get(BattleArmor.SINGLE_HEX_ECM), Aero.LOC_NOSE, false);
            } catch (LocationFullException ex) {
                // ignore
            }
        }

        for (Mounted mounted : getWeaponList()) {
            if ((mounted.getType() instanceof GaussWeapon) && game.getOptions().booleanOption("tacops_gauss_weapons")) {
                String[] modes = { "Powered Up", "Powered Down" };
                ((WeaponType) mounted.getType()).setModes(modes);
                ((WeaponType) mounted.getType()).setInstantModeSwitch(false);
            } else if ((mounted.getType() instanceof ACWeapon) && game.getOptions().booleanOption("tacops_rapid_ac")) {
                String[] modes = { "", "Rapid" };
                ((WeaponType) mounted.getType()).setModes(modes);
            } else if (mounted.getType() instanceof ISBombastLaser) {
                int damage = 12;
                ArrayList<String> modes = new ArrayList<String>();
                String[] stringArray = {};

                for (; damage >= 7; damage--) {
                    modes.add("Damage " + damage);
                }
                ((WeaponType) mounted.getType()).setModes(modes.toArray(stringArray));
            } else if (((WeaponType) mounted.getType()).isCapital() && (((WeaponType) mounted.getType()).getAtClass() != WeaponType.CLASS_CAPITAL_MISSILE) && (((WeaponType) mounted.getType()).getAtClass() != WeaponType.CLASS_AR10)) {
                ArrayList<String> modes = new ArrayList<String>();
                String[] stringArray = {};
                modes.add("");
                if (game.getOptions().booleanOption("stratops_bracket_fire")) {
                    modes.add("Bracket 80%");
                    modes.add("Bracket 60%");
                    modes.add("Bracket 40%");
                }
                if ((mounted.getType() instanceof CapitalLaserBayWeapon) && game.getOptions().booleanOption("stratops_aaa_laser")) {
                    modes.add("AAA");
                    ((WeaponType) mounted.getType()).addEndTurnMode("AAA");
                }
                if (modes.size() > 1) {
                    ((WeaponType) mounted.getType()).setModes(modes.toArray(stringArray));
                }
            }

        }

        for (Mounted misc : getMisc()) {
            if (misc.getType().hasFlag(MiscType.F_BAP) && (this instanceof Aero) && game.getOptions().booleanOption("stratops_ecm")) {
                ArrayList<String> modes = new ArrayList<String>();
                String[] stringArray = {};
                modes.add("Short");
                modes.add("Medium");
                ((MiscType) misc.getType()).setModes(modes.toArray(stringArray));
                ((MiscType) misc.getType()).setInstantModeSwitch(false);
            }
            if (misc.getType().hasFlag(MiscType.F_ECM)) {
                ArrayList<String> modes = new ArrayList<String>();
                modes.add("ECM");
                String[] stringArray = {};
                if (game.getOptions().booleanOption("tacops_eccm")) {
                    modes.add("ECCM");
                    if (misc.getType().hasFlag(MiscType.F_ANGEL_ECM)) {
                        modes.add("ECM & ECCM");
                    }
                } else if (game.getOptions().booleanOption("stratops_ecm") && (this instanceof Aero)) {
                    modes.add("ECCM");
                    if (misc.getType().hasFlag(MiscType.F_ANGEL_ECM)) {
                        modes.add("ECM & ECCM");
                    }
                }
                if (game.getOptions().booleanOption("tacops_ghost_target")) {
                    if (misc.getType().hasFlag(MiscType.F_ANGEL_ECM)) {
                        modes.add("ECM & Ghost Targets");
                        modes.add("ECCM & Ghost Targets");
                    } else {
                        modes.add("Ghost Targets");
                    }
                }
                ((MiscType) misc.getType()).setModes(modes.toArray(stringArray));
            }
        }

    }

    public void setGrappleSide(int side) {
        // TODO?
    }

    public int getGrappleSide() {
        return Entity.NONE;
    }

    public boolean hasFunctionalArmAES(int location) {
        return false;
    }

    public boolean hasFunctionalLegAES() {
        return false;
    }

    public boolean isEvading() {
        return evading;
    }

    public void setEvading(boolean evasion) {
        evading = evasion;
    }

    public int getEvasionBonus() {
        if (isProne()) {
            return 0;
        }

        if (this instanceof SmallCraft) {
            return 2;
        } else if (this instanceof Jumpship) {
            return 1;
        } else if (this instanceof Aero) {
            return 3;
        } else {
            if (game.getOptions().booleanOption("tacops_skilled_evasion")) {
                int piloting = crew.getPiloting();
                if (piloting < 2) {
                    return 3;
                } else if (piloting < 4) {
                    return 2;
                } else if (piloting < 6) {
                    return 1;
                }
            } else {
                return 1;
            }
        }
        return 0;
    }

    public void setCarefulStand(boolean stand) {
        isCarefulStanding = stand;
    }

    public boolean isCarefulStand() {
        return false;
    }

    public Vector<Sensor> getSensors() {
        return sensors;
    }

    public Sensor getActiveSensor() {
        return activeSensor;
    }

    public Sensor getNextSensor() {
        return nextSensor;
    }

    public void setNextSensor(Sensor s) {
        nextSensor = s;
    }

    public int getSensorCheck() {
        return sensorCheck;
    }

    public boolean hasModularArmor() {
        return hasModularArmor(0);
    }

    public boolean hasModularArmor(int loc) {
        return false;
    }

    public int getDamageReductionFromModularArmor(int loc, int damage, Vector<Report> vDesc) {

        if (!hasModularArmor(loc)) {
            return damage;
        }

        for (Mounted mount : this.getEquipment()) {
            if ((mount.getLocation() == loc) && !mount.isDestroyed() && (mount.getType() instanceof MiscType) && ((MiscType) mount.getType()).hasFlag(MiscType.F_MODULAR_ARMOR)) {

                int damageAbsorption = mount.getBaseDamageCapacity() - mount.getDamageTaken();
                if (damageAbsorption > damage) {
                    mount.damageTaken += damage;
                    Report r = new Report(3535);
                    r.subject = getId();
                    r.add(damage);
                    r.indent(1);
                    r.newlines = 0;
                    vDesc.addElement(r);
                    Report.addNewline(vDesc);

                    return 0;
                }

                if (damageAbsorption == damage) {
                    Report.addNewline(vDesc);
                    Report r = new Report(3535);
                    r.subject = getId();
                    r.add(damage);
                    r.indent(1);
                    r.newlines = 0;
                    vDesc.addElement(r);
                    r = new Report(3536);
                    r.subject = getId();
                    r.indent();
                    vDesc.addElement(r);

                    mount.damageTaken += damage;
                    mount.setDestroyed(true);
                    mount.setHit(true);
                    return 0;
                }

                if (damageAbsorption < damage) {
                    Report.addNewline(vDesc);
                    Report r = new Report(3535);
                    r.subject = getId();
                    r.add(damageAbsorption);
                    r.indent(1);
                    r.newlines = 0;
                    vDesc.addElement(r);
                    r = new Report(3536);
                    r.subject = getId();
                    r.indent(1);
                    vDesc.addElement(r);

                    damage -= mount.baseDamageAbsorptionRate - mount.damageTaken;
                    mount.damageTaken = mount.baseDamageAbsorptionRate;
                    mount.setDestroyed(true);
                    mount.setHit(true);
                }
            }

        }

        return damage;
    }

    public int getGhostTargetRoll() {
        return ghostTargetRoll;
    }

    public int getGhostTargetRollMoS() {
        return ghostTargetRoll - (getCrew().getSensorOps() + 2);
    }

    public int getGhostTargetOverride() {
        return ghostTargetOverride;
    }

    public int getCoolantFailureAmount() {
        return 0;
    }

    public void addCoolantFailureAmount(int amount) {
        // TODO?
    }

    /**
     * @return the tonnage of additional mounted communications equipment
     */
    public int getExtraCommGearTons() {
        int i = 0;
        for (Mounted mounted : miscList) {
            if (mounted.getType().hasFlag(MiscType.F_COMMUNICATIONS)) {
                i += mounted.getType().getTonnage(this);
            }
        }
        return i;
    }

    /**
     * @return the strength of the ECM field this unit emits
     */
    public double getECMStrength() {
        int strength = 0;
        for (Mounted m : getMisc()) {
            if (m.getType().hasFlag(MiscType.F_ECM) && (strength < 1)) {
                strength = 1;
            }
            if (m.getType().hasFlag(MiscType.F_ANGEL_ECM) && (strength < 2)) {
                strength = 2;
            }
        }
        return strength;
    }

    /**
     * @return the strength of the ECCM field this unit emits
     */
    public double getECCMStrength() {
        double strength = 0;
        for (Mounted m : getMisc()) {
            if (m.getType().hasFlag(MiscType.F_COMMUNICATIONS)) {
                if ((getTotalCommGearTons() > 3) && (strength < 0.5)) {
                    strength = 0.5;
                }
                if ((getTotalCommGearTons() > 6) && (strength < 1)) {
                    strength = 1;
                }
            }
            if (m.getType().hasFlag(MiscType.F_ECM) && (strength < 1)) {
                strength = 1;
            }
            if (m.getType().hasFlag(MiscType.F_ANGEL_ECM) && (strength < 2)) {
                strength = 2;
            }
        }
        return strength;
    }

    /**
     * @return the total tonnage of communications gear in this entity
     */
    public abstract int getTotalCommGearTons();

    /**
     * @return the initiative bonus this Entity grants for HQ
     */
    public int getHQIniBonus() {
        int bonus = 0;
        for (Mounted misc : getMisc()) {
            if (misc.getType().hasFlag(MiscType.F_COMMUNICATIONS) && misc.curMode().equals("Default")) {
                if (getTotalCommGearTons() >= 3) {
                    bonus += 1;
                }
                if (getTotalCommGearTons() >= 7) {
                    bonus += 1;
                }
                break;
            }
        }

        return bonus;
    }

    /**
     * @return the initiative bonus this Entity grants for MD implants
     */
    public int getMDIniBonus() {
        if (crew.getOptions().booleanOption("comm_implant") || crew.getOptions().booleanOption("boost_comm_implant")) {
            return 1;
        }
        return 0;
    }

    /**
     * Apply any pending Santa Anna allocations to Killer Whale ammo bins
     * effectively "splitting" the ammo bins in two This is a hack that I should
     * really creat a general method of "splitting" ammo bins for
     */
    public void applySantaAnna() {

        // I can't add the ammo while I am iterating through the ammo list
        // so collect vectors containing number of shots and location
        Vector<Integer> locations = new Vector<Integer>();
        Vector<Integer> ammo = new Vector<Integer>();
        Vector<Mounted> baymounts = new Vector<Mounted>();
        Vector<String> name = new Vector<String>();

        for (Mounted amounted : getAmmo()) {
            if (amounted.getNSantaAnna() > 0) {
                // first reduce the current ammo load by number of santa annas
                int nSantaAnna = amounted.getNSantaAnna();
                amounted.setShotsLeft(Math.max(amounted.getShotsLeft() - nSantaAnna, 0));
                // save the new ammo information
                locations.add(amounted.getLocation());
                ammo.add(nSantaAnna);
                // name depends on type
                if (amounted.getType().getInternalName().indexOf("AR10") != -1) {
                    name.add("AR10 SantaAnna Ammo");
                } else {
                    name.add("SantaAnna Ammo");
                }
                if (null == getBayByAmmo(amounted)) {
                    System.err.println("cannot find the right bay for Santa Anna ammo");
                    return;
                }
                baymounts.add(getBayByAmmo(amounted));
                // now make sure santa anna loadout is reset
                amounted.setNSantaAnna(0);
            }
        }

        // now iterate through to get new mounts
        if ((ammo.size() != locations.size()) || (ammo.size() != name.size()) || (ammo.size() != baymounts.size())) {
            // this shouldn't happen
            System.err.println("cannot load santa anna ammo");
            return;
        }
        for (int i = 0; i < ammo.size(); i++) {
            try {
                Mounted newmount = addEquipment(EquipmentType.get(name.elementAt(i)), locations.elementAt(i), false, ammo.elementAt(i));
                // add this mount to the ammo for the bay
                baymounts.elementAt(i).addAmmoToBay(getEquipmentNum(newmount));
            } catch (LocationFullException ex) {
                // throw new LocationFullException(ex.getMessage());
            }
        }
    }

    /**
     * get the bay that ammo is associated with
     *
     * @param mammo
     * @return
     */
    public Mounted getBayByAmmo(Mounted mammo) {

        for (Mounted m : getWeaponBayList()) {
            for (int bayAmmoId : m.getBayAmmo()) {
                Mounted bayammo = getEquipment(bayAmmoId);
                if (bayammo == mammo) {
                    return m;
                }
            }
        }
        return null;
    }

    /**
     * Return how many BA vibroclaws this <code>Entity</code> is equipped with
     */
    public int getVibroClaws() {
        // generic entities can't carry vibroclaws
        return 0;
    }

    /**
     * shut this unit down due to a BA Taser attack
     *
     * @param turns
     *            - the amount of rounds for which this Entity should be
     *            shutdown
     */
    public void baTaserShutdown(int turns) {
        setShutDown(true);
        taserShutdownRounds = turns;
        shutdownByBATaser = true;
    }

    /**
     * get the number of rounds for which this unit should be shutdown by taser
     *
     * @return
     */
    public int getTaserShutdownRounds() {
        return taserShutdownRounds;
    }

    public void setTaserShutdownRounds(int rounds) {
        taserShutdownRounds = rounds;
    }

    public boolean isBATaserShutdown() {
        return shutdownByBATaser;
    }

    public void setBATaserShutdown(boolean value) {
        shutdownByBATaser = value;
    }

    /**
     * set this entity to suffer from taser feedback
     *
     * @param rounds
     *            - the number of rounds to suffer from taserfeedback
     */
    public void setTaserFeedback(int rounds) {
        taserFeedBackRounds = rounds;
    }

    /**
     * get the rounds for which this entity suffers from taser feedback
     *
     * @return
     */
    public int getTaserFeedBackRounds() {
        return taserFeedBackRounds;
    }

    public void setTaserInterference(int value, int rounds) {
        taserInterference = value;
        taserInterferenceRounds = rounds;
    }

    public int getTaserInterference() {
        return taserInterference;
    }

    public int getTaserInterferenceRounds() {
        return taserInterferenceRounds;
    }

    /**
     * returns whether the unit is a military unit (as opposed to a civilian
     * unit). TODO: for now this just returns true, but at some point the
     * database should be updated
     */
    public boolean isMilitary() {
        return true;
    }

    /**
     * is this entity a large craft? (dropship, jumpship, warship, or space
     * station)
     */
    public boolean isLargeCraft() {
        return (this instanceof Dropship) || (this instanceof Jumpship);
    }

    /**
     * Do units loaded onto this entity still have active ECM/ECCM/etc.?
     */
    public boolean loadedUnitsHaveActiveECM() {
        return false;
    }

    /**
     * is this entity loaded into a fighter squadron?
     */
    public boolean isPartOfFighterSquadron() {
        if (game == null) {
            return false;
        }
        if (conveyance == Entity.NONE) {
            return false;
        }
        return game.getEntity(conveyance) instanceof FighterSquadron;
    }

    /**
     * Return a HTML string that describes the BV calculations
     *
     * @return a <code>String</code> explaining the BV calculation
     */
    public String getBVText() {
        if (bvText == null) {
            return "";
        }

        return bvText.toString();
    }

    /**
     * Return the BAR-rating of this Entity's armor
     *
     * @return the BAR rating
     */
    public int getBARRating() {
        // normal armor has a BAR rating of 10
        return 10;
    }

    /**
     * does this Entity have BAR armor?
     *
     * @return
     */
    public boolean hasBARArmor() {
        return getBARRating() < 10;
    }

    /**
     * does this entity have an armored chassis?
     *
     * @return
     */
    public boolean hasArmoredChassis() {
        // normal entities don't, subclasses should
        // override
        return false;
    }

    /**
     * does this <code>Entity</code> have Environmental sealing? (only Support
     * Vehicles or IndustrialMechs should mount this)
     *
     * @return
     */
    public boolean hasEnvironmentalSealing() {
        for (Mounted misc : miscList) {
            if (misc.getType().hasFlag(MiscType.F_ENVIRONMENTAL_SEALING)) {
                return true;
            }
        }
        return false;
    }

    /**
     * possibly do a ICE-Engine stall PSR, only industrialmechs will actually do
     * such a roll
     *
     * @param vPhaseReport
     *            the <code>Vector<Report></code> containing the phase reports
     * @return a Vector<Report> containing the passed in reports, and any
     *         additional ones
     */
    public Vector<Report> doCheckEngineStallRoll(Vector<Report> vPhaseReport) {
        return vPhaseReport;
    }

    /**
     * check for unstalling of this Entity's engine (only used for ICE
     * industrial Mechs)
     *
     * @param vPhaseReport
     *            the <code>Vector<Report></code> containing the phase reports
     */
    public void checkUnstall(Vector<Report> vPhaseReport) {
        return;
    }

    public boolean hasArmoredEngine() {
        return false;
    }

    /**
     * Is this Entity's ICE Engine stalled?
     *
     * @return if this Entity's ICE engine is stalled
     */
    public boolean isStalled() {
        return false;
    }

    /**
     * is this a naval vessel on the surface of the water?
     */
    public boolean isSurfaceNaval() {
        // TODO: assuming submarines on the surface act like surface naval
        // vessels until rules clarified
        // http://www.classicbattletech.com/forums/index.php/topic,48987.0.html
        return (getElevation() == 0) && ((getMovementMode() == IEntityMovementMode.NAVAL) || (getMovementMode() == IEntityMovementMode.HYDROFOIL) || (getMovementMode() == IEntityMovementMode.SUBMARINE));
    }

    /**
     * used to set the source of the creation of this entity, i.e RS PPU Custom
     * what not Fluff for MMLab
     *
     * @param source
     */
    public void setSource(String source) {
        if (source != null) {
            this.source = source;
        }
    }

    public String getSource() {
        if (source == null) {
            return "";
        }
        return source;
    }

}
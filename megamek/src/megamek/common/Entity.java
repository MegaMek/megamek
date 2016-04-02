/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur
 * (bmazur@sev.org)
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

import java.io.Serializable;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import megamek.client.ui.swing.GUIPreferences;
import megamek.common.Building.BasementType;
import megamek.common.IGame.Phase;
import megamek.common.MovePath.MoveStepType;
import megamek.common.actions.AbstractAttackAction;
import megamek.common.actions.ChargeAttackAction;
import megamek.common.actions.DfaAttackAction;
import megamek.common.actions.DisplacementAttackAction;
import megamek.common.actions.EntityAction;
import megamek.common.actions.PushAttackAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.event.GameEntityChangeEvent;
import megamek.common.options.GameOptions;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PartialRepairs;
import megamek.common.options.Quirks;
import megamek.common.preference.PreferenceManager;
import megamek.common.util.StringUtil;
import megamek.common.weapons.ACWeapon;
import megamek.common.weapons.ASEWMissileWeapon;
import megamek.common.weapons.ASMissileWeapon;
import megamek.common.weapons.ATMWeapon;
import megamek.common.weapons.AlamoMissileWeapon;
import megamek.common.weapons.AltitudeBombAttack;
import megamek.common.weapons.BayWeapon;
import megamek.common.weapons.BombArrowIV;
import megamek.common.weapons.BombISRL10;
import megamek.common.weapons.BombTAG;
import megamek.common.weapons.CLAAAMissileWeapon;
import megamek.common.weapons.CLLAAMissileWeapon;
import megamek.common.weapons.CapitalLaserBayWeapon;
import megamek.common.weapons.DiveBombAttack;
import megamek.common.weapons.GaussWeapon;
import megamek.common.weapons.ISAAAMissileWeapon;
import megamek.common.weapons.ISBombastLaser;
import megamek.common.weapons.ISLAAMissileWeapon;
import megamek.common.weapons.ISLAC5;
import megamek.common.weapons.ISSnubNosePPC;
import megamek.common.weapons.MMLWeapon;
import megamek.common.weapons.SCLBayWeapon;
import megamek.common.weapons.SpaceBombAttack;
import megamek.common.weapons.TSEMPWeapon;
import megamek.common.weapons.VariableSpeedPulseLaserWeapon;
import megamek.common.weapons.WeaponHandler;
import megamek.common.weapons.battlearmor.ISBAPopUpMineLauncher;

/**
 * Entity is a master class for basically anything on the board except terrain.
 */
public abstract class Entity extends TurnOrdered implements Transporter,
        Targetable, RoundUpdated, PhaseUpdated {
    /**
     *
     */
    private static final long serialVersionUID = 1430806396279853295L;

    public static final int DOES_NOT_TRACK_HEAT = 999;

    /**
     * Entity Type Id Definitions These are used to identify the type of Entity,
     * such as 'mech or aero.
     */
    public static final long ETYPE_MECH = 1L;
    public static final long ETYPE_BIPED_MECH = 1L << 1;
    public static final long ETYPE_LAND_AIR_MECH = 1L << 2;
    public static final long ETYPE_QUAD_MECH = 1L << 3;
    public static final long ETYPE_ARMLESS_MECH = 1L << 4;

    public static final long ETYPE_AERO = 1L << 5;

    public static final long ETYPE_JUMPSHIP = 1L << 6;
    public static final long ETYPE_WARSHIP = 1L << 7;
    public static final long ETYPE_SPACE_STATION = 1L << 8;

    public static final long ETYPE_CONV_FIGHTER = 1L << 9;
    public static final long ETYPE_FIXED_WING_SUPPORT = 1L << 10;

    public static final long ETYPE_FIGHTER_SQUADRON = 1L << 11;

    public static final long ETYPE_SMALL_CRAFT = 1L << 12;
    public static final long ETYPE_DROPSHIP = 1L << 13;

    public static final long ETYPE_TELEMISSILE = 1L << 14;

    public static final long ETYPE_INFANTRY = 1L << 15;
    public static final long ETYPE_BATTLEARMOR = 1L << 16;
    public static final long ETYPE_MECHWARRIOR = 1L << 17;

    public static final long ETYPE_PROTOMECH = 1L << 18;

    public static final long ETYPE_TANK = 1L << 19;

    public static final long ETYPE_GUN_EMPLACEMENT = 1L << 20;

    public static final long ETYPE_SUPER_HEAVY_TANK = 1L << 21;

    public static final long ETYPE_SUPPORT_TANK = 1L << 22;
    public static final long ETYPE_LARGE_SUPPORT_TANK = 1L << 23;

    public static final long ETYPE_VTOL = 1L << 24;
    public static final long ETYPE_SUPPORT_VTOL = 1L << 25;

    public static final long ETYPE_TRIPOD_MECH = 1L << 26;

    public static final int NONE = -1;

    public static final int LOC_NONE = -1;
    public static final int LOC_DESTROYED = -2;

    public static final int MAX_C3_NODES = 12;
    public static final int MAX_C3i_NODES = 6;

    public static final int GRAPPLE_BOTH = 0;
    public static final int GRAPPLE_RIGHT = 1;
    public static final int GRAPPLE_LEFT = 2;

    public static final int DMG_NONE = 0;
    public static final int DMG_LIGHT = 1;
    public static final int DMG_MODERATE = 2;
    public static final int DMG_HEAVY = 3;
    public static final int DMG_CRIPPLED = 4;
    
    public static final int USE_STRUCTURAL_RATING = -1;

    // Weapon sort order defines
    public static enum WeaponSortOrder {
        DEFAULT("DEFAULT"),
        RANGE_LH("RANGE_LH"),
        RANGE_HL("RANGE_HL"),
        DAMAGE_LH("DAMAGE_LH"),
        DAMAGE_HL("DAMAGE_HL"),
        CUSTOM("CUSTOM");

        public final String i18nEntry;

        WeaponSortOrder(String s) {
            i18nEntry = s;
        }
    }

    protected transient IGame game;

    protected int id = Entity.NONE;

    protected String camoCategory = IPlayer.NO_CAMO;
    protected String camoFileName = null;

    /**
     * ID settable by external sources (such as mm.net)
     */
    protected String externalId = "-1";

    protected float weight;
    protected boolean omni = false;
    protected String chassis;
    protected String model;
    protected int year = 3071;
    protected int techLevel;
    /**
     * Used by support vehicles to define the structural tech rating 
     * (TM pg 117).  The values should come from EquipmentType.RATING_A-X.
     */
    protected int structuralTechRating =  EquipmentType.RATING_A;
    /**
     * Used by support vehicles to define tech rating of armor.  Default value
     * indicates that structural tech rating should be used, as in most cases
     * the armor and structural tech ratings match.
     */
    protected int armorTechRating = USE_STRUCTURAL_RATING;
    /**
     * Used by support vehicles to define tech rating of armor.  Default value
     * indicates that structural tech rating should be used, as in most cases
     * the engine and structural tech ratings match.
     */
    protected int engineTechRating = USE_STRUCTURAL_RATING;
    protected Engine engine;
    protected boolean mixedTech = false;
    protected boolean designValid = true;
    protected boolean useManualBV = false;
    protected int manualBV = -1;

    protected String displayName = null;
    protected String shortName = null;
    public int duplicateMarker = 1;

    protected transient IPlayer owner;
    protected int ownerId;
    protected int traitorId = -1;

    protected int targetBay = -1;

    private int startingPos = Board.START_NONE;

    /**
     * The pilot of the entity. Even infantry has a 'pilot'.
     */
    private Crew crew = new Crew(1);

    private Quirks quirks = new Quirks();
    private PartialRepairs partReps = new PartialRepairs();

    // Variable for manually shutdown mechs.
    protected boolean manualShutdown = false;
    protected boolean startupThisPhase = false;

    protected boolean shutDown = false;
    protected boolean shutDownThisPhase = false;
    protected boolean doomed = false;
    protected boolean destroyed = false;

    private Coords position = null;

    /**
     * Used for Entities that are bigger than a single hex. This contains the
     * central hex plus all of the other hexes this entity occupies. The central
     * hex is important for drawing multi-hex sprites.
     */
    protected Map<Integer, Coords> secondaryPositions = null;

    protected int facing = 0;
    protected int sec_facing = 0;

    protected int walkMP = 0;
    protected int jumpMP = 0;

    protected boolean done = false;

    protected boolean prone = false;
    protected boolean hullDown = false;
    protected boolean findingClub = false;
    protected boolean armsFlipped = false;
    protected boolean unjammingRAC = false;
    protected boolean selfDestructing = false;
    protected boolean selfDestructInitiated = false;
    /**
     * Variable to store the state of a possible externally mounted searchlight.
     * True if an operable searchlight is externally mounted, false if one isn't
     * mounted or if it is destroyed. Other searchlights may be mounted as
     * equipment on the entity.
     */
    protected boolean hasExternalSpotlight = false;
    protected boolean illuminated = false;
    protected boolean spotlightIsActive = false;
    protected boolean usedSearchlight = false;
    protected boolean stuckInSwamp = false;
    protected boolean canUnstickByJumping = false;
    protected int taggedBy = -1;
    protected boolean layingMines = false;
    protected boolean _isEMId = false;
    protected boolean[] hardenedArmorDamaged;
    protected boolean[] locationBlownOff;
    protected boolean[] locationBlownOffThisPhase;
    protected int[] armorType;
    protected int[] armorTechLevel;
    protected boolean isJumpingNow = false;

    protected DisplacementAttackAction displacementAttack = null;

    public int heat = 0;
    public int heatBuildup = 0;
    public int heatFromExternal = 0;
    public int coolFromExternal = 0;
    public int delta_distance = 0;
    public int mpUsed = 0;
    public EntityMovementType moved = EntityMovementType.MOVE_NONE;
    private boolean movedBackwards = false;
    /**
     * Used to keep track of usage of the power reverse quirk, which allows a
     * combat vehicle to use flank MP in reverse.  If power reverse is used and
     * a PSR is required, it adds a +1 modifier to the PSR.
     */
    private boolean isPowerReverse = false;
    protected int mpUsedLastRound = 0;
    public boolean gotPavementBonus = false;
    public boolean hitThisRoundByAntiTSM = false;
    public boolean inReverse = false;
    protected boolean struck = false;
    protected boolean fell = false;

    private int[] exposure;
    private int[] armor;
    private int[] internal;
    private int[] orig_armor;
    private int[] orig_internal;
    public int damageThisPhase;
    public int damageThisRound;
    public int engineHitsThisPhase;
    public boolean rolledForEngineExplosion = false; // So that we don't roll
    // twice in one round
    public boolean dodging;
    public boolean reckless;
    private boolean evading = false;

    public boolean spotting;
    private boolean clearingMinefield = false;
    protected int killerId = Entity.NONE;
    private int offBoardDistance = 0;
    private OffBoardDirection offBoardDirection = OffBoardDirection.NONE;
    private OffBoardDirection retreatedDirection = OffBoardDirection.NONE;

    protected int[] vectors = {0, 0, 0, 0, 0, 0};
    private int recoveryTurn = 0;
    // need to keep a list of areas that this entity has passed through on the
    // current turn
    private Vector<Coords> passedThrough = new Vector<Coords>();
    private List<Integer> passedThroughFacing = new ArrayList<>();
    /**
     * Stores the player selected hex ground to air targeting.
     * For ground to air, distance to target for the ground unit is determined
     * by the closest hex in the flight path of the airborne unit.  It's
     * possible that there are multiple equidistance hexes in the flight path
     * and in some cases, one of those hexes will be better than the other (ie,
     * one could be side arc and one rear).  By default, MM picks the first hex,
     * but the user should be able to distinguish between multiple equi-distant
     * hexes.
     */
    private Map<Integer, Coords> playerPickedPassThrough = new HashMap<>();
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
    private String c3UUID = null;
    private String c3MasterIsUUID = null;
    private String[] c3iUUIDs = new String[MAX_C3i_NODES];

    protected int structureType = EquipmentType.T_STRUCTURE_UNKNOWN;
    protected int structureTechLevel = TechConstants.T_TECH_UNKNOWN;

    protected String source = "";

    /**
     * Keeps track of whether this Entity was hit by a TSEMP this turn.
     */
    private int tsempHitsThisTurn = 0;

    /**
     * Keeps track of the current TSEMP effect on this entity
     */
    private int tsempEffect = TSEMPWeapon.TSEMP_EFFECT_NONE;

    /**
     * Keeps track of whether this Entity fired a TSEMP this turn
     */
    private boolean firedTsempThisTurn = false;

    /**
     * Keeps track of whether this Entity has ever fired a TSEMP.  This is used
     * to avoid having to iterate over all weapons looking for TSEMPs to reset
     * at the start of every round.
     */
    private boolean hasFiredTsemp = false;

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
    protected EntityMovementMode movementMode = EntityMovementMode.NONE;

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
     * Set to <code>true</code> if this unit was loaded this turn.
     */
    private boolean loadedThisTurn = false;

    /**
     * Need to keep a vector of entity IDs loaded in the chat lounge
     */
    private Vector<Integer> loadedKeepers = new Vector<Integer>();

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
     * Tracks if this entity was never deployed
     */
    private boolean neverDeployed = true;

    /**
     * The unit number of this entity. All entities which are members of the
     * same low-level unit are expected to share the same unit number. Future
     * implementations may store multiple unit designations in the same unit
     * number (e.g. battalion, company, platoon, and lance).
     */
    private short unitNumber = Entity.NONE;

    /**
     * Indicates whether this entity has been seen by the enemy during the
     * course of this game. Used in double-blind.
     */
    private boolean everSeenByEnemy = false;

    /**
     * Indicates whether this entity can currently be seen by the enemy. Used in
     * double-blind.
     */
    private boolean visibleToEnemy = false;

    /**
     * Flag that indicates whether this entity has been detected by sensors by
     * an enemy.
     */
    private boolean detectedByEnemy;

    /**
     * Check to see who has seen this Entity Used for Double Blind Reports.
     */
    private Vector<IPlayer> entitySeenBy = new Vector<IPlayer>();
    
    /**
     * Check to see what players have detected this entity with sensors, for
     * double blind play.
     */
    private Vector<IPlayer> entityDetectedBy = new Vector<IPlayer>();

    /**
     * Whether this entity is captured or not.
     */
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
     * altitude is different from elevation. It is used to measure the vertical
     * distance of Aero units from the ground on low atmosphere and ground maps.
     */
    protected int altitude = 0;

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
    protected String lastTargetDisplayName = "";

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
    protected boolean taserInterferenceHeat = false;

    // contains a HTML string describing BV calculation
    protected StringBuffer bvText = null;
    protected String startTable = "<TABLE>";
    protected String endTable = "</TABLE>";

    protected String startRow = "<TR>";
    protected String endRow = "</TR>";

    protected String startColumn = "<TD>";
    protected String endColumn = "</TD>";

    protected String nl = "<BR>";

    // Max range modifer is 6
    protected double[] battleForceMinRangeModifier = new double[]{1, .92,
                                                                  .83, .75, .66, .58, .50};
    // When getting the to hit mod add 4 got it and make sure the max is 8 since
    // the range is -4 to 8
    protected double[] battleForceToHitModifier = new double[]{1.20, 1.15,
                                                               1.10, 1.05, 1, .95, .9, .85, .8};
    public static final int BATTLEFORCESHORTRANGE = 0;
    public static final int BATTLEFORCEMEDIUMRANGE = 4;
    public static final int BATTLEFORCELONGRANGE = 16;
    public static final int BATTLEFORCEEXTREMERANGE = 24;

    // for how many rounds has blueshield been active?
    private int blueShieldRounds = 0;

    // Entity fluff object for use with MegaMekLab
    protected EntityFluff fluff;

    // a settable armor tonnage for use with MML - this is not what
    // is calculated by getArmorTonnage
    protected double armorTonnage;

    protected static int[] MASC_FAILURE = {3, 5, 7, 11, 13, 13, 13};
    protected static int[] ALTERNATE_MASC_FAILURE = {0, 3, 5, 7, 11, 13, 13,
                                                     13};
    protected static int[] ALTERNATE_MASC_FAILURE_ENHANCED = {0, 3, 3, 5, 7,
                                                              11, 13, 13, 13};

    // MASCLevel is the # of turns MASC has been used previously
    protected int nMASCLevel = 0;

    protected boolean bMASCWentUp = false;

    protected boolean usedMASC = false; // Has masc been used?

    /**
     * Nova CEWS can adjust the network on the fly. This keeps track of the C3
     * net ID to be switched to on the next turn.
     */
    private String newC3NetIdString = null;

    /**
     * Keeps track of the number of iATM improved magnetic pulse (IMP) his this
     * entity took this turn.
     */
    private int impThisTurn = 0;

    /**
     * Keeps track of the number of iATM improved magnetic pulse (IMP) his this
     * entity took last turn.
     */
    private int impLastTurn = 0;

    private int impThisTurnHeatHelp = 0;

    protected boolean military;

    /**
     * Keeps track of whether or not this Entity has a critically hit radical
     * heat sink.  Using a flag will prevent having to iterate over all of the
     * Entity's mounted equipment
     */
    protected boolean hasDamagedRHS = false;

    /**
     * Keeps track of the number of consecutive turns a radical heat sink has
     * been used.
     */
    protected int consecutiveRHSUses = 0;

    /**
     * Flag that can be used to indicate whether this Entity should use a
     * geometric mean when computing BV.
     */
    protected boolean useGeometricBV = false;

    protected boolean useReducedOverheatModifierBV = false;

    private final Set<Integer> attackedByThisTurn =
            Collections.newSetFromMap(new ConcurrentHashMap<Integer, Boolean>());

    /**
     * Determines the sort order for weapons in the UnitDisplay weapon list.
     */
    private WeaponSortOrder weaponSortOrder;

    /**
     * Maps a weapon id to a user-specified index, used to get a custom ordering
     * for weapons.
     */
    private Map<Integer, Integer> customWeapOrder = null;

    /**
     * Flag that indicates weapon sort order has changed (included ordering for
     * custom sort order).
     */
    private boolean weapOrderChanged = false;

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
        armorType = new int[locations()];
        armorTechLevel = new int[locations()];
        for (int i = 0; i < locations(); i++) {
            crits[i] = new CriticalSlot[getNumberOfCriticals(i)];
            armorType[i] = EquipmentType.T_ARMOR_UNKNOWN;
            armorTechLevel[i] = TechConstants.T_TECH_UNKNOWN;
        }
        hardenedArmorDamaged = new boolean[locations()];
        locationBlownOff = new boolean[locations()];
        locationBlownOffThisPhase = new boolean[locations()];
        setC3NetId(this);
        quirks.initialize();
        secondaryPositions = new HashMap<Integer, Coords>();
        fluff = new EntityFluff();
        impThisTurn = 0;
        impLastTurn = 0;

        weaponSortOrder = WeaponSortOrder.values()[GUIPreferences.getInstance()
                .getDefaultWeaponSortOrder()];
        
        //set a random UUID for external ID, this will help us sort enemy salvage and prisoners in MHQ
        //and should have no effect on MM (but need to make sure it doesnt screw up MekWars)
        externalId = UUID.randomUUID().toString();
    }

    protected void initMilitary() {
        military = hasViableWeapons();
    }

    protected boolean hasViableWeapons() {
        int totalDmg = 0;
        boolean hasRangeSixPlus = false;
        List<Mounted> weaponList = getTotalWeaponList();
        for (Mounted weapon : weaponList) {
            if (weapon.isCrippled()) {
                continue;
            }
            WeaponType type = (WeaponType) weapon.getType();
            if (type.getDamage() == WeaponType.DAMAGE_VARIABLE) {

            } else if (type.getDamage() == WeaponType.DAMAGE_ARTILLERY) {
                return true;
            } else if (type.getDamage() == WeaponType.DAMAGE_BY_CLUSTERTABLE) {
                totalDmg += type.getRackSize();
            } else if (type.getDamage() == WeaponType.DAMAGE_SPECIAL) {
                if (type instanceof ISBAPopUpMineLauncher) {
                    totalDmg += 4;
                }
            } else {
                totalDmg += type.getDamage();
            }

            if (type.getLongRange() >= 6) {
                hasRangeSixPlus = true;
            }
        }
        return (totalDmg >= 5) || hasRangeSixPlus;
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
     * @param id the new ID.
     */
    public void setId(int id) {
        this.id = id;
        displayName = null;
        shortName = null;
    }

    /**
     * this returns the external ID. 1/16/2012 - Taharqa: I am changing
     * externalId to a string so I can use UUIDs in MHQ. It should only require
     * a simple parseInt to be added to it to return an integer for other
     * programs (i.e. MekWars)
     *
     * @return the ID settable by external sources (such as mm.net)
     * @see megamek.common.Entity#externalId
     */
    public int getExternalId() {
        return Integer.parseInt(externalId);
    }

    public String getExternalIdAsString() {
        return externalId;
    }

    /**
     * This sets the external ID.
     *
     * @param externalId the new external ID for this Entity.
     * @see megamek.common.Entity#externalId
     */
    public void setExternalIdAsString(String externalId) {
        this.externalId = externalId;
    }

    public void setExternalId(int id) {
        externalId = Integer.toString(id);
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
     * @param game the game.
     */
    public void setGame(IGame game) {
        this.game = game;
        restore();
        // Make sure the owner is set.
        if (null == owner) {
            if (Entity.NONE == ownerId) {
                throw new IllegalStateException(
                        "Entity doesn't know its owner's ID.");
            }
            IPlayer player = game.getPlayer(ownerId);
            if (null == player) {
                System.err.println("Entity can't find player #" + ownerId);
            } else {
                setOwner(player);
            }
        }
        // also set game for our transports
        // they need it to return correct entites, because they store just the
        // IDs
        for (Transporter transport : getTransports()) {
            transport.setGame(game);
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
     * @param model The unit code.
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
     * @param chassis The chassis name.
     */
    public void setChassis(String chassis) {
        this.chassis = chassis;
    }

    /**
     * Returns the fluff for this entity.
     */
    public EntityFluff getFluff() {
        return fluff;
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
     * @param techLevel The tech level, it must be one of the
     *                  {@link megamek.common.TechConstants TechConstants }.
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

    public boolean isManualShutdown() {
        return manualShutdown;
    }

    public void setManualShutdown(boolean tf) {
        manualShutdown = tf;
    }

    public void performManualShutdown() {
        if (isManualShutdown() || (getTaserShutdownRounds() != 0)
            || isShutDown()) {
            return;
        }
        setShutDown(true);
        setManualShutdown(true);
    }

    public void performManualStartup() {
        if (!isManualShutdown()) {
            return;
        }
        setManualShutdown(false);
        // Can't startup if a taser shutdown or a TSEMP shutdown
        if ((getTaserShutdownRounds() == 0)
            && (getTsempEffect() != TSEMPWeapon.TSEMP_EFFECT_SHUTDOWN)) {
            setShutDown(false);
            setStartupThisPhase(true);
        }
    }

    /**
     * Checks if this is a clan unit. It is determined by tech level.
     *
     * @return true if this unit is a clan unit.
     * @see megamek.common.Entity#setTechLevel(int)
     */
    public boolean isClan() {
        return ((techLevel == TechConstants.T_CLAN_TW)
                || (techLevel == TechConstants.T_CLAN_ADVANCED)
                || (techLevel == TechConstants.T_CLAN_EXPERIMENTAL) || (techLevel == TechConstants.T_CLAN_UNOFFICIAL));
    }

    public boolean isClanArmor(int loc) {
        if (getArmorTechLevel(loc) == TechConstants.T_TECH_UNKNOWN) {
            return isClan();
        }
        return ((getArmorTechLevel(loc) == TechConstants.T_CLAN_TW)
                || (getArmorTechLevel(loc) == TechConstants.T_CLAN_ADVANCED)
                || (getArmorTechLevel(loc) == TechConstants.T_CLAN_EXPERIMENTAL) || (getArmorTechLevel(loc) ==
                                                                                     TechConstants.T_CLAN_UNOFFICIAL));
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

    // TODO: WeightClass is no longer correct. See the Tech Manual
    public int getWeightClass() {
        return EntityWeightClass.getWeightClass(getWeight(), this);
    }

    public String getWeightClassName() {
        return EntityWeightClass.getClassName(getWeightClass(), this);
    }

    public void setWeight(float weight) {
        this.weight = weight;
        // Any time the weight is reset we need to reset the crew size
        crew.setSize(Compute.getFullCrewSize(this));
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
    public IPlayer getOwner() {
        return owner;
    }

    public void setOwner(IPlayer player) {
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
        if (other == null) {
            return false;
        }
        if (null == owner) {
            return ((id != other.getId()) && (ownerId != other.ownerId));
        }
        return (id != other.getId()) && owner.isEnemyOf(other.getOwner());
    }

    public Crew getCrew() {
        return crew;
    }

    public void setCrew(Crew crew) {
        this.crew = crew;
    }

    public boolean isShutDown() {
        return shutDown;
    }

    public void setShutDown(boolean shutDown) {
        this.shutDown = shutDown;
        setShutDownThisPhase(shutDown);
    }

    public void setShutDownThisPhase(boolean shutDown) {
        shutDownThisPhase = shutDown;
    }

    public boolean isShutDownThisPhase() {
        return shutDownThisPhase;
    }

    public void setStartupThisPhase(boolean shutDown) {
        startupThisPhase = shutDown;
    }

    public boolean isStartupThisPhase() {
        return startupThisPhase;
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

    public void setLoadedThisTurn(boolean loaded) {
        loadedThisTurn = loaded;
    }

    /**
     * Determine if this entity participate in the current game phase.
     *
     * @return <code>true</code> if this entity is not shut down, is not
     * destroyed, has an active crew, and was not unloaded from a
     * transport this turn. <code>false</code> otherwise.
     */
    public boolean isActive() {
        return this.isActive(-1);
    }

    public boolean isActive(int turn) {
        boolean isActive = !shutDown && !isManualShutdown() && !destroyed
                           && getCrew().isActive() && !unloadedThisTurn;

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
        return !done && (conveyance == Entity.NONE) && !unloadedThisTurn
               && !isClearingMinefield() && !isCarcass();
    }

    /**
     * Returns true if this entity could potentially be loaded (did not move
     * from starting hex)
     */
    public boolean isLoadableThisTurn() {
        return (delta_distance == 0) && (conveyance == Entity.NONE)
               && !unloadedThisTurn && !isClearingMinefield();
    }

    /**
     * Determine if this <code>Entity</code> was unloaded previously this turn.
     *
     * @return <code>true</code> if this entity was unloaded for any reason
     * during this turn.
     */
    public boolean isUnloadedThisTurn() {
        return unloadedThisTurn;
    }

    public boolean wasLoadedThisTurn() {
        return loadedThisTurn;
    }

    /**
     * Returns true if this entity is targetable for attacks.  A unit is
     * targetable if it is not destroyed, not doomed, deployed, not off board,
     * not being transported, and not captured.
     */
    public boolean isTargetable() {
        return !destroyed && !doomed && deployed && !isOffBoard()
               && (conveyance == Entity.NONE) && !captured
               && (getPosition() != null);
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
        return isShutDown() || ((crew != null) && crew.isUnconscious());
    }

    /**
     * This method returns true if a unit is permanently immobilized either
     * because its crew is dead/gone or because of damage
     *
     * @return true if unit is permanently immobile
     */
    public boolean isPermanentlyImmobilized(boolean checkCrew) {
        if (checkCrew && ((getCrew() == null) || getCrew().isDead())) {
            return true;
        } else if (((getOriginalWalkMP() > 0) || (getOriginalRunMP() > 0) || (getOriginalJumpMP() > 0))
                /*
                 * Need to make sure here that we're ignoring heat because
                 * that's not actually "permanent":
                 */
                && ((getWalkMP(true, true, false) == 0)
                    && (getRunMP(true, true, false) == 0) && (getJumpMP() == 0))) {
            return true;
        } else {
            return false;
        }
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

    public void setDisplacementAttack(
            DisplacementAttackAction displacementAttack) {
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
        for (Entity other : game.getEntitiesVector()) {
            if (other.hasDisplacementAttack()
                && (other.getDisplacementAttack().getTargetId() == id)) {
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
     * Returns a set of the coords this Entity occupies
     *
     * @return
     */
    public HashSet<Coords> getOccupiedCoords() {
        HashSet<Coords> positions = new HashSet<Coords>();
        if ((getSecondaryPositions() != null)
            && (getSecondaryPositions().size() != 0)) {
            for (int key : getSecondaryPositions().keySet()) {
                positions.add(getSecondaryPositions().get(key));
            }
        } else if (getPosition() != null) {
            positions.add(getPosition());
        }
        return positions;
    }

    public void setPosition(Coords position) {
        setPosition(position, true);
    }

    /**
     * Sets the current position of this entity on the board.
     *
     * @param position the new position.
     */
    public void setPosition(Coords position, boolean gameUpdate) {
        HashSet<Coords> oldPositions = null;
        if ((game != null) && gameUpdate) {
            oldPositions = getOccupiedCoords();
        }
        this.position = position;
        if ((game != null) && gameUpdate) {
            game.updateEntityPositionLookup(this, oldPositions);
        }
    }

    /**
     * @return the coords of the second to last position on the passed through
     * vector or the current position if too small
     */

    public Coords getPriorPosition() {
        if (passedThrough.size() < 2) {
            return getPosition();
        }
        return passedThrough.elementAt(passedThrough.size() - 2);
    }

    /**
     * Sets the current elevation of this entity above the ground.  This is the
     * number of levels the unit is above the level of the hex.
     *
     * @param elevation an <code>int</code> representing the new elevation.
     */
    public void setElevation(int elevation) {
        this.elevation = elevation;
    }

    /**
     * A helper function for fiddling with elevation. Takes the current hex, a
     * hex being moved to, returns the elevation the Entity will be considered
     * to be at w/r/t it's new hex.
     */
    public int calcElevation(IHex current, IHex next, int assumedElevation,
            boolean climb, boolean wigeEndClimbPrevious) {
        int retVal = assumedElevation;
        if (this instanceof Aero) {
            return retVal;
        }
        if ((getMovementMode() == EntityMovementMode.SUBMARINE)
            || ((getMovementMode() == EntityMovementMode.INF_UMU)
                && next.containsTerrain(Terrains.WATER) && current
                .containsTerrain(Terrains.WATER))
            || (getMovementMode() == EntityMovementMode.VTOL)
            // a WIGE in climb mode or that ended climb mode in the previous
            // hex stays at the same flight level, like a VTOL
            || ((getMovementMode() == EntityMovementMode.WIGE)
                && (climb || wigeEndClimbPrevious) && (assumedElevation > 0))
            || ((getMovementMode() == EntityMovementMode.QUAD_SWIM) && hasUMU())
            || ((getMovementMode() == EntityMovementMode.BIPED_SWIM) && hasUMU())) {
            retVal += current.surface();
            retVal -= next.surface();
        } else {
            if ((getMovementMode() != EntityMovementMode.HOVER)
                && (getMovementMode() != EntityMovementMode.NAVAL)
                && (getMovementMode() != EntityMovementMode.HYDROFOIL)
                && (getMovementMode() != EntityMovementMode.WIGE)) {
                int prevWaterLevel = 0;
                if (current.containsTerrain(Terrains.WATER)) {
                    prevWaterLevel = current.terrainLevel(Terrains.WATER);
                    if (!(current.containsTerrain(Terrains.ICE))
                        || (assumedElevation < 0)) {
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

                        if (((waterLevel == 1) && (prevWaterLevel == 1))
                            || ((prevWaterLevel <= 2) && climb)
                            || (assumedElevation >= 0)) {
                            retVal += waterLevel;
                        }
                    }
                    retVal -= waterLevel;
                }
            }
            
            // Airborne WiGEs remain 1 elevation above underlying terrain
            if ((getMovementMode() == EntityMovementMode.WIGE) 
                    && (assumedElevation > 0)) {
                if ((next.ceiling() - assumedElevation) 
                        <= getMaxElevationChange()) {
                    retVal = 1 + next.maxTerrainFeatureElevation(game
                            .getBoard().inAtmosphere());
                }
            }

            if ((next.containsTerrain(Terrains.BUILDING)
                || current.containsTerrain(Terrains.BUILDING))
                && (getMovementMode() != EntityMovementMode.WIGE)) {
                int bldcur = Math.max(-current.depth(true),
                                      current.terrainLevel(Terrains.BLDG_ELEV));
                int bldnex = Math.max(-next.depth(true),
                                      next.terrainLevel(Terrains.BLDG_ELEV));
                if (((assumedElevation == bldcur)
                     && (climb || isJumpingNow) && (this instanceof Mech))
                    || (retVal > bldnex)) {
                    retVal = bldnex;
                } else if ((bldnex + next.surface())
                        > (bldcur + current.surface())) {
                    int nextBasement =
                            next.terrainLevel(Terrains.BLDG_BASEMENT_TYPE);
                    int collapsedBasement =
                            next.terrainLevel(Terrains.BLDG_BASE_COLLAPSED);
                    if (climb || isJumpingNow) {
                        retVal = bldnex + next.surface();
                    // If the basement is collapsed, there is no level 0
                    } else if ((assumedElevation == 0)
                            && (nextBasement > BasementType.NONE.getValue())
                            && (collapsedBasement > 0)) {
                        retVal -= BasementType.getType(
                                next.terrainLevel(Terrains.BLDG_BASEMENT_TYPE))
                                              .getDepth();
                    } else {
                        retVal += current.surface();
                        retVal -= next.surface();
                    }
                } else if (elevation == -(current.depth(true))) {
                    if (climb || isJumpingNow) {
                        retVal = bldnex + next.surface();
                    } else if ((current
                                        .terrainLevel(Terrains.BLDG_BASEMENT_TYPE) > BasementType.NONE
                                        .getValue())
                               && (assumedElevation == -BasementType
                            .getType(
                                    current.terrainLevel(Terrains.BLDG_BASEMENT_TYPE))
                            .getDepth())) {
                        retVal = -BasementType.getType(
                                next.terrainLevel(Terrains.BLDG_BASEMENT_TYPE))
                                              .getDepth();
                    } else {
                        retVal += current.surface();
                        retVal -= next.surface();
                    }
                }
            }
            if ((getMovementMode() != EntityMovementMode.NAVAL)
                    && (getMovementMode() != EntityMovementMode.HYDROFOIL)
                    && (next.containsTerrain(Terrains.BRIDGE) || current
                            .containsTerrain(Terrains.BRIDGE))) {
                int bridgeElev;
                if (next.containsTerrain(Terrains.BRIDGE)) {
                    bridgeElev = next.terrainLevel(Terrains.BRIDGE_ELEV);
                } else {
                    bridgeElev = 0;
                }
                int elevDiff = Math.abs((next.surface() + bridgeElev)
                        - (current.surface() + assumedElevation)); 
                if (elevDiff <= getMaxElevationChange()) {
                    // bridge is reachable at least
                    if (climb || !isElevationValid(retVal, next)) {
                        // use bridge if you can't use the base terrain or if
                        // you prefer to by climb mode
                        retVal = bridgeElev;
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
     * Returns the elevation of this entity, relative to the current Hex's
     * surface
     */
    public int getElevation() {
        if (Entity.NONE != getTransportId()) {
            return game.getEntity(getTransportId()).getElevation();
        }

        if ((null == getPosition()) && (isDeployed())) {
            throw new IllegalStateException("Entity #" + getId()
                                            + " does not know its position.");
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
        if (!getGame().getBoard().contains(assumedPos)) {
            return false;
        }
        boolean inWaterOrWoods = false;
        IHex hex = getGame().getBoard().getHex(assumedPos);
        int assumedAlt = assumedElevation + hex.surface();
        int minAlt = hex.surface();
        if (hex.containsTerrain(Terrains.WOODS)
            || hex.containsTerrain(Terrains.WATER)
            || hex.containsTerrain(Terrains.JUNGLE)) {
            inWaterOrWoods = true;
        }
        switch (getMovementMode()) {
            case INF_JUMP:
            case INF_LEG:
            case INF_MOTORIZED:
                minAlt -= Math.max(
                        0,
                        BasementType.getType(
                                hex.terrainLevel(Terrains.BLDG_BASEMENT_TYPE))
                                    .getDepth());
                break;
            case VTOL:
            case WIGE:
                minAlt = hex.ceiling();
                if (inWaterOrWoods) {
                    minAlt++; // can't land here
                }
                break;
            case AERODYNE:
            case SPHEROID:
                assumedAlt = assumedElevation;
                if (game.getBoard().inAtmosphere()) {
                    minAlt = hex.ceiling(true) + 1;
                } else if (game.getBoard().onGround() && isAirborne()) {
                    minAlt = 1;
                }
                // if sensors are damaged then, one higher
                if ((this instanceof Aero)
                    && (((Aero) this).getSensorHits() > 0)) {
                    minAlt++;
                }
                break;
            case SUBMARINE:
            case INF_UMU:
            case BIPED_SWIM:
            case QUAD_SWIM:
                minAlt = hex.floor();
                break;
            case BIPED:
            case QUAD:
                if (this instanceof Protomech) {
                    minAlt -= Math
                            .max(0,
                                 BasementType
                                         .getType(
                                                 hex.terrainLevel(Terrains.BLDG_BASEMENT_TYPE))
                                         .getDepth());
                } else {
                    return false;
                }
                break;
            default:
                return false;
        }
        return (assumedAlt > minAlt);
    }

    /**
     * is it possible to go up, or are we at maximum altitude? assuming passed
     * elevation.
     */
    public boolean canGoUp(int assumedElevation, Coords assumedPos) {
        // Could have a hex off the board
        if (!getGame().getBoard().contains(assumedPos)) {
            return false;
        }
        IHex hex = getGame().getBoard().getHex(assumedPos);
        int assumedAlt = assumedElevation + hex.surface();
        int maxAlt = hex.surface();
        switch (getMovementMode()) {
            case INF_JUMP:
            case INF_LEG:
            case INF_MOTORIZED:
                maxAlt += Math.max(0, hex.terrainLevel(Terrains.BLDG_ELEV));
                break;
            case VTOL:
                maxAlt = hex.surface() + 50;
                break;
            case AERODYNE:
            case SPHEROID:
                if (!game.getBoard().inSpace()) {
                    assumedAlt = assumedElevation;
                    maxAlt = 10;
                }
                break;
            case SUBMARINE:
                maxAlt = hex.surface() - getHeight();
                break;
            case INF_UMU:
            case BIPED_SWIM:
            case QUAD_SWIM:
                // UMU's won't allow the entity to break the surface of the
                // water
                maxAlt = hex.surface() - (getHeight() + 1);
                break;
            case WIGE:
                maxAlt = hex.surface() + 1;
                break;
            case BIPED:
            case QUAD:
                if (this instanceof Protomech) {
                    maxAlt += Math.max(0, hex.terrainLevel(Terrains.BLDG_ELEV));
                } else {
                    return false;
                }
                break;
            default:
                return false;
        }
        return (assumedAlt < maxAlt);
    }

    /**
     * Check if this entity can legally occupy the requested elevation. Does not
     * check stacking, only terrain limitations
     */
    public boolean isElevationValid(int assumedElevation, IHex hex) {
        int assumedAlt = assumedElevation + hex.surface();
        if (getMovementMode() == EntityMovementMode.VTOL) {
            if ((this instanceof Infantry)
                && (hex.containsTerrain(Terrains.BUILDING)
                    || hex.containsTerrain(Terrains.WOODS) || hex
                    .containsTerrain(Terrains.JUNGLE))) {
                // VTOL BA (sylph) can move as ground unit as well
                return ((assumedElevation <= 50) && (assumedAlt >= hex.floor()));
            } else if (hex.containsTerrain(Terrains.WOODS)
                       || hex.containsTerrain(Terrains.WATER)
                       || hex.containsTerrain(Terrains.JUNGLE)) {
                return ((assumedElevation <= 50) && (assumedAlt > hex.ceiling()));
            }
            return ((assumedElevation <= 50) && (assumedAlt >= hex.ceiling()));
        } else if ((getMovementMode() == EntityMovementMode.SUBMARINE)
                   || ((getMovementMode() == EntityMovementMode.INF_UMU) && hex
                .containsTerrain(Terrains.WATER))
                   || ((getMovementMode() == EntityMovementMode.QUAD_SWIM) && hasUMU())
                   || ((getMovementMode() == EntityMovementMode.BIPED_SWIM) && hasUMU())) {
            return ((assumedAlt >= hex.floor()) && (assumedAlt <= hex.surface()));
        } else if ((getMovementMode() == EntityMovementMode.HYDROFOIL)
                   || (getMovementMode() == EntityMovementMode.NAVAL)) {
            return assumedAlt == hex.surface();
        } else if (getMovementMode() == EntityMovementMode.WIGE) {
            // WiGEs can possibly be at any location above or on the surface
            return (assumedAlt >= hex.floor());
        } else {
            // regular ground units
            if (hex.containsTerrain(Terrains.ICE)
                || ((getMovementMode() == EntityMovementMode.HOVER) && hex
                    .containsTerrain(Terrains.WATER))) {
                // surface of ice is OK, surface of water is OK for hovers
                if (assumedAlt == hex.surface()) {
                    return true;
                }
            }
            // only mechs can move underwater
            if (hex.containsTerrain(Terrains.WATER)
                && (assumedAlt < hex.surface()) && !(this instanceof Mech)
                && !(this instanceof Protomech)) {
                return false;
            }
            // can move on the ground unless its underwater
            if (assumedAlt == hex.floor()) {
                return true;
            }

            if (hex.containsTerrain(Terrains.BRIDGE)) {
                // can move on top of a bridge
                if (assumedElevation == hex.terrainLevel(Terrains.BRIDGE_ELEV)) {
                    return true;
                }
            }
            if (hex.containsTerrain(Terrains.BUILDING)) {
                // Any unit can fall into a basement
                if ((assumedAlt < 0) && (hex.depth(true) > 0)) {
                    return true;
                }
                // Mechs, protos and infantry can occupy any floor in the
                // building
                if ((this instanceof Mech) || (this instanceof Protomech)
                    || (this instanceof Infantry)) {
                    if ((assumedAlt >= (hex.surface() - hex.depth(true)))
                        && (assumedAlt <= hex.ceiling())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Returns the height of the unit, that is, how many levels above its
     * elevation it is for LOS purposes. Default is 0.
     */
    public int height() {
        return 0;
    }

    /**
     * Returns the elevation of the entity's highest point relative to
     * the surface of the hex the entity is in , i.e.
     * relHeight() == getElevation() + getHeight()
     */
    public int relHeight() {
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
            Entity transporter = game.getEntity(conveyance);
            if (transporter == null) {
                transporter = game.getOutOfGameEntity(conveyance);
            }
            return transporter.getFacing();
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
     * As of 5/22/2012 also returns true if there is a jammed and not destroyed
     * Ultra AC and the unofficial options is enabled.  Jammed ACs and LACs can
     * also be unjammed if rapid-fire ACs is turned on.
     */
    public boolean canUnjamRAC() {
        for (Mounted mounted : getTotalWeaponList()) {
            WeaponType wtype = (WeaponType) mounted.getType();
            if ((wtype.getAmmoType() == AmmoType.T_AC_ROTARY)
                && mounted.isJammed() && !mounted.isDestroyed()) {
                return true;
            }
            if (((wtype.getAmmoType() == AmmoType.T_AC_ULTRA)
                 || (wtype.getAmmoType() == AmmoType.T_AC_ULTRA_THB)
                 || (wtype.getAmmoType() == AmmoType.T_AC)
                 || (wtype.getAmmoType() == AmmoType.T_LAC))
                && mounted.isJammed()
                && !mounted.isDestroyed()
                && game.getOptions().booleanOption("unjam_uac")) {
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
     * @param gravity    Should the movement be factored for gravity
     * @param ignoreheat Should heat be ignored?
     */
    public int getWalkMP(boolean gravity, boolean ignoreheat) {
        return getWalkMP(gravity, ignoreheat, false);
    }

    public int getWalkMP(boolean gravity, boolean ignoreheat,
                         boolean ignoremodulararmor) {
        int mp = getOriginalWalkMP();

        if (!ignoreheat) {
            mp = Math.max(0, mp - getHeatMPReduction());
        }
        mp = Math.max(mp - getCargoMpReduction(), 0);
        if (null != game) {
            int weatherMod = game.getPlanetaryConditions()
                                 .getMovementMods(this);
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
        return getRunMP(true, false, false);
    }

    public int getRunMP(boolean gravity, boolean ignoreheat,
                        boolean ignoremodulararmor) {
        return (int) Math.ceil(getWalkMP(gravity, ignoreheat,
                                         ignoremodulararmor) * 1.5);
    }

    /**
     * Returns run MP without considering MASC
     */
    public int getRunMPwithoutMASC() {
        return getRunMPwithoutMASC(true, false, false);
    }

    /**
     * Returns run MP without considering MASC, optionally figuring in gravity
     * and possibly ignoring heat
     */
    public abstract int getRunMPwithoutMASC(boolean gravity,
                                            boolean ignoreheat, boolean ignoremodulararmor);

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
     * Returns this entity's unmodified sprint mp.
     */
    protected int getOriginalSprintMP() {
        return getOriginalRunMP();
    }

    /**
     * Returns this entity's running/flank mp modified for heat and gravity.
     */
    public int getSprintMP() {
        return getRunMP();
    }

    public int getSprintMP(boolean gravity, boolean ignoreheat,
                           boolean ignoremodulararmor) {
        return getRunMP(gravity, ignoreheat, ignoremodulararmor);
    }

    /**
     * Returns sprint MP without considering MASC
     */
    public int getSprintMPwithoutMASC() {
        return getRunMPwithoutMASC();
    }

    /**
     * Returns sprint MP without considering MASC, optionally figuring in
     * gravity and possibly ignoring heat
     */
    public int getSprintMPwithoutMASC(boolean gravity, boolean ignoreheat,
                                      boolean ignoremodulararmor) {
        return getRunMPwithoutMASC(gravity, ignoreheat, ignoremodulararmor);
    }

    /**
     * Returns this entity's sprint mp as a string.
     */
    public String getSprintMPasString() {
        return Integer.toString(getSprintMP());
    }

    /**
     * get the heat generated by this Entity when sprinting
     */
    public int getSprintHeat() {
        return 3;
    }

    /**
     * get the gravity limit for ground movement
     */
    public int getRunningGravityLimit() {
        return getRunMP(false, false, false);
    }

    /**
     * Returns this entity's original jumping mp.
     */
    public int getOriginalJumpMP() {

        if (hasModularArmor()) {
            return Math.max(0, jumpMP - 1);
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
     * Returns this entity's current jumping MP, not affected by terrain,
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
     * @param movedMP the number of movement points spent
     */
    public int getJumpHeat(int movedMP) {
        return 0;
    }

    /**
     * Returns this entity's current jumping MP, affected by terrain (like
     * water.)
     */
    public int getJumpMPWithTerrain() {
        return getJumpMP();
    }

    /**
     * Returns the absolute elevation above ground level 0 that this entity
     * would be on if it were placed into the specified hex.
     * Hovercraft, naval vessels, and hydrofoils move on the
     * surface of the water
     */
    public int elevationOccupied(IHex hex) {
        return elevationOccupied(hex, getElevation());
    }
     
    public int elevationOccupied(IHex hex, int elevation) {
        if (hex == null) {
            return 0;
        }
        if ((movementMode == EntityMovementMode.VTOL)
            || (movementMode == EntityMovementMode.WIGE)) {
            return hex.surface() + elevation;
        } else if (((movementMode == EntityMovementMode.HOVER)
                    || (movementMode == EntityMovementMode.NAVAL)
                    || (movementMode == EntityMovementMode.HYDROFOIL) 
                    || hex.containsTerrain(Terrains.ICE))
                   && hex.containsTerrain(Terrains.WATER)) {
            return hex.surface();
        } else {
            return hex.floor();
        }
    }

    /**
     * Returns true if the specified hex contains some sort of prohibited
     * terrain.
     */
    public boolean isLocationProhibited(Coords c) {
        return isLocationProhibited(c, elevation);
    }
        
    /**
     * Returns true if the specified hex contains some sort of prohibited 
     * terrain if the Entity is at the specified elevation.  Elevation generally
     * only matters for units like WiGEs or VTOLs.
     * 
     * @param c
     * @param currElevation
     * @return
     */
    public boolean isLocationProhibited(Coords c, int currElevation) {
        IHex hex = game.getBoard().getHex(c);
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
    public abstract String getMovementString(EntityMovementType mtype);

    /**
     * Returns the abbreviation of the name of the type of movement used.
     */
    public abstract String getMovementAbbr(EntityMovementType mtype);

    /**
     * Returns the name of the location specified.
     */
    public String getLocationName(HitData hit) {
        return getLocationName(hit.getLocation());
    }

    public abstract String[] getLocationNames();

    /**
     * Returns the name of the location specified.
     */
    public String getLocationName(int loc) {
        String[] locationNames = getLocationNames();

        if ((null == locationNames) || (loc >= locationNames.length)) {
            return "";
        }

        if (loc < 0) {
            return "None";
        }

        return locationNames[loc];
    }

    public abstract String[] getLocationAbbrs();

    /**
     * Returns the abbreviated name of the location specified.
     */
    public String getLocationAbbr(HitData hit) {
        return getLocationAbbr(hit.getLocation())
               + (hit.isRear() && hasRearArmor(hit.getLocation()) ? "R" : "")
               + (((hit.getEffect() & HitData.EFFECT_CRITICAL) == HitData.EFFECT_CRITICAL) ? " (critical)"
                                                                                           : "");
    }

    /**
     * Returns the abbreviated name of the location specified.
     */
    public String getLocationAbbr(int loc) {
        String[] locationAbbrs = getLocationAbbrs();

        if ((null == locationAbbrs) || (loc >= locationAbbrs.length)) {
            return "";
        }
        if (loc == Entity.LOC_NONE) {
            return "None";
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
    public abstract HitData rollHitLocation(int table, int side,
                                            int aimedLocation, int aimingMode, int cover);

    /**
     * Rolls up a hit location
     */
    public abstract HitData rollHitLocation(int table, int side);

    /**
     * Gets the location that excess damage transfers to. That is, one location
     * inwards.
     */
    public abstract HitData getTransferLocation(HitData hit);

    /**
     * int version
     */
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
        return getArmorForReal(loc, rear);
    }

    public int getArmorForReal(int loc, boolean rear) {
        return armor[loc];
    }

    public int getArmorForReal(int loc) {
        return getArmorForReal(loc, false);
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
     * @param loc  the location to check.
     * @param rear if true inspect the rear armor, else check the front.
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
     * @param val  the value of the armor (eg how many armor points)
     * @param loc  the location of the armor
     * @param rear true iff the armor is rear mounted.
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
        return getInternalForReal(loc);
    }

    public int getInternalForReal(int loc) {
        if ((this instanceof GunEmplacement) && (loc == Tank.LOC_TURRET)) {
            return Tank.LOC_TURRET;
        }
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
        return (getInternal(loc) == IArmorState.ARMOR_DESTROYED)
               || (isLocationBlownOff(loc) && !isLocationBlownOffThisPhase(loc));
    }

    public boolean isLocationTrulyDestroyed(int loc) {
        return internal[loc] == IArmorState.ARMOR_DESTROYED;
    }

    /**
     * Is this location destroyed or breached?
     */
    public boolean isLocationDoomed(int loc) {
        return (getInternal(loc) == IArmorState.ARMOR_DOOMED)
               || isLocationBlownOff(loc);
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
     * @param loc    the location who's exposure is to be set
     * @param status the status to set
     */
    public void setLocationStatus(int loc, int status) {
        setLocationStatus(loc, status, false);
    }

    /**
     * sets location exposure
     *
     * @param loc         the location who's exposure is to be set
     * @param status      the status to set
     * @param allowChange allow change of breached locations
     */
    public void setLocationStatus(int loc, int status, boolean allowChange) {
        if (allowChange || (exposure[loc] > ILocationExposureStatus.BREACHED)) { // can't
            // change
            // BREACHED
            // status
            exposure[loc] = status;
        }
    }

    /**
     * Returns true is the location is a leg
     *
     * @param loc the location to check.
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
        } else if ((value == IArmorState.ARMOR_DOOMED)
                   || (value == IArmorState.ARMOR_DESTROYED)) {
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
        if ((mod > 0) && (getCrew() != null)
            && getCrew().getOptions().booleanOption("some_like_it_hot")) {
            mod--;
        }

        return mod;
    }

    /**
     * Creates a new mount for this equipment and adds it in.
     */
    public Mounted addEquipment(EquipmentType etype, int loc)
            throws LocationFullException {
        return addEquipment(etype, loc, false);
    }

    /**
     * Creates a new mount for this equipment and adds it in.
     */
    public Mounted addEquipment(EquipmentType etype, int loc,
                                boolean rearMounted) throws LocationFullException {
        return addEquipment(etype, loc, rearMounted,
                            BattleArmor.MOUNT_LOC_NONE, false, false);
    }

    /**
     * Creates a new mount for this equipment and adds it in.
     */
    public Mounted addEquipment(EquipmentType etype, int loc,
                                boolean rearMounted, int baMountLoc, boolean isArmored,
                                boolean isTurreted) throws LocationFullException {
        return addEquipment(etype, loc, rearMounted, baMountLoc, isArmored,
                            isTurreted, false);
    }

    public Mounted addEquipment(EquipmentType etype, int loc,
                                boolean rearMounted, int baMountLoc, boolean isArmored,
                                boolean isTurreted, boolean isSponsonTurreted)
            throws LocationFullException {
        return addEquipment(etype, loc, rearMounted, baMountLoc, isArmored,
                            isTurreted, isSponsonTurreted, false);
    }

    public Mounted addEquipment(EquipmentType etype, int loc,
                                boolean rearMounted, int baMountLoc, boolean isArmored,
                                boolean isTurreted, boolean isSponsonTurreted,
                                boolean isPintleTurreted) throws LocationFullException {
        Mounted mounted = new Mounted(this, etype);
        mounted.setArmored(isArmored);
        mounted.setBaMountLoc(baMountLoc);
        mounted.setMechTurretMounted(isTurreted);
        mounted.setSponsonTurretMounted(isSponsonTurreted);
        mounted.setPintleTurretMounted(isPintleTurreted);
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
    public Mounted addEquipment(EquipmentType etype, int loc,
                                boolean rearMounted, int nAmmo) throws LocationFullException {
        Mounted mounted = new Mounted(this, etype);
        addEquipment(mounted, loc, rearMounted, nAmmo);
        return mounted;

    }

    /**
     * indicate whether this is a bomb mount
     */
    public Mounted addBomb(EquipmentType etype, int loc)
            throws LocationFullException {
        Mounted mounted = new Mounted(this, etype);
        addBomb(mounted, loc);
        return mounted;
    }

    protected void addBomb(Mounted mounted, int loc)
            throws LocationFullException {
        mounted.setBombMounted(true);
        addEquipment(mounted, loc, false);
    }

    public Mounted addWeaponGroup(EquipmentType etype, int loc)
            throws LocationFullException {
        Mounted mounted = new Mounted(this, etype);
        addEquipment(mounted, loc, false, true);
        return mounted;
    }

    /**
     * indicate whether this is bodymounted for BAs
     */
    public Mounted addEquipment(EquipmentType etype, int loc,
                                boolean rearMounted, int baMountLoc, boolean dwpMounted)
            throws LocationFullException {
        Mounted mounted = new Mounted(this, etype);
        mounted.setBaMountLoc(baMountLoc);
        mounted.setDWPMounted(dwpMounted);
        addEquipment(mounted, loc, rearMounted);
        return mounted;
    }

    protected void addEquipment(Mounted mounted, int loc, boolean rearMounted,
                                int nAmmo) throws LocationFullException {
        if ((mounted.getType() instanceof AmmoType) && (nAmmo > 1)) {
            mounted.setByShot(true);
            mounted.setShotsLeft(nAmmo);
            mounted.setOriginalShots(nAmmo);
        }

        addEquipment(mounted, loc, rearMounted);
    }

    protected void addEquipment(Mounted mounted, int loc, boolean rearMounted,
                                boolean isWeaponGroup) throws LocationFullException {
        mounted.setWeaponGroup(true);

        addEquipment(mounted, loc, rearMounted);
    }

    public void addEquipment(Mounted mounted, int loc, boolean rearMounted)
            throws LocationFullException {
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
            if (mounted.getType().hasFlag(WeaponType.F_ONESHOT)
                    && (AmmoType.getOneshotAmmo(mounted) != null)) {
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
        Mounted m = cs.getMount();
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
                totalShotsLeft += amounted.getUsableShotsLeft();
            }
        }
        return totalShotsLeft;
    }

    /**
     * Determine how much ammunition (of all munition types) remains which is
     * compatable with the given ammo.
     *
     * @param et - the <code>EquipmentType</code> of the ammo to be found. This
     *           value may be <code>null</code>.
     * @return the <code>int</code> count of the amount of shots of all
     * munitions equivalent to the given ammo type.
     */
    public int getTotalMunitionsOfType(EquipmentType et) {
        int totalShotsLeft = 0;
        for (Mounted amounted : getAmmo()) {
            if (amounted.getType().equals(et) && !amounted.isDumping()) {
                totalShotsLeft += amounted.getUsableShotsLeft();
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

    public List<Mounted> getTotalWeaponList() {
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
     * ready.
     */
    public int getFirstWeapon() {
        // Now phase appropriate, since we don't really care to select weapons
        // we can't use during this phase... do we?
        for (Mounted mounted : getWeaponList()) {
            // TAG only in the correct phase...
            if ((mounted.getType().hasFlag(WeaponType.F_TAG) && (game
                                                                         .getPhase() != IGame.Phase.PHASE_OFFBOARD))
                || (!mounted.getType().hasFlag(WeaponType.F_TAG) && (game
                                                                             .getPhase() == IGame.Phase.PHASE_OFFBOARD))
                || mounted.getType().hasFlag(WeaponType.F_AMS)) {
                continue;
            }

            // Artillery only in the correct phase...
            if (!mounted.getType().hasFlag(WeaponType.F_ARTILLERY)
                && (game.getPhase() == IGame.Phase.PHASE_TARGETING)) {
                continue;
            }

            // No linked MGs...
            if (mounted.getType().hasFlag(WeaponType.F_MG)) {
                if (hasLinkedMGA(mounted)) {
                    continue;
                }
            }

            // It must be ready to be used...
            if (mounted.isReady()) {
                return getEquipmentNum(mounted);
            }
        }
        return -1;
    }

    /**
     * Returns true if the weapon, specified as a weapon id, is valid for the
     * current phase.
     *
     * @param weapNum
     * @return True if valid, else false
     */
    public boolean isWeaponValidForPhase(int weapNum) {
        return isWeaponValidForPhase(equipmentList.get(weapNum));
    }

    /**
     * Returns true if the weapon, specified as a <code>Mounted</code>, is
     * valid for the current phase.
     *
     * @param mounted
     * @return True if valid, else false
     */
    public boolean isWeaponValidForPhase(Mounted mounted) {
        // Start reached, now we can attempt to pick a weapon.
        if ((mounted != null)
            && (mounted.isReady())
            && (!mounted.getType().hasFlag(WeaponType.F_AMS))
            && ((mounted.getLinked() == null)
                || mounted.getLinked().getType().hasFlag(MiscType.F_AP_MOUNT)
                || (mounted.getLinked().getUsableShotsLeft() > 0))) {

            // TAG only in the correct phase...
            if ((mounted.getType().hasFlag(WeaponType.F_TAG)
                    && (game.getPhase() != IGame.Phase.PHASE_OFFBOARD))
                    || (!mounted.getType().hasFlag(WeaponType.F_TAG)
                            && (game.getPhase()
                                    == IGame.Phase.PHASE_OFFBOARD))) {
                return false;
            }

            // Artillery only in the correct phase...
            if (!mounted.getType().hasFlag(WeaponType.F_ARTILLERY)
                && (game.getPhase() == IGame.Phase.PHASE_TARGETING)) {
                return false;
            }

            // No linked MGs...
            if (mounted.getType().hasFlag(WeaponType.F_MG)) {
                if (hasLinkedMGA(mounted)) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
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

        if (mountedAmmo.isAmmoUsable() && !wtype.hasFlag(WeaponType.F_ONESHOT)
            && (atype.getAmmoType() == wtype.getAmmoType())
            && (atype.getRackSize() == wtype.getRackSize())) {
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

        if ((oldammo != null)
            && (!((AmmoType) oldammo.getType()).equals(atype) || (((AmmoType) oldammo
                .getType()).getMunitionType() != atype
                                                                          .getMunitionType()))) {
            return false;
        }

        return loadWeapon(mounted, mountedAmmo);
    }

    /**
     * Checks whether a weapon has been fired on this unit this turn
     *
     * @return
     */
    public boolean weaponFired() {
        boolean fired = false;
        for (int loc = 0; (loc < locations()) && !fired; loc++) {
            fired |= weaponFiredFrom(loc);
        }
        return fired;
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
            if ((slot == null)
                || (slot.getType() != CriticalSlot.TYPE_EQUIPMENT)) {
                continue;
            }
            Mounted mounted = slot.getMount();
            if ((mounted.getType() instanceof WeaponType)
                && mounted.isUsedThisRound()) {
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

    public Vector<Mounted> getBombs(BigInteger flag) {
        Vector<Mounted> bombs = new Vector<Mounted>();
        for (Mounted bomb : getBombs()) {
            BombType btype = (BombType) bomb.getType();
            if (!bomb.isInoperable() && (bomb.getUsableShotsLeft() > 0)
                && btype.hasFlag(flag)) {
                bombs.add(bomb);
            }
        }
        return bombs;
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

    public void removeWeapon(String toRemove) {
        for (Mounted mounted : getMisc()) {
            if (mounted.getName().equals(toRemove)) {
                weaponList.remove(mounted);
                equipmentList.remove(mounted);
                break;
            }
        }
    }

    /**
     * Clear all bombs and bomb attacks
     */
    public void clearBombs() {
        bombList.clear();
        for (Iterator<Mounted> i = equipmentList.iterator(); i.hasNext(); ) {
            Mounted m = i.next();
            if ((m.getType() instanceof BombType)
                || (m.getType() instanceof DiveBombAttack)
                || (m.getType() instanceof SpaceBombAttack)
                || (m.getType() instanceof AltitudeBombAttack)
                || (m.getType() instanceof ISAAAMissileWeapon)
                || (m.getType() instanceof CLAAAMissileWeapon)
                || (m.getType() instanceof ASMissileWeapon)
                || (m.getType() instanceof ASEWMissileWeapon)
                || (m.getType() instanceof ISLAAMissileWeapon)
                || (m.getType() instanceof CLLAAMissileWeapon)
                || (m.getType() instanceof BombArrowIV)
                    /*|| m.getType() instanceof CLBombArrowIV*/
                    || (m.getType() instanceof BombTAG)
                    || (m.getType() instanceof BombISRL10)
                    || (m.getType() instanceof AlamoMissileWeapon)) {
                i.remove();
            }
        }
        for (Iterator<Mounted> i = weaponList.iterator(); i.hasNext(); ) {
            Mounted m = i.next();
            if ((m.getType() instanceof DiveBombAttack)
                || (m.getType() instanceof SpaceBombAttack)
                || (m.getType() instanceof AltitudeBombAttack)
                || (m.getType() instanceof ISAAAMissileWeapon)
                || (m.getType() instanceof CLAAAMissileWeapon)
                || (m.getType() instanceof ASMissileWeapon)
                || (m.getType() instanceof ASEWMissileWeapon)
                || (m.getType() instanceof ISLAAMissileWeapon)
                || (m.getType() instanceof CLLAAMissileWeapon)
                || (m.getType() instanceof BombArrowIV)
                    /*|| m.getType() instanceof CLBombArrowIV*/
                    || (m.getType() instanceof BombTAG)
                    || (m.getType() instanceof BombISRL10)
                    || (m.getType() instanceof AlamoMissileWeapon)) {
                i.remove();
            }
        }
        for (Iterator<Mounted> i = ammoList.iterator(); i.hasNext(); ) {
            Mounted m = i.next();
            if (m.getType() instanceof BombType) {
                i.remove();
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
     * Check if the entity has an arbitrary type of misc equipment
     *
     * @param flag A MiscType.F_XXX
     * @return true if at least one ready item.
     */
    public boolean hasWorkingMisc(BigInteger flag) {
        return hasWorkingMisc(flag, -1);
    }

    /**
     * Check if the entity has an arbitrary type of misc equipment
     *
     * @param flag      A MiscType.F_XXX
     * @param secondary A MiscType.S_XXX or -1 for don't care
     * @return true if at least one ready item.
     */
    public boolean hasWorkingMisc(BigInteger flag, long secondary) {
        for (Mounted m : miscList) {
            if ((m.getType() instanceof MiscType) && m.isReady()) {
                MiscType type = (MiscType) m.getType();
                if (type.hasFlag(flag)
                    && ((secondary == -1) || type.hasSubType(secondary))) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public boolean hasMisc(BigInteger flag) {
        for (Mounted m : miscList) {
            if ((m.getType() instanceof MiscType)) {
                MiscType type = (MiscType) m.getType();
                if (type.hasFlag(flag)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * return how many misc equipments with the specified flag the unit has
     *
     * @param flag
     * @return
     */
    public int countWorkingMisc(BigInteger flag) {
        return countWorkingMisc(flag, -1);
    }

    public int countWorkingMisc(BigInteger flag, int location) {
        int count = 0;
        for (Mounted m : getMisc()) {
            if (!m.isInoperable() && m.getType().hasFlag(flag)
                && (!m.getType().hasModes() || m.curMode().equals("On"))) { //$NON-NLS-1$
                if ((location == -1) || (m.getLocation() == location)) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Check if the entity has an arbitrary type of misc equipment
     *
     * @param name MiscType internal name
     * @return true if at least one ready item.
     */
    public boolean hasWorkingMisc(String name) {
        for (Mounted m : miscList) {
            if ((m.getType() instanceof MiscType) && m.isReady()) {
                MiscType type = (MiscType) m.getType();
                if (type.internalName.equalsIgnoreCase(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if the entity has an arbitrary type of misc equipment
     *
     * @param flag      A MiscType.F_XXX
     * @param secondary A MiscType.S_XXX or -1 for don't care
     * @param location  The location to check e.g. Mech.LOC_LARM
     * @return true if at least one ready item.
     */
    public boolean hasWorkingMisc(BigInteger flag, long secondary, int location) {
        // go through the location slot by slot, because of misc equipment that
        // is spreadable
        for (int slot = 0; slot < getNumberOfCriticals(location); slot++) {
            CriticalSlot crit = getCritical(location, slot);
            if ((null != crit)
                && (crit.getType() == CriticalSlot.TYPE_EQUIPMENT)) {
                Mounted mount = crit.getMount();
                if (mount == null) {
                    continue;
                }
                if ((mount.getType() instanceof MiscType) && mount.isReady()) {
                    MiscType type = (MiscType) mount.getType();
                    if (type.hasFlag(flag)
                        && ((secondary == -1) || type.hasSubType(secondary))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Check if the entity has an arbitrary type of weapon
     *
     * @param flag A WeaponType.F_XXX
     */
    public boolean hasWorkingWeapon(BigInteger flag) {
        return hasWorkingWeapon(flag, -1);
    }

    /**
     * Check if the entity has an arbitrary type of weapon
     *
     * @param flag      A WeaponType.F_XXX
     * @param secondary A WeaponType.S_XXX or -1 for don't care
     * @return true if at least one ready item.
     */
    public boolean hasWorkingWeapon(BigInteger flag, long secondary) {
        for (Mounted m : weaponList) {
            if ((m.getType() instanceof WeaponType) && m.isReady()) {
                WeaponType type = (WeaponType) m.getType();
                if (type.hasFlag(flag)
                    && ((secondary == -1) || type.hasSubType(secondary))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if the entity has an arbitrary type of weapon
     *
     * @param name internal name of the weapon.
     * @return true if at least one ready item.
     */
    public boolean hasWorkingWeapon(String name) {
        for (Mounted m : weaponList) {
            if ((m.getType() instanceof WeaponType) && m.isReady()) {
                WeaponType type = (WeaponType) m.getType();
                if (type.getInternalName().equalsIgnoreCase(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if the entity has an arbitrary type of weapon
     *
     * @param flag      A WeaponType.F_XXX
     * @param secondary A WeaponType.S_XXX or -1 for don't care
     * @param location  The location to check e.g. Mech.LOC_LARM
     * @return true if at least one ready item.
     */
    public boolean hasWorkingWeapon(BigInteger flag, int secondary, int location) {
        // go through the location slot by slot, because of misc equipment that
        // is spreadable
        for (int slot = 0; slot < getNumberOfCriticals(location); slot++) {
            CriticalSlot crit = getCritical(location, slot);
            if ((null != crit)
                && (crit.getType() == CriticalSlot.TYPE_EQUIPMENT)) {
                Mounted mount = crit.getMount();
                if (mount == null) {
                    continue;
                }
                if ((mount.getType() instanceof WeaponType) && mount.isReady()) {
                    WeaponType type = (WeaponType) mount.getType();
                    if (type.hasFlag(flag)
                        && ((secondary == -1) || type.hasSubType(secondary))) {
                        return true;
                    }
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
     * Adds a critical to a critical slot, first trying the supplied slot
     * number, and continuing from there if it's full
     *
     * @return true if there was room for the critical
     */
    public boolean addCritical(int loc, CriticalSlot cs, int slotNumber) {
        for (int i = 0; i < getNumberOfCriticals(loc); i++) {
            if (getCritical(loc, slotNumber) == null) {
                crits[loc][slotNumber] = cs;
                return true;
            }
            slotNumber = (slotNumber + 1) % getNumberOfCriticals(loc);
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
            CriticalSlot crit = getCritical(loc, i);
            if ((crit != null) && getCritical(loc, i).isHittable()) {
                hittable++;
            }
            // Reactive armor criticals in a location with armor should count
            // as hittable, evne though they aren't actually hittable
            else if ((crit != null)
                    && (crit.getType() == CriticalSlot.TYPE_EQUIPMENT)
                    && (crit.getMount() != null)
                    && crit.getMount().getType().hasFlag(MiscType.F_REACTIVE)
                    && (getArmor(loc) > 0)) {
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
        Mounted m = null;
        if (type == CriticalSlot.TYPE_EQUIPMENT) {
            m = getEquipment(index);
        }

        int numberOfCriticals = getNumberOfCriticals(loc);
        for (int i = 0; i < numberOfCriticals; i++) {
            CriticalSlot ccs = getCritical(loc, i);

            //  Check to see if this crit mounts the supplied item
            //  For systems, we can compare the index, but for equipment we
            //  need to get the Mounted that is mounted in that index and
            //  compare types.  Superheavies may have two Mounted in each crit
            if ((ccs != null) && (ccs.getType() == type)) {
                if (!ccs.isDestroyed() && !ccs.isBreached()) {
                    if ((type == CriticalSlot.TYPE_SYSTEM) && (ccs.getIndex() == index)) {
                        operational++;
                    } else if ((type == CriticalSlot.TYPE_EQUIPMENT) && (m.equals(ccs.getMount()) || m.equals(ccs
                                                                                                                      .getMount2()))) {
                        operational++;
                    }
                }
            }
        }
        return operational;
    }

    /**
     * The number of critical slots that are destroyed or breached in the
     * location or missing along with it (if it was blown off).
     */
    public int getBadCriticals(int type, int index, int loc) {
        int hits = 0;
        Mounted m = null;
        if (type == CriticalSlot.TYPE_EQUIPMENT) {
            m = getEquipment(index);
        }

        int numberOfCriticals = getNumberOfCriticals(loc);
        for (int i = 0; i < numberOfCriticals; i++) {
            CriticalSlot ccs = getCritical(loc, i);

            //  Check to see if this crit mounts the supplied item
            //  For systems, we can compare the index, but for equipment we
            //  need to get the Mounted that is mounted in that index and
            //  compare types.  Superheavies may have two Mounted in each crit
            if ((ccs != null) && (ccs.getType() == type)) {
                if (ccs.isDestroyed() || ccs.isBreached() || ccs.isMissing()) {
                    if ((type == CriticalSlot.TYPE_SYSTEM) && (ccs.getIndex() == index)) {
                        hits++;
                    } else if ((type == CriticalSlot.TYPE_EQUIPMENT) && (m.equals(ccs.getMount()) || m.equals(ccs
                                                                                                                      .getMount2()))) {
                        hits++;
                    }
                }
            }
        }
        return hits;
    }

    /**
     * Number of slots damaged (but not breached) in a location
     */
    public int getDamagedCriticals(int type, int index, int loc) {
        int hits = 0;
        Mounted m = null;
        if (type == CriticalSlot.TYPE_EQUIPMENT) {
            m = getEquipment(index);
        }

        int numberOfCriticals = getNumberOfCriticals(loc);
        for (int i = 0; i < numberOfCriticals; i++) {
            CriticalSlot ccs = getCritical(loc, i);

            //  Check to see if this crit mounts the supplied item
            //  For systems, we can compare the index, but for equipment we
            //  need to get the Mounted that is mounted in that index and
            //  compare types.  Superheavies may have two Mounted in each crit
            if ((ccs != null) && (ccs.getType() == type)) {
                if (ccs.isDamaged()) {
                    if ((type == CriticalSlot.TYPE_SYSTEM) && (ccs.getIndex() == index)) {
                        hits++;
                    } else if ((type == CriticalSlot.TYPE_EQUIPMENT) && (m.equals(ccs.getMount()) || m.equals(ccs
                                                                                                                      .getMount2()))) {
                        hits++;
                    }
                }
            }
        }
        return hits;
    }

    /**
     * Number of slots doomed, missing or destroyed in a location
     */
    public int getHitCriticals(int type, int index, int loc) {
        int hits = 0;
        Mounted m = null;
        if (type == CriticalSlot.TYPE_EQUIPMENT) {
            m = getEquipment(index);
        }

        int numberOfCriticals = getNumberOfCriticals(loc);
        for (int i = 0; i < numberOfCriticals; i++) {
            CriticalSlot ccs = getCritical(loc, i);

            //  Check to see if this crit mounts the supplied item
            //  For systems, we can compare the index, but for equipment we
            //  need to get the Mounted that is mounted in that index and
            //  compare types.  Superheavies may have two Mounted in each crit
            if ((ccs != null) && (ccs.getType() == type)) {
                if (ccs.isDamaged() || ccs.isBreached() || ccs.isMissing()) {
                    if ((type == CriticalSlot.TYPE_SYSTEM) && (ccs.getIndex() == index)) {
                        hits++;
                    } else if ((type == CriticalSlot.TYPE_EQUIPMENT) && (m.equals(ccs.getMount()) || m.equals(ccs
                                                                                                                      .getMount2()))) {
                        hits++;
                    }
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
        if ((null == noOfSlots) || (loc >= noOfSlots.length)
            || (loc == LOC_NONE)) {
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
            if ((ccs != null) && (ccs.getType() == type)
                && (ccs.getIndex() == index)) {
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
            if ((ccs != null) && (getEquipmentType(ccs) != null)
                && getEquipmentType(ccs).equals(etype)) {
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
                if ((getBadCriticals(CriticalSlot.TYPE_SYSTEM,
                                     Mech.ACTUATOR_HIP, i) > 0)
                    || (getBadCriticals(CriticalSlot.TYPE_SYSTEM,
                                        Mech.ACTUATOR_UPPER_LEG, i) > 0)
                    || (getBadCriticals(CriticalSlot.TYPE_SYSTEM,
                                        Mech.ACTUATOR_LOWER_LEG, i) > 0)
                    || (getBadCriticals(CriticalSlot.TYPE_SYSTEM,
                                        Mech.ACTUATOR_FOOT, i) > 0)) {
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
            if ((ccs != null) && (ccs.getType() == CriticalSlot.TYPE_SYSTEM)
                && (ccs.getIndex() == system) && !ccs.isDamaged()
                && !ccs.isBreached()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns false if there is at least one non-repairable critical slot for
     * this system in the given location
     */
    public boolean isSystemRepairable(int system, int loc) {
        for (int i = 0; i < getNumberOfCriticals(loc); i++) {
            CriticalSlot ccs = getCritical(loc, i);
            if ((ccs != null) && (ccs.getType() == CriticalSlot.TYPE_SYSTEM)
                && (ccs.getIndex() == system) && !ccs.isRepairable()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if the the location has a system of the type, whether is
     * destroyed or not
     */
    public boolean hasSystem(int system, int loc) {
        for (int i = 0; i < getNumberOfCriticals(loc); i++) {
            CriticalSlot ccs = getCritical(loc, i);
            if ((ccs != null) && (ccs.getType() == CriticalSlot.TYPE_SYSTEM)
                && (ccs.getIndex() == system)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks to see if this entity is wielding any vibroblades
     *
     * @return always returns <code>false</code> as Only biped mechs can wield
     * vibroblades
     */
    public boolean hasVibroblades() {
        return false;
    }

    /**
     * Checks to see if any heat is given off by an active vibro blade
     *
     * @param location
     * @return always returns <code>0</code> as Only biped mechs can wield
     * vibroblades
     */
    public int getActiveVibrobladeHeat(int location) {
        return 0;
    }

    public int getActiveVibrobladeHeat(int location, boolean ignoreMode) {
        return 0;
    }

    /**
     * Does the mech have any shields. a mech can have up to 2 shields.
     *
     * @return <code>true</code> if <code>shieldCount</code> is greater than 0
     * else <code>false</code>
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
        if (!(this instanceof Mech) && !(this instanceof BattleArmor)) {
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

        if ((this instanceof BattleArmor)
            && (getMovementMode() == EntityMovementMode.INF_UMU)) {
            // UMU MP for BA is stored in jumpMP
            return jumpMP;
        }

        if (hasShield() && (getNumberOfShields(MiscType.S_SHIELD_LARGE) > 0)) {
            return 0;
        }

        for (Mounted m : getMisc()) {
            EquipmentType type = m.getType();
            if ((type instanceof MiscType) && type.hasFlag(MiscType.F_UMU)
                && !(m.isDestroyed() || m.isMissing() || m.isBreached())) {
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

        if ((this instanceof BattleArmor)
            && (getMovementMode() == EntityMovementMode.INF_UMU)) {
            // UMU MP for BA is stored in jumpMP
            return jumpMP;
        }

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
        return hasActiveECM(false);
    }

    /**
     * check if we have an active ECM unit for stealth armor purposes
     *
     * @param stealth
     * @return
     */
    public boolean hasActiveECM(boolean stealth) {
        // no ECM in space unless strat op option enabled
        if (game.getBoard().inSpace()
            && !game.getOptions().booleanOption("stratops_ecm")) {
            return false;
        }
        if (!isShutDown()) {
            for (Mounted m : getMisc()) {
                EquipmentType type = m.getType();
                // EQ equipment does not count for stealth armor
                if (stealth && type.hasFlag(MiscType.F_EW_EQUIPMENT)) {
                    continue;
                }
                // TacOps p. 100 Angle ECM can have 1 ECM and 1 ECCM at the same
                // time
                if ((type instanceof MiscType)
                    && type.hasFlag(MiscType.F_ECM)
                    && (m.curMode().equals("ECM")
                        || m.curMode().equals("ECM & ECCM") || m
                        .curMode().equals("ECM & Ghost Targets"))) {
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
        if (game.getBoard().inSpace()
            && !game.getOptions().booleanOption("stratops_ecm")) {
            return false;
        }
        if (game.getOptions().booleanOption("tacops_angel_ecm")
            && !isShutDown()) {
            for (Mounted m : getMisc()) {
                EquipmentType type = m.getType();
                if ((type instanceof MiscType)
                    && type.hasFlag(MiscType.F_ANGEL_ECM)
                    && (m.curMode().equals("ECM")
                        || m.curMode().equals("ECM & ECCM") || m
                        .curMode().equals("ECM & Ghost Targets"))) {
                    return !(m.isInoperable());
                }
            }
        }
        return false;
    }

    /**
     * WOR Does the mech have a functioning ECM unit?
     */
    public boolean hasActiveNovaECM() {
        // no ECM in space unless strat op option enabled
        if (game.getBoard().inSpace()
            && !game.getOptions().booleanOption("stratops_ecm")) {
            return false;
        }
        if (!isShutDown()) {
            for (Mounted m : getMisc()) {
                EquipmentType type = m.getType();
                if ((type instanceof MiscType) && type.hasFlag(MiscType.F_NOVA)
                    && m.curMode().equals("ECM")) {
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
            if ((type instanceof MiscType)
                && type.hasFlag(MiscType.F_ECM)
                && (m.curMode().equals("Ghost Targets")
                    || m.curMode().equals("ECM & Ghost Targets") || m
                    .curMode().equals("ECCM & Ghost Targets"))
                && !(m.isInoperable() || getCrew().isUnconscious())) {
                hasGhost = true;
            }
            if ((type instanceof MiscType)
                && type.hasFlag(MiscType.F_COMMUNICATIONS)
                && m.curMode().equals("Ghost Targets")
                && (getTotalCommGearTons() >= 7)
                && !(m.isInoperable() || getCrew().isUnconscious())) {
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
     * mode <code>false</code> if the entity does not have angel ecm or
     * it is not in eccm mode or it is damaged.
     */
    public boolean hasActiveECCM() {
        // no ECM in space unless strat op option enabled
        if (game.getBoard().inSpace()
            && !game.getOptions().booleanOption("stratops_ecm")) {
            return false;
        }
        if ((game.getOptions().booleanOption("tacops_eccm") || game
                .getOptions().booleanOption("stratops_ecm")) && !isShutDown()) {
            for (Mounted m : getMisc()) {
                EquipmentType type = m.getType();
                // TacOps p. 100 Angle ECM can have 1 ECM and 1 ECCM at the same
                // time
                if ((type instanceof MiscType)
                    && ((type.hasFlag(MiscType.F_ECM) && (m.curMode()
                                                           .equals("ECCM")
                                                          || m.curMode().equals("ECM & ECCM") || m
                        .curMode().equals("ECCM & Ghost Targets"))) || (type
                                                                                .hasFlag(MiscType.F_COMMUNICATIONS) && m
                                                                                .curMode().equals("ECCM")))) {
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
     * mode <code>false</code> if the entity does not have angel ecm or
     * it is not in eccm mode or it is damaged.
     */
    public boolean hasActiveAngelECCM() {
        if (game.getOptions().booleanOption("tacops_angel_ecm")
            && game.getOptions().booleanOption("tacops_eccm")
            && !isShutDown()) {
            for (Mounted m : getMisc()) {
                EquipmentType type = m.getType();
                if ((type instanceof MiscType)
                    && type.hasFlag(MiscType.F_ANGEL_ECM)
                    && (m.curMode().equals("ECCM")
                        || m.curMode().equals("ECM & ECCM") || m
                        .curMode().equals("ECCM & Ghost Targets"))) {
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
     * <code>Entity.NONE</code> if no ECM is active.
     */
    public int getECMRange() {
        // no ECM in space unless strat op option enabled
        if (game.getBoard().inSpace()
            && !game.getOptions().booleanOption("stratops_ecm")) {
            return Entity.NONE;
        }
        // If we have stealth up and running, there's no bubble.
        if (isStealthOn()) {
            return Entity.NONE;
        }

        if (!isShutDown()) {
            for (Mounted m : getMisc()) {
                EquipmentType type = m.getType();
                if ((type instanceof MiscType) && type.hasFlag(MiscType.F_ECM)
                    && !m.isInoperable()) {
                    if (type.hasFlag(MiscType.F_SINGLE_HEX_ECM)) {
                        return 0;
                    }
                    int toReturn = 6;
                    if (type.hasFlag(MiscType.F_ANGEL_ECM)
                        && (this instanceof BattleArmor)) {
                        toReturn = 2;
                    }
                    if (type.hasFlag(MiscType.F_EW_EQUIPMENT)
                        || type.hasFlag(MiscType.F_NOVA)
                        || type.hasFlag(MiscType.F_WATCHDOG)) {
                        toReturn = 3;
                    }
                    if (game.getPlanetaryConditions().hasEMI()) {
                        return toReturn * 2;
                    }
                    return toReturn;
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
        if (((game != null) && game.getPlanetaryConditions().hasEMI())
            || isShutDown()) {
            return false;
        }
        for (Mounted m : getMisc()) {
            EquipmentType type = m.getType();
            if ((type instanceof MiscType) && type.hasFlag(MiscType.F_BAP)) {

                if (!m.isInoperable()) {
                    // Beagle Isn't affected by normal ECM
                    if (type.getName().equals("Beagle Active Probe")) {

                        if ((game != null)
                            && checkECM
                            && ComputeECM.isAffectedByAngelECM(this,
                                                               getPosition(), getPosition())) {
                            return false;
                        }
                        return true;
                    }
                    return !checkECM
                           || (game == null)
                           || !ComputeECM.isAffectedByECM(this, getPosition(),
                                                          getPosition());
                }
            }
        }
        // check for Manei Domini implants
        if (((crew.getOptions().booleanOption("cyber_eye_im") || crew
                .getOptions().booleanOption("mm_eye_im"))
             && (this instanceof Infantry) && !(this instanceof BattleArmor))
            || (crew.getOptions().booleanOption("mm_eye_im") && (crew
                                                                         .getOptions().booleanOption("vdni") || crew
                                                                         .getOptions().booleanOption("bvdni")))) {
            return !checkECM
                   || !ComputeECM.isAffectedByECM(this, getPosition(),
                                                  getPosition());
        }
        // check for quirk
        if (hasQuirk(OptionsConstants.QUIRK_POS_IMPROVED_SENSORS)) {
            return !checkECM
                   || !ComputeECM.isAffectedByECM(this, getPosition(),
                                                  getPosition());
        }

        return false;
    }

    /**
     * What's the range of the BAP equipment?
     *
     * @return the <code>int</code> range of this unit's BAP. This value will be
     * <code>Entity.NONE</code> if no BAP is active.
     */
    public int getBAPRange() {
        if (game.getPlanetaryConditions().hasEMI() || isShutDown()) {
            return Entity.NONE;
        }
        // check for Manei Domini implants
        int cyberBonus = 0;
        if (((crew.getOptions().booleanOption("cyber_eye_im") || crew
                .getOptions().booleanOption("mm_eye_im"))
             && (this instanceof Infantry) && !(this instanceof BattleArmor))
            || (crew.getOptions().booleanOption("mm_eye_im") && (crew
                                                                         .getOptions().booleanOption("vdni") || crew
                                                                         .getOptions().booleanOption("bvdni")))) {
            cyberBonus = 1;
        }

        // check for quirks
        // TODO: assuming the range of this active probe is 2
        // http://www.classicbattletech.com/forums/index.php/topic,52961.new.html#new
        int quirkBonus = 0;
        if (hasQuirk(OptionsConstants.QUIRK_POS_IMPROVED_SENSORS)) {
            quirkBonus = 2;
        }

        for (Mounted m : getMisc()) {
            EquipmentType type = m.getType();
            if ((type instanceof MiscType) && type.hasFlag(MiscType.F_BAP)
                && !m.isInoperable()) {
                // System.err.println("BAP type name: "+m.getName()+"
                // internalName: "+((MiscType)m.getType()).internalName);
                // in space the range of all BAPs is given by the mode
                if (game.getBoard().inSpace()) {
                    if (m.curMode().equals("Medium")) {
                        return 12;
                    }
                    return 6;
                }

                if (m.getName().equals("Bloodhound Active Probe (THB)")
                    || m.getName().equals(Sensor.BAP)) {
                    return 8 + cyberBonus + quirkBonus;
                }
                if ((m.getType()).getInternalName().equals(Sensor.CLAN_AP)
                    || (m.getType()).getInternalName().equals(
                        Sensor.WATCHDOG)
                    || (m.getType()).getInternalName().equals(Sensor.NOVA)) {
                    return 5 + cyberBonus + quirkBonus;
                }
                if ((m.getType()).getInternalName().equals(Sensor.LIGHT_AP)
                    || (m.getType().getInternalName()
                         .equals(Sensor.CLBALIGHT_AP))
                    || (m.getType().getInternalName()
                         .equals(Sensor.ISBALIGHT_AP))) {
                    return 3 + cyberBonus + quirkBonus;
                }
                if (m.getType().getInternalName().equals(Sensor.ISIMPROVED)
                    || (m.getType().getInternalName()
                         .equals(Sensor.CLIMPROVED))) {
                    return 2 + cyberBonus + quirkBonus;
                }
                return 4 + cyberBonus + quirkBonus;// everthing else should be
                // range 4
            }
        }
        if ((cyberBonus + quirkBonus) > 0) {
            return cyberBonus + quirkBonus;
        }

        return Entity.NONE;
    }

    /**
     * Returns wether or not this entity has a Targeting Computer.
     */
    public boolean hasTargComp() {
        for (Mounted m : getMisc()) {
            if ((m.getType() instanceof MiscType)
                && m.getType().hasFlag(MiscType.F_TARGCOMP)) {
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
            if ((m.getType() instanceof MiscType)
                && m.getType().hasFlag(MiscType.F_TARGCOMP)
                && m.curMode().equals("Aimed shot")) {
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
            if ((m.getType() instanceof MiscType)
                && (m.getType().hasFlag(MiscType.F_C3S) || m.getType()
                                                            .hasFlag(MiscType.F_C3SBS)) && !m.isInoperable()) {
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
     * Does this entity have an undamaged HarJel system in this location?
     * (Type-dependent, defaults to false.)
     * Does not include Harjel II or Harjel III, as they do not prevent breach
     * checks like Harjel does.
     *
     * @param location the <code>int</code> location to check
     * @return a <code>boolean</code> value indicating a present HarJel system
     */
    public boolean hasHarJelIn(int location) {
        for (Mounted mounted : getMisc()) {
            if ((mounted.getLocation() == location)
                && mounted.isReady()
                && (mounted.getType().hasFlag(MiscType.F_HARJEL))) {
                return true;
            }
        }
        return false;
    }

    public boolean hasBoostedC3() {
        if (isShutDown() || isOffBoard()) {
            return false;
        }
        for (Mounted m : getEquipment()) {
            if ((m.getType().hasFlag(MiscType.F_C3SBS) || m.getType().hasFlag(
                    WeaponType.F_C3MBS))
                && !m.isInoperable()) {
                return true;
            }
        }
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
            if ((m.getType() instanceof WeaponType)
                && (m.getType().hasFlag(WeaponType.F_C3M) || m.getType()
                                                              .hasFlag(WeaponType.F_C3MBS)) && !m.isInoperable()) {
                // If this unit is configured as a company commander,
                // and if this computer is the company master, then
                // this unit does not have a lance master computer.
                if (C3MasterIs(this)
                    && (c3CompanyMasterIndex == getEquipmentNum(m))) {
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
                if ((m.getType() instanceof WeaponType)
                    && (m.getType().hasFlag(WeaponType.F_C3M) || m
                        .getType().hasFlag(WeaponType.F_C3MBS))
                    && !m.isInoperable()) {
                    // Now look for the company command master.
                    while ((c3CompanyMasterIndex == LOC_DESTROYED)
                           && e.hasNext()) {
                        m = e.next();
                        if ((m.getType() instanceof WeaponType)
                            && (m.getType().hasFlag(WeaponType.F_C3M) || m
                                .getType().hasFlag(WeaponType.F_C3MBS))
                            && !m.isInoperable()) {
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

    /**
     * Checks if we have nova CEWS that is not offline.
     *
     * @return
     */
    public boolean hasActiveNovaCEWS() {
        if (isShutDown() || isOffBoard()) {
            return false;
        }
        for (Mounted m : getEquipment()) {
            if ((m.getType() instanceof MiscType)
                && m.getType().hasFlag(MiscType.F_NOVA)
                && !m.isInoperable() && !m.curMode().equals("Off")) {
                return true;
            }
        }
        return false;
    }

    public boolean hasNovaCEWS() {
        for (Mounted m : getEquipment()) {
            if ((m.getType() instanceof MiscType)
                && m.getType().hasFlag(MiscType.F_NOVA)
                && !m.isInoperable()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasC3i() {
        if (isShutDown() || isOffBoard()) {
            return false;
        }
        for (Mounted m : getEquipment()) {
            if ((m.getType() instanceof MiscType)
                && m.getType().hasFlag(MiscType.F_C3I) && !m.isInoperable()) {
                return true;
            }
        }
        // check for Manei Domini implants
        if ((this instanceof Infantry) && (null != crew)
            && crew.getOptions().booleanOption("mm_eye_im")
            && crew.getOptions().booleanOption("boost_comm_implant")) {
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
            } else if (hasActiveNovaCEWS()) {
                c3NetIdString = "C3Nova." + getId();
            }
        }
        return c3NetIdString;
    }

    public String getOriginalNovaC3NetId() {
        return "C3Nova." + getId();
    }

    /**
     * Switches the C3 network Id to the new network ID.
     */
    public void newRoundNovaNetSwitch() {
        if (hasNovaCEWS()) {
            // FIXME: no check for network limit of 3 units
            c3NetIdString = newC3NetIdString;
        }
    }

    /**
     * Set the C3 network ID to be used on the next turn. Used for reconfiguring
     * a C3 network with Nova CEWS.
     *
     * @param str
     */
    public void setNewRoundNovaNetworkString(String str) {
        // Only allow Nova CEWS to change
        if (hasNovaCEWS()) {
            newC3NetIdString = str;
        } else {
            newC3NetIdString = getOriginalNovaC3NetId();
        }
    }

    /**
     * Returns the C3 network id that will be switched to on the next turn.
     *
     * @return
     */
    public String getNewRoundNovaNetworkString() {
        if ((newC3NetIdString == null) || (newC3NetIdString == "")) {
            newC3NetIdString = getOriginalNovaC3NetId();
        }
        return newC3NetIdString;
    }

    public void setC3NetId(Entity e) {
        if ((e == null) || isEnemyOf(e)) {
            return;
        }
        c3NetIdString = e.c3NetIdString;
    }

    public void setC3NetIdSelf() {
        if (hasActiveNovaCEWS()) {
            c3NetIdString = "C3Nova." + getId();
        } else {
            c3NetIdString = "C3i." + getId();
        }
    }

    /**
     * Determine the remaining number of other C3 Master computers that can
     * connect to this <code>Entity</code>.
     * <p/>
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
                for (Entity e : game.getEntitiesVector()) {
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
                for (Entity e : game.getEntitiesVector()) {
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
     * <p/>
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
                for (Entity e : game.getEntitiesVector()) {
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
                for (Entity e : game.getEntitiesVector()) {
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
        } else if (hasActiveNovaCEWS()) {
            nodes = 2;
            if (game != null) {
                for (Entity e : game.getEntitiesVector()) {
                    if (!equals(e) && onSameC3NetworkAs(e)) {
                        nodes--;
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
     * itself, if none is above this
     */
    public Entity getC3Top() {
        Entity m = this;
        Entity master = m.getC3Master();
        while ((master != null)
               && !master.equals(m)
               && master.hasC3()
               && ((m.hasBoostedC3() && !ComputeECM.isAffectedByAngelECM(m,
                                                                         m.getPosition(),
                                                                         master.getPosition())) || !(ComputeECM
                                                                                                             .isAffectedByECM(m, m.getPosition(),
                                                                                                                              master.getPosition())))
               && ((master.hasBoostedC3() && !ComputeECM.isAffectedByAngelECM(
                master, master.getPosition(), master.getPosition())) || !(ComputeECM
                                                                                  .isAffectedByECM(master, master.getPosition(),
                                                                                                   master.getPosition())))) {
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
     * network. This value may be <code>null</code>. If the value master
     * unit has shut down, then the value will be non-<code>null</code>
     * after the master unit restarts.
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
            else if (((eMaster.c3CompanyMasterIndex > LOC_NONE) && !eMaster
                    .hasC3MM())
                     || ((eMaster.c3CompanyMasterIndex <= LOC_NONE) && !eMaster
                    .hasC3M())) {
                c3Master = NONE;
            }
            // maximum depth of a c3 network is 2 levels.
            else if (eMaster != this) {
                Entity eCompanyMaster = eMaster.getC3Master();
                if ((eCompanyMaster != null)
                    && (eCompanyMaster.getC3Master() != eCompanyMaster)) {
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
     * unit's C3 network, or <code>Entity.NONE</code>.
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
     * <p/>
     * Please note, that when an <code>Entity</code> is it's own C3 Master, then
     * it is a company commander.
     * <p/>
     * Also note that when <code>null</code> is the master for this
     * <code>Entity</code>, then it is an independent master.
     *
     * @param e - the <code>Entity</code> that may be this unit's C3 Master.
     * @return a <code>boolean</code> that is <code>true</code> when the passed
     * <code>Entity</code> is this unit's commander. If the passed unit
     * isn't this unit's commander, this routine returns
     * <code>false</code>.
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
     * @param e - the <code>Entity</code> that should be set as our C3 Master.
     */
    public void setC3Master(Entity e, boolean reset) {
        if (e == null) {
            setC3Master(NONE, reset);
        } else {
            if (isEnemyOf(e)) {
                return;
            }
            setC3Master(e.id, reset);
        }
    }

    /**
     * @param entityId
     */
    public void setC3Master(int entityId, boolean reset) {
        if (reset && ((id == entityId) != (id == c3Master))) {
            // this just changed from a company-level to lance-level (or vice
            // versa); have to disconnect all slaved units to maintain
            // integrity.
            for (Entity e : game.getEntitiesVector()) {
                if (e.C3MasterIs(this) && !equals(e)) {
                    e.setC3Master(NONE, reset);
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
        for (Entity e : game.getEntitiesVector()) {
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
     * @param e         The <code>Entity</code> to check against this entity
     * @param ignoreECM a <code>boolean</code> indicating if ECM should be ignored, we
     *                  need this for c3i
     * @return a <code>boolean</code> that is <code>true</code> if the given
     * entity is on the same network, <code>false</code> if not.
     */
    public boolean onSameC3NetworkAs(Entity e, boolean ignoreECM) {
        if (isEnemyOf(e) || isShutDown() || e.isShutDown()) {
            return false;
        }

        // Active Mek Stealth prevents entity from participating in C3.
        // Turn off the stealth, and your back in the network.
        if (((this instanceof Mech) || (this instanceof Tank))
            && isStealthActive()) {
            return false;
        }
        if (((e instanceof Mech) || (e instanceof Tank)) && e.isStealthActive()) {
            return false;
        }

        // C3i is easy - if they both have C3i, and their net ID's match,
        // they're on the same network!
        if (hasC3i() && e.hasC3i() && getC3NetId().equals(e.getC3NetId())) {
            if (ignoreECM) {
                return true;
            }
            return !(ComputeECM.isAffectedByECM(e, e.getPosition(),
                                                e.getPosition()))
                   && !(ComputeECM.isAffectedByECM(this, getPosition(),
                                                   getPosition()));
        }

        // Nova is easy - if they both have Nova, and their net ID's match,
        // they're on the same network!
        // At least I hope thats how it works.
        if (hasActiveNovaCEWS() && e.hasActiveNovaCEWS()
            && getC3NetId().equals(e.getC3NetId())) {
            if (ignoreECM) {
                return true;
            }
            ECMInfo srcInfo = ComputeECM.getECMEffects(e, e.getPosition(),
                    e.getPosition(), true, null);
            ECMInfo dstInfo = ComputeECM.getECMEffects(this, getPosition(),
                    getPosition(), true, null);
            return !((srcInfo != null) && srcInfo.isNovaECM()) 
                    && !((dstInfo != null) && dstInfo.isNovaECM());
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
            if ((mounted.getLocation() == loc)
                && mounted.getType().hasFlag(MiscType.F_CASE)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether there is CASE anywhere on this {@code Entity}.
     */
    public boolean hasCase() {
        // Clan Mechs always have CASE!
        if (isClan()) {
            return true;
        }
        for (Mounted mounted : getMisc()) {
            if (mounted.getType().hasFlag(MiscType.F_CASE)) {
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
            CriticalSlot cs = getCritical(loc, i);
            if ((cs != null) && (cs.getType() == orig.getType())) {
                Mounted csMount = cs.getMount();
                if ((csMount != null) && csMount.equals(orig.getMount())) {
                    cs.setHit(true);
                }
            }
        }
    }

    /**
     * Start a new round
     *
     * @param roundNumber the <code>int</code> number of the new round
     */
    public void newRound(int roundNumber) {
        fell = false;
        struck = false;
        unloadedThisTurn = false;
        loadedThisTurn = false;
        done = false;
        delta_distance = 0;
        mpUsedLastRound = mpUsed;
        mpUsed = 0;
        isJumpingNow = false;
        damageThisRound = 0;
        if (assaultDropInProgress == 2) {
            assaultDropInProgress = 0;
        }
        moved = EntityMovementType.MOVE_NONE;
        movedBackwards = false;
        isPowerReverse = false;
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

        // update the number of turns we used a blue shield
        if (hasActiveBlueShield()) {
            blueShieldRounds++;
        }

        // for dropping troops, check to see if they are going to land
        // this turn, if so, then set their assault drop status to true
        if (isAirborne()
            && !(this instanceof Aero)
            && (getAltitude() <= game.getPlanetaryConditions()
                                     .getDropRate())) {
            setAssaultDropInProgress(true);
        }

        for (Mounted m : getEquipment()) {
            m.newRound(roundNumber);
        }

        newRoundNovaNetSwitch();
        doNewRoundIMP();

        // reset hexes passed through
        setPassedThrough(new Vector<Coords>());
        setPassedThroughFacing(new ArrayList<Integer>());
        if (playerPickedPassThrough == null) {
            playerPickedPassThrough = new HashMap<>();
        } else {
            playerPickedPassThrough.clear();
        }

        resetFiringArcs();

        resetBays();

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

        // update fatigue count
        if ((null != crew) && isDeployed()) {
            crew.incrementFatigueCount();
        }

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
                taserInterferenceHeat = false;
            }
        }
        if (taserFeedBackRounds > 0) {
            taserFeedBackRounds--;
        }

        // If we are affected by the TSEMP Shutdown effect, we should remove
        // it now, so we can startup during the end phase
        if (getTsempEffect() == TSEMPWeapon.TSEMP_EFFECT_SHUTDOWN) {
            setTsempEffect(TSEMPWeapon.TSEMP_EFFECT_NONE);
            // The TSEMP interference effect shouldn't be removed until the start
            //  of a round where we didn't have any TSEMP hits and didn't fire a
            //  TSEMP, since we need the effect active during the firing phase
        } else if ((getTsempHitsThisTurn() == 0) && !isFiredTsempThisTurn()) {
            setTsempEffect(TSEMPWeapon.TSEMP_EFFECT_NONE);
        }

        // TSEMPs can fire every other round, so if we didn't fire last
        //  round and the TSEMP isn't one-shot, reset it's fired state
        if (hasFiredTsemp()) {
            for (Mounted m : getWeaponList()) {
                if (m.getType().hasFlag(WeaponType.F_TSEMP)
                    && !m.getType().hasFlag(WeaponType.F_ONESHOT)) {
                    if (m.isTSEMPDowntime()) {
                        m.setFired(false);
                        m.setTSEMPDowntime(false);
                    } else if (m.isFired()) {
                        m.setTSEMPDowntime(true);
                    }
                }
            }
        }

        // Reset TSEMP hits
        tsempHitsThisTurn = 0;
        // Reset TSEMP firing flag
        setFiredTsempThisTurn(false);

        // Decrement the number of consecutive turns if not used last turn
        if (!hasActivatedRadicalHS()) {
            setConsecutiveRHSUses(Math.max(0, getConsecutiveRHSUses() - 1));
        }
        // Reset used RHS flag
        deactivateRadicalHS();

        clearAttackedByThisTurn();
    }

    /**
     * Applies any damage that the entity has suffered. When anything gets hit
     * it is simply marked as "hit" but does not stop working until this is
     * called.
     */
    public void applyDamage() {
        // mark all damaged equipment destroyed
        for (Mounted mounted : getEquipment()) {
            if (mounted.isHit()) {
                mounted.setDestroyed(true);
            }
        }

        // destroy criticals that were hit last phase
        for (int i = 0; i < locations(); i++) {
            for (int j = 0; j < getNumberOfCriticals(i); j++) {
                final CriticalSlot cs = getCritical(i, j);
                if ((cs != null) && cs.isHit()) {
                    cs.setDestroyed(true);
                }
            }
        }

        // Reset any "blown off this phase" markers.
        for (int i = 0; i < locations(); i++) {
            setLocationBlownOffThisPhase(i, false);
        }

        // destroy armor/internals if the section was removed
        for (int i = 0; i < locations(); i++) {
            if (getInternal(i) == IArmorState.ARMOR_DOOMED) {
                setArmor(IArmorState.ARMOR_DESTROYED, i);
                setArmor(IArmorState.ARMOR_DESTROYED, i, true);
                setInternal(IArmorState.ARMOR_DESTROYED, i);
                // destroy any Narc beacons
                for (Iterator<NarcPod> iter = narcPods.iterator(); iter
                        .hasNext(); ) {
                    NarcPod p = iter.next();
                    if (p.getLocation() == i) {
                        iter.remove();
                    }
                }
                for (Iterator<INarcPod> iter = iNarcPods.iterator(); iter
                        .hasNext(); ) {
                    INarcPod p = iter.next();
                    if (p.getLocation() == i) {
                        iter.remove();
                    }
                }
                for (Iterator<NarcPod> iter = pendingNarcPods.iterator(); iter
                        .hasNext(); ) {
                    NarcPod p = iter.next();
                    if (p.getLocation() == i) {
                        iter.remove();
                    }
                }
                for (Iterator<INarcPod> iter = pendingINarcPods.iterator(); iter
                        .hasNext(); ) {
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
                if ((mounted.getLinked() == null)
                    || (mounted.getLinked().getUsableShotsLeft() <= 0)
                    || mounted.getLinked().isDumping()) {
                    loadWeaponWithSameAmmo(mounted);
                }
            }
        }
    }

    /**
     * Return the currently operable AMS mounted in this Entity.
     *
     * @return
     */
    public List<Mounted> getActiveAMS() {
        ArrayList<Mounted> ams = new ArrayList<>();
        for (Mounted weapon : getWeaponList()) {
            // Skip anything that's not AMS
            if (!weapon.getType().hasFlag(WeaponType.F_AMS)) {
                continue;
            }

            // Make sure the AMS is good to go
            if (!weapon.isReady() || weapon.isMissing()
                || weapon.curMode().equals("Off")) {
                continue;
            }

            // AMS blocked by transported units can not fire
            if (isWeaponBlockedAt(weapon.getLocation(),
                                  weapon.isRearMounted())) {
                continue;
            }

            // Make sure ammo is loaded
            boolean baAPDS = (this instanceof BattleArmor)
                    && (weapon.getType().getInternalName().equals("ISBAAPDS"));
            Mounted ammo = weapon.getLinked();
            if (!(weapon.getType().hasFlag(WeaponType.F_ENERGY)) && !baAPDS
                && ((ammo == null) || (ammo.getUsableShotsLeft() == 0)
                    || ammo.isDumping())) {
                loadWeapon(weapon);
                ammo = weapon.getLinked();
            }

            // try again
            if (!(weapon.getType().hasFlag(WeaponType.F_ENERGY)) && !baAPDS
                && ((ammo == null) || (ammo.getUsableShotsLeft() == 0)
                    || ammo.isDumping())) {
                // No ammo for this AMS.
                continue;
            }
            ams.add(weapon);
        }
        return ams;
    }

    /**
     * Assign AMS systems to the most dangerous incoming missile attacks. This
     * should only be called once per turn, or AMS will get extra attacks
     */
    public void assignAMS(Vector<WeaponHandler> vAttacks) {

        HashSet<WeaponAttackAction> targets = new HashSet<WeaponAttackAction>();
        for (Mounted ams : getActiveAMS()) {
            // Ignore APDS, it gets assigned elsewhere
            if (ams.isAPDS()) {
                continue;
            }
            // make a new vector of only incoming attacks in arc
            Vector<WeaponAttackAction> vAttacksInArc = new Vector<WeaponAttackAction>(
                    vAttacks.size());
            for (WeaponHandler wr : vAttacks) {
                if (!targets.contains(wr.waa)
                        && Compute.isInArc(game, getId(), getEquipmentNum(ams),
                                game.getEntity(wr.waa.getEntityId()))) {
                    vAttacksInArc.addElement(wr.waa);
                }
            }
            // find the most dangerous salvo by expected damage
            WeaponAttackAction waa = Compute.getHighestExpectedDamage(game,
                    vAttacksInArc, true);
            if (waa != null) {
                waa.addCounterEquipment(ams);
                targets.add(waa);
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
     * @param pod The <code>NarcPod</code> to be attached.
     */
    public void attachNarcPod(NarcPod pod) {
        pendingNarcPods.add(pod);
    }

    /**
     * attach an iNarcPod
     *
     * @param pod The <code>INarcPod</code> to be attached.
     */
    public void attachINarcPod(INarcPod pod) {
        pendingINarcPods.add(pod);
    }

    /**
     * Have we been iNarced with a homing pod from that team?
     *
     * @param nTeamID The id of the team that we are wondering about.
     * @return true if the Entity is narced by that team.
     */
    public boolean isINarcedBy(int nTeamID) {
        for (INarcPod pod : iNarcPods) {
            if ((pod.getTeam() == nTeamID)
                && (pod.getType() == INarcPod.HOMING)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Have we been iNarced with the named pod from any team?
     *
     * @param type the <code>int</code> type of iNarc pod.
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
     * @param pod the <code>INarcPod</code> to be removed.
     * @return <code>true</code> if the pod was removed, <code>false</code> if
     * the pod was not attached to this entity.
     */
    public boolean removeINarcPod(INarcPod pod) {
        return iNarcPods.remove(pod);
    }

    /**
     * Calculates the battle value of this entity
     */
    public abstract int calculateBattleValue();

    public boolean useGeometricMeanBV() {
        return useGeometricBV || ((game != null)
                                  && game.getOptions().booleanOption("geometric_mean_bv"));
    }

    public boolean useReducedOverheatModifierBV() {
        return (useReducedOverheatModifierBV || ((game != null)
                                                && game.getOptions().booleanOption("reduced_overheat_modifier_bv")));
    }

    /**
     * Calculates the battle value of this mech. If the parameter is true, then
     * the battle value for c3 will be added whether the mech is currently part
     * of a network or not. This should be overwritten if necessary
     *
     * @param ignoreC3    if the contribution of the C3 computer should be ignored when
     *                    calculating BV.
     * @param ignorePilot if the extra BV due to piloting skill should be ignore, needed
     *                    for c3 bv
     */
    public int calculateBattleValue(boolean ignoreC3, boolean ignorePilot) {
        if (useManualBV) {
            return manualBV;
        }
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
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }
        if((null == obj) || (getClass() != obj.getClass())) {
            return false;
        }
        final Entity other = (Entity) obj;
        return (id == other.id);
    }

    @Override
    public int hashCode() {
        return id;
    }

    /**
     * Get the movement mode of the entity
     */
    public EntityMovementMode getMovementMode() {
        return movementMode;
    }

    /**
     * Get the movement mode of the entity as a String.
     */
    public String getMovementModeAsString() {
        switch (getMovementMode()) {
            case NONE:
                return "None";
            case BIPED:
            case BIPED_SWIM:
                return "Biped";
            case QUAD:
            case QUAD_SWIM:
                return "Quad";
            case TRACKED:
                return "Tracked";
            case WHEELED:
                return "Wheeled";
            case HOVER:
                return "Hover";
            case VTOL:
                return "VTOL";
            case NAVAL:
                return "Naval";
            case HYDROFOIL:
                return "Hydrofoil";
            case SUBMARINE:
                return "Submarine";
            case INF_UMU:
                return "UMU";
            case INF_LEG:
                return "Leg";
            case INF_MOTORIZED:
                return "Motorized";
            case INF_JUMP:
                return "Jump";
            case WIGE:
                return "WiGE";
            case AERODYNE:
                return "Aerodyne";
            case SPHEROID:
                return "Spheroid";
            case RAIL:
                return "Rail";
            case MAGLEV:
                return "MagLev";
            default:
                return "ERROR";
        }
    }

    /**
     * Set the movement type of the entity
     */
    public void setMovementMode(EntityMovementMode movementMode) {
        this.movementMode = movementMode;
    }

    /**
     * Helper function to determine if a entity is a biped
     */
    public boolean entityIsBiped() {
        return (getMovementMode() == EntityMovementMode.BIPED);
    }

    /**
     * Helper function to determine if a entity is a quad
     */
    public boolean entityIsQuad() {
        return (getMovementMode() == EntityMovementMode.QUAD);
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
    public PilotingRollData getBasePilotingRoll(EntityMovementType moveType) {
        final int entityId = getId();

        PilotingRollData roll;

        // Crew dead?
        if (getCrew().isDead() || getCrew().isDoomed()
            || (getCrew().getHits() >= 6)) {
            // Following line switched from impossible to automatic failure
            // -- bug fix for dead units taking PSRs
            return new PilotingRollData(entityId, TargetRoll.AUTOMATIC_FAIL,
                                        "Pilot dead");
        }
        // pilot awake?
        else if (!getCrew().isActive()) {
            return new PilotingRollData(entityId, TargetRoll.IMPOSSIBLE,
                                        "Pilot unconscious");
        }
        // gyro operational?
        if ((getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO,
                             Mech.LOC_CT) > 1) && (getGyroType() != Mech.GYRO_HEAVY_DUTY)) {
            return new PilotingRollData(entityId, TargetRoll.AUTOMATIC_FAIL,
                                        getCrew().getPiloting() + 6, "Gyro destroyed");
        }

        // Takes 3+ hits to kill an HD Gyro.
        if ((getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO,
                             Mech.LOC_CT) > 2) && (getGyroType() == Mech.GYRO_HEAVY_DUTY)) {
            return new PilotingRollData(entityId, TargetRoll.AUTOMATIC_FAIL,
                                        getCrew().getPiloting() + 6, "Gyro destroyed");
        }

        // both legs present?
        if (this instanceof BipedMech) {
            if (((BipedMech) this).countBadLegs() == 2) {
                return new PilotingRollData(entityId,
                                            TargetRoll.AUTOMATIC_FAIL,
                                            getCrew().getPiloting() + 10, "Both legs destroyed");
            }
        } else if (this instanceof QuadMech) {
            if (((QuadMech) this).countBadLegs() >= 3) {
                return new PilotingRollData(entityId,
                                            TargetRoll.AUTOMATIC_FAIL, getCrew().getPiloting()
                                                                       + (((Mech) this).countBadLegs() * 5),
                                            ((Mech) this).countBadLegs() + " legs destroyed");
            }
        }
        // entity shut down?
        if (isShutDown() && isShutDownThisPhase()) {
            return new PilotingRollData(entityId, TargetRoll.AUTOMATIC_FAIL,
                                        getCrew().getPiloting() + 3, "Reactor shut down");
        } else if (isShutDown()) {
            return new PilotingRollData(entityId, TargetRoll.AUTOMATIC_FAIL,
                                        TargetRoll.IMPOSSIBLE, "Reactor shut down");
        }

        // okay, let's figure out the stuff then
        roll = new PilotingRollData(entityId, getCrew().getPiloting(),
                                    "Base piloting skill");

        // Let's see if we have a modifier to our piloting skill roll. We'll
        // pass in the roll
        // object and adjust as necessary
        roll = addEntityBonuses(roll);

        // add planetary condition modifiers
        roll = addConditionBonuses(roll, moveType);

        if (isCarefulStand() && ((getWalkMP() - mpUsed) > 2)) {
            roll.addModifier(-2, "careful stand");
        }

        if (hasQuirk(OptionsConstants.QUIRK_NEG_HARD_PILOT)) {
            roll.addModifier(+1, "hard to pilot");
        }

        if (getPartialRepairs() != null) {
            if (getPartialRepairs().booleanOption("mech_gyro_1_crit")) {
                roll.addModifier(+1, "Partial repair of Gyro (+1)");
            }
            if (getPartialRepairs().booleanOption("mech_gyro_2_crit")) {
                roll.addModifier(+1, "Partial repair of Gyro (+2)");
            }
        }

        if (game.getOptions().booleanOption("tacops_fatigue")
            && crew.isPilotingFatigued()) {
            roll.addModifier(1, "fatigue");
        }

        if (taserInterference > 0) {
            roll.addModifier(taserInterference, "taser interference");
        }

        if ((game.getPhase() == Phase.PHASE_MOVEMENT) && isPowerReverse()) {
            roll.addModifier(1, "power reverse");
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
    public PilotingRollData addConditionBonuses(PilotingRollData roll,
                                                EntityMovementType moveType) {

        if (moveType == EntityMovementType.MOVE_SPRINT) {
            roll.addModifier(2, "Sprinting");
        }

        PlanetaryConditions conditions = game.getPlanetaryConditions();
        // check light conditions for "running" entities
        if ((moveType == EntityMovementType.MOVE_RUN)
            || (moveType == EntityMovementType.MOVE_SPRINT)
            || (moveType == EntityMovementType.MOVE_VTOL_RUN)
            || (moveType == EntityMovementType.MOVE_OVER_THRUST)) {
            int lightPenalty = conditions.getLightPilotPenalty();
            if (lightPenalty > 0) {
                roll.addModifier(lightPenalty,
                        conditions.getLightDisplayableName());
            }
        }

        // check weather conditions for all entities
        int weatherMod = conditions.getWeatherPilotPenalty();
        if ((weatherMod != 0)
            && !game.getBoard().inSpace()
            && ((null == crew) || !crew.getOptions().booleanOption(
                "allweather"))) {
            roll.addModifier(weatherMod, conditions.getWeatherDisplayableName());
        }

        // check wind conditions for all entities
        int windMod = conditions.getWindPilotPenalty(this);
        if ((windMod != 0)
            && !game.getBoard().inSpace()
            && ((null == crew) || !crew.getOptions().booleanOption(
                "allweather"))) {
            roll.addModifier(windMod, conditions.getWindDisplayableName());
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
    public PilotingRollData checkGetUp(MoveStep step,
            EntityMovementType moveType) {

        if ((step == null)
            || ((step.getType() != MoveStepType.GET_UP) 
                    && (step.getType() != MoveStepType.CAREFUL_STAND))) {
            return new PilotingRollData(id, TargetRoll.CHECK_FALSE,
                    "Check false: Entity is not attempting to get up.");
        }

        PilotingRollData roll = getBasePilotingRoll(moveType);

        if (this instanceof BipedMech) {
            if ((((Mech) this).countBadLegs() >= 1)
                    && (isLocationBad(Mech.LOC_LARM) 
                            && isLocationBad(Mech.LOC_RARM))) {
                roll.addModifier(TargetRoll.IMPOSSIBLE,
                        "can't get up with destroyed leg and arms");
                return roll;
            }
        }

        if (isHullDown() && (this instanceof QuadMech)) {
            roll.addModifier(TargetRoll.AUTOMATIC_SUCCESS,
                    "getting up from hull down");
            return roll;
        }

        if (!needsRollToStand()
                && (getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO,
                        Mech.LOC_CT) < 2)) {
            roll.addModifier(TargetRoll.AUTOMATIC_SUCCESS, "\n"
                    + getDisplayName()
                    + " does not need to make a piloting skill check "
                    + "to stand up because it has all four of " + "its legs.");
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
    public PilotingRollData checkRunningWithDamage(
            EntityMovementType overallMoveType) {
        PilotingRollData roll = getBasePilotingRoll(overallMoveType);

        int gyroDamage = getBadCriticals(CriticalSlot.TYPE_SYSTEM,
                                         Mech.SYSTEM_GYRO, Mech.LOC_CT);
        if (getGyroType() == Mech.GYRO_HEAVY_DUTY) {
            gyroDamage--; // HD gyro ignores 1st damage
        }
        if (((overallMoveType == EntityMovementType.MOVE_RUN) || (overallMoveType == EntityMovementType.MOVE_SPRINT))
            && !isProne() && ((gyroDamage > 0) || hasHipCrit())) {
            // append the reason modifier
            roll.append(new PilotingRollData(getId(), 0,
                                             "running with damaged hip actuator or gyro"));
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE,
                             "Check false: Entity is not attempting to run with damage");
        }
        addPilotingModifierForTerrain(roll);
        return roll;
    }

    /**
     * Checks if the entity is attempting to sprint with MASC engaged. If so,
     * returns the target roll for the piloting skill check.
     */
    public PilotingRollData checkSprintingWithMASC(
            EntityMovementType overallMoveType, int used) {
        PilotingRollData roll = getBasePilotingRoll(overallMoveType);

        if ((overallMoveType == EntityMovementType.MOVE_SPRINT)
            && (used > ((int) Math.ceil(2.0 * this.getWalkMP())))) {
            roll.append(new PilotingRollData(getId(), 0,
                                             "sprinting with active MASC/Supercharger"));
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE,
                             "Check false: Entity is not attempting to sprint with MASC");
        }

        addPilotingModifierForTerrain(roll);
        return roll;
    }

    /**
     * Checks if the entity is attempting to sprint with supercharger engaged.
     * If so, returns the target roll for the piloting skill check.
     */
    public PilotingRollData checkSprintingWithSupercharger(
            EntityMovementType overallMoveType, int used) {
        PilotingRollData roll = getBasePilotingRoll(overallMoveType);

        if ((overallMoveType == EntityMovementType.MOVE_SPRINT)
            && (used > ((int) Math.ceil(2.5 * this.getWalkMP())))) {
            roll.append(new PilotingRollData(getId(), 0,
                                             "sprinting with active MASC/Supercharger"));
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE,
                             "Check false: Entity is not attempting to sprint with Supercharger");
        }

        addPilotingModifierForTerrain(roll);
        return roll;
    }

    /**
     * Checks if an entity is passing through certain terrain while not moving
     * carefully
     */
    public PilotingRollData checkRecklessMove(MoveStep step,
            EntityMovementType moveType, IHex curHex, Coords lastPos,
            Coords curPos, IHex prevHex) {
        PilotingRollData roll = getBasePilotingRoll(moveType);
        // no need to go further if movement is careful
        if (step.isCareful()) {
            roll.addModifier(TargetRoll.CHECK_FALSE, "moving carefully");
            return roll;
        }

        // this only applies in fog, night conditions, or if a hex along the
        // move path has ice
        boolean isFoggy = game.getPlanetaryConditions().getFog() != PlanetaryConditions.FOG_NONE;
        boolean isDark = game.getPlanetaryConditions().getLight() > PlanetaryConditions.L_DUSK;

        // if we are jumping, then no worries
        if (moveType == EntityMovementType.MOVE_JUMP) {
            roll.addModifier(TargetRoll.CHECK_FALSE, "jumping is not reckless?");
            return roll;
        }

        // we need to make this check on the first move forward and anytime the
        // hex is not clear or is a level change
        if ((isFoggy || isDark) && !lastPos.equals(curPos)
            && lastPos.equals(step.getEntity().getPosition())) {
            roll.append(new PilotingRollData(getId(), 0, "moving recklessly"));
        }
        // FIXME: no perfect solution in the current code to determine if hex is
        // clear. I will use movement costs
        else if ((isFoggy || isDark)
                 && !lastPos.equals(curPos)
                 && ((curHex.movementCost(this) > 0) || ((null != prevHex) && (prevHex
                                                                                       .getLevel() != curHex
                                                                                       .getLevel())))) {
            roll.append(new PilotingRollData(getId(), 0, "moving recklessly"));
            // ice conditions
        } else if (curHex.containsTerrain(Terrains.ICE)) {
            roll.append(new PilotingRollData(getId(), 4, "moving recklessly"));
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE, "not moving recklessly");
        }

        adjustDifficultTerrainPSRModifier(roll);

        return roll;
    }

    /**
     * Checks if the entity is landing (from a jump) with damage that would
     * force a PSR. If so, returns the target roll for the piloting skill check.
     */
    public PilotingRollData checkLandingWithDamage(
            EntityMovementType overallMoveType) {
        PilotingRollData roll = getBasePilotingRoll(overallMoveType);

        if (((getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO,
                              Mech.LOC_CT) > 0) && (getGyroType() != Mech.GYRO_HEAVY_DUTY))
            || ((getBadCriticals(CriticalSlot.TYPE_SYSTEM,
                                 Mech.SYSTEM_GYRO, Mech.LOC_CT) > 1) && (getGyroType() == Mech.GYRO_HEAVY_DUTY))
            || hasLegActuatorCrit()) {
            // append the reason modifier
            roll.append(new PilotingRollData(getId(), 0,
                                             "landing with damaged leg actuator or gyro"));
            addPilotingModifierForTerrain(roll);
        } else {
            roll.addModifier(
                    TargetRoll.CHECK_FALSE,
                    "Entity does not have gyro or leg actuator damage -- checking for purposes of determining PSR " +
                    "after jump.");
        }
        return roll;
    }

    /**
     * Checks if the entity is landing (from a jump) with a prototype JJ If so,
     * returns the target roll for the piloting skill check.
     */
    public PilotingRollData checkLandingWithPrototypeJJ(
            EntityMovementType overallMoveType) {
        PilotingRollData roll = getBasePilotingRoll(overallMoveType);

        if (getJumpType() == Mech.JUMP_PROTOTYPE) {
            // append the reason modifier
            roll.append(new PilotingRollData(getId(), 3,
                                             "landing with prototype jump jets"));
            addPilotingModifierForTerrain(roll);
        } else {
            roll.addModifier(
                    TargetRoll.CHECK_FALSE,
                    "Entity does not have protype jump jets -- checking for purposes of determining PSR after jump.");
        }
        return roll;
    }

    /**
     * Checks if an entity is landing (from a jump) in heavy woods.
     */
    public PilotingRollData checkLandingInHeavyWoods(
            EntityMovementType overallMoveType, IHex curHex) {
        PilotingRollData roll = getBasePilotingRoll(overallMoveType);
        if (curHex.containsTerrain(Terrains.WOODS, 2)) {
            roll.append(new PilotingRollData(getId(), 0,
                                             "landing in heavy woods"));
            addPilotingModifierForTerrain(roll);
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE,
                             "hex does not contain heavy woods");
        }
        return roll;
    }

    /**
     * Checks if the entity is landing (from a jump) on ice-covered water.
     */
    public PilotingRollData checkLandingOnIce(
            EntityMovementType overallMoveType, IHex curHex) {
        PilotingRollData roll = getBasePilotingRoll(overallMoveType);

        if (curHex.containsTerrain(Terrains.ICE)
            && (curHex.terrainLevel(Terrains.WATER) > 0)) {
            roll.append(new PilotingRollData(getId(), 0,
                                             "landing on ice-covered water"));
            addPilotingModifierForTerrain(roll);
            adjustDifficultTerrainPSRModifier(roll);
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE,
                             "hex is not covered by ice");
        }

        return roll;
    }

    /**
     * return a <code>PilotingRollData</code> checking for wether this Entity
     * moved too fast due to low gravity
     *
     * @param step
     * @return
     */
    public PilotingRollData checkMovedTooFast(MoveStep step,
            EntityMovementType moveType) {
        PilotingRollData roll = getBasePilotingRoll(moveType);
        addPilotingModifierForTerrain(roll, step);
        switch (moveType) {
            case MOVE_WALK:
            case MOVE_RUN:
            case MOVE_VTOL_WALK:
            case MOVE_VTOL_RUN:
                int maxSafeMP = (int) Math.ceil(getOriginalWalkMP() * 1.5);
                if ((this instanceof Tank) && gotPavementBonus) {
                    maxSafeMP++;
                }
                if (step.getMpUsed() > maxSafeMP) {
                    roll.append(new PilotingRollData(getId(), 0,
                            "used more MPs than at 1G possible"));
                } else {
                    roll.addModifier(TargetRoll.CHECK_FALSE,
                            "Check false: Entity did not use more "
                            + "MPs walking/running than possible at 1G");
                }
                break;
            case MOVE_JUMP:
                if (step.getMpUsed() > getJumpMP(false)) {
                    roll.append(new PilotingRollData(getId(), 0,
                            "used more MPs than at 1G possible"));
                } else {
                    roll.addModifier(TargetRoll.CHECK_FALSE,
                            "Check false: Entity did not use more "
                            + "MPs jumping than possible at 1G");
                }
                break;
            default:
        }
        return roll;
    }

    /**
     * Checks if the entity might skid on pavement. If so, returns the target
     * roll for the piloting skill check.
     */
    public PilotingRollData checkSkid(EntityMovementType moveType,
            IHex prevHex, EntityMovementType overallMoveType,
            MoveStep prevStep, int prevFacing, int curFacing, Coords lastPos,
            Coords curPos, boolean isInfantry, int distance) {

        PilotingRollData roll = getBasePilotingRoll(overallMoveType);
        addPilotingModifierForTerrain(roll, lastPos);

        if (isAirborne() || isAirborneVTOLorWIGE()) {
            roll.addModifier(TargetRoll.CHECK_FALSE,
                             "Check false: flyinge entities don't skid");
            return roll;
        }

        if (isInfantry) {
            roll.addModifier(TargetRoll.CHECK_FALSE,
                             "Check false: infantry don't skid");
            return roll;
        }

        if (moveType == EntityMovementType.MOVE_JUMP) {
            roll.addModifier(TargetRoll.CHECK_FALSE,
                             "Check false: jumping entities don't skid");
            return roll;
        }

        if ((null != prevStep) && prevStep.isHasJustStood()) {
            roll.addModifier(TargetRoll.CHECK_FALSE,
                             "Check false: getting up entities don't skid");
            return roll;
        }

        /*
         * IHex curHex = null; if (null != curPos) { curHex =
         * game.getBoard().getHex(curPos); }
         */

        // TODO: add check for elevation of pavement, road,
        // or bridge matches entity elevation.
        if ((prevHex != null)
            && prevHex.containsTerrain(Terrains.ICE)
            && (((movementMode != EntityMovementMode.HOVER) && (movementMode != EntityMovementMode.WIGE)) || ((
                                                                                                                      (movementMode == EntityMovementMode.HOVER) || (movementMode == EntityMovementMode.WIGE)) && ((game
                                                                                                                                                                                                                            .getPlanetaryConditions().getWeather() == PlanetaryConditions.WE_HEAVY_SNOW) || (game
                                                                                                                                                                                                                                                                                                                     .getPlanetaryConditions().getWindStrength() >= PlanetaryConditions.WI_STORM))))
            && (prevFacing != curFacing) && !lastPos.equals(curPos)) {
            roll.append(new PilotingRollData(getId(),
                                             getMovementBeforeSkidPSRModifier(distance),
                                             "turning on ice"));
            adjustDifficultTerrainPSRModifier(roll);
            return roll;
        } else if ((prevHex != null)
                   && (prevStep.isPavementStep()
                       && ((overallMoveType == EntityMovementType.MOVE_RUN) || (overallMoveType == EntityMovementType
                .MOVE_SPRINT))
                       && (movementMode != EntityMovementMode.HOVER) && (movementMode != EntityMovementMode.WIGE))
                   && (prevFacing != curFacing) && !lastPos.equals(curPos)) {
            if (this instanceof Mech) {
                roll.append(new PilotingRollData(getId(),
                                                 getMovementBeforeSkidPSRModifier(distance),
                                                 "running & turning on pavement"));
            } else {
                roll.append(new PilotingRollData(getId(),
                                                 getMovementBeforeSkidPSRModifier(distance),
                                                 "reckless driving on pavement"));
            }
            adjustDifficultTerrainPSRModifier(roll);
            return roll;
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE,
                             "Check false: Entity is not apparently skidding");
            return roll;
        }
    }

    /**
     * Checks if the entity is moving into rubble. If so, returns the target
     * roll for the piloting skill check.
     */
    public PilotingRollData checkRubbleMove(MoveStep step,
            EntityMovementType moveType, IHex curHex, Coords lastPos,
            Coords curPos, boolean isLastStep) {
        PilotingRollData roll = getBasePilotingRoll(moveType);
        boolean enteringRubble = true;
        addPilotingModifierForTerrain(roll, curPos, enteringRubble);

        if (!lastPos.equals(curPos)
            && ((moveType != EntityMovementType.MOVE_JUMP)
                || isLastStep)
            && (curHex.terrainLevel(Terrains.RUBBLE) > 0)
            && (this instanceof Mech)) {
            adjustDifficultTerrainPSRModifier(roll);
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE,
                             "Check false: Entity is not entering rubble");
        }

        return roll;
    }

    /**
     * Checks if the entity is moving into a hex that might cause it to bog
     * down. If so, returns the target roll for the piloting skill check.
     */
    public PilotingRollData checkBogDown(MoveStep step,
            EntityMovementType moveType, IHex curHex, Coords lastPos,
            Coords curPos, int lastElev, boolean isPavementStep) {
        PilotingRollData roll = getBasePilotingRoll(moveType);
        int bgMod = curHex.getBogDownModifier(getMovementMode(),
                this instanceof LargeSupportTank);
        if ((!lastPos.equals(curPos) || (step.getElevation() != lastElev))
                && (bgMod != TargetRoll.AUTOMATIC_SUCCESS)
                && (moveType != EntityMovementType.MOVE_JUMP)
                && (step.getElevation() == 0) && !isPavementStep) {
            roll.append(new PilotingRollData(getId(), bgMod,
                    "avoid bogging down"));
            if ((this instanceof Mech) && ((Mech) this).isSuperHeavy()) {
                roll.addModifier(1, "superheavy mech avoiding bogging down");
            }
            adjustDifficultTerrainPSRModifier(roll);
        } else {
            roll.addModifier(
                    TargetRoll.CHECK_FALSE,
                    "Check false: Not entering bog-down terrain, or jumping/hovering over such terrain");
        }
        return roll;
    }

    /**
     * Checks if the entity is moving into depth 1+ water. If so, returns the
     * target roll for the piloting skill check.
     */
    public PilotingRollData checkWaterMove(MoveStep step,
            EntityMovementType moveType, IHex curHex, Coords lastPos,
            Coords curPos, boolean isPavementStep) {
        if ((curHex.terrainLevel(Terrains.WATER) > 0)
            && (step.getElevation() < 0) && !lastPos.equals(curPos)
            && (moveType != EntityMovementType.MOVE_JUMP)
            && (getMovementMode() != EntityMovementMode.HOVER)
            && (getMovementMode() != EntityMovementMode.VTOL)
            && (getMovementMode() != EntityMovementMode.NAVAL)
            && (getMovementMode() != EntityMovementMode.HYDROFOIL)
            && (getMovementMode() != EntityMovementMode.SUBMARINE)
            && (getMovementMode() != EntityMovementMode.INF_UMU)
            && (getMovementMode() != EntityMovementMode.BIPED_SWIM)
            && (getMovementMode() != EntityMovementMode.QUAD_SWIM)
            && (getMovementMode() != EntityMovementMode.WIGE)
            && !isPavementStep) {
            return checkWaterMove(curHex.terrainLevel(Terrains.WATER), moveType);
        }
        return checkWaterMove(0, moveType);
    }

    /**
     * Checks if the entity is moving into depth 1+ water. If so, returns the
     * target roll for the piloting skill check.
     */
    public PilotingRollData checkWaterMove(int waterLevel,
                                           EntityMovementType overallMoveType) {
        PilotingRollData roll = getBasePilotingRoll(overallMoveType);

        int mod;
        if (waterLevel == 1) {
            mod = -1;
        } else if (waterLevel == 2) {
            mod = 0;
        } else {
            mod = 1;
        }

        if (waterLevel > 0) {
            // append the reason modifier
            roll.append(new PilotingRollData(getId(), mod, "entering Depth "
                                                           + waterLevel + " Water"));
            adjustDifficultTerrainPSRModifier(roll);
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE,
                             "Check false: No water here.");
        }

        return roll;
    }

    /**
     * Checks if the entity is being swarmed. If so, returns the target roll for
     * the piloting skill check to dislodge them.
     */
    public PilotingRollData checkDislodgeSwarmers(MoveStep step,
            EntityMovementType moveType) {

        // If we're not being swarmed, return CHECK_FALSE
        if (Entity.NONE == getSwarmAttackerId()) {
            return new PilotingRollData(getId(), TargetRoll.CHECK_FALSE,
                    "Check false: No swarmers attached");
        }

        // append the reason modifier
        PilotingRollData roll = getBasePilotingRoll(moveType);
        roll.append(new PilotingRollData(getId(), 0,
                "attempting to dislodge swarmers by dropping prone"));
        addPilotingModifierForTerrain(roll, step);

        return roll;
    }

    /**
     * Checks to see if an entity is moving through building walls. Note: this
     * method returns true/false, unlike the other checkStuff() methods above.
     *
     * @return 0, no eligible building; 1, exiting; 2, entering; 3, both; 4,
     * stepping on roof, 8 changing elevations within a building
     */
    public int checkMovementInBuilding(MoveStep step, MoveStep prevStep,
                                       Coords curPos, Coords prevPos) {
        if ((prevPos == null)
            || (prevPos.equals(curPos) && !(this instanceof Protomech))) {
            return 0;
        }
        IHex curHex = game.getBoard().getHex(curPos);
        IHex prevHex = game.getBoard().getHex(prevPos);
        // ineligible because of movement type or unit type
        if (isAirborne()) {
            return 0;
        }

        if ((this instanceof Infantry)
            && (step.getMovementType(false) != EntityMovementType.MOVE_JUMP)) {
            return 0;
        }

        if ((this instanceof Protomech) && (prevStep != null)
            && (prevStep.getMovementType(false) == EntityMovementType.MOVE_JUMP)) {
            return 0;
        }

        // check for movement inside a hangar
        Building curBldg = game.getBoard().getBuildingAt(curPos);
        if ((null != curBldg)
            && curBldg.isIn(prevPos)
            && (curBldg.getBldgClass() == Building.HANGAR)
            && (curHex.terrainLevel(Terrains.BLDG_ELEV) > height())
            && (step.getElevation() < curHex
                .terrainLevel(Terrains.BLDG_ELEV))) {
            return 0;
        }

        int rv = 0;
        // check current hex for building
        if (step.getElevation() < curHex.terrainLevel(Terrains.BLDG_ELEV)) {
            rv += 2;
        } else if (((step.getElevation() == curHex
                .terrainLevel(Terrains.BLDG_ELEV)) || (step.getElevation() == curHex
                .terrainLevel(Terrains.BRIDGE_ELEV)))
                   && (step.getMovementType(false) != EntityMovementType.MOVE_JUMP)) {
            rv += 4;
        }
        // check previous hex for building
        if (prevHex != null) {
            int prevEl = getElevation();
            if (prevStep != null) {
                prevEl = prevStep.getElevation();
            }
            if ((prevEl < prevHex.terrainLevel(Terrains.BLDG_ELEV))
                && ((curHex.terrainLevel(Terrains.BLDG_CLASS) != 1) || (getHeight() >= curHex
                    .terrainLevel(Terrains.BLDG_ELEV)))) {
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

        // Check for changing levels within a building
        if (curPos.equals(prevPos)
            && (curBldg != null)
            && (prevStep != null)
            && (step.getElevation() != prevStep.getElevation())
            && ((step.getType() == MoveStepType.UP) || (step.getType() == MoveStepType.DOWN))) {
            rv = 8;
        }

        if ((this instanceof Infantry) || (this instanceof Protomech)) {
            if ((rv != 2) && (rv != 8) && (rv != 10)) {
                rv = 0;
            }
        }
        return rv;
    }

    /**
     * Calculates and returns the roll for an entity moving in buildings.
     */
    public PilotingRollData rollMovementInBuilding(Building bldg, int distance,
                                                   String why, EntityMovementType overallMoveType) {
        PilotingRollData roll = getBasePilotingRoll(overallMoveType);

        if ((this instanceof Mech) && ((Mech) this).isSuperHeavy()) {
            roll.addModifier(4, "superheavy mech moving in building");
        }

        int mod = 0;
        String desc;

        if (why.equals("")) {
            desc = "moving through ";
        } else {
            desc = why + " ";
        }

        switch (bldg.getType()) {
            case Building.LIGHT:
                desc = "Light";
                break;
            case Building.MEDIUM:
                if (bldg.getBldgClass() != Building.HANGAR) {
                    mod = 1;
                    desc = "Medium";
                }
                if (bldg.getBldgClass() >= Building.FORTRESS) {
                    mod = 2;
                    desc = desc + " Fortress";
                }
                break;
            case Building.HEAVY:
                mod = 2;
                desc = "Heavy";
                if (bldg.getBldgClass() == Building.HANGAR) {
                    mod = 1;
                    desc = desc + " Hangar";
                }
                if (bldg.getBldgClass() == Building.FORTRESS) {
                    mod = 3;
                    desc = desc + " Fortress";
                }
                // if(bldg.getBldgClass() == Building.CASTLE_BRIAN) {
                // mod = 4;
                // desc = desc + " Castle Brian";
                // }
                break;
            case Building.HARDENED:
                mod = 5;
                desc = "Hardened";
                if (bldg.getBldgClass() == Building.HANGAR) {
                    mod = 3;
                    desc = desc + " Hangar";
                }
                if (bldg.getBldgClass() == Building.FORTRESS) {
                    mod = 4;
                    desc = desc + " Fortress";
                }
                break;
            case Building.WALL:
                mod = 12;
                desc = "";
                break;
        }

        // append the reason modifier
        roll.append(new PilotingRollData(getId(), mod, "moving through " + desc
                                                       + " " + bldg.getName()));
        adjustDifficultTerrainPSRModifier(roll);

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
     * calculate any changes to the PSR modifier for entering difficult terrain
     */
    private void adjustDifficultTerrainPSRModifier(PilotingRollData psr) {
        if (hasQuirk(OptionsConstants.QUIRK_POS_EASY_PILOT) && (getCrew().getPiloting() > 3)) {
            psr.addModifier(-1, "easy to pilot");
        }
        if (hasQuirk(OptionsConstants.QUIRK_NEG_UNBALANCED)) {
            psr.addModifier(+1, "unbalanced");
        }
    }

    /**
     * The maximum elevation change the entity can cross
     */
    public abstract int getMaxElevationChange();

    /**
     * by default, entities can move as far down as they can move up
     */
    public int getMaxElevationDown() {
        return getMaxElevationDown(getElevation());
    }
    
    /**
     * Returns the maximum number of downard elevation changes a unit can make.
     * For some units (namely, WiGEs), this can depend upon their current
     * elevation (since elevation determines if the WiGEs is using WiGE movement
     * or not).
     *  
     * @param currElevation
     * @return
     */
    public int getMaxElevationDown(int currElevation) {
        return getMaxElevationChange();
    }

    /**
     * Add a transportation component to this Entity. Please note, this method
     * should only be called during this entity's construction.
     *
     * @param component - One of this new entity's <code>Transporter</code>s.
     */
    public void addTransporter(Transporter component) {
        component.setGame(game);
        transports.add(component);
    }

    public void removeTransporter(Transporter t) {
        transports.remove(t);
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
     * @param unit - the <code>Entity</code> to be loaded.
     * @return <code>true</code> if the unit can be loaded, <code>false</code>
     * otherwise.
     */
    public boolean canLoad(Entity unit, boolean checkElev) {
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
                if (next.canLoad(unit)
                    && (!checkElev || (unit.getElevation() == getElevation()))) {
                    return true;
                }
            }
        }

        // If we got here, none of our transports can carry the unit.
        return false;
    }

    public boolean canLoad(Entity unit) {
        return this.canLoad(unit, true);
    }

    /**
     * Load the given unit.
     *
     * @param unit - the <code>Entity</code> to be loaded.
     * @throws IllegalArgumentException If the unit can't be loaded
     */
    public void load(Entity unit, boolean checkElev, int bayNumber) {
        // Walk through this entity's transport components;
        // find the one that can load the unit.
        // Stop looking after the first match.
        Enumeration<Transporter> iter = transports.elements();
        while (iter.hasMoreElements()) {
            Transporter next = iter.nextElement();
            if (next.canLoad(unit)
                && (!checkElev || (unit.getElevation() == getElevation()))
                && ((bayNumber == -1) || (((Bay) next).getBayNumber() == bayNumber))) {
                next.load(unit);
                unit.setTargetBay(-1); // Reset the target bay for later.
                return;
            }
        }

        // If we got to this point, then we can't load the unit.
        throw new IllegalArgumentException(getShortName() + " can not load "
                                           + unit.getShortName());
    }

    public void load(Entity unit, boolean checkElev) {
        this.load(unit, checkElev, -1);
    }

    public void load(Entity unit, int bayNumber) {
        this.load(unit, true, bayNumber);
    }

    public void load(Entity unit) {
        this.load(unit, true, -1);
    }

    /**
     * Recover the given unit. Only for ASF and Small Craft
     *
     * @param unit - the <code>Entity</code> to be loaded.
     * @throws IllegalArgumentException If the unit can't be loaded
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
                if (next instanceof DockingCollar) {
                    ((DockingCollar) next).recover(unit);
                    return;
                }
            }
        }

        // If we got to this point, then we can't load the unit.
        throw new IllegalArgumentException(getShortName() + " can not recover "
                                           + unit.getShortName());
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
            Bay chosenBay = potential.elementAt(Compute.randomInt(potential
                                                                          .size()));
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
            DockingCollar chosenDC = potential.elementAt(Compute
                                                                 .randomInt(potential.size()));
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
     * list will never be <code>null</code>, but it may be empty. The
     * returned <code>List</code> is independant from the under- lying
     * data structure; modifying one does not affect the other.
     */
    public List<Entity> getLoadedUnits() {
        List<Entity> result = new ArrayList<Entity>();

        // Walk through this entity's transport components;
        // add all of their lists to ours.
        for (Transporter next : transports) {
            for (Entity e : next.getLoadedUnits()) {
                result.add(e);
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
     * return the bay that the given entity is loaded into
     *
     * @param loaded
     * @return
     */
    public Bay getBay(Entity loaded) {
        for (Transporter next : transports) {
            if (next instanceof Bay) {
                for (Entity e : next.getLoadedUnits()) {
                    if (loaded.getId() == e.getId()) {
                        return (Bay) next;
                    }
                }
            }
        }
        return null;
    }

    public Bay getBayById(int bayNumber) {
        for (Transporter next : transports) {
            if (next instanceof Bay) {
                if (((Bay) next).getBayNumber() == bayNumber) {
                    return (Bay) next;
                }
            }
        }
        return null;
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
     * recovery)
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

    /**
     * @return only entities in that can be combat dropped
     */
    public Vector<Entity> getDroppableUnits() {
        Vector<Entity> result = new Vector<Entity>();

        // Walk through this entity's transport components;
        // add all of their lists to ours.

        // I should only add entities in bays that are functional
        for (Transporter next : transports) {
            if ((next instanceof Bay) && (((Bay) next).getDoors() > 0)) {
                Bay nextbay = (Bay) next;
                for (Entity e : nextbay.getDroppableUnits()) {
                    result.addElement(e);
                }
            }
        }

        // Return the list.
        return result;
    }

    /**
     * @return only entities in that can be unloaded on ground
     */
    public Vector<Entity> getUnitsUnloadableFromBays() {
        Vector<Entity> result = new Vector<Entity>();

        // Walk through this entity's transport components;
        // add all of their lists to ours.

        // I should only add entities in bays that are functional
        for (Transporter next : transports) {
            if ((next instanceof Bay) && (((Bay) next).canUnloadUnits())) {
                Bay nextbay = (Bay) next;
                for (Entity e : nextbay.getUnloadableUnits()) {
                    if (!e.wasLoadedThisTurn()) {
                        result.addElement(e);
                    }
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
            if (((next instanceof ASFBay) || (next instanceof SmallCraftBay))
                && (((Bay) next).getDoors() > 0)) {
                result.addElement((Bay) next);
            }
        }

        // Return the list.
        return result;

    }

    /**
     * @return get the bays separately
     */
    public Vector<DockingCollar> getDockingCollars() {
        Vector<DockingCollar> result = new Vector<DockingCollar>();

        for (Transporter next : transports) {
            if (next instanceof DockingCollar) {
                result.addElement((DockingCollar) next);
            }
        }

        // Return the list.
        return result;

    }

    /**
     * Returns vector of Transports for everything a unit transports
     *
     * @return
     */
    public Vector<Transporter> getTransports() {
        return transports;
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

    public void resetBays() {
        for (Transporter next : transports) {
            if (next instanceof Bay) {
                ((Bay) next).resetCounts();
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
            if ((next instanceof SmallCraftBay)
                && (((SmallCraftBay) next).getDoors() > 0)) {
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
            if ((next instanceof SmallCraftBay)
                && (((SmallCraftBay) next).getDoors() > 0)) {
                Bay nextbay = (Bay) next;
                for (Entity e : nextbay.getLaunchableUnits()) {
                    result.addElement(e);
                }
            }
        }

        // Return the list.
        return result;
    }

    public Vector<Entity> getLoadedDropships() {
        Vector<Entity> result = new Vector<Entity>();

        // Walk through this entity's transport components;
        // add all of their lists to ours.
        for (Transporter next : transports) {
            if (next instanceof DockingCollar) {
                for (Entity e : next.getLoadedUnits()) {
                    result.addElement(e);
                }
            }
        }

        // Return the list.
        return result;
    }

    public Vector<Entity> getLaunchableDropships() {
        Vector<Entity> result = new Vector<Entity>();

        // Walk through this entity's transport components;
        // add all of their lists to ours.
        for (Transporter next : transports) {
            if (next instanceof DockingCollar) {
                DockingCollar collar = (DockingCollar) next;
                for (Entity e : collar.getLaunchableUnits()) {
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
            if ((next instanceof SmallCraftBay)
                && (((SmallCraftBay) next).getDoors() > 0)) {
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
     * @param unit - the <code>Entity</code> to be unloaded.
     * @return <code>true</code> if the unit was contained in this space,
     * <code>false</code> otherwise.
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

    public void resetTransporter() {
        // Walk through this entity's transport components;
        // and resets them
        Enumeration<Transporter> iter = transports.elements();
        while (iter.hasMoreElements()) {
            Transporter next = iter.nextElement();
            next.resetTransporter();
        }
    }

    /**
     * Return a string that identifies the unused capacity of this transporter.
     *
     * @return A <code>String</code> meant for a human.
     */
    public String getUnusedString() {
        return getUnusedString(false);
    }

    public double getUnused() {
        double capacity = 0;
        for (Transporter transport : transports) {
            capacity += transport.getUnused();
        }
        return capacity;
    }

    /**
     * Returns the current amount of cargo space for an entity of the given
     * type.
     *
     * @param e An entity that defines the unit class
     * @return The number of units of the given type that can be loaded in this
     * Entity
     */
    public double getUnused(Entity e) {
        double capacity = 0;
        for (Transporter transport : transports) {
            if (transport.canLoad(e)) {
                capacity += transport.getUnused();
            }
        }
        return capacity;
    }

    /**
     * Return a string that identifies the unused capacity of this transporter.
     *
     * @return A <code>String</code> meant for a human.
     */
    public String getUnusedString(boolean useBRTag) {
        StringBuffer result = new StringBuffer();

        // Walk through this entity's transport components;
        // add all of their string to ours.
        Enumeration<Transporter> iter = transports.elements();
        while (iter.hasMoreElements()) {
            Transporter next = iter.nextElement();
            result.append(next.getUnusedString());
            // Add a newline character between strings.
            if (iter.hasMoreElements()) {
                if (useBRTag) {
                    result.append("<br>");
                } else {
                    result.append("\n");
                }
            }
        }

        // Return the String.
        return result.toString();
    }

    /**
     * Determine if transported units prevent a weapon in the given location
     * from firing.
     *
     * @param loc    - the <code>int</code> location attempting to fire.
     * @param isRear - a <code>boolean</code> value stating if the given location
     *               is rear facing; if <code>false</code>, the location is front
     *               facing.
     * @return <code>true</code> if a transported unit is in the way,
     * <code>false</code> if the weapon can fire.
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
     * @param loc    - the <code>int</code> location hit by attack.
     * @param isRear - a <code>boolean</code> value stating if the given location
     *               is rear facing; if <code>false</code>, the location is front
     *               facing.
     * @return The <code>Entity</code> being transported on the outside at that
     * location. This value will be <code>null</code> if no unit is
     * transported on the outside at that location.
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
     * @param transportId - the <code>int</code> ID of our transport. The ID is
     *                    <b>not</b> validated. This value should be
     *                    <code>Entity.NONE</code> if this unit has been unloaded.
     */
    public void setTransportId(int transportId) {
        conveyance = transportId;
        // If we were unloaded, set the appropriate flags.
        if (transportId == Entity.NONE) {
            unloadedThisTurn = true;
            done = true;
        } else {
            loadedThisTurn = true;
        }
    }

    /**
     * Get the ID <code>Entity</code> that has loaded this one.
     *
     * @return the <code>int</code> ID of our transport. The ID may be invalid.
     * This value should be <code>Entity.NONE</code> if this unit has
     * not been loaded.
     */
    public int getTransportId() {
        return conveyance;
    }

    /**
     * Determine if this unit has an active and working stealth system.
     * <p/>
     * Sub-classes are encouraged to override this method.
     *
     * @return <code>true</code> if this unit has a stealth system that is
     * currently active and it's actually working, <code>false</code> if
     * there is no stealth system or if it is inactive.
     */
    public boolean isStealthActive() {
        return false;
    }

    /**
     * Determine if this unit has an active and working stealth system.
     * <p/>
     * Sub-classes are encouraged to override this method.
     *
     * @return <code>true</code> if this unit has a stealth system that is
     * currently active and it's actually working, <code>false</code> if
     * there is no stealth system or if it is inactive.
     */
    public boolean isStealthOn() {
        return false;
    }

    /**
     * Determine if this unit has an active null-signature system.
     * <p/>
     * Sub-classes are encouraged to override this method.
     *
     * @return <code>true</code> if this unit has a null signature system that
     * is currently active, <code>false</code> if there is no stealth
     * system or if it is inactive.
     */
    public boolean isNullSigActive() {
        return false;
    }

    /**
     * Determine if this unit has an active null-signature system.
     * <p/>
     * Sub-classes are encouraged to override this method.
     *
     * @return <code>true</code> if this unit has a null signature system that
     * is currently active, <code>false</code> if there is no stealth
     * system or if it is inactive.
     */
    public boolean isNullSigOn() {
        return false;
    }

    /**
     * Determine if this unit has an active void signature system that is
     * providing its benefits.
     * <p/>
     * Sub-classes are encouraged to override this method.
     *
     * @return <code>true</code> if this unit has a void signature system that
     * is currently active, <code>false</code> if there is no stealth
     * system or if it is inactive.
     */
    public boolean isVoidSigActive() {
        return false;
    }

    /**
     * Determine if this unit has an active void signature system.
     * <p/>
     * Sub-classes are encouraged to override this method.
     *
     * @return <code>true</code> if this unit has a void signature system that
     * is currently active, <code>false</code> if there is no stealth
     * system or if it is turned off.
     */
    public boolean isVoidSigOn() {
        return false;
    }

    /**
     * Determine if this unit has an active chameleon light polarization field.
     * <p/>
     * Sub-classes are encouraged to override this method.
     *
     * @return <code>true</code> if this unit has a void signature system that
     * is currently active, <code>false</code> if there is no stealth
     * system or if it is inactive.
     */
    public boolean isChameleonShieldActive() {
        return false;
    }

    /**
     * Determine if this unit has an active chameleon light polarization field.
     * <p/>
     * Sub-classes are encouraged to override this method.
     *
     * @return <code>true</code> if this unit has a void signature system that
     * is currently active, <code>false</code> if there is no stealth
     * system or if it is inactive.
     */
    public boolean isChameleonShieldOn() {
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
     * @param range - an <code>int</code> value that must match one of the
     *              <code>Compute</code> class range constants.
     * @param ae    - the entity making the attack, who maybe immune to certain
     *              kinds of stealth
     * @return a <code>TargetRoll</code> value that contains the stealth
     * modifier for the given range.
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
            case RangeType.RANGE_LOS:
            case RangeType.RANGE_OUT:
                result = new TargetRoll(0, "stealth not installed");
                break;
            default:
                throw new IllegalArgumentException("Unknown range constant: "
                                                   + range);
        }

        // Return the result.
        return result;
    }

    /**
     * Record the ID of the <code>Entity</code> that is the current target of a
     * swarm attack by this unit. A unit that stops swarming can neither move
     * nor attack for the rest of the turn.
     *
     * @param id - the <code>int</code> ID of the swarm attack's target. The ID
     *           is <b>not</b> validated. This value should be
     *           <code>Entity.NONE</code> if this unit has stopped swarming.
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
     * be invalid. This value should be <code>Entity.NONE</code> if this
     * unit is not swarming.
     */
    public int getSwarmTargetId() {
        return swarmTargetId;
    }

    /**
     * Record the ID of the <code>Entity</code> that is attacking this unit with
     * a swarm attack.
     *
     * @param id - the <code>int</code> ID of the swarm attack's attacker. The
     *           ID is <b>not</b> validated. This value should be
     *           <code>Entity.NONE</code> if the swarm attack has ended.
     */
    public void setSwarmAttackerId(int id) {
        swarmAttackerId = id;
    }

    /**
     * Get the ID of the <code>Entity</code> that is attacking this unit with a
     * swarm attack.
     *
     * @return the <code>int</code> ID of the swarm attack's attacker The ID may
     * be invalid. This value should be <code>Entity.NONE</code> if this
     * unit is not being swarmed.
     */
    public int getSwarmAttackerId() {
        return swarmAttackerId;
    }

    /**
     * Scans through the ammo on the unit for any inferno rounds.
     *
     * @return <code>true</code> if the unit is still loaded with Inferno
     * rounds. <code>false</code> if no rounds were ever loaded or if
     * they have all been fired.
     */
    public boolean hasInfernoAmmo() {
        boolean found = false;

        // Walk through the unit's ammo, stop when we find a match.
        for (Mounted amounted : getAmmo()) {
            AmmoType atype = (AmmoType) amounted.getType();
            if (((atype.getAmmoType() == AmmoType.T_SRM) || (atype
                                                                     .getAmmoType() == AmmoType.T_MML))
                && (atype.getMunitionType() == AmmoType.M_INFERNO)
                && (amounted.getHittableShotsLeft() > 0)) {
                found = true;
            }
            if ((atype.getAmmoType() == AmmoType.T_IATM)
                && (atype.getMunitionType() == AmmoType.M_IATM_IIW)
                && (amounted.getHittableShotsLeft() > 0)) {
                found = true;
            }
        }
        return found;
    }

    /**
     * Record if the unit is just combat-lossed or if it has been utterly
     * destroyed.
     *
     * @param canSalvage - a <code>boolean</code> that is <code>true</code> if the unit
     *                   can be repaired (given time and parts); if this value is
     *                   <code>false</code>, the unit is utterly destroyed.
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
     * salvageable components; if this value is <code>false</code> the
     * unit is utterly destroyed.
     * @see #isRepairable()
     */
    public boolean isSalvage() {
        return salvageable;
    }

    /**
     * Determine if the unit can be repaired, or only harvested for spares.
     *
     * @return A <code>boolean</code> that is <code>true</code> if the unit can
     * be repaired (given enough time and parts); if this value is
     * <code>false</code>, the unit is only a source of spares.
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
     * @param removalCondition New value of property removalCondition.
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
        if (game.getOptions().booleanOption("pilots_cannot_spot")
            && (this instanceof MechWarrior)) {
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
            str = str + "Location: (" + (getPosition().getX() + 1) + ", "
                  + (getPosition().getY() + 1) + ") ";
        }
        str = str + "Owner: " + owner.getName() + " Armor: " + getTotalArmor()
              + "/" + getTotalOArmor() + " Internal Structure: "
              + getTotalInternal() + "/" + getTotalOInternal();

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
     * @param loc the location
     * @return a string descibing the status of the location.
     */
    public String statusToString(int loc) {
        if (loc == LOC_NONE) {
            return "No location given.";
        }

        return getLocationName(loc) + " (" + getLocationAbbr(loc)
               + "): Armor: " + getArmorString(loc) + "/" + getOArmor(loc)
               + " Structure: " + getInternalString(loc) + "/"
               + getOInternal(loc);
    }

    /**
     * @param str a string defining the location
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
     * @param deployRound an int
     */
    public void setDeployRound(int deployRound) {
        this.deployRound = deployRound;
        // also set this for any transported units
        for (Transporter transport : getTransports()) {
            for (Entity e : transport.getLoadedUnits()) {
                e.setDeployRound(deployRound);
            }
        }

        // Entity's that deploy after the start can set their own deploy zone
        // If the deployRound is being set back to 0, make sure we reset the
        // starting position (START_NONE implies inheritance from owning player)
        if (deployRound == 0) {
            setStartingPos(Board.START_NONE);
        }
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
        if (deployed) {
            neverDeployed = false;
        }
    }

    /**
     * Checks to see if an entity has been deployed
     */
    public boolean isDeployed() {
        return deployed;
    }

    /**
     * Checks to see if entity was never deployed
     */
    public boolean wasNeverDeployed() {
        return neverDeployed;
    }

    /**
     * Toggles if an entity has been deployed
     */
    public void setNeverDeployed(boolean neverDeployed) {
        this.neverDeployed = neverDeployed;
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
     * @param unit the number for the low-level unit that this
     *             entity belongs to. This entity can be removed from its unit by
     *             passing the value, <code>{@link Entity#NONE}</code>.
     */
    public void setUnitNumber(final short unit) {
        unitNumber = unit;
    }

    /**
     * Get the unit number of this entity.
     *
     * @return The unit number. If the entity does not belong
     * to a unit, <code>{@link Entity#NONE}</code> will be returned.
     */
    public short getUnitNumber() {
        return unitNumber;
    }

    /**
     * Returns whether an entity can flee from its current position. Currently
     * returns true if the entity is on the edge of the board.
     */
    public boolean canFlee() {
        Coords pos = getPosition();
        return (pos != null)
               && ((getWalkMP() > 0) || (this instanceof Infantry))
               && !isProne()
               && !isStuck()
               && !isShutDown()
               && !getCrew().isUnconscious()
               && ((pos.getX() == 0) || (pos.getX() == (game.getBoard().getWidth() - 1))
                   || (pos.getY() == 0) || (pos.getY() == (game.getBoard()
                                                               .getHeight() - 1)));
    }

    public void setEverSeenByEnemy(boolean b) {
        everSeenByEnemy = b;
    }

    public boolean isEverSeenByEnemy() {
        return everSeenByEnemy;
    }

    public void setVisibleToEnemy(boolean b) {
        visibleToEnemy = b;
    }

    public boolean isVisibleToEnemy() {
        // If double blind isn't on, the unit is always visible
        if ((game != null) && !game.getOptions().booleanOption("double_blind")) {
            return true;
        }
        return visibleToEnemy;
    }

    public void setDetectedByEnemy(boolean b) {
        detectedByEnemy = b;
    }

    public boolean isDetectedByEnemy() {
        // If double blind isn't on, the unit is always detected
        if ((game != null) && !game.getOptions().booleanOption("double_blind")) {
            return true;
        }
        return detectedByEnemy;
    }

    public void addBeenSeenBy(IPlayer p) {
        if ((p != null) && !entitySeenBy.contains(p)) {
            entitySeenBy.add(p);
        }
    }

    public Vector<IPlayer> getWhoCanSee() {
        return entitySeenBy;
    }
    
    public void setWhoCanSee(Vector<IPlayer> entitySeenBy) {
        this.entitySeenBy = entitySeenBy;
    }

    public void clearSeenBy() {
        entitySeenBy.clear();
    }

    /**
     * Returns true if the the given player can see this Entity, including 
     * teammates if team_vision is on.
     *  
     */
    public boolean hasSeenEntity(IPlayer p) {
        // No double blind - everyone sees everything
        if ((game == null) || !game.getOptions().booleanOption("double_blind")) {
            return true;
        }
        // Null players see nothing
        if (p == null) {
            return false;
        }
        // A Player can always see their own 'mechs
        if (getOwner().equals(p)) {
            return true;
        }

        // If a player can see all, it sees this
        if (p.canSeeAll()) {
            return true;
        }

        // Observers can see units spotted by an enemy
        if (p.isObserver()) {
            for (IPlayer other : entitySeenBy) {
                if (other.isEnemyOf(getOwner())) {
                    return true;
                }
            }
            return false;
        }

        if (entitySeenBy.contains(p)) {
            return true;
        }
        // If team vision, see if any players on team can see
        if (game.getOptions().booleanOption("team_vision")) {
            for (IPlayer teammate : game.getPlayersVector()) {
                if ((teammate.getTeam() == p.getTeam())
                        && entitySeenBy.contains(teammate)) {
                    return true;
                }
            }
        }
        // Can't see
        return false;
    }
    
    public void addBeenDetectedBy(IPlayer p) {
        // This is for saved-game backwards compatibility
        if (entityDetectedBy == null) {
            entityDetectedBy = new Vector<IPlayer>();
        }
        if ((p != null) && !entityDetectedBy.contains(p)) {
            entityDetectedBy.add(p);
        }
    }

    public Vector<IPlayer> getWhoCanDetect() {
        return entityDetectedBy;
    }
    
    public void setWhoCanDetect(Vector<IPlayer> entityDetectedBy) {
        this.entityDetectedBy = entityDetectedBy;
    }

    public void clearDetectedBy() {
        // This is for saved-game backwards compatibility
        if (entityDetectedBy == null) {
            entityDetectedBy = new Vector<IPlayer>();
        }
        entityDetectedBy.clear();
    }

    /**
     * Returns true if the the given player can see this Entity, including 
     * teammates if team_vision is on.
     *  
     */
    public boolean hasDetectedEntity(IPlayer p) {
        // No sensors - no one detects anything
        if ((game == null)
                || !game.getOptions().booleanOption("tacops_sensors")) {
            return false;
        }
        // Null players detect nothing
        if (p == null) {
            return false;
        }
        // This is for saved-game backwards compatibility
        if (entityDetectedBy == null) {
            entityDetectedBy = new Vector<IPlayer>();
        }

        // Observers can detect units detected by an enemy
        if (p.isObserver()) {
            for (IPlayer other : entityDetectedBy) {
                if (other.isEnemyOf(getOwner())) {
                    return true;
                }
            }
            return false;
        }

        if (entityDetectedBy.contains(p)) {
            return true;
        }
        // If team vision, see if any players on team can see
        if (game.getOptions().booleanOption("team_vision")) {
            for (IPlayer teammate : game.getPlayersVector()) {
                if ((teammate.getTeam() == p.getTeam())
                        && entityDetectedBy.contains(teammate)) {
                    return true;
                }
            }
        }
        // Can't see
        return false;
    }  
    
    /**
     * Returns whether this Entity is a sensor return to the given player.
     * 
     * @param spotter
     *            The player trying to view this unit
     * @return True if the given player can only see this Entity as a sensor
     *         return
     */
    public boolean isSensorReturn(IPlayer spotter) {
        boolean alliedUnit = 
                !getOwner().isEnemyOf(spotter)
                || (getOwner().getTeam() == spotter.getTeam() 
                    && game.getOptions().booleanOption("team_vision"));
        
        boolean sensors = game.getOptions().booleanOption(
                "tacops_sensors");
        boolean sensorsDetectAll = game.getOptions().booleanOption(
                "sensors_detect_all");
        boolean doubleBlind = game.getOptions().booleanOption(
                "double_blind");
        
        return sensors && doubleBlind && !alliedUnit && !sensorsDetectAll 
                && !hasSeenEntity(spotter) && hasDetectedEntity(spotter);
    }

    protected int applyGravityEffectsOnMP(int MP) {
        int result = MP;
        if (game != null) {
            float fMP = MP / game.getPlanetaryConditions().getGravity();
            fMP = (Math.abs((Math.round(fMP) - fMP)) == 0.5) ? (float) Math
                    .floor(fMP) : Math.round(fMP); // the
            // rule
            // requires
            // rounding down on .5
            result = (int) fMP;
        }
        return result;
    }

    /**
     * Whether this type of unit can perform charges
     */
    public boolean canCharge() {
        return !isImmobile() && (getWalkMP() > 0) && !isStuck() && !isProne();
    }

    /**
     * Whether this type of unit can perform DFA attacks
     */
    public boolean canDFA() {
        return !isImmobile() && (getJumpMP() > 0) && !isStuck() && !isProne();
    }

    /**
     * Whether this type of unit can perform Ramming attacks
     */
    public boolean canRam() {
        return false;
    }

    public boolean isUsingManAce() {
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

    public int getKillerId() {
        return killerId;
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
        if ((phase != IGame.Phase.PHASE_PHYSICAL)
            && (phase != IGame.Phase.PHASE_FIRING)
            && (phase != IGame.Phase.PHASE_OFFBOARD)) {
            return false;
        }
        // if you're charging or finding a club, it's already declared
        if (isUnjammingRAC() || isCharging() || isMakingDfa() || isRamming()
            || isFindingClub() || isOffBoard()) {
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

        // Check for weapons. If we find them, return true. Otherwise... we
        // return false.
        // Bug 3648: No, no, no - you cannot skip units with no weapons - what
        // about spotting, unjamming, etc.?
        /*
         * for (Mounted mounted : getWeaponList()) { WeaponType wtype =
         * (WeaponType) mounted.getType(); if ((wtype != null) &&
         * (!wtype.hasFlag(WeaponType.F_AMS) && !wtype.hasFlag(WeaponType.F_TAG)
         * && mounted.isReady() && ((mounted.getLinked() == null) || (mounted
         * .getLinked().getUsableShotsLeft() > 0)))) { return true; } }
         */

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
        if (!isActive()
            || (isImmobile() && !isManualShutdown() && !canUnjamRAC() && !game
                .getOptions().booleanOption("vehicles_can_eject"))) {
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
            if ((wtype != null)
                && (wtype.hasFlag(WeaponType.F_TAG) && mounted.isReady())) {
                return true;
            }
        }
        return false;// only things w/ tag are
    }

    public boolean isAttackingThisTurn() {
        List<EntityAction> actions = game.getActionsVector();
        for (EntityAction ea : actions) {
            if ((ea.getEntityId() == getId())
                && (ea instanceof AbstractAttackAction)) {
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
        if (isUnjammingRAC() || isCharging() || isMakingDfa()
            || isFindingClub() || isOffBoard() || isAssaultDropInProgress()
            || isDropping()) {
            return false;
        }

        // check game options
        if (game.getOptions().booleanOption("no_clan_physical") && isClan()
            && !hasINarcPodsAttached() && (getSwarmAttackerId() == NONE)) {
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

        // sprinted?
        if (moved == EntityMovementType.MOVE_SPRINT) {
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
        Iterator<Entity> e = game.getEntities();
        while (!canHit && e.hasNext()) {
            Entity target = e.next();

            // don't shoot at friendlies unless you are into that sort of thing
            // and do not shoot yourself even then
            if (!(isEnemyOf(target) || (friendlyFire && (getId() != target
                    .getId())))) {
                continue;
            }

            if (!target.isDeployed()) {
                continue;
            }
            // No physical attack works at distances > 1.
            if ((target.getPosition() != null)
                && (Compute.effectiveDistance(game, this, target) > 1)) {
                continue;
            }

            canHit |= Compute.canPhysicalTarget(game, getId(), target);
            // check if we can dodge and target can attack us,
            // then we are eligible.
            canHit |= ((this instanceof Mech) && !isProne()
                       && getCrew().getOptions().booleanOption("dodge_maneuver") && Compute
                    .canPhysicalTarget(game, target.getId(), this));
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
                final BuildingTarget target = new BuildingTarget(coords,
                                                                 game.getBoard(), false);
                canHit |= Compute.canPhysicalTarget(game, getId(), target);

            } // Check the next hex of the building

        } // Check the next building

        return canHit;
    }

    /**
     * Determines if this Entity is eligible to pre-designate hexes as
     * auto-hits. Per TacOps pg 180, if a player has offboard artillery they get
     * 5 pre- designated hexes per mapsheet.
     *
     * @return
     */
    public boolean isEligibleForArtyAutoHitHexes() {
        return isEligibleForTargetingPhase()
               && (isOffBoard() || game.getOptions().booleanOption(
                "on_map_predesignate"));
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

    public float getTroopCarryingSpace() {
        float space = 0;
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

    /**
     * Returns true if this unit has a ClampMountMech or ClampMountTank that
     * is currently unloaded.
     * @return
     */
    public boolean hasUnloadedClampMount() {
        for (Transporter t : transports) {
            if (((t instanceof ClampMountTank) || (t instanceof ClampMountMech))
                    && (t.getUnused() > 0)) {
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
     * @param distance  the <code>int</code> distance in hexes that the unit will be
     *                  deployed from the board; this value must not be negative.
     * @param direction the <code>int</code> direction from the board that the unit
     *                  will be deployed; a valid value must be selected from: NONE,
     *                  NORTH, SOUTH, EAST, or WEST.
     * @throws IllegalArgumentException if a negative distance, an invalid direction is selected, or
     *                                  the distance does not match the direction.
     */
    public void setOffBoard(int distance, OffBoardDirection direction) {
        if (distance < 0) {
            throw new IllegalArgumentException(
                    "negative number given for distance offboard");
        }
        if ((0 == distance) && (OffBoardDirection.NONE != direction)) {
            throw new IllegalArgumentException(
                    "onboard unit was given an offboard direction");
        }
        if ((0 != distance) && (OffBoardDirection.NONE == direction)) {
            throw new IllegalArgumentException(
                    "offboard unit was not given an offboard direction");
        }
        switch (direction) {
            case NORTH:
                setFacing(3);
                break;
            case SOUTH:
                setFacing(0);
                break;
            case WEST:
                setFacing(2);
                break;
            case EAST:
                setFacing(4);
                break;
            default:
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
     * deployed (in hexes); this value will never be negative.
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
     * deployed. Only valid values will be returned.
     */
    public OffBoardDirection getOffBoardDirection() {
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
            throw new IllegalStateException(
                    "game not set; possible serialization error");
        }
        // N.B. 17 / 2 = 8, but the middle of 1..17 is 9, so we
        // add a bit (because 17 % 2 == 1 and 16 % 2 == 0).
        switch (offBoardDirection) {
            case NONE:
                break;
            case NORTH:
                setPosition(new Coords((game.getBoard().getWidth() / 2)
                        + (game.getBoard().getWidth() % 2),
                        -getOffBoardDistance()));
                setFacing(3);
                setDeployed(true);
                break;
            case SOUTH:
                setPosition(new Coords((game.getBoard().getWidth() / 2)
                        + (game.getBoard().getWidth() % 2), game.getBoard()
                        .getHeight() + getOffBoardDistance()));
                setFacing(0);
                setDeployed(true);
                break;
            case EAST:
                setPosition(new Coords(game.getBoard().getWidth()
                        + getOffBoardDistance(),
                        (game.getBoard().getHeight() / 2)
                                + (game.getBoard().getHeight() % 2)));
                setFacing(5);
                setDeployed(true);
                break;
            case WEST:
                setPosition(new Coords(-getOffBoardDistance(), (game.getBoard()
                        .getHeight() / 2) + (game.getBoard().getHeight() % 2)));
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
     * @param arg the <code>boolean</code> value to assign.
     */
    public void setCaptured(boolean arg) {
        captured = arg;
    }

    public void setExternalSpotlight(boolean arg) {
        hasExternalSpotlight = arg;
    }

    /**
     * Returns state of hasExternalSpotlight, does not consider mounted
     * spotlights.
     *
     * @return
     */
    public boolean hasExternaSpotlight() {
        return hasExternalSpotlight;
    }

    /**
     * Returns true if the unit has a usable spotlight. It considers both
     * externally mounted spotlights as well as internally mounted ones.
     *
     * @return
     */
    public boolean hasSpotlight() {
        for (Mounted m : getMisc()) {
            if (m.getType().hasFlag(MiscType.F_SEARCHLIGHT)
                && !m.isInoperable()) {
                return true;
            }
        }
        return hasExternalSpotlight;
    }

    /**
     * Method to destroy a single spotlight on an entity. Spotlights can be
     * destroyed on a roll of 7+ on a torso hit on a mek or on a front/side hit
     * on a combat vehicle.
     */
    public void destroyOneSpotlight() {
        if (!hasSpotlight()) {
            return;
        }
        // A random spotlight should be destroyed, but this is easier...
        if (hasExternalSpotlight) {
            hasExternalSpotlight = false;
        }

        for (Mounted m : getMisc()) {
            if (m.getType().hasFlag(MiscType.F_SEARCHLIGHT)
                && !m.isInoperable()) {
                m.setDestroyed(true);
                break;
            }
        }

        // Turn off the light all spot lights were destroyed
        if (!hasSpotlight()) {
            setSpotlightState(false);
        }

    }

    public void setSpotlightState(boolean arg) {
        if (hasSpotlight()) {
            spotlightIsActive = arg;
            if (arg) {
                illuminated = true;
            }
        } else {
            spotlightIsActive = false;
        }
    }

    public boolean isIlluminated() {
        // Regardless of illuminated state, if we have a spotlight active we
        //  are illuminated
        return illuminated || spotlightIsActive;
    }

    public void setIlluminated(boolean arg) {
        illuminated = spotlightIsActive || arg;
    }

    public boolean isUsingSpotlight() {
        return hasSpotlight() && spotlightIsActive;
    }

    public void setUsedSearchlight(boolean arg) {
        usedSearchlight = arg;
    }

    public boolean usedSearchlight() {
        return usedSearchlight;
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
     * @param arg the <code>boolean</code> value to assign
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
     * @param arg the <code>boolean</code> value to assign
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
            sb.append("\n*** ").append(
                    mw.getDisplayName() + " died in the wreckage. ***\n");
        }
        return sb.toString();
    }

    /**
     * Add a targeting by a swarm volley from a specified entity
     *
     * @param entityId The <code>int</code> id of the shooting entity
     * @param weaponId The <code>int</code> id of the shooting lrm launcher
     */
    public void addTargetedBySwarm(int entityId, int weaponId) {
        hitBySwarmsEntity.addElement(new Integer(entityId));
        hitBySwarmsWeapon.addElement(new Integer(weaponId));
    }

    /**
     * Were we targeted by a certain swarm/swarm-i volley this turn?
     *
     * @param entityId The <code>int</code> id of the shooting entity we are checking
     * @param weaponId The <code>int</code> id of the launcher to check
     * @return a fitting <code>boolean</code> value
     */
    public boolean getTargetedBySwarm(int entityId, int weaponId) {
        for (int i = 0; i < hitBySwarmsEntity.size(); i++) {
            Integer entityIdToTest = hitBySwarmsEntity.elementAt(i);
            Integer weaponIdToTest = hitBySwarmsWeapon.elementAt(i);
            if ((entityId == entityIdToTest.intValue())
                && (weaponId == weaponIdToTest.intValue())) {
                return true;
            }
        }
        return false;
    }

    public int getShortRangeModifier() {
        int mod = 0;
        if (hasQuirk(OptionsConstants.QUIRK_POS_IMP_TARG_S)) {
            mod--;
        }
        if (hasQuirk(OptionsConstants.QUIRK_NEG_POOR_TARG_S)) {
            mod++;
        }
        if (hasQuirk(OptionsConstants.QUIRK_POS_VAR_RNG_TARG_L)) {
            mod++;
        }
        if (hasQuirk(OptionsConstants.QUIRK_POS_VAR_RNG_TARG_S)) {
            mod--;
        }
        return mod;
    }

    public int getMediumRangeModifier() {
        int mod = 2;
        if (getCrew().getOptions().booleanOption("sniper")) {
            mod = mod / 2;
        }
        if (hasQuirk(OptionsConstants.QUIRK_POS_IMP_TARG_M)) {
            mod--;
        }
        if (hasQuirk(OptionsConstants.QUIRK_NEG_POOR_TARG_M)) {
            mod++;
        }
        return mod;
    }

    public int getLongRangeModifier() {
        int mod = 4;
        if (getCrew().getOptions().booleanOption("sniper")) {
            mod = mod / 2;
        }
        if (hasQuirk(OptionsConstants.QUIRK_POS_IMP_TARG_L)) {
            mod--;
        }
        if (hasQuirk(OptionsConstants.QUIRK_NEG_POOR_TARG_L)) {
            mod++;
        }
        if (hasQuirk(OptionsConstants.QUIRK_POS_VAR_RNG_TARG_L)) {
            mod--;
        }
        if (hasQuirk(OptionsConstants.QUIRK_POS_VAR_RNG_TARG_S)) {
            mod++;
        }
        return mod;
    }

    public int getExtremeRangeModifier() {
        int mod = 6;
        if (getCrew().getOptions().booleanOption("sniper")) {
            mod = mod / 2;
        }
        return mod;
    }

    public int getLOSRangeModifier() {
        int mod = 8;
        if (getCrew().getOptions().booleanOption("sniper")) {
            mod = mod / 2;
        }
        return mod;
    }

    public void setArmorType(int armType) {
        for (int i = 0; i < locations(); i++) {
            armorType[i] = armType;
        }
    }

    public void setArmorType(int armType, int loc) {
        armorType[loc] = armType;
    }

    public void setStructureType(int strucType) {
        structureType = strucType;
        structureTechLevel = getTechLevel();
    }

    public void setStructureTechLevel(int level) {
        structureTechLevel = level;
    }

    public void setArmorType(String armType) {
        if (!(armType.startsWith("Clan ") || armType.startsWith("IS "))) {
            armType = TechConstants.isClan(getArmorTechLevel(0)) ? "Clan "
                                                                   + armType : "IS " + armType;
        }
        EquipmentType et = EquipmentType.get(armType);
        if (et == null) {
            setArmorType(EquipmentType.T_ARMOR_UNKNOWN);
        } else {
            setArmorType(EquipmentType.getArmorType(et));
            // TODO: Is this needed? WTF is the point of it?
            if (et.getCriticals(this) == 0) {
                try {
                    this.addEquipment(et, LOC_NONE);
                } catch (LocationFullException e) {
                    // can't happen
                    e.printStackTrace();
                }
            }
        }
    }

    public void setArmorType(String armType, int loc) {
        if (!(armType.startsWith("Clan ") || armType.startsWith("IS "))) {
            armType = TechConstants.isClan(getArmorTechLevel(0)) ? "Clan "
                                                                   + armType : "IS " + armType;
        }
        EquipmentType et = EquipmentType.get(armType);
        if (et == null) {
            setArmorType(EquipmentType.T_ARMOR_UNKNOWN, loc);
        } else {
            setArmorType(EquipmentType.getArmorType(et), loc);
            // TODO: Is this needed? WTF is the point of it?
            if (et.getCriticals(this) == 0) {
                try {
                    this.addEquipment(et, LOC_NONE);
                } catch (LocationFullException e) {
                    // can't happen
                    e.printStackTrace();
                }
            }
        }
    }

    public void setStructureType(String strucType) {
        if (!(strucType.startsWith("Clan ") || strucType.startsWith("IS "))) {
            strucType = isClan() ? "Clan " + strucType : "IS " + strucType;
        }
        EquipmentType et = EquipmentType.get(strucType);
        setStructureType(EquipmentType.getStructureType(et));
        if (et == null) {
            structureTechLevel = TechConstants.T_TECH_UNKNOWN;
        } else {
            structureTechLevel = et.getTechLevel(year);
            // TODO: Is this needed? WTF is the point of it?
            if (et.getCriticals(this) == 0) {
                try {
                    this.addEquipment(et, LOC_NONE);
                } catch (LocationFullException e) {
                    // can't happen
                    e.printStackTrace();
                }
            }
        }

    }

    public int getArmorType(int loc) {
        return armorType[loc];
    }

    public void setArmorTechLevel(int newTL) {
        for (int i = 0; i < locations(); i++) {
            armorTechLevel[i] = newTL;
        }
    }

    public void setArmorTechLevel(int newTL, int loc) {
        armorTechLevel[loc] = newTL;
    }

    public int getArmorTechLevel(int loc) {
        return armorTechLevel[loc];
    }

    public int getStructureType() {
        return structureType;
    }

    public int getStructureTechLevel() {
        return structureTechLevel;
    }

    public void setWeaponHit(Mounted which) {
        if (weaponList.contains(which)) {
            which.setHit(true);
        }
    }

    public void setTaggedBy(int tagger) {
        taggedBy = tagger;
    }

    public int getTaggedBy() {
        return taggedBy;
    }

    public abstract double getCost(boolean ignoreAmmo);

    public long getWeaponsAndEquipmentCost(boolean ignoreAmmo) {
        // bvText = new StringBuffer();
        long cost = 0;

        NumberFormat commafy = NumberFormat.getInstance();

        for (Mounted mounted : getEquipment()) {
            if (ignoreAmmo
                && (mounted.getType() instanceof AmmoType)
                && (!(((AmmoType) mounted.getType()).getAmmoType() == AmmoType.T_COOLANT_POD))) {
                continue;
            }
            if (mounted.isWeaponGroup()) {
                continue;
            }
            long itemCost = (long) mounted.getType().getCost(this,
                                                             mounted.isArmored(), mounted.getLocation());

            cost += itemCost;
            if ((bvText != null) && (itemCost > 0)) {
                bvText.append(startRow);
                bvText.append(startColumn);
                bvText.append(mounted.getName());
                bvText.append(endColumn);

                bvText.append(startColumn);
                bvText.append(commafy.format(itemCost));
                bvText.append(endColumn);
                bvText.append(endRow);
            }
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
                    if ((location == Mech.LOC_LLEG)
                        || (location == Mech.LOC_LARM)
                        || (location == Mech.LOC_LT)) {
                        return true;
                    }
                    break;
                case LosEffects.COVER_RIGHT:
                    if ((location == Mech.LOC_RLEG)
                        || (location == Mech.LOC_RARM)
                        || (location == Mech.LOC_RT)) {
                        return true;
                    }
                    break;
                case LosEffects.COVER_HORIZONTAL:
                    if ((location == Mech.LOC_LLEG)
                        || (location == Mech.LOC_RLEG)) {
                        return true;
                    }
                    break;
                case LosEffects.COVER_UPPER:
                    if ((location == Mech.LOC_LLEG)
                        || (location == Mech.LOC_RLEG)) {
                        return false;
                    }
                    return true;
                case LosEffects.COVER_FULL:
                    return true;
                case LosEffects.COVER_75LEFT:
                    if ((location == Mech.LOC_RARM)
                        || (location == Mech.LOC_RLEG)) {
                        return false;
                    }
                    return true;
                case LosEffects.COVER_75RIGHT:
                    if ((location == Mech.LOC_LLEG)
                        || (location == Mech.LOC_LARM)) {
                        return false;
                    }
                    return true;
            }
        }
        return false;

    }

    public abstract boolean doomedInVacuum();

    public abstract boolean doomedOnGround();

    public abstract boolean doomedInAtmosphere();

    public abstract boolean doomedInSpace();

    /**
     * The weight of the armor in a specific location, rounded up to the nearest
     * half-ton for patchwork armor as per TacOps page 377 (Errata 3.1). Note:
     * Unless overridden, this should <em>only</em> be called on units with
     * patchwork armor, as rounding behavior is not guaranteed to be correct or
     * even the same for others and units with a single overall armor type have
     * no real reason to specifically care about weight per location anyway.
     *
     * @param loc The code value for the location in question (unit
     *            type-specific).
     * @return The weight of the armor in the location in tons.
     */
    public double getArmorWeight(int loc) {
        double armorPerTon = 16.0 * EquipmentType.getArmorPointMultiplier(
                armorType[loc], armorTechLevel[loc]);
        double points = getOArmor(loc)
                        + (hasRearArmor(loc) ? getOArmor(loc, true) : 0);
        double armorWeight = points / armorPerTon;
        return Math.ceil(armorWeight * 2.0) / 2.0;

    }

    /**
     * The total weight of the armor on this unit. This is guaranteed to be
     * rounded properly for both single-type and patchwork armor.
     *
     * @return The armor weight in tons.
     */
    public double getArmorWeight() {
        if (!hasPatchworkArmor()) {
            // this roundabout method is actually necessary to avoid rounding
            // weirdness. Yeah, it's dumb.
            double armorPerTon = 16.0 * EquipmentType.getArmorPointMultiplier(
                    armorType[0], armorTechLevel[0]);
            double points = getTotalOArmor();
            double armorWeight = points / armorPerTon;
            armorWeight = Math.ceil(armorWeight * 2.0) / 2.0;
            return armorWeight;
        }
        double total = 0;
        for (int loc = 0; loc < locations(); loc++) {
            total += getArmorWeight(loc);
        }
        return total;
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
        return ((game != null) && game.getOptions().booleanOption(
                "all_have_ei_cockpit"));
    }

    public boolean hasActiveEiCockpit() {
        return (hasEiCockpit() && getCrew().getOptions().booleanOption(
                "ei_implant"));
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
            if (!mount.isMissing()
                && (type.hasFlag(MiscType.F_MINE) || type
                    .hasFlag(MiscType.F_VEHICLE_MINE_DISPENSER))
                && !isLayingMines()) {
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
        return sideTable(src, usePrior, face, getPosition());
    }

    public int sideTable(Coords src, boolean usePrior, int face,
                         Coords effectivePos) {
        if (usePrior) {
            effectivePos = getPriorPosition();
        }

        if (src.equals(effectivePos)) {
            // most places handle 0 range explicitly,
            // this is a safe default (calculation gives SIDE_RIGHT)
            return ToHitData.SIDE_FRONT;
        }

        // calculate firing angle
        int fa = (effectivePos.degree(src) + ((6 - face) * 60)) % 360;

        int leftBetter = 2;
        // if we're right on the line, we need to special case this
        // defender would choose along which hex the LOS gets drawn, and that
        // side also determines the side we hit in
        if ((fa % 30) == 0) {
            IHex srcHex = game.getBoard().getHex(src);
            IHex curHex = game.getBoard().getHex(getPosition());
            if ((srcHex != null) && (curHex != null)) {
                LosEffects.AttackInfo ai = LosEffects.buildAttackInfo(src,
                                                                      getPosition(), 1, getElevation(), srcHex.floor(),
                                                                      curHex.floor());
                ArrayList<Coords> in = Coords.intervening(ai.attackPos,
                                                          ai.targetPos, true);
                leftBetter = LosEffects.dividedLeftBetter(in, game, ai,
                                                          Compute.isInBuilding(game, this), new LosEffects());
            }
        }

        boolean targetIsTank = (this instanceof Tank)
                               || (game.getOptions().booleanOption(
                "tacops_advanced_mech_hit_locations") && (this instanceof QuadMech));
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
            // Handle spheroids in atmosphere or on the ground differently
            if (a.isSpheroid() && (game != null) && !game.getBoard().inSpace()) {
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
        } else if (((fa == 150) && (leftBetter == 1))
                   || ((leftBetter == 0) && (fa == 210))) {
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

    /**
     * Method to determine if an entity is currently capable of going hull-down.
     * Note, this is *not* whether the entity can ever go hull-down.
     *
     * @return True if the entity is able to go hull-down, else false.
     */
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
        moved = EntityMovementType.MOVE_JUMP;
    }

    public boolean isAssaultDropInProgress() {
        return assaultDropInProgress != 0;
    }

    /**
     * Apply PSR modifier for difficult terrain at the specified coordinates
     *
     * @param roll the PSR to modify
     * @param c    the coordinates where the PSR happens
     */
    public void addPilotingModifierForTerrain(PilotingRollData roll, Coords c) {
        addPilotingModifierForTerrain(roll, c, false);
    }

    /**
     * Apply PSR modifier for difficult terrain at the specified coordinates
     *
     * @param roll the PSR to modify
     * @param c    the coordinates where the PSR happens
     * @param enteringRubble True if entering rubble, else false
     */
    public void addPilotingModifierForTerrain(PilotingRollData roll, Coords c,
            boolean enteringRubble) {
        if ((c == null) || (roll == null)) {
            return;
        }
        if (isOffBoard() || !(isDeployed())) {
            return;
        }
        IHex hex = game.getBoard().getHex(c);
        hex.terrainPilotingModifier(getMovementMode(), roll, enteringRubble);
    }

    /**
     * Apply PSR modifier for difficult terrain at the move step position
     *
     * @param roll the PSR to modify
     * @param step the move step the PSR occurs at
     */
    public void addPilotingModifierForTerrain(PilotingRollData roll,
            MoveStep step) {
        if (step.getElevation() > 0) {
            return;
        }
        addPilotingModifierForTerrain(roll, step.getPosition());
    }

    /**
     * Apply PSR modifier for difficult terrain in the current position
     *
     * @param roll the PSR to modify
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
        if (!isDeployed() || isOffBoard()
            || !game.getBoard().contains(getPosition())) {
            return false;
        }
        if (!isElevationValid(getElevation(),
                              game.getBoard().getHex(getPosition()))) {
            System.err.println(getDisplayName() + " in hex "
                               + HexTarget.coordsToId(getPosition())
                               + " is at invalid elevation: " + getElevation());
            setElevation(0 - game.getBoard()
                                 .getHex(getPosition()).depth());
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
            if ((s.toLowerCase().indexOf("(is)") != -1)
                || (s.toLowerCase().indexOf("inner sphere") != -1)) {
                return true;
            }
            return false;
        }
        if ((s.toLowerCase().indexOf("(c)") != -1)
            || (s.toLowerCase().indexOf("clan") != -1)) {
            return true;
        }
        return false;
    }

    /**
     * @return Returns the retreatedDirection.
     */
    public OffBoardDirection getRetreatedDirection() {
        return retreatedDirection;
    }

    /**
     * @param retreatedDirection The retreatedDirection to set.
     */
    public void setRetreatedDirection(OffBoardDirection retreatedDirection) {
        this.retreatedDirection = retreatedDirection;
    }

    public void setLastTarget(int id) {
        lastTarget = id;
    }

    public int getLastTarget() {
        return lastTarget;
    }
    
    public void setLastTargetDisplayName(String name) {
        lastTargetDisplayName = name;
    }

    public String getLastTargetDisplayName() {
        return lastTargetDisplayName;
    }

    /**
     * @returns whether or not the unit is suffering from Electromagnetic
     * Interference
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
     * @param carcass true if this unit should be a carcass, false otherwise.
     * @see megamek.common.Entity#isCarcass
     */
    public void setCarcass(boolean carcass) {
        this.carcass = carcass;
    }

    /**
     * Marks all equipment in a location on this entity as destroyed.
     *
     * @param loc The location that is destroyed.
     */

    public void destroyLocation(int loc) {
        destroyLocation(loc, false);
    }

    /**
     * Marks all equipment in a location on this entity as destroyed.
     *
     * @param loc      The location that is destroyed.
     * @param blownOff true if the location was blown off
     */
    public void destroyLocation(int loc, boolean blownOff) {
        // if it's already marked as destroyed, don't bother
        if (getInternal(loc) < 0) {
            return;
        }
        if (blownOff) {
            setLocationBlownOff(loc, true);
            setLocationBlownOffThisPhase(loc, true);
        } else {
            // mark armor, internal as doomed
            setArmor(IArmorState.ARMOR_DOOMED, loc, false);
            setInternal(IArmorState.ARMOR_DOOMED, loc);
            if (hasRearArmor(loc)) {
                setArmor(IArmorState.ARMOR_DOOMED, loc, true);
            }
        }
        // equipment marked missing
        for (Mounted mounted : getEquipment()) {
            if (((mounted.getLocation() == loc) && mounted.getType()
                                                          .isHittable())
                || (mounted.isSplit() && (mounted.getSecondLocation() == loc))) {
                if (blownOff) {
                    mounted.setMissing(true);
                } else {
                    mounted.setHit(true);
                }
            }
        }
        // all critical slots set as missing
        for (int i = 0; i < getNumberOfCriticals(loc); i++) {
            final CriticalSlot cs = getCritical(loc, i);
            if (cs != null) {
                // count engine hits for maxtech engine explosions
                if ((cs.getType() == CriticalSlot.TYPE_SYSTEM)
                    && (cs.getIndex() == Mech.SYSTEM_ENGINE)
                    && !cs.isDamaged()) {
                    engineHitsThisPhase++;
                }
                if (blownOff) {
                    cs.setMissing(true);
                } else {
                    cs.setHit(true);
                }
            }
        }
        // dependent locations destroyed, unless they are already destroyed
        if ((getDependentLocation(loc) != Entity.LOC_NONE)
            && !(getInternal(getDependentLocation(loc)) < 0)) {
            destroyLocation(getDependentLocation(loc), true);
        }
    }

    /**
     * Iterates over all Narc and iNarc pods attached to this entity and removes
     * those still 'stuck' to destroyed or missing locations.
     */
    public void clearDestroyedNarcPods() {
        for (Iterator<NarcPod> i = pendingNarcPods.iterator(); i.hasNext(); ) {
            if (!locationCanHoldNarcPod(i.next().getLocation())) {
                i.remove();
            }
        }
        for (Iterator<NarcPod> i = narcPods.iterator(); i.hasNext(); ) {
            if (!locationCanHoldNarcPod(i.next().getLocation())) {
                i.remove();
            }
        }
        for (Iterator<INarcPod> i = pendingINarcPods.iterator(); i.hasNext(); ) {
            if (!locationCanHoldNarcPod(i.next().getLocation())) {
                i.remove();
            }
        }
        for (Iterator<INarcPod> i = iNarcPods.iterator(); i.hasNext(); ) {
            if (!locationCanHoldNarcPod(i.next().getLocation())) {
                i.remove();
            }
        }
    }

    private boolean locationCanHoldNarcPod(int location) {
        return (getInternal(location) > 0)
               && !isLocationBlownOff(location)
               && !isLocationBlownOffThisPhase(location);
    }

    public PilotingRollData checkSideSlip(EntityMovementType moveType,
                                          IHex prevHex, EntityMovementType overallMoveType,
                                          MoveStep prevStep, int prevFacing, int curFacing, Coords lastPos,
                                          Coords curPos, int distance) {
        PilotingRollData roll = getBasePilotingRoll(overallMoveType);

        if ((moveType != EntityMovementType.MOVE_JUMP)
            && (prevHex != null)
            && (distance > 1)
            && ((overallMoveType == EntityMovementType.MOVE_RUN) || (overallMoveType == EntityMovementType
                .MOVE_VTOL_RUN))
            && (prevFacing != curFacing) && !lastPos.equals(curPos)
            && !(this instanceof Infantry)) {
            roll.append(new PilotingRollData(getId(), 0, "flanking and turning"));
            if (isUsingManAce()) {
                roll.addModifier(-1, "Maneuvering Ace");
            }
            if ((getMovementMode() == EntityMovementMode.VTOL) && isMASCUsed()
                && hasWorkingMisc(MiscType.F_JET_BOOSTER)) {
                roll.addModifier(3, "used VTOL Jet Booster");
            }
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE,
                             "Check false: not apparently sideslipping");
        }

        return roll;
    }

    public boolean isAirborneVTOLorWIGE() {
        // stuff that moves like a VTOL is flying unless at elevation 0 or on
        // top of/in a building,
        if ((getMovementMode() == EntityMovementMode.VTOL)
                || (getMovementMode() == EntityMovementMode.WIGE)) {
            if ((game != null)
                    && (game.getBoard() != null)
                    && (getPosition() != null)
                    && (game.getBoard().getHex(getPosition()) != null)
                    && ((game.getBoard().getHex(getPosition())
                            .terrainLevel(Terrains.BLDG_ELEV) >= getElevation()) || (game
                            .getBoard().getHex(getPosition())
                            .terrainLevel(Terrains.BRIDGE_ELEV) >= getElevation()))) {
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
            if ((m.getLocation() == mounted.getLocation())
                && m.getType().hasFlag(WeaponType.F_MGA)
                && !(m.isDestroyed() || m.isBreached())
                && m.getBayWeapons().contains(getEquipmentNum(mounted))
                && m.getType().hasModes() && m.curMode().equals("Linked")) {
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
        return (this instanceof Aero)
               && !((this instanceof SmallCraft) || (this instanceof Jumpship) || (this instanceof FighterSquadron));
    }

    public boolean isCapitalFighter() {
        return isCapitalFighter(false);
    }

    public boolean isCapitalFighter(boolean lounge) {
        if (null == game) {
            return false;
        }

        // If we're using the unofficial option for single fighters staying
        // standard scale & we're not a member of a squadron... then false.
        if (!lounge && isFighter()
            && game.getOptions().booleanOption("single_no_cap")
            && !isPartOfFighterSquadron()) {
            return false;
        }

        return game.getOptions().booleanOption("stratops_capital_fighter")
               && isFighter();
    }

    /**
     * a function that let's us know if this entity has capital-scale armor
     *
     * @return
     */
    public boolean isCapitalScale() {

        if ((this instanceof Jumpship) || (this instanceof FighterSquadron)
            || isCapitalFighter()) {
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
            if ((bay.getAtClass() == wtype.getAtClass())
                && (m.getLocation() == loc)
                && (m.isRearMounted() == rearMount) && (damage <= 700)) {
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

            if ((mounted.getLocation() == location)
                && (mounted.isRearMounted() == rearMount)) {
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
                if ((newside == ToHitData.SIDE_LEFT)
                    || (newside == ToHitData.SIDE_RIGHT)) {
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
    
    public void setPlayerPickedPassThrough(int attackerId, Coords c) {
        if (playerPickedPassThrough == null) {
            playerPickedPassThrough = new HashMap<>();
        }
        playerPickedPassThrough.put(attackerId, c);
    }
    
    public Coords getPlayerPickedPassThrough(int attackerId) {
        if (playerPickedPassThrough == null) {
            playerPickedPassThrough = new HashMap<>();
        }
        return playerPickedPassThrough.get(attackerId);
    }

    public void setPassedThrough(Vector<Coords> pass) {
        passedThrough = pass;
    }

    public Vector<Coords> getPassedThrough() {
        return passedThrough;
    }
    
    public void setPassedThroughFacing(List<Integer> passFacing) {
        passedThroughFacing = passFacing;
    }

    public List<Integer> getPassedThroughFacing() {
        return passedThroughFacing;
    }

    public void addPassedThrough(Coords c) {
        passedThrough.add(c);
    }

    /**
     * Method that determines if this Entity passed over another entity during
     * its current path
     *
     * @param t
     * @return
     */
    public boolean passedOver(Targetable t) {
        for (Coords crd : passedThrough) {
            if (crd.equals(t.getPosition())) {
                return true;
            }
            for (Coords secondary : t.getSecondaryPositions().values()) {
                if (crd.equals(secondary)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean passedThrough(Coords c) {
        for (Coords crd : passedThrough) {
            if (crd.equals(c)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Did the entity pass within a certain number of hexes of these coords?
     */
    public boolean passedWithin(Coords c, int dist) {
        for (Coords crd : passedThrough) {
            if (crd.distance(c) <= dist) {
                return true;
            }
        }
        return false;
    }

    /**
     * What coords were passed through previous to the given one
     */
    public Coords passedThroughPrevious(Coords c) {
        if (passedThrough.size() == 0) {
            return getPosition();
        }
        Coords prevCrd = passedThrough.get(0);
        for (Coords crd : passedThrough) {
            if (crd.equals(c)) {
                break;
            }
            prevCrd = crd;
        }
        return prevCrd;
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
     * Force rapid fire mode to the highest level on RAC and UAC - this is for
     * aeros
     */
    public void setRapidFire() {
        for (Mounted m : getTotalWeaponList()) {
            WeaponType wtype = (WeaponType) m.getType();
            if (wtype.getAmmoType() == AmmoType.T_AC_ROTARY) {
                m.setMode("6-shot");
                m.setModeSwitchable(false);
            } else if (wtype.getAmmoType() == AmmoType.T_AC_ULTRA) {
                m.setMode("Ultra");
                m.setModeSwitchable(false);
            }
        }
    }

    /**
     * Set the retractable blade in the given location as extended Takes the
     * first piece of appropriate equipment
     */
    public void extendBlade(int loc) {
        for (Mounted m : getEquipment()) {
            if ((m.getLocation() == loc) && !m.isDestroyed() && !m.isBreached()
                && (m.getType() instanceof MiscType)
                && m.getType().hasFlag(MiscType.F_CLUB)
                && m.getType().hasSubType(MiscType.S_RETRACTABLE_BLADE)) {
                m.setMode("extended");
                return;
            }
        }
    }

    /**
     * destroys the first retractable blade critical slot found
     */
    public void destroyRetractableBlade(int loc) {
        // check critical slots
        for (int i = 0; i < this.getNumberOfCriticals(loc); i++) {
            CriticalSlot slot = getCritical(loc, i);
            // ignore empty & system slots
            if ((slot == null)
                || (slot.getType() != CriticalSlot.TYPE_EQUIPMENT)) {
                continue;
            }
            Mounted m = slot.getMount();
            if ((m.getLocation() == loc) && !m.isHit() && !m.isBreached()
                && (m.getType() instanceof MiscType)
                && m.getType().hasFlag(MiscType.F_CLUB)
                && m.getType().hasSubType(MiscType.S_RETRACTABLE_BLADE)) {
                slot.setHit(true);
                m.setHit(true);
                return;
            }
        }
    }

    public TeleMissileTracker getTMTracker() {
        return tmTracker;
    }

    public void setGrappled(int id, boolean attacker) {
        // This is implemented in subclasses.  Do nothing in general.
    }

    public boolean isGrappleAttacker() {
        return false;
    }

    public int getGrappled() {
        return Entity.NONE;
    }
    
    public boolean isChainWhipGrappled() {
        return getGrappleSide() != Entity.GRAPPLE_BOTH;
    }
    
    public boolean isGrappledThisRound() {
        return false;
    }
    
    public void setGrappledThisRound(boolean grappled) {
        // Do nothing here, set in base classes
    }

    public void setGameOptions() {

        if (game == null) {
            return;
        }

        final GameOptions gameOpts = game.getOptions();

        // if the small craft does not already have ECM, then give them a single
        // hex ECM so they can change the mode
        // FIXME: This is a really hacky way to to do it that results in small
        // craft having
        // ECM when the rule is not in effect and in non-space maps
        if ((this instanceof SmallCraft) && !(this instanceof Dropship)
            && !hasActiveECM() && isMilitary()) {
            try {
                String prefix = "IS";
                if (isClan()) {
                    prefix = "CL";
                }
                this.addEquipment(
                        EquipmentType.get(prefix + BattleArmor.SINGLE_HEX_ECM),
                        Aero.LOC_NOSE, false);
            } catch (LocationFullException ex) {
                // ignore
            }
        }

        for (Mounted mounted : getWeaponList()) {
            if ((mounted.getType() instanceof GaussWeapon)
                && gameOpts.booleanOption("tacops_gauss_weapons")) {
                String[] modes = {"Powered Up", "Powered Down"};
                ((WeaponType) mounted.getType()).setModes(modes);
                ((WeaponType) mounted.getType()).setInstantModeSwitch(false);
            } else if ((mounted.getType() instanceof ACWeapon)
                       && gameOpts.booleanOption("tacops_rapid_ac")) {
                String[] modes = {"", "Rapid"};
                ((WeaponType) mounted.getType()).setModes(modes);
                if (gameOpts.booleanOption("kind_rapid_ac")) {
                    mounted.setKindRapidFire(true);
                }
            } else if (mounted.getType() instanceof ISBombastLaser) {
                int damage = 12;
                ArrayList<String> modes = new ArrayList<String>();
                String[] stringArray = {};

                for (; damage >= 7; damage--) {
                    modes.add("Damage " + damage);
                }
                ((WeaponType) mounted.getType()).setModes(modes
                                                                  .toArray(stringArray));
            } else if (((WeaponType) mounted.getType()).isCapital()
                       && (((WeaponType) mounted.getType()).getAtClass()
                           != WeaponType.CLASS_CAPITAL_MISSILE)
                       && (((WeaponType) mounted.getType()).getAtClass()
                           != WeaponType.CLASS_AR10)) {
                ArrayList<String> modes = new ArrayList<String>();
                String[] stringArray = {};
                modes.add("");
                if (gameOpts.booleanOption("stratops_bracket_fire")) {
                    modes.add("Bracket 80%");
                    modes.add("Bracket 60%");
                    modes.add("Bracket 40%");
                }
                if (((mounted.getType() instanceof CapitalLaserBayWeapon)
                     || (mounted.getType() instanceof SCLBayWeapon))
                    && gameOpts.booleanOption("stratops_aaa_laser")) {
                    modes.add("AAA");
                    ((WeaponType) mounted.getType()).addEndTurnMode("AAA");
                }
                if (modes.size() > 1) {
                    ((WeaponType) mounted.getType()).setModes(modes
                                                                      .toArray(stringArray));
                }
            } else if (mounted.getType().hasFlag(WeaponType.F_AMS)
                       && !gameOpts.booleanOption("auto_ams")) {
                Enumeration<EquipmentMode> modeEnum = mounted.getType().getModes();
                ArrayList<String> newModes = new ArrayList<>();
                while (modeEnum.hasMoreElements()) {
                    newModes.add(modeEnum.nextElement().getName());
                }
                if (!newModes.contains("Automatic")) {
                    newModes.add("Automatic");
                }
                String modes[] = new String[newModes.size()];
                newModes.toArray(modes);
                ((WeaponType) mounted.getType()).setModes(modes);
                ((WeaponType) mounted.getType()).setInstantModeSwitch(false);
            }

        }

        for (Mounted misc : getMisc()) {
            if (misc.getType().hasFlag(MiscType.F_BAP)
                && (this instanceof Aero)
                && gameOpts.booleanOption("stratops_ecm")) {
                ArrayList<String> modes = new ArrayList<String>();
                String[] stringArray = {};
                modes.add("Short");
                modes.add("Medium");
                ((MiscType) misc.getType())
                        .setModes(modes.toArray(stringArray));
                ((MiscType) misc.getType()).setInstantModeSwitch(false);
            }
            if (misc.getType().hasFlag(MiscType.F_ECM)) {
                ArrayList<String> modes = new ArrayList<String>();
                modes.add("ECM");
                String[] stringArray = {};
                if (gameOpts.booleanOption("tacops_eccm")) {
                    modes.add("ECCM");
                    if (misc.getType().hasFlag(MiscType.F_ANGEL_ECM)) {
                        modes.add("ECM & ECCM");
                    }
                } else if (gameOpts.booleanOption("stratops_ecm")
                           && (this instanceof Aero)) {
                    modes.add("ECCM");
                    if (misc.getType().hasFlag(MiscType.F_ANGEL_ECM)) {
                        modes.add("ECM & ECCM");
                    }
                }
                if (gameOpts.booleanOption("tacops_ghost_target")) {
                    if (misc.getType().hasFlag(MiscType.F_ANGEL_ECM)) {
                        modes.add("ECM & Ghost Targets");
                        if (gameOpts.booleanOption("tacops_eccm")) {
                            modes.add("ECCM & Ghost Targets");
                        }
                    } else {
                        modes.add("Ghost Targets");
                    }
                }
                ((MiscType) misc.getType())
                        .setModes(modes.toArray(stringArray));
            }
        }

    }

    public void setGrappleSide(int side) {
        // This is implemented in subclasses, do nothing in general
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
        return hasModularArmor(-1);
    }

    public boolean hasModularArmor(int loc) {
        for (Mounted mount : this.getEquipment()) {
            if ((loc == -1) || (mount.getLocation() == loc)) {
                if (!mount.isDestroyed()
                    && (mount.getType() instanceof MiscType)
                    && ((MiscType) mount.getType())
                        .hasFlag(MiscType.F_MODULAR_ARMOR)) {
                    return true;
                }
            }
        }

        return false;
    }

    public int getDamageReductionFromModularArmor(HitData hit, int damage,
                                                  Vector<Report> vDesc) {
        int loc = hit.getLocation();
        if (!hasModularArmor(loc)) {
            return damage;
        }
        for (Mounted mount : this.getEquipment()) {
            if ((mount.getLocation() == loc)
                && !mount.isDestroyed()
                && (mount.getType() instanceof MiscType)
                && ((MiscType) mount.getType())
                    .hasFlag(MiscType.F_MODULAR_ARMOR)
                // On 'Mech torsos only, modular armor covers either front
                // or rear, as mounted.
                && (!(this instanceof Mech)
                    || !((loc == Mech.LOC_CT) || (loc == Mech.LOC_LT) || (loc == Mech.LOC_RT)) || (hit
                                                                                                           .isRear()
                                                                                                   == mount
                    .isRearMounted()))) {

                int damageAbsorption = mount.getBaseDamageCapacity()
                                       - mount.getDamageTaken();
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

                    damage -= mount.baseDamageAbsorptionRate
                              - mount.damageTaken;
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
        // This is implemented in subclasses, do nothing in general
    }

    /**
     * @return the tonnage of additional mounted communications equipment
     */
    public int getExtraCommGearTons() {
        int i = 0;
        for (Mounted mounted : miscList) {
            if (mounted.getType().hasFlag(MiscType.F_COMMUNICATIONS)
                && !mounted.isInoperable()) {
                i += mounted.getType().getTonnage(this);
            }
        }
        return i;
    }

    /**
     * Returns information (range, location, strength) about ECM if the unit
     * has active ECM or null if it doesn't.  In the case of multiple ECCM
     * system, the best one takes precendence, as a unit can only have one
     * active ECCM at a time.
     *
     * @return
     */
    public ECMInfo getECMInfo() {
        // If we don't have a position, ECM doesn't have an effect
        if ((getPosition() == null) || isShutDown() || isStealthOn()
            || (getTransportId() != Entity.NONE)) {
            return null;
        }

        // E(C)CM operates differently in space (SO pg 110)
        if (game.getBoard().inSpace()) {
            // No ECM in space unless SO rule is on
            if (!game.getOptions().booleanOption("stratops_ecm")) {
                return null;
            }
            int range = getECMRange();
            if ((range >= 0) && hasActiveECM()) {
                return new ECMInfo(range, 1, this);
            } else {
                return null;
            }
        }

        // ASF ECM only has an effect if the unit is NOE
        if (isAirborne() && !isNOE()) {
            return null;
        }

        ECMInfo bestInfo = null;
        Comparator<ECMInfo> ecmComparator;
        ecmComparator = new ECMInfo.ECCMComparator();
        for (Mounted m : getMisc()) {
            // Ignore if inoperable
            if (m.isInoperable()) {
                continue;
            }
            ECMInfo newInfo = null;
            // Angel ECM
            if (m.getType().hasFlag(MiscType.F_ANGEL_ECM)) {
                if (m.curMode().equals("ECM")) {
                    newInfo = new ECMInfo(6, 0, this);
                    newInfo.setAngelECMStrength(1);
                } else if (m.curMode().equals("ECM & ECCM")
                           || m.curMode().equals("ECM & Ghost Targets")) {
                    newInfo = new ECMInfo(6, 1, this);
                    // Doesn't count as Angel ECM
                }
                // BA Angel ECM has a shorter range
                if ((newInfo != null) && (this instanceof BattleArmor)) {
                    newInfo.setRange(2);
                }
                // Anything that's not Angel ECM
            } else if (m.getType().hasFlag(MiscType.F_ECM)
                       && m.curMode().equals("ECM")) {
                int range = 6;
                if (m.getType().hasFlag(MiscType.F_SINGLE_HEX_ECM)) {
                    range = 0;
                } else if (m.getType().hasFlag(MiscType.F_EW_EQUIPMENT)
                           || m.getType().hasFlag(MiscType.F_NOVA)
                           || m.getType().hasFlag(MiscType.F_WATCHDOG)) {
                    range = 3;
                }
                newInfo = new ECMInfo(range, 1, this);
                newInfo.setECMNova(m.getType().hasFlag(MiscType.F_NOVA));
            }
            // In some type of ECM mode...
            if (newInfo != null) {
                if ((bestInfo == null)
                    || (ecmComparator.compare(newInfo, bestInfo) > 0)) {
                    bestInfo = newInfo;
                }
            }
        }
        return bestInfo;
    }

    /**
     * Returns information (range, location, strength) about ECCM if the unit
     * has active ECCM or null if it doesn't.  In the case of multiple ECCM
     * system, the best one takes precendence, as a unit can only have one
     * active ECCM at a time.
     *
     * @return
     */
    public ECMInfo getECCMInfo() {
        // If we don't have a position, ECM doesn't have an effect
        if ((getPosition() == null) || isShutDown() || isStealthOn()
            || (getTransportId() != Entity.NONE)) {
            return null;
        }
        // E(C)CM operates differently in space (SO pg 110)
        if (game.getBoard().inSpace()) {
            // No ECCM in space unless SO rule is on
            if (!game.getOptions().booleanOption("stratops_ecm")) {
                return null;
            }
            int bapRange = getBAPRange();
            int range = getECMRange();
            ECMInfo eccmInfo = new ECMInfo(0, 0, this);
            eccmInfo.setECCMStrength(1);
            if (bapRange > 0) {
                eccmInfo.setRange(bapRange);
                // Medium range band only effects the nose, so set direction
                if (bapRange > 6) {
                    eccmInfo.setDirection(getFacing());
                }
            } else if ((range >= 0) && hasActiveECCM()) {
                eccmInfo.setRange(range);
            } else {
                eccmInfo = null;
            }
            return eccmInfo;
        }

        ECMInfo bestInfo = null;
        Comparator<ECMInfo> ecmComparator;
        ecmComparator = new ECMInfo.ECCMComparator();
        for (Mounted m : getMisc()) {
            ECMInfo newInfo = null;
            if (m.getType().hasFlag(MiscType.F_COMMUNICATIONS)
                && m.curMode().equals("ECCM")) {
                if ((getTotalCommGearTons() > 3)) {
                    newInfo = new ECMInfo(6, 0.5, this);
                }
                if ((getTotalCommGearTons() > 6)) {
                    newInfo = new ECMInfo(6, 1, this);
                }
            }
            // Angel ECM
            if (m.getType().hasFlag(MiscType.F_ANGEL_ECM)) {
                if (m.curMode().equals("ECCM")) {
                    newInfo = new ECMInfo(6, 0, this);
                    newInfo.setAngelECCMStrength(1);
                } else if (m.curMode().equals("ECM & ECCM")
                           || m.curMode().equals("ECCM & Ghost Targets")) {
                    newInfo = new ECMInfo(6, 1, this);
                    // Doesn't count as Angel
                }
                // BA Angel ECM has a shorter range
                if ((newInfo != null) && (this instanceof BattleArmor)) {
                    newInfo.setRange(2);
                }
                // Anything that's not Angel ECM
            } else if (m.getType().hasFlag(MiscType.F_ECM)
                       && m.curMode().equals("ECCM")) {
                int range = 6;
                if (m.getType().hasFlag(MiscType.F_SINGLE_HEX_ECM)) {
                    range = 0;
                } else if (m.getType().hasFlag(MiscType.F_EW_EQUIPMENT)
                           || m.getType().hasFlag(MiscType.F_NOVA)
                           || m.getType().hasFlag(MiscType.F_WATCHDOG)) {
                    range = 3;
                }
                newInfo = new ECMInfo(range, 0, this);
                newInfo.setECCMStrength(1);
            }
            // In some type of ECCM mode...
            if (newInfo != null) {
                if ((bestInfo == null)
                    || (ecmComparator.compare(newInfo, bestInfo) > 0)) {
                    bestInfo = newInfo;
                }
            }
        }
        return bestInfo;
    }

    /**
     * @return the strength of the ECM field this unit emits
     */
    public double getECMStrength() {
        int strength = 0;
        for (Mounted m : getMisc()) {
            if (m.getType().hasFlag(MiscType.F_ANGEL_ECM)) {
                if (m.curMode().equals("ECM")) {
                    strength = 2;
                } else if ((strength < 1)
                           && (m.curMode().equals("ECM & ECCM") || m.curMode()
                                                                    .equals("ECM & Ghost Targets"))) {
                    strength = 1;
                }
            } else if (m.getType().hasFlag(MiscType.F_ECM)
                       && m.curMode().equals("ECM") && (strength < 1)) {
                strength = 1;
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
            if (m.getType().hasFlag(MiscType.F_ANGEL_ECM)) {
                if (m.curMode().equals("ECM")) {
                    strength = 2;
                } else if ((strength < 1)
                           && (m.curMode().equals("ECM & ECCM") || m.curMode()
                                                                    .equals("ECCM & Ghost Targets"))) {
                    strength = 1;
                }
            } else if (m.getType().hasFlag(MiscType.F_ECM)
                       && m.curMode().equals("ECCM") && (strength < 1)) {
                strength = 1;
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
            if (misc.getType().hasFlag(MiscType.F_COMMUNICATIONS)
                && misc.curMode().equals("Default") && !misc.isInoperable()) {
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
        if (crew.getOptions().booleanOption("comm_implant")
            || crew.getOptions().booleanOption("boost_comm_implant")) {
            return 1;
        }
        return 0;
    }

    /**
     * @return the initiative bonus this Entity grants for quirks
     */
    public int getQuirkIniBonus() {
        // command battlemech and and battle computer are not cumulative
        if (hasQuirk(OptionsConstants.QUIRK_POS_BATTLE_COMP) && !getCrew().isDead()
            && !getCrew().isUnconscious()) {
            return 2;
        } else if (hasQuirk(OptionsConstants.QUIRK_POS_COMMAND_MECH) && !getCrew().isDead()
                   && !getCrew().isUnconscious()) {
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
                amounted.setShotsLeft(Math.max(amounted.getBaseShotsLeft()
                                               - nSantaAnna, 0));
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
                    System.err
                            .println("cannot find the right bay for Santa Anna ammo");
                    return;
                }
                baymounts.add(getBayByAmmo(amounted));
                // now make sure santa anna loadout is reset
                amounted.setNSantaAnna(0);
            }
        }

        // now iterate through to get new mounts
        if ((ammo.size() != locations.size()) || (ammo.size() != name.size())
            || (ammo.size() != baymounts.size())) {
            // this shouldn't happen
            System.err.println("cannot load santa anna ammo");
            return;
        }
        for (int i = 0; i < ammo.size(); i++) {
            try {
                Mounted newmount = addEquipment(
                        EquipmentType.get(name.elementAt(i)),
                        locations.elementAt(i), false, ammo.elementAt(i));
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
     * shut this unit down due to a Taser attack
     *
     * @param turns   - the amount of rounds for which this Entity should be
     *                shutdown
     * @param baTaser - was this due to a BA taser?
     */
    public void taserShutdown(int turns, boolean baTaser) {
        setShutDown(true);
        taserShutdownRounds = turns;
        shutdownByBATaser = baTaser;
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

    public boolean getTaserInterferenceHeat() {
        return taserInterferenceHeat;
    }

    /**
     * set this entity to suffer from taser feedback
     *
     * @param rounds - the number of rounds to suffer from taserfeedback
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

    public void setTaserInterference(int value, int rounds, boolean heat) {
        taserInterference = value;
        taserInterferenceRounds = rounds;
        taserInterferenceHeat = heat;
    }

    public int getTaserInterference() {
        return taserInterference;
    }

    public int getTaserInterferenceRounds() {
        return taserInterferenceRounds;
    }

    public void addIMPHits(int missiles) {
        // effects last for only one turn.
        impThisTurn += missiles;
        int heatAdd = missiles + impThisTurnHeatHelp;
        impThisTurnHeatHelp = heatAdd % 3;
        heatAdd = heatAdd - impThisTurnHeatHelp;
        heatAdd = heatAdd / 3;
        heatFromExternal += heatAdd;
    }

    private void doNewRoundIMP() {
        impLastTurn = impThisTurn;
        impThisTurn = 0;
    }

    public int getIMPMoveMod() {
        // this function needs to be added to the MP
        // calculating functions
        //however, since no function calls super, it seems unneccesary complicated
        // really.
        int max = 2;
        int modifier = impThisTurn + impLastTurn;
        modifier = modifier - (modifier % 3);
        modifier = modifier / 3;
        return (modifier > max) ? -max : -modifier;
    }

    public int getIMPTHMod() {
        int max = 2;
        int modifier = impThisTurn + impLastTurn;
        modifier = modifier - (modifier % 3);
        modifier = modifier / 3;
        return (modifier > max) ? max : modifier;
    }

    /**
     * returns whether the unit is a military unit (as opposed to a civilian
     * unit).
     */
    public boolean isMilitary() {
        return military;
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
    public int getBARRating(int loc) {
        // normal armor has a BAR rating of 10
        return 10;
    }

    /**
     * does this Entity have BAR armor?
     *
     * @return
     */
    public boolean hasBARArmor(int loc) {
        return getBARRating(loc) < 10;
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
     * Possibly do a ICE-Engine stall PSR (only intended for Mechs, both
     * Industrial and Battle).
     *
     * @param vPhaseReport the <code>Vector<Report></code> containing the phase reports
     * @return a Vector<Report> containing the passed in reports, and any
     * additional ones
     */
    public Vector<Report> doCheckEngineStallRoll(Vector<Report> vPhaseReport) {
        return vPhaseReport;
    }

    /**
     * Check for unstalling of this Entity's engine (only used for ICE-powered
     * 'Mechs).
     *
     * @param vPhaseReport the <code>Vector<Report></code> containing the phase reports
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
        return (getElevation() == 0)
               && ((getMovementMode() == EntityMovementMode.NAVAL)
                   || (getMovementMode() == EntityMovementMode.HYDROFOIL) || (getMovementMode() == EntityMovementMode
                .SUBMARINE));
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

    public synchronized void setQuirks(Quirks quirks) {
        this.quirks = quirks;
    }

    /**
     * Retrieves the quirks object for entity. DO NOT USE this to check boolean
     * options, as it will not check game options for quirks. Use
     * entity#hasQuirk instead
     *
     * @return
     */
    public synchronized Quirks getQuirks() {
        return quirks;
    }

    public boolean hasQuirk(String name) {
        if ((null == game)
            || !game.getOptions().booleanOption("stratops_quirks")) {
            return false;
        }
        return quirks.booleanOption(name);
    }

    public PartialRepairs getPartialRepairs() {
        return partReps;
    }

    public void clearPartialRepairs() {
        for (Enumeration<IOptionGroup> i = partReps.getGroups(); i
                .hasMoreElements(); ) {
            IOptionGroup group = i.nextElement();
            for (Enumeration<IOption> j = group.getOptions(); j
                    .hasMoreElements(); ) {
                IOption option = j.nextElement();
                option.clearValue();
            }
        }

    }

    /**
     * count all the quirks for this unit, positive and negative
     */
    public int countQuirks() {
        int count = 0;

        if ((null == game)
            || !game.getOptions().booleanOption("stratops_quirks")) {
            return count;
        }

        for (Enumeration<IOptionGroup> i = quirks.getGroups(); i
                .hasMoreElements(); ) {
            IOptionGroup group = i.nextElement();
            for (Enumeration<IOption> j = group.getOptions(); j
                    .hasMoreElements(); ) {
                IOption quirk = j.nextElement();

                if (quirk.booleanValue()) {
                    count++;
                }
            }
        }

        return count;
    }
    
    public int countWeaponQuirks() {
        int count = 0;

        if ((null == game)
            || !game.getOptions().booleanOption("stratops_quirks")) {
            return count;
        }
        
        for (Mounted m : getEquipment()) {
            count += m.countQuirks();
        }
        return count;
    }

    public int countPartialRepairs() {
        int count = 0;
        for (Enumeration<IOptionGroup> i = partReps.getGroups(); i
                .hasMoreElements(); ) {
            IOptionGroup group = i.nextElement();
            for (Enumeration<IOption> j = group.getOptions(); j
                    .hasMoreElements(); ) {
                IOption partRep = j.nextElement();

                if (partRep.booleanValue()) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * count the quirks for this unit, for a given group name
     */
    public int countQuirks(String grpKey) {
        int count = 0;

        if ((null == game)
            || !game.getOptions().booleanOption("stratops_quirks")) {
            return count;
        }

        for (Enumeration<IOptionGroup> i = quirks.getGroups(); i
                .hasMoreElements(); ) {
            IOptionGroup group = i.nextElement();
            if (!group.getKey().equalsIgnoreCase(grpKey)) {
                continue;
            }
            for (Enumeration<IOption> j = group.getOptions(); j
                    .hasMoreElements(); ) {
                IOption quirk = j.nextElement();

                if (quirk.booleanValue()) {
                    count++;
                }
            }
        }

        return count;
    }

    /**
     * Returns a string of all the quirk "codes" for this entity, using sep as
     * the separator
     */
    public String getQuirkList(String sep) {
        StringBuffer qrk = new StringBuffer();

        if ((null == game)
            || !game.getOptions().booleanOption("stratops_quirks")) {
            return qrk.toString();
        }

        if (null == sep) {
            sep = "";
        }

        for (Enumeration<IOptionGroup> i = quirks.getGroups(); i
                .hasMoreElements(); ) {
            IOptionGroup group = i.nextElement();
            for (Enumeration<IOption> j = group.getOptions(); j
                    .hasMoreElements(); ) {
                IOption quirk = j.nextElement();
                if (quirk.booleanValue()) {
                    if (qrk.length() > 0) {
                        qrk.append(sep);
                    }
                    qrk.append(quirk.getName());
                    if ((quirk.getType() == IOption.STRING)
                        || (quirk.getType() == IOption.CHOICE)
                        || (quirk.getType() == IOption.INTEGER)) {
                        qrk.append(" ").append(quirk.stringValue());
                    }
                }
            }
        }
        return qrk.toString();
    }

    /**
     * Returns the forward firing arc for this entity - overrided by some units
     */
    public int getForwardArc() {
        return Compute.ARC_FORWARD;
    }

    /**
     * Returns the rear firing arc for this entity - overrided by some units
     */
    public int getRearArc() {
        return Compute.ARC_REAR;
    }

    /**
     * returns a description to the current sensing range of the active sensor
     */
    public String getSensorDesc() {
        if (null == getActiveSensor()) {
            return "none";
        }
        int bracket = Compute.getSensorBracket(getSensorCheck());
        int range = getActiveSensor().getRangeByBracket();
        int maxSensorRange = bracket * range;
        int minSensorRange = Math.max((bracket - 1) * range, 0);
        if (game.getOptions().booleanOption("inclusive_sensor_range")) {
            minSensorRange = 0;
        }
        return getActiveSensor().getDisplayName() + " (" + minSensorRange + "-"
               + maxSensorRange + ")";
    }

    public boolean isAirborne() {
        return (getAltitude() > 0)
               || (getMovementMode() == EntityMovementMode.AERODYNE)
               || (getMovementMode() == EntityMovementMode.SPHEROID);
    }

    public boolean isSpaceborne() {
        // for now if you are in space, you are spaceborne, but this will become
        // more complicated when
        // we start adding multiple maps to the same game and so I should try to
        // replace most calls to
        // game.getBoard().inSpace() with this one
        return game.getBoard().inSpace();
    }

    /**
     * is the unit flying Nape of the Earth? (i.e. one elevation above ground)
     */
    public boolean isNOE() {

        if (!isAirborne()) {
            return false;
        }
        if (game.getBoard().inAtmosphere()) {
            return (1 == (getAltitude() - game.getBoard().getHex(getPosition())
                    .ceiling(true)));
        }
        if (game.getBoard().onGround()) {
            return 1 == getAltitude();
        }
        return false;
    }

    public int getStartingPos() {
        return getStartingPos(true);
    }
    
    public int getStartingPos(boolean inheritFromOwner) {
        if (inheritFromOwner && startingPos == Board.START_NONE) {
            return owner.getStartingPos();
        }
        return startingPos;
    }

    public void setStartingPos(int i) {
        startingPos = i;
    }

    public int getAltitude() {
        return altitude;
    }

    public void setAltitude(int a) {
        altitude = a;
    }

    public boolean getUseManualBV() {
        return useManualBV;
    }

    public void setUseManualBV(boolean bv) {
        useManualBV = bv;
    }

    public int getManualBV() {
        return manualBV;
    }

    public void setManualBV(int bv) {
        manualBV = bv;
    }

    /**
     * produce an int array of the number of bombs of each type based on the
     * current bomblist
     *
     * @return
     */
    public int[] getBombLoadout() {
        int[] loadout = new int[BombType.B_NUM];
        for (Mounted bomb : getBombs()) {
            if ((bomb.getUsableShotsLeft() > 0)
                && (bomb.getType() instanceof BombType)) {
                int type = ((BombType) bomb.getType()).getBombType();
                loadout[type] = loadout[type] + 1;
            }
        }
        return loadout;
    }

    /**
     * Start of Battle Force Conversion Methods
     */

    public int getBattleForcePoints() {
        return 0;
    }

    public long getBattleForceMovementPoints() {
        int baseBFMove = getWalkMP();
        return baseBFMove;
    }

    public long getBattleForceJumpPoints() {
        return 0;
    }

    /**
     * Get the movement mode of the entity and return it as a battle force
     * string.
     */
    public String getMovementModeAsBattleForceString() {
        switch (getMovementMode()) {
            case NONE:
            case BIPED:
            case BIPED_SWIM:
            case QUAD:
            case QUAD_SWIM:
                return "";
            case TRACKED:
                return "t";
            case WHEELED:
                return "w";
            case HOVER:
                return "h";
            case VTOL:
                return "v";
            case NAVAL:
            case HYDROFOIL:
                return "n";
            case SUBMARINE:
            case INF_UMU:
                return "s";
            case INF_LEG:
                return "f";
            case INF_MOTORIZED:
                return "m";
            case INF_JUMP:
                return "j";
            case WIGE:
                return "g";
            case AERODYNE:
                return "a";
            case SPHEROID:
                return "p";
            default:
                return "ERROR";
        }
    }

    /**
     * Returns the Battle Force Movement string this is used in a battle force
     * game
     *
     * @return
     */
    public String getBattleForceMovement() {
        StringBuilder result = new StringBuilder();

        long jumpPoints = getBattleForceJumpPoints();
        long walkPoints = getBattleForceMovementPoints();

        result.append(walkPoints);
        result.append(getMovementModeAsBattleForceString());

        if (jumpPoints == walkPoints) {
            result.append("j");
        } else if (jumpPoints > 0) {
            result.append("/");
            result.append(jumpPoints);
            result.append("j");
        }

        return result.toString();
    }

    public int getBattleForceArmorPoints() {
        double armorPoints = 0;

        for (int loc = 0; loc < locations(); loc++) {
            double armorMod = 1;
            switch (getArmorType(loc)) {
                case EquipmentType.T_ARMOR_COMMERCIAL:
                    armorMod = .5;
                    break;
                case EquipmentType.T_ARMOR_INDUSTRIAL:
                case EquipmentType.T_ARMOR_HEAVY_INDUSTRIAL:
                    armorMod = getBARRating(0) / 10;
                    break;
                case EquipmentType.T_ARMOR_FERRO_LAMELLOR:
                    armorMod = 1.2;
                    break;
                case EquipmentType.T_ARMOR_HARDENED:
                    armorMod = 1.5;
                    break;
                case EquipmentType.T_ARMOR_REFLECTIVE:
                case EquipmentType.T_ARMOR_REACTIVE:
                    armorMod = .75;
                    break;
            }
            armorPoints += Math.ceil(getArmor(loc) * armorMod);

        }
        if (this.hasModularArmor()) {
            // Modular armor is always "regular" armor
            for (Mounted mount : this.getEquipment()) {
                if (!mount.isDestroyed()
                    && (mount.getType() instanceof MiscType)
                    && ((MiscType) mount.getType())
                        .hasFlag(MiscType.F_MODULAR_ARMOR)) {
                    armorPoints += 10;
                }
            }
        }

        return (int) Math.round(armorPoints / 30);
    }

    /**
     * only used for Aerospace and Dropships
     *
     * @return
     */
    public String getBattleForceDamageThresholdString() {
        return "";
    }

    /**
     * this will be unit specific
     *
     * @return
     */
    public int getBattleForceStructurePoints() {
        return 0;
    }

    /**
     * @param range
     * @return
     */
    public int getBattleForceStandardWeaponsDamage(int range) {
        return getBattleForceStandardWeaponsDamage(range, AmmoType.T_NA, false,
                                                   false);
    }

    /**
     * @param range
     * @param ammoType
     * @return
     */
    public int getBattleForceStandardWeaponsDamage(int range, int ammoType) {
        return getBattleForceStandardWeaponsDamage(range, ammoType, false,
                                                   false);
    }

    /**
     * @param range
     * @param ignoreHeat
     * @param ignoreSpecialAbilities
     * @return
     */
    public int getBattleForceStandardWeaponsDamage(int range,
                                                   boolean ignoreHeat, boolean ignoreSpecialAbilities) {
        return getBattleForceStandardWeaponsDamage(range, AmmoType.T_NA,
                                                   ignoreHeat, ignoreSpecialAbilities);
    }

    /**
     * @param range
     * @param ammoType
     * @param ignoreHeat           if set to true then heat modifier is not used on damage
     * @param ignoreSpecialAbility calculate special attacks into total damage if set to true
     * @return
     */
    public int getBattleForceStandardWeaponsDamage(int range, int ammoType,
                                                   boolean ignoreHeat, boolean ignoreSpecialAbility) {
        double totalDamage = 0;
        double frontArcWeaponsTotalDamage = 0;
        double rearArcWeaponsTotalDamage = 0;
        double totalHeat = 0;
        boolean hasArtemis = false;
        boolean hasTC = hasTargComp();
        double baseDamage = 0;

        TreeSet<String> weaponsUsed = new TreeSet<String>();

        ArrayList<Mounted> weaponsList = getWeaponList();

        for (int pos = 0; pos < weaponList.size(); pos++) {
            double damageModifier = 1;
            double weaponCount = 1;
            double minRangeDamageModifier = 1;
            hasArtemis = false;
            Mounted mount = weaponsList.get(pos);
            if ((mount == null) || mount.isRearMounted()
                || weaponsUsed.contains(mount.getName())) {
                continue;
            }

            WeaponType weapon = (WeaponType) mount.getType();

            if ((weapon.getLongRange() < range) && !(weapon instanceof ISLAC5)
                && !(weapon instanceof ATMWeapon)
                && !(weapon instanceof MMLWeapon)) {
                continue;
            }

            if ((ammoType != AmmoType.T_NA)
                && (weapon.getAmmoType() != ammoType)) {
                continue;
            }

            if ((weapon.getAmmoType() == AmmoType.T_INARC)
                || (weapon.getAmmoType() == AmmoType.T_NARC)) {
                continue;
            }

            if (weapon.hasFlag(WeaponType.F_ARTILLERY)) {
                // Each Artillery weapon is separately accounted for
                continue;
            }

            // Check ammo weapons first since they had a hidden modifier
            if ((weapon.getAmmoType() != AmmoType.T_NA)
                && !weapon.hasFlag(WeaponType.F_ONESHOT)) {
                weaponsUsed.add(weapon.getName());
                for (int nextPos = pos + 1; nextPos < weaponList.size(); nextPos++) {
                    Mounted nextWeapon = weaponList.get(nextPos);

                    if ((nextWeapon == null) || nextWeapon.isRearMounted()) {
                        continue;
                    }

                    if (nextWeapon.getType().equals(weapon)) {
                        weaponCount++;
                    }

                }
                int ammoCount = 0;
                // Check if they have enough ammo for all the guns to last at
                // least 10 rounds
                for (Mounted ammo : getAmmo()) {

                    AmmoType at = (AmmoType) ammo.getType();
                    if ((at.getAmmoType() == weapon.getAmmoType())
                        && (at.getRackSize() == weapon.getRackSize())) {
                        // RACs are always fired on 6 shot so that means you
                        // need 6 times the ammo to avoid the ammo damage
                        // modifier
                        if (at.getAmmoType() == AmmoType.T_AC_ROTARY) {
                            ammoCount += at.getShots() / 6;
                        } else {
                            ammoCount += at.getShots();
                        }
                    }
                }

                if ((ammoCount / weaponCount) < 10) {
                    damageModifier *= .75;
                }
            }

            if (weapon.hasFlag(WeaponType.F_MISSILE)) {
                baseDamage = Compute.calculateClusterHitTableAmount(7,
                                                                    weapon.getRackSize());
                baseDamage *= weaponCount;
            } else {
                baseDamage = weapon.getDamage() * weaponCount;
            }

            if (range == Entity.BATTLEFORCESHORTRANGE) {
                int minRange = Math.min(6,
                                        Math.max(0, weapon.getMinimumRange()));
                minRangeDamageModifier *= battleForceMinRangeModifier[minRange];
            }
            int toHitMod = weapon.getToHitModifier() + 4;

            switch (weapon.getAmmoType()) {
                case AmmoType.T_AC_LBX:
                case AmmoType.T_AC_LBX_THB:
                    baseDamage = Compute.calculateClusterHitTableAmount(7,
                                                                        weapon.getRackSize()) * weaponCount;
                    toHitMod--;
                    break;
                case AmmoType.T_MRM:
                    Mounted mLinker = mount.getLinkedBy();
                    if (((mLinker != null)
                         && (mLinker.getType() instanceof MiscType)
                         && !mLinker.isDestroyed() && !mLinker.isMissing()
                         && !mLinker.isBreached() && mLinker.getType()
                                                            .hasFlag(MiscType.F_APOLLO))) {
                        toHitMod--;
                        baseDamage = Compute.calculateClusterHitTableAmount(6,
                                                                            weapon.getRackSize()) * weaponCount;
                    }
                    break;
                case AmmoType.T_LRM:
                    mLinker = mount.getLinkedBy();
                    if (((mLinker != null)
                         && (mLinker.getType() instanceof MiscType)
                         && !mLinker.isDestroyed() && !mLinker.isMissing()
                         && !mLinker.isBreached() && mLinker.getType()
                                                            .hasFlag(MiscType.F_ARTEMIS))) {
                        baseDamage = Compute.calculateClusterHitTableAmount(9,
                                                                            weapon.getRackSize()) * weaponCount;
                        hasArtemis = true;
                    } else if (((mLinker != null)
                                && (mLinker.getType() instanceof MiscType)
                                && !mLinker.isDestroyed() && !mLinker.isMissing()
                                && !mLinker.isBreached() && mLinker.getType()
                                                                   .hasFlag(MiscType.F_ARTEMIS_V))) {
                        baseDamage = Compute.calculateClusterHitTableAmount(10,
                                                                            weapon.getRackSize()) * weaponCount;
                        hasArtemis = true;
                    }
                    break;
                case AmmoType.T_SRM:
                    mLinker = mount.getLinkedBy();
                    if (((mLinker != null)
                         && (mLinker.getType() instanceof MiscType)
                         && !mLinker.isDestroyed() && !mLinker.isMissing()
                         && !mLinker.isBreached() && mLinker.getType()
                                                            .hasFlag(MiscType.F_ARTEMIS))) {
                        baseDamage = Compute.calculateClusterHitTableAmount(9,
                                                                            weapon.getRackSize()) * 2 * weaponCount;
                        hasArtemis = true;
                    } else if (((mLinker != null)
                                && (mLinker.getType() instanceof MiscType)
                                && !mLinker.isDestroyed() && !mLinker.isMissing()
                                && !mLinker.isBreached() && mLinker.getType()
                                                                   .hasFlag(MiscType.F_ARTEMIS_V))) {
                        baseDamage = Compute.calculateClusterHitTableAmount(10,
                                                                            weapon.getRackSize()) * 2 * weaponCount;
                        hasArtemis = true;
                    } else {
                        baseDamage = Compute.calculateClusterHitTableAmount(7,
                                                                            weapon.getRackSize()) * 2 * weaponCount;
                    }
                    break;
                case AmmoType.T_ATM:
                    minRangeDamageModifier = 1;
                    switch (range) {
                        case Entity.BATTLEFORCESHORTRANGE:
                            baseDamage = Compute
                                                 .calculateClusterHitTableAmount(9,
                                                                                 weapon.getRackSize())
                                         * weaponCount * 3;
                            break;
                        case Entity.BATTLEFORCEMEDIUMRANGE:
                            baseDamage = Compute
                                                 .calculateClusterHitTableAmount(9,
                                                                                 weapon.getRackSize())
                                         * weaponCount * 2;
                            break;
                        case Entity.BATTLEFORCELONGRANGE:
                            baseDamage = Compute
                                                 .calculateClusterHitTableAmount(9,
                                                                                 weapon.getRackSize())
                                         * weaponCount;
                            break;
                    }
                    break;
                case AmmoType.T_AC_ULTRA:
                case AmmoType.T_AC_ULTRA_THB:
                    damageModifier *= 1.5;
                    break;
                case AmmoType.T_HAG:
                    switch (range) {
                        case Entity.BATTLEFORCESHORTRANGE:
                            baseDamage = Compute
                                                 .calculateClusterHitTableAmount(9,
                                                                                 weapon.getRackSize())
                                         * weaponCount;
                            break;
                        case Entity.BATTLEFORCELONGRANGE:
                            baseDamage = Compute
                                                 .calculateClusterHitTableAmount(5,
                                                                                 weapon.getRackSize())
                                         * weaponCount;
                            break;
                        case Entity.BATTLEFORCEMEDIUMRANGE:
                            baseDamage = Compute
                                                 .calculateClusterHitTableAmount(7,
                                                                                 weapon.getRackSize())
                                         * weaponCount;
                            break;
                    }
                    break;
                case AmmoType.T_SRM_STREAK:
                    baseDamage = weapon.getRackSize() * 2 * weaponCount;
                    break;
                case AmmoType.T_AC_ROTARY:
                    baseDamage = Compute.calculateClusterHitTableAmount(7,
                                                                        weapon.getRackSize()) * weaponCount * 5;
                    break;

            }

            if (weapon instanceof ISSnubNosePPC) {
                switch (range) {
                    case Entity.BATTLEFORCESHORTRANGE:
                        baseDamage = 10;
                        break;
                    case Entity.BATTLEFORCELONGRANGE:
                        baseDamage = 0;
                        break;
                    case Entity.BATTLEFORCEMEDIUMRANGE:
                        baseDamage = 5;
                        break;
                }
            }

            if (weapon instanceof VariableSpeedPulseLaserWeapon) {
                switch (range) {
                    case Entity.BATTLEFORCESHORTRANGE:
                        toHitMod = 1;
                        break;
                    case Entity.BATTLEFORCEMEDIUMRANGE:
                        toHitMod = 2;
                        break;
                    case Entity.BATTLEFORCELONGRANGE:
                        toHitMod = 3;
                        break;
                }
            }

            damageModifier *= battleForceToHitModifier[toHitMod];

            // For those entities that has a capital weapon but
            if (weapon.isCapital()) {
                damageModifier *= 10;
            }

            if (weapon.hasFlag(WeaponType.F_ONESHOT)) {
                damageModifier *= .1;
            }

            // Targetting Computer
            if (hasTC && weapon.hasFlag(WeaponType.F_DIRECT_FIRE)
                && (weapon.getAmmoType() != AmmoType.T_AC_LBX)
                && (weapon.getAmmoType() != AmmoType.T_AC_LBX_THB)) {
                damageModifier *= 1.10;
            }

            if ((weapon.getAmmoType() == AmmoType.T_LRM)
                || (weapon.getAmmoType() == AmmoType.T_AC)
                || (weapon.getAmmoType() == AmmoType.T_LAC)
                || (weapon.getAmmoType() == AmmoType.T_SRM)) {
                double damage = baseDamage * damageModifier;

                // TODO if damage is greater than 10 then we do not add it to
                // the
                // standard damage it will be used in special weapons
                if (((damage < 10) && !ignoreSpecialAbility)
                    || (ignoreSpecialAbility && !hasArtemis)
                    || (!ignoreSpecialAbility && hasArtemis)) {

                    if (range == Entity.BATTLEFORCESHORTRANGE) {
                        damage *= minRangeDamageModifier;
                    }
                    frontArcWeaponsTotalDamage += damage;
                }
            } else if (weapon.hasFlag(WeaponType.F_PPC)) {
                Mounted mLinker = mount.getLinkedBy();
                if (range == Entity.BATTLEFORCESHORTRANGE) {
                    baseDamage *= minRangeDamageModifier;
                }
                // PPC Capacitors?
                if (((mLinker != null)
                     && (mLinker.getType() instanceof MiscType)
                     && !mLinker.isDestroyed() && !mLinker.isMissing()
                     && !mLinker.isBreached() && mLinker.getType().hasFlag(
                        MiscType.F_PPC_CAPACITOR))) {
                    frontArcWeaponsTotalDamage += ((baseDamage + 5) * .5)
                                                  * damageModifier;
                } else {
                    frontArcWeaponsTotalDamage += baseDamage * damageModifier;
                }
            } else if (weapon.getAmmoType() == AmmoType.T_MML) {
                double ammoDamage = 1;

                Mounted mLinker = mount.getLinkedBy();
                if (((mLinker != null)
                     && (mLinker.getType() instanceof MiscType)
                     && !mLinker.isDestroyed() && !mLinker.isMissing()
                     && !mLinker.isBreached() && mLinker.getType().hasFlag(
                        MiscType.F_ARTEMIS))) {
                    baseDamage = Compute.calculateClusterHitTableAmount(9,
                                                                        weapon.getRackSize()) * weaponCount;
                    hasArtemis = true;
                } else if (((mLinker != null)
                            && (mLinker.getType() instanceof MiscType)
                            && !mLinker.isDestroyed() && !mLinker.isMissing()
                            && !mLinker.isBreached() && mLinker.getType().hasFlag(
                        MiscType.F_ARTEMIS_V))) {
                    baseDamage = Compute.calculateClusterHitTableAmount(10,
                                                                        weapon.getRackSize()) * weaponCount;
                    hasArtemis = true;
                }

                switch (range) {
                    case Entity.BATTLEFORCESHORTRANGE:
                        ammoDamage = 2;
                        break;
                    case Entity.BATTLEFORCELONGRANGE:
                        ammoDamage = 1;
                        break;
                    case Entity.BATTLEFORCEMEDIUMRANGE:
                        ammoDamage = 1;
                        baseDamage = Math.round((baseDamage * 3) / 2);
                        break;
                }

                double damage = baseDamage * damageModifier;
                // if damage is greater than 10 then we do not add it to the
                // standard damage it will be used in special weapons
                if (((damage < 10) && !ignoreSpecialAbility)
                    || (ignoreSpecialAbility && !hasArtemis && (damage >= 10))
                    || (!ignoreSpecialAbility && hasArtemis)) {

                    frontArcWeaponsTotalDamage += damage * ammoDamage;
                }
            } else {
                if (range == Entity.BATTLEFORCESHORTRANGE) {
                    baseDamage *= minRangeDamageModifier;
                }
                frontArcWeaponsTotalDamage += baseDamage * damageModifier;
            }
        }

        totalDamage = Math.max(frontArcWeaponsTotalDamage,
                               rearArcWeaponsTotalDamage);

        totalHeat = getBattleForceTotalHeatGeneration(false) - 4;

        if ((totalHeat > getHeatCapacity()) && !ignoreHeat) {
            totalDamage = Math.ceil((totalDamage * getHeatCapacity())
                                    / totalHeat);
        }

        if (ignoreSpecialAbility && (totalDamage < 10)) {
            totalDamage = 0;
        } else if ((ammoType != AmmoType.T_NA)) {
            totalDamage = Math.round(totalDamage / 10);
        } else {
            totalDamage = Math.ceil(totalDamage / 10);
        }
        return (int) totalDamage;
    }

    public int getBattleForceTotalHeatGeneration(boolean allowRear) {
        int totalHeat = 0;

        // finish the max heat calculations
        if (this.getJumpMP() > 0) {
            totalHeat += getJumpHeat(getJumpMP());
        } else {
            if ((this instanceof Mech) && !((Mech) this).isIndustrial()) {
                totalHeat += getEngine().getRunHeat(this);
            }
        }

        for (Mounted mount : getWeaponList()) {
            WeaponType weapon = (WeaponType) mount.getType();
            if (weapon.hasFlag(WeaponType.F_ONESHOT)
                || (allowRear && !mount.isRearMounted())
                || (!allowRear && mount.isRearMounted())) {
                continue;
            }
            totalHeat += weapon.getHeat();
        }

        if ((this instanceof Mech) && hasWorkingMisc(MiscType.F_STEALTH, -1)) {
            totalHeat += 10;
        }

        return totalHeat;
    }

    public String getBattleForceOverHeatValue() {

        int standardDamageValue = 0;
        int damageValueNoHeat = 0;

        int totalHeat = getBattleForceTotalHeatGeneration(false) - 4;

        if (getHeatCapacity() >= totalHeat) {
            return "None";
        }

        standardDamageValue = getBattleForceStandardWeaponsDamage(
                Entity.BATTLEFORCEMEDIUMRANGE, false, true);

        if (standardDamageValue <= 0) {
            standardDamageValue = getBattleForceStandardWeaponsDamage(
                    Entity.BATTLEFORCESHORTRANGE, false, true);
            damageValueNoHeat = getBattleForceStandardWeaponsDamage(
                    Entity.BATTLEFORCESHORTRANGE, true, true);
        } else {
            damageValueNoHeat = getBattleForceStandardWeaponsDamage(
                    Entity.BATTLEFORCEMEDIUMRANGE, true, true);
        }

        if (damageValueNoHeat > standardDamageValue) {
            return Integer.toString(Math.min(4, damageValueNoHeat
                                                - standardDamageValue));
        }
        return "None";
    }

    public String getBattleForceSpecialAbilites() {
        return "None";
    }

    public int getBattleForceSize() {
        // the default BF Size is for ground Combat elements. Other types will
        // need to override this
        // The tables are on page 356 of StartOps
        if (getWeight() < 40) {
            return 1;
        }
        if (getWeight() < 60) {
            return 2;
        }
        if (getWeight() < 80) {
            return 3;
        }

        return 4;
    }

    public Map<Integer, Coords> getSecondaryPositions() {
        return secondaryPositions;
    }

    /**
     * Checks to see if this unit has a functional Blue Shield Particle Field
     * Damper that is turned on
     *
     * @return <code>true</code> if the entity has a working, switched on blue
     * field <code>false</code> otherwise
     */
    public boolean hasActiveBlueShield() {
        if (!isShutDown()) {
            for (Mounted m : getMisc()) {
                EquipmentType type = m.getType();
                if ((type instanceof MiscType)
                    && type.hasFlag(MiscType.F_BLUE_SHIELD)
                    && m.curMode().equals("On")) {
                    return !(m.isDestroyed() || m.isMissing() || m.isBreached() || isShutDown());
                }
            }
        }
        return false;
    }

    public int getBlueShieldRounds() {
        return blueShieldRounds;
    }

    public boolean isDropping() {
        return isAirborne() && !(this instanceof Aero);
    }

    /**
     * does this unit have stealth armor?
     *
     * @return
     */
    public boolean hasStealth() {
        // only non-patchwork stealth actually works as stealth
        if (((getArmorType(1) == EquipmentType.T_ARMOR_STEALTH) || (getArmorType(1) == EquipmentType
                .T_ARMOR_STEALTH_VEHICLE))
            && !hasPatchworkArmor()) {
            return true;
        }
        return false;
    }

    /**
     * Computes and returns the power amplifier weight for this entity, if any.
     * Returns 0.0 if the entity needs no amplifiers due to engine type or not
     * carrying any weapons requiring them.
     *
     * @return the power amplifier weight in tons.
     */
    public double getPowerAmplifierWeight() {
        // If we're fusion- or fission-powered, we need no amplifiers to begin
        // with.
        if (engine.isFusion() || (engine.getEngineType() == Engine.FISSION)) {
            return 0.0;
        }
        // Otherwise we need to iterate over our weapons, find out which of them
        // require amplification, and keep a running weight total of those.
        double total = 0.0;
        for (Mounted m : getWeaponList()) {
            WeaponType wt = (WeaponType) m.getType();
            if ((wt.hasFlag(WeaponType.F_LASER) && (wt.getAmmoType() == AmmoType.T_NA))
                || wt.hasFlag(WeaponType.F_PPC)
                || wt.hasFlag(WeaponType.F_PLASMA)
                || wt.hasFlag(WeaponType.F_PLASMA_MFUK)
                || (wt.hasFlag(WeaponType.F_FLAMER) && (wt.getAmmoType() == AmmoType.T_NA))) {
                total += wt.getTonnage(this);
            }
        }
        // Finally use that total to compute and return the actual power
        // amplifier weight.
        return Math.ceil(total / 5) / 2;
    }

    public class EntityFluff implements Serializable {
        /**
         *
         */
        private static final long serialVersionUID = -8018098140016149185L;
        private String capabilities = "";
        private String overview = "";
        private String deployment = "";
        private String history = "";
        private String mmlImageFilePath = "";

        public EntityFluff() {
            // Constructor
        }

        public String getCapabilities() {
            return capabilities;
        }

        public void setCapabilities(String newCapabilities) {
            capabilities = newCapabilities;
        }

        public String getOverview() {
            return overview;
        }

        public void setOverview(String newOverview) {
            overview = newOverview;
        }

        public String getDeployment() {
            return deployment;
        }

        public void setDeployment(String newDeployment) {
            deployment = newDeployment;
        }

        public void setHistory(String newHistory) {
            history = newHistory;
        }

        public String getHistory() {
            return history;
        }

        public String getMMLImagePath() {
            return mmlImageFilePath;
        }

        public void setMMLImagePath(String filePath) {
            mmlImageFilePath = filePath;
        }


    }

    public Vector<Integer> getLoadedKeepers() {
        return loadedKeepers;
    }

    public void setLoadedKeepers(Vector<Integer> v) {
        loadedKeepers = v;
    }

    public int getExtraC3BV(int baseBV) {
        // extra from c3 networks. a valid network requires at least 2 members
        // some hackery and magic numbers here. could be better
        // also, each 'has' loops through all equipment. inefficient to do it 3
        // times
        int xbv = 0;
        if ((game != null)
            && ((hasC3MM() && (calculateFreeC3MNodes() < 2))
                || (hasC3M() && (calculateFreeC3Nodes() < 3))
                || (hasC3S() && (c3Master > NONE)) || (hasC3i() && (calculateFreeC3Nodes() < 5)))) {
            int totalForceBV = 0;
            totalForceBV += baseBV;
            for (Entity e : game.getC3NetworkMembers(this)) {
                if (!equals(e) && onSameC3NetworkAs(e)) {
                    totalForceBV += e.calculateBattleValue(true, true);
                }
            }
            double multiplier = 0.05;
            if (hasBoostedC3()) {
                multiplier = 0.07;
            }
            xbv += totalForceBV * multiplier;
        }
        return xbv;
    }

    public boolean hasUnloadedUnitsFromBays() {
        for (Transporter next : transports) {
            if ((next instanceof Bay)
                && (((Bay) next).getNumberUnloadedThisTurn() > 0)) {
                return true;
            }
        }
        return false;
    }

    public boolean getMovedBackwards() {
        return movedBackwards;
    }

    public void setMovedBackwards(boolean back) {
        movedBackwards = back;
    }

    public boolean isPowerReverse() {
        return isPowerReverse;
    }

    public void setPowerReverse(boolean isPowerReverse) {
        this.isPowerReverse = isPowerReverse;
    }

    public void setHardenedArmorDamaged(HitData hit, boolean damaged) {
        hardenedArmorDamaged[hit.getLocation()] = damaged;
    }

    /**
     * do we have a half-hit hardened armor point in the location struck by
     * this?
     *
     * @param hit
     * @return
     */
    public boolean isHardenedArmorDamaged(HitData hit) {
        return hardenedArmorDamaged[hit.getLocation()];
    }

    public void setLocationBlownOff(int loc, boolean damaged) {
        locationBlownOff[loc] = damaged;
    }

    public boolean isLocationBlownOff(int loc) {
        return locationBlownOff[loc];
    }

    /**
     * Marks the location as blown off in the current phase. This should be
     * called together with {@link #setLocationBlownOff(int, boolean) } whenever
     * a location gets blown off <em>during play</em>, to allow relevant methods
     * (notably {@link #isLocationBad(int) }) to distinguish between fresh and
     * preexisting damage. A location's "newly blown off" status resets with the
     * next call to {@link #applyDamage() }.
     *
     * @param loc     Subclass-dependent code for the location.
     * @param damaged The location's "recently blown off" status.
     */
    public void setLocationBlownOffThisPhase(int loc, boolean damaged) {
        locationBlownOffThisPhase[loc] = damaged;
    }

    /**
     * Has the indicated location been blown off this phase (as opposed to
     * either earlier or not at all)?
     *
     * @param loc Subclass-dependent code for the location.
     * @return The locations "recently blown off" status.
     */
    public boolean isLocationBlownOffThisPhase(int loc) {
        return locationBlownOffThisPhase[loc];
    }

    /**
     * does this entity have patchwork armor?
     *
     * @return
     */
    public boolean hasPatchworkArmor() {
        int type = armorType[0];
        int level = armorTechLevel[0];
        for (int i = 1; i < locations(); i++) {
            if ((armorType[i] != type) || (armorTechLevel[i] != level)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasHardenedArmor() {
        for (int i = 0; i < locations(); i++) {
            if ((armorType[i] == EquipmentType.T_ARMOR_HARDENED)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the number of turns MASC has been used continuously.
     * <p/>
     * This method should <strong>only</strong> be used during serialization.
     *
     * @return the <code>int</code> number of turns MASC has been used.
     */
    public int getMASCTurns() {
        return nMASCLevel;
    }

    /**
     * Set the number of turns MASC has been used continuously.
     * <p/>
     * This method should <strong>only</strong> be used during deserialization.
     *
     * @param turns The <code>int</code> number of turns MASC has been used.
     */
    public void setMASCTurns(int turns) {
        nMASCLevel = turns;
    }

    /**
     * Determine if MASC has been used this turn.
     *
     * @return <code>true</code> if MASC has been used.
     */
    public boolean isMASCUsed() {
        return usedMASC;
    }

    /**
     * Set whether MASC has been used.
     * <p/>
     * This method should <strong>only</strong> be used during deserialization.
     *
     * @param used The <code>boolean</code> whether MASC has been used.
     */
    public void setMASCUsed(boolean used) {
        usedMASC = used;
    }

    public int getMASCTarget() {
        if (game.getOptions().booleanOption("alternate_masc_enhanced")) {
            return ALTERNATE_MASC_FAILURE_ENHANCED[nMASCLevel];
        } else if (game.getOptions().booleanOption("alternate_masc")) {
            return ALTERNATE_MASC_FAILURE[nMASCLevel];
        } else {
            return MASC_FAILURE[nMASCLevel];
        }
    }

    /**
     * This function cheks for masc failure.
     *
     * @param md         the movement path.
     * @param vDesc      the description off the masc failure. used as output.
     * @param vCriticals contains tuple of intiger and critical slot. used as output.
     * @return true if there was a masc failure.
     */
    public boolean checkForMASCFailure(MovePath md, Vector<Report> vDesc,
                                       HashMap<Integer, List<CriticalSlot>> vCriticals) {
        if (md.hasActiveMASC()) {
            boolean bFailure = false;

            // If usedMASC is already set, then we've already checked MASC
            // this turn. If we succeded before, return false.
            // If we failed before, the MASC was destroyed, and we wouldn't
            // have gotten here (hasActiveMASC would return false)
            if (!usedMASC) {
                Mounted masc = getMASC();
                Mounted superCharger = getSuperCharger();
                bFailure = doMASCCheckFor(masc, vDesc, vCriticals);
                boolean bSuperChargeFailure = doMASCCheckFor(superCharger,
                                                             vDesc, vCriticals);
                usedMASC = true;
                return bFailure || bSuperChargeFailure;
            }
        }
        return false;
    }

    /**
     * check one masc system for failure
     *
     * @param masc
     * @param vDesc
     * @param vCriticals
     * @return
     */
    private boolean doMASCCheckFor(Mounted masc, Vector<Report> vDesc,
                                   HashMap<Integer, List<CriticalSlot>> vCriticals) {
        if (masc != null) {
            boolean bFailure = false;
            int nRoll = Compute.d6(2);
            if (masc.getType().hasSubType(MiscType.S_SUPERCHARGER)
                && (((this instanceof Mech) && ((Mech) this).isIndustrial())
                    || (this instanceof SupportTank) || (this instanceof SupportVTOL))) {
                nRoll -= 1;
            }
            usedMASC = true;
            Report r = new Report(2365);
            r.subject = getId();
            r.addDesc(this);
            r.add(masc.getName());
            vDesc.addElement(r);
            r = new Report(2370);
            r.subject = getId();
            r.indent();
            r.add(getMASCTarget());
            r.add(nRoll);

            if (nRoll < getMASCTarget()) {
                // uh oh
                bFailure = true;
                r.choose(false);
                vDesc.addElement(r);

                if (((MiscType) (masc.getType()))
                        .hasSubType(MiscType.S_SUPERCHARGER)) {
                    if (masc.getType().hasFlag(MiscType.F_MASC)) {
                        masc.setHit(true);
                        masc.setMode("Off");
                    }
                    // do the damage - engine crits
                    int hits = 0;
                    int roll = Compute.d6(2);
                    r = new Report(6310);
                    r.subject = getId();
                    r.add(roll);
                    r.newlines = 0;
                    vDesc.addElement(r);
                    if (roll <= 7) {
                        // no effect
                        r = new Report(6005);
                        r.subject = getId();
                        r.newlines = 0;
                        vDesc.addElement(r);
                    } else if ((roll >= 8) && (roll <= 9)) {
                        hits = 1;
                        r = new Report(6315);
                        r.subject = getId();
                        r.newlines = 0;
                        vDesc.addElement(r);
                    } else if ((roll >= 10) && (roll <= 11)) {
                        hits = 2;
                        r = new Report(6320);
                        r.subject = getId();
                        r.newlines = 0;
                        vDesc.addElement(r);
                    } else if (roll == 12) {
                        hits = 3;
                        r = new Report(6325);
                        r.subject = getId();
                        r.newlines = 0;
                        vDesc.addElement(r);
                    }
                    if (this instanceof Mech) {
                        vCriticals.put(new Integer(Mech.LOC_CT),
                                       new LinkedList<CriticalSlot>());
                        for (int i = 0; (i < 12) && (hits > 0); i++) {
                            CriticalSlot cs = getCritical(Mech.LOC_CT, i);
                            if ((cs.getType() == CriticalSlot.TYPE_SYSTEM)
                                && (cs.getIndex() == Mech.SYSTEM_ENGINE)
                                && cs.isHittable()) {
                                vCriticals.get(new Integer(Mech.LOC_CT))
                                          .add(cs);
                                hits--;
                            }
                        }
                    } else {
                        // this must be a Tank
                        Tank tank = (Tank) this;
                        boolean vtolStabilizerHit = (this instanceof VTOL)
                                                    && tank.isStabiliserHit(VTOL.LOC_ROTOR);
                        boolean minorMovementDamage = tank
                                .hasMinorMovementDamage();
                        boolean moderateMovementDamage = tank
                                .hasModerateMovementDamage();
                        boolean heavyMovementDamage = tank
                                .hasHeavyMovementDamage();
                        vCriticals.put(new Integer(Tank.LOC_BODY),
                                       new LinkedList<CriticalSlot>());
                        vCriticals.put(new Integer(-1),
                                       new LinkedList<CriticalSlot>());
                        if (tank instanceof VTOL) {
                            vCriticals.put(new Integer(VTOL.LOC_ROTOR),
                                           new LinkedList<CriticalSlot>());
                        }
                        for (int i = 0; i < hits; i++) {
                            if (tank instanceof VTOL) {
                                if (vtolStabilizerHit) {
                                    vCriticals.get(new Integer(Tank.LOC_BODY))
                                              .add(new CriticalSlot(
                                                      CriticalSlot.TYPE_SYSTEM,
                                                      Tank.CRIT_ENGINE));
                                } else {
                                    vCriticals
                                            .get(new Integer(VTOL.LOC_ROTOR))
                                            .add(new CriticalSlot(
                                                    CriticalSlot.TYPE_SYSTEM,
                                                    VTOL.CRIT_FLIGHT_STABILIZER));
                                    vtolStabilizerHit = true;
                                }
                            } else {
                                if (heavyMovementDamage) {
                                    vCriticals.get(new Integer(Tank.LOC_BODY))
                                              .add(new CriticalSlot(
                                                      CriticalSlot.TYPE_SYSTEM,
                                                      Tank.CRIT_ENGINE));
                                } else if (moderateMovementDamage) {
                                    // HACK: we abuse the criticalslot item to
                                    // signify the calling function to deal
                                    // movement damage
                                    vCriticals
                                            .get(new Integer(-1))
                                            .add(new CriticalSlot(
                                                    CriticalSlot.TYPE_SYSTEM, 3));
                                    heavyMovementDamage = true;
                                } else if (minorMovementDamage) {
                                    // HACK: we abuse the criticalslot item to
                                    // signify the calling function to deal
                                    // movement damage
                                    vCriticals
                                            .get(new Integer(-1))
                                            .add(new CriticalSlot(
                                                    CriticalSlot.TYPE_SYSTEM, 2));
                                    moderateMovementDamage = true;
                                } else {
                                    // HACK: we abuse the criticalslot item to
                                    // signify the calling function to deal
                                    // movement damage
                                    vCriticals
                                            .get(new Integer(-1))
                                            .add(new CriticalSlot(
                                                    CriticalSlot.TYPE_SYSTEM, 1));
                                    minorMovementDamage = true;
                                }
                            }
                        }
                    }

                } else {
                    // do the damage.
                    // random crit on each leg, but MASC is not destroyed
                    for (int loc = 0; loc < locations(); loc++) {
                        if (locationIsLeg(loc)
                            && (getHittableCriticals(loc) > 0)) {
                            CriticalSlot slot = null;
                            do {
                                int slotIndex = Compute
                                        .randomInt(getNumberOfCriticals(loc));
                                slot = getCritical(loc, slotIndex);
                            } while ((slot == null) || !slot.isHittable());
                            vCriticals.put(new Integer(loc),
                                           new LinkedList<CriticalSlot>());
                            vCriticals.get(new Integer(loc)).add(slot);
                        }
                    }
                }
                // failed a PSR, check for stalling
                doCheckEngineStallRoll(vDesc);
            } else {
                r.choose(true);
                vDesc.addElement(r);
            }
            return bFailure;
        }
        return false;
    }

    /**
     * get non-supercharger MASC mounted on this entity
     *
     * @return
     */
    public Mounted getMASC() {
        for (Mounted m : getMisc()) {
            MiscType mtype = (MiscType) m.getType();
            if (mtype.hasFlag(MiscType.F_MASC) && m.isReady()
                && !mtype.hasSubType(MiscType.S_SUPERCHARGER)
                && !mtype.hasSubType(MiscType.S_JETBOOSTER)) {
                return m;
            }
        }
        return null;
    }

    /**
     * get a supercharger mounted on this mech
     *
     * @return
     */
    public Mounted getSuperCharger() {
        for (Mounted m : getMisc()) {
            MiscType mtype = (MiscType) m.getType();
            if (mtype.hasFlag(MiscType.F_MASC) && m.isReady()
                && mtype.hasSubType(MiscType.S_SUPERCHARGER)) {
                return m;
            }
        }
        return null;
    }

    public abstract int getEngineHits();

    /**
     * Returns the number of destroyed jump jets.
     */
    public int damagedJumpJets() {
        int jumpJets = 0;
        for (Mounted mounted : getMisc()) {
            EquipmentType etype = mounted.getType();
            if (!mounted.isDestroyed()) {
                continue;
            }
            if (etype.hasFlag(MiscType.F_JUMP_JET)) {
                jumpJets++;
            }
        }
        return jumpJets;
    }

    public abstract String getLocationDamage(int loc);

    /**
     * This method returns a true if the unit can reasonably escape from the
     * board. It can be used to determine whether some non-destroyed units
     * should be considered possible salvage. 
     *
     * @return
     */
    public boolean canEscape() {        
        if(null == getCrew()) {
            return false;
        }
        //if the crew is unconscious, dead, or ejected, no escape
        if(getCrew().isUnconscious() 
                || getCrew().isDead() 
                || (getCrew().isEjected() && !(this instanceof EjectedCrew))) {
            return false;
        }
        
        //what else? If its permaneantly immobilized or shutdown it can't escape
        //TODO: should stalled and stuck be here?
        return !isPermanentlyImmobilized(false) && !isShutDown();
    }

    /**
     * Returns TRUE if the entity meets the requirements for crippling damage as
     * detailed in TW pg 258.
     *
     * @return boolean
     */
    public abstract boolean isCrippled();

    /**
     * Returns TRUE if the entity meets the requirements for crippling damage as
     * detailed in TW pg 258. Excepting dead or non-existing crew issues
     *
     * @return boolean
     */
    public abstract boolean isCrippled(boolean checkCrew);

    /**
     * Returns TRUE if the entity has been heavily damaged.
     *
     * @return boolean
     */
    public abstract boolean isDmgHeavy();

    /**
     * Returns TRUE if the entity has been moderately damaged.
     *
     * @return boolean
     */
    public abstract boolean isDmgModerate();

    /**
     * Returns TRUE if the entity has been lightly damaged.
     *
     * @return boolean
     */
    public abstract boolean isDmgLight();

    /**
     * Returns the entity's current damage level.
     *
     * @return DMG_CRIPLED, DMG_HEAVY, DMG_MODERATE, DMG_LIGHT or DMG_NONE.
     */
    public int getDamageLevel() {
        return getDamageLevel(true);
    }

    /**
     * Returns the entity's current damage level.
     *
     * @return DMG_CRIPLED, DMG_HEAVY, DMG_MODERATE, DMG_LIGHT or DMG_NONE.
     */
    public int getDamageLevel(boolean checkCrew) {
        if (isCrippled(checkCrew)) {
            return DMG_CRIPPLED;
        }
        if (isDmgHeavy()) {
            return DMG_HEAVY;
        }
        if (isDmgModerate()) {
            return DMG_MODERATE;
        }
        if (isDmgLight()) {
            return DMG_LIGHT;
        }
        return DMG_NONE;
    }

    // Make a UUID for this entity and assign it to entity's String c3UUID
    public void setC3UUID() {
        UUID id = UUID.randomUUID();
        // check for the very rare chance of getting same id
        /*
         * while(null != unitIds.get(id)) { id = UUID.randomUUID(); }
         */

        setC3UUIDAsString(id.toString());
    }

    public void setC3UUIDAsString(String c3id) {
        c3UUID = c3id;
    }

    public String getC3UUIDAsString() {
        return c3UUID;
    }

    public void setC3MasterIsUUIDAsString(String c3id) {
        c3MasterIsUUID = c3id;
    }

    public String getC3MasterIsUUIDAsString() {
        return c3MasterIsUUID;
    }

    public void setC3iNextUUIDAsString(int pos, String c3id) {
        c3iUUIDs[pos] = c3id;
    }

    public String getC3iNextUUIDAsString(int pos) {
        return c3iUUIDs[pos];
    }

    public int getFreeC3iUUID() {
        int pos = 0;
        while (c3iUUIDs[pos] != null) {
            pos++;
            if (pos >= MAX_C3i_NODES) {
                return -1;
            }
        }
        return pos;
    }

    /**
     * Indicates if a unit was physically struck (punch, kick, DFA, etc).
     *
     * @return
     */
    public boolean wasStruck() {
        return struck;
    }

    /**
     * Indicates if a unit was physically struck (punch, kick, DFA, etc).
     *
     * @return
     */
    public void setStruck(boolean struck) {
        this.struck = struck;
    }

    /**
     * Indicates if a unit has falling in the current phase.
     *
     * @return
     */
    public boolean hasFallen() {
        return fell;
    }

    /**
     * Indicates if a unit has falling in the current phase.
     *
     * @return
     */
    public void setFallen(boolean fell) {
        this.fell = fell;
    }

    /**
     * This is used to get an alternative cost that will be added to the
     * MechSummaryCache - at the moment it is primarily used to rework infantry
     * costs for MekHQ, but it could be applied to other unit types as well -
     * defaults to -1, so there is no confusion
     *
     * @return
     */
    public double getAlternateCost() {
        return -1;
    }

    /**
     * Are we trapped inside of a destroyed transport? If so we shouldn't count
     * for BV, which is why we have this check.
     */
    public boolean isTrapped() {
        if (getTransportId() != Entity.NONE) {
            Entity transport = game.getEntity(getTransportId());
            if (transport == null) {
                transport = game.getOutOfGameEntity(getTransportId());
            }
            if (transport.isDestroyed()) {
                return true;
            }
        }
        return false;
    }

    // Deal with per entity camo
    public void setCamoCategory(String name) {
        camoCategory = name;
    }

    public String getCamoCategory() {
        return camoCategory;
    }

    public void setCamoFileName(String name) {
        camoFileName = name;
    }

    public String getCamoFileName() {
        return camoFileName;
    }

    public boolean getSelfDestructing() {
        return selfDestructing;
    }

    public void setSelfDestructing(boolean tf) {
        selfDestructing = tf;
    }

    public boolean getSelfDestructInitiated() {
        return selfDestructInitiated;
    }

    public void setSelfDestructInitiated(boolean tf) {
        selfDestructInitiated = tf;
    }

    public void setIsJumpingNow(boolean jumped) {
        isJumpingNow = jumped;
    }

    public boolean getIsJumpingNow() {
        return isJumpingNow;
    }

    public void setTraitorId(int id) {
        traitorId = id;
    }

    public int getTraitorId() {
        return traitorId;
    }

    /**
     * Used to determine net velocity of ramming attack
     */
    public int sideTableRam(Coords src) {
        return sideTableRam(src, facing);
    }

    public int sideTableRam(Coords src, int facing) {
        int fa = (getPosition().degree(src) + ((6 - facing) * 60)) % 360;
        if (((fa > 30) && (fa <= 90)) || ((fa < 330) && (fa >= 270))) {
            return Aero.RAM_TOWARD_OBL;
        } else if ((fa > 150) && (fa < 210)) {
            return Aero.RAM_AWAY_DIR;
        } else if (((fa > 90) && (fa <= 150)) || ((fa < 270) && (fa >= 210))) {
            return Aero.RAM_AWAY_OBL;
        } else {
            return Aero.RAM_TOWARD_DIR;
        }
    }

    public void setArmorTonnage(double ton) {
        armorTonnage = ton;
    }

    public double getLabArmorTonnage() {
        return armorTonnage;
    }

    public int getLabTotalArmorPoints() {
        double armorPerTon = 16.0 * EquipmentType.getArmorPointMultiplier(
                armorType[0], armorTechLevel[0]);
        return (int) Math.floor(armorPerTon * armorTonnage);
    }

    public void loadDefaultCustomWeaponOrder() {
        WeaponOrderHandler.WeaponOrder weapOrder = WeaponOrderHandler.getWeaponOrder(
                getChassis(), getModel());

        if (weapOrder != null) {
            setWeaponSortOrder(weapOrder.orderType);
            setCustomWeaponOrder(weapOrder.customWeaponOrderMap);
        }
    }

    public void loadDefaultQuirks() {

        // Get a list of quirks for this entity.
        List<QuirkEntry> quirks = QuirksHandler.getQuirks(getChassis(),
                getModel());

        // If this unit has no quirks, we do not need to proceed further.
        if ((quirks == null) || quirks.isEmpty()) {
            return;
        }

        // System.out.println("Loading quirks for " + getChassis() + " " +
        // getModel());

        // Load all the unit's quirks.
        for (QuirkEntry q : quirks) {

            // System.out.print("  " + q.toLog() + "... ");

            // If the quirk doesn't have a location, then it is a unit quirk,
            // not a weapon quirk.
            if (StringUtil.isNullOrEmpty(q.getLocation())) {

                // Activate the unit quirk.
                if (getQuirks().getOption(q.getQuirk()) == null) {
                    System.out.println(q.toLog() + " failed for "
                                       + getChassis() + " " + getModel()
                                       + " - Invalid quirk!");
                    continue;
                }
                getQuirks().getOption(q.getQuirk()).setValue(true);
                // System.out.println("Loaded.");
                continue;
            }

            // Get the weapon in the indicated location and slot.
            // System.out.print("Getting CriticalSlot... ");
            CriticalSlot cs = getCritical(getLocationFromAbbr(q.getLocation()),
                                          q.getSlot());
            if (cs == null) {
                System.out.println(q.toLog() + " failed for " + getChassis()
                                   + " " + getModel() + " - Critical slot ("
                                   + q.getLocation() + "-" + q.getSlot()
                                   + ") did not load!");
                continue;
            }
            Mounted m = cs.getMount();
            if (m == null) {
                System.out.println(q.toLog() + " failed for " + getChassis()
                                   + " " + getModel() + " - Critical slot ("
                                   + q.getLocation() + "-" + q.getSlot() + ") is empty!");
                continue;
            }

            // Make sure this is a weapon.
            // System.out.print("Getting WeaponType... ");
            if (!(m.getType() instanceof WeaponType) 
                    && !(m.getType().hasFlag(MiscType.F_CLUB))) {
                System.out.println(q.toLog() + " failed for " + getChassis()
                                   + " " + getModel() + " - " + m.getName()
                                   + " is not a weapon!");
                continue;
            }

            // Make sure it is the weapon we expect.
            // System.out.print("Matching weapon... ");
            boolean matchFound = false;
            Enumeration<String> typeNames = m.getType().getNames();
            while (typeNames.hasMoreElements()) {
                String typeName = typeNames.nextElement();
                // System.out.print(typeName + "... ");
                if (typeName.equals(q.getWeaponName())) {
                    matchFound = true;
                    break;
                }
            }
            if (!matchFound) {
                System.out.println(q.toLog() + " failed for " + getChassis()
                                   + " " + getModel() + " - " + m.getType().getName()
                                   + " != " + q.getWeaponName());
                continue;
            }

            // Activate the weapon quirk.
            // System.out.print("Activating quirk... ");
            if (m.getQuirks().getOption(q.getQuirk()) == null) {
                System.out.println(q.toLog() + " failed for " + getChassis()
                                   + " " + getModel() + " - Invalid quirk!");
                continue;
            }
            m.getQuirks().getOption(q.getQuirk()).setValue(true);
            // System.out.println("Loaded.");
        }
    }

    public void newPhase(IGame.Phase phase) {
        for (Mounted m : getEquipment()) {
            m.newPhase(phase);
        }
        if (getCrew().isDoomed()) {
            getCrew().setDoomed(false);
            getCrew().setDead(true);
            if (this instanceof Tank) {
                setCarcass(true);
                ((Tank) this).immobilize();
            } else {
                setDestroyed(true);
            }
        }

    }

    /**
     * Checks to see if the entities' elevation is below the surface of a water
     * hex.
     *
     * @return True if the entity is underwater, else false.
     */
    public boolean isUnderwater() {
        IHex occupiedHex = game.getBoard().getHex(getPosition());
        if (occupiedHex.containsTerrain(Terrains.WATER)
            && (relHeight() < occupiedHex.surface())) {
            return true;
        }
        return false;
    }

    public int getTechLevelYear() {
        if (game != null) {
            return game.getOptions().intOption("year");
        }
        return year;
    }

    public int getTargetBay() {
        return targetBay;
    }

    public void setTargetBay(int tb) {
        targetBay = tb;
    }

    public abstract long getEntityType();

    /**
     * Given an Entity type, return the name of the major class it belongs to
     * (eg: Mech, Aero, Tank, Infantry).
     *
     * @param typeId The type Id to get a major name for
     * @return The major class name for the given type id
     */
    public static String getEntityMajorTypeName(long typeId) {
        if ((typeId & ETYPE_MECH) == ETYPE_MECH) {
            return "Mech";
        } else if ((typeId & ETYPE_AERO) == ETYPE_AERO) {
            return "Aero";
        } else if ((typeId & ETYPE_TANK) == ETYPE_TANK) {
            return "Tank";
        } else if ((typeId & ETYPE_INFANTRY) == ETYPE_INFANTRY) {
            return "Infantry";
        } else {
            return "Unknown";
        }
    }

    /**
     * Returns the specific entity type name for the given type id
     * (eg: Biped Mech, Conventional Fighter, VTOL).
     *
     * @param typeId
     * @return
     */
    public static String getEntityTypeName(long typeId) {

        if ((typeId & ETYPE_BIPED_MECH) == ETYPE_BIPED_MECH) {
            return "Biped Mech";
        } else if ((typeId & ETYPE_LAND_AIR_MECH) == ETYPE_LAND_AIR_MECH) {
            return "Landair Mech";
        } else if ((typeId & ETYPE_QUAD_MECH) == ETYPE_QUAD_MECH) {
            return "Quad Mech";
        } else if ((typeId & ETYPE_TRIPOD_MECH) == ETYPE_TRIPOD_MECH) {
            return "Tripod Mech";
        } else if ((typeId & ETYPE_ARMLESS_MECH) == ETYPE_ARMLESS_MECH) {
            return "Armless Mech";
        } else if ((typeId & ETYPE_MECH) == ETYPE_MECH) {
            return "Mech";
        } else if ((typeId & ETYPE_JUMPSHIP) == ETYPE_JUMPSHIP) {
            return "Jumpship";
        } else if ((typeId & ETYPE_WARSHIP) == ETYPE_WARSHIP) {
            return "Warship";
        } else if ((typeId & ETYPE_SPACE_STATION) == ETYPE_SPACE_STATION) {
            return "Space station";
        } else if ((typeId & ETYPE_CONV_FIGHTER) == ETYPE_CONV_FIGHTER) {
            return "Convetional Fighter";
        } else if ((typeId & ETYPE_FIXED_WING_SUPPORT) == ETYPE_FIXED_WING_SUPPORT) {
            return "Fixed Wing Support";
        } else if ((typeId & ETYPE_FIGHTER_SQUADRON) == ETYPE_FIGHTER_SQUADRON) {
            return "Fighter squadron";
        } else if ((typeId & ETYPE_SMALL_CRAFT) == ETYPE_SMALL_CRAFT) {
            return "Small craft";
        } else if ((typeId & ETYPE_DROPSHIP) == ETYPE_DROPSHIP) {
            return "Dropship";
        } else if ((typeId & ETYPE_TELEMISSILE) == ETYPE_TELEMISSILE) {
            return "Telemissile";
        } else if ((typeId & ETYPE_AERO) == ETYPE_AERO) {
            return "Aerospace fighter";
        } else if ((typeId & ETYPE_BATTLEARMOR) == ETYPE_BATTLEARMOR) {
            return "Battlearmor";
        } else if ((typeId & ETYPE_MECHWARRIOR) == ETYPE_MECHWARRIOR) {
            return "Mechwarrior";
        } else if ((typeId & ETYPE_PROTOMECH) == ETYPE_PROTOMECH) {
            return "Protomech";
        } else if ((typeId & ETYPE_INFANTRY) == ETYPE_INFANTRY) {
            return "Infantry";
        } else if ((typeId & ETYPE_GUN_EMPLACEMENT) == ETYPE_GUN_EMPLACEMENT) {
            return "Gun Emplacement";
        } else if ((typeId & ETYPE_SUPER_HEAVY_TANK) == ETYPE_SUPER_HEAVY_TANK) {
            return "Superheavy Tank";
        } else if ((typeId & ETYPE_SUPPORT_TANK) == ETYPE_SUPPORT_TANK) {
            return "Support Tank";
        } else if ((typeId & ETYPE_LARGE_SUPPORT_TANK) == ETYPE_LARGE_SUPPORT_TANK) {
            return "Large Support Tank";
        } else if ((typeId & ETYPE_VTOL) == ETYPE_VTOL) {
            return "VTOL";
        } else if ((typeId & ETYPE_SUPPORT_VTOL) == ETYPE_SUPPORT_VTOL) {
            return "Support VTOL";
        } else if ((typeId & ETYPE_TANK) == ETYPE_TANK) {
            return "Tank";
        } else {
            return "Unknown";
        }
    }

    public void damageSystem(int type, int slot, int hits) {
        for (int loc = 0; loc < locations(); loc++) {
            damageSystem(type, slot, loc, hits);
        }
    }

    public void damageSystem(int type, int slot, int loc, int hits) {
        int nhits = 0;
        for (int i = 0; i < getNumberOfCriticals(loc); i++) {
            CriticalSlot cs = getCritical(loc, i);
            // ignore empty & system slots
            if ((cs == null) || (cs.getType() != type)) {
                continue;
            }
            Mounted m = null;
            if (type == CriticalSlot.TYPE_EQUIPMENT) {
                m = getEquipment(slot);
            }
            if (((type == CriticalSlot.TYPE_SYSTEM) && (cs.getIndex() == slot))
                || ((type == CriticalSlot.TYPE_EQUIPMENT)
                    && (m.equals(cs.getMount()) || m.equals(cs.getMount2())))) {
                if (nhits < hits) {
                    cs.setHit(true);
                    cs.setDestroyed(true);
                    nhits++;
                } else {
                    cs.setHit(false);
                    cs.setDestroyed(false);
                    cs.setRepairable(true);
                }
            }
        }
    }

    // Most units cannot eject.
    // ToDo Look up ejection rules for ASF.
    public boolean isEjectionPossible() {
        return false;
    }

    public int getAllowedPhysicalAttacks() {
        if ((null != crew) && crew.getOptions().booleanOption("melee_master")) {
            return 2;
        }
        return 1;
    }

    public int getMaxWeaponRange() {
        // Aeros on the ground map must shoot along their flight path, giving
        // them effectively 0 range
        if (((ETYPE_AERO & getEntityType()) == ETYPE_AERO) && isAirborne() 
                && game.getBoard().onGround()) {
            return 0;
        }
        
        int maxRange = 0;
        if ((ETYPE_MECH == getEntityType())
                || (ETYPE_INFANTRY == getEntityType())
                || (ETYPE_PROTOMECH == getEntityType())) {
            // account for physical attacks.
            maxRange = 1;
        }

        for (Mounted weapon : getWeaponList()) {
            if (!weapon.isReady()) {
                continue;
            }

            WeaponType type = (WeaponType) weapon.getType();
            int range = (game.getOptions().booleanOption(
                    OptionsConstants.AC_TAC_OPS_RANGE) ? type.getExtremeRange()
                    : type.getLongRange());
            if (range > maxRange) {
                maxRange = range;
            }
        }
        return maxRange;
    }

    public int getHeat() {
        return heat;
    }

    public int getTsempHitsThisTurn() {
        return tsempHitsThisTurn;
    }

    public void addTsempHitThisTurn() {
        tsempHitsThisTurn++;
    }

    public int getTsempEffect() {
        return tsempEffect;
    }

    public void setTsempEffect(int tsempEffect) {
        this.tsempEffect = tsempEffect;
    }

    public boolean isFiredTsempThisTurn() {
        return firedTsempThisTurn;
    }

    public void setFiredTsempThisTurn(boolean firedTsempThisTurn) {
        this.firedTsempThisTurn = firedTsempThisTurn;
    }

    public boolean hasFiredTsemp() {
        return hasFiredTsemp;
    }

    public void setHasFiredTsemp(boolean hasFiredTSEMP) {
        hasFiredTsemp = hasFiredTSEMP;
    }

    public boolean hasActivatedRadicalHS() {
        for (Mounted m : getMisc()) {
            if (m.getType().hasFlag(MiscType.F_RADICAL_HEATSINK)
                && m.curMode().equals("On")) {
                return true;
            }
        }
        return false;
    }

    public void deactivateRadicalHS() {
        for (Mounted m : getMisc()) {
            if (m.getType().hasFlag(MiscType.F_RADICAL_HEATSINK)) {
                m.setMode("Off");
                // Can only have one radical heat sink
                break;
            }
        }
    }

    public int getConsecutiveRHSUses() {
        return consecutiveRHSUses;
    }

    public void setConsecutiveRHSUses(int consecutiveRHSUses) {
        this.consecutiveRHSUses = consecutiveRHSUses;
    }

    public boolean hasDamagedRHS() {
        return hasDamagedRHS;
    }

    public void setHasDamagedRHS(boolean hasDamagedRHS) {
        this.hasDamagedRHS = hasDamagedRHS;
    }

    public boolean isUseGeometricBV() {
        return useGeometricBV;
    }

    public void setUseGeometricBV(boolean useGeometricBV) {
        this.useGeometricBV = useGeometricBV;
    }

    public boolean isUseReducedOverheatModifierBV() {
        return useReducedOverheatModifierBV;
    }

    public void setUseReducedOverheatModifierBV(boolean useReducedOverheatModifierBV) {
        this.useReducedOverheatModifierBV = useReducedOverheatModifierBV;
    }

    public void addAttackedByThisTurn(int entityId) {
        attackedByThisTurn.add(entityId);
    }

    public void clearAttackedByThisTurn() {
        attackedByThisTurn.clear();
    }

    public Collection<Integer> getAttackedByThisTurn() {
        return new HashSet<>(attackedByThisTurn);
    }

    public WeaponSortOrder getWeaponSortOrder() {
        if (weaponSortOrder == null) {
            return WeaponSortOrder.DEFAULT;
        }
        return weaponSortOrder;
    }

    public void setWeaponSortOrder(WeaponSortOrder weaponSortOrder) {
        if (weaponSortOrder != this.weaponSortOrder) {
            setWeapOrderChanged(true);
        }
        // If sort mode is custom, and the custom order is null, create it
        // and make the order the same as default (based on eqId)
        if ((weaponSortOrder == WeaponSortOrder.CUSTOM)
            && (customWeapOrder == null)) {
            customWeapOrder = new HashMap<Integer, Integer>();
            for (Mounted weap : weaponList) {
                int eqId = getEquipmentNum(weap);
                customWeapOrder.put(eqId, eqId);
            }
        }
        this.weaponSortOrder = weaponSortOrder;
    }

    public Map<Integer, Integer> getCustomWeaponOrder() {
        return customWeapOrder;
    }

    public void setCustomWeaponOrder(Map<Integer, Integer> customWeapOrder) {
        this.customWeapOrder = customWeapOrder;
    }

    public int getCustomWeaponOrder(Mounted weapon) {
        int eqId = getEquipmentNum(weapon);
        if (customWeapOrder == null) {
            return eqId;
        }
        Integer order = customWeapOrder.get(eqId);
        if (order == null) {
            return -1;
        } else {
            return order;
        }
    }

    public void setCustomWeaponOrder(Mounted weapon, int order) {
        setWeapOrderChanged(true);
        int eqId = getEquipmentNum(weapon);
        if (eqId == -1) {
            return;
        }
        customWeapOrder.put(eqId, order);
    }

    public boolean isWeapOrderChanged() {
        return weapOrderChanged;
    }

    public void setWeapOrderChanged(boolean weapOrderChanged) {
        this.weapOrderChanged = weapOrderChanged;
    }

    public int getMpUsedLastRound() {
        return mpUsedLastRound;
    }

    public void setMpUsedLastRound(int mpUsedLastRound) {
        this.mpUsedLastRound = mpUsedLastRound;
    }
    
    /**
     * Flag that determines if the Entity is a support vehicle.
     * @return
     */
    public boolean isSupportVehicle() {
        return false;
    }

    public int getStructuralTechRating() {
        return structuralTechRating;
    }

    public void setStructuralTechRating(int structuralTechRating) {
        this.structuralTechRating = structuralTechRating;
    }
    
    /**
     * Returns the base engine value for support vehicles, see TM pg 120.  Non
     * support vehicle Entities will return 0.
     * 
     * @return
     */
    public double getBaseEngineValue() {
        return 0;
    }

    /**
     * Returns the base chassis value for support vehicles, see TM pg 120.  Non
     * support vehicle Entities will return 0.
     * 
     * @return
     */
    public double getBaseChassisValue() {
        return 0;
    }

    public int getArmorTechRating() {
        if (armorTechRating == USE_STRUCTURAL_RATING) {
            return structuralTechRating;
        }
        return armorTechRating;
    }

    public void setArmorTechRating(int armorTechRating) {
        this.armorTechRating = armorTechRating;
    }

    public int getEngineTechRating() {
        if (engineTechRating == USE_STRUCTURAL_RATING) {
            return structuralTechRating;
        }
        return engineTechRating;
    }

    public void setEngineTechRating(int engineTechRating) {
        this.engineTechRating = engineTechRating;
    }
}

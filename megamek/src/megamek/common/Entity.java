/*
 * Copyright (c) 2000-2005 - Ben Mazur (bmazur@sev.org).
 * Copyright (c) 2018-2022 - The MegaMek Team. All Rights Reserved.
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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import megamek.MMConstants;
import megamek.client.bot.princess.FireControl;
import megamek.client.ui.Base64Image;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.calculationReport.CalculationReport;
import megamek.client.ui.swing.calculationReport.DummyCalculationReport;
import megamek.codeUtilities.StringUtility;
import megamek.common.MovePath.MoveStepType;
import megamek.common.actions.*;
import megamek.common.annotations.Nullable;
import megamek.common.battlevalue.BVCalculator;
import megamek.common.enums.*;
import megamek.common.equipment.*;
import megamek.common.event.GameEntityChangeEvent;
import megamek.common.force.Force;
import megamek.common.hexarea.HexArea;
import megamek.common.icons.Camouflage;
import megamek.common.jacksonadapters.EntityDeserializer;
import megamek.common.options.*;
import megamek.common.planetaryconditions.Atmosphere;
import megamek.common.planetaryconditions.PlanetaryConditions;
import megamek.common.planetaryconditions.Wind;
import megamek.common.preference.PreferenceManager;
import megamek.common.util.DiscordFormat;
import megamek.common.weapons.*;
import megamek.common.weapons.bayweapons.AR10BayWeapon;
import megamek.common.weapons.bayweapons.BayWeapon;
import megamek.common.weapons.bayweapons.CapitalMissileBayWeapon;
import megamek.common.weapons.bombs.*;
import megamek.common.weapons.capitalweapons.CapitalMissileWeapon;
import megamek.common.weapons.infantry.InfantryWeapon;
import megamek.logging.MMLogger;
import megamek.utilities.xml.MMXMLUtility;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Entity is a master class for basically anything on the board except terrain.
 */
@JsonDeserialize(using = EntityDeserializer.class)
public abstract class Entity extends TurnOrdered implements Transporter, Targetable, RoundUpdated,
        PhaseUpdated, ITechnology, ForceAssignable, CombatRole, Deployable {

    private static final MMLogger logger = MMLogger.create(Entity.class);

    private static final long serialVersionUID = 1430806396279853295L;

    public enum InvalidSourceBuildReason {
        UNIT_OLDER_THAN_EQUIPMENT_INTRO_YEAR,
        NOT_ENOUGH_SLOT_COUNT,
        UNIT_OVERWEIGHT,
        INVALID_OR_OUTDATED_BUILD,
        INCOMPLETE_BUILD,
        INVALID_ENGINE,
        INVALID_CREW,
    }

    public static final int DOES_NOT_TRACK_HEAT = 999;
    public static final int UNLIMITED_JUMP_DOWN = 999;

    /**
     * Entity Type Id Definitions These are used to identify the type of Entity,
     * such as 'Mek or aero.
     */
    public static final long ETYPE_MEK = 1L;
    public static final long ETYPE_BIPED_MEK = 1L << 1;
    public static final long ETYPE_LAND_AIR_MEK = 1L << 2;
    public static final long ETYPE_QUAD_MEK = 1L << 3;
    public static final long ETYPE_ARMLESS_MEK = 1L << 4;

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
    public static final long ETYPE_MEKWARRIOR = 1L << 17;

    public static final long ETYPE_PROTOMEK = 1L << 18;

    public static final long ETYPE_TANK = 1L << 19;

    public static final long ETYPE_GUN_EMPLACEMENT = 1L << 20;

    public static final long ETYPE_SUPER_HEAVY_TANK = 1L << 21;

    public static final long ETYPE_SUPPORT_TANK = 1L << 22;
    public static final long ETYPE_LARGE_SUPPORT_TANK = 1L << 23;

    public static final long ETYPE_VTOL = 1L << 24;
    public static final long ETYPE_SUPPORT_VTOL = 1L << 25;

    public static final long ETYPE_TRIPOD_MEK = 1L << 26;
    public static final long ETYPE_QUADVEE = 1L << 27;

    public static final long ETYPE_AEROSPACEFIGHTER = 1L << 28;

    public static final int BLOOD_STALKER_TARGET_CLEARED = -2;

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

    protected transient Game game;
    protected transient IGame iGame;

    protected int id = Entity.NONE;

    protected Camouflage camouflage = new Camouflage();

    /**
     * ID settable by external sources (such as mm.net)
     */
    protected String externalId = "-1";

    protected double weight;
    protected boolean omni = false;
    protected String chassis;
    protected String model;

    /**
     * The special chassis name for Clan Meks such as Timber Wolf for the Mad Cat.
     * This is appended to the
     * base chassis name to form the full chassis name such as Mad Cat (Timber Wolf)
     * in
     * {@link #getShortNameRaw()} and {@link #getFullChassis()}. This is only saved
     * in mtf files (as of 2024).
     */
    protected String clanChassisName = "";

    /**
     * If this is a unit from an official source, the MUL ID links it to its
     * corresponding
     * entry in the online Master Unit List.
     */
    protected int mulId = -1;

    protected int year = 3071;
    protected int techLevel;
    private CompositeTechLevel compositeTechLevel;

    /**
     * Used by support vehicles to define the structural tech rating
     * (TM pg 117). The values should come from EquipmentType.RATING_A-X.
     */
    protected int structuralTechRating = EquipmentType.RATING_A;

    /**
     * Used by support vehicles to define tech rating of armor. Default value
     * indicates that structural tech rating should be used, as in most cases
     * the armor and structural tech ratings match.
     */
    protected int armorTechRating = USE_STRUCTURAL_RATING;

    /**
     * Used by support vehicles to define tech rating of armor. Default value
     * indicates that structural tech rating should be used, as in most cases
     * the engine and structural tech ratings match.
     */
    protected int engineTechRating = USE_STRUCTURAL_RATING;

    /**
     * Used by omni support vehicles to track the weight of optional fire control
     * systems.
     */
    private double baseChassisFireConWeight = 0.0;

    /**
     * Year to use calculating engine and control system weight and fuel efficiency
     * for primitive
     * support vehicles and aerospace units. This needs to be tracked separately
     * from intro year to
     * account for refits that change the intro year but don't affect the structural
     * components
     */
    private int originalBuildYear = -1;

    private Engine engine;
    protected boolean mixedTech = false;
    protected boolean designValid = true;
    protected boolean useManualBV = false;
    protected int manualBV = -1;

    protected int initialBV = -1;

    /**
     * Protects: displayName, shortName, duplicateMarker.
     */
    private String displayName = null;
    private String shortName = null;
    private int duplicateMarker = 1;

    protected transient Player owner;
    protected int ownerId;
    protected int traitorId = -1;

    protected int targetBay = -1;

    private int startingPos = Board.START_NONE;
    private int startingOffset = 0;
    private int startingWidth = 3;

    public static final int STARTING_ANY_NONE = -1;
    private int startingAnyNWx = STARTING_ANY_NONE;
    private int startingAnyNWy = STARTING_ANY_NONE;
    private int startingAnySEx = STARTING_ANY_NONE;
    private int startingAnySEy = STARTING_ANY_NONE;

    /**
     * The pilot of the entity. Even infantry has a 'pilot'.
     */
    private Crew crew;

    // Crew and passenger numbers
    protected int nCrew;
    protected int nPassenger;
    protected int nMarines;

    private Quirks quirks = new Quirks();
    private final PartialRepairs partReps = new PartialRepairs();

    // Variable for manually shutdown Meks.
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
    protected GamePhase twistedPhase = null;

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
    protected boolean selfDestructedThisTurn = false;

    /**
     * True when the entity has an undestroyed searchlight that is neither a
     * Quirk searchlight nor a mounted (0.5t / 1slot) searchlight.
     */
    protected boolean hasExternalSearchlight = false;
    protected boolean illuminated = false;
    protected boolean searchlightIsActive = false;
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
    protected boolean convertingNow = false;
    private int conversionMode = 0;
    protected EntityMovementMode previousMovementMode;

    protected DisplacementAttackAction displacementAttack = null;

    public int heat = 0;
    public int heatBuildup = 0;
    public int heatFromExternal = 0;
    public int coolFromExternal = 0;
    public int delta_distance = 0;
    public int mpUsed = 0;
    public int underwaterRounds = 0;
    public EntityMovementType moved = EntityMovementType.MOVE_NONE;
    public EntityMovementType movedLastRound = EntityMovementType.MOVE_NONE;
    private boolean movedBackwards = false;

    /**
     * Used to keep track of usage of the power reverse quirk, which allows a
     * combat vehicle to use flank MP in reverse. If power reverse is used and
     * a PSR is required, it adds a +1 modifier to the PSR.
     */
    private boolean isPowerReverse = false;

    private boolean wigeLiftoffHover = false;
    protected int mpUsedLastRound = 0;
    public boolean gotPavementOrRoadBonus = false;
    public int wigeBonus = 0;
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
    public boolean rolledForEngineExplosion = false; // So that we don't roll twice in one round
    public boolean dodging;
    public boolean reckless;
    private boolean evading = false;

    public boolean spotting;
    private boolean clearingMinefield = false;
    protected int killerId = Entity.NONE;
    private int offBoardDistance = 0;
    private OffBoardDirection offBoardDirection = OffBoardDirection.NONE;
    private OffBoardDirection retreatedDirection = OffBoardDirection.NONE;

    protected int[] vectors = { 0, 0, 0, 0, 0, 0 };
    private int recoveryTurn = 0;

    // need to keep a list of areas that this entity has passed through on the
    // current turn
    private Vector<Coords> passedThrough = new Vector<>();

    private List<Integer> passedThroughFacing = new ArrayList<>();

    private List<InvalidSourceBuildReason> invalidSourceBuildReasons = new ArrayList<>();
    /**
     * Stores the player selected hex ground to air targeting.
     * For ground to air, distance to target for the ground unit is determined
     * by the closest hex in the flight path of the airborne unit. It's
     * possible that there are multiple equidistance hexes in the flight path
     * and in some cases, one of those hexes will be better than the other (ie,
     * one could be side arc and one rear). By default, MM picks the first hex,
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
    private final String[] c3iUUIDs = new String[MAX_C3i_NODES];
    private final String[] NC3UUIDs = new String[MAX_C3i_NODES];

    protected int structureType = EquipmentType.T_STRUCTURE_UNKNOWN;
    protected int structureTechLevel = TechConstants.T_TECH_UNKNOWN;
    private transient Boolean cacheHasModularArmor = null;


    protected String source = "";

    /**
     * Keeps track of whether this Entity was hit by a TSEMP this turn.
     */
    private int tsempHitsThisTurn = 0;

    /**
     * Keeps track of the current TSEMP effect on this entity
     */
    private int tsempEffect = MMConstants.TSEMP_EFFECT_NONE;

    /**
     * Keeps track of the current ASEW effect on this entity
     */
    protected int asewAffectedTurns = 0;

    /**
     * Keeps track of whether this Entity fired a TSEMP this turn
     */
    private boolean firedTsempThisTurn = false;

    /**
     * Keeps track of whether this Entity has ever fired a TSEMP. This is used
     * to avoid having to iterate over all weapons looking for TSEMPs to reset
     * at the start of every round.
     */
    private boolean hasFiredTsemp = false;

    /**
     * A list of all mounted equipment. (Weapons, ammo, and misc)
     */
    protected List<Mounted<?>> equipmentList = new ArrayList<>();

    /**
     * A list of all mounted weapons. This only includes regular weapons, not
     * bay mounts or grouped weapon mounts.
     */
    protected List<WeaponMounted> weaponList = new ArrayList<>();

    /**
     * A list of all mounted weapon bays
     */
    protected List<WeaponMounted> weaponBayList = new ArrayList<>();

    /**
     * A list of all mounted weapon groups
     */
    protected List<WeaponMounted> weaponGroupList = new ArrayList<>();

    /**
     * A list of every weapon mount, including bay mounts and weapon group
     * mounts
     */
    protected List<WeaponMounted> totalWeaponList = new ArrayList<>();

    /**
     * A list of all mounted ammo.
     */
    protected List<AmmoMounted> ammoList = new ArrayList<>();

    /**
     * A list of all mounted bombs.
     */
    protected List<BombMounted> bombList = new ArrayList<>();

    /**
     * A list of all remaining equipment.
     */
    protected List<MiscMounted> miscList = new ArrayList<>();

    protected ArrayList<INarcPod> pendingINarcPods = new ArrayList<>();
    protected ArrayList<INarcPod> iNarcPods = new ArrayList<>();
    protected ArrayList<NarcPod> pendingNarcPods = new ArrayList<>();
    protected ArrayList<NarcPod> narcPods = new ArrayList<>();

    protected ArrayList<String> failedEquipmentList = new ArrayList<>();

    // which teams have NARCd us? a long allows for 64 teams.
    protected long m_lNarcedBy = 0;
    protected long m_lPendingNarc = 0;

    /**
     * This matrix stores critical slots in the format [location][slot #]. What
     * locations entities have and how many slots there are in each is
     * determined by the subclasses of Entity such as Mek.
     */
    protected CriticalSlot[][] crits;

    /**
     * Stores the current movement mode.
     */
    protected EntityMovementMode movementMode = EntityMovementMode.NONE;

    /**
     * Flag that determines if this Entity is a hidden unit or not (see TW pg 259).
     */
    protected boolean isHidden = false;

    /**
     * Used to determine if this Entity has made a pointblank shot so far this
     * round.
     */
    protected boolean madePointblankShot = false;

    /**
     * Keeps track of whether this Entity should activate in a particular game
     * phase. Generally this
     * will be Game.Phase.UNKNOWN, indicating the unit isn't activating.
     */
    protected GamePhase hiddenActivationPhase = GamePhase.UNKNOWN;

    protected boolean carcass = false;

    /**
     * The components of this entity that can transport other entities.
     */
    private Vector<Transporter> transports = new Vector<>();

    /**
     * The components of this entity that can transport other entities and occupy
     * pod space of an omni unit.
     */
    private final Vector<Transporter> omniPodTransports = new Vector<>();

    /**
     * The ids of the MekWarriors this entity has picked up
     */
    private final Vector<Integer> pickedUpMekWarriors = new Vector<>();

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
    private Vector<Integer> loadedKeepers = new Vector<>();

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
     * The removal condition is set when the entity is removed from the game.
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
    private Vector<Player> entitySeenBy = new Vector<>();

    /**
     * Check to see what players have detected this entity with sensors, for
     * double blind play.
     */
    private Vector<Player> entityDetectedBy = new Vector<>();

    /**
     * Contains the ids of all entities that have been detected by this entity's
     * sensors.
     * Used for double-blind on space maps - SO p117
     *
     * Entities need only be cleared from this when they move out of range,
     * are destroyed, or move off the board
     */
    public Set<Integer> sensorContacts = new HashSet<>();

    /**
     * Contains the ids of all entities that this entity has established a firing
     * solution on.
     * Used for double-blind on space maps - SO p117
     *
     * Entities need only be cleared from this when they move out of range,
     * are destroyed, or move off the board
     */
    public Set<Integer> firingSolutions = new HashSet<>();

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
     * @see Entity#hitBySwarmsWeapon
     */
    private final Vector<Integer> hitBySwarmsEntity = new Vector<>();

    /**
     * A vector that stores from which launcher we were hit by a swarm weapon this
     * round. This
     * vector holds the weapon IDs.
     *
     * @see Entity#hitBySwarmsEntity
     */
    private final Vector<Integer> hitBySwarmsWeapon = new Vector<>();

    /**
     * True if and only if this is a canon (published) unit. This is established
     * by checking against a text file in the docs folder; "OfficialUnitList.txt".
     * @see MekFileParser
     */
    private boolean canon;

    private int assaultDropInProgress = 0;

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();
    private boolean climbMode = GUIP.getMoveDefaultClimbMode();

    protected int lastTarget = Entity.NONE;
    protected String lastTargetDisplayName = "";

    /**
     * The entity id of our current spot-target
     */
    private int spotTargetId = Entity.NONE;

    private boolean isCommander = false;

    protected boolean isCarefulStanding = false;

    private boolean turnWasInterrupted = false;

    /**
     * A vector of currently active sensors that might be able to check range
     */
    private final Vector<Sensor> sensors = new Vector<>();
    // the currently selected sensor
    private Sensor activeSensor;
    // the sensor chosen for next turn
    private Sensor nextSensor;
    // roll for sensor check
    private int sensorCheck;

    // the roll for ghost targets
    private Roll ghostTargetRoll;
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

    // for how many rounds has blueshield been active?
    private int blueShieldRounds = 0;

    // Entity fluff object for use with MegaMekLab
    protected EntityFluff fluff = new EntityFluff();

    // a settable armor tonnage for use with MML - this is not what
    // is calculated by getArmorTonnage
    protected double armorTonnage;

    protected static int[] MASC_FAILURE = { 3, 5, 7, 11, 13, 13, 13 };
    protected static int[] ALTERNATE_MASC_FAILURE = { 0, 3, 5, 7, 11, 13, 13, 13 };
    protected static int[] ALTERNATE_MASC_FAILURE_ENHANCED = { 0, 3, 3, 5, 7, 11, 13, 13, 13 };

    // MASCLevel is the # of turns MASC has been used previously
    protected int nMASCLevel = 0;
    protected boolean bMASCWentUp = false;
    protected boolean usedMASC = false; // Has masc been used?

    // SuperchargerLevel is the # of turns Supercharger has been used previously
    protected int nSuperchargerLevel = 0;
    protected boolean bSuperchargerWentUp = false;
    protected boolean usedSupercharger = false; // Has Supercharger been used?

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
     * heat sink. Using a flag will prevent having to iterate over all of the
     * Entity's mounted equipment
     */
    protected boolean hasDamagedRHS = false;

    /**
     * Keeps track of the number of consecutive turns a radical heat sink has
     * been used.
     */
    protected int consecutiveRHSUses = 0;

    private final Set<Integer> attackedByThisTurn = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<Integer> groundAttackedByThisTurn = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * Determines the sort order for weapons in the UnitDisplay weapon list.
     */
    private WeaponSortOrder weaponSortOrder;

    /**
     * Maps a weapon id to a user-specified index, used to get a custom ordering
     * for weapons.
     */
    private Map<Integer, Integer> customWeaponOrder = null;

    /**
     * Flag that indicates weapon sort order has changed (included ordering for
     * custom sort order).
     */
    private boolean weapOrderChanged = false;

    /**
     * Set of team IDs that have observed this entity making attacks from off-board
     */
    private final Set<Integer> offBoardShotObservers;

    private String forceString = "";
    private int forceId = Force.NO_FORCE;

    /**
     * The current target of the Blood Stalker SPA.
     */
    private int bloodStalkerTarget = Entity.NONE;

    /**
     * The location the unit is bracing as per TacOps:AR page 82
     */
    private int braceLocation = Entity.LOC_NONE;

    /**
     * The persistent (except for client/server-transmission and saving) battle
     * value calculator for this entity.
     */
    private transient BVCalculator bvCalculator = BVCalculator.getBVCalculator(this);

    private UnitRole role = UnitRole.UNDETERMINED;

    /**
     * Vector storing references to friendly weapon attack actions this entity may
     * need to support;
     * Primarily used by Princess to speed up TAG utility calculations.
     */
    protected ArrayList<WeaponAttackAction> incomingGuidedAttacks;

    /**
     * A list of entity IDs being towed behind this entity, if present. Used to ensure all following
     * trailers are disconnected if the train is broken at this entity.
     */
    private final List<Integer> connectedUnits = new ArrayList<>();

    /**
     * Map containing all the objects this entity is carrying as cargo, indexed by
     * location
     */
    private Map<Integer, ICarryable> carriedObjects = new HashMap<>();

    /**
     * Round-long flag indicating that this entity has picked up an object this
     * round.
     */
    private boolean endOfTurnCargoInteraction;

    /**
     * The icon for this unit; This is empty unless the unit file has an embedded
     * icon.
     */
    protected Base64Image icon = new Base64Image();

    private boolean hasFleeZone = false;
    private HexArea fleeZone = HexArea.EMPTY_AREA;

    /**
     * Generates a new, blank, entity.
     */
    public Entity() {
        crew = new Crew(defaultCrewType());
        nCrew = 0;
        nPassenger = 0;
        nMarines = 0;
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
        secondaryPositions = new HashMap<>();
        impThisTurn = 0;
        impLastTurn = 0;

        weaponSortOrder = GUIP.getDefaultWeaponSortOrder();

        // set a random UUID for external ID, this will help us sort enemy salvage and
        // prisoners in MHQ
        // and should have no effect on MM (but need to make sure it doesn't screw up
        // MekWars)
        externalId = UUID.randomUUID().toString();
        initTechAdvancement();
        offBoardShotObservers = new HashSet<>();
        incomingGuidedAttacks = new ArrayList<>();
        carriedObjects = new HashMap<>();
    }

    /**
     * @see UnitType
     */
    public abstract int getUnitType();

    public CrewType defaultCrewType() {
        return CrewType.SINGLE;
    }

    protected void initMilitary() {
        military = hasViableWeapons();
    }

    protected boolean hasViableWeapons() {
        int totalDmg = Compute.computeTotalDamage(getTotalWeaponList());

        // Find any weapons with range of 6+
        boolean hasRangeSixPlus = false;
        List<WeaponMounted> weaponList = getTotalWeaponList();
        for (WeaponMounted weapon : weaponList) {
            if (weapon.isCrippled()) {
                continue;
            }
            if (weapon.getType().getLongRange() >= 6) {
                hasRangeSixPlus = true;
                break;
            }
        }
        return (totalDmg >= 5) || hasRangeSixPlus;
    }

    /**
     * Restores the entity after serialization
     */
    public void restore() {
        // restore all mounted equipments
        for (Mounted<?> mounted : equipmentList) {
            mounted.restore();
        }

        // in some situations, an entity's facing winds up having an illegal value
        // this will correct it as best as possible
        facing = FireControl.correctFacing(getFacing());

        // set game options, we derive some equipment's modes from this
        setGameOptions();
    }

    @Override
    public int getId() {
        return id;
    }

    /**
     * Sets the ID number of this Entity, which will also set the display name
     * and short name to null.
     *
     * @param id the new ID.
     */
    @Override
    public void setId(int id) {
        this.id = id;
        displayName = null;
        shortName = null;
    }

    /**
     * This returns the external ID.
     * <p>
     * Taharqa: I am changing
     * externalId to a string so I can use UUIDs in MHQ. It should only require
     * a simple parseInt to be added to it to return an integer for other
     * programs (i.e. MekWars).
     *
     * @return the ID settable by external sources (such as mm.net)
     * @throws NumberFormatException if the stored ID is not an integer
     * @see Entity#externalId
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
     * @see Entity#externalId
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
    @Nullable
    public Game getGame() {
        return game;
    }

    public void setIGame(IGame iGame) {
        this.iGame = iGame;
        if (iGame instanceof Game) {
            game = (Game) iGame;
        }
    }

    /**
     * This sets the game the entity belongs to. It also restores the entity and
     * checks that the game is in a consistent state. This function takes care
     * of the units transported by this entity.
     *
     * @param game The current {@link Game}
     */
    @Override
    public void setGame(Game game) {
        this.game = game;
        restore();
        // Make sure the owner is set.
        if (null == owner) {
            if (Entity.NONE == ownerId) {
                throw new IllegalStateException("Entity doesn't know its owner's ID.");
            }
            Player player = game.getPlayer(ownerId);
            if (null == player) {
                logger.debug("Entity can't find player #" + ownerId);
            } else {
                setOwner(player);
            }
        }
        // also set game for our transports
        // they need it to return correct entities, because they store just the
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
     * @return The pure chassis name without any additions, e.g. "Mad Cat"
     */
    public String getChassis() {
        return chassis;
    }

    /**
     * @return The full chassis name plus the additional name, if any, e.g. Mad Cat
     *         (Timber Wolf)
     */
    public String getFullChassis() {
        return chassis + (StringUtility.isNullOrBlank(clanChassisName) ? "" : " (" + clanChassisName + ")");
    }

    /**
     * sets the chassis name for this entity.
     *
     * @param chassis The chassis name.
     */
    public void setChassis(String chassis) {
        this.chassis = chassis;
    }

    /** Sets the {@link #clanChassisName} for this unit, e.g. "Timber Wolf". */
    public void setClanChassisName(String name) {
        clanChassisName = Objects.requireNonNullElse(name, "");
    }

    /**
     * @return The {@link #clanChassisName} for this unit, e.g. "Timber Wolf", or ""
     *         if there is none.
     */
    public String getClanChassisName() {
        return clanChassisName;
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
        recalculateTechAdvancement();
    }

    /**
     * Sets initial TechAdvancement without equipment based on construction options.
     */
    protected void initTechAdvancement() {
        compositeTechLevel = new CompositeTechLevel(this, F_NONE);
        addSystemTechAdvancement(compositeTechLevel);
    }

    public CompositeTechLevel factionTechLevel(int techFaction) {
        if (techFaction == F_NONE) {
            return compositeTechLevel;
        }
        CompositeTechLevel retVal = new CompositeTechLevel(this, techFaction);
        addSystemTechAdvancement(retVal);
        for (Mounted<?> m : getEquipment()) {
            retVal.addComponent(m.getType());
            if (m.isArmored()) {
                retVal.addComponent(TA_ARMORED_COMPONENT);
            }
        }
        return retVal;
    }

    protected void addTechComponent(ITechnology tech) {
        compositeTechLevel.addComponent(tech);
    }

    @Override
    public boolean isIntroLevel() {
        return compositeTechLevel.isIntroLevel();
    }

    @Override
    public boolean isUnofficial() {
        return compositeTechLevel.isUnofficial();
    }

    @Override
    public int getIntroductionDate() {
        return year;
    }

    @Override
    public int getIntroductionDate(boolean clan, int faction) {
        return year;
    }

    /**
     * @return The earliest date this unit could be built, based on the latest intro
     *         date
     *         of the components.
     */
    public int getEarliestTechDate() {
        return compositeTechLevel.getEarliestTechDate();
    }

    @Override
    public int getPrototypeDate() {
        return compositeTechLevel.getPrototypeDate();
    }

    @Override
    public int getPrototypeDate(boolean clan, int faction) {
        return compositeTechLevel.getPrototypeDate(clan, faction);
    }

    @Override
    public int getProductionDate() {
        return compositeTechLevel.getProductionDate();
    }

    @Override
    public int getProductionDate(boolean clan, int faction) {
        return compositeTechLevel.getProductionDate(clan, faction);
    }

    @Override
    public int getCommonDate() {
        return compositeTechLevel.getCommonDate();
    }

    @Override
    public int getExtinctionDate() {
        return compositeTechLevel.getExtinctionDate();
    }

    @Override
    public int getExtinctionDate(boolean clan, int faction) {
        return compositeTechLevel.getExtinctionDate(clan, faction);
    }

    @Override
    public int getReintroductionDate() {
        return compositeTechLevel.getReintroductionDate();
    }

    @Override
    public int getReintroductionDate(boolean clan, int faction) {
        return compositeTechLevel.getReintroductionDate(clan, faction);
    }

    @Override
    public int getTechRating() {
        return compositeTechLevel.getTechRating();
    }

    @Override
    public SimpleTechLevel getStaticTechLevel() {
        return compositeTechLevel.getStaticTechLevel();
    }

    public String getTechBaseDescription() {
        String techBase = "";
        if (isMixedTech()) {
            if (isClan()) {
                techBase += Messages.getString("Entity.MixedClan");
            } else {
                techBase += Messages.getString("Entity.MixedIS");
            }
        } else {
            if (isClan()) {
                techBase += Messages.getString("Entity.Clan");
            } else {
                techBase += Messages.getString("Entity.IS");
            }
        }

        return techBase;
    }

    public static List<String> getTechBaseDescriptions() {
        List<String> result = new ArrayList<>();
        result.add(Messages.getString("Entity.IS"));
        result.add(Messages.getString("Entity.Clan"));
        result.add(Messages.getString("Entity.MixedIS"));
        result.add(Messages.getString("Entity.MixedClan"));

        return result;
    }

    @Override
    public int getBaseAvailability(int era) {
        return compositeTechLevel.getBaseAvailability(era);
    }

    @Override
    public String getExtinctionRange() {
        return compositeTechLevel.getExtinctionRange();
    }

    /**
     * return - the base construction option tech advancement
     */
    public abstract TechAdvancement getConstructionTechAdvancement();

    /**
     * Resets techAdvancement to initial value and adjusts for all installed
     * equipment.
     */
    public void recalculateTechAdvancement() {
        initTechAdvancement();
        for (Mounted<?> m : getEquipment()) {
            compositeTechLevel.addComponent(m.getType());
            if (m.isArmored()) {
                compositeTechLevel.addComponent(TA_ARMORED_COMPONENT);
            }
        }
    }

    protected static final TechAdvancement TA_OMNI = new TechAdvancement(TECH_BASE_ALL)
            .setISAdvancement(DATE_NONE, DATE_NONE, 3052)
            .setClanAdvancement(2854, 2856, 2864).setClanApproximate(true)
            .setPrototypeFactions(F_CCY, F_CSF).setProductionFactions(F_CCY, F_DC)
            .setTechRating(RATING_E).setAvailability(RATING_X, RATING_E, RATING_E, RATING_D)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
    // This is not in the rules anywhere, but is implied by the existence of the
    // Badger and Bandit
    // tanks used by Wolf's Dragoons and sold to the merc market as early as 3008.
    private static final TechAdvancement TA_OMNIVEHICLE = new TechAdvancement(TECH_BASE_ALL)
            .setISAdvancement(3008, DATE_NONE, 3052).setISApproximate(true)
            .setClanAdvancement(2854, 2856, 2864).setClanApproximate(true)
            .setPrototypeFactions(F_CCY, F_CSF, F_MERC).setProductionFactions(F_CCY, F_DC)
            .setTechRating(RATING_E).setAvailability(RATING_X, RATING_E, RATING_E, RATING_D)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
    // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
    protected static final TechAdvancement TA_PATCHWORK_ARMOR = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_PS, 3080, DATE_NONE).setApproximate(false, true, false)
            .setTechRating(RATING_A)
            .setAvailability(RATING_E, RATING_D, RATING_E, RATING_E)
            .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
    protected static final TechAdvancement TA_MIXED_TECH = new TechAdvancement(TECH_BASE_ALL)
            .setISAdvancement(DATE_NONE, 3050, 3082, DATE_NONE, DATE_NONE)
            .setClanAdvancement(DATE_NONE, 2820, 3082, DATE_NONE, DATE_NONE)
            .setApproximate(false, true, true, false, false)
            .setPrototypeFactions(F_CLAN, F_DC, F_FS, F_LC)
            .setTechRating(RATING_A).setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
    // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
    protected static final TechAdvancement TA_ARMORED_COMPONENT = new TechAdvancement(TECH_BASE_ALL)
            .setISAdvancement(3061, 3082, DATE_NONE, DATE_NONE, DATE_NONE)
            .setISApproximate(false, true, false, false, false)
            .setClanAdvancement(3061, 3082, DATE_NONE, DATE_NONE, DATE_NONE)
            .setClanApproximate(false, true, false, false, false)
            .setPrototypeFactions(F_CSF, F_FW).setProductionFactions(F_CJF, F_FW)
            .setTechRating(RATING_E).setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
            .setStaticTechLevel(SimpleTechLevel.ADVANCED);

    public static TechAdvancement getOmniAdvancement() {
        return getOmniAdvancement(null);
    }

    public static TechAdvancement getOmniAdvancement(Entity en) {
        if (en instanceof Tank) {
            return new TechAdvancement(TA_OMNIVEHICLE);
        } else {
            return new TechAdvancement(TA_OMNI);
        }
    }

    public static TechAdvancement getPatchworkArmorAdvancement() {
        return new TechAdvancement(TA_PATCHWORK_ARMOR);
    }

    public static TechAdvancement getMixedTechAdvancement() {
        return new TechAdvancement(TA_MIXED_TECH);
    }

    public static TechAdvancement getArmoredComponentTechAdvancement() {
        return new TechAdvancement(TA_ARMORED_COMPONENT);
    }

    /**
     * Incorporate dates for components that are not in the equipment list, such as
     * engines and structure.
     */
    protected void addSystemTechAdvancement(CompositeTechLevel ctl) {
        if (hasEngine()) {
            ctl.addComponent(getEngine());
        }
        if (isOmni()) {
            ctl.addComponent(TA_OMNI);
        }
        if (hasPatchworkArmor()) {
            ctl.addComponent(TA_PATCHWORK_ARMOR);
            for (int loc = 0; loc < locations(); loc++) {
                ctl.addComponent(ArmorType.forEntity(this, loc).getTechAdvancement());
            }
        } else {
            ArmorType armor = ArmorType.forEntity(this);
            if (armor != null) {
                ctl.addComponent(armor.getTechAdvancement());
            }
        }
        if (isMixedTech()) {
            ctl.addComponent(TA_MIXED_TECH);
        }
        ctl.addComponent(EquipmentType.getStructureTechAdvancement(structureType,
                TechConstants.isClan(structureTechLevel)));
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
                && (getTsempEffect() != MMConstants.TSEMP_EFFECT_SHUTDOWN)) {
            setShutDown(false);
            setStartupThisPhase(true);
        }
    }

    /**
     * Checks if this is a clan unit. This is determined by tech level.
     *
     * @return true if this unit is a clan unit.
     * @see Entity#setTechLevel(int)
     */
    @Override
    public boolean isClan() {
        return (IntStream.of(TechConstants.T_CLAN_TW, TechConstants.T_CLAN_ADVANCED,
                TechConstants.T_CLAN_EXPERIMENTAL, TechConstants.T_CLAN_UNOFFICIAL)
                .anyMatch(i -> (techLevel == i)));
    }

    public boolean isClanArmor(int loc) {
        // if the location does not exist, it does not have clan armor
        if (loc >= locations()) {
            return false;
        }

        if (getArmorTechLevel(loc) == TechConstants.T_TECH_UNKNOWN) {
            return isClan();
        }
        return IntStream.of(TechConstants.T_CLAN_TW, TechConstants.T_CLAN_ADVANCED,
                TechConstants.T_CLAN_EXPERIMENTAL, TechConstants.T_CLAN_UNOFFICIAL)
                .anyMatch(i -> (getArmorTechLevel(loc) == i));
    }

    @Override
    public int getTechBase() {
        return isClan() ? TECH_BASE_CLAN : TECH_BASE_IS;
    }

    @Override
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

    /** @return the tonnage of the Entity, not its weight */
    public double getWeight() {
        return weight;
    }

    // TODO: WeightClass is no longer correct. See the Tech Manual
    public int getWeightClass() {
        return EntityWeightClass.getWeightClass(getWeight(), this);
    }

    public String getWeightClassName() {
        return EntityWeightClass.getClassName(getWeightClass(), this);
    }

    // Since this varies by unit type, it will be defined as overrides in each
    // relevant class
    public boolean isSuperHeavy() {
        return false;
    }

    public void setWeight(double weight) {
        this.weight = weight;
        // Any time the weight is reset we need to reset the crew size
        crew.setSize(Compute.getFullCrewSize(this));
        crew.setCurrentSize(crew.getSize());
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
     * Determines where to place equipment that does not require a specific
     * location. What
     * this means varies by {@link Entity} type.
     *
     * @return The location to place equipment that is not required to be assigned a
     *         location,
     *         defaulting to Entity.LOC_NONE for unit types that do not have such a
     *         location.
     */
    public int getBodyLocation() {
        return LOC_NONE;
    }

    /**
     * Returns the player that "owns" this entity.
     */
    public Player getOwner() {
        // Replaced 24 NOV 2020
        // Server and other central classes already used
        // game.getplayer(entity.getownerID())
        // instead of entity.getowner() and it is noted that getOwner is not reliable.
        // The entity owner object would have to be replaced whenever a player is
        // updated
        // which does not happen. The player ID on the other hand stays the same and the
        // game
        // object is not usually replaced. I expect entity.game to be up to date much
        // more than owner.
        // Unfortunately, entities freshly created may not have the game set. Therefore,
        // fall
        // back to the old version when game == null or the player is no longer in the
        // game
        if ((game != null) && (game.getPlayer(ownerId) != null)) {
            return game.getPlayer(ownerId);
        } else {
            return owner;
        }
    }

    public void setOwner(Player player) {
        owner = player;
        ownerId = player.getId();

        generateDisplayName();
    }

    @Override
    public void setOwnerId(int ownerId) {
        setOwner(game.getPlayer(ownerId));
    }

    @Override
    public int getOwnerId() {
        return ownerId;
    }

    /**
     * Returns true if the other entity is an enemy of this entity. This is more
     * reliable than Player.isEnemyOf since it knows that an entity will never
     * be an enemy of itself.
     */
    @Override
    public boolean isEnemyOf(Entity other) {
        if (null == other) {
            return false;
        }
        if (null == getOwner()) {
            return ((id != other.getId()) && (ownerId != other.ownerId));
        }
        return (id != other.getId())
                && ((null == other.getOwner()) || getOwner().isEnemyOf(other.getOwner()));
    }

    public Crew getCrew() {
        return crew;
    }

    public void setCrew(Crew crew) {
        this.crew = crew;
    }

    /**
     * @return The total number of crew available to supplement marines on boarding
     *         actions.
     *         Includes officers, enlisted, and bay personnel, but not marines/ba or
     *         passengers.
     */
    public int getNCrew() {
        return nCrew;
    }

    public void setNCrew(int crew) {
    }

    /**
     * @return The number of passengers on this unit. Intended for spacecraft, where we want to get the
     * crews of transported units plus actual passengers assigned to quarters
     */
    public int getNPassenger() {
        return nPassenger;
    }

    public void setNPassenger(int pass) {
    }

    /**
     * @return The number conventional marines available to vessels for boarding
     *         actions.
     */
    public int getNMarines() {
        return nMarines;
    }

    /**
     * Updates the number of marines aboard
     *
     * @param marines The number of marines to add/subtract
     */
    public void setNMarines(int marines) {

    }

    /**
     * Units with a cockpit command console provide an initiative bonus to their
     * side, provided
     * that the commander is not currently functioning as pilot, the unit has
     * advanced fire control,
     * and the unit is heavy or assault weight class.
     *
     * @return Whether the Entity qualifies for initiative bonus from cockpit
     *         command console.
     */
    public boolean hasCommandConsoleBonus() {
        return false;
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
    @Override
    public int getTargetType() {
        return Targetable.TYPE_ENTITY;
    }

    @Override
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
     * This method should <strong>only</strong> be called when needed to remove a
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
     *         destroyed, has an active crew, and was not unloaded from a
     *         transport this turn. <code>false</code> otherwise.
     */
    public boolean isActive() {
        return isActive(-1);
    }

    public boolean isActive(int turn) {
        boolean isActive = !shutDown && !isManualShutdown() && !destroyed && getCrew().isActive()
                && !unloadedThisTurn;

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
                && !unloadedThisTurn && !isClearingMinefield() && (getTractor() == Entity.NONE);
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

    public boolean wasLoadedThisTurn() {
        return loadedThisTurn;
    }

    /**
     * Returns true if this entity is targetable for attacks. A unit is
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
    @Override
    public boolean isImmobile() {
        return isImmobile(true);
    }

    public boolean isImmobileForJump() {
        return isImmobile();
    }

    /**
     * Is this entity shut down, or if applicable is the crew unconscious?
     *
     * @param checkCrew If true, consider the fitness of the crew when determining
     *                  if the entity is immobile.
     */
    public boolean isImmobile(boolean checkCrew) {
        return isShutDown() || (checkCrew && (crew != null) && crew.isUnconscious());
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
                // Need to make sure here that we're ignoring heat because
                // that's not actually "permanent":
                && ((getWalkMP(MPCalculationSetting.PERM_IMMOBILIZED) == 0)
                        && (getRunMP(MPCalculationSetting.PERM_IMMOBILIZED) == 0)
                        && (getJumpMP(MPCalculationSetting.PERM_IMMOBILIZED) == 0))) {
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
    public @Nullable DisplacementAttackAction findTargetedDisplacement() {
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
     * Set whether or not the Mek's arms are flipped to the rear
     */
    public void setArmsFlipped(boolean armsFlipped) {
        GamePhase phase = getGame().getPhase();
        if (phase != null) {
            // If we already twisted/flipped in an earlier phase, return out
            if (getAlreadyTwisted()) {
                return;
            }
            if (phase.isTargeting() || phase.isOffboard() || phase.isFiring()) {
                // Only Off-board and Firing phases could conceivably have later phases with
                // twisting
                setAlreadyTwisted(true);
            }
        }
        setArmsFlipped(armsFlipped, true);
    }

    /**
     * Set whether or not the Mek's arms are flipped to the rear.
     * Does not fire the game event, useful for when it's called repeatedly
     * such as during bot turn calculations
     */
    public void setArmsFlipped(boolean armsFlipped, boolean fireEvent) {
        this.armsFlipped = armsFlipped;

        if (fireEvent) {
            game.processGameEvent(new GameEntityChangeEvent(this, this));
        }
    }

    /**
     * Returns true if the Mek's arms are flipped to the rear
     */
    public boolean getArmsFlipped() {
        return armsFlipped;
    }

    /**
     * @return true if the VTOL or LAM is making a VTOL strafe or VTOL/AirMek bomb
     *         attack
     */
    public boolean isMakingVTOLGroundAttack() {
        return false;
    }

    /**
     * Returns the current position of this entity on the board. This is not
     * named getLocation(), since I want the word location to refer to hit
     * locations on a Mek or vehicle.
     */
    @Override
    public Coords getPosition() {
        return position;
    }

    /**
     * Returns a set of the coords this Entity occupies
     *
     * @return
     */
    public HashSet<Coords> getOccupiedCoords() {
        HashSet<Coords> positions = new HashSet<>();
        if ((getSecondaryPositions() != null) && !getSecondaryPositions().isEmpty()) {
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
     *         vector or the current position if too small
     */

    public Coords getPriorPosition() {
        if (passedThrough.size() < 2) {
            return getPosition();
        }
        return passedThrough.elementAt(passedThrough.size() - 2);
    }

    /**
     * Sets the current elevation of this entity above the ground. This is the
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
     * to be at w/r/t its new hex.
     */
    public int calcElevation(Hex current, Hex next, int assumedElevation,
            boolean climb, boolean wigeEndClimbPrevious) {
        int retVal = assumedElevation;
        if (next == null) {
            return retVal;
        }
        if (isAero()) {
            return retVal;
        }
        if (getMovementMode() == EntityMovementMode.WIGE) {
            // Airborne WiGEs remain 1 elevation above underlying terrain, unless climb mode
            // is
            // on, then they maintain current absolute elevation as long as it is at least
            // one level above the ground.
            // WiGEs treat the tops of buildings as the underlying terrain, but must pay an
            // additional
            // 2 MP to climb.
            // See
            // http://bg.battletech.com/forums/index.php?topic=51081.msg1297747#msg1297747

            // Find level equivalent of current elevation
            int level = current.getLevel() + assumedElevation;
            // For WiGE purposes, the surface of a hex with a building is the roof;
            // otherwise it's the surface of the hex.
            int curSurface = current.getLevel();
            if (current.containsTerrain(Terrains.BLDG_ELEV)) {
                curSurface += current.terrainLevel(Terrains.BLDG_ELEV);
            }
            int nextSurface = next.getLevel();
            if (next.containsTerrain(Terrains.BLDG_ELEV)) {
                nextSurface += next.terrainLevel(Terrains.BLDG_ELEV);
            }

            int nextLevel;
            if (level - curSurface <= 0) {
                // If we are not above the effective surface, we are not airborne and the next
                // level
                // is the effective surface of the next hex.
                nextLevel = nextSurface;
            } else if (climb) {
                // If climb mode is on, we maintain the same level unless the next surface
                // requires climbing.
                // is the effective surface of the next hex.
                nextLevel = Math.max(level, nextSurface + 1);
            } else {
                // Otherwise we move to one elevation level above the effective surface.
                nextLevel = nextSurface + 1;
            }
            // Elevation is this height of the level above the actual surface elevation of
            // the hex.
            retVal = nextLevel - next.getLevel();
        } else if (((getMovementMode().isSubmarine() || getMovementMode().isUMUInfantry())
                && next.containsTerrain(Terrains.WATER) && current.containsTerrain(Terrains.WATER))
                || getMovementMode().isVTOL()
                || (getMovementMode().isQuadSwim() && hasUMU())
                || (getMovementMode().isBipedSwim() && hasUMU())) {
            retVal += current.getLevel();
            retVal -= next.getLevel();
        } else {
            // if we're a hovercraft, surface ship, WIGE or a "fully amphibious" vehicle, we
            // go on the water surface
            // without adjusting elevation
            if ((getMovementMode() != EntityMovementMode.HOVER)
                    && (getMovementMode() != EntityMovementMode.NAVAL)
                    && (getMovementMode() != EntityMovementMode.HYDROFOIL)
                    && (getMovementMode() != EntityMovementMode.WIGE)
                    && !hasWorkingMisc(MiscType.F_FULLY_AMPHIBIOUS)) {
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
                        // a Mek can only climb out onto ice in depth 2 or
                        // shallower water
                        // Mek on the surface will stay on the surface

                        if (((waterLevel == 1) && (prevWaterLevel == 1))
                                || ((prevWaterLevel <= 2) && climb)
                                || (assumedElevation >= 0)) {
                            retVal += waterLevel;
                        }
                    }
                    retVal -= waterLevel;
                }
            }

            if (next.containsTerrain(Terrains.BUILDING) || current.containsTerrain(Terrains.BUILDING)) {
                int bldcur = Math.max(-current.depth(true), current.terrainLevel(Terrains.BLDG_ELEV));
                int bldnex = Math.max(-next.depth(true), next.terrainLevel(Terrains.BLDG_ELEV));
                if (((assumedElevation == bldcur)
                        && (climb || isJumpingNow) && (this instanceof Mek))
                        || (retVal > bldnex)) {
                    retVal = bldnex;
                } else if ((bldnex + next.getLevel()) > (bldcur + current.getLevel())) {
                    BasementType nextBasement = BasementType.getType(next.terrainLevel(Terrains.BLDG_BASEMENT_TYPE));
                    int collapsedBasement = next.terrainLevel(Terrains.BLDG_BASE_COLLAPSED);
                    if (climb || isJumpingNow) {
                        retVal = bldnex + next.getLevel();
                        // If the basement is collapsed, there is no level 0
                    } else if ((assumedElevation == 0)
                            && !nextBasement.isUnknownOrNone()
                            && (collapsedBasement > 0)) {
                        retVal -= nextBasement.getDepth();
                    } else {
                        retVal += current.getLevel();
                        retVal -= next.getLevel();
                    }
                } else if (elevation == -(current.depth(true))) {
                    BasementType currentBasement = BasementType
                            .getType(current.terrainLevel(Terrains.BLDG_BASEMENT_TYPE));
                    if (climb || isJumpingNow) {
                        retVal = bldnex + next.getLevel();
                    } else if (!currentBasement.isUnknownOrNone()
                            && (assumedElevation == -currentBasement.getDepth())) {
                        retVal = -BasementType.getType(next.terrainLevel(Terrains.BLDG_BASEMENT_TYPE)).getDepth();
                    } else {
                        retVal += current.getLevel();
                        retVal -= next.getLevel();
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
                int elevDiff = Math.abs((next.getLevel() + bridgeElev)
                        - (current.getLevel() + assumedElevation));
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

    public int calcElevation(Hex current, Hex next) {
        return calcElevation(current, next, elevation, false, false);
    }

    /**
     * @return The elevation of this Entity, relative to the current Hex's surface.
     *         When the unit is
     *         transported, returns the elevation of the carrier.
     */
    @Override
    public int getElevation() {
        if (game != null) {
            Entity carrier = game.getEntity(getTransportId());
            if (carrier != null) {
                return carrier.getElevation();
            }
        }

        return isOffBoard() ? 0 : elevation;
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
        Hex hex = getGame().getBoard().getHex(assumedPos);
        int assumedAlt = assumedElevation + hex.getLevel();
        int minAlt = hex.getLevel();
        switch (getMovementMode()) {
            case INF_JUMP:
            case INF_LEG:
            case INF_MOTORIZED:
                minAlt -= Math.max(0, BasementType.getType(hex.terrainLevel(Terrains.BLDG_BASEMENT_TYPE)).getDepth());
                break;
            case WIGE:
                // Per errata, WiGEs have flotation hull, which makes no sense unless it changes
                // the rule
                // in TW that they cannot land on water.
                // See
                if (isAirborne()) {
                    return false;
                }
                if (hex.containsTerrain(Terrains.WATER)) {
                    minAlt = hex.getLevel();
                    break;
                }
                // else fall through
            case VTOL:
                int minElev = 0;
                // When over a bridge, limit downward movement. Can land on a bridge.
                if (hex.containsTerrain(Terrains.BRIDGE_ELEV)
                        && (assumedElevation >= hex.terrainLevel(Terrains.BRIDGE_ELEV))) {
                    minElev = hex.terrainLevel(Terrains.BRIDGE_ELEV);
                }
                // Cannot land on woods or water
                if (hex.containsTerrain(Terrains.WOODS) || hex.containsTerrain(Terrains.JUNGLE)) {
                    minElev = Math.max(minElev, hex.terrainLevel(Terrains.FOLIAGE_ELEV) - hex.depth() + 1);
                }
                if (hex.depth() > 0) {
                    minElev = Math.max(minElev, 1);
                }
                // Can land on buildings
                if (hex.containsTerrain(Terrains.BLDG_ELEV)) {
                    minElev = Math.max(minElev, hex.terrainLevel(Terrains.BLDG_ELEV) - hex.depth());
                }
                minAlt = minElev + hex.getLevel();
                break;
            case AERODYNE:
            case SPHEROID:
                assumedAlt = assumedElevation;
                if (game.getBoard().inAtmosphere()) {
                    minAlt = Math.max(0, hex.ceiling(true)) + 1;
                } else if (game.getBoard().onGround() && isAirborne()) {
                    minAlt = 1;
                }
                // if sensors are damaged then, one higher
                if (isAero() && (((IAero) this).getSensorHits() > 0)) {
                    minAlt++;
                }
                break;
            case INF_UMU:
                /* non-mechanized SCUBA infantry have a maximum depth of 2 */
                if (this instanceof Infantry && ((Infantry) this).hasSpecialization(Infantry.SCUBA)
                        && hex.containsTerrain(Terrains.WATER)) {
                    minAlt = Math.max(hex.floor(), -2);
                } else {
                    minAlt = hex.floor();
                }
                break;
            case SUBMARINE:
            case BIPED_SWIM:
            case QUAD_SWIM:
                minAlt = hex.floor();
                break;
            case BIPED:
            case QUAD:
                if (this instanceof ProtoMek) {
                    minAlt -= Math.max(0,
                            BasementType.getType(hex.terrainLevel(Terrains.BLDG_BASEMENT_TYPE)).getDepth());
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
        Hex hex = getGame().getBoard().getHex(assumedPos);
        int assumedAlt = assumedElevation + hex.getLevel();
        int maxAlt = hex.getLevel();
        switch (getMovementMode()) {
            case INF_JUMP:
            case INF_LEG:
            case INF_MOTORIZED:
                maxAlt += Math.max(0, hex.terrainLevel(Terrains.BLDG_ELEV));
                break;
            case VTOL:
                maxAlt = hex.getLevel() + 50;
                // When under a bridge, restrict upward movement
                // "- 1" to correct that height() reports one less than the rules (TW p.99) say
                if (hex.containsTerrain(Terrains.BRIDGE_ELEV)
                        && assumedElevation < hex.terrainLevel(Terrains.BRIDGE_ELEV)) {
                    maxAlt = hex.terrainLevel(Terrains.BRIDGE_ELEV) - height() - 1;
                }
                break;
            case AERODYNE:
            case SPHEROID:
                if (!game.getBoard().inSpace()) {
                    assumedAlt = assumedElevation;
                    maxAlt = 10;
                }
                break;
            case SUBMARINE:
                maxAlt = hex.getLevel() - getHeight();
                break;
            case INF_UMU:
            case BIPED_SWIM:
            case QUAD_SWIM:
                // UMU's won't allow the entity to break the surface of the
                // water
                maxAlt = hex.getLevel() - (getHeight() + 1);
                break;
            case WIGE:
                if (this instanceof LandAirMek) {
                    if (isAirborne()) {
                        return false;
                    }
                    maxAlt = hex.getLevel() + 25;
                } else if (this instanceof ProtoMek) {
                    maxAlt = hex.getLevel() + 12;
                } else if (hex.containsTerrain(Terrains.BLDG_ELEV)) {
                    maxAlt = Math.max(hex.getLevel(), hex.terrainLevel(Terrains.BLDG_ELEV)) + 1;
                } else {
                    maxAlt = hex.getLevel() + 1;
                }
                break;
            case BIPED:
            case QUAD:
                if (this instanceof ProtoMek) {
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
    public boolean isElevationValid(int assumedElevation, Hex hex) {
        int assumedAlt = assumedElevation + hex.getLevel();
        if (getMovementMode() == EntityMovementMode.VTOL) {
            if ((this instanceof Infantry)
                    && (hex.containsTerrain(Terrains.BUILDING)
                            || hex.containsTerrain(Terrains.WOODS)
                            || hex.containsTerrain(Terrains.JUNGLE))) {
                // VTOL BA (sylph) can move as ground unit as well
                return ((assumedElevation <= 50) && (assumedAlt >= hex.floor()));
            } else {
                // VTOLs can be anywhere on or above ground and fly beneath bridges,
                // land on buildings and ignore planted fields and industrial zone
                // but cannot land on water or trees
                // As always, height() reports one less than the rules (TW p.99) say
                // Units may move under a bridge if their top is
                // lower than or equal to the bridge height (TW p.62)
                boolean allowed = (assumedElevation <= 50) && (assumedElevation >= 0);
                if (hex.containsTerrain(Terrains.BRIDGE_ELEV)) {
                    allowed &= (assumedElevation >= hex.terrainLevel(Terrains.BRIDGE_ELEV))
                            || (assumedElevation + height() + 1 <= hex.terrainLevel(Terrains.BRIDGE_ELEV));
                }
                if (hex.containsTerrain(Terrains.FOLIAGE_ELEV)) {
                    allowed &= (assumedElevation > hex.terrainLevel(Terrains.FOLIAGE_ELEV) - hex.depth());
                }
                if (hex.depth() > 0) {
                    allowed &= (assumedElevation > 0);
                }
                if (hex.containsTerrain(Terrains.BLDG_ELEV)) {
                    allowed &= (assumedElevation >= hex.terrainLevel(Terrains.BLDG_ELEV) - hex.depth());
                }
                return allowed;
            }
        } else if ((getMovementMode() == EntityMovementMode.SUBMARINE)
                || ((getMovementMode() == EntityMovementMode.INF_UMU) && hex
                        .containsTerrain(Terrains.WATER))
                || ((getMovementMode() == EntityMovementMode.QUAD_SWIM) && hasUMU())
                || ((getMovementMode() == EntityMovementMode.BIPED_SWIM) && hasUMU())) {
            if (this instanceof Infantry && ((Infantry) this).hasSpecialization(Infantry.SCUBA)
                    && getMovementMode() == EntityMovementMode.INF_UMU) {
                return assumedAlt >= Math.max(hex.floor(), -2)
                        && (assumedAlt <= hex.getLevel());
            }
            return ((assumedAlt >= hex.floor()) && (assumedAlt <= hex.getLevel()));
        } else if ((getMovementMode() == EntityMovementMode.HYDROFOIL)
                || (getMovementMode() == EntityMovementMode.NAVAL)) {
            return assumedAlt == hex.getLevel();
        } else if (getMovementMode() == EntityMovementMode.WIGE) {
            // WiGEs can possibly be at any location above or on the surface
            return (assumedAlt >= hex.floor());
        } else {
            // regular ground units
            if (hex.containsTerrain(Terrains.ICE)
                    || (((getMovementMode() == EntityMovementMode.HOVER) || hasWorkingMisc(MiscType.F_FULLY_AMPHIBIOUS))
                            &&
                            hex.containsTerrain(Terrains.WATER))) {
                // surface of ice is OK, surface of water is OK for hovers and "fully
                // amphibious" units
                if (assumedAlt == hex.getLevel()) {
                    return true;
                }
            }
            // only Meks can move underwater
            if (hex.containsTerrain(Terrains.WATER)
                    && (assumedAlt < hex.getLevel()) && !(this instanceof Mek)
                    && !(this instanceof ProtoMek)) {
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
                // Meks, protos and infantry can occupy any floor in the
                // building
                if ((this instanceof Mek) || (this instanceof ProtoMek)
                        || (this instanceof Infantry)) {
                    return (assumedAlt >= (hex.getLevel() - hex.depth(true))) && (assumedAlt <= hex.ceiling());
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
     * the surface of the hex the entity is in, i.e.
     * relHeight() == getElevation() + getHeight()
     */
    @Override
    public int relHeight() {
        return getElevation() + height();
    }

    /**
     * Convenience method to determine whether this entity is on a ground map with
     * an atmosphere
     */
    public boolean isOnAtmosphericGroundMap() {
        boolean onGroundOrinAtmosphere = getGame().getBoard().onGround() ||
        // doesn't make sense in english, but "atmospheric" map actually
        // covers maps that are within a planet's gravity well
                getGame().getBoard().inAtmosphere();
        PlanetaryConditions conditions = getGame().getPlanetaryConditions();
        return conditions.getAtmosphere().isDenserThan(Atmosphere.TRACE)
                && onGroundOrinAtmosphere;
    }

    /**
     * Convenience method to determine whether this entity should be treated as an
     * airborne aero on a ground map.
     *
     * @return True if this is an airborne aircraft on a ground map.
     */
    public boolean isAirborneAeroOnGroundMap() {
        return isAero() && isAirborne() && getGame() != null && getGame().getBoard().onGround();
    }

    /**
     * Gets the marker used to disambiguate this entity
     * from others with the same name. These are monotonically
     * increasing values, starting from one.
     */
    public synchronized int getDuplicateMarker() {
        return duplicateMarker;
    }

    /**
     * Sets the marker used to disambiguate this entity
     * from others with the same name. These are monotonically
     * increasing values, starting from one.
     *
     * @param duplicateMarker A marker to disambiguate this entity
     *                        from others with the same name.
     */
    public synchronized void setDuplicateMarker(int duplicateMarker) {
        this.duplicateMarker = duplicateMarker;
        if (duplicateMarker > 1) {
            shortName = createShortName(duplicateMarker);
            displayName = createDisplayName(duplicateMarker);
        }
    }

    /**
     * Updates the marker used to disambiguate this entity
     * from others with the same name after one of them has
     * been removed from the game.
     *
     * @param removedMarker The marker of the removed entity.
     * @return A value indicating whether or not this entity
     *         updated its duplicate marker.
     */
    public synchronized boolean updateDuplicateMarkerAfterDelete(int removedMarker) {
        if (duplicateMarker > removedMarker) {
            duplicateMarker--;
            shortName = createShortName(duplicateMarker);
            displayName = createDisplayName(duplicateMarker);
            return true;
        }

        return false;
    }

    /**
     * Returns the display name for this entity.
     */
    @Override
    public String getDisplayName() {
        if (displayName == null) {
            generateDisplayName();
        }
        return displayName;
    }

    /**
     * Sets the display name for this entity.
     *
     * @param displayName The new display name.
     */
    protected void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Generates the display name for this entity.
     * <p>
     * Sub-classes are allowed to override this method. The display name is in
     * the format [Chassis] [Model] ([Player Name]).
     */
    public synchronized void generateDisplayName() {
        displayName = createDisplayName(duplicateMarker);
    }

    /**
     * Creates a display name for the entity.
     *
     * @param duplicateMarker A number used to disambiguate two entities with
     *                        the same name.
     * @return A display name for the entity.
     */
    private String createDisplayName(int duplicateMarker) {
        StringBuilder builder = new StringBuilder();
        builder.append(createShortName(duplicateMarker));

        if (getOwner() != null && getOwner().getName() != null && !getOwner().getName().isBlank()) {
            builder.append(" (").append(getOwner().getName()).append(")");
        }

        return builder.toString();
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
     * <p>
     * Sub-classes are allowed to override this method. The display name is in
     * the format [Chassis] [Model].
     */
    public synchronized void generateShortName() {
        shortName = createShortName(duplicateMarker);
    }

    /**
     * Creates a short name for the entity.
     *
     * @param duplicateMarker A number used to disambiguate two entities with
     *                        the same name.
     * @return A short name for the entity.
     */
    private String createShortName(int duplicateMarker) {
        StringBuilder builder = new StringBuilder();
        builder.append(getShortNameRaw());
        // if show unit id is on, append the id
        if (PreferenceManager.getClientPreferences().getShowUnitId()) {
            builder.append(" ID:").append(getId());
        } else if (duplicateMarker > 1) {
            // if not, and a player has more than one unit with the same name,
            // append "#N" after the model to differentiate.
            builder.append(" #").append(duplicateMarker);
        }

        return builder.toString();
    }

    public String getShortNameRaw() {
        String unitName = chassis;
        unitName += StringUtility.isNullOrBlank(clanChassisName) ? "" : " (" + clanChassisName + ")";
        unitName += StringUtility.isNullOrBlank(model) ? "" : " " + model;
        return unitName;
    }

    /**
     * Returns the primary facing, or -1 if n/a
     */
    public int getFacing() {
        if ((Entity.NONE != conveyance) && (game != null)) {
            Entity transporter = game.getEntity(conveyance);
            if (transporter == null) {
                transporter = game.getOutOfGameEntity(conveyance);
            }

            if (transporter != null) {
                return transporter.getFacing();
            }
        }
        return facing;
    }

    public String getFacingName(int facing) {
        return switch (facing) {
            case 0 -> Messages.getString("Entity.facing.north");
            case 1 -> Messages.getString("Entity.facing.northeast");
            case 2 -> Messages.getString("Entity.facing.southeast");
            case 3 -> Messages.getString("Entity.facing.south");
            case 4 -> Messages.getString("Entity.facing.southwest");
            case 5 -> Messages.getString("Entity.facing.northwest");
            default -> "";
        };
    }

    /**
     * Sets the primary facing.
     */
    public void setFacing(int facing) {
        this.facing = FireControl.correctFacing(facing);
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

    public void setSecondaryFacing(int sec_facing) {
        setSecondaryFacing(sec_facing, true);
    }

    /**
     * Sets the secondary facing.
     * Optionally does not fire a game change event (useful for bot evaluation)
     */
    public void setSecondaryFacing(int sec_facing, boolean fireEvent) {
        if (game != null) {
            // Only allow changing secondary facing if we haven't done so in a prior phase
            GamePhase phase = game.getPhase();
            if (phase != null) {
                // If we already twisted in an earlier phase, return out
                if (getAlreadyTwisted()) {
                    return;
                }
                if (phase.isTargeting() || phase.isOffboard() || phase.isFiring()) {
                    setAlreadyTwisted(true);
                }
            }
        }

        this.sec_facing = FireControl.correctFacing(sec_facing);

        if (fireEvent && (game != null)) {
            game.processGameEvent(new GameEntityChangeEvent(this, this));
        }
    }

    /**
     * Utility function that handles situations where a facing change
     * imparts some kind of permanent effect to the entity.
     */
    public void postProcessFacingChange() {

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
     * @return the closest valid secondary facing.
     */
    public abstract int clipSecondaryFacing(int dir);

    /**
     * @return whether this entity already changed a secondary facing in an earlier
     *         phase
     */
    public boolean getAlreadyTwisted() {
        return twistedPhase != null && twistedPhase.isBefore(game.getPhase());
    }

    /**
     * Used by TargetingPhaseDisplay.java and FiringDisplay.java
     *
     * @param value true sets twistedPhase to current phase (or leaves it if set);
     *              false unsets twistedPhase
     */
    public void setAlreadyTwisted(boolean value) {
        twistedPhase = (value) ? (twistedPhase == null) ? game.getPhase() : twistedPhase : null;
    }

    /**
     * Returns true if the entity has an RAC which is jammed and not destroyed
     * As of 5/22/2012 also returns true if there is a jammed and not destroyed
     * Ultra AC and the unofficial options is enabled. Jammed ACs and LACs can
     * also be unjammed if rapid-fire ACs is turned on.
     */
    public boolean canUnjamRAC() {
        for (WeaponMounted mounted : getTotalWeaponList()) {

            // Tanks can suffer a "weapon malfunction" critical. The weapon is marked
            // as jammed but it should not be unjammed in the movement phase, it should
            // be unjammed in the firing phase - that is, if this weapon is jammed from
            // a weapon malfunction critical, we shouldn't unjam it using this method.
            if (isVehicle() && this instanceof Tank tank) {
                if (tank.getJammedWeapons().contains(mounted)) {
                    continue;
                }
            }

            int ammotype = mounted.getType().getAmmoType();
            if ((ammotype == AmmoType.T_AC_ROTARY)
                    && mounted.isJammed() && !mounted.isDestroyed()) {
                return true;
            }
            if (((ammotype == AmmoType.T_AC_ULTRA)
                    || (ammotype == AmmoType.T_AC_ULTRA_THB)
                    || (ammotype == AmmoType.T_AC)
                    || (ammotype == AmmoType.T_LAC)
                    || (ammotype == AmmoType.T_AC_IMP)
                    || (ammotype == AmmoType.T_PAC))
                    && mounted.isJammed()
                    && !mounted.isDestroyed()
                    && game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_UNJAM_UAC)) {
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
     * Returns true if the entity can pick up ground objects
     */
    public boolean canPickupGroundObject() {
        return false;
    }

    /**
     * The maximum tonnage of ground objects that can be picked up by this unit
     */
    public double maxGroundObjectTonnage() {
        return 0.0;
    }

    /**
     * Put a ground object into the given location
     */
    public void pickupGroundObject(ICarryable carryable, Integer location) {
        if (carriedObjects == null) {
            carriedObjects = new HashMap<>();
        }

        // "none" means we should just put it wherever it goes by default.
        // rules checks are done prior to this, so we just set the data
        if (location == null || location == LOC_NONE) {
            for (Integer defaultLocation : getDefaultPickupLocations()) {
                carriedObjects.put(defaultLocation, carryable);
            }
        } else {
            carriedObjects.put(location, carryable);
        }
        endOfTurnCargoInteraction = true;
    }

    /**
     * Remove a specific carried object - useful for when you have the object
     * but not its location, or when an object is being carried in multiple
     * locations.
     */
    public void dropGroundObject(ICarryable carryable, boolean isUnload) {
        // build list of locations to clear out
        List<Integer> locationsToClear = new ArrayList<>();

        for (Integer location : carriedObjects.keySet()) {
            if (carriedObjects.get(location).equals(carryable)) {
                locationsToClear.add(location);
            }
        }

        for (Integer location : locationsToClear) {
            carriedObjects.remove(location);
        }

        // if it's not an "unload", we're going to leave the "end of turn cargo
        // interaction" flag alone
        if (isUnload) {
            endOfTurnCargoInteraction = true;
        }
    }

    /**
     * Remove a ground object (cargo) from the given location
     */
    public void dropGroundObject(int location) {
        carriedObjects.remove(location);
    }

    /**
     * Convenience method to drop all cargo.
     */
    public void dropGroundObjects() {
        carriedObjects.clear();
    }

    /**
     * Get the object carried in the given location. May return null.
     */
    public ICarryable getCarriedObject(int location) {
        return carriedObjects.get(location);
    }

    public Map<Integer, ICarryable> getCarriedObjects() {
        return carriedObjects;
    }

    public void setCarriedObjects(Map<Integer, ICarryable> value) {
        carriedObjects = value;
    }

    public List<ICarryable> getDistinctCarriedObjects() {
        return carriedObjects.values().stream().distinct().toList();
    }

    /**
     * A list of the "default" cargo pick up locations for when none is specified
     */
    protected List<Integer> getDefaultPickupLocations() {
        return List.of(LOC_NONE);
    }

    /**
     * A list of all the locations that the entity can use to pick up cargo
     * following the TacOps
     * "one handed" pickup rules
     */
    public List<Integer> getValidHalfWeightPickupLocations(ICarryable cargo) {
        return List.of(LOC_NONE);
    }

    /**
     * Whether a weapon in a given location can be fired,
     * given the entity's currently carried cargo
     */
    public boolean canFireWeapon(int location) {
        if (getBlockedFiringLocations() == null) {
            return true;
        }

        // loop through everything we are carrying
        // if the weapon location is blocked by the carried object, then we cannot fire
        // the weapon
        for (int carriedObjectLocation : getCarriedObjects().keySet()) {
            if (getBlockedFiringLocations().containsKey(carriedObjectLocation) &&
                    getBlockedFiringLocations().get(carriedObjectLocation).contains(location)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Method that returns the mapping between locations which, if cargo is carried,
     * block other locations from firing.
     */
    protected Map<Integer, List<Integer>> getBlockedFiringLocations() {
        return null;
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
     * @return this entity's walking/cruising mp, factoring in all MP-changing
     *         effects.
     */
    public int getWalkMP() {
        return getWalkMP(MPCalculationSetting.STANDARD);
    }

    public int getWalkMP(MPCalculationSetting mpCalculationSetting) {
        int mp = getOriginalWalkMP();

        if (!mpCalculationSetting.ignoreHeat) {
            mp = Math.max(0, mp - getHeatMPReduction());
        }

        if (!mpCalculationSetting.ignoreCargo) {
            mp = Math.max(mp - getCargoMpReduction(this), 0);
        }

        if (!mpCalculationSetting.ignoreWeather && (game != null)) {
            int weatherModifier = game.getPlanetaryConditions().getMovementMods(this);
            mp = Math.max(mp + weatherModifier, 0);
        }

        if (!mpCalculationSetting.ignoreGravity) {
            mp = applyGravityEffectsOnMP(mp);
        }

        return mp;
    }

    /**
     * @return The number of movement points (MP) lost due to the current heat level of the unit
     */
    public int getHeatMPReduction() {
        if ((game != null) && game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_HEAT)) {
            if (heat < 30) {
                return heat / 5;
            } else if (heat >= 49) {
                return 9;
            } else if (heat >= 43) {
                return 8;
            } else if (heat >= 37) {
                return 7;
            } else if (heat >= 31) {
                return 6;
            } else {
                return 5;
            }
        } else {
            return heat / 5;
        }
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

    /** @return This entity's unmodified running/flank mp. */
    public int getOriginalRunMP() {
        return (int) Math.ceil(getOriginalWalkMP() * 1.5);
    }

    /** @return This entity's running/flank mp modified for all in-game effects. */
    public int getRunMP() {
        return getRunMP(MPCalculationSetting.STANDARD);
    }

    public int getRunMP(MPCalculationSetting mpCalculationSetting) {
        return (int) Math.ceil(getWalkMP(mpCalculationSetting) * 1.5);
    }

    /**
     * Returns run MP without considering any MASC systems
     */
    public int getRunMPwithoutMASC() {
        return getRunMP(MPCalculationSetting.NO_MASC);
    }

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
     * Returns this entity's sprinting MP, modified for all its current
     * circumstances such
     * as gravity and damage. See {@link MPCalculationSetting#STANDARD}.
     * For units that can't sprint, this is equal to the modified run/flank MP.
     *
     * @return This entity's modified sprinting MP
     */
    public int getSprintMP() {
        return getSprintMP(MPCalculationSetting.STANDARD);
    }

    /**
     * Returns this entity's sprinting MP, modified according to the given setting.
     * For units that can't sprint, this is equal to the modified run/flank MP.
     *
     * @return This entity's modified sprinting MP
     */
    public int getSprintMP(MPCalculationSetting mpCalculationSetting) {
        return getRunMP(mpCalculationSetting);
    }

    /**
     * Returns sprint MP without considering MASC
     */
    public int getSprintMPwithOneMASC() {
        return getSprintMP(MPCalculationSetting.ONE_MASC);
    }

    /**
     * Returns sprint MP without considering MASC
     */
    public int getSprintMPwithoutMASC() {
        return getSprintMP(MPCalculationSetting.NO_MASC);
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
        return getRunMP(MPCalculationSetting.NO_GRAVITY);
    }

    /**
     * Returns this entity's original jumping mp.
     */
    public int getOriginalJumpMP() {
        return getOriginalJumpMP(false);
    }

    public int getOriginalJumpMP(boolean ignoreModularArmor) {

        if (!ignoreModularArmor && hasModularArmor()) {
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
        return getJumpMP(MPCalculationSetting.STANDARD);
    }

    public int getJumpMP(MPCalculationSetting mpCalculationSetting) {
        if (mpCalculationSetting.ignoreGravity) {
            return getOriginalJumpMP();
        } else {
            return applyGravityEffectsOnMP(getOriginalJumpMP());
        }
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
        return getJumpMP(MPCalculationSetting.DEDUCT_SUBMERGED_JJ);
    }

    /**
     * Tanks and certain other units can get a +1 bonus to MP if their move is
     * entirely on pavement.
     *
     * @return true if the <code>Entity</code> gets a movement bonus on pavement
     */
    public boolean isEligibleForPavementOrRoadBonus() {
        return false;
    }

    /**
     * Returns the absolute elevation above ground level 0 that this entity
     * would be on if it were placed into the specified hex.
     * Hovercraft, naval vessels, and hydrofoils move on the
     * surface of the water
     */
    public int elevationOccupied(Hex hex) {
        return elevationOccupied(hex, getElevation());
    }

    public int elevationOccupied(Hex hex, int elevation) {
        if (hex == null) {
            return 0;
        }
        if ((movementMode == EntityMovementMode.VTOL)
                || (movementMode == EntityMovementMode.WIGE)) {
            return hex.getLevel() + elevation;
        } else if (((movementMode == EntityMovementMode.HOVER)
                || (movementMode == EntityMovementMode.NAVAL)
                || (movementMode == EntityMovementMode.HYDROFOIL)
                || hex.containsTerrain(Terrains.ICE))
                && hex.containsTerrain(Terrains.WATER)) {
            return hex.getLevel();
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
     * terrain if the Entity is at the specified elevation. Elevation generally
     * only matters for units like WiGEs or VTOLs.
     *
     * @param c
     * @param currElevation
     * @return
     */
    public boolean isLocationProhibited(Coords c, int currElevation) {
        Hex hex = game.getBoard().getHex(c);
        if (hex.containsTerrain(Terrains.IMPASSABLE)) {
            return !isAirborne();
        }

        if (hex.containsTerrain(Terrains.SPACE) && doomedInSpace()) {
            return true;
        }

        // Additional restrictions for hidden units
        if (isHidden()) {
            // Can't deploy in paved hexes
            if (hex.containsTerrain(Terrains.PAVEMENT)
                    || hex.containsTerrain(Terrains.ROAD)) {
                return true;
            }
            // Can't deploy on a bridge
            if ((hex.terrainLevel(Terrains.BRIDGE_ELEV) == currElevation)
                    && hex.containsTerrain(Terrains.BRIDGE)) {
                return true;
            }
            // Can't deploy on the surface of water
            if (hex.containsTerrain(Terrains.WATER) && (currElevation == 0)) {
                return true;
            }
        }

        return false;
    }

    /**
     * @return True if the given board is prohibited to this unit.
     */
    public boolean isBoardProhibited(int mapType) {
        return ((mapType == Board.T_GROUND) && doomedOnGround())
                || ((mapType == Board.T_ATMOSPHERE) && doomedInAtmosphere())
                || ((mapType == Board.T_SPACE) && doomedInSpace());
    }

    /**
     * Returns true if the specified hex contains some sort of deadly
     * terrain.
     */
    public boolean isLocationDeadly(Coords c) {
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
     * Joins the abbreviations for the locations into a String with / as the
     * separator.
     * If the number of locations exceeds the provided limit, the result is
     * abbreviated. By default the abbreviation is simply an asterisk, but Meks have
     * specific abbreviations locations that include all torso or leg positions.
     *
     * @param locations A list of location indices
     * @param limit     The maximum number of locations to show in full
     * @return A string formatted for display that shows the locations
     */
    public String joinLocationAbbr(List<Integer> locations, int limit) {
        if (locations.size() > limit) {
            return "*";
        } else {
            return locations.stream().map(this::getLocationAbbr).collect(Collectors.joining("/"));
        }
    }

    /**
     * Rolls the to-hit number
     */
    public abstract HitData rollHitLocation(int table, int side, int aimedLocation,
            AimingMode aimingMode, int cover);

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
     * @return The index of the first armored location (skipping vehicle body, et.
     *         al.)
     */
    public int firstArmorIndex() {
        return 0;
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
     * Sets the internal structure for every location to appropriate undamaged
     * values for the unit and location.
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
        if (allowChange || (exposure[loc] > ILocationExposureStatus.BREACHED)) { // can't change BREACHED status
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
        boolean mtHeat = game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_HEAT);
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
                && hasAbility(OptionsConstants.UNOFF_SOME_LIKE_IT_HOT)) {
            mod--;
        }

        return mod;
    }

    /**
     * Creates a new mount for this equipment and adds it in.
     */
    public Mounted<?> addEquipment(EquipmentType etype, int loc)
            throws LocationFullException {
        return addEquipment(etype, loc, false);
    }

    /**
     * Creates a new mount for this equipment and adds it in.
     */
    public Mounted<?> addEquipment(EquipmentType etype, int loc,
            boolean rearMounted) throws LocationFullException {
        return addEquipment(etype, loc, rearMounted,
                BattleArmor.MOUNT_LOC_NONE, false, false, false, false, false);
    }

    /**
     * Creates a new mount for this equipment and adds it in.
     */
    public Mounted<?> addEquipment(EquipmentType etype, int loc,
            boolean rearMounted, int baMountLoc, boolean isArmored,
            boolean isTurreted) throws LocationFullException {
        return addEquipment(etype, loc, rearMounted, baMountLoc, isArmored,
                isTurreted, false, false, false);
    }

    public Mounted<?> addEquipment(EquipmentType etype, int loc,
            boolean rearMounted, int baMountLoc, boolean isArmored,
            boolean isTurreted, boolean isSponsonTurreted)
            throws LocationFullException {
        return addEquipment(etype, loc, rearMounted, baMountLoc, isArmored,
                isTurreted, isSponsonTurreted, false, false);
    }

    public Mounted<?> addEquipment(EquipmentType etype, int loc,
            boolean rearMounted, int baMountLoc, boolean isArmored,
            boolean isTurreted, boolean isSponsonTurreted,
            boolean isPintleTurreted) throws LocationFullException {
        return addEquipment(etype, loc, rearMounted, baMountLoc, isArmored,
                isTurreted, isSponsonTurreted, isPintleTurreted, false);
    }

    public Mounted<?> addEquipment(EquipmentType etype, int loc,
            boolean rearMounted, int baMountLoc, boolean isArmored,
            boolean isTurreted, boolean isSponsonTurreted,
            boolean isPintleTurreted, boolean isOmniPodded) throws LocationFullException {
        Mounted<?> mounted = Mounted.createMounted(this, etype);
        mounted.setArmored(isArmored);
        mounted.setBaMountLoc(baMountLoc);
        mounted.setMekTurretMounted(isTurreted);
        mounted.setSponsonTurretMounted(isSponsonTurreted);
        mounted.setPintleTurretMounted(isPintleTurreted);
        mounted.setOmniPodMounted(isOmniPodded);
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
    public Mounted<?> addEquipment(EquipmentType etype, int loc,
            boolean rearMounted, int nAmmo) throws LocationFullException {
        Mounted<?> mounted = Mounted.createMounted(this, etype);
        addEquipment(mounted, loc, rearMounted, nAmmo);
        return mounted;

    }

    /**
     * indicate whether this is a bomb mount
     */
    public Mounted<?> addBomb(EquipmentType etype, int loc)
            throws LocationFullException {
        Mounted<?> mounted = Mounted.createMounted(this, etype);
        addBomb(mounted, loc);
        return mounted;
    }

    protected void addBomb(Mounted<?> mounted, int loc)
            throws LocationFullException {
        mounted.setBombMounted(true);
        addEquipment(mounted, loc, false);
    }

    public WeaponMounted addWeaponGroup(EquipmentType etype, int loc)
            throws LocationFullException {
        WeaponMounted mounted = (WeaponMounted) Mounted.createMounted(this, etype);
        addEquipment(mounted, loc, false, true);
        return mounted;
    }

    /**
     * indicate whether this is bodymounted for BAs
     */
    public Mounted<?> addEquipment(EquipmentType etype, int loc,
            boolean rearMounted, int baMountLoc, boolean dwpMounted)
            throws LocationFullException {
        Mounted<?> mounted = Mounted.createMounted(this, etype);
        mounted.setBaMountLoc(baMountLoc);
        mounted.setDWPMounted(dwpMounted);
        addEquipment(mounted, loc, rearMounted);
        return mounted;
    }

    protected void addEquipment(Mounted<?> mounted, int loc, boolean rearMounted,
            int nAmmo) throws LocationFullException {
        if ((mounted instanceof AmmoMounted) && (nAmmo > 1)) {
            mounted.setByShot(true);
            mounted.setShotsLeft(nAmmo);
            mounted.setOriginalShots(nAmmo);
            double tonnage = Math.max(1, nAmmo / ((AmmoMounted) mounted).getType().getShots()) * mounted.getTonnage();
            ((AmmoMounted) mounted).setAmmoCapacity(tonnage);
        }

        addEquipment(mounted, loc, rearMounted);
    }

    protected void addEquipment(Mounted<?> mounted, int loc, boolean rearMounted,
            boolean isWeaponGroup) throws LocationFullException {
        if (mounted instanceof WeaponMounted) {
            mounted.setWeaponGroup(isWeaponGroup);
        }

        addEquipment(mounted, loc, rearMounted);
    }

    public void addEquipment(Mounted<?> mounted, int loc, boolean rearMounted)
            throws LocationFullException {
        mounted.setLocation(loc, rearMounted);
        equipmentList.add(mounted);

        compositeTechLevel.addComponent(mounted.getType());
        if (mounted.isArmored()) {
            compositeTechLevel.addComponent(TA_ARMORED_COMPONENT);
        }

        // add it to the proper sub-list
        if (mounted instanceof WeaponMounted) {
            totalWeaponList.add((WeaponMounted) mounted);
            if (mounted.isWeaponGroup()) {
                weaponGroupList.add((WeaponMounted) mounted);
            } else if (mounted.getType() instanceof BayWeapon) {
                weaponBayList.add((WeaponMounted) mounted);
            } else {
                weaponList.add((WeaponMounted) mounted);
            }

            if (mounted.getType().hasFlag(WeaponType.F_ARTILLERY)) {
                aTracker.addWeapon(mounted);
            }

            // one-shot launchers need their single shot of ammo added.
            if ((mounted.getType().hasFlag(WeaponType.F_ONESHOT)
                    || (isSupportVehicle() && (mounted.getType() instanceof InfantryWeapon)))
                    && (AmmoType.getOneshotAmmo(mounted) != null)) {
                addOneshotAmmo(mounted);
            }
        }
        if (mounted instanceof AmmoMounted) {
            ammoList.add((AmmoMounted) mounted);
        }
        if (mounted instanceof BombMounted) {
            bombList.add((BombMounted) mounted);
        }
        if (mounted instanceof MiscMounted) {
            miscList.add((MiscMounted) mounted);
        }
        if (!(mounted instanceof AmmoMounted) && !(mounted instanceof MiscMounted)
                && !(mounted instanceof WeaponMounted)) {
            logger.error("Trying to add plain Mounted class {} on {}!", mounted, this);
        }
    }

    private void addOneshotAmmo(Mounted<?> mounted) throws LocationFullException {
        EquipmentType ammo;
        int shots;
        if (mounted.getType() instanceof InfantryWeapon) {
            ammo = EquipmentType.get(EquipmentTypeLookup.INFANTRY_AMMO);
            shots = ((InfantryWeapon) mounted.getType()).getShots() * (int) mounted.getSize();
        } else {
            ammo = AmmoType.getOneshotAmmo(mounted);
            shots = 1;
        }
        if (ammo == null) {
            logger.error("Equipment lookup failed for ammo for " + mounted.getName());
            return;
        }
        Mounted<?> m = Mounted.createMounted(this, ammo);
        m.setOmniPodMounted(mounted.isOmniPodMounted());
        // BA pop-up mines can be fired individually and need a shot for each launcher
        // in the squad.
        if (mounted.getType().hasFlag(WeaponType.F_BA_INDIVIDUAL)) {
            shots = getTotalInternal();
        }
        m.setShotsLeft(shots);
        m.setOriginalShots(shots);
        mounted.setLinked(m);
        addEquipment(m, Entity.LOC_NONE, false);
        // Fusillade gets a second round, which can be a different munition type so
        // need to allow for two separate mounts. Some infantry weapons have alternate
        // inferno ammo, which will use the same mechanism but start with zero shots.
        if (mounted.getType().hasFlag(WeaponType.F_DOUBLE_ONESHOT)) {
            Mounted<?> m2 = Mounted.createMounted(this, m.getType());
            m2.setOmniPodMounted(mounted.isOmniPodMounted());
            m2.setShotsLeft(shots);
            m2.setOriginalShots(shots);
            m.setLinked(m2);
            addEquipment(m2, Entity.LOC_NONE, false);
        } else if ((mounted.getType() instanceof InfantryWeapon)
                && ((InfantryWeapon) mounted.getType()).hasInfernoAmmo()) {
            Mounted<?> m2 = Mounted.createMounted(this, EquipmentType.get(EquipmentTypeLookup.INFANTRY_INFERNO_AMMO));
            m2.setOmniPodMounted(mounted.isOmniPodMounted());
            m2.setShotsLeft(0);
            m.setLinked(m2);
            addEquipment(m2, Entity.LOC_NONE, false);
        }
    }

    public void addFailedEquipment(String s) {
        failedEquipmentList.add(s);
    }

    /**
     * Returns the equipment number of the specified equipment, or -1 if
     * equipment is not present.
     */
    public int getEquipmentNum(Mounted<?> mounted) {
        if (mounted != null) {
            return equipmentList.indexOf(mounted);
        }
        return -1;
    }

    /**
     * Returns an enumeration of all equipment
     */
    public List<Mounted<?>> getEquipment() {
        return equipmentList;
    }

    /**
     * Returns the equipment, specified by number
     */
    public Mounted<?> getEquipment(int index) {
        try {
            return equipmentList.get(index);
        } catch (IndexOutOfBoundsException ex) {
            return null;
        }
    }

    public WeaponMounted getWeapon(int index) {
        Mounted<?> mounted = getEquipment(index);
        return mounted instanceof WeaponMounted ? (WeaponMounted) mounted : null;
    }

    public MiscMounted getMisc(int index) {
        Mounted<?> mounted = getEquipment(index);
        return mounted instanceof MiscMounted ? (MiscMounted) mounted : null;
    }

    public AmmoMounted getAmmo(int index) {
        Mounted<?> mounted = getEquipment(index);
        return mounted instanceof AmmoMounted ? (AmmoMounted) mounted : null;
    }

    public EquipmentType getEquipmentType(CriticalSlot cs) {
        if (cs.getType() != CriticalSlot.TYPE_EQUIPMENT) {
            return null;
        }
        Mounted<?> m = cs.getMount();
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
        for (AmmoMounted amounted : getAmmo()) {
            // FIXME: Consider new AmmoType::equals / BombType::equals
            if (amounted.getType().equals(et) && !amounted.isDumping()) {
                totalShotsLeft += amounted.getUsableShotsLeft();
            }
        }
        return totalShotsLeft;
    }

    /**
     * Determine how much ammunition (of all munition types) remains which is
     * compatible with the given weapon.
     *
     * @param weapon The weapon being considered
     * @return the <code>int</code> count of the amount of shots of all
     *         munitions available for the given weapon.
     */
    public int getTotalMunitionsOfType(WeaponMounted weapon) {
        int totalShotsLeft = 0;

        // specifically don't count caseless munitions as being of the same type as
        // non-caseless
        for (AmmoMounted amounted : getAmmo()) {
            boolean canSwitchToAmmo = AmmoType.canSwitchToAmmo(weapon, amounted.getType());

            if (canSwitchToAmmo && !amounted.isDumping()) {
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

    public Iterator<WeaponMounted> getWeapons() {
        if (usesWeaponBays()) {
            return weaponBayList.iterator();
        }
        if (isCapitalFighter()) {
            return weaponGroupList.iterator();
        }

        return weaponList.iterator();
    }

    public List<WeaponMounted> getIndividualWeaponList() {
        return weaponList;
    }

    public List<WeaponMounted> getWeaponList() {
        if (usesWeaponBays()) {
            if (!hasQuirk(OptionsConstants.QUIRK_POS_INTERNAL_BOMB)) {
                return weaponBayList;
            }
            List<WeaponMounted> combinedWeaponList = new ArrayList<WeaponMounted>(weaponBayList);
            for (Iterator<WeaponMounted> iterator = weaponList.iterator(); iterator.hasNext();) {
                WeaponMounted next = iterator.next();
                if (next.isGroundBomb() || next.isBombMounted()) {
                    combinedWeaponList.add(next);
                }
            }
            return combinedWeaponList;

        }
        if (isCapitalFighter()) {
            return weaponGroupList;
        }

        return weaponList;
    }

    public List<WeaponMounted> getTotalWeaponList() {
        // return full weapon list even bay mounts and weapon groups
        return totalWeaponList;
    }

    public List<WeaponMounted> getWeaponBayList() {
        return weaponBayList;
    }

    public List<WeaponMounted> getWeaponGroupList() {
        return weaponGroupList;
    }

    /**
     * Returns true if the given weapon is valid for the current phase.
     *
     * @param mounted The WeaponMounted to test
     * @return True if valid, else false
     */
    public boolean isWeaponValidForPhase(@Nullable WeaponMounted mounted) {
        // Start reached, now we can attempt to pick a weapon.
        if ((mounted != null)
                && (mounted.isReady())
                && (!(mounted.getType().hasFlag(WeaponType.F_AMS) && mounted.curMode().equals(Weapon.MODE_AMS_ON)))
                && (!(mounted.getType().hasFlag(WeaponType.F_AMS) && mounted.curMode().equals(Weapon.MODE_AMS_OFF)))
                && (!mounted.getType().hasFlag(WeaponType.F_AMSBAY))
                && (!(mounted.hasModes() && mounted.curMode().equals("Point Defense")))
                && ((mounted.getLinked() == null)
                        || mounted.getLinked().getType().hasFlag(MiscType.F_AP_MOUNT)
                        || (mounted.getLinked().getUsableShotsLeft() > 0))) {

            // TAG only in the correct phase...
            if ((mounted.getType().hasFlag(WeaponType.F_TAG) && !getGame().getPhase().isOffboard())
                    || (!mounted.getType().hasFlag(WeaponType.F_TAG) && getGame().getPhase().isOffboard())) {
                return false;
            }

            // Artillery or Bearings-only missiles only in the targeting phase...
            if (getGame().getPhase().isTargeting()
                    && !(mounted.getType().hasFlag(WeaponType.F_ARTILLERY)
                            || mounted.isInBearingsOnlyMode()
                            || ((getAltitude() == 0) && (mounted.getType() instanceof CapitalMissileWeapon)))) {
                return false;
            }
            // No Bearings-only missiles in the firing phase
            if (mounted.isInBearingsOnlyMode() && getGame().getPhase().isFiring()) {
                return false;
            }

            // No linked MGs...
            if (mounted.getType().hasFlag(WeaponType.F_MG) && hasLinkedMGA(mounted)) {
                return false;
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
        for (WeaponMounted mounted : getTotalWeaponList()) {
            if (mounted.getType().getAmmoType() != AmmoType.T_NA) {
                loadWeapon(mounted);
            }
        }
    }

    /**
     * Tries to load the specified weapon with the first available ammo
     */
    public void loadWeapon(WeaponMounted mounted) {
        for (AmmoMounted mountedAmmo : getAmmo()) {
            if (loadWeapon(mounted, mountedAmmo)) {
                break;
            }
        }
    }

    /**
     * Tries to load the specified weapon with the first available ammo of the
     * same munition type as currently in use. If this fails, use first ammo.
     *
     * If this is a weapon bay, try to load the weapon with ammo in the same bay,
     * and if it fails, load with compatible ammo in the same location.
     *
     * If this unit is part of a train, also check the vehicles directly connected
     * to it for compatible ammo
     */
    public void loadWeaponWithSameAmmo(WeaponMounted mounted) {
        for (AmmoMounted mountedAmmo : getAmmo()) {
            if (loadWeaponWithSameAmmo(mounted, mountedAmmo)) {
                return;
            }
        }
        // Check the unit towing this one for ammo
        if (getTowedBy() != Entity.NONE) {
            Entity ahead = game.getEntity(getTowedBy());
            if (ahead != null) {
                for (AmmoMounted towedByAmmo : ahead.getAmmo()) {
                    if (loadWeaponWithSameAmmo(mounted, towedByAmmo)) {
                        return;
                    }
                }
            }
        }
        // Then check the unit towed by this one for ammo
        if (getTowing() != Entity.NONE) {
            Entity behind = game.getEntity(getTowing());
            if (behind != null) {
                for (AmmoMounted towingAmmo : behind.getAmmo()) {
                    if (loadWeaponWithSameAmmo(mounted, towingAmmo)) {
                        return;
                    }
                }
            }
        }
        // fall back to use any ammo
        loadWeapon(mounted);
    }

    /**
     * Tries to load the specified weapon with the specified ammo. Returns true
     * if successful, false otherwise.
     */
    public boolean loadWeapon(WeaponMounted mounted, AmmoMounted mountedAmmo) {
        boolean success = false;
        WeaponType wtype = mounted.getType();
        AmmoType atype = mountedAmmo.getType();

        if (mountedAmmo.isAmmoUsable() && !wtype.hasFlag(WeaponType.F_ONESHOT)
                && (atype.getAmmoType() == wtype.getAmmoType())
                && (atype.getRackSize() == wtype.getRackSize())) {
            mounted.setLinked(mountedAmmo);
            success = true;
        } else if ((wtype.hasFlag(WeaponType.F_DOUBLE_ONESHOT)
                || (wtype.getAmmoType() == AmmoType.T_INFANTRY))
                && (mountedAmmo.getLocation() == Entity.LOC_NONE)) {
            // Make sure this ammo is in the chain, then move it to the head.
            for (Mounted<?> current = mounted; current != null; current = current.getLinked()) {
                if (current == mountedAmmo) {
                    current.getLinkedBy().setLinked(current.getLinked());
                    current.setLinked(mounted.getLinked());
                    mounted.setLinked(current);
                    return true;
                }
            }
        }
        return success;
    }

    /**
     * Tries to load the specified weapon with the specified ammo. Returns true
     * if successful, false otherwise.
     */
    public boolean loadWeaponWithSameAmmo(WeaponMounted mounted, AmmoMounted mountedAmmo) {
        AmmoType atype = mountedAmmo.getType();
        AmmoMounted oldammo = mounted.getLinkedAmmo();

        if ((oldammo != null) && !oldammo.getType().equals(atype)) {
            return false;
        }

        return loadWeapon(mounted, mountedAmmo);
    }

    /**
     * @return True when this unit has fired a weapon has been fired this turn.
     */
    public boolean weaponFired() {
        for (int loc = 0; loc < locations(); loc++) {
            if (weaponFiredFrom(loc)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return True when a weapon has been fired from the specified location this turn.
     */
    public boolean weaponFiredFrom(int loc) {
        // check critical slots for used weapons
        for (int i = 0; i < getNumberOfCriticals(loc); i++) {
            CriticalSlot slot = getCritical(loc, i);
            // ignore empty & system slots
            if ((slot == null) || (slot.getType() != CriticalSlot.TYPE_EQUIPMENT)) {
                continue;
            }
            Mounted<?> mounted = slot.getMount();
            if ((mounted.getType() instanceof WeaponType) && mounted.isUsedThisRound()) {
                return true;
            }
        }
        return false;
    }

    public List<AmmoMounted> getAmmo() {
        return ammoList;
    }

    /**
     * @param weapon we want to find available ammo for
     * @return an ArrayList containing _one_ Mounted ammo for each viable type
     */
    public List<AmmoMounted> getAmmo(WeaponMounted weapon) {
        Set<EquipmentType> etypes = new HashSet<>();
        ArrayList<AmmoMounted> ammos = new ArrayList<>();

        // Only add one valid ammo to the list to reduce repeat calculations
        for (AmmoMounted ammo : ammoList) {
            if (AmmoType.isAmmoValid(ammo, weapon.getType())) {
                if (etypes.add(ammo.getType())) {
                    ammos.add(ammo);
                }
            }
        }
        // One entry per ammo type that is currently usable by this weapon.
        return ammos;
    }

    public List<MiscMounted> getMisc() {
        return miscList;
    }

    public List<BombMounted> getBombs() {
        return bombList;
    }

    /**
     * Returns a list of operable bombs with shots left and with the given flag (e.g. AmmoType.F_SPACE_BOMB)
     *
     * @param flag The AmmoType bomb flag to search for
     * @return The list of found bombs
     * @see Mounted#isOperable()
     */
    public List<BombMounted> getBombs(EquipmentFlag flag) {
        return getBombs().stream()
                .filter(b -> b.getType().hasFlag(flag))
                .filter(Mounted::isOperable)
                .filter(Mounted::hasUsableShotsLeft)
                .toList();
    }

    /**
     * Reset bomb attacks according to what bombs are available.
     */
    protected void resetBombAttacks() {
        // Remove all bomb attacks
        List<WeaponMounted> bombAttacksToRemove = new ArrayList<>();
        EquipmentType spaceBomb = EquipmentType.get(IBomber.SPACE_BOMB_ATTACK);
        EquipmentType altBomb = EquipmentType.get(IBomber.ALT_BOMB_ATTACK);
        EquipmentType diveBomb = EquipmentType.get(IBomber.DIVE_BOMB_ATTACK);
        for (WeaponMounted eq : totalWeaponList) {
            // FIXME: Consider new BombType::equals
            if (eq.getType().equals(spaceBomb) || eq.getType().equals(altBomb)
                    || eq.getType().equals(diveBomb)) {
                bombAttacksToRemove.add(eq);
            } else if (eq.getLinked() != null && eq.getLinked().isInternalBomb()) {
                // Remove any used internal bombs
                if (eq.getLinked().getUsableShotsLeft() <= 0) {
                    bombAttacksToRemove.add(eq);
                }
            }
        }
        equipmentList.removeAll(bombAttacksToRemove);
        weaponList.removeAll(bombAttacksToRemove);
        totalWeaponList.removeAll(bombAttacksToRemove);
        weaponGroupList.removeAll(bombAttacksToRemove);
        weaponBayList.removeAll(bombAttacksToRemove);

        boolean foundSpaceBomb = false;
        int addedBombAttacks = 0;

        for (BombMounted m : getBombs()) {
            // Add the space bomb attack
            if (!foundSpaceBomb
                    && game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_SPACE_BOMB)
                    && m.getType().hasFlag(AmmoType.F_SPACE_BOMB)
                    && isBomber()
                    && game.getBoard().inSpace()) {
                try {
                    WeaponMounted bomb = (WeaponMounted) addEquipment(spaceBomb, m.getLocation(), false);
                    if (hasETypeFlag(ETYPE_FIGHTER_SQUADRON)) {
                        bomb.setWeaponGroup(true);
                        weaponGroupList.add(bomb);
                    }
                } catch (LocationFullException ignored) {

                }
                foundSpaceBomb = true;
            }

            if (!game.getBoard().inSpace()
                    && m.getType().hasFlag(AmmoType.F_GROUND_BOMB)
                    && !((this instanceof LandAirMek)
                            && (getConversionMode() == LandAirMek.CONV_MODE_MEK))) {
                if (addedBombAttacks < 1) {
                    try {
                        WeaponMounted bomb = (WeaponMounted) addEquipment(diveBomb, m.getLocation(), false);
                        if (hasETypeFlag(ETYPE_FIGHTER_SQUADRON)) {
                            bomb.setWeaponGroup(true);
                            weaponGroupList.add(bomb);
                        }
                    } catch (LocationFullException ignored) {

                    }
                }

                if ((addedBombAttacks < 10) && isBomber()) {
                    try {
                        WeaponMounted bomb = (WeaponMounted) addEquipment(altBomb, m.getLocation(), false);
                        if (hasETypeFlag(ETYPE_FIGHTER_SQUADRON)) {
                            bomb.setWeaponGroup(true);
                            weaponGroupList.add(bomb);
                        }
                    } catch (LocationFullException ignored) {

                    }
                }
                addedBombAttacks++;
            }
        }
    }

    /**
     * Removes the first misc eq. whose name equals the specified string. Used
     * for removing broken tree clubs.
     */
    public void removeMisc(String toRemove) {
        for (MiscMounted mounted : getMisc()) {
            if (mounted.getName().equals(toRemove)) {
                miscList.remove(mounted);
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
        equipmentList.removeIf(m -> (m.getType() instanceof BombType)
                || (m.getType() instanceof DiveBombAttack)
                || (m.getType() instanceof SpaceBombAttack)
                || (m.getType() instanceof AltitudeBombAttack)
                || (m.getType() instanceof ISAAAMissileWeapon)
                || (m.getType() instanceof CLAAAMissileWeapon)
                || (m.getType() instanceof ISASMissileWeapon)
                || (m.getType() instanceof ISASEWMissileWeapon)
                || (m.getType() instanceof CLASMissileWeapon)
                || (m.getType() instanceof CLASEWMissileWeapon)
                || (m.getType() instanceof ISLAAMissileWeapon)
                || (m.getType() instanceof CLLAAMissileWeapon)
                || (m.getType() instanceof BombArrowIV)
                /* || m.getType() instanceof CLBombArrowIV */
                || (m.getType() instanceof CLBombTAG)
                || (m.getType() instanceof ISBombTAG)
                || (m.getType() instanceof BombISRL10)
                || (m.getType() instanceof AlamoMissileWeapon));
        weaponList.removeIf(m -> (m.getType() instanceof DiveBombAttack)
                || (m.getType() instanceof SpaceBombAttack)
                || (m.getType() instanceof AltitudeBombAttack)
                || (m.getType() instanceof ISAAAMissileWeapon)
                || (m.getType() instanceof CLAAAMissileWeapon)
                || (m.getType() instanceof ISASMissileWeapon)
                || (m.getType() instanceof ISASEWMissileWeapon)
                || (m.getType() instanceof CLASMissileWeapon)
                || (m.getType() instanceof CLASEWMissileWeapon)
                || (m.getType() instanceof ISLAAMissileWeapon)
                || (m.getType() instanceof CLLAAMissileWeapon)
                || (m.getType() instanceof BombArrowIV)
                /* || m.getType() instanceof CLBombArrowIV */
                || (m.getType() instanceof CLBombTAG)
                || (m.getType() instanceof ISBombTAG)
                || (m.getType() instanceof BombISRL10)
                || (m.getType() instanceof AlamoMissileWeapon));
        ammoList.removeIf(m -> m.getType() instanceof BombType);
    }

    public List<MiscMounted> getClubs() {
        List<MiscMounted> rv = new ArrayList<>();
        for (MiscMounted m : getMisc()) {
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
    public boolean hasWorkingMisc(EquipmentFlag flag) {
        return hasWorkingMisc(flag, -1);
    }

    /**
     * Check if the entity has an arbitrary type of misc equipment
     *
     * @param flag      A MiscType.F_XXX
     * @param secondary A MiscType.S_XXX or -1 for don't care
     * @return true if at least one ready item.
     */
    public boolean hasWorkingMisc(EquipmentFlag flag, long secondary) {
        for (MiscMounted m : miscList) {
            if (m.isReady() && m.getType().hasFlag(flag)
                    && ((secondary == -1) || m.getType().hasSubType(secondary))) {
                return true;
            }
        }
        return false;
    }

    public boolean hasMisc(EquipmentFlag flag) {
        return miscList.stream().anyMatch(misc -> misc.getType().hasFlag(flag));
    }

    public List<MiscMounted> getMiscEquipment(EquipmentFlag flag) {
        return miscList.stream()
                .filter(item -> item.getType().hasFlag(flag))
                .collect(Collectors.toList());
    }

    /**
     * Returns the number of equipment of the given internal name that are mounted
     * on this unit, regardless of
     * their working condition. Ideally use {@link EquipmentTypeLookup} for the
     * internal name.
     *
     * @param internalName The {@link EquipmentType#internalName} of the equipment
     * @return The equipment count on this unit
     */
    public long countEquipment(String internalName) {
        return getEquipment().stream().filter(m -> m.is(internalName)).count();
    }

    /**
     * return how many misc equipments with the specified flag the unit has
     *
     * @param flag
     * @return
     */
    public int countWorkingMisc(EquipmentFlag flag) {
        return countWorkingMisc(flag, -1);
    }

    public int countWorkingMisc(EquipmentFlag flag, int location) {
        int count = 0;
        OUTER: for (MiscMounted m : getMisc()) {
            if (!m.isInoperable() && m.getType().hasFlag(flag)
                    && ((location == -1) || (m.getLocation() == location))) {
                if (m.hasModes()) {
                    for (Enumeration<EquipmentMode> e = m.getType().getModes(); e.hasMoreElements();) {
                        if (e.nextElement().equals("On") && !m.curMode().equals("On")) {
                            continue OUTER;
                        }
                    }
                }
                count++;
            }
        }
        return count;
    }

    public int countWorkingMisc(String internalName, int location) {
        int count = 0;
        OUTER: for (MiscMounted m : getMisc()) {
            if (!m.isInoperable() && m.getType().getInternalName().equalsIgnoreCase(internalName)
                    && ((location == -1) || (m.getLocation() == location))) {
                if (m.hasModes()) {
                    for (Enumeration<EquipmentMode> e = m.getType().getModes(); e.hasMoreElements();) {
                        if (e.nextElement().equals("On") && !m.curMode().equals("On")) {
                            continue OUTER;
                        }
                    }
                }
                count++;
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
        for (MiscMounted m : miscList) {
            if (m.isReady() && m.getType().internalName.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the entity has an arbitrary type of misc equipment
     *
     * @param flag      A MiscType.F_XXX
     * @param secondary A MiscType.S_XXX or -1 for don't care
     * @param location  The location to check e.g. Mek.LOC_LARM
     * @return true if at least one ready item.
     */
    public boolean hasWorkingMisc(EquipmentFlag flag, long secondary, int location) {
        // go through the location slot by slot, because of misc equipment that
        // is spreadable
        for (int slot = 0; slot < getNumberOfCriticals(location); slot++) {
            CriticalSlot crit = getCritical(location, slot);
            if ((null != crit)
                    && (crit.getType() == CriticalSlot.TYPE_EQUIPMENT)) {
                Mounted<?> mount = crit.getMount();
                if (mount == null) {
                    continue;
                }
                if ((mount.getType() instanceof MiscType type) && mount.isReady()) {
                    if (type.hasFlag(flag) && ((secondary == -1) || type.hasSubType(secondary))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Returns the CriticalSlots in the given location as a list. The returned list
     * can be empty
     * depending on the unit and the chosen slot but not null. The entries are not
     * filtered in
     * any way (could be null although that is probably an error in the internal
     * representation
     * of the unit.)
     *
     * @param location The location, e.g. Mek.LOC_HEAD
     * @return A list of CriticalSlots in that location, possibly empty
     */
    public List<CriticalSlot> getCriticalSlots(int location) {
        List<CriticalSlot> result = new ArrayList<>();
        for (int slot = 0; slot < getNumberOfCriticals(location); slot++) {
            result.add(getCritical(location, slot));
        }
        return result;
    }

    /**
     * @return true if the entity has any critical slot that isn't damaged yet
     */
    public boolean hasUndamagedCriticalSlots() {
        return IntStream.range(0, locations())
            .mapToLong(i -> getCriticalSlots(i)
                .stream()
                .filter(Objects::nonNull)
                .filter(CriticalSlot::isHittable)
                .count()
            ).sum() > 0;
    }

    /**
     * @return True when this unit has a RISC Super-Cooled Myomer System (even if
     *         the SCM is destroyed).
     */
    public boolean hasSCM() {
        return miscList.stream().anyMatch(m -> m.is(EquipmentTypeLookup.SCM));
    }

    /**
     * @return True when this unit has an operable RISC Super-Cooled Myomer System.
     */
    public boolean hasWorkingSCM() {
        return miscList.stream().filter(m -> m.is(EquipmentTypeLookup.SCM)).anyMatch(Mounted::isOperable);
    }

    public int damagedSCMCritCount() {
        return scmCritStateCount(CriticalSlot::isDamaged);
    }

    protected int scmCritStateCount(Predicate<CriticalSlot> slotState) {
        int stateAppliesCount = 0;
        for (int location = 0; location < locations(); location++) {
            for (int index = 0; index < crits[location].length; index++) {
                final CriticalSlot slot = crits[location][index];
                if ((slot != null) && (slot.getType() == CriticalSlot.TYPE_EQUIPMENT)
                        && slot.getMount().is(EquipmentTypeLookup.SCM) && slotState.test(slot)) {
                    stateAppliesCount++;
                }
            }
        }
        return stateAppliesCount;
    }

    /**
     * Returns the amount of heat that the entity can sink each turn.
     */
    public int getHeatCapacity() {
        return getHeatCapacity(true);
    }

    public abstract int getHeatCapacity(boolean radicalHeatSink);

    /**
     * Pretty-prints the heat capacity of a unit, including optional heat sinking systems.
     * Typically, this is equivalent to {@link #getHeatCapacity()},
     * but in the presence of Radical Heat Sinks, Coolant Pods, or the RISC Emergency Coolant System,
     * produces strings like "24 [36]" or "12 [+MoS]".
     * @return The formatted heat capacity
     */
    public String formatHeat() {
        // This method might make sense as an abstract method with overrides in Mek and Aero,
        // But since the implementation would be the same in both classes, this way avoids code duplication.
        int sinks;
        if (this instanceof Mek m) {
            sinks = m.getActiveSinks();
        } else if (this instanceof Aero a) {
            sinks = a.getHeatSinks();
        } else {
            return "(none)";
        }

        StringBuilder sb = new StringBuilder();
        int capacity = getHeatCapacity(false);
        sb.append(capacity);

        // Radical Heat Sinks
        if (hasWorkingMisc(MiscType.F_RADICAL_HEATSINK)) {
            capacity += sinks;
            sb.append(", ").append(capacity).append(" with RHS");
        }

        // Coolant Pod
        for (AmmoMounted m : getAmmo()) {
            if (m.getType().ammoType == AmmoType.T_COOLANT_POD) {
                capacity += sinks;
                sb.append(", ").append(capacity).append(" with Coolant Pod");
                break;
            }
        }

        // RISC ECS
        for (MiscMounted m : getMisc()) {
            if (m.getType().hasFlag(MiscType.F_EMERGENCY_COOLANT_SYSTEM)) {
                sb.append(", +MoS with RISC ECS");
            }
        }

        return sb.toString();
    }

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
    public @Nullable CriticalSlot getCritical(int loc, int slot) {
        return ((loc < crits.length) && (slot < crits[loc].length)) ? crits[loc][slot] : null;
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
     * Only Meks have Gyros but this helps keep the code a bit cleaner.
     *
     * @return <code>-1</code>
     */
    public int getGyroType() {
        return -1;
    }

    public static Map<Integer, String> getAllGyroCodeName() {
        Map<Integer, String> result = new HashMap<>();

        result.put(Mek.GYRO_UNKNOWN, Mek.getGyroDisplayString(Mek.GYRO_UNKNOWN));
        result.put(Mek.GYRO_STANDARD, Mek.getGyroDisplayString(Mek.GYRO_STANDARD));
        result.put(Mek.GYRO_XL, Mek.getGyroDisplayString(Mek.GYRO_XL));
        result.put(Mek.GYRO_COMPACT, Mek.getGyroDisplayString(Mek.GYRO_COMPACT));
        result.put(Mek.GYRO_HEAVY_DUTY, Mek.getGyroDisplayString(Mek.GYRO_HEAVY_DUTY));
        result.put(Mek.GYRO_NONE, Mek.getGyroDisplayString(Mek.GYRO_NONE));
        result.put(Mek.GYRO_SUPERHEAVY, Mek.getGyroDisplayString(Mek.GYRO_SUPERHEAVY));

        return result;
    }

    /**
     * Only Meks have gyros, but this helps keep the code a bit cleaner.
     *
     * @return true if the <code>Entity</code> is a Mek and has taken enough gyro
     *         hits to destroy it
     */
    public boolean isGyroDestroyed() {
        return false;
    }

    /**
     * Returns the number of operational critical slots of the specified type in
     * the location
     */
    public int getGoodCriticals(int type, int index, int loc) {
        return critStateCount(type, index, loc, cs -> !cs.isDestroyed() && !cs.isBreached());
    }

    /**
     * The number of critical slots that are destroyed or breached in the
     * location or missing along with it (if it was blown off).
     */
    public int getBadCriticals(int type, int index, int loc) {
        return critStateCount(type, index, loc, cs -> cs.isDestroyed() || cs.isBreached() || cs.isMissing());
    }

    /**
     * Number of slots damaged (but not breached) in a location
     */
    public int getDamagedCriticals(int type, int index, int loc) {
        return critStateCount(type, index, loc, CriticalSlot::isDamaged);
    }

    /**
     * Number of slots doomed, missing or destroyed in a location
     */
    public int getHitCriticals(int type, int index, int loc) {
        return critStateCount(type, index, loc, cs -> cs.isDamaged() || cs.isBreached() || cs.isMissing());
    }

    /**
     * @return the number of critical slots of the equipment given as index for
     *         {@link #getEquipment(int)} in location loc wherein the type is the
     *         critical slot type that fit the slot state given as slotState
     *         Predicate such as {@link CriticalSlot#isDestroyed()}. The crit slots
     *         tested are only those in location loc except for Super-Cooled Myomer
     *         where all locations are considered.
     */
    protected int critStateCount(int type, int index, int loc, Predicate<CriticalSlot> slotState) {
        int stateAppliesCount = 0;
        Mounted<?> m = null;
        if (type == CriticalSlot.TYPE_EQUIPMENT) {
            m = getEquipment(index);
            if (m == null) {
                logger.error("Null Equipment found in equipment list of entity " + this);
                return 0;
            }
            if ((this instanceof Mek) && m.is(EquipmentTypeLookup.SCM)) {
                return scmCritStateCount(slotState);
            }
        }

        int numberOfCriticals = getNumberOfCriticals(loc);
        for (int i = 0; i < numberOfCriticals; i++) {
            CriticalSlot ccs = getCritical(loc, i);

            // Check to see if this crit mounts the supplied item
            // For systems, we can compare the index, but for equipment we
            // need to get the Mounted that is mounted in that index and
            // compare types. Superheavies may have two Mounted in each crit
            if ((ccs != null) && (ccs.getType() == type) && slotState.test(ccs)) {
                if ((type == CriticalSlot.TYPE_SYSTEM) && (ccs.getIndex() == index)) {
                    stateAppliesCount++;
                } else if ((type == CriticalSlot.TYPE_EQUIPMENT)
                        && (m.equals(ccs.getMount()) || m.equals(ccs.getMount2()))) {
                    stateAppliesCount++;
                }
            }
        }
        return stateAppliesCount;
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
     * Returns the number of critical slots present in the Mek, destroyed or
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
                        Mek.ACTUATOR_HIP, i) > 0)
                        || (getBadCriticals(CriticalSlot.TYPE_SYSTEM,
                                Mek.ACTUATOR_UPPER_LEG, i) > 0)
                        || (getBadCriticals(CriticalSlot.TYPE_SYSTEM,
                                Mek.ACTUATOR_LOWER_LEG, i) > 0)
                        || (getBadCriticals(CriticalSlot.TYPE_SYSTEM,
                                Mek.ACTUATOR_FOOT, i) > 0)) {
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
     * @return true if the location has a system of the type, whether is destroyed
     *         or not
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
     * @return True if this unit wields vibroblades
     */
    public boolean hasVibroblades() {
        return false;
    }

    /**
     * @param location The location to check
     * @return The heat generated by active vibroblades in the given location if this unit has any
     */
    public int getActiveVibrobladeHeat(int location) {
        return 0;
    }

    public int getActiveVibrobladeHeat(int location, boolean ignoreMode) {
        return 0;
    }

    /**
     * Does the Mek have any shields. a Mek can have up to 2 shields.
     *
     * @return <code>true</code> if <code>shieldCount</code> is greater than 0
     *         else <code>false</code>
     */
    public boolean hasShield() {
        return false;
    }

    /**
     * Check to see how many shields of a certain size a mek has. you can have
     * up to shields per Mek. However they can be of different size and each
     * size has its own draw backs. So check each size and add modifiers based on
     * the number shields of that size.
     */
    public int getNumberOfShields(long size) {
        return 0;
    }

    /**
     * Does the Mek have an active shield This should only be called after
     * hasShield has been called.
     */
    public boolean hasActiveShield(int location, boolean rear) {
        return true;
    }

    /**
     * Does the Mek have an active shield This should only be called by
     * hasActiveShield(location, rear)
     */
    public boolean hasActiveShield(int location) {
        return false;
    }

    /**
     * Does the Mek have a passive shield This should only be called after
     * hasShield has been called.
     */
    public boolean hasPassiveShield(int location, boolean rear) {
        return false;
    }

    /**
     * Does the Mek have a passive shield This should only be called by
     * hasPassiveShield(location, rear)
     */
    public boolean hasPassiveShield(int location) {
        return false;
    }

    /**
     * Does the Mek have an shield in no defense mode
     */
    public boolean hasNoDefenseShield(int location) {
        return false;
    }

    /**
     * This method checks to see if a unit has Underwater Maneuvering Units
     *
     * @return <code>boolean</code> if the entity has usable UMU crits.
     */
    public boolean hasUMU() {
        return getActiveUMUCount() > 0;
    }

    /**
     * This counts the number of UMU's a Mek has that are still viable
     *
     * @return number <code>int</code>of useable UMU's
     */
    public int getActiveUMUCount() {
        if (hasShield() && (getNumberOfShields(MiscType.S_SHIELD_LARGE) > 0)) {
            return 0;
        }
        int count = 0;
        for (MiscMounted m : getMisc()) {
            if (m.getType().hasFlag(MiscType.F_UMU) && !(m.isDestroyed() || m.isMissing() || m.isBreached())) {
                count++;
            }
        }
        return count;
    }

    /**
     * This returns all UMU a Mek has.
     *
     * @return <code>int</code>Total number of UMUs a Mek has.
     */
    public int getAllUMUCount() {
        if (hasShield() && (getNumberOfShields(MiscType.S_SHIELD_LARGE) > 0)) {
            return 0;
        }
        int count = 0;
        for (MiscMounted m : getMisc()) {
            if (m.getType().hasFlag(MiscType.F_UMU)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Does the Mek have a functioning ECM unit?
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
                && !game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_ECM)) {
            return false;
        }
        if (!isShutDown()) {
            for (MiscMounted m : getMisc()) {
                // EQ equipment does not count for stealth armor
                if (stealth && m.getType().hasFlag(MiscType.F_EW_EQUIPMENT)) {
                    continue;
                }
                // TacOps p. 100 Angle ECM can have 1 ECM and 1 ECCM at the same
                // time
                if (m.getType().hasFlag(MiscType.F_ECM)
                        && (m.curMode().equals("ECM")
                                || m.curMode().equals("ECM & ECCM") || m
                                        .curMode().equals("ECM & Ghost Targets"))) {
                    return !(m.isInoperable());
                }
            }
        }
        return false;
    }

    // MiscType.F_ECM should be part of all ECM systems
    public boolean hasECM() {
        for (Mounted<?> m : getMisc()) {
            EquipmentType type = m.getType();
            if ((type instanceof MiscType) && type.hasFlag(MiscType.F_ECM)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Does the Mek have a functioning ECM unit, tuned to ghost target generation?
     */
    public boolean hasGhostTargets(boolean active) {
        // no Ghost Targets in space unless strat op option enabled
        if (game.getBoard().inSpace()) {
            return false;
        }

        // if you failed your ghost target PSR, then it doesn't matter
        if ((ghostTargetRoll == null) || (active && (getGhostTargetRollMoS() < 0)) || isShutDown()) {
            return false;
        }
        boolean hasGhost = false;
        for (MiscMounted m : getMisc()) {
            MiscType type = m.getType();
            // TacOps p. 100 Angle ECM can have ECM/ECCM and Ghost Targets at
            // the same time
            if (type.hasFlag(MiscType.F_ECM)
                    && (m.curMode().equals("Ghost Targets")
                            || m.curMode().equals("ECM & Ghost Targets") || m
                                    .curMode().equals("ECCM & Ghost Targets"))
                    && !(m.isInoperable() || getCrew().isUnconscious())) {
                hasGhost = true;
            }
            if (type.hasFlag(MiscType.F_COMMUNICATIONS)
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
     *         mode <code>false</code> if the entity does not have angel ecm or
     *         it is not in eccm mode or it is damaged.
     */
    public boolean hasActiveECCM() {
        // no ECM in space unless strat op option enabled
        if (game.getBoard().inSpace()
                && !game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_ECM)) {
            return false;
        }
        if ((game.getOptions().booleanOption(OptionsConstants.ADVANCED_TACOPS_ECCM) || game
                .getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_ECM)) && !isShutDown()) {
            for (MiscMounted m : getMisc()) {
                MiscType type = m.getType();
                // TacOps p. 100 Angle ECM can have 1 ECM and 1 ECCM at the same
                // time
                if (((type.hasFlag(MiscType.F_ECM)
                        && (m.curMode().equals("ECCM") || m.curMode().equals("ECM & ECCM")
                                || m.curMode().equals("ECCM & Ghost Targets")))
                        || (type.hasFlag(MiscType.F_COMMUNICATIONS) && m.curMode().equals("ECCM")))) {
                    return !m.isInoperable();
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
        if (game.getBoard().inSpace()
                && !game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_ECM)) {
            return Entity.NONE;
        }
        // If we have stealth up and running, there's no bubble.
        if (isStealthOn()) {
            return Entity.NONE;
        }

        if (!isShutDown()) {
            for (MiscMounted m : getMisc()) {
                MiscType type = m.getType();
                if (type.hasFlag(MiscType.F_ECM)
                        && !m.isInoperable()) {
                    if (type.hasFlag(MiscType.F_SINGLE_HEX_ECM)) {
                        return 0;
                    }
                    int toReturn = 6;
                    if (type.hasFlag(MiscType.F_ANGEL_ECM)
                            && (this instanceof BattleArmor)) {
                        toReturn = 2;
                    }
                    if (type.hasFlag(MiscType.F_EW_EQUIPMENT)) {
                        toReturn = 3;
                    }
                    PlanetaryConditions conditions = game.getPlanetaryConditions();
                    if (conditions.getEMI().isEMI()) {
                        return toReturn * 2;
                    }
                    return toReturn;
                }
            }
        }
        return Entity.NONE;
    }

    /**
     * Does the Mek have a functioning BAP? This is just for the basic BAP for
     * Beagle BloodHound WatchDog Clan Active or Light.
     */
    public boolean hasBAP() {
        return hasBAP(true);
    }

    public boolean hasBAP(boolean checkECM) {
        if (game != null) {
            PlanetaryConditions conditions = game.getPlanetaryConditions();
            if (conditions.getEMI().isEMI()) {
                return false;
            }
        }
        if (isShutDown()) {
            return false;
        }
        for (MiscMounted m : getMisc()) {
            if (m.getType().hasFlag(MiscType.F_BAP)) {

                if (!m.isInoperable()) {
                    // Beagle Isn't affected by normal ECM
                    if (m.getType().getName().equals("Beagle Active Probe")) {
                        return (game == null)
                                || !checkECM
                                || !ComputeECM.isAffectedByAngelECM(this, getPosition(), getPosition());
                    }
                    return !checkECM
                            || (game == null)
                            || !ComputeECM.isAffectedByECM(this, getPosition(), getPosition());
                }
            }
        }

        // check for Manei Domini implants
        if (((hasAbility(OptionsConstants.MD_CYBER_IMP_AUDIO)
                || hasAbility(OptionsConstants.MD_CYBER_IMP_VISUAL)
                || hasAbility(OptionsConstants.MD_MM_IMPLANTS))
                && isConventionalInfantry())
                || (hasAbility(OptionsConstants.MD_MM_IMPLANTS)
                        && (hasAbility(OptionsConstants.MD_VDNI)
                                || hasAbility(OptionsConstants.MD_BVDNI)))) {
            return !checkECM || !ComputeECM.isAffectedByECM(this, getPosition(), getPosition());
        }
        // check for quirk
        if (hasQuirk(OptionsConstants.QUIRK_POS_IMPROVED_SENSORS)) {
            return !checkECM || !ComputeECM.isAffectedByECM(this, getPosition(), getPosition());
        }
        // check for SPA
        if (hasAbility(OptionsConstants.MISC_EAGLE_EYES)) {
            return !checkECM || !ComputeECM.isAffectedByECM(this, getPosition(), getPosition());
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
        PlanetaryConditions conditions = game.getPlanetaryConditions();
        if (conditions.getEMI().isEMI()
                || isShutDown()) {
            return Entity.NONE;
        }
        // check for Manei Domini implants
        int cyberBonus = 0;
        if (((hasAbility(OptionsConstants.MD_CYBER_IMP_AUDIO)
                || hasAbility(OptionsConstants.MD_MM_IMPLANTS))
                || hasAbility(OptionsConstants.MD_CYBER_IMP_VISUAL)
                        && isConventionalInfantry())
                || (hasAbility(OptionsConstants.MD_MM_IMPLANTS)
                        && (hasAbility(OptionsConstants.MD_VDNI)
                                || hasAbility(OptionsConstants.MD_BVDNI)))) {
            cyberBonus = 2;
        }

        // check for quirks
        int quirkBonus = 0;
        if (hasQuirk(OptionsConstants.QUIRK_POS_IMPROVED_SENSORS)) {
            quirkBonus = isClan() ? 5 : 4;
        }

        // check for SPA
        int spaBonus = 0;
        if (hasAbility(OptionsConstants.MISC_EAGLE_EYES)) {
            spaBonus = 1;
        }

        for (MiscMounted m : getMisc()) {
            MiscType type = m.getType();
            if (type.hasFlag(MiscType.F_BAP) && !m.isInoperable()) {
                // Quirk bonus is only 2 if equipped with BAP
                if (quirkBonus > 0) {
                    quirkBonus = 2;
                }

                // in space the range of all BAPs is given by the mode
                if (game.getBoard().inSpace()) {
                    return m.curMode().equals("Medium") ? 12 : 6;
                }

                if (m.getName().equals("Bloodhound Active Probe (THB)")
                        || m.getName().equals(Sensor.BAP)) {
                    return 8 + cyberBonus + quirkBonus + spaBonus;
                }
                if ((m.getType()).getInternalName().equals(Sensor.CLAN_AP)
                        || (m.getType()).getInternalName().equals(Sensor.WATCHDOG)
                        || (m.getType()).getInternalName().equals(Sensor.NOVA)) {
                    return 5 + cyberBonus + quirkBonus + spaBonus;
                }
                if ((m.getType()).getInternalName().equals(Sensor.LIGHT_AP)
                        || (m.getType().getInternalName().equals(Sensor.CLBALIGHT_AP))
                        || (m.getType().getInternalName().equals(Sensor.ISBALIGHT_AP))) {
                    return 3 + cyberBonus + quirkBonus + spaBonus;
                }
                if (m.getType().getInternalName().equals(Sensor.ISIMPROVED)
                        || (m.getType().getInternalName().equals(Sensor.CLIMPROVED))) {
                    return 2 + cyberBonus + quirkBonus + spaBonus;
                }
                return 4 + cyberBonus + quirkBonus + spaBonus;// everthing else should be
                // range 4
            }
        }
        if ((cyberBonus + quirkBonus + spaBonus) > 0) {
            return cyberBonus + quirkBonus + spaBonus;
        }

        return Entity.NONE;
    }

    /**
     * Returns whether this entity has a Drone Operating System
     */
    public boolean hasDroneOs() {
        return getMisc().stream().anyMatch(
                m -> (m.getType() != null) && m.getType().hasFlag(MiscType.F_DRONE_OPERATING_SYSTEM));
    }

    /**
     * Returns wether or not this entity has a Targeting Computer.
     */
    public boolean hasTargComp() {
        for (MiscMounted m : getMisc()) {
            if (m.getType().hasFlag(MiscType.F_TARGCOMP)) {
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
            if (this instanceof Mek) {
                if (((Mek) this).getCockpitStatus() == Mek.COCKPIT_AIMED_SHOT) {
                    return true;
                }
            } else {
                return true;
            }
        }
        for (MiscMounted m : getMisc()) {
            if (m.getType().hasFlag(MiscType.F_TARGCOMP)
                    && m.curMode().equals("Aimed shot")) {
                return !m.isInoperable();
            }
        }
        return false;
    }

    /**
     * Returns whether this 'Mek has a C3 Slave or not.
     */
    public boolean hasC3S() {
        if (isShutDown() || isOffBoard()) {
            return false;
        }
        for (MiscMounted m : getMisc()) {
            if ((m.getType().hasFlag(MiscType.F_C3S) || m.getType().hasFlag(MiscType.F_C3SBS)) && !m.isInoperable()) {
                return true;
            }
        }
        return false;
    }

    /**
     * True if the unit has CASE II (on any location in the case of Meks).
     * Only Meks and Fighters can have CASE II so all other entities should return
     * false.
     *
     * @return True if the unit has CASE II
     */
    public boolean hasCASEII() {
        return getMisc().stream().anyMatch(m -> m.getType().hasFlag(MiscType.F_CASEII));
    }

    /**
     * True if the unit has CASE II in the given location.
     * Only Meks and Fighters can have CASE II so all other entities should return
     * false.
     *
     * @param location The location to check
     * @return True if the unit has CASE II in the given location
     */
    public boolean hasCASEII(int location) {
        return getMisc().stream()
                .filter(m -> m.getLocation() == location)
                .anyMatch(m -> m.getType().hasFlag(MiscType.F_CASEII));
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
        for (MiscMounted mounted : getMisc()) {
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
        for (Mounted<?> m : getEquipment()) {
            if (((m.getType() instanceof MiscType) && m.getType().hasFlag(MiscType.F_C3SBS))
                    || ((m.getType() instanceof WeaponType) && m.getType().hasFlag(WeaponType.F_C3MBS))
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
        for (WeaponMounted m : getWeaponList()) {
            if ((m.getType().hasFlag(WeaponType.F_C3M) || m.getType().hasFlag(WeaponType.F_C3MBS))
                    && !m.isInoperable()) {
                // If this unit is configured as a company commander,
                // and if this computer is the company master, then
                // this unit does not have a lance master computer.
                return !C3MasterIs(this) || (c3CompanyMasterIndex != getEquipmentNum(m));
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
            Iterator<Mounted<?>> e = getEquipment().iterator();
            while ((c3CompanyMasterIndex == LOC_DESTROYED) && e.hasNext()) {
                Mounted<?> m = e.next();
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

        Mounted<?> m = getEquipment(c3CompanyMasterIndex);
        return !m.isDestroyed() && !m.isBreached();
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
     * @return True if this unit is not offboard nor shutdown and has a Nova CEWS that is not offline.
     */
    public boolean hasActiveNovaCEWS() {
        if (isShutDown() || isOffBoard()) {
            return false;
        } else {
            return getMisc().stream().filter(Mounted::isOperable)
                    .filter(m -> !m.curMode().equals("Off"))
                    .anyMatch(m -> m.getType().hasFlag(MiscType.F_NOVA));
        }
    }

    /**
     * @return True if this unit is not offboard nor shutdown and has a Nova CEWS that is not offline.
     */
    public boolean hasNovaCEWS() {
        return getMisc().stream().filter(Mounted::isOperable)
                .anyMatch(m -> m.getType().hasFlag(MiscType.F_NOVA));
    }

    public boolean hasNavalC3() {
        if (isShutDown() || isOffBoard()) {
            return false;
        }
        for (MiscMounted m : getMisc()) {
            if (m.getType().hasFlag(MiscType.F_NAVAL_C3) && !m.isInoperable()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasC3i() {
        if (isShutDown() || isOffBoard()) {
            return false;
        }
        for (MiscMounted m : getMisc()) {
            if (m.getType().hasFlag(MiscType.F_C3I) && !m.isInoperable()) {
                return true;
            }
        }
        // check for Manei Domini implants
        if ((this instanceof Infantry) && (null != crew)
        // Fix for Bug Report #1194
                && hasAbility(OptionsConstants.MD_ENH_MM_IMPLANTS)
                && hasAbility(OptionsConstants.MD_BOOST_COMM_IMPLANT)) {
            return true;
        }
        return false;
    }

    /**
     * Returns true if the unit has a nonhierarchic C3 system (C3i, NC3 or Nova
     * CEWS).
     */
    public boolean hasNhC3() {
        return hasC3i() || hasNavalC3() || hasNovaCEWS();
    }

    /**
     * Returns true if the unit has a standard C3M/S, a Naval C3 or C3i or a Nova
     * CEWS.
     */
    public boolean hasAnyC3System() {
        return hasC3() || hasNhC3();
    }

    public String getC3NetId() {
        if (c3NetIdString == null) {
            if (hasC3()) {
                c3NetIdString = "C3." + getId();
            } else if (hasC3i()) {
                c3NetIdString = "C3i." + getId();
            } else if (hasActiveNovaCEWS()) {
                c3NetIdString = "C3Nova." + getId();
            } else if (hasNavalC3()) {
                c3NetIdString = "NC3." + getId();
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
        if ((newC3NetIdString == null) || newC3NetIdString.isEmpty()) {
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
        } else if (hasNavalC3()) {
            c3NetIdString = "NC3." + getId();
        } else {
            c3NetIdString = "C3i." + getId();
        }
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
     * <p>
     * Please note, if this <code>Entity</code> has two C3 Master computers,
     * then this function only returns the remaining number of <b>C3 Slave</b>
     * computers that can connect.
     *
     * @return a non-negative <code>int</code> value.
     */
    public int calculateFreeC3Nodes() {
        int nodes = 0;
        if (hasC3i() || hasNavalC3()) {
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
     *         itself, if none is above this
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
     * @param e - the <code>Entity</code> that may be this unit's C3 Master.
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
     * Set another <code>Entity</code> as C3 Master. Pass (null, true) to disconnect
     * the entity from a C3 network that isn't its own (C3 Net id with its own id).
     *
     * @param e - the <code>Entity</code> that should be set as our C3 Master.
     */
    public void setC3Master(Entity e, boolean reset) {
        if (e == null) {
            setC3Master(NONE, reset);
        } else if (!isEnemyOf(e)) {
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
                    e.setC3Master(NONE, true);
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
        } else if (hasNavalC3() && (entityId == NONE)) {
            c3NetIdString = "NC3." + id;
        } else if (hasC3() || hasC3i() || hasNavalC3()) {
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
     * @param ignoreECM a <code>boolean</code> indicating if ECM should be ignored,
     *                  we
     *                  need this for c3i
     * @return a <code>boolean</code> that is <code>true</code> if the given
     *         entity is on the same network, <code>false</code> if not.
     */
    public boolean onSameC3NetworkAs(Entity e, boolean ignoreECM) {
        if (isEnemyOf(e) || isShutDown() || e.isShutDown()) {
            return false;
        }

        // Active Mek Stealth prevents entity from participating in C3.
        // Turn off the stealth, and your back in the network.
        if (((this instanceof Mek) || (this instanceof Tank)) && isStealthActive()) {
            return false;
        }
        if (((e instanceof Mek) || (e instanceof Tank)) && e.isStealthActive()) {
            return false;
        }

        // C3i is easy - if they both have C3i, and their net ID's match,
        // they're on the same network!
        if (hasC3i() && e.hasC3i() && getC3NetId().equals(e.getC3NetId())) {
            if (ignoreECM) {
                return true;
            }
            return !(ComputeECM.isAffectedByECM(e, e.getPosition(), e.getPosition()))
                    && !(ComputeECM.isAffectedByECM(this, getPosition(), getPosition()));
        }

        // NC3 is easy too - if they both have NC3, and their net ID's match,
        // they're on the same network!
        if (hasNavalC3() && e.hasNavalC3() && getC3NetId().equals(e.getC3NetId())) {
            int distance = Compute.effectiveDistance(game, this, e, false);
            // Naval C3 is not affected by ECM, but nodes must be within 60 hexes of one
            // another
            if (game.getRoundCount() > 0) {
                if (distance > 60) {
                    return false;
                }
                // Naval C3 only works in space
                if (!isSpaceborne()) {
                    return false;
                }
            }
            return true;
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
        for (MiscMounted mounted : getMisc()) {
            if ((mounted.getLocation() == loc)
                    && mounted.getType().hasFlag(MiscType.F_CASE) | (mounted.getType().hasFlag(MiscType.F_CASEP))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether there is CASE anywhere on this {@code Entity}.
     */
    public boolean hasCase() {
        // Clan Meks always have CASE!
        if (isClan()) {
            return true;
        }
        for (MiscMounted mounted : getMisc()) {
            if (mounted.getType().hasFlag(MiscType.F_CASE) || (mounted.getType().hasFlag(MiscType.F_CASEP))) {
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
                Mounted<?> csMount = cs.getMount();
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
    @Override
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
        convertingNow = false;
        damageThisRound = 0;
        if (assaultDropInProgress == 2) {
            assaultDropInProgress = 0;
        }
        movedLastRound = moved;
        moved = EntityMovementType.MOVE_NONE;
        movedBackwards = false;
        isPowerReverse = false;
        wigeLiftoffHover = false;
        gotPavementOrRoadBonus = false;
        wigeBonus = 0;
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

        if (!pendingINarcPods.isEmpty()) {
            iNarcPods.addAll(pendingINarcPods);
            pendingINarcPods.clear();
        }

        if (!pendingNarcPods.isEmpty()) {
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
                && !isAero()
                && (getAltitude() <= game.getPlanetaryConditions()
                        .getDropRate())) {
            setAssaultDropInProgress(true);
        }

        for (Mounted<?> m : getEquipment()) {
            m.newRound(roundNumber);
        }

        newRoundNovaNetSwitch();
        doNewRoundIMP();

        // reset hexes passed through
        setPassedThrough(new Vector<>());
        setPassedThroughFacing(new ArrayList<>());
        if (playerPickedPassThrough == null) {
            playerPickedPassThrough = new HashMap<>();
        } else {
            playerPickedPassThrough.clear();
        }

        resetFiringArcs();

        setAlreadyTwisted(false);

        resetBays();

        if (isBomber()) {
            resetBombAttacks();
        }

        // Reset deployed or incoming weapons which need TAG guidance (mainly for bot
        // computations)
        if (incomingGuidedAttacks == null) {
            incomingGuidedAttacks = new ArrayList<>();
        } else {
            incomingGuidedAttacks.clear();
        }

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
        ghostTargetRoll = Compute.rollD6(2);
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
        if (getTsempEffect() == MMConstants.TSEMP_EFFECT_SHUTDOWN) {
            setTsempEffect(MMConstants.TSEMP_EFFECT_NONE);
            // The TSEMP interference effect shouldn't be removed until the start
            // of a round where we didn't have any TSEMP hits and didn't fire a
            // TSEMP, since we need the effect active during the firing phase
        } else if ((getTsempHitsThisTurn() == 0) && !isFiredTsempThisTurn()) {
            setTsempEffect(MMConstants.TSEMP_EFFECT_NONE);
        }

        // Standard TSEMPs can fire every other round, so if we didn't fire last
        // round and the TSEMP isn't one-shot or repeating, reset it's fired state
        if (hasFiredTsemp()) {
            for (WeaponMounted m : getWeaponList()) {
                if (m.getType().hasFlag(WeaponType.F_TSEMP)
                        && !m.getType().hasFlag(WeaponType.F_ONESHOT)) {
                    if (m.getType().hasFlag(WeaponType.F_REPEATING) || m.isTSEMPDowntime()) {
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

        setMadePointblankShot(false);

        setSelfDestructedThisTurn(false);

        setClimbMode(GUIP.getMoveDefaultClimbMode());

        endOfTurnCargoInteraction = false;

        setTurnInterrupted(false);
    }

    /**
     * Applies any damage that the entity has suffered. When anything gets hit
     * it is simply marked as "hit" but does not stop working until this is
     * called.
     */
    public void applyDamage() {
        // mark all damaged equipment destroyed
        for (Mounted<?> mounted : getEquipment()) {
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
        for (WeaponMounted mounted : getTotalWeaponList()) {
            WeaponType wtype = mounted.getType();

            if (wtype.getAmmoType() != AmmoType.T_NA) {
                if ((mounted.getLinked() == null) || (mounted.getLinked().getUsableShotsLeft() <= 0)
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
    public List<WeaponMounted> getActiveAMS() {
        List<WeaponMounted> ams = new ArrayList<>();
        for (WeaponMounted weapon : getWeaponList()) {
            // Skip anything that's not AMS
            if (!weapon.getType().hasFlag(WeaponType.F_AMS)) {
                continue;
            }

            // Make sure the AMS is good to go
            if (!weapon.isReady() || weapon.isMissing()
                    || !weapon.curMode().equals(Weapon.MODE_AMS_ON)) {
                continue;
            }

            // AMS blocked by transported units can not fire
            if (isWeaponBlockedAt(weapon.getLocation(), weapon.isRearMounted())) {
                continue;
            }

            // Make sure ammo is loaded
            boolean baAPDS = (this instanceof BattleArmor)
                    && (weapon.getType().getInternalName().equals("ISBAAPDS"));
            AmmoMounted ammo = weapon.getLinkedAmmo();
            if (!(weapon.getType().hasFlag(WeaponType.F_ENERGY)) && !baAPDS
                    && ((ammo == null) || (ammo.getUsableShotsLeft() == 0) || ammo.isDumping())) {
                loadWeapon(weapon);
                ammo = weapon.getLinkedAmmo();
            }

            // try again
            if (!(weapon.getType().hasFlag(WeaponType.F_ENERGY)) && !baAPDS
                    && ((ammo == null) || (ammo.getUsableShotsLeft() == 0) || ammo.isDumping())) {
                // No ammo for this AMS.
                continue;
            }
            ams.add(weapon);
        }
        return ams;
    }

    /**
     * Assign AMS systems to an incoming telemissile attack. This allows AMS bays to
     * work against
     * these modified physical attacks.
     */
    public void assignTMAMS(final TeleMissileAttackAction telemissileAttack) {
        // AMS Bays can fire at all incoming attacks each round
        // Point defense bays are added too, provided they haven't fired at something
        // else already.
        getActiveAMS().stream()
                .filter(ams -> ams.getType().hasFlag(WeaponType.F_AMSBAY)
                        || (ams.getType().hasFlag(WeaponType.F_PDBAY) && !ams.isUsedThisRound()))
                .filter(ams -> Compute.isInArc(game, getId(), getEquipmentNum(ams),
                        game.getEntity(telemissileAttack.getEntityId())))
                .forEach(telemissileAttack::addCounterEquipment);
    }

    /**
     * Assign AMS systems to the most dangerous incoming missile attacks. This
     * should only be called once per turn, or AMS will get extra attacks
     */
    public void assignAMS(final List<WeaponHandler> attacks) {
        final Set<WeaponAttackAction> targets = new HashSet<>();
        getActiveAMS().stream()
                .filter(ams -> !ams.isAPDS())
                .forEach(ams -> {
                    // make a new list of only incoming attacks in arc
                    final List<WeaponAttackAction> attacksInArc = attacks.stream()
                            .filter(weaponHandler -> (weaponHandler.getWaa() != null)
                                    && !targets.contains(weaponHandler.getWaa())
                                    && Compute.isInArc(getGame(), getId(), getEquipmentNum(ams),
                                            (weaponHandler instanceof CapitalMissileBearingsOnlyHandler)
                                                    ? getGame().getTarget(
                                                            weaponHandler.getWaa().getOriginalTargetType(),
                                                            weaponHandler.getWaa().getOriginalTargetId())
                                                    : getGame().getEntity(weaponHandler.getWaa().getEntityId())))
                            .map(WeaponHandler::getWaa)
                            .collect(Collectors.toList());

                    if (attacksInArc.isEmpty()) {
                        return;
                    }

                    // AMS Bays can fire at all incoming attacks each round
                    // So can standard AMS if the unofficial option is turned on
                    if ((ams.getType().hasFlag(WeaponType.F_AMSBAY))
                            || (getGame().getOptions().booleanOption(OptionsConstants.ADVCOMBAT_MULTI_USE_AMS)
                                    && ams.getType().hasFlag(WeaponType.F_AMS))) {
                        attacksInArc.forEach(waa -> waa.addCounterEquipment(ams));
                    } else if (ams.getType().hasFlag(WeaponType.F_PDBAY)) {
                        // Point defense bays are assigned to the attack with the greatest threat
                        // Unlike single AMS, PD bays can gang up on 1 attack
                        Compute.getHighestExpectedDamage(getGame(), attacksInArc, true).addCounterEquipment(ams);
                    } else {
                        // Otherwise, find the most dangerous salvo by expected damage and target it
                        // this ensures that only 1 AMS targets the strike. Use for non-bays.
                        final WeaponAttackAction waa = Compute.getHighestExpectedDamage(getGame(), attacksInArc, true);
                        waa.addCounterEquipment(ams);
                        targets.add(waa);
                    }
                });
    }

    /**
     * has the team attached a narc pod to me?
     */
    public boolean isNarcedBy(int nTeamID) {
        return narcPods.stream().anyMatch(pod -> pod.getTeam() == nTeamID);
    }

    /**
     * add a narc pod from this team to the Mek. Unremovable
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
        return iNarcPods.stream().anyMatch(pod -> (pod.getTeam() == nTeamID) && (pod.getType() == INarcPod.HOMING));
    }

    /**
     * Have we been iNarced with the named pod from any team?
     *
     * @param type the <code>int</code> type of iNarc pod.
     * @return <code>true</code> if we have.
     */
    public boolean isINarcedWith(long type) {
        return iNarcPods.stream().anyMatch(pod -> pod.getType() == type);
    }

    /**
     * Remove all attached iNarc Pods
     */
    public void removeAllINarcPods() {
        iNarcPods.clear();
    }

    /** Returns true if any iNarc pods are attached to this unit. */
    public boolean hasINarcPodsAttached() {
        return !iNarcPods.isEmpty();
    }

    /** Returns true if any Narc pods are attached to this unit. (Ignores iNarc) */
    public boolean hasNarcPodsAttached() {
        return !narcPods.isEmpty();
    }

    /** Returns true if any Narc or iNarc pods are attached to this unit. */
    public boolean hasAnyTypeNarcPodsAttached() {
        return hasINarcPodsAttached() || hasNarcPodsAttached();
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
     *         the pod was not attached to this entity.
     */
    public boolean removeINarcPod(INarcPod pod) {
        return iNarcPods.remove(pod);
    }

    /**
     * Calculates the Battle Value of this unit. Both C3 and crew skill based
     * changes
     * to the BV are taken into account. Note that when a unit has a manual BV value
     * set in its definition file,
     * this manual BV value is returned instead of a calculated BV value.
     *
     * @return The full Battle Value of this unit including C3 and crew skill
     *         modifications or the manual BV in
     *         case this unit uses a manual BV value
     */
    public final int calculateBattleValue() {
        return manualOrCalculateBV(false, false, new DummyCalculationReport());
    }

    /**
     * Calculates the Battle Value of this unit. The parameters can be used to
     * control C3 / skill-based changes
     * to the BV. When both are true, the "base" BV of the unit is calculated. Note
     * that when a unit has a manual BV
     * value set in its definition file, this manual BV value is returned instead of
     * a calculated BV value.
     *
     * @param ignoreC3    When true, the BV contributions of any C3 computers are
     *                    not added
     * @param ignoreSkill When true, the skill of the crew / pilot is not taken into
     *                    account for BV
     * @return The Battle Value of this unit
     */
    public final int calculateBattleValue(boolean ignoreC3, boolean ignoreSkill) {
        return manualOrCalculateBV(ignoreC3, ignoreSkill, new DummyCalculationReport());
    }

    /**
     * Calculates the Battle Value of this unit. Both C3 and crew skill based
     * changes
     * to the BV are taken into account. Note that when a unit has a manual BV value
     * set in its definition file,
     * this manual BV value is returned instead of a calculated BV value and no
     * calculation report info will be
     * generated.
     *
     * @param calculationReport A CalculationReport to write the BV calculation to
     * @return The full Battle Value of this unit including C3 and crew skill
     *         modifications or the manual BV in
     *         case this unit uses a manual BV value
     */
    public int calculateBattleValue(CalculationReport calculationReport) {
        return manualOrCalculateBV(false, false, calculationReport);
    }

    /**
     * Calculates the Battle Value of this unit. The parameters can be used to
     * control C3 / skill-based changes
     * to the BV. When both are true, the "base" BV of the unit is calculated. Note
     * that when a unit has a manual BV
     * value set in its definition file, this manual BV value is returned instead of
     * a calculated BV value and
     * no calculation report info will be generated.
     *
     * @param ignoreC3          When true, the BV contributions of any C3 computers
     *                          are not added
     * @param ignoreSkill       When true, the skill of the crew / pilot is not
     *                          taken into account for BV
     * @param calculationReport A CalculationReport to write the BV calculation to
     * @return The Battle Value of this unit
     */
    public int calculateBattleValue(boolean ignoreC3, boolean ignoreSkill, CalculationReport calculationReport) {
        return manualOrCalculateBV(ignoreC3, ignoreSkill, calculationReport);
    }

    /**
     * Checks if this unit uses a manual BV and if so, returns it. Otherwise,
     * forwards to the actual
     * BV calculation method.
     *
     * @param ignoreC3          When true, the BV contributions of any C3 computers
     *                          are not added
     * @param ignoreSkill       When true, the skill of the crew / pilot is not
     *                          taken into account for BV
     * @param calculationReport A CalculationReport to write the BV calculation to
     * @return The Battle Value of this unit
     */
    private int manualOrCalculateBV(boolean ignoreC3, boolean ignoreSkill, CalculationReport calculationReport) {
        return useManualBV ? manualBV : doBattleValueCalculation(ignoreC3, ignoreSkill, calculationReport);
    }

    /**
     * Calculates and returns the Battle Value of this unit taking into account the
     * parameters.
     * This method should be overridden by subclasses of Entity to provide a unit
     * type specific calculation of the
     * Battle Value. A report of the calculation should be written to the given
     * calculationReport.
     *
     * @param ignoreC3          When true, the BV contributions of any C3 computers
     *                          are not added
     * @param ignoreSkill       When true, the skill of the crew / pilot is not
     *                          taken into account for BV
     * @param calculationReport A CalculationReport to write the BV calculation to
     * @return The Battle Value of this unit calculated from its current state
     */
    protected int doBattleValueCalculation(boolean ignoreC3, boolean ignoreSkill, CalculationReport calculationReport) {
        return getBvCalculator().calculateBV(ignoreC3, ignoreSkill, calculationReport);
    }

    /**
     * Calculates a "generic" Battle Value that is based on the average of all units
     * of this type and tonnage. The
     * purpose of this generic Battle Value is to allow a comparison of this unit's
     * actual BV to that for units of
     * its class. This can be used to balance forces without respect to unit or
     * pilot quality.
     *
     * The generic BV values are calculated by a statistical elasticity model based
     * on all data from the MegaMek
     * database.
     *
     * @return The generic Battle value for this unit based on its tonnage and type
     */
    public abstract int getGenericBattleValue();

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
        if (this == obj) {
            return true;
        }
        if ((null == obj) || (getClass() != obj.getClass())) {
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
        return switch (getMovementMode()) {
            case NONE -> "None";
            case BIPED, BIPED_SWIM -> "Biped";
            case TRIPOD -> "Tripod";
            case QUAD, QUAD_SWIM -> "Quad";
            case TRACKED -> "Tracked";
            case WHEELED -> "Wheeled";
            case HOVER -> "Hover";
            case VTOL -> "VTOL";
            case NAVAL -> "Naval";
            case HYDROFOIL -> "Hydrofoil";
            case SUBMARINE -> "Submarine";
            case INF_UMU -> "UMU";
            case INF_LEG -> "Leg";
            case INF_MOTORIZED -> "Motorized";
            case INF_JUMP -> "Jump";
            case WIGE -> "WiGE";
            case AERODYNE -> "Aerodyne";
            case SPHEROID -> "Spheroid";
            case RAIL -> "Rail";
            case MAGLEV -> "MagLev";
            case STATION_KEEPING -> "Station-Keeping";
            default -> "ERROR";
        };
    }

    /**
     * Set the movement type of the entity
     */
    public void setMovementMode(EntityMovementMode movementMode) {
        this.movementMode = movementMode;
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
        if (getCrew().isDead() || getCrew().isDoomed() || (getCrew().getHits() >= 6)) {
            // Following line switched from impossible to automatic failure
            // -- bug fix for dead units taking PSRs
            return new PilotingRollData(entityId, TargetRoll.AUTOMATIC_FAIL, "Pilot dead");
        }
        // pilot awake?
        else if (!getCrew().isActive()) {
            return new PilotingRollData(entityId, TargetRoll.IMPOSSIBLE, "Pilot unconscious");
        }
        // gyro operational? does not apply if using tracked/quadvee vehicle/lam fighter
        // movement
        if (isGyroDestroyed() && canFall()
                && moveType != EntityMovementType.MOVE_VTOL_WALK
                && moveType != EntityMovementType.MOVE_VTOL_RUN) {
            return new PilotingRollData(entityId, TargetRoll.AUTOMATIC_FAIL, getCrew().getPiloting() + 6, "Gyro destroyed");
        }

        // both legs present?
        if ((this instanceof BipedMek)
                && (((BipedMek) this).countBadLegs() == 2)
                && (moveType != EntityMovementType.MOVE_VTOL_WALK)
                && (moveType != EntityMovementType.MOVE_VTOL_RUN)) {
            return new PilotingRollData(entityId, TargetRoll.AUTOMATIC_FAIL,
                getCrew().getPiloting() + 10, "Both legs destroyed");
        } else if (this instanceof QuadMek) {
            if (((QuadMek) this).countBadLegs() >= 3) {
                return new PilotingRollData(entityId,
                        TargetRoll.AUTOMATIC_FAIL, getCrew().getPiloting() + (((Mek) this).countBadLegs() * 5),
                        ((Mek) this).countBadLegs() + " legs destroyed");
            }
        }
        // entity shut down?
        if (isShutDown() && isShutDownThisPhase()) {
            return new PilotingRollData(entityId, TargetRoll.AUTOMATIC_FAIL,
                    getCrew().getPiloting() + 3, "Reactor shut down");
        } else if (isShutDown()) {
            return new PilotingRollData(entityId, TargetRoll.AUTOMATIC_FAIL, TargetRoll.IMPOSSIBLE, "Reactor shut down");
        }

        // okay, let's figure out the stuff then
        roll = new PilotingRollData(entityId, getCrew().getPiloting(moveType), "Base piloting skill");

        // Let's see if we have a modifier to our piloting skill roll. We'll pass in the roll object and adjust as necessary
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
            if (getPartialRepairs().booleanOption("mek_gyro_1_crit")) {
                roll.addModifier(+1, "Partial repair of Gyro (+1)");
            }
            if (getPartialRepairs().booleanOption("mek_gyro_2_crit")) {
                roll.addModifier(+1, "Partial repair of Gyro (+2)");
            }
        }

        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_TACOPS_FATIGUE) && crew.isPilotingFatigued()) {
            roll.addModifier(1, "fatigue");
        }

        if (taserInterference > 0) {
            roll.addModifier(taserInterference, "taser interference");
        }

        if (getGame().getPhase().isMovement() && isPowerReverse()) {
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
        PlanetaryConditions conditions = game.getPlanetaryConditions();

        if (moveType == EntityMovementType.MOVE_SPRINT
                || moveType == EntityMovementType.MOVE_VTOL_SPRINT) {
            roll.addModifier(2, "Sprinting");
        }
        // check weather conditions for all entities
        int weatherMod = conditions.getWeatherPilotPenalty();
        boolean hasAllWeather = (null == crew) || !hasAbility(OptionsConstants.UNOFF_ALLWEATHER);
        if ((weatherMod != 0)
                && !game.getBoard().inSpace()
                && hasAllWeather) {
            roll.addModifier(weatherMod, conditions.getWeather().toString());
        }

        // check wind conditions for all entities
        int windMod = conditions.getWindPilotPenalty(this);
        if ((windMod != 0)
                && !game.getBoard().inSpace()
                && hasAllWeather) {
            roll.addModifier(windMod, conditions.getWind().toString());
        }

        if (!hasAbility(OptionsConstants.UNOFF_ALLWEATHER)
                && getCrew().getOptions().stringOption(OptionsConstants.MISC_ENV_SPECIALIST).equals(Crew.ENVSPC_RAIN)) {
            if (conditions.getWeather().isGustingRain()) {
                if ((this instanceof Mek)
                        || isAirborne()
                        || getMovementMode().isTrackedOrWheeled()) {
                    roll.addModifier(-1, Messages.getString("PilotingSPA.EnvSpec.RainSpec"));
                }

                if (isAirborneVTOLorWIGE() || getMovementMode().isHover()) {
                    roll.addModifier(-2, Messages.getString("PilotingSPA.EnvSpec.RainSpec"));
                }
            }

            if (conditions.getWeather().isHeavyRainOrDownpour()) {
                roll.addModifier(-1, Messages.getString("PilotingSPA.EnvSpec.RainSpec"));
            }
        }

        if (!hasAbility(OptionsConstants.UNOFF_ALLWEATHER)
                && getCrew().getOptions().stringOption(OptionsConstants.MISC_ENV_SPECIALIST).equals(Crew.ENVSPC_SNOW)) {
            if (conditions.getWeather().isHeavySnow()) {
                roll.addModifier(-1, Messages.getString("PilotingSPA.EnvSpec.SnowSpec"));
            }

            boolean airborne = isAirborneVTOLorWIGE()
                    || isAirborne();
            if (conditions.getWeather().isSnowFlurriesOrSleetOrIceStorm()
                    && airborne) {
                roll.addModifier(-1, Messages.getString("PilotingSPA.EnvSpec.SnowSpec"));
            }
        }

        if (!hasAbility(OptionsConstants.UNOFF_ALLWEATHER)
                && conditions.getWeather().isClear()
                && getCrew().getOptions().stringOption(OptionsConstants.MISC_ENV_SPECIALIST).equals(Crew.ENVSPC_WIND)) {
            if ((conditions.getWind().isModerateGale()) && isAirborneVTOLorWIGE()) {
                roll.addModifier(-1, Messages.getString("PilotingSPA.EnvSpec.WindSpec"));
            }

            if (conditions.getWind().isStrongGale()) {
                if ((this instanceof Mek)
                        || isAirborne()
                        || isAirborneVTOLorWIGE()
                        || getMovementMode().isHover()) {
                    roll.addModifier(-1, Messages.getString("PilotingSPA.EnvSpec.WindSpec"));
                }
            }

            if (conditions.getWind().isStorm()) {
                if ((this instanceof Mek)
                        || isAirborneVTOLorWIGE()
                        || getMovementMode().isHover()) {
                    roll.addModifier(-2, Messages.getString("PilotingSPA.EnvSpec.WindSpec"));
                }

                if (isAirborne()) {
                    roll.addModifier(-1, Messages.getString("PilotingSPA.EnvSpec.WindSpec"));
                }
            }

            if (conditions.getWind().isTornadoF1ToF3()) {
                roll.addModifier(-2, Messages.getString("PilotingSPA.EnvSpec.WindSpec"));
            }

            if (conditions.getWind().isTornadoF4()) {
                roll.addModifier(-3, Messages.getString("PilotingSPA.EnvSpec.WindSpec"));
            }
        }

        return roll;
    }
    /**
     * Checks if the entity is getting up. If so, returns the target roll for
     * the piloting skill check.
     */
    public PilotingRollData checkGetUp(MoveStep step, EntityMovementType moveType) {
        PilotingRollData roll = getBasePilotingRoll(moveType);
        return checkGetUp(roll, step);
    }
    /**
     * Checks if the entity is getting up. If so, returns the target roll for
     * the piloting skill check.
     */
    public PilotingRollData checkGetUp(PilotingRollData basePilotRoll, MoveStep step) {
        if ((step == null)
                || ((step.getType() != MoveStepType.GET_UP)
                        && (step.getType() != MoveStepType.CAREFUL_STAND))) {
            return new PilotingRollData(id, TargetRoll.CHECK_FALSE,
                    "Check false: Entity is not attempting to get up.");
        }

        PilotingRollData roll = basePilotRoll.copy();

        if (this instanceof BipedMek) {
            if ((((Mek) this).countBadLegs() >= 1)
                    && (isLocationBad(Mek.LOC_LARM)
                            && isLocationBad(Mek.LOC_RARM))) {
                roll.addModifier(TargetRoll.IMPOSSIBLE,
                        "can't get up with destroyed leg and arms");
                return roll;
            }
        }

        if (isHullDown() && (this instanceof QuadMek)) {
            roll.addModifier(TargetRoll.AUTOMATIC_SUCCESS,
                    "getting up from hull down");
            return roll;
        }

        if (!needsRollToStand() && !isGyroDestroyed()) {
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
    public PilotingRollData checkRunningWithDamage(EntityMovementType overallMoveType) {
        PilotingRollData roll = getBasePilotingRoll(overallMoveType);
        return checkRunningWithDamage(roll, overallMoveType);
    }

    /**
     * Checks if the entity is attempting to run with damage that would force a
     * PSR. If so, returns the target roll for the piloting skill check.
     */
    public PilotingRollData checkRunningWithDamage(PilotingRollData basePilotingRoll, EntityMovementType overallMoveType) {
        PilotingRollData roll = basePilotingRoll.copy();

        int gyroDamage = getBadCriticals(CriticalSlot.TYPE_SYSTEM,
                Mek.SYSTEM_GYRO, Mek.LOC_CT);
        if (getGyroType() == Mek.GYRO_HEAVY_DUTY) {
            gyroDamage--; // HD gyro ignores 1st damage
        }
        if (((overallMoveType == EntityMovementType.MOVE_RUN)
                || (overallMoveType == EntityMovementType.MOVE_SPRINT))
                && canFall() && ((gyroDamage > 0) || hasHipCrit())) {
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
     * Checks if the entity is attempting to sprint with MASC or Supercharger
     * engaged (but not both). If so,
     * returns the target roll for the piloting skill check.
     */
    public PilotingRollData checkSprintingWithMASCXorSupercharger(
        EntityMovementType overallMoveType, int used) {
            PilotingRollData roll = getBasePilotingRoll(overallMoveType);
        return checkSprintingWithMASCXorSupercharger(roll, overallMoveType, used);
    }
    /**
     * Checks if the entity is attempting to sprint with MASC or Supercharger
     * engaged (but not both). If so,
     * returns the target roll for the piloting skill check.
     */
    public PilotingRollData checkSprintingWithMASCXorSupercharger(
        PilotingRollData basePilotingRoll, EntityMovementType overallMoveType, int used) {
        PilotingRollData roll = basePilotingRoll.copy();

        if ((overallMoveType == EntityMovementType.MOVE_SPRINT
                || overallMoveType == EntityMovementType.MOVE_VTOL_SPRINT)
                && (used > ((int) Math.ceil(2.0 * getWalkMP())))) {
            roll.append(new PilotingRollData(getId(), 0,
                    "sprinting with active MASC or Supercharger"));
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE,
                    "Check false: Entity is not attempting to sprint with MASC or Supercharger");
        }

        addPilotingModifierForTerrain(roll);
        return roll;
    }

    /**
     * Checks if the entity is attempting to sprint with MASC and supercharger
     * engaged.
     * If so, returns the target roll for the piloting skill check.
     */
    public PilotingRollData checkSprintingWithMASCAndSupercharger(
        EntityMovementType overallMoveType, int used) {
        PilotingRollData roll = getBasePilotingRoll(overallMoveType);
        return checkSprintingWithMASCXorSupercharger(roll, overallMoveType, used);
    }

    /**
     * Checks if the entity is attempting to sprint with MASC and supercharger
     * engaged.
     * If so, returns the target roll for the piloting skill check.
     */
    public PilotingRollData checkSprintingWithMASCAndSupercharger(
        PilotingRollData basePilotingRoll, EntityMovementType overallMoveType, int used) {
        PilotingRollData roll = basePilotingRoll.copy();

        if ((overallMoveType == EntityMovementType.MOVE_SPRINT
                || overallMoveType == EntityMovementType.MOVE_VTOL_SPRINT)
                && (used > ((int) Math.ceil(2.5 * getWalkMP())))) {
            roll.append(new PilotingRollData(getId(), 0,
                    "sprinting with active MASC and Supercharger"));
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE,
                    "Check false: Entity is not attempting to sprint with MASC and Supercharger");
        }

        addPilotingModifierForTerrain(roll);
        return roll;
    }

    /**
     * Checks if the entity is attempting to sprint with supercharger engaged.
     * If so, returns the target roll for the piloting skill check.
     */
    public PilotingRollData checkUsingOverdrive(EntityMovementType overallMoveType) {
        PilotingRollData roll = getBasePilotingRoll(overallMoveType);
        return checkUsingOverdrive(roll, overallMoveType);
    }

    /**
     * Checks if the entity is attempting to sprint with supercharger engaged.
     * If so, returns the target roll for the piloting skill check.
     */
    public PilotingRollData checkUsingOverdrive(PilotingRollData basePilotingRoll, EntityMovementType overallMoveType) {
        PilotingRollData roll = basePilotingRoll.copy();

        if ((overallMoveType == EntityMovementType.MOVE_SPRINT
                || overallMoveType == EntityMovementType.MOVE_VTOL_SPRINT)
                && (this instanceof Tank
                        || (this instanceof QuadVee && getConversionMode() == QuadVee.CONV_MODE_VEHICLE))) {
            roll.append(new PilotingRollData(getId(), 0, "using overdrive"));
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE,
                    "Check false: Entity is not using overdrive");
        }

        return roll;
    }

    /**
     * Checks if the entity is attempting to increase two speed categories.
     * If so, returns the target roll for the piloting skill check.
     */
    public PilotingRollData checkGunningIt(EntityMovementType overallMoveType) {
        PilotingRollData roll = getBasePilotingRoll(overallMoveType);
        return checkGunningIt(roll, overallMoveType);
    }

    /**
     * Checks if the entity is attempting to increase two speed categories.
     * If so, returns the target roll for the piloting skill check.
     */
    public PilotingRollData checkGunningIt(PilotingRollData basePilotingRoll, EntityMovementType overallMoveType) {
        PilotingRollData roll = basePilotingRoll.copy();

        if (game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_VEHICLE_ACCELERATION)
                && (this instanceof Tank
                        || (this instanceof QuadVee && getConversionMode() == QuadVee.CONV_MODE_VEHICLE))) {
            if (((overallMoveType == EntityMovementType.MOVE_SPRINT
                    || overallMoveType == EntityMovementType.MOVE_VTOL_SPRINT)
                    && (movedLastRound == EntityMovementType.MOVE_WALK
                            || movedLastRound == EntityMovementType.MOVE_VTOL_WALK))
                    || ((overallMoveType == EntityMovementType.MOVE_RUN
                            || overallMoveType == EntityMovementType.MOVE_VTOL_RUN)
                            && (movedLastRound == EntityMovementType.MOVE_NONE
                                    || movedLastRound == EntityMovementType.MOVE_JUMP
                                    || movedLastRound == EntityMovementType.MOVE_SKID))) {
                roll.append(new PilotingRollData(getId(), 0, "gunning it"));
                return roll;
            }
        }
        roll.addModifier(TargetRoll.CHECK_FALSE,
                "Check false: Entity is not gunning it");
        return roll;
    }

    /**
     * Checks if an entity is passing through certain terrain while not moving
     * carefully
     */
    public PilotingRollData checkRecklessMove(MoveStep step,
                                              EntityMovementType moveType, Hex curHex, Coords lastPos,
                                              Coords curPos, Hex prevHex) {

        PilotingRollData roll = getBasePilotingRoll(moveType);
        return checkRecklessMove(roll, step, moveType, curHex, lastPos, curPos, prevHex);
    }

    /**
     * Checks if an entity is passing through certain terrain while not moving
     * carefully
     */
    public PilotingRollData checkRecklessMove(PilotingRollData basePilotingRoll, MoveStep step,
            EntityMovementType moveType, Hex curHex, Coords lastPos,
            Coords curPos, Hex prevHex) {
        PilotingRollData roll = new PilotingRollData(basePilotingRoll);

        PlanetaryConditions conditions = game.getPlanetaryConditions();
        // no need to go further if movement is careful
        if (step.isCareful()) {
            roll.addModifier(TargetRoll.CHECK_FALSE, "moving carefully");
            return roll;
        }

        // this only applies in fog, night conditions, or if a hex along the
        // move path has ice
        boolean isBlackIce;

        boolean blackIceCheck = game.getOptions().booleanOption(OptionsConstants.ADVANCED_BLACK_ICE)
                && conditions.getTemperature() <= PlanetaryConditions.BLACK_ICE_TEMP;
        isBlackIce = conditions.getWeather().isIceStorm() || blackIceCheck;

        // if we are jumping, then no worries
        if (moveType == EntityMovementType.MOVE_JUMP) {
            roll.addModifier(TargetRoll.CHECK_FALSE, "jumping is not reckless?");
            return roll;
        }

        // we need to make this check on the first move forward and anytime the
        // hex is not clear or is a level change
        boolean levelChange = (null != prevHex)
                && (prevHex.getLevel() != curHex.getLevel());
        boolean moved = (curHex.movementCost(this) > 0)
                || levelChange;
        if (conditions.isRecklessConditions()
                && !lastPos.equals(curPos)
                && lastPos.equals(step.getEntity().getPosition())) {
            roll.append(new PilotingRollData(getId(), 0, "moving recklessly"));
        }
        // FIXME: no perfect solution in the current code to determine if hex is
        // clear. I will use movement costs
        else if (conditions.isRecklessConditions()
                && !lastPos.equals(curPos)
                && moved) {
            roll.append(new PilotingRollData(getId(), 0, "moving recklessly"));
            // ice conditions
        } else if (curHex.containsTerrain(Terrains.ICE)) {
            roll.append(new PilotingRollData(getId(), 0, "moving recklessly"));
        } else if (curHex.containsTerrain(Terrains.BLACK_ICE)) {
            roll.append(new PilotingRollData(getId(), 0, "moving recklessly"));
        } else if (curHex.hasPavement() && isBlackIce) {
            roll.append(new PilotingRollData(getId(), 0, "moving recklessly"));
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
    public PilotingRollData checkLandingWithDamage(EntityMovementType overallMoveType) {
        PilotingRollData roll = getBasePilotingRoll(overallMoveType);
        return checkLandingWithDamage(roll);
    }
    /**
     * Checks if the entity is landing (from a jump) with damage that would
     * force a PSR. If so, returns the target roll for the piloting skill check.
     */
    public PilotingRollData checkLandingWithDamage(PilotingRollData basePilotingRoll) {
        PilotingRollData roll = basePilotingRoll.copy();

        int gyroHits = getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_GYRO, Mek.LOC_CT);
        // Heavy duty gyro does not force PSR until second hit
        if (getGyroType() == Mek.GYRO_HEAVY_DUTY || getGyroType() == Mek.GYRO_SUPERHEAVY) {
            gyroHits--;
        }
        if (gyroHits > 0 || hasLegActuatorCrit()) {
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
        return checkLandingWithPrototypeJJ(roll);
    }
    /**
     * Checks if the entity is landing (from a jump) with a prototype JJ If so,
     * returns the target roll for the piloting skill check.
     */
    public PilotingRollData checkLandingWithPrototypeJJ(PilotingRollData basePilotingRoll) {
        PilotingRollData roll = basePilotingRoll.copy();

        if (getJumpType() == Mek.JUMP_PROTOTYPE) {
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
    public PilotingRollData checkLandingInHeavyWoods(EntityMovementType overallMoveType, Hex curHex) {
        PilotingRollData roll = getBasePilotingRoll(overallMoveType);
        return checkLandingInHeavyWoods(roll, curHex);
    }
    /**
     * Checks if an entity is landing (from a jump) in heavy woods.
     */
    public PilotingRollData checkLandingInHeavyWoods(PilotingRollData basePilotingRoll, Hex curHex) {
        PilotingRollData roll = basePilotingRoll.copy();
        if (curHex.containsTerrain(Terrains.WOODS, 2)) {
            roll.append(new PilotingRollData(getId(), 0,
                    "landing in heavy woods"));
            addPilotingModifierForTerrain(roll);
        } else if (curHex.containsTerrain(Terrains.WOODS, 3)) {
            roll.append(new PilotingRollData(getId(), 0,
                    "landing in ultra woods"));
            addPilotingModifierForTerrain(roll);
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE,
                    "hex does not contain heavy or ultra woods");
        }
        return roll;
    }

    /**
     * Checks if the entity is landing (from a jump) on ice-covered water.
     */
    public PilotingRollData checkLandingOnIce(
            EntityMovementType overallMoveType, Hex curHex) {
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
     * Checks if the entity is landing (from a jump) on black ice.
     */
    public PilotingRollData checkLandingOnBlackIce(
            EntityMovementType overallMoveType, Hex curHex) {
        PilotingRollData roll = getBasePilotingRoll(overallMoveType);

        if (curHex.containsTerrain(Terrains.BLACK_ICE)) {
            roll.append(new PilotingRollData(getId(), 0,
                    "landing on black ice"));
            addPilotingModifierForTerrain(roll);
            adjustDifficultTerrainPSRModifier(roll);
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE,
                    "hex is not covered by black ice");
        }

        return roll;
    }



    /**
     * return a <code>PilotingRollData</code> checking for whether this Entity
     * moved too fast due to low gravity
     *
     * @param step
     * @param moveType
     * @return PilotingRollData
     */
    public PilotingRollData checkMovedTooFast(MoveStep step, EntityMovementType moveType) {
        PilotingRollData roll = getBasePilotingRoll(moveType);
        return checkMovedTooFast(roll, step, moveType);
    }

    /**
     * return a <code>PilotingRollData</code> checking for whether this Entity
     * moved too fast due to low gravity
     *
     * @param pilotBaseRoll
     * @param step
     * @return PilotingRollData
     */
    public PilotingRollData checkMovedTooFast(PilotingRollData pilotBaseRoll, MoveStep step,
            EntityMovementType moveType) {
        PilotingRollData roll = pilotBaseRoll.copy();
        addPilotingModifierForTerrain(roll, step);
        int maxSafeMP;
        switch (moveType) {
            case MOVE_JUMP:
                maxSafeMP = getJumpMP(MPCalculationSetting.NO_GRAVITY);
                break;
            case MOVE_SPRINT:
            case MOVE_VTOL_SPRINT:
                maxSafeMP = getSprintMP(MPCalculationSetting.SAFE_MOVE) + wigeBonus;
                if (isEligibleForPavementOrRoadBonus() && gotPavementOrRoadBonus) {
                    maxSafeMP++;
                }
                break;
            default:
                // Max safe MP is based on whatever is the current maximum.
                // http://bg.battletech.com/forums/index.php?topic=6681.msg154097#msg154097
                maxSafeMP = getRunMP(MPCalculationSetting.SAFE_MOVE) + wigeBonus;
                if (isEligibleForPavementOrRoadBonus() && gotPavementOrRoadBonus) {
                    maxSafeMP++;
                }
                break;
        }
        if (step.getMpUsed() > maxSafeMP) {
            roll.append(new PilotingRollData(getId(), 0,
                    "used more MPs than at 1G possible"));
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE,
                    "Check false: Entity did not use more "
                            + "MPs walking/running than possible at 1G");
        }
        return roll;
    }

    /**
     * Checks if the entity might skid. If so, returns the target roll for the piloting skill check.
     */
    public PilotingRollData checkSkid(EntityMovementType moveType, Hex prevHex,
                                      EntityMovementType overallMoveType, MoveStep prevStep, MoveStep currStep,
                                      int prevFacing, int curFacing, Coords lastPos, Coords curPos,
                                      boolean isInfantry, int distance) {
        PilotingRollData roll = getBasePilotingRoll(overallMoveType);
        return checkSkid(roll, moveType, prevHex, overallMoveType, prevStep, currStep, prevFacing, curFacing, lastPos, curPos, isInfantry, distance);

    }
    /**
     * Checks if the entity might skid. If so, returns the target roll for the piloting skill check.
     */
    public PilotingRollData checkSkid(PilotingRollData pilotBaseRoll,  EntityMovementType moveType, Hex prevHex,
            EntityMovementType overallMoveType, MoveStep prevStep, MoveStep currStep,
            int prevFacing, int curFacing, Coords lastPos, Coords curPos,
            boolean isInfantry, int distance) {

        if (isAirborne() || isAirborneVTOLorWIGE()) {
            return new PilotingRollData(id, TargetRoll.CHECK_FALSE, "flying units don't skid");
        }

        if (moveType == EntityMovementType.MOVE_JUMP) {
            return new PilotingRollData(id, TargetRoll.CHECK_FALSE, "jumping units don't skid");
        }

        if ((null != prevStep) && prevStep.isHasJustStood()) {
            return new PilotingRollData(id, TargetRoll.CHECK_FALSE, "units don't skid from getting up");
        }

        PilotingRollData roll = pilotBaseRoll.copy();

        // If we aren't traveling along a road, apply terrain modifiers
        boolean previousStepCountsAsPavement = (prevStep == null) || prevStep.isPavementStep();
        if (!previousStepCountsAsPavement || !currStep.isPavementStep()) {
            addPilotingModifierForTerrain(roll, lastPos);
        }

        boolean prevStepPavement = (prevStep != null) ? prevStep.isPavementStep() : ((prevHex != null) && prevHex.hasPavement());
        PlanetaryConditions conditions = game.getPlanetaryConditions();
        boolean affectedByIce = !movementMode.isHoverOrWiGE() || conditions.getWind().isStrongerThan(Wind.STRONG_GALE);
        boolean runOrSprint = (overallMoveType == EntityMovementType.MOVE_RUN) || (overallMoveType == EntityMovementType.MOVE_SPRINT);
        boolean unitTouchesIce = (prevHex != null) && prevHex.containsTerrain(Terrains.ICE) && (currStep.getElevation() == 0);
        boolean unitTouchesBlackIce = (prevHex != null) && prevHex.containsTerrain(Terrains.BLACK_ICE)
            && (((currStep.getElevation() == 0) && prevHex.containsAnyTerrainOf(Terrains.ROAD, Terrains.PAVEMENT))
            || (prevHex.containsTerrain(Terrains.BRIDGE_ELEV) && (currStep.getElevation() == prevHex.terrainLevel(Terrains.BRIDGE_ELEV))));
        boolean isMoveAndTurn = (prevFacing != curFacing) && !Objects.equals(curPos, lastPos);

        if (unitTouchesIce && affectedByIce && isMoveAndTurn) {
            // Turning on ice
            roll.append(new PilotingRollData(getId(), getMovementBeforeSkidPSRModifier(distance), "turning on ice"));

        } else if (unitTouchesBlackIce && affectedByIce && isMoveAndTurn) {
            // Turning on black ice
            roll.append(new PilotingRollData(getId(), getMovementBeforeSkidPSRModifier(distance), "turning on black ice"));

        } else if (prevStepPavement && runOrSprint && !movementMode.isHoverOrWiGE() && isMoveAndTurn) {
            // Running & turning on pavement
            String description = isMek() ? "running & turning on pavement" : "reckless driving on pavement";
            roll.append(new PilotingRollData(getId(), getMovementBeforeSkidPSRModifier(distance), description));

        } else {
            return new PilotingRollData(id, TargetRoll.CHECK_FALSE, "unit doesn't skid");
        }
        adjustDifficultTerrainPSRModifier(roll);
        return roll;
    }

    /**
     * Checks if the entity is moving into rubble. If so, returns the target
     * roll for the piloting skill check.
     */
    public PilotingRollData checkRubbleMove(MoveStep step,
            EntityMovementType moveType, Hex curHex, Coords lastPos,
            Coords curPos, boolean isLastStep, boolean isPavementStep) {
        PilotingRollData roll = getBasePilotingRoll(moveType);
        addPilotingModifierForTerrain(roll, curPos, true);

        if (!lastPos.equals(curPos)
                && ((moveType != EntityMovementType.MOVE_JUMP) || isLastStep)
                && (curHex.terrainLevel(Terrains.RUBBLE) > 0) && !isPavementStep
                && (step.getElevation() == 0) && canFall()) {
            adjustDifficultTerrainPSRModifier(roll);
            if (hasAbility(OptionsConstants.PILOT_TM_MOUNTAINEER)) {
                roll.addModifier(-1, "Mountaineer");
            }
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
            EntityMovementType moveType, Hex curHex, Coords lastPos,
            Coords curPos, int lastElev, boolean isPavementStep) {
        PilotingRollData roll = getBasePilotingRoll(moveType);
        int bgMod = curHex.getBogDownModifier(getMovementMode(),
                this instanceof LargeSupportTank);

        // we check for bog down on entering a new hex or changing altitude
        // but not if we're jumping, above the "ground" (meaning the bottom of the
        // lake),
        // not susceptible to bog down as per getBogDownModifier,
        // and not on pavement
        if ((!lastPos.equals(curPos) || (step.getElevation() != lastElev))
                && (bgMod != TargetRoll.AUTOMATIC_SUCCESS)
                && (moveType != EntityMovementType.MOVE_JUMP)
                && (step.getElevation() == -curHex.depth()) && !isPavementStep) {

            roll.append(new PilotingRollData(getId(), bgMod, "avoid bogging down"));

            if ((this instanceof Mek) && isSuperHeavy()) {
                roll.addModifier(1, "superheavy Mek avoiding bogging down");
            }

            if (hasAbility(OptionsConstants.PILOT_TM_SWAMP_BEAST)) {
                roll.addModifier(-1, "Swamp Beast");
            }

            addPilotingModifierForTerrain(roll, curPos, false);
            adjustDifficultTerrainPSRModifier(roll);
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE,
                    "Check false: Not entering bog-down terrain, "
                            + "or jumping/hovering over such terrain");
        }
        return roll;
    }

    /**
     * Checks if the entity is moving into depth 1+ water. If so, returns the
     * target roll for the piloting skill check.
     */
    public PilotingRollData checkWaterMove(MoveStep step,
                                           EntityMovementType moveType, Hex curHex, Coords lastPos,
                                           Coords curPos, boolean isPavementStep) {
        PilotingRollData roll = getBasePilotingRoll(moveType);
        return checkWaterMove(roll, step, moveType, curHex, lastPos, curPos, isPavementStep);
    }
    /**
     * Checks if the entity is moving into depth 1+ water. If so, returns the
     * target roll for the piloting skill check.
     */
    public PilotingRollData checkWaterMove(PilotingRollData pilotBaseRoll, MoveStep step,
            EntityMovementType moveType, Hex curHex, Coords lastPos,
            Coords curPos, boolean isPavementStep) {
        PilotingRollData roll = pilotBaseRoll.copy();

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
                && canFall()
                && !isPavementStep) {
            return checkWaterMove(roll, curHex.terrainLevel(Terrains.WATER));
        }
        return checkWaterMove(roll, 0);
    }


    /**
     * Checks if the entity is moving into depth 1+ water. If so, returns the
     * target roll for the piloting skill check.
     */
    public PilotingRollData checkWaterMove(int waterLevel,
                                           EntityMovementType overallMoveType) {
        PilotingRollData roll = getBasePilotingRoll(overallMoveType);
        return checkWaterMove(roll, waterLevel);
    }

    /**
     * Checks if the entity is moving into depth 1+ water. If so, returns the
     * target roll for the piloting skill check.
     */
    public PilotingRollData checkWaterMove(PilotingRollData pilotBaseRoll, int waterLevel) {
        PilotingRollData roll = pilotBaseRoll.copy();

        int mod;
        if (waterLevel == 1) {
            mod = -1;
        } else if (waterLevel == 2) {
            mod = 0;
        } else {
            mod = 1;
        }

        if ((waterLevel > 1) && hasAbility(OptionsConstants.PILOT_TM_FROGMAN)
                && ((this instanceof Mek) || (this instanceof ProtoMek))) {
            roll.append(new PilotingRollData(getId(), -1, "Frogman"));
        }
        if (waterLevel > 0) {
            // append the reason modifier
            roll.append(new PilotingRollData(getId(), mod,
                    "entering Depth " + waterLevel + " Water"));
            adjustDifficultTerrainPSRModifier(roll);
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE,
                    "Check false: No water here.");
        }

        return roll;
    }
    // pilotingBaseRoll

    /**
     * Checks if the entity is being swarmed. If so, returns the target roll for
     * the piloting skill check to dislodge them.
     */
    public PilotingRollData checkDislodgeSwarmers(MoveStep step,
                                                  EntityMovementType moveType) {
        PilotingRollData roll = getBasePilotingRoll(moveType);
        return checkDislodgeSwarmers(roll, step);
    }

        /**
         * Checks if the entity is being swarmed. If so, returns the target roll for
         * the piloting skill check to dislodge them.
         */
    public PilotingRollData checkDislodgeSwarmers(PilotingRollData pilotingBaseRoll, MoveStep step) {
        // If we're not being swarmed, return CHECK_FALSE
        if (Entity.NONE == getSwarmAttackerId()) {
            return new PilotingRollData(getId(), TargetRoll.CHECK_FALSE,
                    "Check false: No swarmers attached");
        }

        // append the reason modifier
        PilotingRollData roll = pilotingBaseRoll.copy();
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
     *         stepping on roof, 8 changing elevations within a building
     */
    public int checkMovementInBuilding(MoveStep step, MoveStep prevStep,
            Coords curPos, Coords prevPos) {
        if ((prevPos == null)
                || (prevPos.equals(curPos) && !(this instanceof ProtoMek))) {
            return 0;
        }
        Hex curHex = game.getBoard().getHex(curPos);
        Hex prevHex = game.getBoard().getHex(prevPos);
        // ineligible because of movement type or unit type
        if (isAirborne()) {
            return 0;
        }

        if ((this instanceof Infantry)
                && (step.getMovementType(false) != EntityMovementType.MOVE_JUMP)) {
            return 0;
        }

        if ((this instanceof ProtoMek) && (prevStep != null)
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
                .terrainLevel(Terrains.BLDG_ELEV))
                || (step.getElevation() == curHex
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
            Building bldgEntered;
            bldgEntered = game.getBoard().getBuildingAt(curPos);
            if (bldgEntered.getType() == BuildingType.WALL) {
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

        if ((this instanceof Infantry) || (this instanceof ProtoMek)) {
            if ((rv != 2) && (rv != 8) && (rv != 10)) {
                rv = 0;
            }
        }
        return rv;
    }



    /**
     * Calculates and returns the roll for an entity moving in buildings.
     */
    public PilotingRollData rollMovementInBuilding(Building bldg, int distance, String why, EntityMovementType overallMoveType) {
        PilotingRollData roll = getBasePilotingRoll(overallMoveType);
        return rollMovementInBuilding(roll, bldg, distance, why);
    }

    /**
     * Calculates and returns the roll for an entity moving in buildings.
     */
    public PilotingRollData rollMovementInBuilding(PilotingRollData pilotingBaseRoll, Building bldg, int distance, String why) {
        PilotingRollData roll = pilotingBaseRoll.copy();

        if ((this instanceof Mek) && isSuperHeavy()) {
            roll.addModifier(4, "superheavy Mek moving in building");
        }

        if (hasQuirk(OptionsConstants.QUIRK_NEG_OVERSIZED)) {
            roll.addModifier(1, "oversized unit");
        }

        int mod = 0;
        String desc;

        if (why.isBlank()) {
            desc = "moving through ";
        } else {
            desc = why + " ";
        }

        switch (bldg.getType()) {
            case LIGHT:
                desc = "Light";
                break;
            case MEDIUM:
                if (bldg.getBldgClass() != Building.HANGAR) {
                    mod = 1;
                    desc = "Medium";
                }

                if (bldg.getBldgClass() >= Building.FORTRESS) {
                    mod = 2;
                    desc = desc + " Fortress";
                }
                break;
            case HEAVY:
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
                break;
            case HARDENED:
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
            case WALL:
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
     * Only check for satisfied turn mode for Tanks or QuadVees in vehicle mode, or
     * LAMs in
     * AirMek mode. Except for LAMs, check whether advanced vehicle ground movement
     * is enabled.
     *
     * @return True if this <code>Entity</code> must make a driving check for
     *         turning too sharply.
     */
    public boolean usesTurnMode() {
        return false;
    }

    /**
     * If using advanced vehicle ground movement, checks whether the unit is
     * required to make
     * a driving roll for turning, and if so whether it succeeds.
     *
     * @param overallMoveType   The type move movement used this turn.
     * @param straightLineHexes The number of hexes that were moved in a straight
     *                          line before turning.
     * @param mpUsed            The total number of movement points used by the
     *                          entity during the current turn.
     * @param currPos           The position of the hex where the turn is taking
     *                          place, which may
     *                          modify a roll for terrain.
     * @return True if the entity failed a driving check due to turning too sharply.
     */
    public PilotingRollData checkTurnModeFailure(EntityMovementType overallMoveType,
            int straightLineHexes, int mpUsed, Coords currPos) {
        PlanetaryConditions conditions = game.getPlanetaryConditions();

        PilotingRollData roll = getBasePilotingRoll(overallMoveType);
        // Turn mode
        if (!usesTurnMode()) {
            roll.addModifier(TargetRoll.CHECK_FALSE,
                    "Check false: unit does not use turn modes.");
            return roll;
        }

        int turnMode = mpUsed / 5;
        if (straightLineHexes >= turnMode) {
            roll.addModifier(TargetRoll.CHECK_FALSE,
                    "Check false: unit did not exceed turn mode.");
            return roll;
        }

        if (getWeightClass() < EntityWeightClass.WEIGHT_MEDIUM
                || getWeightClass() == EntityWeightClass.WEIGHT_SMALL_SUPPORT) {
            roll.addModifier(-1, "light vehicle");
        } else if (getWeightClass() == EntityWeightClass.WEIGHT_ASSAULT
                || getWeightClass() == EntityWeightClass.WEIGHT_SUPER_HEAVY) {
            roll.addModifier(+1, "assault vehicle");
        }

        Hex currHex = game.getBoard().getHex(currPos);
        if (movementMode != EntityMovementMode.HOVER
                && movementMode != EntityMovementMode.VTOL
                && movementMode != EntityMovementMode.WIGE) {
            if (currHex.containsTerrain(Terrains.MUD)) {
                roll.addModifier(+1, "mud");
            }
            if (currHex.containsTerrain(Terrains.ICE)) {
                roll.addModifier(movementMode == EntityMovementMode.TRACKED ? 1 : 2, "ice");
            }
            if (conditions.isSleeting()
                    || conditions.getFog().isFogHeavy()
                    || conditions.getWeather().isHeavyRainOrGustingRainOrDownpour()) {
                roll.addModifier(+1, "fog/rain");
            }
            if (conditions.getWeather().isHeavySnow()) {
                roll.addModifier(movementMode == EntityMovementMode.TRACKED ? 1 : 2, "snow");
            }
        }

        roll.addModifier(turnMode - straightLineHexes, "did not satisfy turn mode");

        return roll;
    }

    /**
     * Calculate the piloting skill roll modifier, based upon the number of
     * hexes moved this phase. Used for skidding.
     */
    public int getMovementBeforeSkidPSRModifier(int distance) {
        int mod;

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
        } else { // 0-2 hexes
            mod = -1;
        }

        if (hasAbility(OptionsConstants.PILOT_MANEUVERING_ACE)) {
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
     * Returns the maximum number of downward elevation changes a unit can make.
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
        addTransporter(component, false);
    }

    /**
     * Add a transportation component to this Entity. Please note, this method
     * should only be called during this entity's construction.
     *
     * @param component - One of this new entity's <code>Transporter</code>s.
     * @param isOmniPod - Whether this is part of an omni unit's pod space.
     */
    public void addTransporter(Transporter component, boolean isOmniPod) {
        component.setGame(game);
        transports.add(component);
        if (isOmniPod) {
            omniPodTransports.add(component);
        }
    }

    public void removeTransporter(Transporter t) {
        transports.remove(t);
        omniPodTransports.remove(t);
    }

    /**
     * Remove all transportation components from this Entity. Should probably
     * only be called during construction.
     */
    public void removeAllTransporters() {
        transports = new Vector<>();
        omniPodTransports.clear();
    }

    /**
     * Determines if this object can accept the given unit. The unit may not be
     * of the appropriate type or there may be no room for the unit.
     *
     * @param unit - the <code>Entity</code> to be loaded.
     * @return <code>true</code> if the unit can be loaded, <code>false</code>
     *         otherwise.
     */
    public boolean canLoad(Entity unit, boolean checkElev) {
        // For now, if it's infantry, it can't load anything.
        // Period!
        if (this instanceof Infantry) {
            return false;
        }

        // one can only load one's own team's units!
        if (!unit.isEnemyOf(this)) {

            /*
             * Mechanized BA and protoMeks occupy the same space, and if one is already
             * present the other cannot be loaded. It is still possible for a support vee
             * to carry mechanized BA externally and protos in a bay, so we need to check
             * for
             * external units first then check for conflicts only for other external mounts.
             */
            boolean hasExternalBA = false;
            boolean hasExternalProtos = false;
            boolean hasExternalUltraheavy = false;
            if (unit.hasETypeFlag(Entity.ETYPE_BATTLEARMOR)
                    || unit.hasETypeFlag(Entity.ETYPE_PROTOMEK)) {
                for (Transporter t : transports) {
                    // ProtoMekClampMount is a subclass of BattleArmorHandles so we need to check it
                    // first
                    if (t instanceof ProtoMekClampMount) {
                        hasExternalProtos |= t.getUnused() == 0;
                        hasExternalUltraheavy |= t.getLoadedUnits().stream()
                                .anyMatch(e -> e.getWeightClass() == EntityWeightClass.WEIGHT_SUPER_HEAVY);
                    } else if (t instanceof BattleArmorHandles) {
                        hasExternalBA |= t.getUnused() == 0;
                    }
                }
            }
            // We can't mix BA and protos, and we can't mount an ultraheavy proto if already
            // carrying another.
            boolean noExternalMount = (unit.hasETypeFlag(Entity.ETYPE_BATTLEARMOR) && hasExternalProtos)
                    || (unit.hasETypeFlag(Entity.ETYPE_PROTOMEK) && hasExternalBA);

            if (unit.hasETypeFlag(Entity.ETYPE_PROTOMEK) && hasExternalProtos) {
                noExternalMount |= hasExternalUltraheavy
                        || (unit.getWeightClass() == EntityWeightClass.WEIGHT_SUPER_HEAVY);
            }

            for (Transporter t : transports) {
                if (t.canLoad(unit)
                        && (!checkElev || unit.getElevation() == getElevation())
                        && !((t instanceof BattleArmorHandles) && noExternalMount)) {
                    return true;
                }
            }
        }

        // If we got here, none of our transports can carry the unit.
        return false;
    }

    @Override
    public boolean canLoad(Entity unit) {
        return canLoad(unit, true);
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
                    && ((bayNumber == -1)
                            || ((next instanceof Bay) && (((Bay) next).getBayNumber() == bayNumber))
                            || ((next instanceof DockingCollar)
                                    && (((DockingCollar) next).getCollarNumber() == bayNumber)))) {
                next.load(unit);
                unit.setTargetBay(-1); // Reset the target bay for later.
                return;
            }
        }

        // If we got to this point, then we can't load the unit.
        throw new IllegalArgumentException(getShortName() + " can not load " + unit.getShortName());
    }

    /**
     * Load the given unit.
     *
     * @param unit      the Entity to be loaded.
     * @param checkElev When true, only allows the load if both units are at the
     *                  same elevation
     * @throws IllegalArgumentException If the unit can't be loaded
     */
    public void load(Entity unit, boolean checkElev) {
        load(unit, checkElev, -1);
    }

    /**
     * Load the given unit.
     *
     * @param unit      the Entity to be loaded.
     * @param bayNumber The bay to load into
     * @throws IllegalArgumentException If the unit can't be loaded
     */
    public void load(Entity unit, int bayNumber) {
        load(unit, true, bayNumber);
    }

    /**
     * Load the given unit, checking if the elevation of both units is the same.
     *
     * @param unit the Entity to be loaded.
     * @throws IllegalArgumentException If the unit can't be loaded
     */
    @Override
    public void load(Entity unit) {
        load(unit, true, -1);
    }

    /**
     * Recover the given unit. Only for ASF and Small Craft
     *
     * @param unit - the <code>Entity</code> to be loaded.
     * @throws IllegalArgumentException If the unit can't be loaded
     */
    public void recover(Entity unit) {
        // Walk through this entity's transport components;
        // find those that can load the unit.
        // load the unit into the best match.
        if (unit.getElevation() == getElevation()) {
            if (unit.isDropShip()) {
                for (Transporter nextbay : transports) {
                    if ((nextbay instanceof DockingCollar) && nextbay.canLoad(unit)) {
                        ((DockingCollar) nextbay).recover(unit);
                        return;
                    }
                }
            } else {
                if (unit.isFighter()) {
                    for (Bay nextbay : getTransportBays()) {
                        if ((nextbay instanceof ASFBay) && nextbay.canLoad(unit)) {
                            ((ASFBay) nextbay).recover(unit);
                            return;
                        }
                    }
                }
                for (Bay nextbay : getTransportBays()) {
                    if ((nextbay instanceof SmallCraftBay) && nextbay.canLoad(unit)) {
                        ((SmallCraftBay) nextbay).recover(unit);
                        return;
                    }
                }
            }
        }
        throw new IllegalArgumentException(
                getDisplayName() + " does not have a bay that can load" + unit.getDisplayName());
    }

    /**
     * cycle through and update Bays
     */
    public void updateBays() {
        Enumeration<Transporter> iter = transports.elements();
        while (iter.hasMoreElements()) {
            Transporter next = iter.nextElement();
            if (next instanceof ASFBay nextBay) {
                nextBay.updateSlots();
            }
            if (next instanceof SmallCraftBay nextBay) {
                nextBay.updateSlots();
            }
        }
    }

    /**
     * Damages a randomly determined bay door on the entity, if one exists
     */
    public String damageBayDoor() {
        String bayType = "none";

        Vector<Bay> potential = new Vector<>();

        Enumeration<Transporter> iter = transports.elements();
        while (iter.hasMoreElements()) {
            Transporter next = iter.nextElement();
            if (next instanceof Bay nextBay) {
                if (nextBay.getCurrentDoors() > 0) {
                    potential.add(nextBay);
                }
            }
        }

        if (!potential.isEmpty()) {
            Bay chosenBay = potential.elementAt(Compute.randomInt(potential.size()));
            chosenBay.destroyDoor();
            chosenBay.resetDoors();
            bayType = String.format("%s bay #%s", chosenBay.getType(), chosenBay.getBayNumber());
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
     * Damages a randomly determined docking collar on the entity, if one exists.
     *
     * @return true if a docking collar was found, false otherwise
     */
    public boolean damageDockCollar() {
        Vector<DockingCollar> dockingCollars = getDockingCollars();
        if (!dockingCollars.isEmpty()) {
            DockingCollar chosenDC = dockingCollars.elementAt(Compute.randomInt(dockingCollars.size()));
            chosenDC.setDamaged(true);
            return true;
        } else {
            return false;
        }
    }

    public void pickUp(MekWarrior mw) {
        pickedUpMekWarriors.addElement(mw.getId());
    }

    /**
     * Get a <code>List</code> of the units currently loaded into this payload.
     *
     * @return A <code>List</code> of loaded <code>Entity</code> units. This
     *         list will never be <code>null</code>, but it may be empty. The
     *         returned <code>List</code> is independent from the under- lying
     *         data structure; modifying one does not affect the other.
     */
    @Override
    public List<Entity> getLoadedUnits() {
        List<Entity> result = new ArrayList<>();

        // Walk through this entity's transport components;
        // add all of their lists to ours.
        for (Transporter next : transports) {
            // Don't look at trailer hitches here, that's separate
            if (next instanceof TankTrailerHitch) {
                continue;
            }
            for (Entity e : next.getLoadedUnits()) {
                if (e != null) {
                    result.add(e);
                }
            }
        }

        // Return the list.
        return result;
    }

    /**
     * @return the number of docking collars
     */
    public int getDocks() {
        return getDocks(false);
    }

    /**
     * @param forCost Whether this value is being used for cost calculations, in
     *                which case
     *                dropshuttle bays count as two collars.
     * @return The number of docking collars
     */
    public int getDocks(boolean forCost) {
        int n = 0;
        for (Transporter next : transports) {
            if ((next instanceof DockingCollar)
                    || (forCost && (next instanceof DropshuttleBay))) {
                n += next.hardpointCost();
            }
        }
        return n;
    }

    /**
     * Generate a list of the Ids of entities stored in bays.
     * Used by MHQ in cases where we can't get the entities via Game
     *
     * @return
     */
    public List<Integer> getBayLoadedUnitIds() {
        List<Integer> result = new ArrayList<>();

        // Walk through this entity's transport components;
        // add all of their lists to ours.
        for (Transporter next : transports) {
            if (next instanceof Bay) {
                result.addAll(((Bay) next).getLoadedUnitIds());
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
        // TODO: Change transports to a map or other indexed data structure to avoid
        // linear-time algorithm.
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
     * Returns the DockingCollar with the given ID or null if this unit doesn't have
     * such a
     * Docking Collar.
     *
     * @return the DockingCollar with the given ID or null
     */
    @Nullable
    public DockingCollar getCollarById(int collarNumber) {
        return getDockingCollars().stream()
                .filter(dc -> dc.getCollarNumber() == collarNumber)
                .findAny().orElse(null);
    }

    /**
     * @return only entities in ASF Bays that can be launched (i.e. not in
     *         recovery)
     */
    public Vector<Entity> getLaunchableFighters() {
        Vector<Entity> result = new Vector<>();

        // Walk through this entity's transport components;
        // add all of their lists to ours.

        // I should only add entities in bays that are functional
        for (Transporter next : transports) {
            if ((next instanceof ASFBay nextbay) && (nextbay.getCurrentDoors() > 0)) {
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
        Vector<Entity> result = new Vector<>();

        // Walk through this entity's transport components;
        // add all of their lists to ours.

        // I should only add entities in bays that are functional
        for (Transporter next : transports) {
            if ((next instanceof Bay nextbay) && (nextbay.getCurrentDoors() > 0)) {
                for (Entity e : nextbay.getDroppableUnits()) {
                    result.addElement(e);
                }
            }
        }

        // Return the list.
        return result;
    }

    /**
     * @return All Entities that can at this point be unloaded from any of the bays
     *         of this Entity. This
     *         does not include any units that were loaded this turn or any bays
     *         where the door capacity has
     *         been exceeded this turn.
     *         Note that the returned list may be unmodifiable.
     *
     * @see #wasLoadedThisTurn()
     * @see Bay#canUnloadUnits()
     */
    public List<Entity> getUnitsUnloadableFromBays() {
        return transports.stream()
                .filter(t -> t instanceof Bay).map(t -> (Bay) t)
                .filter(Bay::canUnloadUnits)
                .flatMap(b -> b.getUnloadableUnits().stream())
                .filter(e -> !e.wasLoadedThisTurn())
                .toList();
    }

    /**
     * @return All Entities that can at this point be unloaded from any transports
     *         of this Entity which are
     *         no Bays. This does not include any units that were loaded this turn.
     *         Note that the returned list may be unmodifiable.
     *
     * @see #wasLoadedThisTurn()
     */
    public List<Entity> getUnitsUnloadableFromNonBays() {
        return transports.stream()
                .filter(t -> !(t instanceof Bay))
                .flatMap(b -> b.getLoadedUnits().stream())
                .filter(e -> !e.wasLoadedThisTurn())
                .toList();
    }

    /**
     * @return All Entities that can at this point be unloaded from any transports
     *         of this Entity. This does
     *         not include any units that were loaded this turn nor units from bays
     *         where the door capacity has been
     *         exceeded this turn.
     *
     * @see #wasLoadedThisTurn()
     * @see Bay#canUnloadUnits()
     */
    public List<Entity> getUnloadableUnits() {
        List<Entity> loadedUnits = new ArrayList<>(getUnitsUnloadableFromNonBays());
        loadedUnits.addAll(getUnitsUnloadableFromBays());
        return loadedUnits;
    }

    /**
     * @return get the bays separately
     */
    public Vector<Bay> getFighterBays() {
        Vector<Bay> result = new Vector<>();

        for (Transporter next : transports) {
            if (((next instanceof ASFBay) || (next instanceof SmallCraftBay))
                    && (((Bay) next).getCurrentDoors() > 0)) {
                result.addElement((Bay) next);
            }
        }

        // Return the list.
        return result;
    }

    /**
     * Returns a list of all Docking Collars (Hardpoints) of this unit.
     *
     * @return a list of all Docking Collars.
     */
    public Vector<DockingCollar> getDockingCollars() {
        return transports.stream()
                .filter(t -> t instanceof DockingCollar)
                .map(t -> (DockingCollar) t)
                .collect(Collectors.toCollection(Vector::new));
    }

    /**
     * @return A vector of Transporter objects this unit uses/has
     */
    public Vector<Transporter> getTransports() {
        return transports;
    }

    public boolean isPodMountedTransport(Transporter t) {
        return omniPodTransports.contains(t);
    }

    public Vector<Bay> getTransportBays() {
        Vector<Bay> result = new Vector<>();

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

    public Vector<Entity> getLaunchableSmallCraft() {
        Vector<Entity> result = new Vector<>();

        // Walk through this entity's transport components;
        // add all of their lists to ours.
        for (Transporter next : transports) {
            if ((next instanceof SmallCraftBay nextbay)
                    && (nextbay.getCurrentDoors() > 0)) {
                for (Entity e : nextbay.getLaunchableUnits()) {
                    result.addElement(e);
                }
            }
        }

        // Return the list.
        return result;
    }

    /**
     * Returns a list of DropShips launchable from any of the Transport
     * facilities of this unit. The list may be empty but not null.
     *
     * @return A list of launchable DropShips.
     */
    public List<Entity> getLaunchableDropships() {
        return transports.stream()
                .filter(t -> t instanceof DockingCollar)
                .map(t -> ((DockingCollar) t).getLaunchableUnits())
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    @Override
    public boolean unload(Entity unit) {
        // Walk through this entity's transport components; try to remove the unit from
        // each in turn.
        for (Transporter trsp : transports) {
            if (trsp.unload(unit)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void resetTransporter() {
        transports.forEach(Transporter::resetTransporter);
    }

    @Override
    public String getUnusedString() {
        return getUnusedString(ViewFormatting.NONE);
    }

    @Override
    public double getUnused() {
        return transports.stream().mapToDouble(Transporter::getUnused).sum();
    }

    /**
     * Returns the current amount of cargo space for an entity of the given
     * type.
     *
     * @param e An entity that defines the unit class
     * @return The number of units of the given type that can be loaded in this
     *         Entity
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
    public String getUnusedString(ViewFormatting formatting) {
        StringBuilder result = new StringBuilder();

        // Walk through this entity's transport components;
        // add all of their string to ours.
        Enumeration<Transporter> iter = transports.elements();
        while (iter.hasMoreElements()) {
            Transporter next = iter.nextElement();
            if ((next instanceof Bay) && ((Bay) next).isQuarters()) {
                continue;
            }
            if ((next instanceof DockingCollar) && ((DockingCollar) next).isDamaged()) {
                continue;
            }
            if (formatting == ViewFormatting.HTML && (next instanceof Bay) && (((Bay) next).getBayDamage() > 0)) {
                result.append("<font color='red'>")
                        .append(next.getUnusedString())
                        .append("</font>");
            } else if (formatting == ViewFormatting.DISCORD && (next instanceof Bay)
                    && (((Bay) next).getBayDamage() > 0)) {
                result.append(DiscordFormat.RED)
                        .append(next.getUnusedString())
                        .append(DiscordFormat.RESET);
            } else {
                result.append(next.getUnusedString());
            }
            if (isOmni() && ((next instanceof InfantryCompartment)
                    || (next instanceof Bay))) {
                if (omniPodTransports.contains(next)) {
                    result.append(" (Pod)");
                } else {
                    result.append(" (Fixed)");
                }
            }
            // Add a newline character between strings.
            if (iter.hasMoreElements()) {
                if (formatting == ViewFormatting.HTML) {
                    result.append("<br>");
                } else {
                    result.append("\n");
                }
            }
        }

        // Return the String.
        return result.toString();
    }

    @Override
    public boolean isWeaponBlockedAt(int loc, boolean isRear) {
        return transports.stream().anyMatch(trsp -> trsp.isWeaponBlockedAt(loc, isRear));
    }

    @Override
    public Entity getExteriorUnitAt(int loc, boolean isRear) {
        return transports.stream()
                .map(trsp -> trsp.getExteriorUnitAt(loc, isRear))
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
    }

    @Override
    public List<Entity> getExternalUnits() {
        List<Entity> externalUnits = new ArrayList<>();
        for (Transporter t : transports) {
            externalUnits.addAll(t.getExternalUnits());
        }
        return externalUnits;
    }

    @Override
    public int getCargoMpReduction(Entity carrier) {
        return transports.stream().mapToInt(t -> t.getCargoMpReduction(carrier)).sum();
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
     * @return The number of additional crew capacity provided by quarters in
     *         transport bays.
     */
    public int getBayPersonnel() {
        int count = 0;
        for (Bay bay : getTransportBays()) {
            count += bay.getPersonnel(isClan());
        }
        return count;
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
     * Determine if this unit has an active and working stealth system.
     * <p>
     * Sub-classes are encouraged to override this method.
     *
     * @return <code>true</code> if this unit has a stealth system that is
     *         currently active and it's actually working, <code>false</code> if
     *         there is no stealth system or if it is inactive.
     */
    public boolean isStealthActive() {
        return false;
    }

    /**
     * Determine if this unit has an active and working stealth system.
     * <p>
     * Sub-classes are encouraged to override this method.
     *
     * @return <code>true</code> if this unit has a stealth system that is
     *         currently active and it's actually working, <code>false</code> if
     *         there is no stealth system or if it is inactive.
     */
    public boolean isStealthOn() {
        return false;
    }

    /**
     * Determine if this unit has an active null-signature system.
     * <p>
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
     * Determine if this unit has an active null-signature system.
     * <p>
     * Sub-classes are encouraged to override this method.
     *
     * @return <code>true</code> if this unit has a null signature system that
     *         is currently active, <code>false</code> if there is no stealth
     *         system or if it is inactive.
     */
    public boolean isNullSigOn() {
        return false;
    }

    /**
     * Determine if this unit has an active void signature system that is
     * providing its benefits.
     * <p>
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
     * Determine if this unit has an active void signature system.
     * <p>
     * Sub-classes are encouraged to override this method.
     *
     * @return <code>true</code> if this unit has a void signature system that
     *         is currently active, <code>false</code> if there is no stealth
     *         system or if it is turned off.
     */
    public boolean isVoidSigOn() {
        return false;
    }

    /**
     * Determine if this unit has an active chameleon light polarization field.
     * <p>
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
     * Determine if this unit has an active chameleon light polarization field.
     * <p>
     * Sub-classes are encouraged to override this method.
     *
     * @return <code>true</code> if this unit has a void signature system that
     *         is currently active, <code>false</code> if there is no stealth
     *         system or if it is inactive.
     */
    public boolean isChameleonShieldOn() {
        return false;
    }

    /**
     * Determine the stealth modifier for firing at this unit from the given
     * range. If the value supplied for <code>range</code> is not one of the
     * <code>Entity</code> class range constants, an
     * <code>IllegalArgumentException</code> will be thrown.
     * <p>
     * Sub-classes are encouraged to override this method.
     *
     * @param range - an <code>int</code> value that must match one of the
     *              <code>Compute</code> class range constants.
     * @param ae    - the entity making the attack, who maybe immune to certain
     *              kinds of stealth
     * @return a <code>TargetRoll</code> value that contains the stealth
     *         modifier for the given range.
     */
    public TargetRoll getStealthModifier(int range, Entity ae) {
        TargetRoll result;

        // Stealth must be active.
        if (!isStealthActive()) {
            result = new TargetRoll(0, "stealth not active");
        }

        // Get the range modifier.
        result = switch (range) {
            case RangeType.RANGE_MINIMUM, RangeType.RANGE_SHORT, RangeType.RANGE_MEDIUM, RangeType.RANGE_LONG,
                 RangeType.RANGE_EXTREME, RangeType.RANGE_LOS, RangeType.RANGE_OUT ->
                    new TargetRoll(0, "stealth not installed");
            default -> throw new IllegalArgumentException("Unknown range constant: "
                    + range);
        };

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
        for (AmmoMounted amounted : getAmmo()) {
            AmmoType atype = amounted.getType();
            if (((atype.getAmmoType() == AmmoType.T_SRM) || (atype.getAmmoType() == AmmoType.T_SRM_IMP)
                    || (atype.getAmmoType() == AmmoType.T_MML))
                    && (atype.getMunitionType().contains(AmmoType.Munitions.M_INFERNO))
                    && (amounted.getHittableShotsLeft() > 0)) {
                found = true;
            }
            if ((atype.getAmmoType() == AmmoType.T_IATM)
                    && (atype.getMunitionType().contains(AmmoType.Munitions.M_IATM_IIW))
                    && (amounted.getHittableShotsLeft() > 0)) {
                found = true;
            }
        }
        return found;
    }

    /**
     * Check for vulnerability to anti-TSM munitions. ATSM affects Meks with
     * prototype TSM and
     * any industrial TSM created before 3050, and conventional infantry with TSM
     * implants.
     *
     * @return Whether the unit is affected by ATSM munitions
     */
    public boolean antiTSMVulnerable() {
        return false;
    }

    /**
     * Record if the unit is just combat-lossed or if it has been utterly
     * destroyed.
     *
     * @param canSalvage - a <code>boolean</code> that is <code>true</code> if the
     *                   unit
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

    public void setClearingMinefield(boolean clearingMinefield) {
        this.clearingMinefield = clearingMinefield;
    }

    /**
     * @return True if this entity is spotting this round.
     */
    public boolean isSpotting() {
        return spotting;
    }

    /**
     * Sets if this unit is spotting this round.
     *
     * @param spotting True if this unit is to be set as spotting
     */
    public void setSpotting(boolean spotting) {
        this.spotting = spotting;
    }

    /**
     * Um, basically everything can spot for LRM indirect fire.
     * Except for off-board units, units that sprinted,
     * and units evading.
     *
     * @return true, if the entity is eligible to spot
     */
    public boolean canSpot() {
        return isActive() && !isOffBoard() &&
                (moved != EntityMovementType.MOVE_SPRINT) &&
                (moved != EntityMovementType.MOVE_VTOL_SPRINT) &&
                (!isEvading());
    }

    @Override
    public String toString() {
        return "Entity [" + getDisplayName() + ", ID: " + getId() + "]";
    }

    /**
     * This returns a textual description of the entity for visually impaired users.
     */
    public String statusToString() {
        // should include additional information like immobile.
        String str = "Entity [" + getDisplayName() + ", " + getId() + "]:";
        if (getPosition() != null) {
            str = str + "Location: (" + (getPosition().getX() + 1) + ", "
                    + (getPosition().getY() + 1) + ") ";
            str += "Facing: " + Facing.valueOfInt(getFacing()).name();
        }

        str += " MP: " + getWalkMP();
        if (getOriginalJumpMP() > 0) {
            str += " Jump: " + getJumpMP();
        }
        str += " Owner: " + getOwner().getName() + " Armor: " + getTotalArmor()
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

        String str = getLocationName(loc) + " (" + getLocationAbbr(loc)
                + "): Armor: " + getArmorString(loc) + "/" + getOArmor(loc)
                + " Structure: " + getInternalString(loc) + "/"
                + getOInternal(loc) + "\n ";
        for (CriticalSlot cs : crits[loc]) {
            if (cs != null) {
                Mounted<?> mount = cs.getMount();
                if (mount != null) {
                    str += mount.getDesc() + "\n ";
                }
            }
        }
        return str;
    }

    /**
     * @param str a string defining the location
     * @return the status of the given location.
     */
    public String statusToString(String str) {
        int loc = getLocationFromAbbr(str);

        if (loc == LOC_NONE) {
            try {
                loc = Integer.parseInt(str);
            } catch (NumberFormatException nfe) {
                // stay at LOC_NONE
            }
        }

        return statusToString(loc);
    }

    /**
     * The round the unit will be deployed. We will deploy at the end of a
     * round. So if depoyRound is set to 5, we will deploy when round 5 is over.
     *
     * @param deployRound an int
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
        return !isDeployed()
                && (getDeployRound() <= round)
                && !isOffBoard();
    }

    /**
     * Returns true if the offboard entity should be deployed this round.
     *
     * @param round The current round number.
     * @return True if and only if the offboard entity should deploy this
     *         round, otherwise false.
     */
    public boolean shouldOffBoardDeploy(int round) {
        return isOffBoard()
                && !isDeployed()
                && (getDeployRound() <= round);
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
     *         to a unit, <code>{@link Entity#NONE}</code> will be returned.
     */
    public short getUnitNumber() {
        return unitNumber;
    }

    /**
     * Returns whether an entity can flee from its current position. Currently
     * returns true if the entity is on the edge of the board.
     */
    public boolean canFlee(Coords pos) {
        return ((getWalkMP() > 0) || (this instanceof Infantry))
                && !isProne()
                && !isStuck()
                && !isShutDown()
                && !getCrew().isUnconscious()
                && (getSwarmTargetId() == NONE)
                && (isOffBoard() || ((pos != null) && game.canFleeFrom(this, pos)));
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
        if ((game != null) && !game.getOptions().booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND)) {
            return true;
        }
        return visibleToEnemy;
    }

    public void setDetectedByEnemy(boolean b) {
        detectedByEnemy = b;
    }

    public boolean isDetectedByEnemy() {
        // If double blind isn't on, the unit is always detected
        if ((game != null) && !game.getOptions().booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND)) {
            return true;
        }
        return detectedByEnemy;
    }

    public void addBeenSeenBy(Player p) {
        if ((p != null) && !entitySeenBy.contains(p)) {
            entitySeenBy.add(p);
        }
    }

    public Vector<Player> getWhoCanSee() {
        return entitySeenBy;
    }

    public void setWhoCanSee(Vector<Player> entitySeenBy) {
        this.entitySeenBy = entitySeenBy;
    }

    public void clearSeenBy() {
        entitySeenBy.clear();
    }

    /**
     * Returns true if the given player can see this Entity, including
     * teammates if team_vision is on.
     *
     */
    public boolean hasSeenEntity(Player p) {
        // No double blind - everyone sees everything
        if ((game == null) || !game.getOptions().booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND)) {
            return true;
        }
        // Null players see nothing
        if (p == null) {
            return false;
        }
        // A Player can always see their own 'Meks
        if (getOwner().equals(p)) {
            return true;
        }

        // If a player can see all, it sees this
        if (p.canIgnoreDoubleBlind()) {
            return true;
        }

        // Observers can see units spotted by an enemy
        if (p.isObserver()) {
            for (Player other : entitySeenBy) {
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
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_TEAM_VISION)) {
            for (Player teammate : game.getPlayersList()) {
                if ((teammate.getTeam() == p.getTeam()) && entitySeenBy.contains(teammate)) {
                    return true;
                }
            }
        }
        // Can't see
        return false;
    }

    public void addBeenDetectedBy(Player p) {
        // This is for saved-game backwards compatibility
        if (entityDetectedBy == null) {
            entityDetectedBy = new Vector<>();
        }

        if ((p != null) && !entityDetectedBy.contains(p)) {
            entityDetectedBy.add(p);
        }
    }

    public Vector<Player> getWhoCanDetect() {
        return entityDetectedBy;
    }

    public void setWhoCanDetect(Vector<Player> entityDetectedBy) {
        this.entityDetectedBy = entityDetectedBy;
    }

    public void clearDetectedBy() {
        // This is for saved-game backwards compatibility
        if (entityDetectedBy == null) {
            entityDetectedBy = new Vector<>();
        }
        entityDetectedBy.clear();
    }

    /**
     * @return true if the given player can see this Entity, including teammates if
     *         team_vision is on.
     */
    public boolean hasDetectedEntity(Player p) {
        // No sensors - no one detects anything
        if ((game == null)
                || !game.getOptions().booleanOption(OptionsConstants.ADVANCED_TACOPS_SENSORS)) {
            return false;
        }
        // Null players detect nothing
        if (p == null) {
            return false;
        }
        // This is for saved-game backwards compatibility
        if (entityDetectedBy == null) {
            entityDetectedBy = new Vector<>();
        }

        // Observers can detect units detected by an enemy
        if (p.isObserver()) {
            for (Player other : entityDetectedBy) {
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
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_TEAM_VISION)) {
            for (Player teammate : game.getPlayersList()) {
                if ((teammate.getTeam() == p.getTeam()) && entityDetectedBy.contains(teammate)) {
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
     *                The player trying to view this unit
     * @return True if the given player can only see this Entity as a sensor
     *         return
     */
    public boolean isSensorReturn(Player spotter) {
        boolean alliedUnit = !getOwner().isEnemyOf(spotter)
                || (getOwner().getTeam() == spotter.getTeam()
                        && game.getOptions().booleanOption(OptionsConstants.ADVANCED_TEAM_VISION));

        boolean sensors = (game.getOptions().booleanOption(
                OptionsConstants.ADVANCED_TACOPS_SENSORS)
                || game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_ADVANCED_SENSORS));
        boolean sensorsDetectAll = game.getOptions().booleanOption(
                OptionsConstants.ADVANCED_SENSORS_DETECT_ALL);
        boolean doubleBlind = game.getOptions().booleanOption(
                OptionsConstants.ADVANCED_DOUBLE_BLIND);

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
     * @return Whether this type of unit can perform charges
     */
    public boolean canCharge() {
        return !isImmobile() && (getWalkMP() > 0) && !isStuck() && !isProne();
    }

    /**
     * @return Whether this type of unit can perform DFA attacks
     */
    public boolean canDFA() {
        return !isImmobile() && (getJumpMP() > 0) && !isStuck() && !isProne();
    }

    /**
     * @return Whether this type of unit can perform Ramming attacks
     */
    public boolean canRam() {
        return false;
    }

    public boolean isUsingManAce() {
        return hasAbility(OptionsConstants.PILOT_MANEUVERING_ACE);
    }

    public Enumeration<Entity> getKills() {
        final int killer = id;
        return game.getSelectedOutOfGameEntities(entity -> killer == entity.killerId);
    }

    public int getKillNumber() {
        final int killer = id;
        return game.getSelectedOutOfGameEntityCount(entity -> killer == entity.killerId);
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
    public boolean isEligibleFor(GamePhase phase) {
        // only deploy in deployment phase
        if (phase.isDeployment() == isDeployed()) {
            if (!isDeployed() && phase.isSetArtilleryAutohitHexes()
                    && isEligibleForArtyAutoHitHexes()) {
                logger.debug("Artillery Units Present and Advanced PreDesignate option enabled");
            } else {
                return false;
            }
        }

        // carcass can't do anything
        if (isCarcass()) {
            return false;
        }

        // Hidden units are always eligible for PRE phases
        if (phase.isPremovement() || phase.isPrefiring()) {
            return isHidden();
        }

        // Hidden units shouldn't be counted for turn order, unless deploying or firing
        // (spotting)
        if (isHidden() && !phase.isDeployment() && !phase.isFiring()) {
            return false;
        }

        return switch (phase) {
            case MOVEMENT -> isEligibleForMovement();
            case FIRING -> isEligibleForFiring();
            case PHYSICAL -> isEligibleForPhysical();
            case TARGETING -> isEligibleForTargetingPhase();
            case OFFBOARD -> isEligibleForOffboard();
            default -> true;
        };
    }

    /**
     * Determines if an entity is eligible for a phase. Called only if at least
     * one entity returned true to isEligibleFor() This is for using
     * searchlights in physical and offboard phase, without forcing the phase to be
     * played when not needed. However, it could be used for other things in the
     * future
     */
    public boolean canAssist(GamePhase phase) {
        if (!phase.isPhysical() && !phase.isFiring() && !phase.isOffboard()) {
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
        return isUsingSearchlight();
    }

    /**
     * An entity is eligible for firing if it's not taking some kind of action
     * that prevents it from firing, such as a full-round physical attack
     * or sprinting.
     */
    public boolean isEligibleForFiring() {
        // if you're charging, no shooting
        if (isUnjammingRAC() || isCharging() || isMakingDfa() || isRamming()) {
            return false;
        }

        if (moved == EntityMovementType.MOVE_SPRINT
            || moved == EntityMovementType.MOVE_VTOL_SPRINT) {
            if(isMek()) {
                return getMisc().stream().anyMatch(m -> m.getType().hasFlag(MiscType.F_TSM));
            }
            return false;
        }

        // if you're offboard, no shooting
        if (isOffBoard() || isAssaultDropInProgress()) {
            return false;
        }

        // check game options
        if (!game.getOptions().booleanOption(OptionsConstants.BASE_SKIP_INELIGIBLE_FIRING)) {
            return true;
        }

        // must be active
        return isActive();
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
        if (isOffBoard() || (isAssaultDropInProgress()
                && !(movementMode == EntityMovementMode.WIGE))) {
            return false;
        }
        // Prevent ejected crews from moving when advanced movement rule is off
        if (!game.useVectorMove() && isSpaceborne() && this instanceof EjectedCrew) {
            return false;
        }
        // check game options
        if (!game.getOptions().booleanOption(OptionsConstants.BASE_SKIP_INELIGIBLE_MOVEMENT)) {
            return true;
        }
        // Must be active: this is slightly different from isActive();
        // we don't want to skip manually shutdown units (so they can restart)
        boolean isActive = (!shutDown || isManualShutdown()) && !destroyed
                && getCrew().isActive() && !unloadedThisTurn && deployed;
        if (!isActive
                || (isImmobile() && !isManualShutdown() && !canUnjamRAC() &&
                        !game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_VEHICLES_CAN_EJECT))) {
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
        for (WeaponMounted mounted : getWeaponList()) {
            WeaponType wtype = mounted.getType();
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
        boolean friendlyFire = game.getOptions().booleanOption(OptionsConstants.BASE_FRIENDLY_FIRE);

        if (getPosition() == null) {
            return false; // not on board?
        }

        if ((this instanceof Infantry)
                && hasWorkingMisc(MiscType.F_TOOLS,
                        MiscType.S_DEMOLITION_CHARGE)) {
            Hex hex = game.getBoard().getHex(getPosition());
            if (hex == null) {
                return false;
            }
            return hex.containsTerrain(Terrains.BUILDING);
        }

        // only Meks and protos have physical attacks (except tank charges)
        if (!((this instanceof Mek) || (this instanceof ProtoMek) || (this instanceof Infantry))) {
            return false;
        }

        // if you're charging or finding a club, it's already declared
        if (isUnjammingRAC() || isCharging() || isMakingDfa() || isRamming()
                || isFindingClub() || isOffBoard() || isAssaultDropInProgress()
                || isDropping() || isBracing()) {
            return false;
        }

        // check game options
        if (game.getOptions().booleanOption(OptionsConstants.ALLOWED_NO_CLAN_PHYSICAL)
                && getCrew().isClanPilot() && !hasINarcPodsAttached()
                && (getSwarmAttackerId() == NONE)) {
            return false;
        }

        // Issue with Vibroblades only being turned on/off during Physical phase
        // -- Torren
        if (hasVibroblades()) {
            return true;
        }

        if (!game.getOptions().booleanOption(OptionsConstants.BASE_SKIP_INELIGIBLE_PHYSICAL)) {
            return true;
        }

        // dead mek walking
        if (!isActive()) {
            return false;
        }

        // sprinted?
        if (moved == EntityMovementType.MOVE_SPRINT
                || moved == EntityMovementType.MOVE_VTOL_SPRINT) {
            return false;
        }

        // check if we have iNarc pods attached that can be brushed off
        if (hasINarcPodsAttached() && (this instanceof Mek)) {
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
            canHit |= ((this instanceof Mek) && !isProne()
                    && hasAbility(OptionsConstants.PILOT_DODGE_MANEUVER) && Compute
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
     * @return True if this Entity is eligible to pre-designate hexes as
     * auto-hits. Per TacOps pg 180, if a player has offboard artillery they get
     * 5 pre- designated hexes per mapsheet.
     */
    public boolean isEligibleForArtyAutoHitHexes() {
        return isEligibleForTargetingPhase()
                && (isOffBoard() || game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_ON_MAP_PREDESIGNATE));
    }

    public boolean isEligibleForTargetingPhase() {
        if (isAssaultDropInProgress()) {
            return false;
        }
        for (WeaponMounted mounted : getWeaponList()) {
            WeaponType wtype = mounted.getType();
            if ((wtype != null) && (wtype.hasFlag(WeaponType.F_ARTILLERY))) {
                return true;
            }
            // Bearings-only capital missiles fire during the targeting phase
            if ((wtype instanceof CapitalMissileBayWeapon) || (wtype instanceof AR10BayWeapon)) {
                if (mounted.isInBearingsOnlyMode()) {
                    return true;
                }
            }

            // Surface to surface capital missiles count as artillery
            if (getAltitude() == 0
                    && wtype instanceof CapitalMissileWeapon) {
                return true;
            }
        }
        return false;
    }

    public double getTroopCarryingSpace() {
        double space = 0;
        for (Transporter t : transports) {
            if (t instanceof InfantryCompartment) {
                space += ((InfantryCompartment) t).totalSpace;
            }
        }
        return space;
    }

    public double getPodMountedTroopCarryingSpace() {
        double space = 0;
        for (Transporter t : omniPodTransports) {
            if (t instanceof InfantryCompartment) {
                space += ((InfantryCompartment) t).totalSpace;
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

    @Override
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
     * @throws IllegalArgumentException if a negative distance, an invalid direction
     *                                  is selected, or
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
     *         deployed (in hexes); this value will never be negative.
     */
    public int getOffBoardDistance() {
        return offBoardDistance;
    }

    /**
     * Returns the direction off the board that the unit will be deployed. If the
     * unit
     * is deployed onboard, IOffBoardDirections.NONE is returned, otherwise one of
     * the
     * values:
     * <ul>
     * <li><code>IOffBoardDirections.NORTH</code></li>
     * <li><code>IOffBoardDirections.SOUTH</code></li>
     * <li><code>IOffBoardDirections.EAST</code></li>
     * <li><code>IOffBoardDirections.WEST</code></li>
     * </ul>
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
     * <p>
     * Onboard units (units with an offboard distance of zero and a direction of
     * <code>Entity.NONE</code>) will be unaffected by this method.
     *
     * @param round The current round number.
     */
    public void deployOffBoard(int round) {
        if (null == game) {
            throw new IllegalStateException(
                    "game not set; possible serialization error");
        }
        // N.B. 17 / 2 = 8, but the middle of 1..17 is 9, so we
        // add a bit (because 17 % 2 == 1 and 16 % 2 == 0).
        switch (offBoardDirection) {
            case NONE:
                return;
            case NORTH:
                setPosition(new Coords((game.getBoard().getWidth() / 2)
                        + (game.getBoard().getWidth() % 2),
                        -getOffBoardDistance() - 1));
                setFacing(3);
                break;
            case SOUTH:
                setPosition(new Coords((game.getBoard().getWidth() / 2)
                        + (game.getBoard().getWidth() % 2),
                        game.getBoard()
                                .getHeight() + getOffBoardDistance()));
                setFacing(0);
                break;
            case EAST:
                setPosition(new Coords(game.getBoard().getWidth()
                        + getOffBoardDistance(),
                        (game.getBoard().getHeight() / 2)
                                + (game.getBoard().getHeight() % 2)));
                setFacing(5);
                break;
            case WEST:
                setPosition(new Coords(-getOffBoardDistance() - 1, (game.getBoard()
                        .getHeight() / 2) + (game.getBoard().getHeight() % 2)));
                setFacing(1);
                break;
        }

        // deploy the unit, but only if it should be deployed this round
        setDeployed(shouldOffBoardDeploy(round));
    }

    public Vector<Integer> getPickedUpMekWarriors() {
        return pickedUpMekWarriors;
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

    public void setExternalSearchlight(boolean arg) {
        hasExternalSearchlight = arg;
    }

    /**
     * @return True if this unit has an external searchlight (does not consider mounted ones).
     */
    public boolean hasExternalSearchlight() {
        return hasExternalSearchlight;
    }

    /**
     * @return True if the unit has a usable searchlight. It considers both
     * externally mounted searchlights as well as internally mounted ones.
     */
    public boolean hasSearchlight() {
        for (MiscMounted m : getMisc()) {
            if (m.getType().hasFlag(MiscType.F_SEARCHLIGHT)
                    && !m.isInoperable()) {
                return true;
            }
        }
        return hasExternalSearchlight;
    }

    /**
     * Method to destroy a single searchlight on an entity. Searchlights can be
     * destroyed on a roll of 7+ on a torso hit on a mek or on a front/side hit
     * on a combat vehicle.
     */
    public void destroyOneSearchlight() {
        if (!hasSearchlight()) {
            return;
        }
        // A random searchlight should be destroyed, but this is easier...
        if (hasExternalSearchlight) {
            hasExternalSearchlight = false;
        }

        for (MiscMounted m : getMisc()) {
            if (m.getType().hasFlag(MiscType.F_SEARCHLIGHT)
                    && !m.isInoperable()) {
                m.setDestroyed(true);
                break;
            }
        }

        // Turn off the light all spot lights were destroyed
        if (!hasSearchlight()) {
            setSearchlightState(false);
        }

    }

    public void setSearchlightState(boolean arg) {
        if (hasSearchlight()) {
            searchlightIsActive = arg;
            if (arg) {
                illuminated = true;
            }
        } else {
            searchlightIsActive = false;
        }
    }

    public boolean isIlluminated() {
        // Regardless of illuminated state, if we have a searchlight active we
        // are illuminated
        return illuminated || searchlightIsActive;
    }

    public void setIlluminated(boolean arg) {
        illuminated = searchlightIsActive || arg;
    }

    public boolean isUsingSearchlight() {
        return hasSearchlight() && searchlightIsActive;
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
     * Set whether this Entity is stuck in a swamp or not
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
        StringBuilder sb = new StringBuilder();

        int condition = IEntityRemovalConditions.REMOVE_SALVAGEABLE;
        if (!canSalvage) {
            setSalvage(false);
            condition = IEntityRemovalConditions.REMOVE_DEVASTATED;
        }

        if (isDoomed() || isDestroyed()) {
            return sb.toString();
        }

        // working under the assumption that entity was neither doomed or
        // destroyed before from here on out

        setDoomed(true);

        Enumeration<Integer> iter = getPickedUpMekWarriors().elements();
        while (iter.hasMoreElements()) {
            Integer mekWarriorId = iter.nextElement();
            Entity mw = game.getEntity(mekWarriorId);
            mw.setDestroyed(true);
            game.removeEntity(mw.getId(), condition);
            sb.append("\n*** ").append(mw.getDisplayName()).append(" died in the wreckage. ***\n");
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
        hitBySwarmsEntity.addElement(entityId);
        hitBySwarmsWeapon.addElement(weaponId);
    }

    /**
     * Were we targeted by a certain swarm/swarm-i volley this turn?
     *
     * @param entityId The <code>int</code> id of the shooting entity we are
     *                 checking
     * @param weaponId The <code>int</code> id of the launcher to check
     * @return a fitting <code>boolean</code> value
     */
    public boolean getTargetedBySwarm(int entityId, int weaponId) {
        for (int i = 0; i < hitBySwarmsEntity.size(); i++) {
            Integer entityIdToTest = hitBySwarmsEntity.elementAt(i);
            Integer weaponIdToTest = hitBySwarmsWeapon.elementAt(i);
            if ((entityId == entityIdToTest) && (weaponId == weaponIdToTest)) {
                return true;
            }
        }
        return false;
    }

    public int getShortRangeModifier() {
        int mod = 0;
        if (hasAbility(OptionsConstants.GUNNERY_RANGE_MASTER, Crew.RANGEMASTER_MEDIUM)) {
            mod = 2;
        }
        if (hasAbility(OptionsConstants.GUNNERY_RANGE_MASTER, Crew.RANGEMASTER_LONG)) {
            mod = 4;
        }
        if (hasAbility(OptionsConstants.GUNNERY_RANGE_MASTER, Crew.RANGEMASTER_EXTREME)) {
            mod = 6;
        }
        if (hasAbility(OptionsConstants.GUNNERY_SNIPER) && (mod > 0)) {
            mod = mod / 2;
        }
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
        if (hasAbility(OptionsConstants.GUNNERY_RANGE_MASTER, Crew.RANGEMASTER_MEDIUM)) {
            mod = 0;
        }
        if (hasAbility(OptionsConstants.GUNNERY_SNIPER) && (mod > 0)) {
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
        if (hasAbility(OptionsConstants.GUNNERY_RANGE_MASTER, Crew.RANGEMASTER_LONG)) {
            mod = 0;
        }
        if (hasAbility(OptionsConstants.GUNNERY_SNIPER) && (mod > 0)) {
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
        if (hasAbility(OptionsConstants.GUNNERY_RANGE_MASTER, Crew.RANGEMASTER_EXTREME)) {
            mod = 0;
        }
        if (hasAbility(OptionsConstants.GUNNERY_SNIPER) && (mod > 0)) {
            mod = mod / 2;
        }
        return mod;
    }

    public int getLOSRangeModifier() {
        return 8;
    }

    public void setArmorType(int armType) {
        for (int i = 0; i < locations(); i++) {
            armorType[i] = armType;
        }
    }

    public void setArmorType(int armType, int loc) {
        armorType[loc] = armType;
        recalculateTechAdvancement();
    }

    public void setStructureType(int strucType) {
        structureType = strucType;
        structureTechLevel = getTechLevel();
        recalculateTechAdvancement();
    }

    public void setStructureTechLevel(int level) {
        structureTechLevel = level;
        recalculateTechAdvancement();
    }

    public void setArmorType(String armType) {
        if (!(armType.startsWith("Clan ") || armType.startsWith("IS "))) {
            armType = (TechConstants.isClan(getArmorTechLevel(0)) ? "Clan " : "IS ") + armType;
        }
        EquipmentType et = EquipmentType.get(armType);
        if (!(et instanceof ArmorType newArmorType)) {
            setArmorType(EquipmentType.T_ARMOR_UNKNOWN);
        } else {
            setArmorType(newArmorType.getArmorType());
            setArmorTechLevel(newArmorType.getStaticTechLevel().getCompoundTechLevel(newArmorType.isClan()));
            // TODO: Is this needed? WTF is the point of it?
            if (et.getCriticals(this) == 0) {
                try {
                    addEquipment(et, LOC_NONE);
                } catch (Exception e) {
                    // can't happen
                    logger.error("", e);
                }
            }
        }
        recalculateTechAdvancement();
    }

    public void setArmorType(String armType, int loc) {
        if (!(armType.startsWith("Clan ") || armType.startsWith("IS "))) {
            armType = (TechConstants.isClan(getArmorTechLevel(0)) ? "Clan " : "IS ") + armType;
        }
        EquipmentType et = EquipmentType.get(armType);
        if (et == null) {
            setArmorType(EquipmentType.T_ARMOR_UNKNOWN, loc);
        } else {
            setArmorType(EquipmentType.getArmorType(et), loc);
            // TODO: Is this needed? WTF is the point of it?
            if (et.getCriticals(this) == 0) {
                try {
                    addEquipment(et, LOC_NONE);
                } catch (Exception e) {
                    // can't happen
                    logger.error("", e);
                }
            }
        }
        recalculateTechAdvancement();
    }

    public void setStructureType(String strucType) {
        if (!(strucType.startsWith("Clan ") || strucType.startsWith("IS "))) {
            strucType = (isClan() ? "Clan " : "IS ") + strucType;
        }
        if (!(strucType.endsWith("Structure"))) {
            strucType += " Structure";
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
                    addEquipment(et, LOC_NONE);
                } catch (Exception e) {
                    // can't happen
                    logger.error("", e);
                }
            }
        }
        recalculateTechAdvancement();
    }

    public int getArmorType(int loc) {
        if ((loc >= 0) && (loc < armorType.length)) {
            return armorType[loc];
        } else {
            return EquipmentType.T_ARMOR_UNKNOWN;
        }
    }

    public void setArmorTechLevel(int newTL) {
        for (int i = 0; i < locations(); i++) {
            armorTechLevel[i] = newTL;
        }
        recalculateTechAdvancement();
    }

    public void setArmorTechLevel(int newTL, int loc) {
        armorTechLevel[loc] = newTL;
        recalculateTechAdvancement();
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

    public void setWeaponHit(WeaponMounted which) {
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

    /**
     * Calculates and returns the C-bill cost of the unit. The parameter can be used
     * to include or
     * exclude ("dry cost") the cost of ammunition on the unit.
     *
     * @param ignoreAmmo When true, the cost of ammo on the unit will be excluded
     *                   from the cost
     * @return The cost in C-Bills of the 'Mek in question.
     */
    public final double getCost(boolean ignoreAmmo) {
        return getCost(new DummyCalculationReport(), ignoreAmmo);
    }

    /**
     * Calculates and returns the C-bill cost of the unit. The parameter ignoreAmmo
     * can be used to include or
     * exclude ("dry cost") the cost of ammunition on the unit. A report for the
     * cost calculation will
     * be written to the given calcReport.
     *
     * @param calcReport A CalculationReport to write the report for the cost
     *                   calculation to
     * @param ignoreAmmo When true, the cost of ammo on the unit will be excluded
     *                   from the cost
     * @return The cost in C-Bills of the 'Mek in question.
     */
    public abstract double getCost(CalculationReport calcReport, boolean ignoreAmmo);

    /**
     * Returns a multiplier that combines multiplicative construction cost modifiers
     * for this Entity.
     *
     * This includes only modifiers that apply to an Entity's final, total cost
     * (e.g. - the 1.25x modifier for being
     * an omni-unit, or the 32.0x for being an aerodyne DropShip). It does NOT
     * include multipliers that only apply to
     * a sub-part of the unit (e.g. the weight based multiplier that applies to a
     * vehicle's internal structure cost).
     *
     * This allows MekHQ to scale the price of a Unit's Parts in a more appropriate
     * manner.
     *
     * Defaults to 1.0
     */
    public double getPriceMultiplier() {
        return 1.0;
    }

    /**
     * Used to for cost calculations. Though the TM rules allow a Clan unit to be
     * designed without CASE,
     * MM assumes that CASE is present in any location that has explosive equipment.
     *
     * @return The number of locations protected by Clan CASE beyond what is
     *         explicitly mounted.
     */
    public int implicitClanCASE() {
        return 0;
    }

    public boolean removePartialCoverHits(int location, int cover, int side) {
        if (cover > LosEffects.COVER_NONE) {
            switch (cover) {
                case LosEffects.COVER_LOWLEFT:
                    if (location == Mek.LOC_LLEG) {
                        return true;
                    }
                    break;
                case LosEffects.COVER_LOWRIGHT:
                    if (location == Mek.LOC_RLEG) {
                        return true;
                    }
                    break;
                case LosEffects.COVER_LEFT:
                    if ((location == Mek.LOC_LLEG)
                            || (location == Mek.LOC_LARM)
                            || (location == Mek.LOC_LT)) {
                        return true;
                    }
                    break;
                case LosEffects.COVER_RIGHT:
                    if ((location == Mek.LOC_RLEG)
                            || (location == Mek.LOC_RARM)
                            || (location == Mek.LOC_RT)) {
                        return true;
                    }
                    break;
                case LosEffects.COVER_HORIZONTAL:
                    if ((location == Mek.LOC_LLEG)
                            || (location == Mek.LOC_RLEG)) {
                        return true;
                    }
                    break;
                case LosEffects.COVER_UPPER:
                    return (location != Mek.LOC_LLEG) && (location != Mek.LOC_RLEG);
                case LosEffects.COVER_FULL:
                    return true;
                case LosEffects.COVER_75LEFT:
                    return (location != Mek.LOC_RARM) && (location != Mek.LOC_RLEG);
                case LosEffects.COVER_75RIGHT:
                    return (location != Mek.LOC_LLEG) && (location != Mek.LOC_LARM);
            }
        }
        return false;

    }

    public abstract boolean doomedInExtremeTemp();

    public abstract boolean doomedInVacuum();

    public abstract boolean doomedOnGround();

    public abstract boolean doomedInAtmosphere();

    public abstract boolean doomedInSpace();

    /**
     * Prior to TacOps errata 3.3, armor was rounded up to the nearest half ton
     * As of TacOps errata 3.3, patchwork armor is not rounded by location. Previous
     * editions
     * of the rules required it to be rounded up to the nearest half ton by
     * location.
     * Note:
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
        double armorPerTon = ArmorType.forEntity(this, loc).getPointsPerTon(this);
        double points = getOArmor(loc) + (hasRearArmor(loc) ? getOArmor(loc, true) : 0);
        return points / armorPerTon;
    }

    /**
     * The total weight of the armor on this unit. This is guaranteed to be
     * rounded properly for both single-type and patchwork armor.
     *
     * @return The armor weight in tons.
     */
    public double getArmorWeight() {
        if (hasPatchworkArmor()) {
            double total = 0;
            for (int loc = 0; loc < locations(); loc++) {
                total += getArmorWeight(loc);
            }
            return RoundWeight.standard(total, this);
        } else {
            ArmorType armor = ArmorType.forEntity(this);
            if (armor.hasFlag(MiscType.F_SUPPORT_VEE_BAR_ARMOR)) {
                double total = getTotalOArmor() * armor.getSVWeightPerPoint(getArmorTechRating());
                return RoundWeight.standard(total, this);
            } else if (armor.hasFlag(MiscType.F_BA_EQUIPMENT)) {
                return getTotalOArmor() * armor.getWeightPerPoint();
            } else {
                double armorPerTon = ArmorType.forEntity(this).getPointsPerTon(this);
                return RoundWeight.standard(getTotalOArmor() / armorPerTon, this);
            }
        }
    }

    public boolean hasTAG() {
        for (WeaponMounted m : getWeaponList()) {
            if (m.getType().hasFlag(WeaponType.F_TAG)) {
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

    /**
     * Get the entity's "climbing mode"
     *
     * @return True or false, where true = climb up and false = go through
     */
    public boolean climbMode() {
        return climbMode;
    }

    public void setClimbMode(boolean state) {
        climbMode = state;
    }

    public boolean usedTag() {
        for (WeaponMounted weapon : getWeaponList()) {
            if (weapon.isUsedThisRound() && weapon.getType().hasFlag(WeaponType.F_TAG)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasEiCockpit() {
        return ((game != null) && game.getOptions().booleanOption(
                OptionsConstants.ADVANCED_ALL_HAVE_EI_COCKPIT));
    }

    public boolean hasActiveEiCockpit() {
        return (hasEiCockpit() && hasAbility(OptionsConstants.UNOFF_EI_IMPLANT));
    }

    public boolean isLayingMines() {
        return layingMines;
    }

    public void setLayingMines(boolean laying) {
        layingMines = laying;
    }

    public boolean canLayMine() {
        for (MiscMounted mounted : getMisc()) {
            MiscType type = mounted.getType();
            if (!mounted.isMissing() && !isLayingMines()
                    && (type.hasFlag(MiscType.F_MINE) || type.hasFlag(MiscType.F_VEHICLE_MINE_DISPENSER))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int sideTable(Coords src) {
        return sideTable(src, false);
    }

    @Override
    public int sideTable(Coords src, boolean usePrior) {
        return sideTable(src, usePrior, facing);
    }

    public int sideTable(Coords src, boolean usePrior, int face) {
        return sideTable(src, usePrior, face, getPosition());
    }

    public int sideTable(Coords src, boolean usePrior, int face, Coords effectivePos) {
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
            Hex srcHex = game.getBoard().getHex(src);
            Hex curHex = game.getBoard().getHex(getPosition());
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
                        OptionsConstants.ADVCOMBAT_TACOPS_ADVANCED_MEK_HIT_LOCATIONS) && (this instanceof QuadMek));
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
        if (isAero()) {
            IAero a = (IAero) this;
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
     * @param roll           the PSR to modify
     * @param c              the coordinates where the PSR happens
     * @param enteringRubble True if entering rubble, else false
     */
    public void addPilotingModifierForTerrain(PilotingRollData roll, Coords c, boolean enteringRubble) {
        if ((c == null) || (roll == null)) {
            return;
        }
        if (isOffBoard() || !(isDeployed())) {
            return;
        }
        Hex hex = game.getBoard().getHex(c);
        hex.applyTerrainPilotingModifiers(getMovementMode(), roll, enteringRubble);

        if (hex.containsTerrain(Terrains.JUNGLE) && hasAbility(OptionsConstants.PILOT_TM_FOREST_RANGER)) {
            roll.addModifier(-1, "Forest Ranger");
        }
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

        if (!isElevationValid(getElevation(), game.getBoard().getHex(getPosition()))) {
            logger.error(String.format("%s in hex %s is at invalid elevation %s",
                    getDisplayName(), HexTarget.coordsToId(getPosition()), getElevation()));
            setElevation(-game.getBoard().getHex(getPosition()).depth());
            logger.error(" moved to elevation " + getElevation());
            return true;
        }
        return false;
    }

    public Engine getEngine() {
        return engine;
    }

    /**
     * @return The type of engine if it has an engine, or Engine.NONE, if it has no
     *         engine.
     */
    public int getEngineType() {
        return hasEngine() ? getEngine().getEngineType() : Engine.NONE;
    }

    public boolean hasEngine() {
        return (null != engine);
    }

    public void setEngine(Engine e) {
        engine = e;
    }

    public boolean itemOppositeTech(String s) {
        if (isClan()) { // Clan base
            return s.toLowerCase().contains("(is)") || s.toLowerCase().contains("inner sphere");
        } else {
            return s.toLowerCase().contains("(c)") || s.toLowerCase().contains("clan");
        }
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
     * @return whether the unit is suffering from Electromagnetic
     *         Interference
     */
    public boolean isSufferingEMI() {
        return _isEMId;
    }

    public void setEMI(boolean inVal) {
        _isEMId = inVal;
    }

    /**
     * The attack direction modifier for rolls on the motive system hits table
     * for the given side (as defined in {@link ToHitData}). This will return 0
     * if Tactical Operations vehicle effectiveness rules are in effect or if
     * the side parameter falls outside ToHitData's range of "fixed" side
     * values; in particular, it will return 0 if handed
     * {@link ToHitData#SIDE_RANDOM}.
     *
     * @param side
     *             The attack direction as specified above.
     * @return The appropriate directional roll modifier.
     */
    public int getMotiveSideMod(int side) {
        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_VEHICLE_EFFECTIVE)) {
            return 0;
        }
        return switch (side) {
            case ToHitData.SIDE_LEFT, ToHitData.SIDE_RIGHT, ToHitData.SIDE_FRONTLEFT, ToHitData.SIDE_FRONTRIGHT,
                 ToHitData.SIDE_REARLEFT, ToHitData.SIDE_REARRIGHT -> 2;
            case ToHitData.SIDE_REAR -> 1;
            default -> 0;
        };
    }

    /**
     * Checks if the unit is hardened against nuclear strikes.
     *
     * @return true if this is a hardened unit.
     */
    public abstract boolean isNuclearHardened();

    /**
     * Set the hidden state of this entity (used for hidden units rules, TW pg 259).
     */
    public void setHidden(boolean inVal) {
        isHidden = inVal;
    }

    public void setMadePointblankShot(boolean inVal) {
        madePointblankShot = inVal;
    }

    /**
     * @param phase the phase for this hidden unit to become active in.
     */
    public void setHiddenActivationPhase(final GamePhase phase) {
        hiddenActivationPhase = phase;
    }

    /** Returns true if this unit is currently hidden (hidden units, TW pg 259). */
    public boolean isHidden() {
        return isHidden;
    }

    /**
     * @return True if this unit has already made a pointblank shot this round.
     */
    public boolean madePointblankShot() {
        return madePointblankShot;
    }

    /**
     * @return the phase that this hidden unit will activate in (generally this will
     *         be
     *         Game.Phase.UNKNOWN, indicating that the unit isn't activating).
     */
    public GamePhase getHiddenActivationPhase() {
        return hiddenActivationPhase;
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
     * @see Entity#isCarcass
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

        // all critical slots set as missing
        // while we're here, if something is mounted in those crits, set it as hit,
        // instead of looping through all equipment in the unit
        for (int i = 0; i < getNumberOfCriticals(loc); i++) {
            final CriticalSlot cs = getCritical(loc, i);
            if (cs != null) {
                // count engine hits for maxtech engine explosions
                if ((cs.getType() == CriticalSlot.TYPE_SYSTEM)
                        && (cs.getIndex() == Mek.SYSTEM_ENGINE)
                        && !cs.isDamaged()) {
                    engineHitsThisPhase++;
                }

                final boolean mountOneIsHittable = (cs.getMount() != null) && cs.getMount().getType().isHittable();
                final boolean mountTwoIsHittable = (cs.getMount2() != null) && cs.getMount2().getType().isHittable();

                if (blownOff) {
                    cs.setMissing(true);

                    if (mountOneIsHittable) {
                        cs.getMount().setMissing(true);
                    }

                    if (mountTwoIsHittable) {
                        cs.getMount2().setMissing(true);
                    }

                } else {
                    cs.setHit(true);

                    if (mountOneIsHittable) {
                        cs.getMount().setHit(true);
                    }

                    if (mountTwoIsHittable) {
                        cs.getMount2().setHit(true);
                    }
                }
            }
        }

        // some equipment is not present in critical slots
        // but is present in the location, so we'll need to look at it as well
        for (Mounted<?> mounted : getEquipment()) {
            if (((mounted.getLocation() == loc) && mounted.getType().isHittable())
                    || (mounted.isSplit() && (mounted.getSecondLocation() == loc))) {
                if (blownOff) {
                    mounted.setMissing(true);
                    // we don't want to hit something twice here to avoid triggering
                    // things that fire off when a mounted is hit
                } else if (!mounted.isHit()) {
                    mounted.setHit(true);
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
        pendingNarcPods.removeIf(narcPod -> !locationCanHoldNarcPod(narcPod.getLocation()));
        narcPods.removeIf(narcPod -> !locationCanHoldNarcPod(narcPod.getLocation()));
        pendingINarcPods.removeIf(iNarcPod -> !locationCanHoldNarcPod(iNarcPod.getLocation()));
        iNarcPods.removeIf(iNarcPod -> !locationCanHoldNarcPod(iNarcPod.getLocation()));
    }

    private boolean locationCanHoldNarcPod(final int location) {
        return (getInternal(location) > 0)
                && !isLocationBlownOff(location)
                && !isLocationBlownOffThisPhase(location);
    }

    /**
     * This clears all Narc and iNarc Pods from an Entity. It is used in MekHQ to
     * clear this
     * transient data.
     */
    public void clearNarcAndiNarcPods() {
        pendingNarcPods.clear();
        narcPods.clear();
        pendingINarcPods.clear();
        iNarcPods.clear();
    }

    public PilotingRollData checkSideSlip(EntityMovementType moveType,
                                          Hex prevHex, EntityMovementType overallMoveType,
                                          MoveStep prevStep, int prevFacing, int curFacing, Coords lastPos,
                                          Coords curPos, int distance, boolean speedBooster) {
        PilotingRollData roll = getBasePilotingRoll(overallMoveType);
        return checkSideSlip(roll, moveType, prevHex, overallMoveType, prevStep, prevFacing, curFacing,
                lastPos, curPos, distance, speedBooster);
    }

    public PilotingRollData checkSideSlip(PilotingRollData pilotBaseRoll, EntityMovementType moveType,
            Hex prevHex, EntityMovementType overallMoveType,
            MoveStep prevStep, int prevFacing, int curFacing, Coords lastPos,
            Coords curPos, int distance, boolean speedBooster) {
        PilotingRollData roll = pilotBaseRoll.copy();

        if ((moveType != EntityMovementType.MOVE_JUMP)
                && (prevHex != null)
                && (distance > 1)
                && ((overallMoveType == EntityMovementType.MOVE_RUN)
                        || (overallMoveType == EntityMovementType.MOVE_VTOL_RUN)
                        || (overallMoveType == EntityMovementType.MOVE_SPRINT)
                        || (overallMoveType == EntityMovementType.MOVE_VTOL_SPRINT))
                && (prevFacing != curFacing) && !lastPos.equals(curPos)
                && !(this instanceof Infantry)
                && !(this instanceof ProtoMek)) {
            roll.append(new PilotingRollData(getId(), 0, "flanking and turning"));
            if (isUsingManAce()) {
                roll.addModifier(-1, "Maneuvering Ace");
            }
        } else if (moveType != EntityMovementType.MOVE_JUMP
                && prevFacing == curFacing && !lastPos.equals(curPos)
                && lastPos.direction(curPos) % 3 != curFacing % 3
                && !(isUsingManAce() && (overallMoveType == EntityMovementType.MOVE_WALK
                        || overallMoveType == EntityMovementType.MOVE_VTOL_WALK))) {
            roll.append(new PilotingRollData(getId(), -1, "controlled sideslip"));
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE,
                    "Check false: not apparently sideslipping");
        }

        return roll;
    }

    @Override
    public boolean isAirborneVTOLorWIGE() {
        // Dead VTOLs/ WiGEs can't be airborne
        if (isDestroyed()) {
            return false;
        }

        // stuff that moves like a VTOL is flying unless at elevation 0 or on
        // top of/in a building,
        if ((getMovementMode() == EntityMovementMode.VTOL)
                || (getMovementMode() == EntityMovementMode.WIGE)) {
            if ((game != null)
                    && (game.getBoard() != null)
                    && (getPosition() != null)
                    && (game.getBoard().getHex(getPosition()) != null)
                    && ((game.getBoard().getHex(getPosition())
                            .terrainLevel(Terrains.BLDG_ELEV) >= getElevation())
                            || (game
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

    public boolean hasLinkedMGA(WeaponMounted mounted) {
        for (WeaponMounted m : getWeaponList()) {
            if ((m.getLocation() == mounted.getLocation())
                    && m.getType().hasFlag(WeaponType.F_MGA)
                    && !(m.isDestroyed() || m.isBreached())
                    && m.getBayWeapons().contains(mounted)
                    && m.hasModes() && m.curMode().equals("Linked")) {
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
                && game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_SINGLE_NO_CAP)
                && !isPartOfFighterSquadron()) {
            return false;
        }

        return game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_CAPITAL_FIGHTER)
                && isFighter();
    }

    /** @return True when this unit has capital-scale armor. */
    public boolean isCapitalScale() {
        return isCapitalFighter();
    }

    /**
     * @return True if this unit is using weapons bays
     */
    public boolean usesWeaponBays() {
        return false;
    }

    /**
     * return the bay of the current weapon or ammo
     *
     * @param equipmentId The equipment index
     * @return The bay mount, or null if the equipment is not in a bay
     */
    public WeaponMounted whichBay(int equipmentId) {
        for (WeaponMounted m : getWeaponBayList()) {
            for (WeaponMounted weapon : m.getBayWeapons()) {
                // find the weapon and determine if it is there
                if (weapon.getEquipmentNum() == equipmentId) {
                    return m;
                }
            }
            for (AmmoMounted ammo : m.getBayAmmo()) {
                if (ammo.getEquipmentNum() == equipmentId) {
                    return m;
                }
            }
        }
        return null;
    }

    public int getHeatInArc(int location, boolean rearMount) {

        int arcHeat = 0;

        for (WeaponMounted mounted : getTotalWeaponList()) {
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
        int thrust;
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

        Vector<Integer> heading = new Vector<>();
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

    public @Nullable Coords getPlayerPickedPassThrough(int attackerId) {
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
     * Returns true if this Entity passed over the given target during its current path.
     *
     * @param target The target
     * @return True if this unit passed over the target this turn
     */
    public boolean passedOver(Targetable target) {
        for (Coords crd : passedThrough) {
            if (crd.equals(target.getPosition())) {
                return true;
            }
            for (Coords secondary : target.getSecondaryPositions().values()) {
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
        if (passedThrough.isEmpty()) {
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
        for (WeaponMounted m : getTotalWeaponList()) {
            int atype = m.getType().getAmmoType();
            if (atype == AmmoType.T_AC_ROTARY) {
                m.setMode("6-shot");
                m.setModeSwitchable(false);
            } else if (atype == AmmoType.T_AC_ULTRA) {
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
        for (MiscMounted m : getMisc()) {
            if ((m.getLocation() == loc) && !m.isDestroyed() && !m.isBreached()
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
        for (int i = 0; i < getNumberOfCriticals(loc); i++) {
            CriticalSlot slot = getCritical(loc, i);
            // ignore empty & system slots
            if ((slot == null)
                    || (slot.getType() != CriticalSlot.TYPE_EQUIPMENT)) {
                continue;
            }
            Mounted<?> m = slot.getMount();
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
        // This is implemented in subclasses. Do nothing in general.
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

        final var gameOpts = game.getOptions();

        // if the small craft does not already have ECM, then give them a single
        // hex ECM so they can change the mode
        // FIXME : This is a really hacky way to do it that results in small craft
        // having ECM when
        // FIXME : the rule is not in effect and in non-space maps
        if ((this instanceof SmallCraft) && !(this instanceof Dropship)
                && !hasActiveECM() && isMilitary()) {
            try {
                String prefix = isClan() ? "CL" : "IS";
                addEquipment(
                        EquipmentType.get(prefix + BattleArmor.SINGLE_HEX_ECM),
                        Aero.LOC_NOSE, false);
            } catch (LocationFullException ignored) {

            }
        }

        for (WeaponMounted mounted : getWeaponList()) {
            mounted.adaptToGameOptions(game.getOptions());
            mounted.setModesForMapType();
        }

        for (MiscMounted misc : getMisc()) {
            if (misc.getType().hasFlag(MiscType.F_BAP)
                    && (this instanceof Aero || this instanceof LandAirMek)
                    && gameOpts.booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_ECM)) {
                ArrayList<String> modes = new ArrayList<>();
                String[] stringArray = {};
                modes.add("Short");
                modes.add("Medium");
                misc.getType().setModes(modes.toArray(stringArray));
                misc.getType().setInstantModeSwitch(false);
            }

            if (misc.getType().hasFlag(MiscType.F_ECM)) {
                ArrayList<String> modes = new ArrayList<>();
                modes.add("ECM");
                String[] stringArray = {};
                if (gameOpts.booleanOption(OptionsConstants.ADVANCED_TACOPS_ECCM)) {
                    modes.add("ECCM");
                    if (misc.getType().hasFlag(MiscType.F_ANGEL_ECM)) {
                        modes.add("ECM & ECCM");
                    }
                } else if (gameOpts.booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_ECM)
                        && (this instanceof Aero || this instanceof LandAirMek)) {
                    modes.add("ECCM");
                    if (misc.getType().hasFlag(MiscType.F_ANGEL_ECM)) {
                        modes.add("ECM & ECCM");
                    }
                }

                if (gameOpts.booleanOption(OptionsConstants.ADVANCED_TACOPS_GHOST_TARGET)) {
                    if (misc.getType().hasFlag(MiscType.F_ANGEL_ECM)) {
                        modes.add("ECM & Ghost Targets");
                        if (gameOpts.booleanOption(OptionsConstants.ADVANCED_TACOPS_ECCM)) {
                            modes.add("ECCM & Ghost Targets");
                        }
                    } else {
                        modes.add("Ghost Targets");
                    }
                }

                misc.getType().setModes(modes.toArray(stringArray));
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
        } else if (isAero()) {
            return 3;
        } else {
            if (game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_SKILLED_EVASION)) {
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

    public void setTurnInterrupted(boolean interrupted) {
        turnWasInterrupted = interrupted;
    }

    /**
     * This should eventually be true for any situation where the entity's
     * turn was interrupted, e.g. walking over a minefield
     */
    public boolean turnWasInterrupted() {
        return turnWasInterrupted;
    }

    public boolean endOfTurnCargoInteraction() {
        return endOfTurnCargoInteraction;
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

    /**
     * A method to determine if an aero has suffered 3 sensor hits.
     * When double-blind is on, this affects both standard visibility and sensor
     * rolls
     */
    public boolean isAeroSensorDestroyed() {
        return false;
    }

    public boolean hasModularArmor() {
        return hasModularArmor(-1);
    }

    public boolean hasModularArmor(int loc) {
        if (cacheHasModularArmor == null) {
            cacheHasModularArmor = false;

            for (MiscMounted mount : getMisc()) {
                if ((loc == -1) || (mount.getLocation() == loc)) {
                    if (!mount.isDestroyed() && mount.getType().hasFlag(MiscType.F_MODULAR_ARMOR)) {
                        cacheHasModularArmor = true;
                    }
                }
            }
        }

        return cacheHasModularArmor;
    }

    public int getDamageReductionFromModularArmor(HitData hit, int damage, Vector<Report> vDesc) {
        int loc = hit.getLocation();
        if (!hasModularArmor(loc)) {
            return damage;
        }
        for (MiscMounted mount : getMisc()) {
            if ((mount.getLocation() == loc)
                    && !mount.isDestroyed()
                    && mount.getType().hasFlag(MiscType.F_MODULAR_ARMOR)
                    // On 'Mek torsos only, modular armor covers either front
                    // or rear, as mounted.
                    && (!(this instanceof Mek)
                            || !((loc == Mek.LOC_CT) || (loc == Mek.LOC_LT) || (loc == Mek.LOC_RT))
                            || (hit.isRear() == mount.isRearMounted()))) {

                int damageAbsorption = mount.getBaseDamageCapacity() - mount.getDamageTaken();
                if (damageAbsorption > damage) {
                    mount.takeDamage(damage);
                    Report r = new Report(3535);
                    r.subject = getId();
                    r.add(damage);
                    r.indent(1);
                    r.newlines = 0;
                    vDesc.addElement(r);
                    Report.addNewline(vDesc);

                    return 0;
                } else if (damageAbsorption == damage) {
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

                    mount.takeDamage(damage);
                    mount.setHit(true);
                    return 0;
                } else {
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

                    damage -= mount.getBaseDamageAbsorptionRate() - mount.getDamageTaken();
                    mount.setDamageTaken(mount.getBaseDamageAbsorptionRate());
                    mount.setDestroyed(true);
                    mount.setHit(true);
                }
            }

        }

        return damage;
    }

    public Roll getGhostTargetRoll() {
        return ghostTargetRoll;
    }

    public int getGhostTargetRollMoS() {
        return ghostTargetRoll.getIntValue() - (getCrew().getSensorOps() + 2);
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

    public void resetCoolantFailureAmount() {
        // This is implemented in subclasses, do nothing in general
    }

    /**
     * @return the tonnage of additional mounted communications equipment
     */
    public int getExtraCommGearTons() {
        double i = 0;
        for (MiscMounted mounted : getMisc()) {
            if (mounted.getType().hasFlag(MiscType.F_COMMUNICATIONS) && !mounted.isInoperable()) {
                i += mounted.getTonnage();
            }
        }
        return (int) Math.round(i); // the rounding shouldn't be needed, but is safer
    }

    /**
     * @return information (range, location, strength) about ECM if the unit
     * has active ECM or null if it doesn't. In the case of multiple ECCM
     * system, the best one takes precedence, as a unit can only have one
     * active ECCM at a time.
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
            if (!game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_ECM)) {
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
        for (MiscMounted m : getMisc()) {
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
     * @return information (range, location, strength) about ECCM if the unit
     * has active ECCM or null if it doesn't. In the case of multiple ECCM
     * system, the best one takes precedence, as a unit can only have one
     * active ECCM at a time.
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
            if (!game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_ECM)) {
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
        for (MiscMounted m : getMisc()) {
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
        for (MiscMounted m : getMisc()) {
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
        for (MiscMounted m : getMisc()) {
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
        for (MiscMounted misc : getMisc()) {
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
     * @return the initiative bonus this Entity grants for quirks
     */
    public int getQuirkIniBonus() {
        // command battleMek and and battle computer are not cumulative
        if (hasQuirk(OptionsConstants.QUIRK_POS_BATTLE_COMP) && !getCrew().isDead()
                && !getCrew().isUnconscious()) {
            return 2;
        } else if (hasQuirk(OptionsConstants.QUIRK_POS_COMMAND_MEK) && !getCrew().isDead()
                && !getCrew().isUnconscious()) {
            return 1;
        }
        return 0;
    }

    /**
     * Returns the Bay that the given ammo is associated with.
     *
     * @param mammo an AmmoMounted to search for
     * @return The bay (WeaponMounted) that the ammo works with
     */
    public WeaponMounted getBayByAmmo(AmmoMounted mammo) {
        for (WeaponMounted m : getWeaponBayList()) {
            for (AmmoMounted bayammo : m.getBayAmmo()) {
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
     * @return the number of rounds for which this unit should be shutdown by taser
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
     * @return The rounds for which this entity suffers from taser feedback.
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
        // this function needs to be added to the MP calculating functions
        // however, since no function calls super, it seems unneccesary complicated really.
        int max = 2;
        int modifier = impThisTurn + impLastTurn;
        modifier = modifier - (modifier % 3);
        modifier = modifier / 3;
        return (modifier > max) ? -max : -modifier;
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
     * Return the BAR-rating of this Entity's armor
     *
     * @return the BAR rating
     */
    public int getBARRating(int loc) {
        // normal armor has a BAR rating of 10
        return 10;
    }

    /**
     * @return True if this unit has BAR armor of a rating of less than 10 in the given location.
     */
    public boolean hasBARArmor(int loc) {
        return getBARRating(loc) < 10;
    }

    /**
     * Sets the barrier armor rating for support vehicles. Has no effect on other
     * unit types.
     *
     * @param rating The BAR
     */
    public void setBARRating(int rating) {
    }

    /**
     * Sets the barrier armor rating in a specific location for support vehicles.
     * Has no effect on other unit types.
     *
     * @param rating The BAR
     * @param loc    The location index
     */
    public void setBARRating(int rating, int loc) {
    }

    /**
     * @return True if this entity has an armored chassis.
     */
    public boolean hasArmoredChassis() {
        return false;
    }

    /**
     * @return True if this unit has Environmental Sealing
     */
    public boolean hasEnvironmentalSealing() {
        for (MiscMounted misc : miscList) {
            if (misc.getType().hasFlag(MiscType.F_ENVIRONMENTAL_SEALING)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Possibly do a ICE-Engine stall PSR (only intended for Meks, both
     * Industrial and Battle).
     *
     * @param vPhaseReport the {@link Report} <code>Vector</code> containing the
     *                     phase reports
     * @return a {@link Report} <code>Vector</code> containing the passed in
     *         reports, and any additional ones
     */
    public Vector<Report> doCheckEngineStallRoll(Vector<Report> vPhaseReport) {
        return vPhaseReport;
    }

    /**
     * Check for unstalling of this Entity's engine (only used for ICE-powered
     * 'Meks).
     *
     * @param vPhaseReport the {@link Report} <code>Vector</code> containing the
     *                     phase reports
     */
    public void checkUnstall(Vector<Report> vPhaseReport) {

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
        return (getElevation() == 0) && isNaval();
    }

    /**
     * Is this a naval vessel?
     *
     * @return Whether it is or not.
     */
    public boolean isNaval() {
        return (getMovementMode() == EntityMovementMode.NAVAL)
                || (getMovementMode() == EntityMovementMode.HYDROFOIL)
                || (getMovementMode() == EntityMovementMode.SUBMARINE);
    }

    /**
     * Determines if the pilot has the Nightwalker SPA
     *
     * @return true when pilots have the SPA and are not
     *         in a flying vehicle.
     */

    public boolean isNightwalker() {
        return getCrew().getOptions().booleanOption(OptionsConstants.PILOT_TM_NIGHTWALKER);
    }

    /**
     * Sets the source (TRO or other) of the creation of this entity.
     *
     * @param source The source name
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
     * Retrieves the quirks object for entity. DO NOT USE this to check boolean options, as it will not
     * check game options for quirks. Use {@link #hasQuirk(String)} instead!
     *
     * @return This unit's quirks, independently of the game's settings
     * @see #hasQuirk(String)
     */
    public synchronized Quirks getQuirks() {
        return quirks;
    }

    public boolean hasQuirk(String name) {
        if ((game != null) && !game.getOptions().booleanOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS)) {
            return false;
        }
        return quirks.booleanOption(name);
    }

    public PartialRepairs getPartialRepairs() {
        return partReps;
    }

    public void clearPartialRepairs() {
        for (Enumeration<IOptionGroup> i = partReps.getGroups(); i.hasMoreElements();) {
            IOptionGroup group = i.nextElement();
            for (Enumeration<IOption> j = group.getOptions(); j.hasMoreElements();) {
                IOption option = j.nextElement();
                option.clearValue();
            }
        }

    }

    /**
     * count all the quirks for this unit, positive and negative
     */
    public int countQuirks() {
        if ((null == game)
                || !game.getOptions().booleanOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS)) {
            return 0;
        }

        return quirks.count();
    }

    public int countWeaponQuirks() {
        int count = 0;

        if ((null == game)
                || !game.getOptions().booleanOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS)) {
            return count;
        }

        for (Mounted<?> m : getEquipment()) {
            count += m.countQuirks();
        }
        return count;
    }

    public int countPartialRepairs() {
        if ((null == game) ||
                !game.getOptions().booleanOption(OptionsConstants.ADVANCED_STRATOPS_PARTIALREPAIRS)) {
            return 0;
        }

        return partReps.count();
    }

    /**
     * count the quirks for this unit, for a given group name
     */
    public int countQuirks(String grpKey) {
        if ((null == game)
                || !game.getOptions().booleanOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS)) {
            return 0;
        }

        return quirks.count(grpKey);
    }

    /**
     * Returns a string of all the quirk "codes" for this entity, using sep as
     * the separator
     */
    public String getQuirkList(String sep) {
        if ((null == game)
                || !game.getOptions().booleanOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS)) {
            return "";
        }

        return quirks.getOptionList(sep);
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

    @Override
    public boolean isAirborne() {
        return (!isDestroyed())
                && (getAltitude() > 0)
                || (getMovementMode() == EntityMovementMode.AERODYNE)
                || (getMovementMode() == EntityMovementMode.SPHEROID);
    }

    public boolean isSpaceborne() {
        // for now if you are in space, you are spaceborne, but this will become
        // more complicated when
        // we start adding multiple maps to the same game and so I should try to
        // replace most calls to
        // game.getBoard().inSpace() with this one
        return game != null && game.getBoard().inSpace();
    }

    /**
     * is the unit flying Nap of the Earth? (i.e. one elevation above ground)
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
            final var gOpts = getGame().getOptions();
            if (!getOwner().isBot() && gOpts.booleanOption(OptionsConstants.BASE_SET_PLAYER_DEPLOYMENT_TO_PLAYER_0)) {
                return game.getPlayer(0).getStartingPos();
            } else {
                return getOwner().getStartingPos();
            }
        }
        return startingPos;
    }

    public void setStartingPos(int i) {
        startingPos = i;
    }

    @Override
    public int getAltitude() {
        if (isSpaceborne()) {
            // This happens so often that reporting it blows up the log; ideally it
            // shouldn't happen at all in space
            // logger.warn("Altitude retrieved for a spaceborne unit!");
            return 0;
        } else {
            return altitude;
        }
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
     * Gets the initial BV of a unit.
     *
     * Useful for comparisons with the current BV.
     *
     * @return The initial BV of a unit.
     */
    public int getInitialBV() {
        return initialBV;
    }

    /**
     * Sets the initial BV for a unit.
     *
     * Called when the game is initialized.
     *
     * @param bv The initial BV of a unit.
     */
    public void setInitialBV(int bv) {
        initialBV = bv;
    }

    /**
     * @return an int array of the number of bombs of each type based on the
     * current bomblist
     */
    public int[] getBombLoadout() {
        return getBombLoadout(false);
    }

    public int[] getBombLoadout(boolean internalOnly) {
        int[] loadout = new int[BombType.B_NUM];
        for (BombMounted bomb : getBombs()) {
            if ((bomb.getUsableShotsLeft() > 0)) {
                // Either count all bombs, or just internal bombs
                if (internalOnly && !bomb.isInternalBomb()) {
                    continue;
                } else {
                    int type = bomb.getType().getBombType();
                    loadout[type] = loadout[type] + 1;
                }
            }
        }
        return loadout;
    }

    public int[] getInternalBombLoadout() {
        return getBombLoadout(true);
    }

    public int[] getExternalBombLoadout() {
        int[] allBombs = getBombLoadout();
        int[] intBombs = getBombLoadout(true);
        for (int i = 0; i < allBombs.length; i++) {
            allBombs[i] -= intBombs[i];
        }
        return allBombs;
    }

    @Override
    public Map<Integer, Coords> getSecondaryPositions() {
        return secondaryPositions;
    }

    /**
     * Checks to see if this unit has a functional Blue Shield Particle Field
     * Damper that is turned on
     *
     * @return <code>true</code> if the entity has a working, switched on blue
     *         field <code>false</code> otherwise
     */
    public boolean hasActiveBlueShield() {
        if (!isShutDown()) {
            for (MiscMounted m : getMisc()) {
                EquipmentType type = m.getType();
                if (type.hasFlag(MiscType.F_BLUE_SHIELD) && m.curMode().equals("On")) {
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
        return isAirborne() && !(isAero());
    }

    /**
     * @return True if this unit has working stealth armor (not patchwork armor).
     */
    public boolean hasStealth() {
        // only non-patchwork stealth actually works as stealth
        return !hasPatchworkArmor() && ((getArmorType(1) == EquipmentType.T_ARMOR_STEALTH)
                || (getArmorType(1) == EquipmentType.T_ARMOR_STEALTH_VEHICLE));
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
        if (hasEngine() && (getEngine().isFusion() || (getEngine().getEngineType() == Engine.FISSION))) {
            return 0.0;
        }
        // Small support vehicles do not need power amplifiers.
        if (getWeightClass() == EntityWeightClass.WEIGHT_SMALL_SUPPORT) {
            return 0.0;
        }
        // Otherwise, we need to iterate over our weapons, find out which of them
        // require amplification, and keep a running weight total of those.
        double total = 0.0;
        for (WeaponMounted m : getWeaponList()) {
            WeaponType wt = m.getType();
            if ((wt.hasFlag(WeaponType.F_LASER) && (wt.getAmmoType() == AmmoType.T_NA))
                    || wt.hasFlag(WeaponType.F_PPC)
                    || wt.hasFlag(WeaponType.F_PLASMA)
                    || wt.hasFlag(WeaponType.F_PLASMA_MFUK)
                    || (wt.hasFlag(WeaponType.F_FLAMER) && (wt.getAmmoType() == AmmoType.T_NA))) {
                total += m.getTonnage();
            }
            if ((m.getLinkedBy() != null) && (m.getLinkedBy().getType() instanceof MiscType)
                    && m.getLinkedBy().getType().hasFlag(MiscType.F_PPC_CAPACITOR)) {
                total += m.getLinkedBy().getTonnage();
            }
        }
        for (MiscMounted m : getMisc()) {
            if (m.getType().hasFlag(MiscType.F_CLUB) && m.getType().hasSubType(MiscType.S_SPOT_WELDER)) {
                total += m.getTonnage();
            }
        }
        // Finally use that total to compute and return the actual power
        // amplifier weight.
        return RoundWeight.nextHalfTon(total / 10.0);
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
        // also, each 'has' loops through all equipment. inefficient to do it 3 times
        // Nova CEWS is quirky and handled apart from the other C3
        int extraBV = 0;
        if (game != null) {
            int totalForceBV = 0;
            double multiplier = 0.05;
            if ((hasC3MM() && (calculateFreeC3MNodes() < 2))
                        || (hasC3M() && (calculateFreeC3Nodes() < 3))
                        || (hasC3S() && (c3Master > NONE))
                        || ((hasC3i() || hasNavalC3()) && (calculateFreeC3Nodes() < 5))) {
                totalForceBV += baseBV;
                for (Entity entity : game.getC3NetworkMembers(this)) {
                    if (!equals(entity) && onSameC3NetworkAs(entity)) {
                        totalForceBV += entity.calculateBattleValue(true, true);
                    }
                }
                if (hasBoostedC3()) {
                    multiplier = 0.07;
                }

            } else if (hasNovaCEWS()) { //Nova CEWS applies 5% to every mek with Nova on the team {
                for (Entity entity : game.getEntitiesVector()) {
                    if (!equals(entity) && entity.hasNovaCEWS() && !(entity.owner.isEnemyOf(this.owner))) {
                        totalForceBV += entity.calculateBattleValue(true, true);
                    }
                }
                if (totalForceBV > 0) { //But only if there's at least one other mek with Nova CEWS
                    totalForceBV += baseBV;
                }
            }
            extraBV += (int) Math.round(totalForceBV * multiplier);
        }
        return extraBV;
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

    /**
     * Tracks whether a WiGE lifted off this turn (or a LAM hovered). Needed to
     * track state
     * in case movement is continued from an interruption, so that the unit does not
     * have a minimum
     * movement for the turn.
     *
     * @return whether a WiGE lifted off during this turn's movement
     */
    public boolean wigeLiftoffHover() {
        return wigeLiftoffHover;
    }

    public void setWigeLiftoffHover(boolean lifted) {
        wigeLiftoffHover = lifted;
    }

    public void setHardenedArmorDamaged(HitData hit, boolean damaged) {
        hardenedArmorDamaged[hit.getLocation()] = damaged;
    }

    /**
     * do we have a half-hit hardened armor point in the location struck by this?
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
     * @return True if this unit has patchwork armor.
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
     * <p>
     * This method should <strong>only</strong> be used during serialization.
     *
     * @return the <code>int</code> number of turns MASC has been used.
     */
    public int getMASCTurns() {
        return nMASCLevel;
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
     * Get the number of turns Supercharger has been used continuously.
     * <p>
     * This method should <strong>only</strong> be used during serialization.
     * </p>
     *
     * @return the <code>int</code> number of turns Supercharger has been used.
     */
    public int getSuperchargerTurns() {
        return nSuperchargerLevel;
    }

    /**
     * Determine if Supercharger has been used this turn.
     *
     * @return <code>true</code> if Supercharger has been used.
     */
    public boolean isSuperchargerUsed() {
        return usedSupercharger;
    }

    protected void incrementMASCAndSuperchargerLevels() {
        // If MASC was used last turn, increment the counter,
        // otherwise decrement. Then, clear the counter
        if (usedMASC) {
            nMASCLevel++;
            bMASCWentUp = true;
        } else {
            nMASCLevel = Math.max(0, nMASCLevel - 1);
            if (bMASCWentUp) {
                nMASCLevel = Math.max(0, nMASCLevel - 1);
                bMASCWentUp = false;
            }
        }
        usedMASC = false;

        // If Supercharger was used last turn, increment the counter,
        // otherwise decrement. Then, clear the counter
        if (usedSupercharger) {
            nSuperchargerLevel++;
            bSuperchargerWentUp = true;
        } else {
            nSuperchargerLevel = Math.max(0, nSuperchargerLevel - 1);
            if (bSuperchargerWentUp) {
                nSuperchargerLevel = Math.max(0, nSuperchargerLevel - 1);
                bSuperchargerWentUp = false;
            }
        }
        usedSupercharger = false;
    }

    /**
     * Set whether MASC has been used.
     * <p>
     * This method should <strong>only</strong> be used during deserialization.
     *
     * @param used The <code>boolean</code> whether MASC has been used.
     */
    public void setMASCUsed(boolean used) {
        usedMASC = used;
    }

    public int getMASCTarget() {
        return getMASCorSuperchargerTarget(nMASCLevel);
    }

    /**
     * Set whether Supercharger has been used.
     * <p>
     * This method should <strong>only</strong> be used during deserialization.
     * </p>
     *
     * @param used The <code>boolean</code> whether Supercharger has been used.
     */
    public void setSuperchargerUsed(boolean used) {
        usedSupercharger = used;
    }

    public int getSuperchargerTarget() {
        // uses same TNs as MASC
        return getMASCorSuperchargerTarget(nSuperchargerLevel);
    }

    /** @return Target number taking into account game options */
    private int getMASCorSuperchargerTarget(int nLevel) {
        if ((game != null) && game.getOptions().booleanOption(OptionsConstants.ADVANCED_ALTERNATE_MASC_ENHANCED)) {
            return ALTERNATE_MASC_FAILURE_ENHANCED[nLevel];
        } else if (game != null && game.getOptions().booleanOption(OptionsConstants.ADVANCED_ALTERNATE_MASC)) {
            return ALTERNATE_MASC_FAILURE[nLevel];
        } else {
            return MASC_FAILURE[nLevel];
        }
    }

    /**
     * This function cheks for masc failure.
     *
     * @param md         the movement path.
     * @param vDesc      the description off the masc failure. used as output.
     * @param vCriticals contains tuple of intiger and critical slot. used as
     *                   output.
     * @return true if there was a masc failure.
     */
    public boolean checkForMASCFailure(MovePath md, Vector<Report> vDesc,
            HashMap<Integer, List<CriticalSlot>> vCriticals) {
        if (md.hasActiveMASC()) {
            boolean bFailure;

            // If usedMASC is already set, then we've already checked MASC
            // this turn. If we succeded before, return false.
            // If we failed before, the MASC was destroyed, and we wouldn't
            // have gotten here (hasActiveMASC would return false)
            if (!usedMASC) {
                MiscMounted masc = getMASC();
                bFailure = doMASCOrSuperchargerFailureCheckFor(masc, vDesc, vCriticals);
                usedMASC = true;
                return bFailure;
            }
        }
        return false;
    }

    /**
     * This function cheks for Supercharger failure.
     *
     * @param md         the movement path.
     * @param vDesc      the description off the Supercharger failure. used as
     *                   output.
     * @param vCriticals contains tuple of intiger and critical slot. used as
     *                   output.
     * @return true if there was a Supercharger failure.
     */
    public boolean checkForSuperchargerFailure(MovePath md, Vector<Report> vDesc,
            HashMap<Integer, List<CriticalSlot>> vCriticals) {
        if (md.hasActiveSupercharger()) {
            boolean bFailure;

            // If usedSupercharger is already set, then we've already checked Supercharger
            // this turn. If we succeded before, return false.
            // If we failed before, the Supercharger was destroyed, and we wouldn't
            // have gotten here (hasActiveSupercharger would return false)
            if (!usedSupercharger) {
                MiscMounted superCharger = getSuperCharger();
                bFailure = doMASCOrSuperchargerFailureCheckFor(superCharger, vDesc, vCriticals);
                usedSupercharger = true;
                return bFailure;
            }
        }
        return false;
    }

    /**
     * check one masc system for failure
     *
     * @param masc The Mounted for MASC
     * @param vDesc A reports vector to add reports to
     * @param vCriticals A map to write crit results to
     * @return True if there is a MASC failure, false otherwise
     */
    private boolean doMASCOrSuperchargerFailureCheckFor(MiscMounted masc, Vector<Report> vDesc,
            HashMap<Integer, List<CriticalSlot>> vCriticals) {
        if ((masc != null) && masc.curMode().equals("Armed")) {
            boolean bFailure = false;
            Roll diceRoll = Compute.rollD6(2);
            int rollValue = diceRoll.getIntValue();
            String rollCalc = String.valueOf(rollValue);
            boolean isSupercharger = masc.getType().hasSubType(MiscType.S_SUPERCHARGER);
            // WHY is this -1 here?
            if (isSupercharger
                    && (((this instanceof Mek) && ((Mek) this).isIndustrial())
                            || (this instanceof SupportTank) || (this instanceof SupportVTOL))) {
                rollValue -= 1;
                rollCalc = rollValue + " [" + diceRoll.getIntValue() + " - 1]";
            }

            if (isSupercharger) {
                usedSupercharger = true;
            } else {
                usedMASC = true;
            }
            Report r = new Report(2365);
            r.subject = getId();
            r.addDesc(this);
            r.add(masc.getName());
            vDesc.addElement(r);
            r = new Report(2370);
            r.subject = getId();
            r.indent();
            r.add(isSupercharger ? getSuperchargerTarget() : getMASCTarget());
            r.addDataWithTooltip(rollCalc, diceRoll.getReport());

            if ((!isSupercharger && (rollValue < getMASCTarget()))
                    || (isSupercharger && (rollValue < getSuperchargerTarget()))) {
                // uh oh
                bFailure = true;
                r.choose(false);
                vDesc.addElement(r);

                if (isSupercharger) {
                    // do the damage - engine crits
                    int hits = 0;
                    Roll diceRoll2 = Compute.rollD6(2);
                    r = new Report(6310);
                    r.subject = getId();
                    r.add(diceRoll2);
                    r.newlines = 0;
                    vDesc.addElement(r);
                    if (diceRoll2.getIntValue() <= 7) {
                        // no effect
                        r = new Report(6005);
                        r.subject = getId();
                        r.newlines = 0;
                        vDesc.addElement(r);
                    } else if ((diceRoll2.getIntValue() == 8) || (diceRoll2.getIntValue() == 9)) {
                        hits = 1;
                        r = new Report(6315);
                        r.subject = getId();
                        r.newlines = 0;
                        vDesc.addElement(r);
                    } else if ((diceRoll2.getIntValue() == 10) || (diceRoll2.getIntValue() == 11)) {
                        hits = 2;
                        r = new Report(6320);
                        r.subject = getId();
                        r.newlines = 0;
                        vDesc.addElement(r);
                    } else if (diceRoll2.getIntValue() == 12) {
                        hits = 3;
                        r = new Report(6325);
                        r.subject = getId();
                        r.newlines = 0;
                        vDesc.addElement(r);
                    }
                    if (this instanceof Mek) {
                        vCriticals.put(Mek.LOC_CT, new LinkedList<>());
                        for (int i = 0; (i < 12) && (hits > 0); i++) {
                            CriticalSlot cs = getCritical(Mek.LOC_CT, i);
                            if ((cs.getType() == CriticalSlot.TYPE_SYSTEM)
                                    && (cs.getIndex() == Mek.SYSTEM_ENGINE) && cs.isHittable()) {
                                vCriticals.get(Mek.LOC_CT).add(cs);
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
                        vCriticals.put(Tank.LOC_BODY, new LinkedList<>());
                        vCriticals.put(-1, new LinkedList<>());
                        if (tank instanceof VTOL) {
                            vCriticals.put(VTOL.LOC_ROTOR, new LinkedList<>());
                        }
                        for (int i = 0; i < hits; i++) {
                            if (tank instanceof VTOL) {
                                if (vtolStabilizerHit) {
                                    vCriticals.get(Tank.LOC_BODY).add(new CriticalSlot(
                                            CriticalSlot.TYPE_SYSTEM, Tank.CRIT_ENGINE));
                                } else {
                                    vCriticals.get(VTOL.LOC_ROTOR).add(new CriticalSlot(
                                            CriticalSlot.TYPE_SYSTEM, VTOL.CRIT_FLIGHT_STABILIZER));
                                    vtolStabilizerHit = true;
                                }
                            } else {
                                if (heavyMovementDamage) {
                                    vCriticals.get(Tank.LOC_BODY).add(new CriticalSlot(
                                            CriticalSlot.TYPE_SYSTEM, Tank.CRIT_ENGINE));
                                } else if (moderateMovementDamage) {
                                    // HACK: we abuse the criticalslot item to
                                    // signify the calling function to deal
                                    // movement damage
                                    vCriticals.get(-1).add(new CriticalSlot(CriticalSlot.TYPE_SYSTEM, 3));
                                    heavyMovementDamage = true;
                                } else if (minorMovementDamage) {
                                    // HACK: we abuse the criticalslot item to
                                    // signify the calling function to deal
                                    // movement damage
                                    vCriticals.get(-1).add(new CriticalSlot(CriticalSlot.TYPE_SYSTEM, 2));
                                    moderateMovementDamage = true;
                                } else {
                                    // HACK: we abuse the criticalslot item to
                                    // signify the calling function to deal
                                    // movement damage
                                    vCriticals.get(-1).add(new CriticalSlot(CriticalSlot.TYPE_SYSTEM, 1));
                                    minorMovementDamage = true;
                                }
                            }
                        }
                    }

                } else {
                    // do the damage.
                    // random crit on each leg, but MASC is not destroyed
                    for (int loc = 0; loc < locations(); loc++) {
                        if (locationIsLeg(loc) && (getHittableCriticals(loc) > 0)) {
                            CriticalSlot slot;
                            do {
                                int slotIndex = Compute.randomInt(getNumberOfCriticals(loc));
                                slot = getCritical(loc, slotIndex);
                            } while ((slot == null) || !slot.isHittable());
                            vCriticals.put(loc, new LinkedList<>());
                            vCriticals.get(loc).add(slot);
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
    public MiscMounted getMASC() {
        for (MiscMounted m : getMisc()) {
            MiscType mtype = m.getType();
            if (mtype.hasFlag(MiscType.F_MASC) && m.isReady()
                    && !mtype.hasSubType(MiscType.S_SUPERCHARGER)
                    && !mtype.hasSubType(MiscType.S_JETBOOSTER)) {
                return m;
            }
        }
        return null;
    }

    /**
     * @return An operable Supercharger if there is one on this unit.
     */
    public MiscMounted getSuperCharger() {
        for (MiscMounted m : getMisc()) {
            MiscType mtype = m.getType();
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
        for (MiscMounted mounted : getMisc()) {
            if (!mounted.isDestroyed()) {
                continue;
            }
            if (mounted.getType().hasFlag(MiscType.F_JUMP_JET)) {
                jumpJets++;
            }
        }
        return jumpJets;
    }

    public abstract String getLocationDamage(int loc);

    /**
     * @return true if this unit can reasonably escape from the board. It can be used to determine
     * whether some non-destroyed units should be considered possible salvage.
     */
    public boolean canEscape() {
        if (null == getCrew()) {
            return false;
        }
        // if the crew is unconscious, dead, or ejected, no escape
        if (getCrew().isUnconscious()
                || getCrew().isDead()
                || (getCrew().isEjected() && !(this instanceof EjectedCrew))) {
            return false;
        }

        // TODO: what else? If its permaneantly immobilized or shutdown it can't escape
        // TODO: should stalled and stuck be here?
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

    public void setNC3NextUUIDAsString(int pos, String c3id) {
        NC3UUIDs[pos] = c3id;
    }

    public String getNC3NextUUIDAsString(int pos) {
        return NC3UUIDs[pos];
    }

    public int getFreeNC3UUID() {
        int pos = 0;
        while (NC3UUIDs[pos] != null) {
            pos++;
            if (pos >= MAX_C3i_NODES) {
                return -1;
            }
        }
        return pos;
    }

    public boolean isC3CompanyCommander() {
        return getC3MasterId() == id;
    }

    public boolean isC3IndependentMaster() {
        return !isC3CompanyCommander();
    }

    /**
     * @return True if a unit was physically struck (punch, kick, DFA, etc).
     */
    public boolean wasStruck() {
        return struck;
    }

    /**
     * Sets if a unit was physically struck (punch, kick, DFA, etc).
     */
    public void setStruck(boolean struck) {
        this.struck = struck;
    }

    /**
     * @return True if a unit has fallen in the current phase.
     */
    public boolean hasFallen() {
        return fell;
    }

    /**
     * Sets if the unit has fallen in the current phase.
     *
     * @param fell True if the unit should be considered to have fallen this phase
     */
    public void setFallen(boolean fell) {
        this.fell = fell;
    }

    /**
     * @return An alternative cost that will be added to the MekSummaryCache - at the moment it
     * is primarily used to rework infantry costs for MekHQ, but it could be applied to other unit
     * types as well - defaults to -1, so there is no confusion.
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
            Entity transport = game.getEntityFromAllSources(getTransportId());
            return (transport != null) && transport.isDestroyed();
        }
        return false;
    }

    // Deal with per entity camo
    public Camouflage getCamouflage() {
        return camouflage;
    }

    public Camouflage getCamouflageOrElseOwners() {
        return getCamouflageOrElse(getOwner().getCamouflage());
    }

    public Camouflage getCamouflageOrElse(final Camouflage camouflage) {
        return getCamouflageOrElse(camouflage, true);
    }

    public Camouflage getCamouflageOrElse(final Camouflage camouflage, final boolean checkForces) {
        // if we're checking forces and the game exists, then initialize the force.
        // Leave it as null otherwise.
        final Force force = checkForces && (game != null) ? game.getForces().getForce(this) : null;

        // if the camouflage is default and the force is null, return the current
        // entity-specific camouflage
        // if the force is not null, return the force specific camouflage
        // if the camouflage is not default, just return the current entity-specific
        // camouflage
        return getCamouflage().hasDefaultCategory()
                ? ((force == null) ? camouflage : force.getCamouflageOrElse(game, camouflage))
                : getCamouflage();
    }

    public void setCamouflage(Camouflage camouflage) {
        this.camouflage = camouflage;
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

    public boolean getSelfDestructedThisTurn() {
        return selfDestructedThisTurn;
    }

    public void setSelfDestructedThisTurn(boolean tf) {
        selfDestructedThisTurn = tf;
    }

    public void setIsJumpingNow(boolean jumped) {
        isJumpingNow = jumped;
    }

    public boolean getIsJumpingNow() {
        return isJumpingNow;
    }

    public void setConvertingNow(boolean converting) {
        convertingNow = converting;
    }

    public boolean isConvertingNow() {
        return convertingNow;
    }

    public int getConversionMode() {
        return conversionMode;
    }

    /**
     * Sets the unit to be in the given mode.
     *
     * @param mode The new mode
     */
    public void setConversionMode(int mode) {
        conversionMode = mode;
    }

    /**
     * Entities that can convert movement modes (LAMs, QuadVees) report the next mode to assume
     * when a convert movement command is processed. This provides a set order for cycling through
     * available modes.
     *
     * @param afterMode The movement mode to convert from.
     * @return The next movement mode in the sequence.
     */
    public EntityMovementMode nextConversionMode(EntityMovementMode afterMode) {
        return movementMode;
    }

    /**
     * Sets the movement mode to the next in the conversion sequence for QuadVees,
     * LAMs, and Meks
     * with tracks. In most cases this switches between two available modes, but
     * LAMs that start
     * the turn in AirMek mode have three available.
     */
    public void toggleConversionMode() {
        setMovementMode(nextConversionMode(movementMode));
    }

    /**
     * Only applicable to Meks, but here for convenience. Meks that are already
     * prone, or
     * QuadVees and LAMs in non-leg mode are not subject to PSRs for falling. Note
     * that PSRs
     * are sometimes required for other reasons.
     *
     * @param gyroLegDamage Whether the potential fall is due to damage to gyro or
     *                      leg actuators,
     *                      in which case Meks using tracks are not subject to
     *                      falls.
     * @return Whether the <code>Entity</code> is required to make PSRs to avoid
     *         falling.
     */
    public boolean canFall(boolean gyroLegDamage) {
        return false;
    }

    /**
     * Only applicable to Meks, but here for convenience. Meks that are already
     * prone, or
     * QuadVees and LAMs in fighter mode are not subject to PSRs for falling. Note
     * that PSRs
     * are sometimes required for other reasons.
     *
     * @return Whether the <code>Entity</code> is required to make PSRs to avoid
     *         falling.
     */
    public boolean canFall() {
        return canFall(false);
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
        ArmorType armor = ArmorType.forEntity(this);
        if (armor.hasFlag(MiscType.F_SUPPORT_VEE_BAR_ARMOR)) {
            return (int) Math.floor(armorTonnage / armor.getSVWeightPerPoint(getArmorTechRating()));
        } else {
            double armorPerTon = ArmorType.forEntity(this).getPointsPerTon(this);
            return (int) Math.floor(armorPerTon * armorTonnage);
        }
    }

    public void loadDefaultCustomWeaponOrder() {
        WeaponOrderHandler.WeaponOrder weapOrder = WeaponOrderHandler.getWeaponOrder(
                getChassis(), getModel());

        if (weapOrder != null) {
            setWeaponSortOrder(weapOrder.orderType);
            setCustomWeaponOrder(weapOrder.customWeaponOrderMap);
        }
    }

    public void loadQuirks(List<QuirkEntry> quirks) {
        // Load all the unit's quirks.
        for (QuirkEntry quirkEntry : quirks) {
            // If the quirk doesn't have a location, then it is a unit quirk, not a weapon
            // quirk.
            if (StringUtility.isNullOrBlank(quirkEntry.getLocation())) {
                // Activate the unit quirk.
                if (getQuirks().getOption(quirkEntry.getQuirk()) == null) {
                    logger.warn(String.format("%s failed for %s %s - Invalid quirk!",
                            quirkEntry, getChassis(), getModel()));
                    continue;
                }
                getQuirks().getOption(quirkEntry.getQuirk()).setValue(true);
            } else {
                assignWeaponQuirk(quirkEntry);
            }
        }
    }

    /**
     * Returns the Mounted that is referred to by the quirkEntry (which must be a
     * weapon quirk).
     * //FIXME: This is very specialized code that is only needed because we need to
     * identify a weapon
     * in the blk/mtf file. It might be better to write the weaponquirk directly to
     * the weapon,
     * removing the need to find it. Note the override in ProtoMek that doesnt use
     * crit slots.
     * For meks, this is challenge as the specific weapon in a location must
     * still be addressed.
     *
     * @param quirkEntry The weapon quirk entry
     * @return The Mounted at the specified location
     */
    protected Mounted<?> getEquipmentForWeaponQuirk(QuirkEntry quirkEntry) {
        // Get the weapon in the indicated location and slot.
        CriticalSlot cs = getCritical(getLocationFromAbbr(quirkEntry.getLocation()), quirkEntry.getSlot());
        if (cs != null) {
            return cs.getMount();
        } else {
            logger.warn(String.format("%s failed for %s %s - Critical slot (%s-%s) did not load!",
                    quirkEntry, getChassis(), getModel(), quirkEntry.getLocation(), quirkEntry.getSlot()));
            return null;
        }
    }

    protected void assignWeaponQuirk(QuirkEntry quirkEntry) {
        Mounted<?> m = getEquipmentForWeaponQuirk(quirkEntry);
        if (m == null) {
            logger.warn(String.format("%s failed for %s %s - Critical slot (%s-%s) is empty!",
                    quirkEntry, getChassis(), getModel(), quirkEntry.getLocation(), quirkEntry.getSlot()));
            return;
        }

        // Make sure this is a weapon.
        if (!(m.getType() instanceof WeaponType) && !(m.getType().hasFlag(MiscType.F_CLUB))) {
            logger.warn(String.format("%s failed for %s %s - %s is not a weapon!",
                    quirkEntry, getChassis(), getModel(), m.getName()));
            return;
        }

        // Make sure it is the weapon we expect.
        boolean matchFound = false;
        Enumeration<String> typeNames = m.getType().getNames();
        while (typeNames.hasMoreElements()) {
            String typeName = typeNames.nextElement();
            if (typeName.equals(quirkEntry.getWeaponName())) {
                matchFound = true;
                break;
            }
        }

        if (!matchFound) {
            logger.warn(String.format("%s failed for %s %s - %s != %s",
                    quirkEntry, getChassis(), getModel(), m.getType().getName(), quirkEntry.getWeaponName()));
            return;
        }

        // Activate the weapon quirk.
        if (m.getQuirks().getOption(quirkEntry.getQuirk()) == null) {
            logger.warn(String.format("%s failed for %s %s - Invalid quirk!",
                    quirkEntry, getChassis(), getModel()));
            return;
        }
        m.getQuirks().getOption(quirkEntry.getQuirk()).setValue(true);
    }

    @Override
    public void newPhase(GamePhase phase) {
        for (Mounted<?> m : getEquipment()) {
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
        setIsJumpingNow(false);
    }

    /** @return True if this entity is completely submerged. */
    public boolean isUnderwater() {
        if (!hasOccupiedHex()) {
            return false;
        }
        Hex occupiedHex = game.getBoard().getHex(getPosition());
        return occupiedHex.containsTerrain(Terrains.WATER) && (relHeight() < occupiedHex.getLevel());
    }

    /**
     * @return True if this entity has a non-null occupied hex, i.e. it has a game,
     *         a position, a board etc.
     */
    public boolean hasOccupiedHex() {
        return !isOffBoard() && (getPosition() != null) && (game != null) && (game.getBoard() != null)
                && (game.getBoard().getHex(getPosition()) != null);
    }

    public int getTechLevelYear() {
        if (game != null) {
            return game.getOptions().intOption(OptionsConstants.ALLOWED_YEAR);
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
     * Convenience method that checks whether a bit is set in the entity type field.
     *
     * @param flag An ETYPE_* value
     * @return true if getEntityType() has the flag set
     */
    public boolean hasETypeFlag(long flag) {
        return (getEntityType() & flag) == flag;
    }

    /**
     * Given an Entity type, return the name of the major class it belongs to
     * (eg: Mek, Aero, Tank, Infantry).
     *
     * @param typeId The type Id to get a major name for
     * @return The major class name for the given type id
     */
    public static String getEntityMajorTypeName(long typeId) {
        if ((typeId & ETYPE_MEK) == ETYPE_MEK) {
            return "Mek";
        } else if ((typeId & ETYPE_AERO) == ETYPE_AERO) {
            return "Aero";
        } else if ((typeId & ETYPE_VTOL) == ETYPE_VTOL) {
            return "VTOL";
        } else if ((typeId & ETYPE_TANK) == ETYPE_TANK) {
            return "Tank";
        } else if ((typeId & ETYPE_INFANTRY) == ETYPE_INFANTRY) {
            return "Infantry";
        } else if ((typeId & ETYPE_PROTOMEK) == ETYPE_PROTOMEK) {
            return "ProtoMek";
        } else {
            return "Unknown";
        }
    }

    /**
     * Returns the specific entity type name for the given type id (eg: Biped Mek, Conventional Fighter, VTOL).
     *
     * @param typeId The ETYPE_* type
     * @return A String representation of the given type
     */
    public static String getEntityTypeName(long typeId) {
        if ((typeId & ETYPE_LAND_AIR_MEK) == ETYPE_LAND_AIR_MEK) {
            return "Landair Mek";
        } else if ((typeId & ETYPE_BIPED_MEK) == ETYPE_BIPED_MEK) {
            return "Biped Mek";
        } else if ((typeId & ETYPE_QUAD_MEK) == ETYPE_QUAD_MEK) {
            return "Quad Mek";
        } else if ((typeId & ETYPE_TRIPOD_MEK) == ETYPE_TRIPOD_MEK) {
            return "Tripod Mek";
        } else if ((typeId & ETYPE_ARMLESS_MEK) == ETYPE_ARMLESS_MEK) {
            return "Armless Mek";
        } else if ((typeId & ETYPE_MEK) == ETYPE_MEK) {
            return "Mek";
        } else if ((typeId & ETYPE_WARSHIP) == ETYPE_WARSHIP) {
            return "WarShip";
        } else if ((typeId & ETYPE_SPACE_STATION) == ETYPE_SPACE_STATION) {
            return "Space Station";
        } else if ((typeId & ETYPE_JUMPSHIP) == ETYPE_JUMPSHIP) {
            return "JumpShip";
        } else if ((typeId & ETYPE_FIXED_WING_SUPPORT) == ETYPE_FIXED_WING_SUPPORT) {
            return "Fixed Wing Support";
        } else if ((typeId & ETYPE_CONV_FIGHTER) == ETYPE_CONV_FIGHTER) {
            return "Conventional Fighter";
        } else if ((typeId & ETYPE_FIGHTER_SQUADRON) == ETYPE_FIGHTER_SQUADRON) {
            return "Fighter Squadron";
        } else if ((typeId & ETYPE_DROPSHIP) == ETYPE_DROPSHIP) {
            return "DropShip";
        } else if ((typeId & ETYPE_SMALL_CRAFT) == ETYPE_SMALL_CRAFT) {
            return "Small Craft";
        } else if ((typeId & ETYPE_TELEMISSILE) == ETYPE_TELEMISSILE) {
            return "Telemissile";
        } else if ((typeId & ETYPE_AERO) == ETYPE_AERO) {
            return "Aerospace fighter";
        } else if ((typeId & ETYPE_BATTLEARMOR) == ETYPE_BATTLEARMOR) {
            return "Battlearmor";
        } else if ((typeId & ETYPE_MEKWARRIOR) == ETYPE_MEKWARRIOR) {
            return "MekWarrior";
        } else if ((typeId & ETYPE_PROTOMEK) == ETYPE_PROTOMEK) {
            return "ProtoMek";
        } else if ((typeId & ETYPE_INFANTRY) == ETYPE_INFANTRY) {
            return "Infantry";
        } else if ((typeId & ETYPE_GUN_EMPLACEMENT) == ETYPE_GUN_EMPLACEMENT) {
            return "Gun Emplacement";
        } else if ((typeId & ETYPE_SUPER_HEAVY_TANK) == ETYPE_SUPER_HEAVY_TANK) {
            return "Superheavy Tank";
        } else if ((typeId & ETYPE_LARGE_SUPPORT_TANK) == ETYPE_LARGE_SUPPORT_TANK) {
            return "Large Support Tank";
        } else if ((typeId & ETYPE_SUPPORT_TANK) == ETYPE_SUPPORT_TANK) {
            return "Support Tank";
        } else if ((typeId & ETYPE_SUPPORT_VTOL) == ETYPE_SUPPORT_VTOL) {
            return "Support VTOL";
        } else if ((typeId & ETYPE_VTOL) == ETYPE_VTOL) {
            return "VTOL";
        } else if ((typeId & ETYPE_TANK) == ETYPE_TANK) {
            return "Tank";
        } else {
            return "Unknown";
        }
    }

    public void damageSystem(int type, int slot, int hits) {
        for (int loc = 0; loc < locations(); loc++) {
            hits -= damageSystem(type, slot, loc, hits);
        }
    }

    public int damageSystem(int type, int slot, int loc, int hits) {
        int nhits = 0;
        for (int i = 0; i < getNumberOfCriticals(loc); i++) {
            CriticalSlot cs = getCritical(loc, i);
            // ignore empty & system slots
            if ((cs == null) || (cs.getType() != type)) {
                continue;
            }
            Mounted<?> m = null;
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
        return nhits;
    }

    // Most units cannot eject.
    // ToDo Look up ejection rules for ASF.
    public boolean isEjectionPossible() {
        return false;
    }

    public int getAllowedPhysicalAttacks() {
        if ((null != crew) && hasAbility(OptionsConstants.PILOT_MELEE_MASTER)) {
            return 2;
        }
        return 1;
    }

    /**
     * The max weapons range of this entity, taking into account whether we're on an air/space map,
     * using extreme range. Assumes target is not airborne if we are on a ground map.
     *
     * @return The maximum weapon range of weapons on this unit
     */
    public int getMaxWeaponRange() {
        return getMaxWeaponRange(false);
    }

    /**
     * The max weapons range of this entity, taking into account whether we're on an air/space map,
     * using extreme range, and whether the target is airborne.
     *
     * @param targetIsAirborne True to assume the target is airborne
     * @return The maximum weapon range of weapons on this unit
     */
    public int getMaxWeaponRange(boolean targetIsAirborne) {
        // Aeros on the ground map must shoot along their flight path, giving
        // them effectively 0 range
        if (isAirborneAeroOnGroundMap() && !targetIsAirborne) {
            return 0;
        }

        int maxRange = 0;
        if ((ETYPE_MEK == getEntityType())
                || (ETYPE_INFANTRY == getEntityType())
                || (ETYPE_PROTOMEK == getEntityType())) {
            // account for physical attacks.
            maxRange = 1;
        }

        for (WeaponMounted weapon : getWeaponList()) {
            if (!weapon.isReady()) {
                continue;
            }

            WeaponType type = weapon.getType();
            int range;

            if (isAirborne()) {
                int rangeMultiplier = type.isCapital() ? 2 : 1;
                rangeMultiplier *= isAirborneAeroOnGroundMap() ? 8 : 1;

                range = WeaponType.AIRBORNE_WEAPON_RANGES[type.getMaxRange(weapon)] * rangeMultiplier;
            } else {
                range = (game != null && game.getOptions().booleanOption(
                        OptionsConstants.ADVCOMBAT_TACOPS_RANGE) ? type.getExtremeRange()
                                : type.getLongRange());
            }

            if (range > maxRange) {
                maxRange = range;
            }
        }
        return maxRange;
    }

    public int getOptimalRange() {
        Map<Integer, Integer> rangeDamages = new TreeMap<>();
        if ((ETYPE_MEK == getEntityType())
            || (ETYPE_INFANTRY == getEntityType())
            || (ETYPE_PROTOMEK == getEntityType())) {
            // account for physical attacks.
            rangeDamages.put(1,
                PunchAttackAction.getDamageFor(this, PunchAttackAction.BOTH, false, false)
                + KickAttackAction.getDamageFor(this, KickAttackAction.BOTH, false));
        }

        for (WeaponMounted weapon : getWeaponList()) {
            if (!weapon.isReady()) {
                continue;
            }

            WeaponType type = weapon.getType();

            if (isAirborne()) {
                int rangeMultiplier = type.isCapital() ? 2 : 1;
                rangeMultiplier *= isAirborneAeroOnGroundMap() ? 8 : 1;

                var range = WeaponType.AIRBORNE_WEAPON_RANGES[type.getMaxRange(weapon)] * rangeMultiplier;
                rangeDamages.put(range, rangeDamages.getOrDefault(range, 0) + type.getDamage(range));
            } else {
                var range = type.getShortRange();
                rangeDamages.put(range, rangeDamages.getOrDefault(range, 0) + type.getDamage(range));
            }
        }

        var keys = new ArrayList<>(rangeDamages.keySet());
        Collections.sort(keys);

        if (keys.isEmpty()) {
            return -1;
        }

        if (keys.size() == 1) {
            return keys.get(0);
        }

        var totalDamage = rangeDamages.values().stream().mapToInt(Integer::intValue).sum();
        var halfDamage = totalDamage / 2;
        var accumulator = 0;
        for (int i = rangeDamages.size() -1; i > 0; i--) {
            accumulator += rangeDamages.get(keys.get(i));
            if (accumulator > halfDamage / 2) {
                return keys.get(i);
            }
        }

        return keys.get(keys.size() -1);
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

    /**
     * Sets the number of rounds that the entity is affected by an ASEW missile
     *
     * @param turns - integer specifying the number of end phases that the effects
     *              last through
     *              Technically, about 1.5 turns elapse per the rules for ASEW
     *              missiles in TO
     */
    public void setASEWAffected(int turns) {
        asewAffectedTurns = turns;
    }

    /**
     * @return the number of rounds that the entity is affected by an ASEW missile
     */
    public int getASEWAffected() {
        return asewAffectedTurns;
    }

    public boolean hasActivatedRadicalHS() {
        for (MiscMounted m : getMisc()) {
            if (m.getType().hasFlag(MiscType.F_RADICAL_HEATSINK)
                    && m.curMode().equals("On")) {
                return true;
            }
        }
        return false;
    }

    public void deactivateRadicalHS() {
        for (MiscMounted m : getMisc()) {
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

    public void addAttackedByThisTurn(int entityId) {
        attackedByThisTurn.add(entityId);
    }

    public void addGroundAttackedByThisTurn(int entityId) {
        groundAttackedByThisTurn.add(entityId);
    }

    public void clearAttackedByThisTurn() {
        attackedByThisTurn.clear();
        if (groundAttackedByThisTurn != null) {
            groundAttackedByThisTurn.clear();
        }
    }

    public Collection<Integer> getAttackedByThisTurn() {
        return new HashSet<>(attackedByThisTurn);
    }

    public Collection<Integer> getGroundAttackedByThisTurn() {
        return new HashSet<>(groundAttackedByThisTurn);
    }

    public WeaponSortOrder getWeaponSortOrder() {
        return (weaponSortOrder == null) ? WeaponSortOrder.DEFAULT : weaponSortOrder;
    }

    public void setWeaponSortOrder(WeaponSortOrder weaponSortOrder) {
        if (weaponSortOrder != this.weaponSortOrder) {
            setWeapOrderChanged(true);
        }
        // If sort mode is custom, and the custom order is null, create it
        // and make the order the same as default (based on eqId)
        if (weaponSortOrder.isCustom() && (customWeaponOrder == null)) {
            customWeaponOrder = new HashMap<>();
            for (WeaponMounted weapon : weaponList) {
                int eqId = getEquipmentNum(weapon);
                customWeaponOrder.put(eqId, eqId);
            }
        }
        this.weaponSortOrder = weaponSortOrder;
    }

    public Map<Integer, Integer> getCustomWeaponOrder() {
        return customWeaponOrder;
    }

    public void setCustomWeaponOrder(Map<Integer, Integer> customWeapOrder) {
        customWeaponOrder = customWeapOrder;
    }

    public int getCustomWeaponOrder(WeaponMounted weapon) {
        int eqId = getEquipmentNum(weapon);
        if (customWeaponOrder == null) {
            return eqId;
        }
        Integer order = customWeaponOrder.get(eqId);
        return Objects.requireNonNullElse(order, -1);
    }

    public void setCustomWeaponOrder(WeaponMounted weapon, int order) {
        setWeapOrderChanged(true);
        int eqId = getEquipmentNum(weapon);
        if (eqId == -1) {
            return;
        }
        customWeaponOrder.put(eqId, order);
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
     * @return Whether the unit uses primitive or retrotech construction rules
     */
    public boolean isPrimitive() {
        return false;
    }

    public int getStructuralTechRating() {
        return structuralTechRating;
    }

    public void setStructuralTechRating(int structuralTechRating) {
        this.structuralTechRating = structuralTechRating;
    }

    /**
     * @return the base engine value for support vehicles, see TM pg 120.
     *         Non-support vehicle
     *         entities will return 0.
     */
    public double getBaseEngineValue() {
        return 0;
    }

    /**
     * @return the base chassis value for support vehicles, see TM pg 120.
     *         Non-support vehicle
     *         entities will return 0.
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

    /**
     * Used by omni support vehicles to track the weight of fire control systems.
     * This limits the tonnage that can be devoted to weapons in pods.
     *
     * @return The fixed weight of fire control systems.
     */
    public double getBaseChassisFireConWeight() {
        return baseChassisFireConWeight;
    }

    /**
     * Used by omni support vehicles to set the weight of fixed fire control systems
     * in the base chassis.
     *
     * @param weight The weight of fixed fire control systems.
     */
    public void setBaseChassisFireConWeight(double weight) {
        baseChassisFireConWeight = weight;
    }

    /**
     * Units with construction data that varies by year (such as engine and control
     * system weight
     * for some primitive aerospace units) require tracking the original build year
     * separately
     * from the intro year for the model to account for refits that don't affect the
     * core components.
     *
     * @return The year to use for core component construction data.
     */
    public int getOriginalBuildYear() {
        if (originalBuildYear < 0) {
            return year;
        }
        return originalBuildYear;
    }

    public void setOriginalBuildYear(int year) {
        originalBuildYear = year;
    }

    /**
     * This method (and getActiveSubEntities()) is meant for groups of entities
     * handled as a
     * singular one. Examples include fighter squadrons on space maps or lances in
     * BattleForce
     * game modes.
     * <p>
     * To check if a given entity consists of multiple sub-entities, use
     *
     * <pre>
     * if (entity.getSubEntities().isPresent()) {
     *     ...
     * }
     * </pre>
     *
     * To iterate over entities (if present), use:
     *
     * <pre>
     * entity.getSubEntities().ifPresent(entities -&gt; entities.forEach(
     *     subEntity -&gt; {
     *         ...
     *     });
     * </pre>
     *
     * @return an optional collection of sub-entities, if this entity is considered
     *         a grouping of them.
     */
    public List<Entity> getSubEntities() {
        return Collections.emptyList();
    }

    /**
     * A list of all active sub-entities. In most cases, this is simply an empty
     * list.
     *
     * @return an optional collection of sub-entities, if this entity is considered
     *         a grouping of them,
     *         pre-filtered to only contain active (non-destroyed and non-doomed)
     *         entities.
     */
    public List<Entity> getActiveSubEntities() {
        return Collections.emptyList();
    }

    /**
     * Used to determine the draw priority of different Entity subclasses.
     * This allows different unit types to always be draw above/below other
     * types.
     */
    public int getSpriteDrawPriority() {
        return 0;
    }

    /**
     * Entities that use different sprites for different modes should override this
     *
     * @return a code identifying the mode, or an empty string for the default
     *         sprite
     */
    public String getTilesetModeString() {
        return "";
    }

    // Tractors and trailers, tugs, etc

    /**
     * Used to determine if this vehicle can be towed by a tractor
     */
    public boolean isTrailer() {
        return false;
    }

    /**
     * Used to determine if this vehicle can be the engine/tractor
     * for a bunch of trailers
     */
    public boolean isTractor() {
        return false;
    }

    /**
     * @return A Set of Coords that need to be checked for entities that can be towed. This accounts for
     * the hexes occupied by each entity in the 'train', plus hexes in front of or behind each trailer hitch.
     */
    public Set<Coords> getHitchLocations() {
        Set<Coords> trailerPos = new HashSet<>();
        // First, set up a list of all the entities in this train
        ArrayList<Entity> thisTrain = new ArrayList<>();
        thisTrain.add(this);
        for (int id : getAllTowedUnits()) {
            Entity trailer = game.getEntity(id);
            thisTrain.add(trailer);
        }
        // Check each Entity in the train for working hitches. When found, add the hex
        // that Entity is in and the hex the hitch faces.
        for (Entity e : thisTrain) {
            for (Transporter t : e.getTransports()) {
                if ((t instanceof TankTrailerHitch) && (t.getUnused() > 0)) {
                    trailerPos.add(e.getPosition());
                    int dir = e.getFacing();
                    if (((TankTrailerHitch) t).getRearMounted()) {
                        dir = (dir + 3) % 6;
                    }
                    trailerPos.add(e.getPosition().translated(dir));
                }
            }
        }
        return trailerPos;
    }

    /**
     * Matches up a trailer hitch transporter with its Id #
     *
     * @param bayNumber - the index of the transporter we're trying to find.
     * @return the trailerhitch corresponding to the passed-in value
     */
    public TankTrailerHitch getHitchById(int bayNumber) {
        Transporter transporter = transports.get(bayNumber);
        if (transporter instanceof TankTrailerHitch) {
            return (TankTrailerHitch) transporter;
        }
        return null;
    }

    /**
     * Finds the trailer hitch transporter that is carrying a given entityId
     * Hitches move around in Transports on loading a saved game
     *
     * @param id - the id of the loaded Entity we're trying to find
     * @return the trailerhitch corresponding to the passed-in value
     */
    public TankTrailerHitch getHitchCarrying(int id) {
        for (Transporter next : transports) {
            if (next instanceof TankTrailerHitch) {
                if (next.getLoadedUnits().contains(game.getEntity(id))) {
                    return (TankTrailerHitch) next;
                }
            }
        }
        return null;
    }

    /**
     * Determines if this vehicle is currently able to tow designated trailer.
     *
     * @param trailerId - the ID of the <code>Entity</code> to be towed.
     * @return <code>true</code> if the trailer can be towed, <code>false</code>
     *         otherwise.
     */
    public boolean canTow(int trailerId) {
        Entity trailer = game.getEntity(trailerId);

        // Null check
        if (trailer == null) {
            return false;
        }

        // Shouldn't be using this method if Trailer isn't a trailer
        if (!trailer.isTrailer()) {
            return false;
        }

        // Can't tow if we aren't a tractor
        if (!isTractor()) {
            return false;
        }

        // If this entity is in a transport bay, it can't tow another
        if (getTransportId() != Entity.NONE) {
            return false;
        }

        // If Trailer moved or is already being towed, discard it
        if (!trailer.isLoadableThisTurn()) {
            return false;
        }

        // Can't tow yourself, either.
        if (trailer.equals(this)) {
            return false;
        }

        // one can only tow friendly units!
        if (trailer.isEnemyOf(this)) {
            return false;
        }

        // Can't tow if hitch and trailer aren't at the same elevation
        if (trailer.getElevation() != getElevation()) {
            return false;
        }

        // If none of the above happen, assume that we can't tow the trailer...
        boolean result = false;

        // First, set up a list of all the entities in this train
        ArrayList<Entity> thisTrain = new ArrayList<>();
        thisTrain.add(this);
        for (int id : getAllTowedUnits()) {
            Entity tr = game.getEntity(id);
            thisTrain.add(tr);
        }

        // Add up the weight of all carried trailers. A tractor can tow a total tonnage
        // equal to its own.
        double tractorWeight = getWeight();
        double trailerWeight = 0;
        // Add up what the tractor's already towing
        for (int id : getAllTowedUnits()) {
            Entity tr = game.getEntity(id);
            trailerWeight += tr.getWeight();
        }
        if (trailerWeight + trailer.getWeight() > tractorWeight) {
            return false;
        }

        // Next, look for an empty hitch somewhere in the train
        boolean hitchFound = false;
        for (Entity e : thisTrain) {
            // Quit looking if we've already found a valid hitch
            if (hitchFound) {
                break;
            }
            for (Transporter t : e.getTransports()) {
                if (t.canTow(trailer)) {
                    result = true;
                    hitchFound = true;
                    // stop looking
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Used with MoveStep.TOW to find and update the correct entity when adding it
     * to a train
     */
    private int isTowing = Entity.NONE;

    /**
     * @return the entity to be towed
     */
    public int getTowing() {
        return isTowing;
    }

    /**
     * Change the towed status of this entity
     *
     * @param id - the ID of the entity being towed
     */
    public void setTowing(int id) {
        isTowing = id;
    }

    /**
     * The id of the powered tractor towing the whole train
     * this entity is part of. This will often be the same
     * entity as towedBy
     */
    private int tractor = Entity.NONE;

    /**
     * @return the tractor towing the train this entity is part of
     */
    public int getTractor() {
        return tractor;
    }

    /**
     * Sets the tractor towing the train this entity is part of
     *
     * @param id - id of the tractor towing this train
     */
    public void setTractor(int id) {
        tractor = id;
    }

    /**
     * The ID of the entity directly towing this one
     * Used to find and set the correct Transporter
     */
    private int towedBy = Entity.NONE;

    /**
     * @return the Entity that is directly towing this one
     */
    public int getTowedBy() {
        return towedBy;
    }

    /**
     * Sets the Entity that is directly towing this one
     *
     * @param id - the id of the Entity towing this trailer
     */
    public void setTowedBy(int id) {
        towedBy = id;
    }

    /**
     * @return the entities towed behind this entity
     */
    public List<Integer> getConnectedUnits() {
        return Collections.unmodifiableList(connectedUnits);
    }

    /**
     * Attaches a trailer to this train
     *
     * @param id - if of the entity to be added to this train
     */
    public void towUnit(int id) {
        Entity towed = game.getEntity(id);
        // Add this trailer to the connected list for all trailers already in this train
        for (int tr : getAllTowedUnits()) {
            Entity trailer = game.getEntity(tr);
            trailer.connectedUnits.add(id);
        }
        addTowedUnit(id);
        towed.setTractor(getId());
        // Now, find the transporter and the actual towing entity (trailer or tractor)
        Entity towingEnt = game.getEntity(towed.towedBy);
        if (towingEnt != null) {
            Transporter hitch = towingEnt.getHitchById(towed.getTargetBay());
            if (hitch != null) {
                hitch.load(towed);
            }
        }
    }

    /**
     * Detaches an entity from this entity's towing mechanism
     * also detaches all trailers behind this one from the whole
     * train
     *
     * @param id - the id of entity to be detached
     */
    public void disconnectUnit(int id) {
        Entity towed = game.getEntity(id);
        Entity tractor = game.getEntity(towed.getTractor());
        // Remove the designated trailer from the tractor's carried units
        removeTowedUnit(id);
        // Now, find and empty the transporter on the actual towing entity (trailer or
        // tractor)
        Entity towingEnt = game.getEntity(towed.getTowedBy());
        towingEnt.connectedUnits.clear();
        if (towingEnt != null) {
            Transporter hitch = towingEnt.getHitchCarrying(id);
            if (hitch != null) {
                hitch.unload(towed);
            }
        }
        // If there are other trailers behind the one being dropped, disconnect all of
        // them
        // from the tractor and from each other, so they can be picked up again later
        for (int i : towed.getConnectedUnits()) {
            Entity trailer = game.getEntity(i);
            trailer.setTractor(Entity.NONE);
            tractor.removeTowedUnit(i);
            towingEnt = game.getEntity(trailer.getTowedBy());
            if (towingEnt != null) {
                Transporter hitch = towingEnt.getHitchCarrying(i);
                if (hitch != null) {
                    hitch.unload(trailer);
                }
            }
            trailer.setTowedBy(Entity.NONE);
            trailer.connectedUnits.clear();
        }
        // Update these last, or we get concurrency issues
        towed.setTractor(Entity.NONE);
        towed.setTowedBy(Entity.NONE);
        towed.setTowing(Entity.NONE);
        towed.connectedUnits.clear();
    }

    /**
     * A list of all the entity IDs towed by this entity,
     * including those connected to other towed trailers
     *
     * Use this for the tractor/engine/tug
     */
    private final List<Integer> isTractorFor = new ArrayList<>();

    /**
     * @return a list of all entities towed behind this tractor.
     */
    public List<Integer> getAllTowedUnits() {
        return Collections.unmodifiableList(isTractorFor);
    }

    /**
     * Adds an entity to this tractor's train
     */
    public void addTowedUnit(int id) {
        isTractorFor.add(id);
    }

    /**
     * Removes an entity from this tractor's train
     */
    public void removeTowedUnit(int id) {
        isTractorFor.remove((Integer) id);
        if (getTowing() == id) {
            setTowing(Entity.NONE);
        }
    }

    /**
     * Get a <code>List</code> of the trailers currently loaded into this payload.
     *
     * @return A <code>List</code> of loaded <code>Entity</code> units. This
     *         list will never be <code>null</code>, but it may be empty. The
     *         returned <code>List</code> is independent from the under- lying
     *         data structure; modifying one does not affect the other.
     *
     *         This will only return loaded trailers
     */
    public List<Entity> getLoadedTrailers() {
        List<Entity> result = new ArrayList<>();

        // Walk through this entity's transport components;
        // add any trailers we find there
        for (Transporter next : transports) {
            for (Entity e : next.getLoadedUnits()) {
                if (e.isTrailer()) {
                    result.add(e);
                }
            }
        }

        // Now do the same for any additional trailers being carried by those trailers
        for (int id : getAllTowedUnits()) {
            Entity trailer = game.getEntity(id);
            for (Transporter next : trailer.transports) {
                for (Entity e : next.getLoadedUnits()) {
                    if (e.isTrailer()) {
                        result.add(e);
                    }
                }
            }
        }

        // Return the list.
        return result;
    }

    /**
     * Determine if a connected tractor/trailer prevents a weapon in the given
     * location
     * from firing.
     *
     * @param loc             - the <code>int</code> location attempting to fire.
     * @param secondaryFacing - the <code>int</code> direction the turret is facing
     *                        if the weapon is mounted there.
     * @param isRear          - a <code>boolean</code> value stating if the given
     *                        location
     *                        is rear facing; if <code>false</code>, the location is
     *                        front
     *                        facing.
     * @return <code>true</code> if a tractor/trailer unit is in the way,
     *         <code>false</code> if the weapon can fire.
     */
    public boolean isWeaponBlockedByTowing(int loc, int secondaryFacing, boolean isRear) {
        // Per TW p205, assume our trailer is being towed from the front.
        if (getTowedBy() != Entity.NONE) {
            if (loc == Tank.LOC_FRONT ||
                    ((loc == Tank.LOC_TURRET
                            || loc == Tank.LOC_TURRET_2
                            || loc == SuperHeavyTank.LOC_TURRET
                            || loc == SuperHeavyTank.LOC_TURRET_2)
                            && (secondaryFacing == getFacing()))) {
                return true;
            }
        }
        if (!getAllTowedUnits().isEmpty() || !getConnectedUnits().isEmpty()) {
            // If we're towing something, check for a front or rear hitch
            Entity towed = null;
            if (!getAllTowedUnits().isEmpty()) {
                towed = game.getEntity(getAllTowedUnits().get(0));
            } else {
                towed = game.getEntity(getConnectedUnits().get(0));
            }
            if (towed == null) {
                // shouldn't happen, but just in case
                return false;
            }
            TankTrailerHitch hitch = getHitchCarrying(towed.getId());
            if (hitch != null) {
                if ((hitch.getRearMounted()) && loc == Tank.LOC_REAR
                        || isRear
                        || loc == SuperHeavyTank.LOC_REAR ||
                        ((loc == Tank.LOC_TURRET
                                || loc == Tank.LOC_TURRET_2
                                || loc == SuperHeavyTank.LOC_TURRET
                                || loc == SuperHeavyTank.LOC_TURRET_2)
                                && (secondaryFacing == ((getFacing() + 3) % 6)))) {
                    return true;
                } else if (!hitch.getRearMounted() && (loc == Tank.LOC_FRONT ||
                        ((loc == Tank.LOC_TURRET
                                || loc == Tank.LOC_TURRET_2
                                || loc == SuperHeavyTank.LOC_TURRET
                                || loc == SuperHeavyTank.LOC_TURRET_2)
                                && (secondaryFacing == getFacing())))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * determine if an entity has an ability that is identified by its presence or
     * absence only.
     * The entity may gain this ability from different places, not exclusively the
     * crew.
     *
     * @param name - name of the ability as recorded in the options
     * @return true if the entity has this ability from some source
     */
    public boolean hasAbility(String name) {
        if (null != getCrew()) {
            return getCrew().getOptions().booleanOption(name);
        }
        // TODO: look for the ability at the player level
        return false;
    }

    /**
     * determine if an entity has an ability at a given level.
     * The entity may gain this ability from different places, not exclusively the
     * crew.
     *
     * @param name   - name of the ability as recorded in the optionsme
     * @param choice - A string indicating the given level being asked about
     * @return true if the entity has this ability at the given choice from some
     *         source
     */
    public boolean hasAbility(String name, String choice) {
        if (null != getCrew()) {
            return getCrew().getOptions().stringOption(name).equals(choice);
        }
        return false;
    }

    public int modifyPhysicalDamageForMeleeSpecialist() {
        if (!hasAbility(OptionsConstants.PILOT_MELEE_SPECIALIST)) {
            return 0;
        }

        return 1;
    }

    // Getters and setters for sensor contacts and firing solutions. Currently, only
    // used in space combat

    /**
     * Retrieves the IDs of all entities that this entity has detected with sensors
     *
     * @return the contents of this entity's sensorContacts set
     */
    public Set<Integer> getSensorContacts() {
        return sensorContacts;
    }

    /**
     * Checks the sensorContacts set for a specific target's ID number
     *
     * @param targetId the ID number of the target entity to check for
     * @return true if the entity's sensorContacts set contains the passed-in target
     *         ID
     */
    public boolean hasSensorContactFor(int targetId) {
        return sensorContacts.contains(targetId);
    }

    /**
     * Adds the specified target entity's ID to this entity's sensorContacts
     *
     * @param targetId the ID number of the target entity to add
     */
    public void addSensorContact(int targetId) {
        sensorContacts.add(targetId);
    }

    /**
     * Removes the specified target entity's ID from this entity's sensorContacts
     *
     * @param targetIds the ID number of the target entity to remove
     */
    public void removeSensorContact(Collection<Integer> targetIds) {
        sensorContacts.removeAll(targetIds);
    }

    /**
     * Empties this entity's sensorContacts
     * Used when it dies or moves offboard
     */
    public void clearSensorContacts() {
        sensorContacts.clear();
    }

    /**
     * Retrieves the IDs of all entities that this entity has established firing
     * solutions on
     *
     * @return the contents of this entity's firingSolutions set
     */
    public Set<Integer> getFiringSolutions() {
        return firingSolutions;
    }

    /**
     * Checks the firingSolutions set for a specific target's ID number
     *
     * @param targetId the ID number of the target entity to check for
     * @return true if the entity's firingSolutions set contains the passed-in
     *         target ID
     */
    public boolean hasFiringSolutionFor(int targetId) {
        return firingSolutions.contains(targetId);
    }

    /**
     * Adds the specified target entity's ID to this entity's firingSolutions
     *
     * @param targetId the ID number of the target entity to add
     */
    public void addFiringSolution(int targetId) {
        firingSolutions.add(targetId);
    }

    /**
     * Removes the specified target entity's ID from this entity's firingSolutions
     *
     * @param targetIds the ID number of the target entity to remove
     */
    public void removeFiringSolution(Collection<Integer> targetIds) {
        firingSolutions.removeAll(targetIds);
    }

    /**
     * Empties this entity's firingSolutions
     * Used when it dies or moves offboard
     */
    public void clearFiringSolutions() {
        firingSolutions.clear();
    }

    /**
     * Indicate that an off-board artillery attack by this entity has been observed
     * by a particular team
     */
    public void addOffBoardObserver(int teamID) {
        offBoardShotObservers.add(teamID);
    }

    /**
     * Has the given team observed an off-board artillery attack by this entity?
     */
    public boolean isOffBoardObserved(int teamID) {
        return offBoardShotObservers.contains(teamID);
    }

    @Override
    public String getForceString() {
        return forceString;
    }

    @Override
    public void setForceString(String f) {
        forceString = MMXMLUtility.escape(f);
    }

    @Override
    public int getForceId() {
        return forceId;
    }

    @Override
    public void setForceId(int newId) {
        forceId = newId;
    }

    public void setBloodStalkerTarget(int value) {
        bloodStalkerTarget = value;
    }

    public int getStartingOffset() {
        return getStartingOffset(true);
    }

    public int getStartingOffset(boolean inheritFromOwner) {
        // if we are given permission to use the owner's settings
        // and have specified entity-specific settings, use the owner's settings
        if (inheritFromOwner && (startingPos == Board.START_NONE)) {
            final var gOpts = getGame().getOptions();
            if (!getOwner().isBot() && gOpts.booleanOption(OptionsConstants.BASE_SET_PLAYER_DEPLOYMENT_TO_PLAYER_0)) {
                return game.getPlayer(0).getStartOffset();
            } else {
                return getOwner().getStartOffset();
            }
        }

        return startingOffset;
    }

    public void setStartingOffset(int startingOffset) {
        this.startingOffset = startingOffset;
    }

    public int getStartingWidth() {
        return getStartingWidth(true);
    }

    public int getStartingWidth(boolean inheritFromOwner) {
        // if we are given permission to use the owner's settings
        // and have specified entity-specific settings, use the owner's settings
        if (inheritFromOwner && (startingPos == Board.START_NONE)) {
            final var gOpts = getGame().getOptions();
            if (!getOwner().isBot() && gOpts.booleanOption(OptionsConstants.BASE_SET_PLAYER_DEPLOYMENT_TO_PLAYER_0)) {
                return game.getPlayer(0).getStartWidth();
            } else {
                return getOwner().getStartWidth();
            }
        }

        return startingWidth;
    }

    public void setStartingWidth(int startingWidth) {
        this.startingWidth = startingWidth;
    }

    public int getStartingAnyNWx() {
        return getStartingAnyNWx(true);
    }

    public int getStartingAnyNWx(boolean inheritFromOwner) {
        // if we are given permission to use the owner's settings
        // and have specified entity-specific settings, use the owner's settings
        if (inheritFromOwner && (startingPos == Board.START_NONE)) {
            final var gOpts = getGame().getOptions();
            if (!getOwner().isBot() && gOpts.booleanOption(OptionsConstants.BASE_SET_PLAYER_DEPLOYMENT_TO_PLAYER_0)) {
                return game.getPlayer(0).getStartingAnyNWx();
            } else {
                return getOwner().getStartingAnyNWx();
            }
        }

        return startingAnyNWx;
    }

    public void setStartingAnyNWx(int i) {
        startingAnyNWx = i;
    }

    public int getStartingAnyNWy() {
        return getStartingAnyNWy(true);
    }

    public int getStartingAnyNWy(boolean inheritFromOwner) {
        // if we are given permission to use the owner's settings
        // and have specified entity-specific settings, use the owner's settings
        if (inheritFromOwner && (startingPos == Board.START_NONE)) {
            final var gOpts = getGame().getOptions();
            if (!getOwner().isBot() && gOpts.booleanOption(OptionsConstants.BASE_SET_PLAYER_DEPLOYMENT_TO_PLAYER_0)) {
                return game.getPlayer(0).getStartingAnyNWy();
            } else {
                return getOwner().getStartingAnyNWy();
            }
        }

        return startingAnyNWy;
    }

    public void setStartingAnyNWy(int i) {
        startingAnyNWy = i;
    }

    public int getStartingAnySEx() {
        return getStartingAnySEx(true);
    }

    public int getStartingAnySEx(boolean inheritFromOwner) {
        // if we are given permission to use the owner's settings
        // and have specified entity-specific settings, use the owner's settings
        if (inheritFromOwner && (startingPos == Board.START_NONE)) {
            final var gOpts = getGame().getOptions();
            if (!getOwner().isBot() && gOpts.booleanOption(OptionsConstants.BASE_SET_PLAYER_DEPLOYMENT_TO_PLAYER_0)) {
                return game.getPlayer(0).getStartingAnySEx();
            } else {
                return getOwner().getStartingAnySEx();
            }
        }

        return startingAnySEx;
    }

    public void setStartingAnySEx(int i) {
        startingAnySEx = i;
    }

    public int getStartingAnySEy() {
        return getStartingAnySEy(true);
    }

    public int getStartingAnySEy(boolean inheritFromOwner) {
        // if we are given permission to use the owner's settings
        // and have specified entity-specific settings, use the owner's settings
        if (inheritFromOwner && (startingPos == Board.START_NONE)) {
            final var gOpts = getGame().getOptions();
            if (!getOwner().isBot() && gOpts.booleanOption(OptionsConstants.BASE_SET_PLAYER_DEPLOYMENT_TO_PLAYER_0)) {
                return game.getPlayer(0).getStartingAnySEy();
            } else {
                return getOwner().getStartingAnySEy();
            }
        }

        return startingAnySEy;
    }

    public void setStartingAnySEy(int i) {
        startingAnySEy = i;
    }

    public int getBloodStalkerTarget() {
        return bloodStalkerTarget;
    }

    /**
     * Whether this entity can activate the "blood stalker" ability
     */
    public boolean canActivateBloodStalker() {
        return hasAbility(OptionsConstants.GUNNERY_BLOOD_STALKER) &&
                (getBloodStalkerTarget() == Entity.NONE);
    }

    public int braceLocation() {
        return braceLocation;
    }

    public void setBraceLocation(int location) {
        braceLocation = location;
    }

    @Override
    public boolean isBracing() {
        return braceLocation != Entity.LOC_NONE;
    }

    public boolean canBrace() {
        return false;
    }

    public int getBraceMPCost() {
        return Entity.LOC_NONE;
    }

    public List<Integer> getValidBraceLocations() {
        return Collections.emptyList();
    }

    /**
     * @return does this Mek have MASC, Supercharger or both?
     */
    public MPBoosters getMPBoosters() {
        return getMPBoosters(false);
    }

    /**
     * @return does this Mek have Armed MASC, Supercharger or both?
     */
    public MPBoosters getArmedMPBoosters() {
        return getMPBoosters(true);
    }

    /**
     * @return if this Mek has MASC, Supercharger or both?
     */
    public MPBoosters getMPBoosters(boolean onlyArmed) {
        boolean hasMASC = false;
        boolean hasSupercharger = false;
        for (MiscMounted m : getMisc()) {
            if (!m.isInoperable() && m.getType().hasFlag(MiscType.F_MASC)) {
                // Supercharger is a subtype of MASC in MiscType
                if (m.getType().hasSubType(MiscType.S_SUPERCHARGER)) {
                    hasSupercharger = !onlyArmed || m.curMode().equals("Armed");
                } else {
                    hasMASC = !onlyArmed || m.curMode().equals("Armed");
                }
            }

            if (hasMASC && hasSupercharger) {
                break;
            }
        }

        if (hasMASC && hasSupercharger) {
            return MPBoosters.MASC_AND_SUPERCHARGER;
        } else if (hasMASC) {
            return MPBoosters.MASC_ONLY;
        } else if (hasSupercharger) {
            return MPBoosters.SUPERCHARGER_ONLY;
        } else {
            return MPBoosters.NONE;
        }
    }

    /**
     * Returns this entity's MUL ID linking it to a unit on the
     * online Master Unit List. Use hasMulID() to check if the entity has a valid
     * MUL ID.
     */
    public int getMulId() {
        return mulId;
    }

    public void setMulId(int newId) {
        mulId = newId;
    }

    /**
     * Returns true when this entity has a useful MUL ID, meaning it
     * is a unit from an official source and can be linked to a unit on the
     * online Master Unit List.
     */
    public boolean hasMulId() {
        return mulId > 0;
    }

    /**
     * For clan units that get automatic Clan CASE, adds clan CASE in every location
     * that has
     * potentially explosive equipment (this includes uncharged PPC Capacitors).
     * <P>
     * As clan CASE does not need critical slots, this method does not perform
     * checks whether
     * other CASE types are already present on a location.
     * <P>
     * This method does nothing by default and must be overridden for unit types
     * that get Clan CASE.
     */
    public void addClanCase() {
    }

    /**
     * @return True for unit types that have an automatic external searchlight (Meks
     *         and Tanks).
     */
    public boolean getsAutoExternalSearchlight() {
        return false;
    }

    @Override
    public int getStrength() {
        return calculateBattleValue();
    }

    /** @return The persistent BV Calculator object for this entity. */
    public BVCalculator getBvCalculator() {
        if (bvCalculator == null) {
            bvCalculator = BVCalculator.getBVCalculator(this);
        }
        return bvCalculator;
    }

    /**
     * Returns the GameOptions of this Entity's game if it has one. If game is null
     * (happens in unit
     * construction in MML), a new (default) options object is returned. Prefer this
     * method over directly
     * calling game.getOptions() to avoid NPEs in places where game is null.
     *
     * @return The GameOptions of this Entity's game if it has one, otherwise a
     * default options object.
     */
    protected final IGameOptions gameOptions() {
        return game != null ? game.getOptions() : new GameOptions();
    }

    public void setUnitRole(UnitRole role) {
        this.role = role;
    }

    @Override
    public UnitRole getRole() {
        return (role == null) ? UnitRole.UNDETERMINED : role;
    }

    /**
     * Returns the slot in which the given mounted equipment is in its main
     * location. Returns -1 when
     * the mounted is not in a valid location or cannot be found.
     *
     * @param mounted the equipment to look for
     * @return the (first) slot number that holds the mounted or -1 if none can be
     *         found
     */
    public int slotNumber(Mounted<?> mounted) {
        int location = mounted.getLocation();
        if (location == Entity.LOC_NONE) {
            return -1;
        }
        for (int slot = 0; slot < getNumberOfCriticals(location); slot++) {
            CriticalSlot cs = getCritical(location, slot);
            if ((cs != null) && (cs.getType() == CriticalSlot.TYPE_EQUIPMENT)) {
                if ((cs.getMount() == mounted) ||
                        ((cs.getMount2() != null) && (cs.getMount2() == mounted))) {
                    return slot;
                }
            }
        }
        return -1;
    }

    @Override
    public @Nullable Image getFluffImage() {
        return fluff.getFluffImage();
    }

    @Override
    public String generalName() {
        return getChassis();
    }

    @Override
    public String specificName() {
        return getModel();
    }

    @Override
    public Image getIcon() {
        return icon.getImage();
    }

    /** Sets the embedded icon for this unit to the given base64 string. */
    public void setIcon(String icon64) {
        icon = new Base64Image(icon64);
    }

    /**
     * Returns true when this unit has an embedded icon, i.e. an icon stored in the
     * unit file rather than
     * found by the mekset. Currently, returns false when a mode-specific icon is
     * needed (LAMs/QVs)
     *
     * @return True when this unit has an embedded icon
     */
    public boolean hasEmbeddedIcon() {
        return !icon.isEmpty() && getTilesetModeString().isBlank();
    }

    /** @return The embedded icon of this unit in the full Base64Image form. */
    public Base64Image getBase64Icon() {
        return icon;
    }

    @Override
    public boolean countForStrengthSum() {
        return !isDestroyed() && !isTrapped() && !isPartOfFighterSquadron();
    }

    /**
     * @return True if the unit should use Edge based on the current options and
     *         assigned Edge points
     */
    public boolean shouldUseEdge(String option) {
        return (game.getOptions().booleanOption(OptionsConstants.EDGE)
                && getCrew() != null
                && getCrew().hasEdgeRemaining()
                && getCrew().getOptions().booleanOption(option));
    }

    public boolean hasFlotationHull() {
        return hasWorkingMisc(MiscType.F_FLOTATION_HULL);
    }

    @Override
    public boolean hasFleeZone() {
        return hasFleeZone;
    }

    @Override
    public HexArea getFleeZone() {
        return fleeZone;
    }

    /**
     * Sets the board area this unit may flee from. The area may be empty, in which case the unit may not flee. Also sets this unit to know
     * that it has a flee zone and the owning player should not be asked to provide this information.
     *
     * @param fleeZone The new flee zone
     */
    public void setFleeZone(HexArea fleeZone) {
        this.fleeZone = fleeZone;
        hasFleeZone = true;
    }

    /**
     * Resets the flee information this unit has. After calling this method, the unit will no longer consider to have its own flee area; the
     * game will refer to the unit's owner to see if it can flee from a hex.
     */
    public void removeFleeZone() {
        fleeZone = HexArea.EMPTY_AREA;
        hasFleeZone = false;
    }

    public void setInvalidSourceBuildReasons(List<InvalidSourceBuildReason> invalidSourceBuildReasons) {
        this.invalidSourceBuildReasons = invalidSourceBuildReasons;
    }

    public List<InvalidSourceBuildReason> getInvalidSourceBuildReasons() {
        return invalidSourceBuildReasons;
    }

    public boolean canonUnitWithInvalidBuild() {
        if (this.isCanon() && mulId > -1)
        {
            return !this.getInvalidSourceBuildReasons().isEmpty();
        }
        return false;
    }
}

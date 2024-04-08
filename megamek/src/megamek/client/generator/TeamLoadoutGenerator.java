package megamek.client.generator;

import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.dialog.AbstractUnitSelectorDialog;
import megamek.common.*;
import megamek.common.containers.MunitionTree;
import megamek.common.equipment.ArmorType;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryconditions.Light;
import org.apache.commons.collections.IteratorUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.*;

import static java.util.Map.entry;

/**
 * Notes: check out
 * - RATGenerator.java
 * - ForceDescriptor.java
 * for era-based search examples
 */

public class TeamLoadoutGenerator {

    public static final ArrayList<String> AP_MUNITIONS = new ArrayList<>(List.of(
            "Armor-Piercing", "Tandem-Charge"
    ));

    public static final ArrayList<String> FLAK_MUNITIONS = new ArrayList<>(List.of(
            "ADA", "Cluster", "Flak", "Precision"
    ));

    public static final ArrayList<String> ANTI_INF_MUNITIONS = new ArrayList<>(List.of(
            "Inferno", "Fragmentation", "Flechette", "Fuel-Air", "Anti-personnel", "Acid"
    ));

    public static final ArrayList<String> HEAT_MUNITIONS = new ArrayList<>(List.of(
            "Inferno", "Incendiary"
    ));

    public static final ArrayList<String> ILLUM_MUNITIONS = new ArrayList<>(List.of(
            "Illumination", "Tracer", "Inferno", "Incendiary", "Flare"
    ));

    public static final ArrayList<String> UTILITY_MUNITIONS = new ArrayList<>(List.of(
            "Illumination", "Smoke", "Mine Clearance", "Anti-TSM", "Laser Inhibiting",
            "Thunder", "FASCAM", "Thunder-Active", "Thunder-Augmented", "Thunder-Vibrabomb",
            "Thunder-Inferno", "Flare"
    ));

    public static final ArrayList<String> GUIDED_MUNITIONS = new ArrayList<>(List.of(
            "Semi-Guided", "Narc-capable", "Homing", "Copperhead"
    ));

    public static final ArrayList<String> AMMO_REDUCING_MUNITIONS = new ArrayList<>(List.of(
            "Acid", "Laser Inhibiting", "Follow The Leader", "Heat-Seeking", "Tandem-Charge",
            "Thunder-Active", "Thunder-Augmented", "Thunder-Vibrabomb", "Thunder-Inferno"
    ));

    public static final ArrayList<String> TYPE_LIST = new ArrayList<String>(List.of(
            "LRM", "SRM", "AC", "ATM", "Arrow IV", "Artillery", "Artillery Cannon",
            "Mek Mortar", "Narc", "Bomb"
    ));

    public static final Map<String, ArrayList<String>> TYPE_MAP =
         Map.ofEntries(
            entry("lrm", MunitionTree.LRM_MUNITION_NAMES),
            entry("srm", MunitionTree.SRM_MUNITION_NAMES),
            entry("ac", MunitionTree.AC_MUNITION_NAMES),
            entry("atm", MunitionTree.ATM_MUNITION_NAMES),
            entry("arrow iv", MunitionTree.ARROW_MUNITION_NAMES),
            entry("artillery", MunitionTree.ARTILLERY_MUNITION_NAMES),
            entry("artillery cannon", MunitionTree.MEK_MORTAR_MUNITION_NAMES),
            entry("mek mortar", MunitionTree.MEK_MORTAR_MUNITION_NAMES),
            entry("narc", MunitionTree.NARC_MUNITION_NAMES),
            entry("bomb", MunitionTree.BOMB_MUNITION_NAMES
        )
    );

    private static ClientGUI cg;
    private static Game game;

    protected GameOptions gameOptions = null;
    protected boolean enableYearLimits = false;
    protected int allowedYear = AbstractUnitSelectorDialog.ALLOWED_YEAR_ANY;
    protected int gameTechLevel = TechConstants.T_SIMPLE_INTRO;
    protected SimpleTechLevel legalLevel;
    protected boolean eraBasedTechLevel = false;
    protected boolean advAeroRules = false;
    protected boolean showExtinct = false;
    protected boolean trueRandom = false;
    protected String defaultBotMunitionsFile = null;

    public TeamLoadoutGenerator(ClientGUI gui){
        cg = gui;
        game = cg.getClient().getGame();
        gameOptions = game.getOptions();
        updateOptionValues();
    }

    public TeamLoadoutGenerator(ClientGUI gui, String defaultSettings){
        this(gui);
        this.defaultBotMunitionsFile = defaultSettings;
    }

    public void updateOptionValues() {
        gameOptions = cg.getClient().getGame().getOptions();
        enableYearLimits = true;
        allowedYear = gameOptions.intOption(OptionsConstants.ALLOWED_YEAR);
        gameTechLevel = TechConstants.getSimpleLevel(gameOptions.stringOption(OptionsConstants.ALLOWED_TECHLEVEL));
        legalLevel = SimpleTechLevel.getGameTechLevel(game);
        eraBasedTechLevel = gameOptions.booleanOption(OptionsConstants.ALLOWED_ERA_BASED);
        advAeroRules = gameOptions.booleanOption(OptionsConstants.ADVAERORULES_AERO_ARTILLERY_MUNITIONS);
        showExtinct = gameOptions.booleanOption((OptionsConstants.ALLOWED_SHOW_EXTINCT));
    }

    // See if
    public boolean checkLegality(AmmoType aType, String faction, String techBase, boolean mixedTech) {
        boolean legal = false;
        boolean clan = techBase.equals("CL");

        if (eraBasedTechLevel) {
            // Check if tech is legal to use in this game based on year, tech level, etc.
            legal = aType.isLegal(allowedYear, legalLevel, clan,
                    mixedTech, showExtinct);
            // Check if tech is widely available, or if the specific faction has access to it
            legal &= aType.isAvailableIn(allowedYear, showExtinct) || aType.isAvailableIn(allowedYear, clan, ITechnology.getCodeFromIOAbbr(faction));
        } else {
            // Basic year check only
            legal = aType.getStaticTechLevel().ordinal() <= legalLevel.ordinal();
        }

        return legal;
    }

    public void setTrueRandom(boolean value) {
        trueRandom = value;
    }

    public boolean getTrueRandom() {
        return trueRandom;
    }

    // Section: Check for various unit types, armor types, etc.
    private static long checkForBombers(ArrayList<Entity> el) {
        return el.stream().filter(Targetable::isBomber).count();
    }

    private static long checkForFliers(ArrayList<Entity> el) {
        return el.stream().filter(e -> e.isAero() || e.hasETypeFlag(Entity.ETYPE_VTOL)).count();
    }

    private static long checkForInfantry(ArrayList<Entity> el) {
        return el.stream().filter(e -> e.isInfantry() || e.isBattleArmor()).count();
    }

    private static long checkForVehicles(ArrayList<Entity> el) {
        return el.stream().filter(BTObject::isVehicle).count();
    }

    private static long checkForMeks(ArrayList<Entity> el) {
        return el.stream().filter(BTObject::isMek).count();
    }

    /**
     * Quick and dirty energy boat calc; useful for selecting Laser-Inhibiting Arrow and heat-based weapons
     * @param el
     * @return
     */
    private static long checkForEnergyBoats(ArrayList<Entity> el) {
        return el.stream().filter(e -> e.getAmmo().isEmpty()).count();
    }

    /**
     * "Missile Boat" defined here as any unit with half or more weapons dedicated to missiles
     * (This could probably be traded for a weight- or role-based check)
     * @param el
     * @return
     */
    private static long checkForMissileBoats(ArrayList<Entity> el) {
        return el.stream().filter(
                e -> e.getWeaponList().stream().filter(
                        w -> w.getName().toLowerCase().contains("lrm") ||
                        w.getName().toLowerCase().contains("srm") ||
                        w.getName().toLowerCase().contains("atm") ||
                        w.getName().toLowerCase().contains("mml") ||
                        w.getName().toLowerCase().contains("arrow")
                ).count() >= e.getWeaponList().size()
        ).count();
    }

    private static long checkForTAG(ArrayList<Entity> el) {
        return el.stream().filter(e -> e.hasTAG()).count();
    }

    private static long checkForNARC(ArrayList<Entity> el) {
        return el.stream().filter(
                e -> e.getAmmo().stream().anyMatch(
                        a -> ((AmmoType) a.getType()).getAmmoType() == AmmoType.T_NARC)
        ).count();
    }

    private static long checkForAdvancedArmor(ArrayList<Entity> el) {
        // Most units have a location 0
        return el.stream().filter(
                e -> e.getArmorType(0) == ArmorType.T_ARMOR_HARDENED ||
                e.getArmorType(0) == ArmorType.T_ARMOR_BALLISTIC_REINFORCED ||
                e.getArmorType(0) == ArmorType.T_ARMOR_REACTIVE ||
                e.getArmorType(0) == ArmorType.T_ARMOR_BA_REACTIVE ||
                e.getArmorType(0) == ArmorType.T_ARMOR_FERRO_LAMELLOR
        ).count();
    }

    private static long checkForReflectiveArmor(ArrayList<Entity> el) {
        return el.stream().filter(
                e -> e.getArmorType(0) == ArmorType.T_ARMOR_REFLECTIVE ||
                e.getArmorType(0) == ArmorType.T_ARMOR_BA_REFLECTIVE
        ).count();
    }

    private static long checkForFireproofArmor(ArrayList<Entity> el) {
        return el.stream().filter(
                e -> e.getArmorType(0) == ArmorType.T_ARMOR_BA_FIRE_RESIST
        ).count();
    }

    private static long checkForFastMovers(ArrayList<Entity> el) {
        return el.stream().filter(
                e -> e.getOriginalWalkMP() > 5
        ).count();
    }

    public ReconfigurationParameters generateParameters(Team t) {
        return generateParameters(game, gameOptions, t);
    }

    /**
     * Create the parameters that will determine how to configure ammo loadouts for this team
     * @param g
     * @param gOpts
     * @param t
     * @return ReconfigurationParameters with information about enemy and friendly forces
     */
    public static ReconfigurationParameters generateParameters(Game g, GameOptions gOpts, Team t) {
        ReconfigurationParameters rp = new ReconfigurationParameters();

        // Get our own side's numbers for comparison
        ArrayList<Entity> ownTeamEntities = (ArrayList<Entity>) IteratorUtils.toList(g.getTeamEntities(t));
        rp.friendlyCount = ownTeamEntities.size();

        // If our team can see other teams...
        if (!gOpts.booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND)) {
            // This team can see the opponent teams; set appropriate options
            for (Team et: g.getTeams()) {
                if (!et.isEnemyOf(t)) {
                    continue;
                }

                rp.enemyFactions.add(et.getFaction());
                ArrayList<Entity> etEntities = (ArrayList<Entity>) IteratorUtils.toList(g.getTeamEntities(et));
                rp.enemyCount += etEntities.size();
                rp.enemyFliers += checkForFliers(etEntities);
                rp.enemyBombers += checkForBombers(etEntities);
                rp.enemyInfantry += checkForInfantry(etEntities);
                rp.enemyVehicles += checkForVehicles(etEntities);
                rp.enemyMeks += checkForMeks(etEntities);
                rp.enemyEnergyBoats += checkForEnergyBoats(etEntities);
                // Enemy Missile Boats might be good to know for Retro Streak weighting
                rp.enemyMissileBoats += checkForMissileBoats(etEntities);
                rp.enemyAdvancedArmorCount += checkForAdvancedArmor(etEntities);
                rp.enemyReflectiveArmorCount += checkForReflectiveArmor(etEntities);
                rp.enemyFireproofArmorCount += checkForFireproofArmor(etEntities);
                rp.enemyFastMovers += checkForFastMovers(etEntities);
            }
        } else {
            // Assume we know _nothing_ about enemies if Double Blind is on.
            rp.enemiesVisible = false;

            // Estimate enemy count for ammo count purposes; may include observers.  The fog of war!
            rp.enemyCount = g.getEntitiesVector().size() - ownTeamEntities.size();
        }

        // Friendly force info
        rp.friendlyEnergyBoats = checkForEnergyBoats(ownTeamEntities);
        rp.friendlyMissileBoats = checkForMissileBoats(ownTeamEntities);
        rp.friendlyTAGs = checkForTAG(ownTeamEntities);
        rp.friendlyNARCs = checkForNARC(ownTeamEntities);

        // General parameters
        rp.darkEnvironment = g.getPlanetaryConditions().getLight().isDarkerThan(Light.DAY);

        return rp;
    }

    public MunitionTree generateMunitionTree(ReconfigurationParameters rp, Team t) {
        return generateMunitionTree(rp, t, "");
    }

    /**
     * Generate the list of desired ammo load-outs for this team.
     * TODO: implement generateDetailedMunitionTree with more complex breakdowns per unit type
     * NOTE: if subclassing this generator, should only need to override this method.
     * @param rc
     * @param t
     * @param defaultSettingsFile
     * @return generated MunitionTree with imperatives for each weapon type
     */
    public static MunitionTree generateMunitionTree(ReconfigurationParameters rp, Team t, String defaultSettingsFile) {

        MunitionTree mt = (defaultSettingsFile == null | defaultSettingsFile.isBlank()) ?
                new MunitionTree() : new MunitionTree(new File(defaultSettingsFile));

        // Based on various requirements from rp, set weights for some ammo types over others
        MunitionWeightCollection mwc = new MunitionWeightCollection();

        // Modify weights for parameters
        if (rp.darkEnvironment) {
            // Bump munitions that light stuff up
            mwc.increaseIllumMunitions();
        }

        // Adjust weights for enemy force composition
        if (rp.enemiesVisible) {
            // Flak: bump for any bombers, or fliers > 1/4th of enemy force
            if (rp.enemyBombers > 0.0) {
                mwc.increaseFlakMunitions();
            }
            if (rp.enemyFliers >= rp.enemyCount / 4.0) {
                mwc.increaseFlakMunitions();
            }

            // AP munitions are hard-countered by hardened, reactive, etc. armor
            if (rp.enemyAdvancedArmorCount > 0.0 && rp.enemyAdvancedArmorCount > rp.enemyReflectiveArmorCount) {
                mwc.decreaseAPMunitions();
            } else if (rp.enemyReflectiveArmorCount > rp.enemyAdvancedArmorCount) {
                // But AP munitions really hurt Reflective!
                mwc.increaseAPMunitions();
            }

            // Heat-based weapons kill infantry dead, also vehicles
            if (rp.enemyFireproofArmorCount == 0.0) {
                if (rp.enemyInfantry >= rp.enemyCount / 4.0) {
                    mwc.increaseHeatMunitions();
                }
                if (rp.enemyVehicles >= rp.enemyCount / 4.0) {
                    mwc.increaseHeatMunitions();
                }
            } else if (rp.enemyFireproofArmorCount >= rp.enemyCount / 4.0) {
                mwc.decreaseHeatMunitions();
                if (rp.enemyInfantry >= rp.enemyCount / 4.0) {
                    mwc.increaseAntiInfMunitions();
                }
            }
        }

        // Section: Friendly capabilities
        if (rp.enemyCount >= rp.friendlyCount * 2.0) {
            // Skip munitions that reduce the number of rounds because we need to shoot a lot!
            mwc.decreaseAmmoReducingMunitions();
        }

        if (rp.friendlyTAGs > 2.0 || rp.friendlyNARCs > 2.0) {
            mwc.increaseGuidedMunitions();

            if (rp.friendlyMissileBoats >= rp.friendlyCount / 2.0) {
                mwc.increaseGuidedMunitions();
            }
        }

        // TODO: add per-unit, per-weapon-type, per-munition-type customization here

        // Just for LOLs: when FS fights CC in 3028 ~ 3050, set Anti-TSM weight to 15.0
        if (t.getFaction().equals("FS") && rp.enemyFactions.contains("CC")
                && (3028 <= rp.allowedYear && rp.allowedYear <= 3050)) {
            ArrayList<String> tsmOnly = new ArrayList(List.of("Anti-TSM"));
            mwc.increaseMunitions(tsmOnly);
            mwc.increaseMunitions(tsmOnly);
            mwc.increaseMunitions(tsmOnly);
        }

        // Convert MWC to MunitionsTree for loading
        applyWeightsToMunitionTree(mt, mwc);
        return mt;
    }

    /**
     * Turn a selection of the computed munition weights into imperatives to load in the MunitionTree
     * @param mt
     * @param rp
     * @return
     */
    public static MunitionTree applyWeightsToMunitionTree(MunitionTree mt, MunitionWeightCollection mwc) {
        // Iterate over every entry in the set of top-weighted munitions for each category
        HashMap<String, List<String>> topWeights = mwc.getTopN(3);

        for (Map.Entry<String, List<String>> e: topWeights.entrySet()) {
            StringBuilder sb = new StringBuilder();
            String lastMunition = "";
            double lastWeight = 0.0;
            int size = e.getValue().size();
            for (int i = 0; i < size; i++) {
                String[] fields = e.getValue().get(i).split("=");
                if (lastWeight > Double.valueOf(fields[1])) {
                    // If the difference between current and prior weight is double or more, add another
                    // copy of it
                    sb.append(lastMunition).append(":");
                }
                lastMunition = fields[0];
                lastWeight = Double.valueOf(fields[1]);
                // Add the current munition
                sb.append(fields[0]);
                if (i < size) {
                    sb.append(":");
                }
            }

            mt.insertImperative("any", "any", "any", e.getKey(), sb.toString());
        }
        return mt;
    }

    /**
     * Wrapper to streamline bot team configuration using standardized defaults
     * @param team
     */
    public void reconfigureBotTeamWithDefaults(Team team, String faction) {
        // Load in some hard-coded defaults now before calculating more.
        reconfigureTeam(team, faction, defaultBotMunitionsFile);
    }

    public void reconfigureTeam(Team team, String faction, MunitionTree mt) {
        reconfigureTeam(game, team, faction, mt);
    }

    /**
     * Wrapper to load a file of preset munition imperatives
     * @param team
     * @param defaultFile
     */
    public void reconfigureTeam(Team team, String faction, String adfFile) {
        ReconfigurationParameters rp = generateParameters(game, gameOptions, team);
        rp.allowedYear = allowedYear;
        MunitionTree mt = generateMunitionTree(rp, team, adfFile);
        reconfigureTeam(game, team, faction, mt);
    }

    /**
     * Main configuration function; mutates units of passed-in team
     *
     * @param g       Game instance
     * @param team    containing units to configure
     * @param mt      MunitionTree with imperatives for desired/required ammo loads per Chassis, variant, pilot
     * @param faction
     */
    public void reconfigureTeam(Game g, Team team, String faction, MunitionTree mt) {
        // configure team according to MunitionTree
        for (Player p: team.players()) {
            for (Entity e : g.getPlayerEntities(p, false)){
                reconfigureEntity(e, mt, faction);
            }
        }
    }

    /**
     * Configure Bot Team with all munitions randomized
     * @param g
     * @param team
     */
    public void randomizeBotTeamConfiguration(Team team, String faction) {
        reconfigureTeam(game, team, faction, generateRandomizedMT());
    }

    public static MunitionTree generateRandomizedMT() {
        MunitionTree mt = new MunitionTree();
        for (String typeName: TYPE_LIST) {
            mt.insertImperative("any", "any", "any", typeName, "Random");
        }
        return mt;
    }

    /**
     * Method to apply a MunitionTree to a specific unit.
     * Main application logic
     * @param e
     * @param mt
     */
    public void reconfigureEntity(Entity e, MunitionTree mt, String faction) {
        String chassis = e.getFullChassis();
        String model = e.getModel();
        String pilot = e.getCrew().getName(0);

        // Create map of bin counts in unit by type
        HashMap<String, ArrayList<Mounted>> binLists = new HashMap<>();

        // Populate map with _valid_, _available_ ammo
        for (Mounted ammoBin: e.getAmmo()) {
            AmmoType aType = (AmmoType) ammoBin.getType();
            String sName = ("".equals(aType.getBaseName())) ? ammoBin.getType().getShortName() : aType.getBaseName();

            // Store the actual bins under their types
            if (!binLists.containsKey(sName)) {
                binLists.put(sName, new ArrayList<>());
            }
            binLists.get(sName).add(ammoBin);
        }

        // Iterate over each type and fill it with the requested ammos (as much as possible)
        for (String binName: binLists.keySet()) {
            iterativelyLoadAmmo(e, mt, binLists.get(binName), binName, faction);
        }
    }

    private void iterativelyLoadAmmo(
        Entity e, MunitionTree mt, ArrayList<Mounted> binList, String binName, String faction
    ){
        String techBase = (e.isClan()) ? "CL" : "IS";
        iterativelyLoadAmmo(e, mt, binList, binName, techBase, faction);
    }

    /**
     * Manage loading ammo bins for a given type.
     * Type can be designated by size (LRM-5) or generic (AC)
     * Logic:
     *  Iterate over list of priorities and fill the first as many times as requested.
     *  Repeat for 2nd..Nth ammo types
     *  If more bins remain than desired types are specified, fill the remainder with the top priority type
     *  If more desired types remain than there are bins, oh well.
     *  If a requested ammo type is not available in the specified timeframe or faction, skip it.
     *
     * @param e Entity to load
     * @param mt MunitionTree, stores required munitions in desired loading order
     * @param binList List of actual mounted ammo bins matching this type
     * @param binName String bin type we are loading now
     */
    private void iterativelyLoadAmmo(
            Entity e, MunitionTree mt, ArrayList<Mounted> binList, String binName, String techBase, String faction
    ) {
        Logger logger = LogManager.getLogger();
        // Copy counts that we will update, otherwise mt entry gets edited permanently.
        HashMap<String, Integer> counts = new HashMap<String, Integer>(mt.getCountsOfAmmosForKey(e.getFullChassis(), e.getModel(), e.getCrew().getName(0), binName));
        List<String> priorities = mt.getPriorityList(e.getFullChassis(), e.getModel(), e.getCrew().getName(0), binName);
        // Track default type for filling in unfilled bins
        AmmoType defaultType = null;
        int defaultIdx = 0;

        // If the imperative is to use Random for every bin, we need a different Random for each bin
        if (priorities.size() == 1 && priorities.get(0).contains("Random")) {
            priorities = new ArrayList<>(Collections.nCopies(binList.size(), "Random"));
        }

        for (int i = 0; i < priorities.size() && !binList.isEmpty(); i++) {
            // binName is the weapon to which the bin connects: LRM-15, AC20, SRM, etc.
            // binType is the munition type loaded in currently
            // If all required bins are filled, revert to defaultType
            // If "Random", choose a random ammo type.  Availability will be checked later.
            // If not trueRandom, only select from munitions that deal damage

            boolean random = priorities.get(i).contains("Random");
            String binType = (random) ?
                    getRandomBin(binName, trueRandom) : priorities.get(i);
            Mounted bin = binList.get(0);
            AmmoType desired = null;

            // Load matching AmmoType
            if (binType.toLowerCase().contains("standard")) {
                desired = (AmmoType) EquipmentType.get(techBase + " " + binName + " " + "Ammo");
                if (desired == null) {
                    // Some ammo, like AC/XX ammo, is named funny
                    desired = (AmmoType) EquipmentType.get(techBase + " Ammo " + binName);
                }
            } else {
                // Get available munitions
                Vector<AmmoType> vAllTypes = AmmoType.getMunitionsFor(((AmmoType) bin.getType()).getAmmoType());
                if (vAllTypes == null) {
                    continue;
                }

                // Make sure the desired munition type is available and valid
                desired = vAllTypes.stream()
                        .filter(m -> m.getInternalName().startsWith(techBase) && m.getBaseName().contains(binName) && m.getName().contains(binType))
                        .filter(d -> checkLegality(d, faction, techBase, e.isMixedTech()))
                        .findFirst().orElse(null);
            }

            if (desired == null) {
                // Couldn't find a bin, move on to the next priority.
                // Update default idx if we're currently setting the default
                defaultIdx = (i==defaultIdx) ? defaultIdx + 1 : defaultIdx;
                continue;
            }

            // Add one of the current binType to counts so we get a new random type every bin
            if (random) {
                counts.put(binType, 1);
            }

            // Store default AmmoType
            if (i == defaultIdx) {
                defaultType = desired;
            }

            // Continue filling with this munition type until count is fulfilled or there are no more bins
            while (
                    counts.getOrDefault(binType, 0) > 0
                    && !binList.isEmpty()
            ) {
                try {
                    // fill one ammo bin with the requested ammo type
                    // Check if the bin can even load the desired munition
                    if (!((AmmoType)bin.getType()).equalsAmmoTypeOnly(desired)){
                        // can't use this ammo
                        logger.debug("Unable to load bin " + bin.getName() + " with " + desired.getName());
                        // Unset default bin if it was unloadable
                        if (i == defaultIdx) {
                            defaultType = null;
                            defaultIdx += 1;
                        }
                        break;
                    }
                    // Apply ammo change
                    binList.get(0).changeAmmoType(desired);

                    // Decrement count and remove bin from list
                    counts.put(binType, counts.get(binType) - 1);
                    binList.remove(0);

                } catch (Exception ex) {
                    logger.debug("Error loading ammo bin!", ex);
                    break;
                }
            }
        }

        if (!(defaultType == null || binList.isEmpty())) {
            for (Mounted bin : binList) {
                bin.changeAmmoType(defaultType);
            }
        }
    }

    private static String getRandomBin(String binName, boolean trueRandom) {
        String result = "";
        for (String typeName: TYPE_LIST) {
            if ((trueRandom || !UTILITY_MUNITIONS.contains(typeName)) &&
                    (binName.toLowerCase().contains(typeName.toLowerCase())
                    || typeName.toLowerCase().contains(binName.toLowerCase()))) {
                ArrayList<String> tList = TYPE_MAP.get(typeName.toLowerCase());
                result = tList.get(new Random().nextInt(tList.size()));
                break;
            }
        }
        return result;
    }
}

/**
 * Bare data class to pass around and store configuration-influencing info
 */
class ReconfigurationParameters {

    // Game settings
    public boolean enemiesVisible = true;
    public int allowedYear = 3151;

    // Map settings
    public boolean darkEnvironment = false;

    // Enemy stats
    public long enemyCount = 0;
    public long enemyFliers = 0;
    public long enemyBombers = 0;
    public long enemyInfantry = 0;
    public long enemyVehicles = 0;
    public long enemyMeks = 0;
    public long enemyEnergyBoats = 0;
    public long enemyMissileBoats = 0;
    public long enemyAdvancedArmorCount = 0;
    public long enemyReflectiveArmorCount = 0;
    public long enemyFireproofArmorCount = 0;
    public long enemyFastMovers = 0;
    public HashSet<String> enemyFactions = new HashSet<String>();

    // Friendly stats
    public long friendlyCount = 0;
    public long friendlyTAGs = 0;
    public long friendlyNARCs = 0;
    public long friendlyEnergyBoats = 0;
    public long friendlyMissileBoats = 0;

    // Datatype for passing around game parameters the Loadout Generator cares about
    ReconfigurationParameters() {
    }
}

class MunitionWeightCollection {
    private HashMap<String, Double> lrmWeights;
    private HashMap<String, Double> srmWeights;
    private HashMap<String, Double> acWeights;
    private HashMap<String, Double> atmWeights;
    private HashMap<String, Double> arrowWeights;
    private HashMap<String, Double> artyWeights;
    private HashMap<String, Double> artyCannonWeights;
    private HashMap<String, Double> mekMortarWeights;
    private HashMap<String, Double> narcWeights;
    private HashMap<String, Double> bombWeights;
    private Map<String, HashMap<String, Double>> mapTypeToWeights;

    MunitionWeightCollection() {
        resetWeights();
    }

    public void resetWeights() {
        // Initialize weights for all the weapon types using known munition names
        lrmWeights = initializeWeaponWeights(MunitionTree.LRM_MUNITION_NAMES);
        srmWeights = initializeWeaponWeights(MunitionTree.SRM_MUNITION_NAMES);
        acWeights = initializeWeaponWeights(MunitionTree.AC_MUNITION_NAMES);
        atmWeights = initializeWeaponWeights(MunitionTree.ATM_MUNITION_NAMES);
        arrowWeights = initializeWeaponWeights(MunitionTree.ARROW_MUNITION_NAMES);
        artyWeights = initializeWeaponWeights(MunitionTree.ARTILLERY_MUNITION_NAMES);
        artyCannonWeights = initializeWeaponWeights(MunitionTree.ARTILLERY_CANNON_MUNITION_NAMES);
        mekMortarWeights = initializeWeaponWeights(MunitionTree.MEK_MORTAR_MUNITION_NAMES);
        narcWeights = initializeWeaponWeights(MunitionTree.NARC_MUNITION_NAMES);
        bombWeights = initializeWeaponWeights(MunitionTree.BOMB_MUNITION_NAMES);

        mapTypeToWeights = new HashMap<>(Map.ofEntries(
                entry("lrm", lrmWeights),
                entry("srm", srmWeights),
                entry("ac", acWeights),
                entry("atm", atmWeights),
                entry("arrow iv", arrowWeights),
                entry("artillery", artyWeights),
                entry("artillery cannon", artyCannonWeights),
                entry("mek mortar", mekMortarWeights),
                entry("narc", narcWeights),
                entry("bomb", bombWeights)
        ));
    }

    // Section: initializing weights
    private static HashMap<String, Double> initializeWeaponWeights(ArrayList<String> wepAL) {
        HashMap<String, Double> weights = new HashMap<String, Double>();
        for (String name: wepAL) {
            weights.put(name, 1.0);
        }
        // Every weight list should have a Standard set as weight 2.0
        weights.put("Standard", 2.0);
        return weights;
    }

    // Increase/Decrease functions.  Increase is 2x + 1, decrease is 0.5x, so items
    // voted up and down multiple times should still exceed items never voted up _or_ down.

    public void increaseMunitions(ArrayList<String> munitions) {
        mapTypeToWeights.entrySet().forEach(
                e -> modifyMatchingWeights(
                        e.getValue(), munitions, 2.0, 1.0
                )
        );
    }

    public void decreaseMunitions(ArrayList<String> munitions) {
        mapTypeToWeights.entrySet().forEach(
                e -> modifyMatchingWeights(
                        e.getValue(), munitions, 0.5, 0.0
                )
        );
    }

    public void increaseAPMunitions() {
        increaseMunitions(TeamLoadoutGenerator.AP_MUNITIONS);
    }

    public void decreaseAPMunitions() {
        decreaseMunitions(TeamLoadoutGenerator.AP_MUNITIONS);
    }

    public void increaseFlakMunitions() {
        increaseMunitions(TeamLoadoutGenerator.FLAK_MUNITIONS);
    }

    public void decreaseFlakMunitions() {
        decreaseMunitions(TeamLoadoutGenerator.FLAK_MUNITIONS);
    }

    public void increaseAntiInfMunitions() {
        increaseMunitions(TeamLoadoutGenerator.ANTI_INF_MUNITIONS);
    }

    public void decreaseAntiInfMunitions() {
        decreaseMunitions(TeamLoadoutGenerator.ANTI_INF_MUNITIONS);
    }

    public void increaseHeatMunitions() {
        increaseMunitions(TeamLoadoutGenerator.HEAT_MUNITIONS);
    }

    public void decreaseHeatMunitions() {
        decreaseMunitions(TeamLoadoutGenerator.HEAT_MUNITIONS);
    }

    public void increaseIllumMunitions() {
        increaseMunitions(TeamLoadoutGenerator.ILLUM_MUNITIONS);
    }

    public void decreaseIllumMunitions() {
        decreaseMunitions(TeamLoadoutGenerator.ILLUM_MUNITIONS);
    }

    public void increaseUtilityMunitions() {
        increaseMunitions(TeamLoadoutGenerator.UTILITY_MUNITIONS);
    }

    public void decreaseUtilityMunitions() {
        decreaseMunitions(TeamLoadoutGenerator.UTILITY_MUNITIONS);
    }

    public void increaseGuidedMunitions() {
        increaseMunitions(TeamLoadoutGenerator.GUIDED_MUNITIONS);
    }

    public void decreaseGuidedMunitions() {
        decreaseMunitions(TeamLoadoutGenerator.GUIDED_MUNITIONS);
    }

    public void increaseAmmoReducingMunitions() {
        increaseMunitions(TeamLoadoutGenerator.AMMO_REDUCING_MUNITIONS);
    }

    public void decreaseAmmoReducingMunitions() {
        increaseMunitions(TeamLoadoutGenerator.AMMO_REDUCING_MUNITIONS);
    }


    /**
     * Update all matching types in a category by multiplying by a factor and adding an increment
     * (1.0, 0.0) = no change; (2.0, 0.0) = double, (0.5, 0.0) = halve,
     * (1.0, 1.0) = increment by 1, (1.0, -1.0) = decrement by 1, etc.
     * @param current
     * @param types
     * @param factor
     * @param increment
     */
    private static void modifyMatchingWeights(HashMap<String, Double> current, ArrayList<String> types, double factor, double increment) {
        for (String key: types) {
            if (current.containsKey(key)) {
                current.put(key, current.get(key) * factor + increment);
            }
        }
    }

    public ArrayList<String> getMunitionTypesInWeightOrder(Map<String, Double> weightMap) {
        ArrayList<String> orderedTypes = new ArrayList<>();
        weightMap.entrySet().stream()
                .sorted((E1, E2) -> E2.getValue().compareTo(E1.getValue()))
                .forEach(k -> orderedTypes.add(String.valueOf(k)));
        return orderedTypes;
    }

    public HashMap<String, List<String>> getTopN(int count) {
        HashMap<String, List<String>> topMunitionsMap = new HashMap<>();
        for (String key: TeamLoadoutGenerator.TYPE_MAP.keySet()) {
            List<String> orderedList = getMunitionTypesInWeightOrder(mapTypeToWeights.get(key));
            topMunitionsMap.put(key, (orderedList.size() >= count) ? orderedList.subList(0, count) : orderedList);
        }
        return topMunitionsMap;
    }

    public HashMap<String, Double> getLrmWeights() {
        return lrmWeights;
    }

    public HashMap<String, Double> getSrmWeights() {
        return srmWeights;
    }

    public HashMap<String, Double> getAcWeights() {
        return acWeights;
    }

    public HashMap<String, Double> getAtmWeights() {
        return atmWeights;
    }

    public HashMap<String, Double> getArrowWeights() {
        return arrowWeights;
    }

    public HashMap<String, Double> getArtyWeights() {
        return artyWeights;
    }

    public HashMap<String, Double> getArtyCannonWeights() {
        return artyCannonWeights;
    }

    public HashMap<String, Double> getMekMortarWeights() {
        return mekMortarWeights;
    }
}

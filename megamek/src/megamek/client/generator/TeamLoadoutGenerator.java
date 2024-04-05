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

/**
 * Notes: checkout
 * - RATGenerator.java
 * - ForceDescriptor.java
 * for era-based search examples
 */

public class TeamLoadoutGenerator {

    private static ClientGUI cg;
    private static Game game;

    protected GameOptions gameOptions = null;
    protected boolean enableYearLimits = false;
    protected int allowedYear = AbstractUnitSelectorDialog.ALLOWED_YEAR_ANY;
    protected int gameTechLevel = TechConstants.T_SIMPLE_INTRO;
    protected boolean eraBasedTechLevel = false;
    protected String defaultBotMunitionsFile = null;

    TeamLoadoutGenerator(ClientGUI gui){
        cg = gui;
        game = cg.getClient().getGame();
        gameOptions = game.getOptions();
    }

    TeamLoadoutGenerator(ClientGUI gui, String defaultSettings){
        this(gui);
        this.defaultBotMunitionsFile = defaultSettings;
    }

    public void updateOptionValues() {
        gameOptions = cg.getClient().getGame().getOptions();
        enableYearLimits = true;
        allowedYear = gameOptions.intOption(OptionsConstants.ALLOWED_YEAR);
        gameTechLevel = TechConstants.getSimpleLevel(gameOptions.stringOption("techlevel"));
        eraBasedTechLevel = gameOptions.booleanOption(OptionsConstants.ALLOWED_ERA_BASED);
    }

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
        return el.stream().filter(e -> e.getAmmo().size() == 0).count();
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

    /**
     * Create the parameters that will determine how to configure ammo loadouts for this team
     * @param g
     * @param gOpts
     * @param t
     * @return ReconfigurationParameters with information about enemy and friendly forces
     */
    public static ReconfigurationParameters generateParameters(Game g, GameOptions gOpts, Team t) {
        ReconfigurationParameters rc = new ReconfigurationParameters();

        // Get our own side's numbers for comparison
        ArrayList<Entity> ownTeamEntities = (ArrayList<Entity>) IteratorUtils.toList(g.getTeamEntities(t));
        rc.friendlyCount = ownTeamEntities.size();

        // If our team can see other teams...
        if (!gOpts.booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND)) {
            // This team can see the opponent teams; set appropriate options
            for (Team et: g.getTeams()) {
                if (!et.isEnemyOf(t)) {
                    continue;
                }
                ArrayList<Entity> etEntities = (ArrayList<Entity>) IteratorUtils.toList(g.getTeamEntities(et));
                rc.enemyCount += etEntities.size();
                rc.enemyFliers += checkForFliers(etEntities);
                rc.enemyBombers += checkForBombers(etEntities);
                rc.enemyInfantry += checkForInfantry(etEntities);
                rc.enemyVehicles += checkForVehicles(etEntities);
                rc.enemyMeks += checkForMeks(etEntities);
                rc.enemyEnergyBoats += checkForEnergyBoats(etEntities);
                rc.enemyAdvancedArmorCount += checkForAdvancedArmor(etEntities);
                rc.enemyReflectiveArmorCount += checkForReflectiveArmor(etEntities);
                rc.enemyFireproofArmorCount += checkForFireproofArmor(etEntities);
            }
        } else {
            rc.enemiesVisible = false;
            // Estimate enemy count for ammo count purposes; may include observers.  The fog of war!
            rc.enemyCount = g.getEntitiesVector().size() - ownTeamEntities.size();
        }

        // Friendly force info
        rc.friendlyEnergyBoats = checkForEnergyBoats(ownTeamEntities);
        rc.friendlyMissileBoats = checkForMissileBoats(ownTeamEntities);
        rc.friendlyTAGs = checkForTAG(ownTeamEntities);
        rc.friendlyNARCs = checkForNARC(ownTeamEntities);

        // General parameters
        rc.darkEnvironment = g.getPlanetaryConditions().getLight().isDarkerThan(Light.DAY);


        return rc;
    }

    // Section: initializing weights
    private static HashMap<String, Double> initializeLRMWeights() {
        HashMap<String, Double> weights = new HashMap<String, Double>();
        weights.put("Standard", 1.0);
        for (AmmoType aType: AmmoType.getMunitionsFor(AmmoType.T_LRM)) {
            weights.put(aType.getMunitionType().toString(), 0.0);
        }
        return weights;
    }

    /**
     * Generate the list of desired ammo load-outs for this team.
     * TODO: implement generateDetailedMunitionTree with more complex breakdowns per unit type
     * @param rc
     * @param t
     * @param defaultSettingsFile
     * @return generated MunitionTree with imperatives for each weapon type
     */
    public static MunitionTree generateMunitionTree(ReconfigurationParameters rp, Team t, String defaultSettingsFile) {

        MunitionTree mt = (defaultSettingsFile == null) ?
                new MunitionTree() : new MunitionTree(new File(defaultSettingsFile));

        HashMap<String, Double> lrmWeights = initializeLRMWeights();
        HashMap<String, Double> srmWeights = new HashMap<String, Double>();
        HashMap<String, Double> acWeights = new HashMap<String, Double>();
        HashMap<String, Double> atmWeights = new HashMap<String, Double>();
        HashMap<String, Double> arrowWeights = new HashMap<String, Double>();
        HashMap<String, Double> artyWeights = new HashMap<String, Double>();
        HashMap<String, Double> artyCannonWeights = new HashMap<String, Double>();
        HashMap<String, Double> mekMortarWeights = new HashMap<String, Double>();

        // Based on various requirements from rp, set weights for some ammo types over others


        // Generate general loadouts

        return mt;
    }

    /**
     * Wrapper to streamline bot team configuration
     * @param team
     */
    public void reconfigureBotTeam(Team team) {
        // Load in some hard-coded defaults so we don't have to do all the work now.
        reconfigureTeam(team, defaultBotMunitionsFile);
    }

    public void reconfigureTeam(Team team, String defaultFile) {
        ReconfigurationParameters rp = generateParameters(game, gameOptions, team);
        MunitionTree mt = generateMunitionTree(rp, team, defaultFile);
        reconfigureTeam(game, team, mt);
    }

    /**
     * Main configuration function; mutates units of passed-in team
     * @param g Game instance
     * @param team containing units to configure
     * @param mt MunitionTree with imperatives for desired/required ammo loads per Chassis, variant, pilot
     */
    public void reconfigureTeam(Game g, Team team, MunitionTree mt) {
        // configure team according to MunitionTree
        for (Player p: team.players()) {
            for (Entity e : g.getPlayerEntities(p, false)){
                reconfigureEntity(e, mt);
            }
        }
    }

    /**
     * Method to apply a MunitionTree to a specific unit.
     * Main application logic
     * @param e
     * @param mt
     */
    public void reconfigureEntity(Entity e, MunitionTree mt) {
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
            iterativelyLoadAmmo(e, mt, binLists.get(binName), binName);
        }
    }

    private static void iterativelyLoadAmmo(
        Entity e, MunitionTree mt, ArrayList<Mounted> binList, String binName
    ){
        String techBase = (e.isClan()) ? "CL" : "IS";
        iterativelyLoadAmmo(e, mt, binList, binName, techBase);
    }

    /**
     * Manage loading ammo bins for a given type.
     * Type can be designated by size (LRM-5) or generic (AC)
     * Logic:
     *  Iterate over list of priorities and fill the first as many times as desired.
     *  Repeat for 2nd..Nth ammo types
     *  If more bins remain than desired types are specified, fill the remainder with the top priority type
     *  If more desired types remain than there are bins, oh well.
     *
     * @param e Entity to load
     * @param mt MunitionTree, stores required munitions in desired loading order
     * @param binList List of actual mounted ammo bins matching this type
     * @param binName String bin type we are loading now
     */
    private static void iterativelyLoadAmmo(
            Entity e, MunitionTree mt, ArrayList<Mounted> binList, String binName, String techBase
    ) {
        Logger logger = LogManager.getLogger();
        HashMap<String, Integer> counts = mt.getCountsOfAmmosForKey(e.getFullChassis(), e.getModel(), e.getCrew().getName(0), binName);
        List<String> priorities = mt.getPriorityList(e.getFullChassis(), e.getModel(), e.getCrew().getName(0), binName);
        // Count of total required bins
        AmmoType defaultType = null;
        int defaultIdx = 0;

        for (int i = 0; i < priorities.size(); i++) {
            // binName is the weapon to which the bin connects: LRM-15, AC20, SRM, etc.
            // binType is the munition type loaded in currently
            // If all required bins are filled, revert to defaultType
            String binType = priorities.get(i);
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

                // Make sure the desired munition type is available
                desired = vAllTypes.stream().filter(m -> m.getInternalName().startsWith(techBase) && m.getBaseName().contains(binName) && m.getName().contains(binType)).findFirst().orElse(null);
            }

            if (desired == null) {
                // Couldn't find a bin, move on to the next priority.
                // Update default idx if we're currently setting the default
                defaultIdx = (i==defaultIdx) ? defaultIdx + 1 : defaultIdx;
                continue;
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
}

/**
 * Bare data class to pass around and store configuration-influencing info
 */
class ReconfigurationParameters {

    // Game settings
    public boolean enemiesVisible = true;

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


    // Friendly stats
    public long friendlyCount = 0;
    public long friendlyTAGs = 0;
    public long friendlyNARCs = 0;
    public long friendlyEnergyBoats = 0;
    public long friendlyMissileBoats = 0;




    ReconfigurationParameters() {
    }
}

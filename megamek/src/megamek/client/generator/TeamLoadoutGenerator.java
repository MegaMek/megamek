package megamek.client.generator;

import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.dialog.AbstractUnitSelectorDialog;
import megamek.common.*;
import megamek.common.containers.MunitionTree;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
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

    public static ReconfigurationParameters generateParameters(Game g, GameOptions gOpts, Team t) {
        ReconfigurationParameters rc = new ReconfigurationParameters();
        if (!game.getOptions().booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND)) {
            // This team can see the opponent teams; set appropriate options
            rc.enemiesVisible = true;
        }
        // General parameters

        return rc;
    }

    /**
     * Generate the list of desired ammo load-outs for this team.
     * @param rc
     * @param t
     * @param defaultSettingsFile
     * @return
     */
    public static MunitionTree generateMunitionTree(ReconfigurationParameters rp, Team t, String defaultSettingsFile) {
        MunitionTree mt = (defaultSettingsFile == null) ?
                new MunitionTree() : new MunitionTree(new File(defaultSettingsFile));

        // Decide which weapons need alternate munitions loaded

        // Based on various requirements from rp, set weights for some ammo types over others

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
     * @param binLists Map of actual mounted ammo bins, listed by type
     * @param binName String bin type we are loading now
     */
    private static void iterativelyLoadAmmo(
            Entity e, MunitionTree mt, ArrayList<Mounted> binList, String binName, String techBase
    ) {
        Logger logger = LogManager.getLogger();
        HashMap<String, Integer> counts = mt.getCountsOfAmmosForKey(e.getFullChassis(), e.getModel(), e.getCrew().getName(0), binName);
        List<String> priorities = mt.getPriorityList(e.getFullChassis(), e.getModel(), e.getCrew().getName(0), binName);
        // Count of total required bins
        int required = counts.values().stream().reduce(0, Integer::sum);
        int filled = 0;
        AmmoType defaultType = null;

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
            } else {
                // Get available munitions
                Vector<AmmoType> vAllTypes = AmmoType.getMunitionsFor(((AmmoType) bin.getType()).getAmmoType());
                if (vAllTypes == null) {
                    continue;
                }

                // Make sure the desired munition type is available
                desired = vAllTypes.stream().filter(m -> m.getInternalName().startsWith(techBase) && m.getBaseName().contains(binName) && m.getName().contains(binType)).findFirst().orElse(null);
                if (desired == null) {
                    continue;
                }
            }

            // Store default AmmoType
            if (i == 0) {
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
                        break;
                    }
                    // Apply ammo change
                    binList.get(0).changeAmmoType(desired);

                    // Decrement count and remove bin from list
                    counts.put(binType, counts.get(binType) - 1);
                    binList.remove(0);
                    filled += 1;

                } catch (Exception ex) {
                    logger.debug("Error loading ammo bin!", ex);
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

    public boolean enemiesVisible = false;

    ReconfigurationParameters() {
    }
}

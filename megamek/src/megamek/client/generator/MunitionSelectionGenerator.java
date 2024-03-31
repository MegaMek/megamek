package megamek.client.generator;

import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.EquipChoicePanel;
import megamek.client.ui.swing.dialog.AbstractUnitSelectorDialog;
import megamek.common.*;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class MunitionSelectionGenerator {

    private static ClientGUI cg;
    private static Game game;

    protected GameOptions gameOptions = null;
    protected boolean enableYearLimits = false;
    protected int allowedYear = AbstractUnitSelectorDialog.ALLOWED_YEAR_ANY;
    protected int gameTechLevel = TechConstants.T_SIMPLE_INTRO;
    protected boolean eraBasedTechLevel = false;

    MunitionSelectionGenerator(ClientGUI gui){
        cg = gui;
        game = cg.getClient().getGame();
        gameOptions = game.getOptions();
    }

    public void updateOptionValues() {
        gameOptions = cg.getClient().getGame().getOptions();
        enableYearLimits = true;
        allowedYear = gameOptions.intOption(OptionsConstants.ALLOWED_YEAR);
        gameTechLevel = TechConstants.getSimpleLevel(gameOptions.stringOption("techlevel"));
        eraBasedTechLevel = gameOptions.booleanOption(OptionsConstants.ALLOWED_ERA_BASED);
    }

    public static ArrayList<WeaponType> collateEntityWeaponTypes(Entity entity) {
        // We only want 1. weapons 2. that utilize ammo bins
        // Ignore bay weapons as Aerospace units can't take "alt" ammo.
        ArrayList<WeaponType> types = new ArrayList<>();
        for (Mounted m : entity.getWeaponList()) {
            if (!(m.getType() instanceof WeaponType)) {
                continue;
            }

            WeaponType wt = (WeaponType) m.getType();
            int at = wt.getAmmoType();
            if (at != WeaponType.WEAPON_NA) {
                types.add(wt);
            }
        }
        return types;
    }

    public Set<WeaponType> collectWeaponTypes(Team team){
        Set<WeaponType> types = new HashSet<WeaponType>();
        for (Player teamMember : team.players()) {
            // Get the "real" player object, as the team's may be wrong
            Player player = game.getPlayer(teamMember.getId());
            // Iterate over entities
            for (Entity entity : game.getPlayerEntities(player, false)) {
                types.addAll(collateEntityWeaponTypes(entity));
            }
        }
        return types;
    }

    /** Determine which munitions are allowed for the given faction in the
     *  selected year, depending on several settings.
     *  (See {@link EquipChoicePanel.WeaponAmmoChoicePanel} for hints)
     *  (See {@link EquipChoicePanel#setupMunitions()} for hints)
     *
     * @param wTypes
     * @param yearLimits
     * @param year
     * @param techLevel
     * @param eraBased
     * @return
     */
    public Set<AmmoType.Munitions> determineViableMunitions(
            String faction,
            Set<WeaponType> wTypes,
            boolean yearLimits,
            int year,
            int techLevel,
            boolean eraBased
    ){
        Set<AmmoType.Munitions> munitions = new HashSet<AmmoType.Munitions>();

        for (WeaponType wt: wTypes) {

        }

        return munitions;
    }

    /**
     * Given the game setting and team's faction, choose valid munitions
     *
     * @param team to generate munition selection for
     * @return ArrayList of allowed munitions
     */
    public Set<AmmoType.Munitions> generateMunitionSelection(Team team){
        String faction = team.getFaction();
        Set<AmmoType.Munitions> munitions = new HashSet<AmmoType.Munitions>();

        // Iterate over all units to populate a set of ammo-fed weapons
        Set<WeaponType> weaponTypes = collectWeaponTypes(team);

        // Iterate all munitions and keep only those available to this team's faction in the time
        // period selected.
        munitions = determineViableMunitions(faction, weaponTypes, enableYearLimits, allowedYear, gameTechLevel, eraBasedTechLevel);

        return munitions;
    }
}
/*
 * MegaAero - Copyright (C) 2007 Jay Lawson This program is free software; you
 * can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
/*
 * Created on Jun 17, 2007
 */
package megamek.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Vector;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import megamek.common.IGame.Phase;
import megamek.common.options.OptionsConstants;

/**
 * @author Jay Lawson Fighter squadrons are basically "containers" for a bunch
 *         of fighters.
 */
public class FighterSquadron extends Aero {
    private static final long serialVersionUID = 3491212296982370726L;

    public static int MAX_SIZE = 6;
    // Value is arbitrary, but StratOps shows up to 10 so we'll use that as an
    // alternate MAX_SIZE when using
    // the option for larger squadrons
    public static int ALTERNATE_MAX_SIZE = 10;

    private static final Predicate<Entity> ACTIVE_CHECK = ent -> !(ent.isDestroyed() || ent.isDoomed());
    
    private Vector<Integer> fighters = new Vector<Integer>();

    // fighter squadrons need to keep track of heat capacity apart from their
    // fighters
    private int heatcap = 0;
    private int heatcapNoRHS = 0;

    public FighterSquadron() {
        super();
        setChassis("Squadron");
        setModel("");
    }

    /**
     * construct fighter squadron with a specific name
     */
    public FighterSquadron(String name) {
        super();
        setChassis(name.trim() + " Squadron");
        setModel("");
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.Aero#getCost(boolean)
     */
    @Override
    public double getCost(boolean ignoreAmmo) {
        return fighters.stream()
                .mapToDouble(fid -> game.getEntity(fid).getCost(ignoreAmmo))
                .sum();
    }

    /**
     * overrides the default {@link Entity#isCapitalFighter()} with true
     */
    @Override
    public boolean isCapitalFighter() {
        return true;
    }

    @Override
    public int get0SI() {
        return fighters.stream().map(fid -> game.getEntity(fid))
            .filter(ACTIVE_CHECK).mapToInt(ent -> ((IAero)ent).get0SI()).min().orElse(0);
    }

    @Override
    public int getSI() {
        return fighters.stream().map(fid -> game.getEntity(fid))
            .filter(ACTIVE_CHECK).mapToInt(ent -> ((IAero)ent).getSI()).min().orElse(0);
    }

    @Override
    public int getTotalArmor() {
        return fighters.stream()
                .mapToInt(fid -> ((IAero)game.getEntity(fid)).getCapArmor())
                .sum();
    }

    @Override
    public int getTotalOArmor() {
        return fighters.stream()
                .mapToInt(fid -> ((IAero) game.getEntity(fid)).getCap0Armor())
                .sum();
    }

    /**
     * Returns the percent of the armor remaining
     */
    @Override
    public double getArmorRemainingPercent() {
        if (getTotalOArmor() == 0) {
            return IArmorState.ARMOR_NA;
        }
        return ((double) getTotalArmor() / (double) getTotalOArmor());
    }

    @Override
    public int getWalkMP(boolean gravity, boolean ignoreheat,
            boolean ignoremodulararmor) {
        return fighters.stream().map(fid -> game.getEntity(fid))
                .filter(ACTIVE_CHECK)
                .mapToInt(ent -> ent.getWalkMP(gravity, ignoreheat)).min()
                .orElse(0);
    }

    @Override
    public int getFuel() {
        return fighters.stream().map(fid -> game.getEntity(fid))
            .filter(ACTIVE_CHECK).mapToInt(ent -> ((IAero)ent).getFuel()).min().orElse(0);
    }

    /*
     * base this on the max size of the fighter squadron, since the initial size
     * can fluctuate due to joining and splitting
     */
    @Override
    public double getInternalRemainingPercent() {
        return (getActiveSubEntities().orElse(Collections.emptyList()).size() * 1.0 / getMaxSize());
    }

    @Override
    public boolean hasTargComp() {
        List<Entity> activeFighters = getActiveSubEntities()
                .orElse(Collections.emptyList());
        if (activeFighters.isEmpty()) {
            return false;
        }
        int nTC = activeFighters.stream()
                .mapToInt(ent -> ent.hasTargComp() ? 1 : 0).sum();
        return (nTC * 1.0 / activeFighters.size() >= 0.5);
    }

    @Override
    public boolean hasActiveECM() {
        if (!game.getOptions()
                .booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_ECM)
                || !game.getBoard().inSpace()) {
            return super.hasActiveECM();
        }
        return fighters.stream().map(fid -> game.getEntity(fid))
                .filter(ACTIVE_CHECK).filter(ent -> ent.hasActiveECM())
                .findFirst().isPresent();
    }

    /**
     * Do units loaded onto this entity still have active ECM/ECCM/etc.?
     */
    @Override
    public boolean loadedUnitsHaveActiveECM() {
        return true;
    }

    @Override
    public PilotingRollData addEntityBonuses(PilotingRollData prd) {

        // movement effects
        // some question as to whether "above safe thrust" applies to thrust or
        // velocity
        // I will treat it as thrust until it is resolved
        if (moved == EntityMovementType.MOVE_OVER_THRUST) {
            prd.addModifier(+1, "Used more than safe thrust");
        }
        int vel = getCurrentVelocity();
        int vmod = vel - (2 * getWalkMP());
        if (vmod > 0) {
            prd.addModifier(vmod, "Velocity greater than 2x safe thrust");
        }

        // add in atmospheric effects later
        int atmoCond = game.getPlanetaryConditions().getAtmosphere();
        if (!(game.getBoard().inSpace() || atmoCond == PlanetaryConditions.ATMO_VACUUM)) {
            prd.addModifier(+2, "Atmospheric operations");

            prd.addModifier(-1, "fighter/small craft");
        }

        // according to personal communication with Welshman, the normal crit
        // penalties are added up across the fighter squadron
        fighters.stream().map(fid -> game.getEntity(fid))
            .filter(ACTIVE_CHECK).map(ent -> (IAero)ent).forEachOrdered(
            ent -> {
                int avihits = ent.getAvionicsHits();
                if ((avihits > 0) && (avihits < 3)) {
                    prd.addModifier(avihits, "Avionics Damage");
                } else if (avihits >= 3) {
                    // this should probably be replaced with some kind of AVI_DESTROYED boolean
                    prd.addModifier(5, "Avionics Destroyed");
                }

                // life support (only applicable to non-ASFs)
                if(!ent.hasLifeSupport()) {
                    prd.addModifier(2, "No life support");
                }

                if(((Entity)ent).hasModularArmor()) {
                    prd.addModifier(1, "Modular Armor");
                }
            });
        return prd;
    }

    @Override
    public int getClusterMods() {
        return fighters.stream().map(fid -> game.getEntity(fid))
            .filter(ACTIVE_CHECK).filter(ent -> (((IAero)ent).getFCSHits() <= 2))
            .mapToInt(ent -> ((IAero)ent).getClusterMods()).sum();
    }

    @Override
    public int calculateBattleValue(boolean ignoreC3, boolean ignorePilot) {
        if (useManualBV) {
            return manualBV;
        }

        int bv = 0;
        /*
         * for(Aero fighter : fighters) { bv +=
         * fighter.calculateBattleValue(ignoreC3, ignorePilot); }
         */
        return bv;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.Aero#calculateBattleValue()
     */
    @Override
    public int calculateBattleValue() {
        if (useManualBV) {
            return manualBV;
        }
        return calculateBattleValue(false, false);
    }

    @Override
    public int getHeatSinks() {
        return fighters.stream().map(fid -> game.getEntity(fid))
            .filter(ACTIVE_CHECK).mapToInt(ent -> ((IAero)ent).getHeatSinks()).sum();
    }
    
    @Override
    public int getHeatCapacity(boolean includeRadicalHeatSink){
        if (includeRadicalHeatSink){
            return heatcap;
        } else {
            return heatcapNoRHS;
        }
    }

    public void resetHeatCapacity() {
        List<Entity> activeFighters = fighters.stream()
                .map(fid -> game.getEntity(fid)).filter(ACTIVE_CHECK)
                .collect(Collectors.toList());
        heatcap = activeFighters.stream()
                .mapToInt(ent -> ent.getHeatCapacity(true)).sum();
        heatcapNoRHS = activeFighters.stream()
                .mapToInt(ent -> ent.getHeatCapacity(false)).sum();
    }

    @Override
    public double getWeight() {
        return fighters.stream().map(fid -> game.getEntity(fid))
            .filter(ACTIVE_CHECK).mapToDouble(ent -> ent.getWeight()).sum();
    }

    public double getAveWeight() {
        List<Entity> activeFighters = getActiveSubEntities()
                .orElse(Collections.emptyList());
        return activeFighters.isEmpty() ? Double.NaN
                : (getWeight() / activeFighters.size());
    }

    /***
     * rather than keeping track of weapons on each fighter, every new round
     * just collect the current weapon groups by cycling through each fighter
     * and then create a new weaponGroupList. This will be trickier in terms of
     * using and keeping track of ammo, which is necessary in case squadron
     * splits, but should work otherwise
     */

    /**
     * Fighter Squadron units can only get hit in undestroyed fighters.
     */
    @Override
    public HitData rollHitLocation(int table, int side, int aimedLocation, int aimingMode, int cover) {
        // Create a list of the indices of all the active fighters, which serves as the list of valid hit locations
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < fighters.size(); i++) {
            if (ACTIVE_CHECK.test(game.getEntity(fighters.get(i)))) {
                indices.add(i);
            }
        }
        // If this squadron is doomed or is of size 1 then just return the first
        // one
        if (isDoomed() || indices.isEmpty()) {
            return new HitData(0);
        }

        // Pick a random number between 0 and the number of fighters in the
        // squadron.
        int hit = indices.get(Compute.randomInt(indices.size()));
        return new HitData(hit);
    }

    @Override
    public HitData rollHitLocation(int table, int side) {
        return rollHitLocation(table, side, LOC_NONE, IAimingModes.AIM_MODE_NONE, LosEffects.COVER_NONE);
    }

    @Override
    public void newRound(int roundNumber) {
        super.newRound(roundNumber);
        updateWeaponGroups();
        loadAllWeapons();
        updateSkills();
        resetHeatCapacity();
    }

    /**
     * instead of trying to track the individual units weapons, just recompile
     * the weapon groups for this squadron each round
     */
    @Override
    public void updateWeaponGroups() {
        // first we need to reset all the weapons in our existing mounts to zero
        // until proven otherwise
        Set<String> set = weaponGroups.keySet();
        Iterator<String> iter = set.iterator();
        while (iter.hasNext()) {
            String key = iter.next();
            this.getEquipment(weaponGroups.get(key)).setNWeapons(0);
        }
        // now collect a hash of all the same weapons in each location by id
        Map<String, Integer> groups = new HashMap<String, Integer>();
        for (Integer fId : fighters) {
            Entity entity = game.getEntity(fId);
            IAero fighter = (IAero) entity;
            if (fighter.getFCSHits() > 2) {
                // can't fire with no more FCS
                continue;
            }
            for (Mounted mounted : entity.getWeaponGroupList()) {
                if (mounted.isHit() || mounted.isDestroyed()) {
                    continue;
                }
                int loc = mounted.getLocation();
                if (entity instanceof LandAirMech) {
                    loc = LandAirMech.getAeroLocation(loc);
                }
                String key = mounted.getType().getInternalName() + ":" + loc;
                if (null == groups.get(key)) {
                    groups.put(key, mounted.getNWeapons());
                } else if (!mounted.getType().hasFlag(WeaponType.F_SPACE_BOMB)) {
                    groups.put(key, groups.get(key) + mounted.getNWeapons());
                }
            }
        }
        // now we just need to traverse the hash and either update our existing
        // equipment or add new ones if there is none
        Set<String> newSet = groups.keySet();
        Iterator<String> newIter = newSet.iterator();
        while (newIter.hasNext()) {
            String key = newIter.next();
            if (null != weaponGroups.get(key)) {
                // then this equipment is already loaded, so we just need to
                // correctly update the number of weapons
                this.getEquipment(weaponGroups.get(key)).setNWeapons(groups.get(key));
            } else {
                // need to add a new weapon
                String name = key.split(":")[0];
                int loc = Integer.parseInt(key.split(":")[1]);
                EquipmentType etype = EquipmentType.get(name);
                Mounted newmount;
                if (etype != null) {
                    try {
                        newmount = addWeaponGroup(etype, loc);
                        newmount.setNWeapons(groups.get(key));
                        weaponGroups.put(key, getEquipmentNum(newmount));
                    } catch (LocationFullException ex) {
                        System.out.println("Unable to compile weapon groups"); //$NON-NLS-1$
                        ex.printStackTrace();
                        return;
                    }
                } else if (name != "0") {
                    addFailedEquipment(name);
                }
            }
        }
        // make sure to set all the UACs and RACs to rapid fire
        setRapidFire();
    }

    /**
     * When fighters are removed it is necessary to unlink all ammo to the
     * squadron's weapons and reload it to ensure that ammo from the removed
     * fighter does not remain linked
     */
    // TODO: Evaluate for removal
    @SuppressWarnings("unused")
    private void reloadAllWeapons() {
        for (Mounted weapon : getTotalWeaponList()) {
            if ((((WeaponType) weapon.getType()).getAmmoType() != AmmoType.T_NA) && (null != weapon.getLinked())
                    && (weapon.getLinked().getType() instanceof AmmoType)) {
                weapon.unlink();
            }
        }
    }

    /**
     * update the skills for this squadron
     */
    public void updateSkills() {
        List<Entity> activeFighters = getActiveSubEntities().orElse(Collections.emptyList());
        if(activeFighters.isEmpty()) {
            return;
        }
        int pilotingTotal = 0;
        int gunneryTotal = 0;
        int gunneryLTotal = 0;
        int gunneryMTotal = 0;
        int gunneryBTotal = 0;
        for(Entity fighter : activeFighters) {
            pilotingTotal += fighter.getCrew().getPiloting();
            gunneryTotal += fighter.getCrew().getGunnery();
            gunneryLTotal += fighter.getCrew().getGunneryL();
            gunneryMTotal += fighter.getCrew().getGunneryM();
            gunneryBTotal += fighter.getCrew().getGunneryB();
        }
        getCrew().setPiloting(pilotingTotal / activeFighters.size(), 0);
        getCrew().setGunnery(gunneryTotal / activeFighters.size(), 0);
        getCrew().setGunneryL(gunneryLTotal / activeFighters.size(), 0);
        getCrew().setGunneryM(gunneryMTotal / activeFighters.size(), 0);
        getCrew().setGunneryB(gunneryBTotal / activeFighters.size(), 0);
    }

    @Override
    public ArrayList<Mounted> getAmmo() {
        ArrayList<Mounted> allAmmo = new ArrayList<Mounted>();
        for (Integer fId : fighters) {
            Entity fighter = game.getEntity(fId);
            allAmmo.addAll(fighter.getAmmo());
        }
        return allAmmo;
    }

    @Override
    public void useFuel(int fuel) {
        for (Integer fId : fighters) {
            IAero fighter = (IAero) game.getEntity(fId);
            fighter.useFuel(fuel);
        }
    }

    @Override
    public void autoSetMaxBombPoints() {
        maxBombPoints = Integer.MAX_VALUE;
        for (Integer fId : fighters) {
            Entity fighter = game.getEntity(fId);
            int currBombPoints = (int) Math.round(fighter.getWeight() / 5);
            maxBombPoints = Math.min(maxBombPoints, currBombPoints);
        }
    }

    @Override
    public void setBombChoices(int[] bc) {
        // Set the bombs for the squadron
        if (bc.length == bombChoices.length) {
            bombChoices = bc;
        }
        // Update each fighter in the squadron
        for (Integer fId : fighters) {
            IBomber fighter = (IBomber) game.getEntity(fId);
            fighter.setBombChoices(bc);
        }
    }

    /**
     * Produce an int array of the number of bombs of each type based on the
     * current bomblist. Since this is a FighterSquadron, these numbers
     * represent the number of bombs in a salvo. That is, it is a count of the
     * number of fighters in the squadron that have a bomb of the particular
     * type mounted.
     *
     * @return
     */
    @Override
    public int[] getBombLoadout() {
        int[] loadout = new int[BombType.B_NUM];
        for (Integer fId : fighters) {
            Entity fighter = (Entity) game.getEntity(fId);
            for (Mounted m : fighter.getBombs()) {
                loadout[((BombType) m.getType()).getBombType()]++;
            }
        }
        return loadout;
    }

    @Override
    public void applyBombs() {
        // Make sure all of the aeros have their bombs applied, otherwise
        // problems
        // once the bombs are applied, the choices are cleared, so it's not an
        // issue if the bombs are applied twice for an Aero
        for (Integer fId : fighters) {
            IBomber fighter = (IBomber) game.getEntity(fId);
            fighter.applyBombs();
        }
        computeSquadronBombLoadout();
    }

    /**
     * This method looks at the bombs equipped on all the fighters in the
<<<<<<< HEAD
     * squadron and determines what possible bombing attacks the squadrons can
     * make.
=======
     * squadron and determines what possible bombing attacks the squadrons
     * can make.
     * 
     * TODO: Make this into a generic "clean up bomb loadout" method
>>>>>>> master
     */
    public void computeSquadronBombLoadout() {
        // Remove any currently equipped bombs
        for (Mounted bomb : bombList) {
            equipmentList.remove(bomb);
        }
        bombList.clear();

        // Find out what bombs everyone has
        for (int btype = 0; btype < BombType.B_NUM; btype++) {
            // This is smallest number of such a bomb
            int maxBombCount = 0;
            for (Integer fId : fighters) {
                int bombCount = 0;
                Entity fighter = game.getEntity(fId);
                for (Mounted m : fighter.getBombs()) {
                    if (((BombType) m.getType()).getBombType() == btype) {
                        bombCount++;
                    }
                }
                maxBombCount = Math.max(bombCount, maxBombCount);
            }
            bombChoices[btype] = maxBombCount;
        }

        // Now that we know our bomb choices, load 'em
        int gameTL = TechConstants.getSimpleLevel(game.getOptions().stringOption("techlevel"));
        for (int type = 0; type < BombType.B_NUM; type++) {
            for (int i = 0; i < bombChoices[type]; i++) {
                if ((type == BombType.B_ALAMO)
                        && !game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_AT2_NUKES)) {
                    continue;
                }
                if ((type > BombType.B_TAG) && (gameTL < TechConstants.T_SIMPLE_ADVANCED)) {
                    continue;
                }

                // some bombs need an associated weapon and if so
                // they need a weapon for each bomb
                if ((null != BombType.getBombWeaponName(type)) && (type != BombType.B_ARROW)
                        && (type != BombType.B_HOMING)) {
                    try {
                        addBomb(EquipmentType.get(BombType.getBombWeaponName(type)), LOC_NOSE);
                    } catch (LocationFullException ex) {
                        // throw new LocationFullException(ex.getMessage());
                    }
                }
                // If the bomb was added as a weapon, don't add the ammo
                // The ammo will end up never getting removed from the squadron
                // because it doesn't count as a weapon.
                if ((type != BombType.B_TAG) && (null == BombType.getBombWeaponName(type))) {
                    try {
                        addEquipment(EquipmentType.get(BombType.getBombInternalName(type)), LOC_NOSE, false);
                    } catch (LocationFullException ex) {
                        // throw new LocationFullException(ex.getMessage());
                    }
                }
            }
            // Clear out the bomb choice once the bombs are loaded
            bombChoices[type] = 0;
        }
        // add the space bomb attack
        if (game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_SPACE_BOMB)
                && game.getBoard().inSpace() && (getBombs(AmmoType.F_SPACE_BOMB).size() > 0)) {
            try {
                addEquipment(EquipmentType.get(SPACE_BOMB_ATTACK), LOC_NOSE, false);
            } catch (LocationFullException ex) {
                // throw new LocationFullException(ex.getMessage());
            }
        }
        if (!game.getBoard().inSpace() && (getBombs(AmmoType.F_GROUND_BOMB).size() > 0)) {
            try {
                addEquipment(EquipmentType.get(DIVE_BOMB_ATTACK), LOC_NOSE, false);
            } catch (LocationFullException ex) {
                // throw new LocationFullException(ex.getMessage());
            }
            for (int i = 0; i < Math.min(10, getBombs(AmmoType.F_GROUND_BOMB).size()); i++) {
                try {
                    addEquipment(EquipmentType.get(ALT_BOMB_ATTACK), LOC_NOSE, false);
                } catch (LocationFullException ex) {
                    // throw new LocationFullException(ex.getMessage());
                }
            }
        }

        updateWeaponGroups();
        loadAllWeapons();
    }

    /*
     * Determine MAX_SIZE based on game options
     */
    public int getMaxSize() {
        if (game.getOptions().booleanOption(
                OptionsConstants.ADVAERORULES_ALLOW_LARGE_SQUADRONS)) {
            return ALTERNATE_MAX_SIZE;
        }
        return MAX_SIZE;
    }

    /*
     * The transporter functions
     */

    /**
     * Determines if this object can accept the given unit. The unit may not be
     * of the appropriate type or there may be no room for the unit.
     *
     * @param unit
     *            - the <code>Entity</code> to be loaded.
     * @return <code>true</code> if the unit can be loaded, <code>false</code>
     *         otherwise.
     */
    @Override
    public boolean canLoad(Entity unit, boolean checkFalse) {
        // We must have enough space for the new fighter.
        if(!unit.isEnemyOf(this) && unit.isFighter() && (fighters.size() < getMaxSize())) {
            return true;
        }
        // fighter squadrons can also load other fighter squadrons provided
        // there is enough space
        // and the loadee is not empty
        if ((unit instanceof FighterSquadron)
                && !unit.isEnemyOf(this)
                && (getId() != unit.getId())
                && (((FighterSquadron) unit).fighters.size() > 0)
                && ((fighters.size() + ((FighterSquadron) unit).fighters.size()) <= getMaxSize())) {
            return true;
        }

        return false;
    }

    /**
     * Load the given unit.
     *
     * @param unit
     *            - the <code>Entity</code> to be loaded.
     * @exception -
     *                If the unit can't be loaded, an
     *                <code>IllegalArgumentException</code> exception will be
     *                thrown.
     */
    @Override
    public void load(Entity unit, boolean checkFalse) throws IllegalArgumentException {
        // If we can't load the unit, throw an exception.
        if (!canLoad(unit)) {
            throw new IllegalArgumentException("Can not load " + unit.getShortName() + " into this squadron. ");
        }
        // if this is a fighter squadron then we actually need to load the
        // individual units
        if (unit instanceof FighterSquadron) {
            fighters.addAll(((FighterSquadron) unit).fighters);
        } else {
            // Add the unit to our squadron.
            fighters.addElement(unit.getId());
        }
        if (game.getPhase() != Phase.PHASE_LOUNGE) {
            computeSquadronBombLoadout();
            // updateWeaponGroups() and loadAllWeapons() are called in
            // computeSquadronBombLoadout()
        } else {
            updateWeaponGroups();
            loadAllWeapons();
        }
        updateSkills();
    }

    /**
     * We need to override this function to make sure the proper load method
     * gets called in some cases, but Squadrons can't have bays, so we can just
     * ignore the bay number.
     */
    @Override
    public void load(Entity unit, boolean checkFalse, int bayNumber) {
        load(unit, checkFalse);
    }

    /**
     * Unload the given unit. TODO: need to strip out ammo
     *
     * @param unit
     *            - the <code>Entity</code> to be unloaded.
     * @return <code>true</code> if the unit was contained in this space,
     *         <code>false</code> otherwise.
     */
    @Override
    public boolean unload(Entity unit) {
        // Remove the unit if we are carrying it.
        boolean success = fighters.removeElement(unit.getId());
        if (game.getPhase() != Phase.PHASE_LOUNGE) {
            computeSquadronBombLoadout();
            // updateWeaponGroups() and loadAllWeapons() are called in
            // computeSquadronBombLoadout()
        } else {
            updateWeaponGroups();
            loadAllWeapons();
        }
        updateSkills();
        return success;
    }

    /**
     * Get a <code>List</code> of the units currently loaded into this payload.
     *
     * @return A <code>List</code> of loaded <code>Entity</code> units. This
     *         list will never be <code>null</code>, but it may be empty. The
     *         returned <code>List</code> is independant from the under- lying
     *         data structure; modifying one does not affect the other.
     */
    @Override
    public Vector<Entity> getLoadedUnits() {
        return fighters.stream().map(fid -> game.getEntity(fid))
            .collect(Collectors.toCollection(Vector::new));
    }

    /**
     * Return a string that identifies the unused capacity of this transporter.
     *
     * @return A <code>String</code> meant for a human.
     */
    @Override
    public String getUnusedString() {
        return " - " + (getMaxSize() - fighters.size()) + " units";
    }

    @Override
    public double getUnused() {
        return getMaxSize() - fighters.size();
    }

    /**
     * Returns the current amount of cargo space for an entity of the given
     * type.
     * 
     * @param e
     *            An entity that defines the unit class
     * @return The number of units of the given type that can be loaded in this
     *         Entity
     * 
     *         TODO: Fix this so we can't actually "load" warships or
     *         tele-operated missiles into fighter squadrons ...
     */
    @Override
    public double getUnused(Entity e) {
        if (e.isFighter()) {
            return getUnused();
        } else {
            return 0;
        }
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
    @Override
    public boolean isWeaponBlockedAt(int loc, boolean isRear) {
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
    @Override
    public Entity getExteriorUnitAt(int loc, boolean isRear) {
        return null;
    }

    @Override
    public int getCargoMpReduction() {
        return 0;
    }

    @Override
    public long getEntityType() {
        return Entity.ETYPE_AERO | Entity.ETYPE_FIGHTER_SQUADRON;
    }

    @Override
    public Engine getEngine() {
        return null;
    }
    
    @Override
    public boolean hasEngine() {
        return false;
    }

    /**
     * Get the movement mode of the entity, based on consensus.
     */
    @Override
    public EntityMovementMode getMovementMode() {
        if (fighters.size() < 1) {
            return EntityMovementMode.NONE;
        }
        EntityMovementMode moveMode = game.getEntity(fighters.get(0)).getMovementMode();
        for (Integer fId : fighters) {
            Entity fighter = game.getEntity(fId);
            if (moveMode != fighter.getMovementMode()) {
                System.out.println("Error: Fighter squadron movement mode " + "doesn't agree!");
                return EntityMovementMode.NONE;
            }
        }
        return moveMode;
    }
    
    @Override
    public Optional<List<Entity>> getSubEntities() {
        return Optional.of(fighters.stream().map(fid -> game.getEntity(fid))
            .collect(Collectors.toList()));
    }
    
    @Override
    public Optional<List<Entity>> getActiveSubEntities() {
        return Optional.of(fighters.stream().map(fid -> game.getEntity(fid))
            .filter(ACTIVE_CHECK)
            .collect(Collectors.toList()));
    }

}
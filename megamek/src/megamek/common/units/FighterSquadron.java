/*
 * Copyright (c) 2007 - Jay Lawson
 * Copyright (C) 2008-2023-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.units;

import java.io.Serial;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Vector;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import megamek.client.ui.clientGUI.calculationReport.CalculationReport;
import megamek.common.HitData;
import megamek.common.LosEffects;
import megamek.common.MPCalculationSetting;
import megamek.common.bays.CargoBay;
import megamek.common.compute.Compute;
import megamek.common.cost.CostCalculator;
import megamek.common.enums.AimingMode;
import megamek.common.equipment.*;
import megamek.common.equipment.enums.BombType;
import megamek.common.equipment.enums.BombType.BombTypeEnum;
import megamek.common.exceptions.LocationFullException;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.rolls.PilotingRollData;
import megamek.logging.MMLogger;

/**
 * Fighter squadrons are basically "containers" for a bunch of fighters.
 *
 * @author Jay Lawson
 */
public class FighterSquadron extends AeroSpaceFighter {
    private static final MMLogger LOGGER = MMLogger.create(FighterSquadron.class);

    @Serial
    private static final long serialVersionUID = 3491212296982370726L;

    public static final int MAX_SIZE = 6;
    // Value is arbitrary, but StratOps shows up to 10, so we'll use that as an
    // alternate MAX_SIZE
    // when using the option for larger squadrons
    public static final int ALTERNATE_MAX_SIZE = 10;

    private static final Predicate<Entity> ACTIVE_CHECK = ent -> !((ent == null) || ent.isDestroyed()
          || ent.isDoomed());

    private final List<Integer> fighters = new ArrayList<>();

    // fighter squadrons need to keep track of heat capacity apart from their
    // fighters
    private int heatCapacity = 0;
    private int heatCapacityNoRHS = 0;

    public FighterSquadron() {
        super();
        setChassis("Squadron");
        setModel("");
    }

    public FighterSquadron(String name) {
        super();
        setChassis(name.trim() + " Squadron");
        setModel("");
    }

    @Override
    public double getCost(CalculationReport calcReport, boolean ignoreAmmo) {
        CostCalculator.addNoReportNote(calcReport, this);
        return getSubEntities().stream()
              .mapToDouble(entity -> entity.getCost(ignoreAmmo))
              .sum();
    }

    @Override
    public boolean isCapitalFighter() {
        return true;
    }

    @Override
    public int getOSI() {
        return getActiveSubEntities().stream()
              .mapToInt(ent -> ((IAero) ent).getOSI())
              .min()
              .orElse(0);
    }

    @Override
    public int getSI() {
        return getActiveSubEntities().stream()
              .mapToInt(ent -> ((IAero) ent).getSI())
              .min()
              .orElse(0);
    }

    @Override
    public int getTotalArmor() {
        return getSubEntities().stream()
              .mapToInt(entity -> ((IAero) entity).getCapArmor())
              .sum();
    }

    @Override
    public int getTotalOArmor() {
        return getSubEntities().stream()
              .mapToInt(entity -> ((IAero) entity).getCap0Armor())
              .sum();
    }

    /**
     * Per SO, fighter squadrons can't actually be crippled Individual crippled fighters should be detached and sent
     * home, but it isn't required by the rules
     *
     * @see Aero#isCrippled()
     */
    @Override
    public boolean isCrippled(boolean checkCrew) {
        return false;
    }

    @Override
    public double getArmorRemainingPercent() {
        if (getTotalOArmor() == 0) {
            return IArmorState.ARMOR_NA;
        }
        return ((double) getTotalArmor() / (double) getTotalOArmor());
    }

    @Override
    public int getWalkMP(MPCalculationSetting mpCalculationSetting) {
        return getActiveSubEntities().stream()
              .mapToInt(ent -> ent.getWalkMP(mpCalculationSetting))
              .min()
              .orElse(0);
    }

    @Override
    public int getCurrentThrust() {
        return getActiveSubEntities().stream()
              .mapToInt(ent -> ((IAero) ent).getCurrentThrust())
              .min()
              .orElse(0);
    }

    @Override
    public int getFuel() {
        return getActiveSubEntities().stream()
              .mapToInt(ent -> ((IAero) ent).getFuel())
              .min()
              .orElse(0);
    }

    @Override
    public int getCurrentFuel() {
        return getActiveSubEntities().stream()
              .mapToInt(ent -> ((IAero) ent).getCurrentFuel())
              .min()
              .orElse(0);
    }

    /**
     * Squadrons have an SI for PSR purposes, but don't take SI damage. This should return 100%.
     */
    @Override
    public double getInternalRemainingPercent() {
        return 1.0;
    }

    @Override
    public boolean hasTargComp() {
        List<Entity> activeFighters = getActiveSubEntities();
        long tcCount = activeFighters.stream().filter(Entity::hasTargComp).count();
        return (2 * tcCount >= activeFighters.size());
    }

    @Override
    public boolean hasActiveECM() {
        if (isSpaceborne() && isActiveOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_ECM)) {
            return getActiveSubEntities().stream().anyMatch(Entity::hasActiveECM);
        } else {
            return super.hasActiveECM();
        }
    }

    @Override
    public boolean loadedUnitsHaveActiveECM() {
        return true;
    }

    @Override
    public PilotingRollData addEntityBonuses(PilotingRollData prd) {
        // movement effects
        // some question whether "above safe thrust" applies to thrust or velocity
        // I will treat it as thrust until it is resolved
        if (moved == EntityMovementType.MOVE_OVER_THRUST) {
            prd.addModifier(+1, "Used more than safe thrust");
        }
        int vel = getCurrentVelocity();
        int velocityModifier = vel - (2 * getWalkMP());
        if (!isSpaceborne() && (velocityModifier > 0)) {
            prd.addModifier(velocityModifier, "Velocity greater than 2x safe thrust");
        }

        // add in atmospheric effects later
        PlanetaryConditions conditions = game.getPlanetaryConditions();
        if (!(isSpaceborne()
              || conditions.getAtmosphere().isVacuum())) {
            prd.addModifier(+2, "Atmospheric operations");
            prd.addModifier(-1, "fighter/ small craft");
        }

        // according to personal communication with Welshman, the normal crit
        // penalties are added up across the fighter squadron
        fighters.stream()
              .map(fid -> game.getEntity(fid))
              .filter(ACTIVE_CHECK).map(ent -> (IAero) ent).filter(Objects::nonNull)
              .forEachOrdered(
                    ent -> {
                        int avionicsHits = ent.getAvionicsHits();
                        if ((avionicsHits > 0) && (avionicsHits < 3)) {
                            prd.addModifier(avionicsHits, "Avionics Damage");
                        } else if (avionicsHits >= 3) {
                            // this should probably be replaced with some kind of AVI_DESTROYED boolean
                            prd.addModifier(5, "Avionics Destroyed");
                        }

                        // life support (only applicable to non-ASFs)
                        if (!ent.hasLifeSupport()) {
                            prd.addModifier(2, "No life support");
                        }

                        if (((Entity) ent).hasModularArmor()) {
                            prd.addModifier(1, "Modular Armor");
                        }
                    });
        return prd;
    }

    @Override
    public int getClusterMods() {
        return getActiveSubEntities().stream()
              .filter(ent -> (((IAero) ent).getFCSHits() <= 2))
              .mapToInt(ent -> ((IAero) ent).getClusterMods())
              .sum();
    }

    @Override
    public int doBattleValueCalculation(boolean ignoreC3, boolean ignoreSkill, CalculationReport calculationReport) {
        int bv = 0;
        for (Entity fighter : getActiveSubEntities()) {
            bv += fighter.calculateBattleValue(ignoreC3, ignoreSkill);
        }
        return bv;
    }

    @Override
    public int getHeatSinks() {
        return getActiveSubEntities().stream().mapToInt(ent -> ((IAero) ent).getHeatSinks()).sum();
    }

    @Override
    public int getHeatCapacity(final boolean includeRadicalHeatSink) {
        return includeRadicalHeatSink ? heatCapacity : heatCapacityNoRHS;
    }

    public void resetHeatCapacity() {
        List<Entity> activeFighters = getActiveSubEntities();
        heatCapacity = activeFighters.stream().mapToInt(ent -> ent.getHeatCapacity(true)).sum();
        heatCapacityNoRHS = activeFighters.stream().mapToInt(ent -> ent.getHeatCapacity(false)).sum();
    }

    @Override
    public double getWeight() {
        return getActiveSubEntities().stream().mapToDouble(Entity::getWeight).sum();
    }

    @Override
    public HitData rollHitLocation(int table, int side, int aimedLocation, AimingMode aimingMode,
          int cover) {
        List<Entity> activeFighters = getActiveSubEntities();

        // If this squadron is doomed or is of size 1 then just return the first one
        if (isDoomed() || (activeFighters.size() <= 1)) {
            return new HitData(0);
        }

        // Pick a random number between 0 and the number of fighters in the squadron.
        int hit = Compute.randomInt(activeFighters.size());
        return new HitData(hit);
    }

    @Override
    public HitData rollHitLocation(int table, int side) {
        return rollHitLocation(table, side, LOC_NONE, AimingMode.NONE, LosEffects.COVER_NONE);
    }

    @Override
    public void newRound(int roundNumber) {
        super.newRound(roundNumber);
        updateWeaponGroups();
        updateSensors();
        loadAllWeapons();
        updateSkills();
        resetHeatCapacity();
    }

    /**
     * Update sensors. Use the active sensor of the first fighter in the squadron that hasn't taken 3 sensor hits BAPs
     * don't count as active sensors in space, but they do make detection rolls easier
     */
    public void updateSensors() {
        if (getActiveSensor() == null) {
            for (Entity entity : getActiveSubEntities()) {
                Aero fighter = (Aero) entity;
                if (fighter.getSensorHits() > 2) {
                    // Sensors destroyed. Check the next fighter
                    continue;
                }
                if (fighter.getActiveSensor() != null) {
                    if (fighter.getActiveSensor().isBAP()) {
                        // BAP active. Check the next fighter
                        continue;
                    }
                    for (Sensor sensor : fighter.getSensors()) {
                        getSensors().add(sensor);
                    }
                    setNextSensor(getSensors().firstElement());
                    break;
                }
            }
        }
    }

    /**
     * instead of trying to track the individual units weapons, just recompile the weapon groups for this squadron each
     * round
     */
    @Override
    public void updateWeaponGroups() {
        // first we need to reset all the weapons in our existing mounts to zero
        // until proven otherwise
        for (Integer group : weaponGroups.values()) {
            Mounted<?> groupEquipment = getEquipment(group);
            if (groupEquipment != null) {
                groupEquipment.setNWeapons(0);
            }
        }
        // now collect a hash of all the same weapons in each location by id
        Map<String, Integer> groups = new HashMap<>();
        for (Entity entity : getActiveSubEntities()) {
            IAero fighter = (IAero) entity;
            if (fighter.getFCSHits() > 2) {
                // can't fire with no more FCS
                continue;
            }
            for (Mounted<?> mounted : entity.getWeaponGroupList()) {
                if (mounted.isHit() || mounted.isDestroyed()) {
                    continue;
                }
                int loc = mounted.getLocation();
                if (entity instanceof LandAirMek) {
                    loc = LandAirMek.getAeroLocation(loc);
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
        for (String key : newSet) {
            if (null != weaponGroups.get(key) && null != getEquipment(weaponGroups.get(key))) {
                // then this equipment is already loaded, so we just need to
                // correctly update the number of weapons
                getEquipment(weaponGroups.get(key)).setNWeapons(groups.get(key));
            } else {
                // need to add a new weapon
                String name = key.split(":")[0];
                int loc = Integer.parseInt(key.split(":")[1]);
                EquipmentType etype = EquipmentType.get(name);
                WeaponMounted newMount;
                if (etype != null) {
                    try {
                        newMount = addWeaponGroup(etype, loc);
                        newMount.setNWeapons(groups.get(key));
                        weaponGroups.put(key, getEquipmentNum(newMount));
                    } catch (LocationFullException ex) {
                        LOGGER.error("Unable to compile weapon groups.", ex);
                        return;
                    }
                } else if (!Objects.equals(name, "0")) {
                    addFailedEquipment(name);
                }
            }
        }
        // make sure to set all the UACs and RACs to rapid fire
        setRapidFire();
    }

    public void updateSkills() {
        List<Entity> activeFighters = getActiveSubEntities();
        if (activeFighters.isEmpty()) {
            return;
        }
        int pilotingTotal = 0;
        int gunneryTotal = 0;
        int gunneryLTotal = 0;
        int gunneryMTotal = 0;
        int gunneryBTotal = 0;
        for (Entity fighter : activeFighters) {
            pilotingTotal += fighter.getCrew().getPiloting();
            gunneryTotal += fighter.getCrew().getGunnery();
            if (fighter.gameOptions().booleanOption(OptionsConstants.RPG_RPG_GUNNERY)) {
                gunneryLTotal += fighter.getCrew().getGunneryL();
                gunneryMTotal += fighter.getCrew().getGunneryM();
                gunneryBTotal += fighter.getCrew().getGunneryB();
            } else {
                gunneryLTotal = gunneryTotal;
                gunneryMTotal = gunneryTotal;
                gunneryBTotal = gunneryTotal;
            }
        }
        getCrew().setPiloting(pilotingTotal / activeFighters.size(), 0);
        getCrew().setGunnery(gunneryTotal / activeFighters.size(), 0);
        getCrew().setGunneryL(gunneryLTotal / activeFighters.size(), 0);
        getCrew().setGunneryM(gunneryMTotal / activeFighters.size(), 0);
        getCrew().setGunneryB(gunneryBTotal / activeFighters.size(), 0);
    }

    @Override
    public List<AmmoMounted> getAmmo() {
        List<AmmoMounted> allAmmo = new ArrayList<>();
        getActiveSubEntities().forEach(fighter -> allAmmo.addAll(fighter.getAmmo()));
        return allAmmo;
    }

    @Override
    public void useFuel(int fuel) {
        for (Entity fighter : getActiveSubEntities()) {
            ((IAero) fighter).useFuel(fuel);
        }
    }

    @Override
    public void autoSetMaxBombPoints() {
        maxExtBombPoints = maxIntBombPoints = Integer.MAX_VALUE;
        for (Entity fighter : getSubEntities()) {
            // External bomb points
            int currBombPoints = (int) Math.round(fighter.getWeight() / 5);
            maxExtBombPoints = Math.min(maxExtBombPoints, currBombPoints);
            // Internal (cargo bay) bomb points; requires IBB to utilize
            currBombPoints = getTransportBays().stream()
                  .filter(tb -> tb instanceof CargoBay)
                  .mapToInt(tb -> (int) Math.floor(tb.getUnused()))
                  .sum();
            maxIntBombPoints = Math.min(maxIntBombPoints, currBombPoints);
        }
    }

    @Override
    public void setBombChoices(BombLoadout bc) {
        // Set the bombs for the squadron
        extBombChoices = new BombLoadout(bc);
        // Update each fighter in the squadron
        for (Entity bomber : getSubEntities()) {
            ((IBomber) bomber).setBombChoices(bc);
        }
    }

    /**
     * Produce an int array of the number of bombs of each type based on the current bomb list. Since this is a
     * FighterSquadron, these numbers represent the number of bombs in a salvo. That is, it is a count of the number of
     * fighters in the squadron that have a bomb of the particular type mounted.
     */
    @Override
    public BombLoadout getBombLoadout() {
        BombLoadout loadout = new BombLoadout();
        for (Entity fighter : getSubEntities()) {
            for (Mounted<?> m : fighter.getBombs()) {
                loadout.addBombs(((BombType) m.getType()).getBombType(), 1);
            }
        }
        return loadout;
    }

    @Override
    public void applyBombs() {
        // Make sure all the aerospace have their bombs applied, otherwise problems
        // once the bombs are applied, the choices are cleared, so it's not an
        // issue if the bombs are applied twice for an Aero
        for (Entity fighter : getSubEntities()) {
            ((IBomber) fighter).applyBombs();
        }
        computeSquadronBombLoadout();
    }

    /**
     * This method looks at the bombs equipped on all the fighters in the squadron and determines what possible bombing
     * attacks the squadrons can make.
     * <p>
     * TODO: Make this into a generic "clean up bomb loadout" method
     */
    public void computeSquadronBombLoadout() {
        clearBombs();

        // Find out what bombs everyone has
        BombLoadout squadronCapabilities = calculateSquadronBombCapabilities();
        extBombChoices = new BombLoadout(squadronCapabilities);

        // Now that we know our bomb choices, load 'em
        loadBombEquipment(squadronCapabilities);

        // Add special attack types
        addSpaceBombAttack();
        addGroundBombAttacks();

        // Finalization
        updateWeaponGroups();
        loadAllWeapons();
    }

    /**
     * Calculates the maximum bomb capabilities of the squadron by analyzing each fighter's bomb loadout.
     *
     * @return BombLoadout representing the squadron's maximum bomb capabilities
     */
    private BombLoadout calculateSquadronBombCapabilities() {
        BombLoadout capabilities = new BombLoadout();

        // For each bomb type, find the maximum count across all fighters
        for (BombTypeEnum bombType : BombTypeEnum.values()) {
            if (bombType == BombTypeEnum.NONE) {continue;}

            int maxBombCount = getSubEntities().stream()
                  .mapToInt(fighter -> countBombsOfType(fighter, bombType))
                  .max()
                  .orElse(0);

            if (maxBombCount > 0) {
                capabilities.put(bombType, maxBombCount);
            }
        }

        return capabilities;
    }

    /**
     * Counts the number of bombs of a specific type on a fighter.
     *
     * @param fighter  The fighter entity to check
     * @param bombType The type of bomb to count
     *
     * @return The number of bombs of the specified type
     */
    private int countBombsOfType(Entity fighter, BombTypeEnum bombType) {
        return (int) fighter.getBombs().stream()
              .filter(mounted -> mounted.getType().getBombType() == bombType)
              .count();
    }

    /**
     * Loads bomb equipment onto the squadron based on calculated capabilities.
     *
     * @param capabilities The squadron's bomb capabilities
     */
    private void loadBombEquipment(BombLoadout capabilities) {
        for (Map.Entry<BombTypeEnum, Integer> entry : capabilities.entrySet()) {
            BombTypeEnum bombType = entry.getKey();
            int count = entry.getValue();

            if (!bombType.isAllowedByGameOptions(game.getOptions())) {
                continue;
            }

            for (int i = 0; i < count; i++) {
                try {
                    // Add weapon if bomb type requires one
                    if (requiresWeapon(bombType)) {
                        try {
                            EquipmentType weaponType = EquipmentType.get(bombType.getWeaponName());
                            if (weaponType != null) {
                                addBomb(weaponType, LOC_NOSE);
                            }
                        } catch (Exception ignored) {
                            LOGGER.warn("Failed to add bomb ammo for type for Requires weapon: {}",
                                  bombType.getDisplayName());
                        }
                    }

                    // Add ammo/equipment if bomb type requires it
                    if (requiresAmmo(bombType)) {
                        try {
                            EquipmentType ammoType = EquipmentType.get(bombType.getInternalName());
                            if (ammoType != null) {
                                addEquipment(ammoType, LOC_NOSE, false);
                            }
                        } catch (Exception ignored) {
                            LOGGER.warn("Failed to add bomb ammo for type for Requires Ammo: {}",
                                  bombType.getDisplayName());
                        }
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to add bomb equipment for type: {}, iteration: {}",
                          bombType.getDisplayName(), i, e);
                }
            }
        }

        // Clear bomb choices after loading equipment
        extBombChoices.clear();
    }

    /**
     * Checks if a bomb type requires a weapon to be added.
     * TODO: Maybe this should be moved to BombTypeEnum?
     *
     * @param bombType The bomb type to check
     *
     * @return true if a weapon is required
     */
    private boolean requiresWeapon(BombTypeEnum bombType) {
        return (bombType.getWeaponName() != null) &&
              (bombType != BombTypeEnum.ARROW) &&
              (bombType != BombTypeEnum.HOMING);
    }

    /**
     * Checks if a bomb type requires ammo to be added.
     * TODO: Maybe this should be moved to BombTypeEnum?
     *
     * @param bombType The bomb type to check
     *
     * @return true if ammo is required
     */
    private boolean requiresAmmo(BombTypeEnum bombType) {
        return (bombType != BombTypeEnum.TAG) && (bombType.getWeaponName() == null);
    }

    /**
     * Adds space bomb attack if conditions are met.
     */
    private void addSpaceBombAttack() {
        if (isActiveOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_SPACE_BOMB) &&
              isSpaceborne() &&
              !getBombs(AmmoType.F_SPACE_BOMB).isEmpty()) {

            try {
                addEquipment(EquipmentType.get(SPACE_BOMB_ATTACK), LOC_NOSE, false);
            } catch (Exception ignored) {
                LOGGER.warn("Failed to add space bomb attack - Space Bomb Attack");
            }
        }
    }

    /**
     * Adds ground bomb attacks if conditions are met.
     */
    private void addGroundBombAttacks() {
        if (isSpaceborne() || getBombs(AmmoType.F_GROUND_BOMB).isEmpty()) {
            return;
        }

        // Add dive bomb attack
        try {
            addEquipment(EquipmentType.get(DIVE_BOMB_ATTACK), LOC_NOSE, false);
        } catch (Exception ignored) {
            LOGGER.warn("Failed to add dive bomb attack - Ground Bomb Attack");
        }

        // Add altitude bomb attacks (up to 10)
        int bombCount = Math.min(10, getBombs(AmmoType.F_GROUND_BOMB).size());
        for (int i = 0; i < bombCount; i++) {
            try {
                addEquipment(EquipmentType.get(ALT_BOMB_ATTACK), LOC_NOSE, false);
            } catch (Exception ignored) {
                LOGGER.warn("Failed to add altitude bomb attack {}", i);
            }
        }
    }

    /**
     * @return The maximum fighter count of a fighter squadron. This depends on game options ("Large Squadrons").
     */
    public int getMaxSize() {
        return gameOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_ALLOW_LARGE_SQUADRONS)
              ? ALTERNATE_MAX_SIZE
              : MAX_SIZE;
    }

    @Override
    public boolean canLoad(Entity unit, boolean checkFalse) {
        if (!unit.isEnemyOf(this) && unit.isFighter() && (fighters.size() < getMaxSize())) {
            return true;
        }
        // fighter squadrons can also load other fighter squadrons provided there is
        // enough space
        // and the loaded is not empty
        return (unit instanceof FighterSquadron)
              && !unit.isEnemyOf(this)
              && (getId() != unit.getId())
              && !((FighterSquadron) unit).fighters.isEmpty()
              && ((fighters.size() + ((FighterSquadron) unit).fighters.size()) <= getMaxSize());
    }

    @Override
    public void load(Entity unit, boolean checkFalse, int bayNumber) throws IllegalArgumentException {
        if (!canLoad(unit)) {
            throw new IllegalArgumentException("Can not load " + unit.getShortName() + " into this squadron. ");
        }
        // if this is a fighter squadron then we actually need to load the individual
        // units
        if (unit instanceof FighterSquadron) {
            fighters.addAll(((FighterSquadron) unit).fighters);
        } else {
            // Add the unit to our squadron.
            fighters.add(unit.getId());
        }
        // FighterSquadrons should handle this collectively
        unit.setTransportId(id);

        if (!getGame().getPhase().isLounge()) {
            computeSquadronBombLoadout(); // this calls updateWeaponGroups() and loadAllWeapons()
        } else {
            updateWeaponGroups();
            loadAllWeapons();
        }
        updateSkills();
    }

    @Override
    public boolean unload(Entity unit) {
        // TODO: need to strip out ammo
        boolean success = fighters.remove((Integer) unit.getId());
        if (!getGame().getPhase().isLounge()) {
            computeSquadronBombLoadout(); // this calls updateWeaponGroups() and loadAllWeapons()
        } else {
            updateWeaponGroups();
            loadAllWeapons();
        }
        updateSkills();
        unit.setTransportId(Entity.NONE);
        return success;
    }

    @Override
    public Vector<Entity> getLoadedUnits() {
        return new Vector<>(getSubEntities());
    }

    @Override
    public String getUnusedString() {
        return " - " + (getMaxSize() - fighters.size()) + " units";
    }

    @Override
    public double getUnused() {
        return getMaxSize() - fighters.size();
    }

    @Override
    public double getUnused(Entity e) {
        if (e.isFighter()) {
            return getUnused();
        } else {
            return 0;
        }
    }

    @Override
    public boolean isWeaponBlockedAt(int loc, boolean isRear) {
        return false;
    }

    @Override
    public Entity getExteriorUnitAt(int loc, boolean isRear) {
        return null;
    }

    @Override
    public int getCargoMpReduction(Entity carrier) {
        return 0;
    }

    @Override
    public long getEntityType() {
        return super.getEntityType() | Entity.ETYPE_FIGHTER_SQUADRON;
    }

    @Override
    public Engine getEngine() {
        return null;
    }

    @Override
    public boolean hasEngine() {
        return false;
    }

    @Override
    public EntityMovementMode getMovementMode() {
        List<Entity> entities = getSubEntities();

        if (entities.isEmpty()) {
            return EntityMovementMode.NONE;
        }

        EntityMovementMode moveMode = entities.get(0).getMovementMode();
        for (Entity fighter : entities) {
            if (moveMode != fighter.getMovementMode()) {
                LOGGER.error("Error: Fighter squadron movement mode doesn't agree!");
                return EntityMovementMode.NONE;
            }
        }
        return moveMode;
    }

    @Override
    public List<Entity> getSubEntities() {
        return fighters.stream().map(fid -> game.getEntity(fid))
              .filter(Objects::nonNull)
              .collect(Collectors.toList());
    }

    @Override
    public List<Entity> getActiveSubEntities() {
        return fighters.stream().map(fid -> game.getEntity(fid))
              .filter(Objects::nonNull)
              .filter(ACTIVE_CHECK)
              .collect(Collectors.toList());
    }

    @Override
    public boolean isUnitGroup() {
        return true;
    }

    @Override
    public boolean isFighter() {
        return false;
    }

    @Override
    public boolean isCapitalScale() {
        return true;
    }

    /**
     * Override of Entity method. This needs to be set, or we can't do a reverse lookup from a Capital Fighter to its
     * Squadron.
     *
     * @param transportId - the <code>int</code> ID of our transport. The ID is
     *                    <b>not</b> validated. This value should be
     *                    <code>Entity.NONE</code> if this unit has been unloaded.
     */
    @Override
    public void setTransportId(int transportId) {
        fighters.stream().map(fid -> game.getEntity(fid)).filter(Objects::nonNull)
              .forEach(f -> f.setTransportId(transportId));
    }

    /**
     * Damage a capital fighter's weapons. WeaponGroups are damaged by critical hits. This matches up the individual
     * fighter's weapons and critical slots and damages those for MHQ resolution
     *
     * @param loc - Int corresponding to the location struck
     */
    public void damageCapFighterWeapons(int loc) {
        for (int fid : fighters) {
            Entity fighter = game.getEntity(fid);
            if (fighter instanceof AeroSpaceFighter aeroSpaceFighter) {
                aeroSpaceFighter.damageLocation(loc);
            }
        }
    }

    @Override
    public boolean isCarryableObject() {
        return false;
    }
}

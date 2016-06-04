/*
 * MegaMek - Copyright (C) 2003, 2004, 2005 Ben Mazur (bmazur@sev.org)
 * ScenarioLoader - Copyright (C) 2002 Josh Yockey
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
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

package megamek.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import megamek.client.ui.swing.util.ImageFileFactory;
import megamek.common.Aero;
import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.Board;
import megamek.common.Compute;
import megamek.common.Configuration;
import megamek.common.Coords;
import megamek.common.Crew;
import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.Game;
import megamek.common.HitData;
import megamek.common.IArmorState;
import megamek.common.IBoard;
import megamek.common.IGame;
import megamek.common.IPlayer;
import megamek.common.IStartingPositions;
import megamek.common.Infantry;
import megamek.common.MapSettings;
import megamek.common.Mech;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.Mounted;
import megamek.common.Player;
import megamek.common.Protomech;
import megamek.common.Tank;
import megamek.common.TechConstants;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.IOption;
import megamek.common.util.BoardUtilities;
import megamek.common.util.DirectoryItems;

public class ScenarioLoader {
    private static final String COMMENT_MARK = "#"; //$NON-NLS-1$
    
    private static final String SEPARATOR_PROPERTY = "="; //$NON-NLS-1$
    private static final String SEPARATOR_COMMA = ","; //$NON-NLS-1$
    private static final String SEPARATOR_SPACE = " "; //$NON-NLS-1$
    private static final String SEPARATOR_COLON = ":"; //$NON-NLS-1$
    private static final String SEPARATOR_UNDERSCORE = "_"; //$NON-NLS-1$

    private static final String FILE_SUFFIX_BOARD = ".board"; //$NON-NLS-1$

    private static final String PARAM_MMSVERSION = "MMSVersion"; //$NON-NLS-1$
    private static final String PARAM_GAME_OPTIONS_FILE = "GameOptionsFile"; //$NON-NLS-1$
    private static final String PARAM_GAME_EXTERNAL_ID = "ExternalId"; //$NON-NLS-1$
    private static final String PARAM_FACTIONS = "Factions"; //$NON-NLS-1$

    private static final String PARAM_MAP_WIDTH = "MapWidth"; //$NON-NLS-1$
    private static final String PARAM_MAP_HEIGHT = "MapHeight"; //$NON-NLS-1$
    private static final String PARAM_BOARD_WIDTH = "BoardWidth"; //$NON-NLS-1$
    private static final String PARAM_BOARD_HEIGHT = "BoardHeight"; //$NON-NLS-1$
    private static final String PARAM_BRIDGE_CF = "BridgeCF"; //$NON-NLS-1$
    private static final String PARAM_MAPS = "Maps"; //$NON-NLS-1$

    private static final String PARAM_TEAM = "Team"; //$NON-NLS-1$
    private static final String PARAM_LOCATION = "Location"; //$NON-NLS-1$
    private static final String PARAM_MINEFIELDS = "Minefields"; //$NON-NLS-1$
    private static final String PARAM_DAMAGE = "Damage"; //$NON-NLS-1$
    private static final String PARAM_SPECIFIC_DAMAGE = "DamageSpecific"; //$NON-NLS-1$
    private static final String PARAM_CRITICAL_HIT = "CritHit"; //$NON-NLS-1$
    private static final String PARAM_AMMO_AMOUNT = "SetAmmoTo"; //$NON-NLS-1$
    private static final String PARAM_AMMO_TYPE = "SetAmmoType"; //$NON-NLS-1$
    private static final String PARAM_PILOT_HITS = "PilotHits"; //$NON-NLS-1$
    private static final String PARAM_EXTERNAL_ID = "ExternalID"; //$NON-NLS-1$
    private static final String PARAM_ADVANTAGES = "Advantages"; //$NON-NLS-1$
    private static final String PARAM_AUTO_EJECT = "AutoEject"; //$NON-NLS-1$
    private static final String PARAM_COMMANDER = "Commander"; //$NON-NLS-1$
    private static final String PARAM_DEPLOYMENT_ROUND = "DeploymentRound"; //$NON-NLS-1$
    private static final String PARAM_CAMO = "Camo"; //$NON-NLS-1$
    private static final String PARAM_ALTITUDE = "Altitude"; //$NON-NLS-1$

    private static final String MAP_RANDOM = "RANDOM"; //$NON-NLS-1$

    private final File scenarioFile;
    // copied from ChatLounge.java
    private final List<DamagePlan> damagePlans = new ArrayList<>();
    // Used to store Crit Hits
    private final List<CritHitPlan> critHitPlans = new ArrayList<>();
    // Used to set ammo Spec Ammounts
    private final List<SetAmmoPlan> ammoPlans = new ArrayList<>();
    private DirectoryItems camos;

    public ScenarioLoader(File f) {
        scenarioFile = f;
        try {
            camos = new DirectoryItems(Configuration.camoDir(), "", ImageFileFactory.getInstance()); //$NON-NLS-1$
        } catch (Exception e) {
            camos = null;
        }
    }
    
    // TODO: legal/valid ammo type handling and game options, since they are set at this point
    private AmmoType getValidAmmoType(IGame game, Mounted mounted, String ammoString) {
        final Entity e = mounted.getEntity();
        final int year = game.getOptions().intOption("year"); //$NON-NLS-1$
        final EquipmentType currentAmmoType = mounted.getType();
        final Mounted currentWeapon = mounted.getLinkedBy();
        final EquipmentType currentWeaponType = (null != currentWeapon) ? currentWeapon.getType() : null;
        final EquipmentType newAmmoType = EquipmentType.get(ammoString);
        if(null == newAmmoType) {
            System.err.println(String.format("Ammo type '%s' not found", ammoString)); //$NON-NLS-1$
            return null;
        }
        if(!(newAmmoType instanceof AmmoType)) {
            System.err.println(String.format("Equipment %s is not an ammo type", newAmmoType.getName())); //$NON-NLS-1$
            return null;
        }
        if(!TechConstants.isLegal(
            TechConstants.getGameTechLevel(game, e.isClan()),
            newAmmoType.getTechLevel(year), true,
            e.isMixedTech())) {
            System.out.println(String.format("Ammo %s (TL %d) is not legal for year %d (TL %d)", //$NON-NLS-1$
                newAmmoType.getName(), newAmmoType.getTechLevel(year), year,
                TechConstants.getGameTechLevel(game, e.isClan())));
            return null;
        }
        if(e.isClan() && !game.getOptions().booleanOption("clan_ignore_eq_limits")) { //$NON-NLS-1$
            // Check for clan weapon restrictions
            final long muniType = ((AmmoType) newAmmoType).getMunitionType() & ~AmmoType.M_INCENDIARY_LRM;
            if((muniType == AmmoType.M_SEMIGUIDED)
                || (muniType == AmmoType.M_SWARM_I)
                || (muniType == AmmoType.M_FLARE)
                || (muniType == AmmoType.M_FRAGMENTATION)
                || (muniType == AmmoType.M_THUNDER_AUGMENTED)
                || (muniType == AmmoType.M_THUNDER_INFERNO)
                || (muniType == AmmoType.M_THUNDER_VIBRABOMB)
                || (muniType == AmmoType.M_THUNDER_ACTIVE)
                || (muniType == AmmoType.M_INFERNO_IV)
                || (muniType == AmmoType.M_VIBRABOMB_IV)
                || (muniType == AmmoType.M_LISTEN_KILL)
                || (muniType == AmmoType.M_ANTI_TSM) 
                || (muniType == AmmoType.M_SMOKE_WARHEAD)) {
                System.out.println(String.format("Ammo type %s not allowed by Clan rules", //$NON-NLS-1$
                    newAmmoType.getName()));
                return null;
            }
        }
        if(AmmoType.canDeliverMinefield((AmmoType) newAmmoType)
            && !game.getOptions().booleanOption("minefields")) { //$NON-NLS-1$
            System.out.println(String.format("Minefield-creating ammo type %s forbidden by game rules", //$NON-NLS-1$
                newAmmoType.getName()));
            return null;
        }
        int weaponAmmoType = (currentWeaponType instanceof WeaponType) ? ((WeaponType) currentWeaponType).getAmmoType() : 0;
        if((((AmmoType) newAmmoType).getRackSize() == ((AmmoType) currentAmmoType).getRackSize())
            && (newAmmoType.hasFlag(AmmoType.F_BATTLEARMOR) == currentAmmoType.hasFlag(AmmoType.F_BATTLEARMOR))
            && (newAmmoType.hasFlag(AmmoType.F_ENCUMBERING) == currentAmmoType.hasFlag(AmmoType.F_ENCUMBERING))
            && (newAmmoType.getTonnage(e) == currentAmmoType.getTonnage(e))
            && (((AmmoType) newAmmoType).getAmmoType() == weaponAmmoType)) {
            return (AmmoType) newAmmoType;
        } else {
            return null;
        }
    }

    /**
     * The damage procedures are built into a server object, so we delay dealing
     * the random damage until a server is made available to us.
     */
    public void applyDamage(Server s) {
        for(DamagePlan damagePlan : damagePlans) {
            System.out.println(String.format("Applying damage to %s", damagePlan.entity.getShortName())); //$NON-NLS-1$
            for(int y = 0; y < damagePlan.nBlocks; ++ y) {
                HitData hit = damagePlan.entity.rollHitLocation(ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT);
                System.out.println("[s.damageEntity(dp.entity, hit, 5)]"); //$NON-NLS-1$
                s.damageEntity(damagePlan.entity, hit, 5);
            }

            // Apply Spec Damage
            for(SpecDam specDamage : damagePlan.specificDammage) {
                if(damagePlan.entity.locations() <= specDamage.loc) {
                    // location is valid
                    System.err.println(String.format("\tInvalid location specified %d", specDamage.loc)); //$NON-NLS-1$
                } else {
                    // Infantry only take damage to "internal"
                    if(specDamage.internal
                        || ((damagePlan.entity instanceof Infantry) && !(damagePlan.entity instanceof BattleArmor))) {
                        if(damagePlan.entity.getOInternal(specDamage.loc) > specDamage.setArmorTo) {
                            damagePlan.entity.setInternal(specDamage.setArmorTo, specDamage.loc);
                            System.out.println(String.format("\tSet armor value for (internal %s) to %d", //$NON-NLS-1$
                                damagePlan.entity.getLocationName(specDamage.loc), specDamage.setArmorTo));
                            if(specDamage.setArmorTo == 0) {
                                System.out.println(String.format("\tSection destoyed %s", //$NON-NLS-1$
                                    damagePlan.entity.getLocationName(specDamage.loc)));
                                damagePlan.entity.destroyLocation(specDamage.loc);
                            }
                        }
                    } else {
                        if(specDamage.rear && damagePlan.entity.hasRearArmor(specDamage.loc)) {
                            if(damagePlan.entity.getOArmor(specDamage.loc, true) > specDamage.setArmorTo) {
                                System.out.println(String.format("\tSet armor value for (rear %s) to %d", //$NON-NLS-1$
                                    damagePlan.entity.getLocationName(specDamage.loc), specDamage.setArmorTo));
                                damagePlan.entity.setArmor(specDamage.setArmorTo, specDamage.loc, true);
                            }
                        } else {
                            if(damagePlan.entity.getOArmor(specDamage.loc, false) > specDamage.setArmorTo) {
                                System.out.println(String.format("\tSet armor value for (%s) to %d", //$NON-NLS-1$
                                    damagePlan.entity.getLocationName(specDamage.loc), specDamage.setArmorTo));

                                // Battle Armor Handled Differently
                                // If armor set to Zero kill the Armor sport
                                // which represents one member of the squad
                                if(damagePlan.entity instanceof BattleArmor) {
                                    if(specDamage.setArmorTo == 0) {
                                        damagePlan.entity.setArmor(IArmorState.ARMOR_DOOMED,specDamage.loc, false);
                                        damagePlan.entity.setInternal(IArmorState.ARMOR_DOOMED,specDamage.loc);
                                    } else {
                                        // For some reason setting armor to 1 will result in 2 armor points
                                        // left on the GUI. Dont know why but adjust here!
                                        damagePlan.entity.setArmor(specDamage.setArmorTo - 1, specDamage.loc);
                                    }
                                } else {
                                    damagePlan.entity.setArmor(specDamage.setArmorTo, specDamage.loc);
                                }
                            }
                        }
                    }
                }
            }
        }

        // Loop throught Crit Hits
        for(CritHitPlan chp : critHitPlans) {
            System.out.println(String.format("Applying critical hits to %s", //$NON-NLS-1$
                chp.entity.getShortName()));
            for(CritHit critHit : chp.critHits) {
                // Apply a critical hit to the indicated slot.
                if(chp.entity.locations() <= critHit.loc) {
                    System.err.println(String.format("\tInvalid location specified %d", //$NON-NLS-1$
                        critHit.loc));
                } else {
                    // Make sure that we have crit spot to hit
                    if((chp.entity instanceof Mech) || (chp.entity instanceof Protomech)) {
                        // Is this a torso weapon slot?
                        CriticalSlot cs = null;
                        if ((chp.entity instanceof Protomech)
                                && (Protomech.LOC_TORSO == critHit.loc)
                                && ((Protomech.SYSTEM_TORSO_WEAPON_A == critHit.slot) || (Protomech.SYSTEM_TORSO_WEAPON_B == critHit.slot))) {
                            cs = new CriticalSlot(CriticalSlot.TYPE_SYSTEM, critHit.slot);
                        }
                        // Is this a valid slot number?
                        else if((critHit.slot < 0)
                                || (critHit.slot > chp.entity.getNumberOfCriticals(critHit.loc))) {
                            System.err.println(String.format("%s - invalid slot specified %d: %d", //$NON-NLS-1$
                                chp.entity.getShortName(), critHit.loc, (critHit.slot + 1)));
                        }
                        // Get the slot from the entity.
                        else {
                            cs = chp.entity.getCritical(critHit.loc, critHit.slot);
                        }

                        // Ignore invalid, unhittable, and damaged slots.
                        if((null == cs) || !cs.isHittable()) {
                            System.err.println(String.format("%s - slot not hittable %d: %d", //$NON-NLS-1$
                                chp.entity.getShortName(), critHit.loc, (critHit.slot + 1)));
                        } else {
                            System.out.print("[s.applyCriticalHit(chp.entity, ch.loc, cs, false)]"); //$NON-NLS-1$
                            s.applyCriticalHit(chp.entity, critHit.loc, cs, false, 0, false);
                        }
                    }
                    // Handle Tanks differently.
                    else if(chp.entity instanceof Tank) {
                        if((critHit.slot < 0) || (critHit.slot >= 6)) {
                            System.err.println(String.format("%s - invalid slot specified %d: %d", //$NON-NLS-1$
                                chp.entity.getShortName(), critHit.loc, (critHit.slot + 1)));
                        } else {
                            CriticalSlot cs = new CriticalSlot(CriticalSlot.TYPE_SYSTEM, critHit.slot + 1);
                            System.out.print("[s.applyCriticalHit(chp.entity, ch.loc, cs, false)]"); //$NON-NLS-1$
                            s.applyCriticalHit(chp.entity, Entity.NONE, cs, false, 0, false);
                        }
                    }
                }
            }
        }

        // Loop throught Set Ammo To
        for(SetAmmoPlan sap : ammoPlans) {
            System.out.println(String.format("Applying ammo adjustment to %s", //$NON-NLS-1$
                sap.entity.getShortName()));
            for(SetAmmoType sa : sap.ammoSetType) {
                // Limit to 'Mechs for now (needs to be extended later)
                if(sap.entity instanceof Mech) {
                    if(sa.slot < sap.entity.getNumberOfCriticals(sa.loc)) {
                        CriticalSlot cs = sap.entity.getCritical(sa.loc, sa.slot);
                        if(null != cs) {
                            Mounted ammo = sap.entity.getCritical(sa.loc, sa.slot).getMount();
                            if(null == ammo) {
                                System.err.println(String.format("%s - invalid slot specified %d: %d", //$NON-NLS-1$
                                    sap.entity.getShortName(), sa.loc, sa.slot + 1));
                            } else if(ammo.getType() instanceof AmmoType) {
                                AmmoType newAmmoType = getValidAmmoType(s.getGame(), ammo, sa.type);
                                if(null != newAmmoType) {
                                    ammo.changeAmmoType(newAmmoType);
                                } else {
                                    System.out.println(String.format("Illegal ammo type '%s' for unit %s, slot %s", //$NON-NLS-1$
                                        sa.type, sap.entity.getDisplayName(), ammo.getName()));
                                }
                            }
                        }
                    }
                }
            }
            for(SetAmmoTo sa : sap.ammoSetTo) {
                // Only can be done against Mechs
                if(sap.entity instanceof Mech) {
                    if(sa.slot < sap.entity.getNumberOfCriticals(sa.loc)) {
                        // Get the piece of equipment and check to make sure it
                        // is a ammo item then set its amount!
                        CriticalSlot cs = sap.entity.getCritical(sa.loc, sa.slot);
                        if(null != cs) {
                            Mounted ammo = sap.entity.getCritical(sa.loc, sa.slot).getMount();
                            if(null == ammo) {
                                System.err.println(String.format("%s - invalid slot specified %d: %d", //$NON-NLS-1$
                                    sap.entity.getShortName(), sa.loc, sa.slot + 1));
                            } else if(ammo.getType() instanceof AmmoType) {
                                // Also make sure we dont exceed the max allowed
                                ammo.setShotsLeft(Math.min(sa.setAmmoTo, ammo.getBaseShotsLeft()));
                            }
                        }
                    }
                }
            }
        }
    }

    public IGame createGame() throws Exception {
        System.out.println(String.format("Loading scenario from %s", scenarioFile)); //$NON-NLS-1$
        StringMultiMap p = load();

        String sCheck = p.getString(PARAM_MMSVERSION);
        if (sCheck == null) {
            throw new ScenarioLoaderException("missingMMSVersion"); //$NON-NLS-1$
        }

        Game g = new Game();

        // build the board
        g.board = createBoard(p);

        // build the faction players
        Collection<Player> players = createPlayers(p);
        for(Player player : players) {
            g.addPlayer(player.getId(), player);
        }

        // build the entities
        int entityId = 0;
        for(Player player : players) {
            Collection<Entity> entities = buildFactionEntities(p, player);
            for(Entity entity : entities) {
                entity.setOwner(player);
                entity.setId(entityId);
                ++ entityId;
                g.addEntity(entity);
            }
        }
        // game's ready
        g.getOptions().initialize();
        String optionFile = p.getString(PARAM_GAME_OPTIONS_FILE);
        if (optionFile == null) {
            g.getOptions().loadOptions();
        } else {
            g.getOptions().loadOptions(new File(scenarioFile.getParentFile(), optionFile), true);
        }

        // set wind
        g.getPlanetaryConditions().determineWind();

        // Set up the teams (for initiative)
        g.setupTeams();

        g.setPhase(IGame.Phase.PHASE_STARTING_SCENARIO);

        g.setupRoundDeployment();

        // Read the external game id from the scenario file
        g.setExternalGameId(parseExternalGameId(p));

        g.setVictoryContext(new HashMap<String, Object>());
        g.createVictoryConditions();

        return g;
    }

    private Collection<Entity> buildFactionEntities(StringMultiMap p, IPlayer player) throws ScenarioLoaderException {
        String faction = player.getName();
        Pattern unitPattern = Pattern.compile(
            String.format("^Unit_\\Q%s\\E_[^_]+$", faction)); //$NON-NLS-1$
        Pattern unitDataPattern = Pattern.compile(
            String.format("^(Unit_\\Q%s\\E_[^_]+)_([A-Z][^_]+)$", faction)); //$NON-NLS-1$

        Map<String, Entity> entities = new HashMap<>();
        
        // Gather all defined units
        for(String key : p.keySet()) {
            if(unitPattern.matcher(key).matches() && (p.getNumValues(key) > 0)) {
                if(p.getNumValues(key) > 1) {
                    System.err.println(String.format("Scenario loading: Unit declaration %s found %d times", //$NON-NLS-1$
                        key, p.getNumValues(key)));
                    throw new ScenarioLoaderException("multipleUnitDeclarations", key); //$NON-NLS-1$
                }
                entities.put(key, parseEntityLine(p.getString(key)));
            }
        }
        
        // Add other information
        for(String key: p.keySet()) {
            Matcher dataMatcher = unitDataPattern.matcher(key);
            if(dataMatcher.matches()) {
                String unitKey = dataMatcher.group(1);
                if(!entities.containsKey(unitKey)) {
                    System.out.println(String.format("Scenario loading: Data for undeclared unit encountered, ignoring: %s", //$NON-NLS-1$
                        key));
                    continue;
                }
                Entity e = entities.get(unitKey);
                switch(dataMatcher.group(2)) {
                    case PARAM_DAMAGE:
                        for(String val : p.get(key)) {
                            damagePlans.add(new DamagePlan(e, Integer.parseInt(val)));
                        }
                        break;
                    case PARAM_SPECIFIC_DAMAGE:
                        DamagePlan dp = new DamagePlan(e);
                        for(String val : p.getString(key).split(SEPARATOR_COMMA, -1)) {
                            dp.addSpecificDamage(val);
                        }
                        damagePlans.add(dp);
                        break;
                    case PARAM_CRITICAL_HIT:
                        CritHitPlan chp = new CritHitPlan(e);
                        for(String val : p.getString(key).split(SEPARATOR_COMMA, -1)) {
                            chp.addCritHit(val);
                        }
                        critHitPlans.add(chp);
                        break;
                    case PARAM_AMMO_AMOUNT:
                        SetAmmoPlan amountSap = new SetAmmoPlan(e);
                        for(String val : p.getString(key).split(SEPARATOR_COMMA, -1)) {
                            amountSap.addSetAmmoTo(val);
                        }
                        ammoPlans.add(amountSap);
                        break;
                    case PARAM_AMMO_TYPE:
                        SetAmmoPlan typeSap = new SetAmmoPlan(e);
                        for(String val : p.getString(key).split(SEPARATOR_COMMA, -1)) {
                            typeSap.addSetAmmoType(val);
                        }
                        ammoPlans.add(typeSap);
                        break;
                    case PARAM_PILOT_HITS:
                        int hits = Integer.parseInt(p.getString(key));
                        e.getCrew().setHits(Math.min(hits, 5));
                        break;
                    case PARAM_EXTERNAL_ID:
                        e.setExternalIdAsString(p.getString(key));
                        break;
                    case PARAM_ADVANTAGES:
                        parseAdvantages(e, p.getString(key, SEPARATOR_SPACE));
                        break;
                    case PARAM_AUTO_EJECT:
                        parseAutoEject(e, p.getString(key));
                        break;
                    case PARAM_COMMANDER:
                        parseCommander(e, p.getString(key));
                        break;
                    case PARAM_DEPLOYMENT_ROUND:
                        int round = Integer.parseInt(p.getString(key));
                        if(round > 0) {
                            System.out.println(String.format("%s will be deployed before round %d", //$NON-NLS-1$
                                e.getDisplayName(), round));
                            e.setDeployRound(round);
                            e.setDeployed(false);
                            e.setNeverDeployed(false);
                            e.setPosition(null);
                        }
                        break;
                    case PARAM_CAMO:
                        parseCamo(e, p.getString(key));
                        break;
                    case PARAM_ALTITUDE:
                        int altitude = Math.min(Integer.parseInt(p.getString(key)), 10);
                        if(e instanceof Aero) {
                            e.setAltitude(altitude);
                            if(altitude <= 0) {
                                ((Aero) e).land();
                            }
                        } else {
                            System.out.println(String.format("Altitude setting for a non-aerospace unit %s; ignoring", //$NON-NLS-1$
                                e.getShortName()));
                        }
                        break;
                    default:
                        System.err.println(String.format("Scenario loading: Unknown unit data key %s", key)); //$NON-NLS-1$
                }
            }
        }
        
        return entities.values();
    }

    private Entity parseEntityLine(String s) throws ScenarioLoaderException {
        try {
            String[] parts = s.split(SEPARATOR_COMMA, -1);
            String sRef = parts[0];
            MechSummary ms = MechSummaryCache.getInstance().getMech(sRef);
            if (ms == null) {
                throw new ScenarioLoaderException("missingRequiredEntity", sRef); //$NON-NLS-1$
            }
            System.out.println(String.format("Loading %s", ms.getName())); //$NON-NLS-1$
            Entity e = new MechFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
            e.setCrew(new Crew(parts[1], 1, Integer.parseInt(parts[2]), Integer.parseInt(parts[3])));
            if(parts.length >= 7) {
                String direction = parts[4].toUpperCase(Locale.ROOT);
                switch(direction) {
                    case "N": //$NON-NLS-1$
                        e.setFacing(0);
                        break;
                    case "NW": //$NON-NLS-1$
                        e.setFacing(5);
                        break;
                    case "SW": //$NON-NLS-1$
                        e.setFacing(4);
                        break;
                    case "S": //$NON-NLS-1$
                        e.setFacing(3);
                        break;
                    case "SE": //$NON-NLS-1$
                        e.setFacing(2);
                        break;
                    case "NE": //$NON-NLS-1$
                        e.setFacing(1);
                        break;
                    default:
                        break;
                }
                int x = Integer.parseInt(parts[5]) - 1;
                int y = Integer.parseInt(parts[6]) - 1;
                Coords coords = new Coords(x, y);
                e.setPosition(coords);
                e.setDeployed(true);
            }
            return e;
        } catch (NumberFormatException | IndexOutOfBoundsException | EntityLoadingException ex) {
            ex.printStackTrace();
            throw new ScenarioLoaderException("unparsableEntityLine", s); //$NON-NLS-1$
        }
    }

    private void parseAdvantages(Entity entity, String adv) {
        String[] advantages = adv.split(SEPARATOR_SPACE, -1);

        for(String curAdv : advantages) {
            String[] advantageData = curAdv.split(SEPARATOR_COLON, -1);
            IOption option = entity.getCrew().getOptions().getOption(advantageData[0]);
            if(null == option) {
                System.err.println(String.format("Ignoring invalid pilot advantage '%s'", //$NON-NLS-1$
                    curAdv));
            } else {
                System.out.println(String.format("Adding pilot advantage '%s' to %s", //$NON-NLS-1$
                    curAdv, entity.getDisplayName()));
                if(advantageData.length > 1) {
                    option.setValue(advantageData[1]);
                } else {
                    option.setValue(true);
                }
            }
        }
    }

    private void parseAutoEject(Entity entity, String eject) {
        if (entity instanceof Mech) {
            Mech mech = (Mech) entity;
            mech.setAutoEject(Boolean.valueOf(eject).booleanValue());
        }
    }

    private void parseCommander(Entity entity, String commander) {
        entity.setCommander(Boolean.valueOf(commander).booleanValue());
    }

    private String getValidCamoGroup(String camoGroup) {
        // Translate base categories for userfriendliness.
        if(camoGroup.equals("No Camo") || camoGroup.equals("None")) { //$NON-NLS-1$ //$NON-NLS-2$
            camoGroup = IPlayer.NO_CAMO;
        } else if (camoGroup.equals("General")) { //$NON-NLS-1$
            camoGroup = IPlayer.ROOT_CAMO;
        } else {
            // If CamoGroup does not have a trailing slash, add one, since all
            // subdirectories require it
            if (camoGroup.charAt(camoGroup.length() - 1) != '/') {
                camoGroup += "/"; //$NON-NLS-1$
            }
        }
        
        boolean validGroup = false;

        if(camoGroup.equals(IPlayer.NO_CAMO) || camoGroup.equals(IPlayer.ROOT_CAMO)) {
            validGroup = true;
        } else {
            Iterator<String> catNames = camos.getCategoryNames();
            while (catNames.hasNext()) {
                String s = catNames.next();
                if (s.equals(camoGroup)) {
                    validGroup = true;
                }
            }
        }

        return validGroup ? camoGroup : null;
    }
    
    private String getValidCamoName(String camoGroup, String camoName) {
        boolean validName = false;

        // Validate CamoName
        if(camoGroup.equals(IPlayer.NO_CAMO)) {
            for(String color : IPlayer.colorNames) {
                if(camoName.equals(color)) {
                    validName = true;
                }
            }
        } else {
            Iterator<String> camoNames;
            if(camoGroup.equals(IPlayer.ROOT_CAMO)) {
                camoNames = camos.getItemNames(""); //$NON-NLS-1$
            } else {
                camoNames = camos.getItemNames(camoGroup);
            }
            while (camoNames.hasNext()) {
                String s = camoNames.next();
                if (s.equals(camoName)) {
                    validName = true;
                }
            }
        }
        
        return validName ? camoName : null;
    }
    
    /*
     * Camo Parser/Validator for Individual Entity Camo
     */
    private void parseCamo(Entity entity, String camoString) throws ScenarioLoaderException {
        String[] camoData = camoString.split(SEPARATOR_COMMA, -1);
        String camoGroup = getValidCamoGroup(camoData[0]);
        if(null == camoGroup) {
            throw new ScenarioLoaderException("invalidIndividualCamoGroup", //$NON-NLS-1$
                camoData[0], entity.getDisplayName());
        }
        String camoName = getValidCamoName(camoGroup, camoData[1]);
        if(null == camoName) {
            throw new ScenarioLoaderException("invalidIndividualCamoName", //$NON-NLS-1$
                camoData[1], camoGroup, entity.getDisplayName());
        }

        entity.setCamoCategory(camoGroup);
        entity.setCamoFileName(camoName);
    }

    /*
     * Camo Parser/Validator for Faction Camo
     */
    private void parseCamo(IPlayer player, String camoString) throws ScenarioLoaderException {
        String[] camoData = camoString.split(SEPARATOR_COMMA, -1);
        String camoGroup = getValidCamoGroup(camoData[0]);
        if(null == camoGroup) {
            throw new ScenarioLoaderException("invalidFactionCamoGroup", //$NON-NLS-1$
                camoData[0], player.getName());
        }
        String camoName = getValidCamoName(camoGroup, camoData[1]);
        if(null == camoName) {
            throw new ScenarioLoaderException("invalidFactionCamoName", //$NON-NLS-1$
                camoData[1], camoGroup, player.getName());
        }

        player.setCamoCategory(camoGroup);
        player.setCamoFileName(camoName);
    }

    private int findIndex(String[] sa, String s) {
        for (int x = 0; x < sa.length; x++) {
            if (sa[x].equalsIgnoreCase(s)) {
                return x;
            }
        }
        return -1;
    }

    private String getFactionParam(String faction, String param) {
        return param + SEPARATOR_UNDERSCORE + faction;
    }
    
    private Collection<Player> createPlayers(StringMultiMap p) throws ScenarioLoaderException {
        String sFactions = p.getString(PARAM_FACTIONS);
        if((null == sFactions) || sFactions.isEmpty()) {
            throw new ScenarioLoaderException("missingFactions"); //$NON-NLS-1$
        }
        String[] factions = sFactions.split(SEPARATOR_COMMA, -1);
        List<Player> result = new ArrayList<>(factions.length);
        
        int playerId = 0;
        int teamId = 0;
        for(String faction : factions) {
            Player player = new Player(playerId, faction);
            result.add(player);
            ++ playerId;
            
            // scenario players start out as ghosts to be logged into
            player.setGhost(true);
            
            String loc = p.getString(getFactionParam(faction, PARAM_LOCATION));
            if(null == loc) {
                loc = "Any"; //$NON-NLS-1$
            }
            int dir = Math.max(findIndex(IStartingPositions.START_LOCATION_NAMES, loc), 0);
            player.setStartingPos(dir);
            
            String camo = p.getString(getFactionParam(faction, PARAM_CAMO));
            if((null != camo) && !camo.isEmpty()) {
                parseCamo(player, camo);
            }
            
            String team = p.getString(getFactionParam(faction, PARAM_TEAM));
            if((null != team) && !team.isEmpty()) {
                try {
                    teamId = Integer.parseInt(team);
                } catch(NumberFormatException nfex) {
                    ++ teamId;
                }
            } else {
                ++ teamId;
            }
            player.setTeam(Math.min(teamId, IPlayer.MAX_TEAMS - 1));
            
            String minefields = p.getString(getFactionParam(faction, PARAM_MINEFIELDS));
            if((null != minefields) && !minefields.isEmpty()) {
                String[] mines = minefields.split(SEPARATOR_COMMA, -1);
                if(mines.length >= 3) {
                    try {
                        int minesConventional = Integer.parseInt(mines[0]);
                        int minesCommand = Integer.parseInt(mines[1]);
                        int minesVibra = Integer.parseInt(mines[2]);
                        player.setNbrMFConventional(minesConventional);
                        player.setNbrMFCommand(minesCommand);
                        player.setNbrMFVibra(minesVibra);
                    } catch(NumberFormatException nfex) {
                        System.err.println(String.format("Format error with minefields string '%s' for %s", //$NON-NLS-1$
                            minefields, faction));
                    }
                }
            }
        }
        
        return result;
    }

    /**
     * Load board files and create the megaboard.
     */
    private IBoard createBoard(StringMultiMap p) throws ScenarioLoaderException {
        int mapWidth = 16, mapHeight = 17;
        if(null == p.getString(PARAM_MAP_WIDTH)) {
            System.out.println(String.format("No map width specified; using %d", mapWidth)); //$NON-NLS-1$
        } else {
            mapWidth = Integer.parseInt(p.getString(PARAM_MAP_WIDTH));
        }

        if(null == p.getString(PARAM_MAP_HEIGHT)) {
            System.out.println(String.format("No map height specified; using %d", mapHeight)); //$NON-NLS-1$
        } else {
            mapHeight = Integer.parseInt(p.getString(PARAM_MAP_HEIGHT));
        }

        int nWidth = 1, nHeight = 1;
        if(null == p.getString(PARAM_BOARD_WIDTH)) {
            System.out.println(String.format("No board width specified; using %d", nWidth)); //$NON-NLS-1$
        } else {
            nWidth = Integer.parseInt(p.getString(PARAM_BOARD_WIDTH));
        }

        if(null == p.getString(PARAM_BOARD_HEIGHT)) {
            System.out.println(String.format("No board height specified; using %d", nHeight)); //$NON-NLS-1$
        } else {
            nHeight = Integer.parseInt(p.getString(PARAM_BOARD_HEIGHT));
        }

        System.out.println(String.format("Mapsheets are %d by %d hexes.", mapWidth, mapHeight)); //$NON-NLS-1$
        System.out.println(String.format("Constructing %d by %d board.", nWidth, nHeight)); //$NON-NLS-1$
        int cf = 0;
        if(null == p.getString(PARAM_BRIDGE_CF)) {
            System.out.println("No CF for bridges defined. Using map file defaults."); //$NON-NLS-1$
        } else {
            cf = Integer.parseInt(p.getString(PARAM_BRIDGE_CF));
            System.out.println(String.format("Overriding map-defined bridge CFs with %d.", cf)); //$NON-NLS-1$
        }
        // load available boards
        // basically copied from Server.java. Should get moved somewhere neutral
        List<String> boards = new ArrayList<>();

        for(String file : Configuration.boardsDir().list()) {
            if(file.toLowerCase(Locale.ROOT).endsWith(FILE_SUFFIX_BOARD)) {
                boards.add(file.substring(0, file.length() - FILE_SUFFIX_BOARD.length()));
            }
        }

        IBoard[] ba = new IBoard[nWidth * nHeight];
        Queue<String> maps = new LinkedList<>(
            Arrays.asList(p.getString(PARAM_MAPS).split(SEPARATOR_COMMA, -1)));
        List<Boolean> rotateBoard = new ArrayList<>();
        for(int x = 0; x < nWidth; x++) {
            for(int y = 0; y < nHeight; y++) {
                int n = (y * nWidth) + x;
                String board = MAP_RANDOM;
                if(!maps.isEmpty()) {
                    board = maps.poll();
                }
                System.out.println(String.format("(%d,%d) %s", x, y, board)); //$NON-NLS-1$

                boolean isRotated = false;
                if(board.startsWith(Board.BOARD_REQUEST_ROTATION)) {
                    isRotated = true;
                    board = board.substring(Board.BOARD_REQUEST_ROTATION.length());
                }

                String sBoardFile;
                if(board.equals(MAP_RANDOM)) {
                    sBoardFile = (boards.get(Compute.randomInt(boards.size()))) + FILE_SUFFIX_BOARD;
                } else {
                    sBoardFile = board + FILE_SUFFIX_BOARD;
                }
                File fBoard = new File(Configuration.boardsDir(), sBoardFile);
                if (!fBoard.exists()) {
                    throw new ScenarioLoaderException("nonexistantBoard", board); //$NON-NLS-1$
                }
                ba[n] = new Board();
                ba[n].load(new File(Configuration.boardsDir(), sBoardFile));
                if(cf > 0) {
                    ba[n].setBridgeCF(cf);
                }
                BoardUtilities.flip(ba[n], isRotated, isRotated);
                rotateBoard.add(isRotated);
            }
        }

        // if only one board just return it.
        if (ba.length == 1) {
            return ba[0];
        }
        // construct the big board
        return BoardUtilities.combine(mapWidth, mapHeight, nWidth, nHeight, ba,
                rotateBoard, MapSettings.MEDIUM_GROUND);
    }

    private StringMultiMap load() throws ScenarioLoaderException {
        StringMultiMap props = new StringMultiMap();
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(scenarioFile), "UTF-8"))) { //$NON-NLS-1$
            String line = null;
            int lineNum = 0;
            while(null != (line = reader.readLine())) {
                ++ lineNum;
                line = line.trim();
                if(line.startsWith(COMMENT_MARK) || (line.length() == 0)) {
                    continue;
                }
                if(!line.contains(SEPARATOR_PROPERTY)) {
                    System.err.println(String.format("Equality sign in scenario file %s on line %d missing; ignoring", //$NON-NLS-1$
                        scenarioFile, lineNum));
                    continue;
                }
                String elements[] = line.split(SEPARATOR_PROPERTY, -1);
                if(elements.length > 2) {
                    System.err.println(String.format("Multiple equality signs in scenario file %s on line %d; ignoring", //$NON-NLS-1$
                            scenarioFile, lineNum));
                        continue;
                }
                props.put(elements[0].trim(), elements[1].trim());
            }
        } catch(IOException e) {
            e.printStackTrace();
            throw new ScenarioLoaderException("exceptionReadingFile", scenarioFile); //$NON-NLS-1$
        }
        return props;
    }

    /**
     * Parses out the external game id from the scenario file
     */
    private int parseExternalGameId(StringMultiMap p) {
        String sExternalId = p.getString(PARAM_GAME_EXTERNAL_ID);
        int ExternalGameId = 0;
        if (sExternalId != null) {
            ExternalGameId = Integer.parseInt(sExternalId);
        }
        return ExternalGameId;
    }

    public static void main(String[] saArgs) throws Exception {
        ScenarioLoader sl = new ScenarioLoader(new File(saArgs[0]));
        IGame g = sl.createGame();
        if (g != null) {
            System.out.println("Successfully loaded."); //$NON-NLS-1$
        }
    }

    /**
     * This is used specify the critical hit location
     */
    private class CritHit {
        public int loc;
        public int slot;

        public CritHit(int l, int s) {
            loc = l;
            slot = s;
        }
    }

    /**
     * This class is used to store the critical hit plan for a entity it is
     * loaded from the scenario file. It contains a vector of CritHit.
     */
    private class CritHitPlan {
        public Entity entity;
        List<CritHit> critHits = new ArrayList<>();

        public CritHitPlan(Entity e) {
            entity = e;
        }

        public void addCritHit(String s) {
            int ewSpot = s.indexOf(':');
            int loc = Integer.parseInt(s.substring(0, ewSpot));
            int slot = Integer.parseInt(s.substring(ewSpot + 1));
            
            critHits.add(new CritHit(loc, slot - 1));
        }
    }

    /**
     * This is used to store the armor to change ammo at a given location
     */
    private class SetAmmoTo {
        public final int loc;
        public final int slot;
        public final int setAmmoTo;

        public SetAmmoTo(int loc, int slot, int setAmmoTo) {
            this.loc = loc;
            this.slot = slot;
            this.setAmmoTo = setAmmoTo;
        }
    }
    
    private class SetAmmoType {
        public final int loc;
        public final int slot;
        public final String type;
        
        public SetAmmoType(int loc, int slot, String type) {
            this.loc = loc;
            this.slot = slot;
            this.type = type;
        }

    }

    /**
     * This class is used to store the ammo Adjustments it is loaded from the
     * scenario file. It contains a vector of SetAmmoTo.
     */
    private class SetAmmoPlan {
        public final Entity entity;
        public final List<SetAmmoTo> ammoSetTo = new ArrayList<>();
        public final List<SetAmmoType> ammoSetType = new ArrayList<>();

        public SetAmmoPlan(Entity e) {
            entity = e;
        }

        /**
         * Converts 2:1-34 to Location 2 Slot 1 set Ammo to 34
         */
        public void addSetAmmoTo(String s) {
            int ewSpot = s.indexOf(':');
            int amSpot = s.indexOf('-');
            if(s.isEmpty() || (ewSpot < 1) || (amSpot < ewSpot)) {
                return;
            }
            int loc = Integer.parseInt(s.substring(0, ewSpot));
            int slot = Integer.parseInt(s.substring(ewSpot + 1, amSpot));
            int setTo = Integer.parseInt(s.substring(amSpot + 1));

            ammoSetTo.add(new SetAmmoTo(loc, slot - 1, setTo));
        }
        
        public void addSetAmmoType(String s) {
            int ewSpot = s.indexOf(':');
            int atSpot = s.indexOf('-');
            if(s.isEmpty() || (ewSpot < 1) || (atSpot < ewSpot)) {
                return;
            }
            int loc = Integer.parseInt(s.substring(0, ewSpot));
            int slot = Integer.parseInt(s.substring(ewSpot + 1, atSpot));
            
            ammoSetType.add(new SetAmmoType(loc, slot - 1, s.substring(atSpot + 1)));
        }
    }

    /**
     * This is used specify the one damage location
     */
    private class SpecDam {
        public final int loc;
        public final int setArmorTo;
        public final boolean rear;
        public final boolean internal;

        public SpecDam(int Location, int SetArmorTo, boolean RearHit,
                       boolean Internal) {
            loc = Location;
            setArmorTo = SetArmorTo;
            rear = RearHit;
            internal = Internal;
        }
    }

    /**
     * This class is used to store the damage plan for a entity it is loaded
     * from the scenario file. It contains a vector of SpecDam.
     */
    private class DamagePlan {
        public final Entity entity;
        public final int nBlocks;
        public final List<SpecDam> specificDammage = new ArrayList<>();

        public DamagePlan(Entity e, int n) {
            entity = e;
            nBlocks = n;
        }

        public DamagePlan(Entity e) {
            entity = e;
            nBlocks = 0;
        }

        /**
         * Converts N2:1 to Nornam hit to location 2 set armor to 1!
         */
        public void addSpecificDamage(String s) {
            int ewSpot = s.indexOf(':');
            if(s.isEmpty() || (ewSpot < 1)) {
                return;
            }
            int loc = Integer.parseInt(s.substring(1, ewSpot));
            int setTo = Integer.parseInt(s.substring(ewSpot + 1));
            boolean rear = (s.charAt(0) == 'R');
            boolean internal = (s.charAt(0) == 'I');
            
            specificDammage.add(new SpecDam(loc, setTo, rear, internal));
        }
    }
    
    private static class ScenarioLoaderException extends Exception {
        private static final long serialVersionUID = 8622648319531348199L;
        
        private final Object[] params;

        public ScenarioLoaderException(String errorKey) {
            super(errorKey);
            this.params = null;
        }
        
        public ScenarioLoaderException(String errorKey, Object ... params) {
            super(errorKey);
            this.params = params;
        }
        
        @Override
        public String getMessage() {
            String result = Messages.getString("ScenarioLoaderException." + super.getMessage()); //$NON-NLS-1$
            if(null != params) {
                try {
                    return String.format(result, params);
                } catch(IllegalFormatException ifex) {
                    // Ignore, return the base translation instead
                }
            }
            return result;
        }
    }
    
    private static class StringMultiMap extends HashMap<String, Collection<String>> {
        private static final long serialVersionUID = 2171662843329151622L;

        public void put(String key, String value) {
            Collection<String> values = get(key);
            if(null == values) {
                values = new ArrayList<>();
                put(key, values);
            }
            values.add(value);
        }

        public String getString(String key) {
            return getString(key, SEPARATOR_COMMA);
        }

        public String getString(String key, String separator) {
            Collection<String> values = get(key);
            if(null == values || values.size() == 0) {
                return null;
            }
            
            boolean firstElement = true;
            StringBuilder sb = new StringBuilder();
            for(String val : values) {
                if(firstElement) {
                    firstElement = false;
                } else {
                    sb.append(separator);
                }
                sb.append(val);
            }
            return sb.toString();
        }
        
        /** @return the number of values for this key in the file */
        public int getNumValues(String key) {
            Collection<String> values = get(key);
            return (null == values) ? 0 : values.size();
        }
    }
}

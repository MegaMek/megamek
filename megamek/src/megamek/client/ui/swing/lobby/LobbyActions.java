/*  
 * MegaMek - Copyright (C) 2021 - The MegaMek Team  
 *  
 * listener program is free software; you can redistribute it and/or modify it under  
 * the terms of the GNU General Public License as published by the Free Software  
 * Foundation; either version 2 of the License, or (at your option) any later  
 * version.  
 *  
 * listener program is distributed in the hope that it will be useful, but WITHOUT  
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS  
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more  
 * details.  
 */ 
package megamek.client.ui.swing.lobby;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Map.Entry;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import megamek.MegaMek;
import megamek.client.Client;
import megamek.client.generator.*;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.*;
import megamek.client.ui.swing.dialog.MMConfirmDialog;
import megamek.client.ui.swing.dialog.MMConfirmDialog.Response;
import megamek.client.ui.swing.dialog.imageChooser.CamoChooserDialog;
import megamek.common.*;
import megamek.common.enums.Gender;
import megamek.common.force.Force;
import megamek.common.force.Forces;
import megamek.common.icons.Camouflage;
import megamek.common.options.OptionsConstants;
import megamek.common.util.CollectionUtil;

import static megamek.client.ui.swing.lobby.LobbyUtility.*;
import static java.util.stream.Collectors.*;

/** This class contains the methods that act on entity changes from the pop-up menu and elsewhere. */
public class LobbyActions {

    private final ChatLounge lobby;
    private final IGame game;
    private final Client client;
    private final JFrame frame;
    private final IPlayer localPlayer;
    private final ClientGUI clientGui;

    private String cmdSelectedTab = null;   // TODO: required?

    /** This class contains the methods that act on entity changes from the pop-up menu and elsewhere. */
    LobbyActions(ChatLounge cl) {
        lobby = cl;
        game = lobby.game();
        client = lobby.client();
        clientGui = lobby.getClientgui();
        frame = lobby.getClientgui().getFrame();
        localPlayer = client.getLocalPlayer();
    }

    /** Sets a deployment round for the given entities. Sends an update to the server. */ 
    void applyDeployment(Collection<Entity> entities, int newRound) {
        if (!validateUpdate(entities)) {
            return;
        }
        Set<Entity> updateCandidates = new HashSet<>();
        for (Entity entity: entities) {
            if (entity.getDeployRound() != newRound) {
                entity.setDeployRound(newRound);
                updateCandidates.add(entity);
            }
        }
        sendUpdates(updateCandidates);
    }

    /** Sets starting heat for the given entities. Sends an update to the server. */
    void applyHeat(Collection<Entity> entities, int heat) {
        if (!validateUpdate(entities)) {
            return;
        }
        if (entities.stream().anyMatch(e -> !e.tracksHeat())) {
            LobbyErrors.showHeatTracking(frame);
            return;
        }
        Set<Entity> updateCandidates = new HashSet<>();
        for (Entity entity: entities) {
            if (entity.getHeat() != heat) {
                entity.heat = heat;
                updateCandidates.add(entity);
            }
        }
        sendUpdates(updateCandidates);
    }

    /** Sets/removes hidden deployment for the given entities. Sends an update to the server. */
    void applyHidden(Collection<Entity> entities, boolean newHidden) {
        if (!validateUpdate(entities)) {
            return;
        }
        Set<Entity> updateCandidates = new HashSet<>();
        for (Entity entity: entities) {
            if (entity.isHidden() != newHidden) {
                entity.setHidden(newHidden);
                updateCandidates.add(entity);
            }
        }
        sendUpdates(updateCandidates);
    }

    /** Sets deploy prone for the given entities. Sends an update to the server. */
    void applyProne(Collection<Entity> entities, String info) {
        if (!validateUpdate(entities)) {
            return;
        }
        if (entities.stream().anyMatch(e -> e.getUnitType() != UnitType.MEK)) {
            LobbyErrors.showOnlyMeks(frame);
            return;
        }
        Set<Entity> updateCandidates = new HashSet<>();
        boolean goProne = info.equals("PRONE");
        boolean goHullDown = info.equals("HULLDOWN");
        boolean stand = !goProne && !goHullDown;
        for (Entity entity: entities) {
            if ((goProne && !entity.isProne()) || (goHullDown && !entity.isHullDown())
                    || (stand && (entity.isProne() || entity.isHullDown()))) {
                entity.setProne(goProne);
                entity.setHullDown(goHullDown);
                updateCandidates.add(entity);
            }
        }
        sendUpdates(updateCandidates);
    }
    
    /**
     * Attaches the given force as a subforce to the given new parent. 
     * Does NOT work for newParentId == NO_FORCE. Use promoteForce to do this.
     * Does not allow attaching a force to one of its own subforces.
     */
    void attachForce(int forceId, int newParentId) {
        Forces forces = game.getForces();
        if (!forces.contains(forceId) || !forces.contains(newParentId)
                || (forceId == newParentId)) {
            return;
        }
        
        Force force = forces.getForce(forceId);
        Force newParent = forces.getForce(newParentId);
        List<Force> subForces = forces.getFullSubForces(force);
        IPlayer owner = forces.getOwner(force);
        IPlayer newParentOwner = forces.getOwner(newParent);
            
        if (owner.isEnemyOf(newParentOwner)) {
            LobbyErrors.showForceOnlyTeam(frame);;
            return;
        }
        if (subForces.contains(newParent)) {
            LobbyErrors.showForceNoAttachSubForce(frame);
            return;
        }
        if (!isEditable(force)) {
            LobbyErrors.showCannotConfigEnemies(frame);
            return;
        }

        List<Force> changedForces = forces.attachForce(force, newParent);
        if (!changedForces.isEmpty()) {
            correctSender(force).sendUpdateForce(changedForces);
        }
    }
    

    /**
     * Makes a force top-level, detaching it from any former parent. 
     */
    void promoteForce(Collection<Integer> forceIds) {
        Forces forces = game.getForces();
        if (forceIds.stream().anyMatch(id -> !forces.contains(id))) {
            return;
        }
        Set<Force> promForces = forceIds.stream().map(id -> forces.getForce(id)).collect(toSet());
        if (!areForcesEditable(promForces)) {
            LobbyErrors.showCannotConfigEnemies(frame);
            return;
        }
        Collection<Force> changedForces = new HashSet<Force>();
        promForces.stream().map(f -> forces.promoteForce(f)).forEach(changedForces::addAll);
        sendUpdates(new ArrayList<>(), changedForces);
    }

    /** Shows the dialog which allows adding pre-existing damage to units. */
    void configureDamage(Collection<Entity> entities) {
        if (entities.size() != 1) {
            LobbyErrors.showSingleUnit(frame, "assign damage");
            return;
        }
        if (!validateUpdate(entities)) {
            return;
        }
        Entity entity = CollectionUtil.randomElement(entities);
        UnitEditorDialog med = new UnitEditorDialog(frame, entity);
        med.setVisible(true);
        sendUpdates(entities);
    }

    /** 
     * Displays a CamoChooser to choose an individual camo for the given entities. 
     * The camo will only be applied to units configurable by the local player, 
     * i.e. his own units or those of his bots.
     */
    public void individualCamo(Collection<Entity> entities) {
        if (!validateUpdate(entities)) {
            return;
        }

        // Display the CamoChooser and await the result
        // The dialog is preset to a random entity's settings
        Entity entity = CollectionUtil.randomElement(entities);
        CamoChooserDialog ccd = new CamoChooserDialog(frame,
                entity.getOwner().getCamouflage(), entity.getCamouflage());
        if ((ccd.showDialog() == JOptionPane.CANCEL_OPTION) || (ccd.getSelectedItem() == null)) {
            return;
        }

        // Choosing the player camo resets the units to have no individual camo.
        Camouflage selectedItem = ccd.getSelectedItem();
        Camouflage ownerCamo = entity.getOwner().getCamouflage();
        boolean noIndividualCamo = selectedItem.equals(ownerCamo);

        // Update all allowed entities with the camo
        for (Entity ent: entities) {
            if (noIndividualCamo) {
                ent.setCamouflage(ownerCamo);
            } else {
                ent.setCamouflage(selectedItem);
            }
        }
        sendUpdates(entities);
    }

    /**
     * Configure multiple entities at once. Only affects deployment options.
     */
    public void customizeMechs(Collection<Entity> entities) {
        if (!validateUpdate(entities)) {
            return;
        }
        if (!haveSingleOwner(entities)) {
            LobbyErrors.showSingleOwnerRequired(frame);
            return;
        }
        Entity randomSelected = CollectionUtil.randomElement(entities);
        String ownerName = randomSelected.getOwner().getName();
        int ownerId = randomSelected.getOwner().getId();

        boolean editable = clientGui.getBots().get(ownerName) != null;
        Client client;
        if (editable) {
            client = clientGui.getBots().get(ownerName);
        } else {
            editable |= ownerId == localPlayer.getId();
            client = clientGui.getClient();
        }

        CustomMechDialog cmd = new CustomMechDialog(clientGui, client, new ArrayList<>(entities), editable);
        cmd.setSize(new Dimension(GUIPreferences.getInstance().getCustomUnitWidth(),
                GUIPreferences.getInstance().getCustomUnitHeight()));
        cmd.setTitle(Messages.getString("ChatLounge.CustomizeUnits")); 
        cmd.setVisible(true);
        GUIPreferences.getInstance().setCustomUnitHeight(cmd.getSize().height);
        GUIPreferences.getInstance().setCustomUnitWidth(cmd.getSize().width);
        if (editable && cmd.isOkay()) {
            // send changes
            for (Entity entity : entities) {
                // If a LAM with mechanized BA was changed to non-mech mode, unload the BA.
                if ((entity instanceof LandAirMech)
                        && entity.getConversionMode() != LandAirMech.CONV_MODE_MECH) {
                    for (Entity loadee : entity.getLoadedUnits()) {
                        entity.unload(loadee);
                        loadee.setTransportId(Entity.NONE);
                        client.sendUpdateEntity(loadee);
                    }
                }

                client.sendUpdateEntity(entity);

                // Changing state to a transporting unit can update state of
                // transported units, so update those as well
                for (Transporter transport : entity.getTransports()) {
                    for (Entity loaded : transport.getLoadedUnits()) {
                        client.sendUpdateEntity(loaded);
                    }
                }

                // Customizations to a Squadron can effect the fighters
                if (entity instanceof FighterSquadron) {
                    entity.getSubEntities().ifPresent(ents -> ents.forEach(client::sendUpdateEntity));
                }
            }
        }
        if (cmd.isOkay() && (cmd.getStatus() != CustomMechDialog.DONE)) {
            Entity nextEnt = cmd.getNextEntity(cmd.getStatus() == CustomMechDialog.NEXT);
            customizeMech(nextEnt);
        }
    }

    /**
     *
     * @param entity
     */
    public void customizeMech(Entity entity) {
        if (!validateUpdate(Arrays.asList(entity))) {
            return;
        }
        boolean editable = clientGui.getBots().get(entity.getOwner().getName()) != null;
        Client c;
        if (editable) {
            c = clientGui.getBots().get(entity.getOwner().getName());
        } else {
            editable |= entity.getOwnerId() == localPlayer.getId();
            c = clientGui.getClient();
        }
        // When we customize a single entity's C3 network setting,
        // **ALL** members of the network may get changed.
        Entity c3master = entity.getC3Master();
        ArrayList<Entity> c3members = new ArrayList<>();
        Iterator<Entity> playerUnits = c.getGame().getPlayerEntities(c.getLocalPlayer(), false).iterator();
        while (playerUnits.hasNext()) {
            Entity unit = playerUnits.next();
            if (!entity.equals(unit) && entity.onSameC3NetworkAs(unit)) {
                c3members.add(unit);
            }
        }

        boolean doneCustomizing = false;
        while (!doneCustomizing) {
            // display dialog
            List<Entity> entities = new ArrayList<>();
            entities.add(entity);
            CustomMechDialog cmd = new CustomMechDialog(clientGui, c, entities, editable);
            cmd.setSize(new Dimension(GUIPreferences.getInstance().getCustomUnitWidth(),
                    GUIPreferences.getInstance().getCustomUnitHeight()));
            cmd.refreshOptions();
            cmd.refreshQuirks();
            cmd.refreshPartReps();
            cmd.setTitle(entity.getShortName());
            if (cmdSelectedTab != null) {
                cmd.setSelectedTab(cmdSelectedTab);
            }
            cmd.setVisible(true);
            GUIPreferences.getInstance().setCustomUnitHeight(cmd.getSize().height);
            GUIPreferences.getInstance().setCustomUnitWidth(cmd.getSize().width);
            cmdSelectedTab = cmd.getSelectedTab();
            if (editable && cmd.isOkay()) {
                Set<Entity> updateCandidates = new HashSet<>();
                updateCandidates.add(entity);
                // If a LAM with mechanized BA was changed to non-mech mode, unload the BA.
                if ((entity instanceof LandAirMech)
                        && entity.getConversionMode() != LandAirMech.CONV_MODE_MECH) {
                    for (Entity loadee : entity.getLoadedUnits()) {
                        entity.unload(loadee);
                        loadee.setTransportId(Entity.NONE);
                        updateCandidates.add(loadee);
                    }
                }

                // Changing state to a transporting unit can update state of
                // transported units, so update those as well
                for (Transporter transport : entity.getTransports()) {
                    for (Entity loaded : transport.getLoadedUnits()) {
                        updateCandidates.add(loaded);
                    }
                }

                // Customizations to a Squadron can effect the fighters
                if (entity instanceof FighterSquadron) {
                    entity.getSubEntities().ifPresent(ents -> updateCandidates.addAll(ents));
                }

                // Do we need to update the members of our C3 network?
                if (((c3master != null) && !c3master.equals(entity.getC3Master()))
                        || ((c3master == null) && (entity.getC3Master() != null))) {
                    for (Entity unit : c3members) {
                        updateCandidates.add(unit);
                    }
                }
                sendUpdates(updateCandidates);
            }
            if (cmd.isOkay() && (cmd.getStatus() != CustomMechDialog.DONE)) {
                entity = cmd.getNextEntity(cmd.getStatus() == CustomMechDialog.NEXT);
            } else {
                doneCustomizing = true;
            }
        }
    }

    /** 
     * Sets random skills for the given entities, as far as they can
     * be configured by the local player. 
     */
    void setRandomSkills(Collection<Entity> entities) {
        if (!validateUpdate(entities)) {
            return;
        }
        for (Entity e: entities) {
            Client c = lobby.getLocalClient(e);
            for (int i = 0; i < e.getCrew().getSlotCount(); i++) {
                int[] skills = c.getRandomSkillsGenerator().getRandomSkills(e, true);
                e.getCrew().setGunnery(skills[0], i);
                e.getCrew().setPiloting(skills[1], i);
                if (e.getCrew() instanceof LAMPilot) {
                    skills = c.getRandomSkillsGenerator().getRandomSkills(e, true);
                    ((LAMPilot) e.getCrew()).setGunneryAero(skills[0]);
                    ((LAMPilot) e.getCrew()).setPilotingAero(skills[1]);
                }
            }
            e.getCrew().sortRandomSkills();
        }
        sendUpdates(entities);
    }

    /** 
     * Sets random names for the given entities' pilots, as far as they can
     * be configured by the local player. 
     */
    void setRandomNames(Collection<Entity> entities) {
        if (!validateUpdate(entities)) {
            return;
        }
        for (Entity e: entities) {
            for (int i = 0; i < e.getCrew().getSlotCount(); i++) {
                Gender gender = RandomGenderGenerator.generate();
                e.getCrew().setGender(gender, i);
                e.getCrew().setName(RandomNameGenerator.getInstance().generate(gender, e.getOwner().getName()), i);
            }
        }
        sendUpdates(entities);
    }

    /** 
     * Sets random callsigns for the given entities' pilots, as far as they can
     * be configured by the local player. 
     */
    void setRandomCallsigns(Collection<Entity> entities) {
        if (!validateUpdate(entities)) {
            return;
        }
        for (Entity e: entities) {
            for (int i = 0; i < e.getCrew().getSlotCount(); i++) {
                e.getCrew().setNickname(RandomCallsignGenerator.getInstance().generate(), i);
            }
        }
        sendUpdates(entities);
    }
    
    /**
     * Toggles burst MG fire for the given entities to the state given as burstOn
     */
    void toggleBurstMg(Collection<Entity> entities, boolean burstOn) {
        if (!validateUpdate(entities)) {
            return;
        }
        Set<Entity> updateCandidates = new HashSet<>();
        for (Entity entity: entities) {
            for (Mounted m: entity.getWeaponList()) {
                if (((WeaponType) m.getType()).hasFlag(WeaponType.F_MG)) {
                    m.setRapidfire(burstOn);
                    updateCandidates.add(entity);
                }
            }
        }
        sendUpdates(updateCandidates);
    }
    
    /**
     * Toggles hot loading LRMs for the given entities to the state given as hotLoadOn
     */
    void toggleHotLoad(Collection<Entity> entities, boolean hotLoadOn) {
        if (!validateUpdate(entities)) {
            return;
        }
        Set<Entity> updateCandidates = new HashSet<>();
        for (Entity entity: entities) {
            for (Mounted m: entity.getAmmo()) { 
                // setHotLoad checks the Ammo to see if it can be hotloaded
                m.setHotLoad(hotLoadOn);
                // TODO: The following should ideally be part of setHotLoad in Mounted
                if (hotLoadOn) {
                    m.setMode("HotLoad");
                } else if (((EquipmentType)m.getType()).hasModeType("HotLoad")) {
                    m.setMode("");
                }
                updateCandidates.add(entity);
            };
        }
        sendUpdates(updateCandidates);
    }
    
    public void load(Collection<Entity> selEntities, String info) {
        StringTokenizer stLoad = new StringTokenizer(info, ":");
        int loaderId = Integer.parseInt(stLoad.nextToken());
        Entity loader = game.getEntity(loaderId);
        int bayNumber = Integer.parseInt(stLoad.nextToken());
        // Remove those entities from the candidates that are already carried by that loader
        Collection<Entity> entities = new HashSet<>(selEntities);
        entities.removeIf(e -> e.getTransportId() == loaderId);
        if (entities.isEmpty()) {
            return;
        }
        
        // If a unit of the selected units is currently loaded onto another, 2nd unit of the selected
        // units, do not continue. The player should unload units first. This would require
        // a server update offloading that second unit AND embarking it. Currently not possible
        // as a single server update and updates for one unit shouldn't be chained.
        Set<Entity> carriers = entities.stream()
                .filter(e -> e.getTransportId() != Entity.NONE)
                .map(e -> game.getEntity(e.getTransportId())).collect(toSet());
        if (!Collections.disjoint(entities, carriers)) {
            LobbyErrors.showNoDualLoad(frame);
            return;
        }

        boolean loadRear = false;
        if (stLoad.hasMoreTokens()) {
            loadRear = Boolean.parseBoolean(stLoad.nextToken());
        }

        StringBuilder errorMsg = new StringBuilder();
        if (!LobbyUtility.validateLobbyLoad(entities, loader, bayNumber, loadRear, errorMsg)) {
            JOptionPane.showMessageDialog(frame, errorMsg.toString(), 
                    Messages.getString("LoadingBay.error"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        for (Entity e : entities) {
            if (e.getTransportId() != Entity.NONE) {
                Entity formerLoader = game.getEntity(e.getTransportId());
                Set<Entity> updateCandidates = new HashSet<>();
                lobby.disembark(e, updateCandidates);
                if (!updateCandidates.isEmpty()) {
                    lobby.getLocalClient(formerLoader).sendUpdateEntity(formerLoader);
                }
            }
            lobby.loadOnto(e, loaderId, bayNumber);
        }
    }
    
    /** Asks for a new name for the provided forceId and applies it. */
    void renameForce(int forceId) {
        Forces forces = game.getForces();
        if (!forces.contains(forceId)) {
            return;
        }
        Force force = forces.getForce(forceId); 
        if (!isEditable(force)) {
            LobbyErrors.showCannotConfigEnemies(frame);
            return;
        }
        // Ask for a name
        String name = JOptionPane.showInputDialog(frame, "Choose a force designation");
        if ((name == null) || (name.trim().length() == 0)) {
            return;
        }
        forces.renameForce(name, forceId);
        List<Force> forceList = new ArrayList<Force>();
        forceList.add(force);
        correctSender(force).sendUpdateForce(forceList);
    }
    
    /**
     * Deletes the given forces and entities. Asks for confirmation if confirm is true. 
     */
    void delete(Collection<Force> foDelete, Collection<Entity> enDelete, boolean confirm) {
        Forces forces = game.getForces();
        // Remove redundant forces = subforces of other forces in the list 
        Set<Force> allSubForces = new HashSet<>();
        foDelete.stream().forEach(f -> allSubForces.addAll(forces.getFullSubForces(f)));
        foDelete.removeIf(f -> allSubForces.contains(f));
        Set<Force> finalFoDelete = new HashSet<>(foDelete);
        // Remove redundant entities = entities in the given forces
        Set<Entity> inForces = new HashSet<Entity>();
        foDelete.stream().map(f -> forces.getFullEntities(f)).forEach(inForces::addAll);
        enDelete.removeIf(e -> inForces.contains(e));
        Set<Entity> finalEnDelete = new HashSet<>(enDelete);
        
        if (!enDelete.isEmpty() && !validateUpdate(finalEnDelete)) {
            return;
        }
        if (!areForcesEditable(finalFoDelete)) {
            LobbyErrors.showCannotConfigEnemies(frame);
            return;
        }

        if (confirm) {
            int foCount = finalFoDelete.size();
            int enCount = finalEnDelete.size() + inForces.size();
            String question = "Really delete ";
            if (foCount > 0) {
                question += (foCount == 1 ? "one force" : foCount + " forces");
            }
            if (enCount > 0) {
                question += foCount > 0 ? " and " : "";
                question += (enCount == 1 ? "one unit" : enCount + " units");
            }
            question += "?";
            if (Response.NO == MMConfirmDialog.confirm(frame, "Delete Units...", question)) {
                return;
            }
        }
        
        // Send a command to remove the forceless entities
        Set<Client> senders = finalEnDelete.stream().map(this::correctSender).distinct().collect(toSet());
        for (Client sender: senders) {
            // Gather the entities for this sending client; 
            // Serialization doesn't like the toList() result, therefore the new ArrayList
            List<Integer> ids = new ArrayList<Integer>(finalEnDelete.stream()
                    .filter(e -> correctSender(e).equals(sender))
                    .map(Entity::getId)
                    .collect(toList()));
            sender.sendDeleteEntities(ids);
        }
        
        // Send a command to remove the forces (with entities)
        senders = finalFoDelete.stream().map(this::correctSender).distinct().collect(toSet());
        for (Client sender: senders) {
            List<Force> foList = new ArrayList<>(finalFoDelete.stream()
                    .filter(f -> correctSender(f).equals(sender))
                    .collect(toList()));
            sender.sendDeleteForces(foList);
        }
    }
    
    /**
     * Removes the given entities from their force(s), making them force-less.
     * Entities must have a single owner and be editable (local units or local bot's units)
     * (Having multiple owners makes sending updates correctly for one's own bots difficult) 
     */
    void removeFromForce(Collection<Entity> entities) {
        if (!validateUpdate(entities)) {
            return;
        }
        Set<Force> changedForces = game.getForces().removeEntityFromForces(entities);
        sendUpdates(entities, changedForces);
    }
    
    /**
     * Swaps pilots between the given entity 
     * and another entity of the given id
     */
    void swapPilots(Collection<Entity> entities, int targetId) {
        Entity target = game.getEntity(targetId);
        if (target == null) {
            return;
        }
        if (entities.size() != 1) {
            LobbyErrors.showSingleUnit(frame, "swap pilots");
            return;
        }
        Entity selected = CollectionUtil.randomElement(entities);
        if (!validateUpdate(Arrays.asList(target, selected))) {
            return;
        }
        Crew temp = target.getCrew();
        target.setCrew(selected.getCrew());
        selected.setCrew(temp);
        sendUpdates(Arrays.asList(target, selected));
    }
    
    /** 
     * Disconnects the passed entities from their C3 network, if any.
     * Due to the way C3 networks are represented in Entity, units
     * cannot disconnect from a C3 network with an id that is the
     * entity's own id. 
     */
    void c3DisconnectFromNetwork(Collection<Entity> entities) {
        if (!validateUpdate(entities)) {
            return;
        }
        Set<Entity> updateCandidates = performDisconnect(entities);
        sendUpdates(updateCandidates);
    }
    
    /** 
     * Performs a disconnect from C3 networks for the given entities without sending an update. 
     * Returns a set of all affected units. 
     */
    private HashSet<Entity> performDisconnect(Collection<Entity> entities) {
        HashSet<Entity> updateCandidates = new HashSet<>();
        for (Entity entity: entities) {
            if (entity.hasNhC3()) {
                entity.setC3NetIdSelf();
                updateCandidates.add(entity);
            } else if (entity.hasAnyC3System()) {
                entity.setC3Master(null, true);
                updateCandidates.add(entity);
            }
        }
        // Also disconnect all units connected *to* that entity
        for (Entity entity: game.getEntitiesVector()) {
            if (entities.contains(entity.getC3Master())) {
                entity.setC3Master(null, true);
                updateCandidates.add(entity);
            }
        }
        return updateCandidates;
    }
    
    /**  Sets the entities' C3M to act as a Company Master. */
    void c3SetCompanyMaster(Collection<Entity> entities) {
        if (!validateUpdate(entities)) {
            return;
        }
        if (!entities.stream().allMatch(e -> e.hasC3M())) {
            LobbyErrors.showOnlyC3M(frame);
            return;
        }
        entities.stream().forEach(e -> e.setC3Master(e.getId(), true));
        sendUpdates(entities);
    }
    
    /**  Sets the entities' C3M to act as a Lance Master (aka normal mode). */
    void c3SetLanceMaster(Collection<Entity> entities) {
        if (!validateUpdate(entities)) {
            return;
        }
        if (!entities.stream().allMatch(e -> e.hasC3M())) {
            LobbyErrors.showOnlyC3M(frame);
            return;
        }
        entities.stream().forEach(e -> e.setC3Master(-1, true));
        sendUpdates(entities);
    }
    
    /** 
     * Connects the passed entities to a nonhierarchic C3 (NC3, C3i or Nova CEWS)
     * identified by masterID.
     */
    void c3JoinNh(Collection<Entity> entities, int masterID, boolean disconnectFirst) {
        if (!validateUpdate(entities)) {
            return;
        }
        Entity master = game.getEntity(masterID);
        if (!master.hasNhC3() || !entities.stream().allMatch(e -> sameNhC3System(master, e))) {
            LobbyErrors.showSameC3(frame);
            return;
        }
        if (disconnectFirst) {
            performDisconnect(entities);
        }
        int freeNodes = master.calculateFreeC3Nodes();
        freeNodes += entities.contains(master) ? 1 : 0;
        if (entities.size() > freeNodes) {
            LobbyErrors.showExceedC3Capacity(frame);
            return;
        }
        entities.stream().forEach(e -> e.setC3NetId(master));
        sendUpdates(entities);
    }

    /** 
     * Connects the passed entities to a standard C3M
     * identified by masterID.
     */
    void c3Connect(Collection<Entity> entities, int masterID, boolean disconnectFirst) {
        Entity master = game.getEntity(masterID);
        // To make it possible to mark a C3S/C3S/C3S/C3M lance and connect it:
        entities.remove(master);
        if (!validateUpdate(entities)) {
            return;
        }
        boolean connectMS = master.isC3IndependentMaster()  && entities.stream().allMatch(e -> e.hasC3S());
        boolean connectMM = master.isC3CompanyCommander() && entities.stream().allMatch(e -> e.hasC3M());
        if (!connectMM && !connectMS) {
            LobbyErrors.showSameC3(frame);
            return;
        }
        Set<Entity> updateCandidates = new HashSet<>(entities);
        if (disconnectFirst) { // this is only true when a C3 lance is formed from SSSM
            updateCandidates.addAll(performDisconnect(entities));
            updateCandidates.addAll(performDisconnect(Arrays.asList(master)));
        }
        int newC3nodeCount = entities.stream().mapToInt(e -> game.getC3SubNetworkMembers(e).size()).sum();
        int masC3nodeCount = game.getC3NetworkMembers(master).size();
        if (newC3nodeCount + masC3nodeCount > Entity.MAX_C3_NODES || entities.size() > master.calculateFreeC3Nodes()) {
            LobbyErrors.showExceedC3Capacity(frame);
            return;
        }
        entities.stream().forEach(e -> e.setC3Master(master, true));
        sendUpdates(updateCandidates);
    }
    
    /** Change the given entities' controller to the player with ID newOwnerId. */
    void changeOwner(Collection<Entity> entities, Collection<Force> assignedForces, int newOwnerId) {
        if (entities.isEmpty()) {
            return;
        } else if (!assignedForces.isEmpty()) {
            LobbyErrors.showOnlyEntityOrForce(frame);
            return;
        }
        if (!validateUpdate(entities)) {
            return;
        }
        IPlayer newOwner = game.getPlayer(newOwnerId);
        if (newOwner == null) {
            MegaMek.getLogger().warning("Tried to change entity owner to a non-existent player!");
            return;
        }
        
        Set<Entity> updateCandidates = new HashSet<>();
        updateCandidates.addAll(entities);
        List<Entity> switchTeam = entities.stream().filter(e -> e.getOwner().isEnemyOf(newOwner)).collect(toList());        
        
        // For any units that are switching teams, offload units from them
        // and have them disembark if carried if the other unit doesn't also switch
        //TODO Don't unload units that transfer together. 
        for (Entity entity: switchTeam) {
            lobby.offloadFrom(entity, updateCandidates);
            lobby.disembark(entity, updateCandidates);
        }
        
        // Units that are switching teams cannot stay part of their current forces and C3 networks
        Set<Force> changedForces = game.getForces().removeEntityFromForces(switchTeam);
        updateCandidates.addAll(performDisconnect(switchTeam)); 

        // Assemble for sending into as few groups as possible
        // The standard sendUpdates cannot be used here because when changing owner
        // special care must be taken to send the update from the correct (former!) client.
        Set<Client> senders = new HashSet<>();
        senders.addAll(updateCandidates.stream().map(this::correctSender).distinct().collect(toList()));
        senders.addAll(changedForces.stream().map(this::correctSender).distinct().collect(toList()));
        
        for (Client sender: senders) {
            if (sender == null) {
                continue;
            }
            List<Entity> enList = updateCandidates.stream().filter(e -> correctSender(e).equals(sender)).collect(toList());
            List<Force> foList = changedForces.stream().filter(f -> correctSender(f).equals(sender)).collect(toList());
            
            // Entities must change owner this late so that the right sending client can still be found
            for (Entity entity: enList) {
                if (entities.contains(entity)) {
                    entity.setOwner(newOwner);
                }
            }
            if (foList.isEmpty()) {
                sender.sendUpdateEntity(enList);   
            } else {
                sender.sendUpdateForce(foList, enList);
            }
        }
    }
    
    /** Change the team of a controlled player (the local player or one of his bots). */
    void changeTeam(int team) {
        Client c = lobby.getSelectedClient();
        
        // If the team was not actually changed or the selected player 
        // is not editable (not the local player or local bot), do nothing
        if ((c == null) || (c.getLocalPlayer().getTeam() == team)) {
            return;
        }
        
        // Since different teams are always enemies, changing the team forces 
        // the units of this player to offload and disembark from 
        // all units of other players
        Set<Entity> updateCandidates = new HashSet<>();
        for (Entity entity: c.getGame().getPlayerEntities(c.getLocalPlayer(), false)) {
            lobby.offloadFromDifferentOwner(entity, updateCandidates);
            lobby.disembarkDifferentOwner(entity, updateCandidates);
        }
        
        // Units and forces cannot stay part of a former teammate's forces and vice versa
        Forces forces = game.getForces();
        int localId = c.getLocalPlayerNumber();
        Set<Entity> leaveForce = new HashSet<>();
        for (Entity entity: game.getEntitiesVector()) {
            if (!entity.partOfForce()) {
                continue;
            }
            Force force = forces.getForce(entity);
            // Must leave force if one of Entity or Force belongs to the changing player but the other doesn't
            if ((entity.getOwnerId() == localId) != (force.getOwnerId() == localId)) {
                leaveForce.add(entity);
            }
        }
        updateCandidates.addAll(leaveForce);
        Set<Force> changedForces = game.getForces().removeEntityFromForces(leaveForce);
        for (Force force: forces.getAllForces()) {
            if ((force.getParentId() != Force.NO_FORCE) && ((force.getOwnerId() == localId) != (forces.getForce(force.getParentId()).getOwnerId() == localId))) {
                changedForces.addAll(forces.promoteForce(force));
            }
        }
        
        // Units must leave their C3 networks
        updateCandidates.addAll(performDisconnect(game.getPlayerEntities(c.getLocalPlayer(), false)));

        sendUpdates(updateCandidates, changedForces);
        c.getLocalPlayer().setTeam(team);
        c.sendPlayerInfo();
    }
    
    /**
     * Add the entities to the force if admissible (the entities must all be editable
     * by the local player and be allied to the force's owner.
     */
    void addToForce(Collection<Entity> entities, int forceId) {
        Forces forces = game.getForces();
        if (!validateUpdate(entities) || !forces.contains(forceId)) {
            return;
        }
        Force force = forces.getForce(forceId);
        if (!admissibleToForce(entities, force)) {
            LobbyErrors.showForceOnlyTeam(frame);
            return;
        }
        Set<Entity> changedEntities = new HashSet<>();
        Set<Force> changedForces = new HashSet<>();
        for (Entity entity: entities) {
            List<Force> result = forces.addEntity(entity, forceId);
            if (!result.isEmpty()) {
                changedForces.addAll(result);
                changedEntities.add(entity);
            }
        }
        correctSender(force).sendUpdateForce(changedForces, changedEntities);
    }
    
    /** 
     * Changes the owner of the given forces to a different player without 
     * affecting force structure. 
     * When assigning the force only to an enemy, it would dislodge that force
     * from its parent and dislodge all units from it and leave it an empty
     * force for the enemy. That seems useless. Therefore this is restricted
     * to only assign to team members of the former owner. 
     */
    void assignForceOnly(Collection<Force> assignedForces, int newOwnerId) {
        IPlayer newOwner = game.getPlayer(newOwnerId);
        if (newOwner == null) {
            return;
        }
        if (!areForcesEditable(assignedForces)) {
            LobbyErrors.showCannotConfigEnemies(frame);
            return;
        }
        Forces forces = game.getForces();
        if (assignedForces.stream().anyMatch(f -> newOwner.isEnemyOf(forces.getOwner(f)))) {
            LobbyErrors.showOnlyTeammate(frame);
            return;
        }
        List<Force> changedForces = new ArrayList<>();
        for (Force force: assignedForces) {
            changedForces.addAll(forces.assignForceOnly(force, newOwner));
        }
        sendUpdates(new ArrayList<Entity>(), changedForces);
    }

    /** 
     * Changes the owner of the given forces to a different player together with
     * all subforces and units.
     */
    void assignForce(Collection<Force> assignedForces, int newOwnerId) {
        IPlayer newOwner = game.getPlayer(newOwnerId);
        if (newOwner == null) {
            return;
        }
        if (!areForcesEditable(assignedForces)) {
            LobbyErrors.showCannotConfigEnemies(frame);
            return;
        }
        // First, remove any redundant forces (subforces of others in the list)
        // The remaining forces will not overlap and can be treated separately
        Forces forces = game.getForces();
        Set<Force> allSubForces = new HashSet<>();
        assignedForces.stream().forEach(f -> allSubForces.addAll(forces.getFullSubForces(f)));
        List<Force> toBeAssigned = new ArrayList<>(assignedForces);
        toBeAssigned.removeIf(f -> allSubForces.contains(f));
        
        for (Force force: toBeAssigned) {
            changeOwner(force, newOwnerId);
        }
    }
    
    /** Change the owner of the given force and everything below it. */
    private void changeOwner(Force force, int newOwnerId) {
        Collection<Entity> entities = game.getForces().getFullEntities(force);
        if (!validateUpdate(entities)) {
            return;
        }
        IPlayer newOwner = game.getPlayer(newOwnerId);
        if (newOwner == null) {
            MegaMek.getLogger().warning("Tried to change entity owner to a non-existent player!");
            return;
        }
        // Get the right sender now before updating owners
        Client sender = correctSender(force);
        Set<Entity> updateCandidates = new HashSet<>();
        updateCandidates.addAll(entities);
        List<Entity> switchTeam = entities.stream().filter(e -> e.getOwner().isEnemyOf(newOwner)).collect(toList());        
        
        // For any units that are switching teams, offload units from them
        // and have them disembark if carried if the other unit doesn't also switch
        //TODO Don't unload units that transfer together. 
        for (Entity entity: switchTeam) {
            lobby.offloadFrom(entity, updateCandidates);
            lobby.disembark(entity, updateCandidates);
        }
        
        Set<Force> changedForces = game.getForces().assignFullForces(force, newOwner);

        // Entities must change owner this late so that the right sending client can still be found
        for (Entity entity: updateCandidates) {
            if (entities.contains(entity)) {
                entity.setOwner(newOwner);
            }
        }
        
        // Since all affected (sub)forces and entities must belong to one team, as they are
        // a single force structure, the owning client of the force can send the update.
        if (changedForces.isEmpty() && !updateCandidates.isEmpty()) {
            sender.sendUpdateEntity(updateCandidates);
        } else if (!changedForces.isEmpty()) {
            sender.sendUpdateForce(changedForces, updateCandidates);
        }
    }
    
    void unloadFromBay(Collection<Entity> entities, int bayId) {
        if (entities.size() != 1) {
            LobbyErrors.showSingleUnit(frame, "offload from bays");
            return;
        }
        Entity carrier = CollectionUtil.randomElement(entities);
        if (!validateUpdate(Arrays.asList(carrier))) {
            return;
        }
        Bay bay = carrier.getBayById(bayId);
        if (bay == null) {
            LobbyErrors.showNoSuchBay(frame);
        }
        Set<Entity> updateCandidates = new HashSet<>();
        for (Entity loadee : bay.getLoadedUnits()) {
            lobby.disembark(loadee, updateCandidates);
        }
        sendUpdates(updateCandidates);
    }
    
    /**
     * Creates a fighter squadron from the given list of entities.
     * Checks if all entities are fighters and if the number of entities
     * does not exceed squadron capacity. Asks for a squadron name.
     */
    void createSquadron(Collection<Entity> entities) {
        if (!validateUpdate(entities)) {
            return;
        }
        if (entities.stream().anyMatch(e -> !e.isFighter() || e instanceof FighterSquadron)) {
            LobbyErrors.showOnlyFighter(frame);
            return;
        }
        if (!areAllied(entities)) {
            LobbyErrors.showLoadOnlyAllied(frame);
            return;
        }
        boolean largeSquadrons = game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_ALLOW_LARGE_SQUADRONS);
        if ((!largeSquadrons && entities.size() > FighterSquadron.MAX_SIZE) 
                || entities.size() > FighterSquadron.ALTERNATE_MAX_SIZE) {
            LobbyErrors.showSquadronTooMany(frame);
        }
        
        // Ask for a squadron name
        String name = JOptionPane.showInputDialog(frame, "Choose a squadron designation");
        if ((name == null) || (name.trim().length() == 0)) {
            return;
        }
        
        // Now, actually create the squadron
        FighterSquadron fs = new FighterSquadron(name);
        fs.setOwner(createSquadronOwner(entities));
        List<Integer> fighterIds = new ArrayList<>(entities.stream().map(e -> e.getId()).collect(toList()));
        correctSender(fs).sendAddSquadron(fs, fighterIds);
    }
    
    /** 
     * Returns a likely owner client; if any of the fighter belongs to the local
     * player, returns the local player. If not, returns a local bot if any of the
     * fighters belongs to that; finally, returns the owner of a random one of the 
     * fighters.
     */
    private IPlayer createSquadronOwner(Collection<Entity> entities) {
        if (entities.stream().anyMatch(e -> e.getOwner().equals(localPlayer))) {
            return localPlayer;
        } else {
            for (Entry<String, Client> en: client.bots.entrySet()) {
                IPlayer bot = en.getValue().getLocalPlayer();
                if (entities.stream().anyMatch(e -> e.getOwner().equals(bot))) {
                    return en.getValue().getLocalPlayer();
                }
            }
        }
        return entities.stream().map(e -> e.getOwner()).findAny().get();
    }

    /** 
     * Performs standard checks for updates (units must be present, visible and editable)
     * and returns false if that's not the case. Also shows an error message dialog.
     */
    private boolean validateUpdate(Collection<Entity> entities) {
        if (entities.isEmpty()) {
            return false;
        }
        if (!isEditable(entities)) {
            LobbyErrors.showCannotConfigEnemies(frame);
            return false;
        }
        if (!canSeeAll(entities)) {
            LobbyErrors.showCannotViewHidden(frame);
            return false;
        }
        return true;
    }

    /** 
     * Sends the entities in the given Collection to the Server. 
     * Sends only those that can be edited, i.e. the player's own
     * or his bots' units. Will separate the units into update
     * packets for the local player and any local bots so that the 
     * server accepts all changes (as the server does not know of
     * local bots and rejects updates that are not for the sending client
     * or its teammates. 
     */
    void sendUpdates(Collection<Entity> entities) {
        // Gather the necessary sending clients; this list may contain null if some units 
        // cannot be affected at all, i.e. are enemies to localplayer and all his bots
        List<Client> senders = entities.stream().map(this::correctSender).distinct().collect(toList());
        for (Client sender: senders) {
            if (sender == null) {
                continue;
            }
            sender.sendUpdateEntity(new ArrayList<Entity>(
                    entities.stream().filter(e -> correctSender(e).equals(sender)).collect(toList())));
        }
    }
    
    /** 
     * Sends the entities and forces in the given Collections to the Server. 
     * Sends only those that can be edited, i.e. the player's own
     * or his bots' units. Will separate the units into update
     * packets for the local player and any local bots so that the 
     * server accepts all changes (as the server does not know of
     * local bots and rejects updates that are not for the sending client
     * or its teammates. 
     */
    void sendUpdates(Collection<Entity> changedEntities, Collection<Force> changedForces) {
        // Gather the necessary sending clients; this list may contain null if some units 
        // cannot be affected at all, i.e. are enemies to localplayer and all his bots
        Set<Client> senders = new HashSet<>();
        senders.addAll(changedEntities.stream().map(this::correctSender).distinct().collect(toList()));
        senders.addAll(changedForces.stream().map(this::correctSender).distinct().collect(toList()));
        
        for (Client sender: senders) {
            if (sender == null) {
                continue;
            }
            List<Entity> enList = changedEntities.stream().filter(e -> correctSender(e).equals(sender)).collect(toList());
            List<Force> foList = changedForces.stream().filter(f -> correctSender(f).equals(sender)).collect(toList());
            
            if (foList.isEmpty()) {
                sender.sendUpdateEntity(enList);   
            } else {
                sender.sendUpdateForce(foList, enList);
            }
        }
    }
    
    void sendSingleUpdate(Collection<Entity> changedEntities, Collection<Force> changedForces) {
        if (!areAllied(changedEntities, changedForces)) {
            MegaMek.getLogger().error("Cannot send force update unless all changed entities and forces are allied!");
            return;
        }
        
    }

    /** 
     * Returns the best sending client for an update of the given entity or
     * null if none can be found (entity is an enemy to the local player and all his bots)
     */
    private Client correctSender(Entity entity) {
        IPlayer owner = entity.getOwner();
        if (localPlayer.equals(owner)) {
            return client;
        } else if (client.bots.containsKey(owner.getName())) {
            return client.bots.get(owner.getName());
        } else if (!localPlayer.isEnemyOf(owner)) {
            return client;
        } else {
            for (Client bot: client.bots.values()) {
                if (!bot.getLocalPlayer().isEnemyOf(owner)) {
                    return bot;
                }
            }
        }
        
        return null;
    }
    
    /** 
     * Returns the best sending client for an update of the given force or
     * null if none can be found (force is an enemy to the local player and all his bots)
     */
    private Client correctSender(Force force) {
        IPlayer owner = game.getForces().getOwner(force);
        if (localPlayer.equals(owner) || isEditable(force)) {
            return client;
        } else if (client.bots.containsKey(owner.getName())) {
            return client.bots.get(owner.getName());
        } else if (!localPlayer.isEnemyOf(owner)) {
            return client;
        } else {
            for (Client bot: client.bots.values()) {
                if (!bot.getLocalPlayer().isEnemyOf(owner)) {
                    return bot;
                }
            }
        }
        return null;
    }

    /** 
     * Returns true when the given entity may be configured by the local player,
     * i.e. if it is his own unit or one of his bot's units.
     * <P>Note that this is more restrictive than the Server is. The Server
     * accepts entity changes also for teammates so that entity updates that 
     * signal transporting a teammate's unit don't get rejected. 
     * I think it's important to generally limit entity changes by other players
     * to avoid collisions of updates.
     * TODO: A possible enhancement might be a GM mode for MM, where only one 
     * player is allowed to change everything.
     */
    boolean isEditable(Entity entity) {
        return clientGui.getBots().containsKey(entity.getOwner().getName())
                || (entity.getOwnerId() == localPlayer.getId())
                || (entity.partOfForce() && isSelfOrLocalBot(game.getForces().getOwner(entity.getForceId())))
                || (entity.partOfForce() && isEditable(game.getForces().getForce(entity)));
    }

    /** 
     * Returns true when the given entity may NOT be configured by the local player,
     * i.e. if it is not own unit or one of his bot's units.
     * @see #isEditable(Entity)
     */
    boolean isNotEditable(Entity entity) {
        return !isEditable(entity);
    }

    /** 
     * Returns true when all given entities may be configured by the local player,
     * i.e. if they are his own units or one of his bot's units.
     * @see #isEditable(Entity)
     */
    boolean isEditable(Collection<Entity> entities) {
        return !entities.stream().anyMatch(this::isNotEditable);
    }

    /**
     * Returns true if the local player can see all of the given entities.
     * This is true except when a blind drop option is active and one or more
     * of the entities are not on his team.
     */
    boolean canSeeAll(Collection<Entity> entities) {
        if (!isBlindDrop(game) && !isRealBlindDrop(game)) {
            return true;
        }
        return !entities.stream().anyMatch(this::isLocalEnemy);
    }

    boolean entityInLocalTeam(Entity entity) {
        return !localPlayer.isEnemyOf(entity.getOwner());
    }
    
    boolean isSelfOrLocalBot(IPlayer player) {
        return client.bots.containsKey(player.getName()) || localPlayer.equals(player);
    }

    /** Returns true if the entity is an enemy of the local player. */
    boolean isLocalEnemy(Entity entity) {
        return localPlayer.isEnemyOf(entity.getOwner());
    }
    
    /**
     * A force is editable to the local player if any forces in its force chain
     * (this includes the force itself) is owned by the local player or one of the
     * local bots. This allows editing forces of other players if they are a subforce
     * of a local/bot force.  
     */
    boolean isEditable(Force force) {
        List<Force> chain = game.getForces().forceChain(force);
        return chain.stream().map(f -> game.getForces().getOwner(f)).anyMatch(this::isSelfOrLocalBot);
    }
    
    boolean isEditable(int forceId) {
        return game.getForces().contains(forceId) && isEditable(game.getForces().getForce(forceId)); 
    }
    
    boolean areForcesEditable(Collection<Force> forces) {
        return !forces.stream().anyMatch(f -> !isEditable(f));
    }
    
    /** 
     * Returns true when the entities may all be added to the force, i.e. when there are no
     * enemies to that force's owner among the entities. 
     */
    private boolean admissibleToForce(Collection<Entity> entities, Force force) {
        return areAllied(entities) 
                && !game.getForces().getOwner(force).isEnemyOf(CollectionUtil.randomElement(entities).getOwner());
    }
    
    /**
     * Returns true if no two of the given entities are enemies. This is
     * true when all entities belong to a single player. If they belong to 
     * different players, it is true when all belong to the same team and 
     * that team is one of Teams 1 through 5 (not "No Team").
     * <P>Returns true when entities is empty or has only one entity. The case of
     * entities being empty should be considered by the caller.  
     */
    private boolean areAllied(Collection<Entity> entities) {
        if (entities.isEmpty()) {
            MegaMek.getLogger().warning("Empty collection of entities received, cannot determine if no entities are all allied. Returning true.");
            return true;
        }
        Entity randomEntry = entities.stream().findAny().get();
        return !entities.stream().anyMatch(e -> e.isEnemyOf(randomEntry));
    }
    
    /**
     * Returns true if no two of the given entities and forces are enemies. Also checks 
     * between forces and entities.
     * @see #areAllied(Collection)
     * @see #areForcesAllied(Collection)
     */
    private boolean areAllied(Collection<Entity> entities, Collection<Force> forces) {
        if (entities.isEmpty() && forces.isEmpty()) {
            MegaMek.getLogger().warning("Empty collection of entities and forces received, cannot determine if these are allied. Returning true.");
            return true;
        }
        if (forces.isEmpty()) {
            return areAllied(entities);
        }
        if (entities.isEmpty()) {
            return areForcesAllied(forces);
        }
        Entity randomEntity = entities.stream().findAny().get();
        IPlayer entityOwner = randomEntity.getOwner();
        Force randomForce = forces.stream().findAny().get();
        IPlayer forceOwner = game.getForces().getOwner(randomForce);
        return areAllied(entities) && areForcesAllied(forces) && !entityOwner.isEnemyOf(forceOwner);
        
    }
    
    /**
     * Returns true if no two of the given forces are enemies. This is
     * true when all forces belong to a single player. If they belong to 
     * different players, it is true when all belong to the same team and 
     * that team is one of Teams 1 through 5 (not "No Team").
     * <P>Returns true when forces is empty or has only one force. The case of
     * forces being empty should be considered by the caller.  
     */
    private boolean areForcesAllied(Collection<Force> forces) {
        if (forces.isEmpty()) {
            MegaMek.getLogger().warning("Empty collection of forces received, cannot determine if these are allied. Returning true.");
            return true;
        }
        Force randomEntry = forces.stream().findAny().get();
        IPlayer owner = game.getForces().getOwner(randomEntry);
        return !forces.stream().anyMatch(f -> game.getForces().getOwner(f).isEnemyOf(owner));
    }
}

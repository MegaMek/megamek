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
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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

    /** Shows the dialog which allows adding pre-existing damage to units. */
    public void configureDamage(Collection<Entity> entities) {
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
        if (!validateUpdate(List.of(entity))) {
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
    
    /** 
     * If confirm is true, confirms that the player really wants to delete the units.
     * Offloads/disembarks units first.
     * If so or confirm is false, deletes them.
     */
    void deleteAction(Collection<Entity> entities, boolean confirm) {
        if (!validateUpdate(entities)) {
            return;
        }
        if (confirm) {
            int count = entities.size();
            String question = "Really delete " + ((count == 1) ? "one unit?" : count + " units?");
            if (Response.NO == MMConfirmDialog.confirm(frame, "Delete Units...", question)) {
                return;
            }
        }
        Set<Entity> updateCandidates = new HashSet<>();
        // Cycle the entities, disembark/offload all
        for (Entity entity: entities) {
            lobby.offloadFrom(entity, updateCandidates);
            lobby.disembark(entity, updateCandidates);
        }

        // Update the units, but not those that will be deleted anyway
        updateCandidates.removeAll(entities);
        sendUpdates(updateCandidates);
        // Finally, delete them
        // Gather the necessary sending clients
        List<Client> senders = entities.stream().map(this::correctSender).distinct().collect(toList());
        for (Client sender: senders) {
            // Gather the entities for this sending client; 
            // Serialization doesn't like the toList() result, therefore the new ArrayList
            List<Integer> ids = new ArrayList<Integer>(entities.stream()
                    .filter(e -> correctSender(e).equals(sender))
                    .map(e -> e.getId())
                    .collect(toList()));
            System.out.println("Sender " + sender.getLocalPlayer().getName() + ids);
            sender.sendDeleteEntities(new ArrayList<Integer>(ids));
        }
    }

    /**
     * Deletes the given forces if they are empty 
     */
    void deleteForces(Collection<Force> toDelete) {
//        if (toDelete.stream().anyMatch(f -> !f.isEmpty())) {
//            LobbyErrors.showOnlyEmptyForce(frame);
//            return;
//        }
        if (!areForcesEditable(toDelete)) {
            LobbyErrors.showCannotConfigEnemies(frame);
            return;
        }
        
        //TODO: Delete non-empty
        //TODO: Delete force tree without entities
        
        Set<Client> senders = new HashSet<>();
        senders.addAll(toDelete.stream().map(this::correctSender).distinct().collect(toList()));
        
        for (Client sender: senders) {
            List<Force> foList = toDelete.stream().filter(f -> correctSender(f).equals(sender)).collect(toList());
            
            if (!foList.isEmpty()) {
                sender.sendDeleteForces(foList);
            }
        }
    }
    
    /**
     * Deletes the given forces if they are empty 
     */
    void delete(Collection<Force> foDelete, Collection<Entity> enDelete) {
        // Remove redundant forces = subforces of other forces in the list 
        Forces forces = game.getForces();
        Set<Force> allSubForces = new HashSet<>();
        foDelete.stream().forEach(f -> allSubForces.addAll(forces.getFullSubForces(f)));
        Set<Force> RedFoDelete = new HashSet<>(foDelete);
        RedFoDelete.removeIf(f -> allSubForces.contains(f));
        // Add all force's entities to the entity list
        foDelete.stream().map(f -> game.getForces().getFullEntities(f)).forEach(enDelete::addAll);
        if (!validateUpdate(enDelete)) {
            if (!enDelete.isEmpty()) {
                return;
            }
        }
        if (!areForcesEditable(foDelete)) {
            LobbyErrors.showCannotConfigEnemies(frame);
            return;
        }
        
        removeFromForce(enDelete);
        deleteAction(enDelete, false);
        // Delete also all subforces
        Set<Force> FullFoDelete = new HashSet<>(RedFoDelete);
        RedFoDelete.stream().map(f -> forces.getFullSubForces(f)).forEach(FullFoDelete::addAll);
        deleteForces(FullFoDelete);
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
        if (!validateUpdate(List.of(target, selected))) {
            return;
        }
        Crew temp = target.getCrew();
        target.setCrew(selected.getCrew());
        selected.setCrew(temp);
        sendUpdates(List.of(target, selected));
    }
    
    /** Change the given entities' controller to the player with ID newOwnerId. */
    void changeOwner(Collection<Entity> entities, Collection<Force> assignedForces, int newOwnerId) {
        if (entities.isEmpty()) {
            return;
        } else if (!assignedForces.isEmpty()) {
            LobbyErrors.showOnlyEntityOrForce(frame);
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
        
        // Units that are switching teams cannot stay part of their current forces
        Set<Force> changedForces = game.getForces().removeEntityFromForces(switchTeam);

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
        Set<Force> changedForces = game.getForces().removeEntityFromForces(leaveForce);
        for (Force force: forces.getAllForces()) {
            if ((force.getParentId() != Force.NO_FORCE) && ((force.getOwnerId() == localId) != (forces.getForce(force.getParentId()).getOwnerId() == localId))) {
                changedForces.addAll(forces.promoteForce(force));
            }
        }
        
        sendUpdates(updateCandidates, changedForces);
        c.getLocalPlayer().setTeam(team);
        c.sendPlayerInfo();
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

    /** 
     * Returns the best sending client for an update of the given entity or
     * null if none can be found (entity is an enemy to the local player and all his bots)
     */
    private Client correctSender(Entity entity) {
        IPlayer owner = entity.getOwner();
        if (!localPlayer.isEnemyOf(owner)) {
            return client;
        } else if (client.bots.containsKey(owner.getName())) {
            return client.bots.get(owner.getName());
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
        if (!localPlayer.isEnemyOf(owner)) {
            return client;
        } else if (client.bots.containsKey(owner.getName())) {
            return client.bots.get(owner.getName());
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
}

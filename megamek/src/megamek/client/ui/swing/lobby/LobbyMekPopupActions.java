/*  
* MegaMek - Copyright (C) 2021 - The MegaMek Team  
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
package megamek.client.ui.swing.lobby;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import megamek.common.Bay;
import megamek.common.Entity;
import megamek.common.QuirksHandler;

/** The ActionListener for the lobby popup menu for both the MekTable and MekTrees. */
public class LobbyMekPopupActions implements ActionListener {

    private ChatLounge lobby;
    
    /** The ActionListener for the lobby popup menu for both the MekTable and MekTrees. */
    LobbyMekPopupActions(ChatLounge cl) {
        lobby = cl;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println(e.getActionCommand());
        StringTokenizer st = new StringTokenizer(e.getActionCommand(), "|");
        String command = st.nextToken();
        // info need not be an int; loading uses the format "##:##"
        String info = st.nextToken();
        // The entities list may be empty
        Set<Entity> entities = LobbyUtility.getEntities(lobby.game(), st.nextToken());
        
        switch (command) {
            
            // Single entity commands
        case "CONFIGURE":
        case "UNLOADALLFROMBAY":
        case "C3DISCONNECT":
        case "C3CM":
        case "C3LM":
        case "C3JOIN":
        case "C3CONNECT":
            if (!entities.isEmpty()) {
                Entity randomSelected = entities.stream().findAny().get();
                singleEntityAction(command, randomSelected, info);
            }
            break;
            
            // Multi entity commands
        case "SWAP":
        case "DAMAGE":
        case "BV":
        case "VIEW":
        case "INDI_CAMO":
        case "CONFIGURE_ALL":
        case "DELETE":
        case "SKILLS":
        case "NAME":
        case "CALLSIGN":
        case "RAPIDFIREMG_ON":
        case "RAPIDFIREMG_OFF":
        case "HOTLOAD_ON":
        case "HOTLOAD_OFF":
        case "SQUADRON":
        case "SAVE_QUIRKS_ALL":
        case "SAVE_QUIRKS_MODEL":
        case "LOAD": 
        case "UNLOAD":
        case "UNLOADALL":
        case "DEPLOY":
        case "ASSIGN":
        case "HEAT":
        case "HIDDEN":
        case "STAND":
            if (!entities.isEmpty()) {
                multiEntityAction(command, entities, info);
            }
            break;
            
            // Force commands
        case "FCREATESUB":
        case "FADDTO":
        case "FRENAME":
        case "FCREATETOP":
        case "FREMOVE":
        case "FDELETE":
        case "FATTACH":
        case "FPROMOTE":
        case "FASSIGN":
        case "FASSIGNONLY":
            forceAction(command, entities, info);
            break;
        }
    }
    
    /** Calls lobby actions for forces. */
    private void forceAction(String command, Set<Entity> entities, String info) {
        switch (command) {
        case "FCREATESUB":
            int parentId = Integer.parseInt(info);
            lobby.createSubForce(parentId);  
            break;
            
        case "FADDTO":
            int forceId = Integer.parseInt(info);
            lobby.addToForce(entities, forceId);
            break;
            
        case "FRENAME":
            forceId = Integer.parseInt(info);
            lobby.renameForce(forceId);  
            break;
            
        case "FCREATETOP":
            lobby.createTopForce();
            break;
            
        case "FREMOVE":
            lobby.lobbyActions.removeFromForce(entities);
            break;
            
        case "FDELETE":
//            lobby.lobbyActions.deleteAction(entities, true);
//            lobby.lobbyActions.deleteForces(LobbyUtility.getForces(lobby.game(), info));
            lobby.lobbyActions.delete(LobbyUtility.getForces(lobby.game(), info), entities);
            break;
            
        case "FATTACH":
            StringTokenizer st = new StringTokenizer(info, ":");
            parentId = Integer.parseInt(st.nextToken());
            forceId = Integer.parseInt(st.nextToken());
            lobby.attachForce(forceId, parentId);
            break;
            
        case "FPROMOTE":
            forceId = Integer.parseInt(info);
            lobby.promoteForce(forceId);
            break;
            
        case "FASSIGN":
            st = new StringTokenizer(info, ":");
            int newOwnerId = Integer.parseInt(st.nextToken());
            lobby.lobbyActions.assignForce(LobbyUtility.getForces(lobby.game(), st.nextToken()), newOwnerId);
            break;
            
        case "FASSIGNONLY":
            st = new StringTokenizer(info, ":");
            newOwnerId = Integer.parseInt(st.nextToken());
            lobby.lobbyActions.assignForceOnly(LobbyUtility.getForces(lobby.game(), st.nextToken()), newOwnerId);
            break;
        }
    }
    
    /** Calls lobby actions for multiple entities. */
    private void multiEntityAction(String command, Set<Entity> entities, String info) {
        switch (command) {
        case "INDI_CAMO":
            lobby.lobbyActions.individualCamo(entities);
            break;
            
        case "CONFIGURE_ALL":
            lobby.lobbyActions.customizeMechs(entities);
            break;
            
        case "DELETE":
            lobby.lobbyActions.deleteAction(entities, true);
            break;
            
        case "SKILLS":
            lobby.lobbyActions.setRandomSkills(entities);
            break;
            
        case "NAME":
            lobby.lobbyActions.setRandomNames(entities);
            break;
            
        case "CALLSIGN":
            lobby.lobbyActions.setRandomCallsigns(entities);
            break;
            
        case "RAPIDFIREMG_ON":
        case "RAPIDFIREMG_OFF":
            lobby.lobbyActions.toggleBurstMg(entities, command.equals("RAPIDFIREMG_ON"));
            break;

        case "HOTLOAD_ON":
        case "HOTLOAD_OFF":
            lobby.lobbyActions.toggleHotLoad(entities, command.equals("HOTLOAD_ON"));
            break;
            
        case "SQUADRON":
            lobby.createSquadron(entities);
            break;
            
        case "SAVE_QUIRKS_ALL":
            for (Entity e : entities) {
                QuirksHandler.addCustomQuirk(e, false);
            }
            break;
            
        case "SAVE_QUIRKS_MODEL":
            for (Entity e : entities) {
                QuirksHandler.addCustomQuirk(e, true);
            }
            break;
            
        case "LOAD":
            lobby.load(entities, info);
            break;

        case "UNLOAD":
            Set<Entity> updateCandidates = new HashSet<>();
            lobby.disembarkAll(entities); //TODO ?????? leftover call
            for (Entity e: entities) {
                lobby.disembark(e, updateCandidates);
            }
            lobby.sendUpdate(updateCandidates);
            break;
            
        case "UNLOADALL":
            updateCandidates = new HashSet<>();
            lobby.offloadAll(entities, updateCandidates);
            lobby.sendUpdate(updateCandidates);
            break;
            
        case "ASSIGN":
            StringTokenizer st = new StringTokenizer(info, ":");
            int newOwnerId = Integer.parseInt(st.nextToken());
            lobby.lobbyActions.changeOwner(entities, LobbyUtility.getForces(lobby.game(), st.nextToken()), newOwnerId);
            break;
            
        case "DEPLOY":
            int round = Integer.parseInt(info);
            lobby.lobbyActions.applyDeployment(entities, round);
            break;
            
        case "HEAT":
            int heat = Integer.parseInt(info);
            lobby.lobbyActions.applyHeat(entities, heat);
            break;
            
        case "HIDDEN":
            lobby.lobbyActions.applyHidden(entities, info.equals("HIDE"));
            break;
            
        case "STAND":
            lobby.lobbyActions.applyProne(entities, info);
            break;
            
        case "VIEW":
            lobby.mechReadoutAction(entities);
            break;
            
        case "BV":
            lobby.mechBVAction(entities);
            break;
            
        case "DAMAGE":
            lobby.lobbyActions.configureDamage(entities);
            break;
            
        case "SWAP":
            int id = Integer.parseInt(info);
            lobby.lobbyActions.swapPilots(entities, id);
            break;
        }
        
    }
    
    /** Calls lobby actions for a single entity. */
    private void singleEntityAction(String command, Entity entity, String info) {
        switch (command) {
        case "CONFIGURE":
            lobby.lobbyActions.customizeMech(entity);
            break;
            
        case "C3DISCONNECT":
            lobby.disconnectC3FromNetwork(entity);
            break;
            
        case "C3CM":
            lobby.setC3CompanyMaster(entity);
            break;
            
        case "C3LM":
            lobby.setC3LanceMaster(entity);
            break;
            
        case "C3JOIN":
            int master = Integer.parseInt(info);
            lobby.joinNhC3(entity, master);
            break;
            
        case "C3CONNECT":
            master = Integer.parseInt(info);
            lobby.connectToC3(entity, master);
            break;
            
        case "UNLOADALLFROMBAY":
            Set<Entity> updateCandidates = new HashSet<>();
            int id = Integer.parseInt(info);
            Bay bay = entity.getBayById(id);
            for (Entity loadee : bay.getLoadedUnits()) {
                lobby.disembark(loadee, updateCandidates);
            }
            lobby.sendUpdate(updateCandidates);
            break;
        }
    }
    
}

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

import megamek.common.Entity;
import megamek.common.QuirksHandler;
import static megamek.client.ui.swing.lobby.LobbyMekPopup.*;

/** The ActionListener for the lobby popup menu for both the MekTable and MekTrees. */
public class LobbyMekPopupActions implements ActionListener {

    private ChatLounge lobby;

    /** The ActionListener for the lobby popup menu for both the MekTable and MekTrees. */
    LobbyMekPopupActions(ChatLounge cl) {
        lobby = cl;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        StringTokenizer st = new StringTokenizer(e.getActionCommand(), "|");
        String command = st.nextToken();
        // info need not be an int; loading uses the format "##:##"
        String info = st.nextToken();
        // The entities list may be empty
        Set<Entity> entities = LobbyUtility.getEntities(lobby.game(), st.nextToken());

        switch (command) {
            // Single entity commands
            case LMP_CONFIGURE:
                if (!entities.isEmpty()) {
                    Entity randomSelected = entities.stream().findAny().get();
                    singleEntityAction(command, randomSelected, info);
                }
                break;

                // Multi entity commands
            case LMP_ALPHASTRIKE:
            case LMP_UNLOADALLFROMBAY:
            case LMP_C3CM:
            case LMP_C3LM:
            case LMP_C3JOIN:
            case LMP_C3CONNECT:
            case LMP_C3DISCONNECT:
            case LMP_C3FORMC3:
            case LMP_C3FORMNHC3:
            case LMP_SWAP:
            case LMP_DAMAGE:
            case LMP_BV:
            case LMP_COST:
            case LMP_VIEW:
            case LMP_INDI_CAMO:
            case LMP_CONFIGURE_ALL:
            case LMP_DELETE:
            case LMP_SKILLS:
            case LMP_NAME:
            case LMP_CALLSIGN:
            case LMP_RAPIDFIREMG_ON:
            case LMP_RAPIDFIREMG_OFF:
            case LMP_HOTLOAD_ON:
            case LMP_HOTLOAD_OFF:
            case LMP_SQUADRON:
            case LMP_SAVE_QUIRKS_ALL:
            case LMP_SAVE_QUIRKS_MODEL:
            case LMP_LOAD:
            case LMP_UNLOAD:
            case LMP_UNLOADALL:
            case LMP_DEPLOY:
            case LMP_ASSIGN:
            case LMP_HEAT:
            case LMP_HIDDEN:
            case LMP_STAND:
            case LMP_PRIO_TARGET:
                if (!entities.isEmpty()) {
                    multiEntityAction(command, entities, info);
                }
                break;

            case LMP_MOVE_UP:
            case LMP_MOVE_DOWN:
                multiEntityAction(command, entities, info);
                break;

                // Force commands
            case LMP_FCREATESUB:
            case LMP_FADDTO:
            case LMP_FRENAME:
            case LMP_FCREATETOP:
            case LMP_FREMOVE:
            case LMP_FPROMOTE:
            case LMP_FASSIGN:
            case LMP_FASSIGNONLY:
            case LMP_FCREATEFROM:
            case LMP_SBFFORMATION:
                forceAction(command, entities, info);
                break;
        }
    }

    /** Calls lobby actions for forces. */
    private void forceAction(String command, Set<Entity> entities, String info) {
        switch (command) {
            case LMP_FCREATESUB:
                int parentId = Integer.parseInt(info);
                lobby.lobbyActions.forceCreateSub(parentId);
                break;

            case LMP_FADDTO:
                int forceId = Integer.parseInt(info);
                lobby.lobbyActions.forceAddEntity(entities, forceId);
                break;

            case LMP_FRENAME:
                forceId = Integer.parseInt(info);
                lobby.lobbyActions.forceRename(forceId);
                break;

            case LMP_FCREATETOP:
                lobby.lobbyActions.forceCreateEmpty();
                break;

            case LMP_FCREATEFROM:
                lobby.lobbyActions.forceCreateFrom(entities);
                break;

            case LMP_FREMOVE:
                lobby.lobbyActions.forceRemoveEntity(entities);
                break;

            case LMP_FPROMOTE:
                StringTokenizer fst = new StringTokenizer(info, ",");
                Set<Integer> forceIds = new HashSet<>();
                while (fst.hasMoreTokens()) {
                    forceIds.add(Integer.parseInt(fst.nextToken()));
                }
                lobby.lobbyActions.forcePromote(forceIds);
                break;

            case LMP_FASSIGN:
                StringTokenizer st = new StringTokenizer(info, ":");
                int newOwnerId = Integer.parseInt(st.nextToken());
                lobby.lobbyActions.forceAssignFull(LobbyUtility.getForces(lobby.game(), st.nextToken()), newOwnerId);
                break;

            case LMP_FASSIGNONLY:
                st = new StringTokenizer(info, ":");
                newOwnerId = Integer.parseInt(st.nextToken());
                lobby.lobbyActions.forceAssignOnly(LobbyUtility.getForces(lobby.game(), st.nextToken()), newOwnerId);
                break;

            case LMP_SBFFORMATION:
                lobby.lobbyActions.showSbfView(LobbyUtility.getForces(lobby.game(), info));
                break;
        }
    }

    /** Calls lobby actions for multiple entities. */
    private void multiEntityAction(String command, Set<Entity> entities, String info) {
        switch (command) {
            case LMP_INDI_CAMO:
                lobby.lobbyActions.individualCamo(entities);
                break;

            case LMP_CONFIGURE_ALL:
                lobby.lobbyActions.customizeMechs(entities);
                break;

            case LMP_DELETE:
                lobby.lobbyActions.delete(LobbyUtility.getForces(lobby.game(), info), entities, true);
                break;

            case LMP_SKILLS:
                lobby.lobbyActions.setRandomSkills(entities);
                break;

            case LMP_NAME:
                lobby.lobbyActions.setRandomNames(entities);
                break;

            case LMP_CALLSIGN:
                lobby.lobbyActions.setRandomCallsigns(entities);
                break;

            case LMP_RAPIDFIREMG_ON:
            case LMP_RAPIDFIREMG_OFF:
                lobby.lobbyActions.toggleBurstMg(entities, command.equals(LMP_RAPIDFIREMG_ON));
                break;

            case LMP_HOTLOAD_ON:
            case LMP_HOTLOAD_OFF:
                lobby.lobbyActions.toggleHotLoad(entities, command.equals(LMP_HOTLOAD_ON));
                break;

            case LMP_SQUADRON:
                lobby.lobbyActions.createSquadron(entities);
                break;

            case LMP_SAVE_QUIRKS_ALL:
                for (Entity e : entities) {
                    QuirksHandler.addCustomQuirk(e, false);
                }
                break;

            case LMP_SAVE_QUIRKS_MODEL:
                for (Entity e : entities) {
                    QuirksHandler.addCustomQuirk(e, true);
                }
                break;

            case LMP_LOAD:
                lobby.lobbyActions.load(entities, info);
                break;

            case LMP_UNLOAD:
                Set<Entity> updateCandidates = new HashSet<>();
                lobby.disembarkAll(entities);
                break;

            case LMP_UNLOADALL:
                updateCandidates = new HashSet<>();
                lobby.offloadAll(entities, updateCandidates);
                lobby.sendUpdate(updateCandidates);
                break;

            case LMP_ASSIGN:
                StringTokenizer st = new StringTokenizer(info, ":");
                int newOwnerId = Integer.parseInt(st.nextToken());
                lobby.lobbyActions.changeOwner(entities, LobbyUtility.getForces(lobby.game(), st.nextToken()), newOwnerId);
                break;

            case LMP_DEPLOY:
                int round = Integer.parseInt(info);
                lobby.lobbyActions.applyDeployment(entities, round);
                break;

            case LMP_HEAT:
                int heat = Integer.parseInt(info);
                lobby.lobbyActions.applyHeat(entities, heat);
                break;

            case LMP_HIDDEN:
                lobby.lobbyActions.applyHidden(entities, info.equals(LMP_HIDE));
                break;

            case LMP_STAND:
                lobby.lobbyActions.applyProne(entities, info);
                break;

            case LMP_VIEW:
                lobby.mechReadoutAction(entities);
                break;

            case LMP_BV:
                lobby.mechBVAction(entities);
                break;

            case LMP_COST:
                lobby.mechCostAction(entities);
                break;

            case LMP_DAMAGE:
                lobby.lobbyActions.configureDamage(entities);
                break;

            case LMP_SWAP:
                int id = Integer.parseInt(info);
                lobby.lobbyActions.swapPilots(entities, id);
                break;

            case LMP_C3DISCONNECT:
                lobby.lobbyActions.c3DisconnectFromNetwork(entities);
                break;

            case LMP_C3CM:
                lobby.lobbyActions.c3SetCompanyMaster(entities);
                break;

            case LMP_C3LM:
                lobby.lobbyActions.c3SetLanceMaster(entities);
                break;

            case LMP_C3JOIN:
                int master = Integer.parseInt(info);
                lobby.lobbyActions.c3JoinNh(entities, master, false);
                break;

            case LMP_C3CONNECT:
                master = Integer.parseInt(info);
                lobby.lobbyActions.c3Connect(entities, master, false);
                break;

            case LMP_C3FORMC3:
                master = Integer.parseInt(info);
                lobby.lobbyActions.c3Connect(entities, master, true);
                break;

            case LMP_C3FORMNHC3:
                master = Integer.parseInt(info);
                lobby.lobbyActions.c3JoinNh(entities, master, true);
                break;

            case LMP_UNLOADALLFROMBAY:
                int bay = Integer.parseInt(info);
                lobby.lobbyActions.unloadFromBay(entities, bay);
                break;

            case LMP_MOVE_DOWN:
                lobby.lobbyActions.forceMove(LobbyUtility.getForces(lobby.game(), info), entities, false);
                break;

            case LMP_MOVE_UP:
                lobby.lobbyActions.forceMove(LobbyUtility.getForces(lobby.game(), info), entities, true);
                break;

            case LMP_PRIO_TARGET:
                lobby.lobbyActions.setPrioTarget(info, entities);
                break;

            case LMP_ALPHASTRIKE:
                lobby.lobbyActions.showAlphaStrikeView(entities);
                break;
        }
    }

    /** Calls lobby actions for a single entity. */
    private void singleEntityAction(String command, Entity entity, String info) {
        switch (command) {
            case LMP_CONFIGURE:
                lobby.lobbyActions.customizeMech(entity);
                break;


        }
    }
}

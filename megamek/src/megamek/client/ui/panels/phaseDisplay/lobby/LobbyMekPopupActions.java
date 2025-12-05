/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.panels.phaseDisplay.lobby;

import static megamek.client.ui.panels.phaseDisplay.lobby.LobbyMekPopup.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import megamek.MMConstants;
import megamek.client.generator.ReconfigurationParameters;
import megamek.client.generator.TeamLoadOutGenerator;
import megamek.client.ratgenerator.FactionRecord;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.common.Player;
import megamek.common.Team;
import megamek.common.containers.MunitionTree;
import megamek.common.equipment.BombLoadout;
import megamek.common.force.Force;
import megamek.common.game.Game;
import megamek.common.net.packets.InvalidPacketDataException;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.units.IBomber;
import megamek.common.util.StringUtil;
import megamek.logging.MMLogger;

/** The ActionListener for the lobby popup menu for both the MekTable and MekTrees. */
public record LobbyMekPopupActions(ChatLounge lobby) implements ActionListener {
    private static final MMLogger LOGGER = MMLogger.create(LobbyMekPopupActions.class);

    /** The ActionListener for the lobby popup menu for both the MekTable and MekTrees. */
    public LobbyMekPopupActions {
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
            case LMP_TOW:
                if (!entities.isEmpty()) {
                    Entity randomSelected = entities.stream().findAny().get();
                    singleEntityAction(command, randomSelected, info);
                }
                break;

            // Multi entity commands
            case LMP_ALPHA_STRIKE:
            case LMP_AUTOCONFIG:
            case LMP_SAVE_CONFIG:
            case LMP_APPLY_CONFIG:
            case LMP_RANDOM_CONFIG:
            case LMP_UNLOAD_ALL_FROM_BAY:
            case LMP_C3CM:
            case LMP_C3LM:
            case LMP_C3JOIN:
            case LMP_C3CONNECT:
            case LMP_C3DISCONNECT:
            case LMP_C3_FORM_C3:
            case LMP_C3_FORM_NHC3:
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
            case LMP_RAPID_FIRE_MG_ON:
            case LMP_RAPID_FIRE_MG_OFF:
            case LMP_HOT_LOAD_ON:
            case LMP_HOT_LOAD_OFF:
            case LMP_VRT_LONG:
            case LMP_VRT_SHORT:
            case LMP_SQUADRON:
            case LMP_LOAD:
            case LMP_UNLOAD:
            case LMP_UNLOAD_ALL:
            case LMP_DETACH_TRAILER:
            case LMP_DETACH_FROM_TRACTOR:
            case LMP_DEPLOY:
            case LMP_ASSIGN:
            case LMP_HEAT:
            case LMP_HIDDEN:
            case LMP_STAND:
            case LMP_PRIORITY_TARGET:
                if (!entities.isEmpty()) {
                    multiEntityAction(command, entities, info);
                }
                break;

            case LMP_MOVE_UP:
            case LMP_MOVE_DOWN:
                multiEntityAction(command, entities, info);
                break;

            // Force commands
            case LMP_F_CREATE_SUB:
            case LMP_F_ADD_TO:
            case LMP_F_RENAME:
            case LMP_F_CREATE_TOP:
            case LMP_F_REMOVE:
            case LMP_F_PROMOTE:
            case LMP_F_ASSIGN:
            case LMP_F_ASSIGN_ONLY:
            case LMP_F_CREATE_FROM:
            case LMP_SBF_FORMATION:
            case LMP_FC_DELETE_EMPTY:
                forceAction(command, entities, info);
                break;
        }
    }

    /** Calls lobby actions for forces. */
    private void forceAction(String command, Set<Entity> entities, String info) {
        int forceId;
        int newOwnerId;
        StringTokenizer stringTokenizer;

        switch (command) {
            case LMP_F_CREATE_SUB:
                int parentId = StringUtil.toInt(info, Force.NO_FORCE);
                lobby.lobbyActions.forceCreateSub(parentId);
                break;

            case LMP_F_ADD_TO:
                forceId = StringUtil.toInt(info, Force.NO_FORCE);
                lobby.lobbyActions.forceAddEntity(entities, forceId);
                break;

            case LMP_F_RENAME:
                forceId = StringUtil.toInt(info, Force.NO_FORCE);
                lobby.lobbyActions.forceRename(forceId);
                break;

            case LMP_F_CREATE_TOP:
                lobby.lobbyActions.forceCreateEmpty();
                break;

            case LMP_FC_DELETE_EMPTY:
                forceId = StringUtil.toInt(info, Force.NO_FORCE);
                lobby.lobbyActions.forceDeleteEmpty(forceId);
                break;

            case LMP_F_CREATE_FROM:
                lobby.lobbyActions.forceCreateFrom(entities);
                break;

            case LMP_F_REMOVE:
                lobby.lobbyActions.forceRemoveEntity(entities);
                break;

            case LMP_F_PROMOTE:
                StringTokenizer fst = new StringTokenizer(info, ",");
                Set<Integer> forceIds = new HashSet<>();
                while (fst.hasMoreTokens()) {
                    forceIds.add(StringUtil.toInt(fst.nextToken(), Force.NO_FORCE));
                }
                lobby.lobbyActions.forcePromote(forceIds);
                break;

            case LMP_F_ASSIGN:
                stringTokenizer = new StringTokenizer(info, ":");
                newOwnerId = StringUtil.toInt(stringTokenizer.nextToken(), Player.PLAYER_NONE);
                lobby.lobbyActions.forceAssignFull(LobbyUtility.getForces(lobby.game(), stringTokenizer.nextToken()),
                      newOwnerId);
                break;

            case LMP_F_ASSIGN_ONLY:
                stringTokenizer = new StringTokenizer(info, ":");
                newOwnerId = StringUtil.toInt(stringTokenizer.nextToken(), Player.PLAYER_NONE);
                lobby.lobbyActions.forceAssignOnly(LobbyUtility.getForces(lobby.game(), stringTokenizer.nextToken()),
                      newOwnerId);
                break;

            case LMP_SBF_FORMATION:
                lobby.lobbyActions.showSbfView(LobbyUtility.getForces(lobby.game(), info));
                break;
        }
    }

    /** Calls lobby actions for multiple entities. */
    private void multiEntityAction(String command, Set<Entity> entities, String info) {
        Set<Entity> updateCandidates;
        int master;

        try {
            switch (command) {
                case LMP_INDI_CAMO:
                    lobby.lobbyActions.individualCamo(entities);
                    break;

                case LMP_CONFIGURE_ALL:
                    lobby.lobbyActions.customizeMeks(entities);
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
                    lobby.lobbyActions.setRandomCallSigns(entities);
                    break;

                case LMP_RAPID_FIRE_MG_ON:
                case LMP_RAPID_FIRE_MG_OFF:
                    lobby.lobbyActions.toggleBurstMg(entities, command.equals(LMP_RAPID_FIRE_MG_ON));
                    break;

                case LMP_HOT_LOAD_ON:
                case LMP_HOT_LOAD_OFF:
                    lobby.lobbyActions.toggleHotLoad(entities, command.equals(LMP_HOT_LOAD_ON));
                    break;

                case LMP_VRT_LONG:
                case LMP_VRT_SHORT:
                    lobby.lobbyActions.setVRTMode(entities, command.equals(LMP_VRT_LONG));
                    break;

                case LMP_SQUADRON:
                    lobby.lobbyActions.createSquadron(entities);
                    break;

                case LMP_LOAD:
                    lobby.lobbyActions.load(entities, info);
                    break;

                case LMP_UNLOAD:
                    updateCandidates = new HashSet<>();
                    lobby.disembarkAll(entities);
                    break;

                case LMP_UNLOAD_ALL:
                    updateCandidates = new HashSet<>();
                    lobby.offloadAll(entities, updateCandidates);
                    lobby.sendUpdate(updateCandidates);
                    break;

                case LMP_ASSIGN:
                    StringTokenizer st = new StringTokenizer(info, ":");
                    int newOwnerId = Integer.parseInt(st.nextToken());
                    lobby.lobbyActions.changeOwner(entities,
                          LobbyUtility.getForces(lobby.game(), st.nextToken()),
                          newOwnerId);
                    break;

                case LMP_DETACH_TRAILER:
                    updateCandidates = new HashSet<>();
                    lobby.detachTrailers(entities, updateCandidates);
                    lobby.sendUpdate(updateCandidates);
                    break;

                case LMP_DETACH_FROM_TRACTOR:
                    updateCandidates = new HashSet<>();
                    lobby.detachFromTractors(entities, updateCandidates);
                    lobby.sendUpdate(updateCandidates);
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
                    List<Integer> entityIds = entities.stream().map(Entity::getId).toList();
                    LobbyUtility.liveEntityReadoutAction(entityIds, lobby.canSeeAll(entities),
                          lobby.getClientGUI().getFrame(), lobby.game());
                    break;

                case LMP_BV:
                    LobbyUtility.mekBVAction(entities,
                          lobby.canSeeAll(entities),
                          false,
                          lobby.getClientGUI().getFrame());
                    break;

                case LMP_COST:
                    LobbyUtility.mekCostAction(entities,
                          lobby.canSeeAll(entities),
                          false,
                          lobby.getClientGUI().getFrame());
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
                    master = Integer.parseInt(info);
                    lobby.lobbyActions.c3JoinNh(entities, master, false);
                    break;

                case LMP_C3CONNECT:
                    master = Integer.parseInt(info);
                    lobby.lobbyActions.c3Connect(entities, master, false);
                    break;

                case LMP_C3_FORM_C3:
                    master = Integer.parseInt(info);
                    lobby.lobbyActions.c3Connect(entities, master, true);
                    break;

                case LMP_C3_FORM_NHC3:
                    master = Integer.parseInt(info);
                    lobby.lobbyActions.c3JoinNh(entities, master, true);
                    break;

                case LMP_UNLOAD_ALL_FROM_BAY:
                    int bay = Integer.parseInt(info);
                    lobby.lobbyActions.unloadFromBay(entities, bay);
                    break;

                case LMP_MOVE_DOWN:
                    lobby.lobbyActions.forceMove(LobbyUtility.getForces(lobby.game(), info), entities, false);
                    break;

                case LMP_MOVE_UP:
                    lobby.lobbyActions.forceMove(LobbyUtility.getForces(lobby.game(), info), entities, true);
                    break;

                case LMP_PRIORITY_TARGET:
                    lobby.lobbyActions.setPriorityTarget(info, entities);
                    break;

                case LMP_ALPHA_STRIKE:
                    lobby.lobbyActions.showAlphaStrikeView(entities);
                    break;

                case LMP_AUTOCONFIG:
                case LMP_RANDOM_CONFIG:
                case LMP_SAVE_CONFIG:
                case LMP_APPLY_CONFIG:
                    runMunitionConfigCMD(entities, command);
                    break;
            }
        } catch (InvalidPacketDataException e) {
            LOGGER.error("Invalid packet data:", e);
        }
    }

    /**
     * Run config command for a set of entities
     *
     */
    private void runMunitionConfigCMD(Set<Entity> entities, String command) {
        TeamLoadOutGenerator tlg = new TeamLoadOutGenerator(lobby.game());
        MunitionTree munitionTree = new MunitionTree();
        ArrayList<Entity> entityArrayList = new ArrayList<>(entities);
        ClientGUI clientGUI = lobby.getClientGUI();
        // Team team = lobby.game().getTeamForPlayer(entityArrayList.get(0).getOwner());
        Team team = clientGUI.getClient().getGame().getTeamForPlayer(entityArrayList.get(0).getOwner());
        String faction = (team != null) ? team.getFaction() : FactionRecord.IS_GENERAL_KEY;

        // Parameters are generated _from_ the teams' information, _for_ the selected entities
        ReconfigurationParameters reconfigurationParameters = tlg.generateParameters(entityArrayList, faction, team);
        // Extra nuke controls don't apply in the context menu; rely on game option!
        reconfigurationParameters.nukesBannedForMe = lobby.game()
              .getOptions()
              .booleanOption(OptionsConstants.ADVANCED_AERO_RULES_AT2_NUKES);
        // Reduce Pirate ammo somewhat; others get full loadouts
        reconfigurationParameters.isPirate = faction.equalsIgnoreCase("PIR");
        reconfigurationParameters.binFillPercent = (reconfigurationParameters.isPirate) ?
              TeamLoadOutGenerator.UNSET_FILL_RATIO :
              1.0f;

        boolean reconfigured = false;

        switch (command) {
            case LMP_AUTOCONFIG:
                munitionTree = TeamLoadOutGenerator.generateMunitionTree(reconfigurationParameters,
                      entityArrayList,
                      "");
                resetBombChoices(clientGUI, lobby.game(), entityArrayList);
                tlg.reconfigureEntities(entityArrayList, faction, munitionTree, reconfigurationParameters);
                reconfigured = true;
                break;
            case LMP_RANDOM_CONFIG:
                munitionTree = TeamLoadOutGenerator.generateRandomizedMT();
                resetBombChoices(clientGUI, lobby.game(), entityArrayList);
                tlg.reconfigureEntities(entityArrayList, faction, munitionTree, reconfigurationParameters);
                reconfigured = true;
                break;
            case LMP_SAVE_CONFIG:
                munitionTree.loadEntityList(entityArrayList);
                saveLoadout(munitionTree);
                break;
            case LMP_APPLY_CONFIG:
                munitionTree = loadLoadout();
                if (null != munitionTree) {
                    // Apply to entities
                    resetBombChoices(clientGUI, lobby.game(), entityArrayList);
                    tlg.reconfigureEntities(entityArrayList, faction, munitionTree, reconfigurationParameters);
                    reconfigured = true;
                }
                break;
        }
        if (reconfigured) {
            // Have to send reconfig as controlling player
            clientGUI.chatlounge.sendProxyUpdates(entityArrayList,
                  lobby.game().getPlayer(entityArrayList.get(0).getOwnerId()));
        }
    }

    public static void resetBombChoices(ClientGUI clientgui, Game game, ArrayList<Entity> el) {
        ArrayList<Entity> resetBombers = new ArrayList<>();
        for (Entity entity : el) {
            if (entity.isBomber() && !entity.isVehicle()) {
                IBomber bomber = (IBomber) entity;
                // Clear existing bomb choices!
                bomber.setIntBombChoices(new BombLoadout());
                bomber.setExtBombChoices(new BombLoadout());
                resetBombers.add(entity);
            }
        }
        if (!resetBombers.isEmpty()) {
            clientgui.chatlounge.sendProxyUpdates(resetBombers, game.getPlayer(el.get(0).getOwnerId()));
        }
    }

    private void saveLoadout(MunitionTree source) {
        //ignoreHotKeys = true;
        JFileChooser fc = new JFileChooser(Paths.get(MMConstants.USER_LOADOUTS_DIR).toAbsolutePath().toString());
        FileNameExtensionFilter adfFilter = new FileNameExtensionFilter(
              "adf files (*.adf)", "adf");
        fc.addChoosableFileFilter(adfFilter);
        fc.setFileFilter(adfFilter);
        fc.setLocation(lobby.getLocation().x + 150, lobby.getLocation().y + 100);
        fc.setDialogTitle(Messages.getString("ClientGui.LoadoutSaveDialog.title"));

        int returnVal = fc.showSaveDialog(lobby);
        if ((returnVal != JFileChooser.APPROVE_OPTION) || (fc.getSelectedFile() == null)) {
            // No file selected?  No loadout!
            return;
        }
        String file = fc.getSelectedFile().getAbsolutePath();
        if (!file.toLowerCase().endsWith(".adf")) {
            file = file + ".adf";
        }
        source.writeToADFFilename(file);
    }

    private MunitionTree loadLoadout() {
        MunitionTree munitionTree;
        JFileChooser jFileChooser = new JFileChooser(Paths.get(MMConstants.USER_LOADOUTS_DIR)
              .toAbsolutePath()
              .toString());
        FileNameExtensionFilter adfFilter = new FileNameExtensionFilter(
              "adf files (*.adf)", "adf");
        jFileChooser.addChoosableFileFilter(adfFilter);
        jFileChooser.setFileFilter(adfFilter);
        jFileChooser.setLocation(lobby.getLocation().x + 150, lobby.getLocation().y + 100);
        jFileChooser.setDialogTitle(Messages.getString("ClientGui.LoadoutLoadDialog.title"));

        int returnVal = jFileChooser.showOpenDialog(lobby);
        if ((returnVal != JFileChooser.APPROVE_OPTION) || (jFileChooser.getSelectedFile() == null)) {
            // No file selected?  No loadout!
            return null;
        }

        String file = jFileChooser.getSelectedFile().getAbsolutePath();
        munitionTree = new MunitionTree(file);
        return munitionTree;
    }

    /** Calls lobby actions for a single entity. */
    private void singleEntityAction(String command, Entity entity, String info) {
        switch (command) {
            case LMP_CONFIGURE:
                lobby.lobbyActions.customizeMek(entity);
                break;

            case LMP_TOW:
                lobby.lobbyActions.tow(entity, info);
                break;
        }
    }
}

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

import static megamek.client.ui.swing.lobby.LobbyMekPopup.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import megamek.MMConstants;
import megamek.client.generator.ReconfigurationParameters;
import megamek.client.generator.TeamLoadOutGenerator;
import megamek.client.ratgenerator.FactionRecord;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.ClientGUI;
import megamek.common.BombType;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.IBomber;
import megamek.common.Player;
import megamek.common.Team;
import megamek.common.containers.MunitionTree;
import megamek.common.force.Force;
import megamek.common.options.OptionsConstants;
import megamek.common.util.StringUtil;

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
            case LMP_TOW:
                if (!entities.isEmpty()) {
                    Entity randomSelected = entities.stream().findAny().get();
                    singleEntityAction(command, randomSelected, info);
                }
                break;

                // Multi entity commands
            case LMP_ALPHASTRIKE:
            case LMP_AUTOCONFIG:
            case LMP_SAVECONFIG:
            case LMP_APPLYCONFIG:
            case LMP_RANDOMCONFIG:
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
            case LMP_FCDELETEEMPTY:
                forceAction(command, entities, info);
                break;
        }
    }

    /** Calls lobby actions for forces. */
    private void forceAction(String command, Set<Entity> entities, String info) {
        switch (command) {
            case LMP_FCREATESUB:
                int parentId = StringUtil.toInt(info, Force.NO_FORCE);
                lobby.lobbyActions.forceCreateSub(parentId);
                break;

            case LMP_FADDTO:
                int forceId = StringUtil.toInt(info, Force.NO_FORCE);
                lobby.lobbyActions.forceAddEntity(entities, forceId);
                break;

            case LMP_FRENAME:
                forceId = StringUtil.toInt(info, Force.NO_FORCE);
                lobby.lobbyActions.forceRename(forceId);
                break;

            case LMP_FCREATETOP:
                lobby.lobbyActions.forceCreateEmpty();
                break;

            case LMP_FCDELETEEMPTY:
                forceId = StringUtil.toInt(info, Force.NO_FORCE);
                lobby.lobbyActions.forceDeleteEmpty(forceId);
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
                    forceIds.add(StringUtil.toInt(fst.nextToken(), Force.NO_FORCE));
                }
                lobby.lobbyActions.forcePromote(forceIds);
                break;

            case LMP_FASSIGN:
                StringTokenizer st = new StringTokenizer(info, ":");
                int newOwnerId = StringUtil.toInt(st.nextToken(), Player.PLAYER_NONE);
                lobby.lobbyActions.forceAssignFull(LobbyUtility.getForces(lobby.game(), st.nextToken()), newOwnerId);
                break;

            case LMP_FASSIGNONLY:
                st = new StringTokenizer(info, ":");
                newOwnerId = StringUtil.toInt(st.nextToken(), Player.PLAYER_NONE);
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
                LobbyUtility.mekReadoutAction(entities, lobby.canSeeAll(entities), false, lobby.getClientgui().getFrame());
                break;

            case LMP_BV:
                LobbyUtility.mekBVAction(entities, lobby.canSeeAll(entities), false, lobby.getClientgui().getFrame());
                break;

            case LMP_COST:
                LobbyUtility.mekCostAction(entities, lobby.canSeeAll(entities), false, lobby.getClientgui().getFrame());
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

            case LMP_AUTOCONFIG:
            case LMP_RANDOMCONFIG:
            case LMP_SAVECONFIG:
            case LMP_APPLYCONFIG:
                runMunitionConfigCMD(entities, command);
                break;
        }
    }

    /** Run config command for a set of entities
     *
     * @param entities
     * @param command
     */
    private void runMunitionConfigCMD(Set<Entity> entities, String command) {
        TeamLoadOutGenerator tlg = new TeamLoadOutGenerator(lobby.game());
        MunitionTree mt = new MunitionTree();
        ArrayList<Entity> el = new ArrayList<Entity>(entities);
        ClientGUI clientgui = lobby.getClientgui();
        // Team team = lobby.game().getTeamForPlayer(el.get(0).getOwner());
        Team team = clientgui.getClient().getGame().getTeamForPlayer(el.get(0).getOwner());
        String faction = (team != null) ? team.getFaction() : FactionRecord.IS_GENERAL_KEY;

        // Parameters are generated _from_ the teams' information, _for_ the selected entities
        ReconfigurationParameters rp = tlg.generateParameters(el, faction, team);
        // Extra nuke controls don't apply in the context menu; rely on game option!
        rp.nukesBannedForMe = lobby.game().getOptions().booleanOption(OptionsConstants.ADVAERORULES_AT2_NUKES);
        // Reduce Pirate ammo somewhat; others get full loadouts
        rp.isPirate = faction.toUpperCase().equals("PIR");
        rp.binFillPercent = (rp.isPirate) ? TeamLoadOutGenerator.UNSET_FILL_RATIO : 1.0f;

        boolean reconfigured = false;

        switch (command) {
            case LMP_AUTOCONFIG:
                mt = tlg.generateMunitionTree(rp, el, "");
                resetBombChoices(clientgui, lobby.game(), el);
                tlg.reconfigureEntities(el, faction, mt, rp);
                reconfigured = true;
                break;
            case LMP_RANDOMCONFIG:
                mt = TeamLoadOutGenerator.generateRandomizedMT();
                resetBombChoices(clientgui, lobby.game(), el);
                tlg.reconfigureEntities(el, faction, mt, rp);
                reconfigured = true;
                break;
            case LMP_SAVECONFIG:
                mt.loadEntityList(el);
                saveLoadout(mt);
                break;
            case LMP_APPLYCONFIG:
                mt = loadLoadout();
                if (null != mt && null != clientgui) {
                    // Apply to entities
                    resetBombChoices(clientgui, lobby.game(), el);
                    tlg.reconfigureEntities(el, faction, mt, rp);
                    reconfigured = true;
                }
                break;
        }
        if (reconfigured) {
            // Have to send reconfig as controlling player
            clientgui.chatlounge.sendProxyUpdates(el, lobby.game().getPlayer(el.get(0).getOwnerId()));
        }
    }

    public static void resetBombChoices(ClientGUI clientgui, Game game, ArrayList<Entity> el) {
        ArrayList<Entity> resetBombers = new ArrayList<>();
        for (Entity entity: el) {
            if (entity.isBomber() && !entity.isVehicle()) {
                IBomber bomber = (IBomber) entity;
                // Clear existing bomb choices!
                bomber.setIntBombChoices(new int[BombType.B_NUM]);
                bomber.setExtBombChoices(new int[BombType.B_NUM]);
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
        if (fc.getSelectedFile() != null) {
            String file = fc.getSelectedFile().getAbsolutePath();
            if (!file.toLowerCase().endsWith(".adf")) {
                file = file + ".adf";
            }
            source.writeToADFFilename(file);
        }
    }

    private MunitionTree loadLoadout() {
        MunitionTree mt = null;
        JFileChooser fc = new JFileChooser(Paths.get(MMConstants.USER_LOADOUTS_DIR).toAbsolutePath().toString());
        FileNameExtensionFilter adfFilter = new FileNameExtensionFilter(
                "adf files (*.adf)", "adf");
        fc.addChoosableFileFilter(adfFilter);
        fc.setFileFilter(adfFilter);
        fc.setLocation(lobby.getLocation().x + 150, lobby.getLocation().y + 100);
        fc.setDialogTitle(Messages.getString("ClientGui.LoadoutLoadDialog.title"));

        int returnVal = fc.showOpenDialog(lobby);
        if ((returnVal != JFileChooser.APPROVE_OPTION) || (fc.getSelectedFile() == null)) {
            // No file selected?  No loadout!
            return null;
        }

        if (fc.getSelectedFile() != null) {
            String file = fc.getSelectedFile().getAbsolutePath();
            mt = new MunitionTree(file);
        }
        return mt;
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

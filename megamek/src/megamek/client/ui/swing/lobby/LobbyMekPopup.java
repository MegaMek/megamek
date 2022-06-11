/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.swing.lobby;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.util.MenuScroller;
import megamek.client.ui.swing.util.ScalingPopup;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.*;
import megamek.common.force.Force;
import megamek.common.force.Forces;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.preference.PreferenceManager;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.stream.Collectors;

import static megamek.client.ui.swing.util.UIUtil.menuItem;
import static megamek.common.util.CollectionUtil.anyOneElement;
import static megamek.common.util.CollectionUtil.theElement;

/** Creates the Lobby Mek right-click pop-up menu for both the sortable table and the force tree. */
class LobbyMekPopup {
    
    static final String LMP_SKILLS = "SKILLS";
    static final String LMP_CALLSIGN = "CALLSIGN";
    static final String LMP_NAME = "NAME";
    static final String LMP_C3DISCONNECT = "C3DISCONNECT";
    static final String LMP_DEPLOY = "DEPLOY";
    static final String LMP_HEAT = "HEAT";
    static final String LMP_HULLDOWN = "HULLDOWN";
    static final String LMP_PRONE = "PRONE";
    static final String LMP_STAND = "STAND";
    static final String LMP_NOHIDE = "NOHIDE";
    static final String LMP_HIDE = "HIDE";
    static final String LMP_HIDDEN = "HIDDEN";
    static final String LMP_FASSIGNONLY = "FASSIGNONLY";
    static final String LMP_FASSIGN = "FASSIGN";
    static final String LMP_SAVE_QUIRKS_MODEL = "SAVE_QUIRKS_MODEL";
    static final String LMP_SAVE_QUIRKS_ALL = "SAVE_QUIRKS_ALL";
    static final String LMP_RAPIDFIREMG_OFF = "RAPIDFIREMG_OFF";
    static final String LMP_RAPIDFIREMG_ON = "RAPIDFIREMG_ON";
    static final String LMP_HOTLOAD_OFF = "HOTLOAD_OFF";
    static final String LMP_HOTLOAD_ON = "HOTLOAD_ON";
    static final String LMP_SWAP = "SWAP";
    static final String LMP_ASSIGN = "ASSIGN";
    static final String LMP_C3CONNECT = "C3CONNECT";
    static final String LMP_C3JOIN = "C3JOIN";
    static final String LMP_C3FORMNHC3 = "C3FORMNHC3";
    static final String LMP_C3FORMC3 = "C3FORMC3";
    static final String LMP_C3LM = "C3LM";
    static final String LMP_C3CM = "C3CM";
    static final String LMP_SQUADRON = "SQUADRON";
    static final String LMP_LOAD = "LOAD";
    static final String LMP_FADDTO = "FADDTO";
    static final String LMP_FREMOVE = "FREMOVE";
    static final String LMP_FPROMOTE = "FPROMOTE";
    static final String LMP_FRENAME = "FRENAME";
    static final String LMP_FCREATESUB = "FCREATESUB";
    static final String LMP_FCREATETOP = "FCREATETOP";
    static final String LMP_FCREATEFROM = "FCREATEFROM";
    static final String LMP_DELETE = "DELETE";
    static final String LMP_UNLOADALL = "UNLOADALL";
    static final String LMP_UNLOAD = "UNLOAD";
    static final String LMP_MOVE_DOWN = "MOVE_DOWN";
    static final String LMP_INDI_CAMO = "INDI_CAMO";
    static final String LMP_DAMAGE = "DAMAGE";
    static final String LMP_CONFIGURE_ALL = "CONFIGURE_ALL";
    static final String LMP_CONFIGURE = "CONFIGURE";
    static final String LMP_BV = "BV";
    static final String LMP_COST = "COST";
    static final String LMP_VIEW = "VIEW";
    static final String LMP_MOVE_UP = "MOVE_UP";
    static final String LMP_PRIO_TARGET = "PRIO_TARGET";
    static final String LMP_ALPHASTRIKE = "ALPHASTRIKE";
    static final String LMP_SBFFORMATION = "SBFFORMATION";

    private static final String NOINFO = "|-1";
    
    static final String LMP_UNLOADALLFROMBAY = "UNLOADALLFROMBAY";
    
    static ScalingPopup getPopup(List<Entity> entities, List<Force> forces, ActionListener listener,
                                 ChatLounge lobby) {
        ClientGUI clientGui = lobby.getClientgui();
        Game game = lobby.game();
        GameOptions opts = game.getOptions();
        
        boolean optQuirks = opts.booleanOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS);
        boolean optBurstMG = opts.booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_BURST);
        boolean optLRMHotLoad = opts.booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_HOTLOAD);
        boolean optCapFighters = opts.booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_CAPITAL_FIGHTER);

        // A set of all selected entities and all entities in selected forces and their subforces
        Set<Entity> joinedEntities = new HashSet<>(entities);
        for (Force force: forces) {
            joinedEntities.addAll(game.getForces().getFullEntities(force));
        }  

        // Find certain unit features among all units the player can access
        // Used to hide some menu items entirely like "Form Squadron" when there's no fighter in the game
        HashSet<Entity> accessibleEntities = new HashSet<>(game.getEntitiesVector());
        accessibleEntities.removeIf(lobby.lobbyActions::isNotEditable);
        boolean accessibleFighters = accessibleEntities.stream().anyMatch(Entity::isFighter);
        boolean accessibleTransportBays = accessibleEntities.stream().anyMatch(e -> !e.getTransportBays().isEmpty());
        boolean accessibleCarriers = accessibleEntities.stream().anyMatch(e -> !e.getLoadedUnits().isEmpty());
        boolean accessibleProtomeks = accessibleEntities.stream().anyMatch(e -> e.hasETypeFlag(Entity.ETYPE_PROTOMECH));

        // Find what can be done with the selected entities incl. those in selected forces
        boolean anyCarrier = joinedEntities.stream().anyMatch(e -> !e.getLoadedUnits().isEmpty());
        boolean noneEmbarked = joinedEntities.stream().allMatch(e -> e.getTransportId() == Entity.NONE);
        boolean allProtomeks = joinedEntities.stream().allMatch(e -> e.hasETypeFlag(Entity.ETYPE_PROTOMECH));
        boolean anyRFMGOn = joinedEntities.stream().anyMatch(LobbyMekPopup::hasRapidFireMG);
        boolean anyRFMGOff = joinedEntities.stream().anyMatch(LobbyMekPopup::hasNormalFireMG);
        boolean anyHLOn = joinedEntities.stream().anyMatch(LobbyMekPopup::hasHotLoaded);
        boolean anyHLOff = joinedEntities.stream().anyMatch(LobbyMekPopup::hasNonHotLoaded);

        boolean hasjoinedEntities = !joinedEntities.isEmpty();
        boolean joinedOneEntitySelected = (entities.size() == 1) && forces.isEmpty();
        boolean canSeeAll = lobby.canSeeAll(joinedEntities);

        ScalingPopup popup = new ScalingPopup();

        // All command strings should follow the layout COMMAND|INFO|ID1,ID2,I3...
        // and use -1 when something is not needed (COMMAND|-1|-1)
        String eId = "|" + (entities.isEmpty() ? "-1" : entities.get(0).getId());
        String eIds = enToken(entities);
        String seIds = enToken(joinedEntities);

        popup.add(menuItem("View...", LMP_VIEW + NOINFO + seIds, hasjoinedEntities, listener, KeyEvent.VK_V));
        popup.add(menuItem("View BV Calculation...", LMP_BV + NOINFO + seIds, hasjoinedEntities, listener, KeyEvent.VK_B));
        popup.add(menuItem("View Cost Calculation...", LMP_COST + NOINFO + seIds, hasjoinedEntities, listener));
        popup.add(ScalingPopup.spacer());

        if (joinedOneEntitySelected) {
            popup.add(menuItem("Configure...", LMP_CONFIGURE + NOINFO + eId, hasjoinedEntities, listener, KeyEvent.VK_C));
        } else {
            popup.add(menuItem("Configure...", LMP_CONFIGURE_ALL + NOINFO + seIds, hasjoinedEntities, listener, KeyEvent.VK_C));
        }
        popup.add(menuItem("Edit Damage...", LMP_DAMAGE + NOINFO + seIds, hasjoinedEntities, listener, KeyEvent.VK_E));
        popup.add(menuItem("Set individual camo...", LMP_INDI_CAMO + NOINFO + seIds, hasjoinedEntities, listener, KeyEvent.VK_I));

        if (lobby.isForceView()) {
            JMenuItem moveUp = menuItem("Move Up", LMP_MOVE_UP + "|" + foToken(forces) + eIds, !forces.isEmpty() || !entities.isEmpty(), listener, KeyEvent.VK_I);
            moveUp.setAccelerator(KeyStroke.getKeyStroke("ctrl UP"));
            popup.add(moveUp);
            JMenuItem moveDn = menuItem("Move Down", LMP_MOVE_DOWN + "|" + foToken(forces) + eIds, !forces.isEmpty() || !entities.isEmpty(), listener, KeyEvent.VK_I);
            moveDn.setAccelerator(KeyStroke.getKeyStroke("ctrl DOWN"));
            popup.add(moveDn);
        }

        popup.add(deployMenu(clientGui, hasjoinedEntities, listener, joinedEntities));
        popup.add(randomizeMenu(hasjoinedEntities, listener, seIds));
        popup.add(swapPilotMenu(hasjoinedEntities, joinedEntities, clientGui, listener));
        popup.add(prioTargetMenu(clientGui, hasjoinedEntities, listener, joinedEntities));

        if (optBurstMG || optLRMHotLoad) {
            popup.add(equipMenu(anyRFMGOn, anyRFMGOff, anyHLOn, anyHLOff, optLRMHotLoad, optBurstMG, listener, seIds));
        }
        
        if (optQuirks) {
            popup.add(quirksMenu(!entities.isEmpty() && canSeeAll, listener, eIds));
        }
        
        popup.add(ScalingPopup.spacer());
        popup.add(changeOwnerMenu(!entities.isEmpty() || !forces.isEmpty(), clientGui, listener, entities, forces));
        popup.add(loadMenu(clientGui, true, listener, joinedEntities));
        
        if (accessibleCarriers) {
            popup.add(menuItem("Disembark / leave from carriers", LMP_UNLOAD + NOINFO + seIds, !noneEmbarked, listener));
            popup.add(menuItem("Offload all carried units", LMP_UNLOADALL + NOINFO + seIds, anyCarrier, listener));
        }

        if (accessibleTransportBays) {
            popup.add(offloadBayMenu(anyCarrier, joinedEntities, listener));
        }

        if (accessibleFighters && optCapFighters) {
            popup.add(squadronMenu(clientGui, true, listener, joinedEntities));
        }

        if (accessibleProtomeks) {
            popup.add(protoMenu(clientGui, allProtomeks, listener, joinedEntities));
        }

        popup.add(c3Menu(hasjoinedEntities, joinedEntities, clientGui, listener));
        popup.add(forceMenu(lobby, entities, forces, listener));

        popup.add(ScalingPopup.spacer());
        popup.add(menuItem("View AlphaStrike Stats", LMP_ALPHASTRIKE + NOINFO + seIds, true, listener));
        popup.add(menuItem("Convert to SBF Formation", LMP_SBFFORMATION + "|" + foToken(forces) + eIds, lobby.isForceView(), listener));
        popup.add(ScalingPopup.spacer());
        popup.add(menuItem("Delete", LMP_DELETE + "|" + foToken(forces) + seIds, !entities.isEmpty() && forces.isEmpty(), listener, KeyEvent.VK_D));

        return popup;
    }

    /**
     * Returns the "Force" submenu, allowing assignment to forces
     */
    private static JMenu forceMenu(ChatLounge lobby, List<Entity> entities, List<Force> forces, ActionListener listener) {
        JMenu menu = new JMenu("Force");
        if (!forces.isEmpty() || !entities.isEmpty()) {
            menu.add(menuItem("Create Force from...", LMP_FCREATEFROM + "|" + foToken(forces) + enToken(entities), true, listener));
        }
        menu.add(menuItem("Add empty Force...", LMP_FCREATETOP + NOINFO + NOINFO, true, listener));

        // If exactly one force is selected, offer force options
        if ((forces.size() == 1) && entities.isEmpty()) {
            Force force = forces.get(0);
            boolean editable = lobby.lobbyActions.isEditable(force);
            String fId = "|" + force.getId();
            menu.add(menuItem("Add Subforce...", LMP_FCREATESUB + fId + NOINFO, editable, listener));
            menu.add(menuItem("Rename", LMP_FRENAME + fId + NOINFO, editable, listener));
            menu.add(menuItem("Promote to Top-Level Force", LMP_FPROMOTE + fId + NOINFO, editable && !force.isTopLevel(), listener));
        }

        // If entities are selected but no forces, offer entity options
        if (forces.isEmpty() && !entities.isEmpty() && LobbyUtility.haveSingleOwner(entities)) {
            // Add to force menu tree
            JMenu addMenu = new JMenu("Add to...");
            for (Force force: lobby.game().getForces().getTopLevelForces()) {
                addMenu.add(forceTreeMenu(force, lobby.game(), enToken(entities), listener));
            }
            menu.add(addMenu);
            menu.add(menuItem("Remove from Force", LMP_FREMOVE + NOINFO + enToken(entities), true, listener));
        }

        menu.setEnabled(menu.getItemCount() > 0);
        return menu;
    }

    private static JMenuItem forceTreeMenu(Force force, Game game, String enToken, ActionListener listener) {
        JMenuItem result;
        String item = "<HTML>" + force.getName() + idString(game, force.getId());
        if (force.getSubForces().isEmpty()) {
            result = menuItem(item, LMP_FADDTO + "|" + force.getId() + enToken, true, listener);
        } else {
            result = new JMenu(item);
            for (Integer subForceId: force.getSubForces()) {
                result.add(forceTreeMenu(game.getForces().getForce(subForceId), game, enToken, listener));
            }
        }
        return result;
    }

    static String idString(Game game, int id) {
        if (PreferenceManager.getClientPreferences().getShowUnitId()) {
            return " <FONT" + UIUtil.colorString(UIUtil.uiGray()) +">[" + id + "]</FONT>"; 
        } else {
            return "";
        }
    }

    /**
     * Returns the "Load" submenu, allowing general embarking
     */
    private static JMenu loadMenu(ClientGUI cg, boolean enabled, ActionListener listener,
                                  Collection<Entity> entities) {
        Game game = cg.getClient().getGame();
        JMenu menu = new JMenu("Load onto");
        if (enabled && !entities.isEmpty()) {
            // DropShip -> JumpShip loading gives free collars info
            if (entities.stream().allMatch(e -> e instanceof Dropship)) {
                game.getEntitiesVector().stream()
                .filter(e -> e instanceof Jumpship)
                .filter(e -> !entities.contains(e))
                .filter(e -> canLoadAll(e, entities))
                .forEach(e -> menu.add(menuItem(
                        "<HTML>" + e.getShortNameRaw() + idString(game, e.getId()) + " (Free Collars: " + ((Jumpship) e).getFreeDockingCollars() + ")", 
                        LMP_LOAD + "|" + e.getId() + ":-1" + enToken(entities), enabled, listener)));
            } else if (entities.stream().noneMatch(e -> e.hasETypeFlag(Entity.ETYPE_PROTOMECH))) {
                // Standard loading, not ProtoMeks, not DropShip -> JumpShip
                game.getEntitiesVector().stream()
                .filter(e -> !e.isCapitalFighter(true))
                .filter(e -> !entities.contains(e))
                .filter(e -> canLoadAll(e, entities))
                .forEach(e -> menu.add(menuItem(
                        "<HTML>" + e.getShortNameRaw() + idString(game, e.getId()), 
                        LMP_LOAD + "|" + e.getId() + ":-1" + enToken(entities), enabled, listener)));
            }
        }
        menu.setEnabled(enabled && (menu.getItemCount() > 0));
        return menu;
    }

    /**
     * Returns the "Load ProtoMek" submenu
     */
    private static JMenu protoMenu(ClientGUI cg, boolean enabled, ActionListener listener,
                                   Collection<Entity> entities) {
        JMenu menu = new JMenu("Load ProtoMek");
        if (enabled && entities.stream().anyMatch(e -> e.hasETypeFlag(Entity.ETYPE_PROTOMECH))) {
            Entity entity = entities.stream().filter(e -> e.hasETypeFlag(Entity.ETYPE_PROTOMECH)).findAny().get();
            for (Entity loader: cg.getClient().getGame().getEntitiesVector()) {
                if (!loader.hasETypeFlag(Entity.ETYPE_MECH) || !loader.canLoad(entity, false)) {
                    continue;
                }
                Transporter front = null;
                Transporter rear = null;
                for (Transporter t : loader.getTransports()) {
                    if (t instanceof ProtomechClampMount) {
                        if (((ProtomechClampMount) t).isRear()) {
                            rear = t;
                        } else {
                            front = t;
                        }
                    }
                }
                JMenu loaderMenu = new JMenu(loader.getShortName());
                if ((front != null) && front.canLoad(entity)
                        && ((entity.getWeightClass() < EntityWeightClass.WEIGHT_SUPER_HEAVY)
                                || (rear == null) || rear.getLoadedUnits().isEmpty())) {
                    loaderMenu.add(menuItem("Onto Front", LMP_LOAD + "|" + loader.getId() + ":0" + enToken(entities), enabled, listener));
                }
                boolean frontUltra = (front != null)
                        && front.getLoadedUnits().stream()
                        .anyMatch(l -> l.getWeightClass() == EntityWeightClass.WEIGHT_SUPER_HEAVY);
                if ((rear != null) && rear.canLoad(entity) && !frontUltra) {
                    loaderMenu.add(menuItem("Onto Rear", LMP_LOAD + "|" + loader.getId() + ":1" + enToken(entities), enabled, listener));
                }
                if (loaderMenu.getItemCount() > 0) {
                    menu.add(loaderMenu);
                }
            }
        }
        menu.setEnabled(enabled && (menu.getItemCount() > 0));
        return menu;
    }

    /**
     * Returns the "Fighter Squadron" submenu, allowing to assign units to or
     * create a fighter squadron
     */
    private static JMenu squadronMenu(ClientGUI cg, boolean enabled, ActionListener listener,
                                      Collection<Entity> entities) {
        JMenu menu = new JMenu("Fighter Squadrons");
        boolean hasFighter = entities.stream().anyMatch(Entity::isFighter);
        if (enabled && hasFighter) {
            menu.add(menuItem("Form Fighter Squadron", LMP_SQUADRON + NOINFO + enToken(entities), enabled, listener));

            // Join [Squadron] menu items
            cg.getClient().getGame().getEntitiesVector().stream()
                .filter(e -> e instanceof FighterSquadron)
                .filter(e -> !entities.contains(e))
                .filter(e -> canLoadAll(e, entities))
                .forEach(e -> menu.add(menuItem("Join " + e.getShortName(), 
                        LMP_LOAD + "|" + e.getId() + ":-1" + enToken(entities), enabled, listener)));
        }
        menu.setEnabled(enabled && (menu.getItemCount() > 0));
        return menu;
    }

    /**
     * Returns the "Priority Target" submenu, allowing to assign units as a strategic
     * target to a local Princess bot.
     */
    private static JMenu prioTargetMenu(ClientGUI cg, boolean enabled, ActionListener listener,
                                        Collection<Entity> entities) {
        JMenu menu = new JMenu("Set Priority Target for");
        if (enabled && !cg.getBots().isEmpty()) {
            for (String bot : cg.getBots().keySet()) {
                menu.add(menuItem(bot, LMP_PRIO_TARGET + "|" + bot + enToken(entities), enabled, listener));
            }
        }
        menu.setEnabled(enabled && (menu.getItemCount() > 0));
        return menu;
    }

    /** Returns true when the loader can load all the given entities (under lobby conditions). */
    private static boolean canLoadAll(Entity loader, Collection<Entity> entities) {
        return entities.stream().allMatch(e -> loader.canLoad(e, false));
    }

    /**
     * Returns the "Deploy" submenu, allowing late deployment
     */
    private static JMenu deployMenu(ClientGUI clientGui, boolean enabled, ActionListener listener,
                                    Set<Entity> entities) {
        String eIds = enToken(entities);
        JMenu menu = new JMenu("Deploy");
        if (enabled) {
            // Hidden, Prone, Hull-down
            if (clientGui.getClient().getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_HIDDEN_UNITS)) {
                boolean anyHidden = entities.stream().anyMatch(Entity::isHidden);
                boolean anyNotHidden = entities.stream().anyMatch(e -> !e.isHidden());
                menu.add(menuItem("Hidden", LMP_HIDDEN + "|" + LMP_HIDE + eIds, anyNotHidden, listener));
                menu.add(menuItem("Not Hidden", LMP_HIDDEN + "|" + LMP_NOHIDE + eIds, anyHidden, listener));
                menu.add(ScalingPopup.spacer());
            }

            menu.add(menuItem("Standing", LMP_STAND + "|" + LMP_STAND + eIds, enabled, listener));
            menu.add(menuItem("Prone", LMP_STAND + "|" + LMP_PRONE + eIds, enabled, listener));
            if (clientGui.getClient().getGame().getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_HULL_DOWN)) {
                menu.add(menuItem("Hull-Down", LMP_STAND + "|" + LMP_HULLDOWN + eIds, enabled, listener));
            }
            menu.add(ScalingPopup.spacer());

            // Heat
            JMenu heatMenu = new JMenu("Heat at start");
            heatMenu.add(menuItem("No heat", LMP_HEAT + "|0" + eIds, enabled, listener));
            for (int i = 1; i < 11; i++) {
                heatMenu.add(menuItem("Heat " + i, LMP_HEAT + "|" + i + eIds, enabled, listener));
            }
            JMenu subHeatMenu = new JMenu("More heat");
            for (int i = 11; i < 41; i++) {
                subHeatMenu.add(menuItem("Heat " + i, LMP_HEAT + "|" + i + eIds, enabled, listener));
            }
            heatMenu.add(subHeatMenu);
            menu.add(heatMenu);
            menu.add(ScalingPopup.spacer());

            // Late deployment
            JMenu lateMenu = new JMenu("Deployment round");
            lateMenu.add(menuItem("At game start", LMP_DEPLOY + "|0" + eIds, enabled, listener));
            for (int i = 1; i < 11; i++) {
                lateMenu.add(menuItem("Before round " + i, LMP_DEPLOY + "|" + i + eIds, enabled, listener));
            }
            JMenu veryLateMenu = new JMenu("Later");
            for (int i = 11; i < 41; i++) {
                veryLateMenu.add(menuItem("Before round " + i, LMP_DEPLOY + "|" + i + eIds, enabled, listener));
            }
            
            lateMenu.add(veryLateMenu);
            menu.add(lateMenu);
        }
        menu.setEnabled(enabled);
        return menu;
    }

    /**
     * Returns the "Randomize" submenu, allowing to randomly assign name, callsign and skills
     */
    private static JMenu randomizeMenu(boolean enabled, ActionListener listener, String eIds) {
        // listener menu uses the following Mnemonic Keys: C, N, S

        JMenu menu = new JMenu("Randomize");
        menu.setEnabled(enabled);
        menu.setMnemonic(KeyEvent.VK_R);

        menu.add(menuItem("Name", LMP_NAME + NOINFO + eIds, enabled, listener, KeyEvent.VK_N));
        menu.add(menuItem("Callsign", LMP_CALLSIGN + NOINFO + eIds, enabled, listener, KeyEvent.VK_C));
        menu.add(menuItem("Skills", LMP_SKILLS + NOINFO + eIds, enabled, listener, KeyEvent.VK_S));
        return menu;
    }

    /** Returns the C3 computer submenu. */
    private static JMenu c3Menu(boolean enabled, Collection<Entity> entities, ClientGUI cg,
                                ActionListener listener) {
        JMenu menu = new JMenu("C3");

        if (entities.stream().anyMatch(Entity::hasAnyC3System)) {

            menu.add(menuItem("Disconnect", LMP_C3DISCONNECT + NOINFO + enToken(entities), enabled, listener));

            if (entities.stream().anyMatch(e -> e.hasC3MM() || e.hasC3M())) {
                boolean allCM = entities.stream().allMatch(Entity::isC3CompanyCommander);
                menu.add(menuItem("Set as C3 Company Master", LMP_C3CM + NOINFO + enToken(entities), !allCM, listener));
                boolean allLM = entities.stream().allMatch(Entity::isC3IndependentMaster);
                menu.add(menuItem("Set as C3 Lance Master", LMP_C3LM + NOINFO + enToken(entities), !allLM, listener));
            }

            // Special treatment if exactly a C3SSSM is selected
            if (entities.size() == 4) {
                long countM = entities.stream().filter(Entity::hasC3M).count();
                long countS = entities.stream().filter(Entity::hasC3S).count();
                if (countM == 1 && countS == 3) {
                    Entity master = entities.stream().filter(Entity::hasC3M).findAny().get();
                    menu.add(menuItem("Form C3 Lance", LMP_C3FORMC3 + "|" + master.getId() + enToken(entities), true, listener));
                }
            }
            
            // Special treatment if a group of NhC3 is selected
            if (entities.size() > 1 && entities.size() <= 6) {
                Entity master = anyOneElement(entities);
                if (entities.stream().allMatch(e -> LobbyUtility.sameNhC3System(master, e))) {
                    menu.add(menuItem("Form C3 Lance", LMP_C3FORMNHC3 + "|" + master.getId() + enToken(entities), true, listener));
                }
            }

            Entity entity = entities.stream().filter(Entity::hasAnyC3System).findAny().get();
            // ideally, find one slave or C3i/NC3/Nova to get some connection options
            entity = entities.stream().filter(e -> e.hasC3S() || e.hasNhC3()).findAny().orElse(entity);
            Game game = cg.getClient().getGame();
            ArrayList<String> usedNetIds = new ArrayList<>();
            
            for (Entity other : cg.getClient().getEntitiesVector()) {
                // ignore enemies and self; only link the same type of C3
                if (entity.isEnemyOf(other) || entity.equals(other)
                        || (entity.hasC3i() != other.hasC3i())
                        || (entity.hasNavalC3() != other.hasNavalC3())
                        || (entity.hasNovaCEWS() != other.hasNovaCEWS())
                        || !other.hasAnyC3System() || other.hasC3S()) {
                    continue;
                }
                // maximum depth of a c3 network is 2 levels.
                Entity eCompanyMaster = other.getC3Master();
                if ((eCompanyMaster != null) && (eCompanyMaster.getC3Master() != eCompanyMaster)) {
                    continue;
                }
                int nodes = other.calculateFreeC3Nodes();
                if (other.hasC3MM() && entity.hasC3M() && other.C3MasterIs(other)) {
                    nodes = other.calculateFreeC3MNodes();
                }
                if (entity.C3MasterIs(other)) {
                    nodes++;
                }
                if ((entity.hasNhC3()) && entity.onSameC3NetworkAs(other)) {
                    nodes++;
                }

                if (other.hasNhC3()) {
                    // Don't add the following checks to the line above
                    if (!entity.onSameC3NetworkAs(other) && !usedNetIds.contains(other.getC3NetId())) {
                        String item = "<HTML>Join " + other.getShortNameRaw() + idString(game, other.getId());
                        item += " (" + other.getC3NetId() + ")";
                        item += (nodes == 0 ? " - full" : " - " + nodes + " free spots");
                        menu.add(menuItem(item, LMP_C3JOIN + "|" + other.getId() + enToken(entities), nodes != 0, listener));
                        usedNetIds.add(other.getC3NetId());
                    }

                } else if (other.isC3CompanyCommander() && other.hasC3MM()) {
                    String item = "<HTML>Connect to " + other.getShortNameRaw() + idString(game, other.getId());
                    item += " (" + other.getC3NetId() + ")";
                    item += (nodes == 0 ? " - full" : " - " + nodes + " free spots");
                    menu.add(menuItem(item, LMP_C3CONNECT + "|" + other.getId() + enToken(entities), nodes != 0, listener));

                } else if (other.isC3CompanyCommander() == entity.hasC3M() 
                        && !entity.isC3CompanyCommander()) {
                    String item = "<HTML>Connect to " + other.getShortNameRaw() + idString(game, other.getId());
                    item += " (" + other.getC3NetId() + ")";
                    if (entity.C3MasterIs(other)) {
                        item += " - already connected";
                        nodes = 0;
                    } else {
                        item += (nodes == 0 ? " - full" : " - " + nodes + " free spots");
                    }
                    menu.add(menuItem(item, LMP_C3CONNECT + "|" + other.getId() + enToken(entities), nodes != 0, listener));
                }
            }
        }
        menu.setEnabled(enabled && menu.getItemCount() > 0);
        return menu;
    }

    /**
     * Returns the "Change Unit Owner" submenu.
     */
    private static JMenu changeOwnerMenu(boolean enabled, ClientGUI clientGui,
                                         ActionListener listener, Collection<Entity> entities,
                                         Collection<Force> forces) {
        JMenu menu = new JMenu(Messages.getString("ChatLounge.ChangeOwner"));
        menu.setEnabled(enabled);
        menu.setMnemonic(KeyEvent.VK_O);

        Game game = clientGui.getClient().getGame();
        Forces gameForces = game.getForces();

        if (!entities.isEmpty()) {
            for (Player player: game.getPlayersVector()) {
                String command = LMP_ASSIGN + "|" + player.getId() + ":" + foToken(forces) + enToken(entities);
                menu.add(menuItem(player.getName(), command, enabled, listener));
            }
        }

        if (entities.isEmpty() && !forces.isEmpty()) {
            JMenu assignMenu = new JMenu("Assign to");
            JMenu fOnlyMenu = new JMenu("Force only");
            JMenu fFullMenu = new JMenu("Everything in the Force");
            assignMenu.add(fOnlyMenu);
            assignMenu.add(fFullMenu);
            Force force = anyOneElement(forces);
            for (Player player: game.getPlayersVector()) {
                if (!player.isEnemyOf(gameForces.getOwner(force))) {
                    String command = LMP_FASSIGNONLY + "|" + player.getId() + ":" + foToken(forces) + NOINFO;
                    fOnlyMenu.add(menuItem(player.getName(), command, true, listener));
                }
                String command = LMP_FASSIGN + "|" + player.getId() + ":" + foToken(forces) + NOINFO;
                fFullMenu.add(menuItem(player.getName(), command, true, listener));
            }
            assignMenu.setEnabled(enabled && assignMenu.getItemCount() > 0);
            menu.add(assignMenu);
        }

        return menu;
    }

    /**
     * @return the "Quirks" submenu, allowing to save the quirks to the quirks config file.
     */
    private static JMenu quirksMenu(boolean enabled, ActionListener listener, String eIds) {
        JMenu menu = new JMenu(Messages.getString("ChatLounge.popup.quirks"));
        menu.setEnabled(enabled);
        menu.add(menuItem("Save Quirks for Chassis", LMP_SAVE_QUIRKS_ALL + NOINFO + eIds, enabled, listener));
        menu.add(menuItem("Save Quirks for Chassis/Model", LMP_SAVE_QUIRKS_MODEL + NOINFO + eIds, enabled, listener));
        return menu;
    }

    /**
     * @return the "Equipment" submenu, allowing hotloading LRMs and setting MGs to rapid fire mode
     */
    private static JMenu equipMenu(boolean anyRFOn, boolean anyRFOff, boolean anyHLOn,
                                   boolean anyHLOff, boolean optHL, boolean optRF,
                                   ActionListener listener, String eIds) {
        JMenu menu = new JMenu(Messages.getString("ChatLounge.Equipment"));
        menu.setEnabled(anyRFOff || anyRFOn || anyHLOff || anyHLOn);        
        if (optRF) {
            menu.add(menuItem(Messages.getString("ChatLounge.RapidFireToggleOn"), LMP_RAPIDFIREMG_ON + NOINFO + eIds, 
                    anyRFOff, listener));
            menu.add(menuItem(Messages.getString("ChatLounge.RapidFireToggleOff"), LMP_RAPIDFIREMG_OFF + NOINFO + eIds, 
                   anyRFOn, listener));
        }
        if (optHL) {
            menu.add(menuItem(Messages.getString("ChatLounge.HotLoadToggleOn"), LMP_HOTLOAD_ON + NOINFO + eIds, 
                    anyHLOff, listener));
            menu.add(menuItem(Messages.getString("ChatLounge.HotLoadToggleOff"), LMP_HOTLOAD_OFF + NOINFO + eIds, 
                    anyHLOn, listener));
        }
        return menu;
    }

    /**
     * Returns the "Offload from" submenu, allowing to offload
     * units from a specific bay of the given entity
     */
    private static JMenu offloadBayMenu(boolean enabled, Collection<Entity> entities, ActionListener listener) {
        JMenu menu = new JMenu("Offload All From...");
        if (enabled && entities.size() == 1) {
            Entity entity = theElement(entities);
            for (Bay bay : entity.getTransportBays()) {
                if (!bay.getLoadedUnits().isEmpty()) {
                    String label = "Bay #" + bay.getBayNumber() + " (" + bay.getLoadedUnits().size() + " units)";
                    menu.add(menuItem(label, LMP_UNLOADALLFROMBAY + "|" + bay.getBayNumber() + enToken(entities), enabled, listener));
                }
            }
        }
        menu.setEnabled(enabled && menu.getItemCount() > 0);
        MenuScroller.createScrollBarsOnMenus(menu);
        return menu;
    }

    /**
     * Returns the "Swap Pilot" submenu, allowing to swap the unit
     * pilot with a pilot of an equivalent unit. Does work with multiple 
     * selected units but expects the Lobby to issue an error message as
     * only one unit can swap pilot with one other unit.
     */
    private static JMenu swapPilotMenu(boolean enabled, Collection<Entity> entities, ClientGUI clientGui, ActionListener listener) {
        Game game = clientGui.getClient().getGame();

        JMenu menu = new JMenu("Swap pilots with");
        if (!entities.isEmpty()) {
            // use a random selected unit to determine the targets
            Entity entity = anyOneElement(entities);
            for (Entity swapper: game.getEntitiesVector()) {
                // only swap your own pilots and with the same unit and crew type
                if (swapper.getOwnerId() == entity.getOwnerId() 
                        && swapper.getId() != entity.getId()
                        && swapper.getUnitType() == entity.getUnitType()
                        && swapper.getCrew().getCrewType() == entity.getCrew().getCrewType()) {
                    
                    String item = "<HTML>" + swapper.getShortNameRaw() + idString(game, swapper.getId());
                    String command = LMP_SWAP + "|" + swapper.getId() + enToken(entities);
                    menu.add(menuItem(item, command, enabled, listener));
                }
            }
        }
        menu.setEnabled(enabled && menu.getItemCount() > 0);
        MenuScroller.createScrollBarsOnMenus(menu);
        return menu;
    }

    /** 
     * Returns a command string token containing the IDs of the given entities and a leading |
     * E.g. |2,14,44,22
     */
    private static String enToken(Collection<Entity> entities) {
        if (entities.isEmpty()) {
            return "|-1";
        }
        List<String> ids = entities.stream().map(Entity::getId).map(Object::toString).collect(Collectors.toList());
        return "|" + String.join(",", ids);
    }

    /** 
     * Returns a command string token containing the IDs of the given forces
     * E.g. 2,14,44,22
     */
    private static String foToken(Collection<Force> forces) {
        if (forces.isEmpty()) {
            return "-1";
        }
        List<String> ids = forces.stream().map(Force::getId).map(Object::toString).collect(Collectors.toList());
        return String.join(",", ids);
    }

    /**
     * @return true when the entity has an MG set to rapid fire.
     */
    private static boolean hasRapidFireMG(Entity entity) {
        for (Mounted m: entity.getWeaponList()) {
            EquipmentType etype = m.getType();
            if (etype.hasFlag(WeaponType.F_MG) && m.isRapidfire()) {
                return true;
            }
        }
        return false;
    }

    /** Returns true when the entity has an MG set to normal (non-rapid) fire. */ 
    private static boolean hasNormalFireMG(Entity entity) {
        for (Mounted m: entity.getWeaponList()) {
            EquipmentType etype = m.getType();
            if (etype.hasFlag(WeaponType.F_MG) && !m.isRapidfire()) {
                return true;
            }
        }
        return false;
    }

    /** Returns true when the entity has a weapon with ammo set to hot-loaded. */ 
    private static boolean hasHotLoaded(Entity entity) {
        for (Mounted ammo: entity.getAmmo()) {
            AmmoType etype = (AmmoType) ammo.getType();
            if (etype.hasFlag(AmmoType.F_HOTLOAD) && ammo.isHotLoaded()) {
                return true;
            }
        }
        return false;
    }

    /** Returns true when the entity has a weapon with ammo set to non-hot-loaded. */ 
    private static boolean hasNonHotLoaded(Entity entity) {
        for (Mounted ammo: entity.getAmmo()) {
            AmmoType etype = (AmmoType) ammo.getType();
            if (etype.hasFlag(AmmoType.F_HOTLOAD) && !ammo.isHotLoaded()) {
                return true;
            }
        }
        return false;
    }
}

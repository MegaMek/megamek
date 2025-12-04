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

import static megamek.client.ui.util.UIUtil.menuItem;
import static megamek.common.util.CollectionUtil.anyOneElement;
import static megamek.common.util.CollectionUtil.theElement;

import java.awt.FileDialog;
import java.awt.Image;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.dialogs.iconChooser.CamoChooserDialog;
import megamek.client.ui.tileset.EntityImage;
import megamek.client.ui.tileset.MMStaticDirectoryManager;
import megamek.client.ui.util.MenuScroller;
import megamek.client.ui.util.ScalingPopup;
import megamek.client.ui.util.UIUtil;
import megamek.common.Player;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.battleArmor.ProtoMekClampMount;
import megamek.common.bays.Bay;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.Transporter;
import megamek.common.equipment.WeaponType;
import megamek.common.force.Force;
import megamek.common.force.Forces;
import megamek.common.game.Game;
import megamek.common.icons.Camouflage;
import megamek.common.interfaces.ForceAssignable;
import megamek.common.options.OptionsConstants;
import megamek.common.preference.PreferenceManager;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.FighterSquadron;
import megamek.common.units.Jumpship;
import megamek.common.units.ProtoMek;
import megamek.common.util.C3Util;
import megamek.logging.MMLogger;

/**
 * Creates the Lobby Mek right-click pop-up menu for both the sortable table and the force tree.
 */
class LobbyMekPopup {
    private static final MMLogger logger = MMLogger.create(LobbyMekPopup.class);

    static final String LMP_SKILLS = "SKILLS";
    static final String LMP_CALLSIGN = "CALLSIGN";
    static final String LMP_NAME = "NAME";
    static final String LMP_C3DISCONNECT = "C3DISCONNECT";
    static final String LMP_DEPLOY = "DEPLOY";
    static final String LMP_HEAT = "HEAT";
    static final String LMP_HULL_DOWN = "HULLDOWN";
    static final String LMP_PRONE = "PRONE";
    static final String LMP_STAND = "STAND";
    static final String LMP_NO_HIDE = "NOHIDE";
    static final String LMP_HIDE = "HIDE";
    static final String LMP_HIDDEN = "HIDDEN";
    static final String LMP_F_ASSIGN_ONLY = "FASSIGNONLY";
    static final String LMP_F_ASSIGN = "FASSIGN";
    static final String LMP_RAPID_FIRE_MG_OFF = "RAPIDFIREMG_OFF";
    static final String LMP_RAPID_FIRE_MG_ON = "RAPIDFIREMG_ON";
    static final String LMP_HOT_LOAD_OFF = "HOTLOAD_OFF";
    static final String LMP_HOT_LOAD_ON = "HOTLOAD_ON";
    static final String LMP_VRT_LONG = "VRT_LONG";
    static final String LMP_VRT_SHORT = "VRT_SHORT";
    static final String LMP_SWAP = "SWAP";
    static final String LMP_ASSIGN = "ASSIGN";
    static final String LMP_C3CONNECT = "C3CONNECT";
    static final String LMP_C3JOIN = "C3JOIN";
    static final String LMP_C3_FORM_NHC3 = "C3FORMNHC3";
    static final String LMP_C3_FORM_C3 = "C3FORMC3";
    static final String LMP_C3LM = "C3LM";
    static final String LMP_C3CM = "C3CM";
    static final String LMP_SQUADRON = "SQUADRON";
    static final String LMP_LOAD = "LOAD";
    static final String LMP_TOW = "TOW";
    static final String LMP_F_ADD_TO = "FADDTO";
    static final String LMP_F_REMOVE = "FREMOVE";
    static final String LMP_F_PROMOTE = "FPROMOTE";
    static final String LMP_F_RENAME = "FRENAME";
    static final String LMP_F_CREATE_SUB = "FCREATESUB";
    static final String LMP_F_CREATE_TOP = "FCREATETOP";
    static final String LMP_F_CREATE_FROM = "FCREATEFROM";
    static final String LMP_FC_DELETE_EMPTY = "FCDELETEEMPTY";
    static final String LMP_SBF_FORMATION = "SBFFORMATION";
    static final String LMP_DELETE = "DELETE";
    static final String LMP_UNLOAD_ALL = "UNLOADALL";
    static final String LMP_UNLOAD = "UNLOAD";
    static final String LMP_DETACH_FROM_TRACTOR = "DETACHFROMTRACTOR";
    static final String LMP_DETACH_TRAILER = "DETACHTRAILER";
    static final String LMP_MOVE_DOWN = "MOVE_DOWN";
    static final String LMP_INDI_CAMO = "INDI_CAMO";
    static final String LMP_DAMAGE = "DAMAGE";
    static final String LMP_CONFIGURE_ALL = "CONFIGURE_ALL";
    static final String LMP_CONFIGURE = "CONFIGURE";
    static final String LMP_BV = "BV";
    static final String LMP_COST = "COST";
    static final String LMP_VIEW = "VIEW";
    static final String LMP_MOVE_UP = "MOVE_UP";
    static final String LMP_PRIORITY_TARGET = "PRIO_TARGET";
    static final String LMP_ALPHA_STRIKE = "ALPHASTRIKE";
    static final String LMP_AUTOCONFIG = "AUTOCONFIG";
    static final String LMP_RANDOM_CONFIG = "RANDOMCONFIG";
    static final String LMP_SAVE_CONFIG = "SAVECONFIG";
    static final String LMP_APPLY_CONFIG = "APPLYCONFIG";

    private static final String NO_INFO = "|-1";

    static final String LMP_UNLOAD_ALL_FROM_BAY = "UNLOADALLFROMBAY";

    static ScalingPopup getPopup(List<Entity> entities, List<Force> forces, ActionListener listener,
          ChatLounge lobby) {
        ClientGUI clientGui = lobby.getClientGUI();
        Game game = lobby.game();
        var opts = game.getOptions();

        boolean optBurstMG = opts.booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_BURST);
        boolean optLRMHotLoad = opts.booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_HOT_LOAD);
        boolean optCapFighters = opts.booleanOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_CAPITAL_FIGHTER);

        // A set of all selected entities and all entities in selected forces and their
        // subForces
        Set<Entity> joinedEntities = new HashSet<>(entities);
        for (Force force : forces) {
            joinedEntities.addAll(ForceAssignable.filterToEntityList(game.getForces().getFullEntities(force)));
        }

        // Find certain unit features among all units the player can access
        // Used to hide some menu items entirely like "Form Squadron" when there's no
        // fighter in the game
        HashSet<Entity> accessibleEntities = new HashSet<>(game.getEntitiesVector());
        accessibleEntities.removeIf(lobby.lobbyActions::isNotEditable);
        boolean accessibleFighters = accessibleEntities.stream().anyMatch(Entity::isFighter);
        boolean accessibleTransportBays = accessibleEntities.stream().anyMatch(e -> !e.getTransportBays().isEmpty());
        boolean accessibleCarriers = accessibleEntities.stream().anyMatch(e -> !e.getLoadedUnits().isEmpty());
        boolean accessibleTractors = accessibleEntities.stream().anyMatch(e -> e.getTowing() != Entity.NONE);
        boolean accessibleTrailers = accessibleEntities.stream().anyMatch(e -> e.getTowedBy() != Entity.NONE);

        // Find what can be done with the selected entities incl. those in selected
        // forces
        boolean anyCarrier = joinedEntities.stream().anyMatch(e -> !e.getLoadedUnits().isEmpty());
        boolean noneEmbarked = joinedEntities.stream().allMatch(e -> e.getTransportId() == Entity.NONE);
        boolean anyTractor = joinedEntities.stream().anyMatch(e -> e.getTowing() != Entity.NONE);
        boolean anyTrailer = joinedEntities.stream().anyMatch(e -> e.getTowedBy() != Entity.NONE);

        boolean allProtoMeks = !joinedEntities.isEmpty()
              && joinedEntities.stream().allMatch(e -> e instanceof ProtoMek);
        boolean anyRFMGOn = joinedEntities.stream().anyMatch(LobbyMekPopup::hasRapidFireMG);
        boolean anyRFMGOff = joinedEntities.stream().anyMatch(LobbyMekPopup::hasNormalFireMG);
        boolean anyHLOn = joinedEntities.stream().anyMatch(LobbyMekPopup::hasHotLoaded);
        boolean anyHLOff = joinedEntities.stream().anyMatch(LobbyMekPopup::hasNonHotLoaded);
        boolean anyVRTLong = joinedEntities.stream().anyMatch(LobbyMekPopup::hasVRTLong);
        boolean anyVRTShort = joinedEntities.stream().anyMatch(LobbyMekPopup::hasVRTShort);
        boolean anyVRT = joinedEntities.stream().anyMatch(LobbyMekPopup::hasVRT);

        boolean oneSelected = entities.size() == 1;
        boolean hasJoinedEntities = !joinedEntities.isEmpty();
        boolean joinedOneEntitySelected = oneSelected && forces.isEmpty();

        ScalingPopup popup = new ScalingPopup();

        // All command strings should follow the layout COMMAND|INFO|ID1,ID2,I3...
        // and use -1 when something is not needed (COMMAND|-1|-1)
        String eId = "|" + (entities.isEmpty() ? "-1" : entities.get(0).getId());
        String eIds = enToken(entities);
        String seIds = enToken(joinedEntities);

        popup.add(menuItem("View...", LMP_VIEW + NO_INFO + seIds, hasJoinedEntities, listener, KeyEvent.VK_V));
        popup.add(menuItem("View BV Calculation...", LMP_BV + NO_INFO + seIds, hasJoinedEntities, listener,
              KeyEvent.VK_B));
        popup.add(menuItem("View Cost Calculation...", LMP_COST + NO_INFO + seIds, hasJoinedEntities, listener));
        popup.add(ScalingPopup.spacer());

        if (joinedOneEntitySelected) {
            popup.add(
                  menuItem("Configure...", LMP_CONFIGURE + NO_INFO + eId, hasJoinedEntities, listener, KeyEvent.VK_C));
        } else {
            popup.add(menuItem("Configure...", LMP_CONFIGURE_ALL + NO_INFO + seIds, hasJoinedEntities, listener,
                  KeyEvent.VK_C));
        }
        popup.add(menuItem("Edit Damage...", LMP_DAMAGE + NO_INFO + seIds, hasJoinedEntities, listener, KeyEvent.VK_E));
        popup.add(menuItem("Set individual camo...", LMP_INDI_CAMO + NO_INFO + seIds, hasJoinedEntities, listener,
              KeyEvent.VK_I));

        if (lobby.isForceView()) {
            JMenuItem moveUp = menuItem("Move Up", LMP_MOVE_UP + "|" + foToken(forces) + eIds,
                  !forces.isEmpty() || !entities.isEmpty(), listener, KeyEvent.VK_I);
            moveUp.setAccelerator(KeyStroke.getKeyStroke("ctrl UP"));
            popup.add(moveUp);
            JMenuItem moveDn = menuItem("Move Down", LMP_MOVE_DOWN + "|" + foToken(forces) + eIds,
                  !forces.isEmpty() || !entities.isEmpty(), listener, KeyEvent.VK_I);
            moveDn.setAccelerator(KeyStroke.getKeyStroke("ctrl DOWN"));
            popup.add(moveDn);
        }

        popup.add(deployMenu(clientGui, hasJoinedEntities, listener, joinedEntities));
        popup.add(randomizeMenu(hasJoinedEntities, listener, seIds));
        popup.add(munitionsConfigMenu(hasJoinedEntities, listener, joinedEntities));
        popup.add(swapPilotMenu(hasJoinedEntities, joinedEntities, clientGui, listener));
        popup.add(priorityTargetMenu(clientGui, hasJoinedEntities, listener, joinedEntities));

        if (optBurstMG || optLRMHotLoad || anyVRT) {
            popup.add(equipMenu(anyRFMGOn, anyRFMGOff, anyHLOn, anyHLOff, anyVRTLong, anyVRTShort, anyVRT,
                  optLRMHotLoad, optBurstMG, listener, seIds));
        }

        popup.add(ScalingPopup.spacer());
        popup.add(changeOwnerMenu(!entities.isEmpty() || !forces.isEmpty(), clientGui, listener, entities, forces));
        popup.add(loadMenu(clientGui, true, listener, joinedEntities));
        if (entities.size() == 1) {
            popup.add(towMenu(clientGui, true, listener, entities.get(0)));
        }

        if (accessibleCarriers) {
            popup.add(
                  menuItem("Disembark / leave from carriers", LMP_UNLOAD + NO_INFO + seIds, !noneEmbarked, listener));
            popup.add(menuItem("Offload all carried units", LMP_UNLOAD_ALL + NO_INFO + seIds, anyCarrier, listener));
        }

        if (accessibleTrailers) {
            popup.add(menuItem("Detach from Tractor", LMP_DETACH_FROM_TRACTOR + NO_INFO + seIds, anyTrailer, listener));
        }

        if (accessibleTractors) {
            popup.add(menuItem("Detach Trailer", LMP_DETACH_TRAILER + NO_INFO + seIds, anyTractor, listener));
        }

        if (accessibleTransportBays) {
            popup.add(offloadBayMenu(anyCarrier, joinedEntities, listener));
        }

        if (accessibleFighters && optCapFighters) {
            popup.add(squadronMenu(clientGui, true, listener, joinedEntities));
        }

        if (allProtoMeks) {
            popup.add(protoMenu(clientGui, true, listener, joinedEntities));
        }

        popup.add(c3Menu(hasJoinedEntities, joinedEntities, clientGui, listener));
        popup.add(forceMenu(lobby, entities, forces, listener));

        popup.add(ScalingPopup.spacer());
        popup.add(menuItem("View AlphaStrike Stats", LMP_ALPHA_STRIKE + NO_INFO + seIds, true, listener));

        if (oneSelected) {
            popup.add(exportEntitySpriteMenu(clientGui.getFrame(), entities.get(0)));
        }

        popup.add(menuItem("Convert to SBF Formation", LMP_SBF_FORMATION + "|" + foToken(forces) + eIds,
              lobby.isForceView(), listener));
        popup.add(ScalingPopup.spacer());
        popup.add(menuItem("Delete", LMP_DELETE + "|" + foToken(forces) + seIds,
              !entities.isEmpty() && forces.isEmpty(), listener, KeyEvent.VK_D));

        return popup;
    }

    /**
     * Returns the "Force" submenu, allowing assignment to forces
     */
    private static JMenu forceMenu(ChatLounge lobby, List<Entity> entities, List<Force> forces,
          ActionListener listener) {
        JMenu menu = new JMenu("Force");
        if (!forces.isEmpty() || !entities.isEmpty()) {
            menu.add(menuItem("Create Force from...",
                  LMP_F_CREATE_FROM + "|" + foToken(forces) + enToken(entities),
                  true,
                  listener));
        }
        menu.add(menuItem("Add empty Force...", LMP_F_CREATE_TOP + NO_INFO + NO_INFO, true, listener));

        // If exactly one force is selected, offer force options
        if ((forces.size() == 1) && entities.isEmpty()) {
            Force force = forces.get(0);
            boolean editable = lobby.lobbyActions.isEditable(force);
            String fId = "|" + force.getId();
            menu.add(menuItem("Add Sub force...", LMP_F_CREATE_SUB + fId + NO_INFO, editable, listener));
            menu.add(menuItem("Rename", LMP_F_RENAME + fId + NO_INFO, editable, listener));
            menu.add(menuItem("Promote to Top-Level Force", LMP_F_PROMOTE + fId + NO_INFO,
                  editable && !force.isTopLevel(), listener));
        }

        // If entities are selected but no forces, offer entity options
        if (forces.isEmpty() && !entities.isEmpty() && LobbyUtility.haveSingleOwner(entities)) {
            // Add to force menu tree
            JMenu addMenu = new JMenu("Add to...");
            for (Force force : lobby.game().getForces().getTopLevelForces()) {
                addMenu.add(forceTreeMenu(force, lobby.game(), enToken(entities), listener));
            }
            menu.add(addMenu);
            menu.add(menuItem("Remove from Force", LMP_F_REMOVE + NO_INFO + enToken(entities), true, listener));
        }

        // If exactly one force is selected, offer force options
        if ((forces.size() == 1) && entities.isEmpty()) {
            Force force = forces.get(0);
            boolean editable = lobby.lobbyActions.isEditable(force);
            menu.add(menuItem("Delete empty Force...", LMP_FC_DELETE_EMPTY + "|" + foToken(forces) + NO_INFO,
                  (editable && force.getChildCount() == 0), listener));
        }

        menu.setEnabled(menu.getItemCount() > 0);
        return menu;
    }

    private static JMenuItem forceTreeMenu(Force force, Game game, String enToken, ActionListener listener) {
        JMenuItem result;
        String item = "<HTML>" + force.getName() + idString(game, force.getId());
        if (force.getSubForces().isEmpty()) {
            result = menuItem(item, LMP_F_ADD_TO + "|" + force.getId() + enToken, true, listener);
        } else {
            result = new JMenu(item);
            for (Integer subForceId : force.getSubForces()) {
                result.add(forceTreeMenu(game.getForces().getForce(subForceId), game, enToken, listener));
            }
        }
        return result;
    }

    static String idString(Game game, int id) {
        if (PreferenceManager.getClientPreferences().getShowUnitId()) {
            return " <FONT" + UIUtil.colorString(UIUtil.uiGray()) + ">[" + id + "]</FONT>";
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
                            "<HTML>" + e.getShortNameRaw() + idString(game, e.getId()) + " (Free Collars: "
                                  + ((Jumpship) e).getFreeDockingCollars() + ")",
                            LMP_LOAD + "|" + e.getId() + ":-1" + enToken(entities), true, listener)));
            } else if (entities.size() == 1) {
                Entity transportedUnit = entities.iterator().next();
                // Standard loading, not ProtoMeks, not DropShip -> JumpShip
                game.getEntitiesVector().stream()
                      .filter(e -> !e.isCapitalFighter(true))
                      .filter(e -> !entities.contains(e))
                      .filter(e -> canLoadAll(e, entities))
                      .forEach(e -> {
                          JMenu loaderMenu = new JMenu("<HTML>" + e.getShortNameRaw() + idString(game, e.getId()));
                          e.getTransports().forEach(t -> {
                              if (t.canLoad(transportedUnit)) {
                                  // FIXME #7640: Update once we can properly specify any transporter an entity has, and properly load into that transporter.
                                  loaderMenu.add(menuItem(
                                        "Onto " + t.toString(),
                                        LMP_LOAD + "|" + e.getId() + ":" + (Integer.MAX_VALUE
                                              - e.getTransports().indexOf(t)) + enToken(entities),
                                        true, listener));
                              }
                          });
                          if (loaderMenu.getItemCount() > 0) {
                              menu.add(loaderMenu);
                          }
                      });
            } else if (entities.stream().noneMatch(e -> e.hasETypeFlag(Entity.ETYPE_PROTOMEK))) {
                // Standard loading, not ProtoMeks, not DropShip -> JumpShip
                game.getEntitiesVector().stream()
                      .filter(e -> !e.isCapitalFighter(true))
                      .filter(e -> !entities.contains(e))
                      .filter(e -> canLoadAll(e, entities))
                      .forEach(e -> menu.add(menuItem(
                            "<HTML>" + e.getShortNameRaw() + idString(game, e.getId()),
                            LMP_LOAD + "|" + e.getId() + ":-1" + enToken(entities), true, listener)));
            }
        }
        menu.setEnabled(enabled && (menu.getItemCount() > 0));
        return menu;
    }

    /**
     * Returns the "Tow" submenu, allowing towing
     */
    private static JMenu towMenu(ClientGUI cg, boolean enabled, ActionListener listener,
          Entity entity) {
        Game game = cg.getClient().getGame();
        JMenu menu = new JMenu("Towed by");
        menu.setVisible(false);
        if (enabled && entity.isTrailer()) {
            menu.setVisible(true);
            game.getEntitiesVector().stream()
                  .filter(tractor -> tractor.getTowing() == Entity.NONE)
                  .filter(tractor -> tractor.canTow(entity.getId()))
                  .filter(tractor -> !tractor.equals(entity))
                  .forEach(tractor -> menu.add(menuItem("<HTML>" + tractor.getShortNameRaw() + idString(game,
                              tractor.getId()),
                        LMP_TOW + "|" + tractor.getId() + ":-1|" + entity.getId(), true, listener

                  )));
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
        if (!(enabled || entities.stream().allMatch(e -> e.hasETypeFlag(Entity.ETYPE_PROTOMEK)))) {
            return menu;
        }

        Game game = cg.getClient().getGame();

        Optional<Entity> optionalEntity = entities.stream()
              .filter(e -> e.hasETypeFlag(Entity.ETYPE_PROTOMEK))
              .findAny();
        Entity entity = optionalEntity.orElse(null);
        List<Entity> loaders = game.getEntitiesVector();

        // Handle front and rear Magnetic Clamp Mounts
        for (Entity loader : loaders) {
            if (!loader.hasETypeFlag(Entity.ETYPE_MEK)) {
                continue;
            }
            Transporter front = null;
            Transporter rear = null;
            for (Transporter t : loader.getTransports()) {
                if (t instanceof ProtoMekClampMount) {
                    if (((ProtoMekClampMount) t).isRear()) {
                        rear = t;
                    } else {
                        front = t;
                    }
                }
            }
            JMenu loaderMenu = new JMenu(loader.getShortName());
            if ((entity != null) && (front != null) && front.canLoad(entity)
                  && ((entity.getWeightClass() < EntityWeightClass.WEIGHT_SUPER_HEAVY)
                  || (rear == null) || rear.getLoadedUnits().isEmpty())) {
                loaderMenu.add(menuItem("Onto Front", LMP_LOAD + "|" + loader.getId() + ":0" + enToken(entities),
                      enabled, listener));
            }
            boolean frontUltra = (front != null)
                  && front.getLoadedUnits().stream()
                  .anyMatch(l -> l.getWeightClass() == EntityWeightClass.WEIGHT_SUPER_HEAVY);
            if ((rear != null) && rear.canLoad(entity) && !frontUltra) {
                loaderMenu.add(menuItem("Onto Rear", LMP_LOAD + "|" + loader.getId() + ":1" + enToken(entities),
                      enabled, listener));
            }
            if (loaderMenu.getItemCount() > 0) {
                menu.add(loaderMenu);
            }
        }

        // Handle all other valid loaders, such as Dropships
        loaders.stream()
              .filter(e -> !e.isCapitalFighter(true))
              .filter(e -> !entities.contains(e))
              .filter(e -> canLoadAll(e, entities))
              .forEach(e -> menu.add(menuItem(
                    "<HTML>" + e.getShortNameRaw() + idString(game, e.getId()),
                    LMP_LOAD + "|" + e.getId() + ":-1" + enToken(entities), enabled, listener)));

        menu.setEnabled(enabled && (menu.getItemCount() > 0));
        return menu;
    }

    /**
     * Returns the "Fighter Squadron" submenu, allowing to assign units to or create a fighter squadron
     */
    private static JMenu squadronMenu(ClientGUI cg, boolean enabled, ActionListener listener,
          Collection<Entity> entities) {
        JMenu menu = new JMenu("Fighter Squadrons");
        boolean hasFighter = entities.stream().anyMatch(Entity::isFighter);
        if (enabled && hasFighter) {
            menu.add(menuItem("Form Fighter Squadron", LMP_SQUADRON + NO_INFO + enToken(entities), true, listener));

            // Join [Squadron] menu items
            cg.getClient().getGame().getEntitiesVector().stream()
                  .filter(e -> e instanceof FighterSquadron)
                  .filter(e -> !entities.contains(e))
                  .filter(e -> canLoadAll(e, entities))
                  .forEach(e -> menu.add(menuItem("Join " + e.getShortName(),
                        LMP_LOAD + "|" + e.getId() + ":-1" + enToken(entities), true, listener)));
        }
        menu.setEnabled(enabled && (menu.getItemCount() > 0));
        return menu;
    }

    /**
     * Returns the "Priority Target" submenu, allowing to assign units as a strategic target to a local Princess bot.
     */
    private static JMenu priorityTargetMenu(ClientGUI cg, boolean enabled, ActionListener listener,
          Collection<Entity> entities) {
        JMenu menu = new JMenu("Set Priority Target for");
        if (enabled && !cg.getLocalBots().isEmpty()) {
            for (String bot : cg.getLocalBots().keySet()) {
                menu.add(menuItem(bot, LMP_PRIORITY_TARGET + "|" + bot + enToken(entities), true, listener));
            }
        }
        menu.setEnabled(enabled && (menu.getItemCount() > 0));
        return menu;
    }

    /**
     * Returns true when the loader can load all the given entities (under lobby conditions).
     */
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
                menu.add(menuItem("Not Hidden", LMP_HIDDEN + "|" + LMP_NO_HIDE + eIds, anyHidden, listener));
                menu.add(ScalingPopup.spacer());
            }

            menu.add(menuItem("Standing", LMP_STAND + "|" + LMP_STAND + eIds, true, listener));
            menu.add(menuItem("Prone", LMP_STAND + "|" + LMP_PRONE + eIds, true, listener));
            if (clientGui.getClient().getGame().getOptions()
                  .booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_HULL_DOWN)) {
                menu.add(menuItem("Hull-Down", LMP_STAND + "|" + LMP_HULL_DOWN + eIds, true, listener));
            }
            menu.add(ScalingPopup.spacer());

            // Heat
            JMenu heatMenu = new JMenu("Heat at start");
            heatMenu.add(menuItem("No heat", LMP_HEAT + "|0" + eIds, true, listener));
            for (int i = 1; i < 11; i++) {
                heatMenu.add(menuItem("Heat " + i, LMP_HEAT + "|" + i + eIds, true, listener));
            }
            JMenu subHeatMenu = new JMenu("More heat");
            for (int i = 11; i < 41; i++) {
                subHeatMenu.add(menuItem("Heat " + i, LMP_HEAT + "|" + i + eIds, true, listener));
            }
            heatMenu.add(subHeatMenu);
            menu.add(heatMenu);
            menu.add(ScalingPopup.spacer());

            // Late deployment
            JMenu lateMenu = new JMenu("Deployment round");
            lateMenu.add(menuItem("At game start", LMP_DEPLOY + "|0" + eIds, true, listener));
            for (int i = 1; i < 11; i++) {
                lateMenu.add(menuItem("Before round " + i, LMP_DEPLOY + "|" + i + eIds, true, listener));
            }
            JMenu veryLateMenu = new JMenu("Later");
            for (int i = 11; i < 41; i++) {
                veryLateMenu.add(menuItem("Before round " + i, LMP_DEPLOY + "|" + i + eIds, true, listener));
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

        menu.add(menuItem("Name", LMP_NAME + NO_INFO + eIds, enabled, listener, KeyEvent.VK_N));
        menu.add(menuItem("Callsign", LMP_CALLSIGN + NO_INFO + eIds, enabled, listener, KeyEvent.VK_C));
        menu.add(menuItem("Skills", LMP_SKILLS + NO_INFO + eIds, enabled, listener, KeyEvent.VK_S));
        return menu;
    }

    /**
     * Returns the "Configure Munitions" submenu.
     */
    private static JMenu munitionsConfigMenu(boolean enabled, ActionListener listener, Collection<Entity> entityCol) {
        JMenu menu = new JMenu("Configure Munitions");
        Entity first = (entityCol.isEmpty()) ? null : entityCol.iterator().next();
        enabled &= (first != null && entityCol.stream().noneMatch(e -> e.isEnemyOf(first)));
        menu.setEnabled(enabled);
        String eIds = enToken(entityCol);

        menu.add(menuItem("Autoconfig", LMP_AUTOCONFIG + NO_INFO + eIds, enabled, listener, KeyEvent.VK_A));
        menu.add(menuItem("Randomize", LMP_RANDOM_CONFIG + NO_INFO + eIds, enabled, listener, KeyEvent.VK_R));
        menu.add(menuItem("Save config", LMP_SAVE_CONFIG + NO_INFO + eIds, enabled, listener, KeyEvent.VK_S));
        menu.add(menuItem("Apply config", LMP_APPLY_CONFIG + NO_INFO + eIds, enabled, listener, KeyEvent.VK_P));

        return menu;
    }

    /** Returns the C3 computer submenu. */
    private static JMenu c3Menu(boolean enabled, Collection<Entity> entities, ClientGUI cg,
          ActionListener listener) {
        JMenu menu = new JMenu("C3");

        if (entities.stream().anyMatch(Entity::hasAnyC3System)) {

            menu.add(menuItem("Disconnect", LMP_C3DISCONNECT + NO_INFO + enToken(entities), enabled, listener));

            if (entities.stream().anyMatch(e -> e.hasC3MM() || e.hasC3M())) {
                boolean allCM = entities.stream().allMatch(Entity::isC3CompanyCommander);
                menu.add(menuItem("Set as C3 Company Master",
                      LMP_C3CM + NO_INFO + enToken(entities),
                      !allCM,
                      listener));
                boolean allLM = entities.stream().allMatch(Entity::isC3IndependentMaster);
                menu.add(menuItem("Set as C3 Lance Master", LMP_C3LM + NO_INFO + enToken(entities), !allLM, listener));
            }

            // Special treatment if exactly a C3SSSM is selected
            if (entities.size() == 4) {
                long countM = entities.stream().filter(Entity::hasC3M).count();
                long countS = entities.stream().filter(Entity::hasC3S).count();
                if (countM == 1 && countS == 3) {
                    Optional<Entity> optionalMaster = entities.stream().filter(Entity::hasC3M).findAny();
                    optionalMaster.ifPresent(master -> menu.add(menuItem("Form C3 Lance",
                          LMP_C3_FORM_C3 + "|" + master.getId() + enToken(entities),
                          true,
                          listener)));
                }
            }

            // Special treatment if a group of NhC3 is selected
            if (entities.size() > 1 && entities.size() <= 6) {
                Entity master = anyOneElement(entities);
                if (entities.stream().allMatch(e -> C3Util.sameNhC3System(master, e))) {
                    menu.add(menuItem("Form C3 Lance",
                          LMP_C3_FORM_NHC3 + "|" + master.getId() + enToken(entities),
                          true,
                          listener));
                }
            }

            Optional<Entity> c3Entity = entities.stream().filter(Entity::hasAnyC3System).findAny();

            Entity entity = null;

            if (c3Entity.isPresent()) {
                entity = entities.stream().filter(e -> e.hasC3S() || e.hasNhC3()).findAny().orElse(c3Entity.get());
            }

            // ideally, find one slave or C3i/NC3/Nova to get some connection options
            Game game = cg.getClient().getGame();
            ArrayList<String> usedNetIds = new ArrayList<>();

            if (entity != null) {
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
                            menu.add(menuItem(item, LMP_C3JOIN + "|" + other.getId() + enToken(entities), nodes != 0,
                                  listener));
                            usedNetIds.add(other.getC3NetId());
                        }

                    } else if (other.isC3CompanyCommander() && other.hasC3MM()) {
                        String item = "<HTML>Connect to " + other.getShortNameRaw() + idString(game, other.getId());
                        item += " (" + other.getC3NetId() + ")";
                        item += (nodes == 0 ? " - full" : " - " + nodes + " free spots");
                        menu.add(menuItem(item, LMP_C3CONNECT + "|" + other.getId() + enToken(entities), nodes != 0,
                              listener));

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
                        menu.add(menuItem(item, LMP_C3CONNECT + "|" + other.getId() + enToken(entities), nodes != 0,
                              listener));
                    }
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
            for (Player player : game.getPlayersList()) {
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
            for (Player player : game.getPlayersList()) {
                if (!player.isEnemyOf(gameForces.getOwner(force))) {
                    String command = LMP_F_ASSIGN_ONLY + "|" + player.getId() + ":" + foToken(forces) + NO_INFO;
                    fOnlyMenu.add(menuItem(player.getName(), command, true, listener));
                }
                String command = LMP_F_ASSIGN + "|" + player.getId() + ":" + foToken(forces) + NO_INFO;
                fFullMenu.add(menuItem(player.getName(), command, true, listener));
            }
            assignMenu.setEnabled(enabled && assignMenu.getItemCount() > 0);
            menu.add(assignMenu);
        }

        return menu;
    }

    /**
     * @return the "Equipment" submenu, allowing hot loading LRMs, setting MGs to rapid fire mode,
     *         and Variable Range Targeting mode selection
     */
    private static JMenu equipMenu(boolean anyRFOn, boolean anyRFOff, boolean anyHLOn,
          boolean anyHLOff, boolean anyVRTLong, boolean anyVRTShort, boolean anyVRT,
          boolean optHL, boolean optRF, ActionListener listener, String eIds) {
        JMenu menu = new JMenu(Messages.getString("ChatLounge.Equipment"));
        menu.setEnabled(anyRFOff || anyRFOn || anyHLOff || anyHLOn || anyVRT);
        if (optRF) {
            menu.add(menuItem(Messages.getString("ChatLounge.RapidFireToggleOn"), LMP_RAPID_FIRE_MG_ON + NO_INFO + eIds,
                  anyRFOff, listener));
            menu.add(menuItem(Messages.getString("ChatLounge.RapidFireToggleOff"),
                  LMP_RAPID_FIRE_MG_OFF + NO_INFO + eIds,
                  anyRFOn,
                  listener));
        }
        if (optHL) {
            menu.add(menuItem(Messages.getString("ChatLounge.HotLoadToggleOn"), LMP_HOT_LOAD_ON + NO_INFO + eIds,
                  anyHLOff, listener));
            menu.add(menuItem(Messages.getString("ChatLounge.HotLoadToggleOff"), LMP_HOT_LOAD_OFF + NO_INFO + eIds,
                  anyHLOn, listener));
        }
        if (anyVRT) {
            menu.add(menuItem(Messages.getString("ChatLounge.VRTSetLong"), LMP_VRT_LONG + NO_INFO + eIds,
                  anyVRTShort, listener));
            menu.add(menuItem(Messages.getString("ChatLounge.VRTSetShort"), LMP_VRT_SHORT + NO_INFO + eIds,
                  anyVRTLong, listener));
        }
        return menu;
    }

    /**
     * Returns the "Offload from" submenu, allowing to offload units from a specific bay of the given entity
     */
    private static JMenu offloadBayMenu(boolean enabled, Collection<Entity> entities, ActionListener listener) {
        JMenu menu = new JMenu("Offload All From...");
        if (enabled && entities.size() == 1) {
            Entity entity = theElement(entities);
            for (Bay bay : entity.getTransportBays()) {
                if (!bay.getLoadedUnits().isEmpty()) {
                    String label = "Bay #" + bay.getBayNumber() + " (" + bay.getLoadedUnits().size() + " units)";
                    menu.add(menuItem(label, LMP_UNLOAD_ALL_FROM_BAY + "|" + bay.getBayNumber() + enToken(entities),
                          true, listener));
                }
            }
        }
        menu.setEnabled(enabled && menu.getItemCount() > 0);
        MenuScroller.createScrollBarsOnMenus(menu);
        return menu;
    }

    /**
     * Returns the "Swap Pilot" submenu, allowing to swap the unit pilot with a pilot of an equivalent unit. Does work
     * with multiple selected units but expects the Lobby to issue an error message as only one unit can swap pilot with
     * one other unit.
     */
    private static JMenu swapPilotMenu(boolean enabled, Collection<Entity> entities, ClientGUI clientGui,
          ActionListener listener) {
        Game game = clientGui.getClient().getGame();

        JMenu menu = new JMenu("Swap pilots with");
        if (!entities.isEmpty()) {
            // use a random selected unit to determine the targets
            Entity entity = anyOneElement(entities);
            for (Entity swapper : game.getEntitiesVector()) {
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

    private static JMenu exportEntitySpriteMenu(final JFrame frame, final Entity entity) {
        final JMenu exportUnitSpriteMenu = new JMenu(Messages.getString("exportUnitSpriteMenu.title"));
        exportUnitSpriteMenu.setToolTipText(Messages.getString("exportUnitSpriteMenu.toolTipText"));
        exportUnitSpriteMenu.setName("exportUnitSpriteMenu");

        final JMenuItem miCurrentCamouflage = new JMenuItem(Messages.getString("miCurrentCamouflage.text"));
        miCurrentCamouflage.setToolTipText(Messages.getString("miCurrentCamouflage.toolTipText"));
        miCurrentCamouflage.setName("miCurrentCamouflage");
        miCurrentCamouflage
              .addActionListener(evt -> exportSprite(frame, entity, entity.getCamouflageOrElseOwners(), false));
        exportUnitSpriteMenu.add(miCurrentCamouflage);

        final JMenuItem miCurrentDamage = new JMenuItem(Messages.getString("miCurrentDamage.text"));
        miCurrentDamage.setToolTipText(Messages.getString("miCurrentDamage.toolTipText"));
        miCurrentDamage.setName("miCurrentDamage");
        miCurrentDamage.addActionListener(evt -> exportSprite(frame, entity, new Camouflage(), true));
        exportUnitSpriteMenu.add(miCurrentDamage);

        final JMenuItem miCurrentCamouflageAndDamage = new JMenuItem(
              Messages.getString("miCurrentCamouflageAndDamage.text"));
        miCurrentCamouflageAndDamage.setToolTipText(Messages.getString("miCurrentCamouflageAndDamage.toolTipText"));
        miCurrentCamouflageAndDamage.setName("miCurrentCamouflageAndDamage");
        miCurrentCamouflageAndDamage
              .addActionListener(evt -> exportSprite(frame, entity, entity.getCamouflageOrElseOwners(), true));
        exportUnitSpriteMenu.add(miCurrentCamouflageAndDamage);

        if (!(entity instanceof FighterSquadron)) {
            final JMenuItem miSelectedCamouflage = new JMenuItem(Messages.getString("miSelectedCamouflage.text"));
            miSelectedCamouflage.setToolTipText(Messages.getString("miSelectedCamouflage.toolTipText"));
            miSelectedCamouflage.setName("miSelectedCamouflage");
            miSelectedCamouflage.addActionListener(evt -> {
                final CamoChooserDialog camoChooserDialog = new CamoChooserDialog(frame,
                      entity.getCamouflageOrElseOwners());
                try {
                    if (camoChooserDialog.showDialog().isConfirmed()) {
                        exportSprite(frame, entity, camoChooserDialog.getSelectedItem(), false);
                    }
                } finally {
                    camoChooserDialog.dispose();
                }
            });
            exportUnitSpriteMenu.add(miSelectedCamouflage);

            final JMenuItem miSelectedCamouflageAndCurrentDamage = new JMenuItem(
                  Messages.getString("miSelectedCamouflageAndCurrentDamage.text"));
            miSelectedCamouflageAndCurrentDamage.setToolTipText(
                  Messages.getString("miSelectedCamouflageAndCurrentDamage.toolTipText"));
            miSelectedCamouflageAndCurrentDamage.setName("miSelectedCamouflageAndCurrentDamage");
            miSelectedCamouflageAndCurrentDamage.addActionListener(evt -> {
                final CamoChooserDialog camoChooserDialog = new CamoChooserDialog(frame,
                      entity.getCamouflageOrElseOwners());
                try {
                    if (camoChooserDialog.showDialog().isConfirmed()) {
                        exportSprite(frame, entity, camoChooserDialog.getSelectedItem(), true);
                    }
                } finally {
                    camoChooserDialog.dispose();
                }
            });
            exportUnitSpriteMenu.add(miSelectedCamouflageAndCurrentDamage);
        }

        return exportUnitSpriteMenu;
    }

    private static void exportSprite(final JFrame frame, final Entity entity,
          final Camouflage camouflage, final boolean showDamage) {
        // Save Location
        FileDialog fd = new FileDialog(frame, Messages.getString("ExportUnitSpriteDialog.title"));
        fd.setMode(FileDialog.SAVE);
        fd.setDirectory("");
        fd.setFile(entity.getDisplayName() + ".png");
        fd.setFilenameFilter((dir, file) -> file.endsWith(".png"));
        fd.setVisible(true);

        String directory = fd.getDirectory();
        String filename = fd.getFile();
        if ((filename == null) || (directory == null)) {
            return;
        }
        File file = new File(directory, filename);

        // Ensure it's a PNG file
        final String path = file.getPath();
        if (!path.endsWith(".png")) {
            file = new File(path + ".png");
        }

        // Get the Sprite
        final Image base = MMStaticDirectoryManager.getMekTileset().imageFor(entity);
        final Image sprite = EntityImage.createLobbyIcon(base, camouflage, entity).loadPreviewImage(showDamage);

        // Export to File
        try {
            ImageIO.write((BufferedImage) sprite, "png", file);
        } catch (Exception ex) {
            logger.error(ex, "Failed to export to file");
        }
    }

    /**
     * Returns a command string token containing the IDs of the given entities and a leading | E.g. |2,14,44,22
     */
    private static String enToken(Collection<Entity> entities) {
        if (entities.isEmpty()) {
            return "|-1";
        }
        List<String> ids = entities.stream().map(Entity::getId).map(Object::toString).collect(Collectors.toList());
        return "|" + String.join(",", ids);
    }

    /**
     * Returns a command string token containing the IDs of the given forces E.g. 2,14,44,22
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
        // Battle armor cannot use burst-fire MGs per errata
        if (entity instanceof BattleArmor) {
            return false;
        }
        for (Mounted<?> m : entity.getWeaponList()) {
            EquipmentType etype = m.getType();
            if (etype.hasFlag(WeaponType.F_MG) && m.isRapidFire()) {
                return true;
            }
        }
        return false;
    }

    /** Returns true when the entity has an MG set to normal (non-rapid) fire. */
    private static boolean hasNormalFireMG(Entity entity) {
        // Battle armor cannot use burst-fire MGs per errata
        if (entity instanceof BattleArmor) {
            return false;
        }
        for (Mounted<?> m : entity.getWeaponList()) {
            EquipmentType etype = m.getType();
            if (etype.hasFlag(WeaponType.F_MG) && !m.isRapidFire()) {
                return true;
            }
        }
        return false;
    }

    /** Returns true when the entity has a weapon with ammo set to hot-loaded. */
    private static boolean hasHotLoaded(Entity entity) {
        for (Mounted<?> ammo : entity.getAmmo()) {
            AmmoType etype = (AmmoType) ammo.getType();
            if (etype.hasFlag(AmmoType.F_HOTLOAD) && ammo.isHotLoaded()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true when the entity has a weapon with ammo set to non-hot-loaded.
     */
    private static boolean hasNonHotLoaded(Entity entity) {
        for (Mounted<?> ammo : entity.getAmmo()) {
            AmmoType etype = (AmmoType) ammo.getType();
            if (etype.hasFlag(AmmoType.F_HOTLOAD) && !ammo.isHotLoaded()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true when the entity has Variable Range Targeting quirk set to LONG mode.
     */
    private static boolean hasVRTLong(Entity entity) {
        return entity.hasVariableRangeTargeting() && entity.getVariableRangeTargetingMode().isLong();
    }

    /**
     * Returns true when the entity has Variable Range Targeting quirk set to SHORT mode.
     */
    private static boolean hasVRTShort(Entity entity) {
        return entity.hasVariableRangeTargeting() && entity.getVariableRangeTargetingMode().isShort();
    }

    /**
     * Returns true when the entity has the Variable Range Targeting quirk.
     */
    private static boolean hasVRT(Entity entity) {
        return entity.hasVariableRangeTargeting();
    }
}

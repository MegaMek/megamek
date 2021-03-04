/*  
 * MegaMek - Copyright (C) 2020 - The MegaMek Team  
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

import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.JMenu;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.util.MenuScroller;
import megamek.client.ui.swing.util.ScalingPopup;
import megamek.common.*;
import megamek.common.force.Force;
import megamek.common.force.Forces;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.preference.PreferenceManager;
import megamek.common.util.CollectionUtil;

import static megamek.client.ui.swing.util.UIUtil.*;

/** Creates the Lobby Mek right-click pop-up menu for both the sortable table and the force tree. */
class LobbyMekPopup {
    
    private static final String NOINFO = "|-1";
    
    static ScalingPopup getPopup(List<Entity> entities, List<Force> forces,
            ActionListener listener, ChatLounge lobby) {

        ClientGUI clientGui = lobby.getClientgui();
        
        GameOptions opts = clientGui.getClient().getGame().getOptions();
        boolean optQuirks = opts.booleanOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS);
        boolean optBurstMG = opts.booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_BURST);
        boolean optLRMHotLoad = opts.booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_HOTLOAD);
        boolean optCapFighters = opts.booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_CAPITAL_FIGHTER);

        // Generate a set of all selected entities and all entities in selected forces (and any subforces)
        Set<Entity> joinedEntities = new HashSet<Entity>(entities);
        for (Force force: forces) {
            joinedEntities.addAll(lobby.game().getForces().getFullEntities(force));
        }  
        
        // Create a list of selected units that belong to the local player or one
        // of his bots, as only those are configurable, so the popup menu really
        // reflects what can be configured
        HashSet<Entity> configurableEntities = new HashSet<>(entities);
        configurableEntities.removeIf(e -> !lobby.isEditable(e));

        boolean unconfigSelected = entities.size() != configurableEntities.size();
        boolean canConfigureAny = configurableEntities.size() > 0;
        boolean canConfigureAll = entities.size() == configurableEntities.size();
        boolean canConfigureDeployAll = lobby.canConfigureMultipleDeployment(entities);
        boolean canSeeAll = lobby.canSeeAll(entities);
        boolean oneSelected = entities.size() == 1;
        

        // Find certain unit features among all units the player can access
        // i.e. his own units or his bots' units (not only selected units!)
        HashSet<Entity> teamEntities = new HashSet<>(clientGui.getClient().getGame().getEntitiesVector());
        teamEntities.removeIf(e -> !lobby.isEditable(e));
        
        boolean accessibleFighters = false;
        boolean accessibleJumpships = false;
        boolean accessibleTransportBays = false;
        boolean accessibleCarriers = false;
        boolean accessibleProtomeks = false;
        for (Entity en: teamEntities) {
            accessibleFighters |= en.isFighter(); 
            accessibleJumpships |= en.hasETypeFlag(Entity.ETYPE_JUMPSHIP);
            accessibleTransportBays |= !en.getTransportBays().isEmpty();
            accessibleCarriers |= en.getLoadedUnits().size() > 0;
            accessibleProtomeks |= en.hasETypeFlag(Entity.ETYPE_PROTOMECH);
        }

        // Find what can be done with the entities
        boolean allCapFighter = !unconfigSelected;
        boolean allDropships = !unconfigSelected;
        boolean allProtomeks = !unconfigSelected;
        boolean anyHLOn = false;
        boolean anyHLOff = false;
        boolean anyRFMGOn = false;
        boolean anyRFMGOff = false;
        boolean anyCarrier = false;
        boolean allEmbarked = true;
        boolean noneEmbarked = true;
        boolean allHaveMagClamp = true;
        for (Entity en: joinedEntities) {
            if (en.getTransportId() == Entity.NONE) {
                allEmbarked = false;
            } else {
                noneEmbarked = false;
            }
            if (en.getLoadedUnits().size() > 0) {
                anyCarrier = true;
            }
            if (!en.isCapitalFighter(true) || (en instanceof FighterSquadron)) {
                allCapFighter = false;
            }
            if (en.hasETypeFlag(Entity.ETYPE_BATTLEARMOR)
                    || en.hasETypeFlag(Entity.ETYPE_PROTOMECH)) {
                allHaveMagClamp &= en.hasWorkingMisc(MiscType.F_MAGNETIC_CLAMP);
            }
            allProtomeks &= en.hasETypeFlag(Entity.ETYPE_PROTOMECH);
            allDropships &= en.hasETypeFlag(Entity.ETYPE_DROPSHIP);
            if (optBurstMG) {
                for (Mounted m: en.getWeaponList()) {
                    EquipmentType etype = m.getType();
                    if (etype.hasFlag(WeaponType.F_MG)) {
                        anyRFMGOn |= m.isRapidfire();
                        anyRFMGOff |= !m.isRapidfire();
                    }
                }
            }
            if (optLRMHotLoad) {
                for (Mounted ammo: en.getAmmo()) {
                    AmmoType etype = (AmmoType) ammo.getType();
                    if (etype.hasFlag(AmmoType.F_HOTLOAD)) {
                        anyHLOn |= ammo.isHotLoaded();
                        anyHLOff |= !ammo.isHotLoaded();
                    }
                }
            }
        }
        
        // "Joined" means all selected entities + all entities of selected forces incl. subforces
        boolean hasjoinedEntities = !joinedEntities.isEmpty();
        boolean joinedOneEntitySelected = (entities.size() == 1) && forces.isEmpty();
        boolean joinedCanSeeAllEntities = hasjoinedEntities && lobby.canSeeAll(joinedEntities);
        boolean joinedcanEditEntities = hasjoinedEntities && lobby.isEditable(joinedEntities);
        boolean joinedhasLateDeployment = hasjoinedEntities && joinedEntities.stream().anyMatch(e -> e.getDeployRound() != 0);
        boolean joinedCanConfigureDeployAll = hasjoinedEntities && lobby.canConfigureMultipleDeployment(joinedEntities);
        
        ScalingPopup popup = new ScalingPopup();
        
        // All command strings should follow the layout COMMAND|INFO|ID1|ID2|I3...
        // Commands that don't need INFO should still have it for parsing
        // Commands that don't need an entity ID should still add noInfo for parsing
        String eId = "|" + (entities.isEmpty() ? "-1" : entities.get(0).getId());
        String eIds = enToken(entities);
        String seIds = enToken(joinedEntities);
        
        popup.add(menuItem("View...", "VIEW" + NOINFO + seIds, hasjoinedEntities, listener, KeyEvent.VK_V));
        popup.add(menuItem("View BV Calculation...", "BV" + NOINFO + seIds, hasjoinedEntities, listener, KeyEvent.VK_B));
        popup.add(ScalingPopup.spacer());

        if (joinedOneEntitySelected) {
            popup.add(menuItem("Configure...", "CONFIGURE" + NOINFO + eId, hasjoinedEntities, listener, KeyEvent.VK_C));
        } else {
            popup.add(menuItem("Configure...", "CONFIGURE_ALL" + NOINFO + seIds, hasjoinedEntities, listener, KeyEvent.VK_C));
        }
        popup.add(menuItem("Edit Damage...", "DAMAGE" + NOINFO + seIds, hasjoinedEntities, listener, KeyEvent.VK_E));
        popup.add(menuItem("Set individual camo...", "INDI_CAMO" + NOINFO + seIds, hasjoinedEntities, listener, KeyEvent.VK_I));
        popup.add(deployMenu(clientGui, hasjoinedEntities, listener, joinedEntities));
        popup.add(randomizeMenu(hasjoinedEntities, listener, seIds));
        popup.add(swapPilotMenu(hasjoinedEntities, joinedEntities, clientGui, listener));
        
        if (optBurstMG || optLRMHotLoad) {
            popup.add(equipMenu(anyRFMGOn, anyRFMGOff, anyHLOn, anyHLOff, optLRMHotLoad, optBurstMG, listener, seIds));
        }
        
        if (optQuirks) {
            popup.add(quirksMenu(!entities.isEmpty() && canSeeAll, listener, eIds));
        }
        
        popup.add(ScalingPopup.spacer());
        popup.add(changeOwnerMenu(!entities.isEmpty() || !forces.isEmpty(), clientGui, listener, entities, forces));
        popup.add(loadMenu(clientGui, canConfigureAny && !allEmbarked, listener, entities));
        
        if (accessibleCarriers) {
            popup.add(menuItem("Disembark / leave from carriers", "UNLOAD" + NOINFO + eIds, canConfigureAny && !noneEmbarked, listener));
            popup.add(menuItem("Offload all carried units", "UNLOADALL" + NOINFO + eIds, canConfigureAny && anyCarrier, listener));
        }

        if (accessibleTransportBays) {
            popup.add(offloadBayMenu(oneSelected && anyCarrier && canConfigureAny, entities.get(0), listener));
        }

        if (accessibleFighters && optCapFighters) {
            boolean fsEnabled = canConfigureAny && allCapFighter && noneEmbarked;
            popup.add(squadronMenu(clientGui, fsEnabled, listener, entities));
        }

        if (accessibleJumpships) {
            boolean jsEnabled = canConfigureAny && allDropships && noneEmbarked;
            popup.add(jumpShipMenu(clientGui, jsEnabled, listener, entities));
        }
        
        if (accessibleProtomeks) {
            boolean prEnabled = oneSelected && canConfigureAny && allProtomeks && noneEmbarked && allHaveMagClamp;
            popup.add(protoMenu(clientGui, prEnabled, listener, entities.get(0)));
        }
        
        popup.add(c3Menu(joinedOneEntitySelected && canConfigureAny, entities, clientGui, listener));
        popup.add(forceMenu(lobby, entities, forces, listener));
        
        popup.add(ScalingPopup.spacer());
        popup.add(menuItem("Delete", "DELETE" + NOINFO + eIds, !entities.isEmpty() && forces.isEmpty(), listener, KeyEvent.VK_D));
        
        return popup;
    }
    
    /**
     * Returns the "Force" submenu, allowing assignment to forces
     */
    private static JMenu forceMenu(ChatLounge lobby, List<Entity> entities, List<Force> forces, ActionListener listener) {
        IGame game = lobby.getClientgui().getClient().getGame();
        Forces gameForces = game.getForces();
        IPlayer localPlayer = lobby.getClientgui().getClient().getLocalPlayer();

        JMenu menu = new JMenu("Force");
        menu.add(menuItem("Create new Force...", "FCREATETOP" + NOINFO + NOINFO, true, listener));
        
        // If exactly one force is selected, offer force options
        if ((forces.size() == 1) && entities.isEmpty()) {
            Force force = forces.get(0);
            boolean editable = lobby.lobbyActions.isEditable(force);
            String fId = "|" + force.getId();
            menu.add(menuItem("Add Subforce...", "FCREATESUB" + fId + NOINFO, editable, listener));
            menu.add(menuItem("Rename", "FRENAME" + fId + NOINFO, editable, listener));
            menu.add(menuItem("Promote to Top-Level Force", "FPROMOTE" + fId + NOINFO, editable && !force.isTopLevel(), listener));
//            JMenu assignMenu = new JMenu("Assign to");
//            JMenu fOnlyMenu = new JMenu("Force only");
//            JMenu fFullMenu = new JMenu("Everything in the Force");
//            assignMenu.add(fOnlyMenu);
//            assignMenu.add(fFullMenu);
//            for (IPlayer player: game.getPlayersVector()) {
//                if (player.getId() != gameForces.getOwnerId(force)) {
//                    fFullMenu.add(menuItem(player.getName(), "FASSIGN|" + player.getId() + ":" + force.getId() + NOINFO, true, listener));
//                }
//                if (!player.isEnemyOf(gameForces.getOwner(force))) {
//                    fOnlyMenu.add(menuItem(player.getName(), "FASSIGNONLY|" + player.getId() + ":" + force.getId() + NOINFO, true, listener));
//                }
//            }
//            assignMenu.setEnabled(editable && assignMenu.getItemCount() > 0);
//            menu.add(assignMenu);
            
        }

        menu.add(menuItem("Delete Force(s)", "FDELETE|" + foToken(forces) + enToken(entities), true, listener));
        
        // If entities are selected but no forces, offer entity options
        if (forces.isEmpty() && !entities.isEmpty() && LobbyUtility.haveSingleOwner(entities)) {
//            for (Force candidate: gameForces.getAvailableForces(entities.get(0).getOwner())) {
//                String command = "FADDTO|" + candidate.getId() + enToken(entities);
//                String display = candidate.getName() + idString(game, candidate.getId());
//                menu.add(menuItem("Add to " + display, command, true, listener));
//            }
            menu.add(menuItem("Remove from Force", "FREMOVE" + NOINFO + enToken(entities), true, listener));
        }

        menu.setEnabled(menu.getItemCount() > 0);
        return menu;
    }
    
    static String idString(IGame game, int id) {
        if (PreferenceManager.getClientPreferences().getShowUnitId()) {
            return " [" + id + "]"; 
        } else {
            return "";
        }
    }

    /**
     * Returns the "Load" submenu, allowing general embarking
     */
    private static JMenu loadMenu(ClientGUI clientGui, boolean enabled, ActionListener listener,
            Collection<Entity> entities) {

        JMenu menu = new JMenu("Load onto");
        menu.setEnabled(enabled);
        if (enabled) {
            for (Entity loader: clientGui.getClient().getGame().getEntitiesVector()) {
                if (loader.isCapitalFighter()) {
                    continue;
                }
                boolean loadable = true;
                for (Entity en : entities) {
                    if (!loader.canLoad(en, false)
                            || (loader.getId() == en.getId())
                            || en.hasETypeFlag(Entity.ETYPE_PROTOMECH)
                            //TODO: support edge case where a support vee with an internal vehicle bay can load trailer internally
                            || (loader.canTow(en.getId()))) {
                        loadable = false;
                        break;
                    }
                }
                if (loadable) {
                    menu.add(menuItem(loader.getShortName(), "LOAD|" + loader.getId() + ":-1" + enToken(entities), enabled, listener));
                }
            }
        }
        menu.setEnabled(enabled && (menu.getItemCount() > 0));
        return menu;
    }

    /**
     * Returns the "Load Protomech" submenu
     */
    private static JMenu protoMenu(ClientGUI clientGui, boolean enabled, ActionListener listener,
            Entity entity) {

        JMenu menu = new JMenu("Load Protomek");
        if (enabled) {
            for (Entity loader: clientGui.getClient().getGame().getEntitiesVector()) {
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
                    loaderMenu.add(menuItem("Onto Front", "LOAD|" + loader.getId() + ":0", enabled, listener));
                }
                boolean frontUltra = (front != null)
                        && front.getLoadedUnits().stream()
                        .anyMatch(l -> l.getWeightClass() == EntityWeightClass.WEIGHT_SUPER_HEAVY);
                if ((rear != null) && rear.canLoad(entity) && !frontUltra) {
                    loaderMenu.add(menuItem("Onto Rear", "LOAD|" + loader.getId() + ":1", enabled, listener));
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
    private static JMenu squadronMenu(ClientGUI clientGui, boolean enabled, ActionListener listener,
            Collection<Entity> entities) {

        JMenu menu = new JMenu("Fighter Squadrons");
        menu.setEnabled(enabled);
        if (enabled) {
            menu.add(menuItem("Create Fighter Squadron", "SQUADRON", enabled, listener));
            for (Entity loader: clientGui.getClient().getGame().getEntitiesVector()) {
                // TODO don't allow capital fighters to load one another
                // at the moment
                if (!(loader instanceof FighterSquadron)) {
                    continue;
                }
                boolean loadable = true;
                for (Entity en: entities) {
                    if (!loader.canLoad(en, false) || (loader.getId() == en.getId())) {
                        loadable = false;
                        break;
                    }
                }
                if (loadable) {
                    menu.add(menuItem("Join " + loader.getShortName(), "LOAD|" + loader.getId() + ":-1" + enToken(entities), enabled, listener));
                }
            }
        }
        return menu;
    }

    /**
     * Returns the "Load onto" submenu, allowing to load dropships
     * onto a jumpship
     */
    private static JMenu jumpShipMenu(ClientGUI clientGui, boolean enabled, ActionListener listener,
            Collection<Entity> entities) {

        JMenu menu = new JMenu("Load onto...");
        if (enabled) {
            for (Entity loader: clientGui.getClient().getGame().getEntitiesVector()) {
                if (!(loader instanceof Jumpship)) {
                    continue;
                }
                boolean loadable = true;
                for (Entity en : entities) {
                    if (!loader.canLoad(en, false) || (loader.getId() == en.getId())) {
                        loadable = false;
                        break;
                    }
                }
                if (loadable) {
                    int freeCollars = 0;
                    for (Transporter t : loader.getTransports()) {
                        if (t instanceof DockingCollar) {
                            freeCollars += (int)t.getUnused();
                        }
                    }
                    String name = loader.getShortName() + " (Free Collars: " + freeCollars + ")";
                    menu.add(menuItem(name, "LOAD|" + loader.getId() + ":-1" + enToken(entities), enabled, listener));
                }
            }
        }
        menu.setEnabled(enabled && (menu.getItemCount() > 0));
        MenuScroller.createScrollBarsOnMenus(menu);
        return menu;
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
                menu.add(menuItem("Hidden", "HIDDEN|HIDE" + eIds, anyNotHidden, listener));
                menu.add(menuItem("Not Hidden", "HIDDEN|NOHIDE" + eIds, anyHidden, listener));
                menu.add(ScalingPopup.spacer());
            }

            menu.add(menuItem("Standing", "STAND|STAND" + eIds, enabled, listener));
            menu.add(menuItem("Prone", "STAND|PRONE" + eIds, enabled, listener));
            if (clientGui.getClient().getGame().getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_HULL_DOWN)) {
                menu.add(menuItem("Hull-Down", "STAND|HULLDOWN" + eIds, enabled, listener));
            }
            menu.add(ScalingPopup.spacer());

            // Heat
            JMenu heatMenu = new JMenu("Heat at start");
            heatMenu.add(menuItem("No heat", "HEAT|0" + eIds, enabled, listener));
            for (int i = 1; i < 11; i++) {
                heatMenu.add(menuItem("Heat " + i, "HEAT|" + i + eIds, enabled, listener));
            }
            JMenu subHeatMenu = new JMenu("More heat");
            for (int i = 11; i < 41; i++) {
                subHeatMenu.add(menuItem("Heat " + i, "DEPLOY|" + i + eIds, enabled, listener));
            }
            heatMenu.add(subHeatMenu);
            menu.add(heatMenu);
            menu.add(ScalingPopup.spacer());

            // Late deployment
            JMenu lateMenu = new JMenu("Deployment round");
            lateMenu.add(menuItem("At game start", "DEPLOY|0" + eIds, enabled, listener));
            for (int i = 2; i < 11; i++) {
                lateMenu.add(menuItem("Before round " + i, "DEPLOY|" + i + eIds, enabled, listener));
            }
            JMenu veryLateMenu = new JMenu("Later");
            for (int i = 11; i < 41; i++) {
                veryLateMenu.add(menuItem("Before round " + i, "DEPLOY|" + i + eIds, enabled, listener));
            }
            
            lateMenu.add(veryLateMenu);
            menu.add(lateMenu);
        }
        menu.setEnabled(enabled);
        return menu;
    }

    /**
     * Returns the "Randomize" submenu, allowing to randomly assign
     * name, callsign and skills
     */
    private static JMenu randomizeMenu(boolean enabled, ActionListener listener, String eIds) {
        // listener menu uses the following Mnemonic Keys:
        // C, N, S

        JMenu menu = new JMenu("Randomize");
        menu.setEnabled(enabled);
        menu.setMnemonic(KeyEvent.VK_R);

        menu.add(menuItem("Name", "NAME" + NOINFO + eIds, enabled, listener, KeyEvent.VK_N));
        menu.add(menuItem("Callsign", "CALLSIGN" + NOINFO + eIds, enabled, listener, KeyEvent.VK_C));
        menu.add(menuItem("Skills", "SKILLS" + NOINFO + eIds, enabled, listener, KeyEvent.VK_S));
        return menu;
    }
    
    /**
     * Returns the "C3" submenu, allowing C3 changes
     */
    private static JMenu c3Menu(boolean enabled, List<Entity> entities, ClientGUI cg, ActionListener listener) {
        JMenu menu = new JMenu("C3");
        menu.setEnabled(false);

        if (!entities.isEmpty()) {
            Entity entity = entities.get(0);
            enabled = enabled && entity.hasAnyC3System();
            

            String eId = "|" + entity.getId();
            if ((entity.getC3Master() != null && !entity.isC3CompanyCommander())
                    || (entity.hasNhC3() && !isNhC3Owner(entity))) {
                menu.add(menuItem("Disconnect", "C3DISCONNECT" + NOINFO + eId, enabled, listener));
            }

            if (entity.hasC3MM() || entity.hasC3M()) {
                if (!entity.isC3CompanyCommander()) {
                    String item = "Set as C3 Company Master";
                    menu.add(menuItem(item, "C3CM" + NOINFO + eId, enabled, listener));
                }

                if (!entity.isC3IndependentMaster()) {
                    String item = "Set as C3 Lance Master";
                    menu.add(menuItem(item, "C3LM" + NOINFO + eId, enabled, listener));
                }
            }

            ArrayList<String> usedNetIds = new ArrayList<String>();
            for (Entity other : cg.getClient().getEntitiesVector()) {
                // ignore enemies and self; only link the same type of C3
                if (entity.isEnemyOf(other) || entity.equals(other)
                        || (entity.hasC3i() != other.hasC3i())
                        || (entity.hasNavalC3() != other.hasNavalC3())
                        || (entity.hasNovaCEWS() != other.hasNovaCEWS())
                        ) {
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
                if (entity.C3MasterIs(other) && !entity.equals(other)) {
                    nodes++;
                }
                if ((entity.hasNhC3()) && (entity.onSameC3NetworkAs(other) || entity.equals(other))) {
                    nodes++;
                }
                if (nodes == 0) {
                    continue;
                }

                if (other.hasNhC3()) {
                    // Don't add the following checks to the line above
                    if (!entity.onSameC3NetworkAs(other) && !usedNetIds.contains(other.getC3NetId())) {
                        String item = Messages.getString("CustomMechDialog.join1", 
                                other.getShortNameRaw(), other.getC3NetId(), nodes);
                        menu.add(menuItem(item, "C3JOIN|" + other.getId() + eId, enabled, listener));
                        usedNetIds.add(other.getC3NetId());
                    }

                } else if (other.C3MasterIs(other) && other.hasC3MM()) {
                    // Company masters with 2 computers can have *both* sub-masters AND slave units.
                    String item = Messages.getString("CustomMechDialog.connect2", 
                            other.getShortNameRaw(), other.getC3NetId(), nodes);
                    menu.add(menuItem(item, "C3CONNECT|" + other.getId() + eId, enabled, listener));

                } else if (other.C3MasterIs(other) != entity.hasC3M()) {
                    // If we're a slave-unit, we can only connect to sub-masters,
                    // not main masters; likewise, if we're a master unit, we can
                    // only connect to main master units, not sub-masters.
                } else if (entity.C3MasterIs(other)) {
                } else {
                    // Make sure the limit of 12 units in a C3 network is maintained
                    int entC3nodeCount = cg.getClient().getGame().getC3SubNetworkMembers(entity).size();
                    int choC3nodeCount = cg.getClient().getGame().getC3NetworkMembers(other).size();
                    if ((entC3nodeCount + choC3nodeCount) <= Entity.MAX_C3_NODES) {
                        String item = Messages.getString("CustomMechDialog.connect2", 
                                other.getShortNameRaw(), nodes);
                        menu.add(menuItem(item, "C3CONNECT|" + other.getId() + eId, enabled, listener));
                    }
                }
            }
            menu.setEnabled(enabled && menu.getItemCount() > 0);
        }
        return menu;
    }
    
    /** 
     * Returns true when this entity is the "owner" of this C3i or NC3 network
     * which (only) means that the network id uses this entity's id.
     */
    private static boolean isNhC3Owner(Entity entity) {
        return entity.hasNhC3() && entity.getC3NetId().endsWith("." + entity.getId());
    }

    /**
     * Returns the "Change Unit Owner" submenu.
     */
    private static JMenu changeOwnerMenu(boolean enabled, ClientGUI clientGui, ActionListener listener, 
            Collection<Entity> entities, Collection<Force> forces) {

        JMenu menu = new JMenu(Messages.getString("ChatLounge.ChangeOwner"));
        menu.setEnabled(enabled);
        menu.setMnemonic(KeyEvent.VK_O);
        
        IGame game = clientGui.getClient().getGame();
        Forces gameForces = game.getForces();
        
        if (!entities.isEmpty()) {
            for (IPlayer player: game.getPlayersVector()) {
                String command = "ASSIGN|" + player.getId() + ":" + foToken(forces) + enToken(entities);
                menu.add(menuItem(player.getName(), command, enabled, listener));
            }
        }
        
        if (entities.isEmpty() && !forces.isEmpty()) {
            JMenu assignMenu = new JMenu("Assign to");
            JMenu fOnlyMenu = new JMenu("Force only");
            JMenu fFullMenu = new JMenu("Everything in the Force");
            assignMenu.add(fOnlyMenu);
            assignMenu.add(fFullMenu);
            Force force = CollectionUtil.randomElement(forces);
            for (IPlayer player: game.getPlayersVector()) {
                if (!player.isEnemyOf(gameForces.getOwner(force))) {
                    String command = "FASSIGNONLY|" + player.getId() + ":" + foToken(forces) + NOINFO;
                    fOnlyMenu.add(menuItem(player.getName(), command, true, listener));
                }
                String command = "FASSIGN|" + player.getId() + ":" + foToken(forces) + NOINFO;
                fFullMenu.add(menuItem(player.getName(), command, true, listener));
            }
            assignMenu.setEnabled(enabled && assignMenu.getItemCount() > 0);
            menu.add(assignMenu);
        }

        return menu;
    }

    /**
     * Returns the "Quirks" submenu, allowing to save the quirks
     * to the quirks config file.
     */
    private static JMenu quirksMenu(boolean enabled, ActionListener listener, String eIds) {

        JMenu menu = new JMenu(Messages.getString("ChatLounge.popup.quirks"));
        menu.setEnabled(enabled);
        menu.add(menuItem("Save Quirks for Chassis", "SAVE_QUIRKS_ALL" + eIds, enabled, listener));
        menu.add(menuItem("Save Quirks for Chassis/Model", "SAVE_QUIRKS_MODEL" + eIds, enabled, listener));
        return menu;
    }

    /**
     * Returns the "Equipment" submenu, allowing 
     * hotloading LRMs and
     * setting MGs to rapid fire mode
     */
    private static JMenu equipMenu(boolean anyRFOn, boolean anyRFOff, boolean anyHLOn, boolean anyHLOff,
            boolean optHL, boolean optRF, ActionListener listener, String eIds) {

        JMenu menu = new JMenu(Messages.getString("ChatLounge.Equipment"));
        menu.setEnabled(anyRFOff || anyRFOn || anyHLOff || anyHLOn);        
        if (optRF) {
            menu.add(menuItem(Messages.getString("ChatLounge.RapidFireToggleOn"), "RAPIDFIREMG_ON" + NOINFO + eIds, 
                    anyRFOff, listener));
            menu.add(menuItem(Messages.getString("ChatLounge.RapidFireToggleOff"), "RAPIDFIREMG_OFF" + NOINFO + eIds, 
                   anyRFOn, listener));
        }
        if (optHL) {
            menu.add(menuItem(Messages.getString("ChatLounge.HotLoadToggleOn"), "HOTLOAD_ON" + NOINFO + eIds, 
                    anyHLOff, listener));
            menu.add(menuItem(Messages.getString("ChatLounge.HotLoadToggleOff"), "HOTLOAD_OFF" + NOINFO + eIds, 
                    anyHLOn, listener));
        }
        return menu;
    }
    
    /**
     * Returns the "Offload from" submenu, allowing to offload
     * units from a specific bay of the given entity
     */
    private static JMenu offloadBayMenu(boolean enabled, Entity entity, ActionListener listener) {

        JMenu menu = new JMenu("Offload All From...");
        if (enabled) {
            for (Bay bay : entity.getTransportBays()) {
                if (bay.getLoadedUnits().size() > 0) {
                    String label = "Bay #" + bay.getBayNumber() + " (" + bay.getLoadedUnits().size() + " units)";
                    menu.add(menuItem(label, "UNLOADALLFROMBAY|" + entity.getId() + "|" + bay.getBayNumber(), enabled, listener));
                }
            }
        }
        menu.setEnabled(enabled && menu.getItemCount() > 0);
        MenuScroller.createScrollBarsOnMenus(menu);
        return menu;
    }

    /**
     * Returns the "Swap Pilot" submenu, allowing to swap the unit
     * pilot with a pilot of an equivalent unit
     */
    private static JMenu swapPilotMenu(boolean enabled, Collection<Entity> entities, ClientGUI clientGui, ActionListener listener) {

        JMenu menu = new JMenu("Swap pilots with");
        
        if (!entities.isEmpty()) {
            Entity entity = CollectionUtil.randomElement(entities);
            for (Entity swapper: clientGui.getClient().getGame().getEntitiesVector()) {
                if (swapper.isCapitalFighter()) {
                    continue;
                }
                // only swap your own pilots and with the same unit and crew type
                if ((swapper.getOwnerId() == entity.getOwnerId()) && (swapper.getId() != entity.getId())
                        && (swapper.getUnitType() == entity.getUnitType())
                        && swapper.getCrew().getCrewType() == entity.getCrew().getCrewType()) {
                    menu.add(menuItem(swapper.getShortName(), "SWAP|" + swapper.getId() + enToken(entities), enabled, listener));
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
        List<String> ids = entities.stream().map(e -> e.getId()).map(n -> n.toString()).collect(Collectors.toList());
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
        List<String> ids = forces.stream().map(e -> e.getId()).map(n -> n.toString()).collect(Collectors.toList());
        return String.join(",", ids);
    }
}


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

import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.util.MenuScroller;
import megamek.client.ui.swing.util.ScalingPopup;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.*;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;

class MekTablePopup {

    static ScalingPopup mekTablePopup(ClientGUI clientGui, List<Entity> entities, 
            Entity e2, ActionListener listener, ChatLounge lobby) {
        
        if (entities.isEmpty()) {
            return new ScalingPopup();
        }
        
        GameOptions opts = clientGui.getClient().getGame().getOptions();
        boolean optQuirks = opts.booleanOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS);
        boolean optBurstMG = opts.booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_BURST);
        boolean optLRMHotLoad = opts.booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_HOTLOAD);
        boolean optCapFighters = opts.booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_CAPITAL_FIGHTER);
        
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
        boolean anyHotLoadOn = false;
        boolean anyHotLoadOff = false;
        boolean anyRapidFireMGOn = false;
        boolean anyRapidFireMGOff = false;
        boolean anyCarrier = false;
        boolean allEmbarked = true;
        boolean noneEmbarked = true;
        boolean allHaveMagClamp = true;
        for (Entity en: configurableEntities) {
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
                        anyRapidFireMGOn |= m.isRapidfire();
                        anyRapidFireMGOff |= !m.isRapidfire();
                    }
                }
            }
            if (optLRMHotLoad) {
                for (Mounted ammo: en.getAmmo()) {
                    AmmoType etype = (AmmoType)ammo.getType();
                    if (etype.hasFlag(AmmoType.F_HOTLOAD)) {
                        anyHotLoadOn |= ammo.isHotLoaded();
                        anyHotLoadOff |= !ammo.isHotLoaded();
                    }
                }
            }
        }
        
        ScalingPopup popup = new ScalingPopup();
        
        popup.add(menuItem("View...", "VIEW", oneSelected && canSeeAll, listener, KeyEvent.VK_V));
        popup.add(menuItem("View BV Calculation...", "BV", oneSelected && canSeeAll, listener, KeyEvent.VK_B));
        popup.add(menuItem("Edit Damage...", "DAMAGE", oneSelected && canConfigureAny, listener, KeyEvent.VK_E));

        if (oneSelected) {
            popup.add(menuItem("Configure...", "CONFIGURE", canConfigureAny, listener, KeyEvent.VK_C));
        } else {
            popup.add(menuItem("Configure all...", "CONFIGURE_ALL", canConfigureDeployAll, listener, KeyEvent.VK_C));
        }
        popup.add(deployMenu(canConfigureAny, listener));
        popup.add(spacer());
        popup.add(menuItem("Set individual camo...", "INDI_CAMO", canConfigureAny, listener, KeyEvent.VK_I));
        popup.add(menuItem("Delete", "DELETE", canConfigureAll, listener, KeyEvent.VK_D));
        popup.add(randomizeMenu(canConfigureAny, listener));
        popup.add(changeOwnerMenu(canConfigureAny, clientGui, listener));
        popup.add(swapPilotMenu(oneSelected && canConfigureAny, entities.get(0), clientGui, listener));
        
        popup.add(c3Menu(oneSelected && canConfigureAny, entities.get(0), clientGui, listener));
        
        if (optBurstMG || optLRMHotLoad) {
            popup.add(equipMenu(anyRapidFireMGOn, anyRapidFireMGOff, anyHotLoadOn, anyHotLoadOff, optLRMHotLoad, optBurstMG, listener));
        }
        
        if (optQuirks) {
            popup.add(quirksMenu(canSeeAll, listener));
        }
        
        popup.add(spacer());

        popup.add(loadMenu(clientGui, canConfigureAny && !allEmbarked, listener, entities));
        
        if (accessibleCarriers) {
            popup.add(menuItem("Disembark / leave from carriers", "UNLOAD", canConfigureAny && !noneEmbarked, listener));
            popup.add(menuItem("Offload all carried units", "UNLOADALL", canConfigureAny && anyCarrier, listener));
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

        return popup;
    }

    /** Returns a spacer (empty, small menu item) for the popup. */
    private static JMenuItem spacer() {

        JMenuItem result = new JMenuItem() {
            private static final long serialVersionUID = 1249257644704746075L;

            @Override
            public Dimension getPreferredSize() {
                Dimension s = super.getPreferredSize();
                return new Dimension(s.width, UIUtil.scaleForGUI(8));
            }
        };
        result.setEnabled(false);
        return result;
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
                    menu.add(menuItem(loader.getShortName(), "LOAD|" + loader.getId() + ":-1", enabled, listener));
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
                    menu.add(menuItem("Join " + loader.getShortName(), "LOAD|" + loader.getId() + ":-1", enabled, listener));
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
                    menu.add(menuItem(name, "LOAD|" + loader.getId() + ":-1", enabled, listener));
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
    private static JMenu deployMenu(boolean enabled, ActionListener listener) {

        JMenu menu = new JMenu("Deploy");
        if (enabled) {
            menu.add(menuItem("At game start", "DEPLOY|0", enabled, listener));
            for (int i = 2; i < 11; i++) {
                menu.add(menuItem("Before round " + i, "DEPLOY|" + i, enabled, listener));
            }
            JMenu subMenu = new JMenu("Later");
            for (int i = 11; i < 41; i++) {
                subMenu.add(menuItem("Before round " + i, "DEPLOY|" + i, enabled, listener));
            }
            menu.add(subMenu);
        }
        menu.setEnabled(enabled);
        return menu;
    }

    /**
     * Returns the "Randomize" submenu, allowing to randomly assign
     * name, callsign and skills
     */
    private static JMenu randomizeMenu(boolean enabled, ActionListener listener) {
        // listener menu uses the following Mnemonic Keys:
        // C, N, S

        JMenu menu = new JMenu("Randomize");
        menu.setEnabled(enabled);
        menu.setMnemonic(KeyEvent.VK_R);

        menu.add(menuItem("Name", ChatLounge.NAME_COMMAND, enabled, listener, KeyEvent.VK_N));
        menu.add(menuItem("Callsign", ChatLounge.CALLSIGN_COMMAND, enabled, listener, KeyEvent.VK_C));
        menu.add(menuItem("Skills", "SKILLS", enabled, listener, KeyEvent.VK_S));
        return menu;
    }
    
    /**
     * Returns the "C3" submenu, allowing C3 changes
     */
    private static JMenu c3Menu(boolean enabled, Entity entity, ClientGUI cg, ActionListener listener) {
        JMenu menu = new JMenu("C3");
        enabled = enabled && (entity.hasC3() || entity.hasC3i() || entity.hasNavalC3());
        menu.setEnabled(enabled);

        if ((entity.hasC3i() || entity.hasNavalC3()) && entity.calculateFreeC3Nodes() < 5 
                && !isC3iOwner(entity)) {
            menu.add(menuItem("Disconnect", "C3|CREATE", enabled, listener));
            
        } else if (entity.hasC3MM() || entity.hasC3M()) {
            if (!entity.isC3CompanyCommander()) {
                String item = "Set as C3 Company Commander";
                menu.add(menuItem(item, "C3|C3CC", enabled, listener));
            }

            if (!entity.isC3IndependentMaster()) {
                String item = "Set as independent C3 Master";
                menu.add(menuItem(item, "C3|C3IM", enabled, listener));
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
            if ((eCompanyMaster != null)
                    && (eCompanyMaster.getC3Master() != eCompanyMaster)) {
                continue;
            }
            int nodes = other.calculateFreeC3Nodes();
            if (other.hasC3MM() && entity.hasC3M() && other.C3MasterIs(other)) {
                nodes = other.calculateFreeC3MNodes();
            }
            if (entity.C3MasterIs(other) && !entity.equals(other)) {
                nodes++;
            }
            if ((entity.hasC3i() || entity.hasNavalC3())
                    && (entity.onSameC3NetworkAs(other) || entity.equals(other))) {
                nodes++;
            }
            if (nodes == 0) {
                continue;
            }
            
            if (other.hasNC3OrC3i()) {
                // Don't add the following checks to the line above
                if (!entity.onSameC3NetworkAs(other) && !usedNetIds.contains(other.getC3NetId())) {
                    String item = Messages.getString("CustomMechDialog.join1", 
                            other.getShortNameRaw(), other.getC3NetId(), nodes);
                    menu.add(menuItem(item, "C3|JOIN|" + other.getId(), enabled, listener));
                    usedNetIds.add(other.getC3NetId());
                }
                
            } else if (other.C3MasterIs(other) && other.hasC3MM()) {
                // Company masters with 2 computers can have *both* sub-masters AND slave units.
                String item = Messages.getString("CustomMechDialog.connect2", 
                        other.getShortNameRaw(), other.getC3NetId(), nodes);
                menu.add(menuItem(item, "C3|CONNECT|" + other.getId(), enabled, listener));
                
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
                    menu.add(menuItem(item, "C3|CONNECT|" + other.getId(), enabled, listener));
                }
            }
        }
        
        if (entity.getC3Master() != null && !entity.isC3CompanyCommander()) {
            menu.add(menuItem("Disconnect", "C3|DISCONNECT", enabled, listener));
        }
    
        return menu;
    }
    
    /** 
     * Returns true when this entity is the "owner" of this C3i or NC3 network
     * which (only) means that the network id uses this entity's id.
     */
    private static boolean isC3iOwner(Entity entity) {
        return (entity.hasC3i() && entity.getC3NetId().equals("C3i." + entity.getId()))
                || (entity.hasNavalC3() && entity.getC3NetId().equals("NC3." + entity.getId()));
    }

    /**
     * Returns the "Change Unit Owner" submenu.
     */
    private static JMenu changeOwnerMenu(boolean enabled, ClientGUI clientGui, ActionListener listener) {

        JMenu menu = new JMenu(Messages.getString("ChatLounge.ChangeOwner"));
        menu.setEnabled(enabled);
        menu.setMnemonic(KeyEvent.VK_O);

        for (IPlayer player: clientGui.getClient().getGame().getPlayersVector()) {
            menu.add(menuItem(player.getName(), "CHANGE_OWNER|" + player.getId(), enabled, listener));
        }
        return menu;
    }

    /**
     * Returns the "Quirks" submenu, allowing to save the quirks
     * to the quirks config file.
     */
    private static JMenu quirksMenu(boolean enabled, ActionListener listener) {

        JMenu menu = new JMenu(Messages.getString("ChatLounge.popup.quirks"));
        menu.setEnabled(enabled);
        menu.add(menuItem("Save Quirks for Chassis", "SAVE_QUIRKS_ALL", enabled, listener));
        menu.add(menuItem("Save Quirks for Chassis/Model", "SAVE_QUIRKS_MODEL", enabled, listener));
        return menu;
    }

    /**
     * Returns the "Equipment" submenu, allowing 
     * hotloading LRMs and
     * setting MGs to rapid fire mode
     */
    private static JMenu equipMenu(boolean anyRFOn, boolean anyRFOff, boolean anyHLOn, boolean anyHLOff,
            boolean optHL, boolean optRF, ActionListener listener) {

        JMenu menu = new JMenu(Messages.getString("ChatLounge.Equipment"));
        menu.setEnabled(anyRFOff || anyRFOn || anyHLOff || anyHLOn);        
        if (optRF) {
            menu.add(menuItem(Messages.getString("ChatLounge.RapidFireToggleOn"), "RAPIDFIREMG_ON", 
                    anyRFOff, listener));
            menu.add(menuItem(Messages.getString("ChatLounge.RapidFireToggleOff"), "RAPIDFIREMG_OFF", 
                   anyRFOn, listener));
        }
        if (optHL) {
            menu.add(menuItem(Messages.getString("ChatLounge.HotLoadToggleOn"), "HOTLOAD_ON", 
                    anyHLOff, listener));
            menu.add(menuItem(Messages.getString("ChatLounge.HotLoadToggleOff"), "HOTLOAD_OFF", 
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
                    menu.add(menuItem(label, "UNLOADALLFROMBAY|" + bay.getBayNumber(), enabled, listener));
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
    private static JMenu swapPilotMenu(boolean enabled, Entity entity, ClientGUI clientGui, ActionListener listener) {

        JMenu menu = new JMenu("Swap pilots with");
        for (Entity swapper: clientGui.getClient().getGame().getEntitiesVector()) {
            if (swapper.isCapitalFighter()) {
                continue;
            }
            // only swap your own pilots and with the same unit and crew type
            if ((swapper.getOwnerId() == entity.getOwnerId()) && (swapper.getId() != entity.getId())
                    && (swapper.getUnitType() == entity.getUnitType())
                    && swapper.getCrew().getCrewType() == entity.getCrew().getCrewType()) {
                menu.add(menuItem(swapper.getShortName(), "SWAP|" + swapper.getId(), enabled, listener));
            }
        }
        menu.setEnabled(enabled && menu.getItemCount() > 0);
        MenuScroller.createScrollBarsOnMenus(menu);
        return menu;
    }

    /**
     * Returns a single menu item with the given text, the given command string
     * cmd, the given enabled state, and assigned the given listener.
     */
    private static JMenuItem menuItem(String text, String cmd, boolean enabled, 
            ActionListener listener) {

        return menuItem(text, cmd, enabled, listener, Integer.MIN_VALUE);
    }

    /**
     * Returns a single menu item with the given text, the given command string
     * cmd, the given enabled state, and assigned the given listener. Also assigns
     * the given key mnemonic.
     */
    private static JMenuItem menuItem(String text, String cmd, boolean enabled, 
            ActionListener listener, int mnemonic) {

        JMenuItem result = new JMenuItem(text);
        result.setActionCommand(cmd);
        result.addActionListener(listener);
        result.setEnabled(enabled);
        if (mnemonic != Integer.MIN_VALUE) {
            result.setMnemonic(mnemonic);
        }
        return result;
    }
}


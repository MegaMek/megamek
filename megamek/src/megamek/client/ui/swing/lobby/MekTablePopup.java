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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.util.MenuScroller;
import megamek.client.ui.swing.util.ScalingPopup;
import megamek.common.*;
import megamek.common.options.OptionsConstants;

class MekTablePopup {


    static ScalingPopup mekTablePopup(ClientGUI clientGui, List<Entity> entities, 
            Entity entity, ActionListener listener, ChatLounge lobby) {
        // create a list of entities that belong to the local player or one
        // of his bots, as only those are configurable, so the popup menu really
        // reflects what can be configured
        HashSet<Entity> configurableEntities = new HashSet<>(entities);
        configurableEntities.removeIf(e -> !lobby.isEditable(e));

        ScalingPopup popup = new ScalingPopup();

        boolean canConfigureAny = configurableEntities.size() > 0;
        boolean canConfigureAll = lobby.canConfigureAll(entities);
        boolean canSeeAll = lobby.canSeeAll(entities);

        boolean isOwner = entity.getOwner().equals(clientGui.getClient().getLocalPlayer());
        boolean isBot = clientGui.getBots().get(entity.getOwner().getName()) != null;
        boolean blindDrop = clientGui.getClient().getGame().getOptions()
                .booleanOption(OptionsConstants.BASE_BLIND_DROP);
        boolean isQuirksEnabled = clientGui.getClient().getGame().getOptions()
                .booleanOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS);
        boolean optBurstMG = clientGui.getClient().getGame().getOptions()
                .booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_BURST);
        boolean optLRMHotLoad = clientGui.getClient().getGame().getOptions()
                .booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_HOTLOAD);

        boolean anyCarrier = false;
        boolean oneSelected = configurableEntities.size() == 1;
        boolean allCarried = true;
        boolean noneCarried = true;
        boolean allCapFighter = true;
        boolean allDropships = true;
        boolean allInfantry = true;
        boolean allBattleArmor = true;
        boolean allProtomechs = true;
        boolean allSameEntityType = true;
        boolean hasMGs = false;
        boolean hasLRMS = false;
        boolean anyHotLoadOn = false;
        boolean anyHotLoadOff = false;
        boolean anyRapidFireMGOn = false;
        boolean anyRapidFireMGOff = false;
        boolean allSamePlayer = true;
        Entity prevEntity = null;
        int prevOwnerId = -1;

        // find what can be done with the entities
        for (Entity en : entities) {
            if ((prevOwnerId != -1) && (en.getOwnerId() != prevOwnerId)) {
                allSamePlayer = false;
            }
            prevOwnerId = en.getOwnerId();
            prevEntity = en;
        }
        
        // Popup Actions may be
        // + actions you would want to be able to quickly apply to all or many units, knowing that they
        // will not apply to other player's units or unsuitable units
        // -- Set rapid fire, hot loading, disembark, offload
        // -- Unconfigurable or unsuitable units should be disregarded for these actions  
        // + actions that require specifically marking only a few units and should not or
        // cannot be applied to all or many units or should not be done in blind drop mode for hidden units
        // -- Join a fighter squadron, load onto another unit, individiual camo, change owner, save quirks
        // -- View unit, View BV, Edit damage, Configure, Delete
        // -- Unconfigurable or unsuitable units should be fully considered for these actions

        for (Entity en : entities) {
            if (en.getTransportId() == Entity.NONE) {
                allCarried = false;
            } else {
                noneCarried = false;
            }
            if (!en.isCapitalFighter(true) || (en instanceof FighterSquadron)) {
                allCapFighter = false;
            }
            if (entity.getLoadedUnits().size() > 0) {
                anyCarrier = true;
            }
            
            allDropships &= en.hasETypeFlag(Entity.ETYPE_DROPSHIP);
            allInfantry &= en.hasETypeFlag(Entity.ETYPE_INFANTRY);
            allBattleArmor &= en.hasETypeFlag(Entity.ETYPE_BATTLEARMOR);
            allProtomechs &= en.hasETypeFlag(Entity.ETYPE_PROTOMECH);
            if ((prevEntity != null) && !en.getClass().equals(prevEntity.getClass()) && !allInfantry) {
                allSameEntityType = false;
            }
            if (optBurstMG) {
                for (Mounted m: en.getWeaponList()) {
                    EquipmentType etype = m.getType();
                    if (etype.hasFlag(WeaponType.F_MG)) {
                        hasMGs = true;
                        anyRapidFireMGOn |= m.isRapidfire();
                        anyRapidFireMGOff |= !m.isRapidfire();
                    }
                }
            }
            if (optLRMHotLoad) {
                for (Mounted ammo: en.getAmmo()) {
                    AmmoType etype = (AmmoType)ammo.getType();
                    if (etype.hasFlag(AmmoType.F_HOTLOAD)) {
                        hasLRMS = true;
                        anyHotLoadOn |= ammo.isHotLoaded();
                        anyHotLoadOff |= !ammo.isHotLoaded();
                    }
                }
            }
        }

        // listener menu uses the following Mnemonics:
        // B, C, D, E, I, O, R, V
        JMenuItem menuItem;
        popup.add(menuItem("View...", "VIEW", oneSelected && canSeeAll, listener, KeyEvent.VK_V));
        popup.add(menuItem("Edit Damage...", "DAMAGE", oneSelected && canConfigureAny, listener, KeyEvent.VK_E));

        if (oneSelected) {
            popup.add(menuItem("Configure...", "CONFIGURE", canConfigureAny, listener, KeyEvent.VK_C));
        } else {
            popup.add(menuItem("Configure all...", "CONFIGURE_ALL", canConfigureAll, listener, KeyEvent.VK_C));
        }

        popup.add(menuItem("Set individual camo...", "INDI_CAMO", canConfigureAny, listener, KeyEvent.VK_I));
        popup.add(menuItem("Delete", "DELETE", canConfigureAny, listener, KeyEvent.VK_D));
        popup.add(randomizeMenu(clientGui, configurableEntities, prevEntity, listener, lobby));
        popup.add(changeOwnerMenu(clientGui, configurableEntities, prevEntity, listener, lobby));

        JMenu menu;

        if (!noneCarried) {
            popup.add(menuItem("Disembark All Units from Carriers", "UNLOAD", canConfigureAny && !noneCarried, listener));
        }

        if (entity.getLoadedUnits().size() > 0) {
            if (oneSelected)    {        
                popup.add(menuItem("Offload All Carried Units", "UNLOADALL", canConfigureAny && !noneCarried, listener));
            }
        }

        if (oneSelected && (entity.getLoadedUnits().size() > 0)) {
//            menuItem = new JMenuItem("Unload All Carried Units");
//            menuItem.setActionCommand("UNLOADALL");
//            menuItem.addActionListener(listener);
//            menuItem.setEnabled((isOwner || isBot));
//            popup.add(menuItem);
            JMenu subMenu = new JMenu("Offload All From...");
            for (Bay bay : entity.getTransportBays()) {
                if (bay.getLoadedUnits().size() > 0) {
//                    menuItem = new JMenuItem(
//                            "Bay # " + bay.getBayNumber() + " (" + bay.getLoadedUnits().size() + " units)");
//                    menuItem.setActionCommand("UNLOADALLFROMBAY|" + bay.getBayNumber());
//                    menuItem.addActionListener(listener);
//                    menuItem.setEnabled((isOwner || isBot));
//                    subMenu.add(menuItem);
                    String label = "Bay #" + bay.getBayNumber() + " (" + bay.getLoadedUnits().size() + " units)";
                    subMenu.add(menuItem(label, "UNLOADALLFROMBAY|" + bay.getBayNumber(), isOwner || isBot, listener));
                }
            }
            if (subMenu.getItemCount() > 0) {
                subMenu.setEnabled((isOwner || isBot));
                popup.add(subMenu);
            }
        }

        //        if (allCapFighter && noneCarried && allSamePlayer) {
        boolean fsEnabled = canConfigureAny && allCapFighter && noneCarried && allSamePlayer;
        popup.add(menuItem("Create Fighter Squadron", "SQUADRON", fsEnabled, listener));

        if (oneSelected) {
            menu = new JMenu("Swap pilots with");
            boolean canSwap = false;
            for (Entity swapper : clientGui.getClient().getGame().getEntitiesVector()) {
                if (swapper.isCapitalFighter()) {
                    continue;
                }
                // only swap your own pilots and with the same unit and crew type
                if ((swapper.getOwnerId() == entity.getOwnerId()) && (swapper.getId() != entity.getId())
                        && (swapper.getUnitType() == entity.getUnitType())
                        && swapper.getCrew().getCrewType() == entity.getCrew().getCrewType()) {
                    canSwap = true;
                    popup.add(menuItem(swapper.getShortName(), "SWAP|" + swapper.getId(), isOwner || isBot, listener));
//                    menuItem = new JMenuItem(swapper.getShortName());
//                    menuItem.setActionCommand("SWAP|" + swapper.getId());
//                    menuItem.addActionListener(listener);
//                    menuItem.setEnabled((isOwner || isBot));
//                    menu.add(menuItem);
                }
            }
            if (canSwap) {
                menu.setEnabled((isOwner || isBot) && canSwap);
                popup.add(menu);
            }
        }

        // Equipment Submenu
        if (optBurstMG || optLRMHotLoad) {
            popup.add(equipMenu(anyRapidFireMGOn, anyRapidFireMGOff, anyHotLoadOn, anyHotLoadOff, optLRMHotLoad, optBurstMG, listener));
        }

        // Quirks submenu
        if (isQuirksEnabled) {
            popup.add(quirksMenu(canSeeAll, listener));
        }

        return popup;
    }













//    private static JMenu randomizeMenu(ClientGUI clientGui, Collection<Entity> entities, 
//            Entity entity, ActionListener listener, ChatLounge lobby) {
//
//        JMenu menu;
//        // Loading Submenus
//        if (noneCarried) {
//            menu = new JMenu("Load...");
//            JMenu menuDocking = new JMenu("Dock With...");
//            JMenu menuSquadrons = new JMenu("Join...");
//            JMenu menuMounting = new JMenu("Mount...");
//            JMenu menuClamp = new JMenu("Mag Clamp...");
//            JMenu menuLoadAll = new JMenu("Load All Into");
//            boolean canLoad = false;
//            boolean allHaveMagClamp = true;
//            for (Entity b : entities) {
//                if (b.hasETypeFlag(Entity.ETYPE_BATTLEARMOR)
//                        || b.hasETypeFlag(Entity.ETYPE_PROTOMECH)) {
//                    allHaveMagClamp &= b.hasWorkingMisc(MiscType.F_MAGNETIC_CLAMP);
//                }
//            }
//            for (Entity loader : clientGui.getClient().getGame().getEntitiesVector()) {
//                // TODO don't allow capital fighters to load one another
//                // at the moment
//                if (loader.isCapitalFighter() && !(loader instanceof FighterSquadron)) {
//                    continue;
//                }
//                boolean loadable = true;
//                for (Entity en : entities) {
//                    if (!loader.canLoad(en, false)
//                            || (loader.getId() == en.getId())
//                            //TODO: support edge case where a support vee with an internal vehicle bay can load trailer internally
//                            || (loader.canTow(en.getId()))) {
//                        loadable = false;
//                        break;
//                    }
//                }
//                if (loadable) {
//                    canLoad = true;
//                    menuItem = new JMenuItem(loader.getShortName());
//                    menuItem.setActionCommand("LOAD|" + loader.getId() + ":-1");
//                    menuItem.addActionListener(listener);
//                    menuItem.setEnabled((isOwner || isBot) && noneCarried);
//                    menuLoadAll.add(menuItem);
//                    JMenu subMenu = new JMenu(loader.getShortName());
//                    if ((loader instanceof FighterSquadron) && allCapFighter) {
//                        menuItem = new JMenuItem("Join " + loader.getShortName());
//                        menuItem.setActionCommand("LOAD|" + loader.getId() + ":-1");
//                        menuItem.addActionListener(listener);
//                        menuItem.setEnabled((isOwner || isBot) && noneCarried);
//                        menuSquadrons.add(menuItem);
//                    } else if ((loader instanceof Jumpship) && allDropships) {
//                        int freeCollars = 0;
//                        for (Transporter t : loader.getTransports()) {
//                            if (t instanceof DockingCollar) {
//                                freeCollars += t.getUnused();
//                            }
//                        }
//                        menuItem = new JMenuItem(
//                                loader.getShortName() + " (Free Collars: " + freeCollars + ")");
//                        menuItem.setActionCommand("LOAD|" + loader.getId() + ":-1");
//                        menuItem.addActionListener(listener);
//                        menuItem.setEnabled((isOwner || isBot) && noneCarried);
//                        menuDocking.add(menuItem);
//                    } else if (allBattleArmor && allHaveMagClamp && !loader.isOmni()
//                            // Only load magclamps if applicable
//                            && loader.hasUnloadedClampMount()
//                            // Only choose MagClamps as last option
//                            && (loader.getUnused(entities.get(0)) < 2)) {
//                        for (Transporter t : loader.getTransports()) {
//                            if ((t instanceof ClampMountMech) || (t instanceof ClampMountTank)) {
//                                menuItem = new JMenuItem("Onto " + loader.getShortName());
//                                menuItem.setActionCommand("LOAD|" + loader.getId() + ":-1");
//                                menuItem.addActionListener(listener);
//                                menuItem.setEnabled((isOwner || isBot) && noneCarried);
//                                menuClamp.add(menuItem);
//                            }
//                        }
//                    } else if (allProtomechs && allHaveMagClamp
//                            && loader.hasETypeFlag(Entity.ETYPE_MECH)) {
//                        Transporter front = null;
//                        Transporter rear = null;
//                        for (Transporter t : loader.getTransports()) {
//                            if (t instanceof ProtomechClampMount) {
//                                if (((ProtomechClampMount) t).isRear()) {
//                                    rear = t;
//                                } else {
//                                    front = t;
//                                }
//                            }
//                        }
//                        Entity en = entities.get(0);
//                        if ((front != null) && front.canLoad(en)
//                                && ((en.getWeightClass() < EntityWeightClass.WEIGHT_SUPER_HEAVY)
//                                        || (rear == null) || rear.getLoadedUnits().isEmpty())) {
//                            menuItem = new JMenuItem("Onto Front");
//                            menuItem.setActionCommand("LOAD|" + loader.getId() + ":0");
//                            menuItem.addActionListener(listener);
//                            menuItem.setEnabled((isOwner || isBot) && noneCarried);
//                            subMenu.add(menuItem);
//                        }
//                        boolean frontUltra = (front != null)
//                                && front.getLoadedUnits().stream()
//                                .anyMatch(l -> l.getWeightClass() == EntityWeightClass.WEIGHT_SUPER_HEAVY);
//                        if ((rear != null) && rear.canLoad(en) && !frontUltra) {
//                            menuItem = new JMenuItem("Onto Rear");
//                            menuItem.setActionCommand("LOAD|" + loader.getId() + ":1");
//                            menuItem.addActionListener(listener);
//                            menuItem.setEnabled((isOwner || isBot) && noneCarried);
//                            subMenu.add(menuItem);
//                        }
//                        if (subMenu.getItemCount() > 0) {
//                            menuClamp.add(subMenu);
//                        }
//                    } else if (allInfantry) {
//                        menuItem = new JMenuItem(loader.getShortName());
//                        menuItem.setActionCommand("LOAD|" + loader.getId() + ":-1");
//                        menuItem.addActionListener(listener);
//                        menuItem.setEnabled((isOwner || isBot) && noneCarried);
//                        menuMounting.add(menuItem);
//                    }
//                    Entity en = entities.get(0);
//                    if (allSameEntityType && !allDropships) {
//                        for (Transporter t : loader.getTransports()) {
//                            if (t.canLoad(en)) {
//                                if (t instanceof Bay) {
//                                    Bay bay = (Bay) t;
//                                    menuItem = new JMenuItem("Into Bay #" + bay.getBayNumber() + " (Free "
//                                            + "Slots: "
//                                            + (int) loader.getBayById(bay.getBayNumber()).getUnusedSlots()
//                                            + loader.getBayById(bay.getBayNumber()).getDefaultSlotDescription()
//                                            + ")");
//                                    menuItem.setActionCommand(
//                                            "LOAD|" + loader.getId() + ":" + bay.getBayNumber());
//                                    /*
//                                     * } else { menuItem = new
//                                     * JMenuItem(
//                                     * t.getClass().getName()+
//                                     * "Transporter" );
//                                     * menuItem.setActionCommand("LOAD|"
//                                     * + loader.getId() + ":-1"); }
//                                     */
//                                    menuItem.addActionListener(listener);
//                                    menuItem.setEnabled((isOwner || isBot) && noneCarried);
//                                    subMenu.add(menuItem);
//                                }
//                            }
//                        }
//                        if (subMenu.getMenuComponentCount() > 0) {
//                            menu.add(subMenu);
//                        }
//                    }
//                }
//            }
//            if (canLoad) {
//                if (menu.getMenuComponentCount() > 0) {
//                    menu.setEnabled((isOwner || isBot) && noneCarried);
//                    MenuScroller.createScrollBarsOnMenus(menu);
//                    popup.add(menu);
//                }
//                if (menuDocking.getMenuComponentCount() > 0) {
//                    menuDocking.setEnabled((isOwner || isBot) && noneCarried);
//                    MenuScroller.createScrollBarsOnMenus(menuDocking);
//                    popup.add(menuDocking);
//                }
//                if (menuSquadrons.getMenuComponentCount() > 0) {
//                    menuSquadrons.setEnabled((isOwner || isBot) && noneCarried);
//                    MenuScroller.createScrollBarsOnMenus(menuSquadrons);
//                    popup.add(menuSquadrons);
//                }
//                if (menuMounting.getMenuComponentCount() > 0) {
//                    menuMounting.setEnabled((isOwner || isBot) && noneCarried);
//                    MenuScroller.createScrollBarsOnMenus(menuMounting);
//                    popup.add(menuMounting);
//                }
//                if (menuClamp.getMenuComponentCount() > 0) {
//                    menuClamp.setEnabled((isOwner || isBot) && noneCarried);
//                    MenuScroller.createScrollBarsOnMenus(menuClamp);
//                    popup.add(menuClamp);
//                }
//                boolean hasMounting = menuMounting.getMenuComponentCount() > 0;
//                boolean hasSquadrons = menuSquadrons.getMenuComponentCount() > 0;
//                boolean hasDocking = menuDocking.getMenuComponentCount() > 0;
//                boolean hasLoad = menu.getMenuComponentCount() > 0;
//                boolean hasClamp = menuClamp.getMenuComponentCount() > 0;
//                if ((menuLoadAll.getMenuComponentCount() > 0)
//                        && !(hasMounting || hasSquadrons || hasDocking || hasLoad || hasClamp)) {
//                    menuLoadAll.setEnabled((isOwner || isBot) && noneCarried);
//                    MenuScroller.createScrollBarsOnMenus(menuLoadAll);
//                    popup.add(menuLoadAll);
//                }
//            }
//        }
//    }




    private static JMenu randomizeMenu(ClientGUI clientGui, Collection<Entity> entities, 
            Entity entity, ActionListener listener, ChatLounge lobby) {
        // listener menu uses the following Mnemonic Keys:
        // C, N, S

        boolean enabled = lobby.canConfigureAny(entities);
        JMenu menu = new JMenu("Randomize");
        menu.setEnabled(enabled);
        menu.setMnemonic(KeyEvent.VK_R);

        menu.add(menuItem("Name", ChatLounge.NAME_COMMAND, enabled, listener, KeyEvent.VK_N));
        menu.add(menuItem("Callsign", ChatLounge.CALLSIGN_COMMAND, enabled, listener, KeyEvent.VK_C));
        menu.add(menuItem("Skills", "SKILLS", enabled, listener, KeyEvent.VK_S));
        return menu;
    }

    private static JMenu changeOwnerMenu(ClientGUI clientGui, Collection<Entity> entities, 
            Entity entity, ActionListener listener, ChatLounge lobby) {

        boolean enabled = lobby.canConfigureAny(entities);
        JMenu menu = new JMenu(Messages.getString("ChatLounge.ChangeOwner"));
        menu.setEnabled(enabled);
        menu.setMnemonic(KeyEvent.VK_O);

        for (IPlayer player: clientGui.getClient().getGame().getPlayersVector()) {
            menu.add(menuItem(player.getName(), "CHANGE_OWNER|" + player.getId(), enabled, listener));
        }
        return menu;
    }

    private static JMenu quirksMenu(boolean enabled, ActionListener listener) {

        JMenu menu = new JMenu(Messages.getString("ChatLounge.popup.quirks"));
        menu.setEnabled(enabled);
        menu.add(menuItem("Save Quirks for Chassis", "SAVE_QUIRKS_ALL", enabled, listener));
        menu.add(menuItem("Save Quirks for Chassis/Model", "SAVE_QUIRKS_MODEL", enabled, listener));
        return menu;
    }

    private static JMenu equipMenu(boolean anyRFOn, boolean anyRFOff, boolean anyHLOn, boolean anyHLOff,
            boolean optHL, boolean optRF, ActionListener listener) {

        JMenu menu = new JMenu(Messages.getString("ChatLounge.Equipment"));
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


    private static JMenuItem menuItem(String text, String cmd, boolean enabled, 
            ActionListener listener) {

        return menuItem(text, cmd, enabled, listener, Integer.MIN_VALUE);
    }

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

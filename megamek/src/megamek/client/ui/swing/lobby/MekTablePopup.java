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
            Entity e2, ActionListener listener, ChatLounge lobby) {
        
        if (entities.isEmpty()) {
            return new ScalingPopup();
        }
        
        // create a list of entities that belong to the local player or one
        // of his bots, as only those are configurable, so the popup menu really
        // reflects what can be configured
        HashSet<Entity> configurableEntities = new HashSet<>(entities);
        configurableEntities.removeIf(e -> !lobby.isEditable(e));

        

        boolean canConfigureAny = configurableEntities.size() > 0;
        boolean canConfigureAll = entities.size() == configurableEntities.size();
        boolean canConfigureDeployAll = lobby.canConfigureDeploymentAll(entities);
        boolean canSeeAll = lobby.canSeeAll(entities);

//        boolean isOwner = entity.getOwner().equals(clientGui.getClient().getLocalPlayer());
//        boolean isBot = clientGui.getBots().get(entity.getOwner().getName()) != null;
        boolean blindDrop = clientGui.getClient().getGame().getOptions()
                .booleanOption(OptionsConstants.BASE_BLIND_DROP);
        boolean isQuirksEnabled = clientGui.getClient().getGame().getOptions()
                .booleanOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS);
        boolean optBurstMG = clientGui.getClient().getGame().getOptions()
                .booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_BURST);
        boolean optLRMHotLoad = clientGui.getClient().getGame().getOptions()
                .booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_HOTLOAD);

        boolean oneSelected = configurableEntities.size() == 1;
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
            if (en.hasETypeFlag(Entity.ETYPE_BATTLEARMOR)
                    || en.hasETypeFlag(Entity.ETYPE_PROTOMECH)) {
                allHaveMagClamp &= en.hasWorkingMisc(MiscType.F_MAGNETIC_CLAMP);
            }

        }
        
        boolean enemiesSelected = entities.size() != configurableEntities.size();
        
        
        // Popup Actions may be
        // + actions you would want to be able to quickly apply to all or many units, knowing that they
        // will not apply to other player's units or unsuitable units
        // -- Set rapid fire, hot loading, disembark, offload, individ camo
        // -- Unconfigurable or unsuitable units should be disregarded for these actions  
        // + actions that require specifically marking only a few units and should not or
        // cannot be applied to all or many units or should not be done in blind drop mode for hidden units
        // -- Join a fighter squadron, load onto another unit, change owner, save quirks
        // -- View unit, View BV, Edit damage, Configure, Delete
        // -- Unconfigurable or unsuitable units should be fully considered for these actions

        for (Entity en: entities) {
            if (!en.isCapitalFighter(true) || (en instanceof FighterSquadron)) {
                allCapFighter = false;
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
        ScalingPopup popup = new ScalingPopup();
        
        popup.add(menuItem("View...", "VIEW", oneSelected && canSeeAll, listener, KeyEvent.VK_V));
        popup.add(menuItem("Edit Damage...", "DAMAGE", oneSelected && canConfigureAny, listener, KeyEvent.VK_E));

        if (oneSelected) {
            popup.add(menuItem("Configure...", "CONFIGURE", canConfigureAny, listener, KeyEvent.VK_C));
        } else {
            popup.add(menuItem("Configure all...", "CONFIGURE_ALL", canConfigureDeployAll, listener, KeyEvent.VK_C));
        }

        popup.add(menuItem("Set individual camo...", "INDI_CAMO", canConfigureAny, listener, KeyEvent.VK_I));
        popup.add(menuItem("Delete", "DELETE", canConfigureAll, listener, KeyEvent.VK_D));
        popup.add(randomizeMenu(canConfigureAny, listener));
        popup.add(changeOwnerMenu(canConfigureAny, clientGui, listener));

        String disembark = "Disembark / Leave";
        if (!oneSelected) {
            disembark = "Disembark All Units from Carriers";
        }
        popup.add(menuItem(disembark, "UNLOAD", canConfigureAny && !noneEmbarked, listener));
        popup.add(menuItem("Offload All Carried Units", "UNLOADALL", canConfigureAny && anyCarrier, listener));
        popup.add(offloadBayMenu(oneSelected && anyCarrier && canConfigureAny, entities.get(0), listener));
        
        boolean fsEnabled = canConfigureAny && allCapFighter && noneEmbarked && allSamePlayer;
        popup.add(squadronMenu(clientGui, fsEnabled, listener, entities));

//        if (oneSelected && anyCarrier && canConfigureAny) {
//            Entity entity = entities.get(0);
//            boolean enabled = oneSelected && anyCarrier && canConfigureAny;
//            JMenu subMenu = new JMenu("Offload All From...");
//            for (Bay bay : entity.getTransportBays()) {
//                if (bay.getLoadedUnits().size() > 0) {
////                    menuItem = new JMenuItem(
////                            "Bay # " + bay.getBayNumber() + " (" + bay.getLoadedUnits().size() + " units)");
////                    menuItem.setActionCommand("UNLOADALLFROMBAY|" + bay.getBayNumber());
////                    menuItem.addActionListener(listener);
////                    menuItem.setEnabled((isOwner || isBot));
////                    subMenu.add(menuItem);
//                    String label = "Bay #" + bay.getBayNumber() + " (" + bay.getLoadedUnits().size() + " units)";
//                    subMenu.add(menuItem(label, "UNLOADALLFROMBAY|" + bay.getBayNumber(), enabled, listener));
//                }
//            }
//            if (subMenu.getItemCount() > 0) {
//                subMenu.setEnabled(enabled);
//                popup.add(subMenu);
//            }
//        }

        //        if (allCapFighter && noneCarried && allSamePlayer) {
//        popup.add(menuItem("Create Fighter Squadron", "SQUADRON", fsEnabled, listener));

        popup.add(swapPilotMenu(oneSelected && canConfigureAny, entities.get(0), clientGui, listener));

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















    

    /**
     * Returns the "Join" submenu, allowing to assign units to or
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


//private static JMenu randomizeMenu(ClientGUI clientGui, Collection<Entity> entities, 
//Entity entity, ActionListener listener, ChatLounge lobby) {
//
//JMenu menu;
//// Loading Submenus
//if (noneCarried) {
//menu = new JMenu("Load...");
//JMenu menuDocking = new JMenu("Dock With...");
//JMenu menuSquadrons = new JMenu("Join...");
//JMenu menuMounting = new JMenu("Mount...");
//JMenu menuClamp = new JMenu("Mag Clamp...");
//JMenu menuLoadAll = new JMenu("Load All Into");
//boolean canLoad = false;
//boolean allHaveMagClamp = true;
//for (Entity b : entities) {
//  if (b.hasETypeFlag(Entity.ETYPE_BATTLEARMOR)
//          || b.hasETypeFlag(Entity.ETYPE_PROTOMECH)) {
//      allHaveMagClamp &= b.hasWorkingMisc(MiscType.F_MAGNETIC_CLAMP);
//  }
//}
//for (Entity loader : clientGui.getClient().getGame().getEntitiesVector()) {
//  // TODO don't allow capital fighters to load one another
//  // at the moment
//  if (loader.isCapitalFighter() && !(loader instanceof FighterSquadron)) {
//      continue;
//  }
//  boolean loadable = true;
//  for (Entity en : entities) {
//      if (!loader.canLoad(en, false)
//              || (loader.getId() == en.getId())
//              //TODO: support edge case where a support vee with an internal vehicle bay can load trailer internally
//              || (loader.canTow(en.getId()))) {
//          loadable = false;
//          break;
//      }
//  }
//  if (loadable) {
//      canLoad = true;
//      menuItem = new JMenuItem(loader.getShortName());
//      menuItem.setActionCommand("LOAD|" + loader.getId() + ":-1");
//      menuItem.addActionListener(listener);
//      menuItem.setEnabled((isOwner || isBot) && noneCarried);
//      menuLoadAll.add(menuItem);
//      JMenu subMenu = new JMenu(loader.getShortName());
//      if ((loader instanceof FighterSquadron) && allCapFighter) {
//          menuItem = new JMenuItem("Join " + loader.getShortName());
//          menuItem.setActionCommand("LOAD|" + loader.getId() + ":-1");
//          menuItem.addActionListener(listener);
//          menuItem.setEnabled((isOwner || isBot) && noneCarried);
//          menuSquadrons.add(menuItem);
//      } else if ((loader instanceof Jumpship) && allDropships) {
//          int freeCollars = 0;
//          for (Transporter t : loader.getTransports()) {
//              if (t instanceof DockingCollar) {
//                  freeCollars += t.getUnused();
//              }
//          }
//          menuItem = new JMenuItem(
//                  loader.getShortName() + " (Free Collars: " + freeCollars + ")");
//          menuItem.setActionCommand("LOAD|" + loader.getId() + ":-1");
//          menuItem.addActionListener(listener);
//          menuItem.setEnabled((isOwner || isBot) && noneCarried);
//          menuDocking.add(menuItem);
//      } else if (allBattleArmor && allHaveMagClamp && !loader.isOmni()
//              // Only load magclamps if applicable
//              && loader.hasUnloadedClampMount()
//              // Only choose MagClamps as last option
//              && (loader.getUnused(entities.get(0)) < 2)) {
//          for (Transporter t : loader.getTransports()) {
//              if ((t instanceof ClampMountMech) || (t instanceof ClampMountTank)) {
//                  menuItem = new JMenuItem("Onto " + loader.getShortName());
//                  menuItem.setActionCommand("LOAD|" + loader.getId() + ":-1");
//                  menuItem.addActionListener(listener);
//                  menuItem.setEnabled((isOwner || isBot) && noneCarried);
//                  menuClamp.add(menuItem);
//              }
//          }
//      } else if (allProtomechs && allHaveMagClamp
//              && loader.hasETypeFlag(Entity.ETYPE_MECH)) {
//          Transporter front = null;
//          Transporter rear = null;
//          for (Transporter t : loader.getTransports()) {
//              if (t instanceof ProtomechClampMount) {
//                  if (((ProtomechClampMount) t).isRear()) {
//                      rear = t;
//                  } else {
//                      front = t;
//                  }
//              }
//          }
//          Entity en = entities.get(0);
//          if ((front != null) && front.canLoad(en)
//                  && ((en.getWeightClass() < EntityWeightClass.WEIGHT_SUPER_HEAVY)
//                          || (rear == null) || rear.getLoadedUnits().isEmpty())) {
//              menuItem = new JMenuItem("Onto Front");
//              menuItem.setActionCommand("LOAD|" + loader.getId() + ":0");
//              menuItem.addActionListener(listener);
//              menuItem.setEnabled((isOwner || isBot) && noneCarried);
//              subMenu.add(menuItem);
//          }
//          boolean frontUltra = (front != null)
//                  && front.getLoadedUnits().stream()
//                  .anyMatch(l -> l.getWeightClass() == EntityWeightClass.WEIGHT_SUPER_HEAVY);
//          if ((rear != null) && rear.canLoad(en) && !frontUltra) {
//              menuItem = new JMenuItem("Onto Rear");
//              menuItem.setActionCommand("LOAD|" + loader.getId() + ":1");
//              menuItem.addActionListener(listener);
//              menuItem.setEnabled((isOwner || isBot) && noneCarried);
//              subMenu.add(menuItem);
//          }
//          if (subMenu.getItemCount() > 0) {
//              menuClamp.add(subMenu);
//          }
//      } else if (allInfantry) {
//          menuItem = new JMenuItem(loader.getShortName());
//          menuItem.setActionCommand("LOAD|" + loader.getId() + ":-1");
//          menuItem.addActionListener(listener);
//          menuItem.setEnabled((isOwner || isBot) && noneCarried);
//          menuMounting.add(menuItem);
//      }
//      Entity en = entities.get(0);
//      if (allSameEntityType && !allDropships) {
//          for (Transporter t : loader.getTransports()) {
//              if (t.canLoad(en)) {
//                  if (t instanceof Bay) {
//                      Bay bay = (Bay) t;
//                      menuItem = new JMenuItem("Into Bay #" + bay.getBayNumber() + " (Free "
//                              + "Slots: "
//                              + (int) loader.getBayById(bay.getBayNumber()).getUnusedSlots()
//                              + loader.getBayById(bay.getBayNumber()).getDefaultSlotDescription()
//                              + ")");
//                      menuItem.setActionCommand(
//                              "LOAD|" + loader.getId() + ":" + bay.getBayNumber());
//                      /*
//                       * } else { menuItem = new
//                       * JMenuItem(
//                       * t.getClass().getName()+
//                       * "Transporter" );
//                       * menuItem.setActionCommand("LOAD|"
//                       * + loader.getId() + ":-1"); }
//                       */
//                      menuItem.addActionListener(listener);
//                      menuItem.setEnabled((isOwner || isBot) && noneCarried);
//                      subMenu.add(menuItem);
//                  }
//              }
//          }
//          if (subMenu.getMenuComponentCount() > 0) {
//              menu.add(subMenu);
//          }
//      }
//  }
//}
//if (canLoad) {
//  if (menu.getMenuComponentCount() > 0) {
//      menu.setEnabled((isOwner || isBot) && noneCarried);
//      MenuScroller.createScrollBarsOnMenus(menu);
//      popup.add(menu);
//  }
//  if (menuDocking.getMenuComponentCount() > 0) {
//      menuDocking.setEnabled((isOwner || isBot) && noneCarried);
//      MenuScroller.createScrollBarsOnMenus(menuDocking);
//      popup.add(menuDocking);
//  }
//  if (menuSquadrons.getMenuComponentCount() > 0) {
//      menuSquadrons.setEnabled((isOwner || isBot) && noneCarried);
//      MenuScroller.createScrollBarsOnMenus(menuSquadrons);
//      popup.add(menuSquadrons);
//  }
//  if (menuMounting.getMenuComponentCount() > 0) {
//      menuMounting.setEnabled((isOwner || isBot) && noneCarried);
//      MenuScroller.createScrollBarsOnMenus(menuMounting);
//      popup.add(menuMounting);
//  }
//  if (menuClamp.getMenuComponentCount() > 0) {
//      menuClamp.setEnabled((isOwner || isBot) && noneCarried);
//      MenuScroller.createScrollBarsOnMenus(menuClamp);
//      popup.add(menuClamp);
//  }
//  boolean hasMounting = menuMounting.getMenuComponentCount() > 0;
//  boolean hasSquadrons = menuSquadrons.getMenuComponentCount() > 0;
//  boolean hasDocking = menuDocking.getMenuComponentCount() > 0;
//  boolean hasLoad = menu.getMenuComponentCount() > 0;
//  boolean hasClamp = menuClamp.getMenuComponentCount() > 0;
//  if ((menuLoadAll.getMenuComponentCount() > 0)
//          && !(hasMounting || hasSquadrons || hasDocking || hasLoad || hasClamp)) {
//      menuLoadAll.setEnabled((isOwner || isBot) && noneCarried);
//      MenuScroller.createScrollBarsOnMenus(menuLoadAll);
//      popup.add(menuLoadAll);
//  }
//}
//}
//}
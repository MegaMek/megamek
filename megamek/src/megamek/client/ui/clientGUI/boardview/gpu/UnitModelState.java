/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import megamek.client.ui.tileset.EquipmentModelPolicy;
import megamek.client.ui.tileset.UnitModelEquipment;
import megamek.client.ui.util.PlayerColour;
import megamek.common.alphaStrike.conversion.ASConverter;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.battlefieldSupport.BFSAssetType;
import megamek.common.battlefieldSupport.BattlefieldSupportAsset;
import megamek.common.battlefieldSupport.OverlayStyle;
import megamek.common.battlefieldSupport.StripeDirection;
import megamek.common.icons.Camouflage;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.FighterSquadron;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import megamek.common.units.ProneCause;
import megamek.common.units.ProtoMek;
import megamek.common.units.QuadMek;
import megamek.common.units.Tank;
import megamek.common.units.TripodMek;

/** Swing-owned capture after visibility filtering. Structure changes are independent of pose and damage changes. */
record UnitModelState(Structure structure, Appearance appearance, Pose pose) {
    record Structure(EntityMovementMode movement, List<UnitModelEquipment.Mount> equipment, List<Integer> members,
          int activeTroopers, boolean externalSearchlight, MekAnatomy anatomy, BodyForm bodyForm) {
        Structure(EntityMovementMode movement, List<UnitModelEquipment.Mount> equipment, List<Integer> members,
              int activeTroopers, boolean externalSearchlight, MekAnatomy anatomy) {
            this(movement, equipment, members, activeTroopers, externalSearchlight, anatomy, null);
        }
        Structure(EntityMovementMode movement, List<UnitModelEquipment.Mount> equipment, List<Integer> members,
              int activeTroopers, boolean externalSearchlight) {
            this(movement, equipment, members, activeTroopers, externalSearchlight, null);
        }

        Structure {
            equipment = List.copyOf(equipment);
            members = List.copyOf(members);
        }
    }

    /** Family-only structure; resting proportions never depend on transient gameplay height or posture. */
    record BodyForm(String configuration, int size, int turrets, List<FlightMember> fighters) {
        BodyForm(String configuration, int size, int turrets) {
            this(configuration, size, turrets, List.of());
        }

        BodyForm {
            fighters = List.copyOf(fighters);
        }
    }

    /** A squadron borrows actual visible member state; it never creates additional game entities. */
    record FlightMember(int id, Structure structure) { }

    record MekAnatomy(String configuration, List<String> hands, List<String> lowerArms, int size, int weightClass) {
        MekAnatomy(String configuration, List<String> hands, List<String> lowerArms) {
            this(configuration, hands, lowerArms, 3, EntityWeightClass.WEIGHT_HEAVY);
        }

        MekAnatomy {
            hands = List.copyOf(hands);
            lowerArms = List.copyOf(lowerArms);
        }

        boolean superHeavy() {
            return weightClass == EntityWeightClass.WEIGHT_SUPER_HEAVY;
        }
    }

    record Appearance(Set<Integer> inoperableEquipment, boolean searchlightOn, Camo camo,
          Map<Integer, Appearance> fighters) {
        Appearance(Set<Integer> inoperableEquipment, boolean searchlightOn, Camo camo) {
            this(inoperableEquipment, searchlightOn, camo, Map.of());
        }

        Appearance {
            inoperableEquipment = Set.copyOf(inoperableEquipment);
            fighters = Map.copyOf(fighters);
        }
    }

    /** Resolved pixels cross the Swing boundary; neither the icon nor an AWT image reaches the renderer. */
    record Camo(String category, String filename, int rotation, int scale, int rgb, BoardScene.Pixels image,
          Marker marker) { }

    record Marker(int rgb, StripeDirection direction, OverlayStyle style, BoardScene.Pixels image) { }

    record Pose(ProneCause proneCause, int facing, int secondaryFacing, megamek.common.units.UnitLocation.Form form, boolean dead) {
        Pose(ProneCause proneCause, int facing, int secondaryFacing, megamek.common.units.UnitLocation.Form form) {
            this(proneCause, facing, secondaryFacing, form, false);
        }
        Pose(ProneCause proneCause, int facing, int secondaryFacing) {
            this(proneCause, facing, secondaryFacing, null);
        }
    }

    static UnitModelState capture(Entity entity) {
        List<UnitModelEquipment.Mount> equipment = new ArrayList<>();
        Set<Integer> inoperable = new TreeSet<>();
        for (var mount : entity.getEquipment()) {
            var visual = UnitModelEquipment.describe(entity, mount);
            if (visual.policy() != EquipmentModelPolicy.NONE) {
                equipment.add(visual);
                if (mount.isInoperable()) {
                    inoperable.add(mount.getEquipmentNum());
                }
            }
        }
        List<Integer> members = new ArrayList<>();
        int troopers = 0;
        if (entity instanceof BattleArmor armor) {
            for (int location = 1; location < armor.locations(); location++) {
                if (armor.getInternal(location) > 0) {
                    members.add(location);
                }
            }
            troopers = members.size();
        } else if (entity instanceof Infantry infantry) {
            troopers = Math.max(0, infantry.getActiveTroopers());
        } else if (entity instanceof BattlefieldSupportAsset asset
              && (asset.getAssetType() == BFSAssetType.CONV_INFANTRY || asset.getAssetType() == BFSAssetType.BATTLE_ARMOR)) {
            // A support card has no personnel locations. Its family marker is one representative figure.
            troopers = 1;
            members.add(1);
        }
        Camouflage camo = entity.getCamouflageOrElseOwners();
        Marker marker = entity instanceof BattlefieldSupportAsset && camo != null && camo.getOverlayStyle().isVisible()
              ? new Marker(camo.getOverlayColor().getRGB(), camo.getOverlayDirection(), camo.getOverlayStyle(), null) : null;
        Camo appearance = camo == null ? null : new Camo(camo.getCategory(), camo.getFilename(),
              camo.getRotationAngle(), camo.getScale(),
              camo.isColourCamouflage() ? PlayerColour.parseFromString(camo.getFilename()).getHex() : 0xFFFFFF, null, marker);
        MekAnatomy anatomy = entity instanceof Mek mek ? new MekAnatomy(mek instanceof QuadMek ? "quad"
              : mek instanceof TripodMek ? "tripod" : "biped",
              UnitModelEquipment.armsWith(mek, Mek.ACTUATOR_HAND), UnitModelEquipment.armsWith(mek, Mek.ACTUATOR_LOWER_ARM),
              ASConverter.sizeFor(mek), mek.getWeightClass()) : null;
        List<FlightMember> fighters = new ArrayList<>();
        Map<Integer, Appearance> fighterAppearances = new HashMap<>();
        if (entity instanceof FighterSquadron squadron && entity.getGame() != null) {
            for (var fighter : squadron.getActiveSubEntities()) {
                var member = capture(fighter);
                fighters.add(new FlightMember(fighter.getId(), member.structure()));
                fighterAppearances.put(fighter.getId(), member.appearance());
            }
        }
        var movement = entity instanceof megamek.common.units.QuadVee ? EntityMovementMode.QUAD : entity.getMovementMode();
        var form = new BodyForm(entity instanceof ProtoMek proto ? (proto.isQuad() ? "quad-proto"
              : proto.isGlider() ? "glider-proto" : "proto") : movement.name(),
              anatomy != null ? anatomy.size() : ASConverter.canConvert(entity) ? ASConverter.sizeFor(entity) : 2,
              entity instanceof Tank tank ? tank.getTurretCount() : 0, fighters);
        // Meks/vehicles receive a generic rules searchlight at game start; that is not an authored lamp housing.
        // The design quirk still describes hardware when optional quirk effects are disabled. Mounted lamps
        // already have their own equipment entries, and explicitly added lights on other families remain visible.
        boolean externalLamp = entity.hasExternalSearchlight() && (!entity.getsAutoExternalSearchlight()
              || entity.getQuirks().booleanOption(OptionsConstants.QUIRK_POS_SEARCHLIGHT));
        return new UnitModelState(new Structure(movement, equipment, members, troopers,
              externalLamp, anatomy, form),
              new Appearance(inoperable, entity.isUsingSearchlight(), appearance, fighterAppearances),
              new Pose(entity instanceof Mek ? entity.getProneCause() : ProneCause.NONE,
                    entity.getFacing(), entity.getSecondaryFacing(), megamek.common.units.UnitLocation.Form.capture(entity),
                    entity.isDestroyed() || entity.isDoomed()));
    }
}

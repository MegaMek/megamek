/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Authored joint roles in one body/formation member. Definitions contain no mutable playback or GPU state. */
record UnitRig(String family, String type, String container, Map<String, String> joints,
          List<UnitModelDescriptor.Emitter> emitters, List<UnitModelDescriptor.LandingSupport> landingSupports,
          Map<String, String> legBends) {
    static final String[][] LEGS = { { "leftLeg", "leftShin", "leftFoot" }, { "rightLeg", "rightShin", "rightFoot" },
          { "CL", "CLShin", "CLFoot" }, { "FLL", "FLLShin", "FLLFoot" }, { "FRL", "FRLShin", "FRLFoot" },
          { "RLL", "RLLShin", "RLLFoot" }, { "RRL", "RRLShin", "RRLFoot" },
          { "leg0", "leg0Shin", "leg0Foot" }, { "leg1", "leg1Shin", "leg1Foot" },
          { "leg2", "leg2Shin", "leg2Foot" }, { "leg3", "leg3Shin", "leg3Foot" } };

    UnitRig {
        joints = Map.copyOf(joints);
        emitters = List.copyOf(emitters);
        landingSupports = List.copyOf(landingSupports);
        legBends = Map.copyOf(legBends);
    }

    UnitRig(String family, String type, String container, Map<String, String> joints,
          List<UnitModelDescriptor.Emitter> emitters, List<UnitModelDescriptor.LandingSupport> landingSupports) {
        this(family, type, container, joints, emitters, landingSupports, Map.of());
    }

    UnitRig(UnitModelDescriptor descriptor) {
        this(descriptor.family(), descriptor.rig(), null, descriptor.joints(), descriptor.emitters(), descriptor.landingSupports(),
              descriptor.legBends());
    }

    /** A formation adds a placement node; squadrons additionally namespace their members' authored node IDs. */
    UnitRig inside(String placement, String nodePrefix) {
        return new UnitRig(family, type, placement, joints.entrySet().stream().collect(Collectors.toMap(
              Map.Entry::getKey, entry -> nodePrefix + entry.getValue())), emitters.stream().map(emitter ->
                    new UnitModelDescriptor.Emitter(emitter.id(), nodePrefix + emitter.node(), emitter.position(),
                          emitter.direction(), emitter.role(), emitter.effect())).toList(),
              landingSupports.stream().map(support -> support.prefixed(nodePrefix)).toList(), legBends);
    }

    /** The authored knee branch, independent of travel direction. Omitted roles retain the conventional gait. */
    float kneeDirection(String leg) { return "reverse".equals(legBends.get(leg)) ? -1 : 1; }

    boolean trooper() {
        return "trooper-v1".equals(type);
    }

    boolean mek() {
        return "biped-v1".equals(type) || "quad-v1".equals(type) || "tripod-v1".equals(type);
    }

    String kickingLeg(int location) {
        if ("quad-v1".equals(type)) {
            return switch (location) {
                case megamek.common.units.Mek.LOC_LEFT_ARM -> "FLL";
                case megamek.common.units.Mek.LOC_RIGHT_ARM -> "FRL";
                case megamek.common.units.Mek.LOC_LEFT_LEG -> "RLL";
                default -> "RRL";
            };
        }
        return switch (location) {
            // The rules may identify the center location; visually kick with a side leg for balance.
            case megamek.common.units.Mek.LOC_CENTER_LEG -> "leftLeg";
            case megamek.common.units.Mek.LOC_LEFT_LEG -> "leftLeg";
            default -> "rightLeg";
        };
    }

    boolean transport() {
        return "infantry-transport".equals(family);
    }
}

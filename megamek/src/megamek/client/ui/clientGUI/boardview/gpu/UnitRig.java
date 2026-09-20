/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Authored joint roles in one body/formation member. Definitions contain no mutable playback or GPU state. */
record UnitRig(String family, String type, String container, Map<String, String> joints,
          List<UnitModelDescriptor.Emitter> emitters, List<UnitModelDescriptor.LandingSupport> landingSupports) {
    UnitRig {
        joints = Map.copyOf(joints);
        emitters = List.copyOf(emitters);
        landingSupports = List.copyOf(landingSupports);
    }

    UnitRig(UnitModelDescriptor descriptor) {
        this(descriptor.family(), descriptor.rig(), null, descriptor.joints(), descriptor.emitters(), descriptor.landingSupports());
    }

    /** A formation adds a placement node; squadrons additionally namespace their members' authored node IDs. */
    UnitRig inside(String placement, String nodePrefix) {
        return new UnitRig(family, type, placement, joints.entrySet().stream().collect(Collectors.toMap(
              Map.Entry::getKey, entry -> nodePrefix + entry.getValue())), emitters.stream().map(emitter ->
                    new UnitModelDescriptor.Emitter(emitter.id(), nodePrefix + emitter.node(), emitter.position(),
                          emitter.direction(), emitter.role(), emitter.effect())).toList(),
              landingSupports.stream().map(support -> support.prefixed(nodePrefix)).toList());
    }

    boolean trooper() {
        return "trooper-v1".equals(type);
    }

    boolean transport() {
        return "infantry-transport".equals(family);
    }
}

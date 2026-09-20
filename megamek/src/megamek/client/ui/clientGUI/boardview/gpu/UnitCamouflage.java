/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import megamek.client.ui.tileset.EntityImage;
import megamek.common.icons.Camouflage;

/** Swing-owned image resolution, called only after unit visibility filtering. Retains the current frame's art. */
final class UnitCamouflage {
    private record Source(String category, String filename) { }

    private final Map<Source, BoardScene.Pixels> images = new HashMap<>();
    private final Map<UnitModelState.Marker, BoardScene.Pixels> markers = new HashMap<>();
    private final Set<Source> usedImages = new HashSet<>();
    private final Set<UnitModelState.Marker> usedMarkers = new HashSet<>();

    void begin() {
        usedImages.clear();
        usedMarkers.clear();
    }

    BoardScene.UnitModel resolve(BoardScene.UnitModel selection) {
        if (selection == null || selection.state() == null) {
            return selection;
        }
        var state = selection.state();
        return new BoardScene.UnitModel(selection.asset(), selection.fallback(), selection.variant(), selection.figures(),
              selection.twist(), selection.damage(), new UnitModelState(state.structure(), resolve(state.appearance()), state.pose()));
    }

    private UnitModelState.Appearance resolve(UnitModelState.Appearance appearance) {
        Map<Integer, UnitModelState.Appearance> fighters = new HashMap<>();
        appearance.fighters().forEach((id, member) -> fighters.put(id, resolve(member)));
        var camo = appearance.camo();
        if (camo != null) {
            BoardScene.Pixels pixels = camo.image();
            var icon = new Camouflage(camo.category(), camo.filename());
            if (!icon.hasDefaultCategory() && !icon.isColourCamouflage()) {
                var source = new Source(camo.category(), camo.filename());
                usedImages.add(source);
                pixels = images.computeIfAbsent(source, ignored -> BoardScene.Pixels.copy(icon.getImage()));
            }
            var marker = camo.marker();
            if (marker != null) {
                var key = new UnitModelState.Marker(marker.rgb(), marker.direction(), marker.style(), null);
                usedMarkers.add(key);
                marker = new UnitModelState.Marker(key.rgb(), key.direction(), key.style(),
                      markers.computeIfAbsent(key, UnitCamouflage::marker));
            }
            camo = new UnitModelState.Camo(camo.category(), camo.filename(), camo.rotation(), camo.scale(),
                  camo.rgb(), pixels, marker);
        }
        return new UnitModelState.Appearance(appearance.inoperableEquipment(), appearance.searchlightOn(), camo, fighters);
    }

    private static BoardScene.Pixels marker(UnitModelState.Marker marker) {
        BufferedImage image = new BufferedImage(84, 72, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setRGB(x, y, EntityImage.markerPixel(x, y, marker.rgb(), marker.direction(), marker.style()));
            }
        }
        return new BoardScene.Pixels(image);
    }

    void retain() {
        images.keySet().retainAll(usedImages);
        markers.keySet().retainAll(usedMarkers);
    }

    void clear() {
        images.clear();
        markers.clear();
        begin();
    }
}

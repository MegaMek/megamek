/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.event;

import megamek.client.ui.Base64Image;
import megamek.common.annotations.Nullable;
import megamek.server.scriptedevent.NarrativeDisplayProvider;

import java.awt.*;

public class GameScriptedMessageEvent extends GameScriptedEvent implements NarrativeDisplayProvider {

    private final String message;
    private final String header;
    private final Base64Image image;

    public GameScriptedMessageEvent(Object source, String header, String message, @Nullable Base64Image image) {
        super(source);
        this.message = message;
        this.header = header;
        this.image = image;
    }

    public String message() {
        return message;
    }

    @Override
    public String header() {
        return header;
    }

    @Override
    public String text() {
        return message;
    }

    @Override
    public Image portrait() {
        return image.getImage();
    }

    @Override
    public Image splashImage() {
        return image.getImage();
    }
}

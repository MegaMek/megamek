/*
 * Copyright (c) 2023 - The MegaMek Team. All Rights Reserved.
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

package megamek.client.ui.swing.audio;

import megamek.common.annotations.Nullable;

import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;

/**
 * Contains a sound Clip to allow for managing playback
 */
public class Sound
{
    private final Clip clip;

    public Sound(@Nullable final Clip clip) {
        this.clip = clip;
    }

    /**
     * Starts playback of the contained sound file, if one has been loaded
     */
    public void play() {
        if (clip != null) {
            clip.setFramePosition(0);
            clip.start();
        }
    }

    /**
     * Sets the output volume of the sound
     * @param volume - float value of the output volume
     */
    public void setVolume(float volume) {
        if(volume < 0.0f || volume > 1.0f) {
            throw new IllegalArgumentException("Invalid volume: " + volume);
        }

        if(clip != null) {
            FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            gainControl.setValue(20f * (float) Math.log10(volume));
        }
    }
}

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

import megamek.client.ui.Messages;
import megamek.client.ui.swing.GUIPreferences;
import megamek.common.annotations.Nullable;
import org.apache.logging.log4j.LogManager;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SoundManager implements AudioService {
    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    private final List<Sound> sounds = new ArrayList<>();

    /**
     * Loads the sound files from the paths given in the client settings
     */
    @Override
    public void loadSoundFiles()  {
        if(!sounds.isEmpty()) {
            sounds.clear();
        }

        Sound sound;

        final Clip bingClipChat = loadSoundClip(GUIP.getSoundBingFilenameChat());
        sound = new Sound(bingClipChat);
        sounds.add(sound);

        final Clip bingClipMyTurn = loadSoundClip(GUIP.getSoundBingFilenameMyTurn());
        sound = new Sound(bingClipMyTurn);
        sounds.add(sound);

        final Clip bingClipOthersTurn = loadSoundClip(GUIP.getSoundBingFilenameOthersTurn());
        sound = new Sound(bingClipOthersTurn);
        sounds.add(sound);

        setVolume();
    }

    /**
     * Starts playback of a sound if it has been loaded
     * @param id - SoundType enum indicating which sound to play
     */
    @Override
    public void playSound(SoundType id) {
        Sound sound = null;

        switch(id)
        {
            case BING_CHAT:
                if(!GUIP.getSoundMuteChat()) {
                    sound = sounds.get(0);
                }
                break;
            case BING_MY_TURN:
                if(!GUIP.getSoundMuteMyTurn()) {
                    sound = sounds.get(1);
                }
                break;
            case BING_OTHERS_TURN:
                if(!GUIP.getSoundMuteMyTurn()) {
                    sound = sounds.get(2);
                }
                break;
        }

        if(sound != null) {
            sound.play();
        }
    }

    /**
     * Sets the output volume of all sounds that are loaded
     */
    @Override
    public void setVolume() {
        for (var sound: sounds) {
            setVolume(sound);
        }
    }

    private @Nullable Clip loadSoundClip(@Nullable String filename) {
        if (filename == null) {
            return null;
        }

        final File file = new File(filename);

        if (!file.exists()) {
            LogManager.getLogger().error(Messages.getString("SoundManager.failedToLoadAudioFile") + " " + filename);
            return null;
        }

        try {
            Clip clip = AudioSystem.getClip();
            try (AudioInputStream ais = AudioSystem.getAudioInputStream(file)) {
                clip.open(ais);
                return clip;
            }
        } catch (Exception ex) {
            LogManager.getLogger().error("SoundManager was unable to load the sound", ex);
            return null;
        }
    }

    private void setVolume(final Sound sound) {
        final float volume = GUIP.getMasterVolume() / 100.0f;

        sound.setVolume(volume);
    }
}

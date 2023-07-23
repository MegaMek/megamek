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

public class SoundManager {
    private static final SoundManager instance = new SoundManager();

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    private Sound bingChat;
    private Sound bingMyTurn;
    private Sound bingOthersTurn;

    public static SoundManager getInstance() {
        return instance;
    }

    protected SoundManager() {
    }

    /**
     * Plays a sound when a chat message is entered
     */
    public void bingChat() {
        if(!GUIP.getSoundMuteChat()) {
            setVolume(bingChat);
            bingChat.play();
        }
    }

    /**
     * Plays a sound when it is the user's turn during a game
     */
    public void bingMyTurn() {
        if(!GUIP.getSoundMuteMyTurn()) {
            setVolume(bingMyTurn);
            bingMyTurn.play();
        }
    }

    /**
     * Plays a sound when it is another user's turn during a game
     */
    public void bingOthersTurn() {
        if(!GUIP.getSoundMuteOthersTurn()) {
            setVolume(bingOthersTurn);
            bingOthersTurn.play();
        }
    }

    /**
     * Loads the sound files from the paths given in the client settings
     */
    public void loadSoundFiles()  {
        final Clip bingClipChat = loadSoundClip(GUIP.getSoundBingFilenameChat());
        bingChat = new Sound(bingClipChat);

        final Clip bingClipMyTurn = loadSoundClip(GUIP.getSoundBingFilenameMyTurn());
        bingMyTurn = new Sound(bingClipMyTurn);

        final Clip bingClipOthersTurn = loadSoundClip(GUIP.getSoundBingFilenameOthersTurn());
        bingOthersTurn = new Sound(bingClipOthersTurn);
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
        float volume = GUIP.getMasterVolume() / 100.0f;

        if(sound != null) {
            sound.setVolume(volume);
        }
    }
}

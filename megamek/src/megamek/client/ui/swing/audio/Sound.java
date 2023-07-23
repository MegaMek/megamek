package megamek.client.ui.swing.audio;

import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;

public class Sound
{
    private final Clip clip;

    public Sound(final Clip clip) {
        this.clip = clip;
    }

    public void play() {
        if (clip != null) {
            clip.setFramePosition(0);
            clip.start();
        }
    }

    public void setVolume(float volume) {
        FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        gainControl.setValue(20f * (float) Math.log10(volume));
    }
}

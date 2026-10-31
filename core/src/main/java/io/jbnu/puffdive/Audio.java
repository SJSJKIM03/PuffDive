package io.jbnu.puffdive;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * BGM / 효과음 관리.
 * 모든 음원(assets/audio)은 직접 작곡한 악보를 코드로 합성(사각파·삼각파·톱니파·노이즈)해 만든 칩튠이라 저작권 문제가 없다.
 * M 키로 음소거 토글.
 */
public class Audio implements Disposable {
    public enum Sfx { JUMP, PUFF, STAR, HURT, CRASH, POWER, CLEAR, SHOOT, GULP, SPIT, GLITCH }

    private static final float MUSIC_VOL = 0.45f, SFX_VOL = 0.55f;

    private final ObjectMap<String, Music> tracks = new ObjectMap<>();
    private final Sound[] sounds = new Sound[Sfx.values().length];
    private Music current;
    private String currentName;
    private boolean muted;
    private float duck = 1f;

    public Audio() {
        for (Sfx s : Sfx.values()) {
            try {
                sounds[s.ordinal()] = Gdx.audio.newSound(Gdx.files.internal("audio/sfx_" + s.name().toLowerCase() + ".wav"));
            } catch (Exception e) {
                Gdx.app.error("Audio", "sfx load failed: " + s, e);
            }
        }
    }

    /** name: cloud / cave / magma / glitch */
    public void music(String name) {
        if (name.equals(currentName)) return;
        if (current != null) current.stop();
        Music m = tracks.get(name);
        if (m == null) {
            try {
                m = Gdx.audio.newMusic(Gdx.files.internal("audio/bgm_" + name + ".wav"));
                m.setLooping(true);
                tracks.put(name, m);
            } catch (Exception e) {
                Gdx.app.error("Audio", "bgm load failed: " + name, e);
                return;
            }
        }
        current = m;
        currentName = name;
        applyVolume();
        current.play();
    }

    /** 일시정지 화면 등에서 음악 볼륨을 줄임 */
    public void setDuck(float d) {
        if (Math.abs(d - duck) < 0.001f) return;
        duck = d;
        applyVolume();
    }

    public void toggleMute() {
        muted = !muted;
        applyVolume();
    }

    public boolean muted() { return muted; }

    private void applyVolume() {
        if (current != null) current.setVolume(muted ? 0 : MUSIC_VOL * duck);
    }

    public void play(Sfx s) { play(s, 1f); }

    public void play(Sfx s, float volume) {
        Sound snd = sounds[s.ordinal()];
        if (snd == null || muted) return;
        snd.play(SFX_VOL * volume, MathUtils.random(0.95f, 1.05f), 0);
    }

    @Override
    public void dispose() {
        for (Music m : tracks.values()) m.dispose();
        for (Sound s : sounds) if (s != null) s.dispose();
    }
}

package io.jbnu.puffdive.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

import io.jbnu.puffdive.Assets;
import io.jbnu.puffdive.Controls.Act;
import io.jbnu.puffdive.Main;
import io.jbnu.puffdive.Session;
import io.jbnu.puffdive.world.Entities.Ability;
import io.jbnu.puffdive.world.Entities.Player;
import io.jbnu.puffdive.world.World;

/** 스테이지 진행 화면. 상태: 플레이 / 일시정지 / 스테이지 클리어 / 쓰러짐 */
public class GameScreen extends ScreenAdapter {
    private enum State { PLAY, PAUSE, CLEAR, DOWN }
    private static final String[] TRACKS = {"cloud", "cave", "magma"};

    private final Main g;
    private final int levelIndex;
    private final OrthographicCamera cam = new OrthographicCamera();
    private final FitViewport viewport = new FitViewport(Main.VW, Main.VH, cam);
    private final OrthographicCamera hudCam = new OrthographicCamera();
    private final FitViewport hudViewport = new FitViewport(Main.VW, Main.VH, hudCam);
    private final GlyphLayout layout = new GlyphLayout();
    private World world;
    private State state = State.PLAY;
    private float stateTime, introTime;

    public GameScreen(Main g, int levelIndex) {
        this.g = g;
        this.levelIndex = levelIndex;
        hudCam.position.set(Main.VW / 2, Main.VH / 2, 0);
        load();
    }

    private void load() {
        world = new World(g.session, g.controls, g.audio, levelIndex, cam, Main.VW, Main.VH);
        state = State.PLAY;
        stateTime = 0;
        introTime = 0;
    }

    @Override
    public void render(float delta) {
        float dt = Math.min(delta, 1f / 30f);
        stateTime += dt;
        introTime += dt;

        if (g.controls.pressed(Act.PAUSE) && (state == State.PLAY || state == State.PAUSE)) {
            state = state == State.PLAY ? State.PAUSE : State.PLAY;
            stateTime = 0;
        }

        switch (state) {
            case PLAY:
                g.session.time += dt;
                world.update(dt);
                if (world.cleared) { state = State.CLEAR; stateTime = 0; }
                else if (world.p.hp <= 0) { state = State.DOWN; stateTime = 0; g.session.falls++; }
                break;
            case PAUSE:
                if (g.controls.pressed(Act.RESTART)) { load(); return; }
                if (g.controls.pressed(Act.QUIT)) { g.go(new TitleScreen(g)); return; }
                break;
            case CLEAR:
                world.update(dt);
                if (stateTime > 2.2f) {
                    if (levelIndex + 1 < Session.LEVELS.length) g.go(new GameScreen(g, levelIndex + 1));
                    else g.go(new ClearScreen(g));
                    return;
                }
                break;
            case DOWN:
                if (stateTime > 1.6f) { load(); return; }
                break;
        }

        g.audio.music(world.inGlitch() ? "glitch" : TRACKS[Math.min(levelIndex, TRACKS.length - 1)]);
        g.audio.setDuck(state == State.PAUSE ? 0.3f : 1f);

        ScreenUtils.clear(0, 0, 0, 1);
        viewport.apply();
        world.render(g.batch, g.shapes, g.font, g.assets);
        hudViewport.apply();
        drawHud();
    }

    private void drawHud() {
        SpriteBatch b = g.batch;
        Assets a = g.assets;
        BitmapFont f = g.font;
        Player p = world.p;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        b.setProjectionMatrix(hudCam.combined);
        b.begin();

        // 천장 위험 경고 (붉은 테두리)
        if (world.danger > 0) {
            float al = world.danger * (0.35f + 0.15f * MathUtils.sin(world.clock * 12));
            b.setColor(1, 0, 0, al);
            b.draw(a.white, 0, Main.VH - 70, Main.VW, 70);
            b.setColor(1, 0, 0, al * 0.5f);
            b.draw(a.white, 0, 0, 14, Main.VH);
            b.draw(a.white, Main.VW - 14, 0, 14, Main.VH);
        }

        // 상단 바
        b.setColor(0, 0, 0, 0.45f);
        b.draw(a.white, 0, Main.VH - 34, Main.VW, 34);
        b.setColor(1, 1, 1, 1);
        for (int i = 0; i < Player.MAX_HP; i++)
            b.draw(i < p.hp ? a.heart : a.heartEmpty, 10 + i * 20, Main.VH - 25, 16, 16);

        String ab = p.stone ? "STONE (X: back)" : p.mouth != null ? "MOUTH FULL  X:spit  DOWN:swallow"
            : p.ability == Ability.FIRE ? "FIRE (hold X)" : p.ability == Ability.STONE ? "STONE (X: drop)" : "-";
        f.setColor(p.ability == Ability.FIRE ? new Color(1, 0.6f, 0.2f, 1)
            : p.ability == Ability.STONE ? new Color(0.8f, 0.8f, 0.9f, 1) : Color.WHITE);
        f.draw(b, "ABILITY: " + ab, 120, Main.VH - 11);
        f.setColor(Color.WHITE);

        b.draw(a.star, Main.VW - 190, Main.VH - 28, 22, 22);
        f.draw(b, "x " + (g.session.stars + world.levelStars), Main.VW - 165, Main.VH - 11);
        f.draw(b, formatTime(g.session.time), Main.VW - 80, Main.VH - 11);

        // 공중에서 남은 퍼프 횟수
        if (!p.grounded && !p.stone) {
            for (int i = 0; i < Player.MAX_PUFFS; i++) {
                b.setColor(1, 1, 1, i < Player.MAX_PUFFS - p.puffsUsed ? 0.9f : 0.2f);
                b.draw(a.air, 10 + i * 18, Main.VH - 52, 14, 14);
            }
            b.setColor(1, 1, 1, 1);
        }

        // 스테이지 이름
        String title = world.inGlitch() ? "STAGE ??? - " + world.level().name
            : "STAGE " + (levelIndex + 1) + " - " + world.level().name;
        if (introTime < 2.5f) {
            float al = Math.min(1, (2.5f - introTime) * 2);
            bigText(title, Main.VH / 2 + 60, 2f, new Color(1, 1, 1, al));
        } else {
            f.setColor(0.85f, 0.85f, 0.85f, 1);
            layout.setText(f, title);
            f.draw(b, title, (Main.VW - layout.width) / 2, 20);
            f.setColor(Color.WHITE);
        }

        switch (state) {
            case PAUSE:
                b.setColor(0, 0, 0, 0.6f);
                b.draw(a.white, 0, 0, Main.VW, Main.VH);
                b.setColor(1, 1, 1, 1);
                bigText("PAUSED", Main.VH / 2 + 70, 3f, Color.WHITE);
                centerText("P / ESC : resume", Main.VH / 2 + 10);
                centerText("R : restart stage      Q : quit to title      M : sound " + (g.audio.muted() ? "ON" : "OFF"), Main.VH / 2 - 15);
                centerText("Move: ARROWS   Jump/Puff: Z   Inhale/Ability: X   Drop ability: C", Main.VH / 2 - 60);
                break;
            case CLEAR:
                bigText("STAGE CLEAR!", Main.VH / 2 + 40, 3f, new Color(1, 0.9f, 0.3f, 1));
                centerText("stars this stage: " + world.levelStars + " / " + world.totalStars(), Main.VH / 2 - 20);
                break;
            case DOWN:
                b.setColor(0, 0, 0, Math.min(0.6f, stateTime));
                b.draw(a.white, 0, 0, Main.VW, Main.VH);
                b.setColor(1, 1, 1, 1);
                bigText("OUCH... TRY AGAIN!", Main.VH / 2 + 20, 2.5f, new Color(1, 0.5f, 0.6f, 1));
                break;
            default:
                break;
        }
        b.end();
    }

    private void bigText(String s, float y, float scale, Color c) {
        BitmapFont f = g.font;
        f.getData().setScale(scale);
        layout.setText(f, s);
        f.setColor(0, 0, 0, c.a * 0.6f);
        f.draw(g.batch, s, (Main.VW - layout.width) / 2 + 3, y - 3);
        f.setColor(c);
        f.draw(g.batch, s, (Main.VW - layout.width) / 2, y);
        f.getData().setScale(1f);
        f.setColor(Color.WHITE);
    }

    private void centerText(String s, float y) {
        layout.setText(g.font, s);
        g.font.draw(g.batch, s, (Main.VW - layout.width) / 2, y);
    }

    static String formatTime(float t) {
        int m = (int) (t / 60), s = (int) (t % 60);
        return m + ":" + (s < 10 ? "0" : "") + s;
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, false);
        hudViewport.update(width, height, true);
    }

    /** 창이 포커스를 잃으면 자동 일시정지 */
    @Override
    public void pause() {
        if (state == State.PLAY) { state = State.PAUSE; stateTime = 0; }
    }
}

package io.jbnu.puffdive.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

import io.jbnu.puffdive.Controls.Act;
import io.jbnu.puffdive.Main;
import io.jbnu.puffdive.Session;
import io.jbnu.puffdive.world.Level;

/** 모든 스테이지 클리어 → 기록 표시 → ENTER 로 1스테이지부터 재시작 */
public class ClearScreen extends ScreenAdapter {
    private final Main g;
    private final OrthographicCamera cam = new OrthographicCamera();
    private final FitViewport vp = new FitViewport(Main.VW, Main.VH, cam);
    private final GlyphLayout layout = new GlyphLayout();
    private final int totalStars;
    private float t;

    public ClearScreen(Main g) {
        this.g = g;
        cam.position.set(Main.VW / 2, Main.VH / 2, 0);
        int n = 0;
        for (String path : Session.LEVELS) {
            Level lv = Level.parse(Gdx.files.internal(path).readString("UTF-8"));
            for (Level.Spawn s : lv.spawns) if (s.c == '*') n++;
        }
        totalStars = n;
    }

    @Override
    public void render(float delta) {
        t += delta;
        g.audio.setDuck(1f);
        g.audio.music("cloud");
        if (t > 1f && g.controls.pressed(Act.CONFIRM)) { g.newGame(); return; }
        if (t > 1f && g.controls.pressed(Act.QUIT)) { g.go(new TitleScreen(g)); return; }

        ScreenUtils.clear(0.08f, 0.04f, 0.15f, 1);
        vp.apply();
        SpriteBatch b = g.batch;
        BitmapFont f = g.font;
        b.setProjectionMatrix(cam.combined);
        b.begin();
        for (int i = 0; i < 24; i++) {
            float x = (i * 97 + t * 30) % Main.VW;
            float y = Main.VH - ((i * 53 + t * (60 + i * 5)) % (Main.VH + 40));
            b.setColor(new Color().fromHsv((i * 40 + t * 60) % 360, 0.5f, 1));
            b.draw(g.assets.star, x, y, 18, 18, 36, 36, 1, 1, t * 90 + i * 20, 0, 0, 32, 32, false, false);
        }
        b.setColor(1, 1, 1, 1);
        b.draw(g.assets.pNormal, Main.VW / 2 - 24, 320 + Math.abs(MathUtils.sin(t * 5)) * 24, 48, 48);

        BitmapFont big = g.bigFont;
        big.getData().setScale(1.5f);
        center(b, big, "모든 스테이지 클리어!", 300, new Color(1, 0.9f, 0.3f, 1));
        big.getData().setScale(1f);
        Session s = g.session;
        center(b, f, "클리어 시간  " + GameScreen.formatTime(s.time), 230, Color.WHITE);
        center(b, f, "모은 별  " + s.stars + " / " + totalStars, 205, Color.WHITE);
        center(b, f, "비밀 글리치 별  " + (s.glitchStar ? "발견!" : "못 찾음... (2스테이지 맵 끝 너머를 살펴보자)"), 180,
            s.glitchStar ? new Color(0.3f, 1, 1, 1) : new Color(0.7f, 0.7f, 0.7f, 1));
        center(b, f, "쓰러진 횟수  " + s.falls + "      복사한 능력  " + s.abilitiesCopied, 155, Color.WHITE);
        if (((int) (t * 2)) % 2 == 0) center(b, f, "스페이스 / 엔터 : 1스테이지부터 다시      Q : 타이틀로", 80, new Color(1, 0.6f, 0.78f, 1));
        f.setColor(Color.WHITE);
        b.end();
    }

    private void center(SpriteBatch b, BitmapFont f, String s, float y, Color c) {
        layout.setText(f, s);
        f.setColor(c);
        f.draw(b, s, (Main.VW - layout.width) / 2, y);
    }

    @Override
    public void resize(int width, int height) {
        vp.update(width, height, false);
    }
}

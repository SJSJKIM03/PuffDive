package io.jbnu.puffdive.screens;

import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

import io.jbnu.puffdive.Controls.Act;
import io.jbnu.puffdive.Main;

/** 타이틀 화면 + 조작법 안내 */
public class TitleScreen extends ScreenAdapter {
    private final Main g;
    private final OrthographicCamera cam = new OrthographicCamera();
    private final FitViewport vp = new FitViewport(Main.VW, Main.VH, cam);
    private final GlyphLayout layout = new GlyphLayout();
    private float t;

    public TitleScreen(Main g) {
        this.g = g;
        cam.position.set(Main.VW / 2, Main.VH / 2, 0);
    }

    @Override
    public void render(float delta) {
        t += delta;
        if (t > 0.3f && g.controls.pressed(Act.CONFIRM)) { g.newGame(); return; }

        ScreenUtils.clear(0, 0, 0, 1);
        vp.apply();
        ShapeRenderer s = g.shapes;
        s.setProjectionMatrix(cam.combined);
        s.begin(ShapeRenderer.ShapeType.Filled);
        s.rect(0, 0, Main.VW, Main.VH, new Color(0.82f, 0.94f, 1, 1), new Color(0.82f, 0.94f, 1, 1),
            new Color(0.42f, 0.72f, 1, 1), new Color(0.42f, 0.72f, 1, 1));
        s.setColor(1, 1, 1, 0.6f);
        for (int i = 0; i < 6; i++) {
            float x = ((i * 137 + t * 20 * (1 + i % 3)) % (Main.VW + 120)) - 60;
            float y = 60 + i * 70;
            s.circle(x, y, 24);
            s.circle(x + 22, y - 4, 18);
            s.circle(x - 22, y - 5, 16);
        }
        s.end();

        SpriteBatch b = g.batch;
        BitmapFont f = g.font;
        b.setProjectionMatrix(cam.combined);
        b.begin();
        // 떨어지는 주인공
        float py = Main.VH - ((t * 160) % (Main.VH + 80)) + 40;
        b.draw(g.assets.pPuff, Main.VW / 2 + 150 + MathUtils.sin(t * 3) * 20, py, 48, 48);
        b.draw(g.assets.pNormal, Main.VW / 2 - 200, 310 + Math.abs(MathUtils.sin(t * 4)) * 20, 40, 40);

        f.getData().setScale(4f);
        layout.setText(f, "PUFF DIVE");
        f.setColor(0.4f, 0.1f, 0.3f, 1);
        f.draw(b, "PUFF DIVE", (Main.VW - layout.width) / 2 + 4, 380 - 4);
        f.setColor(1f, 0.6f, 0.78f, 1);
        f.draw(b, "PUFF DIVE", (Main.VW - layout.width) / 2, 380);
        f.getData().setScale(1.2f);
        center(b, f, "Dive down the maze. Inhale. Copy. Puff. Escape.", 300, new Color(0.15f, 0.2f, 0.45f, 1));
        f.getData().setScale(1f);
        String[] lines = {
            "ARROWS : move          Z / SPACE : jump  (again in mid-air = PUFF)",
            "X : inhale enemy  ->  X spit star  /  DOWN swallow = COPY ABILITY",
            "FIRE: hold X to shoot     STONE: X to drop & smash cracked blocks",
            "C : drop ability     DOWN + Z : through thin platforms     P / ESC : pause",
        };
        for (int i = 0; i < lines.length; i++) center(b, f, lines[i], 230 - i * 24, new Color(0.1f, 0.12f, 0.3f, 1));
        if (((int) (t * 2)) % 2 == 0) {
            f.getData().setScale(1.6f);
            center(b, f, "PRESS ENTER", 90, new Color(0.9f, 0.2f, 0.45f, 1));
            f.getData().setScale(1f);
        }
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

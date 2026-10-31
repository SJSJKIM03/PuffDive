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
        g.audio.setDuck(1f);
        g.audio.music("cloud");
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

        BitmapFont big = g.bigFont;
        big.getData().setScale(2.5f);
        layout.setText(big, "PUFF DIVE");
        big.setColor(0.4f, 0.1f, 0.3f, 1);
        big.draw(b, "PUFF DIVE", (Main.VW - layout.width) / 2 + 4, 400 - 4);
        big.setColor(1f, 0.6f, 0.78f, 1);
        big.draw(b, "PUFF DIVE", (Main.VW - layout.width) / 2, 400);
        big.getData().setScale(1f);
        center(b, big, "퍼프 다이브", 318, new Color(0.9f, 0.35f, 0.55f, 1));
        center(b, f, "미로 아래로 파고들어라! 빨아들이고, 따라 하고, 부풀어 올라라!", 282, new Color(0.15f, 0.2f, 0.45f, 1));
        String[] lines = {
            "방향키 : 이동            스페이스 : 점프 (공중에서 한 번 더 = 퍼프 비행)",
            "X : 적 빨아들이기  ->  X 별 뱉기 / 아래 키 삼키기 = 능력 복사",
            "불꽃 능력 : X 누르고 있으면 발사       돌 능력 : X로 낙하해 금 간 블록 부수기",
            "C : 능력 버리기      아래 + 스페이스 : 얇은 발판 내려가기",
            "P / ESC : 일시정지      M : 소리 끄기/켜기",
        };
        for (int i = 0; i < lines.length; i++) center(b, f, lines[i], 230 - i * 24, new Color(0.1f, 0.12f, 0.3f, 1));
        if (((int) (t * 2)) % 2 == 0) {
            big.getData().setScale(0.75f);
            center(b, big, "스페이스를 눌러 시작", 82, new Color(0.9f, 0.2f, 0.45f, 1));
            big.getData().setScale(1f);
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

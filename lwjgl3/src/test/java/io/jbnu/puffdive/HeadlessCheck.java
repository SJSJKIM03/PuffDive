package io.jbnu.puffdive;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.GdxNativesLoader;

import io.jbnu.puffdive.world.Entities.Ability;
import io.jbnu.puffdive.world.Entities.Enemy;
import io.jbnu.puffdive.world.Level;
import io.jbnu.puffdive.world.World;

/** 창 없이 게임 로직 시나리오를 돌려 핵심 기능이 동작하는지 확인하는 자동 점검 */
public class HeadlessCheck extends ApplicationAdapter {
    private static int fails;
    private static final int T = Level.TILE;

    public static void main(String[] args) {
        GdxNativesLoader.disableNativesLoading = true;
        String n = System.getenv("PUFF_NATIVES");
        if (n != null && !n.isEmpty()) System.load(n + "/libgdxarm64.dylib");
        HeadlessApplicationConfiguration c = new HeadlessApplicationConfiguration();
        c.updatesPerSecond = -1;
        new HeadlessApplication(new HeadlessCheck(), c);
    }

    private static void check(String name, boolean ok, String info) {
        System.out.println((ok ? "[PASS] " : "[FAIL] ") + name + "  " + info);
        if (!ok) fails++;
    }

    private World world(int level, String script, Controls[] out) {
        Controls c = new Controls(script);
        out[0] = c;
        return new World(new Session(), c, null, level, new OrthographicCamera(), 640, 480);
    }

    private void run(World w, Controls c, int frames) {
        for (int f = 0; f < frames; f++) {
            c.update(f);
            w.update(1f / 60f);
        }
    }

    private static void noEnemies(World w) {
        for (Enemy e : w.cur.enemies) { e.dead = true; e.respawnTimer = 0; }
    }

    private static String pos(World w) {
        return String.format("tile=(%.1f, %.1f) hp=%d", w.p.box.x / T, w.p.box.y / T, w.p.hp);
    }

    /** 한글 폰트가 실제 글리프를 만드는지 확인: 레벨 힌트/UI 문구를 폰트 아틀라스 PNG로 저장 */
    private void fontPreview(String out) {
        String n = System.getenv("PUFF_NATIVES");
        System.load(n + "/libgdx-freetypearm64.dylib");
        com.badlogic.gdx.utils.SharedLibraryLoader.setLoaded("gdx-freetype");
        com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator gen =
            new com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator(Gdx.files.internal("fonts/Galmuri11.ttf"));
        StringBuilder sb = new StringBuilder();
        for (String path : Session.LEVELS) for (Level.Hint h : Level.parse(Gdx.files.internal(path).readString("UTF-8")).hints) sb.append(h.text);
        sb.append("구름정원수정동굴용암심장부스테이지일시정지능력없음불꽃돌입에물었다뱉기삼키기아야다시도전클리어방향키스페이스점프퍼프");
        com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter p =
            new com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter();
        p.size = 24;
        p.characters = sb.toString();
        com.badlogic.gdx.graphics.g2d.PixmapPacker packer = new com.badlogic.gdx.graphics.g2d.PixmapPacker(1024, 512,
            com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888, 2, false);
        p.packer = packer;
        com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeBitmapFontData data = gen.generateData(p);
        int missing = 0;
        for (char ch : sb.toString().toCharArray()) if (ch != ' ' && data.getGlyph(ch) == null) missing++;
        com.badlogic.gdx.graphics.Pixmap page = packer.getPages().first().getPixmap();
        com.badlogic.gdx.graphics.Pixmap bg = new com.badlogic.gdx.graphics.Pixmap(page.getWidth(), 160, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        bg.setColor(0.13f, 0.13f, 0.2f, 1);
        bg.fill();
        bg.setBlending(com.badlogic.gdx.graphics.Pixmap.Blending.SourceOver);
        bg.drawPixmap(page, 0, 0);
        com.badlogic.gdx.graphics.PixmapIO.writePNG(Gdx.files.absolute(out), bg);
        check("korean font has all glyphs", missing == 0, "missing=" + missing + " preview=" + out);
    }

    @Override
    public void create() {
        try {
            Controls[] c = new Controls[1];

            // 1) 모든 레벨 로드 + 가만히 3초
            for (int i = 0; i < Session.LEVELS.length; i++) {
                World w = world(i, null, c);
                run(w, c[0], 180);
                check("level " + (i + 1) + " loads & idles", w.p.hp > 0 && w.p.grounded,
                    w.level().name + " " + w.level().w + "x" + w.level().h + " " + pos(w));
            }

            // 1b) 몬스터가 가시 위로 걸어가지 않음 (1스테이지 가시 바닥 왼쪽 워커)
            World sw = world(0, null, c);
            Enemy spikeWalker = null;
            for (Enemy e : sw.cur.enemies)
                if (e.type == Enemy.Type.WALKER && e.spawnY == (sw.level().h - 1 - 14) * T) spikeWalker = e;
            run(sw, c[0], 60 * 20);
            check("walker never steps onto spikes", spikeWalker != null && !spikeWalker.dead && spikeWalker.box.x + spikeWalker.box.width <= 8 * T + 1,
                spikeWalker == null ? "not found" : String.format("x=%.2f..%.2f tiles", spikeWalker.box.x / T, (spikeWalker.box.x + spikeWalker.box.width) / T));

            // 1c) 3스테이지 적들이 위험 지형에 빠져 사라지지 않음
            sw = world(2, null, c);
            for (int i = 0; i < 60 * 15; i++) { c[0].update(i); sw.p.invuln = 99; sw.update(1f / 60f); sw.p.hp = 5; }
            int alive = 0;
            for (Enemy e : sw.cur.enemies) if (!e.dead) alive++;
            check("L3 enemies stay off lava/spikes", alive == sw.cur.enemies.size, alive + "/" + sw.cur.enemies.size + " alive");

            // 2) 1스테이지: 오른쪽으로 걸으면 첫 구멍으로 떨어짐
            World w = world(0, "10:+RIGHT", c);
            float y0 = w.p.box.y;
            run(w, c[0], 240);
            check("L1 walk right falls through first gap", w.p.box.y < y0 - 3 * T, pos(w));

            // 3) 흡입 → 입에 물기 → 삼키기
            w = world(0, "5:+ACTION,200:-ACTION", c);
            w.p.box.setPosition(8 * T, w.p.box.y);
            w.p.facingRight = true;
            run(w, c[0], 210);
            check("L1 inhale captures walker", w.p.mouth == Enemy.Type.WALKER, "mouth=" + w.p.mouth);

            // 4) 퍼프: 공중에서 Z 반복 시 천천히 떨어짐
            w = world(0, "5:*JUMP,30:*JUMP,50:*JUMP,70:*JUMP", c);
            run(w, c[0], 80);
            check("puff count limited to 3", w.p.puffsUsed == 3 && w.p.puffing, "puffs=" + w.p.puffsUsed);

            // 5) 2스테이지: STONE 능력으로 금 간 블록 파괴
            w = world(1, "20:*ACTION", c);
            noEnemies(w);
            Level lv = w.level();
            w.p.ability = Ability.STONE;
            int row9 = lv.h - 1 - 9;
            w.p.box.setPosition(3 * T, (row9 + 1) * T);
            run(w, c[0], 90);
            check("L2 stone drop breaks cracked floor", lv.tile(3, row9) == '.' && w.p.box.y < row9 * T, pos(w));

            // 6) 2스테이지: 오른쪽 맵 밖 → 글리치 월드 (중력 반전)
            w = world(1, "5:+RIGHT,100:-RIGHT", c);
            lv = w.level();
            w.p.box.setPosition(22 * T, (lv.h - 1 - 19) * T);
            run(w, c[0], 120);
            check("L2 out of bounds enters glitch world", w.inGlitch() && w.level().gravity == -1, pos(w));
            run(w, c[0], 120);
            check("glitch gravity pulls player up to ceiling", w.p.grounded && w.p.box.y > (w.level().h - 3) * T, pos(w));
            // 포탈로 이동하면 메인 레벨 R 지점으로 복귀
            w.p.box.setPosition(23 * T, (w.level().h - 1 - 2) * T);
            run(w, c[0], 5);
            check("portal returns to stage 2 shortcut", !w.inGlitch(), pos(w));

            // 7) 3스테이지: 가만히 있으면 천장에 깔림
            w = world(2, null, c);
            run(w, c[0], 60 * 25);
            check("L3 spike ceiling hurts idle player", w.p.hp < 5, pos(w) + " ceilY=" + w.ceilY / T);

            // 8) 일시정지와 무관하게 골 문 접촉 시 클리어
            w = world(0, null, c);
            lv = w.level();
            w.p.box.setPosition(11 * T + 4, (lv.h - 1 - 29) * T);
            run(w, c[0], 3);
            check("L1 goal door clears stage", w.cleared, pos(w));
        } catch (Throwable t) {
            t.printStackTrace();
            fails++;
        }
        String preview = System.getenv("PUFF_FONT_PREVIEW");
        if (preview != null) fontPreview(preview);
        System.out.println(fails == 0 ? "ALL CHECKS PASSED" : fails + " CHECK(S) FAILED");
        Gdx.app.exit();
    }
}

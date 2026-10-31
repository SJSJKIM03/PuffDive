package io.jbnu.puffdive;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

/**
 * 외부 이미지 파일 없이 Pixmap 으로 모든 스프라이트를 코드로 그려서 만든다.
 * (저작권 걱정 없음 + 테마별 색은 SpriteBatch tint 로 바꿔 끼움)
 */
public class Assets implements Disposable {
    public final Texture white, block, cracked, oneway, spike;
    public final Texture pNormal, pPuff, pInhale, pFull, pStone, hatFire, hatStone;
    public final Texture walker, fireEnemy, stoneEnemy, bat;
    public final Texture star, door, flagDown, flagUp, portal, fireball, air, flame, heart, heartEmpty;
    private final Array<Texture> all = new Array<>();

    public Assets() {
        white = make(solid(4, 4, 1, 1, 1, 1));
        block = make(block(false));
        cracked = make(block(true));
        oneway = make(oneway());
        spike = make(spike());
        pNormal = make(player(0));
        pPuff = make(puff());
        pInhale = make(player(1));
        pFull = make(full());
        pStone = make(stone());
        hatFire = make(hatFire());
        hatStone = make(hatStone());
        walker = make(walker());
        fireEnemy = make(fireEnemy());
        stoneEnemy = make(stoneEnemy());
        bat = make(bat());
        star = make(star(32, 15, 6, 1f, 0.85f, 0.2f));
        door = make(door());
        flagDown = make(flag(false));
        flagUp = make(flag(true));
        portal = make(portal());
        fireball = make(ball(16, 1f, 0.55f, 0.1f, 1f, 0.95f, 0.4f));
        air = make(ball(16, 0.85f, 0.95f, 1f, 1f, 1f, 1f));
        flame = make(ball(16, 0.9f, 0.15f, 0.1f, 1f, 0.6f, 0.2f));
        heart = make(heart(true));
        heartEmpty = make(heart(false));
    }

    private Texture make(Pixmap p) {
        Texture t = new Texture(p);
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        p.dispose();
        all.add(t);
        return t;
    }

    private static Pixmap solid(int w, int h, float r, float g, float b, float a) {
        Pixmap p = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        p.setColor(r, g, b, a);
        p.fill();
        return p;
    }

    private static Pixmap block(boolean crackedBlock) {
        Pixmap p = solid(32, 32, 0.92f, 0.92f, 0.92f, 1);
        p.setColor(1, 1, 1, 1);
        p.fillRectangle(0, 0, 32, 4);
        p.fillRectangle(0, 0, 4, 32);
        p.setColor(0.72f, 0.72f, 0.72f, 1);
        p.fillRectangle(0, 28, 32, 4);
        p.fillRectangle(28, 0, 4, 32);
        p.setColor(0.6f, 0.6f, 0.6f, 1);
        p.drawRectangle(0, 0, 32, 32);
        p.setColor(0.85f, 0.85f, 0.85f, 1);
        p.fillRectangle(9, 9, 3, 3);
        p.fillRectangle(20, 18, 3, 3);
        if (crackedBlock) {
            p.setColor(0.25f, 0.25f, 0.3f, 1);
            int[][] lines = {{16, 2, 13, 10}, {13, 10, 18, 16}, {18, 16, 12, 24}, {12, 24, 15, 30},
                {18, 16, 26, 19}, {13, 10, 5, 13}, {26, 19, 29, 27}};
            for (int[] l : lines) {
                p.drawLine(l[0], l[1], l[2], l[3]);
                p.drawLine(l[0] + 1, l[1], l[2] + 1, l[3]);
            }
        }
        return p;
    }

    private static Pixmap oneway() {
        Pixmap p = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
        p.setColor(0.95f, 0.95f, 0.95f, 1);
        p.fillRectangle(0, 0, 32, 9);
        p.setColor(0.65f, 0.65f, 0.65f, 1);
        p.fillRectangle(0, 7, 32, 2);
        p.fillRectangle(6, 9, 3, 5);
        p.fillRectangle(23, 9, 3, 5);
        p.setColor(0.8f, 0.8f, 0.8f, 1);
        for (int x = 2; x < 32; x += 8) p.fillRectangle(x, 2, 3, 3);
        return p;
    }

    private static Pixmap spike() {
        Pixmap p = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
        p.setColor(0.45f, 0.45f, 0.5f, 1);
        p.fillRectangle(0, 26, 32, 6);
        for (int i = 0; i < 4; i++) {
            int x = i * 8;
            p.setColor(0.75f, 0.75f, 0.8f, 1);
            p.fillTriangle(x, 27, x + 8, 27, x + 4, 6);
            p.setColor(1, 1, 1, 1);
            p.fillTriangle(x + 1, 26, x + 4, 26, x + 4, 9);
        }
        return p;
    }

    // ---- 플레이어 (분홍 공 캐릭터) ----
    private static void body(Pixmap p, int cx, int cy, int r) {
        p.setColor(0.55f, 0.15f, 0.3f, 1);
        p.fillCircle(cx, cy, r + 1);
        p.setColor(1f, 0.62f, 0.76f, 1);
        p.fillCircle(cx, cy, r);
        p.setColor(1f, 0.78f, 0.87f, 1);
        p.fillCircle(cx - r / 3, cy - r / 3, r / 3);
    }

    private static void feet(Pixmap p, int y) {
        p.setColor(0.5f, 0.05f, 0.15f, 1);
        p.fillCircle(9, y, 5);
        p.fillCircle(23, y, 5);
        p.setColor(0.9f, 0.12f, 0.3f, 1);
        p.fillCircle(9, y, 4);
        p.fillCircle(23, y, 4);
    }

    private static void eyes(Pixmap p, int x1, int x2, int y, int h) {
        p.setColor(0.08f, 0.05f, 0.2f, 1);
        p.fillRectangle(x1, y, 3, h);
        p.fillRectangle(x2, y, 3, h);
        p.setColor(0.3f, 0.4f, 1f, 1);
        p.fillRectangle(x1, y + h - 2, 3, 2);
        p.fillRectangle(x2, y + h - 2, 3, 2);
        p.setColor(1, 1, 1, 1);
        p.fillRectangle(x1, y, 2, 2);
        p.fillRectangle(x2, y, 2, 2);
    }

    private static Pixmap player(int mode) {
        Pixmap p = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
        feet(p, 27);
        body(p, 16, 16, 13);
        eyes(p, 15, 21, 8, 7);
        p.setColor(1f, 0.4f, 0.55f, 1);
        p.fillRectangle(10, 18, 4, 2);
        p.fillRectangle(25, 18, 3, 2);
        if (mode == 1) { // 흡입 중: 큰 입
            p.setColor(0.4f, 0.02f, 0.1f, 1);
            p.fillCircle(24, 20, 6);
            p.setColor(0.9f, 0.3f, 0.4f, 1);
            p.fillCircle(24, 22, 3);
        } else {
            p.setColor(0.6f, 0.05f, 0.15f, 1);
            p.fillRectangle(19, 19, 3, 2);
        }
        return p;
    }

    private static Pixmap puff() {
        Pixmap p = new Pixmap(40, 40, Pixmap.Format.RGBA8888);
        p.setColor(0.5f, 0.05f, 0.15f, 1);
        p.fillCircle(10, 35, 4);
        p.fillCircle(30, 35, 4);
        p.setColor(0.55f, 0.15f, 0.3f, 1);
        p.fillCircle(20, 19, 18);
        p.setColor(1f, 0.62f, 0.76f, 1);
        p.fillCircle(20, 19, 17);
        p.setColor(1f, 0.8f, 0.88f, 1);
        p.fillCircle(14, 13, 6);
        eyes(p, 19, 26, 9, 6);
        p.setColor(1f, 0.4f, 0.55f, 1);
        p.fillRectangle(12, 21, 4, 3);
        p.fillRectangle(30, 21, 4, 3);
        p.setColor(0.6f, 0.05f, 0.15f, 1);
        p.fillCircle(25, 25, 2);
        return p;
    }

    private static Pixmap full() {
        Pixmap p = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
        feet(p, 28);
        p.setColor(0.55f, 0.15f, 0.3f, 1);
        p.fillCircle(11, 17, 12);
        p.fillCircle(21, 17, 12);
        p.setColor(1f, 0.62f, 0.76f, 1);
        p.fillCircle(11, 17, 11);
        p.fillCircle(21, 17, 11);
        eyes(p, 13, 20, 7, 6);
        p.setColor(1f, 0.4f, 0.55f, 1);
        p.fillCircle(6, 20, 3);
        p.fillCircle(27, 20, 3);
        p.setColor(0.6f, 0.05f, 0.15f, 1);
        p.fillRectangle(14, 21, 6, 2);
        return p;
    }

    private static Pixmap stone() {
        Pixmap p = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
        p.setColor(0.25f, 0.25f, 0.28f, 1);
        p.fillTriangle(1, 31, 31, 31, 16, 1);
        p.fillRectangle(3, 12, 26, 20);
        p.setColor(0.55f, 0.55f, 0.6f, 1);
        p.fillTriangle(3, 30, 29, 30, 16, 4);
        p.fillRectangle(5, 13, 22, 17);
        p.setColor(0.7f, 0.7f, 0.75f, 1);
        p.fillRectangle(9, 12, 5, 4);
        p.fillRectangle(18, 22, 4, 3);
        p.setColor(0.1f, 0.1f, 0.12f, 1);
        p.fillRectangle(11, 18, 3, 4);
        p.fillRectangle(19, 18, 3, 4);
        p.drawLine(10, 16, 14, 17);
        p.drawLine(18, 17, 22, 16);
        return p;
    }

    private static Pixmap hatFire() {
        Pixmap p = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
        p.setColor(0.9f, 0.85f, 0.2f, 1);
        p.fillRectangle(6, 5, 20, 4);
        p.setColor(1f, 0.35f, 0.05f, 1);
        p.fillTriangle(6, 6, 14, 6, 9, -6);
        p.fillTriangle(12, 6, 20, 6, 16, -9);
        p.fillTriangle(18, 6, 26, 6, 23, -6);
        p.setColor(1f, 0.85f, 0.2f, 1);
        p.fillTriangle(13, 6, 19, 6, 16, -3);
        return p;
    }

    private static Pixmap hatStone() {
        Pixmap p = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
        p.setColor(0.45f, 0.25f, 0.1f, 1);
        p.fillRectangle(4, 4, 24, 4);
        p.fillTriangle(3, 6, 0, 2, 0, 10);
        p.setColor(0.7f, 0.45f, 0.2f, 1);
        p.fillRectangle(4, 4, 24, 2);
        return p;
    }

    // ---- 적 ----
    private static Pixmap walker() {
        Pixmap p = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
        p.setColor(0.45f, 0.2f, 0.05f, 1);
        p.fillCircle(9, 28, 4);
        p.fillCircle(23, 28, 4);
        p.setColor(0.6f, 0.25f, 0.05f, 1);
        p.fillCircle(16, 15, 13);
        p.setColor(1f, 0.55f, 0.15f, 1);
        p.fillCircle(16, 15, 12);
        p.setColor(1f, 0.85f, 0.65f, 1);
        p.fillCircle(17, 17, 8);
        p.setColor(0.1f, 0.05f, 0.05f, 1);
        p.fillRectangle(13, 13, 2, 5);
        p.fillRectangle(19, 13, 2, 5);
        p.setColor(1f, 0.55f, 0.5f, 1);
        p.fillRectangle(11, 19, 2, 2);
        p.fillRectangle(22, 19, 2, 2);
        return p;
    }

    private static Pixmap fireEnemy() {
        Pixmap p = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
        p.setColor(1f, 0.4f, 0f, 1);
        p.fillTriangle(6, 14, 26, 14, 16, -2);
        p.fillTriangle(4, 16, 14, 14, 6, 2);
        p.fillTriangle(18, 14, 28, 16, 26, 2);
        p.setColor(1f, 0.85f, 0.2f, 1);
        p.fillTriangle(11, 14, 21, 14, 16, 4);
        p.setColor(0.5f, 0.05f, 0.05f, 1);
        p.fillCircle(16, 20, 11);
        p.setColor(0.85f, 0.15f, 0.1f, 1);
        p.fillCircle(16, 20, 10);
        p.setColor(1, 1, 1, 1);
        p.fillCircle(12, 19, 3);
        p.fillCircle(20, 19, 3);
        p.setColor(0, 0, 0, 1);
        p.fillRectangle(12, 19, 2, 3);
        p.fillRectangle(20, 19, 2, 3);
        p.setColor(0.4f, 0.05f, 0.05f, 1);
        p.fillRectangle(9, 29, 5, 3);
        p.fillRectangle(18, 29, 5, 3);
        return p;
    }

    private static Pixmap stoneEnemy() {
        Pixmap p = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
        p.setColor(0.25f, 0.25f, 0.3f, 1);
        p.fillRectangle(2, 4, 28, 28);
        p.setColor(0.55f, 0.55f, 0.6f, 1);
        p.fillRectangle(4, 6, 24, 24);
        p.setColor(0.7f, 0.7f, 0.75f, 1);
        p.fillRectangle(4, 6, 24, 4);
        p.setColor(1, 1, 1, 1);
        p.fillRectangle(8, 13, 6, 6);
        p.fillRectangle(18, 13, 6, 6);
        p.setColor(0, 0, 0, 1);
        p.fillRectangle(10, 15, 3, 3);
        p.fillRectangle(20, 15, 3, 3);
        p.drawLine(7, 11, 14, 13);
        p.drawLine(25, 11, 18, 13);
        p.setColor(0.2f, 0.2f, 0.2f, 1);
        p.fillRectangle(11, 23, 10, 2);
        return p;
    }

    private static Pixmap bat() {
        Pixmap p = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
        p.setColor(0.35f, 0.1f, 0.45f, 1);
        p.fillTriangle(0, 8, 12, 14, 6, 22);
        p.fillTriangle(32, 8, 20, 14, 26, 22);
        p.setColor(0.55f, 0.2f, 0.7f, 1);
        p.fillCircle(16, 16, 9);
        p.setColor(1f, 0.9f, 0.2f, 1);
        p.fillRectangle(11, 13, 3, 3);
        p.fillRectangle(18, 13, 3, 3);
        p.setColor(1, 1, 1, 1);
        p.fillTriangle(13, 20, 15, 20, 14, 23);
        p.fillTriangle(17, 20, 19, 20, 18, 23);
        return p;
    }

    // ---- 아이템 ----
    private static Pixmap star(int size, float outer, float inner, float r, float g, float b) {
        Pixmap p = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        float c = size / 2f;
        drawStar(p, c, c, outer + 1.5f, inner + 1f, r * 0.6f, g * 0.45f, b * 0.2f);
        drawStar(p, c, c, outer, inner, r, g, b);
        p.setColor(1, 1, 1, 0.9f);
        p.fillCircle((int) (c - 2), (int) (c - 3), 2);
        return p;
    }

    private static void drawStar(Pixmap p, float cx, float cy, float ro, float ri, float r, float g, float b) {
        p.setColor(r, g, b, 1);
        for (int i = 0; i < 5; i++) {
            float a0 = -MathUtils.PI / 2 + i * MathUtils.PI2 / 5;
            float a1 = a0 + MathUtils.PI / 5;
            float a2 = a0 + MathUtils.PI2 / 5;
            int ox = (int) (cx + MathUtils.cos(a0) * ro), oy = (int) (cy + MathUtils.sin(a0) * ro);
            int ix = (int) (cx + MathUtils.cos(a1) * ri), iy = (int) (cy + MathUtils.sin(a1) * ri);
            int nx = (int) (cx + MathUtils.cos(a2) * ro), ny = (int) (cy + MathUtils.sin(a2) * ro);
            p.fillTriangle((int) cx, (int) cy, ox, oy, ix, iy);
            p.fillTriangle((int) cx, (int) cy, ix, iy, nx, ny);
        }
    }

    private static Pixmap door() {
        Pixmap p = new Pixmap(32, 48, Pixmap.Format.RGBA8888);
        p.setColor(0.3f, 0.15f, 0.05f, 1);
        p.fillRectangle(2, 14, 28, 34);
        p.fillCircle(16, 15, 14);
        p.setColor(0.1f, 0.05f, 0.15f, 1);
        p.fillRectangle(6, 16, 20, 32);
        p.fillCircle(16, 16, 10);
        drawStar(p, 16, 26, 8, 3.5f, 1f, 0.9f, 0.2f);
        return p;
    }

    private static Pixmap flag(boolean up) {
        Pixmap p = new Pixmap(32, 48, Pixmap.Format.RGBA8888);
        p.setColor(0.5f, 0.5f, 0.55f, 1);
        p.fillRectangle(6, 2, 3, 46);
        p.fillCircle(7, 2, 2);
        if (up) {
            p.setColor(0.2f, 0.85f, 0.4f, 1);
            p.fillTriangle(9, 4, 9, 20, 30, 12);
        } else {
            p.setColor(0.55f, 0.55f, 0.6f, 1);
            p.fillTriangle(9, 28, 9, 44, 26, 36);
        }
        return p;
    }

    private static Pixmap portal() {
        Pixmap p = new Pixmap(40, 40, Pixmap.Format.RGBA8888);
        float[][] cols = {{1f, 0.1f, 0.8f}, {0.1f, 0.9f, 1f}, {0.2f, 0f, 0.4f}, {1, 1, 1}};
        int[] rad = {19, 15, 11, 4};
        for (int i = 0; i < 4; i++) {
            p.setColor(cols[i][0], cols[i][1], cols[i][2], 1);
            p.fillCircle(20, 20, rad[i]);
        }
        p.setColor(1, 1, 1, 1);
        p.fillRectangle(18, 1, 4, 8);
        p.fillRectangle(18, 31, 4, 8);
        return p;
    }

    private static Pixmap ball(int s, float r1, float g1, float b1, float r2, float g2, float b2) {
        Pixmap p = new Pixmap(s, s, Pixmap.Format.RGBA8888);
        p.setColor(r1, g1, b1, 1);
        p.fillCircle(s / 2, s / 2, s / 2 - 1);
        p.setColor(r2, g2, b2, 1);
        p.fillCircle(s / 2 - 1, s / 2 - 1, s / 4);
        return p;
    }

    private static Pixmap heart(boolean filled) {
        Pixmap p = new Pixmap(16, 16, Pixmap.Format.RGBA8888);
        if (filled) p.setColor(1f, 0.3f, 0.45f, 1); else p.setColor(0.25f, 0.25f, 0.3f, 1);
        p.fillCircle(4, 5, 4);
        p.fillCircle(11, 5, 4);
        p.fillTriangle(0, 6, 15, 6, 8, 15);
        if (filled) {
            p.setColor(1, 0.8f, 0.85f, 1);
            p.fillRectangle(3, 3, 2, 2);
        }
        return p;
    }

    @Override
    public void dispose() {
        for (Texture t : all) t.dispose();
    }
}

package io.jbnu.puffdive.world;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;

/** 고정 크기 배열 풀을 쓰는 가벼운 파티클 시스템 (GC 없음) */
public class Particles {
    private static final int MAX = 800;
    private final float[] x = new float[MAX], y = new float[MAX], vx = new float[MAX], vy = new float[MAX];
    private final float[] life = new float[MAX], maxLife = new float[MAX], size = new float[MAX], grav = new float[MAX];
    private final float[] r = new float[MAX], g = new float[MAX], b = new float[MAX];
    private int next;

    public void emit(float px, float py, int count, float speed, float cr, float cg, float cb,
                     float sz, float lifeTime, float gravity) {
        for (int i = 0; i < count; i++) {
            int k = next;
            next = (next + 1) % MAX;
            float a = MathUtils.random(MathUtils.PI2);
            float s = MathUtils.random(0.3f, 1f) * speed;
            x[k] = px; y[k] = py;
            vx[k] = MathUtils.cos(a) * s;
            vy[k] = MathUtils.sin(a) * s;
            life[k] = maxLife[k] = lifeTime * MathUtils.random(0.6f, 1f);
            size[k] = sz * MathUtils.random(0.6f, 1.2f);
            grav[k] = gravity;
            r[k] = cr; g[k] = cg; b[k] = cb;
        }
    }

    /** 특정 방향으로 흐르는 파티클 (흡입 기류 등) */
    public void stream(float px, float py, float pvx, float pvy, float cr, float cg, float cb, float sz, float lifeTime) {
        int k = next;
        next = (next + 1) % MAX;
        x[k] = px; y[k] = py; vx[k] = pvx; vy[k] = pvy;
        life[k] = maxLife[k] = lifeTime;
        size[k] = sz; grav[k] = 0;
        r[k] = cr; g[k] = cg; b[k] = cb;
    }

    public void update(float dt) {
        for (int i = 0; i < MAX; i++) {
            if (life[i] <= 0) continue;
            life[i] -= dt;
            vy[i] -= grav[i] * dt;
            x[i] += vx[i] * dt;
            y[i] += vy[i] * dt;
        }
    }

    public void render(SpriteBatch batch, Texture white) {
        for (int i = 0; i < MAX; i++) {
            if (life[i] <= 0) continue;
            float a = life[i] / maxLife[i];
            float s = size[i] * (0.4f + 0.6f * a);
            batch.setColor(r[i], g[i], b[i], a);
            batch.draw(white, x[i] - s / 2, y[i] - s / 2, s, s);
        }
        batch.setColor(1, 1, 1, 1);
    }

    public void clear() {
        for (int i = 0; i < MAX; i++) life[i] = 0;
    }
}

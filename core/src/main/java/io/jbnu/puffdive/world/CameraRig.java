package io.jbnu.puffdive.world;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;

/**
 * 플레이어를 1:1로 붙어다니지 않는 카메라.
 *  - 지수 감쇠(lerp)로 부드럽게 따라감 (프레임레이트 독립)
 *  - 진행 방향(특히 낙하 방향)을 미리 보여주는 look-ahead
 *  - trauma 기반 화면 흔들림 (trauma^2 로 감쇠)
 *  - 레벨 경계 밖은 보여주지 않도록 clamp
 */
public class CameraRig {
    public final OrthographicCamera cam;
    private final float viewW, viewH;
    private float x, y, trauma, jitter;

    public CameraRig(OrthographicCamera cam, float viewW, float viewH) {
        this.cam = cam;
        this.viewW = viewW;
        this.viewH = viewH;
    }

    public void snap(float tx, float ty, float worldW, float worldH) {
        x = tx;
        y = ty;
        clamp(worldW, worldH);
        apply();
    }

    public void update(float tx, float ty, float vx, float vy, int grav, float worldW, float worldH, float dt) {
        float lookX = MathUtils.clamp(vx * 0.35f, -80, 80);
        float fall = -vy * grav; // 중력 방향 속도(양수 = 떨어지는 중)
        float lookY = fall > 150 ? -grav * MathUtils.clamp((fall - 150) * 0.45f, 0, 150) : 0;
        float bias = -grav * 40; // 떨어지는 게임이므로 아래쪽을 더 보여줌
        float gx = tx + lookX, gy = ty + lookY + bias;

        float ax = 1 - (float) Math.pow(0.002, dt);
        float ay = 1 - (float) Math.pow(0.0005, dt);
        x += (gx - x) * ax;
        y += (gy - y) * ay;
        clamp(worldW, worldH);

        trauma = Math.max(0, trauma - dt * 1.4f);
        apply();
    }

    private void clamp(float worldW, float worldH) {
        float hw = viewW / 2, hh = viewH / 2;
        x = worldW <= viewW ? worldW / 2 : MathUtils.clamp(x, hw, worldW - hw);
        y = worldH <= viewH ? worldH / 2 : MathUtils.clamp(y, hh, worldH - hh);
    }

    private void apply() {
        float s = trauma * trauma;
        float ox = MathUtils.random(-1f, 1f) * 14 * s + MathUtils.random(-1f, 1f) * jitter;
        float oy = MathUtils.random(-1f, 1f) * 14 * s;
        float rot = MathUtils.random(-1f, 1f) * 2.5f * s;
        cam.up.set(0, 1, 0);
        cam.direction.set(0, 0, -1);
        cam.rotate(rot);
        cam.position.set(x + ox, y + oy, 0);
        cam.update();
    }

    public void shake(float amount) { trauma = Math.min(1f, trauma + amount); }

    public void setJitter(float j) { jitter = j; }

    public float x() { return x; }
    public float y() { return y; }
    public float viewW() { return viewW; }
    public float viewH() { return viewH; }
}

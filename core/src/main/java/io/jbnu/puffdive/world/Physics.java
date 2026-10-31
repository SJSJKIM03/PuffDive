package io.jbnu.puffdive.world;

import com.badlogic.gdx.math.Rectangle;

/**
 * 타일 그리드 기반 충돌 처리.
 * 수업 예제는 모든 블록 Rectangle 과 겹침 검사를 했지만,
 * 여기서는 이동한 사각형이 걸치는 타일만 조회한다 (O(블록 수) → O(1)).
 */
public final class Physics {
    private static final int T = Level.TILE;

    private Physics() { }

    private static int fl(float v) { return (int) Math.floor(v); }

    /** @return 벽에 막혔으면 true */
    public static boolean moveX(Level lv, Rectangle r, float dx) {
        if (dx == 0) return false;
        r.x += dx;
        int y0 = fl(r.y / T), y1 = fl((r.y + r.height - 0.01f) / T);
        if (dx > 0) {
            int tx = fl((r.x + r.width - 0.01f) / T);
            for (int y = y0; y <= y1; y++) {
                if (lv.isSolid(tx, y)) { r.x = tx * T - r.width; return true; }
            }
        } else {
            int tx = fl(r.x / T);
            for (int y = y0; y <= y1; y++) {
                if (lv.isSolid(tx, y)) { r.x = (tx + 1) * T; return true; }
            }
        }
        return false;
    }

    /**
     * @param oneWay '-' 발판을 위에서 내려올 때만 바닥으로 취급할지
     * @return 바닥/천장에 막혔으면 true
     */
    public static boolean moveY(Level lv, Rectangle r, float dy, boolean oneWay) {
        if (dy == 0) return false;
        float prevBottom = r.y;
        r.y += dy;
        int x0 = fl(r.x / T), x1 = fl((r.x + r.width - 0.01f) / T);
        if (dy < 0) {
            int ty = fl(r.y / T);
            for (int x = x0; x <= x1; x++) {
                boolean platform = oneWay && lv.tile(x, ty) == '-' && prevBottom >= (ty + 1) * T - 0.5f;
                if (lv.isSolid(x, ty) || platform) { r.y = (ty + 1) * T; return true; }
            }
        } else {
            int ty = fl((r.y + r.height - 0.01f) / T);
            for (int x = x0; x <= x1; x++) {
                if (lv.isSolid(x, ty)) { r.y = ty * T - r.height; return true; }
            }
        }
        return false;
    }

    /** 사각형(약간 확장)이 위험 타일에 닿아 있는지 */
    public static boolean touchesHazard(Level lv, Rectangle r, float pad) {
        int x0 = fl((r.x - pad) / T), x1 = fl((r.x + r.width + pad - 0.01f) / T);
        int y0 = fl((r.y - pad) / T), y1 = fl((r.y + r.height + pad - 0.01f) / T);
        for (int x = x0; x <= x1; x++)
            for (int y = y0; y <= y1; y++)
                if (Level.isHazard(lv.tile(x, y))) return true;
        return false;
    }

    /** 수직 방향 바로 아래(gravity 방향) 타일들 중 조건을 만족하는 것이 있는지 */
    public static boolean solidBelow(Level lv, float x, float y) {
        return lv.isSolid(fl(x / T), fl(y / T)) || lv.tile(fl(x / T), fl(y / T)) == '-';
    }

    /** 적이 걸어도 되는 바닥인지 (가시·용암은 고체지만 밟으면 안 됨) */
    public static boolean walkableBelow(Level lv, float x, float y) {
        char c = lv.tile(fl(x / T), fl(y / T));
        return c == '#' || c == 'X' || c == '-';
    }
}

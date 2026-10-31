package io.jbnu.puffdive.world;

import com.badlogic.gdx.utils.Array;

/**
 * 텍스트 파일(assets/levels/*.txt)로 정의되는 데이터 기반 레벨.
 * 헤더(key=value) + "map:" 아래 ASCII 타일맵으로 구성된다.
 *
 * 타일 문자
 *   #  단단한 블록          X  금이 간 블록(STONE 능력으로 파괴)
 *   -  한 방향 발판          ^  가시(닿으면 피해)       ~  용암(닿으면 피해)
 * 엔티티 문자 (로드 시 빈칸으로 바뀌고 spawns 목록에 들어감)
 *   P 시작점, G 골 문, C 체크포인트, * 별, g 글리치 별, E 탈출 포탈, R 복귀 지점
 *   w 워커, f 파이어, s 스톤, b 박쥐
 */
public class Level {
    public static final int TILE = 32;

    public String name = "Level";
    public int theme;
    public boolean spikeCeiling;
    public boolean openRight;
    public int gravity = 1; // 1 = 아래로, -1 = 위로(글리치 월드)

    public int w, h;
    public char[][] t; // [x][y], y=0 이 맨 아래

    public float startX, startY;
    public final Array<Spawn> spawns = new Array<>();
    public final Array<Hint> hints = new Array<>();

    public static class Spawn {
        public final char c;
        public final int x, y;
        Spawn(char c, int x, int y) { this.c = c; this.x = x; this.y = y; }
    }

    public static class Hint {
        public final float x, y;
        public final String text;
        Hint(float x, float y, String text) { this.x = x; this.y = y; this.text = text; }
    }

    public static Level parse(String text) {
        Level lv = new Level();
        String[] lines = text.replace("\r", "").split("\n");
        Array<String> map = new Array<>();
        Array<String> rawHints = new Array<>();
        boolean inMap = false;
        for (String line : lines) {
            if (inMap) {
                if (line.trim().isEmpty()) continue;
                map.add(line);
                continue;
            }
            if (line.trim().equals("map:")) { inMap = true; continue; }
            int eq = line.indexOf('=');
            if (eq < 0) continue;
            String k = line.substring(0, eq).trim();
            String v = line.substring(eq + 1).trim();
            if (k.equals("name")) lv.name = v;
            else if (k.equals("theme")) lv.theme = Integer.parseInt(v);
            else if (k.equals("ceiling")) lv.spikeCeiling = Boolean.parseBoolean(v);
            else if (k.equals("openRight")) lv.openRight = Boolean.parseBoolean(v);
            else if (k.equals("gravity")) lv.gravity = Integer.parseInt(v);
            else if (k.equals("hint")) rawHints.add(v);
        }

        lv.h = map.size;
        for (String row : map) lv.w = Math.max(lv.w, row.length());
        lv.t = new char[lv.w][lv.h];
        for (int r = 0; r < lv.h; r++) {
            String row = map.get(r);
            int y = lv.h - 1 - r; // 파일 윗줄이 월드의 위쪽
            for (int x = 0; x < lv.w; x++) {
                char c = x < row.length() ? row.charAt(x) : '.';
                switch (c) {
                    case '#': case 'X': case '-': case '^': case '~': case '.':
                        lv.t[x][y] = c;
                        break;
                    case 'P':
                        lv.startX = x * TILE;
                        lv.startY = y * TILE;
                        lv.t[x][y] = '.';
                        break;
                    default:
                        lv.spawns.add(new Spawn(c, x, y));
                        lv.t[x][y] = '.';
                }
            }
        }

        for (String hv : rawHints) {
            String[] parts = hv.split(",", 3);
            int col = Integer.parseInt(parts[0].trim());
            int row = Integer.parseInt(parts[1].trim());
            lv.hints.add(new Hint(col * TILE, (lv.h - 1 - row) * TILE + 22, parts[2].trim()));
        }
        return lv;
    }

    public char tile(int x, int y) {
        if (y < 0 || y >= h) return '#';
        if (x < 0) return '#';
        if (x >= w) return openRight ? '.' : '#';
        return t[x][y];
    }

    public void set(int x, int y, char c) {
        if (x >= 0 && x < w && y >= 0 && y < h) t[x][y] = c;
    }

    /** 충돌 판정용. 가시와 용암도 "밟을 수 있는" 고체로 처리하고 피해는 별도 판정한다. */
    public boolean isSolid(int x, int y) {
        char c = tile(x, y);
        return c == '#' || c == 'X' || c == '^' || c == '~';
    }

    public static boolean isHazard(char c) {
        return c == '^' || c == '~';
    }

    public float width() { return w * TILE; }
    public float height() { return h * TILE; }
}

package io.jbnu.puffdive;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.Array;

/**
 * 키 → 행동(Action) 매핑 계층.
 * 게임 코드는 키 코드가 아니라 행동만 묻는다. 그래서 키를 여러 개 묶거나,
 * 테스트용 입력 스크립트(PUFF_SCRIPT 환경변수)로 자동 플레이를 돌릴 수 있다.
 * 스크립트 예: "30:+RIGHT,90:-RIGHT,95:*JUMP"  (+누름, -뗌, *한 프레임 탭)
 */
public class Controls {
    public enum Act { LEFT, RIGHT, UP, DOWN, JUMP, ACTION, DROP, PAUSE, CONFIRM, RESTART, QUIT }

    private static final int[][] KEYS = {
        {Input.Keys.LEFT, Input.Keys.A},
        {Input.Keys.RIGHT, Input.Keys.D},
        {Input.Keys.UP, Input.Keys.W},
        {Input.Keys.DOWN, Input.Keys.S},
        {Input.Keys.Z, Input.Keys.SPACE, Input.Keys.K},
        {Input.Keys.X, Input.Keys.J},
        {Input.Keys.C, Input.Keys.L},
        {Input.Keys.P, Input.Keys.ESCAPE},
        {Input.Keys.ENTER, Input.Keys.SPACE, Input.Keys.Z},
        {Input.Keys.R},
        {Input.Keys.Q},
    };

    private final boolean[] now = new boolean[Act.values().length];
    private final boolean[] before = new boolean[Act.values().length];
    private final boolean[] scripted = new boolean[Act.values().length];
    private final boolean[] tap = new boolean[Act.values().length];
    private final IntMap<Array<String>> script = new IntMap<>();

    public Controls(String scriptText) {
        if (scriptText == null || scriptText.trim().isEmpty()) return;
        for (String ev : scriptText.split(",")) {
            String[] p = ev.trim().split(":");
            if (p.length != 2) continue;
            int f = Integer.parseInt(p[0].trim());
            Array<String> list = script.get(f);
            if (list == null) { list = new Array<>(); script.put(f, list); }
            list.add(p[1].trim());
        }
    }

    public void update(long frame) {
        for (int i = 0; i < tap.length; i++) {
            if (tap[i]) { scripted[i] = false; tap[i] = false; }
        }
        Array<String> evs = script.get((int) frame);
        if (evs != null) {
            for (String e : evs) {
                char op = e.charAt(0);
                Act a = Act.valueOf(e.substring(1));
                if (op == '+') scripted[a.ordinal()] = true;
                else if (op == '-') scripted[a.ordinal()] = false;
                else if (op == '*') { scripted[a.ordinal()] = true; tap[a.ordinal()] = true; }
            }
        }
        for (Act a : Act.values()) {
            int i = a.ordinal();
            before[i] = now[i];
            boolean d = scripted[i];
            for (int k : KEYS[i]) d |= Gdx.input.isKeyPressed(k);
            now[i] = d;
        }
    }

    public boolean held(Act a) { return now[a.ordinal()]; }

    public boolean pressed(Act a) { return now[a.ordinal()] && !before[a.ordinal()]; }

    public boolean released(Act a) { return !now[a.ordinal()] && before[a.ordinal()]; }
}

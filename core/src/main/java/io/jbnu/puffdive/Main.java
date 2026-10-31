package io.jbnu.puffdive;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.utils.IntMap;

import java.util.zip.Deflater;

import io.jbnu.puffdive.screens.GameScreen;
import io.jbnu.puffdive.screens.TitleScreen;

/**
 * PUFF DIVE - 아래로 파고드는 2D 플랫포머.
 * Game + Screen 구조: Title → Game(스테이지 1~3, 숨은 글리치 월드) → Clear → 다시 처음부터.
 */
public class Main extends Game {
    public static final float VW = 640, VH = 480;

    public SpriteBatch batch;
    public ShapeRenderer shapes;
    public BitmapFont font, bigFont; // 갈무리11 (한글 픽셀 폰트, OFL)
    private FreeTypeFontGenerator fontGen, bigGen;
    public Assets assets;
    public Audio audio;
    public Controls controls;
    public Session session = new Session();

    private long frame;
    private final IntMap<String> shots = new IntMap<>();
    private int exitFrame = -1;

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        fontGen = new FreeTypeFontGenerator(Gdx.files.internal("fonts/Galmuri11.ttf"));
        bigGen = new FreeTypeFontGenerator(Gdx.files.internal("fonts/Galmuri11-Bold.ttf"));
        font = makeFont(fontGen, 12);
        bigFont = makeFont(bigGen, 24);
        assets = new Assets();
        controls = new Controls(System.getenv("PUFF_SCRIPT"));
        audio = new Audio();

        // 문서/테스트용: PUFF_SHOTS="120:/path/a.png,300:/path/b.png" → 해당 프레임 스크린샷 저장 후 종료
        String spec = System.getenv("PUFF_SHOTS");
        if (spec != null && !spec.trim().isEmpty()) {
            for (String s : spec.split(",")) {
                int c = s.indexOf(':');
                int f = Integer.parseInt(s.substring(0, c).trim());
                shots.put(f, s.substring(c + 1).trim());
                exitFrame = Math.max(exitFrame, f + 1);
            }
        }

        String lv = System.getenv("PUFF_LEVEL");
        if (lv != null && !lv.trim().isEmpty()) setScreen(new GameScreen(this, Integer.parseInt(lv.trim())));
        else setScreen(new TitleScreen(this));
    }

    /** incremental 모드: 화면에 처음 나오는 글자만 그때그때 생성 (한글 11,172자를 미리 굽지 않음) */
    private static BitmapFont makeFont(FreeTypeFontGenerator gen, int size) {
        FreeTypeFontParameter p = new FreeTypeFontParameter();
        p.size = size;
        p.incremental = true;
        p.minFilter = Texture.TextureFilter.Linear;
        p.magFilter = Texture.TextureFilter.Linear;
        BitmapFont f = gen.generateFont(p);
        f.setUseIntegerPositions(false);
        return f;
    }

    /** 세션 초기화 후 1스테이지부터 */
    public void newGame() {
        session = new Session();
        go(new GameScreen(this, 0));
    }

    public void go(Screen next) {
        Screen old = getScreen();
        setScreen(next);
        if (old != null) old.dispose();
    }

    @Override
    public void render() {
        controls.update(frame);
        if (controls.pressed(Controls.Act.MUTE)) audio.toggleMute();
        super.render();
        String path = shots.get((int) frame);
        if (path != null) {
            Pixmap pm = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
            PixmapIO.writePNG(Gdx.files.absolute(path), pm, Deflater.DEFAULT_COMPRESSION, true);
            pm.dispose();
        }
        if (frame == exitFrame) Gdx.app.exit();
        frame++;
    }

    @Override
    public void dispose() {
        if (getScreen() != null) getScreen().dispose();
        batch.dispose();
        shapes.dispose();
        font.dispose();
        bigFont.dispose();
        fontGen.dispose();
        bigGen.dispose();
        assets.dispose();
        audio.dispose();
    }
}

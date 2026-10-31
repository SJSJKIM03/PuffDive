package io.jbnu.puffdive.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

import io.jbnu.puffdive.Assets;
import io.jbnu.puffdive.Audio;
import io.jbnu.puffdive.Audio.Sfx;
import io.jbnu.puffdive.Controls;
import io.jbnu.puffdive.Controls.Act;
import io.jbnu.puffdive.Session;
import io.jbnu.puffdive.world.Entities.Ability;
import io.jbnu.puffdive.world.Entities.Area;
import io.jbnu.puffdive.world.Entities.Enemy;
import io.jbnu.puffdive.world.Entities.Item;
import io.jbnu.puffdive.world.Entities.Player;
import io.jbnu.puffdive.world.Entities.Popup;
import io.jbnu.puffdive.world.Entities.Projectile;

/** 한 스테이지의 게임 로직과 렌더링 */
public class World {
    public static final int T = Level.TILE;

    // 물리 상수
    private static final float MOVE = 190, MOVE_SLOW = 150;
    private static final float ACCEL_GROUND = 1700, ACCEL_AIR = 1100;
    private static final float GRAVITY = 1500, JUMP = 520, MAX_FALL = 620;
    private static final float PUFF_V = 250, PUFF_GRAVITY = 650, PUFF_MAX_FALL = 110;
    private static final float STONE_GRAVITY = 2600, STONE_MAX_FALL = 900;
    private static final float INHALE_RANGE = 3.6f * T, INHALE_PULL = 280;
    private static final float CEIL_GAP = 11 * T;
    private static final float CEIL_DELAY = 4f, CEIL_SPEED = 30, CEIL_MAX_SPEED = 50;

    private final Controls in;
    private final Audio audio;
    private final Session session;
    public final int levelIndex;
    public final Area main;
    private Area glitch;
    public Area cur;

    public final Player p = new Player();
    public final CameraRig rig;
    private final Particles particles = new Particles();
    private final Array<Projectile> shots = new Array<>();
    private final Array<Popup> popups = new Array<>();
    private final GlyphLayout layout = new GlyphLayout();

    private float respawnX, respawnY;
    private float knock;
    public boolean cleared;
    public int levelStars;
    public float clock;

    // 추격하는 가시 천장 (3스테이지)
    public float ceilY;
    private float ceilTimer;
    public float danger;

    public World(Session session, Controls in, Audio audio, int levelIndex, OrthographicCamera cam, float viewW, float viewH) {
        this.session = session;
        this.audio = audio;
        this.in = in;
        this.levelIndex = levelIndex;
        this.main = new Area(Level.parse(Gdx.files.internal(Session.LEVELS[levelIndex]).readString("UTF-8")));
        this.cur = main;
        this.rig = new CameraRig(cam, viewW, viewH);
        Level lv = main.level;
        p.box.setPosition(lv.startX + 4, lv.startY);
        respawnX = p.box.x;
        respawnY = p.box.y;
        ceilY = respawnY + CEIL_GAP;
        rig.snap(cx(p.box), cy(p.box), lv.width(), lv.height());
    }

    public Level level() { return cur.level; }
    public boolean inGlitch() { return cur != main; }
    public int totalStars() { return main.count(Item.Kind.STAR); }

    private void sfx(Sfx s, float vol) {
        if (audio != null) audio.play(s, vol);
    }

    private void sfx(Sfx s) { sfx(s, 1f); }

    private static float cx(Rectangle r) { return r.x + r.width / 2; }
    private static float cy(Rectangle r) { return r.y + r.height / 2; }

    // =====================================================================
    // UPDATE
    // =====================================================================
    public void update(float dt) {
        clock += dt;
        Level lv = cur.level;
        if (!cleared) {
            updatePlayer(dt, lv);
            updateEnemies(dt, lv);
            updateShots(dt, lv);
            updateItems();
            updateCeiling(dt, lv);
        }
        particles.update(dt);
        for (int i = popups.size - 1; i >= 0; i--) {
            Popup pp = popups.get(i);
            pp.life -= dt;
            pp.y += 30 * dt;
            if (pp.life <= 0) popups.removeIndex(i);
        }
        rig.setJitter(0); // 숨은 구역도 카메라 떨림 없음 (가독성)
        rig.update(cx(p.box), cy(p.box), p.vx, p.vy, lv.gravity, lv.width(), lv.height(), dt);
    }

    private void updatePlayer(float dt, Level lv) {
        int g = lv.gravity;
        p.anim += dt;
        p.invuln -= dt;
        p.fireCooldown -= dt;
        p.dropTimer -= dt;
        knock -= dt;
        p.squash += (0 - p.squash) * Math.min(1, dt * 12);

        boolean left = in.held(Act.LEFT), right = in.held(Act.RIGHT);
        if (knock > 0) left = right = false;

        if (in.pressed(Act.DROP) && p.ability != Ability.NONE) loseAbility(true);

        // ---- 행동 버튼 (능력별 상태 머신) ----
        p.inhaling = false;
        if (p.stone) {
            if (in.pressed(Act.ACTION)) {
                p.stone = false;
                puffBurst(cx(p.box), cy(p.box), 0.7f, 0.7f, 0.75f);
            }
        } else if (p.mouth != null) {
            if (in.pressed(Act.ACTION)) spitStar();
            else if (in.pressed(Act.DOWN)) swallow();
        } else if (p.puffing && in.pressed(Act.ACTION)) {
            p.puffing = false;
            float dir = p.facingRight ? 1 : -1;
            shots.add(new Projectile(Projectile.Kind.AIR, cx(p.box) + dir * 16, cy(p.box), dir * 320, 0, 0.3f));
        } else {
            switch (p.ability) {
                case NONE:
                    if (in.held(Act.ACTION) && knock <= 0) p.inhaling = true;
                    break;
                case FIRE:
                    if (in.held(Act.ACTION) && p.fireCooldown <= 0) shootFire();
                    break;
                case STONE:
                    if (in.pressed(Act.ACTION)) {
                        p.stone = true;
                        p.puffing = false;
                        p.vx = 0;
                        p.vy = -g * 200;
                        p.squash = 0.4f;
                    }
                    break;
            }
        }

        // ---- 좌우 이동 ----
        float speed = (p.mouth != null || p.puffing) ? MOVE_SLOW : MOVE;
        float target = 0;
        if (!p.inhaling && !p.stone) {
            if (left) target -= speed;
            if (right) target += speed;
            if (left && !right) p.facingRight = false;
            if (right && !left) p.facingRight = true;
        }
        if (knock <= 0) {
            float acc = p.grounded ? ACCEL_GROUND : ACCEL_AIR;
            p.vx = approach(p.vx, target, acc * dt);
        }

        // ---- 점프 / 퍼프 / 발판 내려가기 ----
        if (in.pressed(Act.JUMP) && !p.stone && knock <= 0) {
            if (p.grounded) {
                if (g > 0 && in.held(Act.DOWN) && standingOnPlatform(lv)) {
                    p.dropTimer = 0.25f;
                    p.box.y -= 1;
                } else {
                    p.vy = JUMP * g;
                    p.grounded = false;
                    p.squash = -0.35f;
                    sfx(Sfx.JUMP, 0.7f);
                    particles.emit(cx(p.box), feetY(g), 6, 60, 1, 1, 1, 5, 0.35f, 0);
                }
            } else if (p.mouth == null && !p.inhaling && p.puffsUsed < Player.MAX_PUFFS) {
                p.puffing = true;
                p.puffsUsed++;
                p.vy = PUFF_V * g;
                p.squash = -0.25f;
                puffBurst(cx(p.box), feetY(g), 1, 1, 1);
                sfx(Sfx.PUFF);
            }
        }
        // 가변 점프: 상승 중 버튼을 떼면 짧게
        if (in.released(Act.JUMP) && !p.puffing && p.vy * g > 0) p.vy *= 0.5f;

        // ---- 중력 ----
        float gAcc = p.stone ? STONE_GRAVITY : (p.puffing ? PUFF_GRAVITY : GRAVITY);
        float maxFall = p.stone ? STONE_MAX_FALL : (p.puffing ? PUFF_MAX_FALL : MAX_FALL);
        p.vy -= gAcc * g * dt;
        if (g > 0) p.vy = Math.max(p.vy, -maxFall); else p.vy = Math.min(p.vy, maxFall);

        // ---- 이동 + 충돌 ----
        if (Physics.moveX(lv, p.box, p.vx * dt)) p.vx = 0;
        float fallSpeed = -p.vy * g;
        float dy = p.vy * dt;
        boolean wasGrounded = p.grounded;
        p.grounded = false;
        if (Physics.moveY(lv, p.box, dy, g > 0 && p.dropTimer <= 0)) {
            boolean towardFloor = dy * g < 0;
            if (towardFloor) {
                if (p.stone && breakCracked(lv)) {
                    p.vy = -g * 300; // 부수고 계속 떨어짐
                } else {
                    if (!wasGrounded && fallSpeed > 380) {
                        p.squash = 0.35f;
                        particles.emit(cx(p.box), feetY(g), 8, 90, 1, 1, 1, 5, 0.4f, 0);
                        if (p.stone) { rig.shake(0.45f); sfx(Sfx.CRASH, 0.6f); }
                    }
                    p.grounded = true;
                    p.puffing = false;
                    p.puffsUsed = 0;
                    p.vy = 0;
                }
            } else {
                p.vy = 0;
            }
        }

        // ---- 흡입 ----
        if (p.inhaling) inhale(dt);

        // ---- 위험 지형 / 맵 밖 ----
        if (Physics.touchesHazard(lv, p.box, 1.5f)) {
            hazardHit("아야!");
            return;
        }
        if (!inGlitch() && lv.openRight && p.box.x > lv.width() + 6) {
            enterGlitch();
            return;
        }
        if (p.box.y < -96 || p.box.y > lv.height() + 96) hazardHit("추락!");
    }

    private float feetY(int g) { return g > 0 ? p.box.y : p.box.y + p.box.height; }

    private boolean standingOnPlatform(Level lv) {
        int ty = (int) Math.floor((p.box.y - 1) / T);
        int x0 = (int) Math.floor(p.box.x / T), x1 = (int) Math.floor((p.box.x + p.box.width - 0.01f) / T);
        boolean any = false;
        for (int x = x0; x <= x1; x++) {
            if (lv.isSolid(x, ty)) return false;
            if (lv.tile(x, ty) == '-') any = true;
        }
        return any;
    }

    /** 스톤 상태로 떨어지며 착지한 타일들 중 금 간 블록 파괴 */
    private boolean breakCracked(Level lv) {
        int g = lv.gravity;
        int ty = g > 0 ? (int) Math.floor((p.box.y - 1) / T) : (int) Math.floor((p.box.y + p.box.height + 1) / T);
        int x0 = (int) Math.floor(p.box.x / T) - 1, x1 = (int) Math.floor((p.box.x + p.box.width) / T) + 1;
        boolean broke = false;
        for (int x = x0; x <= x1; x++) {
            if (lv.tile(x, ty) == 'X') {
                lv.set(x, ty, '.');
                particles.emit(x * T + 16, ty * T + 16, 14, 220, 0.6f, 0.65f, 0.8f, 7, 0.7f, 600);
                broke = true;
            }
        }
        if (broke) {
            rig.shake(0.6f);
            sfx(Sfx.CRASH);
            popups.add(new Popup("와장창!", cx(p.box), p.box.y + 40, 0.8f, 0.9f, 1));
        }
        return broke;
    }

    private void inhale(float dt) {
        float dir = p.facingRight ? 1 : -1;
        float mouthX = p.facingRight ? p.box.x + p.box.width : p.box.x;
        Rectangle cone = new Rectangle(p.facingRight ? mouthX : mouthX - INHALE_RANGE, p.box.y - 10,
            INHALE_RANGE, p.box.height + 20);
        // 빨려 들어가는 기류 파티클
        for (int i = 0; i < 2; i++) {
            float px = mouthX + dir * MathUtils.random(20, INHALE_RANGE);
            float py = cy(p.box) + MathUtils.random(-16, 16);
            particles.stream(px, py, -dir * 420, (cy(p.box) - py) * 3, 1, 1, 1, 3, 0.18f);
        }
        for (Enemy e : cur.enemies) {
            if (e.dead || !e.box.overlaps(cone)) continue;
            e.pulled = true;
            float ex = cx(e.box);
            e.box.x += Math.signum(mouthX - ex) * INHALE_PULL * dt;
            e.box.y += (p.box.y - e.box.y) * Math.min(1, dt * 8);
            if (Math.abs(ex - mouthX) < 18 && Math.abs(cy(e.box) - cy(p.box)) < 20) {
                killEnemy(e, false);
                p.mouth = e.type;
                p.squash = -0.3f;
                sfx(Sfx.GULP);
                popups.add(new Popup("꿀꺽!", cx(p.box), p.box.y + 36, 1, 0.8f, 0.9f));
                break;
            }
        }
    }

    private void spitStar() {
        float dir = p.facingRight ? 1 : -1;
        shots.add(new Projectile(Projectile.Kind.STAR, cx(p.box) + dir * 18, cy(p.box), dir * 430, 0, 1.4f));
        p.mouth = null;
        p.squash = 0.25f;
        sfx(Sfx.SPIT);
        rig.shake(0.12f);
    }

    private void swallow() {
        Enemy.Type t = p.mouth;
        p.mouth = null;
        Ability a = Ability.NONE;
        if (t == Enemy.Type.FIRE) a = Ability.FIRE;
        else if (t == Enemy.Type.STONE) a = Ability.STONE;
        if (a == Ability.NONE) {
            popups.add(new Popup("...아무 능력 없음", cx(p.box), p.box.y + 36, 0.9f, 0.9f, 0.9f));
            return;
        }
        p.ability = a;
        sfx(Sfx.POWER);
        session.abilitiesCopied++;
        particles.emit(cx(p.box), cy(p.box), 30, 260, 1, 0.95f, 0.4f, 6, 0.6f, 0);
        rig.shake(0.25f);
        popups.add(new Popup(a == Ability.FIRE ? "불꽃 능력!" : "돌 능력!", cx(p.box), p.box.y + 40,
            a == Ability.FIRE ? 1f : 0.75f, a == Ability.FIRE ? 0.5f : 0.75f, a == Ability.FIRE ? 0.1f : 0.8f));
    }

    private void shootFire() {
        float dir = p.facingRight ? 1 : -1;
        shots.add(new Projectile(Projectile.Kind.FIRE, cx(p.box) + dir * 14, cy(p.box) + MathUtils.random(-4, 4),
            dir * 380, 0, 0.9f));
        p.fireCooldown = 0.28f;
        sfx(Sfx.SHOOT, 0.45f);
    }

    private void loseAbility(boolean voluntary) {
        if (p.ability == Ability.NONE) return;
        popups.add(new Popup(voluntary ? "능력 해제" : "능력을 잃었다!", cx(p.box), p.box.y + 40, 0.8f, 0.8f, 0.8f));
        p.ability = Ability.NONE;
        p.stone = false;
        puffBurst(cx(p.box), cy(p.box), 1, 1, 0.5f);
    }

    private void puffBurst(float x, float y, float r, float g, float b) {
        particles.emit(x, y, 10, 110, r, g, b, 7, 0.4f, 0);
    }

    private void hurt(float fromX) {
        if (p.invuln > 0 || p.stone || p.hp <= 0) return;
        p.hp--;
        p.invuln = 1.4f;
        knock = 0.25f;
        int g = cur.level.gravity;
        p.vx = (cx(p.box) < fromX ? -1 : 1) * 230;
        p.vy = 300 * g;
        p.puffing = false;
        p.mouth = null;
        loseAbility(false);
        rig.shake(0.5f);
        sfx(Sfx.HURT);
        particles.emit(cx(p.box), cy(p.box), 16, 200, 1, 0.3f, 0.4f, 6, 0.5f, 0);
    }

    private void hazardHit(String msg) {
        if (p.hp <= 0) return;
        p.hp--;
        rig.shake(0.6f);
        sfx(Sfx.HURT);
        particles.emit(cx(p.box), cy(p.box), 22, 240, 1, 0.3f, 0.3f, 6, 0.6f, 0);
        if (p.hp <= 0) return; // GameScreen 이 쓰러짐 처리
        p.box.setPosition(respawnX, respawnY);
        p.vx = p.vy = 0;
        p.invuln = 1.4f;
        p.puffing = false;
        p.stone = false;
        p.mouth = null;
        knock = 0;
        ceilY = respawnY + CEIL_GAP;
        ceilTimer = 0;
        popups.add(new Popup(msg, cx(p.box), p.box.y + 40, 1, 0.4f, 0.4f));
    }

    private void killEnemy(Enemy e, boolean burst) {
        e.dead = true;
        e.respawnTimer = e.gives() != Ability.NONE ? 6f : 0f; // 능력 적은 다시 나타나 소프트락 방지
        if (burst) {
            particles.emit(cx(e.box), cy(e.box), 18, 220, 1, 0.9f, 0.3f, 6, 0.5f, 0);
            rig.shake(0.15f);
            sfx(Sfx.CRASH, 0.35f);
        }
    }

    private void updateEnemies(float dt, Level lv) {
        int g = lv.gravity;
        float pcx = cx(p.box), pcy = cy(p.box);
        for (Enemy e : cur.enemies) {
            if (e.dead) {
                if (e.respawnTimer > 0) {
                    e.respawnTimer -= dt;
                    if (e.respawnTimer <= 0) {
                        if (Math.abs(e.spawnX - pcx) + Math.abs(e.spawnY - pcy) > 4 * T) {
                            e.reset();
                            particles.emit(cx(e.box), cy(e.box), 12, 120, 1, 1, 1, 6, 0.5f, 0);
                        } else e.respawnTimer = 0.5f;
                    }
                }
                continue;
            }
            e.t += dt;
            if (e.pulled) {
                e.pulled = false;
            } else if (e.type == Enemy.Type.BAT) {
                e.box.x = e.spawnX + 3 + MathUtils.sin(e.t * 1.3f) * 72;
                e.box.y = e.spawnY + 6 + MathUtils.sin(e.t * 3.1f) * 16;
                e.dir = MathUtils.cos(e.t * 1.3f) > 0 ? 1 : -1;
            } else {
                e.vy -= GRAVITY * g * dt;
                if (g > 0) e.vy = Math.max(e.vy, -MAX_FALL); else e.vy = Math.min(e.vy, MAX_FALL);
                if (Physics.moveX(lv, e.box, e.dir * e.speed() * dt)) e.dir = -e.dir;
                float edy = e.vy * dt;
                e.grounded = false;
                if (Physics.moveY(lv, e.box, edy, g > 0)) {
                    e.grounded = edy * g < 0;
                    e.vy = 0;
                }
                if (e.grounded) {
                    float aheadX = e.dir > 0 ? e.box.x + e.box.width + 2 : e.box.x - 2;
                    float footY = g > 0 ? e.box.y - 2 : e.box.y + e.box.height + 2;
                    if (!Physics.walkableBelow(lv, aheadX, footY)) e.dir = -e.dir; // 가시·용암·낭떠러지 앞에서 돌아섬
                }
                if (e.type == Enemy.Type.FIRE) {
                    float dx = pcx - cx(e.box), dyy = pcy - cy(e.box);
                    if (Math.abs(dx) < 7 * T && Math.abs(dyy) < 1.6f * T) {
                        e.shootTimer -= dt;
                        if (e.shootTimer <= 0) {
                            e.dir = dx > 0 ? 1 : -1;
                            float len = Math.max(1, (float) Math.sqrt(dx * dx + dyy * dyy));
                            shots.add(new Projectile(Projectile.Kind.ENEMY_FLAME, cx(e.box) + e.dir * 12, cy(e.box),
                                dx / len * 170, dyy / len * 170, 3f));
                            e.shootTimer = 2.6f;
                        }
                    }
                }
            }
            if (e.box.y < -200) e.dead = true;
            float underY = g > 0 ? e.box.y - 1 : e.box.y + e.box.height + 1;
            boolean onHazard = Level.isHazard(lv.tile((int) Math.floor(cx(e.box) / T), (int) Math.floor(underY / T)));
            if (!e.dead && e.type != Enemy.Type.BAT && (onHazard || Physics.touchesHazard(lv, e.box, -3f))) {
                killEnemy(e, true); // 위험 지형에 떨어지면 적도 사라짐
                continue;
            }
            // 플레이어와 접촉
            if (!e.dead && e.box.overlaps(p.box)) {
                if (p.stone) {
                    killEnemy(e, true);
                    popups.add(new Popup("쾅!", cx(e.box), e.box.y + 30, 0.8f, 0.8f, 0.9f));
                } else if (!p.inhaling) {
                    hurt(cx(e.box));
                }
            }
        }
    }

    private void updateShots(float dt, Level lv) {
        for (int i = shots.size - 1; i >= 0; i--) {
            Projectile s = shots.get(i);
            s.life -= dt;
            s.rot += dt * 720;
            s.box.x += s.vx * dt;
            s.box.y += s.vy * dt;
            if (s.kind == Projectile.Kind.FIRE && MathUtils.randomBoolean(0.6f))
                particles.emit(cx(s.box), cy(s.box), 1, 30, 1, MathUtils.random(0.3f, 0.8f), 0.1f, 5, 0.25f, -80);
            if (s.kind == Projectile.Kind.STAR && MathUtils.randomBoolean(0.5f))
                particles.emit(cx(s.box), cy(s.box), 1, 20, 1, 0.95f, 0.5f, 4, 0.3f, 0);
            int tx = (int) Math.floor(cx(s.box) / T), ty = (int) Math.floor(cy(s.box) / T);
            if (lv.isSolid(tx, ty)) s.dead = true;
            if (s.friendly()) {
                for (Enemy e : cur.enemies) {
                    if (e.dead || !e.box.overlaps(s.box)) continue;
                    killEnemy(e, true);
                    if (s.kind != Projectile.Kind.STAR) s.dead = true; // 별은 관통
                }
            } else if (s.box.overlaps(p.box)) {
                hurt(cx(s.box));
                s.dead = true;
            }
            if (s.life <= 0 || s.dead) {
                if (s.dead) particles.emit(cx(s.box), cy(s.box), 6, 100, 1, 0.8f, 0.4f, 4, 0.3f, 0);
                shots.removeIndex(i);
            }
        }
    }

    private void updateItems() {
        for (Item it : cur.items) {
            if (it.taken || !it.box.overlaps(p.box)) continue;
            switch (it.kind) {
                case STAR:
                    it.taken = true;
                    levelStars++;
                    particles.emit(cx(it.box), cy(it.box), 14, 160, 1, 0.9f, 0.3f, 5, 0.5f, 0);
                    sfx(Sfx.STAR, 0.8f);
                    popups.add(new Popup("+1", cx(it.box), it.box.y + 24, 1, 0.9f, 0.3f));
                    break;
                case GLITCH_STAR:
                    it.taken = true;
                    session.glitchStar = true;
                    rig.shake(0.5f);
                    particles.emit(cx(it.box), cy(it.box), 40, 300, 0.2f, 1, 1, 6, 0.8f, 0);
                    sfx(Sfx.POWER);
                    popups.add(new Popup("비밀 글리치 별!", cx(it.box), it.box.y + 30, 0.3f, 1, 1));
                    break;
                case CHECKPOINT:
                    if (!it.active) {
                        it.active = true;
                        respawnX = it.box.x + 4;
                        respawnY = it.box.y;
                        particles.emit(cx(it.box), it.box.y + 40, 20, 180, 0.3f, 1, 0.5f, 5, 0.6f, 0);
                        p.hp = Player.MAX_HP; // 체크포인트는 체력 회복
                        sfx(Sfx.STAR);
                        popups.add(new Popup("체크포인트 · 체력 회복", cx(it.box), it.box.y + 56, 0.4f, 1, 0.5f));
                    }
                    break;
                case GOAL:
                    if (!inGlitch()) {
                        cleared = true;
                        session.stars += levelStars;
                        sfx(Sfx.CLEAR);
                        particles.emit(cx(it.box), cy(it.box), 60, 320, 1, 0.9f, 0.3f, 7, 1f, 200);
                        rig.shake(0.3f);
                    }
                    break;
                case PORTAL:
                    exitGlitch();
                    return;
                default:
                    break;
            }
        }
    }

    // ---- 숨겨진 글리치 월드 (맵 밖으로 나가면 진입, 중력 반전) ----
    private void enterGlitch() {
        if (glitch == null) glitch = new Area(Level.parse(Gdx.files.internal(Session.GLITCH).readString("UTF-8")));
        cur = glitch;
        Level gl = glitch.level;
        p.box.setPosition(gl.startX + 4, gl.startY);
        p.vx = p.vy = 0;
        p.puffing = p.stone = false;
        p.puffsUsed = 0;
        respawnX = p.box.x;
        respawnY = p.box.y;
        shots.clear();
        rig.shake(0.8f);
        rig.snap(cx(p.box), cy(p.box), gl.width(), gl.height());
        sfx(Sfx.GLITCH);
        popups.add(new Popup("??? 맵 밖으로 나갔다 ???", cx(p.box), p.box.y + 50, 1, 0.2f, 1));
    }

    private void exitGlitch() {
        cur = main;
        Item ret = main.find(Item.Kind.RETURN);
        float rx = ret != null ? ret.box.x : main.level.startX;
        float ry = ret != null ? ret.box.y : main.level.startY;
        p.box.setPosition(rx, ry);
        p.vx = p.vy = 0;
        p.puffing = p.stone = false;
        p.puffsUsed = 0;
        respawnX = rx;
        respawnY = ry;
        shots.clear();
        rig.shake(0.6f);
        rig.snap(cx(p.box), cy(p.box), main.level.width(), main.level.height());
        particles.emit(cx(p.box), cy(p.box), 40, 260, 1, 0.3f, 1, 6, 0.8f, 0);
        sfx(Sfx.GLITCH, 0.6f);
        popups.add(new Popup("지름길!", cx(p.box), p.box.y + 40, 1, 0.4f, 1));
    }

    private void updateCeiling(float dt, Level lv) {
        danger = 0;
        if (inGlitch() || !lv.spikeCeiling) return;
        ceilTimer += dt;
        if (ceilTimer > CEIL_DELAY) {
            float speed = Math.min(CEIL_SPEED + (ceilTimer - CEIL_DELAY) * 0.4f, CEIL_MAX_SPEED);
            ceilY -= speed * dt;
        }
        float top = p.box.y + p.box.height;
        if (ceilY > top + CEIL_GAP) ceilY = top + CEIL_GAP; // 고무줄: 너무 멀어지지 않게
        float gap = ceilY - top;
        danger = MathUtils.clamp(1 - gap / (5f * T), 0, 1);
        if (danger > 0.75f) rig.shake(dt * 0.5f);
        if (gap <= 0) hazardHit("깔렸다!");
    }

    private static float approach(float v, float target, float step) {
        if (v < target) return Math.min(v + step, target);
        return Math.max(v - step, target);
    }

    // =====================================================================
    // RENDER
    // =====================================================================
    private static final float[][] BG_TOP = {{0.42f, 0.72f, 1f}, {0.06f, 0.04f, 0.18f}, {0.12f, 0.02f, 0.02f}, {0.02f, 0f, 0.05f}};
    private static final float[][] BG_BOT = {{0.82f, 0.94f, 1f}, {0.14f, 0.2f, 0.42f}, {0.5f, 0.12f, 0.04f}, {0.12f, 0f, 0.2f}};
    private static final float[][] BLOCK = {{0.62f, 0.86f, 0.5f}, {0.5f, 0.6f, 0.95f}, {0.62f, 0.42f, 0.36f}, {0.9f, 0.3f, 0.9f}};
    private static final float[][] CAP = {{0.3f, 0.78f, 0.32f}, {0.6f, 0.95f, 1f}, {1f, 0.5f, 0.12f}, {0.2f, 1f, 1f}};

    public void render(SpriteBatch batch, ShapeRenderer shapes, BitmapFont font, Assets a) {
        Level lv = cur.level;
        OrthographicCamera cam = rig.cam;
        int th = MathUtils.clamp(lv.theme, 0, 3);
        float vw = rig.viewW() + 80, vh = rig.viewH() + 80;
        float left = cam.position.x - vw / 2, bottom = cam.position.y - vh / 2;

        // ---- 배경 그라데이션 + 패럴랙스 장식 ----
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(cam.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        float[] bt = BG_TOP[th], bb = BG_BOT[th];
        Color top = new Color(bt[0], bt[1], bt[2], 1), bot = new Color(bb[0], bb[1], bb[2], 1);
        shapes.rect(left, bottom, vw, vh, bot, bot, top, top);
        drawDecor(shapes, th, cam, lv);
        shapes.end();

        batch.setProjectionMatrix(cam.combined);
        batch.begin();
        // ---- 타일 ----
        int x0 = Math.max(0, (int) (left / T)), x1 = Math.min(lv.w - 1, (int) ((left + vw) / T));
        int y0 = Math.max(0, (int) (bottom / T)), y1 = Math.min(lv.h - 1, (int) ((bottom + vh) / T));
        float[] bc = BLOCK[th], cc = CAP[th];
        if (th == 3) { // 색이 아주 천천히 보라 <-> 분홍 사이를 오감
            float k = 0.5f + 0.5f * MathUtils.sin(clock * 0.6f);
            bc = new float[]{0.55f + 0.25f * k, 0.35f, 0.85f - 0.15f * k};
        }
        int g = lv.gravity;
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                char c = lv.t[x][y];
                float px = x * T, py = y * T;
                switch (c) {
                    case '#':
                        batch.setColor(bc[0], bc[1], bc[2], 1);
                        batch.draw(a.block, px, py, T, T);
                        char open = lv.tile(x, y + g);
                        if (open == '.' || open == '-') { // 노출된 윗면에 테마 색 띠
                            batch.setColor(cc[0], cc[1], cc[2], 1);
                            batch.draw(a.white, px, g > 0 ? py + T - 6 : py, T, 6);
                        }
                        break;
                    case 'X':
                        batch.setColor(bc[0] * 0.85f, bc[1] * 0.85f, bc[2] * 0.95f, 1);
                        batch.draw(a.cracked, px, py, T, T);
                        break;
                    case '-':
                        batch.setColor(bc[0], bc[1], bc[2], 1);
                        batch.draw(a.oneway, px, py, T, T);
                        break;
                    case '^':
                        batch.setColor(1, 1, 1, 1);
                        boolean down = g < 0 || lv.isSolid(x, y + 1) && !lv.isSolid(x, y - 1);
                        batch.draw(a.spike, px, py, T, T, 0, 0, 32, 32, false, down);
                        break;
                    case '~':
                        float w = MathUtils.sin(clock * 3 + x * 0.9f);
                        batch.setColor(0.95f, 0.3f + 0.08f * w, 0.05f, 1);
                        batch.draw(a.white, px, py, T, T);
                        batch.setColor(1f, 0.75f, 0.2f, 1);
                        batch.draw(a.white, px, py + T - 7 + w * 2, T, 5);
                        if (MathUtils.randomBoolean(0.02f))
                            particles.emit(px + MathUtils.random(T), py + T, 1, 40, 1, 0.6f, 0.1f, 4, 0.6f, -150);
                        break;
                    default:
                        break;
                }
            }
        }
        batch.setColor(1, 1, 1, 1);

        // ---- 힌트 텍스트 ----
        for (Level.Hint h : lv.hints) {
            if (th != 0) {
                layout.setText(font, h.text);
                batch.setColor(0, 0, 0, 0.55f);
                batch.draw(a.white, h.x - 6, h.y - layout.height - 6, layout.width + 12, layout.height + 12);
                batch.setColor(1, 1, 1, 1);
            }
            font.setColor(0, 0, 0, 0.55f);
            font.draw(batch, h.text, h.x + 1.5f, h.y - 1.5f);
            font.setColor(th == 0 ? new Color(0.15f, 0.2f, 0.45f, 1) : new Color(1, 1, 1, 0.92f));
            font.draw(batch, h.text, h.x, h.y);
        }
        font.setColor(Color.WHITE);

        // ---- 아이템 ----
        for (Item it : cur.items) {
            if (it.taken) continue;
            float bob = MathUtils.sin(clock * 3 + it.box.x) * 3;
            switch (it.kind) {
                case STAR:
                    draw(batch, a.star, cx(it.box), it.box.y - 4 + bob, 28, 28, false, false, 1, 1, MathUtils.sin(clock * 2) * 15, 0.5f);
                    break;
                case GLITCH_STAR:
                    Color gc = new Color().fromHsv((clock * 60) % 360, 0.6f, 1);
                    batch.setColor(gc);
                    draw(batch, a.star, cx(it.box), it.box.y - 8 + bob, 36, 36, false, false, 1, 1, clock * 60, 0.5f);
                    batch.setColor(1, 1, 1, 1);
                    break;
                case CHECKPOINT:
                    batch.draw(it.active ? a.flagUp : a.flagDown, it.box.x, it.box.y, 32, 48);
                    break;
                case GOAL:
                    batch.draw(a.door, it.box.x, it.box.y, 32, 48);
                    break;
                case PORTAL:
                    draw(batch, a.portal, cx(it.box), it.box.y, 40, 40, false, false,
                        1 + 0.1f * MathUtils.sin(clock * 6), 1 + 0.1f * MathUtils.sin(clock * 6), clock * 180, 0.5f);
                    break;
                default:
                    break;
            }
        }

        // ---- 적 ----
        for (Enemy e : cur.enemies) {
            if (e.dead) continue;
            Texture tex = e.type == Enemy.Type.WALKER ? a.walker : e.type == Enemy.Type.FIRE ? a.fireEnemy
                : e.type == Enemy.Type.STONE ? a.stoneEnemy : a.bat;
            float sx = 1, sy = 1, rot = 0;
            if (e.type == Enemy.Type.BAT) sy = 0.8f + 0.25f * MathUtils.sin(e.t * 18);
            else rot = MathUtils.sin(e.t * 10) * 6;
            if (e.pulled || (e.type == Enemy.Type.FIRE && e.shootTimer < 0.4f)) {
                batch.setColor(1, 0.6f, 0.6f, 1);
                rot = MathUtils.random(-20, 20);
            }
            boolean flipY = g < 0;
            draw(batch, tex, cx(e.box), flipY ? e.box.y + e.box.height + 2 : e.box.y - 2, 32, 32, e.dir < 0, flipY, sx, sy, rot, 0);
            batch.setColor(1, 1, 1, 1);
        }

        // ---- 투사체 ----
        for (Projectile s : shots) {
            switch (s.kind) {
                case STAR:
                    draw(batch, a.star, cx(s.box), s.box.y - 4, 30, 30, false, false, 1, 1, s.rot, 0.5f);
                    break;
                case FIRE:
                    draw(batch, a.fireball, cx(s.box), s.box.y - 2, 18, 18, s.vx < 0, false, 1, 1, 0, 0.5f);
                    break;
                case AIR:
                    batch.setColor(1, 1, 1, Math.min(1, s.life * 4));
                    draw(batch, a.air, cx(s.box), s.box.y - 4, 22, 22, false, false, 1, 1, 0, 0.5f);
                    batch.setColor(1, 1, 1, 1);
                    break;
                case ENEMY_FLAME:
                    draw(batch, a.flame, cx(s.box), s.box.y - 2, 18, 18, false, false, 1, 1, s.rot, 0.5f);
                    break;
            }
        }

        // ---- 플레이어 ----
        drawPlayer(batch, a, g);

        particles.render(batch, a.white);

        // ---- 가시 천장 ----
        if (!inGlitch() && lv.spikeCeiling) {
            batch.setColor(0.25f, 0.05f, 0.05f, 1);
            batch.draw(a.white, left, ceilY + T - 4, vw, Math.max(0, cam.position.y + vh - ceilY));
            batch.setColor(1, 1, 1, 1);
            for (float sx = (float) Math.floor(left / T) * T; sx < left + vw; sx += T)
                batch.draw(a.spike, sx, ceilY, T, T, 0, 0, 32, 32, false, true);
            batch.setColor(1, 1, 1, 1);
        }

        // ---- 팝업 텍스트 ----
        for (Popup pp : popups) {
            layout.setText(font, pp.text);
            float px = pp.x - layout.width / 2;
            font.setColor(0, 0, 0, Math.min(1, pp.life));
            font.draw(batch, pp.text, px + 1, pp.y - 1);
            font.setColor(pp.r, pp.g, pp.b, Math.min(1, pp.life));
            font.draw(batch, pp.text, px, pp.y);
        }
        font.setColor(Color.WHITE);
        batch.end();

        // ---- 글리치 월드 스캔라인 ----
        if (inGlitch()) {
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            float y0s = (float) Math.floor(bottom / 6) * 6;
            for (float y = y0s; y < bottom + vh; y += 6) {
                shapes.setColor(0, 0, 0, 0.07f);
                shapes.rect(left, y, vw, 2f);
            }
            // 가끔(약 4초에 한 번) 얇은 줄 하나만 스쳐 지나감
            float sweep = (clock % 4f) / 0.6f;
            if (sweep < 1) {
                shapes.setColor(0.6f, 0.3f, 1f, 0.12f);
                shapes.rect(left, bottom + vh * (1 - sweep), vw, 3);
            }
            shapes.end();
        }
    }

    private void drawPlayer(SpriteBatch batch, Assets a, int g) {
        if (p.invuln > 0 && ((int) (p.invuln * 12)) % 2 == 0) return;
        Texture tex = a.pNormal;
        float w = 32, h = 32;
        if (p.stone) tex = a.pStone;
        else if (p.puffing) { tex = a.pPuff; w = h = 40; }
        else if (p.mouth != null) tex = a.pFull;
        else if (p.inhaling) tex = a.pInhale;

        // 속도에 따른 squash & stretch
        float stretch = MathUtils.clamp(Math.abs(p.vy) / 1500f, 0, 0.18f) - p.squash * 0.5f;
        if (p.grounded || p.stone) stretch = -p.squash * 0.5f;
        float sx = 1 - stretch * 0.8f, sy = 1 + stretch;
        float walkRot = p.grounded && Math.abs(p.vx) > 20 ? MathUtils.sin(p.anim * 18) * 6 : 0;
        boolean flipY = g < 0;
        float foot = flipY ? p.box.y + p.box.height + 2 : p.box.y - 2;
        draw(batch, tex, cx(p.box), foot, w, h, !p.facingRight, flipY, sx, sy, walkRot, 0);
        if (!p.stone && !p.puffing && p.mouth == null && p.ability != Ability.NONE) {
            Texture hat = p.ability == Ability.FIRE ? a.hatFire : a.hatStone;
            draw(batch, hat, cx(p.box), foot, w, h, !p.facingRight, flipY, sx, sy, walkRot, 0);
        }
    }

    /** 발 위치(또는 중심) 기준으로 스케일/회전/뒤집기를 지원하는 그리기 도우미 */
    private static void draw(SpriteBatch batch, Texture tex, float cx, float y, float w, float h,
                             boolean flipX, boolean flipY, float sx, float sy, float rot, float originYFrac) {
        float oy = flipY ? h * (1 - originYFrac) : h * originYFrac;
        float drawY = flipY ? y - h : y;
        batch.draw(tex, cx - w / 2, drawY, w / 2, oy, w, h, sx, sy, rot,
            0, 0, tex.getWidth(), tex.getHeight(), flipX, flipY);
    }

    private void drawDecor(ShapeRenderer s, int th, OrthographicCamera cam, Level lv) {
        float par = 0.45f, spacing = 150;
        float camY = cam.position.y, camX = cam.position.x;
        int k0 = (int) Math.floor((camY * par - 420) / spacing), k1 = (int) Math.ceil((camY * par + 420) / spacing);
        for (int k = k0; k <= k1; k++) {
            float hx = hash(k) * (lv.width() + 200) - 100;
            float x = hx + camX * (1 - par) - lv.width() / 2 * (1 - par);
            float y = k * spacing + camY * (1 - par);
            float r = 18 + hash(k * 7 + 3) * 26;
            switch (th) {
                case 0:
                    s.setColor(1, 1, 1, 0.55f);
                    s.circle(x, y, r);
                    s.circle(x + r * 0.9f, y - r * 0.2f, r * 0.75f);
                    s.circle(x - r * 0.9f, y - r * 0.25f, r * 0.7f);
                    break;
                case 1:
                    s.setColor(0.4f, 0.8f, 1f, 0.25f + 0.15f * MathUtils.sin(clock * 2 + k));
                    s.triangle(x - r * 0.4f, y, x + r * 0.4f, y, x, y + r * 2.2f);
                    s.setColor(0.8f, 0.4f, 1f, 0.2f);
                    s.triangle(x + r * 0.3f, y, x + r, y, x + r * 0.65f, y + r * 1.4f);
                    break;
                case 2:
                    float ey = y + ((clock * 40 + hash(k) * 300) % 300) - 150;
                    s.setColor(1, 0.5f + 0.3f * hash(k + 1), 0.1f, 0.5f);
                    s.circle(x, ey, 3 + hash(k + 2) * 4);
                    s.setColor(0.3f, 0.05f, 0.02f, 0.5f);
                    s.rect(x - r, y - 200, r * 1.5f, 200 + r);
                    break;
                default:
                    s.setColor(0.5f, 0.3f, 0.9f, 0.10f + 0.05f * MathUtils.sin(clock + k));
                    s.rect(x, y, r * 1.6f, r * 0.4f);
                    break;
            }
        }
    }

    private static float hash(int k) {
        float v = MathUtils.sin(k * 127.1f + 311.7f) * 43758.5453f;
        return v - (float) Math.floor(v);
    }
}

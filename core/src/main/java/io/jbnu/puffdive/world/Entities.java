package io.jbnu.puffdive.world;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

/** 작은 엔티티 클래스 모음 */
public final class Entities {
    private Entities() { }

    public enum Ability { NONE, FIRE, STONE }

    public static class Player {
        public static final int MAX_HP = 5;
        public static final int MAX_PUFFS = 3;
        public final Rectangle box = new Rectangle(0, 0, 24, 24);
        public float vx, vy;
        public boolean grounded, facingRight = true;
        public boolean puffing, inhaling, stone;
        public int puffsUsed;
        public Enemy.Type mouth; // null 이 아니면 입에 적을 물고 있는 상태
        public Ability ability = Ability.NONE;
        public int hp = MAX_HP;
        public float invuln, dropTimer, fireCooldown, anim, squash;
    }

    public static class Enemy {
        public enum Type { WALKER, FIRE, STONE, BAT }
        public final Type type;
        public final Rectangle box = new Rectangle();
        public final float spawnX, spawnY;
        public float vx, vy, t, shootTimer = 2f, respawnTimer;
        public int dir = -1;
        public boolean dead, pulled, grounded;

        public Enemy(Type type, float x, float y) {
            this.type = type;
            this.spawnX = x;
            this.spawnY = y;
            reset();
        }

        public void reset() {
            float w = 26, h = type == Type.BAT ? 20 : 26;
            box.set(spawnX + (Level.TILE - w) / 2f, spawnY, w, h);
            vx = vy = 0;
            dead = false;
            t = (spawnX * 0.37f) % 6f;
        }

        public Ability gives() {
            if (type == Type.FIRE) return Ability.FIRE;
            if (type == Type.STONE) return Ability.STONE;
            return Ability.NONE;
        }

        public float speed() {
            switch (type) {
                case FIRE: return 38;
                case STONE: return 28;
                default: return 52;
            }
        }
    }

    public static class Projectile {
        public enum Kind { STAR, FIRE, AIR, ENEMY_FLAME }
        public final Kind kind;
        public final Rectangle box = new Rectangle();
        public float vx, vy, life, rot;
        public boolean dead;

        public Projectile(Kind kind, float cx, float cy, float vx, float vy, float life) {
            this.kind = kind;
            float s = kind == Kind.STAR ? 22 : 14;
            box.set(cx - s / 2, cy - s / 2, s, s);
            this.vx = vx;
            this.vy = vy;
            this.life = life;
        }

        public boolean friendly() { return kind != Kind.ENEMY_FLAME; }
    }

    public static class Item {
        public enum Kind { STAR, GLITCH_STAR, CHECKPOINT, GOAL, PORTAL, RETURN }
        public final Kind kind;
        public final Rectangle box = new Rectangle();
        public boolean taken, active;

        public Item(Kind kind, float tileX, float tileY) {
            this.kind = kind;
            switch (kind) {
                case GOAL: case CHECKPOINT: box.set(tileX, tileY, 32, 48); break;
                case PORTAL: box.set(tileX - 4, tileY - 4, 40, 40); break;
                case RETURN: box.set(tileX + 4, tileY, 24, 24); break;
                default: box.set(tileX + 5, tileY + 5, 22, 22);
            }
        }
    }

    public static class Popup {
        public final String text;
        public float x, y, life = 1.2f;
        public final float r, g, b;

        public Popup(String text, float x, float y, float r, float g, float b) {
            this.text = text; this.x = x; this.y = y; this.r = r; this.g = g; this.b = b;
        }
    }

    /** 레벨 하나에 속한 적/아이템 묶음 (메인 레벨과 글리치 월드를 따로 유지하기 위함) */
    public static class Area {
        public final Level level;
        public final Array<Enemy> enemies = new Array<>();
        public final Array<Item> items = new Array<>();

        public Area(Level level) {
            this.level = level;
            int T = Level.TILE;
            for (Level.Spawn s : level.spawns) {
                float x = s.x * T, y = s.y * T;
                switch (s.c) {
                    case 'w': enemies.add(new Enemy(Enemy.Type.WALKER, x, y)); break;
                    case 'f': enemies.add(new Enemy(Enemy.Type.FIRE, x, y)); break;
                    case 's': enemies.add(new Enemy(Enemy.Type.STONE, x, y)); break;
                    case 'b': enemies.add(new Enemy(Enemy.Type.BAT, x, y)); break;
                    case '*': items.add(new Item(Item.Kind.STAR, x, y)); break;
                    case 'g': items.add(new Item(Item.Kind.GLITCH_STAR, x, y)); break;
                    case 'C': items.add(new Item(Item.Kind.CHECKPOINT, x, y)); break;
                    case 'G': items.add(new Item(Item.Kind.GOAL, x, y)); break;
                    case 'E': items.add(new Item(Item.Kind.PORTAL, x, y)); break;
                    case 'R': items.add(new Item(Item.Kind.RETURN, x, y)); break;
                    default: break;
                }
            }
        }

        public Item find(Item.Kind k) {
            for (Item i : items) if (i.kind == k) return i;
            return null;
        }

        public int count(Item.Kind k) {
            int n = 0;
            for (Item i : items) if (i.kind == k) n++;
            return n;
        }
    }
}

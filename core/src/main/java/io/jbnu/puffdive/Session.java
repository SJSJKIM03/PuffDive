package io.jbnu.puffdive;

/** 한 번의 플레이(1스테이지 ~ 엔딩) 동안 유지되는 기록 */
public class Session {
    public static final String[] LEVELS = {"levels/level1.txt", "levels/level2.txt", "levels/level3.txt"};
    public static final String GLITCH = "levels/glitch.txt";

    public float time;
    public int stars;
    public int falls; // 쓰러진 횟수
    public boolean glitchStar;
    public int abilitiesCopied;
}

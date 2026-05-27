package core;

import entity.GolemEnemy;

public class EnemyWaveManager {
    public interface WaveActions {
        void spawnDayWolves(int count);

        void spawnNightWolves(int count);

        void spawnGolems(int count, GolemEnemy.GolemMode mode);

        void spawnWallJumpers(int count);

        void showWarning();

        void showDawn();

        void startDawnRetreat();

        void finishDawn();
    }

    private int activeDay = -1;
    private boolean dayWolfSpawned;
    private boolean warningShown;
    private boolean wave1Spawned;
    private boolean wave2Spawned;
    private boolean dawnHandled;

    public void reset() {
        activeDay = -1;
        dayWolfSpawned = false;
        warningShown = false;
        wave1Spawned = false;
        wave2Spawned = false;
        dawnHandled = false;
    }

    public void update(long nowNs, DayNightManager dayNightManager, WaveActions actions) {
        if (dayNightManager == null || actions == null) {
            return;
        }

        int day = dayNightManager.getDay(nowNs);
        if (day != activeDay) {
            if (activeDay > 0) {
                actions.finishDawn();
            }
            activeDay = day;
            dayWolfSpawned = false;
            warningShown = false;
            wave1Spawned = false;
            wave2Spawned = false;
            dawnHandled = false;
        }

        DayNightManager.Phase phase = dayNightManager.getPhase(nowNs);
        switch (phase) {
            case DAY -> {
                if (!dayWolfSpawned) {
                    actions.spawnDayWolves(3);
                    dayWolfSpawned = true;
                }
            }
            case WARNING -> {
                if (!warningShown) {
                    actions.showWarning();
                    warningShown = true;
                }
            }
            case NIGHT_WAVE_1 -> {
                if (!wave1Spawned) {
                    actions.spawnNightWolves(2);
                    actions.spawnGolems(5, GolemEnemy.GolemMode.NORMAL);
                    actions.spawnGolems(2, GolemEnemy.GolemMode.WALL_BREAKER);
                    wave1Spawned = true;
                }
            }
            case NIGHT_WAVE_2 -> {
                if (!wave2Spawned) {
                    actions.spawnNightWolves(5);
                    actions.spawnWallJumpers(7);
                    actions.spawnGolems(5, GolemEnemy.GolemMode.WALL_BREAKER);
                    wave2Spawned = true;
                }
            }
            case DAWN -> {
                if (!dawnHandled) {
                    actions.showDawn();
                    actions.startDawnRetreat();
                    dawnHandled = true;
                }
            }
            default -> {
            }
        }
    }
}

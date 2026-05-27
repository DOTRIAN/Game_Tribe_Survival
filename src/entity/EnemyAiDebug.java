package entity;

public class EnemyAiDebug {
    private long lastLogAtNs;
    private int pathRequests;
    private int pathFails;
    private int wolfFenceAttacks;
    private int golemFenceAttacks;
    private int stuckEnemies;
    private long flowRebuildTotalNs;
    private int flowRebuildCount;

    public void recordPathRequest() {
        pathRequests++;
    }

    public void recordPathFail() {
        pathFails++;
    }

    public void recordFenceAttack(String enemyType) {
        if ("WOLF".equals(enemyType)) {
            wolfFenceAttacks++;
        } else if ("GOLEM".equals(enemyType)) {
            golemFenceAttacks++;
        }
    }

    public void recordStuck() {
        stuckEnemies++;
    }

    public void recordFlowRebuild(long durationNs) {
        flowRebuildCount++;
        flowRebuildTotalNs += Math.max(0L, durationNs);
    }

    public void flush(long nowNs, int enemyCount) {
        if (lastLogAtNs != 0L && nowNs - lastLogAtNs < 1_000_000_000L) {
            return;
        }
        lastLogAtNs = nowNs;
        long averageFlowMs = flowRebuildCount <= 0 ? 0L : (flowRebuildTotalNs / flowRebuildCount) / 1_000_000L;
        System.out.println("[AI DEBUG] Enemies=" + enemyCount
                + " Path requests/sec=" + pathRequests
                + " Path fails/sec=" + pathFails
                + " Flow rebuild ms=" + averageFlowMs
                + " Wolf attacking fence=" + wolfFenceAttacks
                + " Golem attacking fence=" + golemFenceAttacks
                + " Stuck enemies=" + stuckEnemies);
        pathRequests = 0;
        pathFails = 0;
        wolfFenceAttacks = 0;
        golemFenceAttacks = 0;
        stuckEnemies = 0;
        flowRebuildTotalNs = 0L;
        flowRebuildCount = 0;
    }
}

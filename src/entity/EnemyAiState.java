package entity;

public enum EnemyAiState {
    IDLE,
    PATROL,
    CHASE_PLAYER,
    MOVE_TO_BASE,
    MOVE_TO_OBSTACLE,
    ATTACK_PLAYER,
    ATTACK_BASE,
    ATTACK_OBSTACLE,
    RETURN_HOME,
    STUCK_RECOVERY,
    DEATH
}

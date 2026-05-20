package event;

// GameEventType:
// - Danh sach su kien runtime de cac subsystem phat/tiep nhan ma khong phu thuoc cung.
public enum GameEventType {
    RESOURCE_COLLECTED,
    BASE_CAMP_DAMAGED,
    PLAYER_DAMAGED,
    BOSS_SPAWNED,
    BOSS_KILLED,
    WORLD_SAVED,
    WORLD_LOADED
}

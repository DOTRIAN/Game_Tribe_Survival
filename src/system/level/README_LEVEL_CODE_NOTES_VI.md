# Ghi chu code folder `src/system/level`

## Muc tieu
- Giup nguoi doc code moi vao project hieu nhanh vai tro tung file level.
- Ghi ro moi lien he giua level system va he hit/combat/resource.

## Vai tro tung file
1. `Level.java`
- Model runtime cua 1 level doc tu JSON.
- Chua objective list va ham check hoan thanh objective.

2. `LevelManager.java`
- Dieu phoi load/select/complete/save tien do level.
- La diem noi giua Game state va file save `data/player_progress.json`.

3. `LevelObjective.java`
- Logic objective don le (COLLECT/SURVIVE).
- Build progress text de UI hien thi.

4. `LevelResult.java`
- Snapshot ket qua clear level (score, stars, play time...).
- Duoc save vao progress va hien trong overlay ket thuc level.

5. `PlayerProgress.java`
- Luu unlocked levels + ket qua tung level.
- Cung cap tong so sao / tong so level da clear cho UI.

6. `SimpleJson.java`
- Parser/writer JSON toi gian khong can thu vien ngoai.
- Dung cho ca level data va progress data.

## Moi quan he voi he hit/combat/resource
`Player attack -> DamageSystem/ResourceManager -> Game cap nhat collectedResources + elapsed time -> LevelObjective.isCompleted -> Level.areObjectivesCompleted -> LevelManager.completeLevel -> PlayerProgress.save`

## Ghi chu ve comment da them
- Moi file trong folder level da duoc them JavaDoc dau file voi:
  - Cong dung chinh cua file
  - Du lieu vao/ra quan trong
  - Lien he voi luong combat/hit de de trace code

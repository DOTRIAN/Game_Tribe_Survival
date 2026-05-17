# Ghi chu thay doi: Hit Effect + Floating Damage Text

## Pham vi folder da sua
- `src/entity`
- `src/system`
- `src/system/resource`
- `src/map`
- `src/ui`
- `src/core`

## Da lam gi
1. Them hieu ung hit cho entity:
- Bo sung trang thai flash trong `Entity` (`hitFlashUntilNs`, `hitFlashColor`).
- Them ham `triggerHitFlash(...)`, `isHitFlashActive(...)` de renderer doc va ve hieu ung.

2. Quái bi danh nhay do 100-150ms:
- `DamageSystem` trigger flash do `120ms` khi enemy/player nhan damage.
- `Enemy.draw(...)` duoc nang cap de ve lop do mo tren sprite khi dang flash.

3. Cây/đá bi danh nhay trang:
- `ResourceManager` trigger flash trang `130ms` cho `ResourceNode` moi lan bi hit.
- `MapRenderer` ve overlay trang tren tile giao voi resource dang flash.

4. Floating damage text:
- Them class `FloatingDamageText` (text `-20`, bay len, fade out, het han thi xoa).
- `Game` quan ly danh sach floating text, spawn khi enemy/resource mat mau, tu dong cleanup.
- `Renderer` render floating text trong world-space: bay len + mo dan.

5. Crit damage:
- Them `DamageResult` trong `system`.
- `DamageSystem` them logic crit co ban cho player (20%, x1.8 damage).
- Neu crit: text vang, font lon hon text thuong.

6. Resource hit result chi tiet hon:
- Them `ResourceHitResult` de tra ve damage moi lan hit (khong doi den luc bi pha).
- `Game` dung ket qua nay de spawn damage text cho cay/da ngay khi mat mau.

7. Fix theo feedback moi:
- Tang do dam hieu ung hit:
  - Enemy hit flash do tang len (mau do dam hon + alpha render cao hon).
  - Resource hit flash trang de `150ms` de de nhan ra impact.
- Bo hieu ung "hinh vuong", doi sang "om object":
  - Enemy: dung `BlendMode.SRC_ATOP` de phu mau theo alpha sprite.
  - Resource tile: dung `BlendMode.SRC_ATOP` de phu mau theo alpha tile da ve.
- Fix quai lat huong lien tuc khi sat player:
  - Them nguong `FACE_FLIP_THRESHOLD_X` de chi doi huong khi chenh X du lon.
  - Them `APPROACH_STOP_DISTANCE` de enemy khong dao dong qua lai khi da ap sat.

8. Fix bo sung theo feedback "van con hinh vuong" + "nhin to ra":
- Nguyen nhan:
  - Cach to mau cu phu len ca vung draw, nen tao cam giac khung vuong va doi luc nhin nhu sprite bi "phinh".
- Cach sua:
  - Enemy: tao anh tint moi dua tren `alpha` cua sprite goc (pixel trong suot giu nguyen trong suot), sau do ve chong len.
  - Tile resource: cat tung tile trong tileset, tao ban tint theo `alpha` tile goc roi moi ve.
- Ket qua:
  - Mau hit bam dung hinh dang nhan vat/cay theo sprite alpha, khong con phu mau dang hinh vuong.

9. Tinh chinh theo feedback moi nhat:
- Giam do dam mau hit:
  - Enemy alpha tint giam xuong muc nhe hon.
  - Resource tile alpha tint giam xuong muc nhe hon.
- Sua loi "quai ap sat player nhin nhu phong to":
  - Khi enemy dang o khoang cach rat gan (`huggingTarget`), giu animation run thay vi doi sang idle frame.
  - Ly do: bo frame idle/run co the khac do day hinh, doi state ngay luc ap sat de tao cam giac "phinh to".
  - Them pixel-snap khi ve enemy (`Math.round`) de giam blur va rung hinh o toa do le.

## Dong the hien moi quan he giua cac file
`Game.performPlayerAttack -> DamageSystem.applyDamage / ResourceManager.hitFirstResourceIntersecting -> (Entity/ResourceNode trigger flash) -> Enemy.draw + MapRenderer (SRC_ATOP tint om sprite/tile) -> Renderer ve FloatingDamageText`

## 1) Mục tiêu
Chuẩn hóa dữ liệu map để code Java:
1. Phân biệt đúng loại resource (`tree`, `rock`, ...)
2. Trừ HP / drop item chuẩn
3. Khi resource bị phá: **toàn bộ hình resource biến mất** (gốc + tán)
4. Chuẩn bị sẵn dữ liệu cho phase sau: đổi sang hiệu ứng mảnh vụn (debris)

---

## 2) Layer chuẩn bắt buộc

### 2.1 Tile Layers
- `Grounds`: nền map
- `Objects`: phần thân/vật thể thấp
- `Foreground`: phần che lên player (tán cây, mái nhà)

### 2.2 Object Layers
- `Collisions`: vật cản tĩnh, không tương tác (không hp, không drop)
- `Resources`: object gameplay có hp/drop
- `ResourceVisuals` (MỚI): vùng hình ảnh “đầy đủ” của resource để ẩn khi bị phá (bao cả gốc + tán)

---

## 3) Schema bắt buộc cho object layer `Resources`

Mỗi object resource bắt buộc có:

- `class` (hoặc `type`): `Resource`
- `resourceId` (string, bắt buộc, duy nhất trong map)  
  Ví dụ: `tree_001`, `rock_014`
- `kind` (string, bắt buộc)  
  Ví dụ: `tree_oak`, `rock_small`, `grass`, `vegetable_carrot`
- `maxHp` (int, bắt buộc)
- `dropItem` (string, bắt buộc)  
  Ví dụ: `wood`, `stone`, `fiber`, `carrot`

Optional:
- `dropMin` (int, default `1`)
- `dropMax` (int, default = `dropMin`)
- `respawnSec` (int, default `-1`)

---

## 4) Schema bắt buộc cho object layer `ResourceVisuals`

Mỗi object visual bắt buộc có:

- `class` (hoặc `type`): `ResourceVisual`
- `resourceId` (string, bắt buộc)  
  Phải trùng với `resourceId` của object trong `Resources`
- `visualRole` (string, optional): `full`, `trunk`, `canopy` (khuyến nghị `full`)
- Hình dạng object (rectangle) bao phủ vùng hình cần ẩn (gốc + tán)

### Quy tắc:
- Nếu cây có tán ở `Foreground`, object `ResourceVisual` phải phủ luôn vùng tán đó.
- Khi `resourceId` bị phá, code sẽ ẩn mọi tile giao với vùng visual cùng `resourceId`.

---

## 5) Quy ước cho layer `Collisions`
- `class/type`: `Collision`
- Không có `dropItem`, `maxHp`, `resourceId`, `kind`

---

## 6) Quy ước đặt tên
- Dùng `snake_case`, không dấu, không khoảng trắng
- Key phân biệt chữ hoa/thường chính xác:
  - `resourceId`, `kind`, `maxHp`, `dropItem`, `dropMin`, `dropMax`, `respawnSec`

---

## 7) Flow gameplay thống nhất

1. Player chém resource theo object trong `Resources`
2. HP về 0 => resource destroyed
3. Code tìm `resourceId` tương ứng trong `ResourceVisuals`
4. Ẩn toàn bộ vùng visual (=> cây biến mất cả gốc + tán)
5. Spawn drop item
6. Phase sau: thay bước 5 bằng spawn debris trước, debris hút về player rồi cộng inventory

---

## 8) Chuẩn bị cho phase “mảnh vụn” (debris)

Trong `Resources`, thêm optional:
- `debrisType` (string) ví dụ: `wood_chip`, `stone_shard`
- `debrisCount` (int) ví dụ: `6`

Nếu chưa có asset debris, vẫn để trống, code fallback drop trực tiếp như hiện tại.

---

## 9) Checklist bàn giao map

1. Có đủ layer: `Grounds`, `Objects`, `Foreground`, `Collisions`, `Resources`, `ResourceVisuals`
2. Mỗi resource có đủ: `resourceId`, `kind`, `maxHp`, `dropItem`
3. Mỗi resource có ít nhất 1 object visual cùng `resourceId`
4. `resourceId` là duy nhất, không trùng
5. `dropMin <= dropMax`
6. Không trộn resource object vào `Collisions`

---

## 10) Ví dụ mẫu

### Resource object (layer `Resources`)
- `class=Resource`
- `resourceId=tree_001`
- `kind=tree_oak`
- `maxHp=5`
- `dropItem=wood`
- `dropMin=1`
- `dropMax=3`

### Visual object (layer `ResourceVisuals`)
- `class=ResourceVisual`
- `resourceId=tree_001`
- `visualRole=full`
- Rectangle phủ toàn bộ cây (gốc + tán)
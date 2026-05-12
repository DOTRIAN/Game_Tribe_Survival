# MAP CONTRACT (Tiled <-> Java)
Version: 1.0  
Applies to: `mapdemo.tmx` and all future maps

## 1) Mục tiêu
Tài liệu này là **hợp đồng dữ liệu** giữa:
- Người vẽ map trên Tiled
- Người code Java (load map, collision, resource, drop)

Mục tiêu: hai bên làm độc lập nhưng dữ liệu luôn đọc được, không phải sửa tay nhiều trong code.

---

## 2) Layer chuẩn bắt buộc

## 2.1 Tile Layers
1. `Grounds`
- Chỉ chứa nền (đất, cỏ, đường, sàn)
- Không chứa logic gameplay

2. `Objects`
- Chứa vật thể thấp (prop thấp, phần thân thấp)
- Chủ yếu phục vụ render

3. `Foreground`
- Chứa phần cần che player (tán cây, mái nhà, vật thể cao)
- Chỉ render overlay, không dùng để collision

## 2.2 Object Layers
4. `Collisions`
- Chỉ chứa object chặn đường tĩnh
- Ví dụ: tường, mép nhà, đá không tương tác
- Không chứa `dropItem`, `maxHp`

5. `Resources`
- Chứa object tài nguyên có tương tác
- Ví dụ: cây, đá, cỏ, rau
- Có HP và drop item

---

## 3) Schema object cho layer `Resources` (BẮT BUỘC)

Mỗi object trong `Resources` phải có:

- `class` (hoặc `type`): `Resource`
- `kind` (string, bắt buộc)  
  Ví dụ: `tree_oak`, `rock_small`, `grass`, `vegetable_carrot`
- `maxHp` (int, bắt buộc)  
  HP khởi tạo của resource
- `dropItem` (string, bắt buộc)  
  Ví dụ: `wood`, `stone`, `fiber`, `carrot`

Thuộc tính optional:
- `dropMin` (int, mặc định `1`)
- `dropMax` (int, mặc định `dropMin`)
- `respawnSec` (int, mặc định `-1`, nghĩa là không hồi)

### Ví dụ object hợp lệ
- `class=Resource`
- `kind=tree_oak`
- `maxHp=5`
- `dropItem=wood`
- `dropMin=1`
- `dropMax=3`
- `respawnSec=120`

---

## 4) Schema object cho layer `Collisions`

Mỗi object trong `Collisions`:
- `class` (hoặc `type`): `Collision`
- Dùng shape đơn giản: Rectangle hoặc Ellipse
- Không set `dropItem`, `maxHp`, `dropMin`, `dropMax`

---

## 5) Quy ước đặt tên / dữ liệu

1. Các key property dùng đúng chữ hoa/thường:
- `kind`
- `maxHp`
- `dropItem`
- `dropMin`
- `dropMax`
- `respawnSec`

2. Giá trị string dùng:
- `snake_case`
- Không dấu
- Không khoảng trắng
- Ví dụ: `tree_oak`, `stone_resource`

3. Không dùng `name` để code phân loại gameplay
- `name` chỉ để con người đọc trong Tiled
- Code sẽ đọc bằng `kind`

---

## 6) Quy tắc cây có gốc + tán

- Gốc cây (hitbox gameplay): đặt object ở layer `Resources`
- Tán cây (che player): vẽ tile ở layer `Foreground`

Lý do:
- Va chạm + chặt cây xử lý bằng object resource
- Hiệu ứng che lớp xử lý bằng foreground render

---

## 7) Ranh giới trách nhiệm

## Bên map
- Đảm bảo đúng tên layer
- Đảm bảo object resource có đủ property bắt buộc
- Không trộn resource vào `Collisions`

## Bên code
- Parse theo schema này
- Validate dữ liệu thiếu/sai và log object id bị lỗi
- Dùng default cho field optional

---

## 8) Checklist nghiệm thu map trước khi bàn giao

1. Có đủ layer: `Grounds`, `Objects`, `Foreground`, `Collisions`, `Resources`
2. Mọi object trong `Resources` có đủ: `kind`, `maxHp`, `dropItem`
3. Nếu có `dropMin/dropMax` thì `dropMin <= dropMax`
4. Không có object resource đặt nhầm trong `Collisions`
5. Không typo key (ví dụ `dropitem`, `hp`, `maxhp`)

---

## 9) Ví dụ lỗi thường gặp (cần tránh)

- Đặt `hp` thay vì `maxHp`
- Đặt `dropitem` thay vì `dropItem`
- Để `kind="Tree Oak"` (có khoảng trắng/chữ hoa)
- Vẽ tán cây ở `Objects` thay vì `Foreground`
- Đặt object tài nguyên trong layer `Collisions`

---

## 10) Chính sách thay đổi contract
- Mọi thay đổi key/layer phải báo trước cho cả team
- Chỉ tăng version khi thay đổi schema (ví dụ 1.0 -> 1.1)
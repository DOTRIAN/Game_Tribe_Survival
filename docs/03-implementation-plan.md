# Cách làm (không code)

## 1) Kiến trúc tính năng theo module
- Module Time & Phase:
  - State machine: Day -> Dusk -> Night -> Dawn.
  - Event bus cho các hệ: spawn, UI, economy.
- Module Economy:
  - Inventory tài nguyên.
  - Gather/Spend/Repair với log rõ ràng.
- Module Build:
  - Placement, collision, nav-blocking rule.
  - Durability + repair + upgrade.
- Module Enemy:
  - Spawn director theo ngày.
  - Behavior theo archetype và priority target.
- Module Progression:
  - Unlock theo mốc ngày/cấp nhà chính.
  - Meta tuning bằng bảng dữ liệu.

## 2) Pipeline sản xuất nội dung
- Bước 1: Chốt data sheet cân bằng (CSV/Google Sheet).
- Bước 2: Chốt prefab set tối thiểu (nhà, rào, tháp, quái).
- Bước 3: Chốt hành vi AI theo test map phẳng.
- Bước 4: Thêm địa hình và sự kiện thời tiết.
- Bước 5: Polish VFX/SFX + UX cảnh báo ngày/đêm.

## 3) Milestone triển khai
- M0 (prototype 1-2 tuần):
  - Core loop chạy được, chưa đẹp, 1 loại quái.
- M1 (vertical slice):
  - Đủ trải nghiệm 10-15 phút với 2-3 đêm.
- M2 (content alpha):
  - Đủ loại công trình/quái theo spec cơ bản.
- M3 (balance beta):
  - Tuning khó, sửa exploit, tối ưu nhịp độ.

## 4) Checklist test gameplay
- Người chơi mới có hiểu mục tiêu trong 60 giây đầu không?
- Có bị thiếu tài nguyên vô lý trong 3 ngày đầu không?
- Có chiến thuật nào quá mạnh khiến game mất thử thách không?
- Tỉ lệ thua hợp lý ở ngày bao nhiêu (target theo độ khó)?
- Có moment "căng" ở 30 giây cuối mỗi đêm không?

## 5) KPI để cân bằng
- Tỉ lệ sống sót đến ngày 5/8/12.
- Tỉ lệ dùng từng loại công trình.
- Số lần rời căn cứ ban ngày (đo mức mạo hiểm).
- Nguồn gây chết chính (quái nào, tình huống nào).

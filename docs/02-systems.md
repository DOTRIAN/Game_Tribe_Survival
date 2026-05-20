# Gameplay Systems

## 1) Hệ thống thời gian
- Đề xuất baseline:
  - Ngày: 5 phút.
  - Đêm: 3 phút.
  - Tổng vòng: 8 phút.
- Scale độ khó:
  - Ngày 1-3: dễ làm quen.
  - Ngày 4-7: tăng số lượng quái và thêm quái vai trò.
  - Ngày 8+: mở boss/biến thể đêm.

## 2) Kinh tế tài nguyên
- Gỗ: rào cơ bản, sửa chữa nhanh.
- Đá: tháp/công trình bền hơn.
- Vàng: nâng cấp nhà chính, mở khóa công nghệ, mua vật phẩm.
- Quy tắc cân bằng:
  - Tài nguyên hữu hạn theo cụm spawn.
  - Node có thời gian hồi, không cho farm vô hạn.
  - Tài nguyên tốt nằm xa căn cứ để ép ra ngoài.

## 3) Công trình phòng thủ
- Nhà chính:
  - Có HP lớn, bị phá là game over.
  - Nâng cấp theo cấp để mở khóa hệ thống mới (tháp, bẫy, dân làng).
- Rào:
  - Tuyến phòng thủ đầu tiên, rẻ, build nhanh.
  - Có độ bền và chi phí sửa.
- Tháp canh:
  - Tự bắn, có tầm bắn/tốc bắn/ưu tiên mục tiêu.
  - Nâng cấp theo nhánh (sát thương đơn, diện rộng, làm chậm).
- Bẫy (mở sau):
  - Bẫy gai, hố chông, lửa, chuông báo động.

## 4) Quái và AI ưu tiên
- Quái nhanh: áp lực truy đuổi.
- Quái phá rào: counter lối chơi build kín.
- Quái đánh xa: buộc bố trí đội hình không chỉ dựa vào rào.
- Boss: xuất hiện theo mốc ngày, ép đổi chiến thuật.
- Priority AI:
  - Nếu thấy rào gần nhất -> tấn công rào.
  - Nếu có đường trống -> dồn vào nhà chính.
  - Nếu người chơi trong tầm dễ hạ -> chuyển mục tiêu.

## 5) Cơ chế chống "thủ kín"
- Luôn có 1 loại quái phá công trình mạnh.
- Giới hạn số lượng công trình theo cấp nhà chính.
- Công trình hao mòn sau mỗi đêm, bắt buộc bảo trì.
- Đêm đặc biệt (sương mù/gió mạnh/trăng máu) làm giảm hiệu quả thủ cố định.

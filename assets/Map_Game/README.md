Vì sao game thường không dùng “1 ảnh tự động chạy 2-3s”?
Vì game cần kiểm soát animation rất linh hoạt:
•
đang đứng thì frame idle
•
đang chạy thì frame run
•
đang tấn công thì frame attack
•
chết thì frame death
•
đi trái / phải thì frame khác nhau
Nếu dùng một file tự chạy sẵn kiểu “ảnh động” thì rất khó:
•
đồng bộ với state game
•
dừng/đổi animation đúng lúc
•
điều khiển từng trạng thái
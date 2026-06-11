input/

- `InputHandler`: luu trang thai phim nhan va gan su kien ban phim vao `Scene`.
- Package nay chi xu ly input, khong chua logic gameplay.



Tại sao viết new HashSet<>() mà field lại là Set<KeyCode>? Đây là cách viết rất phổ biến trong Java.
Ý nghĩa:
•
biến được khai báo theo interface hoặc kiểu tổng quát hơn là Set
•
object cụ thể đang dùng là HashSet
Lợi ích:
•
code linh hoạt hơn
•
sau này có thể đổi sang loại Set khác mà ít ảnh hưởng
Đây là một tư duy OOP/Java tốt:
•
khai báo theo abstraction
•
khởi tạo bằng implementation cụ thể

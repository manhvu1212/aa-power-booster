# Bản ghi phát hành (Release Notes) - AA Power Booster v1.0.9

## 1. Bản ngắn gọn (Dùng để nhập trên Google Play Console - Dưới 500 ký tự)
```text
Bản phát hành AA Power Booster v1.0.9 (io.github.manhvu1212.aapowerbooster):
- Khắc phục lỗi không khởi chạy được trên Android Auto (bổ sung khai báo cấp API Car App còn thiếu).
- Điều khiển chế độ lái ngay trên app điện thoại (Race/Sport/Normal/City/Eco + tăng/giảm cấp).
- Hiển thị đúng cấp độ của từng chế độ và số phiên bản trên ứng dụng.
```

---

## 2. Bản chi tiết (Changelog & Hướng dẫn kỹ thuật)

### Các cập nhật trong phiên bản 1.0.9:

1. **Khắc phục lỗi khởi chạy trên Android Auto (điểm chính):**
   * Bổ sung khai báo bắt buộc của thư viện Car App còn thiếu trong manifest:
     `androidx.car.app.minCarApiLevel` (đặt = 1 để tương thích rộng nhất).
   * Việc thiếu khai báo này khiến host Android Auto có thể từ chối khởi tạo dịch vụ car của ứng dụng (gặp lỗi ngay khi mở trên màn hình xe, trước khi giao diện kịp hiển thị).

### Các tính năng đã có (từ các bản gần đây):
* **Điều khiển chế độ lái trên app điện thoại:** bảng "Chọn chế độ lái" với 5 chế độ (Race – Sport – Normal – City – Eco) và nút tăng/giảm cấp độ.
* **Hiển thị đúng cấp độ từng chế độ** và **số phiên bản** ngay trên ứng dụng.
* **Tối ưu giao diện Android Auto** (lưới 5 ô, nút tăng/giảm cấp ở thanh hành động) và **tự đồng bộ lại** trạng thái sau mỗi lần đổi chế độ.

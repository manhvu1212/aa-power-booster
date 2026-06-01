# Bản ghi phát hành (Release Notes) - AA Power Booster v1.0.10

## 1. Bản ngắn gọn (Dùng để nhập trên Google Play Console - Dưới 500 ký tự)
```text
Bản phát hành AA Power Booster v1.0.10 (io.github.manhvu1212.aapowerbooster):
- Khắc phục lỗi hiển thị giao diện Android Auto (chuyển nút tăng/giảm cấp sang dạng biểu tượng).
- Android Auto khởi chạy đúng sau khi bổ sung khai báo cấp API còn thiếu.
- Điều khiển chế độ lái ngay trên app điện thoại + hiển thị đúng cấp độ từng chế độ.
```

---

## 2. Bản chi tiết (Changelog & Hướng dẫn kỹ thuật)

### Các cập nhật trong phiên bản 1.0.10:

1. **Khắc phục lỗi giao diện Android Auto (điểm chính):**
   * Thanh hành động (ActionStrip) của Android Auto chỉ cho phép tối đa một nút có chữ. Hai nút "Cấp -" và "Cấp +" đều dùng chữ nên bị từ chối (lỗi "Action List exceeded max number of 1 action with custom titles").
   * Nay hai nút tăng/giảm cấp dùng **biểu tượng (+ / −)** thay cho chữ, nên hợp lệ và hiển thị được.

2. **Android Auto khởi chạy đúng (từ 1.0.9):**
   * Bổ sung khai báo bắt buộc `androidx.car.app.minCarApiLevel` để host Android Auto khởi tạo được dịch vụ car của ứng dụng.

### Các tính năng đã có (từ các bản gần đây):
* **Điều khiển chế độ lái trên app điện thoại:** bảng "Chọn chế độ lái" với 5 chế độ (Race – Sport – Normal – City – Eco) và nút tăng/giảm cấp độ.
* **Hiển thị đúng cấp độ từng chế độ** và **số phiên bản** ngay trên ứng dụng.
* **Tự đồng bộ lại** trạng thái chân ga sau mỗi lần đổi chế độ.

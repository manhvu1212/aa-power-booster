# Bản ghi phát hành (Release Notes) - AA Power Booster v1.0.7

## 1. Bản ngắn gọn (Dùng để nhập trên Google Play Console - Dưới 500 ký tự)
```text
Bản phát hành AA Power Booster v1.0.7 (io.github.manhvu1212.aapowerbooster):
- Thêm điều khiển chế độ lái ngay trên app điện thoại (Race/Sport/Normal/City/Eco + tăng/giảm cấp).
- Hiển thị đúng cấp độ của từng chế độ.
- Tối ưu giao diện Android Auto để tránh văng ứng dụng.
- Tự đồng bộ lại trạng thái chân ga sau mỗi lần đổi chế độ.
```

---

## 2. Bản chi tiết (Changelog & Hướng dẫn kỹ thuật)

### Các cập nhật trong phiên bản 1.0.7:

1. **Điều khiển chế độ trên app điện thoại (tính năng mới):**
   * Bổ sung bảng **"Chọn chế độ lái"** ngay trên app điện thoại (hiện khi đã kết nối): 5 nút chế độ theo thứ tự Race – Sport – Normal – City – Eco, nút đang chọn được tô sáng.
   * Hàng **"Cấp độ nhạy"** với nút tăng/giảm cấp (1–9); tự vô hiệu hóa khi ở chế độ Normal (zin).
   * Trước đây việc đổi chế độ chỉ thực hiện được trên màn hình Android Auto; nay có thể điều khiển trực tiếp từ điện thoại.

2. **Hiển thị đúng cấp độ từng chế độ (Level Sync):**
   * App đọc đúng ô cấp độ tương ứng với chế độ đang chọn trong gói tin trạng thái, nên cấp độ hiển thị chính xác cho từng chế độ.

3. **Ổn định Android Auto:**
   * Giao diện lưới giữ ở 5 ô chế độ (nút tăng/giảm cấp nằm ở thanh hành động) để nằm trong giới hạn của mọi đầu màn hình, kèm cơ chế bắt lỗi hiển thị thông báo thay vì văng ứng dụng.

4. **Tự đồng bộ lại sau khi đổi chế độ (Auto Re-sync):**
   * Sau mỗi lệnh đổi chế độ/cấp độ, app tự lấy lại trạng thái đầy đủ từ thiết bị để giao diện luôn đúng thực tế.

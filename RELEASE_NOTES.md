# Bản ghi phát hành (Release Notes) - AA Power Booster v1.0.8

## 1. Bản ngắn gọn (Dùng để nhập trên Google Play Console - Dưới 500 ký tự)
```text
Bản phát hành AA Power Booster v1.0.8 (io.github.manhvu1212.aapowerbooster):
- Hiển thị số phiên bản ngay trên ứng dụng để dễ kiểm tra bản đang dùng.
- Cải thiện cơ chế chẩn đoán sự cố trên Android Auto (ghi vết tiến trình từ sớm).
- Điều khiển chế độ lái ngay trên app điện thoại (Race/Sport/Normal/City/Eco + tăng/giảm cấp).
- Hiển thị đúng cấp độ của từng chế độ.
```

---

## 2. Bản chi tiết (Changelog & Hướng dẫn kỹ thuật)

### Các cập nhật trong phiên bản 1.0.8:

1. **Hiển thị phiên bản trên ứng dụng:**
   * App điện thoại hiển thị số phiên bản hiện tại (ví dụ "v1.0.8") ngay dưới tiêu đề, giúp xác nhận đúng bản đang chạy.

2. **Cải thiện chẩn đoán Android Auto:**
   * Ghi lại "vết chân" tiến trình của dịch vụ Android Auto từ rất sớm (lúc host kết nối, tạo phiên, tạo màn hình) bằng cơ chế ghi đồng bộ, nhằm xác định chính xác nguyên nhân khi gặp sự cố hiển thị.

### Các tính năng đã có (từ các bản gần đây):
* **Điều khiển chế độ lái trên app điện thoại:** bảng "Chọn chế độ lái" với 5 chế độ (Race – Sport – Normal – City – Eco) và nút tăng/giảm cấp độ.
* **Hiển thị đúng cấp độ từng chế độ.**
* **Tự đồng bộ lại trạng thái chân ga sau mỗi lần đổi chế độ.**

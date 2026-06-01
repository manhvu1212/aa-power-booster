# Bản ghi phát hành (Release Notes) - AA Power Booster v2.0.0

## 1. Bản ngắn gọn (Dùng để nhập trên Google Play Console - Dưới 500 ký tự)
```text
Bản phát hành AA Power Booster v2.0.0 (io.github.manhvu1212.aapowerbooster):
- Giao diện mới gọn gàng, hiện đại: chọn chế độ bằng biểu tượng, logo mới.
- Điều khiển đầy đủ chế độ lái ngay trên điện thoại (Race/Sport/Normal/City/Eco).
- Mỗi chế độ ghi nhớ cấp độ riêng; hiển thị đúng cấp của từng chế độ.
- Tự động kết nối thiết bị đã lưu; ẩn dò tìm khi đã có thiết bị.
- Khắc phục hiển thị & ổn định trên Android Auto.
```

---

## 2. Bản chi tiết (Changelog & Hướng dẫn kỹ thuật)

### Phiên bản 2.0.0 — Nâng cấp lớn về giao diện & trải nghiệm:

**Giao diện app điện thoại:**
1. **Chọn chế độ bằng biểu tượng:** 5 chế độ (Race – Sport – Normal – City – Eco) hiển thị dưới dạng 5 ô biểu tượng cân đối trên một hàng, chế độ đang chọn được làm nổi bật.
2. **Điều khiển ngay trên điện thoại:** đổi chế độ và tăng/giảm cấp độ nhạy (1–9) trực tiếp, không cần Android Auto.
3. **Logo mới:** biểu tượng cánh đỏ đặt giữa nền đen.
4. **Gọn gàng hơn:** bỏ phần tiêu đề thừa và các khung gỡ lỗi; mục dò tìm chỉ hiện khi chưa lưu thiết bị.
5. **Tự động kết nối** thiết bị đã lưu ngay khi mở ứng dụng.

**Cấp độ theo từng chế độ:**
6. Mỗi chế độ ghi nhớ cấp độ riêng. Khi chuyển chế độ, ứng dụng khôi phục đúng cấp độ đã lưu của chế độ đó (đọc từ gói trạng thái thiết bị). Áp dụng cho cả app điện thoại lẫn Android Auto.

**Android Auto:**
7. Khắc phục lỗi không khởi chạy/hiển thị được trên Android Auto (bổ sung khai báo cấp API Car App; tối ưu lưới 5 ô; chuyển nút tăng/giảm cấp sang biểu tượng trên thanh hành động).
8. Hiển thị chế độ và cấp độ hiện tại trên tiêu đề màn hình xe.
9. Tự đồng bộ lại trạng thái sau mỗi lần đổi chế độ.

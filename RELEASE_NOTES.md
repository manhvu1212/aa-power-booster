# Bản ghi phát hành (Release Notes) - AA Power Booster v1.0.6

## 1. Bản ngắn gọn (Dùng để nhập trên Google Play Console - Dưới 500 ký tự)
```text
Bản phát hành AA Power Booster v1.0.6 (io.github.manhvu1212.aapowerbooster):
- Khắc phục lỗi văng ứng dụng trên Android Auto: tối ưu giao diện còn 5 ô chế độ, chuyển nút tăng/giảm cấp xuống thanh hành động.
- Hiển thị đúng cấp độ của từng chế độ (trước đây bị kẹt ở một giá trị).
- Tự đồng bộ lại trạng thái chân ga sau mỗi lần đổi chế độ.
- Cải thiện cơ chế ghi nhận lỗi để hỗ trợ chẩn đoán.
```

---

## 2. Bản chi tiết (Changelog & Hướng dẫn kỹ thuật)

### Các cập nhật trong phiên bản 1.0.6:

1. **Khắc phục văng ứng dụng trên Android Auto (điểm chính):**
   * Nhiều đầu màn hình ô tô giới hạn số ô trên giao diện lưới (thường là 6). Phiên bản cũ hiển thị 7 ô (5 chế độ + 2 nút tăng/giảm cấp) nên có thể bị host từ chối và văng app.
   * Nay **2 nút tăng/giảm cấp được chuyển xuống thanh hành động (ActionStrip)**, lưới chỉ còn **5 ô chế độ** — nằm an toàn trong giới hạn của mọi đầu màn hình. Cấp độ hiện tại được hiển thị ngay trên ô của chế độ đang chọn.

2. **Hiển thị đúng cấp độ từng chế độ (Level Sync Fix):**
   * Gói tin trạng thái từ thiết bị chứa cấp độ đã lưu của tất cả chế độ xếp tuần tự; app nay đọc đúng ô tương ứng với chế độ đang chọn nên cấp độ hiển thị chính xác, không còn bị kẹt ở một giá trị.

3. **Tự đồng bộ lại sau khi đổi chế độ (Auto Re-sync):**
   * Sau mỗi lệnh đổi chế độ/cấp độ, app tự lấy lại trạng thái đầy đủ từ thiết bị để giao diện luôn đúng thực tế.

4. **Cải thiện chẩn đoán & ghi nhận lỗi (Diagnostics):**
   * Ghi nhật ký lỗi và "vết chân" tiến trình của giao diện Android Auto một cách an toàn (ghi đồng bộ), giúp xác định nguyên nhân sự cố ngay trên app điện thoại mà không cần công cụ ngoài.

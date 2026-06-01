# Bản ghi phát hành (Release Notes) - AA Power Booster v1.0.3

## 1. Bản ngắn gọn (Dùng để nhập trên Google Play Console - Dưới 500 ký tự)
```text
Bản phát hành AA Power Booster v1.0.3 (io.github.manhvu1212.aapowerbooster):
- Sửa lỗi đồng bộ dữ liệu hiển thị chế độ thực tế từ cảm biến chân ga.
- Giới hạn bộ lọc dò quét Bluetooth chỉ hiển thị thiết bị hỗ trợ (JDY).
- Hỗ trợ Android Auto giúp chỉnh chân ga an toàn trên màn hình xe.
- 5 chế độ lái theo thứ tự tối ưu: Race - Sport - Normal - City - Eco.
- Tăng/Giảm độ nhạy (cấp 1-9) và chèn logo "AA" nhận diện thương hiệu.
```

---

## 2. Bản chi tiết (Changelog & Hướng dẫn kỹ thuật)

### Các tính năng chính:
* **Tương thích Android Auto (IoT Category):** Cho phép điều khiển bộ điều tốc chân ga trực tiếp trên màn hình ô tô bằng giao diện tối giản, an toàn khi lái xe.
* **5 Chế độ lái trực quan:** Các phím chế độ được sắp xếp theo đúng thứ tự yêu cầu: **Race - Sport - Normal - City - Eco**.
* **Phím cộng/trừ tăng giảm cấp độ:** Hỗ trợ tăng giảm cấp độ nhạy (Level 1..9) cho các chế độ (trừ Normal).
* **Đồng bộ hóa thời gian thực (Real-time Sync):** Tự động gửi lệnh `getData` khi kết nối và lắng nghe Bluetooth notification để đồng bộ trạng thái hiển thị với phím vật lý trên xe hoặc app gốc.
* **Tránh xung đột kết nối:** Hiển thị rõ trạng thái kết nối/thiết bị bận và hỗ trợ nút ngắt kết nối nhanh trên điện thoại để trả quyền cho app gốc.

### Các cập nhật mới nhất (Phiên bản 1.0.3):
1. **Sửa lỗi đồng bộ dữ liệu chân ga (BLE Parsing Fix):** Sửa lỗi hoán đổi thứ tự byte trong gói tin phản hồi của Bluetooth Notification (Little-Endian parser), giúp hiển thị chính xác chế độ và cấp độ ga thực tế của cảm biến lên màn hình xe.
2. **Cải tiến dò quét Bluetooth (Scan Filter):** Cập nhật bộ quét BLE chỉ hiển thị các thiết bị có chứa từ khóa `"JDY"` (bộ điều khiển chân ga được hỗ trợ), loại bỏ các thiết bị Bluetooth không liên quan.
3. **Ký số tự động và Logo "AA":** Đã tích hợp chữ ký số và đóng dấu "AA" ở góc logo để nhận diện dễ dàng.

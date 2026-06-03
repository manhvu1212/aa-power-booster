# Bản ghi phát hành (Release Notes) - AA Power Booster v2.1.2

## 1. Bản ngắn gọn (Dùng để nhập trên Google Play Console - Dưới 500 ký tự)
```text
Bản phát hành AA Power Booster v2.1.2 (io.github.manhvu1212.aapowerbooster):
- Khắc phục triệt để lỗi "Không thể hoàn tất khi đang lái xe" bằng tiêu đề template cố định.
- Hiển thị mức độ nhạy (Level x) trực tiếp trên ô chế độ đang chọn thay vì trên tiêu đề màn hình.
```

---

## 2. Bản chi tiết (Changelog & Hướng dẫn kỹ thuật)

### Phiên bản 2.1.2:
1. **Tiêu đề cố định khắc phục lỗi lái xe:** Chuyển tiêu đề màn hình Android Auto thành chuỗi cố định `"AA Power Booster"` khi đã kết nối. Việc này ngăn chặn Android Auto đếm mỗi lần đổi chế độ hoặc thay đổi mức độ nhạy (+/-) là một "bước màn hình" mới (new step), giúp loại bỏ hoàn toàn lỗi khóa màn hình "Không thể hoàn tất việc này khi đang lái xe" (Do hạn mức 5 bước tối đa của hệ thống khi xe di chuyển).
2. **Hiển thị mức độ nhạy trực quan:** Thay vì hiển thị mức độ nhạy trên tiêu đề màn hình, mức độ nhạy của chế độ đang chọn sẽ được hiển thị trực tiếp làm tiêu đề của ô chế độ đó (ví dụ: hiển thị `"Level 5"` thay cho chữ `"Sport"` khi chế độ Sport đang kích hoạt). Chế độ Normal vẫn hiển thị là `"Normal"`.

### Đã có từ 2.1.1:
1. **Sửa lỗi Android Auto chặn khi đang lái:** khi bấm dồn dập (đổi chế độ/cấp/P/R liên tục), Android Auto có thể bung cảnh báo "Không thể hoàn tất việc này khi đang lái xe" do app đẩy template quá dày, vượt hạn mức của hệ thống. Đã khắc phục bằng cách: (a) gộp các lần vẽ lại thành tối đa một lần mỗi 300ms, và (b) luôn giữ cùng một loại bố cục lưới (không đổi sang màn thông báo), nên mỗi lần vẽ được tính là "làm mới" miễn phí — không còn chạm hạn mức.

### Đã có từ 2.1.0:
1. **Nút chuyển P/R trên Android Auto:** thêm ô thứ 6 trên màn hình xe để chuyển nhanh giữa **P** (bộ tăng lực hoạt động) và **R** (trả về ga nguyên bản), độc lập với 5 chế độ lái — không làm mất chế độ/cấp đang chọn.
2. **Biểu tượng trực quan:** trong khung bo góc có "P / R"; chữ của chế độ đang bật được **tô đặc**, chữ còn lại chỉ có **viền rỗng**, nên liếc là biết đang ở chế độ nào.
3. **Thông báo xác nhận:** khi thiết bị phản hồi, hiện CarToast "✓ Mode P" / "✓ Mode R" — giống cơ chế xác nhận đổi chế độ/cấp hiện có.

### Đã có từ 2.0.4:
1. **Giao diện tiếng Anh:** toàn bộ chữ hiển thị trong ứng dụng (điện thoại + Android Auto) đã chuyển sang tiếng Anh.
2. **Thông báo xác nhận khi thiết bị phản hồi:** sau khi bấm đổi chế độ/cấp, khi thiết bị gửi dữ liệu trả về, ứng dụng hiện thông báo ngắn (Toast trên điện thoại, CarToast trên Android Auto) dạng "✓ Race · Level 5" — bằng chứng lệnh đã tới thiết bị thật. Chỉ hiện khi do người dùng vừa bấm, không hiện khi đồng bộ nền.
3. **Chế độ Normal gọn hơn:** bỏ chữ "(stock)" — chỉ hiển thị "Normal".

### Đã có từ 2.0.3:
- **Android Auto:** chế độ đang chọn có viền tròn bao quanh biểu tượng; bỏ dòng "Cấp x" dưới biểu tượng (cấp độ vẫn hiển thị trên tiêu đề).

### Đã có từ 2.0.2:
- **Sửa logo trên Android Auto:** biểu tượng thích ứng nền đen tràn toàn bộ vòng tròn, cánh đỏ ở giữa — không còn viền trắng.
- **Đổi thứ tự chế độ:** thống nhất là **Race · Sport · City · Normal · Eco** trên cả điện thoại lẫn Android Auto.
- **Điện thoại:** hiển thị tên chế độ đang chọn ngay cạnh nhãn chọn chế độ.

### Đã có từ 2.0.1:
- **Sửa lỗi hiển thị tràn viền:** nội dung không còn bị thanh trạng thái, tai thỏ (notch) hay thanh điều hướng che khuất.

### Đã có từ 2.0.0 (nâng cấp lớn):
- **Giao diện điện thoại mới:** chọn chế độ bằng 5 biểu tượng cân đối; tự động kết nối thiết bị đã lưu; mục dò tìm chỉ hiện khi chưa lưu thiết bị.
- **Logo mới:** biểu tượng cánh đỏ trên nền đen.
- **Cấp độ riêng theo từng chế độ:** chuyển chế độ là khôi phục đúng cấp đã đặt.
- **Riêng tư:** không thu thập dữ liệu, không quyền Internet, không quảng cáo.

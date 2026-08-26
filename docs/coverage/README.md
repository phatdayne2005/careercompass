# Báo cáo độ bao phủ mã nguồn — JaCoCo

Ảnh chụp báo cáo bao phủ tại thời điểm hoàn thành **Phần A — kỹ thuật kiểm thử hộp đen**.

## Cách xem

Tải thư mục này về rồi mở `index.html` bằng trình duyệt. Không cần cài gì thêm.

GitHub không tự hiển thị được vì đây là trang HTML tĩnh nhiều tệp.

## Số liệu

| Chỉ số | Trước Phần A | Sau Phần A | Tăng |
|---|---|---|---|
| Dòng lệnh (Line) | 60,1% | **72,6%** | +12,5 |
| Nhánh (Branch) | 48,2% | **65,1%** | +16,9 |
| Độ phức tạp được phủ | 53,4% | 65,2% | +11,8 |
| Phương thức | 65,1% | 75,6% | +10,5 |
| Lớp | 75,4% | 84,6% | +9,2 |

Cột "Trước" đo ngày 19/08/2026, khi dự án có 198 unit test và chưa có test hộp đen.
Cột "Sau" đo sau khi bổ sung 32 test của Phần A.

## Ý nghĩa

Ba mươi hai test của Phần A được thiết kế bằng kỹ thuật **hộp đen** — phân hoạch lớp
tương đương, phân tích giá trị biên, bảng quyết định, chuyển đổi trạng thái. Chúng suy ra
từ **đặc tả**, hoàn toàn không nhắm vào việc phủ mã nguồn.

Vậy mà bao phủ nhánh tăng gần 17 điểm phần trăm. Điều này cho thấy kỹ thuật hộp đen có
giá trị thực chất, không chỉ là bài tập vẽ bảng.

Nhưng vẫn còn **34,9% nhánh chưa chạm** — đúng như slide 51 của chương IV:
*độ bao phủ 100% không có nghĩa là 100% được test*, và chiều ngược lại cũng đúng:
phủ trọn tiêu chí hộp đen không có nghĩa phủ trọn mã nguồn.

## Ví dụ cụ thể

Mở `vn.uth.careercompass.roadmap.service/ProgressService.java.html`, xem hàm
`updateProgress()`:

- Bảng quyết định của Phần A phủ **6/6 rule** — đạt tiêu chí đủ của kỹ thuật
- Nhưng bao phủ nhánh của hàm này chỉ **83,3%**

Ba nhánh còn thiếu đều là **kiểm tra đầu vào**, không thuộc luật nghiệp vụ nên bảng
quyết định không mô hình hoá:

```java
if (skillNodeId == null)   → 400 Bad Request
if (status == null)        → 400 Bad Request
.orElseThrow(...)          → 404 Not Found
```

Muốn phủ nốt phải dùng kỹ thuật **hộp trắng** (mục IV.4), tính số test tối thiểu bằng
độ phức tạp Cyclomatic. Hàm `updateProgress()` có **V(G) = 7**, bộ test hiện tại đi được
5 đường — thiếu đúng 2, khớp với hai nhánh `null` chưa chạm.

## Bảng màu của JaCoCo

| Dấu hiệu | Nghĩa |
|---|---|
| Nền xanh | Dòng đã chạy |
| Nền đỏ | Dòng chưa chạy |
| Nền vàng | Dòng đã chạy nhưng còn nhánh chưa đi |
| Kim cương xanh | Mọi nhánh của dòng đều đã chạy |
| Kim cương vàng | Còn nhánh chưa chạy |
| Kim cương đỏ | Không nhánh nào chạy |

## Tự tạo lại báo cáo

```bash
docker compose up -d          # cần MySQL cho CareerCompassApplicationTests
./mvnw clean test             # JaCoCo tự sinh báo cáo sau khi test xong
```

Kết quả nằm ở `target/site/jacoco/index.html`.

# Báo cáo độ bao phủ mã nguồn — JaCoCo

Ảnh chụp báo cáo bao phủ sau khi hoàn thành **Phần A — kỹ thuật kiểm thử hộp đen**.

## Cách xem

Tải thư mục này về rồi mở `index.html` bằng trình duyệt. Không cần cài gì thêm.

GitHub không tự hiển thị được vì đây là trang HTML tĩnh nhiều tệp.

## Số liệu

| Chỉ số | Trước Phần A | Sau Phần A | Tăng |
|---|---|---|---|
| Dòng lệnh (Line) | 60,1% | **75,1%** | +15,0 |
| Nhánh (Branch) | 48,2% | **66,5%** | +18,3 |
| Độ phức tạp được phủ | 53,4% | **66,4%** | +13,0 |
| Phương thức | 65,1% | **77,8%** | +12,7 |
| Lớp | 75,4% | **89,2%** | +13,8 |

Cột "Trước" đo ngày 19/08/2026, khi dự án có 198 test và chưa có test hộp đen nào.

Cột "Sau" đo sau khi bổ sung các test hộp đen của Phần A. Toàn dự án hiện có
**362 test**, trong đó:

| Số test | Gói | Kỹ thuật |
|---:|---|---|
| 19 | `blackbox.RegisterStandardBvaTest` | Standard + Robustness BVA |
| 17 | `blackbox.RegisterTagCoverageTest` | Gộp tag thành test case |
| 14 | `blackbox.RegisterEquivalencePartitionTest` | Phân hoạch lớp tương đương |
| 6 | `bva.OnboardingFileSizeBvaTest` | BVA dung lượng tệp |
| 6 | `blackbox.ProgressDecisionTableTest` | Bảng quyết định |
| 4 | `blackbox.TokenValidityDecisionTableTest` | Bảng quyết định |
| 9 | `blackbox.ProgressStateTransitionTest` | Chuyển đổi trạng thái |
| **75** | | **thuộc phạm vi báo cáo Phần A** |

## Ý nghĩa

Bảy mươi lăm test này được thiết kế bằng kỹ thuật **hộp đen** — phân hoạch lớp tương
đương, phân tích giá trị biên, bảng quyết định, chuyển đổi trạng thái. Chúng suy ra từ
**đặc tả**, hoàn toàn không nhắm vào việc phủ mã nguồn.

Vậy mà bao phủ nhánh tăng hơn 18 điểm phần trăm. Điều này cho thấy kỹ thuật hộp đen có
giá trị thực chất, không chỉ là bài tập vẽ bảng.

Nhưng vẫn còn **33,5% nhánh chưa chạm** — đúng như slide 51 của chương IV:
*độ bao phủ 100% không có nghĩa là 100% được test*, và chiều ngược lại cũng đúng:
phủ trọn tiêu chí hộp đen không có nghĩa phủ trọn mã nguồn.

## Ví dụ cụ thể

Mở `vn.uth.careercompass.onboarding.service/OnboardingService.java.html`, xem hàm
`saveTranscript()`:

- BVA dung lượng tệp của Phần A phủ **6/6 giá trị biên** — `0`, `1 byte`, `5 MB`,
  `10MB−1`, `10 MB`, `10MB+1`. Đạt tiêu chí đủ của kỹ thuật, tag `B22`–`B27` xanh hết
- Nhưng bao phủ nhánh của lớp này chỉ **75,0%** (9/12)

Ba nhánh còn thiếu nằm gọn ở **một dòng duy nhất**, dòng 38:

```java
if (!ext.equals(".pdf") && !ext.equals(".png")
        && !ext.equals(".jpg") && !ext.equals(".jpeg")) {
    throw new IllegalArgumentException("Chỉ chấp nhận file PDF, PNG hoặc JPG.");
}
```

Dòng này có 8 nhánh, mới đi được 5. Lý do rất rõ: mọi test đều đặt tên tệp là
`transcript.pdf`, nên ba nhánh `.png`, `.jpg`, `.jpeg` chưa bao giờ được thử.

Đây là minh hoạ sạch cho giới hạn của kỹ thuật giá trị biên: **đuôi tệp là một BIẾN
ĐẦU VÀO KHÁC**, không nằm trên trục dung lượng. BVA dung lượng dù làm hoàn hảo đến đâu
cũng không thể chạm tới nó — muốn phủ nốt phải phân hoạch lớp tương đương trên đuôi
tệp, hoặc dùng kỹ thuật **hộp trắng** (mục IV.4) để lần theo từng nhánh của biểu thức
điều kiện.

Ngược lại, những lớp mà kỹ thuật hộp đen mô hình hoá được trọn vẹn thì đã phủ kín:

| Lớp | Nhánh | Kỹ thuật đã áp |
|---|---|---|
| `ProgressService` | **100%** (12/12) | Bảng quyết định 6 luật + chuyển đổi trạng thái 9 cạnh |
| `PasswordResetToken` | **100%** (4/4) | Bảng quyết định 4 luật |
| `PasswordResetService` | **100%** (6/6) | — |

`ProgressService` từng dừng ở 83,3% ở lần đo trước, do ba nhánh kiểm tra đầu vào nằm
ngoài bảng quyết định. Bộ test hiện tại đã phủ nốt.

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

Kết quả nằm ở `target/site/jacoco/index.html`. Thư mục này là bản sao của nó.

Dùng `clean` để tránh cảnh báo `Execution data ... does not match` — cảnh báo đó xuất
hiện khi `target/jacoco.exec` còn dữ liệu của bytecode cũ.

# Báo cáo độ bao phủ mã nguồn — JaCoCo

Ảnh chụp báo cáo bao phủ sau khi hoàn thành **Phần A — kỹ thuật kiểm thử hộp đen**.

## Cách xem

Tải thư mục này về rồi mở `index.html` bằng trình duyệt. Không cần cài gì thêm.

GitHub không tự hiển thị được vì đây là trang HTML tĩnh nhiều tệp.

## Số liệu

| Chỉ số | Trước Phần A | Sau Phần A | Tăng |
|---|---|---|---|
| Dòng lệnh (Line) | 60,1% | **78,6%** | +18,5 |
| Nhánh (Branch) | 48,2% | **72,5%** | +24,3 |
| Độ phức tạp được phủ | 53,4% | **71,0%** | +17,6 |
| Phương thức | 65,1% | **81,2%** | +16,1 |
| Lớp | 75,4% | **89,4%** | +14,0 |

Cột "Trước" đo ngày 19/08/2026, khi dự án có 198 test và chưa có test hộp đen nào.

Cột "Sau" đo sau khi bổ sung các test hộp đen của Phần A. Toàn dự án hiện có
**408 test**, trong đó:

| Số test | Gói | Kỹ thuật |
|---:|---|---|
| 19 | `blackbox.RegisterStandardBvaTest` | Standard + Robustness BVA |
| 17 | `blackbox.RegisterTagCoverageTest` | Gộp tag thành test case |
| 14 | `blackbox.RegisterEquivalencePartitionTest` | Phân hoạch lớp tương đương |
| 6 | `bva.OnboardingFileSizeBvaTest` | BVA dung lượng tệp |
| 6 | `blackbox.ProgressDecisionTableTest` | Bảng quyết định |
| 4 | `blackbox.TokenValidityDecisionTableTest` | Bảng quyết định |
| 9 | `blackbox.ProgressStateTransitionTest` | Chuyển đổi trạng thái |
| 7 | `blackbox.TokenStateTransitionTest` | Chuyển đổi trạng thái |
| 9 | `blackbox.MentorTitleStandardBvaTest` | Standard + Robustness BVA |
| 9 | `blackbox.TranscriptFileDecisionTableTest` | Bảng quyết định |
| 12 | `blackbox.OnboardingStateTransitionTest` | Chuyển đổi trạng thái |
| **112** | | **thuộc phạm vi báo cáo Phần A** |

## Ý nghĩa

112 test này được thiết kế bằng kỹ thuật **hộp đen** — phân hoạch lớp tương
đương, phân tích giá trị biên, bảng quyết định, chuyển đổi trạng thái. Chúng suy ra từ
**đặc tả**, hoàn toàn không nhắm vào việc phủ mã nguồn.

Vậy mà bao phủ nhánh tăng hơn 24 điểm phần trăm. Điều này cho thấy kỹ thuật hộp đen có
giá trị thực chất, không chỉ là bài tập vẽ bảng.

Nhưng vẫn còn **27,5% nhánh chưa chạm** — đúng như slide 51 của chương IV:
*độ bao phủ 100% không có nghĩa là 100% được test*, và chiều ngược lại cũng đúng:
phủ trọn tiêu chí hộp đen không có nghĩa phủ trọn mã nguồn.

## Ví dụ cụ thể — một lỗ hổng đã được dự đoán, rồi được đóng

Phần này giữ lại nguyên mạch của lần đo trước, vì nó là ví dụ sạch nhất trong cả báo cáo
về việc **đọc số bao phủ để tìm ra kỹ thuật còn thiếu**.

### Lần đo trước: 75,0%

Mở `vn.uth.careercompass.onboarding.service/OnboardingService.java.html`, xem hàm
`saveTranscript()`:

- BVA dung lượng tệp phủ **6/6 giá trị biên** — `0`, `1 byte`, `5 MB`, `10MB−1`,
  `10 MB`, `10MB+1`. Đạt tiêu chí đủ của kỹ thuật, tag `B22`–`B27` xanh hết
- Nhưng bao phủ nhánh của lớp này chỉ **75,0%** (9/12)

Ba nhánh còn thiếu nằm gọn ở **một dòng duy nhất**, dòng kiểm đuôi tệp:

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
cũng không thể chạm tới nó.

### Lần đo này: 100,0%

Lần đo trước đã dự đoán cách đóng lỗ hổng — *"muốn phủ nốt phải phân hoạch lớp tương
đương trên đuôi tệp"*. Việc đã làm đúng như vậy, bằng một **bảng quyết định** ba điều
kiện (tên tệp null · đuôi tệp · dung lượng), sheet `03c` của báo cáo Excel:

| Chỉ số `OnboardingService` | Trước | Sau |
|---|---|---|
| Nhánh | 75,0% (9/12) | **100,0%** (14/14) |
| Dòng | 94,7% (18/19) | **100,0%** (19/19) |

Mẫu số tăng từ 12 lên 14 vì bản sửa **DEF-011** thêm một nhánh mới.

### DEF-011 — nói rõ ai phát hiện, ai buộc phải sửa

Dựng cột `R8` của bảng quyết định (*tệp không có dấu chấm*) chạm phải một khiếm khuyết
thật: `lastIndexOf(".")` trả `-1`, kéo theo `substring(-1)` ném
`StringIndexOutOfBoundsException` — người dùng nhận lỗi 500 thay vì thông báo 400.

Nhưng **bảng quyết định không phải nơi phát hiện nó**. Test hộp trắng
`OnboardingServiceTest` đã ghi đúng nguyên nhân từ trước, nguyên văn trong mã nguồn:

> `BUG?: filename thiếu phần mở rộng (vd "resume") làm substring(lastIndexOf(".")) với`
> `index=-1 ném StringIndexOutOfBoundsException, không phải thông báo "Tên file không`
> `hợp lệ". Nên guard lastIndexOf(".") < 0 để trả IllegalArgumentException cho đồng nhất.`

Chỉ có điều chính test đó lại **chốt luôn hành vi lỗi**:

```java
assertThatThrownBy(() -> onboardingService.saveTranscript(new User(), file))
        .isInstanceOf(StringIndexOutOfBoundsException.class);   // <- khoá hành vi SAI
```

Bộ test vì thế vẫn xanh, và khiếm khuyết nằm im suốt thời gian đó.

Đóng góp thật của kỹ thuật bảng quyết định là **buộc phải sửa**: mỗi cột của bảng phải
ứng với một hành động xác định trong nhóm `A1`–`A4`, không thể để một ô là *"sập 500"*.
Một ghi chú `BUG?` có thể sống mãi trong mã nguồn; một ô trống trong bảng quyết định thì
không sống sót qua khâu duyệt.

## Những lớp đã phủ kín, và những lớp còn hở

Ngược lại, những lớp mà kỹ thuật hộp đen mô hình hoá được trọn vẹn thì đã phủ kín:

| Lớp | Nhánh | Kỹ thuật đã áp |
|---|---|---|
| `ProgressService` | **100%** (12/12) | Bảng quyết định 6 luật + chuyển đổi trạng thái 9 cạnh |
| `PasswordResetToken` | **100%** (4/4) | Bảng quyết định 4 luật |
| `PasswordResetService` | **100%** (6/6) | — |
| `OnboardingService` | **100%** (14/14) | Bảng quyết định 8 rule + BVA dung lượng 6 biên |

Và hai lớp còn hở, ghi lại để không nhận công quá tay:

| Lớp | Nhánh | Vì sao còn hở |
|---|---|---|
| `MentorService` | 87,5% (14/16) | BVA ngưỡng cắt tiêu đề chỉ mô hình hoá một biến. Hai nhánh còn lại thuộc khối `try/catch` gọi LLM và các phép kiểm `null` khi dựng prompt — chúng là *xử lý sự cố hạ tầng*, không phải luật nghiệp vụ suy ra được từ đặc tả |
| `OnboardingController` | 57,5% (23/40) | Máy trạng thái phủ trọn các cạnh chuyển bước, nhưng nhánh xử lý tệp tải lên ở `POST step2` (tệp rỗng · lỗi lưu · phân tích bảng điểm trả `null`) nằm ngoài mô hình trạng thái |

Cả hai đều đúng như slide 51 của chương IV: *độ bao phủ 100% không có nghĩa là 100% được
test*, và chiều ngược lại cũng đúng — **phủ trọn tiêu chí hộp đen không có nghĩa phủ trọn
mã nguồn**. Muốn đóng nốt hai dòng trên phải dùng kỹ thuật hộp trắng (mục IV.4), lần theo
từng nhánh của đồ thị dòng điều khiển.

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

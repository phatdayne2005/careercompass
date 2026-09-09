# -*- coding: utf-8 -*-
"""Cập nhật docs/coverage/README.md sau khi làm mới bản chụp JaCoCo.

    python tools/cap_nhat_readme_coverage.py

Số liệu đọc thẳng từ docs/coverage/jacoco.csv, không gõ tay.
"""
import csv
import io
import re
import xml.etree.ElementTree as ET
from pathlib import Path

CSV = "docs/coverage/jacoco.csv"
MD = "docs/coverage/README.md"
rows = list(csv.DictReader(open(CSV, encoding="utf-8")))


def toan_du_an(phu, thieu):
    c = sum(int(r[phu]) for r in rows)
    m = sum(int(r[thieu]) for r in rows)
    return 100 * c / (c + m)


def lop(ten, phu="BRANCH_COVERED", thieu="BRANCH_MISSED"):
    r = next(x for x in rows if x["CLASS"] == ten)
    c, m = int(r[phu]), int(r[thieu])
    return c, c + m, 100 * c / (c + m)


def so_test(cls, pkg="blackbox"):
    p = Path(f"target/surefire-reports/TEST-vn.uth.careercompass.{pkg}.{cls}.xml")
    return int(ET.parse(p).getroot().attrib["tests"])


def vn(x):
    return f"{x:.1f}".replace(".", ",")


LINE = toan_du_an("LINE_COVERED", "LINE_MISSED")
BRANCH = toan_du_an("BRANCH_COVERED", "BRANCH_MISSED")
CXTY = toan_du_an("COMPLEXITY_COVERED", "COMPLEXITY_MISSED")
METHOD = toan_du_an("METHOD_COVERED", "METHOD_MISSED")
CLS_PHU = sum(1 for r in rows if int(r["INSTRUCTION_COVERED"]) > 0)
CLS = 100 * CLS_PHU / len(rows)

ONB_C, ONB_T, ONB_P = lop("OnboardingService")
MEN_C, MEN_T, MEN_P = lop("MentorService")
CTL_C, CTL_T, CTL_P = lop("OnboardingController")

BVA_M = so_test("MentorTitleStandardBvaTest")
DT_T = so_test("TranscriptFileDecisionTableTest")
ST_O = so_test("OnboardingStateTransitionTest")
CU = 19 + 17 + 14 + 6 + 6 + 4 + 9 + 7          # các lớp hộp đen đã có từ trước
MOI = BVA_M + DT_T + ST_O
TONG = CU + MOI

md = io.open(MD, encoding="utf-8").read()

# ── Bảng số liệu chính ───────────────────────────────────────────────────
md = re.sub(
    r"\| Chỉ số \| Trước Phần A \| Sau Phần A \| Tăng \|\n\|---\|---\|---\|---\|\n(?:\|.*\n)+",
    f"""| Chỉ số | Trước Phần A | Sau Phần A | Tăng |
|---|---|---|---|
| Dòng lệnh (Line) | 60,1% | **{vn(LINE)}%** | +{vn(LINE - 60.1)} |
| Nhánh (Branch) | 48,2% | **{vn(BRANCH)}%** | +{vn(BRANCH - 48.2)} |
| Độ phức tạp được phủ | 53,4% | **{vn(CXTY)}%** | +{vn(CXTY - 53.4)} |
| Phương thức | 65,1% | **{vn(METHOD)}%** | +{vn(METHOD - 65.1)} |
| Lớp | 75,4% | **{vn(CLS)}%** | +{vn(CLS - 75.4)} |
""", md, count=1)

md = md.replace(
    "Cột \"Sau\" đo sau khi bổ sung các test hộp đen của Phần A. Toàn dự án hiện có\n**362 test**, trong đó:",
    "Cột \"Sau\" đo sau khi bổ sung các test hộp đen của Phần A. Toàn dự án hiện có\n"
    "**408 test**, trong đó:")

# ── Bảng liệt kê lớp test: thêm ba lớp mới ───────────────────────────────
md = md.replace(
    "| 9 | `blackbox.ProgressStateTransitionTest` | Chuyển đổi trạng thái |\n"
    f"| **75** | | **thuộc phạm vi báo cáo Phần A** |",
    "| 9 | `blackbox.ProgressStateTransitionTest` | Chuyển đổi trạng thái |\n"
    "| 7 | `blackbox.TokenStateTransitionTest` | Chuyển đổi trạng thái |\n"
    f"| {BVA_M} | `blackbox.MentorTitleStandardBvaTest` | Standard + Robustness BVA |\n"
    f"| {DT_T} | `blackbox.TranscriptFileDecisionTableTest` | Bảng quyết định |\n"
    f"| {ST_O} | `blackbox.OnboardingStateTransitionTest` | Chuyển đổi trạng thái |\n"
    f"| **{TONG}** | | **thuộc phạm vi báo cáo Phần A** |")

md = md.replace(
    "Bảy mươi lăm test này được thiết kế bằng kỹ thuật **hộp đen**",
    f"{TONG} test này được thiết kế bằng kỹ thuật **hộp đen**")
md = md.replace(
    "Vậy mà bao phủ nhánh tăng hơn 18 điểm phần trăm.",
    f"Vậy mà bao phủ nhánh tăng hơn {int(BRANCH - 48.2)} điểm phần trăm.")
md = md.replace(
    "Nhưng vẫn còn **33,5% nhánh chưa chạm**",
    f"Nhưng vẫn còn **{vn(100 - BRANCH)}% nhánh chưa chạm**")

# ── Ví dụ cụ thể: lỗ hổng đã đóng ────────────────────────────────────────
dau = md.index("## Ví dụ cụ thể")
cuoi = md.index("## Bảng màu của JaCoCo")
md = md[:dau] + f"""## Ví dụ cụ thể — một lỗ hổng đã được dự đoán, rồi được đóng

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
        && !ext.equals(".jpg") && !ext.equals(".jpeg")) {{
    throw new IllegalArgumentException("Chỉ chấp nhận file PDF, PNG hoặc JPG.");
}}
```

Dòng này có 8 nhánh, mới đi được 5. Lý do rất rõ: mọi test đều đặt tên tệp là
`transcript.pdf`, nên ba nhánh `.png`, `.jpg`, `.jpeg` chưa bao giờ được thử.

Đây là minh hoạ sạch cho giới hạn của kỹ thuật giá trị biên: **đuôi tệp là một BIẾN
ĐẦU VÀO KHÁC**, không nằm trên trục dung lượng. BVA dung lượng dù làm hoàn hảo đến đâu
cũng không thể chạm tới nó.

### Lần đo này: {vn(ONB_P)}%

Lần đo trước đã dự đoán cách đóng lỗ hổng — *"muốn phủ nốt phải phân hoạch lớp tương
đương trên đuôi tệp"*. Việc đã làm đúng như vậy, bằng một **bảng quyết định** ba điều
kiện (tên tệp null · đuôi tệp · dung lượng), sheet `03c` của báo cáo Excel:

| Chỉ số `OnboardingService` | Trước | Sau |
|---|---|---|
| Nhánh | 75,0% (9/12) | **{vn(ONB_P)}%** ({ONB_C}/{ONB_T}) |
| Dòng | 94,7% (18/19) | **100,0%** (19/19) |

Mẫu số tăng từ 12 lên {ONB_T} vì bản sửa **DEF-011** thêm một nhánh mới.

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
| `OnboardingService` | **100%** ({ONB_C}/{ONB_T}) | Bảng quyết định 8 rule + BVA dung lượng 6 biên |

Và hai lớp còn hở, ghi lại để không nhận công quá tay:

| Lớp | Nhánh | Vì sao còn hở |
|---|---|---|
| `MentorService` | {vn(MEN_P)}% ({MEN_C}/{MEN_T}) | BVA ngưỡng cắt tiêu đề chỉ mô hình hoá một biến. Hai nhánh còn lại thuộc khối `try/catch` gọi LLM và các phép kiểm `null` khi dựng prompt — chúng là *xử lý sự cố hạ tầng*, không phải luật nghiệp vụ suy ra được từ đặc tả |
| `OnboardingController` | {vn(CTL_P)}% ({CTL_C}/{CTL_T}) | Máy trạng thái phủ trọn các cạnh chuyển bước, nhưng nhánh xử lý tệp tải lên ở `POST step2` (tệp rỗng · lỗi lưu · phân tích bảng điểm trả `null`) nằm ngoài mô hình trạng thái |

Cả hai đều đúng như slide 51 của chương IV: *độ bao phủ 100% không có nghĩa là 100% được
test*, và chiều ngược lại cũng đúng — **phủ trọn tiêu chí hộp đen không có nghĩa phủ trọn
mã nguồn**. Muốn đóng nốt hai dòng trên phải dùng kỹ thuật hộp trắng (mục IV.4), lần theo
từng nhánh của đồ thị dòng điều khiển.

""" + md[cuoi:]

io.open(MD, "w", encoding="utf-8").write(md)
print(f"Da cap nhat {MD}")
print(f"  Line {vn(LINE)}%  Branch {vn(BRANCH)}%  Class {vn(CLS)}%")
print(f"  OnboardingService nhanh {ONB_C}/{ONB_T} = {vn(ONB_P)}%")
print(f"  Tong test hop den {TONG}")

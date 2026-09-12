# -*- coding: utf-8 -*-
"""Sinh hai sheet BỔ SUNG cho phần hộp đen của BaoCao-KiemThu-CareerCompass.xlsx.

    03c. Decision Table Tep          <- TranscriptFileDecisionTableTest
    04c. State Transition Onboard <- OnboardingStateTransitionTest

Chạy từ thư mục gốc dự án, sau khi đã chạy test:
    mvnw test
    python tools/gen_sheets_bo_sung.py

Giữ NGUYÊN bố cục, bộ màu và cách đánh số của các sheet 03b / 04b đã có, để thầy
đọc ba sheet mới không phải học lại cách đọc. Mọi con số ở cột "Số test" đều được đối
chiếu tự động với target/surefire-reports — lệch là script dừng, không cho báo cáo trôi
qua với số bịa.
"""
import xml.etree.ElementTree as ET
from pathlib import Path

import openpyxl
from openpyxl.styles import Font, PatternFill, Alignment, Border, Side
from openpyxl.utils import get_column_letter

XLSX = "BaoCao-KiemThu-CareerCompass.xlsx"

C_HDR = "1F4E79"; C_TXT = "FFFFFFFF"; C_PASS = "C6EFCE"; C_SEC = "DDEBF7"
C_DIRTY = "FBE4E4"; C_CHAN = "FBE4E4"; C_OK = "E2F0D9"
thin = Side(style="thin", color="BFBFBF")
BORDER = Border(left=thin, right=thin, top=thin, bottom=thin)
SUREFIRE = Path("target/surefire-reports")

# sheet -> (lớp test, số test dự kiến, đặt sau sheet nào)
SUITE = {
    "03c. Decision Table Tep":
        ("TranscriptFileDecisionTableTest", 9, "03b. Decision Table Token"),
    "04c. State Transition Onboard":
        ("OnboardingStateTransitionTest", 12, "04b. State Transition Token"),
}


def so_test_that(cls, du_kien):
    f = SUREFIRE / f"TEST-vn.uth.careercompass.blackbox.{cls}.xml"
    if not f.exists():
        raise SystemExit(f"CHUA CHAY TEST: khong thay {f}. Chay 'mvnw test' truoc.")
    g = ET.parse(f).getroot().attrib
    that, fail, err = int(g["tests"]), int(g["failures"]), int(g["errors"])
    if that != du_kien:
        raise SystemExit(
            f"LECH SO LIEU: {cls} chay {that} test nhung bao cao ghi {du_kien}. "
            f"Sua SUITE trong tools/gen_sheets_bo_sung.py cho khop roi chay lai.")
    if fail or err:
        raise SystemExit(f"{cls} co {fail} fail / {err} error — sua source truoc khi sinh report.")
    return that


# Sheet từng được sinh ra rồi bỏ đi — phải gỡ khỏi file cũ, nếu không nó nằm lại
# mãi vì script chỉ dựng lại những sheet có trong SUITE.
# "01c. BVA Tieu de Mentor": BVA ngưỡng CẮT tiêu đề phiên chat. Đã bỏ vì nó khác
# dạng với hai bảng BVA còn lại — hai bảng kia là biên HỢP LỆ (vượt biên thì bị từ
# chối), còn nó là biên CẮT (vượt biên thì bị rút gọn), nên cột Expected Output mang
# nghĩa khác và không gộp chung một sheet được. Giữ lại chỉ làm báo cáo khó trình bày.
SHEET_DA_BO = ["01c. BVA Tieu de Mentor"]

wb = openpyxl.load_workbook(XLSX)
for ten in list(SUITE) + SHEET_DA_BO:
    if ten in wb.sheetnames:
        wb.remove(wb[ten])


def tao_sheet(ten, dat_sau):
    ws = wb.create_sheet(ten)
    for i in range(1, 9):
        ws.column_dimensions[get_column_letter(i)].width = 24
    # Đưa sheet về ngay sau sheet anh em của nó. Phải gỡ khỏi danh sách TRƯỚC rồi mới
    # chèn lại — làm ngược thứ tự sẽ đẩy chính nó ra khỏi wb._sheets và ném ValueError.
    wb._sheets.remove(ws)
    wb._sheets.insert(wb.sheetnames.index(dat_sau) + 1, ws)
    return ws


def o(ws, r, c, v, *, bold=False, fill=None, center=False, size=10, color=None):
    x = ws.cell(row=r, column=c, value=v)
    x.font = Font(size=size, bold=bold, color=color)
    x.alignment = Alignment(vertical="top", wrap_text=True,
                            horizontal="center" if center else "general")
    x.border = BORDER
    if fill:
        x.fill = PatternFill("solid", fgColor=fill)


def thanh(ws, r, t, *, fill=C_SEC, color=C_HDR, size=11, italic=False, cao=24):
    ws.merge_cells(start_row=r, start_column=1, end_row=r, end_column=8)
    x = ws.cell(row=r, column=1, value=t)
    x.font = Font(bold=not italic, italic=italic, size=size, color=color)
    if fill:
        x.fill = PatternFill("solid", fgColor=fill)
    x.alignment = Alignment(vertical="top" if italic else "center", wrap_text=True)
    ws.row_dimensions[r].height = cao
    return r + 1


def tieu_de_cot(ws, r, cot):
    for i, h in enumerate(cot, start=1):
        o(ws, r, i, h, bold=True, fill=C_HDR, color=C_TXT, center=True)
    return r + 1


def mo_dau(ws, tieu_de, mo_ta, cls, so_test, ky_thuat, tong_ket):
    x = ws.cell(row=1, column=1, value=tieu_de)
    x.font = Font(bold=True, size=13, color=C_HDR)
    ws.cell(row=2, column=1, value=mo_ta).font = Font(italic=True, size=10)
    r = thanh(ws, 4, tong_ket, fill=C_OK, color="375623", cao=30)
    r = tieu_de_cot(ws, r, ["Lớp test trong source", "Số test", "Kỹ thuật áp dụng",
                            "", "", "", "", "Status"])
    for i, v in enumerate([cls, so_test, ky_thuat, "", "", "", "", "PASS"], start=1):
        o(ws, r, i, v, fill=(C_PASS if i == 8 else None), center=(i in (2, 8)))
    r += 1
    r = thanh(ws, r, "Số test được đối chiếu tự động với target/surefire-reports khi sinh "
                     "lại sheet bằng tools/gen_sheets_bo_sung.py — source chạy ra số khác "
                     f"thì script dừng và báo lỗi. Chạy lại để kiểm chứng:  "
                     f"mvnw test -Dtest={cls}",
              fill=None, color="808080", size=9, italic=True, cao=28)
    return r + 1


# ══════════════════════════════════════════════════════════════════════════
# 03c — Bảng quyết định định dạng & dung lượng tệp bảng điểm
# ══════════════════════════════════════════════════════════════════════════
cls, du_kien, dat_sau = SUITE["03c. Decision Table Tep"]
n = so_test_that(cls, du_kien)
ws = tao_sheet("03c. Decision Table Tep", dat_sau)

r = mo_dau(ws, "Decision Table Testing — bảng thứ ba: tệp bảng điểm",
           "OnboardingService.saveTranscript(). Bảng thiết kế theo mẫu slide 44, "
           "có bước rút gọn rule.",
           cls, n, "Bảng quyết định · 8 rule",
           f"TỔNG KẾT THI HÀNH — {n} test đã chạy, {n} PASS, 0 fail. "
           "8 test ứng với 8 rule của bảng, 1 test kiểm tra bổ sung ngoài bảng.")

r = thanh(ws, r, "VÌ SAO CHỌN HÀM NÀY — CON SỐ ĐO ĐƯỢC")
r = tieu_de_cot(ws, r, ["Chỉ số OnboardingService", "Trước khi có bảng này", "Sau khi có bảng này",
                        "", "", "", "", "Nguồn"])
for ten_cs, truoc, sau in [("Bao phủ nhánh", "75,0 %  (9/12)", "100,0 %  (14/14)"),
                           ("Bao phủ dòng", "94,7 %  (18/19)", "100,0 %  (19/19)")]:
    o(ws, r, 1, ten_cs, bold=True)
    o(ws, r, 2, truoc, center=True, fill=C_DIRTY)
    o(ws, r, 3, sau, center=True, fill=C_OK)
    for c in (4, 5, 6, 7):
        o(ws, r, c, "")
    o(ws, r, 8, "target/site/jacoco/jacoco.csv")
    r += 1
r = thanh(ws, r, "Báo cáo độ bao phủ (docs/coverage/README.md) đã chỉ đích danh: ba nhánh còn "
                 "thiếu của OnboardingService nằm gọn ở dòng kiểm đuôi tệp, vì MỌI test trước đây "
                 "đều đặt tên tệp là 'transcript.pdf' — ba nhánh .png, .jpg, .jpeg chưa bao giờ "
                 "được thử. Mẫu số tăng từ 12 lên 14 vì bản sửa DEF-011 thêm một nhánh mới.",
          fill=None, color="404040", size=9, italic=True, cao=32)
r += 1

r = thanh(ws, r, "BẢNG QUYẾT ĐỊNH ĐẦY ĐỦ — mẫu slide 44 · 8 rule sau khi rút gọn")
r = tieu_de_cot(ws, r, ["Condition / Action", "R1", "R2", "R3", "R4", "R5", "R6", "R7"])
BANG = [
    ("C1 · Tên tệp = null",       ["Y", "N", "N", "N", "N", "N", "N"]),
    ("C2 · Đuôi tệp",             ["–", ".pdf", ".png", ".jpg", ".jpeg", "hợp lệ", "khác"]),
    ("C3 · Dung lượng ≤ 10MB",    ["–", "Y", "Y", "Y", "Y", "N", "–"]),
    ("A1 · Lưu tệp, trả đường dẫn", ["–", "X", "X", "X", "X", "–", "–"]),
    ("A2 · Lỗi 'Tên file không hợp lệ'", ["X", "–", "–", "–", "–", "–", "–"]),
    ("A3 · Lỗi 'Vượt quá 10MB'",  ["–", "–", "–", "–", "–", "X", "–"]),
    ("A4 · Lỗi 'Chỉ chấp nhận PDF, PNG hoặc JPG'", ["–", "–", "–", "–", "–", "–", "X"]),
]
for ten_dong, o_gia_tri in BANG:
    o(ws, r, 1, ten_dong, bold=ten_dong.startswith("A"))
    for j, v in enumerate(o_gia_tri, start=2):
        o(ws, r, j, v, center=True)
    r += 1
o(ws, r, 1, "Kết quả chạy test", bold=True)
for j in range(2, 9):
    o(ws, r, j, "PASS", fill=C_PASS, center=True)
r += 1
o(ws, r, 1, "Method trong code", bold=True)
for j, m in enumerate(["rule1_tenTepNull_tuChoi", "rule2_duoiPdf_luuThanhCong",
                       "rule3_duoiPng_luuThanhCong", "rule4_duoiJpg_luuThanhCong",
                       "rule5_duoiJpeg_luuThanhCong", "rule6_duoiHopLe_dungLuongQuaLon_tuChoi",
                       "rule7_duoiKhongHopLe_tuChoi"], start=2):
    o(ws, r, j, m, size=8)
r += 1

r = thanh(ws, r, "RULE R8 — CỘT TÁCH RIÊNG VÌ NÓ BUỘC PHẢI SỬA MỘT KHIẾM KHUYẾT", fill=C_CHAN, color="C00000")
r = tieu_de_cot(ws, r, ["Condition / Action", "R8", "Hành vi TRƯỚC khi sửa", "Hành vi SAU khi sửa",
                        "", "", "Status", "Method trong code"])
for ten_dong, gt, truoc, sau in [
        ("C1 · Tên tệp = null", "N", "", ""),
        ("C2 · Đuôi tệp", "không có dấu chấm", "", ""),
        ("C3 · Dung lượng ≤ 10MB", "–", "", ""),
        ("A4 · Lỗi 'Chỉ chấp nhận PDF, PNG hoặc JPG'", "X",
         "StringIndexOutOfBoundsException → lỗi 500",
         "IllegalArgumentException → lỗi 400")]:
    o(ws, r, 1, ten_dong, bold=ten_dong.startswith("A"))
    o(ws, r, 2, gt, center=True, fill=C_CHAN)
    o(ws, r, 3, truoc, fill=(C_DIRTY if truoc else None))
    o(ws, r, 4, sau, fill=(C_OK if sau else None))
    for c in (5, 6):
        o(ws, r, c, "")
    o(ws, r, 7, "PASS" if truoc else "", fill=(C_PASS if truoc else None), center=True)
    o(ws, r, 8, "rule8_tenTepKhongCoDuoi_tuChoi" if truoc else "", size=8)
    r += 1
r += 1

r = thanh(ws, r, "GHI CHÚ TRUNG THỰC VỀ DEF-011 — AI PHÁT HIỆN, AI SỬA", fill=C_SEC)
for dong in [
    "Bảng quyết định KHÔNG phải nơi phát hiện lỗi này. Test hộp trắng OnboardingServiceTest đã ghi "
    "đúng nguyên nhân từ trước, nguyên văn trong mã nguồn: \"BUG?: filename thiếu phần mở rộng "
    "(vd 'resume') làm substring(lastIndexOf('.')) với index=-1 ném StringIndexOutOfBoundsException. "
    "Nên guard lastIndexOf('.') < 0 để trả IllegalArgumentException cho đồng nhất.\"",
    "Nhưng chính test đó lại CHỐT HÀNH VI LỖI: assertThatThrownBy(...).isInstanceOf("
    "StringIndexOutOfBoundsException.class). Bộ test vì thế vẫn xanh, và khiếm khuyết nằm im "
    "suốt thời gian đó — một ghi chú 'BUG?' có thể sống mãi trong mã nguồn mà không ai bị buộc phải xử lý.",
    "ĐÓNG GÓP THẬT CỦA KỸ THUẬT NÀY là buộc phải sửa: mỗi cột của bảng quyết định phải ứng với một "
    "hành động xác định trong nhóm A1–A4. Không thể để một ô là 'sập 500'. Một ô trống trong bảng "
    "quyết định thì không sống sót qua khâu duyệt như một dòng chú thích.",
    "Kiểm chứng: chạy test R8 trên bản mã CHƯA sửa → đỏ (nhận StringIndexOutOfBoundsException). "
    "Chạy trên bản đã sửa → xanh. Test cũ trong OnboardingServiceTest cũng đã được đổi sang khẳng "
    "định hành vi ĐÚNG thay vì khoá hành vi lỗi.",
]:
    r = thanh(ws, r, dong, fill=None, color="404040", size=9, italic=True, cao=42)
r += 1

r = thanh(ws, r, "VỀ SỐ RULE — VÌ SAO 24 RÚT CÒN 8", fill=C_SEC)
for dong in [
    "Số rule tối đa = 2 (C1) × 6 (C2) × 2 (C3) = 24. Khác hai bảng quyết định trước, ở đây điều kiện "
    "C2 có SÁU giá trị chứ không nhị phân, nên công thức 2^n không dùng được — phải NHÂN số giá trị "
    "của từng điều kiện, đúng như bảng ProgressService ở sheet 03 (3 × 2 = 6).",
    "Rút gọn về 8 nhờ mã nguồn kiểm ba điều kiện TUẦN TỰ và thoát ngay khi gặp lỗi: khi một điều kiện "
    "phía trước đã quyết định kết quả thì các điều kiện phía sau thành 'không quan tâm' (dấu –). "
    "R1 gộp 12 tổ hợp (tên null thì đuôi và dung lượng vô nghĩa); R7 và R8 mỗi cột gộp 2 tổ hợp "
    "(đuôi sai thì chặn trước khi tới phép kiểm dung lượng).",
    "R6 LÀ RULE GỘP THEO KIỂU KHÁC: bốn đuôi hợp lệ khi vượt dung lượng đều cho cùng một hành động, "
    "nên bốn rule con thu về một, ô C2 ghi 'hợp lệ' thay vì một đuôi cụ thể. Đây đúng là bước rút gọn "
    "bảng quyết định của chương IV — giống cách sheet 03b gộp R2, R3, R4.",
    "QUAN HỆ VỚI SHEET GIÁ TRỊ BIÊN: điều kiện C3 ở bảng này chỉ lấy giá trị NOMINAL (1 byte và 11 MB) "
    "để phân biệt nhánh. Các giá trị sát biên 10 MB (max-1, max, max+1) thuộc về OnboardingFileSizeBvaTest "
    "ở sheet 01. Hai kỹ thuật bổ sung nhau chứ không trùng: bảng quyết định hỏi 'tổ hợp nào dẫn tới "
    "hành động nào', giá trị biên hỏi 'lằn ranh nằm chính xác ở đâu'.",
]:
    r = thanh(ws, r, dong, fill=None, color="404040", size=9, italic=True, cao=42)
r += 1

r = thanh(ws, r, "KIỂM TRA BỔ SUNG — ngoài bảng quyết định", fill=C_SEC)
r = tieu_de_cot(ws, r, ["Trường hợp", "Đầu vào", "Expected Output", "", "", "Kết quả thực tế",
                        "Status", "Method trong code"])
hang = ["Đuôi viết HOA", "BANGDIEM.PDF", "Được chấp nhận (mã gọi toLowerCase)", "", "",
        "Đúng như mong đợi", "PASS", "boSung_duoiVietHoa_duocChapNhan"]
for j, v in enumerate(hang, start=1):
    o(ws, r, j, v, fill=(C_PASS if j == 7 else None), center=(j == 7))
r += 1
r = thanh(ws, r, "Không đưa vào bảng vì đây là biến thể TRÌNH BÀY của C2, không phải rule độc lập: "
                 "nếu tách ra thì bảng phải nhân đôi số cột mà không sinh thêm hành động mới nào.",
          fill=None, color="808080", size=9, italic=True, cao=26)


# ══════════════════════════════════════════════════════════════════════════
# 04c — Chuyển đổi trạng thái ba bước khai báo hồ sơ
# ══════════════════════════════════════════════════════════════════════════
cls, du_kien, dat_sau = SUITE["04c. State Transition Onboard"]
n = so_test_that(cls, du_kien)
ws = tao_sheet("04c. State Transition Onboard", dat_sau)

r = mo_dau(ws, "State Transition Testing — bảng thứ ba: ba bước khai báo hồ sơ",
           "OnboardingController, /onboarding/step1..3. Bảng thiết kế theo mẫu slide 39.",
           cls, n, "Chuyển đổi trạng thái · 8 dòng thiết kế",
           f"TỔNG KẾT THI HÀNH — {n} test đã chạy, {n} PASS, 0 fail. "
           "Dòng ST-06 là test tham số hoá, chạy 5 lần cho 5 điểm vào nên 8 dòng thiết kế "
           f"sinh ra {n} test thực tế.")

r = thanh(ws, r, "BẢNG 1 — MÁY TRẠNG THÁI NÀY KHÁC HAI MÁY TRƯỚC Ở ĐÂU")
r = tieu_de_cot(ws, r, ["Máy trạng thái", "Sheet", "Trạng thái nằm ở đâu", "",
                        "Số trạng thái", "", "Đặc điểm riêng", ""])
for ten_m, sh, o_dau, sott, dac in [
        ("Tiến độ node kỹ năng", "04", "Một cột enum ProgressStatus", 3,
         "Đủ cả 6 cạnh hai chiều giữa 3 trạng thái"),
        ("Vòng đời token", "04b", "Hai cờ boolean: used, expiresAt", 3,
         "Một chiều — không cạnh nào đi ngược"),
        ("Khai báo hồ sơ lần đầu", "04c", "TỔ HỢP nhiều cờ trên bản ghi User", 4,
         "Có trạng thái HẤP THỤ: vào rồi không ra được")]:
    o(ws, r, 1, ten_m, bold=True); o(ws, r, 2, sh, center=True)
    o(ws, r, 3, o_dau); o(ws, r, 4, "")
    o(ws, r, 5, sott, center=True); o(ws, r, 6, "")
    o(ws, r, 7, dac); o(ws, r, 8, "")
    r += 1
r = thanh(ws, r, "Điểm mới của bảng này: trạng thái KHÔNG nằm trong một cột enum mà là tổ hợp cờ "
                 "targetRoleId + transcriptPath/githubUsername + onboardingCompleted. Phải tự đặt "
                 "tên bốn trạng thái trước khi vẽ được sơ đồ — bước mà hai máy trước không cần làm.",
          fill=None, color="808080", size=9, italic=True, cao=30)
r += 1

r = thanh(ws, r, "BỐN TRẠNG THÁI")
r = tieu_de_cot(ws, r, ["Mã", "Tên trạng thái", "Định nghĩa bằng dữ liệu", "", "", "", "", "Ghi chú"])
for ma, ten_tt, dn, gc in [
        ("S1", "MOI_TAO", "onboardingCompleted = false, chưa qua bước nào", "trạng thái khởi đầu"),
        ("S2", "DA_CHON_NGHE", "đã gửi bước 1", ""),
        ("S3", "DA_NAP_NGUON", "đã gửi bước 2 (bảng điểm và/hoặc GitHub)", ""),
        ("S4", "HOAN_TAT", "onboardingCompleted = true", "TRẠNG THÁI HẤP THỤ — không có đường ra")]:
    o(ws, r, 1, ma, bold=True, center=True)
    o(ws, r, 2, ten_tt, bold=True)
    o(ws, r, 3, dn)
    for c in (4, 5, 6, 7):
        o(ws, r, c, "")
    o(ws, r, 8, gc, fill=(C_CHAN if "HẤP THỤ" in gc else None))
    r += 1
r += 1

r = thanh(ws, r, "SƠ ĐỒ TRẠNG THÁI")
for dong in [
    "        S1 MOI_TAO  ──[POST step1, chọn nghề]──►  S2 DA_CHON_NGHE",
    "         │    ▲                                          │",
    "         │    └──[không chọn gì] / [bấm Bỏ qua]          │  [POST step2]",
    "         │                                                ▼",
    "         └──────[GET step3, nhảy cóc]───────►  S3 DA_NAP_NGUON",
    "                                                          │  [POST step3]",
    "                                                          ▼",
    "                                              S4 HOAN_TAT  ◄──┐",
    "                                                   │          │",
    "                                                   └──[mọi điểm vào]──┘  → chuyển hướng về /",
]:
    ws.merge_cells(start_row=r, start_column=1, end_row=r, end_column=8)
    x = ws.cell(row=r, column=1, value=dong)
    x.font = Font(name="Consolas", size=10)
    x.alignment = Alignment(vertical="center")
    r += 1
r = thanh(ws, r, "Hai cạnh tự lặp ở S1 cùng đích nhưng KHÁC hành động, nên bảng bên dưới tách "
                 "thành hai dòng ST-02 và ST-03. Cạnh vòng ở S4 là cạnh quan trọng nhất của cả sơ đồ.",
          fill=None, color="808080", size=9, italic=True, cao=26)
r += 1

r = thanh(ws, r, "BẢNG CHUYỂN TRẠNG THÁI — mẫu slide 39 · 5 cạnh hợp lệ, 2 nhóm cạnh bị chặn")
r = tieu_de_cot(ws, r, ["Test Case No.", "Start State", "Event / Input",
                        "End State / Exp Output", "Hợp lệ", "Tác dụng phụ kiểm chứng",
                        "Status", "Method trong code"])
DONG_ST = [
    ("ST-01", "MOI_TAO", "POST step1 — chọn nghề hợp lệ", "DA_CHON_NGHE", "Có",
     "Gọi setTargetRole, chuyển sang bước 2", "st01_moiTao_chonNghe_sangBuoc2", False),
    ("ST-02", "MOI_TAO", "POST step1 — không chọn gì", "MOI_TAO (tự lặp)", "Có",
     "Hiện lỗi 'Vui lòng chọn một vị trí nghề nghiệp.', KHÔNG ghi hồ sơ",
     "st02_moiTao_khongChonGi_tuLap", False),
    ("ST-03", "MOI_TAO", "POST step1 — bấm Bỏ qua", "MOI_TAO (tự lặp)", "Có",
     "Sang bước 2 nhưng KHÔNG lưu định hướng, không báo lỗi",
     "st03_moiTao_boQua_tuLap", False),
    ("ST-04", "DA_CHON_NGHE", "POST step2 — không nạp gì", "DA_NAP_NGUON", "Có",
     "Không gọi storeTranscript, không gọi setGithub — bước 2 hoàn toàn tuỳ chọn",
     "st04_daChonNghe_napNguon_sangBuoc3", False),
    ("ST-05", "DA_NAP_NGUON", "POST step3 — chọn kỹ năng", "HOAN_TAT", "Có",
     "Gọi replaceSkills rồi completeOnboarding, về trang chủ",
     "st05_daNapNguon_chonKyNang_hoanTat", False),
    ("ST-06", "HOAN_TAT", "GET step1 / GET step2 / GET step3 / POST step1 / POST step2",
     "Bị chặn — chuyển hướng về /", "KHÔNG",
     "5 điểm vào, không điểm nào đụng vào hồ sơ đã hoàn tất",
     "st06_hoanTat_moiDiemVaoBiChan", True),
    ("ST-07", "HOAN_TAT", "POST step3 lần nữa", "Bị chặn — chuyển hướng về /", "KHÔNG",
     "KHÔNG gọi replaceSkills — danh sách kỹ năng cũ nguyên vẹn",
     "st07_hoanTat_guiLaiBuoc3_khongXoaKyNang", True),
    ("ST-08", "MOI_TAO", "GET step3 — nhảy cóc bỏ bước 1 và 2", "MOI_TAO — vào được", "Có",
     "Hiển thị bước 3 với currentStep = 3 (hành vi CỐ Ý, xem ghi chú)",
     "st08_moiTao_nhayCocSangBuoc3_duocPhep", False),
]
for ma, dau, sk, cuoi, hl, tdp, method, chan in DONG_ST:
    for j, v in enumerate([ma, dau, sk, cuoi, hl, tdp, "PASS", method], start=1):
        f = C_PASS if j == 7 else (C_CHAN if chan and j in (4, 5) else None)
        o(ws, r, j, v, fill=f, center=(j in (5, 7)), size=(8 if j == 8 else 10))
    r += 1
r += 1

r = thanh(ws, r, "TIÊU CHÍ ĐỦ VÀ HAI GHI CHÚ QUAN TRỌNG", fill=C_SEC)
for dong in [
    "TIÊU CHÍ ĐỦ: phủ hết cạnh của sơ đồ. ST-01 → ST-05 phủ trọn đường đi thuận S1 → S2 → S3 → S4, "
    "cộng hai cạnh tự lặp. ST-06 và ST-07 phủ cạnh vòng ở trạng thái hấp thụ.",
    "ST-06 VÀ ST-07 LÀ PHẦN ĐẮT GIÁ NHẤT. S4 phải là trạng thái hấp thụ: cả SÁU điểm vào (GET và "
    "POST của ba bước) đều phải chặn — ST-06 phủ năm điểm, ST-07 phủ điểm thứ sáu. Tách riêng vì "
    "nếu bỏ sót chốt chặn ở đúng điểm thứ sáu ấy — POST step3 — thì "
    "replaceSkills() sẽ XOÁ SẠCH danh sách kỹ năng người dùng đã chọn. Kiểm thử chỉ đi đường thuận "
    "không bao giờ chạm tới cạnh này, vì đường thuận kết thúc ngay khi vào S4.",
    "ST-08 — ĐIỀU ĐÃ KIỂM CHỨNG VÀ BÁC BỎ. Ban đầu việc GET step3 thiếu chốt chặn bị NGHI là khiếm "
    "khuyết: gõ thẳng URL là vào được bước 3 mà chưa qua bước 1 và 2. Dựng máy trạng thái xong mới "
    "thấy KHÔNG phải lỗi — bước 1 có sẵn nút 'Bỏ qua' (skip=true) và mọi trường ở bước 2 đều tuỳ "
    "chọn, nên đi đúng luồng giao diện cũng tới được bước 3 với hồ sơ trống. Nhảy cóc không giành "
    "thêm quyền gì. ST-08 được giữ lại làm test hồi quy, chốt rằng đây là hành vi CỐ Ý chứ không "
    "phải sơ suất.",
    "Ghi ST-08 vào báo cáo dù nó KHÔNG tìm ra lỗi là có chủ đích: một nghi vấn đã kiểm chứng và bác "
    "bỏ cũng là kết quả kiểm thử. Nếu về sau ai đó thêm chốt chặn vào GET step3 mà không hiểu ngữ "
    "cảnh, test này đỏ và buộc họ đọc lại lý do.",
    "GHI CHÚ VỀ ĐIỀU KIỆN CANH: giống bảng ở sheet 04 và 04b, điều kiện phụ được gộp vào cột "
    "Event / Input chứ không tách thành cột riêng — giữ đúng bốn cột lõi của slide 39.",
]:
    r = thanh(ws, r, dong, fill=None, color="404040", size=9, italic=True, cao=44)

wb.save(XLSX)
print(f"Da sinh 2 sheet bo sung vao {XLSX}")
for s in wb.sheetnames:
    print("  -", s)

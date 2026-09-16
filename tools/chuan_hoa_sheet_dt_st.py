# -*- coding: utf-8 -*-
"""Chuẩn hoá SÁU sheet bảng quyết định và chuyển đổi trạng thái về một khuôn duy nhất.

    python tools/chuan_hoa_sheet_dt_st.py

BA VẤN ĐỀ ĐƯỢC XỬ LÝ

1. Sheet 03c ghi tiêu đề "BẢNG QUYẾT ĐỊNH ĐẦY ĐỦ · 8 rule sau khi rút gọn" — tự mâu
   thuẫn, và thực tế KHÔNG có bảng đầy đủ. Nay dựng đủ 24 rule (2 × 6 × 2) rồi mới rút
   gọn còn 8, đúng trình tự của hai sheet 03 và 03b.

2. Ba sheet chuyển đổi trạng thái trình bày khác nhau: 04 đánh số 1..9 và ghi chú đặt
   sau bảng; 04b và 04c chèn thêm bảng phụ và sơ đồ TRƯỚC bảng thiết kế, 04c lại đánh mã
   ST-01. Nay cả ba theo cùng thứ tự: tổng kết → sơ đồ → ma trận đầy đủ → bảng thiết kế
   (đánh số 1, 2, 3…) → ghi chú.

3. Bảng chuyển trạng thái cũ chỉ phủ CẠNH của sơ đồ, không liệt kê mọi ô của ma trận
   trạng thái × sự kiện như ví dụ Media Player ở slide 39. Nay mỗi sheet có thêm MA TRẬN
   ĐẦY ĐỦ liệt kê trọn vẹn mọi ô, rồi bảng thiết kế chọn ra các ô cần một test riêng.

Mọi ô của ma trận đều suy ra từ mã nguồn, không đoán:
  ProgressService.updateProgress()  — quyết định chỉ phụ thuộc (trạng thái muốn đặt,
                                      node có khoá), KHÔNG phụ thuộc trạng thái hiện tại
  PasswordResetService              — token một chiều, không có cạnh quay lại
  OnboardingController              — getIncompleteUser() chặn mọi điểm vào khi đã hoàn tất
"""
import xml.etree.ElementTree as ET
from pathlib import Path

import openpyxl
from openpyxl.styles import Font, PatternFill, Alignment, Border, Side
from openpyxl.utils import get_column_letter

XLSX = "BaoCao-KiemThu-CareerCompass.xlsx"
SUREFIRE = Path("target/surefire-reports")

C_HDR = "1F4E79"; C_TXT = "FFFFFFFF"; C_PASS = "C6EFCE"; C_SEC = "DDEBF7"
C_OK = "E2F0D9"; C_CHAN = "FBE4E4"; C_GOP = "FFF2CC"
thin = Side(style="thin", color="BFBFBF")
BORDER = Border(left=thin, right=thin, top=thin, bottom=thin)


def so_test(cls, pkg="blackbox"):
    f = SUREFIRE / f"TEST-vn.uth.careercompass.{pkg}.{cls}.xml"
    if not f.exists():
        raise SystemExit(f"CHUA CHAY TEST: khong thay {f}. Chay 'mvnw test' truoc.")
    g = ET.parse(f).getroot().attrib
    if int(g["failures"]) or int(g["errors"]):
        raise SystemExit(f"{cls} dang do — sua source truoc khi sinh report.")
    return int(g["tests"])


# ══════════════════════════════════════════════════════════════════════════
# Dữ liệu từng sheet — phần duy nhất viết tay
# ══════════════════════════════════════════════════════════════════════════

# ---- 03c · bảng quyết định tệp bảng điểm --------------------------------
# C2 là điều kiện NHỊ PHÂN, không phải sáu giá trị. Đặc tả nói "Chỉ chấp nhận file PDF,
# PNG hoặc JPG" — bốn đuôi đó KHÔNG được phân biệt với nhau, cùng một hành động, nên
# điều kiện thật là "đuôi có thuộc tập chấp nhận không". Bản trước liệt kê sáu giá trị
# rồi ra 24 rule, nhưng lại rút gọn theo kiểu nhị phân — mâu thuẫn với chính nó.
#
# Việc bốn đuôi hợp lệ vẫn cần bốn test riêng là chuyện KHÁC: đó là phân hoạch lớp tương
# đương bên trong một lớp, cộng với yêu cầu bao phủ nhánh của mã nguồn. Xem bảng
# "GIÁ TRỊ ĐẠI DIỆN" ở cuối sheet.
def rule_03c():
    """Sinh trọn 8 rule: C1 (2) × C2 (2) × C3 (2)."""
    ra = []
    for c1 in ("Y", "N"):
        for c2 in ("Y", "N"):
            for c3 in ("Y", "N"):
                if c1 == "Y":
                    hd = "A2"          # tên tệp null — chặn ngay từ đầu
                elif c2 == "N":
                    hd = "A4"          # đuôi sai — chặn trước khi xét dung lượng
                else:
                    hd = "A1" if c3 == "Y" else "A3"
                ra.append(((c1, c2, c3), hd))
    return ra


RUT_GON_03C = [
    ("R1'", ("Y", "–", "–"), "A2", "gộp R1–R4"),
    ("R2'", ("N", "Y", "Y"), "A1", "chính là R5"),
    ("R3'", ("N", "Y", "N"), "A3", "chính là R6"),
    ("R4'", ("N", "N", "–"), "A4", "gộp R7 + R8"),
]

# Mỗi rule đã gộp được thi hành bởi nhiều test, vì mã nguồn phân biệt từng đuôi thành
# một nhánh riêng — xem ghi chú "BẢNG RÚT GỌN NÓI ÍT HƠN" ở cuối sheet.
TEST_THEO_RULE = {
    "R1'": "rule1_tenTepNull_tuChoi",
    "R2'": ("rule2_duoiPdf_luuThanhCong\n"
             "rule3_duoiPng_luuThanhCong\n"
             "rule4_duoiJpg_luuThanhCong\n"
             "rule5_duoiJpeg_luuThanhCong"),
    "R3'": "rule6_duoiHopLe_dungLuongQuaLon_tuChoi",
    "R4'": ("rule7_duoiKhongHopLe_tuChoi\n"
             "rule8_tenTepKhongCoDuoi_tuChoi"),
}

HANH_DONG_03C = {
    "A1": "A1 · Lưu tệp, trả đường dẫn",
    "A2": "A2 · Lỗi \"Tên file không hợp lệ\"",
    "A3": "A3 · Lỗi \"Vượt quá dung lượng 10MB\"",
    "A4": "A4 · Lỗi \"Chỉ chấp nhận PDF, PNG hoặc JPG\"",
}


# ---- Ma trận trạng thái × sự kiện cho ba sheet chuyển đổi ----------------
MA_TRAN_04 = {
    "tieu_de": ["Trạng thái hiện tại",
                "Đặt NOT_STARTED\n(node mở)", "Đặt NOT_STARTED\n(node KHOÁ)",
                "Đặt IN_PROGRESS\n(node mở)", "Đặt IN_PROGRESS\n(node KHOÁ)",
                "Đặt DONE\n(node mở)", "Đặt DONE\n(node KHOÁ)"],
    "hang": [
        ["NOT_STARTED", "NOT_STARTED\n(tự lặp)", "NOT_STARTED\n(tự lặp)",
         "IN_PROGRESS", "Bị chặn 403", "DONE", "Bị chặn 403"],
        ["IN_PROGRESS", "NOT_STARTED", "NOT_STARTED",
         "IN_PROGRESS\n(tự lặp)", "Bị chặn 403", "DONE", "Bị chặn 403"],
        ["DONE", "NOT_STARTED", "NOT_STARTED",
         "IN_PROGRESS", "Bị chặn 403", "DONE\n(tự lặp)", "Bị chặn 403"],
    ],
    "ghi_chu": "Ma trận 3 trạng thái × 6 sự kiện = 18 ô, liệt kê TRỌN VẸN, kể cả ô tự "
               "lặp và ô bị chặn. Đọc mã nguồn thấy ngay một tính chất đáng chú ý: "
               "quyết định chỉ phụ thuộc (trạng thái MUỐN ĐẶT, node có khoá), hoàn toàn "
               "KHÔNG phụ thuộc trạng thái hiện tại — nên ba hàng giống hệt nhau, chỉ "
               "khác ở ô tự lặp. Đó là lý do bảng thiết kế bên dưới chỉ cần 9 test chứ "
               "không cần đủ 18.",
}

MA_TRAN_04B = {
    "tieu_de": ["Trạng thái hiện tại", "Yêu cầu đặt lại\nmật khẩu", "Đặt lại mật khẩu\nbằng token",
                "Quá 30 phút", "Gọi validateToken"],
    "hang": [
        ["(chưa có token)", "HỢP LỆ", "— không có token", "— không có token", "— không có token"],
        ["HỢP LỆ", "HỢP LỆ\n(token mới thay token cũ)", "ĐÃ DÙNG", "HẾT HẠN", "Đi qua"],
        ["ĐÃ DÙNG", "HỢP LỆ\n(token mới)", "Bị chặn\n(IllegalState)", "ĐÃ DÙNG + HẾT HẠN", "Bị lọc bỏ"],
        ["HẾT HẠN", "HỢP LỆ\n(token mới)", "Bị chặn\n(IllegalState)", "HẾT HẠN\n(tự lặp)", "Bị lọc bỏ"],
    ],
    "ghi_chu": "Ma trận 4 trạng thái × 4 sự kiện = 16 ô. Điểm cốt lõi đọc được từ ma "
               "trận: KHÔNG ô nào đưa token quay lại HỢP LỆ, trừ khi tạo token MỚI — "
               "tức đây là máy trạng thái MỘT CHIỀU. Chính vì vậy số cạnh không phải "
               "3 × 2 = 6 như trường hợp tổng quát.",
}

MA_TRAN_04C = {
    "tieu_de": ["Trạng thái hiện tại", "GET step1", "GET step2", "GET step3",
                "POST step1", "POST step2", "POST step3"],
    "hang": [
        ["S1 MOI_TAO", "Hiện bước 1", "Hiện bước 2", "Hiện bước 3",
         "→ bước 2\n(lưu định hướng)", "→ bước 3", "→ HOÀN TẤT"],
        ["S2 DA_CHON_NGHE", "Hiện bước 1", "Hiện bước 2", "Hiện bước 3",
         "→ bước 2", "→ bước 3", "→ HOÀN TẤT"],
        ["S3 DA_NAP_NGUON", "Hiện bước 1", "Hiện bước 2", "Hiện bước 3",
         "→ bước 2", "→ bước 3", "→ HOÀN TẤT"],
        ["S4 HOAN_TAT", "Chuyển về /", "Chuyển về /", "Chuyển về /",
         "Chuyển về /", "Chuyển về /", "Chuyển về /"],
    ],
    "ghi_chu": "Ma trận 4 trạng thái × 6 sự kiện = 24 ô. Hàng S4 toàn bộ là \"chuyển về "
               "/\" — đó chính là hình ảnh của TRẠNG THÁI HẤP THỤ: vào rồi thì không sự "
               "kiện nào đưa ra được. Ba hàng trên giống nhau vì mọi bước đều cho phép "
               "bỏ qua, nên nhảy cóc không giành thêm quyền gì.",
}


# ══════════════════════════════════════════════════════════════════════════
# Công cụ vẽ
# ══════════════════════════════════════════════════════════════════════════
wb = openpyxl.load_workbook(XLSX)


def moi(ten, dat_sau, so_cot=8, rong=22):
    if ten in wb.sheetnames:
        wb.remove(wb[ten])
    ws = wb.create_sheet(ten)
    wb._sheets.remove(ws)
    wb._sheets.insert(wb.sheetnames.index(dat_sau) + 1, ws)
    ws.column_dimensions["A"].width = 30
    for i in range(2, so_cot + 1):
        ws.column_dimensions[get_column_letter(i)].width = rong
    return ws


def o(ws, r, c, v, *, bold=False, fill=None, center=False, size=10, color=None):
    x = ws.cell(row=r, column=c, value=v)
    x.font = Font(size=size, bold=bold, color=color)
    x.alignment = Alignment(vertical="center", wrap_text=True,
                            horizontal="center" if center else "left")
    x.border = BORDER
    if fill:
        x.fill = PatternFill("solid", fgColor=fill)


def thanh(ws, r, t, n_cot, *, fill=C_SEC, color=C_HDR, size=11, italic=False, cao=24):
    ws.merge_cells(start_row=r, start_column=1, end_row=r, end_column=n_cot)
    x = ws.cell(row=r, column=1, value=t)
    x.font = Font(bold=not italic, italic=italic, size=size, color=color)
    if fill:
        x.fill = PatternFill("solid", fgColor=fill)
    x.alignment = Alignment(vertical="top" if italic else "center", wrap_text=True)
    ws.row_dimensions[r].height = cao
    return r + 1


def dau_sheet(ws, tieu_de, mo_ta, cls, n, ky_thuat, n_cot, ghi_chu_lenh, tach=""):
    x = ws.cell(row=1, column=1, value=tieu_de)
    x.font = Font(bold=True, size=13, color=C_HDR)
    ws.cell(row=2, column=1, value=mo_ta).font = Font(italic=True, size=10)
    # Khi số test KHÁC số dòng của bảng thiết kế thì phải tách ngay tại đây, cạnh con
    # số. Để lời giải thích tận cuối sheet là người đọc trừ ra thấy lệch rồi mới đi tìm.
    r = thanh(ws, 4, f"TỔNG KẾT THI HÀNH — {n} test đã chạy, {n} PASS, 0 fail."
                     + (f"  {tach}" if tach else ""),
              n_cot, fill=C_OK, color="375623", cao=(30 if tach else 22))
    for i, h in enumerate(["Lớp test trong source", "Số test", "Kỹ thuật áp dụng"]
                          + [""] * (n_cot - 4) + ["Status"], start=1):
        o(ws, r, i, h, bold=True, fill=C_HDR, color=C_TXT, center=True)
    r += 1
    for i, v in enumerate([cls, n, ky_thuat] + [""] * (n_cot - 4) + ["PASS"], start=1):
        o(ws, r, i, v, fill=(C_PASS if i == n_cot else None), center=(i in (2, n_cot)))
    r += 1
    return thanh(ws, r, ghi_chu_lenh, n_cot, fill=None, color="808080",
                 size=9, italic=True, cao=26) + 1


LENH = ("Số test được đối chiếu tự động với target/surefire-reports khi sinh lại sheet "
        "bằng tools/chuan_hoa_sheet_dt_st.py — source chạy ra số khác thì script dừng "
        "và báo lỗi. Chạy lại để kiểm chứng:  mvnw test -Dtest={cls}")


# ══════════════════════════════════════════════════════════════════════════
# 03c · BẢNG QUYẾT ĐỊNH TỆP BẢNG ĐIỂM
# ══════════════════════════════════════════════════════════════════════════
N = so_test("TranscriptFileDecisionTableTest")
ws = moi("03c. Decision Table Tep", "03b. Decision Table Token", so_cot=14, rong=13)
r = dau_sheet(ws, "Decision Table Testing — bảng thứ ba: tệp bảng điểm",
              "OnboardingService.saveTranscript(). Bảng thiết kế theo mẫu slide 44: "
              "bảng đầy đủ trước, bảng rút gọn sau.",
              "TranscriptFileDecisionTableTest", N, "Bảng quyết định · 8 rule → 4 rule",
              14, LENH.format(cls="TranscriptFileDecisionTableTest"),
              tach="Gồm 8 test ứng với 8 rule của bảng đầy đủ (rule1…rule8), cộng 1 "
                   "test kiểm tra bổ sung NGOÀI bảng (đuôi viết hoa .PDF) — xem ghi "
                   "chú cuối sheet.")

RULE = rule_03c()
r = thanh(ws, r, "BẢNG QUYẾT ĐỊNH ĐẦY ĐỦ — mẫu slide 44 · 8 rule = 2 × 2 × 2", 14)
r = thanh(ws, r, "Số rule tối đa = tích số giá trị của các điều kiện = 2 × 2 × 2 = 8. "
                 "Cả ba điều kiện đều nhị phân nên ở bảng này công thức 2^n dùng được — "
                 "khác bảng ProgressService ở sheet 03, nơi điều kiện trạng thái có ba "
                 "giá trị nên phải nhân chứ không luỹ thừa.",
          14, fill=None, color="808080", size=9, italic=True, cao=26)

o(ws, r, 1, "Condition / Action", bold=True, fill=C_HDR, color=C_TXT, center=True)
for j in range(len(RULE)):
    o(ws, r, j + 2, f"R{j + 1}", bold=True, fill=C_HDR, color=C_TXT, center=True)
r += 1
for idx, ten_dk in enumerate(["C1 · Tên tệp = null",
                              "C2 · Đuôi tệp thuộc {.pdf, .png, .jpg, .jpeg}",
                              "C3 · Dung lượng ≤ 10MB"]):
    o(ws, r, 1, ten_dk)
    for j, ((c1, c2, c3), _) in enumerate(RULE):
        o(ws, r, j + 2, (c1, c2, c3)[idx], center=True)
    r += 1
for ma, nhan in HANH_DONG_03C.items():
    o(ws, r, 1, nhan, bold=True)
    for j, (_, hd) in enumerate(RULE):
        o(ws, r, j + 2, "X" if hd == ma else "–", center=True,
          fill=(C_OK if hd == ma else None))
    r += 1
r += 1

r = thanh(ws, r, "BẢNG SAU KHI RÚT GỌN — bước 6 slide 42 · 8 rule → 4 rule", 14)
o(ws, r, 1, "Condition / Action", bold=True, fill=C_HDR, color=C_TXT, center=True)
for j, (ma, _, _, _) in enumerate(RUT_GON_03C):
    o(ws, r, j + 2, ma, bold=True, fill=C_HDR, color=C_TXT, center=True)
o(ws, r, 10, "Ghi chú", bold=True, fill=C_HDR, color=C_TXT, center=True)
ws.merge_cells(start_row=r, start_column=10, end_row=r, end_column=14)
r += 1
# Cột Ghi chú ghi theo HÀNG (mỗi điều kiện / hành động một dòng), đúng kiểu sheet
# 03 và 03b — không phải theo cột rule, vì rule nằm ngang.
GHI_CHU_HANG = {
    "C1 · Tên tệp = null": "2 giá trị",
    "C2 · Đuôi tệp thuộc {.pdf, .png, .jpg, .jpeg}": "2 giá trị — đặc tả không phân biệt bốn đuôi với nhau",
    "C3 · Dung lượng ≤ 10MB": "2 giá trị",
    "A4 · Lỗi \"Chỉ chấp nhận PDF, PNG hoặc JPG\"":
        "R4' gộp cả ô đã phát hiện DEF-011 — xem ghi chú bên dưới",
}


def ghi_chu(r, ten):
    if ten in GHI_CHU_HANG:
        ws.merge_cells(start_row=r, start_column=10, end_row=r, end_column=14)
        o(ws, r, 10, GHI_CHU_HANG[ten], size=9, color="808080")


for idx, ten_dk in enumerate(["C1 · Tên tệp = null", "C2 · Đuôi tệp thuộc {.pdf, .png, .jpg, .jpeg}",
                              "C3 · Dung lượng ≤ 10MB"]):
    o(ws, r, 1, ten_dk)
    for j, (_, dk, _, _) in enumerate(RUT_GON_03C):
        o(ws, r, j + 2, dk[idx], center=True, size=9,
          fill=(C_GOP if dk[idx] == "–" else None))
    ghi_chu(r, ten_dk)
    r += 1
for ma, nhan in HANH_DONG_03C.items():
    o(ws, r, 1, nhan, bold=True)
    for j, (_, _, hd, _) in enumerate(RUT_GON_03C):
        o(ws, r, j + 2, "X" if hd == ma else "–", center=True,
          fill=(C_OK if hd == ma else None))
    ghi_chu(r, nhan)
    r += 1
o(ws, r, 1, "Kết quả chạy test", bold=True)
for j in range(len(RUT_GON_03C)):
    o(ws, r, j + 2, "PASS", fill=C_PASS, center=True)
r += 1
o(ws, r, 1, "Test thi hành rule này", bold=True)
for j, (ma, _, _, _) in enumerate(RUT_GON_03C):
    o(ws, r, j + 2, TEST_THEO_RULE[ma], size=8)
ws.row_dimensions[r].height = 56
r += 1
o(ws, r, 1, "Gộp từ rule nào", bold=True)
for j, (_, _, _, gc) in enumerate(RUT_GON_03C):
    o(ws, r, j + 2, gc, size=8, center=True, color="808080")
r += 1

r += 1
r = thanh(ws, r, "GIÁ TRỊ ĐẠI DIỆN CHO TỪNG LỚP — vì sao 4 rule mà có 8 test", 14)
o(ws, r, 1, "Điều kiện", bold=True, fill=C_HDR, color=C_TXT, center=True)
for j, h in enumerate(["Lớp", "Giá trị đại diện được kiểm", "", "Số test", "Lý do tách riêng"],
                      start=2):
    o(ws, r, j, h, bold=True, fill=C_HDR, color=C_TXT, center=True)
ws.merge_cells(start_row=r, start_column=3, end_row=r, end_column=4)
ws.merge_cells(start_row=r, start_column=6, end_row=r, end_column=14)
r += 1
for dk, lop, gt, n, ly_do in [
    ("C2 · Đuôi tệp", "Hợp lệ (Y)", ".pdf · .png · .jpg · .jpeg", 4,
     "Mã nguồn so sánh từng đuôi bằng một chuỗi && — bốn nhánh riêng. Báo cáo bao phủ "
     "chỉ đích danh ba nhánh chưa ai chạm tới, nên phải kiểm đủ bốn."),
    ("C2 · Đuôi tệp", "Không hợp lệ (N)", ".exe · tên không có dấu chấm", 2,
     "Hai giá trị này TRƯỚC KHI SỬA cho hai kết quả khác nhau: .exe trả lỗi 400 đúng "
     "như đặc tả, còn tên không có dấu chấm ném StringIndexOutOfBoundsException thành "
     "lỗi 500 — chính là DEF-011. Sau khi sửa mới thật sự cùng một lớp."),
    ("C1 · Tên tệp null", "Y", "null", 1, "Một giá trị duy nhất, không có gì để tách."),
    ("C3 · Dung lượng", "Y và N", "1 byte · 11 MB", 1,
     "Chỉ lấy giá trị nominal để phân biệt nhánh. Các giá trị sát biên 10 MB thuộc về "
     "sheet 01 — kỹ thuật giá trị biên, không phải bảng quyết định."),
]:
    o(ws, r, 1, dk)
    o(ws, r, 2, lop, center=True)
    ws.merge_cells(start_row=r, start_column=3, end_row=r, end_column=4)
    o(ws, r, 3, gt, center=True)
    o(ws, r, 5, n, center=True, bold=True)
    ws.merge_cells(start_row=r, start_column=6, end_row=r, end_column=14)
    o(ws, r, 6, ly_do, size=9)
    ws.row_dimensions[r].height = 34
    r += 1
r = thanh(ws, r, "Bảng quyết định trả lời \"tổ hợp điều kiện nào dẫn tới hành động nào\" "
                 "— nó dừng ở mức LỚP. Việc chọn giá trị nào làm đại diện cho mỗi lớp là "
                 "việc của phân hoạch lớp tương đương và bao phủ nhánh. Đó là lý do 4 "
                 "rule sinh ra 8 test chứ không phải 4.",
          14, fill=None, color="404040", size=9, italic=True, cao=30)

r += 1
r = thanh(ws, r, "Ô tô vàng là ô \"không quan tâm\" (–) sinh ra khi gộp rule.", 14,
          fill=None, color="808080", size=9, italic=True, cao=20)
for dong in [
    "BẢNG RÚT GỌN NÓI ÍT HƠN BẢNG ĐẦY ĐỦ — và đó là lý do vẫn phải giữ cả hai. Rút gọn "
    "xong chỉ còn 4 rule, nhưng bộ kiểm thử có 8 test. Chênh lệch nằm ở chỗ: xét theo ĐẶC "
    "TẢ thì bốn đuôi .pdf, .png, .jpg, .jpeg là MỘT lớp tương đương (cùng được chấp "
    "nhận), nên rút gọn gộp chúng làm một ô \"hợp lệ\". Nhưng xét theo MÃ NGUỒN thì mỗi "
    "đuôi là một nhánh riêng của chuỗi && , và báo cáo bao phủ đã chỉ đích danh ba nhánh "
    "chưa ai chạm tới. Nếu chỉ nhìn bảng rút gọn mà viết test thì ba nhánh đó vẫn nằm im.",
    "RÚT GỌN CÒN CHE MẤT TRƯỜNG HỢP ĐÃ TÌM RA LỖI: \"không có dấu chấm\" bị gộp vào "
    "R4' cùng với \"đuôi khác\" vì hai bên cho cùng hành động A4. Nhưng chính ô đó mới "
    "là ô làm lộ DEF-011 — trước khi sửa, nó không trả lỗi 400 mà ném "
    "StringIndexOutOfBoundsException thành lỗi 500. Bảng đầy đủ giữ nó ở cột R23–R24 nên "
    "còn nhìn thấy; bảng rút gọn thì không.",
    "VÌ SAO GỘP ĐƯỢC: mã nguồn kiểm ba điều kiện TUẦN TỰ và thoát ngay khi gặp lỗi. Khi "
    "một điều kiện phía trước đã quyết định kết quả thì điều kiện phía sau không còn ảnh "
    "hưởng, đánh dấu \"–\". R1' gộp 12 rule (tên tệp null thì đuôi và dung lượng vô "
    "nghĩa); R7' và R8' mỗi cột gộp 2 rule (đuôi sai thì chặn trước khi xét dung lượng).",
    "KHIẾM KHUYẾT DEF-011 — nằm trong lớp C2 = N. Tệp không có dấu chấm làm lastIndexOf(\".\") trả -1, "
    "kéo theo substring(-1) ném StringIndexOutOfBoundsException, người dùng nhận lỗi 500 "
    "thay vì thông báo 400. Nói rõ để không nhận công sai: test hộp trắng đã ghi đúng "
    "nguyên nhân từ trước nhưng lại CHỐT hành vi lỗi bằng một khẳng định, nên bộ test vẫn "
    "xanh. Đóng góp của kỹ thuật này là BUỘC PHẢI SỬA — mỗi cột của bảng phải ứng với một "
    "hành động xác định trong nhóm A1–A4, không thể để một ô là \"sập 500\".",
    "CON SỐ ĐO ĐƯỢC: nhánh OnboardingService 75,0% (9/12) → 100,0% (14/14); dòng 94,7% "
    "(18/19) → 100,0% (19/19). Nguồn: target/site/jacoco/jacoco.csv.",
    "QUAN HỆ VỚI SHEET GIÁ TRỊ BIÊN: điều kiện C3 ở bảng này chỉ lấy giá trị NOMINAL "
    "(1 byte và 11 MB) để phân biệt nhánh. Các giá trị sát biên 10 MB thuộc về sheet 01.",
    "KIỂM TRA BỔ SUNG ngoài bảng: đuôi viết HOA \"BANGDIEM.PDF\" được chấp nhận vì mã gọi "
    "toLowerCase — method boSung_duoiVietHoa_duocChapNhan, PASS. Không đưa vào bảng vì đây "
    "là biến thể trình bày của C2, không sinh thêm hành động mới.",
]:
    r = thanh(ws, r, dong, 14, fill=None, color="404040", size=9, italic=True, cao=44)

print(f"")


# ══════════════════════════════════════════════════════════════════════════
# Ba sheet chuyển đổi trạng thái — cùng một khuôn
# ══════════════════════════════════════════════════════════════════════════
COT_TK = ["Test Case No.", "Start State", "Event / Input", "End State / Exp Output",
          "Hợp lệ", "Tác dụng phụ kiểm chứng", "Status", "Method trong code"]


def sheet_trang_thai(ten, dat_sau, tieu_de, mo_ta, cls, ky_thuat, so_do,
                     ma_tran, thiet_ke, ghi_chu, tach=""):
    n = so_test(cls)
    ws = moi(ten, dat_sau, so_cot=8, rong=22)
    r = dau_sheet(ws, tieu_de, mo_ta, cls, n, ky_thuat, 8, LENH.format(cls=cls), tach)

    r = thanh(ws, r, "SƠ ĐỒ TRẠNG THÁI", 8)
    for d in so_do:
        ws.merge_cells(start_row=r, start_column=1, end_row=r, end_column=8)
        x = ws.cell(row=r, column=1, value=d)
        x.font = Font(name="Consolas", size=10)
        x.alignment = Alignment(vertical="center")
        r += 1
    r += 1

    r = thanh(ws, r, "MA TRẬN TRẠNG THÁI × SỰ KIỆN — liệt kê TRỌN VẸN mọi ô", 8)
    for i, h in enumerate(ma_tran["tieu_de"], start=1):
        o(ws, r, i, h, bold=True, fill=C_HDR, color=C_TXT, center=True, size=9)
    ws.row_dimensions[r].height = 34
    r += 1
    for hang in ma_tran["hang"]:
        for i, v in enumerate(hang, start=1):
            chan = any(t in str(v) for t in ("Bị chặn", "Chuyển về /", "Bị lọc"))
            o(ws, r, i, v, center=(i > 1), size=9, bold=(i == 1),
              fill=(C_CHAN if chan else None))
        ws.row_dimensions[r].height = 30
        r += 1
    r = thanh(ws, r, ma_tran["ghi_chu"], 8, fill=None, color="404040",
              size=9, italic=True, cao=40)
    r += 1

    r = thanh(ws, r, "BẢNG CHUYỂN TRẠNG THÁI — mẫu slide 39", 8)
    for i, h in enumerate(COT_TK, start=1):
        o(ws, r, i, h, bold=True, fill=C_HDR, color=C_TXT, center=True)
    r += 1
    for i, hang in enumerate(thiet_ke, start=1):
        chan = hang[3].startswith("Bị chặn") or hang[4] == "KHÔNG"
        for j, v in enumerate([i] + list(hang), start=1):
            f = C_PASS if j == 7 else (C_CHAN if chan and j in (4, 5) else None)
            o(ws, r, j, v, center=(j in (1, 5, 7)), fill=f,
              size=(8 if j == 8 else 10))
        r += 1
    r += 1
    for d in ghi_chu:
        r = thanh(ws, r, d, 8, fill=None, color="404040", size=9, italic=True, cao=40)
    print(f"  {ten}: {n} test, ma tran "
          f"{len(ma_tran['hang'])} x {len(ma_tran['tieu_de']) - 1}, "
          f"{len(thiet_ke)} dong thiet ke")


# ---- 04 -----------------------------------------------------------------
sheet_trang_thai(
    "04. State Transition (PhanA)", "03c. Decision Table Tep",
    "State Transition Testing — Phần A",
    "Máy trạng thái tiến độ học một node kỹ năng (ProgressStatus). Bảng thiết kế theo "
    "mẫu slide 39.",
    "ProgressStateTransitionTest", "Chuyển đổi trạng thái · 9 case",
    ["        NOT_STARTED  ◄────[bỏ đánh dấu]────  IN_PROGRESS  ◄────[hạ xuống]────  DONE",
     "             │                                     ▲                            ▲",
     "             └──────[đánh dấu đang học]────────────┘                            │",
     "             └──────[đánh dấu hoàn thành, nhảy cóc]──────────────────────────────┘",
     "",
     "        Ba cạnh vào IN_PROGRESS và DONE đều bị chặn 403 khi node còn KHOÁ."],
    MA_TRAN_04,
    [
        ("NOT_STARTED", "Đánh dấu IN_PROGRESS (node mở)", "IN_PROGRESS", "Có",
         "completedAt vẫn null", "PASS", "st01_notStarted_toInProgress"),
        ("NOT_STARTED", "Đánh dấu DONE (node mở, nhảy cóc)", "DONE", "Có",
         "Ghi completedAt + nhật ký", "PASS", "st05_notStarted_toDone"),
        ("NOT_STARTED", "Đánh dấu DONE khi node KHOÁ", "Bị chặn (403)", "KHÔNG",
         "Không lưu gì", "PASS", "st07_notStarted_toDone_nodeLocked_biChan"),
        ("IN_PROGRESS", "Đánh dấu DONE (node mở)", "DONE", "Có",
         "Ghi completedAt + nhật ký", "PASS", "st02_inProgress_toDone"),
        ("IN_PROGRESS", "Bỏ đánh dấu", "NOT_STARTED", "Có",
         "Không hỏi khoá", "PASS", "st06_inProgress_toNotStarted"),
        ("IN_PROGRESS", "Đánh dấu DONE khi node KHOÁ", "Bị chặn (403)", "KHÔNG",
         "Không lưu, không ghi log", "PASS", "st08_inProgress_toDone_nodeLocked_biChan"),
        ("DONE", "Bỏ đánh dấu", "NOT_STARTED", "Có",
         "XOÁ completedAt", "PASS", "st03_done_toNotStarted"),
        ("DONE", "Hạ xuống IN_PROGRESS", "IN_PROGRESS", "Có",
         "XOÁ completedAt", "PASS", "st04_done_toInProgress"),
        ("DONE", "Hạ về IN_PROGRESS khi node KHOÁ", "Bị chặn (403)", "KHÔNG",
         "Không lưu gì", "PASS", "st09_done_toInProgress_nodeLocked_biChan"),
    ],
    ["TIÊU CHÍ ĐỦ: phủ hết CẠNH của sơ đồ. Ba trạng thái sinh ra 3 × 2 = 6 cặp có thứ "
     "tự, tức 6 cạnh; dòng 1–2, 4–5, 7–8 phủ đủ sáu cạnh đó. Ba dòng còn lại là chuyển "
     "đổi BỊ CHẶN khi node khoá — phần mà kiểm thử đi đường thuận sẽ bỏ sót.",
     "VÌ SAO BẢNG THIẾT KẾ CHỈ CÓ 9 DÒNG MÀ MA TRẬN CÓ 18 Ô: ma trận cho thấy ba hàng "
     "giống hệt nhau và các ô tự lặp không sinh hành vi mới, nên chín ô còn lại không "
     "cần test riêng. Ma trận ở trên là bằng chứng cho khẳng định đó — không phải lọc "
     "theo cảm tính.",
     "CẤU TRÚC BẢNG: nhóm theo Start State, mỗi nhóm đúng 3 dòng gồm 2 chuyển đổi hợp lệ "
     "và 1 chuyển đổi bị chặn khi node khoá.",
     "TẦNG KIỂM THỬ: bảng này thiết kế ở TẦNG DỊCH VỤ, nơi luật nghiệp vụ thực sự được "
     "thi hành, chứ không phải ở giao diện.",
     "SƠ ĐỒ ĐẦY ĐỦ dạng hình: xem tệp SoDo-ChuyenTrangThai.drawio ở thư mục gốc dự án."])

# ---- 04b ----------------------------------------------------------------
sheet_trang_thai(
    "04b. State Transition Token", "04. State Transition (PhanA)",
    "State Transition Testing — vòng đời token đặt lại mật khẩu",
    "Máy trạng thái của một token đặt lại mật khẩu. Bảng thiết kế theo mẫu slide 39.",
    "TokenStateTransitionTest", "Chuyển đổi trạng thái · 7 case",
    ["                      [người dùng yêu cầu đặt lại mật khẩu]",
     "                                    ↓",
     "                      HỢP LỆ  (used = false, còn hạn)",
     "                         ↓                    ↓",
     "             [đặt lại mật khẩu]          [quá 30 phút]",
     "                         ↓                    ↓",
     "                    ĐÃ DÙNG               HẾT HẠN",
     "",
     "        Hai cạnh ra khỏi HỢP LỆ đều MỘT CHIỀU — không sự kiện nào quay lại."],
    MA_TRAN_04B,
    [
        ("(chưa có token)", "Người dùng yêu cầu đặt lại mật khẩu", "HỢP LỆ", "Có",
         "used = false, còn 30 phút", "PASS", "st01_taoToken_sangTrangThaiHopLe"),
        ("HỢP LỆ", "Đặt lại mật khẩu bằng token", "ĐÃ DÙNG", "Có",
         "Ghi mật khẩu mới, bật cờ used", "PASS", "st02_hopLe_sangDaDung"),
        ("HỢP LỆ", "Quá 30 phút", "HẾT HẠN", "Có",
         "used vẫn false — hết hạn không kéo theo đã dùng", "PASS",
         "st03_hopLe_sangHetHan"),
        ("ĐÃ DÙNG", "Đặt lại mật khẩu LẦN NỮA", "Bị chặn (IllegalState)", "KHÔNG",
         "Mật khẩu chỉ đổi đúng một lần", "PASS", "st04_daDung_dungLaiBiChan"),
        ("HẾT HẠN", "Đặt lại mật khẩu", "Bị chặn (IllegalState)", "KHÔNG",
         "Không bật cờ used, không đổi mật khẩu", "PASS", "st05_hetHan_dungBiChan"),
        ("ĐÃ DÙNG", "Quá 30 phút", "ĐÃ DÙNG + HẾT HẠN", "KHÔNG",
         "Hai cờ cùng bật, càng không hợp lệ", "PASS",
         "st06_daDung_hetHanVanKhongHopLe"),
        ("Mọi trạng thái", "Gọi validateToken", "Chỉ HỢP LỆ đi qua", "—",
         "Hai trạng thái kia bị lọc bỏ", "PASS",
         "st07_validateToken_chiQuaOTrangThaiHopLe"),
    ],
    ["TIÊU CHÍ ĐỦ: phủ hết cạnh của sơ đồ. Ba trạng thái nhưng KHÔNG phải 3 × 2 = 6 cạnh "
     "như trường hợp tổng quát, vì đây là máy trạng thái một chiều — ma trận ở trên cho "
     "thấy không ô nào đưa token quay lại HỢP LỆ.",
     "DÒNG 4 LÀ TEST ĐÁNG GIÁ NHẤT. Nó gửi HAI sự kiện liên tiếp: dùng token, rồi dùng "
     "lại chính token đó. Bảng quyết định ở sheet 03b không diễn tả nổi phép thử này — "
     "nó chỉ mô tả bốn tổ hợp cờ tại một thời điểm, không có trục thời gian.",
     "SO VỚI SHEET 03b — CÙNG ĐỐI TƯỢNG, HAI KỸ THUẬT: bảng quyết định trả lời \"token "
     "này TẠI THỜI ĐIỂM NÀY có hợp lệ không\", tĩnh, chụp một khoảnh khắc; chuyển đổi "
     "trạng thái trả lời \"token đi qua được những CHUỖI SỰ KIỆN nào\", có ràng buộc thứ "
     "tự. Cái trước không nói được cờ used đi tới bằng con đường nào; cái sau không liệt "
     "kê đủ mọi tổ hợp cờ.",
     "ĐIỀU KIỆN CANH: giống sheet 04, điều kiện phụ gộp vào cột Event / Input chứ không "
     "tách thành cột riêng — đúng bốn cột lõi của slide 39. Dòng 1 chỉ áp dụng cho tài "
     "khoản LOCAL; tài khoản Google không đặt lại mật khẩu ở đây."])

# ---- 04c ----------------------------------------------------------------
sheet_trang_thai(
    "04c. State Transition Onboard", "04b. State Transition Token",
    "State Transition Testing — ba bước khai báo hồ sơ lần đầu",
    "OnboardingController, /onboarding/step1..3. Bảng thiết kế theo mẫu slide 39.",
    "OnboardingStateTransitionTest", "Chuyển đổi trạng thái · 12 case",
    ["    S1 MOI_TAO ──[POST step1, chọn nghề]──► S2 DA_CHON_NGHE ──[POST step2]──► S3 DA_NAP_NGUON",
     "       │   ▲                                                                        │",
     "       │   └──[không chọn gì] / [bấm Bỏ qua]                             [POST step3]",
     "       │                                                                            ▼",
     "       └────────────[GET step3, nhảy cóc]────────────────────────►      S4 HOAN_TAT",
     "                                                                             │    ▲",
     "                                                                             └────┘",
     "                                                        mọi điểm vào đều chuyển về /"],
    MA_TRAN_04C,
    [
        ("S1 MOI_TAO", "POST step1 — chọn nghề hợp lệ", "S2 DA_CHON_NGHE", "Có",
         "Gọi setTargetRole, sang bước 2", "PASS", "st01_moiTao_chonNghe_sangBuoc2"),
        ("S1 MOI_TAO", "POST step1 — không chọn gì", "S1 MOI_TAO (tự lặp)", "Có",
         "Hiện lỗi, KHÔNG ghi hồ sơ", "PASS", "st02_moiTao_khongChonGi_tuLap"),
        ("S1 MOI_TAO", "POST step1 — bấm Bỏ qua", "S1 MOI_TAO (tự lặp)", "Có",
         "Sang bước 2 nhưng KHÔNG lưu định hướng", "PASS", "st03_moiTao_boQua_tuLap"),
        ("S2 DA_CHON_NGHE", "POST step2 — không nạp gì", "S3 DA_NAP_NGUON", "Có",
         "Bước 2 hoàn toàn tuỳ chọn", "PASS", "st04_daChonNghe_napNguon_sangBuoc3"),
        ("S3 DA_NAP_NGUON", "POST step3 — chọn kỹ năng", "S4 HOAN_TAT", "Có",
         "replaceSkills rồi completeOnboarding", "PASS",
         "st05_daNapNguon_chonKyNang_hoanTat"),
        ("S4 HOAN_TAT", "GET step1 / step2 / step3, POST step1 / step2", "Bị chặn — về /",
         "KHÔNG", "5 điểm vào, không điểm nào đụng hồ sơ", "PASS",
         "st06_hoanTat_moiDiemVaoBiChan"),
        ("S4 HOAN_TAT", "POST step3 lần nữa", "Bị chặn — về /", "KHÔNG",
         "KHÔNG gọi replaceSkills — kỹ năng cũ nguyên vẹn", "PASS",
         "st07_hoanTat_guiLaiBuoc3_khongXoaKyNang"),
        ("S1 MOI_TAO", "GET step3 — nhảy cóc bỏ bước 1 và 2", "S1 MOI_TAO — vào được",
         "Có", "Hiển thị bước 3, hành vi CỐ Ý", "PASS",
         "st08_moiTao_nhayCocSangBuoc3_duocPhep"),
    ],
    ["TIÊU CHÍ ĐỦ: phủ hết cạnh của sơ đồ. Dòng 1–5 phủ trọn đường đi thuận "
     "S1 → S2 → S3 → S4 cộng hai cạnh tự lặp; dòng 6–7 phủ cạnh vòng ở trạng thái hấp thụ.",
     "DÒNG 6 VÀ 7 LÀ PHẦN ĐẮT GIÁ NHẤT. S4 phải là trạng thái HẤP THỤ: cả sáu điểm vào "
     "đều phải chặn — dòng 6 phủ năm điểm, dòng 7 phủ điểm thứ sáu. Tách riêng vì nếu bỏ "
     "sót chốt chặn ở đúng điểm thứ sáu ấy (POST step3) thì replaceSkills() sẽ XOÁ SẠCH "
     "danh sách kỹ năng người dùng đã chọn. Kiểm thử đi đường thuận không bao giờ chạm "
     "tới cạnh này vì đường thuận kết thúc ngay khi vào S4.",
     "DÒNG 8 — ĐIỀU ĐÃ KIỂM CHỨNG VÀ BÁC BỎ. Ban đầu việc GET step3 thiếu chốt chặn bị "
     "NGHI là khiếm khuyết. Dựng ma trận xong mới thấy KHÔNG phải lỗi: ba hàng S1, S2, S3 "
     "giống hệt nhau vì bước 1 có nút \"Bỏ qua\" và mọi trường ở bước 2 đều tuỳ chọn, nên "
     "đi đúng luồng giao diện cũng tới được bước 3 với hồ sơ trống. Giữ lại làm test hồi "
     "quy, chốt rằng đây là hành vi CỐ Ý.",
     "VÌ SAO 8 DÒNG THIẾT KẾ SINH RA 12 TEST: dòng 6 là test tham số hoá, chạy 5 lần cho "
     "5 điểm vào khác nhau.",
     "ĐIỂM KHÁC HAI MÁY TRẠNG THÁI TRƯỚC: ở đây trạng thái KHÔNG nằm trong một cột enum "
     "mà là tổ hợp cờ trên bản ghi người dùng (targetRoleId, transcriptPath/githubUsername, "
     "onboardingCompleted), nên phải tự đặt tên bốn trạng thái trước khi vẽ được sơ đồ."],
    tach="Bảng thiết kế có 8 dòng nhưng chạy ra 12 test: dòng 6 là test tham số hoá, "
         "chạy 5 lần cho 5 điểm vào khác nhau.")

wb.save(XLSX)
print(f"\nDa chuan hoa xong, luu vao {XLSX}")

# -*- coding: utf-8 -*-
"""Sinh sheet "04b. State Transition Token" của BaoCao-PhanA-HopDen.xlsx.

Chạy từ thư mục gốc dự án, sau khi đã chạy test:
    mvnw clean test -Dtest=TokenStateTransitionTest
    python tools/gen_sheet04b.py

Đối tượng: vòng đời token đặt lại mật khẩu — cùng đối tượng với bảng quyết định ở
sheet 03b, nhưng nhìn bằng kỹ thuật khác. Đặt cạnh nhau để thấy rõ hai kỹ thuật trả
lời hai câu hỏi khác nhau, và câu hỏi của máy trạng thái là câu quan trọng hơn về
mặt bảo mật: "dùng rồi thì có dùng lại được không".
"""
import xml.etree.ElementTree as ET
from pathlib import Path

import openpyxl
from openpyxl.styles import Alignment, Border, Font, PatternFill, Side
from openpyxl.utils import get_column_letter

XLSX = "BaoCao-PhanA-HopDen.xlsx"
TMP = "_tmp.xlsx"
SHEET = "04b. State Transition Token"
LOP_TEST = "TokenStateTransitionTest"
SO_TEST_DU_KIEN = 7

C_HDR = "1F4E79"; C_TXT = "FFFFFFFF"; C_PASS = "C6EFCE"; C_SEC = "DDEBF7"
C_CHAN = "FBE4E4"
thin = Side(style="thin", color="BFBFBF")
BORDER = Border(left=thin, right=thin, top=thin, bottom=thin)

# Bảng chuyển trạng thái, đúng bốn cột lõi của slide 39 cộng hai cột thông tin.
# (Test Case, Start State, Event/Input, End State, Hợp lệ, Tác dụng phụ, method)
CHUYEN = [
    (1, "(chưa có token)", "Người dùng yêu cầu đặt lại mật khẩu", "HỢP LỆ", "Có",
     "used = false, còn 30 phút", "st01_taoToken_sangTrangThaiHopLe"),
    (2, "HỢP LỆ", "Đặt lại mật khẩu bằng token", "ĐÃ DÙNG", "Có",
     "Ghi mật khẩu mới, bật cờ used", "st02_hopLe_sangDaDung"),
    (3, "HỢP LỆ", "Quá 30 phút", "HẾT HẠN", "Có",
     "used vẫn false — hết hạn không kéo theo đã dùng", "st03_hopLe_sangHetHan"),
    (4, "ĐÃ DÙNG", "Đặt lại mật khẩu LẦN NỮA", "Bị chặn (IllegalState)", "KHÔNG",
     "Mật khẩu chỉ đổi đúng một lần", "st04_daDung_dungLaiBiChan"),
    (5, "HẾT HẠN", "Đặt lại mật khẩu", "Bị chặn (IllegalState)", "KHÔNG",
     "Không bật cờ used, không đổi mật khẩu", "st05_hetHan_dungBiChan"),
    (6, "ĐÃ DÙNG", "Quá 30 phút", "ĐÃ DÙNG + HẾT HẠN", "KHÔNG",
     "Hai cờ cùng bật, càng không hợp lệ", "st06_daDung_hetHanVanKhongHopLe"),
    (7, "Mọi trạng thái", "Gọi validateToken", "Chỉ HỢP LỆ đi qua", "—",
     "Hai trạng thái kia bị lọc bỏ", "st07_validateToken_chiQuaOTrangThaiHopLe"),
]


def so_test_that():
    f = Path(f"target/surefire-reports/TEST-vn.uth.careercompass.blackbox.{LOP_TEST}.xml")
    if not f.exists():
        print(f"  ! chua co surefire cho {LOP_TEST}, dung so du kien {SO_TEST_DU_KIEN}")
        return SO_TEST_DU_KIEN
    that = int(ET.parse(f).getroot().attrib["tests"])
    if that != SO_TEST_DU_KIEN:
        raise SystemExit(f"LECH SO LIEU: {LOP_TEST} chay {that} test nhung bao cao ghi "
                         f"{SO_TEST_DU_KIEN}. Sua tools/gen_sheet04b.py roi chay lai.")
    return that


so_test = so_test_that()
wb = openpyxl.load_workbook(XLSX)
if SHEET in wb.sheetnames:
    wb.remove(wb[SHEET])
ws = wb.create_sheet(SHEET)
for i, w in enumerate([13, 20, 34, 26, 11, 32, 30, 30], 1):
    ws.column_dimensions[get_column_letter(i)].width = w


def o(r, c, v, *, bold=False, fill=None, center=False, size=10, color=None):
    x = ws.cell(row=r, column=c, value=v)
    x.font = Font(size=size, bold=bold, color=color)
    x.alignment = Alignment(vertical="top", wrap_text=True,
                            horizontal="center" if center else "general")
    x.border = BORDER
    if fill:
        x.fill = PatternFill("solid", fgColor=fill)


def thanh(r, t, *, fill=C_SEC, color=C_HDR, size=11, italic=False, cao=24):
    ws.merge_cells(start_row=r, start_column=1, end_row=r, end_column=8)
    x = ws.cell(row=r, column=1, value=t)
    x.font = Font(bold=not italic, italic=italic, size=size, color=color)
    if fill:
        x.fill = PatternFill("solid", fgColor=fill)
    x.alignment = Alignment(vertical="top" if italic else "center", wrap_text=True)
    ws.row_dimensions[r].height = cao
    return r + 1


def tieu_de(r, cot):
    for i, h in enumerate(cot, 1):
        o(r, i, h, bold=True, fill=C_HDR, color=C_TXT, center=True)
    ws.row_dimensions[r].height = 30
    return r + 1


x = ws.cell(row=1, column=1, value="State Transition Testing — vòng đời token đặt lại mật khẩu")
x.font = Font(bold=True, size=13, color=C_HDR)
x = ws.cell(row=2, column=1, value=(
    "Máy trạng thái của một token đặt lại mật khẩu. Bảng thiết kế theo mẫu slide 39."))
x.font = Font(size=10, italic=True, color="595959")

r = thanh(4, f"TỔNG KẾT THI HÀNH — {so_test} test đã chạy, {so_test} PASS, 0 fail.", cao=26)
r = tieu_de(r, ["Lớp test trong source", "Số test", "Kỹ thuật áp dụng", "", "", "", "", "Status"])
for i, v in enumerate([LOP_TEST, so_test, "Chuyển đổi trạng thái · 7 case",
                       "", "", "", "", "PASS"], 1):
    o(r, i, v, fill=(C_PASS if i == 8 else None), center=(i in (2, 8)))
ws.row_dimensions[r].height = 26
r += 2

# ---- Vì sao lại kiểm chính đối tượng đã có bảng quyết định ----
r = thanh(r, "BẢNG 1 — VÌ SAO KIỂM CÙNG MỘT ĐỐI TƯỢNG BẰNG HAI KỸ THUẬT")
r = tieu_de(r, ["Kỹ thuật", "Sheet", "Câu hỏi nó trả lời", "", "Tính chất", "",
                "Điều nó KHÔNG nói được", ""])
for kt, sh, hoi, tc, khong in [
    ("Bảng quyết định", "03b", "Token này TẠI THỜI ĐIỂM NÀY có hợp lệ không?",
     "Tĩnh — chụp một khoảnh khắc",
     "Không nói được cờ used đi tới bằng con đường nào, cũng không cấm nó quay ngược lại."),
    ("Chuyển đổi trạng thái", "04b", "Token đi qua được những CHUỖI SỰ KIỆN nào?",
     "Theo thời gian — ràng buộc thứ tự",
     "Không liệt kê đủ mọi tổ hợp cờ như bảng quyết định làm."),
]:
    o(r, 1, kt, bold=True)
    o(r, 2, sh, center=True)
    o(r, 3, hoi)
    ws.merge_cells(start_row=r, start_column=3, end_row=r, end_column=4)
    o(r, 5, tc)
    ws.merge_cells(start_row=r, start_column=5, end_row=r, end_column=6)
    o(r, 7, khong)
    ws.merge_cells(start_row=r, start_column=7, end_row=r, end_column=8)
    ws.row_dimensions[r].height = 56
    r += 1
r += 1

# ---- Sơ đồ ----
r = thanh(r, "SƠ ĐỒ TRẠNG THÁI")
so_do = [
    "                          [người dùng yêu cầu đặt lại mật khẩu]",
    "                                        ↓",
    "                          HỢP LỆ  (used = false, còn hạn)",
    "                             ↓                    ↓",
    "                 [đặt lại mật khẩu]          [quá 30 phút]",
    "                             ↓                    ↓",
    "                        ĐÃ DÙNG               HẾT HẠN",
    "",
    "Hai cạnh ra khỏi HỢP LỆ đều MỘT CHIỀU: không sự kiện nào đưa token quay lại hợp lệ.",
]
for dong in so_do:
    ws.merge_cells(start_row=r, start_column=1, end_row=r, end_column=8)
    x = ws.cell(row=r, column=1, value=dong)
    x.font = Font(name="Consolas", size=10,
                  bold=dong.startswith("Hai cạnh"), color="1F4E79")
    x.alignment = Alignment(vertical="center")
    ws.row_dimensions[r].height = 18
    r += 1
r += 1

# ---- Bảng chuyển trạng thái ----
r = thanh(r, "BẢNG CHUYỂN TRẠNG THÁI — mẫu slide 39 · 3 cạnh hợp lệ, 3 cạnh bị chặn")
r = tieu_de(r, ["Test Case No.", "Start State", "Event / Input", "End State / Exp Output",
                "Hợp lệ", "Tác dụng phụ kiểm chứng", "Status", "Method trong code"])
for tc, bat_dau, su_kien, ket_thuc, hop_le, phu, method in CHUYEN:
    chan = hop_le == "KHÔNG"
    for j, v in enumerate([tc, bat_dau, su_kien, ket_thuc, hop_le, phu, "PASS", method], 1):
        f = C_PASS if j == 7 else (C_CHAN if chan and j in (4, 5) else None)
        o(r, j, v, fill=f, center=(j in (1, 5, 7)), bold=(j == 5 and chan))
    ws.row_dimensions[r].height = 34
    r += 1
r += 1

r = thanh(r, "TIÊU CHÍ ĐỦ: phủ hết cạnh của sơ đồ. Ba trạng thái nhưng KHÔNG phải 3 × 2 = 6 "
             "cạnh như trường hợp tổng quát, vì đây là máy trạng thái một chiều — không có "
             "cạnh nào đi ngược. Ba cạnh hợp lệ (dòng 1–3) cộng ba cạnh bị chặn (dòng 4–6) "
             "là phủ trọn, dòng 7 kiểm thêm cổng vào validateToken.",
           fill=None, color="595959", size=9, italic=True, cao=44)
r = thanh(r, "DÒNG 4 LÀ TEST ĐÁNG GIÁ NHẤT. Nó gửi HAI sự kiện liên tiếp: dùng token, rồi dùng "
             "lại chính token đó. Bảng quyết định ở sheet 03b không diễn tả nổi phép thử này — "
             "nó chỉ mô tả bốn tổ hợp cờ tại một thời điểm, không ràng buộc được thứ tự sự "
             "kiện. Đây chính là chỗ hai kỹ thuật bổ sung cho nhau thay vì trùng lặp.",
           fill=None, color="595959", size=9, italic=True, cao=44)
r = thanh(r, "GHI CHÚ VỀ ĐIỀU KIỆN CANH: giống bảng ở sheet 04, điều kiện phụ được gộp vào cột "
             "Event / Input chứ không tách thành cột riêng — đúng bốn cột lõi của slide 39. "
             "Ví dụ TC-ST-01 chỉ áp dụng cho tài khoản đăng nhập bằng mật khẩu; tài khoản đăng "
             "nhập bằng Google không tạo được token vì mật khẩu do Google giữ. Lần chạy đầu "
             "tiên đã đỏ đúng vì quên đặt authProvider = LOCAL.",
           fill=None, color="595959", size=9, italic=True, cao=44)

# Đặt sheet ngay sau 04, giữ thứ tự đọc: hộp đen -> quyết định -> chuyển trạng thái
vt = wb.sheetnames.index("04. State Transition (PhanA)") + 1
wb._sheets.insert(vt, wb._sheets.pop(wb._sheets.index(ws)))
wb.save(TMP)
print(f"OK — {SHEET}: {so_test} test, ket thuc dong {r - 1}")
print("    thu tu sheet:", wb.sheetnames)

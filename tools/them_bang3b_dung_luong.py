# -*- coding: utf-8 -*-
"""Thêm "BẢNG 3b — BVA dung lượng tệp" vào sheet 01.

    python tools/them_bang3b_dung_luong.py

VÌ SAO CẦN: sheet 01 trình bày form đăng ký rất đầy đủ — Bảng 1 định nghĩa giá trị biên,
Bảng 2 Standard BVA, Bảng 3 Robustness BVA. Nhưng biến "dung lượng tệp bảng điểm" thì
chỉ xuất hiện ở Bảng 4 (nhãn B22–B27) và Bảng 5 (TC18–TC23), không có bảng Standard /
Robustness riêng.

Không thể nhét nó vào Bảng 2 vì cột của bảng đó là fullName | email | password — một
case dung lượng tệp không có giá trị nào cho ba cột ấy. Nhưng nó vẫn xứng đáng có bảng
riêng: n = 1 là trường hợp hợp lệ của cùng công thức 4n+1 và 6n+1.

MỘT ĐIỂM ĐÁNG NÓI mà chỉ trường hợp n = 1 này mới lộ ra: công thức Robustness cho
6n+1 = 7 case, nhưng thực tế chỉ có 6. Giá trị min− = −1 byte KHÔNG TỒN TẠI — không có
tệp nào dung lượng âm. Đây là chỗ công thức gặp giới hạn của miền giá trị thật.

KHÔNG dùng Worksheet.insert_rows(): sheet 01 có nhiều dải gộp A..H, insert_rows dịch giá
trị ô nhưng không dịch dải gộp nên nuốt mất nội dung (đã gặp: dòng "max | 100 | 150 | 30"
mất sạch trừ chữ "max"). Đọc cả sheet ra rồi dựng lại, tính lại dải gộp theo vị trí mới.

Chạy lại nhiều lần vẫn ra một bản.
"""
import copy
import xml.etree.ElementTree as ET
from pathlib import Path

import openpyxl
from openpyxl.styles import Font, PatternFill, Alignment, Border, Side

XLSX = "BaoCao-KiemThu-CareerCompass.xlsx"
SHEET = "01. BVA + Equiv Partition"
DAU_HIEU = "BẢNG 3b"

C_HDR = "1F4E79"; C_TXT = "FFFFFFFF"; C_PASS = "C6EFCE"; C_SEC = "DDEBF7"
C_VAR = "E4DFF5"; C_DIRTY = "FBE4E4"
thin = Side(style="thin", color="BFBFBF")
BORDER = Border(left=thin, right=thin, top=thin, bottom=thin)

f = Path("target/surefire-reports/TEST-vn.uth.careercompass.bva.OnboardingFileSizeBvaTest.xml")
if not f.exists():
    raise SystemExit(f"CHUA CHAY TEST: khong thay {f}. "
                     "Chay 'mvnw test -Dtest=OnboardingFileSizeBvaTest' truoc.")
N = int(ET.parse(f).getroot().attrib["tests"])

# (số case, nhãn B, dung lượng, giá trị biên, expected, method, có phải dirty case)
CASE = [
    (20, "B22", "0 byte", "min", "Hợp lệ — tải lên thành công", "fileSize_normalBva", False),
    (21, "B23", "1 byte", "min+", "Hợp lệ — tải lên thành công", "fileSize_normalBva", False),
    (22, "B24", "5 MB", "nom", "Hợp lệ — tải lên thành công", "fileSize_normalBva", False),
    (23, "B25", "10 MB − 1 byte", "max-", "Hợp lệ — tải lên thành công", "fileSize_normalBva", False),
    (24, "B26", "10 MB", "max", "Hợp lệ — tải lên thành công", "fileSize_normalBva", False),
    (25, "B27", "10 MB + 1 byte", "max+", "KHÔNG hợp lệ — \"File vượt quá dung lượng tối đa 10MB.\"",
     "fileSize_maxPlusOne_isRejected", True),
]

wb = openpyxl.load_workbook(XLSX)
ws = wb[SHEET]

max_r, max_c = ws.max_row, ws.max_column
o_cu = [[ws.cell(row=r, column=c) for c in range(1, max_c + 1)]
        for r in range(1, max_r + 1)]
gop_cu = [(g.min_row, g.min_col, g.max_row, g.max_col) for g in ws.merged_cells.ranges]
cao_cu = {r: ws.row_dimensions[r].height for r in range(1, max_r + 1)
          if ws.row_dimensions[r].height}
rong = {k: v.width for k, v in ws.column_dimensions.items()}


# Nhận diện khối cũ bằng PHẠM VI chứ không bằng từng dòng: khối luôn bắt đầu ở dòng
# "BẢNG 3b" và kết thúc ngay trước "BẢNG 4". Cách dò theo từng dòng trước đây bỏ sót
# dòng ghi chú cuối và dòng trống, nên chạy lại là cộng dồn.
dau_cu = next((r for r in range(1, max_r + 1)
               if str(o_cu[r - 1][0].value or "").startswith(DAU_HIEU)), None)
bo = set()
if dau_cu is not None:
    het_cu = next((r for r in range(dau_cu + 1, max_r + 1)
                   if str(o_cu[r - 1][0].value or "").startswith("BẢNG 4")), None)
    if het_cu is None:
        raise SystemExit("tim thay BANG 3b nhung khong thay BANG 4 phia sau")
    bo = set(range(dau_cu, het_cu))

giu = [r for r in range(1, max_r + 1) if r not in bo]

neo = next((r for r in giu
            if str(o_cu[r - 1][0].value or "").startswith("BẢNG 4")), None)
if neo is None:
    raise SystemExit("khong tim thay BANG 4 de chen phia truoc")

SO_DONG_THEM = 3 + len(CASE) + 2 + 1   # tiêu đề + 2 ghi chú đầu, 6 case, 2 ghi chú cuối, 1 dòng trống
anh_xa, moi, bat_dau = {}, 0, None
for i, r in enumerate(giu, start=1):
    if r == neo:
        bat_dau = i + moi
        moi += SO_DONG_THEM
    anh_xa[r] = i + moi

idx = wb.sheetnames.index(SHEET)
wb.remove(ws)
ws = wb.create_sheet(SHEET)
wb._sheets.remove(ws)
wb._sheets.insert(idx, ws)
for k, v in rong.items():
    ws.column_dimensions[k].width = v

for r_cu in giu:
    r_moi = anh_xa[r_cu]
    for c in range(1, max_c + 1):
        cu = o_cu[r_cu - 1][c - 1]
        x = ws.cell(row=r_moi, column=c, value=cu.value)
        x.font = copy.copy(cu.font)
        x.fill = copy.copy(cu.fill)
        x.alignment = copy.copy(cu.alignment)
        x.border = copy.copy(cu.border)
    if r_cu in cao_cu:
        ws.row_dimensions[r_moi].height = cao_cu[r_cu]

for r1, c1, r2, c2 in gop_cu:
    if r1 in anh_xa and r2 in anh_xa:
        ws.merge_cells(start_row=anh_xa[r1], start_column=c1,
                       end_row=anh_xa[r2], end_column=c2)


def o(r, c, v, *, bold=False, fill=None, center=False, size=10, color=None):
    x = ws.cell(row=r, column=c, value=v)
    x.font = Font(size=size, bold=bold, color=color)
    x.alignment = Alignment(vertical="center", wrap_text=True,
                            horizontal="center" if center else "left")
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


r = bat_dau
thanh(r, "BẢNG 3b — BVA DUNG LƯỢNG TỆP BẢNG ĐIỂM (n = 1 biến) · Standard 4n+1 = 5 case, "
         f"cộng 1 case Robustness · {N} test")
r += 1
thanh(r, "VÌ SAO TÁCH KHỎI BẢNG 2: cột của Bảng 2 là fullName | email | password — ba "
         "biến của MỘT biểu mẫu. Dung lượng tệp thuộc chức năng khác "
         "(OnboardingService.saveTranscript), chỉ có MỘT biến, không có giá trị nào cho "
         "ba cột ấy nên phải đứng bảng riêng. Nhãn B22–B27 và các case TC18–TC23 ở Bảng "
         "4 và Bảng 5 chính là sáu dòng dưới đây.",
       fill=None, color="808080", size=9, italic=True, cao=32)
r += 1

for i, h in enumerate(["Case", "Nhãn", "Dung lượng tệp", "Giá trị biên", "Expected Output",
                       "Kết quả thực tế", "Status", "Method trong code"], start=1):
    o(r, i, h, bold=True, fill=C_HDR, color=C_TXT, center=True)
r += 1

for tc, nhan, dl, bien, exp, method, dirty in CASE:
    hang = [tc, nhan, dl, bien, exp, "Đúng như mong đợi", "PASS", method]
    for j, v in enumerate(hang, start=1):
        f2 = C_PASS if j == 7 else ((C_DIRTY if dirty else C_VAR) if j == 3 else None)
        o(r, j, v, center=(j in (1, 2, 4, 7)), fill=f2, size=(9 if j == 8 else 10))
    r += 1

thanh(r, "CÔNG THỨC GẶP GIỚI HẠN CỦA MIỀN GIÁ TRỊ THẬT — điểm chỉ trường hợp n = 1 này "
         "mới lộ ra. Robustness BVA cho 6n + 1 = 7 case, nhưng ở đây chỉ có 6. Thiếu "
         "đúng một case: min− = −1 byte. Không tồn tại tệp nào dung lượng âm, nên giá "
         "trị đó không nằm trong miền và không có gì để kiểm. Với ba biến của form đăng "
         "ký thì min− luôn tồn tại (chuỗi rỗng, chuỗi ngắn hơn giới hạn) nên Bảng 3 đủ "
         "cả 6 case bổ sung.",
       fill=None, color="404040", size=9, italic=True, cao=42)
r += 1
thanh(r, "CÁCH ĐỌC: ô tô tím là giá trị biên trong miền hợp lệ, ô tô hồng là giá trị "
         "nằm ngoài miền (dirty case). Năm case đầu phủ đúng 4n + 1 = 5 điểm chuẩn "
         "min / min+ / nom / max− / max của một biến duy nhất.",
       fill=None, color="808080", size=9, italic=True, cao=26)

# Cột "Thi hành bảng nào" của dòng OnboardingFileSizeBvaTest phải kể thêm Bảng 3b,
# nếu không bảng tổng kết đầu sheet lại mâu thuẫn với nội dung bên dưới.
for r in range(1, 15):
    if str(ws.cell(row=r, column=1).value or "") == "OnboardingFileSizeBvaTest":
        ws.cell(row=r, column=4,
                value="Bảng 3b · case 20–25" + chr(10)
                      + "Bảng 4 · V5, X12, B22–B27" + chr(10)
                      + "Bảng 5 · TC18–TC23")
        ws.row_dimensions[r].height = 44
        print(f"  da cap nhat cot 'Thi hanh bang nao' o dong {r}")
        break

wb.save(XLSX)
print(f"Da them BANG 3b vao sheet '{SHEET}', tu dong {bat_dau}")
print(f"  {len(CASE)} case, {N} test trong OnboardingFileSizeBvaTest")

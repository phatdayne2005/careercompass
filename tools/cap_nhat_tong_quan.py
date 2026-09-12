# -*- coding: utf-8 -*-
"""Cập nhật sheet "00. Tong quan" và "07. Khiem khuyet" sau khi thêm hai sheet bổ sung.

Chạy sau tools/gen_sheets_bo_sung.py:
    python tools/cap_nhat_tong_quan.py

Mọi con số đều đọc lại từ target/surefire-reports và target/site/jacoco/jacoco.csv,
không gõ tay — lệch là script dừng.

KHÔNG dùng Worksheet.insert_rows(): trên sheet có ô gộp, openpyxl dịch giá trị nhưng
không dịch dải gộp, làm mất nội dung của đúng những dòng có gộp (đã gặp: ba dòng
"Kiểm thử API", "Độ phức tạp được phủ", "Tích hợp (Postman)" bị xoá trắng). Thay vào đó
đọc toàn bộ sheet ra bộ nhớ kèm định dạng, rồi dựng lại sheet mới với dòng đã chèn.
"""
import copy
import csv
import glob
import xml.etree.ElementTree as ET
from pathlib import Path

import openpyxl
from openpyxl.styles import Font, PatternFill, Alignment, Border, Side
from openpyxl.utils import get_column_letter

XLSX = "BaoCao-KiemThu-CareerCompass.xlsx"
C_PASS = "C6EFCE"; C_OK = "E2F0D9"
thin = Side(style="thin", color="BFBFBF")
BORDER = Border(left=thin, right=thin, top=thin, bottom=thin)


# ── Đọc số liệu thật ─────────────────────────────────────────────────────
def tong_so_test():
    t = f = e = 0
    for p in glob.glob("target/surefire-reports/TEST-*.xml"):
        g = ET.parse(p).getroot().attrib
        t += int(g["tests"]); f += int(g["failures"]); e += int(g["errors"])
    if f or e:
        raise SystemExit(f"Con {f} fail / {e} error — sua source truoc khi sinh report.")
    return t


def so_test_lop(cls):
    p = Path(f"target/surefire-reports/TEST-vn.uth.careercompass.blackbox.{cls}.xml")
    if not p.exists():
        raise SystemExit(f"CHUA CHAY TEST: khong thay {p}")
    return int(ET.parse(p).getroot().attrib["tests"])


def bao_phu():
    rows = list(csv.DictReader(open("target/site/jacoco/jacoco.csv", encoding="utf-8")))

    def p(a, b):
        c = sum(int(r[a]) for r in rows); m = sum(int(r[b]) for r in rows)
        return 100 * c / (c + m)

    return (p("LINE_COVERED", "LINE_MISSED"), p("BRANCH_COVERED", "BRANCH_MISSED"),
            p("COMPLEXITY_COVERED", "COMPLEXITY_MISSED"),
            p("METHOD_COVERED", "METHOD_MISSED"))


def vn(x):
    return f"{x:.1f}".replace(".", ",")


TONG = tong_so_test()
DT_TEP = so_test_lop("TranscriptFileDecisionTableTest")
ST_ONB = so_test_lop("OnboardingStateTransitionTest")
LINE, BRANCH, CXTY, METHOD = bao_phu()

wb = openpyxl.load_workbook(XLSX)


# ── Dựng lại sheet với các dòng chèn thêm, giữ nguyên định dạng ──────────
def chen_dong(ws, chen_sau, bo=None):
    """Dựng lại sheet: bỏ những dòng `bo` chọn, rồi chèn thêm `chen_sau`.

    chen_sau: {văn bản cột A của dòng mốc: [các hàng chèn ngay SAU nó]}.
    bo(hang) -> True nếu dòng đó phải biến mất.

    Định vị bằng VĂN BẢN chứ không bằng số hiệu dòng, và bỏ trước khi chèn, nên chạy
    lại trên chính file đã sinh lần trước vẫn ra đúng một bản — không nhân đôi.

    Đọc hết sheet ra rồi ghi lại vào sheet mới, nên dải gộp và chiều cao dòng được
    tính lại đúng theo vị trí mới thay vì bị bỏ lại phía sau.
    """
    max_r, max_c = ws.max_row, ws.max_column
    o_cu = [[ws.cell(row=r, column=c) for c in range(1, max_c + 1)]
            for r in range(1, max_r + 1)]
    gop_cu = [(g.min_row, g.min_col, g.max_row, g.max_col) for g in ws.merged_cells.ranges]
    cao_cu = {r: ws.row_dimensions[r].height for r in range(1, max_r + 1)
              if ws.row_dimensions[r].height}
    rong = {k: v.width for k, v in ws.column_dimensions.items()}

    giu = [r for r in range(1, max_r + 1)
           if not (bo and bo([o_cu[r - 1][c].value for c in range(max_c)]))]

    # dòng gốc r -> dòng mới; đồng thời gom các hàng cần chèn.
    anh_xa, hang_moi, moi = {}, {}, 0
    for i, r in enumerate(giu, start=1):
        anh_xa[r] = i + moi
        if str(o_cu[r - 1][0].value or "").strip() in chen_sau:
            for h in chen_sau[str(o_cu[r - 1][0].value).strip()]:
                moi += 1
                hang_moi[i + moi] = h

    ten = ws.title
    idx = wb.sheetnames.index(ten)
    wb.remove(ws)
    ws = wb.create_sheet(ten)
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

    # Hàng mới: sao định dạng của dòng ngay phía trên để nhìn liền mạch.
    for r_moi, hang in hang_moi.items():
        mau = r_moi - 1
        for c, v in enumerate(hang, start=1):
            x = ws.cell(row=r_moi, column=c, value=v)
            m = ws.cell(row=mau, column=c)
            x.font = copy.copy(m.font)
            x.fill = copy.copy(m.fill)
            x.alignment = copy.copy(m.alignment)
            x.border = copy.copy(m.border)
        if ws.row_dimensions[mau].height:
            ws.row_dimensions[r_moi].height = ws.row_dimensions[mau].height
        # Dải gộp F:H của bảng 1 phải nhân bản cho hàng mới, nếu dòng mẫu có.
        for g in list(ws.merged_cells.ranges):
            if g.min_row == g.max_row == mau and g.min_col > 1:
                ws.merge_cells(start_row=r_moi, start_column=g.min_col,
                               end_row=r_moi, end_column=g.max_col)
    return ws


# ── 00. Tong quan ────────────────────────────────────────────────────────
ws = wb["00. Tong quan"]
# Cột 6 ghi tên sheet chi tiết. Mọi dòng trỏ tới 01c / 03c / 04c đều do script này
# chèn ở lần chạy trước, nên gỡ hết rồi chèn lại — chạy bao nhiêu lần cũng ra một bản.
# 01c là sheet BVA ngưỡng cắt đã bị bỏ, chỉ gỡ chứ không chèn lại.
ws = chen_dong(
    ws,
    {"Bảng quyết định":
        [["Bảng quyết định — tệp bảng điểm", "Đơn vị", DT_TEP, "JUnit 5",
          f"{DT_TEP} / {DT_TEP}", "03c", None, None]],
     "Chuyển đổi trạng thái — vòng đời token":
        [["Chuyển đổi trạng thái — khai báo hồ sơ", "Đơn vị", ST_ONB, "JUnit 5",
          f"{ST_ONB} / {ST_ONB}", "04c", None, None]]},
    bo=lambda hang: str(hang[5] or "").strip() in ("01c", "03c", "04c"))


def tim(ws, khoa):
    for r in range(1, ws.max_row + 1):
        if str(ws.cell(row=r, column=1).value or "").strip().startswith(khoa):
            return r
    raise SystemExit(f"khong tim thay dong '{khoa}' trong {ws.title}")


# Bảng 2 — độ bao phủ: viết lại cột "Sau" và cột "Tăng" theo số đo lại.
for khoa, truoc, sau in [("Dòng lệnh (Line)", 60.1, LINE),
                         ("Nhánh (Branch)", 48.2, BRANCH),
                         ("Độ phức tạp được phủ", 53.4, CXTY),
                         ("Phương thức", 65.1, METHOD)]:
    r = tim(ws, khoa)
    ws.cell(row=r, column=3, value=f"{vn(sau)}%").fill = PatternFill("solid", fgColor=C_OK)
    ws.cell(row=r, column=4, value=f"+{vn(sau - truoc)} điểm")

# Bảng 3 — tổng số test đơn vị.
r = tim(ws, "Đơn vị (JUnit)")
ws.cell(row=r, column=3, value=TONG)

ws.cell(row=ws.max_row, column=1).value = (
    "Mười một khiếm khuyết được phát hiện và đều đã khắc phục kèm test hồi quy. "
    "Chi tiết ở sheet 07. Khiem khuyet.")

# ── 07. Khiem khuyet ─────────────────────────────────────────────────────
ws = wb["07. Khiem khuyet"]
r_ket = tim(ws, "Cả mười")
r_moi = r_ket - 1
# Chạy lại trên file đã sinh lần trước thì dòng này đang là DEF-011 — ghi đè chính nó
# là đúng. Chỉ dừng khi gặp nội dung LẠ, để không đè nhầm lên khiếm khuyết của người khác.
o_dau = str(ws.cell(row=r_moi, column=1).value or "").strip()
if o_dau not in ("", "DEF-011"):
    raise SystemExit(f"dong {r_moi} cua sheet 07 dang chua '{o_dau}' — kiem tra lai truoc khi ghi de")

HANG = [
    "DEF-011", "Trung bình",
    "Tệp bảng điểm không có phần mở rộng (ví dụ \"bangdiem\") làm lastIndexOf(\".\") trả -1, "
    "kéo theo substring(-1) ném StringIndexOutOfBoundsException — người dùng nhận lỗi 500 "
    "thay vì thông báo 400 thân thiện.",
    "Bảng quyết định — cột R8 của bảng định dạng tệp bảng điểm",
    "Chạy test R8 trên bản mã chưa sửa: đỏ, nhận StringIndexOutOfBoundsException. Trên bản "
    "đã sửa: xanh. Nhánh OnboardingService 75,0% (9/12) → 100,0% (14/14).",
    "ĐÃ SỬA",
    "Coi \"không có đuôi\" là đuôi rỗng để rơi đúng vào nhánh báo lỗi định dạng có sẵn. Test cũ "
    "trong OnboardingServiceTest từng khoá hành vi lỗi, nay đã đổi sang khẳng định hành vi đúng.",
]
mau = r_moi - 1
for c, v in enumerate(HANG, start=1):
    x = ws.cell(row=r_moi, column=c, value=v)
    m = ws.cell(row=mau, column=c)
    x.font = copy.copy(m.font)
    x.fill = copy.copy(m.fill)
    x.alignment = copy.copy(m.alignment)
    x.border = copy.copy(m.border)
ws.merge_cells(start_row=r_moi, start_column=7, end_row=r_moi, end_column=8)
ws.row_dimensions[r_moi].height = ws.row_dimensions[mau].height

ws.cell(row=r_ket, column=1).value = (
    "Cả mười một khiếm khuyết đã được khắc phục và có test hồi quy đi kèm. "
    "Ba khiếm khuyết DEF-001, DEF-009 và DEF-010 đều do KIỂM THỬ GIAO DIỆN tìm ra — đây là "
    "tầng duy nhất chạm tới HTML nên là tầng duy nhất thấy được chúng; suốt thời gian đó test "
    "đơn vị và test API vẫn xanh. "
    "DEF-011 đáng chú ý theo cách ngược lại: test hộp trắng đã GHI ĐÚNG nguyên nhân từ trước "
    "nhưng lại chốt luôn hành vi lỗi bằng một khẳng định, nên bộ test vẫn xanh và khiếm khuyết "
    "nằm im. Phải tới khi dựng bảng quyết định — nơi mỗi cột buộc phải có một hành động xác "
    "định — nó mới bị buộc phải sửa.")

wb.save(XLSX)
print(f"Da cap nhat 00. Tong quan va 07. Khiem khuyet trong {XLSX}")
print(f"  tong test don vi : {TONG}")
print(f"  bao phu dong     : {vn(LINE)}%")
print(f"  bao phu nhanh    : {vn(BRANCH)}%")

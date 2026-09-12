# -*- coding: utf-8 -*-
"""Thêm sheet "00b. Phan cong nhom" vào BaoCao-KiemThu-CareerCompass.xlsx.

    python tools/gen_sheet_phan_cong.py      (chạy sau cap_nhat_tong_quan.py)

Vì sao cần sheet này: báo cáo Excel là một trong hai tài liệu giảng viên dùng để chấm
điểm, và chấm cho cả nhóm lẫn từng cá nhân. Nhưng 16 sheet nội dung không ghi tác giả ở
bất kỳ đâu — đọc xong không biết ai làm phần nào. Đáng chú ý nhất là năm sheet hộp trắng
B1–B5 hoàn toàn do một thành viên thực hiện mà không có dấu hiệu nào cho biết điều đó.

Người thực hiện từng sheet được xác định bằng cách tra tác giả commit đầu tiên tạo ra
tệp nguồn tương ứng (git log --diff-filter=A), không dựa vào ghi nhớ.

Chạy lại nhiều lần vẫn ra một bản: sheet cũ bị gỡ trước khi dựng lại.
"""
import collections
import subprocess
import xml.etree.ElementTree as ET
from pathlib import Path

import openpyxl
from openpyxl.styles import Font, PatternFill, Alignment, Border, Side
from openpyxl.utils import get_column_letter

XLSX = "BaoCao-KiemThu-CareerCompass.xlsx"
SHEET = "00b. Phan cong nhom"
DAT_SAU = "00. Tong quan"
BUOI_DAU = "2026-07-22"

C_HDR = "1F4E79"; C_TXT = "FFFFFFFF"; C_SEC = "DDEBF7"; C_OK = "E2F0D9"
thin = Side(style="thin", color="BFBFBF")
BORDER = Border(left=thin, right=thin, top=thin, bottom=thin)

GOP_TEN = {
    "PhatDepZai": "Nguyễn Thành Phát",
    "vmquan2200": "Vòng Minh Quân",
    "PHAMSONTUANKIET": "Phạm Sơn Tuấn Kiệt",
    "NhwNgocc": "Trần Tô Như Ngọc",
}

# sheet trong báo cáo -> (nội dung, người thực hiện)
SHEET_PHAN_CONG = [
    ("00. Tong quan", "Tổng hợp bốn tầng kiểm thử, độ bao phủ, số phép kiểm",
     "Nguyễn Thành Phát"),
    ("01. BVA + Equiv Partition", "Giá trị biên và phân hoạch lớp tương đương cho biểu "
     "mẫu đăng ký; giá trị biên cho dung lượng tệp",
     "Nguyễn Thành Phát · Vòng Minh Quân"),
    ("03. Decision Table (PhanA)", "Bảng quyết định — luật chặn cập nhật tiến độ học",
     "Nguyễn Thành Phát"),
    ("03b. Decision Table Token", "Bảng quyết định — hiệu lực mã đặt lại mật khẩu",
     "Nguyễn Thành Phát"),
    ("03c. Decision Table Tep", "Bảng quyết định — định dạng và dung lượng tệp bảng điểm",
     "Nguyễn Thành Phát"),
    ("04. State Transition (PhanA)", "Chuyển đổi trạng thái — tiến độ node kỹ năng",
     "Nguyễn Thành Phát"),
    ("04b. State Transition Token", "Chuyển đổi trạng thái — vòng đời mã đặt lại mật khẩu",
     "Nguyễn Thành Phát"),
    ("04c. State Transition Onboard", "Chuyển đổi trạng thái — quy trình khai báo hồ sơ",
     "Nguyễn Thành Phát"),
    ("B1_CFG_va_Basis_Path", "Đồ thị dòng điều khiển, độ phức tạp Cyclomatic, 9 đường "
     "cơ sở", "Vòng Minh Quân"),
    ("B2_Coverage_Levels", "So sánh bốn mức bao phủ và ma trận tổ hợp điều kiện MC/DC",
     "Vòng Minh Quân"),
    ("B3_Truoc_va_Sau", "Độ bao phủ trước và sau khi bổ sung kiểm thử, kèm nhận xét",
     "Vòng Minh Quân"),
    ("B4_Coverage_Per_Class", "Độ bao phủ chi tiết theo từng lớp", "Vòng Minh Quân"),
    ("B5_Danh_Sach_Test_Whitebox", "Danh mục tệp kiểm thử thuộc phạm vi hộp trắng",
     "Vòng Minh Quân"),
    ("05. API - Postman", "Kiểm thử API: phân bổ theo nhóm chức năng, phép kiểm, lỗi. "
     "Bộ kiểm thử do Nguyễn Thành Phát và Vòng Minh Quân xây dựng", "Trần Tô Như Ngọc"),
    ("06. E2E - CodeceptJS", "Kiểm thử giao diện đầu-cuối: 17 kịch bản, ma trận truy "
     "vết. Bộ kiểm thử do Nguyễn Thành Phát xây dựng", "Trần Tô Như Ngọc"),
    ("07. Khiem khuyet", "11 khiếm khuyết: mô tả, kỹ thuật phát hiện, cách khắc phục",
     "Nguyễn Thành Phát"),
]

# Mảng công việc không hiện thành sheet riêng nhưng vẫn nằm trong phạm vi môn học.
NGOAI_SHEET = [
    ("Kiểm thử đơn vị tầng dịch vụ", "24 lớp kiểm thử, phủ 9 phân hệ nghiệp vụ",
     "Nguyễn Thành Phát"),
    ("Kiểm thử tầng điều khiển", "8 lớp kiểm thử, kèm 2 lớp hỗ trợ CSRF và bảo mật",
     "Vòng Minh Quân"),
    ("Kiểm thử bộ xử lý ngoại lệ", "Đối chiếu loại ngoại lệ với mã trạng thái HTTP",
     "Vòng Minh Quân"),
    ("Bản thuyết minh bằng văn bản", "Bốn phần: đơn vị, API, giao diện, kỹ thuật dựa "
     "trên kinh nghiệm", "Trần Tô Như Ngọc"),
    ("Xây dựng bộ kiểm thử API", "Tập lệnh Postman, biến môi trường, bảo đảm chạy lại "
     "được nhiều lần", "Nguyễn Thành Phát · Vòng Minh Quân"),
    ("Xây dựng bộ kiểm thử giao diện", "CodeceptJS và Playwright, mô hình Page Object",
     "Nguyễn Thành Phát"),
    ("Sửa lỗi và tái cấu trúc giao diện", "Lỗi rời trang KCPMS-22, trang cố vấn AI",
     "Vòng Minh Quân"),
    ("Quy trình CI/CD và triển khai", "GitHub Actions, Docker, VPS, chặn deploy khi đỏ",
     "Vòng Minh Quân · Nguyễn Thành Phát"),
    ("Phân tích tĩnh và đo bao phủ", "SonarCloud, JaCoCo, ngưỡng chặn 65% nhánh",
     "Nguyễn Thành Phát"),
    ("Khắc phục khiếm khuyết ứng dụng", "11 khiếm khuyết, kèm kiểm thử hồi quy",
     "Nguyễn Thành Phát"),
]


def thanh_vien():
    raw = subprocess.run(["git", "log", "--format=%an", f"--since={BUOI_DAU}"],
                         capture_output=True, text=True, encoding="utf-8").stdout
    dem = collections.Counter(GOP_TEN.get(a.strip(), a.strip())
                              for a in raw.strip().split("\n") if a.strip())
    return dem.most_common()


def tong_test():
    t = 0
    for p in Path("target/surefire-reports").glob("TEST-*.xml"):
        t += int(ET.parse(p).getroot().attrib["tests"])
    return t


wb = openpyxl.load_workbook(XLSX)
if SHEET in wb.sheetnames:
    wb.remove(wb[SHEET])
ws = wb.create_sheet(SHEET)
wb._sheets.remove(ws)
wb._sheets.insert(wb.sheetnames.index(DAT_SAU) + 1, ws)
for i, w in enumerate([26, 30, 26, 16, 16, 16, 16, 16], start=1):
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


def tieu_de_cot(r, cot):
    for i, h in enumerate(cot, start=1):
        o(r, i, h, bold=True, fill=C_HDR, color=C_TXT, center=True)
    return r + 1


TV = thanh_vien()
TONG_COMMIT = sum(n for _, n in TV)

x = ws.cell(row=1, column=1, value="Phân công thực hiện trong nhóm")
x.font = Font(bold=True, size=13, color=C_HDR)
ws.cell(row=2, column=1, value="Người thực hiện từng phần được xác định từ lịch sử Git "
        "(tác giả commit đầu tiên tạo ra tệp nguồn tương ứng), không ghi theo trí nhớ."
        ).font = Font(italic=True, size=10)

r = thanh(4, f"KỲ BÁO CÁO — từ {BUOI_DAU[8:10]}/{BUOI_DAU[5:7]}/{BUOI_DAU[:4]}, "
             f"{TONG_COMMIT} commit của {len(TV)} thành viên, "
             f"{tong_test()} trường hợp kiểm thử tự động",
          fill=C_OK, color="375623", cao=26)
r += 1

r = thanh(r, "BẢNG 1 — THÀNH VIÊN VÀ MẢNG CÔNG VIỆC CHÍNH")
r = tieu_de_cot(r, ["Thành viên", "Mảng công việc chính", "", "Commit", "", "", "", ""])
MO_TA = {
    "Nguyễn Thành Phát": "Kiểm thử đơn vị tầng dịch vụ · Kiểm thử hộp đen chương IV · "
                         "Xây dựng bộ kiểm thử API và giao diện · Khắc phục khiếm khuyết",
    "Vòng Minh Quân": "Kiểm thử hộp trắng · Kiểm thử tầng điều khiển · Kiểm thử giá trị "
                      "biên · Sửa lỗi giao diện · Các báo cáo dạng bảng",
    "Trần Tô Như Ngọc": "Thực hiện kiểm thử API và kiểm thử giao diện đầu-cuối, lập báo "
                        "cáo kết quả · Bản thuyết minh kiểm chứng phần mềm bằng văn bản",
}
for ten, n in TV:
    o(r, 1, ten, bold=True)
    ws.merge_cells(start_row=r, start_column=2, end_row=r, end_column=3)
    o(r, 2, MO_TA.get(ten, ""))
    o(r, 4, n, center=True)
    for c in (5, 6, 7, 8):
        o(r, c, "")
    r += 1
r = thanh(r, "SỐ COMMIT KHÔNG TỶ LỆ VỚI KHỐI LƯỢNG CÔNG VIỆC. Một bản thuyết minh dài "
             "hơn trăm đoạn kèm bảng biểu và ảnh chụp màn hình chỉ chiếm một đến hai "
             "commit, trong khi một đợt chỉnh sửa cách trình bày báo cáo có thể sinh ra "
             "hơn mười commit nhỏ. Hai bảng bên dưới phản ánh đóng góp chính xác hơn.",
          fill=None, color="808080", size=9, italic=True, cao=30)
r += 1

r = thanh(r, "BẢNG 2 — NGƯỜI THỰC HIỆN TỪNG SHEET CỦA BÁO CÁO NÀY")
r = tieu_de_cot(r, ["Sheet", "Nội dung", "", "Người thực hiện", "", "", "", ""])
for ten_sheet, nd, ai in SHEET_PHAN_CONG:
    o(r, 1, ten_sheet)
    ws.merge_cells(start_row=r, start_column=2, end_row=r, end_column=3)
    o(r, 2, nd)
    ws.merge_cells(start_row=r, start_column=4, end_row=r, end_column=5)
    o(r, 4, ai)
    for c in (6, 7, 8):
        o(r, c, "")
    r += 1
r = thanh(r, "Năm sheet B1–B5 là phần kiểm thử hộp trắng: đồ thị dòng điều khiển, độ "
             "phức tạp Cyclomatic, đường cơ sở, ma trận tổ hợp điều kiện MC/DC và đối "
             "chiếu độ bao phủ trước–sau.",
          fill=None, color="808080", size=9, italic=True, cao=26)
r = thanh(r, "Với sheet 05 và 06, cột \"Người thực hiện\" ghi người CHẠY kiểm thử và "
             "LẬP BÁO CÁO — đó mới là nội dung của hai sheet này. Người xây dựng bộ "
             "kiểm thử ghi trong cột Nội dung và ở Bảng 3.",
          fill=None, color="808080", size=9, italic=True, cao=26)
r += 1

r = thanh(r, "BẢNG 3 — CÁC PHẦN VIỆC KHÔNG HIỆN THÀNH SHEET RIÊNG")
r = tieu_de_cot(r, ["Phần việc", "Nội dung", "", "Người thực hiện", "", "", "", ""])
for phan, nd, ai in NGOAI_SHEET:
    o(r, 1, phan)
    ws.merge_cells(start_row=r, start_column=2, end_row=r, end_column=3)
    o(r, 2, nd)
    ws.merge_cells(start_row=r, start_column=4, end_row=r, end_column=5)
    o(r, 4, ai)
    for c in (6, 7, 8):
        o(r, c, "")
    r += 1
r = thanh(r, "Các phần trên nằm trong mã nguồn và tài liệu kèm theo, không trình bày "
             "thành bảng riêng trong tệp Excel này. Chi tiết theo tuần xem tệp "
             "BaoCao-CongViec-Theo-Tuan.docx.",
          fill=None, color="808080", size=9, italic=True, cao=26)

wb.save(XLSX)
print(f"Da them sheet '{SHEET}' vao {XLSX}")
for ten, n in TV:
    print(f"  {ten:<20} {n:>3} commit")

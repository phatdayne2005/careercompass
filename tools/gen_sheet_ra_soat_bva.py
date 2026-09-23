# -*- coding: utf-8 -*-
"""Sinh sheet "10. Ra soat BVA" — kiểm kê toàn bộ ứng viên giá trị biên trong hệ thống.

    python tools/gen_sheet_ra_soat_bva.py

VÌ SAO CẦN: sheet 01 trình bày rất kỹ BỐN bảng BVA đã làm, nhưng không trả lời được câu
"còn biên nào trong hệ thống mà nhóm chưa áp BVA không". Người chấm hỏi câu đó thì trả
lời "chắc là đủ rồi" nghe rất yếu.

Sheet này liệt kê MỌI ngưỡng số học tìm được trong mã nguồn và mọi ràng buộc có biên
trong SRS, rồi nói rõ từng cái: đã áp BVA chưa, nếu chưa thì vì sao. Có những biên cố ý
không áp — biên cắt chuỗi nội bộ không có yêu cầu nào ràng buộc — và nói thẳng ra vẫn
tốt hơn là im lặng.

Danh sách dưới đây dựng bằng tay sau khi quét mã nguồn bằng:
    grep -rn 'length() >\\|getSize() >\\|<= [0-9]\\|>= [0-9]\\|plusMinutes' src/main/java
Mỗi dòng ghi kèm vị trí để người đọc tự đối chiếu. Số test đọc từ surefire.
"""
import glob
import xml.etree.ElementTree as ET
from pathlib import Path

import openpyxl
from openpyxl.styles import Alignment, Border, Font, PatternFill, Side

XLSX = "BaoCao-KiemThu-CareerCompass.xlsx"
SHEET = "10. Ra soat BVA"

C_HDR = "1F4E79"; C_TXT = "FFFFFFFF"; C_SEC = "DDEBF7"
C_DA = "C6EFCE"; C_MOI = "FFE699"; C_KHONG = "F2F2F2"
thin = Side(style="thin", color="BFBFBF")
BORDER = Border(left=thin, right=thin, top=thin, bottom=thin)

DA = "Đã áp dụng"
MOI = "Mới bổ sung"
KHONG = "Không áp dụng"

# (biến, biên, vị trí trong mã nguồn, ràng buộc trong SRS, trạng thái, lớp kiểm thử, lý do)
UNG_VIEN = [
    ("Họ tên khi đăng ký", "1 → 100 ký tự",
     "RegisterFormDTO @Size(max = 100)", "AC-6.1.x, bảng trường mục 4.6", DA,
     "RegisterStandardBvaTest, RegisterFormDTOBvaTest",
     "Bảng 1, 2, 3 của sheet 01 — Standard 4n+1 và Robustness 6n+1."),

    ("Email khi đăng ký", "6 → 150 ký tự",
     "RegisterFormDTO @Size(min = 6, max = 150)", "AC-6.1.x", DA,
     "RegisterStandardBvaTest, RegisterFormDTOBvaTest",
     "Cùng ba bảng trên. min = 6 là email ngắn nhất có thật: a@b.co."),

    ("Mật khẩu khi đăng ký", "6 → 30 ký tự",
     "RegisterFormDTO @Size(min = 6, max = 30)", "AC-6.1.3 (nêu đích danh 5/6/30/31)", DA,
     "RegisterStandardBvaTest, RegisterFormDTOBvaTest",
     "AC-6.1.3 yêu cầu đúng bốn điểm 5/6/30/31 — đã phủ đủ."),

    ("Dung lượng tệp bảng điểm", "0 → 10 MB",
     "OnboardingService:48  file.getSize() > 10*1024*1024", "AC-1.3.3", DA,
     "OnboardingFileSizeBvaTest",
     "Bảng 3b sheet 01. Trường hợp n = 1 biến, và là chỗ duy nhất lộ ra giới hạn "
     "của công thức: Robustness cho 6n+1 = 7 case nhưng min− = −1 byte không tồn tại."),

    ("Mật khẩu ở BA đường đặt", "6 → 30 ký tự VÀ ≤ 72 byte",
     "PasswordPolicy  ·  AuthController:register/resetPassword  ·  ProfileController:changePassword",
     "AC-6.1.3 (mới chỉ nói ký tự)", MOI,
     "PasswordPolicyBvaTest",
     "DEF-013. Trước đây chỉ đường ĐĂNG KÝ có biên trên; đặt lại và đổi mật khẩu chỉ "
     "kiểm < 6, không có max. Nặng hơn: @Size đếm KÝ TỰ còn BCrypt giới hạn 72 BYTE — "
     "25 ký tự tiếng Việt có dấu là 75 byte, lọt validation rồi vỡ ở tầng mã hoá."),

    ("Hạn dùng token đặt lại mật khẩu", "30 phút",
     "PasswordResetService:40 plusMinutes(30)  ·  PasswordResetToken:34 isAfter",
     "FR6.1, NFR-S07", MOI,
     "TokenExpiryBvaTest",
     "Sheet 03b và 04b đã có bảng quyết định và chuyển trạng thái cho token, nhưng chỉ "
     "dùng hai mốc cách biên rất xa (±1 phút, +30 phút). Biên thật nằm ở phép so sánh "
     "isAfter — nghiêm ngặt, nên đúng thời điểm hết hạn vẫn còn hiệu lực."),

    ("Độ dài mô tả tin tuyển dụng", "3.900 ký tự (cột VARCHAR(4000))",
     "ScraperService:38 MAX_DESC  ·  :107 cap(raw, MAX_DESC)", "AC-4.1.4", MOI,
     "ScraperServiceTest",
     "Có AC riêng trong SRS và đụng trần cột CSDL. Tác vụ cào chạy nền lúc 02:00 nên "
     "nếu tràn thì lỗi chỉ nằm trong log, không ai thấy cho tới khi trang trống."),

    ("Độ dài tiêu đề tin tuyển dụng", "255 ký tự",
     "ScraperService:107 cap(title, 255)", "—", DA,
     "ScraperServiceTest",
     "Đã có sẵn một phép kiểm biên trước đợt rà soát này."),

    ("Số lần thử lại khi gọi LLM", "4 lần",
     "LlmClient:41  for (attempt = 1; attempt <= 4)", "FR1.2, NFR-P02", DA,
     "LlmClientTest",
     "Miền rời rạc rất nhỏ nên phủ được TOÀN BỘ: 1 lần (thành công ngay), 2 lần (lỗi "
     "rồi thành công), 4 lần (hỏng suốt), và 1 lần với lỗi 400 không được thử lại."),

    ("Tầng của node kỹ năng", "tier ≤ 1, = 2, ≥ 3",
     "RoadmapService:87, :131", "FR2.2, AC-2.2.x", KHONG,
     "RoadmapServiceTest + sheet B2 (MC/DC)",
     "Miền chỉ có ba lớp hành vi và chúng khác nhau về LOGIC chứ không về độ lớn, nên "
     "bảng tổ hợp điều kiện MC/DC ở sheet B2 mô tả đúng bản chất hơn BVA. Vẫn phủ 100% "
     "nhánh."),

    ("Độ dài tiêu đề phiên chat", "cắt ở 60 ký tự",
     "MentorService:52", "AC-1.1.4", KHONG,
     "MentorServiceTest",
     "Biên CẮT chứ không phải biên CHẤP NHẬN/TỪ CHỐI: qua 60 ký tự không bị từ chối mà "
     "chỉ bị rút gọn để hiển thị. Sai một ký tự ở đây không gây hỏng nghiệp vụ nào. "
     "AC-1.1.4 chỉ yêu cầu tiêu đề khác 'Cuộc trò chuyện mới', đã có phép kiểm."),

    ("Độ dài văn bản bảng điểm gửi AI", "cắt ở 6.000 ký tự",
     "TranscriptAnalysisService:32", "—", KHONG,
     "TranscriptAnalysisServiceTest",
     "Biên cắt nội bộ để tiết kiệm token của mô hình, không có yêu cầu nào trong SRS "
     "ràng buộc con số này và không đụng trần cột CSDL."),

    ("Độ dài ngữ cảnh README gửi AI", "cắt ở 3.000 / trích 200 ký tự",
     "PortfolioService:243, :249", "—", KHONG,
     "PortfolioServiceTest",
     "Cùng lý do trên: biên cắt nội bộ, không phải ràng buộc nghiệp vụ."),

    ("Số tài liệu học mỗi node", "≥ 2",
     "(không cưỡng chế trong mã nguồn)", "AC-2.3.1, AC-2.3.3, BR-61", KHONG,
     "—",
     "KHÔNG kiểm được bằng BVA vì mã nguồn KHÔNG cưỡng chế quy tắc này — hệ thống cho "
     "lưu node có 0 hoặc 1 tài liệu. Đây là khoảng trống HIỆN THỰC chứ không phải "
     "khoảng trống kiểm thử; ghi nhận để nhóm xử lý sau."),

    ("Độ dài URL tài nguyên học", "≤ 500 ký tự",
     "(không cưỡng chế trong mã nguồn)", "AC-2.3.2", KHONG,
     "—",
     "Cùng tình trạng: SRS nêu giới hạn nhưng mã nguồn chưa kiểm, nên chưa có biên để "
     "kiểm thử. Ghi nhận cùng nhóm với dòng trên."),

    ("Độ dài slug hồ sơ công khai", "≤ 120 ký tự",
     "PortfolioService:256 (base + 6 ký tự UUID)", "Bảng CSDL mục 8", KHONG,
     "PortfolioServiceTest",
     "Slug do hệ thống sinh chứ người dùng không nhập, và độ dài bị chặn bởi tên "
     "repository của GitHub (tối đa 100). Không có đầu vào nào đẩy tới biên."),

    ("Ngưỡng thời gian phản hồi", "2s / 15s / 30s / 5s",
     "(không có trong mã nguồn)", "NFR-P01 → NFR-P04", KHONG,
     "Postman — phép kiểm responseTime",
     "Là ngưỡng HIỆU NĂNG đo trên hệ thống đang chạy, không phải biên của một tham số "
     "đầu vào. Kiểm ở tầng Postman đúng chỗ hơn; BVA đơn vị không nói được gì."),
]


def doc_surefire():
    kq = {}
    for p in glob.glob("target/surefire-reports/TEST-*.xml"):
        g = ET.parse(p).getroot().attrib
        kq[Path(p).stem[len("TEST-"):].rsplit(".", 1)[-1]] = int(g["tests"])
    if not kq:
        raise SystemExit("CHUA CHAY TEST: chay './mvnw clean test' truoc.")
    return kq


TEST = doc_surefire()
for *_, trang_thai, lop_test, _ in UNG_VIEN:
    for lop in [c.strip() for c in lop_test.split(",")]:
        ten = lop.split(" ")[0]
        if ten.endswith("Test") and ten not in TEST:
            raise SystemExit(f"lop kiem thu '{ten}' khong co trong surefire-reports")

MAU = {DA: C_DA, MOI: C_MOI, KHONG: C_KHONG}

wb = openpyxl.load_workbook(XLSX)
if SHEET in wb.sheetnames:
    wb.remove(wb[SHEET])
ws = wb.create_sheet(SHEET)
for cot, rong in zip("ABCDEFG", (28, 26, 42, 22, 13, 30, 62)):
    ws.column_dimensions[cot].width = rong


def o(r, c, v, *, bold=False, fill=None, center=False, size=10, color=None):
    x = ws.cell(row=r, column=c, value=v)
    x.font = Font(size=size, bold=bold, color=color)
    x.alignment = Alignment(vertical="top", wrap_text=True,
                            horizontal="center" if center else "left")
    x.border = BORDER
    if fill:
        x.fill = PatternFill("solid", fgColor=fill)


def thanh(r, t, *, fill=C_SEC, color=C_HDR, size=11, italic=False, cao=24):
    ws.merge_cells(start_row=r, start_column=1, end_row=r, end_column=7)
    x = ws.cell(row=r, column=1, value=t)
    x.font = Font(bold=not italic, italic=italic, size=size, color=color)
    if fill:
        x.fill = PatternFill("solid", fgColor=fill)
    x.alignment = Alignment(vertical="top", wrap_text=True)
    ws.row_dimensions[r].height = cao
    return r + 1


r = 1
r = thanh(r, "RÀ SOÁT GIÁ TRỊ BIÊN TOÀN HỆ THỐNG — CÒN SÓT CHỖ NÀO KHÔNG?", size=14, cao=30)
r = thanh(r, "Sheet 01 trình bày các bảng BVA đã làm. Sheet này trả lời câu khác: đã quét "
             "hết hệ thống chưa. Danh sách dựng bằng cách grep mọi ngưỡng số học trong "
             "src/main/java rồi đối chiếu với mọi ràng buộc có biên trong SRS. Mỗi dòng "
             "nói rõ đã áp BVA chưa, và nếu không áp thì vì sao — nói thẳng vẫn hơn im lặng.",
           fill=None, color="595959", size=9, italic=True, cao=40)
r += 1

for i, h in enumerate(["Biến có biên", "Biên", "Vị trí trong mã nguồn",
                       "Ràng buộc trong SRS", "Trạng thái", "Lớp kiểm thử",
                       "Nhận định"], 1):
    o(r, i, h, bold=True, fill=C_HDR, color=C_TXT, center=True)
ws.row_dimensions[r].height = 28
r += 1

dem = {DA: 0, MOI: 0, KHONG: 0}
for bien, gia_tri, vi_tri, srs, trang_thai, lop_test, ly_do in UNG_VIEN:
    dem[trang_thai] += 1
    o(r, 1, bien, bold=True)
    o(r, 2, gia_tri, center=True)
    o(r, 3, vi_tri, size=9)
    o(r, 4, srs, center=True, size=9)
    o(r, 5, trang_thai, center=True, fill=MAU[trang_thai], bold=True, size=9)
    o(r, 6, lop_test, size=9)
    o(r, 7, ly_do, size=9)
    ws.row_dimensions[r].height = max(34, 11 * (len(ly_do) // 62 + 1))
    r += 1

o(r, 1, "TỔNG", bold=True, fill=C_SEC)
o(r, 2, f"{len(UNG_VIEN)} biên", bold=True, fill=C_SEC, center=True)
for c in (3, 4, 6, 7):
    o(r, c, "", fill=C_SEC)
o(r, 5, f"{dem[DA]} đã áp · {dem[MOI]} mới · {dem[KHONG]} không áp",
  bold=True, fill=C_SEC, center=True, size=9)
r += 2

r = thanh(r, f"KẾT LUẬN: quét được {len(UNG_VIEN)} biên trong toàn hệ thống. "
             f"{dem[DA]} biên đã có BVA từ trước, {dem[MOI]} biên vừa bổ sung sau đợt rà "
             f"soát này, {dem[KHONG]} biên cố ý không áp và đã ghi rõ lý do ở cột cuối.",
           fill=C_DA, color="375623", size=10, cao=28)

r = thanh(r, "BA LÝ DO KHIẾN MỘT BIÊN KHÔNG ĐÁNG ÁP BVA — dùng để trả lời khi bị hỏi:",
           fill=None, color=C_HDR, size=10, cao=20)
for i, (tieu_de, noi_dung) in enumerate([
    ("Biên CẮT chứ không phải biên CHẤP NHẬN",
     "Vượt biên không bị từ chối mà chỉ bị rút gọn (tiêu đề chat 60 ký tự, ngữ cảnh "
     "README 3.000 ký tự). Không có hành vi nào lật ở biên nên không có lỗi lệch một "
     "đơn vị để bắt. Ngoại lệ là mô tả tin tuyển dụng: nó đụng trần cột CSDL nên vượt "
     "biên là hỏng thật — vì vậy dòng đó VẪN được áp BVA."),
    ("Miền phân biệt bằng LOGIC chứ không bằng độ lớn",
     "Tầng node chỉ có ba lớp hành vi (≤1, =2, ≥3) và khác nhau ở điều kiện mở khoá chứ "
     "không ở chỗ 'gần biên'. Bảng tổ hợp điều kiện MC/DC mô tả đúng bản chất hơn."),
    ("Mã nguồn chưa cưỡng chế ràng buộc",
     "SRS nêu 'mỗi node ≥ 2 tài liệu' và 'URL ≤ 500 ký tự' nhưng mã nguồn không kiểm, "
     "nên chưa tồn tại biên để kiểm thử. Đây là khoảng trống HIỆN THỰC, không phải "
     "khoảng trống kiểm thử — đã ghi nhận để xử lý sau."),
], 1):
    o(r, 1, f"{i}. {tieu_de}", bold=True, size=9)
    ws.merge_cells(start_row=r, start_column=1, end_row=r, end_column=2)
    o(r, 3, noi_dung, size=9)
    ws.merge_cells(start_row=r, start_column=3, end_row=r, end_column=7)
    ws.row_dimensions[r].height = max(40, 11 * (len(noi_dung) // 120 + 1))
    r += 1

wb.save(XLSX)
print(f"Da sinh sheet '{SHEET}'")
print(f"  {len(UNG_VIEN)} bien: {dem[DA]} da ap, {dem[MOI]} moi bo sung, {dem[KHONG]} khong ap")

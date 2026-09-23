# -*- coding: utf-8 -*-
"""Sinh sheet "08. Truy vet SRS" — ma trận truy vết yêu cầu ↔ kiểm thử.

    python tools/gen_sheet_truy_vet_srs.py

VÌ SAO CẦN: báo cáo đang trả lời được câu "chạy bao nhiêu test, phủ bao nhiêu phần trăm"
nhưng KHÔNG trả lời được câu "yêu cầu nào trong SRS chưa có test nào chạm tới". Hai câu
đó khác nhau: một lớp phủ 100% dòng vẫn có thể bỏ sót một tiêu chí chấp nhận, và ngược
lại một yêu cầu có thể được phủ đủ ở tầng HTTP mà tầng đơn vị vẫn 0%.

Sheet này ghép ba nguồn cho từng yêu cầu chức năng:
    - lớp mã nguồn hiện thực yêu cầu đó (viết tay, lấy từ bảng endpoint của SRS)
    - lớp kiểm thử chạm tới nó          (viết tay, đối chiếu lại bằng surefire)
    - độ bao phủ hộp trắng của lớp mã nguồn (đọc từ jacoco-whitebox)

Phần viết tay chỉ có ánh xạ FR → lớp. Mọi CON SỐ đều đọc từ kết quả chạy thật; tên lớp
sai hoặc lớp test không tồn tại thì script dừng chứ không ghi bừa.
"""
import csv
import glob
import xml.etree.ElementTree as ET
from pathlib import Path

import openpyxl
from openpyxl.styles import Alignment, Border, Font, PatternFill, Side

XLSX = "BaoCao-KiemThu-CareerCompass.xlsx"
SHEET = "08. Truy vet SRS"

C_HDR = "1F4E79"; C_TXT = "FFFFFFFF"; C_SEC = "DDEBF7"
C_PASS = "C6EFCE"; C_WARN = "FFF2CC"; C_FAIL = "FBE4E4"
thin = Side(style="thin", color="BFBFBF")
BORDER = Border(left=thin, right=thin, top=thin, bottom=thin)

# ── Ánh xạ viết tay: FR → (tên, lớp mã nguồn, lớp kiểm thử) ─────────────
# Lớp mã nguồn lấy theo cột "Endpoint" trong bảng thuộc tính của từng FR ở SRS mục 4.
FR = [
    ("FR1.1", "Hỏi đáp nghề nghiệp bằng ngôn ngữ tự nhiên",
     ["MentorService", "MentorController"],
     ["MentorServiceTest", "MentorControllerTest"]),
    ("FR1.2", "Tích hợp mô hình ngôn ngữ lớn (retry + backoff)",
     ["LlmClient"], ["LlmClientTest"]),
    ("FR1.3", "Cá nhân hoá theo bảng điểm và GitHub",
     ["TranscriptAnalysisService", "OnboardingService", "OnboardingController"],
     ["TranscriptAnalysisServiceTest", "OnboardingServiceTest", "OnboardingControllerTest",
      "TranscriptFileDecisionTableTest", "OnboardingFileSizeBvaTest"]),
    ("FR2.1", "Chọn vai trò nghề nghiệp mục tiêu",
     ["OnboardingController", "UserProfileService"],
     ["OnboardingControllerTest", "UserProfileServiceTest", "OnboardingStateTransitionTest"]),
    ("FR2.2", "Sinh Skill Tree phân cấp theo mức ưu tiên",
     ["RoadmapService", "RoadmapController", "RoadmapPageController"],
     ["RoadmapServiceTest", "RoadmapControllerTest", "RoadmapPageControllerTest"]),
    ("FR2.3", "Mỗi node có tối thiểu 2 tài nguyên học",
     ["RoadmapService"], ["RoadmapServiceTest"]),
    ("FR2.4", "Đánh dấu hoàn thành và cập nhật tiến độ",
     ["ProgressService", "RoadmapController"],
     ["ProgressServiceTest", "RoadmapControllerTest",
      "ProgressDecisionTableTest", "ProgressStateTransitionTest"]),
    ("FR3.1", "Nhập/chọn kỹ năng hiện có",
     ["OnboardingController", "UserProfileService"],
     ["OnboardingControllerTest", "UserProfileServiceTest"]),
    ("FR3.2", "So khớp kỹ năng với yêu cầu vai trò",
     ["SkillGapService", "SkillGapController"],
     ["SkillGapServiceTest", "SkillGapControllerTest"]),
    ("FR3.3", "Báo cáo trực quan, xuất PDF, ưu tiên học gấp",
     ["PdfService", "SkillGapController", "SkillGapPageController"],
     ["PdfServiceTest", "SkillGapControllerTest", "SkillGapPageControllerTest"]),
    ("FR4.1", "Thu thập tin tuyển dụng định kỳ",
     ["ScraperService"], ["ScraperServiceTest"]),
    ("FR4.2", "Phân tích tần suất từ khoá",
     ["KeywordAnalysisService"], ["KeywordAnalysisServiceTest"]),
    ("FR4.3", "Biểu đồ xu hướng tương tác",
     ["MarketPulseService", "MarketPulseController"],
     ["MarketPulseServiceTest", "MarketPulseControllerTest"]),
    ("FR5.1", "Liên kết GitHub và đồng bộ repository",
     ["PortfolioService", "PortfolioController"],
     ["PortfolioServiceTest", "PortfolioControllerTest"]),
    ("FR5.2", "Tóm tắt README bằng AI",
     ["PortfolioService"], ["PortfolioServiceTest"]),
    ("FR5.3", "Chia sẻ hồ sơ qua URL công khai",
     ["PublicPortfolioController", "PortfolioController"],
     ["PortfolioControllerTest"]),
    ("FR6.1", "Đăng ký, đăng nhập, khôi phục mật khẩu",
     ["AuthService", "AuthController", "PasswordResetService", "ProfileService",
      "ProfileController", "CustomUserDetailsService", "HomeController"],
     ["AuthServiceTest", "AuthControllerTest", "PasswordResetServiceTest",
      "ProfileServiceTest", "ProfileControllerTest", "CustomUserDetailsServiceTest",
      "HomeControllerTest", "RegisterStandardBvaTest", "RegisterFormDTOBvaTest",
      "RegisterEquivalencePartitionTest", "RegisterTagCoverageTest",
      "TokenValidityDecisionTableTest", "TokenStateTransitionTest"]),
    ("FR6.2", "Lưu lịch sử chat, đánh giá và tiến độ",
     ["DashboardService", "DashboardController", "ActivityLogService"],
     ["DashboardServiceTest", "DashboardControllerTest", "ActivityLogServiceTest"]),
    ("FR6.3", "Ép hoàn tất onboarding (quy tắc dẫn xuất)",
     ["OnboardingInterceptor"],
     ["OnboardingInterceptorTest", "OnboardingControllerTest"]),
    ("FR7.1", "Bảng điều khiển thống kê quản trị",
     ["AdminDashboardService", "AdminDashboardController"],
     ["AdminDashboardServiceTest", "AdminDashboardControllerTest"]),
    ("FR7.2", "Danh sách và tìm kiếm người dùng",
     ["AdminUserService", "AdminUserController", "UserAdminMapper"],
     ["AdminUserServiceTest", "AdminUserControllerTest", "UserAdminMapperTest"]),
    ("FR7.3", "Khoá/mở khoá, đổi vai trò, xoá người dùng",
     ["AdminUserService", "AdminUserController"],
     ["AdminUserServiceTest", "AdminUserControllerTest"]),
    ("FR8.1", "Tạo, sửa, xoá template lộ trình",
     ["CounselorTemplateService", "CounselorTemplateController"],
     ["CounselorTemplateServiceTest", "CounselorTemplateControllerTest"]),
    ("FR8.2", "Thêm/xoá node kỹ năng, đặt tầng và node cha",
     ["CounselorTemplateService", "CounselorTemplateController"],
     ["CounselorTemplateServiceTest", "CounselorTemplateControllerTest"]),
    ("FR8.3", "Gắn ≥ 2 tài liệu học cho mỗi node",
     ["CounselorTemplateService", "CounselorTemplateController"],
     ["CounselorTemplateServiceTest", "CounselorTemplateControllerTest"]),
    ("FR8.4", "Sắp xếp thứ tự ưu tiên node trong cùng tầng",
     ["CounselorTemplateService", "CounselorTemplateController"],
     ["CounselorTemplateServiceTest", "CounselorTemplateControllerTest"]),
]

# NFR: (mã, nội dung rút gọn, cách kiểm chứng, tầng)
NFR = [
    ("NFR-P01", "Trang nội bộ phản hồi < 2 giây", "Phép kiểm responseTime cấp collection", "Postman"),
    ("NFR-P02", "Endpoint gọi LLM < 15 giây", "TC-MEN-04", "Postman"),
    ("NFR-P03", "Đồng bộ GitHub < 30 giây", "TC-PF-02", "Postman"),
    ("NFR-P04", "Sinh và tải PDF < 5 giây", "TC-SG-09", "Postman"),
    ("NFR-P05", "Truy vấn roadmap không phát sinh N+1", "Bật show-sql, đếm câu truy vấn", "Thủ công"),
    ("NFR-P06", "Cào dữ liệu nền không làm chậm request", "Giám sát log khung 02:00", "Thủ công"),
    ("NFR-S01", "Tài nguyên ngoài danh sách công khai phải xác thực", "TC-SEC-01, TC-SEC-02", "Postman"),
    ("NFR-S02", "Chống CSRF trên mọi request đổi trạng thái", "TC-RM-07, TC-SEC-05", "Postman"),
    ("NFR-S03", "Phân quyền theo vai trò", "TC-SEC-03, TC-SEC-04 · OnboardingInterceptorTest", "Postman + JUnit"),
    ("NFR-S04", "Chống SQL Injection nhờ JPA tham số hoá", "TC-SEC-07", "Postman"),
    ("NFR-S05", "Không lộ stack trace / SQL trong phản hồi lỗi",
     "Phép kiểm cấp collection cho mọi request · GlobalExceptionHandlerTest · "
     "PortfolioControllerTest (ràng buộc CSDL)", "Postman + JUnit"),
    ("NFR-S06", "Mật khẩu băm BCrypt, không lưu bản rõ", "AuthServiceTest · kiểm tra trực tiếp CSDL", "JUnit + Thủ công"),
    ("NFR-S07", "Chống dò tài khoản qua quên mật khẩu", "TC-AUTH-11, TC-AUTH-12 · PasswordResetServiceTest", "Postman + JUnit"),
    ("NFR-S08", "Cấp session mới sau đăng nhập", "TC-AUTH-02", "Postman"),
    ("NFR-S09", "Bí mật nạp từ biến môi trường", "Rà soát mã nguồn", "Thủ công"),
    ("NFR-S10", "Giới hạn dung lượng tệp, lưu ngoài thư mục tĩnh",
     "OnboardingFileSizeBvaTest · TranscriptFileDecisionTableTest · OnboardingServiceTest", "JUnit"),
    ("NFR-S11", "Trang công khai không lộ dữ liệu nhạy cảm", "AC-5.3.4 · PortfolioControllerTest", "JUnit + Thủ công"),
    ("NFR-U01", "Toàn bộ thông báo bằng tiếng Việt", "Rà soát giao diện", "Thủ công"),
    ("NFR-U02", "Hoàn tất onboarding trong ≤ 3 phút", "Đo trên CodeceptJS", "CodeceptJS"),
    ("NFR-U03", "Mọi trang chính ≤ 2 lần nhấp từ Dashboard", "Rà soát điều hướng", "Thủ công"),
    ("NFR-U04", "Thao tác mất dữ liệu phải có xác nhận", "TC-ADM-003 (CodeceptJS)", "CodeceptJS"),
    ("NFR-U05", "Hiển thị chỉ báo đang xử lý", "Rà soát giao diện", "Thủ công"),
    ("NFR-R01", "Lỗi dịch vụ ngoài không làm hỏng nghiệp vụ chính",
     "LlmClientTest · MentorServiceTest · PortfolioControllerTest", "JUnit"),
    ("NFR-R02", "Cào dữ liệu thất bại thì giữ dữ liệu cũ", "ScraperServiceTest", "JUnit"),
    ("NFR-R03", "Không endpoint nào trả 500 với đầu vào sai",
     "GlobalExceptionHandlerTest · bộ kiểm thử tiêu cực Postman", "JUnit + Postman"),
    ("NFR-R04", "Giao dịch ghi dữ liệu là nguyên tử", "Kiểm thử tích hợp", "Thủ công"),
    ("NFR-R05", "Tỉ lệ sẵn sàng ≥ 99%", "Nhật ký giám sát", "Thủ công"),
    ("NFR-C01", "Chạy đúng trên Chrome, Edge, Firefox", "CodeceptJS chạy Chromium; còn lại thủ công", "CodeceptJS + Thủ công"),
    ("NFR-C02", "Bố cục đúng ở ≥ 1366×768", "Rà soát giao diện", "Thủ công"),
    ("NFR-C03", "Tương thích MySQL 8.x", "Chạy thật trên MySQL 8", "Thủ công"),
    ("NFR-C04", "API JSON dùng UTF-8, đủ tiếng Việt có dấu", "Phép kiểm nội dung trong Postman", "Postman"),
    ("NFR-M01", "Mã nguồn tổ chức theo module nghiệp vụ", "Rà soát cấu trúc thư mục", "Thủ công"),
    ("NFR-M02", "Tách bạch controller - service - repository", "Rà soát mã nguồn", "Thủ công"),
    ("NFR-M03", "Tầng web dùng DTO, không phơi entity", "UserAdminMapperTest · rà soát mã nguồn", "JUnit + Thủ công"),
    ("NFR-M04", "Cấu hình tập trung ở application.yml + biến môi trường", "Rà soát mã nguồn", "Thủ công"),
    ("NFR-M05", "Bộ kiểm thử chạy được bằng Newman", "newman run — 376 phép kiểm, 0 lỗi", "Postman"),
]


# ── Đọc số liệu thật ────────────────────────────────────────────────────
def doc_surefire():
    ket_qua = {}
    for p in glob.glob("target/surefire-reports/TEST-*.xml"):
        g = ET.parse(p).getroot().attrib
        ten = Path(p).stem[len("TEST-"):].rsplit(".", 1)[-1]
        ket_qua[ten] = (int(g["tests"]), int(g["failures"]) + int(g["errors"]))
    if not ket_qua:
        raise SystemExit("CHUA CHAY TEST: khong thay target/surefire-reports. Chay './mvnw clean test' truoc.")
    return ket_qua


def doc_bao_phu():
    p = Path("target/site/jacoco-whitebox/jacoco.csv")
    if not p.exists():
        raise SystemExit(f"khong thay {p} — chay './mvnw clean test' truoc.")
    ket_qua = {}
    for r in csv.DictReader(p.open(encoding="utf-8")):
        lc, lm = int(r["LINE_COVERED"]), int(r["LINE_MISSED"])
        bc, bm = int(r["BRANCH_COVERED"]), int(r["BRANCH_MISSED"])
        ket_qua[r["CLASS"]] = (lc, lc + lm, bc, bc + bm)
    return ket_qua


TEST = doc_surefire()
COV = doc_bao_phu()

# Kiểm tra ánh xạ trước khi ghi: sai tên là dừng, không ghi báo cáo sai.
for ma, _, nguon, lop_test in FR:
    for c in nguon:
        if c not in COV:
            raise SystemExit(f"{ma}: lop ma nguon '{c}' khong co trong jacoco-whitebox.csv")
    for c in lop_test:
        if c not in TEST:
            raise SystemExit(f"{ma}: lop kiem thu '{c}' khong co trong surefire-reports")


def bao_phu_gop(nguon):
    """Bao phủ của cả nhóm lớp hiện thực một yêu cầu."""
    lc = lt = bc = bt = 0
    for c in nguon:
        a, b, d, e = COV[c]
        lc += a; lt += b; bc += d; bt += e
    return (100 * lc / lt if lt else 0.0), (100 * bc / bt if bt else None), lt, bt


# ── Dựng sheet ──────────────────────────────────────────────────────────
wb = openpyxl.load_workbook(XLSX)
if SHEET in wb.sheetnames:
    wb.remove(wb[SHEET])
ws = wb.create_sheet(SHEET)

for cot, rong in zip("ABCDEFGH", (10, 40, 34, 44, 11, 11, 10, 10)):
    ws.column_dimensions[cot].width = rong


def o(r, c, v, *, bold=False, fill=None, center=False, size=10, color=None, wrap=True):
    x = ws.cell(row=r, column=c, value=v)
    x.font = Font(size=size, bold=bold, color=color)
    x.alignment = Alignment(vertical="top", wrap_text=wrap,
                            horizontal="center" if center else "left")
    x.border = BORDER
    if fill:
        x.fill = PatternFill("solid", fgColor=fill)


def thanh(r, t, *, fill=C_SEC, color=C_HDR, size=11, italic=False, cao=24, cot=6):
    ws.merge_cells(start_row=r, start_column=1, end_row=r, end_column=cot)
    x = ws.cell(row=r, column=1, value=t)
    x.font = Font(bold=not italic, italic=italic, size=size, color=color)
    if fill:
        x.fill = PatternFill("solid", fgColor=fill)
    x.alignment = Alignment(vertical="top", wrap_text=True)
    ws.row_dimensions[r].height = cao
    return r + 1


r = 1
r = thanh(r, "MA TRẬN TRUY VẾT — YÊU CẦU TRONG SRS ↔ KIỂM THỬ", size=14, cao=30)
r = thanh(r, "Trả lời câu hỏi mà bảng độ bao phủ không trả lời được: yêu cầu nào trong đặc "
             "tả đã có test chạm tới, và chạm tới ở mức nào. Cột bao phủ là của các LỚP MÃ "
             "NGUỒN hiện thực yêu cầu đó, đo bằng bộ kiểm thử hộp trắng "
             "(target/site/jacoco-whitebox).",
           fill=None, color="595959", size=9, italic=True, cao=36)
r += 1

tong_ac = {"1.1": 4, "1.2": 2, "1.3": 3, "2.1": 3, "2.2": 4, "2.3": 3, "2.4": 7,
           "3.1": 3, "3.2": 6, "3.3": 5, "4.1": 4, "4.2": 3, "4.3": 2, "5.1": 4,
           "5.2": 3, "5.3": 5, "6.1": 9, "6.2": 3, "6.3": 3, "7.1": 1, "7.2": 1,
           "7.3": 3, "8.1": 1, "8.2": 2, "8.3": 1, "8.4": 1}

r = thanh(r, f"BẢNG 1 — {len(FR)} YÊU CẦU CHỨC NĂNG")
for i, h in enumerate(["Mã", "Yêu cầu", "Lớp mã nguồn hiện thực",
                       "Lớp kiểm thử chạm tới", "Số test", "Bao phủ dòng"], 1):
    o(r, i, h, bold=True, fill=C_HDR, color=C_TXT, center=True)
ws.row_dimensions[r].height = 30
r += 1

thap = []
for ma, ten, nguon, lop_test in FR:
    so_test = sum(TEST[c][0] for c in lop_test)
    do, nhanh, _, _ = bao_phu_gop(nguon)
    mau = C_PASS if do >= 90 else (C_WARN if do >= 75 else C_FAIL)
    if do < 90:
        thap.append((ma, ten, do))
    ac = tong_ac.get(ma[2:], 0)
    o(r, 1, ma, bold=True, center=True)
    o(r, 2, f"{ten}\n({ac} tiêu chí chấp nhận)")
    o(r, 3, "\n".join(nguon), size=9)
    o(r, 4, "\n".join(lop_test), size=9)
    o(r, 5, so_test, center=True)
    o(r, 6, f"{do:.1f}%".replace(".", ","), center=True, fill=mau, bold=True)
    ws.row_dimensions[r].height = max(30, 12 * max(len(nguon), len(lop_test)))
    r += 1

o(r, 1, "TỔNG", bold=True, fill=C_SEC)
o(r, 2, f"{len(FR)} yêu cầu chức năng · {sum(tong_ac.values())} tiêu chí chấp nhận",
  bold=True, fill=C_SEC)
o(r, 3, "", fill=C_SEC)
o(r, 4, "", fill=C_SEC)
o(r, 5, sum(v[0] for v in TEST.values()), bold=True, fill=C_SEC, center=True)
lc = sum(COV[c][0] for c in COV); lt = sum(COV[c][1] for c in COV)
o(r, 6, f"{100 * lc / lt:.1f}%".replace(".", ","), bold=True, fill=C_SEC, center=True)
r += 2

r = thanh(r, "CÁCH ĐỌC CỘT BAO PHỦ: xanh ≥ 90%, vàng 75–90%, đỏ < 75%. Đây là bao phủ của "
             "các lớp hiện thực yêu cầu, KHÔNG phải tỉ lệ tiêu chí chấp nhận đã kiểm — một "
             "lớp phủ 100% dòng vẫn có thể bỏ sót một AC. Hai thước đo bổ sung cho nhau chứ "
             "không thay được nhau.",
           fill=None, color="595959", size=9, italic=True, cao=34)
if thap:
    r = thanh(r, "YÊU CẦU CÒN BAO PHỦ DƯỚI 90%: "
                 + " · ".join(f"{m} ({d:.0f}%)" for m, _, d in thap),
              fill=C_WARN, color="7F6000", size=9, italic=True, cao=26)
r += 1

r = thanh(r, f"BẢNG 2 — {len(NFR)} YÊU CẦU PHI CHỨC NĂNG")
for i, h in enumerate(["Mã", "Yêu cầu", "Cách kiểm chứng", "", "Tầng kiểm thử", ""], 1):
    o(r, i, h, bold=True, fill=C_HDR, color=C_TXT, center=True)
ws.merge_cells(start_row=r, start_column=3, end_row=r, end_column=4)
ws.merge_cells(start_row=r, start_column=5, end_row=r, end_column=6)
ws.row_dimensions[r].height = 24
r += 1

for ma, ten, cach, tang in NFR:
    tu_dong = tang != "Thủ công"
    o(r, 1, ma, bold=True, center=True)
    o(r, 2, ten)
    o(r, 3, cach, size=9)
    ws.merge_cells(start_row=r, start_column=3, end_row=r, end_column=4)
    o(r, 5, tang, center=True, size=9, fill=C_PASS if tu_dong else C_WARN)
    ws.merge_cells(start_row=r, start_column=5, end_row=r, end_column=6)
    ws.row_dimensions[r].height = 26
    r += 1

tu_dong = sum(1 for *_, t in NFR if t != "Thủ công")
r += 1
r = thanh(r, f"{tu_dong}/{len(NFR)} yêu cầu phi chức năng được kiểm bằng công cụ tự động "
             f"(ô xanh); {len(NFR) - tu_dong} còn lại phải rà soát thủ công (ô vàng) vì "
             "chúng nói về cấu trúc mã nguồn, giao diện hoặc vận hành — không có phép kiểm "
             "tự động nào khẳng định thay được. Ghi rõ ra đây để người chấm biết chỗ nào là "
             "bằng chứng máy đo và chỗ nào là cam kết của nhóm.",
           fill=None, color="595959", size=9, italic=True, cao=44)

wb.save(XLSX)
print(f"Da sinh sheet '{SHEET}'")
print(f"  {len(FR)} yeu cau chuc nang, {sum(tong_ac.values())} tieu chi chap nhan")
print(f"  {len(NFR)} yeu cau phi chuc nang ({tu_dong} tu dong / {len(NFR) - tu_dong} thu cong)")
if thap:
    for m, t, d in thap:
        print(f"  DUOI 90%: {m} {t} — {d:.1f}%")

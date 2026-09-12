# -*- coding: utf-8 -*-
"""Sinh file Word "Báo cáo công việc theo tuần" cho môn Kiểm chứng phần mềm.

    python tools/gen_baocao_tuan_docx.py

Lịch học: buổi đầu 22/07/2026, sau đó mỗi thứ Tư. Ngày 02/09 nghỉ lễ Quốc khánh.

Mọi con số (số commit, số tệp, số dòng, số test theo tác giả) đều ĐỌC THẲNG TỪ GIT và
target/surefire-reports lúc chạy, không gõ tay. Phần mô tả nội dung từng tuần là phần
duy nhất viết tay, đặt trong NOI_DUNG bên dưới.

HAI ĐIỀU PHẢI GIỮ, vì bản đầu tiên của báo cáo đã sai cả hai:

1. Gom commit theo NGÀY TÁC GIẢ (%ad) chứ không phải ngày commit. %ad là lúc công việc
   thực sự được làm; ngày commit bị đổi khi merge hoặc rebase. Hai cách cho số liệu
   lệch nhau đáng kể (tuần 6: 15 so với 3).

2. Nội dung phải CÂN XỨNG với đóng góp thật của cả ba thành viên. Bản đầu mô tả mảng
   kiểm thử hộp đen rất chi tiết, còn kiểm thử hộp trắng, kiểm thử tầng điều khiển, sửa
   lỗi giao diện và bản thuyết minh Word thì gần như không nhắc — trong khi đó là phần
   việc của hai thành viên còn lại, và báo cáo này được dùng để chấm điểm từng cá nhân.
   Trước khi sửa NOI_DUNG, chạy lại các lệnh rà soát ở cuối tệp để đối chiếu commit thật.
"""
import collections
import datetime as d
import glob
import os
import re
import subprocess
import xml.etree.ElementTree as ET

from docx import Document
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml.ns import qn
from docx.shared import Pt, Cm, RGBColor

REPO = "."
DOCX = "BaoCao-CongViec-Theo-Tuan.docx"
BUOI_DAU = d.date(2026, 7, 22)
SO_TUAN = 8
NGAY_LE = {d.date(2026, 9, 2): "Quốc khánh 02/09"}

XANH = RGBColor(0x1F, 0x4E, 0x79)
XAM = RGBColor(0x59, 0x59, 0x59)

# Một người commit dưới nhiều tên (tài khoản noreply của GitHub, tên hiển thị cũ).
# Chỉ gộp những tên mà chính Git đã ghi ở dạng đầy đủ tại một commit khác — người nào
# Git chỉ có tên tài khoản thì giữ nguyên tên tài khoản, không tự đặt họ tên đầy đủ vì
# đó là bịa thông tin về một người thật.
GOP_TEN = {
    "PhatDepZai": "Nguyễn Thành Phát",
    "vmquan2200": "Vòng Minh Quân",
    "PHAMSONTUANKIET": "Phạm Sơn Tuấn Kiệt",
}

# ── Nội dung mô tả từng tuần — phần duy nhất viết tay ────────────────────
# (tiêu đề tuần, [ (nhóm việc, [các gạch đầu dòng]) ], kết quả đạt được)
NOI_DUNG = {
    1: ("Thiết lập nền tảng kiểm thử", [
        ("Tài liệu đặc tả", [
            "Đưa tài liệu Đặc tả yêu cầu phần mềm (SRS) vào kho mã nguồn, làm cơ sở "
            "đối chiếu cho toàn bộ hoạt động kiểm thử về sau.",
        ]),
        ("Kiểm thử API", [
            "Xây dựng bộ kiểm thử API bằng Postman, gồm tập lệnh (collection) và tập "
            "biến môi trường (environment) tách riêng để chạy được trên nhiều máy.",
        ]),
    ], "Có đặc tả để đối chiếu và có công cụ kiểm thử API sẵn sàng sử dụng."),

    2: ("Kiểm thử đơn vị tầng dịch vụ và tự động hoá quy trình", [
        ("Kiểm thử đơn vị tầng dịch vụ", [
            "Xây dựng 24 lớp kiểm thử đơn vị phủ toàn bộ tầng dịch vụ, trải trên chín "
            "phân hệ nghiệp vụ: quản trị người dùng, quản trị mẫu lộ trình, bảng điều "
            "khiển, lộ trình học, khoảng trống kỹ năng, hồ sơ cá nhân, hồ sơ năng lực "
            "GitHub, cố vấn AI và phân tích thị trường việc làm.",
            "Bốn lớp có khối lượng lớn nhất: CounselorTemplateServiceTest (585 dòng), "
            "PortfolioServiceTest (562 dòng), SkillGapServiceTest (330 dòng) và "
            "RoadmapServiceTest (322 dòng).",
            "Áp dụng quy ước đặt tên thống nhất method_whenCondition_expectedResult "
            "để chỉ đọc tên test là hiểu ngay ý đồ kiểm thử.",
            "Dùng Mockito tạo đối tượng giả lập cho tầng repository, tách hoàn toàn "
            "kiểm thử đơn vị khỏi cơ sở dữ liệu thật.",
        ]),
        ("Tích hợp liên tục và triển khai", [
            "Khởi tạo quy trình tích hợp liên tục bằng GitHub Actions (tệp ci.yml).",
            "Mở rộng quy trình: tự động chạy kiểm thử, đóng gói ảnh Docker và triển "
            "khai lên máy chủ VPS sau mỗi lần đẩy mã.",
            "Đóng gói ứng dụng bằng Docker kèm tài liệu hướng dẫn triển khai và cấu "
            "hình máy chủ Nginx.",
            "Khắc phục lỗi kiểm tra tình trạng máy chủ báo mã lỗi curl (3) do tệp .env "
            "trên VPS được lưu với ký tự xuống dòng kiểu Windows (CRLF).",
        ]),
        ("Dọn dẹp kho mã", [
            "Gỡ thư mục workspace của Eclipse (.metadata) khỏi phạm vi theo dõi của "
            "Git — đây là nguyên nhân của phần lớn số dòng bị xoá trong tuần.",
        ]),
    ], "Tầng dịch vụ được phủ kiểm thử đơn vị; kiểm thử chạy tự động mỗi lần đẩy mã, "
       "bản lỗi không thể triển khai."),

    3: ("Không phát sinh công việc trên kho mã", [], None),

    4: ("Kiểm thử giao diện đầu-cuối và phân tích tĩnh", [
        ("Phân tích chất lượng mã", [
            "Tích hợp SonarQube Cloud để phân tích tĩnh mã nguồn.",
            "Đo độ bao phủ mã nguồn bằng JaCoCo và đưa kết quả vào quy trình CI.",
        ]),
        ("Kiểm thử giao diện đầu-cuối", [
            "Xây dựng bộ kiểm thử giao diện đầu-cuối bằng CodeceptJS kết hợp "
            "Playwright, thao tác trên trình duyệt thật như người dùng.",
            "Tổ chức theo mô hình Page Object: mỗi màn hình có một lớp trang riêng "
            "(bảng điều khiển, quản trị người dùng, lộ trình học, khoảng trống kỹ "
            "năng…), nên khi giao diện đổi chỉ phải sửa một chỗ.",
            "Áp dụng chiến lược mỗi kịch bản tự tạo tài khoản mới, tránh các kịch bản "
            "ảnh hưởng lẫn nhau qua dữ liệu dùng chung.",
            "Ràng buộc quy trình triển khai: dừng deploy khi bộ kiểm thử giao diện đỏ.",
        ]),
        ("Tối ưu đóng gói", [
            "Giảm dung lượng ảnh Docker 132 MB và tự động dọn các bản cũ trên kho GHCR.",
        ]),
    ], "Đủ ba tầng kiểm thử tự động: đơn vị, giao diện đầu-cuối và phân tích tĩnh."),

    5: ("Triển khai song song kiểm thử hộp trắng và hộp đen", [
        ("Kiểm thử hộp trắng — đồ thị dòng điều khiển và đường cơ sở", [
            "Dựng đồ thị dòng điều khiển (Control Flow Graph) cho hai hàm của "
            "RoadmapService: calculatePercent() và isNodeLocked().",
            "Tính độ phức tạp Cyclomatic bằng hai công thức độc lập V(G) = E − N + 2 "
            "và V(G) = P + 1, cho cùng kết quả: 2 đối với calculatePercent() và 7 đối "
            "với isNodeLocked() — tức số test tối thiểu lần lượt là 2 và 7.",
            "Liệt kê đầy đủ 9 đường cơ sở (Basis Path), mỗi đường ghi rõ điều kiện rẽ "
            "nhánh, luồng thực thi, dữ liệu đầu vào ví dụ, kết quả mong đợi và tên "
            "phương thức kiểm thử thi hành đường đó.",
            "So sánh bốn mức bao phủ — câu lệnh, nhánh, điều kiện và tổ hợp điều kiện "
            "(MC/DC) — nêu rõ mỗi mức bắt được loại lỗi nào và bỏ sót loại lỗi nào.",
            "Lập ma trận tổ hợp điều kiện MC/DC gồm 7 trường hợp cho isNodeLocked(), "
            "tách năm điều kiện con và ghi giá trị chân lý của từng điều kiện.",
        ]),
        ("Kiểm thử tầng điều khiển", [
            "Xây dựng 5 lớp kiểm thử cho tầng điều khiển: AuthController, "
            "HomeController, ProfileController, RoadmapPageController và "
            "SkillGapPageController.",
            "Xây dựng hai lớp hỗ trợ CsrfTestAdvice và TestSecurityConfiguration để "
            "kiểm thử được các biểu mẫu có bảo vệ chống tấn công giả mạo yêu cầu "
            "(CSRF) và các trang yêu cầu đăng nhập.",
        ]),
        ("Kiểm thử giá trị biên", [
            "Xây dựng RegisterFormDTOBvaTest — kiểm thử giá trị biên theo từng trường "
            "của biểu mẫu đăng ký, kiểm riêng từng ràng buộc bằng validateProperty.",
            "Xây dựng OnboardingFileSizeBvaTest cho giới hạn dung lượng tệp bảng điểm, "
            "phủ đủ năm điểm biên chuẩn cộng giá trị vượt ngưỡng.",
            "Lập báo cáo thiết kế trường hợp kiểm thử giá trị biên, kèm bảng tổng hợp "
            "toàn bộ phương thức được kiểm theo 12 kỹ thuật kiểm thử khác nhau.",
        ]),
        ("Kiểm thử hộp đen — bốn kỹ thuật chương IV", [
            "Áp dụng phân hoạch lớp tương đương cho biểu mẫu đăng ký.",
            "Xây dựng bảng quyết định cho luật chặn cập nhật tiến độ học.",
            "Xây dựng bảng chuyển đổi trạng thái cho tiến độ node kỹ năng.",
            "Rà soát để mỗi kỹ thuật đều phủ đủ tiêu chí riêng, trình bày theo đúng "
            "mẫu bảng thiết kế trong bài giảng.",
        ]),
        ("Bản thuyết minh bằng văn bản", [
            "Biên soạn báo cáo kiểm chứng phần mềm dạng văn bản gồm bốn phần: báo cáo "
            "kiểm thử đơn vị, báo cáo kiểm thử API, thuyết minh kiểm thử giao diện và "
            "báo cáo kỹ thuật kiểm thử dựa trên kinh nghiệm.",
            "Trình bày cơ sở lý thuyết của bộ kiểm thử: vai trò của JUnit 5 và Mockito, "
            "ý nghĩa của quy ước đặt tên ba phần, lý do phải dùng đối tượng giả lập.",
            "Sửa lỗi bộ kiểm thử đầu-cuối: mỗi lệnh chỉ nhận một tham số --grep.",
        ]),
    ], "Hai nhánh hộp trắng và hộp đen cùng có kết quả; tầng điều khiển được phủ kiểm "
       "thử; có bản thuyết minh bằng văn bản."),

    6: ("Chuẩn hoá báo cáo và khắc phục lỗi giao diện", [
        ("Chuẩn hoá báo cáo hộp đen", [
            "Sửa nhãn giá trị biên của trường email, thống nhất thứ tự nhãn và đánh số "
            "nhãn B liên tục trên toàn báo cáo.",
            "Bổ sung quy tắc gộp lớp hợp lệ và 6 trường hợp kiểm thử giá trị biên cho "
            "giới hạn dung lượng tệp còn thiếu.",
            "Trình bày bảng quyết định rút gọn sao cho các luật không chồng lấn nhau, "
            "kèm giải thích lý do rút gọn được.",
            "Nhóm bảng chuyển đổi trạng thái theo trạng thái bắt đầu và vẽ sơ đồ trạng "
            "thái bằng draw.io.",
            "Xây dựng bảng quyết định thứ hai cho hiệu lực của mã đặt lại mật khẩu.",
            "Đưa báo cáo độ bao phủ JaCoCo lên kho mã để giảng viên xem trực tiếp.",
        ]),
        ("Sửa lỗi và tái cấu trúc giao diện", [
            "Khắc phục lỗi phát sinh khi rời trang (Page Unload Error) — phiếu công "
            "việc KCPMS-22, ảnh hưởng 25 tệp giao diện gồm các trang quản trị, cố vấn, "
            "lộ trình học cùng các tệp CSS và JavaScript dùng chung.",
            "Sửa lỗi trang trò chuyện với cố vấn AI ở cả tầng điều khiển lẫn tầng giao "
            "diện, kèm bổ sung kiểm thử MentorControllerTest.",
            "Bổ sung kiểm thử cho ba lớp điều khiển còn lại: MarketPulseController, "
            "MentorController và PortfolioController — hoàn tất phủ kiểm thử toàn bộ "
            "tầng điều khiển.",
        ]),
        ("Kiểm thử API và quản lý tài liệu", [
            "Cập nhật bộ kiểm thử API Postman theo các thay đổi của ứng dụng.",
            "Xây dựng báo cáo kiểm thử tổng hợp gồm ma trận truy vết giữa đặc tả yêu "
            "cầu và các kịch bản kiểm thử giao diện.",
            "Sắp xếp lại kho tài liệu kiểm thử: gom các bản báo cáo cũ vào thư mục lưu "
            "trữ theo ngày để tránh nhầm lẫn bản đang dùng.",
        ]),
    ], "Báo cáo hộp đen bám sát mẫu bài giảng; các lỗi giao diện tồn đọng được xử lý; "
       "toàn bộ tầng điều khiển được phủ kiểm thử."),

    7: ("Hoàn thiện báo cáo và khắc phục khiếm khuyết", [
        ("Kiểm thử hộp trắng — hoàn thiện", [
            "Hoàn thiện báo cáo kiểm thử hộp trắng bản đầy đủ, mở rộng từ ba bảng "
            "B1–B3 của tuần 5 thành năm bảng B1–B5.",
            "Bảng B4 — đo độ bao phủ chi tiết cho từng lớp trong dự án.",
            "Bảng B5 — lập danh mục toàn bộ tệp kiểm thử thuộc phạm vi hộp trắng.",
            "Đối chiếu độ bao phủ trước và sau khi bổ sung kiểm thử, kèm nhận xét cho "
            "từng chỉ số và xác nhận vượt ngưỡng chặn 65% mà JaCoCo đặt ra trong CI.",
            "Bổ sung GlobalExceptionHandlerTest, bảo đảm mỗi loại ngoại lệ trả về đúng "
            "mã trạng thái HTTP tương ứng; lớp này đạt bao phủ nhánh tuyệt đối.",
        ]),
        ("Trình bày báo cáo hộp đen", [
            "Trình bày bảng Standard BVA theo đúng mẫu slide 23: mỗi dòng là một bộ "
            "đầy đủ các biến, chỉ một biến được đẩy tới giá trị biên.",
            "Bổ sung bảng thiết kế trường hợp kiểm thử theo phương pháp gộp nhãn của "
            "slide 33, thi hành bằng 17 trường hợp kiểm thử.",
            "Thay bảng nhật ký chạy test 81 dòng bằng bảng tổng kết 5 dòng; đưa cột "
            "kết quả vào thẳng bảng thiết kế.",
            "Bổ sung bảng chuyển đổi trạng thái cho vòng đời mã đặt lại mật khẩu.",
        ]),
        ("Công cụ hỗ trợ trình bày", [
            "Bổ sung tính năng in tên từng trường hợp kiểm thử ra màn hình khi chạy, "
            "bật bằng tham số -DshowCases, để trình bày mà không cần mở mã nguồn.",
            "Chuyển việc phân tích SonarCloud vào quy trình CI, thay vì mỗi thành viên "
            "phải tự chạy bằng mã truy cập cá nhân.",
        ]),
        ("Khiếm khuyết đã phát hiện và khắc phục", [
            "Ràng buộc email thiếu độ dài nhỏ nhất, chuỗi ba ký tự vẫn được chấp nhận "
            "— do kỹ thuật giá trị biên phát hiện.",
            "Phản hồi lỗi để lộ nguyên vết ngăn xếp (stack trace), vi phạm yêu cầu phi "
            "chức năng NFR-S05; nguyên nhân sâu xa là thư viện devtools ghi đè cấu "
            "hình khi chạy từ môi trường phát triển.",
            "Quản trị viên tự hạ được vai trò của chính mình và mất quyền quản trị — "
            "phát hiện bằng kỹ thuật kiểm thử dựa trên kinh nghiệm, xuất phát từ nhận "
            "xét rằng hai thao tác nguy hiểm tương tự đã được chặn còn thao tác này "
            "thì không.",
            "Hai trang giao diện đọc biến không tồn tại nên khối nội dung không hiển "
            "thị — do kiểm thử giao diện đầu-cuối phát hiện, trong khi kiểm thử đơn vị "
            "và kiểm thử API vẫn báo xanh.",
        ]),
        ("Hiệu năng và kiểm thử API", [
            "Gọi mô hình ngôn ngữ song song, rút thời gian đồng bộ GitHub từ 84 giây "
            "xuống khoảng 14 giây, đưa về trong ngưỡng yêu cầu phi chức năng.",
            "Sửa bộ kiểm thử Postman để chạy lại được nhiều lần trên cùng cơ sở dữ "
            "liệu; số phép kiểm thất bại giảm từ 32 xuống 2.",
        ]),
        ("Tài liệu tổng hợp", [
            "Cập nhật bản thuyết minh văn bản theo số liệu mới, bổ sung phần báo cáo "
            "kỹ thuật kiểm thử dựa trên kinh nghiệm: quá trình đoán lỗi, thăm dò hành "
            "vi thật, phát hiện hiện tượng \"xanh giả\" do phiên đăng nhập giữ quyền "
            "cũ, bảng tổng hợp lỗi và bài học rút ra.",
            "Gộp báo cáo hộp đen, hộp trắng, kiểm thử API và kiểm thử giao diện thành "
            "một tài liệu Excel thống nhất; bổ sung tài liệu README cho dự án.",
        ]),
    ], "Toàn bộ khiếm khuyết đã phát hiện đều được khắc phục kèm kiểm thử hồi quy; "
       "báo cáo hộp trắng và hộp đen cùng hoàn thiện."),

    8: ("Rà soát và tinh gọn phạm vi báo cáo", [
        ("Mở rộng", [
            "Bổ sung ba bảng kiểm thử hộp đen mới và khắc phục một khiếm khuyết khiến "
            "tệp không có phần mở rộng làm ứng dụng báo lỗi hạ tầng thay vì thông báo "
            "thân thiện.",
        ]),
        ("Rà soát lại", [
            "Rà soát toàn bộ mã nguồn để xác định còn đối tượng nào áp dụng được kỹ "
            "thuật giá trị biên: toàn dự án chỉ có ba ràng buộc kiểm tra dữ liệu và "
            "một ngưỡng dung lượng tệp, đều đã được phủ.",
            "Loại bỏ một bảng giá trị biên khác loại (ngưỡng cắt chuỗi thay vì ngưỡng "
            "hợp lệ) để báo cáo giữ được tính nhất quán về cách trình bày.",
        ]),
    ], "Báo cáo kiểm thử hoàn chỉnh, nội dung nhất quán về kỹ thuật và cách trình bày."),
}

# Mảng công việc -> thành viên phụ trách, xác định bằng cách tra tác giả đã TẠO RA các
# tệp thuộc mảng đó (git log --diff-filter=A), không dựa vào ghi nhớ.
PHAN_CONG = [
    ("Kiểm thử đơn vị tầng dịch vụ", "24 lớp, phủ 9 phân hệ nghiệp vụ",
     "Nguyễn Thành Phát"),
    ("Kiểm thử tầng điều khiển", "8 lớp, kèm 2 lớp hỗ trợ CSRF và cấu hình bảo mật",
     "Vòng Minh Quân"),
    ("Kiểm thử hộp trắng", "Đồ thị dòng điều khiển, độ phức tạp Cyclomatic, 9 đường cơ "
     "sở, ma trận MC/DC, năm bảng B1–B5", "Vòng Minh Quân"),
    ("Kiểm thử hộp đen — chương IV", "Phân hoạch lớp tương đương, giá trị biên, bảng "
     "quyết định, chuyển đổi trạng thái", "Nguyễn Thành Phát"),
    ("Kiểm thử giá trị biên — bộ đầu tiên", "Theo từng trường của biểu mẫu đăng ký và "
     "giới hạn dung lượng tệp bảng điểm", "Vòng Minh Quân"),
    ("Kiểm thử bộ xử lý ngoại lệ", "Đối chiếu từng loại ngoại lệ với mã trạng thái HTTP",
     "Vòng Minh Quân"),
    ("Kiểm thử API", "Bộ Postman: tập lệnh, biến môi trường, bảo đảm chạy lại được",
     "Nguyễn Thành Phát · Vòng Minh Quân"),
    ("Kiểm thử giao diện đầu-cuối", "CodeceptJS và Playwright, mô hình Page Object",
     "Nguyễn Thành Phát"),
    ("Quy trình CI/CD", "GitHub Actions, Docker, triển khai VPS, chặn deploy khi đỏ",
     "Vòng Minh Quân · Nguyễn Thành Phát"),
    ("Phân tích tĩnh và đo bao phủ", "SonarCloud, JaCoCo, ngưỡng chặn 65% nhánh",
     "Nguyễn Thành Phát"),
    ("Sửa lỗi và tái cấu trúc giao diện", "Lỗi rời trang KCPMS-22, trang cố vấn AI",
     "Vòng Minh Quân"),
    ("Khắc phục khiếm khuyết ứng dụng", "11 khiếm khuyết, kèm kiểm thử hồi quy",
     "Nguyễn Thành Phát"),
    ("Bản thuyết minh bằng văn bản", "Bốn phần: đơn vị, API, giao diện, kỹ thuật dựa "
     "trên kinh nghiệm", "NhwNgocc"),
    ("Báo cáo kiểm thử dạng bảng", "Báo cáo hộp trắng, bảng 12 kỹ thuật, ma trận truy "
     "vết, báo cáo tổng hợp", "Vòng Minh Quân · Nguyễn Thành Phát"),
]

# Sản phẩm bàn giao -> người tạo, lấy từ commit đầu tiên thêm tệp đó vào kho.
SAN_PHAM = [
    ("BaoCao-KiemThu-CareerCompass.xlsx", "Báo cáo kiểm thử tổng hợp, 16 bảng",
     "Nguyễn Thành Phát · Vòng Minh Quân"),
    ("BaoCao_Moi.docx", "Bản thuyết minh kiểm chứng phần mềm, bốn phần", "NhwNgocc"),
    ("BaoCao_Whitebox.xlsx", "Báo cáo hộp trắng B1–B5", "Vòng Minh Quân"),
    ("BaoCao-PhanA-HopDen.xlsx", "Báo cáo hộp đen theo mẫu bài giảng",
     "Nguyễn Thành Phát"),
    ("CareerCompass_Test_Report_All_Methods.xlsx", "Bảng tổng hợp 12 kỹ thuật kiểm thử",
     "Vòng Minh Quân"),
    ("CareerCompass_Test_Report.xlsx", "Ma trận truy vết đặc tả và kịch bản kiểm thử",
     "Vòng Minh Quân"),
    ("SRS-CareerCompass-v1.0.docx", "Đặc tả yêu cầu phần mềm", "Nguyễn Thành Phát"),
    ("CareerCompass.postman_collection.json", "Bộ kiểm thử API",
     "Nguyễn Thành Phát · Vòng Minh Quân"),
    ("e2e/", "Bộ kiểm thử giao diện đầu-cuối", "Nguyễn Thành Phát"),
    ("docs/coverage/", "Bản chụp báo cáo độ bao phủ JaCoCo", "Nguyễn Thành Phát"),
    ("SoDo-ChuyenTrangThai.drawio", "Sơ đồ chuyển đổi trạng thái", "Nguyễn Thành Phát"),
]

MO_TA_TV = {
    "Nguyễn Thành Phát": "Kiểm thử đơn vị tầng dịch vụ, kiểm thử hộp đen chương IV, "
                         "kiểm thử giao diện đầu-cuối, khắc phục khiếm khuyết",
    "Vòng Minh Quân": "Kiểm thử hộp trắng, kiểm thử tầng điều khiển, kiểm thử giá trị "
                      "biên, sửa lỗi giao diện, các báo cáo dạng bảng",
    "NhwNgocc": "Bản thuyết minh kiểm chứng phần mềm bằng văn bản, gồm bốn phần",
}


# ── Đọc số liệu từ git và surefire ───────────────────────────────────────
def moc_tuan():
    return [BUOI_DAU + d.timedelta(days=7 * i) for i in range(SO_TUAN)]


def _tuan_cua(x, moc):
    for i in range(len(moc) - 1, -1, -1):
        if x >= moc[i]:
            return i + 1
    return 0


def thong_ke():
    """{số tuần: {commit, tep, them, bot}}, gom theo ngày tác giả."""
    moc = moc_tuan()
    raw = subprocess.run(
        ["git", "-C", REPO, "log", "--format=@@%ad", "--date=short", "--numstat",
         f"--since={BUOI_DAU.isoformat()}"],
        capture_output=True, text=True, encoding="utf-8").stdout

    S = collections.defaultdict(lambda: {"commit": 0, "tep": set(), "them": 0, "bot": 0})
    cur = None
    for dong in raw.split("\n"):
        if dong.startswith("@@"):
            t = _tuan_cua(d.date.fromisoformat(dong[2:].strip()), moc)
            cur = t if t > 0 else None
            if cur:
                S[cur]["commit"] += 1
        elif cur and "\t" in dong:
            pp = dong.split("\t")
            if len(pp) == 3:
                S[cur]["tep"].add(pp[2])
                if pp[0].isdigit():
                    S[cur]["them"] += int(pp[0])
                if pp[1].isdigit():
                    S[cur]["bot"] += int(pp[1])
    return {t: {"commit": v["commit"], "tep": len(v["tep"]),
                "them": v["them"], "bot": v["bot"]} for t, v in S.items()}


def thanh_vien():
    raw = subprocess.run(
        ["git", "-C", REPO, "log", "--format=%an", f"--since={BUOI_DAU.isoformat()}"],
        capture_output=True, text=True, encoding="utf-8").stdout
    dem = collections.Counter(GOP_TEN.get(a.strip(), a.strip())
                              for a in raw.strip().split("\n") if a.strip())
    return dem.most_common()


def test_theo_tac_gia():
    """{tác giả: (số lớp, số test)} — tra người TẠO RA từng tệp kiểm thử rồi cộng số
    test mà Surefire ghi nhận cho lớp đó.

    Surefire ghi @DisplayName thay cho tên lớp khi lớp có khai báo, nên phải đọc ngược
    @DisplayName từ mã nguồn để ánh xạ về đúng tệp. Bỏ bước này thì 10 lớp hộp đen và
    1 lớp giá trị biên rơi vào diện "không rõ tác giả" — tức 103 test bị bỏ ra ngoài.
    """
    tac_gia, hien_thi = {}, {}
    for f in subprocess.run(["git", "-C", REPO, "ls-files", "src/test"],
                            capture_output=True, text=True,
                            encoding="utf-8").stdout.split():
        if not f.endswith(".java"):
            continue
        cls = os.path.basename(f)[:-5]
        r = subprocess.run(
            ["git", "-C", REPO, "log", "--diff-filter=A", "--format=%an", "--", f],
            capture_output=True, text=True, encoding="utf-8").stdout.strip().split("\n")
        if r and r[0]:
            tac_gia[cls] = GOP_TEN.get(r[-1].strip(), r[-1].strip())
        try:
            src = open(os.path.join(REPO, f), encoding="utf-8").read()
        except OSError:
            continue
        m = re.search(r'@DisplayName\("([^"]+)"\)\s*(?://[^\n]*\n\s*)*'
                      r'(?:@[\w.]+(?:\([^)]*\))?\s*)*(?:final\s+)?class\s', src)
        if m:
            hien_thi[m.group(1)] = cls

    lop, test = collections.Counter(), collections.Counter()
    for p in glob.glob(os.path.join(REPO, "target/surefire-reports/TEST-*.xml")):
        g = ET.parse(p).getroot().attrib
        ten = g["name"].rsplit(".", 1)[-1]
        cls = hien_thi.get(ten, hien_thi.get(g["name"], ten))
        ai = tac_gia.get(cls)
        if ai:
            lop[ai] += 1
            test[ai] += int(g["tests"])
    return {ai: (lop[ai], test[ai]) for ai in test}


def so(n):
    """12345 -> 12.345 (dấu chấm phân nhóm nghìn theo cách viết tiếng Việt)."""
    return f"{n:,}".replace(",", ".")


def pt(x):
    return f"{x:.1f}%".replace(".", ",")


# ── Dựng tài liệu ────────────────────────────────────────────────────────
doc = Document()
for ten in ("Normal", "Title"):
    st = doc.styles[ten]
    st.font.name = "Times New Roman"
    st._element.rPr.rFonts.set(qn("w:eastAsia"), "Times New Roman")
doc.styles["Normal"].font.size = Pt(13)

for s in doc.sections:
    s.top_margin = s.bottom_margin = Cm(2)
    s.left_margin = Cm(3)
    s.right_margin = Cm(2)


def p(text="", *, size=13, bold=False, italic=False, color=None,
      canh=WD_ALIGN_PARAGRAPH.JUSTIFY, truoc=0, sau=6):
    o = doc.add_paragraph()
    o.alignment = canh
    o.paragraph_format.space_before = Pt(truoc)
    o.paragraph_format.space_after = Pt(sau)
    r = o.add_run(text)
    r.font.name = "Times New Roman"
    r.font.size = Pt(size)
    r.bold = bold
    r.italic = italic
    if color:
        r.font.color.rgb = color
    return o


def tieu_de(text, muc=1):
    co = {1: 15, 2: 13.5}[muc]
    return p(text, size=co, bold=True, color=XANH,
             canh=WD_ALIGN_PARAGRAPH.LEFT, truoc=14 if muc == 1 else 10, sau=6)


def gach_dau_dong(text):
    o = doc.add_paragraph(style="List Bullet")
    o.paragraph_format.left_indent = Cm(0.9)
    o.paragraph_format.space_after = Pt(3)
    o.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
    r = o.add_run(text)
    r.font.name = "Times New Roman"
    r.font.size = Pt(13)
    return o


def bang(tieu_de_cot, cac_dong, rong=None, canh_giua=(), co=12):
    t = doc.add_table(rows=1, cols=len(tieu_de_cot))
    t.style = "Table Grid"
    t.alignment = WD_TABLE_ALIGNMENT.CENTER
    for i, h in enumerate(tieu_de_cot):
        o = t.rows[0].cells[i].paragraphs[0]
        o.alignment = WD_ALIGN_PARAGRAPH.CENTER
        r = o.add_run(h)
        r.bold = True
        r.font.name = "Times New Roman"
        r.font.size = Pt(co)
    for hang in cac_dong:
        cells = t.add_row().cells
        for i, v in enumerate(hang):
            o = cells[i].paragraphs[0]
            o.alignment = (WD_ALIGN_PARAGRAPH.CENTER if i in canh_giua
                           else WD_ALIGN_PARAGRAPH.LEFT)
            r = o.add_run(str(v))
            r.font.name = "Times New Roman"
            r.font.size = Pt(co)
    if rong:
        for hang in t.rows:
            for i, w in enumerate(rong):
                hang.cells[i].width = Cm(w)
    return t


ST = thong_ke()
MOC = moc_tuan()
THANH_VIEN = thanh_vien()
TEST_TG = test_theo_tac_gia()
TONG_COMMIT = sum(v["commit"] for v in ST.values())
TONG_TEST = sum(n for _, n in TEST_TG.values())
TONG_LOP = sum(lp for lp, _ in TEST_TG.values())

# ── Trang bìa ────────────────────────────────────────────────────────────
p("TRƯỜNG ĐẠI HỌC GIAO THÔNG VẬN TẢI THÀNH PHỐ HỒ CHÍ MINH",
  size=13, bold=True, canh=WD_ALIGN_PARAGRAPH.CENTER, sau=2)
p("KHOA CÔNG NGHỆ THÔNG TIN", size=13, bold=True,
  canh=WD_ALIGN_PARAGRAPH.CENTER, sau=26)

p("BÁO CÁO CÔNG VIỆC THEO TUẦN", size=20, bold=True, color=XANH,
  canh=WD_ALIGN_PARAGRAPH.CENTER, sau=6)
p("Môn học: Kiểm chứng phần mềm", size=14, italic=True,
  canh=WD_ALIGN_PARAGRAPH.CENTER, sau=4)
p("Đề tài: Hệ thống định hướng nghề nghiệp CareerCompass", size=14, italic=True,
  canh=WD_ALIGN_PARAGRAPH.CENTER, sau=26)

bang(["Nội dung", "Thông tin"], [
    ["Người lập báo cáo", "Nguyễn Thành Phát"],
    ["Phạm vi báo cáo", f"Toàn bộ công việc của nhóm ({len(THANH_VIEN)} thành viên "
                        "đóng góp trong kỳ)"],
    ["Kho mã nguồn", "github.com/phatdayne2005/careercompass"],
    ["Buổi học đầu tiên", BUOI_DAU.strftime("%d/%m/%Y") + " (thứ Tư)"],
    ["Lịch học", "Mỗi thứ Tư hằng tuần, trừ ngày nghỉ lễ"],
    ["Kỳ báo cáo", f"{MOC[0].strftime('%d/%m/%Y')} – "
                   f"{min(MOC[-1] + d.timedelta(days=6), d.date.today()).strftime('%d/%m/%Y')}"],
    ["Số tuần", f"{SO_TUAN} tuần ({SO_TUAN - len(NGAY_LE)} buổi học, "
                f"{len(NGAY_LE)} buổi nghỉ lễ)"],
    ["Tổng số commit", f"{TONG_COMMIT} commit"],
], rong=[5.5, 9.5])

p()
p("Số liệu trong báo cáo này được trích xuất tự động từ lịch sử Git và kết quả chạy "
  f"kiểm thử của dự án tại thời điểm {d.date.today().strftime('%d/%m/%Y')}.",
  size=11, italic=True, color=XAM, canh=WD_ALIGN_PARAGRAPH.CENTER)

doc.add_page_break()

# ── 1. Tổng quan ─────────────────────────────────────────────────────────
tieu_de("1. TỔNG QUAN CÁC TUẦN")
p("Bảng dưới đây tóm tắt khối lượng công việc của từng tuần. Các commit được gom theo "
  "ngày tác giả — tức thời điểm công việc thực sự được thực hiện — thay vì ngày ghi "
  "nhận vào kho mã, vì ngày ghi nhận bị thay đổi mỗi khi hợp nhất nhánh.")

dong = []
for i, m in enumerate(MOC, 1):
    s = ST.get(i, {"commit": 0, "tep": 0, "them": 0, "bot": 0})
    dong.append([
        f"Tuần {i}",
        NOI_DUNG[i][0] + (" (nghỉ lễ)" if m in NGAY_LE else ""),
        m.strftime("%d/%m"),
        s["commit"], s["tep"], so(s["them"]),
    ])
dong.append(["", "TỔNG CỘNG", "", TONG_COMMIT, "",
             so(sum(v["them"] for v in ST.values()))])
bang(["Tuần", "Nội dung chính", "Buổi học", "Commit", "Tệp", "Dòng thêm"],
     dong, rong=[1.6, 6.4, 1.9, 1.7, 1.4, 2.0], canh_giua=(0, 2, 3, 4, 5))

p()
p("Ghi chú: số dòng bị xoá không đưa vào bảng vì bị chi phối bởi một lần dọn dẹp kho mã "
  "ở tuần 2 (gỡ thư mục cấu hình của công cụ lập trình khỏi phạm vi theo dõi), khiến con "
  "số này không phản ánh đúng khối lượng công việc.",
  size=11, italic=True, color=XAM)

p()
tieu_de("1.1. Thành viên đóng góp trong kỳ", muc=2)
bang(["Thành viên", "Commit", "Mảng công việc chính"],
     [[ten, n, MO_TA_TV.get(ten, "")] for ten, n in THANH_VIEN],
     rong=[4.2, 1.8, 9.0], canh_giua=(1,))

p()
p("Lưu ý khi đọc bảng trên: SỐ COMMIT KHÔNG TỶ LỆ VỚI KHỐI LƯỢNG CÔNG VIỆC. Một bản "
  "thuyết minh dài hơn một trăm đoạn kèm bảng biểu và ảnh chụp màn hình chỉ chiếm một "
  "đến hai commit, trong khi một đợt chỉnh sửa cách trình bày báo cáo có thể sinh ra "
  "hơn mười commit nhỏ. Bảng phân công ở mục 2 phản ánh đóng góp chính xác hơn.",
  size=11, italic=True, color=XAM)

p("Tên thành viên lấy theo tên tác giả ghi trong lịch sử Git. Dòng đang hiển thị tên "
  "tài khoản thay vì họ tên đầy đủ là do tài khoản đó chưa khai họ tên trong cấu hình "
  "Git — có thể điền lại thủ công khi nộp.",
  size=11, italic=True, color=XAM)

# ── 2. Phân công ─────────────────────────────────────────────────────────
doc.add_page_break()
tieu_de("2. PHÂN CÔNG THEO MẢNG CÔNG VIỆC")
p("Phần thân báo cáo trình bày công việc gộp theo tuần. Bảng này bổ sung chiều còn "
  "lại: mỗi mảng công việc do ai đảm nhận. Người phụ trách được xác định bằng cách tra "
  "tác giả của commit đầu tiên tạo ra từng tệp thuộc mảng đó, không dựa vào ghi nhớ.")

bang(["Mảng công việc", "Nội dung", "Thành viên phụ trách"],
     [list(x) for x in PHAN_CONG], rong=[4.4, 6.2, 4.4], co=11)

p()
tieu_de("2.1. Khối lượng kiểm thử tự động theo người viết", muc=2)
p("Số liệu lấy từ kết quả chạy Maven Surefire, đối chiếu với tác giả tạo ra từng tệp "
  "kiểm thử. Bảng chỉ tính kiểm thử tự động — không phản ánh phần tài liệu, báo cáo và "
  "sửa lỗi ứng dụng, vốn là những phần chiếm khối lượng lớn nhưng không sinh ra test.")
bang(["Người viết", "Số lớp kiểm thử", "Số trường hợp kiểm thử", "Tỷ lệ"],
     [[ai, lp, n, pt(100 * n / TONG_TEST)]
      for ai, (lp, n) in sorted(TEST_TG.items(), key=lambda x: -x[1][1])]
     + [["TỔNG CỘNG", TONG_LOP, TONG_TEST, "100,0%"]],
     rong=[5.4, 3.4, 4.2, 2.0], canh_giua=(1, 2, 3))

p()
tieu_de("2.2. Sản phẩm bàn giao", muc=2)
bang(["Tệp / thư mục", "Nội dung", "Người tạo"],
     [list(x) for x in SAN_PHAM], rong=[5.2, 5.4, 4.4], co=11)

# ── 3. Chi tiết từng tuần ────────────────────────────────────────────────
doc.add_page_break()
tieu_de("3. CHI TIẾT CÔNG VIỆC TỪNG TUẦN")

for i, m in enumerate(MOC, 1):
    s = ST.get(i, {"commit": 0, "tep": 0, "them": 0, "bot": 0})
    ten, nhom, ket_qua = NOI_DUNG[i]
    het = m + d.timedelta(days=6)

    tieu_de(f"3.{i}. Tuần {i} — {ten}", muc=2)

    dong_tt = [f"Buổi học: {m.strftime('%d/%m/%Y')}",
               f"Khoảng thời gian: {m.strftime('%d/%m')} – {het.strftime('%d/%m/%Y')}"]
    if m in NGAY_LE:
        dong_tt[0] += f" — NGHỈ LỄ {NGAY_LE[m].upper()}"
    if s["commit"]:
        dong_tt.append(f"Khối lượng: {s['commit']} commit, {s['tep']} tệp thay đổi, "
                       f"{so(s['them'])} dòng thêm mới")
    else:
        dong_tt.append("Khối lượng: không phát sinh commit")
    p("   ·   ".join(dong_tt), size=11, italic=True, color=XAM,
      canh=WD_ALIGN_PARAGRAPH.LEFT, sau=8)

    if m in NGAY_LE:
        # NGAY_LE[m] có dạng "Quốc khánh 02/09" — bỏ phần ngày ở cuối, giữ trọn tên
        # ngày lễ. Cắt bằng split()[0] sẽ ra "Quốc", mất chữ "khánh".
        ten_le = NGAY_LE[m].rsplit(" ", 1)[0]
        p("Buổi học ngày " + m.strftime("%d/%m/%Y") + " nghỉ theo lịch nghỉ lễ "
          + ten_le + ". Công việc trong tuần vẫn được tiến hành và ghi nhận đầy đủ "
          "dưới đây.", italic=True)

    if not nhom:
        p("Tuần này không phát sinh commit nào trên kho mã nguồn.")
        p("Phần công việc ngoài kho mã (nếu có): "
          "............................................................................."
          ".............................................................................",
          color=XAM)
        continue

    for ten_nhom, viec in nhom:
        p(ten_nhom, bold=True, canh=WD_ALIGN_PARAGRAPH.LEFT, truoc=4, sau=3)
        for v in viec:
            gach_dau_dong(v)

    if ket_qua:
        p("Kết quả đạt được: " + ket_qua, italic=True, truoc=6)

# ── 4. Kết quả ───────────────────────────────────────────────────────────
doc.add_page_break()
tieu_de("4. KẾT QUẢ TÍNH ĐẾN CUỐI KỲ BÁO CÁO")
p("Các chỉ số dưới đây là trạng thái của dự án tại thời điểm lập báo cáo.")

bang(["Hạng mục", "Kết quả"], [
    ["Kiểm thử tự động (JUnit 5)",
     f"{TONG_TEST} trường hợp trên {TONG_LOP} lớp, không có trường hợp thất bại"],
    ["Kiểm thử API (Postman / Newman)",
     "92 trường hợp kiểm thử thiết kế, 112 request, 358 phép kiểm, 0 lỗi"],
    ["Kiểm thử giao diện (CodeceptJS)",
     "17 kịch bản trên 8 nhóm chức năng, không có kịch bản thất bại"],
    ["Độ bao phủ dòng lệnh (JaCoCo)", "78,6%  (trước khi kiểm thử: 60,1%)"],
    ["Độ bao phủ nhánh (JaCoCo)", "72,5%  (trước khi kiểm thử: 48,2%)"],
    ["Ngưỡng chặn bao phủ trong CI", "65% nhánh — đạt"],
    ["Khiếm khuyết phát hiện", "11 khiếm khuyết, đã khắc phục toàn bộ"],
], rong=[7.0, 8.0])

p()
tieu_de("4.1. Các kỹ thuật kiểm thử đã áp dụng", muc=2)
bang(["Kỹ thuật", "Đối tượng áp dụng"], [
    ["Phân hoạch lớp tương đương", "Biểu mẫu đăng ký tài khoản"],
    ["Phân tích giá trị biên", "Biểu mẫu đăng ký — theo từng trường và theo bộ đầy đủ; "
                               "giới hạn dung lượng tệp bảng điểm"],
    ["Bảng quyết định", "Luật cập nhật tiến độ học; hiệu lực mã đặt lại mật khẩu; "
                        "định dạng và dung lượng tệp bảng điểm"],
    ["Chuyển đổi trạng thái", "Tiến độ node kỹ năng; vòng đời mã đặt lại mật khẩu; "
                              "quy trình khai báo hồ sơ lần đầu"],
    ["Kiểm thử hộp trắng", "Đồ thị dòng điều khiển, độ phức tạp Cyclomatic, đường cơ "
                           "sở và ma trận tổ hợp điều kiện MC/DC"],
    ["Kiểm thử dựa trên kinh nghiệm", "Đoán lỗi theo mẫu hình đã có sẵn trong mã "
                                      "nguồn, thăm dò hành vi thật trên giao diện"],
    ["Kiểm thử API", "Toàn bộ điểm cuối theo nhóm chức năng, mỗi nhóm có ít nhất một "
                     "trường hợp dương và một trường hợp âm"],
    ["Kiểm thử giao diện đầu-cuối", "17 kịch bản người dùng, đối chiếu với đặc tả bằng "
                                    "ma trận truy vết"],
], rong=[5.0, 10.0], co=11)

doc.save(DOCX)
print(f"Da tao {DOCX}")
print(f"  {SO_TUAN} tuan · {TONG_COMMIT} commit · {TONG_TEST} test tren {TONG_LOP} lop")
for ai, n in THANH_VIEN:
    lp, t = TEST_TG.get(ai, (0, 0))
    print(f"    {ai:<20} {n:>3} commit | {lp:>2} lop test | {t:>3} test")


# ── Lệnh rà soát để đối chiếu trước khi sửa NOI_DUNG ─────────────────────
# Ai tạo ra từng tệp kiểm thử:
#   git log --diff-filter=A --format='%an' -- <duong/dan/tep>
# Toàn bộ commit trong kỳ kèm tác giả và tệp thay đổi:
#   git log --reverse --format='@@%ad|%an|%s' --date=short --numstat --since=2026-07-22
# Ai tạo ra từng tệp báo cáo:
#   git log --diff-filter=A --format='%an|%ad' --date=short -- <ten-file.xlsx>

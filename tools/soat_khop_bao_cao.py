# -*- coding: utf-8 -*-
"""Soát khớp toàn bộ báo cáo với mã nguồn thật — chạy trước khi nộp.

    ./mvnw clean test
    python tools/soat_khop_bao_cao.py

In ra danh sách mọi chỗ báo cáo ghi một đằng mà đo được một nẻo. Không sai lệch nào thì
in "KHONG CO SAI LECH NAO".

VÌ SAO CẦN: hai tài liệu nộp có hơn một trăm con số rải khắp mười một sheet, cộng thêm
docs/DEMO.md và docs/coverage/README.md. Mỗi lần thêm test là một nửa số đó lệch. Đã
gặp thật nhiều lần: sheet 00 ghi 358 phép kiểm Postman trong khi chạy ra 376; DEMO.md
ghi bao phủ hộp trắng 75,9% suốt hai tuần sau khi con số đã thành 90,7%; sheet B3 nói
370 test còn sheet 00 nói 399.

Kiểm mười một nhóm:
  1. Mọi tên lớp *Test nhắc trong Excel phải thật sự tồn tại trong surefire-reports
  2. Mọi cụm "· N test" phải khớp số test thật của lớp đứng cạnh nó
  3. Sheet 00 — bốn chỉ số bao phủ, bảng 3 tổng số, và bảng 1 (cột số test vs cột "n / n")
  4. Sheet 05 — từng nhóm Postman và dòng TỔNG
  5. Sheet 06 — số kịch bản CodeceptJS
  6. Số khiếm khuyết ghi bằng chữ ở sheet 00 và 07 phải bằng số dòng DEF- đếm được
  7. Sheet 08 — lớp mã nguồn tồn tại, số test cộng đúng, phần trăm bao phủ tính đúng
  8. Sheet 09 — mọi đường dẫn tệp phải tồn tại trên đĩa, số test khớp
  9. Sheet 10 — mọi lớp mã nguồn nhắc tới phải tồn tại
 10. docs/DEMO.md — tổng số test, số test hộp đen, ba dòng bao phủ, số liệu Postman
 11. docs/coverage/README.md — tổng số test

HAI BẪY ĐÃ VẤP KHI VIẾT CHÍNH SCRIPT NÀY, đừng lặp lại:

  - Thuộc tính `name` trong TEST-*.xml là @DisplayName của lớp chứ KHÔNG phải tên lớp,
    vì dự án bật usePhrasedTestSuiteClassName. Phải lấy tên lớp từ tên tệp.
  - `-Dtest=...` KHÔNG dọn target/surefire-reports. Chạy chọn lọc rồi đếm thư mục đó sẽ
    cộng luôn báo cáo của lần chạy trước. Muốn đếm một tập con thì phải xoá thư mục
    trước, hoặc chạy './mvnw clean test' rồi đếm cả bộ.
"""
import csv
import glob
import json
import re
import xml.etree.ElementTree as ET
from pathlib import Path

import openpyxl

sai = []


def bao(muc, msg):
    sai.append(f"[{muc}] {msg}")


# ---------- nguon that ----------
TEST = {}
for p in glob.glob("target/surefire-reports/TEST-*.xml"):
    g = ET.parse(p).getroot().attrib
    TEST[Path(p).stem[5:].rsplit(".", 1)[-1]] = (
        int(g["tests"]), int(g["failures"]) + int(g["errors"]))
TONG_TEST = sum(v[0] for v in TEST.values())
TONG_DO = sum(v[1] for v in TEST.values())


def cov(p):
    R = list(csv.DictReader(open(p, encoding="utf-8")))

    def q(a, b):
        c = sum(int(r[a]) for r in R)
        m = sum(int(r[b]) for r in R)
        return round(100 * c / (c + m), 1)

    return {"line": q("LINE_COVERED", "LINE_MISSED"),
            "branch": q("BRANCH_COVERED", "BRANCH_MISSED"),
            "cx": q("COMPLEXITY_COVERED", "COMPLEXITY_MISSED"),
            "m": q("METHOD_COVERED", "METHOD_MISSED"),
            "rows": {r["CLASS"]: r for r in R}}


GOP = cov("target/site/jacoco/jacoco.csv")
WB = cov("target/site/jacoco-whitebox/jacoco.csv")
BB = cov("target/site/jacoco-blackbox/jacoco.csv")
E2E = json.loads(Path("tools/ket-qua-chay/codecept.json").read_text(encoding="utf-8"))["stats"]
PM = json.loads(Path("tools/ket-qua-chay/postman-stats.json").read_text(encoding="utf-8"))
TM = json.loads(Path("tools/ket-qua-chay/postman-thu-muc.json").read_text(encoding="utf-8"))
REQ = sum(v[0] for v in TM.values())
LOP_NGUON = {Path(p).stem for p in glob.glob("src/main/java/**/*.java", recursive=True)}

print(f"NGUON THAT: {TONG_TEST} test / {TONG_DO} do / {len(TEST)} lop")
print(f"            gop {GOP['line']}%/{GOP['branch']}% | hop trang {WB['line']}%/{WB['branch']}%"
      f" | hop den {BB['line']}%/{BB['branch']}%")
print(f"            E2E {E2E['tests']}/{E2E['failures']} | Postman {REQ} req "
      f"{PM['assertions']['total']} phep kiem\n")

wb = openpyxl.load_workbook("BaoCao-KiemThu-CareerCompass.xlsx")

# ---------- 1. Moi ten lop *Test nhac trong Excel phai ton tai ----------
for ws in wb.worksheets:
    for r in range(1, ws.max_row + 1):
        for c in range(1, ws.max_column + 1):
            v = ws.cell(row=r, column=c).value
            if not isinstance(v, str):
                continue
            # @WebMvcTest la chu thich cua Spring chu khong phai lop kiem thu;
            # "TenLopTest" la cho dien mau trong huong dan chay lenh.
            BO_QUA = {"WebMvcTest", "SpringBootTest", "DataJpaTest"}
            # Bat buoc co it nhat hai ky tu truoc "Test" de khong bat trung tu
            # "Test" don le trong cac cum nhu "Test Case".
            for ten in set(re.findall(r"(?<![@\w])([A-Z][A-Za-z0-9]{2,}Test)\b", v)):
                if ten in BO_QUA:
                    continue
                if ten not in TEST:
                    bao("lop test khong ton tai", f"{ws.title} {r},{c}: {ten}")
            m = re.search(r"·\s*(\d+)\s*test\b", v)
            ds = {t for t in re.findall(r"\b([A-Z]\w*Test)\b", v) if t in TEST}
            if m and len(ds) == 1:
                lop = ds.pop()
                if int(m.group(1)) != TEST[lop][0]:
                    bao("so test lech",
                        f"{ws.title} {r},{c}: ghi {m.group(1)}, {lop} co {TEST[lop][0]}")

# ---------- 2. Sheet 00 ----------
ws = wb["00. Tong quan"]
KY = {"Dòng lệnh (Line)": GOP["line"], "Nhánh (Branch)": GOP["branch"],
      "Độ phức tạp được phủ": GOP["cx"],
      "Phương thức": GOP["m"]}
BA = {"Đơn vị (JUnit)": (TONG_TEST, TONG_DO),
      "Tích hợp (Postman)": (PM["assertions"]["total"], PM["assertions"]["failed"]),
      "Hệ thống (CodeceptJS)": (E2E["tests"], E2E["failures"])}
for r in range(1, ws.max_row + 1):
    a = str(ws.cell(row=r, column=1).value or "").strip()
    if a in KY:
        g = str(ws.cell(row=r, column=3).value or "").replace("%", "").replace(",", ".")
        if abs(float(g) - KY[a]) > 0.05:
            bao("sheet00 coverage", f"dong {r} '{a}': ghi {g}%, that {KY[a]}%")
    if a in BA:
        g = (ws.cell(row=r, column=3).value, ws.cell(row=r, column=4).value)
        if g != BA[a]:
            bao("sheet00 bang3", f"dong {r} '{a}': ghi {g}, that {BA[a]}")
    so, kq = ws.cell(row=r, column=3).value, str(ws.cell(row=r, column=5).value or "")
    m = re.fullmatch(r"(\d+)\s*/\s*(\d+)", kq.strip())
    if m and isinstance(so, int):
        if not (int(m.group(1)) == int(m.group(2)) == so):
            bao("sheet00 bang1", f"dong {r}: so test {so} vs ket qua '{kq}'")

# ---------- 3. Sheet 05 Postman ----------
ws = wb["05. API - Postman"]
for r in range(1, ws.max_row + 1):
    ten = ws.cell(row=r, column=1).value
    if ten in TM:
        g = [ws.cell(row=r, column=c).value for c in (2, 3, 4)]
        if g != TM[ten]:
            bao("sheet05 nhom", f"dong {r} '{ten}': ghi {g}, that {TM[ten]}")
    if str(ten or "").strip() == "TỔNG":
        g = [ws.cell(row=r, column=c).value for c in (2, 3, 4)]
        that = [REQ, PM["assertions"]["total"], PM["assertions"]["failed"]]
        if g != that:
            bao("sheet05 tong", f"dong {r}: ghi {g}, that {that}")

# ---------- 4. Sheet 06 E2E ----------
ws = wb["06. E2E - CodeceptJS"]
for r in range(1, 9):
    v = str(ws.cell(row=r, column=1).value or "")
    m = re.search(r"(\d+) kịch bản, (\d+) đạt, (\d+) không đạt", v)
    if m:
        that = (E2E["tests"], E2E["passes"], E2E["failures"])
        if tuple(int(x) for x in m.groups()) != that:
            bao("sheet06", f"dong {r}: ghi {m.groups()}, that {that}")

# ---------- 5. So khiem khuyet ----------
ws = wb["07. Khiem khuyet"]
n_def = sum(1 for r in range(1, ws.max_row + 1)
            if re.fullmatch(r"DEF-\d+", str(ws.cell(row=r, column=1).value or "").strip()))
CHU = {"Mười": 10, "Mười một": 11,
       "Mười hai": 12, "Mười ba": 13}
for w2 in (wb["00. Tong quan"], ws):
    for r in range(1, w2.max_row + 1):
        v = str(w2.cell(row=r, column=1).value or "")
        if "khiếm khuyết" not in v or "khắc phục" not in v:
            continue
        m = re.search(r"(?:Cả )?([A-ZÀ-ỹ][\wÀ-ỹ]*(?: [\wÀ-ỹ]+)?|\d+)"
                      r" khiếm khuyết", v)
        if not m:
            continue
        so = CHU.get(m.group(1)) or (int(m.group(1)) if m.group(1).isdigit() else None)
        if so is not None and so != n_def:
            bao("so khiem khuyet",
                f"{w2.title} dong {r}: ghi '{m.group(1)}', dem duoc {n_def}")

# ---------- 6. Sheet 08 truy vet ----------
ws = wb["08. Truy vet SRS"]
n_fr = 0
for r in range(1, ws.max_row + 1):
    ma = str(ws.cell(row=r, column=1).value or "").strip()
    if not re.fullmatch(r"FR\d\.\d", ma):
        continue
    n_fr += 1
    nguon = [x.strip() for x in str(ws.cell(row=r, column=3).value or "").split("\n") if x.strip()]
    lt = [x.strip() for x in str(ws.cell(row=r, column=4).value or "").split("\n") if x.strip()]
    for c in nguon:
        if c not in WB["rows"]:
            bao("sheet08 lop nguon", f"{ma}: '{c}' khong co trong jacoco")
    st = sum(TEST[x][0] for x in lt if x in TEST)
    if ws.cell(row=r, column=5).value != st:
        bao("sheet08 so test", f"{ma}: ghi {ws.cell(row=r, column=5).value}, cong lai {st}")
    lc = sum(int(WB["rows"][c]["LINE_COVERED"]) for c in nguon if c in WB["rows"])
    tt = sum(int(WB["rows"][c]["LINE_COVERED"]) + int(WB["rows"][c]["LINE_MISSED"])
             for c in nguon if c in WB["rows"])
    that = round(100 * lc / tt, 1) if tt else 0.0
    ghi = float(str(ws.cell(row=r, column=6).value or "0").replace("%", "").replace(",", "."))
    if abs(ghi - that) > 0.05:
        bao("sheet08 bao phu", f"{ma}: ghi {ghi}%, that {that}%")
if n_fr != 26:
    bao("sheet08", f"dem duoc {n_fr} yeu cau chuc nang, SRS co 26")

# ---------- 7. Sheet 09 duong dan ----------
ws = wb["09. Vi tri file test"]
for r in range(1, ws.max_row + 1):
    dd = str(ws.cell(row=r, column=3).value or "").strip()
    if not dd.startswith("src/test/"):
        continue
    if not Path(dd).exists():
        bao("sheet09 duong dan", f"dong {r}: khong ton tai {dd}")
        continue
    lop = Path(dd).stem
    if ws.cell(row=r, column=5).value != TEST.get(lop, (None,))[0]:
        bao("sheet09 so test",
            f"dong {r} {lop}: ghi {ws.cell(row=r, column=5).value}, that {TEST.get(lop, ('?',))[0]}")

# ---------- 8. Sheet 10 vi tri ma nguon ----------
ws = wb["10. Ra soat BVA"]
for r in range(1, ws.max_row + 1):
    vt = str(ws.cell(row=r, column=3).value or "")
    for ten in re.findall(r"\b([A-Z][A-Za-z0-9]+)(?=:\d+|\s+@Size|\s+MAX_)", vt):
        if ten not in LOP_NGUON:
            bao("sheet10 lop nguon", f"dong {r}: '{ten}' khong co trong src/main/java")

# ---------- 9. Tai lieu ----------
demo = Path("docs/DEMO.md").read_text(encoding="utf-8")
# DEMO.md co HAI dong dang nay: muc 1.1 la rieng hop den, muc 2 moi la toan bo.
# Neo vao lenh './mvnw clean test' de bat dung dong toan bo.
m = re.search(r"\./mvnw clean test\s*```\s*\n+→ \*\*(\d+) test, 0 thất bại\*\*", demo)
m_hd = re.search(r"→ \*\*(\d+) test, 0 thất bại\*\* — đúng bằng tổng các sheet hộp đen", demo)
HOP_DEN = sum(n for c, (n, _) in TEST.items()
              if c in {"RegisterStandardBvaTest", "RegisterTagCoverageTest",
                       "RegisterEquivalencePartitionTest", "OnboardingFileSizeBvaTest",
                       "PasswordPolicyBvaTest", "TokenExpiryBvaTest",
                       "ProgressDecisionTableTest", "TokenValidityDecisionTableTest",
                       "TranscriptFileDecisionTableTest", "ProgressStateTransitionTest",
                       "TokenStateTransitionTest", "OnboardingStateTransitionTest"})
if not m_hd:
    bao("DEMO", "khong thay dong tong so test hop den o muc 1.1")
elif int(m_hd.group(1)) != HOP_DEN:
    bao("DEMO hop den", f"ghi {m_hd.group(1)}, that {HOP_DEN}")
if not m:
    bao("DEMO", "khong thay dong tong so test")
elif int(m.group(1)) != TONG_TEST:
    bao("DEMO tong test", f"ghi {m.group(1)}, that {TONG_TEST}")

for khoa, bo in (("jacoco/`", GOP), ("jacoco-whitebox/`", WB), ("jacoco-blackbox/`", BB)):
    m = re.search(re.escape(khoa) + r"[^\n]*?([\d,]+)%[^\n]*?([\d,]+)%", demo)
    if not m:
        bao("DEMO coverage", f"khong thay dong {khoa}")
        continue
    g = (float(m.group(1).replace(",", ".")), float(m.group(2).replace(",", ".")))
    if abs(g[0] - bo["line"]) > 0.05 or abs(g[1] - bo["branch"]) > 0.05:
        bao("DEMO coverage", f"{khoa}: ghi {g}, that ({bo['line']}, {bo['branch']})")

if f"{REQ} request" not in demo:
    bao("DEMO postman", f"khong thay '{REQ} request'")
if f"{PM['assertions']['total']} phép kiểm" not in demo:
    bao("DEMO postman", f"khong thay so phep kiem {PM['assertions']['total']}")

cvr = Path("docs/coverage/README.md").read_text(encoding="utf-8")
m = re.search(r"\*\*(\d+) test\*\*", cvr)
if m and int(m.group(1)) != TONG_TEST:
    bao("coverage README", f"ghi {m.group(1)} test, that {TONG_TEST}")

print(f"{len(set(sai))} SAI LECH" if sai else "KHONG CO SAI LECH NAO")
for s in sorted(set(sai)):
    print("  " + s)

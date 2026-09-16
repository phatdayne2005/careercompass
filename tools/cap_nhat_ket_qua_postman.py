# -*- coding: utf-8 -*-
"""Sinh lại hai tệp kết quả Postman từ bản xuất JSON của newman.

    npx newman run CareerCompass.postman_collection.json \
        -e CareerCompass.postman_environment.json \
        --reporters cli,json --reporter-json-export newman.json
    python tools/cap_nhat_ket_qua_postman.py newman.json

Ghi ra:
    tools/ket-qua-chay/postman-stats.json    — số tổng chính thức
    tools/ket-qua-chay/postman-thu-muc.json  — [request, phép kiểm, lỗi] theo thư mục

VÌ SAO CẦN: trước đây hai tệp này chụp bằng tay từ một lần chạy tháng 9, sau đó bộ sưu
tập còn được sửa mà số trong báo cáo thì đứng yên — sheet 00 ghi 358 phép kiểm trong khi
chạy thật ra 376.

HAI CÁI BẪY CỦA BẢN XUẤT NEWMAN:

1. Bản xuất KHÔNG kèm cây thư mục, và mỗi execution cũng không nói mình thuộc thư mục
   nào. Nhưng executions chạy đúng thứ tự duyệt cây, và cursor.position chính là chỉ số
   trong danh sách đã làm phẳng. Nên ở đây làm phẳng lại tệp bộ sưu tập theo đúng thứ tự
   đó rồi tra ngược ra thư mục.

2. Một request có chuyển hướng bị ghi nhật ký HAI LẦN với cùng cursor.position. Cộng
   thẳng ra 380 trong khi số chính thức là 376. Gộp theo position rồi lấy bản ghi cuối
   thì khớp đúng 376.
"""
import json
import sys
from pathlib import Path

RA = Path("tools/ket-qua-chay")
BO_SUU_TAP = Path("CareerCompass.postman_collection.json")

if len(sys.argv) != 2:
    raise SystemExit(__doc__)
run = json.loads(Path(sys.argv[1]).read_text(encoding="utf-8"))["run"]

s = run["stats"]
stats = {k: {"total": s[k]["total"], "failed": s[k]["failed"]}
         for k in ("items", "requests", "tests", "assertions",
                   "testScripts", "prerequestScripts")}


def lam_phang(items, thu_muc=""):
    """Trả về [(tên thư mục, tên request)] theo đúng thứ tự newman duyệt."""
    for it in items:
        if "item" in it:
            yield from lam_phang(it["item"], it["name"])
        else:
            yield thu_muc, it["name"]


phang = list(lam_phang(json.loads(BO_SUU_TAP.read_text(encoding="utf-8"))["item"]))

# Gộp theo position, giữ bản ghi cuối — xem bẫy số 2 ở đầu tệp.
theo_vi_tri = {}
for e in run["executions"]:
    theo_vi_tri[e["cursor"]["position"]] = e

thu_muc = {}
for vi_tri, e in sorted(theo_vi_tri.items()):
    if vi_tri >= len(phang):
        raise SystemExit(
            f"position {vi_tri} vuot qua {len(phang)} request trong bo suu tap — "
            "ban xuat newman va tep bo suu tap khong cung mot phien ban.")
    ten_tm, ten_req = phang[vi_tri]
    if ten_req != e["item"]["name"]:
        raise SystemExit(
            f"lech thu tu o position {vi_tri}: bo suu tap co '{ten_req}' nhung ban "
            f"xuat co '{e['item']['name']}' — chay lai newman voi tep hien tai.")
    a = e.get("assertions", [])
    d = thu_muc.setdefault(ten_tm, [0, 0, 0])
    d[0] += 1
    d[1] += len(a)
    d[2] += sum(1 for x in a if x.get("error"))

cong = sum(v[1] for v in thu_muc.values())
if cong != stats["assertions"]["total"]:
    raise SystemExit(f"cong theo thu muc ra {cong} nhung run.stats ghi "
                     f"{stats['assertions']['total']} — dung lai de khoi ghi so sai.")

(RA / "postman-stats.json").write_text(
    json.dumps(stats, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
(RA / "postman-thu-muc.json").write_text(
    json.dumps(thu_muc, ensure_ascii=False) + "\n", encoding="utf-8")

print(f"postman-stats.json   : {stats['requests']['total']} request, "
      f"{stats['assertions']['total']} phep kiem, {stats['assertions']['failed']} loi, "
      f"{stats['testScripts']['failed']} script loi")
print(f"postman-thu-muc.json : {len(thu_muc)} thu muc, cong lai {cong} phep kiem")

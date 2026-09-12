# Công cụ sinh báo cáo

Hai tài liệu nộp cho giảng viên đều được sinh tự động từ mã nguồn và lịch sử Git, không
gõ số bằng tay:

| Tệp | Nội dung |
|---|---|
| `BaoCao-KiemThu-CareerCompass.xlsx` | Bảng số liệu kiểm thử, bảng thiết kế theo mẫu bài giảng |
| `BaoCao-CongViec-Theo-Tuan.docx` | Công việc của nhóm theo từng tuần học |

## Thứ tự chạy

Phải chạy đúng thứ tự — script sau đọc kết quả của script trước.

```bash
# 0. Chạy test trước. Mọi script đều đọc target/surefire-reports và jacoco.csv;
#    thiếu là script dừng chứ không lấy số cũ.
./mvnw clean test

# 1. Làm mới bản chụp độ bao phủ (chỉ khi số bao phủ đã đổi)
rm -rf docs/coverage && cp -r target/site/jacoco docs/coverage
git checkout -- docs/coverage/README.md      # giữ lại phần viết tay

# 2. Sinh các sheet bổ sung của báo cáo Excel
python tools/gen_sheets_bo_sung.py

# 3. Cập nhật sheet tổng quan và sheet khiếm khuyết
python tools/cap_nhat_tong_quan.py

# 4. Sinh sheet phân công nhóm
python tools/gen_sheet_phan_cong.py

# 5. Cập nhật README của bản chụp độ bao phủ
python tools/cap_nhat_readme_coverage.py

# 6. Sinh báo cáo Word theo tuần
python tools/gen_baocao_tuan_docx.py
```

### Khi dựng lại báo cáo Excel từ đầu

`gen_bao_cao_tong_hop.py` dựng lại toàn bộ tệp Excel từ hai tệp nguồn
`BaoCao-PhanA-HopDen.xlsx` và `BaoCao_Whitebox.xlsx`. Chạy nó sẽ **ghi đè B3 và B4
bằng bản gốc chưa có dấu mốc thời gian**, làm sống lại mâu thuẫn 370 test với 399 test.
Sau khi chạy nó, phải chạy lại bước đóng dấu:

```bash
python tools/gen_bao_cao_tong_hop.py
python tools/dong_dau_moc_thoi_gian_whitebox.py   # BẮT BUỘC, nếu không B3/B4 lại lệch
# rồi chạy tiếp từ bước 2 ở trên
```

Cả sáu script đều **chạy lại được nhiều lần** mà không nhân đôi nội dung: script nào
chèn thêm dòng hoặc sheet thì đều gỡ bản cũ trước khi dựng lại.

## Hai cái bẫy đã gặp, đừng vấp lại

**Đừng dùng `Worksheet.insert_rows()` của openpyxl trên sheet có ô gộp.** Nó dịch giá
trị ô nhưng không dịch dải gộp, làm mất trắng nội dung của đúng những dòng có gộp. Đã
mất ba dòng "Kiểm thử API", "Độ phức tạp được phủ", "Tích hợp (Postman)" theo cách này.
`cap_nhat_tong_quan.py` thay bằng cách đọc cả sheet ra bộ nhớ rồi dựng lại.

**Gom commit theo ngày tác giả (`%ad`), không phải ngày commit.** `git log --since`
lọc theo ngày commit, vốn bị đổi mỗi khi merge hoặc rebase. Hai cách cho số liệu lệch
nhau đáng kể — tuần 6 ra 15 commit theo ngày tác giả nhưng chỉ 3 theo ngày commit.

## Khi sửa nội dung viết tay

Phần mô tả công việc từng tuần nằm trong biến `NOI_DUNG` của
`tools/gen_baocao_tuan_docx.py`. Đây là phần duy nhất viết tay trong cả hai báo cáo.

Trước khi sửa, **rà lại commit thật của cả nhóm** chứ đừng viết theo trí nhớ. Bản đầu
tiên của báo cáo đã mắc lỗi này: phần kiểm thử hộp đen được mô tả rất chi tiết, còn
kiểm thử hộp trắng, kiểm thử tầng điều khiển, sửa lỗi giao diện và bản thuyết minh Word
thì gần như không nhắc — trong khi đó là phần việc của hai thành viên còn lại, và tài
liệu này dùng để chấm điểm từng cá nhân.

```bash
# Toàn bộ commit trong kỳ, kèm tác giả và tệp thay đổi
git log --reverse --format='@@%ad|%an|%s' --date=short --numstat --since=2026-07-22

# Ai tạo ra một tệp bất kỳ
git log --diff-filter=A --format='%an|%ad' --date=short -- <đường/dẫn/tệp>

# Số test theo người viết (script tự tính, dùng để đối chiếu)
python tools/gen_baocao_tuan_docx.py
```

## Các script khác

| Script | Việc |
|---|---|
| `gen_sheet01.py` | Sinh sheet 01 — giá trị biên và phân hoạch lớp tương đương |
| `gen_sheets_dt_st.py` | Sinh sheet 03, 03b, 04 — bảng quyết định và chuyển đổi trạng thái |
| `gen_sheet04b.py` | Sinh sheet 04b — chuyển đổi trạng thái vòng đời token |
| `gen_bao_cao_tong_hop.py` | Dựng báo cáo Excel tổng hợp từ các báo cáo thành phần |
| `patch_postman.py` | Vá bộ kiểm thử Postman cho chạy lại được nhiều lần |
| `dong_dau_moc_thoi_gian_whitebox.py` | Đóng dấu ngày đo lên B3/B4 — xem cảnh báo ở trên |

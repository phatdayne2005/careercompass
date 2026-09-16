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

# 2. Làm mới kết quả Postman (chỉ khi bộ sưu tập hoặc ứng dụng đã đổi).
#    Cần ứng dụng đang chạy ở localhost:8080.
npx newman run CareerCompass.postman_collection.json     -e CareerCompass.postman_environment.json     --reporters cli,json --reporter-json-export newman.json
python tools/cap_nhat_ket_qua_postman.py newman.json

# 3. Sinh các sheet bổ sung của báo cáo Excel
python tools/gen_sheets_bo_sung.py

# 4. Cập nhật sheet tổng quan và sheet khiếm khuyết
python tools/cap_nhat_tong_quan.py

# 5. Sinh sheet phân công nhóm
python tools/gen_sheet_phan_cong.py

# 6. Cập nhật README của bản chụp độ bao phủ
python tools/cap_nhat_readme_coverage.py

# 7. Sinh báo cáo Word theo tuần
python tools/gen_baocao_tuan_docx.py
```

### Khi dựng lại báo cáo Excel từ đầu

`gen_bao_cao_tong_hop.py` dựng lại **toàn bộ** tệp Excel từ hai tệp nguồn
`BaoCao-PhanA-HopDen.xlsx` và `BaoCao_Whitebox.xlsx`. Nó không biết gì về các sửa đổi
bổ sung làm sau đó, nên chạy nó sẽ **xoá sạch** những thứ này:

- dấu mốc thời gian trên B3/B4 → làm sống lại mâu thuẫn 370 test với 399 test
- BẢNG 3b (BVA dung lượng tệp) ở sheet 01
- cột Status của BẢNG 5, ghi chú của BẢNG 4, ghi chú đầu sheet 01
- tên method thật ở sheet B1
- bố cục đã chuẩn hoá và bảng đầy đủ của sheet 03c, 04, 04b, 04c

Muốn dựng lại từ đầu thì phải chạy **trọn** dãy dưới đây, đúng thứ tự, rồi mới quay lại
bước 3 ở trên:

```bash
python tools/gen_bao_cao_tong_hop.py
python tools/dong_dau_moc_thoi_gian_whitebox.py   # BẮT BUỘC
python tools/chuan_hoa_sheet_dt_st.py             # dựng lại 03c, 04, 04b, 04c
python tools/sua_ten_method_b1.py
python tools/them_bang3b_dung_luong.py
python tools/them_ghi_chu_sheet01.py
python tools/them_ghi_chu_bang4.py
python tools/them_status_bang5.py
```

Vì dãy này dài và dễ sót, **đừng dựng lại từ đầu nếu chỉ cần sửa vài con số** — sửa
thẳng ô bằng một script nhỏ đọc-cả-sheet-rồi-dựng-lại thì an toàn hơn nhiều.

Mọi script đều **chạy lại được nhiều lần** mà không nhân đôi nội dung: script nào chèn
thêm dòng hoặc sheet thì đều gỡ bản cũ trước khi dựng lại.

## Những cái bẫy đã gặp, đừng vấp lại

**Đừng dùng `Worksheet.insert_rows()` của openpyxl trên sheet có ô gộp.** Nó dịch giá
trị ô nhưng không dịch dải gộp, làm mất trắng nội dung của đúng những dòng có gộp. Đã
mất ba dòng "Kiểm thử API", "Độ phức tạp được phủ", "Tích hợp (Postman)" theo cách này.
`cap_nhat_tong_quan.py` thay bằng cách đọc cả sheet ra bộ nhớ rồi dựng lại.

**Gom commit theo ngày tác giả (`%ad`), không phải ngày commit.** `git log --since`
lọc theo ngày commit, vốn bị đổi mỗi khi merge hoặc rebase. Hai cách cho số liệu lệch
nhau đáng kể — tuần 6 ra 15 commit theo ngày tác giả nhưng chỉ 3 theo ngày commit.

**Bản chụp kết quả Postman sẽ mốc theo thời gian.** `tools/ket-qua-chay/postman-stats.json`
từng được chụp tay một lần rồi để đó; bộ sưu tập sửa tiếp mà báo cáo thì đứng yên, nên
sheet 00 ghi 358 phép kiểm trong khi chạy thật ra 376. Dùng
`cap_nhat_ket_qua_postman.py` chứ đừng gõ lại số bằng tay.

**Đừng khai báo `const` ở phạm vi ngoài cùng trong script test của Postman.** Newman
dùng CHUNG một sandbox cho cả lượt chạy, nên request thứ hai khai báo lại cùng một tên
sẽ ném `SyntaxError` và **toàn bộ** `pm.test` của request đó không chạy — newman vẫn báo
"0 failed" vì phép kiểm không chạy thì không tính là trượt. Đúng 18 phép kiểm đã âm thầm
biến mất kiểu này. Postman GUI không lộ lỗi vì mỗi request một sandbox riêng. Dùng `var`,
hoặc đặt tên khác nhau.

**Thuộc tính `name` trong `TEST-*.xml` của Surefire là `@DisplayName` của lớp**, không
phải tên lớp — dự án bật `usePhrasedTestSuiteClassName`. Lấy tên lớp từ tên tệp.

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
| `cap_nhat_ket_qua_postman.py` | Sinh lại hai tệp kết quả Postman từ bản xuất newman |
| `chuan_hoa_sheet_dt_st.py` | Chuẩn hoá bố cục sheet 03c, 04, 04b, 04c kèm bảng đầy đủ |
| `sua_ten_method_b1.py` | Thay tên method giả ở sheet B1 bằng tên thật trong mã nguồn |
| `them_bang3b_dung_luong.py` | Thêm BẢNG 3b — BVA dung lượng tệp vào sheet 01 |
| `them_ghi_chu_sheet01.py` | Ghi chú vì sao `RegisterFormDTOBvaTest` nằm ngoài bảng tổng kết |
| `them_ghi_chu_bang4.py` | Ghi chú vì sao 15 nhãn chỉ sinh ra 14 test |
| `them_status_bang5.py` | Thêm cột Status cho BẢNG 5 |

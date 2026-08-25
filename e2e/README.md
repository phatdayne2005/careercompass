# Kiểm thử giao diện (E2E) — CareerCompass

Bộ kiểm thử giao diện tự động bằng **CodeceptJS 4 + Playwright**.

Khác với 198 unit test ở tầng service (dùng Mockito, không chạm giao diện), bộ này
điều khiển **trình duyệt thật** trên **ứng dụng đang chạy thật**, nên bắt được lớp lỗi
mà unit test không thấy: lỗi render Thymeleaf, `LazyInitializationException`,
sai chuyển hướng, hổng phân quyền.

## Chạy nhanh

```bash
# 1. Dựng app (từ thư mục gốc dự án)
docker compose up -d

# 2. Cài đặt lần đầu
cd e2e
npm install
npx playwright install chromium

# 3. Chạy test
npm test                 # tất cả, trừ nhóm @known-bug
npm run report           # sinh báo cáo Excel từ kết quả vừa chạy
```

Muốn xem trình duyệt thao tác: `SHOW=true npm test`
Chạy trên môi trường khác: `BASE_URL=https://careercompass.phatnguyendev.site npm test`

## Các lệnh

| Lệnh | Chạy gì |
|---|---|
| `npm test` | Toàn bộ trừ `@known-bug` — dùng hằng ngày |
| `npm run test:p0` | Nhóm P0, cổng chặn deploy trong CI (~1 phút) |
| `npm run test:p1` | Nhóm P1, hồi quy |
| `npm run test:knownbug` | Case đang tố cáo lỗi chưa sửa (sẽ đỏ — đúng như thiết kế) |
| `npm run test:all` | Tất cả, kể cả `@known-bug` |
| `npm run report` | Sinh `output/BaoCao-KiemThu-<ngày>.xlsx` |
| `npm run test:report` | Chạy test rồi sinh báo cáo luôn |

## Cấu trúc

```
e2e/
├── codecept.conf.js       # cấu hình helper Playwright, plugin
├── pages/                 # Page Object — mỗi trang một class
├── tests/                 # kịch bản kiểm thử
├── support/
│   ├── steps_file.js      # bước dùng chung (sinh email duy nhất)
│   ├── resultCollector.js # plugin ghi kết quả ra output/result.json
│   ├── catalog.js         # mô tả test case + ánh xạ FR + danh sách lỗi
│   └── generate-report.js # sinh Excel từ result.json + catalog.js
└── output/                # kết quả, ảnh chụp lỗi, file Excel (gitignored)
```

### Vì sao dùng Page Object

Thao tác với trang được gom vào `pages/`, kịch bản kiểm thử chỉ mô tả *ý định*:

```js
loginPage.login(email, 'MatKhau@123');
onboardingPage.completeAll(2);
dashboardPage.seeDashboard();
```

Khi giao diện đổi — ví dụ đổi `id="username"` thành `id="email"` — chỉ sửa **một dòng**
trong `pages/Login.js`, toàn bộ test vẫn chạy. Không có Page Object thì phải sửa từng file test.

## Phân nhóm bằng tag

| Tag | Ý nghĩa |
|---|---|
| `@P0` | Luồng sống còn. Đỏ là **chặn deploy**. Chạy trong CI mỗi lần push `main`. |
| `@P1` | Hồi quy, tập trung vào vùng từng có bug. |
| `@known-bug` | Case đang tố cáo lỗi chưa sửa. Tách riêng để không làm đỏ CI oan. |
| `@smoke` | Tập con nhanh nhất, kiểm tra hệ thống còn sống. |

## Chiến lược dữ liệu

Mỗi test **tự đăng ký tài khoản mới** với email sinh ngẫu nhiên (`I.uniqueEmail()`).

Nhờ vậy test chạy lại bao nhiêu lần cũng cho cùng kết quả, và không phụ thuộc dữ liệu
sẵn có trên máy ai. Đánh đổi: dữ liệu test tích lũy dần trong database — chấp nhận được
với môi trường dev/CI, dọn bằng `docker compose down -v` khi cần.

## Ba điểm dễ vấp khi viết thêm test

**1. `OnboardingInterceptor` chặn mọi thứ.** Tài khoản STUDENT chưa hoàn thành onboarding
sẽ bị đá về `/onboarding/step1` ở *mọi* trang. Test chức năng nào cũng phải gọi
`onboardingPage.completeAll()` trước.

**2. Checkbox kỹ năng bị ẩn.** Ở bước 3 onboarding, `<input type="checkbox">` có class
Tailwind `sr-only` nên Playwright không click được. Phải click vào `<label for=...>`,
đúng như người dùng thật.

**3. Spring Security giữ quyền trong session.** Đổi vai trò trong database **không** làm
mới phiên đang đăng nhập. Test về phân quyền phải đăng xuất rồi đăng nhập lại mới thấy
hiệu lực — nếu không sẽ xanh giả.

## Báo cáo Excel

`npm run report` ghép kết quả chạy thật (`output/result.json`) với phần mô tả
(`support/catalog.js`) rồi xuất workbook 4 sheet:

1. **Tổng quan** — số liệu tổng hợp, thống kê theo module và theo độ ưu tiên
2. **Chi tiết Test Case** — từng case: điều kiện, các bước, kết quả mong đợi, kết quả thực tế
3. **Ma trận truy vết** — yêu cầu nào trong SRS đã được test, yêu cầu nào chưa
4. **Danh sách lỗi** — defect phát hiện được

Số liệu lấy từ lần chạy thật nên không bao giờ lệch với thực tế.

**Thêm test case mới:** thêm `Scenario` trong `tests/`, đặt tên theo mẫu
`TC-<MODULE>-<số> | mô tả @P0`, rồi thêm mục tương ứng vào `support/catalog.js`.

## Vai trò trong CI/CD

```
push main → build (198 unit test) → docker (build image)
          → e2e (@P0 trên app thật)  ← chặn ở đây nếu đỏ
          → deploy VPS → cleanup
```

CI đính kèm file Excel và ảnh chụp màn hình lỗi vào mục **Artifacts** của mỗi lần chạy,
giữ trong 30 ngày.

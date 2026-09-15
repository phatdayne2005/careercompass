# Bảng lệnh demo — CareerCompass

Mọi lệnh dưới đây **đã chạy thử và ghi lại kết quả thật**. Ai trong nhóm chạy cũng phải
ra đúng những con số này — nếu lệch, xem mục *Khi số không khớp* ở cuối.

Chạy từ thư mục gốc dự án (`lap-trinh-java/`), trừ phần CodeceptJS chạy trong `e2e/`.

---

## 0 · Chuẩn bị

### Cho phần kiểm thử đơn vị (hộp trắng + hộp đen)

Không cần gì cả. Không cần chạy ứng dụng, không cần cơ sở dữ liệu — toàn bộ dùng đối
tượng giả lập (Mockito).

### Cho phần kiểm thử API và giao diện

Phải có ứng dụng **đang chạy bằng Docker**:

```bash
docker compose up -d
docker compose ps          # đợi tới khi cột STATUS ghi "healthy"
```

Kiểm tra đã lên chưa:

```bash
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/login    # phải ra 200
```

> **Dùng Docker, đừng chạy bằng IntelliJ.** Khi chạy từ IDE, thư viện
> `spring-boot-devtools` được nạp và ghi đè cấu hình `server.error.*`, làm phản hồi lỗi
> kèm nguyên vết ngăn xếp. Đã đo trên cùng một mã nguồn, cùng một yêu cầu
> `GET /p/<slug-sai>`: bản `java -jar` trả **103 byte**, bản IntelliJ trả **10.910 byte**
> có nguyên stack trace — làm phép kiểm NFR-S05 báo đỏ oan. Chi tiết ghi trong
> `src/main/resources/application.yml`.

---

## 1 · Kiểm thử hộp đen

### 1.1 Chạy trọn bốn kỹ thuật

```bash
./mvnw test -Dtest='RegisterStandardBvaTest,RegisterFormDTOBvaTest,OnboardingFileSizeBvaTest,RegisterEquivalencePartitionTest,RegisterTagCoverageTest,ProgressDecisionTableTest,TokenValidityDecisionTableTest,TranscriptFileDecisionTableTest,ProgressStateTransitionTest,TokenStateTransitionTest,OnboardingStateTransitionTest'
```

→ **130 test, 0 thất bại**

### 1.2 Chạy từng kỹ thuật một

| Kỹ thuật | Lệnh | Kết quả |
|---|---|---|
| Giá trị biên | `./mvnw test -Dtest='RegisterStandardBvaTest,RegisterFormDTOBvaTest,OnboardingFileSizeBvaTest'` | **52 test** |
| Phân hoạch lớp tương đương | `./mvnw test -Dtest='RegisterEquivalencePartitionTest,RegisterTagCoverageTest'` | **31 test** |
| Bảng quyết định | `./mvnw test -Dtest='ProgressDecisionTableTest,TokenValidityDecisionTableTest,TranscriptFileDecisionTableTest'` | **19 test** |
| Chuyển đổi trạng thái | `./mvnw test -Dtest='ProgressStateTransitionTest,TokenStateTransitionTest,OnboardingStateTransitionTest'` | **28 test** |

### 1.3 In từng trường hợp kiểm thử ra màn hình

Thêm `-DshowCases` vào bất kỳ lệnh nào ở trên. Ví dụ:

```bash
./mvnw test -Dtest='RegisterStandardBvaTest' -DshowCases
```

Màn hình sẽ hiện:

```
  Gia tri bien - trinh bay theo mau slide 23 (bo day du n bien)
  Standard BVA | 4n+1 = 13 case | moi bo deu phai HOP LE
     1. [PASS]  TC1 | "password = min" | (fullName=50, email=75, password=6)
     2. [PASS]  TC2 | "password = min+" | (fullName=50, email=75, password=7)
     3. [PASS]  TC3 | "TAT CA nominal" | (fullName=50, email=75, password=18)
     ...
  Robustness BVA | 6n+1 = 19 case | them min- va max+
    14. [PASS]  TC14 | "password = min-" | ky vong hop le = false
```

Số thứ tự và nội dung khớp đúng bảng thiết kế ở sheet `01`, `03*`, `04*` của báo cáo
Excel — đối chiếu trực tiếp được, không cần mở mã nguồn.

---

## 2 · Kiểm thử hộp trắng

### 2.1 Chín đường cơ sở của bảng B1

```bash
./mvnw test -Dtest='RoadmapServiceTest#calculatePercent_whenTotalZero_returnsZero+isNodeLocked_whenTierOne_alwaysUnlocked+lockExpression_coversConditionCombinations'
```

→ **9 test** (1 + 1 + 7 bộ tham số)

Ánh xạ sang bảng B1 — cột *Test Method* của sheet `B1_CFG_va_Basis_Path` ghi đúng các
tên này:

| Đường | Phương thức |
|---|---|
| `calculatePercent()` P1 · P2 | `calculatePercent_whenTotalZero_returnsZero` · `calculatePercent_roundsToTwoDecimals` |
| `isNodeLocked()` P1 · P2 | `isNodeLocked_whenTierIsNull_treatsNodeAsTierOne` · `isNodeLocked_whenTierOne_alwaysUnlocked` |
| `isNodeLocked()` P3 · P4 | `isNodeLocked_whenAllLowerTiersDone_returnsUnlocked` · `isNodeLocked_whenLowerTierNotAllDone_returnsLocked` |
| `isNodeLocked()` P5 · P6 · P7 | `lockExpression_coversConditionCombinations` — ba bộ tham số về tầng 3 |

### 2.2 Ma trận tổ hợp điều kiện MC/DC của bảng B2

```bash
./mvnw test -Dtest='RoadmapServiceTest#lockExpression_coversConditionCombinations'
```

→ **7 test**, đúng 7 dòng của ma trận MC/DC ở sheet `B2_Coverage_Levels`.

### 2.3 Cả lớp chứa phân tích hộp trắng

```bash
./mvnw test -Dtest='RoadmapServiceTest'
```

→ **28 test**

---

## 3 · Toàn bộ và độ bao phủ

```bash
./mvnw clean test
```

→ **399 test, 0 thất bại**

Báo cáo JaCoCo tự sinh (goal `report` gắn vào phase `test`, không cần gõ thêm lệnh):

```
target/site/jacoco/index.html
```

→ **Dòng 78,6% · Nhánh 72,5%**

> **Bẫy cần tránh khi demo.** JaCoCo chỉ đo những gì lượt chạy vừa chạm tới. Nếu chạy
> `./mvnw clean test -Dtest=RegisterStandardBvaTest` rồi mở báo cáo bao phủ, kết quả ra
> **0,0%** — vì test giá trị biên chỉ gọi `validator.validate(dto)`, phần thực thi nằm
> trong thư viện Hibernate Validator chứ không phải mã dự án. Muốn khoe bao phủ thì
> **phải chạy toàn bộ**, đừng lọc `-Dtest`.
>
> Lối an toàn hơn: mở bản chụp đã commit sẵn `docs/coverage/index.html` — luôn đúng số,
> không phụ thuộc vừa chạy lệnh gì.

---

## 4 · Kiểm thử API

Cần ứng dụng chạy bằng Docker (mục 0).

### Cách demo: Postman GUI

1. Mở Postman → Import → chọn `CareerCompass.postman_collection.json`
2. Import tiếp `CareerCompass.postman_environment.json`
3. Chọn environment **CareerCompass** ở góc phải trên
4. Bấm **Run collection**

→ **112 request · 358 phép kiểm · 0 thất bại**
(trong đó 92 trường hợp kiểm thử thiết kế + 20 bước chuẩn bị)

### Đừng dùng newman cho buổi demo

```bash
npx newman run CareerCompass.postman_collection.json -e CareerCompass.postman_environment.json
```

Lệnh này báo **4–5 lỗi `SyntaxError: Identifier 'data' has already been declared`**, ở
các request `TC-RM-02`, `TC-RM-03`, `TC-SG-01`, `TC-SG-03`.

Nguyên nhân: năm request khai báo `const data = pm.response.json()` ở cấp cao nhất của
script. Postman GUI tạo scope riêng cho từng request nên không sao; newman dùng chung
một sandbox cho cả lượt chạy nên request thứ hai trở đi vấp khai báo trùng.

**Đây là khác biệt giữa hai công cụ chạy, không phải lỗi ứng dụng và cũng không liên
quan tới Docker.** Muốn dùng được newman thì phải sửa năm script đó (đổi `const data`
thành tên khác nhau, hoặc bọc trong `(function(){ ... })()`).

---

## 5 · Kiểm thử giao diện đầu-cuối

Cần ứng dụng chạy bằng Docker (mục 0). Chạy trong thư mục `e2e/`:

```bash
cd e2e
npm install          # chỉ cần lần đầu
```

### 5.1 Chạy ngầm — không mở trình duyệt

```bash
npm run test:all
```

→ **17 kịch bản, 17 pass**, khoảng 90 giây

### 5.2 Chạy hiện trình duyệt — để thầy nhìn thấy thao tác

```bash
SHOW=true npx codeceptjs run --steps
```

Trên PowerShell (Windows), biến môi trường đặt khác:

```powershell
$env:SHOW="true"; npx codeceptjs run --steps
```

Cửa sổ Chromium sẽ mở và tự thao tác: điền form, bấm nút, chuyển trang. Cờ `--steps`
in từng bước ra màn hình.

Muốn xem một kịch bản thôi cho nhanh:

```bash
SHOW=true npx codeceptjs run --grep "TC-ADM-003" --steps
```

### 5.3 Các lệnh lọc khác

| Lệnh | Chạy gì |
|---|---|
| `npm run test:all` | Cả 17 kịch bản |
| `npm test` | 16 kịch bản — **bỏ qua** kịch bản gắn `@known-bug` |
| `npm run test:smoke` | Nhóm `@smoke` — kiểm nhanh hệ thống còn sống |
| `npm run test:p0` | Nhóm ưu tiên cao nhất |

> Báo cáo ghi **17 kịch bản**, nên khi demo hãy dùng `npm run test:all`.
> `npm test` chỉ ra 16 vì nó lọc bỏ `TC-ADM-003` (gắn nhãn `@known-bug` từ hồi khiếm
> khuyết DEF-001 chưa được sửa; nay đã sửa và kịch bản này pass).

---

## 6 · Bảng tra nhanh

| Phần | Lệnh | Kết quả |
|---|---|---|
| Hộp đen, cả bốn kỹ thuật | `./mvnw test -Dtest='...'` (mục 1.1) | 130 test |
| Giá trị biên | `./mvnw test -Dtest='RegisterStandardBvaTest,RegisterFormDTOBvaTest,OnboardingFileSizeBvaTest'` | 52 test |
| Phân hoạch lớp tương đương | `./mvnw test -Dtest='RegisterEquivalencePartitionTest,RegisterTagCoverageTest'` | 31 test |
| Bảng quyết định | `./mvnw test -Dtest='ProgressDecisionTableTest,TokenValidityDecisionTableTest,TranscriptFileDecisionTableTest'` | 19 test |
| Chuyển đổi trạng thái | `./mvnw test -Dtest='ProgressStateTransitionTest,TokenStateTransitionTest,OnboardingStateTransitionTest'` | 28 test |
| Hộp trắng — đường cơ sở | `./mvnw test -Dtest='RoadmapServiceTest#...'` (mục 2.1) | 9 test |
| Hộp trắng — MC/DC | `./mvnw test -Dtest='RoadmapServiceTest#lockExpression_coversConditionCombinations'` | 7 test |
| Toàn bộ + bao phủ | `./mvnw clean test` | 399 test · 78,6% · 72,5% |
| API | Postman GUI → Run collection | 112 request · 358 phép kiểm |
| Giao diện, chạy ngầm | `cd e2e && npm run test:all` | 17 kịch bản |
| Giao diện, hiện trình duyệt | `cd e2e && SHOW=true npx codeceptjs run --steps` | 17 kịch bản |

---

## 7 · Khi số không khớp

**Số test ít hơn mong đợi** — nhiều khả năng gõ sai tên lớp trong `-Dtest`. Maven không
báo lỗi mà lặng lẽ chạy ít test hơn. Đếm lại số ở bảng trên.

**`BUILD FAILURE` ở lệnh có `clean`** — ứng dụng đang chạy và giữ thư mục `target/`.
Dừng ứng dụng rồi chạy lại.

**Bao phủ ra 0% hoặc thấp bất thường** — do vừa chạy có lọc `-Dtest`. Xem cảnh báo ở
mục 3.

**Kiểm thử API báo đỏ ở phép kiểm NFR-S05 (lộ stack trace)** — đang chạy ứng dụng bằng
IntelliJ chứ không phải Docker. Xem mục 0.

**Kiểm thử API báo `SyntaxError: Identifier 'data'`** — đang dùng newman. Dùng Postman
GUI. Xem mục 4.

**Kiểm thử giao diện đỏ vài kịch bản ở lần chạy đầu** — máy chủ còn nguội, một bước quá
hạn chờ rồi kéo đổ các kịch bản sau. Chạy lại lần hai.

**Kiểm thử giao diện chạy được nhưng chậm/chập chờn** — đang trỏ vào máy chủ từ xa thay
vì `localhost`. Bộ kiểm thử mặc định dùng `http://localhost:8080`; chỉ khi đặt biến
`BASE_URL` nó mới đổi đích.

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

### 1.1 Chạy trọn phần hộp đen

```bash
./mvnw test -Dtest='RegisterStandardBvaTest,RegisterTagCoverageTest,RegisterEquivalencePartitionTest,OnboardingFileSizeBvaTest,PasswordPolicyBvaTest,TokenExpiryBvaTest,ProgressDecisionTableTest,TokenValidityDecisionTableTest,TranscriptFileDecisionTableTest,ProgressStateTransitionTest,TokenStateTransitionTest,OnboardingStateTransitionTest'
```

→ **126 test, 0 thất bại** — đúng bằng tổng các sheet hộp đen của báo cáo.

### 1.2 Chạy theo từng SHEET của báo cáo

Gom theo sheet chứ không gom theo kỹ thuật, để mỗi con số in ra đối chiếu thẳng
được với một sheet Excel mở bên cạnh.

| Sheet | Lệnh | Kết quả |
|---|---|---|
| `01. BVA + Equiv Partition` | `./mvnw test -Dtest='RegisterStandardBvaTest,RegisterTagCoverageTest,RegisterEquivalencePartitionTest,OnboardingFileSizeBvaTest'` | **56 test** |
| `03. Decision Table (PhanA)` | `./mvnw test -Dtest='ProgressDecisionTableTest'` | **6 test** |
| `03b. Decision Table Token` | `./mvnw test -Dtest='TokenValidityDecisionTableTest'` | **4 test** |
| `03c. Decision Table Tep` | `./mvnw test -Dtest='TranscriptFileDecisionTableTest'` | **9 test** |
| `04. State Transition (PhanA)` | `./mvnw test -Dtest='ProgressStateTransitionTest'` | **9 test** |
| `04b. State Transition Token` | `./mvnw test -Dtest='TokenStateTransitionTest'` | **7 test** |
| `04c. State Transition Onboard` | `./mvnw test -Dtest='OnboardingStateTransitionTest'` | **12 test** |
| `10. Ra soat BVA` | `./mvnw test -Dtest='PasswordPolicyBvaTest,TokenExpiryBvaTest'` | **23 test** |

> **Một lớp nằm ngoài bảng — nhớ để khỏi bị hỏi bất ngờ.**
> `RegisterFormDTOBvaTest` (**27 test**) cũng là kiểm thử giá trị biên và vẫn nằm trong
> mã nguồn, nhưng **không xuất hiện ở sheet nào**. Lý do đã ghi trong
> `tools/gen_sheet01.py`: nó chia mỗi trường thành một test case riêng, trong khi slide
> 23 và 33 luôn trình bày test case là **một bộ đầu vào đầy đủ**. Các giá trị biên nó
> kiểm đã được Bảng 2, 3, 5 của sheet 01 phủ hết.
>
> ```bash
> ./mvnw test -Dtest='RegisterFormDTOBvaTest'    # 27 test
> ```
>
> Cộng lại: 126 test trong các sheet + 27 test lớp này = **153 test hộp đen** trong mã
> nguồn. Nếu thầy đếm ra số khác con số trong báo cáo thì đây là chỗ giải thích.

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

→ **536 test, 0 thất bại**

Báo cáo JaCoCo tự sinh, **ba bản cùng lúc**, không cần gõ thêm lệnh:

| Thư mục | Đo gì | Dòng | Nhánh |
|---|---|---|---|
| `target/site/jacoco/` | **Toàn bộ** — gộp cả hai bên | **90,9%** | **85,9%** |
| `target/site/jacoco-whitebox/` | Chỉ test hộp trắng | 90,7% | 84,5% |
| `target/site/jacoco-blackbox/` | Chỉ test hộp đen | 7,4% | 11,6% |

> **Vì sao bản gộp và bản hộp trắng gần như bằng nhau.** Không phải lỗi cấu hình: bộ hộp
> đen hầu như không phủ thêm dòng nào mà hộp trắng chưa phủ (chỉ thêm 3 nhánh). Điều đó
> hợp lý — xem đoạn ngay dưới.

Con số trong báo cáo Excel là bản **gộp** — mở `target/site/jacoco/index.html`.

> **Một điểm đáng nói nếu thầy hỏi về bản tách.** Bộ kiểm thử hộp đen chạy riêng chỉ phủ
> **7,4% dòng**. Nghe thấp nhưng đúng bản chất: kỹ thuật hộp đen suy test case từ ĐẶC TẢ,
> không nhắm vào việc phủ mã, và phần lớn test hộp đen ở đây kiểm ràng buộc dữ liệu qua
> thư viện Hibernate Validator chứ không chạy vào mã dự án. Đây chính là minh hoạ cho
> slide 51: *độ bao phủ cao không đồng nghĩa test tốt, và test tốt không đồng nghĩa bao
> phủ cao.* Hai bộ bổ sung nhau — hộp trắng gánh phần bao phủ, hộp đen gánh phần đối
> chiếu với đặc tả và là nơi tìm ra phần lớn khiếm khuyết.

> **Bẫy cần tránh khi demo.** JaCoCo chỉ đo những gì lượt chạy vừa chạm tới. Nếu chạy
> `./mvnw clean test -Dtest=RegisterStandardBvaTest` rồi mở báo cáo bao phủ, kết quả ra
> **0,0%** — vì test giá trị biên chỉ gọi `validator.validate(dto)`, phần thực thi nằm
> trong thư viện Hibernate Validator chứ không phải mã dự án. Muốn khoe bao phủ thì
> **phải chạy toàn bộ**, đừng lọc `-Dtest`.
>
> Lối an toàn hơn: mở bản chụp đã commit sẵn `docs/coverage/index.html` — luôn đúng số,
> không phụ thuộc vừa chạy lệnh gì.

> **Nếu `target/site/jacoco/` không tồn tại** thì đang dùng bản `pom.xml` cũ hơn bước
> gộp. Chạy `git pull` rồi chạy lại. Hai thư mục `jacoco-whitebox` và `jacoco-blackbox`
> không cộng lại được thành con số tổng — cùng một dòng lệnh có thể được cả hai bên phủ,
> cộng vào là đếm hai lần. Bắt buộc phải có bước merge ở mức dữ liệu.

---

## 4 · Kiểm thử API

Cần ứng dụng chạy bằng Docker (mục 0).

### Cách demo: Postman GUI

1. Mở Postman → Import → chọn `CareerCompass.postman_collection.json`
2. Import tiếp `CareerCompass.postman_environment.json`
3. Chọn environment **CareerCompass** ở góc phải trên
4. Bấm **Run collection**

→ **111 request · 376 phép kiểm · 0 thất bại**
(trong đó 94 trường hợp kiểm thử thiết kế + 17 bước chuẩn bị. Newman báo *112 lượt gửi* vì một request bị gửi lại theo chuyển hướng.)

### Cách khác: newman (dòng lệnh)

```bash
npx newman run CareerCompass.postman_collection.json -e CareerCompass.postman_environment.json
```

→ cùng ra **376 phép kiểm · 0 thất bại**, khoảng 70 giây.

> **Một khiếm khuyết của chính bộ kiểm thử, đáng kể khi thầy hỏi.** Trước ngày 16/09,
> lệnh trên báo `SyntaxError: Identifier 'data' has already been declared` ở bốn request
> `TC-RM-02`, `TC-RM-03`, `TC-SG-01`, `TC-SG-03`, và chỉ đếm được **358** phép kiểm.
>
> Nguyên nhân: bốn request cùng khai báo `const data = pm.response.json()` ở cấp cao
> nhất. Postman GUI cho mỗi request một scope riêng nên không sao; newman dùng CHUNG một
> sandbox cho cả lượt chạy, nên từ request thứ hai trở đi là lỗi cú pháp và **toàn bộ
> `pm.test` của request đó không chạy** — đúng 18 phép kiểm biến mất.
>
> Điều nguy hiểm: newman vẫn báo **`0 failed`**. Phép kiểm không chạy thì không tính là
> trượt. Nếu chỉ nhìn dòng "0 failed" thì không ai biết mình vừa mất 18 phép kiểm.
>
> Đã sửa bằng cách đổi `const` thành `var` ở các chỗ trùng tên. Nay newman và Postman GUI
> ra cùng một con số, demo bằng công cụ nào cũng được.

---

## 5 · Kiểm thử giao diện đầu-cuối

Cần ứng dụng chạy bằng Docker (mục 0).

**Mọi lệnh ở mục này phải chạy TRONG thư mục `e2e/`** — tệp cấu hình
`codecept.conf.js` nằm ở đó. Mở terminal mới là phải `cd e2e` lại.

```bash
cd e2e
npm install          # chỉ cần lần đầu
```

> **Chạy nhầm ở thư mục gốc sẽ ra lỗi này:**
>
> ```
> Error: Can not load config from ...\lap-trinh-java\codecept.conf.js
> CodeceptJS is not initialized in this dir. Execute 'codeceptjs init' to start
> ```
>
> **Đừng chạy `codeceptjs init`** như nó gợi ý — sẽ ghi đè cấu hình của dự án. Chỉ cần
> `cd e2e` rồi chạy lại. Nhìn đường dẫn trong thông báo lỗi là biết: nó đang tìm tệp
> cấu hình ở thư mục gốc chứ không phải ở `e2e/`.

### 5.1 Chạy ngầm — không mở trình duyệt

```bash
npm run test:all
```

→ **17 kịch bản, 17 pass**, khoảng 90 giây

### 5.2 Chạy hiện trình duyệt — để thầy nhìn thấy thao tác

```bash
SHOW=true npx codeceptjs run --steps
```

Cách đặt biến môi trường khác nhau theo từng loại terminal:

| Terminal | Lệnh |
|---|---|
| Git Bash | `SHOW=true npx codeceptjs run --steps` |
| PowerShell | `$env:SHOW="true"; npx codeceptjs run --steps` |
| cmd | `set SHOW=true && npx codeceptjs run --steps` |

Cả ba đều đã chạy thử được, miễn là đang đứng trong `e2e/`.

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
| Hộp đen — toàn bộ các sheet | `./mvnw test -Dtest='...'` (mục 1.1) | 126 test |
| Sheet 01 · BVA + phân hoạch | `./mvnw test -Dtest='RegisterStandardBvaTest,RegisterTagCoverageTest,RegisterEquivalencePartitionTest,OnboardingFileSizeBvaTest'` | 56 test |
| Sheet 03 · 03b · 03c | `./mvnw test -Dtest='ProgressDecisionTableTest'` · `'TokenValidityDecisionTableTest'` · `'TranscriptFileDecisionTableTest'` | 6 · 4 · 9 |
| Sheet 04 · 04b · 04c | `./mvnw test -Dtest='ProgressStateTransitionTest'` · `'TokenStateTransitionTest'` · `'OnboardingStateTransitionTest'` | 9 · 7 · 12 |
| Ngoài bảng · BVA theo từng trường | `./mvnw test -Dtest='RegisterFormDTOBvaTest'` | 27 test |
| Hộp trắng — đường cơ sở | `./mvnw test -Dtest='RoadmapServiceTest#...'` (mục 2.1) | 9 test |
| Hộp trắng — MC/DC | `./mvnw test -Dtest='RoadmapServiceTest#lockExpression_coversConditionCombinations'` | 7 test |
| Toàn bộ + bao phủ gộp | `./mvnw clean test` | 536 test · 90,9% · 85,9% |
| Bao phủ riêng hộp trắng / hộp đen | `target/site/jacoco-whitebox/` · `jacoco-blackbox/` | 90,7% / 7,4% dòng |
| API | Postman GUI → Run collection | 111 request · 376 phép kiểm |
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

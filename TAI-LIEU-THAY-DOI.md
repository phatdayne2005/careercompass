# Tài liệu thay đổi — Đợt tích hợp & hoàn thiện toàn dự án

> **Mục đích:** tổng hợp mọi thay đổi trong đợt review/tích hợp code 7 gói (P1–P7) để cả nhóm
> nắm được project hiện tại đã khác gì so với code từng người push ban đầu — tiện review lại,
> cập nhật kiến thức, và viết báo cáo môn học.
>
> **Nhánh:** `integration/review-fixes` → đã merge vào `main` (`a50d893`).
> **Phạm vi:** 20+ commit, 83 file, sửa lỗi tích hợp giữa các gói + hoàn thiện phần còn thiếu.

---

## 0. Tóm tắt nhanh (đọc cái này trước)

| Hạng mục | Trạng thái sau đợt này |
|---|---|
| 7 gói P1–P7 | Chạy được + tích hợp với nhau |
| AI (Mentor, tóm tắt GitHub, scan CV) | Hoạt động thật với Gemini |
| Export PDF (Skill Gap) | Xuất PDF thật (OpenPDF) |
| Reset mật khẩu (`/forgot`) | Hoàn thiện end-to-end |
| Bug tích hợp giữa các gói | Đã sửa (chi tiết mục 2) |

**3 loại lỗi lặp lại nhiều nhất — cả nhóm cần nắm (mục 3):**
1. `LazyInitializationException` do `open-in-view=false` (dính 3 chỗ).
2. `@AuthenticationPrincipal User` bị null khi principal không phải `User`.
3. Template Thymeleaf gọi field/quan hệ **không tồn tại** trên entity → 500.

---

## 1. Thay đổi HỢP ĐỒNG DỮ LIỆU (⚠️ cả nhóm PHẢI biết)

Đây là các thay đổi về entity/field mà **nhiều gói dùng chung** — nếu code bạn còn tham chiếu tên cũ sẽ lỗi.

| Entity | Thay đổi | Ảnh hưởng gói |
|---|---|---|
| `SkillNode` (P7) | Field chuẩn hoá: dùng `tier` (không phải `level`), `parent` (không phải `parentNode`), `template` (không phải `skillTreeTemplate`) | P4 render tree, P7 editor |
| `SkillNode` | **KHÔNG có** `@OneToMany learningResources` — muốn lấy tài liệu của node phải query `LearningResourceRepository.findBySkillNode_IdOrderByIdAsc(nodeId)` | P4, P7 |
| `User` (P1) | Thêm `careerRole` (@ManyToOne) + `targetRoleId` (đọc gián tiếp); thêm `transcriptSummary` (TEXT) cho scan CV | P2, P3, P4 |
| `User` | Thêm `onboardingCompleted` (boolean) | P2 |
| `PasswordResetToken` (P1) | **Entity MỚI** — bảng `password_reset_tokens` | P1 |
| `SkillTreeTemplate` (P7) | Có `targetRoleId` (Long) + `careerRole` (@ManyToOne read-only) trỏ cùng cột `target_role_id` | P4 |

**Bảng DB mới sinh:** `password_reset_tokens`, cùng dữ liệu seed cho `career_role`, `skills`, `skill_tree_templates`, `skill_nodes`, `learning_resources`, `job_trends`.

---

## 2. Thay đổi theo từng gói

### P1 — Kernel + Auth (Leader)
- **`/forgot` Reset mật khẩu (MỚI, hoàn thiện):** thêm `PasswordResetToken` entity + `PasswordResetTokenRepository` + `PasswordResetService` (sinh token UUID hạn 30′, gửi link qua email, validate, đổi mật khẩu BCrypt) + endpoint `POST /forgot`, `GET/POST /reset-password` trong `AuthController` + view `reset-password.html` + `permitAll` cho `/reset-password`. Bảo mật: token dùng 1 lần, hết hạn, không tiết lộ email tồn tại, chỉ user LOCAL.
- **Config:** khôi phục Java 21, fail-fast cho secret (bỏ default giả kiểu `${MAIL_PASSWORD:test}`), thêm `app.base-url`.

### P2 — Onboarding *(core do thành viên P2 viết; đợt này chỉ chỉnh tích hợp)*
- Bước 2 (upload transcript): sau khi lưu file → gọi **scan CV** (AI phân tích, xem P3). File: `OnboardingController`, thêm `TranscriptAnalysisService`.
- Bước 3 (chọn skill): nhóm skill theo `category` cho dễ nhìn. File: `onboarding/step3_skills.html`.

### P3 — AI Mentor + Profile
- **Sửa Mentor/Profile vỡ:** `@AuthenticationPrincipal User` trả null (principal thật là `CustomUserDetails`/`OidcUser`) → chuyển sang `AuthenticatedUserService.requireCurrentUser(authentication)`. Sửa `chat.html` dùng biến `session` (từ khoá Thymeleaf) → đổi `sess`.
- **Sửa Mentor AI không trả lời:** thực chất là `LazyInitializationException` (gọi `session.getUser()` trên entity detached) — KHÔNG phải lỗi API key. Sửa: truyền `user` đã load thẳng vào `buildPrompt`; thêm **retry** khi Gemini trả 503/429; đổi model `gemini-2.5-flash-lite`.
- **Scan CV (MỚI, FR1.3):** `TranscriptAnalysisService` — trích text PDF (OpenPDF) → Gemini phân tích điểm mạnh/định hướng → lưu `User.transcriptSummary` → hiện ở Profile + nạp vào prompt Mentor.
- File chính: `MentorService`, `LlmClient`, `MentorController`, `profile/settings.html`.

### P4 — Roadmap + Skill Gap
- **Export PDF thật (FR3.3):** `PdfService` viết lại dùng **OpenPDF** (trước là file `.txt` stub) — sinh PDF A4 có font Unicode (tiếng Việt) + endpoint tải `GET /skill-gap/reports/{id}/download`.
- **Sửa Skill Gap crash:** trang vỡ `LazyInitializationException` khi user có ≥1 skill (`currentSkills` chứa lazy `Skill` proxy render ngoài session). Sửa: `UserSkillRepository.findByUserWithSkill` (JOIN FETCH).
- **Roadmap:** hiện link học **thật** (≥2 link/node) từ dữ liệu seed; `ProgressService` ghi log `NODE_DONE`.
- File chính: `PdfService`, `SkillGapPageController`, `RoadmapService`, `ProgressService`, `roadmap/index.html`, `skillgap/index.html`.

### P5 — Market Pulse + Dashboard *(core do thành viên P5 viết; đợt này tích hợp + bổ sung)*
- Giải conflict khi merge (giữ Java 21, dùng Dashboard bản của P5 tại `/dashboard`).
- **`JobTrendSeeder` (MỚI):** seed 16 tin tuyển dụng mẫu để biểu đồ Market Pulse luôn có data demo (scraper thật `@Scheduled` thường trả rỗng).
- File chính: `market/pulse.html`, `dashboard/home.html`, `JobTrendSeeder`.

### P6 — E-Portfolio (build từ khung sườn)
- Dựng hoàn chỉnh: sync repo GitHub thật của user → **AI tóm tắt README** (Gemini, có fallback) → trang chia sẻ công khai `/p/{slug}` (URL duy nhất, chống trùng).
- File chính: `PortfolioService`, `PublicPortfolioController`, `portfolio/manage.html`, `portfolio/public.html`.

### P7 — Admin + Counselor
- **`deleteTemplate` crash FK:** xoá template ném lỗi khoá ngoại → sửa xoá đúng thứ tự (resource → node → template).
- **Counselor editor vỡ runtime:** template gọi `node.level`/`parentNode`/`skillTreeTemplate` (tên field sai) → sửa về `tier`/`parent`/`template` + cho phép chọn tier (3 tầng).
- **Counselor không thêm được tài liệu (500):** template gọi `currentNode.learningResources` — `SkillNode` không có quan hệ này → sửa load resources riêng qua repository + helper `populateNodeDetails` cho cả 3 endpoint.
- **Bỏ anti-pattern:** gỡ các `try/catch` nuốt lỗi trong service Counselor + thêm flash message thật.
- **Seed:** tách seeder P7 ra khỏi kernel, bỏ auto-wipe DB + bỏ scrape lúc boot (rất nguy hiểm); thêm 6 lộ trình curate (node = skill cụ thể, 3 tầng, ≥2 link).
- File chính: `CounselorTemplateController`, `AdminUserService`, `counselor/editor.html`, các seeder + `data/roadmaps/*.json`.

### Giao diện chung (mọi trang student)
- Đồng bộ theme tối (#0F172A), sửa HTMX double-load, **giảm cỡ chữ/nav bị to** (nav cao 86→64px, roadmap/skillgap font-spacing ~30%), sửa link "AI Mentor" trên nav bị chết (`href="#"` → `/mentor`).
- File chính: `layout/base.html`.

---

## 3. ⭐ Kiến thức mới cần nắm (quan trọng cho báo cáo)

### 3.1 `LazyInitializationException` + `open-in-view=false`
Dự án tắt Open-Session-In-View (đúng best practice). Hệ quả: **không được truy cập quan hệ LAZY ở tầng View (Thymeleaf) hoặc trên entity đã detached** — session Hibernate đã đóng.
- **Đã dính 3 lần:** Mentor (`session.getUser()`), Skill Gap (`currentSkills` lazy), và trước đó.
- **Cách xử lý:** dùng `@Query ... JOIN FETCH` để nạp sẵn quan hệ, HOẶC truyền entity đã-load vào, HOẶC lấy dữ liệu cần ngay trong tầng service (còn session).

### 3.2 `@AuthenticationPrincipal User` KHÔNG phải lúc nào cũng là `User`
Khi đăng nhập, principal thật là `CustomUserDetails` (form login) hoặc `OidcUser` (Google) — **không phải** entity `User`. Tiêm thẳng `@AuthenticationPrincipal User` → null.
- **Cách đúng:** `authenticatedUserService.requireCurrentUser(authentication)`.

### 3.3 `@Transactional` + dirty-checking (không cần `save()`)
Trong method `@Transactional`, entity đã load là *managed* — sửa field xong Hibernate tự sinh `UPDATE` lúc commit, **không cần gọi `save()`**. (Xem `PasswordResetService.resetPassword` set `used=true`.)

### 3.4 Template chỉ gọi được field/quan hệ CÓ TỒN TẠI trên entity
Thymeleaf gọi `currentNode.learningResources` mà `SkillNode` không khai `@OneToMany` → lỗi SpEL 500. Muốn dùng collection thì hoặc khai quan hệ, hoặc query riêng rồi đưa vào model.

### 3.5 Hạ tầng dùng chung mới
- **`LlmClient`** (Gemini): gọi LLM + retry 503/429 — dùng cho Mentor, Portfolio, scan CV.
- **`EmailService`/`SmtpEmailService`**: gửi mail SMTP — dùng cho reset password.
- **OpenPDF** (`com.github.librepdf:openpdf`): sinh + đọc PDF (Export Skill Gap, trích text CV).

---

## 4. Cách chạy + cấu hình

```bash
./mvnw.cmd spring-boot:run          # port 8080
```
- DB tự tạo (`ddl-auto: update`), MySQL local `root/123456`, DB `careercompass`.
- **Secret** đặt trong `application-local.yml` (ở gốc project, đã gitignore):
  `GEMINI_API_KEY`, `GOOGLE_CLIENT_ID/SECRET`, `MAIL_USERNAME/PASSWORD`.
- Tài khoản seed: `admin@gmail.com` / `student@gmail.com` / `counselor@gmail.com` (mật khẩu `123456`).

---

## 5. Danh sách commit (theo thứ tự)

```
a50d893 feat(p1): hoàn thiện Quên & Đặt lại mật khẩu (FR6.1) - end-to-end
80c601a fix(p4): skill-gap crash khi user có skill (LazyInitializationException)
e95f759 fix(p7): counselor editor - quản lý tài liệu node bị 500 (learningResources)
4dc2505 feat(p3/p2): Scan CV - AI phân tích bảng điểm (FR1.3)
12b5886 polish(ui): giảm cỡ chữ/spacing bị to + sửa nav link AI Mentor chết
8e134a2 fix(p3): Mentor AI thật hoạt động - sửa LazyInitializationException + retry LLM
ed507fb fix(p4): Export PDF thật (OpenPDF) + endpoint tải (FR3.3)
7f93061 polish(ui): đồng bộ giao diện sau QA toàn dự án
da5d397 feat(p5): seed JobTrend mẫu cho Market Pulse
7058c90 fix(p3): sửa Mentor + Profile vỡ do user null + LLM lỗi chặn chat
9df4aa4 feat(p6): build E-Portfolio thật - sync GitHub + AI tóm tắt + public share
5c47601 refactor(p7): bỏ anti-pattern nuốt lỗi + flash message thật cho counselor
ea70521 fix(p7): sửa counselor editor vỡ runtime + cho set tier (3 tầng)
2d444f3 fix(p7): deleteTemplate xoá đúng chuỗi khoá ngoại, không crash
baf688e feat(roadmap+onboarding): hiện link học thật + nhóm skill theo category
10f2fd4 feat(seed): 6 lộ trình curate (node=skill cụ thể, 3 tầng, >=2 link)
4d1af36 refactor(seed): tách seeder P7 khỏi kernel, bỏ auto-wipe + scrape lúc boot
e674da9 fix(config): khôi phục Java 21 + fail-fast cho secret
8e79a72 feat(p5): build Dashboard (Màn ③)
```
*(Các commit `Merge ...`, `Implement P5 ...` là code gốc của thành viên P5, đưa vào qua PR#7.)*

---

*Tài liệu này sinh trong đợt review/tích hợp. Có gì chưa rõ về một thay đổi, xem commit tương ứng bằng `git show <hash>`.*

# Triển khai CareerCompass lên VPS

Kiến trúc chạy thật:

```
Internet --443--> nginx (host) --127.0.0.1:8080--> container app --> container mysql
                                                                        |
                                                              volume db-data (dữ liệu)
                                                              volume uploads (bảng điểm)
```

App chỉ bind loopback (nginx hứng traffic public). MySQL **có** mở port ra ngoài để kết nối bằng
MySQL Workbench — xem mục 9 kèm cảnh báo bảo mật.

Yêu cầu VPS: Ubuntu 22.04/24.04, **RAM tối thiểu 2GB** (build Maven trong Docker khá ngốn RAM;
nếu VPS 1GB thì tạo swap 2GB trước, xem mục Khắc phục sự cố).

---

## 1. Cài Docker trên VPS

```bash
sudo apt update && sudo apt upgrade -y
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER
newgrp docker          # hoặc đăng xuất/đăng nhập lại
docker --version && docker compose version
```

## 2. Lấy source code

```bash
cd ~
git clone https://github.com/phatdayne2005/lap-trinh-java.git
cd lap-trinh-java
```

## 3. Tạo file .env

`.env` bị gitignore nên **không** có sẵn sau khi clone — phải tạo thủ công:

```bash
cp .env.example .env
nano .env
```

Điền các giá trị (lấy từ `application-local.yml` ở máy dev):

| Biến | Ghi chú |
|---|---|
| `DB_PASSWORD` | Đặt mật khẩu mạnh. **Chỉ có tác dụng ở lần chạy đầu** — MySQL khởi tạo volume 1 lần. Port 3306 mở ra internet nên mật khẩu này là lớp bảo vệ duy nhất. |
| `DB_PORT` | Port host cho MySQL Workbench. VPS để `3306`. |
| `BASE_URL` | `https://YOUR_DOMAIN` — dùng để sinh link trong email reset password |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | Google Cloud Console |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | Gmail + App Password 16 ký tự |
| `GEMINI_API_KEY` | Google AI Studio |
| `ADMIN_*` / `STUDENT_*` / `COUNSELOR_*` | Tài khoản seed lần đầu — **đổi mật khẩu khác `123456`** |

```bash
chmod 600 .env      # chỉ chủ sở hữu đọc được
```

## 4. Chạy stack

```bash
docker compose up -d --build      # lần đầu build ~3-5 phút
docker compose ps                 # cả 2 container phải Up, db phải (healthy)
docker compose logs -f app        # chờ dòng "Started CareerCompassApplication"
curl -I http://127.0.0.1:8080/login   # kỳ vọng HTTP 200
```

## 5. Nginx + HTTPS

Trỏ **A record** của domain về IP VPS trước, kiểm tra bằng `dig +short YOUR_DOMAIN`.

```bash
sudo apt install -y nginx
sudo cp deploy/nginx/careercompass.conf /etc/nginx/sites-available/careercompass
sudo sed -i 's/YOUR_DOMAIN/ten-mien-that.com/' /etc/nginx/sites-available/careercompass
sudo ln -sf /etc/nginx/sites-available/careercompass /etc/nginx/sites-enabled/careercompass
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t && sudo systemctl reload nginx
```

Cấp chứng chỉ Let's Encrypt (certbot tự sửa file config thêm block 443 + redirect):

```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d ten-mien-that.com --agree-tos -m email-cua-ban@gmail.com --redirect
sudo certbot renew --dry-run     # kiểm tra auto-gia hạn
```

## 6. Firewall

```bash
sudo ufw allow OpenSSH
sudo ufw allow 'Nginx Full'
sudo ufw allow 3306/tcp          # MySQL Workbench - xem cảnh báo ở mục 9
sudo ufw enable
sudo ufw status
```

Không cần mở 8080 — compose đã bind loopback, nginx proxy nội bộ.

**Khuyến nghị mạnh:** thay `sudo ufw allow 3306/tcp` bằng rule chỉ cho IP của bạn,
như vậy vẫn dùng Workbench được mà bot quét port không đụng tới:

```bash
curl -s ifconfig.me                        # xem IP hiện tại của bạn
sudo ufw allow from <IP_CUA_BAN> to any port 3306 proto tcp
```

Nhược điểm: mạng nhà/4G thường đổi IP động, đổi IP là phải thêm rule mới.

## 7. Cập nhật Google OAuth

Google Cloud Console → APIs & Services → Credentials → OAuth 2.0 Client ID:

- **Authorized JavaScript origins**: `https://ten-mien-that.com`
- **Authorized redirect URIs**: `https://ten-mien-that.com/login/oauth2/code/google`

Thiếu bước này thì nút "Đăng nhập với Google" báo `redirect_uri_mismatch`.

## 8. Kiểm tra sau deploy

- [ ] `https://ten-mien-that.com/login` mở được, có ổ khóa HTTPS
- [ ] Đăng nhập tài khoản seed `ADMIN_EMAIL`
- [ ] Đăng nhập Google
- [ ] Quên mật khẩu → email về được (test SMTP)
- [ ] Onboarding upload bảng điểm PDF (test volume uploads + giới hạn 12M của nginx)
- [ ] Mentor AI trả lời (test GEMINI_API_KEY)
- [ ] Skill Gap → xuất PDF
- [ ] Market Pulse tải được job trend (VPS phải ra được internet)

## 9. Kết nối MySQL Workbench

Container MySQL đã publish port ra ngoài (`DB_PORT` trong `.env`).

Trong Workbench → **MySQL Connections** → dấu `+`:

| Trường | Giá trị |
|---|---|
| Connection Name | CareerCompass VPS |
| Hostname | IP VPS (hoặc domain) |
| Port | `3306` (trên máy dev local là `3307`) |
| Username | `root` |
| Password | giá trị `DB_PASSWORD` trong `.env` |
| Default Schema | `careercompass` |

### "Máy tôi đã có MySQL ở 3306 rồi, connect 3306 của VPS có bị trùng không?"

Không. Port chỉ trùng khi hai tiến trình cùng **lắng nghe** trên **cùng một máy**.

- MySQL cài trên laptop bạn lắng nghe `127.0.0.1:3306` — của **máy bạn**.
- Container trên VPS lắng nghe `0.0.0.0:3306` — của **máy VPS**.

Workbench nối tới `vps-ip:3306` là kết nối *đi ra ngoài*, không chiếm port nào ở máy bạn.
Cả team cùng connect một lúc vẫn bình thường. Trong Workbench chỉ cần tạo 2 connection
riêng: `localhost:3306` (MySQL máy mình) và `vps-ip:3306` (VPS).

Lý do duy nhất phải dùng `DB_PORT=3307` là khi chạy `docker compose` **ngay trên máy dev** —
lúc đó container mới thật sự tranh 3306 với MySQL cài sẵn.

Lưu ý: `DB_PORT` chỉ đổi port publish ra ngoài. App luôn nối tới `db:3306` qua mạng nội bộ
Docker, không phụ thuộc biến này.

Bấm **Test Connection**. Nếu lỗi:

- `Can't connect / timed out` → firewall chưa mở: `sudo ufw status`, kiểm tra rule 3306.
- `Access denied for user 'root'` → sai mật khẩu, hoặc `DB_PASSWORD` trong `.env` đã bị đổi
  **sau** khi volume `db-data` được tạo (MySQL chỉ đọc biến này lần khởi tạo đầu tiên).
- Kiểm tra nhanh từ chính VPS:
  ```bash
  docker compose exec db mysql -uroot -p"$(sed -n 's/^DB_PASSWORD=//p' .env | tr -d '\r"')" \
    -e "SELECT CURRENT_USER(); SHOW DATABASES;"
  ```

**Cảnh báo:** port 3306 mở ra internet cùng user `root` là mục tiêu quét/brute-force thường xuyên.
Chấp nhận được cho đồ án môn học với mật khẩu mạnh, nhưng:
- Đừng dùng lại mật khẩu này ở đâu khác.
- Nên giới hạn firewall theo IP (mục 6).
- Khi báo cáo/demo xong, đóng lại bằng `sudo ufw delete allow 3306/tcp`, hoặc bỏ block
  `ports:` của service `db` trong `docker-compose.yml`.
- Với hệ thống thật, cách đúng là **SSH tunnel** thay vì mở port — Workbench hỗ trợ sẵn
  (`Connection Method: Standard TCP/IP over SSH`), khi đó `db` không cần publish port nào cả.

---

## 10. CI/CD tự động (GitHub Actions)

Sau khi cấu hình xong mục này, **push vào `main` là tự động deploy** — không cần SSH vào VPS nữa.

```
push main ──> [test] 198 testcase + MySQL
                 │ pass
                 ▼
              [docker] build image, push lên ghcr.io
                 │
                 ▼
              [deploy] SSH vào VPS -> pull image -> up -d -> kiểm tra HTTP 200
```

Image được build **trên runner của GitHub**, VPS chỉ `pull`. Nhờ vậy VPS không cần RAM để chạy
Maven, và deploy chỉ mất vài chục giây thay vì vài phút.

Test fail → không build image. Build fail → không deploy. App không trả 200 sau khi deploy →
job báo đỏ kèm log.

### 10.1. Tạo SSH deploy key

Tạo key **riêng cho CI** (đừng dùng key cá nhân), trên máy dev:

```bash
ssh-keygen -t ed25519 -C "github-actions-careercompass" -f ~/.ssh/cc_deploy -N ""
```

Chép public key lên VPS:

```bash
ssh-copy-id -i ~/.ssh/cc_deploy.pub user@IP_VPS_CUA_BAN
# hoặc thủ công: nối nội dung cc_deploy.pub vào ~/.ssh/authorized_keys trên VPS
```

Kiểm tra vào được không: `ssh -i ~/.ssh/cc_deploy user@IP_VPS_CUA_BAN`

### 10.2. Khai báo secrets trên GitHub

Repo → **Settings** → **Secrets and variables** → **Actions** → *New repository secret*:

| Secret | Giá trị | Bắt buộc |
|---|---|---|
| `VPS_HOST` | `IP_VPS_CUA_BAN` | có |
| `VPS_USER` | user SSH trên VPS (vd `root`, `ubuntu`) | có |
| `VPS_SSH_KEY` | **toàn bộ nội dung** `~/.ssh/cc_deploy` (private key, gồm cả dòng `-----BEGIN...` và `-----END...`) | có |
| `VPS_PORT` | port SSH nếu khác `22` | không |
| `VPS_APP_DIR` | đường dẫn repo trên VPS nếu khác `~/lap-trinh-java` | không |

`GITHUB_TOKEN` **không cần tạo** — GitHub tự cấp cho mỗi lần chạy.

### 10.3. Chuẩn bị VPS một lần

VPS phải `git pull` được không cần nhập mật khẩu (repo public thì mặc định đã được), và
`.env` phải tồn tại sẵn (mục 3). Workflow chỉ ghi đè đúng dòng `APP_IMAGE`, các secret khác giữ nguyên.

### 10.4. Chạy thử

Vào tab **Actions** trên GitHub → chọn workflow → **Run workflow** (nhờ `workflow_dispatch`),
hoặc push một commit nhỏ vào `main`.

### 10.5. Rollback

Mỗi image được gắn tag theo SHA commit nên quay lại bản cũ rất nhanh:

```bash
cd ~/lap-trinh-java
docker images | grep careercompass                 # xem các SHA đang có
sed -i 's|^APP_IMAGE=.*|APP_IMAGE=ghcr.io/phatdayne2005/careercompass:<sha-cu>|' .env
docker compose up -d --no-build
```

Cách khác: vào GitHub Actions, mở lần chạy cũ đã xanh, bấm **Re-run all jobs**.

### 10.6. Nếu package GHCR để private

Mặc định image push lên GHCR là **private**. Workflow vẫn deploy được (nó tự `docker login`
bằng token tạm), nhưng khi bạn chạy `./deploy/deploy.sh` **thủ công** trên VPS thì phải đăng nhập:

```bash
echo <GITHUB_PAT_co_quyen_read:packages> | docker login ghcr.io -u <github-username> --password-stdin
```

Muốn khỏi phải login: GitHub → Packages → chọn `careercompass` → Package settings →
Change visibility → **Public**. Image không chứa secret (secret nằm ở `.env` lúc chạy),
nhưng có chứa mã nguồn đã biên dịch — cân nhắc nếu repo đang private.

---

## Các lần deploy sau

Bình thường **không cần làm gì** — push vào `main` là CI/CD tự lo (mục 10).

Deploy tay khi CI hỏng:

```bash
cd ~/lap-trinh-java
./deploy/deploy.sh              # pull image mới nhất từ GHCR (giống CI)
./deploy/deploy.sh --build      # build tại chỗ trên VPS (chậm, tốn RAM)
```

## Vận hành

```bash
docker compose logs -f app          # xem log
docker compose restart app          # khởi động lại app
docker compose down                 # dừng (GIỮ dữ liệu)
docker compose down -v              # dừng và XÓA SẠCH database - cẩn thận
```

Backup database:

```bash
docker compose exec db mysqldump -uroot -p"$(sed -n 's/^DB_PASSWORD=//p' .env | tr -d '\r"')" \
  --single-transaction careercompass > backup-$(date +%F).sql
```

Restore:

```bash
docker compose exec -T db mysql -uroot -p"$(sed -n 's/^DB_PASSWORD=//p' .env | tr -d '\r"')" \
  careercompass < backup-2026-07-31.sql
```

---

## Khắc phục sự cố

**Build bị kill / VPS 1GB RAM** — tạo swap trước khi build:

```bash
sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile
sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

**App restart liên tục** — `docker compose logs app --tail 50`. Thường gặp:
- `Could not resolve placeholder` → thiếu biến trong `.env`
- `Access denied for user 'root'` → đổi `DB_PASSWORD` sau khi volume đã tạo. Hoặc dùng lại mật khẩu cũ,
  hoặc `docker compose down -v` để tạo lại DB từ đầu (mất dữ liệu).

**Google OAuth `redirect_uri_mismatch`** — kiểm tra redirect URI ở bước 7, và biến
`SERVER_FORWARD_HEADERS_STRATEGY=framework` trong `docker-compose.yml` (đã set sẵn).

**502 Bad Gateway** — container app chưa lên: `docker compose ps` + `curl -I http://127.0.0.1:8080/login`.

**`curl: (3) URL using bad/illegal format`** trong job deploy — `.env` trên VPS lưu kiểu CRLF
(soạn/copy từ Windows), khiến giá trị đọc ra có ký tự `\r` ở cuối và URL thành
`http://127.0.0.1:8080\r/login`. Script đã tự lọc, nhưng nên chuẩn hoá luôn cho sạch:

```bash
sed -i 's/\r$//' .env
file .env          # kỳ vọng: "ASCII text", KHÔNG có "with CRLF line terminators"
```

Cũng đừng bọc giá trị trong dấu nháy (`APP_PORT="8080"`) — Docker Compose coi dấu nháy là
một phần của giá trị.

---

## Cảnh báo bảo mật

- `.env` và `application-local.yml` chứa secret thật, **đã gitignore — tuyệt đối không commit**.
- Mật khẩu seed mặc định `123456` chỉ dùng cho dev. Đổi trong `.env` trước khi chạy lần đầu.
- `spring.jpa.hibernate.ddl-auto: update` vẫn đang bật (tiện cho môn học). Với hệ thống thật
  nên chuyển sang `validate` + migration tool để Hibernate không tự sửa schema.

Feature('Xác thực tài khoản');

/**
 * Chiến lược dữ liệu: mỗi test tự đăng ký user mới với email duy nhất.
 * Nhờ vậy test không phụ thuộc dữ liệu có sẵn và chạy lại được nhiều lần.
 */

Scenario(
  'TC-AUTH-001 | Đăng ký tài khoản mới thành công @P0 @smoke',
  async ({ I, registerPage, loginPage }) => {
    const email = await I.uniqueEmail('newuser');

    registerPage.register('Nguyen Van E2E', email, 'MatKhau@123');

    // Đăng ký xong phải đăng nhập được bằng chính tài khoản vừa tạo.
    loginPage.login(email, 'MatKhau@123');
    I.dontSeeInCurrentUrl('/login?error');
  },
);

Scenario(
  'TC-AUTH-002 | Đăng ký bằng email đã tồn tại thì bị từ chối @P0',
  async ({ I, registerPage }) => {
    const email = await I.uniqueEmail('trung');

    registerPage.register('Nguoi Dung Thu Nhat', email, 'MatKhau@123');
    // Lần 2 dùng lại đúng email đó
    registerPage.register('Nguoi Dung Thu Hai', email, 'MatKhau@456');

    // Phải ở lại trang đăng ký kèm thông báo lỗi, không tạo thêm tài khoản.
    I.seeInCurrentUrl('/register');
  },
);

Scenario(
  'TC-AUTH-003 | Đăng nhập sai mật khẩu bị từ chối @P0 @smoke',
  ({ I, loginPage }) => {
    loginPage.login('student@gmail.com', 'mat-khau-sai-hoan-toan');
    loginPage.seeLoginFailed();
  },
);

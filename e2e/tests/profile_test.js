Feature('Hồ sơ cá nhân');

/** Module này từng vỡ runtime do user null (commit 7058c90). */

Scenario(
  'TC-PRF-001 | Đổi mật khẩu rồi đăng nhập lại bằng mật khẩu mới @P1',
  async ({ I, registerPage, loginPage, onboardingPage, profilePage }) => {
    const email = await I.uniqueEmail('prf');
    const oldPwd = 'MatKhau@123';
    const newPwd = 'MatKhauMoi@456';

    registerPage.register('Sinh Vien Doi MK', email, oldPwd);
    loginPage.login(email, oldPwd);
    onboardingPage.completeAll(2);

    profilePage.changePassword(oldPwd, newPwd);

    // Mật khẩu cũ phải hết hiệu lực...
    loginPage.login(email, oldPwd);
    loginPage.seeLoginFailed();

    // ...và mật khẩu mới phải dùng được.
    loginPage.login(email, newPwd);
    I.dontSeeInCurrentUrl('/login?error');
  },
);

Scenario(
  'TC-PRF-002 | Trang hồ sơ mở được và hiện đúng email đang đăng nhập @P1',
  async ({ I, registerPage, loginPage, onboardingPage, profilePage }) => {
    const email = await I.uniqueEmail('prf2');
    registerPage.register('Sinh Vien Ho So', email, 'MatKhau@123');
    loginPage.login(email, 'MatKhau@123');
    onboardingPage.completeAll(2);

    profilePage.open();

    I.seeInCurrentUrl('/profile');
    I.seeInField('input[name=email]', email);
  },
);

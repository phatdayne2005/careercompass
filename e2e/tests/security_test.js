Feature('Phân quyền và bảo mật truy cập');

Scenario(
  'TC-SEC-001 | Sinh viên truy cập trang quản trị thì bị chặn @P0 @smoke',
  async ({ I, registerPage, loginPage, adminUsersPage }) => {
    const email = await I.uniqueEmail('student');
    registerPage.register('Sinh Vien Thuong', email, 'MatKhau@123');
    loginPage.login(email, 'MatKhau@123');

    adminUsersPage.open();

    // Không được nhìn thấy danh sách người dùng.
    I.dontSeeElement(adminUsersPage.searchInput);
  },
);

Scenario(
  'TC-SEC-002 | Khách chưa đăng nhập vào trang trong bị đưa về đăng nhập @P0 @smoke',
  ({ I, dashboardPage, loginPage }) => {
    dashboardPage.open();
    loginPage.seeLoginPage();
  },
);

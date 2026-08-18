Feature('Trang tổng quan');

Scenario(
  'TC-DSB-001 | Dashboard hiển thị thống kê sau khi hoàn tất onboarding @P1',
  async ({ I, registerPage, loginPage, onboardingPage, dashboardPage }) => {
    const email = await I.uniqueEmail('dsb');
    registerPage.register('Sinh Vien Dashboard', email, 'MatKhau@123');
    loginPage.login(email, 'MatKhau@123');
    onboardingPage.completeAll(3);

    dashboardPage.open();
    dashboardPage.seeDashboard();

    // Dashboard gom dữ liệu từ roadmap + skill gap + activity log.
    I.see('node hoàn thành');
    I.see('kỹ năng còn thiếu');
  },
);

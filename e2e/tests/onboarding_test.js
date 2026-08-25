Feature('Luồng khởi tạo hồ sơ (Onboarding)');

/**
 * OnboardingInterceptor đá mọi STUDENT chưa hoàn thành onboarding về step1.
 * Đây vừa là hành vi cần kiểm chứng (TC-ONB-002), vừa là tiền đề bắt buộc
 * cho mọi test chức năng khác.
 */

Scenario(
  'TC-ONB-001 | Hoàn thành 3 bước onboarding rồi vào được dashboard @P0 @smoke',
  async ({ I, registerPage, loginPage, onboardingPage, dashboardPage }) => {
    const email = await I.uniqueEmail('onb');
    registerPage.register('Sinh Vien Onboarding', email, 'MatKhau@123');
    loginPage.login(email, 'MatKhau@123');

    onboardingPage.completeAll(2);

    dashboardPage.open();
    dashboardPage.seeDashboard();
  },
);

Scenario(
  'TC-ONB-002 | Chưa onboarding thì mọi trang đều bị đưa về bước 1 @P0',
  async ({ I, registerPage, loginPage, roadmapPage }) => {
    const email = await I.uniqueEmail('chuaonb');
    registerPage.register('Sinh Vien Chua Onboarding', email, 'MatKhau@123');
    loginPage.login(email, 'MatKhau@123');

    roadmapPage.open();

    I.seeInCurrentUrl('/onboarding/step1');
  },
);

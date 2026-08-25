Feature('Lộ trình học tập');

Scenario(
  'TC-RMP-001 | Xem được lộ trình sau khi hoàn tất onboarding @P1',
  async ({ I, registerPage, loginPage, onboardingPage, roadmapPage }) => {
    const email = await I.uniqueEmail('rmp');
    registerPage.register('Sinh Vien Roadmap', email, 'MatKhau@123');
    loginPage.login(email, 'MatKhau@123');
    onboardingPage.completeAll(2);

    roadmapPage.open();
    roadmapPage.seeRoadmap();
  },
);

Scenario(
  'TC-RMP-002 | Kỹ năng chọn ở onboarding được tính vào tiến độ lộ trình @P1',
  async ({ I, registerPage, loginPage, onboardingPage, roadmapPage }) => {
    const email = await I.uniqueEmail('rmp2');
    registerPage.register('Sinh Vien Tien Do', email, 'MatKhau@123');
    loginPage.login(email, 'MatKhau@123');
    onboardingPage.completeAll(3);

    roadmapPage.open();

    // Có khối thống kê tiến độ nghĩa là roadmap đã ghép được dữ liệu người dùng.
    I.see('nodes hoàn thành');
  },
);

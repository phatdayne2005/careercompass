/**
 * Cấu hình CodeceptJS cho CareerCompass.
 *
 * Test chạy trên app ĐANG CHẠY THẬT (docker compose up -d), không tự boot app.
 * Đổi URL đích:   BASE_URL=https://... npm test
 * Xem trình duyệt: SHOW=true npm test
 */
exports.config = {
  name: 'careercompass-e2e',
  tests: './tests/*_test.js',
  output: './output',

  helpers: {
    Playwright: {
      url: process.env.BASE_URL || 'http://localhost:8080',
      show: process.env.SHOW === 'true',
      browser: 'chromium',
      waitForNavigation: 'load',
      waitForTimeout: 10000,
    },
  },

  // Page Object: mỗi trang một class, test gọi qua biến cùng tên.
  include: {
    I: './support/steps_file.js',
    loginPage: './pages/Login.js',
    registerPage: './pages/Register.js',
    onboardingPage: './pages/Onboarding.js',
    dashboardPage: './pages/Dashboard.js',
    skillGapPage: './pages/SkillGap.js',
    roadmapPage: './pages/Roadmap.js',
    profilePage: './pages/Profile.js',
    adminUsersPage: './pages/AdminUsers.js',
  },

  plugins: {
    // CodeceptJS 4: dùng "screenshot" (mặc định chụp khi fail), không phải
    // screenshotOnFail của bản 3. Ảnh nằm trong output/.
    screenshot: { enabled: true },
    // Thử lại bước lỗi nhất thời (mạng chậm) để giảm test chập chờn.
    retryFailedStep: { enabled: true, retries: 2 },
    // Xuất JUnit XML - CI đọc được để hiện kết quả test ngay trên giao diện GitHub.
    junitReporter: { enabled: true, outputFile: 'junit.xml' },
    // Plugin tự viết: ghi kết quả ra output/result.json cho script Excel đọc.
    resultCollector: {
      enabled: true,
      require: './support/resultCollector.js',
    },
  },
};

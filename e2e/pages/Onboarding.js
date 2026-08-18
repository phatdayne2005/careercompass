const { I } = inject();

/**
 * Luồng onboarding 3 bước — luồng dài nhất của hệ thống.
 *
 * LƯU Ý QUAN TRỌNG: OnboardingInterceptor chặn mọi STUDENT chưa hoàn thành
 * onboarding và đá về /onboarding/step1. Nên mọi test về chức năng khác
 * (roadmap, skill gap, dashboard...) đều PHẢI chạy hết luồng này trước.
 */
module.exports = {
  step1Url: '/onboarding/step1',

  step1: {
    roleCard: '.role-card',              // thẻ nghề nghiệp, có data-roleid
    nextBtn: '#nextBtn',                 // bị disabled cho tới khi chọn 1 thẻ
    skipBtn: 'button[name=skip]',
  },
  step2: {
    githubUsername: '#githubUsername',
    submitBtn: '#step2Form button[type=submit]',
  },
  step3: {
    // Checkbox thật bị ẩn bằng class Tailwind "sr-only" (chỉ dành cho trình đọc
    // màn hình) -> Playwright không click được. Phải click vào <label for=...>,
    // đúng như người dùng thật thao tác.
    skillLabel: 'label[for^="skill_"]',
    skillCheckbox: 'input[name=skillIds]',
    submitBtn: 'form[action*="/onboarding/step3"] button[type=submit]',
  },

  open() {
    I.amOnPage(this.step1Url);
  },

  /** Bước 1: chọn thẻ nghề nghiệp đầu tiên rồi bấm Tiếp tục. */
  chooseFirstRole() {
    I.waitForElement(this.step1.roleCard, 10);
    I.click(`${this.step1.roleCard}:first-of-type`);
    I.waitForEnabled(this.step1.nextBtn, 10);
    I.click(this.step1.nextBtn);
  },

  /** Bước 2: bỏ qua bảng điểm và GitHub (không bắt buộc) để test chạy nhanh, ổn định. */
  skipSources() {
    I.waitForElement(this.step2.submitBtn, 10);
    I.click(this.step2.submitBtn);
  },

  /** Bước 3: tick n kỹ năng đầu tiên rồi hoàn tất. */
  chooseSkills(count = 2) {
    I.waitForElement(this.step3.skillLabel, 10);
    for (let i = 1; i <= count; i++) {
      I.click(locate(this.step3.skillLabel).at(i));
    }
    I.click(this.step3.submitBtn);
  },

  /** Chạy trọn 3 bước. Dùng làm bước chuẩn bị cho các test khác. */
  completeAll(skillCount = 2) {
    this.open();
    this.chooseFirstRole();
    this.skipSources();
    this.chooseSkills(skillCount);
  },
};

const { I } = inject();

/**
 * Trang phân tích khoảng trống kỹ năng (FR3.1, FR3.2, FR3.3).
 * Module này TỪNG CRASH vì LazyInitializationException -> ưu tiên test cao.
 */
module.exports = {
  url: '/skill-gap',

  templateSelect: '#templateId',
  skillSelect: '#skillId',
  addSkillBtn: 'form[action*="/skill-gap/skills"] button[type=submit]',
  saveReportBtn: 'form[action*="/skill-gap/reports"] button[type=submit]',

  open() {
    I.amOnPage(this.url);
  },

  seeAnalysis() {
    I.seeInCurrentUrl('/skill-gap');
    I.seeElement(this.templateSelect);
  },

  /** Lưu kết quả phân tích thành báo cáo (tiền đề để tải PDF). */
  saveReport() {
    I.waitForElement(this.saveReportBtn, 10);
    I.click(this.saveReportBtn);
  },
};

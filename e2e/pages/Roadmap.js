const { I } = inject();

/** Trang lộ trình học (FR2.3). */
module.exports = {
  url: '/roadmap',

  templateSelect: '#templateId',

  open() {
    I.amOnPage(this.url);
  },

  seeRoadmap() {
    I.seeInCurrentUrl('/roadmap');
    I.see('nodes hoàn thành');
  },
};

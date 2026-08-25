const { I } = inject();

/** Trang tổng quan sau khi đăng nhập + hoàn tất onboarding. */
module.exports = {
  url: '/dashboard',

  open() {
    I.amOnPage(this.url);
  },

  seeDashboard() {
    I.seeInCurrentUrl('/dashboard');
    I.see('Hoạt động gần đây');
  },
};

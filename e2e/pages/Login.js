const { I } = inject();

/**
 * Trang đăng nhập.
 * Selector lấy từ src/main/resources/templates/login.html
 */
module.exports = {
  url: '/login',

  fields: {
    email: '#username',      // Spring Security mặc định đọc name="username"
    password: '#password',
  },
  submitBtn: 'form[action*="/login"] button[type=submit]',

  open() {
    I.amOnPage(this.url);
  },

  /** Điền form và bấm đăng nhập. Không khẳng định kết quả — để test tự kiểm. */
  login(email, password) {
    this.open();
    I.fillField(this.fields.email, email);
    I.fillField(this.fields.password, password);
    I.click(this.submitBtn);
  },

  seeLoginPage() {
    I.seeInCurrentUrl('/login');
    I.seeElement(this.fields.email);
  },

  /** Spring Security chuyển hướng về /login?error khi sai thông tin. */
  seeLoginFailed() {
    I.seeInCurrentUrl('/login');
    I.seeInCurrentUrl('error');
  },
};

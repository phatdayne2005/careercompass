const { I } = inject();

/**
 * Trang đăng ký.
 * Ràng buộc từ RegisterFormDTO: fullName<=100, email hợp lệ <=150, password 6..30 ký tự.
 */
module.exports = {
  url: '/register',

  fields: {
    fullName: '#fullName',
    email: '#email',
    password: '#password',
  },
  submitBtn: 'form[action*="/register"] button[type=submit]',

  open() {
    I.amOnPage(this.url);
  },

  register(fullName, email, password) {
    this.open();
    I.fillField(this.fields.fullName, fullName);
    I.fillField(this.fields.email, email);
    I.fillField(this.fields.password, password);
    I.click(this.submitBtn);
  },
};

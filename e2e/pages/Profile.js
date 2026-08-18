const { I } = inject();

/**
 * Trang hồ sơ cá nhân (FR1.3).
 * Từng vỡ runtime do user null -> giữ trong nhóm ưu tiên.
 */
module.exports = {
  url: '/profile',

  fullNameInput: 'form[action*="/profile/name"] input[name=fullName]',
  saveNameBtn: 'form[action*="/profile/name"] button[type=submit]',

  password: {
    current: 'input[name=currentPassword]',
    next: 'input[name=newPassword]',
    confirm: 'input[name=confirmPassword]',
    submit: 'form[action*="/profile/password"] button[type=submit]',
  },

  open() {
    I.amOnPage(this.url);
  },

  changePassword(currentPwd, newPwd) {
    this.open();
    I.fillField(this.password.current, currentPwd);
    I.fillField(this.password.next, newPwd);
    I.fillField(this.password.confirm, newPwd);
    I.click(this.password.submit);
  },
};

const { I } = inject();

/** Trang quản trị người dùng — chỉ ADMIN vào được. */
module.exports = {
  url: '/admin/users',

  searchInput: 'input[name=keyword]',
  searchBtn: 'form[action*="/admin/users"] button[type=submit]',
  roleSelect: 'select[name=roleName]',

  open() {
    I.amOnPage(this.url);
  },

  seeUserList() {
    I.seeInCurrentUrl('/admin/users');
    I.seeElement(this.searchInput);
  },

  /** Đăng xuất để phiên đăng nhập được nạp lại quyền từ database. */
  logout() {
    I.click('form[action*="/logout"] button[type=submit]');
  },

  search(keyword) {
    this.open();
    I.fillField(this.searchInput, keyword);
    I.click(this.searchBtn);
  },
};

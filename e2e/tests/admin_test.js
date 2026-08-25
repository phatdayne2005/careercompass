Feature('Quản trị người dùng');

const ADMIN = { email: 'admin@gmail.com', password: '123456' };

Scenario(
  'TC-ADM-001 | Quản trị viên xem được danh sách người dùng @P1',
  ({ I, loginPage, adminUsersPage }) => {
    loginPage.login(ADMIN.email, ADMIN.password);

    adminUsersPage.open();
    adminUsersPage.seeUserList();
  },
);

Scenario(
  'TC-ADM-002 | Quản trị viên tìm kiếm được người dùng theo email @P1',
  async ({ I, registerPage, loginPage, adminUsersPage }) => {
    // Tạo sẵn một người dùng để có thứ mà tìm.
    const email = await I.uniqueEmail('timkiem');
    registerPage.register('Nguoi Dung Bi Tim', email, 'MatKhau@123');

    loginPage.login(ADMIN.email, ADMIN.password);
    adminUsersPage.search(email);

    I.see(email);
  },
);

Scenario(
  'TC-ADM-003 | Quản trị viên không được tự đổi vai trò của chính mình @P1 @known-bug',
  ({ I, loginPage, adminUsersPage }) => {
    loginPage.login(ADMIN.email, ADMIN.password);
    adminUsersPage.search(ADMIN.email);

    // Hạ chính mình xuống STUDENT — thao tác PHẢI bị hệ thống từ chối.
    I.selectOption(`tr:has-text("${ADMIN.email}") ${adminUsersPage.roleSelect}`, 'STUDENT');
    I.wait(2);

    // Phải đăng xuất rồi đăng nhập lại mới thấy hậu quả: Spring Security giữ
    // quyền trong session, đổi vai trò trong DB không làm mới phiên đang chạy.
    // Nếu chỉ kiểm tra trong cùng phiên thì test xanh giả.
    adminUsersPage.logout();
    loginPage.login(ADMIN.email, ADMIN.password);

    // Kỳ vọng: vẫn còn quyền quản trị.
    // Thực tế (lỗi DEF-001): đã bị hạ xuống STUDENT nên mất quyền vĩnh viễn.
    adminUsersPage.open();
    adminUsersPage.seeUserList();
  },
);

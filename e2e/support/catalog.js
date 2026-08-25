/**
 * Danh mục test case: phần mô tả KHÔNG tự suy ra được từ kết quả chạy.
 *
 * Script sinh Excel ghép dữ liệu ở đây với kết quả thật trong output/result.json
 * theo mã test case (ID). Thêm case mới: thêm Scenario trong tests/ và thêm
 * một mục tương ứng ở đây.
 */
module.exports = {
  project: 'CareerCompass',
  sprint: 'Sprint Week 4',
  testType: 'Kiểm thử giao diện (E2E) - CodeceptJS + Playwright',

  /** Mã yêu cầu lấy từ SRS, dùng cho ma trận truy vết. */
  requirements: {
    'FR1.3': 'Quản lý hồ sơ người dùng (đăng ký, thông tin cá nhân, đổi mật khẩu)',
    'FR2.1': 'Chọn vị trí nghề nghiệp mục tiêu',
    'FR2.3': 'Hiển thị lộ trình học và tiến độ',
    'FR2.4': 'Trang tổng quan tiến độ học tập',
    'FR3.1': 'Ghi nhận kỹ năng hiện có của sinh viên',
    'FR3.2': 'Phân tích khoảng trống kỹ năng',
    'FR3.3': 'Xuất báo cáo khoảng trống kỹ năng ra PDF',
    'FR4.1': 'Thu thập dữ liệu xu hướng tuyển dụng',
    'FR4.2': 'Thống kê từ khoá kỹ năng theo thị trường',
    'FR4.3': 'Hiển thị bảng xu hướng thị trường',
    'FR5.1': 'Liên kết tài khoản GitHub',
    'FR5.3': 'Trang portfolio công khai',
    'FR6.2': 'Nhật ký hoạt động người dùng',
  },

  cases: {
    'TC-AUTH-001': {
      module: 'Xác thực', priority: 'P0', type: 'Smoke', fr: ['FR1.3'],
      precondition: 'Chưa đăng nhập; email chưa tồn tại trong hệ thống',
      steps: '1. Mở /register\n2. Nhập họ tên, email mới, mật khẩu hợp lệ\n3. Bấm Đăng ký\n4. Đăng nhập bằng tài khoản vừa tạo',
      data: 'email sinh ngẫu nhiên; mật khẩu MatKhau@123',
      expected: 'Tạo tài khoản thành công và đăng nhập được, không bị chuyển về /login?error',
    },
    'TC-AUTH-002': {
      module: 'Xác thực', priority: 'P0', type: 'Chức năng', fr: ['FR1.3'],
      precondition: 'Email đã được đăng ký trước đó',
      steps: '1. Đăng ký email X\n2. Đăng ký lại đúng email X',
      data: 'cùng một email cho cả 2 lần',
      expected: 'Lần 2 bị từ chối, ở lại trang /register, không tạo tài khoản trùng',
    },
    'TC-AUTH-003': {
      module: 'Xác thực', priority: 'P0', type: 'Bảo mật', fr: [],
      precondition: 'Tài khoản student@gmail.com tồn tại',
      steps: '1. Mở /login\n2. Nhập đúng email nhưng sai mật khẩu\n3. Bấm Đăng nhập',
      data: 'student@gmail.com / mật khẩu sai',
      expected: 'Bị từ chối, chuyển về /login?error, không vào được hệ thống',
    },
    'TC-ONB-001': {
      module: 'Onboarding', priority: 'P0', type: 'Smoke', fr: ['FR2.1', 'FR3.1'],
      precondition: 'Tài khoản mới, chưa hoàn thành onboarding',
      steps: '1. Đăng ký và đăng nhập\n2. Bước 1: chọn vị trí nghề nghiệp\n3. Bước 2: bỏ qua bảng điểm/GitHub\n4. Bước 3: chọn 2 kỹ năng và hoàn tất\n5. Mở /dashboard',
      data: 'vị trí đầu tiên; 2 kỹ năng đầu tiên',
      expected: 'Hoàn tất 3 bước và vào được trang tổng quan',
    },
    'TC-ONB-002': {
      module: 'Onboarding', priority: 'P0', type: 'Chức năng', fr: ['FR2.1'],
      precondition: 'Tài khoản mới, chưa hoàn thành onboarding',
      steps: '1. Đăng ký và đăng nhập\n2. Truy cập thẳng /roadmap',
      data: 'tài khoản chưa onboarding',
      expected: 'Bị chuyển hướng về /onboarding/step1',
    },
    'TC-SEC-001': {
      module: 'Phân quyền', priority: 'P0', type: 'Bảo mật', fr: [],
      precondition: 'Đang đăng nhập bằng tài khoản vai trò STUDENT',
      steps: '1. Đăng ký và đăng nhập tài khoản sinh viên\n2. Truy cập /admin/users',
      data: 'tài khoản sinh viên thường',
      expected: 'Bị chặn, không nhìn thấy danh sách người dùng',
    },
    'TC-SEC-002': {
      module: 'Phân quyền', priority: 'P0', type: 'Bảo mật', fr: [],
      precondition: 'Chưa đăng nhập',
      steps: '1. Truy cập thẳng /dashboard khi chưa đăng nhập',
      data: 'không có phiên đăng nhập',
      expected: 'Bị đưa về trang /login',
    },
    'TC-SKG-001': {
      module: 'Skill Gap', priority: 'P1', type: 'Chức năng', fr: ['FR3.1', 'FR3.2'],
      precondition: 'Tài khoản đã hoàn thành onboarding với 3 kỹ năng',
      steps: '1. Đăng ký, đăng nhập, hoàn tất onboarding\n2. Mở /skill-gap',
      data: '3 kỹ năng chọn ở onboarding',
      expected: 'Trang phân tích hiển thị được, không lỗi LazyInitializationException',
    },
    'TC-SKG-002': {
      module: 'Skill Gap', priority: 'P1', type: 'Chức năng', fr: ['FR3.2', 'FR3.3'],
      precondition: 'Đã có kết quả phân tích khoảng trống kỹ năng',
      steps: '1. Hoàn tất onboarding\n2. Mở /skill-gap\n3. Bấm lưu báo cáo',
      data: 'lộ trình mặc định',
      expected: 'Lưu báo cáo thành công, không văng lỗi',
    },
    'TC-RMP-001': {
      module: 'Lộ trình', priority: 'P1', type: 'Chức năng', fr: ['FR2.3'],
      precondition: 'Tài khoản đã hoàn thành onboarding',
      steps: '1. Hoàn tất onboarding\n2. Mở /roadmap',
      data: '2 kỹ năng chọn ở onboarding',
      expected: 'Lộ trình hiển thị kèm thống kê số node hoàn thành',
    },
    'TC-RMP-002': {
      module: 'Lộ trình', priority: 'P1', type: 'Tích hợp', fr: ['FR2.3', 'FR3.1'],
      precondition: 'Tài khoản đã chọn 3 kỹ năng ở onboarding',
      steps: '1. Hoàn tất onboarding với 3 kỹ năng\n2. Mở /roadmap',
      data: '3 kỹ năng',
      expected: 'Kỹ năng đã chọn được tính vào tiến độ lộ trình',
    },
    'TC-PRF-001': {
      module: 'Hồ sơ', priority: 'P1', type: 'Bảo mật', fr: ['FR1.3'],
      precondition: 'Tài khoản đã hoàn thành onboarding',
      steps: '1. Mở /profile\n2. Đổi mật khẩu\n3. Đăng nhập bằng mật khẩu CŨ\n4. Đăng nhập bằng mật khẩu MỚI',
      data: 'cũ MatKhau@123 -> mới MatKhauMoi@456',
      expected: 'Mật khẩu cũ bị từ chối, mật khẩu mới đăng nhập được',
    },
    'TC-PRF-002': {
      module: 'Hồ sơ', priority: 'P1', type: 'Chức năng', fr: ['FR1.3'],
      precondition: 'Tài khoản đã hoàn thành onboarding',
      steps: '1. Mở /profile\n2. Kiểm tra ô email',
      data: 'email của tài khoản đang đăng nhập',
      expected: 'Trang hồ sơ mở được và hiện đúng email đang đăng nhập',
    },
    'TC-ADM-001': {
      module: 'Quản trị', priority: 'P1', type: 'Chức năng', fr: [],
      precondition: 'Đăng nhập bằng tài khoản ADMIN',
      steps: '1. Đăng nhập admin\n2. Mở /admin/users',
      data: 'admin@gmail.com',
      expected: 'Xem được danh sách người dùng',
    },
    'TC-ADM-002': {
      module: 'Quản trị', priority: 'P1', type: 'Chức năng', fr: [],
      precondition: 'Có ít nhất một người dùng để tìm',
      steps: '1. Tạo một người dùng mới\n2. Đăng nhập admin\n3. Tìm theo email vừa tạo',
      data: 'email sinh ngẫu nhiên',
      expected: 'Kết quả tìm kiếm hiển thị đúng người dùng',
    },
    'TC-ADM-003': {
      module: 'Quản trị', priority: 'P1', type: 'Bảo mật', fr: [],
      precondition: 'Đăng nhập bằng tài khoản ADMIN duy nhất',
      steps: '1. Đăng nhập admin\n2. Tìm chính tài khoản admin\n3. Đổi vai trò của chính mình thành STUDENT',
      data: 'admin@gmail.com -> STUDENT',
      expected: 'Thao tác bị từ chối, admin giữ nguyên quyền quản trị',
      defect: 'DEF-001',
    },
    'TC-DSB-001': {
      module: 'Tổng quan', priority: 'P1', type: 'Tích hợp', fr: ['FR2.4', 'FR6.2'],
      precondition: 'Tài khoản đã hoàn thành onboarding',
      steps: '1. Hoàn tất onboarding\n2. Mở /dashboard',
      data: '3 kỹ năng',
      expected: 'Hiện thống kê tiến độ, số kỹ năng còn thiếu và nhật ký hoạt động',
    },
  },

  /** Lỗi phát hiện được trong quá trình kiểm thử. */
  defects: [
    {
      id: 'DEF-001',
      testCase: 'TC-ADM-003',
      severity: 'Cao',
      title: 'Quản trị viên tự hạ vai trò của chính mình và mất quyền truy cập vĩnh viễn',
      description:
        'AdminUserService.changeUserRole() không gọi requireNotCurrentUser(), trong khi ' +
        'toggleUserStatus() và deleteUser() trong cùng file đều có gọi. Hậu quả: admin đổi ' +
        'vai trò của chính mình xuống STUDENT thì mất ngay quyền vào /admin/**. Nếu hệ thống ' +
        'chỉ có một admin thì không còn ai khôi phục được, phải sửa trực tiếp trong database.',
      file: 'src/main/java/vn/uth/careercompass/admin/service/AdminUserService.java',
      status: 'Mới phát hiện',
    },
  ],
};

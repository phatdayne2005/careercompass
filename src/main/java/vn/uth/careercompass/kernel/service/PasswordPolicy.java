package vn.uth.careercompass.kernel.service;

import java.nio.charset.StandardCharsets;

/**
 * Quy tắc độ dài mật khẩu, dùng chung cho cả ba đường đặt mật khẩu.
 *
 * <p>DEF-013: trước lớp này, ba đường đặt mật khẩu kiểm ba kiểu khác nhau —
 * {@code POST /register} dựa vào {@code @Size(min = 6, max = 30)} của RegisterFormDTO,
 * còn {@code POST /reset-password} và {@code POST /profile/password} chỉ kiểm
 * {@code length() < 6} mà KHÔNG có giới hạn trên. Hệ quả là cùng một mật khẩu bị từ chối
 * lúc đăng ký nhưng lại đặt được qua chức năng quên mật khẩu.
 *
 * <p>Nghiêm trọng hơn: {@code @Size} đếm KÝ TỰ, trong khi BCrypt giới hạn 72 BYTE và ném
 * {@code IllegalArgumentException} khi vượt. Hai đơn vị đo khác nhau trên cùng một biến.
 * Với tiếng Việt có dấu, mỗi ký tự chiếm 3 byte trong UTF-8 nên 25 ký tự đã là 75 byte —
 * lọt qua {@code @Size(max = 30)} rồi vỡ ở tầng mã hoá.
 *
 * <p>Lớp này gom cả hai biên về một chỗ và trả thông điệp tiếng Việt (NFR-U01), thay vì
 * để nguyên văn "password cannot be more than 72 bytes" của thư viện lọt ra giao diện.
 */
public final class PasswordPolicy {

    /** Số ký tự tối thiểu. */
    public static final int SO_KY_TU_TOI_THIEU = 6;

    /** Số ký tự tối đa. */
    public static final int SO_KY_TU_TOI_DA = 30;

    /**
     * Số byte UTF-8 tối đa. Đây là giới hạn CỨNG của thuật toán BCrypt, không phải lựa
     * chọn của nhóm: {@code BCrypt.hashpw} ném IllegalArgumentException khi vượt 72 byte.
     */
    public static final int SO_BYTE_TOI_DA = 72;

    private PasswordPolicy() {
    }

    /**
     * Kiểm tra mật khẩu thô.
     *
     * @return thông điệp lỗi tiếng Việt, hoặc {@code null} nếu mật khẩu hợp lệ
     */
    public static String kiemTra(String matKhau) {
        return kiemTra(matKhau, "Mật khẩu");
    }

    /**
     * Như {@link #kiemTra(String)} nhưng cho phép đổi cách gọi tên trường trong thông
     * điệp. Màn đổi mật khẩu có ba ô mật khẩu cạnh nhau nên phải nói rõ "Mật khẩu mới",
     * không thì người dùng không biết ô nào sai.
     *
     * @param nhan cách gọi tên trường, ví dụ "Mật khẩu" hoặc "Mật khẩu mới"
     */
    public static String kiemTra(String matKhau, String nhan) {
        if (matKhau == null || matKhau.length() < SO_KY_TU_TOI_THIEU) {
            return nhan + " phải từ " + SO_KY_TU_TOI_THIEU + " ký tự trở lên.";
        }
        if (matKhau.length() > SO_KY_TU_TOI_DA) {
            return nhan + " không được quá " + SO_KY_TU_TOI_DA + " ký tự.";
        }
        if (matKhau.getBytes(StandardCharsets.UTF_8).length > SO_BYTE_TOI_DA) {
            // Đếm theo byte nên thông điệp phải nói rõ vì sao một mật khẩu "chỉ 25 ký tự"
            // lại bị từ chối, nếu không người dùng tưởng hệ thống hỏng.
            return nhan + " chứa quá nhiều ký tự có dấu hoặc biểu tượng "
                    + "(vượt " + SO_BYTE_TOI_DA + " byte). Vui lòng rút ngắn lại.";
        }
        return null;
    }
}

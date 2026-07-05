package vn.uth.careercompass.portfolio.dto;

import java.util.List;

/**
 * Thông tin chủ portfolio để hiển thị trên trang công khai: mục tiêu nghề nghiệp,
 * danh sách kỹ năng (từ dữ liệu hệ thống), và nhận xét điểm mạnh (AI phân tích bảng điểm).
 */
public record OwnerInfoDTO(String careerGoal, List<String> skills, String strength) {
}

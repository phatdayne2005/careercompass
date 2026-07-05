package vn.uth.careercompass.marketpulse.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import vn.uth.careercompass.marketpulse.entity.JobTrend;
import vn.uth.careercompass.marketpulse.repository.JobTrendRepository;

import java.util.List;

/**
 * Seed dữ liệu JobTrend mẫu để Market Pulse (Màn ⑦) có biểu đồ ngay khi demo.
 *
 * <p><b>Vì sao cần:</b> {@code ScraperService} cào LinkedIn/TopCV thật theo lịch (@Scheduled 2h sáng),
 * nhưng các trang này chặn bot mạnh nên thường trả rỗng — biểu đồ sẽ trống lúc bảo vệ. Seeder này
 * cung cấp bộ JD mẫu thực tế (phủ các công nghệ đang theo dõi) để FR4.2/4.3 luôn có dữ liệu.
 * Idempotent: chỉ seed khi bảng còn rỗng, không đụng dữ liệu scrape thật nếu đã có.</p>
 */
@Component
@Order(3)
@RequiredArgsConstructor
public class JobTrendSeeder implements CommandLineRunner {

    private final JobTrendRepository jobTrendRepository;

    @Override
    public void run(String... args) {
        if (jobTrendRepository.count() > 0) {
            return;
        }
        jobTrendRepository.saveAll(List.of(
                job("TOPCV", "Java Backend Developer", "FPT Software",
                        "Tuyển Java Backend: thành thạo Java, Spring Boot, RESTful API, SQL (MySQL/PostgreSQL), Docker. Ưu tiên biết Microservices và CI/CD."),
                job("TOPCV", "Senior Java Engineer", "MoMo",
                        "Yêu cầu Java, Spring Boot, Microservices, Kubernetes, AWS, SQL. Kinh nghiệm hệ thống fintech quy mô lớn."),
                job("LINKEDIN", "Backend Developer (Spring)", "VNPay",
                        "Java, Spring Boot, SQL, Redis, Docker, CI/CD. Thiết kế API cho hệ thống thanh toán."),
                job("TOPCV", "Frontend Developer (React)", "Tiki",
                        "Thành thạo React, TypeScript, JavaScript, HTML/CSS. Biết Node.js là lợi thế."),
                job("LINKEDIN", "Fullstack Developer", "Shopee",
                        "React, TypeScript, Node.js phía frontend; Java, Spring Boot, SQL phía backend. Triển khai bằng Docker."),
                job("TOPCV", "DevOps Engineer", "Viettel Digital",
                        "Docker, Kubernetes, AWS, CI/CD (Jenkins/GitLab). Quản trị hạ tầng microservices."),
                job("LINKEDIN", "Cloud Engineer (AWS)", "VNG Cloud",
                        "AWS, Kubernetes, Docker, Terraform. Vận hành hạ tầng cloud cho sản phẩm quy mô lớn."),
                job("TOPCV", "Data Engineer", "Be Group",
                        "Python, SQL, AWS, ETL pipeline. Xử lý dữ liệu lớn, tối ưu truy vấn."),
                job("LINKEDIN", "Python Developer", "Zalo AI",
                        "Python, SQL, Docker. Xây dựng dịch vụ AI/ML, tích hợp Microservices."),
                job("TOPCV", "Backend Engineer (Node.js)", "Grab",
                        "Node.js, TypeScript, SQL, AWS, Docker. Phát triển API hiệu năng cao."),
                job("LINKEDIN", "Java Developer (Fresher)", "NashTech",
                        "Java, Spring Boot cơ bản, SQL. Đào tạo thêm Docker, CI/CD."),
                job("TOPCV", "Senior Frontend Engineer", "Base.vn",
                        "React, TypeScript, JavaScript. Tối ưu hiệu năng, kiến trúc component."),
                job("LINKEDIN", "Software Engineer", "Axon",
                        "Java, Spring Boot, Microservices, Kubernetes, AWS, SQL. Hệ thống phân tán."),
                job("TOPCV", "Full-stack Java + React", "KMS Technology",
                        "Java, Spring Boot, React, TypeScript, SQL, Docker, CI/CD. Làm sản phẩm cho thị trường Mỹ."),
                job("LINKEDIN", "Platform Engineer", "Sky Mavis",
                        "Kubernetes, Docker, AWS, Python, CI/CD. Vận hành nền tảng blockchain."),
                job("TOPCV", "Backend Developer (Python)", "Got It AI",
                        "Python, SQL, Docker, Microservices. Xây dựng API cho sản phẩm AI.")
        ));
        System.out.println("[JobTrendSeeder] Đã seed 16 JobTrend mẫu cho Market Pulse.");
    }

    private JobTrend job(String source, String title, String company, String description) {
        return JobTrend.builder()
                .source(source)
                .jobTitle(title)
                .company(company)
                .rawDescription(description)
                .build();
    }
}

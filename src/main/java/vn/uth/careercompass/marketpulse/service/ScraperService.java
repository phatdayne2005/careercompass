package vn.uth.careercompass.marketpulse.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import vn.uth.careercompass.marketpulse.entity.JobTrend;
import vn.uth.careercompass.marketpulse.repository.JobTrendRepository;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Cào dữ liệu tuyển dụng IT THẬT từ The Muse API (JSON công khai, nguồn uy tín) — cho Market Pulse (FR4.1).
 *
 * <p>Vì sao The Muse: có API JSON mở + tham số category (lọc sẵn "Software Engineering") + mô tả JD đầy đủ;
 * không chặn bot, không render JS như LinkedIn/TopCV. Danh mục SE vẫn lẫn ít job phần cứng (cơ khí SpaceX...)
 * nên lọc thêm theo tên vị trí.</p>
 *
 * <p>Chạy tự động mỗi ngày 02:00, mỗi lần THAY dữ liệu cũ bằng danh sách mới; cào rỗng/lỗi thì giữ data cũ.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScraperService {

    private static final String MUSE_API = "https://www.themuse.com/api/public/jobs";
    private static final int PAGES = 4;            // ~20 tin/trang
    private static final int MAX_DESC = 3900;      // cột rawDescription VARCHAR(4000)

    /**
     * CHỈ giữ tin có tín hiệu PHẦN MỀM/CNTT cụ thể trong tên vị trí (danh mục The Muse còn lẫn
     * vai trò phần cứng/quản lý/phi kỹ thuật — "engineer" đơn thuần không đủ vì có cả cơ khí/hàng không).
     */
    private static final List<String> SOFTWARE_SIGNALS = List.of(
            "software", "developer", "full stack", "fullstack", "full-stack", "backend", "back end", "back-end",
            "frontend", "front end", "front-end", "devops", "data engineer", "data scientist", "data analyst",
            "machine learning", "ml engineer", "ai engineer", "cloud engineer", "platform engineer",
            "web develop", "mobile develop", "android", "ios ", "programmer", "sre", "site reliability",
            "qa engineer", "security engineer", "database engineer", "infrastructure engineer");

    private final JobTrendRepository jobTrendRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    /** Chạy mỗi ngày lúc 02:00 sáng. */
    @Scheduled(cron = "0 0 2 * * *")
    public void scrapeDaily() {
        scrapeJobs();
    }

    /** Cào The Muse, thay toàn bộ job_trends bằng dữ liệu mới. Trả số bản ghi đã lưu (0 nếu lỗi/rỗng). */
    @Transactional
    @SuppressWarnings("unchecked")
    public int scrapeJobs() {
        List<JobTrend> fresh = new ArrayList<>();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (CareerCompass Market Pulse)");
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            for (int page = 1; page <= PAGES; page++) {
                // Dùng UriComponentsBuilder + URI để tránh RestTemplate double-encode dấu cách trong category.
                URI uri = UriComponentsBuilder.fromUriString(MUSE_API)
                        .queryParam("category", "Software Engineering")
                        .queryParam("category", "Data Science")
                        .queryParam("page", page)
                        .build().toUri();
                ResponseEntity<Map> response = restTemplate.exchange(uri, HttpMethod.GET, entity, Map.class);
                Map<String, Object> body = response.getBody();
                if (body == null) {
                    continue;
                }
                List<Map<String, Object>> results = (List<Map<String, Object>>) body.get("results");
                if (results == null) {
                    continue;
                }
                for (Map<String, Object> item : results) {
                    Object nameObj = item.get("name");
                    if (nameObj == null) {
                        continue;
                    }
                    String title = nameObj.toString();
                    String titleLower = " " + title.toLowerCase() + " ";
                    if (SOFTWARE_SIGNALS.stream().noneMatch(titleLower::contains)) {
                        continue;   // chỉ giữ tin phần mềm/CNTT rõ ràng
                    }

                    String company = (item.get("company") instanceof Map<?, ?> co)
                            ? String.valueOf(co.get("name")) : "";
                    String desc = String.valueOf(item.getOrDefault("contents", ""))
                            .replaceAll("<[^>]+>", " ");   // bỏ thẻ HTML
                    String raw = (title + " " + desc).replaceAll("\\s+", " ").trim();

                    fresh.add(JobTrend.builder()
                            .source("THEMUSE")
                            .jobTitle(cap(title, 255))
                            .company(cap(company, 150))
                            .rawDescription(cap(raw, MAX_DESC))
                            .build());
                }
            }
        } catch (Exception e) {
            log.warn("[ScraperService] Lỗi khi cào The Muse: {}", e.getMessage());
            return 0;
        }

        if (fresh.isEmpty()) {
            return 0;   // không xoá dữ liệu cũ nếu cào không ra gì
        }
        jobTrendRepository.deleteAll();
        jobTrendRepository.saveAll(fresh);
        log.info("[ScraperService] The Muse → lưu {} JobTrend", fresh.size());
        return fresh.size();
    }

    private String cap(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() > max ? value.substring(0, max) : value;
    }
}

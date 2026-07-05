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
import vn.uth.careercompass.marketpulse.entity.JobTrend;
import vn.uth.careercompass.marketpulse.repository.JobTrendRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Cào dữ liệu tuyển dụng THẬT từ RemoteOK API (JSON công khai) — dùng cho Market Pulse (FR4.1).
 *
 * <p>Vì sao RemoteOK: có API JSON mở, không chặn bot, không render JS (Jsoup/LinkedIn/TopCV trước
 * đây không lấy được). Theo ToS của RemoteOK, cần ghi nguồn "RemoteOK" — đã hiển thị ở trang Market Pulse.</p>
 *
 * <p>Chạy tự động mỗi ngày 02:00. Mỗi lần cào THAY dữ liệu cũ bằng danh sách mới nhất; nếu cào rỗng/lỗi
 * thì GIỮ dữ liệu cũ (không làm trống biểu đồ).</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScraperService {

    private static final String REMOTEOK_API = "https://remoteok.com/api";
    private static final int MAX_DESC = 3900;   // cột rawDescription là VARCHAR(4000)

    /**
     * Chỉ giữ tin liên quan CNTT — lọc theo TÊN VỊ TRÍ (tags của RemoteOK là spam, tin nào cũng gắn
     * "dev/engineer" nên không dùng để lọc được).
     */
    private static final List<String> IT_TITLE_SIGNALS = List.of(
            "developer", "engineer", "software", "programmer", "backend", "back-end", "frontend", "front-end",
            "fullstack", "full stack", "full-stack", "devops", "devsecops", " sre ", "architect", "data scientist",
            "data engineer", "data analyst", "machine learning", "ai/ml", " ai ", " ml ", "android", " ios ",
            "mobile", "web dev", " qa ", "tester", "sdet", "security", "cyber", "blockchain", "smart contract",
            " dba ", "database", "cloud", "platform engineer", "tech lead", "technical lead", "sysadmin",
            "system administrator", "game dev", "embedded", "network engineer", "automation", "sysops", "solidity",
            " react", " node", "python", " java ", "golang", " rust", " php ", " ruby ", ".net");

    /** Loại tin phi kỹ thuật dù có lọt tín hiệu IT (vd "Data Entry" chứa "data"). */
    private static final List<String> NON_IT_TITLE = List.of(
            "data entry", "customer support", "customer service", "administrative", "admin assistant",
            "virtual assistant", "file clerk", "recruiter", "sales", "account ", "bookkeep", "strategist",
            "marketing", "content writer", "copywriter", "video editor", "teacher", "tutor", "nurse", "medical",
            "project manager", "product manager", "designer", "human resource", "operations manager");

    private final JobTrendRepository jobTrendRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    /** Chạy mỗi ngày lúc 02:00 sáng. */
    @Scheduled(cron = "0 0 2 * * *")
    public void scrapeDaily() {
        scrapeRemoteOk();
    }

    /**
     * Cào RemoteOK, thay toàn bộ job_trends bằng dữ liệu mới. Trả số bản ghi đã lưu (0 nếu lỗi/rỗng).
     */
    @Transactional
    @SuppressWarnings("unchecked")
    public int scrapeRemoteOk() {
        List<JobTrend> fresh = new ArrayList<>();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (CareerCompass Market Pulse)");
            ResponseEntity<List> response = restTemplate.exchange(
                    REMOTEOK_API, HttpMethod.GET, new HttpEntity<>(headers), List.class);
            List<Map<String, Object>> items = response.getBody();
            if (items == null) {
                return 0;
            }
            for (Map<String, Object> item : items) {
                Object position = item.get("position");
                if (position == null) {
                    continue; // bỏ item đầu (chỉ chứa "legal")
                }
                // Lọc IT theo TÊN VỊ TRÍ (tags của RemoteOK là spam, không dùng lọc được).
                String titleLower = " " + position.toString().toLowerCase() + " ";
                boolean itByTitle = IT_TITLE_SIGNALS.stream().anyMatch(titleLower::contains);
                boolean isNonIt = NON_IT_TITLE.stream().anyMatch(titleLower::contains);
                if (!itByTitle || isNonIt) {
                    continue;
                }

                String tags = (item.get("tags") instanceof List<?> list)
                        ? list.stream().map(String::valueOf).collect(Collectors.joining(" "))
                        : "";
                String desc = String.valueOf(item.getOrDefault("description", ""))
                        .replaceAll("<[^>]+>", " ");   // bỏ thẻ HTML
                String raw = (position + " " + tags + " " + desc).replaceAll("\\s+", " ").trim();

                fresh.add(JobTrend.builder()
                        .source("REMOTEOK")
                        .jobTitle(cap(position.toString(), 255))
                        .company(cap(String.valueOf(item.getOrDefault("company", "")), 150))
                        .rawDescription(cap(raw, MAX_DESC))
                        .build());
            }
        } catch (Exception e) {
            log.warn("[ScraperService] Lỗi khi cào RemoteOK: {}", e.getMessage());
            return 0;
        }

        if (fresh.isEmpty()) {
            return 0; // không xoá dữ liệu cũ nếu cào không ra gì
        }
        jobTrendRepository.deleteAll();
        jobTrendRepository.saveAll(fresh);
        log.info("[ScraperService] RemoteOK → lưu {} JobTrend", fresh.size());
        return fresh.size();
    }

    private String cap(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() > max ? value.substring(0, max) : value;
    }
}

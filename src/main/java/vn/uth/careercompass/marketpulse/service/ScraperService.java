package vn.uth.careercompass.marketpulse.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import vn.uth.careercompass.marketpulse.entity.JobTrend;
import vn.uth.careercompass.marketpulse.repository.JobTrendRepository;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScraperService {

    private final JobTrendRepository jobTrendRepository;

    /** Chạy mỗi ngày lúc 02:00 sáng (ít traffic nhất). */
    @Scheduled(cron = "0 0 2 * * *")
    public void scrapeDaily() {
        scrapeSource("TOPCV", "https://www.topcv.vn/tim-viec-lam-it");
        scrapeSource("LINKEDIN", "https://www.linkedin.com/jobs/search?keywords=developer&location=Vietnam");
    }

    /**
     * Scrape 1 nguồn. Selector CSS (".job-item", ".title"...) là ví dụ minh hoạ —
     * cần inspect HTML thật của site và chỉnh lại cho khớp.
     */
    public void scrapeSource(String source, String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(10_000)
                    .get();

            Elements jobCards = doc.select(".job-item, .base-search-card");
            int saved = 0;
            for (Element card : jobCards) {
                String title = card.select(".title, .base-search-card__title").text();
                String company = card.select(".company, .base-search-card__subtitle").text();
                String description = card.text();

                if (title.isBlank()) continue;

                JobTrend trend = JobTrend.builder()
                        .source(source)
                        .jobTitle(title)
                        .company(company)
                        .rawDescription(description)
                        .build();
                jobTrendRepository.save(trend);
                saved++;
            }
            log.info("[ScraperService] {} → lưu {} JobTrend", source, saved);
        } catch (IOException e) {
            // Không throw để 1 nguồn lỗi không làm crash cả job / không chặn nguồn còn lại.
            log.warn("[ScraperService] Lỗi khi scrape {}: {}", source, e.getMessage());
        }
    }
}
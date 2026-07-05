package vn.uth.careercompass.config;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import vn.uth.careercompass.kernel.service.MarkdownRenderer;

/**
 * Đưa {@link MarkdownRenderer} vào model của MỌI trang dưới tên {@code markdown} để template gọi
 * {@code th:utext="${markdown.toHtml(...)}"} — Thymeleaf CẤM truy cập trực tiếp {@code @beanName}
 * nên phải qua biến model.
 */
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAdvice {

    private final MarkdownRenderer markdownRenderer;

    @ModelAttribute("markdown")
    public MarkdownRenderer markdown() {
        return markdownRenderer;
    }
}

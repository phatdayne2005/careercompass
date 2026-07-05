package vn.uth.careercompass.kernel.service;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Component;

/**
 * Chuyển markdown (do AI trả về: **đậm**, *nghiêng*, danh sách...) thành HTML để hiển thị đúng.
 * Dùng trong template qua {@code th:utext="${@markdownRenderer.toHtml(...)}"}.
 *
 * <p>{@code escapeHtml(true)}: escape mọi HTML thô trong nội dung -> an toàn XSS khi render bằng
 * th:utext (nội dung AI/README không chèn được thẻ HTML độc).</p>
 */
@Component("markdownRenderer")
public class MarkdownRenderer {

    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer renderer = HtmlRenderer.builder().escapeHtml(true).build();

    public String toHtml(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        return renderer.render(parser.parse(markdown));
    }
}

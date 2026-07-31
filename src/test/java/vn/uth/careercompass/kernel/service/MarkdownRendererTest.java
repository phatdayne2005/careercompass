package vn.uth.careercompass.kernel.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test cho {@link MarkdownRenderer}.
 *
 * <p>KHÁC BIỆT: class này KHÔNG có dependency nào -> KHÔNG cần Mockito, KHÔNG cần @Mock.
 * Chỉ {@code new MarkdownRenderer()} rồi test input/output. Đây là dạng "pure function test",
 * đơn giản và nhanh nhất.
 */
class MarkdownRendererTest {

    private final MarkdownRenderer renderer = new MarkdownRenderer();

    @Test
    void toHtml_whenNull_returnsEmptyString() {
        assertThat(renderer.toHtml(null)).isEmpty();
    }

    @Test
    void toHtml_whenBlank_returnsEmptyString() {
        assertThat(renderer.toHtml("   ")).isEmpty();
    }

    @Test
    void toHtml_convertsBoldMarkdownToStrongTag() {
        String html = renderer.toHtml("**đậm**");
        assertThat(html).contains("<strong>đậm</strong>");
    }

    @Test
    void toHtml_escapesRawHtml_preventXss() {
        // escapeHtml(true): HTML thô trong nội dung phải bị "vô hiệu hoá" (escape),
        // để nội dung AI/README độc hại không chèn được thẻ <script> chạy thật.
        String html = renderer.toHtml("<script>alert(1)</script>");
        assertThat(html).doesNotContain("<script>");
        assertThat(html).contains("&lt;script&gt;");
    }
}

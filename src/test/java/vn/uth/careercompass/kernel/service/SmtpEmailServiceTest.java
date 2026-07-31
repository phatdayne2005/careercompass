package vn.uth.careercompass.kernel.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Unit test cho {@link SmtpEmailService}.
 * Ta KHÔNG gửi mail thật — chỉ khẳng định service dựng đúng {@link SimpleMailMessage}
 * (from/to/subject/body) rồi bàn giao cho {@link JavaMailSender}.
 */
@ExtendWith(MockitoExtension.class)
class SmtpEmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private SmtpEmailService smtpEmailService;

    @BeforeEach
    void setUp() {
        // @Value("${app.mail.from}") -> tiêm tay khi không có Spring
        ReflectionTestUtils.setField(smtpEmailService, "from", "no-reply@careercompass.vn");
    }

    @Test
    void sendEmail_buildsMessageAndDelegatesToMailSender() {
        smtpEmailService.sendEmail("a@uth.edu.vn", "Chủ đề", "Nội dung");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage msg = captor.getValue();
        assertThat(msg.getFrom()).isEqualTo("no-reply@careercompass.vn");
        assertThat(msg.getTo()).containsExactly("a@uth.edu.vn");
        assertThat(msg.getSubject()).isEqualTo("Chủ đề");
        assertThat(msg.getText()).isEqualTo("Nội dung");
    }
}

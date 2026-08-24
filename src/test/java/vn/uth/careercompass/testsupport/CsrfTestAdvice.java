package vn.uth.careercompass.testsupport;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Supplies the CSRF model attribute required by the shared Thymeleaf layout in MVC slice tests. */
@ControllerAdvice
public class CsrfTestAdvice {

    @ModelAttribute("_csrf")
    public CsrfToken csrfToken() {
        return new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token");
    }
}

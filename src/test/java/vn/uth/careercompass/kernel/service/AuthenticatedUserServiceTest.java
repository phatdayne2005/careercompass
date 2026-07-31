package vn.uth.careercompass.kernel.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.server.ResponseStatusException;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.repository.UserRepository;
import vn.uth.careercompass.kernel.security.CustomUserDetails;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test cho {@link AuthenticatedUserService#requireCurrentUser}.
 *
 * <p>Đây là ví dụ hay: một method có NHIỀU nhánh phân loại "principal" (đối tượng đăng nhập).
 * Ta không có Spring Security thật, nên {@code Authentication} và các principal đều được mock.
 */
@ExtendWith(MockitoExtension.class)
class AuthenticatedUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthenticatedUserService authenticatedUserService;

    @Test
    void requireCurrentUser_whenAuthenticationNull_throws401() {
        assertThatThrownBy(() -> authenticatedUserService.requireCurrentUser(null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Bạn cần đăng nhập");
    }

    @Test
    void requireCurrentUser_whenNotAuthenticated_throws401() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);

        assertThatThrownBy(() -> authenticatedUserService.requireCurrentUser(auth))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void requireCurrentUser_whenPrincipalIsCustomUserDetails_returnsUserDirectly() {
        // Nhánh nhanh nhất: principal đã bọc sẵn User -> lấy thẳng, KHÔNG cần query DB.
        User user = User.builder().email("a@uth.edu.vn").build();
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(new CustomUserDetails(user));

        User result = authenticatedUserService.requireCurrentUser(auth);

        assertThat(result).isEqualTo(user);
    }

    @Test
    void requireCurrentUser_whenPrincipalIsOidcUser_looksUpByEmail() {
        // Login Google -> principal là OidcUser -> lấy email rồi query DB.
        User user = User.builder().email("g@uth.edu.vn").build();
        OidcUser oidcUser = mock(OidcUser.class);
        when(oidcUser.getEmail()).thenReturn("g@uth.edu.vn");
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(oidcUser);
        when(userRepository.findByEmailWithRole("g@uth.edu.vn")).thenReturn(Optional.of(user));

        User result = authenticatedUserService.requireCurrentUser(auth);

        assertThat(result).isEqualTo(user);
    }

    @Test
    void requireCurrentUser_whenPrincipalIsUserDetails_looksUpByUsername() {
        User user = User.builder().email("u@uth.edu.vn").build();
        UserDetails principal = mock(UserDetails.class);
        // lenient(): principal là UserDetails "trần" (không phải CustomUserDetails).
        // getUsername() được gọi trong resolveEmail; dùng lenient để tránh cảnh báo strict-stub
        // nếu nhánh instanceof kiểm CustomUserDetails trước làm Mockito nghĩ stub thừa.
        lenient().when(principal.getUsername()).thenReturn("u@uth.edu.vn");
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(principal);
        when(userRepository.findByEmailWithRole("u@uth.edu.vn")).thenReturn(Optional.of(user));

        User result = authenticatedUserService.requireCurrentUser(auth);

        assertThat(result).isEqualTo(user);
    }

    @Test
    void requireCurrentUser_whenAnonymousStringPrincipal_throws401() {
        // Spring gán principal = "anonymousUser" khi chưa đăng nhập thực sự -> phải bị coi là chưa auth.
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("anonymousUser");

        assertThatThrownBy(() -> authenticatedUserService.requireCurrentUser(auth))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Không xác định được người dùng");
    }

    @Test
    void requireCurrentUser_whenEmailNotInDb_throws401() {
        OidcUser oidcUser = mock(OidcUser.class);
        when(oidcUser.getEmail()).thenReturn("missing@uth.edu.vn");
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(oidcUser);
        when(userRepository.findByEmailWithRole("missing@uth.edu.vn")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticatedUserService.requireCurrentUser(auth))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Không tìm thấy người dùng");
    }
}

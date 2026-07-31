package vn.uth.careercompass.kernel.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.repository.UserRepository;
import vn.uth.careercompass.kernel.security.CustomUserDetails;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit test cho {@link CustomUserDetailsService} — cầu nối giữa User (DB) và Spring Security.
 * Spring gọi {@code loadUserByUsername(email)} khi user đăng nhập form login.
 */
@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void loadUserByUsername_whenFound_returnsCustomUserDetails() {
        User user = User.builder().email("a@uth.edu.vn").passwordHash("HASH").build();
        // Lưu ý: dùng findByEmailWithRole (JOIN FETCH role) chứ không phải findByEmail thường.
        when(userRepository.findByEmailWithRole("a@uth.edu.vn")).thenReturn(Optional.of(user));

        UserDetails result = customUserDetailsService.loadUserByUsername("a@uth.edu.vn");

        assertThat(result).isInstanceOf(CustomUserDetails.class);
        assertThat(result.getUsername()).isEqualTo("a@uth.edu.vn");
        assertThat(result.getPassword()).isEqualTo("HASH");
        // Khẳng định nó bọc đúng User gốc (không tạo user khác)
        assertThat(((CustomUserDetails) result).getUser()).isEqualTo(user);
    }

    @Test
    void loadUserByUsername_whenNotFound_throwsUsernameNotFound() {
        when(userRepository.findByEmailWithRole("ghost@uth.edu.vn")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("ghost@uth.edu.vn"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("ghost@uth.edu.vn");
    }
}

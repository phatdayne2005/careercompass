package vn.uth.careercompass.kernel.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.repository.UserRepository;
import vn.uth.careercompass.kernel.security.CustomUserDetails;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailWithRole(email).orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy user: " + email));
        return new CustomUserDetails(user);
    }
}

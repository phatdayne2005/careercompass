package vn.uth.careercompass.kernel.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.uth.careercompass.kernel.entity.*;
import vn.uth.careercompass.kernel.repository.RoleRepository;
import vn.uth.careercompass.kernel.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ActivityLogService activityLogService;
    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException{
        // Spring gọi Google lấy info chuẩn (email, name)
        OidcUser oidcUser = super.loadUser(userRequest);

        String email = oidcUser.getEmail();
        String name = oidcUser.getFullName();

        // Tìm xem tài khoản đã được tạo chưa để lấy role
        User user = userRepository.findByEmailWithRole(email).orElseGet(() -> createGoogleUser(email, name));

        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().getName().name()));

        return new DefaultOidcUser(
                authorities,
                oidcUser.getIdToken(),
                oidcUser.getUserInfo(),
                "email"
        );
    }

    private User createGoogleUser(String email, String name){
        Role studentRole = roleRepository.findByName(RoleName.STUDENT).orElseThrow(() -> new IllegalStateException("Chưa seed role Student"));
        User user = User.builder().email(email).fullName(name).authProvider(AuthProvider.GOOGLE).enabled(true).role(studentRole).build();
        userRepository.save(user);
        activityLogService.log(user, KernelActivityLogType.REGISTER, "Đăng ký tài khoản qua Google");
        return user;
    }
}

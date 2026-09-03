package vn.uth.careercompass.kernel.web.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RegisterFormDTO {
    @NotBlank
    @Size(max = 100)
    private String fullName;
    // min = 6 là độ dài của email hợp lệ ngắn nhất có thật: a@b.co — một ký tự phần
    // local, một ký tự tên miền, dấu chấm và tên miền cấp cao hai ký tự. Không khai min
    // thì @Email vẫn cho qua "a@b", trong khi chuỗi đó không thể là hộp thư thật.
    @NotBlank
    @Email
    @Size(min = 6, max = 150)
    private String email;
    @NotBlank
    @Size(min = 6, max = 30)
    private String password;
}

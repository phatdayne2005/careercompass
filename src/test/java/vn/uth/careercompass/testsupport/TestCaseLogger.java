package vn.uth.careercompass.testsupport;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;

/**
 * In tên từng test case ra console ngay khi chạy, để trình bày kết quả kiểm thử
 * mà không cần mở mã nguồn.
 *
 * <p>Mặc định TẮT vì chạy toàn bộ hơn 200 test sẽ rất dài dòng. Bật bằng cờ
 * {@code -DshowCases}:
 *
 * <pre>
 *   mvnw test -Dtest=RegisterStandardBvaTest -DshowCases
 * </pre>
 *
 * <p>Tên in ra là nhãn {@code @DisplayName} và {@code @ParameterizedTest(name = ...)}
 * trong mã nguồn, nên khớp từng dòng với bảng thiết kế ở BaoCao-PhanA-HopDen.xlsx.
 *
 * <p>Đăng ký qua ServiceLoader: xem
 * {@code src/test/resources/META-INF/services/org.junit.platform.launcher.TestExecutionListener}.
 */
public class TestCaseLogger implements TestExecutionListener {

    private static final boolean BAT = System.getProperty("showCases") != null;
    private static final String VACH = "-".repeat(78);

    /** Số thứ tự test case trong lớp hiện tại. */
    private int stt;

    @Override
    public void executionStarted(TestIdentifier id) {
        if (!BAT || id.getSource().isEmpty()) {
            return;
        }
        Object nguon = id.getSource().get();
        if (nguon instanceof ClassSource) {
            stt = 0;
            System.out.println();
            System.out.println(VACH);
            System.out.println("  " + id.getDisplayName());
            System.out.println(VACH);
        } else if (nguon instanceof MethodSource && !id.isTest()) {
            // Nhóm @ParameterizedTest: in tiêu đề nhóm, các case in ở executionFinished.
            System.out.println();
            System.out.println("  " + id.getDisplayName());
        }
    }

    @Override
    public void executionFinished(TestIdentifier id, TestExecutionResult ketQua) {
        if (!BAT || !id.isTest()) {
            return;
        }
        String trangThai = switch (ketQua.getStatus()) {
            case SUCCESSFUL -> "PASS";
            case FAILED -> "FAIL";
            case ABORTED -> "HUY ";
        };
        System.out.printf("    %2d. [%s]  %s%n", ++stt, trangThai, id.getDisplayName());
    }

    @Override
    public void executionSkipped(TestIdentifier id, String lyDo) {
        if (!BAT || !id.isTest()) {
            return;
        }
        System.out.printf("    %2d. [BO  ]  %s  (%s)%n", ++stt, id.getDisplayName(), lyDo);
    }
}

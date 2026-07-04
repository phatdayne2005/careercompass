package vn.uth.careercompass.roadmap;

/**
 * Các loại ActivityLog do gói P4 (Roadmap) sinh ra.
 * Theo quyết định 3.3: ActivityLog.type là String, mỗi gói tự định nghĩa loại của mình
 * để không phải sửa kernel khi thêm loại mới.
 */
public final class RoadmapActivityType {
    public static final String NODE_DONE = "NODE_DONE";

    private RoadmapActivityType() {
    }
}

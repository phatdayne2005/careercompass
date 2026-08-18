/**
 * Các bước dùng chung, gắn thêm vào đối tượng "I".
 * Đặt ở đây thao tác lặp lại nhiều test để tránh copy-paste.
 */
module.exports = function () {
  return actor({
    /**
     * Sinh email duy nhất cho mỗi lần chạy.
     *
     * Đây là cốt lõi của chiến lược "mỗi test tự đăng ký user mới": test không
     * phụ thuộc dữ liệu có sẵn, nên chạy lại bao nhiêu lần cũng cùng kết quả
     * và nhiều test chạy song song không giẫm chân nhau.
     */
    uniqueEmail(prefix = 'e2e') {
      const stamp = Date.now().toString(36) + Math.random().toString(36).slice(2, 7);
      return `${prefix}.${stamp}@e2e.test`;
    },
  });
};

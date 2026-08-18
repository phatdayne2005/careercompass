/**
 * Plugin thu kết quả chạy test và ghi ra output/result.json.
 *
 * Vì sao tự viết thay vì dùng reporter có sẵn: cần lấy thêm tag (@P0/@P1),
 * mã test case (TC-xxx) và thời gian chạy từng case để đổ vào báo cáo Excel.
 */
const { event } = require('codeceptjs');
const fs = require('fs');
const path = require('path');

module.exports = function () {
  const results = [];
  const startedAt = new Date();
  // test.duration chưa có giá trị tại thời điểm event passed/failed bắn ra,
  // nên tự bấm giờ từ lúc test bắt đầu.
  let currentStart = 0;

  const record = (test, status, errMsg) => {
    // Tag nằm trong tên test dạng "... @P0", CodeceptJS cũng gom vào test.tags
    const tags = (test.tags || []).map((t) => String(t).replace(/^@/, ''));
    const title = (test.title || '').replace(/\s*@\S+/g, '').trim();
    // Mã test case quy ước đặt ở đầu tên: "TC-AUTH-001 | mô tả..."
    const m = title.match(/^(TC-[A-Z]+-\d+)\s*\|\s*(.*)$/);

    results.push({
      id: m ? m[1] : '',
      title: m ? m[2] : title,
      feature: (test.parent && test.parent.title) || '',
      tags,
      status,                                  // passed | failed | skipped
      durationMs: test.duration || (currentStart ? Date.now() - currentStart : 0),
      error: errMsg || '',
    });
  };

  event.dispatcher.on(event.test.started, () => { currentStart = Date.now(); });
  event.dispatcher.on(event.test.passed, (test) => record(test, 'passed'));
  event.dispatcher.on(event.test.failed, (test, err) =>
    record(test, 'failed', err && err.message ? err.message : String(err)));
  event.dispatcher.on(event.test.skipped, (test) => record(test, 'skipped'));

  event.dispatcher.on(event.all.result, () => {
    const outDir = path.join(__dirname, '..', 'output');
    fs.mkdirSync(outDir, { recursive: true });
    const payload = {
      startedAt: startedAt.toISOString(),
      finishedAt: new Date().toISOString(),
      baseUrl: process.env.BASE_URL || 'http://localhost:8080',
      total: results.length,
      passed: results.filter((r) => r.status === 'passed').length,
      failed: results.filter((r) => r.status === 'failed').length,
      skipped: results.filter((r) => r.status === 'skipped').length,
      tests: results,
    };
    fs.writeFileSync(path.join(outDir, 'result.json'), JSON.stringify(payload, null, 2), 'utf8');
    console.log(`\n[resultCollector] Đã ghi ${results.length} kết quả vào output/result.json`);
  });
};

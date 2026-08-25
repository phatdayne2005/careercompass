/**
 * Sinh báo cáo Excel từ KẾT QUẢ CHẠY THẬT.
 *
 *   node support/generate-report.js          (hoặc: npm run report)
 *
 * Ghép output/result.json (kết quả thật) với support/catalog.js (mô tả case)
 * rồi xuất ra output/BaoCao-KiemThu-<ngày>.xlsx gồm 4 sheet:
 *   1. Tổng quan          - số liệu tổng hợp
 *   2. Chi tiết Test Case - từng case, kết quả thật
 *   3. Ma trận truy vết   - yêu cầu SRS nào đã/chưa được test
 *   4. Danh sách lỗi      - defect phát hiện được
 */
const ExcelJS = require('exceljs');
const fs = require('fs');
const path = require('path');
const catalog = require('./catalog');

const OUT_DIR = path.join(__dirname, '..', 'output');
const RESULT_FILE = path.join(OUT_DIR, 'result.json');

// ----- màu dùng chung -----
const CLR = {
  header: 'FF1F4E79',
  headerText: 'FFFFFFFF',
  pass: 'FFC6EFCE',
  fail: 'FFFFC7CE',
  notrun: 'FFF2F2F2',
  warn: 'FFFFEB9C',
  band: 'FFF7F9FC',
};

function styleHeader(row) {
  row.eachCell((cell) => {
    cell.font = { bold: true, color: { argb: CLR.headerText }, size: 11 };
    cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: CLR.header } };
    cell.alignment = { vertical: 'middle', horizontal: 'center', wrapText: true };
    cell.border = { bottom: { style: 'thin' } };
  });
  row.height = 28;
}

function statusFill(status) {
  if (status === 'Đạt') return CLR.pass;
  if (status === 'Không đạt') return CLR.fail;
  return CLR.notrun;
}

function main() {
  if (!fs.existsSync(RESULT_FILE)) {
    console.error('Chưa có output/result.json. Chạy "npm test" trước đã.');
    process.exit(1);
  }

  const result = JSON.parse(fs.readFileSync(RESULT_FILE, 'utf8'));
  const byId = new Map(result.tests.map((t) => [t.id, t]));

  const wb = new ExcelJS.Workbook();
  wb.creator = 'CareerCompass E2E';
  wb.created = new Date();

  // Gom dữ liệu: mọi case trong danh mục, kèm kết quả thật nếu có.
  const rows = Object.entries(catalog.cases).map(([id, c]) => {
    const run = byId.get(id);
    const status = !run || run.status === 'skipped'
      ? 'Chưa chạy'
      : run.status === 'passed' ? 'Đạt' : 'Không đạt';
    return {
      id,
      ...c,
      status,
      actual: !run || run.status === 'skipped'
        ? 'Chưa thực thi trong lần chạy này'
        : run.status === 'passed'
          ? 'Đúng như kết quả mong đợi'
          : run.error || 'Thất bại',
      durationSec: run ? (run.durationMs / 1000).toFixed(2) : '',
      feature: run ? run.feature : '',
      // Tên luồng lấy từ chính tên Scenario đã chạy (nguồn sự thật duy nhất).
      title: run ? run.title : '',
    };
  });

  const total = rows.length;
  const passed = rows.filter((r) => r.status === 'Đạt').length;
  const failed = rows.filter((r) => r.status === 'Không đạt').length;
  const notRun = rows.filter((r) => r.status === 'Chưa chạy').length;
  const executed = passed + failed;
  const passRate = executed ? ((passed / executed) * 100).toFixed(1) : '0.0';

  // ============ SHEET 1: TỔNG QUAN ============
  const s1 = wb.addWorksheet('1. Tổng quan');
  s1.columns = [{ width: 30 }, { width: 42 }, { width: 14 }, { width: 14 }, { width: 14 }, { width: 14 }];

  s1.mergeCells('A1:F1');
  const title = s1.getCell('A1');
  title.value = `BÁO CÁO KIỂM THỬ GIAO DIỆN - ${catalog.project}`;
  title.font = { bold: true, size: 16, color: { argb: CLR.header } };
  title.alignment = { horizontal: 'center' };
  s1.getRow(1).height = 32;

  const info = [
    ['Dự án', catalog.project],
    ['Giai đoạn', catalog.sprint],
    ['Loại kiểm thử', catalog.testType],
    ['Môi trường kiểm thử', result.baseUrl],
    ['Thời điểm bắt đầu', new Date(result.startedAt).toLocaleString('vi-VN')],
    ['Thời điểm kết thúc', new Date(result.finishedAt).toLocaleString('vi-VN')],
  ];
  s1.addRow([]);
  info.forEach(([k, v]) => {
    const r = s1.addRow([k, v]);
    r.getCell(1).font = { bold: true };
  });

  s1.addRow([]);
  const hdr = s1.addRow(['CHỈ SỐ TỔNG HỢP', 'Giá trị']);
  styleHeader(hdr);
  const metrics = [
    ['Tổng số test case', total],
    ['Đã thực thi', executed],
    ['Đạt', passed],
    ['Không đạt', failed],
    ['Chưa chạy', notRun],
    ['Tỷ lệ đạt (trên số đã chạy)', `${passRate}%`],
    ['Số lỗi phát hiện', catalog.defects.length],
  ];
  metrics.forEach(([k, v]) => {
    const r = s1.addRow([k, v]);
    r.getCell(1).font = { bold: true };
    if (k === 'Đạt') r.getCell(2).fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: CLR.pass } };
    if (k === 'Không đạt' && failed > 0)
      r.getCell(2).fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: CLR.fail } };
  });

  // Thống kê theo module
  s1.addRow([]);
  const modHdr = s1.addRow(['THỐNG KÊ THEO MODULE', 'Tổng', 'Đạt', 'Không đạt', 'Chưa chạy', 'Tỷ lệ đạt']);
  styleHeader(modHdr);
  const modules = [...new Set(rows.map((r) => r.module))];
  modules.forEach((m) => {
    const g = rows.filter((r) => r.module === m);
    const p = g.filter((r) => r.status === 'Đạt').length;
    const f = g.filter((r) => r.status === 'Không đạt').length;
    const n = g.filter((r) => r.status === 'Chưa chạy').length;
    const ex = p + f;
    s1.addRow([m, g.length, p, f, n, ex ? `${((p / ex) * 100).toFixed(0)}%` : '-']);
  });

  // Thống kê theo độ ưu tiên
  s1.addRow([]);
  const priHdr = s1.addRow(['THỐNG KÊ THEO ĐỘ ƯU TIÊN', 'Tổng', 'Đạt', 'Không đạt', 'Chưa chạy', 'Tỷ lệ đạt']);
  styleHeader(priHdr);
  ['P0', 'P1', 'P2'].forEach((p) => {
    const g = rows.filter((r) => r.priority === p);
    if (!g.length) return;
    const ok = g.filter((r) => r.status === 'Đạt').length;
    const f = g.filter((r) => r.status === 'Không đạt').length;
    const n = g.filter((r) => r.status === 'Chưa chạy').length;
    const ex = ok + f;
    const label = p === 'P0' ? 'P0 - Chặn deploy' : p === 'P1' ? 'P1 - Hồi quy' : 'P2 - Phụ thuộc ngoài';
    s1.addRow([label, g.length, ok, f, n, ex ? `${((ok / ex) * 100).toFixed(0)}%` : '-']);
  });

  // ============ SHEET 2: CHI TIẾT TEST CASE ============
  const s2 = wb.addWorksheet('2. Chi tiết Test Case');
  s2.columns = [
    { header: 'Mã TC', key: 'id', width: 15 },
    { header: 'Module', key: 'module', width: 14 },
    { header: 'Tên luồng kiểm thử', key: 'name', width: 42 },
    { header: 'Ưu tiên', key: 'priority', width: 9 },
    { header: 'Loại', key: 'type', width: 12 },
    { header: 'Yêu cầu (SRS)', key: 'fr', width: 14 },
    { header: 'Điều kiện tiên quyết', key: 'pre', width: 32 },
    { header: 'Các bước thực hiện', key: 'steps', width: 46 },
    { header: 'Dữ liệu kiểm thử', key: 'data', width: 26 },
    { header: 'Kết quả mong đợi', key: 'expected', width: 40 },
    { header: 'Kết quả thực tế', key: 'actual', width: 40 },
    { header: 'Trạng thái', key: 'status', width: 13 },
    { header: 'Thời gian (giây)', key: 'dur', width: 14 },
    { header: 'Tự động hoá', key: 'auto', width: 12 },
    { header: 'Mã lỗi', key: 'defect', width: 11 },
  ];
  styleHeader(s2.getRow(1));
  s2.views = [{ state: 'frozen', ySplit: 1 }];

  rows.forEach((r, i) => {
    const row = s2.addRow({
      id: r.id,
      module: r.module,
      name: r.title || r.expected,
      priority: r.priority,
      type: r.type,
      fr: r.fr.join(', ') || '-',
      pre: r.precondition,
      steps: r.steps,
      data: r.data,
      expected: r.expected,
      actual: r.actual,
      status: r.status,
      dur: r.durationSec,
      auto: 'Có',
      defect: r.defect || '',
    });
    row.alignment = { vertical: 'top', wrapText: true };
    row.getCell('status').fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: statusFill(r.status) } };
    row.getCell('status').font = { bold: true };
    row.getCell('status').alignment = { horizontal: 'center', vertical: 'middle' };
    if (i % 2 === 1) {
      ['id', 'module', 'priority', 'type', 'fr'].forEach((k) => {
        row.getCell(k).fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: CLR.band } };
      });
    }
  });
  s2.autoFilter = { from: 'A1', to: { row: 1, column: s2.columnCount } };

  // ============ SHEET 3: MA TRẬN TRUY VẾT ============
  const s3 = wb.addWorksheet('3. Ma trận truy vết');
  s3.columns = [
    { header: 'Mã yêu cầu', key: 'fr', width: 14 },
    { header: 'Mô tả yêu cầu (SRS)', key: 'desc', width: 56 },
    { header: 'Test case phủ', key: 'tcs', width: 34 },
    { header: 'Số case', key: 'count', width: 10 },
    { header: 'Tình trạng phủ', key: 'cover', width: 20 },
    { header: 'Kết quả', key: 'res', width: 16 },
  ];
  styleHeader(s3.getRow(1));
  s3.views = [{ state: 'frozen', ySplit: 1 }];

  Object.entries(catalog.requirements).forEach(([fr, desc]) => {
    const covering = rows.filter((r) => r.fr.includes(fr));
    const anyFail = covering.some((r) => r.status === 'Không đạt');
    const allPass = covering.length > 0 && covering.every((r) => r.status === 'Đạt');
    const row = s3.addRow({
      fr,
      desc,
      tcs: covering.map((r) => r.id).join(', ') || '(chưa có)',
      count: covering.length,
      cover: covering.length ? 'Đã phủ' : 'CHƯA PHỦ',
      res: !covering.length ? '-' : anyFail ? 'Có case không đạt' : allPass ? 'Tất cả đạt' : 'Chưa chạy đủ',
    });
    row.alignment = { vertical: 'top', wrapText: true };
    const fill = !covering.length ? CLR.warn : anyFail ? CLR.fail : CLR.pass;
    row.getCell('cover').fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: fill } };
    row.getCell('cover').font = { bold: true };
  });

  const covered = Object.keys(catalog.requirements).filter((fr) => rows.some((r) => r.fr.includes(fr))).length;
  const totalFr = Object.keys(catalog.requirements).length;
  s3.addRow([]);
  const sum = s3.addRow([
    'TỔNG KẾT',
    `Đã phủ ${covered}/${totalFr} yêu cầu`,
    '',
    '',
    `${((covered / totalFr) * 100).toFixed(0)}%`,
    '',
  ]);
  sum.font = { bold: true };

  // ============ SHEET 4: DANH SÁCH LỖI ============
  const s4 = wb.addWorksheet('4. Danh sách lỗi');
  s4.columns = [
    { header: 'Mã lỗi', key: 'id', width: 11 },
    { header: 'Test case phát hiện', key: 'tc', width: 18 },
    { header: 'Mức nghiêm trọng', key: 'sev', width: 16 },
    { header: 'Tiêu đề', key: 'title', width: 52 },
    { header: 'Mô tả chi tiết', key: 'desc', width: 76 },
    { header: 'Tập tin liên quan', key: 'file', width: 52 },
    { header: 'Trạng thái', key: 'status', width: 16 },
  ];
  styleHeader(s4.getRow(1));
  s4.views = [{ state: 'frozen', ySplit: 1 }];

  if (!catalog.defects.length) {
    s4.addRow({ id: '-', title: 'Không phát hiện lỗi nào trong lần chạy này' });
  } else {
    catalog.defects.forEach((d) => {
      const row = s4.addRow({
        id: d.id, tc: d.testCase, sev: d.severity, title: d.title,
        desc: d.description, file: d.file, status: d.status,
      });
      row.alignment = { vertical: 'top', wrapText: true };
      const sev = { 'Cao': CLR.fail, 'Trung bình': CLR.warn, 'Thấp': CLR.notrun };
      row.getCell('sev').fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: sev[d.severity] || CLR.notrun } };
      row.getCell('sev').font = { bold: true };
    });
  }

  // ----- ghi file -----
  const stamp = new Date(result.finishedAt);
  const name = `BaoCao-KiemThu-${stamp.getFullYear()}${String(stamp.getMonth() + 1).padStart(2, '0')}${String(stamp.getDate()).padStart(2, '0')}.xlsx`;
  const outFile = path.join(OUT_DIR, name);

  return wb.xlsx.writeFile(outFile).then(() => {
    console.log(`\nĐã tạo báo cáo: output/${name}`);
    console.log(`  Tổng: ${total} case | Đạt: ${passed} | Không đạt: ${failed} | Chưa chạy: ${notRun}`);
    console.log(`  Tỷ lệ đạt: ${passRate}%  |  Yêu cầu đã phủ: ${covered}/${totalFr}`);
    if (failed > 0) console.log(`  Có ${failed} case không đạt - xem sheet "4. Danh sách lỗi".`);
  });
}

main();

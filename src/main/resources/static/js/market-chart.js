/**
 * CareerCompass - Market Pulse Chart Initialization
 */

function initKeywordChart(labels, counts) {
    const ctx = document.getElementById('keywordChart');
    if (!ctx) return;

    new Chart(ctx, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [{
                label: 'Số tin tuyển dụng nhắc tới',
                data: counts,
                backgroundColor: 'rgba(99, 102, 241, 0.75)',
                borderColor: '#818cf8',
                borderWidth: 1.5,
                borderRadius: 8,
                hoverBackgroundColor: 'rgba(129, 140, 248, 0.9)',
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
                tooltip: {
                    backgroundColor: '#0f172a',
                    borderColor: 'rgba(255, 255, 255, 0.1)',
                    borderWidth: 1,
                    titleColor: '#ffffff',
                    bodyColor: '#cbd5e1',
                    padding: 10,
                    cornerRadius: 8,
                }
            },
            scales: {
                x: {
                    ticks: {
                        color: '#94a3b8',
                        font: { family: "'Plus Jakarta Sans', sans-serif", weight: '600', size: 11 }
                    },
                    grid: { color: 'rgba(255, 255, 255, 0.04)', drawBorder: false }
                },
                y: {
                    beginAtZero: true,
                    ticks: {
                        color: '#94a3b8',
                        font: { family: "'Plus Jakarta Sans', sans-serif", size: 11 },
                        stepSize: 1
                    },
                    grid: { color: 'rgba(255, 255, 255, 0.04)', drawBorder: false }
                }
            }
        }
    });
}

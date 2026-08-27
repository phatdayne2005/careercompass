/**
 * CareerCompass - Cyber AI Market Pulse Chart Script
 */
document.addEventListener('DOMContentLoaded', () => {
    const canvas = document.getElementById('marketChart');
    if (!canvas) return;

    try {
        let labels = window.marketPulseLabels || [];
        let counts = window.marketPulseCounts || [];

        if (typeof labels === 'string') {
            labels = JSON.parse(labels);
        }
        if (typeof counts === 'string') {
            counts = JSON.parse(counts);
        }

        // Fallback default sample data if empty
        if (!labels || labels.length === 0) {
            labels = ['Java', 'Spring Boot', 'SQL', 'Docker', 'React', 'Git', 'REST API', 'AWS'];
            counts = [120, 95, 80, 65, 55, 50, 45, 40];
        }

        const ctx = canvas.getContext('2d');
        
        // Create Cyber Cyan -> Violet Gradient Fill
        const gradient = ctx.createLinearGradient(0, 0, 0, 300);
        gradient.addColorStop(0, 'rgba(6, 182, 212, 0.85)');
        gradient.addColorStop(0.5, 'rgba(139, 92, 246, 0.75)');
        gradient.addColorStop(1, 'rgba(217, 70, 239, 0.4)');

        new Chart(ctx, {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [{
                    label: 'Nhu cầu tuyển dụng',
                    data: counts,
                    backgroundColor: gradient,
                    borderColor: 'rgba(6, 182, 212, 0.9)',
                    borderWidth: 1.5,
                    borderRadius: 10,
                    borderSkipped: false,
                    hoverBackgroundColor: 'rgba(6, 182, 212, 1)',
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        display: false
                    },
                    tooltip: {
                        backgroundColor: 'rgba(7, 11, 22, 0.9)',
                        titleColor: '#38bdf8',
                        bodyColor: '#f1f5f9',
                        borderColor: 'rgba(6, 182, 212, 0.4)',
                        borderWidth: 1,
                        padding: 12,
                        cornerRadius: 12,
                        titleFont: {
                            family: '"Plus Jakarta Sans", sans-serif',
                            size: 13,
                            weight: 'bold'
                        },
                        bodyFont: {
                            family: '"Plus Jakarta Sans", sans-serif',
                            size: 12
                        }
                    }
                },
                scales: {
                    x: {
                        grid: {
                            color: 'rgba(255, 255, 255, 0.03)'
                        },
                        ticks: {
                            color: '#94a3b8',
                            font: {
                                family: '"Plus Jakarta Sans", sans-serif',
                                size: 11,
                                weight: '600'
                            }
                        }
                    },
                    y: {
                        grid: {
                            color: 'rgba(255, 255, 255, 0.04)'
                        },
                        ticks: {
                            color: '#94a3b8',
                            stepSize: 1,
                            font: {
                                family: '"Plus Jakarta Sans", sans-serif',
                                size: 11
                            }
                        }
                    }
                }
            }
        });
    } catch (e) {
        console.error('Failed to initialize Cyber AI Market Chart:', e);
    }
});

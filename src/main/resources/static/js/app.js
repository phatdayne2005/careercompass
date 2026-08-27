/**
 * CareerCompass - Global Frontend Script
 */

document.addEventListener('DOMContentLoaded', () => {
    // 1. Tự động gán CSRF Token vào request header của HTMX
    document.addEventListener('htmx:configRequest', (event) => {
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
        if (csrfHeader && csrfToken) {
            event.detail.headers[csrfHeader] = csrfToken;
        }
    });

    // 2. Xử lý Mobile Navigation Menu Drawer
    const mobileMenuBtn = document.getElementById('mobileMenuBtn');
    const mobileDrawer = document.getElementById('mobileDrawer');
    const mobileDrawerBackdrop = document.getElementById('mobileDrawerBackdrop');
    const closeMobileMenuBtn = document.getElementById('closeMobileMenuBtn');

    function openMobileMenu() {
        if (mobileDrawer) {
            mobileDrawer.classList.remove('translate-x-full');
            if (mobileDrawerBackdrop) mobileDrawerBackdrop.classList.remove('hidden');
            document.body.classList.add('overflow-hidden');
        }
    }

    function closeMobileMenu() {
        if (mobileDrawer) {
            mobileDrawer.classList.add('translate-x-full');
            if (mobileDrawerBackdrop) mobileDrawerBackdrop.classList.add('hidden');
            document.body.classList.remove('overflow-hidden');
        }
    }

    if (mobileMenuBtn) {
        mobileMenuBtn.addEventListener('click', openMobileMenu);
    }
    if (closeMobileMenuBtn) {
        closeMobileMenuBtn.addEventListener('click', closeMobileMenu);
    }
    if (mobileDrawerBackdrop) {
        mobileDrawerBackdrop.addEventListener('click', closeMobileMenu);
    }

    // 3. Tự động ẩn các thông báo Flash Alert sau 5 giây
    const autoDismissAlerts = document.querySelectorAll('.auto-dismiss-alert');
    autoDismissAlerts.forEach(alert => {
        setTimeout(() => {
            alert.style.transition = 'opacity 0.5s ease-out, transform 0.5s ease-out';
            alert.style.opacity = '0';
            alert.style.transform = 'translateY(-10px)';
            setTimeout(() => alert.remove(), 500);
        }, 5000);
    });
});

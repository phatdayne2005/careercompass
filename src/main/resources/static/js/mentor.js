/**
 * CareerCompass - AI Mentor Chat Scripts
 */

function scrollMentorToBottom() {
    const el = document.getElementById('messageList');
    if (el) {
        el.scrollTo({
            top: el.scrollHeight,
            behavior: 'smooth'
        });
    }
}

document.addEventListener('DOMContentLoaded', () => {
    scrollMentorToBottom();

    // Reset input value after sending message via HTMX
    const chatForm = document.getElementById('chatForm');
    if (chatForm) {
        chatForm.addEventListener('htmx:afterRequest', () => {
            const input = chatForm.querySelector('input[name="content"]');
            if (input) {
                input.value = '';
                input.focus();
            }
        });
    }
});

document.body.addEventListener('htmx:afterSwap', (event) => {
    if (event.detail.target.id === 'messageList') {
        scrollMentorToBottom();
    }
});

(function () {
    function addForumLink(nav) {
        if (!nav || nav.querySelector('a[href="/forum"]')) return;
        const link = document.createElement('a');
        link.href = '/forum';
        link.className = 'forum-nav-link';
        link.textContent = '论坛';
        nav.appendChild(link);
    }

    function mount() {
        const navs = Array.from(document.querySelectorAll('.nav-links'));
        navs.forEach(addForumLink);
        if (!navs.length) {
            const headerRight = document.querySelector('.header-right');
            if (headerRight && !headerRight.querySelector('a[href="/forum"]')) {
                const nav = document.createElement('div');
                nav.className = 'nav-links';
                headerRight.insertBefore(nav, headerRight.firstChild);
                addForumLink(nav);
            }
        }
    }

    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', mount);
    else mount();
})();

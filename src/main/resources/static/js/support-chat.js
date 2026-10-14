// Floating support chat. Opens lazily, loads the reader's thread as an HTML fragment,
// and posts questions without a page reload. CSRF comes from the meta tags in the page head.
(function () {
    var widget = document.getElementById('chat-widget');
    if (!widget) {
        return;
    }

    var panel = document.getElementById('chat-widget-panel');
    var body = document.getElementById('chat-widget-body');
    var form = document.getElementById('chat-widget-form');
    var launcher = document.getElementById('chat-launcher');
    var closeBtn = document.getElementById('chat-widget-close');
    var loaded = false;

    var csrfTokenEl = document.querySelector('meta[name="_csrf"]');
    var csrfHeaderEl = document.querySelector('meta[name="_csrf_header"]');
    var csrfToken = csrfTokenEl ? csrfTokenEl.getAttribute('content') : '';
    var csrfHeader = csrfHeaderEl ? csrfHeaderEl.getAttribute('content') : 'X-CSRF-TOKEN';

    function scrollBottom() {
        body.scrollTop = body.scrollHeight;
    }

    function open() {
        widget.classList.add('open');
        if (!loaded) {
            load();
        } else {
            scrollBottom();
        }
    }

    function close() {
        widget.classList.remove('open');
    }

    function load() {
        fetch(widget.dataset.threadUrl, { headers: { 'Accept': 'text/html' } })
            .then(function (r) { return r.text(); })
            .then(function (html) {
                body.innerHTML = html;
                loaded = true;
                scrollBottom();
            })
            .catch(function () {
                body.innerHTML = '<p class="chat-error">Could not reach the library right now. Please try again.</p>';
            });
    }

    launcher.addEventListener('click', function () {
        if (widget.classList.contains('open')) {
            close();
        } else {
            open();
        }
    });

    if (closeBtn) {
        closeBtn.addEventListener('click', close);
    }

    form.addEventListener('submit', function (event) {
        event.preventDefault();
        var area = form.querySelector('textarea');
        var text = area.value.trim();
        if (!text) {
            return;
        }
        var button = form.querySelector('button');
        button.disabled = true;

        var payload = new URLSearchParams();
        payload.set('body', text);

        var headers = { 'Content-Type': 'application/x-www-form-urlencoded' };
        if (csrfHeader && csrfToken) {
            headers[csrfHeader] = csrfToken;
        }

        fetch(widget.dataset.sendUrl, { method: 'POST', headers: headers, body: payload.toString() })
            .then(function (r) { return r.text(); })
            .then(function (html) {
                body.innerHTML = html;
                area.value = '';
                scrollBottom();
            })
            .catch(function () {
                body.insertAdjacentHTML('beforeend', '<p class="chat-error">That message did not send. Try again.</p>');
            })
            .finally(function () {
                button.disabled = false;
                area.focus();
            });
    });
})();

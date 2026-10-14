/*
 * cart-confirm.js — one dialog, reused by every Buy and Rent button on the shop shelf.
 *
 * Neither button posts the card's form any more. Clicking one fills this dialog with the
 * book's details — including the summary the admin wrote — and the form is submitted only
 * when the reader presses the confirm button. The dialog remembers which of the two the
 * reader clicked, and says so in its heading, its terms line and its button label.
 *
 * Everything is written with textContent, never innerHTML, so a title or summary typed in
 * by an admin can never become markup here.
 */
(function () {
    var modal = document.getElementById('cart-modal');
    if (!modal) { return; }

    var confirmBtn = document.getElementById('cart-confirm');

    var fields = {
        heading: document.getElementById('cart-modal-title'),
        letter: document.getElementById('cart-cover-letter'),
        cover: document.getElementById('cart-cover'),
        name: document.getElementById('cart-name'),
        meta: document.getElementById('cart-meta'),
        tags: document.getElementById('cart-tags'),
        price: document.getElementById('cart-price'),
        rental: document.getElementById('cart-rental'),
        summary: document.getElementById('cart-summary'),
        termsBuy: document.getElementById('cart-terms-buy'),
        termsRent: document.getElementById('cart-terms-rent'),
        stock: document.getElementById('cart-stock')
    };

    /* The two ways the same dialog reads, keyed by the action the form will post. */
    var MODES = {
        BUY: {heading: 'Before you buy', label: 'Add to cart — buy', action: 'BUY'},
        RENT: {heading: 'Before you rent', label: 'Add to cart — rent', action: 'RENT'}
    };

    var pendingForm = null;
    var lastFocus = null;

    function money(value) {
        var number = parseFloat(value);
        return isNaN(number) ? value : '$' + number.toFixed(2);
    }

    function fill(button, mode) {
        var data = button.dataset;

        fields.heading.textContent = mode.heading;
        confirmBtn.textContent = mode.label;
        confirmBtn.dataset.action = mode.action;
        fields.termsBuy.hidden = mode.action !== 'BUY';
        fields.termsRent.hidden = mode.action !== 'RENT';

        fields.letter.textContent = (data.title || '?').charAt(0).toUpperCase();
        fields.name.textContent = data.title || '';
        fields.meta.textContent = (data.author || '') + ' · ' + (data.year || '');
        fields.tags.textContent = [data.genre, data.language].filter(Boolean).join(' · ');
        fields.price.textContent = 'Buy ' + money(data.price);
        fields.rental.textContent = 'Rent ' + money(data.rental);
        fields.summary.textContent = data.summary
            ? data.summary
            : 'No summary added for this book yet — a librarian can write one on the book form.';
        fields.stock.textContent = data.stock > 0 ? data.stock + ' copy/copies ready' : 'Not in stock';

        if (data.cover) {
            fields.cover.removeAttribute('hidden');
            fields.cover.src = data.cover;
            fields.cover.alt = 'Cover of ' + data.title;
            fields.letter.style.visibility = 'hidden';
        } else {
            fields.cover.setAttribute('hidden', 'hidden');
            fields.cover.removeAttribute('src');
            fields.letter.style.visibility = 'visible';
        }
    }

    function open(button) {
        var mode = MODES[button.dataset.mode] || MODES.BUY;
        lastFocus = button;
        pendingForm = button.closest('form');
        fill(button, mode);
        modal.hidden = false;
        document.body.classList.add('cart-modal-open');
        confirmBtn.focus();
    }

    function close() {
        modal.hidden = true;
        pendingForm = null;
        document.body.classList.remove('cart-modal-open');
        if (lastFocus) { lastFocus.focus(); }
    }

    document.addEventListener('click', function (event) {
        var button = event.target.closest('.cart-check');
        if (button) { open(button); return; }
        if (event.target.hasAttribute('data-cart-close')) { close(); }
    });

    document.addEventListener('keydown', function (event) {
        if (!modal.hidden && event.key === 'Escape') { close(); }
    });

    confirmBtn.addEventListener('click', function () {
        if (!pendingForm) { return; }

        /* Both buttons used to be submits carrying name="action". Now that neither posts,
           the chosen action travels as a hidden input added just before submitting. */
        var action = document.createElement('input');
        action.type = 'hidden';
        action.name = 'action';
        action.value = confirmBtn.dataset.action;
        pendingForm.appendChild(action);

        var form = pendingForm;
        close();
        form.submit();
    });
})();

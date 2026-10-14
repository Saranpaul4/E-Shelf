/*
 * Buy-confirm.js — one dialog, reused by every card on the shop shelf.
 *
 * Clicking a card's Buy button no longer posts the form. It fills this dialog with the
 * book's details, including the summary the admin wrote, and the form is only submitted
 * when the reader presses "Add to cart".
 *
 * Everything is written with textContent, never innerHTML, so a title or summary typed in
 * by an admin can never become markup here.
 */
(function () {
    var modal = document.getElementById('buy-modal');
    if (!modal) { return; }

    var confirmBtn = document.getElementById('buy-confirm');
    var pendingForm = null;
    var lastFocus = null;

    var fields = {
        letter: document.getElementById('buy-cover-letter'),
        cover: document.getElementById('buy-cover'),
        name: document.getElementById('buy-name'),
        meta: document.getElementById('buy-meta'),
        tags: document.getElementById('buy-tags'),
        price: document.getElementById('buy-price'),
        rental: document.getElementById('buy-rental'),
        summary: document.getElementById('buy-summary'),
        stock: document.getElementById('buy-stock')
    };

    function money(value) {
        var number = parseFloat(value);
        return isNaN(number) ? value : '$' + number.toFixed(2);
    }

    function fill(button) {
        var data = button.dataset;
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
        lastFocus = button;
        pendingForm = button.closest('form');
        fill(button);
        modal.hidden = false;
        document.body.classList.add('buy-modal-open');
        confirmBtn.focus();
    }

    function close() {
        modal.hidden = true;
        pendingForm = null;
        document.body.classList.remove('buy-modal-open');
        if (lastFocus) { lastFocus.focus(); }
    }

    document.addEventListener('click', function (event) {
        var button = event.target.closest('.buy-check');
        if (button) { open(button); return; }
        if (event.target.hasAttribute('data-buy-close')) { close(); }
    });

    document.addEventListener('keydown', function (event) {
        if (!modal.hidden && event.key === 'Escape') { close(); }
    });

    confirmBtn.addEventListener('click', function () {
        if (!pendingForm) { return; }

        // The Rent button carries action=RENT; a hidden field is how Buy says the same
        // thing now that its button is no longer a submit.
        var action = document.createElement('input');
        action.type = 'hidden';
        action.name = 'action';
        action.value = 'BUY';
        pendingForm.appendChild(action);

        var form = pendingForm;
        close();
        form.submit();
    });
})();

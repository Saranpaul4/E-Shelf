(function () {
    const tabs = document.querySelectorAll(".site-nav a, .auth-tab");

    tabs.forEach(function (tab) {
        tab.addEventListener("pointerdown", function () {
            tab.classList.add("is-pressing");
        });

        ["pointerup", "pointerleave", "pointercancel", "blur"].forEach(function (eventName) {
            tab.addEventListener(eventName, function () {
                tab.classList.remove("is-pressing");
            });
        });
    });
    // Moving the cursor onto a book's cover pops a large copy of that cover next
    // to the pointer. Only the picture reacts, not the title/price block.
    const covers = document.querySelectorAll(".book-card .book-cover");
    const pop = document.getElementById("coverPop");

    if (pop && covers.length > 0) {
        const media = document.getElementById("coverPopMedia");
        const caption = document.getElementById("coverPopCaption");
        const GAP = 16;
        let pinnedCard = null;

        function letterTile(card) {
            media.innerHTML = '<span class="cover-pop-letter">' +
                (card.dataset.letter || "?") + "</span>";
        }

        function fill(card) {
            const large = card.dataset.large;
            if (!large) {
                letterTile(card);
            } else {
                const image = document.createElement("img");
                image.className = "cover-pop-image";
                image.alt = "";
                // If the big size is missing, fall back to the gold letter tile
                // rather than showing an empty frame.
                image.onerror = function () {
                    letterTile(card);
                };
                image.src = large;
                media.replaceChildren(image);
            }
            caption.textContent = card.dataset.title || "";
        }

        function place(x, y) {
            // The popup is visible by now, so measure it for real instead of guessing.
            const popWidth = pop.offsetWidth;
            const popHeight = pop.offsetHeight;
            // Prefer the right of the cursor; flip to the left when it would overflow.
            let left = x + GAP;
            if (left + popWidth > window.innerWidth - 8) {
                left = x - GAP - popWidth;
                pop.dataset.side = "left";
            } else {
                pop.dataset.side = "right";
            }
            const top = Math.min(Math.max(y - popHeight / 2, 8),
                window.innerHeight - popHeight - 8);
            pop.style.left = Math.max(left, 8) + "px";
            pop.style.top = Math.max(top, 8) + "px";
        }

        function show(card, x, y) {
            fill(card);
            pop.hidden = false;
            place(x, y);
        }

        function hide() {
            pop.hidden = true;
            if (pinnedCard) {
                pinnedCard.classList.remove("is-zoom");
            }
            pinnedCard = null;
        }

        covers.forEach(function (cover) {
            const card = cover.closest(".book-card");

            cover.addEventListener("pointerenter", function (event) {
                if (event.pointerType === "mouse") {
                    hide();
                    show(card, event.clientX, event.clientY);
                }
            });

            cover.addEventListener("pointermove", function (event) {
                if (!pop.hidden && event.pointerType === "mouse") {
                    place(event.clientX, event.clientY);
                }
            });

            cover.addEventListener("pointerleave", function (event) {
                if (event.pointerType === "mouse") {
                    hide();
                }
            });

            // Touch has no hover, so tapping the cover pins the popup, second tap closes.
            cover.addEventListener("click", function (event) {
                if (event.pointerType === "mouse") {
                    return;
                }
                if (pinnedCard === card) {
                    hide();
                    return;
                }
                const rect = cover.getBoundingClientRect();
                hide();
                pinnedCard = card;
                card.classList.add("is-zoom");
                show(card, rect.right, rect.top + rect.height / 2);
            });
        });

        window.addEventListener("scroll", hide, { passive: true });
    }
})();

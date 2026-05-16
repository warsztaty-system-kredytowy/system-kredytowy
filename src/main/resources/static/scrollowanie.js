document.addEventListener('DOMContentLoaded', () => {
    const scrollButton = document.getElementById("scrollToTopBtn"); // przycisk przewijania
    const scrollableContent1 = document.querySelector('.content'); 

    function showScrollButton() {
        if (
            (scrollableContent1 && scrollableContent1.scrollTop > 100) || //przycisk pojawia sie po 100 px
            (document.documentElement.scrollTop > 50) // przewinięcie strony głównej
        ) {
            scrollButton.style.display = "block"; 
        } else {
            scrollButton.style.display = "none";
        }
    }

    // Funkcja przewijania obu kontenerów na górę
    function scrollToTop() {
        if (scrollableContent1) {
            scrollableContent1.scrollTo({ top: 0, behavior: 'smooth' }); // Płynne przewijanie kontenera 1
        }
        window.scrollTo({ top: 0, behavior: 'smooth' }); // Przewijanie całego dokumentu
    }

    // Nasłuchiwanie przewijania w obu kontenerach
    if (scrollableContent1) {
        scrollableContent1.addEventListener('scroll', showScrollButton);
    }
    window.addEventListener('scroll', showScrollButton);

    // Dodanie obsługi kliknięcia w przycisk
    if (scrollButton) {
        scrollButton.addEventListener('click', scrollToTop);

        // Ukryj przycisk na początku
        scrollButton.style.display = "none";
    }
});

const slider = document.getElementById("creditSlider");
const output = document.getElementById("creditValue");

// ustaw wartość na start
output.textContent = slider.value;

// reaguj na zmianę
slider.addEventListener("input", function() {
    output.textContent = slider.value;
});
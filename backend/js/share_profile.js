document.shareProfileOnClick = () => {
    // Get the share link input element
    const shareLink = document.getElementById('share-link');
    if (!shareLink) {
        return;
    }

    // Get the include filters checkbox
    const includeFiltersCheckbox = document.getElementById('include-filters');

    // Function to generate the share link
    function generateShareLink() {
        // Get the base URL (current URL without query parameters)
        let baseUrl = window.location.href.split('?')[0];

        // If include filters is checked, add the current filters as query parameters
        if (includeFiltersCheckbox.checked) {
            const filterForm = document.getElementById('player-filters');
            const formData = new FormData(filterForm);

            // Convert form data to URL parameters
            const params = new URLSearchParams();
            for (const [key, value] of formData.entries()) {
                params.append(key, value);
            }

            // Add parameters to the URL
            const queryString = params.toString();
            if (queryString) {
                baseUrl += '?' + queryString;
            }
        }

        // Update the share link input value
        shareLink.value = baseUrl;
    }

    // Generate the link when the checkbox state changes
    includeFiltersCheckbox.addEventListener('change', generateShareLink);

    // Generate the initial link
    generateShareLink();
}

document.copyShareLink = () => {
    const shareLink = document.getElementById('share-link');
    if (!shareLink) {
        return;
    }
    const copyButton = document.getElementById('copy-share-link');
    if (!copyButton) {
        return;
    }
    // Select the text
    shareLink.select();
    shareLink.setSelectionRange(0, 99999); // For mobile devices

    // Copy the text to clipboard
    navigator.clipboard.writeText(shareLink.value)
        .then(() => {
            // Change button text temporarily to indicate success
            const originalText = "Copy";
            copyButton.textContent = 'Copied!';

            // Reset button text after 2 seconds
            setTimeout(() => {
                copyButton.textContent = originalText;
            }, 2000);
        })
        .catch(err => {
            console.error('Failed to copy: ', err);
        });
}

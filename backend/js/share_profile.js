document.shareProfileOnClick = () => {
    // Get the share link input element
    const shareLink = document.getElementById('share-link');
    if (!shareLink) {
        return;
    }

    // Get all the individual filter checkboxes
    const filterCheckboxes = {
        'include-profile': 'profile-name',
        'include-game-version': 'game-version',
        'include-character': 'character',
        'include-stat-type': 'stat-type',
        'include-start-date': 'start-date',
        'include-end-date': 'end-date',
        'include-abandoned': 'include-abandoned'
    };

    // Function to generate the share link
    function generateShareLink() {
        // Get the base URL (current URL without query parameters)
        let baseUrl = window.location.href.split('?')[0];

        // Get the filter form
        const filterForm = document.getElementById('player-filters');
        const formData = new FormData(filterForm);

        // Convert form data to URL parameters, but only include selected filters
        const params = new URLSearchParams();

        for (const [checkboxId, formFieldName] of Object.entries(filterCheckboxes)) {
            const checkbox = document.getElementById(checkboxId);
            if (checkbox && checkbox.checked) {
                if (Array.isArray(formFieldName)) {
                    // Handle multiple form fields (like dates)
                    for (const fieldName of formFieldName) {
                        const value = formData.get(fieldName);
                        if (value) {
                            params.append(fieldName, value);
                        }
                    }
                } else {
                    // Handle single form field
                    const value = formData.get(formFieldName);
                    if (value) {
                        params.append(formFieldName, value);
                    }
                }
            }
        }

        // Add parameters to the URL
        const queryString = params.toString();
        if (queryString) {
            baseUrl += '?' + queryString;
        }

        // Update the share link input value
        shareLink.value = baseUrl;
    }

    // Add event listeners to all filter checkboxes
    for (const checkboxId of Object.keys(filterCheckboxes)) {
        const checkbox = document.getElementById(checkboxId);
        if (checkbox) {
            checkbox.addEventListener('change', generateShareLink);
        }
    }

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

(function () {
	document.addEventListener('DOMContentLoaded', () => {
		closeableElements();
		closeElements();
	});
	
	function closeableElements() {
		const dismissLinks = document.querySelectorAll('a[data-dismiss]');

		dismissLinks.forEach(link => {
			link.addEventListener('click', (e) => {
				e.preventDefault();

				const dismissId = link.getAttribute('data-dismiss');

				// Get existing dismissed list or initialize empty array
				let dismissed = [];
				const stored = localStorage.getItem('dismissed');
				if (stored) {
					dismissed = JSON.parse(stored);
				}

				// Add to list if not already present
				if (!dismissed.includes(dismissId)) {
					dismissed.push(dismissId);
				}

				// Store back to localStorage
				localStorage.setItem('dismissed', JSON.stringify(dismissed));

				const element = document.getElementById(dismissId);
				if (element) {
					element.style.display = 'none';
				}
			});
		});
	}

	function closeElements() {
		const stored = localStorage.getItem('dismissed');
		if (!stored) return;

		let dismissed = [];
		try {
			dismissed = JSON.parse(stored);
		} catch (e) {
			return;
		}

		// Hide elements with matching IDs
		dismissed.forEach(id => {
			const element = document.getElementById(id);
			if (element) {
				element.style.display = 'none';
			}
		});
	}

})();

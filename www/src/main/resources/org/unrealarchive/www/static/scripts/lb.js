(function () {
	let img, overlay, btnClose, btnPrev, btnNext;
	let images = [];
	let currentIndex = -1;

	document.addEventListener('DOMContentLoaded', () => {
		init();
	});

	function init() {
		// Overlay
		overlay = document.createElement('div');
		overlay.classList.add('lightbox');

		// Close button
		btnClose = document.createElement('button');
		btnClose.classList.add('lightbox-close');
		btnClose.textContent = '×';
		btnClose.addEventListener('click', close);

		// Previous button
		btnPrev = document.createElement('button');
		btnPrev.classList.add('lightbox-prev');
		btnPrev.textContent = '‹';
		btnPrev.addEventListener('click', (e) => {
			e.stopPropagation();
			navigate(-1);
		});

		// Next button
		btnNext = document.createElement('button');
		btnNext.classList.add('lightbox-next');
		btnNext.textContent = '›';
		btnNext.addEventListener('click', (e) => {
			e.stopPropagation();
			navigate(1);
		});

		// Image
		img = document.createElement('img');
		img.addEventListener('click', (e) => e.stopPropagation());
		img.addEventListener('load', () => {
			let w = img.naturalWidth;
			let h = img.naturalHeight;
			if (w <= 256) {
				w *= 3;
				h *= 3;
			} else if (w <= 512) {
				w *= 2;
				h *= 2;
			}
			img.setAttribute('width', w);
			img.setAttribute('height', h);
			img.style.left = `calc(50% - ${w / 2}px)`;
			img.style.top = `calc(50% - ${h / 2}px)`;
		});

		overlay.appendChild(btnClose);
		overlay.appendChild(btnPrev);
		overlay.appendChild(img);
		overlay.appendChild(btnNext);
		overlay.addEventListener('click', close);

		document.body.appendChild(overlay);

		// Keyboard navigation
		document.addEventListener('keydown', (e) => {
			if (!overlay.classList.contains('lightbox-visible')) return;
			if (e.key === 'Escape') close();
			else if (e.key === 'ArrowLeft') navigate(-1);
			else if (e.key === 'ArrowRight') navigate(1);
		});

		scanImages();
	}

	function scanImages() {
		const els = document.querySelectorAll('img.lb, a[href$=".png"], a[href$=".jpg"], a[href$=".jpeg"], a[href$=".gif"]');

		images = [];
		els.forEach((el, i) => {
			const src = el.hasAttribute('href') ? el.href : el.src;
			images.push(src);
			el.style.cursor = 'pointer';
			el.addEventListener('click', (e) => {
				e.preventDefault();
				open(i);
			});
		});
	}

	function open(index) {
		currentIndex = index;
		setImage(images[currentIndex]);
		overlay.classList.add('lightbox-visible');
		updateNav();
	}

	function close() {
		overlay.classList.remove('lightbox-visible');
	}

	function navigate(direction) {
		currentIndex += direction;
		if (currentIndex < 0) currentIndex = images.length - 1;
		if (currentIndex >= images.length) currentIndex = 0;
		setImage(images[currentIndex]);
		updateNav();
	}

	function updateNav() {
		btnPrev.style.display = images.length > 1 ? '' : 'none';
		btnNext.style.display = images.length > 1 ? '' : 'none';
	}

	function setImage(src) {
		img.removeAttribute('width');
		img.removeAttribute('height');
		img.setAttribute('src', src);
	}
})();

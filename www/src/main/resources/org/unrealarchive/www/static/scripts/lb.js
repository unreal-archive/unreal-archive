(function() {
	document.addEventListener('DOMContentLoaded', () => {
		this.init();
	});

	/**
	 * Create the lightbox and image elements, and add them to the body
	 * of the document.
	 */
	this.init = function() {
		this.lightbox = document.createElement('a');
		this.img = document.createElement('img');

		// this.lightbox.setAttribute('href', '#_');
		this.lightbox.addEventListener('click', e => {
			e.preventDefault();
			document.location.replace('#');
		});

		this.lightbox.setAttribute('id', 'lb');
		this.lightbox.classList.add('lightbox');
		this.lightbox.appendChild(this.img);

		document.getElementsByTagName('body')[0].appendChild(this.lightbox);

		// special case, for small native images, let's just make them bigger
		this.img.addEventListener('load', () => {
			let w = this.img.width;
			let h = this.img.height;
			if (this.img.width <= 512 && !this.img.getAttribute('sized')) {
				w *= 2;
				h *= 2;
			}
			this.img.setAttribute('width', w);
			this.img.setAttribute('height', h);
			this.img.setAttribute('sized', 1);
			this.img.style.left = `calc(50% - ${w / 2}px)`;
			this.img.style.top = `calc(50% - ${h / 2}px)`;
		});

		/* now we can look for image links on the page */
		this.scanImages();
	}

	/**
	 * Search through all links to images on the page. Any image link will
	 * have its target appear in the lightbox, if Javascript is available,
	 * otherwise it will function as a normal link, still supporting middle-
	 * clicking et al.
	 */
	this.scanImages = function() {
		/* find links to png, jpg and gif images */
		const imgs = document.querySelectorAll('img.lb, a[href$=".png"], a[href$=".jpg"], a[href$=".jpeg"], a[href$=".gif"]');

		/* for each link attach a click handler to set the image location and
		   show the lightbox */
		imgs.forEach(l => {
			l.addEventListener('click', e => {
				e.preventDefault();
				if (l.hasAttribute('href')) this.setImage(l.href);
				else this.setImage(l.src);
				document.location.replace('#lb');
			});
			l.style.cursor = 'pointer';
		});
	}

	/**
	 * Set the image element source to the provided URL.
	 */
	this.setImage = function(img) {
		this.img.removeAttribute('width');
		this.img.removeAttribute('height');
		this.img.removeAttribute('sized');

		this.img.setAttribute('src', img);
	}
})();

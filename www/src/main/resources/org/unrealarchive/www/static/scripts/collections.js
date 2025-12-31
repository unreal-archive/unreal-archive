/* Unreal Archive Collections - LocalStorage management and Add-to-Collection UI */
(function() {
	const KEY = 'ua.collections';

	function load() {
		try {
			const data = JSON.parse(localStorage.getItem(KEY) || '[]');
			return Array.isArray(data) ? data : [];
		} catch (e) {
			return [];
		}
	}

	function save(list) {
		localStorage.setItem(KEY, JSON.stringify(list));
	}

	function newId() {
		return 'c_' + Date.now().toString(36) + Math.random().toString(36).slice(2, 7);
	}

	function ensureCreatedDate(c) {
		if (!c.createdDate) c.createdDate = new Date().toISOString().substring(0, 10);
		return c;
	}

	/* UI for index page */
	function renderIndex() {
		const root = document.getElementById('local-collections-root');
		if (!root) return;

		function setupNewButton() {
			const btn = document.getElementById('new-collection-btn');
			if (!btn) return;

			btn.addEventListener('click', () => {
				const list = load();
				list.unshift(ensureCreatedDate({
					id: newId(),
					title: 'Untitled Collection',
					description: '',
					author: '',
					items: [],
					image: null
				}));
				save(list);
				render();
			});
		}

		const render = () => {
			const data = load();
			root.innerHTML = '';

			if (data.length === 0) {
				const p = document.createElement('p');
				p.textContent = 'Nothing here yet.';
				root.appendChild(p);
				return;
			}

			const outer = document.createElement('div');
			data.forEach((c, idx) => {
				const inner = document.createElement('div');
				inner.className = 'collection';

				const h = document.createElement('h3');
				h.className = 'title';
				h.textContent = c.title || 'Untitled Collection';
				inner.appendChild(h);

				const form = document.createElement('div');
				form.className = 'editor';
				form.innerHTML = `
				  <div class="label-input"><label>Title</label><span><input type="text" value="${(c.title || '').replace(/"/g, '&quot;')}"></span></div>
				  <div class="label-input"><label>Author</label><span><input type="text" value="${(c.author || '').replace(/"/g, '&quot;')}"></span></div>
				  <div class="label-input"><label>Description</label><span><textarea rows="4">${(c.description || '').replace(/</g, '&lt;')}</textarea></span></div>
				  <div class="label-input"><label>Cover Image</label><span><button type="button" class="col-img">Set Image</button></span></div>
		
				  <div class="label-input">
					<label>Items in collection (${c.items.length})</label>
					<span><ul class="ua-items-list"></ul></span>
				  </div>
		
				  <div class="label-input"><span>
					<button type="button" class="col-del">Delete Collection</button>
					<button type="button" class="col-save">Finalise and Submit</button>
				  </span></div>
				`;

				const [titleI, authorI, descI] = form.querySelectorAll('input,textarea');

				titleI.addEventListener('input', () => {
					const list = load();
					list[idx].title = titleI.value;
					save(list);
					h.textContent = titleI.value || 'Untitled Collection';
				});
				authorI.addEventListener('input', () => {
					const list = load();
					list[idx].author = authorI.value;
					save(list);
				});
				descI.addEventListener('input', () => {
					const list = load();
					list[idx].description = descI.value;
					save(list);
				});

				const itemsListEl = form.querySelector('.ua-items-list');

				const renderItems = () => {
					const list = load();
					const coll = list[idx];
					const items = (coll && Array.isArray(coll.items)) ? coll.items : [];

					itemsListEl.innerHTML = '';

					if (items.length === 0) {
						const emptyLi = document.createElement('li');
						emptyLi.textContent = '(No items yet)';
						itemsListEl.appendChild(emptyLi);
						return;
					}

					items.forEach((it, itemIdx) => {
						const row = document.createElement('li');

						const a = document.createElement('a');
						a.textContent = it.title || it.reference || 'Untitled item';
						a.href = it.url || '#';
						a.target = '_self';
						a.rel = 'noopener';
						row.appendChild(a);

						const del = document.createElement('button');
						del.type = 'button';
						del.className = 'ua-item-del';
						del.textContent = 'Remove';
						del.style.marginLeft = '8px';
						del.addEventListener('click', () => {
							const l2 = load();
							const c2 = l2[idx];
							if (!c2 || !Array.isArray(c2.items)) return;

							c2.items.splice(itemIdx, 1);
							save(l2);
							renderItems();
						});
						row.appendChild(del);

						itemsListEl.appendChild(row);
					});
				};

				renderItems();

				form.querySelector('.col-del').addEventListener('click', () => {
					if (confirm('Are you sure you want to delete this collection? This cannot be undone.')) {
						const list = load();
						list.splice(idx, 1);
						save(list);
						render();
					}
				});
				form.querySelector('.col-save').addEventListener('click', () => {
					if (confirm('Are you sure you want to submit this collection? Once submitted, it cannot be edited.')) {
						console.log(JSON.stringify(c, null, 2));
					}
				});

				const imgButton = form.querySelector('.col-img');
				if (c.image) {
					const img = document.createElement('img');
					img.src = c.image;
					img.style.maxWidth = '100px';
					img.style.maxHeight = '100px';
					imgButton.parentNode.insertBefore(img, imgButton);

					imgButton.textContent = 'Remove Image';
					imgButton.addEventListener('click', () => {
						const list = load();
						list[idx].image = null;
						save(list);
						render();
					});
				} else {
					imgButton.textContent = 'Set Image';
					imgButton.addEventListener('click', () => {
						const input = document.createElement('input');
						input.type = 'file';
						input.accept = 'image/*';
						input.onchange = () => {
							if (!input.files.length) return;
							const file = input.files[0];
							if (file.size > 1024 * 1024) {
								alert('Image must be 1MB or smaller.');
								return;
							}
							const reader = new FileReader();
							reader.onload = () => {
								const list = load();
								list[idx].image = reader.result;
								save(list);
								render();
							};
							reader.readAsDataURL(file);
						};
						input.click();
					});
				}

				inner.appendChild(form);
				outer.appendChild(inner);
			});

			root.appendChild(outer);
		};

		setupNewButton();
		render();
	}

	/* Utility to detect content details on current page */
	function detectContent() {
		const ogId = document.querySelector('head meta[property="og:id"]');
		const ogTitle = document.querySelector('head meta[property="og:title"]');

		const reference = (ogId && ogId.getAttribute('content') || '').trim();
		if (!reference) return null;

		const title = (ogTitle && ogTitle.getAttribute('content') || '').trim()
			|| (document.title || 'Untitled').trim();

		return {title, reference, url: location.pathname};
	}

	/* Global add-to-collection control */
	function injectAddControl() {
		const item = detectContent();
		if (!item) return;

		const data = load();
		if (data.length === 0) return;

		const wrap = document.createElement('div');
		wrap.className = 'collections-add';

		const label = document.createElement('span');
		label.textContent = 'Add to collection:';
		wrap.appendChild(label);
		const sel = document.createElement('select');
		data.forEach((c, i) => {
			const o = document.createElement('option');
			o.value = i;
			o.textContent = c.title || 'Untitled';
			sel.appendChild(o);
		});
		wrap.appendChild(sel);

		const btn = document.createElement('button');
		btn.type = 'button';
		btn.textContent = 'Add';
		btn.addEventListener('click', () => {
			const list = load();
			const chosen = list[parseInt(sel.value, 10)];
			if (!chosen.items) chosen.items = [];
			// Avoid duplicate by same reference
			if (!chosen.items.some(x => x.reference === item.reference)) {
				chosen.items.push({title: item.title, reference: item.reference, url: item.url});
				save(list);
				btn.textContent = 'Added!';
				setTimeout(() => btn.textContent = 'Add', 1200);
			} else {
				btn.textContent = 'Already in';
				setTimeout(() => btn.textContent = 'Add', 1200);
			}
		});
		wrap.appendChild(btn);

		const content = document.querySelector('.contentpage .content-body');
		content.insertBefore(wrap, content.firstChild);
	}

	document.addEventListener('DOMContentLoaded', function() {
		renderIndex();
		injectAddControl();
	});
})();

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

		const render = () => {
			const data = load();
			root.innerHTML = '';

			const btn = document.createElement('button');
			btn.type = 'button';
			btn.textContent = 'New Collection';
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

			if (data.length === 0) {
				const p = document.createElement('p');
				p.textContent = 'This browser has no local collections yet.';
				root.appendChild(p);
				const p2 = document.createElement('p');
				p2.appendChild(btn);
				root.appendChild(p2);
				return;
			}

			const ul = document.createElement('ul');
			data.forEach((c, idx) => {
				const li = document.createElement('li');
				const h = document.createElement('div');
				h.className = 'title';
				h.textContent = c.title || 'Untitled Collection';
				li.appendChild(h);

				const meta = document.createElement('div');
				meta.className = 'meta';
				meta.innerHTML = `${c.author || 'Unknown'} • ${c.createdDate || ''}`;
				li.appendChild(meta);

				const form = document.createElement('div');
				form.className = 'editor';
				form.innerHTML = `
          <div class="label-value"><label>Title</label><span><input type="text" value="${(c.title || '').replace(/"/g, '&quot;')}"></span></div>
          <div class="label-value"><label>Author</label><span><input type="text" value="${(c.author || '').replace(/"/g, '&quot;')}"></span></div>
          <div class="label-value"><label>Description</label><span><textarea rows="4">${(c.description || '').replace(/</g, '&lt;')}</textarea></span></div>

          <div class="label-value">
            <label>Items</label>
            <span>
              <div class="ua-items-meta"><span class="ua-items-count"></span></div>
              <ul class="ua-items-list"></ul>
            </span>
          </div>

          <div class="label-value"><button type="button" class="ua-del">Delete</button></div>
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
					meta.innerHTML = `${authorI.value || 'Unknown'} • ${c.createdDate || ''}`;
				});
				descI.addEventListener('input', () => {
					const list = load();
					list[idx].description = descI.value;
					save(list);
				});

				const itemsCountEl = form.querySelector('.ua-items-count');
				const itemsListEl = form.querySelector('.ua-items-list');

				const renderItems = () => {
					const list = load();
					const coll = list[idx];
					const items = (coll && Array.isArray(coll.items)) ? coll.items : [];

					itemsCountEl.textContent = `${items.length} item(s)`;
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

				form.querySelector('.ua-del').addEventListener('click', () => {
					const list = load();
					list.splice(idx, 1);
					save(list);
					render();
				});

				li.appendChild(form);

				// --- Image handling UI ---
				const imageSection = document.createElement('div');
				imageSection.className = 'collection-image';
				if (c.image) {
					const img = document.createElement('img');
					img.src = c.image;
					img.style.maxWidth = '100px';
					img.style.maxHeight = '100px';
					imageSection.appendChild(img);

					const removeBtn = document.createElement('button');
					removeBtn.type = 'button';
					removeBtn.textContent = 'Remove Image';
					removeBtn.style.marginLeft = '8px';
					removeBtn.addEventListener('click', () => {
						const list = load();
						list[idx].image = null;
						save(list);
						render();
					});
					imageSection.appendChild(removeBtn);
				} else {
					const setBtn = document.createElement('button');
					setBtn.type = 'button';
					setBtn.textContent = 'Set Image';
					setBtn.addEventListener('click', () => {
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
					imageSection.appendChild(setBtn);
				}
				li.appendChild(imageSection);

				const saveBtn = document.createElement('button');
				saveBtn.type = 'button';
				saveBtn.textContent = 'Save to console';
				saveBtn.addEventListener('click', () => {
					console.log(JSON.stringify(c, null, 2));
				});
				li.appendChild(saveBtn);

				ul.appendChild(li);
			});

			root.appendChild(btn);
			root.appendChild(ul);
		};

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
		wrap.style.position = 'fixed';
		wrap.style.right = '10px';
		wrap.style.bottom = '10px';
		wrap.style.zIndex = '9999';
		wrap.style.background = 'var(--bg)';
		wrap.style.border = '1px solid var(--border)';
		wrap.style.padding = '6px 8px';
		wrap.style.borderRadius = '4px';

		const label = document.createElement('span');
		label.textContent = 'Add to collection:';
		label.style.marginRight = '6px';
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
		btn.style.marginLeft = '6px';
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

		document.body.appendChild(wrap);
	}

	document.addEventListener('DOMContentLoaded', function() {
		renderIndex();
		injectAddControl();
	});
})();

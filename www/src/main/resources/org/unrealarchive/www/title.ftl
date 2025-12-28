<#include "macros.ftl">
<header>
	<div class="page">
		<div class="heading">
			<a href="${homeOverride!relPath(siteRoot + "/index.html")}" class="header">
				<img src="${staticPath()}/images/logo.png" alt="Unreal Archive" width="80" height="80"/>
				<span class="a">UNREAL</span><span class="b">ARCHIVE</span>
			</a>
			<div class="burger">
				<label for="hamburger">&#9776;</label>
			</div>
			<div class="menu">
				<input type="checkbox" id="hamburger"/>
				<ul>
					<#if features??>
						<#if features.search><li><a href="${siteUrl}/search/index.html"><@icon "search"/>Search</a></li></#if>
						<#if features.latest><li><a href="${siteUrl}/latest/index.html"><@icon "bulb"/>Latest</a></li></#if>
						<#if features.submit><li><a href="${siteUrl}/submit/index.html"><@icon "upload"/>Submit</a></li></#if>
						<#if features.collections><li><a href="${siteUrl}/collections/index.html"><@icon "collection"/>Collections</a></li></#if>
					</#if>
					<li><a id="theme-switcher" style="cursor:pointer"><@icon "palette"/></a></li>
				</ul>
			</div>
		</div>
	</div>
	<script>
		header = document.querySelector('header');
		document.addEventListener('scroll', function(e) {
			if (window.scrollY > 0) {
				header.classList.add('scrolled');
			} else {
				header.classList.remove('scrolled');
			}
		});

	  const themes = ['default', 'red', 'green', 'grey', 'darkgrey'];

	  function setTheme(theme, save) {
		  if (save) localStorage.setItem('theme', theme);
		  document.documentElement.setAttribute('data-theme', theme);
	  }

	  document.querySelector('#theme-switcher').addEventListener('click', () => {
		  let next = themes.indexOf(theme) + 1;
		  if (next === -1 || next === themes.length) next = 0;
		  theme = themes[next];
		  setTheme(theme, true);
	  });
	</script>
</header>

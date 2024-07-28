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
						<#if features.latest><li><a href="${siteUrl}/latest/index.html"><@icon "bulb"/>Latest Additions</a></li></#if>
						<#if features.submit><li><a href="${siteUrl}/submit/index.html"><@icon "upload"/>Submit Content</a></li></#if>
					</#if>
					<li>
						<a id="theme-switcher" style="cursor:pointer">
                <@icon "moon-stars"/>Theme
						</a>
					</li>
				</ul>
			</div>
		</div>
	</div>
	<script>
		function setTheme(theme, save) {
		  if (save) localStorage.setItem('theme', theme);
		  document.documentElement.setAttribute('data-theme', theme);
		}

		document.addEventListener('DOMContentLoaded', () => {
		  if (theme || !window.matchMedia) return;

		  theme = 'light';
		  if (window.matchMedia('(prefers-color-scheme:dark)').matches) theme = 'dark';
          setTheme(theme, false);
          
          window.matchMedia('(prefers-color-scheme:dark)').addEventListener('change', e => {
            setTheme(e.matches ? "dark" : "light", false);
		  });
		});

		document.querySelector('#theme-switcher').addEventListener('click', () => {
		  if (!theme || theme === 'dark') theme = 'light';
		  else theme = 'dark';
		  setTheme(theme, true);
		});
	</script>
</header>

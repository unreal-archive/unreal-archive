<#include "_header.ftl">

	<section class="header">
		<h1>
			Games
		</h1>
	</section>
	<article class="biglist">
		<ul>
		<#list games.games as k, v>
			<li>
				<span class="meta">${v.maps}</span>
				<a href="${v.path}/index.html">${v.name}</a>
			</li>
		</#list>
		</ul>
	</article>

<#include "_footer.ftl">
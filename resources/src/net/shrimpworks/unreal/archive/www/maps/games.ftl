<#include "_header.ftl">

	<section class="header">
		<h1>
			Games
		</h1>
	</section>
	<article>
		<ul>
		<#list games.games as k, v>
			<li><a href="${v.path}/index.html">${v.name} (${v.maps})</a></li>
		</#list>
		</ul>
	</article>

<#include "_footer.ftl">
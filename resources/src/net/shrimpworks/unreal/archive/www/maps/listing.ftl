<#include "_header.ftl">

	<section class="header">
		<h1>
		${game.name} / ${gametype.name} / ${letter.letter} / pg ${page.number}
		</h1>
	</section>
	<article>
		<ul>
		<#list page.maps as m>
			<li><a href="${m.slug}.html">${m.map.title}</a></li>
		</#list>
		</ul>
	</article>

<#include "_footer.ftl">
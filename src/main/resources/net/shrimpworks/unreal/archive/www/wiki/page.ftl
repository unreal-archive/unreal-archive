<#assign extraCss="wiki.css"/>
<#assign headerbg>${staticPath()}/images/wiki.png</#assign>

<#assign ogDescription="heh">
<#assign ogImage=headerbg>

<#include "../_header.ftl">
<#include "../macros.ftl">

	<@heading bg=[headerbg]>
		${title}
	</@heading>

	<@content class="document wiki">
		<section class="readable">
			${page?no_esc}
		</section>
	</@content>

<#include "../_footer.ftl">
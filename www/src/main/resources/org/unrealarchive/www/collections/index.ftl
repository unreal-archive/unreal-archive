<#include "../_header.ftl">
<#include "../macros.ftl">

<@heading>
	Collections
</@heading>

<@content class="biglist bigger">
	<section>
		<h2><@icon "collection"/>Published Collections</h2>
      <#if (collections?size) == 0>
				<p>No published collections available.</p>
      <#else>
				<ul class="biglist">
					<#list collections as c>
						<#if c.collection.leadImage?has_content>
							<#assign bgi=rootPath(c.collection.leadImage) />
						<#else>
							<#assign bgi=""/>
						</#if>
						<@bigitem link='${relPath(c.path)}' bg="${bgi}" meta='${c.collection.items?size}'>${c.collection.title}</@bigitem>
					</#list>
				</ul>
      </#if>
	</section>
</@content>

<#include "../_footer.ftl">

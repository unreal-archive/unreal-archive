<#include "../_header.ftl">
<#include "../macros.ftl">

<@heading>
	Collections
</@heading>

<@content class="biglist bigger">
	<section class="sectionInfo box">
		<p>
			These are convenient content collections created by the community. They provide a one-stop place to browse or download
			groups of files and content.
		</p>
		<p>
			If you'd like to create a new collection, you can use the <a href="./editor.html">Collections Editor</a>.
		</p>
	</section>
	<section>
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
	</section>
</@content>

<#include "../_footer.ftl">

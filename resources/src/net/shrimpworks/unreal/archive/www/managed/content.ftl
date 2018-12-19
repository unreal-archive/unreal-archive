<#include "../_header.ftl">

<#assign headerbg>${static!"static"}/images/none-managed.png</#assign>

<#if content.managed.titleImage??>
	<#assign headerbg>${content.managed.titleImage}</#assign>
</#if>

	<section class="header" style="background-image: url('${headerbg}')">
		<h1>
			${title}
		</h1>
	</section>

	<article class="managed">
		<div class="meta">
			<div class="label-value">
				<label>Author</label><span>${content.managed.author}</span>
			</div>
			<div class="label-value">
				<label>Release Date</label><span>${content.managed.releaseDate!"-"}</span>
			</div>
			<div class="label-value">
				<label>Date Added</label><span>${content.managed.createdDate}</span>
			</div>
			<div class="label-value">
				<label>Last Updated</label><span>${content.managed.updatedDate}</span>
			</div>
			<div class="label-value">
				<label>Summary</label><span>${content.managed.description}</span>
			</div>
		</div>

		<div class="content readable">
			${document}
		</div>
	</article>

<#include "../_footer.ftl">
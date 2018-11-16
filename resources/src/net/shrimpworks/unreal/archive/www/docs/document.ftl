<#include "../_header.ftl">

	<section class="header" <#if document.titleImage??>style="background-image: url('${document.titleImage}')"</#if>>
		<h1>
			${title}
		</h1>
	</section>
	<article class="document">
		<div class="meta">
			<div class="label-value">
				<label>Author</label><span>${document.author}</span>
			</div>
			<div class="label-value">
				<label>Created Date</label><span>${document.createdDate}</span>
			</div>
			<div class="label-value">
				<label>Last Updated</label><span>${document.updatedDate}</span>
			</div>
			<div class="label-value">
				<label>Summary</label><span>${document.description}</span>
			</div>
		</div>
		<div class="content">
			${content}
		</div>
	</article>

<#include "../_footer.ftl">
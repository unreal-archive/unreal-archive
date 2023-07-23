<#assign extraCss="submit.css"/>
<#include "../_header.ftl">
<#include "../macros.ftl">

<@content class="readable">

	<h1>
		Submit Content
	</h1>

	<div id="form">
		<div id="upload-block">
			<div id="upload-controls" class="display-block">
				<h2>Choose Files to Submit</h2>

				<div id="upload" class="display-block">
					<input type="file" id="files" accept=".zip,.rar,.7z,.ace,.gz,.bz2,.tar,.tgz,.exe,.umod,.ut2mod,.ut4mod" multiple style="display:none">
					<button id="select-files"><img src="${staticPath()}/images/icons/file-plus.svg" alt="Add files"/> Add Files</button>
					<button id="upload-files"><img src="${staticPath()}/images/icons/upload.svg" alt="Upload"/> Upload!</button>
					<select id="upload-type">
						<option value="auto">Auto-Detect File Types</option>
						<option value="map">Map</option>
						<option value="map_pack">Map Pack</option>
						<option value="mutator">Mutator</option>
						<option value="skin">Skin</option>
						<option value="model">Model</option>
						<option value="voice">Voice</option>
					</select>
				</div>

				<div id="url">
					<input type="text" id="link" placeholder="paste link here"/>
					<button id="upload-url"><img src="${staticPath()}/images/icons/upload.svg" alt="Upload"/> Submit!</button>
				</div>
			</div>

			<div id="progress-controls">
				<button id="abort">Cancel Upload</button>
				<progress id="progress" value="0" max="100" style="width: 100%"></progress>
			</div>

			<div id="files-list">
				<h2>Selected Files</h2>
			</div>
		</div>

		<div id="info-block">
			<div id="words" class="display-block">
				<h2>Information</h2>
				<p>
					To upload files, you may select one or more <b>.zip, .rar, .7z,
						.ace, .gz, .bz2, .tar, .tgz, .exe, .umod, ut2mod or .ut4mod</b>
					files to upload in a batch.
				</p>
				<p>
					Files you submit will be added to a queue and processed as in
					order of submission, so on occasion <b>you may need to wait a
						while</b> before your files are processed.
				</p>
				<p>
					If processed successfully, new submissions will be indexed, hosted
					and listed on the website as soon as it has been <b>reviewed and
						merged</b> into
					<a href="https://github.com/unreal-archive/unreal-archive-data">
						the main content archive
					</a>.
				</p>
				<p>
					<b>Tip:</b> If you're submitting your own content, remember to make
					sure the Author is set correctly. For maps, this is set in the
					LevelInfo of the map itself, for all other content, add a ReadMe
					file and put something like <b>Author: Your Name</b> in it so it
					can be correctly attributed to you.
				</p>
				<p>
					<b>Tip:</b> Make sure all required files to use your content are
					included in the uploaded archive, this includes textures, sounds,
					music, etc.
				</p>
				<p>
					<b>Tip:</b> Only submit completed work, rather than work-in-progress
					builds.
				</p>
				<p>
					<b>Tip:</b> Once the upload is complete, you do not need to keep
					the upload page open. It will continue to process on the server.
				</p>
			</div>

			<div id="log">
				<h2>Processing Status</h2>
				<p>
					<b>Tip:</b> Once the upload is complete, you may upload another
					file or close this page. Current submissions will continue to
					process on the server.
				</p>
				<div id="log-lines"></div>
			</div>
		</div>
	</div>

</@content>

<script type="application/javascript">
	let url = "../incoming/";
	let maxUploadSizeGigabytes = 1;

	document.addEventListener("DOMContentLoaded", function() {

		let selectFilesButton = document.querySelector('#select-files');
		let fileSelector = document.querySelector('#files');
		let uploadFilesButton = document.querySelector('#upload-files');
		let uploadTypeOption = document.querySelector('#upload-type');
		let uploadUrlButton = document.querySelector('#upload-url');
		let abortButton = document.querySelector('#abort');

		let filesList = document.querySelector('#files-list');
	  let logView = document.querySelector('#log');
	  let logLines = document.querySelector('#log-lines');
		let infoBlurb = document.querySelector('#words');

		let uploadControls = document.querySelector('#upload-controls');
		let progressControls = document.querySelector('#progress-controls');
		let progressBar = document.querySelector('#progress');

		// let uploadFiles = document.querySelector('#upload');
		// let uploadUrl = document.querySelector('#url');

		let currentRequest = null;

		selectFilesButton.addEventListener('click', () => {
			fileSelector.click();
		});

		uploadFilesButton.addEventListener('click', () => {
			upload();
		});

		uploadUrlButton.addEventListener('click', () => {
			alert("lol");
		});

		abortButton.addEventListener('click', () => {
			toggleProgress(false);
			// reset force type selector
			uploadTypeOption.value = "auto";

			if (currentRequest) {
				currentRequest.abort();
				currentRequest = null;
			}
		});

	  uploadTypeOption.addEventListener('change', () => {
			if (uploadTypeOption.value !== "auto") {
				if (!confirm("Caution!\n\n" +
						"Only use a forced type if auto-detection doesn't identify the uploaded content first!\n\n" +
						"The forced type will be applied to ALL selected files.\n\n" +
						"Continue?")) {
					uploadTypeOption.value = "auto";
				}
			}
		});

	  fileSelector.addEventListener('change', e => {
		  let totalSize = 0;
		  resetFilesList();

		  for (let i = 0; i < e.target.files.length; i++) {
			  let f = e.target.files[i];
			  let name = document.createElement("div");
			  name.innerText = f.name;
			  name.classList.add("name");
			  let size = document.createElement("div");
			  size.innerText = (f.size / 1024 / 1024).toFixed(1) + " mb";
			  size.classList.add("size");
			  let row = document.createElement("div");
			  row.classList.add("file");
			  row.append(name, size);
			  filesList.append(row);
			  totalSize += f.size;
		  }

		  if (!filesList.classList.contains("display-block")) filesList.classList.add("display-block");

		  if (totalSize >= (maxUploadSizeGigabytes * 1024 * 1024 * 1024)) {
			  alert("Caution!\n\n" +
			        "The total max size per upload is " + maxUploadSizeGigabytes + " GB. Reduce the total size of the upload or it may fail.");
		  }
	  });

		if (location.hash) {
			toggleProgress(true);
			abortButton.innerText = "Upload Another";
			pollJob(location.hash.substring(1), true);
		}

		function resetFilesList() {
			while (filesList.childNodes.length > 0) filesList.removeChild(filesList.childNodes[0]);
			let header = document.createElement("h2");
			header.innerText = "Selected Files";
			filesList.append(header);
		}

		function resetLog() {
			while (logLines.childNodes.length > 0) logLines.removeChild(logLines.childNodes[0]);
		}

		function upload() {
			currentRequest = new XMLHttpRequest();
			let data = new FormData();

			let files = fileSelector.files;
			console.log("uploading", files);
			for (let i = 0; i < files.length; i++) {
				data.append('files', files[i]);
			}

			if (uploadTypeOption.value !== "auto") {
		  	data.append('forceType', uploadTypeOption.value);
			}

			currentRequest.addEventListener('load', e => {
				abortButton.innerText = "Upload Another";

				history.pushState(null, document.title, '#' + e.target.response.toString());
				pollJob(e.target.response)
			});

			currentRequest.upload.addEventListener('progress', e => {
		  	progressBar.value = Math.round((e.loaded / e.total) * 100);
			});

			currentRequest.responseType = 'json';

			currentRequest.onerror = function(e) {
				alert("Connection error during upload. Please try again.");
				abortButton.innerText = "Retry upload";
			};

			// Send POST request to the server side script
			currentRequest.open('post', url + 'upload');

			toggleProgress(true);
			abortButton.innerText = "Cancel Upload";

			currentRequest.send(data);
		}

		function toggleProgress(progressing) {
			if (progressing) {
				progressBar.value = 0;
				if (!logView.classList.contains("display-block")) logView.classList.add("display-block");
				if (!progressControls.classList.contains("display-block")) progressControls.classList.add("display-block");
				if (uploadControls.classList.contains("display-block")) uploadControls.classList.remove("display-block");
				if (infoBlurb.classList.contains("display-block")) infoBlurb.classList.remove("display-block");
				resetLog();
			} else {
				if (logView.classList.contains("display-block")) logView.classList.remove("display-block");
				if (progressControls.classList.contains("display-block")) progressControls.classList.remove("display-block");
				if (!uploadControls.classList.contains("display-block")) uploadControls.classList.add("display-block");
				if (filesList.classList.contains("display-block")) filesList.classList.remove("display-block");
				if (!infoBlurb.classList.contains("display-block")) infoBlurb.classList.add("display-block");
				history.pushState(null, document.title, '#');
			}
		}

		function pollJob(jobId, catchup = false) {
			if (jobId !== location.hash.substring(1)) return; // stop polling if we're not looking at this job

			fetch(url + 'job/' + jobId.toString() + (catchup ? '?catchup=1' : ''))
				.then(result => {
					if (result.ok) {
						result.json().then(json => {
							json.forEach(l => pushLog(l));
						});
						pollJob(jobId);
					} else {
						pushLog(result.statusText);
					}
				})
		}

		function pushLog(event) {
			let time = document.createElement("div");
			time.classList.add('time');
			let log = document.createElement("div");
			log.classList.add('message');

			let logRow = document.createElement("div");
			logRow.classList.add('log');

			let dateStamp;

			if (event instanceof String) {
				dateStamp = new Date();
				log.innerText = event;
				logRow.classList.add("error")
			} else {
				dateStamp = new Date(event.time);
				log.innerHTML = linkify(event.message);
				logRow.classList.add(event.type.toLowerCase())
			}

			time.innerText = '[' + dateStamp.toLocaleTimeString() + ']';

			logRow.append(time, log);

			if (!logView.classList.contains("display-block")) logView.classList.add("display-block");
			logLines.append(logRow);
		}

		function linkify(message) {
			let linkPattern = /(\b(https?|ftp):\/\/[-A-Z0-9+&@#\/%?=~_|!:,.;]*[-A-Z0-9+&@#\/%=~_|])/gim;
			return message.replace(linkPattern, '<a href="$1" target="_blank">$1</a>');
		}
	});

</script>

<#include "../_footer.ftl">
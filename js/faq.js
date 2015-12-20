$('.faq-toc a').click(highlightEvent);

function highlightEvent(event) {
	highlight(event.target.dataset.target);
}

function highlight(id) {
	var element = $('#' + id + " + dd");
	element.addClass('highlight');
	setTimeout(function() {
		element.removeClass('highlight');
	}, 500);
}

$(document).ready(function() {
	var hash = document.location.hash;
	if (hash && hash.substr)
		highlight(hash.substr(1));
});

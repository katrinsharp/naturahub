jQuery(document).ready(function($){
	// on submit...
	$("#recipeForm #submit").click(function(e) {
		e.preventDefault();
		alert($("#recipeForm"));
		$("#recipeForm").submit();
		return false;
	});
});


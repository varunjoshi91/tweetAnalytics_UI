$(document).on("click","#searchButtonClick,.filterSection a",function(e){//
e.preventDefault();
	//debugger;
	filterParam='';
	if($(this).attr('class') == "list-group-item "){
		$('#filterClicked').val($(this).find('input').val());
		if($(this).closest('.panel-body').hasClass('concept_filter')){
			filterParam='concept_tag';
		}
		else if($(this).closest('.panel-body').hasClass('hashtag_filter')){
			filterParam='tweet_hashtags';
		}
		else if($(this).closest('.panel-body').hasClass('concept_filter')){
			filterParam='lang';
		}
		filterParam=filterParam+':'+$(this).find('input').val();
		$("#filter").val(filterParam);
		$(this).find('input').attr('checked','checked')
	}
	
	$('.imageSection img').animate({height: '0px'}, 1000,function(){
		$(".searchContainer").animate({paddingTop: '0px'}, 1000);
		$(".searchContainer").animate({paddingBottom: '0px'}, 1000);
	});
	
	//$('.searchResults').html('');
	if($("#searchQueryText").val().length < 3){
		$("#searchQueryText").parent().addClass('has-error');
		return false;
	}
	else{
		
		$.ajax({
            type: "POST",
            url: $(document).find('form').attr('action'),
            data:$(document).find('form').serialize(),
            dataType: "json",
           
            //if received a response from the server
            
            success: function( data, textStatus, jqXHR) {
            	 //console.log("data response "+data);
                //our country code was correct so we have some information to display
                 
                //debugger;
                if(data.response.docs.length < 1){
                	$('.resultList').html('');
                	$('.resultList').append('<li class="lead norel">No results found</li>');
                	 $('.summarySection .panel-body').html('').addClass('hide');
                	}
                 else{
                	 $('.resultList').html('');
                	 $('ul.pagination').remove();
                	 $('.alert-success, .alert-warning, .alert-danger').addClass('hide');
                        for(var i=0;i<data.response.docs.length;i++){
                        	
                        	//var tweetTagContainer = '<div class="tweetTrailer"></div>';
                        	var tweetTagContainer = document.createElement( 'div' );
                        	tweetTagContainer.className='tweetTrailer';
                        	
	                        for(var j=0;j<data.response.docs[i].tweet_hashtags.length;j++){
	                        	   anchor = $("<a href='#'><span class='label label-danger'>#" + data.response.docs[i].tweet_hashtags[j] + "</span></a>");
	                        	   tweetTagContainer.appendChild(anchor[0]);
	                        	    
	                        	//$(tweetTagContainer).append('<a href="" >'+data.response.docs[j].tweet_hashtags[j]+'</a>');
	                        }
                        	
                        	$('.resultList').append('<li class="lead" id='+data.response.docs[i].id+'><div class="tweetHeader"><div class="userImageIcon"><img src='+data.response.docs[i].profile_image_url_https[0]+' alt="userImage" /></div>'+
                        		'<div class="userName">'+data.response.docs[i].name[0]+ '</div><div class="userDate pull-right">'+parseTwitterDate(data.response.docs[i].created_at)+'</div></div>'+
                        		'<div class="tweetBody"><span>'+data.response.docs[i].text[0]+'</span></div>'+
                        		'</li>');
                        	$("#"+data.response.docs[i].id).append(tweetTagContainer);
                        	////debugger;

                        }
                        $('.polScore').closest('h2').removeClass('hide');
                        if(data.query_polarity[0] > 1){
                        	$('.positiveProgress').find('.alert-success').removeClass('hide').parent().find('.polScore').text(Math.round(data.query_polarity[0]*100)+" %").parent().removeClass('hide');
                        }
                        else if(data.query_polarity[0] == 0){
                        	$('.positiveProgress').find('.alert-warning').removeClass('hide').parent().find('.polScore').text(data.query_polarity[0]);
                        }
                        else{
                        	$('.positiveProgress').find('.alert-danger').removeClass('hide').parent().find('.polScore').text(Math.round(data.query_polarity[0]*100)+" %").parent().removeClass('hide');;
                        }
                        
                        //keysSorted = Object.keys(data.query_facets).sort(function(a,b){return list[a]-list[b]});
                        
                        var arr = sortObject(data.query_facets_concepts[0]);
                        console.log(data.query_facets_concepts[0]);
                        //debugger;
                        
                        //data.query_facets_concepts[0].sort(compare);
                        
                        var sort_array_concepts = [];
                        var sort_array_lang = [];
                        var sort_array_hashtags = [];
                        $('.filterSection').removeClass('hide');
                        if(data.query_facets_concepts.length > 0){
                        	$('.filterSection').find('.concept_filter').find('.list-group').html('');
                        		for(var i in data.query_facets_concepts[0]){
                        			sort_array_concepts.push({key:i,value:data.query_facets_concepts[0][i]});
                        		}
                        		sort_array_concepts.sort(function(x,y){return y.value - x.value});
                        		if(sort_array_concepts.length > 5){
	                        		for(var i=0;i<6;i++){
	                        			$('.concept_filter').find('.list-group').append('<a href="" class="list-group-item "><input type="checkbox" class="hide" value="'+sort_array_concepts[i].key+' " />' +sort_array_concepts[i].key+ ' ('+sort_array_concepts[i].value+')</a>');
	                                }
                        		}
                        		else{
                        			for(var i=0;i<sort_array_concepts.length;i++){
	                        			$('.concept_filter').find('.list-group').append('<a href="" class="list-group-item "><input type="checkbox" class="hide" value="'+sort_array_concepts[i].key+' " />' +sort_array_concepts[i].key+ ' ('+sort_array_concepts[i].value+')</a>');
	                                }
                        		}
                        		
                        }
                        //debugger;
                        if(data.query_facets_tweetHashtags.length > 0){
                        	$('.filterSection').find('.hashtag_filter').find('.list-group').html('');
                        		for(var i in data.query_facets_tweetHashtags[0]){
                        			sort_array_hashtags.push({key:i,value:data.query_facets_tweetHashtags[0][i]});
                        		}
                        		sort_array_hashtags.sort(function(x,y){return y.value - x.value});
                        		if(sort_array_hashtags.length > 5){
	                        		for(var i=0;i<6;i++){
	                        			$('.hashtag_filter').find('.list-group').append('<a href="" class="list-group-item "><input type="checkbox" class="hide" value="'+sort_array_hashtags[i].key+' " />' +sort_array_hashtags[i].key+ ' ('+sort_array_hashtags[i].value+')</a>');
	                                }
                        		}
                        		else{
                        			for(var i=0;i<sort_array_hashtags.length;i++){
	                        			$('.hashtag_filter').find('.list-group').append('<a href="" class="list-group-item "><input type="checkbox" class="hide" value="'+sort_array_hashtags[i].key+' " />' +sort_array_hashtags[i].key+ ' ('+sort_array_hashtags[i].value+')</a>');
	                                }
                        		}
                        		
                        }
                        if(data.query_facets_lang.length > 0){
                        	$('.filterSection').find('.lang_filter').find('.list-group').html('');
                        		for(var i in data.query_facets_lang[0]){
                        			sort_array_lang.push({key:i,value:data.query_facets_lang[0][i]});
                        		}
                        		sort_array_lang.sort(function(x,y){return y.value - x.value});
                        			for(var i=0;i<sort_array_lang.length;i++){
	                        			$('.lang_filter').find('.list-group').append('<a href="" class="list-group-item "><input type="checkbox" class="hide" value="'+sort_array_lang[i].key+' " />' +sort_array_lang[i].key+ ' ('+sort_array_lang[i].value+')</a>');
	                            
                        		}
                        }
                        
                       ////debugger;
                        
                       $('.summarySection .panel-body').html('').removeClass('hide');
                       $('.summarySection .panel-title').html(data.query_summary_title[0]);
                       $('.summarySection .panel-body').html(data.query_summary[0].replace(/\?/g,''));
                       $('.summarySection .panel-body').readmore();
                        
                     }
            	 
            	 ////debugger;
            	 
            },
           
            //If there was no resonse from the server
            error: function(jqXHR, textStatus, errorThrown){
                 console.log("Something really bad happened " + textStatus+" "+errorThrown);
            },
           
            //capture the request before it was sent to server
            beforeSend: function(jqXHR, settings){
//                //adding some Dummy data to the request
//                settings.data += "&dummyData=whatever";
//                //disable the button until we get the response
//                $('#myButton').attr("disabled", true);
            },
           
            //this is called after the response or error functions are finsihed
            //so that we can take some action
            complete: function(jqXHR, textStatus){
            	if($("ul.resultList").find('.norel').length == 0)
            	$("ul.resultList").jPaginate();
            	var nameFilter = $('#filterClicked').val();
            	$(".pulse-loader").addClass("hide");
            	//console.log(nameF);
                //enable the button 
            	//console.log('finished');
            }
 
        });  
		
	}
	
});

//$(document).on("click",'.filterSection a',function(){
//	//filter click
//	//debugger;
//	var currentUrl = window.location.href;
//    //var parsedUrl = $.url(currentUrl);
//    
//    //window.location.search = jQuery.query.set("concept", $(this).find('input').val());
//	//window.location.replace(updateQueryStringParameter(currentUrl,"Concept","2"));
//	window.location.href = window.location.href + '#Concept=3';
//	
//});

function compare(a,b) {
	  if (a.value < b.value)
	    return -1;
	  if (a.value > b.value)
	    return 1;
	  return 0;
	}

function sortObject(obj) {
    var arr = [];
    var prop;
    for (prop in obj) {
        if (obj.hasOwnProperty(prop)) {
            arr.push({
                'key': prop,
                'value': obj[prop]
            });
        }
    }
    arr.sort(function(a, b) {
        return a.value - b.value;
    });
    return arr; // returns array
}

function parseTwitterDate(tdate) {
    var system_date = new Date(Date.parse(tdate));
    var user_date = new Date();
    if (K.ie) {
        system_date = Date.parse(tdate.replace(/( \+)/, ' UTC$1'))
    }
    var diff = Math.floor((user_date - system_date) / 1000);
    if (diff <= 1) {return "just now";}
    if (diff < 20) {return diff + " seconds ago";}
    if (diff < 40) {return "half a minute ago";}
    if (diff < 60) {return "less than a minute ago";}
    if (diff <= 90) {return "one minute ago";}
    if (diff <= 3540) {return Math.round(diff / 60) + " minutes ago";}
    if (diff <= 5400) {return "1 hour ago";}
    if (diff <= 86400) {return Math.round(diff / 3600) + " hours ago";}
    if (diff <= 129600) {return "1 day ago";}
    if (diff < 604800) {return Math.round(diff / 86400) + " days ago";}
    if (diff <= 777600) {return "1 week ago";}
    //debugger;
    return "on " + system_date.toDateString();
}
var K = function () {
    var a = navigator.userAgent;
    return {
        ie: a.match(/MSIE\s([^;]*)/)
    }
}();
//function updateQueryStringParameter(uri, key, value) {
//	  var re = new RegExp("([?&])" + key + "=.*?(&|$)", "i");
//	  var separator = uri.indexOf('?') !== -1 ? "&" : "?";
//	  if (uri.match(re)) {
//	    return uri.replace(re, '$1' + key + "=" + value + '$2');
//	  }
//	  else {
//	    return uri + separator + key + "=" + value;
//	  }
//	}
$(document).ready(function(){  
	$(document).ajaxStart(function(){$(".pulse-loader").removeClass("hide");});
    //$("ul.resultList").jPaginate();          
    //console.log("here");
}); 
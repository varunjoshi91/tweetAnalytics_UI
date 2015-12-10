$("#searchButtonClick").on("click",function(){

	//$('.searchResults').html('');
	if($("#searchQueryText").val().length < 3){
		$("#searchQueryText").parent().addClass('has-error');
		return false;
	}
	else{
		
		$.ajax({
            type: "POST",
            url: $(this).closest('form').attr('action'),
            data:$(this).closest('form').serialize(),
            dataType: "json",
           
            //if received a response from the server
            success: function( data, textStatus, jqXHR) {
            	 console.log("data response "+data);
                //our country code was correct so we have some information to display
                 
                debugger;
                if(data.response.docs.length < 1)
                    $('.resultList').append('<li class="lead">No results found</li>');
                 else{
                	 $('.resultList').html();
                        for(var i=0;i<data.response.docs.length;i++){
                        $('.resultList').append('<li class="lead">' +data.response.docs[i].text_en+ '</li>');
                        }
                        if(data.query_polarity[0] > 1){
                        	$('.positiveProgress').find('.alert-success').removeClass('hide').parent().find('.polScore').text(Math.round(data.query_polarity[0]*100)+" %").parent().removeClass('hide');
                        }
                        else if(data.query_polarity[0] == 0){
                        	$('.positiveProgress').find('.alert-warning').removeClass('hide').parent().find('.polScore').text(data.query_polarity[0]);
                        }
                        else{
                        	$('.positiveProgress').find('.alert-danger').removeClass('hide').parent().find('.polScore').text(Math.round(data.query_polarity[0]*100)+" %").parent().removeClass('hide');;
                        }
                        
                        if(data.query_facets.length > 0){
                        	$('.filterSection').find('.list-group').html();
                        		for(var i in data.query_facets[0]){
                        		  console.log(i); // alerts key
                        		  console.log(data.query_facets[0][i]); //alerts key's value
                        		  //debugger;
                        		  $('.filterSection').find('.list-group').append('<a href="#" class="list-group-item ">' +i+ ' ('+data.query_facets[0][i]+')</a>');
                        		}
                        	
//                        		for(var i=0;i<data.query_facets.length;i++){
//                        			$('.filterSection').find('.list-group').append('<a href="#">' +data.response.docs[i].text_en+ '</a>');
//                                }
                        }
                        
                     }
            	 
            	 //debugger;
            	 
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
                //enable the button 
            	//console.log('finished');
            }
 
        });  
		
	}
	
});
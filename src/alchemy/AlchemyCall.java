package alchemy;
import com.getAllTweets.com.*;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;

import com.getAllTweets.com.RunBasicQueriesOnSolr;

import twitter4j.JSONArray;

/**
 * Servlet implementation class AlchemyCall
 */
public class AlchemyCall extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public AlchemyCall() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.getWriter().append("Served at: ").append(request.getContextPath());
		PrintWriter out = response.getWriter();
		out.println("Varun "+request.getParameter("searchString"));
		
		
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		//doGet(request, response);
		
		String query = request.getParameter("searchQueryText");
		//System.out.println(query);
		PrintWriter out = response.getWriter();
		//out.println("Joshi");
		
		JSONArray arrayJSON = new JSONArray();
		
		twitter4j.JSONObject obj=new twitter4j.JSONObject();

		
		  
		  
		 RunBasicQueriesOnSolr runBasic = new RunBasicQueriesOnSolr(); // this is not required
	      
		  if(query.length() > 1){
			  obj = runBasic.runSingleQuery(query);

			  
		  }
		  //out.println(obj);
		  
		response.setContentType("application/json");
		// Get the printwriter object from response to write the required json object to the output stream      
		// Assuming your json object is **jsonObject**, perform the following, it will return your json object  
		out.print(obj);
		out.flush();
		//out.close();
		
		
	}
	

}

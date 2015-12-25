package com.getAllTweets.com;

        import java.io.File;
        import java.util.*;
        import java.io.FileOutputStream;
        import java.io.FileInputStream;
        import java.io.BufferedReader;
        import java.io.IOException;
        import java.io.OutputStream;
        import java.io.OutputStreamWriter;
        import java.io.Writer;
        import java.io.UnsupportedEncodingException;
        import java.util.ArrayList;
        import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/* For language detection */
        import java.io.InputStreamReader;
        import java.net.URLEncoder;
        import java.net.URL;
        import java.net.HttpURLConnection;
        import java.io.DataInputStream;



/* For jSON parsing*/
        import twitter4j.JSONArray;
        import twitter4j.JSONException;
        import twitter4j.JSONObject;

/*
* The program will run the queries as given in queries.txt
* It will get the result  from Koding VM , and generate a TREC-eval compatible output result file
* The TREC-eval will evaluate the found result and compare the same to qrel.txt
* qrel.txt contains manual relevance judgement
* We must try to maximize the score found by TREC-eval on different measures such as F0.5, nDCG etc.
* Part1 : get results from KODING VM
*/
public class RunBasicQueriesOnSolr {
    static ArrayList<String> queryNumber = new ArrayList<>();
    private String queryFileName;
    private String detectLanguageURL = "http://ws.detectlanguage.com/0.2/detect";
    private String twitterCoreURL    = "http://mihirdha.koding.io:8983/solr/projectB/select"; //Enter your URL here
    private String charset;
    private final String API_KEY 	 = "105395efdda297b8a6e2d5e325a245e5"; // get your own API key from the detectlanguage.com ( if you wish so)
    private final String USER_AGENT  = "Mozilla/5.0";
    private String trecLogFile       = "!!!!!!!!!!!final_default_01.txt";
    private String workingDir;
    private HashMap<String, Integer> maxIndexSizeData;
    /**
     *
     * @param queryFileName
     */
    public RunBasicQueriesOnSolr(){
    	System.out.println("constructor");
    }
    public RunBasicQueriesOnSolr(String queryFileName) {
        this.queryFileName = queryFileName;
        charset = java.nio.charset.StandardCharsets.UTF_8.name();
        workingDir = new File("").getAbsolutePath()+"/";
        maxIndexSizeData = new HashMap<>();
    }
    /**
     *
     */
    public ArrayList<String> readQueriesFromFile() {
        ArrayList<String> queries = new ArrayList<>();

        String aLine;
        try {
            FileInputStream fstream1 = new FileInputStream(queryFileName);
            DataInputStream in = new DataInputStream(fstream1);
            BufferedReader br   = new BufferedReader(new InputStreamReader(in, "UTF8"));
            while ( (aLine = br.readLine())  != null) {
                //System.out.println("----- "+aLine.substring(0,3));
                queries.add(aLine.substring(9));
                queryNumber.add(aLine.substring(0, 3));
            }
            br.close();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return queries;
    }
    /**
     *
     * @param jSONString
     * @return
     * @throws JSONException
     */
    public String parseJSONStringAndFindLanguage(String jSONString) throws JSONException {
        JSONObject jObj;
        String language = "";
        jObj = new JSONObject(jSONString);
        language = jObj.getJSONObject("data").getJSONArray("detections").getJSONObject(0).get("language").toString();
        return language;

    }
    /**
     *
     * @param URL
     * @param query
     * @return
     * @throws IOException
     * @throws JSONException 
     */
    public JSONObject fetchHTTPData(String URL, String query) throws IOException, JSONException {
        String response = "";
        int responseCode = 0;
        JSONObject arrayJSON= new JSONObject();
        HttpURLConnection httpConn = (HttpURLConnection) new URL(URL + "?" + query).openConnection();
        httpConn.setDoOutput(true); // Triggers POST.
        httpConn.setRequestProperty("Accept-Charset", charset);
        httpConn.setRequestProperty("User-Agent", USER_AGENT);
        responseCode = httpConn.getResponseCode();
        
        if ( responseCode == 200) { //OK
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(httpConn.getInputStream()));
            String inputLine;
            StringBuffer responseBuffer = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                responseBuffer.append(inputLine);
            }
            in.close();
            response = responseBuffer.toString();
            arrayJSON = new JSONObject(response);
        }

        return arrayJSON;
    }
    /**
     * @param :QueryText
     * The param is tested by firing a HTTP post request to http://ws.detectlanguage.com/0.2/detect
     * The output result is in JSON Format which is parsed to extract language field
     */
    public String getLanguageOfQuery(String queryText) {
        String lang = "";
        queryText = queryText.replace(" ","+");
        String response;
        try {
            String query = String.format("q=%s&key=%s",
                    URLEncoder.encode(queryText, charset),
                    URLEncoder.encode(API_KEY, charset));

            response = fetchHTTPData(detectLanguageURL, query).toString();
            if (!response.equals(""))
                lang     = parseJSONStringAndFindLanguage(response);

            else
                System.out.println("No response from Language detection server...");
        }
        catch(Exception ex) {
            System.out.println("Exception occured while detecting language...");
            ex.printStackTrace();
        }
        return lang;
    }
    /**
     *
     * @param responseFromSolr
     * @return
     * @throws JSONException
     * @throws NumberFormatException
     */
    public String parseJSONResponseFromSolr(String queryNumber, String responseFromSolr, String modelName, boolean bIsOnlyNumFoundRequired)
            throws NumberFormatException, JSONException {

        String numFound;
        StringBuilder trec_Response = new StringBuilder();
        JSONObject jObjTemp;
        if (responseFromSolr.equals(""))
        {
            //	System.out.println("queryNumber :" + queryNumber);
            //System.out.println("-------------------------------------");
            return trec_Response.toString();
        }
        JSONObject jObj = new JSONObject(responseFromSolr).getJSONObject("response");
        numFound = jObj.get("numFound").toString();
        if (bIsOnlyNumFoundRequired)
            return numFound;
        JSONArray jObjArray = new JSONArray(jObj.getString("docs").toString());
        for ( int i = 0 ; i < jObjArray.length(); i++) {
            jObjTemp = jObjArray.getJSONObject(i);
            //System.out.println(RunBasicQueriesOnSolr.queryNumber.get(i));
            trec_Response = trec_Response.append(queryNumber + " Q0 " +
                    jObjTemp.getString("id") + " " +
                    String.valueOf(i) + " " +
                    jObjTemp.getString("score") + " " + modelName + System.lineSeparator());

        }
        //System.out.println("***********");
        return trec_Response.toString();
    }
    /**
     *
     * @param trecSupportedOutput
     */
    public void WriteDataInTRECFormat(String trecSupportedOutput ) {
        try {
            File file = new File( workingDir + trecLogFile);
            if (!file.exists()) {
                file.createNewFile();
            }
            OutputStream outputStream       = new FileOutputStream(file);
            Writer       outputStreamWriter = new OutputStreamWriter(outputStream);


            outputStreamWriter.write(trecSupportedOutput);

            outputStreamWriter.close();
            System.out.println("File created !!!! varun joshi");
            System.out.println("Created file is =" + new File(workingDir + trecLogFile).getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    /**
     * See: {@link org.apache.lucene.queryparser.classic queryparser syntax}
     * for more information on Escaping Special Characters
     */
    public String escapeQueryChars(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            // These characters are part of the query syntax and must be escaped
            if (c == '\\' || c == '+' || c == '-' || c == '!'  || c == '(' || c == ')' || c == ':'
                    || c == '^' || c == '[' || c == ']' || c == '\"' || c == '{' || c == '}' || c == '~'
                    || c == '*' || c == '?' || c == '|' || c == '&'  || c == ';' || c == '/'
                    || Character.isWhitespace(c)) {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }
    /**
     * @throws IOException
     *
     */
    public boolean getMaximumIndexSizeBasedOnLanguage() {
        String response, query = "";
        String lang[] = {"en", "de", "ru" };
        for  (int i = 0 ; i < lang.length; i++)
        {
            try {
                query = String.format("q=*:*&fq=lang:%s&start=0&rows=0&fl=numFound&&wt=json&indent=true",URLEncoder.encode(lang[i],charset));
                response = fetchHTTPData(twitterCoreURL , query).toString();
                maxIndexSizeData.put(lang[i],new Integer(Integer.parseInt(parseJSONResponseFromSolr(queryNumber.get(i), response, "default", true)))); //varun
            } catch (IOException | NumberFormatException | JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
        if (maxIndexSizeData.size() != 3) {
            return false;
        }
        else
            return true;
    }
    /**
     * reads the input query file and gets the data from Solr
     * Main function to call when fetching data
     * Returns error codes in case of failure
     * 0 : success
     * -1: No data present in query file
     * -2: Unsupported Encoding
     * -3: IO Exception
     */
    public int runQueriesOnSolr() {
        ArrayList<String> inputQueries = readQueriesFromFile();
        String lang, query, response;
        StringBuilder sTrecSupportedResponse;
        if ( inputQueries.size() == 0) {
            return -1; //error code -1: no input queries
        }
        int numOfRows = 0;
        if (!getMaximumIndexSizeBasedOnLanguage())
        {
            return -10; // Cannot query Solr
        }
        sTrecSupportedResponse = new StringBuilder();
        for ( int i = 0 ; i < inputQueries.size(); i++) {
            //System.out.println("+++++ "+inputQueries.get(i));
            //lang  = getLanguageOfQuery(inputQueries.get(i));
            lang  = inputQueries.get(i).substring(0,2);
            query=inputQueries.get(i).substring(3);
            //System.out.println(lang+"  "+query);
            try {
                query = escapeQueryChars(inputQueries.get(i).substring(3));
                //System.out.println("lang :" + lang);
                try {
                    numOfRows = maxIndexSizeData.get(lang);
                }
                catch(Exception ex) {
                    numOfRows = 1000;
                }
                /*query = String.format("q=text_%s:%s&fq=lang:%s&wt=json&start=0&rows=%s&indent=true&fl=id,score",
                        URLEncoder.encode(lang, charset), URLEncoder.encode(query, charset), URLEncoder.encode(lang, charset),
                        URLEncoder.encode(String.valueOf(numOfRows), charset));*/


                //dismax
                query = String.format("q=%s&fq=lang:%s&wt=json&start=0&rows=%s&indent=true&defType=edismax&fl=text_en&qf=text_%s^5+tweet_hashtags^10&pf=text_%s^50+tweet_hashtags^20",
                        URLEncoder.encode(query, charset), URLEncoder.encode(lang, charset),
                        URLEncoder.encode(String.valueOf(numOfRows), charset), URLEncoder.encode(lang, charset), URLEncoder.encode(lang, charset));



                response = fetchHTTPData(twitterCoreURL, query).toString();
                System.out.println("query :" + query);
                Integer.parseInt(RunBasicQueriesOnSolr.queryNumber.get(i));
                sTrecSupportedResponse = sTrecSupportedResponse.append(parseJSONResponseFromSolr(String.format("%03d", Integer.parseInt(RunBasicQueriesOnSolr.queryNumber.get(i))), response, "default", false));
                //System.out.println("sTrecSupportedResponse: "+sTrecSupportedResponse.toString());
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return -2;
            } catch (IOException e) {
                e.printStackTrace();
                return -3;
            } catch (NumberFormatException e) {
                e.printStackTrace();
                return -4;
            } catch (JSONException e) {
                e.printStackTrace();
                return -5;
            }
        }
        //WriteDataInTRECFormat(sTrecSupportedResponse.toString());

        return 0;
    }

    public JSONObject runSingleQuery(String queryReceived, String queryFilter){ //varun joshi run single query
    	System.out.println("recieved"+queryFilter);
    	System.out.println("query "+queryReceived);
    	JSONArray arrayJSON = new JSONArray();
    	JSONObject objJSON = new JSONObject();
    	charset = java.nio.charset.StandardCharsets.UTF_8.name();
    	String lang="en";
    	int numOfRows = 1000;
    	try{
    		/*String query = String.format("q=%s&fq=lang:%s&wt=json&start=0&rows=%s&indent=true&defType=edismax&fl=id,score&qf=text_%s^5+tweet_hashtags^10&pf=text_%s^50+tweet_hashtags^20",
                    URLEncoder.encode(queryReceived, charset), URLEncoder.encode(lang, charset),
                    URLEncoder.encode(String.valueOf(numOfRows), charset), URLEncoder.encode(lang, charset), URLEncoder.encode(lang, charset));
*/
    		String query;
    		
    		if(queryFilter == null || queryFilter.length() == 0){
    		 query = String.format("q=%s&fq=lang:%s&wt=json&start=0&rows=%s&indent=true&facet=true&defType=edismax&fl=id,text,name,tweet_hashtags,profile_image_url_https,created_at,concept_tag,relevance_tag,polarity,polarity_confidence,facet_counts&facet.field=concept_tag&facet.field=lang&facet.field=tweet_hashtags&qf=text_%s^5+tweet_hashtags^10&pf=text_%s^50+tweet_hashtags^20",
                    URLEncoder.encode(queryReceived, charset), URLEncoder.encode(lang, charset),
                    URLEncoder.encode(String.valueOf(numOfRows), charset), URLEncoder.encode(lang, charset), URLEncoder.encode(lang, charset));
    		}else{
    		 query = String.format("q=%s&fq=lang:%s&fq=%s&wt=json&start=0&rows=%s&indent=true&facet=true&defType=edismax&fl=id,text,name,tweet_hashtags,profile_image_url_https,created_at,concept_tag,relevance_tag,polarity,polarity_confidence,facet_counts&facet.field=concept_tag&facet.field=lang&facet.field=tweet_hashtags&qf=text_%s^5+tweet_hashtags^10&pf=text_%s^50+tweet_hashtags^20",
                    URLEncoder.encode(queryReceived, charset), URLEncoder.encode(lang, charset),URLEncoder.encode(queryFilter, charset),
                    URLEncoder.encode(String.valueOf(numOfRows), charset), URLEncoder.encode(lang, charset), URLEncoder.encode(lang, charset));
    		}
    		

            //String response = fetchHTTPData(twitterCoreURL, query);
    		objJSON = fetchHTTPData(twitterCoreURL, query);
    		
    		System.out.println(query);
    		
    		
    		Pattern p = Pattern.compile("\\[(.*?)\\]");
    		Matcher m = p.matcher(getSummary(queryReceived));

    		if(m.find()) {
    		    System.out.println("Got it here: "+m.group(1));
    		}
    		
    		//System.out.println("Mihir result: "+parseJSONandFindSentiment(objJSON.toString()));
    		
    		objJSON.append("query_polarity", parseJSONandFindSentiment(objJSON.toString()));
    		objJSON.append("query_facets_concepts", parseJSONandFindFacets(objJSON.toString()).getConcept());    		//parseJSONandFindFacets
    		objJSON.append("query_facets_lang", parseJSONandFindFacets(objJSON.toString()).getLang());    		//parseJSONandFindFacets
    		objJSON.append("query_facets_tweetHashtags", parseJSONandFindFacets(objJSON.toString()).getTweet());    		//queryReceived getSummary
    		objJSON.append("query_summary", getSummary(queryReceived).replace("["+m.group(1)+"]",""));
    		objJSON.append("query_summary_title", m.group(1));
    		
    		//System.out.println("query "+query);
          //  System.out.println("response "+objJSON);
    	}
    	catch(Exception e){
    		System.out.println(e);
    	}
    	
    	return objJSON;
    	
    	
    }
    
    public JSONObject filterClick(String queryReceived){ //varun joshi run single query
    	//System.out.println("here");
    	JSONArray arrayJSON = new JSONArray();
    	JSONObject objJSON = new JSONObject();
    	charset = java.nio.charset.StandardCharsets.UTF_8.name();
    	String lang="en";
    	int numOfRows = 1000;
    	try{
    		/*String query = String.format("q=%s&fq=lang:%s&wt=json&start=0&rows=%s&indent=true&defType=edismax&fl=id,score&qf=text_%s^5+tweet_hashtags^10&pf=text_%s^50+tweet_hashtags^20",
                    URLEncoder.encode(queryReceived, charset), URLEncoder.encode(lang, charset),
                    URLEncoder.encode(String.valueOf(numOfRows), charset), URLEncoder.encode(lang, charset), URLEncoder.encode(lang, charset));
*/

    		String query = String.format("q=%s&fq=lang:%s&wt=json&start=0&rows=%s&indent=true&facet=true&defType=edismax&fl=id,text,name,tweet_hashtags,profile_image_url_https,created_at,concept_tag,relevance_tag,polarity,polarity_confidence,facet_counts&facet.field=concept_tag&facet.field=lang&facet.field=tweet_hashtags&qf=text_%s^5+tweet_hashtags^10&pf=text_%s^50+tweet_hashtags^20",
                    URLEncoder.encode(queryReceived, charset), URLEncoder.encode(lang, charset),
                    URLEncoder.encode(String.valueOf(numOfRows), charset), URLEncoder.encode(lang, charset), URLEncoder.encode(lang, charset));

    		

            //String response = fetchHTTPData(twitterCoreURL, query);
    		objJSON = fetchHTTPData(twitterCoreURL, query);
    		
    		System.out.println(query);
    		
    		
    		Pattern p = Pattern.compile("\\[(.*?)\\]");
    		Matcher m = p.matcher(getSummary(queryReceived));

    		if(m.find()) {
    		    System.out.println("Got it here: "+m.group(1));
    		}
    		
    		//System.out.println("Mihir result: "+parseJSONandFindSentiment(objJSON.toString()));
    		
    		objJSON.append("query_polarity", parseJSONandFindSentiment(objJSON.toString()));
    		objJSON.append("query_facets_concepts", parseJSONandFindFacets(objJSON.toString()).getConcept());    		//parseJSONandFindFacets
    		objJSON.append("query_facets_lang", parseJSONandFindFacets(objJSON.toString()).getLang());    		//parseJSONandFindFacets
    		objJSON.append("query_facets_tweetHashtags", parseJSONandFindFacets(objJSON.toString()).getTweet());    		//queryReceived getSummary
    		objJSON.append("query_summary", getSummary(queryReceived).replace("["+m.group(1)+"]",""));
    		objJSON.append("query_summary_title", m.group(1));
    		
    		//System.out.println("query "+query);
          //  System.out.println("response "+objJSON);
    	}
    	catch(Exception e){
    		System.out.println(e);
    	}
    	
    	return objJSON;
    	
    	
    }
    
    
    /*Mihirs function*/
    
    public float parseJSONandFindSentiment(String responseFromSolr)
            throws NumberFormatException, JSONException {

        String numFound;
        StringBuilder trec_Response = new StringBuilder();
        JSONObject jObjTemp;
        if (responseFromSolr.equals(""))
        {
            return 0;
        }
        JSONObject jObj = new JSONObject(responseFromSolr).getJSONObject("response");
        numFound = jObj.get("numFound").toString();
        JSONArray jObjArray = new JSONArray(jObj.getString("docs").toString());


        float sum=0;
        Integer count=0;

        for ( int i = 0 ; i < jObjArray.length(); i++) {
            jObjTemp = jObjArray.getJSONObject(i);
            //System.out.println(RunBasicQueriesOnSolr.queryNumber.get(i));
            /*trec_Response = trec_Response.append(queryNumber + " Q0 " +
                    jObjTemp.getString("id") + " " +
                    String.valueOf(i) + " " +
                    jObjTemp.getString("score") + " " + modelName + System.lineSeparator());
*/


            if(jObjTemp.getString("polarity").equals("positive")) {
                sum = sum + Float.parseFloat(jObjTemp.getString("polarity_confidence"));
                //System.out.println(Float.parseFloat(jObjTemp.getString("polarity_confidence")));
                count++;
            }
            else if(jObjTemp.getString("polarity").equals("negative")) {
                sum = sum - Float.parseFloat(jObjTemp.getString("polarity_confidence"));
                //System.out.println(Float.parseFloat(jObjTemp.getString("polarity_confidence")));
                count++;
            }
            else if (jObjTemp.getString("polarity").equals("neutral")) {
                //System.out.println(Float.parseFloat(jObjTemp.getString("polarity_confidence")));
                count++;
            }

            ///System.out.println(jObj.toString());
        }
        //System.out.println("***********");
        if(count==0)
            return 0;
        return sum/count;
    }

    
//  Surbhi's function
    
    public MapDataStructure parseJSONandFindFacets(String responseFromSolr)
            throws JSONException {
    	Map< String, Integer> conceptMap= new HashMap<String , Integer>();
    	Map< String, Integer> lang= new HashMap<String , Integer>();
    	Map< String, Integer> hashtags= new HashMap<String , Integer>();
        //StringBuilder trec_Response = new StringBuilder();
        //JSONObject su = new JSONObject();
        MapDataStructure bleh = null;
        if (responseFromSolr.equals(""))
        {
            return bleh;
        }
        //JSONObject jObj = new JSONObject(responseFromSolr).getJSONObject("responseHeader");
        JSONObject jObjFacets = new JSONObject(responseFromSolr).getJSONObject("facet_counts");
        
        //System.out.println("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&"+jObjFacets.toString());
        
        //System.out.println(jObjFacets.get("facet_fields"));
        JSONObject jObjfFacets = jObjFacets.getJSONObject("facet_fields");
        JSONArray conceptTagArray= jObjfFacets.getJSONArray("concept_tag");
        JSONArray langTagArray= jObjfFacets.getJSONArray("lang");
        JSONArray tweetTagArray= jObjfFacets.getJSONArray("tweet_hashtags");
        
        
        for ( int i = 0 ; i < conceptTagArray.length(); i+=2)
        {
        	String field=conceptTagArray.getString(i);
        	int value=conceptTagArray.getInt(i+1);
        	if(value>=2){
        		conceptMap.put(field, value);
        	}
       
        }
        
        for ( int i = 0 ; i < langTagArray.length(); i+=2)
        {
        	String field=langTagArray.getString(i);
        	int value=langTagArray.getInt(i+1);
        	if(value>=2){
        		lang.put(field, value);
        	}
       
        }
        
        for ( int i = 0 ; i < tweetTagArray.length(); i+=2)
        {
        	String field=tweetTagArray.getString(i);
        	int value=tweetTagArray.getInt(i+1);
        	if(value>=2){
        		hashtags.put(field, value);
        	}
       
        }
        
        MapDataStructure obj = new MapDataStructure();
        obj.setConcept(conceptMap);
        obj.setLang(lang);
        obj.setTweet(hashtags);
        
       //System.out.println("Here lies the result"); 
       //System.out.println(conceptMap); 
       
       TreeMap<String, Integer> treeMap = new TreeMap<String,Integer>();
       treeMap.putAll(conceptMap);
        
       //System.out.println(sortByValue(conceptMap));
       
       JSONObject varunObj = new JSONObject(conceptMap);
       
       //return sortByValue(conceptMap);
        return obj;
       }
    
    /**
    *
    * @param responseFromSolr
    * @return
    * @throws JSONException
    * @throws NumberFormatException
    */
   public String getSummary(String query) throws IOException, JSONException {
       //System.out.println("HIII");
       String url = "https://en.wikipedia.org/w/api.php?action=query&list=search&format=json&prop=revisions&rvprop=content&rvsection=0&srsearch="+URLEncoder.encode(query, "UTF-8");

       URL obj = new URL(url);
       HttpURLConnection con = (HttpURLConnection) obj.openConnection();

       // optional default is GET
       con.setRequestMethod("GET");

       //add request header
       con.setRequestProperty("User-Agent", USER_AGENT);

       int responseCode = con.getResponseCode();
       //System.out.println("\nSending 'GET' request to URL : " + url);
       //System.out.println("Response Code : " + responseCode);
       //System.out.println(con.());
       BufferedReader in = new BufferedReader(
               new InputStreamReader(con.getInputStream()));
       String inputLine;
       StringBuffer response = new StringBuffer();

       while ((inputLine = in.readLine()) != null) {
           response.append(inputLine);
       }

       in.close();

       //print result

       //System.out.println("varun "+response.toString());
       JSONObject jsonObj;
       jsonObj = new JSONObject(response.toString());


       JSONObject n = (JSONObject) jsonObj.getJSONObject("query").getJSONArray("search").get(0);
       String title=n.getString("title").toString();
       System.out.println(n.getString("title").toString());// take title from here
       //System.out.println("varun "+jsonObj.toString());
       String summary="["+n.getString("title").toString()+"]";

       String url2 = "https://en.wikipedia.org/w/api.php?format=json&action=query&prop=extracts&exintro=&explaintext=&titles="+URLEncoder.encode(title, "UTF-8");

       URL obj2 = new URL(url2);
       HttpURLConnection con2 = (HttpURLConnection) obj2.openConnection();

       // optional default is GET
       con2.setRequestMethod("GET");

       //add request header
       con2.setRequestProperty("User-Agent", USER_AGENT);

       int responseCode2 = con2.getResponseCode();
       //System.out.println("\nSending 'GET' request to URL : " + url2);
       //System.out.println("Response Code : " + responseCode2);
       //System.out.println(con.());
       BufferedReader in2 = new BufferedReader(
               new InputStreamReader(con2.getInputStream()));
       String inputLine2;
       StringBuffer response2 = new StringBuffer();

       while ((inputLine2 = in2.readLine()) != null) {
           response2.append(inputLine2);
       }

       in2.close();

       //print result

       System.out.println(response2.toString());
       JSONObject jsonObj2;
       jsonObj2 = new JSONObject(response2.toString());


       JSONObject n2 = jsonObj2.getJSONObject("query").getJSONObject("pages");
       Iterator<String> k=jsonObj2.getJSONObject("query").getJSONObject("pages").keys();
       
       if (k.hasNext())
       {
           summary+=n2.getJSONObject(k.next()).getString("extract");
       }
       //String title2=n2.getString("title").toString();
       //System.out.println(n2.toString());
       System.out.println(summary);
       //System.out.println(jsonObj.toString());





       return summary;

   }




    
    
    public static Map<String, Integer> sortByValue(Map<String, Integer> map) {
        List list = new LinkedList(map.entrySet());
        Collections.sort(list, new Comparator() {

            @Override
            public int compare(Object o1, Object o2) {
                return ((Comparable) ((Map.Entry) (o2)).getValue()).compareTo(((Map.Entry) (o1)).getValue());
            }
        });

        Map result = new LinkedHashMap();
        for (Iterator it = list.iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }
    
    
    
}
/*
*
*/
class QueryRun {
    /**
     *
     * @param filePathString : path of file to check if it exists or not
     * @return : true, if file exists; else false
     */
    public static boolean IsFileExists(String filePathString) {
        File f = new File(filePathString);

        if(f.exists() && f.isFile()) {
            return true;
        }
        else
            return false;
    }
    public static void main(String []args) {
//        String queryFileName = "queries.txt";//  /Users/varunjoshi/IdeaProjects/getTweets/queries.txt
//        queryFileName = new File("").getAbsolutePath() +"/"+queryFileName;
//        System.out.println(queryFileName);
//        if (!QueryRun.IsFileExists(queryFileName) ) {
//            System.out.println("Please provide queries.txt file in the current directory ! File not found...");
//            return;
//        }
//        RunBasicQueriesOnSolr runBasic = new RunBasicQueriesOnSolr(queryFileName);
//        runBasic.runQueriesOnSolr();
    	//RunBasicQueriesOnSolr runBasic = new RunBasicQueriesOnSolr();
        //runBasic.runSingleQuery("Syria attack");
    }

}

class MapDataStructure{
	Map< String, Integer> concept;
	Map< String, Integer> lang;
	Map< String, Integer> tweet;
	public Map<String, Integer> getConcept() {
		return concept;
	}
	public void setConcept(Map<String, Integer> concept) {
		this.concept = concept;
	}
	public Map<String, Integer> getLang() {
		return lang;
	}
	public void setLang(Map<String, Integer> lang) {
		this.lang = lang;
	}
	public Map<String, Integer> getTweet() {
		return tweet;
	}
	public void setTweet(Map<String, Integer> tweet) {
		this.tweet = tweet;
	}
	
	
	
	
	
}
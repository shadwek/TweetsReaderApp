package local.treader.main;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("")
public class Utilities {
	
    // HTTP Get Method
    @GET
    @Path("/languages")
    @Produces(MediaType.APPLICATION_JSON) 
    public String getLanguages(){

    	// connect to "language" collection and return it as JSON string
        return Database.connect("languages");
    }
    
    @GET
    @Path("/times")
    @Produces(MediaType.APPLICATION_JSON) 
    public String getTimes(){
    	
    	// connect to "times" collection and return it as JSON string
    	return Database.connect("times");
    }
    
    @GET
    @Path("/topFol")
    @Produces(MediaType.APPLICATION_JSON) 
    public String getTopFollowers(){
    	
    	// connect to "num_of_followers" collection and return it as JSON string
    	return Database.connect("top_num_of_followers");
    }
    
    @GET
    @Path("/topRet")
    @Produces(MediaType.APPLICATION_JSON) 
    public String getTopRetweets(){
    	
    	// connect to "num_of_retweets" collection and return it as JSON string
    	return Database.connect("top_retweet_count");
    }
    
    @GET
    @Path("/topMen")
    @Produces(MediaType.APPLICATION_JSON) 
    public String getTopMentions(){
    	
    	// connect to "num_of_mentions" collection 
    	return Database.connect("top_num_of_mentions");
    }
//    http://localhost:8080/TweetsReader/do/readTweets?limit=122
    @GET
    @Path("/readTweets")
    @Produces(MediaType.APPLICATION_JSON) 
    public String readTweets(@QueryParam("limit") String limit){
    	
    	int l = Integer.parseInt(limit);
    	// read tweets from tweets collection 
    	Tweets.readTweets(l);
    	return Database.connect("tweetsData");
    }
}

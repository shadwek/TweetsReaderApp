package local.treader.main;

import com.mongodb.DBCollection;

import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.auth.AccessToken;

/**
 * 
 * Project: Tweets Reader
 * File: Tweets.java
 * Purpose: read tweets and write/read data to/from database
 * @author Hussain Hajjar : 2015/03/31
 * @version 1.0
 */
public final class Tweets {

	// GLOBAL_VARIABLE required for twitter application authentication
	final static String CONSUMER_KEY = "apKEbLzIestfMjphgIvTIeEPt";
	final static String CONSUMER_SECRET = "02hbrWFlrEYsorp1q5kQbW9oOahlSrQdziDXi6r4OYe7jkZip5";
	final static String ACCESS_TOKEN = "168260031-4DgRSG8eOpa1oXdaqPFkhwsuYYbpgB5Qcoi63kQg";
	final static String ACCESS_TOKEN_SECRET = "jcamQ8GcSt5XXVZZP0XsSoCPPCCwP7j08npgpGVcGnVPT"; 

	// COLLECTION is used to insert documents into 
	// after establishing a new connection to the database
//	private static 

	/**
	 * readTweets method connects to twitter, read tweets then insert
	 * them into MongoDb
	 * 
	 * @param limit: int number of tweets to process
	 * 
	 * @return String[]: tweet
	 * @throws InterruptedException 
	 */
    public static void readTweets(int limit){
    	
    	DBCollection COLLECTION = Database.newConnection();
    	
    	// TwitterStream is used to connect the application to Twitter Streaming API
	    TwitterStream twitterStream = new TwitterStreamFactory().getInstance();
	    
	    // AccessToken is required to make the connection
	    AccessToken accessToken = new AccessToken(ACCESS_TOKEN, ACCESS_TOKEN_SECRET);
	    
	    // all tokens, secret keys are taken from dev.twitter.com
	    twitterStream.setOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);
	    
	    // set Authentications to the connection
	    twitterStream.setOAuthAccessToken(accessToken);
	    // this is used to encode tweets into UTF-8 instead of ?s
	    
	    StatusListener listener = new StatusListener() {
		    
		    // to count tweets to listen to
	    	int counter = 0; 
	    	
			@Override
	        public void onStatus(Status status){
	    		if(status.getText() == null){
	    			counter++; // add 1 for the deleted status
	    		}
	    		
	        	COLLECTION.insert(Database.addDocument(status)); // insert status to database
	            // add tweet to tweets collection
	            //Database.addTweet(tweet);
	            counter++; // add 1 to counter to count tweets
	            // when tweets limit is reached
	        	if(counter == limit){
	        		Database.findTop5("retweet_count"); // find top tweets retweeted by users
	        		Database.findTop5("num_of_mentions"); // find top user mentioned users in the tweet
	        		Database.findTop5("num_of_followers"); // find top users with most followers
	        		Database.findTop5("language"); // find top languages users wrote tweets in
	        		Database.findTop5("minute"); // find top minutes users retweeted in
	        		twitterStream.shutdown();// shutdown twitter stream
	        		Database.closeConnection(); // close database connection
	        	}
	        }
	
	        @Override
	        public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
	        	// this method discards all deleted tweets
	        }
	
	        @Override
	        public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
	        	// this method is called when numberOfLimitedStatuses is reached
	        }
	
	        @Override
	        public void onScrubGeo(long userId, long upToStatusId) {
	        	// this method is called when a location is deleted
	        }
	
	        @Override
	        public void onStallWarning(StallWarning warning) {
	        	// this method is called when twitter replies with stall warning
	        }
	
	        @Override
	        public void onException(Exception ex) {
	            ex.printStackTrace();
	        }
	    };
	    
	    twitterStream.addListener(listener);
	    // listening to tweets by sample() because normal users need
	    // special authentication from twitter, so Twitter4j
	    // sample() connects to random public streaming to 
	    // get tweets from
	    twitterStream.sample();
	    
	    // because twitterStream runs on another thread
	    // by the time this function done executing twitterStream's
	    // thread will be still running to get the tweets, it 
	    // takes approximately 5 seconds to start and one second 
	    // for 100 tweets that is in millisecond = (numberOfTweets * 10)
	    // after that twitterStream is shutdown and data is written
	    // to the database
	    try {
			Thread.sleep(5000 + (limit * 10));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    }
}
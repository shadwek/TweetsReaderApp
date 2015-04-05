package local.treader.main;

import java.util.Locale;

import twitter4j.Status;

import com.google.gson.Gson;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.util.JSON;

/**
 * 
 * Project: Tweets Reader
 * File: Database.java
 * Purpose: do all database related tasks
 * @author Hussain Hajjar : 2015/03/31
 * @version 1.0
 * 
 */
public class Database {

	// GLOBAL_VERIABLES to share data within class instead of passing 
	// them every time necessary 
	private static MongoClient MONGO_CLIENT;
	private static DB DATABASE;
	private static DBCollection COLLECTION;
	private static DBCollection TEMP_COLL;
	private static DBCollection LANGUAGES;
	private static DBCollection TIMES;
	private static String FIELD;
	
	/**
	 * startConnection connects the calling application to MongoDB
	 * to write new data to the database
	 * @return MongoCollection, collection to add documents to 
	 * 		   from calling application
	 */
	@SuppressWarnings("deprecation")
	public static DBCollection newConnection(){
		
		// connect to MongoDB at "localhost:27017"
		MONGO_CLIENT = new MongoClient("localhost", 27017);
		// drop previous database to write new data
		MONGO_CLIENT.getDB("tweetsDB").dropDatabase();
		// create new database
		DATABASE = MONGO_CLIENT.getDB("tweetsDB");
		// create a new collection from database named "tweets"
		COLLECTION = DATABASE.getCollection("tweetsData"); 
		return COLLECTION;
	}
	
	/**
	 * connect connects to MongoDB to only read data from database
	 * 
	 * @param collection: DBCollection collection to connect to
	 * 
	 * @return requested DBCollection
	 */
	@SuppressWarnings({ "deprecation", "static-access" })
	public static String connect(String collection){
		
		// JSON object to return
		JSON json = new JSON();

		MongoClient client = new MongoClient("localhost", 27017);
		DBCursor cursor = client.getDB("tweetsDB").getCollection(collection).find();
		String data = json.serialize(cursor); // serialize converts DBCursor object to String
		client.close();
        return  data;
	}
	
	/**
	 * closeConnection closes the connection with MongoDB
	 */
	public static void closeConnection(){
		
		MONGO_CLIENT.close();
	}	
	
	/** 
	 * addDocument reads data from Status object and save them as BasicDBObject
	 * 
	 * @param status: Status object to read status data from
	 * 
	 * @return tweetDocument: BasicDBObject holding required status data 
	 * 			* useful for recursion when a tweet is a retweet 
	 * 			* to return it as BasicDBObject and store it in "tweet_into"
	 * 			* and to return it to the caller to add it to a collection
	 */
	@SuppressWarnings("deprecation")
	public static BasicDBObject addDocument(Status status){

	    Gson gson = new Gson(); // this is used to encode tweets into UTF-8 instead of ?s
	    // This is to store Language name, because twitter store languages in ISO codes
	    // like en(ISO code) for English(language name) and ar for Arabic
    	String language = new Locale(status.getLang()).getDisplayName(); 
    	
	    // String to hold each tweet to make it more readable
        String tweet = "@" + status.getUser().getScreenName() + " has (" + status.getUser().getFollowersCount() + ") followers.";
        
        if(status.isRetweet()){
        	tweet += " Retweeted in " + language + " : " + "\"" + JSON.parse(gson.toJson(status.getRetweetedStatus().getText())).toString() + 
        			"\"" +" at: " + status.getCreatedAt().getHours() + ":" + status.getCreatedAt().getMinutes() + 
        			" originally writen by " + status.getRetweetedStatus().getUser().getScreenName() + 
        			" \n (TotalRetweets:" + status.getRetweetedStatus().getRetweetCount() + ").";
        }else{
        	tweet += " Tweeted in " + language + " : " + "\"" + JSON.parse(gson.toJson(status.getText())).toString() + 
        			" at " + status.getCreatedAt().getHours() + ":" + status.getCreatedAt().getMinutes() + ".";
        	
    		// language Check to update languages in LANGUAGES collection
    		dataCheck("language", language);
    		// add tweet's minute and day to check and update time in TIMES collection
    		dataCheck("minute", status.getCreatedAt());

        }
		// add required status data to BasicDBObject document
		// and assuming all tweets are originals (not retweets)
		BasicDBObject  tweetDocument = new BasicDBObject ("user_name", status.getUser().getScreenName())
		.append("tweet_text", tweet)
		.append("num_of_followers", status.getUser().getFollowersCount())
		// encode the status for non English languages
		.append("tweet_info", JSON.parse(gson.toJson(status.getText())).toString())
		.append("language", language)
		.append("created_at", status.getCreatedAt())
		.append("is_retweet", "no")
		.append("is_favorited", status.isFavorited())
		.append("retweet_count", status.getRetweetCount())
		.append("favorite_count", status.getFavoriteCount())
		.append("num_of_mentions", countMentions(status.getText()));
		

		// if a tweet is a retweet, replace "tweet_info" value with original 
		// tweet document as BasicDBObject
		if(status.isRetweet()){
			tweetDocument.replace("tweet_info", addDocument(status.getRetweetedStatus()));
			tweetDocument.replace("is_retweet", "yes");
			tweetDocument.replace("retweet_count", status.getRetweetedStatus().getRetweetCount());
		}
		return tweetDocument;
	}
	
	/** 
	 * addTweet reads data from Status object and save them as BasicDBObject
	 * 
	 * @param tweet: tweet String
	 * 
	 */
	public static void addTweet(String tweet){

	    Gson gson = new Gson();
		BasicDBObject  tweetDocument = new BasicDBObject ("tweet", JSON.parse(gson.toJson(tweet)).toString());
		DATABASE.getCollection("tweets").insert(tweetDocument);
	}
	
	/**
	 * dataCheck checks times of tweets
	 * 
	 * @param data: String holding kind of data -> "minute" for time
	 * 
	 * @param date: Date object holding date data
	 */
	public static void dataCheck(String data, java.util.Date date){
		
		@SuppressWarnings("deprecation")
		// get only days and minutes because most of twitter 
		// stream is recent tweets
		String count = date.getDay() + ":" + date.getMinutes();
		dataCheck(data, count);
	}
	
	/** 
	 * toDay convert Date.getDay() (returns int: day number)
	 *       to actual day name to make it more meaningful
	 * @param dayNumber: int day of the week (0-6)
	 * 
	 * @return String: actual day name
	 */
	public static String toDay(int dayNumber){
		
		// list all days starting from Sunday as 0 = Sunday
		String[] days = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
		return days[dayNumber];
	}
	
	/**
	 * dataCheck checks language and time (when called from
	 * dataCheck(String, Date) of tweets
	 * 
	 * @param data: String holding kind of data -> "minute" for time
	 * 									 		-> "language" for language
	 * 
	 * @param String: when it is String with no ":" then it is: language data
	 * 				  when it is String with ":" then it is time data
	 */
	public static void dataCheck(String data, String count){
		
		// create local collection because dataCheck checks
		// two types of data (language and time)
		DBCollection collection;
		String day = null;
		// make a new collection of data (language or time) to run statistic on its data
		if(data.equals("language")){
			collection = DATABASE.getCollection("languages");
			LANGUAGES = collection;
		}else{
			collection = DATABASE.getCollection("times");
			TIMES = collection;
			// store day data and minute as count to count its occurrences
			day = toDay(Integer.parseInt(count.split(":")[0]));
			count = count.split(":")[1];
		}
		
		// count is associated with related data to count its occurrences
		// * query when used with find() acts as a condition to find
		//   all data that has the field "data"
		// * field when used with find() acts as a condition to find
		// queries that match the condition 
		BasicDBObject query = new BasicDBObject(data, count);
		BasicDBObject field = new BasicDBObject(data, count)
			// get all field with "data" where its field "count"
			// has data
			.append("count", 1);
		// append "day" field when it is time to write day
		if(data.equals("minute")) field.append("day", day);
		// execute the query and store the result in langObject
		DBObject langObject = collection.findOne(query, field);
		
		// make sure collection and result (langObject) are not NULL
		if(collection != null && langObject != null){ // when there
			// are already previous "language" or "minute" in data collection
			int newValue; // to store new incremented value
			// get the value associated with "count" and add 1 to it 
			// after converting it to int
			newValue = Integer.parseInt(langObject.get("count").toString()) + 1;
			// update document with new values
			BasicDBObject update = new BasicDBObject(data, count)
			.append("count", newValue); // updating
			collection.update(langObject, update); // update the collection
		}else{ // else if there is no data, make new one and insert it to collection
			if(data.equals("minute")){
				collection.insert(new BasicDBObject(data, count).append("count", 1).append("day", day));
			}else collection.insert(new BasicDBObject(data , count).append("count", 1));
		}
	}
	
	/**
	 * countMentions counts how many users mentioned in a status
	 * 
	 * @param status: String hold status text
	 * 
	 * @return int: number of mentions in the status
	 */
	public static int countMentions(String status){
		
		int counter = 0;
		// split status to be able to identify users mentioned
		String[] list = status.split(" ");
		for(int i = 0; i < list.length; i++){
			if(list[i].contains("@")){
				counter++; // count how many users the tweeter mentioned
				
				// addToMentions(list[i].split("@")[1]); 
				// this is used to count how many times a particular 
				// user is mentioned I didn't implement it because it
				// is unlikely work because public tweets are random 
				// and it is highly impossible to have a user mentioned
				// many times
			}
		}
		return counter;
	}
	
	/**
	 * findTop5 finds top 5 users in respect to param:inField
	 * inField can be any of (num_of_retweets,
	 * 						  num_of_followers,
	 * 						  num_of_mentions,
	 * 						  languages
	 * 						  and times)
	 * then write results to DATABASE for easy fast access
	 * 
	 * @param inField: String holds what statistic to do
	 */
	public static void findTop5(String inField){

		// create a temporary collection to hold all results
		TEMP_COLL = DATABASE.getCollection("top_" + inField);
		FIELD = inField; // field for language and minute
		findTop("$gt", 0, 0);
	}		
	
	/**
	 * findTop finds the top 5 counts (if any: <5) of FIELD 
	 * 
	 * @param relOper: String relational operator that defines
	 * 		  the relation between bestMatch and counts data
	 * @param bestMatch: int the maximum match found in inField
	 * 		  ** first iteration will get the highest value of 
	 * 			 inField and so on
	 * @param count: int represents the number of results (TOP_****)
	 */
	public static void findTop(String relOper, int bestMatch, int count){

		// These conditions save rewriting code for every findTop method 
		// for every statistic (inField)
		String data, inField = null;
		if(FIELD.equals("language")){
			COLLECTION = LANGUAGES;
			data = "language";
			inField = "count";
		}else if(FIELD.equals("minute")){
			COLLECTION = TIMES;
			data = "minute";
			inField = "count";
		}else{
			data = "user_name";
			inField = FIELD;
		}
	    BasicDBObject query = new BasicDBObject(inField, new BasicDBObject(relOper, bestMatch));
	    BasicDBObject user = new BasicDBObject(data, 1)
			.append(inField, 1);
		DBCursor cursor = COLLECTION.find(query, user);
		bestMatch = 0; // reset bestMatch to find next bestMatch
					   // if is not reset it will result the same
					   // bestMatch found the first time
	    DBObject currentDBObject = null, bestDBObject = null;		
		try{
			while(cursor.hasNext()) {
				currentDBObject = cursor.next();
				if(Integer.parseInt(currentDBObject.get(inField).toString()) > bestMatch){ 
					bestMatch = Integer.parseInt(currentDBObject.get(inField).toString());
					bestDBObject = currentDBObject; // store current DBObject to insert it
				   }
			}
		}finally{
			// if the last found bestDBObject was the best,
			// let curerntDBObject (last element at index = 4)
			// is the best found (the one before bestDBObject (index = 3)
			if(bestDBObject == null) bestDBObject = currentDBObject;
			try{ // try to insert
				TEMP_COLL.insert(bestDBObject);
				// if bestDBObject is null exception will be risen
			}catch(NullPointerException e){
				return; // return couldn't find more (if any) top matches
			}
			cursor.close();
		}			// count works as counter to count results
		if(count == 4){// **make global static variable if you want count to be set by user
			// reset COLLECTION to "tweets" because when finding
			// top_languages, COLLECTION = LANGUAGES, 
			// top_minute, COLLECTION = TIMES, and because
			// COLLECTION is global variable any modifications will effect next 
			// calls of findTop when find() will look in LANGUAGES collection
			// instead of COLLECTION (of tweets)
			COLLECTION = DATABASE.getCollection("tweetsData");
			return;
			// find next best match less than found best match "$ls"
		}else findTop("$lt", bestMatch, count + 1);
	}
	
//	/**
//	 * addToMentions method adds 1 to "num_of_mentions" count
//	 * when a user is mentioned.
//	 * Usually it is only once, but in some cases if a famous user
//	 * is mentioned many times so the application keeps track.
//	 * 
//	 * @param userName: String represents the mentioned user 
//	 */
//	public static void addToMentions(String userName){
//		
//		@SuppressWarnings("deprecation")
//		DBCollection collection = MONGO_CLIENT.getDB("tweets").getCollection("mentions");
	// maybe add BasicDBObject query = new BasicDBObject();
	// and collection.find(query, user);
//	    BasicDBObject user = new BasicDBObject("user_name", userName)
//	    	.append("num_of_mentions", new BasicDBObject("$gt", 0));
//	    DBObject doc = collection.findOne(user);
//	    int newValue;
//		if(doc != null){
//			newValue = Integer.parseInt(user.getString("num_of_mentions")) + 1;
//			user.replace("num_of_mentions", newValue);
//			DBObject newUser = user; 
////					new BasicDBObject("user_name", userName)
////				.append("num_of_mentions", newValue);
////			collection.remove(query)
//			
//			collection.update(doc, newUser);
//		}else{
//			collection.insert(new BasicDBObject("user_name", userName).append("num_of_mentions", 1));
//		}
//		closeConnection();
//	}
}

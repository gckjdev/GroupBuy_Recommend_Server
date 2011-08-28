package com.orange.groupbuy.recommender;


import java.util.Random;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.mongodb.BasicDBObject;
import com.orange.common.mongodb.MongoDBClient;
import com.orange.common.utils.StringUtil;
import com.orange.groupbuy.constant.DBConstants;
import com.orange.groupbuy.dao.User;
import com.orange.groupbuy.manager.UserManager;

public class MongoDBData {

    MongoDBClient mongoClient;
	static String userId;
	static String deviceId;
	private Random seed;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() {
		mongoClient = new MongoDBClient("localhost", "groupbuy", "", "");
		seed = new Random();
	}

	
	@Test
	public void insertUserForRecommend() {
	    for (int i = 0; i < 1; i++) {
	        BasicDBObject obj = new BasicDBObject();
//	        obj.put(DBConstants.F_RECOMMEND_STATUS, DBConstants.C_RECOMMEND_STATUS_NOT_RUNNING);
	        obj.put(DBConstants.F_USERID, Integer.toString(i));
	        mongoClient.insert(DBConstants.T_USER, obj);
	    }
	}
	
	@Test
	public void addUserShoppingItem() {
	    for (int i = 0; i < 1; i++) {
	        User user = UserManager.findUserByUserId(mongoClient,  Integer.toString(i));
	        user.addShoppingItem( "item"+0, "西餐", "自助餐", "自助餐", "北京",  10f,  10f);
	        user.addShoppingItem( "item"+1, "西餐", "法国菜", "西餐 法国菜", "广州",  10f,  10f);
	        UserManager.save(mongoClient, user);
	    }
	}
	
	@Test
	public void testRecommend() {
	    BasicDBObject item = new BasicDBObject();       
        item.put(DBConstants.F_PRODUCTID, 1); 
        item.put(DBConstants.F_SCORE,1); 
                                                
        BasicDBObject query = new BasicDBObject();
        query.put(DBConstants.F_FOREIGN_USER_ID,  "0");

        BasicDBObject pushValue = new BasicDBObject();      
        pushValue.put(DBConstants.F_RECOMMENDLIST, item);

        BasicDBObject update = new BasicDBObject();             
        update.put("$push", pushValue);
        
        mongoClient.upsertAll(DBConstants.T_RECOMMEND, query, update);
	}
	
	@Test
    public void resetRecommendStatus() {
            
	    BasicDBObject query = new BasicDBObject();
        BasicDBObject update = new BasicDBObject();
        
        BasicDBObject value = new BasicDBObject();
        value.put("$ne", DBConstants.C_RECOMMEND_STATUS_NOT_RUNNING);
        query.put(DBConstants.F_RECOMMEND_STATUS, value);

        BasicDBObject updateValue = new BasicDBObject();
        updateValue.put(DBConstants.F_RECOMMEND_STATUS, DBConstants.C_RECOMMEND_STATUS_NOT_RUNNING);
        update.put("$set", updateValue);
        
        mongoClient.updateAll(DBConstants.T_USER, query, update);
    }
	
	@Test
	public void testfindAndModifyUpsert() {
	    mongoClient.findAndModifyUpsert(DBConstants.T_USER,"r_status",0,1);
	    
	}
	
}

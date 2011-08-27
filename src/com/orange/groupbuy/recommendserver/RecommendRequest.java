package com.orange.groupbuy.recommendserver;

import java.util.List;

import org.apache.log4j.Logger;

import com.mongodb.BasicDBObject;
import com.orange.common.mongodb.MongoDBClient;
import com.orange.common.processor.BasicProcessorRequest;
import com.orange.common.processor.CommonProcessor;
import com.orange.common.solr.SolrClient;
import com.orange.common.utils.StringUtil;
import com.orange.groupbuy.constant.DBConstants;
import com.orange.groupbuy.dao.Product;
import com.orange.groupbuy.dao.User;
import com.orange.groupbuy.manager.ProductManager;
import com.orange.groupbuy.manager.UserManager;

/**
 * The Class RecommendRequest.
 */
public class RecommendRequest extends BasicProcessorRequest{

    public static final Logger log = Logger.getLogger(RecommendRequest.class.getName());

    private User user;

    public RecommendRequest(User user) {
        super();
        this.user = user;
    }


    @Override
    public void execute(CommonProcessor mainProcessor) {
        MongoDBClient mongoClient = mainProcessor.getMongoDBClient();

        for (int i = 0; i < user.getShoppingItem().size(); i++) {

            BasicDBObject item = (BasicDBObject) (user.getShoppingItem().get(i));

            String city = (String) item.get(DBConstants.F_CITY);
            String cate = (String) item.get(DBConstants.F_CATEGORY_NAME);
            String subcate = (String) item.get(DBConstants.F_SUB_CATEGORY_NAME);
            String itemId = (String) item.get(DBConstants.F_ITEM_ID);

            String keyword = generateKeyword(city, cate, subcate);

            List<Product> productList = ProductManager.searchProductBySolr(SolrClient.getInstance(), mongoClient, city,
                    null, false, keyword, 0, RecommendConstants.MAX_RECOMMEND_COUNT);

            if (productList != null && productList.size() >= 0) {
                for (Product product : productList) {
                    UserManager.insertRecommendItem(mongoClient, user, product, itemId);
                    UserManager.recommendClose(mongoClient, user);
                }
            } else {
                log.info("no product match to be recommended.");
                UserManager.recommendFailure(mongoClient, user);
            }
        }
    }

    private String generateKeyword(String city, String cate, String subcate) {
        
        String keyword = "";
        if (!StringUtil.isEmpty(city)) {
            keyword = city;
        }
        if (!StringUtil.isEmpty(cate)) {
            keyword = keyword.concat(" ").concat(cate);
        }
        if (!StringUtil.isEmpty(subcate)) {
            keyword = keyword.concat(" ").concat(subcate);
        }

        return keyword.trim();
    }

}

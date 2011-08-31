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
import com.orange.groupbuy.dao.RecommendItem;
import com.orange.groupbuy.dao.User;
import com.orange.groupbuy.manager.ProductManager;
import com.orange.groupbuy.manager.PushMessageManager;
import com.orange.groupbuy.manager.RecommendItemManager;
import com.orange.groupbuy.manager.UserManager;

/**
 * The Class RecommendRequest.
 */
public class RecommendRequest extends BasicProcessorRequest {

    public static final Logger log = Logger.getLogger(RecommendRequest.class.getName());

    private User user;

    public RecommendRequest(User user) {
        super();
        this.user = user;
    }

    @Override
    public String toString() {
        return "RecommendRequest [user=" + user.getUserId() + "]";
    }

    @Override
    public void execute(CommonProcessor mainProcessor) {
        MongoDBClient mongoClient = mainProcessor.getMongoDBClient();

        if (user.getShoppingItem() == null || user.getShoppingItem().size() <= 0) {
            log.info("find user(" + user.getUserId() + ") but user has no shopping item");
            return;
        }
        
        String userId = user.getUserId();

        for (int i = 0; i < user.getShoppingItem().size(); i++) {

            try{
                BasicDBObject item = (BasicDBObject) (user.getShoppingItem().get(i));
    
                String city = (String) item.get(DBConstants.F_CITY);
                String cate = (String) item.get(DBConstants.F_CATEGORY_NAME);
                String subcate = (String) item.get(DBConstants.F_SUB_CATEGORY_NAME);
                String itemId = (String) item.get(DBConstants.F_ITEM_ID);
                String keyword = (String) item.get(DBConstants.F_KEYWORD);
    
                String keywords = generateKeyword(city, cate, subcate, keyword);
    
                RecommendItem recommendItem = RecommendItemManager.findRecommendItem(mongoClient, user.getUserId(), itemId);
    
                List<Product> productList = ProductManager.searchProductBySolr(SolrClient.getInstance(), mongoClient, city,
                        null, false, keywords, 0, RecommendConstants.MAX_RECOMMEND_COUNT);
    
                if (productList == null || productList.size() <= 0) {
                    log.info("no product match to be recommended for user=" + userId + ", itemId = " + itemId);
                    UserManager.recommendFailure(mongoClient, user);
                    return;
                }


                // add product into recommend item list
                boolean hasChange = false;
                int addCount = 0;
                for (Product product : productList) {
                    // log.debug("process product " + product.getId() + ", title = " + product.getTitle());
                    if (RecommendItemManager.addOrUpdateProduct(recommendItem, product)) {
                        log.info("add or update product (" + product.getId() +
                                "), score = " + product.getScore() +
                                " into recommend item = " + itemId);

                        hasChange = true;
                        addCount++;
                    }
                }

                log.info(productList.size() + " product found, " + addCount +
                        " are added/updated for user=" + userId + ", itemId = " + itemId);

                if (hasChange) {
                    // sort product in recommend item
                    String productId = recommendItem.sortAndSelectProduct();

                    Product product = ProductManager.findProductById(mongoClient, productId);

                    if (product != null) {
                        // select one product for pushMessage, and set the status to sending
                        saveProductToPushMessage(mongoClient, product,  user);
                    }

                    // save object into DB
                    mongoClient.save(DBConstants.T_RECOMMEND, recommendItem.getDbObject());
                }

                UserManager.recommendClose(mongoClient, user);
            }
            catch (Exception e) {
                log.error("Processing user(" + user.getUserId() +
                        ") shopping item, but catch exception = " +
                        e.toString() + e.getMessage());

                UserManager.recommendFailure(mongoClient, user);
            }

        }
    }

    private void saveProductToPushMessage(MongoDBClient mongoClient, Product product, User user) {
        if (product == null) {
            return;
        }

        log.info("select product = " + product.getId() + ", score = " + product.getScore() +
                " for push to user = " + user.getUserId());

        PushMessageManager.savePushMessage(mongoClient, product, user);
    }

    private String generateKeyword(String city, String cate, String subcate, String keyword) {

        // TODO change conditions
        
        String keywords = "";
        if (!StringUtil.isEmpty(city)) {
            keywords = city;
        }
        if (!StringUtil.isEmpty(cate)) {
            keywords = keyword.concat(" ").concat(cate);
        }
        if (!StringUtil.isEmpty(subcate)) {
            keywords = keyword.concat(" ").concat(subcate);
        }
        if (!StringUtil.isEmpty(keyword)) {
            keywords = keyword.concat(" ").concat(keyword);
        }

        return keywords.trim();
    }

}

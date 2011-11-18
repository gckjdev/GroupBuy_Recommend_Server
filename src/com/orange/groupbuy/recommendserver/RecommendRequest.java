package com.orange.groupbuy.recommendserver;

import java.util.Date;
import java.util.List;
import org.apache.log4j.Logger;
import com.mongodb.BasicDBObject;
import com.orange.common.mongodb.MongoDBClient;
import com.orange.common.processor.BasicProcessorRequest;
import com.orange.common.processor.CommonProcessor;
import com.orange.common.solr.SolrClient;
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
    
    public static final int MAX_RECOMMEND_COUNT = 25;

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

            try {
                BasicDBObject item = (BasicDBObject) (user.getShoppingItem().get(i));
                String city =  item.getString(DBConstants.F_CITY);
                String cate =  item.getString(DBConstants.F_CATEGORY_NAME);
                String subcate = item.getString(DBConstants.F_SUB_CATEGORY_NAME);
                String itemId = item.getString(DBConstants.F_ITEM_ID);
                String keyword = item.getString(DBConstants.F_KEYWORD);
                String latitude = item.getString(DBConstants.F_LATITUDE);
                String longitude = item.getString(DBConstants.F_LONGITUDE);
                String radius = item.getString(DBConstants.F_RADIUS);
                Double maxPrice = null;
                if (item.containsField(DBConstants.F_MAX_PRICE)){
                    maxPrice = item.getDouble(DBConstants.F_MAX_PRICE);
                }
                Date expireDate = (Date) item.get(DBConstants.F_EXPIRE_DATE);
                String appId = item.getString(DBConstants.F_APPID);

                if (RecommendItemManager.isExpire(expireDate)) {
                    log.info("user already expired, skip. userId = " + user.getUserId() + ", itemId = " + itemId + ",  expireDate = " + expireDate);
                    continue;
                }

                String keywords = RecommendItemManager.generateKeyword(cate, subcate, keyword);
    
                RecommendItem recommendItem = RecommendItemManager.findAndUpsertRecommendItem(mongoClient, user.getUserId(), itemId, appId);
                RecommendItemManager.cleanExpireProduct(mongoClient, recommendItem);
                
                if (recommendItem.hasRecommendToday()){
                    log.info("user item "+itemId+" has been recommended today, skip matching action");
                    continue;
                }

                List<Product> productList = null;
                if(latitude != null && latitude.length() > 0
                        &&longitude != null && longitude.length() > 0
                        &&radius != null && radius.length() > 0){
                    
                    Double latitudeValue = Double.valueOf(latitude);
                    Double longitudeValue = Double.valueOf(longitude);
                    Double radiusValue = Double.valueOf(radius)/1000;
                    
                    productList = ProductManager.searchProductBySolr(SolrClient.getInstance(), mongoClient,
                            city, null, false, keywords, maxPrice, latitudeValue, longitudeValue, radiusValue, 0, MAX_RECOMMEND_COUNT);
                }
                else{
                    productList = ProductManager.searchProductBySolr(SolrClient.getInstance(), mongoClient,
                        city, null, false, keywords, maxPrice, 0, MAX_RECOMMEND_COUNT);
                    log.info("<RecommendRequest> location match switch is turned off or null,latitude "+latitude+" longitude is "+longitude+" radius is "+radius);
                }
                if (productList == null || productList.size() <= 0) {
                    log.info("no product match to recommend for user=" + userId + ", itemId = " + itemId);
                    UserManager.recommendFailure(mongoClient, user);
                    continue;
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
                    String productId = recommendItem.sortAndSelectProduct(user);

                    if (productId == null) {
                        log.info("No product to recommend");
                        continue;
                    }

                    Product product = ProductManager.findProductById(mongoClient, productId);

                    if (product != null) {
                        // select one product for pushMessage, and set the status to sending
                        saveProductToPushMessage(mongoClient, product,  user, recommendItem);
                    }

                    mongoClient.save(DBConstants.T_USER, user.getDbObject());
                    mongoClient.save(DBConstants.T_RECOMMEND, recommendItem.getDbObject());
                }
                
                UserManager.recommendClose(mongoClient, user);
            }
            catch (Exception e) {
                log.error("Processing user(" + user.getUserId() + ") shopping item, but catch exception = " 
                        + e.getMessage(), e);
                UserManager.recommendFailure(mongoClient, user);
            }

        }
    }

    private void saveProductToPushMessage(MongoDBClient mongoClient, Product product, User user, RecommendItem item) {
        if (product == null) {
            return;
        }

        log.info("select product = " + product.getId() + " for push to user = " + user.getUserId());
        PushMessageManager.savePushMessage(mongoClient, product, user, item);
    }

              
}

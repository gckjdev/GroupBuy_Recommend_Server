package com.orange.groupbuy.recommendserver;
import com.orange.common.mongodb.MongoDBClient;
import com.orange.common.processor.BasicProcessorRequest;
import com.orange.common.processor.ScheduleServerProcessor;
import com.orange.common.utils.DateUtil;
import com.orange.groupbuy.constant.DBConstants;
import com.orange.groupbuy.dao.User;
import com.orange.groupbuy.manager.UserManager;


public class RecommendProcessor extends ScheduleServerProcessor {

    static final String MONGO_SERVER = "localhost";
    static final String MONGO_USER = "";
    static final String MONGO_PASSWORD = "";

    private static MongoDBClient mongoClient = new MongoDBClient(MONGO_SERVER, DBConstants.D_GROUPBUY, MONGO_USER, MONGO_PASSWORD);

    @Override
    public final MongoDBClient getMongoDBClient () {
        return mongoClient;
    }

    @Override
    public void resetAllRunningMessage () {
        UserManager.resetAllRunningMessage(mongoClient);
    }

    @Override
    public final BasicProcessorRequest getProcessorRequest () {

        User user = UserManager.findUserForRecommend(mongoClient);

        if (user == null) {
            log.info("no user to be recommended.");
            return null;
        }
        RecommendRequest request = new RecommendRequest(user);
        return request;
    }

    @Override
    public final boolean canProcessRequest () {

        if (DateUtil.isMiddleDate(RecommendConstants.START_DATE_HOUR, RecommendConstants.START_DATE_MINUTE,
                                  RecommendConstants.END_DATE_HOUR, RecommendConstants.END_DATE_MINUTE)) {
            return true;
        }
        return false;
    }

}

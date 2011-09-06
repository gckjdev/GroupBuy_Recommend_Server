package com.orange.groupbuy.recommendserver;
import com.orange.common.mongodb.MongoDBClient;
import com.orange.common.processor.BasicProcessorRequest;
import com.orange.common.processor.ScheduleServerProcessor;
import com.orange.common.utils.DateUtil;
import com.orange.groupbuy.dao.User;
import com.orange.groupbuy.manager.UserManager;


public class RecommendProcessor extends ScheduleServerProcessor {

    private MongoDBClient mongoClient;

    public RecommendProcessor(MongoDBClient mongoDBClient) {
        super();
        this.mongoClient = mongoDBClient;
    }

    @Override
    public final MongoDBClient getMongoDBClient () {
        return mongoClient;
    }

    @Override
    public void resetAllRunningMessage () {
        UserManager.resetRecommendationStatus(mongoClient);
    }

    @Override
    public final BasicProcessorRequest getProcessorRequest () {

        User user = UserManager.findUserForRecommend(mongoClient);

        if (user == null) {
            log.debug("No user to be recommended.");
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
        else {
            log.debug("Current date is not valid for running recommendation");
            return false;
        }
    }

}

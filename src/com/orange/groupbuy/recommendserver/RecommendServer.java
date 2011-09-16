package com.orange.groupbuy.recommendserver;

import org.apache.log4j.Logger;

import com.orange.common.mongodb.MongoDBClient;
import com.orange.common.processor.ScheduleServer;
import com.orange.groupbuy.constant.DBConstants;

public class RecommendServer {

    public static final String VERSION_STRING = "0.9 Beta Build 20110829-01";

    public static final Logger log = Logger.getLogger(RecommendServer.class.getName());

    private static final int MAX_THREAD_NUM = 10;
    private static final int RESET_HOUR = 7;            // 7 AM in the morning, reset all tasks

    private static MongoDBClient mongoClient = new MongoDBClient(DBConstants.D_GROUPBUY);


    public static void main(final String[] args) throws InstantiationException, IllegalAccessException {

        log.info("RecommendServer start... version " + VERSION_STRING);


        ScheduleServer scheduleServer = new ScheduleServer(new RecommendProcessor(mongoClient));
        scheduleServer.setThreadNum(MAX_THREAD_NUM);
        scheduleServer.setResetHour(RESET_HOUR);
        Thread server = new Thread(scheduleServer);
        server.start();

    }


}

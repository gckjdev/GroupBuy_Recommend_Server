package com.orange.groupbuy.recommendserver;

import org.apache.log4j.Logger;

import com.orange.common.processor.ScheduleServer;

public class RecommendServer {
    
    public static final String VERSION_STRING = "0.9 Beta Build 20110829-01";
    
    public static final Logger log = Logger.getLogger("abc");

    private static final int MAX_THREAD_NUM = 10;

    public static void main(final String[] args) throws InstantiationException, IllegalAccessException {

        log.info("RecommendServer start... version "+VERSION_STRING);
        
        ScheduleServer scheduleServer = new ScheduleServer(new RecommendProcessor());
        scheduleServer.setMax_thread_num(MAX_THREAD_NUM);
        Thread server = new Thread(scheduleServer);
        server.start();

    }


}

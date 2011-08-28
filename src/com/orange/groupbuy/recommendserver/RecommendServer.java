package com.orange.groupbuy.recommendserver;

import com.orange.common.processor.ScheduleServer;

public class RecommendServer {

    public static void main(final String[] args) throws InstantiationException, IllegalAccessException {

        ScheduleServer scheduleServer = new ScheduleServer(new RecommendProcessor());
        Thread server = new Thread(scheduleServer);
        server.start();

    }


}

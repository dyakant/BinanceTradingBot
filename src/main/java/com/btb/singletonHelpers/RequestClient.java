package com.btb.singletonHelpers;

import com.binance.client.RequestOptions;
import com.binance.client.SyncRequestClient;
import com.btb.data.Config;

public class RequestClient {
    private final SyncRequestClient syncRequestClient;

    private static class RequestClientHolder {
        private static final RequestClient RequestClient = new RequestClient();
    }

    private RequestClient() {
        RequestOptions options = new RequestOptions();
        syncRequestClient = SyncRequestClient.create(Config.API_KEY, Config.SECRET_KEY, options);
    }

    public static RequestClient getRequestClient() {
        return RequestClientHolder.RequestClient;
    }

    public SyncRequestClient getSyncRequestClient() {
        return syncRequestClient;
    }

}

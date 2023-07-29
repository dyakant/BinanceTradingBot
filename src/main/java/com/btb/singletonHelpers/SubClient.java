package com.btb.singletonHelpers;

import com.binance.client.SubscriptionClient;

public class SubClient {
    private final SubscriptionClient subscriptionClient;

    private static class SubClientHolder {
        private static final SubClient subClient = new SubClient();
    }

    private SubClient() {
        subscriptionClient = SubscriptionClient.create();
    }

    public static SubClient getSubClient() {
        return SubClientHolder.subClient;
    }

    public SubscriptionClient getSubscriptionClient() {
        return subscriptionClient;
    }
}

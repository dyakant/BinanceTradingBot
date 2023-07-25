package com.btb.singletonHelpers;

import com.btb.data.Config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExecService {
    private final ExecutorService executorService;

    private static class ExecServiceHolder {
        private static final ExecService execService = new ExecService();
    }

    private ExecService() {
        executorService = Executors.newFixedThreadPool(Config.THREAD_NUM);
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public static ExecService getExecService() {
        return ExecServiceHolder.execService;
    }

}

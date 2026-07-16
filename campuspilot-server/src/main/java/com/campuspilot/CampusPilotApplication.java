package com.campuspilot;

import com.campuspilot.config.AppConfig;
import com.campuspilot.http.ApiHandler;
import com.campuspilot.http.StaticFileHandler;
import com.campuspilot.service.AgentClient;
import com.campuspilot.service.KingdeeDataClient;
import com.campuspilot.store.InMemoryCampusPilotStore;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public final class CampusPilotApplication {
    private CampusPilotApplication() {
    }

    public static void main(String[] args) throws Exception {
        AppConfig config = AppConfig.load(args);
        InMemoryCampusPilotStore store = new InMemoryCampusPilotStore(config);
        KingdeeDataClient kingdeeDataClient = new KingdeeDataClient(config, store);
        AgentClient agentClient = new AgentClient(config, store, kingdeeDataClient);

        HttpServer server = HttpServer.create(new InetSocketAddress(config.host(), config.port()), 0);
        ApiHandler apiHandler = new ApiHandler(config, store, agentClient, kingdeeDataClient);
        server.createContext("/api/campuspilot", apiHandler);
        server.createContext("/api/student", apiHandler);
        server.createContext("/", new StaticFileHandler(config.staticRoot()));
        server.setExecutor(Executors.newFixedThreadPool(config.workerThreads()));
        server.start();

        System.out.println("CampusPilot Java backend started");
        System.out.println("  URL: http://" + config.hostForLog() + ":" + config.port() + "/index.html#dashboard");
        System.out.println("  Static root: " + config.staticRoot());
        System.out.println("  Agent mode: " + (config.agentOpenApiConfigured()
                ? "Kingdee OpenAPI auto discovery: " + config.agentName()
                : "local fallback"));
        System.out.println("  Kingdee data mode: " + kingdeeDataClient.dataMode());
    }
}

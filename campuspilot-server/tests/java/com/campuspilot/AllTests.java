package com.campuspilot;

public final class AllTests {
    public static void main(String[] args) throws Exception {
        KingdeeClientTest.run();
        StudentProfileServiceTest.run();
        GrowthPlanServiceTest.run();
        RiskServiceTest.run();
        System.out.println("All CampusPilot backend tests passed.");
    }
}
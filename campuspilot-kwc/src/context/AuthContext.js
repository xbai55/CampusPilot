import React, { createContext, useContext, useState, useCallback, useEffect } from "react";
import api from "../hooks/useApi";
import { getPermissions, isAuthenticated, getUser } from "../utils/permissions";

const AuthContext = createContext(null);

export function AuthProvider({ children, kwcData = null }) {
  const [user, setUser] = useState(() => getUser());
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);

  const loadData = useCallback(async () => {
    if (kwcData) {
      setData(kwcData);
      return;
    }
    if (!isAuthenticated()) {
      setData(null);
      return;
    }
    setLoading(true);
    try {
      const [
        overview, riskDistribution, students, courses, behaviors,
        warnings, workflow, workflowLogs, riskTrend, effectiveness,
        integrationStatus, lowcodeBlueprint, agentWorkflow, reportCenter, cloudNative, multimodal, agentInsight, roles, auditLogs,
      ] = await Promise.all([
        api.fetchOverview(), api.fetchRiskDistribution(),
        api.fetchStudents(), api.fetchCourses(),
        api.fetchBehaviors(), api.fetchWarnings(),
        api.fetchWorkflow(), api.fetchWorkflowLogs(),
        api.fetchRiskTrend(), api.fetchEffectiveness(),
        api.fetchIntegrationStatus(),
        api.fetchLowcodeBlueprint(), api.fetchAgentWorkflow(),
        api.fetchReportCenter(), api.fetchCloudNative(), api.fetchMultimodal(),
        api.fetchAgentInsight(), api.fetchRoles(), api.fetchAuditLogs(),
      ]);
      setData({
        overview, riskDistribution, students, courses, behaviors,
        warnings, workflow, workflowLogs, riskTrend, effectiveness,
        integrationStatus, lowcodeBlueprint, agentWorkflow, reportCenter, cloudNative, multimodal, agentInsight, roles, auditLogs,
      });
    } catch (err) {
      console.error("Failed to load data:", err);
    } finally {
      setLoading(false);
    }
  }, [kwcData]);

  const login = useCallback((userData) => {
    localStorage.setItem("campuspilot:user", JSON.stringify(userData));
    setUser(userData);
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem("campuspilot:user");
    setUser(null);
    setData(null);
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData, user]);

  const permissions = user ? getPermissions(user.role) : null;
  const isAuth = Boolean(user);

  const value = {
    user,
    isAuth,
    permissions,
    data,
    loading,
    login,
    logout,
    loadData,
    api,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}

export default AuthContext;

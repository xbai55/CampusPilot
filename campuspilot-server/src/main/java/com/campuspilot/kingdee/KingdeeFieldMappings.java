package com.campuspilot.kingdee;

import com.campuspilot.util.Json;

import java.util.LinkedHashMap;
import java.util.Map;

/** Central auditable mapping from Kingdee field codes to CampusPilot business fields. */
public final class KingdeeFieldMappings {
    private static final Map<String, String> FIELDS = fields();

    private KingdeeFieldMappings() {}

    public static Map<String, String> toBusiness(Map<String, String> row) {
        Map<String, String> mapped = new LinkedHashMap<>();
        row.forEach((key, value) -> mapped.put(FIELDS.getOrDefault(key, key), value));
        return mapped;
    }

    public static String toJson(Map<String, String> row) {
        return Json.object(toBusiness(row).entrySet().stream()
                .map(entry -> Json.field(entry.getKey(), entry.getValue()))
                .toArray(String[]::new));
    }

    private static Map<String, String> fields() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("number", "number"); map.put("name", "name");
        add(map, "studentnumber", "studentId", "studentno", "studentId", "student_name", "studentName",
                "studentname", "studentName", "careergoal", "careerGoal", "profiletags", "profileTags",
                "strengths", "strengths", "weaknesses", "weaknesses", "shortplan", "shortPlan",
                "midplan", "midPlan", "recommendedcourses", "recommendedCourses",
                "recommendedcompetiti", "recommendedCompetitions", "recommendedprojects", "recommendedProjects",
                "internationaladvice", "internationalAdvice", "tutoradvice", "tutorAdvice",
                "planstatus", "planStatus", "startdate", "startDate", "reviewdate", "reviewDate",
                "plannumber", "planNumber", "college", "college", "major", "major", "grade", "grade",
                "classname", "className", "interestarea", "interestArea", "gpa", "gpa",
                "creditrate", "creditRate", "failedcount", "failedCount", "attendancerate", "attendanceRate",
                "assignmentrate", "assignmentRate", "innovationscore", "innovationScore", "risklevel", "riskLevel",
                "riskscore", "riskScore", "riskreason", "riskReason", "aisuggestion", "aiSuggestion",
                "currentstatus", "currentStatus", "lastevaltime", "lastEvaluationTime",
                "semester", "semester", "attendance_rate", "attendanceRate",
                "assignment_completio", "assignmentCompletionRate", "assignment_completion", "assignmentCompletionRate", "learning_activity", "learningActivity",
                "interaction_count", "interactionCount", "last_login_time", "lastLoginTime",
                "behavior_risk_reason", "behaviorRiskReason", "recordperiod", "recordPeriod",
                "recorddate", "recordDate", "failedcourse", "failedCourseCount",
                "interactioncount", "interactionCount", "activityscore", "activityScore",
                "studyscore", "studyScore", "regularityscore", "regularityScore", "reviewresult", "reviewResult",
                "libraryvisits", "libraryVisits", "studyroomvisits", "studyRoomVisits", "labvisits", "labVisits",
                "platformactivity", "platformActivity", "checkincount", "checkinCount", "quizaverage", "quizAverage",
                "abnormalnote", "abnormalNote", "recnumber", "recordNumber", "coursename", "courseName",
                "teachername", "teacherName", "quizscore", "quizScore", "questioncount", "questionCount",
                "weakpoints", "weakPoints", "studentfeedback", "studentFeedback",
                "risk_level", "riskLevel", "risk_score", "riskScore", "risk_reason", "riskReason",
                "risk_reason_tag", "riskReasonTag", "ai_suggestion", "aiSuggestion",
                "ai_suggestion_tag", "aiSuggestionTag", "warning_source", "warningSource", "status", "status",
                "counselor", "counselor", "tutor", "tutor", "create_time", "createTime",
                "confirm_time", "confirmTime", "close_time", "closeTime", "std_feedback", "studentFeedback",
                "process_record", "processRecord", "opportunity", "opportunity", "opportunitytype", "opportunityType",
                "matchtags", "matchTags", "matchscore", "matchScore", "reason", "reason",
                "preparation", "preparation", "priority", "priority", "recommendstatus", "recommendStatus",
                "tutoropinion", "tutorOpinion", "suitablegrade", "suitableGrade",
                "abilityrequirement", "abilityRequirement", "tagrequirement", "tagRequirement",
                "description", "description", "owner", "owner", "suitablemajor", "suitableMajor",
                "oppnumber", "opportunityNumber", "oppname", "opportunityName", "starttime", "startTime",
                "deadline", "deadline", "reasontemplate", "reasonTemplate",
                "opportunitytype_title", "opportunityTypeTitle", "status_title", "statusTitle",
                "suitablemajor_title", "suitableMajorTitle", "course_name", "courseName",
                "course_type", "courseType", "credit", "credit", "score", "score",
                "is_core_course", "coreCourse", "is_failed", "failed", "course_direction", "courseDirection",
                "receiver", "receiver", "receiverrole", "receiverRole", "relatedbill", "relatedBill",
                "remindertype", "reminderType", "content", "content", "remindertime", "reminderTime",
                "processstatus", "processStatus", "processresult", "processResult", "notinumber", "notificationNumber",
                "warningnumber", "warningNumber", "checkindate", "checkinDate", "studycontent", "studyContent",
                "taskresult", "taskResult", "assignmentstatus", "assignmentStatus", "selfevaluation", "selfEvaluation",
                "tutorcomment", "tutorComment", "counselorremark", "counselorRemark",
                "abilitydimension", "abilityDimension", "importance", "importance", "triggertag", "triggerTag",
                "improvement", "improvementSuggestion", "projecttype", "projectType", "reflectnumber", "mappingNumber",
                "relatedcareer", "relatedCareer", "relatedcareer_title", "relatedCareerTitle");
        return Map.copyOf(map);
    }

    private static void add(Map<String, String> map, String... pairs) {
        for (int i = 0; i < pairs.length; i += 2) map.put("code_" + pairs[i], pairs[i + 1]);
    }
}
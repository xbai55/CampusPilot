insert into cp_student_profile (
    student_no, student_name, college, major, grade, class_name, career_goal, interests,
    gpa, attendance, failed_courses, course_score, behavior_score, innovation_score,
    risk_level, risk_key, risk_score, profile, suggestion, support_status, updated_at
) values
('STU2026001', '张明远', '信息工程学院', '人工智能', '2023级', 'AI 2301', 'AI 算法工程师', '机器学习、算法竞赛', 2.31, 71, 2, 76, 62, 38, '高风险', 'high', 86, '高等数学和数据结构薄弱，出勤率与作业完成率偏低。', '建议辅导员确认预警，导师制定 4 周课程补强计划。', '帮扶中', '2026-06-05'),
('STU2026002', '李佳怡', '信息工程学院', '数据科学', '2023级', 'DS 2302', '数据分析师', '数据可视化、商业分析', 3.08, 88, 0, 78, 82, 55, '需要关注', 'watch', 62, '无挂科，但近期出勤率下降，学习平台活跃度偏低。', '建议辅导员进行学习习惯提醒，并跟踪未来两周出勤。', '待确认', '2026-06-05'),
('STU2026003', '陈思琪', '计算机学院', '软件工程', '2023级', 'SE 2301', '待明确', '前端开发、产品设计', 3.21, 91, 0, 91, 89, 42, '正常', 'normal', 28, '学业表现稳定，但职业目标不够明确。', '建议导师帮助其明确成长方向，推荐参与课程项目。', '成长规划中', '2026-06-04')
on conflict (student_no) do nothing;

insert into cp_risk_warning (
    warning_code, title, student_no, student_name, risk_level, risk_key, risk_score, source,
    status, status_key, owner, deadline, counselor_note, mentor_plan, student_feedback
) values
('RW2026001', '张明远高风险学业预警', 'STU2026001', '张明远', '高风险', 'high', 86, 'Agent 风险分析', '帮扶中', 'active', '辅导员 / 专业导师', '2026-06-10', '已确认存在学业高风险，需进入重点帮扶。', '制定高等数学与数据结构 4 周补强计划。', '愿意参加每周辅导并提交学习记录。'),
('RW2026002', '李佳怡学习行为关注预警', 'STU2026002', '李佳怡', '需要关注', 'watch', 62, '学习行为监测', '待确认', 'todo', '辅导员', '2026-06-12', '待辅导员核实近期请假与课程参与情况。', '暂未进入导师帮扶。', '未反馈。')
on conflict (warning_code) do nothing;

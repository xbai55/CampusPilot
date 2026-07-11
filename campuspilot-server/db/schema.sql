create table if not exists cp_student_profile (
    student_no varchar(32) primary key,
    student_name varchar(64) not null,
    college varchar(128) not null,
    major varchar(128) not null,
    grade varchar(32) not null,
    class_name varchar(64) not null,
    career_goal varchar(128),
    interests varchar(255),
    gpa numeric(3,2),
    attendance int,
    failed_courses int,
    course_score int,
    behavior_score int,
    innovation_score int,
    risk_level varchar(32),
    risk_key varchar(32),
    risk_score int,
    profile text,
    suggestion text,
    support_status varchar(64),
    updated_at date
);

create table if not exists cp_course_score (
    id bigserial primary key,
    student_no varchar(32) references cp_student_profile(student_no),
    student_name varchar(64) not null,
    course_name varchar(128) not null,
    score int not null,
    course_type varchar(64),
    status varchar(64),
    suggestion text
);

create table if not exists cp_learning_behavior (
    id bigserial primary key,
    student_no varchar(32) references cp_student_profile(student_no),
    student_name varchar(64) not null,
    item varchar(128) not null,
    score int not null,
    trend varchar(64),
    evidence text,
    updated_at date
);

create table if not exists cp_risk_warning (
    warning_code varchar(32) primary key,
    title varchar(255) not null,
    student_no varchar(32) references cp_student_profile(student_no),
    student_name varchar(64) not null,
    risk_level varchar(32),
    risk_key varchar(32),
    risk_score int,
    source varchar(128),
    status varchar(64),
    status_key varchar(32),
    owner varchar(128),
    deadline date,
    counselor_note text,
    mentor_plan text,
    student_feedback text,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);

create table if not exists cp_workflow_log (
    id bigserial primary key,
    warning_code varchar(32) references cp_risk_warning(warning_code),
    log_time timestamp default current_timestamp,
    actor varchar(128),
    step varchar(128),
    detail text
);

create table if not exists cp_audit_log (
    id bigserial primary key,
    log_time timestamp default current_timestamp,
    actor varchar(128),
    action text
);

create index if not exists idx_cp_student_profile_risk_key on cp_student_profile(risk_key);
create index if not exists idx_cp_risk_warning_status_key on cp_risk_warning(status_key);
create index if not exists idx_cp_workflow_log_warning_code on cp_workflow_log(warning_code);

DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'ADMIN' CHECK(role IN('ADMIN', 'STUDENT', 'TEACHER')),
    student_id INTEGER REFERENCES students(id),
    teacher_id INTEGER REFERENCES teachers(id)
);

DROP TABLE IF EXISTS persons;

CREATE TABLE persons (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    dni TEXT NOT NULL UNIQUE,
    firstName TEXT NOT NULL,
    lastName TEXT NOT NULL,
    phone TEXT NOT NULL,
    email TEXT NOT NULL
);

DROP TABLE IF EXISTS teachers;

CREATE TABLE teachers (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    id_persona INTEGER NOT NULL,
    nroLegajo VARCHAR(30) NOT NULL UNIQUE,
    FOREIGN KEY (id_persona) REFERENCES persons(id)
);

DROP TABLE IF EXISTS students;

CREATE TABLE students (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    id_person INTEGER NOT NULL,
    student_type TEXT NOT NULL,
    FOREIGN KEY (id_person) REFERENCES persons(id)
);

DROP TABLE IF EXISTS subjects;

DROP TABLE IF EXISTS study_plans;

CREATE TABLE study_plans (
    id_study_plan INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    year INTEGER NOT NULL CHECK(year > 0),
    id_career INTEGER NOT NULL,
    FOREIGN KEY (id_career) REFERENCES careers(id_careers)
);

CREATE TABLE subjects (
    id_subject INTEGER PRIMARY KEY AUTOINCREMENT,
    code TEXT NOT NULL UNIQUE,
    subject_name TEXT NOT NULL,
    id_study_plan INTEGER,
    FOREIGN KEY (id_study_plan) REFERENCES study_plans(id_study_plan)
);

DROP TABLE IF EXISTS careers;

CREATE TABLE careers (
    id_careers INTEGER PRIMARY KEY AUTOINCREMENT,
    career_name TEXT NOT NULL,
    career_duration INTEGER NOT NULL CHECK(career_duration > 0)
);

DROP TABLE IF EXISTS teacher_subject;

CREATE TABLE teacher_subject (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    teacher_id INTEGER NOT NULL,
    subject_id INTEGER NOT NULL,
    FOREIGN KEY (teacher_id) REFERENCES teachers(id),
    FOREIGN KEY (subject_id) REFERENCES subjects(id_subject)
);

DROP TABLE IF EXISTS enrollments;

CREATE TABLE enrollments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    student_id INTEGER NOT NULL,
    subject_id INTEGER NOT NULL,
    period TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'ENROLLED' CHECK(status IN('ENROLLED', 'DROPPED', 'COMPLETED')),
    created_at TEXT NOT NULL,
    FOREIGN KEY (student_id) REFERENCES students(id),
    FOREIGN KEY (subject_id) REFERENCES subjects(id_subject),
    UNIQUE(student_id, subject_id, period)
);

CREATE TABLE evaluations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    enrollment_id INTEGER NOT NULL UNIQUE,
    grade DECIMAL(4,2) NOT NULL,
    evaluation_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (enrollment_id) REFERENCES enrollments(id),
    CHECK(grade >= 0 AND grade <= 10)
);

DROP TABLE IF EXISTS conditions;

CREATE TABLE conditions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    subject_id INTEGER NOT NULL REFERENCES subjects(id_subject),
    prerequisite_subject_id INTEGER NOT NULL REFERENCES subjects(id_subject),
    type VARCHAR(20) NOT NULL DEFAULT 'REGULAR',
    CHECK(type IN ('REGULAR', 'APROBADA')),
    CHECK(subject_id != prerequisite_subject_id)
);

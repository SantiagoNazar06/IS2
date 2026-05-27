DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'ADMIN' CHECK(role IN('ADMIN', 'STUDENT', 'TEACHER')),
    student_id INTEGER REFERENCES students(id)
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

CREATE TABLE subjects (
    id_subject INTEGER PRIMARY KEY AUTOINCREMENT,
    subject_name TEXT NOT NULL
);

DROP TABLE IF EXISTS careers;

CREATE TABLE careers (
    id_careers INTEGER PRIMARY KEY AUTOINCREMENT,
    career_name TEXT NOT NULL,
    career_duration INTEGER NOT NULL CHECK(career_duration > 0)
);

DROP TABLE IF EXISTS evaluations;

CREATE TABLE evaluations (
    id_evaluations INTEGER PRIMARY KEY AUTOINCREMENT,
    student_id INTEGER NOT NULL,
    subject_id INTEGER NOT NULL,
    evaluation_date DATE NOT NULL,
    evaluation_note INTEGER,
    condition_type TEXT CHECK(condition_type IN('aprobado', 'regular', 'desaprobado')),
    FOREIGN KEY (student_id) REFERENCES students(id),
    FOREIGN KEY (subject_id) REFERENCES subjects(id_subject)
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

DROP TABLE IF EXISTS teacher_assignments;

CREATE TABLE teacher_assignments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    teacher_id INTEGER NOT NULL REFERENCES teachers(id),
    subject_id INTEGER NOT NULL REFERENCES subjects(id_subject),
    role VARCHAR(30) NOT NULL DEFAULT 'RESPONSABLE' CHECK(role IN('RESPONSABLE', 'JTP', 'AYUDANTE')),
    period TEXT NOT NULL,
    UNIQUE(teacher_id, subject_id, period)
);

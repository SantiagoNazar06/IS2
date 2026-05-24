-- Elimina la tabla 'users' si ya existe para asegurar un inicio limpio
DROP TABLE IF EXISTS users;

-- Crea la tabla 'users' con los campos originales, adaptados para SQLite
CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT, -- Clave primaria autoincremental para SQLite
    name TEXT NOT NULL UNIQUE,          -- Nombre de usuario (TEXT es el tipo de cadena recomendado para SQLite), con restricción UNIQUE
    password TEXT NOT NULL           -- Contraseña hasheada (TEXT es el tipo de cadena recomendado para SQLite)
);

-- Elimina la tabla 'persons' si ya existe para asegurar un inicio limpio
DROP TABLE IF EXISTS persons;

-- Crea la tabla 'persons' con los campos originales, adaptados para SQLite
CREATE TABLE persons (
    id INTEGER PRIMARY KEY AUTOINCREMENT, -- Clave primaria autoincremental para SQLite
    dni TEXT NOT NULL UNIQUE,
    firstName TEXT NOT NULL,          -- Nombre de usuario (TEXT es el tipo de cadena recomendado para SQLite), con restricción UNIQUE
    lastName TEXT NOT NULL,
    phone TEXT NOT NULL,
    email TEXT NOT NULL
);


-- Elimina la tabla 'teachers' si ya existe para asegurar un inicio limpio
DROP TABLE IF EXISTS teachers;

-- Crea la tabla 'teachers' con los campos originales, adaptados para SQLite
CREATE TABLE teachers (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    id_persona INTEGER NOT NULL,
    nroLegajo VARCHAR(30) NOT NULL UNIQUE,
    FOREIGN KEY (id_persona) REFERENCES persons(id)
);


-- Elimina la tabla 'students' si ya existe para asegurar un inicio limpio
DROP TABLE IF EXISTS students;

-- Crea la tabla 'students' con los campos originales, adaptados para SQLite
CREATE TABLE students (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    id_person INTEGER NOT NULL,
    student_type TEXT NOT NULL,
    FOREIGN KEY (id_person) REFERENCES persons(id)
);

CREATE TABLE subjects (
    id_subject INTEGER PRIMARY KEY AUTOINCREMENT,
    subject_name TEXT NOT NULL
);

CREATE TABLE careers (
    id_careers INTEGER PRIMARY KEY AUTOINCREMENT,
    career_name TEXT NOT NULL,
    career_duration INTEGER NOT NULL CHECK(career_duration > 0)
);

DROP TABLE IF EXISTS enrollments;
CREATE TABLE enrollments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    student_id INTEGER NOT NULL,
    subject_id INTEGER NOT NULL,
    enrollment_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students(id),
    FOREIGN KEY (subject_id) REFERENCES subjects(id_subject),
    UNIQUE(student_id, subject_id) 
);


CREATE TABLE evaluations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    enrollment_id INTEGER NOT NULL UNIQUE,
    grade DECIMAL(4,2) NOT NULL,
    evaluation_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (enrollment_id) REFERENCES enrollments(id),
    CHECK(grade >= 0 AND grade <= 10)
);

CREATE TABLE conditions (
    id_condition INTEGER PRIMARY KEY AUTOINCREMENT,
    condition_type TEXT CHECK(condition_type IN('aprobado', 'desaprobado'))
)
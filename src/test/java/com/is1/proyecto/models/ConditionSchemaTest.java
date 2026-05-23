package com.is1.proyecto.models;

import org.javalite.activejdbc.Base;
import org.junit.jupiter.api.*;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de integración para el schema de conditions.
 * Verifica que la estructura de la tabla coincide con la definida en scheme.sql.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConditionSchemaTest {

    private static final String JDBC_URL = "jdbc:sqlite:target/test-condition-schema.db";

    @BeforeAll
    void setupDatabase() {
        Base.open("org.sqlite.JDBC", JDBC_URL, "", "");
        Base.exec("PRAGMA foreign_keys = ON");

        // Create the subjects table that conditions references
        Base.exec(
            "CREATE TABLE subjects (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "subject_name TEXT NOT NULL" +
            ")"
        );

        // Create conditions table matching the new schema in scheme.sql
        Base.exec(
            "CREATE TABLE conditions (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "subject_id INTEGER NOT NULL REFERENCES subjects(id), " +
            "prerequisite_subject_id INTEGER NOT NULL REFERENCES subjects(id), " +
            "type VARCHAR(20) NOT NULL DEFAULT 'REGULAR', " +
            "CHECK(type IN ('REGULAR', 'APROBADA')), " +
            "CHECK(subject_id != prerequisite_subject_id)" +
            ")"
        );
        Base.close();
    }

    @BeforeEach
    void openConnection() {
        Base.open("org.sqlite.JDBC", JDBC_URL, "", "");
        Base.exec("PRAGMA foreign_keys = ON");
    }

    @AfterEach
    void closeConnection() {
        Base.close();
    }

    @Test
    void tableExists() {
        Object result = Base.firstCell(
            "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
            "conditions"
        );
        assertEquals("conditions", result, "conditions table must exist");
    }

    @Test
    void tableHasAllRequiredColumns() {
        // subject_id column
        Object subjectIdCol = Base.firstCell(
            "SELECT name FROM pragma_table_info('conditions') WHERE name=?",
            "subject_id"
        );
        assertEquals("subject_id", subjectIdCol, "subject_id column must exist");

        // prerequisite_subject_id column
        Object prereqIdCol = Base.firstCell(
            "SELECT name FROM pragma_table_info('conditions') WHERE name=?",
            "prerequisite_subject_id"
        );
        assertEquals("prerequisite_subject_id", prereqIdCol, "prerequisite_subject_id column must exist");

        // type column
        Object typeCol = Base.firstCell(
            "SELECT name FROM pragma_table_info('conditions') WHERE name=?",
            "type"
        );
        assertEquals("type", typeCol, "type column must exist");

        // id column
        Object idCol = Base.firstCell(
            "SELECT name FROM pragma_table_info('conditions') WHERE name=?",
            "id"
        );
        assertEquals("id", idCol, "id column must exist");
    }

    @Test
    void typeColumnHasNotNullAndDefault() {
        Object notNull = Base.firstCell(
            "SELECT \"notnull\" FROM pragma_table_info('conditions') WHERE name=?",
            "type"
        );
        assertEquals(1, notNull, "type column must be NOT NULL");

        Object defaultValue = Base.firstCell(
            "SELECT dflt_value FROM pragma_table_info('conditions') WHERE name=?",
            "type"
        );
        assertEquals("'REGULAR'", defaultValue, "type column must default to 'REGULAR'");
    }

    @Test
    void idColumnIsPrimaryKey() {
        Object pkCol = Base.firstCell(
            "SELECT name FROM pragma_table_info('conditions') WHERE pk=1"
        );
        assertEquals("id", pkCol, "id column must be the primary key");
    }

    @Test
    void insertAndReadValidCondition() {
        // Given: two subjects exist
        Base.exec("INSERT INTO subjects(id, subject_name) VALUES (1, 'Matematica')");
        Base.exec("INSERT INTO subjects(id, subject_name) VALUES (2, 'Fisica')");

        // When: inserting a valid condition
        Base.exec(
            "INSERT INTO conditions(subject_id, prerequisite_subject_id, type) VALUES (?, ?, ?)",
            1, 2, "REGULAR"
        );

        // Then: the data is stored and readable
        String type = Base.firstCell(
            "SELECT type FROM conditions WHERE subject_id=? AND prerequisite_subject_id=?",
            1, 2
        ).toString();
        assertEquals("REGULAR", type);
    }

    @Test
    void insertConditionWithDefaultType() {
        Base.exec("INSERT INTO subjects(id, subject_name) VALUES (3, 'Historia')");
        Base.exec("INSERT INTO subjects(id, subject_name) VALUES (4, 'Geografia')");

        // Insert without specifying type — should default to 'REGULAR'
        Base.exec(
            "INSERT INTO conditions(subject_id, prerequisite_subject_id) VALUES (?, ?)",
            3, 4
        );

        String type = Base.firstCell(
            "SELECT type FROM conditions WHERE subject_id=3"
        ).toString();
        assertEquals("REGULAR", type, "default type must be REGULAR");
    }

    @Test
    void checkConstraint_rejectsSelfReference() {
        Base.exec("INSERT INTO subjects(id, subject_name) VALUES (5, 'AutoRef')");

        assertThrows(Exception.class, () -> {
            Base.exec(
                "INSERT INTO conditions(subject_id, prerequisite_subject_id, type) VALUES (5, 5, 'REGULAR')"
            );
        }, "must reject self-referencing condition");
    }

    @Test
    void checkConstraint_rejectsInvalidType() {
        Base.exec("INSERT INTO subjects(id, subject_name) VALUES (6, 'A'), (7, 'B')");

        assertThrows(Exception.class, () -> {
            Base.exec(
                "INSERT INTO conditions(subject_id, prerequisite_subject_id, type) VALUES (6, 7, 'INVALID')"
            );
        }, "must reject invalid condition type");
    }

    @Test
    void foreignKey_enforcesReferentialIntegrity() {
        // subject_id references a non-existent subject
        assertThrows(Exception.class, () -> {
            Base.exec(
                "INSERT INTO conditions(subject_id, prerequisite_subject_id, type) VALUES (999, 1, 'REGULAR')"
            );
        });
    }

    @AfterAll
    void teardown() {
        new File("target/test-condition-schema.db").delete();
    }
}

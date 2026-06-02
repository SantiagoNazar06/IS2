package com.is1.proyecto.dto;

public class StudentWithGradeDTO {

    private Long studentId;
    private String studentName;
    private String enrollmentDate;
    private Double grade;
    private String gradeDate;

    public StudentWithGradeDTO(Long studentId, String studentName, String enrollmentDate, Double grade, String gradeDate){
        this.studentId = studentId;
        this.studentName = studentName;
        this.enrollmentDate = enrollmentDate;
        this.grade = grade;
        this.gradeDate = gradeDate;
    }

    public Long getStudentId() {
        return studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public String getEnrollmentDate() {
        return enrollmentDate;
    }

    public Double getGrade() {
        return grade;
    }

    public String getGradeDate() {
        return gradeDate;
    }
    
}

package com.is1.proyecto.dto;

public class GradeDTO {

    private final String subjectName;
    private final Double grade;
    private final String date;

    public GradeDTO(String subjectName, Double grade, String date) {
        this.subjectName = subjectName;
        this.grade = grade;
        this.date = date;
    }

    public String getSubjectName() { return subjectName; }
    public Double getGrade() { return grade; }
    public String getDate() { return date; }
}

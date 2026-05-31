package com.is1.proyecto.dto;

public class SubjectDTO {

    private final Long subjectId;
    private final String subjectName;
    private final String status;
    private final Double grade;
    private final String period;

    public SubjectDTO(Long subjectId, String subjectName, String status, Double grade, String period) {
        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.status = status;
        this.grade = grade;
        this.period = period;
    }

    public Long getSubjectId() { return subjectId; }
    public String getSubjectName() { return subjectName; }
    public String getStatus() { return status; }
    public Double getGrade() { return grade; }
    public String getPeriod() { return period; }
}

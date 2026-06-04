package com.is1.proyecto.dto;

public class EnrollmentDTO {

    private final Long subjectId;
    private final String status;
    private final Double grade;
    private final String period;

    public EnrollmentDTO(Long subjectId, String status, Double grade, String period) {
        this.subjectId = subjectId;
        this.status = status;
        this.grade = grade;
        this.period = period;
    }

    public Long getSubjectId() { return subjectId; }
    public String getStatus() { return status; }
    public Double getGrade() { return grade; }
    public String getPeriod() { return period; }
}

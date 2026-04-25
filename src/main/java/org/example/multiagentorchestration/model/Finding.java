package org.example.multiagentorchestration.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Finding {

    public enum Confidence {
        HIGH,
        MEDIUM,
        LOW
    }

    private String claim;
    @JsonProperty("source_url")
    private String sourceUrl;
    @JsonProperty("document_name")
    private String documentName;
    @JsonProperty("page_number")
    private Integer pageNumber;
    private Confidence confidence;
    @JsonProperty("retrieved_by")
    private String retrievedBy;

    public Finding() {
    }

    public Finding(String claim, String sourceUrl, String documentName,
                   Integer pageNumber, Confidence confidence, String retrievedBy) {
        this.claim = claim;
        this.sourceUrl = sourceUrl;
        this.documentName = documentName;
        this.pageNumber = pageNumber;
        this.confidence = confidence;
        this.retrievedBy = retrievedBy;
    }

    public String getClaim() {
        return claim;
    }

    public void setClaim(String claim) {
        this.claim = claim;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public Confidence getConfidence() {
        return confidence;
    }

    public void setConfidence(Confidence confidence) {
        this.confidence = confidence;
    }

    public String getRetrievedBy() {
        return retrievedBy;
    }

    public void setRetrievedBy(String retrievedBy) {
        this.retrievedBy = retrievedBy;
    }

    @Override
    public String toString() {
        return "Finding{" +
                "claim='" + claim + '\'' +
                ", sourceUrl='" + sourceUrl + '\'' +
                ", documentName='" + documentName + '\'' +
                ", pageNumber=" + pageNumber +
                ", confidence=" + confidence +
                ", retrievedBy='" + retrievedBy + '\'' +
                '}';
    }
}
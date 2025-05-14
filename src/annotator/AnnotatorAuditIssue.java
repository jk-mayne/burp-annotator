package annotator;

import burp.api.montoya.scanner.audit.issues.AuditIssue;
import burp.api.montoya.scanner.audit.issues.AuditIssueConfidence;
import burp.api.montoya.scanner.audit.issues.AuditIssueSeverity;
import burp.api.montoya.scanner.audit.issues.AuditIssueDefinition;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.collaborator.Interaction;
import burp.api.montoya.http.HttpService;


import java.util.Collections;
import java.util.List;

public class AnnotatorAuditIssue implements AuditIssue {
    private final String name;
    private final String detail;
    private final String remediation;
    private final HttpRequestResponse requestResponse;
    private final AuditIssueSeverity severity;
    private final AuditIssueConfidence confidence;
    private final HttpService httpService;

    public AnnotatorAuditIssue(
            String name,
            String remediation,
            String detail,
            HttpRequestResponse requestResponse,
            AuditIssueSeverity severity,
            AuditIssueConfidence confidence) {
        this.name = name;
        this.remediation = remediation;
        this.detail = detail;
        this.requestResponse = requestResponse;
        this.severity = severity;
        this.confidence = confidence;
        this.httpService = requestResponse.httpService();
    }

    @Override
    public HttpService httpService() {
        return httpService;
    }

    @Override 
    public List<HttpRequestResponse> requestResponses() {
        return List.of(requestResponse);
    }

    @Override
    public List<Interaction> collaboratorInteractions() {
        return Collections.emptyList();
    }
  
    @Override
    public String name() {
        return name;
    }

    @Override
    public String detail() {
        return detail;
    }

    @Override
    public String remediation() {
        return null;
    }

    @Override
    public AuditIssueSeverity severity() {
        return severity;
    }

    @Override
    public AuditIssueConfidence confidence() {
        return AuditIssueConfidence.CERTAIN;
    }


    @Override
    public String baseUrl() {
        return requestResponse.request().url().toString();
    }

    @Override
    public AuditIssueDefinition definition() {
        return AuditIssueDefinition.auditIssueDefinition(
            name,
            detail,
            remediation,
            severity
        );
    }
}
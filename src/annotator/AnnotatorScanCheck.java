package annotator;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.scanner.ScanCheck;
import burp.api.montoya.scanner.AuditResult;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.scanner.audit.issues.AuditIssue;
import burp.api.montoya.scanner.audit.issues.AuditIssueConfidence;
import burp.api.montoya.scanner.audit.issues.AuditIssueSeverity;
import burp.api.montoya.scanner.ConsolidationAction;
import burp.api.montoya.scanner.audit.insertionpoint.AuditInsertionPoint;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class AnnotatorScanCheck implements ScanCheck {
    private final MontoyaApi api;
    private final Map<String, String> annotations;
    private final ScannedUrlsPanel scannedUrlsPanel;
    private static final String SCANNED_ISSUE_NAME = "[Annotator] Active Scanned";
    private static final String SCANNED_ISSUE_DETAIL = "This URL has been actively scanned.";
    private static final String SCANNED_ISSUE_REMEDIATION = "This URL has been actively scanned.";
    public AnnotatorScanCheck(MontoyaApi api, Map<String, String> annotations, ScannedUrlsPanel scannedUrlsPanel) {
        this.api = api;
        this.annotations = annotations;
        this.scannedUrlsPanel = scannedUrlsPanel;
    }

    @Override
    public AuditResult activeAudit(HttpRequestResponse baseRequestResponse, AuditInsertionPoint insertionPoint) {
        try {
            String url = baseRequestResponse.request().url().toString();
            
            // Check if URL exists in the panel
            if (scannedUrlsPanel.hasUrl(url)) {
                // If URL exists but doesn't have Scanned tag, add it
                if (!scannedUrlsPanel.hasScannedTag(url)) {
                    scannedUrlsPanel.addScannedUrl(url, "Scanned");
                }
                // If URL exists and has Scanned tag, just return without creating a new issue
                return AuditResult.auditResult();
            }
            
            // Create an informational issue for the scanned URL
            AuditIssue issue = new AnnotatorAuditIssue(
                SCANNED_ISSUE_NAME,
                SCANNED_ISSUE_DETAIL,
                SCANNED_ISSUE_REMEDIATION,
                baseRequestResponse,
                AuditIssueSeverity.INFORMATION,
                AuditIssueConfidence.CERTAIN
            );

            // Add to our annotations
            annotations.put(url, "Scanned (active scan)");
            
            // Update the scanned URLs panel
            scannedUrlsPanel.addScannedUrl(url, "Scanned");
            
            return AuditResult.auditResult(issue);
        } catch (Exception e) {
            return AuditResult.auditResult();
        }
    }

    @Override
    public AuditResult passiveAudit(HttpRequestResponse baseRequestResponse) {
        return AuditResult.auditResult();
    }

    @Override
    public ConsolidationAction consolidateIssues(AuditIssue issue1, AuditIssue issue2) {
        return ConsolidationAction.KEEP_EXISTING;
    }
} 
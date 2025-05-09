import burp.api.montoya.MontoyaApi;
import burp.api.montoya.BurpExtension;
import burp.api.montoya.extension.ExtensionUnloadingHandler;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.scanner.audit.issues.AuditIssue;
import burp.api.montoya.scanner.audit.issues.AuditIssueConfidence;
import burp.api.montoya.scanner.audit.issues.AuditIssueSeverity;
import burp.api.montoya.scanner.audit.issues.AuditIssueDefinition;
import burp.api.montoya.scanner.AuditConfiguration;
import burp.api.montoya.scanner.BuiltInAuditConfiguration;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import burp.api.montoya.ui.editor.extension.ExtensionProvidedHttpRequestEditor;
import burp.api.montoya.ui.editor.extension.EditorCreationContext;
import burp.api.montoya.ui.editor.extension.HttpRequestEditorProvider;
import burp.api.montoya.core.Registration;
import burp.api.montoya.ui.Selection;
import burp.api.montoya.scanner.audit.AuditIssueHandler;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class AnnotatorExtension implements BurpExtension {

    private MontoyaApi api;
    private final Map<String, String> annotations = new ConcurrentHashMap<>();
    private static final String SCANNED_ISSUE_NAME = "URL Scanned by Annotator";
    private static final String SCANNED_ISSUE_DETAIL = "This URL has been actively scanned.";

    @Override
    public void initialize(MontoyaApi api) {
        this.api = api;
        api.extension().setName("Sitemap Annotator");

        api.userInterface().registerContextMenuItemsProvider(new AnnotatorContextMenu());
        api.userInterface().registerHttpRequestEditorProvider(new AnnotatorEditorProvider());
        
        // Register audit issue handler
        api.scanner().registerAuditIssueHandler(new AnnotatorAuditIssueHandler());
    }

    public boolean isBapp() {
        return false;
    }

    public String filename() {
        return "SitemapAnnotator.jar";
    }

    public void setName(String name) {
        api.extension().setName(name);
    }

    public Registration registerUnloadingHandler(ExtensionUnloadingHandler handler) {
        Registration registration = api.extension().registerUnloadingHandler(handler);
        return registration;
    }

    private class AnnotatorContextMenu implements ContextMenuItemsProvider {
        @Override
        public List<Component> provideMenuItems(ContextMenuEvent event) {
            JMenuItem markScanned = new JMenuItem("Mark as Scanned");

            markScanned.addActionListener(e -> {
                for (HttpRequestResponse message : event.selectedRequestResponses()) {
                    try {
                        String url = message.request().url().toString();
                        String normalized = normalizeUrl(new URL(url));
                        annotations.put(normalized, "Scanned (manual)");
                        JOptionPane.showMessageDialog(null, "Marked as scanned: " + normalized);
                    } catch (Exception ex) {
                        api.logging().logToError("Failed to annotate: " + ex.getMessage());
                    }
                }
            });

            return Collections.singletonList(markScanned);
        }
    }

    private class AnnotatorEditorProvider implements HttpRequestEditorProvider {
        @Override
        public ExtensionProvidedHttpRequestEditor provideHttpRequestEditor(EditorCreationContext context) {
            return new AnnotatorEditor();
        }
    }

    private class AnnotatorEditor implements ExtensionProvidedHttpRequestEditor {
        private final JTextArea textArea;
        private final JPanel panel;
        private HttpRequestResponse message;

        public AnnotatorEditor() {
            this.textArea = new JTextArea();
            this.textArea.setEditable(false);
            this.panel = new JPanel(new BorderLayout());
            panel.add(new JScrollPane(textArea), BorderLayout.CENTER);
        }

        @Override
        public void setRequestResponse(HttpRequestResponse requestResponse) {
            this.message = requestResponse;
            updateAnnotation();
        }

        @Override
        public HttpRequest getRequest() {
            return message != null ? message.request() : null;
        }

        private void updateAnnotation() {
            if (message == null) {
                textArea.setText("No request/response available");
                return;
            }

            try {
                String url = message.request().url().toString();
                String normalized = normalizeUrl(new URL(url));

                String annotation = annotations.get(normalized);
                if (annotation != null) {
                    textArea.setText("Annotation: " + annotation);
                    return;
                }

                // Check for our custom audit issue
                boolean found = api.siteMap().issues().stream()
                    .anyMatch(issue -> {
                        try {
                            return issue.name().equals(SCANNED_ISSUE_NAME) && 
                                normalizeUrl(new URL(issue.baseUrl())).equals(normalized);
                        } catch (Exception e) {
                            api.logging().logToError("Error checking issue URL: " + e.getMessage());
                            return false;
                        }
                    });

                textArea.setText("Annotation: " + (found ? "Scanned (active scan)" : "Not Scanned"));

            } catch (Exception e) {
                textArea.setText("Annotation: Error processing URL");
                api.logging().logToError("Annotation error: " + e.getMessage());
            }
        }

        @Override
        public String caption() {
            return "Annotation";
        }

        @Override
        public Component uiComponent() {
            return panel;
        }

        @Override
        public boolean isEnabledFor(HttpRequestResponse httpRequestResponse) {
            return true;
        }

        @Override
        public boolean isModified() {
            return false;
        }

        @Override
        public Selection selectedData() {
            return null;
        }
    }

    private static String normalizeUrl(URL url) {
        try {
            int port = url.getPort();
            boolean isDefaultPort = (url.getProtocol().equals("http") && port == 80)
                    || (url.getProtocol().equals("https") && port == 443)
                    || (port == -1);

            if (isDefaultPort) {
                return url.getProtocol() + "://" + url.getHost() + url.getPath();
            } else {
                return url.getProtocol() + "://" + url.getHost() + ":" + port + url.getPath();
            }
        } catch (Exception e) {
            return url.toString(); // fallback
        }
    }

    private class AnnotatorAuditIssueHandler implements AuditIssueHandler {
        @Override
        public void handleNewAuditIssue(AuditIssue issue) {
            // This will be called when a new audit issue is found
            try {
                String url = issue.baseUrl();
                String normalized = normalizeUrl(new URL(url));
                
                // Add to our annotations
                annotations.put(normalized, "Scanned (active scan)");
                
                // Log that we found an issue
                api.logging().logToOutput("Found audit issue for URL: " + normalized);
                
            } catch (Exception e) {
                api.logging().logToError("Failed to handle audit issue: " + e.getMessage());
            }
        }
    }
}
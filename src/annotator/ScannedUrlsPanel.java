package annotator;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class ScannedUrlsPanel extends JPanel {
    private final UrlTableModel tableModel;
    private final JTable urlTable;
    private static final String[] AVAILABLE_TAGS = {"Scanned", "Param Miner", "XSS", "SQLi", "Custom Tag"};

    public ScannedUrlsPanel() {
        setLayout(new BorderLayout());
        
        // Create the table model
        tableModel = new UrlTableModel();
        urlTable = new JTable(tableModel);
        
        // Set column widths
        urlTable.getColumnModel().getColumn(0).setPreferredWidth(400);
        urlTable.getColumnModel().getColumn(1).setPreferredWidth(200);
        
        // Set custom renderer for the tags column
        urlTable.getColumnModel().getColumn(1).setCellRenderer(new TagRenderer());
        
        // Set custom editor for the tags column
        urlTable.getColumnModel().getColumn(1).setCellEditor(new TagEditor());
        
        // Add a scroll pane for the table
        JScrollPane scrollPane = new JScrollPane(urlTable);
        add(scrollPane, BorderLayout.CENTER);
        
        // Add a label at the top
        JLabel titleLabel = new JLabel("URLs Marked as Scanned");
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(titleLabel, BorderLayout.NORTH);
    }

    public void addScannedUrl(String url) {
        tableModel.addUrl(url);
    }

    // Custom table model
    private class UrlTableModel extends AbstractTableModel {
        private final List<UrlEntry> urls = new ArrayList<>();
        private final String[] columnNames = {"URL", "Tags"};

        @Override
        public int getRowCount() {
            return urls.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            UrlEntry entry = urls.get(rowIndex);
            return columnIndex == 0 ? entry.url : entry;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 1; // Only the tags column is editable
        }

        public void addUrl(String url) {
            // Check if URL already exists
            for (UrlEntry entry : urls) {
                if (entry.url.equals(url)) {
                    return;
                }
            }
            urls.add(new UrlEntry(url));
            fireTableRowsInserted(urls.size() - 1, urls.size() - 1);
        }

        public void toggleTag(int row, String tag) {
            urls.get(row).toggleTag(tag);
            fireTableRowsUpdated(row, row);
        }
    }

    // Custom cell renderer for tags
    private class TagRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
            panel.setOpaque(true);
            
            if (isSelected) {
                panel.setBackground(table.getSelectionBackground());
            } else {
                panel.setBackground(table.getBackground());
            }

            if (value instanceof UrlEntry) {
                UrlEntry entry = (UrlEntry) value;
                
                // Add all tags including Scanned if applicable
                for (String tag : entry.getAllTags()) {
                    Color tagColor = getTagColor(tag);
                    JLabel tagLabel = createTag(tag, tagColor);
                    panel.add(tagLabel);
                }
            }
            
            return panel;
        }

        private JLabel createTag(String text, Color color) {
            JLabel tag = new JLabel(text);
            tag.setOpaque(true);
            tag.setBackground(color);
            tag.setForeground(Color.WHITE);
            tag.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
            tag.setFont(tag.getFont().deriveFont(Font.BOLD));
            return tag;
        }

        private Color getTagColor(String tag) {
            return switch (tag) {
                case "Scanned" -> new Color(76, 175, 80);  // Green
                case "Param Miner" -> new Color(33, 150, 243);  // Blue
                case "XSS" -> new Color(244, 67, 54);          // Red
                case "SQLi" -> new Color(255, 152, 0);         // Orange
                default -> new Color(156, 39, 176);            // Purple
            };
        }
    }

    // Custom cell editor for tags
    private class TagEditor extends AbstractCellEditor implements TableCellEditor {
        private final JComboBox<String> comboBox;
        private UrlEntry currentEntry;

        public TagEditor() {
            comboBox = new JComboBox<>(AVAILABLE_TAGS);
            comboBox.setEditable(false); // Prevent custom tag input
            comboBox.addActionListener(e -> {
                String selectedTag = (String) comboBox.getSelectedItem();
                if (selectedTag != null && !selectedTag.isEmpty() && currentEntry != null) {
                    tableModel.toggleTag(urlTable.getSelectedRow(), selectedTag);
                }
                stopCellEditing();
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            currentEntry = (UrlEntry) value;
            return comboBox;
        }

        @Override
        public Object getCellEditorValue() {
            return currentEntry;
        }
    }

    // Class to hold URL and its tags
    private static class UrlEntry {
        final String url;
        final Set<String> tags;
        boolean isScanned;

        UrlEntry(String url) {
            this.url = url;
            this.tags = new HashSet<>();
            this.isScanned = false; // Don't set as scanned by default
        }

        void toggleTag(String tag) {
            if (tag.equals("Scanned")) {
                isScanned = !isScanned;
            } else {
                if (tags.contains(tag)) {
                    tags.remove(tag);
                } else {
                    tags.add(tag);
                }
            }
        }

        Set<String> getAllTags() {
            Set<String> allTags = new HashSet<>(tags);
            if (isScanned) {
                allTags.add("Scanned");
            }
            return allTags;
        }
    }
} 
package com.ourgiant.worldclock;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialog for managing World Clock preferences (timezones and display options).
 */
public class PreferencesDialog extends JDialog {
    private List<JComboBox<String>> timezoneSelectors;
    private JCheckBox displaySecondsCheckbox;
    private JPasswordField apiNinjaKeyField;
    private JTextField currentLocationField;
    private boolean confirmed;
    private PreferencesListener listener;

    public interface PreferencesListener {
        void onPreferencesChanged(List<String> timeZones, boolean displaySeconds, String apiNinjaKey, String currentLocation);
    }

    public PreferencesDialog(JFrame parent, List<String> currentTimeZones, boolean currentDisplaySeconds, String currentLocation, PreferencesListener listener) {
        super(parent, "Preferences", true);
        this.listener = listener;
        this.timezoneSelectors = new ArrayList<>();
        this.confirmed = false;

        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setSize(1000, 300);
        setLocationRelativeTo(parent);
        setResizable(false);

        initializeUI(currentTimeZones, currentDisplaySeconds, currentLocation);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                confirmed = false;
            }
        });
    }

    private void initializeUI(List<String> currentTimeZones, boolean currentDisplaySeconds, String currentLocation) {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(new Color(240, 240, 240));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Title
        JLabel title = new JLabel("World Clock Preferences", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 18));
        mainPanel.add(title, BorderLayout.NORTH);

        // Content panel with scroll support
        JPanel contentPanel = new JPanel(new GridLayout(3, 1, 10, 10));
        contentPanel.setBackground(new Color(240, 240, 240));

        // Timezone section
        JPanel timezonePanel = createTimezonePanel(currentTimeZones);
        contentPanel.add(timezonePanel);

        // Current Location section
        JPanel currentLocationPanel = createCurrentLocationPanel(currentLocation);
        contentPanel.add(currentLocationPanel);


        // Display options section
        JPanel optionsPanel = createOptionsPanel(currentDisplaySeconds);
        contentPanel.add(optionsPanel);

        // API Ninja section
        JPanel apiPanel = createApiPanel();
        contentPanel.add(apiPanel);

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBackground(new Color(240, 240, 240));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setBackground(new Color(240, 240, 240));

        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> savePreferences());

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private JPanel createTimezonePanel(List<String> currentTimeZones) {
        JPanel panel = new JPanel(new GridLayout(3, 1, 5, 10));
        panel.setBackground(new Color(240, 240, 240));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                "Select Time Zones",
                0, 0,
                new Font("Arial", Font.BOLD, 12),
                Color.BLACK
        ));

        for (int i = 0; i < 3; i++) {
            JPanel rowPanel = new JPanel(new BorderLayout(10, 0));
            rowPanel.setBackground(new Color(240, 240, 240));

            JLabel label = new JLabel("Time Zone " + (i + 1) + ":");
            label.setPreferredSize(new Dimension(100, 25));

            JComboBox<String> comboBox = TimeZoneSelector.createTimeZoneComboBox();
            if (i < currentTimeZones.size()) {
                comboBox.setSelectedItem(currentTimeZones.get(i));
            }

            timezoneSelectors.add(comboBox);

            rowPanel.add(label, BorderLayout.WEST);
            rowPanel.add(comboBox, BorderLayout.CENTER);

            panel.add(rowPanel);
        }

        return panel;
    }
    
    private JPanel createCurrentLocationPanel(String currentLocation) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(240, 240, 240));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                "Current Location",
                0, 0,
                new Font("Arial", Font.BOLD, 12),
                Color.BLACK
        ));

        JLabel label = new JLabel("Current Location:");
        label.setFont(new Font("Arial", Font.PLAIN, 11));

        currentLocationField = new JTextField();
        currentLocationField.setText(currentLocation);
        currentLocationField.setFont(new Font("Arial", Font.PLAIN, 11));

        JLabel infoLabel = new JLabel("<html>Get your Current Lat/Long using Google Maps <a href=''>https://www.google.com/maps</a></html>");
        infoLabel.setFont(new Font("Arial", Font.ITALIC, 10));
        infoLabel.setForeground(new Color(100, 100, 100));

        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
        inputPanel.setBackground(new Color(240, 240, 240));
        inputPanel.add(label, BorderLayout.WEST);
        inputPanel.add(currentLocationField, BorderLayout.CENTER);
        panel.add(inputPanel, BorderLayout.NORTH);
        panel.add(infoLabel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createOptionsPanel(boolean currentDisplaySeconds) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(240, 240, 240));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                "Display Options",
                0, 0,
                new Font("Arial", Font.BOLD, 12),
                Color.BLACK
        ));

        displaySecondsCheckbox = new JCheckBox("Display seconds on clock", currentDisplaySeconds);
        displaySecondsCheckbox.setBackground(new Color(240, 240, 240));
        displaySecondsCheckbox.setFont(new Font("Arial", Font.PLAIN, 12));

        panel.add(displaySecondsCheckbox, BorderLayout.NORTH);

        return panel;
    }

    private JPanel createApiPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(240, 240, 240));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                "API Ninja Configuration",
                0, 0,
                new Font("Arial", Font.BOLD, 12),
                Color.BLACK
        ));

        JLabel label = new JLabel("API Key:");
        label.setFont(new Font("Arial", Font.PLAIN, 11));

        apiNinjaKeyField = new JPasswordField();
        apiNinjaKeyField.setText(PreferencesManager.loadApiNinjaKey());
        apiNinjaKeyField.setFont(new Font("Arial", Font.PLAIN, 11));

        JLabel infoLabel = new JLabel("<html>Get your free API key from <a href=''>api-ninjas.com</a></html>");
        infoLabel.setFont(new Font("Arial", Font.ITALIC, 10));
        infoLabel.setForeground(new Color(100, 100, 100));

        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
        inputPanel.setBackground(new Color(240, 240, 240));
        inputPanel.add(label, BorderLayout.WEST);
        inputPanel.add(apiNinjaKeyField, BorderLayout.CENTER);

        panel.add(inputPanel, BorderLayout.NORTH);
        panel.add(infoLabel, BorderLayout.CENTER);

        return panel;
    }

    private void savePreferences() {
        List<String> selectedTimeZones = new ArrayList<>();
        for (JComboBox<String> selector : timezoneSelectors) {
            selectedTimeZones.add((String) selector.getSelectedItem());
        }

        boolean displaySeconds = displaySecondsCheckbox.isSelected();
        String apiKey = apiNinjaKeyField.getText();
        String currentLocation = currentLocationField.getText();

        // Save preferences
        PreferencesManager.savePreferences(selectedTimeZones, displaySeconds, apiKey, currentLocation);

        confirmed = true;

        // Notify listener
        if (listener != null) {
            listener.onPreferencesChanged(selectedTimeZones, displaySeconds, apiKey, currentLocation);
        }

        dispose();
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}

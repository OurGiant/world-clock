package com.ourgiant.worldclock.gui;

import javax.swing.*;

import com.formdev.flatlaf.FlatDarkLaf;
import com.ourgiant.worldclock.core.HolidayService;
import com.ourgiant.worldclock.core.PreferencesManager;
import com.ourgiant.worldclock.core.WeatherService;

import java.awt.*;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Main entry point for the World Clock application.
 * Displays current time across multiple world zones using Swing.
 */
public class WorldClockApp {
    private DigitalClock utcClock;
    private List<DigitalClock> selectableClocks;
    private List<JComboBox<String>> timeZoneSelectors;
    private List<String> selectedTimeZones;
    private boolean displaySeconds;
    private String currentLocation;
    private Timer updateTimer;
    private JFrame frame;

    public WorldClockApp() {
        selectableClocks = new ArrayList<>();
        timeZoneSelectors = new ArrayList<>();
        selectedTimeZones = PreferencesManager.loadTimeZonePreferences();
        displaySeconds = PreferencesManager.loadDisplaySeconds();
        
        // Load and set API key for holiday service
        String apiKey = PreferencesManager.loadApiNinjaKey();
        String currentLocation = PreferencesManager.loadCurrentLocation();

        HolidayService.setApiNinjaKey(apiKey);
        WeatherService.WeatherData.setCurrentLocation(currentLocation);
    }

    private void initializeUI() {
        frame = new JFrame("World Clock");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setResizable(true);
        frame.setBackground(new Color(20, 20, 20));

        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(new Color(20, 20, 20));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Title with Settings button (top right)
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(new Color(20, 20, 20));
        JLabel title = new JLabel("World Clock", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 28));
        title.setForeground(Color.WHITE);
        titlePanel.add(title, BorderLayout.CENTER);
        
        JButton settingsButton = new JButton("⚙ Settings");
        settingsButton.setPreferredSize(new Dimension(100, 30));
        settingsButton.addActionListener(e -> openPreferencesDialog());
        titlePanel.add(settingsButton, BorderLayout.EAST);
        
        mainPanel.add(titlePanel, BorderLayout.NORTH);

        // UTC Clock (center top section)
        JPanel utcPanel = new JPanel(new BorderLayout());
        utcPanel.setBackground(new Color(20, 20, 20));
        utcPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 0));
        JLabel utcLabel = new JLabel("UTC Primary Clock", SwingConstants.CENTER);
        utcLabel.setFont(new Font("Arial", Font.BOLD, 14));
        utcLabel.setForeground(Color.WHITE);
        utcPanel.add(utcLabel, BorderLayout.NORTH);
        
        utcClock = new DigitalClock("UTC", ZoneId.of("UTC"), displaySeconds);
        utcClock.updateTime();
        
        JPanel utcClockContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        utcClockContainer.setBackground(new Color(20, 20, 20));
        utcClockContainer.add(utcClock);
        utcPanel.add(utcClockContainer, BorderLayout.CENTER);

        // Combined center panel: UTC clock + selectable clocks
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setBackground(new Color(20, 20, 20));
        centerPanel.add(utcPanel, BorderLayout.NORTH);
        JPanel selectablePanel = new JPanel(new GridLayout(3, 1, 10, 10));
        selectablePanel.setBackground(new Color(20, 20, 20));
        selectablePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                "Selectable Time Zones",
                0,
                0,
                new Font("Arial", Font.BOLD, 12),
                Color.WHITE
        ));

        // Create three selectable clock panels
        for (int i = 0; i < 3; i++) {
            JPanel clockPanel = createSelectableClockPanel(i);
            selectablePanel.add(clockPanel);
        }

        centerPanel.add(new JScrollPane(selectablePanel), BorderLayout.CENTER);
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Footer
        JLabel footer = new JLabel("Select timezones from dropdowns", SwingConstants.LEFT);
        footer.setForeground(Color.GRAY);
        footer.setFont(new Font("Arial", Font.PLAIN, 11));
        footer.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        mainPanel.add(footer, BorderLayout.SOUTH);

        frame.add(mainPanel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Start timer to update clocks every second
        startClockUpdater();
    }

    private JPanel createSelectableClockPanel(int index) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(35, 35, 35));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Create clock (top)
        String initialZone = selectedTimeZones.get(index);
        DigitalClock clock = new DigitalClock(initialZone, ZoneId.of(initialZone), displaySeconds);
        clock.updateTime();
        selectableClocks.add(clock);

        // Center the clock horizontally
        JPanel clockContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        clockContainer.setBackground(new Color(35, 35, 35));
        clockContainer.add(clock);

        panel.add(clockContainer, BorderLayout.CENTER);

        // Dropdown selector (bottom) - fixed size
        JPanel selectorPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        selectorPanel.setBackground(new Color(35, 35, 35));

        JComboBox<String> comboBox = TimeZoneSelector.createTimeZoneComboBox();
        comboBox.setSelectedItem(initialZone);

        // Set fixed preferred width for the dropdown (longest timezone string)
        comboBox.setPreferredSize(new Dimension(280, 25));

        // Add listener to update clock when timezone changes
        comboBox.addActionListener(e -> {
            String newZone = (String) comboBox.getSelectedItem();
            clock.setLabel(newZone);
            clock.setZoneId(ZoneId.of(newZone));
            
            // Save preference
            selectedTimeZones.set(index, newZone);
            PreferencesManager.saveTimeZonePreferences(selectedTimeZones);
        });

        timeZoneSelectors.add(comboBox);

        selectorPanel.add(comboBox);

        panel.add(selectorPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void startClockUpdater() {
        updateTimer = new Timer(1000, e -> {
            utcClock.updateTime();
            for (DigitalClock clock : selectableClocks) {
                clock.updateTime();
            }
        });
        updateTimer.start();
    }

    private void openPreferencesDialog() {
        PreferencesDialog dialog = new PreferencesDialog(
            frame,
            selectedTimeZones,
            displaySeconds,
            currentLocation,
            (timeZones, displaySeconds, apiKey, currentLocation) -> {
                // Update instance variables
                this.selectedTimeZones = new ArrayList<>(timeZones);
                this.displaySeconds = displaySeconds;
                this.currentLocation = currentLocation;

                // Update all clocks with new display seconds setting
                utcClock.setDisplaySeconds(displaySeconds);
                for (DigitalClock clock : selectableClocks) {
                    clock.setDisplaySeconds(displaySeconds);
                }

                // Set the API key in HolidayService for future API calls
                HolidayService.setApiNinjaKey(apiKey);
                WeatherService.WeatherData.setCurrentLocation(currentLocation);

                // TODO: If timezone selection changes, update the dropdown selections
                // This would require updating the comboBoxes or recreating the UI
            }
        );
        dialog.setVisible(true);
    }

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        FlatDarkLaf.setup();
        SwingUtilities.invokeLater(() -> {
            WorldClockApp app = new WorldClockApp();
            app.initializeUI();
        });
    }
}

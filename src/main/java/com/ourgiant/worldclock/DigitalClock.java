package com.ourgiant.worldclock;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A digital clock component that displays time, weather, UTC offset, and holiday info for a specific timezone.
 */
public class DigitalClock extends JPanel {
    private ZoneId zoneId;
    private String label;
    private final DateTimeFormatter timeFormatter;
    private final DateTimeFormatter timeFormatterNoSeconds;
    private final DateTimeFormatter dateFormatter;
    private String currentTime;
    private String currentDate;
    private boolean displaySeconds;
    private boolean displayWeather = true;
    private boolean displayUTCOffset = true;
    private boolean displayHolidays = true;
    
    private WeatherService.WeatherData currentWeather;
    private HolidayService.Holiday todayHoliday;

    public DigitalClock(String label, ZoneId zoneId, boolean displaySeconds) {
        this.label = label;
        this.zoneId = zoneId;
        this.displaySeconds = displaySeconds;
        this.timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        this.timeFormatterNoSeconds = DateTimeFormatter.ofPattern("HH:mm");
        this.dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        this.currentTime = "";
        this.currentDate = "";

        setPreferredSize(new Dimension(320, 120));
        setBackground(new Color(30, 30, 30));
        setForeground(Color.WHITE);
        setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));
        
        // Fetch weather and holidays in background
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                currentWeather = WeatherService.getWeather(zoneId);
                todayHoliday = HolidayService.getHolidayForDate(zoneId, LocalDate.now());
                return null;
            }
            
            @Override
            protected void done() {
                repaint();
            }
        };
        worker.execute();
    }

    public void updateTime() {
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        this.currentTime = now.format(displaySeconds ? timeFormatter : timeFormatterNoSeconds);
        this.currentDate = now.format(dateFormatter);
        
        // Refresh holiday info
        todayHoliday = HolidayService.getHolidayForDate(zoneId, LocalDate.now());
        repaint();
    }

    public void setDisplaySeconds(boolean displaySeconds) {
        this.displaySeconds = displaySeconds;
        updateTime();
    }
    
    public void setDisplayWeather(boolean displayWeather) {
        this.displayWeather = displayWeather;
        repaint();
    }
    
    public void setDisplayUTCOffset(boolean displayUTCOffset) {
        this.displayUTCOffset = displayUTCOffset;
        repaint();
    }
    
    public void setDisplayHolidays(boolean displayHolidays) {
        this.displayHolidays = displayHolidays;
        repaint();
    }

    public void setLabel(String label) {
        if (label != null && !label.isEmpty()) {
            this.label = label.length() > 50 ? label.substring(0, 50) : label;
        }
        repaint();
    }

    public void setZoneId(ZoneId zoneId) {
        this.zoneId = zoneId;
        
        // Fetch new weather and holidays
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                currentWeather = WeatherService.getWeather(DigitalClock.this.zoneId);
                todayHoliday = HolidayService.getHolidayForDate(DigitalClock.this.zoneId, LocalDate.now());
                return null;
            }
            
            @Override
            protected void done() {
                updateTime();
            }
        };
        worker.execute();
    }

    public ZoneId getZoneId() {
        return zoneId;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int y = 15;
        
        // Draw timezone ID and UTC offset (top left)
        g2d.setFont(new Font("Arial", Font.BOLD, 11));
        g2d.setColor(Color.LIGHT_GRAY);
        String tzInfo = zoneId.getId();
        if (displayUTCOffset) {
            tzInfo += " (" + TimezoneUtil.getUTCOffsetString(zoneId) + ")";
        }
        g2d.drawString(tzInfo, 10, y);
        y += 35;

        // Draw time (large, centered)
        g2d.setFont(new Font("Courier New", Font.BOLD, 32));
        g2d.setColor(new Color(0, 200, 100));
        FontMetrics fm = g2d.getFontMetrics();
        int timeX = (getWidth() - fm.stringWidth(currentTime)) / 2;
        g2d.drawString(currentTime, timeX, y);
        y += 23;

        // Draw date and holiday on same line
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.setColor(Color.GRAY);
        
        String dateStr = currentDate;
        String holidayStr = "";
        if (displayHolidays) {
            if (todayHoliday != null) {
                holidayStr = " | 🎉 " + todayHoliday.name;
            } else {
                holidayStr = " | No holidays today";
            }
        }
        
        String dateHolidayLine = dateStr + holidayStr;
        fm = g2d.getFontMetrics();
        int dateX = (getWidth() - fm.stringWidth(dateHolidayLine)) / 2;
        
        // Draw date part
        g2d.setColor(Color.GRAY);
        g2d.drawString(dateStr, dateX, y);
        
        // Draw holiday part in different color
        int xOffset = dateX + g2d.getFontMetrics().stringWidth(dateStr);
        if (todayHoliday != null) {
            g2d.setColor(new Color(255, 200, 0));
        }
        g2d.drawString(holidayStr, xOffset, y);
        y += 25;

        // Draw temperature and condition
        if (displayWeather && currentWeather != null) {
            // Temperature with both C and F (larger font, centered)
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            g2d.setColor(Color.CYAN);
            String tempStr = currentWeather.getTemperatureDisplay();
            fm = g2d.getFontMetrics();
            int tempX = (getWidth() - fm.stringWidth(tempStr)) / 2;
            g2d.drawString(tempStr, tempX, y);
            y += 16;
            
            // Weather condition (smaller, centered)
            g2d.setFont(new Font("Arial", Font.PLAIN, 9));
            g2d.setColor(Color.CYAN);
            String conditionStr = currentWeather.condition;
            fm = g2d.getFontMetrics();
            int condX = (getWidth() - fm.stringWidth(conditionStr)) / 2;
            g2d.drawString(conditionStr, condX, y);
        }
    }
}

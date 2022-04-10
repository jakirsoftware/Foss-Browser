package de.baumann.browser.database;

public class Record {

    private final int ordinal;
    private Boolean isDesktopMode;
    private Boolean isNightMode;
    private long iconColor;
    private String title;
    private String url;
    private long time;
    private int type;     //0 History, 1 Start site, 2 Bookmark

    public Record() {
        this.title = null;
        this.url = null;
        this.time = 0L;
        this.ordinal = -1;
        this.type = -1;
        this.isDesktopMode = null;
        this.iconColor = 0L;
    }

    public Record(String title, String url, long time, int ordinal, int type, Boolean DesktopMode, Boolean NightMode, long iconColor) {
        this.title = title;
        this.url = url;
        this.time = time;
        this.ordinal = ordinal;
        this.type = type;
        this.isDesktopMode = DesktopMode;
        this.isNightMode = NightMode;
        this.iconColor = iconColor;
    }

    public long getIconColor() {
        return iconColor;
    }

    public void setIconColor(long iconColor) {
        this.iconColor = iconColor;
    }

    public Boolean getDesktopMode() {
        return isDesktopMode;
    }

    public void setDesktopMode(Boolean desktopMode) {
        isDesktopMode = desktopMode;
    }

    public Boolean getNightMode() {
        return isNightMode;
    }

    public void setNightMode(Boolean desktopMode) {
        isNightMode = desktopMode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getURL() {
        return url;
    }

    public void setURL(String url) {
        this.url = url;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    int getOrdinal() {
        return ordinal;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}

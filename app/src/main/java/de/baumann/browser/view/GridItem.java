package de.baumann.browser.view;

public class GridItem {
    private final String title;
    private final int icon;
    private final int data;

    public GridItem(int icon, String title, int data) {
        this.title = title;
        this.icon = icon;
        this.data = data;
    }

    public String getTitle() {
        return title;
    }

    public int getIcon() {
        return icon;
    }

    public int getData() {
        return data;
    }
}

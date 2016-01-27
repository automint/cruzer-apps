package com.socketmint.cruzer.drawer;

public class DrawerData {
    public int drawerIcon, titleCount;
    public String itemTitle;

    public DrawerData(String itemTitle, int drawerIcon, int titleCount) {
        this.itemTitle = itemTitle;
        this.drawerIcon = drawerIcon;
        this.titleCount = titleCount;
    }
}

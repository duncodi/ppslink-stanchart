package com.duncodi.ppslink.stanchart.enums;

public enum YesNo {

  YES("Yes"),
  NO("No"),
  ;

  private String name;

  YesNo(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }

  public static YesNo fromString(String text) {
    if (text != null) {
      for (YesNo b : YesNo.values()) {
        if (text.equalsIgnoreCase(b.name) || text.equalsIgnoreCase(b + "")) {
          return b;
        }
      }
    }
    return null;
  }

}

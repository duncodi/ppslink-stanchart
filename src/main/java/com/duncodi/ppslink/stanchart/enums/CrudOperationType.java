package com.duncodi.ppslink.stanchart.enums;

public enum CrudOperationType {

  CREATE("Create"),
  UPDATE("Update"),
  UPDATE_CAPTURING_ENTIRE_OBJECT("Update Capturing The Entire Object"),
  DELETE("Delete"),
  LOGIN("Login"),
  LOGOUT("Logout");

  private String name;

  CrudOperationType(String name) {
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

}

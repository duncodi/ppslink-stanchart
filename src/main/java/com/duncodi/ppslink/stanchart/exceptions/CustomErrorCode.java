package com.duncodi.ppslink.stanchart.exceptions;

import lombok.Getter;

@Getter
public enum CustomErrorCode {

    OBJ_404("Object Not Found Exception"),
    ID_404("Identifier Not Found Exception"),
    REQ_404("Request Not Provided Exception"),
    LIST_404("List Not Found Exception"),
    SIMILAR_OBJ("Similar Object Found Exception"),
    DB_CHILD_REC_FOUND("There Exists Child(ren) Record(s)"),
    INCOMPLETE_REQUEST("Incomplete Request Parameters"),
    NO_TOKEN("No Token"),
    INVALID_TOKEN("Invalid Token"),
    CONNECTION_ERROR("Failed to Connect"),
    INVALID_JSON("Invalid Json File Structure"),
    ;

    private String name;
    public void setName(String name) {
        this.name = name;
    }

    CustomErrorCode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}

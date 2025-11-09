package com.duncodi.ppslink.stanchart.enums;


public enum StraightToBankInstructionPriority {

    NORM("Normal payment"),
    HIGH("Urgent payment");

    private String name;

    private StraightToBankInstructionPriority(String name){
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString(){
        return name;
    }

    public static StraightToBankInstructionPriority fromString(String text) {
        if (text != null) {
            for (StraightToBankInstructionPriority b : StraightToBankInstructionPriority.values()) {
                if (text.equalsIgnoreCase(b.name) || text.equalsIgnoreCase(b + "")) {
                    return b;
                }
            }
        }
        return NORM;
    }

}

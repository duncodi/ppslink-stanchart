package com.duncodi.ppslink.stanchart.enums;

public enum StraightToBankInstructionType {

    CREDIT_TRANSFER("Credit Transfer Instruction (Pain 001)"),
    DIRECT_DEBIT("Direct Debit/Collection (Pain 008)");

    private String name;
    private StraightToBankInstructionType(String name) {
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

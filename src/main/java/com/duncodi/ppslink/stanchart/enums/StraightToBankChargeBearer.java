package com.duncodi.ppslink.stanchart.enums;


public enum StraightToBankChargeBearer {

    DEBT("By Scheme"),
    CRED("By Customer"),
    SHAR("Shared");

    /*
    DEBT = Transaction charges are to be borne by the debtor (i.e. ordering customer) (OUR)
CRED = Transaction charges are to be borne by the creditor (i.e. counter party) (BEN)
SHAR = Transaction charges on the sender side are to be borne by the debtor (i.e. ordering customer) and on the receiver side are to be borne by the creditor (i.e. counter party)
     */

    private String name;

    private StraightToBankChargeBearer(String name){
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

    public static StraightToBankChargeBearer fromString(String text) {
        if (text != null) {
            for (StraightToBankChargeBearer b : StraightToBankChargeBearer.values()) {
                if (text.equalsIgnoreCase(b.name) || text.equalsIgnoreCase(b + "")) {
                    return b;
                }
            }
        }
        return DEBT;
    }

}

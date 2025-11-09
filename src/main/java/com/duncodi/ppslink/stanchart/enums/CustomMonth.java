package com.duncodi.ppslink.stanchart.enums;

import lombok.Getter;

@Getter
public enum CustomMonth {

	JAN("January", 1, "01"),
	FEB("February", 2, "02"),
	MAR("March", 3, "03"),
	APR("April", 4, "04"),
	MAY("May", 5, "05"),
	JUN("June", 6, "06"),
	JUL("July", 7, "07"),
	AUG("August", 8, "08"),
	SEP("September", 9, "09"),
	OCT("October", 10, "10"),
	NOV("November", 11, "11"),
	DEC("December", 12, "12");

	private String name;
	private Integer monthInt;
	private String monthIntString;

	public static CustomMonth getMonthByMonthInt(int mon){

		for(CustomMonth m : CustomMonth.values()){
			if(mon==m.getMonthInt()){
				return m;
			}
		}
		return null;
	}

	CustomMonth(String name, Integer monthInt, String monthIntString) {
		this.name = name;
		this.monthInt = monthInt;
		this.monthIntString = monthIntString;
	}

	private CustomMonth(String name) {
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

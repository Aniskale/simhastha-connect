package com.simhastha.packages;
public enum BusType { AC_SLEEPER("AC Sleeper"), NON_AC_SLEEPER("Non-AC Sleeper"), AC_SEATER("AC Seater"), VOLVO_AC("Volvo AC"), SEMI_SLEEPER("Semi Sleeper"); private final String label; BusType(String label){this.label=label;} public String label(){return label;} @Override public String toString(){return label;} }

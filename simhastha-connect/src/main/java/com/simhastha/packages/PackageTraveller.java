package com.simhastha.packages;

/** In-memory traveller details for the prototype booking draft; no KYC is performed. */
public record PackageTraveller(String fullName, int age, String gender, String idType, String idNumber) { }

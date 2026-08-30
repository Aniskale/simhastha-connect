package com.simhastha.model;
import java.util.Map;
/** Language-ready catalogue content; callers safely fall back to English. */
public record LocalizedGhatContent(Map<String,String> values) { public String forLanguage(String language){return values.getOrDefault(language, values.getOrDefault("en", "Information pending verification"));} }

package com.myproxy.ui;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Internationalization manager for MyProxy application
 */
public class I18nManager {
    private static final String BUNDLE_NAME = "messages.messages";
    private static I18nManager instance;
    
    private ResourceBundle bundle;
    private Locale currentLocale;
    
    private I18nManager() {
        // Default to auto-detect
        initialize(Locale.getDefault());
    }
    
    public static synchronized I18nManager getInstance() {
        if (instance == null) {
            instance = new I18nManager();
        }
        return instance;
    }
    
    /**
     * Initialize with specified locale
     */
    public void initialize(Locale locale) {
        this.currentLocale = locale;
        this.bundle = ResourceBundle.getBundle(BUNDLE_NAME, locale);
    }
    
    /**
     * Initialize with language tag (e.g., "zh", "en", "auto")
     */
    public void initialize(String languageTag) {
        if (languageTag == null || languageTag.isEmpty() || "auto".equalsIgnoreCase(languageTag)) {
            initialize(Locale.getDefault());
        } else {
            initialize(Locale.forLanguageTag(languageTag));
        }
    }
    
    /**
     * Get localized string
     */
    public String getString(String key) {
        try {
            return bundle.getString(key);
        } catch (Exception e) {
            return "!" + key + "!";
        }
    }
    
    /**
     * Get localized string with parameters
     */
    public String getString(String key, Object... params) {
        String message = getString(key);
        if (params != null && params.length > 0) {
            return java.text.MessageFormat.format(message, params);
        }
        return message;
    }
    
    /**
     * Get current locale
     */
    public Locale getCurrentLocale() {
        return currentLocale;
    }
}

package com.tsqco.helper;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

public class CookieHelper {
    private String cookieName;
    private String cookieValue;
    private HttpServletResponse httpResponse;
    private Integer maxAge;

    public CookieHelper (String name, String value, HttpServletResponse response, Integer maxAge) {
        this.cookieName = name;
        this.cookieValue = value;
        this.httpResponse = response;
        this.maxAge = maxAge;
    }

    public void addCookie() {
        Cookie cookie = new Cookie(this.cookieName, this.cookieValue);
        cookie.setHttpOnly(true); // Prevent client-side JavaScript access
        cookie.setPath("/");      // Set path for cookie
        cookie.setMaxAge(this.maxAge);   // Set expiration time (in seconds)
        this.httpResponse.addCookie(cookie);
    }
}

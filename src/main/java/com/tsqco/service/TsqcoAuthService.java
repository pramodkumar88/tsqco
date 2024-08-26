package com.tsqco.service;

import com.angelbroking.smartapi.http.exceptions.SmartAPIException;
import com.angelbroking.smartapi.models.User;
import com.tsqco.models.dto.AngelUserProfileDTO;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.Profile;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public interface TsqcoAuthService {
    Profile getAccessToken(String requestToken) throws KiteException, IOException;

    AngelUserProfileDTO getProfile() throws SmartAPIException;

}

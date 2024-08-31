package com.tsqco.service.impl;

import com.angelbroking.smartapi.SmartConnect;
import com.angelbroking.smartapi.http.exceptions.SmartAPIException;
import com.angelbroking.smartapi.models.User;
import com.tsqco.config.TsqcoConfig;
import com.tsqco.config.TsqcoProperties;
import com.tsqco.models.dto.AngelUserProfileDTO;
import com.tsqco.service.TsqcoAuthService;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.Profile;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Date;

import static com.tsqco.constants.TsqcoConstants.FEED_TOKEN;

@Service
@Slf4j
public class TsqcoAuthServiceImpl implements TsqcoAuthService {

    TsqcoProperties tsqcoProps;

    TsqcoConfig tsqcoConfig;

    TsqcoAuthServiceImpl(TsqcoProperties tsqcoProps, TsqcoConfig tsqcoConfig) {
    this.tsqcoProps = tsqcoProps;
    this.tsqcoConfig = tsqcoConfig;
    }

    @Override
    public Profile getAccessToken(String requestToken) throws KiteException, IOException {
        return tsqcoConfig.getKiteConnect().getProfile();
    }

    @Override
    public AngelUserProfileDTO getProfile() throws SmartAPIException {
        SmartConnect smartConnect = tsqcoConfig.getSmartConnect();
        User profile =  smartConnect.getProfile();
        AngelUserProfileDTO angelUserProfileDTO =
                new AngelUserProfileDTO(
                        profile.getUserName(),
                        profile.getUserId(),
                        profile.getMobileNo(),
                        profile.getBrokerName(),
                        profile.getEmail(),
                        profile.getLastLoginTime(),
                        smartConnect.getAccessToken(),
                        smartConnect.getPublicToken(),
                        profile.getProducts(),
                        profile.getExchanges(),
                        FEED_TOKEN);
       return angelUserProfileDTO;
    }
}

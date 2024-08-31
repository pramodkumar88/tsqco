package com.tsqco.controller;

import com.angelbroking.smartapi.http.exceptions.SmartAPIException;
import com.angelbroking.smartapi.models.User;
import com.tsqco.config.TsqcoProperties;
import com.tsqco.constants.TsqcoConstants;
import com.tsqco.helper.CookieHelper;
import com.tsqco.models.dto.AngelUserProfileDTO;
import com.tsqco.service.TsqcoAuthService;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.Profile;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping(value = "/tsqco")
@Slf4j
@AllArgsConstructor
@CrossOrigin(origins = TsqcoConstants.LOCALHOST_WEB)
public class TsqcoAuthController {

    private final TsqcoAuthService tsqcoService;

    private final TsqcoProperties tsqcoProps;

    @GetMapping(value = "/auth", produces = "application/json")
    public ResponseEntity<Profile> userAuth(@RequestParam String token) throws KiteException, IOException {
        tsqcoProps.setKiteRequestToken(token);
        Profile profile = tsqcoService.getAccessToken(token);
        return new ResponseEntity<>(profile, HttpStatus.OK);
    }

    @GetMapping(value = "/angel/auth", produces = "application/json")
    public ResponseEntity<AngelUserProfileDTO> angelAuth() throws SmartAPIException {
        AngelUserProfileDTO user = tsqcoService.getProfile();
        return new ResponseEntity<>(user, HttpStatus.OK);
    }
}


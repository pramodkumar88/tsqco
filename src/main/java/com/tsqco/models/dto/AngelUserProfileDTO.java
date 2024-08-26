package com.tsqco.models.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.Date;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AngelUserProfileDTO {
    private String userName;
    private String userId;
    private String mobileNo;
    private String brokerName;
    private String email;
    private Date lastLoginTime;
    private String accessToken;
    private String refreshToken;
    private String[] products;
    private String[] exchanges;
    private String feedToken;


}

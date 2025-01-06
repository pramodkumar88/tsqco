package com.tsqco.controller;

import com.tsqco.constants.TsqcoConstants;
import com.tsqco.helper.CacheHelper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/tsqco")
@AllArgsConstructor
@Slf4j
@CrossOrigin(origins = TsqcoConstants.LOCALHOST_WEB)
public class TsqcoCacheController {

    private final CacheHelper cacheHelper;
    @DeleteMapping("/redis/cache/{key}")
    public void removecache(@PathVariable String key) {
        cacheHelper.removeTokenFromCache(key);
    }

    @DeleteMapping("/redis/cache/clearall")
    public void removeAll() {
        cacheHelper.removeAllTokensFromCache();
    }
}



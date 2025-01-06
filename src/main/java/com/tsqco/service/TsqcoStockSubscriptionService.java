package com.tsqco.service;

import com.angelbroking.smartapi.smartstream.models.TokenID;
import com.neovisionaries.ws.client.WebSocketException;
import com.tsqco.models.AngelStockSubscription;

import java.util.List;
import java.util.Set;

public interface TsqcoStockSubscriptionService {

    List<AngelStockSubscription> getSubscribedStocks();

    String addStockSubscription(List<AngelStockSubscription> stockSubscription);

    AngelStockSubscription updateStockSubscription(Long id, boolean subscribe);

    void deleteByToken(String token);

    void deleteAllToken() throws  RuntimeException;

    void addStockSubscription(Set<TokenID> tokenIDS);

    void removeStockSubscription(Set<TokenID> tokenIDS);
}


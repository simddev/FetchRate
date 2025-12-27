package com.fetchrate.update;

import org.springframework.stereotype.Service;


@Service
public class RateUpdater {
    private CryptoRateUpdater cryptoUpdate;
    private FiatRateUpdater fiatUpdate;

    public RateUpdater(CryptoRateUpdater cryptoUpdate, FiatRateUpdater fiatUpdate) {
        this.cryptoUpdate = cryptoUpdate;
        this.fiatUpdate = fiatUpdate;
    }

    public void updateRates() {
        cryptoUpdate.update();
        fiatUpdate.update();
    }

}

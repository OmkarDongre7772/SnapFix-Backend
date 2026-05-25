package com.snapfix.wallet.dto;

import com.snapfix.user.dto.UserResponse;
import com.snapfix.wallet.entity.Wallet;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class WalletResponse {

    private String id;

    private UserResponse worker;

    private String balance;

    public static WalletResponse mapToResponse(Wallet w){
        WalletResponse response = new WalletResponse();
        response.setBalance(w.getBalance().toPlainString());
        response.setId(w.getId().toString());
        response.setWorker(UserResponse.mapToResponse(w.getWorker()));
        return response;
    }
}

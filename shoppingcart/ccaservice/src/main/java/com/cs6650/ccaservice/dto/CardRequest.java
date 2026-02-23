package com.cs6650.ccaservice.dto;


// Data: { "cardNumber": "1234-5678-9012-3456" }
public class CardRequest {
    private String cardNumber;

    // 必须有的空构造函数
    public CardRequest() {}

    // Getter and Setter
    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }
}

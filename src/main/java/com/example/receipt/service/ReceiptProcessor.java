package com.example.receipt.service;

import com.example.receipt.dto.ReceiptProcessResponse;

public interface ReceiptProcessor {

    ReceiptProcessResponse processReceipt(byte[] image, String filename);
}

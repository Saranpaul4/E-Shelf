package com.library.web.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Talks to the Razorpay payment gateway using only what the JDK already ships, so the
 * offline Maven build never needs a new dependency. Two jobs:
 *   1. createOrder  - POST to Razorpay to book an amount and get an order id back.
 *   2. verifySignature - recompute the HMAC-SHA256 Razorpay sends in the browser and
 *      compare it, which is how we know the "payment succeeded" notice is genuine.
 */
@Service
public class RazorpayService {

    private static final String ORDERS_ENDPOINT = "https://api.razorpay.com/v1/orders";
    private static final String RECEIPT = "eshelf-cart";
    /** Pulls the "id":"order_..." out of Razorpay's JSON reply without a JSON library. */
    private static final Pattern ORDER_ID = Pattern.compile("\"id\"\\s*:\\s*\"(order_[^\"]+)\"");

    private final String keyId;
    private final String keySecret;
    private final HttpClient http;

    public RazorpayService(@Value("${razorpay.key-id}") String keyId,
                           @Value("${razorpay.key-secret}") String keySecret) {
        this.keyId = keyId;
        this.keySecret = keySecret;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    /** The public key id is safe to hand to the browser; the secret never leaves here. */
    public String keyId() {
        return keyId;
    }

    /** What the gateway said about an order attempt. */
    public record GatewayResult(boolean success, String orderId, int amountPaise, String message) {
    }

    /**
     * Book the amount with Razorpay. Money here is the cart total in whole currency units,
     * but Razorpay only accepts the smallest unit (paise), so we multiply by 100 and round.
     */
    public GatewayResult createOrder(BigDecimal amount, String buyerName) {
        int paise = amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .intValueExact();
        try {
            String body = "{\"amount\":" + paise
                    + ",\"currency\":\"INR\""
                    + ",\"receipt\":\"" + RECEIPT + "\""
                    + ",\"notes\":{\"buyer\":\"" + escape(buyerName) + "\"}}";
            HttpRequest request = HttpRequest.newBuilder(URI.create(ORDERS_ENDPOINT))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", basicAuth())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response =
                    http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) {
                return new GatewayResult(false, null, paise,
                        "Razorpay refused the order (HTTP " + response.statusCode()
                                + "). Check the test keys in application.properties.");
            }
            Matcher matcher = ORDER_ID.matcher(response.body());
            if (!matcher.find()) {
                return new GatewayResult(false, null, paise, "Razorpay replied without an order id.");
            }
            return new GatewayResult(true, matcher.group(1), paise, "Order ready.");
        } catch (Exception e) {
            return new GatewayResult(false, null, paise,
                    "Could not reach Razorpay (" + e.getClass().getSimpleName()
                            + "). Is the machine online and the key real?");
        }
    }

    /**
     * Razorpay sends back order_id|payment_id signed with our secret. Recompute the same
     * signature and compare in constant time; only a match proves the payment is genuine.
     */
    public boolean verifySignature(String orderId, String paymentId, String signature) {
        if (orderId == null || paymentId == null || signature == null || signature.isBlank()) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal((orderId + "|" + paymentId).getBytes(StandardCharsets.UTF_8));
            String expected = HexFormat.of().formatHex(digest);
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }

    private String basicAuth() {
        String pair = keyId + ":" + keySecret;
        return "Basic " + Base64.getEncoder().encodeToString(pair.getBytes(StandardCharsets.UTF_8));
    }

    /** Keep a buyer's name from breaking out of the JSON string we hand to Razorpay. */
    private String escape(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "").replace("\n", "");
    }
}

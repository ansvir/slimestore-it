package com.example.slimestore.util;

import com.example.slimestore.jpa.Order;

public final class OrderUtil {

    public static String buildOrderStatusMessage(Order.OrderStatus orderStatus, Long orderId) {
        return String.format("%s:%s", orderStatus.name(), orderId);
    }

}

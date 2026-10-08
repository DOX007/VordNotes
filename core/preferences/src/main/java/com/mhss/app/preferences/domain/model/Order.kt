package com.mhss.app.preferences.domain.model

sealed class OrderType {
    data object ASC : OrderType()
    data object DESC : OrderType()
}

sealed class Order(val orderType: OrderType) {
    abstract fun copyOrder(orderType: OrderType): Order

    data class Alphabetical(val type: OrderType = OrderType.ASC) : Order(type) {
        override fun copyOrder(orderType: OrderType): Order = copy(type = orderType)
    }

    data class DateCreated(val type: OrderType = OrderType.ASC) : Order(type) {
        override fun copyOrder(orderType: OrderType): Order = copy(type = orderType)
    }

    data class DateModified(val type: OrderType = OrderType.ASC) : Order(type) {
        override fun copyOrder(orderType: OrderType): Order = copy(type = orderType)
    }

    data class Priority(val type: OrderType = OrderType.ASC) : Order(type) {
        override fun copyOrder(orderType: OrderType): Order = copy(type = orderType)
    }

    data class DueDate(val type: OrderType = OrderType.ASC) : Order(type) {
        override fun copyOrder(orderType: OrderType): Order = copy(type = orderType)
    }

    // NYTT – manuell ordning (ignorerar ASC/DESC)
    data object Manual : Order(OrderType.ASC) {
        override fun copyOrder(orderType: OrderType): Order = this
    }
}

fun Int.toOrder(): Order {
    return when (this) {
        0 -> Order.Alphabetical(OrderType.ASC)
        1 -> Order.DateCreated(OrderType.ASC)
        2 -> Order.DateModified(OrderType.ASC)
        3 -> Order.Priority(OrderType.ASC)
        8 -> Order.DueDate(OrderType.ASC)
        4 -> Order.Alphabetical(OrderType.DESC)
        5 -> Order.DateCreated(OrderType.DESC)
        6 -> Order.DateModified(OrderType.DESC)
        7 -> Order.Priority(OrderType.DESC)
        9 -> Order.DueDate(OrderType.DESC)
        10 -> Order.Manual           // ← NYTT värde för manuell ordning
        else -> Order.DateModified(OrderType.ASC)
    }
}

fun Order.toInt(): Int {
    return when (this) {
        is Order.Alphabetical -> if (orderType is OrderType.ASC) 0 else 4
        is Order.DateCreated -> if (orderType is OrderType.ASC) 1 else 5
        is Order.DateModified -> if (orderType is OrderType.ASC) 2 else 6
        is Order.Priority -> if (orderType is OrderType.ASC) 3 else 7
        is Order.DueDate -> if (orderType is OrderType.ASC) 8 else 9
        is Order.Manual -> 10                     // ← NYTT
    }
}
package com.rudderstack.sdk.kotlin.core.ecommerce

/**
 * This object contains the names of the E-Commerce events.
 */
@Suppress("UndocumentedPublicProperty")
object ECommerceEvents {

    const val PRODUCTS_SEARCHED: String = "Products Searched"
    const val PRODUCT_LIST_VIEWED: String = "Product List Viewed"
    const val PRODUCT_LIST_FILTERED: String = "Product List Filtered"
    const val PROMOTION_VIEWED: String = "Promotion Viewed"
    const val PROMOTION_CLICKED: String = "Promotion Clicked"
    const val PRODUCT_CLICKED: String = "Product Clicked"
    const val PRODUCT_VIEWED: String = "Product Viewed"
    const val PRODUCT_ADDED: String = "Product Added"
    const val PRODUCT_REMOVED: String = "Product Removed"
    const val CART_VIEWED: String = "Cart Viewed"
    const val CHECKOUT_STARTED: String = "Checkout Started"
    const val CHECKOUT_STEP_VIEWED: String = "Checkout Step Viewed"
    const val CHECKOUT_STEP_COMPLETED: String = "Checkout Step Completed"
    const val PAYMENT_INFO_ENTERED: String = "Payment Info Entered"
    const val ORDER_UPDATED: String = "Order Updated"
    const val ORDER_COMPLETED: String = "Order Completed"
    const val ORDER_REFUNDED: String = "Order Refunded"
    const val ORDER_CANCELLED: String = "Order Cancelled"
    const val COUPON_ENTERED: String = "Coupon Entered"
    const val COUPON_APPLIED: String = "Coupon Applied"
    const val COUPON_DENIED: String = "Coupon Denied"
    const val COUPON_REMOVED: String = "Coupon Removed"
    const val PRODUCT_ADDED_TO_WISH_LIST: String = "Product Added to Wishlist"
    const val PRODUCT_REMOVED_FROM_WISH_LIST: String = "Product Removed from Wishlist"
    const val WISH_LIST_PRODUCT_ADDED_TO_CART: String = "Wishlist Product Added to Cart"
    const val PRODUCT_SHARED: String = "Product Shared"
    const val CART_SHARED: String = "Cart Shared"
    const val PRODUCT_REVIEWED: String = "Product Reviewed"
}

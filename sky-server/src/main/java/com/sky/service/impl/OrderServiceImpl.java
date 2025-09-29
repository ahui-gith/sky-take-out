package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.service.AddressBookService;
import com.sky.service.OrderService;
import com.sky.service.ShoppingCartService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    /**
     * 用户下单
     *
     * @param ordersSubmitDTO 包含：地址簿id addressBookId; 付款方式 payMethod; 备注 remark;
     *                        计送达时间 estimatedDeliveryTime; 配送状态  1立即送出  0选择具体时间 deliveryStatus;
     *                        餐具数量 tablewareNumber; 餐具数量状态  1按餐量提供  0选择具体数量 tablewareStatus;
     *                        打包费 packAmount; 总金额 amount;
     * @return
     */
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        // 判断是否合法
        // 1. 地址簿不能为空
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            // 返回错误信息
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        // 2. 购物车不能为空
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder().userId(userId).build();
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        if (shoppingCartList == null || shoppingCartList.size() == 0) {
            // 抛出异常
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }


        // 添加一条订单，
        // 包含：订单号 number; 订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消 7退款 status;
        // 下单用户id userId; 地址id ddressBookId; 下单时间 orderTime; 结账时间 checkoutTime;
        // 支付方式 1微信，2支付宝 payMethod; 支付状态 0未支付 1已支付 2退款 payStatus; 实收金额 amount;
        // 备注 remark; 用户名 userName; 手机号 phone; 地址 address; 收货人 consignee; 订单取消原因 cancelReason;
        // 订单拒绝原因 rejectionReason; 订单取消时间 cancelTime; 预计送达时间 estimatedDeliveryTime;
        // 配送状态  1立即送出  0选择具体时间 deliveryStatus; 送达时间 deliveryTime; 打包费 packAmount;
        // 餐具数量 tablewareNumber; 餐具数量状态  1按餐量提供  0选择具体数量 tablewareStatus;
        Orders order = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, order);
        order.setNumber(String.valueOf(System.currentTimeMillis()));// 订单号, 使用当前时间戳
        order.setStatus(Orders.PENDING_PAYMENT);// 待付款
        order.setUserId(userId);// 用户id
        order.setOrderTime(LocalDateTime.now());// 下单时间
        order.setPayStatus(Orders.UN_PAID);// 未支付
        order.setPhone(addressBook.getPhone());// 手机号
        order.setAddress(addressBook.getDetail());// 地址
        order.setConsignee(addressBook.getConsignee());// 收货人

        orderMapper.insert(order);

        // 添加n条订单详情
        List<OrderDetail> orderDetailList = new ArrayList<>();
        shoppingCartList.forEach(cart -> {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(order.getId());
            orderDetailList.add(orderDetail);
        });
        orderDetailMapper.insertBatch(orderDetailList);

        // 删除购物车
        shoppingCartMapper.deleteByUserId(userId);

        // 封装结果值并返回
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(order.getId())
                .orderNumber(order.getNumber())
                .orderAmount(order.getAmount())
                .orderTime(order.getOrderTime())
                .build();
        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }
}

package com.sky.exception;

/**
 * 自定义业务异常类：地址簿异常
 */
public class AddressBookBusinessException extends BaseException {

    public AddressBookBusinessException(String msg) {
        super(msg);
    }

}

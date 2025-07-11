package com.xuecheng.base.exception;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @description 全局异常处理器
 * @author Mr.M
 * @date 2022/9/6 11:29
 * @version 1.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

   @ExceptionHandler(XueChengPlusException.class)
   @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
   public RestErrorResponse customException(XueChengPlusException e) {
      log.error("【系统异常】{}",e.getErrMessage(),e);
      return new RestErrorResponse(e.getErrMessage());

   }

   @ExceptionHandler(Exception.class)
   @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
   public RestErrorResponse exception(Exception e) {

      log.error("【系统异常】{}",e.getMessage(),e);

      return new RestErrorResponse(CommonError.UNKOWN_ERROR.getErrMessage());

   }

   /**
    * 处理JSR303参数校验异常
    */
   @ExceptionHandler(MethodArgumentNotValidException.class)
   @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
   public RestErrorResponse methodArgumentNotValidException(MethodArgumentNotValidException e) {
      BindingResult bindingResult = e.getBindingResult();
      List<String> msgList = new ArrayList<>();
      //将错误信息放在msgList
      bindingResult.getFieldErrors().stream().forEach(item->msgList.add(item.getDefaultMessage()));
      //拼接错误信息
      String msg = StringUtils.join(msgList, ",");
      log.error("【系统异常】{}",msg);
      return new RestErrorResponse(msg);
   }
}
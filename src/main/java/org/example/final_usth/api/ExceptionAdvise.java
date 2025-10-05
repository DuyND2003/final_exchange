package org.example.final_usth.api;

import lombok.extern.slf4j.Slf4j;
import org.example.final_usth.api.model.ErrorMessage;
import org.example.final_usth.exception.ServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.RequestContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Đây là Global Exception Handler cho REST API.
 * Khi bất kỳ controller nào ném exception → Spring sẽ đi qua đây để format lại response.
 * Thay vì client nhận stacktrace khó đọc, client sẽ luôn nhận JSON chuẩn dạng:
 */
@ControllerAdvice // / Đánh dấu class này là global exception handler cho toàn bộ project Spring
@Slf4j
public class ExceptionAdvise {
    // // ----------- 1. Bắt tất cả Exception chung -----------
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR) // Trả về HTTP 500
    @ExceptionHandler(Exception.class)  // // Xử lý mọi Exception không chỉ địn
    @ResponseBody
    public ErrorMessage handleException(Exception e) {
        log.error("http error", e);  // // Log lỗi với stacktrace
        // // Trả về JSON dạng {"message": "..."} cho client
        return new ErrorMessage(e.getMessage());
    }

    //  ----------- 2. Bắt lỗi ServiceException (custom) -----------
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(ServiceException.class)
    @ResponseBody
    public ErrorMessage handleException(ServiceException e, RequestContext request) {
        // Log chi tiết: code, message và URI đang gọi
        log.error("http error: {} {} {}", e.getCode(), e.getMessage(), request.getRequestUri());
        // // Trả về code lỗi để client dễ xử lý
        return new ErrorMessage(e.getCode().name());
    }

    //  // ----------- 3. Bắt lỗi validate dữ liệu (Spring Validation) -----------
    @ResponseStatus(HttpStatus.BAD_REQUEST) //  // HTTP 400
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public ErrorMessage handleException(MethodArgumentNotValidException e) {
        log.error("http error", e);
        //  // Gom các lỗi validate field thành 1 chuỗi dễ đọc
        StringBuilder sb = new StringBuilder();
        e.getFieldErrors().forEach(x -> {
            sb.append(x.getField()).append(":").append(x.getDefaultMessage()).append("\n");
        });
        // // Ví dụ: "email: must not be blank\npassword: too short\n"
        return new ErrorMessage(sb.toString());
    }
    //  // ----------- 4. Bắt lỗi ResponseStatusException (Spring thường ném ra) -----------
    @ExceptionHandler(ResponseStatusException.class)
    @ResponseBody
    public ErrorMessage handleException(ResponseStatusException e, HttpServletRequest request,
                                        HttpServletResponse response) {
        log.error("http error: {} {}", e.getMessage(), request.getRequestURI());

        response.setStatus(e.getStatus().value());
        //  // Trả về JSON {"message": "..."} cho client
        return new ErrorMessage(e.getMessage());
    }
}
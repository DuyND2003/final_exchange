package org.example.final_usth.api.model;

import lombok.Getter;
import lombok.Setter;

// Class ErrorMessage này chính là một DTO để trả về thông tin lỗi cho client khi API gặp sự cố.
@Getter
@Setter
public class ErrorMessage {
    private String message;
    // Constructor mặc định (cần thiết cho framework như Spring khi deserialize JSON)
    public ErrorMessage() {
    }
    // Constructor tiện dụng khi muốn khởi tạo nhanh đối tượng với message
    public ErrorMessage(String message) {
        this.message = message;
    }
}

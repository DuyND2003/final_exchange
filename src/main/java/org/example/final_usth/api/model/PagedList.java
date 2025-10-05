package org.example.final_usth.api.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
// generic wrapper class dùng để trả về kết quả dạng danh sách có phân trang (pagination).
public class PagedList<T> { // Đây là một class generic, có thể chứa bất kỳ kiểu dữ liệu nào (T).
    // items: danh sách dữ liệu của trang hiện tại.
    //count: tổng số bản ghi trong toàn bộ dataset (không chỉ trang hiện tại).
    private List<T> items;
    private long count;
    // Constructor để khởi tạo trực tiếp danh sách + tổng số record.
    public PagedList(List<T> items, long count) {
        this.items = items;
        this.count = count;
    }
}

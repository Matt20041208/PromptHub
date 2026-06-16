package com.prompt.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    private List<T> records;
    private long total;
    private long page;
    private long size;

    public static <T> Result<PageResult<T>> success(List<T> records, long total, long page, long size) {
        PageResult<T> pageResult = new PageResult<>(records, total, page, size);
        return Result.success(pageResult);
    }
}

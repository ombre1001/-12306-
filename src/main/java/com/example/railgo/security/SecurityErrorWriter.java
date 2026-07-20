package com.example.railgo.security;


import com.example.railgo.data.vo.Result;
import com.example.railgo.exception.ErrorCode;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class SecurityErrorWriter {

    private final ObjectMapper objectMapper;

    public void write(
            HttpServletResponse response,
            ErrorCode errorCode)
            throws IOException {

        response.setStatus(
                errorCode.getHttpStatus().value()
        );

        response.setCharacterEncoding("UTF-8");
        response.setContentType(
                "application/json;charset=UTF-8"
        );

        Result<Void> result = new Result<>(
                errorCode.getCode(),
                null,
                errorCode.getMessage()
        );

        objectMapper.writeValue(
                response.getWriter(),
                result
        );
    }
}
